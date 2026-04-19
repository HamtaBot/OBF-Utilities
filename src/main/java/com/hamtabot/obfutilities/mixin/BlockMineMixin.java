package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class BlockMineMixin {

    @Inject(method = "breakBlock", at = @At("RETURN"))
    private void onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.world == null) return;
        BlockState state = client.world.getBlockState(pos);
        Block block = state.getBlock();
        String blockId = Registries.BLOCK.getId(block).toString();
        OBFUtilities.onBlockMined(blockId);
    }
}
