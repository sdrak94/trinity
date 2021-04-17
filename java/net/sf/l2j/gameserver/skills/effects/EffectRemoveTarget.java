package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.util.Rnd;

public class EffectRemoveTarget extends L2Effect
{
public EffectRemoveTarget(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

@Override
public L2EffectType getEffectType()
{
	return L2EffectType.REMOVE_TARGET;
}

@Override
public boolean onStart()
{
	if (!(getEffected() instanceof L2Playable))
		return false;
	if (!(getEffector() instanceof L2Playable && getEffector().getActingPlayer().isGM()))
	{
		final int resistRemoveTargetChance = (int)getEffected().calcStat(Stats.RESIST_REMOVE_TARGET, 0, null, null);
		
		if (resistRemoveTargetChance >= 1)
		{
			if (Rnd.get(100) < resistRemoveTargetChance)
			{
				if (getEffector() instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(getEffected());
					sm.addSkillName(getSkill());
					getEffector().sendPacket(sm);
				}
				
				return false;
			}
		}
		
		if (getEffected() instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance)getEffected();
			final int effectorId = (getEffector() instanceof L2Playable || getEffector() instanceof L2Decoy) ? getEffector().getActingPlayer().getObjectId() : 0;
			
			if (player != null)
			{
				if (getEffectTemplate()._ignoreProtection ? !player.canBeTricked(effectorId) : !player.canBeTricked(0))
				{
					getEffector().sendMessage(player.getDisplayName()+" is not affected by your "+getSkill().getName()+" because of 16 second trick protection");
					return false;
				}
			}
			else return false;
			
			player.setTrickProtectionTime(effectorId);
			
			if (getSkill().getId() == 11) //trick
				getEffected().getActingPlayer().setIsSelectingTarget(7);
			else
				getEffected().getActingPlayer().setIsSelectingTarget(4);
		}
	}
	super.onStart();
	getEffected().setTarget(null);
	getEffected().abortAttack();
	getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	getEffected().abortCast();
	
	return true;
}

@Override
public boolean onActionTime()
{
	return false;
}
}