package net.sf.l2j.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.network.serverpackets.ExRedSky;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.util.Point3D;
import net.sf.l2j.util.Rnd;

public class CursedWeapon
{
private static final Logger _log = Logger.getLogger(CursedWeaponsManager.class.getName());

// _name is the name of the cursed weapon associated with its ID.
private final String _name;
// _itemId is the Item ID of the cursed weapon.
private final int _itemId;
// _skillId is the skills ID.
private final int _skillId;
private final int _skillMaxLevel;
private int _dropRate;
private int _duration;
private int _durationLost;
private int _disapearChance;
private int _stageKills;
public final static int ZARICHE = 8190;
public final static int AKAMANAH = 8689;

// this should be false unless if the cursed weapon is dropped, in that case it would be true.
private boolean _isDropped = false;
// this sets the cursed weapon status to true only if a player has the cursed weapon, otherwise
// this should be false.
private boolean _isActivated = false;
private ScheduledFuture<?> _removeTask;

private int _nbKills = 0;
private long _endTime = 0;

private int _playerId = 0;
protected L2PcInstance _player = null;
private L2ItemInstance _item = null;
private int _playerKarma = 0;
private int _playerPkKills = 0;
protected int transformationId = 0;

// =========================================================
// Constructor
public CursedWeapon(int itemId, int skillId, String name)
{
	_name = name;
	_itemId = itemId;
	_skillId = skillId;
	_skillMaxLevel = SkillTable.getInstance().getMaxLevel(_skillId);
}

// =========================================================
// Private
public void endOfLife()
{
	if (_isActivated)
	{
		if (_player != null && _player.isOnline() == 1)
		{
			// Remove from player
			_log.info(_name + " being removed online.");
			
			_player.abortAttack();
			
			_player.setKarma(_playerKarma);
			_player.setPkKills(_playerPkKills);
			_player.setCursedWeaponEquippedId(0);
			removeSkill();
			
			// Remove
			_player.getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_LR_HAND);
			_player.store();
			
			// Destroy
			L2ItemInstance removedItem = _player.getInventory().destroyItemByItemId("", _itemId, 1, _player, null);
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				if (removedItem.getCount() == 0)
					iu.addRemovedItem(removedItem);
				else
					iu.addModifiedItem(removedItem);
				
				_player.sendPacket(iu);
			}
			else
				_player.sendPacket(new ItemList(_player, true));
			
			_player.broadcastUserInfo();
		}
		else
		{
			// Remove from Db
			_log.info(_name + " being removed offline.");
			
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				
				// Delete the item
				PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?");
				statement.setInt(1, _playerId);
				statement.setInt(2, _itemId);
				if (statement.executeUpdate() != 1)
				{
					_log.warning("Error while deleting itemId " + _itemId + " from userId "
							+ _playerId);
				}
				statement.close();
				/*
				 * Yesod: Skill is not stored into database any more. // Delete the skill
				 * statement =
				 * con.prepareStatement("DELETE FROM character_skills WHERE charId=? AND skill_id=?"
				 * ); statement.setInt(1, _playerId); statement.setInt(2, _skillId); if
				 * (statement.executeUpdate() != 1) {
				 * _log.warning("Error while deleting skillId "+ _skillId
				 * +" from userId "+_playerId); }
				 */
				// Restore the karma
				statement = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE charId=?");
				statement.setInt(1, _playerKarma);
				statement.setInt(2, _playerPkKills);
				statement.setInt(3, _playerId);
				if (statement.executeUpdate() != 1)
				{
					_log.warning("Error while updating karma & pkkills for userId " + _playerId);
				}
				
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Could not delete : " + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{
				}
			}
		}
	}
	else
	{
		// either this cursed weapon is in the inventory of someone who has another cursed
		// weapon equipped, OR this cursed weapon is on the ground.
		if ((_player != null) && (_player.getInventory().getItemByItemId(_itemId) != null))
		{
			// Destroy
			L2ItemInstance removedItem = _player.getInventory().destroyItemByItemId("", _itemId, 1, _player, null);
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				if (removedItem.getCount() == 0)
					iu.addRemovedItem(removedItem);
				else
					iu.addModifiedItem(removedItem);
				
				_player.sendPacket(iu);
			}
			else
				_player.sendPacket(new ItemList(_player, true));
			
			_player.broadcastUserInfo();
		}
		// is dropped on the ground
		else if (_item != null)
		{
			_item.decayMe();
			L2World.getInstance().removeObject(_item);
			_log.info(_name + " item has been removed from World.");
		}
	}
	
	// Delete infos from table if any
	CursedWeaponsManager.removeFromDb(_itemId);
	
	SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
	sm.addItemName(_itemId);
	CursedWeaponsManager.announce(sm);
	
	// Reset state
	cancelTask();
	_isActivated = false;
	_isDropped = false;
	_endTime = 0;
	_player = null;
	_playerId = 0;
	_playerKarma = 0;
	_playerPkKills = 0;
	_item = null;
	_nbKills = 0;
}

private void cancelTask()
{
	if (_removeTask != null)
	{
		_removeTask.cancel(true);
		_removeTask = null;
	}
}

private class RemoveTask implements Runnable
{
protected RemoveTask()
{
}

public void run()
{
	if (System.currentTimeMillis() >= getEndTime())
		endOfLife();
}
}

private void dropIt(L2Attackable attackable, L2PcInstance player)
{
	dropIt(attackable, player, null, true);
}

private void dropIt(L2Attackable attackable, L2PcInstance player, L2Character killer, boolean fromMonster)
{
	_isActivated = false;
	
	if (fromMonster)
	{
		_item = attackable.dropItem(player, _itemId, 1);
		_item.setDropTime(0); // Prevent item from being removed by ItemsAutoDestroy
		
		// RedSky and Earthquake
		ExRedSky packet = new ExRedSky(20);
		Earthquake eq = new Earthquake(player.getX(), player.getY(), player.getZ(), 14, 3);
		Broadcast.toAllOnlinePlayers(packet);
		Broadcast.toAllOnlinePlayers(eq);
	}
	else
	{
		_player.dropItem("DieDrop", _item, killer, true);
		_player.setKarma(_playerKarma);
		_player.setPkKills(_playerPkKills);
		_player.setCursedWeaponEquippedId(0);
		removeSkill();
		_player.abortAttack();
		// L2ItemInstance item = _player.getInventory().getItemByItemId(_itemId);
		// _player.getInventory().dropItem("DieDrop", item, _player, null);
		// _player.getInventory().getItemByItemId(_itemId).dropMe(_player, _player.getX(),
		// _player.getY(), _player.getZ());
	}
	
	_isDropped = true;
	SystemMessage sm = new SystemMessage(SystemMessageId.S2_WAS_DROPPED_IN_THE_S1_REGION);
	if (player != null)
		sm.addZoneName(player.getX(), player.getY(), player.getZ()); // Region Name
	else if (_player != null)
		sm.addZoneName(_player.getX(), _player.getY(), _player.getZ()); // Region Name
	else
		sm.addZoneName(killer.getX(), killer.getY(), killer.getZ()); // Region Name
	sm.addItemName(_itemId);
	CursedWeaponsManager.announce(sm); // in the Hot Spring region
}

/*
 * public void cursedOnLogin() { doTransform(); giveSkill();
 * 
 * SystemMessage msg = new
 * SystemMessage(SystemMessageId.S2_OWNER_HAS_LOGGED_INTO_THE_S1_REGION);
 * msg.addZoneName(_player.getX(), _player.getY(), _player.getZ());
 * msg.addItemName(_player.getCursedWeaponEquippedId()); CursedWeaponsManager.announce(msg);
 * 
 * CursedWeapon cw =
 * CursedWeaponsManager.getInstance().getCursedWeapon(_player.getCursedWeaponEquippedId());
 * SystemMessage msg2 = new
 * SystemMessage(SystemMessageId.S2_MINUTE_OF_USAGE_TIME_ARE_LEFT_FOR_S1); int timeLeftInHours =
 * (int)(((cw.getTimeLeft()/60000)/60)); msg2.addItemName(_player.getCursedWeaponEquippedId());
 * msg2.addNumber(timeLeftInHours*60); _player.sendPacket(msg2); }
 */

/**
 * Yesod:<br>
 * Rebind the passive skill belonging to the CursedWeapon. Invoke this method if the weapon
 * owner switches to a subclass.
 */
public void giveSkill()
{
	int level = 1 + (_nbKills / _stageKills);
	if (level > _skillMaxLevel)
		level = _skillMaxLevel;
	
	L2Skill skill = SkillTable.getInstance().getInfo(_skillId, level);
	// Yesod:
	// To properly support subclasses this skill can not be stored.
	_player.addSkill(skill, false);
	
	skill = SkillTable.getInstance().getInfo(3630, 1); // void burst
	_player.addSkill(skill, false);
	skill = SkillTable.getInstance().getInfo(3631, 1); // void flow
	_player.addSkill(skill, false);
	skill = SkillTable.getInstance().getInfo(3330, 1); // air blade
	_player.addSkill(skill, false);
	skill = SkillTable.getInstance().getInfo(3331, 1); // enslave
	_player.addSkill(skill, false);
	skill = SkillTable.getInstance().getInfo(9011, 1); // void impulse
	_player.addSkill(skill, false);
	
	if (_itemId == ZARICHE)
		skill = SkillTable.getInstance().getInfo(3329, 1); // Power slash
	else
		skill = SkillTable.getInstance().getInfo(3328, 1); // Double stab
	
	_player.addSkill(skill, false);
	
	if (Config.DEBUG)
		_log.info("Player " + _player.getName() + " has been awarded with skill " + skill);
	_player.sendSkillList();
}

public void doTransform()
{
	if (_itemId == 8689)
	{
		transformationId = 302;
	}
	else if (_itemId == 8190)
	{
		transformationId = 301;
	}
	
	if (_player.isTransformed() || _player.isInStance())
	{
		_player.stopTransformation(null);
		
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				TransformationManager.getInstance().transformPlayer(transformationId, _player);
			}
		}, 600);
	}
	else
		TransformationManager.getInstance().transformPlayer(transformationId, _player);
}

public void removeSkill()
{
	_player.removeSkill(_skillId, true);
	_player.removeSkill(3630, true);
	_player.removeSkill(3631, true);
	_player.removeSkill(3328, true);
	_player.removeSkill(3329, true);
	_player.removeSkill(3330, true);
	_player.removeSkill(3331, true);
	_player.removeSkill(9011, true);
	_player.untransform();
	
	/*	if (_player.transformId() > 0)
	{
		TransformationManager.getInstance().transformPlayer(_player.transformId(), _player);
	}*/
}

// =========================================================
// Public
public void reActivate()
{
	_isActivated = true;
	if (_endTime - System.currentTimeMillis() <= 0)
		endOfLife();
	else
		_removeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RemoveTask(), _durationLost * 12000L, _durationLost * 12000L);
	
}

public boolean checkDrop(L2Attackable attackable, L2PcInstance player)
{
	if (player.isInSgradeZone() || attackable.getLevel() < 76 || player.getInstanceId() != 0)
		return false;
	
	if (Rnd.get(100000) < _dropRate)
	{
		// Drop the item
		dropIt(attackable, player);
		
		// Start the Life Task
		_endTime = System.currentTimeMillis() + _duration * 60000L;
		_removeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RemoveTask(), _durationLost * 12000L, _durationLost * 12000L);
		
		return true;
	}
	
	return false;
}

public void activate(L2PcInstance player, L2ItemInstance item)
{
	// if the player is mounted, attempt to unmount first. Only allow picking up the zariche if unmounting is successful.
	if (player.isMounted())
	{
		if (!player.dismount())
		{	player.sendMessage("You may not pick up this item while riding in this territory");
		player.dropItem("InvDrop", item, null, true);
		return;
		}
	}
	
	if ((player._inEventTvT && !Config.TVT_JOIN_CURSED) || (player._inEventCTF && !Config.CTF_JOIN_CURSED))
	{
		if (player._inEventTvT)
			TvT.removePlayer(player);
		if (player._inEventFOS)
			FOS.removePlayer(player);
		if (player._inEventFOS)
			NewFOS.removePlayer(player);
		if (player._inEventCTF)
			CTF.removePlayer(player);
		if (player._inEventCTF)
			NewCTF.removePlayer(player);
		if(player._inEventHG)
			NewHuntingGrounds.removePlayer(player);
	}
	
	_isActivated = true;
	
	// Player holding it data
	_player = player;
	_playerId = _player.getObjectId();
	_playerKarma = _player.getKarma();
	_playerPkKills = _player.getPkKills();
	
	// Change player stats
	_player.setCursedWeaponEquippedId(_itemId);
	_player.setKarma(9999999);
	_player.setPkKills(0);
	if (_player.isInParty())
		_player.getParty().removePartyMember(_player);
	
	// Disable All Skills Do Transform
	doTransform();
	// Add skill
	giveSkill();
	
	// Equip with the weapon
	_item = item;
	// L2ItemInstance[] items =
	_player.getInventory().equipItemAndRecord(_item);
	SystemMessage sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
	sm.addItemName(_item);
	_player.sendPacket(sm);
	
	// Fully heal player
	_player.setCurrentHpMp(_player.getMaxHp(), _player.getMaxMp());
	_player.setCurrentCp(_player.getMaxCp());
	
	// Refresh inventory
	if (!Config.FORCE_INVENTORY_UPDATE)
	{
		InventoryUpdate iu = new InventoryUpdate();
		iu.addItem(_item);
		// iu.addItems(Arrays.asList(items));
		_player.sendPacket(iu);
	}
	else
		_player.sendPacket(new ItemList(_player, false));
	
	// Refresh player stats
	_player.broadcastUserInfo();
	
	SocialAction atk = new SocialAction(_player.getObjectId(), 17);
	
	_player.broadcastPacket(atk);
	
	sm = new SystemMessage(SystemMessageId.THE_OWNER_OF_S2_HAS_APPEARED_IN_THE_S1_REGION);
	sm.addZoneName(_player.getX(), _player.getY(), _player.getZ()); // Region Name
	sm.addItemName(_item);
	CursedWeaponsManager.announce(sm);
}

public void dropIt(L2Character killer)
{
	if (Rnd.get(100) <= _disapearChance)
	{
		// Remove it
		endOfLife();
	}
	else
	{
		// Unequip & Drop
		dropIt(null, null, killer, false);
		// Reset player stats
		_player.setKarma(_playerKarma);
		_player.setPkKills(_playerPkKills);
		_player.setCursedWeaponEquippedId(0);
		_player.abortAttack();
		_player.abortCast();
		
		removeSkill();
		
		// Unequip weapon
		// _player.getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LRHAND);
		
		_player.broadcastUserInfo();
	}
}

public void increaseKills()
{
	_nbKills++;
	
	if (_player != null && _player.isOnline() > 0)
	{
		_player.setPkKills(_nbKills);
		_player.sendPacket(new UserInfo(_player));
		
		if (_nbKills % _stageKills == 0 && _nbKills <= _stageKills * (_skillMaxLevel - 1))
		{
			giveSkill();
		}
	}
	// Reduce time-to-live
	_endTime -= _durationLost * 30000L;
}

// =========================================================
// Setter
public void setDisapearChance(int disapearChance)
{
	_disapearChance = disapearChance;
}

public void setDropRate(int dropRate)
{
	_dropRate = dropRate;
}

public void setDuration(int duration)
{
	_duration = duration;
}

public void setDurationLost(int durationLost)
{
	_durationLost = durationLost;
}

public void setStageKills(int stageKills)
{
	_stageKills = stageKills;
}

public void setNbKills(int nbKills)
{
	_nbKills = nbKills;
}

public void setPlayerId(int playerId)
{
	_playerId = playerId;
}

public void setPlayerKarma(int playerKarma)
{
	_playerKarma = playerKarma;
}

public void setPlayerPkKills(int playerPkKills)
{
	_playerPkKills = playerPkKills;
}

public void setActivated(boolean isActivated)
{
	_isActivated = isActivated;
}

public void setDropped(boolean isDropped)
{
	_isDropped = isDropped;
}

public void setEndTime(long endTime)
{
	_endTime = endTime;
}

public void setPlayer(L2PcInstance player)
{
	_player = player;
}

public void setItem(L2ItemInstance item)
{
	_item = item;
}

// =========================================================
// Getter
public boolean isActivated()
{
	return _isActivated;
}

public boolean isDropped()
{
	return _isDropped;
}

public long getEndTime()
{
	return _endTime;
}

public String getName()
{
	return _name;
}

public int getItemId()
{
	return _itemId;
}

public int getSkillId()
{
	return _skillId;
}

public int getPlayerId()
{
	return _playerId;
}

public L2PcInstance getPlayer()
{
	return _player;
}

public int getPlayerKarma()
{
	return _playerKarma;
}

public int getPlayerPkKills()
{
	return _playerPkKills;
}

public int getNbKills()
{
	return _nbKills;
}

public int getStageKills()
{
	return _stageKills;
}

public boolean isActive()
{
	return _isActivated || _isDropped;
}

public int getLevel()
{
	if (_nbKills > _stageKills * _skillMaxLevel)
	{
		return _skillMaxLevel;
	}
	else
	{
		return (_nbKills / _stageKills);
	}
}

public long getTimeLeft()
{
	return _endTime - System.currentTimeMillis();
}

public void goTo(L2PcInstance player)
{
	if (player == null)
		return;
	
	if (_isActivated && _player != null)
	{
		// Go to player holding the weapon
		player.teleToLocation(_player.getX(), _player.getY(), _player.getZ() + 20, true);
	}
	else if (_isDropped && _item != null)
	{
		// Go to item on the ground
		player.teleToLocation(_item.getX(), _item.getY(), _item.getZ() + 20, true);
	}
	else
	{
		player.sendMessage(_name + " isn't in the World.");
	}
}

public Point3D getWorldPosition()
{
	if (_isActivated && _player != null)
		return _player.getPosition().getWorldPosition();
	
	if (_isDropped && _item != null)
		return _item.getPosition().getWorldPosition();
	
	return null;
}
}
