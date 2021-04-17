package net.sf.l2j.gameserver.communitybbs.Manager.lunaservices;

import net.sf.l2j.gameserver.TradeController;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.L2TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.BuyList;
import net.sf.l2j.gameserver.network.serverpackets.SellList;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class ShopBBSManager 
{
	public static ShopBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public void parsecmd(String command, L2PcInstance activeChar)
	{
			String path = "data/html/CommunityBoard/shop/";
			String filepath = "";
			String content = "";
			if (command.equals("_bbsshop"))
			 		{

				filepath = path +"shop.htm";
				content = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), filepath);
				separateAndSend(content, activeChar);
			 		}
			else if (command.startsWith("_bbsshopbuy"))
			{
				handleBuyRequest(activeChar, command.substring(8));
			}
			else if (command.startsWith("_bbsshopsell"))
			{
				showSellWindow(activeChar);
			}
			else
				{
				ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", "101");
				activeChar.sendPacket(sb);
				activeChar.sendPacket(new ShowBoard(null, "102"));
				activeChar.sendPacket(new ShowBoard(null, "103"));
				}
	}	
	private static void handleBuyRequest(L2PcInstance activeChar, String command)
	{
		int val = -1;
		try
		{
			val = Integer.parseInt(command);
		}
		catch (Exception e)
		{
		}

		if (val == -1)
			return;

		activeChar.tempInventoryDisable();

		L2TradeList list = TradeController.getInstance().getBuyList(val);

		if (list != null)
		{
			activeChar.sendPacket(new BuyList(list, activeChar.getAdena()));
		}
		else
		{
		}
	}
	private static void showSellWindow(L2PcInstance acha)
	{
		acha.sendPacket(new SellList(acha));
	}
	protected void separateAndSend(String html, L2PcInstance acha)
	{
		if (html == null)
			return;
		acha.sendPacket(new ShowBoard(html, "101"));
	}
	private static class SingletonHolder
	{
	protected static final ShopBBSManager _instance = new ShopBBSManager();
	}
}
