package luna.custom.DressMeEngine;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.util.FastMap;
import luna.custom.Conditions;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ArmorSetsTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.L2ArmorSet;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;

public class DressMeVCmd implements IVoicedCommandHandler
{
	private static final Logger	_log					= LoggerFactory.getLogger(DressMeVCmd.class);
	String						index_path				= "data/html/sunrise/dressme/index.htm";
	String						info_path				= "data/html/sunrise/dressme/info.htm";
	String						undressme_path			= "data/html/sunrise/dressme/undressme.htm";
	String						index_armor_path		= "data/html/sunrise/dressme/index-armor.htm";
	String						template_armor_path		= "data/html/sunrise/dressme/template-armor.htm";
	String						index_cloak				= "data/html/sunrise/dressme/index-cloak.htm";
	String						template_cloak_path		= "data/html/sunrise/dressme/template-cloak.htm";
	String						index_shield_path		= "data/html/sunrise/dressme/index-shield.htm";
	String						template_shield_path	= "data/html/sunrise/dressme/template-shield.htm";
	String						index_weapon_path		= "data/html/sunrise/dressme/index-weapon.htm";
	String						template_weapon_path	= "data/html/sunrise/dressme/template-weapon.htm";
	String						index_hat_path			= "data/html/sunrise/dressme/index-hat.htm";
	String						template_hat_path		= "data/html/sunrise/dressme/template-hat.htm";
	String						dress_cloak_path		= "data/html/sunrise/dressme/dress-cloak.htm";
	String						dress_shield_path		= "data/html/sunrise/dressme/dress-shield.htm";
	String						dress_armor_path		= "data/html/sunrise/dressme/dress-armor.htm";
	String						dress_weapon_path		= "data/html/sunrise/dressme/dress-weapon.htm";
	String						dress_hat_path			= "data/html/sunrise/dressme/dress-hat.htm";
	private final String[]		_commandList			= new String[]
	{
		"dressme",
		"undressme",
		"dressinfo",
		"showdress",
		"hidedress",
		"dressme-armor",
		"dress-armor",
		"dress-armorpage",
		"undressme-armor",
		"dressme-cloak",
		"dress-cloak",
		"dress-cloakpage",
		"undressme-cloak",
		"dressme-shield",
		"dress-shield",
		"dress-shieldpage",
		"undressme-shield",
		"dressme-weapon",
		"dress-weapon",
		"dress-weaponpage",
		"undressme-weapon",
		"dressme-hat",
		"dress-hat",
		"dress-hatpage",
		"undressme-hat",
		"dress-cloaktry",
		"dress-shieldtry",
		"dress-weapontry",
		"dress-armortry",
		"dress-hattry",
		"dress-armor-limitedTime",
		"dress-hat-limitedTime"
	};
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance player, String args)
	{
		if (command.equals("dressme"))
		{
			String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), index_path);
			html = html.replace("<?show_hide?>", !player.getVarB("showVisualChange") ? "Show visual equip on other player!" : "Hide visual equip on other player!");
			html = html.replace("<?show_hide_b?>", !player.getVarB("showVisualChange") ? "showdress" : "hidedress");
			sendHtml(player, html);
			return true;
		}
		else if (command.equals("dressme-armor"))
		{
			L2ItemInstance slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if ((slot == null) || !slot.isArmor())
			{
				player.sendMessage("Error: Armor chest must be equiped!");
				return false;
			}
			String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), index_armor_path);
			String template = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), template_armor_path);
			String block = "";
			String list = "";
			if (args == null)
			{
				args = "1";
			}
			String[] param = args.split(" ");
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 5;
			int counter = 0;
			String type = slot.getArmorItem().getItemType().getDescription();
			Map<Integer, DressMeArmorData> map = DressMeHandler.initArmorMap(type, new HashMap<Integer, DressMeArmorData>(), slot);
			if (map == null)
			{
				player.sendMessage("Dress me system: Armor Map is null");
				_log.error("Dress me system: Armor Map is null.");
				return false;
			}
			for (int i = (page - 1) * perpage; i < map.size(); i++)
			{
				DressMeArmorData dress = map.get(i + 1);
				if (dress != null)
				{
					block = template;
					String dress_name = dress.getName();
					if (dress_name.length() > 29)
					{
						dress_name = dress_name.substring(0, 29) + "...";
					}
					block = block.replace("{bypass}", "bypass voice .dress-armorpage " + dress.getId());
					block = block.replace("{name}", dress_name);
					block = block.replace("{price}", Util.formatPay(player, dress.getPriceCount(), dress.getPriceId()));
					block = block.replace("{minTier}", Float.toString(dress.getRequiredTier()));
					block = block.replace("{icon}", Util.getItemIcon(dress.getChest()));
					block = block.replace("{try}", "bypass voice .dress-armortry " + dress.getId());
					list += block;
				}
				counter++;
				if (counter >= perpage)
				{
					break;
				}
			}
			double count = Math.ceil((double) map.size() / perpage);
			int inline = 1;
			String navigation = "";
			for (int i = 1; i <= count; i++)
			{
				if (i == page)
				{
					navigation += "<td width=25 align=center valign=top><button value=\"[" + i + "]\" action=\"bypass -h voice .dressme-armor " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
				}
				else
				{
					navigation += "<td width=25 align=center valign=top><button value=\"" + i + "\" action=\"bypass -h voice .dressme-armor " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
				}
				if (inline == 7)
				{
					navigation += "</tr><tr>";
					inline = 0;
				}
				inline++;
			}
			if (navigation.equals(""))
			{
				navigation = "<td width=30 align=center valign=top>...</td>";
			}
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", navigation);
			NpcHtmlMessage msg = new NpcHtmlMessage(1);
			msg.setHtml(html);
			player.sendPacket(msg);
			return true;
		}
		else if (command.equals("dressme-cloak"))
		{
			String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), index_cloak);
			String template = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), template_cloak_path);
			String block = "";
			String list = "";
			if (args == null)
			{
				args = "1";
			}
			String[] param = args.split(" ");
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 5;
			int counter = 0;
			for (int i = (page - 1) * perpage; i < DressMeCloakHolder.getInstance().size(); i++)
			{
				DressMeCloakData cloak = DressMeCloakHolder.getInstance().getCloak(i + 1);
				if (cloak != null)
				{
					block = template;
					String cloak_name = cloak.getName();
					if (cloak_name.length() > 29)
					{
						cloak_name = cloak_name.substring(0, 29) + "...";
					}
					block = block.replace("{bypass}", "bypass -h voice .dress-cloakpage " + (i + 1));
					block = block.replace("{name}", cloak_name);
					block = block.replace("{price}", Util.formatPay(player, cloak.getPriceCount(), cloak.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(cloak.getCloakId()));
					block = block.replace("{try}", "bypass voice .dress-cloaktry " + cloak.getCloakId());
					list += block;
				}
				counter++;
				if (counter >= perpage)
				{
					break;
				}
			}
			double count = Math.ceil((double) DressMeCloakHolder.getInstance().size() / perpage);
			int inline = 1;
			String navigation = "";
			for (int i = 1; i <= count; i++)
			{
				if (i == page)
				{
					navigation += "<td width=25 align=center valign=top><button value=\"[" + i + "]\" action=\"bypass -h voice .dressme-cloak " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
				}
				else
				{
					navigation += "<td width=25 align=center valign=top><button value=\"" + i + "\" action=\"bypass -h voice .dressme-cloak " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
				}
				if (inline == 7)
				{
					navigation += "</tr><tr>";
					inline = 0;
				}
				inline++;
			}
			if (navigation.equals(""))
			{
				navigation = "<td width=30 align=center valign=top>...</td>";
			}
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", navigation);
			sendHtml(player, html);
			return true;
		}
		else if (command.equals("dressme-shield"))
		{
			String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), index_shield_path);
			String template = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), template_shield_path);
			String block = "";
			String list = "";
			if (args == null)
			{
				args = "1";
			}
			String[] param = args.split(" ");
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 5;
			int counter = 0;
			for (int i = (page - 1) * perpage; i < DressMeShieldHolder.getInstance().size(); i++)
			{
				DressMeShieldData shield = DressMeShieldHolder.getInstance().getShield(i + 1);
				if (shield != null)
				{
					block = template;
					String shield_name = shield.getName();
					if (shield_name.length() > 29)
					{
						shield_name = shield_name.substring(0, 29) + "...";
					}
					block = block.replace("{bypass}", "bypass -h voice .dress-shieldpage " + (i + 1));
					block = block.replace("{name}", shield_name);
					block = block.replace("{price}", Util.formatPay(player, shield.getPriceCount(), shield.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(shield.getShieldId()));
					block = block.replace("{minTier}", Float.toString(shield.getRequiredTier()));
					block = block.replace("{try}", "bypass voice .dress-shieldtry " + shield.getShieldId());
					list += block;
				}
				counter++;
				if (counter >= perpage)
				{
					break;
				}
			}
			double count = Math.ceil((double) DressMeShieldHolder.getInstance().size() / perpage);
			int inline = 1;
			String navigation = "";
			for (int i = 1; i <= count; i++)
			{
				if (i == page)
				{
					navigation += "<td width=25 align=center valign=top><button value=\"[" + i + "]\" action=\"bypass -h voice .dressme-shield " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
				}
				else
				{
					navigation += "<td width=25 align=center valign=top><button value=\"" + i + "\" action=\"bypass -h voice .dressme-shield " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
				}
				if (inline == 7)
				{
					navigation += "</tr><tr>";
					inline = 0;
				}
				inline++;
			}
			if (navigation.equals(""))
			{
				navigation = "<td width=30 align=center valign=top>...</td>";
			}
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", navigation);
			sendHtml(player, html);
			return true;
		}
		else if (command.equals("dressme-weapon"))
		{
			L2ItemInstance slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (slot == null)
			{
				player.sendMessage("Error: Weapon must be equiped!");
				return false;
			}
			String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), index_weapon_path);
			String template = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), template_weapon_path);
			String block = "";
			String list = "";
			if (args == null)
			{
				args = "1";
			}
			String[] param = args.split(" ");
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 5;
			int counter = 0;
			String type = slot.getWeaponItem().getItemType().name();
			Map<Integer, DressMeWeaponData> map = DressMeHandler.initWeaponMap(type, new HashMap<Integer, DressMeWeaponData>(), slot);
			if (map == null)
			{
				_log.error("Dress me system: Weapon Map is null.");
				return false;
			}
			for (int i = (page - 1) * perpage; i < map.size(); i++)
			{
				DressMeWeaponData weapon = map.get(i + 1);
				if (weapon != null)
				{
					block = template;
					String cloak_name = weapon.getName();
					if (cloak_name.length() > 29)
					{
						cloak_name = cloak_name.substring(0, 29) + "...";
					}
					block = block.replace("{bypass}", "bypass voice .dress-weaponpage " + weapon.getId());
					block = block.replace("{name}", cloak_name);
					block = block.replace("{price}", Util.formatPay(player, weapon.getPriceCount(), weapon.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(weapon.getId()));
					block = block.replace("{minTier}", Float.toString(weapon.getRequiredTier()));
					block = block.replace("{try}", "bypass voice .dress-weapontry " + weapon.getId());
					list += block;
				}
				counter++;
				if (counter >= perpage)
				{
					break;
				}
			}
			double count = Math.ceil((double) map.size() / perpage);
			int inline = 1;
			String navigation = "";
			for (int i = 1; i <= count; i++)
			{
				if (i == page)
				{
					navigation += "<td width=25 align=center valign=top><button value=\"[" + i + "]\" action=\"bypass -h voice .dressme-weapon " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
				}
				else
				{
					navigation += "<td width=25 align=center valign=top><button value=\"" + i + "\" action=\"bypass -h voice .dressme-weapon " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
				}
				if (inline == 7)
				{
					navigation += "</tr><tr>";
					inline = 0;
				}
				inline++;
			}
			if (navigation.equals(""))
			{
				navigation = "<td width=30 align=center valign=top>...</td>";
			}
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", navigation);
			sendHtml(player, html);
			return true;
		}
		else if (command.equals("dressme-hat"))
		{
			L2ItemInstance slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
			if (slot == null)
			{
				slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
			}
			if (slot == null)
			{
				player.sendMessage("Error: Hat must be equiped!");
				return false;
			}
			String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), index_hat_path);
			String template = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), template_hat_path);
			String block = "";
			String list = "";
			if (args == null)
			{
				args = "1";
			}
			String[] param = args.split(" ");
			final int page = param[0].length() > 0 ? Integer.parseInt(param[0]) : 1;
			final int perpage = 5;
			int counter = 0;
			Map<Integer, DressMeHatData> map = DressMeHandler.initHatMap(list, new FastMap<Integer, DressMeHatData>(), slot);
			if (map == null || map.isEmpty())
			{
				player.sendMessage("Error: Left slot Hat must be equiped!");
				// _log.error("Dress me system: Hat Map is null.");
				return false;
			}
			for (int i = (page - 1) * perpage; i < map.size(); i++)
			{
				DressMeHatData hat = map.get(i + 1);
				if (hat != null)
				{
					block = template;
					String hat_name = hat.getName();
					if (hat_name.length() > 29)
					{
						hat_name = hat_name.substring(0, 29) + "...";
					}
					block = block.replace("{bypass}", "bypass voice .dress-hatpage " + hat.getId());
					block = block.replace("{name}", hat_name);
					block = block.replace("{price}", Util.formatPay(player, hat.getPriceCount(), hat.getPriceId()));
					block = block.replace("{icon}", Util.getItemIcon(hat.getHatId()));
					block = block.replace("{try}", "bypass voice .dress-hattry " + hat.getId());
					list += block;
				}
				counter++;
				if (counter >= perpage)
				{
					break;
				}
			}
			double count = Math.ceil((double) map.size() / perpage);
			int inline = 1;
			String navigation = "";
			for (int i = 1; i <= count; i++)
			{
				if (i == page)
				{
					navigation += "<td width=25 align=center valign=top><button value=\"[" + i + "]\" action=\"bypass -h voice .dressme-hat " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
				}
				else
				{
					navigation += "<td width=25 align=center valign=top><button value=\"" + i + "\" action=\"bypass -h voice .dressme-hat " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
				}
				if (inline == 7)
				{
					navigation += "</tr><tr>";
					inline = 0;
				}
				inline++;
			}
			if (navigation.equals(""))
			{
				navigation = "<td width=30 align=center valign=top>...</td></tr>";
			}
			html = html.replace("{list}", list);
			html = html.replace("{navigation}", navigation);
			sendHtml(player, html);
			return true;
		}
		else if (command.equals("dress-armorpage"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeArmorData dress = DressMeArmorHolder.getInstance().getArmor(set);
			if (dress != null)
			{
				String limitedTime = "";
				String limitedTime2 = "";
				String limitedTime3 = "";
				String br = "";
				String br2 = "";
				String br3 = "";
				if (dress.getLimitedTimePriceId() != 0)
				{
					br = "<br1>";
					limitedTime = "<button action=\" " + "bypass -h voice .dress-armor-limitedTime " + dress.getId() + " 1\" value=\"Change visual to " + dress.getName() + " for " + dress.getLimitedTimeHours() + " hour(s) \" width=282 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
				}
				if (dress.getLimitedTimePriceId2() != 0)
				{
					br2 = "<br1>";
					limitedTime2 = "<button action=\" " + "bypass -h voice .dress-armor-limitedTime " + dress.getId() + " 2\" value=\"Change visual to " + dress.getName() + " for " + dress.getLimitedTimeHours2() + " hour(s) \" width=282 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
				}
				if (dress.getLimitedTimePriceId3() != 0)
				{
					br3 = "<br1>";
					limitedTime3 = "<button action=\" " + "bypass -h voice .dress-armor-limitedTime " + dress.getId() + " 3\" value=\"Change visual to " + dress.getName() + " for " + dress.getLimitedTimeHours3() + " hour(s) \" width=282 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
				}
				String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), dress_armor_path);
				Inventory inv = player.getInventory();
				L2ItemInstance my_chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
				html = html.replace("{my_chest_icon}", my_chest == null ? "icon.NOIMAGE" : my_chest.getItem().getIcon());
				L2ItemInstance my_legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
				html = html.replace("{my_legs_icon}", my_legs == null ? "icon.NOIMAGE" : my_legs.getItem().getIcon());
				L2ItemInstance my_gloves = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
				html = html.replace("{my_gloves_icon}", my_gloves == null ? "icon.NOIMAGE" : my_gloves.getItem().getIcon());
				L2ItemInstance my_feet = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
				html = html.replace("{my_feet_icon}", my_feet == null ? "icon.NOIMAGE" : my_feet.getItem().getIcon());
				html = html.replace("{bypass}", "bypass voice .dress-armor " + set);
				html = html.replace("{name}", dress.getName());
				html = html.replace("{price}", Util.formatPay(player, dress.getPriceCount(), dress.getPriceId()));
				html = html.replace("{timePrice}", Util.formatPay(player, dress.getLimitedTimePriceCount(), dress.getLimitedTimePriceId()));
				html = html.replace("{minTier}", Float.toString(dress.getRequiredTier()));
				html = html.replace("%limited%", limitedTime);
				html = html.replace("%limited2%", limitedTime2);
				html = html.replace("%limited3%", limitedTime3);
				html = html.replace("%br%", br);
				html = html.replace("%br2%", br2);
				html = html.replace("%br3%", br3);
				L2Item chest = ItemTable.getInstance().getTemplate(dress.getChest());
				html = html.replace("{chest_icon}", chest.getIcon());
				html = html.replace("{chest_name}", chest.getName());
				// html = html.replace("{chest_grade}", chest.getItemGrade().name());
				if (dress.getLegs() != -1)
				{
					L2Item legs = ItemTable.getInstance().getTemplate(dress.getLegs());
					html = html.replace("{legs_icon}", legs.getIcon());
					html = html.replace("{legs_name}", legs.getName());
					// html = html.replace("{legs_grade}", legs.getItemGrade().name());
				}
				else
				{
					html = html.replace("{legs_icon}", chest.getIcon());
					html = html.replace("{legs_name}", chest.getName());
					html = html.replace("{legs_grade}", "NO");
				}
				if (dress.getGloves() != -1)
				{
					L2Item gloves = ItemTable.getInstance().getTemplate(dress.getGloves());
					html = html.replace("{gloves_icon}", gloves.getIcon());
					html = html.replace("{gloves_name}", gloves.getName());
					// html = html.replace("{gloves_grade}", gloves.getItemGrade().name());
				}
				else
				{
					html = html.replace("{gloves_icon}", chest.getIcon());
					html = html.replace("{gloves_name}", chest.getName());
				}
				if (dress.getFeet() != -1)
				{
					L2Item feet = ItemTable.getInstance().getTemplate(dress.getFeet());
					html = html.replace("{feet_icon}", feet.getIcon());
					html = html.replace("{feet_name}", feet.getName());
					// html = html.replace("{feet_grade}", feet.getItemGrade().name());
				}
				else
				{
					html = html.replace("{feet_icon}", chest.getIcon());
					html = html.replace("{feet_name}", chest.getName());
				}
				sendHtml(player, html);
				return true;
			}
			return false;
		}
		else if (command.equals("dress-cloakpage"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeCloakData cloak = DressMeCloakHolder.getInstance().getCloak(set);
			if (cloak != null)
			{
				String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), dress_cloak_path);
				Inventory inv = player.getInventory();
				L2ItemInstance my_cloak = inv.getPaperdollItem(Inventory.PAPERDOLL_BACK);
				html = html.replace("{my_cloak_icon}", my_cloak == null ? "icon.NOIMAGE" : my_cloak.getItem().getIcon());
				html = html.replace("{bypass}", "bypass -h voice .dress-cloak " + cloak.getId());
				html = html.replace("{name}", cloak.getName());
				html = html.replace("{price}", Util.formatPay(player, cloak.getPriceCount(), cloak.getPriceId()));
				L2Item item = ItemTable.getInstance().getTemplate(cloak.getCloakId());
				html = html.replace("{item_icon}", item.getIcon());
				html = html.replace("{item_name}", item.getName());
				// html = html.replace("{item_grade}", item.getItemGrade().name());
				sendHtml(player, html);
				return true;
			}
			return false;
		}
		else if (command.equals("dress-shieldpage"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeShieldData shield = DressMeShieldHolder.getInstance().getShield(set);
			if (shield != null)
			{
				String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), dress_shield_path);
				Inventory inv = player.getInventory();
				L2ItemInstance my_shield = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
				html = html.replace("{my_shield_icon}", my_shield == null ? "icon.NOIMAGE" : my_shield.getItem().getIcon());
				html = html.replace("{bypass}", "bypass -h voice .dress-shield " + shield.getId());
				html = html.replace("{name}", shield.getName());
				html = html.replace("{price}", Util.formatPay(player, shield.getPriceCount(), shield.getPriceId()));
				L2Item item = ItemTable.getInstance().getTemplate(shield.getShieldId());
				html = html.replace("{item_icon}", item.getIcon());
				html = html.replace("{item_name}", item.getName());
				// html = html.replace("{item_grade}", item.getItemGrade().name());
				sendHtml(player, html);
				return true;
			}
			return false;
		}
		else if (command.equals("dress-weaponpage"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeWeaponData weapon = DressMeWeaponHolder.getInstance().getWeapon(set);
			if (weapon != null)
			{
				String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), dress_weapon_path);
				Inventory inv = player.getInventory();
				L2ItemInstance my_weapon = inv.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				html = html.replace("{my_weapon_icon}", my_weapon == null ? "icon.NOIMAGE" : my_weapon.getItem().getIcon());
				html = html.replace("{bypass}", "bypass voice .dress-weapon " + weapon.getId());
				html = html.replace("{name}", weapon.getName());
				html = html.replace("{price}", Util.formatPay(player, weapon.getPriceCount(), weapon.getPriceId()));
				L2Item item = ItemTable.getInstance().getTemplate(weapon.getId());
				html = html.replace("{item_icon}", item.getIcon());
				html = html.replace("{item_name}", item.getName());
				// html = html.replace("{item_grade}", item.getItemGrade().name());
				sendHtml(player, html);
				return true;
			}
			return false;
		}
		else if (command.equals("dress-hatpage"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeHatData hat = DressMeHatHolder.getInstance().getHat(set);
			if (hat != null)
			{
				String limitedTime = "";
				String limitedTime2 = "";
				String limitedTime3 = "";
				String br = "";
				String br2 = "";
				String br3 = "";
				if (hat.getLimitedTimePriceId() != 0)
				{
					br = "<br1>";
					limitedTime = "<button action=\" " + "bypass -h voice .dress-hat-limitedTime " + hat.getId() + " 1\" value=\"Change visual to " + hat.getName() + " for " + hat.getLimitedTimeHours() + " hour(s) \" width=282 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
				}
				if (hat.getLimitedTimePriceId2() != 0)
				{
					br2 = "<br1>";
					limitedTime2 = "<button action=\" " + "bypass -h voice .dress-hat-limitedTime " + hat.getId() + " 2\" value=\"Change visual to " + hat.getName() + " for " + hat.getLimitedTimeHours2() + " hour(s) \" width=282 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
				}
				if (hat.getLimitedTimePriceId3() != 0)
				{
					br3 = "<br1>";
					limitedTime3 = "<button action=\" " + "bypass -h voice .dress-hat-limitedTime " + hat.getId() + " 3\" value=\"Change visual to " + hat.getName() + " for " + hat.getLimitedTimeHours3() + " hour(s) \" width=282 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
				}
				
				String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), dress_hat_path);
				Inventory inv = player.getInventory();
				L2ItemInstance my_hat = null;
				switch (hat.getSlot())
				{
					case 1: // HAIR
					case 3: // FULL HAIR
						my_hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR);
						break;
					case 2: // HAIR2
						my_hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
						break;
				}
				html = html.replace("{my_hat_icon}", my_hat == null ? "icon.NOIMAGE" : my_hat.getItem().getIcon());
				html = html.replace("{bypass}", "bypass voice .dress-hat " + hat.getId());
				html = html.replace("{name}", hat.getName());
				html = html.replace("{price}", Util.formatPay(player, hat.getPriceCount(), hat.getPriceId()));
				L2Item item = ItemTable.getInstance().getTemplate(hat.getHatId());
				html = html.replace("{item_icon}", item.getIcon());
				html = html.replace("{item_name}", item.getName());
				html = html.replace("%limited%", limitedTime);
				html = html.replace("%limited2%", limitedTime2);
				html = html.replace("%limited3%", limitedTime3);
				html = html.replace("%br%", br);
				html = html.replace("%br2%", br2);
				html = html.replace("%br3%", br3);
				sendHtml(player, html);
				return true;
			}
			return false;
		}
		else if (command.equals("dressinfo"))
		{
			String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), info_path);
			sendHtml(player, html);
			return true;
		}
		else if (command.equals("dress-armor"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeArmorData dress = DressMeArmorHolder.getInstance().getArmor(set);
			Inventory inv = player.getInventory();
			L2ItemInstance chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			L2ItemInstance legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			L2ItemInstance gloves = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
			L2ItemInstance feet = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
			if (chest == null)
			{
				player.sendMessage("Error: Chest must be equiped.");
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			L2ArmorSet armoSet = ArmorSetsTable.getInstance().getSet(chest.getItemId());
			if ((armoSet == null) || !armoSet.containAll(player))
			{
				player.sendMessage("Error: You can't visualize, set is not complete.");
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			if (!Conditions.checkPlayerArmorSet(player, dress.getRequiredArmorSet()))
			{
				return false;
			}
			if (Conditions.checkPlayerItemCount(player, dress.getPriceId(), dress.getPriceCount()))
			{
				boolean destroyItem = false;
				if (destroyItem)
				{
					player.destroyItemByItemId("VisualChange", dress.getPriceId(), dress.getPriceCount(), player, true);
				}
				DressMeHandler.visuality(player, chest, dress.getChest(), 0);
				if (dress.getLegs() != -1 && chest.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR)
				{
					DressMeHandler.visuality(player, legs, dress.getLegs(), 0);
				}
				else if (dress.getLegs() == -1 && !(chest.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR))
				{
					DressMeHandler.visuality(player, legs, dress.getChest(), 0);
				}
				if (dress.getLegs() != -1)
				{
					DressMeHandler.visuality(player, legs, dress.getLegs(), 0);
				}
				if (dress.getGloves() != -1)
				{
					DressMeHandler.visuality(player, gloves, dress.getGloves(), 0);
				}
				else if (dress.getGloves() == -1)
				{
					DressMeHandler.visuality(player, gloves, dress.getChest(), 0);
				}
				if (dress.getFeet() != -1)
				{
					DressMeHandler.visuality(player, feet, dress.getFeet(), 0);
				}
				else if (dress.getFeet() == -1)
				{
					DressMeHandler.visuality(player, feet, dress.getChest(), 0);
				}
				// DressMeHandler.visuality(player, gloves, dress.getGloves());
				// DressMeHandler.visuality(player, feet, dress.getFeet());
				player.broadcastUserInfo();
			}
			useVoicedCommand("dressme", player, null);
			return true;
		}
		else if (command.equals("dress-armor-limitedTime"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			final int timeChose = Integer.parseInt(args.split(" ")[1]);
			DressMeArmorData dress = DressMeArmorHolder.getInstance().getArmor(set);
			Inventory inv = player.getInventory();
			L2ItemInstance chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			L2ItemInstance legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			L2ItemInstance gloves = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
			L2ItemInstance feet = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
			int limitedPriceId = 0;
			int limitedPriceCount = 0;
			int limitedHours = 0;
			if (timeChose != 0)
			{
				switch (timeChose)
				{
					case 1:
						limitedPriceId = dress.getLimitedTimePriceId();
						limitedPriceCount = dress.getLimitedTimePriceCount();
						limitedHours = dress.getLimitedTimeHours();
						break;
					case 2:
						limitedPriceId = dress.getLimitedTimePriceId2();
						limitedPriceCount = dress.getLimitedTimePriceCount2();
						limitedHours = dress.getLimitedTimeHours2();
						break;
					case 3:
						limitedPriceId = dress.getLimitedTimePriceId3();
						limitedPriceCount = dress.getLimitedTimePriceCount3();
						limitedHours = dress.getLimitedTimeHours3();
						break;
					default:
						break;
				}
			}
			if (chest == null)
			{
				player.sendMessage("Error: Chest must be equiped.");
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			L2ArmorSet armoSet = ArmorSetsTable.getInstance().getSet(chest.getItemId());
			if ((armoSet == null) || !armoSet.containAll(player))
			{
				player.sendMessage("Error: You can't visualize, set is not complete.");
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			if (!Conditions.checkPlayerArmorSet(player, dress.getRequiredArmorSet()))
			{
				return false;
			}
			if (Conditions.checkPlayerItemCount(player, limitedPriceId, limitedPriceCount))
			{
				if (limitedPriceId != 0 && limitedPriceCount != 0)
				{
					player.destroyItemByItemId("VisualChange", limitedPriceId, limitedPriceCount, player, true);
				}
				final long time = System.currentTimeMillis() + (limitedHours * 60 * 60 * 1000);
				DressMeHandler.visuality(player, chest, dress.getChest(), time);
				if (dress.getLegs() != -1 && chest.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR)
				{
					DressMeHandler.visuality(player, legs, dress.getLegs(), time);
				}
				else if (dress.getLegs() == -1 && !(chest.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR))
				{
					DressMeHandler.visuality(player, legs, dress.getChest(), time);
				}
				if (dress.getLegs() != -1)
				{
					DressMeHandler.visuality(player, legs, dress.getLegs(), time);
				}
				if (dress.getGloves() != -1)
				{
					DressMeHandler.visuality(player, gloves, dress.getGloves(), time);
				}
				else if (dress.getGloves() == -1)
				{
					DressMeHandler.visuality(player, gloves, dress.getChest(), time);
				}
				if (dress.getFeet() != -1)
				{
					DressMeHandler.visuality(player, feet, dress.getFeet(), time);
				}
				else if (dress.getFeet() == -1)
				{
					DressMeHandler.visuality(player, feet, dress.getChest(), time);
				}
				String finalDays = "" + limitedHours + " hours.";
				if (limitedHours >= 24)
				{
					int days = limitedHours / 24;
					String daysStr = String.valueOf(days);
					finalDays = daysStr + " days.";
				}
				player.sendMessage("Activated: " + dress.getName() + " for " + finalDays);
				player.broadcastUserInfo();
			}
			useVoicedCommand("dressme", player, null);
			return true;
		}
		else if (command.equals("dress-armortry"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeArmorData dress = DressMeArmorHolder.getInstance().getArmor(set);
			Inventory inv = player.getInventory();
			L2ItemInstance chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			L2ItemInstance legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			L2ItemInstance gloves = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
			L2ItemInstance feet = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
			if (chest == null)
			{
				player.sendMessage("Error: Chest must be equiped.");
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			L2ArmorSet armoSet = ArmorSetsTable.getInstance().getSet(chest.getItemId());
			if ((armoSet == null) || !armoSet.containAll(player))
			{
				player.sendMessage("Error: You can't visualize, set is not complete.");
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
			if (chest.getTryingItemId() > 0)
			{
				player.sendMessage("You are already trying a hat.");
			}
			else
			{
				DressMeHandler.tryingItem(player, chest, dress.getChest());
				if (dress.getLegs() != -1 && chest.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR)
				{
					DressMeHandler.tryingItem(player, legs, dress.getLegs());
				}
				else if (dress.getLegs() == -1 && !(chest.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR))
				{
					DressMeHandler.tryingItem(player, legs, dress.getChest());
				}
				if (dress.getLegs() != -1)
				{
					DressMeHandler.tryingItem(player, legs, dress.getLegs());
				}
				if (dress.getGloves() != -1)
				{
					DressMeHandler.tryingItem(player, gloves, dress.getGloves());
				}
				else if (dress.getGloves() == -1)
				{
					DressMeHandler.tryingItem(player, gloves, dress.getChest());
				}
				if (dress.getFeet() != -1)
				{
					DressMeHandler.tryingItem(player, feet, dress.getFeet());
				}
				else if (dress.getFeet() == -1)
				{
					DressMeHandler.tryingItem(player, feet, dress.getChest());
				}
			}
			//
			//useVoicedCommand("dressme-armor", player, null);
			return true;
		}
		else if (command.equals("dress-cloak"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeCloakData cloak_data = DressMeCloakHolder.getInstance().getCloak(set);
			Inventory inv = player.getInventory();
			L2ItemInstance cloak = inv.getPaperdollItem(Inventory.PAPERDOLL_BACK);
			if (cloak == null)
			{
				player.sendMessage("Error: Cloak must be equiped.");
				useVoicedCommand("dress-cloakpage", player, args);
				return false;
			}
			if (!Conditions.checkCloakRequiredId(player, cloak_data.get_requiredId()))
			{
				player.sendMessage("This item doesn't match with the item you wanna dress.");
				return false;
			}
			if (!cloak.isDread())
			{
				player.sendMessage("Only Dread Shield can be dressed.");
				return false;
			}
			// if (Conditions.checkPlayerItemCount(player, cloak_data.getPriceId(), cloak_data.getPriceCount()))
			// {
			// //player.destroyItemByItemId("VisualChange", cloak_data.getPriceId(), cloak_data.getPriceCount(), player, true);
			// DressMeHandler.visuality(player, cloak, cloak_data.getCloakId());
			// }
			DressMeHandler.visuality(player, cloak, cloak_data.getCloakId(), 0);
			player.broadcastUserInfo();
			useVoicedCommand("dressme", player, null);
			return true;
		}
		else if (command.equals("dress-shield"))
		{
			final int shield_id = Integer.parseInt(args.split(" ")[0]);
			DressMeShieldData shield_data = DressMeShieldHolder.getInstance().getShield(shield_id);
			Inventory inv = player.getInventory();
			L2ItemInstance shield = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if (shield == null)
			{
				player.sendMessage("Error: Shield must be equiped.");
				useVoicedCommand("dress-shieldpage", player, args);
				return false;
			}
			if (!Conditions.checkShieldRequiredId(player, shield_data.get_requiredShieldId()))
			{
				player.sendMessage("This item doesn't match with the item you wanna dress.");
				return false;
			}
			if (!shield.isDread())
			{
				player.sendMessage("Only Dread Shield can be dressed.");
				return false;
			}
			if (Conditions.checkPlayerItemCount(player, shield_data.getPriceId(), shield_data.getPriceCount()))
			{
				// player.destroyItemByItemId("VisualChange", shield_data.getPriceId(), shield_data.getPriceCount(), player, true);
				DressMeHandler.visuality(player, shield, shield_data.getShieldId(), 0);
			}
			player.broadcastUserInfo();
			useVoicedCommand("dressme", player, null);
			return true;
		}
		else if (command.equals("dress-shieldtry"))
		{
			final int shield_id = Integer.parseInt(args.split(" ")[0]);
			DressMeShieldData shield_data = DressMeShieldHolder.getInstance().getShield(shield_id);
			Inventory inv = player.getInventory();
			L2ItemInstance shield = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if (shield == null)
			{
				player.sendMessage("Error: Shield must be equiped.");
				useVoicedCommand("dress-shieldpage", player, args);
				return false;
			}
			else
			{
				DressMeHandler.tryingItem(player, shield, shield_data.getShieldId());
				player.broadcastUserInfo();
			}
			useVoicedCommand("dressme-shield", player, null);
			return true;
		}
		else if (command.equals("dress-weapon"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeWeaponData weapon_data = DressMeWeaponHolder.getInstance().getWeapon(set);
			Inventory inv = player.getInventory();
			L2ItemInstance weapon = inv.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (weapon == null)
			{
				player.sendMessage("Error: Weapon must be equiped.");
				useVoicedCommand("dress-weaponpage", player, args);
				return false;
			}
			if (!weapon.getItemType().toString().equals(weapon_data.getType()))
			{
				player.sendMessage("Error: Weapon must be equals type. " + weapon.getItemType().toString() + " = " + weapon_data.getType());
				useVoicedCommand("dressme-weapon", player, args);
				return false;
			}
//			if (!weapon.isDread())
//			{
//				player.sendMessage("Only Dread Weapons can be dressed");
//				return false;
//			}
			if (!Conditions.checkWepRequiredId(player, weapon_data.get_requiredWepId()))
			{
				player.sendMessage("This item doesn't match with the item you wanna dress.");
				return false;
			}
			if (Conditions.checkPlayerItemCount(player, weapon_data.getPriceId(), weapon_data.getPriceCount()))
			{
				// player.destroyItemByItemId("VisualChange", weapon_data.getPriceId(), weapon_data.getPriceCount(), player, true);
				DressMeHandler.visuality(player, weapon, weapon_data.getId(), 0);
			}
			player.broadcastUserInfo();
			useVoicedCommand("dressme", player, null);
			return true;
		}
		else if (command.equals("dress-weapontry"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeWeaponData weapon_data = DressMeWeaponHolder.getInstance().getWeapon(set);
			int id = weapon_data.getId();
			Inventory inv = player.getInventory();
			L2ItemInstance weapon = inv.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (weapon == null)
			{
				player.sendMessage("Error: Weapon must be equiped.");
				useVoicedCommand("dress-weaponpage", player, args);
				return false;
			}
			if (!weapon.getItemType().toString().equals(weapon_data.getType()))
			{
				player.sendMessage("Error: Weapon must be equals type. " + weapon.getItemType().toString() + " = " + weapon_data.getType());
				useVoicedCommand("dressme-weapon", player, args);
				return false;
			}
			if (weapon.getTryingItemId() > 0)
			{
				player.sendMessage("You are already trying a weapon.");
			}
			else
			{
				DressMeHandler.tryingItem(player, weapon, id);
				player.broadcastUserInfo();
			}
			useVoicedCommand("dressme-weapon", player, null);
			return true;
		}
		else if (command.equals("dress-cloaktry"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeCloakData cloak_data = DressMeCloakHolder.getInstance().getCloakTry(set);
			Inventory inv = player.getInventory();
			L2ItemInstance cloak = inv.getPaperdollItem(Inventory.PAPERDOLL_BACK);
			if (cloak == null)
			{
				player.sendMessage("Error: Cloak must be equiped.");
				useVoicedCommand("dress-cloakpage", player, args);
				return false;
			}
			else
			{
				DressMeHandler.tryingItem(player, cloak, cloak_data.getCloakId());
				player.broadcastUserInfo();
			}
			useVoicedCommand("dressme-cloak", player, null);
			return true;
		}
		else if (command.equals("dress-hat"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeHatData hat_data = DressMeHatHolder.getInstance().getHat(set);
			Inventory inv = player.getInventory();
			L2ItemInstance hat = null;
			switch (hat_data.getSlot())
			{
				case 1: // HAIR
					hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR);
					break;
				case 2: // HAIR2
					hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
					break;
				case 3: // FULL HAIR
					hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIRALL);
					break;
			}
			if (hat == null)
			{
				player.sendMessage("Error: Hat must be equiped.");
				useVoicedCommand("dress-hatpage", player, args);
				return false;
			}
			// L2Item visual = ItemTable.getInstance().getTemplate(hat_data.getHatId());
			// if (hat.getItem().getBodyPart() != visual.getBodyPart())
			// {
			// player.sendMessage("Error: You can't change visual on different hat types!");
			// useVoicedCommand("dress-hatpage", player, args);
			// return false;
			// }
			if (Conditions.checkPlayerItemCount(player, hat_data.getPriceId(), hat_data.getPriceCount()))
			{
				boolean destroyItem = false;
				if (hat_data.getPriceId() == 327290 || hat_data.getPriceId() == 327300 || hat_data.getPriceId() == 327310 || hat_data.getPriceId() == 327320 || hat_data.getPriceId() == 327340 || hat_data.getPriceId() == 327350 || hat_data.getPriceId() == 327350 || hat_data.getPriceId() == 327360 || hat_data.getPriceId() == 327400 || hat_data.getPriceId() == 327420 || hat_data.getPriceId() == 318540 || hat_data.getPriceId() == 312500 || hat_data.getPriceId() == 326030 || hat_data.getPriceId() == 326050 || hat_data.getPriceId() == 326070 || hat_data.getPriceId() == 326090 || hat_data.getPriceId() == 326110 || hat_data.getPriceId() == 326130 || hat_data.getPriceId() == 329110 || hat_data.getPriceId() == 327330 || hat_data.getPriceId() == 326010)
				{
					destroyItem = false;
				}
				if (destroyItem)
				{
					player.destroyItemByItemId("VisualChange", hat_data.getPriceId(), hat_data.getPriceCount(), player, true);
				}
				DressMeHandler.visuality(player, hat, hat_data.getHatId(), 0);
			}
			player.broadcastUserInfo();
			useVoicedCommand("dressme", player, null);
			return true;
		}
		else if (command.equals("dress-hat-limitedTime"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			final int timeChose = Integer.parseInt(args.split(" ")[1]);
			DressMeHatData hat_data = DressMeHatHolder.getInstance().getHat(set);
			Inventory inv = player.getInventory();
			L2ItemInstance hat = null;
			int limitedPriceId = 0;
			int limitedPriceCount = 0;
			int limitedHours = 0;
			if (timeChose != 0)
			{
				switch (timeChose)
				{
					case 1:
						limitedPriceId = hat_data.getLimitedTimePriceId();
						limitedPriceCount = hat_data.getLimitedTimePriceCount();
						limitedHours = hat_data.getLimitedTimeHours();
						break;
					case 2:
						limitedPriceId = hat_data.getLimitedTimePriceId2();
						limitedPriceCount = hat_data.getLimitedTimePriceCount2();
						limitedHours = hat_data.getLimitedTimeHours2();
						break;
					case 3:
						limitedPriceId = hat_data.getLimitedTimePriceId3();
						limitedPriceCount = hat_data.getLimitedTimePriceCount3();
						limitedHours = hat_data.getLimitedTimeHours3();
						break;
					default:
						break;
				}
			}
			switch (hat_data.getSlot())
			{
				case 1: // HAIR
					hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR);
					break;
				case 2: // HAIR2
					hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
					break;
				case 3: // FULL HAIR
					hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIRALL);
					break;
			}
			if (hat == null)
			{
				player.sendMessage("Error: Hat must be equiped.");
				useVoicedCommand("dress-hatpage", player, args);
				return false;
			}
			if (Conditions.checkPlayerItemCount(player, limitedPriceId, limitedPriceCount))
			{
				final long time = System.currentTimeMillis() + (limitedHours * 60 * 60 * 1000);
				player.destroyItemByItemId("VisualChange", limitedPriceId, limitedPriceCount, player, true);
				DressMeHandler.visuality(player, hat, hat_data.getHatId(), time);
			}
			String finalDays = "" + limitedHours + " hours.";
			if (limitedHours >= 24)
			{
				int days = limitedHours / 24;
				String daysStr = String.valueOf(days);
				finalDays = daysStr + " days.";
			}
			player.sendMessage("Activated " + hat_data.getName() + " for " + finalDays);
			player.broadcastUserInfo();
			useVoicedCommand("dressme", player, null);
			return true;
		}
		else if (command.equals("dress-hattry"))
		{
			final int set = Integer.parseInt(args.split(" ")[0]);
			DressMeHatData hat_data = DressMeHatHolder.getInstance().getHat(set);
			Inventory inv = player.getInventory();
			L2ItemInstance hat = null;
			switch (hat_data.getSlot())
			{
				case 1: // HAIR
					hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR);
					break;
				case 2: // HAIR2
					hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
					break;
				case 3: // FULL HAIR
					hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIRALL);
					break;
			}
			if (hat == null)
			{
				player.sendMessage("Error: Hat must be equiped.");
				useVoicedCommand("dress-hatpage", player, args);
				return false;
			}
			if (hat.getTryingItemId() > 0)
			{
				player.sendMessage("You are already trying a hat.");
			}
			else
			{
				DressMeHandler.tryingItem(player, hat, hat_data.getHatId());
				player.broadcastUserInfo();
			}
			useVoicedCommand("dressme-hat", player, null);
			return true;
		}
		else if (command.equals("undressme"))
		{
			String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), undressme_path);
			html = html.replace("<?show_hide?>", !player.getVarB("showVisualChange") ? "Show visual equip on other player!" : "Hide visual equip on other player!");
			html = html.replace("<?show_hide_b?>", !player.getVarB("showVisualChange") ? "showdress" : "hidedress");
			sendHtml(player, html);
			return true;
		}
		else if (command.equals("undressme-armor"))
		{
			Inventory inv = player.getInventory();
			L2ItemInstance chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			L2ItemInstance legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			L2ItemInstance gloves = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
			L2ItemInstance feet = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
			if (chest != null)
			{
				DressMeHandler.visuality(player, chest, 0, 0);
			}
			if (legs != null)
			{
				DressMeHandler.visuality(player, legs, 0, 0);
			}
			if (gloves != null)
			{
				DressMeHandler.visuality(player, gloves, 0, 0);
			}
			if (feet != null)
			{
				DressMeHandler.visuality(player, feet, 0, 0);
			}
			player.broadcastUserInfo();
			useVoicedCommand("undressme", player, null);
			return true;
		}
		else if (command.equals("undressme-cloak"))
		{
			L2ItemInstance cloak = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_BACK);
			if (cloak != null)
			{
				DressMeHandler.visuality(player, cloak, 0, 0);
			}
			player.broadcastUserInfo();
			useVoicedCommand("undressme", player, null);
			return true;
		}
		else if (command.equals("undressme-shield"))
		{
			L2ItemInstance shield = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if (shield != null)
			{
				DressMeHandler.visuality(player, shield, 0, 0);
			}
			player.broadcastUserInfo();
			useVoicedCommand("undressme", player, null);
			return true;
		}
		else if (command.equals("undressme-weapon"))
		{
			L2ItemInstance weapon = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (weapon != null)
			{
				DressMeHandler.visuality(player, weapon, 0, 0);
			}
			player.broadcastUserInfo();
			useVoicedCommand("undressme", player, null);
			return true;
		}
		else if (command.equals("undressme-hat"))
		{
			L2ItemInstance slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
			if (slot == null)
			{
				slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
			}
			if (slot != null)
			{
				DressMeHandler.visuality(player, slot, 0, 0);
			}
			player.broadcastUserInfo();
			useVoicedCommand("undressme", player, null);
			return true;
		}
		else if (command.equals("showdress"))
		{
			player.setVar("showVisualChange", "true");
			player.broadcastUserInfo();
			useVoicedCommand("dressme", player, null);
			return true;
		}
		else if (command.equals("hidedress"))
		{
			player.setVar("showVisualChange", "false");
			player.broadcastUserInfo();
			useVoicedCommand("dressme", player, null);
			return true;
		}
		return false;
	}
	
	private void sendHtml(L2PcInstance player, String html)
	{
		NpcHtmlMessage msg = new NpcHtmlMessage(1);
		msg.setHtml(html);
		player.sendPacket(msg);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _commandList;
	}
}
