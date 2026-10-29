package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.bossbar.BossBars;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ResourcePackSendS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ResourcePackMixin {

    // both servers sit behind the same proxy address; the pack each one sends tells them apart
    @Inject(method = "onResourcePackSend", at = @At("HEAD"))
    private void obf$rememberServerPack(ResourcePackSendS2CPacket packet, CallbackInfo ci) {
        BossBars.setServerPack(packet.getURL());
    }
}
