package net.snowless.foundcamera.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.snowless.foundcamera.camcorder.CamcorderState;
import net.snowless.foundcamera.camcorder.RecState;

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

        drawVhsEffects(g, w, h, ms, rec, ir);
        drawCorners(g, w, h);

        if (CamcorderState.isShowText()) {
            drawTexts(g, font, w, h, ms, rec, ir);
        }
    }

    // ------------------------------------------------------------ efeitos VHS
    private static void drawVhsEffects(GuiGraphics g, int w, int h, long ms, RecState rec, boolean ir) {
        RandomSource rng = RandomSource.create(ms / 70L); // "treme" ~14x por segundo
        float noise = (rec == RecState.PAUSE ? 2.5f : 1f) * (ir ? 1.5f : 1f);

        // escurece em STOP
        if (rec == RecState.STOP) g.fill(0, 0, w, h, 0x66000000);

        // tinta verde do infravermelho
        if (ir) g.fill(0, 0, w, h, 0x3800FF44);

        // scanlines
        for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, 0x22000000);

        // faixa de tracking rolando
        int bandY = (int) ((ms / 6L) % (h + 200)) - 100;
        g.fill(0, bandY, w, bandY + 16, 0x18FFFFFF);
        g.fill(0, bandY + 16, w, bandY + 18, 0x30FFFFFF);

        // chuvisco
        int dots = (int) (45 * noise);
        for (int i = 0; i < dots; i++) {
            int x = rng.nextInt(w), y = rng.nextInt(h);
            g.fill(x, y, x + 1 + rng.nextInt(3), y + 1, 0x33FFFFFF);
        }

        // glitch horizontal ocasional
        if (rng.nextFloat() < 0.06f * noise) {
            int gy = rng.nextInt(h), gx = rng.nextInt(Math.max(1, w / 2));
            g.fill(gx, gy, gx + w / 3 + rng.nextInt(Math.max(1, w / 3)), gy + 1 + rng.nextInt(3), 0x40FFFFFF);
        }

        // vinheta
        int steps = 28;
        for (int i = 0; i < steps; i++) {
            int a = (int) (0x50 * Math.pow(1f - i / (float) steps, 2));
            int col = a << 24;
            g.fill(i * 3, 0, i * 3 + 3, h, col);
            g.fill(w - i * 3 - 3, 0, w - i * 3, h, col);
        }
        g.fillGradient(0, 0, w, h / 5, 0x66000000, 0x00000000);
        g.fillGradient(0, h - h / 5, w, h, 0x00000000, 0x66000000);
    }

    private static void drawCorners(GuiGraphics g, int w, int h) {
        int m = 14, len = 24, t = 2, c = 0xCCFFFFFF;
        // topo-esq
        g.fill(m, m, m + len, m + t, c);            g.fill(m, m, m + t, m + len, c);
        // topo-dir
        g.fill(w - m - len, m, w - m, m + t, c);    g.fill(w - m - t, m, w - m, m + len, c);
        // baixo-esq
        g.fill(m, h - m - t, m + len, h - m, c);    g.fill(m, h - m - len, m + t, h - m, c);
        // baixo-dir
        g.fill(w - m - len, h - m - t, w - m, h - m, c); g.fill(w - m - t, h - m - len, w - m, h - m, c);
    }

    // ------------------------------------------------------------ textos
    private static void drawTexts(GuiGraphics g, Font font, int w, int h, long ms, RecState rec, boolean ir) {
        int margin = 26;

        // estado (topo-esquerda)
        String label;
        int color = 0xFFFFFFFF;
        switch (rec) {
            case PLAY -> label = "> PLAY";
            case PAUSE -> { label = "|| PAUSE"; color = 0xFFFFE070; }
            default -> { label = "[] STOP"; color = 0xFFCCCCCC; }
        }
        if (rec == RecState.PLAY && (ms / 500L) % 2 == 0) {
            g.fill(margin, margin + 2, margin + 8, margin + 10, 0xFFFF2020); // bolinha REC piscando
        }
        vhsText(g, font, label, margin + 14, margin, color, 2f);

        // timecode (topo-direita)
        long secs = CamcorderState.getRecTicks() / 20L;
        String tc = String.format("%02d:%02d:%02d", secs / 3600, (secs / 60) % 60, secs % 60);
        float tcScale = 2f;
        int tcX = w - margin - (int) (font.width(tc) * tcScale);
        vhsText(g, font, tc, tcX, margin, 0xFFFFFFFF, tcScale);

        // data/hora (baixo-esquerda)
        String date = LocalDateTime.now().format(DATE_FMT).toUpperCase(Locale.ROOT);
        vhsText(g, font, date, margin, h - margin - 8, 0xFFFFFFFF, 1f);

        // bateria (baixo-direita)
        String batt = "BATT [|||]";
        vhsText(g, font, batt, w - margin - font.width(batt), h - margin - 8, 0xFFFFFFFF, 1f);

        // infravermelho (topo-centro)
        if (ir) {
            String s = "NIGHT SHOT";
            vhsText(g, font, s, (w - font.width(s)) / 2, margin, 0xFF66FF88, 1f);
        }
    }

    /** Texto com "aberração cromática" barata: cópias ciano/vermelha deslocadas. */
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
