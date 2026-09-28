package net.snowless.foundcamera.client;

import net.minecraft.client.CameraType;
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
public final class CameraFearShake {
    private CameraFearShake() {}

    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!CamcorderState.isFearShake()) return;
        if (!ModConfig.get().fearShakeEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        CameraType type = mc.options.getCameraType();
        if (!type.isFirstPerson() && type != CameraType.THIRD_PERSON_FRONT) return;

        ModConfig.Data cfg = ModConfig.get();
        float intensity = cfg.fearShakeIntensity;
        float speed = cfg.fearShakeSpeed;
        float t = (System.nanoTime() / 1_000_000_000f) * speed;

        float yawShake = Mth.sin(t * 1.7f) * 0.35f + Mth.sin(t * 3.1f) * 0.20f + Mth.sin(t * 8.4f) * 0.10f;
        float pitchShake = Mth.cos(t * 1.9f) * 0.30f + Mth.sin(t * 4.2f) * 0.18f + Mth.cos(t * 9.1f) * 0.08f;
        float rollShake = Mth.sin(t * 2.3f) * 0.25f + Mth.cos(t * 5.5f) * 0.12f;

        event.setYaw(event.getYaw() + yawShake * intensity);
        event.setPitch(Mth.clamp(event.getPitch() + pitchShake * intensity, -90f, 90f));
        event.setRoll(event.getRoll() + rollShake * intensity * 0.8f);
    }
}