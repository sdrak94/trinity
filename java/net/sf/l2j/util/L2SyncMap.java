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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.sf.l2j.util.L2FastMap.I2ForEach;
import net.sf.l2j.util.L2FastMap.I2ForEachKey;
import net.sf.l2j.util.L2FastMap.I2ForEachValue;

/**
 *
 * Fully synchronized version of L2FastMap class.<br>
 * In addition it`s provide ForEach methods and interfaces that can be used for iterating collection<br>
 * without using iterators. As addition its provide full lock on entire class if needed<br>
 * <font color="red">WARNING!!! methods: keySet(), values() and entrySet() are removed!</font>
 * <br>
 * @author  Julian Version: 1.0.0 <2008-02-07> - Original release
 * @author  Julian Varsion: 1.0.1 <2008-06-17> - Changed underlayng map to L2FastMap
 */
public class L2SyncMap<K extends Object, V extends Object> implements Map<K, V>
{
    static final long serialVersionUID = 1L;
    private final L2FastMap<K, V> _map = new L2FastMap<K, V>();
    
    public synchronized V put(K key, V value) {
    	return _map.put(key, value);
    }
    
    public synchronized V get(Object key) {
        return _map.get(key);
    }
    
    public synchronized V remove(Object key) {
        return _map.remove(key);
    }
    
    public synchronized boolean containsKey(Object key) {
    	return _map.containsKey(key);
    }
    
    public synchronized int size() {
    	return _map.size();
    }

    public synchronized boolean isEmpty() {
        return _map.isEmpty();
    }
    
    public synchronized void clear() {
        _map.clear();
    }
    
    /**
     * This method use specific locking strategy: map which have lowest hashCode() will be locked first
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends K, ? extends V> map) {
    	if (map == null || this == map) return;
    	if (this.hashCode() <= map.hashCode())
    		synchronized (this) {
    			synchronized (map) {
    				_map.putAll(map);
    			}
    		}
    	else {
    		synchronized (map) {
    			synchronized(this) {
    				_map.putAll(map);
    			}
    		}
    	}
    }
    
    public synchronized boolean containsValue(Object value) {
        return _map.containsValue(value);
    }

    public synchronized boolean equals(Object o) {
        return _map.equals(o);
    }
    
    public synchronized int hashCode() {
        return _map.hashCode();
    }
    
    public synchronized String toString() {
        return _map.toString();
    }
    
    /**
     * Public method that iterate entire collection.<br>
     * <br>
     * @param func - a class method that must be executed on every element of collection.<br>
     * @return - returns true if entire collection is iterated, false if it`s been interrupted by
     *             check method (I2ForEach.forEach())<br>
     */
    public synchronized final boolean ForEach(I2ForEach<K,V> func) {
    	return _map.ForEach(func);
    }
    
    public synchronized final boolean ForEachValue(I2ForEachValue<V> func) {
    	return _map.ForEachValue(func);
    }

    public synchronized final boolean ForEachKey(I2ForEachKey<K> func) {
    	return _map.ForEachKey(func);
    }

    /**
     * <font color="red">Unsupported operation!!!</font>
     * @deprecated
     * @throws UnsupportedOperationException
     * @see java.util.Map#values()
     */
    public Collection<V> values() {
    	throw new UnsupportedOperationException();
    }
    
    /**
     * <font color="red">Unsupported operation!!!</font>
     * @deprecated
     * @throws UnsupportedOperationException
     */
    public Set<K> keySet() {
    	throw new UnsupportedOperationException();
    }
    
    /**
     * <font color="red">Unsupported operation!!!</font>
     * @deprecated
     * @throws UnsupportedOperationException
     * @see java.util.Map#entrySet()
     */
    public Set<Map.Entry<K,V>> entrySet() {
    	throw new UnsupportedOperationException();
    }
}
