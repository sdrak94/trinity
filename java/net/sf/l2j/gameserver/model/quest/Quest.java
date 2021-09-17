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
package net.sf.l2j.gameserver.model.quest;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcQuestHtmlMessage;
import net.sf.l2j.gameserver.scripting.ManagedScript;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.GMAudit;
import net.sf.l2j.util.Rnd;

/**
 * @author Luis Arias
 */
public class Quest extends ManagedScript
{
	protected static final Logger					_log						= Logger.getLogger(Quest.class.getName());
	private static final String						LOAD_QUEST_STATES			= "SELECT name,value FROM character_quests WHERE charId=? AND var='<state>'";
	private static final String						LOAD_QUEST_VARIABLES		= "SELECT name,var,value FROM character_quests WHERE charId=? AND var<>'<state>'";
	private static final String						DELETE_INVALID_QUEST		= "SELECT FROM character_quests WHERE name=?";
	private static final String						SET_GLOBAL_QUEST_VAL		= "REPLACE INTO quest_global_data (quest_name,var,value) VALUES (?,?,?)";
	private static final String						GET_GLOBAL_QUEST_VAL		= "SELECT value FROM quest_global_data WHERE quest_name=? AND var=?";
	private static final String						DEL_GLOBAL_QUEST_VAL		= "DELETE FROM quest_global_data WHERE quest_name=? AND var=?";
	private static final String						DEL_ALL_GLOBAL_QUEST_VAL	= "DELETE FROM quest_global_data WHERE quest_name=?";
	private static final String						HTML_NONE_AVAILABLE			= "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	private static final String						HTML_ALREADY_COMPLETED		= "<html><body>This quest has already been completed.</body></html>";
	public static final byte						STATE_CREATED				= 0;
	public static final byte						STATE_STARTED				= 1;
	public static final byte						STATE_COMPLETED				= 2;
	private final Map<Integer, List<QuestTimer>>	_eventTimers				= new ConcurrentHashMap<>();
	private final int								_id;
	private final String							_name;
	private final String							_descr;
	private boolean									_onEnterWorld;
	protected int[]									questItemIds				= null;
	
	public static enum QuestEventType
	{
		ON_FIRST_TALK(false), // control the first dialog shown by NPCs when they are clicked (some quests must override the default npc action)
		QUEST_START(true), // onTalk action from start npcs
		ON_TALK(true), // onTalk action from npcs participating in a quest
		ON_ATTACK(true), // onAttack action triggered when a mob gets attacked by someone
		ON_KILL(true), // onKill action triggered when a mob gets killed.
		ON_SPAWN(true), // onSpawn action triggered when an NPC is spawned or respawned.
		ON_SKILL_SEE(true), // NPC or Mob saw a person casting a skill (regardless what the target is).
		ON_FACTION_CALL(true), // NPC or Mob saw a person casting a skill (regardless what the target is).
		ON_AGGRO_RANGE_ENTER(true), // a person came within the Npc/Mob's range
		ON_SPELL_FINISHED(true), // on spell finished action when npc finish casting skill
		ON_SKILL_LEARN(false), // control the AcquireSkill dialog from quest script
		ON_ENTER_ZONE(true), // on zone enter
		ON_EXIT_ZONE(true); // on zone exit
		
		// control whether this event type is allowed for the same npc template in multiple quests
		// or if the npc must be registered in at most one quest for the specified event
		private boolean _allowMultipleRegistration;
		
		QuestEventType(boolean allowMultipleRegistration)
		{
			_allowMultipleRegistration = allowMultipleRegistration;
		}
		
		public boolean isMultipleRegistrationAllowed()
		{
			return _allowMultipleRegistration;
		}
	}
	
	/**
	 * (Constructor)Add values to class variables and put the quest in HashMaps.
	 * 
	 * @param questId
	 *            : int pointing out the ID of the quest
	 * @param name
	 *            : String corresponding to the name of the quest
	 * @param descr
	 *            : String for the description of the quest
	 */
	public Quest(final int questId, final String name, final String descr)
	{
		_id = questId;
		_name = name;
		_descr = descr;
		_onEnterWorld = false;
	}
	
	/**
	 * Return ID of the quest.
	 * 
	 * @return int
	 */
	public int getQuestId()
	{
		return _id;
	}
	
	/**
	 * Return type of the quest.
	 * 
	 * @return boolean : True for (live) quest, False for script, AI, etc.
	 */
	public boolean isRealQuest()
	{
		return _id > 0;
	}
	
	/**
	 * Return name of the quest.
	 * 
	 * @return String
	 */
	@Override
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Return description of the quest.
	 * 
	 * @return String
	 */
	public String getDescr()
	{
		return _descr;
	}
	
	public void setOnEnterWorld(final boolean val)
	{
		_onEnterWorld = val;
	}
	
	public boolean getOnEnterWorld()
	{
		return _onEnterWorld;
	}
	
	/**
	 * Return registered quest items.
	 * 
	 * @return int[]
	 */
	int[] getRegisteredItemIds()
	{
		return questItemIds;
	}
	
	/**
	 * Add a new QuestState to the database and return it.
	 * 
	 * @param player
	 * @return QuestState : QuestState created
	 */
	public QuestState newQuestState(final L2PcInstance player)
	{
		return new QuestState(player, this, STATE_CREATED);
	}
	
	/**
	 * Add quests to the L2PCInstance of the player.<BR>
	 * <BR>
	 * <U><I>Action : </U></I><BR>
	 * Add state of quests, drops and variables for quests in the HashMap _quest of L2PcInstance
	 * 
	 * @param player
	 *            : Player who is entering the world
	 */
	public final static void playerEnter(L2PcInstance player)
	{
		Connection con = null;
		try
		{
			// Get list of quests owned by the player from database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			PreparedStatement invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE charId=? and name=?");
			PreparedStatement invalidQuestDataVar = con.prepareStatement("delete FROM character_quests WHERE charId=? and name=? and var=?");
			statement = con.prepareStatement("SELECT name,value FROM character_quests WHERE charId=? AND var=?");
			statement.setInt(1, player.getObjectId());
			statement.setString(2, "<state>");
			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				// Get ID of the quest and ID of its state
				String questId = rs.getString("name");
				String statename = rs.getString("value");
				// Search quest associated with the ID
				Quest q = QuestManager.getInstance().getQuest(questId);
				if (q == null)
				{
					_log.finer("Unknown quest " + questId + " for player " + player.getName());
					if (Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestData.setInt(1, player.getObjectId());
						invalidQuestData.setString(2, questId);
						invalidQuestData.executeUpdate();
					}
					continue;
				}
				// Create a new QuestState for the player that will be added to the player's list of quests
				new QuestState(player, q, State.getStateId(statename));
			}
			rs.close();
			invalidQuestData.close();
			statement.close();
			// Get list of quests owned by the player from the DB in order to add variables used in the quest.
			statement = con.prepareStatement("SELECT name,var,value FROM character_quests WHERE charId=? AND var<>?");
			statement.setInt(1, player.getObjectId());
			statement.setString(2, "<state>");
			rs = statement.executeQuery();
			while (rs.next())
			{
				String questId = rs.getString("name");
				String var = rs.getString("var");
				String value = rs.getString("value");
				// Get the QuestState saved in the loop before
				QuestState qs = player.getQuestState(questId);
				if (qs == null)
				{
					_log.finer("Lost variable " + var + " in quest " + questId + " for player " + player.getName());
					if (Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestDataVar.setInt(1, player.getObjectId());
						invalidQuestDataVar.setString(2, questId);
						invalidQuestDataVar.setString(3, var);
						invalidQuestDataVar.executeUpdate();
					}
					continue;
				}
				// Add parameter to the quest
				qs.setInternal(var, value);
			}
			rs.close();
			invalidQuestDataVar.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
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
	
	/**
	 * Insert (or Update) in the database variables that need to stay persistant for this quest after a reboot. This function is for storage of values that do not related to a specific player but are global for all characters. For example, if we need to disable a quest-gatekeeper until a certain
	 * time (as is done with some grand-boss gatekeepers), we can save that time in the DB.
	 * 
	 * @param var
	 *            : String designating the name of the variable for the quest
	 * @param value
	 *            : String designating the value of the variable for the quest
	 */
	public final void setGlobalQuestVar(final String var, final String value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(SET_GLOBAL_QUEST_VAL);
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.setString(3, value);
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "could not set global quest variable:", e);
		}
	}
	
	/**
	 * Read from the database a previously saved variable for this quest. Due to performance considerations, this function should best be used only when the quest is first loaded. Subclasses of this class can define structures into which these loaded values can be saved. However, on-demand usage of
	 * this function throughout the script is not prohibited, only not recommended. Values read from this function were entered by calls to "saveGlobalQuestVar"
	 * 
	 * @param var
	 *            : String designating the name of the variable for the quest
	 * @return String : String representing the loaded value for the passed var, or an empty string if the var was invalid
	 */
	public final String getGlobalQuestVar(final String var)
	{
		String result = "";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(GET_GLOBAL_QUEST_VAL);
			statement.setString(1, getName());
			statement.setString(2, var);
			final ResultSet rs = statement.executeQuery();
			if (rs.first())
				result = rs.getString(1);
			rs.close();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "could not load global quest variable:", e);
		}
		return result;
	}
	
	/**
	 * Permanently delete from the database a global quest variable that was previously saved for this quest.
	 * 
	 * @param var
	 *            : String designating the name of the variable for the quest
	 */
	public final void deleteGlobalQuestVar(final String var)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(DEL_GLOBAL_QUEST_VAL);
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "could not delete global quest variable:", e);
		}
	}
	
	/**
	 * Permanently delete from the database all global quest variables that was previously saved for this quest.
	 */
	public final void deleteAllGlobalQuestVars()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(DEL_ALL_GLOBAL_QUEST_VAL);
			statement.setString(1, getName());
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "could not delete all global quest variables:", e);
		}
	}
	
	/**
	 * @param player
	 *            : The player to make checks on.
	 * @param object
	 *            : to take range reference from
	 * @return A random party member or the passed player if he has no party.
	 */
	public L2PcInstance getRandomPartyMember(final L2PcInstance player, final L2Object object)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
			return null;
		// No party or no object, return player.
		if (object == null || !player.isInParty())
			return player;
		// Player's party.
		final List<L2PcInstance> members = new ArrayList<>();
		for (final L2PcInstance member : player.getParty().getPartyMembers())
			if (member.isInsideRadius(object, Config.ALT_PARTY_RANGE, true, false))
				members.add(member);
		// No party members, return. (note: player is party member too, in most
		// cases he is included in members too)
		if (members.isEmpty())
			return null;
		// Random party member.
		return members.get(Rnd.get(members.size()));
	}
	
	/**
	 * Auxiliary function for party quests. Checks the player's condition. Player member must be within Config.ALT_PARTY_RANGE distance from the npc. If npc is null, distance condition is ignored.
	 * 
	 * @param player
	 *            : the instance of a player whose party is to be searched
	 * @param npc
	 *            : the instance of a L2Npc to compare distance
	 * @param var
	 *            : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @param value
	 *            : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @return QuestState : The QuestState of that player.
	 */
	public QuestState checkPlayerCondition(final L2PcInstance player, final L2Npc npc, final String var, final String value)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
			return null;
		// Check player's quest conditions.
		final QuestState st = player.getQuestState(getName());
		if (st == null)
			return null;
		// Condition exists? Condition has correct value?
		if (st.get(var) == null || !value.equalsIgnoreCase(st.get(var)))
			return null;
		// Invalid npc instance?
		if (npc == null)
			return null;
		// Player is in range?
		if (!player.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false))
			return null;
		return st;
	}
	
	/**
	 * Auxiliary function for party quests. Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own
	 * 
	 * @param player
	 *            : the instance of a player whose party is to be searched
	 * @param npc
	 *            : the instance of a L2Npc to compare distance
	 * @param var
	 *            : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @param value
	 *            : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @return List<L2PcInstance> : List of party members that matches the specified condition, empty list if none matches. If the var is null, empty list is returned (i.e. no condition is applied). The party member must be within Config.ALT_PARTY_RANGE distance from the npc. If npc is null,
	 *         distance condition is ignored.
	 */
	public List<L2PcInstance> getPartyMembers(final L2PcInstance player, final L2Npc npc, final String var, final String value)
	{
		// Output list.
		final List<L2PcInstance> candidates = new ArrayList<>();
		// Valid player instance is passed and player is in a party? Check
		// party.
		if (player != null && player.isInParty())
			// Filter candidates from player's party.
			for (final L2PcInstance partyMember : player.getParty().getPartyMembers())
			{
				if (partyMember == null)
					continue;
				// Check party members' quest condition.
				if (checkPlayerCondition(partyMember, npc, var, value) != null)
					candidates.add(partyMember);
			}
		else if (checkPlayerCondition(player, npc, var, value) != null)
			candidates.add(player);
		return candidates;
	}
	
	/**
	 * Auxiliary function for party quests. Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own
	 * 
	 * @param player
	 *            : the instance of a player whose party is to be searched
	 * @param npc
	 *            : the instance of a L2Npc to compare distance
	 * @param var
	 *            : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @param value
	 *            : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @return L2PcInstance : L2PcInstance for a random party member that matches the specified condition, or null if no match. If the var is null, null is returned (i.e. no condition is applied). The party member must be within 1500 distance from the npc. If npc is null, distance condition is
	 *         ignored.
	 */
	public L2PcInstance getRandomPartyMember(final L2PcInstance player, final L2Npc npc, final String var, final String value)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
			return null;
		// Get all candidates fulfilling the condition.
		final List<L2PcInstance> candidates = getPartyMembers(player, npc, var, value);
		// No candidate, return.
		if (candidates.isEmpty())
			return null;
		// Return random candidate.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Auxiliary function for party quests. Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * 
	 * @param player
	 *            : the instance of a player whose party is to be searched
	 * @param npc
	 *            : the instance of a L2Npc to compare distance
	 * @param value
	 *            : the value of the "cond" variable that must be matched
	 * @return L2PcInstance : L2PcInstance for a random party member that matches the specified condition, or null if no match.
	 */
	public L2PcInstance getRandomPartyMember(final L2PcInstance player, final L2Npc npc, final String value)
	{
		return getRandomPartyMember(player, npc, "cond", value);
	}
	
	/**
	 * Auxiliary function for party quests. Checks the player's condition. Player member must be within Config.ALT_PARTY_RANGE distance from the npc. If npc is null, distance condition is ignored.
	 * 
	 * @param player
	 *            : the instance of a player whose party is to be searched
	 * @param npc
	 *            : the instance of a L2Npc to compare distance
	 * @param state
	 *            : the state in which the party member's QuestState must be in order to be considered.
	 * @return QuestState : The QuestState of that player.
	 */
	public QuestState checkPlayerState(final L2PcInstance player, final L2Npc npc, final byte state)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
			return null;
		// Check player's quest conditions.
		final QuestState st = player.getQuestState(getName());
		if (st == null)
			return null;
		// State correct?
		if (st.getState() != state)
			return null;
		// Invalid npc instance?
		if (npc == null)
			return null;
		// Player is in range?
		if (!player.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false))
			return null;
		return st;
	}
	
	/**
	 * Auxiliary function for party quests. Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * 
	 * @param player
	 *            : the instance of a player whose party is to be searched
	 * @param npc
	 *            : the instance of a L2Npc to compare distance
	 * @param state
	 *            : the state in which the party member's QuestState must be in order to be considered.
	 * @return List<L2PcInstance> : List of party members that matches the specified quest state, empty list if none matches. The party member must be within Config.ALT_PARTY_RANGE distance from the npc. If npc is null, distance condition is ignored.
	 */
	public List<L2PcInstance> getPartyMembersState(final L2PcInstance player, final L2Npc npc, final byte state)
	{
		// Output list.
		final List<L2PcInstance> candidates = new ArrayList<>();
		// Valid player instance is passed and player is in a party? Check
		// party.
		if (player != null && player.isInParty())
			// Filter candidates from player's party.
			for (final L2PcInstance partyMember : player.getParty().getPartyMembers())
			{
				if (partyMember == null)
					continue;
				// Check party members' quest state.
				if (checkPlayerState(partyMember, npc, state) != null)
					candidates.add(partyMember);
			}
		else if (checkPlayerState(player, npc, state) != null)
			candidates.add(player);
		return candidates;
	}
	
	/**
	 * Auxiliary function for party quests. Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * 
	 * @param player
	 *            : the instance of a player whose party is to be searched
	 * @param npc
	 *            : the instance of a monster to compare distance
	 * @param state
	 *            : the state in which the party member's QuestState must be in order to be considered.
	 * @return L2PcInstance: L2PcInstance for a random party member that matches the specified condition, or null if no match. If the var is null, any random party member is returned (i.e. no condition is applied).
	 */
	public L2PcInstance getRandomPartyMemberState(final L2PcInstance player, final L2Npc npc, final byte state)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
			return null;
		// Get all candidates fulfilling the condition.
		final List<L2PcInstance> candidates = getPartyMembersState(player, npc, state);
		// No candidate, return.
		if (candidates.isEmpty())
			return null;
		// Return random candidate.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Retrieves the clan leader quest state.
	 * 
	 * @param player
	 *            : the player to test
	 * @param npc
	 *            : the npc to test distance
	 * @return the QuestState of the leader, or null if not found
	 */
	public QuestState getClanLeaderQuestState(final L2PcInstance player, final L2Npc npc)
	{
		// If player is the leader, retrieves directly the qS and bypass others
		// checks
		if (player.isClanLeader() && player.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false))
			return player.getQuestState(getName());
		// Verify if the player got a clan
		final L2Clan clan = player.getClan();
		if (clan == null)
			return null;
		// Verify if the leader is online
		final L2PcInstance leader = clan.getLeader().getPlayerInstance();
		if (leader == null)
			return null;
		// Verify if the player is on the radius of the leader. If true, send
		// leader's quest state.
		if (leader.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false))
			return leader.getQuestState(getName());
		return null;
	}
	
	/**
	 * Add a timer to the quest, if it doesn't exist already. If the timer is repeatable, it will auto-fire automatically, at a fixed rate, until explicitly canceled.
	 * 
	 * @param name
	 *            name of the timer (also passed back as "event" in onAdvEvent)
	 * @param time
	 *            time in ms for when to fire the timer
	 * @param npc
	 *            npc associated with this timer (can be null)
	 * @param player
	 *            player associated with this timer (can be null)
	 * @param repeating
	 *            indicates if the timer is repeatable or one-time.
	 */
	public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player)
	{
		startQuestTimer(name, time, npc, player, false);
	}
	
	public void startQuestTimer(final String name, final long time, final L2Npc npc, final L2PcInstance player, final boolean repeating)
	{
		// Get quest timers for this timer type.
		List<QuestTimer> timers = _eventTimers.get(name.hashCode());
		if (timers == null)
		{
			// None timer exists, create new list.
			timers = new CopyOnWriteArrayList<>();
			// Add new timer to the list.
			timers.add(new QuestTimer(this, name, npc, player, time, repeating));
			// Add timer list to the map.
			_eventTimers.put(name.hashCode(), timers);
		}
		else
		{
			// Check, if specific timer already exists.
			for (final QuestTimer timer : timers)
				// If so, return.
				if (timer != null && timer.equals(this, name, npc, player))
					return;
			// Add new timer to the list.
			timers.add(new QuestTimer(this, name, npc, player, time, repeating));
		}
	}
	
	public QuestTimer getQuestTimer(final String name, final L2Npc npc, final L2PcInstance player)
	{
		// Get quest timers for this timer type.
		final List<QuestTimer> timers = _eventTimers.get(name.hashCode());
		// Timer list does not exists or is empty, return.
		if (timers == null || timers.isEmpty())
			return null;
		// Check, if specific timer exists.
		for (final QuestTimer timer : timers)
			// If so, return him.
			if (timer != null && timer.equals(this, name, npc, player))
				return timer;
		return null;
	}
	
	public void cancelQuestTimer(final String name, final L2Npc npc, final L2PcInstance player)
	{
		// If specified timer exists, cancel him.
		final QuestTimer timer = getQuestTimer(name, npc, player);
		if (timer != null)
			timer.cancel();
	}
	
	public void cancelQuestTimers(final String name)
	{
		// Get quest timers for this timer type.
		final List<QuestTimer> timers = _eventTimers.get(name.hashCode());
		// Timer list does not exists or is empty, return.
		if (timers == null || timers.isEmpty())
			return;
		// Cancel all quest timers.
		for (final QuestTimer timer : timers)
			if (timer != null)
				timer.cancel();
	}
	
	// Note, keep it default. It is used withing QuestTimer, when it terminates.
	/**
	 * Removes QuestTimer from timer list, when it terminates.
	 * 
	 * @param timer
	 *            : QuestTimer, which is beeing terminated.
	 */
	void removeQuestTimer(final QuestTimer timer)
	{
		// Timer does not exist, return.
		if (timer == null)
			return;
		// Get quest timers for this timer type.
		final List<QuestTimer> timers = _eventTimers.get(timer.getName().hashCode());
		// Timer list does not exists or is empty, return.
		if (timers == null || timers.isEmpty())
			return;
		// Remove timer from the list.
		timers.remove(timer);
	}
	
	/**
	 * Add a temporary (quest) spawn on the location of a character.
	 * 
	 * @param npcId
	 *            the NPC template to spawn.
	 * @param cha
	 *            the position where to spawn it.
	 * @param randomOffset
	 * @param despawnDelay
	 * @param isSummonSpawn
	 *            if true, spawn with animation (if any exists).
	 * @return instance of the newly spawned npc with summon animation.
	 */
	public L2Npc addSpawn(final int npcId, final L2Character cha, final boolean randomOffset, final long despawnDelay, final boolean isSummonSpawn)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), randomOffset, despawnDelay, isSummonSpawn);
	}
	
	/**
	 * Add a temporary (quest) spawn on the Location object.
	 * 
	 * @param npcId
	 *            the NPC template to spawn.
	 * @param loc
	 *            the position where to spawn it.
	 * @param randomOffset
	 * @param despawnDelay
	 * @param isSummonSpawn
	 *            if true, spawn with animation (if any exists).
	 * @return instance of the newly spawned npc with summon animation.
	 */
	public L2Npc addSpawn(final int npcId, final Location loc, final boolean randomOffset, final long despawnDelay, final boolean isSummonSpawn)
	{
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, despawnDelay, isSummonSpawn);
	}
	
	/**
	 * Add a temporary (quest) spawn on the location of a character.
	 * 
	 * @param npcId
	 *            the NPC template to spawn.
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param randomOffset
	 * @param despawnDelay
	 * @param isSummonSpawn
	 *            if true, spawn with animation (if any exists).
	 * @return instance of the newly spawned npc with summon animation.
	 */
	public class DeSpawnScheduleTimerTask implements Runnable
	{
		L2Npc _npc = null;
		
		public DeSpawnScheduleTimerTask(L2Npc npc)
		{
			_npc = npc;
		}
		
		public void run()
		{
			_npc.onDecay();
		}
	}
	
	// Method - Public
	/**
	 * Add a temporary (quest) spawn
	 * Return instance of newly spawned npc
	 */
	public L2Npc addSpawn(int npcId, L2Character cha)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0, false);
	}
	
	/**
	 * Add a temporary (quest) spawn
	 * Return instance of newly spawned npc
	 * with summon animation
	 */
	public L2Npc addSpawn(int npcId, L2Character cha, boolean isSummonSpawn)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0, isSummonSpawn);
	}
	
	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffSet, int despawnDelay)
	{
		return addSpawn(npcId, x, y, z, heading, randomOffSet, despawnDelay, false);
	}
	
	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, isSummonSpawn, 0);
	}
	
	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay, boolean isSummonSpawn, int instanceId)
	{
		L2Npc result = null;
		try
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
			if (template != null)
			{
				// Sometimes, even if the quest script specifies some xyz (for example npc.getX() etc) by the time the code
				// reaches here, xyz have become 0! Also, a questdev might have purposely set xy to 0,0...however,
				// the spawn code is coded such that if x=y=0, it looks into location for the spawn loc! This will NOT work
				// with quest spawns! For both of the above cases, we need a fail-safe spawn. For this, we use the
				// default spawn location, which is at the player's loc.
				if ((x == 0) && (y == 0))
				{
					_log.log(Level.SEVERE, "Failed to adjust bad locks for quest spawn!  Spawn aborted!");
					return null;
				}
				if (randomOffset)
				{
					int offset;
					offset = Rnd.get(2); // Get the direction of the offset
					if (offset == 0)
					{
						offset = -1;
					} // make offset negative
					offset *= Rnd.get(70, 120);
					x += offset;
					offset = Rnd.get(2); // Get the direction of the offset
					if (offset == 0)
					{
						offset = -1;
					} // make offset negative
					offset *= Rnd.get(70, 120);
					y += offset;
				}
				L2Spawn spawn = new L2Spawn(template);
				spawn.setInstanceId(instanceId);
				spawn.setHeading(heading);
				spawn.setLocx(x);
				spawn.setLocy(y);
				spawn.setLocz(z + 20);
				spawn.stopRespawn();
				result = spawn.spawnOne(isSummonSpawn);
				if (despawnDelay > 0)
					ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnScheduleTimerTask(result), despawnDelay);
				return result;
			}
			else
			{
				_log.severe("Quest.java addSpawn() called a null NPC to be spawned w/ ID: " + npcId);
			}
		}
		catch (Exception e1)
		{
			_log.warning("Could not spawn Npc " + npcId);
		}
		return null;
	}
	
	public L2Npc addSpawn(final int npcId, int x, int y, final int z, final int heading, final boolean randomOffset, final long despawnDelay, final boolean isSummonSpawn)
	{
		L2Npc result = null;
		try
		{
			final L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
			if (template != null)
			{
				// Sometimes, even if the quest script specifies some xyz (for
				// example npc.getX() etc) by the time the code
				// reaches here, xyz have become 0! Also, a questdev might have
				// purposely set xy to 0,0...however,
				// the spawn code is coded such that if x=y=0, it looks into
				// location for the spawn loc! This will NOT work
				// with quest spawns! For both of the above cases, we need a
				// fail-safe spawn. For this, we use the
				// default spawn location, which is at the player's loc.
				if (x == 0 && y == 0)
				{
					_log.log(Level.SEVERE, "Failed to adjust bad locks for quest spawn!  Spawn aborted!");
					return null;
				}
				if (randomOffset)
				{
					if (Rnd.get(2) == 0)
						x += Rnd.get(50, 100);
					else
						x += Rnd.get(-100, -50);
					if (Rnd.get(2) == 0)
						y += Rnd.get(50, 100);
					else
						y += Rnd.get(-100, -50);
				}
				final L2Spawn spawn = new L2Spawn(template);
				spawn.setHeading(heading);
				spawn.setLocx(x);
				spawn.setLocy(y);
				spawn.setLocz(z + 20);
				spawn.stopRespawn();
				result = spawn.doSpawn(isSummonSpawn);
				if (despawnDelay > 0)
					result.scheduleDespawn(despawnDelay);
				return result;
			}
		}
		catch (final Exception e1)
		{
			_log.warning("Could not spawn Npc " + npcId);
		}
		return null;
	}
	
	/**
	 * @return default html page "You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements."
	 */
	public static String getNoQuestMsg()
	{
		return HTML_NONE_AVAILABLE;
	}
	
	/**
	 * @return default html page "This quest has already been completed."
	 */
	public static String getAlreadyCompletedMsg()
	{
		return HTML_ALREADY_COMPLETED;
	}
	
	/**
	 * Show a message to player.<BR>
	 * <BR>
	 * <U><I>Concept : </I></U><BR>
	 * 3 cases are managed according to the value of the parameter "res" :<BR>
	 * <LI><U>"res" ends with string ".html" :</U> an HTML is opened in order to be shown in a dialog box</LI>
	 * <LI><U>"res" starts with "<html>" :</U> the message hold in "res" is shown in a dialog box</LI>
	 * <LI><U>otherwise :</U> the message held in "res" is shown in chat box</LI>
	 * 
	 * @param npc
	 *            : which launches the dialog, null in case of random scripts
	 * @param player
	 *            : the player.
	 * @param result
	 *            : String pointing out the message to show at the player
	 * @return boolean
	 */
	public boolean showResult(final L2Npc npc, final L2PcInstance player, final String result)
	{
		if (player == null || result == null || result.isEmpty())
			return false;
		if (result.endsWith(".htm") || result.endsWith(".html"))
		{
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc == null ? 0 : npc.getNpcId());
			npcReply.setFile("./data/html/quests/" + getName() + "/" + result);
			if (npc != null)
				npcReply.replace("%objectId%", String.valueOf(npc.getObjectId()));
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (result.startsWith("<html>"))
		{
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc == null ? 0 : npc.getNpcId());
			npcReply.setHtml(result);
			if (npc != null)
				npcReply.replace("%objectId%", String.valueOf(npc.getObjectId()));
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
			player.sendMessage(result);
		return true;
	}
	
	/**
	 * Show message error to player who has an access level greater than 0
	 * 
	 * @param player
	 *            : L2PcInstance
	 * @param e
	 *            : Throwable
	 * @return boolean
	 */
	public boolean showError(final L2PcInstance player, final Throwable e)
	{
		_log.log(Level.WARNING, getScriptFile().toAbsolutePath() + " " + e);
		if (e.getMessage() == null)
			e.printStackTrace();
		if (player != null && player.isGM())
		{
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(0);
			npcReply.setHtml("<html><body><title>Script error</title>" + e.getMessage() + "</body></html>");
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		return false;
	}
	
	/**
	 * Returns String representation of given quest html.
	 * 
	 * @param fileName
	 *            : the filename to send.
	 * @return String : message sent to client.
	 */
	public String getHtmlText(final String fileName)
	{
		return HtmCache.getInstance().getHtmForce("./data/html/quests/" + getName() + "/" + fileName);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for the specified Event type.<BR>
	 * <BR>
	 * 
	 * @param npcId
	 *            : id of the NPC to register
	 * @param eventType
	 *            : type of event being registered
	 * @return L2NpcTemplate : Npc Template corresponding to the npcId, or null if the id is invalid
	 */
	public L2NpcTemplate addEventId(final int npcId, final QuestEventType eventType)
	{
		try
		{
			final L2NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
			if (t != null)
				t.addQuestEvent(eventType, this);
			return t;
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "Exception on addEventId(): " + e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Add the quest to the NPC's startQuest
	 * 
	 * @param npcIds
	 *            A serie of ids.
	 * @return L2NpcTemplate : Start NPC
	 */
	public L2NpcTemplate[] addStartNpc(final int... npcIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (final int npcId : npcIds)
			value[i++] = addEventId(npcId, QuestEventType.QUEST_START);
		return value;
	}
	
	public L2NpcTemplate addStartNpc(final int npcId)
	{
		return addEventId(npcId, QuestEventType.QUEST_START);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for Attack Events.<BR>
	 * <BR>
	 * 
	 * @param npcIds
	 *            A serie of ids.
	 * @return int : attackId
	 */
	public L2NpcTemplate[] addAttackId(final int... npcIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (final int npcId : npcIds)
			value[i++] = addEventId(npcId, QuestEventType.ON_ATTACK);
		return value;
	}
	
	public L2NpcTemplate addAttackId(final int attackId)
	{
		return addEventId(attackId, QuestEventType.ON_ATTACK);
	}
	
	/**
	 * Quest event notifycator for player's or player's pet attack.
	 * 
	 * @param npc
	 *            Attacked npc instace.
	 * @param attacker
	 *            Attacker or pet owner.
	 * @param damage
	 *            Given damage.
	 * @param isPet
	 *            Player summon attacked?
	 * @param skill
	 * @return boolean
	 */
	public final boolean notifyAttack(final L2Npc npc, final L2PcInstance attacker, final int damage, final boolean isPet, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onAttack(npc, attacker, damage, isPet, skill);
		}
		catch (final Exception e)
		{
			return showError(attacker, e);
		}
		return showResult(npc, attacker, res);
	}
	
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		return null;
	}

	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		return onAttack(npc, attacker, damage, isPet);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for AttackAct Events.<BR>
	 * <BR>
	 * 
	 * @param npcIds
	 *            A serie of ids.
	 * @return int : attackId
	 */
	public L2NpcTemplate[] addAttackActId(final int... npcIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (final int npcId : npcIds)
			value[i++] = addEventId(npcId, QuestEventType.ON_ATTACK);
		return value;
	}
	
	public L2NpcTemplate addAttackActId(final int attackId)
	{
		return addEventId(attackId, QuestEventType.ON_ATTACK);
	}
	
	/**
	 * Quest event notifycator for player being attacked by NPC.
	 * 
	 * @param npc
	 *            Npc providing attack.
	 * @param victim
	 *            Attacked npc player.
	 * @return boolean
	 */
	public final boolean notifyAttackAct(final L2Npc npc, final L2PcInstance victim)
	{
		String res = null;
		try
		{
			res = onAttackAct(npc, victim);
		}
		catch (final Exception e)
		{
			return showError(victim, e);
		}
		return showResult(npc, victim, res);
	}
	
	public String onAttackAct(final L2Npc npc, final L2PcInstance victim)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Character See Events.<BR>
	 * <BR>
	 * 
	 * @param npcIds
	 *            : A serie of ids.
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate[] addAggroRangeEnterId(final int... npcIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (final int npcId : npcIds)
			value[i++] = addEventId(npcId, QuestEventType.ON_AGGRO_RANGE_ENTER);
		return value;
	}
	
	public L2NpcTemplate addAggroRangeEnterId(final int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_AGGRO_RANGE_ENTER);
	}
	
	private class TmpOnAggroEnter implements Runnable
	{
		private final L2Npc			_npc;
		private final L2PcInstance	_pc;
		private final boolean		_isPet;
		
		public TmpOnAggroEnter(final L2Npc npc, final L2PcInstance pc, final boolean isPet)
		{
			_npc = npc;
			_pc = pc;
			_isPet = isPet;
		}
		
		@Override
		public void run()
		{
			String res = null;
			try
			{
				res = onAggroRangeEnter(_npc, _pc, _isPet);
			}
			catch (final Exception e)
			{
				showError(_pc, e);
			}
			showResult(_npc, _pc, res);
		}
	}
	
	public final boolean notifyAggroRangeEnter(final L2Npc npc, final L2PcInstance player, final boolean isPet)
	{
		ThreadPoolManager.getInstance().executeAi(new TmpOnAggroEnter(npc, player, isPet));
		return true;
	}
	
	public String onAggroRangeEnter(final L2Npc npc, final L2PcInstance player, final boolean isPet)
	{
		return null;
	}
	
	public final boolean notifyAcquireSkill(final L2Npc npc, final L2PcInstance player, final L2Skill skill)
	{
		String res = null;
		try
		{
			res = onAcquireSkill(npc, player, skill);
			if (res == "true")
				return true;
			else if (res == "false")
				return false;
		}
		catch (final Exception e)
		{
			return showError(player, e);
		}
		return showResult(npc, player, res);
	}
	
	public String onAcquireSkill(final L2Npc npc, final L2PcInstance player, final L2Skill skill)
	{
		return null;
	}
	
	public final boolean notifyAcquireSkillInfo(final L2Npc npc, final L2PcInstance player, final L2Skill skill)
	{
		String res = null;
		try
		{
			res = onAcquireSkillInfo(npc, player, skill);
		}
		catch (final Exception e)
		{
			return showError(player, e);
		}
		return showResult(npc, player, res);
	}
	
	public String onAcquireSkillInfo(final L2Npc npc, final L2PcInstance player, final L2Skill skill)
	{
		return null;
	}
	
	public final boolean notifyAcquireSkillList(final L2Npc npc, final L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onAcquireSkillList(npc, player);
		}
		catch (final Exception e)
		{
			return showError(player, e);
		}
		return showResult(npc, player, res);
	}
	
	public String onAcquireSkillList(final L2Npc npc, final L2PcInstance player)
	{
		return null;
	}
	
	public final boolean notifyDeath(L2Character killer, L2Character victim, QuestState qs)
	{
		String res = null;
		try
		{
			res = onDeath(killer, victim, qs);
		}
		catch (Exception e)
		{
			return showError(qs.getPlayer(), e);
		}
		return showResult(qs.getPlayer(), res);
	}
	
	public final boolean notifyCreatureKill(L2Character killer, L2Character victim, QuestState qs)
	{
		String res = null;
		try
		{
			res = onCreatureKill(killer, victim, qs);
		}
		catch (Exception e)
		{
			return showError(qs.getPlayer(), e);
		}
		return showResult(qs.getPlayer(), res);
	}
	
	public boolean showResult(L2PcInstance player, String res)
	{
		if (res == null || res.isEmpty() || player == null)
			return true;
		if (res.endsWith(".htm"))
		{
			showHtmlFile(player, res);
		}
		else if (res.startsWith("<html>"))
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(res);
			npcReply.replace("%playername%", player.getName());
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (res.startsWith("bonanzo"))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			player.sendMessage(res);
		}
		return false;
	}
	
	public String showHtmlFile(L2PcInstance player, String fileName)
	{
		String questName = getName();
		int questId = getQuestId();
		// Create handler to file linked to the quest
		String directory = getDescr().toLowerCase();
		String content = HtmCache.getInstance().getHtm("data/scripts/" + directory + "/" + questName + "/" + fileName);
		if (content == null)
			content = HtmCache.getInstance().getHtmForce("data/scripts/quests/" + questName + "/" + fileName);
		if (player != null && player.getTarget() != null)
			content = content.replaceAll("%objectId%", String.valueOf(player.getTarget().getObjectId()));
		// Send message to client if message not empty
		if (content != null)
		{
			if (questId > 0 && questId < 20000)
			{
				NpcQuestHtmlMessage npcReply = new NpcQuestHtmlMessage(5, questId);
				npcReply.setHtml(content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			else
			{
				NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
				npcReply.setHtml(content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		return content;
	}
	
	public String onCreatureKill(L2Character killer, L2Character victim, QuestState qs)
	{
		return null;
	}
	
	public final boolean notifyDeath(final L2Character killer, final L2Character victim, final L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onDeath(killer, victim, player);
		}
		catch (final Exception e)
		{
			return showError(player, e);
		}
		if (killer instanceof L2Npc)
			return showResult((L2Npc) killer, player, res);
		return showResult(null, player, res);
	}
	
	public String onDeath(final L2Character killer, final L2Character victim, final L2PcInstance player)
	{
		if (killer instanceof L2Npc)
			return onAdvEvent("", (L2Npc) killer, player);
		return onAdvEvent("", null, player);
	}
	
	public String onDeath(L2Character killer, L2Character victim, QuestState qs)
	{
		if (killer instanceof L2Npc)
			return onAdvEvent("", (L2Npc) killer, qs.getPlayer());
		else
			return onAdvEvent("", null, qs.getPlayer());
	}
	
	public final boolean notifyEvent(final String event, final L2Npc npc, final L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onAdvEvent(event, npc, player);
		}
		catch (final Exception e)
		{
			return showError(player, e);
		}
		return showResult(npc, player, res);
	}
	
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player)
	{
		// if not overridden by a subclass, then default to the returned value
		// of the simpler (and older) onEvent override
		// if the player has a state, use it as parameter in the next call, else
		// return null
		if (player != null)
		{
			final QuestState qs = player.getQuestState(getName());
			if (qs != null)
				return onEvent(event, qs);
		}
		return null;
	}
	
	public String onEvent(final String event, final QuestState qs)
	{
		return null;
	}
	
	public final boolean notifyEnterWorld(final L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onEnterWorld(player);
		}
		catch (final Exception e)
		{
			return showError(player, e);
		}
		return showResult(null, player, res);
	}
	
	public String onEnterWorld(final L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that triggers, when player enters specified zones.<BR>
	 * <BR>
	 * 
	 * @param zoneIds
	 *            : A serie of zone ids.
	 * @return int[] : ID of the L2ZoneType
	 */
	public L2ZoneType[] addEnterZoneId(final int... zoneIds)
	{
		final L2ZoneType[] value = new L2ZoneType[zoneIds.length];
		int i = 0;
		for (final int zoneId : zoneIds)
			try
			{
				final L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
				value[i++] = zone;
			}
			catch (final Exception e)
			{
				_log.log(Level.WARNING, "Exception on addEnterZoneId(): " + e.getMessage(), e);
				continue;
			}
		return value;
	}
	
	public L2ZoneType addEnterZoneId(final int zoneId)
	{
		try
		{
			final L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
			return zone;
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "Exception on addEnterZoneId(): " + e.getMessage(), e);
			return null;
		}
	}
	
	public final boolean notifyEnterZone(final L2Character character, final L2ZoneType zone)
	{
		final L2PcInstance player = character.getActingPlayer();
		String res = null;
		try
		{
			res = onEnterZone(character, zone);
		}
		catch (final Exception e)
		{
			if (player != null)
				return showError(player, e);
		}
		if (player != null)
			return showResult(null, player, res);
		return true;
	}
	
	public String onEnterZone(final L2Character character, final L2ZoneType zone)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that triggers, when player leaves specified zones.<BR>
	 * <BR>
	 * 
	 * @param zoneIds
	 *            : A serie of zone ids.
	 * @return int[] : ID of the L2ZoneType
	 */
	public L2ZoneType[] addExitZoneId(final int... zoneIds)
	{
		final L2ZoneType[] value = new L2ZoneType[zoneIds.length];
		int i = 0;
		for (final int zoneId : zoneIds)
			try
			{
				final L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
				value[i++] = zone;
			}
			catch (final Exception e)
			{
				_log.log(Level.WARNING, "Exception on addEnterZoneId(): " + e.getMessage(), e);
				continue;
			}
		return value;
	}
	
	public L2ZoneType addExitZoneId(final int zoneId)
	{
		try
		{
			final L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
			return zone;
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "Exception on addExitZoneId(): " + e.getMessage(), e);
			return null;
		}
	}
	
	public final boolean notifyExitZone(final L2Character character, final L2ZoneType zone)
	{
		final L2PcInstance player = character.getActingPlayer();
		String res = null;
		try
		{
			res = onExitZone(character, zone);
		}
		catch (final Exception e)
		{
			if (player != null)
				return showError(player, e);
		}
		if (player != null)
			return showResult(null, player, res);
		return true;
	}
	
	public String onExitZone(final L2Character character, final L2ZoneType zone)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Faction Call Events.<BR>
	 * <BR>
	 * 
	 * @param npcIds
	 *            : A serie of ids.
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate[] addFactionCallId(final int... npcIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (final int npcId : npcIds)
			value[i++] = addEventId(npcId, QuestEventType.ON_FACTION_CALL);
		return value;
	}
	
	public L2NpcTemplate addFactionCallId(final int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_FACTION_CALL);
	}
	
	public final boolean notifyFactionCall(final L2Npc npc, final L2Npc caller, final L2PcInstance attacker, final boolean isPet)
	{
		String res = null;
		try
		{
			res = onFactionCall(npc, caller, attacker, isPet);
		}
		catch (final Exception e)
		{
			return showError(attacker, e);
		}
		return showResult(npc, attacker, res);
	}
	
	public String onFactionCall(final L2Npc npc, final L2Npc caller, final L2PcInstance attacker, final boolean isPet)
	{
		return null;
	}
	
	/**
	 * Add the quest to the NPC's first-talk (default action dialog)
	 * 
	 * @param npcIds
	 *            A serie of ids.
	 * @return L2NpcTemplate : Start NPC
	 */
	public L2NpcTemplate[] addFirstTalkId(final int... npcIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (final int npcId : npcIds)
			value[i++] = addEventId(npcId, QuestEventType.ON_FIRST_TALK);
		return value;
	}
	
	public L2NpcTemplate addFirstTalkId(final int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_FIRST_TALK);
	}
	
	public final boolean notifyFirstTalk(final L2Npc npc, final L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch (final Exception e)
		{
			return showError(player, e);
		}
		// if the quest returns text to display, display it.
		if (res != null && res.length() > 0)
			return showResult(npc, player, res);
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return true;
	}
	
	public String onFirstTalk(final L2Npc npc, final L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for Kill Events.<BR>
	 * <BR>
	 * 
	 * @param killIds
	 *            A serie of ids.
	 * @return int : killId
	 */
	public L2NpcTemplate[] addKillId(final int... killIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[killIds.length];
		int i = 0;
		for (final int killId : killIds)
			value[i++] = addEventId(killId, QuestEventType.ON_KILL);
		return value;
	}
	
	public L2NpcTemplate addKillId(final int killId)
	{
		return addEventId(killId, QuestEventType.ON_KILL);
	}
	
	public final boolean notifyKill(final L2Npc npc, final L2PcInstance killer, final boolean isPet)
	{
		String res = null;
		try
		{
			res = onKill(npc, killer, isPet);
		}
		catch (final Exception e)
		{
			return showError(killer, e);
		}
		return showResult(npc, killer, res);
	}
	
	public String onKill(final L2Npc npc, final L2PcInstance killer, final boolean isPet)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Spawn Events.<BR>
	 * <BR>
	 * 
	 * @param npcIds
	 *            : A serie of ids.
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate[] addSpawnId(final int... npcIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (final int npcId : npcIds)
			value[i++] = addEventId(npcId, QuestEventType.ON_SPAWN);
		return value;
	}
	
	public L2NpcTemplate addSpawnId(final int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_SPAWN);
	}
	
	public final boolean notifySpawn(final L2Npc npc)
	{
		try
		{
			onSpawn(npc);
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "Exception on onSpawn() in notifySpawn(): " + e.getMessage(), e);
			return true;
		}
		return false;
	}
	
	public String onSpawn(final L2Npc npc)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Skill-See Events.<BR>
	 * <BR>
	 * 
	 * @param npcIds
	 *            : A serie of ids.
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate[] addSkillSeeId(final int... npcIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (final int npcId : npcIds)
			value[i++] = addEventId(npcId, QuestEventType.ON_SKILL_SEE);
		return value;
	}
	
	public L2NpcTemplate addSkillSeeId(final int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_SKILL_SEE);
	}
	
	public class TmpOnSkillSee implements Runnable
	{
		private final L2Npc			_npc;
		private final L2PcInstance	_caster;
		private final L2Skill		_skill;
		private final L2Object[]	_targets;
		private final boolean		_isPet;
		
		public TmpOnSkillSee(final L2Npc npc, final L2PcInstance caster, final L2Skill skill, final L2Object[] targets, final boolean isPet)
		{
			_npc = npc;
			_caster = caster;
			_skill = skill;
			_targets = targets;
			_isPet = isPet;
		}
		
		@Override
		public void run()
		{
			String res = null;
			try
			{
				res = onSkillSee(_npc, _caster, _skill, _targets, _isPet);
			}
			catch (final Exception e)
			{
				showError(_caster, e);
			}
			showResult(_npc, _caster, res);
		}
	}
	
	public final boolean notifySkillSee(final L2Npc npc, final L2PcInstance caster, final L2Skill skill, final L2Object[] targets, final boolean isPet)
	{
		ThreadPoolManager.getInstance().executeAi(new TmpOnSkillSee(npc, caster, skill, targets, isPet));
		return true;
	}
	
	public String onSkillSee(final L2Npc npc, final L2PcInstance caster, final L2Skill skill, final L2Object[] targets, final boolean isPet)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to any skill being used by other npcs or players.<BR>
	 * <BR>
	 * 
	 * @param npcIds
	 *            : A serie of ids.
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate[] addSpellFinishedId(final int... npcIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (final int npcId : npcIds)
			value[i++] = addEventId(npcId, QuestEventType.ON_SPELL_FINISHED);
		return value;
	}
	
	public L2NpcTemplate addSpellFinishedId(final int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_SPELL_FINISHED);
	}
	
	public final boolean notifySpellFinished(final L2Npc npc, final L2PcInstance player, final L2Skill skill)
	{
		String res = null;
		try
		{
			res = onSpellFinished(npc, player, skill);
		}
		catch (final Exception e)
		{
			return showError(player, e);
		}
		return showResult(npc, player, res);
	}
	
	public String onSpellFinished(final L2Npc npc, final L2PcInstance player, final L2Skill skill)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Talk Events.<BR>
	 * <BR>
	 * 
	 * @param talkIds
	 *            : A serie of ids.
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate[] addTalkId(final int... talkIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[talkIds.length];
		int i = 0;
		for (final int talkId : talkIds)
			value[i++] = addEventId(talkId, QuestEventType.ON_TALK);
		return value;
	}
	
	public L2NpcTemplate addTalkId(final int talkId)
	{
		return addEventId(talkId, QuestEventType.ON_TALK);
	}
	
	public final boolean notifyTalk(final L2Npc npc, final L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onTalk(npc, player);
		}
		catch (final Exception e)
		{
			return showError(player, e);
		}
		player.setLastQuestNpcObject(npc.getObjectId());
		return showResult(npc, player, res);
	}
	
	public String onTalk(final L2Npc npc, final L2PcInstance talker)
	{
		return null;
	}
	
	@Override
	public void setActive(final boolean status)
	{}
	
	@Override
	public boolean reload()
	{
		unload();
		return super.reload();
	}
	
	@Override
	public boolean unload()
	{
		return unload(true);
	}
	
	public boolean unload(final boolean removeFromList)
	{
		saveGlobalData();
		for (final List<QuestTimer> timers : _eventTimers.values())
			for (final QuestTimer timer : timers)
				timer.cancel();
		_eventTimers.clear();
		if (removeFromList)
			return QuestManager.getInstance().removeQuest(this);
		return true;
	}
	
	/**
	 * This function is, by default, called at shutdown, for all quests, by the QuestManager.<br>
	 * Children of this class can implement this function in order to convert their structures into <var, value> tuples and make calls to save them to the database, if needed.<br>
	 * <br>
	 * By default, nothing is saved.
	 */
	public void saveGlobalData()
	{}
	
	final public static int	HALFDAY		= 0;
	final public static int	ONEDAY		= 1;
	final public static int	TWODAYS		= 2;
	final public static int	HALFWEEK	= 3;
	final public static int	WEEK		= 4;
	
	public long getNextInstanceTime(int type)
	{
		final Calendar calendar = Calendar.getInstance();
		switch (type)
		{
			case HALFDAY:
				calendar.set(Calendar.HOUR, 11);
				calendar.set(Calendar.MINUTE, 59);
				calendar.set(Calendar.SECOND, 59);
				calendar.set(Calendar.MILLISECOND, 999);
				break;
			case ONEDAY:
				calendar.set(Calendar.HOUR_OF_DAY, 23);
				calendar.set(Calendar.MINUTE, 59);
				calendar.set(Calendar.SECOND, 59);
				calendar.set(Calendar.MILLISECOND, 999);
				break;
			case TWODAYS:
				final int day = calendar.get(Calendar.DAY_OF_YEAR);
				if (day % 2 == 0)
					calendar.set(Calendar.DAY_OF_YEAR, day + 2);
				else
					calendar.set(Calendar.DAY_OF_YEAR, day + 1);
				calendar.set(Calendar.HOUR_OF_DAY, 23);
				calendar.set(Calendar.MINUTE, 59);
				calendar.set(Calendar.SECOND, 59);
				calendar.set(Calendar.MILLISECOND, 999);
				break;
			case HALFWEEK:
				Calendar calendar2 = Calendar.getInstance();
				calendar2.setFirstDayOfWeek(Calendar.SUNDAY);
				calendar2.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
				calendar2.set(Calendar.HOUR_OF_DAY, 12);
				calendar2.set(Calendar.MINUTE, 0);
				calendar2.set(Calendar.SECOND, 0);
				calendar2.set(Calendar.MILLISECOND, 0);
				if (calendar.compareTo(calendar2) > 0) // done on thur, fri, sat and last half of wed
				{
					calendar.setFirstDayOfWeek(Calendar.SUNDAY);
					calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
					calendar.set(Calendar.HOUR_OF_DAY, 23);
					calendar.set(Calendar.MINUTE, 59);
					calendar.set(Calendar.SECOND, 59);
					calendar.set(Calendar.MILLISECOND, 999);
				}
				else
				{
					calendar.setFirstDayOfWeek(Calendar.SUNDAY);
					calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
					calendar.set(Calendar.HOUR_OF_DAY, 11);
					calendar.set(Calendar.MINUTE, 59);
					calendar.set(Calendar.SECOND, 59);
					calendar.set(Calendar.MILLISECOND, 999);
				}
				break;
			case WEEK:
				calendar.setFirstDayOfWeek(Calendar.SUNDAY);
				calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
				calendar.set(Calendar.HOUR_OF_DAY, 23);
				calendar.set(Calendar.MINUTE, 59);
				calendar.set(Calendar.SECOND, 59);
				calendar.set(Calendar.MILLISECOND, 999);
				break;
		}
		return calendar.getTimeInMillis();
	}
	
	final protected static void auditInstances(final L2PcInstance player, String template, final int instanceid)
	{
		final String player_acct = player.getAccountName();
		final String player_name = player.getName();
		final String player_IP = player.getIP();
		final String today = GMAudit._formatter.format(new Date());
		template = template.replaceAll(".xml", "");
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO audit_instance(player_acct, player, player_IP, instance, date) VALUES(?,?,?,?,?)");
			statement.setString(1, player_acct);
			statement.setString(2, player_name);
			statement.setString(3, player_IP);
			statement.setString(4, template);
			statement.setString(5, today);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.fine("could not audit instances: " + player_name + " " + e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public boolean checkIPs(L2Party party)
	{
		if (party != null)
		{
			for (L2PcInstance player : party.getPartyMembers())
			{
				final String IP = player.getHWID();
				if (IP != null)
				{
					for (L2PcInstance player2 : party.getPartyMembers())
					{
						if (player == player2)
							continue;
						if (player2.getIP() == null || IP.equalsIgnoreCase(player2.getHWID()))
						{
							party.broadcastMessageToPartyMembers(player2.getName() + " has the same HWID as " + player + " and thus cannot enter the instance");
							return false;
						}
					}
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public String getScriptName()
	{
		return _name;
	}
	
	@Override
	public Path getScriptPath()
	{
		return null;
	}
	
	public static void updateQuestVarInDb(QuestState qs, String var, String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE charId=? AND name=? AND var = ?");
			statement.setString(1, value);
			statement.setInt(2, qs.getPlayer().getObjectId());
			statement.setString(3, qs.getQuest().getName());
			statement.setString(4, var);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not update char quest:", e);
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
	
	public static void createQuestVarInDb(QuestState qs, String var, String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?)");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuest().getName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
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