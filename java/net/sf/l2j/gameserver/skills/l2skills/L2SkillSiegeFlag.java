package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeFlagInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillSiegeFlag extends L2Skill
{
private final boolean _isAdvanced;

public L2SkillSiegeFlag(StatsSet set)
{
	super(set);
	_isAdvanced = set.getBool("isAdvanced", false);
}
/**
 * @see net.sf.l2j.gameserver.model.L2Skill#useSkill(net.sf.l2j.gameserver.model.actor.L2Character, net.sf.l2j.gameserver.model.L2Object[])
 */
@Override
public void useSkill(L2Character activeChar, L2Object[] targets)
{
	if (!(activeChar instanceof L2PcInstance))
		return;
	
	L2PcInstance player = (L2PcInstance) activeChar;
	
	if (player.getClan() == null || player.getClan().getLeaderId() != player.getObjectId())
		return;
	
	Castle castle = CastleManager.getInstance().getCastle(player);
	Fort fort = FortManager.getInstance().getFort(player);
	
	if ((castle == null) && (fort == null))
		return;
	 
	if (castle != null)
	{
		if (!checkIfOkToPlaceFlag(player, castle, true))
			return;
	}
	else
	{
		if (!checkIfOkToPlaceFlag(player, fort, true))
			return;
	}
	
	try
	{
		// Spawn a new flag
		L2SiegeFlagInstance flag = new L2SiegeFlagInstance(player, IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(35062), _isAdvanced);
		flag.setTitle(player.getClan().getName());
		flag.setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
		flag.setHeading(player.getHeading());
		flag.setInstanceId(player.getInstanceId());
		flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);
		if (castle != null)
			castle.getSiege().getFlag(player.getClan()).add(flag);
		else
			fort.getSiege().getFlag(player.getClan()).add(flag);
		
	}
	catch (Exception e)
	{
		player.sendMessage("Error placing flag:" + e);
	}
}
/**
 * Return true if character clan place a flag<BR><BR>
 *
 * @param activeChar The L2Character of the character placing the flag
 * @param isCheckOnly if false, it will send a notification to the player telling him
 * why it failed
 */
public static boolean checkIfOkToPlaceFlag(L2Character activeChar, boolean isCheckOnly)
{
	Castle castle = CastleManager.getInstance().getCastle(activeChar);
	Fort fort = FortManager.getInstance().getFort(activeChar);
	
	if ((castle == null) && (fort == null))
		return false;
	if (castle != null)
		return checkIfOkToPlaceFlag(activeChar, castle, isCheckOnly);
	else
		return checkIfOkToPlaceFlag(activeChar, fort, isCheckOnly);
}

/**
 * 
 * @param activeChar
 * @param castle
 * @param isCheckOnly
 * @return
 */
public static boolean checkIfOkToPlaceFlag(L2Character activeChar, Castle castle, boolean isCheckOnly)
{
	if (!(activeChar instanceof L2PcInstance))
		return false;
	
	String text = "";
	L2PcInstance player = (L2PcInstance) activeChar;
	
	if (castle == null || castle.getCastleId() <= 0)
		text = "You must be on castle ground to place a flag.";
	else if (!castle.getSiege().getIsInProgress())
		text = "You can only place a flag during a siege.";
	else if (castle.getSiege().getAttackerClan(player.getClan()) == null)
		text = "You must be an attacker to place a flag.";
	else if (player.getClan() == null || !player.isClanLeader())
		text = "You must be a clan leader to place a flag.";
	else if (castle.getSiege().getAttackerClan(player.getClan()).getNumFlags() >= SiegeManager.getInstance().getFlagMaxCount())
		text = "You have already placed the maximum number of flags possible";
	//else if (player.isInsideZone(L2Character.ZONE_CASTLE))
	//	text = "You cannot place flag here.";
//	else if (!activeChar.isInsideZone(L2Character.ZONE_HQ))
//		text = "This is not an Headquarters area, you can't spawn flag here.";
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
public static boolean checkIfOkToPlaceFlag(L2Character activeChar, Fort fort, boolean isCheckOnly)
{
	if (!(activeChar instanceof L2PcInstance))
		return false;
	
	String text = "";
	L2PcInstance player = (L2PcInstance) activeChar;
	
	if (fort == null || fort.getFortId() <= 0)
		text = "You must be on fort ground to place a flag.";
	else if (!fort.getSiege().getIsInProgress())
		text = "You can only place a flag during a siege.";
	else if (fort.getSiege().getAttackerClan(player.getClan()) == null)
		text = "You must be an attacker to place a flag.";
	else if (player.getClan() == null || !player.isClanLeader())
		text = "You must be a clan leader to place a flag.";
	else if (fort.getSiege().getAttackerClan(player.getClan()).getNumFlags() >= FortSiegeManager.getInstance().getFlagMaxCount())
		text = "You have already placed the maximum number of flags possible.";
	else if (player.isInsideZone(L2Character.ZONE_NOHQ))
		text = "You cannot place flag here.";
//	else if (!activeChar.isInsideZone(L2Character.ZONE_HQ))
//		text = "This is not an Headquarters area, you can't spawn flag here.";
	else
		return true;
	
	if (!isCheckOnly)
		player.sendMessage(text);
	return false;
}
}