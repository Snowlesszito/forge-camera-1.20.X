package net.snowless.foundcamera.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.snowless.foundcamera.camcorder.CamcorderState;
import net.snowless.foundcamera.registry.ModItems;

@Mod.EventBusSubscriber(modid = ModItems.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraFlip {
    private CameraFlip() {}

    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!CamcorderState.isActive() || !CamcorderState.isFlipping()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float t = ease(CamcorderState.getFlipProgress());
        event.setPitch(Mth.clamp(event.getPitch() - Mth.sin(t * Mth.PI) * 12f, -90f, 90f));
        event.setYaw(event.getYaw() + Mth.sin(t * Mth.PI * 2f) * 4f);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (!CamcorderState.isActive() || !CamcorderState.isFlipping()) return;
        float t = CamcorderState.getFlipProgress();
        event.setFOV(event.getFOV() + Mth.sin(t * Mth.PI) * 8f);
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }
}