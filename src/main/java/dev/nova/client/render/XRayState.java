package dev.nova.client.render;

import java.util.Set;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gemeinsamer Zustand fuer X-Ray. Wird von den Mixins (Chunk-Compiler-Threads
 * und Render-Thread) gelesen, deshalb alles volatile und unveraenderlich.
 */
public final class XRayState
{
	private static volatile boolean active;
	private static volatile Set<Block> visible = Set.of();

	/** Werden von den Mixins gesetzt, damit der Hack pruefen kann, ob sie greifen. */
	public static volatile boolean faceHookRan;
	public static volatile boolean shapeHookRan;
	public static volatile boolean cameraHookRan;

	private XRayState()
	{}

	public static boolean isActive()
	{
		return active;
	}

	/** True, wenn der Block bei aktivem X-Ray versteckt werden soll. */
	public static boolean isHidden(BlockState state)
	{
		return active && !visible.contains(state.getBlock());
	}

	public static void activate(Set<Block> visibleBlocks)
	{
		visible = Set.copyOf(visibleBlocks);
		faceHookRan = false;
		shapeHookRan = false;
		cameraHookRan = false;
		active = true;
	}

	public static void deactivate()
	{
		active = false;
	}
}
