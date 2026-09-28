package net.snowless.foundcamera.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.snowless.foundcamera.camcorder.CamcorderState;

public class CamcorderItem extends Item {
    public CamcorderItem(Properties properties) {
        super(properties);
    }

    /** Botão direito: liga/desliga o modo câmera. Só roda o lado cliente, o estado é visual. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            CamcorderState.toggleActive();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
