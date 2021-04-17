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
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.status.SiegeFlagStatus;
import net.sf.l2j.gameserver.model.entity.FortSiege;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2SiegeFlagInstance extends L2Npc
{
private L2Clan _clan;
private L2PcInstance _player;
private Siege _siege;
private FortSiege _fortSiege;
private final boolean _isAdvanced;
private boolean _canTalk;

public L2SiegeFlagInstance(L2PcInstance player, int objectId, L2NpcTemplate template, boolean advanced)
{
	super(objectId, template);
	
	_clan = player.getClan();
	_player = player;
	_canTalk = true;
	
	_isAdvanced = advanced;
	getStatus();
	setIsInvul(false);
	
	_siege = SiegeManager.getInstance().getSiege(_player);
	_fortSiege = FortSiegeManager.getInstance().getSiege(_player);
	
	if (_clan == null)
	{
		deleteMe();
	}
	else if (_siege != null)
	{
		L2SiegeClan sc = _siege.getAttackerClan(_clan);
		if (sc == null)
			deleteMe();
		else
			sc.addFlag(this);
	}
	else if (_fortSiege != null)
	{
		L2SiegeClan sc = _fortSiege.getAttackerClan(_clan);
		if (sc == null)
			deleteMe();
		else
			sc.addFlag(this);
	}
//	else if (!player.isInsideZone(L2Character.ZONE_HQ))
//	{
//		L2SiegeClan sc = _siege.getAttackerClan(_clan);
//		if (sc == null)
//			deleteMe();
//		else 
//			sc.addFlag(this);
//	}
}

/**
 * Use L2SiegeFlagInstance(L2PcInstance, int, L2NpcTemplate, boolean) instead
 */
@Deprecated
public L2SiegeFlagInstance(L2PcInstance player, int objectId, L2NpcTemplate template)
{
	super(objectId, template);
	_isAdvanced = false;
}

@Override
public boolean isAttackable()
{
	return true;
}

@Override
public boolean isAutoAttackable(L2Character attacker)
{
	return true;
}

@Override
public boolean doDie(L2Character killer)
{
	if (!super.doDie(killer))
		return false;
	if (_clan != null && (_siege != null || _fortSiege != null))
	{
		L2SiegeClan sc = _siege != null ? _siege.getAttackerClan(_clan) : _fortSiege.getAttackerClan(_clan);
		
		if (sc != null)
			sc.removeFlag(this);
	}
	return true;
}

@Override
public void onForcedAttack(L2PcInstance player)
{
	onAction(player);
}

@Override
public void onAction(L2PcInstance player)
{
	if (player == null || !canTarget(player))
		return;
	
	// Check if the L2PcInstance already target the L2NpcInstance
	if (this != player.getTarget())
	{
		// Set the target of the L2PcInstance player
		player.setTarget(this);
		
		// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
		MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
		player.sendPacket(my);
		
		// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int)getStatus().getCurrentHp() );
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp() );
		player.sendPacket(su);
		
		// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
		player.sendPacket(new ValidateLocation(this));
	}
	else
	{
		if (isAutoAttackable(player) && Math.abs(player.getZ() - getZ()) < 100)
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
		else
		{
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
}

public boolean isAdvancedHeadquarter()
{
	return _isAdvanced;
}

@Override
public SiegeFlagStatus getStatus()
{
	return (SiegeFlagStatus) super.getStatus();
}

@Override
public void initCharStatus()
{
	setStatus(new SiegeFlagStatus(this));
}

@Override
public void reduceCurrentHp(double damage, L2Character attacker, L2Skill skill)
{
	super.reduceCurrentHp(damage, attacker, skill);
	if(canTalk())
	{
		if (getCastle() != null && getCastle().getSiege().getIsInProgress())
		{
			if (_clan != null)
			{
				// send warning to owners of headquarters that theirs base is under attack
				_clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.BASE_UNDER_ATTACK));
				setCanTalk(false);
				ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTalkTask(), 20000);
			}
		}
		else if (getFort() != null && getFort().getSiege().getIsInProgress())
		{
			if (_clan != null)
			{
				// send warning to owners of headquarters that theirs base is under attack
				_clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.BASE_UNDER_ATTACK));
				setCanTalk(false);
				ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTalkTask(), 20000);
			}
		}
	}
}
private class ScheduleTalkTask implements Runnable
{

public ScheduleTalkTask() {}

public void run()
{
	setCanTalk(true);
}
}

void setCanTalk(boolean val)
{
	_canTalk = val;
}

private boolean canTalk()
{
	return _canTalk;
}
}
