package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public class MagneticMultiplayerScreen extends MagneticListScreen {

    private ServerList servers;

    public MagneticMultiplayerScreen(Screen parent) {
        super(parent, "Multiplayer");
    }

    @Override
    protected String headerTitle() { return "MULTIPLAYER"; }

    @Override
    protected String headerSub() { return "SERVERS: " + entryCount(); }

    @Override
    protected void onInit() {
        this.servers = new ServerList(Minecraft.getInstance());
        this.servers.load();
    }

    @Override
    protected int entryCount() {
        return servers == null ? 0 : servers.size();
    }

    @Override
    protected String entryName(int i) { return servers.get(i).name; }

    @Override
    protected String entryHint(int i) { return servers.get(i).ip; }

    @Override
    protected String emptyLine1() { return "NO SERVERS SAVED"; }

    @Override
    protected String emptyLine2() { return "Use EDIT LIST below to add one"; }

    @Override
    protected String playLabel() { return "CONNECT"; }

    @Override
    protected String extraLabel() { return "EDIT LIST"; }

    @Override
    protected void onPlay(int index) {
        Minecraft mc = Minecraft.getInstance();
        ServerData data = servers.get(index);
        MagneticUi.navigate(() -> ConnectScreen.startConnecting(
                this, mc, ServerAddress.parseString(data.ip), data, false, null));
    }

    @Override
    protected void onExtra() {
        Minecraft mc = Minecraft.getInstance();
        MagneticUi.navigate(() -> mc.setScreen(new JoinMultiplayerScreen(this)));
    }
}