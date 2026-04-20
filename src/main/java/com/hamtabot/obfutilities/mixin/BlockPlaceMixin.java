package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class BlockPlaceMixin {

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                 CallbackInfoReturnable<ActionResult> cir) {
        ActionResult result = cir.getReturnValue();
        if (result != ActionResult.SUCCESS && result != ActionResult.CONSUME) return;

        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            String blockId = Registries.BLOCK.getId(block).toString();
            String itemId  = Registries.ITEM.getId(stack.getItem()).toString();
            OBFUtilities.onBlockPlaced(blockId, itemId);
        }
    }
}