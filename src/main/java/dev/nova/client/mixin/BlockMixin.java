package dev.nova.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.nova.client.render.XRayState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(Block.class)
public class BlockMixin
{
	/**
	 * Mit X-Ray werden alle Seiten sichtbarer Bloecke gezeichnet (auch wenn ein
	 * Stein daneben liegt) und alle Seiten versteckter Bloecke weggelassen.
	 *
	 * Das ist die Variante von NeoForge mit zusaetzlichem Level und Position;
	 * die alte Methode mit drei Parametern ruft diese auf.
	 */
	@Inject(
		method = "shouldRenderFace(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z",
		at = @At("HEAD"),
		cancellable = true)
	private static void novaShouldRenderFace(BlockGetter level, BlockPos pos,
		BlockState state, BlockState neighborState, Direction direction,
		CallbackInfoReturnable<Boolean> cir)
	{
		if(!XRayState.isActive())
			return;

		XRayState.faceHookRan = true;
		cir.setReturnValue(!XRayState.isHidden(state));
	}
}
