package dev.nova.client.render;

import java.util.Optional;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Render-Pipelines fuer ESP/Tracer: wie die Vanilla-Debug-Pipelines, aber ohne
 * Tiefentest, damit alles durch Waende sichtbar ist.
 */
public final class EspPipelines
{
	private EspPipelines()
	{}

	/** Linien ohne Tiefentest. */
	public static final RenderPipeline ESP_LINES =
		RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
			.withLocation(Identifier.parse("novaclient:pipeline/esp_lines"))
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withCull(false)
			.withVertexBinding(0,
				DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
			.withPrimitiveTopology(PrimitiveTopology.LINES)
			.withDepthStencilState(Optional.empty()).build();

	/** Gefuellte Flaechen ohne Tiefentest. */
	public static final RenderPipeline ESP_QUADS =
		RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Identifier.parse("novaclient:pipeline/esp_quads"))
			.withDepthStencilState(Optional.empty()).withCull(false).build();
}
