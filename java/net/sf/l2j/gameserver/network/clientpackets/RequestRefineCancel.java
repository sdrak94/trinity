package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExPutCommissionResultForVariationMake;
import net.sf.l2j.gameserver.network.serverpackets.ExPutIntensiveResultForVariationMake;
import net.sf.l2j.gameserver.network.serverpackets.ExPutItemResultForVariationMake;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import net.sf.l2j.gameserver.network.serverpackets.ExVariationCancelResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

public final class RequestRefineCancel extends L2GameClientPacket
{
	private static final String	_C__D0_2E_REQUESTREFINECANCEL	= "[C] D0:2E RequestRefineCancel";
	private int					_targetItemObjId;
	private int					ls;
	private int					gem;
	
	protected void readImpl()
	{
		_targetItemObjId = readD();
	}
	
	protected void runImpl()
	{
		L2PcInstance activeChar = ((L2GameClient) getClient()).getActiveChar();
		if (activeChar == null)
			return;
		if (activeChar.isAccountLockedDown())
		{
			activeChar.sendMessage("Your account is in lockdown");
			return;
		}
		L2ItemInstance targetItem = (L2ItemInstance) L2World.getInstance().findObject(_targetItemObjId);
		if (targetItem == null)
		{
			activeChar.sendPacket((L2GameServerPacket) new ExVariationCancelResult(0));
			return;
		}
		if (targetItem.getOwnerId() != activeChar.getObjectId())
		{
			Util.handleIllegalPlayerAction(((L2GameClient) getClient()).getActiveChar(), "Warning!! Character " + ((L2GameClient) getClient()).getActiveChar().getName() + " of account " + ((L2GameClient) getClient()).getActiveChar().getAccountName() + " tryied to augment item that doesn't own.", Config.DEFAULT_PUNISH);
			return;
		}
		if (!targetItem.isAugmented())
		{
			activeChar.sendPacket((L2GameServerPacket) new SystemMessage(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM));
			activeChar.sendPacket((L2GameServerPacket) new ExVariationCancelResult(0));
			return;
		}
		switch (targetItem.getItem().getItemGradeSPlus())
		{
			case 2:
			case 3:
			case 4:
			case 5:
				break;
			default:
				if (targetItem.getItemId() == 20325)
					break;
				activeChar.sendPacket((L2GameServerPacket) new ExVariationCancelResult(0));
				return;
		}
		if (targetItem.isEquipped())
		{
			activeChar.abortAttack();
			activeChar.abortCast();
			L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(activeChar.getInventory().getSlotFromItem(targetItem));
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			activeChar.sendPacket((L2GameServerPacket) iu);
			activeChar.broadcastUserInfo();
			ItemList il = new ItemList(activeChar, true);
			activeChar.sendPacket((L2GameServerPacket) il);
		}
		targetItem.removeAugmentation();
		activeChar.sendPacket((L2GameServerPacket) new ExVariationCancelResult(1));
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		activeChar.sendPacket((L2GameServerPacket) iu);
		SystemMessage sm = new SystemMessage(SystemMessageId.AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1);
		sm.addString(targetItem.getItemName());
		activeChar.sendPacket((L2GameServerPacket) sm);
		ls = activeChar._lsAug;
		gem = activeChar._gemAug;
		L2ItemInstance refinerItem = (L2ItemInstance) L2World.getInstance().findObject(ls);
		// int refinerItemId = refinerItem.getItem().getItemId();
		if (ls != 0)
		{
			activeChar.sendPacket((L2GameServerPacket) new ExShowVariationMakeWindow());
			activeChar.sendPacket((L2GameServerPacket) new ExPutItemResultForVariationMake(_targetItemObjId));
			activeChar.sendPacket((L2GameServerPacket) new ExPutIntensiveResultForVariationMake(_targetItemObjId, 57, 57, 0));
			activeChar.sendPacket((L2GameServerPacket) new ExPutCommissionResultForVariationMake(gem, 1L, 2131));
		}
		else
		{
			activeChar.sendPacket((L2GameServerPacket) new ExShowVariationMakeWindow());
		}
	}
	
	public String getType()
	{
		return "[C] D0:2E RequestRefineCancel";
	}
}
