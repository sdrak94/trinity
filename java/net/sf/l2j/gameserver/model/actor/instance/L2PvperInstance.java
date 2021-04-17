package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;


public class L2PvperInstance extends L2NpcInstance
{
	public L2PvperInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		
		// initial menu
		if (currentCommand.startsWith("menu"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			final int curBuffs = player.getBuffCount();
			html.replace("%lol%", String.valueOf(curBuffs));
			final int maxBuffs = player.getMaxBuffCount();
			html.replace("%nig%", String.valueOf(maxBuffs));
			sendHtmlMessage(player, html);
		}
		
		
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void onAction(L2PcInstance player)
	{
		player.setLastFolkNPC(this);
		
		if (!canTarget(player))
			return;
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance
			// player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			
			// Send a Server->Client packet ValidateLocation to correct the
			// L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the
			// L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				// note: commented out so the player must stand close
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				final int curBuffs = player.getBuffCount();
				html.replace("%lol%", String.valueOf(curBuffs));
				final int maxBuffs = player.getMaxBuffCount();
				html.replace("%nig%", String.valueOf(maxBuffs));
				sendHtmlMessage(player, html);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to
		// avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/*
	 * @Override public void onActionShift(L2GameClient client) { L2PcInstance player =
	 * client.getActiveChar(); if (player == null) return;
	 * 
	 * if (player.getAccessLevel() >= Config.GM_ACCESSLEVEL) { TextBuilder tb = new TextBuilder();
	 * tb.append("<html><title>NPC Buffer - Admin</title>");
	 * tb.append("<body>Changing buffs feature is not implemented yet. :)<br>"); tb.append(
	 * "<br>Please report any bug/impression/suggestion/etc at http://l2jserver.com/forum. " +
	 * "<br>Contact <font color=\"00FF00\">House</font></body></html>");
	 * 
	 * NpcHtmlMessage html = new NpcHtmlMessage(1); html.setHtml(tb.toString());
	 * sendHtmlMessage(player, html);
	 * 
	 * } player.sendPacket(ActionFailed.STATIC_PACKET); }
	 */
	
	private void sendHtmlMessage(L2PcInstance player, NpcHtmlMessage html)
	{
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		player.sendPacket(html);
	}
	
}