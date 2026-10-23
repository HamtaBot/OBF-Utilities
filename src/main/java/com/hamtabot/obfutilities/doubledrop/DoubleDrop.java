package com.hamtabot.obfutilities.doubledrop;

import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class DoubleDrop {

    private static final long WINDOW_MS = 500;

    private static long   lastPressTime = 0;
    private static String lastTarget    = null;

    // Renvoie true si le drop doit avoir lieu (2e appui sur la même cible dans la fenêtre)
    public static boolean allowDrop(String target) {
        if (!OBFUtilities.config.doubleDropEnabled) return true;

        long now = System.currentTimeMillis();
        if (target.equals(lastTarget) && now - lastPressTime <= WINDOW_MS) {
            lastTarget = null;
            lastPressTime = 0;
            return true;
        }

        lastTarget = target;
        lastPressTime = now;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§eAppuyez à nouveau pour drop"), true);
        }
        return false;
    }
}
