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
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.instancemanager.AirShipManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * NPC to control passengers stepping in/out of the airship
 *
 * @author  DrHouse
 */
public class L2AirShipControllerInstance extends L2NpcInstance
{

	private boolean _isBoardAllowed = false;
       /**
     * @param objectId
     * @param template
     */
    public L2AirShipControllerInstance(int objectId, L2NpcTemplate template)
    {
           super(objectId, template);
           AirShipManager.getInstance().registerATC(this);
    }
    
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.equalsIgnoreCase("board"))
		{
			
			L2AirShipInstance ship = AirShipManager.getInstance().getAirShip();
			{
				if (player.isFlyingMounted())
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_NOT_MEET_REQUEIREMENTS));
				else if (ship.isInDock() && _isBoardAllowed)
				{
					ship.onPlayerBoarding(player);
					return;
				}
			}
		}
		else
			super.onBypassFeedback(player, command);
	}
    
    public void broadcastMessage(String message)
    {
       NpcSay say = new NpcSay(getObjectId(), 1, getNpcId(), message);
       Broadcast.toKnownPlayersInRadius(this, say, 5000);
    }
    
    public void setIsBoardAllowed(boolean val)
    {
    	_isBoardAllowed = val;
    }
}