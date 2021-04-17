package net.sf.l2j.gameserver.handler.skillhandlers;

import java.util.logging.Level;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.events.AutomatedTvT;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public class SummonFriend implements ISkillHandler
{
//private static Logger _log = Logger.getLogger(SummonFriend.class.getName());
private static final L2SkillType[] SKILL_IDS =
{
	L2SkillType.SUMMON_FRIEND
};

public static boolean checkSummonerStatus(L2PcInstance summonerChar)
{
	if (summonerChar == null)
		return false;
	
	if (summonerChar.isInOlympiadMode())
	{
		summonerChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
		return false;
	}
	
	if (summonerChar.inObserverMode())
	{
		return false;
	}
	
	if (AutomatedTvT.isPlaying(summonerChar))
	{
		summonerChar.sendPacket(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
		return false;
	}
	
	if (summonerChar.isInFunEvent())
	{
		summonerChar.sendMessage("You cannot summon people while in events");
		return false;
	}
	
	if (!TvTEvent.onEscapeUse(summonerChar.getObjectId()))
	{
		summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
		return false;
	}
	
	if (summonerChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND) || summonerChar.isFlyingMounted())
	{
		summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
		return false;
	}
	return true;
}

public static boolean checkTargetStatus(L2PcInstance targetChar, L2PcInstance summonerChar)
{
	if (targetChar == null)
		return false;
	
	if (targetChar.isAlikeDead())
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED);
		sm.addPcName(targetChar);
		summonerChar.sendPacket(sm);
		return false;
	}
	
	if (targetChar.isInStoreMode())
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.C1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED);
		sm.addPcName(targetChar);
		summonerChar.sendPacket(sm);
		return false;
	}
	
	if (targetChar.isRooted() || targetChar.isInCombat())
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED);
		sm.addPcName(targetChar);
		summonerChar.sendPacket(sm);
		return false;
	}
	
	if (targetChar.isInOlympiadMode())
	{
		summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD));
		return false;
	}
	
	if (targetChar.isFestivalParticipant() || targetChar.isFlyingMounted())
	{
		summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
		return false;
	}
	
	if (targetChar.inObserverMode())
	{
		summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
		return false;
	}
	
	if (!TvTEvent.onEscapeUse(targetChar.getObjectId()))
	{
		summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
		return false;
	}
	
	if (targetChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND) || AutomatedTvT.isPlaying(targetChar))
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.C1_IN_SUMMON_BLOCKING_AREA);
		sm.addString(targetChar.getName());
		summonerChar.sendPacket(sm);
		return false;
	}
	
	if (targetChar.isInFunEvent())
	{
		summonerChar.sendMessage("You cannot summon people while they're in events");
		return false;
	}
	
	if (summonerChar.getInstanceId() > 0)
	{
		Instance summonerInstance = InstanceManager.getInstance().getInstance(summonerChar.getInstanceId());
		if (!Config.ALLOW_SUMMON_TO_INSTANCE || !summonerInstance.isSummonAllowed())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
			summonerChar.sendPacket(sm);
			return false;
		}
	}
	
	// on retail character can enter 7s dungeon with summon friend,
	// but will be teleported away by mobs
	// because currently this is not working in L2J we do not allowing summoning
	if (summonerChar.isIn7sDungeon())
	{
		int targetCabal = SevenSigns.getInstance().getPlayerCabal(targetChar);
		if (SevenSigns.getInstance().isSealValidationPeriod())
		{
			if (targetCabal != SevenSigns.getInstance().getCabalHighestScore())
			{
				summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
				return false;
			}
		}
		else
		{
			if (targetCabal == SevenSigns.CABAL_NULL)
			{
				summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
				return false;
			}
		}
	}
	
	return true;
}

public static void teleToTarget(L2PcInstance targetChar, L2PcInstance summonerChar, L2Skill summonSkill)
{
	if (targetChar == null || summonerChar == null || summonSkill == null)
		return;
	
	if (!checkSummonerStatus(summonerChar))
		return;
	if (!checkTargetStatus(targetChar, summonerChar))
		return;
	
	int itemConsumeId = summonSkill.getTargetConsumeId();
	int itemConsumeCount = summonSkill.getTargetConsume();
	if (itemConsumeId != 0 && itemConsumeCount != 0)
	{
		String ItemName = ItemTable.getInstance().getTemplate(itemConsumeId).getName();
		if (targetChar.getInventory().getInventoryItemCount(itemConsumeId, 0) < itemConsumeCount)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_REQUIRED_FOR_SUMMONING);
			sm.addString(ItemName);
			targetChar.sendPacket(sm);
			return;
		}
		targetChar.getInventory().destroyItemByItemId("Consume", itemConsumeId, itemConsumeCount, summonerChar, targetChar);
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
		sm.addString(ItemName);
		targetChar.sendPacket(sm);
	}
	// set correct instance id
	targetChar.setInstanceId(summonerChar.getInstanceId());
	targetChar.setIsIn7sDungeon(summonerChar.isIn7sDungeon());
	
	targetChar.teleToLocation(summonerChar.getX(), summonerChar.getY(), summonerChar.getZ(), true);
}

/**
 * 
 * @see net.sf.l2j.gameserver.handler.ISkillHandler#useSkill(net.sf.l2j.gameserver.model.actor.L2Character, net.sf.l2j.gameserver.model.L2Skill, net.sf.l2j.gameserver.model.L2Object[])
 */
public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
{
	if (!(activeChar instanceof L2PcInstance))
		return; // currently not implemented for others
	L2PcInstance activePlayer = (L2PcInstance) activeChar;
	
	if (!checkSummonerStatus(activePlayer))
		return;
	
	if (skill.getTargetType(activeChar) == SkillTargetType.TARGET_PARTY || skill.getTargetType(activeChar) == SkillTargetType.TARGET_CLAN)
	{
		for (L2RaidBossInstance boss : RaidBossSpawnManager.getInstance().getBosses().values())
		{
			if (boss != null && !boss.isAlikeDead() && boss.getInstanceId() == activeChar.getInstanceId())
			{
				if (boss.isInsideRadius(activeChar, 3000, true, false))
				{
					activeChar.sendMessage("You can't use mass-summon when near a live raidboss");
					return;
				}
			}
		}
	}
	
	try
	{
		for (L2Character target: (L2Character[]) targets)
		{
			if (activeChar == target)
				continue;
			
			if (target instanceof L2PcInstance)
			{
				L2PcInstance targetPlayer = (L2PcInstance) target;
				
				if (!checkTargetStatus(targetPlayer, activePlayer))
					continue;
				
				if (!Util.checkIfInRange(0, activeChar, target, false))
				{
					if(!targetPlayer.teleportRequest(activePlayer, skill))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_ALREADY_SUMMONED);
						sm.addString(target.getName());
						activePlayer.sendPacket(sm);
						continue;
					}
					if (skill.getId() == 1403) //summon friend
					{
						// Send message
						ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.C1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId());
						confirm.addCharName(activeChar);
						confirm.addZoneName(activeChar.getX(), activeChar.getY(), activeChar.getZ());
						confirm.addTime(30000);
						confirm.addRequesterId(activePlayer.getCharId());
						target.sendPacket(confirm);
					}
					else
					{
						teleToTarget(targetPlayer, activePlayer, skill);
						targetPlayer.teleportRequest(null, null);
					}
				}
			}
		}
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "", e);
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
