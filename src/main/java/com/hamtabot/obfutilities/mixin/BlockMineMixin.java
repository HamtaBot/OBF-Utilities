package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class BlockMineMixin {

    private String pendingBlockId = null;

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void onBreakBlockHead(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.world == null) { pendingBlockId = null; return; }
        BlockState state = client.world.getBlockState(pos);
        Block block = state.getBlock();
        pendingBlockId = Registries.BLOCK.getId(block).toString();
    }

    @Inject(method = "breakBlock", at = @At("RETURN"))
    private void onBreakBlockReturn(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || pendingBlockId == null) return;
        OBFUtilities.onBlockMined(pendingBlockId);
        pendingBlockId = null;
    }
}