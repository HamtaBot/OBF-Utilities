package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.kit.KitTracker;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class KitClickMixin {

    @Inject(method = "clickSlot", at = @At("HEAD"))
    private void onClickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        KitTracker.onSlotClick(syncId, slotId, button, actionType);
    }
}
