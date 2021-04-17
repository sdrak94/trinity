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
package net.sf.l2j.gameserver.pathfinding;

/**
 *
 * @author -Nemesiss-
 */
public class Node
{
	private final AbstractNodeLoc _loc;
	private final int _neighborsIdx;
	private Node[] _neighbors;
	private Node _parent;
	private short _cost;


	public Node(AbstractNodeLoc Loc, int Neighbors_idx)
	{
		_loc = Loc;
		_neighborsIdx = Neighbors_idx;
	}

	public void setParent(Node p)
	{
		_parent = p;
	}

	public void setCost(int cost)
	{
		_cost = (short)cost;
	}

	public void attachNeighbors()
	{
		if(_loc == null) _neighbors = null;
		else _neighbors = PathFinding.getInstance().readNeighbors(this, _neighborsIdx);
	}

	public Node[] getNeighbors()
	{
		return _neighbors;
	}

	public Node getParent()
	{
		return _parent;
	}

	public AbstractNodeLoc getLoc()
	{
		return _loc;
	}

	public short getCost()
	{
		return _cost;
	}

	/**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((_loc == null) ? 0 : _loc.hashCode());
	    return result;
    }

	/**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
	    if (this == obj)
		    return true;
	    if (obj == null)
		    return false;
	    if (!(obj instanceof Node))
		    return false;
	    final Node other = (Node) obj;
	    if (_loc == null)
	    {
		    if (other._loc != null)
			    return false;
	    }
	    else if (!_loc.equals(other._loc))
		    return false;
	    return true;
    }
	
}
