package luna.museum;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2MuseumStatueInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class MuseumManager
{
	public static final Logger _log = LoggerFactory.getLogger(MuseumManager.class);
	private final HashMap<Integer, String> _categoryNames;
	private final HashMap<Integer, MuseumCategory> _categories;
	private final HashMap<Integer, ArrayList<MuseumCategory>> _categoriesByCategoryId;
	private final HashMap<Integer, ArrayList<Integer>> _playersWithReward;
	private int refreshTotal;
	
	public MuseumManager()
	{
		refreshTotal = 3600;
		_categoryNames = new HashMap<>();
		_categories = new HashMap<>();
		_categoriesByCategoryId = new HashMap<>();
		_playersWithReward = new HashMap<>(); 
		loadCategories();
		final long monthlyUpdate = Math.max(100L, (GlobalVariablesManager.getInstance().hasVariable("museum_monthly") ? GlobalVariablesManager.getInstance().getLong("museum_monthly") : 0L) - System.currentTimeMillis());
		final long weeklyUpdate = Math.max(100L, (GlobalVariablesManager.getInstance().hasVariable("museum_weekly") ? GlobalVariablesManager.getInstance().getLong("museum_weekly") : 0L) - System.currentTimeMillis());
		final long dailyUpdate = Math.max(100L, (GlobalVariablesManager.getInstance().hasVariable("museum_daily") ? GlobalVariablesManager.getInstance().getLong("museum_daily") : 0L) - System.currentTimeMillis());
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new UpdateStats(RefreshTime.Total), 100L, refreshTotal * 1000);
		ThreadPoolManager.getInstance().scheduleGeneral(new UpdateStats(RefreshTime.Monthly), monthlyUpdate);
		ThreadPoolManager.getInstance().scheduleGeneral(new UpdateStats(RefreshTime.Weekly), weeklyUpdate);
		ThreadPoolManager.getInstance().scheduleGeneral(new UpdateStats(RefreshTime.Daily), dailyUpdate);
	}
	
	public void giveRewards()
	{
		final ArrayList<Integer> withReward = new ArrayList<>();
		for (final Map.Entry<Integer, ArrayList<Integer>> entry : _playersWithReward.entrySet())
		{
			final L2PcInstance player = L2World.getInstance().getPlayer(entry.getKey());
			if (player != null)
			{
				if (player.isOnline() == 0)
				{
					continue;
				}
				final ArrayList<Integer> cats = entry.getValue();
				for (final int catId : cats)
				{
					if (!_categories.containsKey(catId))
					{
						withReward.add(entry.getKey());
					}
					else
					{
						final MuseumCategory cat = _categories.get(catId);
						if (cat == null)
						{
							withReward.add(entry.getKey());
						}
						else
						{
							for (final MuseumReward reward : cat.getRewards())
							{
								reward.giveReward(player);
								withReward.add(entry.getKey());
							}
						}
					}
				}
			}
		}
		for (final int i : withReward)
		{
			_playersWithReward.remove(i);
		}
		if (_playersWithReward.size() == 0)
		{
			return;
		}
	}
	
	public void giveReward(final L2PcInstance player)
	{
		if (!_playersWithReward.containsKey(player.getObjectId()))
		{
			return;
		}
		final ArrayList<Integer> cats = _playersWithReward.get(player.getObjectId());
		if (cats.size() < 1)
		{
			_playersWithReward.remove(player.getObjectId());
			return;
		}
		for (final int catId : cats)
		{
			if (!_categories.containsKey(catId))
			{
				continue;
			}
			final MuseumCategory cat = _categories.get(catId);
			for (final MuseumReward reward : cat.getRewards())
			{
				reward.giveReward(player);
			}
		}
		_playersWithReward.remove(player.getObjectId());
	}
	
	public void restoreLastTops(final RefreshTime time)
	{
		for (final MuseumCategory cat : getAllCategories().values())
		{
			int i = 1;
			if (!cat.getRefreshTime().equals(time) && !time.equals(RefreshTime.Total))
			{
				continue;
			}
			cat.getAllStatuePlayers().clear();
			try (final Connection con = L2DatabaseFactory.getInstance().getConnection();
				final PreparedStatement statement = con.prepareStatement("SELECT * FROM museum_last_statistics as mls INNER JOIN museum_statistics as ms ON ms.objectId=mls.objectId WHERE mls.category = ? AND mls.category = ms.category AND mls.count > 0 AND mls.timer = '" + cat.getRefreshTime().name().toLowerCase() + "' ORDER BY mls.count DESC LIMIT 5"))
			{
				statement.setString(1, cat.getType());
				try (final ResultSet rset = statement.executeQuery())
				{
					while (rset.next())
					{
						final int objectId = rset.getInt("objectId");
						final String name = rset.getString("name");
						final long count = rset.getLong("count");
						cat.getAllStatuePlayers().put(i, new TopPlayer(objectId, name, count));
						if (i == 1)
						{
							spawnStatue(cat);
						}
						++i;
					}
				}
			}
			catch (Exception e)
			{
				_log.error("Failed loading character museum data.", e);
			}
		}
	}
	
	public void spawnStatue(final MuseumCategory cat)
	{
		for (final L2MuseumStatueInstance statue : cat.getAllSpawnedStatues())
		{
			statue.deleteMe();
		}
		cat.getAllSpawnedStatues().clear();
		if (cat.getAllStatuePlayers().size() > 0)
		{
			final TopPlayer player = cat.getAllStatuePlayers().get(1);
			for (final Location loc : cat.getStatueSpawns())
			{
				final L2MuseumStatueInstance statue2 = new L2MuseumStatueInstance(NpcTable.getInstance().getTemplate(30001), player.getObjectId(), (cat.getCategoryId() * 256) + cat.getTypeId());
				statue2.setXYZ(loc.getX(), loc.getY(), loc.getZ());
				statue2.setHeading(loc.getHeading());
				statue2.spawnMe();
				cat.getAllSpawnedStatues().add(statue2);
			}
		}
	}
	
	public void cleanLastTops(final RefreshTime time)
	{
		try (final Connection con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("DELETE FROM museum_last_statistics WHERE timer='" + time.name().toLowerCase() + "'"))
		{
			statement.execute();
		}
		catch (Exception e)
		{
			_log.warn("Could not store char museum data: " + e.getMessage(), e);
		}
	}
	
	public void refreshTopsFromDatabase(final RefreshTime time)
	{
		_playersWithReward.clear();
		try (final Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			for (final MuseumCategory cat : _categories.values())
			{
				if (!cat.getRefreshTime().equals(time) && !time.equals(RefreshTime.Total))
				{
					continue;
				}
				cat.getAllTops().clear();
				cat.getAllTotalTops().clear();
				cat.getAllStatuePlayers().clear();
				int i = 1;
				int h = 1;
				try (
					final PreparedStatement statement = con.prepareStatement("SELECT * FROM museum_statistics WHERE category = ? AND " + cat.getRefreshTime().name().toLowerCase() + "_count > 0 ORDER BY " + cat.getRefreshTime().name().toLowerCase() + "_count DESC LIMIT " + (cat.getRefreshTime().equals(RefreshTime.Total) ? 20 : 10)))
				{
					statement.setString(1, cat.getType());
					try (final ResultSet rset = statement.executeQuery())
					{
						while (rset.next())
						{
							final int objectId = rset.getInt("objectId");
							final String name = rset.getString("name");
							final long count = rset.getLong(cat.getRefreshTime().name().toLowerCase() + "_count");
							final boolean hasReward = rset.getBoolean("hasReward");
							if (hasReward)
							{
								if (!_playersWithReward.containsKey(objectId))
								{
									_playersWithReward.put(objectId, new ArrayList<Integer>());
								}
								_playersWithReward.get(objectId).add((cat.getCategoryId() * 256) + cat.getTypeId());
							}
							if (cat.getRefreshTime().equals(RefreshTime.Total))
							{
								cat.getAllTotalTops().put(i, new TopPlayer(objectId, name, count));
							}
							else
							{
								cat.getAllTops().put(i, new TopPlayer(objectId, name, count));
							}
							if ((i < 6) && time.equals(cat.getRefreshTime()))
							{
								try (final PreparedStatement stat = con.prepareStatement("REPLACE museum_last_statistics SET objectId=" + objectId + ", name='" + name + "', category='" + cat.getType() + "', count=" + count + ", timer='" + time.name().toLowerCase() + "';"))
								{
									stat.execute();
								}
								if (i == 1)
								{
									try (final PreparedStatement stat = con.prepareStatement("UPDATE museum_statistics SET hasReward = 1 WHERE objectId = " + objectId + " AND category = '" + cat.getType() + "'"))
									{
										stat.execute();
									}
									if (!_playersWithReward.containsKey(objectId))
									{
										_playersWithReward.put(objectId, new ArrayList<Integer>());
									}
									_playersWithReward.get(objectId).add((cat.getCategoryId() * 256) + cat.getTypeId());
								}
							}
							++i;
						}
					}
				}
				if (cat.getRefreshTime().equals(RefreshTime.Total))
				{
					continue;
				}
				try (final PreparedStatement statement = con.prepareStatement("SELECT * FROM museum_statistics WHERE category = ? AND total_count > 0 ORDER BY total_count DESC LIMIT 10"))
				{
					statement.setString(1, cat.getType());
					try (final ResultSet rset = statement.executeQuery())
					{
						while (rset.next())
						{
							final int objectId = rset.getInt("objectId");
							final String name = rset.getString("name");
							final long count = rset.getLong("total_count");
							cat.getAllTotalTops().put(h, new TopPlayer(objectId, name, count));
							++h;
						}
					}
				}
			}
			if (!time.equals(RefreshTime.Total))
			{
				try (final PreparedStatement statement2 = con.prepareStatement("UPDATE museum_statistics SET " + time.name().toLowerCase() + "_count = 0"))
				{
					statement2.execute();
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Could not store char museum data: " + e.getMessage(), e);
		}
		restoreLastTops(time);
		giveRewards();
	}
	
	public void loadCategories()
	{
		_log.info(this.getClass().getSimpleName() + ": Initializing");
		_categoryNames.clear();
		_categories.clear();
		_categoriesByCategoryId.clear();
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		final File file = new File(Config.DATAPACK_ROOT, "data/xml/sunrise/MuseumCategories.xml");
		Document doc = null;
		if (file.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(file);
			}
			catch (Exception e)
			{
				_log.warn("Could not parse MuseumCategories.xml file: " + e.getMessage(), e);
				return;
			}
			int categoryId = 0;
			final Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("set"))
				{
					final String name = d.getAttributes().getNamedItem("name").getNodeValue();
					final String val = d.getAttributes().getNamedItem("val").getNodeValue();
					if (name.equalsIgnoreCase("refreshAllStatisticsIn"))
					{
						refreshTotal = Integer.parseInt(val);
					}
				}
				if (d.getNodeName().equalsIgnoreCase("category"))
				{
					final ArrayList<MuseumCategory> list = new ArrayList<>();
					final String categoryName = d.getAttributes().getNamedItem("name").getNodeValue();
					int typeId = 0;
					for (Node h = d.getFirstChild(); h != null; h = h.getNextSibling())
					{
						if (h.getNodeName().equalsIgnoreCase("type"))
						{
							final String typeName = h.getAttributes().getNamedItem("name").getNodeValue();
							final String type = h.getAttributes().getNamedItem("type").getNodeValue();
							final String refreshTime = h.getAttributes().getNamedItem("refreshTime").getNodeValue();
							boolean timer = false;
							if (h.getAttributes().getNamedItem("timer") != null)
							{
								timer = Boolean.parseBoolean(h.getAttributes().getNamedItem("timer").getNodeValue());
							}
							String additionalText = "";
							if (h.getAttributes().getNamedItem("additionalText") != null)
							{
								additionalText = h.getAttributes().getNamedItem("additionalText").getNodeValue();
							}
							final ArrayList<Location> statueSpawns = new ArrayList<>();
							final ArrayList<MuseumReward> rewards = new ArrayList<>();
							int rewardId = 0;
							for (Node a = h.getFirstChild(); a != null; a = a.getNextSibling())
							{
								if (a.getNodeName().equalsIgnoreCase("spawn"))
								{
									final int x = Integer.parseInt(a.getAttributes().getNamedItem("x").getNodeValue());
									final int y = Integer.parseInt(a.getAttributes().getNamedItem("y").getNodeValue());
									final int z = Integer.parseInt(a.getAttributes().getNamedItem("z").getNodeValue());
									final int heading = (a.getAttributes().getNamedItem("heading") != null) ? Integer.parseInt(a.getAttributes().getNamedItem("heading").getNodeValue()) : 0;
									statueSpawns.add(new Location(x, y, z, heading));
								}
								if (a.getNodeName().equalsIgnoreCase("reward"))
								{
									String rewardType = "";
									int itemId = 0;
									int minCount = 0;
									int maxCount = 0;
									double chance = 0.0;
									if (a.getAttributes().getNamedItem("type") != null)
									{
										rewardType = a.getAttributes().getNamedItem("type").getNodeValue();
									}
									if (a.getAttributes().getNamedItem("id") != null)
									{
										itemId = Integer.parseInt(a.getAttributes().getNamedItem("id").getNodeValue());
									}
									if (a.getAttributes().getNamedItem("min") != null)
									{
										minCount = Integer.parseInt(a.getAttributes().getNamedItem("min").getNodeValue());
									}
									if (a.getAttributes().getNamedItem("max") != null)
									{
										maxCount = Integer.parseInt(a.getAttributes().getNamedItem("max").getNodeValue());
									}
									if (a.getAttributes().getNamedItem("chance") != null)
									{
										chance = Double.parseDouble(a.getAttributes().getNamedItem("chance").getNodeValue());
									}
									rewards.add(new MuseumReward(rewardId, rewardType, itemId, minCount, maxCount, chance));
									++rewardId;
								}
							}
							final int key = (categoryId * 256) + typeId;
							final MuseumCategory category = new MuseumCategory(categoryId, typeId, categoryName, typeName, type, refreshTime, timer, additionalText, statueSpawns, rewards);
							list.add(category);
							_categories.put(key, category);
							++typeId;
						}
					}
					_categoriesByCategoryId.put(categoryId, list);
					_categoryNames.put(categoryId, categoryName);
					++categoryId;
				}
			}
		}
		_log.info(this.getClass().getSimpleName() + ": Successfully loaded " + _categoryNames.size() + " categories and " + _categories.size() + " post categories.");
	}
	
	public HashMap<Integer, String> getAllCategoryNames()
	{
		return _categoryNames;
	}
	
	public HashMap<Integer, MuseumCategory> getAllCategories()
	{
		return _categories;
	}
	
	public ArrayList<MuseumCategory> getAllCategoriesByCategoryId(final int id)
	{
		if (_categoriesByCategoryId.containsKey(id))
		{
			return _categoriesByCategoryId.get(id);
		}
		return null;
	}
	
	public void restoreDataForChar(final L2PcInstance player)
	{
		final HashMap<String, long[]> data = new HashMap<>();
		try (final Connection con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT * FROM museum_statistics WHERE objectId = ?"))
		{
			statement.setInt(1, player.getObjectId());
			try (final ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final long[] d =
					{
						rset.getLong("total_count"),
						rset.getLong("monthly_count"),
						rset.getLong("weekly_count"),
						rset.getLong("daily_count")
					};
					final String category = rset.getString("category");
					data.put(category, d);
				}
			}
		}
		catch (Exception e)
		{
			_log.error("Failed loading character museum data.", e);
		}
		player.setMuseumPlayer(new MuseumPlayer(player.getObjectId(), player.getName(), data));
	}
	
	public void updateDataForChar(L2PcInstance player)
	{
		if (player.getMuseumPlayer() == null)
		{
			return;
		}
		String update = "";
		try (final Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			for (final Map.Entry<String, long[]> entry : player.getMuseumPlayer().getData().entrySet())
			{
				update = "REPLACE museum_statistics SET objectId=" + player.getObjectId() + ", name='" + player.getName() + "', category='" + entry.getKey() + "', total_count=" + entry.getValue()[0] + ", monthly_count=" + entry.getValue()[1] + ", weekly_count=" + entry.getValue()[2] + ", daily_count=" + entry.getValue()[3] + ", hasReward=0;";
				try (final PreparedStatement statement = con.prepareStatement(update))
				{
					statement.execute();
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Could not store char museum data: " + e.getMessage(), e);
		}
	}
	
	public void reloadConfigs()
	{
		loadCategories();
		restoreLastTops(RefreshTime.Total);
	}
	
	public static MuseumManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public class UpdateStats implements Runnable
	{
		RefreshTime _time;
		
		public UpdateStats(final RefreshTime time)
		{
			_time = time;
		}
		
		@Override
		public void run()
		{
			long time = 0L;
			switch (_time)
			{
				case Monthly:
				{
					final Calendar c = Calendar.getInstance();
					c.set(2, c.get(2) + 1);
					c.set(5, 1);
					c.set(11, 0);
					c.set(12, 0);
					c.set(13, 0);
					time = Math.max(100L, c.getTimeInMillis() - System.currentTimeMillis());
					GlobalVariablesManager.getInstance().set("museum_monthly", c.getTimeInMillis());
					ThreadPoolManager.getInstance().scheduleGeneral(new UpdateStats(RefreshTime.Monthly), time);
					cleanLastTops(_time);
					break;
				}
				case Weekly:
				{
					final Calendar c = Calendar.getInstance();
					c.set(7, 2);
					c.set(11, 0);
					c.set(12, 0);
					c.set(13, 0);
					if (c.getTimeInMillis() < System.currentTimeMillis())
					{
						c.setTimeInMillis(c.getTimeInMillis() + 604800000L);
					}
					time = Math.max(100L, c.getTimeInMillis() - System.currentTimeMillis());
					GlobalVariablesManager.getInstance().set("museum_weekly", c.getTimeInMillis());
					ThreadPoolManager.getInstance().scheduleGeneral(new UpdateStats(_time), time);
					cleanLastTops(_time);
					break;
				}
				case Daily:
				{
					final Calendar c = Calendar.getInstance();
					c.set(6, c.get(6) + 1);
					c.set(11, 0);
					c.set(12, 0);
					c.set(13, 0);
					time = Math.max(100L, c.getTimeInMillis() - System.currentTimeMillis());
					GlobalVariablesManager.getInstance().set("museum_daily", c.getTimeInMillis());
					ThreadPoolManager.getInstance().scheduleGeneral(new UpdateStats(_time), time);
					cleanLastTops(_time);
					break;
				}
			}
			refreshTops();
			restoreLastTops(RefreshTime.Total);
		}
		
		public void refreshTops()
		{
			for (final L2PcInstance player : L2World.getInstance().getAllPlayers().values())
			{
				if (player.getMuseumPlayer() != null)
				{
					player.getMuseumPlayer().resetData(_time);
				}
			}
			refreshTopsFromDatabase(_time);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final MuseumManager _instance;
		
		static
		{
			_instance = new MuseumManager();
		}
	}
}
