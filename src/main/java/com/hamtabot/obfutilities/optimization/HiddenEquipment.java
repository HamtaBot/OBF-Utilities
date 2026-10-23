package com.hamtabot.obfutilities.optimization;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class HiddenEquipment {

    private HiddenEquipment() {}

    public static boolean shouldHide(LivingEntity entity) {
        return OBFUtilities.config.hideOtherArmor && entity instanceof PlayerEntity
                && entity != MinecraftClient.getInstance().player;
    }
}
