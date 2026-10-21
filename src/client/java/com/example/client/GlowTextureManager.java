package com.example.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class GlowTextureManager {

    private static Identifier glowOrbTex;
    private static Identifier glowRingTex;
    private static boolean initialized = false;

    private static void init() {
        if (initialized) return;
        initialized = true;

        Minecraft mc = Minecraft.getInstance();

        // 1. Prawdziwa tekstura 256x256 z rozkładem Gaussa (Idealnie gładka kula światła Nursultan)
        int size = 256;
        NativeImage orbImg = new NativeImage(size, size, false);
        float center = size / 2.0f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f - center) / center;
                float dy = (y + 0.5f - center) / center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist >= 1.0f) {
                    orbImg.setPixel(x, y, 0x00000000);
                } else {
                    float gaussian = (float) Math.exp(-dist * dist * 5.2f);
                    float edgeFade = (float) Math.pow(1.0f - dist, 2.2f);
                    float core = (float) Math.pow(Math.max(0.0f, 1.0f - dist * 2.2f), 3.0f);

                    float totalAlpha = Math.clamp(gaussian * edgeFade + core * 0.45f, 0.0f, 1.0f);
                    int a = (int) (totalAlpha * 255.0f);
                    int argb = (a << 24) | 0x00FFFFFF;
                    orbImg.setPixel(x, y, argb);
                }
            }
        }

        DynamicTexture orbDyn = new DynamicTexture(() -> "magnetic_glow_orb", orbImg);
        glowOrbTex = Identifier.fromNamespaceAndPath("magnetic", "glow_orb");
        mc.getTextureManager().register(glowOrbTex, orbDyn);

        // 2. Prawdziwa tekstura 256x256 gładkiego pierścienia fali uderzeniowej
        NativeImage ringImg = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f - center) / center;
                float dy = (y + 0.5f - center) / center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist >= 1.0f) {
                    ringImg.setPixel(x, y, 0x00000000);
                } else {
                    float ringPeak = (float) Math.exp(-Math.pow((dist - 0.82f) / 0.09f, 2.0));
                    float innerGlow = (dist < 0.82f) ? (float) Math.pow(dist / 0.82f, 3.5) * 0.35f : 0.0f;
                    float outerCutoff = Math.clamp((1.0f - dist) / 0.12f, 0.0f, 1.0f);

                    float alpha = Math.clamp((ringPeak + innerGlow) * outerCutoff, 0.0f, 1.0f);
                    int a = (int) (alpha * 255.0f);
                    int argb = (a << 24) | 0x00FFFFFF;
                    ringImg.setPixel(x, y, argb);
                }
            }
        }

        DynamicTexture ringDyn = new DynamicTexture(() -> "magnetic_glow_ring", ringImg);
        glowRingTex = Identifier.fromNamespaceAndPath("magnetic", "glow_ring");
        mc.getTextureManager().register(glowRingTex, ringDyn);
    }

    public static RenderType getOrbRenderType() {
        init();
        return RenderTypes.beaconBeam(glowOrbTex, true);
    }

    public static RenderType getRingRenderType() {
        init();
        return RenderTypes.beaconBeam(glowRingTex, true);
    }
}