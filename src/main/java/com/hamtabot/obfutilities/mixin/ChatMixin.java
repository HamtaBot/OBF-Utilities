package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatMixin {

    // Détecte [i] ou [item] dans les messages envoyés par le joueur
    private static final Pattern AD_PATTERN = Pattern.compile("\\[(i|item)\\]", Pattern.CASE_INSENSITIVE);

    // Patterns pour lire la réponse de /utilitiesstats
    private static final Pattern MINED_PATTERN  = Pattern.compile("blocs min[ée]s\\s*:\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACED_PATTERN = Pattern.compile("blocs pos[ée]s\\s*:\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern KILLS_PATTERN  = Pattern.compile("mobs tu[ée]s\\s*:\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE);

    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (AD_PATTERN.matcher(message).find()) {
            OBFUtilities.onAdSent();
        }
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        String text = packet.content().getString();

        // On check /utilitiesstats
        boolean matched = false;

        Matcher mMined = MINED_PATTERN.matcher(text);
        if (mMined.find()) {
            try {
                int val = Integer.parseInt(mMined.group(1).replaceAll("[,\\.]", ""));
                OBFUtilities.config.serverTotalMined = val;
                matched = true;
            } catch (NumberFormatException ignored) {}
        }

        Matcher mPlaced = PLACED_PATTERN.matcher(text);
        if (mPlaced.find()) {
            try {
                int val = Integer.parseInt(mPlaced.group(1).replaceAll("[,\\.]", ""));
                OBFUtilities.config.serverTotalPlaced = val;
                matched = true;
            } catch (NumberFormatException ignored) {}
        }

        Matcher mKills = KILLS_PATTERN.matcher(text);
        if (mKills.find()) {
            try {
                int val = Integer.parseInt(mKills.group(1).replaceAll("[,\\.]", ""));
                OBFUtilities.config.serverTotalKills = val;
                matched = true;
            } catch (NumberFormatException ignored) {}
        }

        // Cache le gros pavé dégueu mékilémoche
        if (matched) {
            ci.cancel();
        }
    }
}