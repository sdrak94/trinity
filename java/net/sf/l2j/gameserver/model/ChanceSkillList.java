package net.sf.l2j.gameserver.model;

import java.io.IOException;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.skills.effects.EffectChanceSkillTrigger;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public class ChanceSkillList extends FastMap<IChanceSkillTrigger, ChanceCondition>
{
private static final long serialVersionUID = 1L;

private L2Character _owner;

@SuppressWarnings("deprecation")
public ChanceSkillList(L2Character owner)
{
	super();
	setShared(true);
	_owner = owner;
}

public L2Character getOwner()
{
	return _owner;
}

public void setOwner(L2Character owner)
{
	_owner = owner;
}

public void onHit(L2Character target, boolean ownerWasHit, boolean wasCrit)
{
	int event;
	if (ownerWasHit)
	{
		event = ChanceCondition.EVT_ATTACKED | ChanceCondition.EVT_ATTACKED_HIT;
		if (wasCrit)
			event |= ChanceCondition.EVT_ATTACKED_CRIT;
	}
	else
	{
		event = ChanceCondition.EVT_HIT;
		if (wasCrit)
			event |= ChanceCondition.EVT_CRIT;
	}
	
	onEvent(event, target, Elementals.NONE, Elementals.NONE);
}

public void onEvadedHit(L2Character attacker)
{
	onEvent(ChanceCondition.EVT_EVADED_HIT, attacker, Elementals.NONE, Elementals.NONE);
}

public void onSkillHit(L2Character target, boolean ownerWasHit, L2Skill skill)
{
	final boolean wasOffensive = skill.isOffensive();
	int event;
	
	if (ownerWasHit)
	{
		event = ChanceCondition.EVT_HIT_BY_SKILL;
		if (wasOffensive)
		{
			event |= ChanceCondition.EVT_HIT_BY_OFFENSIVE_SKILL;
			event |= ChanceCondition.EVT_ATTACKED;
		}
		else
		{
			event |= ChanceCondition.EVT_HIT_BY_GOOD_MAGIC;
		}
	}
	else
	{
		event = ChanceCondition.EVT_CAST;
		event |= skill.isMagic() ? ChanceCondition.EVT_MAGIC : ChanceCondition.EVT_PHYSICAL;
		event |= wasOffensive ? ChanceCondition.EVT_MAGIC_OFFENSIVE : ChanceCondition.EVT_MAGIC_GOOD;
	}
	
	onEvent(event, target, skill.getElement(), skill.getId());
}

public void onEvent(int event, L2Character target, byte element, int skillID)
{
	final boolean playable = target instanceof L2Playable;
	if(_owner instanceof L2PcInstance && (NewHuntingGrounds._started && _owner.getActingPlayer()._inEventHG))
		return;
	for (FastMap.Entry<IChanceSkillTrigger, ChanceCondition> e = head(), end = tail(); (e = e.getNext()) != end;)
	{
		if (e.getValue() != null && e.getValue().trigger(event, element, playable, skillID))
		{
			if (e.getKey() instanceof L2Skill)
				makeCast((L2Skill)e.getKey(), target);
			else if (e.getKey() instanceof EffectChanceSkillTrigger)
				makeCast((EffectChanceSkillTrigger)e.getKey(), target);
		}
	}
}

private void makeCast(L2Skill skill, L2Character target)
{
	if(_owner instanceof L2PcInstance && (NewHuntingGrounds._started && _owner.getActingPlayer()._inEventHG))
		return;
	if (target instanceof L2Playable)
	{
		if (target.getActingPlayer().isInOlympiadMode() && skill.isDisabledInOlympiad())
			return;

		if (target.getActingPlayer().isInKoreanZone() && skill.isDisabledInKorean())
			return;
		if ((_owner instanceof L2Playable || _owner instanceof L2Decoy) && !skill.isDamaging())
		{
			if (!target.getActingPlayer().isDebuffable(_owner.getActingPlayer()))
				return;
		}
	}
	
	if(skill.getWeaponDependancy(_owner,true) && skill.checkCondition(_owner, target, false))
	{
		if(skill.triggersChanceSkill()) //skill will trigger another skill, but only if its not chance skill
		{
			skill = SkillTable.getInstance().getInfo(skill.getTriggeredChanceId(), skill.getTriggeredChanceLevel());
			if(skill == null || skill.getSkillType() == L2SkillType.NOTDONE)
				return;
			
			final int castRange = skill.getCastRange(_owner);
			
			if (castRange >= 1)
			{
				if (Util.calculateDistance(_owner, target, true) > castRange)
					return;
			}
		}
		
		if (_owner.isSkillDisabled(skill.getId()))
			return;
		
		if (skill.getReuseDelay(_owner) > 0)
			_owner.disableSkill(skill.getId(), skill.getReuseDelay(_owner));
		
		L2Object[] targets = skill.getTargetList(_owner, false, target);
		
		boolean auraSkill = skill.getTargetType(_owner) == SkillTargetType.TARGET_AURA || skill.getTargetType(_owner) == SkillTargetType.TARGET_BEHIND_AURA
		|| skill.getTargetType(_owner) == SkillTargetType.TARGET_FRONT_AURA;
		
		if (targets == null || targets.length == 0)
		{
			if (!auraSkill)
				return;
		}
		
		boolean areaSkill = auraSkill || skill.getTargetType(_owner) == SkillTargetType.TARGET_BEHIND_AREA
		|| skill.getTargetType(_owner) == SkillTargetType.TARGET_FRONT_AREA;
		
		L2Character firstTarget = targets == null ? _owner : areaSkill ? _owner : (L2Character)targets[0];
		
		ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
		
		/*		// Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)
		int timeAtk = Math.max(0, _owner.calculateTimeBetweenAttacks(target, _owner.getActiveWeaponItem()) - 450);
		timeAtk /= 5;*/
		
		/*boolean rangedWep = _owner.isUsingRangedWep();
			
			// the hit is calculated to happen halfway to the animation - might need further tuning e.g. in bow case
			final int delay = (rangedWep ? timeAtk : timeAtk);*/
		
		ThreadPoolManager.getInstance().scheduleAi(new DisplaySkillTask(_owner, firstTarget, skill, targets), Config.CHANCE_SKILL_DELAY);
		
		/*_owner.broadcastPacket(new MagicSkillUse(_owner, firstTarget, skill.getDisplayId(), skill.getDisplayLvl(), 0, 0));*/
		
		if (handler != null)
			try
		{
				handler.useSkill(_owner, skill, targets);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		else
			skill.useSkill(_owner, targets);
		
		if (skill.getSound() != null)
			_owner.broadcastPacket(new PlaySound(0, skill.getSound(), 1, _owner.getObjectId(), _owner.getX(), _owner.getY(), _owner.getZ()));
	}
	
}

private void makeCast(EffectChanceSkillTrigger effect, L2Character target)
{
	if(_owner instanceof L2PcInstance && (NewHuntingGrounds._started && _owner.getActingPlayer()._inEventHG))
		return;
	if (effect == null || !effect.triggersChanceSkill())
		return;
	
	final L2Skill triggered = SkillTable.getInstance().getInfo(effect.getTriggeredChanceId(), effect.getTriggeredChanceLevel());
	
	if (triggered == null || triggered.getSkillType() == L2SkillType.NOTDONE || _owner.isSkillDisabled(triggered.getId()))
		return;
	
	if (target instanceof L2Playable)
	{
		if (target.getActingPlayer().isInOlympiadMode() && triggered.isDisabledInOlympiad())
			return;
		if (target.getActingPlayer().isInKoreanZone() && triggered.isDisabledInKorean())
			return;
		if ((_owner instanceof L2Playable || _owner instanceof L2Decoy) && !triggered.isDamaging())
		{
			if (!target.getActingPlayer().isDebuffable(_owner.getActingPlayer()))
				return;
		}
	}
	
	if(!triggered.getWeaponDependancy(_owner,true) || !triggered.checkCondition(_owner, target, false))
		return;
	
	final int castRange = triggered.getCastRange(_owner);
	
	if (castRange >= 1)
	{
		if (Util.calculateDistance(_owner, target, true) > castRange)
			return;
	}
	
	if (triggered.getReuseDelay(_owner) > 0)
		_owner.disableSkill(triggered.getId(), triggered.getReuseDelay(_owner));
	
	final L2Object[] targets = triggered.getTargetList(_owner, false, target);
	
	boolean auraSkill = triggered.getTargetType(_owner) == SkillTargetType.TARGET_AURA || triggered.getTargetType(_owner) == SkillTargetType.TARGET_BEHIND_AURA
	|| triggered.getTargetType(_owner) == SkillTargetType.TARGET_FRONT_AURA;
	
	if (targets == null || targets.length == 0)
	{
		if (!auraSkill)
			return;
	}
	
	boolean areaSkill = auraSkill || triggered.getTargetType(_owner) == SkillTargetType.TARGET_BEHIND_AREA
	|| triggered.getTargetType(_owner) == SkillTargetType.TARGET_FRONT_AREA;
	
	L2Character firstTarget = targets == null ? _owner : areaSkill ? _owner : (L2Character)targets[0];
	
	final ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(triggered.getSkillType());
	
	/*	// Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)
	int timeAtk = Math.max(0, _owner.calculateTimeBetweenAttacks(target, _owner.getActiveWeaponItem()) - 450);
	timeAtk /= 5;*/
	
	/*boolean rangedWep = _owner.isUsingRangedWep();
	
	// the hit is calculated to happen halfway to the animation - might need further tuning e.g. in bow case
	final int delay = (rangedWep ? timeAtk : timeAtk);*/
	
	ThreadPoolManager.getInstance().scheduleAi(new DisplaySkillTask(_owner, firstTarget, triggered, targets), Config.CHANCE_SKILL_DELAY);
	
	/*_owner.broadcastPacket(new MagicSkillUse(_owner, firstTarget, triggered.getDisplayId(), triggered.getDisplayLvl(), 0, 0));*/
	
	if (handler != null)
		try
	{
			handler.useSkill(_owner, triggered, targets);
	}
	catch (IOException e)
	{
		e.printStackTrace();
	}
	else
		triggered.useSkill(_owner, targets);
	
	if (triggered.getSound() != null)
		_owner.broadcastPacket(new PlaySound(0, triggered.getSound(), 1, _owner.getObjectId(), _owner.getX(), _owner.getY(), _owner.getZ()));
	
}
class DisplaySkillTask implements Runnable
{
private final L2Character _char;
private final L2Character _first;
private final L2Skill _skill;
private final L2Object[] _targets;

DisplaySkillTask(L2Character dude, L2Character first, L2Skill skill, L2Object[] targets)
{
	_char = dude;
	_first = first;
	_skill = skill;
	_targets = targets;
}

public void run()
{
	_char.broadcastPacket(new MagicSkillUse(_char, _first, _skill.getDisplayId(), _skill.getDisplayLvl(), 0, 0));
	_char.broadcastPacket(new MagicSkillLaunched(_char, _skill.getDisplayId(), _skill.getDisplayLvl(), _targets));
}
}
}