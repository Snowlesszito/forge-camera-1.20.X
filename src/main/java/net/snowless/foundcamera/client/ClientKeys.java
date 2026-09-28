package net.snowless.foundcamera.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.snowless.foundcamera.registry.ModItems;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ModItems.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientKeys {
    private static final String CAT = "key.categories.foundcamera";

    public static final KeyMapping PLAY_PAUSE = new KeyMapping("key.foundcamera.playpause",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CAT);
    public static final KeyMapping STOP = new KeyMapping("key.foundcamera.stop",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CAT);
    public static final KeyMapping INFRARED = new KeyMapping("key.foundcamera.infrared",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CAT);
    public static final KeyMapping TOGGLE_TEXT = new KeyMapping("key.foundcamera.toggle_text",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CAT);
    public static final KeyMapping ZOOM_IN = new KeyMapping("key.foundcamera.zoom_in",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, CAT);
    public static final KeyMapping ZOOM_OUT = new KeyMapping("key.foundcamera.zoom_out",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, CAT);
    public static final KeyMapping FLIP = new KeyMapping("key.foundcamera.flip",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CAT);
    public static final KeyMapping FEAR_SHAKE = new KeyMapping("key.foundcamera.fear_shake",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CAT);

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(PLAY_PAUSE);
        event.register(STOP);
        event.register(INFRARED);
        event.register(TOGGLE_TEXT);
        event.register(ZOOM_IN);
        event.register(ZOOM_OUT);
        event.register(FLIP);
        event.register(FEAR_SHAKE);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll(CamcorderHud.ID, new CamcorderHud());
    }
}