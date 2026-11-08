package dev.nova.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.nova.client.render.XRayState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;

@Mixin(Camera.class)
public abstract class CameraMixin
{
	/**
	 * Schaltet das Hoehlen-Culling ab. Sonst werden Chunk-Abschnitte, die von
	 * Stein umschlossen sind, gar nicht gezeichnet, und tiefe Erze waeren unsichtbar.
	 */
	@Inject(
		method = "extractRenderState(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At("RETURN"))
	private void novaDisableSmartCull(CameraRenderState cameraState,
		DeltaTracker deltaTracker, CallbackInfo ci)
	{
		if(!XRayState.isActive())
			return;

		XRayState.cameraHookRan = true;
		cameraState.smartCull = false;
	}
}
