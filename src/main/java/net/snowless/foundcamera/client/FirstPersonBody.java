package net.snowless.foundcamera.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.snowless.foundcamera.camcorder.CamcorderState;
import net.snowless.foundcamera.config.ModConfig;
import net.snowless.foundcamera.registry.ModItems;

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

        model.head.visible = false;
        model.hat.visible = false;
        model.rightArm.visible = true;
        model.leftArm.visible = true;
        model.rightSleeve.visible = true;
        model.leftSleeve.visible = true;

        float headX = model.head.xRot;
        float headY = model.head.yRot;
        float bowPitch = cfg.armPitch + headX * 0.35f;

        if (camArm == HumanoidArm.RIGHT) {
            model.rightArm.xRot = bowPitch;
            model.rightArm.yRot = -cfg.armSpread + headY * 0.25f;
            model.rightArm.zRot = 0.05f;
            model.leftArm.xRot = cfg.freeArmPitch;
            model.leftArm.yRot = 0.1f;
            model.leftArm.zRot = -0.05f;
        } else {
            model.leftArm.xRot = bowPitch;
            model.leftArm.yRot = cfg.armSpread + headY * 0.25f;
            model.leftArm.zRot = -0.05f;
            model.rightArm.xRot = cfg.freeArmPitch;
            model.rightArm.yRot = -0.1f;
            model.rightArm.zRot = 0.05f;
        }

        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.body.visible = true;
        model.leftLeg.visible = true;
        model.rightLeg.visible = true;
        model.jacket.visible = true;
        model.leftPants.visible = true;
        model.rightPants.visible = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (!renderingFpBody) return;
        if (!(event.getRenderer().getModel() instanceof PlayerModel<?> model)) return;
        model.head.visible = true;
        model.hat.visible = true;
    }
}