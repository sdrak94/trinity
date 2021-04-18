package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import luna.custom.globalScheduler.GlobalEventsParser;
import luna.custom.guard.LunaSkillGuard;
import luna.custom.holder.LunaGlobalVariablesHolder;
import luna.custom.skilltrees.SkillTreesParser;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.Domination;
import net.sf.l2j.gameserver.model.events.manager.EventCommander;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventVarHolder;
import net.sf.l2j.gameserver.model.events.manager.EventsParser;
import net.sf.l2j.gameserver.model.events.manager.controler.EventUtils;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;

public class AdminDomination implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_domination",
		"admin_domi",
		"admin_parser_reload",
		"admin_domia",
		"admin_newfos",
		"admin_newfosabort",
		"admin_newctf",
		"admin_newtvt",
		"admin_newdm",
		"admin_newhg",
		"admin_nanoi",
		"admin_run_ev",
		"admin_adomi",
		"admin_nanoi",
		"admin_fosa",
		"admin_tvta",
		"admin_ctfa",
		"admin_hga",
		"admin_domia",
		"admin_dma",
		"admin_tvtaf",
		"admin_threadripper",
		"admin_sk_reload",
		"admin_css",
		"admin_circle",
		"admin_ge_reload"
	};
	
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_domination"))
		{
			Domination.getInstance().startEvent();
		}
		else if (command.startsWith("admin_sk_reload"))
		{
			SkillTreesParser.getInstance().Reload(activeChar);
		}

		else if (command.startsWith("admin_ge_reload"))
		{
			GlobalEventsParser.getInstance().reload();
		}
		else if (command.startsWith("admin_css"))
		{	
			LunaSkillGuard.getInstance().checkForIncorrectSkills(activeChar.getTarget().getActingPlayer());
		}
		
		else if (command.startsWith("admin_circle"))
		{
			Olympiad.getInstance().openOly();
		}
		else if (command.startsWith("admin_parser_reload"))
		{
			EventsParser.getInstance().Reload(activeChar);
		}
		else if (command.equalsIgnoreCase("admin_domi"))
		{
			EventsParser.getInstance().Reload(activeChar);
			NewDomination.loadData(0);
			EventVarHolder.getInstance().setRunningEventId(0);
			NewDomination.autoEvent(EventVarHolder.getInstance().getRunningEventId());
		}
		else if (command.equalsIgnoreCase("admin_newfos"))
		{
			// NewFOS.cleanFos();
			// NewFOS.cleanFosRetail();
			EventsParser.getInstance().Reload(activeChar);
			EventCommander.getInstance().selectEvent("SiegeEvent");
			EventVarHolder.getInstance().setRunningEventId(EventCommander.getInstance().getEventId());
			NewFOS.cleanFos();
			NewFOS.loadData(EventCommander.getInstance().getEventId());
			NewFOS.autoEvent();
		}
		else if (command.equalsIgnoreCase("admin_newctf"))
		{
			// NewFOS.cleanFos();
			// NewFOS.cleanFosRetail();
			EventsParser.getInstance().Reload(activeChar);
			EventCommander.getInstance().selectEvent("CTF");
			EventVarHolder.getInstance().setRunningEventId(EventCommander.getInstance().getEventId());
			// NewCTF.cleanCTF();
			NewCTF.loadData(EventCommander.getInstance().getEventId());
			NewCTF.autoEvent();
		}
		else if (command.equalsIgnoreCase("admin_newtvt"))
		{
			// NewFOS.cleanFos();
			// NewFOS.cleanFosRetail();
			EventsParser.getInstance().Reload(activeChar);
			EventCommander.getInstance().selectEvent("TeamVsTeam");
			EventVarHolder.getInstance().setRunningEventId(EventCommander.getInstance().getEventId());
			// NewCTF.cleanCTF();
			NewTvT.loadData(EventCommander.getInstance().getEventId());
			NewTvT.autoEvent();
		}
		else if (command.equalsIgnoreCase("admin_newdm"))
		{
			// NewFOS.cleanFos();
			// NewFOS.cleanFosRetail();
			EventsParser.getInstance().Reload(activeChar);
			EventCommander.getInstance().selectEvent("DM");
			EventVarHolder.getInstance().setRunningEventId(EventCommander.getInstance().getEventId());
			// NewCTF.cleanCTF();
			NewDM.loadData(EventCommander.getInstance().getEventId());
			NewDM.autoEvent();
		}
		else if (command.equalsIgnoreCase("admin_newhg"))
		{
			// NewFOS.cleanFos();
			// NewFOS.cleanFosRetail();
			EventsParser.getInstance().Reload(activeChar);
			EventCommander.getInstance().selectEvent("HuntingGrounds");
			EventVarHolder.getInstance().setRunningEventId(EventCommander.getInstance().getEventId());
			// NewCTF.cleanCTF();
			NewHuntingGrounds.loadData(EventCommander.getInstance().getEventId());
			NewHuntingGrounds.autoEvent();
			EventEngine.getInstance().setActiveEvent(EventsParser.getInstance().getEvents().get(EventCommander.getInstance().getEventId()));
			EventEngine.getInstance().setEventActive(true);
		}
		else if (command.startsWith("admin_run_ev"))
		{
			String val = command.substring(13);
			EventUtils.makeEvent(activeChar, val);
		}
		else if (command.startsWith("admin_nanoi"))
		{}
		else if (command.equalsIgnoreCase("admin_fosa"))
		{
			NewFOS.abortAndFinish();
		}
		else if (command.equalsIgnoreCase("admin_tvta"))
		{
			NewTvT.abortAndFinish();
		}
		else if (command.equalsIgnoreCase("admin_tvtaf"))
		{
			NewTvT.abortAndFinish();
		}
		else if (command.equalsIgnoreCase("admin_ctfa"))
		{
			NewCTF.abortAndFinish();
		}
		else if (command.equalsIgnoreCase("admin_hga"))
		{
			NewHuntingGrounds.abortAndFinish();
		}
		else if (command.equalsIgnoreCase("admin_domia"))
		{
			NewDomination.abortAndFinish();
		}
		else if (command.equalsIgnoreCase("admin_dma"))
		{
			NewDM.abortAndFinish();
		}
		else if (command.equalsIgnoreCase("admin_doublepvps"))
		{
			StringTokenizer st = new StringTokenizer(command);
			boolean doublePvPs;
			if (st.hasMoreTokens())
			{
				doublePvPs = st.nextToken().equalsIgnoreCase("true");
				if(doublePvPs)
				{
					LunaGlobalVariablesHolder.getInstance().setDoublePvPs(true);
				}
				else
				{
					LunaGlobalVariablesHolder.getInstance().setDoublePvPs(false);
				}
			}
		}
		else if (command.equalsIgnoreCase("admin_evdoublepvpstop"))
		{
			LunaGlobalVariablesHolder.getInstance().endEventDoublePvP();
		}
//		else if (command.startsWith("admin_threadripper"))
//		{
//			MuseumManager.getInstance().reloadConfigs();
//		}
		return true;
	}
	
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}