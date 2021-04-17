package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.PvPColorChanger;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;


public class PvPColorChange implements IItemHandler
{
	private static final int[] ITEM_IDS = { 99985 };
	
    public int[] getItemIds()
    {
    	return ITEM_IDS;
    }

	@Override
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		System.out.println("asasd");
		if (!(playable instanceof L2PcInstance))
	          return;
	        
	    L2PcInstance activeChar = (L2PcInstance)playable;
	    PvPColorChanger.showMenu(activeChar);
	}
}  