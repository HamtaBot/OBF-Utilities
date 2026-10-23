package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.input.DoubleDrop;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class DoubleDropPlayerMixin {

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (self.getMainHandStack().isEmpty()) return;
        if (!DoubleDrop.allowDrop("hotbar:" + self.getInventory().selectedSlot)) {
            cir.setReturnValue(false);
        }
    }
}
