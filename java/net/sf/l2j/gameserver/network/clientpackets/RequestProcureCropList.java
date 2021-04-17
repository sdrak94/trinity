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
package net.sf.l2j.gameserver.network.clientpackets;

import static net.sf.l2j.gameserver.model.actor.L2Npc.INTERACTION_DISTANCE;
import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.MAX_ADENA;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager.CropProcure;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2ManorManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * Format: (ch) d [dddd]
 * d: size
 * [
 * d  obj id
 * d  item id
 * d  manor id
 * d  count
 * ]
 * @author l3x
 *
 */
public class RequestProcureCropList extends L2GameClientPacket
{
	private static final String _C__D0_09_REQUESTPROCURECROPLIST = "[C] D0:09 RequestProcureCropList";

	private static final int BATCH_LENGTH = 20; // length of the one item

	private Crop[] _items = null;

	@Override
	protected void readImpl()
	{
		int count = readD();
		if(count <= 0
				|| count > Config.MAX_ITEM_IN_PACKET
				|| count * BATCH_LENGTH != _buf.remaining())
		{
			return;
		}

		_items = new Crop[count];
		for (int i = 0; i < count; i++)
		{
			int objId = readD();
			int itemId = readD();
			int manorId = readD();
			long cnt = readQ();
			if (objId < 1 || itemId < 1 || manorId < 0 || cnt < 0)
			{
				_items = null;
				return;
			}
			_items[i] = new Crop(objId, itemId, manorId, cnt);
		}
	}

	@Override
    protected void runImpl()
	{
		if (_items == null)
			return;

		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		L2Object manager = player.getTarget();

		if (!(manager instanceof L2ManorManagerInstance))
			manager = player.getLastFolkNPC();

		if (!(manager instanceof L2ManorManagerInstance))
			return;

		if (!player.isInsideRadius(manager, INTERACTION_DISTANCE, false, false))
			return;

		if (_items == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		int castleId = ((L2ManorManagerInstance)manager).getCastle().getCastleId();

		// Calculate summary values
		int slots = 0;
		int weight = 0;

		for (Crop i : _items)
		{
			if (!i.getCrop())
				continue;

			L2Item template = ItemTable.getInstance().getTemplate(i.getReward());
			weight += i.getCount() * template.getWeight();

			if (!template.isStackable())
				slots += i.getCount();
			else if (player.getInventory().getItemByItemId(i.getItemId()) == null)
				slots++;
		}

		if (!player.getInventory().validateWeight(weight))
		{
			sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}

		if (!player.getInventory().validateCapacity(slots))
		{
			sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}

		// Proceed the purchase
		InventoryUpdate playerIU = new InventoryUpdate();

		for (Crop i : _items)
		{
			if (i.getReward() == 0)
				continue;

			long fee = i.getFee(castleId); // fee for selling to other manors

			long rewardPrice = ItemTable.getInstance().getTemplate(i.getReward()).getReferencePrice();
			if (rewardPrice == 0)
				continue;

			long rewardItemCount = i.getPrice() / rewardPrice;
			if (rewardItemCount < 1)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(i.getItemId());
				sm.addItemNumber(i.getCount());
				player.sendPacket(sm);
				continue;
			}

			if (player.getInventory().getAdena() < fee)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(i.getItemId());
				sm.addItemNumber(i.getCount());
				player.sendPacket(sm);
				sm = new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				player.sendPacket(sm);
				continue;
			}

			// Add item to Inventory and adjust update packet
			L2ItemInstance itemDel = null;
			L2ItemInstance itemAdd = null;

			// check if player have correct items count
			L2ItemInstance item = player.getInventory().getItemByObjectId(i.getObjectId());
			if (item == null || item.getCount() < i.getCount())
				continue;

			itemDel = player.getInventory().destroyItem("Manor", i.getObjectId(), i.getCount(), player, manager);
			if (itemDel == null)
				continue;

			if (fee > 0)
				player.getInventory().reduceAdena("Manor", fee, player, manager);

			i.setCrop();

			itemAdd = player.getInventory().addItem("Manor", i.getReward(), rewardItemCount, player, manager);
			if (itemAdd == null)
				continue;

			playerIU.addRemovedItem(itemDel);
			if (itemAdd.getCount() > rewardItemCount)
				playerIU.addModifiedItem(itemAdd);
			else
				playerIU.addNewItem(itemAdd);

			// Send System Messages
			SystemMessage sm = new SystemMessage(
					SystemMessageId.TRADED_S2_OF_CROP_S1);
			sm.addItemName(i.getItemId());
			sm.addItemNumber(i.getCount());
			player.sendPacket(sm);

			if (fee > 0)
			{
				sm = new SystemMessage(SystemMessageId.S1_ADENA_HAS_BEEN_WITHDRAWN_TO_PAY_FOR_PURCHASING_FEES);
				sm.addItemNumber(fee);
				player.sendPacket(sm);
			}

			sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			sm.addItemName(i.getItemId());
			sm.addItemNumber(i.getCount());
			player.sendPacket(sm);

			if (fee > 0)
			{
				sm = new SystemMessage(SystemMessageId.DISAPPEARED_ADENA);
				sm.addItemNumber(fee);
				player.sendPacket(sm);
			}

			sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
			sm.addItemName(itemAdd);
			sm.addItemNumber(rewardItemCount);
			player.sendPacket(sm);
		}

		// Send update packets
		player.sendPacket(playerIU);

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}

	private class Crop
	{
		private final int _objectId;
		private final int _itemId;
		private final int _manorId;
		private final long _count;
		private int _reward = 0;
		private CropProcure _crop = null;
		
		public Crop(int obj, int id, int m, long num)
		{
			_objectId = obj;
			_itemId = id;
			_manorId = m;
			_count = num;
		}

		public int getObjectId()
		{
			return _objectId;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public long getCount()
		{
			return _count;
		}

		public int getReward()
		{
			return _reward;
		}

		public long getPrice()
		{
			return _crop.getPrice() * _count;
		}

		public long getFee(int castleId)
		{
			if (_manorId == castleId)
				return 0;

			return (getPrice() /100) * 5; // 5% fee for selling to other manor
		}

		public boolean getCrop()
		{
			try
			{
				_crop = CastleManager.getInstance().getCastleById(_manorId).getCrop(_itemId, CastleManorManager.PERIOD_CURRENT);
			}
			catch (NullPointerException e)
			{
				return false;
			}
			if (_crop == null || _crop.getId() == 0 || _crop.getPrice() == 0)
				return false;

			if (_count > _crop.getAmount())
				return false;

			if ((MAX_ADENA / _count) < _crop.getPrice())
				return false;

			_reward = L2Manor.getInstance().getRewardItem(_itemId, _crop.getReward()); 
			return true;
		}

		public void setCrop()
		{
			_crop.setAmount(_crop.getAmount() - _count);
			if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				CastleManager.getInstance().getCastleById(_manorId).updateCrop(_itemId, _crop.getAmount(), CastleManorManager.PERIOD_CURRENT);
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_09_REQUESTPROCURECROPLIST;
	}
}
