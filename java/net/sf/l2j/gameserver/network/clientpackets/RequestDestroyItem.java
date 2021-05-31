package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.4.2.6 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestDestroyItem extends L2GameClientPacket
{
private static final String _C__59_REQUESTDESTROYITEM = "[C] 59 RequestDestroyItem";
private static Logger _log = Logger.getLogger(RequestDestroyItem.class.getName());

private int _objectId;
private long _count;

@Override
protected void readImpl()
{
	_objectId = readD();
	_count = readQ();
}

@Override
protected void runImpl()
{
	L2PcInstance activeChar = getClient().getActiveChar();
	if (activeChar == null)
		return;
	
	if(_count <= 0)
	{
		if (_count < 0)
			Util.handleIllegalPlayerAction(activeChar,"[RequestDestroyItem] Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " tried to destroy item with oid " + _objectId + " but has count < 0!", Config.DEFAULT_PUNISH);
		return;
	}
	
	if (!activeChar.getFloodProtectors().getTransaction().tryPerformAction("destroy"))
	{
		activeChar.sendMessage("You destroying items too fast.");
		return;
	}
	
	long count = _count;
	
	if (activeChar.getPrivateStoreType() != 0)
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
		return;
	}
	
	if (activeChar.isAccountLockedDown() || activeChar.isInJail())
	{
		activeChar.sendMessage("Your account is in lockdown");
		return;
	}
	
	L2ItemInstance itemToRemove = activeChar.getInventory().getItemByObjectId(_objectId);
	// if we can't find the requested item, its actually a cheat
	if (itemToRemove == null)
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
		return;
	}
	
	// Cannot discard item that the skill is consuming
	if (activeChar.isCastingNow())
	{
		if (activeChar.getCurrentSkill() != null && activeChar.getCurrentSkill().getSkill().getItemConsumeId() == itemToRemove.getItemId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}
	}
	// Cannot discard item that the skill is consuming
	if (activeChar.isCastingSimultaneouslyNow())
	{
		if (activeChar.getLastSimultaneousSkillCast() != null && activeChar.getLastSimultaneousSkillCast().getItemConsumeId() == itemToRemove.getItemId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}
	}
	
	int itemId = itemToRemove.getItemId();
	
	if (itemToRemove.isWear() || (!activeChar.isGM() && !itemToRemove.isDestroyable())
			|| CursedWeaponsManager.getInstance().isCursed(itemId))
	{
		if (itemToRemove.isHeroItem())
			activeChar.sendPacket(new SystemMessage(SystemMessageId.HERO_WEAPONS_CANT_DESTROYED));
		else
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
		return;
	}
	
	
	if(count > 1 && !itemToRemove.isStackable())
	{
		Util.handleIllegalPlayerAction(activeChar,"[RequestDestroyItem] count > 1 but item is not stackable! oid: "+_objectId+" owner: "+activeChar.getName(),Config.DEFAULT_PUNISH);
		return;
	}
	
	if (itemToRemove.isHeroItem() && !activeChar.isGM() && itemToRemove.getItem() instanceof L2Weapon)
	{
		if (activeChar._heroWpnDelCount >= 4)
		{
			activeChar.sendMessage("You can no longer delete your hero weapons this period since you have deleted 4 already");
			return;
		}
		else
		{
			activeChar._heroWpnDelCount++;
			activeChar.sendMessage("You have deleted your hero weapon and can now get another one. The limit is 4 per period and you have "+ (4 - activeChar._heroWpnDelCount) +" deletions left");
		}
	}
	
	if (_count > itemToRemove.getCount())
		count = itemToRemove.getCount();
	
	if (itemToRemove.isEquipped())
	{
		activeChar.abortAttack();
		activeChar.abortCast();
		
		L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(activeChar.getInventory().getSlotFromItem(itemToRemove));
		InventoryUpdate iu = new InventoryUpdate();
		for (L2ItemInstance item: unequiped)
		{
			iu.addModifiedItem(item);
		}
		activeChar.sendPacket(iu);
		activeChar.broadcastUserInfo();
	}
	
	if (L2PetDataTable.isPetItem(itemId))
	{
		Connection con = null;
		try
		{
			if (activeChar.getPet() != null && activeChar.getPet().getControlItemId() == _objectId)
			{
				activeChar.getPet().unSummon(activeChar);
			}
			
			// if it's a pet control item, delete the pet
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
			statement.setInt(1, _objectId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete pet objectid: ", e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}
	if (itemToRemove.isTimeLimitedItem())
		itemToRemove.endOfLife();
	L2ItemInstance removedItem = activeChar.getInventory().destroyItem("Destroy", _objectId, count, activeChar, null);
	
	if(removedItem == null)
		return;
	
	if (!Config.FORCE_INVENTORY_UPDATE)
	{
		InventoryUpdate iu = new InventoryUpdate();
		if (removedItem.getCount() == 0) iu.addRemovedItem(removedItem);
		else iu.addModifiedItem(removedItem);
		
		//client.getConnection().sendPacket(iu);
		activeChar.sendPacket(iu);
	}
	else sendPacket(new ItemList(activeChar, true));
	
	L2World.getInstance().removeObject(removedItem);
}

@Override
public String getType()
{
	return _C__59_REQUESTDESTROYITEM;
}
}
