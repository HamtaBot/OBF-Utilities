package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.stats.SessionStats;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class BlockPlaceMixin {

    @Unique private BlockItem obf$item;
    @Unique private BlockPos obf$pos;
    @Unique private BlockState obf$before;

    // read before the click: placing the last item of a stack leaves the hand empty afterwards
    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void obf$beforeInteract(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                    CallbackInfoReturnable<ActionResult> cir) {
        obf$item = null;
        if (!(player.getStackInHand(hand).getItem() instanceof BlockItem blockItem)) return;
        ItemPlacementContext ctx = blockItem.getPlacementContext(
                new ItemPlacementContext(new ItemUsageContext(player, hand, hitResult)));
        if (ctx == null) return;
        obf$item = blockItem;
        obf$pos = ctx.getBlockPos();
        obf$before = player.getWorld().getBlockState(obf$pos);
    }

    // counted only when the block really appeared, not when the click opened a chest or was refused
    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void obf$afterInteract(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                   CallbackInfoReturnable<ActionResult> cir) {
        BlockItem blockItem = obf$item;
        obf$item = null;
        if (blockItem == null || !cir.getReturnValue().isAccepted()) return;
        Block block = blockItem.getBlock();
        // not isOf(block): a torch put on a wall becomes another block than the item's
        if (player.getWorld().getBlockState(obf$pos) == obf$before) return;
        SessionStats.onBlockPlaced(Registries.BLOCK.getId(block).toString(), Registries.ITEM.getId(blockItem).toString());
    }
}
