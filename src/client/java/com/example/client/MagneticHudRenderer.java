package com.example.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class MagneticHudRenderer {

    private static final float[] keyPressAnim = new float[6];
    private static final float[] keyPulseAnim = new float[6];
    private static final boolean[] wasKeyDown = new boolean[6];

    private static int draggingId = -1;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;
    private static boolean snappedVert = false;
    private static boolean snappedHoriz = false;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public static void render(GuiGraphics graphics, Minecraft client) {
        if (client.options.hideGui || client.level == null || client.player == null) return;

        Font font = client.font;
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        initDefaultPositions(screenW, screenH);

        boolean isEditMode = (client.screen instanceof MagneticMenuScreen menu && menu.isHudEditorMode())
                || (client.screen instanceof ChatScreen);
        handleHudDragging(client, screenW, screenH, isEditMode);

        int primary = ElasticAnimation.getPrimaryColor(0);
        int secondary = ElasticAnimation.getSecondaryColor();

        if (isEditMode && draggingId != -1) {
            if (snappedVert) {
                graphics.fill(screenW / 2, 0, screenW / 2 + 1, screenH, 0xFF00F5D4);
            }
            if (snappedHoriz) {
                graphics.fill(0, screenH / 2, screenW, screenH / 2 + 1, 0xFF00F5D4);
            }
        }

        int mx = (int) (client.mouseHandler.xpos() * screenW / Math.max(1, client.getWindow().getScreenWidth()));
        int my = (int) (client.mouseHandler.ypos() * screenH / Math.max(1, client.getWindow().getScreenHeight()));

        // 1. Custom crosshair
        if (ModConfig.customCrosshair && client.screen == null) {
            drawCrosshairByStyle(graphics, ModConfig.crosshairStyle, screenW / 2, screenH / 2,
                    Math.round(ModConfig.crosshairSize), client.options.keyAttack.isDown(), primary, secondary);
        }

        // 2. Waypointy na HUD
        if (ModConfig.showWaypoints && !WaypointManager.waypoints.isEmpty()) {
            renderWaypointsOnHud(graphics, font, client, screenW, screenH);
        }

        // 3. FPS (ID = 0)
        if (ModConfig.showFps) {
            int w = 86, h = 20;
            renderCyberBox(graphics, ModConfig.fpsX, ModConfig.fpsY, w, h, primary);
            graphics.drawString(font, "FPS", ModConfig.fpsX + 8, ModConfig.fpsY + 6, primary, true);
            graphics.drawString(font, String.valueOf(client.getFps()), ModConfig.fpsX + 36, ModConfig.fpsY + 6, 0xFFFFFFFF, true);
            if (isEditMode) drawHudEditorWidgetBox(graphics, font, "FPS", ModConfig.fpsX, ModConfig.fpsY, w, h, draggingId == 0, isInside(mx, my, ModConfig.fpsX, ModConfig.fpsY, w, h), primary, secondary);
        }

        // 4. CPS (ID = 1)
        if (ModConfig.showCps) {
            int w = 96, h = 20;
            renderCyberBox(graphics, ModConfig.cpsX, ModConfig.cpsY, w, h, secondary);
            graphics.drawString(font, "CPS", ModConfig.cpsX + 8, ModConfig.cpsY + 6, secondary, true);
            String cpsVal = MagneticClientClient.getLeftCps() + " : " + MagneticClientClient.getRightCps();
            graphics.drawString(font, cpsVal, ModConfig.cpsX + 36, ModConfig.cpsY + 6, 0xFFFFFFFF, true);
            if (isEditMode) drawHudEditorWidgetBox(graphics, font, "CPS", ModConfig.cpsX, ModConfig.cpsY, w, h, draggingId == 1, isInside(mx, my, ModConfig.cpsX, ModConfig.cpsY, w, h), primary, secondary);
        }

        // 5. Keystrokes (ID = 2)
        if (ModConfig.showKeystrokes) {
            int w = 64, h = 64;
            int kx = ModConfig.keysX;
            int ky = ModConfig.keysY;

            renderAnimatedCyberKey(graphics, font, 0, "W", kx + 22, ky, 20, 20, client.options.keyUp.isDown());
            renderAnimatedCyberKey(graphics, font, 1, "A", kx, ky + 22, 20, 20, client.options.keyLeft.isDown());
            renderAnimatedCyberKey(graphics, font, 2, "S", kx + 22, ky + 22, 20, 20, client.options.keyDown.isDown());
            renderAnimatedCyberKey(graphics, font, 3, "D", kx + 44, ky + 22, 20, 20, client.options.keyRight.isDown());

            renderAnimatedCyberKey(graphics, font, 4, "LMB", kx, ky + 44, 31, 18, client.options.keyAttack.isDown());
            renderAnimatedCyberKey(graphics, font, 5, "RMB", kx + 33, ky + 44, 31, 18, client.options.keyUse.isDown());
            if (isEditMode) drawHudEditorWidgetBox(graphics, font, "KEYSTROKES", kx, ky, w, h, draggingId == 2, isInside(mx, my, kx, ky, w, h), primary, secondary);
        }

        // 6. Współrzędne (ID = 3)
        if (ModConfig.showCoords) {
            renderCoordinatesHud(graphics, font, client.player, primary, secondary, isEditMode, mx, my);
        }

        // 7. Kompas (ID = 4)
        if (ModConfig.showDirection) {
            renderDirectionCompass(graphics, font, client.player, primary, secondary, isEditMode, mx, my);
        }

        // 8. Pancerz (ID = 5)
        if (ModConfig.showArmorStatus) {
            renderArmorStatusHud(graphics, font, client.player, primary, secondary, isEditMode, mx, my);
        }
    }

    public static void drawCrosshairByStyle(GuiGraphics g, int style, int cx, int cy, int size, boolean attacking, int primary, int secondary) {

        if (style == ModConfig.CROSSHAIR_NAMES.length - 1) {
            CustomCrosshair.draw(g, cx, cy);
            return;
        }
        int expand = attacking ? 3 : 0;
        int col = attacking ? secondary : primary;
        int s = size + expand;
        float t = (System.currentTimeMillis() % 100000L) / 1000.0f;

        switch (style) {
            case 0 -> {
                int d = s * 2;
                SmoothUiRenderer.drawSmoothCard(g, cx - d / 2, cy - d / 2, d, d, d / 2, 0x00000000, col, ElasticAnimation.withAlpha(col, 90));
                SmoothUiRenderer.drawSmoothCircle(g, cx - 2, cy - 2, 4, 0xFFFFFFFF);
            }
            case 1 -> {
                int gap = 3 + expand;
                g.fill(cx - gap - size, cy - 1, cx - gap, cy + 1, col);
                g.fill(cx + gap, cy - 1, cx + gap + size, cy + 1, col);
                g.fill(cx - 1, cy - gap - size, cx + 1, cy - gap, col);
                g.fill(cx - 1, cy + gap, cx + 1, cy + gap + size, col);
                SmoothUiRenderer.drawSmoothCircle(g, cx - 1, cy - 1, 2, 0xFFFFFFFF);
            }
            case 2 -> {
                g.fill(cx - s, cy - 4, cx - s + 3, cy - 3, col);
                g.fill(cx - s, cy + 3, cx - s + 3, cy + 4, col);
                g.fill(cx - s, cy - 4, cx - s + 1, cy + 4, col);

                g.fill(cx + s - 3, cy - 4, cx + s, cy - 3, col);
                g.fill(cx + s - 3, cy + 3, cx + s, cy + 4, col);
                g.fill(cx + s - 1, cy - 4, cx + s, cy + 4, col);
                SmoothUiRenderer.drawSmoothCircle(g, cx - 2, cy - 2, 4, secondary);
            }
            case 3 -> {
                SmoothUiRenderer.drawSmoothShadow(g, cx - s, cy - s, s * 2, s * 2, s, ElasticAnimation.withAlpha(col, 150));
                SmoothUiRenderer.drawSmoothCircle(g, cx - 3, cy - 3, 6, col);
                SmoothUiRenderer.drawSmoothCircle(g, cx - 1, cy - 1, 2, 0xFFFFFFFF);
            }
            case 4 -> {
                SmoothUiRenderer.drawSmoothCircle(g, cx - 2, cy - s - 2, 4, col);
                SmoothUiRenderer.drawSmoothCircle(g, cx - 2, cy + s - 2, 4, col);
                SmoothUiRenderer.drawSmoothCircle(g, cx - s - 2, cy - 2, 4, col);
                SmoothUiRenderer.drawSmoothCircle(g, cx + s - 2, cy - 2, 4, col);
                SmoothUiRenderer.drawSmoothCircle(g, cx - 1, cy - 1, 2, 0xFFFFFFFF);
            }
            case 5 -> {
                g.fill(cx - 1, cy - s - 2, cx + 1, cy - 3, col);
                for (int i = 3; i <= s + 1; i++) {
                    g.fill(cx - i, cy + i - 1, cx - i + 2, cy + i + 1, col);
                    g.fill(cx + i - 2, cy + i - 1, cx + i, cy + i + 1, col);
                }
                SmoothUiRenderer.drawSmoothCircle(g, cx - 2, cy - 2, 4, secondary);
            }
            case 6 -> {
                int d = s * 2 + 2;
                SmoothUiRenderer.drawSmoothCard(g, cx - d / 2, cy - d / 2, d, d, d / 2, 0x00000000, ElasticAnimation.withAlpha(col, 170), 0);
                g.fill(cx - s - 4, cy, cx - 2, cy + 1, 0xCCFFFFFF);
                g.fill(cx + 3, cy, cx + s + 5, cy + 1, 0xCCFFFFFF);
                g.fill(cx, cy - s - 4, cx + 1, cy - 2, 0xCCFFFFFF);
                g.fill(cx, cy + 3, cx + 1, cy + s + 5, 0xCCFFFFFF);
                SmoothUiRenderer.drawSmoothCircle(g, cx - 1, cy - 1, 3, secondary);
            }
            case 7 -> {
                for (int i = 2; i <= s + 1; i++) {
                    g.fill(cx - i, cy - i, cx - i + 2, cy - i + 2, col);
                    g.fill(cx + i - 1, cy - i, cx + i + 1, cy - i + 2, col);
                    g.fill(cx - i, cy + i - 1, cx - i + 2, cy + i + 1, col);
                    g.fill(cx + i - 1, cy + i - 1, cx + i + 1, cy + i + 1, col);
                }
            }
            case 8 -> {
                int d = s * 2 + 2;
                SmoothUiRenderer.drawSmoothCard(g, cx - d / 2, cy - d / 2, d, d, 4, 0x00000000, col, ElasticAnimation.withAlpha(secondary, 75));
                SmoothUiRenderer.drawSmoothCircle(g, cx - 2, cy - 2, 4, 0xFFFFFFFF);
            }
            case 10 -> { // Chevron Arrow: dwie diagonalne linie zbiegające się w środku
                int gap = 3 + expand;
                for (int k = 0; k < s; k++) {
                    int d = gap + k;
                    g.fill(cx - d - 1, cy - d - 1, cx - d + 1, cy - d + 1, col);
                    g.fill(cx + d - 1, cy - d - 1, cx + d + 1, cy - d + 1, col);
                }
                SmoothUiRenderer.drawSmoothCircle(g, cx - 1, cy - 1, 2, 0xFFFFFFFF);
            }
            case 11 -> { // Orbit Spin: cztery kropki krążące wokół środka
                int r = s + 3;
                for (int i = 0; i < 4; i++) {
                    double ang = t * 3.0 + i * Math.PI / 2.0;
                    int ox = cx + (int) Math.round(Math.cos(ang) * r);
                    int oy = cy + (int) Math.round(Math.sin(ang) * r);
                    SmoothUiRenderer.drawSmoothCircle(g, ox - 2, oy - 2, 4, (i % 2 == 0) ? col : secondary);
                }
                SmoothUiRenderer.drawSmoothCircle(g, cx - 1, cy - 1, 2, 0xFFFFFFFF);
            }
            case 12 -> { // Spark Burst: osiem promieni, które pulsują długością
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4.0 + t * 0.6;
                    double pulse = 0.5 + 0.5 * Math.sin(t * 6.0 + i);
                    int len = Math.round(s * (0.8f + 0.9f * (float) pulse));
                    int rayCol = (i % 2 == 0) ? col : secondary;
                    for (int r = 4; r <= 4 + len; r += 2) {
                        int dx = cx + (int) Math.round(Math.cos(ang) * r);
                        int dy = cy + (int) Math.round(Math.sin(ang) * r);
                        g.fill(dx - 1, dy - 1, dx + 1, dy + 1, rayCol);
                    }
                }
            }
            case 13 -> { // Glow Dot Cross: krzyż z poświatą w środku
                SmoothUiRenderer.drawSmoothShadow(g, cx - 7, cy - 7, 14, 14, 7, ElasticAnimation.withAlpha(col, 120));
                int gap = 3 + expand;
                g.fill(cx - gap - s, cy - 1, cx - gap, cy + 1, col);
                g.fill(cx + gap, cy - 1, cx + gap + s, cy + 1, col);
                g.fill(cx - 1, cy - gap - s, cx + 1, cy - gap, col);
                g.fill(cx - 1, cy + gap, cx + 1, cy + gap + s, col);
                SmoothUiRenderer.drawSmoothCircle(g, cx - 2, cy - 2, 4, 0xFFFFFFFF);
            }
            case 14 -> { // Ring Lock: osiem kropek w okręgu, powoli obracających się
                int r = s + 2;
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4.0 + t * 0.4;
                    int ox = cx + (int) Math.round(Math.cos(ang) * r);
                    int oy = cy + (int) Math.round(Math.sin(ang) * r);
                    SmoothUiRenderer.drawSmoothCircle(g, ox - 1, oy - 1, 3, (i % 2 == 0) ? col : 0xAAFFFFFF);
                }
                SmoothUiRenderer.drawSmoothCircle(g, cx - 1, cy - 1, 2, col);
            }
            default -> { // 9: Dual Pulse Ring
                int d1 = s * 2 + 4;
                int d2 = Math.max(6, s);
                SmoothUiRenderer.drawSmoothCard(g, cx - d1 / 2, cy - d1 / 2, d1, d1, d1 / 2, 0x00000000, col, 0);
                SmoothUiRenderer.drawSmoothCard(g, cx - d2 / 2, cy - d2 / 2, d2, d2, d2 / 2, 0x00000000, secondary, 0);
                SmoothUiRenderer.drawSmoothCircle(g, cx - 1, cy - 1, 2, 0xFFFFFFFF);
            }
        }
    }

    private static void drawHudEditorWidgetBox(GuiGraphics g, Font font, String name, int x, int y, int w, int h, boolean dragging, boolean hovered, int primary, int secondary) {
        int borderCol = dragging ? 0xFF00F5D4 : (hovered ? 0xFFFFFFFF : ElasticAnimation.withAlpha(primary, 170));
        int bgTint = dragging ? ElasticAnimation.withAlpha(primary, 55) : (hovered ? 0x33FFFFFF : 0x00000000);

        SmoothUiRenderer.drawSmoothRoundedRect(g, x - 2, y - 2, w + 4, h + 4, 9, borderCol);
        SmoothUiRenderer.drawSmoothRoundedRect(g, x - 1, y - 1, w + 2, h + 2, 8, 0xFF060911);
        if (bgTint != 0) {
            SmoothUiRenderer.drawSmoothRoundedRect(g, x, y, w, h, 7, bgTint);
        }

        if (dragging || hovered) {
            String badge = name + " [" + x + ", " + y + "]";
            int bw = font.width(badge) + 8;
            int bx = x + (w - bw) / 2;
            int by = (y > 16) ? (y - 14) : (y + h + 4);
            SmoothUiRenderer.drawSmoothRoundedRect(g, bx, by, bw, 12, 4, 0xEE070A12);
            g.drawString(font, badge, bx + 4, by + 2, dragging ? 0xFF00F5D4 : 0xFFFFFFFF, true);
        }
    }

    private static void handleHudDragging(Minecraft client, int screenW, int screenH, boolean canDrag) {
        if (!canDrag) {
            draggingId = -1;
            snappedVert = false;
            snappedHoriz = false;
            return;
        }

        long window = client.getWindow().handle();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        int mx = (int) (client.mouseHandler.xpos() * screenW / Math.max(1, client.getWindow().getScreenWidth()));
        int my = (int) (client.mouseHandler.ypos() * screenH / Math.max(1, client.getWindow().getScreenHeight()));

        if (client.screen instanceof MagneticMenuScreen menu && menu.isHudEditorMode() && my > screenH - 72) {
            if (!leftDown) draggingId = -1;
            return;
        }

        if (!leftDown) {
            if (draggingId != -1) {
                ModConfig.save();
            }
            draggingId = -1;
            snappedVert = false;
            snappedHoriz = false;
            return;
        }

        if (draggingId == -1) {
            if (ModConfig.showFps && isInside(mx, my, ModConfig.fpsX, ModConfig.fpsY, 86, 20)) {
                draggingId = 0; dragOffsetX = mx - ModConfig.fpsX; dragOffsetY = my - ModConfig.fpsY;
            } else if (ModConfig.showCps && isInside(mx, my, ModConfig.cpsX, ModConfig.cpsY, 96, 20)) {
                draggingId = 1; dragOffsetX = mx - ModConfig.cpsX; dragOffsetY = my - ModConfig.cpsY;
            } else if (ModConfig.showKeystrokes && isInside(mx, my, ModConfig.keysX, ModConfig.keysY, 64, 64)) {
                draggingId = 2; dragOffsetX = mx - ModConfig.keysX; dragOffsetY = my - ModConfig.keysY;
            } else if (ModConfig.showCoords && isInside(mx, my, ModConfig.coordsX, ModConfig.coordsY, 128, 34)) {
                draggingId = 3; dragOffsetX = mx - ModConfig.coordsX; dragOffsetY = my - ModConfig.coordsY;
            } else if (ModConfig.showDirection && isInside(mx, my, ModConfig.dirX, ModConfig.dirY, 170, 34)) {
                draggingId = 4; dragOffsetX = mx - ModConfig.dirX; dragOffsetY = my - ModConfig.dirY;
            } else if (ModConfig.showArmorStatus && isInside(mx, my, ModConfig.armorX, ModConfig.armorY, 64, 82)) {
                draggingId = 5; dragOffsetX = mx - ModConfig.armorX; dragOffsetY = my - ModConfig.armorY;
            }
        } else {
            int w = switch (draggingId) {
                case 0 -> 86; case 1 -> 96; case 2 -> 64; case 3 -> 128; case 4 -> 170; default -> 64;
            };
            int h = switch (draggingId) {
                case 0, 1 -> 20; case 2 -> 64; case 3, 4 -> 34; default -> 82;
            };

            int nx = Math.clamp(mx - dragOffsetX, 4, screenW - w - 4);
            int ny = Math.clamp(my - dragOffsetY, 4, screenH - h - 4);

            snappedVert = false;
            snappedHoriz = false;
            int centerTargetX = (screenW - w) / 2;
            int centerTargetY = (screenH - h) / 2;

            if (Math.abs(nx - centerTargetX) <= 6) {
                nx = centerTargetX;
                snappedVert = true;
            } else if (Math.abs(nx - 12) <= 5) {
                nx = 12;
            } else if (Math.abs(nx - (screenW - w - 12)) <= 5) {
                nx = screenW - w - 12;
            }

            if (Math.abs(ny - centerTargetY) <= 6) {
                ny = centerTargetY;
                snappedHoriz = true;
            }

            switch (draggingId) {
                case 0 -> { ModConfig.fpsX = nx; ModConfig.fpsY = ny; }
                case 1 -> { ModConfig.cpsX = nx; ModConfig.cpsY = ny; }
                case 2 -> { ModConfig.keysX = nx; ModConfig.keysY = ny; }
                case 3 -> { ModConfig.coordsX = nx; ModConfig.coordsY = ny; }
                case 4 -> { ModConfig.dirX = nx; ModConfig.dirY = ny; }
                case 5 -> { ModConfig.armorX = nx; ModConfig.armorY = ny; }
            }
        }
    }

    private static void renderWaypointsOnHud(GuiGraphics g, Font font, Minecraft client, int screenW, int screenH) {
        Camera cam = client.gameRenderer.getMainCamera();
        Vec3 camPos = cam.position();
        float camYaw = Mth.wrapDegrees(cam.yRot());

        for (WaypointManager.Waypoint wp : WaypointManager.waypoints) {
            if (!wp.enabled) continue;
            double dx = wp.x - camPos.x;
            double dy = (wp.y + 1.8) - camPos.y;
            double dz = wp.z - camPos.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
            float diffYaw = Mth.wrapDegrees((float) targetYaw - camYaw);

            if (Math.abs(diffYaw) <= 55.0f) {
                int sx = (screenW / 2) + (int) ((diffYaw / 55.0f) * (screenW * 0.42f));
                double horizDist = Math.sqrt(dx * dx + dz * dz);
                double targetPitch = -Math.toDegrees(Math.atan2(dy, horizDist));
                float diffPitch = (float) targetPitch - cam.xRot();
                int sy = (screenH / 2) + (int) ((diffPitch / 40.0f) * (screenH * 0.38f));
                sy = Math.clamp(sy, 28, screenH - 42);

                String label = wp.name + " [" + (int) dist + "m]";
                int bw = font.width(label) + 12;
                int bh = 16;
                SmoothUiRenderer.drawSmoothCard(g, sx - bw / 2, sy - bh / 2, bw, bh, 6, 0xDD080C15, wp.color, ElasticAnimation.withAlpha(wp.color, 70));
                g.drawString(font, label, sx - font.width(label) / 2, sy - 4, 0xFFFFFFFF, true);
            }
        }
    }

    public static void initDefaultPositions(int screenW, int screenH) {
        if (ModConfig.dirX == -1) ModConfig.dirX = (screenW - 170) / 2;
        if (ModConfig.armorX == -1) ModConfig.armorX = (screenW / 2) + 98;
        if (ModConfig.armorY == -1) ModConfig.armorY = screenH - 86;
    }

    private static boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static void renderDirectionCompass(GuiGraphics graphics, Font font, Player player, int primary, int secondary, boolean isEditMode, int mx, int my) {
        int compW = 170;
        int compH = 20;
        int compX = ModConfig.dirX;
        int compY = ModConfig.dirY;

        renderCyberBox(graphics, compX, compY, compW, compH, primary);

        float yaw = Mth.wrapDegrees(player.getYRot());
        if (yaw < 0) yaw += 360.0f;

        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        for (int i = 0; i < 8; i++) {
            float dirAngle = i * 45.0f;
            float diff = Mth.wrapDegrees(dirAngle - yaw);
            if (Math.abs(diff) <= 65.0f) {
                int dx = compX + (compW / 2) + (int) (diff * 1.15f);
                String label = dirs[i];
                boolean isCardinal = label.length() == 1;
                int col = isCardinal ? primary : 0xFFAAAAAA;
                graphics.drawString(font, label, dx - font.width(label) / 2, compY + 6, col, true);
            }
        }

        int centerX = compX + compW / 2;
        String degText = (int) yaw + "°";
        int degW = font.width(degText) + 10;
        renderCyberBox(graphics, centerX - degW / 2, compY + 21, degW, 13, secondary);
        graphics.drawString(font, degText, centerX - font.width(degText) / 2, compY + 23, 0xFFFFFFFF, true);

        if (isEditMode) drawHudEditorWidgetBox(graphics, font, "COMPASS", compX, compY, compW, 34, draggingId == 4, isInside(mx, my, compX, compY, compW, 34), primary, secondary);
    }

    private static void renderCoordinatesHud(GuiGraphics graphics, Font font, Player player, int primary, int secondary, boolean isEditMode, int mx, int my) {
        int w = 128;
        int h = 34;
        int x = ModConfig.coordsX;
        int y = ModConfig.coordsY;

        renderCyberBox(graphics, x, y, w, h, primary);

        int px = Mth.floor(player.getX());
        int py = Mth.floor(player.getY());
        int pz = Mth.floor(player.getZ());

        graphics.drawString(font, "XYZ", x + 7, y + 6, primary, true);
        graphics.drawString(font, px + ", " + py + ", " + pz, x + 30, y + 6, 0xFFFFFFFF, true);

        int nx = px / 8;
        int nz = pz / 8;
        graphics.drawString(font, "NETHER", x + 7, y + 19, secondary, true);
        graphics.drawString(font, nx + ", " + nz, x + 48, y + 19, 0xFFCCCCCC, true);

        if (isEditMode) drawHudEditorWidgetBox(graphics, font, "COORDS", x, y, w, h, draggingId == 3, isInside(mx, my, x, y, w, h), primary, secondary);
    }

    private static void renderArmorStatusHud(GuiGraphics graphics, Font font, Player player, int primary, int secondary, boolean isEditMode, int mx, int my) {
        int startX = ModConfig.armorX;
        int startY = ModConfig.armorY;

        boolean hasAnyArmor = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) {
                hasAnyArmor = true;
                break;
            }
        }
        if (!hasAnyArmor && !isEditMode) return;

        renderCyberBox(graphics, startX, startY, 64, 82, primary);

        if (!hasAnyArmor && isEditMode) {
            graphics.drawString(font, "ARMOR", startX + 14, startY + 36, primary, true);
        }

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = player.getItemBySlot(ARMOR_SLOTS[i]);
            int rowY = startY + 4 + i * 19;

            if (!stack.isEmpty()) {
                graphics.renderItem(stack, startX + 4, rowY);

                if (stack.isDamageableItem()) {
                    int maxDmg = stack.getMaxDamage();
                    int curDmg = maxDmg - stack.getDamageValue();
                    float ratio = Math.clamp((float) curDmg / maxDmg, 0.0f, 1.0f);
                    int pct = (int) (ratio * 100);

                    int barColor = ratio > 0.5f ? primary : (ratio > 0.25f ? 0xFFFFD600 : 0xFFFF1744);
                    graphics.drawString(font, pct + "%", startX + 24, rowY + 3, 0xFFFFFFFF, true);

                    SmoothUiRenderer.drawSmoothRoundedRect(graphics, startX + 24, rowY + 13, 34, 3, 1, 0xFF1A202C);
                    SmoothUiRenderer.drawSmoothRoundedRect(graphics, startX + 24, rowY + 13, Math.max(2, (int) (34 * ratio)), 3, 1, barColor);
                } else {
                    graphics.drawString(font, "MAX", startX + 24, rowY + 5, primary, true);
                }
            }
        }

        if (isEditMode) drawHudEditorWidgetBox(graphics, font, "ARMOR", startX, startY, 64, 82, draggingId == 5, isInside(mx, my, startX, startY, 64, 82), primary, secondary);
    }

    private static void renderAnimatedCyberKey(GuiGraphics graphics, Font font, int id, String text, int x, int y, int w, int h, boolean down) {
        if (down && !wasKeyDown[id]) {
            keyPulseAnim[id] = 1.0f;
        }
        wasKeyDown[id] = down;

        keyPressAnim[id] = ElasticAnimation.lerp(keyPressAnim[id], down ? 1.0f : 0.0f, 0.28f);
        keyPulseAnim[id] = ElasticAnimation.lerp(keyPulseAnim[id], 0.0f, 0.12f);

        float press = keyPressAnim[id];
        float pulse = keyPulseAnim[id];

        int primary = ElasticAnimation.getPrimaryColor(id * 0.3f);

        if (pulse > 0.02f) {
            int expand = (int) ((1.0f - pulse) * 5.0f);
            SmoothUiRenderer.drawSmoothShadow(graphics, x - expand, y - expand, w + expand * 2, h + expand * 2, 7, ElasticAnimation.withAlpha(primary, (int) (pulse * 165.0f)));
        }

        int drawY = y + Math.round(press * 1.5f);

        int pr = ElasticAnimation.getR(primary), pg = ElasticAnimation.getG(primary), pb = ElasticAnimation.getB(primary);
        int r = (int) (10 + press * (pr - 10));
        int g = (int) (14 + press * (pg - 14));
        int b = (int) (22 + press * (pb - 22));
        int bg = (0xE0 << 24) | (r << 16) | (g << 8) | b;

        int border = press > 0.5f ? 0xFFFFFFFF : ElasticAnimation.withAlpha(primary, 180);
        SmoothUiRenderer.drawSmoothCard(graphics, x, drawY, w, h, 6, bg, border, 0);

        int textColor = press > 0.55f ? 0xFF05070A : 0xFFFFFFFF;
        int tx = x + (w - font.width(text)) / 2;
        int ty = drawY + (h - 8) / 2;
        graphics.drawString(font, text, tx, ty, textColor, press <= 0.55f);
    }

    public static void renderCyberBox(GuiGraphics graphics, int x, int y, int w, int h, int accentColor) {
        SmoothUiRenderer.drawSmoothCard(graphics, x, y, w, h, 7,
                0xD9090D16, ElasticAnimation.withAlpha(accentColor, 175), ElasticAnimation.withAlpha(accentColor, 55));
    }
}