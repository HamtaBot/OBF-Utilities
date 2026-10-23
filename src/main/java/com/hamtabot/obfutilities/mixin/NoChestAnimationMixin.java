package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ ChestBlockEntity.class, EnderChestBlockEntity.class })
public class NoChestAnimationMixin {

    @Inject(method = "getAnimationProgress", at = @At("HEAD"), cancellable = true, require = 0)
    private void keepLidClosed(CallbackInfoReturnable<Float> cir) {
        if (OBFUtilities.config.disableChestAnimation) cir.setReturnValue(0f);
    }
}
