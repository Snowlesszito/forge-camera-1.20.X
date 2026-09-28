package net.snowless.foundcamera.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.snowless.foundcamera.camcorder.CamcorderState;
import net.snowless.foundcamera.registry.ModItems;

@Mod.EventBusSubscriber(modid = ModItems.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientForgeEvents {
    /** true se fomos nós que colocamos o night vision falso no jogador. */
    private static boolean addedIrEffect;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            CamcorderState.deactivate();
            addedIrEffect = false;
            return;
        }

        // Se largou a filmadora, desliga o modo câmera
        if (CamcorderState.isActive() && !isHoldingCamcorder(player)) {
            CamcorderState.deactivate();
        }

        if (CamcorderState.isActive()) {
            while (ClientKeys.PLAY_PAUSE.consumeClick()) CamcorderState.togglePlayPause();
            while (ClientKeys.STOP.consumeClick()) CamcorderState.stop();
            while (ClientKeys.INFRARED.consumeClick()) CamcorderState.toggleInfrared();
            while (ClientKeys.TOGGLE_TEXT.consumeClick()) CamcorderState.toggleText();
            CamcorderState.tick();
        } else {
            // descarta cliques acumulados fora do modo câmera
            while (ClientKeys.PLAY_PAUSE.consumeClick()) {}
            while (ClientKeys.STOP.consumeClick()) {}
            while (ClientKeys.INFRARED.consumeClick()) {}
            while (ClientKeys.TOGGLE_TEXT.consumeClick()) {}
        }

        updateInfrared(player);
    }

    private static boolean isHoldingCamcorder(LocalPlayer p) {
        ItemStack main = p.getMainHandItem();
        ItemStack off = p.getOffhandItem();
        return main.is(ModItems.CAMCORDER.get()) || off.is(ModItems.CAMCORDER.get());
    }

    /**
     * Infravermelho: night vision só no cliente (o servidor não sabe de nada),
     * invisível e sem ícone. Não mexe em night vision real vindo de poção.
     */
    private static void updateInfrared(LocalPlayer p) {
        MobEffectInstance cur = p.getEffect(MobEffects.NIGHT_VISION);
        boolean realNightVision = cur != null && cur.isVisible();

        if (CamcorderState.isInfrared() && !realNightVision) {
            if (cur == null || cur.getDuration() < 300) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
            }
            addedIrEffect = true;
        } else if (addedIrEffect) {
            if (cur != null && !cur.isVisible()) {
                p.removeEffectNoUpdate(MobEffects.NIGHT_VISION);
            }
            addedIrEffect = false;
        }
    }

    /** Esconde a mão enquanto filma. Remova se quiser ver o braço segurando a câmera. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (CamcorderState.isActive()) event.setCanceled(true);
    }

    /** Esconde o HUD vanilla para a gravação ficar limpa. */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!CamcorderState.isActive()) return;
        var id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.HOTBAR.id())
                || id.equals(VanillaGuiOverlay.CROSSHAIR.id())
                || id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                || id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())
                || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())
                || id.equals(VanillaGuiOverlay.AIR_LEVEL.id())
                || id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())
                || id.equals(VanillaGuiOverlay.ITEM_NAME.id())) {
            event.setCanceled(true);
        }
    }
}
