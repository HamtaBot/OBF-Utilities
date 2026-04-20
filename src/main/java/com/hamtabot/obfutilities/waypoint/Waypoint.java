package com.hamtabot.obfutilities.waypoint;

public class Waypoint {
    public String name;
    public int x, y, z;
    public String dimension;
    public int color;
    public boolean enabled;

    public Waypoint() { this.enabled = true; this.color = 0xFF00E5FF; }

    public Waypoint(String name, int x, int y, int z, String dimension, int color) {
        this.name      = name;
        this.x         = x;
        this.y         = y;
        this.z         = z;
        this.dimension = dimension;
        this.color     = color;
        this.enabled   = true;
    }

    public String toShareString() {
        String dim = dimension.replace("minecraft:", "");
        return name + "|" + x + "|" + y + "|" + z + "|" + dim + "|" + Integer.toHexString(color);
    }

    public static Waypoint fromShareString(String s) {
        try {
            String[] p = s.split("\\|");
            if (p.length < 6) return null;
            String dim = p[4].contains(":") ? p[4] : "minecraft:" + p[4];
            return new Waypoint(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]), dim, (int) Long.parseLong(p[5], 16));
        } catch (Exception e) { return null; }
    }

    public String getDimShort() {
        if (dimension == null) return "?";
        return dimension.replace("minecraft:", "");
    }
}