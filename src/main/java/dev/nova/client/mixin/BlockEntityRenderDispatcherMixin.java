package dev.nova.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.nova.client.render.XRayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin
{
	/** Versteckt Kisten, Schilder usw., die bei X-Ray nicht in der Liste stehen. */
	@Inject(
		method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At("HEAD"),
		cancellable = true)
	private <S extends BlockEntityRenderState> void novaOnSubmit(S renderState,
		PoseStack matrices, SubmitNodeCollector queue,
		CameraRenderState cameraRenderState, CallbackInfo ci)
	{
		if(!XRayState.isActive())
			return;

		var level = Minecraft.getInstance().level;
		if(level == null)
			return;

		BlockState state = level.getBlockState(renderState.blockPos);
		if(XRayState.isHidden(state))
			ci.cancel();
	}
}
