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
package net.sf.l2j.gameserver.communitybbs;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.communitybbs.Manager.ClanBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.Donation;
import net.sf.l2j.gameserver.communitybbs.Manager.LogindataBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.PartyBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.PostBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.PvPBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.TopBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.TopicBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.AugBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.ClassPathsBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.CommunityBosses;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.EventBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.GlobalEventBBSManager;
// import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.ClassPathsBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.ShopBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.SkillEnchBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.StatsBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.SymbolBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.TeleBBSManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class CommunityBoard
{
	private CommunityBoard()
	{}
	
	public static CommunityBoard getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public void handleCommands(L2GameClient client, String command)
	{
		final L2PcInstance activeChar = client.getActiveChar();
		if (activeChar == null)
			return;
		switch (Config.COMMUNITY_TYPE)
		{
			default:
			case 0: // disabled
				/* activeChar.sendPacket(new SystemMessage(SystemMessageId.CB_OFFLINE)); */
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				break;
			case 1: // old
				RegionBBSManager.getInstance().parsecmd(command, activeChar);
				break;
			case 2: // new

				if (command.startsWith("_bbsclan"))
				{
					ClanBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbsmemo") || command.startsWith("_bbsEventsBoard"))
				{
					EventBBSManager.getInstance().parsecmd(command, activeChar);
					// PartyBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbsGlobalEvents"))
				{
					GlobalEventBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbstopics"))
				{
					TopicBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbsposts"))
				{
					PostBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbstop"))
				{
					TopBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbshome"))
				{
					TopBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbsloc"))
				{
					RegionBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbsgetfav") || command.startsWith("_bbspvp") || command.startsWith("_bbsfame") || command.startsWith("_bbspk") || command.startsWith("_bbsrk") || command.startsWith("_bbsek") || command.startsWith("_bbssk") || command.startsWith("_bbsow"))
				{
					PvPBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbslink") || command.contains("_maillist_0_1_0_"))
				{
					LogindataBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbsmuseum"))
				{
					if (!command.startsWith("_bbsmuseum"))
					{
						command = "_bbsmuseum";
					}
					break;
					//MuseumBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbstele"))
				{
					TeleBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbsshop"))
				{
					ShopBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbsskillench"))
				{
					SkillEnchBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbsclasspath") || command.contains("_friendlist_0_"))
				{
					if (activeChar.isInOlympiadMode())
					{
						activeChar.sendMessage("Can't use Class Paths inside olympiad.");
						break;
					}
					ClassPathsBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbscp"))
				{
					if (activeChar.isInOlympiadMode() || Olympiad.getInstance().isRegistered(activeChar))
					{
						activeChar.sendMessage("Can't use Class Paths while participating or being inside olympiad.");
						break;
					}
					ClassPathsBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbssymbol"))
				{
					SymbolBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbsaug"))
				{
					AugBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbsstats"))
				{
					StatsBBSManager.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("bbs_add_fav"))
				{
					Donation.getInstance().parsecmd(command, activeChar);
				}
				else if (command.startsWith("_bbspartymatching"))
				{
					PartyBBSManager.getInstance().parsecmd(command, activeChar);
				}
//				else if (command.startsWith("_bbsbuffer"))
//				{
//					BufferBBSManager.getInstance().parsecmd(command, activeChar);
//				}
				else if (command.startsWith("_bbsboss"))
				{
					CommunityBosses.getInstance().parsecmd(command, activeChar);
				}
				else
				{
					ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", "101");
					activeChar.sendPacket(sb);
				}
				break;
		}
	}
	
	/**
	 * @param client
	 * @param url
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @param arg5
	 */
	public void handleWriteCommands(L2GameClient client, String url, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		L2PcInstance activeChar = client.getActiveChar();
		if (activeChar == null)
			return;
		switch (Config.COMMUNITY_TYPE)
		{
			case 2:
				if (url.equals("Topic"))
				{
					TopicBBSManager.getInstance().parsewrite(url, arg1, arg2, arg3, arg4, arg5, activeChar);
				}
				else if (url.equals("Post"))
				{
					PostBBSManager.getInstance().parsewrite(url, arg1, arg2, arg3, arg4, arg5, activeChar);
				}
				else if (url.equals("Region"))
				{
					RegionBBSManager.getInstance().parsewrite(url, arg1, arg2, arg3, arg4, arg5, activeChar);
				}
				else if (url.equals("Notice") || url.startsWith("_bbsclan"))
				{
					ClanBBSManager.getInstance().parsewrite(url, arg1, arg2, arg3, arg4, arg5, activeChar);
				}
				else
				{
					ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + url + " is not implemented yet</center><br><br></body></html>", "101");
					activeChar.sendPacket(sb);
				}
				break;
			case 1:
				RegionBBSManager.getInstance().parsewrite(url, arg1, arg2, arg3, arg4, arg5, activeChar);
				break;
			default:
			case 0:
				ShowBoard sb = new ShowBoard("<html><body><br><br><center>The Community board is currently disabled</center><br><br></body></html>", "101");
				activeChar.sendPacket(sb);
				break;
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final CommunityBoard _instance = new CommunityBoard();
	}
}
