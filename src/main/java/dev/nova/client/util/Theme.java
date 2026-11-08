package dev.nova.client.util;

/** Farben und Farbverlaeufe des Clients (alle Werte als ARGB-Integer). */
public final class Theme
{
	public static final int VIOLET = 0xFF8B5CF6;
	public static final int CYAN = 0xFF22D3EE;
	public static final int PANEL = 0xE60F0F17;
	public static final int PANEL_HEADER = 0xF0171724;
	public static final int ROW_HOVER = 0x28FFFFFF;
	public static final int TEXT = 0xFFF2F2F8;
	public static final int TEXT_DIM = 0xFF9A9AB0;
	public static final int GOOD = 0xFF4ADE80;
	public static final int BAD = 0xFFF87171;

	private Theme()
	{}

	public static int argb(int a, int r, int g, int b)
	{
		return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8
			| b & 0xFF;
	}

	public static int withAlpha(int color, int alpha)
	{
		return (alpha & 0xFF) << 24 | color & 0x00FFFFFF;
	}

	public static int lerp(int c1, int c2, float t)
	{
		t = Math.max(0, Math.min(1, t));
		int a = (int)(alpha(c1) + (alpha(c2) - alpha(c1)) * t);
		int r = (int)(red(c1) + (red(c2) - red(c1)) * t);
		int g = (int)(green(c1) + (green(c2) - green(c1)) * t);
		int b = (int)(blue(c1) + (blue(c2) - blue(c1)) * t);
		return argb(a, r, g, b);
	}

	public static int alpha(int c)
	{
		return c >>> 24 & 0xFF;
	}

	public static int red(int c)
	{
		return c >>> 16 & 0xFF;
	}

	public static int green(int c)
	{
		return c >>> 8 & 0xFF;
	}

	public static int blue(int c)
	{
		return c & 0xFF;
	}

	/** Violett-Cyan-Verlauf, t zwischen 0 und 1. */
	public static int gradient(float t)
	{
		return lerp(VIOLET, CYAN, t);
	}

	/** Langsam pulsierender Akzent, offset verschiebt die Phase. */
	public static int accent(double offset)
	{
		double time = System.currentTimeMillis() / 900.0;
		float t = (float)((Math.sin(time + offset) + 1.0) / 2.0);
		return gradient(t);
	}

	/** Regenbogenfarbe, hue in 0..1. */
	public static int rainbow(float hue)
	{
		hue = hue - (float)Math.floor(hue);
		float h = hue * 6F;
		int i = (int)h;
		float f = h - i;
		float q = 1F - f;
		float r, g, b;
		switch(i % 6)
		{
			case 0 -> { r = 1; g = f; b = 0; }
			case 1 -> { r = q; g = 1; b = 0; }
			case 2 -> { r = 0; g = 1; b = f; }
			case 3 -> { r = 0; g = q; b = 1; }
			case 4 -> { r = f; g = 0; b = 1; }
			default -> { r = 1; g = 0; b = q; }
		}
		return argb(255, (int)(r * 255), (int)(g * 255), (int)(b * 255));
	}
}
