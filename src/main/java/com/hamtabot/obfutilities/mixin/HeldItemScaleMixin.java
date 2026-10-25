package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HeldItemRenderer.class)
public class HeldItemScaleMixin {

    @WrapOperation(method = "renderFirstPersonItem",
                   at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"),
                   require = 0)
    private void scaleHeldItem(HeldItemRenderer renderer, LivingEntity entity, ItemStack stack, ModelTransformationMode mode,
                               boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertices, int light,
                               Operation<Void> original) {
        float scale = OBFUtilities.config.heldItemScale;
        if (scale >= 1f) { original.call(renderer, entity, stack, mode, leftHanded, matrices, vertices, light); return; }
        if (scale <= 0f) return;
        matrices.push();
        matrices.scale(scale, scale, scale);
        original.call(renderer, entity, stack, mode, leftHanded, matrices, vertices, light);
        matrices.pop();
    }
}
