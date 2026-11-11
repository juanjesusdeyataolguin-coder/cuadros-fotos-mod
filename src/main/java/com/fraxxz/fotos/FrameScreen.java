package com.fraxxz.fotos;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class FrameScreen extends Screen {
    private enum Mode { MAIN, GAME, GALLERY, LINK }

    private final BlockPos pos;
    private Mode mode = Mode.MAIN;
    private int page = 0;
    private File[] files = new File[0];
    private TextFieldWidget linkField;
    private int topY;

    public FrameScreen(BlockPos pos) {
        super(Text.literal("Cuadro personalizado"));
        this.pos = pos;
    }

    private void btn(String text, int x, int y, int w, Runnable r) {
        addDrawableChild(ButtonWidget.builder(Text.literal(text), b -> r.run()).dimensions(x, y, w, 20).build());
    }

    private void go(Mode m) { mode = m; page = 0; init(); }

    private void loadFiles() {
        File dir = FotosClient.dir(mode == Mode.GAME ? "camara" : "galeria").toFile();
        File[] arr = dir.listFiles(f -> {
            String n = f.getName().toLowerCase();
            return f.isFile() && (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                || n.endsWith(".gif") || n.endsWith(".bmp"));
        });
        files = arr == null ? new File[0] : arr;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
    }

    @Override
    protected void init() {
        clearChildren();
        int x = width / 2 - 100;
        int y = Math.max(30, height / 2 - 70);
        topY = y;
        switch (mode) {
            case MAIN -> {
                btn("Fotos del juego (cámara)", x, y, 200, () -> go(Mode.GAME));
                btn("Foto por link", x, y + 24, 200, () -> go(Mode.LINK));
                btn("Foto de mi galería", x, y + 48, 200, () -> go(Mode.GALLERY));
                btn("Quitar foto", x, y + 72, 200, () -> { FotoUploader.clear(pos); close(); });
                btn("Cerrar", x, y + 96, 200, this::close);
            }
            case GAME, GALLERY -> {
                loadFiles();
                int per = 4;
                int maxPage = Math.max(0, (files.length - 1) / per);
                if (page > maxPage) page = maxPage;
                for (int i = 0; i < per; i++) {
                    int idx = page * per + i;
                    if (idx >= files.length) break;
                    File f = files[idx];
                    String n = f.getName();
                    if (n.length() > 30) n = n.substring(0, 27) + "...";
                    btn(n, x, y + i * 24, 200, () -> choose(f));
                }
                int ny = y + per * 24;
                btn("<<", x, ny, 60, () -> { if (page > 0) { page--; init(); } });
                btn("Atrás", x + 64, ny, 72, () -> go(Mode.MAIN));
                btn(">>", x + 140, ny, 60, () -> { if (page < maxPage) { page++; init(); } });
            }
            case LINK -> {
                linkField = new TextFieldWidget(textRenderer, x, y + 24, 200, 20, Text.literal("Link"));
                linkField.setMaxLength(500);
                linkField.setSuggestion("Pega el link de la imagen");
                linkField.setChangedListener(s -> linkField.setSuggestion(s.isEmpty() ? "Pega el link de la imagen" : ""));
                addDrawableChild(linkField);
                setInitialFocus(linkField);
                btn("Usar link", x, y + 52, 200, this::useLink);
                btn("Atrás", x, y + 76, 200, () -> go(Mode.MAIN));
            }
        }
    }

    private void choose(File f) {
        close();
        FotoUploader.msg("Subiendo foto...");
        new Thread(() -> {
            try {
                FotoUploader.process(pos, Files.readAllBytes(f.toPath()));
            } catch (Exception e) {
                FotoUploader.msg("§cNo pude leer el archivo");
            }
        }, "fotos-subir").start();
    }

    private void useLink() {
        String u = linkField.getText().trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            FotoUploader.msg("§cEl link debe empezar con http:// o https://");
            return;
        }
        close();
        FotoUploader.msg("Descargando imagen...");
        new Thread(() -> {
            try {
                FotoUploader.process(pos, download(u));
            } catch (Exception e) {
                FotoUploader.msg("§cNo pude descargar la imagen (usa un link directo a la foto)");
            }
        }, "fotos-link").start();
    }

    private static byte[] download(String url) throws IOException {
        String cur = url;
        for (int hop = 0; hop < 5; hop++) {
            HttpURLConnection c = (HttpURLConnection) new URL(cur).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = c.getHeaderField("Location");
                if (loc == null) throw new IOException("redirect");
                cur = new URL(new URL(cur), loc).toString();
                continue;
            }
            if (code != 200) throw new IOException("HTTP " + code);
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    o.write(buf, 0, n);
                    if (o.size() > 15_000_000) throw new IOException("muy grande");
                }
                return o.toByteArray();
            }
        }
        throw new IOException("demasiadas redirecciones");
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§6MOD BY FRAXXZ §f- Cuadro personalizado"), width / 2, topY - 14, 0xFFFFFF);
        if ((mode == Mode.GAME || mode == Mode.GALLERY) && files.length == 0) {
            String msg = mode == Mode.GAME
                ? "Aún no tomas fotos. Usa la Cámara."
                : "Copia tus fotos a la carpeta fotos/galeria";
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(msg), width / 2, topY + 4, 0xFFAA00);
            if (mode == Mode.GALLERY) {
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("(dentro de la carpeta del juego)"), width / 2, topY + 18, 0xAAAAAA);
            }
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
}
