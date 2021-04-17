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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.Collection;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbnormalEffect;

/**
 * This class handles following admin commands:
 * - heal = restores HP/MP/CP on target, name or radius
 *
 * @version $Revision: 1.2.4.5 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminHeal implements IAdminCommandHandler
{
	private static Logger			_log			= Logger.getLogger(AdminRes.class.getName());
	private static final String[]	ADMIN_COMMANDS	=
	{
		"admin_heal", "admin_heal_pt", "admin_sendhome_pt", "admin_reuse_pt"
	};
	
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_heal"))
			handleRes(activeChar);
		else if (command.startsWith("admin_heal"))
		{
			try
			{
				String healTarget = command.substring(11);
				handleRes(activeChar, healTarget);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				if (Config.DEVELOPER)
					_log.warning("Heal error: " + e);
				activeChar.sendMessage("Incorrect target/radius specified.");
			}
		}
		else if (command.equals("admin_heal_pt"))
		{
			try
			{
				String healTarget = command.substring(14);
				handleResPt(activeChar, healTarget);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				if (Config.DEVELOPER)
					_log.warning("Heal error: " + e);
				activeChar.sendMessage("Incorrect target/radius specified.");
			}
		}
		else if (command.equals("admin_sendhome_pt"))
		{
			try
			{
				String healTarget = command.substring(11);
				HandleSendHomePt(activeChar, healTarget);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				if (Config.DEVELOPER)
					_log.warning("Heal error: " + e);
				activeChar.sendMessage("Incorrect target/radius specified.");
			}
		}
		else if (command.equals("admin_reuse_pt"))
		{
			try
			{
				String healTarget = command.substring(11);
				HandleReusePt(activeChar, healTarget);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				if (Config.DEVELOPER)
					_log.warning("Heal error: " + e);
				activeChar.sendMessage("Incorrect target/radius specified.");
			}
		}
		return true;
	}
	
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleRes(L2PcInstance activeChar)
	{
		handleRes(activeChar, null);
	}
	
	private void handleRes(L2PcInstance activeChar, String player)
	{
		L2Object obj = activeChar.getTarget();
		if (player != null)
		{
			L2PcInstance plyr = L2World.getInstance().getPlayer(player);
			if (plyr != null)
				obj = plyr;
			else
			{
				try
				{
					int radius = Integer.parseInt(player);
					Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
					// synchronized (activeChar.getKnownList().getKnownObjects())
					{
						for (L2Object object : objs)
						{
							if (object instanceof L2Character)
							{
								L2Character character = (L2Character) object;
								character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());
								if (object instanceof L2PcInstance)
									character.setCurrentCp(character.getMaxCp());
							}
						}
					}
					activeChar.sendMessage("Healed within " + radius + " unit radius.");
					return;
				}
				catch (NumberFormatException nbe)
				{}
			}
		}
		if (obj == null)
			obj = activeChar;
		if (obj instanceof L2Character)
		{
			L2Character target = (L2Character) obj;
			target.setCurrentHpMp(target.getMaxHp(), target.getMaxMp());
			if (target instanceof L2PcInstance)
				target.setCurrentCp(target.getMaxCp());
			if (Config.DEBUG)
				_log.fine("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") healed character " + target.getName());
		}
		else
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
	}
	
	private void handleResPt(L2PcInstance activeChar, String player)
	{
		L2Object obj = activeChar.getTarget();
		if (!(obj instanceof L2PcInstance))
		{
			return;
		}
		if (player != null)
		{
			for (L2PcInstance p : obj.getActingPlayer().getParty().getPartyMembers())
			{
				p.setCurrentHpMp(p.getMaxHp(), p.getMaxMp());
				p.setCurrentCp(p.getMaxCp());
				activeChar.sendMessage("Healed " + p.getName());
				return;
			}
		}
		else
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
	}
	
	private void HandleSendHomePt(L2PcInstance activeChar, String player)
	{
		L2Object obj = activeChar.getTarget();
		if (player != null)
		{
			L2PcInstance plyr = L2World.getInstance().getPlayer(player);
			if (plyr != null)
				obj = plyr;
			else
			{
				try
				{
					Collection<L2PcInstance> objs = obj.getActingPlayer().getParty().getPartyMembers();
					// synchronized (activeChar.getKnownList().getKnownObjects())
					{
						for (L2Object object : objs)
						{
							if (object instanceof L2Character)
							{
								L2Character character = (L2Character) object;
								character.teleToLocation(TeleportWhereType.Town);
								character.stopAbnormalEffect(AbnormalEffect.HOLD_1);
								character.setIsParalyzed(false);
								activeChar.sendMessage("Sent home " + character.getName());
							}
						}
					}
					return;
				}
				catch (NumberFormatException nbe)
				{}
			}
		}
		else
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
	}
	
	private void HandleReusePt(L2PcInstance activeChar, String player)
	{
		L2Object obj = activeChar.getTarget();
		if (player != null)
		{
			L2PcInstance plyr = L2World.getInstance().getPlayer(player);
			if (plyr != null)
				obj = plyr;
			else
			{
				try
				{
					Collection<L2PcInstance> objs = obj.getActingPlayer().getParty().getPartyMembers();
					// synchronized (activeChar.getKnownList().getKnownObjects())
					{
						for (L2Object object : objs)
						{
							if (object instanceof L2Character)
							{
								L2Character character = (L2Character) object;
								for (L2Skill skill : character.getAllSkills())
									character.enableSkill(skill.getId());
								character.sendPacket(new SkillCoolTime(character.getActingPlayer()));
								activeChar.sendMessage("Reused: " + character.getName());
							}
						}
					}
					return;
				}
				catch (NumberFormatException nbe)
				{}
			}
		}
		else
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
	}
}
