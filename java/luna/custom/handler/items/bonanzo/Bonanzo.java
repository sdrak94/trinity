package luna.custom.handler.items.bonanzo;

import java.util.logging.Logger;

import luna.custom.handler.items.bonanzo.BonanzoData.RewardData;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.TutorialShowHtml;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.StringUtil;

public class Bonanzo implements IItemHandler
{
	Logger _log = Logger.getLogger(ItemTable.class.getName());
	
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		BonanzoData.getInstance();
		final L2PcInstance activeChar = (L2PcInstance) playable;
		StringBuilder rewardStr = StringUtil.startAppend(1000, "");
		int itemId = item.getItemId();
		String bgcolor = "bgcolor=FF0000";
		if (BonanzoData.getInstance().getBox(itemId) == null)
		{
			System.out.println("Box Id " + itemId + " doesn't exist in BonanzoBoxes.xml");
		}
		else
		{
			for (RewardData data : BonanzoData.getInstance().getBox(item.getItemId()))
			{
				L2Item reward = ItemTable.getInstance().getTemplate(data.getItemId());
				String aug = data.getAug() ? "(Augmented) " : "";
				String icon = reward.getIcon();
				String name = aug + reward.getName();
				int ammount = data.getAmount();
				int chance = data.getChance() / 100;
				
				//Announcements.getInstance().announceToAll("Chance:" + chance + " Raw Chance: " +data.getChance() + " Display Chance:" +  data.getChanceDisp());
				if(data.getChanceDisp() != -1)
				{
					chance = data.getChanceDisp();
				}
				
				String enchant = "";

				if (!data.getEnc().equalsIgnoreCase("0"))
				{
					enchant = "+" + String.valueOf(data.getEnc());
				}
				String ammountStr = "";
				if (chance >= 100)
					bgcolor = "color=55ff55";
				else if (chance >= 50)
					bgcolor = "color=33ffff";
				else if (chance >= 15)
					bgcolor = "color=3366ff";
				else if (chance >= 3)
					bgcolor = "color=3239ff";
				else if (chance >= 1)
					bgcolor = "color=ff5194";
				else
					bgcolor = "color=ff3310";
				if (chance <= 100)
					bgcolor = "color=55ff55";
				if (chance <= 80)
					bgcolor = "color=1C43C2";
				if (chance <= 50)
					bgcolor = "color=6608FB";
				if (chance <= 35)
					bgcolor = "color=58388D";
				if (chance <= 25)
					bgcolor = "color=594776";
				if (chance <= 10)
					bgcolor = "color=522138";
				if (chance <= 3)
					bgcolor = "color=9F0F11";
				if (ammount >= 1)
				{
					ammountStr = "(x" + ammount + ")";
				}
				if (!enchant.equalsIgnoreCase("+0-0"))
				{
					ammountStr += " " + enchant;
				}
				StringUtil.append(rewardStr, "<table width=400 ><tr>" + "        <td width=44 height=36 align=center>" + "            <table cellpadding=6 cellspacing=-5>" + "                <tr>" + "                    <td>" + "                        <button width=32 height=32 back=" + icon + " fore=" + icon + ">" + "                    </td>" + "                </tr>" + "            </table>" + "        </td>" + "        <td width=220 align=left><font color=>" + ammountStr + "  " + name + "</font> " + "            <br>" + "		</td>" + "        <td width=110 align=center>" + "			<font " + bgcolor + "> chance: " + chance + "%</font> " + "            " + "		</td>" + "    </tr></table>");
			}
		}
		final String filename = "data/html/custom/Bonanzo/bonanzo.htm";
		final String content = HtmCache.getInstance().getHtm(filename);
		// NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
		// itemReply.setFile(filename);
		// itemReply.replace("%rewards%", rewardStr.toString());
		if (content == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
			String fp = content;
			activeChar.sendPacket(new TutorialShowHtml(fp));
		}
		else
		{
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setFile(filename);
			String rewards = rewardStr.toString();
			// String itemId = String.valueOf(item.getItemId());
			itemReply.replace("%title%", "" + ItemTable.getInstance().getTemplate(itemId).getName());
			itemReply.replace("%id%", "" + item.getItemId());
			itemReply.replace("%rewards%", rewards);
			String qsb = itemReply.getText();
			activeChar.sendPacket(new TutorialShowHtml(qsb));
		}
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
