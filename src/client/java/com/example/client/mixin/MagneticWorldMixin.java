package com.example.client.mixin;

import com.example.client.MagneticWorldRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MagneticWorldMixin {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevelTail(CallbackInfo ci) {
        MagneticWorldRenderer.renderGeometryRipples();
    }
}