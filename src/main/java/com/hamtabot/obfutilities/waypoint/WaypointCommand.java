package com.hamtabot.obfutilities.waypoint;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class WaypointCommand {

    private static final int MAX_DATA_LENGTH = 4096;

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("obf_wp_import")
                        .then(ClientCommandManager.argument("data", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String raw = StringArgumentType.getString(ctx, "data");
                                    if (raw == null || raw.length() > MAX_DATA_LENGTH) return 0;
                                    WaypointManager.importShareData(raw);
                                    return 1;
                                }))));
    }
}
