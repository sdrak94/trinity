package net.sf.l2j.gameserver.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbnormalStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PartySpelled;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class CharEffectList
{
	private static final L2Effect[]			EMPTY_EFFECTS			= new L2Effect[0];
	public final static int					BUFFER_BUFFS_DURATION	= 18000;				// 5 hr
	private FastList<L2Effect>				_buffs;
	private FastList<L2Effect>				_debuffs;
	// The table containing the List of all stacked effect in progress for each Stack group Identifier
	private Map<String, List<L2Effect>>		_stackedEffects;
	private boolean							_queuesInitialized		= false;
	private LinkedBlockingQueue<L2Effect>	_addQueue;
	private LinkedBlockingQueue<L2Effect>	_removeQueue;
	private final AtomicBoolean				queueLock				= new AtomicBoolean();
	// only party icons need to be updated
	private boolean							_partyOnly				= false;
	// Owner of this list
	private final L2Character				_owner;
	
	public CharEffectList(L2Character owner)
	{
		_owner = owner;
	}
	
	/**
	 * Returns all effects affecting stored in this CharEffectList
	 * 
	 * @return
	 */
	public final L2Effect[] getAllEffects()
	{
		// If no effect is active, return EMPTY_EFFECTS
		if ((_buffs == null || _buffs.isEmpty()) && (_debuffs == null || _debuffs.isEmpty()))
		{
			return EMPTY_EFFECTS;
		}
		// Create a copy of the effects
		FastList<L2Effect> temp = new FastList<L2Effect>();
		// Add all buffs and all debuffs
		if (_buffs != null)
		{
			synchronized (_buffs)
			{
				if (!_buffs.isEmpty())
					temp.addAll(_buffs);
			}
		}
		if (_debuffs != null)
		{
			synchronized (_debuffs)
			{
				if (!_debuffs.isEmpty())
					temp.addAll(_debuffs);
			}
		}
		// Return all effects in an array
		L2Effect[] tempArray = new L2Effect[temp.size()];
		temp.toArray(tempArray);
		return tempArray;
	}
	
	/**
	 * Returns the first effect matching the given EffectType
	 * 
	 * @param tp
	 * @return
	 */
	public final L2Effect getFirstEffect(L2EffectType tp)
	{
		L2Effect effectNotInUse = null;
		if (_buffs != null)
		{
			synchronized (_buffs)
			{
				if (!_buffs.isEmpty())
				{
					for (L2Effect e : _buffs)
					{
						if (e == null)
							continue;
						if (e.getEffectType() == tp)
						{
							if (e.getInUse())
								return e;
							else
								effectNotInUse = e;
						}
					}
				}
			}
		}
		if (effectNotInUse == null && _debuffs != null)
		{
			synchronized (_debuffs)
			{
				if (!_debuffs.isEmpty())
				{
					for (L2Effect e : _debuffs)
					{
						if (e == null)
							continue;
						if (e.getEffectType() == tp)
						{
							if (e.getInUse())
								return e;
							else
								effectNotInUse = e;
						}
					}
				}
			}
		}
		return effectNotInUse;
	}
	
	/**
	 * Returns the first effect matching the given L2Skill
	 * 
	 * @param skill
	 * @return
	 */
	public final L2Effect getFirstEffect(L2Skill skill)
	{
		L2Effect effectNotInUse = null;
		if (skill.isDebuff())
		{
			if (_debuffs == null)
				return null;
			synchronized (_debuffs)
			{
				if (_debuffs.isEmpty())
					return null;
				for (L2Effect e : _debuffs)
				{
					if (e == null)
						continue;
					if (e.getSkill().getId() == skill.getId())
					{
						if (e.getInUse())
							return e;
						else
							effectNotInUse = e;
					}
				}
			}
			return effectNotInUse;
		}
		else
		{
			if (_buffs == null)
				return null;
			synchronized (_buffs)
			{
				if (_buffs.isEmpty())
					return null;
				for (L2Effect e : _buffs)
				{
					if (e == null)
						continue;
					if (e.getSkill().getId() == skill.getId())
					{
						if (e.getInUse())
							return e;
						else
							effectNotInUse = e;
					}
				}
			}
			return effectNotInUse;
		}
	}
	
	/**
	 * Returns the first effect matching the given skillId
	 * 
	 * @param index
	 * @return
	 */
	public final L2Effect getFirstEffect(int skillId)
	{
		L2Effect effectNotInUse = null;
		if (_buffs != null)
		{
			synchronized (_buffs)
			{
				if (!_buffs.isEmpty())
				{
					for (L2Effect e : _buffs)
					{
						if (e == null)
							continue;
						if (e.getSkill().getId() == skillId)
						{
							if (e.getInUse())
								return e;
							else
								effectNotInUse = e;
						}
					}
				}
			}
		}
		if (effectNotInUse == null && _debuffs != null)
		{
			synchronized (_debuffs)
			{
				if (!_debuffs.isEmpty())
				{
					for (L2Effect e : _debuffs)
					{
						if (e == null)
							continue;
						if (e.getSkill().getId() == skillId)
						{
							if (e.getInUse())
								return e;
							else
								effectNotInUse = e;
						}
					}
				}
			}
		}
		return effectNotInUse;
	}
	
	/**
	 * Checks if the given skill stacks with an existing one.
	 *
	 * @param checkSkill
	 *            the skill to be checked
	 * @return Returns whether or not this skill will stack
	 */
	private boolean doesStack(L2Skill checkSkill)
	{
		if ((_buffs == null || _buffs.isEmpty()) || checkSkill._effectTemplates == null || checkSkill._effectTemplates.length < 1 || checkSkill._effectTemplates[0].stackType == null || "none".equals(checkSkill._effectTemplates[0].stackType))
		{
			return false;
		}
		String stackType = checkSkill._effectTemplates[0].stackType;
		for (L2Effect e : _buffs)
		{
			if (e.getStackType() != null && e.getStackType().equals(stackType))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return the number of buffs in this CharEffectList not counting Songs/Dances
	 * 
	 * @return
	 */
	@SuppressWarnings("incomplete-switch")
	public int getBuffCount()
	{
		if (_buffs == null)
			return 0;
		int buffCount = 0;
		synchronized (_buffs)
		{
			if (_buffs.isEmpty())
				return 0;
			for (L2Effect e : _buffs)
			{
				if (e != null && e.getShowIcon() && !e.getSkill().is7Signs() && e.getPeriod() == BUFFER_BUFFS_DURATION)
				{
					switch (e.getSkill().getSkillType())
					{
						case BUFF:
						case REFLECT:
						case HEAL_PERCENT:
						case MANAHEAL_PERCENT:
							buffCount++;
					}
				}
			}
		}
		return buffCount;
	}
	
	/**
	 * Exits all effects in this CharEffectList
	 */
	public final void stopAllEffects()
	{
		for (L2Effect e : getAllEffects())
		{
			if (e != null)
				e.exit(true);
		}
	}
	
	/**
	 * Exits all effects in this CharEffectList
	 */
	public final void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		// Get all active skills effects from this list
		L2Effect[] effects = getAllEffects();
		// Exit them
		for (L2Effect e : effects)
		{
			if (e != null && !e.getSkill().isStayAfterDeath())
				e.exit(true);
		}
	}
	
	/**
	 * Exit all effects having a specified type
	 * 
	 * @param type
	 */
	public final void stopEffects(L2EffectType type)
	{
		// Go through all active skills effects
		FastList<L2Effect> temp = new FastList<L2Effect>();
		if (_buffs != null)
		{
			synchronized (_buffs)
			{
				if (!_buffs.isEmpty())
				{
					for (L2Effect e : _buffs)
						// Get active skills effects of the selected type
						if (e != null && e.getEffectType() == type)
							temp.add(e);
				}
			}
		}
		if (_debuffs != null)
		{
			synchronized (_debuffs)
			{
				if (!_debuffs.isEmpty())
				{
					for (L2Effect e : _debuffs)
						// Get active skills effects of the selected type
						if (e != null && e.getEffectType() == type)
							temp.add(e);
				}
			}
		}
		if (temp != null && !temp.isEmpty())
		{
			for (L2Effect e : temp)
				if (e != null)
					e.exit();
		}
	}
	
	/**
	 * Exits all effects created by a specific skillId
	 * 
	 * @param skillId
	 */
	public final void stopSkillEffects(int skillId)
	{
		// Go through all active skills effects
		FastList<L2Effect> temp = new FastList<L2Effect>();
		if (_buffs != null)
		{
			synchronized (_buffs)
			{
				if (!_buffs.isEmpty())
				{
					for (L2Effect e : _buffs)
						if (e != null && e.getSkill().getId() == skillId)
							temp.add(e);
				}
			}
		}
		if (_debuffs != null)
		{
			synchronized (_debuffs)
			{
				if (!_debuffs.isEmpty())
				{
					for (L2Effect e : _debuffs)
						if (e != null && e.getSkill().getId() == skillId)
							temp.add(e);
				}
			}
		}
		if (temp != null && !temp.isEmpty())
		{
			for (L2Effect e : temp)
				if (e != null)
					e.exit();
		}
	}
	
	public void updateEffectIcons(boolean partyOnly)
	{
		if (_buffs == null && _debuffs == null)
			return;
		if (partyOnly)
			_partyOnly = true;
		queueRunner();
	}
	
	public void queueEffect(L2Effect effect, boolean remove)
	{
		if (effect == null)
			return;
		if (!_queuesInitialized)
			init();
		if (remove)
			_removeQueue.offer(effect);
		else
			_addQueue.offer(effect);
		queueRunner();
	}
	
	synchronized private void init()
	{
		_addQueue = new LinkedBlockingQueue<L2Effect>();
		_removeQueue = new LinkedBlockingQueue<L2Effect>();
		_queuesInitialized = true;
	}
	
	private void queueRunner()
	{
		if (!queueLock.compareAndSet(false, true))
			return;
		try
		{
			L2Effect effect;
			do
			{
				// remove has more priority than add
				// so removing all effects from queue first
				while ((effect = _removeQueue.poll()) != null)
				{
					removeEffectFromQueue(effect);
					_partyOnly = false;
				}
				if ((effect = _addQueue.poll()) != null)
				{
					addEffectFromQueue(effect);
					_partyOnly = false;
				}
			}
			while (!_addQueue.isEmpty() || !_removeQueue.isEmpty());
			updateEffectIcons();
		}
		finally
		{
			queueLock.set(false);
		}
	}
	
	protected void removeEffectFromQueue(L2Effect effect)
	{
		if (effect == null)
			return;
		FastList<L2Effect> effectList;
		if (effect.getSkill().isDebuff())
		{
			if (_debuffs == null)
				return;
			effectList = _debuffs;
		}
		else
		{
			if (_buffs == null)
				return;
			effectList = _buffs;
		}
		if ("none".equals(effect.getStackType()))
		{
			// Remove Func added by this effect from the L2Character Calculator
			_owner.removeStatsOwner(effect);
		}
		else
		{
			if (_stackedEffects == null)
				return;
			// Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
			List<L2Effect> stackQueue = _stackedEffects.get(effect.getStackType());
			if (stackQueue == null || stackQueue.isEmpty())
				return;
			int index = stackQueue.indexOf(effect);
			// Remove the effect from the stack group
			if (index >= 0)
			{
				stackQueue.remove(effect);
				// Check if the first stacked effect was the effect to remove
				if (index == 0)
				{
					// Remove all its Func objects from the L2Character calculator set
					_owner.removeStatsOwner(effect);
					// Check if there's another effect in the Stack Group
					if (!stackQueue.isEmpty())
					{
						L2Effect newStackedEffect = listsContains(stackQueue.get(0));
						if (newStackedEffect != null)
						{
							// Set the effect to In Use
							if (newStackedEffect.setInUse(true))
								// Add its list of Funcs to the Calculator set of the L2Character
								_owner.addStatFuncs(newStackedEffect.getStatFuncs());
						}
					}
				}
				if (stackQueue.isEmpty())
					_stackedEffects.remove(effect.getStackType());
				else
					// Update the Stack Group table _stackedEffects of the L2Character
					_stackedEffects.put(effect.getStackType(), stackQueue);
			}
		}
		// Remove the active skill L2effect from _effects of the L2Character
		if (effectList.remove(effect) && _owner instanceof L2PcInstance && effect.getShowIcon() && effect.getSendMessage() && !effect._naturallyWornOff)
		{
			effect._naturallyWornOff = true;
			SystemMessage sm;
			if (effect.getSkill().isToggle())
			{
				sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ABORTED);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.EFFECT_S1_DISAPPEARED);
			}
			sm.addSkillName(effect);
			_owner.sendPacket(sm);
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	final private void addEffectFromQueue(final L2Effect newEffect)
	{
		if (newEffect == null)
			return;
		final L2Skill newSkill = newEffect.getSkill();
		if (newSkill.isDebuff())
		{
			int count = -1000;
			if (newEffect.getEffectType() == L2EffectType.DEBUFF || newEffect.getEffectType() == L2EffectType.DMG_OVER_TIME || newEffect.getEffectType() == L2EffectType.STUN ||
			newEffect.getEffectType() == L2EffectType.PARALYZE || newEffect.getEffectType() == L2EffectType.MANA_DMG_OVER_TIME || newEffect.getEffectType() == L2EffectType.CHANCE_SKILL_TRIGGER ||
			newEffect.getEffectType() == L2EffectType.SILENCE_MAGIC_PHYSICAL || newEffect.getEffectType() == L2EffectType.MUTE || newEffect.getEffectTemplate().funcName.equalsIgnoreCase("debuff") ||
			newEffect.getSkill().getSkillType() == L2SkillType.DEBUFF || newEffect.getSkill().isDebuff2() || newEffect.getSkill().isDebuff())
				count = 0;
			final int debuffsMax = (int) (newEffect.getEffector().calcStat(Stats.MORE_DEBUFF, newEffect.getEffector().isAPC() ? 12 : 3, newEffect.getEffected(), newSkill) - newEffect.getEffected().calcStat(Stats.LESS_DEBUFF, 0, newEffect.getEffector(), newSkill));
			if (_debuffs == null)
				_debuffs = new FastList<L2Effect>();
			for (L2Effect e : _debuffs)
			{
				if (e != null && e.getSkill().getId() == newEffect.getSkill().getId() && e.getEffectType() == newEffect.getEffectType() && e.getStackOrder() == newEffect.getStackOrder() && e.getStackType().equals(newEffect.getStackType()))
				{
					// Started scheduled timer needs to be canceled.
					newEffect.stopEffectTask();
					return;
				}
				if (count >= 0)
				{
					if (e.getEffectType() == L2EffectType.DEBUFF || e.getEffectType() == L2EffectType.DMG_OVER_TIME || newEffect.getEffectType() == L2EffectType.STUN ||
					newEffect.getEffectType() == L2EffectType.PARALYZE || e.getEffectType() == L2EffectType.MANA_DMG_OVER_TIME || e.getEffectType() == L2EffectType.CHANCE_SKILL_TRIGGER ||
					newEffect.getEffectType() == L2EffectType.SILENCE_MAGIC_PHYSICAL || newEffect.getEffectType() == L2EffectType.MUTE || newEffect.getEffectTemplate().funcName.equalsIgnoreCase("debuff") ||
					newEffect.getSkill().getSkillType() == L2SkillType.DEBUFF || newEffect.getSkill().isDebuff2() || newEffect.getSkill().isDebuff())
					{
						count++;
						if (count >= debuffsMax)
						{
							newEffect.getEffector().sendMessage(newEffect.getEffected().getDisplayName() + " wasn't affected by your " + newEffect.getSkill().getName() + " because of " + debuffsMax + " debuffs limit.");
							newEffect.getEffected().sendMessage("You weren't affected by " + newEffect.getSkill().getName() + " because of " + debuffsMax + " debuffs limit.");
							newEffect.stopEffectTask();
							return;
						}
					}
				}
			}
			_debuffs.addLast(newEffect);
		}
		else
		{
			if (_owner.getMaxBuffCount() == 0 && newEffect.getPeriod() == BUFFER_BUFFS_DURATION)
			{
				newEffect.stopEffectTask();
				return;
			}
			if (_buffs == null)
				_buffs = new FastList<L2Effect>();
			for (L2Effect e : _buffs)
			{
				if (e != null && e.getSkill().getId() == newEffect.getSkill().getId() && e.getEffectType() == newEffect.getEffectType() && e.getStackOrder() == newEffect.getStackOrder() && e.getStackType().equalsIgnoreCase(newEffect.getStackType()))
				{
					e.exit(); // exit this
				}
			}
			// Remove first buff when buff list is full
			if (!doesStack(newSkill) && !newSkill.is7Signs())
			{
				int effectsToRemove = getBuffCount() - _owner.getMaxBuffCount();
				if (effectsToRemove >= 0 && newEffect.getPeriod() == BUFFER_BUFFS_DURATION)
				{
					switch (newSkill.getSkillType())
					{
						case BUFF:
						case REFLECT:
						case HEAL_PERCENT:
						case MANAHEAL_PERCENT:
						{
							for (L2Effect e : _buffs)
							{
								if (e == null)
									continue;
								switch (e.getSkill().getSkillType())
								{
									case BUFF:
									case REFLECT:
									case HEAL_PERCENT:
									case MANAHEAL_PERCENT:
									{
										if (e.getPeriod() == BUFFER_BUFFS_DURATION)
										{
											e.exit();
											effectsToRemove--;
										}
										break; // break switch()
									}
									default:
										continue; // continue for()
								}
								if (effectsToRemove < 0)
									break; // break for()
							}
						}
					}
				}
			}
			// Icons order: buffs, 7s, toggles, dances
			if (newSkill.isDance())
				_buffs.addLast(newEffect);
			else
			{
				int pos = 0;
				if (newSkill.isToggle())
				{
					// toggle skill - before all dances
					for (L2Effect e : _buffs)
					{
						if (e == null)
							continue;
						if (e.getSkill().isDance())
							break;
						pos++;
					}
				}
				else
				{
					// normal buff - before toggles and 7s and dances
					for (L2Effect e : _buffs)
					{
						if (e == null)
							continue;
						if (e.getSkill().isToggle() || e.getSkill().is7Signs() || e.getSkill().isDance())
							break;
						pos++;
					}
				}
				_buffs.add(pos, newEffect);
			}
		}
		// Check if a stack group is defined for this effect
		if ("none".equals(newEffect.getStackType()))
		{
			// Set this L2Effect to In Use
			if (newEffect.setInUse(true))
				// Add Funcs of this effect to the Calculator set of the L2Character
				_owner.addStatFuncs(newEffect.getStatFuncs());
			return;
		}
		List<L2Effect> stackQueue;
		L2Effect effectToAdd = null;
		L2Effect effectToRemove = null;
		if (_stackedEffects == null)
			_stackedEffects = new FastMap<String, List<L2Effect>>();
		// Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
		stackQueue = _stackedEffects.get(newEffect.getStackType());
		if (stackQueue != null)
		{
			int pos = 0;
			if (!stackQueue.isEmpty())
			{
				// Get the first stacked effect of the Stack group selected
				effectToRemove = listsContains(stackQueue.get(0));
				// Create an Iterator to go through the list of stacked effects in progress on the L2Character
				Iterator<L2Effect> queueIterator = stackQueue.iterator();
				while (queueIterator.hasNext())
				{
					if (newEffect.getStackOrder() < queueIterator.next().getStackOrder())
						pos++;
					else
						break;
				}
				// Add the new effect to the Stack list in function of its position in the Stack group
				stackQueue.add(pos, newEffect);
				// skill.exit() could be used, if the users don't wish to see "effect
				// removed" always when a timer goes off, even if the buff isn't active
				// any more (has been replaced). but then check e.g. npc hold and raid petrification.
				if (Config.EFFECT_CANCELING && !newEffect.isHerbEffect() && stackQueue.size() > 1)
				{
					if (newSkill.isDebuff())
					{
						_debuffs.remove(stackQueue.remove(1));
					}
					else
					{
						_buffs.remove(stackQueue.remove(1));
					}
				}
			}
			else
				stackQueue.add(0, newEffect);
		}
		else
		{
			stackQueue = new FastList<L2Effect>();
			stackQueue.add(0, newEffect);
		}
		// Update the Stack Group table _stackedEffects of the L2Character
		_stackedEffects.put(newEffect.getStackType(), stackQueue);
		// Get the first stacked effect of the Stack group selected
		if (stackQueue != null && !stackQueue.isEmpty())
		{
			effectToAdd = listsContains(stackQueue.get(0));
		}
		if (effectToRemove != effectToAdd)
		{
			if (effectToRemove != null)
			{
				// Remove all Func objects corresponding to this stacked effect from the Calculator set of the L2Character
				_owner.removeStatsOwner(effectToRemove);
				// Set the L2Effect to Not In Use
				effectToRemove.setInUse(false);
			}
			if (effectToAdd != null)
			{
				// Set this L2Effect to In Use
				if (effectToAdd.setInUse(true))
					// Add all Func objects corresponding to this stacked effect to the Calculator set of the L2Character
					_owner.addStatFuncs(effectToAdd.getStatFuncs());
			}
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	protected void updateEffectIcons()
	{
		if (_owner == null || !(_owner instanceof L2Playable))
			return;
		AbnormalStatusUpdate mi = null;
		PartySpelled ps = null;
		if (_owner instanceof L2PcInstance)
		{
			if (_partyOnly)
				_partyOnly = false;
			else
				mi = new AbnormalStatusUpdate();
			if (_owner.isInParty())
				ps = new PartySpelled(_owner);
		}
		else if (_owner instanceof L2Summon)
			ps = new PartySpelled(_owner);
		if (_buffs != null && !_buffs.isEmpty())
		{
			synchronized (_buffs)
			{
				for (L2Effect e : _buffs)
				{
					if (e == null || !e.getShowIcon())
						continue;
					switch (e.getEffectType())
					{
						case CHARGE: // handled by EtcStatusUpdate
						case SIGNET_GROUND:
							continue;
					}
					if (e.getInUse())
					{
						if (mi != null)
							e.addIcon(mi);
						if (ps != null)
							e.addPartySpelledIcon(ps);
					}
				}
			}
		}
		if (_debuffs != null && !_debuffs.isEmpty())
		{
			synchronized (_debuffs)
			{
				for (L2Effect e : _debuffs)
				{
					if (e == null || !e.getShowIcon())
						continue;
					switch (e.getEffectType())
					{
						case SIGNET_GROUND:
							continue;
					}
					if (e.getInUse())
					{
						if (mi != null)
							e.addIcon(mi);
						if (ps != null)
							e.addPartySpelledIcon(ps);
					}
				}
			}
		}
		if (mi != null)
			_owner.sendPacket(mi);
		if (ps != null)
		{
			if (_owner instanceof L2Summon)
			{
				L2PcInstance summonOwner = ((L2Summon) _owner).getOwner();
				if (summonOwner != null)
				{
					if (summonOwner.isInParty())
						summonOwner.getParty().broadcastToPartyMembers(ps);
					else
						summonOwner.sendPacket(ps);
				}
			}
			else if (_owner instanceof L2PcInstance && _owner.isInParty())
				_owner.getParty().broadcastToPartyMembers(ps);
		}
	}
	
	/**
	 * Returns effect if contains in _buffs or _debuffs and null if not found
	 * 
	 * @param effect
	 * @return
	 */
	private L2Effect listsContains(L2Effect effect)
	{
		if (_buffs != null && !_buffs.isEmpty() && _buffs.contains(effect))
			return effect;
		if (_debuffs != null && !_debuffs.isEmpty() && _debuffs.contains(effect))
			return effect;
		return null;
	}
	
	/**
	 * Clear and null all queues and lists
	 * Use only during delete character from the world.
	 */
	public void clear()
	{
		try
		{
			stopAllEffects();
			if (_addQueue != null)
			{
				_addQueue.clear();
				_addQueue = null;
			}
			if (_removeQueue != null)
			{
				_removeQueue.clear();
				_removeQueue = null;
			}
			_queuesInitialized = false;
			if (_buffs != null)
			{
				_buffs.clear();
				_buffs = null;
			}
			if (_debuffs != null)
			{
				_debuffs.clear();
				_debuffs = null;
			}
			if (_stackedEffects != null)
			{
				_stackedEffects.clear();
				_stackedEffects = null;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
