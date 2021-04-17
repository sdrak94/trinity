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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.GMHennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.GMViewCharacterInfo;
import net.sf.l2j.gameserver.network.serverpackets.GMViewItemList;
import net.sf.l2j.gameserver.network.serverpackets.GMViewPledgeInfo;
import net.sf.l2j.gameserver.network.serverpackets.GMViewSkillInfo;
import net.sf.l2j.gameserver.network.serverpackets.GMViewWarehouseWithdrawList;
import net.sf.l2j.gameserver.network.serverpackets.GmViewQuestInfo;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.2.2.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestGMCommand extends L2GameClientPacket
{
	private static final String _C__6E_REQUESTGMCOMMAND = "[C] 6e RequestGMCommand";
	static Logger _log = Logger.getLogger(RequestGMCommand.class.getName());

	private String _targetName;
	private int _command;


	@Override
	protected void readImpl()
	{
		_targetName = readS();
		_command    = readD();
		//_unknown  = readD();
	}

	@Override
	protected void runImpl()
	{
		// prevent non gm or low level GMs from vieweing player stuff
		if (!getClient().getActiveChar().isGM() || !getClient().getActiveChar().getAccessLevel().allowAltG())
			return;

		L2PcInstance player = L2World.getInstance().getPlayer(_targetName);

		// player name was incorrect?
		if (player == null)
			return;

		switch(_command)
		{
		    case 1: // player status
		    {
		        sendPacket(new GMViewCharacterInfo(player));
                sendPacket(new GMHennaInfo(player));
		        break;
		    }
		    case 2: // player clan
		    {
		        if (player.getClan() != null)
		            sendPacket(new GMViewPledgeInfo(player.getClan(),player));
		        break;
		    }
		    case 3: // player skills
		    {
		        sendPacket(new GMViewSkillInfo(player));
		        break;
		    }
		    case 4: // player quests
		    {
		        sendPacket(new GmViewQuestInfo(player));
		        break;
		    }
		    case 5: // player inventory
		    {
		        sendPacket(new GMViewItemList(player));
		        break;
		    }
		    case 6: // player warehouse
		    {
		        // gm warehouse view to be implemented
		        sendPacket(new GMViewWarehouseWithdrawList(player));
		        break;
		    }
		    
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__6E_REQUESTGMCOMMAND;
	}
}
