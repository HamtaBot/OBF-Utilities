package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.render.block.entity.MobSpawnerBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobSpawnerBlockEntityRenderer.class)
public class NoSpawnerAnimationMixin {

    @Inject(method = "render(Lnet/minecraft/block/entity/MobSpawnerBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void hideSpinningMob(CallbackInfo ci) {
        if (OBFUtilities.config.disableSpawnerAnimation) ci.cancel();
    }
}
