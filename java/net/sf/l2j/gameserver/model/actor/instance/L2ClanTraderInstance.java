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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public final class L2ClanTraderInstance extends L2Npc
{
	public L2ClanTraderInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		player.setLastFolkNPC(this);

		if (this != player.getTarget())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (!canInteract(player))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
				showMessageWindow(player);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);

		if (command.equalsIgnoreCase("crp"))
		{
			if (player.getClan().getLevel() > 4)
				html.setFile("data/html/clantrader/" + getNpcId() + "-2.htm");
			else
				html.setFile("data/html/clantrader/" + getNpcId() + "-1.htm");

			sendHtmlMessage(player, html);
		}
		else if (command.startsWith("exchange"))
		{
			int itemId = Integer.parseInt(command.substring(9).trim());

			int reputation = 0;
			int itemCount = 0;

			L2ItemInstance item = player.getInventory().getItemByItemId(itemId);
			long playerItemCount = item == null ? 0 : item.getCount();

			switch (itemId)
			{
				case 9911:
					reputation = Config.BLOODALLIANCE_POINTS;
					itemCount = 1;
					break;
				case 9910:
					reputation = Config.BLOODOATH_POINTS;
					itemCount = 10;
					break;
				case 9912:
					reputation = Config.KNIGHTSEPAULETTE_POINTS;
					itemCount = 100;
					break;
			}

			if (playerItemCount >= itemCount)
			{
				player.destroyItemByItemId("exchange", itemId, itemCount, player, true);

				player.getClan().setReputationScore(player.getClan().getReputationScore() + reputation, true);
				player.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));

				player.sendMessage("Your clan has added " + reputation + " points to its clan reputation score.");
				/* TODO: fix the system message, I cant add the number to the system message
				SystemMessage sm =  new SystemMessage(SystemMessageId.CLAN_ADDED_S1S_POINTS_TO_REPUTATION_SCORE);
				sm.addItemNumber(reputation);
				player.sendPacket(sm);*/

				html.setFile("data/html/clantrader/" + getNpcId() + "-ExchangeSuccess.htm");
			}
			else
				html.setFile("data/html/clantrader/" + getNpcId() + "-ExchangeFailed.htm");

			sendHtmlMessage(player, html);
		}

		super.onBypassFeedback(player, command);
	}

	private void sendHtmlMessage(L2PcInstance player, NpcHtmlMessage html)
	{
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	private void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/clantrader/" + getNpcId() + "-no.htm";

		if (player.isClanLeader())
			filename = "data/html/clantrader/" + getNpcId() + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0) pom = "" + npcId;
		else pom = npcId + "-" + val;

		return "data/html/clantrader/" + pom + ".htm";
	}
}