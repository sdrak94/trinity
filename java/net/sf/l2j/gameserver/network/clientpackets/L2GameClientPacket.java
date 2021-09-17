package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import org.mmocore.network.ReceivablePacket;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

public abstract class L2GameClientPacket extends ReceivablePacket<L2GameClient>
{
protected static final Logger _log = Logger.getLogger(L2GameClientPacket.class.getName());

@Override
protected boolean read()
{
	//_log.info(this.getType());
	try
	{
		readImpl();
		return true;
	}
	catch (Exception e)
	{
		_log.severe("Client: "+getClient().toString()+" - Failed reading: "+getType()+" - L2J Server Version: "+Config.SERVER_VERSION+" - DP Revision: "+Config.DATAPACK_VERSION);
		e.printStackTrace();
	}
	return false;
}

protected abstract void readImpl();

@Override
public void run()
{
	try
	{
		runImpl();
		
		/* Removes onspawn protection - player has faster computer than average
		 * 
		 * True for these packets:
		 * RequestActionUse
		 * RequestMagicSkillUse
		 * 
		 * it could include pickup and talk too, but less is better
		 */
		
		if (getClient().getActiveChar() != null && triggersOnActionRequest())
			getClient().getActiveChar().onActionRequest();
	}
	catch (Throwable t)
	{
		_log.severe("Client: "+getClient().toString()+" - Failed running: "+getType()+" - L2J Server Version: "+Config.SERVER_VERSION+" - DP Revision: "+Config.DATAPACK_VERSION);
		t.printStackTrace();
	}
}

protected abstract void runImpl();

protected final void sendPacket(L2GameServerPacket gsp)
{
	getClient().sendPacket(gsp);
}

/**
 * @return A String with this packet name for debugging purposes
 */
public abstract String getType();

/**
 * Overridden with true value on some packets that should disable spawn protection
 */
protected boolean triggersOnActionRequest()
{
	return false;
}
}
