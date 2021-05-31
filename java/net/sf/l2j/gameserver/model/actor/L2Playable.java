package net.sf.l2j.gameserver.model.actor;

import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.PlayableKnownList;
import net.sf.l2j.gameserver.model.actor.stat.PlayableStat;
import net.sf.l2j.gameserver.model.actor.status.PlayableStatus;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2CharTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

/**
 * This class represents all Playable characters in the world.<BR><BR>
 *
 * L2PlayableInstance :<BR><BR>
 * <li>L2PcInstance</li>
 * <li>L2Summon</li><BR><BR>
 *
 */

public abstract class L2Playable extends L2Character
{
private boolean _isNoblesseBlessed = false; 	// for Noblesse Blessing skill, restores buffs after death
private boolean _getCharmOfLuck = false; 		// Charm of Luck - During a Raid/Boss war, decreased chance for death penalty
private boolean _isPhoenixBlessed = false; 		// for Soul of The Phoenix or Salvation buffs
private boolean _isSilentMoving = false;		// Silent Move
private boolean _ProtectionBlessing = false;

private L2Character _lockedTarget = null;
private int _lockedTargetTicks = 0;

private boolean _ignorePK;

/**
 * Constructor of L2PlayableInstance (use L2Character constructor).<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Call the L2Character constructor to create an empty _skills slot and link copy basic Calculator set to this L2PlayableInstance </li><BR><BR>
 *
 * @param objectId Identifier of the object to initialized
 * @param template The L2CharTemplate to apply to the L2PlayableInstance
 *
 */
public L2Playable(int objectId, L2CharTemplate template)
{
	super(objectId, template);
	setIsInvul(false);
}

@Override
public PlayableKnownList getKnownList()
{
	return (PlayableKnownList)super.getKnownList();
}

@Override
public void initKnownList()
{
	setKnownList(new PlayableKnownList(this));
}

@Override
public PlayableStat getStat()
{
	return (PlayableStat)super.getStat();
}

@Override
public void initCharStat()
{
	setStat(new PlayableStat(this));
}

@Override
public PlayableStatus getStatus()
{
	return (PlayableStatus)super.getStatus();
}

@Override
public void initCharStatus()
{
	setStatus(new PlayableStatus(this));
}

public final boolean isNoblesseBlessed() { return _isNoblesseBlessed; }
public final void setIsNoblesseBlessed(boolean value) { _isNoblesseBlessed = value; }

public final void startNoblesseBlessing()
{
	setIsNoblesseBlessed(true);
	updateAbnormalEffect();
}

public final void stopNoblesseBlessing(L2Effect effect)
{
	if (effect == null)
		stopEffects(L2EffectType.NOBLESSE_BLESSING);
	else
		removeEffect(effect);
	
	setIsNoblesseBlessed(false);
	updateAbnormalEffect();
}

@Override
public boolean doDie(L2Character killer)
{
	// killing is only possible one time
	synchronized (this)
	{
		if (isDead())
			return false;
		// now reset currentHp to zero
		setCurrentHp(0);
		setIsDead(true);
	}
	
	// Set target to null and cancel Attack or Cast
	setTarget(null);
	// Stop movement
	stopMove(null);
	// Stop HP/MP/CP Regeneration task
	getStatus().stopHpMpRegeneration();
	
	// Stop all active skills effects in progress on the L2Character,
	// if the Character isn't affected by Soul of The Phoenix or Salvation
	if (!isInFunEvent())
	{
		if (getCharmOfLuck()) //remove Lucky Charm if player have Nobless blessing buff
			stopCharmOfLuck(null);
		
		if (isNoblesseBlessed())
			stopNoblesseBlessing(null);
	}
	
	// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
	broadcastStatusUpdate();
	
	if (getWorldRegion() != null)
		getWorldRegion().onDeath(this);
	
	// Notify Quest of L2Playable's death
	final L2PcInstance actingPlayer = getActingPlayer();
	for (QuestState qs : actingPlayer.getNotifyQuestOfDeath())
	{
		if (qs != null)
			qs.getQuest().notifyDeath((killer == null ? this : killer), this, qs);
	}
	
	final L2PcInstance player = killer.getActingPlayer();
	
	boolean awardPvp = true;
	
	/*		if (killer instanceof L2Summon && !player.getClassId().isSummoner())
			awardPvp = false;*/
	
	if (player != null/* && this instanceof L2PcInstance*/)
	{
		if (this instanceof L2Summon)
			awardPvp = false;
		
//		if (awardPvp)
//		{
//			if (!player.isInClanwarWith(actingPlayer))
//			{
//				if ((player.isInHuntersVillage() || actingPlayer.isInHuntersVillage()) && (player.getPvpKills() >= 3000 && ((float)actingPlayer.getPvpKills()/(float)player.getPvpKills() < 0.4 ||
//						player.getPvpKills() - actingPlayer.getPvpKills() >= 5500)))
//					awardPvp = false;
//				
//				if (awardPvp && ((player.isInGludin() || actingPlayer.isInGludin())) && (player.getPvpKills() >= 4500 && actingPlayer.getPvpKills() < player.getPvpKills()/2.5))
//					awardPvp = false;
//			}
//		}
		
		player.onKillUpdatePvPKarma(this, awardPvp);
	}
	
	// Notify L2Character AI
	getAI().notifyEvent(CtrlEvent.EVT_DEAD);
	
	return true;
}

public boolean checkIfPvP(L2Character target)
{
	if (target == null) return false;                                               // Target is null
	if (target == this) return false;                                               // Target is self
	if (!(target instanceof L2Playable)) return false;                      // Target is not a L2PlayableInstance
	
	final L2PcInstance player = getActingPlayer();
	
	if (player == null) return false;                                               // Active player is null
	if (player.getKarma() != 0) return false;                                       // Active player has karma
	
	final L2PcInstance targetPlayer = target.getActingPlayer();
	
	if (targetPlayer == null) return false;                                         // Target player is null
	if (targetPlayer == this) return false;                                         // Target player is self
	
	if (targetPlayer.getKarma() != 0) return false;                                 // Target player has karma

	if (targetPlayer.getPvpFlagLasts() < System.currentTimeMillis() - 2000 && !(targetPlayer.isInHuntersVillage()) || targetPlayer.isInPI() || targetPlayer.isInOrcVillage()) return false;
	
	return true;
}

@Override
public boolean isAttackable()
{
	return true;
}

public boolean isAttackable(L2Playable player)
{
	final L2PcInstance attacker = player.getActingPlayer();
	final L2PcInstance activeChar = getActingPlayer();
	
	if (attacker == null || activeChar == null) return false;
	if (attacker == activeChar) return true;
	
	if (activeChar.isInDuel())
	{
		if (attacker.isInDuel() && attacker.getDuelId() == activeChar.getDuelId())
		{
			if (attacker.getDuelState() == Duel.DUELSTATE_DUELLING)
				return true;
		}
		
		return false;
	}
	else if (attacker.isInDuel())
		return false;
	
	return true;
}

// Support for Soul of the Phoenix and Salvation skills
public final boolean isPhoenixBlessed() { return _isPhoenixBlessed; }
public final void setIsPhoenixBlessed(boolean value) { _isPhoenixBlessed = value; }

public final void startPhoenixBlessing()
{
	setIsPhoenixBlessed(true);
	updateAbnormalEffect();
}

public final void stopPhoenixBlessing(L2Effect effect)
{
	if (effect == null)
		stopEffects(L2EffectType.PHOENIX_BLESSING);
	else
		removeEffect(effect);
	
	setIsPhoenixBlessed(false);
	updateAbnormalEffect();
}

/**
 * Set the Silent Moving mode Flag.<BR><BR>
 */
public void setSilentMoving(boolean flag)
{
	_isSilentMoving = flag;
}

/**
 * Return True if the Silent Moving mode is active.<BR><BR>
 */
public boolean isSilentMoving()
{
	return _isSilentMoving;
}

// for Newbie Protection Blessing skill, keeps you safe from an attack by a chaotic character >= 10 levels apart from you
public final boolean getProtectionBlessing() { return _ProtectionBlessing; }
public final void setProtectionBlessing(boolean value) { _ProtectionBlessing = value; }
public void startProtectionBlessing()
{
	setProtectionBlessing(true);
	updateAbnormalEffect();
}
/**
 * @param blessing
 */
public void stopProtectionBlessing(L2Effect effect)
{
	if (effect == null)
		stopEffects(L2EffectType.PROTECTION_BLESSING);
	else
		removeEffect(effect);
	
	setProtectionBlessing(false);
	updateAbnormalEffect();
}

//Charm of Luck - During a Raid/Boss war, decreased chance for death penalty
public final boolean getCharmOfLuck() { return _getCharmOfLuck; }
public final void setCharmOfLuck(boolean value) { _getCharmOfLuck = value; }

public final void startCharmOfLuck()
{
	setCharmOfLuck(true);
	updateAbnormalEffect();
}

public final void stopCharmOfLuck(L2Effect effect)
{
	if (effect == null)
		stopEffects(L2EffectType.CHARM_OF_LUCK);
	else
		removeEffect(effect);
	
	setCharmOfLuck(false);
	updateAbnormalEffect();
}

@Override
public void updateEffectIcons(boolean partyOnly)
{
	_effects.updateEffectIcons(partyOnly);
}

public boolean isLockedTarget()
{
	return _lockedTarget != null && GameTimeController.getGameTicks() < _lockedTargetTicks;
}

public L2Character getLockedTarget()
{
	return _lockedTarget;
}

public void setLockedTarget(L2Character cha, int ticks)
{
	_lockedTarget = cha;
	_lockedTargetTicks = GameTimeController.getGameTicks() + ticks;
}

@Override
public final boolean isInFunEvent()
{
	final L2PcInstance _owner = getActingPlayer();
	
	if (_owner != null)
	{		
		return (_owner.atEvent || (TvT._started && _owner._inEventTvT) || (NewHuntingGrounds._started && _owner._inEventHG) || (NewTvT._started && _owner._inEventTvT) || (FOS._started && _owner._inEventFOS) || (NewFOS._started && _owner._inEventFOS) || (DM._started && _owner._inEventDM) || (NewDM._started && _owner._inEventDM) || (CTF._started && _owner._inEventCTF) || (NewCTF._started && _owner._inEventCTF || (NewDomination._started && _owner._inEventLunaDomi))
				|| (VIP._started && _owner._inEventVIP) || _owner._inEventTvTi || _owner._isInActiveKoreanRoom);
	}
	
	return false;
}

public final boolean isInActiveFunEvent()
{
	final L2PcInstance activeChar = getActingPlayer();
	
	if (TvT._started || NewTvT._started || NewHuntingGrounds._started || FOS._started || NewFOS._started || CTF._started || NewCTF._started || VIP._started || DM._started || NewDM._started || NewDomination._started) // when events started it's slightly different
	{
		if (activeChar._inEventTvT && NewTvT._started)
		{
			return true;
		}
		if (activeChar._inEventHG && NewHuntingGrounds._started)
		{
			return true;
		}
		if (activeChar._inEventFOS && NewFOS._started)
		{
			return true;
		}
		if (activeChar._inEventLunaDomi && NewDomination._started)
		{
			return true;
		}
		if (activeChar._inEventCTF && NewCTF._started)
		{
			return true;
		}
		if (activeChar._inEventDM && NewDM._started)
		{
			return true;
		}
	}
	return false;
}
/*public final boolean isInFunEventDisguised()
{
	final L2PcInstance _owner = getActingPlayer();
	
	if (_owner != null)
	{
		return (_owner.atEvent || (FOS._started && _owner._inEventFOS) || (CTF._started && _owner._inEventCTF) || (VIP._started && _owner._inEventVIP));
	}
	
	return false;
}*/

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
	if (!isAttackable(attacker))
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

public boolean isInUniqueInstance()
{
	return getInstanceId() >= 300000;
}

public boolean isInPartyWith(L2Character target)
{
	return (isInParty() && target.isInParty() && getParty().getPartyMembers().contains(target.getActingPlayer()));
}

public void setIgnorePK(boolean ignorePK)
{
	_ignorePK = ignorePK;
}

public boolean isIgnorePK()
{
	return _ignorePK;
}
@Override
public boolean isDisguised()
{
	return getActingPlayer().isDisguised();
}
}
