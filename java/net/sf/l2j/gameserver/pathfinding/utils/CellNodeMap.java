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
package net.sf.l2j.gameserver.pathfinding.utils;

import java.util.ArrayList;
import java.util.Map;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.pathfinding.Node;

/**
 *
 * @author Sami
 */
public class CellNodeMap
{
	private Map<Integer, ArrayList<Node>> _cellIndex;
	
	public CellNodeMap()
	{
		_cellIndex = new FastMap<Integer, ArrayList<Node>>();
	}
	
	public void add(Node n)
	{
		if (_cellIndex.containsKey(n.getLoc().getY()))
		{
			_cellIndex.get(n.getLoc().getY()).add(n);
		}
		else
		{
			ArrayList<Node> array = new ArrayList<Node>(5);
			array.add(n);
			_cellIndex.put(n.getLoc().getY(), array);
		}
	}
	public boolean contains(Node n)
	{
		ArrayList<Node> array = _cellIndex.get(n.getLoc().getY());
		if (array == null) 
		{
			return false;
		}
		for (Node node : array)
			if(node.equals(n))
				return true;
		return false;
	}
}
