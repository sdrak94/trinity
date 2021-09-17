package net.sf.l2j.gameserver.model.actor.instance;

import java.util.concurrent.Future;
import java.util.logging.Level;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.knownlist.DecoyKnownList;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillDecoy;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class L2TreeOfLifeInstance extends L2Decoy
{
private int _totalLifeTime;
private int _timeRemaining;
private Future<?> _DecoyLifeTask;
private Future<?> _actionTask;
public int _physRange = 40;
public long _atkReuse = 5000;
public boolean _hasGreaterHeal;
public boolean _hasGreaterGroupHeal;

public L2TreeOfLifeInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2Skill skill)
{
	super(objectId, template, owner);
	
	if (skill != null)
	{
		_totalLifeTime = ((L2SkillDecoy)skill).getTotalLifeTime();
	}
	else
	{
		_totalLifeTime = 20000;
	}
	_physRange = Math.max(40, owner.getPhysicalAttackRange());
	_timeRemaining = _totalLifeTime;
	_DecoyLifeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new DecoyLifetime(owner, this), 1000, 1000);
	_actionTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActionTask(this), 1500);
}

@Override
public boolean doDie(L2Character killer)
{
	if (!super.doDie(killer))
		return false;
	
	if (_actionTask != null)
	{
		_actionTask.cancel(true);
		_actionTask = null;
	}
	if (_DecoyLifeTask != null)
	{
		_DecoyLifeTask.cancel(true);
		_DecoyLifeTask = null;
	}
	_totalLifeTime = 0;
	DecayTaskManager.getInstance().addDecayTask(this);
	return true;
}

@Override
public DecoyKnownList getKnownList()
{
	return (DecoyKnownList) super.getKnownList();
}

@Override
public void initKnownList()
{
	setKnownList(new DecoyKnownList(this));
}

static class DecoyLifetime implements Runnable
{
private final L2PcInstance _activeChar;

private final L2TreeOfLifeInstance _Decoy;

DecoyLifetime(L2PcInstance activeChar, L2TreeOfLifeInstance l2TreeOfLifeInstance)
{
	_activeChar = activeChar;
	_Decoy = l2TreeOfLifeInstance;
}

public void run()
{
	try
	{
		double newTimeRemaining;
		_Decoy.decTimeRemaining(1000);
		newTimeRemaining = _Decoy.getTimeRemaining();
		if (newTimeRemaining < 0)
		{
			_Decoy.unSummon(_activeChar);
		}
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Decoy Error: ", e);
	}
}
}

static class ActionTask implements Runnable
{
private final L2TreeOfLifeInstance _decoy;

ActionTask(L2TreeOfLifeInstance l2TreeOfLifeInstance)
{
	_decoy = l2TreeOfLifeInstance;
}

public void run()
{
	final int chance = Rnd.get(100);
	long reuseDelay = _decoy._atkReuse;
	try
	{
			final L2Character ownerTarget = (L2Character) _decoy.getOwner().getTarget();
			final L2PcInstance owner = _decoy.getOwner();
			if (owner.isInParty())
			{
				FastList<L2PcInstance> PtMembers = owner.getParty().getPartyMembers();
			}
			
			{
				_decoy.setTarget(_decoy);
			}
			if (chance < 79)
			{
				_decoy.getOwner().sendMessage(_decoy.getNpcName() + " used "+ GreaterGroupHeal.getName() +" With Chance: "+ chance +"%");
				_decoy.doCast(GreaterGroupHeal);
				reuseDelay = (GreaterGroupHeal.isStaticHitTime() ? GreaterGroupHeal.getHitTime() : Formulas.calcSkillCastTime(_decoy, GreaterGroupHeal, GreaterGroupHeal.getHitTime()));
				
				}
			else if (chance < 86)
			{
				_decoy.getOwner().sendMessage(_decoy.getNpcName() + " used "+ GreaterGroupHeal.getName() +" With Chance: "+ chance +"%");
				_decoy.doCast(MajorGroupHeal);
				reuseDelay = (MajorGroupHeal.isStaticHitTime() ? MajorGroupHeal.getHitTime() : Formulas.calcSkillCastTime(_decoy, MajorGroupHeal, MajorGroupHeal.getHitTime()));
				
			}
			else if (chance < 93)
			{
				_decoy.getOwner().sendMessage(_decoy.getNpcName() + " used "+ BalanceLife.getName() +" With Chance: "+ chance +"%");
				_decoy.doCast(BalanceLife);
				reuseDelay = (BalanceLife.isStaticHitTime() ? BalanceLife.getHitTime() : Formulas.calcSkillCastTime(_decoy, BalanceLife, BalanceLife.getHitTime()));
			}
			else
			{
				_decoy.getOwner().sendMessage(_decoy.getNpcName() + " used "+ BodyOfAvatar.getName() +" With Chance: "+ chance +"%");
				_decoy.doCast(BodyOfAvatar);
				reuseDelay = (BodyOfAvatar.isStaticHitTime() ? BodyOfAvatar.getHitTime() : Formulas.calcSkillCastTime(_decoy, BodyOfAvatar, BodyOfAvatar.getHitTime()));
			}
		/*		_decoy.getOwner().sendMessage(_decoy.getNpcName() + " greater heals "+((L2Character) _decoy.getTarget()).getDisplayName());
				_decoy.doCast(greaterHeal);
				reuseDelay = (greaterHeal.isStaticHitTime() ? greaterHeal.getHitTime() : Formulas.calcSkillCastTime(_decoy, greaterHeal, greaterHeal.getHitTime()))+500;
		*/
	}
	catch (Throwable e)
	{
		_log.log(Level.SEVERE, "Decoy Error: ", e);
	}
	finally
	{
		_decoy._actionTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActionTask(_decoy), reuseDelay);
	}
}
}

@Override
public void unSummon(L2PcInstance owner)
{
	if (_DecoyLifeTask != null)
	{
		_DecoyLifeTask.cancel(true);
		_DecoyLifeTask = null;
	}
	if (_actionTask != null)
	{
		_actionTask.cancel(true);
		_actionTask = null;
	}
	super.unSummon(owner);
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

@Override
public boolean isAttackable()
{
	return true;
}
@Override
public int getMaxMp() { return (int) (getActingPlayer().getStat().getMaxMp() * 0.9); }
@Override
public int getMaxHp() { return (int) (getActingPlayer().getStat().getMaxHp() * 0.9); }
@Override
public int getAccuracy(L2Character target)
{
	return getActingPlayer().getStat().getAccuracy(target)-4;
}
@Override
public int getEvasionRate(L2Character target)
{
	return getActingPlayer().getStat().getEvasionRate(target)-4;
}
@Override
public int getPAtk(L2Character target)
{
	return (int) (getActingPlayer().getStat().getPAtk(target) * 0.85);
}
@Override
public int getMAtk(L2Character target, L2Skill skill)
{
	return (int) (getActingPlayer().getStat().getMAtk(target, skill) * 0.85);
}
@Override
public int getPDef(L2Character target)
{
	return (int) (getActingPlayer().getStat().getPDef(target) * 0.85);
}
@Override
public int getMDef(L2Character target, L2Skill skill)
{
	return (int) (getActingPlayer().getStat().getMDef(target, skill) * 0.85);
}
@Override
public double getCriticalDmg(L2Character target, double init, L2Skill skill)
{
	return getActingPlayer().getStat().getCriticalDmg(target, init, skill) * 0.85;
}
@Override
public int getCriticalHit(L2Character target, L2Skill skill)
{
	return (int) (getActingPlayer().getStat().getCriticalHitRate(target, skill) * 0.85);
}
@Override
public int getMCriticalHit(L2Character target, L2Skill skill)
{
	return (int) (getActingPlayer().getStat().getMCriticalHit(target, skill) * 0.85);
}
@Override
public int getShldDef()
{
	return getActingPlayer().getStat().getShldDef();
}


}