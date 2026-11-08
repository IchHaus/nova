package dev.nova.client.render;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Zeichenwerkzeug fuer 3D-Overlays (Boxen, Linien). Alle Koordinaten werden als
 * Weltkoordinaten uebergeben; die Umrechnung auf die Kamera passiert intern.
 */
public final class EspRenderer
{
	private final PoseStack pose;
	private final Vec3 camera;
	private final float partialTicks;
	private final EspBuffer buffer = new EspBuffer();

	public EspRenderer(PoseStack pose, Vec3 camera, float partialTicks)
	{
		this.pose = pose;
		this.camera = camera;
		this.partialTicks = partialTicks;
	}

	public Vec3 camera()
	{
		return camera;
	}

	public float partialTicks()
	{
		return partialTicks;
	}

	/** Zeichnet alles Gesammelte auf den Bildschirm. */
	public void flush()
	{
		buffer.uploadAndDraw();
	}

	// ---------------------------------------------------------------

	public void line(Vec3 a, Vec3 b, int color)
	{
		VertexConsumer vc = buffer.getBuffer(EspRenderTypes.lines());
		lineRel(vc, (float)(a.x - camera.x), (float)(a.y - camera.y),
			(float)(a.z - camera.z), (float)(b.x - camera.x),
			(float)(b.y - camera.y), (float)(b.z - camera.z), color);
	}

	/** Zeichnet die 12 Kanten einer Box. */
	public void box(AABB b, int color)
	{
		VertexConsumer vc = buffer.getBuffer(EspRenderTypes.lines());
		float x1 = (float)(b.minX - camera.x);
		float y1 = (float)(b.minY - camera.y);
		float z1 = (float)(b.minZ - camera.z);
		float x2 = (float)(b.maxX - camera.x);
		float y2 = (float)(b.maxY - camera.y);
		float z2 = (float)(b.maxZ - camera.z);

		// unten
		lineRel(vc, x1, y1, z1, x2, y1, z1, color);
		lineRel(vc, x2, y1, z1, x2, y1, z2, color);
		lineRel(vc, x2, y1, z2, x1, y1, z2, color);
		lineRel(vc, x1, y1, z2, x1, y1, z1, color);
		// oben
		lineRel(vc, x1, y2, z1, x2, y2, z1, color);
		lineRel(vc, x2, y2, z1, x2, y2, z2, color);
		lineRel(vc, x2, y2, z2, x1, y2, z2, color);
		lineRel(vc, x1, y2, z2, x1, y2, z1, color);
		// Seiten
		lineRel(vc, x1, y1, z1, x1, y2, z1, color);
		lineRel(vc, x2, y1, z1, x2, y2, z1, color);
		lineRel(vc, x2, y1, z2, x2, y2, z2, color);
		lineRel(vc, x1, y1, z2, x1, y2, z2, color);
	}

	/** Zeichnet eine halbtransparent gefuellte Box. */
	public void filledBox(AABB b, int color)
	{
		VertexConsumer vc = buffer.getBuffer(EspRenderTypes.quads());
		PoseStack.Pose entry = pose.last();
		float x1 = (float)(b.minX - camera.x);
		float y1 = (float)(b.minY - camera.y);
		float z1 = (float)(b.minZ - camera.z);
		float x2 = (float)(b.maxX - camera.x);
		float y2 = (float)(b.maxY - camera.y);
		float z2 = (float)(b.maxZ - camera.z);

		quad(vc, entry, color, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
		quad(vc, entry, color, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
		quad(vc, entry, color, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
		quad(vc, entry, color, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
		quad(vc, entry, color, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
		quad(vc, entry, color, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
	}

	/** Box mit Umrandung und leichter Fuellung. */
	public void boxWithFill(AABB b, int color, int fillAlpha)
	{
		filledBox(b, (fillAlpha & 0xFF) << 24 | color & 0x00FFFFFF);
		box(b, color);
	}

	// ---------------------------------------------------------------

	private void quad(VertexConsumer vc, PoseStack.Pose entry, int color,
		float ax, float ay, float az, float bx, float by, float bz, float cx,
		float cy, float cz, float dx, float dy, float dz)
	{
		vc.addVertex(entry, ax, ay, az).setColor(color);
		vc.addVertex(entry, bx, by, bz).setColor(color);
		vc.addVertex(entry, cx, cy, cz).setColor(color);
		vc.addVertex(entry, dx, dy, dz).setColor(color);
	}

	private void lineRel(VertexConsumer vc, float x1, float y1, float z1,
		float x2, float y2, float z2, int color)
	{
		PoseStack.Pose entry = pose.last();
		Vector3f normal = new Vector3f(x2, y2, z2).sub(x1, y1, z1);
		float length = normal.length();
		if(length < 1e-6F)
			return;

		normal.normalize();
		vc.addVertex(entry, x1, y1, z1).setColor(color)
			.setNormal(entry, normal).setLineWidth(2);

		// Wenn die Linie durch die Kamera geht, einen Zwischenpunkt setzen.
		// Das umgeht einen Fehler im Linien-Shader von Minecraft.
		float t = new Vector3f(x1, y1, z1).negate().dot(normal);
		if(t > 0 && t < length)
		{
			Vector3f mid = new Vector3f(normal).mul(t).add(x1, y1, z1);
			vc.addVertex(entry, mid).setColor(color).setNormal(entry, normal)
				.setLineWidth(2);
			vc.addVertex(entry, mid).setColor(color).setNormal(entry, normal)
				.setLineWidth(2);
		}

		vc.addVertex(entry, x2, y2, z2).setColor(color)
			.setNormal(entry, normal).setLineWidth(2);
	}
}
