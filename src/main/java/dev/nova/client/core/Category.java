package dev.nova.client.core;

/** Kategorien im ClickGUI. */
public enum Category
{
	COMBAT("Combat", 0xFFEF4444),
	MOVEMENT("Movement", 0xFF22D3EE),
	RENDER("Render", 0xFFA78BFA),
	PLAYER("Player", 0xFF4ADE80),
	WORLD("World", 0xFFFACC15),
	MISC("Misc", 0xFFF472B6);

	public final String displayName;
	public final int color;

	Category(String displayName, int color)
	{
		this.displayName = displayName;
		this.color = color;
	}
}
