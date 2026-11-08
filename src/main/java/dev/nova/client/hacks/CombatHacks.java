package dev.nova.client.hacks;

import java.util.List;

import dev.nova.client.core.BoolSetting;
import dev.nova.client.core.Category;
import dev.nova.client.core.Hack;
import dev.nova.client.core.NumSetting;
import dev.nova.client.util.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

/** Kampf-Hacks. */
public final class CombatHacks
{
	private CombatHacks()
	{}

	// ---------------------------------------------------------------

	public static final class Killaura extends Hack
	{
		private final NumSetting range = add(new NumSetting("Range",
			"Reichweite in Bloecken.", 4.2, 1, 6, 0.1));
		private final BoolSetting players =
			add(new BoolSetting("Players", "Spieler angreifen.", true));
		private final BoolSetting hostile =
			add(new BoolSetting("Hostile", "Feindliche Mobs angreifen.", true));
		private final BoolSetting passive =
			add(new BoolSetting("Passive", "Friedliche Mobs angreifen.", false));
		private final BoolSetting multi = add(new BoolSetting("Multi",
			"Mehrere Ziele pro Schlag treffen.", false));
		private final BoolSetting aim = add(new BoolSetting("Aim",
			"Blickt automatisch auf das Ziel.", true));

		public Killaura()
		{
			super("Killaura", "Greift Ziele in deiner Naehe automatisch an.",
				Category.COMBAT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(p.getAttackStrengthScale(0) < 1)
				return;

			List<LivingEntity> targets = Util.targets(range.get(),
				players.get(), hostile.get(), passive.get());
			if(targets.isEmpty())
				return;

			int count = multi.get() ? Math.min(targets.size(), 4) : 1;
			for(int i = 0; i < count; i++)
			{
				LivingEntity target = targets.get(i);
				if(aim.get() && i == 0)
				{
					float[] rot =
						Util.rotationsTo(target.getBoundingBox().getCenter());
					p.setYRot(rot[0]);
					p.setXRot(rot[1]);
				}

				MC.gameMode.attack(p, target);
				p.swing(InteractionHand.MAIN_HAND);
			}
		}

		@Override
		public String getSuffix()
		{
			return range.display();
		}
	}

	// ---------------------------------------------------------------

	public static final class TriggerBot extends Hack
	{
		private final BoolSetting players =
			add(new BoolSetting("Players", "Spieler angreifen.", true));
		private final BoolSetting hostile =
			add(new BoolSetting("Hostile", "Feindliche Mobs angreifen.", true));
		private final BoolSetting passive =
			add(new BoolSetting("Passive", "Friedliche Mobs angreifen.", false));

		public TriggerBot()
		{
			super("TriggerBot",
				"Schlaegt automatisch zu, sobald dein Fadenkreuz ein Ziel trifft.",
				Category.COMBAT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(MC.gui.screen() != null || p.getAttackStrengthScale(0) < 1)
				return;

			if(!(MC.hitResult instanceof EntityHitResult hit)
				|| !(hit.getEntity() instanceof LivingEntity target))
				return;

			if(!target.isAlive() || target == p || Util.isFriend(target))
				return;

			boolean ok;
			if(target instanceof Player)
				ok = players.get();
			else if(target instanceof Mob)
				ok = target instanceof Enemy ? hostile.get() : passive.get();
			else
				ok = false;

			if(!ok)
				return;

			MC.gameMode.attack(p, target);
			p.swing(InteractionHand.MAIN_HAND);
		}
	}

	// ---------------------------------------------------------------

	public static final class AimAssist extends Hack
	{
		private final NumSetting range = add(new NumSetting("Range",
			"Reichweite in Bloecken.", 5, 2, 8, 0.5));
		private final NumSetting speed = add(new NumSetting("Speed",
			"Maximale Drehung pro Tick (Grad).", 5, 1, 30, 1));
		private final NumSetting fov = add(new NumSetting("FOV",
			"Nur Ziele in diesem Sichtfeld (Grad).", 120, 30, 360, 10));
		private final BoolSetting onlyAttack = add(new BoolSetting(
			"Only attacking", "Nur zielen, solange du Angriff haeltst.", true));

		public AimAssist()
		{
			super("AimAssist", "Zieht dein Fadenkreuz sanft zum naechsten Ziel.",
				Category.COMBAT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(MC.gui.screen() != null)
				return;

			if(onlyAttack.get() && !MC.options.keyAttack.isDown())
				return;

			for(LivingEntity target : Util.targets(range.get(), true, true,
				false))
			{
				float[] rot =
					Util.rotationsTo(target.getBoundingBox().getCenter());
				float yawDiff = Util.angleDiff(p.getYRot(), rot[0]);
				if(Math.abs(yawDiff) > fov.get() / 2.0)
					continue;

				float pitchDiff = rot[1] - p.getXRot();
				float max = speed.getF();
				p.setYRot(p.getYRot() + Mth.clamp(yawDiff, -max, max));
				p.setXRot(p.getXRot() + Mth.clamp(pitchDiff, -max, max));
				return;
			}
		}
	}

	// ---------------------------------------------------------------

	public static final class AutoTotem extends Hack
	{
		private final NumSetting delay = add(new NumSetting("Delay",
			"Ticks zwischen zwei Aktionen.", 3, 0, 20, 1));
		private int timer;

		public AutoTotem()
		{
			super("AutoTotem", "Haelt immer einen Totem in der Nebenhand.",
				Category.COMBAT);
		}

		@Override
		public void onTick()
		{
			if(timer > 0)
			{
				timer--;
				return;
			}

			LocalPlayer p = MC.player;
			if(p.getOffhandItem().is(Items.TOTEM_OF_UNDYING))
				return;

			Inventory inv = p.getInventory();
			for(int i = 0; i < 36; i++)
			{
				if(!inv.getItem(i).is(Items.TOTEM_OF_UNDYING))
					continue;

				// Inventar-Slot -> Slot-ID im Spieler-Menue
				int menuSlot = i < 9 ? 36 + i : i;
				MC.gameMode.handleContainerInput(0, menuSlot, 40,
					ContainerInput.SWAP, p);
				timer = delay.getI();
				return;
			}
		}
	}

	// ---------------------------------------------------------------

	public static final class AutoEat extends Hack
	{
		private final NumSetting hunger = add(new NumSetting("Hunger",
			"Isst, sobald die Hungerleiste so niedrig ist (max. 20).", 14, 1, 19,
			1));
		private boolean eating;
		private int oldSlot = -1;

		public AutoEat()
		{
			super("AutoEat", "Isst automatisch, wenn du Hunger hast.",
				Category.COMBAT);
		}

		@Override
		protected void onDisable()
		{
			if(eating)
				stopEating();
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			Inventory inv = p.getInventory();

			if(eating)
			{
				boolean full = p.getFoodData().getFoodLevel() >= 20;
				if(full || !p.getMainHandItem().has(DataComponents.FOOD))
				{
					stopEating();
					return;
				}

				MC.options.keyUse.setDown(true);
				if(!p.isUsingItem())
					MC.startUseItem();
				return;
			}

			if(p.getFoodData().getFoodLevel() > hunger.getI()
				|| MC.gui.screen() != null)
				return;

			for(int i = 0; i < 9; i++)
			{
				ItemStack stack = inv.getItem(i);
				if(!stack.has(DataComponents.FOOD) || isBadFood(stack))
					continue;

				oldSlot = inv.getSelectedSlot();
				inv.setSelectedSlot(i);
				eating = true;
				return;
			}
		}

		private boolean isBadFood(ItemStack s)
		{
			return s.is(Items.ROTTEN_FLESH) || s.is(Items.SPIDER_EYE)
				|| s.is(Items.PUFFERFISH) || s.is(Items.POISONOUS_POTATO)
				|| s.is(Items.CHORUS_FRUIT)
				|| s.is(Items.ENCHANTED_GOLDEN_APPLE);
		}

		private void stopEating()
		{
			MC.options.keyUse.setDown(false);
			if(oldSlot >= 0 && MC.player != null)
				MC.player.getInventory().setSelectedSlot(oldSlot);

			oldSlot = -1;
			eating = false;
		}
	}

	// ---------------------------------------------------------------

	public static final class AutoLeave extends Hack
	{
		private final NumSetting hearts = add(new NumSetting("Hearts",
			"Verlaesst den Server bei so wenigen Herzen.", 4, 1, 10, 0.5));

		public AutoLeave()
		{
			super("AutoLeave",
				"Verlaesst den Server automatisch bei niedriger Gesundheit.",
				Category.COMBAT);
		}

		@Override
		public void onTick()
		{
			if(MC.player.getHealth() > hearts.getF() * 2F)
				return;

			MC.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
			setEnabled(false);
		}

		@Override
		public String getSuffix()
		{
			return hearts.display();
		}
	}
}
