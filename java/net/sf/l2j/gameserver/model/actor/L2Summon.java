package net.sf.l2j.gameserver.model.actor;

import java.util.Collection;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Attackable.AggroInfo;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.SummonKnownList;
import net.sf.l2j.gameserver.model.actor.stat.SummonStat;
import net.sf.l2j.gameserver.model.actor.status.SummonStatus;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.itemcontainer.PetInventory;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExPartyPetWindowAdd;
import net.sf.l2j.gameserver.network.serverpackets.ExPartyPetWindowDelete;
import net.sf.l2j.gameserver.network.serverpackets.ExPartyPetWindowUpdate;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.PetDelete;
import net.sf.l2j.gameserver.network.serverpackets.PetInfo;
import net.sf.l2j.gameserver.network.serverpackets.PetItemList;
import net.sf.l2j.gameserver.network.serverpackets.PetStatusShow;
import net.sf.l2j.gameserver.network.serverpackets.PetStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Weapon;

public abstract class L2Summon extends L2Playable
{
//private static Logger _log = Logger.getLogger(L2Summon.class.getName());

private L2PcInstance _owner;
private int _attackRange = 36; //Melee range
private boolean _follow = true;
private boolean _previousFollowStatus = true;

public class AIAccessor extends L2Character.AIAccessor
{
protected AIAccessor() {}
public L2Summon getSummon() { return L2Summon.this; }
public boolean isAutoFollow() {
	return getFollowStatus();
}
public void doPickupItem(L2Object object) {
	L2Summon.this.doPickupItem(object);
}
}

public L2Summon(int objectId, L2NpcTemplate template, L2PcInstance owner)
{
	super(objectId, template);
	
	setInstanceId(owner.getInstanceId()); // set instance to same as owner
	
	_showSummonAnimation = true;
	_owner = owner;
	_ai = new L2SummonAI(new L2Summon.AIAccessor());
	
	setXYZInvisible(owner.getX()+50, owner.getY()+100, owner.getZ()+100);
}

@Override
public void onSpawn()
{
	super.onSpawn();
	if (!(this instanceof L2MerchantSummonInstance))
	{
		setFollowStatus(true);
		updateAndBroadcastStatus(0);
		getOwner().sendPacket(new RelationChanged(this, getOwner().getRelation(getOwner()), false));
		for (L2PcInstance player : getOwner().getKnownList().getKnownPlayersInRadius(800))
			player.sendPacket(new RelationChanged(this, getOwner().getRelation(player), isAutoAttackable(player)));
		L2Party party = getOwner().getParty();
		if (party != null)
		{
			party.broadcastToPartyMembers(getOwner(), new ExPartyPetWindowAdd(this));
		}
	}
	setShowSummonAnimation(false); // addVisibleObject created the info packets with summon animation
	// if someone comes into range now, the animation shouldnt show any more
	
}

@Override
public final SummonKnownList getKnownList()
{
	return (SummonKnownList)super.getKnownList();
}

@Override
public void initKnownList()
{
	setKnownList(new SummonKnownList(this));
}

@Override
public SummonStat getStat()
{
	return (SummonStat)super.getStat();
}

@Override
public void initCharStat()
{
	setStat(new SummonStat(this));
}

@Override
public SummonStatus getStatus()
{
	return (SummonStatus)super.getStatus();
}

@Override
public void initCharStatus()
{
	setStatus(new SummonStatus(this));
}

@Override
public boolean isDebuffable(L2PcInstance attacker)
{
	return getOwner().isDebuffable(attacker);
}

@Override
public L2CharacterAI getAI()
{
	L2CharacterAI ai = _ai; // copy handle
	if (ai == null)
	{
		synchronized(this)
		{
			if (_ai == null) _ai = new L2SummonAI(new L2Summon.AIAccessor());
			return _ai;
		}
	}
	return ai;
}

@Override
public L2NpcTemplate getTemplate()
{
	return (L2NpcTemplate)super.getTemplate();
}

// this defines the action buttons, 1 for Summon, 2 for Pets
public abstract int getSummonType();

@Override
public final void stopAllEffects()
{
	super.stopAllEffects();
	updateAndBroadcastStatus(1);
}

@Override
public final void stopAllEffectsExceptThoseThatLastThroughDeath()
{
	super.stopAllEffectsExceptThoseThatLastThroughDeath();
	updateAndBroadcastStatus(1);
}

@Override
public void updateAbnormalEffect()
{
	Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
	//synchronized (getKnownList().getKnownPlayers())
	{
		for (L2PcInstance player : plrs)
			player.sendPacket(new AbstractNpcInfo.SummonInfo(this, player,1));
	}
}

/**
 * @return Returns the mountable.
 */
public boolean isMountable()
{
	return false;
}

@Override
public void onAction(L2PcInstance player)
{
	// Aggression target lock effect
	if (player.isLockedTarget() && player.getLockedTarget() != this)
	{
		player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (player == _owner && player.getTarget() == this)
	{
		player.sendPacket(new PetStatusShow(this));
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	else if (player.getTarget() != this)
	{
		if (player.isSelectingTarget())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.setIsSelectingTarget(3);
		
		if (Config.DEBUG) _log.fine("new target selected:"+getObjectId());
		player.setTarget(this);
		MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
		player.sendPacket(my);
		
		//sends HP/MP status of the summon to other characters
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		player.sendPacket(su);
	}
	else if (player.getTarget() == this)
	{
		if (isAutoAttackable(player))
		{
			if (Config.GEODATA > 0)
			{
				if (GeoData.getInstance().canSeeTarget(player, this))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				}
			}
			else
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
		}
		else
		{
			// This Action Failed packet avoids player getting stuck when clicking three or more times
			player.sendPacket(ActionFailed.STATIC_PACKET);
			if (Config.GEODATA > 0)
			{
				if (GeoData.getInstance().canSeeTarget(player, this))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
				}
			}
			else
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
			}
		}
	}
}

public long getExpForThisLevel()
{
	if(getLevel() >= Experience.LEVEL.length)
	{
		return 0;
	}
	return Experience.LEVEL[getLevel()];
}

public long getExpForNextLevel()
{
	if(getLevel() >= Experience.LEVEL.length - 1)
	{
		return 0;
	}
	return Experience.LEVEL[getLevel()+1];
}

public final int getKarma()
{
	return getOwner()!= null ? getOwner().getKarma() : 0;
}

public final byte getPvpFlag()
{
	return getOwner()!= null ? getOwner().getPvpFlag() : 0;
}

public final int getTeam()
{
	return getOwner()!= null ? getOwner().getTeam() : 0;
}

public final L2PcInstance getOwner()
{
	return _owner;
}

public final int getNpcId()
{
	return getTemplate().npcId;
}

public int getMaxLoad()
{
	return 0;
}

public void followOwner()
{
	setFollowStatus(true);
}

@Override
public boolean doDie(L2Character killer)
{
	if (!super.doDie(killer))
		return false;
	if (this instanceof L2MerchantSummonInstance)
		return true;
	L2PcInstance owner = getOwner();
	
	if (owner != null)
	{
		Collection<L2Character> KnownTarget = getKnownList().getKnownCharacters();
		for (L2Character TgMob : KnownTarget)
		{
			// get the mobs which have aggro on the this instance
			if (TgMob instanceof L2Attackable)
			{
				if (((L2Attackable) TgMob).isDead())
					continue;
				
				AggroInfo info = ((L2Attackable) TgMob).getAggroList().get(this);
				if (info != null)
					((L2Attackable) TgMob).addDamageHate(owner, info._aggro);
			}
		}
	}
	
	if (isPhoenixBlessed() && (getOwner() != null && !((CTF._started || NewCTF._started) && getOwner()._inEventCTF)))
		getOwner().reviveRequest(getOwner(), null, true);
	
	DecayTaskManager.getInstance().addDecayTask(this);
	return true;
}

public boolean doDie(L2Character killer, boolean decayed)
{
	if (!super.doDie(killer))
		return false;
	if (!decayed)
	{
		DecayTaskManager.getInstance().addDecayTask(this);
	}
	return true;
}

public void stopDecay()
{
	DecayTaskManager.getInstance().cancelDecayTask(this);
}

@Override
public void onDecay()
{
	deleteMe(_owner);
}

@Override
public void broadcastStatusUpdate()
{
	//super.broadcastStatusUpdate();
	updateAndBroadcastStatus(1);
}

public void deleteMe(L2PcInstance owner)
{
	getAI().stopFollow();
	owner.sendPacket(new PetDelete(getObjectId(), 2));
	
	//pet will be deleted along with all his items
	if (getInventory() != null)
	{
		getInventory().destroyAllItems("pet deleted", getOwner(), this);
	}
	decayMe();
	getKnownList().removeAllKnownObjects();
	owner.setPet(null);
}

public void unSummon(L2PcInstance owner)
{
	if (isVisible() && !isDead())
	{
		getAI().stopFollow();
		owner.sendPacket(new PetDelete(getObjectId(), 2));
		L2Party party;
		if ((party = owner.getParty()) != null)
		{
			party.broadcastToPartyMembers(owner, new ExPartyPetWindowDelete(this));
		}
		
		store();
		giveAllToOwner();
		owner.setPet(null);
		
		stopAllEffects();
		L2WorldRegion oldRegion = getWorldRegion();
		decayMe();
		if (oldRegion != null) oldRegion.removeFromZones(this);
		getKnownList().removeAllKnownObjects();
		setTarget(null);
	}
}

public int getAttackRange()
{
	return _attackRange;
}

public void setAttackRange(int range)
{
	if (range < 36)
		range = 36;
	_attackRange = range;
}

public void setFollowStatus(boolean state)
{
	_follow = state;
	if (_follow)
		getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, getOwner());
	else
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
}

public boolean getFollowStatus()
{
	return _follow;
}


@Override
public boolean isAutoAttackable(L2Character attacker)
{
	return _owner.isAutoAttackable(attacker);
}

public int getControlItemId()
{
	return 0;
}

public L2Weapon getActiveWeapon()
{
	return null;
}

@Override
public PetInventory getInventory()
{
	return null;
}

protected void doPickupItem(L2Object object)
{
}

public void giveAllToOwner()
{
}

public void store()
{
}

@Override
public L2ItemInstance getActiveWeaponInstance()
{
	return null;
}

@Override
public L2Weapon getActiveWeaponItem()
{
	return null;
}

@Override
public L2ItemInstance getSecondaryWeaponInstance()
{
	return null;
}

@Override
public L2Weapon getSecondaryWeaponItem()
{
	return null;
}

/**
 * Return True if the L2Summon is invulnerable or if the summoner is in spawn protection.<BR><BR>
 */
@Override
public boolean isInvul()
{
	return _isInvul  || _isTeleporting ||  getOwner().isSpawnProtected();
}

/**
 * Return the L2Party object of its L2PcInstance owner or null.<BR><BR>
 */
@Override
public L2Party getParty()
{
	if (_owner == null)
		return null;
	else
		return _owner.getParty();
}

/**
 * Return True if the L2Character has a Party in progress.<BR><BR>
 */
@Override
public boolean isInParty()
{
	if (_owner == null)
		return false;
	else
		return _owner.getParty() != null;
}

/**
 * Check if the active L2Skill can be casted.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Check if the target is correct </li>
 * <li>Check if the target is in the skill cast range </li>
 * <li>Check if the summon owns enough HP and MP to cast the skill </li>
 * <li>Check if all skills are enabled and this skill is enabled </li><BR><BR>
 * <li>Check if the skill is active </li><BR><BR>
 * <li>Notify the AI with AI_INTENTION_CAST and target</li><BR><BR>
 *
 * @param skill The L2Skill to use
 * @param forceUse used to force ATTACK on players
 * @param dontMove used to prevent movement, if not in range
 *
 */
public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
{
	if (skill == null || isDead())
		return;
	
	// Check if the skill is active
	if (skill.isPassive())
		return;
	//************************************* Check Casting in Progress *******************************************
	
	// If a skill is currently being used
	if (isCastingNow())
		return;
	//************************************* Check Target *******************************************
	
	// Get the target for the skill
	L2Object target = null;
	
	switch (skill.getTargetType(this))
	{
	// OWNER_PET should be cast even if no target has been found
	case TARGET_OWNER_PET:
		target = getOwner();
		break;
		// PARTY, AURA, SELF should be cast even if no target has been found
	case TARGET_PARTY:
	case TARGET_AURA:
	case TARGET_FRONT_AURA:
	case TARGET_BEHIND_AURA:
	case TARGET_SELF:
	case TARGET_SELF_AND_PET:
	case TARGET_ALL:
		target = this;
		break;
	default:
		// Get the first target of the list
		target = skill.getFirstOfTargetList(this);
		break;
	}
	
	// Check the validity of the target
	if (target == null)
	{
		if (getOwner() != null)
			getOwner().sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
		
		return;
	}
	
	//************************************* Check skill availability *******************************************
	
	// Check if this skill is enabled (e.g. reuse time)
	if (isSkillDisabled(skill.getId()))
	{
		if (getOwner() != null)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
			sm.addSkillName(skill);
			getOwner().sendPacket(sm);
		}
		return;
	}
	
	//************************************* Check Consumables *******************************************
	
	// Check if the summon has enough MP
	if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
	{
		// Send a System Message to the caster
		if (getOwner() != null)
			getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
		return;
	}
	
	// Check if the summon has enough HP
	if (getCurrentHp() <= skill.getHpConsume())
	{
		// Send a System Message to the caster
		if (getOwner() != null)
			getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_HP));
		return;
	}
	
	//************************************* Check Summon State *******************************************
	
	// Check if this is offensive magic skill
	if (skill.isOffensive())
	{
		if (isInsidePeaceZone(this, target)	&& getOwner() != null && (!getOwner().getAccessLevel().allowPeaceAttack()))
		{
			if (!isInFunEvent() || !target.isInFunEvent())
			{
				// If summon or target is in a peace zone, send a system message TARGET_IN_PEACEZONE
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				return;
			}
		}
		
		if (getOwner() != null && getOwner().isInOlympiadMode() && !getOwner().isOlympiadStart())
		{
			// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if the target is attackable
		if (target instanceof L2DoorInstance)
		{
			if(!((L2DoorInstance)target).isAttackable(getOwner()))
				return;
		}
		else
		{
			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !forceUse &&
					skill.getTargetType(this) != SkillTargetType.TARGET_AURA &&
					skill.getTargetType(this) != SkillTargetType.TARGET_FRONT_AURA &&
					skill.getTargetType(this) != SkillTargetType.TARGET_BEHIND_AURA &&
					skill.getTargetType(this) != SkillTargetType.TARGET_CLAN &&
					skill.getTargetType(this) != SkillTargetType.TARGET_ALLY &&
					skill.getTargetType(this) != SkillTargetType.TARGET_PARTY &&
					skill.getTargetType(this) != SkillTargetType.TARGET_SELF &&
					skill.getTargetType(this) != SkillTargetType.TARGET_ALL &&
					skill.getTargetType(this) != SkillTargetType.TARGET_SELF_AND_PET)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return;
			}
			
			if (!getOwner().checkPvpSkill(getTarget(), skill, forceUse))
			{
				// Send a System Message to the L2PcInstance
				getOwner().sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return;
			}
		}
	}
	getOwner().setCurrentPetSkill(skill, forceUse, dontMove);
	// Notify the AI with AI_INTENTION_CAST and target
	getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
}

@Override
public void setIsImmobilized(boolean value)
{
	super.setIsImmobilized(value);
	
	if (value)
	{
		_previousFollowStatus = getFollowStatus();
		// if immobilized temporarly disable follow mode
		if (_previousFollowStatus)
			setFollowStatus(false);
	}
	else
	{
		// if not more immobilized restore previous follow mode
		setFollowStatus(_previousFollowStatus);
	}
}

public void setOwner(L2PcInstance newOwner)
{
	_owner = newOwner;
}

@Override
public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
{
	if (miss || getOwner() == null)
		return;
	
	// Prevents the double spam of system messages, if the target is the owning player.
	if (target.getObjectId() != getOwner().getObjectId())
	{
		if (pcrit || mcrit)
			if (this instanceof L2SummonInstance)
				getOwner().sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_BY_SUMMONED_MOB));
			else
				getOwner().sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_BY_PET));
		
		if (getOwner().isInOlympiadMode() &&
				target instanceof L2PcInstance &&
				((L2PcInstance)target).isInOlympiadMode() &&
				((L2PcInstance)target).getOlympiadGameId() == getOwner().getOlympiadGameId())
		{
			Olympiad.getInstance().notifyCompetitorDamage(getOwner(), damage, getOwner().getOlympiadGameId());
		}
		
		final SystemMessage sm;
		
		if (target.isInvul() && !(target instanceof L2NpcInstance))
		{
			
		}
		else
		{
			if (target instanceof L2RaidBossInstance)
			{
				getOwner().incDamageCount(damage);
			}
			sm = new SystemMessage(SystemMessageId.SUMMON_GAVE_DAMAGE_S1);
			sm.addNumber(damage);
			getOwner().sendPacket(sm);
		}
	}
}

@Override
public void reduceCurrentHp(double damage, L2Character attacker, L2Skill skill)
{
	super.reduceCurrentHp(damage, attacker, skill);
	if (getOwner() != null && (!attacker.isInvisible() || (!attacker.isGMReally() && getOwner().canSeeInvisiblePeople())))
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.SUMMON_RECEIVED_DAMAGE_S2_BY_S1);
		sm.addCharName(attacker);
		sm.addNumber((int)damage);
		getOwner().sendPacket(sm);
	}
}

@Override
public void doCast(L2Skill skill)
{
	super.doCast(skill);
}

@Override
public boolean isInCombat()
{
	return getOwner().isInCombat();
}

@Override
public L2PcInstance getActingPlayer()
{
	return getOwner();
}

@Override
public void broadcastPacket(L2GameServerPacket mov)
{
	mov.setInvisible(getOwner().isInvisible());
	super.broadcastPacket(mov);
}

@Override
public void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist)
{
	mov.setInvisible(getOwner().isInvisible());
	super.broadcastPacket(mov, radiusInKnownlist);
}

public void updateAndBroadcastStatus(int val)
{
	getOwner().sendPacket(new PetInfo(this,val));
	getOwner().sendPacket(new PetStatusUpdate(this));
	if (isVisible())
	{
		broadcastNpcInfo(val);
	}
	L2Party party = getOwner().getParty();
	if (party != null)
	{
		party.broadcastToPartyMembers(getOwner(), new ExPartyPetWindowUpdate(this));
	}
	updateEffectIcons(true);
}

public void broadcastNpcInfo(int val)
{
	Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
	for (L2PcInstance player : plrs)
	{
		try
		{
			if (player == getOwner() && !(this instanceof L2MerchantSummonInstance))
				continue;
			player.sendPacket(new AbstractNpcInfo.SummonInfo(this,player, val));
		}
		catch (NullPointerException e)
		{
			// ignore it
		}
	}
}
public boolean isHungry()
{
	return false;
}
@Override
public final boolean isAttackingNow()
{
	return isInCombat();
}

public int getWeapon()
{
	return 0;
}

public int getArmor()
{
	return 0;
}

public int getPetSpeed()
{
	return getTemplate().baseRunSpd;
}

@Override
public void sendInfo(L2PcInstance activeChar)
{
	// Check if the L2PcInstance is the owner of the Pet
	if (activeChar.equals(getOwner()) && !(this instanceof L2MerchantSummonInstance))
	{
		activeChar.sendPacket(new PetInfo(this, 0));
		// The PetInfo packet wipes the PartySpelled (list of active  spells' icons).  Re-add them
		updateEffectIcons(true);
		if (this instanceof L2PetInstance)
		{
			activeChar.sendPacket(new PetItemList((L2PetInstance) this));
		}
	}
	else
		activeChar.sendPacket(new AbstractNpcInfo.SummonInfo(this, activeChar, 0));
}


@Override
public int getMaxHp()
{
	final int baseMaxHP = super.getMaxHp();
	
	return (int) _owner.calcStat(Stats.SUMMON_HP_BOOST, baseMaxHP, this, null);
}
}
