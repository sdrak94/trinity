package cz.nxs.events.engine;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import cz.nxs.events.Configurable;
import cz.nxs.events.EventGame;
import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.base.Event;
import cz.nxs.events.engine.base.EventPlayerData;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.html.EventHtmlManager;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.MainEventManager;
import cz.nxs.events.engine.main.events.AbstractMainEvent;
import cz.nxs.events.engine.mini.MiniEventGame;
import cz.nxs.events.engine.mini.MiniEventManager;
import cz.nxs.events.engine.mini.events.KoreanManager;
import cz.nxs.events.engine.mini.events.MiniTvTManager;
import cz.nxs.events.engine.mini.events.OnevsOneManager;
import cz.nxs.events.engine.mini.events.PartyvsPartyManager;
import cz.nxs.events.engine.stats.EventStatsManager;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.NexusEvents;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.NpcTemplateData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.interf.handlers.AdminCommandHandlerInstance;
import cz.nxs.l2j.CallBack;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.util.Rnd;

/**
 * @author hNoke
 * - loads up and stores events and their managers
 */
public class EventManager
{
	private Map<EventType, Map<Integer, MiniEventManager>> _miniEvents;
	private Map<EventType, AbstractMainEvent> _mainEvents;
	
	private Map<DisconnectedPlayerData, Long> _disconnectedPlayers;
	
	private MainEventManager _mainManager;
	
	private EventHtmlManager _html;
	
	public static boolean ALLOW_VOICE_COMMANDS = EventConfig.getInstance().getGlobalConfigBoolean("allowVoicedCommands");
	public static String REGISTER_VOICE_COMMAND =  EventConfig.getInstance().getGlobalConfigValue("registerVoicedCommand");
	public static String UNREGISTER_VOICE_COMMAND =  EventConfig.getInstance().getGlobalConfigValue("unregisterVoicedCommand");
	
	public EventManager()
	{
		CallBack.getInstance().getOut().registerAdminHandler(new AdminNexus()); //added in core
		
		_miniEvents = new FastMap<EventType, Map<Integer, MiniEventManager>>(EventType.values().length);
		_mainEvents = new FastMap<EventType, AbstractMainEvent>(EventType.values().length);
		
		NexusLoader.debug("Nexus engine: Loading events...");
		loadEvents();
	}
	
	private void loadEvents()
	{
		int count = 0;
		
		_disconnectedPlayers = new FastMap<DisconnectedPlayerData, Long>();
		_mainManager = new MainEventManager();
		
		for(EventType event : EventType.values())
		{
			if(event == EventType.Unassigned)
				continue;
			
			Event eventInstance = event.loadEvent(_mainManager);
			if(eventInstance == null)
			{
				//NexusLoader.debug("Event type " + event.getAltTitle() + " isn't finished yet. Just a debug message!");
				continue;
			}
			
			if(event.isRegularEvent())
			{
				_mainEvents.put(eventInstance.getEventType(), (AbstractMainEvent) eventInstance);
				count ++;
			}
		}
		
		NexusLoader.debug("Nexus engine: Loaded " + count + " main events.");
		
		_miniEvents.put(EventType.Unassigned, new FastMap<Integer, MiniEventManager>());
		_miniEvents.put(EventType.Classic_1v1, new FastMap<Integer, MiniEventManager>());
		_miniEvents.put(EventType.PartyvsParty, new FastMap<Integer, MiniEventManager>());
		_miniEvents.put(EventType.Korean, new FastMap<Integer, MiniEventManager>());
		_miniEvents.put(EventType.MiniTvT, new FastMap<Integer, MiniEventManager>());
		
		NexusLoader.debug("Nexus engine: Loaded " + _miniEvents.size() + " mini event types.");
		
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				setAutoevents(false);
			}
		}, 60000);
	}
	
	public void setHtmlManager(EventHtmlManager manager)
	{
		_html = manager;
	}
	
	public MiniEventManager createManager(EventType type, int modeId, String name, String visibleName, boolean loadConfigs)
	{
		MiniEventManager manager = null;
		switch(type)
		{
			case Classic_1v1:
				manager = new OnevsOneManager(type);
				break;
			case PartyvsParty:
				manager = new PartyvsPartyManager(type);
				break;
			case Korean:
				manager = new KoreanManager(type);
				break;
			case MiniTvT:
				manager = new MiniTvTManager(type);
				break;
			
			default:
				NexusLoader.debug("Event " + type.getAltTitle() + " isn't implemented yet.", Level.WARNING);
				return null;
		}
		
		manager.getMode().setModeName(name);
		manager.getMode().setVisibleName(visibleName);
		
		if(loadConfigs)
		{
			Configurable defaultMode = getEvent(type, 1);
			if(defaultMode != null)
			{
				manager.setConfigs(defaultMode);
			}
		}
		
		_miniEvents.get(type).put(modeId, manager);
		
		return manager;
	}
	
	public static EventManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public Map<EventType, Map<Integer, MiniEventManager>> getMiniEvents()
	{
		return _miniEvents;
	}
	
	public Map<EventType, AbstractMainEvent> getMainEvents()
	{
		return _mainEvents;
	}
	
	public Configurable getEvent(EventType type)
	{
		return getEvent(type, 1);
	}
	
	public Configurable getEvent(EventType type, int modeId)
	{
		if(type.isRegularEvent())
			return getMainEvent(type);
		else
			return getMiniEvent(type, modeId);
	}
	
	public MiniEventManager getMiniEvent(EventType type, int id)
	{
		if(_miniEvents.get(type) == null)
		{
			//NexusLoader.debug("EventManager - type " + type.getAltTitle() + " isn't in the collection!");
			return null;
		}
		
		return _miniEvents.get(type).get(id);
	}
	
	public AbstractMainEvent getMainEvent(EventType type)
	{
		if(!_mainEvents.containsKey(type))
		{
			//NexusLoader.debug("EventManager - type " + type.getAltTitle() + " isn't in the collection!");
			return null;
		}
		
		return _mainEvents.get(type);
	}
	
	public AbstractMainEvent getCurrentMainEvent()
	{
		return _mainManager.getCurrent();
	}
	
	public boolean onBypass(PlayerEventInfo player, String bypass)
	{
		return _html.onBypass(player, bypass);
	}
	
	public boolean showNpcHtml(PlayerEventInfo player, NpcData npc)
	{
		return _html.showNpcHtml(player, npc);
	}
	
	public EventHtmlManager getHtmlManager()
	{
		return _html;
	}
	
	public boolean canRegister(PlayerEventInfo player)
	{
		if (player.isInJail())
		{
			player.sendMessage(LanguageEngine.getMsg("registering_jail"));
			return false;
		}
		
		if (player.isInSiege())
		{
			player.sendMessage(LanguageEngine.getMsg("registering_siege"));
			return false;
		}
		
		if (player.isInDuel())
		{
			player.sendMessage(LanguageEngine.getMsg("registering_duel"));
			return false;
		}

		if (player.isOlympiadRegistered() || player.isInOlympiadMode())
		{
			player.sendMessage(LanguageEngine.getMsg("registering_olympiad"));
			return false;
		}
		
		if (player.getKarma() > 0)
		{
			player.sendMessage(LanguageEngine.getMsg("registering_karma"));
			return false;
		}

		if (player.isCursedWeaponEquipped())
		{
			player.sendMessage(LanguageEngine.getMsg("registering_cursedWeapon"));
			return false;
		}
		
		return true;
	}
	
	public boolean isInEvent(CharacterData cha)
	{
		if(getCurrentMainEvent() != null)
			return getCurrentMainEvent().isInEvent(cha);
		return false;
	}
	
	public boolean allowDie(CharacterData cha, CharacterData killer)
	{
		if(getCurrentMainEvent() != null)
			return getCurrentMainEvent().allowKill(cha, killer);
		return true;
	}
	
	public void onDamageGive(CharacterData cha, CharacterData attacker, int damage, boolean isDOT)
	{
		if(getCurrentMainEvent() != null)
			getCurrentMainEvent().onDamageGive(attacker, cha, damage, isDOT);
	}
	
	public boolean onAttack(CharacterData cha, CharacterData target)
	{
		if(getCurrentMainEvent() != null)
			return getCurrentMainEvent().onAttack(cha, target);
		return true;
	}
	
	public boolean tryVoicedCommand(PlayerEventInfo player, String text)
	{
		if(player != null && ALLOW_VOICE_COMMANDS)
		{
			if(text.equalsIgnoreCase(REGISTER_VOICE_COMMAND))
			{
				EventManager.getInstance().getMainEventManager().registerPlayer(player);
				return true;
			}
			else if(text.equalsIgnoreCase(UNREGISTER_VOICE_COMMAND))
			{
				EventManager.getInstance().getMainEventManager().unregisterPlayer(player, false);
				return true;
			}
			else if(text.equalsIgnoreCase(".suicide"))
			{
				if(player.isInEvent())
				{
					player.sendMessage("You are being suicided.");
					player.doDie();
					return true;
				}
			}
		}
		
		return false;
	}
	
	public void removeEventSkills(PlayerEventInfo player)
	{
		for(SkillData sk : player.getSkills())
		{
			if(sk.getId() >= 35000 && sk.getId() <= 35099)
			{
				player.removeBuff(sk.getId());
				player.removeSkill(sk.getId());
			}
		}
	}
	
	public void onPlayerLogin(final PlayerEventInfo player)
	{
		removeEventSkills(player);
		EventStatsManager.getInstance().onLogin(player);
		
		DisconnectedPlayerData data = null;
		
		for(Entry<DisconnectedPlayerData, Long> e : _disconnectedPlayers.entrySet())
		{
			if(e.getKey().player.getPlayersId() == player.getPlayersId())
			{
				data = e.getKey();
				_disconnectedPlayers.remove(e.getKey());
				break;
			}
		}
		
		if(data != null)
		{
			final DisconnectedPlayerData fData = data;
			final EventGame event = data.event;
			
			if(event != null)
			{
				CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						event.addDisconnectedPlayer(player, fData);
					}
				}, 1500);
			}
		}
	}
	
	public void addDisconnectedPlayer(PlayerEventInfo player, EventTeam team, EventPlayerData d, EventGame event)
	{
		long time = System.currentTimeMillis();
		DisconnectedPlayerData data = new DisconnectedPlayerData(player, event, d, team, time, player.getInstanceId());
		
		_disconnectedPlayers.put(data, time);
	}
	
	public void clearDisconnectedPlayers()
	{
		_disconnectedPlayers.clear();
	}

	public void spectateGame(PlayerEventInfo player, EventType event, int modeId, int gameId)
	{
		MiniEventManager manager = getMiniEvent(event, modeId);
		
		if(manager == null)
		{
			player.sendStaticPacket();
			return;
		}
		
		MiniEventGame game = null;
		
		for(MiniEventGame g : manager.getActiveGames())
		{
			if(g.getGameId() == gameId)
			{
				game = g;
				break;
			}
		}
		
		if(game == null)
		{
			player.sendMessage(LanguageEngine.getMsg("observing_gameEnded"));
			return;
		}
		
		if(!canRegister(player))
		{
			player.sendMessage(LanguageEngine.getMsg("observing_cant"));
			return;
		}
		
		if(player.isRegistered())
		{
			player.sendMessage(LanguageEngine.getMsg("observing_alreadyRegistered"));
			return;
		}
		
		CallBack.getInstance().getPlayerBase().addInfo(player);
		player.initOrigInfo();
		
		game.addSpectator(player);
	}

	public void removePlayerFromObserverMode(PlayerEventInfo pi)
	{
		MiniEventGame game = pi.getActiveGame();
		if(game == null)
			return;
		
		game.removeSpectator(pi, false);
	}
	
	public String getDarkColorForHtml(int teamId)
	{
		switch(teamId)
		{
			case 1:
				return "7C8194"; // blue
			case 2:
				return "987878"; // red
			case 3:
				return "868F81"; // green
			case 4:
				return "937D8D"; // purple
			case 5:
				return "93937D"; // yellow
			case 6:
				return "D2934D"; // orange
			case 7:
				return "3EC1C1"; // teal
			case 8:
				return "D696D1"; // pink
			case 9:
				return "9B7957"; // brown
			case 10:
				return "949494"; // grey
		}
		return "8f8f8f";
	}
	
	public String getTeamColorForHtml(int teamId)
	{
		switch(teamId)
		{
			case 1:
				return "5083CF"; // blue
			case 2:
				return "D04F4F"; // red
			case 3:
				return "56C965"; // green
			case 4:
				return "9F52CD"; // purple
			case 5:
				return "DAC73D"; // yellow
			case 6:
				return "D2934D"; // orange
			case 7:
				return "3EC1C1"; // teal
			case 8:
				return "D696D1"; // pink
			case 9:
				return "9B7957"; // brown
			case 10:
				return "949494"; // grey
		}
		return "FFFFFF";
	}
	
	public int getTeamColorForName(int teamId)
	{
		switch(teamId)
		{
			case 1: 
				return 0xCF8350; // blue
			case 2:
				return 0x4F4FD0; // red
			case 3:
				return 0x65C956; // green
			case 4:
				return 0xCD529F; // purple
			case 5:
				return 0x3DC7DA; // yellow
			case 6:
				return 0x4D93D2; // orange
			case 7:
				return 0xC1C13E; // teal
			case 8:
				return 0xD196D6; // pink
			case 9:
				return 0x57799B; // brown
			case 10:
				return 0x949494; // grey
		}
		return 0;
	}
	
	public String getTeamName(int teamId)
	{
		switch(teamId)
		{
			case 1: 
				return "Blue"; // blue
			case 2:
				return "Red"; // red
			case 3:
				return "Green"; // green
			case 4:
				return "Purple"; // purple
			case 5:
				return "Yellow"; // yellow
			case 6:
				return "Orange"; // orange
			case 7:
				return "Teal"; // teal
			case 8:
				return "Pink"; // pink
			case 9:
				return "Brown";
			case 10:
				return "Grey"; // grey
		}
		return "No";
	}
	
	public void debug(String message)
	{
		NexusLoader.debug(message);
	}
	
	public void debug(Exception e)
	{
		e.printStackTrace();
	}
	
	public MainEventManager getMainEventManager()
	{
		return _mainManager;
	}

	/** compares PlayerEventInfo collection by their levels - descending */
	public Comparator<PlayerEventInfo> compareByLevels = new Comparator<PlayerEventInfo>()
	{
		@Override
		public int compare(PlayerEventInfo o1, PlayerEventInfo o2)
		{
			int level1 = o1.getLevel();
			int level2 = o2.getLevel();
			
			return level1 == level2 ? 0 : level1 < level2 ? 1 : -1;
		}
	};
	
	/** compares PlayerEventInfo collection by their pvp kills - descending */
	public Comparator<PlayerEventInfo> compareByPvps = new Comparator<PlayerEventInfo>()
	{
		@Override
		public int compare(PlayerEventInfo o1, PlayerEventInfo o2)
		{
			int pvp1 = o1.getPvpKills();
			int pvp2 = o2.getPvpKills();
			
			return pvp1 == pvp2 ? 0 : pvp1 < pvp2 ? 1 : -1;
		}
	};
	
	/** sorts event teams by their kills ammount */
	public Comparator<EventTeam> compareTeamKills = new Comparator<EventTeam>()
	{
		@Override
		public int compare(EventTeam t1, EventTeam t2)
		{
			int kills1 = t1.getKills();
			int kills2 = t2.getKills();
			
			return kills1 == kills2 ? 0 : kills1 < kills2 ? 1 : -1;
		}
	};
	
	public Comparator<PlayerEventInfo> comparePlayersKills = new Comparator<PlayerEventInfo>()
	{
		@Override
		public int compare(PlayerEventInfo p1, PlayerEventInfo p2)
		{
			int kills1 = p1.getKills();
			int kills2 = p2.getKills();
			
			return kills1 == kills2 ? 0 : kills1 < kills2 ? 1 : -1;
		}
	};
	
	public Comparator<PlayerEventInfo> comparePlayersScore = new Comparator<PlayerEventInfo>()
	{
		@Override
		public int compare(PlayerEventInfo p1, PlayerEventInfo p2)
		{
			int score1 = p1.getScore();
			int score2 = p2.getScore();
			
			if(score1 == score2)
			{
				int deaths1 = p1.getDeaths();
				int deaths2 = p2.getDeaths();
				
				return deaths1 == deaths2 ? 0 : deaths1 < deaths2 ? -1 : 1;
			}
			else
				return score1 < score2 ? 1 : -1;
		}
	};
	
	public Comparator<EventTeam> compareTeamScore = new Comparator<EventTeam>()
	{
		@Override
		public int compare(EventTeam t1, EventTeam t2)
		{
			int score1 = t1.getScore();
			int score2 = t2.getScore();
			
			return score1 == score2 ? 0 : score1 < score2 ? 1 : -1;
		}
	};
	
	public class DisconnectedPlayerData
	{
		private PlayerEventInfo player;
		private EventGame event;
		private EventPlayerData data;
		private EventTeam team;
		private long time;
		private int instance;
		
		public DisconnectedPlayerData(PlayerEventInfo player, EventGame event, EventPlayerData data, EventTeam team, long time, int instance)
		{
			this.time = time;
			this.player = player;
			this.data = data;
			this.team = team;
			this.event = event;
			this.instance = instance;
		}
		
		public PlayerEventInfo getPlayer()
		{
			return player;
		}
		
		public EventGame getEvent()
		{
			return event;
		}
		
		public EventTeam getTeam()
		{
			return team;
		}
		
		public EventPlayerData getPlayerData()
		{
			return data;
		}
		
		public long getTime()
		{
			return time;
		}
		
		public int getInstance()
		{
			return instance;
		}
	}
	
	public class AdminNexus extends AdminCommandHandlerInstance
	{
		private final String[] ADMIN_COMMANDS =
		{
			"admin_event_manage"
		};

		@Override
		public boolean useAdminCommand(String command, PlayerEventInfo activeChar)
		{
			if (command.startsWith("admin_event_manage"))
			{
				StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				
				if(!st.hasMoreTokens())
					NexusEvents.onAdminBypass(activeChar, "menu");
				else
					NexusEvents.onAdminBypass(activeChar, command.substring(19));
			}

			return true;
		}
		
		@Override
		public String[] getAdminCommandList()
		{
			return ADMIN_COMMANDS;
		}
	}
	
	public static final int FIRST_EVENT_DELAY = 60000 * 60; // 60 minutes
	public static final int WAITING_DURATION = 60000 * 160; // 100 minutes
	public static final int MINI_EVENT_DURATION = 60000 * 40; // 1.5 hour
	
	public ScheduledFuture<?> _autoeventfuture;
	public TypeOfEvent _lastEventType;
	@SuppressWarnings("unused")
	private boolean lastMainEvent;
	
	private List<MiniEventManager> _openedEvents = new FastList<MiniEventManager>();
	
	public int _eventsSinceLastPvpZoneSwitch = 0;
	
	private final int NUMEVENTS_TO_SWITCH_PVPZONE = 4;
	
	enum TypeOfEvent
	{
		MINI, MAIN, WAITING
	}
	
	public void pvpZoneSwitched()
	{
		_eventsSinceLastPvpZoneSwitch = 0;
	}
	
	public void startAutoevents()
	{
		getMainEventManager().abortAutoScheduler(null);
		
		_lastEventType = null;
		
		endAutoevent(true);
	}
	
	public void endAutoevent(boolean startNext)
	{
		if(!getIsAutoeventsEnabled() && startNext)
			return;
		
		TypeOfEvent next = null;
		
		if(_lastEventType == null)
		{
			next = TypeOfEvent.WAITING;
		}
		else
		{
			switch(_lastEventType)
			{
				case WAITING:
					
					next = TypeOfEvent.MAIN;
					
					break;
					
				case MAIN:
					
					// event is already ended by nexus engine
					
					next = TypeOfEvent.MINI;
					break;
				case MINI:
					
					announce("The Mini events have been closed.");
					
					for(MiniEventManager manager : _openedEvents)
					{
						//manager.cleanMe(true); // let the remaining matches finish
						manager.getMode().setAllowed(false);
					}
					_openedEvents.clear();
					
					next = TypeOfEvent.WAITING;
					
					break;
			}
		}

		if(startNext)
			start(next);
	}
	
	public NpcData _registrationNpc = null;
	
	private boolean _firstEvent = true;
	
	private void start(TypeOfEvent next)
	{
		_lastEventType = next;
		
		if(next == TypeOfEvent.WAITING)
		{
			int duration = WAITING_DURATION;
			
			if(_firstEvent)
			{
				duration = FIRST_EVENT_DELAY;
				_firstEvent = false;
				
				CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						announce("A new event (" + EventManager.getInstance().getMainEventManager().getGuessedNextEvent().getHtmlTitle() + ") will start in " + (FIRST_EVENT_DELAY/60000) + " minutes.");
					}
				}, 5000);
			}
			else
			{
				CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						announce("A new event (" + EventManager.getInstance().getMainEventManager().getGuessedNextEvent().getHtmlTitle() + ") will start in " + (WAITING_DURATION/60000) + " minutes.");
					}
				}, 5000);
			}
			
			_eventsSinceLastPvpZoneSwitch ++;
			
			if(_eventsSinceLastPvpZoneSwitch >= NUMEVENTS_TO_SWITCH_PVPZONE)
			{
				CallBack.getInstance().getOut().loadNextPvpZone();
			}
			
			_autoeventfuture = CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					endAutoevent(true);
				}
			}, duration);
			
			if(_registrationNpc != null)
			{
				_registrationNpc.deleteMe();
				_registrationNpc = null;
			}
		}
		else if(next == TypeOfEvent.MINI)
		{
			int duration = MINI_EVENT_DURATION;
			
			// one single and one party mini event
			
			MiniEventManager event = null;
			
			// 1v1 event
			event = getMiniEvent(EventType.Classic_1v1, 1);
			if(event != null)
				_openedEvents.add(event);
			
			// 2v2 event - 100%
			event = getMiniEvent(EventType.PartyvsParty, 2);
			if(event != null)
				_openedEvents.add(event);
			
			if(Rnd.get(2) == 0)
			{
				// 4v4
				event = getMiniEvent(EventType.PartyvsParty, 7);
				if(event != null)
					_openedEvents.add(event);
				
				// 7v7 korean
				event = getMiniEvent(EventType.Korean, 6);
				if(event != null)
					_openedEvents.add(event);
			}
			else
			{
				// 7v7
				event = getMiniEvent(EventType.PartyvsParty, 8);
				if(event != null)
					_openedEvents.add(event);
				
				// 4v4 korean
				event = getMiniEvent(EventType.Korean, 5);
				if(event != null)
					_openedEvents.add(event);
			}
			
			for(MiniEventManager manager : _openedEvents)
			{
				manager.getMode().setAllowed(true);
			}
			
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					announce("The Mini Events have been opened for " + (MINI_EVENT_DURATION/60000) + " minutes.");
				}
			}, 10000);
			
			_autoeventfuture = CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					endAutoevent(true);
				}
			}, duration);
			
			if(_registrationNpc != null)
			{
				_registrationNpc.deleteMe();
				_registrationNpc = null;
			}
			
			int id = 9998;
			
			NpcData data;
			final NpcTemplateData template = new NpcTemplateData(id);
			
			Location loc = new Location(116588, 76165, -2731); //TODO change loc
			
			data = template.doSpawn(loc.getX(), loc.getY(), loc.getZ(), 1, 0);
			
			SkillData skill = new SkillData(5966, 1);
			if(skill.exists())
				data.broadcastSkillUse(data, data, skill.getId(), skill.getLevel());
			
			_registrationNpc = data;
		}
		else if(next == TypeOfEvent.MAIN)
		{
			if(_registrationNpc != null)
			{
				_registrationNpc.deleteMe();
				_registrationNpc = null;
			}
			
			_autoeventfuture = CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					EventManager.getInstance().getMainEventManager().getScheduler().run();
				}
			}, 20000);
			
			// don't schedule anything, just wait till the event calls nextEvent itself
		}
	}
	
	public void announce(String text)
	{
		String announcer = "Events";
		
		CallBack.getInstance().getOut().announceToAllScreenMessage(text, announcer);
	}
	
	public void disableAutoevents()
	{
		endAutoevent(false);
		
		if(_autoeventfuture != null)
		{
			_autoeventfuture.cancel(false);
			_autoeventfuture = null;
		}
	}
	
	private boolean _autoevents = true;
	public boolean getIsAutoeventsEnabled()
	{
		return _autoevents;
	}
	
	public void setAutoevents(boolean b)
	{
		_autoevents = b;
		
		if(!_autoevents)
			disableAutoevents();
		else
			startAutoevents();
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EventManager _instance = new EventManager();
	}
}
