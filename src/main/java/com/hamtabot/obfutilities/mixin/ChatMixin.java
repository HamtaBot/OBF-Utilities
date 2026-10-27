package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.kit.KitTracker;
import com.hamtabot.obfutilities.stats.AdTimer;
import com.hamtabot.obfutilities.stats.ServerStats;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    private void onSendChatMessage(String message, CallbackInfo ci) {
        AdTimer.onChatSent(message);
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"))
    private void onSendChatCommand(String command, CallbackInfo ci) {
        KitTracker.onCommandSent(command);
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (ServerStats.handleServerMessage(packet.content().getString())) ci.cancel();
    }
}
