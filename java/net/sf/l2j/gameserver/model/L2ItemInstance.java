package net.sf.l2j.gameserver.model;

import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.ADENA_ID;
import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.MAX_ADENA;
import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.MAX_SCROLL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.MercTicketManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.NullKnownList;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.AbstractRefinePacket;
import net.sf.l2j.gameserver.network.clientpackets.RequestEnchantItem;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.DropItem;
import net.sf.l2j.gameserver.network.serverpackets.GetItem;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SpawnItem;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.templates.item.L2Armor;
import net.sf.l2j.gameserver.templates.item.L2EtcItem;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.GMAudit;
import net.sf.l2j.util.Rnd;

public final class L2ItemInstance extends L2Object
{
	protected static final Logger	_log		= Logger.getLogger(L2ItemInstance.class.getName());
	private static final Logger		_logItems	= Logger.getLogger("item");
	
	/** Enumeration of locations for item */
	public static enum ItemLocation
	{
		VOID,
		INVENTORY,
		PAPERDOLL,
		WAREHOUSE,
		CLANWH,
		PET,
		PET_EQUIP,
		LEASE,
		FREIGHT,
		NPC
	}
	
	/** ID of the owner */
	private int					_ownerId;
	/** ID of who dropped the item last, used for knownlist */
	private int					_dropperObjectId		= 0;
	/** Quantity of the item */
	private long				_count;
	/** Initial Quantity of the item */
	private long				_initCount;
	/** Remaining time (in miliseconds) */
	private long				_time;
	/** Quantity of the item can decrease */
	private boolean				_decrease				= false;
	/** ID of the item */
	private final int			_itemId;
	/** Object L2Item associated to the item */
	private final L2Item		_item;
	/** Location of the item : Inventory, PaperDoll, WareHouse */
	private ItemLocation		_loc;
	/** Slot where item is stored : Paperdoll slot, inventory order ... */
	private int					_locData;
	/** Level of enchantment of the item */
	private int					_enchantLevel;
	/** Wear Item */
	private boolean				_wear;
	/** Augmented Item */
	private L2Augmentation		_augmentation			= null;
	/** Shadow item */
	private int					_mana					= -1;
	private boolean				_consumingMana			= false;
	private static final int	MANA_CONSUMPTION_RATE	= 60000;
	/** Custom item types (used loto, race tickets) */
	private int					_type1;
	private int					_type2;
	private long				_dropTime;
	private boolean				_chargedFishtshot		= false;
	private boolean				_protected;
	public static final int		UNCHANGED				= 0;
	public static final int		ADDED					= 1;
	public static final int		REMOVED					= 3;
	public static final int		MODIFIED				= 2;
	private int					_lastChange				= 2;					// 1 ??, 2 modified, 3 removed
	private boolean				_existsInDb;									// if a record exists in DB.
	private boolean				_storedInDb;									// if DB data is up-to-date.
	public String				_source;
	public String				_instanceDroppedFrom;
	private final ReentrantLock	_dbLock					= new ReentrantLock();
	private Elementals			_elementals				= null;
	private ScheduledFuture<?>	itemLootShedule			= null;
	public ScheduledFuture<?>	_lifeTimeTask;
	private long				_untradeableTime		= 0;
	public int					_visual_item_id;
	
	/**
	 * Constructor of the L2ItemInstance from the objectId and the itemId.
	 * 
	 * @param objectId
	 *            : int designating the ID of the object in the world
	 * @param itemId
	 *            : int designating the ID of the item
	 */
	public L2ItemInstance(int objectId, int itemId)
	{
		super(objectId);
		_itemId = itemId;
		_item = ItemTable.getInstance().getTemplate(itemId);
		if (_itemId == 0 || _item == null)
			throw new IllegalArgumentException();
		super.setName(_item.getName());
		setCount(1);
		_loc = ItemLocation.VOID;
		_type1 = 0;
		_type2 = 0;
		_dropTime = 0;
		_mana = _item.getDuration();
		_source = null;
		_visual_item_id = 0;
		_dropTime = 0;
		_time = _item.getTime() == -1 ? -1 : System.currentTimeMillis() + ((long) _item.getTime() * 60 * 1000);
		scheduleLifeTimeTask();
		if (_item.isHeroItem() && _item instanceof L2Weapon)
			_enchantLevel = 16;
		/*
		 * else if (automaticPlus12())
		 * _enchantLevel = 12;
		 * else if (automaticPlus3())
		 * _enchantLevel = 3;
		 */
	}
	
	public L2ItemInstance(int objectId, int itemId, String process, String from)
	{
		super(objectId);
		_itemId = itemId;
		_item = ItemTable.getInstance().getTemplate(itemId);
		if (_itemId == 0 || _item == null)
			throw new IllegalArgumentException();
		_source = from;
		if (_source == null)
			_source = process;
		int unpermTimer = 0;
		if (_item.getPermChance() > -1)
		{
			int permChance = _item.getPermChance();
			if (process.equalsIgnoreCase("Multisell"))
			{ // default perm chance
			}
			else
			{
				if (process.equalsIgnoreCase("Loot")) // dropped by mobs
				{
					/* permChance = (int) ((permChance + 5)*5.5); */
				}
				else
					permChance = 100; // 100% chance through other means (such as GM spawn)
			}
			final boolean perm;
			if (permChance < 1)
				perm = false;
			else
				perm = Rnd.get(100) < permChance;
			if (!perm)
			{
				if (Rnd.get(10) == 0)
					unpermTimer = 45 * 24 * 60; // 45 days in MINUTES in 1/10 chance
				else
					unpermTimer = 30 * 24 * 60; // 30 days in MINUTES
			}
		}
		// else if (from != null && from.startsWith("pvp npc"))
		// {
		// setUntradeableTimer(9999999900000L);
		// setIsFromPvPNpc(true);
		// }
		else if (from != null && from.startsWith("shadow npc"))
		{
			switch ((int) _item.getUniqueness())
			{
				case 0:
					unpermTimer = 24 * 20 * 60; // 3 day in MINUTES
					break;
				case 1:
					unpermTimer = 24 * 20 * 60; // 2 day in MINUTES
					break;
				case 2:
					unpermTimer = 24 * 20 * 60; // 1 day in MINUTES
					break;
				case 3:
					unpermTimer = 24 * 20 * 60; // 20 hours in MINUTES
					break;
				default:
					unpermTimer = 24 * 20 * 60; // 6 hours in MINUTES
					break;
			}
		}
		else if (from != null && from.startsWith("donation npc"))
		{
			// final long newTime = System.currentTimeMillis() + (Config.UNTRADEABLE_DONATE*60*60*1000);
			// setUntradeableTimer(newTime);
			int locslot = _item.getBodyPart();
			if (!(locslot == L2Item.SLOT_LR_FINGER || locslot == L2Item.SLOT_LR_EAR || locslot == L2Item.SLOT_NECK))
			{
				addAutoAugmentationDonation();
			}
		}
		super.setName(_item.getName());
		setCount(1);
		_loc = ItemLocation.VOID;
		_type1 = 0;
		_type2 = 0;
		_dropTime = 0;
		_mana = _item.getDuration();
		if (unpermTimer > 0)
			_time = System.currentTimeMillis() + ((long) unpermTimer * 60 * 1000);
		else
			_time = _item.getTime() == -1 ? -1 : System.currentTimeMillis() + ((long) _item.getTime() * 60 * 1000);
		scheduleLifeTimeTask();
		if (_item.isHeroItem() && _item instanceof L2Weapon)
			_enchantLevel = 16;
		/*
		 * else if (automaticPlus12())
		 * _enchantLevel = 12;
		 * else if (automaticPlus3())
		 * _enchantLevel = 3;
		 */
	}
	/*
	 * public boolean automaticPlus12()
	 * {
	 * return _item.getItemGrade() < L2Item.CRYSTAL_S80 && _item.getItemGrade() > L2Item.CRYSTAL_NONE && !isARestrictedItem() && _item.getBodyPart() != L2Item.SLOT_UNDERWEAR;
	 * }
	 * public boolean automaticPlus3()
	 * {
	 * return (_item.getBodyPart() == L2Item.SLOT_UNDERWEAR && _item.getCrystalType() < L2Item.CRYSTAL_S80) || _item.getName().contains("Pride");
	 * }
	 */
	
	/**
	 * Constructor of the L2ItemInstance from the objetId and the description of the item given by the L2Item.
	 * 
	 * @param objectId
	 *            : int designating the ID of the object in the world
	 * @param item
	 *            : L2Item containing informations of the item
	 */
	public L2ItemInstance(int objectId, L2Item item)
	{
		super(objectId);
		_itemId = item.getItemId();
		_item = item;
		if (_itemId == 0)
			throw new IllegalArgumentException();
		super.setName(_item.getName());
		setCount(1);
		_loc = ItemLocation.VOID;
		_mana = _item.getDuration();
		_time = _item.getTime() == -1 ? -1 : System.currentTimeMillis() + ((long) _item.getTime() * 60 * 1000);
		scheduleLifeTimeTask();
		if (_item.isHeroItem() && _item instanceof L2Weapon)
			_enchantLevel = 16;
		/*
		 * else if (automaticPlus12())
		 * _enchantLevel = 12;
		 * else if (automaticPlus3())
		 * _enchantLevel = 3;
		 */
		_source = null;
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new NullKnownList(this));
	}
	
	/**
	 * Remove a L2ItemInstance from the world and send server->client GetItem packets.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client Packet GetItem to player that pick up and its _knowPlayers member</li>
	 * <li>Remove the L2Object from the world</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li>this instanceof L2ItemInstance</li>
	 * <li>_worldRegion != null <I>(L2Object is visible at the beginning)</I></li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Do Pickup Item : PCInstance and Pet</li><BR>
	 * <BR>
	 *
	 * @param player
	 *            Player that pick up the item
	 */
	public final void pickupMe(L2Character player)
	{
		if (Config.ASSERT)
			assert getPosition().getWorldRegion() != null;
		L2WorldRegion oldregion = getPosition().getWorldRegion();
		// Create a server->client GetItem packet to pick up the L2ItemInstance
		GetItem gi = new GetItem(this, player.getObjectId());
		player.broadcastPacket(gi);
		synchronized (this)
		{
			setIsVisible(false);
			getPosition().setWorldRegion(null);
		}
		// if this item is a mercenary ticket, remove the spawns!
		int itemId = getItemId();
		if (MercTicketManager.getInstance().getTicketCastleId(itemId) > 0)
		{
			MercTicketManager.getInstance().removeTicket(this);
			ItemsOnGroundManager.getInstance().removeObject(this);
		}
		/*
		 * if (itemId == 57 || itemId == 6353)
		 * {
		 * L2PcInstance actor = player.getActingPlayer();
		 * if (actor != null)
		 * {
		 * QuestState qs = actor.getQuestState("255_Tutorial");
		 * if (qs != null)
		 * qs.getQuest().notifyEvent("CE"+itemId+"",null, actor);
		 * }
		 * }
		 */
		// outside of synchronized to avoid deadlocks
		// Remove the L2ItemInstance from the world
		L2World.getInstance().removeVisibleObject(this, oldregion);
	}
	
	/**
	 * Sets the ownerID of the item
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param owner_id
	 *            : int designating the ID of the owner
	 * @param creator
	 *            : L2PcInstance Player requesting the item creation
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void setOwnerId(String process, int owner_id, L2PcInstance creator, L2Object reference)
	{
		setOwnerId(owner_id);
		if (Config.LOG_ITEMS)
		{
			LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
			record.setLoggerName("item");
			record.setParameters(new Object[]
			{
				this, creator, reference
			});
			_logItems.log(record);
		}
		if (creator != null)
		{
			if (creator.isGM())
			{
				String referenceName = "no-reference";
				if (reference != null)
				{
					referenceName = (reference.getName() != null ? reference.getName() : "no-name");
				}
				String targetName = (creator.getTarget() != null ? creator.getTarget().getName() : "no-target");
				if (Config.GMAUDIT)
					GMAudit.auditGMAction(creator.getAccountName() + " - " + creator.getName(), process + "(id: " + getItemId() + " name: " + getName() + " - " + getObjectId() + ")", targetName, "reference: " + referenceName);
			}
		}
	}
	
	/**
	 * Sets the ownerID of the item
	 * 
	 * @param owner_id
	 *            : int designating the ID of the owner
	 */
	public void setOwnerId(int owner_id)
	{
		if (owner_id == _ownerId)
			return;
		_ownerId = owner_id;
		_storedInDb = false;
	}
	
	/**
	 * Returns the ownerID of the item
	 * 
	 * @return int : ownerID of the item
	 */
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	/**
	 * Sets the location of the item
	 * 
	 * @param loc
	 *            : ItemLocation (enumeration)
	 */
	public void setLocation(ItemLocation loc)
	{
		setLocation(loc, 0);
	}
	
	/**
	 * Sets the location of the item.<BR>
	 * <BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 * 
	 * @param loc
	 *            : ItemLocation (enumeration)
	 * @param loc_data
	 *            : int designating the slot where the item is stored or the village for freights
	 */
	public void setLocation(ItemLocation loc, int loc_data)
	{
		if (loc == _loc && loc_data == _locData)
			return;
		_loc = loc;
		_locData = loc_data;
		_storedInDb = false;
	}
	
	public ItemLocation getItemLocation()
	{
		return _loc;
	}
	
	/**
	 * Sets the quantity of the item.<BR>
	 * <BR>
	 * 
	 * @param count
	 *            the new count to set
	 */
	public void setCount(long count)
	{
		if (getCount() == count)
		{
			return;
		}
		if (count > PcInventory.MAX_SCROLL)
		{
			if (isScroll())
				count = PcInventory.MAX_SCROLL;
		}
		_count = count >= -1 ? count : 0;
		_storedInDb = false;
	}
	
	private boolean isScroll()
	{
		return (getItem() instanceof L2EtcItem && getEtcItem().getItemType() == L2EtcItemType.SCROLL);
	}
	
	/**
	 * @return Returns the count.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Sets the quantity of the item.<BR>
	 * <BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param count
	 *            : int
	 * @param creator
	 *            : L2PcInstance Player requesting the item creation
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void changeCount(String process, long count, L2PcInstance creator, L2Object reference)
	{
		if (count == 0)
		{
			return;
		}
		long max = Integer.MAX_VALUE;
		if (getItemId() == ADENA_ID)
		{
			max = MAX_ADENA;
		}
		else if (isScroll())
		{
			max = MAX_SCROLL;
		}
		if (count > 0 && getCount() > max - count)
		{
			setCount(max);
		}
		else
		{
			setCount(getCount() + count);
		}
		if (getCount() < 0)
		{
			setCount(0);
		}
		_storedInDb = false;
		if (Config.LOG_ITEMS && process != null)
		{
			LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
			record.setLoggerName("item");
			record.setParameters(new Object[]
			{
				this, creator, reference
			});
			_logItems.log(record);
		}
		if (creator != null)
		{
			if (creator.isGM())
			{
				String referenceName = "no-reference";
				if (reference != null)
				{
					referenceName = (reference.getName() != null ? reference.getName() : "no-name");
				}
				String targetName = (creator.getTarget() != null ? creator.getTarget().getName() : "no-target");
				// if (Config.GMAUDIT)
				// GMAudit.auditGMAction(creator.getAccountName() + " - " + creator.getName(), process + "(id: " + getItemId() + " objId: " + getObjectId() + " name: " + getName() + " count: " + count + ")", targetName, "Reference: " + referenceName);
			}
		}
	}
	
	// No logging (function designed for shots only)
	public void changeCountWithoutTrace(int count, L2PcInstance creator, L2Object reference)
	{
		changeCount(null, count, creator, reference);
	}
	
	/**
	 * Returns if item is equipable
	 * 
	 * @return boolean
	 */
	public boolean isEquipable()
	{
		return !(_item.getBodyPart() == 0 || _item instanceof L2EtcItem);
	}
	
	/**
	 * Returns if item is equipped
	 * 
	 * @return boolean
	 */
	public boolean isEquipped()
	{
		return _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP;
	}
	
	/**
	 * Returns the slot where the item is stored
	 * 
	 * @return int
	 */
	public int getLocationSlot()
	{
		if (Config.ASSERT)
			assert _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP || _loc == ItemLocation.FREIGHT || _loc == ItemLocation.INVENTORY;
		return _locData;
	}
	
	/**
	 * Returns the characteristics of the item
	 * 
	 * @return L2Item
	 */
	public L2Item getItem()
	{
		return _item;
	}
	
	public boolean isEnchantable()
	{
		return _item.isEnchantable();
	}
	
	public int getCustomType1()
	{
		return _type1;
	}
	
	public int getCustomType2()
	{
		return _type2;
	}
	
	public void setCustomType1(int newtype)
	{
		_type1 = newtype;
	}
	
	public void setCustomType2(int newtype)
	{
		_type2 = newtype;
	}
	
	public void setDropTime(long time)
	{
		_dropTime = time;
	}
	
	public long getDropTime()
	{
		return _dropTime;
	}
	
	public boolean isWear()
	{
		return _wear;
	}
	
	public void setWear(boolean newwear)
	{
		_wear = newwear;
	}
	
	/**
	 * Returns the type of item
	 * 
	 * @return Enum
	 */
	@SuppressWarnings("rawtypes")
	public Enum getItemType()
	{
		return _item.getItemType();
	}
	
	/**
	 * Returns the ID of the item
	 * 
	 * @return int
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Returns true if item is an EtcItem
	 * 
	 * @return boolean
	 */
	public boolean isEtcItem()
	{
		return (_item instanceof L2EtcItem);
	}
	
	/**
	 * Returns true if item is a Weapon/Shield
	 * 
	 * @return boolean
	 */
	public boolean isWeapon()
	{
		return (_item instanceof L2Weapon);
	}
	
	/**
	 * Returns true if item is an Armor
	 * 
	 * @return boolean
	 */
	public boolean isArmor()
	{
		return (_item instanceof L2Armor);
	}
	
	/**
	 * Returns the characteristics of the L2EtcItem
	 * 
	 * @return L2EtcItem
	 */
	public L2EtcItem getEtcItem()
	{
		if (_item instanceof L2EtcItem)
		{
			return (L2EtcItem) _item;
		}
		return null;
	}
	
	/**
	 * Returns the characteristics of the L2Weapon
	 * 
	 * @return L2Weapon
	 */
	public L2Weapon getWeaponItem()
	{
		if (_item instanceof L2Weapon)
		{
			return (L2Weapon) _item;
		}
		return null;
	}
	
	/**
	 * Returns the characteristics of the L2Armor
	 * 
	 * @return L2Armor
	 */
	public L2Armor getArmorItem()
	{
		if (_item instanceof L2Armor)
		{
			return (L2Armor) _item;
		}
		return null;
	}
	
	/**
	 * Returns the quantity of crystals for crystallization
	 * 
	 * @return int
	 */
	public final int getCrystalCount()
	{
		return _item.getCrystalCount(_enchantLevel);
	}
	
	public final int getCrystalType()
	{
		return _item.getCrystalType();
	}
	
	/**
	 * Returns the reference price of the item
	 * 
	 * @return int
	 */
	public int getReferencePrice()
	{
		return _item.getReferencePrice();
	}
	
	/**
	 * Returns the name of the item
	 * 
	 * @return String
	 */
	public String getItemName()
	{
		return _item.getName();
	}
	
	/**
	 * Returns the last change of the item
	 * 
	 * @return int
	 */
	public int getLastChange()
	{
		return _lastChange;
	}
	
	/**
	 * Sets the last change of the item
	 * 
	 * @param lastChange
	 *            : int
	 */
	public void setLastChange(int lastChange)
	{
		_lastChange = lastChange;
	}
	
	/**
	 * Returns if item is stackable
	 * 
	 * @return boolean
	 */
	public boolean isStackable()
	{
		return _item.isStackable();
	}
	
	/**
	 * Returns if item is dropable
	 * 
	 * @return boolean
	 */
	public boolean isDropable()
	{
		if (isStackable() && getUntradeableTime() > System.currentTimeMillis())
			return false;
		return (isAugmented() && _augmentation.getSkill() != null) ? false : _item.isDropable();
	}
	
	public boolean isDropableKarma()
	{
		if (isStackable() && getUntradeableTime() > System.currentTimeMillis())
			return false;
		return _item.isDropable();
	}
	
	/**
	 * Returns if item is destroyable
	 * 
	 * @return boolean
	 */
	public boolean isDestroyable()
	{
		return _item.isDestroyable();
	}
	
	/**
	 * set the timer to when the item can be traded/if ever
	 */
	public void setUntradeableTimer(final long timeInMilis)
	{
		if (_untradeableTime == timeInMilis)
			return;
		_untradeableTime = timeInMilis;
		_storedInDb = false;
	}
	
	/**
	 * Returns if item is tradeable
	 * 
	 * @return boolean
	 */
	public boolean isTradeable()
	{
		if (_untradeableTime > 0 && _untradeableTime > System.currentTimeMillis())
			return false;
		if (isTimeLimitedItem())
			return false;
		if (this.getVisualItemId() > 0)
			return false;
		if (_elementals != null)
		{
			if (isWeapon())
			{
				if (_elementals.getValue() >= 185)
					return false;
			}
			else
			{
				if (_elementals.getValue() >= 84)
					return false;
			}
		}
		// if (_instanceDroppedFrom != null)
		// {
		// if (getUniqueness() > 3)
		// {
		// final L2PcInstance player = ((L2PcInstance) L2World.getInstance().findObject(getOwnerId()));
		// if (player != null)
		// {
		// StringTokenizer st = new StringTokenizer(_instanceDroppedFrom, ";");
		// try
		// {
		// int id = Integer.valueOf(st.nextToken());
		// int id2 = Integer.valueOf(st.nextToken());
		// if (id2 > 2000) // greater than kamaloka
		// {
		// if (player.getInstanceId() != id)
		// return false;
		// }
		// }
		// catch (NumberFormatException e)
		// {}
		// }
		// }
		// }
		return _item.isTradeable();
	}
	
	private boolean _isFromPvPNpc = false;
	
	final public boolean isFromPvPNpc()
	{
		return _isFromPvPNpc;
	}
	
	final public void setIsFromPvPNpc(final boolean isFromPvPNpc)
	{
		_isFromPvPNpc = isFromPvPNpc;
	}
	
	public boolean isFreightable()
	{
		if (isTimeLimitedItem())
			return false;
		if (isStackable() && getUntradeableTime() > System.currentTimeMillis())
			return false;
		return (isAugmented() && _augmentation.getSkill() != null) ? false : true;
	}
	
	/**
	 * Returns if item is sellable
	 * 
	 * @return boolean
	 */
	public boolean isSellable()
	{
		return (isAugmented() && _augmentation.getSkill() != null) ? false : _item.isSellable();
	}
	
	/**
	 * Returns if item can be deposited in warehouse or freight
	 * 
	 * @return boolean
	 */
	public boolean isDepositable(boolean isPrivateWareHouse)
	{
		// equipped, hero and quest items
		if (isEquipped() || isHeroItem() || isCastleItem() || _item.getItemType() == L2EtcItemType.QUEST || !_item.isDepositable())
			return false;
		// Staff of Master Yogi
		if (_itemId == 13539)
			return false;
		if (isStackable() && getUntradeableTime() > System.currentTimeMillis())
			return false;
		if (!isPrivateWareHouse)
		{
			// augmented not tradeable
			if (!isTradeable() || isShadowItem())
				return false;
		}
		return true;
	}
	
	public boolean isDepositableFreight()
	{
		// equipped, hero and quest items
		if (isEquipped() || isHeroItem() || isCastleItem() || _item.getItemType() == L2EtcItemType.QUEST)
			return false;
		// Staff of Master Yogi
		if (_itemId == 13539)
			return false;
		if (_itemId == 8542)
			return false;
		if (isDread() || isCorrupted())
			return false;
		if (isStackable() && getUntradeableTime() > System.currentTimeMillis())
			return false;
		// augmented not tradeable
		if (!isFreightable() || isShadowItem())
			return false;
		return true;
	}
	
	/**
	 * Returns if item is consumable
	 * 
	 * @return boolean
	 */
	public boolean isConsumable()
	{
		return _item.isConsumable();
	}
	
	public boolean isHeroItem()
	{
		return _item.isHeroItem();
	}
	
	public boolean isCastleItem()
	{
		return _item.isCastleItem();
	}
	
	public boolean isCommonItem()
	{
		return _item.isCommon();
	}
	
	final public boolean isOlyRestrictedItem()
	{
		final L2Item item = getItem();
		if (item == null)
			return true;
		if (isARestrictedItem())
			return true;
		if (item instanceof L2Armor || item instanceof L2Weapon)
		{
			if (!isAugmented() && item.getCrystalType() <= L2Item.CRYSTAL_S && item.getCrystalType() >= L2Item.CRYSTAL_B && getEnchantLevel() < 25 && item.getUniqueness() < 1)
			{
				if (!Config.LIST_OLY_RESTRICTED_ITEMS.contains(_itemId))
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns if item is available for manipulation
	 * 
	 * @return boolean
	 */
	public boolean isAvailable(L2PcInstance player, boolean allowAdena, boolean allowNonTradeable)
	{
		if (player == null)
			return false;
		if (getItemId() == L2Item.DONATION_TOKEN && player.getAccessLevel().getLevel() < 232)
			return false;
		return ((!isEquipped()) // Not equipped
		&& (getItem().getType2() != 3) // Not Quest Item
		&& (getItem().getType2() != 4 || getItem().getType1() != 1) // TODO: what does this mean?
		&& (player.getPet() == null || getObjectId() != player.getPet().getControlItemId()) // Not Control item of currently summoned pet
		&& (player.getActiveEnchantItem() != this) // Not momentarily used enchant scroll
		&& (allowAdena || getItemId() != 57) // Not adena
		&& (player.getCurrentSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != getItemId()) && (!player.isCastingSimultaneouslyNow() || player.getLastSimultaneousSkillCast() == null || player.getLastSimultaneousSkillCast().getItemConsumeId() != getItemId()) && (allowNonTradeable || isTradeable()) && !isHeroItem());
	}
	
	public boolean isAvailableFreight(L2PcInstance player)
	{
		if (player == null)
			return false;
		if (getItemId() == L2Item.DONATION_TOKEN && player.getAccessLevel().getLevel() < 232)
			return false;
		return ((!isEquipped()) // Not equipped
		&& (getItem().getType2() != 3) // Not Quest Item
		&& (getItem().getType2() != 4 || getItem().getType1() != 1) // TODO: what does this mean?
		&& (player.getPet() == null || getObjectId() != player.getPet().getControlItemId()) // Not Control item of currently summoned pet
		&& (player.getActiveEnchantItem() != this) // Not momentarily used enchant scroll
		&& (player.getCurrentSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != getItemId()) && (!player.isCastingSimultaneouslyNow() || player.getLastSimultaneousSkillCast() == null || player.getLastSimultaneousSkillCast().getItemConsumeId() != getItemId()) && (isFreightable()));
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.L2Object#onAction(net.sf.l2j.gameserver.model.L2PcInstance)
	 * also check constraints: only soloing castle owners may pick up mercenary tickets of their castle
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		// this causes the validate position handler to do the pickup if the location is reached.
		// mercenary tickets can only be picked up by the castle owner.
		int castleId = MercTicketManager.getInstance().getTicketCastleId(_itemId);
		if (castleId > 0 && (!player.isCastleLord(castleId) || player.isInParty()))
		{
			if (player.isInParty()) // do not allow owner who is in party to pick tickets up
				player.sendMessage("You cannot pickup mercenaries while in a party.");
			else
				player.sendMessage("Only the castle lord can pickup mercenaries.");
			player.setTarget(this);
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (player.isFlying()) // cannot pickup
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, this);
	}
	
	/**
	 * Returns the level of enchantment of the item
	 * 
	 * @return int
	 */
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	/**
	 * Sets the level of enchantment of the item
	 * 
	 * @param int
	 */
	public void setEnchantLevel(int enchantLevel)
	{
		if (_enchantLevel == enchantLevel)
			return;
		_enchantLevel = enchantLevel;
		_storedInDb = false;
	}
	
	/**
	 * Returns the physical defense of the item
	 * 
	 * @return int
	 */
	public int getPDef()
	{
		if (_item instanceof L2Armor)
			return ((L2Armor) _item).getPDef();
		return 0;
	}
	
	/**
	 * Returns whether this item is augmented or not
	 * 
	 * @return true if augmented
	 */
	public boolean isAugmented()
	{
		return _augmentation == null ? false : true;
	}
	
	/**
	 * Returns the augmentation object for this item
	 * 
	 * @return augmentation
	 */
	public L2Augmentation getAugmentation()
	{
		return _augmentation;
	}
	
	/**
	 * Sets a new augmentation
	 * 
	 * @param augmentation
	 * @return return true if sucessfull
	 */
	public boolean setAugmentation(L2Augmentation augmentation)
	{
		// there shall be no previous augmentation..
		if (_augmentation != null)
			return false;
		_augmentation = augmentation;
		updateItemAttributes();
		return true;
	}
	
	/**
	 * Remove the augmentation
	 */
	public void removeAugmentation()
	{
		if (_augmentation == null)
			return;
		_augmentation = null;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			if (_elementals != null)
			{
				// Item still has elemental enchant, only update the DB
				statement = con.prepareStatement("UPDATE item_attributes SET augAttributes = -1, augSkillId = -1, augSkillLevel = -1 WHERE itemId = ?");
			}
			else
			{
				// Remove the entry since the item also has no elemental enchant
				statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?");
			}
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not remove augmentation for item: " + getObjectId() + " from DB:", e);
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
	
	public void restoreAttributes()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT augAttributes,augSkillId,augSkillLevel,elemType,elemValue FROM item_attributes WHERE itemId=?");
			statement.setInt(1, getObjectId());
			ResultSet rs = statement.executeQuery();
			rs = statement.executeQuery();
			if (rs.next())
			{
				int aug_attributes = rs.getInt(1);
				int aug_skillId = rs.getInt(2);
				int aug_skillLevel = rs.getInt(3);
				byte elem_type = rs.getByte(4);
				int elem_value = rs.getInt(5);
				if (elem_type != -1 && elem_value != -1)
					_elementals = new Elementals(elem_type, elem_value);
				if (aug_attributes != -1 && aug_skillId != -1 && aug_skillLevel != -1)
					_augmentation = new L2Augmentation(rs.getInt("augAttributes"), rs.getInt("augSkillId"), rs.getInt("augSkillLevel"));
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore augmentation and elemental data for item " + getObjectId() + " from DB: " + e.getMessage(), e);
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
	
	public void updateItemAttributes()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("REPLACE INTO item_attributes VALUES(?,?,?,?,?,?)");
			statement.setInt(1, getObjectId());
			if (_augmentation == null)
			{
				statement.setInt(2, -1);
				statement.setInt(3, -1);
				statement.setInt(4, -1);
			}
			else
			{
				statement.setInt(2, _augmentation.getAttributes());
				if (_augmentation.getSkill() == null)
				{
					statement.setInt(3, 0);
					statement.setInt(4, 0);
				}
				else
				{
					statement.setInt(3, _augmentation.getSkill().getId());
					statement.setInt(4, _augmentation.getSkill().getLevel());
				}
			}
			if (_elementals == null)
			{
				statement.setByte(5, (byte) -1);
				statement.setInt(6, -1);
			}
			else
			{
				statement.setByte(5, _elementals.getElement());
				statement.setInt(6, _elementals.getValue());
			}
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not remove elemental enchant for item: " + getObjectId() + " from DB:", e);
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
	
	public Elementals getElementals()
	{
		return _elementals;
	}
	
	public byte getAttackElementType()
	{
		if ((isWeapon() || getItemId() == 14164) && _elementals != null)
			return _elementals.getElement();
		return -2;
	}
	
	public int getAttackElementPower()
	{
		if ((isWeapon() || getItemId() == 14164) && _elementals != null)
			return _elementals.getValue();
		return 0;
	}
	
	public int getElementDefAttr(byte element)
	{
		if (!canBeAttrEnchanted() && isArmor() && _elementals != null && _elementals.getElement() == element)
		{
			L2PcInstance player = L2World.getInstance().findObject(getOwnerId()).getActingPlayer();
			// LunaLogger.getInstance().log("adrenaline_hacks", player.getName() +" " + getName() + " had illegal element");
			clearElementAttr();
		}
		if (isArmor() && _elementals != null && _elementals.getElement() == element)
			return _elementals.getValue();
		return 0;
	}
	
	public void setElementAttr(byte element, int value)
	{
		if (_elementals == null)
		{
			_elementals = new Elementals(element, value);
		}
		else
		{
			_elementals.setElement(element);
			_elementals.setValue(value);
		}
		updateItemAttributes();
	}
	
	public void clearElementAttr()
	{
		if (_elementals != null)
		{
			_elementals = null;
		}
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			if (_augmentation != null)
			{
				// Item still has augmentation, only update the DB
				statement = con.prepareStatement("UPDATE item_attributes SET elemType = -1, elemValue = -1 WHERE itemId = ?");
			}
			else
			{
				// Remove the entry since the item also has no augmentation
				statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?");
			}
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not remove elemental enchant for item: " + getObjectId() + " from DB:", e);
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
	 * Used to decrease mana
	 * (mana means life time for shadow items)
	 */
	public class ScheduleConsumeManaTask implements Runnable
	{
		private final L2ItemInstance _shadowItem;
		
		public ScheduleConsumeManaTask(L2ItemInstance item)
		{
			_shadowItem = item;
		}
		
		public void run()
		{
			try
			{
				// decrease mana
				if (_shadowItem != null)
					_shadowItem.decreaseMana(true);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * Returns true if this item is a shadow item
	 * Shadow items have a limited life-time
	 * 
	 * @return
	 */
	public boolean isShadowItem()
	{
		return (_mana >= 0);
	}
	
	/**
	 * Returns the remaining mana of this shadow item
	 * 
	 * @return lifeTime
	 */
	public int getMana()
	{
		return _mana;
	}
	
	/**
	 * Decreases the mana of this shadow item,
	 * sends a inventory update
	 * schedules a new consumption task if non is running
	 * optionally one could force a new task
	 * 
	 * @param forces
	 *            a new consumption task if item is equipped
	 */
	public void decreaseMana(boolean resetConsumingMana)
	{
		if (!isShadowItem())
			return;
		if (_mana > 0)
			_mana--;
		if (_storedInDb)
			_storedInDb = false;
		if (resetConsumingMana)
			_consumingMana = false;
		final L2PcInstance player = ((L2PcInstance) L2World.getInstance().findObject(getOwnerId()));
		if (player != null)
		{
			SystemMessage sm;
			switch (_mana)
			{
				case 10:
					sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_10);
					sm.addItemName(_item);
					player.sendPacket(sm);
					break;
				case 5:
					sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_5);
					sm.addItemName(_item);
					player.sendPacket(sm);
					break;
				case 1:
					sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_1);
					sm.addItemName(_item);
					player.sendPacket(sm);
					break;
			}
			if (_mana == 0) // The life time has expired
			{
				sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_0);
				sm.addItemName(_item);
				player.sendPacket(sm);
				// unequip
				if (isEquipped())
				{
					L2ItemInstance[] unequiped = player.getInventory().unEquipItemInSlotAndRecord(getLocationSlot());
					InventoryUpdate iu = new InventoryUpdate();
					for (L2ItemInstance item : unequiped)
					{
						iu.addModifiedItem(item);
					}
					player.sendPacket(iu);
				}
				if (getItemLocation() != ItemLocation.WAREHOUSE)
				{
					// destroy
					player.getInventory().destroyItem("L2ItemInstance", this, player, null);
					// send update
					InventoryUpdate iu = new InventoryUpdate();
					iu.addRemovedItem(this);
					player.sendPacket(iu);
				}
				else
				{
					player.getWarehouse().destroyItem("L2ItemInstance", this, player, null);
				}
				// delete from world
				L2World.getInstance().removeObject(this);
			}
			else
			{
				// Reschedule if still equipped
				if (!_consumingMana && isEquipped())
				{
					scheduleConsumeManaTask();
				}
				if (getItemLocation() != ItemLocation.WAREHOUSE)
				{
					InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(this);
					player.sendPacket(iu);
				}
			}
		}
	}
	
	public void scheduleConsumeManaTask()
	{
		_consumingMana = true;
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleConsumeManaTask(this), MANA_CONSUMPTION_RATE);
	}
	
	/**
	 * Returns false cause item can't be attacked
	 * 
	 * @return boolean false
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
	
	public boolean getChargedFishshot()
	{
		return _chargedFishtshot;
	}
	
	public void setChargedFishshot(boolean type)
	{
		_chargedFishtshot = type;
	}
	
	/**
	 * This function basically returns a set of functions from
	 * L2Item/L2Armor/L2Weapon, but may add additional
	 * functions, if this particular item instance is enhanched
	 * for a particular player.
	 * 
	 * @param player
	 *            : L2Character designating the player
	 * @return Func[]
	 */
	public Func[] getStatFuncs(L2Character player)
	{
		return getItem().getStatFuncs(this, player);
	}
	
	/**
	 * Updates the database.<BR>
	 */
	public void updateDatabase()
	{
		updateDatabase(false);
	}
	
	/**
	 * Updates the database.<BR>
	 * 
	 * @param force
	 *            if the update should necessarilly be done.
	 */
	public void updateDatabase(boolean force)
	{
		if (isWear()) // avoid saving weared items
		{
			return;
		}
		_dbLock.lock();
		try
		{
			if (_existsInDb)
			{
				if (_ownerId == 0 || _loc == ItemLocation.VOID || (getCount() == 0 && _loc != ItemLocation.LEASE))
				{
					removeFromDb();
				}
				else if (!Config.LAZY_ITEMS_UPDATE || force)
				{
					updateInDb();
				}
			}
			else
			{
				if (getCount() == 0 && _loc != ItemLocation.LEASE)
				{
					return;
				}
				if (_loc == ItemLocation.VOID || _loc == ItemLocation.NPC || _ownerId == 0)
				{
					return;
				}
				insertIntoDb();
			}
		}
		finally
		{
			_dbLock.unlock();
		}
	}
	
	/**
	 * Returns a L2ItemInstance stored in database from its objectID
	 * 
	 * @param objectId
	 *            : int designating the objectID of the item
	 * @return L2ItemInstance
	 */
	public static L2ItemInstance restoreFromDb(int ownerId, ResultSet rs)
	{
		L2ItemInstance inst = null;
		int objectId, item_id, loc_data, enchant_level, custom_type1, custom_type2, manaLeft;
		long time, count, tradetime, visualTimer;
		String instanceFrom;
		ItemLocation loc;
		int visualItemId;
		try
		{
			objectId = rs.getInt(1);
			item_id = rs.getInt("item_id");
			count = rs.getLong("count");
			loc = ItemLocation.valueOf(rs.getString("loc"));
			loc_data = rs.getInt("loc_data");
			enchant_level = rs.getInt("enchant_level");
			custom_type1 = rs.getInt("custom_type1");
			custom_type2 = rs.getInt("custom_type2");
			manaLeft = rs.getInt("mana_left");
			time = rs.getLong("time");
			tradetime = rs.getLong("trade_time");
			instanceFrom = rs.getString("instance");
			visualItemId = rs.getInt("visual_item_id");
			visualTimer = rs.getLong("visual_item_limitedTime");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore an item owned by " + ownerId + " from DB:", e);
			return null;
		}
		L2Item item = ItemTable.getInstance().getTemplate(item_id);
		if (item == null)
		{
			_log.severe("Item item_id=" + item_id + " not known, object_id=" + objectId);
			return null;
		}
		inst = new L2ItemInstance(objectId, item);
		inst._ownerId = ownerId;
		inst._enchantLevel = enchant_level;
		inst._type1 = custom_type1;
		inst._type2 = custom_type2;
		if (inst.isScroll())
		{
			inst.setCount(Math.min(MAX_SCROLL, count));
		}
		else
			inst.setCount(count);
		inst._loc = loc;
		inst._locData = loc_data;
		inst._existsInDb = true;
		inst._storedInDb = true;
		inst._instanceDroppedFrom = instanceFrom;
		// Setup life time for shadow weapons
		inst._mana = manaLeft;
		inst._time = time;
		inst._untradeableTime = tradetime;
		// Set visual item id for dress me
		inst._visualItemId = visualItemId;
		// consume 1 mana
		if (inst.isShadowItem() && inst.isEquipped())
		{
			inst.decreaseMana(false);
			// if player still not loaded and not found in the world - force task creation
			inst.scheduleConsumeManaTask();
		}
		if (inst.isTimeLimitedItem())
			inst.scheduleLifeTimeTask();
		// load augmentation and elemental enchant
		if (inst.isEquipable())
		{
			inst.restoreAttributes();
		}
		return inst;
	}
	
	/**
	 * Init a dropped L2ItemInstance and add it in the world as a visible object.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion</li>
	 * <li>Add the L2ItemInstance dropped to _visibleObjects of its L2WorldRegion</li>
	 * <li>Add the L2ItemInstance dropped in the world as a <B>visible</B> object</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _allObjects of L2World </B></FONT><BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li>_worldRegion == null <I>(L2Object is invisible at the beginning)</I></li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Drop item</li>
	 * <li>Call Pet</li><BR>
	 */
	public class doItemDropTask implements Runnable
	{
		private int						_x, _y, _z;
		private final L2Character		_dropper;
		private final L2ItemInstance	_itm;
		
		public doItemDropTask(L2ItemInstance item, L2Character dropper, int x, int y, int z)
		{
			_x = x;
			_y = y;
			_z = z;
			_dropper = dropper;
			_itm = item;
		}
		
		public final void run()
		{
			if (Config.ASSERT)
				assert _itm.getPosition().getWorldRegion() == null;
			if (Config.GEODATA > 0 && _dropper != null)
			{
				Location dropDest = GeoData.getInstance().moveCheck(_dropper.getX(), _dropper.getY(), _dropper.getZ(), _x, _y, _z, _dropper.getInstanceId());
				_x = dropDest.getX();
				_y = dropDest.getY();
				_z = dropDest.getZ();
			}
			if (_dropper != null)
				setInstanceId(_dropper.getInstanceId()); // Inherit instancezone when dropped in visible world
			else
				setInstanceId(0); // No dropper? Make it a global item...
			synchronized (_itm)
			{
				// Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion
				_itm.setIsVisible(true);
				_itm.getPosition().setWorldPosition(_x, _y, _z);
				_itm.getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));
				// Add the L2ItemInstance dropped to _visibleObjects of its L2WorldRegion
			}
			_itm.getPosition().getWorldRegion().addVisibleObject(_itm);
			_itm.setDropTime(System.currentTimeMillis());
			_itm.setDropperObjectId(_dropper != null ? _dropper.getObjectId() : 0); // Set the dropper Id for the knownlist packets in sendInfo
			// this can synchronize on others instancies, so it's out of
			// synchronized, to avoid deadlocks
			// Add the L2ItemInstance dropped in the world as a visible object
			L2World.getInstance().addVisibleObject(_itm, _itm.getPosition().getWorldRegion());
			if (Config.SAVE_DROPPED_ITEM)
				ItemsOnGroundManager.getInstance().save(_itm);
			_itm.setDropperObjectId(0); // Set the dropper Id back to 0 so it no longer shows the drop packet
		}
	}
	
	public final void dropMe(L2Character dropper, int x, int y, int z)
	{
		ThreadPoolManager.getInstance().executeTask(new doItemDropTask(this, dropper, x, y, z));
	}
	
	/**
	 * Update the database with values of the item
	 */
	private void updateInDb()
	{
		if (Config.ASSERT)
			assert _existsInDb;
		if (_wear)
			return;
		if (_storedInDb)
			return;
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,custom_type1=?,custom_type2=?,mana_left=?,time=?,trade_time=?,visual_item_id=?,visual_item_limitedTime=? " + "WHERE object_id = ?");
			statement.setInt(1, _ownerId);
			statement.setLong(2, getCount());
			statement.setString(3, _loc.name());
			statement.setInt(4, _locData);
			statement.setInt(5, getEnchantLevel());
			statement.setInt(6, getCustomType1());
			statement.setInt(7, getCustomType2());
			statement.setInt(8, getMana());
			statement.setLong(9, getTime());
			statement.setLong(10, _untradeableTime);
			statement.setInt(11, getVisualItemIdForDb());
			statement.setLong(12, this.getVisualTimer());
			statement.setInt(13, getObjectId());
			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not update item " + getObjectId() + " in DB: Reason: " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception e)
			{}
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	/**
	 * Insert the item in database
	 */
	private void insertIntoDb()
	{
		if (_itemId == 8190 || _itemId == 8689)
			return;
		if (_wear)
			return;
		if (Config.ASSERT)
			assert !_existsInDb && getObjectId() != 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,object_id,custom_type1,custom_type2,mana_left,time,trade_time,source,instance,visual_item_id,visual_item_limitedTime) " + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, _ownerId);
			statement.setInt(2, _itemId);
			statement.setLong(3, getCount());
			statement.setString(4, _loc.name());
			statement.setInt(5, _locData);
			statement.setInt(6, getEnchantLevel());
			statement.setInt(7, getObjectId());
			statement.setInt(8, _type1);
			statement.setInt(9, _type2);
			statement.setInt(10, getMana());
			statement.setLong(11, getTime());
			statement.setLong(12, _untradeableTime);
			statement.setString(13, _source);
			statement.setString(14, _instanceDroppedFrom);
			statement.setInt(15, getVisualItemIdForDb());
			statement.setLong(16, getVisualTimer());
			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not insert item " + getObjectId() + " into DB: Reason: " + e.getMessage(), e);
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
		if (_elementals != null)
			updateItemAttributes();
	}
	
	/**
	 * Delete item from database
	 */
	private void removeFromDb()
	{
		if (Config.ASSERT)
			assert _existsInDb;
		if (_wear)
			return;
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM items WHERE object_id=?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			_existsInDb = false;
			_storedInDb = false;
			statement.close();
			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not delete item " + getObjectId() + " in DB: " + e.getMessage(), e);
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception e)
			{}
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	/**
	 * Returns the item in String format
	 * 
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "" + _item;
	}
	
	public void resetOwnerTimer()
	{
		if (itemLootShedule != null)
			itemLootShedule.cancel(true);
		itemLootShedule = null;
	}
	
	public void setItemLootShedule(ScheduledFuture<?> sf)
	{
		itemLootShedule = sf;
	}
	
	public ScheduledFuture<?> getItemLootShedule()
	{
		return itemLootShedule;
	}
	
	public void setProtected(boolean is_protected)
	{
		_protected = is_protected;
	}
	
	public boolean isProtected()
	{
		return _protected;
	}
	
	public boolean isNightLure()
	{
		return ((_itemId >= 8505 && _itemId <= 8513) || _itemId == 8485);
	}
	
	public void setCountDecrease(boolean decrease)
	{
		_decrease = decrease;
	}
	
	public boolean getCountDecrease()
	{
		return _decrease;
	}
	
	public void setInitCount(int InitCount)
	{
		_initCount = InitCount;
	}
	
	public long getInitCount()
	{
		return _initCount;
	}
	
	public void restoreInitCount()
	{
		if (_decrease)
			setCount(_initCount);
	}
	
	public boolean isTimeLimitedItem()
	{
		return (_time > 0);
	}
	
	/**
	 * Returns (current system time + time) of this time limited item
	 * 
	 * @return Time
	 */
	public long getTime()
	{
		return _time;
	}
	
	public long getRemainingTime()
	{
		return _time - System.currentTimeMillis();
	}
	
	public void endOfLife()
	{
		L2PcInstance player = ((L2PcInstance) L2World.getInstance().findObject(getOwnerId()));
		if (player != null)
		{
			if (isEquipped())
			{
				SystemMessage sm = null;
				if (getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(getEnchantLevel());
					sm.addItemName(this);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(this);
				}
				player.sendPacket(sm);
				L2ItemInstance[] unequiped = player.getInventory().unEquipItemInSlotAndRecord(getLocationSlot());
				InventoryUpdate iu = new InventoryUpdate();
				for (L2ItemInstance item : unequiped)
				{
					iu.addModifiedItem(item);
				}
				player.sendPacket(iu);
				player.broadcastUserInfo();
			}
			if (getItemLocation() != ItemLocation.WAREHOUSE)
			{
				// destroy
				player.getInventory().destroyItem("L2ItemInstance", this, player, null);
				// send update
				InventoryUpdate iu = new InventoryUpdate();
				iu.addRemovedItem(this);
				player.sendPacket(iu);
			}
			else
			{
				player.getWarehouse().destroyItem("L2ItemInstance", this, player, null);
			}
			player.sendPacket(new SystemMessage(SystemMessageId.TIME_LIMITED_ITEM_DELETED));
			// delete from world
			L2World.getInstance().removeObject(this);
		}
	}
	
	public void scheduleLifeTimeTask()
	{
		if (!isTimeLimitedItem())
			return;
		if (getRemainingTime() <= 0)
			endOfLife();
		else
		{
			if (_lifeTimeTask != null)
				_lifeTimeTask.cancel(false);
			_lifeTimeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleLifeTimeTask(this), getRemainingTime());
		}
	}
	
	public class ScheduleLifeTimeTask implements Runnable
	{
		private final L2ItemInstance _limitedItem;
		
		public ScheduleLifeTimeTask(L2ItemInstance item)
		{
			_limitedItem = item;
		}
		
		public void run()
		{
			try
			{
				if (_limitedItem != null)
					_limitedItem.endOfLife();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	public void updateElementAttrBonus(L2PcInstance player)
	{
		if (_elementals == null)
			return;
		_elementals.updateBonus(player, isArmor() && getItemId() != 14164);
	}
	
	public void setDropperObjectId(int id)
	{
		_dropperObjectId = id;
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (_dropperObjectId != 0)
			activeChar.sendPacket(new DropItem(this, _dropperObjectId));
		else
			activeChar.sendPacket(new SpawnItem(this));
	}
	
	public boolean isARestrictedItem()
	{
		return (isHeroItem() || isCastleItem() || getItem().getUniqueness() >= 3 || getCrystalType() > L2Item.CRYSTAL_S80 || getItem().getBodyPart() == L2Item.SLOT_BACK || getItem().getBodyPart() == L2Item.SLOT_BELT || getItem().getBodyPart() == L2Item.SLOT_L_BRACELET || getItem().getBodyPart() == L2Item.SLOT_DECO || getEnchantLevel() > 22);
	}
	
	public boolean isARestrictedItemZone(int limit)
	{
		switch (limit)
		{
			default: // s
				if (isHairAccessory() && getUniqueness() != 1)
					return true;
				if (getUniqueness() > 2)
					return true;
				if (getCrystalType() > L2Item.CRYSTAL_S)
					return true;
				break;
			case 1: // s80
				if (isHairAccessory() && getUniqueness() > 1)
					return true;
				if (getUniqueness() > 3)
					return true;
				if (getCrystalType() > L2Item.CRYSTAL_S80)
					return true;
				break;
			case 2: // vesper
				if (isHairAccessory() && getUniqueness() >= 3.5)
					return true;
				if (getUniqueness() >= 3.5)
					return true;
				if (getCrystalType() > L2Item.CRYSTAL_S80 && !getName().contains("Vesper"))
					return true;
				break;
			case 3: // titanium up
				if (getUniqueness() > 4)
					return true;
				break;
			case 4: // dread up
				if (getUniqueness() > 4.5)
					return true;
				break;
			case 5: // how could this be
				if (getUniqueness() > 5)
					return true;
				break;
		}
		return false;
	}
	
	public boolean isARestrictedItemOrcArea()
	{
		boolean rares = getItem().getUniqueness() >= 3;
		if (!getItem().isRaidbossItem() && getItem().getUniqueness() == 3 && ((getCrystalType() == L2Item.CRYSTAL_S) || (getCrystalType() == L2Item.CRYSTAL_S80)))
			rares = false;
		return (rares || isHeroItem() || isCastleItem() || getCrystalType() > L2Item.CRYSTAL_S80 || getItem().getBodyPart() == L2Item.SLOT_L_BRACELET || getItem().getBodyPart() == L2Item.SLOT_DECO /* || getEnchantLevel() > 24 */
		|| (isHairAccessory() && getUniqueness() != 1));
	}
	
	public boolean isARestrictedItemCotArea()
	{
		boolean rares = getItem().getUniqueness() > 4;
		boolean talismans = getItem().getItemId() >= 9933 && getItem().getItemId() <= 9947;
		return (rares || isHeroItem() || isCastleItem() || talismans);
	}
	
	public boolean isARestrictedItemFT()
	{
		switch (getItem().getBodyPart())
		{
			case L2Item.SLOT_R_HAND:
			case L2Item.SLOT_L_HAND:
			case L2Item.SLOT_LR_HAND:
			case L2Item.SLOT_CHEST:
			case L2Item.SLOT_LEGS:
			case L2Item.SLOT_FULL_ARMOR:
			case L2Item.SLOT_FEET:
			case L2Item.SLOT_GLOVES:
			case L2Item.SLOT_HEAD:
				if (getCrystalType() <= L2Item.CRYSTAL_S && getEnchantLevel() < 25 && getUniqueness() < 2)
					return true;
		}
		return false;
	}
	
	public boolean isHairAccessory()
	{
		return _item.isHairAccessory();
	}
	
	public long getUntradeableTime()
	{
		return _untradeableTime;
	}
	
	public boolean isUntradeableAfterEquip()
	{
		return getItem().isUntradeableAfterEquip() || isUntradeableAfterEquipEnchant();
	}
	
	public boolean isUntradeableAfterEquipEnchant()
	{
		boolean vesper = getName().contains("Vesper") && getItemId() != 14163 && getItemId() != 14164 && getItemId() != 14165;
		if (!vesper)
		{
			vesper = getName().contains("Titanium") && isStandardShopItem() && getUniqueness() == 4;
		}
		return getEnchantLevel() >= getItem().getClutchEnchantLevel() + (vesper ? 1 : 1);
	}
	
	public boolean shouldBeNowSetAsTradeable()
	{
		return !getItem().isUntradeableAfterEquip() && !isUntradeableAfterEquipEnchant() && getUntradeableTime() == 9999999900000L;
	}
	
	public final boolean isAtOrOverMustBreakEnchantLevel()
	{
		if (getUniqueness() > 4.5)
		{
			return getEnchantLevel() >= 8;
		}
		if (getUniqueness() == 4.5)
		{
			if (isStandardShopItem())
				return getEnchantLevel() >= 11;
			else
				return getEnchantLevel() >= 11;
		}
		else if (getUniqueness() == 4)
		{
			if (isStandardShopItem())
				return getEnchantLevel() >= 14;
			else
				return getEnchantLevel() >= 14;
		}
		else if (getUniqueness() == 3.5)
		{
			if (isStandardShopItem())
				return getEnchantLevel() >= 16;
			else
				return getEnchantLevel() >= 15;
		}
		else if (getUniqueness() == 3)
		{
			if (isStandardShopItem())
				return getEnchantLevel() >= 20;
			else
			{
				if (getCrystalType() == L2Item.CRYSTAL_S)
					return getEnchantLevel() >= 20;
				else
					return getEnchantLevel() >= 20;
			}
		}
		else if (getUniqueness() == 2.5)
		{
			if (isStandardShopItem())
				return getEnchantLevel() >= 19;
			else
			{
				if (getCrystalType() == L2Item.CRYSTAL_S)
					return getEnchantLevel() >= 17;
				else
					return getEnchantLevel() >= 16;
			}
		}
		return false;
	}
	
	public boolean isStandardShopItem()
	{
		return _item.isStandardShopItem();
	}
	
	public boolean isRaidbossItem()
	{
		return _item.isRaidbossItem();
	}
	
	public int getStandardShopItem()
	{
		return _item.getStandardShopItem();
	}
	
	public boolean canBeAttrEnchanted()
	{
		boolean can = false;
		switch (_item.getBodyPart())
		{
			case L2Item.SLOT_CHEST:
			case L2Item.SLOT_FEET:
			case L2Item.SLOT_GLOVES:
			case L2Item.SLOT_HEAD:
			case L2Item.SLOT_LEGS:
			case L2Item.SLOT_BACK:
			case L2Item.SLOT_FULL_ARMOR:
			case L2Item.SLOT_R_HAND:
			case L2Item.SLOT_LR_HAND:
				can = true;
				break;
			case L2Item.SLOT_L_EAR:
			case L2Item.SLOT_R_EAR:
			case L2Item.SLOT_LR_EAR:
			case L2Item.SLOT_L_FINGER:
			case L2Item.SLOT_R_FINGER:
			case L2Item.SLOT_LR_FINGER:
			case L2Item.SLOT_NECK:
			case L2Item.SLOT_HAIR:
			case L2Item.SLOT_HAIR2:
			case L2Item.SLOT_HAIRALL:
			case L2Item.SLOT_BELT:
			case L2Item.SLOT_UNDERWEAR:
				if (_item.getItemId() == 14164 || _item.getItemId() == 14163 || _item.getItemId() == 14165 || _item.getItemId() == 20325) // vesper necklace and plastic hair
					can = true;
				else
					can = false;
				break;
			default:
				can = false;
				break;
		}
		if (isWeapon() && (_item.getBodyPart() == L2Item.SLOT_R_HAND || _item.getBodyPart() == L2Item.SLOT_LR_HAND))
			can = true;
		return can;
	}
	
	public void addAutoAugmentation()
	{
		if (AbstractRefinePacket.isValidAutoAugment(this))
		{
			int grade = 3; // top
			int level = 84; // lvl 84
			switch ((int) getItem().getUniqueness())
			{
				case 0:
					break;
				case 1:
					if (getName().contains("Icarus"))
						level = 80;
					else
						level = 82;
					break;
				case 2:
					if (getItem().getUniqueness() == 2.5)
						level = 76;
					else
						level = 82;
					break;
				case 3:
					if (Rnd.get(100) < 75)
					{
						level = 80;
						grade = 2;
					}
					else
					{
						level = 80;
					}
					break;
				default:
					level = 76;
					grade = 2;
					break;
			}
			if (!isStandardShopItem())
			{
				grade -= 1;
				level = 76;
			}
			if (getLocationSlot() == L2Item.SLOT_BACK)
			{
				grade -= 1;
				level -= 2;
			}
			final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(level, grade, getItem().getBodyPart(), isJewelry());
			setAugmentation(aug);
		}
	}
	
	public void addAutoAugmentationDonation()
	{
		if (AbstractRefinePacket.isValidAutoAugment(this))
		{
			int grade = 3; // top
			int level = 84; // lvl 84
			switch ((int) getItem().getUniqueness())
			{
				case 0:
					break;
				case 1:
					if (getName().contains("Icarus"))
						level = 80;
					else
						level = 82;
					break;
				case 2:
					if (getItem().getUniqueness() == 2.5)
						level = 76;
					else
						level = 82;
					break;
				case 3:
					if (Rnd.get(100) < 66)
					{
						level = 82;
						grade = 2;
					}
					else
					{
						level = 80;
						grade = 3;
					}
					break;
				default:
					level = 76;
					grade = 2;
					break;
			}
			if (!isStandardShopItem())
			{
				grade = 2;
				level = 76;
			}
			if (getLocationSlot() == L2Item.SLOT_BACK)
			{
				grade -= 1;
				level -= 2;
			}
			final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(level, grade, getItem().getBodyPart(), isJewelry());
			setAugmentation(aug);
		}
	}
	
	public int getSuperEnchantLevel()
	{
		return _item.getSuperEnchantLevel();
	}
	
	public boolean attemptToIncreaseEnchantViaPVP(L2PcInstance player)
	{
		int enchantlvl = getEnchantLevel();
		final float uniqueness = getUniqueness();
		int chance = 0;
		if (uniqueness == 0)
		{
			if (enchantlvl < 14)
				chance = 750;
			else if (enchantlvl < 16)
				chance = 500;
			else if (enchantlvl < 18)
				chance = 250;
			else if (enchantlvl < 20)
				chance = 60;
		}
		else if (uniqueness == 1)
		{
			if (enchantlvl < 15)
				chance = 150;
		}
		else if (uniqueness == 1.5) // masterwork
		{
			if (enchantlvl < 15)
				chance = 70;
		}
		else if (uniqueness == 2) // dynasty
		{
			if (enchantlvl < 16)
				chance = 50;
			else if (enchantlvl < 18)
				chance = 7;
		}
		else if (uniqueness == 2.5) // icarus
		{
			if (enchantlvl < 15)
				chance = 16;
		}
		else if (uniqueness == 3) // rare weapons, vesper
		{
			if (enchantlvl < 10)
				chance = 15;
			else if (enchantlvl < 15)
			{
				if (getCrystalType() > L2Item.CRYSTAL_S)
				{
					if (!isStandardShopItem())
						chance = 2;
					else
					{
						switch (getItemId())
						{
							case 14163:
							case 14164:
							case 14165:
								if (enchantlvl >= 14)
									chance = 0;
								else
									chance = 3;
								break;
							default:
								chance = 7;
						}
					}
				}
				else
					chance = 6;
			}
		}
		else if (uniqueness == 3.5) // raidboss weapons
		{
			if (enchantlvl < 10)
				chance = 8;
			else if (enchantlvl < 14)
			{
				if (enchantlvl < 13)
				{
					if (Rnd.get(2) == 0)
						chance = 1;
					else
						chance = 0;
				}
				else
				{
					if (Rnd.get(5) == 0)
						chance = 1;
					else
						chance = 0;
				}
			}
		}
		else if (uniqueness == 4) // titanium, epics
		{
			if (isStandardShopItem())
			{
				if (enchantlvl < 9)
					chance = 6;
				else if (enchantlvl < 11)
					chance = 1;
			}
			else
			{
				if (enchantlvl < 10)
					chance = 3;
			}
		}
		else if (uniqueness == 4.5) // dread
		{
			if (enchantlvl < 7)
				chance = 1;
		}
		else if (uniqueness == 5)
		{
			if (enchantlvl < 5)
				chance = 1;
		}
		if (chance > 0 && (chance >= 1350 || Rnd.get(1350) < chance))
		{
			enchantlvl++;
			setEnchantLevel(enchantlvl);
			updateDatabase();
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED);
			sm.addNumber(enchantlvl);
			sm.addItemName(this);
			player.sendPacket(sm);
			player.broadcastUserInfo();
			if (enchantlvl >= getItem().getClutchEnchantLevel())
			{
				Broadcast.toAllOnlinePlayers(SystemMessage.sendString(player.getName() + " has enchanted +" + enchantlvl + " " + getName() + " via PvPing"));
				// fireworks
				final L2Skill skill = SkillTable.getInstance().getInfo(2025, 1);
				if (skill != null)
				{
					MagicSkillUse MSU = new MagicSkillUse(player, player, 2025, 1, 1, 0);
					player.broadcastPacket(MSU);
					player.useMagic(skill, false, false);
				}
				RequestEnchantItem.auditEnchant(player, null, this, "Yes (PVP)");
			}
			return true;
		}
		return false;
	}
	
	public float getUniqueness()
	{
		return _item.getUniqueness();
	}
	
	public boolean isJewelry()
	{
		return _item.isJewelry();
	}
	
	public void setInstanceDroppedFrom(String string)
	{
		_instanceDroppedFrom = string;
	}
	
	public boolean isDread()
	{
		return _item.isDread();
	}
	
	public boolean isCorrupted()
	{
		return _item.isCorrupted();
	}
	
	public boolean isTit()
	{
		return _item.isTit();
	}
	
	public boolean isUnique()
	{
		return _item.isUnique();
	}
	
	public boolean isMorheim()
	{
		return _item.isMorheim();
	}
	
	public boolean isWarForged()
	{
		return _item.isWarForged();
	}
	
	public boolean isRelicJew()
	{
		return _item.isRelicJew();
	}
	
	public boolean isTalisman()
	{
		return _item.isTalisman();
	}
	
	public boolean isStarterItem()
	{
		return getUntradeableTime() == 9999999900004L;
	}
	
	public int getDisplayId()
	{
		if (getItem() instanceof L2Weapon)
		{
			L2Weapon weap = (L2Weapon) getItem();
			if (weap.getItemType() == L2WeaponType.CROSSBOW)
			{
				if (getActingPlayer().getRace() != Race.Kamael && getActingPlayer().getRace() != Race.DarkElf)
				{
					return getItem().getNonKamaelDisplayId();
				}
			}
		}
		return getItem().getDisplayId();
	}
	
	public int getNonKamaelDisplayId()
	{
		return getItem().getNonKamaelDisplayId();
	}
	
	public int getDisplayId1()
	{
		return getItem().getItemId();
	}
	
	public int fixIdForNonKamaelDelf(int id)
	{
		L2Weapon weap = (L2Weapon) ItemTable.getInstance().getTemplate(id);
		if (weap.getItemType() == L2WeaponType.CROSSBOW)
		{
			L2PcInstance player = L2World.getInstance().getPlayer(_ownerId);
			if (player == null)
				return id;
			if (player.getRace() != Race.Kamael && player.getRace() != Race.DarkElf)
			{
				return getItem().getNonKamaelDisplayId();
			}
		}
		return id;
	}
	
	// Used for dress me engine
	public int	_visualItemId	= 0;
	public long	_visualTime		= 0;
	
	public int getVisualItemId()
	{
		if (_tryingItemId != 0)
		{
			return _tryingItemId;
		}
		return _visualItemId;
	}
	
	public int getVisualItemIdForDb()
	{
		return _visualItemId;
	}
	
	public void setVisualItemId(int itemId)
	{
		_visualItemId = itemId;
	}
	
	public long getVisualTimer()
	{
		return _visualTime;
	}
	
	public void setVisualTimer(long time)
	{
		_visualTime = time;
	}
	
	public int _tryingItemId = 0;
	
	public int getTryingItemId()
	{
		return _tryingItemId;
	}
	
	public void setTryingItemId(int itemId)
	{
		_tryingItemId = itemId;
	}
	
	private boolean _fakeTempItem = false;
	
	public void setFakeTempItem(Boolean val)
	{
		_fakeTempItem = val;
	}
	
	public boolean isFakeTempItem()
	{
		return _fakeTempItem;
	}
}