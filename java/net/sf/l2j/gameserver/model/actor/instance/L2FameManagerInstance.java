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
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/**
 * Reputation score manager
 * @author Kerberos
 */
public class L2FameManagerInstance extends L2Npc
{
	public L2FameManagerInstance(int objectId, L2NpcTemplate template)
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
				showMessageWindow(player);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
    @Override
    public void onBypassFeedback(L2PcInstance player, String command)
    {
    	StringTokenizer st = new StringTokenizer(command, " ");
    	String actualCommand = st.nextToken();
    	if (actualCommand.equalsIgnoreCase("PK_Count"))
        {
    		NpcHtmlMessage html = new NpcHtmlMessage(1);
    		
    		if (player.getFame() >= 5000)
    		{
    			if (player.getPkKills() > 0)
    			{
    				player.setFame(player.getFame()-5000);
    				player.setPkKills(player.getPkKills()-1);
    				player.sendPacket(new UserInfo(player));
    				player.sendPacket(new ExBrExtraUserInfo(player));
    				html.setFile("data/html/famemanager/"+getNpcId()+"-3.htm");
    			}
    			else
    			{
    				html.setFile("data/html/famemanager/"+getNpcId()+"-4.htm");
    			}
    		}
    		else
    		{
    			html.setFile("data/html/famemanager/"+getNpcId()+"-lowfame.htm");
    		}
    		sendHtmlMessage(player, html);
        }
    	else if (actualCommand.equalsIgnoreCase("CRP"))
        {
    		NpcHtmlMessage html = new NpcHtmlMessage(1);
    		if (player.getFame() >= 1000 && player.getClassId().level() >= 2 && player.getClan() != null && player.getClan().getLevel() >= 5)
    		{
    			player.setFame(player.getFame()-1000);
    			player.getClan().setReputationScore(player.getClan().getReputationScore()+50, true);
    			player.sendPacket(new SystemMessage(SystemMessageId.ACQUIRED_50_CLAN_FAME_POINTS));
    			html.setFile("data/html/famemanager/"+getNpcId()+"-5.htm");
    		}
    		else
    		{
    			html.setFile("data/html/famemanager/"+getNpcId()+"-lowfame.htm");
    		}
    		sendHtmlMessage(player, html);
        }
    	super.onBypassFeedback(player, command);
    }
    
    private void sendHtmlMessage(L2PcInstance player, NpcHtmlMessage html)
    {
        html.replace("%objectId%", String.valueOf(getObjectId()));
        player.sendPacket(html);
    }
    
	private void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/famemanager/"+getNpcId()+"-lowfame.htm";
		
		if (player.getFame() > 0)
			filename = "data/html/famemanager/"+getNpcId()+".htm";
        NpcHtmlMessage html = new NpcHtmlMessage(1);
        html.setFile(filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));
        player.sendPacket(html);
    }
}