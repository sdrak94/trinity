package net.sf.l2j.gameserver.model.actor;

import java.util.Collection;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2DecoyInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.chars.L2CharTemplate;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Weapon;

public abstract class L2Decoy extends L2Character
{
private final L2PcInstance _owner;
public static L2Skill decoyHate = SkillTable.getInstance().getInfo(5272, 1);
public static L2Skill twinShot = SkillTable.getInstance().getInfo(507, 37);
public static L2Skill greaterHeal = SkillTable.getInstance().getInfo(1217, 33);
public static L2Skill battleHeal = SkillTable.getInstance().getInfo(1015, 15);

public static L2Skill mobdeathspike = SkillTable.getInstance().getInfo(9051, 1);
public static L2Skill cursefear = SkillTable.getInstance().getInfo(1169, 14);
public static L2Skill anchor = SkillTable.getInstance().getInfo(1170, 13);
public static L2Skill curseofdoom = SkillTable.getInstance().getInfo(1336, 1);


public static L2Skill GreaterGroupHeal = SkillTable.getInstance().getInfo(1219, 33);
public static L2Skill MajorGroupHeal = SkillTable.getInstance().getInfo(1402, 5);
public static L2Skill BalanceLife = SkillTable.getInstance().getInfo(1335, 1);
public static L2Skill BodyOfAvatar = SkillTable.getInstance().getInfo(1311, 6);

public L2Decoy(int objectId, L2CharTemplate template, L2PcInstance owner)
{
	super(objectId, template);
	_owner = owner;
	setXYZInvisible(owner.getX(), owner.getY(), owner.getZ());
	setIsInvul(false);
}

@Override
public void onSpawn()
{
	super.onSpawn();
	if (this instanceof L2DecoyInstance)
		getOwner().sendPacket(new AbstractNpcInfo.DecoyInfo(this));
	else
		getOwner().sendPacket(new AbstractNpcInfo.TowerInfo(this, getOwner(), 2));
}

@Override
public void onAction(L2PcInstance player)
{
	if (player == null)
		return;
	
	// See description in TvTEvent.java
	if (!TvTEvent.onAction(player, _owner.getObjectId()))
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	// Check if the L2PcInstance is confused
	if (player.isOutOfControl())
	{
		// Send a Server->Client packet ActionFailed to the player
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (((TvT._started && !Config.TVT_ALLOW_INTERFERENCE) || (NewTvT._started && !Config.TVT_ALLOW_INTERFERENCE) || (FOS._started && !Config.FortressSiege_ALLOW_INTERFERENCE) || (NewFOS._started && !Config.FortressSiege_ALLOW_INTERFERENCE) || (CTF._started && !Config.CTF_ALLOW_INTERFERENCE) || (NewCTF._started && !Config.CTF_ALLOW_INTERFERENCE)
			|| ((player._inEventTvTi || _owner._inEventTvTi) && !Config.TVTI_ALLOW_INTERFERENCE) || (DM._started && !Config.DM_ALLOW_INTERFERENCE) || (NewDM._started && !Config.DM_ALLOW_INTERFERENCE) || (VIP._started && !Config.VIP_ALLOW_INTERFERENCE)) && !player.isGM())
	{
		if ((_owner._inEventTvT && !player._inEventTvT) || (!_owner._inEventTvT && player._inEventTvT))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		else if ((_owner._inEventHG && !player._inEventHG) || (!_owner._inEventHG && player._inEventHG))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if ((_owner._inEventFOS && !player._inEventFOS) || (!_owner._inEventFOS && player._inEventFOS))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if ((_owner._inEventCTF && !player._inEventCTF) || (!_owner._inEventCTF && player._inEventCTF))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if ((_owner._inEventDM && !player._inEventDM) || (!_owner._inEventDM && player._inEventDM))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if ((_owner._inEventTvTi && !player._inEventTvTi) || (!_owner._inEventTvTi && player._inEventTvTi))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if ((_owner._inEventVIP && !player._inEventVIP) || (!_owner._inEventVIP && player._inEventVIP))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
	}
	
	// Aggression target lock effect
	if (player.isLockedTarget() && player.getLockedTarget() != this)
	{
		player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	// Check if the player already target this L2PcInstance
	if (player.getTarget() != this)
	{
		if (player.isSelectingTarget())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.setIsSelectingTarget(3);
		
		// Set the target of the player
		player.setTarget(this);
		
		// Send a Server->Client packet MyTargetSelected to the player
		// The color to display in the select window is White
		player.sendPacket(new MyTargetSelected(getObjectId(), 0));
		
		if (player != _owner)
			player.sendPacket(new ValidateLocation(this));
	}
	else
	{
		if (player != _owner)
			player.sendPacket(new ValidateLocation(this));
		
		if (isDead())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		// Check if this L2PcInstance is autoAttackable
		if (isAutoAttackable(player))
		{
			// Player with lvl < 21 can't attack a cursed weapon holder
			// And a cursed weapon holder can't attack players with lvl < 21
			if ((_owner.isCursedWeaponEquipped() && player.getLevel() < 50) || (player.isCursedWeaponEquipped() && _owner.getLevel() < 50))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			else
			{
				if (Config.GEODATA > 0)
				{
					if (GeoData.getInstance().canSeeTarget(player, this))
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
					}
					else
						player.sendPacket(ActionFailed.STATIC_PACKET);
				}
				else
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				}
			}
		}
		else
		{
			// This Action Failed packet avoids player getting stuck when clicking three or
			// more times
			player.sendPacket(ActionFailed.STATIC_PACKET);
			
			if (Config.GEODATA > 0)
			{
				if (GeoData.getInstance().canSeeTarget(player, this))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
				}
			}
			else
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
			}
		}
	}
}

@Override
public void onForcedAttack(final L2PcInstance attacker)
{
	final L2PcInstance defender = getActingPlayer();
	
	if (defender == null || isDead())
	{
		attacker.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (attacker.isInFunEvent())
	{
		if (defender.isInFunEvent())
		{
			if (!attacker.CanAttackDueToInEvent(defender))
			{
				attacker.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		else
		{
			attacker.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
	}
	else if (defender.isInFunEvent())
	{
		attacker.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (!defender.isAttackable(attacker))
	{
		attacker.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	if (isInsidePeaceZone(attacker))
	{
		// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
		attacker.sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
		attacker.sendPacket(ActionFailed.STATIC_PACKET);
	}
	else if (!defender.isAttackable() && !attacker.isGM())
	{
		attacker.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	else if (attacker.isOutOfControl())
	{
		attacker.sendPacket(ActionFailed.STATIC_PACKET);
	}
	else if (attacker.isInOlympiadMode() || defender.isInOlympiadMode())
	{
		if (attacker.getOlympiadGameId() != defender.getOlympiadGameId())
		{
			attacker.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (!attacker.isOlympiadStart() || !defender.isOlympiadStart())
		{
			attacker.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
	}
	else
	{
		// GeoData Los Check or dz > 1000
		if (!GeoData.getInstance().canSeeTarget(attacker, this))
		{
			attacker.sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			attacker.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Notify AI with AI_INTENTION_ATTACK
		attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
	}
}

public boolean seeIfCanAttack(L2Character target)
{
	if (target == null || target == this || target == getActingPlayer() || target.isAlikeDead() || target.isInvul())
		return false;
	
	if (target.isInvisible() && (target.isGMReally() || !getActingPlayer().canSeeInvisiblePeople()))
		return false;
	
	if (!GeoData.getInstance().canSeeTarget(this, target))
		return false;
	
	if (!target.isAutoAttackable(getActingPlayer()))
		return false;
	
	if (target instanceof L2Playable)
	{
		if (!getActingPlayer().canAttack(target.getActingPlayer(), false))
			return false;
	}
	else
	{
		if (target instanceof L2Attackable)
		{
			if (!getActingPlayer().canAttackDueToSoloMob(target))
				return false;
		}
	}
	
	return true;
}

@Override
public void updateAbnormalEffect()
{
	Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
	{
		for (L2PcInstance player : plrs)
		{
			if (this instanceof L2DecoyInstance)
				player.sendPacket(new AbstractNpcInfo.DecoyInfo(this));
			else
				player.sendPacket(new AbstractNpcInfo.TowerInfo(this, player, 2));
		}
	}
}

public void stopDecay()
{
	DecayTaskManager.getInstance().cancelDecayTask(this);
}

@Override
public void onDecay()
{
	deleteMe(_owner);
}

@Override
public boolean isAutoAttackable(L2Character attacker)
{
	return _owner.isAutoAttackable(attacker);
}

@Override
public L2ItemInstance getActiveWeaponInstance()
{
	return _owner.getActiveWeaponInstance();
}

@Override
public L2Weapon getActiveWeaponItem()
{
	return _owner.getActiveWeaponItem();
}

@Override
public L2ItemInstance getSecondaryWeaponInstance()
{
	return _owner.getSecondaryWeaponInstance();
}

@Override
public L2Weapon getSecondaryWeaponItem()
{
	return _owner.getSecondaryWeaponItem();
}

public final int getNpcId()
{
	return getTemplate().npcId;
}
public final String getNpcName()
{
	return getTemplate().getName();
}
@Override
public int getLevel()
{
	return _owner.getLevel();
}

public void deleteMe(L2PcInstance owner)
{
	decayMe();
	getKnownList().removeAllKnownObjects();
	owner.setDecoy(null);
}

public synchronized void unSummon(L2PcInstance owner)
{
	if (isVisible() && !isDead())
	{
		if (getWorldRegion() != null)
			getWorldRegion().removeFromZones(this);
		owner.setDecoy(null);
		decayMe();
		getKnownList().removeAllKnownObjects();
	}
}

public final L2PcInstance getOwner()
{
	return _owner;
}

@Override
public L2PcInstance getActingPlayer()
{
	return _owner;
}

@Override
public L2NpcTemplate getTemplate()
{
	return (L2NpcTemplate) super.getTemplate();
}

@Override
public void sendInfo(L2PcInstance activeChar)
{
	if (this instanceof L2DecoyInstance)
		activeChar.sendPacket(new AbstractNpcInfo.DecoyInfo(this));
	else
		activeChar.sendPacket(new AbstractNpcInfo.TowerInfo(this, activeChar, 2));
}

@Override
public boolean isInFunEvent()
{
	return _owner.isInFunEvent();
}


@Override
public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
{
	if (miss || getOwner() == null)
		return;
	
	// Prevents the double spam of system messages, if the target is the owning player.
	if (target.getObjectId() != getOwner().getObjectId())
	{
		if (pcrit || mcrit)
			getOwner().sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_BY_SUMMONED_MOB));
		
		if (getOwner().isInOlympiadMode() &&
				target instanceof L2PcInstance &&
				((L2PcInstance)target).isInOlympiadMode() &&
				((L2PcInstance)target).getOlympiadGameId() == getOwner().getOlympiadGameId())
		{
			Olympiad.getInstance().notifyCompetitorDamage(getOwner(), damage, getOwner().getOlympiadGameId());
		}
		
		final SystemMessage sm;
		
		if (!target.isInvul())
		{
			if (target instanceof L2RaidBossInstance)
			{
				getOwner().incDamageCount(damage);
			}
			sm = new SystemMessage(SystemMessageId.SUMMON_GAVE_DAMAGE_S1);
			sm.addNumber(damage);
			getOwner().sendPacket(sm);
			//getOwner().sendMessage("Your Decoy made " + damage + " damage to " + target.getName());
		}
	}
}

@Override
public boolean isDisguised()
{
	return getActingPlayer().isDisguised();
}
}