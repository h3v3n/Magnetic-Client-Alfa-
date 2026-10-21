package com.example.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class SmoothUiRenderer {

    private static Identifier roundedBoxTex;
    private static Identifier softShadowTex;
    private static Identifier circleTex;
    private static boolean initialized = false;

    private static final int TEX_SIZE = 512;
    private static final float SDF_RADIUS = 112.0f;

    private static void init() {
        if (initialized) return;
        initialized = true;

        Minecraft mc = Minecraft.getInstance();
        float half = TEX_SIZE / 2.0f;

        // 1. Tekstura 512x512 SDF zaokrąglonego panelu (anti-aliasing)
        NativeImage boxImg = new NativeImage(TEX_SIZE, TEX_SIZE, false);
        float bSize = half - SDF_RADIUS - 4.0f;

        for (int y = 0; y < TEX_SIZE; y++) {
            for (int x = 0; x < TEX_SIZE; x++) {
                float px = Math.abs(x + 0.5f - half) - bSize;
                float py = Math.abs(y + 0.5f - half) - bSize;
                float ox = Math.max(px, 0.0f);
                float oy = Math.max(py, 0.0f);
                float dist = (float) Math.hypot(ox, oy) + Math.min(Math.max(px, py), 0.0f) - SDF_RADIUS;

                float alpha = Math.clamp(0.5f - dist * 0.75f, 0.0f, 1.0f);
                int a = (int) (alpha * 255.0f);
                boxImg.setPixel(x, y, (a << 24) | 0x00FFFFFF);
            }
        }
        DynamicTexture boxDyn = new DynamicTexture(() -> "magnetic_sdf_box", boxImg);
        roundedBoxTex = Identifier.fromNamespaceAndPath("magnetic", "sdf_box");
        mc.getTextureManager().register(roundedBoxTex, boxDyn);

        // 2. Tekstura 512x512 miękkiej poświaty (cień / glow)
        NativeImage shadowImg = new NativeImage(TEX_SIZE, TEX_SIZE, false);
        float sbSize = half - SDF_RADIUS - 64.0f;

        for (int y = 0; y < TEX_SIZE; y++) {
            for (int x = 0; x < TEX_SIZE; x++) {
                float px = Math.abs(x + 0.5f - half) - sbSize;
                float py = Math.abs(y + 0.5f - half) - sbSize;
                float ox = Math.max(px, 0.0f);
                float oy = Math.max(py, 0.0f);
                float dist = (float) Math.hypot(ox, oy) + Math.min(Math.max(px, py), 0.0f) - (SDF_RADIUS * 0.65f);

                float glow = (dist <= 0.0f) ? 1.0f : (float) Math.exp(-(dist * dist) / (38.0f * 38.0f));
                int a = (int) (Math.clamp(glow, 0.0f, 1.0f) * 255.0f);
                shadowImg.setPixel(x, y, (a << 24) | 0x00FFFFFF);
            }
        }
        DynamicTexture shadowDyn = new DynamicTexture(() -> "magnetic_sdf_shadow", shadowImg);
        softShadowTex = Identifier.fromNamespaceAndPath("magnetic", "sdf_shadow");
        mc.getTextureManager().register(softShadowTex, shadowDyn);

        // 3. Tekstura 256x256 idealnego kółka
        int cSize = 256;
        float cHalf = cSize / 2.0f;
        NativeImage circleImg = new NativeImage(cSize, cSize, false);
        for (int y = 0; y < cSize; y++) {
            for (int x = 0; x < cSize; x++) {
                float d = (float) Math.hypot(x + 0.5f - cHalf, y + 0.5f - cHalf);
                float alpha = Math.clamp((cHalf - 2.0f) - d, 0.0f, 1.0f);
                int a = (int) (alpha * 255.0f);
                circleImg.setPixel(x, y, (a << 24) | 0x00FFFFFF);
            }
        }
        DynamicTexture circleDyn = new DynamicTexture(() -> "magnetic_sdf_circle", circleImg);
        circleTex = Identifier.fromNamespaceAndPath("magnetic", "sdf_circle");
        mc.getTextureManager().register(circleTex, circleDyn);
    }

    public static void drawSmoothRoundedRect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        init();
        if (w <= 0 || h <= 0) return;
        int r = Math.min(radius, Math.min(w / 2, h / 2));
        int srcCorner = 120;
        int midSrc = TEX_SIZE - srcCorner * 2;

        blitPart(g, roundedBoxTex, x, y, r, r, 0, 0, srcCorner, srcCorner, TEX_SIZE, TEX_SIZE, color);
        blitPart(g, roundedBoxTex, x + w - r, y, r, r, TEX_SIZE - srcCorner, 0, srcCorner, srcCorner, TEX_SIZE, TEX_SIZE, color);
        blitPart(g, roundedBoxTex, x, y + h - r, r, r, 0, TEX_SIZE - srcCorner, srcCorner, srcCorner, TEX_SIZE, TEX_SIZE, color);
        blitPart(g, roundedBoxTex, x + w - r, y + h - r, r, r, TEX_SIZE - srcCorner, TEX_SIZE - srcCorner, srcCorner, srcCorner, TEX_SIZE, TEX_SIZE, color);

        if (w > r * 2) {
            blitPart(g, roundedBoxTex, x + r, y, w - r * 2, r, srcCorner, 0, midSrc, srcCorner, TEX_SIZE, TEX_SIZE, color);
            blitPart(g, roundedBoxTex, x + r, y + h - r, w - r * 2, r, srcCorner, TEX_SIZE - srcCorner, midSrc, srcCorner, TEX_SIZE, TEX_SIZE, color);
        }
        if (h > r * 2) {
            blitPart(g, roundedBoxTex, x, y + r, r, h - r * 2, 0, srcCorner, srcCorner, midSrc, TEX_SIZE, TEX_SIZE, color);
            blitPart(g, roundedBoxTex, x + w - r, y + r, r, h - r * 2, TEX_SIZE - srcCorner, srcCorner, srcCorner, midSrc, TEX_SIZE, TEX_SIZE, color);
        }
        if (w > r * 2 && h > r * 2) {
            blitPart(g, roundedBoxTex, x + r, y + r, w - r * 2, h - r * 2, srcCorner, srcCorner, midSrc, midSrc, TEX_SIZE, TEX_SIZE, color);
        }
    }

    /**
     * Karta SDF. Glow Boost dodaje poświatę w kolorze akcentu, a glowIntensity
     * steruje jej mocą (0 = brak, 1 = domyślnie, 2 = mocno).
     */
    public static void drawSmoothCard(GuiGraphics g, int x, int y, int w, int h, int radius, int bgColor, int borderColor, int glowColor) {
        init();
        float k = ModConfig.glowIntensity;
        boolean boost = ModConfig.glowBoost && k > 0.001f;

        if (boost) {
            int accent = (borderColor >>> 24) > 0 ? borderColor : ElasticAnimation.getPrimaryColor(0);
            int outerA = Math.min(255, Math.round(55 * k));
            int innerA = Math.min(255, Math.round(105 * k));
            drawSmoothShadow(g, x - 14, y - 14, w + 28, h + 28, radius + 14, ElasticAnimation.withAlpha(accent, outerA));
            drawSmoothShadow(g, x - 7, y - 7, w + 14, h + 14, radius + 7, ElasticAnimation.withAlpha(accent, innerA));
        }

        if ((glowColor >>> 24) > 0) {
            int spread = boost ? 9 : 5;
            int alpha = boost
                    ? Math.min(255, Math.round((glowColor >>> 24) * 1.5f * k))
                    : (glowColor >>> 24);
            int col = (alpha << 24) | (glowColor & 0x00FFFFFF);
            drawSmoothShadow(g, x - spread, y - spread, w + spread * 2, h + spread * 2, radius + spread, col);
        }

        if ((borderColor >>> 24) > 0) {
            drawSmoothRoundedRect(g, x, y, w, h, radius, borderColor);
            drawSmoothRoundedRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(1, radius - 1), bgColor);
        } else {
            drawSmoothRoundedRect(g, x, y, w, h, radius, bgColor);
        }

        if (boost && w > radius * 2 + 4 && h > 10) {
            g.fill(x + radius, y + 1, x + w - radius, y + 2, 0x38FFFFFF);
        }
    }

    public static void drawSmoothShadow(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        init();
        if (w <= 0 || h <= 0) return;
        int r = Math.min(radius, Math.min(w / 2, h / 2));
        int srcCorner = 160;
        int midSrc = TEX_SIZE - srcCorner * 2;

        blitPart(g, softShadowTex, x, y, r, r, 0, 0, srcCorner, srcCorner, TEX_SIZE, TEX_SIZE, color);
        blitPart(g, softShadowTex, x + w - r, y, r, r, TEX_SIZE - srcCorner, 0, srcCorner, srcCorner, TEX_SIZE, TEX_SIZE, color);
        blitPart(g, softShadowTex, x, y + h - r, r, r, 0, TEX_SIZE - srcCorner, srcCorner, srcCorner, TEX_SIZE, TEX_SIZE, color);
        blitPart(g, softShadowTex, x + w - r, y + h - r, r, r, TEX_SIZE - srcCorner, TEX_SIZE - srcCorner, srcCorner, srcCorner, TEX_SIZE, TEX_SIZE, color);

        if (w > r * 2) {
            blitPart(g, softShadowTex, x + r, y, w - r * 2, r, srcCorner, 0, midSrc, srcCorner, TEX_SIZE, TEX_SIZE, color);
            blitPart(g, softShadowTex, x + r, y + h - r, w - r * 2, r, srcCorner, TEX_SIZE - srcCorner, midSrc, srcCorner, TEX_SIZE, TEX_SIZE, color);
        }
        if (h > r * 2) {
            blitPart(g, softShadowTex, x, y + r, r, h - r * 2, 0, srcCorner, srcCorner, midSrc, TEX_SIZE, TEX_SIZE, color);
            blitPart(g, softShadowTex, x + w - r, y + r, r, h - r * 2, TEX_SIZE - srcCorner, srcCorner, srcCorner, midSrc, TEX_SIZE, TEX_SIZE, color);
        }
        if (w > r * 2 && h > r * 2) {
            blitPart(g, softShadowTex, x + r, y + r, w - r * 2, h - r * 2, srcCorner, srcCorner, midSrc, midSrc, TEX_SIZE, TEX_SIZE, color);
        }
    }

    public static void drawSmoothCircle(GuiGraphics g, int x, int y, int size, int color) {
        init();
        blitPart(g, circleTex, x, y, size, size, 0, 0, 256, 256, 256, 256, color);
    }

    private static void blitPart(GuiGraphics g, Identifier tex, int x, int y, int w, int h, float u, float v, int regionW, int regionH, int texW, int texH, int color) {
        if (w <= 0 || h <= 0) return;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, regionW, regionH, texW, texH, color);
    }
}