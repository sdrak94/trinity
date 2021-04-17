/**
 * 
 */
package cz.nxs.events.engine.main.pvpzone;

import java.util.List;

import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventManager.DisconnectedPlayerData;
import cz.nxs.events.engine.base.EventPlayerData;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.Loc;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.ItemData;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import javolution.util.FastList;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.util.Rnd;

/**
 * @author hNoke
 *
 */
public class ColiseumZone extends PvpZone
{
	private static EventSpawn team1spawn = new EventSpawn(-1, 1, new Loc(151430, 46716, -3410), 1, "Regular");
	private static EventSpawn team2spawn = new EventSpawn(-1, 2, new Loc(147557, 46723, -3410), 2, "Regular");
	
	private static final int team1Door = 24190002;
	private static final int team2Door = 24190003;
	private static final int team1StaticDoor = 24190001;
	private static final int team2StaticDoor = 24190004;
	
	private static final int PLAYERS_REQUIRED = 4;
	private static final int ROUND_TIME_LIMIT = 600000;
	private static final int ROUND_PREPARATION_DELAY = 35000;
	private static final int EVENT_TIME_LIMIT = 1200000;
	private static final int MAX_ROUNDS = 10;
	
	private State _state = State.Inactive;
	private long _timeEventStarted = 0;
	private long _timeWhenRoundStartedPreparing = 0;
	private long _timeWhenRoundStartedFighting = 0;
	
	private List<PlayerEventInfo> _playersQueue = null;
	private List<EventTeam> _teams = null;
	
	private int _round;
	
	enum State
	{
		Inactive, 
		Waiting,
		Preparation,
		Fight
	}
	
	@Override
	public void start()
	{
		try
		{
			if(_state == State.Inactive)
			{
				_timeEventStarted = System.currentTimeMillis();
				
				_state = State.Waiting;
				
				_round = 0;
				
				_playersQueue = new FastList<PlayerEventInfo>();
				
				_teams = new FastList<EventTeam>();
				_teams.add(new EventTeam(null, 1, "Blue", "Blue Team"));
				_teams.add(new EventTeam(null, 2, "Red", "Red Team"));
				
				_checker = new Checker(5000);
				_checker.start();
				
				handleGates(false);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public synchronized void scheduledCheck()
	{
		if(_state == State.Waiting)
		{
			if(canStartNewRound())
			{
				announce(0, "The zone has reached its time.");
				end();
				return;
			}
			
			// check if enought players are registered
			if(checkIfEnoughtPlayersToStartRound())
			{
				_state = State.Preparation;
				
				for(PlayerEventInfo player : _playersQueue)
				{
					addToTeam(player);
				}
				
				startFirstRound();
			}
		}
		else if(_state == State.Preparation)
		{
			// event doesn't run longer than 20 minutes
			if(canStartNewRound())
			{
				announce(0, "The zone has reached its time.");
				end();
			}
			else
			{
				if(_timeWhenRoundStartedPreparing > 0 && _timeWhenRoundStartedPreparing + ROUND_PREPARATION_DELAY <= System.currentTimeMillis())
				{
					startFightPhase();
				}
			}
		}
		else if(_state == State.Fight)
		{
			if(_timeWhenRoundStartedFighting > 0 && _timeWhenRoundStartedFighting + ROUND_TIME_LIMIT <= System.currentTimeMillis())
			{
				announce(0, "The round time limit of " + ROUND_TIME_LIMIT/60000 + " minutes has passed.");
				endRound();
			}
			else if(checkIfPlayersDeadOrMissing())
			{
				endRound();
			}
		}
	}
	
	private boolean checkIfEnoughtPlayersToStartRound()
	{
		int currentPlayers = 0;
		
		for(EventTeam t : _teams)
		{
			for(PlayerEventInfo player : t.getPlayers())
			{
				if(player.isOnline())
					currentPlayers ++;
			}
		}
		
		if((_playersQueue.size() + currentPlayers) >= PLAYERS_REQUIRED)
		{
			return true;
		}
		else
			return false;
	}
	
	private boolean checkIfPlayersDeadOrMissing()
	{
		int alive = 0;
		String message = null;
		
		for(EventTeam team : _teams)
		{
			alive = 0;
			for(PlayerEventInfo player : team.getPlayers())
			{
				if(player.isOnline() && !player.isDead())
				{
					alive ++;
				}
			}
			
			if(alive == 0)
			{
				message = "All players from " + team.getFullName() + " have been eliminated.";
				announce(0, message);
				rewardOtherTeam(team);
				break;
			}
		}
		
		if(message != null)
		{
			return true;
		}
		
		return false;
	}
	
	private void rewardOtherTeam(EventTeam losers)
	{
		for(EventTeam t : _teams)
		{
			if(losers.getTeamId() != t.getTeamId())
			{
				announce(0, t.getFullName() + " has won this round.", true);
				
				for(PlayerEventInfo player : t.getPlayers())
				{
					player.addItem(14721, 1, true);
				}
			}
		}
	}
	
	private void startFirstRound()
	{
		if(_state == State.Preparation)
		{
			for(EventTeam team : _teams)
			{
				for(PlayerEventInfo player : team.getPlayers())
				{
					teleportToTheRoom(player);
				}
			}
			
			_timeWhenRoundStartedPreparing = System.currentTimeMillis();
		}
	}
	
	private void startFightPhase()
	{
		if(_state == State.Preparation)
		{
			if(!checkIfEnoughtPlayersToStartRound())
			{
				_state = State.Waiting;
				
				announce(0, "There's not enought players to start a new round... Waiting for more.");
				
				return;
			}
			
			
			_state = State.Fight;
			
			_round ++;
			
			announce(0, "Round " + _round + " has started!");
			
			_timeWhenRoundStartedFighting = System.currentTimeMillis();
			
			handleGates(true);
		}
	}
	
	private boolean canStartNewRound()
	{
		return (_round >= MAX_ROUNDS || _timeEventStarted + EVENT_TIME_LIMIT <= System.currentTimeMillis());
	}
	
	private void endRound()
	{
		if(_state == State.Fight)
		{
			_state = State.Preparation;
			
			announce(0, "Round " + _round + " has ended!");
			
			if(canStartNewRound())
				announce(0, "The next round will start soon.");
			
			for(EventTeam team : _teams)
			{
				for(PlayerEventInfo player : team.getPlayers())
				{
					teleportToTheRoom(player);
				}
			}
			
			_timeWhenRoundStartedPreparing = System.currentTimeMillis();
			
			handleGates(false);
		}
	}
	
	// opens or closes gates
	private void handleGates(boolean open)
	{
		L2DoorInstance team1Door = DoorTable.getInstance().getDoor(ColiseumZone.team1Door);
		L2DoorInstance team2Door = DoorTable.getInstance().getDoor(ColiseumZone.team2Door);
		L2DoorInstance team1StaticDoor = DoorTable.getInstance().getDoor(ColiseumZone.team1StaticDoor);
		L2DoorInstance team2StaticDoor = DoorTable.getInstance().getDoor(ColiseumZone.team2StaticDoor);
		
		try
		{
			team1StaticDoor.closeMe();
			team2StaticDoor.closeMe();
			
			if(open)
			{
				team1Door.openMe();
				team2Door.openMe();
			}
			else
			{
				team1Door.closeMe();
				team2Door.closeMe();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void end()
	{
		_checker.disable();
		
		_state = State.Inactive;
		
		announce(0, "The zone has been closed.");
		
		if(_teams != null)
		{
			for(EventTeam team : _teams)
			{
				for(PlayerEventInfo player : team.getPlayers())
				{
					if(!player.isOnline())
						continue;
					
					player.restoreData();
					
					player.teleport(player.getOrigLoc(), 0, true, 0);
					player.sendMessage(LanguageEngine.getMsg("event_teleportBack"));
					
					if (player.getParty() != null)
					{
						PartyData party = player.getParty();
						party.removePartyMember(player);
					}
					
					player.broadcastUserInfo();
					
					team.removePlayer(player);
					
					player.setIsRegisteredToPvpZone(false);
					CallBack.getInstance().getPlayerBase().eventEnd(player);
				}
			}
		}
		
		if(_playersQueue != null)
		{
			for(PlayerEventInfo player : _playersQueue)
			{
				if(!player.isOnline())
					continue;
				
				player.setIsRegisteredToPvpZone(false);
				CallBack.getInstance().getPlayerBase().eventEnd(player);
			}
		}
		
		_playersQueue.clear();
		_teams.clear();
		
		EventManager.getInstance().getMainEventManager().getPvpZoneManager().stop();
	}
	
	@Override
	public boolean canRegister(PlayerEventInfo player)
	{
		//TODO
		return true;
	}
	
	@Override
	public boolean canUnregister(PlayerEventInfo player)
	{
		if(_state == State.Preparation || _state == State.Waiting)
		{
			return true;
		}
		else
			return false;
	}
	
	@Override
	public void addPlayer(PlayerEventInfo player)
	{
		if(_state == State.Inactive || !player.isOnline())
			return;
		
		if(_state == State.Waiting)
		{
			_playersQueue.add(player);
			
			player.sendMessage("You will be teleported to the zone soon once enought players register.");
		}
		
		if(_state == State.Fight)
		{
			_playersQueue.add(player);
			
			player.sendMessage("You will be teleported to the zone after the current round ends.");
		}
		
		if(_state == State.Preparation)
		{
			addToTeam(player);
			teleportToTheRoom(player);
		}
	}
	
	private void addToTeam(PlayerEventInfo player)
	{
		_playersQueue.remove(player);
		
		int team1Players = _teams.get(0).getPlayers().size();
		int team2Players = _teams.get(1).getPlayers().size();
		
		player.onEventStart(this);
		
		if(team1Players > team2Players)
		{
			_teams.get(1).addPlayer(player, true);
		}
		else if(team1Players < team2Players)
		{
			_teams.get(0).addPlayer(player, true);
		}
		else
		{
			_teams.get(Rnd.get(2)).addPlayer(player, true);
		}
	}
	
	private void teleportToTheRoom(PlayerEventInfo player)
	{
		if(player.isDead())
		{
			player.doRevive();
		}
		
		player.screenMessage("You are being teleported to the waiting room.", "Coliseum", false);
		
		if(player.getTeamId() == 1)
		{
			player.teleport(team1spawn.getLoc(), 0, true, 0);
		}
		else if(player.getTeamId() == 2)
		{
			player.teleport(team2spawn.getLoc(), 0, true, 0);
		}
	}
	
	@Override
	public void removePlayer(PlayerEventInfo player)
	{
		_playersQueue.remove(player);
		EventTeam team = player.getEventTeam();
		
		if(team != null)
		{
			team.removePlayer(player);
			
			player.setIsRegisteredToPvpZone(false);
			CallBack.getInstance().getPlayerBase().eventEnd(player);
			
			player.restoreData();
			
			player.teleport(player.getOrigLoc(), 0, true, 0);
			player.sendMessage(LanguageEngine.getMsg("event_teleportBack"));
			
			if (player.getParty() != null)
			{
				PartyData party = player.getParty();
				party.removePartyMember(player);
			}
			
			player.broadcastUserInfo();
		}
	}

	@Override
	public String getName()
	{
		return "Coliseum";
	}
	
	@Override
	public String getStateNameForHtml()
	{
		return _state.toString();
	}
	
	@Override
	public int getPlayersCountForHtml()
	{
		int num = 0;
		
		num += _playersQueue.size();
		
		if(_teams != null)
		{
			for(EventTeam team : _teams)
				num += team.getPlayers().size();
		}
		
		return num;
	}
	
	public void announce(int teamId, String message)
	{
		announce(teamId, message, false);
	}
	
	public void announce(int teamId, String message, boolean special)
	{
		for(EventTeam team : _teams)
		{
			if(teamId <= 0 || team.getTeamId() == teamId)
			{
				for(PlayerEventInfo player : team.getPlayers())
				{
					player.screenMessage(message, getName(), special);
				}
			}
		}
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
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
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
	public boolean canInviteToParty(PlayerEventInfo player, PlayerEventInfo target)
	{
		if(target.getEvent() != player.getEvent())
			return false;
		else if(!player.canInviteToParty() || !target.canInviteToParty())
			return false;
		else if(target.getTeamId() == player.getTeamId())
			return true;
		
		return false;
	}

	@Override
	public void onDisconnect(PlayerEventInfo player)
	{
		if(player.isOnline()) // still should be online
		{
			EventTeam team = player.getEventTeam();
			player.restoreData(); // restore title, etc. before logging out and saving
			player.setXYZInvisible(player.getOrigLoc().getX(), player.getOrigLoc().getY(), player.getOrigLoc().getZ());
			
			team.removePlayer(player);
			
			CallBack.getInstance().getPlayerBase().playerDisconnected(player);
			
			player.setIsRegisteredToPvpZone(false);
		}
	}
	
	@Override
	public boolean canUseItem(PlayerEventInfo player, ItemData item)
	{
		if (item.isScroll())
			return false;
		
		if(item.isPetCollar())
		{
			player.sendMessage(LanguageEngine.getMsg("event_petsNotAllowed"));
			return false;
		}

		return true;
	}
	
	@Override
	public boolean canUseSkill(PlayerEventInfo player, SkillData skill)
	{
		if (skill.getSkillType().equals("RESURRECT"))
			return false;
		
		if (skill.getSkillType().equals("RECALL"))
			return false;
		
		if (skill.getSkillType().equals("SUMMON_FRIEND"))
			return false;

		if (skill.getSkillType().equals("FAKE_DEATH"))
			return false;

		return true;
	}
	
	@Override
	public EventPlayerData createPlayerData(PlayerEventInfo player)
	{
		return null;
	}

	@Override
	public EventPlayerData getPlayerData(PlayerEventInfo player)
	{
		return null;
	}

	@Override
	public void clearEvent()
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
		return true;
	}

	@Override
	public boolean onNpcAction(PlayerEventInfo player, NpcData npc)
	{
		return true;
	}

	@Override
	public void onItemUse(PlayerEventInfo player, ItemData item)
	{
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
	public boolean canTransform(PlayerEventInfo player)
	{
		return false;
	}

	@Override
	public boolean canBeDisarmed(PlayerEventInfo player)
	{
		return true;
	}

	@Override
	public int allowTransformationSkill(PlayerEventInfo playerEventInfo, SkillData skillData)
	{
		return 0;
	}

	@Override
	public boolean canSaveShortcuts(PlayerEventInfo player)
	{
		return true;
	}

	@Override
	public int isSkillOffensive(SkillData skill)
	{
		return -1;
	}

	@Override
	public boolean isSkillNeutral(SkillData skill)
	{
		return false;
	}

	@Override
	public void playerWentAfk(PlayerEventInfo player, boolean warningOnly, int afkTime)
	{
	}

	@Override
	public void playerReturnedFromAfk(PlayerEventInfo player)
	{
		
	}
}
