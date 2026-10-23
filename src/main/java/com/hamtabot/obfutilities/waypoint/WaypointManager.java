package com.hamtabot.obfutilities.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.util.JsonFiles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class WaypointManager {

    private static final Path SAVE_FILE = Paths.get("config/obfutilities_waypoints.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<Waypoint> waypoints = new ArrayList<>();

    public static void load() {
        List<Waypoint> loaded = JsonFiles.read(SAVE_FILE, new TypeToken<List<Waypoint>>(){}.getType(), GSON);
        if (loaded != null) {
            loaded.removeIf(Objects::isNull);
            waypoints = loaded;
        }
    }

    public static void save() {
        JsonFiles.write(SAVE_FILE, waypoints, GSON);
    }

    public static void add(Waypoint wp) {
        waypoints.add(wp);
        save();
    }

    public static void remove(Waypoint wp) {
        if (waypoints.remove(wp)) save();
    }

    public static void importShareData(String raw) {
        String data;
        if (raw.startsWith("OBF-WP:"))       data = raw.substring(7);
        else if (raw.startsWith("OBF-WPS:")) data = raw.substring(8);
        else return;
        for (String part : data.split(";")) {
            Waypoint wp = Waypoint.fromShareString(part);
            if (wp != null) { add(wp); OBFUtilities.LOGGER.info("[OBF] Waypoint importé: " + wp.name); }
        }
    }

    public static List<Waypoint> getAll() { return waypoints; }

    public static List<Waypoint> getForCurrentDimension() {
        String dim = getCurrentDimension();
        return waypoints.stream()
            .filter(w -> dim.equals(w.dimension))
            .collect(Collectors.toList());
    }

    public static String getCurrentDimension() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return "minecraft:overworld";
        RegistryKey<World> key = client.world.getRegistryKey();
        return key.getValue().toString();
    }
}
