package net.sf.l2j.gameserver.communitybbs.boards;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;

public class HeroeList
{
	private int _posId;
	private TextBuilder _heroeList = new TextBuilder();
	
	public HeroeList()
	{
		loadFromDB();
	}
	
	private void loadFromDB()
	{
		java.sql.Connection con = null;
		try
		{
			_posId = 0;
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT h.count, h.played, ch.char_name, ch.base_class, ch.online, cl.clan_name, cl.ally_name FROM heroes h LEFT JOIN characters ch ON ch.charId=h.charId LEFT OUTER JOIN clan_data cl ON cl.clan_id=ch.clanid ORDER BY h.count DESC, ch.char_name ASC LIMIT 20");
			
			ResultSet result = statement.executeQuery();
			
			while (result.next())
			{
				boolean status = false;
				_posId = _posId + 1;
				
				if (result.getInt("online") == 1)
					status = true;
				
				addPlayerToList(_posId, result.getInt("count"), result.getInt("played"), result.getString("char_name"), result.getInt("base_class"), result.getString("clan_name"), result.getString("ally_name"), status);
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
	
	public String loadHeroeList()
	{
		return _heroeList.toString();
	}
	
	private void addPlayerToList(int objId, int count, int played, String name, int ChrClass, String clan, String ally, boolean isOnline)
	{
		_heroeList.append("<table border=0 cellspacing=0 cellpadding=2 width=750>");
		_heroeList.append("<tr>");
		_heroeList.append("<td FIXWIDTH=10></td>");
		_heroeList.append("<td FIXWIDTH=40>" + objId + ".</td>");
		_heroeList.append("<td FIXWIDTH=150>" + name + "</td>");
		_heroeList.append("<td FIXWIDTH=160>" + className(ChrClass) + "</td>");
		_heroeList.append("<td FIXWIDTH=80>" + count + "</td>");
		_heroeList.append("<td FIXWIDTH=80>" + played + "</td>");
		_heroeList.append("<td FIXWIDTH=160>" + clan + "</td>");
		_heroeList.append("<td FIXWIDTH=160>" + ally + "</td>");
		_heroeList.append("<td FIXWIDTH=70>" + ((isOnline) ? "<font color=99FF00>Online</font>" : "<font color=CC0000>Offline</font>") + "</td>");
		_heroeList.append("<td FIXWIDTH=5></td>");
		_heroeList.append("</tr>");
		_heroeList.append("</table>");
		_heroeList.append("<img src=\"L2UI.Squaregray\" width=\"740\" height=\"1\">");
	}
	
	public final static String className(int classId)
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
		
		return classList.get(classId);
	}
}