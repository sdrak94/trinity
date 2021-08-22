package net.sf.l2j.gameserver.model;

import static net.sf.l2j.gameserver.model.CharEffectList.BUFFER_BUFFS_DURATION;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbnormalStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PartySpelled;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.skills.funcs.FuncTemplate;
import net.sf.l2j.gameserver.skills.funcs.Lambda;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.12 $ $Date: 2005/04/11 10:06:07 $
 */
public abstract class L2Effect
{
	static final Logger _log = Logger.getLogger(L2Effect.class.getName());
	
	public static enum EffectState
	{
		CREATED,
		ACTING,
		FINISHING
	}
	
	private static final Func[]		_emptyFunctionSet	= new Func[0];
	// member _effector is the instance of L2Character that cast/used the spell/skill that is
	// causing this effect. Do not confuse with the instance of L2Character that
	// is being affected by this effect.
	private final L2Character		_effector;
	// member _effected is the instance of L2Character that was affected
	// by this effect. Do not confuse with the instance of L2Character that
	// casted/used this effect.
	private final L2Character		_effected;
	// the skill that was used.
	private final L2Skill			_skill;
	// or the items that was used.
	// private final L2Item _item;
	// the value of an update
	private final Lambda			_lambda;
	// the current state
	private EffectState				_state;
	// period, seconds
	private int						_period;
	private int						_periodStartTicks;
	private int						_periodfirsttime;
	private final EffectTemplate	_template;
	// function templates
	private final FuncTemplate[]	_funcTemplates;
	// initial count
	private final int				_totalCount;
	// counter
	private int						_count;
	// abnormal effect mask
	private final AbnormalEffect	_abnormalEffect;
	// special effect mask
	private final AbnormalEffect	_specialEffect;
	// show icon
	private final boolean			_icon, _msg;
	public boolean					_naturallyWornOff	= false;
	public boolean					preventExitUpdate;
	private boolean					isStolen			= false;
	
	public boolean isStolen()
	{
		return isStolen;
	}
	
	public final class EffectTask implements Runnable
	{
		protected final int	_delay;
		protected final int	_rate;
		
		EffectTask(int pDelay, int pRate)
		{
			_delay = pDelay;
			_rate = pRate;
		}
		
		public void run()
		{
			try
			{
				if (getPeriodfirsttime() == 0)
					setPeriodStartTicks(GameTimeController.getGameTicks());
				else
					setPeriodfirsttime(0);
				scheduleEffect();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	private ScheduledFuture<?>	_currentFuture;
	private EffectTask			_currentTask;
	/** The Identifier of the stack group */
	private final String		_stackType;
	/** The position of the effect in the stack group */
	private final float			_stackOrder;
	private boolean				_inUse					= false;
	private boolean				_startConditionsCorrect	= true;
	/**
	 * For special behavior. See Formulas.calcEffectSuccess
	 */
	private double				_effectPower;
	private L2SkillType			_effectSkillType;
	
	/**
	 * <font color="FF0000"><b>WARNING: scheduleEffect nolonger inside constructor</b></font><br>
	 * So you must call it explicitly
	 */
	protected L2Effect(Env env, EffectTemplate template, Boolean ignoreBoost)
	{
		_state = EffectState.CREATED;
		_skill = env.skill;
		// _item = env._item == null ? null : env._item.getItem();
		_template = template;
		_effected = env.target;
		_effector = env.player;
		_lambda = template.lambda;
		_funcTemplates = template.funcTemplates;
		_count = template.counter;
		_totalCount = _count;
		// Support for retail herbs duration when _effected has a Summon
		int temp = template.period;
		/*
		 * if ((_skill.getId() > 2277 && _skill.getId() < 2286) || (_skill.getId() >= 2512 && _skill.getId() <= 2514))
		 * {
		 * if (_effected instanceof L2SummonInstance || (_effected instanceof L2PcInstance && ((L2PcInstance) _effected).getPet() instanceof L2SummonInstance))
		 * {
		 * temp /= 2;
		 * }
		 * }
		 */
		if (!ignoreBoost && temp < BUFFER_BUFFS_DURATION)
		{
			if (env.skillMastery)
				temp *= 2;
			if (_effector != null && _effector instanceof L2PcInstance && _skill != null)
			{
				final int difference = (int) _effector.calcStat(Stats.EFFECT_DURATION_CHANGE, 0, _effected, _skill);
				if (difference != 0)
				{
					temp += difference;
				}
				if (_effector.getActingPlayer().isInOlympiadMode())
				{
					temp *= _skill.getOlyTimeMulti();
				}
				if (_template.effectType == L2SkillType.DEBUFF)
					temp = (int) _effected.calcStat(Stats.DEBUFF_DURATION_REDUCE, temp, _effector, _skill);
				else if (_template.effectType == L2SkillType.STUN)
					temp = (int) _effected.calcStat(Stats.STUN_DURATION_REDUCE, temp, _effector, _skill);
				if (temp < 1)
					temp = 1;
			}
		}
		_period = temp;
		_abnormalEffect = template.abnormalEffect;
		_specialEffect = template.specialEffect;
		_stackType = template.stackType;
		_stackOrder = template.stackOrder;
		_periodStartTicks = GameTimeController.getGameTicks();
		_periodfirsttime = 0;
		_icon = template.icon;
		_msg = template.msg;
		_effectPower = template.effectPower;
		_effectSkillType = template.effectType;
	}
	
	/**
	 * Special constructor to "steal" buffs. Must be implemented on
	 * every child class that can be stolen.<br>
	 * <br>
	 * <font color="FF0000"><b>WARNING: scheduleEffect nolonger inside constructor</b></font>
	 * <br>
	 * So you must call it explicitly
	 * 
	 * @param env
	 * @param effect
	 */
	protected L2Effect(Env env, L2Effect effect)
	{
		_template = effect._template;
		_state = EffectState.CREATED;
		_skill = env.skill;
		_effected = env.target;
		_effector = env.player;
		_lambda = _template.lambda;
		_funcTemplates = _template.funcTemplates;
		_count = effect.getCount();
		_totalCount = _template.counter;
		_period = _template.period - effect.getElapsedTime();
		if (_period > 240) // period is greater than 4 minutes
			_period = 240; // set it so steal divinity buffs can last no longer than 4 minutes
		isStolen = true;
		_abnormalEffect = _template.abnormalEffect;
		_specialEffect = _template.specialEffect;
		_stackType = _template.stackType;
		_stackOrder = _template.stackOrder;
		_periodStartTicks = effect.getPeriodStartTicks();
		_periodfirsttime = effect.getPeriodfirsttime();
		_icon = _template.icon;
		_msg = _template.msg;
		/*
		 * Commented out by DrHouse:
		 * scheduleEffect can call onStart before effect is completly
		 * initialized on constructor (child classes constructor)
		 */
		// scheduleEffect();
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public int getTotalCount()
	{
		return _totalCount;
	}
	
	public void setCount(int newcount)
	{
		_count = newcount;
	}
	
	public void setFirstTime(int newfirsttime)
	{
		if (_currentFuture != null)
		{
			_periodStartTicks = GameTimeController.getGameTicks() - newfirsttime * GameTimeController.TICKS_PER_SECOND;
			_currentFuture.cancel(false);
			_currentFuture = null;
			_currentTask = null;
			_periodfirsttime = newfirsttime;
			if (_period < BUFFER_BUFFS_DURATION)
			{
				int duration = _period - _periodfirsttime;
				// _log.warn("Period: "+_period+"-"+_periodfirsttime+"="+duration);
				_currentTask = new EffectTask(duration * 1000, -1);
				_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask, duration * 1000);
			}
			else
			{
				if (_state == EffectState.CREATED)
				{
					_state = EffectState.ACTING;
					onStart();
					if (_count > 1)
					{
						startEffectTaskAtFixedRate(5, _period * 1000);
						return;
					}
					if (_period > 0)
					{
						_effected.addEffect(this);
					}
				}
				if (_state == EffectState.ACTING)
					return;
			}
		}
	}
	
	public boolean getShowIcon()
	{
		return _icon;
	}
	
	public boolean getSendMessage()
	{
		return _msg;
	}
	
	public int getPeriod()
	{
		return _period;
	}
	
	public int getElapsedTime()
	{
		return (GameTimeController.getGameTicks() - _periodStartTicks) / GameTimeController.TICKS_PER_SECOND;
	}
	
	public int getTime()
	{
		return (GameTimeController.getGameTicks() - _periodStartTicks) / GameTimeController.TICKS_PER_SECOND;
	}
	
	/**
	 * Returns the elapsed time of the task.
	 * 
	 * @return Time in seconds.
	 */
	public int getTaskTime()
	{
		if (_count == _totalCount)
			return 0;
		return (Math.abs(_count - _totalCount + 1) * _period) + getElapsedTime() + 1;
	}
	
	public boolean getInUse()
	{
		return _inUse;
	}
	
	public boolean setInUse(boolean inUse)
	{
		_inUse = inUse;
		if (_inUse)
			_startConditionsCorrect = onStart();
		else
			onExit();
		return _startConditionsCorrect;
	}
	
	public String getStackType()
	{
		return _stackType;
	}
	
	public float getStackOrder()
	{
		return _stackOrder;
	}
	
	public final L2Skill getSkill()
	{
		return _skill;
	}
	
	public final L2Character getEffector()
	{
		return _effector;
	}
	
	public final L2Character getEffected()
	{
		return _effected;
	}
	
	public boolean isSelfEffect()
	{
		return _skill._effectTemplatesSelf != null;
	}
	
	public boolean isHerbEffect()
	{
		if (getSkill().getName().contains("Herb"))
			return true;
		return false;
	}
	
	public boolean isCustomEffectToNotBeRemoved()
	{
		if (getSkill().getId() == 37513 || getSkill().getId() == 37512)
			return true;
		return false;
	}
	
	public final double calc()
	{
		Env env = new Env();
		env.player = _effector;
		env.target = _effected;
		env.skill = _skill;
		return _lambda.calc(env);
	}
	
	private synchronized void startEffectTask(int duration)
	{
		if (duration >= 0)
		{
			stopEffectTask();
			_currentTask = new EffectTask(duration, -1);
			_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask, duration);
		}
		if (_state == EffectState.ACTING)
		{
			_effected.addEffect(this);
		}
	}
	
	private synchronized void startEffectTaskAtFixedRate(int delay, int rate)
	{
		stopEffectTask();
		_currentTask = new EffectTask(delay, rate);
		_currentFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(_currentTask, delay, rate);
		if (_state == EffectState.ACTING)
			_effected.addEffect(this);
	}
	
	/**
	 * Stop the L2Effect task and send Server->Client update packet.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Cancel the effect in the the abnormal effect map of the L2Character</li>
	 * <li>Stop the task of the L2Effect, remove it and update client magic icon</li><BR>
	 * <BR>
	 */
	public final void exit()
	{
		exit(false);
	}
	
	public final void exit(boolean preventUpdate)
	{
		preventExitUpdate = preventUpdate;
		_state = EffectState.FINISHING;
		scheduleEffect();
	}
	
	/**
	 * Stop the task of the L2Effect, remove it and update client magic icon.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Cancel the task</li>
	 * <li>Stop and remove L2Effect from L2Character and update client magic icon</li><BR>
	 * <BR>
	 */
	public void stopEffectTask()
	{
		if (_currentFuture != null)
		{
			// Cancel the task
			_currentFuture.cancel(false);
			_currentFuture = null;
			_currentTask = null;
		}
		if (getEffected() != null)
			getEffected().removeEffect(this);
	}
	
	/** returns effect type */
	public abstract L2EffectType getEffectType();
	
	/** Notify started */
	public boolean onStart()
	{
		if (_abnormalEffect != AbnormalEffect.NULL)
			getEffected().startAbnormalEffect(_abnormalEffect);
		if (_specialEffect != AbnormalEffect.NULL)
			getEffected().startSpecialEffect(_specialEffect);
		return true;
	}
	
	/**
	 * Cancel the effect in the the abnormal effect map of the effected L2Character.<BR>
	 * <BR>
	 */
	public void onExit()
	{
		if (_abnormalEffect != AbnormalEffect.NULL)
			getEffected().stopAbnormalEffect(_abnormalEffect);
		if (_specialEffect != AbnormalEffect.NULL)
			getEffected().stopSpecialEffect(_specialEffect);
		if (_skill.getAfterEffectId() > 0 && getEffected() != null)
		{
			if (_skill.getId() == 16007 || _skill.getId() == 16008)
			{
				if (!_naturallyWornOff)
					return;
				L2ItemInstance wep = getEffected().getActiveWeaponInstance();
				if (wep != null && wep.isTimeLimitedItem())
				{
					wep.endOfLife();
				}
				wep = getEffected().getSecondaryWeaponInstance();
				if (wep != null && wep.isTimeLimitedItem())
				{
					wep.endOfLife();
				}
				if (getEffected() instanceof L2PcInstance)
				{
					L2ItemInstance armor = ((L2PcInstance) getEffected()).getChestArmorInstance();
					if (armor != null && armor.isTimeLimitedItem())
					{
						armor.endOfLife();
					}
				}
			}
			final L2Skill skill = SkillTable.getInstance().getInfo(_skill.getAfterEffectId(), _skill.getAfterEffectLvl());
			if (skill != null && !getEffected().isDead())
			{
				try
				{
					if (skill.getTargetType(getEffected()) == SkillTargetType.TARGET_ALL)
					{
						getEffected().broadcastPacket(new MagicSkillUse(getEffected(), getEffected(), skill.getDisplayId(), skill.getDisplayLvl(), skill.getHitTime(), 0));
						getEffected().broadcastPacket(new MagicSkillLaunched(getEffected(), skill.getDisplayId(), skill.getDisplayLvl(), skill.getTargetList(getEffected())));
						getEffected().callSkill(skill, skill.getTargetList(getEffected()));
					}
					else
					{
						getEffected().broadcastPacket(new MagicSkillUse(getEffected(), skill.getFirstOfTargetList(getEffected()), skill.getDisplayId(), skill.getLevel(), 0, 0));
						getEffected().broadcastPacket(new MagicSkillLaunched(getEffected(), skill.getDisplayId(), skill.getLevel(), skill.getTargetList(getEffected())));
						skill.getEffects(getEffector(), getEffected(), null, true);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	/** Return true for continuation of this effect */
	public abstract boolean onActionTime();
	
	public final void rescheduleEffect()
	{
		if (_state != EffectState.ACTING)
		{
			scheduleEffect();
		}
		else
		{
			if (_count > 1)
			{
				startEffectTaskAtFixedRate(5, _period * 1000);
				return;
			}
			else if (_period > 0)
			{
				if (_period < BUFFER_BUFFS_DURATION)
				{
					startEffectTask(_period * 1000);
				}
				else
				{
					_effected.addEffect(this);
				}
			}
		}
	}
	
	public final void scheduleEffect()
	{
		if (_state == EffectState.CREATED)
		{
			_state = EffectState.ACTING;
			if (_count > 1)
			{
				startEffectTaskAtFixedRate(5, _period * 1000);
				return;
			}
			if (_period > 0 || _period == -1)
			{
				if (_period < BUFFER_BUFFS_DURATION)
				{
					startEffectTask(_period * 1000);
					return;
				}
				else
				{
					_effected.addEffect(this);
					return;
				}
			}
			// effects not having count or period should start
			_startConditionsCorrect = onStart();
		}
		if (_state == EffectState.ACTING)
		{
			if (_period >= BUFFER_BUFFS_DURATION)
				return;
			if (_count-- > 0)
			{
				if (getInUse())
				{ // effect has to be in use
					if (onActionTime() && _startConditionsCorrect)
						return; // false causes effect to finish right away
				}
				else if (_count > 0)
				{ // do not finish it yet, in case reactivated
					return;
				}
			}
			_state = EffectState.FINISHING;
		}
		if (_state == EffectState.FINISHING)
		{
			// Cancel the effect in the the abnormal effect map of the L2Character
			if (getInUse() || !(_count > 1 || _period > 0))
				if (_startConditionsCorrect)
					onExit();
			// If the time left is equal to zero, send the message
			if (_count == 0 && _icon && _msg && !_naturallyWornOff && getEffected() instanceof L2PcInstance)
			{
				_naturallyWornOff = true;
				SystemMessage smsg3 = new SystemMessage(SystemMessageId.S1_HAS_WORN_OFF);
				smsg3.addSkillName(_skill);
				getEffected().sendPacket(smsg3);
			}
			// if task is null - stopEffectTask does not remove effect
			if (_currentFuture == null && getEffected() != null)
				getEffected().removeEffect(this);
			// Stop the task of the L2Effect, remove it and update client magic icon
			stopEffectTask();
		}
	}
	
	public Func[] getStatFuncs()
	{
		if (_funcTemplates == null)
			return _emptyFunctionSet;
		List<Func> funcs = new FastList<Func>();
		for (FuncTemplate t : _funcTemplates)
		{
			Env env = new Env();
			env.player = getEffector();
			env.target = getEffected();
			env.skill = getSkill();
			Func f = t.getFunc(env, this); // effect is owner
			if (f != null)
				funcs.add(f);
		}
		if (funcs.isEmpty())
			return _emptyFunctionSet;
		return funcs.toArray(new Func[funcs.size()]);
	}
	
	public final void addIcon(AbnormalStatusUpdate mi)
	{
		EffectTask task = _currentTask;
		ScheduledFuture<?> future = _currentFuture;
		if ((task == null || future == null) && _period < BUFFER_BUFFS_DURATION)
			return;
		if (_state == EffectState.FINISHING || _state == EffectState.CREATED)
			return;
		L2Skill sk = getSkill();
		if (task != null && task._rate > 0)
		{
			if (sk.isPotion())
				mi.addEffect(sk.getId(), getLevel(), sk.getBuffDuration() - (getTaskTime() * 1000));
			else
				mi.addEffect(sk.getId(), getLevel(), -1);
		}
		else if (future != null)
			mi.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_period == -1)
			mi.addEffect(sk.getId(), getLevel(), _period);
		else if (_period >= BUFFER_BUFFS_DURATION)
			mi.addEffect(sk.getId(), getLevel(), -1);
	}
	
	public final void addPartySpelledIcon(PartySpelled ps)
	{
		ScheduledFuture<?> future = _currentFuture;
		if (future == null && _period < BUFFER_BUFFS_DURATION)
			return;
		if (_state == EffectState.FINISHING || _state == EffectState.CREATED)
			return;
		L2Skill sk = getSkill();
		if (future != null)
			ps.addPartySpelledEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_period == -1)
			ps.addPartySpelledEffect(sk.getId(), getLevel(), _period);
		else if (_period >= BUFFER_BUFFS_DURATION)
			ps.addPartySpelledEffect(sk.getId(), getLevel(), -1);
	}
	
	public final void addOlympiadSpelledIcon(ExOlympiadSpelledInfo os)
	{
		ScheduledFuture<?> future = _currentFuture;
		if (future == null && _period < BUFFER_BUFFS_DURATION)
			return;
		if (_state == EffectState.FINISHING || _state == EffectState.CREATED)
			return;
		L2Skill sk = getSkill();
		if (future != null)
			os.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_period == -1)
			os.addEffect(sk.getId(), getLevel(), _period);
		else if (_period >= BUFFER_BUFFS_DURATION)
			os.addEffect(sk.getId(), getLevel(), -1);
	}
	
	public int getLevel()
	{
		return getSkill().getLevel();
	}
	
	public int getPeriodfirsttime()
	{
		return _periodfirsttime;
	}
	
	public void setPeriodfirsttime(int periodfirsttime)
	{
		_periodfirsttime = periodfirsttime;
	}
	
	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}
	
	public void setPeriodStartTicks(int periodStartTicks)
	{
		_periodStartTicks = periodStartTicks;
	}
	
	public EffectTemplate getEffectTemplate()
	{
		return _template;
	}
	
	public double getEffectPower()
	{
		return _effectPower;
	}
	
	public L2SkillType getSkillType()
	{
		return _effectSkillType;
	}
	
	/**
	 * Returns the elapsed time of the task.
	 *
	 * @return Time in seconds.
	 */
	public int getElapsedTaskTime()
	{
		return (getTotalCount() - _count) * _period + getElapsedTime() + 1;
	}
	
	public int getTotalTaskTime()
	{
		return getTotalCount() * _period;
	}
	
	public int getRemainingTaskTime()
	{
		return getTotalTaskTime() - getElapsedTaskTime();
	}
	
	public void cancel()
	{
		final L2Character effected = getEffected();
		if (effected != null)
			effected.removeEffect(this);
		if (effected instanceof L2PcInstance)
		{
			final L2PcInstance player = effected.getActingPlayer();
			final ClassId oldClassId = player.getClassId();
			ThreadPoolManager.getInstance().scheduleGeneral(() ->
			{
				if (oldClassId == player.getClassId() && _skill != null)
					player.addEffect(this);
			}, 10000);
		}
	}
}