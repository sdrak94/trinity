package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TrapInstance;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2SkillTrap extends L2SkillSummon
{
private int _triggerSkillId = 0;
private int _triggerSkillLvl = 0;
private int _trapNpcId = 0;
protected L2Spawn _trapSpawn;

/**
 * 
 * @param set
 */
public L2SkillTrap(StatsSet set)
{
	super(set);
	_triggerSkillId = set.getInteger("triggerSkillId");
	_triggerSkillLvl = set.getInteger("triggerSkillLvl");
	_trapNpcId = set.getInteger("trapNpcId");
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Skill#useSkill(net.sf.l2j.gameserver.model.actor.L2Character, net.sf.l2j.gameserver.model.L2Object[])
 */
@Override
public void useSkill(L2Character caster, L2Object[] targets)
{
	if (caster.isAlikeDead() || !(caster instanceof L2PcInstance))
		return;
	
	if (_trapNpcId == 0)
		return;
	
	L2PcInstance activeChar = (L2PcInstance) caster;
	
	if (activeChar.inObserverMode())
		return;
	
	if (activeChar.isMounted())
		return;
	
	if (_triggerSkillId == 0)
		return;
	
	if (_triggerSkillLvl < 1)
		_triggerSkillLvl = 1;
	
	L2Skill skill = SkillTable.getInstance().getInfo(_triggerSkillId, _triggerSkillLvl);
	
	if (skill == null)
		return;
	
	L2TrapInstance trap;
	L2NpcTemplate TrapTemplate = NpcTable.getInstance().getTemplate(_trapNpcId);
	trap = new L2TrapInstance(IdFactory.getInstance().getNextId(), TrapTemplate, activeChar, getTotalLifeTime(), skill, this);
	trap.setCurrentHp(trap.getMaxHp());
	trap.setCurrentMp(trap.getMaxMp());
	trap.setIsInvul(true);
	trap.setTitle(activeChar.getDisplayName());
	trap.setHeading(activeChar.getHeading());
	trap.setInstanceId(caster.getInstanceId());
	
	L2World.getInstance().storeObject(trap);
	trap.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
}
}
