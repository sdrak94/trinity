package scripts.achievements;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import luna.custom.achievements.AchievementBBSManager;
import luna.custom.handler.AchievementHolder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2ChestInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeGuardInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.serverpackets.TutorialCloseHtml;
import net.sf.l2j.gameserver.network.serverpackets.TutorialShowQuestionMark;
import net.sf.l2j.gameserver.util.Util;

/**
 * @author Nik
 */
public class Achievements extends Quest
{
	public static final Logger _log = Logger.getLogger(Achievements.class.getName());
	
	private static final String[] COMMANDS =
	{
		"achievements",
		"achievements_close",
		"achievements_cat"
	};
	
	private ScheduledFuture<?> _globalNotification;
	private final Map<Integer, Integer> _achievementMaxLevels;
	private final List<AchievementCategory> _achievementCategories;
	private final Map<Integer, Map<Integer, Integer>> _playersAchievements;
	
	public Achievements()
	{
		super(-1, "Achievements", "custom");
		
		_achievementMaxLevels = new HashMap<>();
		_achievementCategories = new LinkedList<>();
		_playersAchievements = new HashMap<>();
		
		load();
		setOnEnterWorld(true);
		
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			_globalNotification = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(() ->
			{
				if (!Config.L2JMOD_ACHIEVEMENT_SYSTEM)
				{
					if (_globalNotification != null)
					{
						_globalNotification.cancel(false);
						_globalNotification = null;
					}
					return;
				}
				
				try
				{
					L2World.getInstance().getAllPlayers().values().forEach(player ->
					{
						if (player == null)
						{
							return;
						}
						
						for (Entry<Integer, Integer> arco : getPlayerAchievements(player).entrySet())
						{
							int achievementId = arco.getKey();
							int achievementLevel = arco.getValue();
							if (getAchievementMaxLevel(achievementId) <= achievementLevel)
							{
								continue;
							}
							
							Achievement nextLevelAchievement = getAchievement(achievementId, ++achievementLevel);
							if ((nextLevelAchievement != null) && nextLevelAchievement.isDone(player.getCounters().getPoints(nextLevelAchievement.getType())))
							{
								// Make a question mark button.
								player.sendPacket(new TutorialShowQuestionMark(player.getObjectId()));
								break;
							}
						}
					});
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			} , 5000, 5000);
		}
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		for (String command : COMMANDS)
		{
			if (event.startsWith(command))
			{
				useBypass(event, player);
				return null;
			}
		}
		
		return event;
	}
	
	public boolean useBypass(String command, L2PcInstance activeChar)
	{
		//System.out.println("Bypass executed: " + command);
		command = command.replaceAll("%", " ");
		
		if (command.length() < 5)
		{
			return false;
		}
		
		if (command.startsWith("achievements_cat"))
		{
			String[] cm = command.split(" ");
			int category = Integer.parseInt(cm[1]);
			int page = cm.length < 1 ? 1 : Integer.parseInt(cm[2]);
			
			// Get player's achievement levels for this category.
			Map<Integer, Integer> playerAchievementLevelsInCategory = getPlayerAchievements(activeChar, category);
			final int levelSum = playerAchievementLevelsInCategory.values().stream().mapToInt(level -> level).sum();
			
			String html = getHtm("inAchievements.htm");
			
			final int totalpages = (int) (Math.ceil(playerAchievementLevelsInCategory.size() / 5.0));
			
			html = html.replaceAll("%back%", page == 1 ? "<button value=\"\" action=\"bypass achievements\" width=40 height=20 back=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\" fore=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\">" : "<button value=\"\" action=\"bypass achievements_cat " + category + " " + (page - 1) + "\" width=40 height=20 back=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\" fore=\"L2UI_CT1.Inventory_DF_Btn_RotateRight\">");
			html = html.replaceAll("%more%", totalpages <= page ? "&nbsp;" : "<button value=\"\" action=\"bypass achievements_cat " + category + " " + (page + 1) + "\" width=40 height=20 back=\"L2UI_CT1.Inventory_DF_Btn_RotateLeft\" fore=\"L2UI_CT1.Inventory_DF_Btn_RotateLeft\">");
			
			AchievementCategory cat = _achievementCategories.stream().filter(ctg -> ctg.getCategoryId() == category).findAny().orElse(null);
			if (cat == null)
			{
				_log.warning("Achievements: category is null for id " + category + ". Player: " + activeChar);
				return false;
			}
			
			int all = 0;
			String achievementsHTML = "";
			for (Entry<Integer, Integer> entry : playerAchievementLevelsInCategory.entrySet())
			{
				all++;
				if (all > (page * 5))
				{
					continue;
				}
				if (all <= ((page - 1) * 5))
				{
					continue;
				}
				
				int aId = entry.getKey();
				int nextLevel = (entry.getValue() + 1) >= getAchievementMaxLevel(aId) ? getAchievementMaxLevel(aId) : (entry.getValue() + 1);
				Achievement a = getAchievement(aId, nextLevel);
				
				if (a == null)
				{
					_log.warning("Achievements: achievement is null for id " + aId + " level " + nextLevel + ". Player: " + activeChar);
					return false;
				}
				
				long playerPoints = activeChar.getCounters().getPoints(a.getType());
				
				achievementsHTML += a.getHtml(activeChar, playerPoints);
			}
			
			int greenbar = 0;
			if (levelSum > 0)
			{
				greenbar = (748 * ((levelSum * 100) / cat.getAchievements().size())) / 100;
				greenbar = Math.min(greenbar, 748);
			}
			String fp = html;
			fp = fp.replaceAll("%bar1up%", "" + greenbar);
			fp = fp.replaceAll("%bar2up%", "" + (748 - greenbar));
			
			fp = fp.replaceFirst("%caps1%", greenbar > 0 ? "Gauge_DF_Large_Food_Left" : "Gauge_DF_Large_Exp_bg_Left");
			
			fp = fp.replaceFirst("%caps2%", greenbar >= 748 ? "Gauge_DF_Large_Food_Right" : "Gauge_DF_Large_Exp_bg_Right");
			
			fp = fp.replaceFirst("%achievements%", achievementsHTML.isEmpty() ? "&nbsp;" : achievementsHTML);
			fp = fp.replaceFirst("%catName%", cat.getName());
			fp = fp.replaceFirst("%catDesc%", cat.getDesc());
			fp = fp.replaceFirst("%catIcon%", cat.getIcon());

			AchievementBBSManager.getInstance().parsecmd(fp, activeChar);
			//activeChar.sendPacket(new TutorialShowHtml(fp));
		}
		else if (command.equals("achievements_close"))
		{
			activeChar.sendPacket(new TutorialCloseHtml());
		}
		else if (command.startsWith("achievements"))
		{
			checkAchievementRewards(activeChar);
			
			String achievements = getHtm("Achievements.htm");
			
			String ac = "";
			for (AchievementCategory cat : _achievementCategories)
			{
				final int levelSum = getPlayerAchievements(activeChar, cat.getCategoryId()).values().stream().mapToInt(level -> level).sum();
				ac += cat.getHtml(levelSum);
			}
			
			achievements = achievements.replace("%categories%", ac);
			
			// player.sendPacket(html);
			AchievementBBSManager.getInstance().parsecmd(achievements, activeChar);
			//activeChar.sendPacket(new TutorialShowHtml(achievements));
		}
		else
		{
			_log.log(Level.WARNING, "Invalid achievements bypass: " + command);
		}
		return false;
	}
	
	public String onEnterWorld(L2PcInstance player)
	{
		// Load player achievements.
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			QuestState st = player.getQuestState(getName());
			if (st == null)
			{
				st = newQuestState(player);
			}
			
			Map<Integer, Integer> playerAchievementLevels = new HashMap<>(_achievementMaxLevels.size());
			String achievementLevels = Optional.ofNullable(st.get("achievements")).filter(String.class::isInstance).map(String.class::cast).orElse(null);
			if ((achievementLevels != null) && !achievementLevels.isEmpty())
			{
				String[] levels = achievementLevels.split(";");
				for (String ach : levels)
				{
					String[] lvl = ach.split(",");
					int achievementId = Integer.parseInt(lvl[0]);
					int achievementLvl = Integer.parseInt(lvl[1]);
					
					// Check if achievement exists.
					if (getAchievementMaxLevel(achievementId) > 0)
					{
						playerAchievementLevels.put(achievementId, achievementLvl);
					}
				}
			}
			
			// Fill missing achievement levels.
			_achievementMaxLevels.keySet().forEach(id -> playerAchievementLevels.putIfAbsent(id, 0));
			
			_playersAchievements.put(player.getObjectId(), playerAchievementLevels);
			
			player.addNotifyQuestOfKill(st);
		}
		
		return null;
	}
	public String onCreatureKill(L2Character killer, L2Character victim, QuestState qs)
	{
		if (!Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			return null;
		}
		
		L2PcInstance player = killer == null ? null : killer.getActingPlayer();
		if (player == null)
		{
			return null;
		}
		
		if (victim instanceof L2PcInstance)
		{
			victim.getActingPlayer().getCounters().timesDied++;
			
			if (victim.isInsideZone(L2Character.ZONE_SIEGE))
			{
				player.getCounters().playersKilledInSiege++;
			}
		}
		
		if (victim instanceof L2Npc)
		{
			if (victim instanceof L2ChestInstance)
			{
				player.getCounters().treasureBoxesOpened++;
			}
			else if (victim instanceof L2GuardInstance)
			{
				player.getCounters().townGuardsKilled++;
			}
			else if (victim instanceof L2SiegeGuardInstance)
			{
				player.getCounters().siegeGuardsKilled++;
			}
		}
		
		if ((player.getLevel() - victim.getLevel()) >= 10)
		{
			return null;
		}
		
		if (victim instanceof L2MonsterInstance)
		{
			player.getCounters().mobsKilled++;
			
			switch (((L2MonsterInstance) victim).getNpcId())
			{
				case 96020:
					if(player.getParty() != null)
					{
						for(L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().horrorKilled++;
							}
						}
					}
					else
					{
						player.getCounters().horrorKilled++;
					}
					break;
				case 96019:
					if(player.getParty() != null)
					{
						for(L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().holyKnightKilled++;
							}
						}
					}
					else
					{
						player.getCounters().holyKnightKilled++;
					}
					break;
				case 800001:
					if(player.getParty() != null)
					{
						for(L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().majinHorrorKilled++;
							}
						}
					}
					else
					{
						player.getCounters().majinHorrorKilled++;
					}
					break;
				case 800000:
					if(player.getParty() != null)
					{
						for(L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().majinOblivionKilled++;
							}
						}
					}
					else
					{
						player.getCounters().majinOblivionKilled++;
					}
					break;
				case 95103:
					if(player.getParty() != null)
					{
						for(L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().pusKilled++;
							}
						}
					}
					else
					{
						player.getCounters().pusKilled++;
					}
					break;
				case 960180:
					if(player.getParty() != null)
					{
						for(L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().titaniumDreadKilled++;
							}
						}
					}
					else
					{
						player.getCounters().titaniumDreadKilled++;
					}
					break;
				case 95609:
					if(player.getParty() != null)
					{
						for(L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().glabKilled++;
							}
						}
					}
					else
					{
						player.getCounters().glabKilled++;
					}
					break;
					default:
						break;
			}
		}
		
		if (victim.isRaid() && !victim.isRaidMinion())
		{
			forEachPlayerInGroup(player, plr ->
			{
				if (Util.calculateDistance(plr, victim, true) <= 5000)
				{
					plr.getCounters().raidsKilled++;
				}
				
				return true;
			});
		}
		
		if (victim.isChampion())
		{
			player.getCounters().championsKilled++;
		}
		
		if (victim instanceof L2Npc)
		{
			switch (((L2Npc) victim).getNpcId())
			{
				case 29001: // Queen Ant
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().antQueenKilled++;
						}
						
						return true;
					});
					break;
				case 29006: // Core
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().coreKilled++;
						}
						
						return true;
					});
					break;
				case 29014: // Orfen
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().orfenKilled++;
						}
						
						return true;
					});
					break;
				case 29019: // Antharas
				case 29066: // Antharas
				case 29067: // Antharas
				case 29068: // Antharas
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().antharasKilled++;
						}
						
						return true;
					});
					break;
				case 29020: // Baium
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().baiumKilled++;
						}
						
						return true;
					});
					break;
				case 29022: // Zaken Lv. 60
				case 29176: // Zaken Lv. 60
				case 29181: // Zaken Lv. 83
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().zakenKilled++;
						}
						
						return true;
					});
					break;
				case 29028: // Valakas
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().valakasKilled++;
						}
						
						return true;
					});
					break;
				case 29047: // Scarlet van Halisha / Frintezza
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().frintezzaKilled++;
						}
						
						return true;
					});
					break;
				case 29065: // Sailren
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().sailrenKilled++;
						}
						
						return true;
					});
					break;
				case 29099: // Baylor
				case 29186: // Baylor
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().baylorKilled++;
						}
						
						return true;
					});
					break;
				case 29118: // Beleth
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().belethKilled++;
						}
						
						return true;
					});
					break;
				case 29163: // Tiat
				case 29175: // Tiat
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().tiatKilled++;
						}
						
						return true;
					});
					break;
				case 29179: // Freya Normal
				case 29180: // Freya Hard
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().freyaKilled++;
						}
						
						return true;
					});
					break;
			}
		}
		
		return null;
	}
	
	private void forEachPlayerInGroup(L2PcInstance player, Function<L2PcInstance, Boolean> procedure)
	{
		if (player.isInParty())
		{
			if (player.getParty().isInCommandChannel())
			{
				for (L2PcInstance member : player.getParty().getCommandChannel().getMembers())
				{
					if (!procedure.apply(member))
					{
						break;
					}
				}
				return;
			}
			
			for (L2PcInstance member : player.getParty().getPartyMembers())
			{
				if (!procedure.apply(member))
				{
					break;
				}
			}
			return;
		}
		
		procedure.apply(player);
	}
	
	public void checkAchievementRewards(L2PcInstance player)
	{
		Map<Integer, Integer> playerAchievementLevels = getPlayerAchievements(player);
		synchronized (playerAchievementLevels)
		{
			for (Entry<Integer, Integer> arco : playerAchievementLevels.entrySet())
			{
				int achievementId = arco.getKey();
				int achievementLevel = arco.getValue();
				if (getAchievementMaxLevel(achievementId) <= achievementLevel)
				{
					continue;
				}
				
				Achievement nextLevelAchievement;
				do
				{
					achievementLevel++;
					nextLevelAchievement = getAchievement(achievementId, achievementLevel);
					if ((nextLevelAchievement != null) && nextLevelAchievement.isDone(player.getCounters().getPoints(nextLevelAchievement.getType())))
					{
						nextLevelAchievement.reward(player, playerAchievementLevels);
						
						// Save achievement.
						StringBuilder sb = new StringBuilder();
						playerAchievementLevels.entrySet().forEach(e -> sb.append(e.getKey()).append(",").append(e.getValue()).append(";"));
						
						QuestState st = player.getQuestState(getName());
						if (st == null)
						{
							st = newQuestState(player);
						}
						
						st.set("achievements", sb.toString());
					}
				}
				while (nextLevelAchievement != null);
			}
		}
	}
	
	public Achievement getAchievement(int achievementId, int achievementLevel)
	{
		return _achievementCategories.stream().flatMap(c -> c.getAchievements().stream()).filter(a -> (a.getId() == achievementId) && (a.getLevel() == achievementLevel)).findAny().orElse(null);
	}
	
	public int getAchievementMaxLevel(int id)
	{
		return _achievementMaxLevels.getOrDefault(id, 0);
	}
	
	private Map<Integer, Integer> getPlayerAchievements(L2PcInstance player)
	{
		return _playersAchievements.getOrDefault(player.getObjectId(), Collections.emptyMap());
	}
	
	private Map<Integer, Integer> getPlayerAchievements(L2PcInstance player, int categoryId)
	{
		//@formatter:off
		Map<Integer, Integer> playerAchievementLevelsInCategory = new HashMap<>();
		_playersAchievements.getOrDefault(player.getObjectId(), Collections.emptyMap()).entrySet().stream()
							.filter(e -> getAchievement(e.getKey(), Math.max(1, e.getValue())).getCategoryId() == categoryId)
							.forEach(e -> playerAchievementLevelsInCategory.put(e.getKey(), e.getValue()));
		//@formatter:on
		
		return playerAchievementLevelsInCategory;
	}
	public void load()
	{
		_achievementMaxLevels.clear();
		_achievementCategories.clear();
		try
		{
			File file = new File(Config.DATAPACK_ROOT, "data/xml/achievements/achievements.xml");
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			Document doc = factory.newDocumentBuilder().parse(file);
			
			for (Node g = doc.getFirstChild(); g != null; g = g.getNextSibling())
			{
				for (Node z = g.getFirstChild(); z != null; z = z.getNextSibling())
				{
					if (z.getNodeName().equals("categories"))
					{
						for (Node i = z.getFirstChild(); i != null; i = i.getNextSibling())
						{
							if ("cat".equalsIgnoreCase(i.getNodeName()))
							{
								int categoryId = Integer.valueOf(i.getAttributes().getNamedItem("id").getNodeValue());
								String categoryName = String.valueOf(i.getAttributes().getNamedItem("name").getNodeValue());
								String categoryIcon = String.valueOf(i.getAttributes().getNamedItem("icon").getNodeValue());
								String categoryDesc = String.valueOf(i.getAttributes().getNamedItem("desc").getNodeValue());
								_achievementCategories.add(new AchievementCategory(categoryId, categoryName, categoryIcon, categoryDesc));
							}
						}
					}
					else if (z.getNodeName().equals("achievement"))
					{
						int achievementId = Integer.valueOf(z.getAttributes().getNamedItem("id").getNodeValue());
						int achievementCategory = Integer.valueOf(z.getAttributes().getNamedItem("cat").getNodeValue());
						String desc = String.valueOf(z.getAttributes().getNamedItem("desc").getNodeValue());
						String fieldType = String.valueOf(z.getAttributes().getNamedItem("type").getNodeValue());
						int achievementMaxLevel = 0;
						
						for (Node i = z.getFirstChild(); i != null; i = i.getNextSibling())
						{
							if ("level".equalsIgnoreCase(i.getNodeName()))
							{
								int level = Integer.valueOf(i.getAttributes().getNamedItem("id").getNodeValue());
								long pointsToComplete = Long.parseLong(i.getAttributes().getNamedItem("need").getNodeValue());
								int fame = Integer.valueOf(i.getAttributes().getNamedItem("fame").getNodeValue());
								String name = String.valueOf(i.getAttributes().getNamedItem("name").getNodeValue());
								String icon = String.valueOf(i.getAttributes().getNamedItem("icon").getNodeValue());
								Achievement achievement = new Achievement(achievementId, level, name, achievementCategory, icon, desc, pointsToComplete, fieldType, fame);
								
								if (achievementMaxLevel < level)
								{
									achievementMaxLevel = level;
								}
								
								for (Node o = i.getFirstChild(); o != null; o = o.getNextSibling())
								{
									if ("reward".equalsIgnoreCase(o.getNodeName()))
									{
										int Itemid = Integer.valueOf(o.getAttributes().getNamedItem("id").getNodeValue());
										long Itemcount = Long.parseLong(o.getAttributes().getNamedItem("count").getNodeValue());
										achievement.addReward(Itemid, Itemcount);
										achievement.addRewardHolder(achievementId, Itemid, (int)Itemcount);
									}
								}
								
								AchievementCategory lastCategory = _achievementCategories.stream().filter(ctg -> ctg.getCategoryId() == achievementCategory).findAny().orElse(null);
								if (lastCategory != null)
								{
									lastCategory.getAchievements().add(achievement);
								}
							}
						}
						
						_achievementMaxLevels.put(achievementId, achievementMaxLevel);
						AchievementHolder.getInstance().addLevel(achievementId, achievementMaxLevel);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		_log.info("Achievement System: Loaded " + _achievementCategories.size() + " achievement categories and " + _achievementMaxLevels.size() + " achievements.");
	}
	
	public String getHtm(String fileName)
	{
		//String questName = getName();
		//String directory = getDescr().toLowerCase();
		//String content = HtmCache.getInstance().getHtm("data/scripts/" + directory + "/" + questName + "/" + fileName);
		String content = HtmCache.getInstance().getHtm("data/html/custom/Achievements/" + fileName);
		
		if (content == null)
			content = HtmCache.getInstance().getHtm("data/html/custom/Achievements/" + fileName);
			//content = HtmCache.getInstance().getHtmForce("data/scripts/quests/" + questName + "/" + fileName);
		
		return content;
	}
	
	public static void main(String[] args)
	{
		new Achievements();
	}
}