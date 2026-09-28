package net.snowless.foundcamera.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.snowless.foundcamera.camcorder.CamcorderState;
import net.snowless.foundcamera.config.ModConfig;
import net.snowless.foundcamera.registry.ModItems;

@Mod.EventBusSubscriber(modid = ModItems.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraZoom {
    private static float currentZoom = 1f;
    private static boolean init;
    private static long lastNanos;

    private CameraZoom() {}

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (!CamcorderState.isActive() || mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            currentZoom = 1f;
            init = false;
            return;
        }

        long now = System.nanoTime();
        float dt = init ? Mth.clamp((now - lastNanos) / 1.0e9f, 0f, 0.1f) : 0f;
        lastNanos = now;
        init = true;

        float target = CamcorderState.getZoomLevel();
        float speed = ModConfig.get().zoomSpeed;
        float k = 1f - (float) Math.exp(-dt * speed);
        currentZoom += (target - currentZoom) * k;
        event.setFOV(event.getFOV() / currentZoom);
    }
}