package net.snowless.foundcamera.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.snowless.foundcamera.camcorder.CamcorderState;
import net.snowless.foundcamera.camcorder.RecState;
import net.snowless.foundcamera.config.ModConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CamcorderHud implements IGuiOverlay {
    public static final String ID = "camcorder";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd yyyy   HH:mm:ss", Locale.ENGLISH);

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        if (!CamcorderState.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        long ms = Util.getMillis();
        RecState rec = CamcorderState.getRec();
        boolean ir = CamcorderState.isInfrared();
        ModConfig.Data cfg = ModConfig.get();

        drawColorGrade(g, w, h, cfg, ir);
        drawGrainAndLines(g, w, h, ms, rec, ir, cfg);
        if (cfg.enableVignette) drawVignette(g, w, h, cfg, ir);
        drawCorners(g, w, h);
        if (CamcorderState.isShowText()) drawTexts(g, font, w, h, ms, rec, ir);
    }

    private static void drawColorGrade(GuiGraphics g, int w, int h, ModConfig.Data cfg, boolean ir) {
        if (ir) {
            int darkA = (int) Mth.clamp(0x40 * cfg.infraredContrast, 0x20, 0x70);
            g.fill(0, 0, w, h, (darkA << 24));
            int greenA = (int) Mth.clamp(0x55 * cfg.infraredTint, 0x28, 0x80);
            g.fill(0, 0, w, h, (greenA << 24) | 0x002211);
            int veilA = (int) Mth.clamp(0x28 * cfg.infraredTint, 0x10, 0x40);
            g.fill(0, 0, w, h, (veilA << 24) | 0x004433);
        } else if (cfg.desaturation > 0.01f) {
            int a = (int) Mth.clamp(0x55 * cfg.desaturation, 0, 0x70);
            g.fill(0, 0, w, h, (a << 24) | 0x808080);
        }
        if (cfg.contrast > 1.01f && !ir) {
            float t = Mth.clamp(cfg.contrast - 1f, 0f, 1f);
            g.fill(0, 0, w, h, ((int) (0x28 * t)) << 24);
        }
    }

    private static void drawGrainAndLines(GuiGraphics g, int w, int h, long ms, RecState rec, boolean ir, ModConfig.Data cfg) {
        if (rec == RecState.STOP) g.fill(0, 0, w, h, 0x66000000);

        RandomSource rng = RandomSource.create(ms / 50L);
        float noiseMul = (rec == RecState.PAUSE ? 1.8f : 1f) * (ir ? 1.35f : 1f) * cfg.grainStrength;

        if (cfg.enableGrain && cfg.grainStrength > 0.01f) {
            int dots = (int) ((ir ? 110 : 70) * noiseMul);
            for (int i = 0; i < dots; i++) {
                int x = rng.nextInt(Math.max(1, w));
                int y = rng.nextInt(Math.max(1, h));
                int s = 1 + rng.nextInt(2);
                int col = ir
                        ? (rng.nextBoolean() ? 0x28A0FFA0 : 0x22001008)
                        : (rng.nextBoolean() ? 0x22FFFFFF : 0x18000000);
                g.fill(x, y, x + s, y + s, col);
            }
            if (rng.nextFloat() < 0.12f * noiseMul) {
                int gy = rng.nextInt(Math.max(1, h));
                g.fill(0, gy, w, gy + 1, ir ? 0x18A0FF90 : 0x14FFFFFF);
            }
        }

        if (cfg.enableScanlines) {
            for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, 0x18000000);
        }
        if (cfg.enableTrackingBand) {
            int bandY = (int) ((ms / 6L) % (h + 200)) - 100;
            g.fill(0, bandY, w, bandY + 16, 0x18FFFFFF);
            g.fill(0, bandY + 16, w, bandY + 18, 0x30FFFFFF);
        }
    }

    private static void drawVignette(GuiGraphics g, int w, int h, ModConfig.Data cfg, boolean ir) {
        float strength = cfg.vignetteStrength * (ir ? cfg.infraredEdgeDarkness : 1f);
        int steps = (int) Mth.clamp(30 * strength, 10, 55);
        for (int i = 0; i < steps; i++) {
            float t = 1f - i / (float) steps;
            int a = Mth.clamp((int) (0x70 * t * t * strength), 0, 0xA0);
            int col = a << 24;
            int m = i * 3;
            g.fill(m, 0, m + 3, h, col);
            g.fill(w - m - 3, 0, w - m, h, col);
            g.fill(0, m, w, m + 3, col);
            g.fill(0, h - m - 3, w, h - m, col);
        }
        if (ir) {
            int edge = (int) Mth.clamp(0x90 * cfg.infraredEdgeDarkness, 0x40, 0xC0);
            g.fillGradient(0, 0, w, h / 4, (edge << 24), 0x00000000);
            g.fillGradient(0, h - h / 4, w, h, 0x00000000, (edge << 24));
        } else {
            g.fillGradient(0, 0, w, h / 6, 0x44000000, 0x00000000);
            g.fillGradient(0, h - h / 6, w, h, 0x00000000, 0x44000000);
        }
    }

    private static void drawCorners(GuiGraphics g, int w, int h) {
        int m = 14, len = 24, t = 2, c = 0xCCFFFFFF;
        g.fill(m, m, m + len, m + t, c); g.fill(m, m, m + t, m + len, c);
        g.fill(w - m - len, m, w - m, m + t, c); g.fill(w - m - t, m, w - m, m + len, c);
        g.fill(m, h - m - t, m + len, h - m, c); g.fill(m, h - m - len, m + t, h - m, c);
        g.fill(w - m - len, h - m - t, w - m, h - m, c); g.fill(w - m - t, h - m - len, w - m, h - m, c);
    }

    private static void drawTexts(GuiGraphics g, Font font, int w, int h, long ms, RecState rec, boolean ir) {
        int margin = 26;
        String label;
        int color = 0xFFFFFFFF;
        switch (rec) {
            case PLAY -> label = "> PLAY";
            case PAUSE -> { label = "|| PAUSE"; color = 0xFFFFE070; }
            default -> { label = "[] STOP"; color = 0xFFCCCCCC; }
        }
        if (rec == RecState.PLAY && (ms / 500L) % 2 == 0) {
            g.fill(margin, margin + 2, margin + 8, margin + 10, 0xFFFF2020);
        }
        vhsText(g, font, label, margin + 14, margin, color, 2f);

        long secs = CamcorderState.getRecTicks() / 20L;
        String tc = String.format("%02d:%02d:%02d", secs / 3600, (secs / 60) % 60, secs % 60);
        float tcScale = 2f;
        int tcX = w - margin - (int) (font.width(tc) * tcScale);
        vhsText(g, font, tc, tcX, margin, 0xFFFFFFFF, tcScale);

        float z = CamcorderState.getZoomLevel();
        if (z > 1.01f) {
            String zs = String.format("x%.1f", z);
            vhsText(g, font, zs, w - margin - font.width(zs), margin + 22, 0xFFAAFFAA, 1f);
        }

        int line = margin + 22;
        if (CamcorderState.isFacingSelf() || CamcorderState.isFlipping()) {
            String face = CamcorderState.isFlipping() ? "FLIP..." : "FRONT";
            vhsText(g, font, face, margin, line, 0xFFFFCC88, 1f);
            line += 12;
        }
        if (CamcorderState.isFearShake()) {
            vhsText(g, font, "SHAKE", margin, line, 0xFFFF6666, 1f);
        }

        String date = LocalDateTime.now().format(DATE_FMT).toUpperCase(Locale.ROOT);
        vhsText(g, font, date, margin, h - margin - 8, 0xFFFFFFFF, 1f);
        String batt = "BATT [|||]";
        vhsText(g, font, batt, w - margin - font.width(batt), h - margin - 8, 0xFFFFFFFF, 1f);

        if (ir) {
            String s = "NIGHT SHOT";
            vhsText(g, font, s, (w - font.width(s)) / 2, margin, 0xFF66FF88, 1f);
        }
    }

    private static void vhsText(GuiGraphics g, Font font, String s, int x, int y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, s, -1, 0, 0x9900FFFF, false);
        g.drawString(font, s, 1, 0, 0x99FF0040, false);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }
}