package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestAutoSoulShot extends L2GameClientPacket
{
	private static final String	_C__CF_REQUESTAUTOSOULSHOT	= "[C] CF RequestAutoSoulShot";
	private static Logger		_log						= Logger.getLogger(RequestAutoSoulShot.class.getName());
	// format cd
	private int					_itemId;
	private int					_type;																				// 1 = on : 0 = off;
	
	@Override
	protected void readImpl()
	{
		_itemId = readD();
		_type = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		if (activeChar.getPrivateStoreType() == 0 && activeChar.getActiveRequester() == null && !activeChar.isDead())
		{
			_log.fine("AutoSoulShot:" + _itemId);
			L2ItemInstance item = activeChar.getInventory().getItemByItemId(_itemId);
			if (item != null)
			{
				if (_type == 1)
				{
					// Fishingshots are not automatic on retail
					if (_itemId < 6535 || _itemId > 6540)
					{
						// Attempt to charge first shot on activation
						if (_itemId == 6645 || _itemId == 6646 || _itemId == 6647 || _itemId == 20332 || _itemId == 20333 || _itemId == 20334)
						{
							// activeChar.addAutoSoulShot(_itemId);
							ExAutoSoulShot atk = new ExAutoSoulShot(_itemId, _type);
							activeChar.sendPacket(atk);
							// start the auto soulshot use
							SystemMessage sm = new SystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO);
							sm.addItemName(item);// Update Message by rocknow
							activeChar.sendPacket(sm);
							sm = null;
							// activeChar.rechargeAutoSoulShot(true, true, true);
						}
						else
						{
							if (activeChar.getActiveWeaponItem() != activeChar.getFistsWeaponItem())
							{
								if (item.getItem().getCrystalType() == activeChar.getActiveWeaponItem().getItemGradeSPlus())
								{
									// activeChar.addAutoSoulShot(_itemId);
									ExAutoSoulShot atk = new ExAutoSoulShot(_itemId, _type);
									activeChar.sendPacket(atk);
									// start the auto soulshot use
									SystemMessage sm = new SystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO);
									sm.addItemName(item);// Update Message by rocknow
									activeChar.sendPacket(sm);
									sm = null;
									// activeChar.rechargeAutoSoulShot(true, true, false);
								}
							}
							else
							{
								if ((_itemId >= 2509 && _itemId <= 2514) || (_itemId >= 3947 && _itemId <= 3952) || _itemId == 5790 || (_itemId >= 22072 && _itemId <= 22081))
									activeChar.sendPacket(new SystemMessage(SystemMessageId.SPIRITSHOTS_GRADE_MISMATCH));
								else
									activeChar.sendPacket(new SystemMessage(SystemMessageId.SOULSHOTS_GRADE_MISMATCH));
							}
						}
					}
				}
				else if (_type == 0)
				{
					// activeChar.removeAutoSoulShot(_itemId);
					ExAutoSoulShot atk = new ExAutoSoulShot(_itemId, _type);
					activeChar.sendPacket(atk);
					// cancel the auto soulshot use
					SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
					sm.addItemName(item);// Update Message by rocknow
					activeChar.sendPacket(sm);
					sm = null;
				}
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__CF_REQUESTAUTOSOULSHOT;
	}
}