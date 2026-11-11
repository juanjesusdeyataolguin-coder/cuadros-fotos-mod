package com.fraxxz.fotos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class FotosClient implements ClientModInitializer {
    public static Path dir(String name) {
        Path p = FabricLoader.getInstance().getGameDir().resolve("fotos").resolve(name);
        try { Files.createDirectories(p); } catch (IOException ignored) { }
        return p;
    }

    @Override
    public void onInitializeClient() {
        dir("camara");
        dir("galeria");
        dir("cache");

        BlockEntityRendererFactories.register(FotosMod.FRAME_BE, PhotoFrameRenderer::new);
        CameraItem.CLIENT_HOOK = CameraClient::trigger;
        ClientTickEvents.END_CLIENT_TICK.register(CameraClient::tick);

        ClientPlayNetworking.registerGlobalReceiver(FotosMod.OPEN_FRAME, (client, handler, buf, sender) -> {
            BlockPos p = buf.readBlockPos();
            client.execute(() -> client.setScreen(new FrameScreen(p)));
        });
        ClientPlayNetworking.registerGlobalReceiver(FotosMod.PHOTO, (client, handler, buf, sender) -> {
            String id = buf.readString(40);
            int i = buf.readVarInt();
            int t = buf.readVarInt();
            byte[] d = buf.readByteArray(FotosMod.CHUNK + 10);
            client.execute(() -> PhotoCache.onChunk(id, i, t, d));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            client.inGameHud.setTitleTicks(10, 70, 20);
            client.inGameHud.setSubtitle(Text.literal("§eCUADROS DE FOTOS"));
            client.inGameHud.setTitle(Text.literal("§6MOD BY FRAXXZ"));
        }));
    }
}
