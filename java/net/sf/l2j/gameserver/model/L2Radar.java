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
package net.sf.l2j.gameserver.model;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.RadarControl;

/**
 * @author dalrond
 */
public final class L2Radar
{
    private L2PcInstance        _player;
    private FastList<RadarMarker> _markers;

    public L2Radar(L2PcInstance player)
    {
        _player = player;
        _markers = new FastList<RadarMarker>();
    }

    // Add a marker to player's radar
    public void addMarker(int x, int y, int z)
    {
        RadarMarker newMarker = new RadarMarker(x, y, z);

        _markers.add(newMarker);
        _player.sendPacket(new RadarControl(2, 2, x, y, z));
        _player.sendPacket(new RadarControl(0, 1, x, y, z));
    }

    // Remove a marker from player's radar
    public void removeMarker(int x, int y, int z)
    {
        RadarMarker newMarker = new RadarMarker(x, y, z);

        _markers.remove(newMarker);
        _player.sendPacket(new RadarControl(1, 1, x, y, z));
    }

    public void removeAllMarkers()
    {
        for (RadarMarker tempMarker : _markers)
            _player.sendPacket(new RadarControl(2, 2, tempMarker._x, tempMarker._y, tempMarker._z));

        _markers.clear();
    }

    public void loadMarkers()
    {
		_player.sendPacket(new RadarControl(2, 2, _player.getX(), _player.getY(), _player.getZ()));
		for (RadarMarker tempMarker : _markers)
			_player.sendPacket(new RadarControl(0, 1, tempMarker._x, tempMarker._y, tempMarker._z));
	}

    public static class RadarMarker
    {
        // Simple class to model radar points.
        public int _type, _x, _y, _z;

        public RadarMarker(int type, int x, int y, int z)
        {
            _type = type;
            _x = x;
            _y = y;
            _z = z;
        }

        public RadarMarker(int x, int y, int z)
        {
            _type = 1;
            _x = x;
            _y = y;
            _z = z;
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
	        final int prime = 31;
	        int result = 1;
	        result = prime * result + _type;
	        result = prime * result + _x;
	        result = prime * result + _y;
	        result = prime * result + _z;
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
	        if (!(obj instanceof RadarMarker))
		        return false;
	        final RadarMarker other = (RadarMarker) obj;
	        if (_type != other._type)
		        return false;
	        if (_x != other._x)
		        return false;
	        if (_y != other._y)
		        return false;
	        if (_z != other._z)
		        return false;
	        return true;
        }
    }
}
