package cz.nxs.events.engine.mini.events;

import java.util.List;
import java.util.Map;

import cz.nxs.events.engine.EventMapSystem;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.base.description.EventDescription;
import cz.nxs.events.engine.base.description.EventDescriptionSystem;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.events.engine.mini.MiniEventManager;
import cz.nxs.events.engine.mini.RegistrationData;
import cz.nxs.events.engine.mini.features.AbstractFeature;
import cz.nxs.events.engine.mini.features.DelaysFeature;
import cz.nxs.events.engine.mini.features.RoundsFeature;
import cz.nxs.events.engine.mini.features.TeamsAmmountFeature;
import cz.nxs.events.engine.mini.features.TimeLimitFeature;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class OnevsOneManager extends MiniEventManager
{
	private final int MAX_GAMES_COUNT = 10;
	protected int _lastMapIndex;
	
	private FastList<RegistrationData> tempPlayers = new FastList<RegistrationData>();
	
	protected static RewardPosition[] _rewardTypes = { RewardPosition.Winner, RewardPosition.Looser, RewardPosition.Tie_TimeLimit, RewardPosition.Tie };
	protected Map<SpawnType, String> _spawnTypes;
	
	public OnevsOneManager(EventType type)
	{
		super(type);
		
		_lastMapIndex = 0;
		
		_spawnTypes = new FastMap<SpawnType, String>();
		_spawnTypes.put(SpawnType.Regular, "Defines where the players will be spawned.");
		_spawnTypes.put(SpawnType.Buffer, "Defines where the buffer(s) will be spawned.");
		_spawnTypes.put(SpawnType.Fence, "Defines where fences will be spawned.");
		
		// start checking for players
		check();
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		addConfig(new ConfigModel("TeamsAmmount", "2", "The count of fighting players in the event."));
		addConfig(new ConfigModel("RoundsAmmount", "3", "The count of rounds the event has."));
		
		addMapConfig(new ConfigModel("FirstRoundWaitDelay", "30000", "The delay the player has to wait when he is teleported to the map (first round). During this time, he will be preparing himself for the fight and getting buffs. In miliseconds."));
		addMapConfig(new ConfigModel("RoundWaitDelay", "20000", "The waiting delay for player to prepare for the match before all rounds' (except for the first round) start. In miliseconds."));
		addMapConfig(new ConfigModel("RootPlayers", "true", "Put 'true' if you want event to root all players on all rounds' start during the wait time (putting false is good for maps such as Coliseum, with closeable doors where the players can't leave the first place and attack directly the opponent before the fight itself starts, etc.)", InputType.Boolean));
	}
	
	@Override
	public void run()
	{
		check();
	}
	
	@Override
	public void createGame()
	{
		if(_locked) return;
		
		removeInactiveTeams();
		
		if(_games.size() >= getMaxGamesCount())
		{
			check();
			return;
		}
		
		int limit = 3;
		int iterateLimit = _parties.size();
		List<RegistrationData> tempData = new FastList<RegistrationData>();
		
		setIsTemporaryLocked(true);
		
		try
		{
			while(true)
			{
				if(limit == 0 || iterateLimit == 0)
					break;
				
				tempPlayers.clear();
				
				for(RegistrationData player : _parties)
				{
					if(player.isChosen())
						continue;
					
					if(tempPlayers.isEmpty())
					{
						if(tempData.contains(player))
							continue;
						
						tempPlayers.add(player);
						tempData.add(player);
					}
					else if(!tempPlayers.contains(player))
					{
						if(strenghtChecks(player, tempPlayers.getFirst()) && ipChecks(player, tempPlayers.getFirst()))
							tempPlayers.add(player);
						else
							continue;
					}
					
					// enought players, match can be started
					if(tempPlayers.size() >= getTeamsCount())
					{
						for(RegistrationData d : tempPlayers)
							d.setIsChosen(true);

						launchGame(tempPlayers.toArray(new RegistrationData[tempPlayers.size()]), null);
						limit -= 1;
						break;
					}
				}
				iterateLimit -= 1;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		//delete players from the queue
		for(RegistrationData p : _parties)
		{
			if(p.isChosen())
				_parties.remove(p);
		}
		
		setIsTemporaryLocked(false);
		
		check();
	}

	@Override
	public boolean launchGame(RegistrationData[] teams, EventMap map)
	{
		if(map == null)
			map = EventMapSystem.getInstance().getNextMap(this, _lastMapIndex, getMode());
			
		if(map == null)
		{
			cleanMe(true);
			_mode.setAllowed(false);
			
			_log.warning("No map aviable for event " + getEventType().getAltTitle() + " !!! Mode has been disabled.");
			return false;
		}
		
		_lastMapIndex = EventMapSystem.getInstance().getMapIndex(getEventType(), map);
		getNextGameId();
		
		OnevsOneGame game = new OnevsOneGame(_lastGameId, map, this, teams);
		new Thread(game, getEventName() + " ID" + _lastGameId).start(); // execute new thread to handle this game
		
		_games.add(game);
		return true;
	}

	@Override
	public boolean registerTeam(PlayerEventInfo player)
	{
		if(!super.registerTeam(player))
		{
			return false;
		}
		else
		{
			RegistrationData data = createRegistrationData(player);
			data.register(true, this);
			addParty(data);
			
			data.message(LanguageEngine.getMsg("registering_registered2", getEventName()), true);
			
			return true;
		}
	}

	@Override
	public RegistrationData createRegistrationData(PlayerEventInfo player)
	{
		FastList<PlayerEventInfo> playersCollection = new FastList<PlayerEventInfo>();
		playersCollection.add(CallBack.getInstance().getPlayerBase().addInfo(player));

		return new RegistrationData(playersCollection);
	}

	@Override
	public synchronized boolean unregisterTeam(PlayerEventInfo player)
	{
		if(!super.unregisterTeam(player))
		{
			return false;
		}
		else
		{
			for(RegistrationData data : _parties)
			{
				if(data.getKeyPlayer().getPlayersId() == player.getPlayersId())
				{
					deleteTeam(data);
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public void notifyDisconnect(PlayerEventInfo player)
	{
		for(RegistrationData data : _parties)
		{
			if(data.getKeyPlayer().getPlayersId() == player.getPlayersId())
			{
				deleteTeam(data);
				break;
			}
		}
	}
	
	public List<PlayerEventInfo> getTeams()
	{
		List<PlayerEventInfo> list = new FastList<PlayerEventInfo>();
		for(RegistrationData player : _parties)
		{
			list.add(player.getKeyPlayer());
		}
		return list;
	}
	
	@Override
	public boolean checkCanFight(PlayerEventInfo gm, RegistrationData[] team)
	{
		if(team.length != 2)
		{
			gm.sendMessage("2 teams are required.");
			return false;
		}
		
		if(team[0].getKeyPlayer() != null && team[1].getKeyPlayer() != null)
			return true;
		
		gm.sendMessage("No player aviable for one of the teams.");
		return false;
	}

	@Override
	protected int getStartGameInterval()
	{
		return 30000;
	}
	
	@Override
	public int getDefaultPartySizeToJoin()
	{
		return 1;
	}
	
	@Override
	public boolean requireParty()
	{
		return false;
	}
	
	@Override
	public int getMaxGamesCount()
	{
		return MAX_GAMES_COUNT;
	}
	
	@Override
	public RewardPosition[] getRewardTypes()
	{
		return _rewardTypes;
	}
	
	@Override
	public Map<SpawnType, String> getAviableSpawnTypes()
	{
		return _spawnTypes;
	}
	
	@Override
	public int getTeamsCount()
	{
		for(AbstractFeature feature : getMode().getFeatures())
		{
			if(feature.getType() == FeatureType.TeamsAmmount)
			{
				if(((TeamsAmmountFeature) feature).getTeamsAmmount() > 0)
					return ((TeamsAmmountFeature) feature).getTeamsAmmount();
			}
		}
		
		return getInt("TeamsAmmount");
	}
	
	public int getRoundsAmmount()
	{
		for(AbstractFeature feature : getMode().getFeatures())
		{
			if(feature.getType() == FeatureType.Rounds)
			{
				if(((RoundsFeature) feature).getRoundsAmmount() > 0)
					return ((RoundsFeature) feature).getRoundsAmmount();
			}
		}
		
		return getInt("RoundsAmmount");
	}

	@Override
	public String getHtmlDescription()
	{
		if(_htmlDescription == null)
		{
			int roundsCount = getInt("RoundsAmmount");
			int teamsCount = getInt("TeamsAmmount");
			int rejoinDelay = getInt("DelayToWaitSinceLastMatchMs");
			int timeLimit = getInt("TimeLimitMs");
			//int maxLeveLDiff = getInt("MaxLevelDifference");
			
			for(AbstractFeature feature : getMode().getFeatures())
			{
				if(feature instanceof RoundsFeature)
					roundsCount = ((RoundsFeature)feature).getRoundsAmmount();
				else if(feature instanceof TeamsAmmountFeature)
					teamsCount = ((TeamsAmmountFeature)feature).getTeamsAmmount();
				else if(feature instanceof DelaysFeature)
					rejoinDelay = ((DelaysFeature)feature).getRejoinDealy();
				else if(feature instanceof TimeLimitFeature)
					timeLimit = ((TimeLimitFeature)feature).getTimeLimit();
				//else if(feature instanceof StrenghtChecksFeature)
				//	maxLeveLDiff = ((StrenghtChecksFeature)feature).getMaxLevelDiff();
			}
			
			EventDescription desc = EventDescriptionSystem.getInstance().getDescription(getEventType());
			if(desc != null)
			{
				_htmlDescription = desc.getDescription(getConfigs(), roundsCount, teamsCount, 1, rejoinDelay, timeLimit);
			}
			else
			{
				_htmlDescription = "This is a free-for-all mini event. ";
				_htmlDescription += "You will fight against " + (teamsCount-1) + " enemy player" + (teamsCount > 2 ? "s" : "") + " in a randomly chosen map. ";
				
				if(roundsCount > 1)
				{
					_htmlDescription += "Each match has " + roundsCount + " rounds, the winner of round (the player, who kills all his opponents) receives  1 score. ";
					_htmlDescription += "The player, who has the biggest score in the end of all rounds, wins the match. ";
				}
				else
				{
					_htmlDescription += "This event has only one round. If you die, the event ends for you. ";
					_htmlDescription += "The winner of the match is the player, who kills all his opponents. ";
				}
				
				_htmlDescription += "Your opponents will be selected automatically and don't worry, there's a protection, which will ensure that you will always fight only players whose level is similar to yours. ";
				
				_htmlDescription += "If the match doesn't end within " + timeLimit/60000 + " minutes, it will be aborted automatically. ";
				
				_htmlDescription += "Also, after you visit this event, you will have to wait at least " + rejoinDelay/60000 + " minutes to join this event again. ";
			}
		}
		return _htmlDescription;
	}

	@Override
	public String getMissingSpawns(EventMap map)
	{
		TextBuilder tb = new TextBuilder();
		
		for(int i = 0; i < getTeamsCount(); i++)
		{
			if(!map.checkForSpawns(SpawnType.Regular, (i+1), 1))
				tb.append(addMissingSpawn(SpawnType.Regular, i+1, 1));
		}
		
		return tb.toString();
	}
	
	@Override
	protected String addMissingSpawn(SpawnType type, int team, int count)
	{
		return "<font color=bfbfbf>" + getMode().getModeName() + " </font><font color=696969>mode</font> -> <font color=9f9f9f>No</font> <font color=B46F6B>" + type.toString().toUpperCase() + "</font> <font color=9f9f9f>spawn for player " + team + " count " + count + " (or more)</font><br1>";
	}
}