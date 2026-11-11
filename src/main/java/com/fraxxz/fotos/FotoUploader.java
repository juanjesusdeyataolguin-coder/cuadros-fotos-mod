package com.fraxxz.fotos;

import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class FotoUploader {
    public static void msg(String s) {
        MinecraftClient c = MinecraftClient.getInstance();
        c.execute(() -> { if (c.player != null) c.player.sendMessage(Text.literal(s), true); });
    }

    /** Llamar desde un hilo de fondo: convierte la imagen, la sube al servidor y la pone en el cuadro. */
    public static void process(BlockPos pos, byte[] raw) {
        byte[] png = ImageUtil.normalize(raw);
        if (png == null) { msg("§cNo pude leer esa imagen"); return; }
        List<byte[]> parts = Chunker.split(png);
        if (parts.size() > 120) { msg("§cLa imagen es demasiado pesada"); return; }
        String id = UUID.randomUUID().toString();
        MinecraftClient c = MinecraftClient.getInstance();
        c.execute(() -> {
            PhotoCache.saveCache(id, png);
            PhotoCache.register(id, png);
            for (int i = 0; i < parts.size(); i++) {
                PacketByteBuf b = PacketByteBufs.create();
                b.writeString(id, 40);
                b.writeVarInt(i);
                b.writeVarInt(parts.size());
                b.writeByteArray(parts.get(i));
                ClientPlayNetworking.send(FotosMod.UPLOAD, b);
            }
            PacketByteBuf s = PacketByteBufs.create();
            s.writeBlockPos(pos);
            s.writeString(id, 40);
            ClientPlayNetworking.send(FotosMod.SET_FRAME, s);
            if (c.player != null) c.player.sendMessage(Text.literal("§aFoto enviada al cuadro"), true);
        });
    }

    public static void clear(BlockPos pos) {
        PacketByteBuf s = PacketByteBufs.create();
        s.writeBlockPos(pos);
        s.writeString("", 40);
        ClientPlayNetworking.send(FotosMod.SET_FRAME, s);
    }
}
