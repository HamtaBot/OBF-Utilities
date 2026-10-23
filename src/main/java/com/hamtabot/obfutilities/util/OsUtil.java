package com.hamtabot.obfutilities.util;

import com.hamtabot.obfutilities.OBFUtilities;

import java.io.File;

public class OsUtil {

    public static void openFolder(File dir) {
        try {
            String path = dir.getAbsoluteFile().getCanonicalPath();
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) new ProcessBuilder("explorer.exe", path).start();
            else if (os.contains("mac")) new ProcessBuilder("open", path).start();
            else new ProcessBuilder("xdg-open", path).start();
        } catch (Exception e) {
            OBFUtilities.LOGGER.error("[OBF] " + e.getMessage());
        }
    }
}
