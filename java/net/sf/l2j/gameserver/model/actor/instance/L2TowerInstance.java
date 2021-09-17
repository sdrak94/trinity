package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.logging.Level;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.knownlist.DecoyKnownList;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillDecoy;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class L2TowerInstance extends L2Decoy
{
	private int			_totalLifeTime;
	private int			_timeRemaining;
	private Future<?>	_DecoyLifeTask;
	private Future<?>	_actionTask;
	public int			_magicRange	= 0;
	
	public L2TowerInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2Skill skill)
	{
		super(objectId, template, owner);
		_showSummonAnimation = true;
		if (skill != null)
		{
			_totalLifeTime = ((L2SkillDecoy) skill).getTotalLifeTime();
		}
		else
		{
			_totalLifeTime = 20000;
		}
		_magicRange = mobdeathspike.getCastRange(getActingPlayer());
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
		private final L2PcInstance		_activeChar;
		private final L2TowerInstance	_tower;
		
		DecoyLifetime(L2PcInstance activeChar, L2TowerInstance Decoy)
		{
			_activeChar = activeChar;
			_tower = Decoy;
		}
		
		public void run()
		{
			try
			{
				double newTimeRemaining;
				_tower.decTimeRemaining(1000);
				newTimeRemaining = _tower.getTimeRemaining();
				if (newTimeRemaining < 0)
				{
					_tower.unSummon(_activeChar);
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
		private final L2TowerInstance _tower;
		
		ActionTask(L2TowerInstance activeChar)
		{
			_tower = activeChar;
		}
		
		public void run()
		{
			long reuseDelay = 500;
			boolean exit = false;
			try
			{
				final L2Object ownerTarget = _tower.getActingPlayer().getTarget();
				if (!_tower.isOutOfControl() && !_tower.isCastingNow() && !_tower.isMuted())
				{
					final Collection<L2Character> knownchars = _tower.getKnownList().getKnownCharactersInRadius(_tower._magicRange);
					if (knownchars != null && knownchars.size() > 0)
					{
						if (ownerTarget != null && ownerTarget instanceof L2Character && knownchars.contains(ownerTarget))
						{
							if (_tower.seeIfCanAttack((L2Character) ownerTarget))
							{
								reuseDelay = _tower.doAction((L2Character) ownerTarget);
								exit = true;
							}
						}
						if (!exit)
						{
							for (L2Character target : knownchars)
							{
								if (!(target instanceof L2PcInstance))
									continue;
								if (!_tower.seeIfCanAttack(target))
									continue;
								reuseDelay = _tower.doAction(target);
								exit = true;
							}
							if (!exit)
							{
								for (L2Character target : knownchars)
								{
									if (target instanceof L2PcInstance)
										continue;
									if (!_tower.seeIfCanAttack(target))
										continue;
									reuseDelay = _tower.doAction(target);
									exit = true;
								}
							}
						}
					}
				}
			}
			catch (Throwable e)
			{
				_log.log(Level.SEVERE, "Tower Error: ", e);
			}
			finally
			{
				_tower._actionTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActionTask(_tower), reuseDelay);
			}
		}
	}
	
	public long doAction(L2Character target)
	{
		setTarget(target);
		final int chance = Rnd.get(100);
		if (chance < 79)
		{
			doCast(mobdeathspike);
			return (mobdeathspike.isStaticHitTime() ? mobdeathspike.getHitTime() : Formulas.calcSkillCastTime(this, mobdeathspike, mobdeathspike.getHitTime())) + 300;
		}
		else if (chance < 86)
		{
			doCast(anchor);
			return (anchor.isStaticHitTime() ? anchor.getHitTime() : Formulas.calcSkillCastTime(this, anchor, anchor.getHitTime())) + 300;
		}
		else if (chance < 93)
		{
			doCast(cursefear);
			return (cursefear.isStaticHitTime() ? cursefear.getHitTime() : Formulas.calcSkillCastTime(this, cursefear, cursefear.getHitTime())) + 300;
		}
		else
		{
			doCast(curseofdoom);
			return (curseofdoom.isStaticHitTime() ? curseofdoom.getHitTime() : Formulas.calcSkillCastTime(this, curseofdoom, curseofdoom.getHitTime())) + 300;
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
	
	/*
	 * @Override
	 * public int getMaxMp() { return (int) (getActingPlayer().getStat().getMaxMp() * 0.9); }
	 * @Override
	 * public int getMaxHp() { return (int) (getActingPlayer().getStat().getMaxHp() * 0.9); }
	 */
	/*
	 * @Override
	 * public int getAccuracy(L2Character target)
	 * {
	 * return getActingPlayer().getStat().getAccuracy(target)-4;
	 * }
	 * @Override
	 * public int getEvasionRate(L2Character target)
	 * {
	 * return getActingPlayer().getStat().getEvasionRate(target)-4;
	 * }
	 */
	@Override
	public int getPAtk(L2Character target)
	{
		return (getActingPlayer().getStat().getPAtk(target));
	}
	
	@Override
	public int getMAtk(L2Character target, L2Skill skill)
	{
		return (getActingPlayer().getStat().getMAtk(target, skill));
	}
	
	/*
	 * @Override
	 * public int getPDef(L2Character target)
	 * {
	 * return (int) (getActingPlayer().getStat().getPDef(target) * 0.85);
	 * }
	 * @Override
	 * public int getMDef(L2Character target, L2Skill skill)
	 * {
	 * return (int) (getActingPlayer().getStat().getMDef(target, skill) * 0.85);
	 * }
	 */
	/*
	 * @Override
	 * public double getCriticalDmg(L2Character target, double init)
	 * {
	 * return getActingPlayer().getStat().getCriticalDmg(target, init) * 0.85;
	 * }
	 * @Override
	 * public int getCriticalHit(L2Character target, L2Skill skill)
	 * {
	 * return (int) (getActingPlayer().getStat().getCriticalHitRate(target, skill) * 0.85);
	 * }
	 */
	@Override
	public int getMCriticalHit(L2Character target, L2Skill skill)
	{
		return (getActingPlayer().getStat().getMCriticalHit(target, skill));
	}
	/*
	 * @Override
	 * public int getShldDef()
	 * {
	 * return getActingPlayer().getStat().getShldDef();
	 * }
	 */

}