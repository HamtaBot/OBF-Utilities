package com.hamtabot.obfutilities.stats;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import com.hamtabot.obfutilities.ui.Draw;
import com.hamtabot.obfutilities.ui.Theme;

import java.util.regex.Pattern;

public class AdTimer {

    private static final Pattern AD_PATTERN = Pattern.compile("\\[(i|item)\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static long    lastAdTime = 0;
    private static boolean available  = true;

    public static void onChatSent(String message) {
        if (!AD_PATTERN.matcher(message).find()) return;
        lastAdTime = System.currentTimeMillis();
        available  = false;
    }

    public static void tick() {
        if (available || lastAdTime == 0 || getRemainingMs() > 0) return;
        available = true;
        if (OBFUtilities.config.adTitleEnabled) Draw.title("Pub disponible", Theme.DANGER, "Vous pouvez de nouveau l'envoyer");
    }

    public static boolean isAvailable() { return available; }

    public static String savedMessage() {
        return SharedConstants.stripInvalidChars(WHITESPACE.matcher(OBFUtilities.config.adMessage).replaceAll(" ")).trim();
    }

    public static boolean canSendSaved() {
        return available && MinecraftClient.getInstance().player != null && !OBFUtilities.config.adMessage.isBlank();
    }

    public static void sendSaved() {
        if (!canSendSaved()) return;
        String msg = savedMessage();
        if (msg.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        client.inGameHud.getChatHud().addToMessageHistory(msg);
        if (msg.startsWith("/")) client.player.networkHandler.sendChatCommand(msg.substring(1));
        else client.player.networkHandler.sendChatMessage(msg);
    }

    public static long getRemainingMs() {
        return Math.max(0, OBFUtilities.config.adCooldownMs - (System.currentTimeMillis() - lastAdTime));
    }
}
