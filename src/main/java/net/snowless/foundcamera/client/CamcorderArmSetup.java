package net.snowless.foundcamera.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.snowless.foundcamera.registry.ModItems;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

@Mod.EventBusSubscriber(modid = ModItems.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CamcorderArmSetup {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CamcorderArmSetup() {}

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                insertFirst(playerRenderer, new CamcorderArmLayer(playerRenderer));
            }
        }
    }

    /**
     * Coloca a layer NO INÍCIO da lista, para rodar antes da layer do item na mão
     * (assim o item já aparece na posição do braço procedural). Acha a lista pelo tipo do campo.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void insertFirst(LivingEntityRenderer renderer, RenderLayer layer) {
        try {
            for (Field f : LivingEntityRenderer.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType()) && !Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    ((List) f.get(renderer)).add(0, layer);
                    return;
                }
            }
            throw new NoSuchFieldException("lista de layers");
        } catch (Throwable t) {
            LOGGER.warn("[foundcamera] Não consegui inserir a layer no início; o item pode aparecer fora da mão", t);
            renderer.addLayer(layer);
        }
    }
}
