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
package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.util.Rnd;


public class EffectTargetMe extends L2Effect
{
public EffectTargetMe(Env env, EffectTemplate template, Boolean ignoreBoost)
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
	return L2EffectType.TARGET_ME;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onStart()
 */
@Override
public boolean onStart()
{
	if (getEffected() instanceof L2Playable)
	{
		if (!(getEffector() instanceof L2Playable && getEffector().getActingPlayer().isGM()))
		{
			final int agres = (int) getEffected().calcStat(Stats.AGGRESSION_VULN, 100, null, null);
			
			if (Rnd.get(100) < 45 || (agres != 100 && Rnd.get(100) >= agres))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
				sm.addCharName(getEffected());
				sm.addSkillName(getSkill());
				getEffector().sendPacket(sm);
				return false;
			}
			
			if (getEffected() instanceof L2SiegeSummonInstance)
				return false;
			
			if (getEffected() instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance)getEffected();
				final int effectorId = (getEffector() instanceof L2Playable || getEffector() instanceof L2Decoy) ? getEffector().getActingPlayer().getObjectId() : 0;
				
				if (player != null)
				{
					if (player._inEventFOS || SiegeManager.getInstance().getSiege(player) != null)
					{
						if ((player.isAFOSLeader() || player.isClanLeader()) && player.isCastingNow())
						{
							if (player.getCurrentSkill() != null && player.getCurrentSkill().getSkillId() == 246) //seal of ruler
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
								sm.addCharName(player);
								sm.addSkillName(getSkill().getId());
								getEffector().sendPacket(sm);
								return false;
							}
						}
					}
					
					if (getEffectTemplate()._ignoreProtection ? !player.canBeTricked(effectorId) : !player.canBeTricked(0))
					{
						getEffector().sendMessage(player.getDisplayName()+" is not affected by your "+getSkill().getName()+" because of 12 second trick/switch/aggro protection");
						return false;
					}
				}
				else return false;
				
				player.setTrickProtectionTime(effectorId);
			}
		}
		
		if (getEffected().getTarget() != getEffector())
		{
			// Target is different - stop autoattack and break cast
			getEffected().setTarget(getEffector());
			getEffected().abortAttack();
			getEffected().abortCast();
			if (getEffected() instanceof L2PcInstance)
				getEffected().sendPacket(new MyTargetSelected(getEffector().getObjectId(), 0));
			getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		
		((L2Playable)getEffected()).setLockedTarget(getEffector(), getPeriod() * GameTimeController.TICKS_PER_SECOND);
		return true;
	}
	return false;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onExit()
 */
@Override
public void onExit()
{
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
 */
@Override
public boolean onActionTime()
{
	// nothing
	return false;
}
}
