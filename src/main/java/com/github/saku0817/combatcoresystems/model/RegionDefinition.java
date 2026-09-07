package com.github.saku0817.combatcoresystems.model;

import org.bukkit.Location;

import java.util.Map;

public record RegionDefinition(String id, String world, int minX, Integer minY, int minZ,
                               int maxX, Integer maxY, int maxZ, Map<String, String> flags) {
    public boolean contains(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(world)) return false;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ
                && (minY == null || (y >= minY && y <= maxY));
    }

    public long volume() {
        long height = minY == null ? Integer.MAX_VALUE : (long) maxY - minY + 1;
        return ((long) maxX - minX + 1) * ((long) maxZ - minZ + 1) * height;
    }
}
