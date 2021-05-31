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
package net.sf.l2j.gameserver.model;

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.NameColors;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TitleColors;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Lukas
 * add 1-use version
 */
public class PvPColorChanger
{
	private static final int ITEM_ID = 99985;
	
	public static void showMenu(L2PcInstance activeChar)
	{
		if(!canHandleBon(activeChar))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		int pvps = activeChar.getPvpKills();

	    NpcHtmlMessage infoHtml = new NpcHtmlMessage(1);
	    TextBuilder tb = new TextBuilder();
	    
	    tb.append("<html><title>Name / Title color changer</title><br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
	    
	    tb.append("<font color=ac9887>Available NAME colors:</font>");
	    
	    for(NameColors color : NameColors.values())
	    {
	    	if(color.minPvP <= pvps)
	    	{
	    		tb.append("<table width=260><tr>");
	    		tb.append("<td width=130 align=left><font color=" + color.htmlHash + ">" + color.name + " (" + color.minPvP + " PvP)</font></td> <td width=100 align=right><button value=\"Set\" action=\"bypass -h pvpcolor_name " + color.id + "\" width=60 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	    		tb.append("</tr></table>");
	    	}
	    }
	    
	    tb.append("<br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></html>");
	    
	    infoHtml.setHtml(tb.toString());
		activeChar.sendPacket(infoHtml);
	}
	
	public static void onBypass(L2PcInstance activeChar, String command)
	{
		if(!canHandleBon(activeChar))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		String action = command.substring(0);
		if(action.startsWith("_name"))
		{
			int choice = Integer.parseInt(action.substring(6));
			setName(activeChar, choice);
			//activeChar.sendMessage("setName(activeChar, choice);");
			return;
		}
		else if(action.equals("_menu"))
		{
			showMenu(activeChar);
			return;
		}
		else if(action.startsWith("_title"))
		{
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			int choiceName = Integer.parseInt(st.nextToken());
			int choiceTitle = Integer.parseInt(st.nextToken());
			setTitle(activeChar, choiceName, choiceTitle);
			return;
		}
		else if(action.startsWith("_confirm"))
		{
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			int choiceName = Integer.parseInt(st.nextToken());
			int choiceTitle = Integer.parseInt(st.nextToken());
			confirm(activeChar, choiceName, choiceTitle);
			return;
		}
	}
	
	public static void setName(L2PcInstance activeChar, int choiceName)
	{
		int pvps = activeChar.getPvpKills();

	    NpcHtmlMessage infoHtml = new NpcHtmlMessage(1);
	    TextBuilder tb = new TextBuilder();
	    
	    tb.append("<html><title>Name / Title color changer</title><br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
	    
	    tb.append("<font color=ac9887>Available TITLE colors:</font>");
	    
	    for(TitleColors color : TitleColors.values())
	    {
	    	if(color.minPvP <= pvps)
	    	{
	    		tb.append("<table width=260><tr>");
	    		tb.append("<td width=130 align=left><font color=" + color.htmlHash + ">" + color.name + " (" + color.minPvP + " PvP)</font></td> <td width=100 align=right><button value=\"Set\" action=\"bypass -h pvpcolor_title " + choiceName + " " + color.id + "\" width=60 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	    		tb.append("</tr></table><br1>");
	    	}
	    }
	    
	    tb.append("<button value=\"Back\" action=\"bypass -h pvpcolor_menu\" width=70 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
	    
	    tb.append("<br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></html>");
	    
	    infoHtml.setHtml(tb.toString());
		activeChar.sendPacket(infoHtml);
	}
	
	public static void setTitle(L2PcInstance activeChar, int choiceName, int choiceTitle)
	{
		NpcHtmlMessage infoHtml = new NpcHtmlMessage(1);
	    TextBuilder tb = new TextBuilder();
	    
	    
	    NameColors name = null;
	    TitleColors title = null;
	    
	    for(NameColors n : NameColors.values())
	    {
	    	if(n.id == choiceName && n.minPvP <= activeChar.getPvpKills())
	    	{
	    		name = n;
	    		break;
	    	}
	    }
	    
	    for(TitleColors n : TitleColors.values())
	    {
	    	if(n.id == choiceTitle && n.minPvP <= activeChar.getPvpKills())
	    	{
	    		title = n;
	    		break;
	    	}
	    }
	    
	    if(name == null || title == null)
	    {
	    	activeChar.sendMessage("You selected wrong colors.");
	    	return;
	    }
	    
	    tb.append("<html><title>Name / Title color changer</title><br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
	    
	    tb.append("You have selected:<br><br>");
	    
	    tb.append("Name color: <font color=" + name.htmlHash + ">" + name.name + "</font><br1>");
	    tb.append("Title color: <font color=" + title.htmlHash + ">" + title.name + "</font><br>");
	    
	    tb.append("Please confirm your change. The item will be consumed and your name and title color changed.");
	    
	    tb.append("<button value=\"Confirm\" action=\"bypass -h pvpcolor_confirm " + name.id + " " + title.id + "\" width=95 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br>");
	    tb.append("<button value=\"Back\" action=\"bypass -h pvpcolor_menu\" width=70 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
	    
	    tb.append("<br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></html>");
	    
	    infoHtml.setHtml(tb.toString());
		activeChar.sendPacket(infoHtml);
	}
	
	public static void confirm(L2PcInstance activeChar, int choiceName, int choiceTitle)
	{
		L2ItemInstance removedItem = activeChar.getInventory().destroyItemByItemId("PvPColorChange", ITEM_ID, 1, activeChar, null);
		
		if(removedItem != null)
		{
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				if (removedItem.getCount() == 0) iu.addRemovedItem(removedItem);
				else iu.addModifiedItem(removedItem);
				
				activeChar.sendPacket(iu);
			}
			else activeChar.sendPacket(new ItemList(activeChar, true));
			
			//TODO set the recoloring methods

			activeChar.getAppearance().setNameColor(Integer.decode("0x777777")); // purple name
			activeChar.getAppearance().setTitleColor(Integer.decode("0x777777"));
			//activeChar.sendMessage("I'm Here3");

			activeChar.updateNameTitleColors(choiceName, false, false);
			activeChar.updateNameTitleColors(choiceTitle, true, true);
			activeChar.store(false, false);
			
			NpcHtmlMessage infoHtml = new NpcHtmlMessage(1);
		    TextBuilder tb = new TextBuilder();
		    
		    tb.append("<html><title>Name / Title color changer</title><br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		    
		    tb.append("Your name and title color have been updated.");
		    
		    tb.append("<br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></html>");
		    
		    infoHtml.setHtml(tb.toString());
			activeChar.sendPacket(infoHtml);
		}
		else
		{
			activeChar.sendMessage("You don't have the item to change your name/title color.");
		}
	}
	
	private static boolean canHandleBon(L2PcInstance player)
	{
		if(player.isInOlympiadMode())
			return false;
		else if (player.isAlikeDead())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		else if(player.isInJail())
		{
			player.sendMessage("You are in Jail, you can't do this.");
			return false;
		}
		else if(player.isInOlympiadMode())
		{
			player.sendMessage("You are in Oly, you can't do this.");
			return false;
		}
		else if (player.isMovementDisabled() || player.isAlikeDead() || player.isImmobilized())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		else if (player.isInDuel())
		{
			player.sendMessage("You cant do this while you are dueling.");
			return false;
		}
		else if (player.inObserverMode())
		{
			player.sendMessage("You cant do this in observer mode.");
			return false;
		}
		
		return true;
	}
}
