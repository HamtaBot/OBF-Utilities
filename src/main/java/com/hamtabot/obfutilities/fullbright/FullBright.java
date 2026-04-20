package com.hamtabot.obfutilities.fullbright;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class FullBright {

    private static final int EFFECT_DURATION = 400;

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (OBFUtilities.config.fullBrightEnabled) {
            StatusEffectInstance current = client.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 210) {
                client.player.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.NIGHT_VISION, EFFECT_DURATION, 0, false, false, false)
                );
            }
        } else {
            if (client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                StatusEffectInstance effect = client.player.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (effect != null && !effect.isAmbient()) {
                    client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
            }
        }
    }
}