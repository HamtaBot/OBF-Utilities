package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class NoParticlesMixin {

    @Inject(method = {
            "addParticle(Lnet/minecraft/client/particle/Particle;)V",
            "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V",
            "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V"
    }, at = @At("HEAD"), cancellable = true, require = 0)
    private void blockParticles(CallbackInfo ci) {
        if (OBFUtilities.config.disableParticles) ci.cancel();
    }
}
