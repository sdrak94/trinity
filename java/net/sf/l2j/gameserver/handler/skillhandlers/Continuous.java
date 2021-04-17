package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2ClanHallManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;


public class Continuous implements ISkillHandler
{
//private static Logger _log = Logger.getLogger(Continuous.class.getName());

private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.BUFF,
	L2SkillType.DEBUFF,
	L2SkillType.DOT,
	L2SkillType.MDOT,
	L2SkillType.POISON,
	L2SkillType.BLEED,
	L2SkillType.HOT,
	L2SkillType.CPHOT,
	L2SkillType.MPHOT,
	L2SkillType.FEAR,
	L2SkillType.CONT,
	L2SkillType.WEAKNESS,
	L2SkillType.REFLECT,
	L2SkillType.UNDEAD_DEFENSE,
	L2SkillType.AGGDEBUFF,
	L2SkillType.FAKE_DEATH,
	L2SkillType.FUSION
};
private L2Skill _skill;

public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	boolean acted = true;
	
	L2PcInstance player = null;
	if (activeChar instanceof L2PcInstance)
		player = (L2PcInstance) activeChar;
	if (skill.getEffectId() != 0)
	{
		int skillLevel = skill.getEffectLvl();
		int skillEffectId = skill.getEffectId();
		if (skillLevel == 0)
		{
			_skill = SkillTable.getInstance().getInfo(skillEffectId, 1);
		}
		else
		{
			_skill = SkillTable.getInstance().getInfo(skillEffectId, skillLevel);
		}
		
		if (_skill != null)
			skill = _skill;
	}
	
	for (L2Character target: (L2Character[]) targets)
	{
		// With Mystic Immunity you can't be buffed/debuffed
		if (target == null || target.isPreventedFromReceivingBuffs())
			continue;
		
		if (skill.isOffensive() && target.isPreventedFromReceivingDebuffs())
			continue;
		
		byte shld = 0;
		
		if (Formulas.calcSkillReflect(activeChar, target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
			target = activeChar;
		
		// Player holding a cursed weapon can't be buffed and can't buff
		if (skill.getSkillType() == L2SkillType.BUFF && !(activeChar instanceof L2ClanHallManagerInstance))
		{
			if (target != activeChar)
			{
				if (target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquipped())
					continue;
				else if (player != null && player.isCursedWeaponEquipped())
					continue;
			}
			// TODO: boolean isn't good idea, could cause bugs
			else if (skill.getId() == 2168 && activeChar instanceof L2PcInstance)
				((L2PcInstance) activeChar).setCharmOfLuck(true);
		}
		
		if (skill.isOffensive() || skill.isDebuff())
		{
			shld = Formulas.calcShldUse(activeChar, target, skill);
			acted = ((!target.isInvul() || activeChar.isGM()) && Formulas.calcSkillSuccess(activeChar, target, skill, shld));
		}
		
		if (acted)
		{
			if (!(activeChar instanceof L2PcInstance) && skill.isToggle())
			{
				L2Effect[] effects = target.getAllEffects();
				if (effects != null)
				{
					for (L2Effect e : effects)
					{
						if (e != null && skill != null)
						{
							if (e.getSkill().getId() == skill.getId())
							{
								e.exit();
								return;
							}
						}
					}
				}
			}
			
			// if this is a debuff let the duel manager know about it
			// so the debuff can be removed after the duel
			// (player & target must be in the same duel)
			if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && (skill.getSkillType() == L2SkillType.DEBUFF || skill.getSkillType() == L2SkillType.BUFF) && player != null && player.getDuelId() == ((L2PcInstance) target).getDuelId())
			{
				DuelManager dm = DuelManager.getInstance();
				for (L2Effect buff : skill.getEffects(activeChar, target, new Env(shld, true, true, true)))
					if (buff != null)
						dm.onBuff(((L2PcInstance) target), buff);
			}
			else
				skill.getEffects(activeChar, target, new Env(shld, true, true, true));
			
			if (skill.getSkillType() == L2SkillType.AGGDEBUFF)
			{
				if (target instanceof L2Attackable)
					target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) activeChar.calcStat(Stats.AGGRESSION_PROF, skill.getPower(activeChar) * 5, null, null));
				else if (target instanceof L2Playable && (activeChar.isGM() || !target.isInvul()))
				{
					target.getActingPlayer().standUp();
					
					if (target.getTarget() == activeChar)
						target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, activeChar);
					else
					{
						target.setTarget(activeChar);
						target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, activeChar);
					}
				}
			}
		}
		else
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
			sm.addCharName(target);
			sm.addSkillName(skill);
			activeChar.sendPacket(sm);
		}
		
		// Possibility of a lethal strike
		Formulas.calcLethalHit(activeChar, target, skill);
	}
	skill.getEffectsSelf(activeChar);
}

/**
 * 
 * @see net.sf.l2j.gameserver.handler.ISkillHandler#getSkillIds()
 */
public L2SkillType[] getSkillIds()
{
	return SKILL_IDS;
}
}