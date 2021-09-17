package net.sf.l2j.gameserver.ai;

import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_INTERACT;

import java.util.concurrent.Future;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.Territory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2AirShipInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2BoatInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2EventMapGuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FestivalMonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FriendlyMobInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MinionInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RiftInvaderInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.Quest.QuestEventType;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class L2AttackableAI extends L2CharacterAI implements Runnable
{
//protected static final Logger _log = Logger.getLogger(L2AttackableAI.class.getName());

private static final int RANDOM_WALK_RATE = 60; // unconfirmed BITCH
private static final int MAX_ATTACK_TIMEOUT = 300; // int ticks, i.e. 30 seconds

/** The L2Attackable AI task executed every 1s (call onEvtThink method)*/
private Future<?> _aiTask;

/** The delay after which the attacked is stopped */
private int _attackTimeout;
private boolean _kited = false;

/** The L2Attackable aggro counter */
public int _globalAggro;

/** The flag used to indicate that a thinking action is in progress */
private boolean _thinking; // to prevent recursive thinking

/** For attack AI, analysis of mob and its targets */
private final SelfAnalysis _selfAnalysis = new SelfAnalysis();
private final TargetAnalysis _mostHatedAnalysis = new TargetAnalysis();
private final TargetAnalysis _secondMostHatedAnalysis = new TargetAnalysis();

/**
 * Constructor of L2AttackableAI.<BR><BR>
 *
 * @param accessor The AI accessor of the L2Character
 *
 */
public L2AttackableAI(L2Character.AIAccessor accessor)
{
	super(accessor);
	_selfAnalysis.init();
	_attackTimeout = Integer.MAX_VALUE;
	_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
}

public void run()
{
	// Launch actions corresponding to the Event Think
	onEvtThink();
}

/**
 * Return True if the target is autoattackable (depends on the actor type).<BR><BR>
 *
 * <B><U> Actor is a L2GuardInstance</U> :</B><BR><BR>
 * <li>The target isn't a Folk or a Door</li>
 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
 * <li>The target is in the actor Aggro range and is at the same height</li>
 * <li>The L2PcInstance target has karma (=PK)</li>
 * <li>The L2MonsterInstance target is aggressive</li><BR><BR>
 *
 * <B><U> Actor is a L2SiegeGuardInstance</U> :</B><BR><BR>
 * <li>The target isn't a Folk or a Door</li>
 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
 * <li>The target is in the actor Aggro range and is at the same height</li>
 * <li>A siege is in progress</li>
 * <li>The L2PcInstance target isn't a Defender</li><BR><BR>
 *
 * <B><U> Actor is a L2FriendlyMobInstance</U> :</B><BR><BR>
 * <li>The target isn't a Folk, a Door or another L2NpcInstance</li>
 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
 * <li>The target is in the actor Aggro range and is at the same height</li>
 * <li>The L2PcInstance target has karma (=PK)</li><BR><BR>
 *
 * <B><U> Actor is a L2MonsterInstance</U> :</B><BR><BR>
 * <li>The target isn't a Folk, a Door or another L2NpcInstance</li>
 * <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
 * <li>The target is in the actor Aggro range and is at the same height</li>
 * <li>The actor is Aggressive</li><BR><BR>
 *
 * @param target The targeted L2Object
 *
 */
private boolean autoAttackCondition(L2Character target)
{
	if (target == null || !(_actor instanceof L2Attackable))
		return false;
	L2Attackable me = (L2Attackable) _actor;
	
	// Check if the target isn't invulnerable
	if (target.isInvul())
	{
		// However EffectInvincible requires to check GMs specially
		if (target instanceof L2Playable)
		{
			if(target.getActingPlayer().isSpawnProtected())
				return false;
			else if (target.getActingPlayer().isGM())
			{
				if (me.getHating(target) < 4000)
					return false;
			}
		}
	}
	
	// Check if the target isn't a Folk or a Door
	if (target instanceof L2NpcInstance || target instanceof L2DoorInstance ||
			target instanceof L2StaticObjectInstance || target instanceof L2BoatInstance || target instanceof L2AirShipInstance)
		return false;
	
	// Check if the target isn't dead, is in the Aggro range and is at the same height
	if (target.isAlikeDead() || !me.isInsideRadius(target, me.getAggroRange(), false, false) || Math.abs(_actor.getZ() - target.getZ()) > 350)
		return false;
	
	if (_selfAnalysis.cannotMoveOnLand && !target.isInsideZone(L2Character.ZONE_WATER))
		return false;
	
	// Check if the target is a L2PlayableInstance
	if (target instanceof L2Playable)
	{
		// Check if the AI isn't a Raid Boss and the target isn't in silent move mode
		if (me.getLevel() < 91 && !me.isRaid() && !me.isAPC() && ((L2Playable)target).isSilentMoving())
			return false;
	}
	
	// Check if the target is a L2PcInstance
	if (target instanceof L2PcInstance)
	{
		// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO
		if (((L2PcInstance) target).isGM() && !((L2PcInstance) target).getAccessLevel().canTakeAggro())
			return false;
		
		// TODO: Ideally, autoattack condition should be called from the AI script.  In that case,
		// it should only implement the basic behaviors while the script will add more specific
		// behaviors (like varka/ketra alliance, etc).  Once implemented, remove specialized stuff
		// from this location.  (Fulminus)
		
		// Check if player is an ally (comparing mem addr)
		if ("varka".equals(me.getFactionId()) && ((L2PcInstance) target).isAlliedWithVarka())
			return false;
		if ("ketra".equals(me.getFactionId()) && ((L2PcInstance) target).isAlliedWithKetra())
			return false;
		// check if the target is within the grace period for JUST getting up from fake death
		if (((L2PcInstance) target).isRecentFakeDeath())
			return false;
		
		if (target.isInParty() && target.getParty().isInDimensionalRift())
		{
			byte riftType = target.getParty().getDimensionalRift().getType();
			byte riftRoom = target.getParty().getDimensionalRift().getCurrentRoom();
			
			if (me instanceof L2RiftInvaderInstance && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(me.getX(), me.getY(), me.getZ()))
				return false;
		}
	}
	// Check if the target is a L2Summon
	if (target instanceof L2Summon)
	{
		L2PcInstance owner = ((L2Summon) target).getOwner();
		if (owner != null)
		{
			// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO
			if (owner.isGM() && (owner.isInvul() || !owner.getAccessLevel().canTakeAggro()))
				return false;
			// Check if player is an ally (comparing mem addr)
			if ("varka".equals(me.getFactionId()) && owner.isAlliedWithVarka())
				return false;
			if ("ketra".equals(me.getFactionId()) && owner.isAlliedWithKetra())
				return false;
		}
	}
	// Check if the actor is a L2GuardInstance
	if (_actor instanceof L2GuardInstance)
	{
		// Check if the L2PcInstance target has karma (=PK)
		if (target instanceof L2PcInstance && ((L2PcInstance) target).getKarma() > 0 || _actor instanceof L2EventMapGuardInstance)
			// Los Check
			return GeoData.getInstance().canSeeTarget(me, target);
		
		//if (target instanceof L2Summon)
		//    return ((L2Summon)target).getKarma() > 0;
		
		// Check if the L2MonsterInstance target is aggressive
		if (target instanceof L2MonsterInstance)
			return (((L2MonsterInstance) target).isAggressive() && GeoData.getInstance().canSeeTarget(me, target));
		
		return false;
	}
	else if (_actor instanceof L2FriendlyMobInstance)
	{ // the actor is a L2FriendlyMobInstance
		
		// Check if the target isn't another L2NpcInstance
		if (target instanceof L2Npc)
			return false;
		
		// Check if the L2PcInstance target has karma (=PK)
		if (target instanceof L2PcInstance && ((L2PcInstance) target).getKarma() > 0)
			// Los Check
			return GeoData.getInstance().canSeeTarget(me, target);
		else
			return false;
	}
	else
	{ //The actor is a L2MonsterInstance
		
		// Check if the target isn't another L2NpcInstance
		if (target instanceof L2Npc)
			return false;
		
		// depending on config, do not allow mobs to attack _new_ players in peacezones,
		// unless they are already following those players from outside the peacezone.
		if (!Config.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(L2Character.ZONE_PEACE))
			return false;
		
		// Check if the actor is Aggressive
		return (me.isAggressive() && GeoData.getInstance().canSeeTarget(me, target));
	}
}

public void startAITask()
{
	// If not idle - create an AI task (schedule onEvtThink repeatedly)
	if (_aiTask == null)
	{
		_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
	}
}

public void stopAITask()
{
	if (_aiTask != null)
	{
		_aiTask.cancel(false);
		_aiTask = null;
	}
}

@Override
protected void onEvtDead()
{
	stopAITask();
	super.onEvtDead();
}

/**
 * Set the Intention of this L2CharacterAI and create an  AI Task executed every 1s (call onEvtThink method) for this L2Attackable.<BR><BR>
 *
 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If actor _knowPlayer isn't EMPTY, AI_INTENTION_IDLE will be change in AI_INTENTION_ACTIVE</B></FONT><BR><BR>
 *
 * @param intention The new Intention to set to the AI
 * @param arg0 The first parameter of the Intention
 * @param arg1 The second parameter of the Intention
 *
 */
@Override
synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
{
	if (intention == AI_INTENTION_IDLE || intention == AI_INTENTION_ACTIVE)
	{
		// Check if actor is not dead
		if (!_actor.isAlikeDead())
		{
			L2Attackable npc = (L2Attackable) _actor;
			
			// If its _knownPlayer isn't empty set the Intention to AI_INTENTION_ACTIVE
			if (!npc.getKnownList().getKnownPlayers().isEmpty())
				intention = AI_INTENTION_ACTIVE;
			else
			{
				if (npc.getSpawn() != null)
				{
					final int range = Math.max(Config.MAX_DRIFT_RANGE, npc.getRndWalkRange());
					if (!npc.isInsideRadius(npc.getSpawn().getCurX(), npc.getSpawn().getCurY(), npc.getSpawn().getCurZ(), range + range, true, false))
						intention = AI_INTENTION_ACTIVE;
				}
			}
		}
		
		if (intention == AI_INTENTION_IDLE)
		{
			// Set the Intention of this L2AttackableAI to AI_INTENTION_IDLE
			super.changeIntention(AI_INTENTION_IDLE, null, null);
			
			// Stop AI task and detach AI from NPC
			if (_aiTask != null)
			{
				_aiTask.cancel(true);
				_aiTask = null;
			}
			
			// Cancel the AI
			_accessor.detachAI();
			
			return;
		}
	}
	
	// Set the Intention of this L2AttackableAI to intention
	super.changeIntention(intention, arg0, arg1);
	
	// If not idle - create an AI task (schedule onEvtThink repeatedly)
	startAITask();
}

/**
 * Manage the Attack Intention : Stop current Attack (if necessary), Calculate attack timeout, Start a new Attack and Launch Think Event.<BR><BR>
 *
 * @param target The L2Character to attack
 *
 */
@Override
protected void onIntentionAttack(L2Character target)
{
	// Calculate the attack timeout
	_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
	// Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Event
	super.onIntentionAttack(target);
}

/**
 * Manage AI standard thinks of a L2Attackable (called by onEvtThink).<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Update every 1s the _globalAggro counter to come close to 0</li>
 * <li>If the actor is Aggressive and can attack, add all autoAttackable L2Character in its Aggro Range to its _aggroList, chose a target and order to attack it</li>
 * <li>If the actor is a L2GuardInstance that can't attack, order to it to return to its home location</li>
 * <li>If the actor is a L2MonsterInstance that can't attack, order to it to random walk (1/100)</li><BR><BR>
 *
 */
private void thinkActive()
{
	L2Attackable npc = (L2Attackable) _actor;
	
	// Update every 1s the _globalAggro counter to come close to 0
	if (_globalAggro != 0)
	{
		if (_globalAggro < 0)
			_globalAggro++;
		else
			_globalAggro--;
	}
	
	// Add all autoAttackable L2Character in L2Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
	// A L2Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
	if (_globalAggro >= 0)
	{
		for (L2Character target : npc.getKnownList().getKnownCharactersInRadius(((L2Npc)_actor).getAggroRange()))
		{
			/*
			 * Check to see if this is a festival mob spawn.
			 * If it is, then check to see if the aggro trigger
			 * is a festival participant...if so, move to attack it.
			 */
			if ((_actor instanceof L2FestivalMonsterInstance) && target instanceof L2PcInstance)
			{
				L2PcInstance targetPlayer = (L2PcInstance) target;
				
				if (!(targetPlayer.isFestivalParticipant()))
					continue;
			}
			/*
			 * Temporarily adding this commented code as a concept to be used eventually.
			 * However, the way it is written below will NOT work correctly.  The NPC
			 * should only notify Aggro Range Enter when someone enters the range from outside.
			 * Instead, the below code will keep notifying even while someone remains within
			 * the range.  Perhaps we need a short knownlist of range = aggroRange for just
			 * people who are actively within the npc's aggro range?...(Fulminus)
				// notify AI that a playable instance came within aggro range
				if ((obj instanceof L2PcInstance) || (obj instanceof L2Summon))
				{
					if ( !((L2Character)obj).isAlikeDead()
				        && !npc.isInsideRadius(obj, npc.getAggroRange(), true, false) )
					{
						L2PcInstance targetPlayer = (obj instanceof L2PcInstance)? (L2PcInstance) obj: ((L2Summon) obj).getOwner();
				    	if (npc.getTemplate().getEventQuests(QuestEventType.ON_AGGRO_RANGE_ENTER) !=null)
				    		for (Quest quest: npc.getTemplate().getEventQuests(QuestEventType.ON_AGGRO_RANGE_ENTER))
				    			quest.notifyAggroRangeEnter(npc, targetPlayer, (obj instanceof L2Summon));
					}
				}
			 */
			// TODO: The AI Script ought to handle aggro behaviors in onSee.  Once implemented, aggro behaviors ought			// to be removed from here.  (Fulminus)
			// For each L2Character check if the target is autoattackable
			if (autoAttackCondition(target)) // check aggression
			{
				// Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
				int hating = npc.getHating(target);
				
				// Add the attacker to the L2Attackable _aggroList with 0 damage and 1 hate
				if (hating == 0)
					npc.addDamageHate(target, 1);
				
				if (_actor instanceof L2MonsterInstance)
				{
					L2MonsterInstance master = (L2MonsterInstance)_actor;
					
					if (_actor instanceof L2MinionInstance)
					{
						master = ((L2MinionInstance)_actor).getLeader();
						
						if (master != null && !master.isDead() && (!master.isInCombat() || _actor.isAPC()))
						{
							master.addDamageHate(target, 1);
							master.callMinionsToAssist(target, 1, (L2Attackable) _actor);
						}
					}
					else if (master.hasMinions())
						master.callMinionsToAssist(target, 1, null);
				}
				
				if (_actor instanceof L2Attackable)
					notifyFactionAttacked(target);
			}
		}
		
		// Chose a target from its aggroList
		L2Character hated = null;
		
		if (_actor.isConfused())
			hated = getAttackTarget(); // effect handles selection
		else
		{
			for (;;)
			{
				hated = npc.getMostHated();
				
				if (hated != null)
				{
					if (hated.isGM())
					{
						((L2Attackable)_actor).stopHating(hated);
						continue;
					}
					
					if (!_actor.getKnownList().knowsObject(hated))
					{
						if (!(_actor.getInstanceId() == hated.getInstanceId() && _actor.isInsideRadius(hated, 4000, false, false)))
							((L2Attackable)_actor).stopHating(hated);
						else
						{
							_actor.getKnownList().addKnownObject(hated, true);
							break;
						}
					}
					else break;
				}
				else break;
			}
		}
		
		// Order to the L2Attackable to attack the target
		if (hated != null)
		{
			// Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
			int aggro = npc.getHating(hated);
			
			if (aggro + _globalAggro > 0)
			{
				// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
				if (!_actor.isRunning())
					_actor.setRunning();
				
				// Set the AI Intention to AI_INTENTION_ATTACK
				setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated);
			}
			
			return;
		}
	}
	
	// Check if the actor is a L2GuardInstance
	if (_actor instanceof L2GuardInstance)
	{
		// Order to the L2GuardInstance to return to its home location because there's no target to attack
		((L2GuardInstance) _actor).returnHome();
	}
	
	// If this is a festival monster, then it remains in the same location.
	if (_actor instanceof L2FestivalMonsterInstance)
		return;
	
	// Check if the mob should not return to spawn point
	if (!npc.canReturnToSpawnPoint())
		return;
	
	// Minions following leader
	if (_actor instanceof L2MinionInstance && ((L2MinionInstance) _actor).getLeader() != null)
	{
		int offset;
		
		if (_actor.isRaidMinion())
			offset = 500; // for Raids - need correction
		else if (_selfAnalysis.isHealer)
			offset = 600; // for normal minions - need correction :)
		else
			offset = 200; // for normal minions - need correction :)
		
		if (((L2MinionInstance) _actor).getLeader().isRunning())
			_actor.setRunning();
		else
			_actor.setWalking();
		
		if (_actor.getPlanDistanceSq(((L2MinionInstance) _actor).getLeader()) > offset * offset)
		{
			int x1, y1, z1;
			x1 = ((L2MinionInstance) _actor).getLeader().getX() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
			y1 = ((L2MinionInstance) _actor).getLeader().getY() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
			z1 = ((L2MinionInstance) _actor).getLeader().getZ();
			// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
			moveTo(x1, y1, z1);
			return;
		}
	}
	// Order to the L2MonsterInstance to random walk (1/100)
	else if (npc.getSpawn() != null && Rnd.nextInt(RANDOM_WALK_RATE) == 0 && /*!(_actor.isRaid() || _actor instanceof L2MinionInstance || _actor instanceof L2ChestInstance
			|| _actor instanceof L2GuardInstance || _actor.isNoRndWalk())*/npc.getRndWalkRange() > 0)
	{
		int x1, y1, z1;
		final int range = npc.getRndWalkRange();
		
		// If NPC with random coord in territory
		if (npc.getSpawn().getLocx() == 0 && npc.getSpawn().getLocy() == 0)
		{
			// Calculate a destination point in the spawn area
			int p[] = Territory.getInstance().getRandomPoint(npc.getSpawn().getLocation());
			x1 = p[0];
			y1 = p[1];
			z1 = p[2];
			
			// Calculate the distance between the current position of the L2Character and the target (x,y)
			double distance2 = _actor.getPlanDistanceSq(x1, y1);
			
			if (distance2 > (range + range) * (range + range))
			{
				npc.setisReturningToSpawnPoint(true);
				float delay = (float) Math.sqrt(distance2) / range;
				x1 = _actor.getX() + (int) ((x1 - _actor.getX()) / delay);
				y1 = _actor.getY() + (int) ((y1 - _actor.getY()) / delay);
			}
			
			// If NPC with random fixed coord, don't move (unless needs to return to spawnpoint)
			if (Territory.getInstance().getProcMax(npc.getSpawn().getLocation()) > 0 && !npc.isReturningToSpawnPoint())
				return;
		}
		else
		{
			// If NPC with fixed coord
			x1 = npc.getSpawn().getCurX();
			y1 = npc.getSpawn().getCurY();
			z1 = npc.getSpawn().getCurZ();
			
			if (!_actor.isInsideRadius(x1, y1, z1, range + range, true, false))
				npc.setisReturningToSpawnPoint(true);
			else
			{
				x1 += Rnd.nextInt(range * 2) - range;
				y1 += Rnd.nextInt(range * 2) - range;
				z1 = npc.getZ();
				
				if (Config.GEODATA > 0)
				{
					Location destiny = GeoData.getInstance().moveCheck(_actor.getX(), _actor.getY(), z1, x1, y1, z1, _actor.getInstanceId());
					x1 = destiny.getX();
					y1 = destiny.getY();
					z1 = destiny.getZ();
				}
			}
		}
		
		//_log.config("Curent pos ("+getX()+", "+getY()+"), moving to ("+x1+", "+y1+").");
		// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
		moveTo(x1, y1, z1);
	}
}

/**
 * Manage AI attack thinks of a L2Attackable (called by onEvtThink).<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Update the attack timeout if actor is running</li>
 * <li>If target is dead or timeout is expired, stop this attack and set the Intention to AI_INTENTION_ACTIVE</li>
 * <li>Call all L2Object of its Faction inside the Faction Range</li>
 * <li>Chose a target and order to attack it with magic skill or physical attack</li><BR><BR>
 *
 * TODO: Manage casting rules to healer mobs (like Ant Nurses)
 *
 */
private void thinkAttack()
{
	if (_actor instanceof L2MonsterInstance)
	{
		final L2Spawn spawn = ((L2Attackable)_actor).getSpawn();
		
		if (spawn != null)
		{
			int range = 3500;
			
			if (_actor.isAPC())
				range = 5000;
			
			if (_selfAnalysis.isArcher || _selfAnalysis.isMage)
				range += 5000;
			
			if (_actor.getInstanceId() > 0)
				range *= 4;
			
			if (!_actor.isInsideRadius(spawn.getCurX(), spawn.getCurY(), spawn.getCurZ(), range, true, false))
			{
				_actor.teleToLocation(spawn.getCurX(), spawn.getCurY(), spawn.getCurZ(), false);
				_actor.healHP(); // prevents minor exploiting with it
				_actor.setWalking();
				
				if (((L2MonsterInstance)_actor).hasMinions())
					((L2MonsterInstance)_actor)._minionList.recallMinions();
				
				_globalAggro = -7;
				return;
			}
		}
	}
	
	FastList<L2Character> hated;
	
	L2Character originalAttackTarget = getAttackTarget();
	// Check if target is dead or if timeout is expired to stop this attack
	if (originalAttackTarget == null || originalAttackTarget.isAlikeDead() || _attackTimeout < GameTimeController.getGameTicks())
	{
		// Stop hating this target after the attack timeout or if target is dead
		if (originalAttackTarget != null)
		{
			((L2Attackable)_actor).stopHating(originalAttackTarget);
		}
		
		hated = ((L2Attackable)_actor).get2MostHated();
		
		if (hated == null || hated.get(0) == null)
		{
			_actor.setWalking();
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		
		_selfAnalysis.currNumberOfAttacks = 0;
	}
	else
	{
		if (_actor.isConfused())
		{
			hated = new FastList<L2Character>();
			hated.add(originalAttackTarget);
			hated.add(null);
		}
		else
		{
			hated = ((L2Attackable)_actor).get2MostHated();
			
			if (hated == null || hated.get(0) == null)
			{
				_actor.setWalking();
				setIntention(AI_INTENTION_ACTIVE);
				return;
			}
		}
	}
	
	if (hated.get(0) != originalAttackTarget)
	{
		setAttackTarget(hated.get(0));
	}
	
	_mostHatedAnalysis.update(hated.get(0));
	_secondMostHatedAnalysis.update(hated.get(1));
	
	// self and buffs
	if (Rnd.get(100) < 40 && _selfAnalysis.lastBuffTick + 40 < GameTimeController.getGameTicks())
	{
		for (L2Skill sk : _selfAnalysis.buffSkills)
		{
			if (_actor.getFirstEffect(sk.getId()) == null)
			{
				if (!_actor.checkDoCastConditionsNPC(sk, _actor))
					continue;
				
				_actor.setTarget(_actor);
				clientStopMoving(null);
				_accessor.doCast(sk);
				// forcing long reuse delay so if cast get interrupted or there would be several buffs, doesn't cast again
				_selfAnalysis.lastBuffTick = GameTimeController.getGameTicks();
				return;
			}
		}
	}
	
	// Get all information needed to choose between physical or magical attack
	_actor.setTarget(_mostHatedAnalysis.character);
	
	double dist2 = _actor.getPlanDistanceSq(_mostHatedAnalysis.character.getX(), _mostHatedAnalysis.character.getY());
	int combinedCollision = (int) (_actor.getCollisionRadius() + _mostHatedAnalysis.character.getCollisionRadius());
	
	int longestCurrentSkillRange = 0;
	
	for (L2Skill skill : _actor.getAllSkills())
	{
		if (!skill.isPassive() && _actor.checkDoCastConditionsNPC(skill, skill.isPositive() ? _actor : _actor.getTarget()))
		{
			final int range = skill.getMobCastRange(_actor);
			if (range > longestCurrentSkillRange)
				longestCurrentSkillRange = range;
			
			if (longestCurrentSkillRange >= _selfAnalysis.maxCastRange)
				break;
		}
	}
	
	int range = _selfAnalysis.isMage || _selfAnalysis.isHealer ? longestCurrentSkillRange + combinedCollision : _actor.getPhysicalAttackRange() + combinedCollision;
	
	// Reconsider target next round if _actor hasn't got hits in for last 16 seconds
	if (!_actor.isMuted() && _attackTimeout - 160 < GameTimeController.getGameTicks() && _secondMostHatedAnalysis.character != null)
	{
		if (Util.checkIfInRange(1400, _actor, hated.get(1), true))
		{
			// take off 2* the amount the aggro is larger than second most
			((L2Attackable) _actor).reduceHate(hated.get(0), 2 * (((L2Attackable) _actor).getHating(hated.get(0)) - ((L2Attackable) _actor).getHating(hated.get(1))));
			// Calculate a new attack timeout
			_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
		}
	}
	
	boolean swap = false;
	
	if (_secondMostHatedAnalysis.character != null)
	{
		// Reconsider target during next round if actor is rooted and cannot reach mostHated but can	// reach secondMostHated
		if (_actor.isMovementDisabled())
		{
			if (dist2 > range * range && _actor.getPlanDistanceSq(_secondMostHatedAnalysis.character.getX(), _secondMostHatedAnalysis.character.getY()) < range * range)
			{
				swap = true;
			}
		}
		
		if (!swap)
		{
			if (_mostHatedAnalysis.character.isInvul())
			{
				if (!_secondMostHatedAnalysis.character.isInvul() && _secondMostHatedAnalysis.character.isInsideRadius(_actor, 1300, false, false))
				{
					swap = true;
				}
			}
		}
	}
	
	if (swap)
	{
		_mostHatedAnalysis.update(hated.get(1));
		_secondMostHatedAnalysis.update(hated.get(0));
		
		// Get all information needed to choose between physical or magical attack
		setAttackTarget(_mostHatedAnalysis.character);
		_actor.setTarget(_mostHatedAnalysis.character);
		
		dist2 = _actor.getPlanDistanceSq(_mostHatedAnalysis.character.getX(), _mostHatedAnalysis.character.getY());
		combinedCollision = (int) (_actor.getCollisionRadius() + _mostHatedAnalysis.character.getCollisionRadius());
		range = _selfAnalysis.isMage || _selfAnalysis.isHealer ? longestCurrentSkillRange + combinedCollision : _actor.getPhysicalAttackRange() + combinedCollision;
	}
	
	if (_selfAnalysis.hasHealOrResurrect && _actor.getCurrentHp() > _actor.getMaxHp() / 2.3)
	{
		boolean skip = false;
		
		if (!_selfAnalysis.isHealer)
		{
			if (Rnd.get(3) > 0)
				skip = true;
		}
		
		if (!skip)
		{
			final String faction_id = ((L2Npc)_actor).getFactionIdString();
			
			for (L2Character obj : _actor.getKnownList().getKnownCharacters())
			{
				if (obj != null && obj instanceof L2Attackable)
				{
					L2Npc npc = (L2Npc)obj;
					
					// Check if the L2Object is inside the Faction Range of the actor
					if (npc.hasAI() && npc.getAI() != null)
					{
						final boolean sameMobGroup = _actor.isInSameMobGroupAs(obj);
						
						if (!sameMobGroup)
						{
							//Handle SevenSigns mob Factions
							final String npcfaction = npc.getFactionId();
							
							if (!(!faction_id.equalsIgnoreCase("NONE") && faction_id.equalsIgnoreCase(npcfaction)))
								continue;
						}
						
						boolean ok = sameMobGroup ? true : _actor.isInsideRadius(npc, (int) (((L2Attackable)_actor).getFactionRange() + npc.getCollisionRadius()), true, false) ? true : false;
						
						// heal or resurrect friends
						if (ok && npc.getCurrentHp() < npc.getMaxHp() * 0.85)
						{
							if (GeoData.getInstance().canSeeTarget(_actor, npc))
							{
								if (npc.isDead())
								{
									for (L2Skill sk : _selfAnalysis.resurrectSkills)
									{
										int chance = 17;
										
										if (_selfAnalysis.isHealer)
											chance = 100;
										
										if (chance < Rnd.get(100)) // chance
											continue;
										
										if (!Util.checkIfInRange(sk.getMobCastRange(_actor), _actor, npc, true))
											continue;
										
										if (!_actor.checkDoCastConditionsNPC(sk, npc))
											continue;
										
										L2Object OldTarget = _actor.getTarget();
										_actor.setTarget(npc);
										// would this ever be fast enough											// for the decay not to run?											// giving some extra seconds
										DecayTaskManager.getInstance().cancelDecayTask(npc);
										DecayTaskManager.getInstance().addDecayTask(npc);
										clientStopMoving(null);
										_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
										_accessor.doCast(sk);
										_actor.setTarget(OldTarget);
										return;
									}
								}
								else
								{
									for (L2Skill sk : _selfAnalysis.healSkills)
									{
										int chance = 13;
										
										if (_actor instanceof L2MinionInstance)
										{
											// minions support boss
											if (((L2MinionInstance) _actor).getLeader() == npc)
												chance = 20;
											else
												chance = 10;
										}
										
										if (npc instanceof L2GrandBossInstance)
											chance = 6;
										
										if (_selfAnalysis.isHealer)
											chance += 60;
										
										if (chance < Rnd.get(100)) // chance
											continue;
										
										if (!Util.checkIfInRange(sk.getMobCastRange(_actor), _actor, npc, true))
											continue;
										
										if (!_actor.checkDoCastConditionsNPC(sk, npc))
											continue;
										
										L2Object OldTarget = _actor.getTarget();
										_actor.setTarget(npc);
										clientStopMoving(null);
										_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
										_accessor.doCast(sk);
										_actor.setTarget(OldTarget);
										return;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	if (!_kited && !_actor.isMovementDisabled())
	{
		boolean shouldRunAway = false;
		
		if (((_selfAnalysis.isMage || _selfAnalysis.isHealer) && (_mostHatedAnalysis.character.isRooted() || _mostHatedAnalysis.isSlower)) || _selfAnalysis.isArcher)
		{
			int runAwayFactor = 1;
			
			for (L2PcInstance player : _actor.getKnownList().getKnownPlayersInRadius(80))
			{
				if (!player.isArcherClass() && !player.isMageClass() && player.getAI().getAttackTarget() == _actor)
				{
					runAwayFactor += _selfAnalysis.isArcher ? 49 : 30;
					
					if (runAwayFactor > 99)
						break;
				}
			}
			
			if (runAwayFactor > 99 || Rnd.get(100) < runAwayFactor)
				shouldRunAway = true;
		}
		
		final int minKiteRange = _mostHatedAnalysis.character.getPhysicalAttackRange() + combinedCollision;
		final int chanceKite = Rnd.get(7);
		final int KiteToRange = (int) Math.min(range, chanceKite == 0 ? Math.max(minKiteRange * 3, minKiteRange + 120)*3 : chanceKite <= 2 ? Math.max(minKiteRange * 3, minKiteRange + 120)*2 : Math.max(minKiteRange * 3.5, minKiteRange + 150));
		
		if (_actor.getRunSpeed() > 1 &&
				(shouldRunAway || (dist2 < minKiteRange * minKiteRange
						&& !_selfAnalysis.isFighter && !_selfAnalysis.isBalanced && !_selfAnalysis.isDagger
						&& ((_selfAnalysis.hasLongRangeSkills && (_selfAnalysis.isMage || _selfAnalysis.isHealer)) || _selfAnalysis.isArcher)
						&& !_mostHatedAnalysis.isArcher && !_mostHatedAnalysis.isHealer && !_mostHatedAnalysis.isMage
						&& (_mostHatedAnalysis.character.isRooted() || _mostHatedAnalysis.isSlower)
						&& (_selfAnalysis.isArcher ? 90 : _selfAnalysis.isMage ? 20 : Config.GEODATA == 2 ? 15 : 12) >= Rnd.get(100))) // chance
				)
		{
			final int posX = _actor.getX();
			final int posY = _actor.getY();
			final int posZ = _actor.getZ();
			final int targetX = _mostHatedAnalysis.character.getX();
			final int targetY = _mostHatedAnalysis.character.getY();
			final int targetZ = _mostHatedAnalysis.character.getZ();
			
			final int maxMoveDistance = (int) Math.sqrt(KiteToRange*KiteToRange + dist2);
			
			int newX = posX, newY = posY, newZ = posZ;
			int newX2 = newX, newY2 = newY, newZ2 = newZ;
			
			int count = 0;
			int maxDist = 0, maxDist2 = 0;
			
			final int kiteDistLimit = _actor.isAPC() ? 10000 : 8500;
			
			final L2Spawn spawn = _actor instanceof L2MinionInstance ? ((L2MinionInstance)_actor).getLeader().getSpawn() : ((L2Attackable)_actor).getSpawn();
			
			do
			{
				int tempNewX = targetX, tempNewY = targetY;
				
				int x = Rnd.get(-KiteToRange, KiteToRange);
				int y = (int) (Math.sqrt(KiteToRange*KiteToRange - x*x) * (Rnd.get(2) == 0 ? 1 : -1));
				
				tempNewX += x;
				tempNewY += y;
				
				if (Util.calculateDistance(posX, posY, posZ, tempNewX, tempNewY, posZ, false) > maxMoveDistance)
				{
					if (maxDist2 < KiteToRange)
					{
						if (Config.GEODATA > 0)
						{
							Location destiny = GeoData.getInstance().moveCheck(posX, posY, posZ, tempNewX, tempNewY, posZ, _actor.getInstanceId());
							
							if (spawn != null && Util.calculateDistance(destiny.getX(), destiny.getY(), destiny.getZ(), spawn.getCurX(), spawn.getCurY()) >= kiteDistLimit)
								continue;
							
							final int dis = (int) Util.calculateDistance(targetX, targetY, targetZ, destiny.getX(), destiny.getY());
							
							if (dis > maxDist2)
							{
								maxDist2 = dis;
								newX2 = destiny.getX();
								newY2 = destiny.getY();
								newZ2 = destiny.getZ();
							}
						}
					}
					continue;
				}
				
				count++;
				
				if (Config.GEODATA > 0)
				{
					Location destiny = GeoData.getInstance().moveCheck(posX, posY, posZ, tempNewX, tempNewY, posZ, _actor.getInstanceId());
					
					if (spawn != null && Util.calculateDistance(destiny.getX(), destiny.getY(), destiny.getZ(), spawn.getCurX(), spawn.getCurY()) >= kiteDistLimit)
						continue;
					
					final int dis = (int) Util.calculateDistance(targetX, targetY, targetZ, destiny.getX(), destiny.getY());
					if (dis > maxDist)
					{
						maxDist = dis;
						newX = destiny.getX();
						newY = destiny.getY();
						newZ = destiny.getZ();
						
						if (maxDist == KiteToRange)
							break;
					}
				}
			}
			while (count < 12);
			
			final int space = (int) Util.calculateDistance(posX, posY, posZ, newX, newY);
			
			if (space > 25 || maxDist2 >= KiteToRange-10)
			{
				_kited = true;
				
				if (space > 25)
					setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(newX, newY, newZ, 0));
				else
					setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(newX2, newY2, newZ2, 0));
				return;
			}
		}
	}
	
	_kited = false;
	
	int originalDist2 = (int) dist2;
	
	if (_mostHatedAnalysis.character.isMoving() && _actor.isBehind(_mostHatedAnalysis.character))
		dist2 -= 120*120;
	
	// Check if the actor is far from target
	if (_selfAnalysis.isMage || _selfAnalysis.isHealer || dist2 > range * range)
	{
		if (_selfAnalysis.hasLongRangeSkills || !_selfAnalysis.healSkills.isEmpty())
		{
			if (!_mostHatedAnalysis.isCanceled)  // check for long ranged skills and heal/buff skills
			{
				for (L2Skill sk : _selfAnalysis.cancelSkills)
				{
					if (_mostHatedAnalysis.character.getFirstEffect(sk) == null)
					{
						int castRange = sk.getMobCastRange(_actor) + combinedCollision;
						if (dist2 > castRange * castRange || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
							continue;
						if (Rnd.nextInt(100) <= 8)
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_mostHatedAnalysis.isCanceled = true;
							_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
							return;
						}
					}
				}
			}
			
			if (_selfAnalysis.lastDebuffTick + 50 < GameTimeController.getGameTicks())
			{
				for (L2Skill sk : _selfAnalysis.debuffSkills)
				{
					if (_mostHatedAnalysis.character.getFirstEffect(sk) == null)
					{
						int castRange = sk.getMobCastRange(_actor) + combinedCollision;
						if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
							continue;
						int chance = 12;
						if (_selfAnalysis.isFighter && _mostHatedAnalysis.isMage)
							chance = 15;
						if (_selfAnalysis.isFighter && _mostHatedAnalysis.isArcher)
							chance = 15;
						if (_selfAnalysis.isMage)
							chance = 33;
						if (_selfAnalysis.isHealer)
							chance = 12;
						/*if (_mostHatedAnalysis.isMagicResistant)
							chance /= 2;*/
						
						if (Rnd.nextInt(100) <= chance)
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_selfAnalysis.lastDebuffTick = GameTimeController.getGameTicks();
							_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
							return;
						}
					}
				}
			}
			if (!_mostHatedAnalysis.character.isMuted())
			{
				int chance = 15;
				if (!(_mostHatedAnalysis.isMage || _mostHatedAnalysis.isHealer))
					chance = 3;
				for (L2Skill sk : _selfAnalysis.muteSkills)
				{
					if (_mostHatedAnalysis.character.getFirstEffect(sk) == null)
					{
						int castRange = sk.getMobCastRange(_actor) + combinedCollision;
						if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
							continue;
						if (Rnd.nextInt(100) <= chance)
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
							return;
						}
					}
				}
			}
			if (_secondMostHatedAnalysis.character != null && !_secondMostHatedAnalysis.character.isMuted() && (_secondMostHatedAnalysis.isMage || _secondMostHatedAnalysis.isTank || _secondMostHatedAnalysis.isBalanced))
			{
				double secondHatedDist2 = _actor.getPlanDistanceSq(_secondMostHatedAnalysis.character.getX(), _secondMostHatedAnalysis.character.getY());
				
				int chance = 20;
				
				if (_secondMostHatedAnalysis.isHealer)
					chance += 15;
				
				for (L2Skill sk : _selfAnalysis.muteSkills)
				{
					if (_secondMostHatedAnalysis.character.getFirstEffect(sk) == null)
					{
						int castRange = sk.getMobCastRange(_actor) + combinedCollision;
						if ((secondHatedDist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _secondMostHatedAnalysis.character))
							continue;
						if (Rnd.nextInt(100) <= chance)
						{
							_actor.setTarget(_secondMostHatedAnalysis.character);
							clientStopMoving(null);
							_accessor.doCast(sk);
							_actor.setTarget(_mostHatedAnalysis.character);
							return;
						}
					}
				}
			}
			
			if (_selfAnalysis.muteSkills.size() > 0)
			{
				if (Rnd.nextInt(100) < 20)
				{
					L2PcInstance mostHatedPC = _mostHatedAnalysis.character.getActingPlayer();
					
					if (mostHatedPC != null && mostHatedPC.getParty() != null)
					{
						L2Attackable mob = (L2Attackable) _actor;
						
						for (L2PcInstance player : mostHatedPC.getParty().getPartyMembers())
						{
							if (player.isHealerClass() && !player.isMuted())
							{
								if (mob.getAggroList().containsKey(player))
								{
									double healerDist2 = _actor.getPlanDistanceSq(player.getX(), player.getY());
									
									for (L2Skill sk : _selfAnalysis.muteSkills)
									{
										if (player.getFirstEffect(sk) == null)
										{
											int castRange = sk.getMobCastRange(_actor) + combinedCollision;
											
											if ((healerDist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, player))
												continue;
											
											_actor.setTarget(player);
											clientStopMoving(null);
											_accessor.doCast(sk);
											_actor.setTarget(_mostHatedAnalysis.character);
											return;
										}
									}
								}
							}
						}
					}
				}
			}
			
			if (!_mostHatedAnalysis.character.isSleeping())
			{
				for (L2Skill sk : _selfAnalysis.sleepSkills)
				{
					int castRange = sk.getMobCastRange(_actor) + combinedCollision;
					if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
						continue;
					
					if (Rnd.nextInt(100) <= (_selfAnalysis.isHealer ? 36 : 12))
					{
						clientStopMoving(null);
						_accessor.doCast(sk);
						_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
						return;
					}
				}
			}
			
			if (_secondMostHatedAnalysis.character != null && !_secondMostHatedAnalysis.character.isSleeping())
			{
				double secondHatedDist2 = _actor.getPlanDistanceSq(_secondMostHatedAnalysis.character.getX(), _secondMostHatedAnalysis.character.getY());
				for (L2Skill sk : _selfAnalysis.sleepSkills)
				{
					int castRange = sk.getMobCastRange(_actor) + combinedCollision;
					if ((secondHatedDist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _secondMostHatedAnalysis.character))
						continue;
					
					if (Rnd.nextInt(100) <= (_selfAnalysis.isHealer ? 50 : 35))
					{
						_actor.setTarget(_secondMostHatedAnalysis.character);
						clientStopMoving(null);
						_accessor.doCast(sk);
						_actor.setTarget(_mostHatedAnalysis.character);
						return;
					}
				}
			}
			
			if (!_mostHatedAnalysis.character.isRooted())
			{
				int chance = _mostHatedAnalysis.isSlower ? 7 : 14;
				
				if ((_selfAnalysis.isArcher || _selfAnalysis.isMage || _selfAnalysis.isHealer) && (!_mostHatedAnalysis.isArcher && !_mostHatedAnalysis.isMage))
				{
					chance += 20;
					
					if (_mostHatedAnalysis.character.isMoving() && _mostHatedAnalysis.character.isFacing(_actor, 50))
						chance += 15;
				}
				else if ((_selfAnalysis.isFighter || _selfAnalysis.isBalanced || _selfAnalysis.isDagger) &&_mostHatedAnalysis.isArcher)
				{
					chance += 17;
					
					if (_mostHatedAnalysis.character.isMoving() && _actor.isBehind(_mostHatedAnalysis.character, 120))
						chance += 50;
				}
				else if (_mostHatedAnalysis.character.isMoving() && _actor.isBehind(_mostHatedAnalysis.character, 120))
					chance += 15;
				
				for (L2Skill sk : _selfAnalysis.rootSkills)
				{
					int castRange = sk.getMobCastRange(_actor) + combinedCollision;
					if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
						continue;
					if (Rnd.nextInt(100) <= chance)
					{
						clientStopMoving(null);
						_accessor.doCast(sk);
						_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
						return;
					}
				}
			}
			
			if (_selfAnalysis.rootSkills.size() > 0)
			{
				if (Rnd.nextInt(100) < 15)
				{
					L2PcInstance mostHatedPC = _mostHatedAnalysis.character.getActingPlayer();
					
					if (mostHatedPC != null && mostHatedPC.getParty() != null)
					{
						L2Attackable mob = (L2Attackable) _actor;
						
						for (L2PcInstance player : mostHatedPC.getParty().getPartyMembers())
						{
							if (player.isArcherClass() && !player.isRooted())
							{
								if (mob.getAggroList().containsKey(player))
								{
									double archerDist2 = _actor.getPlanDistanceSq(player.getX(), player.getY());
									
									for (L2Skill sk : _selfAnalysis.rootSkills)
									{
										int castRange = sk.getMobCastRange(_actor) + combinedCollision;
										
										if ((archerDist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, player))
											continue;
										
										_actor.setTarget(player);
										clientStopMoving(null);
										_accessor.doCast(sk);
										_actor.setTarget(_mostHatedAnalysis.character);
										return;
									}
								}
							}
						}
					}
				}
			}
			
			if (!_mostHatedAnalysis.character.isAttackingDisabled())
			{
				for (L2Skill sk : _selfAnalysis.generalDisablers)
				{
					if (_mostHatedAnalysis.character.getFirstEffect(sk) == null)
					{
						int castRange = sk.getMobCastRange(_actor) + combinedCollision;
						if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
							continue;
						if (Rnd.nextInt(100) <= (_mostHatedAnalysis.isFighter || _mostHatedAnalysis.isBalanced ? 25 : 14))
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
							return;
						}
					}
				}
			}
			
			if (_actor.getCurrentHp() < _actor.getMaxHp() * 0.8)
			{
				if (!(!_selfAnalysis.isHealer && _actor.getLastSkillCast() != null && _actor.getLastSkillCast().isHeal()))
				{
					for (L2Skill sk : _selfAnalysis.healSkills)
					{
						if (!_actor.checkDoCastConditionsNPC(sk, _actor))
							continue;
						
						int chance = 7;
						if (_mostHatedAnalysis.character.isAttackingDisabled())
							chance += 10;
						if (_secondMostHatedAnalysis.character == null || _secondMostHatedAnalysis.character.isAttackingDisabled())
							chance += 10;
						if (_actor.isRooted() && (_selfAnalysis.isBalanced || _selfAnalysis.isFighter || _selfAnalysis.isDagger))
							chance += 20;
						if (_selfAnalysis.isHealer)
							chance += 70;
						
						if (Rnd.nextInt(100) <= chance)
						{
							_actor.setTarget(_actor);
							clientStopMoving(null);
							_accessor.doCast(sk);
							_actor.setTarget(_mostHatedAnalysis.character);
							return;
						}
					}
				}
			}
			
			if (_selfAnalysis.hasSummonDisableSkills && _selfAnalysis.summonDisablers.size() > 0)
			{
				if (Rnd.nextInt(100) < 17)
				{
					L2Attackable mob = (L2Attackable) _actor;
					
					for (L2Character character : mob.getAggroList().keySet())
					{
						if (character != null && character instanceof L2Summon)
						{
							double summonDist2 = _actor.getPlanDistanceSq(character.getX(), character.getY());
							
							for (L2Skill sk : _selfAnalysis.summonDisablers)
							{
								int castRange = sk.getMobCastRange(_actor) + combinedCollision;
								
								if ((summonDist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, character))
									continue;
								
								_actor.setTarget(character);
								clientStopMoving(null);
								_accessor.doCast(sk);
								_actor.setTarget(_mostHatedAnalysis.character);
								return;
							}
						}
					}
				}
			}
			
			// chance decision for launching long range skills
			int castingChance = 5;
			if (_selfAnalysis.isMage)
				castingChance = 100; // mages
			else if (_selfAnalysis.isHealer)
				castingChance = 40; // healers
			else if (_selfAnalysis.isBalanced)
			{
				if (!_mostHatedAnalysis.isFighter || _mostHatedAnalysis.isArcher) // advance to mages or archers
					castingChance = 25;
				else
					castingChance = 55; // stay away from fighters
			}
			else if (_selfAnalysis.isDagger)
			{
				if (!_mostHatedAnalysis.isFighter || _mostHatedAnalysis.isArcher) // advance to mages or archers
					castingChance = 40;
				else
					castingChance = 70; // stay away from fighters
			}
			else if (_selfAnalysis.isFighter)
			{
				if (_mostHatedAnalysis.isMage || _mostHatedAnalysis.isArcher)
					castingChance = 3;
				else
					castingChance = 6;
			}
			else if (_selfAnalysis.isArcher)
			{
				if (_mostHatedAnalysis.isMage || _mostHatedAnalysis.isArcher)
					castingChance = 50;
				else
					castingChance = 100;
			}
			
			if (_actor.isMovementDisabled())
				castingChance = 120;
			else if (!_mostHatedAnalysis.isSlower && (_selfAnalysis.isBalanced || _selfAnalysis.isFighter || _selfAnalysis.isDagger))
				castingChance += 11;
			
			for (L2Skill sk : _selfAnalysis.generalSkills)
			{
				int castRange = sk.getMobCastRange(_actor) + combinedCollision;
				
				if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
					continue;
				
				int realCastingChance = castingChance;
				
				if (_selfAnalysis.isMage || _selfAnalysis.isHealer)
				{
					if (!sk.isMagic())
						realCastingChance /= 2.2;
				}
				else
				{
					if (sk.isMagic())
						realCastingChance /= 3;
				}
				
				if (Rnd.nextInt(100) <= realCastingChance)
				{
					clientStopMoving(null);
					_accessor.doCast(sk);
					_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
					return;
				}
			}
		}
		
		// unmoving mobs
		if (_actor.getRunSpeed() < 2)
			return;
		
		// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
		if (_selfAnalysis.isMage)
		{
			if (originalDist2 < range * range) // don't move backwards here
				return;
		}
		
		// Minions following leader
		if (_selfAnalysis.isHealer)
		{
			if (_actor instanceof L2MinionInstance && ((L2MinionInstance) _actor).getLeader() != null)
			{
				int offset = 600;
				
				if (((L2MinionInstance) _actor).getLeader().isRunning())
					_actor.setRunning();
				else
					_actor.setWalking();
				
				if (_actor.getPlanDistanceSq(((L2MinionInstance) _actor).getLeader()) > offset * offset)
				{
					int x1, y1, z1;
					x1 = ((L2MinionInstance) _actor).getLeader().getX() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
					y1 = ((L2MinionInstance) _actor).getLeader().getY() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
					z1 = ((L2MinionInstance) _actor).getLeader().getZ();
					// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
					moveTo(x1, y1, z1);
					return;
				}
				else if (Rnd.nextInt(RANDOM_WALK_RATE) == 0)
				{
					// self and clan buffs
					for (L2Skill sk : _selfAnalysis.buffSkills)
					{
						if (_actor.getFirstEffect(sk.getId()) == null)
						{
							// if clan buffs, don't buff every time
							if (sk.getTargetType(_actor) != L2Skill.SkillTargetType.TARGET_SELF && sk.getTargetType(_actor) != L2Skill.SkillTargetType.TARGET_SELF_AND_PET && Rnd.nextInt(2) != 0)
								continue;
							if (!_actor.checkDoCastConditionsNPC(sk, _actor))
								continue;
							L2Object OldTarget = _actor.getTarget();
							_actor.setTarget(_actor);
							clientStopMoving(null);
							_accessor.doCast(sk);
							_actor.setTarget(OldTarget);
							return;
						}
					}
				}
			}
			
			return;
		}
		else if (!_selfAnalysis.isArcher && _selfAnalysis.hasTeleportRushSkills)
		{
			for (L2Skill sk : _selfAnalysis.teleportRushSkills)
			{
				int castRange = sk.getMobCastRange(_actor) + combinedCollision;
				
				if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
					continue;
				
				clientStopMoving(null);
				_accessor.doCast(sk);
				try
				{
					((L2Attackable)_actor).addDamageHate((L2Character)_actor.getTarget(), 8000);
				}
				catch (Exception e)
				{
				}
				return;
			}
		}
		
		if (_mostHatedAnalysis.character.isMoving() && _actor.isBehind(_mostHatedAnalysis.character))
			range -= 170;
		if (range < -15)
			range = -15;
		
		_selfAnalysis.currNumberOfAttacks = 0;
		moveToPawn(_mostHatedAnalysis.character, range);
		return;
	}
	else // Else, if this is close enough for physical attacks
	{		// In case many mobs are trying to hit from same place, move a bit,// circling around the target
		if (!_actor.isMovementDisabled())
		{
			if (_selfAnalysis.shouldAttackFromBehind)
			{
				if (!_actor.isBehind(_mostHatedAnalysis.character))
				{
					if (_mostHatedAnalysis.character.getTarget() != _actor || _mostHatedAnalysis.character.getAI() == null ||
							(_mostHatedAnalysis.character.getAI().getFollowTarget() != _actor && _mostHatedAnalysis.character.getAI().getAttackTarget() != _actor
							&& _mostHatedAnalysis.character.getAI().getCastTarget() != _actor))
					{
						double ph = Util.convertHeadingToDegree(_mostHatedAnalysis.character.getHeading());
						
						ph += 180;
						
						if(ph>360)
							ph-=360;
						
						ph = (Math.PI * ph) / 180;
						
						int x = 0,y = 0;
						
						int px = _mostHatedAnalysis.character.getX();
						int py = _mostHatedAnalysis.character.getY();
						
						if (_mostHatedAnalysis.isArcher || _mostHatedAnalysis.isMage || _mostHatedAnalysis.isHealer)
						{
							x = (int) (px + ((combinedCollision + 25) * Math.cos(ph)));
							y = (int) (py + ((combinedCollision + 25) * Math.sin(ph)));
						}
						else //target is a close-ranged fighter
						{
							x = (int) (px + (range * Math.cos(ph)));
							y = (int) (py + (range * Math.sin(ph)));
						}
						
						int z = _mostHatedAnalysis.character.getZ();
						
						boolean skip = false;
						
						if (Config.GEODATA > 0)
						{
							Location destiny = GeoData.getInstance().moveCheck(_actor.getX(), _actor.getY(), _actor.getZ(), x, y, z, _actor.getInstanceId());
							
							if (x != destiny.getX() || y != destiny.getY())
								skip = true;
						}
						
						if (!skip)
						{
							moveTo(x, y, z);
							return;
						}
					}
				}
			}
			else if (!_selfAnalysis.isArcher && Rnd.nextInt(100) <= 40) // check it once per 3 seconds
			{
				for (L2Object nearby : _actor.getKnownList().getKnownCharactersInRadius(12))
				{
					if (nearby instanceof L2Attackable && nearby != _mostHatedAnalysis.character)
					{
						int diffx = Rnd.get(combinedCollision, combinedCollision + 40);
						if (Rnd.get(10) < 5)
							diffx = -diffx;
						int diffy = Rnd.get(combinedCollision, combinedCollision + 40);
						if (Rnd.get(10) < 5)
							diffy = -diffy;
						moveTo(_mostHatedAnalysis.character.getX() + diffx, _mostHatedAnalysis.character.getY() + diffy, _mostHatedAnalysis.character.getZ());
						return;
					}
				}
			}
		}
		
		// Calculate a new attack timeout.
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
		
		if (_actor.isAttackingDisabled() || _selfAnalysis.minNumberOfContAttacks <= 0 || _selfAnalysis.currNumberOfAttacks == 0)
		{
			// check for close combat skills && heal/buff skills
			if (!_mostHatedAnalysis.isCanceled)
			{
				for (L2Skill sk : _selfAnalysis.cancelSkills)
				{
					if (_mostHatedAnalysis.character.getFirstEffect(sk) == null)
					{
						if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
							continue;
						int castRange = sk.getMobCastRange(_actor) + combinedCollision;
						if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
							continue;
						if (Rnd.nextInt(100) <= 7)
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_mostHatedAnalysis.isCanceled = true;
							return;
						}
					}
				}
			}
			if (_selfAnalysis.lastDebuffTick + 50 < GameTimeController.getGameTicks())
			{
				for (L2Skill sk : _selfAnalysis.debuffSkills)
				{
					if (_mostHatedAnalysis.character.getFirstEffect(sk) == null)
					{
						if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
							continue;
						int castRange = sk.getMobCastRange(_actor) + combinedCollision;
						if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
							continue;
						int chance = 12;
						if (_selfAnalysis.isFighter && _mostHatedAnalysis.isMage)
							chance = 15;
						if (_selfAnalysis.isFighter && _mostHatedAnalysis.isArcher)
							chance = 15;
						if (_selfAnalysis.isMage)
							chance = 33;
						if (_selfAnalysis.isHealer)
							chance = 18;
						if (sk.getMobCastRange(_actor) < 200)
							chance += 5;
						if (Rnd.nextInt(100) <= chance)
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							_selfAnalysis.lastDebuffTick = GameTimeController.getGameTicks();
							return;
						}
					}
				}
			}
			if (!_mostHatedAnalysis.character.isMuted() && (_mostHatedAnalysis.isMage || _mostHatedAnalysis.isBalanced))
			{
				for (L2Skill sk : _selfAnalysis.muteSkills)
				{
					if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
						continue;
					int castRange = sk.getMobCastRange(_actor) + combinedCollision;
					if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
						continue;
					if (Rnd.nextInt(100) <= 11)
					{
						clientStopMoving(null);
						_accessor.doCast(sk);
						return;
					}
				}
			}
			if (_secondMostHatedAnalysis.character != null && !_secondMostHatedAnalysis.character.isMuted() && (_secondMostHatedAnalysis.isMage || _secondMostHatedAnalysis.isBalanced))
			{
				double secondHatedDist2 = _actor.getPlanDistanceSq(_secondMostHatedAnalysis.character.getX(), _secondMostHatedAnalysis.character.getY());
				for (L2Skill sk : _selfAnalysis.muteSkills)
				{
					if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
						continue;
					int castRange = sk.getMobCastRange(_actor) + combinedCollision;
					if ((secondHatedDist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _secondMostHatedAnalysis.character))
						continue;
					if (Rnd.nextInt(100) <= 20)
					{
						_actor.setTarget(_secondMostHatedAnalysis.character);
						clientStopMoving(null);
						_accessor.doCast(sk);
						_actor.setTarget(_mostHatedAnalysis.character);
						return;
					}
				}
			}
			
			if (_selfAnalysis.muteSkills.size() > 0)
			{
				if (Rnd.nextInt(100) < 20)
				{
					L2PcInstance mostHatedPC = _mostHatedAnalysis.character.getActingPlayer();
					
					if (mostHatedPC != null && mostHatedPC.getParty() != null)
					{
						L2Attackable mob = (L2Attackable) _actor;
						
						for (L2PcInstance player : mostHatedPC.getParty().getPartyMembers())
						{
							if (player.isHealerClass() && !player.isMuted())
							{
								if (mob.getAggroList().containsKey(player))
								{
									double healerDist2 = _actor.getPlanDistanceSq(player.getX(), player.getY());
									
									for (L2Skill sk : _selfAnalysis.muteSkills)
									{
										int castRange = sk.getMobCastRange(_actor) + combinedCollision;
										
										if ((healerDist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, player))
											continue;
										
										_actor.setTarget(player);
										clientStopMoving(null);
										_accessor.doCast(sk);
										_actor.setTarget(_mostHatedAnalysis.character);
										return;
									}
								}
							}
						}
					}
				}
			}
			
			if (!_mostHatedAnalysis.character.isSleeping() && _selfAnalysis.isHealer)
			{
				for (L2Skill sk : _selfAnalysis.sleepSkills)
				{
					int castRange = sk.getMobCastRange(_actor) + combinedCollision;
					if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
						continue;
					if (Rnd.nextInt(100) <= 13)
					{
						clientStopMoving(null);
						_accessor.doCast(sk);
						_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
						return;
					}
				}
			}
			if (_secondMostHatedAnalysis.character != null && !_secondMostHatedAnalysis.character.isSleeping())
			{
				double secondHatedDist2 = _actor.getPlanDistanceSq(_secondMostHatedAnalysis.character.getX(), _secondMostHatedAnalysis.character.getY());
				for (L2Skill sk : _selfAnalysis.sleepSkills)
				{
					if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
						continue;
					int castRange = sk.getMobCastRange(_actor) + combinedCollision;
					if ((secondHatedDist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _secondMostHatedAnalysis.character))
						continue;
					if (Rnd.nextInt(100) <= (_selfAnalysis.isHealer ? 50 : 30))
					{
						_actor.setTarget(_secondMostHatedAnalysis.character);
						clientStopMoving(null);
						_accessor.doCast(sk);
						_actor.setTarget(_mostHatedAnalysis.character);
						return;
					}
				}
			}
			if (!_mostHatedAnalysis.character.isRooted() && _mostHatedAnalysis.isFighter && !_selfAnalysis.isFighter)
			{
				for (L2Skill sk : _selfAnalysis.rootSkills)
				{
					if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
						continue;
					int castRange = sk.getMobCastRange(_actor) + combinedCollision;
					if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
						continue;
					if (Rnd.nextInt(100) <= (_selfAnalysis.isHealer ? 10 : 4))
					{
						clientStopMoving(null);
						_accessor.doCast(sk);
						return;
					}
				}
			}
			if (!_mostHatedAnalysis.character.isAttackingDisabled())
			{
				for (L2Skill sk : _selfAnalysis.generalDisablers)
				{
					if (_mostHatedAnalysis.character.getFirstEffect(sk) == null)
					{
						if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
							continue;
						int castRange = sk.getMobCastRange(_actor) + combinedCollision;
						if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
							continue;
						if (Rnd.nextInt(100) <= ((sk.getMobCastRange(_actor) < 200) ? 10 : 6))
						{
							clientStopMoving(null);
							_accessor.doCast(sk);
							return;
						}
					}
				}
			}
			if (_actor.getCurrentHp() < _actor.getMaxHp() * (_selfAnalysis.isHealer ? 0.85 : 0.5))
			{
				for (L2Skill sk : _selfAnalysis.healSkills)
				{
					if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
						continue;
					if (!_actor.checkDoCastConditionsNPC(sk, _actor))
						continue;
					int chance = (_selfAnalysis.isHealer ? 50 : 7);
					if (!_selfAnalysis.isMage && _actor.isAttackingDisabled())
						chance += 25;
					if (_mostHatedAnalysis.character.isAttackingDisabled())
						chance += 10;
					if (_secondMostHatedAnalysis.character == null || _secondMostHatedAnalysis.character.isAttackingDisabled())
						chance += 10;
					if (Rnd.nextInt(100) <= chance)
					{
						_actor.setTarget(_actor);
						clientStopMoving(null);
						_accessor.doCast(sk);
						_actor.setTarget(_mostHatedAnalysis.character);
						return;
					}
				}
			}
			
			if (_selfAnalysis.hasSummonDisableSkills && _selfAnalysis.summonDisablers.size() > 0)
			{
				if (Rnd.nextInt(100) < 17)
				{
					L2Attackable mob = (L2Attackable) _actor;
					
					for (L2Character character : mob.getAggroList().keySet())
					{
						if (character != null && character instanceof L2Summon)
						{
							double summonDist2 = _actor.getPlanDistanceSq(character.getX(), character.getY());
							
							for (L2Skill sk : _selfAnalysis.summonDisablers)
							{
								int castRange = sk.getMobCastRange(_actor) + combinedCollision;
								
								if ((summonDist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, character))
									continue;
								
								_actor.setTarget(character);
								clientStopMoving(null);
								_accessor.doCast(sk);
								_actor.setTarget(_mostHatedAnalysis.character);
								return;
							}
						}
					}
				}
			}
			
			for (L2Skill sk : _selfAnalysis.generalSkills)
			{
				if ((_actor.isMuted() && sk.isMagic()) || (_actor.isPhysicalMuted() && !sk.isMagic()))
					continue;
				int castRange = sk.getMobCastRange(_actor) + combinedCollision;
				if ((dist2 > castRange * castRange) || !_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
					continue;
				
				// chance decision for launching general skills in melee fight close range skills should be higher, long range lower
				int castingChance = 5;
				
				if (_selfAnalysis.isMage)
				{
					castingChance = 100; // mages
				}
				else if (_selfAnalysis.isHealer)
				{
					if (sk.getMobCastRange(_actor) < 200)
						castingChance = 35;
					else
						castingChance = 25;
				}
				else if (_selfAnalysis.isBalanced)
				{
					if (sk.getMobCastRange(_actor) < 200)
						castingChance = 25;
					else
					{
						if (_mostHatedAnalysis.isMage) // hit mages
							castingChance = 15;
						else
							castingChance = 20;
					}
				}
				else if (_selfAnalysis.isArcher)
				{
					if (sk.getMobCastRange(_actor) < 200)
						castingChance = 25;
					else
					{
						if (_mostHatedAnalysis.isMage) // hit mages
							castingChance = 15;
						else
							castingChance = 20;
					}
				}
				else if (_selfAnalysis.isDagger)
				{
					if (sk.getMobCastRange(_actor) < 200)
						castingChance = 95;
					else
					{
						if (_mostHatedAnalysis.isMage) // hit mages
							castingChance = 60;
						else
							castingChance = 50;
					}
				}
				else if (_selfAnalysis.isFighter)
				{
					if (sk.getMobCastRange(_actor) < 200)
						castingChance = 12;
					else
					{
						if (_mostHatedAnalysis.isMage)
							castingChance = 1;
						else
							castingChance = 2;
					}
				}
				
				int realCastingChance = castingChance;
				
				if (_selfAnalysis.isMage || _selfAnalysis.isHealer)
				{
					if (!sk.isMagic())
						realCastingChance /= 1.8;
				}
				else
				{
					if (sk.isMagic())
						realCastingChance /= 4.2;
				}
				
				if (Rnd.nextInt(100) <= realCastingChance)
				{
					clientStopMoving(null);
					_accessor.doCast(sk);
					return;
				}
			}
		}
		
		if (_actor.isAttackingDisabled())
			return;
		
		// Finally, physical attacks
		if (!_selfAnalysis.isHealer)
		{
			_accessor.doAttack(_mostHatedAnalysis.character);
			
			if (_selfAnalysis.minNumberOfContAttacks > 0)
			{
				if (_selfAnalysis.currNumberOfAttacks + 1 >= _selfAnalysis.minNumberOfContAttacks)
					_selfAnalysis.currNumberOfAttacks = 0;
				else
					_selfAnalysis.currNumberOfAttacks++;
			}
		}
		else
		{
			// healers do not even follow
			if (_actor.getRunSpeed() < 2)
				return;
			
			// Minions following leader
			if (_actor instanceof L2MinionInstance && ((L2MinionInstance) _actor).getLeader() != null)
			{
				int offset = 600;
				
				if (((L2MinionInstance) _actor).getLeader().isRunning())
					_actor.setRunning();
				else
					_actor.setWalking();
				
				if (_actor.getPlanDistanceSq(((L2MinionInstance) _actor).getLeader()) > offset * offset)
				{
					int x1, y1, z1;
					x1 = ((L2MinionInstance) _actor).getLeader().getX() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
					y1 = ((L2MinionInstance) _actor).getLeader().getY() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
					z1 = ((L2MinionInstance) _actor).getLeader().getZ();
					// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
					moveTo(x1, y1, z1);
					return;
				}
				else if (Rnd.nextInt(RANDOM_WALK_RATE) == 0)
				{
					// self and clan buffs
					for (L2Skill sk : _selfAnalysis.buffSkills)
					{
						if (_actor.getFirstEffect(sk.getId()) == null)
						{
							// if clan buffs, don't buff every time
							if (sk.getTargetType(_actor) != L2Skill.SkillTargetType.TARGET_SELF && sk.getTargetType(_actor) != L2Skill.SkillTargetType.TARGET_SELF_AND_PET && Rnd.nextInt(2) != 0)
								continue;
							if (!_actor.checkDoCastConditionsNPC(sk, _actor))
								continue;
							L2Object OldTarget = _actor.getTarget();
							_actor.setTarget(_actor);
							clientStopMoving(null);
							_accessor.doCast(sk);
							_actor.setTarget(OldTarget);
							return;
						}
					}
				}
			}
		}
	}
}

/**
 * Manage AI thinking actions of a L2Attackable.<BR><BR>
 */
@Override
protected void onEvtThink()
{
	// Check if the thinking action is already in progress
	if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
		return;
	
	// Start thinking action
	_thinking = true;
	
	try
	{
		// Manage AI thinks of a L2Attackable
		if (getIntention() == AI_INTENTION_ACTIVE)
			thinkActive();
		else if (getIntention() == AI_INTENTION_ATTACK)
			thinkAttack();
	}
	finally
	{
		// Stop thinking action
		_thinking = false;
	}
}

/**
 * Launch actions corresponding to the Event Attacked.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Init the attack : Calculate the attack timeout, Set the _globalAggro to 0, Add the attacker to the actor _aggroList</li>
 * <li>Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance</li>
 * <li>Set the Intention to AI_INTENTION_ATTACK</li><BR><BR>
 *
 * @param attacker The L2Character that attacks the actor
 *
 */
@SuppressWarnings("incomplete-switch")
@Override
protected void onEvtAttacked(L2Character attacker)
{
	if (!_actor.getKnownList().knowsObject(attacker))
		_actor.getKnownList().addKnownObject(attacker, true);
	
	// Calculate the attack timeout
	_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
	
	// Set the _globalAggro to 0 to permit attack even just after spawn
	if (_globalAggro < 0)
		_globalAggro = 0;
	
	// Add the attacker to the _aggroList of the actor
	if (_actor.hasAI() && !((L2Attackable) _actor).isCoreAIDisabled())
		((L2Attackable) _actor).addDamageHate(attacker, 1);
	
	if (_actor instanceof L2MonsterInstance)
	{
		L2MonsterInstance master = (L2MonsterInstance)_actor;
		
		if (_actor instanceof L2MinionInstance)
		{
			master = ((L2MinionInstance)_actor).getLeader();
			
			if (master != null && !master.isDead() && !master.isInCombatAI())
			{
				try
				{
					if (((L2AttackableAI)master.getAI())._globalAggro < 0)
						((L2AttackableAI)master.getAI())._globalAggro = 0;
				}
				catch (Exception e)
				{
				}
				master.addDamageHate(attacker, 1);
				master.callMinionsToAssist(attacker, 1, (L2Attackable) _actor);
			}
		}
		else if (master.hasMinions())
			master.callMinionsToAssist(attacker, 1, null);
	}
	
	if (_actor instanceof L2Attackable)
		notifyFactionAttacked(attacker);
	
	// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
	if (!_actor.isRunning())
		_actor.setRunning();
	
	if (((L2Attackable)_actor).isCoreAIDisabled())
	{
		if (getIntention() != AI_INTENTION_INTERACT)
			setIntention(CtrlIntention.AI_INTENTION_INTERACT, attacker);
	}
	else
	{
		switch (getIntention())
		{
		case AI_INTENTION_MOVE_TO:
			if (_kited || _actor.isOutOfControl())
				break;
		case AI_INTENTION_ACTIVE:
		case AI_INTENTION_IDLE:
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
			break;
		}
	}
	
	super.onEvtAttacked(attacker);
}

private void notifyFactionAttacked(L2Character attacker)
{
	if (attacker == null) return;
	final L2Npc me = (L2Npc) _actor;
	final String faction_id = me.getFactionIdString();
	
	if (!faction_id.equalsIgnoreCase("NONE"))
	{
		for (L2Character obj : _actor.getKnownList().getKnownCharacters())
		{
			if (obj != null && obj instanceof L2Attackable && !obj.isAlikeDead() && !(obj instanceof L2MinionInstance))
			{
				L2Npc npc = (L2Npc)obj;
				
				//Handle SevenSigns mob Factions
				final String npcfaction = npc.getFactionId();
				
				boolean sevenSignFaction = false;
				
				//Catacomb mobs should assist lilim and nephilim other than dungeon
				if ("c_dungeon_clan".equals(faction_id) &&
						("c_dungeon_lilim".equals(npcfaction) || "c_dungeon_nephi".equals(npcfaction)))
					sevenSignFaction = true;
				//Lilim mobs should assist other Lilim and catacomb mobs
				else if ("c_dungeon_lilim".equals(faction_id) &&
						"c_dungeon_clan".equals(npcfaction))
					sevenSignFaction = true;
				//Nephilim mobs should assist other Nephilim and catacomb mobs
				else if ("c_dungeon_nephi".equals(faction_id) &&
						"c_dungeon_clan".equals(npcfaction))
					sevenSignFaction = true;
				
				if (!sevenSignFaction && !faction_id.equalsIgnoreCase(npcfaction))
					continue;
				
				// Check if the L2Object is inside the Faction Range of the actor
				if (npc.hasAI() && npc.getAI() != null && _actor.isInsideRadius(npc, (int) (npc.getFactionRange() + npc.getCollisionRadius()), true, false))
				{
					if (Math.abs(attacker.getZ() - npc.getZ()) < 700 && (npc.getAI()._intention == CtrlIntention.AI_INTENTION_IDLE || npc.getAI()._intention == CtrlIntention.AI_INTENTION_ACTIVE) && GeoData.getInstance().canSeeTarget(npc, _actor))
					{
						if ((attacker instanceof L2Playable) || (attacker instanceof L2Decoy))
						{
							if (npc.getTemplate().getEventQuests(QuestEventType.ON_FACTION_CALL) != null)
							{
								L2PcInstance player = (attacker instanceof L2PcInstance) ? (L2PcInstance) attacker : attacker.getActingPlayer();
								
								if (player != null)
								{
									if (!npc.getKnownList().knowsObject(player))
										npc.getKnownList().addKnownObject(player, true);
									
									try
									{
										if (((L2AttackableAI)npc.getAI())._globalAggro < 0)
											((L2AttackableAI)npc.getAI())._globalAggro = 0;
									}
									catch (Exception e)
									{
									}
									
									for (Quest quest : npc.getTemplate().getEventQuests(QuestEventType.ON_FACTION_CALL))
										quest.notifyFactionCall(npc, me, player, (attacker instanceof L2Summon));
								}
							}
						}
					}
					
					/*// heal or resurrect friends
					if (_selfAnalysis.hasHealOrResurrect && !_actor.isAttackingDisabled() && npc.getCurrentHp() < npc.getMaxHp() * 0.8 && _actor.getCurrentHp() > _actor.getMaxHp() / 2)
					{
						if (GeoData.getInstance().canSeeTarget(_actor, npc))
						{
							if (npc.isDead() && _actor instanceof L2MinionInstance)
							{
								if (((L2MinionInstance) _actor).getLeader() == npc)
								{
									for (L2Skill sk : _selfAnalysis.resurrectSkills)
									{
										if (18 >= Rnd.get(100)) // chance
											continue;
										if (!Util.checkIfInRange(sk.getMobCastRange(_actor), _actor, npc, true))
											continue;
										if (!_actor.checkDoCastConditionsNPC(sk, _actor.getTarget()))
											continue;
										
										L2Object OldTarget = _actor.getTarget();
										_actor.setTarget(npc);
										// would this ever be fast enough											// for the decay not to run?											// giving some extra seconds
										DecayTaskManager.getInstance().cancelDecayTask(npc);
										DecayTaskManager.getInstance().addDecayTask(npc);
										clientStopMoving(null);
										_accessor.doCast(sk);
										_actor.setTarget(OldTarget);
										return;
									}
								}
							}
							else if (npc.isInCombat())
							{
								for (L2Skill sk : _selfAnalysis.healSkills)
								{
									int chance = 13;
									
									if (_actor instanceof L2MinionInstance)
									{
										// minions support boss
										if (((L2MinionInstance) _actor).getLeader() == npc)
											chance = 20;
										else
											chance = 10;
									}
									
										if (npc instanceof L2GrandBossInstance)
										chance = 6;
									
									if (_selfAnalysis.isHealer)
										chance += 60;
									
									if (chance < Rnd.get(100)) // chance
										continue;
									if (!Util.checkIfInRange(sk.getMobCastRange(_actor), _actor, npc, true))
										continue;
									if (!_actor.checkDoCastConditionsNPC(sk, npc))
										continue;
									
									L2Object OldTarget = _actor.getTarget();
									_actor.setTarget(npc);
									clientStopMoving(null);
									_accessor.doCast(sk);
									_actor.setTarget(OldTarget);
									return;
								}
							}
						}
					}*/
				}
			}
		}
	}
}

/**
 * Launch actions corresponding to the Event Aggression.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Add the target to the actor _aggroList or update hate if already present </li>
 * <li>Set the actor Intention to AI_INTENTION_ATTACK (if actor is L2GuardInstance check if it isn't too far from its home location)</li><BR><BR>
 *
 * @param attacker The L2Character that attacks
 * @param aggro The value of hate to add to the actor against the target
 *
 */
@SuppressWarnings("incomplete-switch")
@Override
protected void onEvtAggression(L2Character target, int aggro)
{
	// Calculate the attack timeout
	_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
	
	L2Attackable me = (L2Attackable) _actor;
	
	if (target != null)
	{
		// Add the target to the actor _aggroList or update hate if already present
		me.addDamageHate(target, aggro);
		
		if (_actor instanceof L2MonsterInstance)
		{
			L2MonsterInstance master = (L2MonsterInstance)_actor;
			
			if (_actor instanceof L2MinionInstance)
			{
				master = ((L2MinionInstance)_actor).getLeader();
				
				if (master != null && !master.isDead() && !master.isInCombatAI())
				{
					try
					{
						if (((L2AttackableAI)master.getAI())._globalAggro < 0)
							((L2AttackableAI)master.getAI())._globalAggro = 0;
					}
					catch (Exception e)
					{
					}
					master.addDamageHate(target, 1);
					master.callMinionsToAssist(target, 1, (L2Attackable) _actor);
				}
			}
			else if (master.hasMinions())
				master.callMinionsToAssist(target, 1, null);
		}
		
		// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
		if (!_actor.isRunning())
			_actor.setRunning();
		
		if (((L2Attackable)_actor).isCoreAIDisabled())
		{
			if (getIntention() != AI_INTENTION_INTERACT)
				setIntention(CtrlIntention.AI_INTENTION_INTERACT, target);
		}
		else
		{
			switch (getIntention())
			{
			case AI_INTENTION_MOVE_TO:
				if (_kited || _actor.isOutOfControl())
					break;
			case AI_INTENTION_ACTIVE:
			case AI_INTENTION_IDLE:
				setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
				break;
			}
		}
	}
}

@Override
protected void onIntentionActive()
{
	// Cancel attack timeout
	_attackTimeout = Integer.MAX_VALUE;
	_selfAnalysis.currNumberOfAttacks = 0;
	super.onIntentionActive();
}

public void setGlobalAggro(int value)
{
	_globalAggro = value;
}
}