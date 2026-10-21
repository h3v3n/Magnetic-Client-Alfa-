package com.example.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class MagneticWorldRenderer {

    private static final List<GroundWave> activeWaves = new ArrayList<>();
    private static final List<HitNova> activeNovas = new ArrayList<>();
    private static final List<KillVortex> activeVortexes = new ArrayList<>();
    private static final List<TrailPoint> speedTrail = new ArrayList<>();

    private static double smoothPlayerX = 0, smoothPlayerY = 0, smoothPlayerZ = 0;
    private static boolean initializedSmoothPos = false;
    private static int savedBaseFov = -1;

    public static class GroundWave {
        public final double x, y, z;
        public int age = 0;
        public final int maxAge;
        public final float maxRadius;
        public final boolean isLanding;

        public GroundWave(double x, double y, double z, float maxRadius, int maxAge, boolean isLanding) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.maxRadius = maxRadius;
            this.maxAge = maxAge;
            this.isLanding = isLanding;
        }

        public float getProgress(float tickDelta) {
            return Math.min(1.0f, (age + tickDelta) / (float) maxAge);
        }
    }

    public static class HitNova {
        public final double x, y, z;
        public int age = 0;
        public final int maxAge = 20;

        public HitNova(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float getProgress(float tickDelta) {
            return Math.min(1.0f, (age + tickDelta) / (float) maxAge);
        }
    }

    public static class KillVortex {
        public final double x, y, z;
        public int age = 0;
        public final int maxAge = 45;

        public KillVortex(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float getProgress(float tickDelta) {
            return Math.min(1.0f, (age + tickDelta) / (float) maxAge);
        }
    }

    public static class TrailPoint {
        public final double x, y, z;
        public int age = 0;
        public final int maxAge = 26;

        public TrailPoint(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static void addJumpRipple(double x, double y, double z) {
        addJumpRipple(x, y, z, false);
    }

    public static void addJumpRipple(double x, double y, double z, boolean isLanding) {
        if (!ModConfig.jumpCircles) return;
        if (activeWaves.size() > 14) activeWaves.remove(0);
        float baseSize = ModConfig.jumpCircleSize;
        float radius = isLanding ? baseSize * 1.35f : baseSize;
        int duration = isLanding ? 28 : 22;
        activeWaves.add(new GroundWave(x, y + 0.025, z, radius, duration, isLanding));
    }

    public static void addKillShockwave(double x, double y, double z) {
        if (ModConfig.killEffect) {
            activeVortexes.add(new KillVortex(x, y, z));
        }
        if (ModConfig.jumpCircles) {
            activeWaves.add(new GroundWave(x, y + 0.03, z, ModConfig.jumpCircleSize * 2.1f, 34, true));
        }
    }

    public static void addHitBurst(double x, double y, double z) {
        if (!ModConfig.hitParticles) return;
        if (activeNovas.size() > 12) activeNovas.remove(0);
        activeNovas.add(new HitNova(x, y, z));
    }

    public static void tickEffects(Minecraft client) {
        if (client.player == null || client.level == null) return;

        activeWaves.removeIf(w -> ++w.age >= w.maxAge);
        activeNovas.removeIf(n -> ++n.age >= n.maxAge);
        activeVortexes.removeIf(v -> ++v.age >= v.maxAge);
        speedTrail.removeIf(t -> ++t.age >= t.maxAge);

        if (ModConfig.trailLine && (client.player.isSprinting() || client.player.getDeltaMovement().horizontalDistanceSqr() > 0.012)) {
            double px = client.player.getX();
            double py = client.player.getY() + 0.10;
            double pz = client.player.getZ();
            if (speedTrail.isEmpty()) {
                speedTrail.add(new TrailPoint(px, py, pz));
            } else {
                TrailPoint last = speedTrail.get(speedTrail.size() - 1);
                double distSq = (px - last.x) * (px - last.x) + (py - last.y) * (py - last.y) + (pz - last.z) * (pz - last.z);
                if (distSq > 0.01) {
                    speedTrail.add(new TrailPoint(px, py, pz));
                }
            }
        }
    }

    private static void updateSmoothZoom(Minecraft client) {
        float targetZoom = ModConfig.isZooming ? ModConfig.zoomFactor : 1.0f;
        ModConfig.currentZoomMultiplier = ElasticAnimation.lerp(ModConfig.currentZoomMultiplier, targetZoom, 0.18f);

        int currentFov = client.options.fov().get();
        if (ModConfig.isZooming) {
            if (savedBaseFov == -1) {
                savedBaseFov = currentFov;
            }
            int zoomedFov = Math.clamp(Math.round(savedBaseFov * ModConfig.currentZoomMultiplier), 30, 110);
            if (currentFov != zoomedFov) {
                client.options.fov().set(zoomedFov);
            }
        } else if (savedBaseFov != -1) {
            if (Math.abs(ModConfig.currentZoomMultiplier - 1.0f) < 0.02f) {
                client.options.fov().set(savedBaseFov);
                savedBaseFov = -1;
            } else {
                int restoringFov = Math.clamp(Math.round(savedBaseFov * ModConfig.currentZoomMultiplier), 30, 110);
                client.options.fov().set(restoringFov);
            }
        }
    }

    private static float smoothStep(float t) {
        t = Math.clamp(t, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static double catmullRom(float t, double p0, double p1, double p2, double p3) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2.0 * p1)
                + (-p0 + p2) * t
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    public static void renderGeometryRipples() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        // Wymuszenie nocy w każdej klatce renderowania (nawet gdy serwer wysyła pakiety z dniem!)
        if (ModConfig.alwaysNight || ModConfig.blackHoleSky) {
            client.level.getLevelData().setGameTime(18000L);
        }

        updateSmoothZoom(client);

        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        Quaternionf camRot = camera.rotation();
        float tickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float time = (System.currentTimeMillis() % 1000000L) / 1000.0f;

        double rawPx = Mth.lerp(tickDelta, client.player.xo, client.player.getX());
        double rawPy = Mth.lerp(tickDelta, client.player.yo, client.player.getY());
        double rawPz = Mth.lerp(tickDelta, client.player.zo, client.player.getZ());

        if (!initializedSmoothPos || Math.abs(rawPx - smoothPlayerX) > 10.0) {
            smoothPlayerX = rawPx;
            smoothPlayerY = rawPy;
            smoothPlayerZ = rawPz;
            initializedSmoothPos = true;
        } else {
            smoothPlayerX += (rawPx - smoothPlayerX) * 0.20;
            smoothPlayerY += (rawPy - smoothPlayerY) * 0.20;
            smoothPlayerZ += (rawPz - smoothPlayerZ) * 0.20;
        }

        int c1 = ElasticAnimation.getPrimaryColor(0);
        int c2 = ElasticAnimation.getSecondaryColor();
        int r1 = ElasticAnimation.getR(c1), g1 = ElasticAnimation.getG(c1), b1 = ElasticAnimation.getB(c1);
        int r2 = ElasticAnimation.getR(c2), g2 = ElasticAnimation.getG(c2), b2 = ElasticAnimation.getB(c2);

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));

        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        LivingEntity target = MagneticClientClient.getActiveTarget();

        // =========================================================================================
        // ETAP 1: TEKSTURA GAUSSA HD (Grube Pełne Duszki 3D, Target ESP, Hit Particles, Trail Line)
        // =========================================================================================
        RenderType orbType = GlowTextureManager.getOrbRenderType();
        VertexConsumer orbConsumer = bufferSource.getBuffer(orbType);

        // 1. GRUBE, PEŁNE DUSZKI 3D (Z wyborem: Both / Only Target / Only Self)
        if (ModConfig.ghostOrbs) {
            float thick = ModConfig.ghostOrbThickness;
            boolean showOnSelf = (ModConfig.ghostOrbsMode == 0 || ModConfig.ghostOrbsMode == 2);
            boolean showOnTarget = (ModConfig.ghostOrbsMode == 0 || ModConfig.ghostOrbsMode == 1);

            if (showOnSelf) {
                poseStack.pushPose();
                poseStack.translate(smoothPlayerX - cameraPos.x, (smoothPlayerY - cameraPos.y) + 1.0, smoothPlayerZ - cameraPos.z);
                renderVolumetricThickGhosts(poseStack, orbConsumer, camRot, time, 1.32f, 0.44f * thick, 2.3f, r1, g1, b1, r2, g2, b2, 255);
                poseStack.popPose();
            }

            if (showOnTarget && target != null && target.isAlive()) {
                double tx = Mth.lerp(tickDelta, target.xo, target.getX());
                double ty = Mth.lerp(tickDelta, target.yo, target.getY());
                double tz = Mth.lerp(tickDelta, target.zo, target.getZ());
                float targetCenterY = target.getBbHeight() * 0.55f;
                float targetOrbitR = target.getBbWidth() * 0.90f + 0.35f;

                poseStack.pushPose();
                poseStack.translate(tx - cameraPos.x, (ty - cameraPos.y) + targetCenterY, tz - cameraPos.z);
                renderVolumetricThickGhosts(poseStack, orbConsumer, camRot, time * 1.55f + 1.4f, targetOrbitR, 0.40f * thick, 3.3f, r2, g2, b2, r1, g1, b1, 255);
                poseStack.popPose();
            }
        }

        // 2. PLAYER ORBIT AURA
        if (ModConfig.blackHoleAura) {
            poseStack.pushPose();
            poseStack.translate(smoothPlayerX - cameraPos.x, (smoothPlayerY - cameraPos.y) + 0.95, smoothPlayerZ - cameraPos.z);
            int nodes = 36;
            for (int i = 0; i < nodes; i++) {
                double a = (i * 2.0 * Math.PI / nodes) + time * 1.8;
                float rad = 1.25f + (float) Math.sin(a * 3.0 - time * 3.5) * 0.22f;
                float ox = (float) Math.cos(a) * rad;
                float oz = (float) Math.sin(a) * rad;
                float oy = (float) Math.cos(a * 2.0 + time * 2.8) * 0.42f;

                float pulse = (float) (Math.sin(time * 5.0 + i * 0.5) * 0.5 + 0.5);
                int cr = (int) Mth.lerp(pulse, r1, r2);
                int cg = (int) Mth.lerp(pulse, g1, g2);
                int cb = (int) Mth.lerp(pulse, b1, b2);
                drawBillboardOrb(poseStack, orbConsumer, camRot, ox, oy, oz, 0.18f + pulse * 0.08f, cr, cg, cb, 215);
            }
            poseStack.popPose();
        }

        // 3. TARGET ESP RING
        if (ModConfig.targetEsp && target != null && target.isAlive()) {
            double tx = Mth.lerp(tickDelta, target.xo, target.getX());
            double ty = Mth.lerp(tickDelta, target.yo, target.getY());
            double tz = Mth.lerp(tickDelta, target.zo, target.getZ());
            float h = target.getBbHeight();
            float rad = target.getBbWidth() * 0.88f + 0.25f;
            float scanY = (float) ((Math.sin(time * 3.6) * 0.5 + 0.5) * h);

            poseStack.pushPose();
            poseStack.translate(tx - cameraPos.x, ty - cameraPos.y, tz - cameraPos.z);
            int ringOrbs = 32;
            for (int i = 0; i < ringOrbs; i++) {
                double ang = (i * 2.0 * Math.PI / ringOrbs) + time * 3.2;
                float ox = (float) Math.cos(ang) * rad;
                float oz = (float) Math.sin(ang) * rad;
                float waveY = scanY + (float) Math.sin(ang * 2.0 + time * 6.0) * 0.14f;

                boolean alt = (i % 2 == 0);
                drawBillboardOrb(poseStack, orbConsumer, camRot, ox, waveY, oz, 0.20f, alt ? r1 : r2, alt ? g1 : g2, alt ? b1 : b2, 240);
            }
            poseStack.popPose();
        }

        // 4. HIT PARTICLES
        if (ModConfig.hitParticles && !activeNovas.isEmpty()) {
            for (HitNova nova : activeNovas) {
                float progress = nova.getProgress(tickDelta);
                if (progress >= 1.0f) continue;

                float ease = 1.0f - (float) Math.pow(1.0f - progress, 3.2);
                int alpha = (int) (smoothStep(1.0f - progress) * 250);

                poseStack.pushPose();
                poseStack.translate(nova.x - cameraPos.x, nova.y - cameraPos.y, nova.z - cameraPos.z);

                drawBillboardOrb(poseStack, orbConsumer, camRot, 0, 0, 0, 0.65f * (1.0f - progress * 0.6f), 255, 255, 255, alpha);
                drawBillboardOrb(poseStack, orbConsumer, camRot, 0, 0, 0, 1.15f * ease, r1, g1, b1, (int) (alpha * 0.7f));

                for (int i = 0; i < 10; i++) {
                    double theta = i * (Math.PI * 2.0 / 10.0) + ease * 1.8;
                    double phi = Math.sin(i * 1.7) * 0.75;
                    float dist = ease * 1.45f;
                    float sx = (float) (Math.cos(theta) * Math.cos(phi)) * dist;
                    float sy = (float) Math.sin(phi) * dist;
                    float sz = (float) (Math.sin(theta) * Math.cos(phi)) * dist;
                    drawBillboardOrb(poseStack, orbConsumer, camRot, sx, sy, sz, 0.24f * (1.0f - progress * 0.5f), r2, g2, b2, alpha);
                }
                poseStack.popPose();
            }
        }

        // 5. KILL LIGHTNING VORTEX
        if (ModConfig.killEffect && !activeVortexes.isEmpty()) {
            for (KillVortex vortex : activeVortexes) {
                float progress = vortex.getProgress(tickDelta);
                if (progress >= 1.0f) continue;

                int alpha = (int) (smoothStep(1.0f - progress) * 245);
                poseStack.pushPose();
                poseStack.translate(vortex.x - cameraPos.x, vortex.y - cameraPos.y, vortex.z - cameraPos.z);

                int orbs = 60;
                for (int i = 0; i < orbs; i++) {
                    float t = i / (float) orbs;
                    float y = t * 18.0f * smoothStep(Math.min(1.0f, progress * 2.0f));
                    float rad = (0.2f + t * 1.8f) * (1.0f - progress * 0.35f);
                    double ang = t * 16.0 - time * 9.0;

                    float ox = (float) Math.cos(ang) * rad;
                    float oz = (float) Math.sin(ang) * rad;
                    int cr = (int) Mth.lerp(t, r1, r2);
                    int cg = (int) Mth.lerp(t, g1, g2);
                    int cb = (int) Mth.lerp(t, b1, b2);

                    drawBillboardOrb(poseStack, orbConsumer, camRot, ox, y, oz, 0.35f * (1.0f - t * 0.4f), cr, cg, cb, (int) (alpha * (1.0f - t * 0.5f)));
                    drawBillboardOrb(poseStack, orbConsumer, camRot, -ox, y, -oz, 0.28f * (1.0f - t * 0.4f), 255, 255, 255, (int) (alpha * 0.7f * (1.0f - t * 0.5f)));
                }
                poseStack.popPose();
            }
        }

        // 6. SPEED TRAIL LINE (Catmull-Rom Spline)
        if (ModConfig.trailLine && speedTrail.size() >= 4) {
            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            for (int i = 1; i < speedTrail.size() - 2; i++) {
                TrailPoint p0 = speedTrail.get(i - 1);
                TrailPoint p1 = speedTrail.get(i);
                TrailPoint p2 = speedTrail.get(i + 1);
                TrailPoint p3 = speedTrail.get(i + 2);

                float life = smoothStep(1.0f - ((p1.age + tickDelta) / (float) p1.maxAge));
                if (life <= 0.01f) continue;

                double distCamSq = (p1.x - cameraPos.x) * (p1.x - cameraPos.x)
                        + (p1.y - cameraPos.y) * (p1.y - cameraPos.y)
                        + (p1.z - cameraPos.z) * (p1.z - cameraPos.z);
                float camFade = (float) Math.clamp((distCamSq - 0.65) / 1.4, 0.0, 1.0);
                int alpha = (int) (life * 215 * camFade);
                if (alpha <= 2) continue;

                int subSteps = 5;
                for (int s = 0; s < subSteps; s++) {
                    float st = s / (float) subSteps;
                    float sx = (float) catmullRom(st, p0.x, p1.x, p2.x, p3.x);
                    float sy = (float) catmullRom(st, p0.y, p1.y, p2.y, p3.y);
                    float sz = (float) catmullRom(st, p0.z, p1.z, p2.z, p3.z);

                    int cr = (int) Mth.lerp(life, r2, r1);
                    int cg = (int) Mth.lerp(life, g2, g1);
                    int cb = (int) Mth.lerp(life, b2, b1);

                    drawBillboardOrb(poseStack, orbConsumer, camRot, sx, sy, sz, 0.17f * life, cr, cg, cb, alpha);
                }
            }
            poseStack.popPose();
        }

        bufferSource.endBatch(orbType);

        // =========================================================================================
        // ETAP 2: TEKSTURA PIERŚCIENIA HD (Skalowane Jump Circles, Custom Sky)
        // =========================================================================================
        RenderType ringType = GlowTextureManager.getRingRenderType();
        VertexConsumer ringConsumer = bufferSource.getBuffer(ringType);

        if (ModConfig.jumpCircles && !activeWaves.isEmpty()) {
            for (GroundWave wave : activeWaves) {
                float progress = wave.getProgress(tickDelta);
                if (progress >= 1.0f) continue;

                float ease = 1.0f - (float) Math.pow(1.0f - progress, 3.4);
                float radius = ease * wave.maxRadius;
                float fade = smoothStep(1.0f - progress);
                int alpha = (int) (fade * 245);

                poseStack.pushPose();
                poseStack.translate(wave.x - cameraPos.x, wave.y - cameraPos.y, wave.z - cameraPos.z);
                Matrix4f mat = poseStack.last().pose();

                drawTexturedGroundQuad(ringConsumer, mat, radius, r1, g1, b1, alpha);
                drawTexturedGroundQuad(ringConsumer, mat, radius * 0.96f, 255, 255, 255, (int) (alpha * 0.7f));

                if (wave.isLanding) {
                    drawTexturedGroundQuad(ringConsumer, mat, radius * 0.65f, r2, g2, b2, (int) (alpha * 0.85f));
                }
                poseStack.popPose();
            }
        }

        if (ModConfig.blackHoleSky) {
            poseStack.pushPose();
            poseStack.translate(0.0, 78.0, -35.0);
            poseStack.mulPose(Axis.XP.rotationDegrees(22.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0f));

            Matrix4f diskMat = poseStack.last().pose();
            drawTexturedGroundQuad(ringConsumer, diskMat, 22.0f, 255, 255, 255, 255);
            drawTexturedGroundQuad(ringConsumer, diskMat, 32.0f, r1, g1, b1, 240);
            drawTexturedGroundQuad(ringConsumer, diskMat, 46.0f, r2, g2, b2, 200);

            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(84.0f));
            Matrix4f lensMat = poseStack.last().pose();
            drawTexturedGroundQuad(ringConsumer, lensMat, 19.5f, 255, 255, 255, 245);
            drawTexturedGroundQuad(ringConsumer, lensMat, 26.5f, r1, g1, b1, 215);
            poseStack.popPose();

            poseStack.popPose();
        }

        bufferSource.endBatch(ringType);

        // =========================================================================================
        // ETAP 3: WAYPOINTS 3D BEAMS + HORYZONT ZDARZEŃ CZARNEJ DZIURY
        // =========================================================================================
        VertexConsumer solidConsumer = bufferSource.getBuffer(RenderTypes.lightning());

        if (ModConfig.showWaypoints && !WaypointManager.waypoints.isEmpty()) {
            for (WaypointManager.Waypoint wp : WaypointManager.waypoints) {
                if (!wp.enabled) continue;
                int wr = ElasticAnimation.getR(wp.color);
                int wg = ElasticAnimation.getG(wp.color);
                int wb = ElasticAnimation.getB(wp.color);

                poseStack.pushPose();
                poseStack.translate(wp.x - cameraPos.x, wp.y - cameraPos.y, wp.z - cameraPos.z);
                Matrix4f wpMat = poseStack.last().pose();

                drawVerticalBeam(solidConsumer, wpMat, 0.18f, 120.0f, wr, wg, wb, 165);
                drawVerticalBeam(solidConsumer, wpMat, 0.06f, 120.0f, 255, 255, 255, 230);
                poseStack.popPose();
            }
        }

        if (ModConfig.blackHoleSky) {
            poseStack.pushPose();
            poseStack.translate(0.0, 78.0, -35.0);
            poseStack.mulPose(Axis.XP.rotationDegrees(22.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0f));
            drawSolidSphere(solidConsumer, poseStack.last().pose(), 13.8f, 24, 24, 2, 2, 5, 255);
            poseStack.popPose();
        }

        bufferSource.endBatch(RenderTypes.lightning());
    }

    private static void renderVolumetricThickGhosts(
            PoseStack poseStack, VertexConsumer orbConsumer, Quaternionf camRot,
            float time, float orbitRadius, float headSize, float speed,
            int r1, int g1, int b1, int r2, int g2, int b2, int maxAlpha
    ) {
        int ghostCount = 3;
        int tailSteps = 44;

        for (int g = 0; g < ghostCount; g++) {
            float phase = (float) (g * Math.PI * 2.0 / ghostCount);

            for (int t = tailSteps; t >= 0; t--) {
                float lag = t * 0.014f;
                float ratio = 1.0f - (t / (float) tailSteps);
                float bodyCurve = (float) Math.pow(ratio, 0.85);
                double angle = (time - lag) * speed + phase;

                float dynamicR = orbitRadius + (float) Math.sin((time - lag) * 2.2f + g) * 0.14f;
                float ox = (float) Math.cos(angle) * dynamicR;
                float oz = (float) Math.sin(angle) * dynamicR;
                float oy = (float) Math.sin((time - lag) * speed * 1.25f + g * 2.1f) * 0.48f;

                int cr = (int) Mth.lerp(ratio, r2, r1);
                int cg = (int) Mth.lerp(ratio, g2, g1);
                int cb = (int) Mth.lerp(ratio, b2, b1);

                if (t == 0) {
                    drawBillboardOrb(poseStack, orbConsumer, camRot, ox, oy, oz, headSize * 1.65f, cr, cg, cb, (int) (maxAlpha * 0.65f));
                    drawBillboardOrb(poseStack, orbConsumer, camRot, ox, oy, oz, headSize * 1.15f, cr, cg, cb, maxAlpha);
                    drawBillboardOrb(poseStack, orbConsumer, camRot, ox, oy, oz, headSize * 0.72f, 255, 255, 255, 255);
                } else {
                    float mainSize = headSize * (0.20f + 0.85f * bodyCurve);
                    int mainAlpha = (int) (maxAlpha * 0.55f * bodyCurve);

                    drawBillboardOrb(poseStack, orbConsumer, camRot, ox, oy, oz, mainSize, cr, cg, cb, mainAlpha);
                    drawBillboardOrb(poseStack, orbConsumer, camRot, ox, oy, oz, mainSize * 0.45f, 255, 255, 255, (int) (mainAlpha * 0.85f));

                    double spiralAng = t * 0.45 - time * 10.0 + g;
                    float thicknessOffset = headSize * 0.28f * bodyCurve;
                    float sx = ox + (float) Math.cos(spiralAng) * thicknessOffset;
                    float sy = oy + (float) Math.sin(spiralAng) * thicknessOffset;
                    float sz = oz + (float) Math.sin(spiralAng * 0.7) * thicknessOffset;

                    drawBillboardOrb(poseStack, orbConsumer, camRot, sx, sy, sz, mainSize * 0.75f, cr, cg, cb, (int) (mainAlpha * 0.75f));
                }
            }
        }
    }

    private static void drawBillboardOrb(
            PoseStack poseStack, VertexConsumer consumer, Quaternionf camRot,
            float x, float y, float z, float halfSize, int r, int g, int b, int a
    ) {
        if (a <= 2 || halfSize <= 0.002f) return;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(camRot);
        Matrix4f mat = poseStack.last().pose();

        consumer.addVertex(mat, -halfSize, -halfSize, 0.0f).setColor(r, g, b, a).setUv(0.0f, 1.0f).setOverlay(0).setLight(15728880).setNormal(0, 1, 0);
        consumer.addVertex(mat, halfSize, -halfSize, 0.0f).setColor(r, g, b, a).setUv(1.0f, 1.0f).setOverlay(0).setLight(15728880).setNormal(0, 1, 0);
        consumer.addVertex(mat, halfSize, halfSize, 0.0f).setColor(r, g, b, a).setUv(1.0f, 0.0f).setOverlay(0).setLight(15728880).setNormal(0, 1, 0);
        consumer.addVertex(mat, -halfSize, halfSize, 0.0f).setColor(r, g, b, a).setUv(0.0f, 0.0f).setOverlay(0).setLight(15728880).setNormal(0, 1, 0);

        poseStack.popPose();
    }

    private static void drawTexturedGroundQuad(VertexConsumer consumer, Matrix4f mat, float radius, int r, int g, int b, int a) {
        if (a <= 2 || radius <= 0.01f) return;

        consumer.addVertex(mat, -radius, 0.0f, -radius).setColor(r, g, b, a).setUv(0.0f, 0.0f).setOverlay(0).setLight(15728880).setNormal(0, 1, 0);
        consumer.addVertex(mat, -radius, 0.0f, radius).setColor(r, g, b, a).setUv(0.0f, 1.0f).setOverlay(0).setLight(15728880).setNormal(0, 1, 0);
        consumer.addVertex(mat, radius, 0.0f, radius).setColor(r, g, b, a).setUv(1.0f, 1.0f).setOverlay(0).setLight(15728880).setNormal(0, 1, 0);
        consumer.addVertex(mat, radius, 0.0f, -radius).setColor(r, g, b, a).setUv(1.0f, 0.0f).setOverlay(0).setLight(15728880).setNormal(0, 1, 0);

        consumer.addVertex(mat, -radius, 0.0f, -radius).setColor(r, g, b, a).setUv(0.0f, 0.0f).setOverlay(0).setLight(15728880).setNormal(0, -1, 0);
        consumer.addVertex(mat, radius, 0.0f, -radius).setColor(r, g, b, a).setUv(1.0f, 0.0f).setOverlay(0).setLight(15728880).setNormal(0, -1, 0);
        consumer.addVertex(mat, radius, 0.0f, radius).setColor(r, g, b, a).setUv(1.0f, 1.0f).setOverlay(0).setLight(15728880).setNormal(0, -1, 0);
        consumer.addVertex(mat, -radius, 0.0f, radius).setColor(r, g, b, a).setUv(0.0f, 1.0f).setOverlay(0).setLight(15728880).setNormal(0, -1, 0);
    }

    private static void drawVerticalBeam(VertexConsumer consumer, Matrix4f mat, float halfW, float height, int r, int g, int b, int a) {
        consumer.addVertex(mat, -halfW, 0, 0).setColor(r, g, b, a);
        consumer.addVertex(mat, halfW, 0, 0).setColor(r, g, b, a);
        consumer.addVertex(mat, halfW, height, 0).setColor(r, g, b, 0);

        consumer.addVertex(mat, -halfW, 0, 0).setColor(r, g, b, a);
        consumer.addVertex(mat, halfW, height, 0).setColor(r, g, b, 0);
        consumer.addVertex(mat, -halfW, height, 0).setColor(r, g, b, 0);

        consumer.addVertex(mat, 0, 0, -halfW).setColor(r, g, b, a);
        consumer.addVertex(mat, 0, 0, halfW).setColor(r, g, b, a);
        consumer.addVertex(mat, 0, height, halfW).setColor(r, g, b, 0);

        consumer.addVertex(mat, 0, 0, -halfW).setColor(r, g, b, a);
        consumer.addVertex(mat, 0, height, halfW).setColor(r, g, b, 0);
        consumer.addVertex(mat, 0, height, -halfW).setColor(r, g, b, 0);
    }

    private static void drawSolidSphere(VertexConsumer consumer, Matrix4f matrix, float radius, int stacks, int slices, int r, int g, int b, int a) {
        for (int i = 0; i < stacks; i++) {
            double lat0 = Math.PI * (-0.5 + (double) i / stacks);
            float y0 = (float) Math.sin(lat0) * radius;
            float r0 = (float) Math.cos(lat0) * radius;

            double lat1 = Math.PI * (-0.5 + (double) (i + 1) / stacks);
            float y1 = (float) Math.sin(lat1) * radius;
            float r1 = (float) Math.cos(lat1) * radius;

            for (int j = 0; j < slices; j++) {
                double lng0 = 2 * Math.PI * (double) j / slices;
                double lng1 = 2 * Math.PI * (double) (j + 1) / slices;

                float x00 = (float) Math.cos(lng0) * r0, z00 = (float) Math.sin(lng0) * r0;
                float x01 = (float) Math.cos(lng1) * r0, z01 = (float) Math.sin(lng1) * r0;
                float x10 = (float) Math.cos(lng0) * r1, z10 = (float) Math.sin(lng0) * r1;
                float x11 = (float) Math.cos(lng1) * r1, z11 = (float) Math.sin(lng1) * r1;

                consumer.addVertex(matrix, x00, y0, z00).setColor(r, g, b, a);
                consumer.addVertex(matrix, x10, y1, z10).setColor(r, g, b, a);
                consumer.addVertex(matrix, x11, y1, z11).setColor(r, g, b, a);

                consumer.addVertex(matrix, x00, y0, z00).setColor(r, g, b, a);
                consumer.addVertex(matrix, x11, y1, z11).setColor(r, g, b, a);
                consumer.addVertex(matrix, x01, y0, z01).setColor(r, g, b, a);
            }
        }
    }
}