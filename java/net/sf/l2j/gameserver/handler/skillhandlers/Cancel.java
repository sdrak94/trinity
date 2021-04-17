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
package net.sf.l2j.gameserver.handler.skillhandlers;

import java.util.ArrayList;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.CharEffectList;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.CustomCancelTask;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.util.Rnd;



/**
 * @author DS
 */
public class Cancel implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.SUPER_CANCEL,
	};
	private final ArrayList<L2Effect> _canceled = new ArrayList<>();
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		byte shld = 0;
		// Delimit min/max % success.
		final int minRate = skill.getSkillType() == L2SkillType.SUPER_CANCEL ? 25 : 40;
		final int maxRate = skill.getSkillType() == L2SkillType.SUPER_CANCEL ? 75 : 95;
		// Get skill power (which is used as baseRate).
		final double skillPower = skill.getPower();

        FastMap<L2Skill, int[]> cancelledBuffs = new FastMap<>();
		for (final L2Object obj : targets)
		{
			if (!(obj instanceof L2Character))
				continue;
			L2Character target = (L2Character) obj;

			if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
			target = activeChar;
			if (Formulas.calcSkillSuccess(activeChar, target, skill, shld))
			{
			if (target.isDead())
				continue;
			shld = Formulas.calcShldUse(activeChar, target, skill);
			int lastCanceledSkillId = 0;
			int max = skill.getMaxNegatedEffects();
			// Calculate the difference of level between skill level and victim,
			// and retrieve the vuln/prof.
			final int diffLevel = skill.getMagicLevel() - target.getLevel();
			final double skillVuln = Formulas.calcSkillVulnerability(activeChar, target, skill);
			for (final L2Effect effect : target.getAllEffects())
			{
                    if (!cancelledBuffs.containsKey(effect.getSkill()))
                {
                       cancelledBuffs.put(effect.getSkill(), new int[] { effect.getCount(), effect.getTime() });
                }
				// Don't cancel null effects or toggles.
				if (effect == null || effect.getSkill().isToggle())
					continue;
				double count = 1;
				
					if (effect.getPeriod() == CharEffectList.BUFFER_BUFFS_DURATION)
						continue;
					
					// do not delete signet effects!
					switch (effect.getEffectType())
					{
					case SIGNET_GROUND:
					case SIGNET_EFFECT:
					case DISGUISE:
						continue;
					default:
						break;
					}
					
					switch(effect.getSkill().getId())
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
					
					switch (effect.getSkill().getSkillType())
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
						effect.exit();
					if (count == max)
						break;
					count++;
					
				// If that skill effect was already canceled, continue.
				if (effect.getSkill().getId() == lastCanceledSkillId)
					continue;
				// Calculate the success chance following previous variables.
				if (calcCancelSuccess(effect.getPeriod(), diffLevel, skillPower, skillVuln, minRate, maxRate))
				{
					// Stores the last canceled skill for further use.
					lastCanceledSkillId = effect.getSkill().getId();
					// Exit the effect.
					effect.cancel();
					_canceled.add(effect);
				}
				// Remove 1 to the stack of buffs to remove.
				count--;
				// If the stack goes to 0, then break the loop.
				if (count == 0)
					break;
			}
			// Possibility of a lethal strike
			Formulas.calcLethalHit(activeChar, target, skill);
			
			if (cancelledBuffs.size() > 0)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new CustomCancelTask((L2PcInstance)target, cancelledBuffs), 25*1000);
			}
			if (skill.hasEffects())
			{
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
				SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
				sm.addSkillName(skill);
				target.sendPacket(sm);
			}
		}
		if (skill.hasSelfEffects())
		{
			final L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if (effect != null && effect.isSelfEffect())
				effect.exit();
			skill.getEffectsSelf(activeChar);
		}
		}
	}
	
	private static boolean calcCancelSuccess(final int effectPeriod, final int diffLevel, final double baseRate, final double vuln, final int minRate, final int maxRate)
	{
		double rate = (2 * diffLevel + baseRate + effectPeriod / 120) * vuln;
		if (rate < minRate)
			rate = minRate;
		else if (rate > maxRate)
			rate = maxRate;
		return Rnd.get(100) < rate;
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}