package net.sf.l2j.gameserver.model.actor.instance;

import java.util.logging.Level;

import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2TrapInstance extends L2Npc
{
private int _totalLifeTime;
private int _timeRemaining;
protected L2Skill _skill;
private final L2Skill _originalSkill;
protected boolean _isDetected = false;
final private L2PcInstance _owner;
protected boolean _stopped = false;

public L2TrapInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, int lifeTime, L2Skill skill, L2Skill originalSkill)
{
	super(objectId, template);
	setCollisionRadius(0);
	
	_owner = owner;
	
	if (lifeTime != 0)
	{
		_totalLifeTime = lifeTime;
	}
	else
	{
		_totalLifeTime = 60000;
	}
	
	_timeRemaining = _totalLifeTime;
	
	_skill = skill;
	_originalSkill = originalSkill;
	
	ThreadPoolManager.getInstance().scheduleAi(new TrapTask(this, _skill), 2500);
}

@Override
public boolean doDie(L2Character killer)
{
	if (!super.doDie(killer))
		return false;
	
	_stopped = true;
	_isDetected = true;
	_totalLifeTime = 0;
	_timeRemaining = 0;
	
	DecayTaskManager.getInstance().addDecayTask(this);
	return true;
}

@Override
public boolean isAttackable()
{
	return false;
}

class TrapTask implements Runnable
{
private final L2TrapInstance _trap;

TrapTask(L2TrapInstance trap, L2Skill skill)
{
	_trap = trap;
}

public void run()
{
	try
	{
		if (_trap != null && !_stopped)
		{
			_trap.decTimeRemaining(1000);
			
			for (L2Character trg : _trap.getKnownList().getKnownCharactersInRadius(_skill.getSkillRadius() - 65))
			{
				if (!validateConditions(trg, getOwner()))
				{
					continue;
				}
				else
				{
					_stopped = true;
					
					for (L2PcInstance player : getKnownList().getKnownPlayers().values())
					{
						if (player != null && !player.getKnownList().knowsObject(_trap))
							player.getKnownList().addKnownObject(_trap);
					}
					
					ThreadPoolManager.getInstance().scheduleGeneral(new activateTask(_trap), 420);
					return;
				}
			}
			
			if (!_stopped)
			{
				if (_trap.getTimeRemaining() <= 0)
				{
					_trap.unSummon();
					return;
				}
				
				ThreadPoolManager.getInstance().scheduleAi(new TrapTask(_trap, _skill), 1000);
			}
		}
	}
	catch (Exception e)
	{
		_trap.unSummon();
		_log.log(Level.SEVERE, "", e);
	}
}
}

public boolean validateConditions(L2Character knownTarget, L2Character owner)
{
	if (knownTarget == null || knownTarget == owner || knownTarget.isAlikeDead() || knownTarget.isInvul())
		return false;
	
	if (!(knownTarget instanceof L2Attackable || knownTarget instanceof L2Playable))
		return false;
	
	if (knownTarget instanceof L2GuardInstance)
		return false;
	
	if (!GeoData.getInstance().canSeeTarget(this, knownTarget))
		return false;
	
	if (knownTarget.isInsideZone(L2Character.ZONE_PVP))
		return true;
	
	if (knownTarget instanceof L2PcInstance)
	{
		if (!owner.getActingPlayer().checkAOEPvPSkill(knownTarget.getActingPlayer(), _skill))
			return false;
	}
	
	return true;
}

private class activateTask implements Runnable
{
L2TrapInstance _trap = null;

public activateTask(L2TrapInstance trp)
{
	_trap = trp;
}

public void run()
{
	if (_trap != null)
	{
		try
		{
			doCast(_skill);
			
			try
			{
				wait(_skill.getHitTime() + 500);
			}
			catch (Exception e)
			{
			}
			
			try
			{
				L2Object[] targetList = _skill.getTargetList(_trap);
				
				if (targetList == null || targetList.length == 0)
					return;
				
				for (L2Object atked : targetList)
				{
					if (atked == getOwner())
						continue;
					
					if (!_skill.isDamaging())
						getOwner().sendMessage("Your "+_originalSkill.getName()+" activated against "+((L2Character) atked).getDisplayName());
					
					if (atked instanceof L2PcInstance)
						continue;
					
					if (atked instanceof L2Attackable)
						((L2Attackable)atked).addDamage(getOwner(), 1, null);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (_trap != null && !_trap.isDead())
					_trap.unSummon();
			}
			
		} catch (Throwable t) {_trap.unSummon();_log.log(Level.SEVERE, "", t);}
	}
}
}

public void unSummon()
{
	_stopped = true;
	_totalLifeTime = 0;
	_timeRemaining = 0;
	if (isVisible() && !isDead())
	{
		if (getWorldRegion() != null)
			getWorldRegion().removeFromZones(this);
		
		decayMe();
		getKnownList().removeAllKnownObjects();
	}
}
public void decTimeRemaining(int value)
{
	_timeRemaining -= value;
}
public int getTimeRemaining()
{
	return _timeRemaining;
}
public int getTotalLifeTime()
{
	return _totalLifeTime;
}
public void setDetected()
{
	_isDetected = true;
}

public boolean isDetected()
{
	return _isDetected;
}
public final L2PcInstance getOwner()
{
	return _owner;
}
public boolean isStopped()
{
	return _stopped;
}
@Override
public L2PcInstance getActingPlayer()
{
	return _owner;
}
@Override
public void sendInfo(L2PcInstance activeChar)
{
	activeChar.sendPacket(new AbstractNpcInfo.TrapInfo(this, activeChar));
}
@Override
public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
{
	if (miss) return;
	
	// Prevents the double spam of system messages, if the target is the owning player.
	if (target.getObjectId() != getOwner().getObjectId())
	{
		if (pcrit || mcrit)
			getOwner().sendMessage("Trap Critical Hit!");
		
		if (target.isInvul() && !(target instanceof L2NpcInstance))
		{
			getOwner().sendPacket(new SystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED));
			return;
		}
		
		if (getOwner().isInOlympiadMode() &&
				target instanceof L2PcInstance &&
				((L2PcInstance)target).isInOlympiadMode() &&
				((L2PcInstance)target).getOlympiadGameId() == getOwner().getOlympiadGameId())
		{
			Olympiad.getInstance().notifyCompetitorDamage(getOwner(), damage, getOwner().getOlympiadGameId());
		}
		
		getOwner().sendMessage("Your trap hit "+target.getDisplayName()+" for "+damage+" damage.");
	}
}
}