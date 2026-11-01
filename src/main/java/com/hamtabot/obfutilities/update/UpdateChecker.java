package com.hamtabot.obfutilities.update;

import com.google.gson.JsonParser;
import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.ui.Theme;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Tells the player once per launch, in the chat, when a newer version was released on GitHub. */
public final class UpdateChecker {

    private static final String REPO = "HamtaBot/OBF-Utilities";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String PAGE_URL = "https://github.com/" + REPO + "/releases/latest";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    // lets the join messages of the server go by, so this one isn't scrolled away at once
    private static final int NOTIFY_DELAY_TICKS = 100;

    private static final String CURRENT = FabricLoader.getInstance().getModContainer(OBFUtilities.MOD_ID)
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("0");

    private static boolean started, notified;
    private static int waited;
    private static volatile String newer;

    private UpdateChecker() {}

    /** In game only: the request is sent the first time the option is on, never from the title screen. */
    public static void tick(MinecraftClient client) {
        if (notified || !OBFUtilities.config.updateCheckEnabled) return;
        if (!started) {
            started = true;
            Thread thread = new Thread(UpdateChecker::check, "OBF update check");
            thread.setDaemon(true);
            thread.start();
        }
        String version = newer;
        if (version == null || ++waited < NOTIFY_DELAY_TICKS) return;
        notified = true;
        client.player.sendMessage(message(version), false);
    }

    private static void check() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "OBF-Utilities/" + CURRENT)
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                OBFUtilities.LOGGER.debug("[OBF] Update check: HTTP {}", response.statusCode());
                return;
            }
            String latest = JsonParser.parseString(response.body()).getAsJsonObject().get("tag_name").getAsString();
            if (isNewer(latest, CURRENT)) newer = latest;
        } catch (Exception e) {
            // offline, GitHub down or an unexpected answer: the player simply isn't told
            OBFUtilities.LOGGER.debug("[OBF] Update check: {}", e.getMessage());
        }
    }

    /** Compares the numbers only, so "4.0.8" is newer than "4.0.7-BETA" and "v4.0.8" reads like "4.0.8". */
    static boolean isNewer(String latest, String current) {
        int[] a = numbers(latest), b = numbers(current);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0, y = i < b.length ? b[i] : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int[] numbers(String version) {
        String[] parts = version.replaceFirst("^[vV]", "").split("[-+]", 2)[0].split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return new int[0];
            }
        }
        return numbers;
    }

    private static Text message(String version) {
        return Text.literal("[OBF Utilities] ").styled(s -> s.withColor(Theme.ACCENT & 0xFFFFFF))
                .append(Text.literal("Version " + version + " disponible (tu as la " + CURRENT + "). ")
                        .styled(s -> s.withColor(Theme.TEXT & 0xFFFFFF)))
                .append(Text.literal("[Télécharger]").styled(s -> s
                        .withColor(Theme.SUCCESS & 0xFFFFFF)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, PAGE_URL))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Ouvrir la page de la mise à jour")))));
    }
}
