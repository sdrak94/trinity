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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * Highly concurrent List wrapping class, thread-safe<br>
 * The default constructor sets a wrapped class to ArrayList<T> to minimize code line
 * when wrapped class type is not that importent.<br>
 * <br>
 * Note: Iterator returned does not support element removal!
 * @author  Julian
 */
public class L2SyncList<T extends Object> implements List<T> 
{
	private final List<T> _list;
	private final ReentrantReadWriteLock _rw = new ReentrantReadWriteLock();
	private final ReadLock _rl = _rw.readLock();
	private final WriteLock _wl = _rw.writeLock();
	
	/**
	 * Default constructor use ArrayList as it internal
	 */
	public L2SyncList() {
		_list = new ArrayList<T>();
	}
	
	public L2SyncList(List<T> list) {
		_list = list;
	}
	
	public T get(int index) {
		_rl.lock();
		try {
			return _list.get(index);
		} finally {
			_rl.unlock();
		}
	}
	
    public boolean equals(Object o) {
    	_rl.lock();
    	try {
    		return _list.equals(o);
    	} finally {
    		_rl.unlock();
    	}
    }
    public int hashCode() {
    	_rl.lock();
    	try {
    		return _list.hashCode();
    	} finally {
    		_rl.unlock();
    	}
    }

    public T set(int index, T element) {
    	_wl.lock();
    	try {
    		return _list.set(index, element);
    	} finally {
    		_wl.unlock();
    	}
    }
    
    public void add(int index, T element) {
    	_wl.lock();
    	try {
    		_list.add(index, element);
    	} finally {
    		_wl.unlock();
    	}
    }

    public boolean add(T element) {
    	_wl.lock();
    	try {
    		return _list.add(element);
    	} finally {
    		_wl.unlock();
    	}
    }
    
    public T remove(int index) {
    	_wl.lock();
    	try {
    		return _list.remove(index);
    	} finally {
    		_wl.unlock();
    	}
    }

    public boolean remove(Object value) {
    	_wl.lock();
    	try {
    		return _list.remove(value);
    	} finally {
    		_wl.unlock();
    	}
    }
    
    public boolean removeAll(Collection<?> list) {
    	_wl.lock();
    	try {
    		return _list.removeAll(list);
    	} finally {
    		_wl.unlock();
    	}
    }

    public boolean retainAll(Collection<?> list) {
    	_wl.lock();
    	try {
    		return _list.retainAll(list);
    	} finally {
    		_wl.unlock();
    	}
    }
    public int indexOf(Object o) {
    	_rl.lock();
    	try {
    		return _list.indexOf(o);
    	} finally {
    		_rl.unlock();
    	}
    }

    public boolean contains(Object o) {
    	_rl.lock();
    	try {
    		return _list.contains(o);
    	} finally {
    		_rl.unlock();
    	}
    }
    
    public boolean containsAll(Collection<?> list) {
    	_rl.lock();
    	try {
    		return _list.containsAll(list);
    	} finally {
    		_rl.unlock();
    	}
    }
    
    public int lastIndexOf(Object o) {
    	_rl.lock();
    	try {
    		return _list.lastIndexOf(o);
    	} finally {
    		_rl.unlock();
    	}
    }

    public boolean addAll(Collection<? extends T> list) {
    	_wl.lock();
    	try {
    		return _list.addAll(list);
    	} finally {
    		_wl.unlock();
    	}
    }
    
    public boolean addAll(int index, Collection<? extends T> c) {
    	_wl.lock();
    	try {
    		return _list.addAll(index, c);
    	} finally {
    		_wl.unlock();
    	}
    }
    
    public List<T> subList(int fromIndex, int toIndex) {
    	_rl.lock();
    	try {
    		return new L2SyncList<T>(_list.subList(fromIndex, toIndex));
    	} finally {
    		_rl.unlock();
    	}
    }

    public void clear() {
    	_wl.lock();
    	try {
    		_list.clear();
    	} finally {
    		_wl.unlock();
    	}
    }

    public int size() {
    	_rl.lock();
    	try {
    		return _list.size();
    	} finally {
    		_rl.unlock();
    	}
    }
    
    public boolean isEmpty() {
    	_rl.lock();
    	try {
    		return _list.isEmpty();
    	} finally {
    		_rl.unlock();
    	}
    }

    /**
     * <FONT color="#FF0000">WARNING: Unsupported</FONT>
     */
    public ListIterator<T> listIterator() {
    	throw new UnsupportedOperationException();
    }

    /**
     * <FONT color="#FF0000">WARNING: Unsupported</FONT>
     * @see java.util.List#listIterator(int)
     */
    public ListIterator<T> listIterator(int index) {
    	throw new UnsupportedOperationException();
    }

    /**
     * <FONT color="#FF0000">WARNING: Returned iterator use cloned List</FONT>
     * @see java.util.List#iterator()
     */
	@SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
    	return new Itr((T[])_list.toArray());
    }

    private class Itr implements Iterator<T> {
        int cursor;       // index of next element to return
        int lastRet = -1; // index of last element returned; -1 if no such
        int size;
        T[] elementData;
        
        public Itr(T[] data) {
        	elementData = data;
        	if (data != null) size = data.length; else size = 0;
        }

        public boolean hasNext() {
            return cursor != size;
        }

        public T next() {
            int i = cursor;
            if (i >= size)
                throw new NoSuchElementException();
            cursor = i + 1;
            lastRet = i;
            return elementData[lastRet];
        }

        public void remove() {
        	throw new UnsupportedOperationException();
        }
    }

	public Object[] toArray() {
		_rl.lock();
		try {
			return _list.toArray();
		} finally {
			_rl.unlock();
		}
	}
	
	@SuppressWarnings("hiding")
	public  <T> T[] toArray(T[] a) {
		_rl.lock();
		try {
			return _list.toArray(a);
		} finally {
			_rl.unlock();
		}
	}
}
