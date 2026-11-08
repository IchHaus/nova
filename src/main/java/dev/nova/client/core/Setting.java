package dev.nova.client.core;

import com.google.gson.JsonElement;

/** Basisklasse fuer alle einstellbaren Werte eines Hacks. */
public abstract class Setting
{
	public final String name;
	public final String description;

	protected Setting(String name, String description)
	{
		this.name = name;
		this.description = description;
	}

	public abstract JsonElement save();

	public abstract void load(JsonElement element);
}
