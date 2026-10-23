package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.doubledrop.DoubleDrop;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class DoubleDropScreenMixin {

    @Shadow @Nullable protected Slot focusedSlot;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (focusedSlot == null || !focusedSlot.hasStack()) return;
        if (!MinecraftClient.getInstance().options.dropKey.matchesKey(keyCode, scanCode)) return;
        if (!DoubleDrop.allowDrop("slot:" + focusedSlot.id)) {
            cir.setReturnValue(true);
        }
    }
}
