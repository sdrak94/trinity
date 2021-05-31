package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.stat.PcStat;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.effects.EffectStoneSkin;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class PcStatus extends PlayableStatus
{
private double _currentCp = 0; //Current CP of the L2PcInstance

public PcStatus(L2PcInstance activeChar)
{
	super(activeChar);
}

@Override
public final void reduceCp(int value)
{
	if (getCurrentCp() > value)
		setCurrentCp(getCurrentCp() - value);
	else
		setCurrentCp(0);
}

@Override
public final void reduceHp(double value, L2Character attacker)
{
	reduceHp(value, attacker, true, false, false, false);
}

@Override
public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean bypassCP)
{
	reduceHp(value, attacker, awake, isDOT, isHPConsumption, bypassCP, false);
}

public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean bypassCP, boolean isReflect)
{
	if (getActiveChar().isDead())
		return;

	if (getActiveChar().isInvul())
	{
		int mustGoThruDmg = 0;
		
		if (getActiveChar().isGM() || getActiveChar().isSpawnProtected() || isDOT || isHPConsumption)
		{}
		else
		{
			mustGoThruDmg = (int) (attacker.calcStat(Stats.DMG_ADD, 0, getActiveChar(), null) - getActiveChar().calcStat(Stats.DMG_REMOVE, 0, attacker, null) - getActiveChar().calcStat(Stats.DMG_REMOVE_SHIELD, 0, attacker, null));
		}
		
		if (mustGoThruDmg <= 0)
		{
			if (attacker == getActiveChar())
			{
				if (!isDOT && !isHPConsumption)
					return;
			}
			else
				return;
		}
		else
		{
			value = mustGoThruDmg;
		}
	}
	
	if (!isHPConsumption)
	{
		if (awake && getActiveChar().isSleeping())
			getActiveChar().stopSleeping(null);
		
		if (getActiveChar().isSitting())
			getActiveChar().standUp();
		
		if (getActiveChar().isFakeDeath())
			getActiveChar().stopFakeDeath(null);
		
		if (!isDOT)
		{
			if (getActiveChar().isStunned() && Rnd.get(11) <= 1)
				getActiveChar().stopStunning(null);
			if (getActiveChar().isAfraid() && Rnd.get(6) <= 1)
				getActiveChar().stopFear(null);
			if (getActiveChar().isImmobileUntilAttacked())
				getActiveChar().stopImmobileUntilAttacked(null);
		}
	}
	
	int fullValue = (int) value;
	int tDmg = 0;
	int manaDmg = 0;
	
	if (attacker != null && attacker != getActiveChar())
	{
		final L2PcInstance attackerPlayer = attacker.getActingPlayer();
		
		if (attackerPlayer != null)
		{
			if (attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
				return;
			
			if (getActiveChar().isInDuel())
			{
				if (getActiveChar().getDuelState() == Duel.DUELSTATE_DEAD)
					return;
				else if (getActiveChar().getDuelState() == Duel.DUELSTATE_WINNER)
					return;
				
				// cancel duel if player got hit by another player, that is not part of the duel
				if (attackerPlayer.isGM() && attackerPlayer.getDuelId() != getActiveChar().getDuelId())
					getActiveChar().setDuelState(Duel.DUELSTATE_INTERRUPTED);
			}
		}
		
		final EffectStoneSkin stoneskin = (EffectStoneSkin)getActiveChar().getFirstEffect(L2EffectType.STONESKIN);
		
		if (stoneskin != null)
		{
			final String name = stoneskin.getSkill().getName();
			
			value = stoneskin.addDamage((int) value);
			
			if (value > 0)
			{
				getActiveChar().sendMessage("Your "+name+" absorbed "+(int)(fullValue-value)+" damage from "+attacker.getDisplayName());
				attacker.sendMessage(getActiveChar().getDisplayName()+"'s "+name+" has absorbed "+(int)(fullValue-value)+" damage");
				fullValue = (int) value;
			}
			else //stoneskin fully absorbed damage
			{
				getActiveChar().sendMessage("Your "+name+" absorbed "+fullValue+" damage from "+attacker.getDisplayName());
				attacker.sendMessage(getActiveChar().getDisplayName()+"'s "+name+" has absorbed "+fullValue+" damage");
				return;
			}
		}
		
		
		// Check and calculate transfered damage
		final L2Summon summon = getActiveChar().getPet();
		//TODO correct range
		if (summon != null && summon instanceof L2SummonInstance && Util.checkIfInRange(900, getActiveChar(), summon, true))
		{
			tDmg = (int)value * (int)getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null) /100;
			
			// Only transfer dmg up to current HP, it should not be killed
			tDmg = Math.min((int)summon.getCurrentHp() - 1, tDmg);
			if (tDmg > 0)
			{
				summon.reduceCurrentHp(tDmg, attacker, null);
				value -= tDmg;
				fullValue = (int) value; // reduce the announced value here as player will get a message about summon damage
			}
		}
		
		final int transferManaPerc = (int) getActiveChar().calcStat(Stats.TRANSFER_DAMAGE_MANA, 0, attacker, null);
		
		if (transferManaPerc > 0 && value > 0)
		{
			manaDmg = (int) (value * transferManaPerc/100);
			manaDmg = (int) Math.min(getActiveChar().getCurrentMp(), manaDmg);
			
			if (manaDmg > 0)
			{
				getActiveChar().reduceCurrentMp(manaDmg);
				value -= manaDmg;
				fullValue = (int) value;
			}
		}
		
		if (!bypassCP)
		{
			boolean cp = false;
			
			if (attacker instanceof L2Playable || attacker instanceof L2Decoy)
			{
				if (attackerPlayer.calcStat(Stats.DAMAGE_STRAIGHT_TO_HP, 0, getActiveChar(), null) <= Rnd.get(100))
					cp = true;
			}
			else if (attacker.isAPC())
			{
				if (attacker.calcStat(Stats.DAMAGE_STRAIGHT_TO_HP, 0, getActiveChar(), null) <= Rnd.get(100))
					cp = true;
			}
			
			if (cp)
			{
				if (getCurrentCp() >= value)
				{
					setCurrentCp(getCurrentCp() - value);   // Set Cp to diff of Cp vs value
					value = 0;                              // No need to subtract anything from Hp
				}
				else
				{
					value -= getCurrentCp();                // Get diff from value vs Cp; will apply diff to Hp
					setCurrentCp(0, false);                        // Set Cp to 0
				}
			}
		}
		
		final boolean allow = !attacker.isInvisible() || (!attacker.isGMReally() && getActiveChar().canSeeInvisiblePeople());
		
		if (allow && !isReflect)
		{
			if (fullValue > 0)
			{
				SystemMessage smsg;
				smsg = new SystemMessage(SystemMessageId.C1_RECEIVED_DAMAGE_OF_S3_FROM_C2);
				smsg.addCharName(getActiveChar());
				smsg.addCharName(attacker);
				smsg.addNumber(fullValue);
				getActiveChar().sendPacket(smsg);
				
				if (tDmg > 0)
				{
					smsg = new SystemMessage(SystemMessageId.C1_RECEIVED_DAMAGE_OF_S3_FROM_C2);
					smsg.addCharName(getActiveChar().getPet());
					smsg.addCharName(attacker);
					smsg.addNumber(tDmg);
					getActiveChar().sendPacket(smsg);
					
					if (attackerPlayer != null)
					{
						smsg = new SystemMessage(SystemMessageId.GIVEN_S1_DAMAGE_TO_YOUR_TARGET_AND_S2_DAMAGE_TO_SERVITOR);
						smsg.addNumber(fullValue);
						smsg.addNumber(tDmg);
						attackerPlayer.sendPacket(smsg);
					}
				}
				
				if (manaDmg > 0)
				{
					if (attackerPlayer != null)
					{
						smsg = new SystemMessage(SystemMessageId.GIVEN_S1_DAMAGE_TO_YOUR_TARGET_AND_S2_DAMAGE_TO_SERVITOR);
						smsg.addNumber(fullValue);
						smsg.addNumber(manaDmg);
						attackerPlayer.sendPacket(smsg);
					}
				}
			}
		}
	}
	
	final int value2 = (int) value;
	
	if (value > 0)
	{
		value = getCurrentHp() - value;
		
		if (value <= 0)
		{
			if (getActiveChar().isInDuel())
			{
				getActiveChar().disableAllSkills();
				stopHpMpRegeneration();
				attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				attacker.sendPacket(ActionFailed.STATIC_PACKET);
				
				// let the DuelManager know of his defeat
				DuelManager.getInstance().onPlayerDefeat(getActiveChar());
				value = 1;
			}
			else
				value = 0;
		}
		setCurrentHp(value);
	}
	
	if (getActiveChar().getCurrentHp() < 0.5)
	{
		getActiveChar().abortAttack();
		getActiveChar().abortCast();
		
		if (getActiveChar().isInOlympiadMode())
		{
			if (getActiveChar().getPet() != null)
				getActiveChar().getPet().unSummon(getActiveChar());
			
			stopHpMpRegeneration();
			getActiveChar().setIsDead(true);
			getActiveChar().setIsPendingRevive(true);
			if (getActiveChar().getPet() != null)
				getActiveChar().getPet().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
			return;
		}
		
		if (isDOT)
			getActiveChar().setIgnorePK(true);
		
		getActiveChar().doDie(attacker);
		/*		QuestState qs = getActiveChar().getQuestState("255_Tutorial");
		if (qs != null && qs.getQuest() != null)
			qs.getQuest().notifyEvent("CE30", null, getActiveChar());*/
	}
	else
	{
		if (value > 1)
		{
			if (!isReflect && !isDOT && !isHPConsumption)
			{
				if (Formulas.calcAtkBreak(attacker, getActiveChar(), value2))
				{
					getActiveChar().breakAttack(attacker);
					getActiveChar().breakCast(attacker);
				}
			}
		}
	}
}

@Override
public final void setCurrentHp(double newHp, boolean broadcastPacket)
{
	super.setCurrentHp(newHp, broadcastPacket);
	
	if (getCurrentHp() <= getActiveChar().getStat().getMaxHp() * .3)
	{
		QuestState qs = getActiveChar().getQuestState("255_Tutorial");
		if (qs != null && qs.getQuest() != null)
			qs.getQuest().notifyEvent("CE45", null, getActiveChar());
	}
}

@Override
public final double getCurrentCp()
{
	return _currentCp;
}

@Override
public final void setCurrentCp(double newCp)
{
	setCurrentCp(newCp, true);
}

public final void setCurrentCp(double newCp, boolean broadcastPacket)
{
	// Get the Max CP of the L2Character
	int maxCp = getActiveChar().getStat().getMaxCp();
	
	synchronized (this)
	{
		if (getActiveChar().isDead())
			return;
		
		if (newCp < 0)
			newCp = 0;
		
		if (newCp >= maxCp)
		{
			// Set the RegenActive flag to false
			_currentCp = maxCp;
			_flagsRegenActive &= ~REGEN_FLAG_CP;
			
			// Stop the HP/MP/CP Regeneration task
			if (_flagsRegenActive == 0)
				stopHpMpRegeneration();
		}
		else
		{
			// Set the RegenActive flag to true
			_currentCp = newCp;
			_flagsRegenActive |= REGEN_FLAG_CP;
			
			// Start the HP/MP/CP Regeneration task with Medium priority
			startHpMpRegeneration();
		}
	}
	
	// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
	if (broadcastPacket)
		getActiveChar().broadcastStatusUpdate();
}

@Override
protected void doRegeneration()
{
	final PcStat charstat = getActiveChar().getStat();
	
	// Modify the current CP of the L2Character and broadcast Server->Client packet StatusUpdate
	if (getCurrentCp() < charstat.getMaxCp())
		setCurrentCp(getCurrentCp() + Formulas.calcCpRegen(getActiveChar()), false);
	
	// Modify the current HP of the L2Character and broadcast Server->Client packet StatusUpdate
	if (getCurrentHp() < charstat.getMaxHp())
		setCurrentHp(getCurrentHp() + Formulas.calcHpRegen(getActiveChar()), false);
	
	// Modify the current MP of the L2Character and broadcast Server->Client packet StatusUpdate
	if (getCurrentMp() < charstat.getMaxMp())
		setCurrentMp(getCurrentMp() + Formulas.calcMpRegen(getActiveChar()), false);
	
	getActiveChar().broadcastStatusUpdate(); //send the StatusUpdate packet
}

@Override
public L2PcInstance getActiveChar()
{
	return (L2PcInstance)super.getActiveChar();
}
}