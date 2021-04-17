package net.sf.l2j.gameserver.handler.skillhandlers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.CharEffectList;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.effects.EffectBuff;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class Disablers implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.STUN,
	L2SkillType.ROOT,
	L2SkillType.SLEEP,
	L2SkillType.CONFUSION,
	L2SkillType.AGGDAMAGE,
	L2SkillType.AGGREDUCE,
	L2SkillType.AGGREDUCE_CHAR,
	L2SkillType.AGGREMOVE,
	L2SkillType.MUTE,
	L2SkillType.CONFUSE_MOB_ONLY,
	L2SkillType.NEGATE,
	L2SkillType.CANCEL,
	L2SkillType.CANCEL_DEBUFF,
	L2SkillType.PARALYZE,
	L2SkillType.ERASE,
	L2SkillType.MAGE_BANE,
	L2SkillType.WARRIOR_BANE,
	L2SkillType.BETRAY,
	L2SkillType.DISARM,
	L2SkillType.SWITCH,
	L2SkillType.STEAL_BUFF,
	L2SkillType.PROC
};

protected static final Logger _log = Logger.getLogger(L2Skill.class.getName());

@SuppressWarnings({ "incomplete-switch", "unused" })
public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	final L2SkillType type = skill.getSkillType();
	
	byte shld = 0;
	
	for (L2Character target: (L2Character[]) targets)
	{
		if (target == null || target.isDead() || target.isInvul() || target.isPreventedFromReceivingBuffs()) //bypass if target is null, invul or dead
			continue;
		
		if (skill.isOffensive() && target.isPreventedFromReceivingDebuffs())
			continue;
		
		shld = Formulas.calcShldUse(activeChar, target, skill);
		
		switch (type)
		{
		case BETRAY:
		{
			if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
				sm.addCharName(target);
				sm.addSkillName(skill);
				activeChar.sendPacket(sm);
			}
			break;
		}
		/*		case FAKE_DEATH:
		{
			// stun/fakedeath is not mdef dependant, it depends on lvl difference, target CON and power of stun
			skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			break;
		}*/
		case DISARM:
		{
			if (target instanceof L2PcInstance)
			{
				if (target.getActingPlayer().isCombatFlagEquipped())
				{
					if (activeChar.getTarget() != null && target == activeChar.getTarget())
						activeChar.sendMessage("You cannot disarm combat flags");
					
					return;
				}
			}
		}
		case ROOT:
		case STUN:
		{
			if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
				target = activeChar;
			
			if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			else
			{
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
			}
			break;
		}
		case SLEEP:
		case PARALYZE: //use same as root for now
		{
			if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
				target = activeChar;
			
			if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			else
			{
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
			}
			break;
		}
		case CONFUSION:
		case MUTE:
		{
			if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
				target = activeChar;
			
			if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
			{
				// stop same type effect if available
				L2Effect[] effects = target.getAllEffects();
				for (L2Effect e : effects)
				{
					if (e.getSkill().getSkillType() == type)
						e.exit();
				}
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			}
			else
			{
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
			}
			break;
		}
		case CONFUSE_MOB_ONLY:
		{
			// do nothing if not on mob
			if (target instanceof L2MonsterInstance)
			{
				if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
				{
					L2Effect[] effects = target.getAllEffects();
					for (L2Effect e : effects)
					{
						if (e.getSkill().getSkillType() == type)
							e.exit();
					}
					skill.getEffects(activeChar, target, new Env(shld, true, true, true));
				}
				else
				{
					if (activeChar instanceof L2PcInstance)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
						sm.addCharName(target);
						sm.addSkillName(skill);
						activeChar.sendPacket(sm);
					}
				}
			}
			else
				activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			break;
		}
		case AGGDAMAGE:
		{
			if (target instanceof L2Attackable)
			{
				try
				{
					if (!activeChar.isGM() && target.getAI().getAttackTarget() != null)
					{
						L2Character mobAttackTarget = target.getAI().getAttackTarget();
						
						if (mobAttackTarget.getActingPlayer() != activeChar.getActingPlayer()/* || (activeChar.getPet() != null && targetb != activeChar.getPet())*/)
						{
							if ((activeChar.getActingPlayer().getClanId() == 0 && mobAttackTarget.getActingPlayer().getClanId() == 0) ||
									activeChar.getActingPlayer().getClanId() != mobAttackTarget.getActingPlayer().getClanId())
							{
								if (activeChar.getParty() == null)
									return;
								if (mobAttackTarget.getParty() == null)
									return;
								if (activeChar.getParty().getPartyLeaderOID() != mobAttackTarget.getParty().getPartyLeaderOID())
									return;
							}
						}
					}
				}
				catch (Exception e)
				{    break;    }
				
				int numba = (activeChar instanceof L2PcInstance && activeChar.getActingPlayer().isTankClass()) ? 152 : 75;
				
				if (target.isRaid())
					numba = 152;
				else if (skill.getId() == 18)
					numba = 50;
				
				final int aggression = (int) ((numba * skill.getPower(activeChar) * activeChar.calcStat(Stats.AGGRESSION_PROF, 1, null, skill)) / target.getLevel());
				
				if (aggression > 0)
					target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, aggression);
				
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			}
			else
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			
			break;
		}
		case AGGREDUCE:
		{
			/*			// these skills needs to be rechecked
			if (target instanceof L2MonsterInstance)
			{
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
				
				double aggdiff = ((L2Attackable) target).getHating(activeChar) - target.calcStat(Stats.AGGRESSION, ((L2Attackable) target).getHating(activeChar), target, skill);
				
				if (skill.getPower(activeChar) > 0)
					((L2Attackable) target).reduceHate(null, (int) skill.getPower(activeChar));
				else if (aggdiff > 0)
					((L2Attackable) target).reduceHate(null, (int) aggdiff);
			}*/
			skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			
			break;
		}
		case AGGREDUCE_CHAR:
		{
			if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			else if (activeChar instanceof L2PcInstance)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
				sm.addCharName(target);
				sm.addSkillName(skill);
				activeChar.sendPacket(sm);
			}
			
			target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
			// these skills needs to be rechecked
			/*if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
			{
				if (target instanceof L2MonsterInstance)
				{
					L2Attackable targ = (L2Attackable) target;
					targ.stopHating(activeChar);
					if (targ.getMostHated() == null)
					{
						((L2AttackableAI) targ.getAI()).setGlobalAggro(-25);
						targ.clearAggroList();
						targ.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
						targ.setWalking();
					}
				}
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			}
			else
			{
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
			}*/
			break;
		}
		case AGGREMOVE:
		{
			/*					// these skills needs to be rechecked
					if (target instanceof L2MonsterInstance && !target.isRaid())
					{
						if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
						{
							if (skill.getTargetType() == L2Skill.SkillTargetType.TARGET_UNDEAD)
							{
								if (target.isUndead())
									((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
							}
							else
								((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
						}
						else
						{
							if (activeChar instanceof L2PcInstance)
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
								sm.addCharName(target);
								sm.addSkillName(skill);
								activeChar.sendPacket(sm);
							}
							target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
						}
					}
					else
						target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);*/
			break;
		}
		case ERASE:
		{
			if (Formulas.calcSkillSuccess(activeChar, target, skill, shld)		// doesn't affect siege golem or wild hog cannon
					&& !(target instanceof L2SiegeSummonInstance))
			{
				L2PcInstance summonOwner = null;
				L2Summon summonPet = null;
				summonOwner = ((L2Summon) target).getOwner();
				summonPet = summonOwner.getPet();
				if (summonPet != null)
				{
					summonPet.unSummon(summonOwner);
					SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED);
					summonOwner.sendPacket(sm);
				}
			}
			else
			{
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
			}
			break;
		}
		case MAGE_BANE:
		{
			if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
				target = activeChar;
			
			if (!Formulas.calcSkillSuccess(activeChar, target, skill, shld))
			{
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
				continue;
			}
			
			L2Effect[] effects = target.getAllEffects();
			for (L2Effect e : effects)
			{
				if (e.getStackType().equals("casting_time_down") || e.getStackType().equals("ma_up"))
					e.exit();
			}
			
			break;
		}
		case WARRIOR_BANE:
		{
			if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
				target = activeChar;
			
			if (!Formulas.calcSkillSuccess(activeChar, target, skill, shld))
			{
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
				continue;
			}
			
			L2Effect[] effects = target.getAllEffects();
			for (L2Effect e : effects)
			{
				if (e.getStackType().equals("speed_up") || e.getStackType().equals("attack_time_down"))
					e.exit();
			}
			break;
		}
		case CANCEL_DEBUFF:
		{
			L2Effect[] effects = target.getAllEffects();
			
			if (effects.length == 0 || effects == null)
				break;
			
			int count = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
			for (L2Effect e : effects)
			{
				if (e != null && e.getSkill().isDebuff() && count < skill.getMaxNegatedEffects())
				{
					if (e.getSkill().getAbnormalLvl() < 100)
					{
						//Do not remove raid curse skills
						if (e.getSkill().getId() != 4215 && e.getSkill().getId() != 4515 && e.getSkill().getId() != 4082)
						{
							e.exit();
							if (count > -1)
								count++;
						}
					}
				}
			}
			
			break;
		}
		case STEAL_BUFF:
		{
			if (!(target instanceof L2PcInstance))
				return;
			
			L2Effect[] effects = target.getAllEffects();
			
			if (effects == null || effects.length < 1)
				return;
			
			// Reversing array
			List<L2Effect> list = Arrays.asList(effects);
			Collections.reverse(list);
			list.toArray(effects);
			
			FastList<L2Effect> toSteal = new FastList<L2Effect>();
			int count = 0;
			int lastSkill = 0;
			
			for (L2Effect e : effects)
			{
				if (e == null
						|| !(e instanceof EffectBuff)
						|| e.getSkill().getSkillType() == L2SkillType.HEAL
						|| e.getSkill().isToggle()
						|| e.getSkill().isDebuff()
						|| e.getPeriod() == CharEffectList.BUFFER_BUFFS_DURATION
						|| e.getSkill().isHeroSkill()
						)
					continue;
				
				if (e.getSkill().getId() == lastSkill)
				{
					if (count == 0) count = 1;
					toSteal.add(e);
				}
				else if (count < skill.getPower(activeChar))
				{
					toSteal.add(e);
					count++;
				}
				else
					break;
			}
			if (!toSteal.isEmpty())
				stealEffects(activeChar, target, toSteal);
			break;
		}
		case CANCEL:
		{
			if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
				target = activeChar;
			
			if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
			{
				L2Effect[] effects = target.getAllEffects();
				
				double max = skill.getMaxNegatedEffects();
				if (max == 0)
					max = Integer.MAX_VALUE; //this is for RBcancells and stuff...
				
				if (effects.length >= max)
					effects = SortEffects(effects);
				
				//for(int i = 0; i < effects.length;i++)
				//    activeChar.sendMessage(Integer.toString(effects[i].getSkill().getMagicLevel()));
				
				double count = 1;
				
				for (L2Effect e : effects)
				{
					if (e.getPeriod() == CharEffectList.BUFFER_BUFFS_DURATION)
						continue;
					
					// do not delete signet effects!
					switch (e.getEffectType())
					{
					case SIGNET_GROUND:
					case SIGNET_EFFECT:
					case DISGUISE:
						continue;
					}
					
					switch(e.getSkill().getId())
					{
					case 4082:
					case 4215:
					case 4515:
					case 5182:
					case 110:
					case 111:
					case 1323:
					case 1325:
						continue;
					}
					
					switch (e.getSkill().getSkillType())
					{
					case BUFF:
					case HEAL_PERCENT:
					case REFLECT:
					case COMBATPOINTHEAL:
						break;
					default:
						continue;
					}
					
					double rate = 1 - (count / max);
					if (rate < 0.33)
						rate = 0.33;
					else if (rate > 0.95)
						rate = 0.95;
					if (Rnd.get(1000) < (rate * 1000))
						e.exit();
					if (count == max)
						break;
					count++;
				}
				
				if (skill.hasEffects())
				{
					skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(skill);
					target.sendPacket(sm);
				}
			}
			else
			{
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
			}
			
			break;
		}
		case SWITCH: //custom edit
		{
			if (target != null)
			{
				if (target instanceof L2PcInstance)
				{
					L2PcInstance lider = (L2PcInstance)target;
					
					if (!activeChar.isGM())
					{
						if (!lider.canBeTricked(0))
						{
							activeChar.sendMessage(lider.getDisplayName()+" is not affected by your "+skill.getName()+" because of 12 second trick/switch/aggro protection");
							return;
						}
						
						if (lider._inEventFOS || SiegeManager.getInstance().getSiege(lider) != null)
						{
							if ((lider.isAFOSLeader() || lider.isClanLeader()) && lider.isCastingNow())
							{
								if (lider.getCurrentSkill() != null && lider.getCurrentSkill().getSkillId() == 246) //seal of ruler
								{
									SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
									sm.addCharName(target);
									sm.addSkillName(skill.getId());
									activeChar.sendPacket(sm);
									return;
								}
							}
						}
					}
					
					lider.setTrickProtectionTime(activeChar instanceof L2PcInstance ? activeChar.getObjectId() : 0);
					
					FastList<L2PcInstance> players = new FastList<L2PcInstance>();
					FastList<L2PcInstance> players2 = new FastList<L2PcInstance>();
					
					double closestDistance = 600;
					double tempDistance = 0;
					
					for (L2PcInstance player : target.getKnownList().getKnownPlayersInRadius(600))
					{
						if (player != null)
						{
							if (player == target || player == activeChar || (target.getTarget() != null && player == target.getTarget()))
								continue;
							
							if (player.isAutoAttackableSwitch((L2PcInstance)target) && ((L2PcInstance)target).canAttack(player, false))
							{
								tempDistance = Util.calculateDistance(target, player, false);
								
								if (tempDistance <= closestDistance)
								{
									closestDistance = tempDistance;
									players.addFirst(player);
								}
							}
							else
							{
								if (players2.size() < 10)
								{
									if (!player.isInvisible() && !player.isAlikeDead())
										players2.add(player);
								}
							}
						}
					}
					
					if (players.size() > 0)
					{
						L2PcInstance toBeTargated = players.get(0);
						
						if (toBeTargated != null)
						{
							if (((L2PcInstance)target).isSitting())
								((L2PcInstance)target).standUp();
							else
								target.abortCast();
							
							if (!target.isRunning())
								target.setRunning();
							
							((L2PcInstance)target).setIsSelectingTarget(skill.getNextDanceMpCost());
							
							target.setKnockedbackTimer(GameTimeController.getGameTicks() + skill.getNextDanceMpCost());
							target.setTarget(toBeTargated);
							target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, toBeTargated);
							
							if (!(activeChar instanceof L2PcInstance && activeChar.getActingPlayer().isInvisible()))
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
								sm.addSkillName(skill.getId());
								target.sendPacket(sm);
							}
						}
					}
					else
					{
						if (players2.size() > 0) //these ones are not auto hittable
						{
							int num = Rnd.nextInt(players2.size());
							L2PcInstance toBeTargated = players2.get(num);
							
							if (toBeTargated != null)
							{
								if (((L2PcInstance)target).isSitting())
									((L2PcInstance)target).standUp();
								else
								{
									target.abortAttack();
									target.abortCast();
								}
								
								if (!target.isRunning())
									target.setRunning();
								
								((L2PcInstance)target).setIsSelectingTarget(skill.getNextDanceMpCost());
								
								target.setKnockedbackTimer(GameTimeController.getGameTicks() + skill.getNextDanceMpCost());
								target.setTarget(toBeTargated);
								target.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, toBeTargated);
								
								if (!(activeChar instanceof L2PcInstance && activeChar.getActingPlayer().isInvisible()))
								{
									SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
									sm.addSkillName(skill.getId());
									target.sendPacket(sm);
								}
							}
						}
						else
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
							sm.addCharName(target);
							sm.addSkillName(skill.getId());
							activeChar.sendPacket(sm);
						}
					}
					
					if (skill.hasEffects())
						skill.getEffects(activeChar, target);
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				
				if (skill.hasEffects())
				{
					skill.getEffects(activeChar, target, new Env(shld, true, true, true));
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(skill);
					target.sendPacket(sm);
				}
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			}
			
			break;
		}
		case CANCEL_STATS: // same than CANCEL but
		{
			if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
				target = activeChar;
			
			if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
			{
				L2Effect[] effects = target.getAllEffects();
				
				double max = skill.getMaxNegatedEffects();
				if (max == 0)
					max = Integer.MAX_VALUE; //this is for RBcancells and stuff...
				
				if (effects.length >= max)
					effects = SortEffects(effects);
				
				//for(int i = 0; i < effects.length;i++)
				//    activeChar.sendMessage(Integer.toString(effects[i].getSkill().getMagicLevel()));
				
				double count = 1;
				
				for (L2Effect e : effects)
				{
					// do not delete signet effects!
					switch (e.getEffectType())
					{
					case SIGNET_GROUND:
					case SIGNET_EFFECT:
					case DISGUISE:
						continue;
					}
					
					switch(e.getSkill().getId())
					{
					case 4082:
					case 4215:
					case 4515:
					case 5182:
					case 110:
					case 111:
					case 1323:
					case 1325:
						continue;
					}
					
					switch (e.getSkill().getSkillType())
					{
					case BUFF:
					case HEAL_PERCENT:
					case REFLECT:
					case COMBATPOINTHEAL:
						break;
					default:
						continue;
					}
					
					double rate = 1 - (count / max);
					if (rate < 0.33)
						rate = 0.33;
					else if (rate > 0.95)
						rate = 0.95;
					if (Rnd.get(1000) < (rate * 1000))
					{
						boolean exit = false;
						for (L2SkillType skillType : skill.getNegateStats())
						{
							if (skillType == e.getSkillType())
							{
								exit = true;
								break;
							}
						}
						
						if (exit)
						{
							e.exit();
							if (count == max)
								break;
							
							count++;
						}
					}
				}
			}
			else
			{
				if (activeChar instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
				}
			}
			
			break;
		}
		case NEGATE:
		{
			if (skill.getNegateId().length != 0)
			{
				for (int i = 0; i < skill.getNegateId().length; i++)
				{
					if (skill.getNegateId()[i] != 0)
						target.stopSkillEffects(skill.getNegateId()[i]);
				}
			}					// all others negate type skills
			else
			{
				int removedBuffs = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
				
				for (L2SkillType skillType : skill.getNegateStats())
				{
					if (removedBuffs > skill.getMaxNegatedEffects())
						break;
					
					switch(skillType)
					{
					case BUFF:
						int lvlmodifier = 52 + skill.getMagicLevel() * 2;
						if (skill.getMagicLevel() == 12)
							lvlmodifier = (Experience.MAX_LEVEL - 1);
						int landrate = 90;
						if ((target.getLevel() - lvlmodifier) > 0)
							landrate = 90 - 4 * (target.getLevel() - lvlmodifier);
						
						landrate = (int) activeChar.calcStat(Stats.CANCEL_VULN, landrate, target, null);
						
						if (Rnd.get(100) < landrate)
							removedBuffs += negateEffect(target, L2SkillType.BUFF, skill.getNegateLvl(), skill.getMaxNegatedEffects());
						break;
					case HEAL:
						ISkillHandler Healhandler = SkillHandler.getInstance().getSkillHandler(L2SkillType.HEAL);
						if (Healhandler == null)
						{
							_log.severe("Couldn't find skill handler for HEAL.");
							continue;
						}
						L2Character tgts[] = new L2Character[]{target};
						try
						{
							Healhandler.useSkill(activeChar, skill, tgts);
						}
						catch (IOException e)
						{
							_log.log(Level.WARNING, "", e);
						}
						break;
					default:
						removedBuffs += negateEffect(target, skillType, skill.getNegateLvl(), skill.getMaxNegatedEffects());
						break;
					}//end switch
				}//end for
			}//end else
			if (skill.hasEffects())
			{
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			}
		}
		case PROC:
		{
			int chance = 100;
			
			if (target instanceof L2Attackable)
			{
				if (target.getLevel() >= 95)
					chance = 25;
				else if (target.getLevel() >= 94)
					chance = 40;
				else if (target.getLevel() >= 93)
					chance = 55;
				else if (target.getLevel() >= 92)
					chance = 70;
				else if (target.getLevel() >= 91)
					chance = 85;
				
				if (target instanceof L2RaidBossInstance)
				{
					chance -= 15;
				}
			}
			
			if (chance >= 100 || Rnd.get(100) < chance)
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			
			break;
		}// end case
		}//end switch
		
		//Possibility of a lethal strike
		Formulas.calcLethalHit(activeChar, target, skill);
		
	}//end for
	
	skill.getEffectsSelf(activeChar);
} //end void

/**
 * 
 * @param target
 * @param type
 * @param power
 * @param maxRemoved
 * @return
 */
private int negateEffect(L2Character target, L2SkillType type, double power, int maxRemoved)
{
	return negateEffect(target, type, power, 0, maxRemoved);
}

/**
 * 
 * @param target
 * @param type
 * @param power
 * @param skillId
 * @param maxRemoved
 * @return
 */
private int negateEffect(L2Character target, L2SkillType type, double negateLvl, int skillId, int maxRemoved)
{
	L2Effect[] effects = target.getAllEffects();
	int count = (maxRemoved <= 0) ? -2 : 0;
	for (L2Effect e : effects)
	{
		if (e.getSkill().getId() == 9014 || e.getSkillType() == L2SkillType.CONT || e.getEffectType() == L2EffectType.DISGUISE)
			continue;
		
		if (negateLvl == -1) // if power is -1 the effect is always removed without power/lvl check ^^
		{
			if (e.getSkill().getSkillType() == type ||
					(e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == type) ||
					(e.getEffectType() == L2EffectType.DEBUFF && type == L2SkillType.SLOW) ||
					(e.getEffectType() != null && e.getEffectType().toString().equalsIgnoreCase(type.toString()))
					)
			{
				if (skillId != 0)
				{
					if (skillId == e.getSkill().getId() && count < maxRemoved)
					{
						e.exit();
						if (count > -1)
							count++;
					}
				}
				else if (count < maxRemoved)
				{
					if (type == L2SkillType.SLOW)
					{
						// Only exit debuffs and weaknesses affecting runSpd
						for (Func f : e.getStatFuncs())
						{
							if (f.stat == Stats.RUN_SPEED)
							{
								e.exit();
								if (count > -1)
									count++;
							}
						}
					}
					else
					{
						e.exit();
						if (count > -1)
							count++;
					}
				}
			}
		}
		else
		{
			boolean cancel = false;
			if (e.getSkill().getEffectType() != null)
			{
				if (e.getSkill().getEffectType() == type && (e.getSkill().getEffectAbnormalLvl() <= negateLvl && e.getSkill().getAbnormalLvl() <= negateLvl))
					cancel = true;
			}
			else if (e.getSkill().getSkillType() == type && e.getSkill().getAbnormalLvl() <= negateLvl)
				cancel = true;
			
			if (cancel)
			{
				if (skillId != 0)
				{
					if (skillId == e.getSkill().getId() && count < maxRemoved)
					{
						e.exit();
						if (count > -1)
							count++;
					}
				}
				else if (count < maxRemoved)
				{
					e.exit();
					if (count > -1)
						count++;
				}
			}
		}
	}
	
	return (maxRemoved <= 0) ? count + 2 : count;
}

private void stealEffects(L2Character stealer, L2Character stolen, FastList<L2Effect> stolenEffects)
{
	if (stolen == null || stolenEffects == null || stolenEffects.isEmpty()) return;
	
	for (L2Effect eff : stolenEffects)
	{
		// if eff time is smaller than 1 sec, will not be stolen, just to save CPU,
		// avoid synchronization(?) problems and NPEs
		if (eff.getPeriod() - eff.getElapsedTime() < 2)
			continue;
		
		Env env = new Env();
		env.player = stolen;
		env.target = stealer;
		env.skill = eff.getSkill();
		L2Effect e = eff.getEffectTemplate().getStolenEffect(env, eff);
		
		if (e != null)
			e.scheduleEffect();
		
		// Since there is a previous check that limits allowed effects to those which come from SkillType.BUFF,
		// it is not needed another check for SkillType
		if (stealer instanceof L2PcInstance && e != null)
		{
			SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
			smsg.addSkillName(eff);
			stealer.sendPacket(smsg);
		}
		// Finishing stolen effect
		eff.exit();
	}
}

private L2Effect[] SortEffects(L2Effect[] initial)
{
	//this is just classic insert sort
	//If u can find better sort for max 20-30 units, rewrite this... :)
	int min, index = 0;
	L2Effect pom;
	for (int i = 0; i < initial.length; i++)
	{
		min = initial[i].getSkill().getMagicLevel();
		for (int j = i; j < initial.length; j++)
		{
			if (initial[j].getSkill().getMagicLevel() <= min)
			{
				min = initial[j].getSkill().getMagicLevel();
				index = j;
			}
		}
		pom = initial[i];
		initial[i] = initial[index];
		initial[index] = pom;
	}
	
	return initial;
}

public L2SkillType[] getSkillIds()
{
	return SKILL_IDS;
}
}