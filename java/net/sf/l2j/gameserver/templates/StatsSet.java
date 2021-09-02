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
package net.sf.l2j.gameserver.templates;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;

/**
 * @author mkizub
 * <BR>
 * This class is used in order to have a set of couples (key,value).<BR>
 * Methods deployed are accessors to the set (add/get value from its key) and addition of a whole set in the current one.
 */
public class StatsSet extends HashMap<String, Object>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7918316498247277289L;
	private static final Logger			_log	= Logger.getLogger(StatsSet.class.getName());
	private final Map<String, Object>	_set	= new FastMap<String, Object>();
	
	/**
	 * Returns the set of values
	 * 
	 * @return HashMap
	 */
	
	public final Map<String, Object> getSet()
	{
		return _set;
	}
	
	/**
	 * Add a set of couple values in the current set
	 * 
	 * @param newSet
	 *            : StatsSet pointing out the list of couples to add in the current set
	 */
	public void add(StatsSet newSet)
	{
		Map<String, Object> newMap = newSet.getSet();
		for (String key : newMap.keySet())
		{
			Object value = newMap.get(key);
			_set.put(key, value);
		}
	}
	
	/**
	 * Return the boolean associated to the key put in parameter ("name")
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @return boolean : value associated to the key
	 */
	public boolean getBool(String name)
	{
		Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Boolean value required, but not specified");
		if (val instanceof Boolean)
			return ((Boolean) val).booleanValue();
		try
		{
			return Boolean.parseBoolean((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Boolean value required, but found: " + val);
		}
	}
	
	/**
	 * Return the boolean associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param deflt
	 *            : boolean designating the default value if value associated with the key is null
	 * @return boolean : value of the key
	 */
	public boolean getBool(String name, boolean deflt)
	{
		Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Boolean)
			return ((Boolean) val).booleanValue();
		try
		{
			return Boolean.parseBoolean((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Boolean value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the int associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param deflt
	 *            : byte designating the default value if value associated with the key is null
	 * @return byte : value associated to the key
	 */
	public byte getByte(String name, byte deflt)
	{
		Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).byteValue();
		try
		{
			return Byte.parseByte((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Byte value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the byte associated to the key put in parameter ("name").
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @return byte : value associated to the key
	 */
	public byte getByte(String name)
	{
		Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Byte value required, but not specified");
		if (val instanceof Number)
			return ((Number) val).byteValue();
		try
		{
			return Byte.parseByte((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Byte value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the short associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param deflt
	 *            : short designating the default value if value associated with the key is null
	 * @return short : value associated to the key
	 */
	public short getShort(String name, short deflt)
	{
		Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).shortValue();
		try
		{
			return Short.parseShort((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Short value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the short associated to the key put in parameter ("name").
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @return short : value associated to the key
	 */
	public short getShort(String name)
	{
		Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Short value required, but not specified");
		if (val instanceof Number)
			return ((Number) val).shortValue();
		try
		{
			return Short.parseShort((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Short value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the int associated to the key put in parameter ("name").
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @return int : value associated to the key
	 */
	public int getInteger(String name)
	{
		Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Integer value required, but not specified");
		if (val instanceof Number)
			return ((Number) val).intValue();
		try
		{
			return Integer.parseInt((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the int associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param deflt
	 *            : int designating the default value if value associated with the key is null
	 * @return int : value associated to the key
	 */
	public int getInteger(String name, int deflt)
	{
		Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).intValue();
		try
		{
			return Integer.parseInt((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the int[] associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @return int[] : value associated to the key
	 */
	public int[] getIntegerArray(String name)
	{
		Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Integer value required, but not specified");
		if (val instanceof Number)
		{
			int[] result =
			{
				((Number) val).intValue()
			};
			return result;
		}
		int c = 0;
		String[] vals = ((String) val).split(";");
		int[] result = new int[vals.length];
		for (String v : vals)
		{
			try
			{
				result[c++] = Integer.parseInt(v);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("Integer value required, but found: " + val);
			}
		}
		return result;
	}
	public int[] getIntegerArray(String name, String seperator)
	{
		Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Integer value required, but not specified");
		if (val instanceof Number)
		{
			int[] result =
			{
				((Number) val).intValue()
			};
			return result;
		}
		int c = 0;
		String[] vals = ((String) val).split(seperator);
		int[] result = new int[vals.length];
		for (String v : vals)
		{
			try
			{
				result[c++] = Integer.parseInt(v);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("Integer value required, but found: " + val);
			}
		}
		return result;
	}
	/**
	 * Returns the long associated to the key put in parameter ("name").
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @return long : value associated to the key
	 */
	public long getLong(String name)
	{
		Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Integer value required, but not specified");
		if (val instanceof Number)
			return ((Number) val).longValue();
		try
		{
			return Long.parseLong((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the long associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param deflt
	 *            : long designating the default value if value associated with the key is null
	 * @return long : value associated to the key
	 */
	public long getLong(String name, int deflt)
	{
		Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).longValue();
		try
		{
			return Long.parseLong((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the float associated to the key put in parameter ("name").
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @return float : value associated to the key
	 */
	public float getFloat(String name)
	{
		Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Float value required, but not specified");
		if (val instanceof Number)
			return ((Number) val).floatValue();
		try
		{
			return (float) Double.parseDouble((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the float associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param deflt
	 *            : float designating the default value if value associated with the key is null
	 * @return float : value associated to the key
	 */
	public float getFloat(String name, float deflt)
	{
		Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).floatValue();
		try
		{
			return (float) Double.parseDouble((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the double associated to the key put in parameter ("name").
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @return double : value associated to the key
	 */
	public double getDouble(String name)
	{
		Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Float value required, but not specified");
		if (val instanceof Number)
			return ((Number) val).doubleValue();
		try
		{
			return Double.parseDouble((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the double associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param deflt
	 *            : float designating the default value if value associated with the key is null
	 * @return double : value associated to the key
	 */
	public double getDouble(String name, float deflt)
	{
		Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (val instanceof Number)
			return ((Number) val).doubleValue();
		try
		{
			return Double.parseDouble((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the String associated to the key put in parameter ("name").
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @return String : value associated to the key
	 */
	public String getString(String name)
	{
		Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("String value required, but not specified");
		return String.valueOf(val);
	}
	
	/**
	 * Returns the String associated to the key put in parameter ("name"). If the value associated to the key is null, this method returns the value of the parameter
	 * deflt.
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param deflt
	 *            : String designating the default value if value associated with the key is null
	 * @return String : value associated to the key
	 */
	public String getString(String name, String deflt)
	{
		Object val = _set.get(name);
		if (val == null)
			return deflt;
		return String.valueOf(val);
	}
	
	/**
	 * Returns an enumeration of &lt;T&gt; from the set
	 * 
	 * @param <T>
	 *            : Class of the enumeration returned
	 * @param name
	 *            : String designating the key in the set
	 * @param enumClass
	 *            : Class designating the class of the value associated with the key in the set
	 * @return Enum<T>
	 */
	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> T getEnum(String name, Class<T> enumClass)
	{
		Object val = _set.get(name);
		if (val == null)
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but not specified");
		if (enumClass.isInstance(val))
			return (T) val;
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(val));
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + "required, but found: " + val);
		}
	}
	
	/**
	 * Returns an enumeration of &lt;T&gt; from the set. If the enumeration is empty, the method returns the value of the parameter "deflt".
	 * 
	 * @param <T>
	 *            : Class of the enumeration returned
	 * @param name
	 *            : String designating the key in the set
	 * @param enumClass
	 *            : Class designating the class of the value associated with the key in the set
	 * @param deflt
	 *            : <T> designating the value by default
	 * @return Enum<T>
	 */
	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> T getEnum(String name, Class<T> enumClass, T deflt)
	{
		Object val = _set.get(name);
		if (val == null)
			return deflt;
		if (enumClass.isInstance(val))
			return (T) val;
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(val));
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + "required, but found: " + val);
		}
	}
	
	/**
	 * Add the String hold in param "value" for the key "name"
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param value
	 *            : String corresponding to the value associated with the key
	 */
	public void set(String name, String value)
	{
		_set.put(name, value);
	}
	
	/**
	 * Add the boolean hold in param "value" for the key "name"
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param value
	 *            : boolean corresponding to the value associated with the key
	 */
	public void set(String name, boolean value)
	{
		_set.put(name, value);
	}
	
	/**
	 * Add the int hold in param "value" for the key "name"
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param value
	 *            : int corresponding to the value associated with the key
	 */
	public void set(String name, int value)
	{
		_set.put(name, value);
	}
	
	/**
	 * Safe version of "set". Expected values are within [min, max[<br>
	 * Add the int hold in param "value" for the key "name".
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param value
	 *            : int corresponding to the value associated with the key
	 */
	public void safeSet(String name, int value, int min, int max, String reference)
	{
		if (min <= max && (value < min || value >= max))
		{
			if (Config.ASSERT)
				throw new AssertionError("Incorrect value: " + value + "for: " + name + "Ref: " + reference);
			_log.log(Level.SEVERE, "Incorrect value: " + value + "for: " + name + "Ref: " + reference);
		}
		set(name, value);
	}
	
	/**
	 * Add the double hold in param "value" for the key "name"
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param value
	 *            : double corresponding to the value associated with the key
	 */
	public void set(String name, double value)
	{
		_set.put(name, value);
	}
	
	/**
	 * Add the long hold in param "value" for the key "name"
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param value
	 *            : double corresponding to the value associated with the key
	 */
	public void set(String name, long value)
	{
		_set.put(name, value);
	}
	
	/**
	 * Add the Enum hold in param "value" for the key "name"
	 * 
	 * @param name
	 *            : String designating the key in the set
	 * @param value
	 *            : Enum corresponding to the value associated with the key
	 */
	public void set(String name, Enum<?> value)
	{
		_set.put(name, value);
	}

	public boolean getBoolean(String key, boolean defaultValue)
	{
		Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		if (val instanceof Boolean)
		{
			return ((Boolean) val).booleanValue();
		}
		try
		{
			return Boolean.parseBoolean((String) val);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	public int getInt(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Integer value required, but not specified: " + key + "!");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).intValue();
		}
		
		try
		{
			return Integer.parseInt((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val + "!");
		}
	}
	
	public int getInt(String key, int defaultValue)
	{
		Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		if (val instanceof Number)
		{
			return ((Number) val).intValue();
		}
		try
		{
			return Integer.parseInt((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}

	@SuppressWarnings("unchecked")
	public <E extends Enum<E>> E getEnumU(final String key, final Class<E> enumClass, final E defaultValue)
	{
		Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		if (enumClass.isInstance(val))
		{
			return (E) val;
		}
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(val).toUpperCase());
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but found: " + val);
		}
	}
}
