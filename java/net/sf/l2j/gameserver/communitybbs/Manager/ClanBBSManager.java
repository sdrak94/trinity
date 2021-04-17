package net.sf.l2j.gameserver.communitybbs.Manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.CharactersTable;
import net.sf.l2j.gameserver.datatables.CharactersTable.OfflinePlayerData;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Clan.SinglePetition;
import net.sf.l2j.gameserver.model.L2Clan.SubPledge;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.SubClass;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAdd;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * The Class ClanBBSManager.
 */
public class ClanBBSManager extends BaseBBSManager
{
	/** The Constant _log. */
	private static Logger _log = Logger.getLogger(ClanBBSManager.class.getName());
	
	/** The Constant CLANS_PER_PAGE. */
	private static final int CLANS_PER_PAGE = 6;
	
	/** The Constant MEMBERS_PER_PAGE. */
	private static final int MEMBERS_PER_PAGE = 6;
	
	/** The Constant PATH. */
	public static final String PATH = "data/html/CommunityBoard/clan";
	
	/** The all classes. */
	private final Map<Integer, String> ALL_CLASSES;
	
	/** The sorted clans. */
	public L2Clan[] SORTED_CLANS;
	
	/** The clan skill descriptions. */
	private static Map<Integer, String[]> _clanSkillDescriptions;
	
	@Override
	public void parsecmd(String command, L2PcInstance player)
	{
		StringTokenizer st = new StringTokenizer(command, "_");
		String cmd = st.nextToken();
		String html = null;
		
		if ("bbsclan".equals(cmd))
		{
			if (st.hasMoreTokens())
			{
				int clanId = Integer.parseInt(st.nextToken());
				if (clanId == 0)
				{
					player.sendPacket(SystemMessageId.NOT_JOINED_IN_ANY_CLAN);
					parsecmd("_bbsclanlist_0", player);
					return;
				}
				
				L2Clan clan = ClanTable.getInstance().getClan(clanId);
				if (clan == null)
				{
					parsecmd("_bbsclanlist_0", player);
					return;
				}
				
				html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_clan.htm");
				html = getMainClanPage(player, clan, html);
			}
			else
			{
				final L2Clan clan = player.getClan();
				if (clan != null)
				{
					parsecmd("_bbsclan_" + player.getClanId(), player);
					return;
				}
				
				player.sendMessage("You are not in a clan.");
				parsecmd("_bbsclanlist_0", player);
				return;
			}
		}
		else if ("bbsclanlist".equals(cmd))
		{
			int page = Integer.parseInt(st.nextToken());
			html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_clanlist.htm");
			html = html.replace("%rank%", getAllClansRank(player, page));
			html = html.replace("%myClan%", (player.getClan() != null ? "_bbsclan_" + player.getClanId() : "_bbsclanlist_0"));
		}
		else if ("bbsclanmanage".equals(cmd))// _bbsclanmanage_btn
		{
			String actionToken = st.nextToken();
			int action = Integer.parseInt(actionToken.substring(0, 1));
			
			if (action != 0)
			{
				boolean shouldReturn = manageRecrutationWindow(player, action, actionToken);
				if (shouldReturn)
				{
					return;
				}
			}
			html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_clanrecruit.htm");
			html = getClanRecruitmentManagePage(player, html);
		}
		else if ("bbsclanjoin".equals(cmd))
		{
			int clanId = Integer.parseInt(st.nextToken());
			
			L2Clan clan = ClanTable.getInstance().getClan(clanId);
			if (clan == null)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_bbsclanlist_0");
				return;
			}
			else if (!clan.isRecruiting())
			{
				sendErrorMessage(player, "Clan is not recruiting anymore.", "_bbsclanlist_0");
				return;
			}
			String next = st.nextToken();
			if (Integer.parseInt(next.substring(0, 1)) == 1)
			{
				try
				{
					if (!manageClanJoinWindow(player, clan, next.substring(2)))
					{
						sendInfoMessage(player, "You have already sent petition to this clan!", "_bbsclan_" + clan.getClanId(), true);
						return;
					}
				}
				catch (Exception e)
				{
					sendErrorMessage(player, "The petition you tried to send is incorrect!", "_bbsclanjoin_" + clan.getClanId() + "_0");
					return;
				}
				sendInfoMessage(player, "Your petition has been submitted!", "_bbsclan_" + clan.getClanId(), false);
				return;
			}
			html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_clanjoin.htm");
			html = getClanJoinPage(player, clan, html);
		}
		else if ("bbsclanpetitions".equals(cmd))
		{
			int clanId = Integer.parseInt(st.nextToken());
			
			L2Clan clan = ClanTable.getInstance().getClan(clanId);
			if (clan == null)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_bbsclanlist_0");
				return;
			}
			else if (!clan.isRecruiting())
			{
				sendErrorMessage(player, "Clan is not recruiting anymore.", "_bbsclanlist_0");
				return;
			}
			
			html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_clanpetitions.htm");
			html = getClanPetitionsPage(player, clan, html);
		}
		else if ("bbsclanplayerpetition".equals(cmd))
		{
			int senderId = Integer.parseInt(st.nextToken());
			if (st.hasMoreTokens())
			{
				int action = Integer.parseInt(st.nextToken());
				managePlayerPetition(player, senderId, action);
				return;
			}
			html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_clanplayerpetition.htm");
			
			L2PcInstance sender = L2World.getInstance().getPlayer(senderId);
			if (sender != null)
			{
				html = getClanSinglePetitionPage(player, sender, html);
			}
			else
			{
				html = getClanSinglePetitionPage(player, senderId, html);
			}
		}
		else if ("bbsclanplayerinventory".equals(cmd))
		{
			int senderId = Integer.parseInt(st.nextToken());
			L2PcInstance sender = L2World.getInstance().getPlayer(senderId);
			
			html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_clanplayerinventory.htm");
			
			if (sender != null)
			{
				html = getPlayerInventoryPage(sender, html);
			}
			else
			{
				html = getPlayerInventoryPage(senderId, html);
			}
		}
		else if ("bbsclanmembers".equals(cmd))
		{
			int clanId = Integer.parseInt(st.nextToken());
			if (clanId == 0)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_bbsclanlist_0");
				return;
			}
			
			int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
			
			L2Clan clan = ClanTable.getInstance().getClan(clanId);
			if (clan == null)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_bbsclanlist_0");
				return;
			}
			
			html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_clanmembers.htm");
			html = getClanMembersPage(player, clan, html, page);
		}
		else if ("bbsclansinglemember".equals(cmd))
		{
			int playerId = Integer.parseInt(st.nextToken());
			
			html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_clansinglemember.htm");
			
			L2PcInstance member = L2World.getInstance().getPlayer(playerId);
			if (member != null)
			{
				html = getClanSingleMemberPage(member, html);
			}
			else
			{
				html = getClanSingleMemberPage(playerId, html);
			}
		}
		else if ("bbsclanskills".equals(cmd))
		{
			int clanId = Integer.parseInt(st.nextToken());
			if (clanId == 0)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_bbsclanlist_0");
				return;
			}
			
			L2Clan clan = ClanTable.getInstance().getClan(clanId);
			if (clan == null)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_bbsclanlist_0");
				return;
			}
			
			html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_clanskills.htm");
			html = getClanSkills(clan, html);
		}
		else if ("bbsclanmailwritepledgeform".equals(cmd))
		{
			L2Clan clan = player.getClan();
			if ((clan == null) || (clan.getLevel() < 2) || !player.isClanLeader())
			{
				parsecmd("_bbsclan_" + player.getClanId(), player);
				return;
			}
			
			html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_pledge_mail_write.htm");
			
			html = html.replace("%PLEDGE_ID%", String.valueOf(clan.getClanId()));
			html = html.replace("%pledge_id%", String.valueOf(clan.getClanId()));
			html = html.replace("%pledge_name%", clan.getName());
		}
		else if ("bbsclanannouncepledgewriteform".equals(cmd))
		{
			L2Clan clan = player.getClan();
			if ((clan == null) || (clan.getLevel() < 2) || !player.isClanLeader())
			{
				player.sendMessage("Only clan leaders of clans level 2 or above can manage clan notice.");
				parsecmd("_bbsclan_" + player.getClanId(), player);
				return;
			}
			
			html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_clanannounce.htm");
			
			if (!clan.isNoticeEnabled())
			{
				html = html.replace("%off%", "Disabled");
				html = html.replace("%on%", "<a action=\"bypass _bbsclanannouncepledgeswitchshowflag_1\">Enable</a>");
			}
			else
			{
				html = html.replace("%off%", "<a action=\"bypass _bbsclanannouncepledgeswitchshowflag_0\">Disable</a>");
				html = html.replace("%on%", "Enabled");
			}
			html = html.replace("%flag%", String.valueOf(clan.isNoticeEnabled() ? 1 : 0));
			send1001(html, player);
			send1002(player, clan.getNotice().replaceAll("<br>", System.lineSeparator()).replaceAll("<br1>", System.lineSeparator()), "", "0");
			return;
		}
		else if ("bbsclanannouncepledgeswitchshowflag".equals(cmd))
		{
			L2Clan clan = player.getClan();
			if ((clan == null) || (clan.getLevel() < 2) || !player.isClanLeader())
			{
				parsecmd("_bbsclan_" + player.getClanId(), player);
				return;
			}
			
			int type = Integer.parseInt(st.nextToken());
			clan.setNoticeEnabled(type == 1);
			parsecmd("_bbsclanannouncepledgewriteform", player);
			return;
		}
		
		separateAndSend(html, player);
		return;
	}
	
	@Override
	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(bypass, "_");
		String cmd = st.nextToken();
		if ("bbsclanannouncepledgewrite".equals(cmd))
		{
			L2Clan clan = activeChar.getClan();
			if ((clan == null) || (clan.getLevel() < 2) || !activeChar.isClanLeader())
			{
				parsecmd("_bbsclan_" + activeChar.getObjectId(), activeChar);
				return;
			}
			
			if ((ar3 == null) || ar3.isEmpty())
			{
				parsecmd("_bbsclanannouncepledgewriteform", activeChar);
				return;
			}
			
			// arg3 = removeIllegalText(arg3);
			ar3 = ar3.replace("<", "");
			ar3 = ar3.replace(">", "");
			ar3 = ar3.replace("&", "");
			ar3 = ar3.replace("$", "");
			
			if (ar3.isEmpty())
			{
				parsecmd("_bbsclanannouncepledgewriteform", activeChar);
				return;
			}
			
			if (ar3.length() > 3000)
			{
				ar3 = ar3.substring(0, 3000);
			}
			
			int type = Integer.parseInt(st.nextToken());
			
			if (type == 1)
			{
				clan.setNotice(ar3.replace("\n", "<br1>"));
			}
			else
			{
				clan.setNotice(ar3.replace("\n", "<br1>"));
				clan.setNoticeEnabled(false);
			}
			
			activeChar.sendPacket(SystemMessageId.CLAN_NOTICE_SAVED);
			parsecmd("_bbsclanannouncepledgewriteform", activeChar);
		}
	}
	
	/**
	 * Gets the main clan page.
	 * @param player the player
	 * @param clan the clan
	 * @param html the html
	 * @return the main clan page
	 */
	private String getMainClanPage(L2PcInstance player, L2Clan clan, String html)
	{
		html = html.replace("%clanName%", clan.getName());
		html = html.replace("%clanId%", String.valueOf(clan.getClanId()));
		html = html.replace("%position%", "#" + clan.getRank());
		html = html.replace("%clanLeader%", clan.getLeaderName());
		html = html.replace("%allyName%", (clan.getAllyName() != null ? clan.getAllyName() : "None"));
		html = html.replace("%crp%", String.valueOf(clan.getReputationScore()));
		html = html.replace("%membersCount%", String.valueOf(clan.getMembersCount()));
		html = html.replace("%clanLevel%", String.valueOf(clan.getLevel()));
		html = html.replace("%raidsKilled%", String.valueOf(clan.getRBkills())); // should raids killed include epics? ahm yea why not
		html = html.replace("%epicsKilled%", String.valueOf(0)); // NOT DONE
		
		String castleName = "None";
		String fortName = "None";
		String clanhallName = "None";
		if (clan.getHasCastle() > 0)
		{
			castleName = CastleManager.getInstance().getCastleById(clan.getHasCastle()).getName();
		}
		if (clan.getHasFort() > 0)
		{
			fortName = FortManager.getInstance().getFortById(clan.getHasFort()).getName();
		}
		if (clan.getHasHideout() > 0)
		{
			clanhallName = ClanHallManager.getInstance().getClanHallById(clan.getHasHideout()).getName();
		}
		
		html = html.replace("%clanHall%", clanhallName);
		html = html.replace("%castle%", castleName);
		html = html.replace("%fortress%", fortName);
		
		int[] data = getMainClanPageData(clan);
		
		html = html.replace("%pvps%", String.valueOf(clan.getPvpKills()));
		html = html.replace("%pks%", String.valueOf(data[1]));
		html = html.replace("%nobleCount%", String.valueOf(data[2]));
		html = html.replace("%heroCount%", String.valueOf(data[3]));
		
		String alliances = "";
		if (clan.getAllyId() > 0)
		{
			alliances = "<table width=700><tr>";
			for (L2Clan memberClan : ClanTable.getInstance().getClans())
			{
				if (memberClan.getAllyId() == clan.getAllyId())
				{
					alliances += "<td align=center>";
					alliances += "<button action=\"bypass _bbsclan_" + memberClan.getClanId() + "\" value=\"" + memberClan.getName() + "\" width=150 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.button_df\">";
					alliances += "</td>";
				}
			}
			alliances += "</tr></table>";
		}
		html = html.replace("%alliances%", alliances);
		
		String wars = "<tr>";
		int index = 0;
		for (Integer warClanId : clan.getWarList())
		{
			if (index == 4)
			{
				wars += "</tr><tr>";
				index = 0;
			}
			wars += "<td align=center><button action=\"bypass _bbsclan_" + warClanId + "\" value=\"" + ClanTable.getInstance().getClan(warClanId).getName() + "\" width=100 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.button_df\"></td>";
			index++;
		}
		
		wars += "</tr>";
		
		html = html.replace("%wars%", wars);
		
		String joinClan = "";
		if (player.getClan() == null)
		{
			joinClan = "<tr><td width=200 align=\"center\">";
			joinClan += "<button action=\"bypass _bbsclanjoin_" + clan.getClanId() + "_0\" value=\"Join Clan\" width=200 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\">";
			joinClan += "</td></tr>";
		}
		html = html.replace("%joinClan%", joinClan);
		
		String manageRecruitment = "";
		String managePetitions = "";
		String manageNotice = "";
		if ((player.getClan() != null) && player.getClan().equals(clan) && (player.getClan().getLeaderId() == player.getObjectId()))
		{
			manageRecruitment = "<tr><td width=200 align=\"center\">";
			manageRecruitment += "<button action=\"bypass _bbsclanmanage_0\" value=\"Manage Recruitment\" width=200 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\">";
			manageRecruitment += "</td></tr>";
			
			managePetitions = "<tr><td width=200 align=\"center\">";
			managePetitions += "<button action=\"bypass _bbsclanpetitions_" + clan.getClanId() + "\" value=\"Manage Petitions\" width=200 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\">";
			managePetitions += "</td></tr>";
			
			manageNotice = "<tr><td width=200 align=\"center\">";
			manageNotice += "<button action=\"bypass _bbsclanannouncepledgewriteform\" value=\"Manage Notice\" width=200 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\">";
			manageNotice += "</td></tr>";
		}
		
		html = html.replace("%manageRecruitment%", manageRecruitment);
		html = html.replace("%managePetitions%", managePetitions);
		html = html.replace("%manageNotice%", manageNotice);
		
		return html;
	}
	
	/**
	 * Gets the clan members page.
	 * @param player the player
	 * @param clan the clan
	 * @param html the html
	 * @param page the page
	 * @return the clan members page
	 */
	private String getClanMembersPage(L2PcInstance player, L2Clan clan, String html, int page)
	{
		html = html.replace("%clanName%", clan.getName());
		html = html.replace("%clanId%", String.valueOf(clan.getClanId()));
		L2ClanMember[] members = clan.getMembers();
		
		StringBuilder builder = new StringBuilder();
		int index = 0;
		int max = Math.min(MEMBERS_PER_PAGE + (MEMBERS_PER_PAGE * page), members.length);
		for (int i = MEMBERS_PER_PAGE * page; i < max; i++)
		{
			L2ClanMember member = members[i];
			boolean isLeader = clan.getLeaderId() == member.getObjectId();
			if (!isLeader)
			{
				isLeader = Arrays.stream(clan.getAllSubPledges()).anyMatch(unit -> unit.getLeaderId() == member.getObjectId());
			}
			
			builder.append("<tr>");
			builder.append("<td width=50><font color=\"f1b45d\">").append(index + 1).append(".</font></td>");
			builder.append("<td align=center width=150>");
			builder.append("<button action=\"bypass _bbsclansinglemember_").append(member.getObjectId()).append("\" value=\"").append(member.getName()).append("\" width=150 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\">");
			builder.append("</td>");
			builder.append("<td width=100><center>").append(member.getPlayerInstance() != null ? "<font color=6a9b54>True</font>" : "<font color=9b5454>False</font>").append("</center></td>");
			builder.append("<td width=100><font color=\"bc7420\"><center>").append(isLeader ? "True" : "False").append("</center></font></td>");
			builder.append("<td width=75><font color=\"bc7420\"><center>").append(getUnitName(member.getPledgeType())).append("</center></font></td>");
			builder.append("<td width=75></td>");
			builder.append("</tr>");
			index++;
		}
		
		html = html.replace("%members%", builder.toString());
		
		// Restarting Builder
		builder = new StringBuilder();
		
		builder.append("<table width=750><tr><td align=center width=350>");
		
		if (page > 0)
		{
			builder.append("<button action=\"bypass _bbsclanmembers_").append(clan.getClanId()).append("_").append(page - 1).append("\" value=\"Previous\" width=140 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\">");
		}
		
		builder.append("</td><td align=center width=350>");
		
		if (members.length > (MEMBERS_PER_PAGE + (MEMBERS_PER_PAGE * page)))
		{
			builder.append("<center><button action=\"bypass _bbsclanmembers_").append(clan.getClanId() + "_" + (page + 1)).append("\" value=\"Next\" width=140 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\"></center>");
		}
		
		builder.append("</td></tr></table>");
		
		html = html.replace("%nextPages%", builder.toString());
		return html;
	}
	
	/**
	 * Gets the clan single member page.
	 * @param member the member
	 * @param html the html
	 * @return the clan single member page
	 */
	private String getClanSingleMemberPage(L2PcInstance member, String html)
	{
		L2Clan clan = member.getClan();
		boolean isLeader = (clan != null) && (clan.getLeaderId() == member.getObjectId());
		if (!isLeader && (clan != null))
		{
			isLeader = Arrays.stream(clan.getAllSubPledges()).anyMatch(unit -> unit.getLeaderId() == member.getObjectId());
		}
		html = html.replace("%playerName%", member.getName());
		html = html.replace("%playerId%", String.valueOf(member.getObjectId()));
		html = html.replace("%clanName%", member.getClan() != null ? member.getClan().getName() : "");
		html = html.replace("%online%", "<font color=6a9b54>True</font>");
		html = html.replace("%title%", member.getTitle());
		html = html.replace("%pvpPoints%", String.valueOf(member.getPvpKills()));
		html = html.replace("%pkPoints%", String.valueOf(member.getPkKills()));
		html = html.replace("%rank%", "Level " + (member.getClan() != null ? member.getClan().getClanMember(member.getObjectId()).getPowerGrade() : 0));
		html = html.replace("%onlineTime%", getConvertedTime(member.getOnlineTime()));
		html = html.replace("%leader%", isLeader ? "True" : "False");
		html = html.replace("%subpledge%", getUnitName(member.getPledgeType()));
		html = html.replace("%nobless%", member.isNoble() ? "True" : "False");
		html = html.replace("%hero%", member.isHero() ? "True" : "False");
		html = html.replace("%adena%", getConvertedAdena(member.getAdena()));
		html = html.replace("%recs%", String.valueOf(member.getRecomHave()));
		html = html.replace("%sevenSigns%", SevenSigns.getCabalShortName(SevenSigns.getInstance().getPlayerCabal(member)));
		html = html.replace("%fame%", String.valueOf(member.getFame()));
		
		SubClass firstSub = member.getSubClasses().getOrDefault(1, null);
		SubClass secondSub = member.getSubClasses().getOrDefault(2, null);
		SubClass thirdSub = member.getSubClasses().getOrDefault(3, null);
		html = html.replace("%mainClass%", CharTemplateTable.getInstance().getClassNameById(member.getClassId().getId()) + "(" + member.getLevel() + ")");
		html = html.replace("%firstSub%", firstSub == null ? "" : CharTemplateTable.getInstance().getClassNameById(firstSub.getClassId()) + "(" + firstSub.getLevel() + ")");
		html = html.replace("%secondSub%", secondSub == null ? "" : CharTemplateTable.getInstance().getClassNameById(secondSub.getClassId()) + "(" + secondSub.getLevel() + ")");
		html = html.replace("%thirdSub%", thirdSub == null ? "" : CharTemplateTable.getInstance().getClassNameById(thirdSub.getClassId()) + "(" + thirdSub.getLevel() + ")");
		
		html = html.replace("%clanId%", String.valueOf(member.getClanId()));
		
		return html;
	}
	
	/**
	 * Gets the clan single member page.
	 * @param playerId the player id
	 * @param html the html
	 * @return the clan single member page
	 */
	private String getClanSingleMemberPage(int playerId, String html)
	{
		OfflinePlayerData data = CharactersTable.getOfflinePlayerData(playerId);
		
		html = html.replace("%playerName%", data.char_name);
		html = html.replace("%playerId%", String.valueOf(playerId));
		html = html.replace("%clanName%", data.clanName);
		html = html.replace("%online%", "<font color=9b5454>False</font>");
		html = html.replace("%title%", data.title == null ? "" : data.title);
		html = html.replace("%pvpPoints%", "" + data.pvpKills);
		html = html.replace("%pkPoints%", "" + data.pkKills);
		html = html.replace("%onlineTime%", getConvertedTime(data.onlineTime));
		if (data.isClanLeader)
		{
			html = html.replace("%leader%", "Yes");
		}
		else
		{
			html = html.replace("%leader%", "No");
		}
		html = html.replace("%subpledge%", getUnitName(data.subpledge));
		if (data.isNoble)
		{
			html = html.replace("%nobless%", "Yes");
		}
		else
		{
			html = html.replace("%nobless%", "No");
		}
		if (data.isHero)
		{
			html = html.replace("%hero%", "Yes");
		}
		else
		{
			html = html.replace("%hero%", "No");
		}
		html = html.replace("%adena%", getConvertedAdena(data.adenaCount));
		html = html.replace("%sevenSigns%", SevenSigns.getCabalShortName(data.sevenSignsSide));
		html = html.replace("%recs%", String.valueOf(data.recomHave));
		html = html.replace("%fame%", String.valueOf(data.fame));
		html = html.replace("%clanId%", String.valueOf(data.clanId));
		
		SubClass firstSub = data.subClasses.stream().filter(sub -> sub.getClassIndex() == 1).findAny().orElse(null);
		SubClass secondSub = data.subClasses.stream().filter(sub -> sub.getClassIndex() == 2).findAny().orElse(null);
		SubClass thirdSub = data.subClasses.stream().filter(sub -> sub.getClassIndex() == 3).findAny().orElse(null);
		html = html.replace("%mainClass%", CharTemplateTable.getInstance().getClassNameById(data.baseClassId) + "(" + data.level + ")");
		html = html.replace("%firstSub%", firstSub == null ? "<br>" : CharTemplateTable.getInstance().getClassNameById(firstSub.getClassId()) + "(" + firstSub.getLevel() + ")");
		html = html.replace("%secondSub%", secondSub == null ? "<br>" : CharTemplateTable.getInstance().getClassNameById(secondSub.getClassId()) + "(" + secondSub.getLevel() + ")");
		html = html.replace("%thirdSub%", thirdSub == null ? "<br>" : CharTemplateTable.getInstance().getClassNameById(thirdSub.getClassId()) + "(" + thirdSub.getLevel() + ")");
		
		return html;
	}
	
	/**
	 * Gets the clan skills.
	 * @param clan the clan
	 * @param html the html
	 * @return the clan skills
	 */
	private String getClanSkills(L2Clan clan, String html)
	{
		html = html.replace("%clanName%", clan.getName());
		html = html.replace("%clanId%", String.valueOf(clan.getClanId()));
		
		String skills = "";
		for (L2Skill clanSkill : clan.getAllSkills())
		{
			skills += "<tr><td width=30></td>";
			skills += "<td width=55><br>";
			skills += "<img src=\"" + (clanSkill.getId() < 1000 ? ("icon.skill0" + clanSkill.getId()) : ("icon.skill" + clanSkill.getId())) + "\" height=30 width=30>"; // clanSkill.getIcon()
			skills += "</td><td width=660><br><table width=660><tr><td><font name=\"hs12\" color=\"99BBFF\">";
			skills += clanSkill.getName() + " Lv " + clanSkill.getLevel();
			skills += "</font></td></tr><tr><td>";
			String[] descriptions = _clanSkillDescriptions.get(clanSkill.getId());
			if ((descriptions == null) || (descriptions.length < (clanSkill.getLevel() - 1)))
			{
				_log.log(Level.WARNING, "cannot find skill id:" + clanSkill.getId() + " in Clan Community Skills descriptions!");
			}
			else
			{
				skills += "<font color=\"bc7420\">" + descriptions[clanSkill.getLevel() - 1] + "</font>";
			}
			skills += "</td></tr></table></td></tr>";
		}
		
		html = html.replace("%skills%", skills);
		
		return html;
	}
	
	/**
	 * Gets the clan single petition page.
	 * @param leader the leader
	 * @param member the member
	 * @param html the html
	 * @return the clan single petition page
	 */
	private String getClanSinglePetitionPage(L2PcInstance leader, L2PcInstance member, String html)
	{
		html = html.replace("%clanId%", String.valueOf(leader.getClan().getClanId()));
		html = html.replace("%playerId%", String.valueOf(member.getObjectId()));
		html = html.replace("%playerName%", member.getName());
		html = html.replace("%online%", "<font color=6a9b54>True</font>");
		html = html.replace("%onlineTime%", getConvertedTime(member.getOnlineTime()));
		html = html.replace("%pvpPoints%", String.valueOf(member.getPvpKills()));
		html = html.replace("%pkPoints%", String.valueOf(member.getPkKills()));
		html = html.replace("%fame%", String.valueOf(member.getFame()));
		html = html.replace("%recs%", String.valueOf(member.getRecomHave()));
		html = html.replace("%adena%", getConvertedAdena(member.getAdena()));
		
		html = html.replace("%mainClass%", CharTemplateTable.getInstance().getClassNameById(member.getBaseClass()) + "(Level: " + member.getStat().getBaseClassLevel() + ")");
		
		SubClass firstSub = member.getSubClasses().getOrDefault(1, null);
		SubClass secondSub = member.getSubClasses().getOrDefault(2, null);
		SubClass thirdSub = member.getSubClasses().getOrDefault(3, null);
		html = html.replace("%mainClass%", CharTemplateTable.getInstance().getClassNameById(member.getClassId().getId()) + "(" + member.getLevel() + ")");
		html = html.replace("%firstSub%", firstSub == null ? "" : CharTemplateTable.getInstance().getClassNameById(firstSub.getClassId()) + "(" + firstSub.getLevel() + ")");
		html = html.replace("%secondSub%", secondSub == null ? "" : CharTemplateTable.getInstance().getClassNameById(secondSub.getClassId()) + "(" + secondSub.getLevel() + ")");
		html = html.replace("%thirdSub%", thirdSub == null ? "" : CharTemplateTable.getInstance().getClassNameById(thirdSub.getClassId()) + "(" + thirdSub.getLevel() + ")");
		
		int index = 1;
		for (String question : leader.getClan().getQuestions())
		{
			html = html.replace("%question" + index + "%", (question != null) && (question.length() > 2) ? question + "?" : "");
			index++;
		}
		
		SinglePetition petition = leader.getClan().getPetition(member.getObjectId());
		index = 1;
		for (String answer : petition.getAnswers())
		{
			html = html.replace("%answer" + index + "%", (answer != null) && (answer.length() > 2) ? answer : "");
			index++;
		}
		
		html = html.replace("%comment%", petition.getComment());
		
		return html;
	}
	
	/**
	 * Gets the clan single petition page.
	 * @param leader the leader
	 * @param playerId the player id
	 * @param html the html
	 * @return the clan single petition page
	 */
	private String getClanSinglePetitionPage(L2PcInstance leader, int playerId, String html)
	{
		OfflinePlayerData data = CharactersTable.getOfflinePlayerData(playerId);
		
		html = html.replace("%clanId%", String.valueOf(leader.getClanId()));
		html = html.replace("%playerId%", String.valueOf(playerId));
		html = html.replace("%online%", "<font color=9b5454>No</font>");
		html = html.replace("%playerName%", data.char_name);
		html = html.replace("%onlineTime%", getConvertedTime(data.onlineTime));
		html = html.replace("%pvpPoints%", String.valueOf(data.pvpKills));
		html = html.replace("%pkPoints%", String.valueOf(data.pkKills));
		html = html.replace("%recs%", String.valueOf(data.recomHave));
		html = html.replace("%fame%", String.valueOf(data.fame));
		html = html.replace("%adena%", getConvertedAdena(data.adenaCount));
		
		SubClass firstSub = data.subClasses.stream().filter(sub -> sub.getClassIndex() == 1).findAny().orElse(null);
		SubClass secondSub = data.subClasses.stream().filter(sub -> sub.getClassIndex() == 2).findAny().orElse(null);
		SubClass thirdSub = data.subClasses.stream().filter(sub -> sub.getClassIndex() == 3).findAny().orElse(null);
		html = html.replace("%mainClass%", CharTemplateTable.getInstance().getClassNameById(data.baseClassId) + "(" + data.level + ")");
		html = html.replace("%firstSub%", firstSub == null ? "<br>" : CharTemplateTable.getInstance().getClassNameById(firstSub.getClassId()) + "(" + firstSub.getLevel() + ")");
		html = html.replace("%secondSub%", secondSub == null ? "<br>" : CharTemplateTable.getInstance().getClassNameById(secondSub.getClassId()) + "(" + secondSub.getLevel() + ")");
		html = html.replace("%thirdSub%", thirdSub == null ? "<br>" : CharTemplateTable.getInstance().getClassNameById(thirdSub.getClassId()) + "(" + thirdSub.getLevel() + ")");
		
		int index = 1;
		for (String question : leader.getClan().getQuestions())
		{
			html = html.replace("%question" + index + "%", (question != null) && (question.length() > 2) ? question : "");
			index++;
		}
		
		SinglePetition petition = leader.getClan().getPetition(playerId);
		index = 1;
		for (String answer : petition.getAnswers())
		{
			html = html.replace("%answer" + index + "%", (answer != null) && (answer.length() > 2) ? answer : "");
			index++;
		}
		
		html = html.replace("%comment%", petition.getComment());
		
		return html;
	}
	
	/**
	 * Gets the clan recruitment manage page.
	 * @param player the player
	 * @param html the html
	 * @return the clan recruitment manage page
	 */
	private String getClanRecruitmentManagePage(L2PcInstance player, String html)
	{
		L2Clan clan = player.getClan();
		if (clan == null)
		{
			return html;
		}
		
		html = html.replace("%clanName%", clan.getName());
		boolean firstChecked = clan.getClassesNeeded().size() == ALL_CLASSES.size();
		html = html.replace("%checked1%", firstChecked ? "_checked" : "");
		html = html.replace("%checked2%", firstChecked ? "" : "_checked");
		
		String[] notChoosenClasses = getNotChosenClasses(clan);
		html = html.replace("%firstClassGroup%", notChoosenClasses[0]);
		html = html.replace("%secondClassGroup%", notChoosenClasses[1]);
		
		String list = "<tr>";
		int index = -1;
		for (Integer clas : clan.getClassesNeeded())
		{
			if ((index % 4) == 3)
			{
				list += "</tr><tr>";
			}
			index++;
			
			list += "<td><button value=\"" + ALL_CLASSES.get(clas) + "\" action=\"bypass  _bbsclanmanage_5 " + ALL_CLASSES.get(clas) + "\" back=\"l2ui_ct1.button.button_df_small_down\" width=105 height=20 fore=\"l2ui_ct1.button.button_df_small\"></td>";
		}
		list += "</tr>";
		
		html = html.replace("%choosenClasses%", list);
		
		for (int i = 0; i < 8; i++)
		{
			String clanQuestion = clan.getQuestions()[i];
			html = html.replace("%question" + (i + 1) + "%", (clanQuestion != null) && (clanQuestion.length() > 0) ? clanQuestion : "Question " + (i + 1) + ":");
		}
		
		html = html.replace("%recrutation%", clan.isRecruiting() ? "Stop" : "Start");
		return html;
	}
	
	/**
	 * Gets the clan join page.
	 * @param player the player
	 * @param clan the clan
	 * @param html the html
	 * @return the clan join page
	 */
	private String getClanJoinPage(L2PcInstance player, L2Clan clan, String html)
	{
		List<Integer> playerClasses = player.getSubClasses().values().stream().map(SubClass::getClassId).collect(Collectors.toList());
		playerClasses.add(player.getBaseClass());
		
		html = html.replace("%clanId%", String.valueOf(clan.getClanId()));
		html = html.replace("%clanName%", clan.getName());
		for (int i = 0; i < 8; i++)
		{
			String question = clan.getQuestions()[i];
			if ((question != null) && (question.length() > 2))
			{
				html = html.replace("%question" + (i + 1) + "%", question);
				html = html.replace("%answer" + (i + 1) + "%", "<edit var=\"answer" + (i + 1) + "\" width=275 height=15>");
			}
			else
			{
				html = html.replace("%question" + (i + 1) + "%", "");
				html = html.replace("%answer" + (i + 1) + "%", "");
				html = html.replace("$answer" + (i + 1), " ");
			}
		}
		
		boolean canJoin = false;
		
		String classes = "<tr>";
		int index = -1;
		for (int classNeeded : clan.getClassesNeeded())
		{
			index++;
			if (index == 6)
			{
				classes += "</tr><tr>";
				index = 0;
			}
			boolean goodClass = playerClasses.contains(classNeeded);
			
			if (goodClass)
			{
				canJoin = true;
			}
			
			classes += "<td width=130><font color=\"" + (goodClass ? "6a9b54" : "9b5454") + "\">";
			classes += CharTemplateTable.getInstance().getClassNameById(classNeeded);
			classes += "</font></td>";
		}
		classes += "</tr>";
		
		html = html.replace("%classes%", classes);
		
		if (canJoin)
		{
			html = html.replace("%joinClanButton%", "<center><button action=\"bypass _bbsclanjoin_" + clan.getClanId() + "_1 | $answer1 | $answer2 | $answer3 | $answer4 | $answer5 | $answer6 | $answer7 | $answer8 | $comment |\" value=\"Send\" width=320 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\"></center>");
		}
		else
		{
			html = html.replace("%joinClanButton%", "Your class is not needed in this clan, therefore you may not join.");
		}
		
		return html;
	}
	
	/**
	 * Gets the clan petitions page.
	 * @param player the player
	 * @param clan the clan
	 * @param html the html
	 * @return the clan petitions page
	 */
	private String getClanPetitionsPage(L2PcInstance player, L2Clan clan, String html)
	{
		html = html.replace("%clanName%", clan.getName());
		
		String petitions = "";
		int index = 1;
		List<SinglePetition> _petitionsToRemove = new ArrayList<>();
		
		for (SinglePetition petition : clan.getPetitions())
		{
			ClanPetitionData data = getClanPetitionsData(petition.getSenderId());
			if (data == null)
			{
				_petitionsToRemove.add(petition);
				continue;
			}
			petitions += "<tr><td width=30><font name=\"hs12\" color=\"f1b45d\">";
			petitions += index;
			petitions += ".</font></font></td><td align=center width=150>";
			petitions += "<button action=\"bypass _bbsclanplayerpetition_" + petition.getSenderId() + "\" value=\"" + data.char_name + "\" width=150 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\">";
			petitions += "</td><td width=100><center>";
			petitions += data.online;
			petitions += "</td><td width=95><font color=\"f1b45d\"><center>";
			petitions += data.pvpKills;
			petitions += "</center></font></td><td width=100><font color=\"f1b45d\"><center>";
			petitions += getConvertedTime(data.onlineTime);
			petitions += "</center></font></td><td width=75><font color=\"f1b45d\"><center>";
			petitions += Boolean.toString(data.isNoble);
			petitions += "</center></font></td></tr>";
			index++;
		}
		
		for (SinglePetition petitionToRemove : _petitionsToRemove)
		{
			clan.deletePetition(petitionToRemove);
		}
		
		html = html.replace("%petitions%", petitions);
		
		return html;
	}
	
	/**
	 * Gets the player inventory page.
	 * @param player the player
	 * @param html the html
	 * @return the player inventory page
	 */
	private String getPlayerInventoryPage(L2PcInstance player, String html)
	{
		html = html.replace("%playerName%", player.getName());
		html = html.replace("%back%", (player.getClan() != null ? "_bbsclansinglemember_" + player.getObjectId() : "_bbsclanplayerpetition_" + player.getObjectId()));
		
		PcInventory pcInv = player.getInventory();
		String inventory = "<tr>";
		for (int i = 0; i < SLOTS.length; i++)
		{
			if ((i % 2) == 0)
			{
				inventory += "</tr><tr>";
			}
			inventory += "<td><table><tr><td height=40>";
			inventory += pcInv.getPaperdollItem(SLOTS[i]) != null ? "<img src=" + pcInv.getPaperdollItem(SLOTS[i]).getItem().getIcon() + " width=32 height=32>" : "<img src=\"Icon.low_tab\" width=32 height=32>";
			inventory += "</td><td width=150><font color=\"bc7420\">";
			inventory += pcInv.getPaperdollItem(SLOTS[i]) != null ? pcInv.getPaperdollItem(SLOTS[i]).getItem().getName() + " +" + pcInv.getPaperdollItem(SLOTS[i]).getEnchantLevel() : "No " + NAMES[i];
			inventory += "</font></td></tr></table></td>";
		}
		inventory += "</tr>";
		
		html = html.replace("%inventory%", inventory);
		
		return html;
	}
	
	/**
	 * Gets the player inventory page.
	 * @param playerId the player id
	 * @param html the html
	 * @return the player inventory page
	 */
	private String getPlayerInventoryPage(int playerId, String html)
	{
		OfflinePlayerInventoryData data = getPlayerInventoryData(playerId);
		html = html.replace("%playerName%", data.char_name);
		html = html.replace("%back%", (data.clanId != 0 ? "_bbsclansinglemember_" + playerId : "_bbsclanplayerpetition_" + playerId));
		
		String inventory = "<tr>";
		for (int i = 0; i < SLOTS.length; i++)
		{
			if ((i % 2) == 0)
			{
				inventory += "</tr><tr>";
			}
			int[] item = data.itemIdAndEnchantForSlot.get(i);
			L2Item template = null;
			if ((item != null) && (item[0] > 0))
			{
				template = ItemTable.getInstance().getTemplate(item[0]);
			}
			inventory += "<td><table><tr><td height=40>";
			inventory += template != null ? "<img src=" + template.getIcon() + " width=32 height=32>" : "<img src=\"Icon.low_tab\" width=32 height=32>";
			inventory += "</td><td width=150><font color=\"bc7420\">";
			inventory += (template != null) && (item != null) ? template.getName() + " +" + item[1] : "No " + NAMES[i];
			inventory += "</font></td></tr></table></td>";
		}
		inventory += "</tr>";
		
		html = html.replace("%inventory%", inventory);
		
		return html;
	}
	
	/**
	 * The Class OfflinePlayerInventoryData.
	 */
	class OfflinePlayerInventoryData
	{
		
		/** The char name. */
		String char_name;
		
		/** The clan id. */
		int clanId;
		
		/** The item id and enchant for slot. */
		Map<Integer, int[]> itemIdAndEnchantForSlot = new FastMap<>();
	}
	
	/**
	 * Gets the player inventory data.
	 * @param playerId the player id
	 * @return the player inventory data
	 */
	private OfflinePlayerInventoryData getPlayerInventoryData(int playerId)
	{
		OfflinePlayerInventoryData data = new OfflinePlayerInventoryData();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement st = con.prepareStatement("SELECT char_name, clanid FROM characters WHERE charId = ?"))
			{
				st.setInt(1, playerId);
				try (ResultSet rset = st.executeQuery())
				{
					if (rset.next())
					{
						data.char_name = rset.getString("char_name");
						data.clanId = rset.getInt("clanid");
					}
				}
			}
			
			try (PreparedStatement st = con.prepareStatement("SELECT item_id, loc_data, enchant_level FROM items WHERE owner_id = ? AND loc='PAPERDOLL'"))
			{
				st.setInt(1, playerId);
				try (ResultSet rset = st.executeQuery())
				{
					while (rset.next())
					{
						int loc = rset.getInt("loc_data");
						for (int i = 0; i < SLOTS.length; i++)
						{
							if (loc == SLOTS[i])
							{
								int[] itemData =
									{
										rset.getInt("item_id"),
										rset.getInt("enchant_level")
									};
								data.itemIdAndEnchantForSlot.put(i, itemData);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error in getPlayerInventoryData:", e);
		}
		
		return data;
	}
	
	/**
	 * Gets the main clan page data.
	 * @param clan the clan
	 * @return the main clan page data
	 */
	private int[] getMainClanPageData(L2Clan clan)
	{
		int[] data = new int[4];
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();)
		{
			try (PreparedStatement statement = con.prepareStatement("SELECT SUM(pvpkills), SUM(pkkills), SUM(nobless) FROM characters WHERE clanid = ?"))
			{
				statement.setInt(1, clan.getClanId());
				try (ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						data[0] = rset.getInt(1);
						data[1] = rset.getInt(2);
						data[2] = rset.getInt(3);
					}
				}
			}
			
			try (PreparedStatement statement = con.prepareStatement("SELECT count(`characters`.`charId`) FROM `characters` join `heroes` on `characters`.`charId` = `heroes`.`charId` where `characters`.`clanid` = ?"))
			{
				statement.setInt(1, clan.getClanId());
				try (ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						data[3] = rset.getInt(1);
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error in getMainClanPageData:", e);
		}
		return data;
	}
	
	/**
	 * The Class ClanPetitionData.
	 */
	class ClanPetitionData
	{
		
		/** The char name. */
		String char_name;
		
		/** The online. */
		String online;
		
		/** The pvp kills. */
		int pvpKills;
		
		/** The online time. */
		long onlineTime;
		
		/** The is noble. */
		boolean isNoble;
	}
	
	/**
	 * Gets the clan petitions data.
	 * @param senderId the sender id
	 * @return the clan petitions data
	 */
	private ClanPetitionData getClanPetitionsData(int senderId)
	{
		ClanPetitionData data = new ClanPetitionData();
		L2PcInstance sender = L2World.getInstance().getPlayer(senderId);
		boolean haveclan = false;
		if (sender != null)
		{
			data.char_name = sender.getName();
			data.online = "<font color=6a9b54>True</font>";
			data.pvpKills = sender.getPvpKills();
			data.onlineTime = sender.getOnlineTime();
			data.isNoble = sender.isNoble();
		}
		else
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();)
			{
				try (PreparedStatement statement = con.prepareStatement("SELECT char_name,pvpkills,onlinetime,clanid,nobless FROM characters WHERE charId=?"))
				{
					statement.setInt(1, senderId);
					try (ResultSet rset = statement.executeQuery())
					{
						if (rset.next())
						{
							data.char_name = rset.getString("char_name");
							data.online = "<font color=9b5454>False</font>";
							data.pvpKills = rset.getInt("pvpkills");
							data.onlineTime = rset.getLong("onlinetime");
							data.isNoble = rset.getInt("nobless") == 1;
							if (rset.getInt("clanid") > 0)
							{
								haveclan = true;
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error in getClanPetitionsData:", e);
			}
		}
		
		if (haveclan)
		{
			return null;
		}
		return data;
	}
	
	/**
	 * Gets the converted time.
	 * @param seconds the seconds
	 * @return the converted time
	 */
	private String getConvertedTime(long seconds)
	{
		int days = (int) (seconds / 86400);
		seconds -= days * 86400;
		int hours = (int) (seconds / 3600);
		seconds -= hours * 3600;
		int minutes = (int) (seconds / 60);
		
		boolean includeNext = true;
		String time = "";
		if (days > 0)
		{
			time = days + " Days ";
			if (days > 5)
			{
				includeNext = false;
			}
		}
		if ((hours > 0) && includeNext)
		{
			if (time.length() > 0)
			{
				includeNext = false;
			}
			time += hours + " Hours ";
			if (hours > 10)
			{
				includeNext = false;
			}
		}
		if ((minutes > 0) && includeNext)
		{
			time += minutes + " Mins";
		}
		return time;
	}
	
	/**
	 * Gets the converted adena.
	 * @param adena the adena
	 * @return the converted adena
	 */
	private String getConvertedAdena(long adena)
	{
		String text = "";
		String convertedAdena = String.valueOf(adena);
		int ks = (convertedAdena.length() - 1) / 3;
		long firstValue = adena / (long) (Math.pow(1000, ks));
		text = firstValue + getKs(ks);
		if (((convertedAdena.length() - 2) / 3) < ks)
		{
			adena -= firstValue * (long) (Math.pow(1000, ks));
			if ((adena / (long) (Math.pow(1000, (ks - 1)))) > 0)
			{
				text += " " + (adena / (int) (Math.pow(1000, (ks - 1)))) + getKs(ks - 1);
			}
		}
		return text;
	}
	
	/**
	 * Gets the ks.
	 * @param howMany the how many
	 * @return the ks
	 */
	private String getKs(int howMany)
	{
		String x = "";
		for (int i = 0; i < howMany; i++)
		{
			x += "k";
		}
		return x;
	}
	
	/**
	 * Gets the unit name.
	 * @param type the type
	 * @return the unit name
	 */
	public String getUnitName(int type)
	{
		String subUnitName = "";
		switch (type)
		{
			case L2Clan.SUBUNIT_ACADEMY:
				subUnitName = "Academy";
				break;
			case L2Clan.SUBUNIT_ROYAL1:
			case L2Clan.SUBUNIT_ROYAL2:
				subUnitName = "Royal Guard";
				break;
			case L2Clan.SUBUNIT_KNIGHT1:
			case L2Clan.SUBUNIT_KNIGHT2:
			case L2Clan.SUBUNIT_KNIGHT3:
			case L2Clan.SUBUNIT_KNIGHT4:
				subUnitName = "Order of Knight";
				break;
			default:
				subUnitName = "Main Clan";
		}
		return subUnitName;
	}
	
	/**
	 * Send error message.
	 * @param player the player
	 * @param message the message
	 * @param backPage the back page
	 */
	private void sendErrorMessage(L2PcInstance player, String message, String backPage)
	{
		sendInfoMessage(player, message, backPage, true);
	}
	
	/**
	 * Send info message.
	 * @param player the player
	 * @param message the message
	 * @param backPage the back page
	 * @param error the error
	 */
	private void sendInfoMessage(L2PcInstance player, String message, String backPage, boolean error)
	{
		String html = "<html><head><title>Clan Recruitment</title></head><body>";
		html += "<table border=0 cellpadding=0 cellspacing=0 width=700><tr><td><br><br>";
		html += "<center><font color = \"" + (error ? "9b5454" : "6a9b54") + "\">";
		html += message;
		html += "</font><br><br><br>";
		html += "<button action=\"bypass " + backPage + "\" value=\"Back\" width=130 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\">";
		html += "</center></td></tr></table></body></html>";
		
		separateAndSend(html, player);
	}
	
	/**
	 * Gets the main stats table color.
	 * @param index the index
	 * @return the main stats table color
	 */
	private String getMainStatsTableColor(int index)
	{
		return (index % 2) == 0 ? "222320" : "191919";
	}
	
	/**
	 * Gets the all clans rank.
	 * @param player the player
	 * @param page the page
	 * @return the all clans rank
	 */
	private String getAllClansRank(L2PcInstance player, int page)
	{
		// Making first row
		String text = "<table width=760><tr><td height=30>";
		text += "<table width=760 bgcolor=" + getMainStatsTableColor(0) + " height=30>";
		text += "<tr><td width=45>";
		text += "<font name=\"hs12\" color=\"99BBFF\">Rank</font>";
		text += "</td><td width=220>";
		text += "<center><font name=\"hs12\" color=\"99BBFF\">Clan</font></center>";
		text += "</td><td width=100>";
		text += "<center><font name=\"hs12\" color=\"99BBFF\">Leader</font></center>";
		text += "</td><td width=100>";
		text += "<center><font name=\"hs12\" color=\"99BBFF\">Alliance</font></center>";
		text += "</td><td width=80>";
		text += "<center><font name=\"hs12\" color=\"99BBFF\">Level</font></center>";
		text += "</td><td width=220>";
		text += "<center><font name=\"hs12\" color=\"99BBFF\">Recruitment</font></center>";
		text += "</td></tr></table></td></tr>";
		
		int max = Math.min(CLANS_PER_PAGE + (CLANS_PER_PAGE * page), SORTED_CLANS.length);
		int index = 0;
		for (int i = CLANS_PER_PAGE * page; i < max; i++)
		{
			L2Clan clan = SORTED_CLANS[i];
			if (clan.getName().equalsIgnoreCase("GameMasters"))
			{
				continue;
			}
			text += "<tr><td height=30>";
			text += "<table width=760 bgcolor=\"" + getMainStatsTableColor(index + 1) + "\" height=30>";
			text += "<tr><td width=40>";
			text += "<font name=\"hs12\" color=bc7420>#" + (i + 1) + "</font></center>";
			text += "</td><td width=220>";
			text += "<center><button action=\"bypass _bbsclan_" + clan.getClanId() + "\" value=\"" + clan.getName() + "\" width=90 height=21 back=\"L2Crabbed_icon.bub.1\" fore=\"L2Crabbed_icon.bub.1\"></center>";
			text += "</td><td width=100>";
			text += "<center><font color=\"f1b45d\">" + clan.getLeaderName() + "</font></center>";
			text += "</td><td width=100>";
			text += "<center><font color=\"f1b45d\">" + (clan.getAllyName() != null ? clan.getAllyName() : "") + "</font>";
			text += "</td><td width=80>";
			text += "<center><font color=\"f1b45d\">" + clan.getLevel() + "</font></center>";
			text += "</td><td width=220>";
			if (!clan.isRecruiting() || clan.isFull())
			{
				text += "<center><button action=\"bypass _bbsclanlist_" + page + "\" value=\"Not Recruiting\" width=90 height=21 back=\"L2Crabbed_icon.bub.1\" fore=\"L2Crabbed_icon.bub.1\"></center>";
			}
			else if (player.getClan() != null)
			{
				text += "<center><button action=\"bypass _bbsclanlist_" + page + "\" value=\"Recrutation Up\" width=90 height=21 back=\"L2Crabbed_icon.bub.1\" fore=\"L2Crabbed_icon.bub.1\"></center>";
			}
			else
			{
				text += "<center><button action=\"bypass _bbsclanjoin_" + clan.getClanId() + "_0\" value=\"Join Clan\" width=90 height=21 back=\"L2Crabbed_icon.bub.1\" fore=\"L2Crabbed_icon.bub.1\"></center>";
			}
			
			text += "</td></tr></table>";
			text += "</td></tr>";
			index++;
		}
		text += "</table>";
		text += "<table width=700><tr><td width=350>";
		if (page > 0)
		{
			text += "<center><button action=\"bypass _bbsclanlist_" + (page - 1) + "\" value=\"Previous\" width=140 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\"></center>";
		}
		text += "</td><td width=350>";
		if (SORTED_CLANS.length > (CLANS_PER_PAGE + (CLANS_PER_PAGE * page)))
		{
			text += "<center><button action=\"bypass _bbsclanlist_" + (page + 1) + "\" value=\"Next\" width=140 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\"></center>";
		}
		text += "</td></tr></table>";
		
		return text;
	}
	
	/**
	 * Manage recrutation window.
	 * @param player the player
	 * @param actionToken the action token
	 * @param wholeText the whole text
	 * @return true, if successful
	 */
	private boolean manageRecrutationWindow(L2PcInstance player, int actionToken, String wholeText)
	{
		L2Clan clan = player.getClan();
		
		boolean failedAction = false;
		switch (actionToken)
		{
			case 1:
				clan.getClassesNeeded().clear();
				ALL_CLASSES.keySet().forEach(i -> clan.addClassNeeded(i));
				break;
			case 2:
				clan.getClassesNeeded().clear();
				break;
			case 3:
				if (wholeText.length() > 2)
				{
					String clazz = wholeText.substring(2);
					for (Map.Entry<Integer, String> entry : ALL_CLASSES.entrySet())
					{
						if (entry.getValue().equals(clazz))
						{
							clan.addClassNeeded(entry.getKey());
							break;
						}
					}
				}
				break;
			case 5:
				String clazz = wholeText.substring(2);
				for (Map.Entry<Integer, String> entry : ALL_CLASSES.entrySet())
				{
					if (entry.getValue().equals(clazz))
					{
						clan.deleteClassNeeded(entry.getKey());
						break;
					}
				}
				break;
			case 6:
				String[] questions = clan.getQuestions();
				StringTokenizer st = new StringTokenizer(wholeText.substring(2), "|");
				for (int i = 0; i < 8; i++)
				{
					String question = st.nextToken();
					if (question.length() > 3)
					{
						questions[i] = question;
					}
					clan.setQuestions(questions);
				}
				break;
			case 7:
				clan.setRecruiting(!clan.isRecruiting());
				break;
			default:
				failedAction = true;
		}
		
		if (!failedAction)
		{
			clan.updateRecrutationData();
		}
		
		return false;
	}
	
	/**
	 * Manage clan join window.
	 * @param player the player
	 * @param clan the clan
	 * @param text the text
	 * @return true, if successful
	 */
	private boolean manageClanJoinWindow(L2PcInstance player, L2Clan clan, String text)
	{
		StringTokenizer st = new StringTokenizer(text, "|");
		String[] answers = new String[8];
		for (int i = 0; i < 8; i++)
		{
			String answer = st.nextToken();
			answers[i] = answer;
		}
		String comment = st.nextToken();
		return clan.addPetition(player.getObjectId(), answers, comment);
	}
	
	/**
	 * Manage player petition.
	 * @param player the player
	 * @param senderId the sender id
	 * @param action the action
	 */
	private void managePlayerPetition(L2PcInstance player, int senderId, int action)
	{
		L2PcInstance sender = L2World.getInstance().getPlayer(senderId);
		L2Clan clan = player.getClan();
		switch (action)
		{
			case 1:
				int type = -1;
				
				if (!clan.isFull(L2Clan.SUBUNIT_MAIN_CLAN))
				{
					type = 0;
				}
				else
				{
					for (SubPledge sp : clan.getAllSubPledges())
					{
						if (!clan.isFull(sp.getId()))
						{
							type = sp.getId();
							break;
						}
					}
				}
				
				if (type == -1)
				{
					sendErrorMessage(player, "Clan is full!", "_bbsclanplayerpetition_" + senderId);
					return;
				}
				
				if (sender != null)
				{
					if (sender.getClanJoinExpiryTime() > System.currentTimeMillis())
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN);
						sm.addString(sender.getName());
						player.sendPacket(sm);
						return;
					}
					// add clan member
					ClanTable.getInstance().addClanMember(player.getClan(), sender, type);
				}
				else
				{
					if (CharactersTable.getOfflinePlayerData(senderId).clanJoinPenaltyExpiryTime > System.currentTimeMillis())
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.C1_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN);
						sm.addString(CharactersTable.getOfflinePlayerData(senderId).char_name);
						player.sendPacket(sm);
						return;
					}
					try (Connection con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement st = con.prepareStatement("UPDATE characters SET clanid = ?, subpledge = ? WHERE charId = ?");)
					{
						st.setInt(1, clan.getClanId());
						st.setInt(2, type);
						st.setInt(3, senderId);
						if (st.executeUpdate() > 0)
						{
							final L2ClanMember member = createClanMember(clan, senderId);
							if (member != null)
							{
								clan.addClanMember(member);
								clan.broadcastToOnlineMembers(new PledgeShowMemberListAdd(member));
								clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
							}
						}
					}
					catch (Exception e)
					{
						_log.log(Level.SEVERE, "Error in managePlayerPetition:", e);
					}
				}
				sendInfoMessage(player, "Member has been added!", "_bbsclanpetitions_" + clan.getClanId(), false);
			case 2:
				clan.deletePetition(senderId);
				if (action == 2)
				{
					sendInfoMessage(player, "Petition has been deleted!", "_bbsclanpetitions_" + clan.getClanId(), false);
				}
				break;
		}
	}
	
	/**
	 * Creates the clan member.
	 * @param clan the clan
	 * @param objectId the object id
	 * @return the l 2 clan member
	 */
	private L2ClanMember createClanMember(L2Clan clan, int objectId)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement select = con.prepareStatement("SELECT char_name,level,classid,charId,title,power_grade,subpledge,apprentice,sponsor,sex,race FROM characters WHERE clanid = ? AND charId = ?"))
		{
			select.setInt(1, clan.getClanId());
			select.setInt(2, objectId);
			try (ResultSet clanMember = select.executeQuery())
			{
				if (clanMember.next())
				{
					return new L2ClanMember(clan, clanMember);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, e.getMessage(), e);
		}
		return null;
	}
	
	/**
	 * Gets the not chosen classes.
	 * @param clan the clan
	 * @return the not chosen classes
	 */
	private String[] getNotChosenClasses(L2Clan clan)
	{
		String[] splited = new String[2];
		
		List<String> list = ALL_CLASSES.entrySet().stream().filter(e -> !clan.getClassesNeeded().contains(e.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());
		splited[0] = list.stream().limit(list.size() / 2).collect(Collectors.joining(";"));
		splited[1] = list.stream().skip(list.size() / 2).collect(Collectors.joining(";"));
		return splited;
	}
	
	/** The Constant COMMANDS. */
	public static final String[] COMMANDS = new String[]
		{
			"_bbsclan",
			"_bbsclanlist_",
			"_bbsclanmanage",
			"_bbsclanjoin",
			"_bbsclanpetitions",
			"_bbsclanplayerpetition",
			// "_bbsclanplayerinventory", OPTION DISABLED
			"_bbsclanmembers",
			"_bbsclansinglemember",
			"_bbsclanskills",
			"_bbsclanmailwritepledgeform",
			"_bbsclanannouncepledgewriteform",
			"_bbsclanannouncepledgeswitchshowflag",
			"_bbsclanannouncepledgewrite"
		};
	
	/** The Constant SLOTS. */
	private static final int[] SLOTS =
		{
			Inventory.PAPERDOLL_RHAND,
			Inventory.PAPERDOLL_LHAND,
			Inventory.PAPERDOLL_HEAD,
			Inventory.PAPERDOLL_CHEST,
			Inventory.PAPERDOLL_LEGS,
			Inventory.PAPERDOLL_ALLDRESS,
			Inventory.PAPERDOLL_GLOVES,
			Inventory.PAPERDOLL_FEET,
			Inventory.PAPERDOLL_BACK,
			Inventory.PAPERDOLL_UNDER,
			Inventory.PAPERDOLL_BELT,
			Inventory.PAPERDOLL_LFINGER,
			Inventory.PAPERDOLL_RFINGER,
			Inventory.PAPERDOLL_LEAR,
			Inventory.PAPERDOLL_REAR,
			Inventory.PAPERDOLL_NECK,
			Inventory.PAPERDOLL_LBRACELET
		};
	
	/** The Constant NAMES. */
	private static final String[] NAMES =
		{
			"Weapon",
			"Shield",
			"Helmet",
			"Chest",
			"Legs",
			"Costume",
			"Gloves",
			"Boots",
			"Cloak",
			"Shirt",
			"Belt",
			"Ring",
			" Ring",
			"Earring",
			"Earring",
			"Necklace",
			"Bracelet"
		};
	
	/**
	 * Instantiates a new clan BBS manager.
	 */
	public ClanBBSManager()
	{
		SORTED_CLANS = new L2Clan[0];
		_clanSkillDescriptions = new HashMap<>();
		_clanSkillDescriptions.put(370, new String[]
			{
				"Increases clan members' Max HP by 3%. It only affects those who are of an Heir rank or higher.",
				"Increases clan members' Max HP by 5%. It only affects those who are of an Heir rank or higher.",
				"Increases clan members' Max HP by 6%. It only affects those who are of an Heir rank or higher."
			});
		_clanSkillDescriptions.put(371, new String[]
			{
				"Increases clan members' Max CP by 6%. It only affects those who are of a Baron rank or higher.",
				"Increases clan members' Max CP by 10%. It only affects those who are of a Baron rank or higher.",
				"Increases clan members' Max CP by 12%. It only affects those who are of a Baron rank or higher."
			});
		_clanSkillDescriptions.put(372, new String[]
			{
				"Increases clan members' Max MP by 3%. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Max MP by 5%. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Max MP by 6%. It only affects those who are of a Viscount rank or higher."
			});
		_clanSkillDescriptions.put(373, new String[]
			{
				"Increases clan members' HP regeneration by 3%. It only affects those who are of an Heir rank or higher.",
				"Increases clan members' HP regeneration by 5%. It only affects those who are of an Heir rank or higher.",
				"Increases clan members' HP regeneration by 6%. It only affects those who are of an Heir rank or higher."
			});
		_clanSkillDescriptions.put(374, new String[]
			{
				"Increases clan members' CP regeneration by 6%. It only affects those who are of an Elder rank or higher.",
				"Increases clan members' CP regeneration by 10%. It only affects those who are of an Elder rank or higher.",
				"Increases clan members' CP regeneration by 12%. It only affects those who are of an Elder rank or higher."
			});
		_clanSkillDescriptions.put(375, new String[]
			{
				"Increases clan members' MP regeneration by 3%. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' MP regeneration by 5%. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' MP regeneration by 6%. It only affects those who are of a Viscount rank or higher."
			});
		_clanSkillDescriptions.put(376, new String[]
			{
				"Increases clan members' P. Atk. by 3%. It only affects those who are of a Knight rank or higher.",
				"Increases clan members' P. Atk. by 5%. It only affects those who are of a Knight rank or higher.",
				"Increases clan members' P. Atk. by 6%. It only affects those who are of a Knight rank or higher."
			});
		_clanSkillDescriptions.put(377, new String[]
			{
				"Increases clan members' P. Def. by 3%. It only affects those who are of a Knight rank or higher.",
				"Increases clan members' P. Def. by 5%. It only affects those who are of a Knight rank or higher.",
				"Increases clan members' P. Def. by 6%. It only affects those who are of a Knight rank or higher."
			});
		_clanSkillDescriptions.put(378, new String[]
			{
				"Increases clan members' M. Atk by 6%. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' M. Atk by 10%. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' M. Atk by 12%. It only affects those who are of a Viscount rank or higher."
			});
		_clanSkillDescriptions.put(379, new String[]
			{
				"Increases clan members' M. Def by 6%. It only affects those who are of an Heir rank or higher.",
				"Increases clan members' M. Def by 10%. It only affects those who are of an Heir rank or higher.",
				"Increases clan members' M. Def by 12%. It only affects those who are of an Heir rank or higher."
			});
		_clanSkillDescriptions.put(380, new String[]
			{
				"Increases clan members' Accuracy by 1. It only affects those who are of a Baron rank or higher.",
				"Increases clan members' Accuracy by 2. It only affects those who are of a Baron rank or higher.",
				"Increases clan members' Accuracy by 3. It only affects those who are of a Baron rank or higher."
			});
		_clanSkillDescriptions.put(381, new String[]
			{
				"Increases clan members' Evasion by 1. It only affects those who are of a Baron rank or higher.",
				"Increases clan members' Evasion by 2. It only affects those who are of a Baron rank or higher.",
				"Increases clan members' Evasion by 3. It only affects those who are of a Baron rank or higher."
			});
		_clanSkillDescriptions.put(382, new String[]
			{
				"Increases clan members' Shield Defense by 12%. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Shield Defense by 20%. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Shield Defense by 24%. It only affects those who are of a Viscount rank or higher."
			});
		_clanSkillDescriptions.put(383, new String[]
			{
				"Increases clan members' Shield Defense by 24%. It only affects those who are of a Baron rank or higher.",
				"Increases clan members' Shield Defense by 40%. It only affects those who are of a Baron rank or higher.",
				"Increases clan members' Shield Defense by 48%. It only affects those who are of a Baron rank or higher."
			});
		_clanSkillDescriptions.put(384, new String[]
			{
				"Increases clan members' Resistance to Water/Wind attacks by 3. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Resistance to Water/Wind attacks by 5. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Resistance to Water/Wind attacks by 6. It only affects those who are of a Viscount rank or higher."
			});
		_clanSkillDescriptions.put(385, new String[]
			{
				"Increases clan members' Resistance to Fire/Earth attacks by 3. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Resistance to Fire/Earth attacks by 5. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Resistance to Fire/Earth attacks by 6. It only affects those who are of a Viscount rank or higher."
			});
		_clanSkillDescriptions.put(386, new String[]
			{
				"Increases clan members' Resistance to Stun attacks by 12. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Resistance to Stun attacks by 20. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Resistance to Stun attacks by 24. It only affects those who are of a Viscount rank or higher."
			});
		_clanSkillDescriptions.put(387, new String[]
			{
				"Increases clan members' Resistance to Hold attacks by 12. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Resistance to Hold attacks by 20. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Resistance to Hold attacks by 24. It only affects those who are of a Viscount rank or higher."
			});
		_clanSkillDescriptions.put(388, new String[]
			{
				"Increases clan members' Resistance to Sleep attacks by 12. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Resistance to Sleep attacks by 20. It only affects those who are of a Viscount rank or higher.",
				"Increases clan members' Resistance to Sleep attacks by 24. It only affects those who are of a Viscount rank or higher."
			});
		_clanSkillDescriptions.put(389, new String[]
			{
				"Increases clan members' Speed by 3. It only affects those who are of a Count rank or higher.",
				"Increases clan members' Speed by 5. It only affects those who are of a Count rank or higher.",
				"Increases clan members' Speed by 6. It only affects those who are of a Count rank or higher."
			});
		_clanSkillDescriptions.put(390, new String[]
			{
				"Decreases clan members' experience loss and the chance of other death penalties when killed by a monster or player. It only affects those who are of an Heir rank or higher.",
				"Decreases clan members' experience loss and the chance of other death penalties when killed by a monster or player. It only affects those who are of an Heir rank or higher.",
				"Decreases clan members' experience loss and the chance of other death penalties when killed by a monster or player. It only affects those who are of an Heir rank or higher."
			});
		_clanSkillDescriptions.put(391, new String[]
			{
				"Grants the privilege of Command Channel formation. It only effects Sage / Elder class and above."
			});
		_clanSkillDescriptions.put(590, new String[]
			{
				"The Max HP of clan members in residence increases by 222."
			});
		_clanSkillDescriptions.put(591, new String[]
			{
				"The Max CP of clan members in residence increases by 444."
			});
		_clanSkillDescriptions.put(592, new String[]
			{
				"The Max MP of clan members in residence increases by 168."
			});
		_clanSkillDescriptions.put(593, new String[]
			{
				"The HP Recovery Bonus of clan members in residence increases by 1.09."
			});
		_clanSkillDescriptions.put(594, new String[]
			{
				"CP recovery bonus of clan members in residence increases by 1.09."
			});
		_clanSkillDescriptions.put(595, new String[]
			{
				"The MP Recovery Bonus of clan members in residence increases by 0.47."
			});
		_clanSkillDescriptions.put(596, new String[]
			{
				"P. Atk. of clan members in residence increases by 34.6."
			});
		_clanSkillDescriptions.put(597, new String[]
			{
				"P. Def. of clan members in residence increases by 54.7."
			});
		_clanSkillDescriptions.put(598, new String[]
			{
				"M. Atk. of clan members in residence increases by 40.4."
			});
		_clanSkillDescriptions.put(599, new String[]
			{
				"The M. Def. of clan members in residence increases by 44."
			});
		_clanSkillDescriptions.put(600, new String[]
			{
				"Accuracy of clan members in residence increases by 4."
			});
		_clanSkillDescriptions.put(601, new String[]
			{
				"Evasion of clan members in residence increases by 4."
			});
		_clanSkillDescriptions.put(602, new String[]
			{
				"Shield Defense of clan members in residence increases by 54.7."
			});
		_clanSkillDescriptions.put(603, new String[]
			{
				"Shield Defense. of clan members in residence increases by 225."
			});
		_clanSkillDescriptions.put(604, new String[]
			{
				"Resistance to Water and Wind attacks of clan members in residence increases by 10."
			});
		_clanSkillDescriptions.put(605, new String[]
			{
				"Resistance to Fire and Earth attacks of clan members in residence increases by 10."
			});
		_clanSkillDescriptions.put(606, new String[]
			{
				"Resistance to Stun attacks of clan members in residence increases by 10."
			});
		_clanSkillDescriptions.put(607, new String[]
			{
				"Resistance to Hold attacks of clan members in residence increases by 10."
			});
		_clanSkillDescriptions.put(608, new String[]
			{
				"Resistance to Sleep attacks of clan members in residence increases by 10."
			});
		_clanSkillDescriptions.put(609, new String[]
			{
				"The Speed of clan members in residence increases by 6."
			});
		_clanSkillDescriptions.put(610, new String[]
			{
				"When a clan member within the residence is killed by PK/ordinary monster, the Exp. points consumption rate and the probability of incurring a death after-effect are decreased."
			});
		_clanSkillDescriptions.put(611, new String[]
			{
				"The corresponding troops' P. Atk. increase by 17.3.",
				"The corresponding troops' P. Atk. increase by 17.3 and Critical Rate increase by 15.",
				"The corresponding troops' P. Atk. increase by 17.3, Critical Rate increase by 15, and Critical Damage increase by 100."
			});
		_clanSkillDescriptions.put(612, new String[]
			{
				"The corresponding troops' P. Def. increase by 27.3.",
				"The corresponding troops' P. Def. increase by 27.3 and M. Def. increase by 17.6.",
				"The corresponding troops' P. Def. increase by 27.3, M. Def. increase by 17.6, and Shield Defense. increase by 6%."
			});
		_clanSkillDescriptions.put(613, new String[]
			{
				"The corresponding troops' Accuracy increase by 2.",
				"The corresponding troops' Accuracy increase by 2 and Evasion increase by 2.",
				"The corresponding troops' Accuracy increase by 2, Evasion increase by 2, and Speed increase by 3."
			});
		_clanSkillDescriptions.put(614, new String[]
			{
				"The corresponding troops' M. Def. increase by 17.",
				"The corresponding troops' M. Def. increase by 31.1.",
				"The corresponding troops' M. Def. increase by 44."
			});
		_clanSkillDescriptions.put(615, new String[]
			{
				"The corresponding troops' heal power increase by 20.",
				"The corresponding troops' heal power increase by 20 and Max MP increase by 30%.",
				"The corresponding troops' heal power increase by 20, Max MP increase by 30%, and MP consumption decreases by 5%."
			});
		_clanSkillDescriptions.put(616, new String[]
			{
				"The corresponding troops' M. Atk. increase by 7.17.",
				"The corresponding troops' M. Atk. increase by 19.32.",
				"The corresponding troops' M. Atk. increase by 19.32 and magic Critical Damage rate increases by 1%."
			});
		_clanSkillDescriptions.put(848, new String[]
			{
				"STR+1 / INT+1"
			});
		_clanSkillDescriptions.put(849, new String[]
			{
				"DEX+1 / WIT+1"
			});
		_clanSkillDescriptions.put(850, new String[]
			{
				"STR+1 / MEN+1"
			});
		_clanSkillDescriptions.put(851, new String[]
			{
				"CON+1 / MEN+1"
			});
		_clanSkillDescriptions.put(852, new String[]
			{
				"DEX+1 / MEN+1"
			});
		_clanSkillDescriptions.put(853, new String[]
			{
				"CON+1 / INT+1"
			});
		_clanSkillDescriptions.put(854, new String[]
			{
				"DEX+1 / INT+1"
			});
		_clanSkillDescriptions.put(855, new String[]
			{
				"STR+1 / WIT+1"
			});
		_clanSkillDescriptions.put(856, new String[]
			{
				"CON+1 / WIT+1"
			});
		
		ALL_CLASSES = new LinkedHashMap<>();
		ALL_CLASSES.put(88, "Duelist");
		ALL_CLASSES.put(89, "Dreadnought");
		ALL_CLASSES.put(90, "PhoenixKnight");
		ALL_CLASSES.put(91, "HellKnight");
		ALL_CLASSES.put(92, "Adventurer");
		ALL_CLASSES.put(93, "Saggitarius");
		ALL_CLASSES.put(94, "Archmage");
		ALL_CLASSES.put(95, "SoulTaker");
		ALL_CLASSES.put(96, "ArcanaLord");
		ALL_CLASSES.put(97, "Cardinal");
		ALL_CLASSES.put(98, "Hierophant");
		ALL_CLASSES.put(99, "EvaTemplar");
		ALL_CLASSES.put(100, "SwordMuse");
		ALL_CLASSES.put(101, "WindRider");
		ALL_CLASSES.put(102, "MoonlightSentinel");
		ALL_CLASSES.put(103, "MysticMuse");
		ALL_CLASSES.put(104, "ElementalMaster");
		ALL_CLASSES.put(105, "EvaSaint");
		ALL_CLASSES.put(106, "ShillienTemplar");
		ALL_CLASSES.put(107, "SpectralDancer");
		ALL_CLASSES.put(108, "GhostHunter");
		ALL_CLASSES.put(109, "GhostSentinel");
		ALL_CLASSES.put(110, "StormScreamer");
		ALL_CLASSES.put(111, "SpectralMaster");
		ALL_CLASSES.put(112, "ShillienSaint");
		ALL_CLASSES.put(113, "Titan");
		ALL_CLASSES.put(114, "GrandKhauatari");
		ALL_CLASSES.put(115, "Dominator");
		ALL_CLASSES.put(116, "Doomcryer");
		ALL_CLASSES.put(117, "FortuneSeeker");
		ALL_CLASSES.put(118, "Maestro");
		ALL_CLASSES.put(130, "Arbalester");
		ALL_CLASSES.put(131, "Doombringer");
		ALL_CLASSES.put(132, "MaleSoulhound");
		ALL_CLASSES.put(133, "FemaleSoulhound");
		ALL_CLASSES.put(136, "Judicator");
		
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(() ->
		{
			L2Clan[] clans = ClanTable.getInstance().getClans();
			Arrays.sort(clans, (o1, o2) ->
			{
				if (o1.getLevel() > o2.getLevel())
				{
					return -1;
				}
				if (o2.getLevel() > o1.getLevel())
				{
					return 1;
				}
				if (o1.getReputationScore() > o2.getReputationScore())
				{
					return -1;
				}
				if (o2.getReputationScore() > o1.getReputationScore())
				{
					return 1;
				}
				return 0;
			});
			
			for (int i = 0; i < clans.length; i++)
			{
				if (clans[i].getName().equalsIgnoreCase("GameMasters"))
				{
					clans[i].setRank(0);
					continue;
				}
				clans[i].setRank(i + 1); // Since l2j doesnt set it anywhere, set it here. Not the best spot, not the worst spot.
			}
			
			SORTED_CLANS = clans;
		}, 1000, 300000); // Update clan ranking every 5min
	}
	
	/**
	 * Gets the single instance of ClanBBSManager.
	 * @return single instance of ClanBBSManager
	 */
	public static ClanBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * The Class SingletonHolder.
	 */
	private static class SingletonHolder
	{
		
		/** The Constant _instance. */
		protected static final ClanBBSManager _instance = new ClanBBSManager();
	}
}