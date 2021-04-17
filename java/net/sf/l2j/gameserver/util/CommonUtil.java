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
package net.sf.l2j.gameserver.util;

import java.net.UnknownHostException;
import java.util.logging.Logger;

public class CommonUtil
{
	private static final Logger _log = Logger.getLogger(CommonUtil.class.getName());
	
	public static boolean isInternalIP(final String ipAddress)
	{
		java.net.InetAddress addr = null;
		try
		{
			addr = java.net.InetAddress.getByName(ipAddress);
			return addr.isSiteLocalAddress() || addr.isLoopbackAddress();
		}
		catch (final UnknownHostException e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	public static String printData(final byte[] data, final int len)
	{
		final StringBuilder result = new StringBuilder();
		int counter = 0;
		for (int i = 0; i < len; i++)
		{
			if (counter % 16 == 0)
				result.append(fillHex(i, 4) + ": ");
			result.append(fillHex(data[i] & 0xff, 2) + " ");
			counter++;
			if (counter == 16)
			{
				result.append("   ");
				int charpoint = i - 15;
				for (int a = 0; a < 16; a++)
				{
					final int t1 = data[charpoint++];
					if (t1 > 0x1f && t1 < 0x80)
						result.append((char) t1);
					else
						result.append('.');
				}
				result.append("\n");
				counter = 0;
			}
		}
		final int rest = data.length % 16;
		if (rest > 0)
		{
			for (int i = 0; i < 17 - rest; i++)
				result.append("   ");
			int charpoint = data.length - rest;
			for (int a = 0; a < rest; a++)
			{
				final int t1 = data[charpoint++];
				if (t1 > 0x1f && t1 < 0x80)
					result.append((char) t1);
				else
					result.append('.');
			}
			result.append("\n");
		}
		return result.toString();
	}
	
	public static String fillHex(final int data, final int digits)
	{
		String number = Integer.toHexString(data);
		for (int i = number.length(); i < digits; i++)
			number = "0" + number;
		return number;
	}
	
	public static String printData(final byte[] raw)
	{
		return printData(raw, raw.length);
	}
	
	public static void printSection(String s)
	{
		s = "=[ " + s + " ]";
		while (s.length() < 78)
			s = "-" + s;
		_log.info(s);
	}
	
	public static void printSection(Logger log, String s)
	{
		s = "=[ " + s + " ]";
		while (s.length() < 78)
			s = "-" + s;
		log.info(s);
	}
	
	public static String toHexString(int num)
	{
		return "0x" + String.format("%02d", num).toUpperCase();
	}
}