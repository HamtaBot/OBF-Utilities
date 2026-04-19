package com.hamtabot.obfutilities.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hamtabot.obfutilities.OBFUtilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class WaypointManager {

    private static final Path SAVE_FILE = Paths.get("config/obfutilities_waypoints.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<Waypoint> waypoints = new ArrayList<>();

    public static void load() {
        if (!Files.exists(SAVE_FILE)) return;
        try (Reader r = Files.newBufferedReader(SAVE_FILE)) {
            Type type = new TypeToken<List<Waypoint>>(){}.getType();
            List<Waypoint> loaded = GSON.fromJson(r, type);
            if (loaded != null) waypoints = loaded;
        } catch (Exception e) {
            OBFUtilities.LOGGER.error("[OBF] Erreur chargement waypoints: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(SAVE_FILE.getParent());
            try (Writer w = Files.newBufferedWriter(SAVE_FILE)) {
                GSON.toJson(waypoints, w);
            }
        } catch (Exception e) {
            OBFUtilities.LOGGER.error("[OBF] Erreur sauvegarde waypoints: " + e.getMessage());
        }
    }

    public static void add(Waypoint wp) {
        waypoints.add(wp);
        save();
    }

    public static void remove(int index) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.remove(index);
            save();
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
