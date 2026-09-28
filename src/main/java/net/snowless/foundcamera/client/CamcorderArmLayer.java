package net.snowless.foundcamera.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Braço da selfie: SÓ o braço da mão principal estica para a frente, na direção do olhar da
 * cabeça (que é a direção da câmera frontal). O outro braço fica normal.
 *
 * A fórmula dos ângulos vem da abordagem do SelfieCamMod (ImBonana, MIT):
 *   xRot = head.xRot - PI/2 - 0.25 * head.xRot ; yRot = head.yRot ; zRot = +-0.15 * head.xRot
 * Como o Forge 1.20.1 não tem evento depois do setupAnim, em vez de um mixin usamos esta layer:
 * o braço de fábrica é escondido (CamcorderCamera) e este layer desenha o braço com a nossa pose.
 * Ele é inserido como PRIMEIRA layer, então a layer do item na mão (logo depois) já usa o braço
 * na nossa pose e o item aparece na ponta dele.
 *
 * Em primeira pessoa nada disso roda: o braço fica como no jogo normal.
 */
public class CamcorderArmLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static boolean sleeveWasVisible = true;

    public CamcorderArmLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Chamado antes do render do corpo: esconde só o braço principal de fábrica. */
    static void hideMainArm(PlayerModel<?> m, boolean mainRight) {
        ModelPart arm = mainRight ? m.rightArm : m.leftArm;
        ModelPart sleeve = mainRight ? m.rightSleeve : m.leftSleeve;
        sleeveWasVisible = sleeve.visible;
        arm.visible = false;
        sleeve.visible = false;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partial, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player != Minecraft.getInstance().player || !CamcorderCamera.isProceduralArm()) return;

        PlayerModel<AbstractClientPlayer> model = getParentModel();
        boolean mainRight = player.getMainArm() == HumanoidArm.RIGHT;
        ModelPart arm = mainRight ? model.rightArm : model.leftArm;
        ModelPart sleeve = mainRight ? model.rightSleeve : model.leftSleeve;

        // cabeça (radianos, relativa ao corpo) já com o setupAnim aplicado
        float hx = Mth.clamp(model.head.xRot, (float) Math.toRadians(-90.0), (float) Math.toRadians(35.0));
        float hy = model.head.yRot;

        arm.xRot = hx - Mth.HALF_PI - 0.25f * hx;
        arm.yRot = hy;
        arm.zRot = hx * (mainRight ? 0.15f : -0.15f);

        VertexConsumer vc = buffer.getBuffer(model.renderType(player.getSkinTextureLocation()));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);

        arm.visible = true;
        arm.render(pose, vc, light, overlay);
        if (sleeveWasVisible) {
            sleeve.copyFrom(arm);
            sleeve.visible = true;
            sleeve.render(pose, vc, light, overlay);
        }
    }
}
