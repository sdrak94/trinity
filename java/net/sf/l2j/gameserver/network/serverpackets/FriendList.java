/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.serverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Support for "Chat with Friends" dialog.
 *
 * Format: ch (hdSdh)
 * h: Total Friend Count
 *
 * h: Unknown
 * d: Player Object ID
 * S: Friend Name
 * d: Online/Offline
 * h: Unknown
 *
 * @author Tempy
 *
 */
public class FriendList extends L2GameServerPacket
{
private static Logger _log = Logger.getLogger(FriendList.class.getName());
private static final String _S__FA_FRIENDLIST = "[S] 75 FriendList";
private List<FriendStatus> _friends = new FastList<FriendStatus>();
private L2PcInstance _activeChar;

public FriendList(L2PcInstance character)
{
	_activeChar = character;
	getFriendList();
}

private static class FriendStatus
{
private final int _charId;
private final int _id;
private final String _name;
private final boolean _online;

public FriendStatus(int charId,int id, String name, boolean online)
{
	_charId = charId;
	_id = id;
	_name = name;
	_online = online;
}

/**
 * @return Returns the Char id. (first created player in-game will have id 1 and so on)
 */
 public int getCharId()
{
	return _charId;
}

/**
 * @return Returns the id.
 */
 public int getId()
 {
	 return _id;
 }
 
 /**
  * @return Returns the name.
  */
 public String getName()
 {
	 return _name;
 }
 
 /**
  * @return Returns the online.
  */
 public boolean isOnline()
 {
	 return _online;
 }
}

private void getFriendList()
{
	Connection con = null;
	
	try
	{
		String sqlQuery = "SELECT friendId, friend_name FROM character_friends WHERE " +
		"charId=" + _activeChar.getObjectId() + " ORDER BY friend_name ASC";
		
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement(sqlQuery);
		ResultSet rset = statement.executeQuery(sqlQuery);
		
		int friendId;
		String friendName;
		FriendStatus fs;
		while (rset.next())
		{
			friendId = rset.getInt("friendId");
			friendName = rset.getString("friend_name");
			
			if (friendId == _activeChar.getObjectId())
				continue;
			
			L2PcInstance friend = L2World.getInstance().getPlayer(friendId);
			
			if (friend != null)
				friendName = friend.getName();
			
			fs = new FriendStatus(0x00030b7a, friendId, friendName, friend != null);
			_friends.add(fs);
		}
		
		rset.close();
		statement.close();
	}
	catch (Exception e)
	{
		_log.warning("Error found in " + _activeChar.getName() + "'s FriendList: " + e);
	}
	finally
	{
		try {con.close();} catch (Exception e) {}
	}
}

@Override
protected final void writeImpl()
{
	writeC(0x75);
	writeD(_friends.size());
	for (FriendStatus fs : _friends)
	{
		writeD(fs.getCharId()); // character id
		writeS(fs.getName());
		writeD(fs.isOnline() ? 0x01 : 0x00); // online
		writeD(fs.isOnline() ? fs.getId() : 0x00); // object id if online
	}
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
 */
@Override
public String getType()
{
	return _S__FA_FRIENDLIST;
}
}
