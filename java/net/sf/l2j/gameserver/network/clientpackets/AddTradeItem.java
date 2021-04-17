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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExRpItemLink;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.TradeOtherAdd;
import net.sf.l2j.gameserver.network.serverpackets.TradeOwnAdd;

/**
 * This class ...
 *
 * @version $Revision: 1.5.2.2.2.5 $ $Date: 2005/03/27 15:29:29 $
 */
public final class AddTradeItem extends L2GameClientPacket
{
	private static final String _C__16_ADDTRADEITEM = "[C] 16 AddTradeItem";
	private static final Logger _log = Logger.getLogger(AddTradeItem.class.getName());

	private int _tradeId;
	private int _objectId;
	private long _count;

	public AddTradeItem()
	{
	}

	@Override
	protected void readImpl()
	{
		_tradeId = readD();
		_objectId = readD();
		_count = readQ();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		final TradeList trade = player.getActiveTradeList();
		if (trade == null)
		{
			_log.warning("Character: " + player.getName()
					+ " requested item:" + _objectId
					+ " add without active tradelist:" + _tradeId);
			return;
		}

		final L2PcInstance partner = trade.getPartner(); 
		if (partner == null
				|| L2World.getInstance().findObject(partner.getObjectId()) == null
				|| partner.getActiveTradeList() == null)
		{
			// Trade partner not found, cancel trade
			if (partner != null)
				_log.warning("Character:" + player.getName()
						+ " requested invalid trade object: " + _objectId);
			SystemMessage msg = new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
			player.sendPacket(msg);
			player.cancelActiveTrade();
			return;
		}

		if (trade.isConfirmed() || partner.getActiveTradeList().isConfirmed())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_ADJUST_ITEMS_AFTER_TRADE_CONFIRMED));
			return;
		}

		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disable for your Access Level");
			player.cancelActiveTrade();
			return;
		}

		if (!player.validateItemManipulation(_objectId, "trade"))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
			return;
		}

		final TradeList.TradeItem item = trade.addItem(_objectId, _count);
		if (item != null)
		{
			//final L2ItemInstance object = player.getInventory().getItemByObjectId(_objectId);
			
				//if (object.isAugmented())
				//{
				//trade.getPartner().sendMessage(player.getName() + " added " + item.getItem().getName() + " with the following stats:");
				//	for (AugStat param : AugmentationData.getInstance().getAugStatsById(object.getAugmentation().getAugmentationId()))
				//		trade.getPartner().sendMessage(param.getStat().getName() + " +" + param.getValue());
				//}
			player.sendPacket(new TradeOwnAdd(item));
			trade.getPartner().sendPacket(new TradeOtherAdd(item));
		}
		L2ItemInstance itemInstance = player.getInventory().getItemByObjectId(_objectId);
		if (itemInstance != null && (itemInstance.isAugmented()))
		{
				partner.sendPacket(new ExRpItemLink(itemInstance));
				partner.sendPacket(new CreatureSay(player.getObjectId(), 2, player.getName()+ " (Current Trade)", "offers Augmented \b\tType=1 \tID="+ _objectId +" \tColor=0 \tUnderline=1 \tTitle=" + ((char) 27) + itemInstance.getName() + ((char) 27) + "\b"));				
			/*else if (!itemInstance.isAugmented())
			{
				partner.sendPacket(new ExRpItemLink(itemInstance));
				partner.sendPacket(new CreatureSay(player.getObjectId(), 2, player.getName()+ " (Current Trade)", "offers \b\tType=1 \tID="+ _objectId +" \tColor=0 \tUnderline=1 \tTitle=" + ((char) 27) + itemInstance.getName() + ((char) 27) + "\b"));
			}*/
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__16_ADDTRADEITEM;
	}
}
