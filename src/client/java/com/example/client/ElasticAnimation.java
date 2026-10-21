package com.example.client;

import java.awt.Color;

public class ElasticAnimation {

    public static final int CHROMA_THEME = 25;
    public static final int CUSTOM_THEME = 26;

    private static final int[][] PALETTES = {
            {0xFF9D4EDD, 0xFF00F5D4}, // 0: Nursultan Violet
            {0xFF00E5FF, 0xFFD500F9}, // 1: Cyber Neon
            {0xFFFF2A85, 0xFFFFBE0B}, // 2: Synthwave Sunset
            {0xFF00F5D4, 0xFF7B2CBF}, // 3: Aurora Borealis
            {0xFFFF1744, 0xFFFF9100}, // 4: Crimson Fury
            {0xFFFFD700, 0xFFFF5400}, // 5: Hyperion Gold
            {0xFFFF0A54, 0xFF800F2F}, // 6: Obsidian Rose
            {0xFF4CC9F0, 0xFF4361EE}, // 7: Celestial Aqua
            {0xFF10B981, 0xFF06B6D4}, // 8: Emerald Matrix
            {0xFFC77DFF, 0xFF3C096C}, // 9: Amethyst Void
            {0xFFCAF0F8, 0xFF0077B6}, // 10: Ice Frost
            {0xFFAACC00, 0xFF00B4D8}, // 11: Toxic Bio
            {0xFFFF85A1, 0xFFFBB1BD}, // 12: Sakura Blossom
            {0xFFD90429, 0xFF2B2D42}, // 13: Blood Moon
            {0xFF00B4D8, 0xFF03045E}, // 14: Deep Ocean
            {0xFF39FF14, 0xFF9D00FF}, // 15: Neon Evangelion
            {0xFFFCEE0A, 0xFF00F0FF}, // 16: Cyberpunk 2077
            {0xFFE60026, 0xFF1A1A1D}, // 17: Tokyo Ghoul
            {0xFFFF4655, 0xFF7FFFD4}, // 18: Valorant Vandal
            {0xFFBD93F9, 0xFFFF79C6}, // 19: Dracula Dark
            {0xFF00F5D4, 0xFFFF5D8F}, // 20: Miami Vice
            {0xFF00FF41, 0xFF008F11}, // 21: Matrix Code
            {0xFFE0AAFF, 0xFFD4AF37}, // 22: Royal Phantom
            {0xFF80FFDB, 0xFF5390D9}, // 23: Frostbite Zero
            {0xFFFF6B6B, 0xFF4ECDC4}  // 24: Sunset Blvd
    };

    private static int chroma(double offset) {
        float hue = (float) ((System.currentTimeMillis() * 0.00025 + offset) % 1.0);
        return Color.HSBtoRGB(hue, 0.85f, 1.0f) | 0xFF000000;
    }

    public static int getThemePreviewColor1(int themeIndex) {
        if (themeIndex == CHROMA_THEME) return chroma(0.0);
        if (themeIndex == CUSTOM_THEME) return ModConfig.customThemeA;
        int idx = Math.clamp(themeIndex, 0, PALETTES.length - 1);
        return PALETTES[idx][0];
    }

    public static int getThemePreviewColor2(int themeIndex) {
        if (themeIndex == CHROMA_THEME) return chroma(0.35);
        if (themeIndex == CUSTOM_THEME) return ModConfig.customThemeB;
        int idx = Math.clamp(themeIndex, 0, PALETTES.length - 1);
        return PALETTES[idx][1];
    }

    public static float getProgress(long startTime, float durationMs) {
        float t = Math.clamp((System.currentTimeMillis() - startTime) / durationMs, 0.0f, 1.0f);
        if (t >= 1.0f) return 1.0f;
        float c4 = (float) ((2.0 * Math.PI) / 3.0);
        return (float) (Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * c4) + 1.0);
    }

    public static float lerp(float current, float target, float speed) {
        return current + (target - current) * Math.clamp(speed, 0.0f, 1.0f);
    }

    public static int getPrimaryColor(float offset) {
        int theme = ModConfig.accentTheme;
        if (theme == CHROMA_THEME) {
            float hue = (float) ((System.currentTimeMillis() * 0.00022 + offset * 0.08) % 1.0);
            return Color.HSBtoRGB(hue, 0.82f, 1.0f) | 0xFF000000;
        }

        int c1;
        int c2;
        if (theme == CUSTOM_THEME) {
            c1 = ModConfig.customThemeA;
            c2 = ModConfig.customThemeB;
        } else {
            int idx = Math.clamp(theme, 0, PALETTES.length - 1);
            c1 = PALETTES[idx][0];
            c2 = PALETTES[idx][1];
        }
        float wave = (float) (Math.sin(System.currentTimeMillis() * 0.0025 + offset) * 0.25 + 0.25);
        return blendColors(c1, c2, wave);
    }

    public static int getSecondaryColor() {
        int theme = ModConfig.accentTheme;
        if (theme == CHROMA_THEME) return getPrimaryColor(2.5f);
        if (theme == CUSTOM_THEME) return ModConfig.customThemeB;
        int idx = Math.clamp(theme, 0, PALETTES.length - 1);
        return PALETTES[idx][1];
    }

    public static int blendColors(int c1, int c2, float ratio) {
        ratio = Math.clamp(ratio, 0.0f, 1.0f);
        int r = (int) (getR(c1) + (getR(c2) - getR(c1)) * ratio);
        int g = (int) (getG(c1) + (getG(c2) - getG(c1)) * ratio);
        int b = (int) (getB(c1) + (getB(c2) - getB(c1)) * ratio);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    public static int getCyberColor(float offset) {
        return getPrimaryColor(offset);
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    public static int getR(int color) { return (color >> 16) & 0xFF; }
    public static int getG(int color) { return (color >> 8) & 0xFF; }
    public static int getB(int color) { return color & 0xFF; }
}