package net.sf.l2j.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Shutdown;
import net.sf.l2j.gameserver.Shutdown.Savable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;


public class ChatbanManager implements Savable
{
	private static final String GET = "SELECT type, char_id, expiration FROM character_chatban;";
	
	private static final String ADD = "INSERT INTO character_chatban (type, char_id, expiration) VALUES (?,?,?) ON DUPLICATE KEY"
										+ "UPDATE character_chatban SET expiration=? WHERE type=? AND char_id=?";
	
	private final HashMap<ChatType, HashMap<Integer, Long>> _expirations = new HashMap<>();
	
	public ChatbanManager()
	{
		_expirations.put(ChatType.HERO, new HashMap<Integer, Long>());
		_expirations.put(ChatType.TRADE, new HashMap<Integer, Long>());
		_expirations.put(ChatType.GLOBAL, new HashMap<Integer, Long>());
		loadFromDB();
		Shutdown.addShutdownHook(this);
		System.out.println("Loaded "  + _expirations.get(ChatType.HERO).size() + " hero chat bans.");
		System.out.println("Loaded "  + _expirations.get(ChatType.TRADE).size() + " trade chat bans.");
		System.out.println("Loaded "  + _expirations.get(ChatType.GLOBAL).size() + " global chat bans.");
	}
	
	public boolean isChatBanned(L2PcInstance player, ChatType type)
	{	final long curtime = System.currentTimeMillis();
		final long expirat = getExpiration(player, type);
		if (expirat < curtime)
			return false;
		final long wait = expirat - curtime;
		player.sendMessage("Your recent behaviour made an admin issue a ban chat on this channel. Please consider that this channel has rules that all players have to follow. If you want to express your opinion make a topic in the forums!");
		player.sendMessage("The " + type.toString().toLowerCase() + " chat ban will be lifted in " + wait / 60000 + " minutes.".replace("0 minutes", "a few seconds"));
		return true;
	}
	
	public void setExpiration(L2PcInstance player, int type, int minutes)
	{	_expirations.get(ChatType.values()[type]).put(player.getObjectId(), System.currentTimeMillis() + minutes * 60000);
	}
	
	private long getExpiration(L2PcInstance player, ChatType type)
	{	return _expirations.get(type).getOrDefault(player.getObjectId(), 0L);
	}
	
	private void loadFromDB()
	{
		try(Connection con = L2DatabaseFactory.getInstance().getConnection();Statement st = con.createStatement();ResultSet rs = st.executeQuery(GET))
		{
			while (rs.next())
			{
				final HashMap<Integer, Long> temp = _expirations.get(ChatType.values()[rs.getInt(1)]);
				temp.put(rs.getInt(2), rs.getLong(3));
			}
		}
		catch (Exception e)
		{	e.printStackTrace();
		}
	}
	
	@Override
	public void store()
	{
		try(Connection con = L2DatabaseFactory.getInstance().getConnection();PreparedStatement st = con.prepareStatement(ADD))
		{
			for (Map.Entry<ChatType, HashMap<Integer, Long>> entry : _expirations.entrySet())
			{	final int chatType = entry.getKey().ordinal();
				final HashMap<Integer, Long> temp = entry.getValue();
				for (Map.Entry<Integer, Long> entry2 : temp.entrySet())
				{	
					final int charId = entry2.getKey();
					final long expir = entry2.getValue();
					st.clearParameters();
					st.setInt(1, chatType);
					st.setInt(2, charId);
					st.setLong(3, expir);
					st.setLong(4, expir);
					st.setInt(5, chatType);
					st.setInt(6, charId);
					st.executeUpdate();
				}
			}
		}
		catch (Exception e)
		{	e.printStackTrace();
		}
	}
	
	private static class InstanceHolder
	{
		private static final ChatbanManager instance = new ChatbanManager();
	}
	
	public static ChatbanManager getInstance()
	{
		return InstanceHolder.instance;
	}
	
	public enum ChatType
	{	
		HERO,
		TRADE,
		GLOBAL,
	}
}