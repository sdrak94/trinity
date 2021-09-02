package net.sf.l2j.gameserver.communitybbs.Manager.lunaservices;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.RadarControl;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;

public class CommunityBosses extends BaseBBSManager
{
	protected static final Logger	_log				= Logger.getLogger(CommunityBosses.class.getName());
	private static final int		BOSSES_PER_PAGE		= 16;
	private static final String		BBS_PATH			= "data/html/CommunityBoard/bosses/";
	private static final int[]		BOSSES_TO_NOT_SHOW	=
	{
//		29006,																								// Core
//		29001,																								// Queen Ant
//		29014,																								// Orfen
//		25692,																								// Aenkinel
//		25423,																								// Fairy Queen Timiniel
//		25010,																								// Furious Thieles
//		25532,																								// Kechi
//		25119,																								// Messenger of Fairy Queen Berun
//		25159,																								// Paniel the Unicorn
//		25163,																								// Roaring Skylancer
//		25070,																								// Enchanted Forest Watcher Ruell
//		25603,																								// Darion
//		25544																								// Tully
	};
	
	public void onBypassCommand(L2PcInstance player, String bypass)
	{
		StringTokenizer st = new StringTokenizer(bypass, "_");
		String cmd = st.nextToken();
		player.setSessionVar("add_fav", null);
		if ("bbsbosslist".equals(cmd))// _bbsbosslist_sort_page_search
		{
			int sort = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "1");
			int page = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "0");
			String search = st.hasMoreTokens() ? st.nextToken().trim() : "";
			sendBossListPage(player, getSortByIndex(sort), page, search);
		}
		else if ("bbsboss".equals(cmd))// _bbsboss_sort_page_search_rbId_btn
		{
			int sort = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "3");
			int page = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "0");
			String search = st.hasMoreTokens() ? st.nextToken().trim() : "";
			int bossId = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "25044");
			int buttonClick = Integer.parseInt(st.hasMoreTokens() ? st.nextToken() : "0");
			manageButtons(player, buttonClick, bossId);
			sendBossDetails(player, getSortByIndex(sort), page, search, bossId);
		}
	}
	
	/**
	 * Showing list of bosses in Community Board with their Name, Level, Status and Show Details button
	 * 
	 * @param player
	 *            guy that will receive list
	 * @param sort
	 *            index of the sorting type
	 * @param page
	 *            number of the page(Starting from 0)
	 * @param search
	 *            word in Name of the boss
	 */
	private static void sendBossListPage(L2PcInstance player, SortType sort, int page, String search)
	{
		String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), BBS_PATH + "bbs_boss_list.htm");
		Map<Integer, L2Spawn> allBosses = getSearchedBosses(sort, search);
		Map<Integer, L2Spawn> bossesToShow = getBossesToShow(allBosses, page);
		boolean isThereNextPage = allBosses.size() > bossesToShow.size();
		html = getBossListReplacements(html, page, bossesToShow, isThereNextPage);
		html = getNormalReplacements(html, page, sort, search, -1);
		ShowBoard.separateAndSend(html, player);
	}
	
	/**
	 * Replacing %x% words in bbs_bbslink_list.htm file
	 * 
	 * @param html
	 *            existing file
	 * @param page
	 *            number of the page(Starting from 0)
	 * @param allBosses
	 *            Map<BossId, BossStatsSet> of bosses that will be shown
	 * @param nextPage
	 *            Is the next page?
	 * @return ready HTML
	 */
	private static String getBossListReplacements(String html, int page, Map<Integer, L2Spawn> allBosses, boolean nextPage)
	{
		String newHtml = html;
		int i = 0;
		for (Entry<Integer, L2Spawn> entry : allBosses.entrySet())
		{
			L2NpcTemplate temp = NpcTable.getInstance().getTemplate(entry.getKey().intValue());
			L2Spawn spawn = entry.getValue();
			boolean isAlive = RaidBossSpawnManager.getInstance().getRaidBossStatusId(temp.getNpcId()).equals(RaidBossSpawnManager.StatusEnum.ALIVE);
			newHtml = newHtml.replace("<?name_" + i + "?>", temp.getName());
			newHtml = newHtml.replace("<?level_" + i + "?>", String.valueOf(temp.level));
			newHtml = newHtml.replace("<?status_" + i + "?>", isAlive ? "Alive" : getDeathDate(spawn.getDeathDate()));
			newHtml = newHtml.replace("<?status_color_" + i + "?>", getTextColor(isAlive));
			newHtml = newHtml.replace("<?bp_" + i + "?>", "<button value=\"show\" action=\"bypass _bbsboss_<?sort?>_" + page + "_ <?search?> _" + entry.getKey() + "\" width=40 height=12 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">");
			i++;
		}
		for (int j = i; j < BOSSES_PER_PAGE; j++)
		{
			newHtml = newHtml.replace("<?name_" + j + "?>", "...");
			newHtml = newHtml.replace("<?level_" + j + "?>", "...");
			newHtml = newHtml.replace("<?status_" + j + "?>", "...");
			newHtml = newHtml.replace("<?status_color_" + j + "?>", "FFFFFF");
			newHtml = newHtml.replace("<?bp_" + j + "?>", "...");
		}
		newHtml = newHtml.replace("<?previous?>", page > 0 ? "<button action=\"bypass _bbsbosslist_<?sort?>_" + (page - 1) + "_<?search?>\" width=16 height=16 back=\"L2UI_CH3.shortcut_prev_down\" fore=\"L2UI_CH3.shortcut_prev\">" : "<br>");
		newHtml = newHtml.replace("<?next?>", nextPage && i == BOSSES_PER_PAGE ? "<button action=\"bypass _bbsbosslist_<?sort?>_" + (page + 1) + "_<?search?>\" width=16 height=16 back=\"L2UI_CH3.shortcut_next_down\" fore=\"L2UI_CH3.shortcut_next\">" : "<br>");
		newHtml = newHtml.replace("<?pages?>", String.valueOf(page + 1));
		return newHtml;
	}
	
	/**
	 * Getting all bosses to show(checking only page)
	 * 
	 * @param page
	 *            number of the page(Starting from 0)
	 * @return Bosses
	 */
	private static Map<Integer, L2Spawn> getBossesToShow(Map<Integer, L2Spawn> allBosses, int page)
	{
		Map<Integer, L2Spawn> bossesToShow = new LinkedHashMap<Integer, L2Spawn>();
		int i = 0;
		for (Entry<Integer, L2Spawn> entry : allBosses.entrySet())
		{
			if (i < page * BOSSES_PER_PAGE)
			{
				i++;
			}
			else
			{
				L2Spawn boss = entry.getValue();
				L2NpcTemplate temp = NpcTable.getInstance().getTemplate(entry.getKey().intValue());
				if (boss != null && temp != null)
				{
					i++;
					bossesToShow.put(entry.getKey(), entry.getValue());
					if (i > (page * BOSSES_PER_PAGE + BOSSES_PER_PAGE - 1))
					{
						return bossesToShow;
					}
				}
			}
		}
		return bossesToShow;
	}
	
	/**
	 * Showing detailed info about Boss in Community Board. Including name, level, status, stats, image
	 * 
	 * @param player
	 *            guy that will receive details
	 * @param sort
	 *            index of the sorting type
	 * @param page
	 *            number of the page(Starting from 0)
	 * @param search
	 *            word in Name of the boss
	 * @param bossId
	 *            Id of the boss to show
	 */
	private static void sendBossDetails(L2PcInstance player, SortType sort, int page, CharSequence search, int bossId)
	{
		String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), BBS_PATH + "bbs_boss_details.htm");
		L2Spawn bossSet = RaidBossSpawnManager.getInstance().getSpawns().get(bossId);
		if (bossSet == null)
		{
			ShowBoard.separateAndSend(html, player);
			return;
		}
		L2NpcTemplate bossTemplate = NpcTable.getInstance().getTemplate(bossId);
		L2Npc bossInstance = getAliveBoss(bossId);
		html = getDetailedBossReplacements(html, bossSet, bossTemplate, bossInstance);
		html = getNormalReplacements(html, page, sort, search, bossId);
		// if(!AutoImageSenderManager.isImageAutoSendable(bossId))
		// ImagesCache.getInstance().sendImageToPlayer(player, bossId);
		ShowBoard.separateAndSend(html, player);
	}
	
	/**
	 * Managing buttons that were clicking in Boss Details page
	 * 
	 * @param player
	 *            that clicked button
	 * @param buttonIndex
	 *            1: Showing Location of the boss. 2: Showing Drops
	 * @param bossId
	 *            Id of the boss that player was looking into
	 */
	private static void manageButtons(L2PcInstance player, int buttonIndex, int bossId)
	{
		switch (buttonIndex)
		{
			case 1:// Show Location
				RaidBossSpawnManager.showBossLocation(player, bossId);
				break;
			case 2:// Show Drops
				L2Npc obj = RaidBossSpawnManager.getInstance().getBoss(bossId);
				if (obj == null)
				{
					obj = new L2RaidBossInstance(bossId, NpcTable.getInstance().getTemplate(bossId));
				}
				String htm = obj.getDropHtml(player);

				NpcHtmlMessage html = new NpcHtmlMessage(0);
				html.setHtml(htm);
				player.sendPacket(html);
			case 3:// Go to Boss
					// if(!player.isInZonePeace() || Olympiad.isRegistered(player))
					// {
					// player.sendMessage("You can do it only in safe zone!");
					// return;
					// }
					// NpcInstance aliveInstance = getAliveBoss(bossId);
					// if(aliveInstance != null)
					// player.teleToLocation(aliveInstance.getLoc());
					// else
					// player.sendMessage("Boss isn't alive!");
				break;
			case 4:// Show Location
				player.sendPacket(new RadarControl(2, 2, 0, 0, 0));
		}
	}
	
	/**
	 * Replacing all %a% words by real Values in Detailed Boss Page
	 * 
	 * @param html
	 *            current Html
	 * @param bossSet
	 *            StatsSet of the boss
	 * @param bossTemplate
	 *            NpcTemplate of the boss
	 * @param bossInstance
	 *            any Instance of the boss(can be null)
	 * @return filled HTML
	 */
	private static String getDetailedBossReplacements(String html, L2Spawn bossSet, L2NpcTemplate bossTemplate, L2Npc bossInstance)
	{
		String newHtml = html;
		boolean isAlive = RaidBossSpawnManager.getInstance().getRaidBossStatusId(bossTemplate.getNpcId()).equals(RaidBossSpawnManager.StatusEnum.ALIVE);
		newHtml = newHtml.replace("<?name?>", bossTemplate.getName());
		newHtml = newHtml.replace("<?level?>", String.valueOf(bossTemplate.level));
		newHtml = newHtml.replace("<?status?>", isAlive ? "Alive" : getDeathDate(bossSet.getDeathDate()));
		newHtml = newHtml.replace("<?status_color?>", getTextColor(isAlive));
		newHtml = newHtml.replace("<?minions?>", String.valueOf(getMinionsCount(bossTemplate)));
		newHtml = newHtml.replace("<?currentHp?>", Util.formatAdena((int) (bossInstance != null ? (int) bossInstance.getCurrentHp() : 0)));
		newHtml = newHtml.replace("<?maxHp?>", Util.formatAdena((int) bossTemplate.baseHpMax));
		newHtml = newHtml.replace("<?minions?>", String.valueOf(getMinionsCount(bossTemplate)));
		return newHtml;
	}
	
	/**
	 * Replacing page, sorts, bossId, search
	 * 
	 * @param html
	 *            to fill
	 * @param page
	 *            number
	 * @param sort
	 *            type
	 * @param search
	 *            word
	 * @param bossId
	 *            If of the boss, set -1 if doesn't matter
	 * @return new Html page
	 */
	private static String getNormalReplacements(String html, int page, SortType sort, CharSequence search, int bossId)
	{
		String newHtml = html;
		newHtml = newHtml.replace("<?page?>", String.valueOf(page));
		newHtml = newHtml.replace("<?sort?>", String.valueOf(sort.index));
		newHtml = newHtml.replace("<?bossId?>", String.valueOf(bossId));
		newHtml = newHtml.replace("<?search?>", search);
		for (int i = 1; i <= 6; i++)
		{
			if (Math.abs(sort.index) == i)
				newHtml = newHtml.replace("<?sort" + i + "?>", String.valueOf(-sort.index));
			else
				newHtml = newHtml.replace("<?sort" + i + "?>", String.valueOf(i));
		}
		return newHtml;
	}
	
	private static boolean isBossAlive(int i)
	{
		L2RaidBossInstance rb = (L2RaidBossInstance) L2World.getInstance().findObject(i);
		return rb.getRaidStatus().equals(RaidBossSpawnManager.StatusEnum.ALIVE);
	}
	
	private static String getRespawnTime(StatsSet set)
	{
		if (set.getInteger("respawn_delay", 0) < System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1L))
			return "Dead";
		long delay = set.getInteger("respawn_delay", 0) - (System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1L));
		// System.out.println(delay);
		int hours = (int) (delay / 60 / 60);
		int mins = (int) ((delay - (hours * 60 * 60)) / 60);
		int secs = (int) ((delay - ((hours * 60 * 60) + (mins * 60))));
		String Strhours = hours < 10 ? "0" + hours : "" + hours;
		String Strmins = mins < 10 ? "0" + mins : "" + mins;
		String Strsecs = secs < 10 ? "0" + secs : "" + secs;
		return "<font color=\"b02e31\">" + Strhours + ":" + Strmins + ":" + Strsecs + "</font>";
	}
	
	public static String getDeathDate(long deathDate)
	{
		if (deathDate == 0)
			return "Dead";
		
		String dateFormat = "dd/MM hh:mm";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(deathDate);
		
		return "Died: " + simpleDateFormat.format(calendar.getTime());
	}
	/**
	 * Getting alive and visible instance of the bossId
	 * 
	 * @param bossId
	 *            Id of the boss
	 * @return Instance of the boss
	 */
	private static L2Npc getAliveBoss(int bossId)
	{
		return RaidBossSpawnManager.getInstance().getBoss(bossId);
	}
	
	private static int getMinionsCount(L2NpcTemplate template)
	{
//		int minionsCount = 0;
//		for (L2MinionData minion : template.getMinionData())
//			minionsCount += minion.getAmount();
		return 0;
	}
	
	private static String getTextColor(boolean alive)
	{
		if (alive)
			return "259a30";// "327b39";
		else
			return "b02e31";// "8f3d3f";
	}
	
	/**
	 * Getting List of Bosses that player is looking for(including sort and search)
	 * 
	 * @param sort
	 *            Type of sorting he want to use
	 * @param search
	 *            word that he is looking for
	 * @return Map of Bosses
	 */
	private static Map<Integer, L2Spawn> getSearchedBosses(SortType sort, String search)
	{
		Map<Integer, L2Spawn> result = getBossesMapBySearch(search);
		for (int id : BOSSES_TO_NOT_SHOW)
			result.remove(id);
		result = sortResults(result, sort);
		return result;
	}
	
	/**
	 * Getting List of Bosses that player is looking for(including search)
	 * 
	 * @param search
	 *            String that boss Name needs to contains(can be Empty)
	 * @return MapMap of Bosses
	 */
	private static Map<Integer, L2Spawn> getBossesMapBySearch(String search)
	{
		Map<Integer, L2Spawn> finalResult = new HashMap<Integer, L2Spawn>();
		if (search.isEmpty())
		{
			finalResult = RaidBossSpawnManager.getInstance().getSpawns();
		}
		else
		{
			for (Entry<Integer, L2Spawn> entry : RaidBossSpawnManager.getInstance().getSpawns().entrySet())
			{
				L2NpcTemplate temp = NpcTable.getInstance().getTemplate(entry.getKey().intValue());
				if (containsIgnoreCase(temp.getName(), search))
					finalResult.put(entry.getKey(), entry.getValue());
			}
		}
		return finalResult;
	}
	
	/**
	 * Sorting results by sort type
	 * 
	 * @param result
	 *            map to sort
	 * @param sort
	 *            type
	 * @return sorted Map
	 */
	private static Map<Integer, L2Spawn> sortResults(Map<Integer, L2Spawn> result, SortType sort)
	{
		ValueComparator bvc = new ValueComparator(result, sort);
		Map<Integer, L2Spawn> sortedMap = new TreeMap<Integer, L2Spawn>(bvc);
		sortedMap.putAll(result);
		return sortedMap;
	}
	
	/**
	 * Comparator of Bosses
	 */
	private static class ValueComparator implements Comparator<Integer>, Serializable
	{
		private static final long				serialVersionUID	= 4782405190873267622L;
		private final Map<Integer, L2Spawn>	base;
		private final SortType					sortType;
		
		private ValueComparator(Map<Integer, L2Spawn> base, SortType sortType)
		{
			this.base = base;
			this.sortType = sortType;
		}
		
		@Override
		public int compare(Integer o1, Integer o2)
		{
			int sortResult = sortById(o1, o2, sortType);
			if (sortResult == 0 && !o1.equals(o2) && Math.abs(sortType.index) != 1)
				sortResult = sortById(o1, o2, SortType.NAME_ASC);
			return sortResult;
		}
		
		/**
		 * Comparing a and b but sorting
		 * 
		 * @param a
		 *            first variable
		 * @param b
		 *            second variable
		 * @param sorting
		 *            type of sorting
		 * @return result of comparing
		 */
		private int sortById(Integer a, Integer b, SortType sorting)
		{
			L2NpcTemplate temp1 = NpcTable.getInstance().getTemplate(a.intValue());
			L2NpcTemplate temp2 = NpcTable.getInstance().getTemplate(b.intValue());
			L2Spawn set1 = base.get(a);
			L2Spawn set2 = base.get(b);
			switch (sorting)
			{
				case NAME_ASC:
					return temp1.getName().compareTo(temp2.getName());
				case NAME_DESC:
					return temp2.getName().compareTo(temp1.getName());
				case LEVEL_ASC:
					return Integer.compare(temp1.level, temp2.level);
				case LEVEL_DESC:
					return Integer.compare(temp2.level, temp1.level);
				case STATUS_ASC:
					return Long.compare(set1.getDeathDate(), set2.getDeathDate());
				case STATUS_DESC:
					return Long.compare(set2.getDeathDate(), set1.getDeathDate());
			}
			return 0;
		}
	}
	
	private enum SortType
	{
		NAME_ASC(1),
		NAME_DESC(-1),
		LEVEL_ASC(2),
		LEVEL_DESC(-2),
		STATUS_ASC(3),
		STATUS_DESC(-3);
		
		public final int index;
		
		SortType(int index)
		{
			this.index = index;
		}
	}
	
	/**
	 * Getting SortType by index
	 * 
	 * @param i
	 *            index
	 * @return SortType
	 */
	private static SortType getSortByIndex(int i)
	{
		for (SortType type : SortType.values())
			if (type.index == i)
				return type;
		return SortType.NAME_ASC;
	}
	
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		onBypassCommand(activeChar, command);
	}
	
	public static boolean containsIgnoreCase(final CharSequence str, final CharSequence searchStr)
	{
		if (str == null || searchStr == null)
		{
			return false;
		}
		final int len = searchStr.length();
		final int max = str.length() - len;
		for (int i = 0; i <= max; i++)
		{
			if (regionMatches(str, true, i, searchStr, 0, len))
			{
				return true;
			}
		}
		return false;
	}
	
	static boolean regionMatches(final CharSequence cs, final boolean ignoreCase, final int thisStart, final CharSequence substring, final int start, final int length)
	{
		if (cs instanceof String && substring instanceof String)
		{
			return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
		}
		int index1 = thisStart;
		int index2 = start;
		int tmpLen = length;
		// Extract these first so we detect NPEs the same as the java.lang.String version
		final int srcLen = cs.length() - thisStart;
		final int otherLen = substring.length() - start;
		// Check for invalid parameters
		if (thisStart < 0 || start < 0 || length < 0)
		{
			return false;
		}
		// Check that the regions are long enough
		if (srcLen < length || otherLen < length)
		{
			return false;
		}
		while (tmpLen-- > 0)
		{
			final char c1 = cs.charAt(index1++);
			final char c2 = substring.charAt(index2++);
			if (c1 == c2)
			{
				continue;
			}
			if (!ignoreCase)
			{
				return false;
			}
			// The real same check as in String.regionMatches():
			final char u1 = Character.toUpperCase(c1);
			final char u2 = Character.toUpperCase(c2);
			if (u1 != u2 && Character.toLowerCase(u1) != Character.toLowerCase(u2))
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		// TODO Auto-generated method stub
	}
	
	private static final class SingletonHolder
	{
		protected static final CommunityBosses _instance = new CommunityBosses();
	}
	
	public static CommunityBosses getInstance()
	{
		return SingletonHolder._instance;
	}
}