package luna.custom.handler.items.bdoBox;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.StringUtil;

public class BDOBox implements IItemHandler
{
	Logger _log = Logger.getLogger(ItemTable.class.getName());
	
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		BdoData.getInstance();
		final L2PcInstance activeChar = (L2PcInstance) playable;
		int itemId = item.getItemId();
		if (BdoData.getInstance().getBox(itemId) == null)
		{
			System.out.println("Box Id " + itemId + " doesn't exist in BdoBoxes.xml");
		}
		else
		{
			String filename = "data/html/custom/bdobox.htm";
			final StringBuilder weaponHTML = StringUtil.startAppend(1000, "");
			weaponHTML.append("<center><table width = 300>");
			int counter = 0;
			for (int i = 0; i <  BdoData.getInstance().getRewards(itemId).size(); i++)
			{
				String itemName = "+" + BdoData.getInstance().getRewards(itemId).get(i).getEnc() + " " + ItemTable.getInstance().getTemplate(BdoData.getInstance().getRewards(itemId).get(i).getItemId()).getName();
				StringUtil.append(weaponHTML, "<tr><td><img src="+ItemTable.getInstance().getTemplate(BdoData.getInstance().getRewards(itemId).get(i).getItemId()).getIcon()+" width=32 height=32></td><td width=200><a action=\"bypass -h bdoBox_getItem_ ", String.valueOf(i)," ", String.valueOf(itemId),"\">", itemName, "</a></td></tr><br1>");
				counter++;
			}
			weaponHTML.append("</table></center><br1><br1><br1>");
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setFile(filename);
			if (counter <= 0)
			{
				itemReply.replace("%dtn%", "This box is empty");
			}
			else
			{
				itemReply.replace("%dtn%", weaponHTML.toString());
				itemReply.replace("%nameItem%", ItemTable.getInstance().getTemplate(itemId).getName());
			}
			activeChar.sendPacket(itemReply);
		}
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
