package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.entity.TvTEventTeam;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillDrain;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.util.Rnd;

public class L2CubicInstance
{
	protected static final Logger	_log						= Logger.getLogger(L2CubicInstance.class.getName());
	// Type of Cubics
	public static final int			STORM_CUBIC					= 1;
	public static final int			VAMPIRIC_CUBIC				= 2;
	public static final int			LIFE_CUBIC					= 3;
	public static final int			VIPER_CUBIC					= 4;
	public static final int			POLTERGEIST_CUBIC			= 5;
	public static final int			BINDING_CUBIC				= 6;
	public static final int			AQUA_CUBIC					= 7;
	public static final int			SPARK_CUBIC					= 8;
	public static final int			ATTRACT_CUBIC				= 9;
	public static final int			SMART_CUBIC_EVATEMPLAR		= 10;
	public static final int			SMART_CUBIC_SHILLIENTEMPLAR	= 11;
	public static final int			SMART_CUBIC_ARCANALORD		= 12;
	public static final int			SMART_CUBIC_ELEMENTALMASTER	= 13;
	public static final int			SMART_CUBIC_SPECTRALMASTER	= 14;
	// Max range of cubic skills
	// TODO: Check/fix the max range
	public static final int			MAX_MAGIC_RANGE				= 900;
	// Cubic skills
	public static final int			SKILL_CUBIC_HEAL			= 4051;
	public static final int			SKILL_CUBIC_CURE			= 5579;
	protected L2PcInstance			_owner;
	protected L2Character			_target;
	protected int					_id;
	protected int					_matk;
	protected int					_activationtime;
	protected int					_activationchance;
	protected boolean				_active;
	private final boolean			_givenByOther;
	protected List<L2Skill>			_skills						= new FastList<L2Skill>();
	private Future<?>				_actionTask;
	
	public L2CubicInstance(L2PcInstance owner, int id, int level, int mAtk, int activationtime, int activationchance, int totallifetime, boolean givenByOther)
	{
		_owner = owner;
		_id = id;
		_matk = mAtk;
		_activationtime = activationtime * 1000;
		_activationchance = activationchance;
		_active = false;
		_givenByOther = givenByOther;
		switch (_id)
		{
			case STORM_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4049, level));
				break;
			case VAMPIRIC_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4050, level));
				break;
			case LIFE_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4051, level));
				doAction();
				break;
			case VIPER_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4052, level));
				break;
			case POLTERGEIST_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4053, level));
				_skills.add(SkillTable.getInstance().getInfo(4054, level));
				_skills.add(SkillTable.getInstance().getInfo(4055, level));
				break;
			case BINDING_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4164, level));
				break;
			case AQUA_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4165, level));
				break;
			case SPARK_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4166, level));
				break;
			case ATTRACT_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(5115, level));
				_skills.add(SkillTable.getInstance().getInfo(5116, level));
				break;
			case SMART_CUBIC_ARCANALORD:
				// _skills.add(SkillTable.getInstance().getInfo(4049,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4051, 7)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4165, 9)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the
				// cubic skills list
				break;
			case SMART_CUBIC_ELEMENTALMASTER:
				_skills.add(SkillTable.getInstance().getInfo(4049, 8)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4165,9)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4166, 9)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the
				// cubic skills list
				break;
			case SMART_CUBIC_SPECTRALMASTER:
				_skills.add(SkillTable.getInstance().getInfo(4049, 8)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4052, 6)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4165,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the
				// cubic skills list
				break;
			case SMART_CUBIC_EVATEMPLAR:
				// _skills.add(SkillTable.getInstance().getInfo(4049,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4053, 8)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4165, 9)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the
				// cubic skills list
				break;
			case SMART_CUBIC_SHILLIENTEMPLAR:
				_skills.add(SkillTable.getInstance().getInfo(4049, 8)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4165,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
				_skills.add(SkillTable.getInstance().getInfo(5115, 4)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the
				// cubic skills list
				break;
		}
	}
	
	public synchronized void doAction()
	{
		if (_active)
			return;
		_active = true;
		switch (_id)
		{
			case AQUA_CUBIC:
			case BINDING_CUBIC:
			case SPARK_CUBIC:
			case STORM_CUBIC:
			case POLTERGEIST_CUBIC:
			case VAMPIRIC_CUBIC:
			case VIPER_CUBIC:
			case ATTRACT_CUBIC:
			case SMART_CUBIC_ARCANALORD:
			case SMART_CUBIC_ELEMENTALMASTER:
			case SMART_CUBIC_SPECTRALMASTER:
			case SMART_CUBIC_EVATEMPLAR:
			case SMART_CUBIC_SHILLIENTEMPLAR:
				_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Action(_activationchance), 0, _activationtime);
				break;
			case LIFE_CUBIC:
				_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(), 0, _activationtime);
				break;
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	public final int getMCriticalHit(L2Character target, L2Skill skill)
	{
		// TODO: Temporary now mcrit for cubics is the baseMCritRate of its owner
		return _owner.getTemplate().baseMCritRate;
	}
	
	public int getMAtk()
	{
		return _matk;
	}
	
	public void stopAction()
	{
		_target = null;
		if (_actionTask != null)
		{
			_actionTask.cancel(true);
			_actionTask = null;
		}
		_active = false;
	}
	
	/** this sets the enemy target for a cubic */
	public void getCubicTarget()
	{
		try
		{
			_target = null;
			L2Object ownerTarget = _owner.getTarget();
			if (ownerTarget == null)
				return;
			// TvT event targeting
			if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(_owner.getObjectId()))
			{
				TvTEventTeam enemyTeam = TvTEvent.getParticipantEnemyTeam(_owner.getObjectId());
				if (ownerTarget.getActingPlayer() != null)
				{
					L2PcInstance target = ownerTarget.getActingPlayer();
					if (enemyTeam.containsPlayer(target.getObjectId()) && !(target.isDead()))
					{
						_target = (L2Character) ownerTarget;
					}
				}
				return;
			}
			if (NewHuntingGrounds._started && _owner._inEventHG)
			{
				if (ownerTarget.getActingPlayer() != null)
				{
					L2PcInstance target = ownerTarget.getActingPlayer();
					if (!(target._teamNameHG.equals(_owner._teamNameHG)) && !target.isDead())
					{
						_target = (L2Character) ownerTarget;
					}
				}
				return;
			}
			// TvT event targeting
			if (TvT._started && _owner._inEventTvT)
			{
				if (ownerTarget.getActingPlayer() != null)
				{
					L2PcInstance target = ownerTarget.getActingPlayer();
					if (!(target._teamNameTvT.equals(_owner._teamNameTvT)) && !target.isDead())
					{
						_target = (L2Character) ownerTarget;
					}
				}
				return;
			}
			
			if (NewTvT._started && _owner._inEventTvT)
			{
				if (ownerTarget.getActingPlayer() != null)
				{
					L2PcInstance target = ownerTarget.getActingPlayer();
					if (!(target._teamNameTvT.equals(_owner._teamNameTvT)) && !target.isDead())
					{
						_target = (L2Character) ownerTarget;
					}
				}
				return;
			}
			// TvT event targeting
			if (NewDomination._started && _owner._inEventLunaDomi)
			{
				if (ownerTarget.getActingPlayer() != null)
				{
					L2PcInstance target = ownerTarget.getActingPlayer();
					if (!(target._teamNameLunaDomi.equals(_owner._teamNameLunaDomi)) && !target.isDead())
					{
						_target = (L2Character) ownerTarget;
					}
				}
				return;
			}

			// Duel targeting
			if (_owner.isInDuel())
			{
				L2PcInstance PlayerA = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerA();
				L2PcInstance PlayerB = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerB();
				if (DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel())
				{
					L2Party partyA = PlayerA.getParty();
					L2Party partyB = PlayerB.getParty();
					L2Party partyEnemy = null;
					if (partyA != null)
					{
						if (partyA.getPartyMembers().contains(_owner))
							if (partyB != null)
								partyEnemy = partyB;
							else
								_target = PlayerB;
						else
							partyEnemy = partyA;
					}
					else
					{
						if (PlayerA == _owner)
							if (partyB != null)
								partyEnemy = partyB;
							else
								_target = PlayerB;
						else
							_target = PlayerA;
					}
					if (_target == PlayerA || _target == PlayerB)
						if (_target == ownerTarget)
							return;
					if (partyEnemy != null)
					{
						if (partyEnemy.getPartyMembers().contains(ownerTarget))
							_target = (L2Character) ownerTarget;
						return;
					}
				}
				if (PlayerA != _owner && ownerTarget == PlayerA)
				{
					_target = PlayerA;
					return;
				}
				if (PlayerB != _owner && ownerTarget == PlayerB)
				{
					_target = PlayerB;
					return;
				}
				_target = null;
				return;
			}
			// Olympiad targeting
			if (_owner.isInOlympiadMode())
			{
				if (_owner.isOlympiadStart())
				{
					L2PcInstance[] players = Olympiad.getInstance().getPlayers(_owner.getOlympiadGameId());
					if (players != null)
					{
						if (_owner.getOlympiadSide() == 1)
						{
							if (ownerTarget == players[1])
								_target = players[1];
							else if (players[1].getPet() != null && ownerTarget == players[1].getPet())
								_target = players[1].getPet();
						}
						else
						{
							if (ownerTarget == players[0])
								_target = players[0];
							else if (players[0].getPet() != null && ownerTarget == players[0].getPet())
								_target = players[0].getPet();
						}
					}
				}
				return;
			}
			// test owners target if it is valid then use it
			if (ownerTarget instanceof L2Character && ownerTarget != _owner.getPet() && ownerTarget != _owner)
			{
				// target mob which has aggro on you or your summon
				if (ownerTarget instanceof L2Attackable)
				{
					if (((L2Attackable) ownerTarget).getAggroList().get(_owner) != null && !((L2Attackable) ownerTarget).isDead())
					{
						_target = (L2Character) ownerTarget;
						return;
					}
					if (_owner.getPet() != null)
						if (((L2Attackable) ownerTarget).getAggroList().get(_owner.getPet()) != null && !((L2Attackable) ownerTarget).isDead())
						{
							_target = (L2Character) ownerTarget;
							return;
						}
				}
				// get target in pvp or in siege
				L2PcInstance enemy = null;
				if ((_owner.getPvpFlag() > 0 && !_owner.isInsideZone(L2Character.ZONE_PEACE)) || _owner.isInsideZone(L2Character.ZONE_PVP))
				{
					if (ownerTarget instanceof L2Character && !((L2Character) ownerTarget).isDead())
						enemy = ownerTarget.getActingPlayer();
					if (enemy != null)
					{
						boolean targetIt = true;
						if (_owner.getParty() != null)
						{
							if (_owner.getParty().getPartyMembers().contains(enemy))
								targetIt = false;
							else if (_owner.getParty().getCommandChannel() != null)
							{
								if (_owner.getParty().getCommandChannel().getMembers().contains(enemy))
									targetIt = false;
							}
						}
						if (_owner.getClan() != null && !_owner.isInsideZone(L2Character.ZONE_PVP))
						{
							if (_owner.getClan().isMember(enemy.getObjectId()))
								targetIt = false;
							if (_owner.getAllyId() > 0 && enemy.getAllyId() > 0)
							{
								if (_owner.getAllyId() == enemy.getAllyId())
									targetIt = false;
							}
						}
						if (enemy.getPvpFlag() == 0 && !enemy.isInsideZone(L2Character.ZONE_PVP))
							targetIt = false;
						if (enemy.isInsideZone(L2Character.ZONE_PEACE))
							targetIt = false;
						if (_owner.getSiegeState() > 0 && _owner.getSiegeState() == enemy.getSiegeState())
							targetIt = false;
						if (!enemy.isVisible())
							targetIt = false;
						if (targetIt)
						{
							_target = enemy;
							return;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
	}
	
	private class Action implements Runnable
	{
		private final int _chance;
		
		Action(int chance)
		{
			_chance = chance;
			// run task
		}
		
		public void run()
		{
			try
			{
				if (_owner.isOnline() == 0)
				{
					stopAction();
					_owner.delCubic(_id);
					_owner.broadcastUserInfo();
					return;
				}
				if (_owner.isAlikeDead())
				{
					stopAction();
					return;
				}
				if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(_owner))
				{
					if (_owner.getPet() != null)
					{
						if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(_owner.getPet()))
						{
							stopAction();
							return;
						}
					}
					else
					{
						stopAction();
						return;
					}
				}
				// Smart Cubic debuff cancel is 100%
				boolean UseCubicCure = false;
				L2Skill skill = null;
				if (_id >= SMART_CUBIC_EVATEMPLAR && _id <= SMART_CUBIC_SPECTRALMASTER)
				{
					L2Effect[] effects = _owner.getAllEffects();
					for (L2Effect e : effects)
					{
						if (e.getSkill().isDebuff())
						{
							UseCubicCure = true;
							e.exit();
						}
					}
				}
				if (UseCubicCure)
				{
					// Smart Cubic debuff cancel is needed, no other skill is used in this
					// activation period
					MagicSkillUse msu = new MagicSkillUse(_owner, _owner, SKILL_CUBIC_CURE, 1, 0, 0);
					_owner.broadcastPacket(msu);
				}
				else if (Rnd.get(1, 100) < _chance)
				{
					skill = _skills.get(Rnd.get(_skills.size()));
					if (skill != null)
					{
						if (skill.getId() == SKILL_CUBIC_HEAL)
						{
							// friendly skill, so we look a target in owner's party
							CubicTargetForHeal();
						}
						else
						{
							// offensive skill, we look for an enemy target
							getCubicTarget();
							if (!isInCubicRange(_owner, _target))
								_target = null;
						}
						if ((_target != null) && (!_target.isDead()))
						{
							if (Config.DEBUG)
							{
								_log.info("L2CubicInstance: Action.run();");
								_log.info("Cubic Id: " + _id + " Target: " + _target.getName() + " distance: " + Math.sqrt(_target.getDistanceSq(_owner.getX(), _owner.getY(), _owner.getZ())));
							}
							_owner.broadcastPacket(new MagicSkillUse(_owner, _target, skill.getId(), skill.getLevel(), 0, 0));
							L2SkillType type = skill.getSkillType();
							ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
							L2Character[] targets =
							{
								_target
							};
							if ((type == L2SkillType.PARALYZE) || (type == L2SkillType.STUN) || (type == L2SkillType.ROOT) || (type == L2SkillType.AGGDAMAGE))
							{
								if (Config.DEBUG)
									_log.info("L2CubicInstance: Action.run() handler " + type);
								useCubicDisabler(type, L2CubicInstance.this, skill, targets);
							}
							else if (type == L2SkillType.MDAM)
							{
								if (Config.DEBUG)
									_log.info("L2CubicInstance: Action.run() handler " + type);
								useCubicMdam(L2CubicInstance.this, skill, targets);
							}
							else if ((type == L2SkillType.POISON) || (type == L2SkillType.DEBUFF) || (type == L2SkillType.DOT))
							{
								if (Config.DEBUG)
									_log.info("L2CubicInstance: Action.run() handler " + type);
								useCubicContinuous(L2CubicInstance.this, skill, targets);
							}
							else if (type == L2SkillType.DRAIN)
							{
								if (Config.DEBUG)
									_log.info("L2CubicInstance: Action.run() skill " + type);
								((L2SkillDrain) skill).useCubicSkill(L2CubicInstance.this, targets);
							}
							else
							{
								handler.useSkill(_owner, skill, targets);
								if (Config.DEBUG)
									_log.info("L2CubicInstance: Action.run(); other handler");
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	public void useCubicContinuous(L2CubicInstance activeCubic, L2Skill skill, L2Object[] targets)
	{
		for (L2Character target : (L2Character[]) targets)
		{
			if (target == null || target.isDead())
				continue;
			if (skill.isOffensive())
			{
				byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, skill);
				boolean acted = Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld);
				if (!acted)
				{
					activeCubic.getOwner().sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
					continue;
				}
			}
			// if this is a debuff let the duel manager know about it
			// so the debuff can be removed after the duel
			// (player & target must be in the same duel)
			if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == L2SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId())
			{
				DuelManager dm = DuelManager.getInstance();
				for (L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target))
					if (debuff != null)
						dm.onBuff(((L2PcInstance) target), debuff);
			}
			else
				skill.getEffects(activeCubic, target, null);
		}
	}
	
	public void useCubicMdam(L2CubicInstance activeCubic, L2Skill skill, L2Object[] targets)
	{
		for (L2Character target : (L2Character[]) targets)
		{
			if (target == null)
				continue;
			if (target.isAlikeDead())
			{
				if (target instanceof L2PcInstance)
					target.stopFakeDeath(null);
				else
					continue;
			}
			boolean mcrit = Formulas.calcMCrit(activeCubic.getMCriticalHit(target, skill), target);
			byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, skill);
			int damage = (int) Formulas.calcMagicDam(activeCubic, target, skill, mcrit, shld);
			/*
			 * If target is reflecting the skill then no damage is done
			 * Ignoring vengance-like reflections
			 */
			if ((Formulas.calcSkillReflect(null, target, skill) & Formulas.SKILL_REFLECT_SUCCEED) > 0)
				damage = 0;
			if (Config.DEBUG)
				_log.info("L2SkillMdam: useCubicSkill() -> damage = " + damage);
			if (damage > 0)
			{
				/*
				 * // Manage attack or cast break of the target (calculating rate, sending message...)
				 * if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				 * {
				 * target.breakAttack();
				 * target.breakCast();
				 * }
				 */
				activeCubic.getOwner().sendDamageMessage(target, damage, mcrit, false, false);
				if (skill.hasEffects())
				{
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld))
						skill.getEffects(activeCubic, target, null);
				}
				target.reduceCurrentHp(damage, activeCubic.getOwner(), skill);
			}
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	public void useCubicDisabler(L2SkillType type, L2CubicInstance activeCubic, L2Skill skill, L2Object[] targets)
	{
		if (Config.DEBUG)
			_log.info("Disablers: useCubicSkill()");
		for (L2Character target : (L2Character[]) targets)
		{
			if (target == null || target.isDead()) // bypass if target is null or dead
				continue;
			byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, skill);
			switch (type)
			{
				case STUN:
				{
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld))
					{
						// if this is a debuff let the duel manager know about it
						// so the debuff can be removed after the duel
						// (player & target must be in the same duel)
						if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == L2SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId())
						{
							DuelManager dm = DuelManager.getInstance();
							for (L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target))
								if (debuff != null)
									dm.onBuff(((L2PcInstance) target), debuff);
						}
						else
							skill.getEffects(activeCubic, target, null);
						if (Config.DEBUG)
							_log.info("Disablers: useCubicSkill() -> success");
					}
					else
					{
						if (Config.DEBUG)
							_log.info("Disablers: useCubicSkill() -> failed");
					}
					break;
				}
				case PARALYZE: // use same as root for now
				{
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld))
					{
						// if this is a debuff let the duel manager know about it
						// so the debuff can be removed after the duel
						// (player & target must be in the same duel)
						if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == L2SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId())
						{
							DuelManager dm = DuelManager.getInstance();
							for (L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target))
								if (debuff != null)
									dm.onBuff(((L2PcInstance) target), debuff);
						}
						else
							skill.getEffects(activeCubic, target, null);
						if (Config.DEBUG)
							_log.info("Disablers: useCubicSkill() -> success");
					}
					else
					{
						if (Config.DEBUG)
							_log.info("Disablers: useCubicSkill() -> failed");
					}
					break;
				}
				case CANCEL_DEBUFF:
				{
					L2Effect[] effects = target.getAllEffects();
					if (effects == null || effects.length == 0)
						break;
					int count = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
					for (L2Effect e : effects)
					{
						if (e.getSkill().isDebuff() && count < skill.getMaxNegatedEffects())
						{
							// Do not remove raid curse skills
							if (e.getSkill().getId() != 4215 && e.getSkill().getId() != 4515 && e.getSkill().getId() != 4082)
							{
								e.exit();
								if (count > -1)
									count++;
							}
						}
					}
					break;
				}
				case ROOT:
				{
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill, shld))
					{
						// if this is a debuff let the duel manager know about it
						// so the debuff can be removed after the duel
						// (player & target must be in the same duel)
						if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == L2SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId())
						{
							DuelManager dm = DuelManager.getInstance();
							for (L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target))
								if (debuff != null)
									dm.onBuff(((L2PcInstance) target), debuff);
						}
						else
							skill.getEffects(activeCubic, target, null);
						if (Config.DEBUG)
							_log.info("Disablers: useCubicSkill() -> success");
					}
					else
					{
						if (Config.DEBUG)
							_log.info("Disablers: useCubicSkill() -> failed");
					}
					break;
				}
				case AGGDAMAGE:
				{
					if (target instanceof L2Attackable)
					{
						try
						{
							if (!_owner.isGM() && target.getAI().getAttackTarget() != null)
							{
								L2Character mobAttackTarget = target.getAI().getAttackTarget();
								if (mobAttackTarget.getActingPlayer() != _owner.getActingPlayer()/* || (activeChar.getPet() != null && targetb != activeChar.getPet()) */)
								{
									if ((_owner.getActingPlayer().getClanId() == 0 && mobAttackTarget.getActingPlayer().getClanId() == 0) || _owner.getActingPlayer().getClanId() != mobAttackTarget.getActingPlayer().getClanId())
									{
										if (_owner.getParty() == null)
											return;
										if (mobAttackTarget.getParty() == null)
											return;
										if (_owner.getParty().getPartyLeaderOID() != mobAttackTarget.getParty().getPartyLeaderOID())
											return;
									}
								}
							}
						}
						catch (Exception e)
						{
							break;
						}
						int numba = _owner.getActingPlayer().isTankClass() ? 152 : 75;
						if (target.isRaid())
							numba = 152;
						else if (skill.getId() == 18)
							numba = 50;
						final int aggression = (int) ((numba * skill.getPower(_owner) * _owner.calcStat(Stats.AGGRESSION_PROF, 1, null, skill)) / target.getLevel());
						if (aggression > 0)
							target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, _owner, aggression);
						skill.getEffects(activeCubic, target, new Env(shld, true, true, true));
					}
					else
						skill.getEffects(activeCubic, target, new Env(shld, true, true, true));
					break;
				}
			}
		}
	}
	
	/** returns true if the target is inside of the owner's max Cubic range */
	public boolean isInCubicRange(L2Character owner, L2Character target)
	{
		if (owner == null || target == null)
			return false;

		return target.isInsideRadius(owner, MAX_MAGIC_RANGE, true, true);
//		int x, y, z;
//		// temporary range check until real behavior of cubics is known/coded
//		int range = MAX_MAGIC_RANGE;
//		x = (owner.getX() - target.getX());
//		y = (owner.getY() - target.getY());
//		z = (owner.getZ() - target.getZ());
//		return ((x * x) + (y * y) + (z * z) <= (range * range));
	}
	
	/** this sets the friendly target for a cubic */
	public void CubicTargetForHeal()
	{
		L2Character target = null;
		double percentleft = 100.0;
		L2Party party = _owner.getParty();
		// if owner is in a duel but not in a party duel, then it is the same as he does not have a
		// party
		if (_owner.isInDuel())
			if (!DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel())
				party = null;
		if (party != null && !_owner.isInOlympiadMode())
		{
			// Get all visible objects in a spheric area near the L2Character
			// Get a list of Party Members
			List<L2PcInstance> partyList = party.getPartyMembers();
			for (L2Character partyMember : partyList)
			{
				if (!partyMember.isDead())
				{
					// if party member not dead, check if he is in castrange of heal cubic
					if (isInCubicRange(_owner, partyMember))
					{
						// member is in cubic casting range, check if he need heal and if he have
						// the lowest HP
						if (partyMember.getCurrentHp() < partyMember.getMaxHp())
						{
							if (percentleft > (partyMember.getCurrentHp() / partyMember.getMaxHp()))
							{
								percentleft = (partyMember.getCurrentHp() / partyMember.getMaxHp());
								target = partyMember;
							}
						}
					}
				}
				if (partyMember.getPet() != null)
				{
					if (partyMember.getPet().isDead())
						continue;
					// if party member's pet not dead, check if it is in castrange of heal cubic
					if (!isInCubicRange(_owner, partyMember.getPet()))
						continue;
					// member's pet is in cubic casting range, check if he need heal and if he have
					// the lowest HP
					if (partyMember.getPet().getCurrentHp() < partyMember.getPet().getMaxHp())
					{
						if (percentleft > (partyMember.getPet().getCurrentHp() / partyMember.getPet().getMaxHp()))
						{
							percentleft = (partyMember.getPet().getCurrentHp() / partyMember.getPet().getMaxHp());
							target = partyMember.getPet();
						}
					}
				}
			}
		}
		else
		{
			if (_owner.getCurrentHp() < _owner.getMaxHp())
			{
				percentleft = (_owner.getCurrentHp() / _owner.getMaxHp());
				target = _owner;
			}
			if (_owner.getPet() != null)
				if (!_owner.getPet().isDead() && _owner.getPet().getCurrentHp() < _owner.getPet().getMaxHp() && percentleft > (_owner.getPet().getCurrentHp() / _owner.getPet().getMaxHp()) && isInCubicRange(_owner, _owner.getPet()))
				{
					target = _owner.getPet();
				}
		}
		_target = target;
	}
	
	public boolean givenByOther()
	{
		return _givenByOther;
	}
	
	private class Heal implements Runnable
	{
		Heal()
		{
			// run task
		}
		
		public void run()
		{
			if (_owner.isOnline() == 0)
			{
				stopAction();
				_owner.delCubic(_id);
				_owner.broadcastUserInfo();
				return;
			}
			if (_owner.isAlikeDead())
			{
				return;
			}
			try
			{
				L2Skill skill = null;
				for (L2Skill sk : _skills)
				{
					if (sk != null && sk.getId() == SKILL_CUBIC_HEAL)
					{
						skill = sk;
						break;
					}
				}
				if (skill != null)
				{
					CubicTargetForHeal();
					L2Character target = _target;
					if (target != null && !target.isDead())
					{
						if (target.getMaxHp() - target.getCurrentHp() > skill.getPower())
						{
							L2Character[] targets =
							{
								target
							};
							ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
							if (handler != null)
							{
								handler.useSkill(_owner, skill, targets);
							}
							else
							{
								skill.useSkill(_owner, targets);
							}
							MagicSkillUse msu = new MagicSkillUse(_owner, target, skill.getDisplayId(), skill.getDisplayLvl(), 0, 0);
							_owner.broadcastPacket(msu);
						}
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
}