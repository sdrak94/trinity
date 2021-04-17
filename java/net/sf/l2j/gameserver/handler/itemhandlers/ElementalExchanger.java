package net.sf.l2j.gameserver.handler.itemhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.Elementals;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.StringUtil;

public class ElementalExchanger implements IItemHandler
{
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		L2PcInstance activeChar = (L2PcInstance) playable;
		if (activeChar.getActiveWeaponInstance() == null || activeChar.getActiveWeaponInstance().getElementals() == null)
		{
			activeChar.sendMessage("Your Weapon slot does not meat proper requirements");
			return;
		}
		String filename = "data/html/custom/elemental_exchanger.htm";
		String content = HtmCache.getInstance().getHtm(filename);
		if (content == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
			activeChar.sendPacket(html);
		}
		else
		{
			byte ele_type = activeChar.getActiveWeaponInstance().getAttackElementType();
			int ele_power = activeChar.getActiveWeaponInstance().getAttackElementPower();
			int item_ench = activeChar.getActiveWeaponInstance().getEnchantLevel();
			String item_name = activeChar.getActiveWeaponInstance().getName();
			int ob_id = activeChar.getActiveWeaponInstance().getObjectId();
			
			String ele_name = Elementals.getElementName(ele_type);
			final StringBuilder elemental_htm = StringUtil.startAppend(1000, "");
			elemental_htm.append("<center><table>");
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setFile(filename);
			if (ele_power != 0)
			{
				elemental_htm.append("<tr><td align=center><button action=\"bypass -h ele_elemental_exchange 0 %ele_power% "+ob_id + "\" value=\"+%item_ench% %item_name% - %ele_name% (%ele_power%) -> Fire (%ele_power%) \" width=280 height=30 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td></tr>");
				elemental_htm.append("<tr><td align=center><button action=\"bypass -h ele_elemental_exchange 1 %ele_power% "+ob_id +"\" value=\"+%item_ench% %item_name% - %ele_name% (%ele_power%) -> Water (%ele_power%) \" width=280 height=30 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td></tr>");
				elemental_htm.append("<tr><td align=center><button action=\"bypass -h ele_elemental_exchange 2 %ele_power% "+ob_id + "\" value=\"+%item_ench% %item_name% - %ele_name% (%ele_power%) -> Wind (%ele_power%) \" width=280 height=30 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td></tr>");
				elemental_htm.append("<tr><td align=center><button action=\"bypass -h ele_elemental_exchange 3 %ele_power% "+ob_id + "\" value=\"+%item_ench% %item_name% - %ele_name% (%ele_power%) -> Earth (%ele_power%) \" width=280 height=30 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td></tr>");
				elemental_htm.append("<tr><td align=center><button action=\"bypass -h ele_elemental_exchange 4 %ele_power% "+ob_id + "\" value=\"+%item_ench% %item_name% - %ele_name% (%ele_power%) -> Holy (%ele_power%) \" width=280 height=30 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td></tr>");
				elemental_htm.append("<tr><td align=center><button action=\"bypass -h ele_elemental_exchange 5 %ele_power% "+ob_id + "\" value=\"+%item_ench% %item_name% - %ele_name% (%ele_power%) -> Dark (%ele_power%) \" width=280 height=30 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td></tr>");
			}
			itemReply.replace("%dtn%", elemental_htm.toString());
			itemReply.replace("%ele_name%", ele_name);
			itemReply.replace("%ele_power%", String.valueOf(ele_power));
			itemReply.replace("%item_ench%", String.valueOf(item_ench));
			itemReply.replace("%item_name%", item_name);
			itemReply.replace("%obj_Id%", String.valueOf(ob_id));
			// itemReply.replace("%dtn%", elemental_htm.toString());
			elemental_htm.append("</table></center>");
			activeChar.sendPacket(itemReply);
		}
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	final public static void onBypass(final L2PcInstance player, String action)
	{
		if (action.startsWith("elemental_exchange "))
		{
			StringTokenizer st = new StringTokenizer(action, " ");
			if (st.countTokens() != 4)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			st.nextToken();
			byte eleId = 0;
			int elePower = 0;
			int objId = 0;
			try
			{
				eleId = Byte.parseByte(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			try
			{
				elePower = Integer.parseInt(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			try
			{
				objId = Integer.parseInt(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			final L2ItemInstance oldWep = player.getActiveWeaponInstance();
			if (oldWep == null)
			{
				player.sendMessage("You don't have a weapon equipped; You can only change the weapon that you have equipped");
				return;
			}
			if (oldWep.isShadowItem())
			{
				player.sendMessage("You can't exchange a shadow weapon");
				return;
			}
			if (oldWep.getObjectId() != objId)
			{
				player.sendMessage("There's an error with your item");
				return;
			}
			player.getActiveWeaponInstance().setElementAttr(eleId, elePower);
			player.broadcastUserInfo();
			player.sendPacket(new ItemList(player, true));
			player.playSound("ItemSound.quest_finish");
			player.sendMessage("Congratulations! You have changed your element");
			player.destroyItemByItemId("Donate Clan", 600050, 1, player, true);
		}
	}
}
