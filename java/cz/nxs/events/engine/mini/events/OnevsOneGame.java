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
import cz.nxs.events.engine.team.OnePlayerTeam;
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
public class OnevsOneGame extends MiniEventGame
{
	private final int _teamsAmmount;
	private final int _roundsAmmount;
	
	protected OnePlayerTeam[] _players;
	
	private ScheduledFuture<?> _eventEnd;
	private ScheduledFuture<?> _roundStart;
	
	private int _round;
	
	public OnevsOneGame(int gameId, EventMap arena, OnevsOneManager event, RegistrationData[] teams)
	{
		super(gameId, arena, event, teams);
		
		_teamsAmmount = event.getTeamsCount();
		_roundsAmmount = event.getRoundsAmmount();
		
		_players = new OnePlayerTeam[_teamsAmmount];
		
		for(int i = 0; i < _teamsAmmount; i++)
		{
			_players[i] = new OnePlayerTeam(teams[i], i + 1, teams[i].getKeyPlayer().getPlayersName());
			
			teams[i].getKeyPlayer().onEventStart(this);
			_players[i].addPlayer(teams[i].getKeyPlayer(), true);
		}
		
		CallbackManager.getInstance().eventStarts(1, getEvent().getEventType(), Arrays.asList(_players));
		
		_round = 0;
	}

	@Override
	protected void initEvent()
	{
		super.initEvent();
		
		startEvent();
	}

	@Override
	protected void startEvent()
	{
		try
		{
			broadcastMessage(LanguageEngine.getMsg("game_teleporting"), false);

			_eventEnd = CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					endByTime();
				}
			}, getGameTime());
			
			scheduleMessage(LanguageEngine.getMsg("game_teleportDone"), 1500, true);
			
			nextRound(false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void nextRound(boolean forceEnd)
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
			_round++;
			
			handleDoors(1);
			loadBuffers();
			
			EventSpawn spawn;
			for(OnePlayerTeam team : _players)
			{
				if(team.getPlayer() != null && team.getPlayer().isOnline())
				{
					spawn = _arena.getNextSpawn(team.getTeamId(), SpawnType.Regular);
					
					if(spawn == null)
					{
						abortDueToError("No regular spawn found for team " + team.getTeamId() + ". Match aborted.");
						clearEvent();
						return;
					}
					
					team.getPlayer().teleport(spawn.getLoc(), 0, false, _instanceId);
					
					team.getPlayer().disableAfkCheck(true);
					team.getPlayer().root();
					team.getPlayer().setIsInvul(true);
					
					if(_round == 1 && getEvent().getBoolean("removeCubics"))
						team.getPlayer().removeCubics();
					
					if(_allowSchemeBuffer)
						EventBuffer.getInstance().buffPlayer(team.getPlayer(), true);
					
					if(_round == 1)
						team.getPlayer().enableAllSkills();
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
		
		for(OnePlayerTeam team : _players)
		{
			if(team.getPlayer() != null && team.getPlayer().isOnline())
			{
				team.getPlayer().disableAfkCheck(false);
				team.getPlayer().unroot();
				team.getPlayer().setIsInvul(false);
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
		
		final OnePlayerTeam team = checkLastAlivePlayer();
		if(team != null)
		{
			team.raiseScore(1);
			onScore(team.getPlayers(), 1);
			
			final boolean forceEnd = !checkIfTheMatchCanContinue();
			
			if(_round == _roundsAmmount || forceEnd)
				scheduleMessage(LanguageEngine.getMsg("game_matchEnd"), 3000, true);
			else 
				scheduleMessage(LanguageEngine.getMsg("game_roundWonBy", getRoundName(_round, _roundsAmmount), team.getTeamName()), 3000, true);
			
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					nextRound(forceEnd);
				}
			}, 4000);
		}
	}
	
	private boolean checkIfTheMatchCanContinue()
	{
		int remainingRounds = _roundsAmmount - _round;
		int bestScore = 0;
		int secondScore = 0;
		
		for(OnePlayerTeam team : _players)
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
	
	private OnePlayerTeam checkLastAlivePlayer()
	{
		int alivePlayers = 0;
		OnePlayerTeam tempTeam = null;
		
		for(OnePlayerTeam team : _players)
		{
			if(team.getPlayer() != null && team.getPlayer().isOnline())
			{
				// there is at least one alive player
				if(!team.getPlayer().isDead())
				{
					alivePlayers++;
					tempTeam = team;
					continue;
				}
			}
		}
		
		if(alivePlayers == 1)
			return tempTeam;
		else
			return null;
	}

	private void endByTime()
	{
		if(_aborted)
			return;
		
		cancelSchedulers();
		
		broadcastMessage(LanguageEngine.getMsg("game_matchEnd_timeLimit", (getGameTime() / 60000)), false);
		scheduleMessage(LanguageEngine.getMsg("game_matchEnd_tie"), 3000, false);

		int topScore = 0;
		OnePlayerTeam top = null;

		for(OnePlayerTeam team : _players)
		{
			if(team.getScore() > topScore)
			{
				topScore = team.getScore();
				top = team;
			}

			if(team.getPlayer() != null)
			{
				EventRewardSystem.getInstance().rewardPlayer(getEvent().getEventType(), getEvent().getMode().getModeId(), team.getPlayer(), RewardPosition.Tie_TimeLimit, null, team.getPlayer().getTotalTimeAfk(), 0, 0);
				
				getPlayerData(team.getPlayer()).getGlobalStats().raise(GlobalStatType.LOSES, 1);
				_event.logPlayer(team.getPlayer(), 2);
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
		
		List<OnePlayerTeam> sortedTeams = new FastList<OnePlayerTeam>();
		for(OnePlayerTeam team : _players)
		{
			sortedTeams.add(team);
		}
		
		Collections.sort(sortedTeams, EventManager.getInstance().compareTeamScore);
		
		// score, teams
		Map<Integer, FastList<OnePlayerTeam>> scores = new FastMap<Integer, FastList<OnePlayerTeam>>();
		
		for(OnePlayerTeam team : sortedTeams)
		{
			if(!scores.containsKey(team.getScore()))
				scores.put(team.getScore(), new FastList<OnePlayerTeam>());
			
			scores.get(team.getScore()).add(team);
		}

		int place = 1;
		for(OnePlayerTeam team : sortedTeams)
		{
			broadcastMessage(LanguageEngine.getMsg("event_announceScore_includeKills", place, team.getTeamName(), team.getScore(), team.getKills()), false);
			place++;
		}
		
		place = 1;
		for(Entry<Integer, FastList<OnePlayerTeam>> i : scores.entrySet())
		{
			// winners
			if(place == 1)
			{
				if(i.getValue().size() > 1)// at least two teams are winners (have the same score) and the match has more than 2 teams
				{
					if(_teamsAmmount > i.getValue().size())
					{
						TextBuilder tb = new TextBuilder();
						
						for(OnePlayerTeam team : i.getValue())
						{
							tb.append(LanguageEngine.getMsg("event_ffa_announceWinner2_part1", team.getTeamName()));
						}
						
						String s = tb.toString();
						tb = new TextBuilder(s.substring(0, s.length() - 4));
						tb.append(LanguageEngine.getMsg("event_ffa_announceWinner2_part1"));
						
						broadcastMessage(tb.toString(), false);
						
						for(OnePlayerTeam team : i.getValue())
						{
							setWinner(team);

							if(team.getPlayer() != null)
							{
								if(team.getPlayer().isOnline())
								{
									EventRewardSystem.getInstance().rewardPlayer(getEvent().getEventType(), getEvent().getMode().getModeId(), team.getPlayer(), RewardPosition.Winner, null, team.getPlayer().getTotalTimeAfk(), 0, 0);
									setEndStatus(team.getPlayer(), 1);
									//showScore(team.getPlayer(), 1);
								}
								
								getPlayerData(team.getPlayer()).getGlobalStats().raise(GlobalStatType.WINS, 1);
								_event.logPlayer(team.getPlayer(), 1);
							}
						}
					}
					else // all teams are 'winners' - have the same score (but > 0)
					{
						broadcastMessage(LanguageEngine.getMsg("event_ffa_announceWinner3"), false);
						
						for(OnePlayerTeam team : i.getValue())
						{
							setWinner(team);

							if(team.getPlayer() != null)
							{
								if(team.getPlayer().isOnline())
								{
									EventRewardSystem.getInstance().rewardPlayer(getEvent().getEventType(), getEvent().getMode().getModeId(), team.getPlayer(), RewardPosition.Tie, null, team.getPlayer().getTotalTimeAfk(), 0, 0);
									//setEndStatus(team.getPlayer(), 1);
									//showScore(team.getPlayer(), 2);
								}
								
								getPlayerData(team.getPlayer()).getGlobalStats().raise(GlobalStatType.WINS, 1);
								_event.logPlayer(team.getPlayer(), 2);
							}
						}
					}
				}
				else // single team is winner
				{
					OnePlayerTeam winnerPlayer = i.getValue().getFirst();

					setWinner(winnerPlayer);
					
					broadcastMessage(LanguageEngine.getMsg("event_ffa_announceWinner1", i.getValue().getFirst().getTeamName()), false);
					
					if(winnerPlayer.getPlayer() != null)
					{
						if(winnerPlayer.getPlayer().isOnline())
						{
							EventRewardSystem.getInstance().rewardPlayer(getEvent().getEventType(), getEvent().getMode().getModeId(), winnerPlayer.getPlayer(), RewardPosition.Winner, null, winnerPlayer.getPlayer().getTotalTimeAfk(), 0, 0);
							setEndStatus(winnerPlayer.getPlayer(), 1);
							//showScore(winnerPlayer.getPlayer(), 1);
						}
						
						getPlayerData(winnerPlayer.getPlayer()).getGlobalStats().raise(GlobalStatType.WINS, 1);
						_event.logPlayer(winnerPlayer.getPlayer(), 1);
					}
				}
			}
			else // loosers
			{
				for(OnePlayerTeam team : i.getValue())
				{
					if(team.getPlayer() != null)
					{
						if(team.getPlayer().isOnline())
						{
							EventRewardSystem.getInstance().rewardPlayer(getEvent().getEventType(), getEvent().getMode().getModeId(), team.getPlayer(), RewardPosition.Looser, null, team.getPlayer().getTotalTimeAfk(), 0, 0);
							setEndStatus(team.getPlayer(), 0);
							//showScore(team.getPlayer(), 2);
						}
						
						// update stats
						getPlayerData(team.getPlayer()).getGlobalStats().raise(GlobalStatType.LOSES, 1);
						_event.logPlayer(team.getPlayer(), 2);
					}
				}
			}
			
			place++;
		}
		
		saveGlobalStats();

		scheduleClearEvent(8000);
	}

	@Override
	public void clearEvent()
	{
		cancelSchedulers();
		
		cleanSpectators();
		
		applyStatsChanges();
		
		for(OnePlayerTeam team : _players)
		{
			if(team.getPlayer() != null && team.getPlayer().isOnline())
			{
				if(team.getPlayer().isImmobilized())
					team.getPlayer().unroot();
				
				if(!team.getPlayer().isGM())
					team.getPlayer().setIsInvul(false);
				
				team.getPlayer().restoreData();
				team.getPlayer().teleport(team.getPlayer().getOrigLoc(), 0, true, 0);
				team.getPlayer().sendMessage(LanguageEngine.getMsg("event_teleportBack"));

				CallBack.getInstance().getPlayerBase().eventEnd(team.getPlayer());
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
		if(player != null && player.isOnline()) // player still shouldn't be null at this part
		{
			if(player.isSpectator())
			{
				removeSpectator(player, true);
				return;
			}
			
			EventWarnings.getInstance().addPoints(player.getPlayersId(), 1);
			
			if(_teamsAmmount == 2)
				broadcastMessage(LanguageEngine.getMsg("game_playerDisconnected2", player.getPlayersName()), true);
			else
				broadcastMessage(LanguageEngine.getMsg("game_playerDisconnected", player.getPlayersName()), true);
			
			EventTeam playerTeam = player.getEventTeam();
			
			player.restoreData();
			player.setXYZInvisible(player.getOrigLoc().getX(), player.getOrigLoc().getY(), player.getOrigLoc().getZ());
			
			if(!_aborted)
			{
				playerTeam.removePlayer(player);
				
				if(checkIfPlayersDisconnected())
				{
					broadcastMessage(LanguageEngine.getMsg("event_disconnect_all"), true);
					clearEvent();
					return;
				}
				
				final OnePlayerTeam team = checkLastAlivePlayer();
				if(team != null)
				{
					CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
					{
						@Override
						public void run()
						{
							nextRound(false);
						}
					}, 3000);
				}
			}
		}
	}
	
	private boolean checkIfPlayersDisconnected()
	{
		int teamsOn = 0;
		
		for(OnePlayerTeam team : _players)
		{
			if(team.getPlayer() != null && team.getPlayer().isOnline())
			{
				teamsOn ++;
				continue;
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
		//TODO check if player is near his Regular spawn
		// - check if this is called when it is still the wait time
		// - set min wait time so this function runs fine
		// - call this a few seconds after players are teleported OR get a hook from L2J core to know
	}
	
	private void cancelSchedulers()
	{
		if(_aborted)
			return;
		
		_aborted = true;
		
		CallbackManager.getInstance().eventEnded(1, getEvent().getEventType(), Arrays.asList(_players));
		
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
		return _players;
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

