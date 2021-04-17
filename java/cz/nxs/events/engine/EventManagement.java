/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.nxs.events.engine;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.logging.Level;

import cz.nxs.events.Configurable;
import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventRewardSystem.EventRewards;
import cz.nxs.events.engine.EventRewardSystem.PositionContainer;
import cz.nxs.events.engine.EventRewardSystem.RewardItem;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.GlobalConfigModel;
import cz.nxs.events.engine.base.Loc;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.RewardPosition.PositionType;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.MainEventManager.EventScheduleData;
import cz.nxs.events.engine.main.MainEventManager.RegNpcLoc;
import cz.nxs.events.engine.main.MainEventManager.State;
import cz.nxs.events.engine.main.base.MainEventInstanceType;
import cz.nxs.events.engine.main.base.MainEventInstanceTypeManager;
import cz.nxs.events.engine.main.events.AbstractMainEvent;
import cz.nxs.events.engine.mini.DoorAction;
import cz.nxs.events.engine.mini.EventMode;
import cz.nxs.events.engine.mini.EventMode.FeatureCategory;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.events.engine.mini.FeatureBase;
import cz.nxs.events.engine.mini.FeatureBase.FeatureInfo;
import cz.nxs.events.engine.mini.MiniEventGame;
import cz.nxs.events.engine.mini.MiniEventManager;
import cz.nxs.events.engine.mini.RegistrationData;
import cz.nxs.events.engine.mini.ScheduleInfo.Day;
import cz.nxs.events.engine.mini.ScheduleInfo.RunTime;
import cz.nxs.events.engine.mini.features.AbstractFeature;
import cz.nxs.events.engine.mini.features.AbstractFeature.FeatureConfig;
import cz.nxs.events.engine.mini.tournament.TournamentManager;
import cz.nxs.events.engine.stats.EventStatsManager;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.FenceData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.NpcTemplateData;
import cz.nxs.interf.delegate.ObjectData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 * - a little messy but proud part of Nexus Engine
 * - handles everything inside of //event_manage command
 */
public class EventManagement
{
	private FastMap<Integer, EditingInfo> _editingInfos;
	
	public static final int TRAINING_DUMMY_ID = 31691;
	
	private String[] miniEventEditingPages = { "Maps", "Modes", "Rewards", "Matches", "Configs" };
	private String[] mainEventEditingPages = { "Maps", "Rewards", "Configs", "Instances" };
	
	private class EditingInfo
	{
		private Map<Integer, Map<Integer, ObjectData>> spawned = new FastMap<Integer, Map<Integer, ObjectData>>();
		private int activeEditingMap = 0;
		private String activeEditingMapPage = "Info"; // default page
		private EventType activeEditingMapEvent = null;
		private int activeEditingPage = 0;
		private String activeEventEditingSection = null;
		private int activeEditingSpawn;
		
		private RewardPosition activeEditingRewardPos;
		private String activeEditingRewardParam;
		
		private EventType activeEditingEvent;
		private String activeEditingConfigCategory;
		private int activeEventModeId = 1;
		private int activeEventModeTimeId = 0;
		private FeatureType activeEventModeFeatureShowed = null;
		private FeatureCategory activeEventModeFeatureCategory = null;
		private int activeEditingMatch;
		private int activeEditingReward;
		
		private int activeGlobalConfigPage = 0;
		private String activeGlobalConfigType = null;
		private String activeGlobalConfigKeyShown = "";
		
		private int activeEdittingMainInstanceType = 0;
		
		private String currentGoBackAblePage = null;
		
		public int currentConfigModelPage = 1;
		private String currentConfigModelShownCategory = "";
		private String currentConfigModelShown = "";
		
		private SpawnType spawnFilter = null;
		private boolean asc = false;
		
		private Map<Integer, FastList<PlayerEventInfo>> manualMatchPlayers = new FastMap<Integer, FastList<PlayerEventInfo>>(2);
	}
	
	public EventManagement()
	{
		_editingInfos = new FastMap<Integer, EditingInfo>();
	}
	
	public boolean commandRequiresConfirm(String command)
	{
		if(command.startsWith("remove_event_map"))
			return true;
		else if(command.startsWith("mini_edit_modes_delete"))
			return true;
		else if(command.startsWith("mini_edit_modes_clear"))
			return true;
		else if(command.startsWith("mini_edit_modes_scheduler_removetime"))
			return true;
		else if(command.startsWith("mini_edit_feature_remove"))
			return true;
		else if(command.startsWith("mini_abort_match"))
			return true;
		else if(command.startsWith("abort_current"))
			return true;
		else if(command.startsWith("remove_event_from_map"))
			return true;
		/*else if(command.startsWith(""))
			return true;
		else if(command.startsWith(""))
			return true;
		else if(command.startsWith(""))
			return true;*/
		
		return false;
	}
	
	public void showMenu(PlayerEventInfo gm, boolean miniEvents)
	{
		if(!NexusLoader.loaded()) return;
		
		String html;
		if(!miniEvents)
			html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_menu.htm");
		else
			html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_menu_mini.htm");

		if(html == null)
		{
			gm.sendMessage("HTML files (or at least eventmanage_menu.htm) are missing.");
			return;
		}
		
		html = html.replaceAll("%objectId%", String.valueOf(0));
		
		AbstractMainEvent event = EventManager.getInstance().getCurrentMainEvent();
		
		html = html.replaceAll("%event%", event == null ? "N/A" : event.getEventType().getAltTitle());
		html = html.replaceAll("%map%", event == null ? "N/A" : EventManager.getInstance().getMainEventManager().getMapName());
		
		String time = EventManager.getInstance().getMainEventManager().getTimeLeft(true);
		
		html = html.replaceAll("%time%", event == null ? "N/A" : time);
		html = html.replaceAll("%players%", String.valueOf(EventManager.getInstance().getMainEventManager().getPlayersCount()));
		
		html = html.replaceAll("%state%", EventManager.getInstance().getMainEventManager().getState().toString());
		
		html = html.replaceAll("%pauseName%", EventManager.getInstance().getMainEventManager().autoSchedulerPaused() ? "Unpause" : "Pause");
		
		String runStopButton = EventManager.getInstance().getMainEventManager().autoSchedulerEnabled() ? 
				"<button value=\"Stop\" action=\"bypass admin_event_manage abort_auto_scheduler\" width=85 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" :
				"<button value=\"Start\" action=\"bypass admin_event_manage restart_auto_scheduler\" width=85 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
		
		html = html.replaceAll("%runStop%", runStopButton);
		
		if(EventManager.getInstance().getMainEventManager().getState() == State.RUNNING)
		{
			if(event.isWatching(gm))
				html = html.replaceAll("%runEventAction%", "<button value=\"Stop watching\" action=\"bypass -h admin_event_manage watch_current_stop\" width=78 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			else
				html = html.replaceAll("%runEventAction%", "<button value=\"Watch event\" action=\"bypass -h admin_event_manage watch_current_menu\" width=78 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		}
		else
			html = html.replaceAll("%runEventAction%", "<button value=\"Skip delay\" action=\"bypass -h admin_event_manage skip_current\" width=78 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		
		if(miniEvents)
		{
			TextBuilder tb = new TextBuilder();
			
			boolean bg = false;
			
			for(EventType type : EventType.values())
			{
				if(!EventConfig.getInstance().isEventAllowed(type) || type.isRegularEvent() || !type.allowEdits() || EventManager.getInstance().getMiniEvents().get(type) == null)
					continue;
				
				for(Entry<Integer, MiniEventManager> e : EventManager.getInstance().getMiniEvents().get(type).entrySet())
				{
					if(e.getValue().getMode().isAllowed() && e.getValue().getMode().isRunning())
					{
						tb.append("<table width=270" + (bg ? " bgcolor=4f4f4f" : "") + "><tr>");
						
						tb.append("<td width=120><font color=ac9775>" + type.getHtmlTitle() + "</font></td><td width=130><font color=9f9f9f>" + e.getValue().getMode().getVisibleName() + "</font>" + "</td>");
						tb.append("<td width=45><a action=\"bypass admin_event_manage mini_viewinfo " + e.getKey() + " " + type.getAltTitle() + "\"><font color=9f9f9f>View</font></a></td>");
						tb.append("<td width=45><a action=\"bypass -h admin_event_manage mini_stopevent " + e.getKey() + " " + type.getAltTitle() + "\"><font color=9f9f9f>Stop</font></a></td>");
						tb.append("<td width=30><center>");
						
						try
						{
							int count = e.getValue().getRegisteredTeamsCount();
							tb.append("<font color=ac9775>" + count + "</font>");
						} 
						catch (Exception ex)
						{
							NexusLoader.debug("sent invalid bypass, event " + type.getAltTitle(), Level.WARNING);
							tb.append("<font color=B46F6B>N/A</font>");
						}
						
						tb.append("</center></td></tr></table>");
						bg = !bg;
					}
				}
			}
			
			String result = tb.toString();
			html = html.replaceAll("%miniEvents%", result);
		}
		
		String s = EventManager.getInstance().getMainEventManager().autoSchedulerEnabled() ? "<font color=74BE85>Enabled</font>" : "<font color=B46F6B>Disabled</font>";
		
		if(EventManager.getInstance().getMainEventManager().autoSchedulerPaused())
			s = "<font color=D1A261>Paused</font>";
		
		html = html.replaceAll("%auto_scheduler%", s);
		html = html.replaceAll("%auto_future%", EventManager.getInstance().getMainEventManager().getAutoSchedulerDelay());
		html = html.replaceAll("%auto_nextevent%", EventManager.getInstance().getMainEventManager().autoSchedulerEnabled() + "");
		html = html.replaceAll("%auto_nextmap%", EventManager.getInstance().getMainEventManager().autoSchedulerEnabled() + "");
		
		TextBuilder tb = new TextBuilder();
		
		if(EventManager.getInstance().getMainEventManager().getState() == State.IDLE)
		{
			tb.append("<tr>");
			tb.append("<td><font color=ac9887>Last event was:</font></td>");
			
			if(EventManager.getInstance().getMainEventManager().getLastEventOrder() == null)
				tb.append("<td><font color=9f9f9f>" + EventManager.getInstance().getMainEventManager().getLastEventTime() + "</font></td>");
			else
				tb.append("<td><font color=9f9f9f>" + EventManager.getInstance().getMainEventManager().getLastEventTime() + " (" + EventManager.getInstance().getMainEventManager().getLastEventOrder().getAltTitle() + ")</font></td>");
			
			tb.append("</tr>");
		}
		
		String params = tb.toString();
		
		html = html.replaceAll("%additionalParams%",  params);
		
		boolean isActive = EventManager.getInstance().getMainEventManager().getPvpZoneManager().isActive();
		
		String pvpzoneEnabled = isActive ? "<font color=54B733>Active</font>" : "<font color=B0393D>Inactive</font>";
		String pvpzoneTime = EventManager.getInstance().getMainEventManager().getPvpZoneManager().timeActive();
		String pvpzoneMap = EventManager.getInstance().getMainEventManager().getPvpZoneManager().getMapName();
		
		if(!isActive)
			html = html.replaceAll("%pvp_runstop%", "<button value=\"Start\" action=\"bypass -h admin_event_manage pvpzone_start\" width=78 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		else
			html = html.replaceAll("%pvp_runstop%", "<button value=\"Stop\" action=\"bypass -h admin_event_manage pvpzone_stop\" width=78 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		
		html = html.replaceAll("%pvp_enabled%", pvpzoneEnabled);
		html = html.replaceAll("%pvp_timeactive%", pvpzoneTime);
		html = html.replaceAll("%pvp_map%", pvpzoneMap);
		html = html.replaceAll("%pvp_state%", EventManager.getInstance().getMainEventManager().getPvpZoneManager().getState());
		
		boolean autoevents = EventManager.getInstance().getIsAutoeventsEnabled();
		
		if(autoevents)
			html = html.replaceAll("%autoevents%", "<font color=54B733>Enabled</font>");
		else
			html = html.replaceAll("%autoevents%", "<font color=B0393D>Disabled</font>");
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	public void watchCurrentMenu(PlayerEventInfo gm)
	{
		if(!NexusLoader.loaded()) return;
		
		if(EventManager.getInstance().getMainEventManager().getState() != State.RUNNING)
		{
			gm.sendMessage("Event can be only watched when in running state.");
			return;
		}
		
		String html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_watch_menu.htm");
		
		AbstractMainEvent event = EventManager.getInstance().getCurrentMainEvent();
		
		if(event.getInstances() == null)
		{
			gm.sendMessage("Instances for the event haven't been initialized yet, please try it in a few seconds.");
			return;
		}
		
		html = html.replaceAll("%event%", event == null ? "N/A" : event.getEventType().getAltTitle());
		html = html.replaceAll("%map%", event == null ? "N/A" : EventManager.getInstance().getMainEventManager().getMapName());
		
		TextBuilder tb = new TextBuilder();
		
		tb.append("<table>");
		
		for(InstanceData inst : event.getInstances())
		{
			tb.append("<tr>");
			tb.append("<td><button value=\"" + inst.getName() + " (" + inst.getId() + ")\" action=\"bypass -h admin_event_manage watch_current_event " + inst.getId() + "\" width=230 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr>");
		}
		
		tb.append("</table>");
		
		html = html.replaceAll("%instances%", tb.toString());
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	public void viewMiniEventInfo(PlayerEventInfo gm, int modeId, String type)
	{
		EventType eventType = EventType.getType(type);
		MiniEventManager event = EventManager.getInstance().getMiniEvent(eventType, modeId);
		
		if(eventType.isRegularEvent() || event == null)
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		_editingInfos.get(gm.getPlayersId()).activeEditingEvent = eventType;
		_editingInfos.get(gm.getPlayersId()).activeEventModeId = modeId;
		
		showModesMenu(gm);
	}
	
	public void stopMiniEvent(PlayerEventInfo gm, int modeId, String type)
	{
		EventType eventType = EventType.getType(type);
		MiniEventManager event = EventManager.getInstance().getMiniEvent(eventType, modeId);
		
		if(eventType.isRegularEvent() || event == null)
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		event.getMode().setAllowed(false);
		gm.sendMessage("Mode disabled.");
	}
	
	public void onBypass(PlayerEventInfo gm, String action)
	{
		try
		{
			if(!_editingInfos.containsKey(gm.getPlayersId()))
				_editingInfos.put(gm.getPlayersId(), new EditingInfo());

			if(action.equals("menu"))
			{
				showMenu(gm, false);
			}
			else if(action.equals("autoevents"))
			{
				if(EventManager.getInstance().getIsAutoeventsEnabled())
					EventManager.getInstance().setAutoevents(false);
				else
					EventManager.getInstance().setAutoevents(true);
				
				showMenu(gm, false);
			}

			else if(action.equals("reload_stats"))
			{
				EventStatsManager.getInstance().reload();
				gm.sendMessage("Statistics successfuly reloaded.");
			}
			else if(action.equals("debug_sign"))
			{
				gmDebug(gm);
			}
			else if(action.startsWith("pvpzone_start"))
			{
				if(EventManager.getInstance().getIsAutoeventsEnabled())
				{
					gm.sendMessage("Disable autoevents first.");
					return;
				}
				
				if(EventManager.getInstance().getMainEventManager().autoSchedulerEnabled() || EventManager.getInstance().getMainEventManager().getCurrent() != null)
				{
					gm.sendMessage("You must first disable the main event scheduler and/or the current main event.");
					return;
				}
				
				EventManager.getInstance().getMainEventManager().getPvpZoneManager().start(null);
				EventManagement.getInstance().showMenu(gm, false);
			}
			
			else if(action.startsWith("pvpzone_stop"))
			{
				if(EventManager.getInstance().getIsAutoeventsEnabled())
				{
					gm.sendMessage("Disable autoevents first.");
					return;
				}
				
				EventManager.getInstance().getMainEventManager().getPvpZoneManager().stopByGm();
				EventManagement.getInstance().showMenu(gm, false);
			}
			
			else if(action.startsWith("pvpzone_switchmap"))
			{
				if(EventManager.getInstance().getMainEventManager().getPvpZoneManager().isActive())
					gm.sendMessage("Cannot change map if the zone is active. Stop it first.");
					
				EventManager.getInstance().getMainEventManager().getPvpZoneManager().switchMap();
				EventManagement.getInstance().showMenu(gm, false);
			}
			else if(action.startsWith("tournament_"))
			{
				String action2 = action.substring(11);

				if(action2.startsWith("menu"))
				{
					EventType event = _editingInfos.get(gm.getPlayersId()).activeEditingEvent;
					int modeId = _editingInfos.get(gm.getPlayersId()).activeEventModeId;

					MiniEventManager eventManager = EventManager.getInstance().getMiniEvent(event, modeId);

					if(eventManager != null)
					{
						TournamentManager.setTournamentEvent(gm, eventManager);

						//TODO show menu
						//showHostTournamentMenu(gm);
					}
				}
				else if(action2.startsWith("start"))
				{
					TournamentManager.startTournament(gm);

					//showHostTournamentMenu(gm);
				}
				else if(action2.startsWith("skipreg"))
				{
					TournamentManager.skipRegistration(gm);

					//showHostTournamentMenu(gm);
				}
				else if(action2.startsWith("stop"))
				{
					TournamentManager.stopTournament(gm);

					//showHostTournamentMenu(gm);
				}
				else if(action2.startsWith("set_regdelay"))
				{
					int regDelay = Integer.parseInt(action2.substring(13));

					TournamentManager.setTimeForRegistration(gm, regDelay);
					//showHostTournamentMenu(gm);
				}
				else if(action2.startsWith("set_minteams"))
				{
					int teams = Integer.parseInt(action2.substring(13));

					TournamentManager.setTournamentTeamsCount(gm, teams, true);
					//showHostTournamentMenu(gm);
				}
				else if(action2.startsWith("set_maxteams"))
				{
					int teams = Integer.parseInt(action2.substring(13));

					TournamentManager.setTournamentTeamsCount(gm, teams, false);

					//showHostTournamentMenu(gm);
				}
			}
			
			else if(action.equals("regall"))
			{
				if(EventManager.getInstance().getMainEventManager().getState() != State.REGISTERING)
				{
					gm.sendMessage("The event must be in registering phase.");
					return;
				}
				
				int size = 0;
				for(PlayerEventInfo player : CallBack.getInstance().getOut().getAllPlayers())
				{
					if(EventManager.getInstance().getMainEventManager().registerPlayer(player))
					{
						player.screenMessage("GM has registered you to the event.", "NexusEvents", true);
						size ++;
					}
				}
				gm.sendMessage(size + " players have been registered to the event.");
			}
			else if(action.equals("back"))
			{
				String back = getGoBackPage(gm);
				if(back != null)
					onBypass(gm, back);
			}
			else if(action.equals("menu_mini"))
			{
				showMenu(gm, true);
			}
			else if(action.startsWith("new_event_runtime_edit"))
			{
				showNewEventMenu(gm, EventType.getType(action.substring(10)), true);
			}
			else if(action.startsWith("new_event"))
			{
				if(EventManager.getInstance().getIsAutoeventsEnabled())
				{
					gm.sendMessage("Disable autoevents first.");
					return;
				}
				
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				if(st.hasMoreTokens())
					showNewEventMenu(gm, EventType.getType(action.substring(10)), false);
				else
					showNewEventMenu(gm, null, false);
			}
			else if(action.startsWith("event_start"))
			{
				String parameters = action.substring(12);
				
				String[] splitted = parameters.split(";");
				
				EventType event = _editingInfos.get(gm.getPlayersId()).activeEditingEvent;
				
				int regTime = Integer.parseInt(splitted[0].trim());
				String mapName = splitted[1].trim();
				String npcLoc = splitted[2].trim();
				int runTime = Integer.parseInt(splitted[3].trim());
				
				EventManager.getInstance().getMainEventManager().startEvent(gm, event, regTime, mapName, npcLoc, runTime);
			}
			else if(action.startsWith("abort_current"))
			{
				EventManager.getInstance().getMainEventManager().abort(gm, false);
				showMenu(gm, false);
			}
			else if(action.startsWith("skip_current"))
			{
				EventManager.getInstance().getMainEventManager().skipDelay(gm);
			}
			else if(action.startsWith("watch_current_menu"))
			{
				watchCurrentMenu(gm);
			}
			else if(action.startsWith("watch_current_event"))
			{
				EventManager.getInstance().getMainEventManager().watchEvent(gm, Integer.parseInt(action.substring(20)));
			}
			else if(action.startsWith("watch_current_stop"))
			{
				EventManager.getInstance().getMainEventManager().stopWatching(gm);
			}
			else if(action.startsWith("abort_auto_scheduler"))
			{
				EventManager.getInstance().getMainEventManager().abortAutoScheduler(gm);
				showMenu(gm, false);
			}
			else if(action.startsWith("pause_auto_scheduler"))
			{
				if(EventManager.getInstance().getIsAutoeventsEnabled())
				{
					gm.sendMessage("Disable autoevents first.");
					return;
				}
				
				if(!EventManager.getInstance().getMainEventManager().autoSchedulerPaused())
					EventManager.getInstance().getMainEventManager().pauseAutoScheduler(gm);
				else
					EventManager.getInstance().getMainEventManager().unpauseAutoScheduler(gm, true);
				
				showMenu(gm, false);
			}
			else if(action.startsWith("restart_auto_scheduler"))
			{
				if(EventManager.getInstance().getIsAutoeventsEnabled())
				{
					gm.sendMessage("Disable autoevents first.");
					return;
				}
				
				EventManager.getInstance().getMainEventManager().restartAutoScheduler(gm);
				showMenu(gm, false);
			}
			else if(action.startsWith("config_auto_scheduler"))
			{
				showGlobalConfigMenu(gm, "Scheduler", 0);
			}
			else if(action.startsWith("abort_current"))
			{
				EventManager.getInstance().getMainEventManager().abort(gm, false);
			}
			else if(action.startsWith("skip_current"))
			{
				EventManager.getInstance().getMainEventManager().skipDelay(gm);
			}
			else if(action.startsWith("globalconfig_menu"))
			{
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				String configType = st.nextToken();
				int page = Integer.valueOf(st.nextToken());
				
				showGlobalConfigMenu(gm, configType, page);
			}
			else if(action.startsWith("globalconfig_showinfo"))
			{
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				String configType = st.nextToken();
				int page = Integer.valueOf(st.nextToken());
				String key = st.nextToken();
				
				if(_editingInfos.get(gm.getPlayersId()).activeGlobalConfigKeyShown.equals(key))
					_editingInfos.get(gm.getPlayersId()).activeGlobalConfigKeyShown = "";
				else
					_editingInfos.get(gm.getPlayersId()).activeGlobalConfigKeyShown = key;
				 
				showGlobalConfigMenu(gm, configType, page);
			}
			else if(action.startsWith("globalconfig_edit"))
			{
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				String key = st.nextToken();
				
				showGlobalConfigEditation(gm, key);
			}
			else if(action.startsWith("globalconfig_setvalue"))
			{
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				String key = st.nextToken();
				String value = "";
				
				while(st.hasMoreTokens())
				{
					value = value + st.nextToken();
					
					if(st.hasMoreTokens())
						value += " ";
				}
				
				if(value.length() == 0)
				{
					gm.sendMessage("Wrong value.");
					return;
				}
				
				setGlobalConfigValue(gm, key, value);
			}
			else if(action.startsWith("eventlang_menu"))
			{
				showLanguageSelectMenu(gm);
			}
			else if(action.startsWith("set_language"))
			{
				String lang = action.substring(13);
				LanguageEngine.setLanguage(lang);
				
				gm.sendMessage("Language successfully set to: " + lang);
				
				showLanguageSelectMenu(gm);
			}
			else if(action.startsWith("reload_global_configs"))
			{
				EventConfig.getInstance().loadGlobalConfigs();
				showGlobalConfigMenu(gm, "Core", 1);
				
				gm.sendMessage("Global Configs successfully reloaded from database.");
			}
			else if(action.startsWith("reload_configs"))
			{
				Configurable event = EventManager.getInstance().getEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent);
				if(event != null)
				{
					event.clearConfigs();
					event.loadConfigs();
				}
				
				showConfigsMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle(), _editingInfos.get(gm.getPlayersId()).currentConfigModelPage);
			}
			else if(action.startsWith("expand_configmodel"))
			{
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				String category = st.nextToken();
				String key = st.nextToken();
				
				_editingInfos.get(gm.getPlayersId()).currentConfigModelShownCategory = category;
				
				if(_editingInfos.get(gm.getPlayersId()).currentConfigModelShown.equals(key))
					_editingInfos.get(gm.getPlayersId()).currentConfigModelShown = "";
				else
					_editingInfos.get(gm.getPlayersId()).currentConfigModelShown = key;
				
				if(category.equals("InstanceType"))
				{
					showEditMainInstanceTypeWindow(gm, _editingInfos.get(gm.getPlayersId()).activeEdittingMainInstanceType);
				}
				else if(category.equals("Event"))
				{
					showConfigsMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle(), _editingInfos.get(gm.getPlayersId()).currentConfigModelPage);
				}
				else if(category.equals("MapConfig"))
				{
					showMapEditationEvents(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap);
				}
			}
			else if(action.startsWith("eventorder_menu"))
			{
				showEventOrderMenu(gm, null);
			}
			else if(action.startsWith("eventorder_move"))
			{
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				String direction = st.nextToken();
				String event = st.nextToken();
				
				moveEventInOrder(gm, direction, event);
			}
			else if(action.startsWith("mini_"))
			{
				String action2 = action.substring(5);
				
				if(action2.startsWith("menu"))
				{
					showMiniEventMenu(gm);
					setGoBackPage(gm, action);
				}
				else if(action2.startsWith("edit_event"))
				{
					StringTokenizer st = new StringTokenizer(action2);
					st.nextToken();
					
					int modeId = Integer.parseInt(st.nextToken());
					String editPage = st.hasMoreTokens() ? st.nextToken() : null;
					
					String event = st.hasMoreTokens() ? st.nextToken() : null;
					
					if(event == null)
						showEditMiniEventMenu(gm, modeId, editPage);
					else
					{
						EventType type = EventType.getType(event);
						showEditMiniEventMenu(gm, type.getId(), editPage);
					}
					
					setGoBackPage(gm, action);
				}
				else if(action2.startsWith("viewinfo"))
				{
					StringTokenizer st = new StringTokenizer(action2);
					st.nextToken();
					
					int mode = Integer.parseInt(st.nextToken());
					String event = st.nextToken();
					
					viewMiniEventInfo(gm, mode, event);
				}
				else if(action2.startsWith("stopevent"))
				{
					StringTokenizer st = new StringTokenizer(action2);
					st.nextToken();
					
					int mode = Integer.parseInt(st.nextToken());
					String event = st.nextToken();
					
					stopMiniEvent(gm, mode, event);
				}
				else if(action2.startsWith("set_active_mode"))
				{
					StringTokenizer st = new StringTokenizer(action2);
					st.nextToken();
					
					if(_editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
					{
						gm.sendMessage("Event Modes engine is not implemented for Main events yet.");
						return;
					}
					
					String page = st.nextToken();
					String mode = st.nextToken();
					setModeId(gm, mode);
					
					if(page.equals("menu"))
						showModesMenu(gm);
					else if(page.equals("reward"))
						showRewardsEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle(), _editingInfos.get(gm.getPlayersId()).activeEditingRewardPos, _editingInfos.get(gm.getPlayersId()).activeEditingRewardParam);
					else if(page.equals("match"))
						showMatches(gm);
				}
				else if(action2.startsWith("modes_menu_compact"))
				{
					showCompactModesMenu(gm);
				}
				else if(action2.startsWith("modes_menu"))
				{
					showModesMenu(gm);
				}
				else if(action2.startsWith("edit_modes_menu"))
				{
					int modeId;
					StringTokenizer st = new StringTokenizer(action2);
					st.nextToken();
					
					if(st.hasMoreTokens())
						modeId = Integer.parseInt(st.nextToken());
					else
						modeId = _editingInfos.get(gm.getPlayersId()).activeEventModeId;
					
					showEditModeWindow(gm, modeId);
				}
				else if(action2.startsWith("edit_modes_maps_menu"))
				{
					showEditModeMaps(gm);
				}
				else if(action2.startsWith("edit_modes_map"))
				{
					allowDisallowMapMode(gm, action2.substring(15));
					showEditModeMaps(gm);
				}
				else if(action2.startsWith("edit_modes_new"))
				{
					createNewModeMenu(gm);
				}
				else if(action2.startsWith("create_mode"))
				{
					createNewMode(gm, action2.substring(12));
				}
				else if(action2.startsWith("edit_modes_delete"))
				{
					deleteMode(gm);
					showModesMenu(gm);
				}
				else if(action2.startsWith("edit_modes_enabledisable"))
				{
					StringTokenizer st = new StringTokenizer(action2);
					st.nextToken();
					
					boolean b = false;
					if(st.hasMoreTokens())
					{
						_editingInfos.get(gm.getPlayersId()).activeEventModeId = Integer.parseInt(st.nextToken());
						b = true;
					}
					
					enableDisableMode(gm);
					
					if(!b)
						showEditModeWindow(gm);
					else
						showCompactModesMenu(gm);
				}
				else if(action2.startsWith("edit_modes_generatefile"))
				{
					createRegFile(gm);
					showEditModeWindow(gm);
				}
				else if(action2.startsWith("edit_modes_clear"))
				{
					clearEvent(gm);
					showEditModeWindow(gm);
				}
				else if(action2.startsWith("edit_modes_set_name"))
				{
					setModeName(gm, action2.substring(20), false);
					showEditModeWindow(gm);
				}
				else if(action2.startsWith("edit_modes_vis_name"))
				{
					setModeName(gm, action2.substring(20), true);
					showEditModeWindow(gm);
				}
				else if(action2.startsWith("edit_modes_set_npcid"))
				{
					setModeNpcId(gm, action2.substring(21));
					showEditModeWindow(gm);
				}
				else if(action2.startsWith("edit_modes_set_info"))
				{
					showEditModeWindow(gm);
				}
				else if(action2.startsWith("edit_modes_scheduler_menu"))
				{
					showEditModeSchedulerMenu(gm);
				}
				else if(action2.startsWith("edit_modes_scheduler_edit"))
				{
					showEditModeSchedulerTime(gm, Integer.parseInt(action2.substring(26)));
				}
				else if(action2.startsWith("edit_modes_scheduler_removeday"))
				{
					schedulerRemoveDay(gm, action2.substring(31));
					showEditModeSchedulerTime(gm);
				}
				else if(action2.startsWith("edit_modes_scheduler_addday"))
				{
					schedulerAddDay(gm, action2.substring(28));
					showEditModeSchedulerTime(gm);
				}
				else if(action2.startsWith("edit_modes_scheduler_set_from"))
				{
					schedulerSetHour(gm, action2.substring(30), true);
					showEditModeSchedulerTime(gm);
				}
				else if(action2.startsWith("edit_modes_scheduler_set_to"))
				{
					schedulerSetHour(gm, action2.substring(28), false);
					showEditModeSchedulerTime(gm);
				}
				else if(action2.startsWith("edit_modes_scheduler_removetime"))
				{
					scheduleRemoveTime(gm);
					showEditModeSchedulerMenu(gm);
				}
				else if(action2.startsWith("edit_modes_scheduler_newtime"))
				{
					schedulerAddNewTime(gm);
					showEditModeSchedulerTime(gm);
				}
				else if(action2.startsWith("edit_modes_scheduler_refresh1"))
				{
					schedulerRefresh(gm);
					showEditModeSchedulerTime(gm);
				}
				else if(action2.startsWith("edit_modes_scheduler_refresh2"))
				{
					schedulerRefresh(gm);
					showEditModeSchedulerMenu(gm);
				}
				else if(action2.startsWith("edit_feature_showinfo"))
				{
					setActiveShowedFeature(gm, action2.substring(22));
					showNewMiniEventFeatureMenu(gm, "Default");
				}
				else if(action2.startsWith("edit_feature_setcategory"))
				{
					setActiveFeatureCategory(gm, action2.substring(25));
					showNewMiniEventFeatureMenu(gm, "Default");
				}
				else if(action2.startsWith("edit_feature_add"))
				{
					StringTokenizer st = new StringTokenizer(action2);
					st.nextToken();
					
					String type = st.hasMoreTokens() ? st.nextToken() : "Default";
					
					showNewMiniEventFeatureMenu(gm, type);
				}
				else if(action2.startsWith("edit_feature_remove"))
				{
					StringTokenizer st = new StringTokenizer(action2);
					st.nextToken();
					
					String featureName = st.nextToken();
					
					removeFeature(gm, featureName);
					showModesMenu(gm);
				}
				else if(action2.startsWith("edit_featureconfig_set "))
				{
					StringTokenizer st = new StringTokenizer(action2);
					st.nextToken();
					
					String featureName = st.nextToken();
					String key = st.nextToken();
					
					TextBuilder tb = new TextBuilder();
					
					while(st.hasMoreTokens())
					{
						tb.append(st.nextToken() + " ");
					}
					
					String value = tb.toString();
					
					setFeatureConfigValue(gm, featureName, key, value.substring(0, value.length() - 1));
					showEditFeatureConfig(gm, featureName, key);
				}
				else if(action2.startsWith("edit_featureconfig "))
				{
					StringTokenizer st = new StringTokenizer(action2);
					st.nextToken();
					
					showEditFeatureConfig(gm, st.nextToken(), st.nextToken());
				}
				else if(action2.startsWith("edit_feature "))
				{
					showEditFeature(gm, action2.substring(13));
				}
				else if(action2.startsWith("show_matches"))
				{
					showMatches(gm);
				}
				else if(action2.startsWith("edit_match"))
				{
					editMatch(gm, Integer.parseInt(action2.substring(11)));
				}
				else if(action2.startsWith("watch_match"))
				{
					watchMatch(gm);
					editMatch(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMatch);
				}
				else if(action2.startsWith("abort_match"))
				{
					abortMatch(gm);
					showMatches(gm);
				}
				else if(action2.startsWith("show_team_members"))
				{
					showTeamMembers(gm, Integer.parseInt(action2.substring(18)));
				}
				else if(action2.startsWith("manual_match"))
				{
					String action3 = action2.substring(13);
					
					if(action3.startsWith("menu"))
					{
						showManualMatchMenu(gm);
					}
					else if(action3.startsWith("add_team"))
					{
						if(gm.getTarget() == null || !(gm.getTarget().isPlayer() || gm.getTarget().isSummon()))
						{
							gm.sendMessage("You must target player or his servitor which you want to add into the event.");
							return;
						}
						
						if(gm.getTarget().getEventInfo() == null || gm.getTarget().getEventInfo().isRegistered())
						{
							gm.sendMessage("Your target is already registered to another event.");
							return;
						}
						
						int teamId = Integer.parseInt(action3.substring(9));
						
						if(_editingInfos.get(gm.getPlayersId()).manualMatchPlayers.get(teamId) == null)
							_editingInfos.get(gm.getPlayersId()).manualMatchPlayers.put(teamId, new FastList<PlayerEventInfo>());
						
						_editingInfos.get(gm.getPlayersId()).manualMatchPlayers.get(teamId).add(gm.getTarget().getEventInfo());
						
						gm.sendMessage("Done. Player " + gm.getTarget().getName() + " has been added.");
						
						showManualMatchMenu(gm);
					}
					else if(action3.startsWith("rem_team"))
					{
						int id;
						try
						{
							id = Integer.parseInt(action3.substring(9));
						} catch (Exception e)
						{
							id = 0;
						}
						
						if(id == 0 && (gm.getTarget() == null || gm.getTarget().isPlayer()))
						{
							gm.sendMessage("You must target player which you want to remove from the event.");
							return;
						}
						
						for(Map.Entry<Integer, FastList<PlayerEventInfo>> e : _editingInfos.get(gm.getPlayersId()).manualMatchPlayers.entrySet())
						{
							for(PlayerEventInfo player : e.getValue())
							{
								if(id == 0 && player.getPlayersId() == gm.getTarget().getObjectId() || player.getPlayersId() == id)
								{
									_editingInfos.get(gm.getPlayersId()).manualMatchPlayers.get(e.getKey()).remove(player);
									gm.sendMessage("Done. Player " + player.getPlayersName() + " has been removed successfully.");
									showManualMatchMenu(gm);
									return;
								}
							}
						}

						gm.sendMessage("Yout target is not in the list of went offline.");
						return;
					}
					else if(action3.startsWith("set_map_menu"))
					{
						showManualMatchSetMapMenu(gm);
					}
					else if(action3.startsWith("set_map"))
					{
						setManualMatchMap(gm, Integer.parseInt(action3.substring(8)));
						showManualMatchMenu(gm);
					}
					else if(action3.startsWith("start"))
					{
						startManualMatch(gm);
					}
					else if(action3.startsWith("abort"))
					{
						// manual match
					}
					else if(action3.startsWith("clear"))
					{
						_editingInfos.get(gm.getPlayersId()).manualMatchPlayers.clear();
						showManualMatchMenu(gm);
					}
				}
			}
			else if(action.startsWith("set_aviable"))
			{
				EventType type = _editingInfos.get(gm.getPlayersId()).activeEditingEvent;
				
				if(EventConfig.getInstance().isEventAllowed(type))
					removeAviableEvent(gm);
				else
					addAviableEvent(gm);
				
				if(type.isRegularEvent())
					showEditEventMenu(gm, type.getAltTitle(), _editingInfos.get(gm.getPlayersId()).activeEventEditingSection);
				else 
					showEditMiniEventMenu(gm, type, _editingInfos.get(gm.getPlayersId()).activeEventEditingSection);
			}
			else if(action.startsWith("event_configs_menu_page"))
			{
				int page = Integer.parseInt(action.substring(24));
				_editingInfos.get(gm.getPlayersId()).currentConfigModelPage = page;
				
				showConfigsMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle(), _editingInfos.get(gm.getPlayersId()).currentConfigModelPage);
			}
			else if(action.startsWith("show_configs_menu"))
			{
				_editingInfos.get(gm.getPlayersId()).currentConfigModelPage = 1;
				_editingInfos.get(gm.getPlayersId()).activeEditingConfigCategory = "General";
				
				showConfigsMenu(gm, action.substring(18), _editingInfos.get(gm.getPlayersId()).currentConfigModelPage);
			}
			else if(action.startsWith("show_configs_category"))
			{
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				String cat = st.nextToken();
				_editingInfos.get(gm.getPlayersId()).activeEditingConfigCategory = cat;
				
				showConfigsMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle(), _editingInfos.get(gm.getPlayersId()).currentConfigModelPage);
			}
			else if(action.startsWith("show_config"))
			{
				String key = action.substring(12);
				showConfig(gm, key);
			}
			else if(action.startsWith("set_config"))
			{
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				String key = st.nextToken();
				String value;
				if(st.hasMoreTokens())
					value = st.nextToken();
				else value = "";
				
				setConfig(gm, key, value, false);
				showConfigsMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle(), _editingInfos.get(gm.getPlayersId()).currentConfigModelPage);
			}
			else if(action.startsWith("remove_multiadd_config_value"))
			{
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				int index = Integer.parseInt(st.nextToken());
				String category = _editingInfos.get(gm.getPlayersId()).currentConfigModelShownCategory;
				String config = _editingInfos.get(gm.getPlayersId()).currentConfigModelShown;
				
				if(category.equals("InstanceType"))
				{
					AbstractMainEvent event = EventManager.getInstance().getMainEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent);
					if(event == null)
					{
						gm.sendMessage("This event doesn't exist.");
						return;
					}
					
					MainEventInstanceType instance = event.getInstanceType(_editingInfos.get(gm.getPlayersId()).activeEdittingMainInstanceType);
					instance.getConfigs().get(config).removeMultiAddValueIndex(index);
					
					MainEventInstanceTypeManager.getInstance().updateInstanceType(instance);
					showEditMainInstanceTypeWindow(gm, _editingInfos.get(gm.getPlayersId()).activeEdittingMainInstanceType);
				}
				else if(category.equals("Event"))
				{
					NexusLoader.debug("removing on index " + index + " of config " + config);
					EventConfig.getInstance().removeConfigMultiAddValue(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, config, index);
					showConfigsMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle(), _editingInfos.get(gm.getPlayersId()).currentConfigModelPage);
				}
				else if(category.equals("MapConfig"))
				{
					EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
					
					if(map != null)
					{
						EventConfig.getInstance().removeMapConfigMultiAddValue(map, _editingInfos.get(gm.getPlayersId()).activeEditingEvent, config, index);
						
						showMapEditationEvents(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap);
					}
					else
						gm.sendMessage("This map doesn't exist.");
				}
			}
			else if(action.startsWith("addto_config"))
			{
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				String key = st.nextToken();
				String value = st.nextToken();
				
				setConfig(gm, key, value, true);
				showConfigsMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle(), _editingInfos.get(gm.getPlayersId()).currentConfigModelPage);
			}
			else if(action.startsWith("event_gm_message_all "))
			{
				eventGmMessage(gm, action.substring(21), false);
				eventGmMessageMenu(gm);
			}
			else if(action.startsWith("event_gm_message_menu"))
			{
				eventGmMessageMenu(gm);
			}
			else if(action.startsWith("edit_main_instance_type "))
			{
				int id = Integer.parseInt(action.substring(24));
				showEditMainInstanceTypeWindow(gm, id);
			}
			else if(action.startsWith("delete_main_instance_type"))
			{
				int id = _editingInfos.get(gm.getPlayersId()).activeEdittingMainInstanceType;
				deleteMainInstanceType(gm, id);
				showEditEventInstancesMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent);
			}
			else if(action.startsWith("edit_main_instance_type_set_name"))
			{
				String name = action.substring(33);
				int id = _editingInfos.get(gm.getPlayersId()).activeEdittingMainInstanceType;
				editMainInstanceType(gm, id, "set_name", name, false);
				showEditMainInstanceTypeWindow(gm, id);
			}
			else if(action.startsWith("create_new_main_instance"))
			{
				showEditMainInstanceTypeWindow(gm, -1);
			}
			else if(action.startsWith("edit_main_instance_type_set_config"))
			{
				int id = _editingInfos.get(gm.getPlayersId()).activeEdittingMainInstanceType;
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				String name = st.nextToken();
				String value;
				if(st.hasMoreTokens())
					value = st.nextToken();
				else value = "";
				editMainInstanceType(gm, id, name, value, false);
				
				showEditMainInstanceTypeWindow(gm, id);
			}
			else if(action.startsWith("edit_main_instance_type_addto_config"))
			{
				int id = _editingInfos.get(gm.getPlayersId()).activeEdittingMainInstanceType;
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				String name = st.nextToken();
				String value = st.nextToken();
				editMainInstanceType(gm, id, name, value, true);
				
				showEditMainInstanceTypeWindow(gm, id);
			}
			else if(action.startsWith("edit_event "))
			{
				StringTokenizer st = new StringTokenizer(action);
				st.nextToken();
				
				String event = st.hasMoreTokens() ? st.nextToken() : null;
				String editPage = st.hasMoreTokens() ? st.nextToken() : null;
				
				showEditEventMenu(gm, event, editPage);
				
				setGoBackPage(gm, action);
			}
			else if(action.startsWith("edit_events_menu"))
			{
				showEditEventsMenu(gm);
				
				setGoBackPage(gm, action);
			}
			
			else if(action.startsWith("tournament_menu "))
			{
			}
			else
			{
				// ***************************************************************************
				// event maps
				// ***************************************************************************
				
				if(action.startsWith("edit_event_map"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					int mapId =  Integer.parseInt(st.nextToken());
					int page = Integer.valueOf(st.nextToken());
					
					if(_editingInfos.get(gm.getPlayersId()).activeEditingMap != mapId)
						_editingInfos.get(gm.getPlayersId()).activeEditingMapPage = "Info";
					
					showMapEditation(gm, mapId, page, null);
				}
				else if(action.startsWith("edit_map_add_event_menu"))
				{
					showMapEditationAddEvent(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap);
				}
				else if(action.startsWith("set_active_editing_map_event"))
				{
					_editingInfos.get(gm.getPlayersId()).activeEditingMapEvent = EventType.getType(action.substring(29));
					showMapEditationEvents(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap);
				}
				else if(action.startsWith("edit_map_page"))
				{
					String page = action.substring(14);
					
					_editingInfos.get(gm.getPlayersId()).activeEditingMapPage = page;
					showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
				}
				else if(action.startsWith("show_map_status"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					showMapStatus(gm, Integer.valueOf(st.nextToken()), st.nextToken());
				}
				else if(action.startsWith("config_event_map"))
				{
					showConfigMenu(gm, action.substring(17));
				}
				else if(action.startsWith("set_map_config"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					String type = st.nextToken();
					String key = st.nextToken();
					String value;
					if(st.hasMoreElements())
						value = st.nextToken();
					else
						value = "";
					
					setMapConfig(gm, type, key, value, false);
					showMapEditationEvents(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap);
				}
				else if(action.startsWith("addto_map_config"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					String type = st.nextToken();
					String key = st.nextToken();
					String value;
					if(st.hasMoreTokens())
						value = st.nextToken();
					else value = "";
					
					setMapConfig(gm, type, key, value, true);
					showMapEditationEvents(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap);
				}
				else if(action.startsWith("expand_spawn_info"))
				{
					int id = Integer.parseInt(action.substring(18));
					
					if(_editingInfos.get(gm.getPlayersId()).activeEditingSpawn == id)
					{
						_editingInfos.get(gm.getPlayersId()).activeEditingSpawn = 0;
						gm.removeRadarAllMarkers();
					}
					else
					{
						_editingInfos.get(gm.getPlayersId()).activeEditingSpawn = id;
						showExpandSpawnEffect(gm, id);
					}
					
					showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
				}
				else if(action.startsWith("filter_event_spawns"))
				{
					filterSpawns(gm, action.substring(20));
					showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
				}
				else if(action.startsWith("sort_map_spawns"))
				{
					sortSpawns(gm, action.substring(16));
					showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
				}
				else if(action.startsWith("remove_event_map"))
				{
					removeMap(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap);
					showMiniEventMenu(gm);
				}
				else if(action.startsWith("create_event_map"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					String predefinedEvent = null;
					if(st.hasMoreTokens())
						predefinedEvent = st.nextToken();
					
					EventType type = predefinedEvent == null ? null : EventType.getType(predefinedEvent);
					
					showMapEditation(gm, 0, 0, type);
				}
				// ***
				else if(action.startsWith("set_map_name "))
				{
					setMapName(gm, action.substring(13));
					showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
				}
				else if(action.startsWith("set_map_desc "))
				{
					setMapDesc(gm, action.substring(13));
					showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
				}
				else if(action.startsWith("save_map"))
				{
					boolean b = saveMap(gm);
					
					String back = getGoBackPage(gm);
					if(back != null)
					{
						onBypass(gm, back);
					}
					else
					{
						if(b)
							showEditEventsMenu(gm);
						else
							showMiniEventMenu(gm);
					}
				}
				else if(action.startsWith("add_event_to_map "))
				{
					addAviableEvent(gm, action.substring(17));
					showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
				}
				else if(action.startsWith("remove_event_from_map"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					String event;
					boolean b;
					if(st.hasMoreTokens())
					{
						event = st.nextToken();
						b = true;
					}
					else
					{
						event = _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle();
						b = false;
					}
					
					removeAviableEvent(gm, event);
					saveMap(gm);
					
					if(b)
						showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
					else
					{
						if(EventType.getType(event).isRegularEvent())
							showEditEventsMenu(gm);
						else
							showMiniEventMenu(gm);
					}
				}
				else if(action.startsWith("filter_menu"))
				{
					showFilterMenu(gm);
				}
				
				// ***************************************************************************
				// rewards 
				// ***************************************************************************
				
				else if(action.startsWith("edit_event_reward_menu"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					String event = st.nextToken();
					RewardPosition pos;
					String parameter = null;
					
					if(st.hasMoreTokens())
						pos = RewardPosition.getPosition(st.nextToken());
					else
						pos = RewardPosition.None;
					
					if(st.hasMoreTokens())
					{
						parameter = st.nextToken();
					}
					
					showRewardsEditation(gm, event, pos, parameter);
				}
				else if(action.startsWith("edit_event_reward "))
				{
					showRewardEditation(gm, Integer.parseInt(action.substring(18)));
				}
				else if(action.startsWith("edit_event_reward_new"))
				{
					addEventReward(gm);
				}
				else if(action.startsWith("show_add_position_window_help"))
				{
					showAddPositionToRewardedWindowHelp(gm);
				}
				else if(action.startsWith("show_add_position_window"))
				{
					showAddPositionToRewardedWindow(gm);
				}
				else if(action.startsWith("show_edit_position_window"))
				{
					
				}
				else if(action.startsWith("add_rewarded_position"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					RewardPosition pos = RewardPosition.getPosition(st.nextToken());
					
					String param;
					if(st.hasMoreTokens())
					{
						param = st.nextToken();
						
						if(st.hasMoreTokens())
							param += "-" + st.nextToken();
					}
					else 
						param = null;
					
					
					addPositionToRewarded(gm, pos, param);
				}
				else if(action.startsWith("remove_rewarded_position"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					RewardPosition pos = RewardPosition.getPosition(st.nextToken());
					if(pos == null)
					{
						gm.sendMessage("Select a position first from the list above.");
						showRewardsEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.toString(), _editingInfos.get(gm.getPlayersId()).activeEditingRewardPos, _editingInfos.get(gm.getPlayersId()).activeEditingRewardParam);
						return;
					}
					
					String param = (st.hasMoreTokens() ? st.nextToken() : null);
					
					removePositionFromRewarded(gm, pos, param);
				}
				else if(action.startsWith("edit_event_reward_"))
				{
					String s = action.substring(18);
					
					if(s.startsWith("remove"))
					{
						removeReward(gm);
						showRewardsEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.toString(), _editingInfos.get(gm.getPlayersId()).activeEditingRewardPos, _editingInfos.get(gm.getPlayersId()).activeEditingRewardParam);
					}
					else if(s.startsWith("save"))
					{
						saveReward(gm);
						showRewardsEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.toString(), _editingInfos.get(gm.getPlayersId()).activeEditingRewardPos, _editingInfos.get(gm.getPlayersId()).activeEditingRewardParam);
					}
					else if(s.startsWith("set_item_id"))
					{
						editReward(gm, s.substring(12), "set_item_id");
						showRewardEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingReward);
					}
					else if(s.startsWith("set_min"))
					{
						editReward(gm, s.substring(8), "set_min");
						showRewardEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingReward);
					}
					else if(s.startsWith("set_max"))
					{
						editReward(gm, s.substring(8), "set_max");
						showRewardEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingReward);
					}
					else if(s.startsWith("set_chance"))
					{
						editReward(gm, s.substring(11), "set_chance");
						showRewardEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingReward);
					}
				}
				
				// ***************************************************************************
				// spawns
				// ***************************************************************************

				else if(action.startsWith("spawn_type_info_menu"))
				{
					showSpawnTypeInfoMenu(gm);
				}
				else if(action.startsWith("spawn_type_info_event"))
				{
					showSpawnTypeInfoEvent(gm, action.substring(22));
				}
				else if(action.startsWith("remove_spawn "))
				{
					removeSpawn(gm, Integer.parseInt(action.substring(13)));
					showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
				}
				else if(action.startsWith("teleport_spawn "))
				{
					teleportToSpawn(gm, Integer.parseInt(action.substring(15)));
					showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
				}
				else if(action.startsWith("show_spawn "))
				{
					showSpawn(gm, Integer.parseInt(action.substring(11)), false, false, false);
					showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
				}
				else if(action.startsWith("show_all_spawns"))
				{
					showAllSpawns(gm);
					showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
				}
				else if(action.startsWith("new_spawn"))
				{
					int id = newSpawn(gm);
					
					if(_editingInfos.get(gm.getPlayersId()).activeEditingEvent == EventType.TreasureHunt || _editingInfos.get(gm.getPlayersId()).activeEditingEvent == EventType.TreasureHuntPvp)
						if(id > 0) 
							showSpawn(gm, id, false, false, false);
					
					showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
				}
				else if(action.startsWith("edit_spawn_"))
				{
					String s = action.substring(11);
					
					if(s.startsWith("menu"))
					{
						showEditSpawnMenu(gm, Integer.parseInt(action.substring(16)));
					}
					else if(s.startsWith("save_spawn"))
					{
						saveSpawn(gm);
						showMapEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingMap, _editingInfos.get(gm.getPlayersId()).activeEditingPage, null);
					}
					else if(s.startsWith("set_id"))
					{
						editSpawn(gm, s.substring(7), "set_id");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_team"))
					{
						editSpawn(gm, s.substring(9), "set_team");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_x"))
					{
						editSpawn(gm, s.substring(6), "set_x");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_y"))
					{
						editSpawn(gm, s.substring(6), "set_y");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_z"))
					{
						editSpawn(gm, s.substring(6), "set_z");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_wawenumber"))
					{
						editSpawn(gm, s.substring(15), "set_wawenumber");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_mobid"))
					{
						editSpawn(gm, s.substring(10), "set_mobid");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_mobammount"))
					{
						editSpawn(gm, s.substring(15), "set_mobammount");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_dooraction_init"))
					{
						editSpawn(gm, s.substring(20), "set_dooraction_init");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_dooraction_start"))
					{
						editSpawn(gm, s.substring(21), "set_dooraction_start");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_base_respawnhere"))
					{
						editSpawn(gm, s.substring(21), "set_base_respawnhere");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_importance"))
					{
						editSpawn(gm, s.substring(15), "set_importance");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_width"))
					{
						editSpawn(gm, s.substring(10), "set_width");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_length"))
					{
						editSpawn(gm, s.substring(11), "set_length");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("default_loc"))
					{
						editSpawn(gm, "", "default_loc");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_note"))
					{
						editSpawn(gm, s.substring(9), "set_note");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_type"))
					{
						editSpawn(gm, s.substring(9), "set_type");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
					else if(s.startsWith("set_npc_id"))
					{
						editSpawn(gm, s.substring(11), "set_npc_id");
						showEditSpawnMenu(gm, _editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
					}
				}
				else
					gm.sendMessage("Unknown action. " + action + " doesn't exist!");
			}
		}
		catch (Exception e)
		{
			gm.sendMessage("Bad parameters included! If you're sure it's not you fault, then PM hNoke for repair.");
			NexusLoader.debug("GM " + gm.getPlayersName() + " sent invalid bypass: " + action, Level.WARNING);
			e.printStackTrace();
		}
	}
	
	private void showLanguageSelectMenu(PlayerEventInfo gm)
	{
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_lang.htm");
		
		TextBuilder tb = new TextBuilder();
		
		for(Entry<String, String> e : LanguageEngine.getLanguages().entrySet())
		{
			tb.append("<button value=\"" + e.getValue() + "\" action=\"bypass admin_event_manage set_language " + e.getKey() + "\" width=50 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			tb.append("<br1>");
		}
		
		html = html.replaceAll("%languages%", tb.toString());
		html = html.replaceAll("%current%", LanguageEngine.getLanguage());
		html = html.replaceAll("%events%", tb.toString());
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showEventOrderMenu(PlayerEventInfo gm, String lastModified)
	{
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_order.htm");
		
		TextBuilder tb = new TextBuilder();
		List<EventScheduleData> dataList = EventManager.getInstance().getMainEventManager().getEventScheduleData();
		
		EventScheduleData data = null;
		boolean isLastModified;
		for(int i = 0; i < dataList.size(); i++)
		{
			for(EventScheduleData d : dataList)
			{
				if(d.getOrder() == i+1)
				{
					data = d;
					break;
				}
			}
			
			if(data == null)
			{
				NexusLoader.debug("Missing event in eventOrder system with order = " + (i+1) + " - please correct this in database", Level.WARNING);
				continue;
			}
			
			isLastModified = data.getEvent().getAltTitle().equals(lastModified);
			
			if(isLastModified)
				tb.append("<table width=280 bgcolor=2f2f2f>");
			else
				tb.append("<table width=280>");
			
			tb.append("<tr>");
			
			if(!EventConfig.getInstance().isEventAllowed(data.getEvent()))
			{
				tb.append("<td width=120><font color=LEVEL>" + data.getOrder() + "</font>. <font color=4f4f4f>" + data.getEvent().getAltTitle() + "</font></td>");
			}
			else if(EventMapSystem.getInstance().getMapsCount(data.getEvent()) == 0)
			{
				tb.append("<td width=120><font color=LEVEL>" + data.getOrder() + "</font>. <font color=B56A6A>" + data.getEvent().getAltTitle() + "</font></td>");
			}
			else if(EventManager.getInstance().getMainEventManager().getLastEventOrder() == data.getEvent())
			{
				tb.append("<td width=120><font color=LEVEL>" + data.getOrder() + "</font>. <font color=DCDCDC> " + data.getEvent().getAltTitle() + "</font></td>");
			}
			else
			{
				tb.append("<td width=120><font color=LEVEL>" + data.getOrder() + "</font>. <font color=829F80>" + data.getEvent().getAltTitle() + "</font></td>");	
			}
			
			if(data.getOrder() <= 1)
			{
				tb.append("<td width=50></td>");
				tb.append("<td width=60><button value=\"Down\" action=\"bypass admin_event_manage eventorder_move down " + data.getEvent().getAltTitle() + "\" width=60 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			}
			else if(data.getOrder() >= dataList.size())
			{
				tb.append("<td width=50><button value=\"Up\" action=\"bypass admin_event_manage eventorder_move up " + data.getEvent().getAltTitle() + "\" width=50 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				tb.append("<td width=60></td>");
			}
			else
			{
				tb.append("<td width=50><button value=\"Up\" action=\"bypass admin_event_manage eventorder_move up " + data.getEvent().getAltTitle() + "\" width=50 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				tb.append("<td width=60><button value=\"Down\" action=\"bypass admin_event_manage eventorder_move down " + data.getEvent().getAltTitle() + "\" width=60 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			}
			
			tb.append("</tr>");
			tb.append("</table>");
		}
		
		html = html.replaceAll("%events%", tb.toString());
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void gmDebug(PlayerEventInfo gm)
	{
		if(!NexusLoader.isDebugging(gm))
		{
			NexusLoader.addGmDebug(gm);
			gm.sendMessage("Signed for debugging.");
		}
		else
		{
			NexusLoader.removeGmDebug(gm);
			gm.sendMessage("Unsigned from debugging.");
		}
	}
	
	private void moveEventInOrder(PlayerEventInfo gm, String direction, String event)
	{
		EventType type = EventType.getType(event);
		
		if(type == null || !type.isRegularEvent())
		{
			gm.sendMessage("No/Wrong event specified.");
			return;
		}
		
		EventScheduleData data = EventManager.getInstance().getMainEventManager().getScheduleData(type);
		if(data == null)
		{
			gm.sendMessage("No schedule data found for this event.");
			return;
		}
		
		if(direction.equals("up"))
		{
			data.raiseOrder();
		}
		else if(direction.equals("down"))
		{
			data.decreaseOrder();
		}
		
		showEventOrderMenu(gm, event);
	}
	
	private void showGlobalConfigMenu(PlayerEventInfo gm, String configType, int page)
	{
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_globalconfig_menu.htm");

		html = html.replaceAll("%objectId%", String.valueOf(0));
		
		TextBuilder tb = new TextBuilder();
		
		if(configType == null || configType.length() == 0)
			configType = "Core";
		
		String color = "FFFFFF";
		if(configType.equals("Core"))
		{
			color = "86CE71";
		}
		else if(configType.equals("Scheduler"))
		{
			color = "D19F6D";
		}
		else if(configType.equals("Buffer"))
		{
			color = "A48EE1";
		}
		else if(configType.equals("Features"))
		{
			color = "69A5D6";
		}
		
		// used for 'Back' buttons
		_editingInfos.get(gm.getPlayersId()).activeGlobalConfigType = configType;
		_editingInfos.get(gm.getPlayersId()).activeGlobalConfigPage = page;
		
		if(configType.equals("Scheduler"))
		{
			tb.append("<table width=275 bgcolor=444444><tr>");
			tb.append("<td width=275 align=center><font color=DFBC99><a action=\"bypass admin_event_manage eventorder_menu \">Edit event order</a></font></td>");
			tb.append("</tr></table><br>");
//			tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		}
		
		if(configType.equals("Core"))
		{
			tb.append("<table width=275 bgcolor=444444><tr>");
			tb.append("<td width=275 align=center><font color=DFBC99><a action=\"bypass admin_event_manage eventlang_menu \">Engine Language</a></font></td>");
			tb.append("</tr></table><br>");
		}
		
		if(configType.equals("Features"))
		{
			tb.append("<table width=275 bgcolor=444444><tr>");
			
			if(page == 1)
			{
				tb.append("<td width=90 align=center><font color=CB9258>Page 1</font></td>");
				tb.append("<td width=90 align=center><font color=DFBC99><a action=\"bypass admin_event_manage globalconfig_menu Features 2\">Page 2</a></font></td>");
				tb.append("<td width=90 align=center><font color=DFBC99><a action=\"bypass admin_event_manage globalconfig_menu Features 3\">Page 3</a></font></td>");
			}
			else if(page == 2)
			{
				tb.append("<td width=90 align=center><font color=DFBC99><a action=\"bypass admin_event_manage globalconfig_menu Features 1\">Page 1</a></font></td>");
				tb.append("<td width=90 align=center><font color=CB9258>Page 2</font></td>");
				tb.append("<td width=90 align=center><font color=DFBC99><a action=\"bypass admin_event_manage globalconfig_menu Features 3\">Page 3</a></font></td>");
			}
			else if(page == 3)
			{
				tb.append("<td width=90 align=center><font color=DFBC99><a action=\"bypass admin_event_manage globalconfig_menu Features 1\">Page 1</a></font></td>");
				tb.append("<td width=90 align=center><font color=DFBC99><a action=\"bypass admin_event_manage globalconfig_menu Features 2\">Page 2</a></font></td>");
				tb.append("<td width=90 align=center><font color=CB9258>Page 3</font></td>");
			}
			
			tb.append("</tr></table><br>");
		}
		
		int count = 0;
		final int recordsPerPage = 15;
		int startFrom = (page-1) * recordsPerPage;
		int showTill = page * recordsPerPage;
		
		for(GlobalConfigModel config : EventConfig.getInstance().getGlobalConfigs(configType))
		{
			String value = config.getValue();
			if(value.length() > 19)
				value = value.substring(0, 20) + "..";
			
			if(configType.equals("GearScore") && config.getKey().startsWith("defVal"))
				continue;
			
			count ++;
			
			// for now pages implemented only for 'Features' type configs
			if(configType.equals("Features"))
			{
				if(count > startFrom && count <= showTill)
				{
					
				}
				else
				{
					continue;
				}
			}
			
			tb.append("<table width=275 bgcolor=333333><tr>");
			tb.append("<td width=160 align=left><font color=" + color + "><a action=\"bypass admin_event_manage globalconfig_showinfo " + configType + " " + page + " " + config.getKey() + "\">" + config.getKey() + "</a></font></td>");
			tb.append("<td width=100 align=right><font color=9f9f9f><a action=\"bypass admin_event_manage globalconfig_edit " + config.getKey() + "\">" + value + "</a></font></td><td width=10></td>");
			tb.append("</tr></table>");
			
			if(_editingInfos.get(gm.getPlayersId()).activeGlobalConfigKeyShown.equals(config.getKey()))
			{
				tb.append("<table width=275><tr>");
				tb.append("<td width=275><font color=9f9f9f>" + config.getDesc() + "</font></td>");
				tb.append("</tr></table>");
			}
			
			tb.append("<br>");
		}
		
		if(configType.equals("Buffer"))
		{
			tb.append("<br><center><font color=9f9f9f>These settings apply to the NPC Buffer (ID " + EventConfig.getInstance().getGlobalConfigInt("assignedNpcId") + ") provided by this engine.</font></center>");
			tb.append("<br1><a action=\"bypass -h nxs_npcbuffer_reload\">Apply changes in these configs</a>");
		}
		
		String result = tb.toString();
		
		html = html.replaceAll("%configs%", result);
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showGlobalConfigEditation(PlayerEventInfo gm, String key)
	{
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_globalconfig_edit.htm");
		
		GlobalConfigModel config = EventConfig.getInstance().getGlobalConfig(null, key);
		
		if(config == null)
		{
			gm.sendMessage("Global Config '" + key + "' doesn't exist.");
			return;
		}
		
		int inputType = config.getInputType();
		if(inputType == 1)
			html = html.replaceAll("%input%", "<edit var=\"value\" width=140 height=15>");
		else if(inputType == 2)
			html = html.replaceAll("%input%", "<multiedit var=\"value\" width=180 height=30>");
		
		html = html.replaceAll("%key%", config.getKey());
		html = html.replaceAll("%info%", config.getDesc());
		html = html.replaceAll("%value%", config.getValue());
		
		html = html.replaceAll("%page%", "" + _editingInfos.get(gm.getPlayersId()).activeGlobalConfigPage);
		html = html.replaceAll("%type%", _editingInfos.get(gm.getPlayersId()).activeGlobalConfigType);
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void setGlobalConfigValue(PlayerEventInfo gm, String key, String value)
	{
		GlobalConfigModel config = EventConfig.getInstance().getGlobalConfig(null, key);
		
		if(config == null)
		{
			gm.sendMessage("Global Config '" + key + "' doesn't exist.");
			return;
		}
		
		EventConfig.getInstance().setGlobalConfigValue(config, key, value);
		gm.sendMessage("The value of global config '" + key + "' is now '" + value + "'.");
		
		showGlobalConfigEditation(gm, key);
	}

	private void showNewEventMenu(PlayerEventInfo gm, EventType event, boolean runTimeEdit)
	{
		AbstractMainEvent ev = EventManager.getInstance().getCurrentMainEvent();
		if(ev != null)
		{
			gm.sendMessage("Another event is already running.");
			showMenu(gm, false);
			return;
		}
		
		if(EventManager.getInstance().getMainEventManager().autoSchedulerEnabled() && !EventManager.getInstance().getMainEventManager().autoSchedulerPaused())
		{
			gm.sendMessage("You may not start event when the automatic scheduler is enabled.");
			showMenu(gm, false);
			return;
		}
		
		if(event != null && event.isRegularEvent())
			_editingInfos.get(gm.getPlayersId()).activeEditingEvent = event;
		else if(_editingInfos.get(gm.getPlayersId()).activeEditingEvent != null && _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
			event = _editingInfos.get(gm.getPlayersId()).activeEditingEvent;
			
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_launch_event.htm");

		html = html.replaceAll("%objectId%", String.valueOf(0));
		
		TextBuilder tb = new TextBuilder();
		
		if(event != null)
			tb.append((event.getHtmlTitle().length() > 16 ? event.getAltTitle() : event.getHtmlTitle()) + ";");
		
		for(EventType type : EventType.values())
		{
			if(type.isRegularEvent() && type.allowEdits() && type != EventType.Unassigned && EventManager.getInstance().getMainEvent(type) != null)
			{
				if(event != null && event == type)
					continue;
				
				tb.append((type.getHtmlTitle().length() > 16 ? type.getAltTitle() : type.getHtmlTitle()) + ";");
			}
		}
		
		String result = tb.toString();
		
		html = html.replaceAll("%events%", result.substring(0, result.length() - 1));
		
		tb = new TextBuilder();
		
		if(event == null)
		{
			tb.append("-Select event- ");
		}
		else
		{
			for(EventMap map : EventMapSystem.getInstance().getMainEventMaps(event))
			{
				if(EventManager.getInstance().getMainEvent(event) != null && EventManager.getInstance().getMainEvent(event).canRun(map))
					tb.append(map.getMapName() + ";");
			}
		}
		
		result = tb.toString();
		
		if(result.length() > 0)
			html = html.replaceAll("%aviable_maps%", result.substring(0, result.length() - 1));
		else
			html = html.replaceAll("%aviable_maps%", "No map aviable");
		
		tb = new TextBuilder();
		
		for(Entry<Integer, RegNpcLoc> e : EventManager.getInstance().getMainEventManager().regNpcLocs.entrySet())
		{
			tb.append(e.getValue().name + ";");
		}
		
		result = tb.toString();
		
		html = html.replaceAll("%regNpcLocs%", result.substring(0, result.length() - 1));
		
		if(_editingInfos.get(gm.getPlayersId()).activeEditingEvent != null && !_editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
			_editingInfos.get(gm.getPlayersId()).activeEditingEvent = null;
		
		if(_editingInfos.get(gm.getPlayersId()).activeEditingEvent == null)
			html = html.replaceAll("%selected%", "<font color=BD8282>No event selected</font>");
		else
			html = html.replaceAll("%selected%", _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getHtmlTitle());
		
		if(runTimeEdit)
		{
			html = html.replaceAll("%runTimeName%", "<a action=\"bypass admin_event_manage new_event " + event.getAltTitle() + "\">Run time:</a>");
			html = html.replaceAll("%runTimeEdit%", "<edit var=\"runTime\" width=130 height=13>");
			html = html.replaceAll("%runTimeVar%", "\\$runTime");
		}
		else
		{
			if(event == null)
				html = html.replaceAll("%runTimeName%", "Run time:");
			else
				html = html.replaceAll("%runTimeName%", "<a action=\"bypass admin_event_manage new_event_runtime_edit " + event.getAltTitle() + "\">Run time:</a>");
			
			if(event != null)
			{
				int defaultRunTime = EventConfig.getInstance().getGlobalConfigInt("defaultRunTime");
				try
				{
					defaultRunTime = EventManager.getInstance().getMainEvent(event).getInt("runTime");
				}
				catch (NullPointerException e)
				{
				}
				
				html = html.replaceAll("%runTimeEdit%", "<font color=9f9f9f>" + defaultRunTime + " min</font>");
				html = html.replaceAll("%runTimeVar%", "" + defaultRunTime);
			}
			else
			{
				html = html.replaceAll("%runTimeEdit%", "<font color=9f9f9f>- Select event -</font>");
				html = html.replaceAll("%runTimeVar%", "" + 10);
			}
		}
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void deleteMainInstanceType(PlayerEventInfo gm, int id)
	{
		EventType type = _editingInfos.get(gm.getPlayersId()).activeEditingEvent;
		
		if(type == null || !type.isRegularEvent())
		{
			gm.sendMessage("Wrong event.");
			return;
		}
		
		AbstractMainEvent event = EventManager.getInstance().getMainEvent(type);
		if(event == null)
		{
			gm.sendMessage("Main event " + type.getAltTitle() + " does not exist or isn't finished yet.");
			return;
		}
		
		if(event.getInstanceTypes().size() == 1)
		{
			gm.sendMessage("The event must have at least one InstanceType!");
			return;
		}
		
		MainEventInstanceType instance = event.getInstanceType(id);
		
		MainEventInstanceTypeManager.getInstance().removeInstanceType(instance);
	}
	
	private void showEditEventInstancesMenu(PlayerEventInfo gm, EventType type)
	{
		_editingInfos.get(gm.getPlayersId()).activeEditingEvent = type;
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editevent_instances.htm");
		
		TextBuilder tb = new TextBuilder();
		
		AbstractMainEvent event = EventManager.getInstance().getMainEvent(type);
		if(event == null)
		{
			gm.sendMessage("Main event " + type.getAltTitle() + " does not exist or isn't finished yet.");
			return;
		}
		
		for(MainEventInstanceType instance : event.getInstanceTypes().values())
		{
			tb.append("<table width=280 bgcolor=363636>");
			
			tb.append("<tr>");
			tb.append("<td align=left width=150><font color=ac9887)>" + instance.getName() + " </font><font color=54585C>(" + instance.getId() + ")</font></td>");
			
			tb.append("<td align=right width=80><button value=\"Edit\" action=\"bypass admin_event_manage edit_main_instance_type " + instance.getId() + "\" width=70 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			
			tb.append("</tr>");
			
			tb.append("</table>");
			
			tb.append("<img src=\"L2UI.SquareBlank\" width=1 height=6>");
		}
		
		html = html.replaceAll("%instances%", tb.toString());
		
		if(type != EventType.Unassigned)
		{
			html = html.replaceAll("%event%", type.getAltTitle());
			html = html.replaceAll("%name%", type.getHtmlTitle());
			html = html.replaceAll("%eventId%", String.valueOf(type.getId()));
			
			html = html.replaceAll("%enableDisable%", EventConfig.getInstance().isEventAllowed(type) ? "<font color=74BE85><a action=\"bypass admin_event_manage set_aviable\">Enabled</a></font>" : "<font color=B46F6B><a action=\"bypass admin_event_manage set_aviable\">Disabled</a></font>");
			
			tb = new TextBuilder();
			
			tb.append(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection + ";");
			for(String s : mainEventEditingPages)
			{
				if(!s.equals(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection))
					tb.append(s + ";");
			}
			
			String result = tb.toString();
			
			html = html.replaceAll("%event_pages%", result.substring(0, result.length() - 1));
		}
		else
		{
			gm.sendMessage("This doesn't have instances.");
			return;
		}

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void editMainInstanceType(PlayerEventInfo gm, int id, String action, String value, boolean addToValue)
	{
		EventType type = _editingInfos.get(gm.getPlayersId()).activeEditingEvent;
		
		if(type == null || !type.isRegularEvent())
		{
			gm.sendMessage("Wrong event.");
			return;
		}
		
		AbstractMainEvent event = EventManager.getInstance().getMainEvent(type);
		if(event == null)
		{
			gm.sendMessage("Main event " + type.getAltTitle() + " does not exist or isn't finished yet.");
			return;
		}
		
		MainEventInstanceType instance = event.getInstanceType(id);
		
		if(action.equals("set_name"))
		{
			instance.setName(value);
		}
		else
		{
			instance.setConfig(action, value, addToValue);
		}
		
		MainEventInstanceTypeManager.getInstance().updateInstanceType(instance);
	}
	
	/** -1 id creates new InstanceType */
	private void showEditMainInstanceTypeWindow(PlayerEventInfo gm, int id)
	{
		EventType type = _editingInfos.get(gm.getPlayersId()).activeEditingEvent;
		
		if(type == null || !type.isRegularEvent())
		{
			gm.sendMessage("Wrong event.");
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editevent_instances_edit.htm");
		
		AbstractMainEvent event = EventManager.getInstance().getMainEvent(type);
		if(event == null)
		{
			gm.sendMessage("Main event " + type.getAltTitle() + " does not exist or isn't finished yet.");
			return;
		}
		
		MainEventInstanceType instance;
		
		if(id == -1)
		{
			instance = new MainEventInstanceType(MainEventInstanceTypeManager.getInstance().getNextId(), event, "NewInstance", "New Instance", null);
			event.insertConfigs(instance);
			MainEventInstanceTypeManager.getInstance().addInstanceType(instance, true);
			
			_editingInfos.get(gm.getPlayersId()).currentConfigModelShown = "";
		}
		else
			instance = event.getInstanceType(id);
		
		_editingInfos.get(gm.getPlayersId()).activeEdittingMainInstanceType = instance.getId();
		
		TextBuilder tb = new TextBuilder();
		boolean expanded = false;
		
		for(Entry<String, ConfigModel> e : instance.getConfigs().entrySet())
		{
			if(_editingInfos.get(gm.getPlayersId()).currentConfigModelShownCategory.equals("InstanceType") && _editingInfos.get(gm.getPlayersId()).currentConfigModelShown != null && 
					_editingInfos.get(gm.getPlayersId()).currentConfigModelShown.equals(e.getValue().getKey()))
					expanded = true;
			else
				expanded = false;
			
			if(expanded)
				tb.append("<table width=280 bgcolor=599944>");
			else
				tb.append("<table width=280 bgcolor=3f3f3f>");
			
			tb.append("<tr>");
			
			tb.append("<td width=175 align=left><font color=ac9887> " + e.getValue().getKey() + "</font></td>");
			
			String value = e.getValue().getValue();
			if(value.length() > 6)
				value = "...";
			
			boolean brackets = true;
			if(value.length() >= 6)
				brackets = false;
			
			tb.append("<td width=45 align=left><font color=9f9f9f>" + (brackets ? "(" : "") + "" + value + "" + (brackets? ")" : "") + "</font></td>");
			tb.append("<td width=50 align=right><button value=\"Expand\" width=55 action=\"bypass admin_event_manage expand_configmodel InstanceType " + e.getKey() + "\" height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			
			tb.append("</tr>");
			
			tb.append("</table>");
			
			if(expanded)
			{
				tb.append("<table width=278 bgcolor=2f2f2f>");
				tb.append("<tr>");
				tb.append("<td width=240><font color=9f9f9f>" + e.getValue().getDesc() + "</font></td>");
				tb.append("</tr>");
				tb.append("</table>");
				
				if(e.getValue().getInput() == InputType.MultiEdit || e.getValue().getInput() == InputType.MultiAdd || e.getValue().getValue().length() > 5)
				{
					tb.append("<table width=278 bgcolor=2f2f2f>");
					tb.append("<tr>");
					tb.append(e.getValue().getValueShownInHtml());
					tb.append("</tr>");
					tb.append("</table>");
				}
				
				tb.append("<table width=280 bgcolor=2f2f2f>");
				tb.append("<tr>");
				tb.append("<td>" + e.getValue().getInputHtml(180) + "</td>");
				tb.append("<td align=left><button value=\"" + e.getValue().getAddButtonName() + "\" width=40 action=\"bypass admin_event_manage edit_main_instance_type_" + e.getValue().getAddButtonAction() + "_config " + e.getKey() + " \\$" + e.getKey() + "\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				
				tb.append("</tr>");
				tb.append("</table>");
				
				tb.append("<table width=280 bgcolor=2f2f2f>");
				tb.append("<tr>");
				tb.append("<td><font color=6f6f6f>Default: " + e.getValue().getDefaultVal() + "</font></td>");
				tb.append("<td align=right><button value=\"" + e.getValue().getUtilButtonName() + "\" action=\"bypass admin_event_manage edit_main_instance_type_set_config " + e.getKey() + " " + e.getValue().getDefaultVal() + "\" width=" + e.getValue().getUtilButtonWidth() + " height=17 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				tb.append("</tr>");
				tb.append("</table>");
			}
			
			tb.append("<img src=\"L2UI.SquareBlank\" width=1 height=4>");
		}
		
		html = html.replaceAll("%configs%", tb.toString());
		
		// params
		html = html.replaceAll("%instance_name%", instance.getName());
		html = html.replaceAll("%instance_id%", "" + instance.getId());
		
		html = html.replaceAll("%event%", type.getAltTitle());
		html = html.replaceAll("%name%", type.getHtmlTitle());
		html = html.replaceAll("%eventId%", String.valueOf(type.getId()));
		
		html = html.replaceAll("%enableDisable%", EventConfig.getInstance().isEventAllowed(type) ? "<font color=74BE85><a action=\"bypass admin_event_manage set_aviable\">Enabled</a></font>" : "<font color=B46F6B><a action=\"bypass admin_event_manage set_aviable\">Disabled</a></font>");

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showEditEventMenu(PlayerEventInfo gm, String eventName, String page)
	{
		if(eventName.equals("Default"))
			eventName = "Unassigned";
		
		EventType type = EventType.getType(eventName);
		if(type == null || !type.isRegularEvent())
		{
			gm.sendMessage("Wrong event.");
			return;
		}
		
		_editingInfos.get(gm.getPlayersId()).activeEditingEvent = type;
		_editingInfos.get(gm.getPlayersId()).activeEventModeId = 1;
		
		if(page == null)
			page = mainEventEditingPages[0];
		
		_editingInfos.get(gm.getPlayersId()).activeEventEditingSection = page;
		
		if(page.equals(mainEventEditingPages[1])) //rewards
		{
			showRewardsEditation(gm, type.getAltTitle(), RewardPosition.None, null);
			return;
		}
		else if(page.equals(mainEventEditingPages[2])) //configs
		{
			showConfigsMenu(gm, type.getAltTitle(), 1);
			return;
		}
		else if(page.equals(mainEventEditingPages[3])) //instances
		{
			showEditEventInstancesMenu(gm, type);
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editevent.htm");
		
		TextBuilder tb = new TextBuilder();
		
		boolean error;
		for(EventMap map : EventMapSystem.getInstance().getMaps(type).values())
		{
			error = map.getMissingSpawns().length() > 0;
			
			tb.append("<table width=280 bgcolor=363636>");
			
			tb.append("<tr>");
			tb.append("<td align=left width=150><font color=" + (error ? "CD6565" : "ac9887") + ">" + map.getMapName() + " </font><font color=54585C>" + map.getGlobalId() + "</font></td>");
			
			tb.append("<td align=right width=80><button value=\"" + (error ? "! " : "") + "Status" + (error ? " !" : "") + "\" action=\"bypass admin_event_manage show_map_status " + map.getGlobalId() + " " + type.getAltTitle() + "\" width=70 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("<td align=right width=80><button value=\"Edit\" action=\"bypass admin_event_manage edit_event_map " + map.getGlobalId() + " 0\" width=70 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			
			tb.append("</tr>");
			
			tb.append("</table>");
			
			tb.append("<img src=\"L2UI.SquareBlank\" width=1 height=6>");
		}
		
		if(!EventMapSystem.getInstance().getMaps(type).isEmpty())
			html = html.replaceAll("%maps%", tb.toString());
		else
			html = html.replaceAll("%maps%", "<font color=B46F6B>No maps aviable for this event.</font>");
		
		html = html.replaceAll("%mapsAmmount%", String.valueOf(EventMapSystem.getInstance().getMaps(type).values().size()));
		
		if(type != EventType.Unassigned)
		{
			html = html.replaceAll("%event%", type.getAltTitle());
			html = html.replaceAll("%name%", type.getHtmlTitle());
			html = html.replaceAll("%eventId%", String.valueOf(type.getId()));
			
			html = html.replaceAll("%enableDisable%", EventConfig.getInstance().isEventAllowed(type) ? "<font color=74BE85><a action=\"bypass admin_event_manage set_aviable\">Enabled</a></font>" : "<font color=B46F6B><a action=\"bypass admin_event_manage set_aviable\">Disabled</a></font>");
			
			tb = new TextBuilder();
			
			tb.append(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection + ";");
			for(String s : mainEventEditingPages)
			{
				if(!s.equals(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection))
					tb.append(s + ";");
			}
			
			String result = tb.toString();
			
			html = html.replaceAll("%event_pages%", result.substring(0, result.length() - 1));
		}
		else
		{
			html = html.replaceAll("%event%", "Default");
			html = html.replaceAll("%name%", "Unassigned maps");
			html = html.replaceAll("%eventId%", String.valueOf(0));
			
			html = html.replaceAll("%enableDisable%", "N/A");
			html = html.replaceAll("%event_pages%", "Maps");
		}

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showEditEventsMenu(PlayerEventInfo gm)
	{
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editevents.htm");
		
		int count = 0;
		int i = 0;
		TextBuilder tb = new TextBuilder();
		
		boolean exists;

		// aviable events
		tb.append("<font color=LEVEL>Main Events:</font><br1><table width=270>");
		
		for(EventType type : EventType.values())
		{
			if(!type.isRegularEvent() || !type.allowEdits() || type == EventType.Unassigned)
				continue;
			else
			{
				exists = EventManager.getInstance().getMainEvent(type) != null;
				
				if(i == 0)
				{
					tb.append("<tr>");
				}
				i++;

				if(exists)
					tb.append("<td><button value=\"" + type.getHtmlTitle() + "\" action=\"bypass admin_event_manage edit_event " + type.getAltTitle() + "\" width=130 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				else
					tb.append("<td align=left><font color=6f6f6f>" + type.getHtmlTitle() + "</font></td>");
				
				count++;
				
				if(i == 2)
				{
					tb.append("</tr>");
					i = 0;
				}
			}
		}
		
		if(i != 0 && i % 2 == 1)
			tb.append("</tr>");
		
		tb.append("</table>");

		if(count > 0)
			html = html.replaceAll("%allowed_events%", tb.toString());
		else
			html = html.replaceAll("%allowed_events%", "");

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void allowDisallowMapMode(PlayerEventInfo gm, String map)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		EventMode mode = event.getMode();
		
		int mapId = Integer.parseInt(map);
		
		if(mode.getDisMaps().contains(mapId))
			mode.getDisMaps().remove((Object)mapId);
		else
			mode.getDisMaps().add(mapId);
		
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
	}
	
	private void showEditModeMaps(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		EventMode mode = event.getMode();
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_modes_maps.htm");
		
		TextBuilder tb = new TextBuilder();
		
		tb.append("<table width=270>");
		
		for(EventMap map : EventMapSystem.getInstance().getMaps(_editingInfos.get(gm.getPlayersId()).activeEditingEvent).values())
		{
			if(event.canRun(map))
				tb.append("<tr><td width=180><font color=ac9887>" + map.getMapName() + "</font></td>");
			else
				tb.append("<tr><td width=180><font color=696969>" + map.getMapName() + "</font></td>");
				
			if(mode.getDisMaps().contains(map.getGlobalId()))
				tb.append("<td width=90><font color=B46F6B><a action=\"bypass admin_event_manage mini_edit_modes_map " + map.getGlobalId() + "\">Disabled</a></font></td></tr>");
			else
				tb.append("<td width=90><font color=74BE85><a action=\"bypass admin_event_manage mini_edit_modes_map " + map.getGlobalId() + "\">Enabled</a></font></td></tr>");
		}
		
		tb.append("</table>");
		
		html = html.replaceAll("%maps%", tb.toString());
		
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%type%", event.getEventType().getAltTitle());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));
		html = html.replaceAll("%modeId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEventModeId));
		html = html.replaceAll("%modeName%", mode.getModeName());
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void createNewMode(PlayerEventInfo gm, String modeName)
	{
		int newModeId = 0;
		for(Entry<Integer, MiniEventManager> e : EventManager.getInstance().getMiniEvents().get(_editingInfos.get(gm.getPlayersId()).activeEditingEvent).entrySet())
		{
			if(newModeId < e.getKey())
				newModeId = e.getKey();
		}
		
		newModeId ++;
		
		MiniEventManager manager = EventManager.getInstance().createManager(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, newModeId, modeName, modeName, true);
		
		if(manager == null)
			return;
		
		manager.getMode().setAllowed(false);
		
		_editingInfos.get(gm.getPlayersId()).activeEventModeId = newModeId;
		showEditModeWindow(gm);
	}
	
	private void createNewModeMenu(PlayerEventInfo gm)
	{
		if(_editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("Event Modes aren't implemented for Main events yet.");
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_modes_new_menu.htm");
		
		html = html.replaceAll("%name%", _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getHtmlTitle());
		html = html.replaceAll("%type%", _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle());
		html = html.replaceAll("%eventId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEditingEvent.getId()));
		
		int newModeId = 0;
		for(Entry<Integer, MiniEventManager> e : EventManager.getInstance().getMiniEvents().get(_editingInfos.get(gm.getPlayersId()).activeEditingEvent).entrySet())
		{
			if(newModeId < e.getKey())
				newModeId = e.getKey();
		}
		
		newModeId++;
		
		html = html.replaceAll("%newModeId%", String.valueOf(newModeId));
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void clearEvent(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		event.cleanMe(true);
		
		gm.sendMessage("All games successfully aborted and all players unregistered.");
	}
	
	private void createRegFile(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		EventMode mode = event.getMode();
		final String name = event.getEventType().getAltTitle() + "_" + mode.getModeName() + ".htm";
		
		File file = new File("data/html/mods/nexus/events/" + name);
		
		try
		{
			if(!file.createNewFile())
			{
				file.delete();
				gm.sendMessage("The registration html file has been deleted.");
				return;
			}
			else
			{
				File defaultFile = new File("data/html/mods/nexus/events/" + event.getEventType().getAltTitle() + "_Default.htm");
				if(!defaultFile.exists())
				{
					gm.sendMessage("The default event file 'data/html/mods/nexus/events/" + event.getEventType().getAltTitle() + "_Default.htm' not found.");
					return;
				}
				else
				{
					Writer writer = null;
					BufferedReader reader = null;
					try
					{
						reader = new BufferedReader(new FileReader(defaultFile));
						
						writer = new BufferedWriter(new FileWriter(file));
						String line = reader.readLine();
						
						while(line != null)
						{
							writer.write(line + "\n");
							line = reader.readLine();
						}
					} 
					catch (Exception e)
					{
						e.printStackTrace();
					}
					finally
					{
						reader.close();
						writer.close();
					}
				}
			}
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		CallBack.getInstance().getOut().reloadHtmls();
		gm.sendMessage("The file has been created in 'data/html/mods/nexus/events/" + name + "'.");
	}
	
	private void enableDisableMode(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		event.getMode().setAllowed(!event.getMode().isAllowed());
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
	}
	
	private void deleteMode(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		if(_editingInfos.get(gm.getPlayersId()).activeEventModeId == 1)
		{
			gm.sendMessage("You can't delete the default mode. You may only disable it.");
			return;
		}
		
		event.cleanMe(true);
		
		EventManager.getInstance().getMiniEvents().get(_editingInfos.get(gm.getPlayersId()).activeEditingEvent).remove(_editingInfos.get(gm.getPlayersId()).activeEventModeId);
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, 0);
		
		_editingInfos.get(gm.getPlayersId()).activeEventModeId = 1;
	}
	
	private void setModeName(PlayerEventInfo gm, String name, boolean visible)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		EventMode mode = event.getMode();
		
		if(!visible)
			mode.setModeName(name);
		else
			mode.setVisibleName(name);
		
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
	}
	
	private void setModeNpcId(PlayerEventInfo gm, String value)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		EventMode mode = event.getMode();
		
		try
		{
			int id = Integer.parseInt(value);
			mode.setNpcId(id);
			
			gm.sendMessage("Done. Mini event " + mode.getModeName() + " will now be accessable from NPC Id " + id);
		}
		catch (Exception e)
		{
			gm.sendMessage("Npc id has to be a number.");
		}
		
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
	}
	
	private void scheduleRemoveTime(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		RunTime time = event.getMode().getScheduleInfo().getTimes().get(_editingInfos.get(gm.getPlayersId()).activeEventModeTimeId);
		
		if(time == null)
		{
			gm.sendMessage("RunTime object with ID " + _editingInfos.get(gm.getPlayersId()).activeEventModeTimeId + " does not exist.");
			return;
		}
		
		event.getMode().getScheduleInfo().getTimes().remove(time.id);
		
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		_editingInfos.get(gm.getPlayersId()).activeEventModeTimeId = 0;
	}
	
	private void schedulerSetHour(PlayerEventInfo gm, String hour, boolean from)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		RunTime time = event.getMode().getScheduleInfo().getTimes().get(_editingInfos.get(gm.getPlayersId()).activeEventModeTimeId);
		
		if(time == null)
		{
			gm.sendMessage("RunTime object with ID " + _editingInfos.get(gm.getPlayersId()).activeEventModeTimeId + " does not exist.");
			return;
		}
		
		boolean check = true;
		try
		{
			String hoursString = hour.split(":")[0];
			String minsString = hour.split(":")[1];
			
			if(hour.equals("0:00"))
				hour = "00:00";
			
			int hours = Integer.parseInt(hoursString);
			int mins = Integer.parseInt(minsString);
			
			if(hours < 0 || hours > 23)
			{
				gm.sendMessage("Hours must be within 0 - 23 values.");
				check = false;
			}
			
			if(mins < 0 || mins > 59)
			{
				gm.sendMessage("Minutes must be within 0 - 59 values.");
				check = false;
			}
		} 
		catch (Exception e)
		{
			gm.sendMessage("Sorry, wrong data specified. Use only numbers.");
			check = false;
		}
		
		if(!check)
			return;
		
		if(from)
			time.from = hour;
		else
			time.to = hour;
		
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
	}
	
	private void schedulerAddDay(PlayerEventInfo gm, String dayName)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		RunTime time = event.getMode().getScheduleInfo().getTimes().get(_editingInfos.get(gm.getPlayersId()).activeEventModeTimeId);
		
		if(time == null)
		{
			gm.sendMessage("RunTime object with ID " + _editingInfos.get(gm.getPlayersId()).activeEventModeTimeId + " does not exist.");
			return;
		}
		
		if(dayName.equals("AllDays"))
		{
			for(Day d : Day.values())
			{
				if(!time.days.contains(d))
					time.days.add(d);
			}
		}
		else
		{
			Day d = Day.getDayByName(dayName);
			
			if(!time.days.contains(d))
				time.days.add(d);
			else
				gm.sendMessage("This day has already been added.");
		}
		
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
	}
	
	private void schedulerRemoveDay(PlayerEventInfo gm, String dayPrefix)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		RunTime time = event.getMode().getScheduleInfo().getTimes().get(_editingInfos.get(gm.getPlayersId()).activeEventModeTimeId);
		
		if(time == null)
		{
			gm.sendMessage("RunTime object with ID " + _editingInfos.get(gm.getPlayersId()).activeEventModeTimeId + " does not exist.");
			return;
		}
		
		time.days.remove(Day.getDay(dayPrefix));
		
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
	}
	
	private void showEditModeSchedulerTime(PlayerEventInfo gm)
	{
		showEditModeSchedulerTime(gm, _editingInfos.get(gm.getPlayersId()).activeEventModeTimeId);
	}
	
	private void showEditModeSchedulerTime(PlayerEventInfo gm, int timeId)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		EventMode mode = event.getMode();
		RunTime time = event.getMode().getScheduleInfo().getTimes().get(timeId);
		
		if(time == null)
		{
			gm.sendMessage("RunTime object with ID " + timeId + " does not exist.");
			return;
		}
		
		_editingInfos.get(gm.getPlayersId()).activeEventModeTimeId = timeId;
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_modes_scheduler_edit.htm");
		
		html = html.replaceAll("%from%", time.from);
		html = html.replaceAll("%to%", time.to);
		
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%type%", event.getEventType().getAltTitle());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));
		html = html.replaceAll("%modeId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEventModeId));
		html = html.replaceAll("%modeName%", mode.getModeName());
		
		long l =  time.getNext(!time.isActual(), true);
		Date date = new Date(l);
		String ts = date.toString();
		
		if(time.days.isEmpty())
			html = html.replaceAll("%future%", "Future: N/A - No days are specified!");
		else
			html = html.replaceAll("%future%", (time.isActual() ? "Ends: " : "Starts: ") + ts.substring(0, ts.length() - 9));
		
		TextBuilder tb = new TextBuilder();
		
		tb.append("<table width=220>");
		
		for(Day d : time.days)
		{
			tb.append("<tr>");
			tb.append("<td align=left>" + d.fullName + " (" + d.prefix + ")</td>");
			tb.append("<td align=right><button value=\"Remove\" action=\"bypass admin_event_manage mini_edit_modes_scheduler_removeday " + d.prefix + "\" width=70 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr>");
		} 
		
		tb.append("</table>");
		
		html = html.replaceAll("%days%", tb.toString());
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void schedulerRefresh(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		EventMode mode = event.getMode();
		mode.refreshScheduler();
	}
	
	private void schedulerAddNewTime(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		EventMode mode = event.getMode();
		
		RunTime time = mode.getScheduleInfo().addTime();
		
		_editingInfos.get(gm.getPlayersId()).activeEventModeTimeId = time.id;
		
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, time.id);
	}
	
	private void showEditModeSchedulerMenu(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		EventMode mode = event.getMode();
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_modes_scheduler_menu.htm");
		
		Calendar c = Calendar.getInstance();
		String time = c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);
		
		html = html.replaceAll("%currTime%", time);
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%type%", event.getEventType().getAltTitle());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));
		html = html.replaceAll("%modeId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEventModeId));
		html = html.replaceAll("%modeName%", mode.getModeName());
		
		TextBuilder tb = new TextBuilder();
		
		tb.append("<table width=270>");
		
		for(Entry<Integer, RunTime> e : mode.getScheduleInfo().getTimes().entrySet())
		{
			tb.append("<tr>");
			tb.append("<td align=left><font color=ac9887>" + e.getValue().from + " - " + e.getValue().to);
			tb.append(" - </font><font color=9f9f9f>" + e.getValue().getDaysString(true) + "</font></td>");
			tb.append("<td align=right><button value=\"Edit\" action=\"bypass admin_event_manage mini_edit_modes_scheduler_edit " + e.getKey() + "\" width=50 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr>");
		}
		
		tb.append("</table>");
		
		html = html.replaceAll("%times%", tb.toString());
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showEditModeWindow(PlayerEventInfo gm)
	{
		showEditModeWindow(gm, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
	}
	
	private void showEditModeWindow(PlayerEventInfo gm, int modeId)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, modeId);
		
		_editingInfos.get(gm.getPlayersId()).activeEventModeId = modeId;
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		EventMode mode = event.getMode();
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_modes_edit.htm");
		
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%type%", event.getEventType().getAltTitle());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));
		html = html.replaceAll("%modeId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEventModeId));
		html = html.replaceAll("%modeName%", mode.getModeName());
		html = html.replaceAll("%visibleName%", mode.getVisibleName());
		html = html.replaceAll("%npcId%", (mode.getNpcId() == 0 ? "None assigned" : String.valueOf(mode.getNpcId())));
		html = html.replaceAll("%htmlPath%", "/mods/events/npc/" + event.getEventType().getAltTitle() + "_" + event.getMode().getModeName() + ".htm");
		
		long l = mode.getFuture();
		Date date = new Date(System.currentTimeMillis() + l);
		String ts = date.toString();
		
		if(!event.canRun())
			html = html.replaceAll("%future%", "<font color=804848>No suitable maps for this Event mode. Can't run this mode!</font>");
		else if(mode.isNonstopRun())
			html = html.replaceAll("%future%", "This mode runs nonstop.");
		else if(l == -1 || mode.getScheduleInfo().getTimes().isEmpty())
			html = html.replaceAll("%future%", "<font color=804848>Future: null - No valid scheduler is specified. Can't run this mode!</font>");
		else
			html = html.replaceAll("%future%", (mode.isRunning() ? "Ends: " : "Starts: ") + ts.substring(0, ts.length() - 9));
		
		html = html.replaceAll("%enableDisable%", mode.isAllowed() ? "<font color=74BE85><a action=\"bypass admin_event_manage mini_edit_modes_enabledisable\">Enabled</a></font>" : "<font color=B46F6B><a action=\"bypass admin_event_manage mini_edit_modes_enabledisable\">Disabled</a></font>");
		
		html = html.replaceAll("%deleteButton%", mode.getModeId() == 1 ? "" : "<td width=95><button value=\"Delete mode\" action=\"bypass admin_event_manage mini_edit_modes_delete\" width=95 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		
		boolean isHtmlGenerated = false;
		File file = new File("data/html/mods/nexus/events/" + event.getEventType().getAltTitle() + "_" + event.getMode().getModeName() + ".htm");
		if(file.exists())
			isHtmlGenerated = true;
		
		html = html.replaceAll("%generateButton%", isHtmlGenerated ? "<button value=\"Delete HTML\" action=\"bypass admin_event_manage mini_edit_modes_generatefile\" width=85 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" : "<button value=\"Generate HTML\" action=\"bypass admin_event_manage mini_edit_modes_generatefile\" width=85 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void removeFeature(PlayerEventInfo gm, String featureName)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		AbstractFeature feature = null;

		for(AbstractFeature f : event.getMode().getFeatures())
		{
			if(f.getType().toString().equals(featureName))
			{
				feature = f;
				break;
			}
		}
		
		if(feature == null)
		{
			gm.sendMessage("This feature doesn't exist for this mode.");
			return;
		}
		
		event.getMode().getFeatures().remove(feature);
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
	}
	
	private void setActiveFeatureCategory(PlayerEventInfo gm, String categoryName)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		if(categoryName.equals("All"))
		{
			_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureCategory = null;
			return;
		}
		
		FeatureCategory category = null;
		
		for(FeatureCategory fc : FeatureCategory.values())
		{
			if(fc.toString().equalsIgnoreCase(categoryName))
			{
				category = fc;
				break;
			}
		}
		
		if(category == null)
		{
			gm.sendMessage("This category doesn't exist.");
			return;
		}
		
		_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureCategory = category;
	}
	
	private void setActiveShowedFeature(PlayerEventInfo gm, String featureTypeName)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		if(_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureShowed != null 
				&& _editingInfos.get(gm.getPlayersId()).activeEventModeFeatureShowed.toString().equals(featureTypeName))
		{
			_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureShowed = null;
			return;
		}
		
		for(FeatureType type : FeatureType.values())
		{
			if(type.toString().equalsIgnoreCase(featureTypeName))
			{
				_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureShowed = type;
			}
		}
	}
	
	private void showNewMiniEventFeatureMenu(PlayerEventInfo gm, String featureType)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		EventMode mode = event.getMode();
		
		if(!featureType.equals("Default"))
		{
			Constructor<?> _constructor = null;
			AbstractFeature feature = null;
			
			Class<?>[] classParams = {EventType.class, PlayerEventInfo.class, String.class};
			try
			{
				_constructor = Class.forName("cz.nxs.events.engine.mini.features." + featureType + "Feature").getConstructor(classParams);
			} 
			catch (Exception e)
			{
				e.printStackTrace();
				return;
			}
			
			try
			{
				Object[] objectParams = {_editingInfos.get(gm.getPlayersId()).activeEditingEvent, null, null};
				
				Object tmp = _constructor.newInstance(objectParams);
				feature = (AbstractFeature) tmp;
			} 
			catch (Exception e)
			{
				e.printStackTrace();
				return;
			}
			
			mode.addFeature(feature);
			
			EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, mode.getModeId());
			_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureShowed = null;
			_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureCategory = null;
			
			showEditFeature(gm, featureType);
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_modes_featureedit_new.htm");
		
		TextBuilder tb = new TextBuilder();
		boolean alreadyIn;
		boolean allowedForEvent;
		boolean allowedForCategory;
		int count = 0;
		
		FeatureInfo info;
		
		for(FeatureType feature : FeatureType.values())
		{
			if(feature.toString().equals(featureType))
				continue;
			
			alreadyIn = false;
			
			info = FeatureBase.getInstance().get(feature);
			
			if(info == null)
			{
				NexusLoader.debug("Feature " + feature.toString() + " isn't in FeatureBase.", Level.WARNING);
				continue;
			}
			
			allowedForEvent = info.isForEvent(event.getEventType());
			
			if(_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureCategory != null && 
					_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureCategory != info.getCategory())
			{
				allowedForCategory = false;
			}
			else
				allowedForCategory = true;
			
			for(AbstractFeature f : mode.getFeatures())
			{
				if(f.getType() == feature)
				{
					alreadyIn = true;
					break;
				}
			}
			
			if(!alreadyIn && allowedForEvent && allowedForCategory)
			{
				tb.append("<table width=275 bgcolor=333333><tr>");
				tb.append("<td width=110><font color=ac9887>" + info.getVisibleName() + "</font></td>");
				tb.append("<td width=75 align=left><font color=ac9887><a action=\"bypass admin_event_manage mini_edit_feature_showinfo " + feature.toString() + "\">View Info</a></font></td>");
				tb.append("<td width=90 align=right><button value=\"Add to mode\" action=\"bypass admin_event_manage mini_edit_feature_add " + feature.toString() + "\" width=90 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				tb.append("</tr></table>");
				
				if(_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureShowed == feature)
				{
					tb.append("<table width=275><tr>");
					tb.append("<td width=275><font color=9f9f9f>" + info.getDesc() + "</font></td>");
					tb.append("</tr></table>");
				}
				
				tb.append("<br>");
				
				count++;
			}
		}
		
		if(count == 0)
		{
			String cat = (_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureCategory == null ? "." : " for category " + _editingInfos.get(gm.getPlayersId()).activeEventModeFeatureCategory.toString() + ".");
			tb.append("<font color=B46F6B>No features are aviable" + cat + "</font>");
		}
		
		html = html.replaceAll("%features%", tb.toString());
		
		if(_editingInfos.get(gm.getPlayersId()).activeEventModeFeatureCategory != null)
			html = html.replaceAll("%category%", _editingInfos.get(gm.getPlayersId()).activeEventModeFeatureCategory.toString());
		else
			html = html.replaceAll("%category%", "All");
		
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%type%", event.getEventType().getAltTitle());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));
		html = html.replaceAll("%modeId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEventModeId));
		html = html.replaceAll("%modeName%", mode.getModeName());
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void setFeatureConfigValue(PlayerEventInfo gm, String featureName, String configName, String value)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		AbstractFeature feature = null;

		for(AbstractFeature f : event.getMode().getFeatures())
		{
			if(f.getType().toString().equals(featureName))
			{
				feature = f;
				break;
			}
		}
		
		if(feature == null)
		{
			gm.sendMessage("This feature doesn't exist for this mode.");
			return;
		}
		
		feature.setValueFor(configName, value);
		EventConfig.getInstance().updateEventModes(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		gm.sendMessage("Done");
	}
	
	private void showEditFeatureConfig(PlayerEventInfo gm, String featureName, String config)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		AbstractFeature feature = null;
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_modes_featureedit_config.htm");
		
		for(AbstractFeature f : event.getMode().getFeatures())
		{
			if(f.getType().toString().equals(featureName))
			{
				feature = f;
				break;
			}
		}
		
		if(feature == null)
		{
			gm.sendMessage("This feature doesn't exist for this mode.");
			return;
		}
		
		FeatureConfig configInfo = feature.getConfig(config);
		
		int inputType = configInfo.inputFormType;
		
		if(inputType == 1)
			html = html.replaceAll("%input%", "<edit var=\"value\" width=140 height=15>");
		else if(inputType == 2)
			html = html.replaceAll("%input%", "<multiedit var=\"value\" width=180 height=30>");
		
		html = html.replaceAll("%key%", config);
		html = html.replaceAll("%info%", configInfo.desc);
		html = html.replaceAll("%value%", feature.getValueFor(config));
		
		html = html.replaceAll("%featureName%", feature.getType().toString());
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%type%", event.getEventType().getAltTitle());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));
		html = html.replaceAll("%modeId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEventModeId));
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showEditFeature(PlayerEventInfo gm, String featureName)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		AbstractFeature feature = null;
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_modes_featureedit.htm");
		
		for(AbstractFeature f : event.getMode().getFeatures())
		{
			if(f.getType().toString().equals(featureName))
			{
				feature = f;
				break;
			}
		}
		
		if(feature == null)
		{
			gm.sendMessage("This feature doesn't exist for this mode.");
			return;
		}
		
		TextBuilder tb = new TextBuilder();
		
		boolean bg = true;
		String value;
		
		for(FeatureConfig c : feature.getConfigs())
		{
			value = feature.getValueFor(c.name);
			if(value.length() > 19)
				value = value.substring(0, 20) + "..";
			
			tb.append("<table width=280" + (bg ? " bgcolor=333333" : "") + ">");
			
			tb.append("<tr>");
			tb.append("<td align=left><font color=ac9887><a action=\"bypass admin_event_manage mini_edit_featureconfig " + featureName + " " + c.name + "\">" + c.name + "</font></td>");
			tb.append("<td align=right><font color=9f9f9f><a action=\"bypass admin_event_manage mini_edit_featureconfig " + featureName + " " + c.name + "\">" + value + "</a></font></td>");
			tb.append("</tr>");
			
			tb.append("</table>");
			
			bg = !bg;
		}
		
		html = html.replaceAll("%options%", tb.toString());
		html = html.replaceAll("%featureName%", feature.getType().toString());
		html = html.replaceAll("%desc%", FeatureBase.getInstance().get(feature.getType()).getDesc());
		
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%type%", event.getEventType().getAltTitle());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));
		html = html.replaceAll("%modeId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEventModeId));
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showCompactModesMenu(PlayerEventInfo gm)
	{
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_modes_menu_compact.htm");
		
		TextBuilder tb = new TextBuilder();

		tb.append("<table width=270>");
		
		for(Entry<Integer, MiniEventManager> e : EventManager.getInstance().getMiniEvents().get(_editingInfos.get(gm.getPlayersId()).activeEditingEvent).entrySet())
		{
			tb.append("<tr><td width=180><font color=" + (e.getValue().getMode().isRunning() ? "ac9887" : "606060") + "><a action=\"bypass admin_event_manage mini_edit_modes_menu " + e.getValue().getMode().getModeId() + "\">" + e.getValue().getMode().getModeName() + "</a></font></td>");
			
			if(!e.getValue().getMode().isAllowed())
				tb.append("<td width=90><font color=B46F6B><a action=\"bypass admin_event_manage mini_edit_modes_enabledisable " + e.getValue().getMode().getModeId() + "\">Disabled</a></font></td></tr>");
			else
				tb.append("<td width=90><font color=74BE85><a action=\"bypass admin_event_manage mini_edit_modes_enabledisable " + e.getValue().getMode().getModeId() + "\">Enabled</a></font></td></tr>");
		}
		
		tb.append("</table>");
		
		html = html.replaceAll("%modes%", tb.toString());
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showModesMenu(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null)
		{
			_editingInfos.get(gm.getPlayersId()).activeEventModeId = 1;
			event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		}
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_modes_menu.htm");
		
		TextBuilder tb = new TextBuilder();
		tb.append(event.getMode().getModeName() + ";");
		
		for(Entry<Integer, MiniEventManager> e : EventManager.getInstance().getMiniEvents().get(_editingInfos.get(gm.getPlayersId()).activeEditingEvent).entrySet())
		{
			if(!e.getValue().getMode().getModeName().equals(event.getMode().getModeName()))
			{
				tb.append(e.getValue().getMode().getModeName() + ";");
			}
		}
		
		String result = tb.toString();
		html = html.replaceAll("%modes%", result.substring(0, result.length() - 1));
		
		tb = new TextBuilder();
		
		int sizer = 0;
		String value;
		
		tb.append("<table width=270 bgcolor=4f4f4f>");
		
		for(AbstractFeature feature : event.getMode().getFeatures())
		{
			tb.append("<tr>");
			tb.append("<td width=95><font color=ac9887>" + feature.getType().toString() + "</font></td>");
			tb.append("<td width=105><font color=9f9f9f>");
			
			for(FeatureConfig c : feature.getConfigs())
			{
				if(sizer >= 12)
				{
					tb.append("...");
					break;
				}
				
				value = feature.getValueFor(c.name);
				sizer += value.length() + 2;
				
				tb.append(" " + value);
				tb.append(";");
			}
			
			sizer = 0;
			
			tb.append("</font></td>");
			tb.append("<td width=70 align=right><button value=\"Edit\" action=\"bypass admin_event_manage mini_edit_feature " + feature.getType().toString() + "\" width=70 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr>");
		}
		
		tb.append("</table>");
		
		html = html.replaceAll("%features%", tb.toString());
		
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%type%", event.getEventType().getAltTitle());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));
		html = html.replaceAll("%modeId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEventModeId));
		
		html = html.replaceAll("%enableDisable%", EventConfig.getInstance().isEventAllowed(event.getEventType()) ? "<font color=74BE85><a action=\"bypass admin_event_manage set_aviable\">Enabled</a></font>" : "<font color=B46F6B><a action=\"bypass admin_event_manage set_aviable\">Disabled</a></font>");
		
		tb = new TextBuilder();
		
		tb.append(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection + ";");
		for(String page : miniEventEditingPages)
		{
			if(!page.equals(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection))
				tb.append(page + ";");
		}
		
		result = tb.toString();
		
		html = html.replaceAll("%event_pages%", result.substring(0, result.length() - 1));
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}

	private void showManualMatchSetMapMenu(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_manualmatch_map.htm");
		
		TextBuilder tb = new TextBuilder();
		
		for(EventMap map : EventMapSystem.getInstance().getMaps(_editingInfos.get(gm.getPlayersId()).activeEditingEvent).values())
		{
			tb.append("<button value=\"" + map.getMapName() + "\" action=\"bypass admin_event_manage mini_manual_match_set_map " + map.getGlobalId() + "\" width=160 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			tb.append("<br1>");
		}
		
		html = html.replaceAll("%maps%", tb.toString());
		html = html.replaceAll("%mapsAmmount%", String.valueOf(EventMapSystem.getInstance().getMaps(_editingInfos.get(gm.getPlayersId()).activeEditingEvent).values().size()));
		
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		html = html.replaceAll("%activeMap%", map == null ? "Unknown map" : map.getMapName());
		html = html.replaceAll("%activeMapId%", String.valueOf(map == null ? 0 : map.getGlobalId()));
		
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%type%", event.getEventType().toString());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showMatches(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_showmatches.htm");
		//
		TextBuilder tb = new TextBuilder();
		tb.append(event.getMode().getModeName() + ";");
		
		for(Entry<Integer, MiniEventManager> e : EventManager.getInstance().getMiniEvents().get(_editingInfos.get(gm.getPlayersId()).activeEditingEvent).entrySet())
		{
			if(!e.getValue().getMode().getModeName().equals(event.getMode().getModeName()))
			{
				tb.append(e.getValue().getMode().getModeName() + ";");
			}
		}
		
		String result = tb.toString();
		html = html.replaceAll("%modes%", result.substring(0, result.length() - 1));
		//
		tb = new TextBuilder();
		
		for(MiniEventGame match : event.getActiveGames())
		{
			tb.append("<button value=\"");
			
			int i = 1;
			for(EventTeam t : match.getTeams())
			{
				tb.append(t.getTeamName());
				
				if(i < match.getTeams().length)
					tb.append(" VS ");
				
				i++;
			}
			
			tb.append("\" action=\"bypass admin_event_manage mini_edit_match " + match.getGameId() + "\" width=180 height=19 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			tb.append("<br1>");
		}
		
		html = html.replaceAll("%matches%", tb.toString());
		
		String fontColor = "9f9f9f";
		if(event.getActiveGames().size() == event.getMaxGamesCount())
			fontColor = "B46F6B";
		
		html = html.replaceAll("%matchesAmmount%", "<font color=" + fontColor + ">" + event.getActiveGames().size() + "/" + event.getMaxGamesCount() + "</font>");
		
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%type%", event.getEventType().toString());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));
		
		html = html.replaceAll("%enableDisable%", EventConfig.getInstance().isEventAllowed(event.getEventType()) ? "<font color=74BE85><a action=\"bypass admin_event_manage set_aviable\">Enabled</a></font>" : "<font color=B46F6B><a action=\"bypass admin_event_manage set_aviable\">Disabled</a></font>");
		
		tb = new TextBuilder();
		
		tb.append(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection + ";");
		for(String s : miniEventEditingPages)
		{
			if(!s.equals(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection))
				tb.append(s + ";");
		}
		
		result = tb.toString();
		
		html = html.replaceAll("%event_pages%", result.substring(0, result.length() - 1));

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void editMatch(PlayerEventInfo gm, int matchId)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		MiniEventGame game = null;
		for(MiniEventGame g : event.getActiveGames())
		{
			if(g.getGameId() == matchId)
			{
				game = g;
				break;
			}
		}
		
		_editingInfos.get(gm.getPlayersId()).activeEditingMatch = game.getGameId();

		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_editmatch.htm");
		
		TextBuilder tb = new TextBuilder();
		
		tb.append("<table width=200>");
		
		for(EventTeam team : game.getTeams())
		{
			tb.append("<tr>");
			tb.append("<td><font color=" + team.getNameColorInString() + ">" + team.getTeamName() + "</font></td>");
			tb.append("<td><button value=\"Show\" action=\"bypass admin_event_manage mini_show_team_members " + team.getTeamId() + "\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr>");
		}
		
		tb.append("</table>");
		
		html = html.replaceAll("%teams%", tb.toString());

		html = html.replaceAll("%matchId%", String.valueOf(game.getGameId()));

		tb = new TextBuilder();
		
		int i = 1;
		for(EventTeam team : game.getTeams())
		{
			tb.append("<font color=" + team.getNameColorInString() + "> " + team.getScore() + "</font>");
			
			if(i < game.getTeams().length)
				tb.append(" : ");
			
			i++;
		}
		
		html = html.replaceAll("%score%", tb.toString());
		
		html = html.replaceAll("%activeMap%", game.getMap().getMapName());
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%type%", event.getEventType().toString());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void watchMatch(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		MiniEventGame game = null;
		for(MiniEventGame g : event.getActiveGames())
		{
			if(g.getGameId() == _editingInfos.get(gm.getPlayersId()).activeEditingMatch)
			{
				game = g;
				break;
			}
		}
		
		if(game == null)
		{
			gm.sendMessage("This match doesn't exist.");
			return;
		}
		
		gm.setInstanceId(game.getInstanceId());
		
		Loc loc = null;
		
		for(EventTeam team : game.getTeams())
		{
			for(PlayerEventInfo pi : team.getPlayers())
			{
				if(pi.isOnline())
				{
					loc = new Loc(pi.getX(), pi.getY(), pi.getZ());
					break;
				}
			}
		}
		
		if(loc != null)
			gm.teleToLocation(loc, false);
		else
			gm.sendMessage("There's no player inside the match.");
	}
	
	private void abortMatch(PlayerEventInfo gm)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		MiniEventGame game = null;
		for(MiniEventGame g : event.getActiveGames())
		{
			if(g.getGameId() == _editingInfos.get(gm.getPlayersId()).activeEditingMatch)
			{
				game = g;
				break;
			}
		}
		
		if(game == null)
		{
			gm.sendMessage("This match doesn't exist.");
			return;
		}
		
		game.broadcastMessage("Match was aborted by a GM.", false);
		game.clearEvent();
		
		gm.sendMessage("Match was successfully aborted.");
	}
	
	private void showTeamMembers(PlayerEventInfo gm, int teamId)
	{
		MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
		{
			gm.sendMessage("This mini event doesn't exist.");
			return;
		}
		
		MiniEventGame game = null;
		for(MiniEventGame g : event.getActiveGames())
		{
			if(g.getGameId() == _editingInfos.get(gm.getPlayersId()).activeEditingMatch)
			{
				game = g;
				break;
			}
		}
		
		if(game == null)
		{
			gm.sendMessage("This match doesn't exist.");
			return;
		}

		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_showteam.htm");
		
		TextBuilder tb = new TextBuilder();
		
		tb.append("<table width=200>");
		
		for(PlayerEventInfo player : game.getTeams()[teamId - 1].getPlayers())
		{
			tb.append("<tr>");
			tb.append("<td><font color=9f9f9f>" + player.getPlayersName() + "</font></td>");
			tb.append("<td><button value=\"Teleport\" action=\"bypass admin_teleportto " + player.getPlayersName() + "\" width=65 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr>");
		}
		
		tb.append("</table>");
		
		html = html.replaceAll("%players%", tb.toString());

		html = html.replaceAll("%matchId%", String.valueOf(game.getGameId()));
		
		html = html.replaceAll("%name%", event.getEventName());
		html = html.replaceAll("%teamName%", game.getTeams()[teamId - 1].getTeamName());
		html = html.replaceAll("%type%", event.getEventType().toString());
		html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showManualMatchMenu(PlayerEventInfo gm)
	{
		try
		{
			MiniEventManager event = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
			
			if(event == null || _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
			{
				gm.sendMessage("This mini event doesn't exist.");
				return;
			}
			
			String html;
			html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_manualmatch.htm");
			
			TextBuilder tb = new TextBuilder();
			
			tb.append("<table width=200>");
			
			for(Map.Entry<Integer, FastList<PlayerEventInfo>> e : _editingInfos.get(gm.getPlayersId()).manualMatchPlayers.entrySet())
			{
				for(PlayerEventInfo player : e.getValue())
				{
					tb.append("<tr>");
					tb.append("<td><font color=" + EventManager.getInstance().getTeamColorForHtml(e.getKey()) + ">" + ((player == null || !player.isOnline()) ? "Offline. Clear list" : player.getPlayersName()) + "</font></td>");
					tb.append("<td><button value=\"Remove\" action=\"bypass admin_event_manage mini_manual_match_rem_team " + player != null ? player.getPlayersId() : 0 + "\" width=70 height=19 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					tb.append("</tr>");
				}
			}
			tb.append("<tr>");
			tb.append("<td></td><td><button value=\"Clear\" action=\"bypass admin_event_manage mini_manual_match_clear\" width=70 height=19 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr>");
			
			tb.append("</table>");
			
			html = html.replaceAll("%players%", tb.toString());

			EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
			html = html.replaceAll("%activeMap%", map == null ? "Random map" : map.getMapName());
			html = html.replaceAll("%activeMapId%", String.valueOf(map == null ? 0 : map.getGlobalId()));
			
			html = html.replaceAll("%name%", event.getEventName());
			html = html.replaceAll("%type%", event.getEventType().toString());
			html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));

			html = html.replaceAll("%title%", "Nexus Events");
			gm.sendPacket(html);
			gm.sendStaticPacket();
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void setManualMatchMap(PlayerEventInfo gm, int mapId)
	{
		_editingInfos.get(gm.getPlayersId()).activeEditingMap = mapId;
		gm.sendMessage("Map for this event has been changed.");
	}
	
	private void startManualMatch(PlayerEventInfo gm)
	{
		try
		{
			MiniEventManager manager = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
			
			RegistrationData[] data = new RegistrationData[_editingInfos.get(gm.getPlayersId()).manualMatchPlayers.size()];
			
			FastList<PlayerEventInfo> infos;
			for(Entry<Integer, FastList<PlayerEventInfo>> i : _editingInfos.get(gm.getPlayersId()).manualMatchPlayers.entrySet())
			{
				infos = new FastList<PlayerEventInfo>();
				for(PlayerEventInfo player : i.getValue())
				{
					infos.add(CallBack.getInstance().getPlayerBase().addInfo(player));
				}
				
				data[i.getKey() - 1] = new RegistrationData(infos);
			}
						
			if(!manager.checkCanFight(gm, data))
			{
				gm.sendMessage("Game can't be started. Check registered players and try it again.");
				return;
			}
			
			EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
			if(!manager.launchGame(data, map))
			{
				gm.sendMessage("Game can't be started, propably due to missing teams or players");
				return;
			}
			else
				gm.sendMessage("Game was successfully started.");
			
			_editingInfos.get(gm.getPlayersId()).manualMatchPlayers.clear();
		} 
		catch (Exception e)
		{
			gm.sendMessage("Something is wrong with match settings, propably missing team. Error: " + e.toString());
			e.printStackTrace(); // debugs
		}
	}
	
	private void showMiniEventMenu(PlayerEventInfo gm)
	{
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_menu.htm");

		html = html.replaceAll("%objectId%", String.valueOf(0));
		
		int count = 0;
		int i = 0;
		TextBuilder tb = new TextBuilder();

		// aviable events
		tb.append("<font color=LEVEL>Enabled Mini Events:</font><br1><table width=281>");
		
		for(Map.Entry<EventType, Map<Integer, MiniEventManager>> e : EventManager.getInstance().getMiniEvents().entrySet())
		{
			if(e.getKey().allowEdits() && EventConfig.getInstance().isEventAllowed(e.getKey()))
			{
				if(i == 0)
				{
					tb.append("<tr>");
				}
				i++;

				tb.append("<td><button value=\"" + e.getKey().getHtmlTitle() + "\" action=\"bypass admin_event_manage mini_edit_event " + e.getKey().getId() + "\" width=140 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				count++;
				
				if(i == 2)
				{
					tb.append("</tr>");
					i = 0;
				}
			}
		}
		
		if(i != 0 && i % 2 == 1)
			tb.append("</tr>");
		
		tb.append("</table>");

		if(count > 0)
			html = html.replaceAll("%allowed_events%", tb.toString());
		else
			html = html.replaceAll("%allowed_events%", "");
		
		// temporary disallowed events
		count = 0;
		i = 0;
		tb = new TextBuilder();
		
		// blocked events
		tb.append("<font color=LEVEL>Disabled Mini Events:</font><br1><table width=281>");
		
		for(Map.Entry<EventType, Map<Integer, MiniEventManager>> e : EventManager.getInstance().getMiniEvents().entrySet())
		{
			if(e.getKey() != EventType.Unassigned && e.getKey().allowEdits() && !EventConfig.getInstance().isEventAllowed(e.getKey()))
			{
				if(i == 0)
				{
					tb.append("<tr>");
				}
				i++;

				tb.append("<td><button value=\"" + e.getKey().getHtmlTitle() + "\" action=\"bypass admin_event_manage mini_edit_event " + e.getKey().getId() + "\" width=140 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				count++;
				
				if(i == 2)
				{
					tb.append("</tr>");
					i = 0;
				}
			}
		}
		
		if(i != 0 && i % 2 == 1)
			tb.append("</tr>");
		
		tb.append("</table>");

		if(count > 0)
			html = html.replaceAll("%blocked_events%", tb.toString());
		else
			html = html.replaceAll("%blocked_events%", "");

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void setModeId(PlayerEventInfo gm, String mode)
	{
		for(Entry<Integer, MiniEventManager> e : EventManager.getInstance().getMiniEvents().get(_editingInfos.get(gm.getPlayersId()).activeEditingEvent).entrySet())
		{
			if(e.getValue().getMode().getModeName().equals(mode))
			{
				gm.sendMessage("You're now editting mode " + mode + " (ID "+ e.getKey() + ")."); 
				_editingInfos.get(gm.getPlayersId()).activeEventModeId = e.getKey();
			}
		}
	}
	
	private void showEditMiniEventMenu(PlayerEventInfo gm, int eventId, String page)
	{
		EventType type = null;
		for(EventType t : EventType.values())
		{
			if(t.getId() == eventId)
				type = t;
		}
		
		if(type != null)
			showEditMiniEventMenu(gm, type, page);
		else
		{
			gm.sendMessage("This event doesn't exist.");
			return;
		}
	} 
	
	private void addAviableEvent(PlayerEventInfo gm)
	{
		EventConfig.getInstance().setEventAllowed(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, true);
		gm.sendMessage("Event's been enabled.");
	}
	
	private void removeAviableEvent(PlayerEventInfo gm)
	{
		EventConfig.getInstance().setEventAllowed(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, false);
		gm.sendMessage("Event's been disabled.");
	}
	
	//TODO: !!!!!!!!!
	private void showSpawnTypeInfoMenu(PlayerEventInfo gm)
	{
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_spawntypeinfo_menu.htm");
		
		TextBuilder tb = new TextBuilder();
		
		for(EventType type : EventType.values())
		{
			if(type != EventType.Unassigned && type != EventType.Tournament)
			{
				tb.append("<button value=\"" + type.getHtmlTitle() + "\" action=\"bypass admin_event_manage spawn_type_info_event " + type.getAltTitle() + "\" width=160 height=19 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				tb.append("<br1>");
			}
		}
		
		html = html.replaceAll("%events%", tb.toString());
		html = html.replaceAll("%spawnId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEditingSpawn));
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}	
	
	private void showSpawnTypeInfoEvent(PlayerEventInfo gm, String eventName)
	{
		EventType type = null;
		for(EventType t : EventType.values())
		{
			if(t.getAltTitle().equals(eventName))
				type = t;
		}
		
		if(type == null)
		{
			gm.sendMessage("This event doesn't exist. 1");
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_spawntypeinfo_info.htm");
		
		TextBuilder tb = new TextBuilder();

		MiniEventManager event = EventManager.getInstance().getMiniEvent(type, 1);
		
		if(event == null)
		{
			gm.sendMessage("This event doesn't exist.");
			return;
		}
		
		tb.append("<table width=280>");
		for(Map.Entry<SpawnType, String> e : event.getAviableSpawnTypes().entrySet())
		{
			tb.append("<tr><td><font color=" + e.getKey().getHtmlColor() + ">" + e.getKey().toString() + "</font></td>");
			tb.append("<td><font color=9f9f9f>" + e.getValue() + "</font></td></tr>");
		}
		tb.append("</table>");

		html = html.replaceAll("%spawnTypes%", tb.toString());
		html = html.replaceAll("%event%", type.getHtmlTitle());
		html = html.replaceAll("%spawnId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEditingSpawn));
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}	
	
	private void showEditMiniEventMenu(PlayerEventInfo gm, EventType type, String page)
	{
		_editingInfos.get(gm.getPlayersId()).activeEventModeId = 1;
		
		if(type.isRegularEvent())
		{
			showEditEventMenu(gm, type.getAltTitle(), page);
			return;
		}
		
		if(page == null)
			page = miniEventEditingPages[0];
		
		_editingInfos.get(gm.getPlayersId()).activeEventEditingSection = page;
		
		MiniEventManager event = EventManager.getInstance().getMiniEvent(type, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		
		if(type != EventType.Unassigned)
		{
			if(event == null)
			{
				event = EventManager.getInstance().getMiniEvent(type, 1);
			}
			
			if(event == null)
			{
				event = EventConfig.getInstance().createDefaultMode(type);
				gm.sendMessage("The default mode if this event was somehow deleted (or something has simply fucked up!). The engine created a new one, don't forget to reconfigure it!");
			}
		}
		
		_editingInfos.get(gm.getPlayersId()).activeEditingEvent = type;
		
		if(page.equals(miniEventEditingPages[1])) // modes
		{
			showModesMenu(gm);
			return;
		}
		else if(page.equals(miniEventEditingPages[2])) //rewards
		{
			showRewardsEditation(gm, type.getAltTitle(), RewardPosition.None, null);
			return;
		}
		else if(page.equals(miniEventEditingPages[3])) //matches
		{
			showMatches(gm);
			return;
		}
		else if(page.equals(miniEventEditingPages[4])) //configs
		{
			showConfigsMenu(gm, type.getAltTitle(), 1);
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mini_editevent.htm");
		
		TextBuilder tb = new TextBuilder();
		
		//tb.append("<table width=280 bgcolor=444846>");
		
		boolean error;
		for(EventMap map : EventMapSystem.getInstance().getMaps(type).values())
		{
			error = map.getMissingSpawns().length() > 0;
			
			tb.append("<table width=280 bgcolor=363636>");
			
			tb.append("<tr>");
			tb.append("<td align=left width=150><font color=" + (error ? "CD6565" : "ac9887") + ">" + map.getMapName() + " </font><font color=54585C>" + map.getGlobalId() + "</font></td>");
			
			tb.append("<td align=right width=80><button value=\"" + (error ? "! " : "") + "Status" + (error ? " !" : "") + "\" action=\"bypass admin_event_manage show_map_status " + map.getGlobalId() + " " + type.getAltTitle() + "\" width=70 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("<td align=right width=80><button value=\"Edit\" action=\"bypass admin_event_manage edit_event_map " + map.getGlobalId() + " 0\" width=70 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			
			tb.append("</tr>");
			
			tb.append("</table>");
			
			tb.append("<img src=\"L2UI.SquareBlank\" width=1 height=6>");
		}
		
		//tb.append("</table>");
		
		if(!EventMapSystem.getInstance().getMaps(type).isEmpty())
			html = html.replaceAll("%maps%", tb.toString());
		else
			html = html.replaceAll("%maps%", "<font color=B46F6B>No maps aviable for this event.</font>");
		
		html = html.replaceAll("%mapsAmmount%", String.valueOf(EventMapSystem.getInstance().getMaps(type).values().size()));
		
		if(event != null)
		{
			html = html.replaceAll("%name%", event.getEventName());
			html = html.replaceAll("%type%", event.getEventType().toString());
			html = html.replaceAll("%eventId%", String.valueOf(event.getEventType().getId()));
			html = html.replaceAll("%enableDisable%", EventConfig.getInstance().isEventAllowed(type) ? "<font color=74BE85><a action=\"bypass admin_event_manage set_aviable\">Enabled</a></font>" : "<font color=B46F6B><a action=\"bypass admin_event_manage set_aviable\">Disabled</a></font>");
			
			tb = new TextBuilder();
			
			tb.append(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection + ";");
			for(String s : miniEventEditingPages)
			{
				if(!s.equals(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection))
					tb.append(s + ";");
			}
			
			String result = tb.toString();
			
			html = html.replaceAll("%event_pages%", result.substring(0, result.length() - 1));
		}
		else
		{
			html = html.replaceAll("%name%", "Unassigned maps");
			html = html.replaceAll("%type%", "-");
			html = html.replaceAll("%eventId%", String.valueOf(0));
			html = html.replaceAll("%enableDisable%", "N/A");
			html = html.replaceAll("%event_pages%", "Maps");
		}
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void removeReward(PlayerEventInfo gm)
	{
		RewardItem item = EventRewardSystem.getInstance().getReward(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId, _editingInfos.get(gm.getPlayersId()).activeEditingReward);
		
		if(item == null)
		{
			gm.sendMessage("This item doesn't exist.");
			return;
		}
		
		EventRewardSystem.getInstance().removeRewardFromDb(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEditingReward, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
	}
	
	private void saveReward(PlayerEventInfo gm)
	{
		//EventRewardSystem.getInstance().updateRewardInDb(activeEditingEvent, activeEditingReward);
	}
	
	private void editReward(PlayerEventInfo gm, String parameter, String action)
	{
		RewardItem item = EventRewardSystem.getInstance().getReward(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId, _editingInfos.get(gm.getPlayersId()).activeEditingReward);
		
		if(item == null)
		{
			gm.sendMessage("This item doesn't exist.");
			return;
		}
		
		PositionContainer container = EventRewardSystem.getInstance().getRewardPosition(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId, _editingInfos.get(gm.getPlayersId()).activeEditingReward);
		
		EventRewardSystem.getInstance().removeFromDb(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, container.position, container.parameter, _editingInfos.get(gm.getPlayersId()).activeEventModeId, item.id, item.minAmmount, item.maxAmmount, item.chance);
		
		if(action.equals("set_item_id"))
		{
			if(parameter.startsWith("exp"))
				item.id = -1;
			else if(parameter.startsWith("sp"))
				item.id = -2;
			else if(parameter.startsWith("fame"))
				item.id = -3;
			else
				item.id = Integer.parseInt(parameter);
		}
		else if(action.equals("set_min"))
		{
			item.minAmmount = Integer.parseInt(parameter);
		}
		else if(action.equals("set_max"))
		{
			item.maxAmmount = Integer.parseInt(parameter);
		}
		else if(action.equals("set_chance"))
		{
			item.chance = Integer.parseInt(parameter);
		}
		
		EventRewardSystem.getInstance().addRewardToDb(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, container.position, container.parameter, _editingInfos.get(gm.getPlayersId()).activeEventModeId, item.id, item.minAmmount, item.maxAmmount, item.chance, true);
	}
	
	private void addEventReward(PlayerEventInfo gm)
	{
		if(_editingInfos.get(gm.getPlayersId()).activeEditingEvent == null)
		{
			gm.sendMessage("This event doesn't exist.");
			return;
		}
		
		if(_editingInfos.get(gm.getPlayersId()).activeEditingRewardPos == RewardPosition.None)
		{
			gm.sendMessage("Select a position first.");
			showRewardsEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle(), _editingInfos.get(gm.getPlayersId()).activeEditingRewardPos, _editingInfos.get(gm.getPlayersId()).activeEditingRewardParam);
			return;
		}
		
		int rewardId = EventRewardSystem.getInstance().createReward(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEditingRewardPos, _editingInfos.get(gm.getPlayersId()).activeEditingRewardParam, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		_editingInfos.get(gm.getPlayersId()).activeEditingReward = rewardId;
		
		showRewardEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingReward);
	}
	
	private void showRewardEditation(PlayerEventInfo gm, int rewardId)
	{
		if(_editingInfos.get(gm.getPlayersId()).activeEditingEvent == null)
		{
			gm.sendMessage("This event doesn't exist.");
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editreward.htm");
		
		html = html.replaceAll("%eventName%", _editingInfos.get(gm.getPlayersId()).activeEditingEvent.toString());
		
		RewardItem item = EventRewardSystem.getInstance().getReward(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId, rewardId);
		
		if(item == null)
		{
			gm.sendMessage("This item doesn't exist.");
			return;
		}
		
		_editingInfos.get(gm.getPlayersId()).activeEditingReward = rewardId;

		html = html.replaceAll("%id%", String.valueOf(item.id));
		html = html.replaceAll("%min%", String.valueOf(item.minAmmount));
		html = html.replaceAll("%max%", String.valueOf(item.maxAmmount));
		html = html.replaceAll("%chance%", String.valueOf(item.chance));

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showRewardsEditation(PlayerEventInfo gm, String event, RewardPosition position, String parameter)
	{
		EventType type = null;
		for(EventType t : EventType.values())
		{
			if(t.toString().equalsIgnoreCase(event) || t.getAltTitle().equalsIgnoreCase(event))
				type = t;
		}
		
		if(type == null || type == EventType.Unassigned)
		{
			gm.sendMessage("This event doesn't exist.");
			return;
		}
		
		Configurable eventInstance;
		
		if(!type.isRegularEvent())
			eventInstance = EventManager.getInstance().getMiniEvent(type, 1);
		else
			eventInstance = EventManager.getInstance().getMainEvent(type);
		
		_editingInfos.get(gm.getPlayersId()).activeEditingRewardPos = position;
		_editingInfos.get(gm.getPlayersId()).activeEditingRewardParam = parameter;
		_editingInfos.get(gm.getPlayersId()).activeEditingEvent = type;
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editrewards.htm");
		
		html = html.replaceAll("%eventName%", type.getAltTitle());
		html = html.replaceAll("%eventId%", String.valueOf(type.getId()));
		
		String itemName;
		
		String modesPanel;
		
		TextBuilder tb;
		
		if(!type.isRegularEvent())
		{
			tb = new TextBuilder();
			
			tb.append("<table width=270>");
			tb.append("<tr>");
			tb.append("<td width=60><font color=ac9775><a action=\"bypass admin_event_manage mini_set_active_mode reward Default\">Modes:</a></font></td>");
			tb.append("<td width=100><combobox width=88 height=17 var=ebox list=%modes%></td>");
			tb.append("<td width=40><button value=\"Set\" action=\"bypass admin_event_manage mini_set_active_mode reward \\$ebox\" width=45 height=19 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("<td width=40><button value=\"New\" action=\"bypass admin_event_manage mini_edit_modes_new\" width=45 height=19 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr>");
			tb.append("</table>");
			
			modesPanel = tb.toString();
		}
		else 
			modesPanel = "";
		
		html = html.replaceAll("%modesPanel%", modesPanel);
		
		if(!type.isRegularEvent())
		{
			MiniEventManager manager = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
			
			if(manager == null)
			{
				gm.sendMessage("This mini event doesn't exist.");
				return;
			}
			
			tb = new TextBuilder();
			
			tb.append(manager.getMode().getModeName() + ";");
			
			for(Entry<Integer, MiniEventManager> e : EventManager.getInstance().getMiniEvents().get(_editingInfos.get(gm.getPlayersId()).activeEditingEvent).entrySet())
			{
				if(!e.getValue().getMode().getModeName().equals(manager.getMode().getModeName()))
				{
					tb.append(e.getValue().getMode().getModeName() + ";");
				}
			}
			
			String result = tb.toString();
			html = html.replaceAll("%modes%", result.substring(0, result.length() - 1));
		}
		else
			html = html.replaceAll("%modes%", "N/A");
		
		tb = new TextBuilder();
		
		
		EventRewards rewards = EventRewardSystem.getInstance().getAllRewardsFor(type, _editingInfos.get(gm.getPlayersId()).activeEventModeId);
		int i = 0;
		
		if(rewards == null)
		{
			
		}
		else
		{
			int size = 0;
			
			tb.append("<table bgcolor=4F4F4F width=280>");
			for(Entry<PositionContainer, Map<Integer, RewardItem>> e : rewards.getAllRewards().entrySet())
			{
				if(e.getKey().position == null || e.getKey().position.posType == null)
					continue;
				
				if(i == 0)
				{
					tb.append("<tr>");
				}
				
				tb.append("<td width = 54>");
				tb.append("<font color=" + ((e.getKey().position == position && (e.getKey().parameter == null || e.getKey().parameter.equals(parameter))) ? "LEVEL" : "9f9f9f") + "><a action=\"bypass admin_event_manage edit_event_reward_menu " + type.getAltTitle() + " " + e.getKey().position.toString() + " " + (e.getKey().parameter == null ? "" : e.getKey().parameter) + "\">");
				
				if(e.getKey().position.posType == PositionType.General)
				{
					size += e.getKey().position.toString().length() + 9;
					tb.append(e.getKey().position.toString() + " [" + e.getValue().size() + " it.] ");
				}
				else if(e.getKey().position.posType == PositionType.Numbered && e.getKey().position == RewardPosition.KillingSpree)
				{
					size += e.getKey().parameter.length() + 10;
					tb.append("KS-" + e.getKey().parameter + " [" + e.getValue().size() + " it.] ");
				}
				else if(e.getKey().position.posType == PositionType.Numbered) // not killing spree
				{
					size += e.getKey().parameter.length() + 10;
					tb.append(e.getKey().parameter + ". [" + e.getValue().size() + " it.] ");
				}
				else if(e.getKey().position.posType == PositionType.Range)
				{
					size += e.getKey().parameter.length() + 9;
					tb.append(e.getKey().parameter + " [" + e.getValue().size() + " it.] ");
				}
				else if(e.getKey().position.posType == PositionType.EventSpecific)
				{
					size += e.getKey().position.toString().length() + 9;
					tb.append(e.getKey().position + " [" + e.getValue().size() + " it.] ");
				}
				tb.append("</a></font></td>");
				
				i++;
				
				//NexusLoader.debug("size after " + e.getKey().position.posType + " = " + size);
				if(i == 5 || size >= 40)
				{
					tb.append("</tr>");
					size = 0;
					i = 0;
				}
			}
			tb.append("</table>");
		}
		
		html = html.replaceAll("%rewardedPositions%", tb.toString());
		
		
		tb = new TextBuilder();

		if(position != RewardPosition.None)
		{
			for(Map.Entry<Integer, RewardItem> e : EventRewardSystem.getInstance().getRewards(type, _editingInfos.get(gm.getPlayersId()).activeEventModeId, position, parameter).entrySet())
			{
				if(e.getValue().id == -1)
					itemName = "XP";
				else if(e.getValue().id == -2)
					itemName = "SP";
				else if(e.getValue().id == -3)
					itemName = "Fame";
				else
				{
					itemName = CallBack.getInstance().getOut().getItemName(e.getValue().id);
				}
				
				tb.append("<button value=\"" + itemName + " (" + e.getValue().minAmmount + "-" + e.getValue().maxAmmount + ")" + "\" action=\"bypass admin_event_manage edit_event_reward " + e.getKey() + "\" width=200 height=19 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				tb.append("<br1>");
			}
		}
		else
		{
			tb.append("Select a position.");
		}
		
		html = html.replaceAll("%rewards%", tb.toString());
		

		if(position == RewardPosition.None)
			html = html.replaceAll("%pos%", "N/A");
		else
			html = html.replaceAll("%pos%", position.toString());
		
		html = html.replaceAll("%parameter%", parameter == null ? "" : parameter);
		
		String desc = eventInstance.getDescriptionForReward(position);
		html = html.replaceAll("%desc%", desc == null ? position.description : desc);
		
		tb = new TextBuilder();
		
		for(RewardPosition rewardType : eventInstance.getRewardTypes())
		{
			tb.append(rewardType + ";");
		}
		
		String s = tb.toString();
		html = html.replaceAll("%rewardTypes%", s.substring(0, s.length() - 1));
		
		html = html.replaceAll("%enableDisable%", EventConfig.getInstance().isEventAllowed(type) ? "<font color=74BE85><a action=\"bypass admin_event_manage set_aviable\">Enabled</a></font>" : "<font color=B46F6B><a action=\"bypass admin_event_manage set_aviable\">Disabled</a></font>");
		
		tb = new TextBuilder();
		
		tb.append(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection + ";");
		for(String page : type.isRegularEvent() ? mainEventEditingPages : miniEventEditingPages)
		{
			if(!page.equals(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection))
				tb.append(page + ";");
		}
		
		String result = tb.toString();
		
		html = html.replaceAll("%event_pages%", result.substring(0, result.length() - 1));

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	public void showAddPositionToRewardedWindow(PlayerEventInfo gm)
	{
		if(_editingInfos.get(gm.getPlayersId()).activeEditingEvent == null)
		{
			gm.sendMessage("This event doesn't exist.");
			return;
		}
		
		Configurable eventInstance;
		
		if(!_editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent())
			eventInstance = EventManager.getInstance().getMiniEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, 1);
		else
			eventInstance = EventManager.getInstance().getMainEvent(_editingInfos.get(gm.getPlayersId()).activeEditingEvent);
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editrewards_addpos.htm");
		
		html = html.replaceAll("%eventName%", _editingInfos.get(gm.getPlayersId()).activeEditingEvent.toString());
		
		html = html.replaceAll("%enableDisable%", EventConfig.getInstance().isEventAllowed(_editingInfos.get(gm.getPlayersId()).activeEditingEvent) ? "<font color=74BE85><a action=\"bypass admin_event_manage set_aviable\">Enabled</a></font>" : "<font color=B46F6B><a action=\"bypass admin_event_manage set_aviable\">Disabled</a></font>");
		
		TextBuilder tb = new TextBuilder();
		
		tb.append(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection + ";");
		for(String page : _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent() ? mainEventEditingPages : miniEventEditingPages)
		{
			if(!page.equals(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection))
				tb.append(page + ";");
		}
		
		String result = tb.toString();
		
		html = html.replaceAll("%event_pages%", result.substring(0, result.length() - 1));
		html = html.replaceAll("%eventId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEditingEvent.getId()));
		
		tb = new TextBuilder();
		
		int total = 0;
		int count = 0;
		boolean b = true;
		
		String general = "";
		for(RewardPosition pos : eventInstance.getRewardTypes())
		{
			if(pos.posType != null && pos.posType == PositionType.General)
			{
				if(EventRewardSystem.getInstance().getAllRewardsFor(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId).getContainer(pos, null) == null)
				{
					count ++;
					general += pos.toString() + ";";
				}
			}
		}
		
		if(count > 0)
		{
			total ++;
			
			if(general.length() > 0)
				general = general.substring(0, general.length() - 1);
			
			tb.append("<table width=280><tr>");
			tb.append("<td width=90 align=left><font color=9f9f9f><a action=\"bypass admin_event_manage show_add_position_window_help\">General:</a></font></td>");
			tb.append("<td width=160 align=right><combobox width=150 height=17 var=gen list=\"" + general + "\"></td>");
			tb.append("<td width=40 align=right><button value=\"Add\" action=\"bypass admin_event_manage add_rewarded_position \\$gen\" width=40 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr></table>");
			
			tb.append("<br>");
		}
		else b = true;
		
		count = 0;
		
		for(RewardPosition pos : eventInstance.getRewardTypes())
		{
			if(pos.posType != null && pos.posType == PositionType.Numbered && pos != RewardPosition.KillingSpree)
				count ++;
		}
		
		if(count > 0)
		{
			total ++;
			
			tb.append("<table width=280><tr>");
			tb.append("<td width=150 align=left><font color=9f9f9f><a action=\"bypass admin_event_manage show_add_position_window_help\">Numbered:</a></font></td>");
			tb.append("<td align=right><edit var=\"num\" width=35 height=13></td>");
			tb.append("<td align=left>.</td>");
			tb.append("<td width=40 align=right><button value=\"Add\" action=\"bypass admin_event_manage add_rewarded_position Numbered \\$num\" width=40 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr></table>");
			
			tb.append("<br>");
		}
		
		
		// killing spree rewards
		count = 0;
		
		for(RewardPosition pos : eventInstance.getRewardTypes())
		{
			if(pos.posType != null && pos.posType == PositionType.Numbered && pos == RewardPosition.KillingSpree)
				count ++;
		}
		
		if(count > 0)
		{
			total ++;
			
			tb.append("<table width=280><tr>");
			tb.append("<td width=90 align=left><font color=9f9f9f><a action=\"bypass admin_event_manage show_add_position_window_help\">Killing Spree:</a></font></td>");
			tb.append("<td align=right><edit var=\"num\" width=35 height=13></td>");
			tb.append("<td align=left>kills in a row</td>");
			tb.append("<td width=40 align=right><button value=\"Add\" action=\"bypass admin_event_manage add_rewarded_position KillingSpree \\$num\" width=40 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr></table>");
			
			tb.append("<br>");
		}
		// -
		
		count = 0;
		
		for(RewardPosition pos : eventInstance.getRewardTypes())
		{
			if(pos.posType != null && pos.posType == PositionType.Range)
			{
				count ++;
			}
		}
		
		if(count > 0)
		{
			total ++;
			
			tb.append("<table width=280><tr>");
			tb.append("<td width=150 align=left><font color=9f9f9f><a action=\"bypass admin_event_manage show_add_position_window_help\">Range:</a></font></td>");
			tb.append("<td align=right><edit var=\"ran1\" width=40 height=13></td>");
			tb.append("<td align=left> - </td>");
			tb.append("<td align=left><edit var=\"ran2\" width=40 height=13></td>");
			
			tb.append("<td align=right><button value=\"Add\" action=\"bypass admin_event_manage add_rewarded_position Range \\$ran1 \\$ran2\" width=40 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr></table>");
			
			tb.append("<br>");
		}
		
		count = 0;
		String bonus = "";
		for(RewardPosition pos : eventInstance.getRewardTypes())
		{
			if(pos.posType != null && pos.posType == PositionType.EventSpecific)
			{
				if(EventRewardSystem.getInstance().getAllRewardsFor(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId).getContainer(pos, null) == null)
				{
					count ++;
					bonus += pos.toString() + ";";
				}
			}
		}
		
		if(count > 0)
		{
			total ++;
			
			if(bonus.length() > 0)
				bonus = bonus.substring(0, bonus.length() - 1);
			
			tb.append("<table width=280><tr>");
			tb.append("<td width=90 align=left><font color=9f9f9f><a action=\"bypass admin_event_manage show_add_position_window_help\">Custom:</a></font></td>");
			tb.append("<td width=160 align=right><combobox width=150 height=17 var=bon list=\"" + bonus + "\"></td>");
			tb.append("<td width=40 align=right><button value=\"Add\" action=\"bypass admin_event_manage add_rewarded_position \\$bon\" width=40 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr></table>");
			
			tb.append("<br>");
		}
		
		html = html.replaceAll("%general%", tb.toString());
		
		if(total == 0)
		{
			html = html.replaceAll("%general%", "<font color=9f9f9f>All aviable positions have been added already.</font>");
		}
		else if(b)
		{
			html = html.replaceAll("%general%", "");
		}
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	public void showAddPositionToRewardedWindowHelp(PlayerEventInfo gm)
	{
		if(_editingInfos.get(gm.getPlayersId()).activeEditingEvent == null)
		{
			gm.sendMessage("This event doesn't exist.");
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editrewards_addpos_help.htm");
		
		html = html.replaceAll("%eventName%", _editingInfos.get(gm.getPlayersId()).activeEditingEvent.toString());
		
		html = html.replaceAll("%enableDisable%", EventConfig.getInstance().isEventAllowed(_editingInfos.get(gm.getPlayersId()).activeEditingEvent) ? "<font color=74BE85><a action=\"bypass admin_event_manage set_aviable\">Enabled</a></font>" : "<font color=B46F6B><a action=\"bypass admin_event_manage set_aviable\">Disabled</a></font>");
		
		TextBuilder tb = new TextBuilder();
		
		tb.append(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection + ";");
		for(String page : _editingInfos.get(gm.getPlayersId()).activeEditingEvent.isRegularEvent() ? mainEventEditingPages : miniEventEditingPages)
		{
			if(!page.equals(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection))
				tb.append(page + ";");
		}
		
		String result = tb.toString();
		
		html = html.replaceAll("%event_pages%", result.substring(0, result.length() - 1));
		html = html.replaceAll("%eventId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEditingEvent.getId()));
		
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	public void addPositionToRewarded(PlayerEventInfo gm, RewardPosition position, String param)
	{
		if(EventRewardSystem.getInstance().setPositionRewarded(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId, position, param))
		{
			gm.sendMessage("The position has been added.");
			//TODO add name of the position
		}
		else
		{
			gm.sendMessage("This position has been added already.");
		}
		
		showRewardsEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle(), _editingInfos.get(gm.getPlayersId()).activeEditingRewardPos, _editingInfos.get(gm.getPlayersId()).activeEditingRewardParam);
	}
	
	public void removePositionFromRewarded(PlayerEventInfo gm, RewardPosition position, String param)
	{
		if(EventRewardSystem.getInstance().removePositionRewarded(_editingInfos.get(gm.getPlayersId()).activeEditingEvent, _editingInfos.get(gm.getPlayersId()).activeEventModeId, _editingInfos.get(gm.getPlayersId()).activeEditingRewardPos, _editingInfos.get(gm.getPlayersId()).activeEditingRewardParam))
		{
			gm.sendMessage("The position has been removed");
		}
		else
		{
			gm.sendMessage("This position doesn't exist in the list.");
		}
		
		_editingInfos.get(gm.getPlayersId()).activeEditingRewardPos = RewardPosition.None;
		_editingInfos.get(gm.getPlayersId()).activeEditingRewardParam = null;
		
		showRewardsEditation(gm, _editingInfos.get(gm.getPlayersId()).activeEditingEvent.getAltTitle(), _editingInfos.get(gm.getPlayersId()).activeEditingRewardPos, _editingInfos.get(gm.getPlayersId()).activeEditingRewardParam);
	}
	
	private int newSpawn(PlayerEventInfo gm)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return -1;
		}
		
		if(map.getEvents().contains(EventType.TreasureHunt) || map.getEvents().contains(EventType.TreasureHuntPvp))
		{
			if(map.getSpawns().size() >= 355)
			{
				gm.sendMessage("Maximimum count of spawns for Treasure-type events per map is 355.");
				return -1;
			}
		}
		else
		{
			if(map.getSpawns().size() >= 90)
			{
				gm.sendMessage("Maximimum count of spawns per map is 60.");
				return -1;
			}
		}
		
		int newId = map.getNewSpawnId();
		
		EventSpawn spawn = new EventSpawn(_editingInfos.get(gm.getPlayersId()).activeEditingMap, newId, new Loc(gm.getX(), gm.getY(), gm.getZ()), 0, "RegularSpawn");

		if(_editingInfos.get(gm.getPlayersId()).activeEditingEvent == EventType.TreasureHunt || _editingInfos.get(gm.getPlayersId()).activeEditingEvent == EventType.TreasureHuntPvp)
			spawn.setType("Chest");
		
		spawn.setSaved(false);
		
		List<EventSpawn> list = new FastList<EventSpawn>();
		list.add(spawn);
		
		map.addSpawns(list);

		_editingInfos.get(gm.getPlayersId()).activeEditingSpawn = newId;
		
		return newId;
	}
	
	/** returns false if map is for Mini events */
	private boolean saveMap(PlayerEventInfo gm)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return true;
		}
		
		checkMap(gm, map);
		
		EventMapSystem.getInstance().addMapToDb(map, false);
		
		// strange code, but should work
		for(EventType type : map.getEvents())
		{
			if(type.isRegularEvent())
				return true;
		}
		
		return false;
	}

	private void checkMap(PlayerEventInfo gm, EventMap map)
	{
		map.checkMap(gm);
	}
	
	private void saveSpawn(PlayerEventInfo gm)
	{
		boolean isFence = false;
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return;
		}
		
		EventSpawn spawn = map.getSpawn(_editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
		
		if(spawn == null)
		{
			gm.sendMessage("The spawn with ID " + _editingInfos.get(gm.getPlayersId()).activeEditingSpawn + " doesn't exist.");
			return;
		}
		
		if(spawn.getSpawnType() == SpawnType.Door)
		{
			if(!CallBack.getInstance().getOut().doorExists(spawn.getDoorId()))
			{
				gm.sendMessage("Door with ID " + spawn.getDoorId() + " DOESN'T exist! Spawn won't be saved until you fix it.");
				return;
			}
		}
		else if(spawn.getSpawnType() == SpawnType.Flag)
		{
			if(spawn.getNote() == null || spawn.getNote().equals(""))
			{
				gm.sendMessage("Spawn's NOTE is not specified!!");
				gm.sendMessage("Put in spawn's note flag's heading, or select default value (click on it \"note\").");
				return;
			}
		}
		else if(spawn.getSpawnType() == SpawnType.Fence)
		{
			if(spawn.getNote() == null || spawn.getNote().equals(""))
			{
				gm.sendMessage("Spawn's NOTE is not specified!!");
				gm.sendMessage("Spawn's note defines fence's WIDTH and LENGTH.");
				gm.sendMessage("Separate both values by empty space, example \"200 300\" will spawn fence with width = 200 and length = 300.");
				return;
			}
			
			try
			{
				StringTokenizer st = new StringTokenizer(spawn.getNote());
				int width = Integer.parseInt(st.nextToken());
				int length = Integer.parseInt(st.nextToken());
				gm.sendMessage("Fence's Width = " + width + ", Length = " + length + ".");
			} 
			catch (Exception e)
			{
				gm.sendMessage("Spawn's NOTE is not specified!!");
				gm.sendMessage("Spawn's note defines fence's WIDTH and LENGTH.");
				gm.sendMessage("Separate both values by empty space, example \"200 300\" will spawn fence with width = 200 and length = 300.");
				return;
			}
			isFence = true;
		}
		
		if(spawn.isSaved())
			gm.sendMessage("Spawn saved.");
		else
			gm.sendMessage("Spawn successfully sent to database.");
		if(isFence)
		{
			EventMapSystem.getInstance().addSpawnToDb(spawn);
		}
		EventMapSystem.getInstance().addSpawnToDb(spawn);
	}
	
	private void editSpawn(PlayerEventInfo gm, String parameter, String action)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return;
		}
		
		EventSpawn spawn = map.getSpawn(_editingInfos.get(gm.getPlayersId()).activeEditingSpawn);
		
		if(spawn == null)
		{
			gm.sendMessage("The spawn with ID " + _editingInfos.get(gm.getPlayersId()).activeEditingSpawn + " doesn't exist.");
			return;
		}
		
		if(action.startsWith("set_id"))
		{
			spawn.setId(Integer.parseInt(parameter));
			_editingInfos.get(gm.getPlayersId()).activeEditingSpawn = spawn.getSpawnId();
		}
		else if(action.startsWith("set_team"))
		{
			spawn.setTeamId(Integer.parseInt(parameter));
		}
		else if(action.startsWith("set_x"))
		{
			spawn.setX(Integer.parseInt(parameter));
		}
		else if(action.startsWith("set_y"))
		{
			spawn.setY(Integer.parseInt(parameter));
		}
		else if(action.startsWith("set_z"))
		{
			spawn.setZ(Integer.parseInt(parameter));
		}
		else if(action.startsWith("set_wawenumber"))
		{
			if(spawn.getNote() == null)
				spawn.setNote("20001 1 First");
			
			try
			{
				StringTokenizer st = new StringTokenizer(spawn.getNote());
				
				String mobId = st.nextToken();
				String mobAmmount = st.nextToken();
				
				spawn.setNote(mobId + " " + mobAmmount + " " + parameter);
			} 
			catch (Exception e)
			{
				gm.sendMessage("Error while changing wawe number for this mob spawn: " + e.toString());
				spawn.setNote("20001 1 First");
				return;
			}
		}
		else if(action.startsWith("set_mobid"))
		{
			if(spawn.getNote() == null)
				spawn.setNote("20001 1 First");
			
			int id = Integer.parseInt(parameter);
			
			NpcTemplateData t = new NpcTemplateData(id);
			
			if(!t.exists())
				gm.sendMessage("Mob with ID " + id + " DOESN'T exist! Please fix it.");

			try
			{
				StringTokenizer st = new StringTokenizer(spawn.getNote());
				
				st.nextToken();
				String mobAmmount = st.nextToken();
				String wawe = st.nextToken();
				
				spawn.setNote(id + " " + mobAmmount + " " + wawe);
			} 
			catch (Exception e)
			{
				gm.sendMessage("Error while changing mob id for this mob spawn: " + e.toString());
				spawn.setNote("20001 1 First");
				return;
			}
		}
		else if(action.startsWith("set_mobammount"))
		{
			if(spawn.getNote() == null)
				spawn.setNote("20001 1 First");
			
			int ammount = Integer.parseInt(parameter);

			try
			{
				StringTokenizer st = new StringTokenizer(spawn.getNote());
				
				String mobId = st.nextToken();
				st.nextToken();
				String wawe = st.nextToken();
				
				spawn.setNote(mobId + " " + ammount + " " + wawe);
			} 
			catch (Exception e)
			{
				gm.sendMessage("Error while changing mob ammount for this mob spawn: " + e.toString());
				spawn.setNote("20001 1 First");
				return;
			}
		}
		else if(action.startsWith("set_dooraction_init"))
		{
			if(spawn.getNote() == null)
				spawn.setNote("Default Default");
			
			try
			{
				StringTokenizer st = new StringTokenizer(spawn.getNote());

				String initAction = parameter;
				
				st.nextToken();
				String startAction = st.nextToken();
				
				spawn.setNote(initAction + " " + startAction);
			} 
			catch (Exception e)
			{
				gm.sendMessage("Error while changing actions for this door: " + e.toString());
				spawn.setNote("Default Default");
				return;
			}
		}
		else if(action.startsWith("set_dooraction_start"))
		{
			if(spawn.getNote() == null)
				spawn.setNote("Default Default");
			
			try
			{
				StringTokenizer st = new StringTokenizer(spawn.getNote());

				String initAction = st.nextToken();
				String startAction = parameter;
				
				spawn.setNote(initAction + " " + startAction);
			} 
			catch (Exception e)
			{
				gm.sendMessage("Error while changing actions for this door: " + e.toString());
				spawn.setNote("Default Default");
				return;
			}
		}
		else if(action.startsWith("set_base_respawnhere"))
		{
			if(spawn.getSpawnType() == SpawnType.Base)
			{
				boolean b = Boolean.parseBoolean(parameter);
				
				spawn.setRespawnHere(b);
			}
		}
		else if(action.startsWith("set_importance"))
		{
			if(spawn.getSpawnType() == SpawnType.Base)
			{
				try
				{
					spawn.setImportance(Integer.parseInt(parameter));
				}
				catch (Exception e)
				{
					gm.sendMessage("Must be a number");
				}
			}
		}
		else if(action.startsWith("set_width"))
		{
			if(spawn.getNote() == null)
				spawn.setNote("100 100");
			
			try
			{
				StringTokenizer st = new StringTokenizer(spawn.getNote());

				String width = parameter;
				
				st.nextToken();
				String length = st.nextToken();
				
				spawn.setNote(width + " " + length);
			} 
			catch (Exception e)
			{
				gm.sendMessage("Error while changing width / length for this fence: " + e.toString());
				spawn.setNote("100 100");
				EventManager.getInstance().debug(e);
				return;
			}
		}
		else if(action.startsWith("set_length"))
		{
			if(spawn.getNote() == null)
				spawn.setNote("100 100");
			
			try
			{
				StringTokenizer st = new StringTokenizer(spawn.getNote());
				
				String width = st.nextToken();
				String length = parameter;
				
				spawn.setNote(width + " " + length);
			} 
			catch (Exception e)
			{
				gm.sendMessage("Error while changing width / length for this fence: " + e.toString());
				spawn.setNote("100 100");
				EventManager.getInstance().debug(e);
				return;
			}
		}
		else if(action.startsWith("default_loc"))
		{
			int x = gm.getX();
			int y = gm.getY();
			int z = gm.getZ();
			
			/*if(spawn.getSpawnType() == SpawnType.Door)
			{
				if(gm.getTarget() != null && gm.getTarget() instanceof L2DoorInstance)
				{
					x = ((L2DoorInstance)gm.getTarget()).getDoorId();
					y = 0;
					z = 0;
				}
			}*/
			
			spawn.setX(x);
			spawn.setY(y);
			spawn.setZ(z);
		}
		else if(action.startsWith("set_note"))
		{
			spawn.setNote(parameter.equals("null") ? "" : parameter);
		}
		else if(action.startsWith("set_npc_id"))
		{
			try
			{
				int id = Integer.parseInt(parameter);
				
				NpcTemplateData t = new NpcTemplateData(id);
				if(!t.exists())
				{
					gm.sendMessage("NPC ID " + id + " doesn't exist.");
					return;
				}
				else
				{
					spawn.setNote(String.valueOf(id));
				}
			}
			catch (Exception e)
			{
				gm.sendMessage("The NPC ID must be a number.");
			}
		}
		else if(action.startsWith("set_type"))
		{
			spawn.setType(parameter);
			
			if(spawn.getSpawnType() == SpawnType.Door)
			{
				spawn.setY(0);
				spawn.setZ(0);
				
				if(gm.getTarget() != null && gm.getTarget().isDoor())
				{
					spawn.setX(gm.getTarget().getDoorData().getDoorId());
					gm.sendMessage("Setted Door ID from you target.");
				}
			}
			else if(spawn.getSpawnType() == SpawnType.Npc)
			{
				if(gm.getTarget() != null && gm.getTarget().isNpc())
				{
					spawn.setNote(String.valueOf(gm.getTarget().getNpc().getNpcId()));
					gm.sendMessage("Setted NPC ID from you target.");
				}
			}
			else if(spawn.getSpawnType() == SpawnType.Flag)
			{
				if(spawn.getNote() == null)
				{
					spawn.setNote("" + gm.getHeading());
					gm.sendMessage("Filled spawn's note by your current heading.");
				}
			}
		}
		
		if(spawn.getSpawnType() == SpawnType.Fence)
		{
			if(spawn.getSpawnTeam() != 0)
				spawn.setTeamId(0);
			
			if(spawn.getNote() == null)
				spawn.setNote("100 100");
		}
		else if(spawn.getSpawnType() == SpawnType.Door)
		{
			if(spawn.getSpawnTeam() != 0)
				spawn.setTeamId(0);
			
			if(spawn.getNote() == null)
				spawn.setNote("Default Default");
		}
		else if(spawn.getSpawnType() == SpawnType.Npc)
		{
			if(spawn.getSpawnTeam() != 0)
				spawn.setTeamId(0);
			
			if(spawn.getNote() == null)
				spawn.setNote("-1");
		}
		else if(spawn.getSpawnType() == SpawnType.Monster)
		{
			if(spawn.getNote() == null)
				spawn.setNote("20001 1 First");
		}
		
		map.removeSpawn(spawn.getSpawnId(), false);
		
		List<EventSpawn> list = new FastList<EventSpawn>();
		list.add(spawn);
		
		// reorganize spawns in the collection
		map.addSpawns(list);

		gm.sendMessage("Done.");
	}
	
	private void showEditSpawnMenu(PlayerEventInfo gm, int spawnId)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return;
		}
		
		EventSpawn spawn = map.getSpawn(spawnId);
		
		if(spawn == null)
		{
			gm.sendMessage("The spawn with ID " + spawnId + " doesn't exist.");
			return;
		}
		
		_editingInfos.get(gm.getPlayersId()).activeEditingSpawn = spawn.getSpawnId();
		
		String html;
		
		switch(spawn.getSpawnType())
		{
			case Base:
			{
				html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editspawn_base.htm");
				
				if(spawn.getNote() == null || spawn.getNote().equals(""))
					spawn.setNote("1-false");
				
				break;
			}
			case Door:
				html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editspawn_door.htm");
				
				if(spawn.getNote() == null)
					spawn.setNote("Default Default");
				
				try
				{
					String temp;
					
					StringTokenizer st = new StringTokenizer(spawn.getNote());
					String init = st.nextToken();
					
					TextBuilder tb = new TextBuilder();
					tb.append(init + ";");
					
					for(DoorAction a : DoorAction.values())
					{
						if(!init.equals(a.toString()))
						{
							tb.append(a.toString() + ";");
						}
					}
					
					temp = tb.toString();
					html = html.replaceAll("%actions_init%", temp.substring(0, temp.length() - 1));
					
					String start = st.nextToken();
					tb = new TextBuilder();
					tb.append(start + ";");
					
					for(DoorAction a : DoorAction.values())
					{
						if(!start.equals(a.toString()))
						{
							tb.append(a.toString() + ";");
						}
					}
					
					temp = tb.toString();
					html = html.replaceAll("%actions_start%", temp.substring(0, temp.length() - 1));
				} 
				catch (Exception e)
				{
					gm.sendMessage("Error while changing door's actions. Reverted to default values.");
					spawn.setNote("Default Default");
					return;
				}

				break;
			case Fence:
				html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editspawn_fence.htm");
				
				String width;
				String length;
				if(spawn.getNote() == null)
				{
					width = "100";
					length = "100";
				}
				else
				{
					try
					{
						StringTokenizer st = new StringTokenizer(spawn.getNote());
						width = st.nextToken();
						length = st.nextToken();
					} 
					catch (Exception e)
					{
						gm.sendMessage("Error, wrong length / width format for this fence. Values setted back to their default.");
						width = "100";
						length = "100";
					}

				}
				
				html = html.replaceAll("%width%", width);
				html = html.replaceAll("%length%", length);
				
				break;
			case Npc:
				html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editspawn_npc.htm");
				
				int id = spawn.getNpcId();
				html = html.replaceAll("%id%", String.valueOf(id));
				
				break;
			case Monster:
			case Boss:
				//TODO: survival arena
				/*if(map.getEvents().contains(EventType.SurvivalArena))
				{
					if(spawn.getNote() == null)
					{
						spawn.setNote("20001 1 First");
					}
					
					html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editspawn_monster_survival.htm");
					
					String mobId;
					String mobAmmount;
					
					try
					{
						mobId = String.valueOf(SurvivalArenaGame.getMobId(spawn.getNote()));
						mobAmmount = String.valueOf(SurvivalArenaGame.getMobsCount(spawn.getNote()));
					} 
					catch (Exception e)
					{
						spawn.setNote("20001 1 First");
						mobId = String.valueOf(SurvivalArenaGame.getMobId(spawn.getNote()));
						mobAmmount = String.valueOf(SurvivalArenaGame.getMobsCount(spawn.getNote()));
					}
					
					html = html.replaceAll("%mobId%", mobId);
					html = html.replaceAll("%mobAmmount%", mobAmmount);

					try
					{
						TextBuilder tb = new TextBuilder();
						String waweNum = SurvivalArenaGame.getWaweName(spawn.getNote());
						tb.append(waweNum + ";");
						
						for(RoundName r : RoundName.values())
						{
							if(!r.toString().equals(waweNum))
								tb.append(r.toString() + ";");
						}
						
						String s = tb.toString();

						html = html.replaceAll("%wawe_numbers%", s.substring(0, s.length() - 1));
					} 
					catch (Exception e2)
					{
						gm.sendMessage("An error occured, reseting back to default values.");
						spawn.setNote("20001 1 First");
						return;
					}
				}
				else*/
					html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editspawn_monster.htm");
				
				break;
			default:
				html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editspawn.htm");
		}

		String noteDefault = "null";
		
		if(spawn.getSpawnType() == SpawnType.Flag)
		{
			noteDefault = "" + gm.getHeading();
		}
		else if(spawn.getSpawnType() == SpawnType.Fence)
		{
			noteDefault = "100 100";
		}
		else if(spawn.getSpawnType() == SpawnType.Door)
		{
			noteDefault = "Default Default";
		}

		html = html.replaceAll("%note_default%", noteDefault);
		
		html = html.replaceAll("%mapId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEditingMap));
		html = html.replaceAll("%mapName%", map.getMapName());
		html = html.replaceAll("%id%", String.valueOf(spawn.getSpawnId()));
		html = html.replaceAll("%team%", String.valueOf(spawn.getSpawnTeam()));
		html = html.replaceAll("%x%", String.valueOf(spawn.getLoc().getX()));
		html = html.replaceAll("%y%", String.valueOf(spawn.getSpawnType() != SpawnType.Door ? spawn.getLoc().getY() : "N/A"));
		html = html.replaceAll("%z%", String.valueOf(spawn.getSpawnType() != SpawnType.Door ? spawn.getLoc().getZ() : "N/A"));
		html = html.replaceAll("%note%", spawn.getNote() == null ? " " : spawn.getNote());
		html = html.replaceAll("%type%", spawn.getSpawnType().toString());
		
		TextBuilder tb = new TextBuilder();
		tb.append(spawn.getSpawnType().toString() + ";");
		
		for(SpawnType type : SpawnType.values())
		{
			if(type != spawn.getSpawnType() && type.isForEvents(map.getEvents()))
			{
				tb.append(type.toString() + ";");
			}
		}
		
		String s = tb.toString();
		html = html.replaceAll("%types%", s.substring(0, s.length() - 1));
		
		html = html.replaceAll("%save_close%", spawn.isSaved() ? "Close" : "* Save *");

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void removeSpawn(PlayerEventInfo gm, int spawnId)
	{	
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return;
		}
		
		try
		{
			showSpawn(gm, spawnId, false, false, true);
		}
		catch (Exception e)
		{
		}
		
		if(map.removeSpawn(spawnId, true))
			gm.sendMessage("Spawn with ID " + spawnId + " was successfully removed.");
		else
			gm.sendMessage("The spawn with ID " + spawnId + " doesn't exist.");
	}
	
	private void showAllSpawns(PlayerEventInfo gm)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return;
		}
		
		for(EventSpawn spawn : map.getSpawns())
		{
			showSpawn(gm, spawn.getSpawnId(), false, false, false);
		}
	}
	
	private NpcData showSpawn(PlayerEventInfo gm, int spawnId, boolean onlyShow, boolean onlyDummy, boolean onlyDespawn)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return null;
		}
		
		EventSpawn spawn = map.getSpawn(spawnId);
		
		// already spawned
		if(_editingInfos.get(gm.getPlayersId()).spawned.get(map.getGlobalId()) != null && _editingInfos.get(gm.getPlayersId()).spawned.get(map.getGlobalId()).get(spawn.getSpawnId()) != null)
		{
			if(onlyShow)
				return null;
			
			ObjectData o = _editingInfos.get(gm.getPlayersId()).spawned.get(map.getGlobalId()).get(spawn.getSpawnId());
			if(o.isFence())
			{
				FenceData fence = o.getFence();
				fence.deleteMe();
			}
			else if(o.isNpc())
			{
				NpcData npc = o.getNpc();
				npc.deleteMe();
			}
			
			_editingInfos.get(gm.getPlayersId()).spawned.get(map.getGlobalId()).remove(spawn.getSpawnId());
		}
		else
		{
			if(onlyDespawn)
				return null;
			
			if(spawn != null && spawn.getSpawnType() != SpawnType.Door && spawn.getSpawnType() != SpawnType.Fence)
			{
				NpcTemplateData template = new NpcTemplateData(TRAINING_DUMMY_ID);
				
				if(template != null)
				{
					try
					{
						NpcData npc = template.doSpawn(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ(), 1, gm.getHeading(), 0, gm.getInstanceId());
						
						npc.setTitle("ID: " + spawn.getSpawnId() + "/Team: " + spawn.getSpawnTeam());
						npc.setName(spawn.getSpawnType().toString() + " spawn");
						
						npc.broadcastNpcInfo();
						
						if(_editingInfos.get(gm.getPlayersId()).spawned.get(map.getGlobalId()) == null)
							_editingInfos.get(gm.getPlayersId()).spawned.put(map.getGlobalId(), new FastMap<Integer, ObjectData>());
						
						_editingInfos.get(gm.getPlayersId()).spawned.get(map.getGlobalId()).put(spawn.getSpawnId(), npc.getObjectData());
						
						npc.broadcastSkillUse(npc, npc, 5965, 1);
						
						return npc;
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				
				gm.sendMessage("Done.");
			}
			else if(spawn.getSpawnType() == SpawnType.Fence)
			{
				if(onlyDummy)
					return null;
				
				FenceData fence = CallBack.getInstance().getOut().createFence(2, spawn.getFenceWidth(), spawn.getFenceLength(), spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ(), 0);
				List<FenceData> f = new FastList<FenceData>();
				f.add(fence);
				CallBack.getInstance().getOut().spawnFences(f, gm.getInstanceId());

				if(_editingInfos.get(gm.getPlayersId()).spawned.get(map.getGlobalId()) == null)
					_editingInfos.get(gm.getPlayersId()).spawned.put(map.getGlobalId(), new FastMap<Integer, ObjectData>());
				
				_editingInfos.get(gm.getPlayersId()).spawned.get(map.getGlobalId()).put(spawn.getSpawnId(), fence);
			}
			else
				gm.sendMessage("Spawn can't be spawned or this ID " + spawnId + " doesn't exist.");
		}
		
		return null;
	}
	
	private void teleportToSpawn(PlayerEventInfo gm, int spawnId)
	{	
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return;
		}
		
		EventSpawn spawn = map.getSpawn(spawnId);
		
		if(spawn != null)
		{
			gm.teleToLocation(spawn.getLoc(), false);
			gm.sendMessage("Done.");
		}
		else
			gm.sendMessage("The spawn with ID " + spawnId + " doesn't exist.");
	}
	
	private boolean removeMap(PlayerEventInfo gm, int mapId)
	{
		boolean mainEventMap = false;
		EventMap map = EventMapSystem.getInstance().getMapById(mapId);
		for(EventType type : map.getEvents())
		{
			if(type.isRegularEvent())
				mainEventMap = true;
		}
		
		if(EventMapSystem.getInstance().removeMap(mapId))
			gm.sendMessage("Done.");
		else
			gm.sendMessage("Map with id " + mapId + " doesn't exist.");
		
		return mainEventMap;
	}
	
	private void sortSpawns(PlayerEventInfo gm, String sortType)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist.");
			return;
		}

		if(sortType.equals("id"))
		{
			if(_editingInfos.get(gm.getPlayersId()).asc)
				Collections.sort(map.getSpawns(), EventMap.compareByIdDesc);
			else
				Collections.sort(map.getSpawns(), EventMap.compareByIdAsc);
			
			_editingInfos.get(gm.getPlayersId()).asc = !_editingInfos.get(gm.getPlayersId()).asc;
			gm.sendMessage("Done, spawns sorted " + (_editingInfos.get(gm.getPlayersId()).asc ? "ascending" : "descending") + " by their ID.");
		}
		else if(sortType.equals("type"))
		{
			Collections.sort(map.getSpawns(), EventMap.compareByType);
			gm.sendMessage("Done, spawns sorted by their type priority.");
		}
	}
	
	private void showFilterMenu(PlayerEventInfo gm)
	{
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_filter_menu.htm");

		html = html.replaceAll("%mapId%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEditingMap));

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void filterSpawns(PlayerEventInfo gm, String s)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist.");
			return;
		}
		
		SpawnType type = null;
		
		if(!s.equals("All"))
		{
			for(SpawnType t : SpawnType.values())
			{
				if(t.toString().equalsIgnoreCase(s))
				{
					type = t;
					break;
				}
			}
		}
		
		_editingInfos.get(gm.getPlayersId()).spawnFilter = type;
		gm.sendMessage("Done, spawns filtered by " + s + ".");
	}
	
	private void showConfigsMenu(PlayerEventInfo gm, String type, int page)
	{
		EventType eventType = EventType.getType(type);
		
		if(eventType == null || eventType == EventType.Unassigned)
		{
			gm.sendMessage("Event " + type + " doesn't exist.");
			return;
		}
		
		_editingInfos.get(gm.getPlayersId()).activeEditingEvent = eventType;
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_config_menu.htm");
		
		TextBuilder tb = new TextBuilder();
		
		Configurable event;
		
		if(!eventType.isRegularEvent())
			event = EventManager.getInstance().getMiniEvent(eventType, 1);
		else
			event = EventManager.getInstance().getMainEvent(eventType);
		
		String temp = _editingInfos.get(gm.getPlayersId()).activeEditingConfigCategory;
		String category = null;
		
		for(String c : event.getCategories())
		{
			if(c.equals(temp))
			{
				category = c;
				break;
			}
		}
		
		if(category == null)
			category = "General";
		
		boolean expanded;
		
		List<ConfigModel> configs = new FastList<ConfigModel>();
		
		for(Entry<String, ConfigModel> e : event.getConfigs().entrySet())
		{
			if(e.getValue().getCategory().equals(category))
				configs.add(e.getValue());
		}
		
		final int pageLimit = NexusLoader.isLimitedHtml() ? 10 : 25;
		int counter = 0;
		int maxPages = (int) Math.ceil((double) configs.size() / pageLimit);
		
		if(page > maxPages)
			page = maxPages;
		
		int showFrom = (page-1) * pageLimit;
		int showTo = page * pageLimit - 1;
		
		if(_editingInfos.get(gm.getPlayersId()).currentConfigModelPage > maxPages)
			_editingInfos.get(gm.getPlayersId()).currentConfigModelPage = 1;
		
		for(ConfigModel config : configs)
		{
			if(counter < showFrom || counter > showTo)
			{
				counter ++;
				continue;
			}
			
			counter ++;
			
			if(_editingInfos.get(gm.getPlayersId()).currentConfigModelShownCategory.equals("Event") && _editingInfos.get(gm.getPlayersId()).currentConfigModelShown != null && 
					_editingInfos.get(gm.getPlayersId()).currentConfigModelShown.equals(config.getKey()))
					expanded = true;
			else
				expanded = false;
			
			if(expanded)
				tb.append("<table width=280 bgcolor=599944>");
			else
				tb.append("<table width=280 bgcolor=3f3f3f>");
			
			tb.append("<tr>");
			
			tb.append("<td width=175 align=left><font color=ac9887> " + config.getKey() + "</font></td>");
			
			String value = config.getValue();
			
			if(value.length() > 6)
				value = "...";
			
			boolean brackets = true;
			if(value.length() >= 6)
				brackets = false;
			
			tb.append("<td width=45 align=left><font color=9f9f9f>" + (brackets ? "(" : "") + "" + value + "" + (brackets? ")" : "") + "</font></td>");
			tb.append("<td width=50 align=right><button value=\"Expand\" width=55 action=\"bypass admin_event_manage expand_configmodel Event " + config.getKey() + "\" height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			
			tb.append("</tr>");
			
			tb.append("</table>");
			
			if(expanded)
			{
				tb.append("<table width=278 bgcolor=2f2f2f>");
				tb.append("<tr>");
				tb.append("<td width=240><font color=9f9f9f>" + config.getDesc() + "</font></td>");
				tb.append("</tr>");
				tb.append("</table>");
				
				if(config.getInput() == InputType.MultiEdit || config.getInput() == InputType.MultiAdd || config.getValue().length() > 5)
				{
					tb.append("<table width=278 bgcolor=2f2f2f>");
					tb.append("<tr>");
					tb.append(config.getValueShownInHtml());
					tb.append("</tr>");
					tb.append("</table>");
				}
				
				tb.append("<table width=280 bgcolor=2f2f2f>");
				tb.append("<tr>");
				tb.append("<td>" + config.getInputHtml(220) + "</td>");
				tb.append("<td align=left><button value=\"" + config.getAddButtonName() + "\" width=40 action=\"bypass admin_event_manage " + config.getAddButtonAction() + "_config " + config.getKey() + " \\$" + config.getKey() + "\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				
				tb.append("</tr>");
				tb.append("</table>");
				
				tb.append("<table width=280 bgcolor=2f2f2f>");
				tb.append("<tr>");
				tb.append("<td><font color=6f6f6f>" + config.getConfigHtmlNote() + "</font></td>");
				tb.append("<td align=right><button value=\"" + config.getUtilButtonName() + "\" action=\"bypass admin_event_manage set_config " + config.getKey() + " " + config.getDefaultVal() + "\" width=" + config.getUtilButtonWidth() + " height=17 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				tb.append("</tr>");
				tb.append("</table>");
			}
			
			tb.append("<img src=\"L2UI.SquareBlank\" width=1 height=4>");
		}
		
		html = html.replaceAll("%currentPage%", String.valueOf(page));
		html = html.replaceAll("%event%", eventType.getHtmlTitle());
		html = html.replaceAll("%type%", eventType.getAltTitle());
		html = html.replaceAll("%eventId%", String.valueOf(eventType.getId()));
		html = html.replaceAll("%configs%", tb.toString());
		
		if(maxPages > 1)
		{
			tb = new TextBuilder();
			
			tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=5>");
			tb.append("<table bgcolor=292929 width=290>");
			for(int i = 0; i < maxPages; i++)
			{
				if(i % 4 == 0)
					tb.append("<tr>");
				
				tb.append("<td align=center width=72><font color=" + (page == i+1 ? "80B382" : "9f9f9f") + "><a action=\"bypass admin_event_manage event_configs_menu_page " + (i + 1) + "\">Page " + (i+1) + "</a></font></td>");
				
				if(i > 1 && (i + 1) % 4 == 0)
					tb.append("</tr>");
			}
			tb.append("</table>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=5>");
			
			html = html.replaceAll("%pages%", tb.toString());
		}
		else
			html = html.replaceAll("%pages%", "<br>");
		
		if(!eventType.isRegularEvent())
			html = html.replaceAll("%editEvent%", "mini_edit_event");
		else
			html = html.replaceAll("%editEvent%", "edit_event");
		
		html = html.replaceAll("%enableDisable%", EventConfig.getInstance().isEventAllowed(eventType) ? "<font color=74BE85><a action=\"bypass admin_event_manage set_aviable\">Enabled</a></font>" : "<font color=B46F6B><a action=\"bypass admin_event_manage set_aviable\">Disabled</a></font>");
		
		tb = new TextBuilder();
		
		tb.append(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection + ";");
		for(String s : eventType.isRegularEvent() ? mainEventEditingPages : miniEventEditingPages)
		{
			if(!s.equals(_editingInfos.get(gm.getPlayersId()).activeEventEditingSection))
				tb.append(s + ";");
		}
		
		String result = tb.toString();
		html = html.replaceAll("%event_pages%", result.substring(0, result.length() - 1));
		
		String header;
		
		if(event.getCategories().isEmpty())
			header = "<br1><font color=ac9887>" + eventType.getHtmlTitle() + "</font> <font color=9f9f9f>configs page " + page + "</font><br1>";
		else
		{
			tb = new TextBuilder();
			
			tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=6>");
			tb.append("<table width=290 bgcolor=363636>");
			tb.append("<tr>");
			
			if(category.equals("General"))
				tb.append("<td align=center><font color=D89C67><a action=\"bypass admin_event_manage show_configs_category General\">[General]</a></font></td>");
			else
				tb.append("<td align=center><font color=7f7f7f><a action=\"bypass admin_event_manage show_configs_category General\">[General]</a></font></td>");
			
			int i = 0;
			for(String cat : event.getCategories())
			{
				i++;
				if(category.equals(cat))
					tb.append("<td align=center><font color=D89C67><a action=\"bypass admin_event_manage show_configs_category " + cat + "\">[" + cat + "]</a></font></td>");
				else 
					tb.append("<td align=center><font color=7f7f7f><a action=\"bypass admin_event_manage show_configs_category " + cat + "\">[" + cat + "]</a></font></td>");
				
				if(i == 3)
				{
					tb.append("<tr>");
					tb.append("</tr>");
					i = 0;
				}
			}
			
			tb.append("</tr>");
			tb.append("</table>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=6>");
			
			header = tb.toString();
		}
		
		html = html.replaceAll("%header%", header);
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showConfig(PlayerEventInfo gm, String key)
	{
		gm.sendMessage("weird call");
		
		
		/*EventType eventType = _editingInfos.get(gm.getPlayersId()).activeEditingEvent;
		
		if(eventType == null)
			return;
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_config_show.htm");
		
		html = html.replaceAll("%key%", key);
		
		String value = EventConfig.getInstance().getConfig(eventType, key);
		if(value == null)
			value = "<font color=B46F6B>Not Specified!</font>";
		
		html = html.replaceAll("%value%", value);
		
		String desc;
		if(eventType.isRegularEvent() == false)
			desc = EventManager.getInstance().getMiniEvent(eventType, 1).getConfigs().get(key);
		else
			desc = EventManager.getInstance().getMainEvent(eventType).getConfigs().get(key);
		
		html = html.replaceAll("%desc%", desc == null ? "" : desc);
		
		html = html.replaceAll("%event%", eventType.getHtmlTitle());
		html = html.replaceAll("%type%", eventType.getAltTitle());
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();*/
	}
	
	private void setConfig(PlayerEventInfo gm, String key, String value, boolean addToValue)
	{
		EventType eventType = _editingInfos.get(gm.getPlayersId()).activeEditingEvent;
		
		EventConfig.getInstance().addConfig(eventType, key, value, addToValue);
		gm.sendMessage("Done. Key " + key + " has now value " + value + ".");
	}
	
	private void showConfigMenu(PlayerEventInfo gm, String type)
	{
		EventType event = EventType.getType(type);
		
		if(event == null)
		{
			gm.sendMessage("This event doesn't exist.");
			return;
		}
		
		if(event.isRegularEvent())
		{
			gm.sendMessage("Sorry, this function isn't implemented for Main events yet.");
			return;
		}
		
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist.");
			return;
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mapconfig_menu.htm");

		TextBuilder tb = new TextBuilder();
		
		boolean expanded = false;
		
		int size = 0;
		for(Entry<String, ConfigModel> e : map.getConfigModels().get(event).entrySet())
		{
			if(_editingInfos.get(gm.getPlayersId()).currentConfigModelShownCategory.equals("MapConfig") && _editingInfos.get(gm.getPlayersId()).currentConfigModelShown != null && 
					_editingInfos.get(gm.getPlayersId()).currentConfigModelShown.equals(e.getValue().getKey()))
					expanded = true;
			else
				expanded = false;
			
			if(expanded)
				tb.append("<table width=280 bgcolor=599944>");
			else
				tb.append("<table width=280 bgcolor=3f3f3f>");
			
			tb.append("<tr>");
			
			tb.append("<td width=175 align=left><font color=ac9887> " + e.getValue().getKey() + "</font></td>");
			
			String value = e.getValue().getValue();
			if(value.length() > 6)
				value = "...";
			
			boolean brackets = true;
			if(value.length() >= 6)
				brackets = false;
			
			tb.append("<td width=45 align=left><font color=9f9f9f>" + (brackets ? "(" : "") + "" + value + "" + (brackets? ")" : "") + "</font></td>");
			tb.append("<td width=50 align=right><button value=\"Expand\" width=55 action=\"bypass admin_event_manage expand_configmodel MapConfig " + e.getKey() + "\" height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			
			tb.append("</tr>");
			
			tb.append("</table>");
			
			if(expanded)
			{
				tb.append("<table width=278 bgcolor=2f2f2f>");
				tb.append("<tr>");
				tb.append("<td width=240><font color=9f9f9f>" + e.getValue().getDesc() + "</font></td>");
				tb.append("</tr>");
				tb.append("</table>");
				
				if(e.getValue().getInput() == InputType.MultiEdit || e.getValue().getInput() == InputType.MultiAdd || e.getValue().getValue().length() > 5)
				{
					tb.append("<table width=278 bgcolor=2f2f2f>");
					tb.append("<tr>");
					tb.append(e.getValue().getValueShownInHtml());
					tb.append("</tr>");
					tb.append("</table>");
				}
				
				tb.append("<table width=280 bgcolor=2f2f2f>");
				tb.append("<tr>");
				tb.append("<td>" + e.getValue().getInputHtml(180) + "</td>");
				tb.append("<td align=left><button value=\"" + e.getValue().getAddButtonName() + "\" width=40 action=\"bypass admin_event_manage " + e.getValue().getAddButtonAction() + "_map_config " + event.getAltTitle() + " " + e.getKey() + " \\$" + e.getKey() + "\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				
				tb.append("</tr>");
				tb.append("</table>");
				
				tb.append("<table width=280 bgcolor=2f2f2f>");
				tb.append("<tr>");
				tb.append("<td><font color=6f6f6f>" + e.getValue().getConfigHtmlNote() + "</font></td>");
				tb.append("<td align=right><button value=\"" + e.getValue().getUtilButtonName() + "\" action=\"bypass admin_event_manage set_map_config " + event.getAltTitle() + " " + e.getKey() + " " + e.getValue().getDefaultVal() + "\" width=" + e.getValue().getUtilButtonWidth() + " height=17 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				tb.append("</tr>");
				tb.append("</table>");
			}
			
			tb.append("<img src=\"L2UI.SquareBlank\" width=1 height=4>");
			
			size ++;
		}
		
		/*String[] configs = map.getAviableConfigs(event);
		if(configs == null)
		{
			gm.sendMessage("No configs found.");
			return;
		}
		
		String value;
		int size = 0;
		boolean bg = true;
		
		for(String key : configs)
		{
			value = EventConfig.getInstance().getMapConfig(map.getGlobalId(), event, key);
			if(value == null)
				value = "<font color=B46F6B>N/A</font>";
			
			tb.append("<table width=280" + (bg ? " bgcolor=333333" : "") + ">");
			
			tb.append("<tr>");
			tb.append("<td align=left><font color=ac9887><a action=\"bypass admin_event_manage show_map_config false " + event.getAltTitle() + " " + key + "\">" + key + "</a></font></td>");
			tb.append("<td align=right><font color=9f9f9f><a action=\"bypass admin_event_manage show_map_config false " + event.getAltTitle() + " " + key + "\">" + value + "</a></font></td>");
			//tb.append("<td align=right><button value=\"Edit\" action=\"bypass admin_event_manage show_map_config " + event.getAltTitle() + " " + key + "\" width=35 height=17 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			tb.append("</tr>");
			
			tb.append("</table>");
			
			size++;
			bg = !bg;
		}*/
		
		if(size == 0)
			tb.append("No configs aviable for this event.");
		
		html = html.replaceAll("%configs%", tb.toString());
		
		html = html.replaceAll("%mapName%", map.getMapName());
		html = html.replaceAll("%event%", event.getAltTitle());
		
		html = html.replaceAll("%mapId%", String.valueOf(map.getGlobalId()));
		html = html.replaceAll("%page%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEditingPage));

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void setMapConfig(PlayerEventInfo gm, String type, String key, String value, boolean addToValue)
	{
		EventType event = EventType.getType(type);
		
		if(event == null)
		{
			gm.sendMessage("This event doesn't exist.");
			return;
		}
		
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist.");
			return;
		}
		
		EventConfig.getInstance().setMapConfig(map, event, key, value, addToValue);
		map.setSaved(false);
	}
	
	private void showMapStatus(PlayerEventInfo gm, int mapId, String event)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(mapId);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + mapId + ").");
			return;
		}
		
		_editingInfos.get(gm.getPlayersId()).activeEditingMap = map.getGlobalId();
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_mapstatus.htm");
		
		html = html.replaceAll("%mapName%", map.getMapName());
		html = html.replaceAll("%mapId%", "" + map.getGlobalId());
		html = html.replaceAll("%previous_event%", event);
		
		String notWorkingEvents = null;
		String spawns = null;
		
		notWorkingEvents = map.getNotWorkingEvents();
		spawns = map.getMissingSpawns();
		
		html = html.replaceAll("%notWorkingEvents%", notWorkingEvents);
		html = html.replaceAll("%spawns%", spawns);
		
		if(spawns.length() > 1)
			html = html.replaceAll("%status%", "<font color=B46F6B>This map can't be played unless you fix it.</font>");
		else
			html = html.replaceAll("%status%", "<font color=74BE85>This map is active and can be played.</font>");
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showExpandSpawnEffect(final PlayerEventInfo gm, int spawnId)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return;
		}
		
		final EventSpawn spawn = map.getSpawn(spawnId);
		
		if(spawn == null)
			return;
		
		gm.addRadarMarker(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ());
		
		NpcData data = showSpawn(gm, spawn.getSpawnId(), true, true, false);
		
		if(data == null)
			return;
		
		CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				showSpawn(gm, spawn.getSpawnId(), false, true, false);
			}
		}, 500);
	}
	
	private void showMapEditationAddEvent(PlayerEventInfo gm, int mapId)
	{
		EventMap map = null;
		
		if(mapId > 0)
		{
			map = EventMapSystem.getInstance().getMapById(mapId);
			
			if(map == null)
			{
				gm.sendMessage("This map doesn't exist. (" + mapId + ").");
				return;
			}
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editmap_add_event.htm");

		TextBuilder tb = new TextBuilder();
		
		tb.append("<table width=270>");
		
		int counter = 0;
		
		for(EventType aviable : EventType.values())
		{
			if(aviable.allowEdits() && aviable != EventType.Unassigned)
			{
				if(!map.getEvents().contains(aviable))
				{
					if(EventManager.getInstance().getEvent(aviable) != null)
					{
						counter ++;
						
						if(counter == 1)
							tb.append("<tr>");
						
						tb.append("<td><button value=\"" + aviable.getAltTitle() + "\" action=\"bypass admin_event_manage add_event_to_map " + aviable.getAltTitle() + "\" width=85 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
						
						if(counter == 3)
						{
							tb.append("</tr>");
							counter = 0;
						}
					}
				}
			}
		}
		
		if(counter != 0)
			tb.append("</tr>");
		
		tb.append("</table>");
		
		String events = tb.toString();
		
		html = html.replaceAll("%events%", events);
		
		html = html.replaceAll("%name%", map.getMapName());
		html = html.replaceAll("%save_close%", map.isSaved() ? "Close" : "* Save *");
		
		html = html.replaceAll("%mapId%", String.valueOf(map.getGlobalId()));
		html = html.replaceAll("%page%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEditingPage));
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showMapEditationInfo(PlayerEventInfo gm, EventMap map)
	{
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editmap_info.htm");

		TextBuilder tb = new TextBuilder();
		
		EventType event = _editingInfos.get(gm.getPlayersId()).activeEditingMapEvent;
		
		if(event != null)
			tb.append(event.getAltTitle() + ";");
		
		for(EventType t : map.getEvents())
		{
			if((event != null && t != event) || event == null)
				tb.append(t.getAltTitle() + ";");
		}
		
		String events = tb.toString();
		
		if(events.length() > 0)
			events = events.substring(0, events.length() - 1);
		
		html = html.replaceAll("%events%", events);
		
		html = html.replaceAll("%name%", map.getMapName());
		html = html.replaceAll("%desc%", (map.getMapDesc() == null || map.getMapDesc().length() == 0) ? "This map has no description set." : map.getMapDesc());
		html = html.replaceAll("%save_close%", map.isSaved() ? "Close" : "* Save *");
		
		html = html.replaceAll("%mapId%", String.valueOf(map.getGlobalId()));
		html = html.replaceAll("%page%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEditingPage));
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showMapEditationEvents(PlayerEventInfo gm, int mapId)
	{
		EventMap map = null;
		
		if(mapId > 0)
		{
			map = EventMapSystem.getInstance().getMapById(mapId);
			
			if(map == null)
			{
				gm.sendMessage("This map doesn't exist. (" + mapId + ").");
				return;
			}
		}
		
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editmap_events.htm");

		TextBuilder tb = new TextBuilder();
		
		EventType event = _editingInfos.get(gm.getPlayersId()).activeEditingMapEvent;
		
		boolean selected = false;
		
		if(event != null && map.getEvents().contains(event))
		{
			tb.append(event.getAltTitle() + ";");
			selected = true;
		}
		
		for(EventType t : map.getEvents())
		{
			if((event != null && t != event) || event == null)
				tb.append(t.getAltTitle() + ";");
		}
		
		String events = tb.toString();
		
		if(events.length() > 0)
			events = events.substring(0, events.length() - 1);
		
		html = html.replaceAll("%events%", events);
		
		html = html.replaceAll("%name%", map.getMapName());
		html = html.replaceAll("%save_close%", map.isSaved() ? "Close" : "* Save *");
		
		// CONFIGS
		
		tb = new TextBuilder();
		
		if(selected)
		{
			boolean expanded = false;
			
			int size = 0;
			for(Entry<String, ConfigModel> e : map.getConfigModels().get(event).entrySet())
			{
				if(_editingInfos.get(gm.getPlayersId()).currentConfigModelShownCategory.equals("MapConfig") && _editingInfos.get(gm.getPlayersId()).currentConfigModelShown != null && 
						_editingInfos.get(gm.getPlayersId()).currentConfigModelShown.equals(e.getValue().getKey()))
						expanded = true;
				else
					expanded = false;
				
				if(expanded)
					tb.append("<table width=280 bgcolor=599944>");
				else
					tb.append("<table width=280 bgcolor=3f3f3f>");
				
				tb.append("<tr>");
				
				tb.append("<td width=175 align=left><font color=ac9887> " + e.getValue().getKey() + "</font></td>");
				
				String value = e.getValue().getValue();
				if(value.length() > 6)
					value = "...";
				
				boolean brackets = true;
				if(value.length() >= 6)
					brackets = false;
				
				tb.append("<td width=45 align=left><font color=9f9f9f>" + (brackets ? "(" : "") + "" + value + "" + (brackets? ")" : "") + "</font></td>");
				tb.append("<td width=50 align=right><button value=\"Expand\" width=55 action=\"bypass admin_event_manage expand_configmodel MapConfig " + e.getKey() + "\" height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				
				tb.append("</tr>");
				
				tb.append("</table>");
				
				if(expanded)
				{
					tb.append("<table width=278 bgcolor=2f2f2f>");
					tb.append("<tr>");
					tb.append("<td width=240><font color=9f9f9f>" + e.getValue().getDesc() + "</font></td>");
					tb.append("</tr>");
					tb.append("</table>");
					
					if(e.getValue().getInput() == InputType.MultiEdit || e.getValue().getInput() == InputType.MultiAdd || e.getValue().getValue().length() > 5)
					{
						tb.append("<table width=278 bgcolor=2f2f2f>");
						tb.append("<tr>");
						tb.append(e.getValue().getValueShownInHtml());
						tb.append("</tr>");
						tb.append("</table>");
					}
					
					tb.append("<table width=280 bgcolor=2f2f2f>");
					tb.append("<tr>");
					tb.append("<td>" + e.getValue().getInputHtml(180) + "</td>");
					tb.append("<td align=left><button value=\"" + e.getValue().getAddButtonName() + "\" width=40 action=\"bypass admin_event_manage " + e.getValue().getAddButtonAction() + "_map_config " + event.getAltTitle() + " " + e.getKey() + " \\$" + e.getKey() + "\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					
					tb.append("</tr>");
					tb.append("</table>");
					
					tb.append("<table width=280 bgcolor=2f2f2f>");
					tb.append("<tr>");
					tb.append("<td><font color=6f6f6f>" + e.getValue().getConfigHtmlNote() + "</font></td>");
					tb.append("<td align=right><button value=\"" + e.getValue().getUtilButtonName() + "\" action=\"bypass admin_event_manage set_map_config " + event.getAltTitle() + " " + e.getKey() + " " + e.getValue().getDefaultVal() + "\" width=" + e.getValue().getUtilButtonWidth() + " height=17 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					tb.append("</tr>");
					tb.append("</table>");
				}
				
				tb.append("<img src=\"L2UI.SquareBlank\" width=1 height=4>");
				
				size ++;
			}
			
			if(size == 0)
				tb.append("No configs aviable for this event.");
		}
		else
			tb.append("<font color=B46F6B>Select an event.</font>");
		
		html = html.replaceAll("%configs%", tb.toString());
		
		html = html.replaceAll("%mapId%", String.valueOf(map.getGlobalId()));
		html = html.replaceAll("%page%", String.valueOf(_editingInfos.get(gm.getPlayersId()).activeEditingPage));
		
		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void showMapEditation(PlayerEventInfo gm, int mapId, int page, EventType predefinedEvent)
	{
		EventMap map = null;
		
		if(mapId > 0)
		{
			map = EventMapSystem.getInstance().getMapById(mapId);
			
			if(map == null)
			{
				gm.sendMessage("This map doesn't exist. (" + mapId + ").");
				return;
			}
		}
		
		if(map == null)
		{
			List<EventType> defaultEvent = new FastList<EventType>();
			defaultEvent.add(EventType.Unassigned);
			
			map = new EventMap(EventMapSystem.getInstance().getNewMapId(), "New Map", "", defaultEvent, null, "");
			map.setSaved(false);
			
			EventMapSystem.getInstance().addMap(map);
			
			if(predefinedEvent != null)
			{
				map.addEvent(predefinedEvent);
				map.removeEvent(EventType.Unassigned);
			}
			
			_editingInfos.get(gm.getPlayersId()).activeEditingMapPage = "Info";
		}

		_editingInfos.get(gm.getPlayersId()).activeEditingPage = page;
		_editingInfos.get(gm.getPlayersId()).activeEditingMap = map.getGlobalId();
		
		if(_editingInfos.get(gm.getPlayersId()).activeEditingMapPage != null)
		{
			if(_editingInfos.get(gm.getPlayersId()).activeEditingMapPage.equals("Events"))
			{
				showMapEditationEvents(gm, mapId);
				return;
			}
			else if(_editingInfos.get(gm.getPlayersId()).activeEditingMapPage.equals("Info"))
			{
				showMapEditationInfo(gm, map);
				return;
			}
		}
		
		// html part
		
		TextBuilder tb = new TextBuilder();
		
		if(_editingInfos.get(gm.getPlayersId()).spawnFilter == null)
			tb.append("All;");
		else
			tb.append(_editingInfos.get(gm.getPlayersId()).spawnFilter.toString() + ";");
		
		String filters;
		for(SpawnType t : SpawnType.values())
		{
			if(_editingInfos.get(gm.getPlayersId()).spawnFilter != null && _editingInfos.get(gm.getPlayersId()).spawnFilter == t)
				continue;
			
			for(EventSpawn spawn : map.getSpawns())
			{
				if(spawn.getSpawnType() == t)
				{
					tb.append(t.toString() + ";");
					break;
				}
			}
		}
		
		if(_editingInfos.get(gm.getPlayersId()).spawnFilter != null)
			tb.append("All;");
		
		filters = tb.toString();
		filters = filters.substring(0, filters.length() - 1);
		
		boolean filter = _editingInfos.get(gm.getPlayersId()).spawnFilter != null;
		String spawnUnspawn;

		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_editmap_spawns.htm");
		
		tb = new TextBuilder();

		tb.append("<table width=275 bgcolor=\"666666\">>");
		tb.append("<tr>" +
				"<td width=20><font color=D5D5D5><a action=\"bypass admin_event_manage sort_map_spawns id\">ID</a></font></td>" +
				"<td width=40 align=center><font color=D5D5D5><a action=\"bypass admin_event_manage sort_map_spawns type\">Type</a></font></td>" +
				"<td width=70 align=center><font color=D5D5D5><a action=\"bypass admin_event_manage sort_map_spawns team\">Team</a></font></td>" +
				"<td width=80 align=right><combobox width=80 height=15 var=filt list=\"" + filters + "\"></td>" +
				"<td width=50 align=right><button value=\"Filter\" action=\"bypass admin_event_manage filter_event_spawns \\$filt\" width=50 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" +
				"</tr>");
		tb.append("</table>");
		
		
		int size;
		if(filter)
			size = map.getSpawns(-1, _editingInfos.get(gm.getPlayersId()).spawnFilter).size();
		else
			size = map.getSpawns().size();
		
		final int maxSpawnsPerPage = 15;
		int maxPages = size / maxSpawnsPerPage;
		
		int firstSpawnShowed;
		int lastSpawnShowed;
		
		if(page > maxPages)
			page = maxPages;
		
		firstSpawnShowed = page * maxSpawnsPerPage;
		lastSpawnShowed = size;
		
		if(lastSpawnShowed - firstSpawnShowed > maxSpawnsPerPage)
			lastSpawnShowed = firstSpawnShowed + maxSpawnsPerPage;
		
		int count = 0;
		
		Color color;
		String bgcolor = "444444";
		
		EventType type;
		String desc;

		for(EventSpawn spawn : map.getSpawns())
		{
			if(filter && spawn.getSpawnType() != _editingInfos.get(gm.getPlayersId()).spawnFilter)
				continue;
			
			count++;
			if(count > firstSpawnShowed && count <= lastSpawnShowed)
			{
				if(_editingInfos.get(gm.getPlayersId()).spawned.get(map.getGlobalId()) != null && _editingInfos.get(gm.getPlayersId()).spawned.get(map.getGlobalId()).get(spawn.getSpawnId()) != null)
					spawnUnspawn = "Hide";
				else
					spawnUnspawn = "Show";
				
				int team = spawn.getSpawnTeam();
				
				//if(spawn.getSpawnType() == SpawnType.Regular || spawn.getSpawnType() == SpawnType.Safe)
				{
					if(team == 0)
						bgcolor = "858585";
					else if(team == -1)
						bgcolor = "949494";
					else
						bgcolor = EventManager.getInstance().getTeamColorForHtml(team);
					
					color = new Color(Integer.decode("0x" + bgcolor));
					
					if(_editingInfos.get(gm.getPlayersId()).activeEditingSpawn != spawn.getSpawnId())
						color = color.darker().darker();
					
					bgcolor = Integer.toHexString(0xFF000000 | color.getRGB()).substring(2);
				}
				
				tb.append("<table width=275 bgcolor=" + bgcolor + ">");
				tb.append("<tr>");
				
				tb.append("<td width=15><a action=\"bypass admin_event_manage expand_spawn_info " + spawn.getSpawnId() + "\"><font color=" + spawn.getSpawnType().getHtmlColor() + ">" + spawn.getSpawnId() + "</a></font></td>");
				tb.append("<td width=50><font color=" + spawn.getSpawnType().getHtmlColor() + ">" + spawn.getSpawnType().toString() + "</font></td>"); // type
				tb.append("<td width=15 align=left><font color=" + EventManager.getInstance().getTeamColorForHtml(team) + ">T" + spawn.getSpawnTeam() + "</font></td>"); // team
				tb.append("<td width=60 align=right><button value=\"" + spawnUnspawn + "\" action=\"bypass admin_event_manage show_spawn " + spawn.getSpawnId() + "\" width=50 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>"); // show
				tb.append("<td align=right><button value=\"Expand\" action=\"bypass admin_event_manage expand_spawn_info " + spawn.getSpawnId() + "\" width=60 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>"); // expand
				
				//tb.append("<font color=" + spawn.getSpawnType().getHtmlColor() + ">" + spawn.getSpawnTeam() + "|" + spawn.getLoc().getX() + ";" + spawn.getLoc().getY() + ";" + spawn.getLoc().getZ() + "|</font></td>");
				
				tb.append("</tr>");
				tb.append("</table>");
				
				if(_editingInfos.get(gm.getPlayersId()).activeEditingSpawn == spawn.getSpawnId())
				{
					if(team == 0)
						bgcolor = "949494";
					else if(team == -1)
						bgcolor = "949494";
					else
						bgcolor = EventManager.getInstance().getTeamColorForHtml(team);
					
					color = new Color(Integer.decode("0x" + bgcolor));
					
					if(team == 0 || team == -1)
						bgcolor = Integer.toHexString(0xFF000000 | color.darker().getRGB()).substring(2);
					else
						bgcolor = Integer.toHexString(0xFF000000 | color.darker().darker().getRGB()).substring(2);
					
					tb.append("<table width=270 bgcolor=" + bgcolor + ">");
					tb.append("<tr>");
					
					type = map.getEvents().get(0);
					
					if(type == EventType.Unassigned)
					{
						desc = spawn.getSpawnType().getDefaultDesc();
					}
					else
					{
						Configurable event;
						event = EventManager.getInstance().getEvent(type);
						
						desc = event.getAviableSpawnTypes().get(spawn.getSpawnType());
					}
					
					if(desc == null)
						desc = spawn.getSpawnType().getDefaultDesc();
					
					desc = desc.replaceAll("%TEAM%", "" + spawn.getSpawnTeam());
					
					tb.append("<td width=150><font color=9f9f9f>" + desc + "</font></td>");
					tb.append("</tr>");
					tb.append("</table>");
					
					tb.append("<table width=270 bgcolor=" + bgcolor + ">");
					tb.append("<tr>");
					tb.append("<td><button value=\"Delete\" action=\"bypass admin_event_manage remove_spawn " + spawn.getSpawnId() + "\" width=83 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					tb.append("<td><button value=\"Teleport to\" action=\"bypass admin_event_manage teleport_spawn " + spawn.getSpawnId() + "\" width=83 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					tb.append("<td><button value=\"Edit spawn\" action=\"bypass admin_event_manage edit_spawn_menu " + spawn.getSpawnId() + "\" width=83 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					tb.append("</tr>");
					tb.append("</table>");
				}
				
				tb.append("<img src=\"L2UI.SquareBlank\" width=1 height=4>");
			}
		}
		
		final int maxCountOfPagesUsingButtons = 20;
		int countOfPages = maxPages;
		
		boolean useButtons = true;
		
		if(countOfPages > maxCountOfPagesUsingButtons)
			useButtons = false;

		// show page numbers if size is too big
		if(size > maxSpawnsPerPage)
		{
			tb.append("<table width=270>");
			int i = 4;
			for(int x = 0; x < maxPages + 1; x++)
			{
				if(i == 4)
					tb.append("<tr>");
				
				if(useButtons)
					tb.append("<td><center><button value=\"P " + (x + 1) + "\" action=\"bypass admin_event_manage edit_event_map " + mapId + " " + x + "\" width=35 height=17 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></td>");
				else
					tb.append("<td><center><a action=\"bypass admin_event_manage edit_event_map " + mapId + " " + x + "\"><font color=ac9887>P" + (x + 1) + "</font></a></center></td>");
				
				if(i == 0)
				{
					tb.append("</tr>");
					i = 4;
				}
				else
					i--;
			}
			tb.append("</table>");
		}

		html = html.replaceAll("%name%", map.getMapName());
		
		html = html.replaceAll("%spawns%", tb.toString());
		
		tb = new TextBuilder();
		
		for(EventType t : map.getEvents())
		{
			tb.append(t.getAltTitle() + ";");
		}
		
		String events = tb.toString();
		events = events.substring(0, events.length() - 1);
		
		html = html.replaceAll("%events%", events);
		
		html = html.replaceAll("%save_close%", map.isSaved() ? "Close" : "* Save *");

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	private void setMapName(PlayerEventInfo gm, String n)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return;
		}
		
		if(n.length() > 16)
		{
			gm.sendMessage("The name can't be longer than 20 chars.");
			return;
		}
		
		map.setMapName(n);
	}
	
	private void setMapDesc(PlayerEventInfo gm, String n)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return;
		}
		
		if(n.length() > 1000)
		{
			gm.sendMessage("The description cannot be longer than 1000 chars.");
			return;
		}
		
		map.setMapDesc(n);
	}
	
	private void addAviableEvent(PlayerEventInfo gm, String event)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return;
		}
		
		EventType type = EventType.getType(event);
		
		if(type != null)
		{
			if(!map.getEvents().contains(type))
			{
				map.addEvent(type);
				
				map.removeEvent(EventType.Unassigned);
				
				gm.sendMessage("Done.");
			}
			else
				gm.sendMessage("This event has already been added.");
		}
		else
		{
			gm.sendMessage("This event doesn't exist.");
		}
	}
	
	private void removeAviableEvent(PlayerEventInfo gm, String event)
	{
		EventMap map = EventMapSystem.getInstance().getMapById(_editingInfos.get(gm.getPlayersId()).activeEditingMap);
		
		if(map == null)
		{
			gm.sendMessage("This map doesn't exist. (" + _editingInfos.get(gm.getPlayersId()).activeEditingMap + ").");
			return;
		}
		
		for(EventType t : map.getEvents())
		{
			if(t.toString().equalsIgnoreCase(event) || t.getAltTitle().equalsIgnoreCase(event))
			{
				if(_editingInfos.get(gm.getPlayersId()).activeEditingMapEvent == t)
					_editingInfos.get(gm.getPlayersId()).activeEditingMapEvent = null;
				
				map.removeEvent(t);
				gm.sendMessage("Done.");
				return;
			}
		}
		
		

		gm.sendMessage("This event doesn't exist.");
		gm.sendMessage("You can choose from: " + map.getEvents().toString());
	}
	
	private void setGoBackPage(PlayerEventInfo gm, String page)
	{
		_editingInfos.get(gm.getPlayersId()).currentGoBackAblePage = page;
	}
	
	private String getGoBackPage(PlayerEventInfo gm)
	{
		return _editingInfos.get(gm.getPlayersId()).currentGoBackAblePage;
	}

	private void eventGmMessage(PlayerEventInfo gm, String message, boolean event)
	{
		CallBack.getInstance().getOut().announceToAllScreenMessage(message, "Event");
	}
	
	private void eventGmMessageMenu(PlayerEventInfo gm)
	{
		String html;
		html = EventManager.getInstance().getHtmlManager().getAdminHtml("data/html/admin/events/eventmanage_messagemenu.htm");

		html = html.replaceAll("%title%", "Nexus Events");
		gm.sendPacket(html);
		gm.sendStaticPacket();
	}
	
	public static EventManagement getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EventManagement _instance = new EventManagement();
	}
}
