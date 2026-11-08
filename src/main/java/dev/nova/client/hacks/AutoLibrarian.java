package dev.nova.client.hacks;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import dev.nova.client.core.BoolSetting;
import dev.nova.client.core.Category;
import dev.nova.client.core.Hack;
import dev.nova.client.core.ListSetting;
import dev.nova.client.core.NumSetting;
import dev.nova.client.util.Util;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Trainiert einen Bibliothekar auf ein Wunschbuch: Angebot ansehen, bei einem
 * falschen Buch den Lectern abbauen und neu setzen, bis der Villager eines der
 * gewuenschten Buecher verkauft. Funktioniert wie AutoLibrarian bei Wurst.
 */
public final class AutoLibrarian extends Hack
{
	private enum State
	{
		/** Villager und Lectern suchen. */
		FIND,
		/** Angebote ansehen. */
		TRADE,
		/** Lectern abbauen. */
		BREAK,
		/** Warten, bis der Villager seinen Beruf verloren hat. */
		WAIT_RESET,
		/** Lectern neu setzen. */
		PLACE
	}

	private record Offer(String id, String name, int level, int maxLevel,
		int price)
	{}

	private final ListSetting wanted = add(new ListSetting("Wanted books",
		"Gewuenschte Buecher. Aendern mit .librarian add|remove|list|clear|reset"));
	private final BoolSetting lockIn = add(new BoolSetting("Lock in trade",
		"Kauft etwas beim Villager, sobald das Buch passt, damit sich die"
			+ " Angebote nicht mehr aendern. Braucht 24 Papier und 9 Smaragde"
			+ " (oder 1 Buch und 64 Smaragde).",
		false));
	private final NumSetting range = add(new NumSetting("Range",
		"Reichweite zu Villager und Lectern.", 5, 1, 6, 0.5));

	private final Set<Villager> experienced = new HashSet<>();
	private State state = State.FIND;
	private Villager villager;
	private BlockPos jobSite;
	private int cooldown;
	private int waitTicks;
	private int attempts;
	private boolean sneaking;

	public AutoLibrarian()
	{
		super("AutoLibrarian",
			"Trainiert Bibliothekare, bis sie dein Wunschbuch verkaufen.",
			Category.WORLD);
		resetWanted();
	}

	// ---------------------------------------------------------------
	// Wunschliste (Eintraege: "minecraft:mending;1;64" = id;level;maxPreis)
	// ---------------------------------------------------------------

	public ListSetting getWanted()
	{
		return wanted;
	}

	public void resetWanted()
	{
		wanted.clear();
		for(String s : new String[]{"depth_strider;3", "efficiency;5",
			"feather_falling;4", "fortune;3", "looting;3", "mending;1",
			"protection;4", "respiration;3", "sharpness;5", "silk_touch;1",
			"unbreaking;3"})
			wanted.add("minecraft:" + s + ";64");
	}

	/** "mending" -> "minecraft:mending". */
	public static String normalize(String input)
	{
		String id = input.trim().toLowerCase(java.util.Locale.ROOT);
		return id.indexOf(':') < 0 ? "minecraft:" + id : id;
	}

	/** Sucht die Verzauberung in der Registry. Nur in einer Welt moeglich. */
	public static Optional<? extends Holder<Enchantment>> lookup(String id)
	{
		if(MC.level == null)
			return Optional.empty();

		Identifier identifier = Identifier.tryParse(id);
		if(identifier == null)
			return Optional.empty();

		Registry<Enchantment> registry =
			MC.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		return registry.get(identifier);
	}

	/** True, wenn es die Verzauberung gibt und Villager sie als Buch verkaufen. */
	public static boolean isTradeable(Holder<Enchantment> enchantment)
	{
		return enchantment.is(EnchantmentTags.TRADEABLE);
	}

	/** Fuegt einen Wunsch hinzu und ersetzt einen alten mit gleicher ID und Stufe. */
	public void addWanted(String id, int level, int maxPrice)
	{
		String prefix = id + ";" + level + ";";
		for(String s : new java.util.ArrayList<>(wanted.all()))
			if(s.startsWith(prefix))
				wanted.remove(s);

		wanted.add(prefix + maxPrice);
	}

	/** Entfernt alle Wuensche fuer diese Verzauberung. Gibt die Anzahl zurueck. */
	public int removeWanted(String id)
	{
		int count = 0;
		for(String s : new java.util.ArrayList<>(wanted.all()))
			if(s.startsWith(id + ";") && wanted.remove(s))
				count++;

		return count;
	}

	/** Lesbare Form eines Eintrags fuer den Chat. */
	public static String describe(String entry)
	{
		String[] p = entry.split(";");
		String id = p[0].startsWith("minecraft:") ? p[0].substring(10) : p[0];
		String level = p.length > 1 && !p[1].equals("0") ? " " + p[1] : "";
		String price = p.length > 2 ? " (max " + p[2] + " Smaragde)" : "";
		return id + level + price;
	}

	private boolean isWanted(Offer offer)
	{
		for(String entry : wanted.all())
		{
			String[] p = entry.split(";");
			if(!p[0].equals(offer.id()))
				continue;

			try
			{
				int level = p.length > 1 ? Integer.parseInt(p[1]) : 0;
				int maxPrice = p.length > 2 ? Integer.parseInt(p[2]) : 64;
				int needed = level <= 0 ? offer.maxLevel() : level;
				if(offer.level() >= needed && offer.price() <= maxPrice)
					return true;

			}catch(NumberFormatException e)
			{
				// kaputter Eintrag, ignorieren
			}
		}

		return false;
	}

	// ---------------------------------------------------------------
	// Lebenszyklus
	// ---------------------------------------------------------------

	@Override
	protected void onEnable()
	{
		state = State.FIND;
		villager = null;
		jobSite = null;
		cooldown = 0;
		waitTicks = 0;
		attempts = 0;
		experienced.clear();

		if(wanted.all().isEmpty())
		{
			Util.chat("§cDie Wunschliste ist leer. Nutze .librarian add <buch>.");
			setEnabled(false);
		}
	}

	@Override
	protected void onDisable()
	{
		if(MC.gameMode != null && MC.gameMode.isDestroying())
			MC.gameMode.stopDestroyBlock();

		releaseSneak();
		villager = null;
		jobSite = null;
		experienced.clear();
	}

	@Override
	public String getSuffix()
	{
		return attempts > 0 ? attempts + "x" : null;
	}

	@Override
	public void onTick()
	{
		if(cooldown > 0)
			cooldown--;

		if(state != State.FIND && (villager == null || villager.isRemoved()))
		{
			experienced.remove(villager);
			villager = null;
			jobSite = null;
			state = State.FIND;
		}

		switch(state)
		{
			case FIND -> find();
			case TRADE -> trade();
			case BREAK -> breakLectern();
			case WAIT_RESET -> waitForReset();
			case PLACE -> placeLectern();
		}
	}

	// ---------------------------------------------------------------
	// FIND
	// ---------------------------------------------------------------

	private void find()
	{
		if(villager == null)
		{
			villager = nearestLibrarian();
			if(villager == null)
			{
				String msg = "§cKein Bibliothekar in Reichweite.";
				if(!experienced.isEmpty())
					msg += " (" + experienced.size()
						+ " haben schon Erfahrung und lassen sich nicht mehr trainieren.)";

				Util.chat(msg);
				Util.chat("Stell dich so hin, dass Villager und Lectern in Reichweite sind.");
				setEnabled(false);
				return;
			}
		}

		jobSite = nearestLectern();
		if(jobSite == null)
		{
			Util.chat("§cKein Lectern beim Bibliothekar gefunden.");
			Util.chat("Stell dich so hin, dass Villager und Lectern in Reichweite sind.");
			setEnabled(false);
			return;
		}

		waitTicks = 0;
		state = State.TRADE;
	}

	private Villager nearestLibrarian()
	{
		double rangeSq = range.get() * range.get();
		Villager best = null;
		double bestDist = Double.MAX_VALUE;

		for(Entity e : MC.level.entitiesForRendering())
		{
			if(!(e instanceof Villager v) || !v.isAlive()
				|| experienced.contains(v))
				continue;

			if(!isLibrarian(v) || v.getVillagerData().level() != 1)
				continue;

			double dist = distanceToHitboxSq(v);
			if(dist <= rangeSq && dist < bestDist)
			{
				best = v;
				bestDist = dist;
			}
		}

		return best;
	}

	private BlockPos nearestLectern()
	{
		Vec3 eyes = MC.player.getEyePosition();
		double rangeSq = range.get() * range.get();
		int r = (int)Math.ceil(range.get());
		BlockPos origin = BlockPos.containing(eyes);
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;

		for(int x = -r; x <= r; x++)
			for(int y = -r; y <= r; y++)
				for(int z = -r; z <= r; z++)
				{
					m.set(origin.getX() + x, origin.getY() + y,
						origin.getZ() + z);
					if(!MC.level.getBlockState(m).is(Blocks.LECTERN))
						continue;

					Vec3 center = Vec3.atCenterOf(m);
					if(eyes.distanceToSqr(center) > rangeSq)
						continue;

					double dist = villager.distanceToSqr(center);
					if(dist < bestDist)
					{
						best = m.immutable();
						bestDist = dist;
					}
				}

		return best;
	}

	private static boolean isLibrarian(Villager v)
	{
		return v.getVillagerData().profession().unwrapKey()
			.orElse(null) == VillagerProfession.LIBRARIAN;
	}

	private static double distanceToHitboxSq(Entity e)
	{
		Vec3 start = MC.player.getEyePosition();
		AABB box = e.getBoundingBox();
		double x = Math.clamp(start.x, box.minX, box.maxX);
		double y = Math.clamp(start.y, box.minY, box.maxY);
		double z = Math.clamp(start.z, box.minZ, box.maxZ);
		return start.distanceToSqr(new Vec3(x, y, z));
	}

	// ---------------------------------------------------------------
	// TRADE
	// ---------------------------------------------------------------

	private void trade()
	{
		if(!(MC.gui.screen() instanceof MerchantScreen screen))
		{
			openTradeScreen();
			return;
		}

		waitTicks = 0;
		MerchantMenu menu = screen.getMenu();

		// Erfahrung sieht man erst bei offenem Handelsfenster.
		if(menu.getTraderXp() > 0)
		{
			Util.chat("§eDieser Villager hat schon Erfahrung und laesst sich nicht"
				+ " mehr trainieren. Suche einen anderen...");
			experienced.add(villager);
			villager = null;
			jobSite = null;
			closeTradeScreen();
			state = State.FIND;
			return;
		}

		MerchantOffers offers = menu.getOffers();
		if(offers.isEmpty())
		{
			// Die Angebote kommen manchmal einen Tick nach dem Fenster.
			if(++waitTicks < 20)
				return;
		}

		waitTicks = 0;
		attempts++;
		Offer offer = findBookOffer(offers);
		if(offer == null)
		{
			Util.chat("Der Villager verkauft kein verzaubertes Buch.");
			closeTradeScreen();
			state = State.BREAK;
			return;
		}

		Util.chat("Villager verkauft §d" + offer.name()
			+ (offer.maxLevel() > 1 ? " " + offer.level() : "") + "§f fuer "
			+ offer.price() + " Smaragd" + (offer.price() == 1 ? "" : "e") + ".");

		if(!isWanted(offer))
		{
			closeTradeScreen();
			state = State.BREAK;
			return;
		}

		if(lockIn.get())
		{
			// Das erste Angebot kaufen, damit der Villager seine Trades behaelt.
			menu.setSelectionHint(0);
			menu.tryMoveItems(0);
			MC.getConnection().send(new ServerboundSelectTradePacket(0));
			MC.gameMode.handleContainerInput(menu.containerId, 2, 0,
				ContainerInput.PICKUP, MC.player);
		}

		closeTradeScreen();
		Util.chat("§aFertig!§f Nach " + attempts + " Versuch"
			+ (attempts == 1 ? "" : "en") + " gefunden.");
		setEnabled(false);
	}

	private void openTradeScreen()
	{
		if(cooldown > 0)
			return;

		if(distanceToHitboxSq(villager) > range.get() * range.get())
		{
			Util.chat("§cDer Villager ist ausser Reichweite. Sperr ihn am besten"
				+ " ein, damit er nicht weglaeuft.");
			setEnabled(false);
			return;
		}

		// Nach dem Platzieren dauert es, bis der Villager den Beruf annimmt.
		if(++waitTicks == 600)
			Util.chat("§eDer Villager nimmt den Lectern nicht an. Kommt er an"
				+ " den Lectern heran?");

		EntityHitResult hit = createHitResult(villager);
		MC.gameMode.interact(MC.player, villager, hit,
			InteractionHand.MAIN_HAND);
		MC.player.swing(InteractionHand.MAIN_HAND);
		cooldown = 4;
	}

	private static EntityHitResult createHitResult(Entity e)
	{
		AABB box = e.getBoundingBox();
		Vec3 start = MC.player.getEyePosition();
		Vec3 hitVec = box.clip(start, box.getCenter()).orElse(start);
		return new EntityHitResult(e, hitVec);
	}

	private void closeTradeScreen()
	{
		MC.player.closeContainer();
		cooldown = 4;
	}

	private Offer findBookOffer(MerchantOffers offers)
	{
		for(MerchantOffer offer : offers)
		{
			ItemStack stack = offer.getResult();
			if(stack.getItem() != Items.ENCHANTED_BOOK)
				continue;

			var entries =
				EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet();
			if(entries.isEmpty())
				continue;

			var first = entries.iterator().next();
			Holder<Enchantment> holder = first.getKey();
			String id = holder.getRegisteredName();
			String name = holder.value().description().getString();
			int maxLevel = holder.value().getMaxLevel();
			return new Offer(id, name, first.getIntValue(), maxLevel,
				offer.getCostA().getCount());
		}

		return null;
	}

	// ---------------------------------------------------------------
	// BREAK
	// ---------------------------------------------------------------

	private void breakLectern()
	{
		BlockState block = MC.level.getBlockState(jobSite);
		if(block.canBeReplaced())
		{
			waitTicks = 0;
			state = State.WAIT_RESET;
			return;
		}

		Vec3 eyes = MC.player.getEyePosition();
		Vec3 center = Vec3.atCenterOf(jobSite);
		if(eyes.distanceToSqr(center) > range.get() * range.get())
		{
			Util.chat("§cDer Lectern ist ausser Reichweite.");
			setEnabled(false);
			return;
		}

		equipBestTool(block);
		Direction side = sideFacing(eyes.x - center.x, eyes.y - center.y,
			eyes.z - center.z);
		if(MC.gameMode.continueDestroyBlock(jobSite, side))
			MC.player.swing(InteractionHand.MAIN_HAND);
	}

	/** Die Seite eines Blocks, die in Richtung (dx, dy, dz) zeigt. */
	private static Direction sideFacing(double dx, double dy, double dz)
	{
		double ax = Math.abs(dx);
		double ay = Math.abs(dy);
		double az = Math.abs(dz);
		if(ay >= ax && ay >= az)
			return dy >= 0 ? Direction.UP : Direction.DOWN;
		
		if(ax >= az)
			return dx >= 0 ? Direction.EAST : Direction.WEST;
		
		return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
	}
	
	private void equipBestTool(BlockState block)
	{
		if(MC.player.isCreative())
			return;

		Inventory inv = MC.player.getInventory();
		int best = inv.getSelectedSlot();
		float bestSpeed = inv.getItem(best).getDestroySpeed(block);
		for(int i = 0; i < 9; i++)
		{
			float speed = inv.getItem(i).getDestroySpeed(block);
			if(speed > bestSpeed)
			{
				bestSpeed = speed;
				best = i;
			}
		}

		if(best != inv.getSelectedSlot())
			inv.setSelectedSlot(best);
	}

	// ---------------------------------------------------------------
	// WAIT_RESET
	// ---------------------------------------------------------------

	/**
	 * Wenn man den Lectern sofort neu setzt, behaelt der Villager seinen Beruf
	 * und damit seine Angebote. Deshalb warten wir, bis er ihn verloren hat.
	 */
	private void waitForReset()
	{
		if(isLibrarian(villager) && ++waitTicks < 100)
			return;

		waitTicks = 0;
		state = State.PLACE;
	}

	// ---------------------------------------------------------------
	// PLACE
	// ---------------------------------------------------------------

	private void placeLectern()
	{
		BlockState block = MC.level.getBlockState(jobSite);
		if(!block.canBeReplaced())
		{
			releaseSneak();
			waitTicks = 0;
			state = block.is(Blocks.LECTERN) ? State.TRADE : State.BREAK;
			return;
		}

		if(!MC.player.isHolding(Items.LECTERN))
		{
			// Der abgebaute Lectern wird eingesammelt, wenn man direkt daneben steht.
			if(!selectLectern() && ++waitTicks > 40)
			{
				Util.chat("§cKein Lectern im Inventar. Stell dich direkt neben"
					+ " den Lectern, damit du ihn nach dem Abbauen aufsammelst.");
				setEnabled(false);
			}

			return;
		}

		// Geduckt setzen, damit man keine Kisten, Werkbaenke usw. oeffnet.
		MC.options.keyShift.setDown(true);
		sneaking = true;
		if(!MC.player.isShiftKeyDown())
			return;

		BlockHitResult target = findPlacement();
		if(target == null)
		{
			Util.chat("§cKein Block neben dem Lectern-Platz, an dem man ihn"
				+ " setzen kann.");
			setEnabled(false);
			return;
		}

		MC.gameMode.useItemOn(MC.player, InteractionHand.MAIN_HAND, target);
		MC.player.swing(InteractionHand.MAIN_HAND);
		releaseSneak();
	}

	private boolean selectLectern()
	{
		Inventory inv = MC.player.getInventory();
		for(int i = 0; i < 9; i++)
			if(inv.getItem(i).is(Items.LECTERN))
			{
				inv.setSelectedSlot(i);
				return true;
			}

		for(int i = 9; i < 36; i++)
			if(inv.getItem(i).is(Items.LECTERN))
			{
				MC.gameMode.handleContainerInput(0, i, inv.getSelectedSlot(),
					ContainerInput.SWAP, MC.player);
				return true;
			}

		return false;
	}

	/** Sucht einen festen Nachbarblock, an dem der Lectern gesetzt werden kann. */
	private BlockHitResult findPlacement()
	{
		Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH,
			Direction.WEST, Direction.EAST, Direction.UP};

		for(Direction d : order)
		{
			BlockPos neighbor = jobSite.relative(d);
			BlockState ns = MC.level.getBlockState(neighbor);
			if(ns.isAir() || ns.canBeReplaced()
				|| !ns.getFluidState().isEmpty())
				continue;

			Direction face = d.getOpposite();
			Vec3 hit = Vec3.atCenterOf(neighbor).add(face.getStepX() * 0.5,
				face.getStepY() * 0.5, face.getStepZ() * 0.5);
			return new BlockHitResult(hit, face, neighbor, false);
		}

		return null;
	}

	private void releaseSneak()
	{
		if(sneaking)
		{
			MC.options.keyShift.setDown(false);
			sneaking = false;
		}
	}
}
