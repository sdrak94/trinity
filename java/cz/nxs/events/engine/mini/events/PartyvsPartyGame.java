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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

import cz.nxs.events.engine.EventBuffer;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventRewardSystem;
import cz.nxs.events.engine.EventWarnings;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventPlayerData;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.PvPEventPlayerData;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.mini.MiniEventGame;
import cz.nxs.events.engine.mini.RegistrationData;
import cz.nxs.events.engine.stats.GlobalStats.GlobalStatType;
import cz.nxs.events.engine.stats.GlobalStatsModel;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.events.engine.team.FixedPartyTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class PartyvsPartyGame extends MiniEventGame
{
	private final int _teamsAmmount;
	private final int _roundsAmmount;
	
	private FixedPartyTeam[] _teams;
	private int _round;
	
	private ScheduledFuture<?> _eventEnd;
	private ScheduledFuture<?> _roundStart;
	
	public PartyvsPartyGame(int gameId, EventMap arena, PartyvsPartyManager event, RegistrationData[] teams)
	{
		super(gameId, arena, event, teams);
		
		_teamsAmmount = event.getTeamsCount();
		_roundsAmmount = event.getRoundsAmmount();
		
		_teams = new FixedPartyTeam[_teamsAmmount];

		for(int i = 0; i < _teamsAmmount; i++)
		{
			_teams[i] = new FixedPartyTeam(teams[i], i + 1, teams[i].getKeyPlayer().getPlayersName() + "'s party", event.getDefaultPartySizeToJoin());
			
			for(PlayerEventInfo pi : teams[i].getPlayers())
			{
				pi.onEventStart(this);
				_teams[i].addPlayer(pi, true);
			}
		}
		
		CallbackManager.getInstance().eventStarts(1, getEvent().getEventType(), Arrays.asList(_teams));
		
		_round = 0;
	}

	@Override
	protected void initEvent()
	{
		super.initEvent();
		
		loadBuffers();
		startEvent();
	}

	@Override
	protected void startEvent()
	{
		try
		{
			broadcastMessage(LanguageEngine.getMsg("game_teleporting"), true);

			_eventEnd = CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					endByTime();
				}
			}, getGameTime());
			
			scheduleMessage(LanguageEngine.getMsg("game_teleportDone"), 1500, true);
			
			nextRound(null, false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void nextRound(FixedPartyTeam lastWinner, boolean forceEnd)
	{
		if(_aborted)
			return;
		
		if(_round == _roundsAmmount || forceEnd)
		{
			endByDie();
			return;
		}
		else
		{
			_round ++;
			
			handleDoors(1); // Add this to main events
			loadBuffers();
			
			EventSpawn spawn;
			for(FixedPartyTeam team : _teams)
			{
				spawn = _arena.getNextSpawn(team.getTeamId(), SpawnType.Regular);
				
				if(spawn == null)
				{
					abortDueToError("No regular spawn found for team " + team.getTeamId() + ". Match aborted.");
					clearEvent();
					return;
				}
				
				for(PlayerEventInfo pi : team.getPlayers())
				{
					if(!pi.isOnline()) 
						continue;
					
					pi.teleport(spawn.getLoc(), 0, true, _instanceId);
					
					pi.disableAfkCheck(true);
					pi.root();
					pi.setIsInvul(true);
					
					if(_round == 1 && getEvent().getBoolean("removeCubics"))
						pi.removeCubics();
					
					if(_allowSchemeBuffer)
						EventBuffer.getInstance().buffPlayer(pi, true);
					
					if(_round == 1)
						pi.enableAllSkills();
				}
			}
			
			final int startTime;
			
			if(_round == 1)
				startTime = getEvent().getMapConfigInt(_arena, "FirstRoundWaitDelay");
			else
				startTime = getEvent().getMapConfigInt(_arena, "RoundWaitDelay");

			scheduleMessage(LanguageEngine.getMsg("game_roundStartIn", getRoundName(_round, _roundsAmmount), startTime / 1000), 5000, true);
			
			_roundStart = CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					finishRoundStart();
				}
			}, startTime);
		}
	}
	
	private void finishRoundStart()
	{
		if(_aborted)
			return;
		
		unspawnBuffers();
		
		handleDoors(2);
		
		for(FixedPartyTeam team : _teams)
		{
			for(PlayerEventInfo pi : team.getPlayers())
			{
				if(pi.isOnline())
				{
					pi.disableAfkCheck(false);
					pi.unroot();
					pi.setIsInvul(false);
				}
			}
		}
				
		broadcastMessage(LanguageEngine.getMsg("game_roundStarted", getRoundName(_round, _roundsAmmount)), true);
		
		if(_round == 1)
			startAnnouncing();
	}
	
	@Override
	public void onDie(PlayerEventInfo player, CharacterData killer)
	{
		if(_aborted)
			return;
		
		updateScore(player, killer);
		
		final FixedPartyTeam team = checkLastAliveTeam();
		if(team != null)
		{
			team.raiseScore(1);
			onScore(team.getPlayers(), 1);
			
			final boolean forceEnd = !checkIfTheMatchCanContinue();
			
			if(_round == _roundsAmmount || forceEnd)
				scheduleMessage(LanguageEngine.getMsg("game_matchEnd"), 3000, true);
			else 
			{
				scheduleMessage(LanguageEngine.getMsg("game_roundWonBy", getRoundName(_round, _roundsAmmount), team.getTeamName()), 3000, true);
			}
			
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					nextRound(team, forceEnd);
				}
			}, 4000);
		}
	}
	
	private boolean checkIfTheMatchCanContinue()
	{
		// TODO doesn't work correctly in some cases when there are more than 2 teams
		int remainingRounds = _roundsAmmount - _round;
		int bestScore = 0;
		int secondScore = 0;
		
		for(FixedPartyTeam team : _teams)
		{
			if(team.getScore() > bestScore)
			{
				secondScore = bestScore;
				bestScore = team.getScore();
			}
			else if(team.getScore() > secondScore && secondScore != bestScore)
				secondScore = team.getScore();
		}
		
		// second team has no chance to win the match anymore
		if(bestScore - secondScore > remainingRounds)
			return false;
		else // there are still enought rounds so the second team can still win the match
			return true;
	}
	
	private FixedPartyTeam checkLastAliveTeam()
	{
		int aliveTeams = 0;
		FixedPartyTeam tempTeam = null;
		
		for(FixedPartyTeam team : _teams)
		{
			for(PlayerEventInfo pi : team.getPlayers())
			{
				if(pi.isOnline())
				{
					// there is at least one alive player
					if(!pi.isDead())
					{
						aliveTeams++;
						tempTeam = team;
						break;
					}
				}
			}
		}
		
		if(aliveTeams == 1)
			return tempTeam;
		else
			return null;
	}
	
	private void endByTime()
	{
		if(_aborted)
			return;
		
		cancelSchedulers();
		
		broadcastMessage(LanguageEngine.getMsg("game_matchEnd_timeLimit", getGameTime() / 60000), false);
		scheduleMessage(LanguageEngine.getMsg("game_matchEnd_tie"), 3000, false);

		int topScore = 0;
		FixedPartyTeam top = null;

		for(FixedPartyTeam team : _teams)
		{
			if(team.getScore() > topScore)
			{
				topScore = team.getScore();
				top = team;
			}

			for(PlayerEventInfo pi : team.getPlayers())
			{
				if(pi.isOnline())
				{
					EventRewardSystem.getInstance().rewardPlayer(getEvent().getEventType(), getEvent().getMode().getModeId(), pi, RewardPosition.Tie_TimeLimit, null, pi.getTotalTimeAfk(), 0, 0);
					
					//showScore(pi, 2);
				}
				
				_event.logPlayer(pi, 2);
			}
		}

		if(top != null)
			setWinner(top);
		
		saveGlobalStats();
		
		scheduleClearEvent(8000);
	}

	private void endByDie()
	{
		cancelSchedulers();
		
		List<FixedPartyTeam> sortedTeams = new FastList<FixedPartyTeam>();
		for(FixedPartyTeam team : _teams)
		{
			sortedTeams.add(team);
		}
		
		Collections.sort(sortedTeams, EventManager.getInstance().compareTeamScore);
		
		// score, teams
		Map<Integer, FastList<FixedPartyTeam>> scores = new FastMap<Integer, FastList<FixedPartyTeam>>();
		
		for(FixedPartyTeam team : sortedTeams)
		{
			if(!scores.containsKey(team.getScore()))
				scores.put(team.getScore(), new FastList<FixedPartyTeam>());
			
			scores.get(team.getScore()).add(team);
		}

		int place = 1;
		for(FixedPartyTeam team : sortedTeams)
		{
			broadcastMessage(LanguageEngine.getMsg("event_announceScore_includeKills", place, team.getTeamName(), team.getScore(), team.getKills()), false);
			place++;
		}
		
		place = 1;
		for(Entry<Integer, FastList<FixedPartyTeam>> i : scores.entrySet())
		{
			// winners
			if(place == 1)
			{
				if(i.getValue().size() > 1)// at least two teams are winners (have the same score) and the match has more than 2 teams
				{
					if(_teamsAmmount > i.getValue().size())
					{
						TextBuilder tb = new TextBuilder();
						
						for(FixedPartyTeam team : i.getValue())
						{
							tb.append(LanguageEngine.getMsg("event_team_announceWinner2_part1", team.getTeamName()));
						}
						
						String s = tb.toString();
						tb = new TextBuilder(s.substring(0, s.length() - 4));
						tb.append(LanguageEngine.getMsg("event_team_announceWinner2_part2"));
						
						broadcastMessage(tb.toString(), false);
						
						for(FixedPartyTeam team : i.getValue())
						{
							setWinner(team);

							for(PlayerEventInfo pi : team.getPlayers())
							{
								if(pi.isOnline())
								{
									EventRewardSystem.getInstance().rewardPlayer(getEvent().getEventType(), getEvent().getMode().getModeId(), pi, RewardPosition.Winner, null, pi.getTotalTimeAfk(), 0, 0);
									setEndStatus(pi, 1);
									//showScore(pi, 1);
								}
								
								getPlayerData(pi).getGlobalStats().raise(GlobalStatType.WINS, 1);
								_event.logPlayer(pi, 1);
							}
						}
					}
					else // all teams are 'winners' - have the same score (but > 0)
					{
						broadcastMessage(LanguageEngine.getMsg("event_team_announceWinner3"), false);
						
						for(FixedPartyTeam team : i.getValue())
						{
							setWinner(team);

							for(PlayerEventInfo pi : team.getPlayers())
							{
								if(pi.isOnline())
								{
									EventRewardSystem.getInstance().rewardPlayer(getEvent().getEventType(), getEvent().getMode().getModeId(), pi, RewardPosition.Tie, null, pi.getTotalTimeAfk(), 0, 0);
									//setEndStatus(pi, 1);
									//showScore(pi, 2);
								}
								
								getPlayerData(pi).getGlobalStats().raise(GlobalStatType.WINS, 1);
								_event.logPlayer(pi, 2);
							}
						}
					}
				}
				else // single team is winner
				{
					broadcastMessage(LanguageEngine.getMsg("event_team_announceWinner1", i.getValue().getFirst().getTeamName()), false);

					setWinner(i.getValue().getFirst());
					
					for(PlayerEventInfo pi : i.getValue().getFirst().getPlayers())
					{
						if(pi.isOnline())
						{
							EventRewardSystem.getInstance().rewardPlayer(getEvent().getEventType(), getEvent().getMode().getModeId(), pi, RewardPosition.Winner, null, pi.getTotalTimeAfk(), 0, 0);
							setEndStatus(pi, 1);
							//showScore(pi, 1);
						}
						
						getPlayerData(pi).getGlobalStats().raise(GlobalStatType.WINS, 1);
						_event.logPlayer(pi, 1);
					}
				}
			}
			else // loosers
			{
				for(FixedPartyTeam team : i.getValue())
				{
					for(PlayerEventInfo pi : team.getPlayers())
					{
						if(pi.isOnline())
						{
							EventRewardSystem.getInstance().rewardPlayer(getEvent().getEventType(), getEvent().getMode().getModeId(), pi, RewardPosition.Looser, null, pi.getTotalTimeAfk(), 0, 0);
							setEndStatus(pi, 0);
							//showScore(pi, 2);
						}
						
						getPlayerData(pi).getGlobalStats().raise(GlobalStatType.LOSES, 1);
						_event.logPlayer(pi, 2);
					}
				}
			}
			
			place++;
		}
		
		saveGlobalStats();

		scheduleClearEvent(5000);
	}

	@Override
	public void clearEvent()
	{
		cancelSchedulers();
		
		cleanSpectators();
		
		applyStatsChanges();
		
		for(FixedPartyTeam team : _teams)
		{
			for(PlayerEventInfo pi : team.getPlayers())
			{
				if(pi.isOnline())
				{
					if(pi.isImmobilized())
						pi.unroot();
					
					if(!pi.isGM())
						pi.setIsInvul(false);
					
					pi.restoreData();
					pi.teleport(pi.getOrigLoc(), 0, true, 0);
					pi.sendMessage(LanguageEngine.getMsg("event_teleportBack"));
					
					CallBack.getInstance().getPlayerBase().eventEnd(pi);
				}
			}
		}

		if(_fences != null)
			CallBack.getInstance().getOut().unspawnFences(_fences);
		
		unspawnMapGuards();
		unspawnNpcs();
		
		_event.notifyGameEnd(this, getWinner());
	}

	@Override
	public void onDisconnect(PlayerEventInfo player)
	{
		if(player != null && player.isOnline())
		{
			if(player.isSpectator())
			{
				removeSpectator(player, true);
				return;
			}
			
			EventWarnings.getInstance().addPoints(player.getPlayersId(), 1);
			
			broadcastMessage(LanguageEngine.getMsg("game_playerDisconnected", player.getPlayersName()), true);
			
			EventTeam playerTeam = player.getEventTeam();
			playerTeam.removePlayer(player);
			
			player.restoreData();
			player.setXYZInvisible(player.getOrigLoc().getX(), player.getOrigLoc().getY(), player.getOrigLoc().getZ());

			if(!_aborted)
			{
				if(playerTeam.getPlayers().isEmpty())
				{
					broadcastMessage(LanguageEngine.getMsg("game_teamDisconnected", playerTeam.getTeamName()), true);
				}
				
				if(checkIfTeamsDisconnected())
				{
					broadcastMessage(LanguageEngine.getMsg("event_disconnect_all"), true);
					clearEvent();
					return;
				}
				
				final FixedPartyTeam team = checkLastAliveTeam();
				if(team != null)
				{
					CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
					{
						@Override
						public void run()
						{
							nextRound(team, false);
						}
					}, 3000);
				}
			}
		}
	}
	
	private boolean checkIfTeamsDisconnected()
	{
		int teamsOn = 0;
		
		for(FixedPartyTeam team : _teams)
		{
			for(PlayerEventInfo pi : team.getPlayers())
			{
				if(pi.isOnline())
				{
					teamsOn ++;
					break;
				}
			}
		}
		
		return teamsOn == 0 || teamsOn == 1;
	}
	
	@Override
	protected void checkPlayersLoc()
	{
		//TODO check if player is not in another location
	}
	
	@Override
	protected void checkIfPlayersTeleported()
	{
		
	}
	
	private void cancelSchedulers()
	{
		// already canceled
		if(_aborted)
			return;
		
		_aborted = true;
		
		CallbackManager.getInstance().eventEnded(1, getEvent().getEventType(), Arrays.asList(_teams));
		
		if(_announcer != null)
		{
			_announcer.cancel();
			_announcer = null;
		}
		
		if(_locChecker != null)
		{
			_locChecker.cancel(false);
			_locChecker = null;
		}
		
		if(_eventEnd != null)
		{
			_eventEnd.cancel(false);
			_eventEnd = null;
		}
		
		if(_roundStart != null)
		{
			_roundStart.cancel(false);
			_roundStart = null;
		}
	}

	@Override
	public int getInstanceId()
	{
		return _instanceId;
	}

	@Override
	public EventTeam[] getTeams()
	{
		return _teams;
	}

	@Override
	public EventPlayerData createPlayerData(PlayerEventInfo player)
	{
		return new PvPEventPlayerData(player, this, new GlobalStatsModel(_event.getEventType()));
	}

	@Override
	public PvPEventPlayerData getPlayerData(PlayerEventInfo player)
	{
		return (PvPEventPlayerData) player.getEventData();
	}
}

