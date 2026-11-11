package com.fraxxz.fotos;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class CameraItem extends Item {
    /** Lo pone el cliente; en el servidor queda en null. */
    public static Runnable CLIENT_HOOK;

    public CameraItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient && CLIENT_HOOK != null) CLIENT_HOOK.run();
        user.getItemCooldownManager().set(this, 30);
        return TypedActionResult.success(stack, world.isClient);
    }
}
