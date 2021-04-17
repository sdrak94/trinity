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

/**
 * @author godson
 */

package net.sf.l2j.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.templates.StatsSet;

public class Hero
{
private static Logger _log = Logger.getLogger(Hero.class.getName());

private static final String GET_HEROES = "SELECT heroes.charId, "
	+ "characters.char_name, heroes.class_id, heroes.count, heroes.played "
	+"FROM heroes, characters WHERE characters.charId = heroes.charId "
	+"AND heroes.played = 1";
private static final String GET_ALL_HEROES = "SELECT heroes.charId, "
	+ "characters.char_name, heroes.class_id, heroes.count, heroes.played "
	+"FROM heroes, characters WHERE characters.charId = heroes.charId";
private static final String UPDATE_ALL = "UPDATE heroes SET played = 0";
private static final String INSERT_HERO = "INSERT INTO heroes VALUES (?,?,?,?,?)";
private static final String UPDATE_HERO = "UPDATE heroes SET count = ?, "
	+"played = ?" + " WHERE charId = ?";
private static final String GET_CLAN_ALLY = "SELECT characters.clanid "
	+"AS clanid, coalesce(clan_data.ally_Id, 0) AS allyId FROM characters "
	+"LEFT JOIN clan_data ON clan_data.clan_id = characters.clanid "
	+"WHERE characters.charId = ?";
private static final String GET_CLAN_NAME = "SELECT clan_name FROM clan_data "
	+"WHERE clan_id = (SELECT clanid FROM characters WHERE char_name = ?)";
// delete hero items
private static final String DELETE_ITEMS = "DELETE FROM items WHERE item_id IN "
	+ "(6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621, 9388, 9389, 9390) "
	+ "AND owner_id NOT IN (SELECT charId FROM characters WHERE accesslevel > 0)";

public static Map<Integer, StatsSet> _heroes;
public static Map<Integer, StatsSet> _completeHeroes;

public static final String COUNT = "count";
public static final String PLAYED = "played";
public static final String CLAN_NAME = "clan_name";
public static final String CLAN_CREST = "clan_crest";
public static final String ALLY_NAME = "ally_name";
public static final String ALLY_CREST = "ally_crest";

public static Hero getInstance()
{
	return SingletonHolder._instance;
}

private Hero()
{
	init();
}

private void init()
{
	_heroes = new FastMap<Integer, StatsSet>();
	_completeHeroes = new FastMap<Integer, StatsSet>();
	
	Connection con = null;
	Connection con2 = null;
	
	PreparedStatement statement;
	PreparedStatement statement2;
	
	ResultSet rset;
	ResultSet rset2;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		con2 = L2DatabaseFactory.getInstance().getConnection();
		statement = con.prepareStatement(GET_HEROES);
		rset = statement.executeQuery();
		
		while (rset.next())
		{
			StatsSet hero = new StatsSet();
			int charId = rset.getInt(Olympiad.CHAR_ID);
			hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
			hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
			hero.set(COUNT, rset.getInt(COUNT));
			hero.set(PLAYED, rset.getInt(PLAYED));
			
			statement2 = con2.prepareStatement(GET_CLAN_ALLY);
			statement2.setInt(1, charId);
			rset2 = statement2.executeQuery();
			
			if (rset2.next())
			{
				int clanId = rset2.getInt("clanid");
				int allyId = rset2.getInt("allyId");
				
				String clanName = "";
				String allyName = "";
				int clanCrest = 0;
				int allyCrest = 0;
				
				if (clanId > 0)
				{
					clanName = ClanTable.getInstance().getClan(clanId).getName();
					clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
					
					if (allyId > 0)
					{
						allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
						allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
					}
				}
				
				hero.set(CLAN_CREST, clanCrest);
				hero.set(CLAN_NAME, clanName);
				hero.set(ALLY_CREST, allyCrest);
				hero.set(ALLY_NAME, allyName);
			}
			
			rset2.close();
			statement2.close();
			
			_heroes.put(charId, hero);
		}
		
		rset.close();
		statement.close();
		
		statement = con.prepareStatement(GET_ALL_HEROES);
		rset = statement.executeQuery();
		
		while (rset.next())
		{
			StatsSet hero = new StatsSet();
			int charId = rset.getInt(Olympiad.CHAR_ID);
			hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
			hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
			hero.set(COUNT, rset.getInt(COUNT));
			hero.set(PLAYED, rset.getInt(PLAYED));
			
			statement2 = con2.prepareStatement(GET_CLAN_ALLY);
			statement2.setInt(1, charId);
			rset2 = statement2.executeQuery();
			
			if (rset2.next())
			{
				int clanId = rset2.getInt("clanid");
				int allyId = rset2.getInt("allyId");
				
				String clanName = "";
				String allyName = "";
				int clanCrest = 0;
				int allyCrest = 0;
				
				if (clanId > 0)
				{
					clanName = ClanTable.getInstance().getClan(clanId).getName();
					clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
					
					if (allyId > 0)
					{
						allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
						allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
					}
				}
				
				hero.set(CLAN_CREST, clanCrest);
				hero.set(CLAN_NAME, clanName);
				hero.set(ALLY_CREST, allyCrest);
				hero.set(ALLY_NAME, allyName);
			}
			
			rset2.close();
			statement2.close();
			
			_completeHeroes.put(charId, hero);
		}
		
		rset.close();
		statement.close();
	}
	catch (SQLException e)
	{
		_log.warning("Hero System: Couldnt load Heroes");
		if (Config.DEBUG)
			e.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
			con2.close();
		}
		catch (Exception e)
		{
		}
	}
	
	_log.info("Hero System: Loaded " + _heroes.size() + " Heroes.");
	_log.info("Hero System: Loaded " + _completeHeroes.size() + " all time Heroes.");
}

public Map<Integer, StatsSet> getHeroes()
{
	return _heroes;
}

public synchronized void computeNewHeroes()
{
	final FastList<StatsSet> newHeroes = Olympiad._heroesToBe;
	
	updateHeroes(true);
	clearHeroItemDeletion();
	
	if (!_heroes.isEmpty())
	{
		for (StatsSet hero : _heroes.values())
		{
			String name = hero.getString(Olympiad.CHAR_NAME);
			
			L2PcInstance player = L2World.getInstance().getPlayer(name);
			
			if (player == null)
				continue;
			
			final int heroCharId = player.getObjectId();
			
			if (heroCharId <= 0)
			{
				_log.severe("OLYMPIAD: " + name + " doesn't have a heroCharId!!");
				continue;
			}
			
			if (!newHeroes.isEmpty())
			{
				boolean next = false;
				
				for (StatsSet hero1 : newHeroes)
				{
					final int newheroCharId = hero1.getInteger(Olympiad.CHAR_ID);
					
					if (newheroCharId <= 0)
					{
						_log.severe("OLYMPIAD: " + name + " doesn't have a newheroCharId!!");
						continue;
					}
					
					if (heroCharId == newheroCharId)
					{
						player._previousMonthOlympiadGamesPlayed = Olympiad.getCompetitionDone(newheroCharId);
						
						if (player.canUseHeroItems())
						{
							player._heroWpnDelCount = 0;
							next = true;
						}
						
						break;
					}
				}
				
				if (next)
					continue;
			}
			
			try
			{
				player.setHero(false);
				player._heroWpnDelCount = 0;
				
				for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
				{
					L2ItemInstance equippedItem = player.getInventory().getPaperdollItem(i);
					if (equippedItem != null && equippedItem.isHeroItem())
						player.getInventory().unEquipItemInSlotAndRecord(i);
				}
				
				for (L2ItemInstance item : player.getInventory().getAvailableItems(false, true))
				{
					if (item != null && item.isHeroItem())
					{
						player.destroyItem("Hero", item, null, true);
						InventoryUpdate iu = new InventoryUpdate();
						iu.addRemovedItem(item);
						player.sendPacket(iu);
					}
				}
				
				player.broadcastUserInfo();
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
	
	if (newHeroes.isEmpty())
	{
		_heroes.clear();
		return;
	}
	
	Map<Integer, StatsSet> heroes = new FastMap<Integer, StatsSet>();
	
	for (StatsSet hero : newHeroes)
	{
		int charId = hero.getInteger(Olympiad.CHAR_ID);
		
		if (_completeHeroes != null && _completeHeroes.containsKey(charId))
		{
			StatsSet oldHero = _completeHeroes.get(charId);
			int count = oldHero.getInteger(COUNT);
			oldHero.set(COUNT, count + 1);
			oldHero.set(PLAYED, 1);
			oldHero.set(Olympiad.COMP_DONE, Olympiad.getCompetitionDone(charId));
			
			heroes.put(charId, oldHero);
		}
		else
		{
			StatsSet newHero = new StatsSet();
			newHero.set(Olympiad.CHAR_NAME, hero.getString(Olympiad.CHAR_NAME));
			newHero.set(Olympiad.CLASS_ID, hero.getInteger(Olympiad.CLASS_ID));
			newHero.set(COUNT, 1);
			newHero.set(PLAYED, 1);
			newHero.set(Olympiad.COMP_DONE, Olympiad.getCompetitionDone(charId));
			
			heroes.put(charId, newHero);
		}
	}
	
	deleteItemsInDb();
	
	_heroes.clear();
	_heroes.putAll(heroes);
	heroes.clear();
	
	updateHeroes(false);
	
	for (StatsSet hero : _heroes.values())
	{
		String name = hero.getString(Olympiad.CHAR_NAME);
		
		L2PcInstance player = L2World.getInstance().getPlayer(name);
		
		if (player != null)
		{
			player.setHero(true);
			player.getCounters().timesHero++;
			player._previousMonthOlympiadGamesPlayed = Olympiad.getCompetitionDone(player.getObjectId());
			
			L2Clan clan = player.getClan();
			if (clan != null)
			{
				clan.setReputationScore(clan.getReputationScore() + Config.HERO_POINTS, true);
				SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_MEMBER_C1_BECAME_HERO_AND_GAINED_S2_REPUTATION_POINTS);
				sm.addString(name);
				sm.addNumber(Config.HERO_POINTS);
				clan.broadcastToOnlineMembers(sm);
			}
			player.sendPacket(new UserInfo(player));
			player.sendPacket(new ExBrExtraUserInfo(player));
			player.broadcastUserInfo();
			
		}
		else
		{
			Connection con = null;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(GET_CLAN_NAME);
				statement.setString(1, name);
				ResultSet rset = statement.executeQuery();
				if (rset.next())
				{
					String clanName = rset.getString("clan_name");
					if (clanName != null)
					{
						L2Clan clan = ClanTable.getInstance().getClanByName(clanName);
						if (clan != null)
						{
							clan.setReputationScore(clan.getReputationScore() + Config.HERO_POINTS, true);
							SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_MEMBER_C1_BECAME_HERO_AND_GAINED_S2_REPUTATION_POINTS);
							sm.addString(name);
							sm.addNumber(Config.HERO_POINTS);
							clan.broadcastToOnlineMembers(sm);
						}
					}
				}
				
				rset.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("could not get clan name of " + name + ": " + e);
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
			
//			PlayerCounters pc = new PlayerCounters(hero.getInteger(Olympiad.CHAR_ID));
//			pc.load();
//			pc.timesHero++;
//			pc.save();
		}
	}
}

public void updateHeroes(boolean setDefault)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		if (setDefault)
		{
			PreparedStatement statement = con.prepareStatement(UPDATE_ALL);
			statement.execute();
			statement.close();
		}
		else
		{
			PreparedStatement statement;
			
			for (Integer heroId : _heroes.keySet())
			{
				StatsSet hero = _heroes.get(heroId);
				
				if (_completeHeroes == null || !_completeHeroes.containsKey(heroId))
				{
					statement = con.prepareStatement(INSERT_HERO);
					statement.setInt(1, heroId);
					statement.setString(2, hero.getString(Olympiad.CHAR_NAME));
					statement.setInt(3, hero.getInteger(Olympiad.CLASS_ID));
					statement.setInt(4, hero.getInteger(COUNT));
					statement.setInt(5, hero.getInteger(PLAYED));
					statement.execute();
					
					Connection con2 = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement2 = con2.prepareStatement(GET_CLAN_ALLY);
					statement2.setInt(1, heroId);
					ResultSet rset2 = statement2.executeQuery();
					
					if (rset2.next())
					{
						int clanId = rset2.getInt("clanid");
						int allyId = rset2.getInt("allyId");
						
						String clanName = "";
						String allyName = "";
						int clanCrest = 0;
						int allyCrest = 0;
						
						if (clanId > 0)
						{
							clanName = ClanTable.getInstance().getClan(clanId).getName();
							clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
							
							if (allyId > 0)
							{
								allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
								allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
							}
						}
						
						hero.set(CLAN_CREST, clanCrest);
						hero.set(CLAN_NAME, clanName);
						hero.set(ALLY_CREST, allyCrest);
						hero.set(ALLY_NAME, allyName);
					}
					
					rset2.close();
					statement2.close();
					con2.close();
					
					_heroes.remove(heroId);
					_heroes.put(heroId, hero);
					
					_completeHeroes.put(heroId, hero);
				}
				else
				{
					statement = con.prepareStatement(UPDATE_HERO);
					statement.setInt(1, hero.getInteger(COUNT));
					statement.setInt(2, hero.getInteger(PLAYED));
					statement.setInt(3, heroId);
					statement.execute();
				}
				
				statement.close();
			}
		}
	}
	catch (SQLException e)
	{
		_log.warning("Hero System: Couldnt update Heroes");
		if (Config.DEBUG)
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
			e.printStackTrace();
		}
	}
}

private void deleteItemsInDb()
{
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement(DELETE_ITEMS);
		statement.execute();
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
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}

private void clearHeroItemDeletion()
{
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("update characters set heroWpnDel=0");
		statement.execute();
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
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final Hero _instance = new Hero();
}
}
