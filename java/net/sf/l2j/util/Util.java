/*
 * $Header: Util.java, 14-Jul-2005 03:27:51 luisantonioa Exp $
 * 
 * $Author: luisantonioa $ $Date: 14-Jul-2005 03:27:51 $ $Revision: 1 $ $Log:
 * Util.java,v $ Revision 1 14-Jul-2005 03:27:51 luisantonioa Added copyright
 * notice
 * 
 * 
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
package net.sf.l2j.util;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class ...
 * 
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */

public class Util
{
	public static boolean isInternalIP(String ipAddress)
	{
		java.net.InetAddress addr = null;
		try
		{
			addr = java.net.InetAddress.getByName(ipAddress);
			return addr.isSiteLocalAddress() || addr.isLoopbackAddress();
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public static String printData(byte[] data, int len)
	{
		final StringBuilder result = new StringBuilder(len * 4);
		
		int counter = 0;
		
		for (int i = 0; i < len; i++)
		{
			if (counter % 16 == 0)
			{
				result.append(fillHex(i, 4) + ": ");
			}
			
			result.append(fillHex(data[i] & 0xff, 2) + " ");
			counter++;
			if (counter == 16)
			{
				result.append("   ");
				
				int charpoint = i - 15;
				for (int a = 0; a < 16; a++)
				{
					int t1 = 0xFF & data[charpoint++];
					if (t1 > 0x1f && t1 < 0x80)
					{
						result.append((char) t1);
					}
					else
					{
						result.append('.');
					}
				}
				
				result.append("\n");
				counter = 0;
			}
		}
		
		int rest = data.length % 16;
		if (rest > 0)
		{
			for (int i = 0; i < 17 - rest; i++)
			{
				result.append("   ");
			}
			
			int charpoint = data.length - rest;
			for (int a = 0; a < rest; a++)
			{
				int t1 = 0xFF & data[charpoint++];
				if (t1 > 0x1f && t1 < 0x80)
				{
					result.append((char) t1);
				}
				else
				{
					result.append('.');
				}
			}
			
			result.append("\n");
		}
		
		return result.toString();
	}
	
	public static String fillHex(int data, int digits)
	{
		String number = Integer.toHexString(data);
		
		for (int i = number.length(); i < digits; i++)
		{
			number = "0" + number;
		}
		
		return number;
	}
	
	/**
	 * @param raw
	 * @return
	 */
	public static String printData(byte[] raw)
	{
		return printData(raw, raw.length);
	}
	
	public static String printData(ByteBuffer buf)
	{
		byte[] data = new byte[buf.remaining()];
		buf.get(data);
		String hex = Util.printData(data, data.length);
		buf.position(buf.position() - data.length);
		return hex;
	}
	public static String getFullClassName(int classId)
	{
		String name = null;
		switch (classId)
		{
			case 0:
				name = "Human Fighter";
				break;
			case 1:
				name = "Warrior";
				break;
			case 2:
				name = "Gladiator";
				break;
			case 3:
				name = "Warlord";
				break;
			case 4:
				name = "Human Knight";
				break;
			case 5:
				name = "Paladin";
				break;
			case 6:
				name = "Dark Avenger";
				break;
			case 7:
				name = "Rogue";
				break;
			case 8:
				name = "Treasure Hunter";
				break;
			case 9:
				name = "Hawkeye";
				break;
			case 10:
				name = "Human Mystic";
				break;
			case 11:
				name = "Human Wizard";
				break;
			case 12:
				name = "Sorcerer";
				break;
			case 13:
				name = "Necromancer";
				break;
			case 14:
				name = "Warlock";
				break;
			case 15:
				name = "Cleric";
				break;
			case 16:
				name = "Bishop";
				break;
			case 17:
				name = "Prophet";
				break;
			case 18:
				name = "Elven Fighter";
				break;
			case 19:
				name = "Elven Knight";
				break;
			case 20:
				name = "Temple Knight";
				break;
			case 21:
				name = "Sword Singer";
				break;
			case 22:
				name = "Elven Scout";
				break;
			case 23:
				name = "Plains Walker";
				break;
			case 24:
				name = "Silver Ranger";
				break;
			case 25:
				name = "Elven Mystic";
				break;
			case 26:
				name = "Elven Wizard";
				break;
			case 27:
				name = "Spellsinger";
				break;
			case 28:
				name = "Elemental Summoner";
				break;
			case 29:
				name = "Elven Oracle";
				break;
			case 30:
				name = "Elven Elder";
				break;
			case 31:
				name = "Dark Fighter";
				break;
			case 32:
				name = "Palus Knight";
				break;
			case 33:
				name = "Shillien Knight";
				break;
			case 34:
				name = "Bladedancer";
				break;
			case 35:
				name = "Assassin";
				break;
			case 36:
				name = "Abyss Walker";
				break;
			case 37:
				name = "Phantom Ranger";
				break;
			case 38:
				name = "Dark Mystic";
				break;
			case 39:
				name = "Dark Wizard";
				break;
			case 40:
				name = "Spellhowler";
				break;
			case 41:
				name = "Phantom Summoner";
				break;
			case 42:
				name = "Shillien Oracle";
				break;
			case 43:
				name = "Shillien Elder";
				break;
			case 44:
				name = "Orc Fighter";
				break;
			case 45:
				name = "Orc Raider";
				break;
			case 46:
				name = "Destroyer";
				break;
			case 47:
				name = "Monk";
				break;
			case 48:
				name = "Tyrant";
				break;
			case 49:
				name = "Orc Mystic";
				break;
			case 50:
				name = "Orc Shaman";
				break;
			case 51:
				name = "Overlord";
				break;
			case 52:
				name = "Warcryer";
				break;
			case 53:
				name = "Dwarven Fighter";
				break;
			case 54:
				name = "Scavenger";
				break;
			case 55:
				name = "Bounty Hunter";
				break;
			case 56:
				name = "Artisan";
				break;
			case 57:
				name = "Warsmith";
				break;
			case 88:
				name = "Duelist";
				break;
			case 89:
				name = "Dreadnought";
				break;
			case 90:
				name = "Phoenix Knight";
				break;
			case 91:
				name = "Hell Knight";
				break;
			case 92:
				name = "Sagittarius";
				break;
			case 93:
				name = "Adventurer";
				break;
			case 94:
				name = "Archmage";
				break;
			case 95:
				name = "Soultaker";
				break;
			case 96:
				name = "Arcana Lord";
				break;
			case 97:
				name = "Cardinal";
				break;
			case 98:
				name = "Hierophant";
				break;
			case 99:
				name = "Eva's Templar";
				break;
			case 100:
				name = "Sword Muse";
				break;
			case 101:
				name = "Wind Rider";
				break;
			case 102:
				name = "Moonlight Sentinel";
				break;
			case 103:
				name = "Mystic Muse";
				break;
			case 104:
				name = "Elemental Master";
				break;
			case 105:
				name = "Eva's Saint";
				break;
			case 106:
				name = "Shillien Templar";
				break;
			case 107:
				name = "Spectral Dancer";
				break;
			case 108:
				name = "Ghost Hunter";
				break;
			case 109:
				name = "Ghost Sentinel";
				break;
			case 110:
				name = "Storm Screamer";
				break;
			case 111:
				name = "Spectral Master";
				break;
			case 112:
				name = "Shillien Saint";
				break;
			case 113:
				name = "Titan";
				break;
			case 114:
				name = "Grand Khavatari";
				break;
			case 115:
				name = "Dominator";
				break;
			case 116:
				name = "Doom Cryer";
				break;
			case 117:
				name = "Fortune Seeker";
				break;
			case 118:
				name = "Maestro";
				break;
			case 123:
				name = "Kamael Soldier";
				break;
			case 124:
				name = "Kamael Soldier";
				break;
			case 125:
				name = "Trooper";
				break;
			case 126:
				name = "Warder";
				break;
			case 127:
				name = "Berserker";
				break;
			case 128:
				name = "Soul Breaker";
				break;
			case 129:
				name = "Soul Breaker";
				break;
			case 130:
				name = "Arbalester";
				break;
			case 131:
				name = "Doombringer";
				break;
			case 132:
				name = "Soul Hound";
				break;
			case 133:
				name = "Soul Hound";
				break;
			case 134:
				name = "Trickster";
				break;
			case 135:
				name = "Inspector";
				break;
			case 136:
				name = "Judicator";
				break;
			default:
				name = "Unknown";
		}
		return name;
	}
	public static int[] toIntArray(String str, String delim)
	{
		return toIntArray(str.split(delim));
	}
	
	public static int[] toIntArray(String str)
	{
		if (str == null)
			return new int[0];
		return toIntArray(str.replace(" ", "").split(","));
	}
	
	public static int[] toIntArray(String[] strArray)
	{
		if (strArray == null || strArray.length == 0)
			return new int[0];
		final int[] intArray = new int[strArray.length];
		for (int i=0;i<strArray.length;i++)
		{	try
			{	intArray[i] = Integer.parseInt(strArray[i]);
			}
			catch (Exception e)
			{	e.printStackTrace();
			}
		}
		return intArray;
	}
	
	public static int[] toIntArrayOrNull(String[] strArray)
	{
		final int[] array = toIntArray(strArray);
		return array.length > 0 ? array : null;
	}
	
	public static int[] toIntArrayOrNull(String data, String delim)
	{	if (data == null)
			return null;
		return toIntArrayOrNull(data.split(delim));
	}
	
	public static ArrayList<Integer> toIntList(String[] strArray)
	{
		final int[] intArray = toIntArray(strArray);
		final ArrayList<Integer> temp = new ArrayList<>(intArray.length);
		for (int i : intArray)
			temp.add(i);
		return temp;
		
	}


	public static boolean isMatchingRegexp(String text, String template)
	{
		Pattern pattern = null;
		try
		{
			pattern = Pattern.compile(template);
		}
		catch(PatternSyntaxException e) // invalid template
		{
			e.printStackTrace();
		}
		if(pattern == null)
			return false;
		Matcher regexp = pattern.matcher(text);
		return regexp.matches();
	}


}
