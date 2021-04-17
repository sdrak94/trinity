package net.sf.l2j.gameserver.util;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.regex.Pattern;

public class Strings
{
	private static final char hex[] =
	{
		'0',
		'1',
		'2',
		'3',
		'4',
		'5',
		'6',
		'7',
		'8',
		'9',
		'a',
		'b',
		'c',
		'd',
		'e',
		'f'
	};
	
	public static String bytesToString(byte[] b)
	{
		String ret = "";
		for (byte element : b)
		{
			ret += String.valueOf(hex[(element & 0xF0) >> 4]);
			ret += String.valueOf(hex[element & 0x0F]);
		}
		return ret;
	}
	
	public static String addSlashes(String s)
	{
		if (s == null)
		{
			return "";
		}
		s = s.replace("\\", "\\\\");
		s = s.replace("\"", "\\\"");
		s = s.replace("@", "\\@");
		s = s.replace("'", "\\'");
		return s;
	}
	
	public static String stripSlashes(String s)
	{
		if (s == null)
		{
			return "";
		}
		s = s.replace("\\'", "'");
		s = s.replace("\\\\", "\\");
		return s;
	}
	
	public static Integer parseInt(Object x)
	{
		if (x == null)
		{
			return 0;
		}
		if (x instanceof Integer)
		{
			return (Integer) x;
		}
		if (x instanceof Double)
		{
			return ((Double) x).intValue();
		}
		if (x instanceof Boolean)
		{
			return (Boolean) x ? -1 : 0;
		}
		Integer res = 0;
		try
		{
			res = Integer.parseInt("" + x);
		}
		catch (Exception e)
		{
		}
		return res;
	}
	
	public static Double parseFloat(Object x)
	{
		if (x instanceof Double)
		{
			return (Double) x;
		}
		if (x instanceof Integer)
		{
			return 0.0 + (Integer) x;
		}
		if (x == null)
		{
			return 0.0;
		}
		Double res = 0.0;
		try
		{
			res = Double.parseDouble("" + x);
		}
		catch (Exception e)
		{
		}
		return res;
	}
	
	public static Boolean parseBoolean(Object x)
	{
		if (x instanceof Integer)
		{
			return (Integer) x != 0;
		}
		if (x == null)
		{
			return false;
		}
		if (x instanceof Boolean)
		{
			return (Boolean) x;
		}
		if (x instanceof Double)
		{
			return Math.abs((Double) x) < 0.00001;
		}
		return !("" + x).equals("");
	}
	
	public static String replace(String str, String regex, int flags, String replace)
	{
		return Pattern.compile(regex, flags).matcher(str).replaceAll(replace);
	}
	
	public static boolean matches(String str, String regex, int flags)
	{
		return Pattern.compile(regex, flags).matcher(str).matches();
	}
	
	public static String bbParse(String s)
	{
		if (s == null)
		{
			return null;
		}
		s = s.replace("\r", "");
		s = s.replaceAll("(\\s|\"|\'|\\(|^|\n)\\*(.*?)\\*(\\s|\"|\'|\\)|\\?|\\.|!|:|;|,|$|\n)", "$1<font color=\"LEVEL\">$2</font>$3");
		s = replace(s, "^!(.*?)$", Pattern.MULTILINE, "<font color=\"LEVEL\">$1</font>\n\n");
		s = s.replaceAll("%%\\s*\n", "<br1>");
		s = s.replaceAll("\n\n+", "<br>");
		s = replace(s, "\\[([^\\]\\|]*?)\\|([^\\]]*?)\\]", Pattern.DOTALL, "<a action=\"bypass -h $1\">$2</a>");
		s = s.replaceAll(" @", "\" msg=\"");
		return s;
	}
	
	public static String utf2win(String utfString)
	{
		try
		{
			return new String(utfString.getBytes("Cp1251"));
		}
		catch (UnsupportedEncodingException uee)
		{
			return utfString;
		}
	}
	
	/**
	 * Skleivalka for strings
	 * @param glueStr - string delimiter can be an empty string or null
	 * @param strings - an array of strings that need to stick together
	 * @param startIdx - initial index if you specify a negative then it will be taken away from the number of lines
	 * @param maxCount - meskimum elements if 0 - return an empty string if the negative is not take account of what is
	 * @return
	 */
	public static String joinStrings(String glueStr, String[] strings, int startIdx, int maxCount)
	{
		String result = "";
		if (startIdx < 0)
		{
			startIdx += strings.length;
			if (startIdx < 0)
			{
				return result;
			}
		}
		while ((startIdx < strings.length) && (maxCount != 0))
		{
			if (!result.isEmpty() && (glueStr != null) && !glueStr.isEmpty())
			{
				result += glueStr;
			}
			result += strings[startIdx++];
			maxCount--;
		}
		return result;
	}
	
	/**
	 * Skleivalka for strings
	 * @param glueStr - string delimiter can be an empty string or null
	 * @param strings - an array of strings that need to stick together
	 * @param startIdx - initial index if you specify a negative then it will be taken away from the number of lines
	 * @return
	 */
	public static String joinStrings(String glueStr, String[] strings, int startIdx)
	{
		return joinStrings(glueStr, strings, startIdx, -1);
	}
	
	/**
	 * Skleivalka for strings
	 * @param glueStr - string delimiter can be an empty string or null
	 * @param strings - an array of strings that need to stick together
	 * @return
	 */
	public static String joinStrings(String glueStr, String[] strings)
	{
		return joinStrings(glueStr, strings, 0);
	}
	
	public static String stripToSingleLine(String s)
	{
		if (s.isEmpty())
		{
			return s;
		}
		s = s.replaceAll("\\\\n", "\n");
		int i = s.indexOf("\n");
		if (i > -1)
		{
			s = s.substring(0, i);
		}
		return s;
	}
	
	public static String FormatTable(Collection<String> tds, int rows, boolean appendTD)
	{
		String result = "";
		int i = 0;
		for (String td : tds)
		{
			if (i == 0)
			{
				result += "<tr>";
			}
			result += appendTD ? "<td>" + td + "</td>" : td;
			i++;
			if (i == rows)
			{
				result += "</tr>";
				i = 0;
			}
		}
		if ((i > 0) && (i < rows))
		{
			while (i < rows)
			{
				result += "<td></td>";
				i++;
			}
			result += "</tr>";
		}
		return result;
	}
	
	public static String htmlButton(String value, String action, int width)
	{
		return htmlButton(value, action, width, 22);
	}
	
	public static String htmlButton(String value, String action, int width, int height)
	{
		return String.format("<button value=\"%s\" action=\"%s\" back=\"L2UI_CT1.Button_DF_Small_Down\" width=%d height=%d fore=\"L2UI_CT1.Button_DF_Small\">", value, action, width, height);
	}
}