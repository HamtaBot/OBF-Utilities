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
            if (s == null || s.length() > 512) return null;
            String[] p = s.split("\\|");
            if (p.length < 6) return null;
            int x = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            int z = Integer.parseInt(p[3]);
            if (y < -2048 || y > 2048) return null;
            String dim = p[4].contains(":") ? p[4] : "minecraft:" + p[4];
            if (p[5].length() > 8) return null;
            int color = (int) (Long.parseLong(p[5], 16) & 0xFFFFFFFFL);
            String name = p[0].length() > 64 ? p[0].substring(0, 64) : p[0];
            return new Waypoint(name, x, y, z, dim, color);
        } catch (Exception e) { return null; }
    }

    public String getDimShort() {
        if (dimension == null) return "?";
        return dimension.replace("minecraft:", "");
    }
}