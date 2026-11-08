package dev.nova.client.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/** Liste von Texten (z. B. Block-IDs). Wird per Chat-Befehl bearbeitet. */
public final class ListSetting extends Setting
{
	private final Set<String> values = new LinkedHashSet<>();

	public ListSetting(String name, String description)
	{
		super(name, description);
	}

	public boolean contains(String value)
	{
		return values.contains(value);
	}

	public boolean add(String value)
	{
		return values.add(value);
	}

	public boolean remove(String value)
	{
		return values.remove(value);
	}

	public void clear()
	{
		values.clear();
	}

	public Set<String> all()
	{
		return Collections.unmodifiableSet(values);
	}

	public String display()
	{
		return values.size() + " Eintraege";
	}

	@Override
	public JsonElement save()
	{
		JsonArray array = new JsonArray();
		for(String s : values)
			array.add(s);

		return array;
	}

	@Override
	public void load(JsonElement element)
	{
		if(element == null || !element.isJsonArray())
			return;

		values.clear();
		for(JsonElement e : element.getAsJsonArray())
			if(e.isJsonPrimitive())
				values.add(e.getAsString());
	}
}
