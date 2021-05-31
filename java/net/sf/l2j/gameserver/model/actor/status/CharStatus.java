package net.sf.l2j.gameserver.model.actor.status;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TesterInstance;
import net.sf.l2j.gameserver.model.actor.stat.CharStat;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class CharStatus
{
protected static final Logger _log = Logger.getLogger(CharStatus.class.getName());

private final L2Character _activeChar;

private double _currentHp = 0; //Current HP of the L2Character
private double _currentMp = 0; //Current MP of the L2Character

/** Array containing all clients that need to be notified about hp/mp updates of the L2Character */
private Set<L2Character> _StatusListener;

private Future<?> _regTask;

protected byte _flagsRegenActive = 0;

protected static final byte REGEN_FLAG_CP = 4;
private static final byte REGEN_FLAG_HP = 1;
private static final byte REGEN_FLAG_MP = 2;

public CharStatus(L2Character activeChar)
{
	_activeChar = activeChar;
}

/**
 * Add the object to the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR><BR>
 *
 * <B><U> Concept</U> :</B><BR><BR>
 * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates.
 * Players who must be informed are players that target this L2Character.
 * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<BR><BR>
 *
 * <B><U> Example of use </U> :</B><BR><BR>
 * <li> Target a PC or NPC</li><BR><BR>
 *
 * @param object L2Character to add to the listener
 *
 */
public final void addStatusListener(L2Character object)
{
	if (object == getActiveChar())
		return;
	
	synchronized (getStatusListener())
	{
		getStatusListener().add(object);
	}
}

/**
 * Remove the object from the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR><BR>
 *
 * <B><U> Concept</U> :</B><BR><BR>
 * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates.
 * Players who must be informed are players that target this L2Character.
 * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<BR><BR>
 *
 * <B><U> Example of use </U> :</B><BR><BR>
 * <li> Untarget a PC or NPC</li><BR><BR>
 *
 * @param object L2Character to add to the listener
 *
 */
public final void removeStatusListener(L2Character object)
{
	synchronized (getStatusListener())
	{
		getStatusListener().remove(object);
	}
}

/**
 * Return the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR><BR>
 *
 * <B><U> Concept</U> :</B><BR><BR>
 * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates.
 * Players who must be informed are players that target this L2Character.
 * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<BR><BR>
 *
 * @return The list of L2Character to inform or null if empty
 *
 */
public final Set<L2Character> getStatusListener()
{
	if (_StatusListener == null)
		_StatusListener = new CopyOnWriteArraySet<L2Character>();
	return _StatusListener;
}

// place holder, only PcStatus has CP
public void reduceCp(int value)
{
}

/**
 * Reduce the current HP of the L2Character and launch the doDie Task if necessary.<BR><BR>
 *
 * <B><U> Overridden in </U> :</B><BR><BR>
 * <li> L2Attackable : Set overhit values</li><BR>
 * <li> L2Npc : Update the attacker AggroInfo of the L2Attackable _aggroList and clear duel status of the attacking players</li><BR><BR>
 *
 * @param i The HP decrease value
 * @param attacker The L2Character who attacks
 * @param awake The awake state (If True : stop sleeping)
 *
 */
public void reduceHp(double value, L2Character attacker)
{
	reduceHp(value, attacker, true, false, false, false);
}

public void reduceHp(double value, L2Character attacker, boolean isHpConsumption)
{
	reduceHp(value, attacker, true, false, isHpConsumption, false);
}

public void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption, boolean bypassCP)
{
	if (getActiveChar().isDead() || getActiveChar() instanceof L2TesterInstance)
		return;
	
	if (getActiveChar().isInvul())
	{
		int mustGoThruDmg = 0;
		
		if (isDOT || isHPConsumption || (getActiveChar() instanceof L2Playable && getActiveChar().getActingPlayer().isSpawnProtected()))
		{}
		else
		{
			mustGoThruDmg = (int) (attacker.calcStat(Stats.DMG_ADD, 0, getActiveChar(), null) - getActiveChar().calcStat(Stats.DMG_REMOVE, 0, attacker, null)- getActiveChar().calcStat(Stats.DMG_REMOVE, 0, attacker, null));
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
	
	if (attacker != null)
	{
		final L2PcInstance attackerPlayer = attacker.getActingPlayer();
		if (attackerPlayer != null && attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
			return;
	}
	
	if (!isDOT && !isHPConsumption)
	{
		if (awake && getActiveChar().isSleeping())
			getActiveChar().stopSleeping(null);
		if (getActiveChar().isStunned() && Rnd.get(10) == 0)
			getActiveChar().stopStunning(null);
		if (getActiveChar().isAfraid() && Rnd.get(10) <= 1)
			getActiveChar().stopFear(null);
		if (getActiveChar().isImmobileUntilAttacked())
			getActiveChar().stopImmobileUntilAttacked(null);
	}
	
	if (value > 0) // Reduce Hp if any, and Hp can't be negative
		setCurrentHp(Math.max(getCurrentHp() - value, 0));
	
	if (getActiveChar().getCurrentHp() < 0.5) // Die
	{
		getActiveChar().abortAttack();
		getActiveChar().abortCast();
		
		if (Config.DEBUG)
			_log.fine("char is dead.");
		
		if (getActiveChar() instanceof L2Attackable)
		{
			boolean ok = false;
			
			L2Character mostHated = ((L2Attackable) getActiveChar()).getMostHated();
			
			if (mostHated != null)
			{
				if (Util.checkIfInRange(1300, getActiveChar(), mostHated, false))
				{
					attacker = mostHated;
					ok = true;
				}
			}
			
			if (!ok)
			{
				mostHated = getActiveChar().getAI().getAttackTarget();
				
				if (mostHated != null)
				{
					if (Util.checkIfInRange(1100, getActiveChar(), mostHated, false))
					{
						attacker = mostHated;
					}
				}
			}
		}
		
		if (isDOT && getActiveChar() instanceof L2Playable)
			((L2Playable)getActiveChar()).setIgnorePK(true);
		
		boolean allowDie = true;

		if(allowDie)
			getActiveChar().doDie(attacker);
	}
}

public void reduceMp(double value)
{
	setCurrentMp(Math.max(getCurrentMp() - value, 0));
}

/**
 * Start the HP/MP/CP Regeneration task.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Calculate the regen task period </li>
 * <li>Launch the HP/MP/CP Regeneration task with Medium priority </li><BR><BR>
 *
 */
public final synchronized void startHpMpRegeneration()
{
	if (_regTask == null && !getActiveChar().isDead())
	{
		if (Config.DEBUG)
			_log.fine("HP/MP regen started");
		
		// Get the Regeneration periode
		int period = Formulas.getRegeneratePeriod(getActiveChar());
		
		// Create the HP/MP/CP Regeneration task
		_regTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new RegenTask(), period, period);
	}
}

/**
 * Stop the HP/MP/CP Regeneration task.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Set the RegenActive flag to False </li>
 * <li>Stop the HP/MP/CP Regeneration task </li><BR><BR>
 *
 */
public final synchronized void stopHpMpRegeneration()
{
	if (_regTask != null)
	{
		if (Config.DEBUG)
			_log.fine("HP/MP regen stop");
		
		// Stop the HP/MP/CP Regeneration task
		_regTask.cancel(false);
		_regTask = null;
		
		// Set the RegenActive flag to false
		_flagsRegenActive = 0;
	}
}

// place holder, only PcStatus has CP
public double getCurrentCp()
{
	return 0;
}

// place holder, only PcStatus has CP
public void setCurrentCp(double newCp)
{
}

public final double getCurrentHp()
{
	return _currentHp;
}

public final void setCurrentHp(double newHp)
{
	setCurrentHp(newHp, true);
}

public void setCurrentHp(double newHp, boolean broadcastPacket)
{
	// Get the Max HP of the L2Character
	final double maxHp = getActiveChar().getStat().getMaxHp();
	
	synchronized (this)
	{
		if (getActiveChar().isDead())
			return;
		
		if (newHp >= maxHp)
		{
			// Set the RegenActive flag to false
			_currentHp = maxHp;
			_flagsRegenActive &= ~REGEN_FLAG_HP;
			
			// Stop the HP/MP/CP Regeneration task
			if (_flagsRegenActive == 0)
				stopHpMpRegeneration();
		}
		else
		{
			// Set the RegenActive flag to true
			_currentHp = newHp;
			_flagsRegenActive |= REGEN_FLAG_HP;
			
			// Start the HP/MP/CP Regeneration task with Medium priority
			startHpMpRegeneration();
		}
	}
	
	// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
	if (broadcastPacket)
		getActiveChar().broadcastStatusUpdate();
}

public final void setCurrentHpMp(double newHp, double newMp)
{
	setCurrentHp(newHp, false);
	setCurrentMp(newMp, true); //send the StatusUpdate only once
}

public final double getCurrentMp()
{
	return _currentMp;
}

public final void setCurrentMp(double newMp)
{
	setCurrentMp(newMp, true);
}

public final void setCurrentMp(double newMp, boolean broadcastPacket)
{
	// Get the Max MP of the L2Character
	final int maxMp = getActiveChar().getStat().getMaxMp();
	
	synchronized (this)
	{
		if (getActiveChar().isDead())
			return;
		
		if (newMp >= maxMp)
		{
			// Set the RegenActive flag to false
			_currentMp = maxMp;
			_flagsRegenActive &= ~REGEN_FLAG_MP;
			
			// Stop the HP/MP/CP Regeneration task
			if (_flagsRegenActive == 0)
				stopHpMpRegeneration();
		}
		else
		{
			// Set the RegenActive flag to true
			_currentMp = newMp;
			_flagsRegenActive |= REGEN_FLAG_MP;
			
			// Start the HP/MP/CP Regeneration task with Medium priority
			startHpMpRegeneration();
		}
	}
	
	// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
	if (broadcastPacket)
		getActiveChar().broadcastStatusUpdate();
}

protected void doRegeneration()
{
	final CharStat charstat = getActiveChar().getStat();
	
	// Modify the current HP of the L2Character and broadcast Server->Client packet StatusUpdate
	if (getCurrentHp() < charstat.getMaxHp())
	{
		double regenAmount = Formulas.calcHpRegen(getActiveChar());
		
		if (getActiveChar() instanceof L2Attackable)
		{
			if (!((L2Attackable)getActiveChar()).isInCombat())
			{
				if (((L2Attackable)getActiveChar()).isAPC())
					regenAmount *= 20;
				else if (getActiveChar().isRaid())
					regenAmount *= 12;
			}
		}
		
		setCurrentHp(getCurrentHp() + regenAmount, false);
	}
	
	// Modify the current MP of the L2Character and broadcast Server->Client packet StatusUpdate
	if (getCurrentMp() < charstat.getMaxMp())
		setCurrentMp(getCurrentMp()
				+ Formulas.calcMpRegen(getActiveChar()), false);
	
	if (!getActiveChar().isInActiveRegion())
	{
		// no broadcast necessary for characters that are in inactive regions.
		// stop regeneration for characters who are filled up and in an inactive region.
		if ((getCurrentCp() == charstat.getMaxCp())
				&& (getCurrentHp() == charstat.getMaxHp())
				&& (getCurrentMp() == charstat.getMaxMp()))
			stopHpMpRegeneration();
	}
	else
		getActiveChar().broadcastStatusUpdate(); //send the StatusUpdate packet
}

/** Task of HP/MP regeneration */
class RegenTask implements Runnable
{
public void run()
{
	try
	{
		doRegeneration();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "", e);
	}
}
}

public L2Character getActiveChar()
{
	return _activeChar;
}
}