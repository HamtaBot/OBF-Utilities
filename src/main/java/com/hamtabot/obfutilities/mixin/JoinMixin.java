package com.hamtabot.obfutilities.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class JoinMixin {

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        // ATT 3 sec, vue que monsieur serveur est pas content si tes pas en pré-load de pack
        com.hamtabot.obfutilities.OBFUtilities.onJoined();
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client.player != null) {
                    client.execute(() -> {
                        if (client.getNetworkHandler() != null) {
                            client.getNetworkHandler().sendChatCommand("utilitiesstats");
                        }
                    });
                }
            } catch (InterruptedException ignored) {}
        }, "OBF-StatsRequest").start();
    }
}