package net.snowless.foundcamera.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.snowless.foundcamera.camcorder.CamcorderState;
import net.snowless.foundcamera.config.ModConfig;
import net.snowless.foundcamera.registry.ModItems;
import org.slf4j.Logger;

/**
 * Controle da câmera enquanto a filmadora está ativa.
 *
 * Flip (facingSelf):
 *  - força terceira pessoa frontal e puxa a câmera para ~0.75 do rosto (como arm-selfie-v2)
 *  - durante a animação de flip, faz órbita suave até a posição final
 *
 * Primeira pessoa:
 *  - mão/item já escondidos em ClientForgeEvents (RenderHandEvent)
 *  - offset leve para frente/baixo para parecer que a câmera está na mão, não na cabeça
 *  - holdPoint para o braço procedural (se ativo)
 */
@Mod.EventBusSubscriber(modid = ModItems.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CamcorderCamera {
    private static final Logger LOGGER = LogUtils.getLogger();

    // ================= selfie / flip próximo do corpo =================
    /** Distância final do rosto até a câmera (braço alcança ~0.7). */
    private static final float DISTANCE = 0.75f;
    /** 1 = órbita pela direita, -1 = pela esquerda. */
    private static final float ORBIT_SIDE = 1.0f;
    /** Inclinação lateral no meio da virada (graus). */
    private static final float SWING_ROLL_DEG = 7.0f;
    private static final float PIVOT_Y_OFFSET = 0.0f;
    private static final float HEAD_HIDE_DIST = 0.5f;
    /** Em FP / 3ª por trás: ponto de mão na frente do rosto. */
    private static final float HOLD_FORWARD = 0.5f;

    // ================= FP: visão nasce na filmadora (mão) =================
    /**
     * Alcance do braço a partir dos olhos até a filmadora.
     * ~0.45-0.55 = antebraço esticado segurando câmera na frente do peito.
     */
    private static final float FP_ARM_REACH = 0.48f;
    /** Queda vertical da mão em relação ao olho (ombro → mão). */
    private static final float FP_ARM_DROP = 0.28f;
    /** Lateral da mão principal a partir do centro do peito. */
    private static final float FP_ARM_SIDE = 0.22f;
    /**
     * A câmera fica um pouco atrás da ponta da mão (em direção ao ombro),
     * para o antebraço aparecer na borda inferior da tela.
     */
    private static final float FP_CAM_BACK_FROM_HAND = 0.08f;

    // ================= tremor / peso =================
    private static final float FOLLOW_SPEED = 8.0f;
    private static final float MAX_LAG_DEG = 10.0f;
    private static final float ROLL_FROM_LAG = 0.15f;
    private static final float SHAKE_YAW = 1.2f;
    private static final float SHAKE_PITCH = 0.9f;
    private static final float SHAKE_ROLL = 1.6f;
    private static final float WALK_SHAKE = 6.0f;
    private static final float STEP_BOB_DEG = 1.0f;
    private static final float SHAKE_SPEED = 1.0f;

    private static boolean hideHead;
    private static Vec3 holdPoint;
    private static boolean loggedFirstRun;
    private static CameraType lastLoggedType;

    private static boolean swayInit;
    private static float lagYaw, lagPitch;
    private static long lastNanos;

    private CamcorderCamera() {}

    private static boolean isFilming() {
        return CamcorderState.isActive();
    }

    public static boolean isProceduralArm() {
        // Em FP o FirstPersonBody controla o braço; o layer procedural é só 3ª pessoa (flip/selfie).
        Minecraft mc = Minecraft.getInstance();
        return isFilming() && holdPoint != null
                && mc.options.getCameraType() != null
                && !mc.options.getCameraType().isFirstPerson();
    }

    public static Vec3 getHoldPoint() {
        return holdPoint;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || !isFilming()) {
            hideHead = false;
            holdPoint = null;
            loggedFirstRun = false;
            lastLoggedType = null;
            swayInit = false;
            return;
        }

        // Tipo de câmera: ClientForgeEvents já força FP ou THIRD_PERSON_FRONT conforme facingSelf.
        // Aqui só garantimos que, durante o flip para self, estejamos em alguma 3ª pessoa.
        if (CamcorderState.isFlipping() && CamcorderState.isFlipToSelf()) {
            CameraType t = mc.options.getCameraType();
            if (t.isFirstPerson()) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !isFilming()) {
            swayInit = false;
            hideHead = false;
            holdPoint = null;
            return;
        }

        CameraType type = mc.options.getCameraType();
        if (!loggedFirstRun) {
            loggedFirstRun = true;
            LOGGER.info("[foundcamera] controle de camera ATIVO (tremor/flip-perto/braco)");
        }
        if (type != lastLoggedType) {
            lastLoggedType = type;
            LOGGER.info("[foundcamera] tipo de camera: {}", type);
        }

        long now = System.nanoTime();
        float dt = swayInit ? Mth.clamp((now - lastNanos) / 1.0e9f, 0f, 0.1f) : 0f;
        lastNanos = now;

        float yaw = event.getYaw();
        float pitch = event.getPitch();
        if (!swayInit) {
            lagYaw = yaw;
            lagPitch = pitch;
            swayInit = true;
        }

        // --- arrasto (peso)
        float follow = ModConfig.get().enableCameraSway ? FOLLOW_SPEED : 40f;
        float k = 1f - (float) Math.exp(-dt * follow);
        lagYaw += Mth.wrapDegrees(yaw - lagYaw) * k;
        lagPitch += (pitch - lagPitch) * k;
        float dYaw = Mth.clamp(Mth.wrapDegrees(lagYaw - yaw), -MAX_LAG_DEG, MAX_LAG_DEG);
        float dPitch = Mth.clamp(lagPitch - pitch, -MAX_LAG_DEG, MAX_LAG_DEG);
        lagYaw = yaw + dYaw;
        lagPitch = pitch + dPitch;

        // --- tremor de mão
        float time = (float) ((now / 1.0e9) % 3600.0) * SHAKE_SPEED;
        double speed = player.getDeltaMovement().horizontalDistance();
        float amp = 1f + (float) Math.min(speed, 0.3) * WALK_SHAKE;
        float partial = (float) event.getPartialTick();
        float walk = Mth.lerp(partial, player.walkDistO, player.walkDist);
        float stepBob = Mth.sin(walk * Mth.PI) * (float) Math.min(speed * 8.0, 1.0) * STEP_BOB_DEG;

        float shakeYaw = wobble(time, 0f) * SHAKE_YAW * amp * ModConfig.get().swayAmount;
        float shakePitch = wobble(time, 10f) * SHAKE_PITCH * amp * ModConfig.get().swayAmount + stepBob;
        float shakeRoll = wobble(time, 20f) * SHAKE_ROLL * amp * ModConfig.get().swayAmount;

        // --- progresso do flip (0 = normal, 1 = selfie completa)
        // Órbita de 180° só DURANTE a animação; com facingSelf estável o jogo já usa
        // THIRD_PERSON_FRONT (vista de frente) e nós só puxamos a posição para perto.
        float t = flipProgress01();
        boolean orbiting = CamcorderState.isFlipping();
        float theta = orbiting ? t * 180f : 0f;

        float camYaw = lagYaw - ORBIT_SIDE * theta + shakeYaw;
        float camPitch = Mth.clamp(
                (orbiting ? lagPitch * Mth.cos(theta * Mth.DEG_TO_RAD) : lagPitch) + shakePitch,
                -89f, 89f);
        float roll = event.getRoll() + shakeRoll + dYaw * ROLL_FROM_LAG
                + (orbiting ? ORBIT_SIDE * Mth.sin(t * Mth.PI) * SWING_ROLL_DEG : 0f);

        // --- posição e holdPoint
        Vec3 pivot = player.getEyePosition(partial).add(0.0, PIVOT_Y_OFFSET, 0.0);
        float yr = camYaw * Mth.DEG_TO_RAD;
        float pr = camPitch * Mth.DEG_TO_RAD;
        Vec3 look = new Vec3(-Mth.sin(yr) * Mth.cos(pr), -Mth.sin(pr), Mth.cos(yr) * Mth.cos(pr));
        Vec3 flat = new Vec3(-Mth.sin(yr), 0.0, Mth.cos(yr));
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);

        hideHead = false;
        boolean cameraMovable = CameraAccess.isAvailable();
        boolean wantClose = CamcorderState.isFacingSelf()
                || (CamcorderState.isFlipping() && CamcorderState.isFlipToSelf());

        if (wantClose && cameraMovable && !type.isFirstPerson()) {
            // Flip / selfie: puxa câmera para perto do rosto (arm-selfie-v2)
            double dist = DISTANCE * Math.max(t, CamcorderState.isFacingSelf() && !CamcorderState.isFlipping() ? 1f : t);
            if (CamcorderState.isFacingSelf() && !CamcorderState.isFlipping()) {
                dist = DISTANCE;
            }
            Vec3 target = pivot.subtract(look.scale(dist));

            BlockHitResult hit = mc.level.clip(new ClipContext(pivot, target,
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                dist = Math.max(0.0, hit.getLocation().distanceTo(pivot) - 0.3);
                target = pivot.subtract(look.scale(dist));
            }

            CameraAccess.setPosition(event.getCamera(), target);
            hideHead = dist < HEAD_HIDE_DIST;
            holdPoint = target;
        } else if (type.isFirstPerson() && cameraMovable) {
            // FP: visão na filmadora — ombro → braço esticado → mão/câmera
            boolean mainRight = player.getMainArm() == HumanoidArm.RIGHT;
            double side = mainRight ? FP_ARM_SIDE : -FP_ARM_SIDE;

            // Ponto da mão (onde a filmadora é segurada)
            Vec3 hand = pivot
                    .add(look.scale(FP_ARM_REACH))
                    .add(0.0, -FP_ARM_DROP, 0.0)
                    .add(right.scale(side));

            // Câmera um pouco atrás da ponta da mão (antebraço entra no frame)
            Vec3 toHand = hand.subtract(pivot);
            double handLen = toHand.length();
            Vec3 target = handLen > 1.0e-4
                    ? pivot.add(toHand.scale(Math.max(0.05, (handLen - FP_CAM_BACK_FROM_HAND) / handLen)))
                    : hand;

            BlockHitResult hit = mc.level.clip(new ClipContext(pivot, target,
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                double d = Math.max(0.0, hit.getLocation().distanceTo(pivot) - 0.05);
                target = pivot.add(target.subtract(pivot).normalize().scale(d));
            }

            CameraAccess.setPosition(event.getCamera(), target);
            holdPoint = hand; // braço procedural aponta para a mão/filmadora
        } else if (type == CameraType.THIRD_PERSON_BACK) {
            holdPoint = pivot.add(look.scale(HOLD_FORWARD)).add(0.0, -0.12, 0.0);
        } else {
            holdPoint = pivot.add(look.scale(HOLD_FORWARD)).add(0.0, -0.12, 0.0);
        }

        event.setYaw(camYaw);
        event.setPitch(camPitch);
        event.setRoll(roll);
    }

    /**
     * 0 = vista normal, 1 = selfie completa.
     * Usa o progresso do flip do CamcorderState; se já está facingSelf, fica em 1.
     */
    private static float flipProgress01() {
        if (CamcorderState.isFlipping()) {
            float p = CamcorderState.getFlipProgress();
            if (CamcorderState.isFlipToSelf()) {
                return smootherStep(Mth.clamp(p, 0f, 1f));
            }
            // virando de volta para FP: 1 → 0
            return smootherStep(Mth.clamp(1f - p, 0f, 1f));
        }
        return CamcorderState.isFacingSelf() ? 1f : 0f;
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player) return;
        Object model = event.getRenderer().getModel();

        if (hideHead && model instanceof HumanoidModel<?> humanoid) {
            humanoid.head.visible = false;
            humanoid.hat.visible = false;
        }
        if (isProceduralArm() && model instanceof PlayerModel<?> playerModel) {
            CamcorderArmLayer.hideVanillaArms(playerModel);
        }
    }

    private static float wobble(float t, float seed) {
        return (Mth.sin(t * 1.6f + seed)
                + 0.6f * Mth.sin(t * 3.9f + seed * 1.7f)
                + 0.4f * Mth.sin(t * 7.7f + seed * 2.3f)) / 2.0f;
    }

    private static float smootherStep(float x) {
        return x * x * x * (x * (x * 6f - 15f) + 10f);
    }
}
