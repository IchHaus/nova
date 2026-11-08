package dev.nova.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.nova.client.render.XRayState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockStateBase.class)
public abstract class BlockStateBaseMixin
{
	/**
	 * Versteckt bei aktivem X-Ray alle Bloecke, die nicht in der Liste stehen,
	 * auch Pflanzen, Schnee und Fackeln (deren Flaechen sonst nie weggeschnitten
	 * werden).
	 */
	@Inject(method = "getRenderShape", at = @At("RETURN"), cancellable = true)
	private void novaGetRenderShape(CallbackInfoReturnable<RenderShape> cir)
	{
		if(!XRayState.isActive())
			return;

		BlockState self = (BlockState)(Object)this;
		if(XRayState.isHidden(self))
		{
			XRayState.shapeHookRan = true;
			cir.setReturnValue(RenderShape.INVISIBLE);
		}
	}
}
