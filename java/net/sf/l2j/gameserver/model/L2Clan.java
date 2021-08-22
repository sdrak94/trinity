package net.sf.l2j.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.communitybbs.BB.Forum;
import net.sf.l2j.gameserver.communitybbs.Manager.ForumsBBSManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CrownManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.LeaveWorld;
import net.sf.l2j.gameserver.network.serverpackets.PledgeReceiveSubPledgeCreated;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListDeleteAll;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillListAdd;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.4.2.7 $ $Date: 2005/04/06 16:13:41 $
 */
public class L2Clan
{
	private static final Logger					_log						= Logger.getLogger(L2Clan.class.getName());
	private String								_name;
	private int									_clanId;
	private L2ClanMember						_leader;
	private final Map<Integer, L2ClanMember>	_members					= new FastMap<Integer, L2ClanMember>();
	private String								_allyName;
	private int									_allyId;
	private int									_level;
	private int									_hasCastle;
	private int									_hasFort;
	private int									_hasHideout;
	private boolean								_hasCrest;
	private int									_hiredGuards;
	private int									_crestId;
	private int									_crestLargeId;
	private int									_allyCrestId;
	private int									_auctionBiddedAt			= 0;
	private long								_allyPenaltyExpiryTime;
	private int									_allyPenaltyType;
	private long								_charPenaltyExpiryTime;
	private long								_dissolvingExpiryTime;
	private int									_rbKills					= 0;
	private int									_pvpKills;
	public List<Integer>						tempWarClanIds				= new FastList<Integer>();
	// Ally Penalty Types
	/** Clan leaved ally */
	public static final int						PENALTY_TYPE_CLAN_LEAVED	= 1;
	/** Clan was dismissed from ally */
	public static final int						PENALTY_TYPE_CLAN_DISMISSED	= 2;
	/** Leader clan dismiss clan from ally */
	public static final int						PENALTY_TYPE_DISMISS_CLAN	= 3;
	/** Leader clan dissolve ally */
	public static final int						PENALTY_TYPE_DISSOLVE_ALLY	= 4;
	private final ItemContainer					_warehouse					= new ClanWarehouse(this);
	private final List<Integer>					_atWarWith					= new FastList<Integer>();
	private final List<Integer>					_atWarAttackers				= new FastList<Integer>();
	private boolean								_hasCrestLarge;
	private Forum								_forum;
	private final List<L2Skill>					_skillList					= new FastList<L2Skill>();
	// Clan Privileges
	/** No privilege to manage any clan activity */
	public static final int						CP_NOTHING					= 0;
	/** Privilege to join clan */
	public static final int						CP_CL_JOIN_CLAN				= 2;
	/** Privilege to give a title */
	public static final int						CP_CL_GIVE_TITLE			= 4;
	/** Privilege to view warehouse content */
	public static final int						CP_CL_VIEW_WAREHOUSE		= 8;
	/** Privilege to manage clan ranks */
	public static final int						CP_CL_MANAGE_RANKS			= 16;
	public static final int						CP_CL_PLEDGE_WAR			= 32;
	public static final int						CP_CL_DISMISS				= 64;
	/** Privilege to register clan crest */
	public static final int						CP_CL_REGISTER_CREST		= 128;
	public static final int						CP_CL_APPRENTICE			= 256;
	public static final int						CP_CL_TROOPS_FAME			= 512;
	public static final int						CP_CL_SUMMON_AIRSHIP		= 1024;
	/** Privilege to open a door */
	public static final int						CP_CH_OPEN_DOOR				= 2048;
	public static final int						CP_CH_OTHER_RIGHTS			= 4096;
	public static final int						CP_CH_AUCTION				= 8192;
	public static final int						CP_CH_DISMISS				= 16384;
	public static final int						CP_CH_SET_FUNCTIONS			= 32768;
	public static final int						CP_CS_OPEN_DOOR				= 65536;
	public static final int						CP_CS_MANOR_ADMIN			= 131072;
	public static final int						CP_CS_MANAGE_SIEGE			= 262144;
	public static final int						CP_CS_USE_FUNCTIONS			= 524288;
	public static final int						CP_CS_DISMISS				= 1048576;
	public static final int						CP_CS_TAXES					= 2097152;
	public static final int						CP_CS_MERCENARIES			= 4194304;
	public static final int						CP_CS_SET_FUNCTIONS			= 8388608;
	/** Privilege to manage all clan activity */
	public static final int						CP_ALL						= 16777214;
	// Sub-unit types
	/** Clan subunit type of Academy */
	public static final int						SUBUNIT_ACADEMY				= -1;
	/** Clan subunit type of Main Clan */
	public static final int						SUBUNIT_MAIN_CLAN			= 0;
	/** Clan subunit type of Royal Guard A */
	public static final int						SUBUNIT_ROYAL1				= 100;
	/** Clan subunit type of Royal Guard B */
	public static final int						SUBUNIT_ROYAL2				= 200;
	/** Clan subunit type of Order of Knights A-1 */
	public static final int						SUBUNIT_KNIGHT1				= 1001;
	/** Clan subunit type of Order of Knights A-2 */
	public static final int						SUBUNIT_KNIGHT2				= 1002;
	/** Clan subunit type of Order of Knights B-1 */
	public static final int						SUBUNIT_KNIGHT3				= 2001;
	/** Clan subunit type of Order of Knights B-2 */
	public static final int						SUBUNIT_KNIGHT4				= 2002;
	/** FastMap(Integer, L2Skill) containing all skills of the L2Clan */
	protected final Map<Integer, L2Skill>		_skills						= new FastMap<Integer, L2Skill>();
	protected final Map<Integer, RankPrivs>		_privs						= new FastMap<Integer, RankPrivs>();
	protected final Map<Integer, SubPledge>		_subPledges					= new FastMap<Integer, SubPledge>();
	private int									_reputationScore			= 0;
	private int									_rank						= 0;
	private String								_notice;
	private boolean								_noticeEnabled				= false;
	private static final int					MAX_NOTICE_LENGTH			= 8192;
	
	/**
	 * Called if a clan is referenced only by id.
	 * In this case all other data needs to be fetched from db
	 *
	 * @param clanId
	 *            A valid clan Id to create and restore
	 */
	public L2Clan(int clanId)
	{
		_clanId = clanId;
		initializePrivs();
		restore();
		getWarehouse().restore();
	}
	
	/**
	 * Called only if a new clan is created
	 *
	 * @param clanId
	 *            A valid clan Id to create
	 * @param clanName
	 *            A valid clan name
	 */
	public L2Clan(int clanId, String clanName)
	{
		_clanId = clanId;
		_name = clanName;
		initializePrivs();
	}
	
	/**
	 * @return Returns the clanId.
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * @param clanId
	 *            The clanId to set.
	 */
	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}
	
	/**
	 * @return Returns the leaderId.
	 */
	public int getLeaderId()
	{
		return (_leader != null ? _leader.getObjectId() : 0);
	}
	
	/**
	 * @return L2ClanMember of clan leader.
	 */
	public L2ClanMember getLeader()
	{
		return _leader;
	}
	
	/**
	 * @param leaderId
	 *            The leaderId to set.
	 */
	public void setLeader(L2ClanMember leader)
	{
		_leader = leader;
		_members.put(leader.getObjectId(), leader);
	}
	
	public void setNewLeader(L2ClanMember member)
	{
		if (!getLeader().isOnline())
		{
			return;
		}
		if (member == null)
		{
			return;
		}
		if (!member.isOnline())
		{
			return;
		}
		L2PcInstance exLeader = getLeader().getPlayerInstance();
		SiegeManager.getInstance().removeSiegeSkills(exLeader);
		exLeader.setClan(this);
		exLeader.setClanPrivileges(L2Clan.CP_NOTHING);
		exLeader.broadcastUserInfo();
		setLeader(member);
		updateClanInDB();
		exLeader.setPledgeClass(exLeader.getClan().getClanMember(exLeader.getObjectId()).calculatePledgeClass(exLeader));
		exLeader.broadcastUserInfo();
		exLeader.checkItemRestriction();
		L2PcInstance newLeader = member.getPlayerInstance();
		newLeader.setClan(this);
		newLeader.setPledgeClass(member.calculatePledgeClass(newLeader));
		newLeader.setClanPrivileges(L2Clan.CP_ALL);
		if (getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel())
		{
			SiegeManager.getInstance().addSiegeSkills(newLeader);
			// Transferring siege skills TimeStamps from old leader to new leader to prevent unlimited headquarters
			if (!exLeader.getReuseTimeStamp().isEmpty())
			{
				for (L2Skill sk : SkillTable.getInstance().getSiegeSkills(newLeader.isNoble()))
				{
					if (exLeader.getReuseTimeStamp().containsKey(sk.getId()))
						newLeader.addTimeStamp(exLeader.getReuseTimeStamp().get(sk.getId()));
				}
				newLeader.sendPacket(new SkillCoolTime(newLeader));
			}
		}
		newLeader.broadcastUserInfo();
		broadcastClanStatus();
		SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_LEADER_PRIVILEGES_HAVE_BEEN_TRANSFERRED_TO_C1);
		sm.addString(newLeader.getName());
		broadcastToOnlineMembers(sm);
		sm = null;
		CrownManager.checkCrowns(exLeader);
		CrownManager.checkCrowns(newLeader);
	}
	
	/**
	 * @return Returns the leaderName.
	 */
	public String getLeaderName()
	{
		String name = "";
		try
		{
			name = _members.get(Integer.valueOf(getLeaderId())).getName();
		}
		catch (NullPointerException e)
		{
			final L2PcInstance leader = (L2PcInstance) L2World.getInstance().findObject(getLeaderId());
			if (leader != null)
			{
				leader.setClan(this);
				L2ClanMember member = new L2ClanMember(this, leader);
				setLeader(member);
				broadcastToOnlineMembers(new PledgeShowMemberListAll(this, leader));
				name = leader.getName();
				if (leader.isOnline() == 1)
				{
					leader.sendPacket(new LeaveWorld());
					leader.getClient().setActiveChar(null);
					leader.deleteMe();
				}
			}
			else
			{
				Connection con = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("UPDATE characters set clanid=? WHERE charId=?");
					statement.setInt(1, _clanId);
					statement.setInt(2, getLeaderId());
					statement.execute();
					statement.close();
				}
				catch (SQLException eg)
				{
					_log.warning("could not set failed clan leader:" + e.getMessage());
				}
				finally
				{
					try
					{
						con.close();
					}
					catch (Exception ec)
					{}
				}
			}
		}
		return name;
	}
	
	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name)
	{
		_name = name;
	}
	
	public void addClanMember(L2ClanMember member)
	{
		_members.put(member.getObjectId(), member);
	}
	
	public void addClanMember(L2PcInstance player)
	{
		try
		{
			Olympiad.resetNobleStats(player.getObjectId());
			player.sendMessage("Your olympiad stats have been wiped due to joining a clan");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		// Using a different constructor, to make it easier to read
		// L2ClanMember(L2Clan, L2PcInstance)
		// L2ClanMember member = new L2ClanMember(this,player.getName(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getTitle(), player.getAppearance().getSex(), player.getRace().ordinal());
		L2ClanMember member = new L2ClanMember(this, player);
		// store in memory
		//
		addClanMember(member);
		member.setPlayerInstance(player);
		player.setClan(this);
		player.setPledgeClass(member.calculatePledgeClass(player));
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new ExBrExtraUserInfo(player));
	}
	
	public void updateClanMember(L2PcInstance player)
	{
		L2ClanMember member = new L2ClanMember(player);
		if (player.isClanLeader())
			setLeader(member);
		addClanMember(member);
	}
	
	public L2ClanMember getClanMember(String name)
	{
		for (L2ClanMember temp : _members.values())
		{
			if (temp.getName().equals(name))
				return temp;
		}
		return null;
	}
	
	public L2ClanMember getClanMember(int objectID)
	{
		return _members.get(objectID);
	}
	
	public void removeClanMember(int objectId, long clanJoinExpiryTime)
	{
		L2ClanMember exMember = _members.remove(objectId);
		if (exMember == null)
		{
			_log.warning("Member Object ID: " + objectId + " not found in clan while trying to remove");
			return;
		}
		try
		{
			Olympiad.resetNobleStats(objectId);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		int leadssubpledge = getLeaderSubPledge(objectId);
		if (leadssubpledge != 0)
		{
			// Sub-unit leader withdraws, position becomes vacant and leader
			// should appoint new via NPC
			getSubPledge(leadssubpledge).setLeaderId(0);
			updateSubPledgeInDB(leadssubpledge);
		}
		if (exMember.getApprentice() != 0)
		{
			L2ClanMember apprentice = getClanMember(exMember.getApprentice());
			if (apprentice != null)
			{
				if (apprentice.getPlayerInstance() != null)
					apprentice.getPlayerInstance().setSponsor(0);
				else
					apprentice.initApprenticeAndSponsor(0, 0);
				apprentice.saveApprenticeAndSponsor(0, 0);
			}
		}
		if (exMember.getSponsor() != 0)
		{
			L2ClanMember sponsor = getClanMember(exMember.getSponsor());
			if (sponsor != null)
			{
				if (sponsor.getPlayerInstance() != null)
					sponsor.getPlayerInstance().setApprentice(0);
				else
					sponsor.initApprenticeAndSponsor(0, 0);
				sponsor.saveApprenticeAndSponsor(0, 0);
			}
		}
		exMember.saveApprenticeAndSponsor(0, 0);
		if (Config.REMOVE_CASTLE_CIRCLETS)
		{
			CastleManager.getInstance().removeCirclet(exMember, getHasCastle());
		}
		if (exMember.isOnline())
		{
			L2PcInstance player = exMember.getPlayerInstance();
			/* player.setTitle(""); */
			player.setApprentice(0);
			player.setSponsor(0);
			if (player.isClanLeader())
			{
				SiegeManager.getInstance().removeSiegeSkills(player);
				player.setClanCreateExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L); // 24*60*60*1000 = 86400000
			}
			// remove Clanskills from Player
			for (L2Skill skill : player.getClan().getAllSkills())
				player.removeSkill(skill, false);
			// remove Residential skills
			if (player.getClan().getHasCastle() > 0)
				CastleManager.getInstance().getCastleByOwner(player.getClan()).removeResidentialSkills(player);
			if (player.getClan().getHasFort() > 0)
				FortManager.getInstance().getFortByOwner(player.getClan()).removeResidentialSkills(player);
			player.sendSkillList();
			player.setClan(null);
			// players leaving from clan academy have no penalty
			if (exMember.getPledgeType() != -1)
				player.setClanJoinExpiryTime(clanJoinExpiryTime);
			player.setPledgeClass(exMember.calculatePledgeClass(player));
			CrownManager.checkCrowns(player);
			player.broadcastUserInfo();
			// disable clan tab
			player.sendPacket(new PledgeShowMemberListDeleteAll());
		}
		else
		{
			removeMemberInDatabase(exMember, clanJoinExpiryTime, getLeaderId() == objectId ? System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L : 0);
		}
	}
	
	public Castle getCastle()
	{
		return CastleManager.getInstance().getCastleByOwner(this);
	}
	
	public L2ClanMember[] getMembers()
	{
		return _members.values().toArray(new L2ClanMember[_members.size()]);
	}
	
	public int getMembersCount()
	{
		return _members.size();
	}
	
	public int getSubPledgeMembersCount(int subpl)
	{
		int result = 0;
		for (L2ClanMember temp : _members.values())
		{
			if (temp.getPledgeType() == subpl)
				result++;
		}
		return result;
	}
	
	public int getMaxNrOfMembers(int pledgetype)
	{
		int limit = 0;
		switch (pledgetype)
		{
			case 0:
				switch (getLevel())
				{
					case 5:
						limit = 40;
						break;
					case 6:
						limit = 45;
						break;
					case 7:
						limit = 50;
						break;
					case 8:
						limit = 55;
						break;
					case 9:
						limit = 60;
						break;
					case 10:
						limit = 65;
						break;
					default:
						limit = 40;
						break;
				}
				break;
			case -1:
			case 100:
			case 200:
				limit = 10;
				break;
			case 1001:
			case 1002:
			case 2001:
			case 2002:
				switch (getLevel())
				{
					case 9:
					case 10:
						limit = 10;
						break;
					default:
						limit = 10;
						break;
				}
				break;
			default:
				break;
		}
		return limit;
	}
	
	public L2PcInstance[] getOnlineMembers(int exclude)
	{
		List<L2PcInstance> result = new FastList<L2PcInstance>();
		for (L2ClanMember temp : _members.values())
		{
			try
			{
				if (temp.isOnline() && !(temp.getObjectId() == exclude))
					result.add(temp.getPlayerInstance());
			}
			catch (NullPointerException e)
			{}
		}
		return result.toArray(new L2PcInstance[result.size()]);
	}
	
	/**
	 * @return
	 */
	public int getAllyId()
	{
		return _allyId;
	}
	
	/**
	 * @return
	 */
	public String getAllyName()
	{
		return _allyName;
	}
	
	public void setAllyCrestId(int allyCrestId)
	{
		_allyCrestId = allyCrestId;
	}
	
	/**
	 * @return
	 */
	public int getAllyCrestId()
	{
		return _allyCrestId;
	}
	
	/**
	 * @return
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * @return
	 */
	public int getHasCastle()
	{
		return _hasCastle;
	}
	
	/**
	 * @return
	 */
	public int getHasFort()
	{
		return _hasFort;
	}
	
	/**
	 * @return
	 */
	public int getHasHideout()
	{
		return _hasHideout;
	}
	
	/**
	 * @param crestId
	 *            The id of pledge crest.
	 */
	public void setCrestId(int crestId)
	{
		_crestId = crestId;
	}
	
	/**
	 * @return Returns the clanCrestId.
	 */
	public int getCrestId()
	{
		return _crestId;
	}
	
	/**
	 * @param crestLargeId
	 *            The id of pledge LargeCrest.
	 */
	public void setCrestLargeId(int crestLargeId)
	{
		_crestLargeId = crestLargeId;
	}
	
	/**
	 * @return Returns the clan CrestLargeId
	 */
	public int getCrestLargeId()
	{
		return _crestLargeId;
	}
	
	/**
	 * @param allyId
	 *            The allyId to set.
	 */
	public void setAllyId(int allyId)
	{
		_allyId = allyId;
	}
	
	/**
	 * @param allyName
	 *            The allyName to set.
	 */
	public void setAllyName(String allyName)
	{
		_allyName = allyName;
	}
	
	/**
	 * @param hasCastle
	 *            The hasCastle to set.
	 */
	public void setHasCastle(int hasCastle)
	{
		_hasCastle = hasCastle;
	}
	
	/**
	 * @param hasFort
	 *            The hasFort to set.
	 */
	public void setHasFort(int hasFort)
	{
		_hasFort = hasFort;
	}
	
	/**
	 * @param hasHideout
	 *            The hasHideout to set.
	 */
	public void setHasHideout(int hasHideout)
	{
		_hasHideout = hasHideout;
	}
	
	/**
	 * @param level
	 *            The level to set.
	 */
	public void setLevel(int level)
	{
		_level = level;
		if (_forum == null && Config.COMMUNITY_TYPE > 0)
		{
			Forum forum = ForumsBBSManager.getInstance().getForumByName("ClanRoot");
			if (forum != null)
			{
				_forum = forum.getChildByName(_name);
				if (_forum == null && GameTimeController.getGameTicks() > 700)
				{
					_forum = ForumsBBSManager.getInstance().createNewForum(_name, ForumsBBSManager.getInstance().getForumByName("ClanRoot"), Forum.CLAN, Forum.CLANMEMBERONLY, getClanId());
				}
			}
		}
	}
	/*
	 * public void setLevel2(int level)
	 * {
	 * _level = level;
	 * if(_forum == null)
	 * {
	 * Forum forum = ForumsBBSManager.getInstance().getForumByName("ClanRoot");
	 * if (forum != null)
	 * _forum = forum.getChildByName(_name);
	 * }
	 * }
	 */
	
	/**
	 * @param player
	 *            name
	 * @return
	 */
	public boolean isMember(int id)
	{
		return (id == 0 ? false : _members.containsKey(id));
	}
	
	public void updateClanInDB()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET leader_id=?,ally_id=?,ally_name=?,reputation_score=?,ally_penalty_expiry_time=?,ally_penalty_type=?,char_penalty_expiry_time=?,dissolving_expiry_time=?,pvpKills=?,ally_crest_id=? WHERE clan_id=?");
			statement.setInt(1, getLeaderId());
			statement.setInt(2, getAllyId());
			statement.setString(3, getAllyName());
			statement.setInt(4, getReputationScore());
			statement.setLong(5, getAllyPenaltyExpiryTime());
			statement.setInt(6, getAllyPenaltyType());
			statement.setLong(7, getCharPenaltyExpiryTime());
			statement.setLong(8, getDissolvingExpiryTime());
			statement.setLong(9, getPvpKills());
			statement.setInt(10, getAllyCrestId());
			statement.setInt(11, getClanId());
			statement.execute();
			if (Config.DEBUG)
				_log.fine("New clan leader saved in db: " + getClanId());
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error saving new clan leader.", e);
		}
		finally
		{
			if (statement != null)
				try
				{
					statement.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			if (con != null)
				try
				{
					con.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
		}
	}
	
	public void store()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id,pvpKills) values (?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, getClanId());
			statement.setString(2, getName());
			statement.setInt(3, getLevel());
			statement.setInt(4, getHasCastle());
			statement.setInt(5, getAllyId());
			statement.setString(6, getAllyName());
			statement.setInt(7, getLeaderId());
			statement.setInt(8, getCrestId());
			statement.setInt(9, getCrestLargeId());
			statement.setInt(10, getAllyCrestId());
			statement.setInt(11, getPvpKills());
			statement.execute();
			statement.close();
			if (Config.DEBUG)
				_log.fine("New clan saved in db: " + getClanId());
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error saving new clan.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	private void removeMemberInDatabase(L2ClanMember member, long clanJoinExpiryTime, long clanCreateExpiryTime)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET clanid=0, title=?, clan_join_expiry_time=?, clan_create_expiry_time=?, clan_privs=0, wantspeace=0, subpledge=0, lvl_joined_academy=0, apprentice=0, sponsor=0 WHERE charId=?");
			statement.setString(1, "");
			statement.setLong(2, clanJoinExpiryTime);
			statement.setLong(3, clanCreateExpiryTime);
			statement.setInt(4, member.getObjectId());
			statement.execute();
			statement.close();
			if (Config.DEBUG)
				_log.fine("clan member removed in db: " + getClanId());
			statement = con.prepareStatement("UPDATE characters SET apprentice=0 WHERE apprentice=?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
			statement.close();
			statement = con.prepareStatement("UPDATE characters SET sponsor=0 WHERE sponsor=?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error removing clan member.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	private void restore()
	{
		// restorewars();
		Connection con = null;
		try
		{
			L2ClanMember member;
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id,reputation_score,pvpKills,auction_bid_at,ally_penalty_expiry_time,ally_penalty_type,char_penalty_expiry_time,dissolving_expiry_time FROM clan_data where clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet clanData = statement.executeQuery();
			if (clanData.next())
			{
				setName(clanData.getString("clan_name"));
				setLevel(clanData.getInt("clan_level"));
				setHasCastle(clanData.getInt("hasCastle"));
				setAllyId(clanData.getInt("ally_id"));
				setAllyName(clanData.getString("ally_name"));
				setAllyPenaltyExpiryTime(clanData.getLong("ally_penalty_expiry_time"), clanData.getInt("ally_penalty_type"));
				if (getAllyPenaltyExpiryTime() < System.currentTimeMillis())
				{
					setAllyPenaltyExpiryTime(0, 0);
				}
				setCharPenaltyExpiryTime(clanData.getLong("char_penalty_expiry_time"));
				if (getCharPenaltyExpiryTime() + Config.ALT_CLAN_JOIN_DAYS * 86400000L < System.currentTimeMillis()) // 24*60*60*1000 = 86400000
				{
					setCharPenaltyExpiryTime(0);
				}
				setDissolvingExpiryTime(clanData.getLong("dissolving_expiry_time"));
				setCrestId(clanData.getInt("crest_id"));
				if (getCrestId() != 0)
				{
					setHasCrest(true);
				}
				setCrestLargeId(clanData.getInt("crest_large_id"));
				if (getCrestLargeId() != 0)
				{
					setHasCrestLarge(true);
				}
				setAllyCrestId(clanData.getInt("ally_crest_id"));
				setReputationScore(clanData.getInt("reputation_score"), false);
				setPvpKills(clanData.getInt("pvpKills"), false);
				setAuctionBiddedAt(clanData.getInt("auction_bid_at"), false);
				int leaderId = (clanData.getInt("leader_id"));
				PreparedStatement statement2 = con.prepareStatement("SELECT char_name,level,classid,charId,title,power_grade,subpledge,apprentice,sponsor,sex,race FROM characters WHERE clanid=?");
				statement2.setInt(1, getClanId());
				ResultSet clanMembers = statement2.executeQuery();
				while (clanMembers.next())
				{
					member = new L2ClanMember(this, clanMembers.getString("char_name"), clanMembers.getInt("level"), clanMembers.getInt("classid"), clanMembers.getInt("charId"), clanMembers.getInt("subpledge"), clanMembers.getInt("power_grade"), clanMembers.getString("title"), (clanMembers.getInt("sex") != 0), clanMembers.getInt("race"));
					if (member.getObjectId() == leaderId)
						setLeader(member);
					else
						addClanMember(member);
					member.initApprenticeAndSponsor(clanMembers.getInt("apprentice"), clanMembers.getInt("sponsor"));
				}
				clanMembers.close();
				statement2.close();
			}
			clanData.close();
			statement.close();
			if (Config.DEBUG && getName() != null)
				_log.config("Restored clan data for \"" + getName() + "\" from database.");
			restoreSubPledges();
			restoreRankPrivs();
			restoreSkills();
			restoreNotice();
			restoreClanRecruitment();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error restoring clan data.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	private void restoreNotice()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT enabled,notice FROM clan_notices WHERE clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet noticeData = statement.executeQuery();
			while (noticeData.next())
			{
				_noticeEnabled = noticeData.getBoolean("enabled");
				_notice = noticeData.getString("notice");
			}
			noticeData.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error restoring clan notice.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	private void storeNotice(String notice, boolean enabled)
	{
		if (notice == null)
			notice = "";
		if (notice.length() > MAX_NOTICE_LENGTH)
			notice = notice.substring(0, MAX_NOTICE_LENGTH - 1);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_notices (clan_id,notice,enabled) values (?,?,?) ON DUPLICATE KEY UPDATE notice=?,enabled=?");
			statement.setInt(1, getClanId());
			statement.setString(2, notice);
			if (enabled)
				statement.setString(3, "true");
			else
				statement.setString(3, "false");
			statement.setString(4, notice);
			if (enabled)
				statement.setString(5, "true");
			else
				statement.setString(5, "false");
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Error could not store clan notice: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		_notice = notice;
		_noticeEnabled = enabled;
	}
	
	public void setNoticeEnabled(boolean enabled)
	{
		storeNotice(_notice, enabled);
	}
	
	public void setNotice(String notice)
	{
		storeNotice(notice, _noticeEnabled);
	}
	
	public boolean isNoticeEnabled()
	{
		return _noticeEnabled;
	}
	
	public String getNotice()
	{
		if (_notice == null)
			return "";
		return _notice;
	}
	
	private void restoreSkills()
	{
		Connection con = null;
		try
		{
			// Retrieve all skills of this L2PcInstance from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT skill_id,skill_level FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();
			// Go though the recordset of this SQL query
			while (rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");
				// Create a L2Skill object for each record
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				// Add the L2Skill object to the L2Clan _skills
				_skills.put(skill.getId(), skill);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error restoring clan skills.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	/** used to retrieve all skills */
	public final L2Skill[] getAllSkills()
	{
		if (_skills == null)
			return new L2Skill[0];
		return _skills.values().toArray(new L2Skill[_skills.values().size()]);
	}
	
	/** used to add a skill to skill list of this L2Clan */
	public L2Skill addSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;
		if (newSkill != null)
		{
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);
		}
		return oldSkill;
	}
	
	/** used to add a new skill to the list, send a packet to all online clan members, update their stats and store it in db */
	public L2Skill addNewSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;
		Connection con = null;
		if (newSkill != null)
		{
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement;
				if (oldSkill != null)
				{
					statement = con.prepareStatement("UPDATE clan_skills SET skill_level=? WHERE skill_id=? AND clan_id=?");
					statement.setInt(1, newSkill.getLevel());
					statement.setInt(2, oldSkill.getId());
					statement.setInt(3, getClanId());
					statement.execute();
					statement.close();
				}
				else
				{
					statement = con.prepareStatement("INSERT INTO clan_skills (clan_id,skill_id,skill_level,skill_name) VALUES (?,?,?,?)");
					statement.setInt(1, getClanId());
					statement.setInt(2, newSkill.getId());
					statement.setInt(3, newSkill.getLevel());
					statement.setString(4, newSkill.getName());
					statement.execute();
					statement.close();
				}
			}
			catch (Exception e)
			{
				_log.warning("Error could not store clan skills: " + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
			for (L2ClanMember temp : _members.values())
			{
				try
				{
					if (temp.isOnline())
					{
						if (newSkill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass())
						{
							temp.getPlayerInstance().addSkill(newSkill, false); // Skill is not saved to player DB
							temp.getPlayerInstance().sendPacket(new PledgeSkillListAdd(newSkill.getId(), newSkill.getLevel()));
						}
					}
				}
				catch (NullPointerException e)
				{}
			}
		}
		return oldSkill;
	}
	
	public void addSkillEffects()
	{
		for (L2Skill skill : _skills.values())
		{
			for (L2ClanMember temp : _members.values())
			{
				try
				{
					if (temp.isOnline())
					{
						if (skill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass())
							temp.getPlayerInstance().addSkill(skill, false); // Skill is not saved to player DB
					}
				}
				catch (NullPointerException e)
				{}
			}
		}
	}
	
	public void addSkillEffects(L2PcInstance cm)
	{
		if (cm == null)
			return;
		for (L2Skill skill : _skills.values())
		{
			// TODO add skills according to members class( in ex. don't add Clan Agillity skill's effect to lower class then Baron)
			if (skill.getMinPledgeClass() <= cm.getPledgeClass())
				cm.addSkill(skill, false); // Skill is not saved to player DB
		}
	}
	
	/*
	 * public void broadcastToOnlineAllyMembers(L2GameServerPacket packet)
	 * {
	 * if (getAllyId() == 0)
	 * {
	 * return;
	 * }
	 * for (L2Clan clan : _allyList.values())
	 * {
	 * clan.broadcastToOnlineMembers(packet);
	 * }
	 * }
	 */
	public void broadcastToOnlineAllyMembers(L2GameServerPacket packet)
	{
		if (getAllyId() == 0)
		{
			return;
		}
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == getAllyId())
			{
				clan.broadcastToOnlineMembers(packet);
			}
		}
	}
	
	public void broadcastToOnlineMembers(L2GameServerPacket packet)
	{
		for (L2ClanMember member : _members.values())
		{
			try
			{
				if (member.isOnline())
					member.getPlayerInstance().sendPacket(packet);
			}
			catch (NullPointerException e)
			{}
		}
	}
	
	public void broadcastCSToOnlineMembers(CreatureSay packet, L2PcInstance broadcaster)
	{
		for (L2ClanMember member : _members.values())
		{
			try
			{
				if (member.isOnline() && !BlockList.isBlocked(member.getPlayerInstance(), broadcaster))
					member.getPlayerInstance().sendPacket(packet);
			}
			catch (NullPointerException e)
			{}
		}
	}
	
	public void broadcastToOtherOnlineMembers(L2GameServerPacket packet, L2PcInstance player)
	{
		for (L2ClanMember member : _members.values())
		{
			try
			{
				if (member.isOnline() && member.getPlayerInstance() != player)
					member.getPlayerInstance().sendPacket(packet);
			}
			catch (NullPointerException e)
			{}
		}
	}
	
	@Override
	public String toString()
	{
		return getName();
	}
	
	/**
	 * @return
	 */
	public boolean hasCrest()
	{
		return _hasCrest;
	}
	
	public boolean hasCrestLarge()
	{
		return _hasCrestLarge;
	}
	
	public void setHasCrest(boolean flag)
	{
		_hasCrest = flag;
	}
	
	public void setHasCrestLarge(boolean flag)
	{
		_hasCrestLarge = flag;
	}
	
	public ItemContainer getWarehouse()
	{
		return _warehouse;
	}
	
	public boolean isAtWarWith(Integer id)
	{
		if (_atWarWith != null && !_atWarWith.isEmpty())
			if (_atWarWith.contains(id))
				return true;
		return false;
	}
	
	public boolean isAtWarAttacker(Integer id)
	{
		if (_atWarAttackers != null && !_atWarAttackers.isEmpty())
			if (_atWarAttackers.contains(id))
				return true;
		return false;
	}
	
	public void setEnemyClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarWith.add(id);
	}
	
	public void setEnemyClan(Integer clan)
	{
		_atWarWith.add(clan);
	}
	
	public void setAttackerClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarAttackers.add(id);
	}
	
	public void setAttackerClan(Integer clan)
	{
		_atWarAttackers.add(clan);
	}
	
	public void deleteEnemyClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarWith.remove(id);
	}
	
	public void deleteAttackerClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarAttackers.remove(id);
	}
	
	public int getHiredGuards()
	{
		return _hiredGuards;
	}
	
	public void incrementHiredGuards()
	{
		_hiredGuards++;
	}
	
	public boolean isAtWar()
	{
		if (_atWarWith != null && !_atWarWith.isEmpty())
			return true;
		return false;
	}
	
	public List<Integer> getWarList()
	{
		return _atWarWith;
	}
	
	public List<Integer> getAttackerList()
	{
		return _atWarAttackers;
	}
	
	public void broadcastClanStatus()
	{
		for (L2PcInstance member : getOnlineMembers(0))
		{
			member.sendPacket(new PledgeShowMemberListDeleteAll());
			member.sendPacket(new PledgeShowMemberListAll(this, member));
		}
	}
	
	public void removeSkill(int id)
	{
		L2Skill deleteSkill = null;
		for (L2Skill sk : _skillList)
		{
			if (sk.getId() == id)
			{
				deleteSkill = sk;
				return;
			}
		}
		_skillList.remove(deleteSkill);
	}
	
	public void removeSkill(L2Skill deleteSkill)
	{
		_skillList.remove(deleteSkill);
	}
	
	/**
	 * @return
	 */
	public List<L2Skill> getSkills()
	{
		return _skillList;
	}
	
	public class SubPledge
	{
		private final int _id;
		private String _subPledgeName;
		private int _leaderId;
		
		public SubPledge(int id, String name, int leaderId)
		{
			_id = id;
			_subPledgeName = name;
			_leaderId = leaderId;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public String getName()
		{
			return _subPledgeName;
		}

		public void setName(final String name)
		{
			_subPledgeName = name;
		}
		
		public int getLeaderId()
		{
			return _leaderId;
		}
		
		public void setLeaderId(int leaderId)
		{
			_leaderId = leaderId;
		}
	}
	
	public class RankPrivs
	{
		private final int	_rankId;
		private final int	_party;		// TODO find out what this stuff means and implement it
		private int			_rankPrivs;
		
		public RankPrivs(int rank, int party, int privs)
		{
			_rankId = rank;
			_party = party;
			_rankPrivs = privs;
		}
		
		public int getRank()
		{
			return _rankId;
		}
		
		public int getParty()
		{
			return _party;
		}
		
		public int getPrivs()
		{
			return _rankPrivs;
		}
		
		public void setPrivs(int privs)
		{
			_rankPrivs = privs;
		}
	}
	
	private void restoreSubPledges()
	{
		Connection con = null;
		try
		{
			// Retrieve all subpledges of this clan from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT sub_pledge_id,name,leader_id FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				int id = rset.getInt("sub_pledge_id");
				String name = rset.getString("name");
				int leaderId = rset.getInt("leader_id");
				// Create a SubPledge object for each record
				SubPledge pledge = new SubPledge(id, name, leaderId);
				_subPledges.put(id, pledge);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore clan sub-units: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	/** used to retrieve subPledge by type */
	public final SubPledge getSubPledge(int pledgeType)
	{
		if (_subPledges == null)
			return null;
		return _subPledges.get(pledgeType);
	}
	
	/** used to retrieve subPledge by type */
	public final SubPledge getSubPledge(String pledgeName)
	{
		if (_subPledges == null)
			return null;
		for (SubPledge sp : _subPledges.values())
		{
			if (sp.getName().equalsIgnoreCase(pledgeName))
			{
				return sp;
			}
		}
		return null;
	}
	
	/** used to retrieve all subPledges */
	public final SubPledge[] getAllSubPledges()
	{
		if (_subPledges == null)
			return new SubPledge[0];
		return _subPledges.values().toArray(new SubPledge[_subPledges.values().size()]);
	}
	
	public SubPledge createSubPledge(L2PcInstance player, int pledgeType, int leaderId, String subPledgeName)
	{
		SubPledge subPledge = null;
		pledgeType = getAvailablePledgeTypes(pledgeType);
		if (pledgeType == 0)
		{
			if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
				player.sendPacket(new SystemMessage(SystemMessageId.CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY));
			else
				player.sendMessage("You can't create any more sub-units of this type");
			return null;
		}
		if (_leader.getObjectId() == leaderId)
		{
			player.sendMessage("Leader is not correct");
			return null;
		}
		if (pledgeType != -1 && ((getReputationScore() < 10000 && pledgeType < L2Clan.SUBUNIT_KNIGHT1) || (getReputationScore() < 10000 && pledgeType > L2Clan.SUBUNIT_ROYAL2)))
		{
			if (pledgeType < L2Clan.SUBUNIT_KNIGHT1)
			{
				player.sendMessage("You need 10000 clan reputation points");
			}
			else
			{
				player.sendMessage("You need 10000 clan reputation points");
			}
			SystemMessage sp = new SystemMessage(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
			player.sendPacket(sp);
			return null;
		}
		else
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_subpledges (clan_id,sub_pledge_id,name,leader_id) values (?,?,?,?)");
				statement.setInt(1, getClanId());
				statement.setInt(2, pledgeType);
				statement.setString(3, subPledgeName);
				if (pledgeType != -1)
					statement.setInt(4, leaderId);
				else
					statement.setInt(4, 0);
				statement.execute();
				statement.close();
				subPledge = new SubPledge(pledgeType, subPledgeName, leaderId);
				_subPledges.put(pledgeType, subPledge);
				if (pledgeType != -1)
				{
					if (pledgeType < L2Clan.SUBUNIT_KNIGHT1)
						setReputationScore(getReputationScore() - 10000, true);
					else
						setReputationScore(getReputationScore() - 10000, true);
				}
				if (Config.DEBUG)
					_log.fine("New sub_clan saved in db: " + getClanId() + "; " + pledgeType);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error saving sub clan data.", e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
		}
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(_leader.getClan()));
		broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(subPledge, _leader.getClan()));
		return subPledge;
	}
	
	public int getAvailablePledgeTypes(int pledgeType)
	{
		if (_subPledges.get(pledgeType) != null)
		{
			// _log.warning("found sub-unit with id: "+pledgeType);
			switch (pledgeType)
			{
				case SUBUNIT_ACADEMY:
					return 0;
				case SUBUNIT_ROYAL1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_ROYAL2);
					break;
				case SUBUNIT_ROYAL2:
					return 0;
				case SUBUNIT_KNIGHT1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT2);
					break;
				case SUBUNIT_KNIGHT2:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT3);
					break;
				case SUBUNIT_KNIGHT3:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT4);
					break;
				case SUBUNIT_KNIGHT4:
					return 0;
			}
		}
		return pledgeType;
	}
	
	public void updateSubPledgeInDB(int pledgeType)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE clan_subpledges SET leader_id=? WHERE clan_id=? AND sub_pledge_id=?");
			statement.setInt(1, getSubPledge(pledgeType).getLeaderId());
			statement.setInt(2, getClanId());
			statement.setInt(3, pledgeType);
			statement.execute();
			statement.close();
			if (Config.DEBUG)
				_log.fine("New subpledge leader saved in db: " + getClanId());
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error saving new sub clan leader.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	private void restoreRankPrivs()
	{
		Connection con = null;
		try
		{
			// Retrieve all skills of this L2PcInstance from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT privs,rank,party FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, getClanId());
			// _log.warning("clanPrivs restore for ClanId : "+getClanId());
			ResultSet rset = statement.executeQuery();
			// Go though the recordset of this SQL query
			while (rset.next())
			{
				int rank = rset.getInt("rank");
				// int party = rset.getInt("party");
				int privileges = rset.getInt("privs");
				// Create a SubPledge object for each record
				if (rank == -1)
					continue;
				_privs.get(rank).setPrivs(privileges);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error restoring clan privs by rank.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	public void initializePrivs()
	{
		RankPrivs privs;
		for (int i = 1; i < 10; i++)
		{
			privs = new RankPrivs(i, 0, CP_NOTHING);
			_privs.put(i, privs);
		}
	}
	
	public int getRankPrivs(int rank)
	{
		if (_privs.get(rank) != null)
			return _privs.get(rank).getPrivs();
		else
			return CP_NOTHING;
	}
	
	public void setRankPrivs(int rank, int privs)
	{
		if (_privs.get(rank) != null)
		{
			_privs.get(rank).setPrivs(privs);
			Connection con = null;
			try
			{
				// _log.warning("requested store clan privs in db for rank: "+rank+", privs: "+privs);
				// Retrieve all skills of this L2PcInstance from the database
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,party,privs) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE privs = ?");
				statement.setInt(1, getClanId());
				statement.setInt(2, rank);
				statement.setInt(3, 0);
				statement.setInt(4, privs);
				statement.setInt(5, privs);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Could not store clan privs for rank: " + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
			for (L2ClanMember cm : getMembers())
			{
				if (cm.isOnline())
					if (cm.getPowerGrade() == rank)
						if (cm.getPlayerInstance() != null)
						{
							cm.getPlayerInstance().setClanPrivileges(privs);
							cm.getPlayerInstance().sendPacket(new UserInfo(cm.getPlayerInstance()));
							cm.getPlayerInstance().sendPacket(new ExBrExtraUserInfo(cm.getPlayerInstance()));
						}
			}
			broadcastClanStatus();
		}
		else
		{
			_privs.put(rank, new RankPrivs(rank, 0, privs));
			Connection con = null;
			try
			{
				// _log.warning("requested store clan new privs in db for rank: "+rank);
				// Retrieve all skills of this L2PcInstance from the database
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,party,privs) VALUES (?,?,?,?)");
				statement.setInt(1, getClanId());
				statement.setInt(2, rank);
				statement.setInt(3, 0);
				statement.setInt(4, privs);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Could not create new rank and store clan privs for rank: " + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
		}
	}
	
	/** used to retrieve all RankPrivs */
	public final RankPrivs[] getAllRankPrivs()
	{
		if (_privs == null)
			return new RankPrivs[0];
		return _privs.values().toArray(new RankPrivs[_privs.values().size()]);
	}
	
	public int getLeaderSubPledge(int leaderId)
	{
		int id = 0;
		for (SubPledge sp : _subPledges.values())
		{
			if (sp.getLeaderId() == 0)
				continue;
			if (sp.getLeaderId() == leaderId)
				id = sp.getId();
		}
		return id;
	}
	
	public void setReputationScore(int value, boolean save)
	{
		/*
		 * if(_reputationScore >= 0 && value < 0)
		 * {
		 * broadcastToOnlineMembers(new SystemMessage(SystemMessageId.REPUTATION_POINTS_0_OR_LOWER_CLAN_SKILLS_DEACTIVATED));
		 * L2Skill[] skills = getAllSkills();
		 * for (L2ClanMember member : _members.values())
		 * {
		 * if (member.isOnline() && member.getPlayerInstance() != null)
		 * {
		 * for (L2Skill sk : skills)
		 * member.getPlayerInstance().removeSkill(sk, false);
		 * }
		 * }
		 * }
		 * else if(_reputationScore < 0 && value >= 0)
		 * {
		 * broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_SKILLS_WILL_BE_ACTIVATED_SINCE_REPUTATION_IS_0_OR_HIGHER));
		 * L2Skill[] skills = getAllSkills();
		 * for (L2ClanMember member : _members.values())
		 * {
		 * if (member.isOnline() && member.getPlayerInstance() != null)
		 * {
		 * for (L2Skill sk : skills)
		 * {
		 * if(sk.getMinPledgeClass() <= member.getPlayerInstance().getPledgeClass())
		 * member.getPlayerInstance().addSkill(sk, false);
		 * }
		 * }
		 * }
		 * }
		 */
		_reputationScore = value;
		if (_reputationScore > 100000000)
			_reputationScore = 100000000;
		if (_reputationScore < -100000000)
			_reputationScore = -100000000;
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
		if (save)
			updateClanInDB();
	}
	
	public int getReputationScore()
	{
		return _reputationScore;
	}
	
	public void setPvpKills(int value, boolean save)
	{
		_pvpKills = value;
		if (save)
			updateClanInDB();
	}
	
	public int getPvpKills()
	{
		return _pvpKills;
	}
	
	public void setRank(int rank)
	{
		_rank = rank;
	}
	
	public int getRank()
	{
		return _rank;
	}
	
	public int getAuctionBiddedAt()
	{
		return _auctionBiddedAt;
	}
	
	public void setAuctionBiddedAt(int id, boolean storeInDb)
	{
		_auctionBiddedAt = id;
		if (storeInDb)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET auction_bid_at=? WHERE clan_id=?");
				statement.setInt(1, id);
				statement.setInt(2, getClanId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Could not store auction for clan: " + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
		}
	}
	
	/**
	 * Checks if activeChar and target meet various conditions to join a clan
	 *
	 * @param activeChar
	 * @param target
	 * @param pledgeType
	 * @return
	 */
	public boolean checkClanJoinCondition(L2PcInstance activeChar, L2PcInstance target, int pledgeType)
	{
		if (activeChar == null)
		{
			return false;
		}
		if ((activeChar.getClanPrivileges() & L2Clan.CP_CL_JOIN_CLAN) != L2Clan.CP_CL_JOIN_CLAN)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return false;
		}
		if (target == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET));
			return false;
		}
		if (activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_INVITE_YOURSELF));
			return false;
		}
		if (getCharPenaltyExpiryTime() > System.currentTimeMillis())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_MUST_WAIT_BEFORE_ACCEPTING_A_NEW_MEMBER);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		if (target.getClanId() != 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_WORKING_WITH_ANOTHER_CLAN);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		if (target.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		if ((target.getLevel() > 40 || target.getClassId().level() >= 2) && pledgeType == -1)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOESNOT_MEET_REQUIREMENTS_TO_JOIN_ACADEMY);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			activeChar.sendPacket(new SystemMessage(SystemMessageId.ACADEMY_REQUIREMENTS));
			return false;
		}
		if (getSubPledgeMembersCount(pledgeType) >= getMaxNrOfMembers(pledgeType))
		{
			if (pledgeType == 0)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CLAN_IS_FULL);
				sm.addString(getName());
				activeChar.sendPacket(sm);
				sm = null;
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.SUBCLAN_IS_FULL));
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Checks if activeChar and target meet various conditions to join a clan
	 *
	 * @param activeChar
	 * @param target
	 * @return
	 */
	public boolean checkAllyJoinCondition(L2PcInstance activeChar, L2PcInstance target)
	{
		if (activeChar == null)
		{
			return false;
		}
		if (activeChar.getAllyId() == 0 || !activeChar.isClanLeader() || activeChar.getClanId() != activeChar.getAllyId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER));
			return false;
		}
		L2Clan leaderClan = activeChar.getClan();
		if (leaderClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (leaderClan.getAllyPenaltyType() == PENALTY_TYPE_DISMISS_CLAN)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_INVITE_CLAN_WITHIN_1_DAY));
				return false;
			}
		}
		if (target == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET));
			return false;
		}
		if (activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_INVITE_YOURSELF));
			return false;
		}
		if (target.getClan() == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_MUST_BE_IN_CLAN));
			return false;
		}
		if (!target.isClanLeader())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		L2Clan targetClan = target.getClan();
		if (target.getAllyId() != 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CLAN_ALREADY_MEMBER_OF_S2_ALLIANCE);
			sm.addString(targetClan.getName());
			sm.addString(targetClan.getAllyName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		if (targetClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_LEAVED)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANT_ENTER_ALLIANCE_WITHIN_1_DAY);
				sm.addString(target.getClan().getName());
				sm.addString(target.getClan().getAllyName());
				activeChar.sendPacket(sm);
				sm = null;
				return false;
			}
			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_DISMISSED)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_ENTER_ALLIANCE_WITHIN_1_DAY));
				return false;
			}
		}
		if (activeChar.isInsideZone(L2Character.ZONE_SIEGE) && target.isInsideZone(L2Character.ZONE_SIEGE))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.OPPOSING_CLAN_IS_PARTICIPATING_IN_SIEGE));
			return false;
		}
		if (leaderClan.isAtWarWith(targetClan.getClanId()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.MAY_NOT_ALLY_CLAN_BATTLE));
			return false;
		}
		int numOfClansInAlly = 0;
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == activeChar.getAllyId())
			{
				++numOfClansInAlly;
			}
		}
		if (numOfClansInAlly >= Config.ALT_MAX_NUM_OF_CLANS_IN_ALLY)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_THE_LIMIT));
			return false;
		}
		return true;
	}
	
	public long getAllyPenaltyExpiryTime()
	{
		return _allyPenaltyExpiryTime;
	}
	
	public int getAllyPenaltyType()
	{
		return _allyPenaltyType;
	}
	
	public void setAllyPenaltyExpiryTime(long expiryTime, int penaltyType)
	{
		_allyPenaltyExpiryTime = expiryTime;
		_allyPenaltyType = penaltyType;
	}
	
	public long getCharPenaltyExpiryTime()
	{
		return _charPenaltyExpiryTime;
	}
	
	public void setCharPenaltyExpiryTime(long time)
	{
		_charPenaltyExpiryTime = time;
	}
	
	public long getDissolvingExpiryTime()
	{
		return _dissolvingExpiryTime;
	}
	
	public void setDissolvingExpiryTime(long time)
	{
		_dissolvingExpiryTime = time;
	}
	
	public void createAlly(L2PcInstance player, String allyName)
	{
		if (null == player)
			return;
		if (Config.DEBUG)
			_log.fine(player.getObjectId() + "(" + player.getName() + ") requested ally creation from ");
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE));
			return;
		}
		if (getAllyId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_JOINED_ALLIANCE));
			return;
		}
		if (getLevel() < 5)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TO_CREATE_AN_ALLY_YOU_CLAN_MUST_BE_LEVEL_5_OR_HIGHER));
			return;
		}
		if (getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (getAllyPenaltyType() == L2Clan.PENALTY_TYPE_DISSOLVE_ALLY)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CANT_CREATE_ALLIANCE_10_DAYS_DISOLUTION));
				return;
			}
		}
		if (getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_CREATE_ALLY_WHILE_DISSOLVING));
			return;
		}
		if (!Util.isAlphaNumeric(allyName))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_ALLIANCE_NAME));
			return;
		}
		if (allyName.length() > 16 || allyName.length() < 2)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_ALLIANCE_NAME_LENGTH));
			return;
		}
		if (ClanTable.getInstance().isAllyExists(allyName))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALLIANCE_ALREADY_EXISTS));
			return;
		}
		setAllyId(getClanId());
		setAllyName(allyName.trim());
		setAllyPenaltyExpiryTime(0, 0);
		updateClanInDB();
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new ExBrExtraUserInfo(player));
		// TODO: Need correct message id
		player.sendMessage("Alliance " + allyName + " has been created.");
	}
	
	public void dissolveAlly(L2PcInstance player)
	{
		if (getAllyId() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_CURRENT_ALLIANCES));
			return;
		}
		if (!player.isClanLeader() || getClanId() != getAllyId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER));
			return;
		}
		if (player.isInsideZone(L2Character.ZONE_SIEGE))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_ALLY_WHILE_IN_SIEGE));
			return;
		}
		broadcastToOnlineAllyMembers(new SystemMessage(SystemMessageId.ALLIANCE_DISOLVED));
		long currentTime = System.currentTimeMillis();
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == getAllyId() && clan.getClanId() != getClanId())
			{
				clan.setAllyId(0);
				clan.setAllyName(null);
				clan.setAllyCrestId(0);
				clan.setAllyPenaltyExpiryTime(0, 0);
				clan.updateClanInDB();
			}
		}
		setAllyId(0);
		setAllyName(null);
		setAllyCrestId(0);
		setAllyPenaltyExpiryTime(currentTime + Config.ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED * 86400000L, L2Clan.PENALTY_TYPE_DISSOLVE_ALLY); // 24*60*60*1000 = 86400000
		updateClanInDB();
		// The clan leader should take the XP penalty of a full death.
		player.deathPenalty(false, false, false);
	}
	
	public boolean levelUpClan(L2PcInstance player)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return false;
		}
		if (System.currentTimeMillis() < getDissolvingExpiryTime())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_RISE_LEVEL_WHILE_DISSOLUTION_IN_PROGRESS));
			return false;
		}
		boolean increaseClanLevel = false;
		switch (getLevel())
		{
			case 0:
			{
				// Upgrade to 1
				if (player.getSp() >= 20000 && player.getAdena() >= 650000)
				{
					if (player.reduceAdena("ClanLvl", 650000, player.getTarget(), true))
					{
						player.setSp(player.getSp() - 20000);
						SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
						sp.addNumber(20000);
						player.sendPacket(sp);
						sp = null;
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 1:
			{
				// Upgrade to 2
				if (player.getSp() >= 100000 && player.getAdena() >= 2500000)
				{
					if (player.reduceAdena("ClanLvl", 2500000, player.getTarget(), true))
					{
						player.setSp(player.getSp() - 100000);
						SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
						sp.addNumber(100000);
						player.sendPacket(sp);
						sp = null;
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 2:
			{
				// Upgrade to 3
				if (player.getSp() >= 350000 && player.getInventory().getItemByItemId(1419) != null)
				{
					// itemId 1419 == Blood Mark
					if (player.destroyItemByItemId("ClanLvl", 1419, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 350000);
						SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
						sp.addNumber(350000);
						player.sendPacket(sp);
						sp = null;
						SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(1419);
						sm.addItemNumber(1);
						player.sendPacket(sm);
						sm = null;
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 3:
			{
				// Upgrade to 4
				if (player.getSp() >= 1000000 && player.getInventory().getItemByItemId(3874) != null)
				{
					// itemId 3874 == Alliance Manifesto
					if (player.destroyItemByItemId("ClanLvl", 3874, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 1000000);
						SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
						sp.addNumber(1000000);
						player.sendPacket(sp);
						sp = null;
						SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(3874);
						sm.addItemNumber(1);
						player.sendPacket(sm);
						sm = null;
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 4:
			{
				// Upgrade to 5
				if (player.getSp() >= 2500000 && player.getInventory().getItemByItemId(3870) != null)
				{
					// itemId 3870 == Seal of Aspiration
					if (player.destroyItemByItemId("ClanLvl", 3870, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 2500000);
						SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
						sp.addNumber(2500000);
						player.sendPacket(sp);
						sp = null;
						SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(3870);
						sm.addItemNumber(1);
						player.sendPacket(sm);
						sm = null;
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 5:
				// Upgrade to 6
				if (getReputationScore() >= Config.CLAN_LEVEL_6_COST && getMembersCount() >= 30)
				{
					setReputationScore(getReputationScore() - Config.CLAN_LEVEL_6_COST, true);
					SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					cr.addNumber(Config.CLAN_LEVEL_6_COST);
					player.sendPacket(cr);
					cr = null;
					increaseClanLevel = true;
				}
				break;
			case 6:
				// Upgrade to 7
				if (getReputationScore() >= Config.CLAN_LEVEL_7_COST && getMembersCount() >= 40)
				{
					setReputationScore(getReputationScore() - Config.CLAN_LEVEL_7_COST, true);
					SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					cr.addNumber(Config.CLAN_LEVEL_7_COST);
					player.sendPacket(cr);
					cr = null;
					increaseClanLevel = true;
				}
				break;
			case 7:
				// Upgrade to 8
				if (getReputationScore() >= Config.CLAN_LEVEL_8_COST && getMembersCount() >= 40)
				{
					setReputationScore(getReputationScore() - Config.CLAN_LEVEL_8_COST, true);
					SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					cr.addNumber(Config.CLAN_LEVEL_8_COST);
					player.sendPacket(cr);
					cr = null;
					increaseClanLevel = true;
				}
				break;
			case 8:
				// Upgrade to 9
				if (getReputationScore() >= Config.CLAN_LEVEL_9_COST && player.getInventory().getItemByItemId(9910) != null && getMembersCount() >= 50)
				{
					// itemId 9910 == Blood Oath
					if (player.destroyItemByItemId("ClanLvl", 9910, 150, player.getTarget(), false))
					{
						setReputationScore(getReputationScore() - Config.CLAN_LEVEL_9_COST, true);
						SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
						cr.addNumber(Config.CLAN_LEVEL_9_COST);
						player.sendPacket(cr);
						cr = null;
						SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(9910);
						sm.addItemNumber(150);
						player.sendPacket(sm);
						increaseClanLevel = true;
					}
				}
				break;
			case 9:
				// Upgrade to 10
				if (getReputationScore() >= Config.CLAN_LEVEL_10_COST && player.getInventory().getItemByItemId(9911) != null && getMembersCount() >= 50)
				{
					// itemId 9911 == Blood Alliance
					if (player.destroyItemByItemId("ClanLvl", 9911, 5, player.getTarget(), false))
					{
						setReputationScore(getReputationScore() - Config.CLAN_LEVEL_10_COST, true);
						SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
						cr.addNumber(Config.CLAN_LEVEL_10_COST);
						player.sendPacket(cr);
						cr = null;
						SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(9911);
						sm.addItemNumber(5);
						player.sendPacket(sm);
						increaseClanLevel = true;
					}
				}
				break;
			default:
				return false;
		}
		if (!increaseClanLevel)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.FAILED_TO_INCREASE_CLAN_LEVEL);
			player.sendPacket(sm);
			return false;
		}
		// the player should know that he has less sp now :p
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.SP, player.getSp());
		player.sendPacket(su);
		ItemList il = new ItemList(player, false);
		player.sendPacket(il);
		changeLevel(getLevel() + 1);
		return true;
	}
	
	public void changeLevel(int level)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET clan_level = ? WHERE clan_id = ?");
			statement.setInt(1, level);
			statement.setInt(2, getClanId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not increase clan level:" + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		setLevel(level);
		if (getLeader().isOnline())
		{
			L2PcInstance leader = getLeader().getPlayerInstance();
			if (4 < level)
			{
				SiegeManager.getInstance().addSiegeSkills(leader);
			}
			else if (5 > level)
			{
				SiegeManager.getInstance().removeSiegeSkills(leader);
			}
			if (4 < level)
			{
				leader.sendPacket(new SystemMessage(SystemMessageId.CLAN_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS));
			}
		}
		// notify all the members about it
		broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_LEVEL_INCREASED));
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
		/*
		 * Micht :
		 * - use PledgeShowInfoUpdate instead of PledgeStatusChanged
		 * to update clan level ingame
		 * - remove broadcastClanStatus() to avoid members duplication
		 */
		// clan.broadcastToOnlineMembers(new PledgeStatusChanged(clan));
		// clan.broadcastClanStatus();
	}
	
	public void broadcastToOtherOnlineMembers(String string, L2PcInstance player)
	{
		for (L2ClanMember member : _members.values())
		{
			if (member.isOnline() && member.getPlayerInstance() != null && member.getPlayerInstance() != player)
				member.getPlayerInstance().sendMessage(string);
		}
	}
	
	public void broadcastToOnlineMembers(String string)
	{
		for (L2ClanMember member : _members.values())
		{
			if (member.isOnline() && member.getPlayerInstance() != null)
				member.getPlayerInstance().sendMessage(string);
		}
	}
	
	public boolean canHasCastleDueToTwoCastlePerAllyLimit()
	{
		if (getAllyId() <= 0)
			return true;
		int count = 0;
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan != null && clan.getAllyId() == getAllyId() && clan.getClanId() != getClanId())
			{
				if (clan.getHasCastle() > 0)
					count++;
				if (count >= 2)
					return false;
			}
		}
		return true;
	}
	
	public void incRBkills()
	{
		_rbKills++;
		if (_rbKills >= 8)
			broadcastToOnlineMembers("Your clan has killed 8 raidbosses in a day and cannot kill anymore for today");
	}
	
	public int getRBkills()
	{
		return _rbKills;
	}
	/*
	 * ============================== Clan recruiting members ==============================
	 */
	
	private boolean						_recruiting			= false;
	private final List<Integer>			_classesForRecruit	= new ArrayList<>();
	private String[]					_questions			= new String[8];
	private final List<SinglePetition>	_petitions			= new ArrayList<>();
	
	private void restoreClanRecruitment()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement statement = con.prepareStatement("SELECT * FROM clan_recruitments where clan_id=" + getClanId()); ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					_recruiting = (rset.getInt("recruting") == 1 ? true : false);
					for (String classId : rset.getString("classes").split(","))
					{
						if (classId.length() > 0)
						{
							_classesForRecruit.add(Integer.parseInt(classId));
						}
					}
					for (int i = 1; i <= 8; i++)
					{
						_questions[(i - 1)] = rset.getString("question" + i);
					}
				}
			}
			try (PreparedStatement statement = con.prepareStatement("SELECT * FROM clan_petitions where clan_id=" + getClanId()); ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					String[] answers = new String[8];
					for (int i = 1; i <= 8; i++)
					{
						answers[(i - 1)] = rset.getString("answer" + i);
					}
					_petitions.add(new SinglePetition(rset.getInt("sender_id"), answers, rset.getString("comment")));
				}
			}
		}
		catch (NumberFormatException | SQLException e)
		{
			_log.log(Level.WARNING, "Error while restoring Clan Recruitment", e);
		}
	}
	
	public void updateRecrutationData()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement statement = con.prepareStatement("INSERT INTO clan_recruitments VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE recruting = ?, classes = ?, question1 = ?,question2 = ?,question3 = ?,question4 = ?,question5 = ?,question6 = ?,question7 = ?,question8 = ?"))
			{
				statement.setInt(1, getClanId());
				statement.setInt(2, (_recruiting ? 1 : 0));
				statement.setString(3, getClassesNeeded().stream().map(String::valueOf).collect(Collectors.joining(",")));
				statement.setString(4, _questions[0] == null ? "" : _questions[0]);
				statement.setString(5, _questions[1] == null ? "" : _questions[1]);
				statement.setString(6, _questions[2] == null ? "" : _questions[2]);
				statement.setString(7, _questions[3] == null ? "" : _questions[3]);
				statement.setString(8, _questions[4] == null ? "" : _questions[4]);
				statement.setString(9, _questions[5] == null ? "" : _questions[5]);
				statement.setString(10, _questions[6] == null ? "" : _questions[6]);
				statement.setString(11, _questions[7] == null ? "" : _questions[7]);
				statement.setInt(12, (_recruiting ? 1 : 0));
				statement.setString(13, getClassesNeeded().stream().map(String::valueOf).collect(Collectors.joining(",")));
				statement.setString(14, _questions[0] == null ? "" : _questions[0]);
				statement.setString(15, _questions[1] == null ? "" : _questions[1]);
				statement.setString(16, _questions[2] == null ? "" : _questions[2]);
				statement.setString(17, _questions[3] == null ? "" : _questions[3]);
				statement.setString(18, _questions[4] == null ? "" : _questions[4]);
				statement.setString(19, _questions[5] == null ? "" : _questions[5]);
				statement.setString(20, _questions[6] == null ? "" : _questions[6]);
				statement.setString(21, _questions[7] == null ? "" : _questions[7]);
				statement.execute();
			}
			try (PreparedStatement statement = con.prepareStatement("DELETE FROM clan_petitions WHERE clan_id=?"))
			{
				statement.setInt(1, getClanId());
				statement.execute();
			}
			for (SinglePetition petition : getPetitions())
			{
				try (PreparedStatement statement = con.prepareStatement("INSERT IGNORE INTO clan_petitions VALUES(?,?,?,?,?,?,?,?,?,?,?)"))
				{
					statement.setInt(1, petition.getSenderId());
					statement.setInt(2, getClanId());
					for (int i = 0; i < 8; i++)
					{
						statement.setString(i + 3, petition.getAnswers()[i] == null ? "" : petition.getAnswers()[i]);
					}
					statement.setString(11, petition.getComment());
					statement.execute();
				}
			}
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Error while updating clan recruitment system on clan id '" + _clanId + "' in db", e);
		}
	}
	
	public void setQuestions(String[] questions)
	{
		_questions = questions;
	}
	
	public class SinglePetition
	{
		int			_sender;
		String[]	_answers;
		String		_comment;
		
		SinglePetition(int sender, String[] answers, String comment)
		{
			_sender = sender;
			_answers = answers;
			_comment = comment;
		}
		
		public int getSenderId()
		{
			return _sender;
		}
		
		public String[] getAnswers()
		{
			return _answers;
		}
		
		public String getComment()
		{
			return _comment;
		}
	}
	
	public synchronized boolean addPetition(int senderId, String[] answers, String comment)
	{
		if (getPetition(senderId) != null)
		{
			return false;
		}
		_petitions.add(new SinglePetition(senderId, answers, comment));
		updateRecrutationData();
		L2PcInstance player = L2World.getInstance().getPlayer(getLeaderId());
		if (player != null)
		{
			player.sendMessage("New Clan Petition has arrived!");
		}
		return true;
	}
	
	public SinglePetition getPetition(int senderId)
	{
		return _petitions.stream().filter(petition -> petition.getSenderId() == senderId).findAny().orElse(null);
	}
	
	public List<SinglePetition> getPetitions()
	{
		return _petitions;
	}
	
	public synchronized void deletePetition(int senderId)
	{
		SinglePetition petition = _petitions.stream().filter(p -> p.getSenderId() == senderId).findAny().orElse(null);
		if (petition != null)
		{
			_petitions.remove(petition);
			updateRecrutationData();
		}
	}
	
	public void deletePetition(SinglePetition petition)
	{
		_petitions.remove(petition);
		updateRecrutationData();
	}
	
	public void setRecruiting(boolean b)
	{
		_recruiting = b;
	}
	
	public boolean isRecruiting()
	{
		return _recruiting;
	}
	
	public void addClassNeeded(int clas)
	{
		_classesForRecruit.add(clas);
	}
	
	public void deleteClassNeeded(int clas) // iti arat cum o generez? neahm, ca cred ca stiu care-i baiu :)
	{
		int indexOfClass = _classesForRecruit.indexOf(clas);
		if (indexOfClass != -1)
		{
			_classesForRecruit.remove(indexOfClass);
		}
		else
		{
			_log.log(Level.WARNING, "Tried removing inexistent class: " + clas);
		}
	}
	
	public List<Integer> getClassesNeeded()
	{
		return _classesForRecruit;
	}
	
	public String[] getQuestions()
	{
		return _questions;
	}
	
	/**
	 * @param subPledgeId
	 *            the id of the subpledge or 0 for main clan.
	 * @return if all available spaces in the subpledge are full.
	 */
	public boolean isFull(int subPledgeId)
	{
		return getSubPledgeMembersCount(subPledgeId) >= getMaxNrOfMembers(subPledgeId);
	}
	
	/**
	 * @return if all available spaces in the main clan or the subpledges are full.
	 */
	public boolean isFull()
	{
		if (getSubPledgeMembersCount(SUBUNIT_MAIN_CLAN) < getMaxNrOfMembers(SUBUNIT_MAIN_CLAN))
		{
			return false;
		}
		for (SubPledge sp : getAllSubPledges())
		{
			if (getSubPledgeMembersCount(sp.getId()) < getMaxNrOfMembers(sp.getId()))
			{
				return false;
			}
		}
		return true;
	}

	public synchronized void addReputationScore(final int value)
	{
		setReputationScore(getReputationScore() + value, true);
	}
	/**
	 * @return the online clan member count.
	 */
	public int getOnlineCount()
	{
		int count = 0;
		for (final L2ClanMember temp : _members.values())
		{
			if (temp == null || !temp.isOnline())
				continue;
			count++;
		}
		return count;
	}
	
	public int getOnlineCount(int level)
	{
		int count = 0;
		for (final L2ClanMember temp : _members.values())
		{
			if (temp == null || !temp.isOnline() || temp.getLevel() < level)
				continue;
			count++;
		}
		return count;
	}
}