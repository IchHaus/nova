package dev.nova.client.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.nova.client.NovaClient;
import dev.nova.client.hacks.AutoLibrarian;
import dev.nova.client.hacks.CombatHacks;
import dev.nova.client.hacks.MiscHacks;
import dev.nova.client.hacks.MovementHacks;
import dev.nova.client.hacks.PlayerHacks;
import dev.nova.client.hacks.RenderHacks;
import dev.nova.client.hacks.WorldHacks;
import net.neoforged.fml.loading.FMLPaths;

/** Haelt alle Hacks, speichert und laedt Einstellungen und Tastenbelegungen. */
public final class HackManager
{
	private final Map<String, Hack> byName = new LinkedHashMap<>();
	private final List<Hack> hacks = new ArrayList<>();
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final Path configFile =
		FMLPaths.CONFIGDIR.get().resolve("novaclient.json");

	public HackManager()
	{
		// Combat
		register(new CombatHacks.Killaura(), GLFW.GLFW_KEY_R);
		register(new CombatHacks.TriggerBot());
		register(new CombatHacks.AimAssist());
		register(new CombatHacks.AutoTotem());
		register(new CombatHacks.AutoEat());
		register(new CombatHacks.AutoLeave());

		// Movement
		register(new MovementHacks.Flight(), GLFW.GLFW_KEY_G);
		register(new MovementHacks.Jetpack());
		register(new MovementHacks.Glide());
		register(new MovementHacks.NoFall());
		register(new MovementHacks.AutoSprint());
		register(new MovementHacks.Spider());
		register(new MovementHacks.Dolphin());
		register(new MovementHacks.HighJump());
		register(new MovementHacks.AirJump());
		register(new MovementHacks.FastLadder());
		register(new MovementHacks.Parkour());
		register(new MovementHacks.BunnyHop());
		register(new MovementHacks.AutoWalk());
		register(new MovementHacks.Sneak());
		register(new MovementHacks.SpeedHack());
		register(new MovementHacks.Step());

		// Render
		register(new RenderHacks.Esp(), GLFW.GLFW_KEY_H);
		register(new RenderHacks.Tracers());
		register(new RenderHacks.Fullbright(), GLFW.GLFW_KEY_C);
		register(new RenderHacks.ChestEsp());
		register(new RenderHacks.XRay(), GLFW.GLFW_KEY_V);
		register(new RenderHacks.OreEsp(), GLFW.GLFW_KEY_X);
		register(new RenderHacks.Zoom(), GLFW.GLFW_KEY_Z);
		register(new RenderHacks.Radar());

		// Player
		register(new PlayerHacks.AutoRespawn());
		register(new PlayerHacks.AutoArmor());
		register(new PlayerHacks.FastPlace(), GLFW.GLFW_KEY_B);
		register(new PlayerHacks.FastBreak());
		register(new PlayerHacks.AutoTool());
		register(new PlayerHacks.AntiAfk());
		register(new PlayerHacks.ChestStealer());

		// World
		register(new WorldHacks.Nuker());
		register(new WorldHacks.Scaffold());
		register(new WorldHacks.AutoMine());
		register(new AutoLibrarian());

		// Misc
		register(new MiscHacks.FancyChat());
	}

	private void register(Hack hack)
	{
		byName.put(hack.getName().toLowerCase(), hack);
		hacks.add(hack);
	}

	private void register(Hack hack, int defaultKey)
	{
		hack.setKey(defaultKey);
		register(hack);
	}

	public List<Hack> all()
	{
		return Collections.unmodifiableList(hacks);
	}

	public Hack get(String name)
	{
		return byName.get(name.toLowerCase());
	}

	public List<Hack> inCategory(Category category)
	{
		List<Hack> list = new ArrayList<>();
		for(Hack h : hacks)
			if(h.getCategory() == category)
				list.add(h);

		return list;
	}

	/** Schaltet alle Hacks aus (Panik-Knopf). */
	public void disableAll()
	{
		for(Hack h : hacks)
			h.setEnabled(false);
	}

	// ---------------------------------------------------------------
	// Speichern / Laden
	// ---------------------------------------------------------------

	public void save()
	{
		try
		{
			JsonObject root = new JsonObject();
			JsonObject hackObj = new JsonObject();
			for(Hack h : hacks)
			{
				JsonObject o = new JsonObject();
				o.addProperty("key", h.getKey());
				JsonObject settings = new JsonObject();
				for(Setting s : h.getSettings())
					settings.add(s.name, s.save());

				o.add("settings", settings);
				hackObj.add(h.getName(), o);
			}
			root.add("hacks", hackObj);

			JsonArray friends = new JsonArray();
			for(String f : Friends.all())
				friends.add(f);

			root.add("friends", friends);
			root.addProperty("panicKey", Panic.getKey());

			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, gson.toJson(root),
				StandardCharsets.UTF_8);

		}catch(IOException | RuntimeException e)
		{
			NovaClient.LOGGER.error("Konnte Config nicht speichern", e);
		}
	}

	public void load()
	{
		if(!Files.exists(configFile))
			return;

		try
		{
			String text = Files.readString(configFile, StandardCharsets.UTF_8);
			JsonObject root = JsonParser.parseString(text).getAsJsonObject();

			JsonObject hackObj = root.getAsJsonObject("hacks");
			if(hackObj != null)
				for(Hack h : hacks)
				{
					JsonElement e = hackObj.get(h.getName());
					if(e == null || !e.isJsonObject())
						continue;

					JsonObject o = e.getAsJsonObject();
					if(o.has("key"))
						h.setKey(o.get("key").getAsInt());

					JsonObject settings = o.getAsJsonObject("settings");
					if(settings == null)
						continue;

					for(Setting s : h.getSettings())
						s.load(settings.get(s.name));
				}

			if(root.has("panicKey"))
				Panic.setKey(root.get("panicKey").getAsInt());

			Friends.clear();
			JsonArray friends = root.getAsJsonArray("friends");
			if(friends != null)
				for(JsonElement f : friends)
					Friends.add(f.getAsString());

		}catch(IOException | RuntimeException e)
		{
			NovaClient.LOGGER.error("Konnte Config nicht laden", e);
		}
	}
}
