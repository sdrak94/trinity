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

/**
 * This class permit to pass (x, y, z, heading) position data to method.<BR><BR>
 */
public final class L2CharPosition
{

    public final int x, y, z, heading;

    /**
     * Constructor of L2CharPosition.<BR><BR>
     */
    public L2CharPosition(int pX, int pY, int pZ, int pHeading)
    {
        x = pX;
        y = pY;
        z = pZ;
        heading = pHeading;
    }

}
