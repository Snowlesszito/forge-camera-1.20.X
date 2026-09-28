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
import net.snowless.foundcamera.registry.ModItems;
import org.slf4j.Logger;

/**
 * Dono ÚNICO da câmera enquanto a filmadora está ativa.
 *
 * Como a selfie fica "perto": a técnica do SelfieCam (kubbidev) - câmera frontal do jogo
 * e FOV bem estreito (teleobjetiva). A 3-4 blocos com FOV ~22° o enquadramento é o mesmo de
 * uma câmera a ~1 bloco do rosto, sem depender de mover a câmera nem de colisão.
 *
 *  - tecla V: selfie animada. A câmera gira em órbita ao redor do personagem, se afasta e o FOV
 *    fecha ao mesmo tempo. Se o jogo não deixar mover a câmera, cai no plano B: câmera frontal
 *    do jogo (sem a órbita) + FOV estreito.
 *  - câmera frontal do jogo (F5 frontal / o seu "flip"): só ganha o FOV estreito.
 *  - primeira pessoa: só tremor e arrasto (nada da selfie mexe nela).
 *
 * Roda com prioridade LOWEST: se outro código seu mexer na câmera, este vence.
 */
@Mod.EventBusSubscriber(modid = ModItems.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CamcorderCamera {
    private static final Logger LOGGER = LogUtils.getLogger();

    // ================= AJUSTES: selfie =================
    /** Tecla V: distância final da câmera, em blocos (a frontal do vanilla usa 4). */
    private static final float DISTANCE = 3.0f;
    /** Tecla V: FOV vertical final, em graus. Menor = mais perto. */
    private static final float SELFIE_FOV = 22.0f;
    /** Câmera frontal do jogo (seu flip): FOV vertical usado. */
    private static final float FRONT_FOV = 22.0f;
    /** 1 = a câmera passa pela direita do personagem, -1 = pela esquerda. */
    private static final float ORBIT_SIDE = 1.0f;
    /** Duração da animação, em segundos. */
    private static final float DURATION = 1.0f;
    /** Inclinação lateral (graus) no meio da virada. Inverta o sinal se inclinar pro lado errado. */
    private static final float SWING_ROLL_DEG = 7.0f;
    /** Altura do ponto que a câmera mira (0 = altura dos olhos). */
    private static final float PIVOT_Y_OFFSET = 0.0f;
    /** Abaixo dessa distância a cabeça do modelo é escondida. */
    private static final float HEAD_HIDE_DIST = 0.6f;

    // ================= AJUSTES: tremor / peso =================
    private static final float FOLLOW_SPEED = 8.0f;   // menor = mais arrasto
    private static final float MAX_LAG_DEG = 10.0f;   // limite do atraso
    private static final float ROLL_FROM_LAG = 0.15f; // inclina ao virar rápido
    private static final float SHAKE_YAW = 1.2f;      // graus
    private static final float SHAKE_PITCH = 0.9f;
    private static final float SHAKE_ROLL = 1.6f;
    private static final float WALK_SHAKE = 6.0f;     // quanto o tremor cresce andando
    private static final float STEP_BOB_DEG = 1.0f;   // balanço do passo
    private static final float SHAKE_SPEED = 1.0f;    // multiplicador de frequência

    // ================= estado =================
    private static boolean selfie;
    private static float progress;            // 0 = normal, 1 = selfie completa
    private static boolean forcing;           // tecla V: estamos controlando o tipo de câmera
    private static CameraType previousType = CameraType.FIRST_PERSON;
    private static boolean hideHead;
    private static boolean loggedFirstRun;
    private static CameraType lastLoggedType;

    private static boolean swayInit;
    private static float lagYaw, lagPitch;
    private static long lastNanos;

    private CamcorderCamera() {}

    private static boolean isFilming() {
        return CamcorderState.isActive();
    }

    /** O braço da selfie só existe em terceira pessoa (em primeira pessoa fica como no jogo). */
    public static boolean isProceduralArm() {
        Minecraft mc = Minecraft.getInstance();
        return isFilming() && mc.player != null && !mc.options.getCameraType().isFirstPerson();
    }

    private static CameraType selfieCameraType() {
        // com órbita: terceira pessoa por trás (a câmera é movida por nós); plano B: frontal do jogo
        return CameraAccess.isAvailable() ? CameraType.THIRD_PERSON_BACK : CameraType.THIRD_PERSON_FRONT;
    }

    // ------------------------------------------------------------ tick
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || !isFilming()) {
            while (CamcorderCameraKeys.SELFIE.consumeClick()) {}
            if (forcing) release(mc);
            selfie = false;
            progress = 0f;
            hideHead = false;
            loggedFirstRun = false;
            lastLoggedType = null;
            return;
        }

        while (CamcorderCameraKeys.SELFIE.consumeClick()) {
            selfie = !selfie;
            LOGGER.info("[foundcamera] selfie = {} (orbita: {})", selfie, CameraAccess.isAvailable());
        }

        boolean needThird = selfie || progress > 0f;
        CameraType wanted = selfieCameraType();
        if (needThird && !forcing) {
            previousType = mc.options.getCameraType();
            mc.options.setCameraType(wanted);
            forcing = true;
        } else if (!needThird && forcing) {
            release(mc);
        } else if (needThird && mc.options.getCameraType() != wanted) {
            mc.options.setCameraType(wanted); // apertou F5 no meio
        }
    }

    private static void release(Minecraft mc) {
        mc.options.setCameraType(previousType);
        forcing = false;
    }

    // ------------------------------------------------------------ câmera
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !isFilming()) {
            swayInit = false;
            hideHead = false;
            return;
        }

        CameraType type = mc.options.getCameraType();
        if (!loggedFirstRun) {
            loggedFirstRun = true;
            LOGGER.info("[foundcamera] controle de camera ATIVO (tremor/selfie/braco)");
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
        float k = 1f - (float) Math.exp(-dt * FOLLOW_SPEED);
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

        float shakeYaw = wobble(time, 0f) * SHAKE_YAW * amp;
        float shakePitch = wobble(time, 10f) * SHAKE_PITCH * amp + stepBob;
        float shakeRoll = wobble(time, 20f) * SHAKE_ROLL * amp;

        // --- animação da selfie (tecla V)
        float step = dt / DURATION;
        progress = Mth.clamp(progress + (selfie ? step : -step), 0f, 1f);
        float t = smootherStep(progress);

        boolean orbit = forcing && CameraAccess.isAvailable();
        float theta = orbit ? t * 180f : 0f;
        float basePitch = orbit ? Mth.lerp(t, lagPitch, Mth.clamp(lagPitch, -90f, 35f)) : lagPitch;

        float camYaw = lagYaw - ORBIT_SIDE * theta + shakeYaw;
        float camPitch = Mth.clamp(basePitch * Mth.cos(theta * Mth.DEG_TO_RAD) + shakePitch, -89f, 89f);
        float roll = event.getRoll() + shakeRoll + dYaw * ROLL_FROM_LAG
                + (orbit ? ORBIT_SIDE * Mth.sin(t * Mth.PI) * SWING_ROLL_DEG : 0f);

        // --- posição da câmera (só na órbita da tecla V)
        hideHead = false;
        if (orbit) {
            Vec3 pivot = player.getEyePosition(partial).add(0.0, PIVOT_Y_OFFSET, 0.0);
            float yr = camYaw * Mth.DEG_TO_RAD;
            float pr = camPitch * Mth.DEG_TO_RAD;
            Vec3 look = new Vec3(-Mth.sin(yr) * Mth.cos(pr), -Mth.sin(pr), Mth.cos(yr) * Mth.cos(pr));
            Vec3 target = placeCamera(mc, player, event, pivot, look, DISTANCE * t);
            hideHead = pivot.distanceTo(target) < HEAD_HIDE_DIST;
        }

        event.setYaw(camYaw);
        event.setPitch(camPitch);
        event.setRoll(roll);
    }

    /** FOV estreito = câmera "perto" (técnica do SelfieCam). Só na selfie e na câmera frontal. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (!isFilming() || !event.usedConfiguredFov()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double fov = event.getFOV();
        if (forcing) {
            float t = smootherStep(progress);
            event.setFOV(Mth.lerp(t, fov, Math.min(fov, SELFIE_FOV)));
        } else if (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            event.setFOV(Math.min(fov, FRONT_FOV));
        }
    }

    /** Move a câmera para (pivot - look * dist), sem atravessar blocos. Devolve a posição final. */
    private static Vec3 placeCamera(Minecraft mc, LocalPlayer player, ViewportEvent.ComputeCameraAngles event,
                                    Vec3 pivot, Vec3 look, double dist) {
        Vec3 target = pivot.subtract(look.scale(dist));
        BlockHitResult hit = mc.level.clip(new ClipContext(pivot, target,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            double d = Math.max(0.0, hit.getLocation().distanceTo(pivot) - 0.3);
            target = pivot.subtract(look.scale(d));
        }
        CameraAccess.setPosition(event.getCamera(), target);
        return target;
    }

    /** Esconde cabeça (câmera colada no rosto) e o braço principal de fábrica (o layer desenha o da selfie). */
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
            CamcorderArmLayer.hideMainArm(playerModel, mc.player.getMainArm() == HumanoidArm.RIGHT);
        }
    }

    // ------------------------------------------------------------ util
    private static float wobble(float t, float seed) {
        return (Mth.sin(t * 1.6f + seed)
                + 0.6f * Mth.sin(t * 3.9f + seed * 1.7f)
                + 0.4f * Mth.sin(t * 7.7f + seed * 2.3f)) / 2.0f;
    }

    private static float smootherStep(float x) {
        return x * x * x * (x * (x * 6f - 15f) + 10f);
    }
}
