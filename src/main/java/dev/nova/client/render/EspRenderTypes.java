package dev.nova.client.render;

import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Die RenderTypes fuer ESP, werden beim ersten Zeichnen angelegt. */
public final class EspRenderTypes
{
	private static RenderType lines;
	private static RenderType quads;

	private EspRenderTypes()
	{}

	public static RenderType lines()
	{
		if(lines == null)
			lines = RenderType.create("novaclient:esp_lines",
				RenderSetup.builder(EspPipelines.ESP_LINES)
					.setLayeringTransform(
						LayeringTransform.VIEW_OFFSET_Z_LAYERING)
					.createRenderSetup());

		return lines;
	}

	public static RenderType quads()
	{
		if(quads == null)
			quads = RenderType.create("novaclient:esp_quads",
				RenderSetup.builder(EspPipelines.ESP_QUADS).sortOnUpload()
					.createRenderSetup());

		return quads;
	}
}
