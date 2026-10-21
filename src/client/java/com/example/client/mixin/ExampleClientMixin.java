package com.example.client.mixin;

import com.example.client.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ExampleClientMixin {

	@Inject(method = "renderArmWithItem", at = @At("HEAD"))
	private void magnetic$head(
			AbstractClientPlayer player,
			float tickDelta,
			float pitch,
			InteractionHand hand,
			float swingProgress,
			ItemStack item,
			float equipProgress,
			PoseStack poseStack,
			@Coerce Object bufferOrCollector,
			int light,
			CallbackInfo ci
	) {
		poseStack.pushPose();

		boolean isMain = (hand == InteractionHand.MAIN_HAND);

		if (isMain && ModConfig.customMainHand) {
			poseStack.translate(ModConfig.mainHandX, ModConfig.mainHandY, ModConfig.mainHandZ);
			if (ModConfig.mainHandRotY != 0.0f) {
				poseStack.mulPose(Axis.YP.rotationDegrees(ModConfig.mainHandRotY));
			}
			poseStack.scale(ModConfig.mainHandScale, ModConfig.mainHandScale, ModConfig.mainHandScale);
		} else if (!isMain && ModConfig.customOffHand) {
			poseStack.translate(ModConfig.offHandX, ModConfig.offHandY, ModConfig.offHandZ);
			if (ModConfig.offHandRotY != 0.0f) {
				poseStack.mulPose(Axis.YP.rotationDegrees(ModConfig.offHandRotY));
			}
			poseStack.scale(ModConfig.offHandScale, ModConfig.offHandScale, ModConfig.offHandScale);
		}
	}

	@Inject(method = "renderArmWithItem", at = @At("RETURN"))
	private void magnetic$return(
			AbstractClientPlayer player,
			float tickDelta,
			float pitch,
			InteractionHand hand,
			float swingProgress,
			ItemStack item,
			float equipProgress,
			PoseStack poseStack,
			@Coerce Object bufferOrCollector,
			int light,
			CallbackInfo ci
	) {
		poseStack.popPose();
	}
}