package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.autotool.AutoTool;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class AutoToolMixin {

    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void onAttackBlock(BlockPos pos, net.minecraft.util.math.Direction direction, CallbackInfoReturnable<Boolean> cir) {
        AutoTool.onBlockLook(pos);
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateBreaking(BlockPos pos, net.minecraft.util.math.Direction direction, CallbackInfoReturnable<Boolean> cir) {
        AutoTool.onBlockLook(pos);
    }
}
