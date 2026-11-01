package com.hamtabot.obfutilities.discord;

import com.google.gson.JsonObject;
import com.hamtabot.obfutilities.OBFUtilities;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/** Shows the server on the player's Discord profile for as long as the game is open. */
public final class DiscordPresence {

    // Discord application: its name is the "Joue à ..." line of the profile
    private static final String CLIENT_ID = "1566235998856417420";
    private static final String LOGO_URL = "https://raw.githubusercontent.com/HamtaBot/OBF-Utilities/master/src/main/resources/assets/obfutilities/icon.png";
    private static final String DETAILS = "Joue sur oneblockfrance.fr";
    private static final String STATE = "Utilise OBF Utilities";
    private static final String LOGO_TEXT = "OneBlock France";

    private static final long POLL_NANOS = TimeUnit.SECONDS.toNanos(15);

    private static final long START = System.currentTimeMillis() / 1000;

    private static Thread thread;
    private static DiscordIpc ipc;

    private DiscordPresence() {}

    public static void start() {
        if (CLIENT_ID.isEmpty()) return;
        thread = new Thread(DiscordPresence::run, "OBF Discord presence");
        thread.setDaemon(true);
        thread.start();
    }

    /** Applies the option now instead of at the next poll. */
    public static void wake() {
        if (thread != null) LockSupport.unpark(thread);
    }

    private static void run() {
        while (true) {
            try {
                update();
            } catch (IOException e) {
                OBFUtilities.LOGGER.debug("[OBF] Discord: {}", e.getMessage());
                disconnect();
            }
            LockSupport.parkNanos(POLL_NANOS);
        }
    }

    private static void update() throws IOException {
        if (!OBFUtilities.config.discordPresenceEnabled) {
            disconnect();
            return;
        }
        if (ipc == null) {
            ipc = DiscordIpc.open();
            if (ipc == null) return;
            ipc.exchange(DiscordIpc.OP_HANDSHAKE, handshake());
        }
        // sent again at every poll: this is also what notices that Discord was closed
        ipc.exchange(DiscordIpc.OP_FRAME, setActivity());
    }

    /** Discord removes the presence by itself when its connection closes. */
    private static void disconnect() {
        if (ipc == null) return;
        try {
            ipc.close();
        } catch (IOException ignored) {
        }
        ipc = null;
    }

    private static String handshake() {
        JsonObject json = new JsonObject();
        json.addProperty("v", 1);
        json.addProperty("client_id", CLIENT_ID);
        return json.toString();
    }

    private static String setActivity() {
        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", START);

        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", LOGO_URL);
        assets.addProperty("large_text", LOGO_TEXT);

        JsonObject activity = new JsonObject();
        activity.addProperty("details", DETAILS);
        activity.addProperty("state", STATE);
        activity.add("timestamps", timestamps);
        activity.add("assets", assets);

        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", activity);

        JsonObject json = new JsonObject();
        json.addProperty("cmd", "SET_ACTIVITY");
        json.add("args", args);
        json.addProperty("nonce", UUID.randomUUID().toString());
        return json.toString();
    }
}
