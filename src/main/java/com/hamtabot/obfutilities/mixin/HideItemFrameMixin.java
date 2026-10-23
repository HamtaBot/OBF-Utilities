package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFrameEntityRenderer.class)
public class HideItemFrameMixin {

    @WrapOperation(method = "render(Lnet/minecraft/entity/decoration/ItemFrameEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
                   at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;isInvisible()Z"), require = 0)
    private boolean hideFrame(ItemFrameEntity frame, Operation<Boolean> original) {
        return OBFUtilities.config.hideItemFrames || original.call(frame);
    }
}
