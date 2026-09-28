package net.snowless.foundcamera.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.snowless.foundcamera.item.CamcorderItem;

public final class ModItems {
    // Tem que ser igual ao mod_id do gradle.properties
    public static final String MOD_ID = "foundcamera";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> CAMCORDER =
            ITEMS.register("camcorder", () -> new CamcorderItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}

    /** Chame isso no construtor da sua classe @Mod principal. */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
