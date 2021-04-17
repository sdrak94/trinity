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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import luna.custom.handler.html.PageBuilder;
import luna.custom.handler.html.PageResult;
import luna.custom.handler.html.styles.ButtonsStyle;
import net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager;
import net.sf.l2j.gameserver.datatables.FencesTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminFences extends BaseBBSManager implements IAdminCommandHandler
{
	protected static final Logger	_LOGGER			= Logger.getLogger(AdminFences.class.getName());
	private static final String[]	ADMIN_COMMANDS	=
	{
		"admin_fences",
		"admin_open_fence",
		"admin_close_fence",
		"admin_hide_fence",
		"admin_teleto_fence",
		"admin_reload_fences",
		"admin_spawn_fence",
		"admin_delete_fence",
		"admin_edit_fence",
		"admin_listfence"
	};

	int page = 0;
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken(); // Actual command
		if (command.equals("admin_fences"))
		{
			page = 0;
			if (st.hasMoreTokens())
			{
				page = Integer.parseInt(st.nextToken());
			}
			sendHtml(activeChar, page);
			return true;
		}
		else if (command.startsWith("admin_listfence"))
		{
			page = 0;
			if (st.hasMoreTokens())
			{
				page = Integer.parseInt(st.nextToken());
			}
			sendHtml(activeChar, page);
		}
		else if (command.startsWith("admin_open_fence"))
		{
			manageFanceState(activeChar, L2FenceInstance.UNCLOSED, st);
			sendHtml(activeChar, page);
		}
		else if (command.startsWith("admin_close_fence"))
		{
			manageFanceState(activeChar, L2FenceInstance.CLOSED, st);
			sendHtml(activeChar, page);
		}
		else if (command.startsWith("admin_hide_fence"))
		{
			manageFanceState(activeChar, L2FenceInstance.HIDDEN, st);
			sendHtml(activeChar, page);
		}
		else if (command.startsWith("admin_teleto_fence"))
		{
			try
			{
				int objectId = 0;
				if (st.hasMoreTokens())
				{
					objectId = Integer.parseInt(st.nextToken());
					L2FenceInstance fence = FencesTable.getInstance().getFence(objectId);
					activeChar.teleToLocation(fence.getX(), fence.getY(), fence.getZ());
					sendHtml(activeChar, page);
					return true;
				}
				sendHtml(activeChar, page);
				activeChar.sendMessage("Usage: //teleto_fence objectId");
			}
			catch (NumberFormatException nfex)
			{
				activeChar.sendMessage("Incorrect values, only numbers are accepted. Usage: //teleto_fence objectId");
			}
		}
		else if (command.equals("admin_reload_fences"))
		{
			FencesTable.getInstance().reload();
			activeChar.sendMessage("Reloaded stored fences.");
			sendHtml(activeChar, page);
			return true;
		}
		else if (command.startsWith("admin_spawn_fence"))
		{
			try
			{
				int instanceId = activeChar.getInstanceId();
				int width = 0;
				int length = 0;
				String name = "N/A";
				boolean saveToDB = false;
				if (st.hasMoreTokens())
				{
					name = st.nextToken();
					width = Integer.parseInt(st.nextToken());
					length = width;
					if (st.hasMoreTokens())
						length = Integer.parseInt(st.nextToken());
					if (st.hasMoreTokens())
						saveToDB = st.nextToken().equalsIgnoreCase("True");
					L2FenceInstance fence = new L2FenceInstance(name, activeChar.getX(), activeChar.getY(), activeChar.getZ(), width, length);
					fence.setName(name);
					fence.spawnMe();
					fence.setInstanceId(instanceId);
					if (saveToDB)
						FencesTable.getInstance().storeFence(fence);
					sendHtml(activeChar, page);
					return true;
				}
				sendHtml(activeChar, page);
				activeChar.sendMessage("Usage: //spawn_fence width&length OR //spawn_fence width length OR //spawn_fence width length storeToDB");
			}
			catch (NumberFormatException nfex)
			{
				activeChar.sendMessage("Incorrect values, only numbers are accepted. Usage: //spawn_fence width length");
				return false;
			}
			sendHtml(activeChar, page);
		}
		else if (command.startsWith("admin_delete_fence"))
		{
			try
			{
				int objectId = 0;
				boolean deleteFromDB = true;
				if (st.hasMoreTokens())
				{
					objectId = Integer.parseInt(st.nextToken());
					hideFence(activeChar, objectId);
					if (st.hasMoreTokens())
						deleteFromDB = !st.nextToken().isEmpty();
					FencesTable.getInstance().deleteFence(objectId, deleteFromDB);
					sendHtml(activeChar, page);
					return true;
				}
				hideFence(activeChar, objectId);
				activeChar.sendMessage("Usage: //delete_fence objectId OR //delete_fence objectId deleteFromDB");
				sendHtml(activeChar, 0);
			}
			catch (NumberFormatException nfex)
			{
				activeChar.sendMessage("Incorrect values, only numbers are accepted. Usage: //delete_fence objectId");
				return false;
			}
			sendHtml(activeChar, page);
		}
		else if (command.startsWith("admin_edit_fence"))
		{
			sendHtml(activeChar, page);
		}
		return false;
	}
	
	private void hideFence(L2PcInstance activeChar, int fenceId)
	{
		L2FenceInstance fence = FencesTable.getInstance().getFence(fenceId);
		if (fence != null)
		{
			fence.setState(L2FenceInstance.HIDDEN);
			for (L2PcInstance plr : L2World.getInstance().getAllPlayers().values())
				fence.sendInfo(plr);
		}
		sendHtml(activeChar, page);
	}
	
	private void manageFanceState(L2PcInstance activeChar, int state, StringTokenizer st)
	{
		try
		{
			int objectId = 0;
			if (st.hasMoreTokens())
			{
				objectId = Integer.parseInt(st.nextToken());
				L2FenceInstance fence = FencesTable.getInstance().getFence(objectId);
				if (fence != null)
				{
					fence.setState(state);
					for (L2PcInstance plr : L2World.getInstance().getAllPlayers().values())
						fence.sendInfo(plr);
				}
				return;
			}
			if(activeChar != null)
			{
				activeChar.sendMessage("Usage: //open_fence objectId");
			}
		}
		catch (NumberFormatException nfex)
		{
			activeChar.sendMessage("Incorrect values, only numbers are accepted. Usage: //open_fence objectId");
		}
	}
	
	private String getFenceColor(int fenceId)
	{
		String color = "";
		switch (FencesTable.getInstance().getFence(fenceId).getState())
		{
			case 0:
				color = "766060";
				break;
			case 1:
				color = "457966";
				break;
			case 2:
				color = "2491C4";
				break;
		}
		return color;
	}
	private void sendHtml(L2PcInstance activeChar, int page)
	{
		final PageResult result = PageBuilder.newBuilder(FencesTable.getInstance().getFences().values(), 8, "bypass -h admin_listfence").currentPage(page).style(ButtonsStyle.INSTANCE).bodyHandler((pages, fence, sb) ->
		{
			sb.append("<tr><td width=175 height=24 ><font color="+getFenceColor(fence.getObjectId())+">");
			sb.append(fence.getName() == null ? fence.getObjectId() : fence.getName());
			sb.append("</font></td><td>");
			sb.append("<button value=\"Go\" action=\"bypass -h admin_teleto_fence ");
			sb.append(fence.getObjectId());
			sb.append("\" width=32 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			sb.append("</td><td>");
			sb.append("<button value=\"Hide\" action=\"bypass -h admin_hide_fence ");
			sb.append(fence.getObjectId());
			sb.append(" 0");
			sb.append("\" width=43 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			sb.append("</td><td>");
			sb.append("<button value=\"Normal\" action=\"bypass -h admin_close_fence ");
			sb.append(fence.getObjectId());
			sb.append(" 1");
			sb.append("\" width=54 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			sb.append("</td><td>");
			sb.append("<button value=\"No-Bars\" action=\"bypass -h admin_open_fence ");
			sb.append(fence.getObjectId());
			sb.append(" 2");
			sb.append("\" width=56 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			sb.append("</td><td>");
			sb.append("<button value=\"X\" action=\"bypass -h admin_delete_fence ");
			sb.append(fence.getObjectId());
			sb.append("\" width=26 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			sb.append("</td></tr>");
		}).build();
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile("data/html/admin/fences.htm");
		if (result.getPages() > 0)
		{
			html.replace("%pages%", "<table width=280 cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table>");
		}
		else
		{
			html.replace("%pages%", "");
		}
		html.replace("%fences%", result.getBodyTemplate().toString());
		//activeChar.sendPacket(html);
		//parsecmd(html.toString(), activeChar);
		separateAndSend(html.getText(), activeChar);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		//separateAndSend(command, activeChar);
	}

	@Override
	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		if (!activeChar.isGM())
		{
			return;
		}
		
	}
}