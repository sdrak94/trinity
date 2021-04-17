package net.sf.l2j.gameserver.communitybbs.Manager.lunaservices;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class AugBBSManager 
{
	public static AugBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public void parsecmd(String command, L2PcInstance activeChar)
	{
			String path = "data/html/CommunityBoard/augment/";
			String filepath = "";
			String content = "";
			if (command.equals("_bbsaug"))
			 		{

				filepath = path +"augment.htm";
				content = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), filepath);
				separateAndSend(content, activeChar);
			 		}

			else if (command.equalsIgnoreCase("Augment"))
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

				String filename = "data/html/CommunityBoard/augment/augment.htm";
				content = HtmCache.getInstance().getHtm(filename);

				if (content == null)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
					activeChar.sendPacket(html);
				}
				else
				{
					final int cmdChoice = Integer.parseInt(command.substring(8, 9).trim());

					switch (cmdChoice)
					{
						case 0:
							filename = "data/html/CommunityBoard/augment/augment.htm";
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
							break;
						case 1:
							activeChar.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED));
							activeChar.sendPacket(new ExShowVariationMakeWindow());
							break;
						case 2:
							activeChar.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION));
							activeChar.sendPacket(new ExShowVariationCancelWindow());
							break;
					}
				}
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
	protected static final AugBBSManager _instance = new AugBBSManager();
	}
}
