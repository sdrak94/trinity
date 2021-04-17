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
package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;

/**
 * A landing zone
 *
 * @author  Kerberos
 */
public class L2LandingZone extends L2ZoneType
{
	public L2LandingZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (character instanceof L2PcInstance)
			character.setInsideZone(L2Character.ZONE_LANDING, true);
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (character instanceof L2PcInstance)
			character.setInsideZone(L2Character.ZONE_LANDING, false);
	}

	/**
     * @see net.sf.l2j.gameserver.model.zone.L2ZoneType#onDieInside(net.sf.l2j.gameserver.model.actor.L2Character)
     */
    @Override
    public void onDieInside(L2Character character)
    {  
    }

	/**
     * @see net.sf.l2j.gameserver.model.zone.L2ZoneType#onReviveInside(net.sf.l2j.gameserver.model.actor.L2Character)
     */
    @Override
    public void onReviveInside(L2Character character)
    {
    }
}
