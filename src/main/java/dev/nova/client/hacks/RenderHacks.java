package dev.nova.client.hacks;

import java.util.ArrayList;
import java.util.List;

import dev.nova.client.core.BoolSetting;
import dev.nova.client.core.Category;
import dev.nova.client.core.Hack;
import dev.nova.client.core.ListSetting;
import dev.nova.client.core.NumSetting;
import dev.nova.client.render.EspRenderer;
import dev.nova.client.render.XRayState;
import dev.nova.client.util.Theme;
import dev.nova.client.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Render-Hacks (alles, was du siehst). */
public final class RenderHacks
{
	private RenderHacks()
	{}

	// ---------------------------------------------------------------

	private static int entityColor(Entity e)
	{
		if(e instanceof Player)
			return Util.isFriend(e) ? 0xFF4ADE80 : 0xFF22D3EE;
		if(e instanceof Enemy)
			return 0xFFF87171;
		if(e instanceof ItemEntity)
			return 0xFFA78BFA;
		return 0xFFFACC15;
	}

	public static final class Esp extends Hack
	{
		private final BoolSetting players =
			add(new BoolSetting("Players", "Spieler anzeigen.", true));
		private final BoolSetting hostile =
			add(new BoolSetting("Hostile", "Feindliche Mobs anzeigen.", true));
		private final BoolSetting passive =
			add(new BoolSetting("Passive", "Friedliche Mobs anzeigen.", false));
		private final BoolSetting items =
			add(new BoolSetting("Items", "Herumliegende Items anzeigen.", false));
		private final BoolSetting fill = add(
			new BoolSetting("Fill", "Boxen leicht ausfuellen.", true));

		public Esp()
		{
			super("ESP", "Zeigt Spieler, Mobs und Items durch Waende.",
				Category.RENDER);
		}

		@Override
		public void onRender3D(EspRenderer r)
		{
			float pt = r.partialTicks();
			for(Entity e : MC.level.entitiesForRendering())
			{
				if(e == MC.player || e.isRemoved())
					continue;

				boolean show;
				if(e instanceof Player p)
					show = players.get() && !p.isSpectator();
				else if(e instanceof Mob)
					show = e instanceof Enemy ? hostile.get() : passive.get();
				else if(e instanceof ItemEntity)
					show = items.get();
				else
					show = false;

				if(!show)
					continue;

				AABB box = Util.lerpBox(e, pt);
				int color = entityColor(e);
				if(fill.get())
					r.boxWithFill(box, color, 0x30);
				else
					r.box(box, color);
			}
		}
	}

	// ---------------------------------------------------------------

	public static final class Tracers extends Hack
	{
		private final BoolSetting players =
			add(new BoolSetting("Players", "Spieler anzeigen.", true));
		private final BoolSetting hostile =
			add(new BoolSetting("Hostile", "Feindliche Mobs anzeigen.", true));
		private final BoolSetting passive =
			add(new BoolSetting("Passive", "Friedliche Mobs anzeigen.", false));

		public Tracers()
		{
			super("Tracers", "Zieht Linien von deinem Fadenkreuz zu Zielen.",
				Category.RENDER);
		}

		@Override
		public void onRender3D(EspRenderer r)
		{
			float pt = r.partialTicks();
			LocalPlayer p = MC.player;
			Vec3 start = r.camera().add(p.getViewVector(pt).scale(0.6));

			for(Entity e : MC.level.entitiesForRendering())
			{
				if(e == p || e.isRemoved() || !(e instanceof LivingEntity))
					continue;

				boolean show;
				if(e instanceof Player pl)
					show = players.get() && !pl.isSpectator();
				else if(e instanceof Mob)
					show = e instanceof Enemy ? hostile.get() : passive.get();
				else
					show = false;

				if(!show)
					continue;

				Vec3 end = Util.lerpBox(e, pt).getCenter();
				r.line(start, end, entityColor(e));
			}
		}
	}

	// ---------------------------------------------------------------

	public static final class Fullbright extends Hack
	{
		public Fullbright()
		{
			super("Fullbright", "Alles ist hell, auch in Hoehlen und nachts.",
				Category.RENDER);
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(!p.hasEffect(MobEffects.NIGHT_VISION))
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
					MobEffectInstance.INFINITE_DURATION, 0, false, false));
		}

		@Override
		protected void onDisable()
		{
			if(MC.player != null)
				MC.player.removeEffect(MobEffects.NIGHT_VISION);
		}
	}

	// ---------------------------------------------------------------

	public static final class ChestEsp extends Hack
	{
		private final BoolSetting chests =
			add(new BoolSetting("Chests", "Kisten.", true));
		private final BoolSetting ender =
			add(new BoolSetting("Ender chests", "Endertruhen.", true));
		private final BoolSetting barrels =
			add(new BoolSetting("Barrels", "Faesser.", true));
		private final BoolSetting shulkers =
			add(new BoolSetting("Shulkers", "Shulkerkisten.", true));

		public ChestEsp()
		{
			super("ChestESP", "Zeigt Kisten, Faesser und Shulker durch Waende.",
				Category.RENDER);
		}

		@Override
		public void onRender3D(EspRenderer r)
		{
			int radius = Math.max(2, MC.options.getEffectiveRenderDistance()) + 3;
			net.minecraft.world.level.ChunkPos.rangeClosed(
				MC.player.chunkPosition(), radius).forEach(c -> {
					if(!MC.level.hasChunk(c.x(), c.z()))
						return;

					var chunk = MC.level.getChunk(c.x(), c.z());
					for(BlockEntity be : chunk.getBlockEntities().values())
					{
						int color = colorOf(be);
						if(color != 0)
							r.boxWithFill(new AABB(be.getBlockPos()), color,
								0x28);
					}
				});
		}

		private int colorOf(BlockEntity be)
		{
			if(be instanceof TrappedChestBlockEntity)
				return chests.get() ? 0xFFF87171 : 0;
			if(be instanceof ChestBlockEntity)
				return chests.get() ? 0xFFFACC15 : 0;
			if(be instanceof EnderChestBlockEntity)
				return ender.get() ? 0xFFA78BFA : 0;
			if(be instanceof BarrelBlockEntity)
				return barrels.get() ? 0xFFFB923C : 0;
			if(be instanceof ShulkerBoxBlockEntity)
				return shulkers.get() ? 0xFFF472B6 : 0;
			return 0;
		}
	}

	// ---------------------------------------------------------------

	/** Markiert Erze in der Naehe durch Waende (ohne die Welt zu veraendern). */
	public static final class OreEsp extends Hack
	{
		private record Ore(BlockPos pos, int color)
		{}

		private final NumSetting radius = add(new NumSetting("Radius",
			"Suchradius in Bloecken.", 16, 4, 32, 1));
		private final BoolSetting fill = add(
			new BoolSetting("Fill", "Boxen leicht ausfuellen.", true));
		private final List<Ore> found = new ArrayList<>();
		private int timer;

		public OreEsp()
		{
			super("OreESP", "Markiert Erze in der Naehe durch Waende.",
				Category.RENDER);
		}

		@Override
		protected void onEnable()
		{
			timer = 0;
		}

		@Override
		protected void onDisable()
		{
			found.clear();
		}

		@Override
		public void onTick()
		{
			if(timer-- > 0)
				return;

			timer = 20;
			scan();
		}

		private void scan()
		{
			found.clear();
			BlockPos center = MC.player.blockPosition();
			int r = radius.getI();
			BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

			for(int x = -r; x <= r; x++)
				for(int y = -r; y <= r; y++)
					for(int z = -r; z <= r; z++)
					{
						m.set(center.getX() + x, center.getY() + y,
							center.getZ() + z);
						BlockState state = MC.level.getBlockState(m);
						if(state.isAir())
							continue;

						int color = oreColor(state);
						if(color != 0)
							found.add(new Ore(m.immutable(), color));
					}
		}

		private static int oreColor(BlockState state)
		{
			String id = BuiltInRegistries.BLOCK.getKey(state.getBlock())
				.getPath();
			if(id.equals("ancient_debris"))
				return 0xFFB45309;
			if(!id.endsWith("_ore"))
				return 0;

			if(id.contains("diamond"))
				return 0xFF22D3EE;
			if(id.contains("emerald"))
				return 0xFF4ADE80;
			if(id.contains("gold"))
				return 0xFFFACC15;
			if(id.contains("iron"))
				return 0xFFD6B49A;
			if(id.contains("copper"))
				return 0xFFFB923C;
			if(id.contains("redstone"))
				return 0xFFEF4444;
			if(id.contains("lapis"))
				return 0xFF3B82F6;
			if(id.contains("quartz"))
				return 0xFFF5F5F5;
			if(id.contains("coal"))
				return 0xFF6B7280;
			return 0xFFFFFFFF;
		}

		@Override
		public void onRender3D(EspRenderer r)
		{
			for(Ore ore : found)
			{
				AABB box = new AABB(ore.pos());
				if(fill.get())
					r.boxWithFill(box, ore.color(), 0x30);
				else
					r.box(box, ore.color());
			}
		}

		@Override
		public String getSuffix()
		{
			return Integer.toString(found.size());
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Echter X-Ray: alle Bloecke ausser der Liste werden unsichtbar. Die
	 * eigentliche Arbeit machen die Mixins (siehe dev.nova.client.mixin),
	 * dieser Hack haelt nur die Blockliste und laedt die Chunks neu.
	 */
	public static final class XRay extends Hack
	{
		private static java.util.Map<String, Block> byId;

		private final ListSetting blocks = add(new ListSetting("Blocks",
			"Sichtbare Bloecke. Aendern mit .xray add|remove|list|reset"));
		private final BoolSetting brightness = add(new BoolSetting(
			"Brightness", "Nachtsicht, damit Hoehlen hell sind.", true));
		private boolean addedNightVision;
		private int checkTimer;

		public XRay()
		{
			super("XRay", "Macht alles ausser Erzen und Kisten unsichtbar.",
				Category.RENDER);
			resetBlocks();
		}

		// --- Blockliste ------------------------------------------------

		private static synchronized java.util.Map<String, Block> registry()
		{
			if(byId == null)
			{
				byId = new java.util.HashMap<>();
				for(Block b : BuiltInRegistries.BLOCK)
					byId.put(BuiltInRegistries.BLOCK.getKey(b).toString(), b);
			}

			return byId;
		}

		/** "diamond_ore" -> "minecraft:diamond_ore", oder null, wenn es den Block nicht gibt. */
		public static String normalize(String input)
		{
			String id = input.trim().toLowerCase(java.util.Locale.ROOT);
			if(id.indexOf(':') < 0)
				id = "minecraft:" + id;

			return registry().containsKey(id) ? id : null;
		}

		private static boolean visibleByDefault(String id)
		{
			String path = id.substring(id.indexOf(':') + 1);
			if(path.endsWith("_ore"))
				return true;

			return switch(path)
			{
				case "ancient_debris", "gilded_blackstone", "spawner",
					"trial_spawner", "vault", "chest", "trapped_chest",
					"ender_chest", "end_portal_frame" -> true;
				default -> false;
			};
		}

		public ListSetting getBlocks()
		{
			return blocks;
		}

		public void resetBlocks()
		{
			blocks.clear();
			for(String id : registry().keySet())
				if(visibleByDefault(id))
					blocks.add(id);
		}

		private java.util.Set<Block> visibleBlocks()
		{
			java.util.Set<Block> set = new java.util.HashSet<>();
			for(String id : blocks.all())
			{
				Block b = registry().get(id);
				if(b != null)
					set.add(b);
			}

			return set;
		}

		/** Nach Aenderungen an der Liste aufrufen. */
		public void refresh()
		{
			if(!isEnabled())
				return;

			XRayState.activate(visibleBlocks());
			reloadChunks();
		}

		private static void reloadChunks()
		{
			if(MC.level != null)
				MC.levelExtractor.allChanged();
		}

		// --- Lebenszyklus ----------------------------------------------

		@Override
		protected void onEnable()
		{
			XRayState.activate(visibleBlocks());
			checkTimer = 60;
			reloadChunks();
		}

		@Override
		protected void onDisable()
		{
			XRayState.deactivate();
			reloadChunks();

			if(addedNightVision && MC.player != null)
			{
				MC.player.removeEffect(MobEffects.NIGHT_VISION);
				addedNightVision = false;
			}
		}

		@Override
		public void onTick()
		{
			LocalPlayer p = MC.player;
			if(brightness.get() && !p.hasEffect(MobEffects.NIGHT_VISION))
			{
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
					MobEffectInstance.INFINITE_DURATION, 0, false, false));
				addedNightVision = true;
			}

			// Nach 3 Sekunden pruefen, ob die Mixins wirklich greifen.
			if(checkTimer > 0 && --checkTimer == 0 && !XRayState.shapeHookRan
				&& !XRayState.faceHookRan)
			{
				Util.chat("§cX-Ray: die Mixins greifen nicht (falsche "
					+ "Minecraft-Version?). Schau in logs/latest.log.");
				setEnabled(false);
			}
		}

		@Override
		public String getSuffix()
		{
			return blocks.display();
		}
	}

	// ---------------------------------------------------------------

	public static final class Zoom extends Hack
	{
		private final NumSetting level = add(new NumSetting("Level",
			"Zoom-Faktor.", 3, 1.5, 10, 0.5));

		public Zoom()
		{
			super("Zoom", "Zoomt hinein. Zum Beispiel auf Taste Z legen.",
				Category.RENDER);
			showInList = false;
		}

		@Override
		public float modifyFov(float fov)
		{
			return fov / level.getF();
		}
	}

	// ---------------------------------------------------------------

	public static final class Radar extends Hack
	{
		private final NumSetting range = add(new NumSetting("Range",
			"Reichweite in Bloecken.", 32, 8, 128, 4));
		private final NumSetting size = add(new NumSetting("Size",
			"Groesse in Pixeln.", 90, 60, 160, 10));

		public Radar()
		{
			super("Radar", "Kleine Karte mit Spielern und Mobs in der Naehe.",
				Category.RENDER);
			showInList = false;
		}

		@Override
		public void onRender2D(GuiGraphicsExtractor g, int width, int height)
		{
			int s = size.getI();
			int x0 = 8;
			int y0 = height - s - 8;
			int cx = x0 + s / 2;
			int cy = y0 + s / 2;

			g.fill(x0 - 1, y0 - 1, x0 + s + 1, y0 + s + 1,
				Theme.withAlpha(Theme.accent(0), 0xB0));
			g.fill(x0, y0, x0 + s, y0 + s, 0xD00B0B12);
			g.fill(cx, y0, cx + 1, y0 + s, 0x30FFFFFF);
			g.fill(x0, cy, x0 + s, cy + 1, 0x30FFFFFF);

			LocalPlayer p = MC.player;
			double yaw = Math.toRadians(p.getYRot());
			double sin = Math.sin(yaw);
			double cos = Math.cos(yaw);
			double scale = (s / 2.0) / range.get();

			for(Entity e : MC.level.entitiesForRendering())
			{
				if(e == p || !(e instanceof LivingEntity) || e.isRemoved())
					continue;

				if(!(e instanceof Player) && !(e instanceof Mob))
					continue;

				double dx = e.getX() - p.getX();
				double dz = e.getZ() - p.getZ();
				// In Blickrichtung drehen (oben = vorne)
				double rx = dx * cos + dz * sin;
				double rz = -dx * sin + dz * cos;
				int px = cx + (int)Math.round(rx * scale);
				int py = cy + (int)Math.round(rz * scale);
				if(px < x0 + 1 || px > x0 + s - 2 || py < y0 + 1
					|| py > y0 + s - 2)
					continue;

				int c = entityColor(e);
				g.fill(px - 1, py - 1, px + 2, py + 2, c);
			}

			// Du selbst
			g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
		}
	}
}
