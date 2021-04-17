package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillElemental extends L2Skill {

private final int[] _seeds;
private final boolean _seedAny;

public L2SkillElemental(StatsSet set) {
	super(set);
	
	_seeds = new int[3];
	_seeds[0] = set.getInteger("seed1",0);
	_seeds[1] = set.getInteger("seed2",0);
	_seeds[2] = set.getInteger("seed3",0);
	
	if (set.getInteger("seed_any",0)==1)
		_seedAny = true;
	else
		_seedAny = false;
}

@Override
public void useSkill(L2Character activeChar, L2Object[] targets) {
	if (activeChar.isAlikeDead())
		return;
	
	L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
	
	if (activeChar instanceof L2PcInstance)
	{
		if (weaponInst == null)
		{
			activeChar.sendMessage("You must equip your weapon before casting a spell.");
			return;
		}
	}
	
	for (L2Character target: (L2Character[]) targets)
	{
		if (target.isAlikeDead())
			continue;
		
		boolean charged = true;
		if (!_seedAny){
			for (int seed : _seeds)
			{
				if (seed!=0){
					L2Effect e = target.getFirstEffect(seed);
					if (e==null || !e.getInUse()){
						charged = false;
						break;
					}
				}
			}
		}
		else {
			charged = false;
			for (int seed : _seeds)
			{
				if (seed!=0){
					L2Effect e = target.getFirstEffect(seed);
					if (e!=null && e.getInUse()){
						charged = true;
						break;
					}
				}
			}
		}
		if (!charged)
		{
			activeChar.sendMessage("Target is not charged by elements.");
			continue;
		}
		
		boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, this), target);
		byte shld = Formulas.calcShldUse(activeChar, target, this);
		
		int damage = (int)Formulas.calcMagicDam(activeChar, target, this, shld, true, true, mcrit);
		
		if (damage > 0)
		{
			target.reduceCurrentHp(damage, activeChar, this);
			
			
			if (hasEffects())
			{
				getEffects(activeChar, target, new Env(shld, true, false, true));
			}

			if (activeChar instanceof L2PcInstance && target instanceof L2RaidBossInstance)
			{
				activeChar.incDamageCount((int)damage); 
				target.addRaidAttackerToList(activeChar);
			}
			activeChar.sendDamageMessage(target, damage, false, false, false);
		}
	}
}
}