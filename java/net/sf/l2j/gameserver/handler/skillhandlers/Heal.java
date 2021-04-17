package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeFlagInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.util.Rnd;

public class Heal implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.HEAL,
	L2SkillType.HEAL_PERCENT,
	L2SkillType.HEAL_STATIC,
	L2SkillType.SUPER_HEAL
};

public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	int healers = 0;
	int bishops = 0;
	int elvenElders = 0;
	int shillienElders = 0;
	
	int minus = 3;
	
	if (activeChar.getParty() != null)
	{
		for (L2PcInstance player : activeChar.getParty().getPartyMembers())
		{
			if (player != null && player.isHealerClass() && !player.isInStance())
			{
				healers++;
				if (player.isBishop())
				{
					bishops++;
				}
				else if (player.isElvenElder())
				{
					elvenElders++;
				}
				else if (player.isShillienElder())
				{
					shillienElders++;
				}
				if (minus != 2 && (player.getPvpFlag() != 0 || player.getKarma() > 0/* || player.isInsideZone(L2Character.ZONE_PVP)*/))
					minus = 2;
			}
		}
	}
	
	//check for other effects
	try
	{
		ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(L2SkillType.BUFF);
		
		if (handler != null)
			handler.useSkill(activeChar, skill, targets);
	}
	catch (Exception e)
	{
	}
	
	L2PcInstance player = null;
	if (activeChar instanceof L2PcInstance)
		player = (L2PcInstance) activeChar;
	
	for (L2Character target: (L2Character[]) targets)
	{
		// We should not heal if char is dead
		if (target == null || target.isDead())
			continue;
		
		// Player holding a cursed weapon can't be healed and can't heal
		if (target != activeChar)
		{
			if (skill.getId() == 1015)
			{
				continue;
			}
			if (target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquipped())
				continue;
			else if (player != null && player.isCursedWeaponEquipped())
				continue;
		}
		
		double hp = skill.getPower(activeChar);
		
		if (skill.getSkillType() == L2SkillType.HEAL_PERCENT || skill.getSkillType() == L2SkillType.SUPER_HEAL)
		{
			hp = target.getMaxHp() * hp / 100.0;
		}
		else
		{
			hp *= 1.2;
		}
		
		if (bishops >= 2) //2 or more healers in party
		{
			int nerfValue = (3 + (bishops - minus));
			//activeChar.sendMessage("Bishops: "+bishops +" HP: "+ hp +" Nerfed Value: " + nerfValue);
			hp /= nerfValue;
		}
		if (elvenElders >= 2) //2 or more healers in party
		{
			int nerfValue = (3 + (elvenElders - minus));
			//activeChar.sendMessage("Elven Elders: "+elvenElders +" HP: "+ hp +" Nerfed Value: " + nerfValue);
			hp /= nerfValue;
		}
		if (shillienElders >= 2) //2 or more healers in party
		{
			int nerfValue = (3 + (shillienElders - minus));
			//activeChar.sendMessage("Shillien Elders: "+shillienElders +" HP: "+ hp +" Nerfed Value: " + nerfValue);
			hp /= nerfValue;
		}
		if (healers >= 3) //2 or more healers in party
		{
			int nerfValue = (5 + (healers - minus));
			hp /= nerfValue;
		}
		if (activeChar.isInFunEvent())
		{
			hp = hp * Config.EVENT_HEAL_MUL;
		}
		final double healEffectiveness = target.calcStat(Stats.HEAL_EFFECTIVNESS, 1, null, skill);
		
		if (target instanceof L2DoorInstance || target instanceof L2SiegeFlagInstance)
		{
			hp = 0;
		}
		else
		{
			if (skill.getSkillType() == L2SkillType.HEAL_STATIC)
			{
				hp = skill.getPower(activeChar);
			}
			else if (skill.getSkillType() != L2SkillType.HEAL_PERCENT && skill.getSkillType() != L2SkillType.SUPER_HEAL)
			{
				hp *= healEffectiveness;
				// Healer proficiency (since CT1)
				hp *= activeChar.calcStat(Stats.HEAL_PROFICIENCY, 1, null, skill);
				// Extra bonus (since CT1.5)
				
				if (!skill.isPotion())
				{
					hp += target.calcStat(Stats.HEAL_STATIC_BONUS, 0, null, skill);
					
					// Heal critic, since CT2.3 Gracia Final
					if (skill.getSkillType() == L2SkillType.HEAL && Formulas.calcMCritHeal(activeChar.getMCriticalHit(target, skill)))
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_MAGIC));
						hp *= activeChar.calcStat(Stats.MAGIC_CRITICAL_DAMAGE, 1.7, null, skill);
					}
				}
			}
			else //must be heal_percent or super_heal
			{
				if (healEffectiveness < 1)
					hp *= healEffectiveness;
			}
		}
		
		final int targetHp = (int)target.getCurrentHp();
		
		if (targetHp + hp > target.getMaxHp())
		{
			hp = target.getMaxHp() - targetHp;
		}
		
		if (hp > 0)
		{
			target.setCurrentHp(hp + targetHp);
			target.setLastHealAmount((int) hp);
			StatusUpdate su = new StatusUpdate(target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			target.sendPacket(su);
			
			if (target instanceof L2PcInstance)
			{
				if (skill.getId() == 4051)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.REJUVENATING_HP);
					target.sendPacket(sm);
				}
				else
				{
					if (activeChar instanceof L2PcInstance && activeChar != target)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S2_HP_RESTORED_BY_C1);
						sm.addCharName(activeChar);
						sm.addNumber((int) hp);
						target.sendPacket(sm);
						activeChar.sendMessage("You healed "+target.getDisplayName()+" for "+(int)hp+" HP");
//						for (final L2Attackable att : target.getKnownList().getKnownType(L2Attackable.class))
//							if (att.getMostHated() == target || att.getTarget() == target)
//								att.addDamageHate(activeChar, skill.getAggroPoints());
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_HP_RESTORED);
						sm.addNumber((int) hp);
						target.sendPacket(sm);
					}
				}
			}
		}
		
		if (skill.getSkillType() == L2SkillType.SUPER_HEAL)
		{
			if (target instanceof L2PcInstance)
			{
				int cp = (int)(target.getMaxCp() * skill.getPower(activeChar) / 100.0);
				
				if ((target.getStatus().getCurrentCp() + cp) >= target.getMaxCp())
					cp = (int) (target.getMaxCp() - target.getStatus().getCurrentCp());
				
				if (cp > 0)
				{
					target.getStatus().setCurrentCp(cp + target.getStatus().getCurrentCp());
					StatusUpdate sump = new StatusUpdate(target.getObjectId());
					sump.addAttribute(StatusUpdate.CUR_CP, (int) target.getStatus().getCurrentCp());
					target.sendPacket(sump);
					
					if (activeChar instanceof L2PcInstance && activeChar != target)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED);
						sm.addNumber(cp);
						target.sendPacket(sm);
						activeChar.sendMessage("You CP-healed "+target.getDisplayName()+" for "+cp+" CP");
//						for (final L2Attackable att : target.getKnownList().getKnownType(L2Attackable.class))
//							if (att.getMostHated() == target || att.getTarget() == target)
//								att.addDamageHate(activeChar, skill.getAggroPoints());
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED);
						sm.addNumber(cp);
						target.sendPacket(sm);
					}
				}
			}
			
			int mp = (int)(target.getMaxMp() * skill.getPower(activeChar) / 100.0);
			
			if ((target.getStatus().getCurrentMp() + mp) >= target.getMaxMp())
				mp = (int) (target.getMaxMp() - target.getStatus().getCurrentMp());
			
			if (mp > 0)
			{
				target.getStatus().setCurrentMp(mp + target.getStatus().getCurrentMp());
				StatusUpdate sump = new StatusUpdate(target.getObjectId());
				sump.addAttribute(StatusUpdate.CUR_MP, (int) target.getStatus().getCurrentMp());
				target.sendPacket(sump);
				
				if (activeChar instanceof L2PcInstance && activeChar != target)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S2_MP_RESTORED_BY_C1);
					sm.addCharName(activeChar);
					sm.addNumber(mp);
					target.sendPacket(sm);
					activeChar.sendMessage("You recharged "+target.getDisplayName()+" for "+mp+" MP");
//					for (final L2Attackable att : target.getKnownList().getKnownType(L2Attackable.class))
//						if (att.getMostHated() == target || att.getTarget() == target)
//							att.addDamageHate(activeChar, skill.getAggroPoints());
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_MP_RESTORED);
					sm.addNumber(mp);
					target.sendPacket(sm);
				}
			}
			
			for (L2Effect e : target.getAllEffects())
			{
				if (e != null && e.getSkill().isDebuff())
				{
					e.exit();
					
					if (Rnd.get(100) > 40)
						break;
				}
			}
		}
	}
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
