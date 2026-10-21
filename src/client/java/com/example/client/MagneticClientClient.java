package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MagneticClientClient implements ClientModInitializer {

	private boolean wasOnGround = true;
	private double lastFallVelocity = 0.0;
	private static LivingEntity lastTarget = null;
	private static int targetTicks = 0;
	private static KeyMapping menuKey;
	private static KeyMapping zoomKey;
	private static KeyMapping waypointKey;

	private static final List<Long> leftClicks = new ArrayList<>();
	private static final List<Long> rightClicks = new ArrayList<>();
	private boolean lastLeftDown = false;
	private boolean lastRightDown = false;

	public static int getLeftCps() { return leftClicks.size(); }
	public static int getRightCps() { return rightClicks.size(); }
	public static LivingEntity getActiveTarget() { return targetTicks > 0 ? lastTarget : null; }

	@Override
	public void onInitializeClient() {
		// Wczytujemy zapisane ustawienia przy starcie klienta!
		ModConfig.load();

		menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.magnetic.menu",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				KeyMapping.Category.MISC
		));

		zoomKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.magnetic.zoom",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_C,
				KeyMapping.Category.MISC
		));

		waypointKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.magnetic.waypoint",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_B,
				KeyMapping.Category.MISC
		));

		HudRenderCallback.EVENT.register((graphics, delta) -> {
			MagneticHudRenderer.render(graphics, Minecraft.getInstance());
		});

		// Własne menu główne zamiast vanilla'owego ekranu tytułowego
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (ModConfig.customMainMenu && screen.getClass() == TitleScreen.class) {
				client.execute(() -> client.setScreen(new MagneticTitleScreen()));
			}
		});

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide()) {
				if (entity instanceof LivingEntity living) {
					lastTarget = living;
					targetTicks = 80;
				}

				if (ModConfig.hitParticles) {
					double x = entity.getX();
					double y = entity.getY() + (entity.getBbHeight() * 0.55);
					double z = entity.getZ();
					MagneticWorldRenderer.addHitBurst(x, y, z);
				}
			}
			return InteractionResult.PASS;
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.level == null) return;

			if (ModConfig.blackHoleSky || ModConfig.alwaysNight) {
				client.options.cloudStatus().set(CloudStatus.OFF);
				client.level.getLevelData().setGameTime(18000L);
			}

			ModConfig.isZooming = ModConfig.zoomEnabled && zoomKey.isDown() && client.screen == null;

			long now = System.currentTimeMillis();
			boolean leftDown = client.options.keyAttack.isDown();
			boolean rightDown = client.options.keyUse.isDown();

			if (leftDown && !lastLeftDown) leftClicks.add(now);
			if (rightDown && !lastRightDown) rightClicks.add(now);
			lastLeftDown = leftDown;
			lastRightDown = rightDown;

			leftClicks.removeIf(t -> now - t > 1000);
			rightClicks.removeIf(t -> now - t > 1000);

			while (menuKey.consumeClick()) {
				client.setScreen(new MagneticMenuScreen());
			}

			while (waypointKey.consumeClick()) {
				WaypointManager.addWaypointAtPlayer(client.player.getX(), client.player.getY(), client.player.getZ());
			}

			if (ModConfig.killEffect && lastTarget != null && (lastTarget.isDeadOrDying() || !lastTarget.isAlive())) {
				triggerKillEffect(client, lastTarget.getX(), lastTarget.getY(), lastTarget.getZ());
				lastTarget = null;
				targetTicks = 0;
			}

			if (targetTicks > 0) {
				targetTicks--;
			}

			boolean onGroundNow = client.player.onGround();

			if (ModConfig.jumpCircles) {
				if (wasOnGround && !onGroundNow && client.player.getDeltaMovement().y > 0) {
					MagneticWorldRenderer.addJumpRipple(client.player.getX(), client.player.getY(), client.player.getZ(), false);
				} else if (!wasOnGround && onGroundNow && lastFallVelocity < -0.08) {
					MagneticWorldRenderer.addJumpRipple(client.player.getX(), client.player.getY(), client.player.getZ(), true);
				}
			}

			lastFallVelocity = client.player.getDeltaMovement().y;
			wasOnGround = onGroundNow;

			MagneticWorldRenderer.tickEffects(client);
		});
	}

	private void triggerKillEffect(Minecraft client, double x, double y, double z) {
		if (client.level == null) return;
		client.level.playLocalSound(x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 1.0F, false);
		MagneticWorldRenderer.addKillShockwave(x, y, z);
	}
}