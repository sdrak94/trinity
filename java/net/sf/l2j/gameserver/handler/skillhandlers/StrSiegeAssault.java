package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class StrSiegeAssault implements ISkillHandler
{
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.STRSIEGEASSAULT
};

public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	
	if (!(activeChar instanceof L2PcInstance))
		return;
	
	L2PcInstance player = (L2PcInstance) activeChar;
	
	if (!player.isRidingStrider())
		return;
	if (!(player.getTarget() instanceof L2DoorInstance))
		return;
	
	Castle castle = CastleManager.getInstance().getCastle(player);
	Fort fort = FortManager.getInstance().getFort(player);
	
	if ((castle == null) && (fort == null))
		return;
	
	if (castle != null)
	{
		if (!checkIfOkToUseStriderSiegeAssault(player, castle, true))
			return;
	}
	else
	{
		if (!checkIfOkToUseStriderSiegeAssault(player, fort, true))
			return;
	}
	
	try
	{
		// damage calculation
		int damage = 0;
		
		for (L2Character target: (L2Character[]) targets)
		{
			if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && ((L2PcInstance)target).isFakeDeath())
			{
				target.stopFakeDeath(null);
			}
			else if (target.isDead())
				continue;
			
			boolean dual = activeChar.isUsingDualWeapon();
			byte shld = Formulas.calcShldUse(activeChar, target, skill);
			boolean crit = Formulas.calcCrit(activeChar, activeChar.getCriticalHit(target, skill), target, skill);
			
			if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0)
				damage = 0;
			else
				damage = (int) Formulas.calcPhysDam(activeChar, target, skill, shld, crit, dual, true);
			
			if (damage > 0)
			{
				target.reduceCurrentHp(damage, activeChar, skill);
				
				activeChar.sendDamageMessage(target, damage, false, false, false);
				
			}
			else
				activeChar.sendMessage(skill.getName() + " failed.");
		}
	}
	catch (Exception e)
	{
		player.sendMessage("Error using siege assault:" + e);
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

/**
 * Return true if character clan place a flag<BR><BR>
 *
 * @param activeChar The L2Character of the character placing the flag
 * @param isCheckOnly if false, it will send a notification to the player telling him
 * why it failed
 */
public static boolean checkIfOkToUseStriderSiegeAssault(L2Character activeChar, boolean isCheckOnly)
{
	Castle castle = CastleManager.getInstance().getCastle(activeChar);
	Fort fort = FortManager.getInstance().getFort(activeChar);
	
	if ((castle == null) && (fort == null))
		return false;
	
	if (castle != null)
		return checkIfOkToUseStriderSiegeAssault(activeChar, castle, isCheckOnly);
	else
		return checkIfOkToUseStriderSiegeAssault(activeChar, fort, isCheckOnly);
	
}

/**
 * 
 * @param activeChar
 * @param castle
 * @param isCheckOnly
 * @return
 */
public static boolean checkIfOkToUseStriderSiegeAssault(L2Character activeChar, Castle castle, boolean isCheckOnly)
{
	if (!(activeChar instanceof L2PcInstance))
		return false;
	
	String text = "";
	L2PcInstance player = (L2PcInstance) activeChar;
	
	if (castle == null || castle.getCastleId() <= 0)
		text = "You must be on castle ground to use strider siege assault";
	else if (!castle.getSiege().getIsInProgress())
		text = "You can only use strider siege assault during a siege.";
	else if (!(player.getTarget() instanceof L2DoorInstance))
		text = "You can only use strider siege assault on doors and walls.";
	else if (!player.isRidingStrider())
		text = "You can only use strider siege assault when on strider.";
	else
		return true;
	
	if (!isCheckOnly)
		player.sendMessage(text);
	return false;
}

/**
 * 
 * @param activeChar
 * @param fort
 * @param isCheckOnly
 * @return
 */
public static boolean checkIfOkToUseStriderSiegeAssault(L2Character activeChar, Fort fort, boolean isCheckOnly)
{
	if (!(activeChar instanceof L2PcInstance))
		return false;
	
	String text = "";
	L2PcInstance player = (L2PcInstance) activeChar;
	
	if (fort == null || fort.getFortId() <= 0)
		text = "You must be on fort ground to use strider siege assault";
	else if (!fort.getSiege().getIsInProgress())
		text = "You can only use strider siege assault during a siege.";
	else if (!(player.getTarget() instanceof L2DoorInstance))
		text = "You can only use strider siege assault on doors and walls.";
	else if (!player.isRidingStrider())
		text = "You can only use strider siege assault when on strider.";
	else
		return true;
	
	if (!isCheckOnly)
		player.sendMessage(text);
	return false;
}
}