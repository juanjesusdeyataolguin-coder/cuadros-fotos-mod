package com.fraxxz.fotos;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.texture.NativeImage;

public final class ImageUtil {
    public static final int MAX_SIDE = 512;

    /** Lee cualquier imagen (png/jpg/gif/bmp), la reduce a 512 px máx. y la devuelve como PNG. null si falla. */
    public static byte[] normalize(byte[] raw) {
        int w = 0, h = 0;
        int[] px = null;
        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(raw));
            if (bi != null) {
                w = bi.getWidth();
                h = bi.getHeight();
                px = bi.getRGB(0, 0, w, h, null, 0, w);
            }
        } catch (Throwable ignored) { }
        if (px == null) {
            try (NativeImage ni = NativeImage.read(new ByteArrayInputStream(raw))) {
                w = ni.getWidth();
                h = ni.getHeight();
                px = new int[w * h];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int c = ni.getColor(x, y); // ABGR
                        int a = (c >>> 24) & 255, b = (c >>> 16) & 255, g = (c >>> 8) & 255, r = c & 255;
                        px[y * w + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
            } catch (Throwable t) {
                return null;
            }
        }
        if (w < 1 || h < 1) return null;
        double sc = Math.max(w, h) / (double) MAX_SIDE;
        if (sc < 1) sc = 1;
        int nw = Math.max(1, (int) Math.round(w / sc));
        int nh = Math.max(1, (int) Math.round(h / sc));
        try (NativeImage out = new NativeImage(NativeImage.Format.RGBA, nw, nh, false)) {
            for (int yy = 0; yy < nh; yy++) {
                int y0 = Math.min(h - 1, (int) (yy * sc));
                int y1 = Math.min(h, Math.max(y0 + 1, (int) ((yy + 1) * sc)));
                int sy = Math.max(1, (y1 - y0) / 4);
                for (int xx = 0; xx < nw; xx++) {
                    int x0 = Math.min(w - 1, (int) (xx * sc));
                    int x1 = Math.min(w, Math.max(x0 + 1, (int) ((xx + 1) * sc)));
                    int sx = Math.max(1, (x1 - x0) / 4);
                    long sa = 0, sr = 0, sg = 0, sb = 0;
                    int n = 0;
                    for (int py = y0; py < y1; py += sy) {
                        for (int pxx = x0; pxx < x1; pxx += sx) {
                            int c = px[py * w + pxx];
                            sa += (c >>> 24) & 255;
                            sr += (c >> 16) & 255;
                            sg += (c >> 8) & 255;
                            sb += c & 255;
                            n++;
                        }
                    }
                    if (n == 0) n = 1;
                    int a = (int) (sa / n), r = (int) (sr / n), g = (int) (sg / n), b = (int) (sb / n);
                    out.setColor(xx, yy, (a << 24) | (b << 16) | (g << 8) | r);
                }
            }
            return out.asByteArray();
        } catch (Throwable t) {
            return null;
        }
    }
}
