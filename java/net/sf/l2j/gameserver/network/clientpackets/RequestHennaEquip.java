package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.datatables.HennaTreeTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Henna;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public final class RequestHennaEquip extends L2GameClientPacket
{
	private static final String _C__BC_RequestHennaEquip = "[C] bc RequestHennaEquip";
	//private static Logger _log = Logger.getLogger(RequestHennaEquip.class.getName());
	private int _symbolId;
	// format  cd

	/**
	 * packet type id 0xbb
	 * format:		cd
	 * @param decrypt
	 */
	@Override
	protected void readImpl()
	{
		_symbolId  = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		final L2Henna template = HennaTable.getInstance().getTemplate(_symbolId);

		if (template == null)
			return;
		
		/* Prevents henna drawing exploit: 
           1) talk to L2SymbolMakerInstance 
    	   2) RequestHennaList
    	   3) Don't close the window and go to a GrandMaster and change your subclass
    	   4) Get SymbolMaker range again and press draw
    	   You could draw any kind of henna just having the required subclass...
    	 */

		boolean cheater = true;
		for (L2Henna h : HennaTreeTable.getInstance().getAvailableHenna(activeChar.getClassId()))
		{
			if (h.getSymbolId() == template.getSymbolId()) 
			{
				cheater = false;
				break;
			}
		}
		/*try
		{
			_count = activeChar.getInventory().getItemByItemId(temp.getItemIdDye()).getCount();
		}
		catch(Exception e)
		{
			//
		}*/

		if (activeChar.getHennaEmptySlots() == 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.SYMBOLS_FULL));
			return;
		}

		if (!cheater && (!activeChar.isInOlympiadMode()) && /*(_count >= temp.getAmountDyeRequire())&& */(activeChar.getAdena()>= template.getPrice()) && activeChar.addHenna(template))
		{
			/*SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			sm.addItemName(temp.getItemIdDye());
			sm.addItemNumber(temp.getAmountDyeRequire());
			activeChar.sendPacket(sm);
			sm = null;*/
			

			//HennaInfo hi = new HennaInfo(temp,activeChar);
			//activeChar.sendPacket(hi);

			activeChar.getInventory().reduceAdena("Henna", template.getPrice(), activeChar, null);
			/*L2ItemInstance dyeToUpdate = activeChar.getInventory().destroyItemByItemId("Henna", temp.getItemIdDye(),temp.getAmountDyeRequire(), activeChar, activeChar.getLastFolkNPC());*/
			
			activeChar.sendPacket(new SystemMessage(SystemMessageId.SYMBOL_ADDED));

			//update inventory
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(activeChar.getInventory().getAdenaInstance());
			/*iu.addModifiedItem(dyeToUpdate);*/
			activeChar.sendPacket(iu);
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_DRAW_SYMBOL));
			if ((!activeChar.isGM()) && (cheater))
				Util.handleIllegalPlayerAction(activeChar,"Exploit attempt: Character "+activeChar.getName()+" of account "+activeChar.getAccountName()+" tryed to add a forbidden henna.",Config.DEFAULT_PUNISH);
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__BC_RequestHennaEquip;
	}
}
