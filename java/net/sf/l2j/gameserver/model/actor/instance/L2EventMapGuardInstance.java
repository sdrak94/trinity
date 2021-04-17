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

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public final class L2EventMapGuardInstance extends L2GuardInstance
{
private static Logger _log = Logger.getLogger(L2GuardInstance.class.getName());


/**
 * Constructor of L2GuardInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Call the L2Character constructor to set the _template of the L2GuardInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
 * <li>Set the name of the L2GuardInstance</li>
 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
 *
 * @param objectId Identifier of the object to initialized
 * @param L2NpcTemplate Template to apply to the NPC
 */
public L2EventMapGuardInstance(int objectId, L2NpcTemplate template)
{
	super(objectId, template);
}

@Override
public boolean isAutoAttackable(L2Character attacker)
{
	return false;
}

/**
 * Manage actions when a player click on the L2GuardInstance.<BR><BR>
 *
 * <B><U> Actions on first click on the L2GuardInstance (Select it)</U> :</B><BR><BR>
 * <li>Set the L2GuardInstance as target of the L2PcInstance player (if necessary)</li>
 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li>
 * <li>Set the L2PcInstance Intention to AI_INTENTION_IDLE </li>
 * <li>Send a Server->Client packet ValidateLocation to correct the L2GuardInstance position and heading on the client </li><BR><BR>
 *
 * <B><U> Actions on second click on the L2GuardInstance (Attack it/Interact with it)</U> :</B><BR><BR>
 * <li>If L2PcInstance is in the _aggroList of the L2GuardInstance, set the L2PcInstance Intention to AI_INTENTION_ATTACK</li>
 * <li>If L2PcInstance is NOT in the _aggroList of the L2GuardInstance, set the L2PcInstance Intention to AI_INTENTION_INTERACT (after a distance verification) and show message</li><BR><BR>
 *
 * <B><U> Example of use </U> :</B><BR><BR>
 * <li> Client packet : Action, AttackRequest</li><BR><BR>
 *
 * @param player The L2PcInstance that start an action on the L2GuardInstance
 *
 */
@Override
public void onAction(L2PcInstance player)
{
	if (!canTarget(player)) return;
	
	// Check if the L2PcInstance already target the L2GuardInstance
	if (getObjectId() != player.getTargetId())
	{
		if (Config.DEBUG) _log.fine(player.getObjectId()+": Targetted guard "+getObjectId());
		
		// Set the target of the L2PcInstance player
		player.setTarget(this);
		
		// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
		// The color to display in the select window is White
		MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
		player.sendPacket(my);
		
		// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
		player.sendPacket(new ValidateLocation(this));
	}
	else
	{
		// Check if the L2PcInstance is in the _aggroList of the L2GuardInstance
		if (containsTarget(player))
		{
			if (Config.DEBUG) _log.fine(player.getObjectId()+": Attacked guard "+getObjectId());
			
			// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				// Set the L2PcInstance Intention to AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				player.sendMessage("Did you know that you are on the event right now?");
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}
	// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
	player.sendPacket(ActionFailed.STATIC_PACKET);
}
}
