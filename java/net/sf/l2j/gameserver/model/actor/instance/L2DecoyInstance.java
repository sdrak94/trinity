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
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.util.Rnd;

public class L2DecoyInstance extends L2Decoy
{
	private int			_totalLifeTime;
	private int			_timeRemaining;
	private Future<?>	_DecoyLifeTask;
	private Future<?>	_actionTask;
	public int			_physRange	= 40;
	public long			_atkReuse	= 5000;
	public boolean		_hasGreaterHeal;
	
	public L2DecoyInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2Skill skill)
	{
		super(objectId, template, owner);
		if (skill != null)
		{
			_totalLifeTime = ((L2SkillDecoy) skill).getTotalLifeTime();
		}
		else
		{
			_totalLifeTime = 20000;
		}
		_physRange = Math.max(40, owner.getPhysicalAttackRange());
		_timeRemaining = _totalLifeTime;
		_hasGreaterHeal = owner.getSkillLevel(1217) > 0;
		L2Weapon wep = owner.getActiveWeaponItem();
		int bowReuse = owner.isUsingRangedWep() ? owner.calculateReuseTime(null, wep) : 0;
		_atkReuse = (long) ((owner.calculateTimeBetweenAttacks(null, wep) * 1.1) + bowReuse);
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
		private final L2DecoyInstance	_Decoy;
		
		DecoyLifetime(L2PcInstance activeChar, L2DecoyInstance Decoy)
		{
			_activeChar = activeChar;
			_Decoy = Decoy;
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
		private final L2DecoyInstance _decoy;
		
		ActionTask(L2DecoyInstance activeChar)
		{
			_decoy = activeChar;
		}
		
		public void run()
		{
			long reuseDelay = _decoy._atkReuse;
			boolean exit = false;
			try
			{
				if (_decoy._hasGreaterHeal)
				{
					final L2Character ownerTarget = (L2Character) _decoy.getOwner().getTarget();
					if (ownerTarget != null && !ownerTarget.isAlikeDead() && !ownerTarget.isInvisible() && ownerTarget.isInsideRadius(_decoy, 600, true, false) && !ownerTarget.isAutoAttackable(_decoy.getOwner()))
						_decoy.setTarget(ownerTarget);
					else if (Rnd.get(5) <= 1)
					{
						if (!_decoy.getOwner().isInvisible() && !_decoy.getOwner().isAlikeDead() && _decoy.getOwner().isInsideRadius(_decoy, 600, true, false))
							_decoy.setTarget(_decoy.getOwner());
						else
							_decoy.setTarget(_decoy);
					}
					else
					{
						_decoy.setTarget(_decoy);
					}
					if (Rnd.get(2) == 0 || _decoy.isSkillDisabled(greaterHeal.getId()))
					{
						_decoy.getOwner().sendMessage("Decoy heals " + ((L2Character) _decoy.getTarget()).getDisplayName());
						_decoy.doCast(battleHeal);
						reuseDelay = (battleHeal.isStaticHitTime() ? battleHeal.getHitTime() : Formulas.calcSkillCastTime(_decoy, battleHeal, battleHeal.getHitTime())) + 500;
					}
					else
					{
						_decoy.getOwner().sendMessage("Decoy greater heals " + ((L2Character) _decoy.getTarget()).getDisplayName());
						_decoy.doCast(greaterHeal);
						reuseDelay = (greaterHeal.isStaticHitTime() ? greaterHeal.getHitTime() : Formulas.calcSkillCastTime(_decoy, greaterHeal, greaterHeal.getHitTime())) + 500;
					}
				}
				else if (!_decoy.getOwner().isInOlympiadMode())
				{
					boolean canCastHate = decoyHate.getTargetList(_decoy, true, _decoy).length > 0;
					final L2Object ownerTarget = _decoy.getActingPlayer().getTarget();
					int val = ownerTarget != null && ownerTarget instanceof L2Character && !((L2Character) ownerTarget).isAlikeDead() && ownerTarget.isAutoAttackable(_decoy.getActingPlayer()) ? 5 : 4;
					if (canCastHate && Rnd.get(val) == 0 && !_decoy.isSkillDisabled(decoyHate.getId()))
					{
						_decoy.setTarget(_decoy);
						_decoy.doCast(decoyHate);
						reuseDelay = (decoyHate.isStaticHitTime() ? decoyHate.getHitTime() : Formulas.calcSkillCastTime(_decoy, decoyHate, decoyHate.getHitTime())) + 300;
					}
					else
					{
						if (!_decoy.isAttackingDisabled())
						{
							final Collection<L2Character> knownchars = _decoy.getKnownList().getKnownCharactersInRadius(_decoy._physRange);
							if (knownchars != null && knownchars.size() > 0)
							{
								if (ownerTarget != null && ownerTarget instanceof L2Character && knownchars.contains(ownerTarget))
								{
									if (_decoy.seeIfCanAttack((L2Character) ownerTarget))
									{
										if (Rnd.get(3) <= 1 || _decoy.getOwner().getSkillLevel(507) < 1 || _decoy.isSkillDisabled(twinShot.getId()))
										{
											_decoy.doAttack((L2Character) ownerTarget);
										}
										else
										{
											_decoy.setTarget(ownerTarget);
											_decoy.doCast(twinShot);
											reuseDelay = (twinShot.isStaticHitTime() ? twinShot.getHitTime() : Formulas.calcSkillCastTime(_decoy, twinShot, twinShot.getHitTime())) + 300;
										}
										exit = true;
									}
								}
								if (!exit)
								{
									for (L2Character target : knownchars)
									{
										if (!(target instanceof L2PcInstance))
											continue;
										if (exit)
											break;
										if (!_decoy.seeIfCanAttack(target))
											continue;
										if (Rnd.get(3) <= 1 || _decoy.getOwner().getSkillLevel(507) < 1 || _decoy.isSkillDisabled(twinShot.getId()))
										{
											_decoy.doAttack(target);
										}
										else
										{
											_decoy.setTarget(target);
											_decoy.doCast(twinShot);
											reuseDelay = (twinShot.isStaticHitTime() ? twinShot.getHitTime() : Formulas.calcSkillCastTime(_decoy, twinShot, twinShot.getHitTime())) + 300;
										}
										exit = true;
									}
									if (!exit)
									{
										for (L2Character target : knownchars)
										{
											if (target instanceof L2PcInstance)
												continue;
											if (exit)
												break;
											if (!_decoy.seeIfCanAttack(target))
												continue;
											if (Rnd.get(3) <= 1 || _decoy.getOwner().getSkillLevel(507) < 1 || _decoy.isSkillDisabled(twinShot.getId()))
											{
												_decoy.doAttack(target);
											}
											else
											{
												_decoy.setTarget(target);
												_decoy.doCast(twinShot);
												reuseDelay = (twinShot.isStaticHitTime() ? twinShot.getHitTime() : Formulas.calcSkillCastTime(_decoy, twinShot, twinShot.getHitTime())) + 300;
											}
											exit = true;
										}
									}
								}
							}
						}
						if (!exit && canCastHate && !_decoy.isSkillDisabled(decoyHate.getId()))
						{
							_decoy.setTarget(_decoy);
							_decoy.doCast(decoyHate);
							reuseDelay = (decoyHate.isStaticHitTime() ? decoyHate.getHitTime() : Formulas.calcSkillCastTime(_decoy, decoyHate, decoyHate.getHitTime())) + 300;
						}
					}
				}
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
	public int getMaxMp()
	{
		return (int) (getActingPlayer().getStat().getMaxMp() * 0.9);
	}
	
	@Override
	public int getMaxHp()
	{
		return (int) (getActingPlayer().getStat().getMaxHp() * 0.9);
	}
	
	@Override
	public int getAccuracy(L2Character target)
	{
		return getActingPlayer().getStat().getAccuracy(target) - 4;
	}
	
	@Override
	public int getEvasionRate(L2Character target)
	{
		return getActingPlayer().getStat().getEvasionRate(target) - 4;
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