package net.snowless.foundcamera.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.snowless.foundcamera.registry.ModItems;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ModItems.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CamcorderCameraKeys {
    public static final KeyMapping SELFIE = new KeyMapping("key.foundcamera.selfie",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.foundcamera");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SELFIE);
    }
}
