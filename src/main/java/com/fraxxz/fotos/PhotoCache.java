package com.fraxxz.fotos;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class PhotoCache {
    public static final class Entry {
        public final Identifier tex;
        public final int w, h;
        Entry(Identifier tex, int w, int h) { this.tex = tex; this.w = w; this.h = h; }
    }

    private static final Map<String, Entry> READY = new HashMap<>();
    private static final Map<String, Long> REQUESTED = new HashMap<>();
    private static final Map<String, Assembler> PENDING = new HashMap<>();

    /** Se llama cada frame desde el renderizador. Devuelve null mientras la foto aún no llega. */
    public static Entry get(String id) {
        Entry e = READY.get(id);
        if (e != null) return e;
        long now = System.currentTimeMillis();
        Long t = REQUESTED.get(id);
        if (t == null || now - t > 30000) {
            REQUESTED.put(id, now);
            request(id);
        }
        return READY.get(id);
    }

    private static void request(String id) {
        Path f = FotosClient.dir("cache").resolve(id + ".png");
        if (Files.exists(f)) {
            try {
                register(id, Files.readAllBytes(f));
                return;
            } catch (IOException ignored) { }
        }
        PacketByteBuf b = PacketByteBufs.create();
        b.writeString(id, 40);
        ClientPlayNetworking.send(FotosMod.REQUEST, b);
    }

    public static void onChunk(String id, int i, int total, byte[] d) {
        if (total < 1 || total > 120 || i < 0 || i >= total) return;
        Assembler a = PENDING.computeIfAbsent(id, k -> new Assembler(total));
        if (a.parts.length != total) { PENDING.remove(id); return; }
        byte[] all = a.add(i, d);
        if (all == null) return;
        PENDING.remove(id);
        saveCache(id, all);
        register(id, all);
    }

    public static void saveCache(String id, byte[] png) {
        try {
            Files.write(FotosClient.dir("cache").resolve(id + ".png"), png);
        } catch (IOException ignored) { }
    }

    /** Debe llamarse en el hilo del juego. */
    public static void register(String id, byte[] png) {
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
            int w = img.getWidth(), h = img.getHeight();
            Identifier tex = MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture("fotos_" + id.replace('-', '_'), new NativeImageBackedTexture(img));
            READY.put(id, new Entry(tex, w, h));
        } catch (Exception ignored) { }
    }
}
