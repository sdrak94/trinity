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

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author Vice 
 */
public class L2FortLogisticsInstance extends L2MerchantInstance
{
	private static final int BLOOD_OATH = 9910;
	
    public L2FortLogisticsInstance(int objectID, L2NpcTemplate template)
    {
        super(objectID, template);
    }

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
                showMessageWindow(player);
            }
        }
        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    public void onBypassFeedback(L2PcInstance player, String command)
    {
    	// BypassValidation Exploit plug.
		if (player.getLastFolkNPC().getObjectId() != this.getObjectId())
			return;
        StringTokenizer st = new StringTokenizer(command, " ");
        String actualCommand = st.nextToken(); // Get actual command

        String par = "";
        if (st.countTokens() >= 1) {par = st.nextToken();}

        if (actualCommand.equalsIgnoreCase("Chat"))
        {
            int val = 0;
            try
            {
                val = Integer.parseInt(par);
            }
            catch (IndexOutOfBoundsException ioobe){}
            catch (NumberFormatException nfe){}
            showMessageWindow(player, val);
        }
        else if (actualCommand.equalsIgnoreCase("rewards"))
        {
        	if (player.getClan() != null && getFort().getOwnerClan() != null && player.getClan() == getFort().getOwnerClan() && player.isClanLeader())
        	{
        		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
    			html.setFile("data/html/fortress/logistics-rewards.htm");
    			int blood = getFort().getBloodOathReward();
    			html.replace("%objectId%", String.valueOf(getObjectId()));
    			html.replace("%bloodoath%", String.valueOf(blood));
    			player.sendPacket(html);
    			return;
        	}
        	else
        	{
        		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
    			html.setFile("data/html/fortress/logistics-noprivs.htm");
    			html.replace("%objectId%", String.valueOf(getObjectId()));
    			player.sendPacket(html);
    			return;
        	}
        }
        else if (actualCommand.equalsIgnoreCase("blood"))
        {
        	if (player.getClan() != null && getFort().getOwnerClan() != null && player.getClan() == getFort().getOwnerClan() && player.isClanLeader())
        	{
        		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        		int blood = getFort().getBloodOathReward();
        		if (blood > 0)
        		{
        			html.setFile("data/html/fortress/logistics-blood.htm");
        			player.addItem("Quest", BLOOD_OATH, blood, this, true);
        			getFort().setBloodOathReward(0);
        		}
        		else
        			html.setFile("data/html/fortress/logistics-noblood.htm");
    			html.replace("%objectId%", String.valueOf(getObjectId()));
    			
    			player.sendPacket(html);
    			return;
        	}
        	else
        	{
        		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
    			html.setFile("data/html/fortress/logistics-noprivs.htm");
    			html.replace("%objectId%", String.valueOf(getObjectId()));
    			player.sendPacket(html);
    			return;
        	}
        }
        else
        {
            super.onBypassFeedback(player, command);
        }
    }
    
    private void showMessageWindow(L2PcInstance player)
    {
        showMessageWindow(player, 0);
    }

    private void showMessageWindow(L2PcInstance player, int val)
    {
        player.sendPacket(ActionFailed.STATIC_PACKET);

        String filename;

        if (val == 0)
            filename = "data/html/fortress/logistics.htm";
        else
            filename = "data/html/fortress/logistics-" + val + ".htm";

        NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile(filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));
        html.replace("%npcId%", String.valueOf(getNpcId()));
        if ( getFort().getOwnerClan() != null ) 
            html.replace("%clanname%", getFort().getOwnerClan().getName());
        else
            html.replace("%clanname%", "NPC");
        
        player.sendPacket(html);
    } 

    @Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}
