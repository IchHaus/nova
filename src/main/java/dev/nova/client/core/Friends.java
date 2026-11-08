package dev.nova.client.core;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Freundesliste: Freunde werden von Killaura, ESP usw. anders behandelt. */
public final class Friends
{
	private static final Set<String> NAMES = new TreeSet<>();

	private Friends()
	{}

	public static boolean has(String name)
	{
		return name != null && NAMES.contains(name.toLowerCase(Locale.ROOT));
	}

	public static void add(String name)
	{
		NAMES.add(name.toLowerCase(Locale.ROOT));
	}

	public static boolean remove(String name)
	{
		return NAMES.remove(name.toLowerCase(Locale.ROOT));
	}

	public static Set<String> all()
	{
		return Collections.unmodifiableSet(NAMES);
	}

	public static void clear()
	{
		NAMES.clear();
	}
}
