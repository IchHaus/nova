package dev.nova.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Kleine Zeichenhelfer fuer die Oberflaeche. */
final class Widgets
{
	private Widgets()
	{}

	/** Rechteck mit leicht abgerundeten Ecken (1 Pixel). */
	static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h,
		int color)
	{
		g.fill(x + 1, y, x + w - 1, y + h, color);
		g.fill(x, y + 1, x + w, y + h - 1, color);
	}

	/** Rechteck mit Rahmen. */
	static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h,
		int color)
	{
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y, x + 1, y + h, color);
		g.fill(x + w - 1, y, x + w, y + h, color);
	}

	/** Waagerechter Farbverlauf aus kleinen Streifen. */
	static void gradientH(GuiGraphicsExtractor g, int x, int y, int w, int h,
		int c1, int c2)
	{
		int steps = Math.max(1, Math.min(w, 24));
		for(int i = 0; i < steps; i++)
		{
			int x1 = x + w * i / steps;
			int x2 = x + w * (i + 1) / steps;
			g.fill(x1, y, x2, y + h,
				dev.nova.client.util.Theme.lerp(c1, c2, i / (float)steps));
		}
	}

	/** Sanfte Ease-Out-Kurve. */
	static float ease(float t)
	{
		t = Math.max(0, Math.min(1, t));
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}
}
