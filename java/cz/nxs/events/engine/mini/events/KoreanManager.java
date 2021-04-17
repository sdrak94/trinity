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
package cz.nxs.events.engine.mini.events;

import cz.nxs.events.engine.EventMapSystem;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.base.description.EventDescription;
import cz.nxs.events.engine.base.description.EventDescriptionSystem;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.events.engine.mini.RegistrationData;
import cz.nxs.events.engine.mini.features.AbstractFeature;
import cz.nxs.events.engine.mini.features.DelaysFeature;
import cz.nxs.events.engine.mini.features.TeamSizeFeature;
import cz.nxs.events.engine.mini.features.TimeLimitFeature;
import javolution.text.TextBuilder;

/**
 * @author hNoke
 *
 */
@SuppressWarnings("unused")
public class KoreanManager extends PartyvsPartyManager
{
	private final int MAX_GAMES_COUNT = 3;
	
	private static RewardPosition[] _rewardTypes = { RewardPosition.Winner, RewardPosition.Looser, RewardPosition.Tie_TimeLimit };
	
	public KoreanManager(EventType type)
	{
		super(type);
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		// dont use configs from pt vs pt event
		removeMapConfigs();
		addMapConfig(new ConfigModel("WaitTime", "60000", "The waiting delay during which players will be able to rebuff, organize, etc. In ms."));
		
		// only 1 round and 2 teams
		removeConfig("RoundsAmmount");
		removeConfig("TeamsAmmount");
		removeConfig("removeBuffsOnRespawn");
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
		
		KoreanGame game = new KoreanGame(_lastGameId, map, this, teams);
		
		new Thread(game, getEventName() + " ID" + _lastGameId).start(); // execute new thread to handle this game
		
		_games.add(game);
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
	public RewardPosition[] getRewardTypes()
	{
		return _rewardTypes;
	}
	
	@Override
	public int getMaxGamesCount()
	{
		return MAX_GAMES_COUNT;
	}
	
	@Override
	public int getTeamsCount()
	{
		return 2;
	}
	
	@Override
	public String getHtmlDescription()
	{
		if(_htmlDescription == null)
		{
			int partySize = getInt("PartySize");
			int rejoinDelay = getInt("DelayToWaitSinceLastMatchMs");
			int timeLimit = getInt("TimeLimitMs");
			
			for(AbstractFeature feature : getMode().getFeatures())
			{
				if(feature instanceof DelaysFeature)
					rejoinDelay = ((DelaysFeature)feature).getRejoinDealy();
				else if(feature instanceof TimeLimitFeature)
					timeLimit = ((TimeLimitFeature)feature).getTimeLimit();
				else if(feature instanceof TeamSizeFeature)
					partySize = ((TeamSizeFeature)feature).getTeamSize();
			}
			
			EventDescription desc = EventDescriptionSystem.getInstance().getDescription(getEventType());
			if(desc != null)
			{
				_htmlDescription = desc.getDescription(getConfigs(), 1, 2, partySize, rejoinDelay, timeLimit);
			}
			else
			{
				_htmlDescription = "This is a team-based mini event. You need a party of exactly " + partySize + " players (and be the party leader) to register. ";
				_htmlDescription += "You will fight against one enemy party in a randomly chosen map. ";
				
				_htmlDescription += "The fight is in the famous Korean-style - it's a set of continous 1v1 fights. If you die, you will be replaced by someone from your party. ";
				_htmlDescription += "The match ends when all players from one party are dead. ";
				
				_htmlDescription += "Your opponent will be selected automatically and don't worry, there's a protection, which will ensure that you will always fight only players whose level is similar to yours. ";
				
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
			
			if(!map.checkForSpawns(SpawnType.Safe, (i+1), 1))
				tb.append(addMissingSpawn(SpawnType.Safe, i+1, 1));
		}
		
		return tb.toString();
	}
}
