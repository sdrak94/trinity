package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.GMAudit;
import net.sf.l2j.gameserver.util.StringUtil;

public class AdminBuffs implements IAdminCommandHandler
{

private static final String[] ADMIN_COMMANDS =
{
	"admin_getbuffs",
	"admin_stopbuff",
	"admin_stopallbuffs",
	"admin_getparty",
	"admin_joinparty",
	"admin_areacancel"
};

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	String target = (activeChar.getTarget() != null) ? activeChar.getTarget().getName() : "no-target";
	GMAudit.auditGMAction(activeChar.getName(), command, target, "");
	
	if (command.startsWith("admin_getbuffs"))
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		command = st.nextToken();
		
		if (st.hasMoreTokens())
		{
			L2PcInstance player = null;
			String playername = st.nextToken();
			
			try
			{
				player = L2World.getInstance().getPlayer(playername);
			}
			catch (Exception e)
			{
			}
			
			if (player != null)
			{
				showBuffs(player, activeChar);
				return true;
			}
			else
			{
				activeChar.sendMessage("The player " + playername + " is not online");
				return false;
			}
		}
		else if ((activeChar.getTarget() != null)
				&& (activeChar.getTarget() instanceof L2PcInstance))
		{
			showBuffs((L2PcInstance) activeChar.getTarget(), activeChar);
			return true;
		}
		else
			return true;
	}
	else if (command.startsWith("admin_getparty"))
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		command = st.nextToken();
		
		if (st.hasMoreTokens())
		{
			L2PcInstance player = null;
			String playername = st.nextToken();
			
			try
			{
				player = L2World.getInstance().getPlayer(playername);
			}
			catch (Exception e)
			{
			}
			
			if (player != null)
			{
				showParty(player, activeChar);
				return true;
			}
			else
			{
				activeChar.sendMessage("The player " + playername + " is not online");
				return false;
			}
		}
		else if ((activeChar.getTarget() != null) && (activeChar.getTarget() instanceof L2PcInstance))
		{
			showParty((L2PcInstance) activeChar.getTarget(), activeChar);
			return true;
		}
		else
			return true;
	}
	else if (command.startsWith("admin_joinparty"))
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		command = st.nextToken();
		
		if (st.hasMoreTokens())
		{
			L2PcInstance player = null;
			String playername = st.nextToken();
			
			try
			{
				player = L2World.getInstance().getPlayer(playername);
			}
			catch (Exception e)
			{
			}
			
			if (player != null)
			{
				joinParty(player, activeChar);
				return true;
			}
			else
			{
				activeChar.sendMessage("The player " + playername + " is not online");
				return false;
			}
		}
		else if ((activeChar.getTarget() != null) && (activeChar.getTarget() instanceof L2PcInstance))
		{
			joinParty((L2PcInstance) activeChar.getTarget(), activeChar);
			return true;
		}
		else
			return true;
	}
	else if (command.startsWith("admin_stopbuff"))
	{
		try
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			
			st.nextToken();
			String playername = st.nextToken();
			int SkillId = Integer.parseInt(st.nextToken());
			
			removeBuff(activeChar, playername, SkillId);
			return true;
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Failed removing effect: "
					+ e.getMessage());
			activeChar.sendMessage("Usage: //stopbuff <playername> [skillId]");
			return false;
		}
	}
	else if (command.startsWith("admin_stopallbuffs"))
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		String playername = st.nextToken();
		if (playername != null)
		{
			removeAllBuffs(activeChar, playername);
			return true;
		}
		else
			return false;
		
	}
	else if (command.startsWith("admin_areacancel"))
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		String val = st.nextToken();
		try
		{
			int radius = Integer.parseInt(val);
			
			for (L2Character knownChar : activeChar.getKnownList().getKnownCharactersInRadius(radius))
			{
				if ((knownChar instanceof L2PcInstance)
						&& !(knownChar.equals(activeChar)))
					knownChar.stopAllEffects();
			}
			
			activeChar.sendMessage("All effects canceled within raidus "
					+ radius);
			return true;
		}
		catch (NumberFormatException e)
		{
			activeChar.sendMessage("Usage: //areacancel <radius>");
			return false;
		}
	}
	else
		return true;
	
}

private void joinParty(L2PcInstance target, L2PcInstance activeChar)
{
	if (target.getAccessLevel().getLevel() > activeChar.getAccessLevel().getLevel())
		return;
	
	final L2Party activeParty = activeChar.getParty();
	final L2Party targetParty = target.getParty();
	
	if (activeParty != null)
	{
		if (activeParty.getMemberCount() < 10)
		{
			if (targetParty != null)
				target.leaveParty();
			
			target.joinParty(activeParty);
			activeChar.sendMessage("Added "+target.getName()+" to your party");
		}
		else
		{
			activeChar.sendMessage("Your own party is full");
		}
	}
	else
	{
		if (targetParty != null)
		{
			if (targetParty.getMemberCount() < 10)
			{
				activeChar.joinParty(targetParty);
				activeChar.sendMessage("Joined "+target.getName()+"'s party");
				return;
			}
			else
			{
				target.leaveParty();
			}
		}
		
		activeChar.setParty(new L2Party(activeChar, 3));
		target.joinParty(activeChar.getParty());
		activeChar.sendMessage("Created a new party with "+target.getName());
	}
}

public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}

public void showBuffs(L2PcInstance player, L2PcInstance activeChar)
{
	final L2Effect[] effects = player.getAllEffects();
	final StringBuilder html = StringUtil.startAppend(500 + effects.length * 200,
			"<html><center><font color=\"LEVEL\">Effects of ",
			player.getName(),
			"</font><center><br>" +
			"<table>" +
			"<tr><td width=200>Skill</td><td width=70>Action</td></tr>"
	);
	
	for (L2Effect e : effects) {
		if (e != null) {
			StringUtil.append(html,
					"<tr><td>",
					e.getSkill().getName(),
					"</td><td><button value=\"Remove\" action=\"bypass -h admin_stopbuff ",
					player.getName(),
					" ",
					String.valueOf(e.getSkill().getId()),
					"\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>"
			);
		}
	}
	
	StringUtil.append(html,
			"</table><br>" +
			"<button value=\"Remove All\" action=\"bypass -h admin_stopallbuffs ",
			player.getName(),
			"\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"</html>"
	);
	
	NpcHtmlMessage ms = new NpcHtmlMessage(1);
	ms.setHtml(html.toString());
	
	activeChar.sendPacket(ms);
	
	GMAudit.auditGMAction(activeChar.getName(), "getbuffs", player.getName(), "");
}

public void showParty(L2PcInstance player, L2PcInstance activeChar)
{
	final L2Party party = player.getParty();
	
	if (party == null)
	{
		activeChar.sendMessage(player.getName()+" has no party");
		return;
	}
	
	final StringBuilder html = StringUtil.startAppend(500 + party.getMemberCount() * 200,
			"<html><center><font color=\"LEVEL\">Party of (Leader)",
			party.getLeader().getName() +" ("+ party.getMemberCount() + ")",
			"</font><center><br>" +
			"<table>" +
			"<tr><td width=120>Player</td><td width=150>IP</td></tr>"
	);
	
	for (L2PcInstance p : party.getPartyMembers())
	{
		if (p != null)
		{
			StringUtil.append(html,
					"<tr><td>",
					p.getName(),
					"</td><td>",
					p.getIP(),
					"</td></tr>"
			);
		}
	}
	
	StringUtil.append(html,
			"</table><br>" +
			"<button value=\"Join Da Party!\" action=\"bypass -h admin_joinparty ",
			player.getName(),
			"\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"</html>"
	);
	
	NpcHtmlMessage ms = new NpcHtmlMessage(1);
	ms.setHtml(html.toString());
	
	activeChar.sendPacket(ms);
	
	GMAudit.auditGMAction(activeChar.getName(), "getparty", player.getName(), "");
}

private void removeBuff(L2PcInstance remover, String playername, int SkillId)
{
	L2PcInstance player = null;
	try
	{
		player = L2World.getInstance().getPlayer(playername);
	}
	catch (Exception e)
	{
	}
	
	if ((player != null) && (SkillId > 0))
	{
		L2Effect[] effects = player.getAllEffects();
		
		for (L2Effect e : effects)
		{
			if ((e != null) && (e.getSkill().getId() == SkillId))
			{
				e.exit();
				remover.sendMessage("Removed " + e.getSkill().getName() + " level " + e.getSkill().getLevel() + " from " + playername);
			}
		}
		showBuffs(player, remover);
		GMAudit.auditGMAction(remover.getName(), "stopbuffs", playername, "");
	}
}

private void removeAllBuffs(L2PcInstance remover, String playername)
{
	L2PcInstance player = null;
	try
	{
		player = L2World.getInstance().getPlayer(playername);
	}
	catch (Exception e)
	{
	}
	
	if (player != null)
	{
		player.stopAllEffects();
		remover.sendMessage("Removed all effects from " + playername);
		GMAudit.auditGMAction(remover.getName(), "stopallbuffs", playername, "");
		showBuffs(player, remover);
	}
	else
	{
		remover.sendMessage("The player " + playername + " is not online");
	}
}

}