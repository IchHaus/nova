package dev.nova.client.core;

import java.util.Locale;
import java.util.StringJoiner;

import org.lwjgl.glfw.GLFW;

import dev.nova.client.NovaClient;
import dev.nova.client.hacks.AutoLibrarian;
import dev.nova.client.hacks.RenderHacks;
import dev.nova.client.util.Util;

/** Chat-Befehle mit Punkt davor, z.B. ".t killaura". */
public final class Commands
{
	private Commands()
	{}

	public static void handle(String line)
	{
		String[] a = line.trim().split("\\s+");
		if(a.length == 0 || a[0].isEmpty())
		{
			help();
			return;
		}

		HackManager hm = NovaClient.hacks;
		String cmd = a[0].toLowerCase(Locale.ROOT);

		switch(cmd)
		{
			case "help", "h", "?" -> help();

			case "t", "toggle" ->
			{
				Hack h = need(hm, a, 1);
				if(h != null)
				{
					h.toggle();
					Util.chat(h.getName() + ": "
						+ (h.isEnabled() ? "§aAN" : "§cAUS"));
				}
			}

			case "list", "hacks" ->
			{
				for(Category c : Category.values())
				{
					StringJoiner sj = new StringJoiner("§7, ");
					for(Hack h : hm.inCategory(c))
						sj.add((h.isEnabled() ? "§a" : "§f")
							+ h.getName());

					Util.chat("§d" + c.displayName + "§7: " + sj);
				}
			}

			case "bind" ->
			{
				Hack h = need(hm, a, 1);
				if(h == null)
					return;

				if(a.length < 3)
				{
					Util.chat("Benutzung: .bind <hack> <taste|none>");
					return;
				}

				int key = parseKey(a[2]);
				if(key == -2)
				{
					Util.chat("§cUnbekannte Taste: " + a[2]);
					return;
				}

				h.setKey(key);
				hm.save();
				Util.chat(h.getName() + " liegt jetzt auf §d"
					+ Hack.keyName(key));
			}

			case "set" ->
			{
				Hack h = need(hm, a, 1);
				if(h == null)
					return;

				if(a.length < 4)
				{
					StringJoiner sj = new StringJoiner(", ");
					for(Setting s : h.getSettings())
						sj.add(s.name.replace(" ", ""));

					Util.chat("Benutzung: .set <hack> <einstellung> <wert>");
					Util.chat("Einstellungen von " + h.getName() + ": " + sj);
					return;
				}

				setSetting(h, a[2], a[3]);
				hm.save();
			}

			case "friend", "friends", "f" -> friends(a);

			// Absichtlich ohne Chatmeldung: danach soll nichts mehr zu sehen sein.
			case "panic" -> Panic.trigger();

			case "nova" ->
			{
				String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
				switch(sub)
				{
					case "off" -> Panic.trigger();
					case "on" -> Util.chat("Nova Client ist bereits aktiv.");
					default -> Util.chat("Benutzung: .nova off  (zum "
						+ "Wiedereinschalten: .nova on oder Panic-Taste)");
				}
			}

			case "panickey" ->
			{
				if(a.length < 2)
				{
					Util.chat("Panic-Taste: §d" + Hack.keyName(Panic.getKey())
						+ "§f. Aendern mit .panickey <taste>");
					return;
				}

				int key = parseKey(a[1]);
				if(key < 0)
				{
					Util.chat("§cUngueltige Taste: " + a[1]);
					return;
				}

				Panic.setKey(key);
				hm.save();
				Util.chat("Panic-Taste ist jetzt §d" + Hack.keyName(key));
			}

			case "xray" -> xray(hm, a);

			case "librarian", "lib" -> librarian(hm, a);

			default -> Util.chat("§cUnbekannter Befehl. §7.help");
		}
	}

	private static void help()
	{
		Util.chat("§dBefehle:");
		Util.chat("§7.t <hack> §f- Hack an/aus");
		Util.chat("§7.list §f- alle Hacks anzeigen");
		Util.chat("§7.bind <hack> <taste|none> §f- Taste festlegen");
		Util.chat("§7.set <hack> <einstellung> <wert> §f- Einstellung");
		Util.chat("§7.friend add|remove|list [name] §f- Freunde");
		Util.chat("§7.xray add|remove|list|reset [block] §f- X-Ray-Blockliste");
		Util.chat("§7.librarian add|remove|list|clear|reset §f- Wunschbuecher");
		Util.chat("§7.panic §f- alles ausschalten, Client stumm");
		Util.chat("§7.panickey [taste] §f- Panic-Taste (Standard: Ende)");
		Util.chat("§7Rechtsshift §f- ClickGUI");
	}

	private static void librarian(HackManager hm, String[] a)
	{
		if(!(hm.get("AutoLibrarian") instanceof AutoLibrarian lib))
		{
			Util.chat("§cAutoLibrarian ist nicht verfuegbar.");
			return;
		}

		String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
		switch(sub)
		{
			case "list" ->
			{
				if(lib.getWanted().all().isEmpty())
				{
					Util.chat("Die Wunschliste ist leer.");
					return;
				}

				Util.chat("Wunschbuecher:");
				for(String entry : lib.getWanted().all())
					Util.chat("§7- §f" + AutoLibrarian.describe(entry));
			}

			case "add" ->
			{
				if(a.length < 3)
				{
					Util.chat("Benutzung: .librarian add <buch> [stufe] [maxpreis]");
					Util.chat("Ohne Stufe zaehlt die hoechste Stufe, z. B. .librarian add mending");
					return;
				}

				String id = AutoLibrarian.normalize(a[2]);
				var holder = AutoLibrarian.lookup(id);
				if(holder.isEmpty())
				{
					Util.chat("§cKeine Verzauberung namens \"" + a[2] + "\".");
					return;
				}

				if(!AutoLibrarian.isTradeable(holder.get()))
				{
					Util.chat("§cDiese Verzauberung verkaufen Villager nicht als Buch.");
					return;
				}

				int max = holder.get().value().getMaxLevel();
				try
				{
					int level = a.length > 3 ? Integer.parseInt(a[3]) : max;
					int price = a.length > 4 ? Integer.parseInt(a[4]) : 64;
					if(level < 1 || level > max)
					{
						Util.chat("§cStufe muss zwischen 1 und " + max + " liegen.");
						return;
					}

					if(price < 1 || price > 64)
					{
						Util.chat("§cDer Preis muss zwischen 1 und 64 liegen.");
						return;
					}

					lib.addWanted(id, level, price);
					hm.save();
					Util.chat("§aHinzugefuegt: §f"
						+ AutoLibrarian.describe(id + ";" + level + ";" + price));

				}catch(NumberFormatException e)
				{
					Util.chat("§cStufe und Preis muessen Zahlen sein.");
				}
			}

			case "remove", "rm" ->
			{
				if(a.length < 3)
				{
					Util.chat("Benutzung: .librarian remove <buch>");
					return;
				}

				int count = lib.removeWanted(AutoLibrarian.normalize(a[2]));
				hm.save();
				Util.chat(count > 0 ? "§a" + count + " Eintrag/Eintraege entfernt."
					: a[2] + " stand nicht in der Liste.");
			}

			case "clear" ->
			{
				lib.getWanted().clear();
				hm.save();
				Util.chat("Wunschliste geleert.");
			}

			case "reset" ->
			{
				lib.resetWanted();
				hm.save();
				Util.chat("Wunschliste zurueckgesetzt.");
			}

			default -> Util.chat("Benutzung: .librarian add|remove|list|clear|reset");
		}
	}

	private static void xray(HackManager hm, String[] a)
	{
		if(!(hm.get("XRay") instanceof RenderHacks.XRay x))
		{
			Util.chat("§cX-Ray ist nicht verfuegbar.");
			return;
		}

		String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
		switch(sub)
		{
			case "list" ->
			{
				Util.chat("X-Ray zeigt §d" + x.getBlocks().all().size()
					+ "§f Bloecke:");
				StringJoiner sj = new StringJoiner("§7, §f");
				for(String id : x.getBlocks().all())
					sj.add(id.startsWith("minecraft:") ? id.substring(10) : id);

				Util.chat("§f" + sj);
			}

			case "add", "remove", "rm" ->
			{
				if(a.length < 3)
				{
					Util.chat("Benutzung: .xray " + sub + " <block>");
					return;
				}

				String id = RenderHacks.XRay.normalize(a[2]);
				if(id == null)
				{
					Util.chat("§cUnbekannter Block: " + a[2]);
					return;
				}

				boolean changed = sub.equals("add") ? x.getBlocks().add(id)
					: x.getBlocks().remove(id);
				if(changed)
				{
					x.refresh();
					hm.save();
				}

				Util.chat(changed ? "§a" + sub + ": §f" + id
					: id + (sub.equals("add") ? " war schon in der Liste."
						: " war nicht in der Liste."));
			}

			case "reset" ->
			{
				x.resetBlocks();
				x.refresh();
				hm.save();
				Util.chat("X-Ray-Liste zurueckgesetzt (Erze, Kisten, Spawner).");
			}

			default -> Util.chat("Benutzung: .xray add|remove|list|reset [block]");
		}
	}

	private static Hack need(HackManager hm, String[] a, int index)
	{
		if(a.length <= index)
		{
			Util.chat("§cBitte gib einen Hack an.");
			return null;
		}

		Hack h = hm.get(a[index]);
		if(h == null)
			Util.chat("§cKein Hack namens \"" + a[index] + "\".");

		return h;
	}

	private static void setSetting(Hack h, String name, String value)
	{
		for(Setting s : h.getSettings())
		{
			if(!s.name.replace(" ", "").equalsIgnoreCase(name))
				continue;

			try
			{
				if(s instanceof ListSetting)
				{
					Util.chat("§cDiese Liste aenderst du mit .xray add|remove.");
					return;
				}

				if(s instanceof BoolSetting b)
					b.set(value.equalsIgnoreCase("true")
						|| value.equalsIgnoreCase("on")
						|| value.equalsIgnoreCase("an") || value.equals("1"));
				else if(s instanceof NumSetting n)
					n.set(Double.parseDouble(value.replace(',', '.')));
				else if(s instanceof ModeSetting m)
				{
					boolean found = false;
					for(int i = 0; i < 20 && !found; i++)
					{
						if(m.get().replace(" ", "").equalsIgnoreCase(value))
							found = true;
						else
							m.cycle();
					}

					if(!found)
					{
						Util.chat("§cUnbekannter Modus: " + value);
						return;
					}
				}

				Util.chat(h.getName() + " / " + s.name + " = §d"
					+ s.save().getAsString());

			}catch(NumberFormatException e)
			{
				Util.chat("§c\"" + value + "\" ist keine Zahl.");
			}

			return;
		}

		Util.chat("§cKeine Einstellung namens \"" + name + "\".");
	}

	private static void friends(String[] a)
	{
		if(a.length < 2 || a[1].equalsIgnoreCase("list"))
		{
			Util.chat("Freunde: §a"
				+ (Friends.all().isEmpty() ? "-" : String.join(", ", Friends.all())));
			return;
		}

		if(a.length < 3)
		{
			Util.chat("Benutzung: .friend add|remove|list [name]");
			return;
		}

		switch(a[1].toLowerCase(Locale.ROOT))
		{
			case "add" ->
			{
				Friends.add(a[2]);
				NovaClient.hacks.save();
				Util.chat(a[2] + " ist jetzt dein Freund.");
			}
			case "remove", "rm" ->
			{
				boolean ok = Friends.remove(a[2]);
				NovaClient.hacks.save();
				Util.chat(ok ? a[2] + " entfernt." : a[2] + " war kein Freund.");
			}
			default -> Util.chat("Benutzung: .friend add|remove|list [name]");
		}
	}

	/** -1 = keine Taste, -2 = unbekannt. */
	private static int parseKey(String s)
	{
		s = s.toUpperCase(Locale.ROOT);
		if(s.equals("NONE") || s.equals("NULL") || s.equals("-"))
			return -1;

		if(s.length() == 1)
		{
			char c = s.charAt(0);
			if(c >= 'A' && c <= 'Z' || c >= '0' && c <= '9')
				return c;
		}

		if(s.matches("F([1-9]|1[0-2])"))
			return GLFW.GLFW_KEY_F1 + Integer.parseInt(s.substring(1)) - 1;

		return switch(s)
		{
			case "SPACE" -> GLFW.GLFW_KEY_SPACE;
			case "LSHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
			case "LCTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
			case "RCTRL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
			case "LALT" -> GLFW.GLFW_KEY_LEFT_ALT;
			case "RALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
			case "TAB" -> GLFW.GLFW_KEY_TAB;
			case "UP" -> GLFW.GLFW_KEY_UP;
			case "DOWN" -> GLFW.GLFW_KEY_DOWN;
			case "LEFT" -> GLFW.GLFW_KEY_LEFT;
			case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
			default -> -2;
		};
	}
}
