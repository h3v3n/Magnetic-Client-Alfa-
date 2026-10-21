package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MagneticMenuScreen extends Screen {

    private static final int PANEL_W = 580;
    private static final int PANEL_H = 345;
    private static final int CONTENT_DY = 54;
    private static final int COL_L = 20;
    private static final int COL_R = 298;
    private static final int COL_W = 262;
    private static final int ROW = 25;
    private static final int ROW_H = 22;

    private static final String[] TABS = {"VISUALS", "MODS & HUD", "VIEWMODEL", "WAYPOINTS", "THEMES"};
    private static final int TAB_VISUALS = 0;
    private static final int TAB_HUD = 1;
    private static final int TAB_VIEW = 2;
    private static final int TAB_WAYPOINTS = 3;
    private static final int TAB_THEMES = 4;

    private static int currentTab = TAB_VISUALS;

    private final Screen parent;
    private final List<Item> items = new ArrayList<>();
    private long openTime;
    private long itemsStart;
    private long savedFlashAt = 0L;
    private boolean hudEditor = false;
    private boolean crosshairPicker = false;
    private Slider dragging = null;

    public MagneticMenuScreen() {
        this(null);
    }

    public MagneticMenuScreen(Screen parent) {
        super(Component.literal("Magnetic Menu"));
        this.parent = parent;
    }

    public boolean isHudEditorMode() {
        return hudEditor;
    }

    @Override
    protected void init() {
        this.openTime = System.currentTimeMillis();
        rebuild();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        ModConfig.save();
        super.removed();
    }

    @Override
    public void onClose() {
        if (hudEditor) {
            hudEditor = false;
            ModConfig.save();
            rebuild();
            return;
        }
        MagneticUi.navigate(() -> Minecraft.getInstance().setScreen(parent));
    }

    // ---------------------------------------------------------------------
    // Geometria
    // ---------------------------------------------------------------------

    private int px() { return (this.width - PANEL_W) / 2; }
    private int py() { return (this.height - PANEL_H) / 2; }

    private static int row(int y0, int i) { return y0 + i * ROW; }

    // ---------------------------------------------------------------------
    // Budowanie zawartości
    // ---------------------------------------------------------------------

    private void add(Item it) {
        it.order = items.size();
        items.add(it);
    }

    private void addToggle(int x, int y, int w, String label, BooleanSupplier on, Runnable flip) {
        add(new Toggle(x, y, w, ROW_H, label, on, flip));
    }

    private void addToggle(int x, int y, int w, int h, String label, BooleanSupplier on, Runnable flip) {
        add(new Toggle(x, y, w, h, label, on, flip));
    }

    private void addSlider(int x, int y, int w, String label, float min, float max, Supplier<Float> get, Consumer<Float> set) {
        add(new Slider(x, y, w, ROW_H, label, min, max, get, set));
    }

    private void addRowAction(int x, int y, int w, Supplier<String> label, Supplier<String> extra, Runnable run) {
        add(new Action(x, y, w, ROW_H, label, extra, () -> false, run));
    }

    private void addAction(int x, int y, int w, int h, Supplier<String> label, Supplier<String> extra, BooleanSupplier active, Runnable run) {
        add(new Action(x, y, w, h, label, extra, active, run));
    }

    private void rebuild() {
        items.clear();
        dragging = null;
        itemsStart = System.currentTimeMillis();

        if (hudEditor) {
            buildHudEditor();
            return;
        }

        int px = px();
        int py = py();
        buildTabs(px, py);

        if (currentTab == TAB_THEMES) {
            buildThemes(px, py);
        } else if (crosshairPicker) {
            buildCrosshairPicker(px, py);
        } else {
            switch (currentTab) {
                case TAB_VISUALS -> buildVisuals(px, py);
                case TAB_HUD -> buildHud(px, py);
                case TAB_VIEW -> buildViewmodel(px, py);
                case TAB_WAYPOINTS -> buildWaypoints(px, py);
            }
        }
    }

    private void buildTabs(int px, int py) {
        int tabW = 88;
        int gap = 6;
        int y = py + 13;

        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            addAction(px + 14 + i * (tabW + gap), y, tabW, 24,
                    () -> TABS[idx], null,
                    () -> currentTab == idx && !crosshairPicker,
                    () -> {
                        currentTab = idx;
                        crosshairPicker = false;
                        rebuild();
                    });
        }

        addAction(px + PANEL_W - 92, y, 78, 24,
                () -> System.currentTimeMillis() - savedFlashAt < 1500L ? "✓ SAVED" : "SAVE CFG",
                null,
                () -> false,
                () -> {
                    ModConfig.save();
                    savedFlashAt = System.currentTimeMillis();
                });
    }

    private void buildVisuals(int px, int py) {
        int y0 = py + CONTENT_DY;
        int l = px + COL_L;
        int r = px + COL_R;

        addToggle(l, row(y0, 0), COL_W, "Jump Circles",
                () -> ModConfig.jumpCircles, () -> ModConfig.jumpCircles = !ModConfig.jumpCircles);
        addSlider(l, row(y0, 1), COL_W, "Circle Size", 0.60f, 4.50f,
                () -> ModConfig.jumpCircleSize, v -> ModConfig.jumpCircleSize = v);
        addToggle(l, row(y0, 2), COL_W, "Ghost Orbs 3D",
                () -> ModConfig.ghostOrbs, () -> ModConfig.ghostOrbs = !ModConfig.ghostOrbs);
        addRowAction(l, row(y0, 3), COL_W,
                () -> "Orbs Target",
                () -> ModConfig.GHOST_MODES[ModConfig.ghostOrbsMode],
                () -> ModConfig.ghostOrbsMode = (ModConfig.ghostOrbsMode + 1) % ModConfig.GHOST_MODES.length);
        addSlider(l, row(y0, 4), COL_W, "Orb Thickness", 0.60f, 2.20f,
                () -> ModConfig.ghostOrbThickness, v -> ModConfig.ghostOrbThickness = v);
        addToggle(l, row(y0, 5), COL_W, "Target ESP Ring",
                () -> ModConfig.targetEsp, () -> ModConfig.targetEsp = !ModConfig.targetEsp);
        addToggle(l, row(y0, 6), COL_W, "Hit Particles",
                () -> ModConfig.hitParticles, () -> ModConfig.hitParticles = !ModConfig.hitParticles);

        addToggle(r, row(y0, 0), COL_W, "Kill Lightning",
                () -> ModConfig.killEffect, () -> ModConfig.killEffect = !ModConfig.killEffect);
        addToggle(r, row(y0, 1), COL_W, "Speed Trail Line",
                () -> ModConfig.trailLine, () -> ModConfig.trailLine = !ModConfig.trailLine);
        addToggle(r, row(y0, 2), COL_W, "Player Orbit Aura",
                () -> ModConfig.blackHoleAura, () -> ModConfig.blackHoleAura = !ModConfig.blackHoleAura);
        addToggle(r, row(y0, 3), COL_W, "Blackhole 3D Sky",
                () -> ModConfig.blackHoleSky, () -> ModConfig.blackHoleSky = !ModConfig.blackHoleSky);
        addToggle(r, row(y0, 4), COL_W, "Always Night Mode",
                () -> ModConfig.alwaysNight, () -> ModConfig.alwaysNight = !ModConfig.alwaysNight);
        addToggle(r, row(y0, 5), COL_W, "Glow Boost",
                () -> ModConfig.glowBoost, () -> ModConfig.glowBoost = !ModConfig.glowBoost);
        addSlider(r, row(y0, 6), COL_W, "Glow Intensity", 0.0f, 2.0f,
                () -> ModConfig.glowIntensity, v -> ModConfig.glowIntensity = v);
        addRowAction(r, row(y0, 7), COL_W,
                () -> "Theme Palette",
                () -> ModConfig.THEME_NAMES[ModConfig.accentTheme] + "  ▼",
                () -> {
                    currentTab = TAB_THEMES;
                    crosshairPicker = false;
                    rebuild();
                });
        addRowAction(l, row(y0, 7), COL_W,
                () -> "✎ CREATE CUSTOM THEME",
                null,
                () -> MagneticUi.navigate(() -> Minecraft.getInstance().setScreen(new MagneticThemeCreatorScreen(this))));
    }

    private void buildHud(int px, int py) {
        int y0 = py + CONTENT_DY;
        int l = px + COL_L;
        int r = px + COL_R;

        addToggle(l, row(y0, 0), COL_W, "Zoom [Key: C]",
                () -> ModConfig.zoomEnabled, () -> ModConfig.zoomEnabled = !ModConfig.zoomEnabled);
        addSlider(l, row(y0, 1), COL_W, "Zoom Strength", 0.10f, 0.55f,
                () -> ModConfig.zoomFactor, v -> ModConfig.zoomFactor = v);
        addToggle(l, row(y0, 2), COL_W, "Custom Crosshair",
                () -> ModConfig.customCrosshair, () -> ModConfig.customCrosshair = !ModConfig.customCrosshair);
        addRowAction(l, row(y0, 3), COL_W,
                () -> "Select Crosshair",
                () -> ModConfig.CROSSHAIR_NAMES[ModConfig.crosshairStyle] + "  ▼",
                () -> {
                    crosshairPicker = true;
                    rebuild();
                });
        addSlider(l, row(y0, 4), COL_W, "Crosshair Size", 2.0f, 12.0f,
                () -> ModConfig.crosshairSize, v -> ModConfig.crosshairSize = v);
        addRowAction(l, row(y0, 5), COL_W,
                () -> "► EDIT HUD LAYOUT",
                () -> "DRAG",
                () -> {
                    hudEditor = true;
                    rebuild();
                });

        addToggle(r, row(y0, 0), COL_W, "FPS Counter",
                () -> ModConfig.showFps, () -> ModConfig.showFps = !ModConfig.showFps);
        addToggle(r, row(y0, 1), COL_W, "CPS Counter",
                () -> ModConfig.showCps, () -> ModConfig.showCps = !ModConfig.showCps);
        addToggle(r, row(y0, 2), COL_W, "Keystrokes",
                () -> ModConfig.showKeystrokes, () -> ModConfig.showKeystrokes = !ModConfig.showKeystrokes);
        addToggle(r, row(y0, 3), COL_W, "Coordinates",
                () -> ModConfig.showCoords, () -> ModConfig.showCoords = !ModConfig.showCoords);
        addToggle(r, row(y0, 4), COL_W, "Direction Compass",
                () -> ModConfig.showDirection, () -> ModConfig.showDirection = !ModConfig.showDirection);
        addToggle(r, row(y0, 5), COL_W, "Armor Status",
                () -> ModConfig.showArmorStatus, () -> ModConfig.showArmorStatus = !ModConfig.showArmorStatus);
        addToggle(r, row(y0, 6), COL_W, "Custom Main Menu",
                () -> ModConfig.customMainMenu, () -> ModConfig.customMainMenu = !ModConfig.customMainMenu);
    }

    private void buildViewmodel(int px, int py) {
        int y0 = py + CONTENT_DY;
        int lx = px + 18;
        int rx = px + 198;
        int w = 172;
        int step = 26;

        addToggle(lx, y0, w, "Right Hand ON",
                () -> ModConfig.customMainHand, () -> ModConfig.customMainHand = !ModConfig.customMainHand);
        addSlider(lx, y0 + step, w, "Right X", -1.5f, 1.5f,
                () -> ModConfig.mainHandX, v -> ModConfig.mainHandX = v);
        addSlider(lx, y0 + step * 2, w, "Right Y", -1.5f, 1.5f,
                () -> ModConfig.mainHandY, v -> ModConfig.mainHandY = v);
        addSlider(lx, y0 + step * 3, w, "Right Z", -1.5f, 1.5f,
                () -> ModConfig.mainHandZ, v -> ModConfig.mainHandZ = v);
        addSlider(lx, y0 + step * 4, w, "Right Scale", 0.2f, 2.0f,
                () -> ModConfig.mainHandScale, v -> ModConfig.mainHandScale = v);
        addSlider(lx, y0 + step * 5, w, "Right Rot Y", -180.0f, 180.0f,
                () -> ModConfig.mainHandRotY, v -> ModConfig.mainHandRotY = v);
        addRowAction(lx, y0 + step * 6, w, () -> "RESET RIGHT HAND", null, ModConfig::resetMainHand);

        addToggle(rx, y0, w, "Left Hand ON",
                () -> ModConfig.customOffHand, () -> ModConfig.customOffHand = !ModConfig.customOffHand);
        addSlider(rx, y0 + step, w, "Left X", -1.5f, 1.5f,
                () -> ModConfig.offHandX, v -> ModConfig.offHandX = v);
        addSlider(rx, y0 + step * 2, w, "Left Y", -1.5f, 1.5f,
                () -> ModConfig.offHandY, v -> ModConfig.offHandY = v);
        addSlider(rx, y0 + step * 3, w, "Left Z", -1.5f, 1.5f,
                () -> ModConfig.offHandZ, v -> ModConfig.offHandZ = v);
        addSlider(rx, y0 + step * 4, w, "Left Scale", 0.2f, 2.0f,
                () -> ModConfig.offHandScale, v -> ModConfig.offHandScale = v);
        addSlider(rx, y0 + step * 5, w, "Left Rot Y", -180.0f, 180.0f,
                () -> ModConfig.offHandRotY, v -> ModConfig.offHandRotY = v);
        addRowAction(rx, y0 + step * 6, w, () -> "RESET LEFT HAND", null, ModConfig::resetOffHand);
    }

    private void buildWaypoints(int px, int py) {
        int y0 = py + CONTENT_DY;
        int l = px + COL_L;
        int r = px + COL_R;

        addToggle(l, row(y0, 0), COL_W, "Show Waypoints",
                () -> ModConfig.showWaypoints, () -> ModConfig.showWaypoints = !ModConfig.showWaypoints);
        addRowAction(l, row(y0, 1), COL_W, () -> "+ ADD AT PLAYER  [B]", null, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                WaypointManager.addWaypointAtPlayer(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            }
            rebuild();
        });
        addRowAction(l, row(y0, 2), COL_W, () -> "CLEAR ALL WAYPOINTS", null, () -> {
            WaypointManager.clearAll();
            rebuild();
        });

        int shown = Math.min(8, WaypointManager.waypoints.size());
        for (int i = 0; i < shown; i++) {
            final int idx = i;
            final WaypointManager.Waypoint wp = WaypointManager.waypoints.get(i);
            int y = y0 + i * 28;
            String info = wp.name + "  (" + (int) wp.x + ", " + (int) wp.y + ", " + (int) wp.z + ")";

            addToggle(r, y, COL_W - 44, 24, info, () -> wp.enabled, () -> wp.enabled = !wp.enabled);
            addAction(r + COL_W - 40, y, 40, 24, () -> "DEL", null, () -> false, () -> {
                WaypointManager.removeWaypoint(idx);
                rebuild();
            });
        }
    }

    private void buildThemes(int px, int py) {
        for (int i = 0; i < ModConfig.THEME_NAMES.length; i++) {
            final int idx = i;
            int col = i % 3;
            int rw = i / 3;
            int x = px + 18 + col * (176 + 8);
            int y = py + 50 + rw * 29;
            add(new Swatch(x, y, 176, 25, idx, () -> {
                ModConfig.accentTheme = idx;
                ModConfig.save();
            }));
        }
    }

    private void buildCrosshairPicker(int px, int py) {
        for (int i = 0; i < ModConfig.CROSSHAIR_NAMES.length - 1; i++) {
            final int idx = i;
            int col = i % 3;
            int rw = i / 3;
            int x = px + 18 + col * (176 + 8);
            int y = py + 50 + rw * 44;
            add(new CrosshairCard(x, y, 176, 38, idx, () -> {
                ModConfig.crosshairStyle = idx;
                ModConfig.save();
                crosshairPicker = false;
                rebuild();
            }));
        }

        addAction(px + PANEL_W / 2 - 90, py + 300, 180, 26,
                () -> "◄ BACK TO MODS", null, () -> false, () -> {
                    crosshairPicker = false;
                    rebuild();
                });

        addAction(px + 18, py + 300, 172, 26,
                () -> "✎ DRAW CUSTOM", null, () -> false,
                () -> MagneticUi.navigate(() -> Minecraft.getInstance().setScreen(new MagneticPixelEditorScreen(this))));
    }

    private void buildHudEditor() {
        int cx = this.width / 2;
        int by = this.height - 42;
        int bw = 155;

        addAction(cx - bw - 6, by, bw, 24,
                () -> "◄ SAVE & BACK", null, () -> false, () -> {
                    ModConfig.save();
                    hudEditor = false;
                    rebuild();
                });
        addAction(cx + 6, by, bw, 24,
                () -> "RESET LAYOUT", null, () -> false, () -> {
                    ModConfig.resetHudPositions(this.width, this.height);
                    ModConfig.save();
                });
    }

    // ---------------------------------------------------------------------
    // Rysowanie
    // ---------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int primary = ElasticAnimation.getPrimaryColor(0);
        int secondary = ElasticAnimation.getSecondaryColor();
        float exit = MagneticUi.exitAlpha();

        if (hudEditor) {
            drawHudEditorOverlay(g, primary, secondary);
        } else {
            float open = MagneticUi.entrance(openTime, 300) * exit;
            g.fill(0, 0, this.width, this.height, ((int) (135 * open) << 24) | 0x04060B);
            MagneticUi.drawPopPanel(g, px(), py(), PANEL_W, PANEL_H, open, openTime);
        }

        for (Item it : items) {
            renderItem(g, it, mouseX, mouseY, exit, primary, secondary);
        }

        if (!hudEditor && currentTab == TAB_VIEW && !crosshairPicker) {
            renderPreview(g, px() + 382, py() + CONTENT_DY, 180, 180, primary, secondary);
        }
    }

    private void renderItem(GuiGraphics g, Item it, double mx, double my, float exit, int primary, int secondary) {
        float appear = MagneticUi.entrance(itemsStart + 40L + it.order * 14L, 260) * exit;
        if (appear <= 0.01f) return;

        int oy = Math.round((1.0f - appear) * 8.0f);
        boolean hovered = it.contains(mx, my) && appear > 0.9f;
        float s = MagneticUi.pressScale(it.x, it.y, it.w, it.h) * (hovered ? 1.015f : 1.0f);
        int[] r = MagneticUi.scaled(it.x, it.y + oy, it.w, it.h, s);

        it.draw(g, this.font, r[0], r[1], r[2], r[3], hovered, appear, primary, secondary);
    }

    private void drawHudEditorOverlay(GuiGraphics g, int primary, int secondary) {
        g.fill(0, 0, this.width, this.height, 0x5505070C);

        int gridCol = ElasticAnimation.withAlpha(primary, 18);
        for (int gx = 0; gx < this.width; gx += 24) g.fill(gx, 0, gx + 1, this.height, gridCol);
        for (int gy = 0; gy < this.height; gy += 24) g.fill(0, gy, this.width, gy + 1, gridCol);

        g.fill(this.width / 2, 0, this.width / 2 + 1, this.height, ElasticAnimation.withAlpha(secondary, 65));
        g.fill(0, this.height / 2, this.width, this.height / 2 + 1, ElasticAnimation.withAlpha(secondary, 65));

        int bannerW = 340;
        SmoothUiRenderer.drawSmoothCard(g, (this.width - bannerW) / 2, this.height - 68, bannerW, 20, 6, 0xDD080C14, primary, 0);
        String info = "DRAG WIDGETS TO REPOSITION • CENTER SNAP ACTIVE";
        g.drawString(this.font, info, (this.width - this.font.width(info)) / 2, this.height - 62, 0xFFFFFFFF, true);
    }

    private void renderPreview(GuiGraphics g, int x, int y, int w, int h, int primary, int secondary) {
        SmoothUiRenderer.drawSmoothCard(g, x, y, w, h, 12, 0xE6060911,
                ElasticAnimation.withAlpha(primary, 140), ElasticAnimation.withAlpha(secondary, 45));

        String title = "LIVE HAND PREVIEW";
        g.drawString(this.font, title, x + (w - this.font.width(title)) / 2, y + 7, primary, true);

        int centerX = x + w / 2;
        int horizonY = y + h / 2 + 8;
        g.fill(x + 8, horizonY, x + w - 8, horizonY + 1, 0x22FFFFFF);
        g.fill(centerX, y + 22, centerX + 1, y + h - 8, 0x22FFFFFF);

        Minecraft mc = Minecraft.getInstance();
        ItemStack mainItem = (mc.player != null && !mc.player.getMainHandItem().isEmpty())
                ? mc.player.getMainHandItem() : new ItemStack(Items.DIAMOND_SWORD);
        ItemStack offItem = (mc.player != null && !mc.player.getOffhandItem().isEmpty())
                ? mc.player.getOffhandItem() : new ItemStack(Items.SHIELD);

        float lScale = ModConfig.customOffHand ? ModConfig.offHandScale : 1.0f;
        float lDepth = ModConfig.customOffHand ? Math.clamp(1.0f + ModConfig.offHandZ * 0.35f, 0.45f, 1.65f) : 1.0f;
        float lTotal = Math.clamp(lScale * lDepth, 0.35f, 1.85f);
        int lx = centerX - 42 + (ModConfig.customOffHand ? Math.round(ModConfig.offHandX * 26.0f) : 0);
        int ly = horizonY + 18 - (ModConfig.customOffHand ? Math.round(ModConfig.offHandY * 26.0f) : 0);
        lx = Math.clamp(lx, x + 20, x + w - 20);
        ly = Math.clamp(ly, y + 30, y + h - 22);
        drawSimulatedHand(g, lx, ly, lTotal, ModConfig.customOffHand ? ModConfig.offHandRotY : 0f, offItem, secondary, "L");

        float rScale = ModConfig.customMainHand ? ModConfig.mainHandScale : 1.0f;
        float rDepth = ModConfig.customMainHand ? Math.clamp(1.0f + ModConfig.mainHandZ * 0.35f, 0.45f, 1.65f) : 1.0f;
        float rTotal = Math.clamp(rScale * rDepth, 0.35f, 1.85f);
        int rx = centerX + 42 + (ModConfig.customMainHand ? Math.round(ModConfig.mainHandX * 26.0f) : 0);
        int ry = horizonY + 18 - (ModConfig.customMainHand ? Math.round(ModConfig.mainHandY * 26.0f) : 0);
        rx = Math.clamp(rx, x + 20, x + w - 20);
        ry = Math.clamp(ry, y + 30, y + h - 22);
        drawSimulatedHand(g, rx, ry, rTotal, ModConfig.customMainHand ? ModConfig.mainHandRotY : 0f, mainItem, primary, "R");
    }

    private void drawSimulatedHand(GuiGraphics g, int cx, int cy, float scale, float rotY, ItemStack item, int color, String tag) {
        int armW = Math.max(10, Math.round(20 * scale));
        int armH = Math.max(12, Math.round(26 * scale));
        int rotShift = Math.round((rotY / 180.0f) * 8.0f);

        SmoothUiRenderer.drawSmoothCard(g, cx - armW / 2 + rotShift, cy - armH / 2 + 4, armW, armH, 6,
                ElasticAnimation.withAlpha(0x141C2B, 230), color, 0);
        g.renderItem(item, cx - 8 + rotShift / 2, cy - 10);

        String info = tag + ":" + String.format("%.1fx", scale);
        g.drawString(this.font, info, cx - this.font.width(info) / 2, cy + armH / 2 + 5, 0xFFCCCCCC, true);
    }

    // ---------------------------------------------------------------------
    // Myszka
    // ---------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || MagneticUi.isExiting()) {
            return super.mouseClicked(event, doubleClick);
        }

        Item hit = null;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).contains(event.x(), event.y())) {
                hit = items.get(i);
                break;
            }
        }
        if (hit == null) {
            return super.mouseClicked(event, doubleClick);
        }

        MagneticUi.press(hit.x, hit.y, hit.w, hit.h);
        if (hit instanceof Slider s) {
            dragging = s;
        }
        hit.onClick(event.x());
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging != null && event.button() == 0) {
            dragging.onDrag(event.x());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return super.mouseReleased(event);
    }

    // ---------------------------------------------------------------------
    // Pomocnicze rysowanie
    // ---------------------------------------------------------------------

    /** Mnoży alfę koloru przez a (0..1). */
    private static int af(int c, float a) {
        int alpha = Math.round(((c >>> 24) & 0xFF) * Math.clamp(a, 0.0f, 1.0f));
        return (alpha << 24) | (c & 0x00FFFFFF);
    }

    /** Wspólne tło karty dla wszystkich elementów menu. */
    private static void card(GuiGraphics g, int x, int y, int w, int h, boolean hovered, boolean active, float a, int primary) {
        int bg = af(hovered ? 0xEE171F30 : 0xDD0E131F, a);
        int border = active
                ? af(ElasticAnimation.withAlpha(primary, 200), a)
                : af(hovered ? 0x9964748B : 0x442A3446, a);
        int glow = active ? af(ElasticAnimation.withAlpha(primary, 38), a) : 0;
        SmoothUiRenderer.drawSmoothCard(g, x, y, w, h, 8, bg, border, glow);
    }

    // ---------------------------------------------------------------------
    // Elementy menu
    // ---------------------------------------------------------------------

    private abstract static class Item {
        final int x, y, w, h;
        int order;

        Item(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean contains(double mx, double my) {
            return MagneticUi.isInside(mx, my, x, y, w, h);
        }

        abstract void onClick(double mx);

        void onDrag(double mx) {}

        abstract void draw(GuiGraphics g, Font f, int dx, int dy, int dw, int dh,
                           boolean hovered, float a, int primary, int secondary);
    }

    /** Przełącznik z animowanym pillem. */
    private static final class Toggle extends Item {
        private final String label;
        private final BooleanSupplier on;
        private final Runnable flip;
        private float knob;

        Toggle(int x, int y, int w, int h, String label, BooleanSupplier on, Runnable flip) {
            super(x, y, w, h);
            this.label = label;
            this.on = on;
            this.flip = flip;
            this.knob = on.getAsBoolean() ? 1.0f : 0.0f;
        }

        @Override
        void onClick(double mx) {
            flip.run();
        }

        @Override
        void draw(GuiGraphics g, Font f, int dx, int dy, int dw, int dh,
                  boolean hovered, float a, int primary, int secondary) {
            boolean active = on.getAsBoolean();
            knob = ElasticAnimation.lerp(knob, active ? 1.0f : 0.0f, 0.24f);

            card(g, dx, dy, dw, dh, hovered, active, a, primary);
            g.drawString(f, label, dx + 8, dy + (dh - 8) / 2, af(active ? 0xFFFFFFFF : 0xFF94A3B8, a), true);

            int pillW = 24;
            int pillH = 11;
            int pillX = dx + dw - pillW - 8;
            int pillY = dy + (dh - pillH) / 2;
            int track = active ? ElasticAnimation.withAlpha(primary, 140) : 0xFF1E293B;
            SmoothUiRenderer.drawSmoothRoundedRect(g, pillX, pillY, pillW, pillH, 5, af(track, a));

            int kx = pillX + 1 + Math.round(knob * (pillW - 11));
            SmoothUiRenderer.drawSmoothCircle(g, kx, pillY + 1, 9, af(active ? 0xFFFFFFFF : 0xFF64748B, a));
        }
    }

    /** Suwak. Ustawia wartość przy kliknięciu i przy przeciąganiu. */
    private static final class Slider extends Item {
        private final String label;
        private final float min, max;
        private final Supplier<Float> get;
        private final Consumer<Float> set;

        Slider(int x, int y, int w, int h, String label, float min, float max, Supplier<Float> get, Consumer<Float> set) {
            super(x, y, w, h);
            this.label = label;
            this.min = min;
            this.max = max;
            this.get = get;
            this.set = set;
        }

        @Override
        void onClick(double mx) {
            setFrom(mx);
        }

        @Override
        void onDrag(double mx) {
            setFrom(mx);
        }

        private void setFrom(double mx) {
            float pct = Math.clamp((float) ((mx - (x + 8)) / (w - 16)), 0.0f, 1.0f);
            float v = min + pct * (max - min);
            v = Math.round(v * 100.0f) / 100.0f;
            set.accept(v);
        }

        @Override
        void draw(GuiGraphics g, Font f, int dx, int dy, int dw, int dh,
                  boolean hovered, float a, int primary, int secondary) {
            card(g, dx, dy, dw, dh, hovered, hovered, a, primary);

            float val = get.get();
            float pct = Math.clamp((val - min) / (max - min), 0.0f, 1.0f);

            g.drawString(f, label, dx + 7, dy + 3, af(0xFFE2E8F0, a), true);
            String vs = String.format("%.2f", val);
            g.drawString(f, vs, dx + dw - f.width(vs) - 7, dy + 3, af(secondary, a), true);

            int trackX = dx + 7;
            int trackW = dw - 14;
            int fillW = Math.max(4, Math.round(trackW * pct));
            SmoothUiRenderer.drawSmoothRoundedRect(g, trackX, dy + dh - 6, trackW, 4, 2, af(0xFF1A2234, a));
            SmoothUiRenderer.drawSmoothRoundedRect(g, trackX, dy + dh - 6, fillW, 4, 2, af(primary, a));

            int knobX = trackX + fillW - 4;
            SmoothUiRenderer.drawSmoothCircle(g, knobX, dy + dh - 8, 8, af(0xFFFFFFFF, a));
            SmoothUiRenderer.drawSmoothCircle(g, knobX + 2, dy + dh - 6, 4, af(secondary, a));
        }
    }

    /** Przycisk akcji. Z extra: etykieta po lewej, dodatkowy tekst po prawej. Bez extra: wyśrodkowany. */
    private static final class Action extends Item {
        private final Supplier<String> label;
        private final Supplier<String> extra;
        private final BooleanSupplier active;
        private final Runnable run;

        Action(int x, int y, int w, int h, Supplier<String> label, Supplier<String> extra, BooleanSupplier active, Runnable run) {
            super(x, y, w, h);
            this.label = label;
            this.extra = extra;
            this.active = active;
            this.run = run;
        }

        @Override
        void onClick(double mx) {
            run.run();
        }

        @Override
        void draw(GuiGraphics g, Font f, int dx, int dy, int dw, int dh,
                  boolean hovered, float a, int primary, int secondary) {
            boolean act = active.getAsBoolean();
            boolean lit = hovered || act;
            card(g, dx, dy, dw, dh, hovered, act, a, primary);

            String text = label.get();
            int textCol = af(lit ? 0xFFFFFFFF : 0xFFE2E8F0, a);
            int ty = dy + (dh - 8) / 2;

            if (extra == null) {
                g.drawString(f, text, dx + (dw - f.width(text)) / 2, ty, textCol, true);
            } else {
                g.drawString(f, text, dx + 8, ty, textCol, true);
                String ex = extra.get();
                g.drawString(f, ex, dx + dw - f.width(ex) - 8, ty, af(primary, a), true);
            }
        }
    }

    /** Kafelek motywu: dwie kropki z kolorami motywu i nazwa. */
    private static final class Swatch extends Item {
        private final int themeIdx;
        private final Runnable onPick;

        Swatch(int x, int y, int w, int h, int themeIdx, Runnable onPick) {
            super(x, y, w, h);
            this.themeIdx = themeIdx;
            this.onPick = onPick;
        }

        @Override
        void onClick(double mx) {
            onPick.run();
        }

        @Override
        void draw(GuiGraphics g, Font f, int dx, int dy, int dw, int dh,
                  boolean hovered, float a, int primary, int secondary) {
            boolean act = ModConfig.accentTheme == themeIdx;
            card(g, dx, dy, dw, dh, hovered, act, a, primary);

            int c1 = ElasticAnimation.getThemePreviewColor1(themeIdx);
            int c2 = ElasticAnimation.getThemePreviewColor2(themeIdx);
            int dotY = dy + (dh - 10) / 2;
            SmoothUiRenderer.drawSmoothCircle(g, dx + 7, dotY, 10, af(c1, a));
            SmoothUiRenderer.drawSmoothCircle(g, dx + 19, dotY, 10, af(c2, a));

            int ty = dy + (dh - 8) / 2;
            g.drawString(f, ModConfig.THEME_NAMES[themeIdx], dx + 33, ty, af(act ? 0xFFFFFFFF : 0xFFE2E8F0, a), true);
            if (act) {
                g.drawString(f, "✓", dx + dw - 12, ty, af(primary, a), true);
            }
        }
    }

    /** Kafelek celownika z podglądem. */
    private static final class CrosshairCard extends Item {
        private final int styleIdx;
        private final Runnable onPick;

        CrosshairCard(int x, int y, int w, int h, int styleIdx, Runnable onPick) {
            super(x, y, w, h);
            this.styleIdx = styleIdx;
            this.onPick = onPick;
        }

        @Override
        void onClick(double mx) {
            onPick.run();
        }

        @Override
        void draw(GuiGraphics g, Font f, int dx, int dy, int dw, int dh,
                  boolean hovered, float a, int primary, int secondary) {
            boolean act = ModConfig.crosshairStyle == styleIdx;
            card(g, dx, dy, dw, dh, hovered, act, a, primary);

            int box = dh - 8;
            SmoothUiRenderer.drawSmoothCard(g, dx + 6, dy + 4, box, box, 6, af(0xFF060911, a), af(0x44FFFFFF, a), 0);
            MagneticHudRenderer.drawCrosshairByStyle(g, styleIdx, dx + 6 + box / 2, dy + 4 + box / 2, 5, false, primary, secondary);

            int ty = dy + (dh - 8) / 2;
            g.drawString(f, ModConfig.CROSSHAIR_NAMES[styleIdx], dx + box + 14, ty,
                    af(act ? 0xFFFFFFFF : 0xFFE2E8F0, a), true);
        }
    }
}