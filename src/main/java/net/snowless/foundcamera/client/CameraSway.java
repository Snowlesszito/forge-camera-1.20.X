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
public final class CameraSway {
    private static final float MAX_LAG_DEG = 12.0f;
    private static final float ROLL_FROM_LAG = 0.12f;

    private static boolean init;
    private static float lagYaw, lagPitch;
    private static long lastNanos;

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        ModConfig.Data cfg = ModConfig.get();
        if (!cfg.enableCameraSway
                || !CamcorderState.isActive()
                || mc.player == null
                || !mc.options.getCameraType().isFirstPerson()) {
            init = false;
            return;
        }

        long now = System.nanoTime();
        float dt = init ? Mth.clamp((now - lastNanos) / 1.0e9f, 0f, 0.1f) : 0f;
        lastNanos = now;

        float yaw = event.getYaw();
        float pitch = event.getPitch();
        if (!init) {
            lagYaw = yaw;
            lagPitch = pitch;
            init = true;
        }

        float k = 1f - (float) Math.exp(-dt * cfg.followSpeed);
        lagYaw += Mth.wrapDegrees(yaw - lagYaw) * k;
        lagPitch += (pitch - lagPitch) * k;

        float dYaw = Mth.clamp(Mth.wrapDegrees(lagYaw - yaw), -MAX_LAG_DEG, MAX_LAG_DEG);
        float dPitch = Mth.clamp(lagPitch - pitch, -MAX_LAG_DEG, MAX_LAG_DEG);
        lagYaw = yaw + dYaw;
        lagPitch = pitch + dPitch;

        float t = (float) ((now / 1.0e9) % 10000.0);
        double speed = mc.player.getDeltaMovement().horizontalDistance();
        float amp = cfg.swayAmount * (1f + (float) speed * 6f);

        event.setYaw(lagYaw + Mth.cos(t * 0.7f) * 0.25f * amp);
        event.setPitch(Mth.clamp(lagPitch + Mth.sin(t * 1.3f) * 0.18f * amp, -90f, 90f));
        event.setRoll(event.getRoll() + Mth.sin(t * 0.9f) * 0.35f * amp + dYaw * ROLL_FROM_LAG);
    }
}