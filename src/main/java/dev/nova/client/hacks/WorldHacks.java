package dev.nova.client.hacks;

import dev.nova.client.core.BoolSetting;
import dev.nova.client.core.Category;
import dev.nova.client.core.Hack;
import dev.nova.client.core.NumSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Welt-Hacks (Bloecke abbauen und setzen). */
public final class WorldHacks
{
	private WorldHacks()
	{}

	// ---------------------------------------------------------------

	public static final class Nuker extends Hack
	{
		private final NumSetting range = add(new NumSetting("Range",
			"Reichweite in Bloecken.", 4, 1, 6, 1));
		private final NumSetting perTick = add(new NumSetting("Blocks/tick",
			"Wie viele Bloecke pro Tick im Kreativmodus.", 6, 1, 30, 1));
		private final BoolSetting flat = add(new BoolSetting("Flat",
			"Nur Bloecke auf Augenhoehe und darueber.", false));

		public Nuker()
		{
			super("Nuker", "Baut alle Bloecke in deiner Naehe ab.", Category.WORLD);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			BlockPos center = p.blockPosition();
			int r = range.getI();
			BlockPos best = null;
			double bestDist = Double.MAX_VALUE;
			int count = 0;
			BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

			for(int x = -r; x <= r; x++)
				for(int y = -r; y <= r; y++)
					for(int z = -r; z <= r; z++)
					{
						m.set(center.getX() + x, center.getY() + y,
							center.getZ() + z);
						if(flat.get() && m.getY() < center.getY())
							continue;

						BlockState state = MC.level.getBlockState(m);
						if(state.isAir() || !state.getFluidState().isEmpty()
							|| state.getDestroySpeed(MC.level, m) < 0)
							continue;

						double dist = p.getEyePosition()
							.distanceToSqr(Vec3.atCenterOf(m));
						if(dist > r * r)
							continue;

						if(p.isCreative())
						{
							if(count++ < perTick.getI())
								MC.gameMode.continueDestroyBlock(m.immutable(),
									Direction.UP);

						}else if(dist < bestDist)
						{
							bestDist = dist;
							best = m.immutable();
						}
					}

			if(best != null)
			{
				MC.gameMode.continueDestroyBlock(best, Direction.UP);
				p.swing(InteractionHand.MAIN_HAND);
			}
		}
	}

	// ---------------------------------------------------------------

	public static final class Scaffold extends Hack
	{
		public Scaffold()
		{
			super("Scaffold", "Setzt automatisch Bloecke unter deine Fuesse.",
				Category.WORLD);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			Inventory inv = p.getInventory();

			BlockPos target = p.blockPosition().below();
			BlockState targetState = MC.level.getBlockState(target);
			if(!targetState.canBeReplaced())
				return;

			int slot = findBlockSlot(inv);
			if(slot < 0)
				return;

			for(Direction d : Direction.values())
			{
				BlockPos neighbor = target.relative(d);
				BlockState ns = MC.level.getBlockState(neighbor);
				if(ns.isAir() || ns.canBeReplaced()
					|| !ns.getFluidState().isEmpty())
					continue;

				Direction face = d.getOpposite();
				Vec3 hitPos = Vec3.atCenterOf(neighbor).add(face.getStepX() * 0.5,
					face.getStepY() * 0.5, face.getStepZ() * 0.5);

				int old = inv.getSelectedSlot();
				inv.setSelectedSlot(slot);
				MC.gameMode.useItemOn(p, InteractionHand.MAIN_HAND,
					new BlockHitResult(hitPos, face, neighbor, false));
				p.swing(InteractionHand.MAIN_HAND);
				inv.setSelectedSlot(old);
				return;
			}
		}

		private int findBlockSlot(Inventory inv)
		{
			for(int i = 0; i < 9; i++)
			{
				if(!(inv.getItem(i).getItem() instanceof BlockItem bi))
					continue;

				if(bi.getBlock() instanceof FallingBlock
					|| bi.getBlock() instanceof EntityBlock)
					continue;

				return i;
			}

			return -1;
		}
	}

	// ---------------------------------------------------------------

	public static final class AutoMine extends Hack
	{
		public AutoMine()
		{
			super("AutoMine", "Haelt die Angriffstaste fuer dich gedrueckt.",
				Category.WORLD);
		}

		@Override
		public void onTick()
		{
			if(MC.gui.screen() == null)
				MC.options.keyAttack.setDown(true);
		}

		@Override
		protected void onDisable()
		{
			MC.options.keyAttack.setDown(false);
		}
	}
}
