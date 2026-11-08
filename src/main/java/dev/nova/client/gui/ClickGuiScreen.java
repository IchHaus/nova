package dev.nova.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import dev.nova.client.NovaClient;
import dev.nova.client.core.BoolSetting;
import dev.nova.client.core.Category;
import dev.nova.client.core.Hack;
import dev.nova.client.core.ListSetting;
import dev.nova.client.core.ModeSetting;
import dev.nova.client.core.NumSetting;
import dev.nova.client.core.Panic;
import dev.nova.client.core.Setting;
import dev.nova.client.util.Theme;
import dev.nova.client.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Das ClickGUI: eine Spalte pro Kategorie. Linksklick schaltet ein Modul um,
 * Rechtsklick klappt die Einstellungen auf, Mittelklick belegt eine Taste.
 */
public final class ClickGuiScreen extends Screen
{
	private static final int PW = 124;
	private static final int HEADER = 20;
	private static final int ROW = 15;
	private static final int SROW = 13;

	/** Positionen bleiben erhalten, solange das Spiel laeuft. */
	private static final List<Panel> PANELS = new ArrayList<>();
	private static final Map<Hack, Float> PROGRESS = new HashMap<>();
	private static final Set<Hack> EXPANDED = new HashSet<>();
	private static int scroll;

	private static final class Panel
	{
		final Category category;
		int x;
		int y;
		boolean open = true;

		Panel(Category category, int x, int y)
		{
			this.category = category;
			this.x = x;
			this.y = y;
		}
	}

	private enum Kind
	{
		HEADER,
		MODULE,
		BIND,
		BOOL,
		NUM,
		MODE
	}

	private record Row(Kind kind, int x, int y, int w, int h, Panel panel,
		Hack hack, Setting setting)
	{}

	private final List<Row> rows = new ArrayList<>();
	private Panel dragPanel;
	private int dragOffX;
	private int dragOffY;
	private NumSetting dragSlider;
	private Row dragRow;
	private Hack listening;
	private Hack hovered;
	private long lastNano = System.nanoTime();

	public ClickGuiScreen()
	{
		super(Component.literal("Nova Client"));

		if(PANELS.isEmpty())
		{
			int i = 0;
			for(Category c : Category.values())
				PANELS.add(new Panel(c, 14 + i++ * (PW + 8), 34));
		}
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX,
		int mouseY, float deltaTicks)
	{
		// Kein Vanilla-Blur, wir zeichnen den Hintergrund selbst.
	}

	@Override
	public void onClose()
	{
		if(NovaClient.hacks != null)
			NovaClient.hacks.save();

		super.onClose();
	}

	// ---------------------------------------------------------------
	// Zeichnen
	// ---------------------------------------------------------------

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX,
		int mouseY, float partialTicks)
	{
		long now = System.nanoTime();
		float dt = Math.min(0.1F, (now - lastNano) / 1_000_000_000F);
		lastNano = now;

		rows.clear();
		hovered = null;

		// Hintergrund: leichtes Abdunkeln mit Farbstreifen oben
		g.fill(0, 0, width, height, 0x96060610);
		Widgets.gradientH(g, 0, 0, width, 2, Theme.VIOLET, Theme.CYAN);

		// Titel
		String title = "NOVA CLIENT";
		Util.gradientText(g, title, (width - Util.textWidth(title)) / 2, 9, 0);
		String hint = "Rechtsklick: Einstellungen   Mittelklick: Taste   "
			+ "Rechtsshift: schliessen";
		Util.text(g, hint, (width - Util.textWidth(hint)) / 2, height - 12,
			Theme.TEXT_DIM, true);

		for(Panel p : PANELS)
			drawPanel(g, p, mouseX, mouseY, dt);

		if(hovered != null)
			drawTooltip(g, hovered.getDescription(), mouseX, mouseY);
	}

	private void drawPanel(GuiGraphicsExtractor g, Panel p, int mx, int my,
		float dt)
	{
		List<Hack> hacks = NovaClient.hacks.inCategory(p.category);
		int px = p.x;
		int py = p.y + scroll;
		int accent = p.category.color;

		// Gesamthoehe berechnen (Animationen aktualisieren)
		int total = HEADER;
		if(p.open)
			for(Hack h : hacks)
				total += ROW + extraHeight(h, dt);

		// Schatten und Hintergrund
		g.fill(px - 2, py - 2, px + PW + 2, py + total + 2, 0x38000000);
		g.fill(px, py + HEADER, px + PW, py + total, 0xE60D0D14);

		// Kopfzeile
		Widgets.rounded(g, px, py, PW, HEADER, Theme.PANEL_HEADER);
		g.fill(px + 1, py, px + PW - 1, py + 2, accent);
		g.fill(px + 7, py + 8, px + 11, py + 12, accent);
		Util.text(g, p.category.displayName, px + 16, py + 6, Theme.TEXT, true);
		Util.text(g, p.open ? "-" : "+", px + PW - 12, py + 6,
			Theme.TEXT_DIM, true);
		rows.add(new Row(Kind.HEADER, px, py, PW, HEADER, p, null, null));

		if(!p.open)
			return;

		int y = py + HEADER;
		for(Hack h : hacks)
		{
			boolean hover = inside(mx, my, px, y, PW, ROW);
			if(hover)
				hovered = h;

			// Modulzeile
			if(h.isEnabled())
			{
				Widgets.gradientH(g, px, y, PW, ROW,
					Theme.withAlpha(accent, 0x58), Theme.withAlpha(accent, 0x12));
				g.fill(px, y, px + 2, y + ROW, accent);
			}

			if(hover)
				g.fill(px, y, px + PW, y + ROW, Theme.ROW_HOVER);

			Util.text(g, h.getName(), px + 8, y + 4,
				h.isEnabled() ? Theme.TEXT : Theme.TEXT_DIM, true);
			Util.text(g, EXPANDED.contains(h) ? "-" : "+", px + PW - 11, y + 4,
				Theme.TEXT_DIM, true);
			rows.add(new Row(Kind.MODULE, px, y, PW, ROW, p, h, null));

			int extra = extraHeight(h, 0);
			y += ROW;
			if(extra > 0)
			{
				g.enableScissor(px, y, px + PW, y + extra);
				drawSettings(g, p, h, px, y, mx, my,
					PROGRESS.getOrDefault(h, 0F) > 0.98F);
				g.disableScissor();
				y += extra;
			}
		}
	}

	private void drawSettings(GuiGraphicsExtractor g, Panel p, Hack h, int px,
		int y, int mx, int my, boolean interactive)
	{
		int accent = p.category.color;
		int rowY = y;

		// Hintergrund fuer den aufgeklappten Bereich
		g.fill(px, y, px + PW, y + (1 + h.getSettings().size()) * SROW,
			0xF0090910);
		g.fill(px + 2, y, px + 3, y + (1 + h.getSettings().size()) * SROW,
			Theme.withAlpha(accent, 0x80));

		// Tastenbelegung
		boolean hoverBind = inside(mx, my, px, rowY, PW, SROW);
		if(hoverBind)
			g.fill(px, rowY, px + PW, rowY + SROW, Theme.ROW_HOVER);

		Util.text(g, "Bind", px + 8, rowY + 3, Theme.TEXT_DIM, true);
		String key = listening == h ? "..." : Hack.keyName(h.getKey());
		Util.text(g, key, px + PW - 6 - Util.textWidth(key), rowY + 3,
			listening == h ? Theme.GOOD : accent, true);
		if(interactive)
			rows.add(new Row(Kind.BIND, px, rowY, PW, SROW, p, h, null));

		rowY += SROW;

		for(Setting s : h.getSettings())
		{
			boolean hover = inside(mx, my, px, rowY, PW, SROW);
			if(hover)
				g.fill(px, rowY, px + PW, rowY + SROW, Theme.ROW_HOVER);

			Util.text(g, s.name, px + 8, rowY + 2, Theme.TEXT, true);

			if(s instanceof BoolSetting b)
			{
				int pillX = px + PW - 22;
				int pillY = rowY + 3;
				Widgets.rounded(g, pillX, pillY, 16, 7,
					b.get() ? Theme.withAlpha(accent, 0xFF) : 0xFF2A2A38);
				int knobX = b.get() ? pillX + 10 : pillX + 1;
				g.fill(knobX, pillY + 1, knobX + 5, pillY + 6, 0xFFFFFFFF);
				if(interactive)
					rows.add(new Row(Kind.BOOL, px, rowY, PW, SROW, p, h, s));

			}else if(s instanceof NumSetting n)
			{
				String v = n.display();
				Util.text(g, v, px + PW - 6 - Util.textWidth(v), rowY + 2,
					accent, true);
				int tx = px + 6;
				int tw = PW - 12;
				g.fill(tx, rowY + 10, tx + tw, rowY + 12, 0xFF2A2A38);
				Widgets.gradientH(g, tx, rowY + 10,
					Math.max(1, (int)(tw * n.getFraction())), 2, Theme.VIOLET,
					accent);
				if(interactive)
					rows.add(new Row(Kind.NUM, px, rowY, PW, SROW, p, h, s));

			}else if(s instanceof ModeSetting m)
			{
				String v = m.get();
				Util.text(g, v, px + PW - 6 - Util.textWidth(v), rowY + 2,
					accent, true);
				if(interactive)
					rows.add(new Row(Kind.MODE, px, rowY, PW, SROW, p, h, s));
			}else if(s instanceof ListSetting l)
			{
				// Nur Anzeige, bearbeitet wird per Chat (.xray add|remove).
				String v = l.display();
				Util.text(g, v, px + PW - 6 - Util.textWidth(v), rowY + 2,
					Theme.TEXT_DIM, true);
			}

			if(hover)
				hovered = null; // Tooltip nur fuer Modulzeilen

			rowY += SROW;
		}
	}

	/** Aktuelle Hoehe des aufgeklappten Bereichs, mit Animation. */
	private int extraHeight(Hack h, float dt)
	{
		float target = EXPANDED.contains(h) ? 1F : 0F;
		float cur = PROGRESS.getOrDefault(h, 0F);
		if(cur != target)
		{
			cur += (target - cur) * Math.min(1F, dt * 14F);
			if(Math.abs(target - cur) < 0.01F)
				cur = target;

			PROGRESS.put(h, cur);
		}

		int full = (1 + h.getSettings().size()) * SROW;
		return (int)(full * Widgets.ease(cur));
	}

	private void drawTooltip(GuiGraphicsExtractor g, String text, int mx,
		int my)
	{
		int w = Util.textWidth(text) + 10;
		int x = Math.min(mx + 10, width - w - 4);
		int y = my + 12;
		Widgets.rounded(g, x, y, w, 16, 0xF0101018);
		Widgets.outline(g, x, y, w, 16, Theme.withAlpha(Theme.VIOLET, 0xA0));
		Util.text(g, text, x + 5, y + 4, Theme.TEXT, true);
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h)
	{
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ---------------------------------------------------------------
	// Eingabe
	// ---------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
	{
		int mx = (int)event.x();
		int my = (int)event.y();
		int button = event.button();

		if(listening != null)
		{
			listening = null;
			return true;
		}

		for(Row r : rows)
		{
			if(!inside(mx, my, r.x(), r.y(), r.w(), r.h()))
				continue;

			switch(r.kind())
			{
				case HEADER ->
				{
					if(button == 0)
					{
						dragPanel = r.panel();
						dragOffX = mx - r.panel().x;
						dragOffY = my - scroll - r.panel().y;
					}else if(button == 1)
						r.panel().open = !r.panel().open;
				}

				case MODULE ->
				{
					if(button == 0)
						r.hack().toggle();
					else if(button == 1)
					{
						if(!EXPANDED.remove(r.hack()))
							EXPANDED.add(r.hack());
					}else if(button == 2)
						listening = r.hack();
				}

				case BIND ->
				{
					if(button == 0)
						listening = r.hack();
				}

				case BOOL ->
				{
					if(button == 0)
						((BoolSetting)r.setting()).toggle();
				}

				case MODE ->
				{
					if(button == 0)
						((ModeSetting)r.setting()).cycle();
				}

				case NUM ->
				{
					if(button == 0)
					{
						dragSlider = (NumSetting)r.setting();
						dragRow = r;
						updateSlider(mx);
					}
				}
			}

			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy)
	{
		int mx = (int)event.x();
		int my = (int)event.y();

		if(dragPanel != null)
		{
			dragPanel.x = Math.max(0, Math.min(width - PW, mx - dragOffX));
			dragPanel.y = my - dragOffY - scroll;
			return true;
		}

		if(dragSlider != null)
		{
			updateSlider(mx);
			return true;
		}

		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event)
	{
		dragPanel = null;
		dragSlider = null;
		dragRow = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY,
		double horizontalAmount, double verticalAmount)
	{
		scroll += (int)(verticalAmount * 16);
		scroll = Math.max(-700, Math.min(0, scroll));
		return true;
	}

	private void updateSlider(int mx)
	{
		if(dragSlider == null || dragRow == null)
			return;

		int tx = dragRow.x() + 6;
		int tw = PW - 12;
		dragSlider.setFraction((mx - tx) / (double)tw);
	}

	@Override
	public boolean keyPressed(KeyEvent event)
	{
		int key = event.key();

		if(listening != null)
		{
			if(key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE)
				listening.setKey(-1);
			else if(key != GLFW.GLFW_KEY_ESCAPE)
				listening.setKey(key);

			listening = null;
			return true;
		}

		// Panic-Taste: Client sofort stumm schalten (schliesst auch dieses GUI).
		if(key == Panic.getKey())
		{
			Panic.trigger();
			return true;
		}

		if(key == GLFW.GLFW_KEY_RIGHT_SHIFT)
		{
			onClose();
			return true;
		}

		return super.keyPressed(event);
	}
}
