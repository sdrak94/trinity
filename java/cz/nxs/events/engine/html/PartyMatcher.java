/**
 * 
 */
package cz.nxs.events.engine.html;

import java.util.List;
import java.util.StringTokenizer;

import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.NpcData;
import javolution.text.TextBuilder;
import javolution.util.FastList;

/**
 * @author hNoke
 */
public class PartyMatcher
{
	public static int lastId = 0;
	public static List<PartyRecord> parties = new FastList<PartyRecord>();
	
	public static Cleaner cleaner = new Cleaner();
	
	public static void showMenu(PlayerEventInfo player, NpcData npc)
	{
		showMenu(player, npc, -1);
	}
	
	public static void showMenu(PlayerEventInfo player, NpcData npc, int expanded)
	{
		String html;
		
		TextBuilder tb = new TextBuilder();
		
		tb.append("<html><title>Party manager</title><body>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3><table width=280 bgcolor=353535><tr><td align=left width=200><font color=9F9400>Available party rooms:</font></td><td width=80 align=right><button value=\"Back\" action=\"bypass -h bon_menu\" width=65 height=19 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3>");
		
		int applicantIn = isApplicantSomewhere(player);
		for(PartyRecord party : parties)
		{
			String bgcolor = (applicantIn == party.partyId || party.leader.getPlayersId() == player.getPlayersId()) ? "444A13" : "2f2f2f";
			
			if(party.partyId == expanded)
			{
				tb.append("<table width=280 bgcolor=" + bgcolor + "><tr><td align=left width=120><font color=9F9400>" + party.leader.getPlayersName() + " (" + party.leader.getLevel() + ")</font></td><td width=40 align=left>" + party.getCurrentMembersCount() + "/" + party.membersWanted + "</td><td width=60 align=right></td><td width=50 align=right><button value=\"Open\" action=\"bypass -h nxs_pm_openparty " + party.partyId + "\" width=50 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
				tb.append("<table width=280 bgcolor=1f1f1f><tr><td width=280>" + party.message + "</td></tr></table>");
			}
			else
			{
				tb.append("<table width=280 bgcolor=" + bgcolor + "><tr><td align=left width=120><font color=9F9400>" + party.leader.getPlayersName() + " (" + party.leader.getLevel() + ")</font></td><td width=40 align=left>" + party.getCurrentMembersCount() + "/" + party.membersWanted + "</td><td width=60 align=right><button value=\"Expand\" action=\"bypass -h nxs_pm_expandparty " + party.partyId + "\" width=60 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td width=50 align=right><button value=\"Open\" action=\"bypass -h nxs_pm_openparty " + party.partyId + "\" width=50 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
			}
		}
		
		tb.append("<br><table width=290><tr><td align=left><button value=\"Refresh\" action=\"bypass -h nxs_pm_menu\" width=80 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td align=right><button value=\"Create new party\" action=\"bypass -h nxs_pm_createparty\" width=120 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		tb.append("<br1><table width=290><tr><td align=left></td></tr></table>");
		
		tb.append("</body></html>");

		html = tb.toString();
		
		player.sendHtmlText(html);
		player.sendStaticPacket();
	}
	
	public static void showRequestCreatePartyWindow(PlayerEventInfo player)
	{
		String html;
		
		TextBuilder tb = new TextBuilder();
		
		tb.append("<html><title>Party manager</title><body>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3><table width=280 bgcolor=353535><tr><td align=center width=280><font color=9F9400>Create party room</font></td></tr></table><img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3>");
		
		tb.append("<br>");
		
		tb.append("<font color=LEVEL>Write message for new members here:</font><br1><font color=9F9400>In this message, you shoud describe what kind of players (classes, equip) you are looking for and what the party will do.</font><br1><font color=7f7f7f>The data should not exceed 120 characters (about 2 lines of text).</font><br><multiedit var=\"desc\" width=280 height=32><br>");
		
		tb.append("<center><button value=\"Confirm and create room\" action=\"bypass -h nxs_pm_confirmcreateparty $desc\" width=160 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>");
		
		tb.append("</body></html>");

		html = tb.toString();
		
		player.sendHtmlText(html);
		player.sendStaticPacket();
	}
	
	public static void openPartyWindow(PlayerEventInfo player, int id)
	{
		PartyRecord data = null;
		for(PartyRecord pt : parties)
		{
			if(pt.partyId == id)
			{
				data = pt;
				break;
			}
		}
		
		if(data == null)
		{
			player.sendMessage("This party no longer exists in the party matching system");
			return;
		}
		
		String html;
		
		TextBuilder tb = new TextBuilder();
		
		tb.append("<html><title>Party manager</title><body>");
		
		boolean isLeader = data.leader.getPlayersId() == player.getPlayersId();
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3>");
		
		if(isLeader)
		{
			tb.append("<table width=280 bgcolor=353535><tr><td align=left width=90><font color=LEVEL>[Party room]</font></td><td width=30></td><td width=100 align=right><a action=\"bypass -h nxs_pm_removeparty " + data.partyId + "\"><font color=F16363>Unregister party</font></a></td><td width=60 align=right><a action=\"bypass -h nxs_pm_openparty " + data.partyId + "\">Refresh</a></td></tr></table>");
		}
		else
		{
			tb.append("<table width=280 bgcolor=353535><tr><td align=left width=100><font color=LEVEL>[Party room]</font></td><td width=180 align=right><a action=\"bypass -h nxs_pm_openparty " + data.partyId + "\">Refresh</a></td></tr></table>");
		}
		
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3>");
		
		tb.append("<br>");
		
		tb.append("<table bgcolor=2f2f2f width=280><tr><td width=90 align=left><font color=C8BB91>Party Leader:</font></td><td width=170 align=left><font color=B9B9B9>" + data.leader.getPlayersName() + "</font>&nbsp;&nbsp;&nbsp;<font color=4f4f4f>(PM him)</font></td></tr></table>");
		
		boolean isInParty = false;
		int membersSize = 1;
		
		if(data.getParty() == null)
		{
			if(data.leader.getPlayersId() == player.getPlayersId())
				isInParty = true;
		}
		
		
		if(data.getParty() != null)
		{
			PlayerEventInfo[] members = data.getParty().getPartyMembers();
			membersSize = members.length;
			
			for(PlayerEventInfo m : members)
			{
				if(m.getPlayersId() == player.getPlayersId())
				{
					isInParty = true;
					break;
				}
			}
			
			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=90><font color=ac9887>Members (" + members.length + "):</font></td>");
			
			for(int i = 0; i < Math.min(2, members.length); i++)
			{
				tb.append("<td width=90><font color=7f7f7f>" + members[i].getPlayersName() + " (" + members[i].getLevel() + ")</font></td>");
			}
			
			tb.append("</tr></table>");
			
			if(members.length > 2)
			{
				tb.append("<table bgcolor=2f2f2f width=280><tr>");
				
				int limitPerRow = 0;
				for(int i = 2; i < members.length; i++)
				{
					if(limitPerRow == 3)
					{
						tb.append("</tr><tr>");
						limitPerRow = 0;
					}
					
					tb.append("<td width=90><font color=7f7f7f>" + members[i].getPlayersName() + " (" + members[i].getLevel() + ")</font></td>");
						
					limitPerRow++;
				}
				
				if(limitPerRow == 1)
					tb.append("<td width=90></td><td width=90></td>");
				else if(limitPerRow == 2)
					tb.append("<td width=90></td>");
				
				tb.append("</tr></table>");
			}
		}
		else
		{
			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=center><font color=ac9887>The party is currently empty.</font></td></tr></table>");
		}
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=10>");
		
		if(isLeader)
		{
			tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3><table bgcolor=353535 width=280><tr><td width=280 align=center><font color=ac9887>Message for members:</font></td></tr><tr><td width=280 align=center><br1><font color=7f7f7f>" + data.message + "</font><br1></td></tr></table><img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3>");
			tb.append("<table width=280 bgcolor=3f3f3f><tr><td width=190 align=left><multiedit var=\"desc\" width=190 height=32></td><td width=80 align=right><button value=\"Set message\" action=\"bypass -h nxs_pm_setmessage " + data.partyId + " $desc\" width=80 height=40 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3>");
		}
		else
		{
			tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3><table bgcolor=353535 width=280><tr><td width=280 align=center><font color=ac9887>Message for members:</font></td></tr><tr><td width=280 align=center><br1><font color=7f7f7f>" + data.message + "</font><br1></td></tr></table><img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3>");
		}
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=10>");
		
		if(isLeader)
		{
			if(data.membersWanted > 0)
				tb.append("<table bgcolor=2f2f2f width=280><tr><td width=80 align=left><font color=ac9887>Optimal size:</font></td><td width=80 align=left><font color=9f9f9f>" + data.membersWanted + " members</font></td><td width=40 align=left><edit var=\"members\" width=40 height=16></td><td width=40 align=right><button value=\"Set\" action=\"bypass -h nxs_pm_setmembers " + data.partyId + " $members\" width=40 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
			else
				tb.append("<table bgcolor=2f2f2f width=280><tr><td width=80 align=left><font color=ac9887>Optimal size:</font></td><td width=80 align=left><font color=BD5959>(set the number)</font></td><td width=40 align=left><edit var=\"members\" width=40 height=16></td><td width=40 align=right><button value=\"Set\" action=\"bypass -h nxs_pm_setmembers " + data.partyId + " $members\" width=40 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		}
		else if(data.membersWanted > 1)
		{
			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=100 align=left><font color=ac9887>Optimal party size...</font></td><td width=100 align=right><font color=9f9f9f>" + data.membersWanted + " members</font></td></tr></table>");
			
			if(data.membersWanted <= membersSize)
				tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=center><font color=A96D6D>(the party seems to be 'full' already)</font></td></tr></table>");
		}
		
		if(!isInParty)
		{
			if(data.isApplicant(player))
				tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=15><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3><table bgcolor=353535 width=280><tr><td width=80 align=left><font color=ac9887>Applicants:</font></td><td width=180 align=right><button value=\"Unregister\" action=\"bypass -h nxs_pm_unrequestjoinparty " + data.partyId + " $desc\" width=130 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3>");
			else if(data.isBanned(player))
				tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=15><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3><table bgcolor=353535 width=280><tr><td width=80 align=left><font color=ac9887>Applicants:</font></td><td width=180 align=right><font color=FF0000>You are banned from this party</font></td></tr></table><img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3>");
			else
				tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=15><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3><table bgcolor=353535 width=280><tr><td width=80 align=left><font color=ac9887>Applicants:</font></td><td width=180 align=right><button value=\"Request join party\" action=\"bypass -h nxs_pm_requestjoinparty " + data.partyId + " $desc\" width=130 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3>");
			
			for(PlayerEventInfo applicant : data.getApplicants())
			{
				if(applicant.getPlayersId() == player.getPlayersId())
					tb.append("<table width=280 bgcolor=3f3f3f><tr><td width=120 align=left><font color=C5BA94>" + applicant.getPlayersName() + " (" + applicant.getLevel() + ")</font></td><td width=80 align=left><font color=8f8f8f>" + applicant.getClassName() + "</font></td></tr></table>");
				else
					tb.append("<table width=280 bgcolor=1f1f1f><tr><td width=120 align=left><font color=C5BA94>" + applicant.getPlayersName() + " (" + applicant.getLevel() + ")</font></td><td width=80 align=left><font color=8f8f8f>" + applicant.getClassName() + "</font></td></tr></table>");
			}
		}
		else
		{
			tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=15><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3><table bgcolor=353535 width=280><tr><td width=280 align=center><font color=ac9887>Candidates:</font></td></tr></table><img src=\"L2UI.SquareBlank\" width=280 height=3><img src=\"L2UI.SquareGray\" width=280 height=2><img src=\"L2UI.SquareBlank\" width=270 height=3>");
			
			for(PlayerEventInfo applicant : data.getApplicants())
			{
				tb.append("<table width=280 bgcolor=1f1f1f><tr><td width=120 align=left><font color=C5BA94>" + applicant.getPlayersName() + " </font><font color=6f6f6f>" + applicant.getLevel() + "</font></td><td width=80 align=left><font color=8f8f8f>" + applicant.getClassName() + "</font></td><td width=30 align=right><a action=\"bypass -h nxs_pm_acceptmember " + applicant.getPlayersName() + "\"><font color=4DEA48>Add</font></a></td><td width=20 align=right><a action=\"bypass -h nxs_pm_banmember " + applicant.getPlayersName() + "\"><font color=E64D4D>X</font></a></td></tr></table>");
			}
		}
		
		tb.append("</body></html>");

		html = tb.toString();
		
		player.sendHtmlText(html);
		player.sendStaticPacket();
	}
	
	public static void createParty(PlayerEventInfo player, int membersWanted, String message)
	{
		int id = ++lastId;
		PartyRecord data = new PartyRecord(id, player, message, membersWanted);
		parties.add(data);
		
		openPartyWindow(player, id);
	}
	
	public static int isApplicantSomewhere(PlayerEventInfo player)
	{
		for(PartyRecord pt : parties)
		{
			if(pt.isApplicant(player))
			{
				return pt.partyId;
			}
		}
		return -1;
	}
	
	public static boolean onBypass(PlayerEventInfo player, String action)
	{
		if(action.equals("menu"))
		{
			showMenu(player, null, -1);
			return true;
		}
		else if(action.startsWith("createparty"))
		{
			for(PartyRecord pt : parties)
			{
				if(pt.leader.getPlayersId() == player.getPlayersId())
				{
					player.sendMessage("You have already created a party room.");
					return true;
				}
				
				if(pt.isApplicant(player))
				{
					player.sendMessage("You have already joined another party's room.");
					return true;
				}
				
				if(pt.getParty() != null && player.getParty() != null && player.getParty().getLeadersId() == pt.leader.getPlayersId())
				{
					player.sendMessage("Your party has a party room already.");
					return true;
				}
			}
			
			if(parties.size() > 12)
			{
				player.sendMessage("There can't be more than 12 party rooms opened at a time.");
				player.sendMessage("You can either try to join an existing party, or wait because every room is deleted after one hour.");
				return true;
			}
			
			showRequestCreatePartyWindow(player);
			return true;
		}
		else if(action.startsWith("confirmcreateparty"))
		{
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			try
			{
				String msg = "";
				while(st.hasMoreTokens())
				{
					msg = msg + " " + st.nextToken();
				}
				
				if(msg.length() > 120)
				{
					player.sendMessage("The message was too long. It cannot be longer than 120 chars (about 2 lines of text).");
					showMenu(player, null);
					return true;
				}
				
				if(msg.length() < 7)
				{
					player.sendMessage("The message is too short. It has to be at least 6 characters.");
					showMenu(player, null);
					return true;
				}
				
				createParty(player, -1, msg);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				player.sendMessage("Wrong parameters.");
			}
			
			return true;
		}
		else if(action.startsWith("setmessage"))
		{
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			try
			{
				int id = Integer.parseInt(st.nextToken());
				
				PartyRecord data = null;
				for(PartyRecord pt : parties)
				{
					if(pt.partyId == id)
					{
						data = pt;
						break;
					}
				}
				
				if(data == null) 
				{
					player.sendMessage("This party room no longer exists.");
					return true;
				}
				
				String msg = "";
				while(st.hasMoreTokens())
				{
					msg = msg + " " + st.nextToken();
				}
				
				if(msg.length() > 120)
				{
					player.sendMessage("The message was too long. It cannot be longer than 120 chars (about 2 lines of text)");
					openPartyWindow(player, id);
					return true;
				}
				
				if(data != null)
				{
					data.message = msg;
				}
				
				openPartyWindow(player, id);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				player.sendMessage("Wrong parameters. The count of members must be a number.");
			}
			
			return true;
		}
		else if(action.startsWith("expandparty"))
		{
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			try
			{
				int id = Integer.parseInt(st.nextToken());
				
				showMenu(player, null, id);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				player.sendMessage("Wrong parameters.");
			}
			
			return true;
		}
		else if(action.startsWith("openparty"))
		{
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			try
			{
				int id = Integer.parseInt(st.nextToken());
				
				openPartyWindow(player, id);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				player.sendMessage("Wrong parameters.");
			}
			
			return true;
		}
		else if(action.startsWith("setmembers"))
		{
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			try
			{
				int id = Integer.parseInt(st.nextToken());
				int number = 1;
				
				try
				{
					number = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{
					player.sendMessage("You didn't write the number of members.");
					return true;
				}
				
				if(number >= 10 || number < 2)
				{
					player.sendMessage("Count of members must be at least 2 and less than 10.");
					openPartyWindow(player, id);
					return true;
				}
				
				PartyRecord data = null;
				for(PartyRecord pt : parties)
				{
					if(pt.partyId == id)
					{
						data = pt;
						break;
					}
				}
				
				if(data == null) 
				{
					player.sendMessage("This party room no longer exists.");
					return true;
				}
				else
				{
					data.membersWanted = number;
				}
				
				openPartyWindow(player, id);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				player.sendMessage("Wrong parameters.");
			}
			
			return true;
		}
		else if(action.startsWith("removeparty"))
		{
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			try
			{
				int id = Integer.parseInt(st.nextToken());
				
				PartyRecord data = null;
				for(PartyRecord pt : parties)
				{
					if(pt.partyId == id)
					{
						data = pt;
						break;
					}
				}
				
				if(data == null) 
				{
					player.sendMessage("This party room no longer exists.");
					return true;
				}
				
				if(data.leader.getPlayersId() == player.getPlayersId())
				{
					parties.remove(data);
					player.sendMessage("Your party room has been removed from the party-matching system.");
					showMenu(player, null);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				player.sendMessage("Wrong parameters.");
			}
			
			return true;
		}
		else if(action.startsWith("requestjoinparty"))
		{
			if(isApplicantSomewhere(player) > -1)
			{
				player.sendMessage("You have already registered for another party.");
				return true;
			}
			
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			try
			{
				int id = Integer.parseInt(st.nextToken());
				
				PartyRecord data = null;
				for(PartyRecord pt : parties)
				{
					if(pt.partyId == id)
					{
						data = pt;
						break;
					}
				}
				
				if(data == null) 
				{
					player.sendMessage("This party room no longer exists.");
					return true;
				}
				
				data.addApplicant(player);
				
				if(data.leader != null)
					data.leader.screenMessage(player.getPlayersName() + " (" + player.getLevel() + ") is requesting to join your party.", "PartyMatcher", false);
				
				openPartyWindow(player, id);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				player.sendMessage("Wrong parameters.");
			}
			
			return true;
		}
		else if(action.startsWith("unrequestjoinparty"))
		{
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			try
			{
				int id = Integer.parseInt(st.nextToken());
				
				PartyRecord data = null;
				for(PartyRecord pt : parties)
				{
					if(pt.partyId == id)
					{
						data = pt;
						break;
					}
				}
				
				if(data == null) 
				{
					player.sendMessage("This party room no longer exists.");
					return true;
				}
				
				data.removeApplicant(player.getPlayersName(), false);
				openPartyWindow(player, id);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				player.sendMessage("Wrong parameters.");
			}
			
			return true;
		}
		else if(action.startsWith("acceptmember"))
		{
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			try
			{
				String name = st.nextToken();
				
				PartyRecord data = null;
				for(PartyRecord pt : parties)
				{
					if(pt.leader.getPlayersId() == player.getPlayersId())
					{
						data = pt;
						break;
					}
				}
				
				if(data == null) 
				{
					player.sendMessage("This party room no longer exists.");
					return true;
				}
				
				data.removeApplicant(name, true);
				openPartyWindow(player, data.partyId);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				player.sendMessage("Wrong parameters.");
			}
			
			return true;
		}
		else if(action.startsWith("banmember"))
		{
			StringTokenizer st = new StringTokenizer(action);
			st.nextToken();
			
			try
			{
				String name = st.nextToken();
				
				PartyRecord data = null;
				for(PartyRecord pt : parties)
				{
					if(pt.leader.getPlayersId() == player.getPlayersId())
					{
						data = pt;
						break;
					}
				}
				
				if(data == null) 
				{
					player.sendMessage("This party room no longer exists.");
					return true;
				}
				
				data.banApplicant(name);
				openPartyWindow(player, data.partyId);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				player.sendMessage("Wrong parameters.");
			}
			
			return true;
		}
		
		return false;
	}
	
	public static void onDisconnect(PlayerEventInfo player)
	{
		for(PartyRecord data : parties)
		{
			if(data.leader.getPlayersId() == player.getPlayersId())
			{
				parties.remove(data);
				break;
			}
			
			for(PlayerEventInfo applicant : data.getApplicants())
			{
				if(applicant.getPlayersId() == player.getPlayersId())
				{
					data.getApplicants().remove(applicant);
					break;
				}
			}
		}
	}
}
