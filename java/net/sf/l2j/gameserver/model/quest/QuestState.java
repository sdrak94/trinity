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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.L2DropData;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowQuestMark;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.QuestList;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.TutorialCloseHtml;
import net.sf.l2j.gameserver.network.serverpackets.TutorialEnableClientEvent;
import net.sf.l2j.gameserver.network.serverpackets.TutorialShowHtml;
import net.sf.l2j.gameserver.network.serverpackets.TutorialShowQuestionMark;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.util.Rnd;

/**
 * @author Luis Arias
 */
public final class QuestState
{
	protected static final Logger		_log							= Logger.getLogger(Quest.class.getName());
	public static final String			SOUND_ACCEPT					= "ItemSound.quest_accept";
	public static final String			SOUND_ITEMGET					= "ItemSound.quest_itemget";
	public static final String			SOUND_MIDDLE					= "ItemSound.quest_middle";
	public static final String			SOUND_FINISH					= "ItemSound.quest_finish";
	public static final String			SOUND_GIVEUP					= "ItemSound.quest_giveup";
	public static final String			SOUND_JACKPOT					= "ItemSound.quest_jackpot";
	public static final String			SOUND_FANFARE					= "ItemSound.quest_fanfare_2";
	private static final String			QUEST_SET_VAR					= "REPLACE INTO character_quests (charId,name,var,value) VALUES (?,?,?,?)";
	private static final String			QUEST_DEL_VAR					= "DELETE FROM character_quests WHERE charId=? AND name=? AND var=?";
	private static final String			QUEST_DELETE					= "DELETE FROM character_quests WHERE charId=? AND name=?";
	private static final String			QUEST_COMPLETE					= "DELETE FROM character_quests WHERE charId=? AND name=? AND var<>'<state>'";
	public static final byte			DROP_DIVMOD						= 0;
	public static final byte			DROP_FIXED_RATE					= 1;
	public static final byte			DROP_FIXED_COUNT				= 2;
	public static final byte			DROP_FIXED_BOTH					= 3;
	private final L2PcInstance			_player;
	private final Quest					_quest;
	private byte						_state;
	private Map<String, String>	_vars							= new HashMap<>();
	
	/**
	 * Constructor of the QuestState : save the quest in the list of quests of the player.<BR/>
	 * <BR/>
	 * <U><I>Actions :</U></I><BR/>
	 * <LI>Save informations in the object QuestState created (Quest, Player, Completion, State)</LI>
	 * <LI>Add the QuestState in the player's list of quests by using setQuestState()</LI>
	 * <LI>Add drops gotten by the quest</LI> <BR/>
	 * 
	 * @param quest
	 *            : quest associated with the QuestState
	 * @param player
	 *            : L2PcInstance pointing out the player
	 * @param state
	 *            : state of the quest
	 */
	QuestState(final L2PcInstance player, final Quest quest, final byte state)
	{
		_player = player;
		_quest = quest;
		_state = state;
		_player.setQuestState(this);
	}
	
	/**
	 * Return the L2PcInstance
	 * 
	 * @return L2PcInstance
	 */
	public L2PcInstance getPlayer()
	{
		return _player;
	}
	
	/**
	 * Return the quest
	 * 
	 * @return Quest
	 */
	public Quest getQuest()
	{
		return _quest;
	}
	
	/**
	 * Return the state of the quest
	 * 
	 * @return State
	 */
	public byte getState()
	{
		return _state;
	}
	
	/**
	 * Return true if quest just created, false otherwise
	 * 
	 * @return
	 */
	public boolean isCreated()
	{
		return _state == Quest.STATE_CREATED;
	}
	
	/**
	 * Return true if quest completed, false otherwise
	 * 
	 * @return boolean
	 */
	public boolean isCompleted()
	{
		return _state == Quest.STATE_COMPLETED;
	}
	
	/**
	 * Return true if quest started, false otherwise
	 * 
	 * @return boolean
	 */
	public boolean isStarted()
	{
		return _state == Quest.STATE_STARTED;
	}
	
	/**
	 * Return state of the quest after its initialization.<BR>
	 * <BR>
	 * <U><I>Actions :</I></U>
	 * <LI>Remove drops from previous state</LI>
	 * <LI>Set new state of the quest</LI>
	 * <LI>Add drop for new state</LI>
	 * <LI>Update information in database</LI>
	 * <LI>Send packet QuestList to client</LI>
	 * 
	 * @param state
	 */
	public void setState(final byte state)
	{
		if (_state != state)
		{
			_state = state;
			setQuestVarInDb("<state>", String.valueOf(_state));
			_player.sendPacket(new QuestList());
		}
	}
	
	/**
	 * Destroy element used by quest when quest is exited
	 * 
	 * @param repeatable
	 */
	public void exitQuest(final boolean repeatable)
	{
		// Remove quest from player's notifyDeath list.
		_player.removeNotifyQuestOfDeath(this);
		if (!isStarted())
			return;
		// Remove/Complete quest.
		if (repeatable)
		{
			_player.delQuestState(this);
			_player.sendPacket(new QuestList());
		}
		else
			setState(Quest.STATE_COMPLETED);
		// Remove quest variables.
		_vars.clear();
		// Remove registered quest items.
		final int[] itemIdList = _quest.getRegisteredItemIds();
		if (itemIdList != null)
			for (final int itemId : itemIdList)
				takeItems(itemId, -1);
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement;
			if (repeatable)
				statement = con.prepareStatement(QUEST_DELETE);
			else
				statement = con.prepareStatement(QUEST_COMPLETE);
			statement.setInt(1, _player.getObjectId());
			statement.setString(2, _quest.getName());
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "could not delete char quest:", e);
		}
	}
	
	/**
	 * Add player to get notification of characters death
	 */
	public void addNotifyOfDeath()
	{
		if (_player != null)
			_player.addNotifyQuestOfDeath(this);
	}
	
	/**
	 * Return value of parameter "val" after adding the couple (var,val) in class variable "vars".<BR>
	 * <BR>
	 * <U><I>Actions :</I></U><BR>
	 * <LI>Initialize class variable "vars" if is null</LI>
	 * <LI>Initialize parameter "val" if is null</LI>
	 * <LI>Add/Update couple (var,val) in class variable FastMap "vars"</LI>
	 * <LI>If the key represented by "var" exists in FastMap "vars", the couple (var,val) is updated in the database. The key is
	 * known as existing if the preceding value of the key (given as result of function put()) is not null.<BR>
	 * If the key doesn't exist, the couple is added/created in the database</LI>
	 * 
	 * @param var
	 *            : String indicating the name of the variable for quest
	 * @param value
	 *            : String indicating the value of the variable for quest
	 */
	
	public String set(String var, String val)
	{
		if (_vars == null)
			_vars = new HashMap<String, String>();

		if (val == null)
			val = "";

		// FastMap.put() returns previous value associated with specified key, or null if there was no mapping for key.
		String old = _vars.put(var, val);

		if (old != null)
			Quest.updateQuestVarInDb(this, var, val);
		else
			Quest.createQuestVarInDb(this, var, val);

		if ("cond".equals(var))
		{
			try
			{
				int previousVal = 0;
				try
				{
					previousVal = Integer.parseInt(old);
				}
				catch (Exception ex)
				{
					previousVal = 0;
				}
				setCond(Integer.parseInt(val), previousVal);
			}
			catch (Exception e)
			{
				_log.finer(getPlayer().getName() + ", " + _quest.getName() + " cond [" + val + "] is not an integer.  Value stored, but no packet was sent: " + e);
			}
		}

		return val;
	}
	
//	public void set(final String var, final String value)
//	{
//		if (var == null || var.isEmpty() || value == null || value.isEmpty())
//			return;
//		// FastMap.put() returns previous value associated with specified key,
//		// or null if there was no mapping for key.
//		final String old = _vars.put(var, value);
//		setQuestVarInDb(var, value);
//		if ("cond".equals(var))
//			try
//			{
//				int previousVal = 0;
//				try
//				{
//					previousVal = Integer.parseInt(old);
//				}
//				catch (final Exception ex)
//				{
//					previousVal = 0;
//				}
//				setCond(Integer.parseInt(value), previousVal);
//			}
//			catch (final Exception e)
//			{
//				_log.log(Level.WARNING, _player.getName() + ", " + _quest.getName() + " cond [" + value + "] is not an integer. Value stored, but no packet was sent: " + e.getMessage(), e);
//			}
//	}
	
	/**
	 * Add parameter used in quests.
	 * 
	 * @param var
	 *            : String pointing out the name of the variable for quest
	 * @param value
	 *            : String pointing out the value of the variable for quest
	 */
	public void setInternal(final String var, final String value)
	{
		if (var == null || var.isEmpty() || value == null || value.isEmpty())
			return;
		_vars.put(var, value);
	}
	
	/**
	 * Internally handles the progression of the quest so that it is ready for sending appropriate packets to the client<BR>
	 * <BR>
	 * <U><I>Actions :</I></U><BR>
	 * <LI>Check if the new progress number resets the quest to a previous (smaller) step</LI>
	 * <LI>If not, check if quest progress steps have been skipped</LI>
	 * <LI>If skipped, prepare the variable completedStateFlags appropriately to be ready for sending to clients</LI>
	 * <LI>If no steps were skipped,
	 * flags do not need to be prepared...</LI>
	 * <LI>If the passed step resets the quest to a previous step, reset such that steps after the parameter are not considered, while skipped steps before the parameter, if any, maintain their info</LI>
	 * 
	 * @param cond
	 *            : int indicating the step number for the current quest progress (as will be shown to the client)
	 * @param old
	 *            : int indicating the previously noted step For more info on the variable communicating the progress steps to the client, please see
	 */
	private void setCond(final int cond, final int old)
	{
		int completedStateFlags = 0; // initializing...
		// if there is no change since last setting, there is nothing to do here
		if (cond == old)
			return;
		// cond 0 and 1 do not need completedStateFlags. Also, if cond > 1, the
		// 1st step must
		// always exist (i.e. it can never be skipped). So if cond is 2, we can
		// still safely
		// assume no steps have been skipped.
		// Finally, more than 31 steps CANNOT be supported in any way with
		// skipping.
		if (cond < 3 || cond > 31)
			unset("__compltdStateFlags");
		else
			completedStateFlags = getInt("__compltdStateFlags");
		// case 1: No steps have been skipped so far...
		if (completedStateFlags == 0)
		{
			// check if this step also doesn't skip anything. If so, no further
			// work is needed
			// also, in this case, no work is needed if the state is being reset
			// to a smaller value
			// in those cases, skip forward to informing the client about the
			// change...
			// ELSE, if we just now skipped for the first time...prepare the
			// flags!!!
			if (cond > old + 1)
			{
				// set the most significant bit to 1 (indicates that there exist
				// skipped states)
				// also, ensure that the least significant bit is an 1 (the
				// first step is never skipped, no matter
				// what the cond says)
				completedStateFlags = 0x80000001;
				// since no flag had been skipped until now, the least
				// significant bits must all
				// be set to 1, up until "old" number of bits.
				completedStateFlags |= (1 << old) - 1;
				// now, just set the bit corresponding to the passed cond to 1
				// (current step)
				completedStateFlags |= 1 << cond - 1;
				set("__compltdStateFlags", String.valueOf(completedStateFlags));
			}
		}
		// case 2: There were exist previously skipped steps
		else // if this is a push back to a previous step, clear all completion
		// flags ahead
		if (cond < old)
		{
			completedStateFlags &= (1 << cond) - 1; // note, this also
													// unsets the flag
													// indicating that
													// there exist skips
			// now, check if this resulted in no steps being skipped any
			// more
			if (completedStateFlags == (1 << cond) - 1)
				unset("__compltdStateFlags");
			else
			{
				// set the most significant bit back to 1 again, to
				// correctly indicate that this skips states.
				// also, ensure that the least significant bit is an 1 (the
				// first step is never skipped, no matter
				// what the cond says)
				completedStateFlags |= 0x80000001;
				set("__compltdStateFlags", String.valueOf(completedStateFlags));
			}
		}
		// if this moves forward, it changes nothing on previously skipped
		// steps...so just mark this
		// state and we are done
		else
		{
			completedStateFlags |= 1 << cond - 1;
			set("__compltdStateFlags", String.valueOf(completedStateFlags));
		}
		// send a packet to the client to inform it of the quest progress (step
		// change)
		_player.sendPacket(new QuestList());
		if (_quest.isRealQuest() && cond > 0)
			_player.sendPacket(new ExShowQuestMark(_quest.getQuestId()));
	}
	
	/**
	 * Remove the variable of quest from the list of variables for the quest.<BR>
	 * <BR>
	 * <U><I>Concept : </I></U> Remove the variable of quest represented by "var" from the class variable FastMap "vars" and from the database.
	 * 
	 * @param var
	 *            : String designating the variable for the quest to be deleted
	 */
	public void unset(final String var)
	{
		if (_vars.remove(var) != null)
			removeQuestVarInDb(var);
	}
	
	/**
	 * Return the value of the variable of quest represented by "var"
	 * 
	 * @param var
	 *            : name of the variable of quest
	 * @return String
	 */
	public String get(final String var)
	{
		return _vars.get(var);
	}
	
	/**
	 * Return the value of the variable of quest represented by "var"
	 * 
	 * @param var
	 *            : String designating the variable for the quest
	 * @return int
	 */
	public int getInt(final String var)
	{
		final String variable = _vars.get(var);
		if (variable == null || variable.isEmpty())
			return 0;
		int value = 0;
		try
		{
			value = Integer.parseInt(variable);
		}
		catch (final Exception e)
		{
			_log.log(Level.FINER, _player.getName() + ": variable " + var + " isn't an integer: " + value + " ! " + e.getMessage(), e);
		}
		return value;
	}
	
	/**
	 * Set in the database the quest for the player.
	 * 
	 * @param var
	 *            : String designating the name of the variable for the quest
	 * @param value
	 *            : String designating the value of the variable for the quest
	 */
	private void setQuestVarInDb(final String var, final String value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(QUEST_SET_VAR);
			statement.setInt(1, _player.getObjectId());
			statement.setString(2, _quest.getName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
		}
	}

	
	/**
	 * Delete a variable of player's quest from the database.
	 * 
	 * @param var
	 *            : String designating the variable characterizing the quest
	 */
	private void removeQuestVarInDb(final String var)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(QUEST_DEL_VAR);
			statement.setInt(1, _player.getObjectId());
			statement.setString(2, _quest.getName());
			statement.setString(3, var);
			statement.executeUpdate();
			statement.close();
		}
		catch (final Exception e)
		{
			_log.log(Level.WARNING, "could not delete char quest:", e);
		}
	}
	
	/**
	 * @param itemId
	 *            : ID of the item you're looking for
	 * @return true if item exists in player's inventory, false - if not
	 */
	public boolean hasQuestItems(final int itemId)
	{
		return _player.getInventory().getItemByItemId(itemId) != null;
	}
	
	/**
	 * @param itemId
	 *            : ID of the item wanted to be count
	 * @return the quantity of one sort of item hold by the player
	 */
	public int getQuestItemsCount(final int itemId)
	{
		int count = 0;
		for (final L2ItemInstance item : _player.getInventory().getItems())
			if (item != null && item.getItemId() == itemId)
				count += item.getCount();
		return count;
	}
	
	/**
	 * @param loc
	 *            A paperdoll slot to check.
	 * @return the id of the item in the loc paperdoll slot.
	 */
	public int getItemEquipped(final int loc)
	{
		return _player.getInventory().getPaperdollItemId(loc);
	}
	
	/**
	 * Return the level of enchantment on the weapon of the player(Done specifically for weapon SA's)
	 * 
	 * @param itemId
	 *            : ID of the item to check enchantment
	 * @return int
	 */
	public int getEnchantLevel(final int itemId)
	{
		final L2ItemInstance enchanteditem = _player.getInventory().getItemByItemId(itemId);
		if (enchanteditem == null)
			return 0;
		return enchanteditem.getEnchantLevel();
	}
	
	/**
	 * Give items to the player's inventory.
	 * 
	 * @param itemId
	 *            : Identifier of the item.
	 * @param itemCount
	 *            : Quantity of items to add.
	 */
	public void giveItems(final int itemId, final int itemCount)
	{
		giveItems(itemId, itemCount, 0);
	}
	
	/**
	 * Give items to the player's inventory.
	 * 
	 * @param itemId
	 *            : Identifier of the item.
	 * @param itemCount
	 *            : Quantity of items to add.
	 * @param enchantLevel
	 *            : Enchant level of items to add.
	 */
	public void giveItems(final int itemId, final int itemCount, final int enchantLevel)
	{
		// Incorrect amount.
		if (itemCount <= 0)
			return;
		// Add items to player's inventory.
		final L2ItemInstance item = _player.getInventory().addItem("Quest", itemId, itemCount, _player, _player);
		if (item == null)
			return;
		// Set enchant level for the item.
		if (enchantLevel > 0)
			item.setEnchantLevel(enchantLevel);
		// Send message to the client.
		if (itemId == 57)
		{
			final SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_ADENA);
			// final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA);
			smsg.addItemNumber(itemCount);
			_player.sendPacket(smsg);
		}
		else if (itemCount > 1)
		{
			final SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
			// final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
			smsg.addItemName(item);
			smsg.addItemNumber(itemCount);
			_player.sendPacket(smsg);
		}
		else
		{
			final SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_ITEM);
			// final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
			smsg.addItemName(item);
			_player.sendPacket(smsg);
		}
		// Send status update packet.
		final StatusUpdate su = new StatusUpdate(_player);
		su.addAttribute(StatusUpdate.CUR_LOAD, _player.getCurrentLoad());
		_player.sendPacket(su);
	}
	
	/**
	 * Remove items from the player's inventory.
	 * 
	 * @param itemId
	 *            : Identifier of the item.
	 * @param itemCount
	 *            : Quantity of items to destroy.
	 */
	public void takeItems(final int itemId, long itemCount)
	{
		// Find item in player's inventory.
		final L2ItemInstance item = _player.getInventory().getItemByItemId(itemId);
		if (item == null)
			return;
		// Tests on count value and set correct value if necessary.
		if (itemCount < 0 || itemCount > item.getCount())
			itemCount = item.getCount();
		// Disarm item, if equipped.
		if (item.isEquipped())
		{
			final L2ItemInstance[] unequiped = _player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
			final InventoryUpdate iu = new InventoryUpdate();
			for (final L2ItemInstance itm : unequiped)
				iu.addModifiedItem(itm);
			_player.sendPacket(iu);
			_player.broadcastUserInfo();
		}
		// Destroy the quantity of items wanted.
		_player.destroyItemByItemId("Quest", itemId, itemCount, _player, true);
	}
	
	/**
	 * Drop items to the player's inventory. Rate is 100%, amount is affected by Config.RATE_QUEST_DROP.
	 * 
	 * @param itemId
	 *            : Identifier of the item to be dropped.
	 * @param count
	 *            : Quantity of items to be dropped.
	 * @param neededCount
	 *            : Quantity of items needed to complete the task. If set to 0, unlimited amount is collected.
	 * @return boolean : Indicating whether item quantity has been reached.
	 */
	public boolean dropItemsAlways(final int itemId, final int count, final int neededCount)
	{
		return dropItems(itemId, count, neededCount, L2DropData.MAX_CHANCE, DROP_FIXED_RATE);
	}
	
	/**
	 * Drop items to the player's inventory. Rate and amount is affected by DIVMOD of Config.RATE_QUEST_DROP.
	 * 
	 * @param itemId
	 *            : Identifier of the item to be dropped.
	 * @param count
	 *            : Quantity of items to be dropped.
	 * @param neededCount
	 *            : Quantity of items needed to complete the task. If set to 0, unlimited amount is collected.
	 * @param dropChance
	 *            : Item drop rate (100% chance is defined by the L2DropData.MAX_CHANCE = 1.000.000).
	 * @return boolean : Indicating whether item quantity has been reached.
	 */
	public boolean dropItems(final int itemId, final int count, final int neededCount, final int dropChance)
	{
		return dropItems(itemId, count, neededCount, dropChance, DROP_DIVMOD);
	}
	
	/**
	 * Drop items to the player's inventory.
	 * 
	 * @param itemId
	 *            : Identifier of the item to be dropped.
	 * @param count
	 *            : Quantity of items to be dropped.
	 * @param neededCount
	 *            : Quantity of items needed to complete the task. If set to 0, unlimited amount is collected.
	 * @param dropChance
	 *            : Item drop rate (100% chance is defined by the L2DropData.MAX_CHANCE = 1.000.000).
	 * @param type
	 *            : Item drop behavior: DROP_DIVMOD (rate and), DROP_FIXED_RATE, DROP_FIXED_COUNT or DROP_FIXED_BOTH
	 * @return boolean : Indicating whether item quantity has been reached.
	 */
	public boolean dropItems(final int itemId, final int count, final int neededCount, int dropChance, final byte type)
	{
		// Get current amount of item.
		final int currentCount = getQuestItemsCount(itemId);
		// Required amount reached already?
		if (neededCount > 0 && currentCount >= neededCount)
			return true;
		int amount = 0;
		switch (type)
		{
			case DROP_DIVMOD:
				dropChance *= 1;
				amount = count * (dropChance / L2DropData.MAX_CHANCE);
				if (Rnd.get(L2DropData.MAX_CHANCE) < dropChance % L2DropData.MAX_CHANCE)
					amount += count;
				break;
			case DROP_FIXED_RATE:
				if (Rnd.get(L2DropData.MAX_CHANCE) < dropChance)
					amount = (int) (count * 1);
				break;
			case DROP_FIXED_COUNT:
				if (Rnd.get(L2DropData.MAX_CHANCE) < dropChance * 1)
					amount = count;
				break;
			case DROP_FIXED_BOTH:
				if (Rnd.get(L2DropData.MAX_CHANCE) < dropChance)
					amount = count;
				break;
		}
		boolean reached = false;
		if (amount > 0)
		{
			// Limit count to reach required amount.
			if (neededCount > 0)
			{
				reached = currentCount + amount >= neededCount;
				amount = reached ? neededCount - currentCount : amount;
			}
			// Inventory slot check.
			if (!_player.getInventory().validateCapacityByItemId(itemId))
				return false;
			// Give items to the player.
			giveItems(itemId, amount, 0);
			// Play the sound.
			playSound(reached ? SOUND_MIDDLE : SOUND_ITEMGET);
		}
		return neededCount > 0 && reached;
	}
	
	/**
	 * Reward player with items. The amount is affected by Config.RATE_QUEST_REWARD or Config.RATE_QUEST_REWARD_ADENA.
	 * 
	 * @param itemId
	 *            : Identifier of the item.
	 * @param itemCount
	 *            : Quantity of item to reward before applying multiplier.
	 */
	public void rewardItems(final int itemId, final int itemCount)
	{
		if (itemId == 57)
			giveItems(itemId, (int) (itemCount * 1), 0);
		else
			giveItems(itemId, (int) (itemCount * 1), 0);
	}
	
	/**
	 * Reward player with EXP and SP. The amount is affected by Stats.EXPSP_RATE, Config.RATE_QUEST_REWARD_XP and Config.RATE_QUEST_REWARD_SP
	 * 
	 * @param exp
	 *            : Experience amount.
	 * @param sp
	 *            : Skill point amount.
	 */
	public void rewardExpAndSp(final int exp, final int sp)
	{
		_player.addExpAndSp((int) _player.calcStat(Stats.EXPSP_RATE, exp * 1, null, null), (int) _player.calcStat(Stats.EXPSP_RATE, sp * 1, null, null));
	}
	
	// TODO: More radar functions need to be added when the radar class is
	// complete.
	// BEGIN STUFF THAT WILL PROBABLY BE CHANGED
	public void addRadar(final int x, final int y, final int z)
	{
		_player.getRadar().addMarker(x, y, z);
	}
	
	public void removeRadar(final int x, final int y, final int z)
	{
		_player.getRadar().removeMarker(x, y, z);
	}
	
	public void clearRadar()
	{
		_player.getRadar().removeAllMarkers();
	}
	
	// END STUFF THAT WILL PROBABLY BE CHANGED
	/**
	 * Send a packet in order to play sound at client terminal
	 * 
	 * @param sound
	 */
	public void playSound(final String sound)
	{
		_player.sendPacket(new PlaySound(sound));
	}
	
	public void showQuestionMark(final int number)
	{
		_player.sendPacket(new TutorialShowQuestionMark(number));
	}
	
	public void playTutorialVoice(final String voice)
	{
		_player.sendPacket(new PlaySound(2, voice, 0, 0, _player.getX(), _player.getY(), _player.getZ()));
	}
	
	public void showTutorialHTML(final String html)
	{
		_player.sendPacket(new TutorialShowHtml(HtmCache.getInstance().getHtmForce("data/scripts/quests/Tutorial/" + html)));
	}
	
	public void closeTutorialHtml()
	{
		_player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
	}
	
	public void onTutorialClientEvent(final int number)
	{
		_player.sendPacket(new TutorialEnableClientEvent(number));
	}
}
