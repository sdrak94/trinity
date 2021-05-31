package net.sf.l2j.gameserver.skills.l2skills;

import java.util.logging.Level;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class L2SkillTeleport extends L2Skill
{
private final String _recallType;
private final Location _loc;

public L2SkillTeleport(StatsSet set)
{
	super(set);
	
	_recallType = set.getString("recallType", "");
	String coords = set.getString("teleCoords", null);
	if (coords != null)
	{
		String[] valuesSplit = coords.split(",");
		_loc = new Location(Integer.parseInt(valuesSplit[0]),
				Integer.parseInt(valuesSplit[1]),
				Integer.parseInt(valuesSplit[2]));
	}
	else
		_loc = null;
}

@Override
public void useSkill(L2Character activeChar, L2Object[] targets)
{
	if (activeChar instanceof L2PcInstance)
	{
		// Thanks nbd
		if (activeChar.isInFunEvent() || !TvTEvent.onEscapeUse(((L2PcInstance) activeChar).getObjectId()))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (activeChar.isAfraid())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (((L2PcInstance)activeChar).isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}
		
		if (GrandBossManager.getInstance().getZone(activeChar) != null && !activeChar.isGM())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
			return;
		}
	}
	
	try
	{
		for (L2Character target: (L2Character[]) targets)
		{
			if (target instanceof L2PcInstance)
			{
				L2PcInstance targetChar = (L2PcInstance) target;
				
				// Check to see if the current player target is in a festival.
				if (targetChar.isFestivalParticipant())
				{
					targetChar.sendMessage("You may not use an escape skill in a festival.");
					continue;
				}
				
				// Check to see if player is in jail
				if (targetChar.isInJail())
				{
					targetChar.sendMessage("You can not escape from jail.");
					continue;
				}
				
				// Check to see if player is in a duel
				if (targetChar.isInDuel())
				{
					targetChar.sendMessage("You cannot use escape skills during a duel.");
					continue;
				}
				
				if (targetChar != activeChar)
				{
					if (targetChar.isInFunEvent() || !TvTEvent.onEscapeUse(targetChar.getObjectId()))
						continue;
					
					if (targetChar.isInOlympiadMode())
						continue;
					
					if (GrandBossManager.getInstance().getZone(targetChar) != null)
						continue;
				}
			}
			Location loc = null;
			if (getSkillType() == L2SkillType.TELEPORT)
			{
				if (_loc != null)
				{
					// target is not player OR player is not flying or flymounted
					// TODO: add check for gracia continent coords
					if (!(target instanceof L2PcInstance)
							|| !(target.isFlying() || ((L2PcInstance)target).isFlyingMounted()))
						loc = _loc;
				}
			}
			else
			{
				if (_recallType.equalsIgnoreCase("Castle"))
					loc = MapRegionTable.getInstance().getTeleToLocation(target, MapRegionTable.TeleportWhereType.Castle);
				else if (_recallType.equalsIgnoreCase("ClanHall"))
					loc = MapRegionTable.getInstance().getTeleToLocation(target, MapRegionTable.TeleportWhereType.ClanHall);
				else if (_recallType.equalsIgnoreCase("Fortress"))
					loc = MapRegionTable.getInstance().getTeleToLocation(target, MapRegionTable.TeleportWhereType.Fortress);
				else
					loc = MapRegionTable.getInstance().getTeleToLocation(target, MapRegionTable.TeleportWhereType.Town);
			}
			if (loc != null)
			{
				target.setInstanceId(0);
				if (target instanceof L2PcInstance)
					((L2PcInstance)target).setIsIn7sDungeon(false);
				target.teleToLocation(loc, true);
			}
		}
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "", e);
	}
}
}