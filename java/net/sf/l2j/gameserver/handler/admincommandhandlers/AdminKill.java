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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2ControllableMobInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.util.GMAudit;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class handles following admin commands:
 * - kill = kills target L2Character
 * - kill_monster = kills target non-player
 *
 * - kill <radius> = If radius is specified, then ALL players only in that radius will be killed.
 * - kill_monster <radius> = If radius is specified, then ALL non-players only in that radius will be killed.
 *
 * @version $Revision: 1.2.4.5 $ $Date: 2007/07/31 10:06:06 $
 */
public class AdminKill implements IAdminCommandHandler
{
private static Logger _log = Logger.getLogger(AdminKill.class.getName());
private static final String[] ADMIN_COMMANDS =
{
	"admin_kill",
	"admin_kill_monster"
};

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	if (command.startsWith("admin_kill"))
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken(); // skip command
		
		if (st.hasMoreTokens())
		{
			String firstParam = st.nextToken();
			L2PcInstance plyr = L2World.getInstance().getPlayer(firstParam);
			if (plyr != null)
			{
				if (st.hasMoreTokens())
				{
					try
					{
						int radius = Integer.parseInt(st.nextToken());
						for (L2Character knownChar : plyr.getKnownList().getKnownCharactersInRadius(radius))
						{
							if (knownChar instanceof L2ControllableMobInstance || knownChar == activeChar)
								continue;
							
							kill(activeChar, knownChar);
						}
						
						activeChar.sendMessage("Killed all characters within a " + radius + " unit radius.");
						return true;
					}
					catch (NumberFormatException e)
					{
						activeChar.sendMessage("Invalid radius.");
						return false;
					}
				}
				else
				{
					kill(activeChar, plyr);
				}
			}
			else
			{
				try
				{
					int radius = Integer.parseInt(firstParam);
					
					for (L2Character knownChar : activeChar.getKnownList().getKnownCharactersInRadius(radius))
					{
						if (knownChar instanceof L2ControllableMobInstance || knownChar == activeChar)
							continue;
						kill(activeChar, knownChar);
					}
					
					activeChar.sendMessage("Killed all characters within a " + radius + " unit radius.");
					return true;
				}
				catch (NumberFormatException e)
				{
					activeChar.sendMessage("Usage: //kill <player_name | radius>");
					return false;
				}
			}
		}
		else
		{
			L2Object obj = activeChar.getTarget();
			if (obj instanceof L2ControllableMobInstance || !(obj instanceof L2Character))
				activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			else
				kill(activeChar, (L2Character) obj);
		}
	}
	return true;
}

private void kill(L2PcInstance activeChar, L2Character target)
{
	if (target instanceof L2PcInstance)
	{
		if (!((L2PcInstance) target).isGM())
			target.stopEffects(L2EffectType.INVINCIBLE);
		target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar, null);
	}
	else if (Config.L2JMOD_CHAMPION_ENABLE && target.isChampion())
		target.reduceCurrentHp(target.getMaxHp() * Config.L2JMOD_CHAMPION_HP + 1, activeChar, null);
	else
	{
		if(target.isInvul()) target.setIsInvul(false);
		target.reduceCurrentHp(target.getMaxHp() + 1, activeChar, null);
	}
	
	String partyMembers = "";
	
	if (target instanceof L2Attackable)
	{
		if (activeChar.getParty() != null)
		{
			for (L2PcInstance player : activeChar.getParty().getPartyMembers())
			{
				if (player == null) continue;
				
				if (player.isGM() || Util.calculateDistance(activeChar, player, true) > Math.max(Config.ALT_PARTY_RANGE, Config.ALT_PARTY_RANGE2))
					partyMembers = partyMembers + "("+player.getName()+"), ";
				else
					partyMembers = partyMembers + player.getName()+", ";
			}
		}
	}
	
	GMAudit.auditGMAction(activeChar.getAccountName() + " - " + activeChar.getName(), "Kill", target.getName(), partyMembers);
	
	if (Config.DEBUG)
		_log.fine("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ")" + " killed character " + target.getObjectId());
}

public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}
}
