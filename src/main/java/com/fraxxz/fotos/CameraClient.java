package com.fraxxz.fotos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.sound.SoundEvents;

public final class CameraClient {
    private static int countdown = -1;
    private static boolean prevHud;

    public static void trigger() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (countdown >= 0 || c.player == null) return;
        prevHud = c.options.hudHidden;
        c.options.hudHidden = true;
        c.gameRenderer.setRenderHand(false);
        countdown = 3;
    }

    public static void tick(MinecraftClient c) {
        if (countdown < 0) return;
        if (--countdown > 0) return;
        countdown = -1;

        NativeImage shot = ScreenshotRecorder.takeScreenshot(c.getFramebuffer());
        c.options.hudHidden = prevHud;
        c.gameRenderer.setRenderHand(true);
        byte[] raw = null;
        try {
            raw = shot.asByteArray();
        } catch (IOException ignored) {
        } finally {
            shot.close();
        }
        if (raw == null) return;
        c.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f));
        final byte[] data = raw;
        new Thread(() -> {
            byte[] png = ImageUtil.normalize(data);
            if (png == null) { FotoUploader.msg("§cNo pude guardar la foto"); return; }
            try {
                String name = "foto_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".png";
                Path f = FotosClient.dir("camara").resolve(name);
                Files.write(f, png);
                FotoUploader.msg("§aFoto guardada: " + name);
            } catch (IOException e) {
                FotoUploader.msg("§cNo pude guardar la foto");
            }
        }, "fotos-camara").start();
    }
}
