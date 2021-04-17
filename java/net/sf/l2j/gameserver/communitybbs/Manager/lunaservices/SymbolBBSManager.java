package net.sf.l2j.gameserver.communitybbs.Manager.lunaservices;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.HennaEquipList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class SymbolBBSManager 
{
	public static SymbolBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public void parsecmd(String command, L2PcInstance activeChar)
	{
			String path = "data/html/CommunityBoard/symbols/";
			String filepath = "";
			String content = "";
			if (command.equals("_bbssymbol"))
			 		{

				filepath = path +"SymbolMaker.htm";
				content = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), filepath);
				separateAndSend(content, activeChar);
			 		}

			else if (command.equalsIgnoreCase("draw"))
			{
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}

				if (activeChar.isTransformed() && !activeChar.isUsingInquisitorStance())
				{
					activeChar.sendMessage("Cannot do this while transformed");
					return;
				}

				String filename = "data/html/CommunityBoard/symbols/SymbolMaker.htm";
				content = HtmCache.getInstance().getHtm(filename);

				if (content == null)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
					activeChar.sendPacket(html);
				}
				else
				{

					activeChar.sendPacket(new HennaEquipList(activeChar));
				}
			}
			else if (command.startsWith("enchantskill"))
			{
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}
				if (activeChar.isTransformed() && !activeChar.isUsingInquisitorStance())
				{
					activeChar.sendMessage("Cannot do this while transformed");
					return;
				}

				L2NpcInstance.onBypass(activeChar, command.substring(13));
			}
			else if (command.equalsIgnoreCase("enchanthelp"))
			{
				String filename = "data/html/CommunityBoard/symbols/SymbolMaker.htm";
				content = HtmCache.getInstance().getHtm(filename);

				if (content == null)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
					activeChar.sendPacket(html);
				}
				else
				{
					NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
					itemReply.setHtml(content);
					activeChar.sendPacket(itemReply);
				}
			}
			else
				{
				ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", "101");
				activeChar.sendPacket(sb);
				activeChar.sendPacket(new ShowBoard(null, "102"));
				activeChar.sendPacket(new ShowBoard(null, "103"));
				}
	}
	protected void separateAndSend(String html, L2PcInstance acha)
	{
		if (html == null)
			return;
		acha.sendPacket(new ShowBoard(html, "101"));
	}
	private static class SingletonHolder
	{
	protected static final SymbolBBSManager _instance = new SymbolBBSManager();
	}
}
