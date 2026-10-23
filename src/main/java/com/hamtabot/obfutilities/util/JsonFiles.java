package com.hamtabot.obfutilities.util;

import com.google.gson.Gson;
import com.hamtabot.obfutilities.OBFUtilities;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class JsonFiles {

    private JsonFiles() {}

    public static <T> T read(Path file, Type type, Gson gson) {
        if (!Files.exists(file)) return null;
        try {
            return gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), type);
        } catch (Exception e) {
            OBFUtilities.LOGGER.error("[OBF] Fichier illisible " + file + ": " + e.getMessage());
            backup(file);
            return null;
        }
    }

    public static void write(Path file, Object value, Gson gson) {
        try {
            String json = gson.toJson(value);
            if (Files.exists(file) && json.equals(Files.readString(file, StandardCharsets.UTF_8))) return;
            Files.createDirectories(file.getParent());
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            OBFUtilities.LOGGER.error("[OBF] Erreur sauvegarde " + file + ": " + e.getMessage());
        }
    }

    private static void backup(Path file) {
        try {
            Path backup = file.resolveSibling(file.getFileName() + ".broken");
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            OBFUtilities.LOGGER.error("[OBF] Copie sauvegardée dans " + backup);
        } catch (Exception e) {
            OBFUtilities.LOGGER.error("[OBF] Impossible de sauvegarder " + file + ": " + e.getMessage());
        }
    }
}
