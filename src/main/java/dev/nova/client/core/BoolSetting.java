package dev.nova.client.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolSetting extends Setting
{
	private boolean value;

	public BoolSetting(String name, String description, boolean defaultValue)
	{
		super(name, description);
		this.value = defaultValue;
	}

	public boolean get()
	{
		return value;
	}

	public void set(boolean value)
	{
		this.value = value;
	}

	public void toggle()
	{
		value = !value;
	}

	@Override
	public JsonElement save()
	{
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement element)
	{
		if(element != null && element.isJsonPrimitive()
			&& element.getAsJsonPrimitive().isBoolean())
			value = element.getAsBoolean();
	}
}
