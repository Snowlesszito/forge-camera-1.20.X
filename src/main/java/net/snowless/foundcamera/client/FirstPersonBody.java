package net.snowless.foundcamera.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.snowless.foundcamera.camcorder.CamcorderState;
import net.snowless.foundcamera.config.ModConfig;
import net.snowless.foundcamera.registry.ModItems;

/**
 * Em 1ª pessoa com a filmadora ativa:
 *  - desenha o corpo do jogador (sem cabeça)
 *  - braço que segura a câmera aponta proceduralmente para o holdPoint
 *    (origem da visão = filmadora na mão, não o olho)
 *  - o outro braço fica em pose de apoio
 *  - item/mãos vanilla já são cancelados em ClientForgeEvents
 */
@Mod.EventBusSubscriber(modid = ModItems.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FirstPersonBody {
    private static boolean renderingFpBody;

    private FirstPersonBody() {}

    public static boolean isRenderingFpBody() {
        return renderingFpBody;
    }

    private static boolean shouldShowBody() {
        Minecraft mc = Minecraft.getInstance();
        return CamcorderState.isActive()
                && !CamcorderState.isFacingSelf()
                && !CamcorderState.isFlipping()
                && mc.player != null
                && mc.options.getCameraType().isFirstPerson()
                && !mc.player.isSpectator()
                && !mc.player.isSleeping();
    }

    private static HumanoidArm armHoldingCamera(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean mainCam = main.is(ModItems.CAMCORDER.get());
        boolean offCam = off.is(ModItems.CAMCORDER.get());
        if (mainCam && !offCam) return player.getMainArm();
        if (offCam && !mainCam) return player.getMainArm().getOpposite();
        return player.getMainArm();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!shouldShowBody()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        float pt = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        ModConfig.Data cfg = ModConfig.get();

        var cam = event.getCamera().getPosition();
        double x = Mth.lerp(pt, player.xOld, player.getX()) - cam.x;
        double y = Mth.lerp(pt, player.yOld, player.getY()) - cam.y;
        double z = Mth.lerp(pt, player.zOld, player.getZ()) - cam.z;

        float yRot = Mth.lerp(pt, player.yRotO, player.getYRot());
        float yawRad = yRot * ((float) Math.PI / 180f);

        // Corpo um pouco atrás da câmera (que está na mão à frente)
        x += Mth.sin(yawRad) * cfg.bodyBack - Mth.cos(yawRad) * cfg.bodySide;
        z += -Mth.cos(yawRad) * cfg.bodyBack - Mth.sin(yawRad) * cfg.bodySide;
        y -= cfg.bodyDown;

        renderingFpBody = true;
        try {
            mc.getEntityRenderDispatcher().setRenderShadow(false);
            mc.getEntityRenderDispatcher().render(
                    player, x, y, z, yRot, pt, pose, buffer,
                    mc.getEntityRenderDispatcher().getPackedLightCoords(player, pt)
            );
            buffer.endBatch();
        } finally {
            mc.getEntityRenderDispatcher().setRenderShadow(true);
            renderingFpBody = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!renderingFpBody) return;
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (!(event.getRenderer().getModel() instanceof PlayerModel<?> model)) return;

        ModConfig.Data cfg = ModConfig.get();
        HumanoidArm camArm = armHoldingCamera(player);
        boolean mainRight = camArm == HumanoidArm.RIGHT;

        // Cabeça fora (visão pela filmadora, não pelo olho)
        model.head.visible = false;
        model.hat.visible = false;

        model.body.visible = true;
        model.leftLeg.visible = true;
        model.rightLeg.visible = true;
        model.jacket.visible = true;
        model.leftPants.visible = true;
        model.rightPants.visible = true;

        ModelPart mainArm = mainRight ? model.rightArm : model.leftArm;
        ModelPart freeArm = mainRight ? model.leftArm : model.rightArm;
        ModelPart mainSleeve = mainRight ? model.rightSleeve : model.leftSleeve;
        ModelPart freeSleeve = mainRight ? model.leftSleeve : model.rightSleeve;

        mainArm.visible = true;
        freeArm.visible = true;
        mainSleeve.visible = true;
        freeSleeve.visible = true;

        Vec3 hold = CamcorderCamera.getHoldPoint();
        float partial = event.getPartialTick();

        if (hold != null) {
            // Braço principal: aponta do ombro até a filmadora (holdPoint)
            aimArm(player, partial, mainArm, hold);
        } else {
            // Fallback se holdPoint ainda não existir neste frame
            float headX = model.head.xRot;
            float headY = model.head.yRot;
            mainArm.xRot = cfg.armPitch + headX * 0.35f;
            mainArm.yRot = (mainRight ? -cfg.armSpread : cfg.armSpread) + headY * 0.25f;
            mainArm.zRot = mainRight ? 0.05f : -0.05f;
        }

        // Braço livre: pose de apoio, levemente afastado
        freeArm.xRot = cfg.freeArmPitch;
        freeArm.yRot = mainRight ? 0.12f : -0.12f;
        freeArm.zRot = mainRight ? -0.06f : 0.06f;

        mainSleeve.copyFrom(mainArm);
        freeSleeve.copyFrom(freeArm);
    }

    /**
     * Gira o braço (modelo aponta para baixo por padrão) na direção do alvo no mundo.
     * Mesma cadeia do LivingEntityRenderer: yaw do corpo, escala -1/-1/1, 0.9375, translate (0,-1.501,0).
     */
    private static void aimArm(LocalPlayer p, float partial, ModelPart arm, Vec3 target) {
        Vec3 feet = new Vec3(
                Mth.lerp(partial, p.xo, p.getX()),
                Mth.lerp(partial, p.yo, p.getY()),
                Mth.lerp(partial, p.zo, p.getZ())
        );
        Vec3 rel = target.subtract(feet);

        float bodyYaw = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
        double a = Math.toRadians(-(180.0 - bodyYaw));
        double cos = Math.cos(a), sin = Math.sin(a);
        double qx = rel.x * cos + rel.z * sin;
        double qz = -rel.x * sin + rel.z * cos;
        double qy = rel.y;

        double mx = 16.0 * (-qx / 0.9375);
        double my = 16.0 * (-qy / 0.9375 + 1.501);
        double mz = 16.0 * (qz / 0.9375);

        double dx = mx - arm.x, dy = my - arm.y, dz = mz - arm.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4) return;
        double ux = dx / len, uy = dy / len, uz = dz / len;

        arm.xRot = (float) -Math.acos(Mth.clamp(uy, -1.0, 1.0));
        arm.yRot = (float) Math.atan2(-ux, -uz);
        arm.zRot = 0.0F;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (!renderingFpBody) return;
        if (!(event.getRenderer().getModel() instanceof PlayerModel<?> model)) return;
        model.head.visible = true;
        model.hat.visible = true;
    }
}
