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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2PetData;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.stat.PetStat;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.itemcontainer.PetInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.PetInventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PetItemList;
import net.sf.l2j.gameserver.network.serverpackets.PetStatusShow;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.util.Rnd;

/**
 *
 * This class ...
 *
 * @version $Revision: 1.15.2.10.2.16 $ $Date: 2005/04/06 16:13:40 $
 */
public class L2PetInstance extends L2Summon
{
protected static final Logger _logPet = Logger.getLogger(L2PetInstance.class.getName());

private int _curFed;
private final PetInventory _inventory;
private final int _controlItemId;
private boolean _respawned;
private final boolean _mountable;
private Future<?> _feedTask;
private int _weapon;
private int _armor;
private int _jewel;
private L2PetData _data;

/** The Experience before the last Death Penalty */
private long _expBeforeDeath = 0;

private static final int PET_DECAY_DELAY = 86400000; // 24 hours
private int _maxload;

public final L2PetData getPetData()
{
	if (_data == null)
		_data = L2PetDataTable.getInstance().getPetData(getTemplate().npcId, getStat().getLevel());
	
	return _data;
}

public final void setPetData(L2PetData value) { _data = value; }

/**
 * Manage Feeding Task.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR>
 * <li>Feed or kill the pet depending on hunger level</li>
 * <li>If pet has food in inventory and feed level drops below 55% then consume food from inventory</li>
 * <li>Send a broadcastStatusUpdate packet for this L2PetInstance</li><BR><BR>
 *
 */

class FeedTask implements Runnable
{
public void run()
{
	try
	{
		if (getOwner() == null || getOwner().getPet() == null || getOwner().getPet().getObjectId() != getObjectId())
		{
			stopFeed();
			return;
		}
		else if (getCurrentFed() > getFeedConsume())
		{
			// eat
			setCurrentFed(getCurrentFed() - getFeedConsume());
		}
		else
		{
			setCurrentFed(0);
		}
		
		int[] foodIds = L2PetDataTable.getFoodItemId(getTemplate().npcId);
		if (foodIds[0] == 0) return;
		L2ItemInstance food = null;
		food = getInventory().getItemByItemId(foodIds[0]);
		
		// use better strider food if exists
		if (L2PetDataTable.isStrider(getNpcId()))
		{
			if (getInventory().getItemByItemId(foodIds[1]) != null)
				food = getInventory().getItemByItemId(foodIds[1]);
		}
		if (isRunning() && isHungry())
		{
			setWalking();
		}
		else if (!isHungry() && !isRunning())
		{
			setRunning();
		}
		if (food != null && isHungry())
		{
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(food.getEtcItem());
			if (handler != null)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
				sm.addItemName(food.getItemId());
				getOwner().sendPacket(sm);
				handler.useItem(L2PetInstance.this, food, true);
			}
		}
		else
		{
			if (getCurrentFed() == 0)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_IS_VERY_HUNGRY);
				getOwner().sendPacket(sm);
				if (Rnd.get(100) < 30)
				{
					stopFeed();
					sm = new SystemMessage(SystemMessageId.STARVING_GRUMPY_AND_FED_UP_YOUR_PET_HAS_LEFT);
					getOwner().sendPacket(sm);
					_log.info("Hungry pet deleted for player :"+getOwner().getName()+" Control Item Id :"+getControlItemId());
					deleteMe(getOwner());
				}
			}
			else if (getCurrentFed() < (0.11 * getPetData().getPetMaxFeed()))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.PET_CAN_RUN_AWAY_WHEN_HUNGER_BELOW_10_PERCENT);
				getOwner().sendPacket(sm);
				/*	if (Rnd.get(100) < 3)
						{
							stopFeed();
							sm = new SystemMessage(SystemMessageId.STARVING_GRUMPY_AND_FED_UP_YOUR_PET_HAS_LEFT);
							getOwner().sendPacket(sm);
							_log.info("Hungry pet deleted for player :"+getOwner().getName()+" Control Item Id :"+getControlItemId());
							deleteMe(getOwner());
						}*/
			}
			broadcastStatusUpdate();
		}
	}
	catch (Exception e)
	{
		_logPet.log(Level.SEVERE, "Pet [ObjectId: "+getObjectId()+"] a feed task error has occurred", e);
	}
}

/**
 * @return
 */
private int getFeedConsume()
{
	// if pet is attacking
	if (isAttackingNow())
		return getPetData().getPetFeedBattle();
	else
		return getPetData().getPetFeedNormal();
}
}

public synchronized static L2PetInstance spawnPet(L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
{
	if (L2World.getInstance().getPet(owner.getObjectId()) != null)
		return null; // owner has a pet listed in world
	
	L2PetInstance pet = restore(control, template, owner);
	// add the pet instance to world
	if (pet != null)
	{
		pet.setTitle(owner.getDisplayName());
		L2World.getInstance().addPet(owner.getObjectId(), pet);
	}
	
	return pet;
}

public L2PetInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
{
	super(objectId, template, owner);
	
	_controlItemId = control.getObjectId();
	
	// Pet's initial level is supposed to be read from DB
	// Pets start at :
	// Wolf : Level 15
	// Hatcling : Level 35
	// Tested and confirmed on official servers
	// Sin-eaters are defaulted at the owner's level
	if (template.npcId == 12564)
		getStat().setLevel((byte)getOwner().getLevel());
	else
		getStat().setLevel(template.level);
	
	_inventory = new PetInventory(this);
	_inventory.restore();
	
	int npcId = template.npcId;
	_mountable = L2PetDataTable.isMountable(npcId);
	_maxload = getPetData().getPetMaxLoad();
}

@Override
public PetStat getStat()
{
	return (PetStat)super.getStat();
}

@Override
public void initCharStat()
{
	setStat(new PetStat(this));
}

@Override
public double getLevelMod() { return (100.0 - 11 + getLevel()) / 100.0; }

public boolean isRespawned() { return _respawned; }

@Override
public int getSummonType() { return 2; }

@Override
public void onAction(L2PcInstance player)
{
	// Aggression target lock effect
	if (player.isLockedTarget() && player.getLockedTarget() != this)
	{
		player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	boolean isOwner = player.getObjectId() == getOwner().getObjectId();
	
	player.sendPacket(new ValidateLocation(this));
	if(isOwner && player != getOwner())
		updateRefOwner(player);
	if (this != player.getTarget())
	{
		if (Config.DEBUG)
			_log.fine("new target selected:" + getObjectId());
		
		// Set the target of the L2PcInstance player
		player.setTarget(this);
		
		MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
		player.sendPacket(my);
		
		// Send a Server->Client packet StatusUpdate of the L2PetInstance to the L2PcInstance to update its HP bar
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		player.sendPacket(su);
	}
	else
	{
		// Check if the pet is attackable (without a forced attack) and isn't dead
		if (isAutoAttackable(player) && !isOwner)
		{
			if (Config.GEODATA > 0)
			{
				if (GeoData.getInstance().canSeeTarget(player, this))
				{
					// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				}
			}
			else
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
		}
		else if (!isInsideRadius(player, 150, false, false))
		{
			if (Config.GEODATA > 0)
			{
				if (GeoData.getInstance().canSeeTarget(player, this))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
			}
			else
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
		}
		else
		{
			if (isOwner)
				player.sendPacket(new PetStatusShow(this));
		}
	}
	player.sendPacket(ActionFailed.STATIC_PACKET);
}

@Override
public int getControlItemId() { return _controlItemId; }

public L2ItemInstance getControlItem()
{
	return getOwner().getInventory().getItemByObjectId(_controlItemId);
}

public int getCurrentFed() { return _curFed; }

public void setCurrentFed(int num) { _curFed = num > getMaxFed() ? getMaxFed() : num; }

/**
 * Returns the pet's currently equipped weapon instance (if any).
 */
@Override
public L2ItemInstance getActiveWeaponInstance()
{
	for (L2ItemInstance item : getInventory().getItems())
		if (item.getItemLocation() == L2ItemInstance.ItemLocation.PET_EQUIP &&
		item.getItem().getBodyPart() == L2Item.SLOT_R_HAND)
			return item;
	
	return null;
}

/**
 * Returns the pet's currently equipped weapon (if any).
 */
@Override
public L2Weapon getActiveWeaponItem()
{
	L2ItemInstance weapon = getActiveWeaponInstance();
	
	if (weapon == null)
		return null;
	
	return (L2Weapon)weapon.getItem();
}

@Override
public L2ItemInstance getSecondaryWeaponInstance()
{
	// temporary? unavailable
	return null;
}

@Override
public L2Weapon getSecondaryWeaponItem()
{
	// temporary? unavailable
	return null;
}

@Override
public PetInventory getInventory()
{
	return _inventory;
}

/**
 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
 * @param process : String Identifier of process triggering this action
 * @param objectId : int Item Instance identifier of the item to be destroyed
 * @param count : int Quantity of items to be destroyed
 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
 * @param sendMessage : boolean Specifies whether to send message to Client about this action
 * @return boolean informing if the action was successfull
 */
@Override
public boolean destroyItem(String process, int objectId, long count, L2Object reference, boolean sendMessage)
{
	L2ItemInstance item = _inventory.destroyItem(process, objectId, count, getOwner(), reference);
	
	if (item == null)
	{
		if (sendMessage)
			getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
		
		return false;
	}
	
	// Send Pet inventory update packet
	PetInventoryUpdate petIU = new PetInventoryUpdate();
	petIU.addItem(item);
	getOwner().sendPacket(petIU);
	
	if (sendMessage)
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
		sm.addItemName(item.getItemId());
		sm.addItemNumber(count);
		getOwner().sendPacket(sm);
	}
	return true;
}

/**
 * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
 * @param process : String Identifier of process triggering this action
 * @param itemId : int Item identifier of the item to be destroyed
 * @param count : int Quantity of items to be destroyed
 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
 * @param sendMessage : boolean Specifies whether to send message to Client about this action
 * @return boolean informing if the action was successfull
 */
@Override
public boolean destroyItemByItemId(String process, int itemId, long count, L2Object reference, boolean sendMessage)
{
	L2ItemInstance item = _inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference);
	
	if (item == null)
	{
		if (sendMessage) getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
		return false;
	}
	
	// Send Pet inventory update packet
	PetInventoryUpdate petIU = new PetInventoryUpdate();
	petIU.addItem(item);
	getOwner().sendPacket(petIU);
	
	if (sendMessage)
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
		sm.addItemName(itemId);
		sm.addItemNumber(count);
		getOwner().sendPacket(sm);
	}
	
	return true;
}

@Override
protected void doPickupItem(L2Object object)
{
	boolean follow = getFollowStatus();
	if (isDead()) return;
	getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
	StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());
	
	if (Config.DEBUG)
		_logPet.fine("Pet pickup pos: "+ object.getX() + " "+object.getY()+ " "+object.getZ() );
	
	broadcastPacket(sm);
	
	if (!(object instanceof L2ItemInstance))
	{
		// dont try to pickup anything that is not an item :)
		_logPet.warning("trying to pickup wrong target."+object);
		getOwner().sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	L2ItemInstance target = (L2ItemInstance) object;
	
	// Cursed weapons
	if ( CursedWeaponsManager.getInstance().isCursed(target.getItemId()) )
	{
		SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
		smsg.addItemName(target.getItemId());
		getOwner().sendPacket(smsg);
		return;
	}
	
	synchronized (target)
	{
		if (!target.isVisible())
		{
			getOwner().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if ( !_inventory.validateCapacity(target))
		{
			getOwner().sendPacket(new SystemMessage(SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS));
			return;
		}
		if ( !_inventory.validateWeight(target, target.getCount()))
		{
			getOwner().sendPacket(new SystemMessage(SystemMessageId.UNABLE_TO_PLACE_ITEM_YOUR_PET_IS_TOO_ENCUMBERED));
			return;
		}
		if (target.getOwnerId() != 0 && target.getOwnerId() != getOwner().getObjectId() && !getOwner().isInLooterParty(target.getOwnerId()))
		{
			getOwner().sendPacket(ActionFailed.STATIC_PACKET);
			
			if (target.getItemId() == 57)
			{
				SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
				smsg.addItemNumber(target.getCount());
				getOwner().sendPacket(smsg);
			}
			else if (target.getCount() > 1)
			{
				SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
				smsg.addItemName(target.getItemId());
				smsg.addItemNumber(target.getCount());
				getOwner().sendPacket(smsg);
			}
			else
			{
				SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target.getItemId());
				getOwner().sendPacket(smsg);
			}
			
			return;
		}
		if(target.getItemLootShedule() != null
				&& (target.getOwnerId() == getOwner().getObjectId() || getOwner().isInLooterParty(target.getOwnerId())))
			target.resetOwnerTimer();
		
		target.pickupMe(this);
		
		if(Config.SAVE_DROPPED_ITEM) // item must be removed from ItemsOnGroundManager if is active
			ItemsOnGroundManager.getInstance().removeObject(target);
	}
	
	// Herbs
	if (target.getItemType() == L2EtcItemType.HERB)
	{
		IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getEtcItem());
		if (handler == null)
			_log.fine("No item handler registered for item ID " + target.getItemId() + ".");
		else
			handler.useItem(this, target, true);
		
		ItemTable.getInstance().destroyItem("Consume", target, getOwner(), null);
		
		broadcastStatusUpdate();
		
		
		// Commented out and moved below synchronized block by DrHouse.
		// It seems that pets get full effect of herbs when picking them up by themselves
		/*
			SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
            smsg.addItemName(target.getItemId());
            getOwner().sendPacket(smsg);
			return;
		 */
	}
	else
	{
		if (target.getItemId() == 57)
		{
			SystemMessage sm2 = new SystemMessage(SystemMessageId.PET_PICKED_S1_ADENA);
			sm2.addItemNumber(target.getCount());
			getOwner().sendPacket(sm2);
		}
		else if (target.getEnchantLevel() > 0)
		{
			SystemMessage sm2 = new SystemMessage(SystemMessageId.PET_PICKED_S1_S2);
			sm2.addNumber(target.getEnchantLevel());
			sm2.addString(target.getName());
			getOwner().sendPacket(sm2);
		}
		else if (target.getCount() > 1)
		{
			SystemMessage sm2 = new SystemMessage(SystemMessageId.PET_PICKED_S2_S1_S);
			sm2.addItemNumber(target.getCount());
			sm2.addString(target.getName());
			getOwner().sendPacket(sm2);
		}
		else
		{
			SystemMessage sm2 = new SystemMessage(SystemMessageId.PET_PICKED_S1);
			sm2.addString(target.getName());
			getOwner().sendPacket(sm2);
		}
		getInventory().addItem("Pickup", target, getOwner(), this);
		//FIXME Just send the updates if possible (old way wasn't working though)
		PetItemList iu = new PetItemList(this);
		getOwner().sendPacket(iu);
	}
	
	getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
	
	if (follow)
		followOwner();
}

@Override
public void deleteMe(L2PcInstance owner)
{
	super.deleteMe(owner);
	destroyControlItem(owner, false); //this should also delete the pet from the db
}

@Override
public boolean doDie(L2Character killer)
{
	if (!super.doDie(killer,true))
		return false;
	stopFeed();
	getOwner().sendPacket(new SystemMessage(SystemMessageId.MAKE_SURE_YOU_RESSURECT_YOUR_PET_WITHIN_24_HOURS));
	DecayTaskManager.getInstance().addDecayTask(this, PET_DECAY_DELAY);
	deathPenalty();
	return true;
}

@Override
public void doRevive()
{
	getOwner().removeReviving();
	
	super.doRevive();
	
	// stopDecay
	DecayTaskManager.getInstance().cancelDecayTask(this);
	startFeed();
	if (!isHungry())
		setRunning();
	getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
}

@Override
public void doRevive(double revivePower)
{
	// Restore the pet's lost experience,
	// depending on the % return of the skill used (based on its power).
	restoreExp(revivePower);
	doRevive();
}

/**
 * Transfers item to another inventory
 * @param process : String Identifier of process triggering this action
 * @param itemId : int Item Identifier of the item to be transfered
 * @param count : int Quantity of items to be transfered
 * @param actor : L2PcInstance Player requesting the item transfer
 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
 */
public L2ItemInstance transferItem(String process, int objectId, long count, Inventory target, L2PcInstance actor, L2Object reference)
{
	L2ItemInstance oldItem = getInventory().getItemByObjectId(objectId);
	L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, actor, reference);
	
	if (newItem == null) return null;
	
	// Send inventory update packet
	PetInventoryUpdate petIU = new PetInventoryUpdate();
	if (oldItem.getCount() > 0 && oldItem != newItem) petIU.addModifiedItem(oldItem);
	else petIU.addRemovedItem(oldItem);
	getOwner().sendPacket(petIU);
	
	// Send target update packet
	if (target instanceof PcInventory)
	{
		L2PcInstance targetPlayer = ((PcInventory)target).getOwner();
		InventoryUpdate playerUI = new InventoryUpdate();
		if (newItem.getCount() > count) playerUI.addModifiedItem(newItem);
		else playerUI.addNewItem(newItem);
		targetPlayer.sendPacket(playerUI);
		
		// Update current load as well
		StatusUpdate playerSU = new StatusUpdate(targetPlayer.getObjectId());
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
		targetPlayer.sendPacket(playerSU);
	}
	else if (target instanceof PetInventory)
	{
		petIU = new PetInventoryUpdate();
		if (newItem.getCount() > count) petIU.addRemovedItem(newItem);
		else petIU.addNewItem(newItem);
		((PetInventory)target).getOwner().getOwner().sendPacket(petIU);
	}
	return newItem;
}

@Override
public void giveAllToOwner()
{
	try
	{
		Inventory petInventory = getInventory();
		for (L2ItemInstance giveit: petInventory.getItems())
		{
			if (((giveit.getItem().getWeight() * giveit.getCount())
					+ getOwner().getInventory().getTotalWeight())
					< getOwner().getMaxLoad())
			{
				// If the owner can carry it give it to them
				giveItemToOwner(giveit);
			}
			else
			{
				// If they can't carry it, chuck it on the floor :)
				dropItemHere(giveit);
			}
		}
	}
	catch(Exception e)
	{
		_logPet.warning("Give all items error " + e);
	}
}

public void giveItemToOwner(L2ItemInstance item)
{
	try
	{
		getInventory().transferItem("PetTransfer", item.getObjectId(), item.getCount(), getOwner().getInventory(), getOwner(), this);
		PetInventoryUpdate petiu = new PetInventoryUpdate();
		ItemList PlayerUI = new ItemList(getOwner(), false);
		petiu.addRemovedItem(item);
		getOwner().sendPacket(petiu);
		getOwner().sendPacket(PlayerUI);
	}
	catch (Exception e)
	{
		_logPet.warning("Error while giving item to owner: " + e);
	}
}

/**
 * Remove the Pet from DB and its associated item from the player inventory
 * @param owner The owner from whose invenory we should delete the item
 */
public void destroyControlItem(L2PcInstance owner, boolean evolve)
{
	// remove the pet instance from world
	L2World.getInstance().removePet(owner.getObjectId());
	
	// delete from inventory
	try
	{
		L2ItemInstance removedItem;
		if (evolve)
		{
			removedItem = owner.getInventory().destroyItem("Evolve", getControlItemId(), 1, getOwner(), this);
		}
		else
		{
			removedItem = owner.getInventory().destroyItem("PetDestroy", getControlItemId(), 1, getOwner(), this);
			owner.sendPacket(new SystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(removedItem));
		}
		
		InventoryUpdate iu = new InventoryUpdate();
		iu.addRemovedItem(removedItem);
		
		owner.sendPacket(iu);
		
		StatusUpdate su = new StatusUpdate(owner.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, owner.getCurrentLoad());
		owner.sendPacket(su);
		
		owner.broadcastUserInfo();
		
		L2World world = L2World.getInstance();
		world.removeObject(removedItem);
	}
	catch (Exception e){
		_logPet.warning("Error while destroying control item: " + e);
	}
	
	// pet control item no longer exists, delete the pet from the db
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
		statement.setInt(1, getControlItemId());
		statement.execute();
		statement.close();
	}
	catch (Exception e)
	{
		_logPet.log(Level.SEVERE, "Failed to delete Pet [ObjectId: "+getObjectId()+"]", e);
	}
	finally
	{
		try { con.close(); } catch (Exception e) {}
	}
}

public void dropAllItems()
{
	try
	{
		for (L2ItemInstance item: getInventory().getItems())
		{
			dropItemHere(item);
		}
	}
	catch(Exception e)
	{
		_logPet.warning("Pet Drop Error: " + e);
	}
}

public void dropItemHere(L2ItemInstance dropit)
{
	dropit = getInventory().dropItem("Drop", dropit.getObjectId(), dropit.getCount(), getOwner(), this);
	
	if (dropit != null)
	{
		_logPet.finer("Item id to drop: "+dropit.getItemId()+" amount: "+dropit.getCount());
		dropit.dropMe(this, getX(), getY(), getZ()+100);
	}
}

//	public void startAttack(L2Character target)
//	{
//		if (!knownsObject(target))
//		{
//			target.addKnownObject(this);
//			this.addKnownObject(target);
//		}
//        if (!target.knownsObject(this))
//        {
//            target.addKnownObject(this);
//            this.addKnownObject(target);
//        }
//
//		if (!isRunning())
//		{
//			setRunning(true);
//			ChangeMoveType move = new ChangeMoveType(this, ChangeMoveType.RUN);
//			broadcastPacket(move);
//		}
//
//		super.startAttack(target);
//	}
//
/** @return Returns the mount able. */
@Override
public boolean isMountable() { return _mountable; }

private static L2PetInstance restore(L2ItemInstance control, L2NpcTemplate template, L2PcInstance owner)
{
	Connection con = null;
	try
	{
		L2PetInstance pet;
		if (template.type.compareToIgnoreCase("L2BabyPet")==0)
			pet = new L2BabyPetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
		else
			pet = new L2PetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
		
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, fed, weapon, armor, jewel FROM pets WHERE item_obj_id=?");
		statement.setInt(1, control.getObjectId());
		ResultSet rset = statement.executeQuery();
		if (!rset.next())
		{
			rset.close();
			statement.close();
			return pet;
		}
		
		pet._respawned = true;
		pet.setName(rset.getString("name"));
		
		byte level = rset.getByte("level");
		
		if (level < 27)
			level = 27;
		
		pet.getStat().setLevel(level);
		pet.getStat().setExp(rset.getLong("exp"));
		pet.getStat().setSp(rset.getInt("sp"));
		
		pet.getStatus().setCurrentHp(rset.getDouble("curHp"));
		pet.getStatus().setCurrentMp(rset.getDouble("curMp"));
		pet.getStatus().setCurrentCp(pet.getMaxCp());
		pet.setWeapon(rset.getInt("weapon"));
		pet.setArmor(rset.getInt("armor"));
		pet.setJewel(rset.getInt("jewel"));
		if (rset.getDouble("curHp") < 0.5)
		{
			pet.setIsDead(true);
			pet.stopHpMpRegeneration();
		}
		
		pet.setCurrentFed(rset.getInt("fed"));
		
		rset.close();
		statement.close();
		return pet;
	} catch (Exception e)
	{
		/*		_logPet.warning("could not restore pet data: "+ e);*/
		return null;
	} finally {
		try { con.close(); } catch (Exception e) {}
	}
}

@Override
public void store()
{
	if (getControlItemId() == 0)
	{
		// this is a summon, not a pet, don't store anything
		return;
	}
	
	String req;
	if (!isRespawned())
		req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,fed,weapon,armor,jewel,item_obj_id) "+
				"VALUES (?,?,?,?,?,?,?,?,?,?,?)";
	else
		req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,fed=?,weapon=?,armor=?,jewel=? "+
				"WHERE item_obj_id = ?";
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement(req);
		statement.setString(1, getName());
		statement.setInt(2, getStat().getLevel());
		statement.setDouble(3, getStatus().getCurrentHp());
		statement.setDouble(4, getStatus().getCurrentMp());
		statement.setLong(5, getStat().getExp());
		statement.setInt(6, getStat().getSp());
		statement.setInt(7, getCurrentFed());
		statement.setInt(8, getWeapon());
		statement.setInt(9, getArmor());
		statement.setInt(10, getJewel());
		statement.setInt(11, getControlItemId());
		statement.executeUpdate();
		statement.close();
		_respawned = true;
	}
	catch (Exception e)
	{
		_logPet.log(Level.SEVERE, "Failed to store Pet [ObjectId: "+getObjectId()+"] data", e);
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
	
	L2ItemInstance itemInst = getControlItem();
	if (itemInst != null && itemInst.getEnchantLevel() != getStat().getLevel())
	{
		itemInst.setEnchantLevel(getStat().getLevel());
		itemInst.updateDatabase();
	}
}

public synchronized void stopFeed()
{
	if (_feedTask != null)
	{
		_feedTask.cancel(false);
		_feedTask = null;
		if (Config.DEBUG) _logPet.fine("Pet [#"+getObjectId()+"] feed task stop");
	}
}

public synchronized void startFeed()
{
	// stop feeding task if its active
	
	stopFeed();
	if (!isDead() && getOwner().getPet() == this)
	{
		_feedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
	}
}

@Override
public synchronized void unSummon (L2PcInstance owner)
{
	stopFeed();
	stopHpMpRegeneration();
	super.unSummon(owner);
	
	if(!isDead())
		L2World.getInstance().removePet(owner.getObjectId());
}

/**
 * Restore the specified % of experience this L2PetInstance has lost.<BR><BR>
 */
public void restoreExp(double restorePercent)
{
	if (_expBeforeDeath > 0)
	{
		// Restore the specified % of lost experience.
		getStat().addExp(Math.round((_expBeforeDeath - getStat().getExp()) * restorePercent / 100));
		_expBeforeDeath = 0;
	}
}


private void deathPenalty()
{
	// TODO Need Correct Penalty
	
	int lvl = getStat().getLevel();
	double percentLost = -0.07 * lvl + 6.5;
	
	// Calculate the Experience loss
	long lostExp = Math.round((getStat().getExpForLevel(lvl+1) - getStat().getExpForLevel(lvl)) * percentLost /100);
	
	// Get the Experience before applying penalty
	_expBeforeDeath = getStat().getExp();
	
	// Set the new Experience value of the L2PetInstance
	getStat().addExp(-lostExp);
}

@Override
public void addExpAndSp(long addToExp, int addToSp)
{
	if (getNpcId() == 12564) //SinEater
		getStat().addExpAndSp(Math.round(addToExp * Config.SINEATER_XP_RATE), addToSp);
	else
		getStat().addExpAndSp(Math.round(addToExp * Config.PET_XP_RATE), addToSp);
}

@Override
public long getExpForThisLevel() { return getStat().getExpForLevel(getLevel()); }
@Override
public long getExpForNextLevel() { return getStat().getExpForLevel(getLevel() + 1); }
@Override
public final int getLevel() { return getStat().getLevel(); }
public int getMaxFed() { return getStat().getMaxFeed(); }
/*    @Override
	public int getAccuracy() { return getStat().getAccuracy(); }*/
@Override
public int getCriticalHit(L2Character target, L2Skill skill) { return getStat().getCriticalHitRate(target, skill); }
@Override
public int getEvasionRate(L2Character target) { return getStat().getEvasionRate(target); }
@Override
public int getRunSpeed() { return getStat().getRunSpeed(); }
@Override
public int getPAtkSpd(L2Skill skill) { return getStat().getPAtkSpd(skill); }
@Override
public int getMAtkSpd(L2Skill skill) { return getStat().getMAtkSpd(skill); }
@Override
public int getMAtk(L2Character target, L2Skill skill) { return getStat().getMAtk(target, skill); }
@Override
public int getMDef(L2Character target, L2Skill skill) { return getStat().getMDef(target, skill); }
@Override
public int getPAtk(L2Character target) { return getStat().getPAtk(target); }
@Override
public int getPDef(L2Character target) { return getStat().getPDef(target); }

@Override
public final int getSkillLevel(int skillId)
{
	if (getKnownSkill(skillId) == null)
		return -1;
	
	final int lvl = getLevel();
	return lvl > 70 ? 7 + (lvl - 70) / 5 : lvl / 10;
}

public void updateRefOwner(L2PcInstance owner)
{
	int oldOwnerId = getOwner().getObjectId();
	
	setOwner(owner);
	L2World.getInstance().removePet(oldOwnerId);
	L2World.getInstance().addPet(oldOwnerId, this);
}

public int getCurrentLoad()
{
	return _inventory.getTotalWeight();
}

public final void setMaxLoad(int maxLoad)
{
	_maxload = maxLoad;
}

@Override
public final int getMaxLoad()
{
	return _maxload;
}
public int getInventoryLimit()
{
	return Config.INVENTORY_MAXIMUM_PET;
}


public void refreshOverloaded()
{
	
}

@Override
public void updateAndBroadcastStatus(int val)
{
	refreshOverloaded();
	super.updateAndBroadcastStatus(val);
}

@Override
public final boolean isHungry()
{
	return getCurrentFed() < (0.55 * getPetData().getPetMaxFeed());
}

public final void setWeapon(int id)
{
	_weapon = id;
}

public final void setArmor(int id)
{
	_armor = id;
}
public final void setJewel(int id)
{
	_jewel = id;
}

@Override
public final int getWeapon()
{
	return _weapon;
}

@Override
public final int getArmor()
{
	return _armor;
}

public final int getJewel()
{
	return _jewel;
}

@Override
public int getPetSpeed()
{
	return getPetData().getPetSpeed();
}


}


