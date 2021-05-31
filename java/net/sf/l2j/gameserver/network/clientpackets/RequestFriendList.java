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
package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestFriendList extends L2GameClientPacket
{
private static Logger _log = Logger.getLogger(RequestFriendList.class.getName());
private static final String _C__60_REQUESTFRIENDLIST = "[C] 60 RequestFriendList";

@Override
protected void readImpl()
{
	// trigger
}

@Override
protected void runImpl()
{
	L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar == null)
		return;
	
	SystemMessage sm;
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("SELECT friendId, friend_name FROM character_friends WHERE charId=?");
		statement.setInt(1, activeChar.getObjectId());
		
		ResultSet rset = statement.executeQuery();
		
		//======<Friend List>======
		activeChar.sendPacket(new SystemMessage(SystemMessageId.FRIEND_LIST_HEADER));
		
		L2PcInstance friend = null;
		while (rset.next())
		{
			String friendName = rset.getString("friend_name");
			friend = L2World.getInstance().getPlayer(rset.getInt("friendId"));
			
			if (friend == null)
			{
				//	(Currently: Offline)
				sm = new SystemMessage(SystemMessageId.S1_OFFLINE);
				sm.addString(friendName);
			}
			else
			{
				friendName = friend.getName();
				//(Currently: Online)
				sm = new SystemMessage(SystemMessageId.S1_ONLINE);
				sm.addString(friendName);
			}
			
			activeChar.sendPacket(sm);
		}
		
		//=========================
		activeChar.sendPacket(new SystemMessage(SystemMessageId.FRIEND_LIST_FOOTER));
		sm = null;
		rset.close();
		statement.close();
	}
	catch (Exception e) {
		_log.warning("Error in /friendlist for " + activeChar + ": " + e);
	}
	finally	{
		try {con.close();} catch (Exception e) {}
	}
}

@Override
public String getType()
{
	return _C__60_REQUESTFRIENDLIST;
}
}
