package net.sf.l2j.gameserver.handler.usercommandhandlers;

import java.util.logging.Level;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TeleporterInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;

public class Escape implements IUserCommandHandler
{
private static final int[] COMMAND_IDS =
{
	52
};

/**
 * 
 * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#useUserCommand(int, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
 */
public boolean useUserCommand(int id, L2PcInstance activeChar)
{
	if (!L2TeleporterInstance.checkIfCanTeleport(activeChar))
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return false;
	}
	// Thanks nbd
	if (!TvTEvent.onEscapeUse(activeChar.getObjectId()))
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return false;
	}
	
//	if (activeChar.isInFunEvent())
//	{
//		activeChar.sendMessage("You may not escape from an event.");
//		return false;
//	}
	
	if (activeChar.getInstanceId() != 0 && !activeChar.isInActiveFunEvent())
	{
		activeChar.sendMessage("You may not escape from an instance.");
		return false;
	}
	
	final int unstuckTimer = (activeChar.getAccessLevel().isGm() ? 1000 : Config.UNSTUCK_INTERVAL * 1000);
	
	activeChar.forceIsCasting(GameTimeController.getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
	
	L2Skill escape = SkillTable.getInstance().getInfo(2099, 1); // 5 minutes escape
	L2Skill GM_escape = SkillTable.getInstance().getInfo(2100, 1); // 1 second escape
	
	if (activeChar.getAccessLevel().isGm())
	{
		if (GM_escape != null)
		{
			activeChar.doCast(GM_escape);
			return true;
		}
		activeChar.sendMessage("You use Escape: 1 second.");
	}
	else if(activeChar.isInFunEvent())
	{
		if (TvT._started || NewTvT._started || NewHuntingGrounds._started || FOS._started || NewFOS._started || CTF._started || NewCTF._started || VIP._started || DM._started || NewDM._started || NewDomination._started) // when events started it's slightly different
		{
			if (activeChar._inEventTvT && NewTvT._started)
			{
				activeChar.doCast(escape);
			}
			if (activeChar._inEventHG && NewHuntingGrounds._started)
			{
				activeChar.doCast(escape);
			}
			if (activeChar._inEventFOS && NewFOS._started)
			{
				activeChar.doCast(escape);
			}
			if (activeChar._inEventLunaDomi && NewDomination._started)
			{
				activeChar.doCast(escape);
			}
			if (activeChar._inEventCTF && NewCTF._started)
			{
				activeChar.doCast(escape);
			}
			if (activeChar._inEventDM && NewDM._started)
			{
				activeChar.doCast(escape);
			}
		}
	}
	else if (Config.UNSTUCK_INTERVAL == 300 && escape  != null)
	{
		activeChar.doCast(escape);
		return true;
	}
	
	activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
	//SoE Animation section
	activeChar.setTarget(activeChar);
	activeChar.disableAllSkills();
	
	MagicSkillUse msk = new MagicSkillUse(activeChar, 1050, 1, unstuckTimer, 0);
	activeChar.broadcastPacket(msk);
	SetupGauge sg = new SetupGauge(0, unstuckTimer);
	activeChar.sendPacket(sg);
	//End SoE Animation section
	
	EscapeFinalizer ef = new EscapeFinalizer(activeChar);
	// continue execution later
	activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(ef, unstuckTimer));
	
	return true;
}

static class EscapeFinalizer implements Runnable
{
private L2PcInstance _activeChar;

EscapeFinalizer(L2PcInstance activeChar)
{
	_activeChar = activeChar;
}

public void run()
{
	if (_activeChar.isAlikeDead())
		return;
	
	
	try
	{
		if(_activeChar.isInFunEvent())
		{
			_activeChar.setIsIn7sDungeon(false);
			_activeChar.setIsCastingNow(false);
			_activeChar.enableAllSkills();
			if (TvT._started || NewTvT._started || NewHuntingGrounds._started || FOS._started || NewFOS._started || CTF._started || NewCTF._started || VIP._started || DM._started || NewDM._started || NewDomination._started) // when events started it's slightly different
			{
			
				if (_activeChar._inEventTvT && NewTvT._started)
				{
					NewTvT.telePlayerToRndTeamSpot(_activeChar);
				}
				if (_activeChar._inEventHG && NewHuntingGrounds._started)
				{
					NewHuntingGrounds.telePlayerToRndTeamSpot(_activeChar);
				}
				if (_activeChar._inEventFOS && NewFOS._started)
				{
					NewFOS.telePlayerToRndTeamSpot(_activeChar);
				}
				if (_activeChar._inEventLunaDomi && NewDomination._started)
				{
					NewDomination.telePlayerToRndTeamSpot(_activeChar);
				}
				if (_activeChar._inEventCTF && NewCTF._started)
				{
					NewCTF.telePlayerToRndTeamSpot(_activeChar);
				}
				else if (_activeChar._inEventDM && NewDM._started)
				{
					NewDM.telePlayerToRndTeamSpot(_activeChar);
				}
			}
		}
		else
		{
			_activeChar.setIsIn7sDungeon(false);
			_activeChar.enableAllSkills();
			_activeChar.setIsCastingNow(false);
			_activeChar.setInstanceId(0);
			_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
		}
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "", e);
	}
}
}

/**
 * 
 * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#getUserCommandList()
 */
public int[] getUserCommandList()
{
	return COMMAND_IDS;
}
}