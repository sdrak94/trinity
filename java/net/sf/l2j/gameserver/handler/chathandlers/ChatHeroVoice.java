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
package net.sf.l2j.gameserver.handler.chathandlers;

import java.util.Collection;

import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.instancemanager.ChatbanManager;
import net.sf.l2j.gameserver.instancemanager.ChatbanManager.ChatType;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

/**
 * A chat handler
 *
 * @author  durgus
 */
public class ChatHeroVoice implements IChatHandler
{
private static final int[] COMMAND_IDS =
{
	17
};

/**
 * Handle chat type 'hero voice'
 * @see net.sf.l2j.gameserver.handler.IChatHandler#handleChat(int, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
 */
public void handleChat(int type, L2PcInstance activeChar, String target, String text)
{
	
	if (!activeChar.isGM())
	{
		if (ChatbanManager.getInstance().isChatBanned(activeChar, ChatType.HERO))
		{
			return;
		}
		if (!activeChar.isHero() && activeChar.getPvpKills() < 35000)
		{
			return;
		}
		
		if (!activeChar.getFloodProtectors().getHeroVoice().tryPerformAction("hero voice"))
		{
			activeChar.sendMessage("Heroes are only able to speak in the hero channel once every 60 seconds.");
			return;
		}
		
	}
	
	if (activeChar.isGMReally() || !(activeChar.isInHuntersVillage() || activeChar.isInOrcVillage()))
	{
		/*		text = text.replaceAll("forgotten", "pride");
		text = text.replaceAll("Forgotten", "pride");
		text = text.replaceAll("FORGOTTEN", "pride");*/
		/*		text = text.replaceAll("wartag", "pride");
		text = text.replaceAll("Wartag", "pride");
		text = text.replaceAll("WARTAG", "pride");*/
		
		CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
		
		Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
		//synchronized (L2World.getInstance().getAllPlayers())
		{
			for (L2PcInstance player : pls)
				player.sendPacket(cs);
		}
	}
}

/**
 * Returns the chat types registered to this handler
 * @see net.sf.l2j.gameserver.handler.IChatHandler#getChatTypeList()
 */
public int[] getChatTypeList()
{
	return COMMAND_IDS;
}
}
