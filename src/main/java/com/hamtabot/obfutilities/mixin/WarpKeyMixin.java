package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.warp.WarpMacros;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class WarpKeyMixin {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onWarpKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (action == GLFW.GLFW_PRESS && window == client.getWindow().getHandle() && client.currentScreen == null) {
            WarpMacros.onKeyPressed(client, key);
        }
    }
}
