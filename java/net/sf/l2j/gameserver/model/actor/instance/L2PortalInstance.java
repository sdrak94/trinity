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

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.entity.events.RaidEvent;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2PortalInstance extends L2Npc
{

	public L2PortalInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance playerInstance, String command)
	{
		if (command.startsWith("raideventregister"))
			RaidEvent.getInstance().register(playerInstance);
		else if (command.startsWith("raideventunregister"))
			RaidEvent.getInstance().unregister(playerInstance);
	}

	@Override
	public void showChatWindow(L2PcInstance playerInstance, int val)
	{
		if (playerInstance == null)
			return;
		final String htmContent  = HtmCache.getInstance().getHtm("data/html/mods/raidevent/main.htm");
		if (htmContent == null)
			return;
		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
		npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
		if (npcHtmlMessage!=null)
			playerInstance.sendPacket(npcHtmlMessage);
	}
}
