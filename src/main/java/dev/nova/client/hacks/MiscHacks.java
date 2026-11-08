package dev.nova.client.hacks;

import dev.nova.client.core.Category;
import dev.nova.client.core.Hack;

/** Sonstige Hacks. */
public final class MiscHacks
{
	private MiscHacks()
	{}

	public static final class FancyChat extends Hack
	{
		public FancyChat()
		{
			super("FancyChat", "Schreibt deine Chatnachrichten abwechselnd gross/klein.",
				Category.MISC);
		}

		@Override
		public String onChatSend(String message)
		{
			if(message.startsWith("/") || message.startsWith("."))
				return message;

			StringBuilder sb = new StringBuilder();
			boolean upper = false;
			for(char c : message.toCharArray())
			{
				if(Character.isLetter(c))
				{
					sb.append(upper ? Character.toUpperCase(c)
						: Character.toLowerCase(c));
					upper = !upper;
				}else
					sb.append(c);
			}

			return sb.toString();
		}
	}
}
