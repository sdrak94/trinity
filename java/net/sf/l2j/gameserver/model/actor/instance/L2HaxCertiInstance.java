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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;
import java.util.logging.Level;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.StringUtil;

/**
 * Reputation score manager
 * @author Kerberos
 */
public class L2HaxCertiInstance extends L2Npc
{
	public L2HaxCertiInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	/**
	 * this is called when a player interacts with this NPC
	 * @param player
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player)) return;

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
				showMessageWindow(player, 0);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

    @Override
    public void onBypassFeedback(L2PcInstance player, String command)
    {
    	StringTokenizer st = new StringTokenizer(command, " ");
    	if (command.contains("show_inventory"))
		{
			int page = 0;
			
			try
			{
				page = Integer.parseInt(command.substring(15));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (command.contains("show_inventory"))
			{
				showMessageWindow(player,page);
			}
		}
    	
    	else if (command.contains("delete_item"))
		{
    		final StringTokenizer st1 = new StringTokenizer(command);
			try
			{
				st1.nextToken();
				String sitemobjId = st1.nextToken();;
				String sitemId = st1.nextToken();
				boolean playerGotSkill = false;
				
				int itemobjId = Integer.valueOf(sitemobjId);
				int itemId = Integer.valueOf(sitemId);
				int skillId = 0;
				
					if (itemId == 60078)
					skillId = 35200;
					else if (itemId == 60079)
					skillId = 35202;
					else if (itemId == 60080)
					skillId = 35204;
					else if (itemId == 60081)
					skillId = 35206;
					else if (itemId == 60082)
					skillId = 35208;
					else if (itemId == 60083)
					skillId = 35210;
					else if (itemId == 60084)
					skillId = 35212;
					else if (itemId == 60085)
					skillId = 35214;
					else if (itemId == 60086)
					skillId = 35216;
					else if (itemId == 60087)
					skillId = 35218;
					else if (itemId == 60088)
					skillId = 35220;
					else if (itemId == 60089)
					skillId = 35222;
					else if (itemId == 60090)
					skillId = 35224;
					else if (itemId == 60091)
					skillId = 35226;
					else if (itemId == 60092)
					skillId = 35228;
					else if (itemId == 60093)
					skillId = 35230;
					else if (itemId == 60094)
					skillId = 35232;
					else if (itemId == 60095)
					skillId = 35234;
					else if (itemId == 60096)
					skillId = 35236;
					else if (itemId == 60097)
					skillId = 35238;
					else if (itemId == 60098)
					skillId = 35240;
					else if (itemId == 60099)
					skillId = 35242;
					else if (itemId == 60100)
					skillId = 35244;
					else if (itemId == 60101)
					skillId = 35246;
					else if (itemId == 60102)
					skillId = 35248;
					else if (itemId == 60103)
					skillId = 35250;
					else if (itemId == 60104)
					skillId = 35252;
					else if (itemId == 60105)
					skillId = 35254;
					else if (itemId == 60106)
					skillId = 35256;
					else if (itemId == 60107)
					skillId = 35258;
					else if (itemId == 60108)
					skillId = 35260;
					else if (itemId == 60109)
					skillId = 35262;
					else if (itemId == 60110)
					skillId = 35264;
				
			
				for (L2Skill desiredSkill : player.getAllSkills())
				{
					if (desiredSkill.getId() == skillId)
					{
						playerGotSkill = true;
					}
				}
				if(playerGotSkill)
				{
					player.destroyItemByItemId("Hax Certi", itemId, player.getInventory().getItemByObjectId(itemobjId).getCount(), null, true);
					player.removeSkill(skillId);
					player.sendSkillList();
					player.broadcastUserInfo();
					checkForSkills(player, skillId);

					if (mikra(itemId))
					{
						player.addItem("Hax Certi", 51003, 5, null, true);
					}
					else if (messaia(itemId))
					{
						player.addItem("Hax Certi", 51004, 3, null, true);
					}
					else if (megala(itemId))
					{
						player.addItem("Hax Certi", 51005, 2, null, true);
					}
				}
				else
				{
					player.sendMessage("You must learn the skill before you hax it.");
				}
			}
			catch (Exception e)
			{
				player.sendMessage("You fucked up something.");
			}
		}
    	super.onBypassFeedback(player, command);
    }
    public boolean mikra(int itemId)
    {
    	if(itemId == 60078 || itemId == 60079 || itemId == 60080 || itemId == 60081 || itemId == 60107 || itemId == 60088 || itemId == 60094
    	|| itemId == 60106 || itemId == 60091 || itemId == 60110)
    	{
    		return true;
    	}
    	else
    		return false;
	}
    public boolean messaia(int itemId)
    {
    	if(itemId == 60082 || itemId == 60083 || itemId == 60084 || itemId == 60090 || itemId == 60086 || itemId == 60087 || itemId == 60092
    	|| itemId == 60105 || itemId == 60085 || itemId == 60089 || itemId == 60101)
    	{
    		return true;
    	}
    	else
    		return false;
	}
    public boolean megala(int itemId)
    {
    	if(itemId == 60109 || itemId == 60108 || itemId == 60103 || itemId == 60102 || itemId == 60099 || itemId == 60097 || itemId == 60096
    	|| itemId == 60098 || itemId == 60104 || itemId == 60100 || itemId == 60095)
    	{
    		return true;
    	}
    	else
    		return false;
	}
    public static void checkForSkills(L2PcInstance activeChar, int skillId)
    {
    	L2GameClient client = activeChar.getClient();
    	Connection con = null;
    	try
    	{
    		PreparedStatement statement;
    		ResultSet rs;
    		
    		con = L2DatabaseFactory.getInstance().getConnection();

    		statement = con.prepareStatement("delete from character_skills where skill_id = ? and charId= ?;");
    		
			statement.setInt(1, skillId);
    		statement.setLong(2, activeChar.getObjectId());
    		statement.execute();
    		statement.close();
    	}
    	catch (Exception e)
    	{
    		_log.log(Level.SEVERE, "Failed storing ip", e);
    	}
    	finally
    	{
    		try { con.close(); } catch (Exception e) {}
    	}
    	
    }
	private void showMessageWindow(L2PcInstance player, int page)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
        NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(player.getHtmlPrefix(), "data/html/custom/haxCerti.htm");
		final StringBuilder skillHTML = StringUtil.startAppend(1000, "");
		
		skillHTML.append("<center><table>");
		
		L2ItemInstance[] items;
		items = player.getInventory().getAllCertsByItemId();

		int maxItemsPerPage = 10;
		int maxPages = items.length / maxItemsPerPage;
		if (items.length > (maxItemsPerPage * maxPages))
		{
			maxPages++;
		}
		
		if (page > maxPages)
		{
			page = maxPages;
		}
		
		int itemsStart = maxItemsPerPage * page;
		int itemsEnd = items.length;
		if ((itemsEnd - itemsStart) > maxItemsPerPage)
		{
			itemsEnd = itemsStart + maxItemsPerPage;
		}
		
		html.replace("%PLAYER_NAME%", player.getName());
		
		StringBuilder sbPages = new StringBuilder();
		for (int x = 0; x < maxPages; x++)
		{
			int pagenr = x + 1;
			sbPages.append("<td><button value=\"" + String.valueOf(pagenr) + "\" action=\"bypass -h npc_%objectId%_show_inventory " + String.valueOf(x) + "\" width=20 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		}
		
		html.replace("%PAGES%", sbPages.toString());
		
		StringBuilder sbItems = new StringBuilder();
		for (int i = itemsStart; i < itemsEnd; i++)
		{
				sbItems.append("<tr><td><img src=\"" + items[i].getItem().getIcon() + "\" width=32 height=32></td>");
				sbItems.append("<td width=60>" + items[i].getName() + "</td>");
				sbItems.append("<td><button action=\"bypass -h npc_%objectId%_delete_item " + String.valueOf(items[i].getObjectId()) +" "+  String.valueOf(items[i].getItemId()) + "\" width=16 height=16 back=\"L2UI_ct1.Button_DF_Delete\" fore=\"L2UI_ct1.Button_DF_Delete\">" + "</td></tr>");
		}
		html.replace("%ITEMS%", sbItems.toString());
        html.replace("%objectId%", String.valueOf(getObjectId()));
        player.sendPacket(html);
    }
}