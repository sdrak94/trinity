//package luna.discord;
//
//import net.dv8tion.jda.api.EmbedBuilder;
//import net.dv8tion.jda.api.JDA;
//import net.dv8tion.jda.api.entities.Invite.Channel;
//import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
//import net.dv8tion.jda.api.hooks.ListenerAdapter;
//import net.sf.l2j.gameserver.model.L2Effect;
//
//public class Commands extends ListenerAdapter
//{
//	public void onGuildMessageReceived(GuildMessageReceivedEvent event)
//	{
//		String[] args = event.getMessage().getContentRaw().split("\\s+");
//		if (args[0].equalsIgnoreCase(main.prefix + "event"))
//		{
//			
//			event.getChannel().sendMessage("@everyone").queue();
//			EmbedBuilder info = new EmbedBuilder();
//			info.setTitle("EVENT: RUSSIAN RULLETE");
//			info.setDescription("FRIDAY 20/03/20 @ 18:00 Registrations till 18:30.");
//			info.setColor(0xf45642);
//			info.setFooter("L2Trinity Team", event.getMember().getUser().getAvatarUrl());
//			event.getChannel().sendTyping().queue();
//			event.getChannel().sendMessage(info.build()).queue();
//			info.clear();
//		}
//		else if (args[0].equalsIgnoreCase(main.prefix + "pnik"))
//		{
//			
//			event.getChannel().sendMessage("@everyone").queue();
//			EmbedBuilder info = new EmbedBuilder();
//			info.setTitle("PNIKI PNIKI PNIKI");
//			info.setDescription("PNIKI PNIKI PNIKI PNIKI PNIKI PNIKI PNIKI PNIKI PNIKI.");
//			info.setColor(0xf45642);
//			info.setFooter("L2Trinity Team", event.getMember().getUser().getAvatarUrl());
//			event.getChannel().sendTyping().queue();
//			event.getChannel().sendMessage(info.build()).queue();
//			info.clear();
//		}
//	}
//
//	public void Shout(GuildMessageReceivedEvent event)
//	{
//		String[] args = event.getMessage().getContentRaw().split("\\s+");
//		if (args[0].equalsIgnoreCase(main.prefix + "event"))
//		{
//			
//			Channel channel = (Channel) JDA.getTextChannelsByName("general", true);
//			event.getChannel().sendMessage("@everyone").queue();
//			EmbedBuilder info = new EmbedBuilder();
//			info.setTitle("EVENT: RUSSIAN RULLETE");
//			info.setDescription("FRIDAY 20/03/20 @ 18:00 Registrations till 18:30.");
//			info.setColor(0xf45642);
//			info.setFooter("L2Trinity Team", event.getMember().getUser().getAvatarUrl());
//			event.getChannel().sendTyping().queue();
//			event.getChannel().sendMessage(info.build()).queue();
//			info.clear();
//		}
//		else if (args[0].equalsIgnoreCase(main.prefix + "pnik"))
//		{
//			
//			event.getChannel().sendMessage("@everyone").queue();
//			EmbedBuilder info = new EmbedBuilder();
//			info.setTitle("PNIKI PNIKI PNIKI");
//			info.setDescription("PNIKI PNIKI PNIKI PNIKI PNIKI PNIKI PNIKI PNIKI PNIKI.");
//			info.setColor(0xf45642);
//			info.setFooter("L2Trinity Team", event.getMember().getUser().getAvatarUrl());
//			event.getChannel().sendTyping().queue();
//			event.getChannel().sendMessage(info.build()).queue();
//			info.clear();
//		}
//	}
//}
