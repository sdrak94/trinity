package net.sf.l2j.gameserver.communitybbs.boards;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;

public class TopPlayers
{
	public int pos;
	private TextBuilder _topList = new TextBuilder();
	String sort = "";
	
	public TopPlayers(String file)
	{
		loadDB(file);
	}
	
	private void loadDB(String file)
	{
		Connection con = null;
		
		switch (file)
		{
			case "toppvp":
				sort = "pvpkills";
				break;
			case "toppk":
				sort = "pkkills";
				break;
			case "topfame":
				sort = "fame";
				break;
			case "topadena":
				sort = "SUM(it.count)";
				break;
			//case "toprbrank":
			//	sort = "SUM(chr.points)";
			//	break;
			case "toponline":
				sort = "onlinetime";
				break;
			default:
				break;
		
		}
		
		try
		{
			pos = 0;
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT ch.char_name, ch.title, ch.clanid, ch.pkkills, ch.pvpkills, ch.fame, ch.onlinetime, ch.base_class, ch.online FROM characters ch GROUP BY ch.charId ORDER BY " + sort + " DESC LIMIT 0" + Config.TOP_PLAYER_RESULTS);
			//PreparedStatement statement = con.prepareStatement("SELECT SUM(chr.points), SUM(it.count), ch.char_name, ch.title, ch.clanid, ch.pkkills, ch.pvpkills, ch.fame, ch.onlinetime, ch.base_class, ch.online FROM characters ch LEFT JOIN character_raid_points chr ON ch.charId=chr.charId LEFT OUTER JOIN items it ON ch.charId=it.owner_id WHERE item_id=6393 GROUP BY ch.charId ORDER BY " + sort + " DESC LIMIT 0" + 3);
			
			ResultSet result = statement.executeQuery();
			
			while (result.next())
			{
				boolean status = false;
				pos++;
				if (result.getString("char_name").contains("[GM]"))
				{
					continue;
				}
				if (result.getInt("online") == 1)
					status = true;
				String timeon = getPlayerRunTime(result.getInt("ch.onlinetime"));
				addChar(pos, result.getString("ch.char_name"), result.getString("ch.title"),result.getInt("ch.clanid"), result.getInt("base_class"), result.getInt("ch.pvpkills"), result.getInt("ch.pkkills"), result.getInt("ch.fame"), timeon, status);

				
			}
			result.close();
			statement.close();
		}
		catch (Exception e)
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
	}
	
	
	public String loadTopList()
	{
		return _topList.toString();
	}
	private void addChar(int posList, String name, String title, int clanid, int classid, int pvp, int pk, int fame, String onlinename, boolean isOnline)
	{
	
/*		_topList.append("<table border=0 cellspacing=0 cellpadding=2 bgcolor=050505 height=" + Config.TOP_PLAYER_ROW_HEIGHT + "><tr><td FIXWIDTH=5></td>");
		_topList.append("<td FIXWIDTH=20>" + position(posList) + ".</td>");
		_topList.append("<td FIXWIDTH=180>" + name + "</td>");
		_topList.append("<td FIXWIDTH=175>" + className(classid) + "</td>");
		_topList.append("<td FIXWIDTH=80>" + pvp + "</td>");
		_topList.append("<td FIXWIDTH=80>" + pk + "</td>");
		_topList.append("<td FIXWIDTH=80>" + fame + "</td>");
		//_topList.append("<td FIXWIDTH=150>" + adenas + "</td>");
		_topList.append("<td FIXWIDTH=148>" + online + "</td>");
		_topList.append("<td FIXWIDTH=65>" + ((isOnline) ? "<a action=\"bypass _bbsloc;playerinfo;"+online+"</a>" : "<font color=CC0000>Offline</font>") + "</td>");
		_topList.append("</tr></table><img src=\"L2UI.Squaregray\" width=\"758\" height=\"1\">");
		*/
		onlinename = "<td width=180 valign=top><font color=c0c0c0><a action=\"bypass _bbsloc;playerinfo;" +name+ "\">"+name+"</a></font></td>";
		String offlinename = "<font color=c0c0c0>" +name+ "</font>";
		
		L2Clan clan = ClanTable.getInstance().getClan(clanid);
		String clanname = "-";
		if (clan != null)
			clanname = clan.getName()+ " (Lvl "+clan.getLevel()+")";
		

			
		
		if (title == null)
			title = "";
		title = title.replaceAll("<", "&lt;");
		title = title.replaceAll(">", "&gt;");
		_topList.append("<table width=760><tr><td height=30>");
		_topList.append("<table width=760 bgcolor=" + getMainStatsTableColor(0) + " height=30>");
		_topList.append("<tr><td width=45>");
		_topList.append("<font name=\"hs12\" color=\"99BBFF\">"+ String.valueOf(pos) +"</font>");
		_topList.append("</td><td width=220>");
		_topList.append("<center><font name=\"hs12\" color=\"99BBFF\">" + ((isOnline)?  onlinename : offlinename +"</font></center>"));
		_topList.append("</td><td width=100>");
		_topList.append("<center><font name=\"hs12\" color=\"99BBFF\">"+ title +"</font></center>");
		_topList.append("</td><td width=100>");
		_topList.append("<center><font name=\"hs12\" color=\"99BBFF\">"+ className(classid) +"</font></center>");
		_topList.append("</td><td width=80>");
		_topList.append("<center><font name=\"hs12\" color=\"99BBFF\">"+ pvp +"</font></center>");
		_topList.append("</td><td width=220>");
		_topList.append("<center><font name=\"hs12\" color=\"99BBFF\">"+ pk +"</font></center>");
		_topList.append("</td><td width=220>");
		_topList.append("<center><font name=\"hs12\" color=\"99BBFF\">"+ pk +"</font></center>");
		_topList.append("</td><td width=220>");
		_topList.append("<center><font name=\"hs12\" color=\"99BBFF\">"+ fame +"</font></center>");
		_topList.append("</td></tr></table></td></tr>");
/*		_topList.append("<table width=740>");
		_topList.append("<tr>");
		_topList.append("<td width=740 align=center>");
		_topList.append("<table width=740 height=25 bgcolor=212121>");
		_topList.append("<tr>");
		_topList.append("<td align=center>");
		_topList.append("<table>");
		_topList.append("<tr>");
		_topList.append("<td width=50 align=center valign=top><font name=__SYSTEMWORLDFONT color=b09979>"+ String.valueOf(pos) +"</font></td>");
		_topList.append("<td width=200 align=center valign=top>" + ((isOnline)?  onlinename : offlinename +"</td>"));
		_topList.append("<td width=200 align=center valign=top>");
		_topList.append("<font color=c0c0c0>"+ title +"</font>");
		_topList.append("</td>");
		_topList.append("<td width=200 align=center valign=top>");
		_topList.append("<font color=c0c0c0>"+ clanname +"</font>");
		_topList.append("</td>");
		_topList.append("<td width=200 align=center valign=top>");
		_topList.append("<font color=c0c0c0>"+ className(classid) +"</font>");
		_topList.append("</td>");
		_topList.append("<td width=100 align=center valign=top>");
		_topList.append("<font color=c0c0c0>"+ pvp +"</font>");
		_topList.append("</td>");
		_topList.append("<td width=100 align=center valign=top>");
		_topList.append("<font color=c0c0c0>"+ pk +"</font>");
		_topList.append("</td>");
		_topList.append("<td width=100 align=center valign=top>");
		_topList.append("<font color=c0c0c0>"+ fame +"</font>");
		_topList.append("</td>");
		_topList.append("</tr>");
		_topList.append("</table>");
		_topList.append("</td>");
		_topList.append("</tr>");
		_topList.append("</table>");
		_topList.append("</td>");
		_topList.append("</tr>");
		_topList.append("</table>");
		_topList.append("<img src=L2UI_CT1_CN.Windows.TitleBG width=769 height=3>");*/

	}
	
	private String getMainStatsTableColor(int index)
	{
		return (index % 2) == 0 ? "222320" : "191919";
	}

public final static String position(int posid)
	{
	/*	Map<Integer, String> posList;
		posList = new FastMap<Integer, String>();

		posList.put(1, "<img src=l2ui_ch3.party_summmon_num1 width=16 height=16>");
		posList.put(2, "<img src=l2ui_ch3.party_summmon_num2 width=16 height=16>");
		posList.put(3, "<img src=l2ui_ch3.party_summmon_num3 width=16 height=16>");
		posList.put(4, "<img src=l2ui_ch3.party_summmon_num4 width=16 height=16>");
		posList.put(5, "<img src=l2ui_ch3.party_summmon_num5 width=16 height=16>");
		posList.put(6, "<img src=l2ui_ch3.party_summmon_num6 width=16 height=16>");
		posList.put(7, "<img src=l2ui_ch3.party_summmon_num7 width=16 height=16>");
		posList.put(8, "<img src=l2ui_ch3.party_summmon_num8 width=16 height=16>");
		posList.put(9, "<img src=l2ui_ch3.party_summmon_num9 width=16 height=16>");
		posList.put(10, "<img src=l2ui_ch3.party_summmon_num10 width=16 height=16>");
		return posList.get(posid);
		*/
	return "<img src=l2ui_ch3.party_summmon_num"+ posid + " width=16 height=16>";
	}
	public final static String className(int classid)
	{
		Map<Integer, String> classList;
		classList = new FastMap<Integer, String>();
		classList.put(0, "Fighter");
		classList.put(1, "Warrior");
		classList.put(2, "Gladiator");
		classList.put(3, "Warlord");
		classList.put(4, "Knight");
		classList.put(5, "Paladin");
		classList.put(6, "Dark Avenger");
		classList.put(7, "Rogue");
		classList.put(8, "Treasure Hunter");
		classList.put(9, "Hawkeye");
		classList.put(10, "Mage");
		classList.put(11, "Wizard");
		classList.put(12, "Sorcerer");
		classList.put(13, "Necromancer");
		classList.put(14, "Warlock");
		classList.put(15, "Cleric");
		classList.put(16, "Bishop");
		classList.put(17, "Prophet");
		classList.put(18, "Elven Fighter");
		classList.put(19, "Elven Knight");
		classList.put(20, "Temple Knight");
		classList.put(21, "Swordsinger");
		classList.put(22, "Elven Scout");
		classList.put(23, "Plains Walker");
		classList.put(24, "Silver Ranger");
		classList.put(25, "Elven Mage");
		classList.put(26, "Elven Wizard");
		classList.put(27, "Spellsinger");
		classList.put(28, "Elemental Summoner");
		classList.put(29, "Oracle");
		classList.put(30, "Elder");
		classList.put(31, "Dark Fighter");
		classList.put(32, "Palus Knightr");
		classList.put(33, "Shillien Knight");
		classList.put(34, "Bladedancer");
		classList.put(35, "Assasin");
		classList.put(36, "Abyss Walker");
		classList.put(37, "Phantom Ranger");
		classList.put(38, "Dark Mage");
		classList.put(39, "Dark Wizard");
		classList.put(40, "Spellhowler");
		classList.put(41, "Phantom Summoner");
		classList.put(42, "Shillien Oracle");
		classList.put(43, "Shilien Elder");
		classList.put(44, "Orc Fighter");
		classList.put(45, "Orc Raider");
		classList.put(46, "Destroyer");
		classList.put(47, "Orc Monk");
		classList.put(48, "Tyrant");
		classList.put(49, "Orc Mage");
		classList.put(50, "Orc Shaman");
		classList.put(51, "Overlord");
		classList.put(52, "Warcryer");
		classList.put(53, "Dwarven Fighter");
		classList.put(54, "Scavenger");
		classList.put(55, "Bounty Hunter");
		classList.put(56, "Artisan");
		classList.put(57, "Warsmith");
		classList.put(88, "Duelist");
		classList.put(89, "Dreadnought");
		classList.put(90, "Phoenix Knight");
		classList.put(91, "Hell Knight");
		classList.put(92, "Sagittarius");
		classList.put(93, "Adventurer");
		classList.put(94, "Archmage");
		classList.put(95, "Soultaker");
		classList.put(96, "Arcana Lord");
		classList.put(97, "Cardinal");
		classList.put(98, "Hierophant");
		classList.put(99, "Evas Templar");
		classList.put(100, "Sword Muse");
		classList.put(101, "Wind Rider");
		classList.put(102, "Moonlight Sentinel");
		classList.put(103, "Mystic Muse");
		classList.put(104, "Elemental Master");
		classList.put(105, "Evas Saint");
		classList.put(106, "Shillien Templar");
		classList.put(107, "Spectral Dancer");
		classList.put(108, "Ghost Hunter");
		classList.put(109, "Ghost Sentinel");
		classList.put(110, "Storm Screamer");
		classList.put(111, "Spectral Master");
		classList.put(112, "Shillien Saint");
		classList.put(113, "Titan");
		classList.put(114, "Grand Khavatari");
		classList.put(115, "Dominator");
		classList.put(116, "Doomcryer");
		classList.put(117, "Fortune Seeker");
		classList.put(118, "Maestro");
		classList.put(123, "Male Soldier");
		classList.put(124, "Female Soldier");
		classList.put(125, "Trooper");
		classList.put(126, "Warder");
		classList.put(127, "Berserker");
		classList.put(128, "Male Soulbreaker");
		classList.put(129, "Female Soulbreaker");
		classList.put(130, "Arbalester");
		classList.put(131, "Doombringer");
		classList.put(132, "Male Soulhound");
		classList.put(133, "Female Soulhound");
		classList.put(134, "Trickster");
		classList.put(135, "Inspector");
		classList.put(136, "Judicator");
		
		return classList.get(classid);
	}
	
	public String getPlayerRunTime(int secs)
	{
		String timeResult = "";
		if (secs >= 86400)
			timeResult = Integer.toString(secs / 86400) + " Days " + Integer.toString((secs % 86400) / 3600) + " hours";
		else
			timeResult = Integer.toString(secs / 3600) + " Hours " + Integer.toString((secs % 3600) / 60) + " mins";
		return timeResult;
	}
}