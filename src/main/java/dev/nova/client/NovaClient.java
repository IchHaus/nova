package dev.nova.client;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.nova.client.core.HackManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Nova Client: ein client-seitiger Utility-Mod mit ClickGUI. */
@Mod(value = NovaClient.MODID, dist = Dist.CLIENT)
public class NovaClient
{
	public static final String MODID = "novaclient";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final String VERSION = "1.0.0";

	/** Wird beim Client-Setup erzeugt (siehe NovaEvents). */
	public static HackManager hacks;

	public NovaClient(IEventBus modBus)
	{
		LOGGER.info("Nova Client {} wird geladen", VERSION);
		
		// Mod-Bus
		modBus.addListener(NovaEvents::onRegisterPipelines);
		modBus.addListener(NovaEvents::onClientSetup);
		
		// Spiel-Bus
		IEventBus game = NeoForge.EVENT_BUS;
		game.addListener(NovaEvents::onTick);
		game.addListener(NovaEvents::onRenderLevel);
		game.addListener(NovaEvents::onRenderGui);
		game.addListener(NovaEvents::onKey);
		game.addListener(NovaEvents::onChat);
		game.addListener(NovaEvents::onFov);
	}
}
