package net.snowless.foundcamera.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.snowless.foundcamera.camcorder.CamcorderState;
import net.snowless.foundcamera.registry.ModItems;

/** Balanço natural + arrasto (a câmera "atrasa" em relação ao mouse, simulando peso). */
@Mod.EventBusSubscriber(modid = ModItems.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraSway {
    // --- ajuste aqui ---
    private static final float FOLLOW_SPEED = 9.0f;   // menor = mais pesado / mais arrasto
    private static final float MAX_LAG_DEG = 12.0f;   // limite do atraso em graus
    private static final float ROLL_FROM_LAG = 0.12f; // inclinação lateral ao virar rápido
    private static final float IDLE_AMOUNT = 1.0f;    // intensidade do balanço parado

    private static boolean init;
    private static float lagYaw, lagPitch;
    private static long lastNanos;

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!CamcorderState.isActive() || mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
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

        // segue o alvo de forma exponencial e limita o atraso
        float k = 1f - (float) Math.exp(-dt * FOLLOW_SPEED);
        lagYaw += Mth.wrapDegrees(yaw - lagYaw) * k;
        lagPitch += (pitch - lagPitch) * k;

        float dYaw = Mth.clamp(Mth.wrapDegrees(lagYaw - yaw), -MAX_LAG_DEG, MAX_LAG_DEG);
        float dPitch = Mth.clamp(lagPitch - pitch, -MAX_LAG_DEG, MAX_LAG_DEG);
        lagYaw = yaw + dYaw;
        lagPitch = pitch + dPitch;

        // balanço "respirando"; aumenta um pouco andando
        float t = (float) ((now / 1.0e9) % 10000.0);
        double speed = mc.player.getDeltaMovement().horizontalDistance();
        float amp = IDLE_AMOUNT * (1f + (float) speed * 6f);

        float idleYaw = Mth.cos(t * 0.7f) * 0.25f * amp;
        float idlePitch = Mth.sin(t * 1.3f) * 0.18f * amp;
        float idleRoll = Mth.sin(t * 0.9f) * 0.35f * amp;

        event.setYaw(lagYaw + idleYaw);
        event.setPitch(Mth.clamp(lagPitch + idlePitch, -90f, 90f));
        event.setRoll(event.getRoll() + idleRoll + dYaw * ROLL_FROM_LAG);
    }
}
