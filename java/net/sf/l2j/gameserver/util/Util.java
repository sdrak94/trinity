/*
 * $Header: Util.java, 21/10/2005 23:17:40 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 21/10/2005 23:17:40 $
 * $Revision: 1 $
 * $Log: Util.java,v $
 * Revision 1  21/10/2005 23:17:40  luisantonioa
 * Added copyright notice
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
package net.sf.l2j.gameserver.util;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * General Utility functions related to Gameserver
 *
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public final class Util
{
	public static void handleIllegalPlayerAction(L2PcInstance actor, String message, int punishment)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new IllegalPlayerAction(actor, message, punishment), 5000);
	}
	
	public static String getRelativePath(File base, File file)
	{
		return file.toURI().getPath().substring(base.toURI().getPath().length());
	}
	
	/**
	 * Return degree value of object 2 to the horizontal line with object 1
	 * being the origin
	 */
	public static double calculateAngleFrom(L2Object obj1, L2Object obj2)
	{
		return calculateAngleFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}
	
	/**
	 * Return degree value of object 2 to the horizontal line with object 1
	 * being the origin
	 */
	public final static double calculateAngleFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		if (angleTarget < 0)
			angleTarget = 360 + angleTarget;
		return angleTarget;
	}
	
	public final static double convertHeadingToDegree(int clientHeading)
	{
		double degree = clientHeading / 182.044444444;
		return degree;
	}
	
	public final static int convertDegreeToClientHeading(double degree)
	{
		if (degree < 0)
			degree = 360 + degree;
		return (int) (degree * 182.044444444);
	}
	
	public final static int calculateHeadingFrom(L2Object obj1, L2Object obj2)
	{
		return calculateHeadingFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}
	
	public final static int calculateHeadingFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		if (angleTarget < 0)
			angleTarget = 360 + angleTarget;
		return (int) (angleTarget * 182.044444444);
	}
	
	public final static int calculateHeadingFrom(double dx, double dy)
	{
		double angleTarget = Math.toDegrees(Math.atan2(dy, dx));
		if (angleTarget < 0)
			angleTarget = 360 + angleTarget;
		return (int) (angleTarget * 182.044444444);
	}
	
	public static double calculateDistance(int x1, int y1, int z1, int x2, int y2)
	{
		return calculateDistance(x1, y1, 0, x2, y2, 0, false);
	}
	
	public static double calculateDistance(int x1, int y1, int z1, int x2, int y2, int z2, boolean includeZAxis)
	{
		double dx = (double) x1 - x2;
		double dy = (double) y1 - y2;
		
		if (includeZAxis)
		{
			double dz = z1 - z2;
			return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
		}
		else
			return Math.sqrt((dx * dx) + (dy * dy));
	}
	
	public static double calculateDistance(L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return 1000000;
		
		return calculateDistance(obj1.getPosition().getX(), obj1.getPosition().getY(), obj1.getPosition().getZ(), obj2.getPosition().getX(), obj2.getPosition().getY(), obj2.getPosition().getZ(), includeZAxis);
	}
	public static double calculateDistance(Location obj1, Location obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return 1000000;
		
		return calculateDistance(obj1.getX(), obj1.getY(), obj1.getZ(), obj2.getX(), obj2.getY(), obj2.getZ(), includeZAxis);
	}
	public static double calculateDistance(ILocational loc1, ILocational loc2, boolean includeZAxis)
	{
		return calculateDistance(loc1.getX(), loc1.getY(), loc1.getZ(), loc2.getX(), loc2.getY(), loc2.getZ(), includeZAxis, false);
	}
	
	public static double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2, boolean includeZAxis, boolean squared)
	{
		final double distance = Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + (includeZAxis ? Math.pow(z1 - z2, 2) : 0);
		return (squared) ? distance : Math.sqrt(distance);
	}
	/**
	 * Capitalizes the first letter of a string, and returns the result.<BR>
	 * (Based on ucfirst() function of PHP)
	 *
	 * @param String str
	 * @return String containing the modified string.
	 */
	public static String capitalizeFirst(String str)
	{
		str = str.trim();
		
		if (str.length() > 0 && Character.isLetter(str.charAt(0)))
			return str.substring(0, 1).toUpperCase() + str.substring(1);
		
		return str;
	}
	
	/**
	 * Capitalizes the first letter of every "word" in a string.<BR>
	 * (Based on ucwords() function of PHP)
	 *
	 * @param String str
	 * @return String containing the modified string.
	 */
	public static String capitalizeWords(String str)
	{
		char[] charArray = str.toCharArray();
		String result = "";
		
		// Capitalize the first letter in the given string!
		charArray[0] = Character.toUpperCase(charArray[0]);
		
		for (int i = 0; i < charArray.length; i++)
		{
			if (Character.isWhitespace(charArray[i]))
				charArray[i + 1] = Character.toUpperCase(charArray[i + 1]);
			
			result += Character.toString(charArray[i]);
		}
		
		return result;
	}
	public static boolean checkIfInRange(int range, ILocational loc1, ILocational loc2, boolean includeZAxis)
	{
		if ((loc1 == null) || (loc2 == null) || (loc1.getInstanceWorld() != loc2.getInstanceWorld()))
			return false;
		if (range == -1)
			return true; // not limited
		int radius = 0;
		if (loc1 instanceof L2Character)
			radius += ((L2Character)loc1).getActingPlayer().getTemplate().getCollisionRadius();
		if (loc2 instanceof L2Character)
			radius += ((L2Character)loc2).getActingPlayer().getTemplate().getCollisionRadius();
		return calculateDistance(loc1, loc2, includeZAxis, false) <= (range + radius);
	}
	public static double calculateDistance(ILocational loc1, ILocational loc2, boolean includeZAxis, boolean squared)
	{
		return calculateDistance(loc1.getX(), loc1.getY(), loc1.getZ(), loc2.getX(), loc2.getY(), loc2.getZ(), includeZAxis, squared);
	}
	/*
	 *  Checks if object is within range, adding collisionRadius
	 */
	public static boolean checkIfInRange(int range, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return false;
		if (obj1.getInstanceId() != obj2.getInstanceId())
			return false;
		if (range == -1)
			return true; // not limited
		
		int rad = 0;
		if (obj1 instanceof L2Character)
			rad += ((L2Character) obj1).getCollisionRadius();
		if (obj2 instanceof L2Character)
			rad += ((L2Character) obj2).getCollisionRadius();
		
		double dx = obj1.getX() - obj2.getX();
		double dy = obj1.getY() - obj2.getY();
		
		if (includeZAxis)
		{
			double dz = obj1.getZ() - obj2.getZ();
			double d = dx * dx + dy * dy + dz * dz;
			
			return d <= range * range + 2 * range * rad + rad * rad;
		}
		else
		{
			double d = dx * dx + dy * dy;
			
			return d <= range * range + 2 * range * rad + rad * rad;
		}
	}
	
	/*
	 *  Checks if object is within short (sqrt(int.max_value)) radius,
	 *  not using collisionRadius. Faster calculation than checkIfInRange
	 *  if distance is short and collisionRadius isn't needed.
	 *  Not for long distance checks (potential teleports, far away castles etc)
	 */
	public static boolean checkIfInShortRadius(int radius, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return false;
		if (radius == -1)
			return true; // not limited
		
		int dx = obj1.getX() - obj2.getX();
		int dy = obj1.getY() - obj2.getY();
		
		if (includeZAxis)
		{
			int dz = obj1.getZ() - obj2.getZ();
			return dx * dx + dy * dy + dz * dz <= radius * radius;
		}
		else
		{
			return dx * dx + dy * dy <= radius * radius;
		}
	}
	
	/**
	 * Returns the number of "words" in a given string.
	 *
	 * @param String str
	 * @return int numWords
	 */
	public static int countWords(String str)
	{
		return str.trim().split(" ").length;
	}
	
	/**
	 * Returns a delimited string for an given array of string elements.<BR>
	 * (Based on implode() in PHP)
	 *
	 * @param String[] strArray
	 * @param String strDelim
	 * @return String implodedString
	 */
	public static String implodeString(String[] strArray, String strDelim)
	{
		String result = "";
		
		for (String strValue : strArray)
			result += strValue + strDelim;
		
		return result;
	}
	
	/**
	 * Returns a delimited string for an given collection of string elements.<BR>
	 * (Based on implode() in PHP)
	 *
	 * @param Collection&lt;String&gt; strCollection
	 * @param String strDelim
	 * @return String implodedString
	 */
	public static String implodeString(Collection<String> strCollection, String strDelim)
	{
		return implodeString(strCollection.toArray(new String[strCollection.size()]), strDelim);
	}
	
	/**
	 * Returns the rounded value of val to specified number of digits
	 * after the decimal point.<BR>
	 * (Based on round() in PHP)
	 *
	 * @param float val
	 * @param int numPlaces
	 * @return float roundedVal
	 */
	public static float roundTo(float val, int numPlaces)
	{
		if (numPlaces <= 1)
			return Math.round(val);
		
		float exponent = (float) Math.pow(10, numPlaces);
		
		return (Math.round(val * exponent) / exponent);
	}
	
	public static boolean isAlphaNumeric(String text)
	{
		if (text == null)
			return false;
		boolean result = true;
		char[] chars = text.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			if (!Character.isLetterOrDigit(chars[i]))
			{
				result = false;
				break;
			}
		}
		return result;
	}
	
	/**
	 * Return amount of adena formatted with "," delimiter
	 * @param amount
	 * @return String formatted adena amount
	 */
	public static String formatAdena(long amount)
	{
		String s = "";
		long rem = amount % 1000;
		s = Long.toString(rem);
		amount = (amount - rem) / 1000;
		while (amount > 0)
		{
			if (rem < 99)
				s = '0' + s;
			if (rem < 9)
				s = '0' + s;
			rem = amount % 1000;
			s = Long.toString(rem) + "," + s;
			amount = (amount - rem) / 1000;
		}
		return s;
	}
	
	/**
	 * @param <T>
	 * @param array - the array to look into
	 * @param obj - the object to search for
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise.
	 */
	public static <T> boolean contains(T[] array, T obj)
	{
		for (T element : array)
		{
			if (element == obj)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param array - the array to look into
	 * @param obj - the integer to search for
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise
	 */
	public static boolean contains(int[] array, int obj)
	{
		for (int element : array)
		{
			if (element == obj)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param array - the array to look into
	 * @param obj - the object to search for
	 * @param ignoreCase
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise.
	 */
	public static boolean contains(String[] array, String obj, boolean ignoreCase)
	{
		for (String element : array)
		{
			if (element.equals(obj) || (ignoreCase && element.equalsIgnoreCase(obj)))
			{
				return true;
			}
		}
		return false;
	}

		/**
	 * @param text - the text to check
	 * @return {@code true} if {@code text} contains only numbers, {@code false} otherwise
	 */
	public static boolean isDigit(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		for (char c : text.toCharArray())
		{
			if (!Character.isDigit(c))
			{
				return false;
			}
		}
		return true;
	}

	public static String formatDate(final Date date, final String format)
	{
		final DateFormat dateFormat = new SimpleDateFormat(format);
		if (date != null)
			return dateFormat.format(date);
		return null;
	}

	public static int limit(int numToTest, int min, int max)
	{
		return (numToTest > max) ? max : ((numToTest < min) ? min : numToTest);
	}
	
	public static <A> void clearArray(final A[] array)
	{
		for (int i = 0; i < array.length; i++)
			array[i] = null;
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
			{	intArray[i] = Integer.parseInt(strArray[i].trim());
			}
			catch (Exception e)
			{	e.printStackTrace();
			}
		}
		return intArray;
	}
	

}
