package com.example.client;

import java.util.ArrayList;
import java.util.List;

public class WaypointManager {

    public static class Waypoint {
        public final String name;
        public final double x, y, z;
        public final int color;
        public boolean enabled = true;

        public Waypoint(String name, double x, double y, double z, int color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
        }
    }

    public static final List<Waypoint> waypoints = new ArrayList<>();
    private static int counter = 1;

    public static void addWaypointAtPlayer(double x, double y, double z) {
        int col = ElasticAnimation.getPrimaryColor(counter * 0.7f);
        String name = "Waypoint #" + counter++;
        waypoints.add(new Waypoint(name, Math.floor(x) + 0.5, Math.floor(y), Math.floor(z) + 0.5, col));
    }

    public static void removeWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.remove(index);
        }
    }

    public static void clearAll() {
        waypoints.clear();
        counter = 1;
    }
}