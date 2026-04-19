package com.hamtabot.obfutilities.mixin;

import com.hamtabot.obfutilities.waypoint.Waypoint;
import com.hamtabot.obfutilities.waypoint.WaypointManager;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class WaypointChatMixin {

    private static final Pattern WP_SINGLE  = Pattern.compile("\\[OBF-WP:([^\\]]+)\\]");
    private static final Pattern WP_MULTI   = Pattern.compile("\\[OBF-WPS:([^\\]]+)\\]");

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Text transformWaypointMessage(Text original) {
        String raw = original.getString();

        Matcher m1 = WP_SINGLE.matcher(raw);
        if (m1.find()) {
            String data = m1.group(1);
            return buildClickableText(raw, "[OBF-WP:" + data + "]",
                "§b[Cliquer pour importer le waypoint]",
                "OBF-WP:" + data);
        }

        Matcher m2 = WP_MULTI.matcher(raw);
        if (m2.find()) {
            String data = m2.group(1);
            return buildClickableText(raw, "[OBF-WPS:" + data + "]",
                "§b[Cliquer pour importer les waypoints]",
                "OBF-WPS:" + data);
        }

        return original;
    }

    private Text buildClickableText(String original, String waypointPart, String label, String clickData) {
        int idx = original.indexOf(waypointPart);
        MutableText result = Text.literal(idx > 0 ? original.substring(0, idx) : "");

        MutableText button = Text.literal(label)
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/obf_wp_import " + clickData))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Cliquer pour ajouter à vos waypoints")))
                .withColor(net.minecraft.util.Formatting.AQUA)
                .withUnderline(true));

        result.append(button);

        int end = idx + waypointPart.length();
        if (end < original.length()) result.append(Text.literal(original.substring(end)));

        return result;
    }
}
