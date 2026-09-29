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
import net.minecraft.world.phys.Vec3;

/**
 * Braço procedural: os dois braços apontam para o "ponto de mão" (onde está a câmera),
 * sempre. É inserido como PRIMEIRA layer do PlayerRenderer, então quando a layer do item na
 * mão roda (logo depois), o item já aparece na ponta do nosso braço.
 *
 * O modelo do jogador não tem cotovelo: o braço inteiro aponta para a câmera.
 */
public class CamcorderArmLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static boolean rightSleeveOn = true;
    private static boolean leftSleeveOn = true;

    public CamcorderArmLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Chamado antes do render do corpo: esconde os braços de fábrica. */
    static void hideVanillaArms(PlayerModel<?> m) {
        rightSleeveOn = m.rightSleeve.visible;
        leftSleeveOn = m.leftSleeve.visible;
        m.rightArm.visible = false;
        m.leftArm.visible = false;
        m.rightSleeve.visible = false;
        m.leftSleeve.visible = false;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partial, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player != Minecraft.getInstance().player || !CamcorderCamera.isProceduralArm()) return;

        Vec3 hold = CamcorderCamera.getHoldPoint();
        PlayerModel<AbstractClientPlayer> model = getParentModel();

        boolean mainRight = player.getMainArm() == HumanoidArm.RIGHT;
        double sign = mainRight ? 1.0 : -1.0;

        // "direita" do corpo (mundo), para separar as duas mãos na câmera
        float bodyYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        double by = Math.toRadians(bodyYaw);
        Vec3 right = new Vec3(-Math.cos(by), 0.0, -Math.sin(by));

        Vec3 mainTarget = hold.add(right.scale(sign * 0.04));
        Vec3 offTarget = hold.subtract(right.scale(sign * 0.14));

        aim(player, partial, mainRight ? model.rightArm : model.leftArm, mainTarget);
        aim(player, partial, mainRight ? model.leftArm : model.rightArm, offTarget);

        VertexConsumer vc = buffer.getBuffer(model.renderType(player.getSkinTextureLocation()));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);

        draw(model.rightArm, model.rightSleeve, rightSleeveOn, pose, vc, light, overlay);
        draw(model.leftArm, model.leftSleeve, leftSleeveOn, pose, vc, light, overlay);
    }

    private static void draw(ModelPart arm, ModelPart sleeve, boolean sleeveOn,
                             PoseStack pose, VertexConsumer vc, int light, int overlay) {
        arm.visible = true;
        arm.render(pose, vc, light, overlay);
        if (sleeveOn) {
            sleeve.copyFrom(arm);
            sleeve.visible = true;
            sleeve.render(pose, vc, light, overlay);
        }
    }

    /**
     * Gira o braço (que no modelo aponta para baixo) na direção do alvo.
     * Converte o alvo do mundo para o espaço do modelo, desfazendo a mesma cadeia de
     * transformações do LivingEntityRenderer: rotação (180 - yawCorpo), escala (-1,-1,1),
     * escala 0.9375 e translação (0,-1.501,0).
     */
    private static void aim(AbstractClientPlayer p, float partial, ModelPart arm, Vec3 target) {
        Vec3 feet = new Vec3(Mth.lerp(partial, p.xo, p.getX()),
                Mth.lerp(partial, p.yo, p.getY()),
                Mth.lerp(partial, p.zo, p.getZ()));
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
}
