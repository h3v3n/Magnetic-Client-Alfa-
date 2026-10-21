package com.example.client;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class ModConfig {
    // Zakładka VISUALS
    public static boolean jumpCircles = true;
    public static float jumpCircleSize = 2.4f;

    public static boolean ghostOrbs = true;
    // 0 = Both (Ty + Cel), 1 = Only Target (Tylko bity przeciwnik), 2 = Only Self (Tylko Ty)
    public static int ghostOrbsMode = 0;
    public static final String[] GHOST_MODES = {"Both", "Only Target", "Only Self"};
    public static float ghostOrbThickness = 1.25f;

    public static boolean targetEsp = true;
    public static boolean hitParticles = true;
    public static boolean killEffect = true;
    public static boolean trailLine = true;
    public static boolean blackHoleAura = true;
    public static boolean blackHoleSky = true;
    public static boolean alwaysNight = true;

    // Dodatkowe świecenie paneli, przycisków i menu
    public static boolean glowBoost = true;
    public static float glowIntensity = 1.0f;
    public static int customThemeA = 0xFF9D4EDD;
    public static int customThemeB = 0xFF00F5D4;
    public static boolean customMainMenu = true;

    // Zakładka MODS & HUD
    public static boolean showFps = true;
    public static boolean showCps = true;
    public static boolean showKeystrokes = true;
    public static boolean showCoords = true;
    public static boolean showDirection = true;
    public static boolean showArmorStatus = true;

    // Zoom
    public static boolean zoomEnabled = true;
    public static float zoomFactor = 0.22f;
    public static boolean isZooming = false;
    public static float currentZoomMultiplier = 1.0f;

    // 10 Customowych Celowników w otwieranej liście
    public static boolean customCrosshair = true;
    public static int crosshairStyle = 0;
    public static final String[] CROSSHAIR_NAMES = {
            "Circle + Dot",
            "Dynamic Cross",
            "Cyber Brackets",
            "Clean Dot",
            "Tactical Diamond",
            "Tri-Laser Apex",
            "Sniper Reticle",
            "X-Hitmarker",
            "Square Matrix",
            "Dual Pulse Ring",
            "Chevron Arrow",
            "Orbit Spin",
            "Spark Burst",
            "Glow Dot Cross",
            "Ring Lock",
            "Custom Pixel Art"
    };
    public static float crosshairSize = 5.0f;

    public static boolean showWaypoints = true;

    // Pozycje X/Y na ekranie dla każdego modułu HUD
    public static int fpsX = 12, fpsY = 12;
    public static int cpsX = 12, cpsY = 36;
    public static int keysX = 12, keysY = 62;
    public static int coordsX = 12, coordsY = 134;
    public static int dirX = -1, dirY = 8;
    public static int armorX = -1, armorY = -1;

    // Zakładka VIEWMODEL: Niezależne suwaki dla Prawej i Lewej Ręki
    public static boolean customMainHand = true;
    public static float mainHandX = 0.08f;
    public static float mainHandY = -0.18f;
    public static float mainHandZ = -0.15f;
    public static float mainHandScale = 0.85f;
    public static float mainHandRotY = 0.0f;

    public static boolean customOffHand = true;
    public static float offHandX = -0.08f;
    public static float offHandY = -0.18f;
    public static float offHandZ = -0.15f;
    public static float offHandScale = 0.85f;
    public static float offHandRotY = 0.0f;

    // 26 Motywów Kolorystycznych
    public static int accentTheme = 0;
    public static final String[] THEME_NAMES = {
            "Nursultan Violet",
            "Cyber Neon",
            "Synthwave Sunset",
            "Aurora Borealis",
            "Crimson Fury",
            "Hyperion Gold",
            "Obsidian Rose",
            "Celestial Aqua",
            "Emerald Matrix",
            "Amethyst Void",
            "Ice Frost",
            "Toxic Bio",
            "Sakura Blossom",
            "Blood Moon",
            "Deep Ocean",
            "Neon Evangelion",
            "Cyberpunk 2077",
            "Tokyo Ghoul",
            "Valorant Vandal",
            "Dracula Dark",
            "Miami Vice",
            "Matrix Code",
            "Royal Phantom",
            "Frostbite Zero",
            "Sunset Blvd",
            "Chroma RGB",
            "Custom Theme"
    };

    public static void resetMainHand() {
        mainHandX = 0.0f;
        mainHandY = 0.0f;
        mainHandZ = 0.0f;
        mainHandScale = 1.0f;
        mainHandRotY = 0.0f;
    }

    public static void resetOffHand() {
        offHandX = 0.0f;
        offHandY = 0.0f;
        offHandZ = 0.0f;
        offHandScale = 1.0f;
        offHandRotY = 0.0f;
    }

    public static void resetHudPositions(int screenW, int screenH) {
        fpsX = 12; fpsY = 12;
        cpsX = 12; cpsY = 36;
        keysX = 12; keysY = 62;
        coordsX = 12; coordsY = 134;
        dirX = (screenW - 170) / 2; dirY = 8;
        armorX = (screenW / 2) + 98; armorY = screenH - 86;
    }

    private static File getConfigFile() {
        File dir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "magnetic_client.properties");
    }

    public static void save() {
        try {
            Properties props = new Properties();
            props.setProperty("jumpCircles", String.valueOf(jumpCircles));
            props.setProperty("jumpCircleSize", String.valueOf(jumpCircleSize));
            props.setProperty("ghostOrbs", String.valueOf(ghostOrbs));
            props.setProperty("ghostOrbsMode", String.valueOf(ghostOrbsMode));
            props.setProperty("ghostOrbThickness", String.valueOf(ghostOrbThickness));
            props.setProperty("targetEsp", String.valueOf(targetEsp));
            props.setProperty("hitParticles", String.valueOf(hitParticles));
            props.setProperty("killEffect", String.valueOf(killEffect));
            props.setProperty("trailLine", String.valueOf(trailLine));
            props.setProperty("blackHoleAura", String.valueOf(blackHoleAura));
            props.setProperty("blackHoleSky", String.valueOf(blackHoleSky));
            props.setProperty("alwaysNight", String.valueOf(alwaysNight));
            props.setProperty("glowBoost", String.valueOf(glowBoost));
            props.setProperty("glowIntensity", String.valueOf(glowIntensity));
            props.setProperty("customThemeA", String.valueOf(customThemeA));
            props.setProperty("customThemeB", String.valueOf(customThemeB));
            props.setProperty("customPixels", CustomCrosshair.encode());
            props.setProperty("customMainMenu", String.valueOf(customMainMenu));

            props.setProperty("showFps", String.valueOf(showFps));
            props.setProperty("showCps", String.valueOf(showCps));
            props.setProperty("showKeystrokes", String.valueOf(showKeystrokes));
            props.setProperty("showCoords", String.valueOf(showCoords));
            props.setProperty("showDirection", String.valueOf(showDirection));
            props.setProperty("showArmorStatus", String.valueOf(showArmorStatus));

            props.setProperty("zoomEnabled", String.valueOf(zoomEnabled));
            props.setProperty("zoomFactor", String.valueOf(zoomFactor));
            props.setProperty("customCrosshair", String.valueOf(customCrosshair));
            props.setProperty("crosshairStyle", String.valueOf(crosshairStyle));
            props.setProperty("crosshairSize", String.valueOf(crosshairSize));
            props.setProperty("showWaypoints", String.valueOf(showWaypoints));

            props.setProperty("fpsX", String.valueOf(fpsX));
            props.setProperty("fpsY", String.valueOf(fpsY));
            props.setProperty("cpsX", String.valueOf(cpsX));
            props.setProperty("cpsY", String.valueOf(cpsY));
            props.setProperty("keysX", String.valueOf(keysX));
            props.setProperty("keysY", String.valueOf(keysY));
            props.setProperty("coordsX", String.valueOf(coordsX));
            props.setProperty("coordsY", String.valueOf(coordsY));
            props.setProperty("dirX", String.valueOf(dirX));
            props.setProperty("dirY", String.valueOf(dirY));
            props.setProperty("armorX", String.valueOf(armorX));
            props.setProperty("armorY", String.valueOf(armorY));

            props.setProperty("customMainHand", String.valueOf(customMainHand));
            props.setProperty("mainHandX", String.valueOf(mainHandX));
            props.setProperty("mainHandY", String.valueOf(mainHandY));
            props.setProperty("mainHandZ", String.valueOf(mainHandZ));
            props.setProperty("mainHandScale", String.valueOf(mainHandScale));
            props.setProperty("mainHandRotY", String.valueOf(mainHandRotY));

            props.setProperty("customOffHand", String.valueOf(customOffHand));
            props.setProperty("offHandX", String.valueOf(offHandX));
            props.setProperty("offHandY", String.valueOf(offHandY));
            props.setProperty("offHandZ", String.valueOf(offHandZ));
            props.setProperty("offHandScale", String.valueOf(offHandScale));
            props.setProperty("offHandRotY", String.valueOf(offHandRotY));

            props.setProperty("accentTheme", String.valueOf(accentTheme));

            try (FileOutputStream fos = new FileOutputStream(getConfigFile())) {
                props.store(fos, "Magnetic Client Configuration");
            }
        } catch (Exception ignored) {
        }
    }

    public static void load() {
        File file = getConfigFile();
        if (!file.exists()) return;
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties p = new Properties();
            p.load(fis);

            jumpCircles = Boolean.parseBoolean(p.getProperty("jumpCircles", "true"));
            jumpCircleSize = Float.parseFloat(p.getProperty("jumpCircleSize", "2.4"));
            ghostOrbs = Boolean.parseBoolean(p.getProperty("ghostOrbs", "true"));
            ghostOrbsMode = Integer.parseInt(p.getProperty("ghostOrbsMode", "0"));
            ghostOrbThickness = Float.parseFloat(p.getProperty("ghostOrbThickness", "1.25"));
            targetEsp = Boolean.parseBoolean(p.getProperty("targetEsp", "true"));
            hitParticles = Boolean.parseBoolean(p.getProperty("hitParticles", "true"));
            killEffect = Boolean.parseBoolean(p.getProperty("killEffect", "true"));
            trailLine = Boolean.parseBoolean(p.getProperty("trailLine", "true"));
            blackHoleAura = Boolean.parseBoolean(p.getProperty("blackHoleAura", "true"));
            blackHoleSky = Boolean.parseBoolean(p.getProperty("blackHoleSky", "true"));
            alwaysNight = Boolean.parseBoolean(p.getProperty("alwaysNight", "true"));
            glowBoost = Boolean.parseBoolean(p.getProperty("glowBoost", "true"));
            glowIntensity = Float.parseFloat(p.getProperty("glowIntensity", "1.0"));
            customMainMenu = Boolean.parseBoolean(p.getProperty("customMainMenu", "true"));

            showFps = Boolean.parseBoolean(p.getProperty("showFps", "true"));
            showCps = Boolean.parseBoolean(p.getProperty("showCps", "true"));
            showKeystrokes = Boolean.parseBoolean(p.getProperty("showKeystrokes", "true"));
            showCoords = Boolean.parseBoolean(p.getProperty("showCoords", "true"));
            showDirection = Boolean.parseBoolean(p.getProperty("showDirection", "true"));
            showArmorStatus = Boolean.parseBoolean(p.getProperty("showArmorStatus", "true"));

            zoomEnabled = Boolean.parseBoolean(p.getProperty("zoomEnabled", "true"));
            zoomFactor = Float.parseFloat(p.getProperty("zoomFactor", "0.22"));
            customCrosshair = Boolean.parseBoolean(p.getProperty("customCrosshair", "true"));
            crosshairStyle = Integer.parseInt(p.getProperty("crosshairStyle", "0"));
            crosshairSize = Float.parseFloat(p.getProperty("crosshairSize", "5.0"));
            showWaypoints = Boolean.parseBoolean(p.getProperty("showWaypoints", "true"));

            fpsX = Integer.parseInt(p.getProperty("fpsX", "12"));
            fpsY = Integer.parseInt(p.getProperty("fpsY", "12"));
            cpsX = Integer.parseInt(p.getProperty("cpsX", "12"));
            cpsY = Integer.parseInt(p.getProperty("cpsY", "36"));
            keysX = Integer.parseInt(p.getProperty("keysX", "12"));
            keysY = Integer.parseInt(p.getProperty("keysY", "62"));
            coordsX = Integer.parseInt(p.getProperty("coordsX", "12"));
            coordsY = Integer.parseInt(p.getProperty("coordsY", "134"));
            dirX = Integer.parseInt(p.getProperty("dirX", "-1"));
            dirY = Integer.parseInt(p.getProperty("dirY", "8"));
            armorX = Integer.parseInt(p.getProperty("armorX", "-1"));
            armorY = Integer.parseInt(p.getProperty("armorY", "-1"));

            customMainHand = Boolean.parseBoolean(p.getProperty("customMainHand", "true"));
            mainHandX = Float.parseFloat(p.getProperty("mainHandX", "0.08"));
            mainHandY = Float.parseFloat(p.getProperty("mainHandY", "-0.18"));
            mainHandZ = Float.parseFloat(p.getProperty("mainHandZ", "-0.15"));
            mainHandScale = Float.parseFloat(p.getProperty("mainHandScale", "0.85"));
            mainHandRotY = Float.parseFloat(p.getProperty("mainHandRotY", "0.0"));

            customOffHand = Boolean.parseBoolean(p.getProperty("customOffHand", "true"));
            offHandX = Float.parseFloat(p.getProperty("offHandX", "-0.08"));
            offHandY = Float.parseFloat(p.getProperty("offHandY", "-0.18"));
            offHandZ = Float.parseFloat(p.getProperty("offHandZ", "-0.15"));
            offHandScale = Float.parseFloat(p.getProperty("offHandScale", "0.85"));
            offHandRotY = Float.parseFloat(p.getProperty("offHandRotY", "0.0"));

            accentTheme = Math.clamp(Integer.parseInt(p.getProperty("accentTheme", "0")), 0, THEME_NAMES.length - 1);
            customThemeA = Integer.parseInt(p.getProperty("customThemeA", String.valueOf(0xFF9D4EDD)));
            customThemeB = Integer.parseInt(p.getProperty("customThemeB", String.valueOf(0xFF00F5D4)));
            CustomCrosshair.decode(p.getProperty("customPixels", ""));
        } catch (Exception ignored) {
        }
    }
}