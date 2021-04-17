/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.events.Korean;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2KoreanRegInstance extends L2Npc
{
	static final Logger _log = Logger.getLogger(L2KoreanRegInstance.class.getName());
	
	public L2KoreanRegInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(L2PcInstance playerInstance, int val)
	{
		if (playerInstance == null)
			return;
		String path = "data/html/custom/event/";
		String file = "registering.htm";
		switch (Korean.state)
		{
			case REGISTER:
				file = "registering.htm";
				break;
			case ACTIVE:
				file = "active.htm";
				break;
			default:
				file = "innactive.htm";
				break;
		}
		String htmContent = HtmCache.getInstance().getHtm(path + file);
		if (htmContent != null)
		{
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
			npcHtmlMessage.setHtml(htmContent);
			npcHtmlMessage.replace("%button%", String.valueOf(getObjectId()));
			playerInstance.sendPacket(npcHtmlMessage);
		}
		playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	int pageVal = 0;
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		// BypassValidation Exploit plug.
		if (player == null || player.getLastFolkNPC() == null || player.getLastFolkNPC().getObjectId() != this.getObjectId())
			return;
		if (command.startsWith("Chat"))
		{
			int val = Integer.parseInt(command.substring(5));
			pageVal = val;
			showChatWindow(player, val);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}