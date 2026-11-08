package dev.nova.client.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Auswahl-Einstellung, die per Klick durch mehrere Modi blaettert. */
public final class ModeSetting extends Setting
{
	private final String[] modes;
	private int index;

	public ModeSetting(String name, String description, String defaultMode,
		String... modes)
	{
		super(name, description);
		this.modes = modes;
		for(int i = 0; i < modes.length; i++)
			if(modes[i].equalsIgnoreCase(defaultMode))
				index = i;
	}

	public String get()
	{
		return modes[index];
	}

	public boolean is(String mode)
	{
		return modes[index].equalsIgnoreCase(mode);
	}

	public void cycle()
	{
		index = (index + 1) % modes.length;
	}

	@Override
	public JsonElement save()
	{
		return new JsonPrimitive(modes[index]);
	}

	@Override
	public void load(JsonElement element)
	{
		if(element == null || !element.isJsonPrimitive())
			return;

		String s = element.getAsString();
		for(int i = 0; i < modes.length; i++)
			if(modes[i].equalsIgnoreCase(s))
				index = i;
	}
}
