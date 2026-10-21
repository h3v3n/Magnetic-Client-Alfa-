package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class MagneticTitleScreen extends Screen {

    private static final String[] LABELS = {"SINGLEPLAYER", "MULTIPLAYER", "SETTINGS", "OPTIONS", "QUIT GAME"};
    private static final int BTN_W = 230;
    private static final int BTN_H = 30;
    private static final int BTN_GAP = 10;

    private long openTime;

    public MagneticTitleScreen() {
        super(Component.literal("Magnetic Main Menu"));
    }

    @Override
    protected void init() {
        this.openTime = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int btnX() {
        return this.width / 2 - BTN_W / 2;
    }

    private int btnY(int i) {
        return this.height / 2 - 10 + i * (BTN_H + BTN_GAP);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float ex = MagneticUi.exitAlpha();
        float fade = MagneticUi.entrance(openTime, 600) * ex;
        int secondary = ElasticAnimation.getSecondaryColor();

        MagneticUi.drawBackground(g, this.width, this.height, mouseX, mouseY, fade);
        MagneticUi.drawHeader(g, this.font, "MAGNETIC",
                "THEME: " + ModConfig.THEME_NAMES[ModConfig.accentTheme],
                this.width / 2, this.height / 2 - 80, fade);

        int x = btnX();
        for (int i = 0; i < LABELS.length; i++) {
            // Każdy przycisk wjeżdża z opóźnieniem
            float bt = MagneticUi.entrance(openTime + 120L + i * 70L, 420);
            int bx = x + Math.round((1.0f - bt) * 40.0f);
            int y = btnY(i);
            boolean hovered = MagneticUi.isInside(mouseX, mouseY, x, y, BTN_W, BTN_H);
            MagneticUi.drawButton(g, this.font, bx, y, BTN_W, BTN_H, LABELS[i], hovered, false, bt * ex);
        }

        String footer = "MAGNETIC CLIENT  •  MINECRAFT 1.21.11";
        g.drawString(this.font, footer, this.width / 2 - this.font.width(footer) / 2, this.height - 14,
                ElasticAnimation.withAlpha(secondary, (int) (200 * fade)), true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (MagneticUi.isExiting()) return true;
        if (event.button() == 0) {
            int x = btnX();
            for (int i = 0; i < LABELS.length; i++) {
                int y = btnY(i);
                if (MagneticUi.isInside(event.x(), event.y(), x, y, BTN_W, BTN_H)) {
                    MagneticUi.press(x, y, BTN_W, BTN_H);
                    onButton(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void onButton(int index) {
        Minecraft mc = Minecraft.getInstance();
        switch (index) {
            case 0 -> MagneticUi.navigate(() -> mc.setScreen(new MagneticSingleplayerScreen(this)));
            case 1 -> MagneticUi.navigate(() -> mc.setScreen(new MagneticMultiplayerScreen(this)));
            case 2 -> MagneticUi.navigate(() -> mc.setScreen(new MagneticMenuScreen(this)));
            case 3 -> MagneticUi.navigate(() -> mc.setScreen(new OptionsScreen(this, mc.options)));
            case 4 -> mc.stop();
        }
    }
}