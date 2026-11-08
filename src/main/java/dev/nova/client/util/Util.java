package dev.nova.client.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.nova.client.core.Friends;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Gemeinsame Hilfsfunktionen. */
public final class Util
{
	public static final Minecraft MC = Minecraft.getInstance();

	private Util()
	{}

	public static boolean inGame()
	{
		return MC.player != null && MC.level != null && MC.gameMode != null;
	}

	/** Schreibt eine Nachricht nur fuer dich in den Chat. */
	public static void chat(String message)
	{
		if(MC.gui == null)
			return;

		ChatComponent chat = MC.gui.hud.getChat();
		chat.addClientSystemMessage(
			Component.literal("§5[§dNova§5]§r " + message));
	}

	// ---------------------------------------------------------------
	// Positionen & Rotationen
	// ---------------------------------------------------------------

	public static Vec3 lerpPos(Entity e, float partialTicks)
	{
		if(e.isRemoved())
			return e.position();

		double x = Mth.lerp(partialTicks, e.xOld, e.getX());
		double y = Mth.lerp(partialTicks, e.yOld, e.getY());
		double z = Mth.lerp(partialTicks, e.zOld, e.getZ());
		return new Vec3(x, y, z);
	}

	public static AABB lerpBox(Entity e, float partialTicks)
	{
		if(e.isRemoved())
			return e.getBoundingBox();

		Vec3 offset = lerpPos(e, partialTicks).subtract(e.position());
		return e.getBoundingBox().move(offset);
	}

	/** Liefert {yaw, pitch}, um vom Auge des Spielers auf das Ziel zu schauen. */
	public static float[] rotationsTo(Vec3 target)
	{
		Vec3 eye = MC.player.getEyePosition();
		double dx = target.x - eye.x;
		double dy = target.y - eye.y;
		double dz = target.z - eye.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float)-Math.toDegrees(Math.atan2(dy, horizontal));
		return new float[]{yaw, Mth.clamp(pitch, -90F, 90F)};
	}

	/** Unterschied zwischen zwei Winkeln im Bereich -180..180. */
	public static float angleDiff(float from, float to)
	{
		return Mth.wrapDegrees(to - from);
	}

	// ---------------------------------------------------------------
	// Bewegung
	// ---------------------------------------------------------------

	public static boolean isMoving()
	{
		var o = MC.options;
		return o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown()
			|| o.keyRight.isDown();
	}

	/** Horizontale Bewegung in Blick-/Tastenrichtung mit der gegebenen Geschwindigkeit. */
	public static Vec3 horizontalMove(double speed)
	{
		var o = MC.options;
		float forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
		float strafe = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
		if(forward == 0 && strafe == 0)
			return Vec3.ZERO;

		double length = Math.sqrt(forward * forward + strafe * strafe);
		forward /= length;
		strafe /= length;

		double yaw = Math.toRadians(MC.player.getYRot());
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);
		return new Vec3((strafe * cos - forward * sin) * speed, 0,
			(forward * cos + strafe * sin) * speed);
	}

	// ---------------------------------------------------------------
	// Ziele
	// ---------------------------------------------------------------

	public static boolean isFriend(Entity e)
	{
		return e instanceof Player && Friends.has(e.getName().getString());
	}

	/** Alle gueltigen Kampfziele in Reichweite, nach Entfernung sortiert. */
	public static List<LivingEntity> targets(double range, boolean players,
		boolean hostile, boolean passive)
	{
		List<LivingEntity> list = new ArrayList<>();
		double rangeSq = range * range;

		for(Entity e : MC.level.entitiesForRendering())
		{
			if(!(e instanceof LivingEntity living) || e == MC.player
				|| !living.isAlive())
				continue;

			if(MC.player.distanceToSqr(living) > rangeSq)
				continue;

			if(e instanceof Player player)
			{
				if(!players || player.isSpectator() || isFriend(player))
					continue;

			}else if(e instanceof Mob)
			{
				if(e instanceof Enemy ? !hostile : !passive)
					continue;

			}else
				continue;

			list.add(living);
		}

		list.sort(Comparator.comparingDouble(en -> MC.player.distanceToSqr(en)));
		return list;
	}

	// ---------------------------------------------------------------
	// Text
	// ---------------------------------------------------------------

	public static int textWidth(String s)
	{
		return MC.font.width(s);
	}

	public static void text(GuiGraphicsExtractor g, String s, int x, int y,
		int color, boolean shadow)
	{
		g.text(MC.font, s, x, y, color, shadow);
	}

	/** Text mit Violett-Cyan-Farbverlauf ueber die Buchstaben. */
	public static void gradientText(GuiGraphicsExtractor g, String s, int x,
		int y, double phase)
	{
		int cx = x;
		int n = Math.max(1, s.length() - 1);
		for(int i = 0; i < s.length(); i++)
		{
			String c = String.valueOf(s.charAt(i));
			int color = Theme.accent(phase + i * 0.35 + 0 * n);
			g.text(MC.font, c, cx, y, color, true);
			cx += MC.font.width(c);
		}
	}
}
