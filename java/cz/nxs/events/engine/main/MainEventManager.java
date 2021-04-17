package cz.nxs.events.engine.main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventBuffer;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventMapSystem;
import cz.nxs.events.engine.EventWarnings;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.events.AbstractMainEvent;
import cz.nxs.events.engine.main.pvpzone.PvpZoneManager;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.NpcTemplateData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 * - manages everything related to the running of MainEvents
 */
public class MainEventManager
{
	EventManager _manager;
	
	// active main events
	public enum State
	{
		IDLE, REGISTERING, RUNNING, TELE_BACK, END
	}
	
	private EventTaskScheduler _task;
	
	private AbstractMainEvent current;
	private EventMap activeMap;
	
	private List<PlayerEventInfo> _players;
	
	private State _state;
	private int _counter;
	
	private long lastEvent;
	
	private RegistrationCountdown _regCountdown;
	
	private ScheduledFuture<?> _regCountdownFuture;
	private ScheduledFuture<?> taskFuture;

	public Map<Integer, RegNpcLoc> regNpcLocs;
	
	private RegNpcLoc regNpc;
	private NpcData regNpcInstance;
	private int eventRunTime;
	
	private boolean autoScheduler = false;
	private double pausedTimeLeft;
	
	private EventScheduler scheduler;
	
	// new Event order system
	private List<EventScheduleData> _eventScheduleData = new FastList<EventScheduleData>();
	private EventType _lastEvent = null;
	
	private PvpZoneManager _pvpZoneManager;
	
	public PvpZoneManager getPvpZoneManager()
	{
		if(_pvpZoneManager == null)
			_pvpZoneManager = new PvpZoneManager();
		
		return _pvpZoneManager;
	}
	
	public MainEventManager()
	{
		_manager = EventManager.getInstance();
		
		_state = State.IDLE;
		
		_task = new EventTaskScheduler();
		_regCountdown = new RegistrationCountdown();
		
		_counter = 0;
		
		activeMap = null;
		eventRunTime = 0;
		
		_players = new FastList<PlayerEventInfo>();
		
		initRegNpcLocs();
		
		scheduler = new EventScheduler();
		//scheduler.schedule(-1, true);
		
		_pvpZoneManager = new PvpZoneManager();
	}
	
	private void initRegNpcLocs()
	{
		regNpcLocs = new FastMap<Integer, RegNpcLoc>();
		
		regNpcLocs.put(1, new RegNpcLoc("Your cords", null));
		regNpcLocs.put(2, new RegNpcLoc("Hunters Village", new int[] {116541,76077,-2730,0}));
		regNpcLocs.put(3, new RegNpcLoc("Goddard Town", new int[] {147726,-56323,-2781,0}));
		regNpcLocs.put(4, new RegNpcLoc("Ketra/Varka", new int[] {125176,-69204,-3260,0}));
		regNpcLocs.put(5, new RegNpcLoc("Cemetery", new int[] {182297,19407,-3174,0}));
		regNpcLocs.put(6, new RegNpcLoc("Aden Town", new int[] {148083,26983,-2205,0}));
	}
	
	public synchronized void startEvent(PlayerEventInfo gm, EventType type, int regTime, String mapName, String npcLoc, int runTime)
	{
		/**/ if(NexusLoader.detailedDebug) print((gm == null ? "GM" : "Scheduler") + " starting an event");
		
		AbstractMainEvent event = EventManager.getInstance().getMainEvent(type);
		if(event == null)
		{
			if(gm != null)
				gm.sendMessage("This event is not finished yet (most likely cause it is being reworked to be a mini event).");
			
			NexusLoader.debug("An unfinished event is chosen to be run. Skipping to the next one...", Level.WARNING);
			
			scheduler.run();
			return;
		}
		
		EventMap map = EventMapSystem.getInstance().getMap(type, mapName);
		if(map == null)
		{
			if(gm != null)
				gm.sendMessage("Map " + mapName + " doesn't exist or is not allowed for this event.");
			else
				NexusLoader.debug("Map " + mapName + " doesn't exist for event " + type.getAltTitle(), Level.WARNING);
			
			return;
		}
		
		RegNpcLoc npc = null;
		if(npcLoc != null)
		{
			for(Entry<Integer, RegNpcLoc> e : regNpcLocs.entrySet())
			{
				if(e.getValue().name.equalsIgnoreCase(npcLoc))
				{
					npc = e.getValue();
					break;
				}
			}
		}
		
		if(npc == null && gm != null)
		{
			gm.sendMessage("Reg NPC location " + npcLoc + " is not registered in the engine.");
			return;
		}
		else if(npc == null)
		{
			String configsCords = EventConfig.getInstance().getGlobalConfigValue("spawnRegNpcCords");
			
			int x = Integer.parseInt(configsCords.split(";")[0]);
			int y = Integer.parseInt(configsCords.split(";")[1]);
			int z = Integer.parseInt(configsCords.split(";")[2]);
			
			/*Loc loc = new Loc(116588, 76165, -2731);
			
			try 
			{
				if(CallBack.getInstance().getOut().getPvpZoneMainEventNpcLoc() != null)
					loc = CallBack.getInstance().getOut().getPvpZoneMainEventNpcLoc();
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}*/
			
			npc = new RegNpcLoc("From Configs", new int[]{x,y,z,0});
		}
		
		/**/ if(NexusLoader.detailedDebug) print("map " + map.getMapName() + ", event " + event.getEventName());
		
		if(regTime <= 0 || regTime >= 1439)
		{
			if(gm != null)gm.sendMessage("The minutes for registration must be within interval 1-1439 minutes.");
			else NexusLoader.debug("Can't start main event (automatic scheduler) - regTime is too high or too low (" + regTime + ").", Level.SEVERE);
			return;
		}
		
		int eventsRunTime = event.getInt("runTime");
		
		if(gm == null && eventsRunTime > 0) // launched automatically - use configs for runtime value
			runTime = eventsRunTime;
		
		if(runTime <= 0 || runTime >= 120) // max event runTime is 120 minutes
		{
			if(gm != null)gm.sendMessage("RunTime must be at least 1 minute and max. 120 minutes.");
			else NexusLoader.debug("Can't start main event (automatic scheduler) - runTime is too high or too low (" + runTime + ").", Level.SEVERE);
			return;
		}
		
		eventRunTime = runTime * 60;
		
		/**/ if(NexusLoader.detailedDebug) print("event runtime (in seconds) is " + eventsRunTime *60 + "s, regtime is " + (regTime * 60) + "s");
		
		regNpc = npc;
		
		_state = State.REGISTERING;
		current = event;
		current.startRegistration();
		
		/**/ if(NexusLoader.detailedDebug) print("event registration started, state is now REGISTERING");
		
		activeMap = map;

		_counter = (regTime) * 60;
		_regCountdownFuture = CallBack.getInstance().getOut().scheduleGeneral(_regCountdown, 1);
		
		/**/ if(NexusLoader.detailedDebug) print("scheduled registration countdown");
		
		spawnRegNpc(gm);

		/**/ if(NexusLoader.detailedDebug) print("regNpc finished spawn method");
		
		announce(LanguageEngine.getMsg("announce_eventStarted", type.getHtmlTitle()));
		
		String announce = EventConfig.getInstance().getGlobalConfigValue("announceRegNpcPos");
		if(announce.equals("-"))
			return;
		else
		{
			if(gm != null)
			{
				if(!npc.name.equals("Your cords") && !npc.name.equals("From Configs"))
				{
					announce(LanguageEngine.getMsg("announce_npcPos", npc.name));
					/**/ if(NexusLoader.detailedDebug) print("announcing registration cords (1 - gm != null)");
				}
				else
					/**/ if(NexusLoader.detailedDebug) print("not announcing registration cords (either Your Cords or From Configs chosen)");
			}
			else
			{
				announce(LanguageEngine.getMsg("announce_npcPos", announce));
				/**/ if(NexusLoader.detailedDebug) print("announcing registration cords (2 - gm == null)");
			}
		}
		
		if(EventConfig.getInstance().getGlobalConfigBoolean("announce_moreInfoInCb"))
		{
			announce(LanguageEngine.getMsg("announce_moreInfoInCb"));
		}
		
		NexusLoader.debug("Started registration for event " + current.getEventName());
		if(gm != null)
			gm.sendMessage("The event has been started.");
		
		/**/ if(NexusLoader.detailedDebug) print("finished startEvent() method");
	}
	
	private void spawnRegNpc(PlayerEventInfo gm)
	{
		if(gm == null && !EventConfig.getInstance().getGlobalConfigBoolean("allowSpawnRegNpc"))
		{
			print("configs permitted spawning regNpc");
			return;
		}
		
		if(regNpc != null)
		{
			final int id = EventConfig.getInstance().getGlobalConfigInt("mainEventManagerId");
			final NpcTemplateData template = new NpcTemplateData(id);
			
			print("spawning npc id " + id + ", template exists = " + template.exists());

			try
			{
				NpcData data;
				
				if(regNpc.cords == null)
				{
					data = template.doSpawn(gm.getX(), gm.getY(), gm.getZ(), 1, gm.getHeading(), 0);
				}
				else
				{
					data = template.doSpawn(regNpc.cords[0], regNpc.cords[1], regNpc.cords[2], 1, regNpc.cords[3], 0);
				}

				regNpcInstance = data;
				
				regNpcInstance.setTitle(current.getEventType().getHtmlTitle());
				regNpcInstance.broadcastNpcInfo();
				
				SkillData skill = new SkillData(5966, 1);
				if(skill.exists())
					regNpcInstance.broadcastSkillUse(regNpcInstance, regNpcInstance, skill.getId(), skill.getLevel());
				
				print("NPC spawned to cords " + data.getLoc().getX() + ", " + data.getLoc().getY() + ", " + data.getLoc().getZ() + "; objId = " + data.getObjectId());
			}
			catch (Exception e)
			{
				e.printStackTrace();
				print("error spawning NPC, " + NexusLoader.getTraceString(e.getStackTrace()));
			}
		}
	}
	
	public void unspawnRegNpc()
	{
		/**/ if(NexusLoader.detailedDebug) print("unspawnRegNpc()");
		
		if(regNpcInstance != null)
		{
			/**/ if(NexusLoader.detailedDebug) print("regNpcInstance is not null, unspawning it...");
			
			regNpcInstance.deleteMe();
			regNpcInstance = null;
		}
		else
			/**/ if(NexusLoader.detailedDebug) print("regNpcInstance is NULL!");
		
		regNpc = null;
	}
	
	public synchronized void skipDelay(PlayerEventInfo gm)
	{
		/**/ if(NexusLoader.detailedDebug) print("skipping event delay... ");
		
		if(_state == State.IDLE)
		{
			/**/ if(NexusLoader.detailedDebug) print("state is idle, can't skip delay");
			
			gm.sendMessage("There's no active event atm.");
			return;
		}
		else if(_state == State.REGISTERING)
		{
			/**/ if(NexusLoader.detailedDebug) print("state is registering, skipping delay...");
			
			if(_regCountdownFuture != null)
				_regCountdownFuture.cancel(false);
			
			if(taskFuture != null)
				taskFuture.cancel(false);
			
			_counter = 0;
			_regCountdownFuture = CallBack.getInstance().getOut().scheduleGeneral(_regCountdown, 1);
			
			/**/ if(NexusLoader.detailedDebug) print("delay successfully skipped");
		}
		else
		{
			gm.sendMessage("The event can skip waiting delay only when it's in the registration state.");
			
			/**/ if(NexusLoader.detailedDebug) print("can't skip delay, state is " + _state.toString());
		}
	}
	
	public void watchEvent(PlayerEventInfo gm, int instanceId)
	{
		AbstractMainEvent event = current;
		if(event == null)
		{
			gm.sendMessage("No event is available now.");
			return;
		}
		
		try
		{
			event.addSpectator(gm, instanceId);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			gm.sendMessage("Event cannot be spectated now. Please try it again later.");
		}
	}
	
	public void stopWatching(PlayerEventInfo gm)
	{
		AbstractMainEvent event = current;
		if(event == null)
		{
			gm.sendMessage("No event is available now.");
			return;
		}
		
		event.removeSpectator(gm);
	}
	
	public synchronized void abort(PlayerEventInfo gm, boolean error)
	{
		/**/ if(NexusLoader.detailedDebug) print("MainEventManager.abort(), error = " + error);
		
		if(error)
		{
			/**/ if(NexusLoader.detailedDebug) print("aborting due to error...");
			
			unspawnRegNpc();
			
			try
			{
				current.clearEvent();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				clean(null);
				
				/**/ if(NexusLoader.detailedDebug) print("error while aborting - " + NexusLoader.getTraceString(e.getStackTrace()));
			}
		}
		else
		{
			/**/ if(NexusLoader.detailedDebug) print("aborting due to GM... - _state = " + _state.toString());
			
			if(_state == State.REGISTERING)
			{
				/**/ if(NexusLoader.detailedDebug) print("aborting while in registering state");
				
				NexusLoader.debug("Event aborted by GM");
				
				unspawnRegNpc();
				
				current.clearEvent();
				announce(LanguageEngine.getMsg("announce_regAborted"));
				
				_regCountdown.abort();
				
				/**/ if(NexusLoader.detailedDebug) print("event (in registration) successfully aborted");
			}
			else if(_state == State.RUNNING)
			{
				/**/ if(NexusLoader.detailedDebug) print("aborting while in running state");
				
				unspawnRegNpc();
				
				if(current != null)
					current.clearEvent();
				else
					clean("in RUNNING state after current was null!!!");
				
				announce(LanguageEngine.getMsg("announce_eventAborted"));
				
				/**/ if(NexusLoader.detailedDebug) print("event (in runtime) successfully aborted");
			}
			else 
			{
				/**/ if(NexusLoader.detailedDebug) print("can't abort event now!");
				
				gm.sendMessage("Event cannot be aborted now.");
				return;
			}
		}
		
		/**/ if(NexusLoader.detailedDebug) print("MainEventManager.abort() finished");
		
		if(!autoSchedulerPaused() && autoSchedulerEnabled())
		{
			scheduler.schedule(-1, false);
			
			/**/ if(NexusLoader.detailedDebug) print("scheduler enabled, scheduling next event...");
		}
	}
	
	public void endDueToError(String text)
	{
		/**/ if(NexusLoader.detailedDebug) print("starting MainEventManager.endDueToError(): " + text);
		
		announce(text);
		
		abort(null, true);
		
		/**/ if(NexusLoader.detailedDebug) print("finished MainEventManager.endDueToError()");
	}
	
	public void end()
	{
		/**/ if(NexusLoader.detailedDebug) print("started MainEventManager.end()");
		
		_state = State.TELE_BACK;
		schedule(1);
		
		/**/ if(NexusLoader.detailedDebug) print("finished MainEventManager.end()");
	}
	
	private void schedule(int time)
	{
		/**/ if(NexusLoader.detailedDebug) print("MainEventManager.schedule(): " + time);
		
		taskFuture = CallBack.getInstance().getOut().scheduleGeneral(_task, time);
	}
	
	public void announce(String text)
	{
		String announcer = "Event Engine";
		
		if(current != null)
			announcer = current.getEventType().getAltTitle();
		
		/**/ if(NexusLoader.detailedDebug) print("MainEventManager.announce(): '" + text + "' announcer = " + announcer);
		
		CallBack.getInstance().getOut().announceToAllScreenMessage(text, announcer);
	}
	
	public List<PlayerEventInfo> getPlayers()
	{
		return _players;
	}
	
	public int getCounter()
	{
		return _counter;
	}
	
	public String getTimeLeft(boolean digitalClockFormat)
	{
		try
		{
			if(_state == State.REGISTERING)
			{
				if(digitalClockFormat)
					return _regCountdown.getTimeAdmin();
				else 
					return _regCountdown.getTime();
			}
			else if(_state == State.RUNNING)
			{
				return current.getEstimatedTimeLeft();
			}
			else
				return "N/A";
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return "<font color=AE0000>Event error</font>";
		}
	}
	
	public String getMapName()
	{
		if(activeMap == null)
			return "N/A";
		else
			return activeMap.getMapName();
	}
	
	public String getMapDesc()
	{
		if(activeMap == null)
			return "N/A";
		else
			if(activeMap.getMapDesc() == null || activeMap.getMapDesc().length() == 0)
			{
				return "This map has no description.";
			}
			else
				return activeMap.getMapDesc();
	}
	
	public EventMap getMap()
	{
		return activeMap;
	}
	
	public int getRunTime()
	{
		return eventRunTime == 0 ? 120 : eventRunTime;
	}
	
	public State getState()
	{
		return _state;
	}
	
	private void msgToAll(String text)
	{
		/**/ if(NexusLoader.detailedDebug) print("MainEventManager.msgToAll(): " + text);
		
		for (PlayerEventInfo player : _players)
			player.sendMessage(text);
	}
	
	public void paralizeAll(boolean para)
	{
		try
		{
			/**/ if(NexusLoader.detailedDebug) print("paralyze all called, para = " + para);
			
			for(PlayerEventInfo player : _players)
			{
				if(player.isOnline())
				{
					player.setIsParalyzed(para);
					player.setIsInvul(para);
					
					player.paralizeEffect(para);
				}
			}
		}
		catch (NullPointerException e)
		{
			/**/ if(NexusLoader.detailedDebug) print("error while paralyzing, " + NexusLoader.getTraceString(e.getStackTrace()));
		}
	}
	
	public boolean canRegister(PlayerEventInfo player, boolean start)
	{
		if (player.getLevel() > current.getInt("maxLvl"))
		{
			player.sendMessage(LanguageEngine.getMsg("registering_highLevel"));
			return false;
		}
		
		if (player.getLevel() < current.getInt("minLvl"))
		{
			player.sendMessage(LanguageEngine.getMsg("registering_lowLevel"));
			return false;
		}
		
		if (!player.isGM() && start && current.getBoolean("dualboxCheck") && dualboxDetected(player, current.getInt("maxPlayersPerIp")))
		{
			player.sendMessage(LanguageEngine.getMsg("registering_sameIp"));
			return false;
		}
		
		if(!EventManager.getInstance().canRegister(player))
		{
			player.sendMessage(LanguageEngine.getMsg("registering_status"));
			return false;
		}
		
		return true;
	}
	
	public boolean registerPlayer(PlayerEventInfo player)
	{
		/**/ if(NexusLoader.detailedDebug) print(". starting registerPlayer() for " + player.getPlayersName());
		
		if (_state != State.REGISTERING)
		{
			player.sendMessage(LanguageEngine.getMsg("registering_notRegState"));
			return false;
		}
		
		if (player.isRegistered())
		{
			player.sendMessage(LanguageEngine.getMsg("registering_alreadyRegistered"));
			return false;
		}
		
		int i = EventWarnings.getInstance().getPoints(player);
		if(i >= EventWarnings.MAX_WARNINGS && !player.isGM())
		{
			player.sendMessage(LanguageEngine.getMsg("registering_warningPoints", EventWarnings.MAX_WARNINGS, i));
			
			/**/ if(NexusLoader.detailedDebug) print("... registerPlayer() for " + player.getPlayersName() + ", player has too many warnings! (" + i + ")");
			return false;
		}
		
		if (canRegister(player, true))
		{
			if(!getCurrent().canRegister(player))
			{
				/**/ if(NexusLoader.detailedDebug) print("... registerPlayer() for " + player.getPlayersName() + ", player failed to register on event, event itself didn't allow so!");
				
				player.sendMessage(LanguageEngine.getMsg("registering_notAllowed"));
				return false;
			}
			
			if(EventConfig.getInstance().getGlobalConfigBoolean("eventSchemeBuffer"))
			{
				if (!EventBuffer.getInstance().hasBuffs(player))
				{
					player.sendMessage(LanguageEngine.getMsg("registering_buffs"));
				}
				
				EventManager.getInstance().getHtmlManager().showSelectSchemeForEventWindow(player, "main", getCurrent().getEventType().getAltTitle());
			}
			
			player.sendMessage(LanguageEngine.getMsg("registering_registered"));
			
			PlayerEventInfo pi = CallBack.getInstance().getPlayerBase().addInfo(player);
			pi.setIsRegisteredToMainEvent(true, current.getEventType());
			
			synchronized(_players)
			{
				_players.add(pi);
			}
			
			/**/ if(NexusLoader.detailedDebug) print("... registerPlayer() for " + player.getPlayersName() + ", player has been registered!");
			
			return true;
		}
		else
		{
			/**/ if(NexusLoader.detailedDebug) print("... registerPlayer() for " + player.getPlayersName() + ", player failed to register on event, manager didn't allow so!");
			
			player.sendMessage(LanguageEngine.getMsg("registering_fail"));
			return false;
		}
	}
	
	public boolean unregisterPlayer(PlayerEventInfo player, boolean force)
	{
		if(player == null)
			return false;
		
		/**/ if(NexusLoader.detailedDebug) print(". starting unregisterPlayer() for " + player.getPlayersName() + ", force = " + force);
		
		if(!EventConfig.getInstance().getGlobalConfigBoolean("enableUnregistrations"))
		{
			/**/ if(NexusLoader.detailedDebug) print("... unregisterPlayer()  - unregistrations are not allowed here!");
			
			if(!force) player.sendMessage(LanguageEngine.getMsg("unregistering_cantUnregister"));
			return false;
		}
		
		if (!_players.contains(player))
		{
			/**/ if(NexusLoader.detailedDebug) print("... unregisterPlayer() for " + player.getPlayersName() + " player is not registered");
			
			if(!force) player.sendMessage(LanguageEngine.getMsg("unregistering_notRegistered"));
			return false;
		}
		
		if (_state != State.REGISTERING && !force)
		{
			/**/ if(NexusLoader.detailedDebug) print("... unregisterPlayer() for " + player.getPlayersName() + " player can't unregister now, becuase _state = " + _state.toString());
			
			player.sendMessage(LanguageEngine.getMsg("unregistering_cant"));
			return false;
		}
		else
		{
			player.sendMessage(LanguageEngine.getMsg("unregistering_unregistered"));
			
			player.setIsRegisteredToMainEvent(false, null);
			CallBack.getInstance().getPlayerBase().eventEnd(player);
			
			synchronized(_players)
			{
				_players.remove(player);
			}
			
			/**/ if(NexusLoader.detailedDebug) print("... unregisterPlayer() for " + player.getPlayersName() + " player has been unregistered");
			
			if(current != null)
				current.playerUnregistered(player);
			
			return true;
		}
	}
	
	public class RegNpcLoc
	{
		public String name;
		public int[] cords;

		public RegNpcLoc(String name, int[] cords)
		{
			this.name = name;
			this.cords = cords;
		}
	}
	
	public boolean dualboxDetected(PlayerEventInfo player)
	{
		if(!player.isOnline(true))
			return false;
		
		String ip1 = player.getIp();
		
		if(ip1 == null)
			return false;
		
        String ip2;
        
        for(PlayerEventInfo p : _players)
        {
        	ip2 = p.getIp();
        	
        	if(ip1.equals(ip2))
        	{
				/**/ if(NexusLoader.detailedDebug) print("... MainEventManager.dualboxDetected() for " + player.getPlayersName() + ", found dualbox for IP " + player.getIp());
        		return true;
        	}
        }
        
        return false;
	}
	
	public boolean dualboxDetected(PlayerEventInfo player, int maxPerIp)
	{
		if(!player.isOnline(true))
			return false;
		
		int occurences = 0;
		String ip1 = player.getIp();
		
		if(ip1 == null)
			return false;
		
		for(PlayerEventInfo p : _players)
		{
			if(ip1.equals(p.getIp()))
			{
				occurences ++;
			}
		}
		
		if(occurences >= maxPerIp)
		{
			/**/ if(NexusLoader.detailedDebug) print("... MainEventManager.dualboxDetected() for " + player.getPlayersName() + ", found dualbox for IP (method 2) " + player.getIp() + " maxPerIp " + maxPerIp + " occurences = " + occurences);
			return true;
		}
		
		return false;
	}
	
	public AbstractMainEvent getCurrent()
	{
		return current;
	}
	
	public int getPlayersCount()
	{
		return _players.size();
	}
	
	public class EventScheduler implements Runnable
	{
		private ScheduledFuture<?> _future;
		boolean run = false;
		@Override
		public void run()
		{
			try
			{
				if (!run)
					return;
				boolean selected = false;
				
				/**/ if(NexusLoader.detailedDebug) print("starting EventScheduler.run()");
				
				for(int i = 0; i < EventType.values().length; i++)
				{
					/**/ if(NexusLoader.detailedDebug) print("trying to find an event to be started...");
					
					NexusLoader.debug("Trying to find an event to be started:", Level.INFO);
					EventType next = EventType.getNextRegularEvent();
					
					if(next == null)
					{
						/**/ if(NexusLoader.detailedDebug) print("no next event is available. stopping it here, pausing scheduler");
						
						NexusLoader.debug("No next event is aviaible!", Level.INFO);
						if(!autoSchedulerPaused())
							schedule(-1, false);
						
						break;
					}
					
					EventMap nextMap = null;
					
					/**/ if(NexusLoader.detailedDebug) print("next selected event is " + next.getAltTitle());
					
					AbstractMainEvent event = EventManager.getInstance().getMainEvent(next);
					
					List<EventMap> maps = new FastList<EventMap>();
					maps.addAll(EventMapSystem.getInstance().getMaps(next).values());
					Collections.shuffle(maps);
					
					/**/ if(NexusLoader.detailedDebug) print("no available map for event " + next.getAltTitle());
					
					for(EventMap map : maps)
					{
						if(event.canRun(map))
						{
							nextMap = map;
							break;
						}
					}
					
					if(nextMap == null) // start another event
					{
						/**/ if(NexusLoader.detailedDebug) print("no available map for event " + next.getAltTitle());
						continue;
					}
					
					selected = true;
					
					/**/ if(NexusLoader.detailedDebug) print("selected and starting next event via automatic scheduler");
					
					int runTime = EventConfig.getInstance().getGlobalConfigInt("defaultRunTime");
					
					if(next == EventType.Zombies || next == EventType.TreasureHunt || next == EventType.TreasureHuntPvp)
						runTime = 10;
					
					//startEvent(null, next, EventConfig.getInstance().getGlobalConfigInt("defaultRegTime"), nextMap.getMapName(), null, runTime);
					break;
				}
				
				if(!selected)
				{
					NexusLoader.debug("No event could be started. Check if you have any maps for them and if they are configured properly.");
					
					/**/ if(NexusLoader.detailedDebug) print("no event could be started...");					
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		public boolean abort()
		{
			/**/ if(NexusLoader.detailedDebug) print("aborting event scheduler");
			
			if(_future != null)
			{
				_future.cancel(false);
				_future = null;
				return true;
			}
			return false;
		}
		
		public void schedule(double delay, boolean firstStart)
		{
			if(!EventConfig.getInstance().getGlobalConfigBoolean("enableAutomaticScheduler"))
				return;
			
			
			if(_future != null)
			{
				_future.cancel(false);
				_future = null;
			}
			
			autoScheduler = false;
			
			if(current == null)
			{
				if(firstStart)
				{
					delay = EventConfig.getInstance().getGlobalConfigInt("firstEventDelay") * 60000;
					_future = CallBack.getInstance().getOut().scheduleGeneral(this, (long)delay);
				}
				else
				{
					if(delay > -1)
					{
						_future = CallBack.getInstance().getOut().scheduleGeneral(this, (long)delay * 1000);
					}
					else
					{
						delay = EventConfig.getInstance().getGlobalConfigInt("delayBetweenEvents") * 60000;
						_future = CallBack.getInstance().getOut().scheduleGeneral(this, (long)delay);
					}
				}
				
				/**/ if(NexusLoader.detailedDebug) print("scheduling next event in " + Math.round(delay / 60000) + " minutes.");
				
				NexusLoader.debug("Next event in " + Math.round(delay / 60000) + " minutes.", Level.INFO);
			}
			else
			{
				NexusLoader.debug("Automatic scheduler reeanbled.");
				
				/**/ if(NexusLoader.detailedDebug) print("reenabling automatic scheduler");				
			}
		}
	}
	
	
	public void abortAutoScheduler(PlayerEventInfo gm)
	{
		if(autoSchedulerPaused())
			unpauseAutoScheduler(gm, false);
		
		if(scheduler.abort())
		{
			if(gm != null)
				gm.sendMessage("Automatic event scheduling has been disabled");
			
			NexusLoader.debug("Automatic scheduler disabled" + (gm != null ? " by a GM." : "."), Level.INFO);
		}
		
		/**/ if(NexusLoader.detailedDebug) print("aborting auto scheduler, gm is null? " + (gm == null));
		
		autoScheduler = false;
	}
	
	public void pauseAutoScheduler(PlayerEventInfo gm)
	{
		if(!EventConfig.getInstance().getGlobalConfigBoolean("enableAutomaticScheduler"))
		{
			gm.sendMessage("The automatic event scheduler has been disabled in configs.");
			return;
		}
		
		if(scheduler == null)
			return;
		
		if(getCurrent() != null)
		{
			gm.sendMessage("There's no pausable delay. Wait till the event ends.");
			return;
		}
		
		if(!autoSchedulerPaused() && autoSchedulerEnabled())
		{
			if(scheduler._future == null)
			{
				gm.sendMessage("Cannot pause the scheduler now.");
				return;
			}
			
			if(scheduler._future.getDelay(TimeUnit.SECONDS) < 2)
			{
				gm.sendMessage("Cannot pause now. Event starts in less than 2 seconds.");
				return;
			}
			
			pausedTimeLeft = scheduler._future.getDelay(TimeUnit.SECONDS);
			scheduler.abort();
			
			NexusLoader.debug("Automatic scheduler paused" + (gm != null ? " by a GM." : "."), Level.INFO);
		}
		else
			gm.sendMessage("The scheduler must be enabled.");
	}
	
	public void unpauseAutoScheduler(PlayerEventInfo gm, boolean run)
	{
		if(!EventConfig.getInstance().getGlobalConfigBoolean("enableAutomaticScheduler"))
		{
			gm.sendMessage("The automatic event scheduler has been disabled in configs.");
			return;
		}
		
		if(scheduler == null)
			return;
		
		if(getCurrent() != null)
		{
			gm.sendMessage("An event is already running.");
			return;
		}
		
		if(autoSchedulerPaused())
		{
			if(run)
			{
				scheduler.schedule(pausedTimeLeft, false);
				NexusLoader.debug("Automatic scheduler continues (event in " + pausedTimeLeft + " seconds) again after being paused" + (gm != null ? " by a GM." : "."), Level.INFO);
			}
			else
				NexusLoader.debug("Automatic scheduler unpaused " + (gm != null ? " by a GM." : "."), Level.INFO);
			
			pausedTimeLeft = 0;
		}
		else if(gm != null)
			gm.sendMessage("The scheduler is not paused.");
	}
	
	public void restartAutoScheduler(PlayerEventInfo gm)
	{
		if(!EventConfig.getInstance().getGlobalConfigBoolean("enableAutomaticScheduler"))
		{
			gm.sendMessage("The automatic event scheduler has been disabled in configs.");
			return;
		}
		
		if(autoSchedulerPaused())
		{
			unpauseAutoScheduler(gm, true);
		}
		else
		{
			NexusLoader.debug("Automatic scheduler enabled" + (gm != null ? " by a GM." : "."), Level.INFO);
			scheduler.schedule(-1, false);
		}
		
		if(gm != null && current == null)
			gm.sendMessage("Automatic event scheduling has been enabled. Next event in " + EventConfig.getInstance().getGlobalConfigInt("delayBetweenEvents") + " minutes.");
	}
	
	public EventScheduler getScheduler()
	{
		return scheduler;
	}
	
	public boolean autoSchedulerEnabled()
	{
		return autoScheduler;
	}
	
	public boolean autoSchedulerPaused()
	{
		return pausedTimeLeft > 0;
	}
	
	public String getAutoSchedulerDelay()
	{
		double d = 0;
		if(scheduler._future != null && !scheduler._future.isDone())
			d = scheduler._future.getDelay(TimeUnit.SECONDS);

		if(autoSchedulerPaused())
			d = pausedTimeLeft;
		
		if(d == 0)
			return "N/A";
		else
		{
			if(d >= 60)
				return (int)d / 60 + " min";
			else
				return (int)d + " sec";
		}
	}
	
	public String getLastEventTime()
	{
		if(lastEvent == 0)
			return "N/A";
		
		long time = System.currentTimeMillis();
		
		long diff = time - lastEvent;
		if(diff > 1000)
			diff /= 1000;
		else return "< 1 sec ago";
		
		if(diff > 60)
		{
			diff /= 60;
			if(diff > 60)
				diff /= 60;
			else
				return (diff + " min ago");
		}
		else 
			return (diff + " sec ago");
		
		return (diff + " hours ago");
	}
	
	public List<EventScheduleData> getEventScheduleData()
	{
		return _eventScheduleData;
	}
	
	public EventType nextAvailableEvent(boolean testOnly)
	{
		EventType event = null;
		int lastOrder = 0;
		
		if(_lastEvent != null)
		{
			for(EventScheduleData d : _eventScheduleData)
			{
				if(d.getEvent() == _lastEvent)
				{
					lastOrder = d.getOrder();
				}
			}
		}
		
		int limit = _eventScheduleData.size()*2;
		
		if(_eventScheduleData.isEmpty())
			return null;
		
		while(event == null)
		{
			for(EventScheduleData d : _eventScheduleData)
			{
				if(d.getOrder() == lastOrder+1)
				{
					// check if event can run (has maps, isn't disabled, etc.
					if(d.getEvent().isRegularEvent() && EventConfig.getInstance().isEventAllowed(d.getEvent()) && EventManager.getInstance().getMainEvent(d.getEvent()) != null && EventMapSystem.getInstance().getMapsCount(d.getEvent()) > 0)
					{
						// chance check
						if(testOnly || CallBack.getInstance().getOut().random(100) < d.getChance())
						{
							event = d.getEvent();
							
							if(!testOnly)
								_lastEvent = event;
							break;
						}
					}
				}
			}
			
			if(--limit <= 0)
				break;
			else
			{
				if(lastOrder > _eventScheduleData.size())
					lastOrder = 0; // go again from start
				else
					lastOrder ++;
			}
		}
		
		return event;
	}
	
	public EventScheduleData getScheduleData(EventType type)
	{
		for(EventScheduleData d : _eventScheduleData)
		{
			if(d.getEvent().equals(type))
				return d;
		}
		return null;
	}
	
	public EventType getLastEventOrder()
	{
		return _lastEvent;
	}
	
	public EventType getGuessedNextEvent()
	{
		return nextAvailableEvent(true);
	}
	
	private void addScheduleData(EventType type, int order, int chance, boolean updateInDb)
	{
		if(type == null)
			return;
		
		boolean selectOrder = false;
		
		if(order == -1 || order > _eventScheduleData.size())
			selectOrder = true;
		else
		{
			for(EventScheduleData d : _eventScheduleData)
			{
				if(d.getOrder() == order)
				{
					selectOrder = true;
					break;
				}
			}
		}
		
		if(selectOrder)
		{
			int freeOrder = -1;
			boolean found;
			
			for(int i = 0; i < _eventScheduleData.size(); i++)
			{
				found = false;
				for(EventScheduleData d : _eventScheduleData)
				{
					if(d.getOrder() == i+1)
					{
						found = true;
						break;
					}
				}
				
				if(!found)
				{
					freeOrder = i+1;
					break;
				}
			}
			
			if(freeOrder == -1)
			{
				int highest = 0;
				for(EventScheduleData d : _eventScheduleData)
				{
					if(d.getOrder() > highest)
						highest = d.getOrder();
				}
				order = highest + 1;
			}
			else
			{
				order = freeOrder;
			}
		}
		
		boolean add = true;
		for(EventScheduleData d : _eventScheduleData)
		{
			if(d.getEvent() == type)
			{
				add = false;
				break;
			}
		}
		
		if(add)
		{
			EventScheduleData data = new EventScheduleData(type, order, chance);
			_eventScheduleData.add(data);
		}
		
		if(selectOrder)
		{
			saveScheduleData(type);
			
			if(updateInDb)
			{
				if(order != -1)
					NexusLoader.debug("Adding wrong-configured/missing " + type.getAltTitle() + " event to EventOrder system with order " + order);
				else
					NexusLoader.debug("Error adding " + type.getAltTitle() + " event to EventOrder system");
			}
		}
	}
	
	public void loadScheduleData()
	{
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			PreparedStatement statement = con.prepareStatement("SELECT * FROM nexus_eventorder ORDER BY eventOrder ASC");
			ResultSet rset = statement.executeQuery();
			
			while(rset.next())
			{
				String event = rset.getString("event");
				int order = rset.getInt("eventOrder");
				int chance = rset.getInt("chance");
				
				for(EventScheduleData d : _eventScheduleData)
				{
					if(d.getOrder() == order)
					{
						NexusLoader.debug("Duplicate order in EventOrder system for event " + event, Level.WARNING);
						order = -1;
					}
				}
				
				addScheduleData(EventType.getType(event), order, chance, false);
			}
			
			rset.close();
			statement.close();
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try {con.close();}
			catch (Exception e){}
		}
		
		for(EventType type : EventType.values())
		{
			if(type.isRegularEvent() && type != EventType.Unassigned && EventManager.getInstance().getEvent(type) != null)
			{
				if(getScheduleData(type) == null)
				{
					addScheduleData(type, -1, 100, true);
				}
			}
		}
	}
	
	public int saveScheduleData(EventType event)
	{
		Connection con = null;
		EventScheduleData data = getScheduleData(event);
		
		if(data == null)
			return -1;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			
			PreparedStatement statement = con.prepareStatement("REPLACE INTO nexus_eventorder VALUES (?,?,?)");
			statement.setString(1, data.getEvent().getAltTitle());
			statement.setInt(2, data.getOrder());
			statement.setInt(3, data.getChance());
			
			statement.execute();
			statement.close();
		}
		
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try {con.close();}
			catch (Exception e){}
		}
		
		return data._order;
	}
	
	public class EventScheduleData
	{
		private final EventType _event;
		private int _order;
		private int _chance;
		
		private EventScheduleData(EventType event, int order, int chance)
		{
			_event = event;
			_order = order;
			_chance = chance;
		}
		
		public EventType getEvent()
		{
			return _event;
		}
		
		public int getOrder()
		{
			return _order;
		}
		
		public void setOrder(int c)
		{
			_order = c;
		}
		
		public int getChance()
		{
			return _chance;
		}
		
		public void setChance(int c)
		{
			_chance = c;
		}
		
		public boolean decreaseOrder()
		{
			boolean done = false;
			for(EventScheduleData d : _eventScheduleData)
			{
				if(d.getEvent() != getEvent() && d.getOrder() == _order+1)
				{
					d.setOrder(_order);
					_order ++;
					
					saveScheduleData(d.getEvent());
					saveScheduleData(getEvent());
					
					done = true;
					break;
				}
			}
			
			return done;
		}
		
		public boolean raiseOrder()
		{
			boolean done = false;
			for(EventScheduleData d : _eventScheduleData)
			{
				if(d.getEvent() != getEvent() && d.getOrder() == _order-1)
				{
					d.setOrder(_order);
					_order --;
					
					saveScheduleData(d.getEvent());
					saveScheduleData(getEvent());
					
					done = true;
					break;
				}
			}
			
			return done;
		}
	}
	
	private class RegistrationCountdown implements Runnable
	{
		private String getTimeAdmin()
		{
			String mins = "" + _counter / 60;
			String secs = (_counter % 60 < 10 ? "0" + _counter % 60 : "" + _counter % 60);
			return "" + mins + ":" + secs + "";
		}
		
		private String getTime()
		{
			if(_counter > 60)
			{
				int min = _counter/60;
				if(min < 1) min = 1;
				return min + " minutes";
			}
			else
			{
				return _counter + " seconds";
			}
		}

		@Override
		@SuppressWarnings("synthetic-access")
		public void run()
		{
			if (_state == State.REGISTERING)
			{
				switch (_counter)
				{
					case 1800:
					case 1200:
					case 600:
					case 300:
					case 60:
						announce(LanguageEngine.getMsg("announce_timeleft_min", _counter/60));
						break;
					case 30:
					case 10:
					case 5:
						announce(LanguageEngine.getMsg("announce_timeleft_sec", _counter));
						break;
				}
			}

			if (_counter == 0)
			{
				/**/ if(NexusLoader.detailedDebug) print("registration coutndown counter 0, scheduling next action");
				
				schedule(1);
			}
			else
			{
				_counter--;
				_regCountdownFuture = CallBack.getInstance().getOut().scheduleGeneral(_regCountdown, 1000);
			}
		}
		
		private void abort()
		{
			/**/ if(NexusLoader.detailedDebug) print("aborting regcoutndown... ");
			
			if(_regCountdownFuture != null)
			{
				/**/ if(NexusLoader.detailedDebug) print("... regCount is not null");
				
				_regCountdownFuture.cancel(false);
				_regCountdownFuture = null;
			}
			else /**/ if(NexusLoader.detailedDebug) print("... regCount is NULL!");
			
			_counter = 0;
		}
	}
	
	private void abortCast()
	{
		/**/ if(NexusLoader.detailedDebug) print("aborting cast of all players on the event");
		
		for(PlayerEventInfo p : _players)
		{
			p.abortCasting();
		}
	}
	
	private class EventTaskScheduler implements Runnable
	{
		@Override
		@SuppressWarnings({ "synthetic-access", "incomplete-switch" })
		public void run()
		{
			switch (_state)
			{
				case REGISTERING:
				{
					/**/ if(NexusLoader.detailedDebug) print("eventtask - ending registration");
					
					announce(LanguageEngine.getMsg("announce_regClosed"));
					NexusLoader.debug("Registration phase ended.");
					
					for(PlayerEventInfo p : _players)
					{
						if(!canRegister(p, false))
							unregisterPlayer(p, true);
					}
					
					/**/ if(NexusLoader.detailedDebug) print("eventtask - players that can't participate were unregistered");

					if (!current.canStart())
					{
						/**/ if(NexusLoader.detailedDebug) print("eventtask - can't start - not enought players - " + _players.size());
						
						NexusLoader.debug("Not enought participants.");
						
						unspawnRegNpc();
						
						current.clearEvent();
						announce(LanguageEngine.getMsg("announce_lackOfParticipants"));
						
						if(!autoSchedulerPaused() && autoSchedulerEnabled())
							scheduler.schedule(-1, false);
					}
					else
					{
						/**/ if(NexusLoader.detailedDebug) print("eventtask - event started");
						
						NexusLoader.debug("Event starts.");
						announce(LanguageEngine.getMsg("announce_started"));
						current.initEvent();
						
						_state = State.RUNNING;
					
						msgToAll(LanguageEngine.getMsg("announce_teleport10sec"));
						msgToAll("Type .respawn if you get stuck to respawn.");
						
						int delay = EventConfig.getInstance().getGlobalConfigInt("teleToEventDelay");
						if(delay <= 0 || delay > 60000)
							delay = 10000;
						
						/**/ if(NexusLoader.detailedDebug) print("eventtask - event started, teletoevent delay " + delay);
						
						if(EventConfig.getInstance().getGlobalConfigBoolean("antistuckProtection"))
						{
							/**/ if(NexusLoader.detailedDebug) print("eventtask - anti stuck protection ON");
							
							abortCast();
							
							/**/ if(NexusLoader.detailedDebug) print("eventtask - aborted cast...");
							
							final int fDelay = delay;
							CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
							{
								@Override
								public void run()
								{
									paralizeAll(true);
									
									schedule(fDelay-1000);
								}
							}, 1000);
						}
						else
						{
							/**/ if(NexusLoader.detailedDebug) print("eventtask - anti stuck protection OFF");
							
							paralizeAll(true);
							
							schedule(delay);
							
							/**/ if(NexusLoader.detailedDebug) print("eventtask - scheduled for next state in " + delay);
						}
					}
					break;
				}
				case RUNNING:
				{
					/**/ if(NexusLoader.detailedDebug) print("eventtask - event started, players teleported");
					
					paralizeAll(false);
					
					/**/ if(NexusLoader.detailedDebug) print("eventtask - event started, players unparalyzed");
					
					current.runEvent();
					
					/**/ if(NexusLoader.detailedDebug) print("eventtask - event started, event runned");
					
					current.initMap();
					
					/**/ if(NexusLoader.detailedDebug) print("eventtask - event started, map initialized");

					/**/ if(NexusLoader.detailedDebug) print("eventtask - event started, stats given");

					break;
				}
				case TELE_BACK:
				{
					/**/ if(NexusLoader.detailedDebug) print("eventtask - event ending, teleporting back in 10 sec");
					
					// rewards
					current.onEventEnd();
					
					/**/ if(NexusLoader.detailedDebug) print("eventtask - on event end");
					
					msgToAll(LanguageEngine.getMsg("announce_teleportBack10sec"));
					
					_state = State.END;
					NexusLoader.debug("Teleporting back.");
					schedule(10000);
					break;
				}
				case END:
				{	
					/*if(EventConfig.getInstance().getGlobalConfigBoolean("statTrackingEnabled"))
					{
						Stats.getInstance().applyChanges();
						Stats.getInstance().tempTable.clear();
						Stats.getInstance().updateSQL(current.getPlayers(0), current.getEventType().getMainEventId());
					}*/
					
					/**/ if(NexusLoader.detailedDebug) print("eventtask - event ended, teleporting back NOW!");
					
					unspawnRegNpc();
					
					current.clearEvent(); 
					
					/**/ if(NexusLoader.detailedDebug) print("eventtask - event ended, event cleared!");
					
					announce(LanguageEngine.getMsg("announce_end"));
					
					CallBack.getInstance().getOut().purge();
					
					/**/ if(NexusLoader.detailedDebug) print("eventtask - event ended, after purge");
					
					if(!autoSchedulerPaused() && autoSchedulerEnabled())
						scheduler.schedule(-1, false);
					
					NexusLoader.debug("Event ended.");
					break;
				}
			}
		}
	}
	
	/**
	 * @param message - null if no announce
	 */
	public void clean(String message)
	{
		/**/ if(NexusLoader.detailedDebug) print("MainEventManager() clean: " + message);
		
		current = null;
		activeMap = null;
		eventRunTime = 0;
		
		_players.clear();
		
		if(message != null)
			announce(message);
		
		_state = State.IDLE;
		
		if(regNpcInstance != null)
		{
			regNpcInstance.deleteMe();
			regNpcInstance = null;
		}
		
		regNpc = null;
		
		lastEvent = System.currentTimeMillis();
		
		EventManager.getInstance().endAutoevent(true);
	}
	
	/** detailed debug */
	protected void print(String msg)
	{
		NexusLoader.detailedDebug(msg);
	}
}
