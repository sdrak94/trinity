package net.sf.l2j.gameserver.ai;

import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_CAST;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_MOVE_TO;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_REST;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Universe;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2ItemInstance.ItemLocation;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2AirShipInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2BoatInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStop;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.util.Point3D;
import net.sf.l2j.util.Rnd;

/**
 * This class manages AI of L2Character.<BR><BR>
 *
 * L2CharacterAI :<BR><BR>
 * <li>L2AttackableAI</li>
 * <li>L2DoorAI</li>
 * <li>L2PlayerAI</li>
 * <li>L2SummonAI</li><BR><BR>
 *
 */
public class L2CharacterAI extends AbstractAI
{
public class IntentionCommand
{
protected final CtrlIntention _crtlIntention;
protected final Object _arg0, _arg1;

protected IntentionCommand(CtrlIntention pIntention, Object pArg0, Object pArg1)
{
	_crtlIntention = pIntention;
	_arg0 = pArg0;
	_arg1 = pArg1;
}

public CtrlIntention getIntention()
{
	return _crtlIntention;
}
}

/**
 * Constructor of L2CharacterAI.<BR><BR>
 *
 * @param accessor The AI accessor of the L2Character
 *
 */
public L2CharacterAI(L2Character.AIAccessor accessor)
{
	super(accessor);
}

public IntentionCommand getNextIntention()
{
	return null;
}

@Override
protected void onEvtAttacked(L2Character attacker)
{
	if (attacker instanceof L2Attackable && !((L2Attackable) attacker).isCoreAIDisabled())
		clientStartAutoAttack();
}

/**
 * Manage the Idle Intention : Stop Attack, Movement and Stand Up the actor.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Set the AI Intention to AI_INTENTION_IDLE </li>
 * <li>Init cast and attack target </li>
 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast) </li>
 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast) </li>
 * <li>Stand up the actor server side AND client side by sending Server->Client packet ChangeWaitType (broadcast) </li><BR><BR>
 *
 */
@Override
protected void onIntentionIdle()
{
	// Set the AI Intention to AI_INTENTION_IDLE
	changeIntention(AI_INTENTION_IDLE, null, null);
	
	// Init cast and attack target
	setCastTarget(null);
	setAttackTarget(null);
	
	// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
	clientStopMoving(null);
	
	// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
	clientStopAutoAttack();
	
}

/**
 * Manage the Active Intention : Stop Attack, Movement and Launch Think Event.<BR><BR>
 *
 * <B><U> Actions</U> : <I>if the Intention is not already Active</I></B><BR><BR>
 * <li>Set the AI Intention to AI_INTENTION_ACTIVE </li>
 * <li>Init cast and attack target </li>
 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast) </li>
 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast) </li>
 * <li>Launch the Think Event </li><BR><BR>
 *
 */
@Override
protected void onIntentionActive()
{
	// Check if the Intention is not already Active
	if (getIntention() != AI_INTENTION_ACTIVE)
	{
		// Set the AI Intention to AI_INTENTION_ACTIVE
		changeIntention(AI_INTENTION_ACTIVE, null, null);
		
		// Init cast and attack target
		setCastTarget(null);
		setAttackTarget(null);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		// Also enable random animations for this L2Character if allowed
		// This is only for mobs - town npcs are handled in their constructor
		if (_actor instanceof L2Attackable)
			((L2Npc) _actor).startRandomAnimationTimer();
		
		// Launch the Think Event
		onEvtThink();
	}
}

/**
 * Manage the Rest Intention.<BR><BR>
 *
 * <B><U> Actions</U> : </B><BR><BR>
 * <li>Set the AI Intention to AI_INTENTION_IDLE </li><BR><BR>
 *
 */
@Override
protected void onIntentionRest()
{
	// Set the AI Intention to AI_INTENTION_IDLE
	setIntention(AI_INTENTION_IDLE);
}

/**
 * Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Event.<BR><BR>
 *
 * <B><U> Actions</U> : </B><BR><BR>
 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast) </li>
 * <li>Set the Intention of this AI to AI_INTENTION_ATTACK </li>
 * <li>Set or change the AI attack target </li>
 * <li>Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart (broadcast) </li>
 * <li>Launch the Think Event </li><BR><BR>
 *
 *
 * <B><U> Overridden in</U> :</B><BR><BR>
 * <li>L2AttackableAI : Calculate attack timeout</li><BR><BR>
 *
 */
@Override
protected void onIntentionAttack(L2Character target)
{
	if (target == null)
	{
		clientActionFailed();
		return;
	}
	
	if (_actor instanceof L2PcInstance)
	{
		((L2PlayerAI)this).clearNextIntention();
		_actor.getActingPlayer().onActionRequest();
		
		try {
			final L2Summon pet = _actor.getPet();
			boolean canAttack = true;
			
			if (pet != null && pet != target && !pet.isAttackingDisabled() && !pet.isOutOfControl() && pet.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
			{
				if (target.isInsidePeaceZone((L2PcInstance)_actor))
				{
					if (!_actor.isInFunEvent() || !target.isInFunEvent())
						canAttack = false;
				}
				
				if (pet.getNpcId() == 12564 || pet.getNpcId() == 12621)
				{
					// sin eater and wyvern can't attack with attack button
					canAttack = false;
				}
				
				if (target.isAttackable() && canAttack)
				{
					if (target instanceof L2DoorInstance)
					{
						if (((L2DoorInstance) target).isAttackable() && pet.getNpcId() != L2SiegeSummonInstance.SWOOP_CANNON_ID)
						{
							pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
						}
					}			// siege golem AI doesn't support attacking other than doors at the moment
					else if (pet.getNpcId() != L2SiegeSummonInstance.SIEGE_GOLEM_ID)
					{
						pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	if (getIntention() == AI_INTENTION_REST)
	{
		clientActionFailed();
		return;
	}
	
	if (_actor.isAllSkillsDisabled() || _actor.isAfraid())
	{
		clientActionFailed();
		return;
	}
	
	if (_actor.isCastingNow())
	{
		clientActionFailed();
		
		if (_actor instanceof L2PcInstance)
		{
			if (getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
				((L2PlayerAI)this).saveNextIntention(CtrlIntention.AI_INTENTION_ATTACK, target, null);
		}
		
		return;
	}
	
	// Check if the Intention is already AI_INTENTION_ATTACK
	if (getIntention() == AI_INTENTION_ATTACK)
	{
		// Check if the AI already targets the L2Character
		if (getAttackTarget() != target)
		{
			// Set the AI attack target (change target)
			setAttackTarget(target);
			
			stopFollow();
			
			// Launch the Think Event
			notifyEvent(CtrlEvent.EVT_THINK, null);
		}
		else
			clientActionFailed(); // else client freezes until cancel target
	}
	else
	{
		// Set the Intention of this AbstractAI to AI_INTENTION_ATTACK
		changeIntention(AI_INTENTION_ATTACK, target, null);
		
		// Set the AI attack target
		setAttackTarget(target);
		
		stopFollow();
		
		// Launch the Think Event
		notifyEvent(CtrlEvent.EVT_THINK, null);
	}
}

/**
 * Manage the Cast Intention : Stop current Attack, Init the AI in order to cast and Launch Think Event.<BR><BR>
 *
 * <B><U> Actions</U> : </B><BR><BR>
 * <li>Set the AI cast target </li>
 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast) </li>
 * <li>Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor </li>
 * <li>Set the AI skill used by INTENTION_CAST </li>
 * <li>Set the Intention of this AI to AI_INTENTION_CAST </li>
 * <li>Launch the Think Event </li><BR><BR>
 *
 */
@Override
protected void onIntentionCast(L2Skill skill, L2Object target)
{
	if (getIntention() == AI_INTENTION_REST && skill.isMagic())
	{
		clientActionFailed();
		_actor.setIsCastingNow(false);
		return;
	}
	
	// Set the AI cast target
	setCastTarget((L2Character) target);
	
	// Stop actions client-side to cast the skill
	if (skill.getHitTime() > 50)
		_actor.abortAttack();
	
	// Set the AI skill used by INTENTION_CAST
	_skill = skill;
	
	// Change the Intention of this AbstractAI to AI_INTENTION_CAST
	changeIntention(AI_INTENTION_CAST, skill, target);
	
	// Launch the Think Event
	notifyEvent(CtrlEvent.EVT_THINK, null);
}

/**
 * Manage the Move To Intention : Stop current Attack and Launch a Move to Location Task.<BR><BR>
 *
 * <B><U> Actions</U> : </B><BR><BR>
 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast) </li>
 * <li>Set the Intention of this AI to AI_INTENTION_MOVE_TO </li>
 * <li>Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast) </li><BR><BR>
 *
 */
@Override
protected void onIntentionMoveTo(L2CharPosition pos)
{
	if (getIntention() == AI_INTENTION_REST)
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isMovementDisabled())
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	// Abort the attack of the L2Character and send Server->Client ActionFailed packet
	_actor.abortAttack();
	
	// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
	clientStopAutoAttack();
	
	// Set the Intention of this AbstractAI to AI_INTENTION_MOVE_TO
	changeIntention(AI_INTENTION_MOVE_TO, pos, null);
	
	// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
	moveTo(pos.x, pos.y, pos.z);
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.ai.AbstractAI#onIntentionMoveToInABoat(net.sf.l2j.gameserver.model.L2CharPosition, net.sf.l2j.gameserver.model.L2CharPosition)
 */
@Override
protected void onIntentionMoveToInABoat(L2CharPosition destination, L2CharPosition origin)
{
	if (getIntention() == AI_INTENTION_REST)
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	// Set the Intention of this AbstractAI to AI_INTENTION_MOVE_TO
	//
	//changeIntention(AI_INTENTION_MOVE_TO, new L2CharPosition(((L2PcInstance)_actor).getBoat().getX() - destination.x, ((L2PcInstance)_actor).getBoat().getY() - destination.y, ((L2PcInstance)_actor).getBoat().getZ() - destination.z, 0)  , null);
	
	// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
	clientStopAutoAttack();
	
	// Abort the attack of the L2Character and send Server->Client ActionFailed packet
	_actor.abortAttack();
	
	// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
	moveToInABoat(destination, origin);
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.ai.AbstractAI#onIntentionMoveToInAirShip(net.sf.l2j.gameserver.model.L2CharPosition, net.sf.l2j.gameserver.model.L2CharPosition)
 */
@Override
protected void onIntentionMoveToInAirShip(L2CharPosition destination, L2CharPosition origin)
{
	if (getIntention() == AI_INTENTION_REST)
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	// Set the Intention of this AbstractAI to AI_INTENTION_MOVE_TO
	//
	//changeIntention(AI_INTENTION_MOVE_TO, new L2CharPosition(((L2PcInstance)_actor).getBoat().getX() - destination.x, ((L2PcInstance)_actor).getBoat().getY() - destination.y, ((L2PcInstance)_actor).getBoat().getZ() - destination.z, 0)  , null);
	
	// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
	clientStopAutoAttack();
	
	// Abort the attack of the L2Character and send Server->Client ActionFailed packet
	_actor.abortAttack();
	
	// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
	moveToInAirShip(destination, origin);
}

/**
 * Manage the Follow Intention : Stop current Attack and Launch a Follow Task.<BR><BR>
 *
 * <B><U> Actions</U> : </B><BR><BR>
 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast) </li>
 * <li>Set the Intention of this AI to AI_INTENTION_FOLLOW </li>
 * <li>Create and Launch an AI Follow Task to execute every 1s </li><BR><BR>
 *
 */
@Override
protected void onIntentionFollow(L2Character target)
{
	if (getIntention() == AI_INTENTION_REST)
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	if (_actor.isImmobilized() || _actor.isRooted())
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	// Dead actors can`t follow
	if (_actor.isDead())
	{
		clientActionFailed();
		return;
	}
	
	// do not follow yourself
	if (_actor == target)
	{
		clientActionFailed();
		return;
	}
	
	// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
	clientStopAutoAttack();
	
	// Set the Intention of this AbstractAI to AI_INTENTION_FOLLOW
	changeIntention(AI_INTENTION_FOLLOW, target, null);
	
	// Create and Launch an AI Follow Task to execute every 1s
	startFollow(target);
}

/**
 * Manage the PickUp Intention : Set the pick up target and Launch a Move To Pawn Task (offset=20).<BR><BR>
 *
 * <B><U> Actions</U> : </B><BR><BR>
 * <li>Set the AI pick up target </li>
 * <li>Set the Intention of this AI to AI_INTENTION_PICK_UP </li>
 * <li>Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast) </li><BR><BR>
 *
 */
@Override
protected void onIntentionPickUp(L2Object object)
{
	if (getIntention() == AI_INTENTION_REST)
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
	clientStopAutoAttack();
	
	if (object instanceof L2ItemInstance && ((L2ItemInstance)object).getItemLocation() != ItemLocation.VOID)
		return;
	
	// Set the Intention of this AbstractAI to AI_INTENTION_PICK_UP
	changeIntention(AI_INTENTION_PICK_UP, object, null);
	
	// Set the AI pick up target
	setTarget(object);
	if (object.getX() == 0 && object.getY() == 0) // TODO: Find the drop&spawn bug
	{
		_log.warning("Object in coords 0,0 - using a temporary fix");
		object.setXYZ(getActor().getX(), getActor().getY(), getActor().getZ() + 5);
	}
	
	// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
	moveToPawn(object, 20);
}

/**
 * Manage the Interact Intention : Set the interact target and Launch a Move To Pawn Task (offset=60).<BR><BR>
 *
 * <B><U> Actions</U> : </B><BR><BR>
 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast) </li>
 * <li>Set the AI interact target </li>
 * <li>Set the Intention of this AI to AI_INTENTION_INTERACT </li>
 * <li>Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast) </li><BR><BR>
 *
 */
@Override
protected void onIntentionInteract(L2Object object)
{
	if (getIntention() == AI_INTENTION_REST)
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
	{
		// Cancel action client side by sending Server->Client packet ActionFailed to the L2PcInstance actor
		clientActionFailed();
		return;
	}
	
	// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
	clientStopAutoAttack();
	
	if (getIntention() != AI_INTENTION_INTERACT)
	{
		// Set the Intention of this AbstractAI to AI_INTENTION_INTERACT
		changeIntention(AI_INTENTION_INTERACT, object, null);
		
		// Set the AI interact target
		setTarget(object);
		
		// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
		moveToPawn(object, 60);
	}
}

/**
 * Do nothing.<BR><BR>
 */
@Override
protected void onEvtThink()
{
	// do nothing
}

/**
 * Do nothing.<BR><BR>
 */
@Override
protected void onEvtAggression(L2Character target, int aggro)
{
	// do nothing
}

/**
 * Launch actions corresponding to the Event Stunned then onAttacked Event.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
 * <li>Break an attack and send Server->Client ActionFailed packet and a System Message to the L2Character </li>
 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character </li>
 * <li>Launch actions corresponding to the Event onAttacked (only for L2AttackableAI after the stunning periode) </li><BR><BR>
 *
 */
@Override
protected void onEvtStunned(L2Character attacker)
{
	// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
	_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
	if (AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
	
	// Stop Server AutoAttack also
	setAutoAttacking(false);
	
	// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
	clientStopMoving(null);
	
	// Launch actions corresponding to the Event onAttacked (only for L2AttackableAI after the stunning periode)
	onEvtAttacked(attacker);
}

@Override
protected void onEvtParalyzed(L2Character attacker)
{
	// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
	_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
	if (AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
	
	// Stop Server AutoAttack also
	setAutoAttacking(false);
	
	// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
	clientStopMoving(null);
	
	// Launch actions corresponding to the Event onAttacked (only for L2AttackableAI after the stunning periode)
	onEvtAttacked(attacker);
}

/**
 * Launch actions corresponding to the Event Sleeping.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
 * <li>Break an attack and send Server->Client ActionFailed packet and a System Message to the L2Character </li>
 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character </li><BR><BR>
 *
 */
@Override
protected void onEvtSleeping(L2Character attacker)
{
	// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
	_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
	if (AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
	
	// stop Server AutoAttack also
	setAutoAttacking(false);
	
	// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
	clientStopMoving(null);
}

/**
 * Launch actions corresponding to the Event Rooted.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
 * <li>Launch actions corresponding to the Event onAttacked</li><BR><BR>
 *
 */
@Override
protected void onEvtRooted(L2Character attacker)
{
	// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
	//_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
	//if (AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
	//    AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
	
	// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
	clientStopMoving(null);
	
	// Launch actions corresponding to the Event onAttacked
	onEvtAttacked(attacker);
	
}

/**
 * Launch actions corresponding to the Event Confused.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
 * <li>Launch actions corresponding to the Event onAttacked</li><BR><BR>
 *
 */
@Override
protected void onEvtConfused(L2Character attacker)
{
	// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
	clientStopMoving(null);
	
	// Launch actions corresponding to the Event onAttacked
	onEvtAttacked(attacker);
}

/**
 * Launch actions corresponding to the Event Muted.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character </li><BR><BR>
 *
 */
@Override
protected void onEvtMuted(L2Character attacker)
{
	// Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character
	onEvtAttacked(attacker);
}

/**
 * Launch actions corresponding to the Event ReadyToAct.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Launch actions corresponding to the Event Think</li><BR><BR>
 *
 */
@Override
protected void onEvtReadyToAct()
{
	// Launch actions corresponding to the Event Think
	onEvtThink();
}

/**
 * Do nothing.<BR><BR>
 */
@Override
protected void onEvtUserCmd(Object arg0, Object arg1)
{
	// do nothing
}

/**
 * Launch actions corresponding to the Event Arrived.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>If the Intention was AI_INTENTION_MOVE_TO, set the Intention to AI_INTENTION_ACTIVE</li>
 * <li>Launch actions corresponding to the Event Think</li><BR><BR>
 *
 */
@Override
protected void onEvtArrived()
{
	// Launch an explore task if necessary
	if (Config.ACTIVATE_POSITION_RECORDER && _accessor.getActor() instanceof L2PcInstance)
	{
		((L2PcInstance) _accessor.getActor()).explore();
	}
	_accessor.getActor().revalidateZone(true);
	
	if (_accessor.getActor().moveToNextRoutePoint())
		return;
	
	if (_accessor.getActor() instanceof L2Attackable)
	{
		((L2Attackable) _accessor.getActor()).setisReturningToSpawnPoint(false);
	}
	clientStoppedMoving();
	
	// If the Intention was AI_INTENTION_MOVE_TO, set the Intention to AI_INTENTION_ACTIVE
	if (getIntention() == AI_INTENTION_MOVE_TO)
	{
		setIntention(AI_INTENTION_ACTIVE);
	}
	
	// Launch actions corresponding to the Event Think
	onEvtThink();
	
	if (_actor instanceof L2BoatInstance)
	{
		((L2BoatInstance) _actor).evtArrived();
	}
	else if (_actor instanceof L2AirShipInstance)
	{
		((L2AirShipInstance) _actor).evtArrived();
	}
}

/**
 * Launch actions corresponding to the Event ArrivedRevalidate.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Launch actions corresponding to the Event Think</li><BR><BR>
 *
 */
@Override
protected void onEvtArrivedRevalidate()
{
	// Launch actions corresponding to the Event Think
	onEvtThink();
}

/**
 * Launch actions corresponding to the Event ArrivedBlocked.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
 * <li>If the Intention was AI_INTENTION_MOVE_TO, set the Intention to AI_INTENTION_ACTIVE</li>
 * <li>Launch actions corresponding to the Event Think</li><BR><BR>
 *
 */
@Override
protected void onEvtArrivedBlocked(L2CharPosition blocked_at_pos)
{
	// If the Intention was AI_INTENTION_MOVE_TO, set the Intention to AI_INTENTION_ACTIVE
	if (getIntention() == AI_INTENTION_MOVE_TO || getIntention() == AI_INTENTION_CAST)
		setIntention(AI_INTENTION_ACTIVE);
	
	// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
	clientStopMoving(blocked_at_pos);
	
	if (Config.ACTIVATE_POSITION_RECORDER && Universe.getInstance().shouldLog(_accessor.getActor().getObjectId()))
	{
		if (!_accessor.getActor().isFlying())
			Universe.getInstance().registerObstacle(blocked_at_pos.x, blocked_at_pos.y, blocked_at_pos.z);
		if (_accessor.getActor() instanceof L2PcInstance)
			((L2PcInstance) _accessor.getActor()).explore();
	}
	
	// Launch actions corresponding to the Event Think
	onEvtThink();
}

/**
 * Launch actions corresponding to the Event ForgetObject.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>If the object was targeted  and the Intention was AI_INTENTION_INTERACT or AI_INTENTION_PICK_UP, set the Intention to AI_INTENTION_ACTIVE</li>
 * <li>If the object was targeted to attack, stop the auto-attack, cancel target and set the Intention to AI_INTENTION_ACTIVE</li>
 * <li>If the object was targeted to cast, cancel target and set the Intention to AI_INTENTION_ACTIVE</li>
 * <li>If the object was targeted to follow, stop the movement, cancel AI Follow Task and set the Intention to AI_INTENTION_ACTIVE</li>
 * <li>If the targeted object was the actor , cancel AI target, stop AI Follow Task, stop the movement and set the Intention to AI_INTENTION_IDLE </li><BR><BR>
 *
 */
@Override
protected void onEvtForgetObject(L2Object object)
{
	// If the object was targeted  and the Intention was AI_INTENTION_INTERACT or AI_INTENTION_PICK_UP, set the Intention to AI_INTENTION_ACTIVE
	if (getTarget() == object)
	{
		setTarget(null);
		
		if (getIntention() == AI_INTENTION_INTERACT)
			setIntention(AI_INTENTION_ACTIVE);
		else if (getIntention() == AI_INTENTION_PICK_UP)
			setIntention(AI_INTENTION_ACTIVE);
	}
	
	// Check if the object was targeted to attack
	if (getAttackTarget() == object)
	{
		// Cancel attack target
		setAttackTarget(null);
		
		// Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE
		setIntention(AI_INTENTION_ACTIVE);
	}
	
	// Check if the object was targeted to cast
	if (getCastTarget() == object)
	{
		// Cancel cast target
		setCastTarget(null);
		
		// Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE
		setIntention(AI_INTENTION_ACTIVE);
	}
	
	// Check if the object was targeted to follow
	if (getFollowTarget() == object)
	{
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Stop an AI Follow Task
		stopFollow();
		
		// Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE
		setIntention(AI_INTENTION_ACTIVE);
	}
	
	// Check if the targeted object was the actor
	if (_actor == object)
	{
		// Cancel AI target
		setTarget(null);
		setAttackTarget(null);
		setCastTarget(null);
		
		// Stop an AI Follow Task
		stopFollow();
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Set the Intention of this AbstractAI to AI_INTENTION_IDLE
		changeIntention(AI_INTENTION_IDLE, null, null);
	}
}

/**
 * Launch actions corresponding to the Event Cancel.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Stop an AI Follow Task</li>
 * <li>Launch actions corresponding to the Event Think</li><BR><BR>
 *
 */
@Override
protected void onEvtCancel()
{
	_actor.abortCast();
	
	// Stop an AI Follow Task
	stopFollow();
	
	if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
	
	// Launch actions corresponding to the Event Think
	onEvtThink();
}

/**
 * Launch actions corresponding to the Event Dead.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Stop an AI Follow Task</li>
 * <li>Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)</li><BR><BR>
 *
 */
@Override
protected void onEvtDead()
{
	// Stop an AI Follow Task
	stopFollow();
	
	// Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)
	clientNotifyDead();
	
	if (!(_actor instanceof L2PcInstance))
		_actor.setWalking();
}

/**
 * Launch actions corresponding to the Event Fake Death.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Stop an AI Follow Task</li>
 *
 */
@Override
protected void onEvtFakeDeath()
{
	// Stop an AI Follow Task
	stopFollow();
	
	// Stop the actor movement and send Server->Client packet StopMove/StopRotation (broadcast)
	clientStopMoving(null);
	
	// Init AI
	_intention = AI_INTENTION_IDLE;
	setTarget(null);
	setCastTarget(null);
	setAttackTarget(null);
}

/**
 * Do nothing.<BR><BR>
 */
@Override
protected void onEvtFinishCasting()
{
	// do nothing
}

protected boolean maybeMoveToPosition(Point3D worldPosition, int offset)
{
	if (worldPosition == null)
	{
		_log.warning("maybeMoveToPosition: worldPosition == NULL!");
		return false;
	}
	
	if (offset < 0)
		return false; // skill radius -1
	
	if (!_actor.isInsideRadius(worldPosition.getX(), worldPosition.getY(), offset + (int)_actor.getCollisionRadius(), false))
	{
		if (_actor.isMovementDisabled())
			return true;
		
		if (!_actor.isRunning() && !(this instanceof L2PlayerAI) && !(this instanceof L2SummonAI))
			_actor.setRunning();
		
		stopFollow();
		
		int x = _actor.getX();
		int y = _actor.getY();
		
		double dx = worldPosition.getX() - x;
		double dy = worldPosition.getY() - y;
		
		double dist = Math.sqrt(dx * dx + dy * dy);
		
		double sin = dy / dist;
		double cos = dx / dist;
		
		dist -= offset - 5;
		
		x += (int) (dist * cos);
		y += (int) (dist * sin);
		
		moveTo(x, y, worldPosition.getZ());
		return true;
	}
	
	if (getFollowTarget() != null)
		stopFollow();
	
	return false;
}

/**
 * Manage the Move to Pawn action in function of the distance and of the Interact area.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Get the distance between the current position of the L2Character and the target (x,y)</li>
 * <li>If the distance > offset+20, move the actor (by running) to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
 * <li>If the distance <= offset+20, Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li><BR><BR>
 *
 * <B><U> Example of use </U> :</B><BR><BR>
 * <li> L2PLayerAI, L2SummonAI</li><BR><BR>
 *
 * @param target The targeted L2Object
 * @param offset The Interact area radius
 *
 * @return True if a movement must be done
 *
 */
protected boolean maybeMoveToPawn(L2Object target, int offset)
{
	// Get the distance between the current position of the L2Character and the target (x,y)
	if (target == null || target == _actor)
	{
		return false;
	}
	
	if (offset < 0)
		return false; // skill radius -1
	
	offset += _actor.getCollisionRadius();
	if (target instanceof L2Character)
		offset += ((L2Character) target).getCollisionRadius();
	
	offset -= 7;
	
	/*boolean checkZ = false;
	
	if (Util.calculateDistance(_actor, target, false) <= offset)
	{
		final int zDifferential = _actor.getZ() - target.getZ();
		
		if (zDifferential > 35)
		{
			if (zDifferential > offset * 1.75)
			{
				_actor.abortAttack();
				_actor.abortCast();
				setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				if (_actor instanceof L2PcInstance)
					((L2PcInstance)_actor).sendPacket(new SystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED));
				
				return true;
			}
			checkZ = false;
		}
		else if (zDifferential < 35)
		{
			if (zDifferential * -1 > offset)
			{
				_actor.abortAttack();
				_actor.abortCast();
				setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				if (_actor instanceof L2PcInstance)
					((L2PcInstance)_actor).sendPacket(new SystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED));
				
				return true;
			}
			checkZ = true;
		}
		else
		{
			checkZ = false;
		}
	}*/
	
	if (!_actor.isInsideRadius(target, offset, false, false))
	{
		final L2Character followTarget = getFollowTarget();
		
		if (followTarget != null && followTarget == target)
		{
			if (followTarget.isMoving())
			{
				if (_actor.isBehind(followTarget, 120))
				{
					// allow larger hit range when the target is moving (check is run only once per second)
					if (_actor.isInsideRadius(target, offset + 150, false, false))
					{
						stopFollow();
						return false;
					}
				}
			}
			
			/*if (_actor.isInsideRadius(target, offset + 145, false, false))
        		{
                	stopFollow();
                    return false;
                }*/
			
			return true;
		}
		
		if (_actor.isMovementDisabled())
			return true;
		
		// while flying there is no move to cast
		if (_actor.getAI().getIntention() == CtrlIntention.AI_INTENTION_CAST && _actor instanceof L2PcInstance && ((L2PcInstance)_actor).isTransformed())
		{
			if (!((L2PcInstance)_actor).getTransformation().canStartFollowToCast())
			{
				((L2PcInstance)_actor).sendPacket(new SystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED));
				((L2PcInstance)_actor).sendPacket(ActionFailed.STATIC_PACKET);
				
				return true;
			}
		}
		
		// If not running, set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
		if (!_actor.isRunning() && !(this instanceof L2PlayerAI) && !(this instanceof L2SummonAI))
			_actor.setRunning();
		
		stopFollow();
		
		if ((target instanceof L2Character) && !(target instanceof L2DoorInstance))
		{
			if (((L2Character)target).isMoving())
			{
				if (_actor.isBehind(target, 120))
					offset -= 100;
				else
					offset -= 50;
			}
			
			if (offset < 5)
				offset = 5;
			
			startFollow((L2Character)target, offset);
		}
		else
		{
			// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
			moveToPawn(target, offset);
		}
		return true;
	}
	
	if (getFollowTarget() != null)
		stopFollow();
	
	// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
	// clientStopMoving(null);
	return false;
}

/**
 * Modify current Intention and actions if the target is lost or dead.<BR><BR>
 *
 * <B><U> Actions</U> : <I>If the target is lost or dead</I></B><BR><BR>
 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
 * <li>Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE</li><BR><BR>
 *
 * <B><U> Example of use </U> :</B><BR><BR>
 * <li> L2PLayerAI, L2SummonAI</li><BR><BR>
 *
 * @param target The targeted L2Object
 *
 * @return True if the target is lost or dead (false if fakedeath)
 *
 */
protected boolean checkTargetLostOrDead(L2Character target)
{
	if (target == null || target.isDead())
	{
		// Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE
		setIntention(AI_INTENTION_ACTIVE);
		return true;
	}
	
	return false;
}

/**
 * Modify current Intention and actions if the target is lost.<BR><BR>
 *
 * <B><U> Actions</U> : <I>If the target is lost</I></B><BR><BR>
 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
 * <li>Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE</li><BR><BR>
 *
 * <B><U> Example of use </U> :</B><BR><BR>
 * <li> L2PLayerAI, L2SummonAI</li><BR><BR>
 *
 * @param target The targeted L2Object
 *
 * @return True if the target is lost
 *
 */
protected boolean checkTargetLost(L2Object target)
{
	if (target == null)
	{
		// Set the Intention of this AbstractAI to AI_INTENTION_ACTIVE
		setIntention(AI_INTENTION_ACTIVE);
		return true;
	}
	
	// check if player is fakedeath
	if (target instanceof L2PcInstance)
	{
		L2PcInstance target2 = (L2PcInstance) target; //convert object to chara
		
		if (target2.isFakeDeath())
		{
			target2.stopFakeDeath(null);
		}
	}
	
	return false;
}

protected class SelfAnalysis
{
public boolean isMage = false;
public boolean isBalanced;
public boolean isArcher = false;
public boolean isDagger = false;
public boolean isHealer = false;
public boolean hasSummonDisableSkills = false;
public boolean shouldAttackFromBehind = false;
public boolean isFighter = false;
public boolean cannotMoveOnLand = false;
public List<L2Skill> generalSkills = new FastList<L2Skill>();
public List<L2Skill> buffSkills = new FastList<L2Skill>();
public int lastBuffTick = 0;
public List<L2Skill> debuffSkills = new FastList<L2Skill>();
public int lastDebuffTick = 0;
public List<L2Skill> cancelSkills = new FastList<L2Skill>();
public List<L2Skill> healSkills = new FastList<L2Skill>();
//public List<L2Skill> trickSkills = new FastList<L2Skill>();
public List<L2Skill> generalDisablers = new FastList<L2Skill>();
public List<L2Skill> summonDisablers = new FastList<L2Skill>();
public List<L2Skill> sleepSkills = new FastList<L2Skill>();
public List<L2Skill> rootSkills = new FastList<L2Skill>();
public List<L2Skill> muteSkills = new FastList<L2Skill>();
public List<L2Skill> resurrectSkills = new FastList<L2Skill>();
public List<L2Skill> teleportRushSkills = new FastList<L2Skill>();
public boolean hasHealOrResurrect = false;
public boolean hasLongRangeSkills = false;
public boolean hasLongRangeDamageSkills = false;
public boolean hasTeleportRushSkills = false;
public int maxCastRange = 0;
public int minNumberOfContAttacks = 0;
public int currNumberOfAttacks = 0;

public SelfAnalysis()
{
}

public void init()
{
	switch (((L2NpcTemplate) _actor.getTemplate()).AI)
	{
	case FIGHTER:
		isFighter = true;
		minNumberOfContAttacks = Config.APC_ATTACK_ROW;
		break;
	case MAGE:
		isMage = true;
		break;
	case DAGGER:
		isDagger = true;
		break;
	case CORPSE:
	case BALANCED:
		isBalanced = true;
		minNumberOfContAttacks = Config.APC_ATTACK_ROW_BALANCED;
		break;
	case ARCHER:
		isArcher = true;
		break;
	case HEALER:
		isHealer = true;
		break;
	default:
		isFighter = true;
		minNumberOfContAttacks = Config.APC_ATTACK_ROW;
		break;
	}
	
	// water movement analysis
	if (_actor instanceof L2Npc)
	{
		int npcId = ((L2Npc) _actor).getNpcId();
		
		switch (npcId)
		{
		case 20314: // great white shark
		case 20849: // Light Worm
			cannotMoveOnLand = true;
			break;
		default:
			cannotMoveOnLand = false;
			break;
		}
	}
	int skilCount = 0;
	// skill analysis
	for (L2Skill sk : _actor.getAllSkills())
	{
		if (sk.isPassive())
			continue;
		
		if (sk.isTeleTypeSkill2())
		{
			teleportRushSkills.add(sk);
			hasTeleportRushSkills = true;
		}
		
		int castRange = sk.getMobCastRange(_actor);
		boolean hasLongRangeDamageSkill = false;
		switch (sk.getSkillType())
		{
		case HEAL:
		case HEAL_PERCENT:
		case HEAL_STATIC:
		case SUPER_HEAL:
		case BALANCE_LIFE:
		case HOT:
			healSkills.add(sk);
			hasHealOrResurrect = true;
			continue; // won't be considered something for fighting
		case BUFF:
			buffSkills.add(sk);
			continue; // won't be considered something for fighting
		case PARALYZE:
		case STUN:
			// hardcoding petrification until improvements are made to	// EffectTemplate... petrification is totally different for	// AI than paralyze
			switch (sk.getId())
			{
			case 367:
			case 4111:
			case 4383:
			case 4616:
			case 4578:
				sleepSkills.add(sk);
				break;
			default:
				generalDisablers.add(sk);
				break;
			}
			break;
		case MUTE:
			muteSkills.add(sk);
			break;
		case SLEEP:
			sleepSkills.add(sk);
			break;
		case ROOT:
			rootSkills.add(sk);
			break;
		case FEAR: // could be used as an alternative for healing?
		case CONFUSION:
			//  trickSkills.add(sk);
		case DEBUFF:
		case WEAKNESS:
		case SWITCH:
		case INSTANT_JUMP:
			debuffSkills.add(sk);
			break;
		case CANCEL:
		case MAGE_BANE:
		case WARRIOR_BANE:
		case NEGATE:
			cancelSkills.add(sk);
			break;
		case RESURRECT:
			resurrectSkills.add(sk);
			hasHealOrResurrect = true;
			break;
		case NOTDONE:
		case COREDONE:
			continue; // won't be considered something for fighting
		case BETRAY:
		case ERASE:
			hasSummonDisableSkills = true;
			summonDisablers.add(sk);
			break;
		case BLOW:
			if (!isArcher && !isMage && !isHealer && sk.getId() != 921)
				shouldAttackFromBehind = true;
		default:
			generalSkills.add(sk);
			hasLongRangeDamageSkill = true;
			break;
		}
		
		if (castRange > 70)
		{
			hasLongRangeSkills = true;
			
			if (hasLongRangeDamageSkill)
				hasLongRangeDamageSkills = true;
		}
		
		if (castRange > maxCastRange)
			maxCastRange = castRange;
		
		if (sk.isOffensive() && sk.getSkillType() != L2SkillType.NOTDONE)
			skilCount++;
	}
	
	if (buffSkills.size() > 1)
	{
		FastList<L2Skill> sortedBuffSkills = new FastList<L2Skill>();
		sortedBuffSkills.addAll(buffSkills);
		Collections.sort(sortedBuffSkills, new buffDurationComparator());
		buffSkills = sortedBuffSkills;
	}
	if (generalSkills.size() > 1)
	{
		FastList<L2Skill> sortedBuffSkills = new FastList<L2Skill>();
		sortedBuffSkills.addAll(generalSkills);
		Collections.sort(sortedBuffSkills, new skillReuseComparator(_actor));
		generalSkills = sortedBuffSkills;
	}
	if (healSkills.size() > 1)
	{
		FastList<L2Skill> sortedBuffSkills = new FastList<L2Skill>();
		sortedBuffSkills.addAll(healSkills);
		Collections.sort(sortedBuffSkills, new skillReuseComparator(_actor));
		healSkills = sortedBuffSkills;
	}
	if (debuffSkills.size() > 1)
	{
		FastList<L2Skill> sortedBuffSkills = new FastList<L2Skill>();
		sortedBuffSkills.addAll(debuffSkills);
		Collections.sort(sortedBuffSkills, new skillReuseComparator(_actor));
		debuffSkills = sortedBuffSkills;
	}
	if (generalDisablers.size() > 1)
	{
		FastList<L2Skill> sortedBuffSkills = new FastList<L2Skill>();
		sortedBuffSkills.addAll(generalDisablers);
		Collections.sort(sortedBuffSkills, new skillReuseComparator(_actor));
		generalDisablers = sortedBuffSkills;
	}
	
	// Because of missing skills, some mages/balanced cannot play like mages
	if (skilCount < 1 && !hasLongRangeDamageSkills && isMage)
	{
		isBalanced = true;
		isMage = false;
		isFighter = false;
	}
	if (!hasLongRangeSkills && (isMage || isBalanced))
	{
		isBalanced = false;
		isMage = false;
		isFighter = true;
	}
	if (skilCount < 1 && generalSkills.isEmpty() && isMage)
	{
		isBalanced = true;
		isMage = false;
	}
}
}

protected class TargetAnalysis
{
public L2Character character;
public boolean isMage;
public boolean isHealer;
public boolean isTank;
public boolean isBalanced;
public boolean isArcher;
public boolean isFighter;
public boolean isCanceled;
public boolean isSlower;
public boolean isMagicResistant;

public TargetAnalysis()
{
}

public void update(L2Character target)
{
	// update status once in 4 seconds
	if (target == character && Rnd.nextInt(100) > 25)
		return;
	character = target;
	if (target == null)
		return;
	isMage = false;
	isHealer = false;
	isTank = false;
	isBalanced = false;
	isArcher = false;
	isFighter = false;
	isCanceled = false;
	
	if (target instanceof L2PcInstance)
	{
		L2PcInstance player = (L2PcInstance) target;
		
		if (player.isTankClass())
		{
			isTank = true;
			isBalanced = true;
		}
		else if (player.isHealerClass())
		{
			isHealer = true;
			isMage = true;
		}
		else if (player.isArcherClass())
		{
			isArcher = true;
		}
		else if (player.isMageClass())
		{
			isMage = true;
		}
		else if (player.isSupportClass() || player.isDaggerClass() || player.isKamaelClass() || player.isGladyTyrantClass())
		{
			isBalanced = true;
			isFighter = true;
		}
		else
		{
			isFighter = true;
		}
	}
	else
	{
		if (target.getMAtk(null, null) > 1.5 * target.getPAtk(null))
			isMage = true;
		else if (target.getPAtk(null) * 0.8 < target.getMAtk(null, null) || target.getMAtk(null, null) * 0.8 > target.getPAtk(null))
		{
			isBalanced = true;
		}
		else
		{
			L2Weapon weapon = target.getActiveWeaponItem();
			if (weapon != null && (weapon.getItemType() == L2WeaponType.BOW || weapon.getItemType() == L2WeaponType.CROSSBOW))
				isArcher = true;
			else
				isFighter = true;
		}
	}
	
	if (target.getRunSpeed() < _actor.getRunSpeed() - 9)
		isSlower = true;
	else
		isSlower = false;
	
	if (target.getMDef(null, null) > target.getPDef(null))
		isMagicResistant = true;
	else
		isMagicResistant = false;
	
	if (target.getBuffCount() < 8)
		isCanceled = true;
}
}
public static class buffDurationComparator implements Comparator<L2Skill>
{
public buffDurationComparator()
{
}
public int compare(L2Skill s1, L2Skill s2)
{
	return Integer.valueOf(s2.getLongestEffectDuration()).compareTo(Integer.valueOf(s1.getLongestEffectDuration()));
}
}
public static class skillReuseComparator implements Comparator<L2Skill>
{
L2Character actor;
public skillReuseComparator(L2Character chara)
{
	actor = chara;
}
public int compare(L2Skill s1, L2Skill s2)
{
	return Integer.valueOf(s2.getReuseDelay(actor)).compareTo(Integer.valueOf(s1.getReuseDelay(actor)));
}
}
}
