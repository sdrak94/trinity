package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.instance.L2DecoyInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TowerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TreeOfLifeInstance;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;

public class L2SkillDecoy extends L2Skill
{

private final int _npcId;
private final int _summonTotalLifeTime;

public L2SkillDecoy(StatsSet set)
{
	super(set);
	_npcId = set.getInteger("npcId", 0);
	_summonTotalLifeTime= set.getInteger("summonTotalLifeTime", 20000);
}

@Override
public void useSkill(L2Character caster, L2Object[] targets)
{
	if (caster.isAlikeDead() || !(caster instanceof L2PcInstance))
		return;
	
	if (_npcId == 0)
		return;
	
	L2PcInstance activeChar = (L2PcInstance) caster;
	
	if (activeChar.inObserverMode())
		return;
	
	if (/*activeChar.getPet() != null || */activeChar.isMounted())
		return;
	
	L2NpcTemplate DecoyTemplate = NpcTable.getInstance().getTemplate(_npcId);
	
	L2Decoy Decoy = null;
	
	if (getName().contains("Haunt"))
	{
		Decoy = new L2TowerInstance(IdFactory.getInstance().getNextId(), DecoyTemplate, activeChar, this);
		Decoy.setCurrentHp(Decoy.getMaxHp());
		Decoy.setCurrentMp(Decoy.getMaxMp());
	}
	if (getName().contains("Tree"))
	{
		Decoy = new L2TreeOfLifeInstance(IdFactory.getInstance().getNextId(), DecoyTemplate, activeChar, this);
		Decoy.setCurrentHp(Decoy.getMaxHp());
		Decoy.setCurrentMp(Decoy.getMaxMp());
	}
	else
	{
		Decoy = new L2DecoyInstance(IdFactory.getInstance().getNextId(), DecoyTemplate, activeChar, this);
		Decoy.setCurrentHp(activeChar.getMaxHp());
		Decoy.setCurrentMp(activeChar.getMaxMp());
	}
	
	Decoy.setHeading(activeChar.getHeading());
	Decoy.setInstanceId(activeChar.getInstanceId());
	activeChar.setDecoy(Decoy);
	L2World.getInstance().storeObject(Decoy);
	
	if (Decoy instanceof L2TowerInstance)
	{
		double angle = Util.convertHeadingToDegree(caster.getHeading());
		double radian = Math.toRadians(angle);
		double course = Math.toRadians(180);
		
		int x1 = (int) (Math.cos(Math.PI + radian + course) * 65);
		int y1 = (int) (Math.sin(Math.PI + radian + course) * 65);
		
		int x = caster.getX() + x1;
		int y = caster.getY() + y1;
		int z = caster.getZ() + 5;
		
		if (Config.GEODATA > 0)
		{
			Location destiny = GeoData.getInstance().moveCheck(caster.getX(), caster.getY(), caster.getZ(), x, y, z, caster.getInstanceId());
			x = destiny.getX();
			y = destiny.getY();
			z = destiny.getZ();
		}
		
		Decoy.spawnMe(x, y, z);
	}
	else
		Decoy.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
	
	if (hasEffects())
		getEffects(activeChar, activeChar);
}

public final int getTotalLifeTime()
{
	return _summonTotalLifeTime;
}
}