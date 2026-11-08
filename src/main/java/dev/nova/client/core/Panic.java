package dev.nova.client.core;

import dev.nova.client.NovaClient;
import dev.nova.client.gui.ClickGuiScreen;
import dev.nova.client.util.Util;
import net.minecraft.client.Minecraft;

/**
 * Not-Aus fuer den ganzen Client (wie das Panic-Feature in Wurst).
 *
 * Nach {@link #trigger()} sind alle Hacks aus, das ClickGUI ist zu, HUD,
 * Tastenbelegungen und Befehle sind stumm. Nur die Panic-Taste und der
 * Chat-Befehl ".nova on" funktionieren noch. Der Mod wird dabei nicht aus
 * dem Spiel entladen, er verhaelt sich nur wie ein Mod ohne Funktion.
 */
public final class Panic
{
	public static final int DEFAULT_KEY = 269; // GLFW_KEY_END

	private static volatile boolean active = true;
	private static volatile int key = DEFAULT_KEY;

	private Panic()
	{}

	/** True, solange der Client normal laeuft. */
	public static boolean isActive()
	{
		return active;
	}

	public static int getKey()
	{
		return key;
	}

	public static void setKey(int newKey)
	{
		key = newKey;
	}

	/** Taste gedrueckt: erst ausschalten, beim naechsten Mal wieder an. */
	public static void toggle()
	{
		if(active)
			trigger();
		else
			restore();
	}

	/** Alles aus. Absichtlich ohne Chatmeldung, damit nichts auf dem Bildschirm bleibt. */
	public static void trigger()
	{
		if(!active)
			return;

		active = false;

		Minecraft mc = Minecraft.getInstance();
		if(mc.gui.screen() instanceof ClickGuiScreen)
			mc.gui.setScreen(null);

		if(NovaClient.hacks != null)
		{
			NovaClient.hacks.disableAll();
			NovaClient.hacks.save();
		}
	}

	/** Client wieder freigeben. Die Hacks bleiben aus, bis du sie neu einschaltest. */
	public static void restore()
	{
		if(active)
			return;
		
		active = true;
		Util.chat("Nova Client wieder aktiv. Alle Hacks sind aus.");
	}
}
