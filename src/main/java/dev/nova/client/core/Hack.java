package dev.nova.client.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.nova.client.NovaClient;
import dev.nova.client.render.EspRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Basisklasse fuer jedes Feature ("Hack"/Modul) des Clients. */
public abstract class Hack
{
	protected static final Minecraft MC = Minecraft.getInstance();

	private final String name;
	private final String description;
	private final Category category;
	private final List<Setting> settings = new ArrayList<>();
	private boolean enabled;
	private int key = -1;

	/** Ob der Hack in der Modulliste (HUD oben rechts) auftaucht. */
	protected boolean showInList = true;

	protected Hack(String name, String description, Category category)
	{
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected final <T extends Setting> T add(T setting)
	{
		settings.add(setting);
		return setting;
	}

	public final String getName()
	{
		return name;
	}

	public final String getDescription()
	{
		return description;
	}

	public final Category getCategory()
	{
		return category;
	}

	public final List<Setting> getSettings()
	{
		return Collections.unmodifiableList(settings);
	}

	public final boolean isEnabled()
	{
		return enabled;
	}

	public final boolean isShownInList()
	{
		return showInList;
	}

	public final int getKey()
	{
		return key;
	}

	public final void setKey(int key)
	{
		this.key = key;
	}

	public final void toggle()
	{
		setEnabled(!enabled);
	}

	public final void setEnabled(boolean value)
	{
		if(value == enabled)
			return;

		enabled = value;
		try
		{
			if(value)
				onEnable();
			else
				onDisable();

		}catch(Throwable t)
		{
			NovaClient.LOGGER.error("Fehler in {}", name, t);
			enabled = false;
		}
	}

	/** Wird beim Einschalten aufgerufen. */
	protected void onEnable()
	{}

	/** Wird beim Ausschalten aufgerufen. */
	protected void onDisable()
	{}

	/** Einmal pro Client-Tick, solange der Hack an ist und ein Spiel laeuft. */
	public void onTick()
	{}

	/** In der Welt zeichnen (ESP, Tracer, ...). */
	public void onRender3D(EspRenderer r)
	{}

	/** Auf dem HUD zeichnen. */
	public void onRender2D(GuiGraphicsExtractor g, int width, int height)
	{}

	/** Darf eine ausgehende Chatnachricht veraendern. */
	public String onChatSend(String message)
	{
		return message;
	}

	/** Darf das Sichtfeld (FOV) veraendern. */
	public float modifyFov(float fov)
	{
		return fov;
	}

	/** Zusatztext hinter dem Namen in der Modulliste, z.B. der Modus. */
	public String getSuffix()
	{
		return null;
	}

	/** Sichere Textdarstellung der gebundenen Taste. */
	public static String keyName(int key)
	{
		if(key < 0)
			return "None";

		if(key >= 65 && key <= 90 || key >= 48 && key <= 57)
			return String.valueOf((char)key);

		if(key >= 290 && key <= 301)
			return "F" + (key - 289);

		return switch(key)
		{
			case 32 -> "Space";
			case 262 -> "Right";
			case 263 -> "Left";
			case 264 -> "Down";
			case 265 -> "Up";
			case 340 -> "LShift";
			case 344 -> "RShift";
			case 341 -> "LCtrl";
			case 345 -> "RCtrl";
			case 342 -> "LAlt";
			case 346 -> "RAlt";
			case 258 -> "Tab";
			case 280 -> "Caps";
			case 91 -> "[";
			case 93 -> "]";
			case 59 -> ";";
			case 39 -> "'";
			case 44 -> ",";
			case 46 -> ".";
			case 47 -> "/";
			case 92 -> "\\";
			case 45 -> "-";
			case 61 -> "=";
			default -> "Key" + key;
		};
	}
}
