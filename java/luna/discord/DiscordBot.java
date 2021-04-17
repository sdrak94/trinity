package luna.discord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.sf.l2j.gameserver.model.L2World;

public class DiscordBot
{
	String	system			= "769260216801689621";
	String	all_chat		= "769254954350542849";
	String	trade_chat		= "769258431642992671";
	String	shout_chat		= "769258713797623869";
	String	clan_chat		= "769258501049810954";
	String	party_chat		= "769258767334113400";
	String	announce_chat	= "769258557760864286";
	String	voice_chat		= "769258845595369554";
	String	marketplace		= "769262205518479384";

	TextChannel channelDefault;
	
	VoiceChannel channel;
	
	JDA api;
	
	public DiscordBot()
	{
		runCaptain();
		runCommander(system, "Initiated, Connected with Server. - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
	}
	
	public void runCaptain()
	{
		try
		{
			JDA apiTemp = new JDABuilder(AccountType.BOT).setToken("Njg5NzA0MzkwMzAyNjI5OTI5.XnHAJQ.pv_w99k2HBYibZe-6tJGMEvuNUg").build();
			api = apiTemp;
			api.awaitReady();
			channelDefault = api.awaitReady().getTextChannelById(all_chat);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void runCommander(String channelStr, String text)
	{
		try
		{
			String Channel_target = system;
			if (channelStr.equalsIgnoreCase("system"))
				Channel_target = system;
			if (channelStr.equalsIgnoreCase("all_chat"))
				Channel_target = all_chat;
			if (channelStr.equalsIgnoreCase("marketplace"))
				Channel_target = marketplace;
			if (channelStr.equalsIgnoreCase("trade_chat"))
				Channel_target = trade_chat;
			if (channelStr.equalsIgnoreCase("shout_chat"))
				Channel_target = shout_chat;
			if (channelStr.equalsIgnoreCase("clan_chat"))
				Channel_target = clan_chat;
			if (channelStr.equalsIgnoreCase("party_chat"))
				Channel_target = party_chat;
			if (channelStr.equalsIgnoreCase("announce_chat"))
				Channel_target = announce_chat;
			if (channelStr.equalsIgnoreCase("voice_chat"))
				Channel_target = voice_chat;
			TextChannel channel = api.awaitReady().getTextChannelById(Channel_target);
			channel.sendMessage(text).queue();
			updateOnlineCount();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void updateOnlineCount()
	{
		int online = 0;
		VoiceChannel serverOnline;
		VoiceChannel discordOnline;
		try
		{
			int IGonline = L2World.getInstance().getAllPlayersCount();
			if (IGonline != 0 && L2World.getInstance() != null)
			{
				online = IGonline;
			}
			serverOnline = api.awaitReady().getVoiceChannelById("769464406488252426");
			serverOnline.getManager().setName("ONLINE PLAYERS: " + online).queue();
			
			discordOnline = api.awaitReady().getVoiceChannelById("769468133554913290");
			discordOnline.getManager().setName("DISCORD USERS: " + getGuildUserCount(channelDefault.getGuild())).queue();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	public static int getGuildUserCount(Guild guild)
	{
		int i = 0;
		for (Member member : guild.getMembers())
		{
			if (!member.getUser().isBot())
			{
				i++;
			}
		}
		return i;
	}
	
	public static DiscordBot getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DiscordBot INSTANCE = new DiscordBot();
	}
}