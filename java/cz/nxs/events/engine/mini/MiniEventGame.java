package cz.nxs.events.engine.mini;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.nxs.events.EventGame;
import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventManager.DisconnectedPlayerData;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.events.engine.mini.features.AbstractFeature;
import cz.nxs.events.engine.mini.features.BufferFeature;
import cz.nxs.events.engine.mini.features.EnchantFeature;
import cz.nxs.events.engine.mini.features.ItemGradesFeature;
import cz.nxs.events.engine.mini.features.ItemsFeature;
import cz.nxs.events.engine.mini.features.SkillsFeature;
import cz.nxs.events.engine.mini.features.TimeLimitFeature;
import cz.nxs.events.engine.stats.EventStatsManager;
import cz.nxs.events.engine.stats.GlobalStats.GlobalStatType;
import cz.nxs.events.engine.stats.GlobalStatsModel;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.DoorData;
import cz.nxs.interf.delegate.FenceData;
import cz.nxs.interf.delegate.ItemData;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.NpcTemplateData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 * - a mother class of all mini event games 
 */
public abstract class MiniEventGame implements Runnable, EventGame
{
	protected static Logger _log = Logger.getLogger(MiniEventGame.class.getName());

	protected RegistrationData[] _registeredTeams;

	public static final int MAP_GUARD_ID = 9996;
	
	protected int _instanceId;
	
	protected int _gameId;
	
	protected MiniEventManager _event;
	protected EventMap _arena;
	
	protected Announcer _announcer;
	
	private LocChecker _locCheckerInstance;
	
	protected ScheduledFuture<?> _locChecker = null;
	private static int _locCheckDelay = 10000; //TODO add as config
	
	protected List<PlayerEventInfo> _spectators;
	List<PlayerEventInfo> _voted;
	protected List<FenceData> _fences;
	protected List<NpcData> _buffers;
	protected List<NpcData> _mapGuards;
	protected List<NpcData> _npcs;
	
	protected boolean _aborted = false;
	
	private int[] notAllovedSkillls;
	private int[] notAllovedItems;
	
	private int[] setOffensiveSkills;
	private int[] setNotOffensiveSkills;
	private int[] setNeutralSkills;
	
	protected boolean _allowSchemeBuffer;
	protected boolean _allowSummons;
	protected boolean _allowPets;
	
	public MiniEventGame(int gameId, EventMap arena, MiniEventManager event, RegistrationData[] teams)
	{
		_gameId = gameId;
		_event = event;
		_arena = arena;
		_instanceId = 0;
		_registeredTeams = teams;
	}

	public abstract int getInstanceId();
	public abstract EventTeam[] getTeams();
	
	protected void initAnnouncer()
	{
		_announcer = new Announcer();
		_announcer.setTime(System.currentTimeMillis() + getGameTime());
	}
	
	@Override
	public void run()
	{
		initEvent();
	}
	
	//TODO call me from mini events on start
	public void scheduleLocChecker()
	{
		if(_locCheckerInstance == null)
			_locCheckerInstance = new LocChecker();
		
		_locChecker = CallBack.getInstance().getOut().scheduleGeneral(_locCheckerInstance, _locCheckDelay);
	}
	
	private class LocChecker implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				checkPlayersLoc();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			scheduleLocChecker();
		}
	}
	
	protected abstract void checkPlayersLoc();
	protected abstract void checkIfPlayersTeleported();
	
	public void addSpectator(PlayerEventInfo player)
	{
		if(_spectators == null)
			_spectators = new FastList<PlayerEventInfo>();
		
		EventSpawn spectatorLoc = getMap().getNextSpawn(-1, SpawnType.Spectator);
		if(spectatorLoc == null)
			spectatorLoc = getMap().getNextSpawn(-1, SpawnType.Regular);
		
		if(spectatorLoc == null)
		{
			player.sendMessage(LanguageEngine.getMsg("observing_noSpawn"));
			return;
		}
		
		player.setIsSpectator(true);
		player.setActiveGame(this);
		
		player.removeSummon();
		
		player.removeCubics();
		
		if (player.getParty() != null)
			player.getParty().removePartyMember(player);
		
		player.setInstanceId(_instanceId);
		player.enterObserverMode(spectatorLoc.getLoc().getX(), spectatorLoc.getLoc().getY(), spectatorLoc.getLoc().getZ());
		
		_spectators.add(player);
	}
	
	public void removeSpectator(PlayerEventInfo pi, boolean disconnect)
	{
		if(!pi.isOnline())
			return;
		
		if(!disconnect)
		{
			pi.removeObserveMode();
			CallBack.getInstance().getPlayerBase().eventEnd(pi);
		}
		
		_spectators.remove(pi);
	}
	
	protected void cleanSpectators()
	{
		if(_spectators != null)
		{
			for(PlayerEventInfo pi : _spectators)
			{
				removeSpectator(pi, false);
			}
		}
	}
	
	protected void initEvent()
	{
		_instanceId = CallBack.getInstance().getOut().createInstance("Game " + getEvent().getEventName() + " ID" + _gameId, getGameTime() + 59000, 0, true).getId();
		
		handleDoors(0);
		
		loadFences();
		CallBack.getInstance().getOut().spawnFences(_fences, _instanceId);
		
		loadNpcs();
		loadMapGuards();

		initAnnouncer();
		
		_allowSchemeBuffer = EventConfig.getInstance().getGlobalConfigBoolean("eventSchemeBuffer");
		_allowSummons = getEvent().getBoolean("allowSummons");
		_allowPets = getEvent().getBoolean("allowPets");
		
		// load not alloved skills
		if(!_event.getString("notAllowedSkills").equals(""))
		{
			String[] splits = _event.getString("notAllowedSkills").split(",");
			notAllovedSkillls = new int[splits.length];
			
			try
			{
				for(int i = 0; i < splits.length; i++)
				{
					notAllovedSkillls[i] = Integer.parseInt(splits[i]);
				}
				
				Arrays.sort(notAllovedSkillls);
			}
			catch (Exception e)
			{
				//NexusLoader.debug("Error while loading config 'notAllowedSkills' for event " + _event.getEventName() + " - " + e.toString(), Level.SEVERE);
			}
		}
		
		// load not alloved items
		if(!_event.getString("notAllowedItems").equals(""))
		{
			String[] splits = _event.getString("notAllowedItems").split(",");
			notAllovedItems = new int[splits.length];
			
			try
			{
				for(int i = 0; i < splits.length; i++)
				{
					notAllovedItems[i] = Integer.parseInt(splits[i]);
				}
				
				Arrays.sort(notAllovedItems);
			}
			catch (Exception e)
			{
				//NexusLoader.debug("Error while loading config 'notAllowedItems' for event " + _event.getEventName() + " - " + e.toString(), Level.SEVERE);
			}
		}
		
		loadOverridenSkillsParameters();
	}
	
	private void loadOverridenSkillsParameters()
	{
		String s;
		
		// offensive skills
		s = EventConfig.getInstance().getGlobalConfigValue("setOffensiveSkills");
		try
		{
			String[] splits = s.split(";");
			setOffensiveSkills = new int[splits.length];
			
			try
			{
				for(int i = 0; i < splits.length; i++)
				{
					setOffensiveSkills[i] = Integer.parseInt(splits[i]);
				}
				
				Arrays.sort(setOffensiveSkills);
			}
			catch (Exception e)
			{
				NexusLoader.debug("Error while loading GLOBAL config 'setOffensiveSkills' in event " + _event.getEventName() + " - " + e.toString(), Level.SEVERE);
			}
		}
		catch (Exception e)
		{
			NexusLoader.debug("Error while loading GLOBAL config 'setOffensiveSkills' in event " + _event.getEventName() + " - " + e.toString(), Level.SEVERE);
		}
		
		// not offensive
		s = EventConfig.getInstance().getGlobalConfigValue("setNotOffensiveSkills");
		try
		{
			String[] splits = s.split(";");
			setNotOffensiveSkills = new int[splits.length];
			
			try
			{
				for(int i = 0; i < splits.length; i++)
				{
					setNotOffensiveSkills[i] = Integer.parseInt(splits[i]);
				}
				
				Arrays.sort(setNotOffensiveSkills);
			}
			catch (Exception e)
			{
				NexusLoader.debug("Error while loading GLOBAL config 'setNotOffensiveSkills' in event " + _event.getEventName() + " - " + e.toString(), Level.SEVERE);
			}
		}
		catch (Exception e)
		{
			NexusLoader.debug("Error while loading GLOBAL config 'setNotOffensiveSkills' in event " + _event.getEventName() + " - " + e.toString(), Level.SEVERE);
		}

		// neutral skills
		s = EventConfig.getInstance().getGlobalConfigValue("setNeutralSkills");
		try
		{
			String[] splits = s.split(";");
			setNeutralSkills = new int[splits.length];
			
			try
			{
				for(int i = 0; i < splits.length; i++)
				{
					setNeutralSkills[i] = Integer.parseInt(splits[i]);
				}
				
				Arrays.sort(setNeutralSkills);
			}
			catch (Exception e)
			{
				NexusLoader.debug("Error while loading GLOBAL config 'setNeutralSkills' in event " + _event.getEventName() + " - " + e.toString(), Level.SEVERE);
			}
		}
		catch (Exception e)
		{
			NexusLoader.debug("Error while loading GLOBAL config 'setNeutralSkills' in event " + _event.getEventName() + " - " + e.toString(), Level.SEVERE);
		}
	}
	
	/**
	 * returns: 1 = is forced offensive, 0 = is forced to not be offensive, -1 default L2Skill value
	 */
	@Override
	public int isSkillOffensive(SkillData skill)
	{
		if (setOffensiveSkills != null && Arrays.binarySearch(setOffensiveSkills, skill.getId()) >= 0)
			return 1;
		else if (setNotOffensiveSkills != null && Arrays.binarySearch(setNotOffensiveSkills, skill.getId()) >= 0)
			return 0;
		
		return -1;
	}
	
	@Override
	public boolean isSkillNeutral(SkillData skill)
	{
		if (setNeutralSkills != null && Arrays.binarySearch(setNeutralSkills, skill.getId()) >= 0)
			return true;
		return false;
	}
	
	protected void updateScore(PlayerEventInfo player, CharacterData killer)
	{
		player.raiseDeaths(1);
		player.getEventTeam().raiseDeaths(1);
		
		if(killer != null)
		{
			if(killer.getEventInfo() != null)
			{
				if(killer.getEventInfo().getEventTeam() == null)
				{
					//NexusLoader.debug("no event team found for character " + killer.getName() + " on 1v1 event!!!", Level.SEVERE);
					return;
				}
				
				killer.getEventInfo().raiseKills(1);
				
				killer.getEventInfo().getEventTeam().raiseKills(1);
			}
		}
	}

	protected void startEvent()
	{
		
	}
	
	protected void setEndStatus(PlayerEventInfo pi, int status)
	{
		//_statsTable.get(pi.getPlayersId())[0] = status;
	}
	
	public void applyStatsChanges()
	{
		
	}
	
	protected void onScore(List<PlayerEventInfo> players, int ammount)
	{
		
	}
	
	public void abortDueToError(String message)
	{
		broadcastMessage(message, false);
		clearEvent();
		
		EventManager.getInstance().debug(_event.getEventType() + " match automatically aborted: " + message);
	}
	
	public void broadcastMessage(String msg, boolean abortable)
	{
		if(abortable && _aborted)
			return;
		
		for(EventTeam team : getTeams())
		{
			for(PlayerEventInfo pi : team.getPlayers())
			{
				pi.screenMessage(msg, getEvent().getEventName(), false);
			}
		}
		
		if(_spectators != null)
		{
			for(PlayerEventInfo pi : _spectators)
			{
				pi.screenMessage(msg, getEvent().getEventName(), false);
			}
		}
	}
	
	protected boolean checkTeamStatus(int teamId)
	{
		for(PlayerEventInfo pi : getTeams()[teamId - 1].getPlayers())
		{
			if(pi.isOnline())
			{
				if(!pi.isDead())
					return true;
			}
		}
		// everyone is death
		return false;
	}

	protected void loadFences()
	{
		try
		{
			_fences = new FastList<FenceData>();
			for(EventSpawn spawn : _arena.getSpawns(0, SpawnType.Fence))
			{
				_fences.add(CallBack.getInstance().getOut().createFence(2, spawn.getFenceWidth(), spawn.getFenceLength(), spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ(), _arena.getGlobalId()));
			}
		} 
		catch (NullPointerException e)
		{
			//EventManager.getInstance().debug(e);
		}
	}
	
	private void loadMapGuards()
	{
		int id = EventConfig.getInstance().getGlobalConfigInt("mapGuardNpcId");
		
		if(id == -1)
			return;
		
		NpcTemplateData template = new NpcTemplateData(id);
		
		if(!template.exists())
		{
			_log.warning("Missing template for EventMap Guard.");
			return;
		}
		
		for(EventSpawn spawn : getMap().getSpawns())
		{
			if(spawn.getSpawnType() == SpawnType.MapGuard)
			{
				try
				{
					NpcData data = template.doSpawn(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ(), 1, _instanceId);
					
					if(_mapGuards == null)
						_mapGuards = new FastList<NpcData>();
					
					_mapGuards.add(data);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	protected void loadNpcs()
	{
		for(EventSpawn spawn : getMap().getSpawns(-1, SpawnType.Npc))
		{
			try
			{
				int npcId = spawn.getNpcId();
				if(npcId == -1)
					continue;
				
				NpcData data = new NpcTemplateData(npcId).doSpawn(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ(), 1, _instanceId);
				
				if(_npcs == null)
					_npcs = new FastList<NpcData>();
				
				_npcs.add(data);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	protected void unspawnNpcs()
	{
		if(_npcs != null)
		{
			for(NpcData npc : _npcs)
			{
				if(npc != null)
					npc.deleteMe();
			}
			_npcs.clear();
		}
	}
	
	protected void loadBuffers()
	{
		try
		{
			int bufferId = EventConfig.getInstance().getGlobalConfigInt("npcBufferId");
			
			for(AbstractFeature feature : _event.getMode().getFeatures())
			{
				if(feature.getType() == FeatureType.Buffer)
				{
					if(!((BufferFeature) feature).canSpawnBuffer())
						return;
					
					if(((BufferFeature) feature).getCustomNpcBufferId() != 0)
						bufferId = ((BufferFeature) feature).getCustomNpcBufferId();
				}
			}
			
			if(bufferId == -1)
				return;
			
			NpcTemplateData template = new NpcTemplateData(bufferId);
			
			if(!template.exists())
			{
				_log.warning("Missing NPC Buffer's template (ID " + bufferId + ") for event system.");
				return;
			}
			
			for(EventSpawn spawn : _arena.getSpawns())
			{
				if(spawn.getSpawnType() == SpawnType.Buffer)
				{
					NpcData data = template.doSpawn(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ(), 1, _instanceId);
					
					if(_buffers == null)
						_buffers = new FastList<NpcData>();
					
					_buffers.add(data);
				}
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	protected void unspawnBuffers()
	{
		if(_buffers != null)
		{
			for(NpcData npc : _buffers)
			{
				if(npc != null)
					npc.deleteMe();
			}
			_buffers.clear();
		}
	}
	
	protected void unspawnMapGuards()
	{
		if(_mapGuards != null)
		{
			for(NpcData npc : _mapGuards)
			{
				if(npc != null)
					npc.deleteMe();
			}
			_mapGuards.clear();
		}
	}
	
	/**
	 * state 0 - called on event's initialization (adds doors to the instance) <br>
	 * state 1 - called on a wait time start (to close doors) <br>
	 * state 2 - called on a wait time end (to open doors)
	 */
	protected void handleDoors(int state)
	{
		if(!_arena.hasDoor())
			return;
		
		DoorAction action;
		if(state == 0) // init (after teleported to the event, in wait time start, etc.
		{
			for(EventSpawn doorSpawn : _arena.getDoors())
			{
				action = DoorAction.getAction(doorSpawn.getNote(), 1);
				CallBack.getInstance().getOut().addDoorToInstance(_instanceId, doorSpawn.getDoorId(), (action == DoorAction.Open));
			}
		}
		else // right before fight starts - on wait round wait time end, etc.
		{
			for(DoorData door : CallBack.getInstance().getOut().getInstanceDoors(_instanceId))
			{
				for(EventSpawn doorSpawn : _arena.getDoors())
				{
					action = DoorAction.getAction(doorSpawn.getNote(), state);
					if(doorSpawn.getDoorId() == door.getDoorId())
					{
						if(action == DoorAction.Close && door.isOpened())
							door.closeMe();
						else if(action == DoorAction.Open && !door.isOpened())
							door.openMe();
					}
				}
			}
		}
	}
	
	@Override
	public void playerWentAfk(PlayerEventInfo player, boolean warningOnly, int afkTime)
	{
		if(warningOnly)
		{
			player.sendMessage(LanguageEngine.getMsg("event_afkWarning", (PlayerEventInfo.AFK_WARNING_DELAY / 1000), (PlayerEventInfo.AFK_KICK_DELAY /1000)));
		}
		else
		{
			if(afkTime == 0)
			{
				player.sendMessage(LanguageEngine.getMsg("event_afkMarked"));
			}
			else if(afkTime % 60 == 0)
			{
				player.sendMessage(LanguageEngine.getMsg("event_afkDurationInfo", afkTime / 60));
			}
		}
	}
	
	@Override
	public void playerReturnedFromAfk(PlayerEventInfo player)
	{
		
	}
	
	protected void scheduleClearEvent(int delay)
	{
		CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				clearEvent();
			}
		}, 8000);
	}
	
	protected void startAnnouncing()
	{
		if(_announcer != null)
			_announcer.announce = true;
	}

	public EventMap getMap()
	{
		return _arena;
	}
	
	public int getGameId()
	{
		return _gameId;
	}

	public MiniEventManager getEvent()
	{
		return _event;
	}
	
	protected void saveGlobalStats()
	{
		Map<PlayerEventInfo, GlobalStatsModel> stats = new FastMap<PlayerEventInfo, GlobalStatsModel>();
		
		for(EventTeam team : getTeams())
		{
			for(PlayerEventInfo pi : team.getPlayers())
			{
				getPlayerData(pi).getGlobalStats().raise(GlobalStatType.COUNT_PLAYED, 1);
				stats.put(pi, getPlayerData(pi).getGlobalStats());
			}
		}
		
		EventStatsManager.getInstance().getGlobalStats().updateGlobalStats(stats);
	}
	
	protected class Announcer implements Runnable
	{
		private long _start;
		boolean announce = false;
		private ScheduledFuture<?> _nextAnnounce;
		
		public void setTime(long startTime)
		{
			_start = startTime;
			run();
		}
		
		@Override
		public void run()
		{
			int delay = (int) Math.round((_start - System.currentTimeMillis()) / 1000.0);
			
			if (announce && delay > 0)
				announce(delay);
			
			int nextMsg = 0;
			if (delay > 3600)
			{
				nextMsg = delay - 3600;
			}
			else if (delay > 1800)
			{
				nextMsg = delay - 1800;
			}
			else if (delay > 900)
			{
				nextMsg = delay - 900;
			}
			else if (delay > 600)
			{
				nextMsg = delay - 600;
			}
			else if (delay > 300)
			{
				nextMsg = delay - 300;
			}
			else if (delay > 60)
			{
				nextMsg = delay - 60;
			}
			else if (delay > 10)
			{
				nextMsg = delay - 10;
			}
			else
				return;
			
			if (delay > 0)
			{
				_nextAnnounce = CallBack.getInstance().getOut().scheduleGeneral(this, nextMsg * 1000);
			}
		}
		
		/** delay in seconds */
		private void announce(int delay)
		{
			if (delay >= 3600 && delay % 3600 == 0)
			{
				int d = delay / 3600;

				broadcastMessage(LanguageEngine.getMsg("game_countdown", d, ("hour" + (d == 1 ? "" : "s"))), false);
			}
			else if (delay >= 60)
			{
				int d = delay / 60;

				broadcastMessage(LanguageEngine.getMsg("game_countdown", d, ("minute" + (d == 1 ? "" : "s"))), false);
			}
			else
			{
				broadcastMessage(LanguageEngine.getMsg("game_countdown", delay, ("second" + (delay == 1 ? "" : "s"))), false);
			}
		}
		
		public void cancel()
		{
			if(_nextAnnounce != null)
				_nextAnnounce.cancel(false);
		}
	}
	
	protected void scheduleMessage(final String message, int delay, final boolean abortable)
	{
		CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				broadcastMessage(message, abortable);
			}
		}, delay);
	}
	
	protected String getRoundName(int round, int maxRounds)
	{
		if(round == maxRounds)
		{
			return LanguageEngine.getMsg("round_final");
		}
		else
		{
			switch(round)
			{
				case 1:
					return LanguageEngine.getMsg("round_1");
				case 2:
					return LanguageEngine.getMsg("round_2");
				case 3:
					return LanguageEngine.getMsg("round_3");
				case 4:
					return LanguageEngine.getMsg("round_4");
				case 5:
					return LanguageEngine.getMsg("round_5");
				case 6:
					return LanguageEngine.getMsg("round_6");
				case 7:
					return LanguageEngine.getMsg("round_7");
				case 8:
					return LanguageEngine.getMsg("round_8");
				case 9:
					return LanguageEngine.getMsg("round_9");
				case 10:
					return LanguageEngine.getMsg("round_10");
			}
		}
		return round + "th"; // should be never called
	}
	
	protected int getGameTime()
	{
		for(AbstractFeature f : _event.getMode().getFeatures())
		{
			if(f.getType() == FeatureType.TimeLimit)
			{
				return ((TimeLimitFeature) f).getTimeLimit();
			}
		}
		
		return _event.getInt("TimeLimitMs");
	}
	
	// inheritable methods
	
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		
	}
	
	@Override
	public boolean canAttack(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null)
			return true;
		else if(target.getEventInfo().getEvent() != player.getEvent())
			return false;
		else if(target.getEventInfo().getTeamId() != player.getTeamId())
			return true;
		
		return false;
	}
	
	@Override
	public boolean onAttack(CharacterData cha, CharacterData target)
	{
		return true;
	}
	
	@Override
	public boolean canSupport(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null || target.getEventInfo().getEvent() != player.getEvent())
			return false;
		else if(target.getEventInfo().getTeamId() == player.getTeamId())
			return true;
		
		return false;
	}
	
	@Override
	public void onDie(PlayerEventInfo player, CharacterData killer)
	{
		
	}
	
	@Override
	public void onDamageGive(CharacterData cha, CharacterData target, int damage, boolean isDOT)
	{
		
	}
	
	@Override
	public void onDisconnect(PlayerEventInfo player)
	{
		
	}
	
	@Override
	public boolean addDisconnectedPlayer(PlayerEventInfo player, DisconnectedPlayerData data)
	{
		return false;
	}
	
	@Override
	public boolean onSay(PlayerEventInfo player, String text, int channel)
	{
		if(text.equals(".scheme"))
		{
			EventManager.getInstance().getHtmlManager().showSelectSchemeForEventWindow(player, "none", getEvent().getEventType().getAltTitle());
			return false;
		}
		
		if(text.equalsIgnoreCase(".voteabort") || text.equalsIgnoreCase(".voteend"))
		{
			voteEnd(player);
			return false;
		}
		
		return true;
	}
	
	private void voteEnd(PlayerEventInfo player)
	{
		if(_voted == null)
			_voted = new FastList<PlayerEventInfo>();
		
		if(!_voted.contains(player))
		{
			_voted.add(player);
			
			broadcastMessage("A player voted to end this mini event.", true);
			
			for(EventTeam t : getTeams())
			{
				for(PlayerEventInfo p : t.getPlayers())
				{
					if(!_voted.contains(p))
						return;
				}
			}
			
			abortDueToError("Players voted to abort this match.");
		}
	}
	
	@Override
	public boolean onNpcAction(PlayerEventInfo player, NpcData npc)
	{
		return true;
	}
	
	@Override
	public boolean canUseItem(PlayerEventInfo player, ItemData item)
	{
		if (notAllovedItems != null && Arrays.binarySearch(notAllovedItems, item.getItemId()) >= 0)
		{
			player.sendMessage(LanguageEngine.getMsg("event_itemNotAllowed"));
			return false;
		}
		
		if (item.isScroll())
			return false;
		
		if (item.isPotion() && !_event.getBoolean("allowPotions"))
			return false;
		
		for(AbstractFeature f : getEvent().getMode().getFeatures())
		{
			if(f.getType() == FeatureType.ItemGrades)
				if(!((ItemGradesFeature) f).checkItem(player, item))
					return false;
			
			if(f.getType() == FeatureType.Items)
				if(!((ItemsFeature) f).checkItem(player, item))
					return false;
			
			if(f.getType() == FeatureType.Enchant)
				if(!((EnchantFeature) f).checkItem(player, item))
					return false;
		}
		
		if(item.isPetCollar() && !_allowPets)
		{
			player.sendMessage(LanguageEngine.getMsg("event_petsNotAllowed"));
			return false;
		}
		
		if(item.isArmor() || item.isJewellery())
		{
			player.sendMessage("Due to the Gear rating system, you cannot change your equippped items in mini events.");
			return false;
		}
		
		return true;
	}
	
	@Override
	public void onItemUse(PlayerEventInfo player, ItemData item)
	{
		
	}
	
	@Override
	public boolean canUseSkill(PlayerEventInfo player, SkillData skill)
	{
		if (notAllovedSkillls != null && Arrays.binarySearch(notAllovedSkillls, skill.getId()) >= 0)
		{
			player.sendMessage(LanguageEngine.getMsg("event_skillNotAllowed"));
			return false;
		}

		if (skill.getSkillType().equals("RESURRECT"))
			return false;
		
		if (skill.getSkillType().equals("RECALL"))
			return false;
		
		if (skill.getSkillType().equals("SUMMON_FRIEND"))
			return false;

		if (skill.getSkillType().equals("FAKE_DEATH"))
			return false;

		for(AbstractFeature f : getEvent().getMode().getFeatures())
		{
			if(f.getType() == FeatureType.Skills)
				if(!((SkillsFeature) f).checkSkill(player, skill))
					return false;
		}
		
		if(!_allowSummons && skill.getSkillType().equals("SUMMON"))
		{
			player.sendMessage(LanguageEngine.getMsg("event_summonsNotAllowed"));
			return false;
		}
		
		return true;
	}
	
	@Override
	public void onSkillUse(PlayerEventInfo player, SkillData skill)
	{
		
	}
	
	@Override
	public boolean canDestroyItem(PlayerEventInfo player, ItemData item)
	{
		return true;
	}
	
	@Override
	public boolean canInviteToParty(PlayerEventInfo player, PlayerEventInfo target)
	{
		if(target.getEvent() != player.getEvent())
			return false;
		else if(target.getTeamId() == player.getTeamId())
			return true;
		
		return false;
	}
	
	@Override
	public boolean canTransform(PlayerEventInfo player)
	{
		return true;
	}
	
	@Override
	public boolean canBeDisarmed(PlayerEventInfo player)
	{
		return true;
	}
	
	@Override
	public int allowTransformationSkill(PlayerEventInfo player, SkillData skill)
	{
		return 0;
	}
	
	@Override
	public boolean canSaveShortcuts(PlayerEventInfo player)
	{
		return true;
	}

	private RegistrationData _winner = null;

	public void setWinner(EventTeam team)
	{
		if(_winner == null)
			_winner = team.getRegData();
	}

	public RegistrationData getWinner()
	{
		if(_winner == null)
			throw new NullPointerException("Winner cannot be null!");

		return _winner;
	}

	public RegistrationData[] getRegisteredTeams()
	{
		return _registeredTeams;
	}
}
