package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MagneticSingleplayerScreen extends MagneticListScreen {

    private final List<String> worlds = new ArrayList<>();

    public MagneticSingleplayerScreen(Screen parent) {
        super(parent, "Singleplayer");
    }

    @Override
    protected String headerTitle() { return "SINGLEPLAYER"; }

    @Override
    protected String headerSub() { return "SAVED WORLDS: " + worlds.size(); }

    @Override
    protected void onInit() {
        worlds.clear();
        File saves = new File(Minecraft.getInstance().gameDirectory, "saves");
        File[] dirs = saves.listFiles(f -> f.isDirectory() && new File(f, "level.dat").exists());
        if (dirs == null) return;

        // Najnowsze na górze
        Arrays.sort(dirs, Comparator.comparingLong(File::lastModified).reversed());
        for (File d : dirs) {
            worlds.add(d.getName());
        }
    }

    @Override
    protected int entryCount() { return worlds.size(); }

    @Override
    protected String entryName(int i) { return worlds.get(i); }

    @Override
    protected String entryHint(int i) { return ""; }

    @Override
    protected String emptyLine1() { return "NO SAVED WORLDS FOUND"; }

    @Override
    protected String emptyLine2() { return "Create a new world below"; }

    @Override
    protected String playLabel() { return "PLAY"; }

    @Override
    protected String extraLabel() { return "CREATE NEW"; }

    @Override
    protected void onPlay(int index) {
        Minecraft mc = Minecraft.getInstance();
        MagneticUi.navigate(() -> mc.setScreen(new SelectWorldScreen(this)));
    }
    @Override
    protected void onExtra() {
        Minecraft mc = Minecraft.getInstance();
        MagneticUi.navigate(() -> CreateWorldScreen.openFresh(mc, () -> mc.setScreen(this)));
    }
}