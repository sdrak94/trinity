package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.FriendAddRequest;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestFriendInvite extends L2GameClientPacket
{
	private static final String _C__5E_REQUESTFRIENDINVITE = "[C] 5E RequestFriendInvite";
	private static Logger _log = Logger.getLogger(RequestFriendInvite.class.getName());

	private String _name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();

        if (activeChar == null)
            return;

        L2PcInstance friend = L2World.getInstance().getPlayer(_name);
        
        SystemMessage sm;
		Connection con = null;

    	if (friend == null)
        {
    	    //Target is not found in the game.
    	    sm = new SystemMessage(SystemMessageId.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME);
    	    activeChar.sendPacket(sm);
    	    sm = null;
    	    return;
    	}
        else if (friend == activeChar)
        {
    	    //You cannot add yourself to your own friend list.
    	    sm = new SystemMessage(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_OWN_FRIEND_LIST);
    	    activeChar.sendPacket(sm);
    	    sm = null;
    	    return;
    	}
        else if (friend.getAccessLevel().getLevel() > activeChar.getAccessLevel().getLevel())
        {
    	    //Target is not found in the game.
    	    sm = new SystemMessage(SystemMessageId.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME);
    	    activeChar.sendPacket(sm);
    	    sm = null;
    	    return;      	
        }
        else if (BlockList.isBlocked(friend, activeChar))
        {
        	activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME));
			return;        	
        }
        else if (friend.getPvpFlag() != 0 || activeChar.getPvpFlag() != 0)
        {   //cannot inv friend when fightings
    		activeChar.sendMessage("You cannot invite a friend when one of you is in PvP mode");
    		return;
        }
        else if ((friend.getTradeRefusal() && !activeChar.isGM()) || activeChar.isInJail())
        {
        	activeChar.sendMessage("Target is in interaction refusal mode");
            return;
        }
    	
        if (friend.isInOlympiadMode() || activeChar.isInOlympiadMode())
        {
        	activeChar.sendMessage("You or your target cant request trade in Olympiad mode");
            return;
        }
        
        if (friend.isInDuel() || activeChar.isInDuel())
        {
        	activeChar.sendMessage("You or your target cant request trade in dueling mode");
            return;
        }
    	
    	String name = friend.getName();
		try
		{
		    con = L2DatabaseFactory.getInstance().getConnection();
		    PreparedStatement statement = con.prepareStatement("SELECT charId FROM character_friends WHERE charId=? AND friendId=?");
		    statement.setInt(1, activeChar.getObjectId());
		    statement.setInt(2, friend.getObjectId());
		    ResultSet rset = statement.executeQuery();

            if (rset.next())
            {
    			//Player already is in your friendlist
    			sm = new SystemMessage(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST);
    			sm.addString(name);
		    }
            else
            {
		        if (!friend.isProcessingRequest())
		        {
		    	    //requets to become friend
    			    activeChar.onTransactionRequest(friend);
    			    sm = new SystemMessage(SystemMessageId.C1_REQUESTED_TO_BECOME_FRIENDS);
    			    sm.addString(name);
    			    FriendAddRequest ajf = new FriendAddRequest(activeChar.getName());
    			    friend.sendPacket(ajf);
    			}
                else
                {
    			    sm = new SystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
    			    sm.addString(name);
    			}
		    }

			activeChar.sendPacket(sm);
			sm = null;
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
		    _log.log(Level.WARNING, "could not add friend objectid: ", e);
		}
		finally
		{
		    try { con.close(); } catch (Exception e) {}
		}
	}

	@Override
	public String getType()
	{
		return _C__5E_REQUESTFRIENDINVITE;
	}
}