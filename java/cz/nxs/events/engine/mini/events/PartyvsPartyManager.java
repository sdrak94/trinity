package cz.nxs.events.engine.mini.events;

import java.util.List;
import java.util.Map;

import cz.nxs.events.engine.EventMapSystem;
import cz.nxs.events.engine.base.ConfigModel;
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
import cz.nxs.events.engine.mini.features.TeamSizeFeature;
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
public class PartyvsPartyManager extends MiniEventManager
{
	protected int _lastMapIndex;
	
	private FastList<RegistrationData> tempTeams = new FastList<RegistrationData>();

	private final int MAX_GAMES_COUNT = 3;
	
	private Map<SpawnType, String> _spawnTypes;
	
	private static RewardPosition[] _rewardTypes = { RewardPosition.Winner, RewardPosition.Looser, RewardPosition.Tie_TimeLimit, RewardPosition.Tie };
	
	public PartyvsPartyManager(EventType type)
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
		
		addConfig(new ConfigModel("PartySize", "6", "The exact size of registered party. If the party has lower or higher # of players, it won't be able to join."));
		addConfig(new ConfigModel("TeamsAmmount", "2", "The count of teams (parties) fighting in the event."));
		addConfig(new ConfigModel("RoundsAmmount", "3", "The count of rounds the event has."));
		
		addMapConfig(new ConfigModel("FirstRoundWaitDelay", "30000", "The delay the players has to wait when he is teleported to the map (first round). During this time, he will be preparing himself for the fight and getting buffs. In miliseconds."));
		addMapConfig(new ConfigModel("RoundWaitDelay", "20000", "The waiting delay for players to prepare for the match before all rounds' (except for the first round) start. In miliseconds."));
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
		
		int iterateLimit = _parties.size();
		int limit = 1;
		List<RegistrationData> tempData = new FastList<RegistrationData>();
		
		setIsTemporaryLocked(true);
		
		try
		{
			while(true)
			{
				if(limit == 0 || iterateLimit == 0)
					break;
				
				tempTeams.clear();
				
				for(RegistrationData team : _parties)
				{
					if(team.isChosen())
						continue;
					
					if(tempTeams.isEmpty())
					{
						if(tempData.contains(team))
							continue;
						
						tempTeams.add(team);
						tempData.add(team);
					}
					else if(!tempTeams.contains(team))
					{
						if(strenghtChecks(team, tempTeams.getFirst()) && ipChecks(team, tempTeams.getFirst()))
							tempTeams.add(team);
						else
							continue;
					}
					
					// enought players, match can be started
					if(tempTeams.size() >= getTeamsCount())
					{
						for(RegistrationData d : tempTeams)
							d.setIsChosen(true);

						launchGame(tempTeams.toArray(new RegistrationData[tempTeams.size()]), null);
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
			
			_log.warning("No map available for event " + getEventType().getAltTitle() + " !!! Mode has been disabled.");
			return false;
		}
		
		_lastMapIndex = EventMapSystem.getInstance().getMapIndex(getEventType(), map);
		getNextGameId();
		
		PartyvsPartyGame game = new PartyvsPartyGame(_lastGameId, map, this, teams);
		
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
			RegistrationData regData = createRegistrationData(player);
			regData.register(true, this);
			regData.message(LanguageEngine.getMsg("registering_registered2", getEventName()), true);
			
			addParty(regData);

			return true;
		}
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
			for(RegistrationData t : _parties)
			{
				if(t.getKeyPlayer().getPlayersId() == player.getPlayersId())
				{
					deleteTeam(t);
					return true;
				}
			}
			
			return false;
		}
	}
	
	@Override
	public boolean checkCanFight(PlayerEventInfo gm, RegistrationData[] teams)
	{
		if(teams.length != 2)
		{
			gm.sendMessage("2 teams are required.");
			return false;
		}
		
		if(teams[0].getPlayers().size() < getDefaultPartySizeToJoin() / 2 || teams[1].getPlayers().size() < getDefaultPartySizeToJoin() / 2)
		{
			gm.sendMessage("Not enought players in one of the teams, minimal # of players registered is " + getDefaultPartySizeToJoin() / 2 + ".");
			return false;
		}
		return true;
	}

	@Override
	protected int getStartGameInterval()
	{
		return 30000;
	}
	
	@Override
	public int getDefaultPartySizeToJoin()
	{
		for(AbstractFeature feature : getMode().getFeatures())
		{
			if(feature.getType() == FeatureType.TeamSize)
			{
				if(((TeamSizeFeature) feature).getTeamSize() > 0)
					return ((TeamSizeFeature) feature).getTeamSize();
			}
		}
		
		return getInt("PartySize");
	}
	
	@Override
	public boolean requireParty()
	{
		return true;
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
			int partySize = getInt("PartySize");
			int rejoinDelay = getInt("DelayToWaitSinceLastMatchMs");
			int timeLimit = getInt("TimeLimitMs");
			
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
				else if(feature instanceof TeamSizeFeature)
					partySize = ((TeamSizeFeature)feature).getTeamSize();
			}
			
			EventDescription desc = EventDescriptionSystem.getInstance().getDescription(getEventType());
			if(desc != null)
			{
				_htmlDescription = desc.getDescription(getConfigs(), roundsCount, teamsCount, partySize, rejoinDelay, timeLimit);
			}
			else
			{
				_htmlDescription = "This is a team-based mini event. You need a party of exactly " + partySize + " players (and be the party leader) to register. ";
				_htmlDescription += "You will fight against " + (teamsCount-1) + " enemy part" + (teamsCount > 2 ? "ies" : "y") + " in a randomly chosen map. ";
				
				if(roundsCount > 1)
				{
					_htmlDescription += "Each match has " + roundsCount + " rounds, the winner of round (the party, who kills all it's opponents) receives  1 score. ";
					_htmlDescription += "The party, who has the biggest score in the end of all rounds, wins the match. ";
				}
				else
				{
					_htmlDescription += "This match has only one round. If you die, you can get revived only by your party-mate. ";
					_htmlDescription += "The winner of the match is the party, who kills all it's opponents. ";
				}
				
				_htmlDescription += "Your opponent(s) will be selected automatically and don't worry, there's a protection, which will ensure that you will always fight only players whose level is similar to yours. ";
				
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
	protected boolean allowHealers()
	{
		if(getMode() != null && getMode().getModeId() == 2)
			return false;
		
		return getBoolean("allowHealers");
	}

	@Override
	public RegistrationData createRegistrationData(PlayerEventInfo player)
	{
		FastList<PlayerEventInfo> partyPlayers = new FastList<PlayerEventInfo>();

		for(PlayerEventInfo p : player.getParty().getPartyMembers())
		{
			if(p != null)
			{
				CallBack.getInstance().getPlayerBase().addInfo(p);
				partyPlayers.add(p);
			}
		}

		return new RegistrationData(partyPlayers);
	}
}
