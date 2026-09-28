package net.snowless.foundcamera.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.snowless.foundcamera.camcorder.CamcorderState;
import net.snowless.foundcamera.config.ModConfig;
import net.snowless.foundcamera.registry.ModItems;

@Mod.EventBusSubscriber(modid = ModItems.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientForgeEvents {
    private static boolean addedIrEffect;

    private ClientForgeEvents() {}

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

        if (CamcorderState.isActive() && !isHoldingCamcorder(player)) {
            CamcorderState.deactivate();
        }

        if (CamcorderState.isActive()) {
            while (ClientKeys.PLAY_PAUSE.consumeClick()) CamcorderState.togglePlayPause();
            while (ClientKeys.STOP.consumeClick()) CamcorderState.stop();
            while (ClientKeys.INFRARED.consumeClick()) CamcorderState.toggleInfrared();
            while (ClientKeys.TOGGLE_TEXT.consumeClick()) CamcorderState.toggleText();
            while (ClientKeys.FLIP.consumeClick()) CamcorderState.toggleFlip();
            while (ClientKeys.FEAR_SHAKE.consumeClick()) CamcorderState.toggleFearShake();

            float step = ModConfig.get().zoomStep;
            while (ClientKeys.ZOOM_IN.consumeClick()) CamcorderState.addZoom(+step);
            while (ClientKeys.ZOOM_OUT.consumeClick()) CamcorderState.addZoom(-step);

            CamcorderState.tick();

            if (CamcorderState.isFacingSelf() && !CamcorderState.isFlipping()) {
                if (mc.options.getCameraType() != CameraType.THIRD_PERSON_FRONT) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                }
            } else if (!CamcorderState.isFlipping()) {
                if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                }
            }
        } else {
            while (ClientKeys.PLAY_PAUSE.consumeClick()) {}
            while (ClientKeys.STOP.consumeClick()) {}
            while (ClientKeys.INFRARED.consumeClick()) {}
            while (ClientKeys.TOGGLE_TEXT.consumeClick()) {}
            while (ClientKeys.FLIP.consumeClick()) {}
            while (ClientKeys.FEAR_SHAKE.consumeClick()) {}
            while (ClientKeys.ZOOM_IN.consumeClick()) {}
            while (ClientKeys.ZOOM_OUT.consumeClick()) {}

            if (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
        }

        updateInfrared(player);
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!ModConfig.get().zoomWithScroll) return;
        if (!CamcorderState.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;

        double delta = event.getScrollDelta();
        if (delta == 0) return;
        float step = ModConfig.get().zoomStep;
        CamcorderState.addZoom(delta > 0 ? +step : -step);
        event.setCanceled(true);
    }

    private static boolean isHoldingCamcorder(LocalPlayer p) {
        ItemStack main = p.getMainHandItem();
        ItemStack off = p.getOffhandItem();
        return main.is(ModItems.CAMCORDER.get()) || off.is(ModItems.CAMCORDER.get());
    }

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

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (CamcorderState.isActive() && mc.options.getCameraType().isFirstPerson()) {
            event.setCanceled(true);
        }
    }

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