package dev.nova.client.hacks;

import java.util.Random;

import dev.nova.client.core.BoolSetting;
import dev.nova.client.core.Category;
import dev.nova.client.core.Hack;
import dev.nova.client.core.NumSetting;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Spieler-Hacks. */
public final class PlayerHacks
{
	private PlayerHacks()
	{}

	// ---------------------------------------------------------------

	public static final class AutoRespawn extends Hack
	{
		public AutoRespawn()
		{
			super("AutoRespawn", "Respawnt sofort nach dem Tod.", Category.PLAYER);
		}

		@Override
		public void onTick()
		{
			if(MC.gui.screen() instanceof DeathScreen)
				MC.player.respawn();
		}
	}

	// ---------------------------------------------------------------

	public static final class AutoArmor extends Hack
	{
		private int timer;

		public AutoArmor()
		{
			super("AutoArmor", "Zieht Ruestung aus dem Inventar automatisch an.",
				Category.PLAYER);
		}

		@Override
		public void onTick()
		{
			if(timer-- > 0)
				return;

			LocalPlayer p = MC.player;
			Inventory inv = p.getInventory();

			EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
				EquipmentSlot.LEGS, EquipmentSlot.FEET};

			for(int s = 0; s < slots.length; s++)
			{
				if(!p.getItemBySlot(slots[s]).isEmpty())
					continue;

				for(int i = 0; i < 36; i++)
				{
					ItemStack stack = inv.getItem(i);
					Equippable eq = stack.get(DataComponents.EQUIPPABLE);
					if(eq == null || eq.slot() != slots[s])
						continue;

					int menuSlot = i < 9 ? 36 + i : i;
					MC.gameMode.handleContainerInput(0, menuSlot, 0,
						ContainerInput.QUICK_MOVE, p);
					timer = 4;
					return;
				}
			}
		}
	}

	// ---------------------------------------------------------------

	public static final class FastPlace extends Hack
	{
		public FastPlace()
		{
			super("FastPlace", "Bloecke und Items ohne Verzoegerung benutzen.",
				Category.PLAYER);
		}

		@Override
		public void onTick()
		{
			MC.rightClickDelay = 0;
		}
	}

	// ---------------------------------------------------------------

	public static final class FastBreak extends Hack
	{
		public FastBreak()
		{
			super("FastBreak", "Keine Pause zwischen zwei abgebauten Bloecken.",
				Category.PLAYER);
		}

		@Override
		public void onTick()
		{
			MC.gameMode.destroyDelay = 0;
		}
	}

	// ---------------------------------------------------------------

	public static final class AutoTool extends Hack
	{
		private int oldSlot = -1;

		public AutoTool()
		{
			super("AutoTool", "Waehlt beim Abbauen das schnellste Werkzeug.",
				Category.PLAYER);
		}

		@Override
		protected void onDisable()
		{
			restore();
		}

		private void restore()
		{
			if(oldSlot >= 0 && MC.player != null)
				MC.player.getInventory().setSelectedSlot(oldSlot);

			oldSlot = -1;
		}

		@Override
		public void onTick()
		{
			if(MC.gui.screen() != null || !MC.options.keyAttack.isDown()
				|| !(MC.hitResult instanceof BlockHitResult hit)
				|| hit.getType() != HitResult.Type.BLOCK)
			{
				restore();
				return;
			}

			BlockState state = MC.level.getBlockState(hit.getBlockPos());
			Inventory inv = MC.player.getInventory();

			int best = inv.getSelectedSlot();
			float bestSpeed = inv.getItem(best).getDestroySpeed(state);
			for(int i = 0; i < 9; i++)
			{
				float speed = inv.getItem(i).getDestroySpeed(state);
				if(speed > bestSpeed)
				{
					bestSpeed = speed;
					best = i;
				}
			}

			if(best != inv.getSelectedSlot())
			{
				if(oldSlot < 0)
					oldSlot = inv.getSelectedSlot();

				inv.setSelectedSlot(best);
			}
		}
	}

	// ---------------------------------------------------------------

	public static final class AntiAfk extends Hack
	{
		private final Random random = new Random();
		private int timer;

		public AntiAfk()
		{
			super("AntiAFK", "Bewegt dich ab und zu, damit du nicht gekickt wirst.",
				Category.PLAYER);
		}

		@Override
		public void onTick()
		{
			if(timer-- > 0)
				return;

			timer = 60 + random.nextInt(80);
			LocalPlayer p = MC.player;
			p.setYRot(p.getYRot() + random.nextInt(61) - 30);
			if(p.onGround())
				p.jumpFromGround();
		}
	}

	// ---------------------------------------------------------------

	public static final class ChestStealer extends Hack
	{
		private final NumSetting delay = add(new NumSetting("Delay",
			"Ticks zwischen zwei Items.", 1, 0, 10, 1));
		private int timer;

		public ChestStealer()
		{
			super("ChestStealer", "Nimmt alles aus geoeffneten Kisten mit.",
				Category.PLAYER);
		}

		@Override
		public void onTick()
		{
			if(!(MC.gui.screen() instanceof ContainerScreen screen))
				return;

			if(timer-- > 0)
				return;

			ChestMenu menu = screen.getMenu();
			int size = menu.getRowCount() * 9;
			for(int i = 0; i < size; i++)
			{
				if(menu.slots.get(i).getItem().isEmpty())
					continue;

				MC.gameMode.handleContainerInput(menu.containerId, i, 0,
					ContainerInput.QUICK_MOVE, MC.player);
				timer = delay.getI();
				return;
			}
		}
	}
}
