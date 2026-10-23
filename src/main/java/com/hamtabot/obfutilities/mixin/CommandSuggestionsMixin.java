package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.warp.WarpMacros;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class CommandSuggestionsMixin {

    @Inject(method = "onCommandSuggestions", at = @At("HEAD"), cancellable = true)
    private void onWarpSuggestions(CommandSuggestionsS2CPacket packet, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().isOnThread()) return;
        if (WarpMacros.onSuggestions(packet)) ci.cancel();
    }
}
