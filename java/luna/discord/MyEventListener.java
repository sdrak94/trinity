package luna.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MyEventListener extends ListenerAdapter
{
	public void onMessageReceived(MessageReceivedEvent event)
	{
		if (event.getAuthor().isBot())
			return;
		Message message = event.getMessage();
		String content = message.getContentRaw();
		MessageChannel channel = event.getChannel();
		
		if (content.equalsIgnoreCase("!mana"))
		{
			channel.sendMessage("MIA MANA EINAI POUTANA");
			channel.sendMessage("H AGGELIKI POU EXEI TA BYZIA MEGALA");
		}
	}
}
