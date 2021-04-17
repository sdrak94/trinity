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
package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;

public class StaticObjStat extends CharStat
{
    // =========================================================
    // Data Field

    // =========================================================
    // Constructor
    public StaticObjStat(L2StaticObjectInstance activeChar)
    {
        super(activeChar);

        setLevel((byte)1);
    }

    // =========================================================
    // Method - Public

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    @Override
	public L2StaticObjectInstance getActiveChar() { return (L2StaticObjectInstance)super.getActiveChar(); }

    @Override
	public final byte getLevel() { return 1; }
}
