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

import java.util.Arrays;

import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2WyvernManagerInstance extends L2Npc
{
	private static final int[] STRIDERS = { 12526, 12527, 12528, 16038, 16039, 16040 };

	public L2WyvernManagerInstance (int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("RideWyvern"))
		{
			if (!isOwnerClan(player))
			{
				return;
			}
			if ((SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK) && SevenSigns.getInstance().isSealValidationPeriod())
			{
				player.sendMessage("You cannot ride wyvern while Seal of Strife controlled by Dusk.");
				return;     		
			}
			if(player.getPet() == null)
			{
				if(player.isMounted())
				{
					player.sendMessage("You already have a pet.");
					return;
				}
				else
				{
					player.sendMessage("Summon your Strider first.");
					return;
				}
			}
			else if (Arrays.binarySearch(STRIDERS, player.getPet().getNpcId()) >= 0 )
			{
				if (player.getInventory().getItemByItemId(1460) != null && player.getInventory().getItemByItemId(1460).getCount() >= 25)
				{
					if (player.getPet().getLevel() < 55)
					{
						player.sendMessage("Your Strider Has not reached the required level.");
						return;
					}
					else
					{
						player.getPet().unSummon(player);
						if (player.mount(12621, 0, true))
						{
							player.getInventory().destroyItemByItemId("Wyvern", 1460, 25, player, player.getTarget());
							player.addSkill(SkillTable.getInstance().getInfo(4289, 1));
							player.sendMessage("The Wyvern has been summoned successfully!");
						}
						return;
					}
				}
				else
				{
					player.sendMessage("You need 25 Crystals: B Grade.");
					return;
				}
			}
			else
			{
				player.sendMessage("Unsummon your pet.");
				return;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		player.setLastFolkNPC(this);

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				showMessageWindow(player);
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket( ActionFailed.STATIC_PACKET );
		String filename = "data/html/wyvernmanager/wyvernmanager-no.htm";

		if (isOwnerClan(player))
		{
            filename = "data/html/wyvernmanager/wyvernmanager.htm";      // Owner message window
		}

		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	protected boolean isOwnerClan(L2PcInstance player)
	{
		return true;
	}
}
