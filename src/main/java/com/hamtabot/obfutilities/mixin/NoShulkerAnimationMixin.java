package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.render.block.entity.ShulkerBoxBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShulkerBoxBlockEntityRenderer.class)
public class NoShulkerAnimationMixin {

    @WrapOperation(method = "render(Lnet/minecraft/block/entity/ShulkerBoxBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
                   at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/ShulkerBoxBlockEntity;getAnimationProgress(F)F"), require = 0)
    private float keepLidClosed(ShulkerBoxBlockEntity shulker, float tickDelta, Operation<Float> original) {
        return OBFUtilities.config.disableChestAnimation ? 0f : original.call(shulker, tickDelta);
    }
}
