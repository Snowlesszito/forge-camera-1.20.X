package net.snowless.foundcamera.camcorder;

/** Estado da filmadora (somente cliente). Java puro, seguro de referenciar do item. */
public final class CamcorderState {
    private static boolean active;
    private static boolean infrared;
    private static boolean showText = true;
    private static RecState rec = RecState.PLAY;
    private static long recTicks;

    private CamcorderState() {}

    public static boolean isActive() { return active; }
    public static boolean isInfrared() { return active && infrared; }
    public static boolean isShowText() { return showText; }
    public static RecState getRec() { return rec; }
    public static long getRecTicks() { return recTicks; }

    public static void toggleActive() {
        if (active) deactivate(); else activate();
    }

    public static void activate() {
        active = true;
        rec = RecState.PLAY; // ao levantar a câmera já começa em PLAY
    }

    public static void deactivate() {
        active = false;
        infrared = false;
        recTicks = 0;
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

    public static void tick() {
        if (active && rec == RecState.PLAY) recTicks++;
    }
}
