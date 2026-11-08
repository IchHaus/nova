package dev.nova.client.hacks;

import dev.nova.client.core.BoolSetting;
import dev.nova.client.core.Category;
import dev.nova.client.core.Hack;
import dev.nova.client.core.ModeSetting;
import dev.nova.client.core.NumSetting;
import dev.nova.client.util.Util;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bewegungs-Hacks. */
public final class MovementHacks
{
	private MovementHacks()
	{}

	// ---------------------------------------------------------------

	public static final class Flight extends Hack
	{
		private final NumSetting speed = add(new NumSetting("Speed",
			"Horizontale Geschwindigkeit (Bloecke pro Tick).", 0.8, 0.1, 5, 0.1));
		private final NumSetting vertical = add(new NumSetting("Vertical",
			"Vertikale Geschwindigkeit (Bloecke pro Tick).", 0.5, 0.1, 3, 0.1));

		public Flight()
		{
			super("Flight", "Du kannst fliegen. Leertaste hoch, Shift runter.",
				Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			Vec3 h = Util.horizontalMove(speed.get());
			double y = 0;
			if(MC.options.keyJump.isDown())
				y += vertical.get();
			if(MC.options.keyShift.isDown())
				y -= vertical.get();

			p.setDeltaMovement(h.x, y, h.z);
			p.fallDistance = 0;
		}

		@Override
		public String getSuffix()
		{
			return speed.display();
		}
	}

	// ---------------------------------------------------------------

	public static final class Jetpack extends Hack
	{
		private final NumSetting power = add(new NumSetting("Power",
			"Auftrieb pro Tick.", 0.3, 0.1, 1, 0.05));

		public Jetpack()
		{
			super("Jetpack", "Halte Leertaste, um in der Luft zu steigen.",
				Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			if(!MC.options.keyJump.isDown())
				return;

			LocalPlayer p = MC.player;
			Vec3 v = p.getDeltaMovement();
			p.setDeltaMovement(v.x, power.get(), v.z);
		}
	}

	// ---------------------------------------------------------------

	public static final class Glide extends Hack
	{
		private final NumSetting fall = add(new NumSetting("Fall speed",
			"Maximale Fallgeschwindigkeit.", 0.1, 0.01, 0.5, 0.01));

		public Glide()
		{
			super("Glide", "Du faellst nur noch langsam.", Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(p.onGround() || p.isInWater() || p.getAbilities().flying)
				return;

			Vec3 v = p.getDeltaMovement();
			if(v.y < -fall.get())
				p.setDeltaMovement(v.x, -fall.get(), v.z);
		}
	}

	// ---------------------------------------------------------------

	public static final class NoFall extends Hack
	{
		public NoFall()
		{
			super("NoFall", "Verhindert Fallschaden.", Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(p.fallDistance > 2 && !p.getAbilities().flying)
				p.connection
					.send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
		}
	}

	// ---------------------------------------------------------------

	public static final class AutoSprint extends Hack
	{
		public AutoSprint()
		{
			super("AutoSprint", "Du sprintest automatisch, wenn du vorwaerts gehst.",
				Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(p.horizontalCollision || p.isShiftKeyDown() || p.isSwimming())
				return;

			if(p.zza <= 0)
				return;

			p.setSprinting(true);
		}
	}

	// ---------------------------------------------------------------

	public static final class Spider extends Hack
	{
		public Spider()
		{
			super("Spider", "Klettere an Waenden hoch.", Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(!p.horizontalCollision)
				return;

			Vec3 v = p.getDeltaMovement();
			p.setDeltaMovement(v.x, 0.2, v.z);
		}
	}

	// ---------------------------------------------------------------

	public static final class Dolphin extends Hack
	{
		public Dolphin()
		{
			super("Dolphin", "Du treibst an der Wasseroberflaeche.",
				Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(!p.isInWater() || p.isShiftKeyDown())
				return;

			Vec3 v = p.getDeltaMovement();
			p.setDeltaMovement(v.x, Math.max(v.y, 0.1), v.z);
		}
	}

	// ---------------------------------------------------------------

	public static final class HighJump extends Hack
	{
		private final NumSetting height = add(new NumSetting("Power",
			"Sprungkraft (normal ist 0.42).", 0.8, 0.5, 3, 0.1));

		public HighJump()
		{
			super("HighJump", "Du springst viel hoeher.", Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			Vec3 v = p.getDeltaMovement();
			// Der Tick, in dem Minecraft den normalen Sprung ausgeloest hat
			boolean justJumped = !p.onGround() && Math.abs(v.y - 0.3332) < 0.02
				&& MC.options.keyJump.isDown();
			if(justJumped)
				p.setDeltaMovement(v.x, height.get(), v.z);
		}
	}

	// ---------------------------------------------------------------

	public static final class AirJump extends Hack
	{
		private boolean wasJumping;

		public AirJump()
		{
			super("AirJump", "Springe beliebig oft in der Luft.",
				Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			boolean jumping = MC.options.keyJump.isDown();
			if(jumping && !wasJumping && !p.onGround())
			{
				Vec3 v = p.getDeltaMovement();
				p.setDeltaMovement(v.x, 0.42, v.z);
			}

			wasJumping = jumping;
		}
	}

	// ---------------------------------------------------------------

	public static final class FastLadder extends Hack
	{
		public FastLadder()
		{
			super("FastLadder", "Du kletterst Leitern schneller.",
				Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(!p.onClimbable() || !p.horizontalCollision)
				return;

			if(!MC.options.keyUp.isDown() && !MC.options.keyJump.isDown())
				return;

			Vec3 v = p.getDeltaMovement();
			p.setDeltaMovement(v.x, 0.28, v.z);
		}
	}

	// ---------------------------------------------------------------

	public static final class Parkour extends Hack
	{
		public Parkour()
		{
			super("Parkour", "Springt automatisch an Kanten ab.",
				Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(!p.onGround() || p.isShiftKeyDown() || !Util.isMoving())
				return;

			AABB feet = new AABB(p.getX() - 0.1, p.getY() - 0.5, p.getZ() - 0.1,
				p.getX() + 0.1, p.getY(), p.getZ() + 0.1);
			if(MC.level.noCollision(p, feet))
				p.jumpFromGround();
		}
	}

	// ---------------------------------------------------------------

	public static final class BunnyHop extends Hack
	{
		private final ModeSetting jumpIf = add(new ModeSetting("Jump if",
			"Wann gesprungen wird.", "Sprinting", "Sprinting", "Walking",
			"Always"));

		public BunnyHop()
		{
			super("BunnyHop", "Springt automatisch weiter, sobald du landest.",
				Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(!p.onGround() || p.isShiftKeyDown())
				return;

			boolean walking = p.zza != 0 || p.xxa != 0;
			boolean ok;
			if(jumpIf.is("Always"))
				ok = true;
			else if(jumpIf.is("Walking"))
				ok = walking;
			else
				ok = p.isSprinting() && walking;

			if(ok)
				p.jumpFromGround();
		}

		@Override
		public String getSuffix()
		{
			return jumpIf.get();
		}
	}

	// ---------------------------------------------------------------

	public static final class AutoWalk extends Hack
	{
		public AutoWalk()
		{
			super("AutoWalk", "Du gehst automatisch geradeaus.",
				Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			MC.options.keyUp.setDown(true);
		}

		@Override
		protected void onDisable()
		{
			MC.options.keyUp.setDown(false);
		}
	}

	// ---------------------------------------------------------------

	public static final class Sneak extends Hack
	{
		public Sneak()
		{
			super("Sneak", "Du schleichst dauerhaft.", Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			MC.options.keyShift.setDown(true);
		}

		@Override
		protected void onDisable()
		{
			MC.options.keyShift.setDown(false);
		}
	}

	// ---------------------------------------------------------------

	public static final class Step extends Hack
	{
		private final NumSetting height = add(new NumSetting("Height",
			"Wie hohe Bloecke du hochsteigen kannst.", 1.5, 1, 5, 0.5));

		public Step()
		{
			super("Step", "Du steigst Bloecke ohne Springen hoch.",
				Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			var attr = MC.player.getAttribute(Attributes.STEP_HEIGHT);
			if(attr != null)
				attr.setBaseValue(height.get());
		}

		@Override
		protected void onDisable()
		{
			if(MC.player == null)
				return;

			var attr = MC.player.getAttribute(Attributes.STEP_HEIGHT);
			if(attr != null)
				attr.setBaseValue(0.6);
		}

		@Override
		public String getSuffix()
		{
			return height.display();
		}
	}

	// ---------------------------------------------------------------

	public static final class SpeedHack extends Hack
	{
		private final NumSetting factor = add(new NumSetting("Factor",
			"Beschleunigung pro Tick am Boden.", 1.3, 1.0, 2.5, 0.05));
		private final BoolSetting onlyGround = add(new BoolSetting("Ground only",
			"Nur am Boden beschleunigen.", true));

		public SpeedHack()
		{
			super("Speed", "Du laeufst schneller.", Category.MOVEMENT);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(!Util.isMoving() || onlyGround.get() && !p.onGround())
				return;

			Vec3 v = p.getDeltaMovement();
			p.setDeltaMovement(v.x * factor.get(), v.y, v.z * factor.get());
		}

		@Override
		public String getSuffix()
		{
			return factor.display();
		}
	}
}
