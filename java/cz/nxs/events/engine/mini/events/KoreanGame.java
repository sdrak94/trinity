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
import java.util.concurrent.ScheduledFuture;

import cz.nxs.events.engine.EventBuffer;
import cz.nxs.events.engine.EventRewardSystem;
import cz.nxs.events.engine.EventWarnings;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventPlayerData;
import cz.nxs.events.engine.base.PvPEventPlayerData;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.mini.MiniEventGame;
import cz.nxs.events.engine.mini.RegistrationData;
import cz.nxs.events.engine.stats.GlobalStats.GlobalStatType;
import cz.nxs.events.engine.stats.GlobalStatsModel;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.events.engine.team.KoreanTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;


/**
 * @author hNoke
 *
 */
public class KoreanGame extends MiniEventGame implements Runnable
{
	private KoreanTeam[] _teams;
	private boolean _initState;
	
	private ScheduledFuture<?> _eventEnd;
	private ScheduledFuture<?> _roundStart;
	
	public KoreanGame(int gameId, EventMap arena, KoreanManager event, RegistrationData[] teams)
	{
		super(gameId, arena, event, teams);
		
		_initState = true;
		final int teamsAmmount = 2;
		
		_teams = new KoreanTeam[teamsAmmount];
		
		for(int i = 0; i < teamsAmmount; i++)
		{
			_teams[i] = new KoreanTeam(teams[i], i + 1, teams[i].getKeyPlayer().getPlayersName() + "'s party");
			
			for(PlayerEventInfo pi : teams[i].getPlayers())
			{
				pi.onEventStart(this);
				_teams[i].addPlayer(pi, true);
			}
		}
		
		CallbackManager.getInstance().eventStarts(1, getEvent().getEventType(), Arrays.asList(_teams));
	}
	
	@Override
	public void run() 
	{
		initEvent();
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
			
			for(KoreanTeam team : _teams)
			{
				for(PlayerEventInfo pi : team.getPlayers())
				{
					pi.teleport(_arena.getNextSpawn(team.getTeamId(), SpawnType.Safe).getLoc(), 0, true, _instanceId);
					
					pi.disableAfkCheck(true);
					
					if(getEvent().getBoolean("removeCubics"))
						pi.removeCubics();
					
					if(_allowSchemeBuffer)
						EventBuffer.getInstance().buffPlayer(pi, true);
					
					pi.enableAllSkills();
				}
			}

			scheduleMessage(LanguageEngine.getMsg("game_teleportDone"), 1500, true);
			
			handleDoors(1);
			
			int startTime = _event.getMapConfigInt(_arena, "WaitTime");

			_roundStart = CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					finishRoundStart();
				}
			}, startTime);
			
			scheduleMessage(LanguageEngine.getMsg("game_matchStartsIn", (startTime / 1000)), 5000, true);
		}
		catch (Exception e)
		{
			abortDueToError("Map wasn't set up correctly.");
			e.printStackTrace();
		}
	}
	
	private void finishRoundStart()
	{
		if(_aborted)
			return;
		
		broadcastMessage(LanguageEngine.getMsg("game_korean_teleportingToArena"), true);
		
		unspawnBuffers();
		
		handleDoors(2);
		
		teleportToEventLocation();
		
		_initState = false;
		
		final PlayerEventInfo player1 = getNextPlayer(1);
		final PlayerEventInfo player2 = getNextPlayer(2);

		scheduleMessage(LanguageEngine.getMsg("game_korean_nextFight", player1.getPlayersName(), player2.getPlayersName(), 8), 3000, true);
		
		CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				startFight(player1, player2);
			}
		}, 11000);

		startAnnouncing();
	}
	
	private void startFight(PlayerEventInfo player1, PlayerEventInfo player2)
	{
		if(_aborted)
			return;
		
		SkillData skill = new SkillData(5965, 1);
		
		player1.disableAfkCheck(false);
		player1.setIsParalyzed(false);
		player1.setIsInvul(false);
		player1.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_STEALTH());
		player1.broadcastSkillUse(null, null, skill.getId(), skill.getLevel());
		player1.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
		
		player2.disableAfkCheck(false);
		player2.setIsParalyzed(false);
		player2.setIsInvul(false);
		player2.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_STEALTH());
		player2.broadcastSkillUse(null, null, skill.getId(), skill.getLevel());
		player2.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
		
		broadcastMessage(LanguageEngine.getMsg("game_korean_fightStarted"), true);
	}
	
	private void teleportToEventLocation()
	{
		try
		{
			for(KoreanTeam team : _teams)
			{
				for(PlayerEventInfo member : team.getPlayers())
				{
					member.teleport(_arena.getNextSpawn(team.getTeamId(), SpawnType.Regular).getLoc(), 0, false, -1);
					
					member.setIsInvul(true);
					member.setIsParalyzed(true);
					member.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_STEALTH());
				}
			}
		} 
		catch (Exception e)
		{
			abortDueToError("Map wasn't propably set up correctly.");
			e.printStackTrace();
		}
		
	}
	
	private PlayerEventInfo getNextPlayer(int teamId)
	{
		return _teams[teamId - 1].getNextPlayer();
	}
	
	@Override
	public void onDie(final PlayerEventInfo player, CharacterData killer)
	{
		if(_aborted)
			return;
		
		updateScore(player, killer);
		
		player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
		
		if(player.getEventTeam().getDeaths() >= player.getEventTeam().getPlayers().size())
		{
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					endByDie(oppositeTeam(player.getEventTeam()));
				}
			}, 3000);
		}
		else // unroots another player
		{
			final PlayerEventInfo nextPlayer = ((KoreanTeam) player.getEventTeam()).getNextPlayer();
			
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					announceNextPlayer(nextPlayer);
				}
			}, 3000);
		}
	}
	
	private void announceNextPlayer(PlayerEventInfo nextPlayer)
	{
		SkillData skill = new SkillData(5965, 1);
		
		nextPlayer.setIsParalyzed(false);
		nextPlayer.setIsInvul(false);
		nextPlayer.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_STEALTH());
		nextPlayer.broadcastSkillUse(null, null, skill.getId(), skill.getLevel());
		nextPlayer.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
		
		broadcastMessage(LanguageEngine.getMsg("game_korean_nextPlayer", nextPlayer.getPlayersName()), false);
	}

	private void endByTime()
	{
		if(_aborted)
			return;
		
		cancelSchedulers();
		
		broadcastMessage(LanguageEngine.getMsg("game_matchEnd_timeLimit", getGameTime() / 60000), false);
		scheduleMessage(LanguageEngine.getMsg("game_matchEnd_tie"), 3000, false);

		int topScore = 0;
		KoreanTeam top = null;
		for(KoreanTeam team : _teams)
		{
			if(team.getScore() > topScore)
			{
				topScore = team.getScore();
				top = team;
			}

			for(PlayerEventInfo pi : team.getPlayers())
			{
				EventRewardSystem.getInstance().rewardPlayer(getEvent().getEventType(), getEvent().getMode().getModeId(), pi, RewardPosition.Tie_TimeLimit, null, pi.getTotalTimeAfk(), 0, 0);
				
				getPlayerData(pi).getGlobalStats().raise(GlobalStatType.LOSES, 1);
				_event.logPlayer(pi, 2);
			}
		}

		if(top != null)
			setWinner(top);
		
		saveGlobalStats();
		
		CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				clearEvent();
			}
		}, 8000);
		
		return;
	}

	private void endByDie(EventTeam winner)
	{
		cancelSchedulers();
		
		broadcastMessage(LanguageEngine.getMsg("game_korean_winner", winner.getTeamName()), false);

		setWinner(winner);

		for(PlayerEventInfo pi : winner.getPlayers())
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
		
		for(PlayerEventInfo pi : oppositeTeam(winner).getPlayers())
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
		
		saveGlobalStats();

		CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				clearEvent();
			}
		}, 5000);
	}

	@Override
	public void clearEvent()
	{
		cancelSchedulers();
		
		cleanSpectators();
		
		applyStatsChanges();
		
		for(KoreanTeam team : _teams)
		{
			for(PlayerEventInfo pi : team.getPlayers())
			{
				if(pi.isOnline())
				{
					if(pi.isParalyzed())
						pi.setIsParalyzed(false);
					
					pi.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
					pi.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_STEALTH());
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
	public void onDisconnect(final PlayerEventInfo player)
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
			
			final EventTeam playerTeam = player.getEventTeam();
			playerTeam.removePlayer(player);
			
			player.restoreData();
			player.setXYZInvisible(player.getOrigLoc().getX(), player.getOrigLoc().getY(), player.getOrigLoc().getZ());
			
			if(!_aborted)
			{
				if(playerTeam.getPlayers().isEmpty() || !checkTeamStatus(playerTeam.getTeamId()))
				{
					cancelSchedulers();
					
					CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
					{
						@Override
						public void run()
						{
							broadcastMessage(LanguageEngine.getMsg("event_disconnect_all"), false);
							if(_initState)
								clearEvent();
							else 
								endByDie(oppositeTeam(playerTeam));
						}
					}, 3000);
				}
				else if(!_initState)
				{
					if(((KoreanTeam) playerTeam).isFighting(player))
					{
						final PlayerEventInfo nextPlayer = ((KoreanTeam) playerTeam).getNextPlayer();
						
						if(nextPlayer == null)
						{
							cancelSchedulers();
							
							CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
							{
								@Override
								public void run()
								{
									broadcastMessage(LanguageEngine.getMsg("event_disconnect_all"), false);
									endByDie(oppositeTeam(playerTeam));
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
									announceNextPlayer(nextPlayer);
								}
							}, 5000);
						}
					}
				}
			}
		}
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
	
	private KoreanTeam oppositeTeam(EventTeam team)
	{
		if(team.getTeamId() == 1)
			return _teams[1]; // returns second team
		else if(team.getTeamId() == 2)
			return _teams[0]; // returns first team
		return null;
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

