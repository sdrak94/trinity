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

import java.io.File;
import java.util.StringTokenizer;

import luna.custom.ranking.Ranking;
import luna.custom.ranking.TopData;
import luna.custom.ranking.xml.data.RankData;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.boards.CastleStatus;
import net.sf.l2j.gameserver.communitybbs.boards.ClanList;
import net.sf.l2j.gameserver.communitybbs.boards.GrandBossList;
import net.sf.l2j.gameserver.communitybbs.boards.HeroeList;
import net.sf.l2j.gameserver.communitybbs.boards.RaidList;
import net.sf.l2j.gameserver.communitybbs.boards.TopPlayers;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class AdvancedStats extends BaseBBSManager
{
	/**
	 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsecmd(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
	 */
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		String path = "data/html/CommunityBoard/";
		String filepath = "";
		String content = "";
		content = replaceVars(activeChar, content);
		if (command.equals("_bbsstats"))
		{
			filepath = path + "stats.htm";
			content = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), filepath);
			content = replaceVars(activeChar, content);
			separateAndSend(content, activeChar);
		}
		else if (command.startsWith("_bbstop;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			String file = st.nextToken();
			filepath = path + file + ".htm";
			File filecom = new File(filepath);
			content = replaceVars(activeChar, content);
			if (!(filecom.exists()))
			{
				content = "<html><body><br><br><center>The command " + command + " points to file(" + filepath + ") that NOT exists.</center></body></h>";
				separateAndSend(content, activeChar);
				return;
			}
			content = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), filepath);
			if (content.isEmpty())
				content = "<html><body><br><br><center>Content Empty: The command " + command + " points to an invalid or empty html file(" + filepath + ").</center></body></html>";
			switch (file)
			{
				case "toppvp":
					final StringBuilder HTML = new StringBuilder();
					HTML.append("<center><table width=\"100%\">");
					int index = 0;
					for (; index < Ranking.topPvp.size(); index++)
					{
						if (index >= 20)
							break;
						final TopData topData = Ranking.topPvp.get(index);
						appendPlayer(HTML, index, topData);
					}
					final int myPos = Ranking.getMyPosition(Ranking.topPvp, activeChar);
					final TopData myData = Ranking.topPvp.get(myPos);
					// appendMe(HTML, myPos, myData);
					HTML.append("</table></center><br></body></html>");
					content = content.replace("%toppvp%", HTML.toString());
					separateAndSend(content, activeChar);
					break;
				case "topEvents":
					final StringBuilder HTMLev = new StringBuilder();
					HTMLev.append("<center><table width=\"100%\">");
					int indexev = 0;
					for (; indexev < Ranking.topPvp.size(); indexev++)
					{
						if (indexev >= 20)
							break;
						final TopData topData = Ranking.topPvp.get(indexev);
						appendPlayer(HTMLev, indexev, topData);
					}
					// final int myPos = Ranking.getMyPosition(Ranking.topPvp, activeChar);
					// final TopData myData = Ranking.topPvp.get(myPos);
					// appendMe(HTML, myPos, myData);
					HTMLev.append("</table></center><br></body></html>");
					content = content.replace("%toppvp%", HTMLev.toString());
					separateAndSend(content, activeChar);
					break;
				case "toppk":
					final StringBuilder HTMLpk = new StringBuilder();
					int indexpk = 0;
					HTMLpk.append("<center><table width=\"100%\">");
					for (; indexpk < Ranking.topPk.size(); indexpk++)
					{
						if (indexpk >= 20)
							break;
						final TopData topData = Ranking.topPk.get(indexpk);
						appendPlayer(HTMLpk, indexpk, topData);
					}
					final int myPosPk = Ranking.getMyPosition(Ranking.topPk, activeChar);
					final TopData myDataPk = Ranking.topPk.get(myPosPk);
					// appendMe(HTMLpk, myPosPk, myDataPk);
					HTMLpk.append("</table></center><br></body></html>");
					content = content.replace("%toppk%", HTMLpk.toString());
					separateAndSend(content, activeChar);
					break;
				case "topfame":
					final StringBuilder HTMLfame = new StringBuilder();
					int indexfame = 0;
					HTMLfame.append("<center><table width=\"100%\">");
					for (; indexfame < Ranking.topFame.size(); indexfame++)
					{
						if (indexfame >= 20)
							break;
						final TopData topData = Ranking.topFame.get(indexfame);
						appendPlayer(HTMLfame, indexfame, topData);
					}
					final int myPosFame = Ranking.getMyPosition(Ranking.topFame, activeChar);
					final TopData myDataFame = Ranking.topFame.get(myPosFame);
					// appendMe(HTMLfame, myPosFame, myDataFame);
					HTMLfame.append("</table></center><br></body></html>");
					content = content.replace("%topfame%", HTMLfame.toString());
					separateAndSend(content, activeChar);
					break;
				case "topraid":
					final StringBuilder HTMLRaid = new StringBuilder();
					int indexRaid = 0;
					HTMLRaid.append("<center><table width=\"100%\">");
					for (; indexRaid < Ranking.topRaid.size(); indexRaid++)
					{
						if (indexRaid >= 20)
							break;
						final TopData topData = Ranking.topRaid.get(indexRaid);
						appendPlayer(HTMLRaid, indexRaid, topData);
					}
					final int myPosRaid = Ranking.getMyPosition(Ranking.topRaid, activeChar);
					final TopData myDataRaid = Ranking.topRaid.get(myPosRaid);
					// appendMe(HTMLRaid, myPosRaid, myDataRaid);
					HTMLRaid.append("</table></center><br></body></html>");
					content = content.replace("%topraid%", HTMLRaid.toString());
					separateAndSend(content, activeChar);
					break;
				case "topadena":
					TopPlayers adena = new TopPlayers(file);
					content = content.replaceAll("%topadena%", adena.loadTopList());
					break;
				case "toponline":
					TopPlayers online = new TopPlayers(file);
					content = content.replaceAll("%toponline%", online.loadTopList());
					break;
				case "heroes":
					HeroeList hr = new HeroeList();
					content = content.replaceAll("%heroelist%", hr.loadHeroeList());
					break;
				case "castle":
					CastleStatus status = new CastleStatus();
					content = content.replaceAll("%castle%", status.loadCastleList());
					break;
				case "boss":
					GrandBossList gb = new GrandBossList();
					content = content.replaceAll("%gboss%", gb.loadGrandBossList());
					break;
				case "stats":
					content = content.replace("%online%", Integer.toString(L2World.getInstance().getAllPlayersCount()));
					content = content.replace("%servercapacity%", Integer.toString(Config.MAXIMUM_ONLINE_USERS));
					content = content.replace("%serverruntime%", getServerRunTime());
					break;
				default:
					break;
			}
			if (file.startsWith("clan"))
			{
				int cid = Integer.parseInt(file.substring(4));
				ClanList cl = new ClanList(cid);
				content = content.replaceAll("%clanlist%", cl.loadClanList());
			}
			if (file.startsWith("raiddeneixame"))
			{
				String rfid = file.substring(13);
				RaidList rd = new RaidList(rfid);
				content = content.replaceAll("%raidlist%", rd.loadRaidList());
			}
			if (content.isEmpty())
			{
				content = "<html><body><br><br><center>404 :File not found or empty: " + filepath + " your command is " + command + "</center></body></html>";
			}
			separateAndSend(content, activeChar);
		}
		else
		{
			ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", "101");
			activeChar.sendPacket(sb);
			activeChar.sendPacket(new ShowBoard(null, "102"));
			activeChar.sendPacket(new ShowBoard(null, "103"));
		}
	}
	
	private static String getLineColor(int i)
	{
		if (i % 2 == 0)
			return "18191e";
		return "22181a";
	}
	
	private static void appendPlayer(final StringBuilder sb, final int pos, TopData topData)
	{
		final String color = pos % 2 == 0 ? "414452" : "250e14";
		final L2PcInstance player = L2World.getInstance().getPlayer(topData.getCharId());
		String charTitle = topData.getCharTitle();
		charTitle = charTitle.replace("<", "&lt;");
		charTitle = charTitle.replace(">", "&gt;");
		charTitle = charTitle.replace("<table>", "&lt;");
		charTitle = charTitle.replace("</table>", "&gt;");
		final RankData deco = Ranking.getRank(pos);
		String posicon = "<td width=15 align=center valign=top><img src=" + deco.getIcon() + " width=\"15\" height=\"16\"></td>";
		String poscolor = deco.getColor();
		sb.append("</table>");
		sb.append("<table bgcolor=" + color + " width=610 height=20>");
		sb.append("<tr>");
		sb.append("<td width=30 align=center valign=top><font name=__SYSTEMWORLDFONT color=b09979>" + (pos + 1) + "</font></td>");
		if (player != null)
			sb.append("<td width=130 align=center valign=top><font color=" + poscolor + ">" + "<a action=\"bypass _bbsloc;playerinfo;" + topData.getCharName() + "\">" + topData.getCharName() + "</a>" + "</font></td>");
		else
			sb.append("<td width=130 align=center valign=top><font color=" + poscolor + ">" + topData.getCharName() + "</font></td>");
		sb.append("<td width=130 align=center valign=top><font color=" + poscolor + ">" + charTitle + "</font></td>");
		sb.append("<td width=130 align=center valign=top><font color=" + poscolor + ">" + topData.getClanName() + "</font></td>");
		sb.append("<td width=120 align=center valign=top><font color=" + poscolor + ">" + topData.getClassName() + "</font></td>");
		sb.append("<td width=100 align=center valign=top><font color=" + poscolor + ">" + topData.getPvpKills() + "</font></td>");
		sb.append("</tr>");
	}
	
	private static void appendMe(final StringBuilder sb, final int pos, TopData topData)
	{
		String charTitle = topData.getCharTitle();
		charTitle = charTitle.replace("<", "&lt;");
		charTitle = charTitle.replace(">", "&gt;");
		final RankData deco = Ranking.getRank(pos);
		String myposicon = "<td width=16 align=center valign=top><img src=" + deco.getIcon() + " width=\"16\" height=\"16\"></td>";
		String myposcolor = deco.getColor();
		sb.append("<table width=740>");
		sb.append("<tr>");
		sb.append("<td width=740 align=center >");
		sb.append("<table width=740 height=33 bgcolor=" + myposcolor + ">");
		sb.append("<tr>");
		sb.append("<td align=center>");
		sb.append("<table>");
		sb.append("<tr>");
		sb.append("<td width=30 align=center valign=top><font name=__SYSTEMWORLDFONT color=b09979>" + (pos + 1) + "</font></td>" + myposicon + "");
		sb.append("<td width=200 align=center valign=top><font color=c0c0c0>" + "<a action=\"bypass _bbsloc;playerinfo;" + topData.getCharName() + "\">" + topData.getCharName() + "</a>" + "</font></td>");
		sb.append("<td width=200 align=center valign=top>");
		sb.append("<font color=c0c0c0>" + charTitle + "</font>");
		sb.append("</td>");
		sb.append("<td width=200 align=center valign=top>");
		sb.append("<font color=c0c0c0>" + topData.getClanName() + "</font>");
		sb.append("</td>");
		sb.append("<td width=200 align=center valign=top>");
		sb.append("<font color=c0c0c0>" + topData.getClassName() + "</font>");
		sb.append("</td>");
		sb.append("<td width=100 align=center valign=top>");
		sb.append("<font color=c0c0c0>" + topData.getPvpKills() + "</font>");
		sb.append("</td>");
		sb.append("<td width=100 align=center valign=top>");
		sb.append("<font color=c0c0c0>" + topData.getPkKills() + "</font>");
		sb.append("</td>");
		sb.append("<td width=100 align=center valign=top>");
		sb.append("<font color=c0c0c0>" + topData.getFame() + "</font>");
		sb.append("</td>");
		sb.append("<td width=100 align=center valign=top>");
		sb.append("<font color=c0c0c0>" + topData.getRaidPoints() + "</font>");
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("<img src=L2UI_CT1_CN.Windows.TitleBG width=769 height=3>");
		// sb.append("<tr>");
		/*
		 * sb.append("<td><font color="+color+">"+(pos + 1)+"</font></td>");
		 * if (player != null)
		 * sb.append("<td><font color="+color+">"+"<a action=\"bypass _bbsloc;playerinfo;" +topData.getCharName() + "\">"+topData.getCharName()+"</a>"+"</font></td>");
		 * else
		 * sb.append("<td><font color="+color+">"+topData.getCharName()+"</font></td>");
		 * sb.append("<td><img width=0 height=5><font color="+color+">"+topData.getCharTitle()+"</font></td>");
		 * sb.append("<td><img width=0 height=5><font color="+color+">"+topData.getClassName()+"</font></td>");
		 * sb.append("<td><img width=0 height=5><font color="+color+">"+topData.getClanName()+"</font></td>");
		 * sb.append("<td><img width=0 height=5><font color="+color+">"+topData.getFame()+"</font></td>");
		 * sb.append("<td><img width=0 height=5><font color="+color+">"+topData.getPvpKills()+"</font></td>");
		 * sb.append("<td><img width=0 height=5><font color="+color+">"+topData.getPkKills()+"</font></td>");
		 * sb.append("</tr>");
		 */
	}
	
	private String replaceVars(L2PcInstance activeChar, String content)
	{
		int uptime = (int) activeChar.getUptime() / 1000;
		int h = uptime / 3600;
		int m = (uptime - (h * 3600)) / 60;
		int s = ((uptime - (h * 3600)) - (m * 60));
		content = content.replace("%name%", activeChar.getName());
		content = content.replace("%class%", CharTemplateTable.getInstance().getClassNameById(activeChar.getClassId().getId()));
		content = content.replace("%level%", String.valueOf(activeChar.getLevel()));
		if (activeChar.getClan() != null)
		{
			content = content.replace("%clan%", String.valueOf(activeChar.getClan().getName()));
		}
		else
		{
			content = content.replace("%clan%", "Hello Darkness");
		}
		// content = content.replace("%noble%", String.valueOf(activeChar.isNoble() ? "Yes" : "No"));
		content = content.replace("%online_time%", String.valueOf(h) + " Hrs " + String.valueOf(m) + " Mins " + String.valueOf(s) + " Sec");
		content = content.replace("%ip%", activeChar.getIP());
		content = content.replace("%restart%", String.valueOf(GameServer.dateTimeServerStarted.getTime()));
		// content = content.replace("%online%", String.valueOf(RegionBBSManager.getInstance().getOnlineCountpub()));
		L2Clan clan = ClanTable.getInstance().getClan(activeChar.getClanId());
		String clanname = "-";
		if (clan != null)
			clanname = clan.getName() + " (Lvl " + clan.getLevel() + ")";
		// String topRaid = "None";
		// int topRaidStatus = Ranking.topRaid.get(0).getRaidPoints();
		// if (topRaidStatus != 0)
		// {
		// topRaid = Ranking.topRaid.get(0).getCharName() + " ("+Ranking.topRaid.get(0).getRaidPoints()+" Points)";
		// }
		content = content.replace("%name%", activeChar.getName());
		content = content.replace("%title%", activeChar.getTitle() == null ? "&nbsp;" : activeChar.getTitle());
		content = content.replace("%clan%", clanname);
		content = content.replace("%class%", activeChar.getClassId().getName());
		content = content.replace("%pvp%", String.valueOf(activeChar.getPvpKills()));
		content = content.replace("%pk%", String.valueOf(activeChar.getPkKills()));
		content = content.replace("%fame%", String.valueOf(activeChar.getFame()));
		content = content.replace("%serverr%", String.valueOf(GameServer.dateTimeServerStarted.getTime()));
		content = content.replace("%onlinecount%", String.valueOf(L2World.getInstance().getAllPlayersCount()));
		// content = content.replace("%topPvp%", Ranking.topPvp.get(0).getCharName() +" ("+ Ranking.topPvp.get(0).getPvpKills()+") PvP Kills.");
		// content = content.replace("%topRaid%", topRaid);
		final int mypos = Ranking.getMyPosition(Ranking.topPvp, activeChar);
		final RankData deco = Ranking.getRank(mypos);
		content = content.replace("%pvprank%", String.valueOf(mypos + 1) + " <font color=" + deco.getColor() + ">" + deco + "</font>");
		return content;
	}
	
	/**
	 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsewrite(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
	 */
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		// TODO Auto-generated method stub
	}
	
	/**
	 * @return
	 */
	public static AdvancedStats getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final AdvancedStats _instance = new AdvancedStats();
	}
	
	public String getServerRunTime()
	{
		int timeSeconds = (GameTimeController.getGameTicks() - 36000) / 10;
		String timeResult = "";
		if (timeSeconds >= 86400)
			timeResult = Integer.toString(timeSeconds / 86400) + " Days " + Integer.toString((timeSeconds % 86400) / 3600) + " hours";
		else
			timeResult = Integer.toString(timeSeconds / 3600) + " Hours " + Integer.toString((timeSeconds % 3600) / 60) + " mins";
		return timeResult;
	}
	
	@Override
	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		// TODO Auto-generated method stub
	}
}