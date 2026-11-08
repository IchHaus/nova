package dev.nova.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.nova.client.core.Commands;
import dev.nova.client.core.Hack;
import dev.nova.client.core.HackManager;
import dev.nova.client.core.Panic;
import dev.nova.client.gui.ClickGuiScreen;
import dev.nova.client.gui.Hud;
import dev.nova.client.render.EspPipelines;
import dev.nova.client.render.EspRenderer;
import dev.nova.client.util.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Alle Event-Handler des Clients an einer Stelle (registriert in NovaClient). */
public final class NovaEvents
{
	private static final Minecraft MC = Minecraft.getInstance();

	private NovaEvents()
	{}

	// ---------------------------------------------------------------
	// Mod-Bus
	// ---------------------------------------------------------------

	static void onRegisterPipelines(RegisterRenderPipelinesEvent event)
	{
		event.registerPipeline(EspPipelines.ESP_LINES);
		event.registerPipeline(EspPipelines.ESP_QUADS);
	}

	static void onClientSetup(FMLClientSetupEvent event)
	{
		HackManager manager = new HackManager();
		manager.load();
		NovaClient.hacks = manager;
		Runtime.getRuntime()
			.addShutdownHook(new Thread(manager::save, "NovaClient-Save"));
	}

	// ---------------------------------------------------------------
	// Spiel-Bus
	// ---------------------------------------------------------------

	static void onTick(ClientTickEvent.Post event)
	{
		if(NovaClient.hacks == null || !Panic.isActive() || !Util.inGame())
			return;

		for(Hack h : NovaClient.hacks.all())
		{
			if(!h.isEnabled())
				continue;

			try
			{
				h.onTick();

			}catch(Throwable t)
			{
				NovaClient.LOGGER.error("Fehler in {}", h.getName(), t);
				h.setEnabled(false);
				Util.chat("§c" + h.getName()
					+ " ist abgestuerzt und wurde ausgeschaltet.");
			}
		}
	}

	static void onRenderLevel(RenderLevelStageEvent.AfterLevel event)
	{
		if(NovaClient.hacks == null || !Panic.isActive() || !Util.inGame())
			return;

		Camera camera = MC.gameRenderer.mainCamera();
		if(camera == null)
			return;

		// Der Event liefert keinen PoseStack, daher bauen wir uns einen.
		PoseStack pose = new PoseStack();
		pose.mulPose(event.getModelViewMatrix());
		EspRenderer renderer = new EspRenderer(pose, camera.position(),
			event.getLevelRenderState().worldPartialTicks);

		for(Hack h : NovaClient.hacks.all())
		{
			if(!h.isEnabled())
				continue;

			try
			{
				h.onRender3D(renderer);

			}catch(Throwable t)
			{
				NovaClient.LOGGER.error("Render-Fehler in {}", h.getName(), t);
				h.setEnabled(false);
			}
		}

		renderer.flush();
	}

	static void onRenderGui(RenderGuiEvent.Post event)
	{
		if(NovaClient.hacks == null || !Panic.isActive() || MC.options.hideGui)
			return;

		int w = MC.getWindow().getGuiScaledWidth();
		int h = MC.getWindow().getGuiScaledHeight();
		Hud.render(event.getGuiGraphics(), w, h);
	}

	static void onKey(InputEvent.Key event)
	{
		if(NovaClient.hacks == null || event.getAction() != GLFW.GLFW_PRESS
			|| !Util.inGame() || MC.gui.screen() != null)
			return;

		int key = event.getKeyEvent().key();
		
		// Panic-Taste hat immer Vorrang und funktioniert auch im Panic-Zustand.
		if(key == Panic.getKey())
		{
			Panic.toggle();
			return;
		}
		
		if(!Panic.isActive())
			return;
		
		if(key == GLFW.GLFW_KEY_RIGHT_SHIFT)
		{
			MC.gui.setScreen(new ClickGuiScreen());
			return;
		}

		for(Hack h : NovaClient.hacks.all())
			if(h.getKey() == key)
				h.toggle();
	}

	static void onChat(ClientChatEvent event)
	{
		if(NovaClient.hacks == null)
			return;

		String message = event.getMessage();
		
		// Im Panic-Zustand bleibt der Chat ganz normal. Nur ".nova on" wird
		// abgefangen, damit es nicht auf dem Server landet.
		if(!Panic.isActive())
		{
			if(message.trim().equalsIgnoreCase(".nova on"))
			{
				event.setCanceled(true);
				MC.gui.hud.getChat().addRecentChat(message);
				Panic.restore();
			}
			
			return;
		}
		
		if(message.startsWith("."))
		{
			event.setCanceled(true);
			MC.gui.hud.getChat().addRecentChat(message);
			Commands.handle(message.substring(1));
			return;
		}

		String result = message;
		for(Hack h : NovaClient.hacks.all())
			if(h.isEnabled())
				result = h.onChatSend(result);

		if(!result.equals(message))
			event.setMessage(result);
	}

	static void onFov(ViewportEvent.ComputeFov event)
	{
		if(NovaClient.hacks == null || !Panic.isActive())
			return;

		float fov = event.getFOV();
		for(Hack h : NovaClient.hacks.all())
			if(h.isEnabled())
				fov = h.modifyFov(fov);

		event.setFOV(fov);
	}
}
