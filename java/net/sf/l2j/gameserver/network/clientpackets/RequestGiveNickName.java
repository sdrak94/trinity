/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class RequestGiveNickName extends L2GameClientPacket
{
	static Logger	_log	= Logger.getLogger(RequestGiveNickName.class.getName());
	private String	_target;
	private String	_title;
	
	@Override
	protected void readImpl()
	{
		_target = readS();
		_title = readS();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		// Noblesse can bestow a title to themselves
		if (activeChar.isNoble() && _target.matches(activeChar.getName()))
		{
			if (activeChar.isChatBanned())
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (activeChar.isInSiege())
			{
				activeChar.sendMessage("Target is participating in a siege right now");
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			activeChar.setTitle(replaceInvalidChars(_title), true);
			SystemMessage sm = new SystemMessage(SystemMessageId.TITLE_CHANGED);
			activeChar.sendPacket(sm);
			activeChar.broadcastTitleInfo();
		}
		// Can the player change/give a title?
		else if ((activeChar.getClanPrivileges() & L2Clan.CP_CL_GIVE_TITLE) == L2Clan.CP_CL_GIVE_TITLE)
		{
			if (activeChar.getClan().getLevel() < 3)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_LVL_3_NEEDED_TO_ENDOWE_TITLE);
				activeChar.sendPacket(sm);
				sm = null;
				return;
			}
			L2ClanMember member1 = activeChar.getClan().getClanMember(_target);
			if (member1 != null)
			{
				L2PcInstance member = member1.getPlayerInstance();
				if (member != null)
				{
					if (member.isChatBanned())
					{
						activeChar.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					if (member.isInSiege())
					{
						activeChar.sendMessage("Target is participating in a siege right now");
						activeChar.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					// is target from the same clan?
					member.setTitle(replaceInvalidChars(_title), true);
					SystemMessage sm = new SystemMessage(SystemMessageId.TITLE_CHANGED);
					member.sendPacket(sm);
					member.broadcastTitleInfo();
					sm = null;
				}
				else
				{
					activeChar.sendMessage("Target needs to be online to get a title");
				}
			}
			else
			{
				activeChar.sendMessage("Target does not belong to your clan");
			}
		}
	}
	
	private String replaceInvalidChars(String title)
	{
		return title.replaceAll("/n", "").replaceAll("/r", "");
	}
	
	@Override
	public String getType()
	{
		return "[C] 55 RequestGiveNickName";
	}
}
