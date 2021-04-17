package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExVariationResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestRefine extends AbstractRefinePacket
{
	private static final String	_C__D0_2C_REQUESTREFINE	= "[C] D0:2C RequestRefine";
	private int					_targetItemObjId;
	private int					_refinerItemObjId;
	private int					_gem;
	
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
		_gem = readD();
		readQ();
	}
	
	public int getItem()
	{
		return _targetItemObjId;
	}
	
	public int getLs()
	{
		return _refinerItemObjId;
	}
	
	public int getGem()
	{
		return _gem;
	}
	
	protected void runImpl()
	{
		L2PcInstance activeChar = ((L2GameClient) getClient()).getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		if (activeChar.isAccountLockedDown())
		{
			activeChar.sendMessage("Your account is in lockdown");
			return;
		}
		L2ItemInstance targetItem = (L2ItemInstance) L2World.getInstance().findObject(_targetItemObjId);
		if (targetItem == null)
			return;
		L2ItemInstance refinerItem = (L2ItemInstance) L2World.getInstance().findObject(_refinerItemObjId);
		if (refinerItem == null)
		{
			return;
		}
		if (!isValid(activeChar, targetItem, refinerItem))
		{
			activeChar.sendPacket((L2GameServerPacket) new ExVariationResult(0, 0, 0));
			activeChar.sendPacket((L2GameServerPacket) new SystemMessage(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS));
			return;
		}
		AbstractRefinePacket.LifeStone ls = getLifeStone(refinerItem.getItemId());
		if (ls == null)
		{
			return;
		}
		int lifeStoneLevel = ls.getLevel();
		int lifeStoneGrade = ls.getGrade();
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
		}
		if (!activeChar.destroyItem("RequestRefine", refinerItem, 1L, null, false))
		{
			return;
		}
		if (!refinerItem.isTradeable() && refinerItem.getUntradeableTime() > System.currentTimeMillis())
		{
			if (targetItem.getUntradeableTime() < refinerItem.getUntradeableTime())
			{
				targetItem.setUntradeableTimer(refinerItem.getUntradeableTime());
				activeChar.sendMessage("Your " + targetItem.getName() + " is now untradeable for " + (refinerItem.getUntradeableTime() / 3600000L) + "" + " hours due to your life stone being temporarily untradeable");
			}
		}
		boolean doYouHavePride = false;
		if (targetItem.isJewelry())
		{
			switch (targetItem.getItemId())
			{
				case 9455:
				case 9456:
				case 9457:
				case 9458:
				case 9460:
				case 14163:
				case 14164:
				case 14165:
				case 850000: //Superior Valakas' Fiery			
				case 850001: //Superior Antharas' Tremor		
				case 850002: //Superior Baium's Anger			
				case 850003: //Superior Zaken's Dementia		
				case 850004: //Superior Queen's Grasp			
				case 850005: //Superior Frintezza's Phylactery	
				case 850006: //Superior Beleth's Ring			
				case 850007: //Superior Baylor's Earring		
				case 850008: //Superior Orfen's Wrath 			
				case 850009: //Superior Core's Soul				
				case 850010: //Superior Helbram's Ring
					doYouHavePride = true;
					break;
			}
		}
		else if (targetItem.getItem().getBodyPart() == 268435456)
		{
			doYouHavePride = true;
		}
//		L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(lifeStoneLevel, lifeStoneGrade, targetItem.getItem().getBodyPart(), doYouHavePride);
//		targetItem.setAugmentation(aug);
//		int stat12 = 0xFFFF & aug.getAugmentationId();
//		int stat34 = aug.getAugmentationId() >> 16;
//		activeChar.sendPacket((L2GameServerPacket) new ExVariationResult(stat12, stat34, 1));
//		activeChar.sendPacket((L2GameServerPacket) new SystemMessage(SystemMessageId.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED));
//		activeChar._itemAug = _targetItemObjId;
//		activeChar._lsAug = _refinerItemObjId;
//		activeChar._gemAug = _gem;
//		activeChar.sendPacket((L2GameServerPacket) new ExShowVariationCancelWindow());
//		activeChar.sendPacket((L2GameServerPacket) new ExPutItemResultForVariationCancel(_targetItemObjId, 0));
//		InventoryUpdate iu = new InventoryUpdate();
//		iu.addModifiedItem(targetItem);
//		activeChar.sendPacket((L2GameServerPacket) iu);
		

		L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(lifeStoneLevel, lifeStoneGrade, targetItem.getItem().getBodyPart(), doYouHavePride);
		targetItem.setAugmentation(aug);

		int stat12 = 0xFFFF & aug.getAugmentationId();
		int stat34 = aug.getAugmentationId() >> 16;
		activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
		activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED));
		
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		activeChar.sendPacket(iu);
		
//		StatusUpdate su = new StatusUpdate(activeChar);
//		su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
//		activeChar.sendPacket(su);
	}
	
	public String getType()
	{
		return "[C] D0:2C RequestRefine";
	}
}
