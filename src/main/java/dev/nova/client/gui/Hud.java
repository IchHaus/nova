package dev.nova.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.nova.client.NovaClient;
import dev.nova.client.core.Hack;
import dev.nova.client.util.Theme;
import dev.nova.client.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** HUD: Wasserzeichen, Koordinaten, Modulliste und Hack-Overlays. */
public final class Hud
{
	private Hud()
	{}

	public static void render(GuiGraphicsExtractor g, int width, int height)
	{
		if(!Util.inGame())
			return;

		drawWatermark(g);
		drawArrayList(g, width);

		for(Hack h : NovaClient.hacks.all())
			if(h.isEnabled())
				h.onRender2D(g, width, height);
	}

	private static void drawWatermark(GuiGraphicsExtractor g)
	{
		String name = "Nova";
		String sub = " Client " + NovaClient.VERSION;
		int w = Util.textWidth(name) + Util.textWidth(sub) + 12;

		Widgets.rounded(g, 4, 4, w, 16, 0xB00B0B12);
		g.fill(5, 18, 3 + w, 19, Theme.withAlpha(Theme.accent(0), 0xFF));
		Util.gradientText(g, name, 10, 8, 0);
		Util.text(g, sub, 10 + Util.textWidth(name), 8, Theme.TEXT_DIM, true);

		var p = Util.MC.player;
		String xyz = String.format(java.util.Locale.ROOT, "XYZ  %d  %d  %d",
			(int)Math.floor(p.getX()), (int)Math.floor(p.getY()),
			(int)Math.floor(p.getZ()));
		Util.text(g, xyz, 8, 24, Theme.TEXT_DIM, true);
	}

	private static void drawArrayList(GuiGraphicsExtractor g, int width)
	{
		List<Hack> active = new ArrayList<>();
		for(Hack h : NovaClient.hacks.all())
			if(h.isEnabled() && h.isShownInList())
				active.add(h);

		active.sort(Comparator.comparingInt((Hack h) -> -lineWidth(h)));

		int y = 4;
		int i = 0;
		for(Hack h : active)
		{
			String suffix = h.getSuffix();
			int w = lineWidth(h);
			int x = width - w - 8;
			int color = Theme.accent(i * 0.45);

			g.fill(x - 3, y, width - 2, y + 12, 0xA0090910);
			g.fill(width - 3, y, width - 1, y + 12, color);
			Util.text(g, h.getName(), x, y + 2, color, true);
			if(suffix != null)
				Util.text(g, suffix, x + Util.textWidth(h.getName() + " "),
					y + 2, Theme.TEXT_DIM, true);

			y += 12;
			i++;
		}
	}

	private static int lineWidth(Hack h)
	{
		String s = h.getName();
		if(h.getSuffix() != null)
			s += " " + h.getSuffix();

		return Util.textWidth(s) + 2;
	}
}
