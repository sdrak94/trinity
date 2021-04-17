package net.sf.l2j.gameserver.skills.effects;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2EffectPointInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSignetCasttime;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.util.Point3D;

public class EffectSignetMDam extends L2Effect
{
private L2EffectPointInstance _actor;

public EffectSignetMDam(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#getEffectType()
 */
@Override
public L2EffectType getEffectType()
{
	return L2EffectType.SIGNET_GROUND;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onStart()
 */
@Override
public boolean onStart()
{
	L2NpcTemplate template;
	if (getSkill() instanceof L2SkillSignetCasttime)
		template = NpcTable.getInstance().getTemplate(((L2SkillSignetCasttime) getSkill())._effectNpcId);
	else
		return false;
	
	L2EffectPointInstance effectPoint = new L2EffectPointInstance(IdFactory.getInstance().getNextId(), template, getEffector());
	effectPoint.setCurrentHp(effectPoint.getMaxHp());
	effectPoint.setCurrentMp(effectPoint.getMaxMp());
	L2World.getInstance().storeObject(effectPoint);
	
	int x = getEffector().getX();
	int y = getEffector().getY();
	int z = getEffector().getZ();
	
	if (getEffector() instanceof L2PcInstance && getSkill().getTargetType(getEffector()) == L2Skill.SkillTargetType.TARGET_GROUND)
	{
		Point3D wordPosition = ((L2PcInstance) getEffector()).getCurrentSkillWorldPosition();
		
		if (wordPosition != null)
		{
			x = wordPosition.getX();
			y = wordPosition.getY();
			z = wordPosition.getZ();
		}
	}
	super.onStart();
	effectPoint.setIsInvul(true);
	effectPoint.spawnMe(x, y, z);
	
	_actor = effectPoint;
	return true;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
 */
@Override
public boolean onActionTime()
{
	if (getCount() >= getTotalCount() - 2)
		return true; // do nothing first 2 times
	int mpConsume = getSkill().getMpConsume();
	
	L2PcInstance caster = (L2PcInstance) getEffector();
	
	boolean ss = true;
	boolean bss = true;
	
	FastList<L2Character> targets = new FastList<L2Character>();
	
	for (L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(getSkill().getSkillRadius()))
	{
		if (cha == null || cha == caster)
			continue;
		
		if (cha instanceof L2Attackable
				|| cha instanceof L2Playable)
		{
			if (cha.isAlikeDead())
				continue;
			
			if (!cha.isAutoAttackable(caster))
				continue;
			
			if (mpConsume > caster.getCurrentMp())
			{
				caster.sendPacket(new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP));
				return false;
			}
			else
				caster.reduceCurrentMp(mpConsume);
			
			if (cha instanceof L2Playable)
			{
				if (cha instanceof L2Summon && ((L2Summon)cha).getOwner() == caster){}
				else
					caster.updatePvPStatus(cha);
			}
			
			targets.add(cha);
		}
	}
	
	if (!targets.isEmpty())
	{
		caster.broadcastPacket(new MagicSkillLaunched(caster, getSkill().getDisplayId(), getSkill().getDisplayLvl(), targets.toArray(new L2Character[targets.size()])));
		for (L2Character target : targets)
		{
			boolean mcrit = Formulas.calcMCrit(caster.getMCriticalHit(target, getSkill()), target);
			byte shld = Formulas.calcShldUse(caster, target, getSkill());
			int mdam = (int) Formulas.calcMagicDam(caster, target, getSkill(), shld, ss, bss, mcrit);
			
			if (target instanceof L2Summon)
				target.broadcastStatusUpdate();
			
			if (mdam > 0)
			{
				/*if (!target.isRaid()
					        && Formulas.calcAtkBreak(target, mdam))
					{
						target.breakAttack();
						target.breakCast();
					}*/
				caster.sendDamageMessage(target, mdam, mcrit, false, false);
				target.reduceCurrentHp(mdam, caster, getSkill());
			}
			target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, caster);
		}
	}
	return true;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onExit()
 */
@Override
public void onExit()
{
	super.onExit();
	if (_actor != null)
	{
		_actor.deleteMe();
	}
}
}
