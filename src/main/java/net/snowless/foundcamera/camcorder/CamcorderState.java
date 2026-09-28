package net.snowless.foundcamera.camcorder;

import net.minecraft.util.Mth;
import net.snowless.foundcamera.config.ModConfig;

public final class CamcorderState {
    private static boolean active;
    private static boolean infrared;
    private static boolean showText = true;
    private static RecState rec = RecState.PLAY;
    private static long recTicks;
    private static float zoomLevel = 1f;

    private static boolean facingSelf;
    private static float flipProgress = -1f;
    private static boolean flipToSelf;
    private static boolean fearShake;

    private CamcorderState() {}

    public static boolean isActive() { return active; }
    public static boolean isInfrared() { return active && infrared; }
    public static boolean isShowText() { return showText; }
    public static RecState getRec() { return rec; }
    public static long getRecTicks() { return recTicks; }
    public static float getZoomLevel() { return zoomLevel; }
    public static boolean isFacingSelf() { return facingSelf; }
    public static boolean isFlipping() { return flipProgress >= 0f; }
    public static float getFlipProgress() { return Math.max(0f, flipProgress); }
    public static boolean isFlipToSelf() { return flipToSelf; }
    public static boolean isFearShake() { return active && fearShake; }

    public static void toggleActive() {
        if (active) deactivate();
        else activate();
    }

    public static void activate() {
        active = true;
        rec = RecState.PLAY;
        zoomLevel = 1f;
        facingSelf = false;
        flipProgress = -1f;
        fearShake = false;
    }

    public static void deactivate() {
        active = false;
        infrared = false;
        recTicks = 0;
        zoomLevel = 1f;
        facingSelf = false;
        flipProgress = -1f;
        fearShake = false;
    }

    public static void togglePlayPause() {
        if (!active) return;
        rec = (rec == RecState.PLAY) ? RecState.PAUSE : RecState.PLAY;
    }

    public static void stop() {
        if (!active) return;
        rec = RecState.STOP;
        recTicks = 0;
    }

    public static void toggleInfrared() { if (active) infrared = !infrared; }
    public static void toggleText() { showText = !showText; }

    public static void toggleFearShake() {
        if (active) fearShake = !fearShake;
    }

    public static void toggleFlip() {
        if (!active || isFlipping()) return;
        flipToSelf = !facingSelf;
        flipProgress = 0f;
    }

    public static void addZoom(float delta) {
        if (!active) return;
        zoomLevel = Mth.clamp(zoomLevel + delta, 1f, ModConfig.get().maxZoom);
    }

    public static void tick() {
        if (!active) return;
        if (rec == RecState.PLAY) recTicks++;

        if (flipProgress >= 0f) {
            float dur = Math.max(0.1f, ModConfig.get().flipDurationSec);
            flipProgress += 1f / (dur * 20f);
            if (flipProgress >= 1f) {
                flipProgress = -1f;
                facingSelf = flipToSelf;
            }
        }
    }
}