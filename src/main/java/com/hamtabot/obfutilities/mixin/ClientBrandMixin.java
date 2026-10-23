package com.hamtabot.obfutilities.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public class ClientBrandMixin {

    @Inject(method = "getClientModName", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void addModName(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(cir.getReturnValue() + " (OBF Utilities)");
    }
}
