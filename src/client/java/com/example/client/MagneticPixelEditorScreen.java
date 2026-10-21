package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class MagneticPixelEditorScreen extends Screen {

    private static final int CELL = 14;
    private static final int GRID = CustomCrosshair.SIZE * CELL;
    private static final int SLIDER_W = 200;

    private static final int[] PRESETS = {
            0xFFFFFFFF, 0xFF000000, 0xFFFF1744, 0xFFFF9100, 0xFFFFD600, 0xFF00E676, 0xFF00E5FF, 0xFF2979FF,
            0xFFD500F9, 0xFFFF4081, 0xFF8D6E63, 0xFF94A3B8, 0xFF1DE9B6, 0xFFFFAB40, 0xFF7C4DFF, 0x00000000
    };
    private static final String[] CHANNELS = {"RED", "GREEN", "BLUE", "ALPHA"};

    private final Screen parent;
    private final int[] work = new int[CustomCrosshair.SIZE * CustomCrosshair.SIZE];
    private int color = 0xFFFFFFFF;
    private int paintMode = 0; // 0 nic, 1 maluj, 2 gumka
    private int dragChan = -1;
    private long openTime;

    public MagneticPixelEditorScreen(Screen parent) {
        super(Component.literal("Pixel Crosshair Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.openTime = System.currentTimeMillis();
        System.arraycopy(CustomCrosshair.pixels, 0, work, 0, work.length);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        back();
    }

    // ---------------------------------------------------------------------
    // Geometria
    // ---------------------------------------------------------------------

    private int gx() { return this.width / 2 - 250; }
    private int gy() { return this.height / 2 - GRID / 2; }
    private int rx() { return gx() + 240; }
    private int previewX() { return rx() + 196; }
    private int buttonsY() { return gy() + 56 + 4 * 26; }

    private int cellAt(double mx, double my) {
        int cx = (int) Math.floor((mx - gx()) / CELL);
        int cy = (int) Math.floor((my - gy()) / CELL);
        if (cx < 0 || cy < 0 || cx >= CustomCrosshair.SIZE || cy >= CustomCrosshair.SIZE) return -1;
        return cy * CustomCrosshair.SIZE + cx;
    }

    // ---------------------------------------------------------------------
    // Kolor
    // ---------------------------------------------------------------------

    private int channel(int i) {
        return switch (i) {
            case 0 -> (color >> 16) & 0xFF;
            case 1 -> (color >> 8) & 0xFF;
            case 2 -> color & 0xFF;
            default -> (color >>> 24) & 0xFF;
        };
    }

    private void setChannel(int i, int v) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        switch (i) {
            case 0 -> r = v;
            case 1 -> g = v;
            case 2 -> b = v;
            default -> a = v;
        }
        color = (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void applyPaint(int cell) {
        work[cell] = (paintMode == 2 || (color >>> 24) == 0) ? 0 : color;
    }

    // ---------------------------------------------------------------------
    // Rysowanie
    // ---------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        float fade = MagneticUi.entrance(openTime, 450) * MagneticUi.exitAlpha();
        int primary = ElasticAnimation.getPrimaryColor(0);
        int gx = gx();
        int gy = gy();
        int rx = rx();

        MagneticUi.drawBackground(g, this.width, this.height, mx, my, fade);
        MagneticUi.drawHeader(g, this.font, "PIXEL CROSSHAIR",
                "LMB PAINT  •  RMB ERASE  •  DRAG TO DRAW", this.width / 2, 14, fade);

        // Siatka
        g.fill(gx - 4, gy - 4, gx + GRID + 4, gy + GRID + 4, ElasticAnimation.withAlpha(primary, (int) (150 * fade)));
        g.fill(gx - 2, gy - 2, gx + GRID + 2, gy + GRID + 2, 0xFF05070C);

        for (int y = 0; y < CustomCrosshair.SIZE; y++) {
            for (int x = 0; x < CustomCrosshair.SIZE; x++) {
                int c = work[y * CustomCrosshair.SIZE + x];
                int col = ((c >>> 24) == 0) ? (((x + y) & 1) == 0 ? 0xFF1A2030 : 0xFF121826) : c;
                int x0 = gx + x * CELL;
                int y0 = gy + y * CELL;
                g.fill(x0, y0, x0 + CELL - 1, y0 + CELL - 1, col);
            }
        }

        int hc = cellAt(mx, my);
        if (hc >= 0) {
            int hx = gx + (hc % CustomCrosshair.SIZE) * CELL;
            int hy = gy + (hc / CustomCrosshair.SIZE) * CELL;
            int o = 0xCCFFFFFF;
            g.fill(hx, hy, hx + CELL - 1, hy + 1, o);
            g.fill(hx, hy + CELL - 2, hx + CELL - 1, hy + CELL - 1, o);
            g.fill(hx, hy, hx + 1, hy + CELL - 1, o);
            g.fill(hx + CELL - 2, hy, hx + CELL - 1, hy + CELL - 1, o);
        }

        // Paleta
        for (int i = 0; i < PRESETS.length; i++) {
            int sx = rx + (i % 8) * 24;
            int sy = gy + (i / 8) * 24;
            boolean hov = MagneticUi.isInside(mx, my, sx, sy, 20, 20);
            MagneticEditorUi.swatch(g, sx, sy, 20, PRESETS[i], PRESETS[i] == color || hov, primary);
        }

        // Podgląd
        int pxX = previewX();
        SmoothUiRenderer.drawSmoothCard(g, pxX, gy, 84, 84, 10, 0xE6060911,
                ElasticAnimation.withAlpha(primary, 140), 0);
        CustomCrosshair.drawPixels(g, work, pxX + 42, gy + 42, CustomCrosshair.scale());

        g.fill(pxX, gy + 96, pxX + 84, gy + 114, color);
        g.fill(pxX - 1, gy + 95, pxX + 85, gy + 96, 0xFF2A3446);
        g.fill(pxX - 1, gy + 114, pxX + 85, gy + 115, 0xFF2A3446);

        // Suwaki RGBA
        for (int i = 0; i < CHANNELS.length; i++) {
            int sy = gy + 56 + i * 26;
            boolean hov = MagneticUi.isInside(mx, my, rx, sy, SLIDER_W, 22);
            MagneticEditorUi.slider(g, this.font, rx, sy, SLIDER_W, 22,
                    CHANNELS[i], channel(i), 255, primary, hov);
        }

        // Przyciski
        int by = buttonsY();
        MagneticUi.drawButton(g, this.font, rx, by, 64, 24, "CLEAR",
                MagneticUi.isInside(mx, my, rx, by, 64, 24), false, fade);
        MagneticUi.drawButton(g, this.font, rx + 68, by, 64, 24, "SAVE",
                MagneticUi.isInside(mx, my, rx + 68, by, 64, 24), false, fade);
        MagneticUi.drawButton(g, this.font, rx + 136, by, 64, 24, "BACK",
                MagneticUi.isInside(mx, my, rx + 136, by, 64, 24), false, fade);

        String hex = String.format("COLOR  #%08X", color);
        g.drawString(this.font, hex, rx, by + 34,
                ElasticAnimation.withAlpha(0xFFCBD5E1, (int) (255 * fade)), true);
    }

    // ---------------------------------------------------------------------
    // Myszka
    // ---------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (MagneticUi.isExiting()) return true;
        int button = event.button();
        if (button != 0 && button != 1) return super.mouseClicked(event, doubleClick);

        double mx = event.x();
        double my = event.y();

        int cell = cellAt(mx, my);
        if (cell >= 0) {
            paintMode = (button == 0) ? 1 : 2;
            applyPaint(cell);
            return true;
        }

        if (button != 0) return super.mouseClicked(event, doubleClick);

        int rx = rx();
        int gy = gy();

        for (int i = 0; i < PRESETS.length; i++) {
            int sx = rx + (i % 8) * 24;
            int sy = gy + (i / 8) * 24;
            if (MagneticUi.isInside(mx, my, sx, sy, 20, 20)) {
                color = PRESETS[i];
                return true;
            }
        }

        for (int i = 0; i < CHANNELS.length; i++) {
            int sy = gy + 56 + i * 26;
            if (MagneticUi.isInside(mx, my, rx, sy, SLIDER_W, 22)) {
                dragChan = i;
                setChannel(i, MagneticEditorUi.valueFromMouse(mx, rx, SLIDER_W, 255));
                return true;
            }
        }

        int by = buttonsY();
        if (MagneticUi.isInside(mx, my, rx, by, 64, 24)) {
            MagneticUi.press(rx, by, 64, 24);
            java.util.Arrays.fill(work, 0);
            return true;
        }
        if (MagneticUi.isInside(mx, my, rx + 68, by, 64, 24)) {
            MagneticUi.press(rx + 68, by, 64, 24);
            save();
            return true;
        }
        if (MagneticUi.isInside(mx, my, rx + 136, by, 64, 24)) {
            MagneticUi.press(rx + 136, by, 64, 24);
            back();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragChan >= 0) {
            setChannel(dragChan, MagneticEditorUi.valueFromMouse(event.x(), rx(), SLIDER_W, 255));
            return true;
        }
        if (paintMode > 0) {
            int cell = cellAt(event.x(), event.y());
            if (cell >= 0) applyPaint(cell);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        paintMode = 0;
        dragChan = -1;
        return super.mouseReleased(event);
    }

    // ---------------------------------------------------------------------
    // Zapis i powrót
    // ---------------------------------------------------------------------

    private void save() {
        System.arraycopy(work, 0, CustomCrosshair.pixels, 0, work.length);
        ModConfig.crosshairStyle = ModConfig.CROSSHAIR_NAMES.length - 1; // Custom Pixel Art
        ModConfig.save();
        back();
    }

    private void back() {
        MagneticUi.navigate(() -> Minecraft.getInstance().setScreen(parent));
    }
}