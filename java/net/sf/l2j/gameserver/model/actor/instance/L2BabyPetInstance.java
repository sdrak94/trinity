/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.PetSkillsTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

/**
 *
 * This class ...
 *
 * @version $Revision: 1.15.2.10.2.16 $ $Date: 2005/04/06 16:13:40 $
 */
public final class L2BabyPetInstance extends L2PetInstance
{
	protected static final int BUFF_CONTROL = 5771;

	protected FastList<Integer> _buffs = null;
	protected int _majorHeal = 0;
	protected int _minorHeal = 0;
	protected int _recharge = 0;

	private Future<?> _castTask;

	protected long _buffControlTimestamp = 0;

	public L2BabyPetInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
	{
		super(objectId, template, owner, control);
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void onSpawn()
	{
		super.onSpawn();

		L2Skill skill;
		for (int id : PetSkillsTable.getInstance().getAvailableSkills(this))
		{
			double healPower = 0;

			skill = SkillTable.getInstance().getInfo(id, PetSkillsTable.getInstance().getAvailableLevel(L2BabyPetInstance.this, id));
			if (skill != null)
			{
				if (skill.getId() == BUFF_CONTROL)
					continue;

				switch (skill.getSkillType())
				{
					case HEAL:
						if (healPower == 0)
						{
							// set both heal types to the same skill
							_majorHeal = id;
							_minorHeal = id;
							healPower = skill.getPower();
						}
						else
						{
							// another heal skill found - search for most powerful
							if (skill.getPower() > healPower)
								_majorHeal = id;
							else
								_minorHeal = id;
						}
						break;
					case BUFF:
						if (_buffs == null)
							_buffs = new FastList<Integer>();
						_buffs.add(id);
						break;
					case MANAHEAL:
					case MANARECHARGE:
						_recharge = id;
						break;
				}
			}
		}
		startCastTask();
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		stopCastTask();
		return true;
	}

	@Override
	public synchronized void unSummon (L2PcInstance owner)
	{
		stopCastTask();
		abortCast();
		super.unSummon(owner);
	}

	@Override
	public void doRevive()
	{
		super.doRevive();
		startCastTask();
	}

	@Override
	public void onDecay()
	{
		super.onDecay();

		if (_buffs != null)
			_buffs.clear();
	}

	private final void startCastTask()
	{
		if (_majorHeal > 0 || _buffs != null || _recharge > 0)
			_castTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new CastTask(this), 3000, 1000);
	}

	private final void stopCastTask()
	{
		if (_castTask != null)
		{
			_castTask.cancel(false);
			_castTask = null;
		}
	}

	protected void castSkill(L2Skill skill)
	{
		// casting automatically stops any other action (such as autofollow or a move-to).
		// We need to gather the necessary info to restore the previous state.
		final boolean previousFollowStatus = getFollowStatus();

		// pet not following and owner outside cast range
		if (!previousFollowStatus && !isInsideRadius(getOwner(), skill.getCastRange(this), true, true))
			return;

		useMagic(skill, false, false);

		SystemMessage msg = new SystemMessage(SystemMessageId.PET_USES_S1);
		msg.addSkillName(skill);
		getOwner().sendPacket(msg);

		// calling useMagic changes the follow status, if the babypet actually casts
		// (as opposed to failing due some factors, such as too low MP, etc).
		// if the status has actually been changed, revert it.  Else, allow the pet to
		// continue whatever it was trying to do.
		// NOTE: This is important since the pet may have been told to attack a target.
		// reverting the follow status will abort this attack!  While aborting the attack
		// in order to heal is natural, it is not acceptable to abort the attack on its own,
		// merely because the timer stroke and without taking any other action...
		if(previousFollowStatus != getFollowStatus())
			setFollowStatus(previousFollowStatus);
	}

	private class CastTask implements Runnable
	{
		private final L2BabyPetInstance _baby;
		private final List<L2Skill> _currentBuffs = new FastList<L2Skill>();

		public CastTask(L2BabyPetInstance baby)
		{
			_baby = baby;
		}

		public void run()
		{
			L2PcInstance owner = _baby.getOwner();

			// if the owner is dead, merely wait for the owner to be resurrected
			// if the pet is still casting from the previous iteration, allow the cast to complete...
			if (owner != null
					&& !owner.isDead()
					&& !owner.isInvul()
					&& !_baby.isCastingNow()
					&& !_baby.isBetrayed()
					&& !_baby.isMuted()
					&& _baby.getAI().getIntention() != CtrlIntention.AI_INTENTION_CAST)
			{
				L2Skill skill = null;

				if (_majorHeal > 0)
				{
					// if the owner's HP is more than 80%, do nothing.
					// if the owner's HP is very low (less than 20%) have a high chance for strong heal
					// otherwise, have a low chance for weak heal
					final double hpPercent = owner.getCurrentHp()/owner.getMaxHp();
					if (hpPercent < 0.15
							&& !_baby.isSkillDisabled(_majorHeal)
							&& Rnd.get(100) <= 75)
						skill = SkillTable.getInstance().getInfo(_majorHeal, PetSkillsTable.getInstance().getAvailableLevel(_baby, _majorHeal));
					else if (hpPercent < 0.8
							&& !_baby.isSkillDisabled(_minorHeal)
							&& Rnd.get(100) <= 25)
						skill = SkillTable.getInstance().getInfo(_minorHeal, PetSkillsTable.getInstance().getAvailableLevel(_baby, _minorHeal));

					if (skill != null && _baby.getCurrentMp() >= skill.getMpConsume())
					{
						castSkill(skill);
						return;
					}
				}

				if (!_baby.isSkillDisabled(BUFF_CONTROL)) // Buff Control is not active
				{
					// searching for usable buffs
					if (_buffs != null && !_buffs.isEmpty())
					{
						for (int id : _buffs)
						{
							if (_baby.isSkillDisabled(id))
								continue;
							skill = SkillTable.getInstance().getInfo(id, PetSkillsTable.getInstance().getAvailableLevel(_baby, id));
							if (skill != null && _baby.getCurrentMp() >= skill.getMpConsume())
								_currentBuffs.add(skill);
						}
					}

					// buffs found, checking owner buffs
					if (!_currentBuffs.isEmpty())
					{
						L2Effect[] effects = owner.getAllEffects();
						Iterator<L2Skill> iter;
						L2Skill currentSkill;
						for (L2Effect e : effects)
						{
							if (e == null)
								continue;

							currentSkill = e.getSkill();
							// skipping debuffs, passives, toggles
							if (currentSkill.isDebuff()
									|| currentSkill.isPassive()
									|| currentSkill.isToggle())
								continue;

							// if buff does not need to be casted - remove it from list
							iter = _currentBuffs.iterator();
							while (iter.hasNext())
							{
								skill = iter.next();
								if (currentSkill.getId() == skill.getId()
										&& currentSkill.getLevel() >= skill.getLevel())
								{
									iter.remove();
								}
								else
								{
									// effect with same stacktype and greater or equal stackorder
									if (skill.hasEffects()
											&& !"none".equals(skill.getEffectTemplates()[0].stackType)
											&& e.getStackType().equals(skill.getEffectTemplates()[0].stackType)
											&& e.getStackOrder() >= skill.getEffectTemplates()[0].stackOrder)
									{
										iter.remove();
									}
								}
							}
							// no more buffs in list
							if (_currentBuffs.isEmpty())
								break;
						}
						// buffs list ready, casting random
						if (!_currentBuffs.isEmpty())
						{
							castSkill(_currentBuffs.get(Rnd.get(_currentBuffs.size())));
							_currentBuffs.clear();
							return;
						}
					}
				}

				// buffs/heal not casted, trying recharge, if exist
				if (_recharge > 0
						&& !_baby.isSkillDisabled(_recharge)
						&& owner.getCurrentMp()/owner.getMaxMp() < 0.7
						&& owner.isInCombat() // recharge casted only if owner in combat stance
						&& Rnd.get(100) <= 60)
				{
					skill = SkillTable.getInstance().getInfo(_recharge, PetSkillsTable.getInstance().getAvailableLevel(_baby, _recharge));
					if (skill != null && _baby.getCurrentMp() >= skill.getMpConsume());
					{
						castSkill(skill);
						return;
					}
				}
			}
		}
	}
}
