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
package net.sf.l2j.util;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * A custom version of HashMap with extension for iterating without using temporary collection<br>
 * <br>
 * @author  Julian Version 1.0.1 (2008-02-07)<br>
 * Changes:<br>
 *      1.0.0 - Initial version.<br>
 *      1.0.1 - Made forEachP() final.<br>
 */

public class L2FastMap<K extends Object, V extends Object> extends HashMap<K,V>
{
	static final long serialVersionUID = 1L;
	
	/**
	 * Public inner interface used by ForEach iterations<br>
	 *
	 * @author  Julian
	 */
	public interface I2ForEach<K,V> {
		public boolean forEach(K key, V val);
	}

	public interface I2ForEachKey<K> {
		public boolean forEach(K key);
	}

	public interface I2ForEachValue<V> {
		public boolean forEach(V val);
	}

	/**
	 * Public method that iterate entire collection.<br>
	 * <br>
	 * @param func - a class method that must be executed on every element of collection.<br>
	 * @return - returns true if entire collection is iterated, false if it`s been interrupted by<br>
	 *             check method (I2ForEach.forEach())<br>
	 */
	public boolean ForEach(I2ForEach<K,V> func) {
		for (Map.Entry<K,V> e: this.entrySet())
			if (!func.forEach(e.getKey(),e.getValue())) return false;
		return true;
	}

	public boolean ForEachKey(I2ForEachKey<K> func) {
		for (K k: this.keySet())
			if (!func.forEach(k)) return false;
		return true;
	}

	public boolean ForEachValue(I2ForEachValue<V> func) {
		for (V v: this.values())
			if (!func.forEach(v)) return false;
		return true;
	}
}
