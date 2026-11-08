package dev.nova.client.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Slider-Einstellung mit Minimum, Maximum und Schrittweite. */
public final class NumSetting extends Setting
{
	private double value;
	public final double min;
	public final double max;
	public final double step;

	public NumSetting(String name, String description, double defaultValue,
		double min, double max, double step)
	{
		super(name, description);
		this.min = min;
		this.max = max;
		this.step = step;
		set(defaultValue);
	}

	public double get()
	{
		return value;
	}

	public float getF()
	{
		return (float)value;
	}

	public int getI()
	{
		return (int)Math.round(value);
	}

	public void set(double v)
	{
		if(step > 0)
			v = Math.round(v / step) * step;

		value = Math.max(min, Math.min(max, v));
	}

	/** Setzt den Wert anhand eines Bruchteils (0..1) des Slider-Bereichs. */
	public void setFraction(double fraction)
	{
		set(min + (max - min) * Math.max(0, Math.min(1, fraction)));
	}

	public double getFraction()
	{
		return (value - min) / (max - min);
	}

	public String display()
	{
		if(step >= 1)
			return Integer.toString((int)Math.round(value));

		return String.format(java.util.Locale.ROOT, "%.2f", value);
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
			&& element.getAsJsonPrimitive().isNumber())
			set(element.getAsDouble());
	}
}
