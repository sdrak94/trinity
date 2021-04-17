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
package cz.nxs.events.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.RewardPosition.PositionType;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.events.AbstractMainEvent;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 * - loads and manages everything related to EventRewards
 */
public class EventRewardSystem
{
	private Map<EventType, FastMap<Integer, EventRewards>> _rewards;
	
	public EventRewardSystem()
	{
		_rewards = new FastMap<EventType, FastMap<Integer, EventRewards>>();
		
		for(EventType t : EventType.values())
		{
			_rewards.put(t, new FastMap<Integer, EventRewards>());
		}
		
		loadRewards();
	}
	
	private EventType getType(String s)
	{
		for(EventType t : EventType.values())
		{
			if(t.getAltTitle().equalsIgnoreCase(s))
			{
				return t;
			}
		}
		return null;
	}
	
	public EventRewards getAllRewardsFor(EventType event, int modeId)
	{
		if(_rewards.get(event).get(modeId) == null)
			_rewards.get(event).put(modeId, new EventRewards());

		return _rewards.get(event).get(modeId);
	}

	public void loadRewards()
	{
		Connection con = null;

		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT eventType, modeId, position, parameter, item_id, min, max, chance FROM nexus_rewards");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				EventRewards rewards = null;
				EventType type = getType(rset.getString("eventType"));
				int modeId = rset.getInt("modeId");
				
				if(!_rewards.get(type).containsKey(modeId))
				{
					rewards = new EventRewards();
					_rewards.get(type).put(modeId, rewards);
				}
				else
					rewards = _rewards.get(type).get(modeId);
				
				rewards.addItem(RewardPosition.getPosition(rset.getString("position")), rset.getString("parameter"), rset.getInt("item_id"), rset.getInt("min"), rset.getInt("max"), rset.getInt("chance"));
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
			try
			{
				con.close();
			}

			catch (Exception e)
			{
			}
		}
		
		NexusLoader.debug("Nexus Engine: Reward System Loaded.");
	}
	
	public int addRewardToDb(EventType type, RewardPosition position, String parameter, int modeId, int id, int minAmmount, int maxAmmount, int chance, boolean updateOnly)
	{
		if(_rewards.get(type).get(modeId) == null)
			_rewards.get(type).put(modeId, new EventRewards());
		
		EventRewards rewards = _rewards.get(type).get(modeId);
		int newId = 0;
		
		if(!updateOnly)
			newId = rewards.addItem(position, parameter, id, minAmmount, maxAmmount, chance);
		
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("REPLACE INTO nexus_rewards VALUES (?,?,?,?,?,?,?,?)");
			statement.setString(1, type.getAltTitle());
			statement.setInt(2, modeId);
			statement.setString(3, position.toString());
			statement.setString(4, (parameter == null ? "" : parameter));
			statement.setInt(5, id);
			statement.setInt(6, minAmmount);
			statement.setInt(7, maxAmmount);
			statement.setInt(8, chance);
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
		
		return newId;
	}

	public int createReward(EventType type, RewardPosition position, String parameter, int modeId)
	{
		return addRewardToDb(type, position, parameter, modeId, 57, 1, 1, 100, false);
	}
	
	public boolean setPositionRewarded(EventType type, int modeId, RewardPosition position, String parameter)
	{
		if(_rewards.get(type).get(modeId) == null)
			return false;
		
		if(_rewards.get(type).get(modeId).getContainer(position, parameter) != null)
		{
			return false;
		}
		else
		{
			_rewards.get(type).get(modeId).getOrCreateContainer(position, parameter);
			return true;
		}
	}
	
	public boolean removePositionRewarded(EventType type, int modeId, RewardPosition position, String parameter)
	{
		if(_rewards.get(type).get(modeId) == null)
			return false;
		
		if(_rewards.get(type).get(modeId).getContainer(position, parameter) == null)
		{
			return false;
		}
		else
		{
			PositionContainer container = _rewards.get(type).get(modeId).getContainer(position, parameter);
			
			Map<Integer, RewardItem> map = _rewards.get(type).get(modeId).getAllRewards().get(container);
			for(Entry<Integer, RewardItem> e : map.entrySet())
			{
				removeRewardFromDb(type, e.getKey(), modeId);
			}
			
			_rewards.get(type).get(modeId).getAllRewards().remove(container);
			
			return true;
		}
	}
	
	public void updateRewardInDb(EventType type, int rewardId, int modeId)
	{
		if(_rewards.get(type).get(modeId) == null)
			_rewards.get(type).put(modeId, new EventRewards());
		
		EventRewards rewards = _rewards.get(type).get(modeId);
		RewardItem item = rewards.getItem(rewardId);
		
		if(item == null)
			return;
		
		PositionContainer position = getRewardPosition(type, modeId, rewardId);
		addRewardToDb(type, position.position, position.parameter, modeId, item.id, item.minAmmount, item.maxAmmount, item.chance, true);
	}
	
	public void removeFromDb(EventType type, RewardPosition position, String parameter, int modeId, int itemId, int min, int max, int chance)
	{
		Connection con = null;
		
		try
		{
			con = CallBack.getInstance().getOut().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM nexus_rewards WHERE eventType = '" + type.getAltTitle() + "' AND position = '" + position.toString() + "' AND parameter = '" + (parameter == null ? "" : parameter) + "' AND modeId = " + modeId + " AND item_id = " + itemId + " AND min = " + min + " AND max = " + max + " AND chance = " + chance);
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
	}
	
	public void removeRewardFromDb(EventType type, int rewardId, int modeId)
	{
		PositionContainer container = getRewardPosition(type, modeId, rewardId);
		
		if(_rewards.get(type).get(modeId) == null)
			_rewards.get(type).put(modeId, new EventRewards());
		
		EventRewards rewards = _rewards.get(type).get(modeId);
		RewardItem item = rewards.getItem(rewardId);
		
		rewards.removeItem(container.position, container.parameter, rewardId);

		removeFromDb(type, container.position, container.parameter, modeId, item.id, item.minAmmount, item.maxAmmount, item.chance);
	}
	
	public Map<Integer, RewardItem> getRewards(EventType type, int modeId, RewardPosition position, String parameter)
	{
		if(_rewards.get(type).get(modeId) == null)
			_rewards.get(type).put(modeId, new EventRewards());
		
		Map<Integer, RewardItem> map = _rewards.get(type).get(modeId).getRewards(position, parameter);
			
		if(map != null)
			return map;
		else
			return new FastMap<Integer, RewardItem>();
	}
	
	public RewardItem getReward(EventType type, int modeId, int rewardId)
	{
		if(_rewards.get(type).get(modeId) == null)
			_rewards.get(type).put(modeId, new EventRewards());
		
		return _rewards.get(type).get(modeId).getItem(rewardId);
	}
	
	public PositionContainer getRewardPosition(EventType type, int modeId, int rewardId)
	{
		if(_rewards.get(type).get(modeId) == null)
			_rewards.get(type).put(modeId, new EventRewards());
		
		for(Map.Entry<PositionContainer, Map<Integer, RewardItem>> e : _rewards.get(type).get(modeId)._rewards.entrySet())
		{
			for(int i : e.getValue().keySet())
			{
				if(i == rewardId)
					return e.getKey();
			}
		}
		return new PositionContainer(RewardPosition.None, null);
	}
	
	public Map<Integer, List<EventTeam>> rewardTeams(Map<EventTeam, Integer> teams, EventType event, int modeId, int minScore, final int halfRewardAfkTime, final int noRewardAfkTime)
	{
		count = 0;
		notEnoughtScore = 0;
		
		int totalCount = teams.size();
		
		EventTeam team;
		int score;
		
		Map<Integer, List<EventTeam>> scores = new FastMap<Integer, List<EventTeam>>();
		
		for(Entry<EventTeam, Integer> e : teams.entrySet())
		{
			team = e.getKey();
			score = e.getValue();
			
			if(!scores.containsKey(score))
				scores.put(score, new FastList<EventTeam>());
			
			scores.get(score).add(team);
		}
		
		int position = 1;
		int count;
		
		PositionContainer temp;
		for(Entry<Integer, List<EventTeam>> e : scores.entrySet())
		{
			score = e.getKey();
			count = e.getValue().size();
			
			if(position == 1) // winner
			{
				if(count == 1) // one winner
				{
					//***** REWARD WINNERS ******//
					temp = existsReward(event, modeId, RewardPosition.Numbered, "1");
					if(temp != null) // numbered reward exists
					{
						giveRewardsToTeams(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
					}
					else
					{
						temp = existsRangeReward(event, modeId, position);
						if(temp != null) // range reward exists
						{
							giveRewardsToTeams(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
						}
						else
						{
							temp = existsReward(event, modeId, RewardPosition.Winner, null);
							if(temp != null) // general winner exists
							{
								giveRewardsToTeams(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
							}
						}
					}
					//***** REWARD WINNERS END ******//
				}
				else // more winners
				{
					if(totalCount > count) // more than one winner
					{
						
						//***** REWARD WINNERS ******//
						temp = existsReward(event, modeId, RewardPosition.Numbered, "1");
						if(temp != null) // numbered reward exists
						{
							giveRewardsToTeams(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
						}
						else
						{
							temp = existsRangeReward(event, modeId, position);
							if(temp != null) // range reward exists
							{
								giveRewardsToTeams(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
							}
							else
							{
								temp = existsReward(event, modeId, RewardPosition.Winner, null);
								if(temp != null) // general winner exists
								{
									giveRewardsToTeams(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
								}
							}
						}
						//***** REWARD WINNERS END ******//
						
					}
					else // all are winners
					{
						
						//***** REWARD TIE ******//
						temp = existsReward(event, modeId, RewardPosition.Tie, null);
						if(temp != null)
						{
							giveRewardsToTeams(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
						}
						//***** REWARD TIE END ******//
						
					}
				}
			}
			else // loosers
			{
				//***** REWARD LOSERS ******//
				temp = existsReward(event, modeId, RewardPosition.Numbered, String.valueOf(position));
				if(temp != null)
				{
					giveRewardsToTeams(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
				}
				else
				{
					temp = existsRangeReward(event, modeId, position);
					if(temp != null)
					{
						giveRewardsToTeams(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
					}
					else
					{
						temp = existsReward(event, modeId, RewardPosition.Looser, null);
						if(temp != null)
						{
							giveRewardsToTeams(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
						}
					}
				}
				//***** REWARD LOSERS END ******//
				
			}
			
			position ++;
		}
		
		try
		{
			if(event.isRegularEvent())
			{
				AbstractMainEvent ev = EventManager.getInstance().getMainEvent(event);
				if(ev != null)
				{
					dump(ev.getPlayers(0).size());
				}
			}
		}
		catch (Exception e2)
		{
		}
		
		return scores;
	}
	
	private void dump(int total)
	{
		NexusLoader.debug(total + " was the count of players in the event.");
		NexusLoader.debug(count + " players were rewarded.");
		NexusLoader.debug(notEnoughtScore + " players were not rewarded because they didn't have enought score.");
		
		/**/ if(NexusLoader.detailedDebug) NexusLoader.detailedDebug(total + " was the count of players in the event.");
		/**/ if(NexusLoader.detailedDebug) NexusLoader.detailedDebug(count + " players were rewarded.");
		/**/ if(NexusLoader.detailedDebug) NexusLoader.detailedDebug(notEnoughtScore + " players were not rewarded because they didn't have enought score.");
	}
	
	public Map<Integer, List<PlayerEventInfo>> rewardPlayers(Map<PlayerEventInfo, Integer> players, EventType event, int modeId, int minScore, final int halfRewardAfkTime, final int noRewardAfkTime)
	{
		count = 0;
		notEnoughtScore = 0;
		
		int totalCount = players.size();
		
		PlayerEventInfo player;
		int score;
		
		Map<Integer, List<PlayerEventInfo>> scores = new FastMap<Integer, List<PlayerEventInfo>>();
		
		for(Entry<PlayerEventInfo, Integer> e : players.entrySet())
		{
			player = e.getKey();
			score = e.getValue();
			
			if(!scores.containsKey(score))
				scores.put(score, new FastList<PlayerEventInfo>());
			
			scores.get(score).add(player);
		}
		
		int position = 1;
		int count;
		
		PositionContainer temp;
		for(Entry<Integer, List<PlayerEventInfo>> e : scores.entrySet())
		{
			score = e.getKey();
			count = e.getValue().size();
			
			if(position == 1) // winner
			{
				if(count == 1) // one winner
				{
					
					//***** REWARD WINNERS ******//
					temp = existsReward(event, modeId, RewardPosition.Numbered, "1");
					if(temp != null) // numbered reward exists
					{
						giveRewardsToPlayers(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
					}
					else
					{
						temp = existsRangeReward(event, modeId, position);
						if(temp != null) // range reward exists
						{
							giveRewardsToPlayers(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
						}
						else
						{
							temp = existsReward(event, modeId, RewardPosition.Winner, null);
							if(temp != null) // general winner exists
							{
								giveRewardsToPlayers(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
							}
						}
					}
					//***** REWARD WINNERS END ******//
					
				}
				else // more winners
				{
					if(totalCount > count) // more than one winner
					{
						
						//***** REWARD WINNERS ******//
						temp = existsReward(event, modeId, RewardPosition.Numbered, "1");
						if(temp != null) // numbered reward exists
						{
							giveRewardsToPlayers(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
						}
						else
						{
							temp = existsRangeReward(event, modeId, position);
							if(temp != null) // range reward exists
							{
								giveRewardsToPlayers(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
							}
							else
							{
								temp = existsReward(event, modeId, RewardPosition.Winner, null);
								if(temp != null) // general winner exists
								{
									giveRewardsToPlayers(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
								}
							}
						}
						//***** REWARD WINNERS END ******//
						
					}
					else // all are winners
					{
						
						//***** REWARD TIE ******//
						temp = existsReward(event, modeId, RewardPosition.Tie, null);
						if(temp != null)
						{
							giveRewardsToPlayers(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
						}
						//***** REWARD TIE END ******//
						
					}
				}
			}
			else // loosers
			{
				
				//***** REWARD LOSERS ******//
				temp = existsReward(event, modeId, RewardPosition.Numbered, String.valueOf(position));
				if(temp != null)
				{
					giveRewardsToPlayers(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
				}
				else
				{
					temp = existsRangeReward(event, modeId, position);
					if(temp != null)
					{
						giveRewardsToPlayers(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
					}
					else
					{
						temp = existsReward(event, modeId, RewardPosition.Looser, null);
						if(temp != null)
						{
							giveRewardsToPlayers(temp, e.getValue(), event, modeId, minScore, halfRewardAfkTime, noRewardAfkTime);
						}
					}
				}
				//***** REWARD LOSERS END ******//
				
			}
			
			position ++;
		}
		
		return scores;
	}
	
	private int count = 0;
	private int notEnoughtScore = 0;
	
	private void giveRewardsToPlayers(PositionContainer container, List<PlayerEventInfo> players, EventType event, int modeId, int minScore, final int halfRewardAfkTime, final int noRewardAfkTime)
	{
		for(PlayerEventInfo player : players)
		{
			if(player.isOnline())
			{
				//TODO implement minKills/minScore
				
				// temp fix:
				if(player.getEventData().getScore() >= minScore)
				{
					count ++;
					rewardPlayer(event, modeId, player, container.position, container.parameter, player.getTotalTimeAfk(), halfRewardAfkTime, noRewardAfkTime);
				}
				else
				{
					notEnoughtScore ++;
					
					if(minScore > 0 && player.getScore() < minScore)
						player.sendMessage(LanguageEngine.getMsg("event_notEnoughtScore", minScore));
					
					/*if(minKills > 0 && player.getKills() < minKills)
						player.sendMessage("You need at least " + minKills + " kills to receive a reward.");*/
				}
			}
			else
			{
				NexusLoader.debug("trying to reward player " + player.getPlayersName() + " (player) which is not online()", Level.WARNING);
			}
		}
	}
	
	private void giveRewardsToTeams(PositionContainer container, List<EventTeam> teams, EventType event, int modeId, int minScore, final int halfRewardAfkTime, final int noRewardAfkTime)
	{
		for(EventTeam team : teams)
		{
			for(PlayerEventInfo player : team.getPlayers())
			{
				if(player.isOnline())
				{
					//TODO implement minKills/minScore
					
					if(player.getEventData().getScore() >= minScore)
					{
						count ++;
						rewardPlayer(event, modeId, player, container.position, container.parameter, player.getTotalTimeAfk(), halfRewardAfkTime, noRewardAfkTime);
					}
					else
					{
						notEnoughtScore ++;
						
						if(minScore > 0 && player.getScore() < minScore)
							player.sendMessage(LanguageEngine.getMsg("event_notEnoughtScore", minScore));
					}
				}
				else
				{
					NexusLoader.debug("trying to reward player " + player.getPlayersName() + " (team) which is not online()", Level.WARNING);
				}
			}
		}
	}
	
	private PositionContainer existsReward(EventType event, int modeId, RewardPosition pos, String parameter)
	{
		if(_rewards.get(event).get(modeId) == null)
			return null;
		
		PositionContainer c = _rewards.get(event).get(modeId).getContainer(pos, parameter);
		if(c == null || _rewards.get(event).get(modeId).getAllRewards().get(c).isEmpty())
		{
			return null;
		}
		else return c;
	}
	
	private PositionContainer existsRangeReward(EventType event, int modeId, int position)
	{
		if(_rewards.get(event).get(modeId) == null)
			return null;
		
		int from, to;
		for(Entry<PositionContainer, Map<Integer, RewardItem>> e : _rewards.get(event).get(modeId).getAllRewards().entrySet())
		{
			if(e.getValue() == null || e.getValue().isEmpty())
				continue;
			
			if(e.getKey().position.posType != null && e.getKey().position.posType == PositionType.Range)
			{
				from = Integer.parseInt(e.getKey().parameter.split("-")[0]);
				to = Integer.parseInt(e.getKey().parameter.split("-")[1]);
				
				if(position >= from && position <= to)
				{
					return e.getKey();
				}
				else continue;
			}
		}
		
		return null;
	}
	
	public boolean rewardPlayer(EventType event, int modeId, PlayerEventInfo player, RewardPosition position, String parameter, int afkTime, final int halfRewardAfkTime, final int noRewardAfkTime)
	{
		if(player == null)
			return false;
		
		if(_rewards.get(event).get(modeId) == null)
			_rewards.get(event).put(modeId, new EventRewards());
		
		if(_rewards.get(event).get(modeId).getRewards(position, parameter) == null)
		{
			//NexusLoader.debug(event.getAltTitle() + " Reward not exist: " + position.posType.toString() + ", " + parameter, Level.WARNING);
			return false;
		}
		
		if(noRewardAfkTime > 0 && afkTime >= noRewardAfkTime)
		{
			player.sendMessage("You receive no reward because you were afk too much.");
			return false;
		}
		else if(halfRewardAfkTime > 0 && afkTime >= halfRewardAfkTime)
			player.sendMessage("You receive half reward because you were afk too much.");
		
		boolean given = false;
		
		int ammount;
		for(RewardItem item : _rewards.get(event).get(modeId).getRewards(position, parameter).values())
		{
			ammount = item.getAmmount(player);
			if(ammount > 0)
			{
				if(ammount > 1)
				{
					if(halfRewardAfkTime > 0 && afkTime >= halfRewardAfkTime)
					{
						ammount /= 2;
					}
				}
				
				if(item.id == -1)
					player.addExpAndSp(ammount, 0);
				else if(item.id == -2)
					player.addExpAndSp(0, ammount);
				else if(item.id == -3)
					player.setFame(player.getFame() + ammount);
				else
					player.addItem(item.id, ammount, true);
				
				given = true;
			}
		}
		
		return given;
	}

	public static EventRewardSystem getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public class PositionContainer
	{
		public RewardPosition position;
		public String parameter;
		public boolean rewarded;
		
		PositionContainer(RewardPosition position, String parameter)
		{
			this.position = position;
			this.parameter = parameter;
		}
		
		public void setRewarded(boolean b) { rewarded = b; }
		public boolean isRewarded() { return rewarded; }
	}

	public class EventRewards
	{
		private int _lastId = 0;
		private Map<PositionContainer, Map<Integer, RewardItem>> _rewards;
		
		public EventRewards()
		{
			_rewards = new FastMap<PositionContainer, Map<Integer, RewardItem>>();
		}
		
		public PositionContainer getOrCreateContainer(RewardPosition position, String posParameter)
		{
			PositionContainer container = null;
			
			container = getContainer(position, posParameter);
			
			if(container == null)
				container = new PositionContainer(position, posParameter);
			
			// create the list of rewards for this Position type and parameter
			if(!_rewards.containsKey(container))
			{
				_rewards.put(container, new FastMap<Integer, RewardItem>());
			}
			
			return container;
		}
		
		public int addItem(RewardPosition position, String posParameter, int id, int minAmmount, int maxAmmount, int chance)
		{
			if(position == null)
			{
				NexusLoader.debug("Null RewardPosition for item ID " + id + ", minAmmount " + minAmmount + " maxAmmount " + maxAmmount + " chance " + chance, Level.WARNING);
				return _lastId ++;
			}
			
			if("".equals(posParameter))
				posParameter = null;
			
			PositionContainer container = getOrCreateContainer(position, posParameter);
			
			_lastId++;
			RewardItem item = new RewardItem(id, minAmmount, maxAmmount, chance, 0, 0);

			_rewards.get(container).put(_lastId, item);
			
			return _lastId;
		}
		
		public PositionContainer getContainer(RewardPosition position, String parameter)
		{
			for(PositionContainer ps : _rewards.keySet())
			{
				if(ps.position == null)
					continue;
				
				if(ps.position.toString().equals(position.toString()))
				{
					if((parameter == null || parameter.equals("null") || parameter.equals(ps.parameter)))
					{
						return ps;
					}
				}
				
			}
			return null;
		}
		
		public void removeItem(RewardPosition position, String parameter, int rewardId)
		{
			PositionContainer ps = getContainer(position, parameter);
			
			if(ps != null && _rewards.containsKey(ps))
				_rewards.get(ps).remove(rewardId);
		}
		
		public Map<Integer, RewardItem> getRewards(RewardPosition position, String parameter)
		{
			PositionContainer ps = getContainer(position, parameter);
			if(ps != null)
				return _rewards.get(ps);
			else return null;
		}
		
		public Map<PositionContainer, Map<Integer, RewardItem>> getAllRewards()
		{
			return _rewards;
		}
		
		public RewardItem getItem(int rewardId)
		{
			for(Map<Integer, RewardItem> i : _rewards.values())
			{
				for(Map.Entry<Integer, RewardItem> e : i.entrySet())
				{
					if(e.getKey() == rewardId)
						return e.getValue();
				}
			}
			return null;
		}
	}
	
	public class RewardItem
	{
		public int id;
		public int minAmmount;
		public int maxAmmount;
		public int chance;
		public int pvpRequired;
		public int levelRequired;
		
		public RewardItem(int id, int minAmmount, int maxAmmount, int chance, int pvpRequired, int levelRequired)
		{
			this.id = id;
			this.minAmmount = minAmmount;
			this.maxAmmount = maxAmmount;
			this.chance = chance;
			this.pvpRequired = pvpRequired;
			this.levelRequired = levelRequired;
		}
		
		public int getAmmount(PlayerEventInfo player)
		{
			if(CallBack.getInstance().getOut().random(100) < chance)
				return CallBack.getInstance().getOut().random(minAmmount, maxAmmount);
			else
			{
				NexusLoader.debug("chance check for reward failed for player " + player.getPlayersName() + ", reward item " + id);
				return 0;
			}
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EventRewardSystem _instance = new EventRewardSystem();
	}
}
