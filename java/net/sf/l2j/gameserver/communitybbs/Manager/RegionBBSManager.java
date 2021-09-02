/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.communitybbs.Manager;

import java.util.Collections;
import java.util.Comparator;
import java.util.StringTokenizer;

import javolution.util.FastList;
import javolution.util.FastMap;
import luna.custom.ranking.Ranking;
import luna.custom.ranking.xml.data.RankData;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.util.StringUtil;

public class RegionBBSManager extends BaseBBSManager
{
	/* private static Logger _logChat = Logger.getLogger("chat"); */
	private RegionBBSManager()
	{}
	
	/**
	 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsecmd(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
	 */
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		if (command.equals("_bbsloc"))
		{
			showOldCommunity(activeChar, 1);
		}
		else if (command.startsWith("_bbsloc;page;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			int page = 0;
			try
			{
				page = Integer.parseInt(st.nextToken());
			}
			catch (NumberFormatException nfe)
			{}
			showOldCommunity(activeChar, page);
		}
		else if (command.startsWith("_bbsloc;playerinfo;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			String name = st.nextToken();
			showOldCommunityPI(activeChar, name);
		}
		else
		{
			if (Config.COMMUNITY_TYPE == 1)
			{
				showOldCommunity(activeChar, 1);
			}
			else
			{
				ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", "101");
				activeChar.sendPacket(sb);
			}
		}
	}
	
	/**
	 * @param activeChar
	 * @param name
	 */
	private void showOldCommunityPI(L2PcInstance activeChar, String name)
	{
		final StringBuilder htmlCode = StringUtil.startAppend(1000, "<html><body><title>" + "Info of " + name + "</title>");
		L2PcInstance player = L2World.getInstance().getPlayer(name);
		if (player != null)
		{
			if (player._equipmentViewer == 1)
			{
				if (!activeChar.isGM() && activeChar != player)
				{
					player = activeChar;
				}
			}
			String sex = "Male";
			if (player.getAppearance().getSex())
				sex = "Female";
			@SuppressWarnings("unused")
			String levelApprox = "low";
			if (player.getLevel() >= 95)
				levelApprox = "maxed";
			else if (player.getLevel() >= 93)
				levelApprox = "extremely high";
			else if (player.getLevel() >= 90)
				levelApprox = "super high";
			else if (player.getLevel() >= 86)
				levelApprox = "very high";
			else if (player.getLevel() >= 79)
				levelApprox = "high";
			else if (player.getLevel() >= 50)
				levelApprox = "medium";
			else if (player.getLevel() >= 20)
				levelApprox = "low";
			StringUtil.append(htmlCode, "<br><center><table border=0 width=755><tr><td align=center valign=top width=755><font name=hs12 color=AFACEC>", player.getName(), " (", sex, " ", player.getTemplate().className, ")</font></td></tr></table>");
			if (activeChar != null && (activeChar.isGM() || player.getObjectId() == activeChar.getObjectId() || Config.SHOW_LEVEL_COMMUNITYBOARD))
			{
				long nextLevelExp = player.getExp();
				if (player.getLevel() < (Experience.MAX_LEVEL - 1))
				{
					nextLevelExp = Experience.LEVEL[player.getLevel() + 1];
				}
				StringUtil.append(htmlCode, "<center><img src=L2UI_CT1_CN.Windows.TitleBG width=285 height=3><table border=0 bgcolor=0e0d0d width=350><tr><td width=125><font name=__SYSTEMWORLDFONT>Level: ", String.valueOf(player.getLevel()), "</font></td>" + "<td width=125><font name=__SYSTEMWORLDFONT>Next Level: ", String.valueOf((player.getExp() / nextLevelExp) * 100), "%", "</font></td></tr>");
				int uptime = (int) player.getUptime() / 1000;
				int h = uptime / 3600;
				int m = (uptime - (h * 3600)) / 60;
				int s = ((uptime - (h * 3600)) - (m * 60));
				StringUtil.append(htmlCode, "<tr><td width=125><font name=__SYSTEMWORLDFONT>Uptime: ", String.valueOf(h), "h ", String.valueOf(m), "m ", String.valueOf(s), "s</font></td>");
				if (player.getClan() != null)
				{
					StringUtil.append(htmlCode, "<td width=125><font name=__SYSTEMWORLDFONT>Clan: ", player.getClan().getName(), " (Lvl ", String.valueOf(player.getClan().getLevel()), ")", "</font></td>");
				}
				StringUtil.append(htmlCode, "</tr>");
				StringUtil.append(htmlCode, "<tr><td width=125><font name=__SYSTEMWORLDFONT color=CC3333>Fame: ", String.valueOf(player.getFame()), "</td>" + "<td width=125>PvP Kills: ", String.valueOf(player.getPvpKills()), "</font></td></tr></table><img src=L2UI_CT1_CN.Windows.TitleBG width=285 height=3></center><br>");
				// StringUtil.append(htmlCode, "<br><center><table><tr><td><multiedit var=\"pm\" width=400 height=30><button value=\"Send PM\" action=\"Write Region PM ", player.getName(), " pm pm pm\" width=110 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><button value=\"Back\" action=\"bypass _bbsloc\" width=40 height=15 back=\"L2UI_ct1.button_df\"
				// fore=\"L2UI_ct1.button_df\"></td></tr>" + "</table></center><br>");
			}
			final RankData rank = Ranking.getRank(Ranking.topPvp, player);
			StringUtil.append(htmlCode, "<center><img src=" + rank.getIconLarge() + " width=128 height=128></center>");
			StringUtil.append(htmlCode, player.getInventory().equipInfo().toString());
			StringUtil.append(htmlCode, "<br>");
			// StringUtil.append(htmlCode, "<img src=L2UI_CT1_CN.Windows.TitleBG width=770 height=4>");*/
			// StringUtil.append(htmlCode, "<table cellpadding=4><tr>");
			// StringUtil.append(htmlCode, "<td><button value=\"Region\" width=120 height=32 action=\"bypass _bbsloc\" fore=L2UI_CT1_CN.Control.LobbyTextInput_normal back=L2UI_CT1_CN.Control.LobbyTextInput_focus></td>");
			// StringUtil.append(htmlCode, "</tr></table>");
			// StringUtil.append(htmlCode, "<table cellpadding=4><tr>");
			// StringUtil.append(htmlCode, "<td><button value=\"Top PvP\" width=120 height=32 action=\"bypass _bbstop;toppvp\" fore=\"L2UI_CT1_CN.Control.LobbyTextInput_normal\" back=\"L2UI_CT1_CN.Control.LobbyTextInput_focus\"></td>");
			// StringUtil.append(htmlCode, "<td><button value=\"Top Fame\" width=120 height=32 action=\"bypass _bbstop;topfame\" fore=\"L2UI_CT1_CN.Control.LobbyTextInput_normal\" back=\"L2UI_CT1_CN.Control.LobbyTextInput_focus\"></td>");
			// StringUtil.append(htmlCode, "<td><button value=\"Top PK\" width=120 height=32 action=\"bypass _bbstop;toppk\" fore=\"L2UI_CT1_CN.Control.LobbyTextInput_normal\" back=\"L2UI_CT1_CN.Control.LobbyTextInput_focus\"></td>");
			// StringUtil.append(htmlCode, "</tr></table>");
			// StringUtil.append(htmlCode, "<table cellpadding=4><tr>");
			// StringUtil.append(htmlCode, "<td><button value=\"Main Menu\" width=120 height=32 action=\"bypass _bbstop\" fore=L2UI_CT1_CN.Control.LobbyTextInput_normal back=L2UI_CT1_CN.Control.LobbyTextInput_focus></td>");
			// StringUtil.append(htmlCode, "</tr></table>");
			StringUtil.append(htmlCode, "<img src=L2UI_CT1_CN.Windows.TitleBG width=770 height=4>");
			StringUtil.append(htmlCode, "</body></html>");
			separateAndSend(htmlCode.toString(), activeChar);
		}
		else
		{
			ShowBoard sb = new ShowBoard(StringUtil.concat("<html><body><br><br><center>No player with name ", name, "</center><br><br></body></html>"), "101");
			activeChar.sendPacket(sb);
		}
	}
	
	/**
	 * @param activeChar
	 */
	private void showOldCommunity(L2PcInstance activeChar, int page)
	{
		separateAndSend(getCommunityPage(page, activeChar.isGM() ? "gm" : "pl"), activeChar);
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsewrite(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
	 */
	@Override
	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		if (activeChar == null)
			return;
		if (ar1.equals("PM"))
		{
			activeChar.sendMessage("Use PM");
			return;
			/*
			 * final StringBuilder htmlCode = StringUtil.startAppend(500, "<html><body><br>"
			 * + "<table border=0><tr><td FIXWIDTH=15></td><td align=center>L2J Community Board<img src=\"sek.cbui355\" width=610 height=1></td></tr><tr><td FIXWIDTH=15></td><td>");
			 * try
			 * {
			 * L2PcInstance receiver = L2World.getInstance().getPlayer(ar2);
			 * if (receiver == null)
			 * {
			 * StringUtil.append(htmlCode, "Player not found!<br><button value=\"Back\" action=\"bypass _bbsloc;playerinfo;", ar2, "\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">"
			 * + "</td></tr></table></body></html>");
			 * separateAndSend(htmlCode.toString(), activeChar);
			 * return;
			 * }
			 * if (Config.JAIL_DISABLE_CHAT && receiver.isInJail())
			 * {
			 * activeChar.sendMessage("Player is in jail.");
			 * return;
			 * }
			 * if (receiver.isChatBanned())
			 * {
			 * activeChar.sendMessage("Player is chat banned.");
			 * return;
			 * }
			 * if (activeChar.isInJail() && Config.JAIL_DISABLE_CHAT)
			 * {
			 * activeChar.sendMessage("You can not chat while in jail.");
			 * return;
			 * }
			 * if (activeChar.isChatBanned())
			 * {
			 * activeChar.sendMessage("You are banned from using chat");
			 * return;
			 * }
			 * if (Config.LOG_CHAT)
			 * {
			 * LogRecord record = new LogRecord(Level.INFO, ar3);
			 * record.setLoggerName("chat");
			 * record.setParameters(new Object[] { "TELL", "[" + activeChar.getName() + " to " + receiver.getName() + "]" });
			 * _logChat.log(record);
			 * }
			 * CreatureSay cs = new CreatureSay(activeChar.getObjectId(), Say2.TELL, activeChar.getName(), ar3);
			 * if (receiver != null && ((!BlockList.isBlocked(receiver, activeChar) && !receiver.getBlockList().isBlockAll()) || activeChar.isGM()))
			 * {
			 * if (Config.JAIL_DISABLE_CHAT && receiver.isInJail())
			 * {
			 * activeChar.sendMessage("Player is in jail.");
			 * return;
			 * }
			 * if (receiver.isChatBanned())
			 * {
			 * activeChar.sendPacket(new SystemMessage(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED));
			 * return;
			 * }
			 * if (receiver.getClient().isDetached())
			 * {
			 * activeChar.sendMessage("Player is in offline mode.");
			 * return;
			 * }
			 * if (!receiver.getMessageRefusal())
			 * {
			 * receiver.sendPacket(cs);
			 * activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), Say2.TELL, "->" + receiver.getName(), ar3));
			 * StringUtil.append(htmlCode, "Message Sent<br><button value=\"Back\" action=\"bypass _bbsloc;playerinfo;", receiver.getName(), "\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">"
			 * + "</td></tr></table></body></html>");
			 * separateAndSend(htmlCode.toString(), activeChar);
			 * }
			 * else
			 * {
			 * SystemMessage sm = new SystemMessage(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
			 * activeChar.sendPacket(sm);
			 * parsecmd("_bbsloc;playerinfo;" + receiver.getName(), activeChar);
			 * }
			 * }
			 * else
			 * {
			 * SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_ONLINE);
			 * sm.addString(receiver.getName());
			 * activeChar.sendPacket(sm);
			 * sm = null;
			 * }
			 * }
			 * catch (StringIndexOutOfBoundsException e)
			 * {
			 * // ignore
			 * }
			 */
		}
		else
		{
			ShowBoard sb = new ShowBoard(StringUtil.concat("<html><body><br><br><center>the command: ", ar1, " is not implemented yet</center><br><br></body></html>"), "101");
			activeChar.sendPacket(sb);
		}
	}
	
	private int													_onlineCount	= 0;
	private int													_onlineCountGm	= 0;
	private static FastMap<Integer, FastList<L2PcInstance>>		_onlinePlayers	= new FastMap<Integer, FastList<L2PcInstance>>().shared();
	private static FastMap<Integer, FastMap<String, String>>	_communityPages	= new FastMap<Integer, FastMap<String, String>>().shared();
	
	/**
	 * @return
	 */
	public static RegionBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public synchronized void changeCommunityBoard()
	{
		FastList<L2PcInstance> sortedPlayers = new FastList<L2PcInstance>();
		// synchronized (L2World.getInstance().getAllPlayers())
		{
			sortedPlayers.addAll(L2World.getInstance().getAllPlayers().values());
		}
		Collections.sort(sortedPlayers, new Comparator<L2PcInstance>()
		{
			public int compare(L2PcInstance p1, L2PcInstance p2)
			{
				return p1.getName().compareToIgnoreCase(p2.getName());
			}
		});
		_onlinePlayers.clear();
		_onlineCount = 0;
		_onlineCountGm = 0;
		for (L2PcInstance player : sortedPlayers)
		{
			addOnlinePlayer(player);
		}
		_communityPages.clear();
		writeCommunityPages();
	}
	
	private void addOnlinePlayer(L2PcInstance player)
	{
		boolean added = false;
		for (FastList<L2PcInstance> page : _onlinePlayers.values())
		{
			if (page.size() < Config.NAME_PAGE_SIZE_COMMUNITYBOARD)
			{
				if (!page.contains(player))
				{
					page.add(player);
					/* if (!player.isInvisible()) */
					_onlineCount++;
					_onlineCountGm++;
				}
				added = true;
				break;
			}
			else if (page.contains(player))
			{
				added = true;
				break;
			}
		}
		if (!added)
		{
			FastList<L2PcInstance> temp = new FastList<L2PcInstance>();
			int page = _onlinePlayers.size() + 1;
			if (temp.add(player))
			{
				_onlinePlayers.put(page, temp);
				/* if (!player.isInvisible()) */
				_onlineCount++;
				_onlineCountGm++;
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void writeCommunityPages()
	{
		final StringBuilder htmlCode = new StringBuilder(2000);
		final String tdClose = "</td>";
		final String tdOpen = "<td align=left valign=top>";
		final String trClose = "</tr>";
		final String trOpen = "<tr>";
		final String colSpacer = "<td FIXWIDTH=15></td>";
		for (int page : _onlinePlayers.keySet())
		{
			FastMap<String, String> communityPage = new FastMap<String, String>();
			htmlCode.setLength(0);
			StringUtil.append(htmlCode, "<html>\r\n" + "<body>\r\n" + "<table cellspacing=0 cellpadding=0>\r\n" + "<tr>\r\n" + "<td><img src=\"l2trinity_cb.img4\" width=765 height=525></td>\r\n" + "</tr>\r\n" + "</table>\r\n" + "<table>\r\n" + "<tr>\r\n" + "<td width=510></td>\r\n" + "<td>\r\n" + "<br><br>\r\n" + "<table cellspacing=-165 cellpadding=-355>\r\n" + "<tr>\r\n" + "<td>\r\n" + "<table>\r\n" + "<tr>\r\n" + "<td align=center width=200>" + "			<table border=0 cellpadding=0 cellspacing=0 width=769 height=50 background=\"l2ui_ct1.Windows_DF_TooltipBG\">" + "				<tr>" + "					<td valign=\"top\" align=\"center\" valign=top>" + "						<table width=755 height=50>" + "							<tr>" + "								<td width=755><br>" + "									<table border=0 cellspacing=0 cellpadding=0 width=755 height=20>" + "										<tr>" + "											<td WIDTH=375 align=center valign=top>" + "												<font name=hs12 color=AFACEC>Region</font><br>" + "											</td>" + "										</tr>" + "									</table>" + "									<table width=760 cellspacing=0 cellpadding=0 height=50 border=0>" + "										<tr>" + "											<td align=center>" + "												<table width=740 border=0>" + "													<tr>" + "														<td width=740 align=center>" + "															<table cellspacing=3 cellpadding=4 width=740 height=40 bgcolor=373737>" + "																<tr>" + "																	<td FIXWIDTH=40 align=right valign=top>" + "																		<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"icon.skill0817\">" + "																			<tr>" + "																				<td width=32 align=center valign=top>" + "																					<img src=\"L2UI_CH3.BloodHoodWnd.bloodhood_icon04\" width=\"36\" height=\"36\">" + "																				</td>" + "																			</tr>" + "																		</table>" + "																	</td>" + "																	<td FIXWIDTH=500 align=center valign=top>" + "																		<br1> <font name=__SYSTEMWORLDFONT>Server Restarted: " + String.valueOf(GameServer.dateTimeServerStarted.getTime()) + "</font>" + "<br1> <font name=__SYSTEMWORLDFONT>Online Players: " + String.valueOf(L2World.getInstance().getAllPlayersCount()) + " Player(s) online in L2 Trinity.</font></td>" + "<td FIXWIDTH=40 align=right valign=top>" + "<table border=0 cellspacing=0 cellpadding=0 width=36 height=32 background=\"icon.skill0817\">" + "																			<tr>" + "																				<td width=32 align=center valign=top>" + "																					<img src=\"L2UI_CH3.BloodHoodWnd.bloodhood_icon04\" width=\"36\" height=\"36\">" + "																				</td>" + "																			</tr>" + "																		</table>" + "																	</td>" + "																</tr>" + "															</table>" + "														</td>" + "													</tr>" + "												</table>" + "								</td>" + "							</tr>" + "						</table>" + "					</td>" + "				</tr>" + "			</table>" + "		</td>" + "	</center>" + "</tr>" + "</table>" + "<br><br><br>");
			int cell = 0;
			if (Config.BBS_SHOW_PLAYERLIST)
			{
				htmlCode.append("<table border=0><tr><td><table border=0>");
				for (L2PcInstance player : getOnlinePlayers(page))
				{
					cell++;
					if (cell == 1)
					{
						htmlCode.append(trOpen);
					}
					StringUtil.append(htmlCode, "<td align=left valign=top FIXWIDTH=130><button value=\" " + player.getName() + "\"width=120 height=32 action=\"bypass _bbsloc;playerinfo;", player.getName(), "\" fore=\"L2UI_CT1_CN.Control.LobbyTextInput_normal\" back=\"L2UI_CT1_CN.Control.LobbyTextInput_focus\">");
					// StringUtil.append(htmlCode, "<td align=left valign=top FIXWIDTH=110><a action=\"bypass _bbsloc;playerinfo;", player.getName(), "\">");
					// if (player.isGM())
					// {
					// StringUtil.append(htmlCode, "<font color=\"LEVEL\">", player.getName(), "</font>");
					// }
					// else
					// {
					// htmlCode.append(player.getName());
					// }
					htmlCode.append("</td>");
					if (cell < Config.NAME_PER_ROW_COMMUNITYBOARD)
						htmlCode.append(colSpacer);
					if (cell == Config.NAME_PER_ROW_COMMUNITYBOARD)
					{
						cell = 0;
						htmlCode.append(trClose);
					}
				}
				if (cell > 0 && cell < Config.NAME_PER_ROW_COMMUNITYBOARD)
				{
					htmlCode.append(trClose);
				}
				htmlCode.append("</table><br></td></tr>" + trOpen + "<td><img src=\"sek.cbui355\" width=600 height=1><br></td>" + trClose + "</table>");
			}
			if (getOnlineCount("gm") > Config.NAME_PAGE_SIZE_COMMUNITYBOARD)
			{
				htmlCode.append("<table border=0 width=600><tr>");
				if (page == 1)
				{
					htmlCode.append("<td align=right width=190><button value=\"Prev\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				else
				{
					StringUtil.append(htmlCode, "<td align=right width=190><button value=\"Prev\" action=\"bypass _bbsloc;page;", String.valueOf(page - 1), "\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				StringUtil.append(htmlCode, "<td FIXWIDTH=10></td>" + "<td align=center valign=top width=200>Displaying Page Num: ", String.valueOf(page), "</td>" + "<td FIXWIDTH=10></td>");
				if (getOnlineCount("gm") <= (page * Config.NAME_PAGE_SIZE_COMMUNITYBOARD))
				{
					htmlCode.append("<td width=190><button value=\"Next\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				else
				{
					StringUtil.append(htmlCode, "<td width=190><button value=\"Next\" action=\"bypass _bbsloc;page;", String.valueOf(page + 1), "\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				htmlCode.append("</tr></table>");
			}
			htmlCode.append("</body></html>");
			communityPage.put("gm", htmlCode.toString());
			htmlCode.setLength(0);
			StringUtil.append(htmlCode, "<html>\r\n" + "<body>\r\n" + "<table cellspacing=0 cellpadding=0>\r\n" + "<tr>\r\n" + "<td><img src=\"l2trinity_cb.img4\" width=765 height=525></td>\r\n" + "</tr>\r\n" + "</table>\r\n" + "<table>\r\n" + "<tr>\r\n" + "<td width=510></td>\r\n" + "<td>\r\n" + "<br><br>\r\n" + "<table cellspacing=-165 cellpadding=-355>\r\n" + "<tr>\r\n" + "<td>\r\n" + "<table>\r\n" + "<tr>\r\n" + "<td align=center width=200>" + "			<table border=0 cellpadding=0 cellspacing=0 width=769 height=50 background=\"l2ui_ct1.Windows_DF_TooltipBG\">" + "				<tr>" + "					<td valign=\"top\" align=\"center\" valign=top>" + "						<table width=755 height=50>" + "							<tr>" + "								<td width=755><br>" + "									<table border=0 cellspacing=0 cellpadding=0 width=755 height=20>" + "										<tr>" + "											<td WIDTH=375 align=center valign=top>" + "												<font name=hs12 color=AFACEC>Region</font><br>" + "											</td>" + "										</tr>" + "									</table>" + "									<table width=760 cellspacing=0 cellpadding=0 height=50 border=0>" + "										<tr>" + "											<td align=center>" + "												<table width=740 border=0>" + "													<tr>" + "														<td width=740 align=center>" + "															<table cellspacing=3 cellpadding=4 width=740 height=40 bgcolor=373737>" + "																<tr>" + "																	<td FIXWIDTH=40 align=right valign=top>" + "																		<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"icon.skill0817\">" + "																			<tr>" + "																				<td width=32 align=center valign=top>" + "																					<img src=\"L2UI_CH3.BloodHoodWnd.bloodhood_icon04\" width=\"36\" height=\"36\">" + "																				</td>" + "																			</tr>" + "																		</table>" + "																	</td>" + "																	<td FIXWIDTH=500 align=center valign=top>" + "																		<br1> <font name=__SYSTEMWORLDFONT>Server Restarted: " + String.valueOf(GameServer.dateTimeServerStarted.getTime()) + "</font>" + "<br1> <font name=__SYSTEMWORLDFONT>Online Players: " + String.valueOf(L2World.getInstance().getAllPlayersCount()) + " Player(s) online in L2 Trinity.</font></td>" + "<td FIXWIDTH=40 align=right valign=top>" + "<table border=0 cellspacing=0 cellpadding=0 width=36 height=32 background=\"icon.skill0817\">" + "																			<tr>" + "																				<td width=32 align=center valign=top>" + "																					<img src=\"L2UI_CH3.BloodHoodWnd.bloodhood_icon04\" width=\"36\" height=\"36\">" + "																				</td>" + "																			</tr>" + "																		</table>" + "																	</td>" + "																</tr>" + "															</table>" + "														</td>" + "													</tr>" + "												</table>" + "								</td>" + "							</tr>" + "						</table>" + "					</td>" + "				</tr>" + "			</table>" + "		</td>" + "	</center>" + "</tr>" + "</table>" + "<br><br><br>");
			if (Config.BBS_SHOW_PLAYERLIST)
			{
				htmlCode.append("<table border=0><tr><td><table border=0>");
				cell = 0;
				for (L2PcInstance player : getOnlinePlayers(page))
				{
					if (player == null)
						continue; // Go to next
					if (player.isGM())
					{
						if (player.isInvisible() || !GmListTable.getInstance().getAllGms(false).contains(player))
							continue;
					}
					else if (player._equipmentViewer == 1)
						continue;
					cell++;
					if (cell == 1)
					{
						htmlCode.append(trOpen);
					}
					StringUtil.append(htmlCode, "<td align=left valign=top FIXWIDTH=130><button value=\" " + player.getName() + "\"width=120 height=32 action=\"bypass _bbsloc;playerinfo;", player.getName(), "\" fore=\"L2UI_CT1_CN.Control.LobbyTextInput_normal\" back=\"L2UI_CT1_CN.Control.LobbyTextInput_focus\">");
					// StringUtil.append(htmlCode, "<td align=left valign=top FIXWIDTH=110><a action=\"bypass _bbsloc;playerinfo;", player.getName(), "\">");
					// if (player.isGM())
					// {
					// StringUtil.append(htmlCode, "<font color=\"LEVEL\">", player.getName(), "</font>");
					// }
					// else
					// {
					// htmlCode.append(player.getName());
					// }
					htmlCode.append("</td>");
					if (cell < Config.NAME_PER_ROW_COMMUNITYBOARD)
						htmlCode.append(colSpacer);
					if (cell == Config.NAME_PER_ROW_COMMUNITYBOARD)
					{
						cell = 0;
						htmlCode.append(trClose);
					}
				}
				if (cell > 0 && cell < Config.NAME_PER_ROW_COMMUNITYBOARD)
					htmlCode.append(trClose);
				htmlCode.append("</table><br></td></tr>" + trOpen + "<td><img src=\"sek.cbui355\" width=600 height=1><br></td>" + trClose + "</table>");
			}
			if (getOnlineCount("pl") > Config.NAME_PAGE_SIZE_COMMUNITYBOARD)
			{
				htmlCode.append("<table border=0 width=600><tr>");
				if (page == 1)
				{
					htmlCode.append("<td align=right width=190><button value=\"Prev\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				else
				{
					StringUtil.append(htmlCode, "<td align=right width=190><button value=\"Prev\" action=\"bypass _bbsloc;page;", String.valueOf(page - 1), "\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				StringUtil.append(htmlCode, "<td FIXWIDTH=10></td>" + "<td align=center valign=top width=200>Displaying Page Num: ", String.valueOf(page), "</td>" + "<td FIXWIDTH=10></td>");
				if (getOnlineCount("pl") <= (page * Config.NAME_PAGE_SIZE_COMMUNITYBOARD))
				{
					htmlCode.append("<td width=190><button value=\"Next\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				else
				{
					StringUtil.append(htmlCode, "<td width=190><button value=\"Next\" action=\"bypass _bbsloc;page;", String.valueOf(page + 1), "\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				}
				htmlCode.append("</tr></table>");
			}
			htmlCode.append("</body></html>");
			communityPage.put("pl", htmlCode.toString());
			_communityPages.put(page, communityPage);
		}
	}
	
	private int getOnlineCount(String type)
	{
		if (type.equalsIgnoreCase("gm"))
			return _onlineCountGm;
		else
			return _onlineCount;
	}
	
	int getOnlineCountpub()
	{
		return _onlineCount;
	}
	
	private FastList<L2PcInstance> getOnlinePlayers(int page)
	{
		return _onlinePlayers.get(page);
	}
	
	public String getCommunityPage(int page, String type)
	{
		if (_communityPages.get(page) != null)
			return _communityPages.get(page).get(type);
		else
			return null;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final RegionBBSManager _instance = new RegionBBSManager();
	}
}