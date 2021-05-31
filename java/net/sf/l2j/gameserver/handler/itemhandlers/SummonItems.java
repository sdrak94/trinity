package net.sf.l2j.gameserver.handler.itemhandlers;

import java.util.logging.Level;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SummonItemsData;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2SummonItem;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PetItemList;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Broadcast;

public class SummonItems implements IItemHandler
{
public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
{
	if (!(playable instanceof L2PcInstance))
		return;
	
	if (!TvTEvent.onItemSummon(playable.getObjectId()))
		return;
	
	final L2PcInstance activeChar = (L2PcInstance) playable;
	
	if (!activeChar.getFloodProtectors().getItemPetSummon().
			tryPerformAction("summon items"))
	{
		return;
	}
	
	if (activeChar.isSitting())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
		return;
	}
	
	if (activeChar.inObserverMode())
		return;
	
	if (activeChar.isInOlympiadMode())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
		return;
	}
	if (activeChar.isAllSkillsDisabled() || activeChar.isCastingNow())
		return;
	
	final L2SummonItem sitem = SummonItemsData.getInstance().getSummonItem(item.getItemId());
	
	if ((activeChar.getPet() != null || activeChar.isMounted()) && sitem.isPetSummon())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_HAVE_A_PET));
		return;
	}
	
	if (activeChar.isAttackingNow())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_IN_COMBAT));
		return;
	}
	
	if (activeChar.isCursedWeaponEquipped() && sitem.isPetSummon())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE));
		return;
	}
	
	final int npcID = sitem.getNpcId();
	if (npcID == 0)
		return;
	
	final L2NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(npcID);
	if (npcTemplate == null)
		return;
	
	activeChar.stopMove(null, false);
	
	switch (sitem.getType())
	{
	case 0: // static summons (like Christmas tree)
		try
		{
			final L2Spawn spawn = new L2Spawn(npcTemplate);
			
			spawn.setId(IdFactory.getInstance().getNextId());
			spawn.setLocx(activeChar.getX());
			spawn.setLocy(activeChar.getY());
			spawn.setLocz(activeChar.getZ());
			spawn.setInstanceId(activeChar.getInstanceId());
			L2World.getInstance().storeObject(spawn.spawnOne(true));
			activeChar.destroyItem("Summon", item.getObjectId(), 1, null, false);
			activeChar.sendMessage("Created " + npcTemplate.name + " at x: " + spawn.getLocx() + " y: " + spawn.getLocy() + " z: " + spawn.getLocz());
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Target is not ingame.");
		}
		break;
	case 1: // pet summons
		final L2Object oldtarget = activeChar.getTarget();
		activeChar.setTarget(activeChar);
		Broadcast.toSelfAndKnownPlayersInRadius(activeChar, new MagicSkillUse(activeChar, 2046, 1, 5000, 0), 2000);
		activeChar.setTarget(oldtarget);
		activeChar.sendPacket(new SetupGauge(0, 5000));
		activeChar.sendPacket(new SystemMessage(SystemMessageId.SUMMON_A_PET));
		activeChar.setIsCastingNow(true);
		
		ThreadPoolManager.getInstance().scheduleGeneral(new PetSummonFinalizer(activeChar, npcTemplate, item), 5000);
		break;
	case 2: // wyvern
		activeChar.mount(sitem.getNpcId(), item.getObjectId(), true);
		break;
	case 3: // Great Wolf
		activeChar.mount(sitem.getNpcId(), item.getObjectId(), false);
		break;
	}
}

static class PetSummonFeedWait implements Runnable
{
private final L2PcInstance _activeChar;
private final L2PetInstance _petSummon;

PetSummonFeedWait(L2PcInstance activeChar, L2PetInstance petSummon)
{
	_activeChar = activeChar;
	_petSummon = petSummon;
}

public void run()
{
	try
	{
		if (_petSummon.getCurrentFed() <= 0)
			_petSummon.unSummon(_activeChar);
		else
			_petSummon.startFeed();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "", e);
	}
}
}

// TODO: this should be inside skill handler
static class PetSummonFinalizer implements Runnable
{
private final L2PcInstance _activeChar;
private final L2ItemInstance _item;
private final L2NpcTemplate _npcTemplate;

PetSummonFinalizer(L2PcInstance activeChar, L2NpcTemplate npcTemplate, L2ItemInstance item)
{
	_activeChar = activeChar;
	_npcTemplate = npcTemplate;
	_item = item;
}

public void run()
{
	try
	{
		_activeChar.sendPacket(new MagicSkillLaunched(_activeChar, 2046, 1));
		_activeChar.setIsCastingNow(false);
		
		// check for summon item validity
		if (_item == null
				|| _item.getOwnerId() != _activeChar.getObjectId()
				|| _item.getItemLocation() != L2ItemInstance.ItemLocation.INVENTORY)
			return;
		
		final L2PetInstance petSummon = L2PetInstance.spawnPet(_npcTemplate, _activeChar, _item);
		if (petSummon == null)
			return;
		
		petSummon.setTitle(_activeChar.getDisplayName());
		
		if (!petSummon.isRespawned())
		{
			petSummon.setCurrentHp(petSummon.getMaxHp());
			petSummon.setCurrentMp(petSummon.getMaxMp());
			petSummon.getStat().setExp(petSummon.getExpForThisLevel());
			petSummon.setCurrentFed(petSummon.getMaxFed());
		}
		
		petSummon.setRunning();
		
		if (!petSummon.isRespawned())
			petSummon.store();
		
		_activeChar.setPet(petSummon);
		
		petSummon.setInstanceId(_activeChar.getInstanceId());
		L2World.getInstance().storeObject(petSummon);
		petSummon.spawnMe(_activeChar.getX() + 50, _activeChar.getY() + 100, _activeChar.getZ());
		petSummon.startFeed();
		_item.setEnchantLevel(petSummon.getLevel());
		
		if (petSummon.getCurrentFed() <= 0)
			ThreadPoolManager.getInstance().scheduleGeneral(new PetSummonFeedWait(_activeChar, petSummon), 60000);
		else
			petSummon.startFeed();
		
		petSummon.setFollowStatus(true);
		petSummon.setShowSummonAnimation(false); // shouldn't be this always true?
		final int weaponId = petSummon.getWeapon();
		final int armorId = petSummon.getArmor();
		final int jewelId = petSummon.getJewel();
		if (weaponId > 0 && petSummon.getOwner().getInventory().getItemByItemId(weaponId)!= null)
		{
			final L2ItemInstance item = petSummon.getOwner().getInventory().getItemByItemId(weaponId);
			final L2ItemInstance newItem = petSummon.getOwner().transferItem("Transfer", item.getObjectId(), 1, petSummon.getInventory(), petSummon);
			if (newItem == null)
			{
				_log.warning("Invalid item transfer request: " + petSummon.getName() + "(pet) --> " + petSummon.getOwner().getName());
				petSummon.setWeapon(0);
			}
			else
				petSummon.getInventory().equipItem(newItem);
		}
		else
			petSummon.setWeapon(0);
		if (armorId > 0 && petSummon.getOwner().getInventory().getItemByItemId(armorId)!= null)
		{
			final L2ItemInstance item = petSummon.getOwner().getInventory().getItemByItemId(armorId);
			final L2ItemInstance newItem = petSummon.getOwner().transferItem("Transfer", item.getObjectId(), 1, petSummon.getInventory(), petSummon);
			if (newItem == null)
			{
				_log.warning("Invalid item transfer request: " + petSummon.getName() + "(pet) --> " + petSummon.getOwner().getName());
				petSummon.setArmor(0);
			}
			else
				petSummon.getInventory().equipItem(newItem);
		}
		else
			petSummon.setArmor(0);
		if (jewelId > 0 && petSummon.getOwner().getInventory().getItemByItemId(jewelId)!= null)
		{
			final L2ItemInstance item = petSummon.getOwner().getInventory().getItemByItemId(jewelId);
			final L2ItemInstance newItem = petSummon.getOwner().transferItem("Transfer", item.getObjectId(), 1, petSummon.getInventory(), petSummon);
			if (newItem == null)
			{
				_log.warning("Invalid item transfer request: " + petSummon.getName() + "(pet) --> " + petSummon.getOwner().getName());
				petSummon.setJewel(0);
			}
			else
				petSummon.getInventory().equipItem(newItem);
		}
		else
			petSummon.setJewel(0);
		petSummon.getOwner().sendPacket(new PetItemList(petSummon));
		petSummon.broadcastStatusUpdate();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "", e);
	}
}
}
}