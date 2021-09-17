package net.sf.l2j.gameserver.handler.itemhandlers;

import java.io.File;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;

import inertia.controller.InertiaController;
import javolution.text.TextBuilder;
import net.sf.l2j.Base64;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.TradeController;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.handler.UserCommandHandler;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2TradeList;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2ClassMasterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SymbolMakerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TeleporterInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.BuyList;
import net.sf.l2j.gameserver.network.serverpackets.ClassUpgradeWnd;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import net.sf.l2j.gameserver.network.serverpackets.HennaEquipList;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SellList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;

public class Gem implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		60000
	};
	
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		final L2PcInstance activeChar = (L2PcInstance) playable;
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("Cannot use while in Olympiad");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (activeChar.isInJail())
		{
			activeChar.sendMessage("Cannot use while in jail");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		final String filename = "data/html/custom/Gem/menu.htm";
		final String content = HtmCache.getInstance().getHtm(filename);
		if (content == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
			activeChar.sendPacket(html);
		}
		else
		{
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setHtml(content);
			activeChar.sendPacket(itemReply);
		}
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void remoteUse(L2PcInstance activeChar)
	{
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("Cannot use while in Olympiad");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (activeChar.isInJail())
		{
			activeChar.sendMessage("Cannot use while in jail");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		final String filename = "data/html/custom/Gem/menu.htm";
		final String content = HtmCache.getInstance().getHtm(filename);
		if (content == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
			activeChar.sendPacket(html);
		}
		else
		{
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setHtml(content);
			activeChar.sendPacket(itemReply);
		}
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * @see net.sf.l2j..gameserver.handler.IItemHandler#getItemIds()
	 */
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	final public static void onBypass(L2PcInstance player, String action)
	{
		if (player.isInJail())
		{
			player.sendMessage("Cannot use while in jail");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (action.equals("changepass"))
		{
			if (player.isAccountLockedDown())
			{
				player.sendMessage("Your account is in lockdown");
				return;
			}
			if (player.getSecretCode() == null || player.getSecretCode().equalsIgnoreCase("")) // doesn't have a secret code set
			{
				String filename = "data/html/custom/Gem/account/setsecretcode.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "You don't have an account secret code set, you must set it first before you can change your password.");
				player.sendPacket(itemReply);
			}
			else
			// has a secret code set
			{
				String filename = "data/html/custom/Gem/account/passchangemain.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "");
				player.sendPacket(itemReply);
			}
			return;
		}
		else if (action.startsWith("changepass_action "))
		{
			if (player.isAccountLockedDown())
			{
				player.sendMessage("Your account is in lockdown");
				return;
			}
			final String errorMsg = doPasswordChange(player, action);
			if (errorMsg != null)
			{
				String filename = "data/html/custom/Gem/account/passchangemain.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/Gem/account/passchangemain-done.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
			}
			return;
		}
		else if (action.startsWith("setsecret_action "))
		{
			if (player.isAccountLockedDown())
			{
				player.sendMessage("Your account is in lockdown");
				return;
			}
			final String errorMsg = setSecretCode(player, action);
			if (errorMsg != null)
			{
				String filename = "data/html/custom/Gem/account/setsecretcode.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/Gem/account/setsecretcode-done.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
			}
			return;
		}
		else if (action.startsWith("setpincode_action "))
		{
			final String errorMsg = setPinCode(player, action);
			if (errorMsg != null)
			{
				String filename = "data/html/custom/Gem/account/setpincode.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/Gem/account/setsecretcode-done.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
			}
			return;
		}
		else if (action.equals("changepin"))
		{
			if (player.getPinCode() == null || player.getPinCode().equalsIgnoreCase("")) // doesn't have a secret code set
			{
				String filename = "data/html/custom/Gem/account/setpincode.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "You don't have a pin code set to begin with, you can set it here.");
				player.sendPacket(itemReply);
			}
			else
			// has a pin code set
			{
				String filename = "data/html/custom/Gem/account/changepincode.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "");
				player.sendPacket(itemReply);
			}
			return;
		}
		else if (action.startsWith("changepin_action "))
		{
			final String errorMsg = setPinCode(player, action);
			if (errorMsg != null)
			{
				String filename = "data/html/custom/Gem/account/changepincode.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/Gem/account/changesecretcode-done.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
			}
			return;
		}
		else if (action.equals("changesecret"))
		{
			if (player.isAccountLockedDown())
			{
				player.sendMessage("Your account is in lockdown");
				return;
			}
			if (player.getSecretCode() == null || player.getSecretCode().equalsIgnoreCase("")) // doesn't have a secret code set
			{
				String filename = "data/html/custom/Gem/account/setsecretcode.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "You don't have a secret code set to begin with, you can set it here.");
				player.sendPacket(itemReply);
			}
			else
			// has a secret code set
			{
				String filename = "data/html/custom/Gem/account/changesecretcode.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "");
				player.sendPacket(itemReply);
			}
			return;
		}
		else if (action.startsWith("changesecret_action "))
		{
			if (player.isAccountLockedDown())
			{
				player.sendMessage("Your account is in lockdown");
				return;
			}
			final String errorMsg = setSecretCode(player, action);
			if (errorMsg != null)
			{
				String filename = "data/html/custom/Gem/account/changesecretcode.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/Gem/account/changesecretcode-done.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
			}
			return;
		}
		if (player.isInOlympiadMode())
		{
			player.sendMessage("Cannot use while in Olympiad");
			return;
		}
		if (action.equalsIgnoreCase("gemmain"))
		{
			String filename = "data/html/custom/Gem/menu.htm";
			String content = HtmCache.getInstance().getHtm(filename);
			if (content == null)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setHtml(content);
				player.sendPacket(itemReply);
			}
		}
		else if (action.equalsIgnoreCase("upgradeclass"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat/in pvp zone");
				return;
			}
			if (player.isTransformed() || player.isInStance())
			{
				player.sendMessage("Cannot do this while transformed");
				return;
			}
			Gem.sendClassChangeHTML(player);
		}
		else if (action.startsWith("upgradeclasschoose"))
		{
			if (player.isTransformed())
			{
				player.sendMessage("Cannot do this while transformed");
				return;
			}
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat");
				return;
			}
			final int val = Integer.parseInt(action.substring(19));
			final ClassId classId = player.getClassId();
			final ClassId newClassId = ClassId.values()[val];
			final int level = player.getLevel();
			final int jobLevel = classId.level();
			final int newJobLevel = newClassId.level();
			// Prevents changing to class not in same class tree
			if (!newClassId.childOf(classId))
				return;
			// Prevents changing between same level jobs
			if (newJobLevel != jobLevel + 1)
				return;
			// Check for player level
			if (level < 20 && newJobLevel > 1)
				return;
			if (level < 40 && newJobLevel > 2)
				return;
			if (level < 76 && newJobLevel > 3)
				return;
			// -- Prevention ends
			changeClass(player, val);
			if (newJobLevel == 3)
				player.sendPacket(new SystemMessage(SystemMessageId.THIRD_CLASS_TRANSFER));
			else
				player.sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER));
			/* player.rewardSkills(); */// already sent in changeClass
			NpcHtmlMessage html = new NpcHtmlMessage(31228);
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("Class Upgrader:<br>");
			sb.append("<br>");
			sb.append("You have become a <font color=\"LEVEL\">" + CharTemplateTable.getInstance().getClassNameById(player.getClassId().getId()) + "</font>.");
			if ((level >= 76 && newJobLevel < 3) || (level >= 40 && newJobLevel < 2))
			{
				sb.append("<br><button value=\"Next Class\" action=\"bypass -h gem_upgradeclass\" width=100 height=23 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\">");
			}
			else
				sb.append("<br><button value=\"Welcome Page\" action=\"bypass -h gem_welcome\" width=100 height=23 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\">");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
		}
		else if (action.startsWith("telemenu"))
		{
			showTelePage(player, action.substring(9));
		}
		else if (action.startsWith("welcome"))
		{
			final File mainText = new File(Config.DATAPACK_ROOT, "data/html/welcome.htm"); // Return the pathfile of the HTML file
			if (mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/welcome.htm");
				html.replace("%name%", player.getName()); // replaces %name% with activeChar.getName(), so you can say like "welcome to the server %name%"
				player.sendPacket(html);
			}
		}
		else if (action.startsWith("settings"))
		{
			String filename = "data/html/custom/Gem/CharacterPanel/main.htm";
			String content = HtmCache.getInstance().getHtm(filename);
			if (content == null)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setHtml(content);
				itemReply.replace("%name%", player.getName()); // replaces %name% with activeChar.getName(), so you can say like "welcome to the server %name%"
				itemReply.replace("%trade_status%", player.getActingPlayer().getVarB("noTrade") ? "<font color=\"2e73a3\">ON</font>" : "<font color=\"a22020\">OFF</font>");
				itemReply.replace("%shots_anim_status%", player.getActingPlayer().getVarB("hideEnchAnime") ? "<font color=\"2e73a3\">Hidden</font>" : "<font color=\"a22020\">OFF</font>");
				itemReply.replace("%hide_stores_status%", player.getActingPlayer().getVarB("hideStores") ? "<font color=\"2e73a3\">Hidden</font>" : "<font color=\"a22020\">OFF</font>");
				itemReply.replace("%skills_anim_status%", player.getActingPlayer().getVarB("hideSkillsAnim") ? "<font color=\"2e73a3\">Hidden</font>" : "<font color=\"a22020\">OFF</font>");
				itemReply.replace("%cloaks_anim_status%", player.getActingPlayer().getVarB("hidecloaks") ? "<font color=\"2e73a3\">Hidden</font>" : "<font color=\"a22020\">OFF</font>");
				itemReply.replace("%dresses_anim_status%", player.getActingPlayer().getVarB("hidedress") ? "<font color=\"2e73a3\">Hidden</font>" : "<font color=\"a22020\">OFF</font>");
				player.sendPacket(itemReply);
			}
		}
		else if (action.startsWith("options"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("data/html/custom/Gem/CharacterPanel/main.htm");
			String subCommand = action.split(" ")[1];
			switch (subCommand)
			{
				case "tradeprot":
					if (player.getVarB("noTrade"))
					{
						player.setVar("noTrade", "false");
						player.sendMessage("Trade refusal mode disabled.");
					}
					else
					{
						player.setVar("noTrade", "true");
						player.sendMessage("Trade refusal mode enabled.");
					}
					break;
				case "hidestores":
					if (player.getVarB("hideStores"))
					{
						player.setVar("hideStores", "false");
						player.sendMessage("All stores are visible, please restart.");
					}
					else
					{
						player.setVar("hideStores", "true");
						player.sendMessage("All stores are invisible, please restart.");
					}
					break;
				case "skillsanimations":
					if (player.getVarB("hideSkillsAnim"))
					{
						player.setVar("hideSkillsAnim", "false");
						player.sendMessage("Hide all Skills Animations disabled.");
					}
					else
					{
						player.setVar("hideSkillsAnim", "true");
						player.sendMessage("Hide all Skills Animations enabled.");
					}
					break;
				case "blockenchantanime":
					if (!player.getVarB("hideEnchAnime"))
					{
						player.setVar("hideEnchAnime", "true");
						player.sendMessage("Broadcast Enchant glow enabled.");
					}
					else
					{
						player.setVar("hideEnchAnime", "false");
						player.sendMessage("Broadcast Enchant glow disabled.");
					}
					break;
				case "blockCloaks":
					if (!player.getVarB("hidecloaks"))
					{
						player.setVar("hidecloaks", "true");
						player.sendMessage("Broadcast Cloaks enabled.");
					}
					else
					{
						player.setVar("hidecloaks", "false");
						player.sendMessage("Broadcast Cloaks disabled.");
					}
					break;
				case "blockDresses":
					if (!player.getVarB("hidedress"))
					{
						player.setVar("hidedress", "true");
						player.sendMessage("Broadcast dresses enabled.");
					}
					else
					{
						player.setVar("hidedress", "false");
						player.sendMessage("Broadcast dresses disabled.");
					}
					break;
			}
			html.replace("%trade_status%", player.getActingPlayer().getVarB("noTrade") ? "<font color=\"2e73a3\">ON</font>" : "<font color=\"a22020\">OFF</font>");
			html.replace("%shots_anim_status%", player.getActingPlayer().getVarB("hideEnchAnime") ? "<font color=\"2e73a3\">Hidden</font>" : "<font color=\"a22020\">OFF</font>");
			html.replace("%hide_stores_status%", player.getActingPlayer().getVarB("hideStores") ? "<font color=\"2e73a3\">Hidden</font>" : "<font color=\"a22020\">OFF</font>");
			html.replace("%skills_anim_status%", player.getActingPlayer().getVarB("hideSkillsAnim") ? "<font color=\"2e73a3\">Hidden</font>" : "<font color=\"a22020\">OFF</font>");
			html.replace("%cloaks_anim_status%", player.getActingPlayer().getVarB("hidecloaks") ? "<font color=\"2e73a3\">Hidden</font>" : "<font color=\"a22020\">OFF</font>");
			html.replace("%dresses_anim_status%", player.getActingPlayer().getVarB("hidedress") ? "<font color=\"2e73a3\">Hidden</font>" : "<font color=\"a22020\">OFF</font>");
			player.sendPacket(html);
		}
		else if (action.startsWith("teleto"))
		{
			if (player != null)
			{
				if (player.isInFunEvent())
				{
					player.sendMessage("Cannot use while in an event");
					return;
				}
				if (player.isFlying() || player.isFlyingMounted() || player.isInJail())
				{
					player.sendMessage("Denied");
					return;
				}
				if (action.substring(7).equalsIgnoreCase("unstuck"))
				{
					IUserCommandHandler handler = UserCommandHandler.getInstance().getUserCommandHandler(52);
					if (handler != null)
						handler.useUserCommand(52, player); // unstuck command
				}
				else
				{
					if (player.getInstanceId() > 0 && (player.getInstanceId() == 1 || (InstanceManager.getInstance().getPlayerWorld(player) != null && InstanceManager.getInstance().getPlayerWorld(player).templateId != InstanceManager.ULTRAVERSE_ID)))
					{
						player.sendMessage("Cannot use while in an instance");
						return;
					}
					L2TeleporterInstance.doTeleport(player, Integer.parseInt(action.substring(7)), true);
				}
			}
		}
		else if (action.equalsIgnoreCase("trainskill"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat");
				return;
			}
			if (player.isTransformed() && !player.isUsingInquisitorStance())
			{
				player.sendMessage("Cannot do this while transformed");
				return;
			}
			String filename = "data/html/custom/Gem/skill_enchant.htm";
			String content = HtmCache.getInstance().getHtm(filename);
			if (content == null)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setHtml(content);
				player.sendPacket(itemReply);
			}
		}
		else if (action.startsWith("enchantskill"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat");
				return;
			}
			if (player.isTransformed() && !player.isUsingInquisitorStance())
			{
				player.sendMessage("Cannot do this while transformed");
				return;
			}
			L2NpcInstance.onBypass(player, action.substring(13));
		}
		else if (action.equalsIgnoreCase("enchanthelp"))
		{
			String filename = "data/html/custom/Gem/skillenchanthelp.htm";
			String content = HtmCache.getInstance().getHtm(filename);
			if (content == null)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setHtml(content);
				player.sendPacket(itemReply);
			}
		}
		else if (action.startsWith("inertia_main"))
		{
			InertiaController.getInstance().renderChill(player);
		
		}
		else if (action.equalsIgnoreCase("stats"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			TextBuilder html1 = new TextBuilder("<html><body>");
			html1.append("<br><center><font color=\"LEVEL\">[Additional Player Stats]</font></center>");
			html1.append("<table border=0 width=\"100%\">");
			html1.append("<tr><td>Critical Damage Multi</td><td>" + new DecimalFormat("0.##").format(player.getCriticalDmg(null, 1.66, null)) + "x +" + player.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, null, null) + "</td></tr>");
			html1.append("<tr><td>Magic Critical Rate</td><td>" + Math.round(player.getMCriticalHit(null, null) / 10) + "%" + "</td></tr>");
			final int combinedCritRate = (int) (player.calcStat(Stats.SKILL_CRITICAL_CHANCE_INCREASE, 15 * (player.isDaggerClass() ? Formulas.STRbonus[player.getSTR()] : Formulas.DEXbonus[player.getDEX()]), null, null));
			html1.append("<tr><td>Skill Critical Rate</td><td>" + combinedCritRate + "%" + "</td></tr>");
			html1.append("<tr><td>Skill Reuse Delay</td><td>" + (int) (player.getStat().getMReuseRateGem(false) * 100) + "%" + "</td></tr>");
			html1.append("<tr><td>Magic Reuse Delay</td><td>" + (int) (player.getStat().getMReuseRateGem(true) * 100) + "%" + "</td></tr>");
			html1.append("<tr><td>Attack Reuse Delay</td><td>" + (int) (player.getAtkReuse(100)) + "%" + "</td></tr>");
			final int shldRate = (int) Math.min(player.getShldRate(null, null), player.calcStat(Stats.BLOCK_RATE_MAX, 80, null, null));
			html1.append("<tr><td>Shield Block Rate</td><td>" + shldRate + "%" + "</td></tr>");
			html1.append("<tr><td>Shield Defense</td><td>" + player.getShldDef() + "</td></tr>");
			html1.append("<tr><td>Shield Defense Angle</td><td>" + (shldRate >= 1 ? (int) player.calcStat(Stats.SHIELD_DEFENCE_ANGLE, 120, null, null) : "N/A") + "</td></tr>");
			html1.append("<tr><td>Healed Boost (received)</td><td>" + (int) (player.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>Healing Power (given)</td><td>" + (int) (player.calcStat(Stats.HEAL_PROFICIENCY, 100, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>PVP Attack Hits Damage</td><td>" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null)) + "x" + "</td></tr>");
			html1.append("<tr><td>PVP Physical Skill Damage</td><td>" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null)) + "x" + "</td></tr>");
			html1.append("<tr><td>PVP Magical Damage</td><td>" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null)) + "x" + "</td></tr>");
			html1.append("<tr><td>PVM Damage Bonus</td><td>" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVM_DAMAGE, 1, null, null)) + "x" + "</td></tr>");
			html1.append("<tr><td>PVM Damage Vulnerability</td><td>" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVM_DAMAGE_VUL, 1, null, null)) + "</td></tr>");
			html1.append("<tr><td>Physical Skill Dodge</td><td>" + (int) (player.calcStat(Stats.P_SKILL_EVASION, 0, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>Magic Skill Dodge</td><td>" + (int) (player.calcStat(Stats.M_SKILL_EVASION, 0, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>Attack Range</td><td>" + player.getPhysicalAttackRange() + "</td></tr>");
			html1.append("<tr><td>Cast Range</td><td>" + "skill default +" + player.getStat().getMagicalRangeBoost() + "</td></tr>");
			html1.append("<tr><td>Damage Reflect</td><td>" + (int) (player.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>Skill Reflect</td><td>" + (int) (player.getStat().calcStat(Stats.REFLECT_SKILL_PHYSIC, 0, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>Magic Reflect</td><td>" + (int) (player.getStat().calcStat(Stats.REFLECT_SKILL_MAGIC, 0, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>HP Regen</td><td>" + (int) (Formulas.calcHpRegen(player)) + " per tick" + "</td></tr>");
			html1.append("<tr><td>Vamp. Absorb %</td><td>" + (int) (player.getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null)) + "%" + "</td></tr><br><br>");
			html1.append("<tr><td>Skill Vamp. Absorb %</td><td>" + (int) (player.getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT_SKILL, 0, null, null)) + "%" + "</td></tr><br><br>");
			html1.append("<tr><td>Critical Damage Resist</td><td>" + (int) (1 - player.getStat().calcStat(Stats.CRIT_VULN, 1, null, null)) * 100 + "%" + "</td></tr><br><br>");
			html1.append("<tr><td>Critical Hit Negation</td><td>" + player.calcStat(Stats.CRIT_DAMAGE_EVASION, 0, null, null) + "%" + "</td></tr><br><br>");
			html1.append("<tr><td>Magical Damage Resist</td><td>" + (int) (1 - player.getStat().calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, null)) * 100 + "%" + "</td></tr><br><br>");
			html1.append("<tr><td>Magic Crit Dmg Multi</td><td>" + new DecimalFormat("0.##").format(player.getStat().calcStat(Stats.MAGIC_CRITICAL_DAMAGE, 2, null, null)) + "x" + "</td></tr><br><br>");
			final int atkCount = (int) (player.getStat().calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null));
			html1.append("<tr><td>Attack Count</td><td>" + atkCount + "</td></tr><br><br>");
			html1.append("<tr><td>Attack AOE Angle</td><td>" + (atkCount > 1 ? (int) (player.getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null)) : "N/A") + "</td></tr><br><br>");
			html1.append("<tr><td>Absolute Evasion Chance</td><td>" + (int) (player.getStat().calcStat(Stats.EVASION_ABSOLUTE, 0, null, null)) + "%" + "</td></tr><br><br>");
			html1.append("</table>");
			html1.append("<center><button value=\"Back\" action=\"bypass -h gem_gemmain\" width=90 height=23 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td></center>");
			html1.append("</body></html>");
			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else if (action.startsWith("symbol"))
		{
			String command = action.substring(7);
			if (command.equals("Draw"))
			{
				player.sendPacket(new HennaEquipList(player));
			}
			else if (command.equals("RemoveList"))
			{
				L2SymbolMakerInstance.showRemoveChat(player);
			}
			else if (command.startsWith("Remove "))
			{
				int slot = Integer.parseInt(command.substring(7));
				player.removeHenna(slot);
			}
			else if (command.equalsIgnoreCase("main"))
			{
				String filename = "data/html/custom/Gem/SymbolMaker.htm";
				String content = HtmCache.getInstance().getHtm(filename);
				if (content == null)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
					player.sendPacket(html);
				}
				else
				{
					NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
					itemReply.setHtml(content);
					player.sendPacket(itemReply);
				}
			}
			else
			{}
		}
		else if (action.startsWith("Augment"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat");
				return;
			}
			if (player.isTransformed())
			{
				player.sendMessage("Cannot do this while transformed");
				return;
			}
			final int cmdChoice = Integer.parseInt(action.substring(8, 9).trim());
			switch (cmdChoice)
			{
				case 0:
					String filename = "data/html/custom/Gem/augment.htm";
					String content = HtmCache.getInstance().getHtm(filename);
					if (content == null)
					{
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
						player.sendPacket(html);
					}
					else
					{
						NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
						itemReply.setHtml(content);
						player.sendPacket(itemReply);
					}
					break;
				case 1:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED));
					player.sendPacket(new ExShowVariationMakeWindow());
					break;
				case 2:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION));
					player.sendPacket(new ExShowVariationCancelWindow());
					break;
			}
		}
		else if (action.equalsIgnoreCase("shop"))
		{
			String filename = "data/html/custom/Gem/shop/shopmain.htm";
			String content = HtmCache.getInstance().getHtm(filename);
			if (content == null)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setHtml(content);
				player.sendPacket(itemReply);
			}
		}
		else if (action.startsWith("shopshow"))
		{
			showShopPage(player, action.substring(9));
		}
		else if (action.startsWith("shopbuy"))
		{
			handleBuyRequest(player, action.substring(8));
		}
		else if (action.startsWith("shopsell"))
		{
			showSellWindow(player);
		}
		else if (action.startsWith("shadow"))
		{
			showSellWindow(player);
		}
		else if (action.startsWith("shadow_armor_get"))
		{
			showSellWindow(player);
		}
		else if (action.startsWith("shadow_weapon_get"))
		{
			showSellWindow(player);
		}
		else if (action.startsWith("bon_")) // bon = brain of nexus
		{
			bonBypass(action.substring(3), player);
		}
		if (action.equals("classpath"))
		{
			mainPath(player);
			return;
		}
		if (action.equals("classpathoff"))
		{
			offensive(player);
			return;
		}
		if (action.equals("classpathmage"))
		{
			mage(player);
			return;
		}
		if (action.equals("classpathdef"))
		{
			def(player);
			return;
		}
		if (action.equals("classpathsup"))
		{
			sup(player);
			return;
		}
		if (action.equals("middleoff"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				offensive(player);
				return;
			}
			if (player.getMiddleOff() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9421, 1);
				player.addSkill(Skill, true);
				player.incMiddleOff();
			}
			else if (player.getMiddleOff() == 1)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9422, 1);
				player.addSkill(Skill, true);
				player.incMiddleOff();
			}
			else if (player.getMiddleOff() == 2)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9423, 1);
				player.addSkill(Skill, true);
				player.incMiddleOff();
			}
			else
			{
				offensive(player);
				return;
			}
			pak(player);
			offensive(player);
			return;
		}
		if (action.equals("leftoff"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints() || player.getRightOff() >= 1)
			{
				if (player.getMaxCpPoints() >= player.getCpPoints())
					player.sendMessage("You don't have enough path points.");
				if (player.getRightOff() >= 1)
					player.sendMessage("You can't choose both left and right path.");
				offensive(player);
				return;
			}
			if (player.getLeftOff() == 0 && player.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9424, 1);
				player.addSkill(Skill, true);
				player.incLeftOff();
			}
			else if (player.getLeftOff() == 1 && player.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9425, 1);
				player.addSkill(Skill, true);
				player.incLeftOff();
			}
			else if (player.getLeftOff() == 2 && player.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9426, 1);
				player.addSkill(Skill, true);
				player.incLeftOff();
			}
			else
			{
				offensive(player);
				return;
			}
			pak(player);
			offensive(player);
			return;
		}
		if (action.equals("leftoffone"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				offensive(player);
				return;
			}
			if (player.getLeftOff1() == 0 && player.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9427, 1);
				player.addSkill(Skill, true);
				player.incLeftOff1();
			}
			else if (player.getLeftOff1() == 1 && player.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9428, 1);
				player.addSkill(Skill, true);
				player.incLeftOff1();
			}
			else if (player.getLeftOff1() == 2 && player.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9429, 1);
				player.addSkill(Skill, true);
				player.incLeftOff1();
			}
			else
			{
				offensive(player);
				return;
			}
			pak(player);
			offensive(player);
			return;
		}
		if (action.equals("leftofftwo"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				offensive(player);
				return;
			}
			if (player.getLeftOff2() == 0 && player.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9430, 1);
				player.addSkill(Skill, true);
				player.incLeftOff2();
			}
			else if (player.getLeftOff2() == 1 && player.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9431, 1);
				player.addSkill(Skill, true);
				player.incLeftOff2();
			}
			else if (player.getLeftOff2() == 2 && player.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9432, 1);
				player.addSkill(Skill, true);
				player.incLeftOff2();
			}
			else
			{
				offensive(player);
				return;
			}
			pak(player);
			offensive(player);
			return;
		}
		if (action.equals("rightoff"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints() || player.getLeftOff() >= 1)
			{
				if (player.getMaxCpPoints() >= player.getCpPoints())
					player.sendMessage("You don't have enough path points.");
				if (player.getLeftOff() >= 1)
					player.sendMessage("You can't choose both left and right path.");
				offensive(player);
				return;
			}
			if (player.getRightOff() == 0 && player.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9433, 1);
				player.addSkill(Skill, true);
				player.incRightOff();
			}
			else if (player.getRightOff() == 1 && player.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9434, 1);
				player.addSkill(Skill, true);
				player.incRightOff();
			}
			else if (player.getRightOff() == 2 && player.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9435, 1);
				player.addSkill(Skill, true);
				player.incRightOff();
			}
			else
			{
				offensive(player);
				return;
			}
			pak(player);
			offensive(player);
			return;
		}
		if (action.equals("rightoffone"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				offensive(player);
				return;
			}
			if (player.getRightOff1() == 0 && player.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9436, 1);
				player.addSkill(Skill, true);
				player.incRightOff1();
			}
			else if (player.getRightOff1() == 1 && player.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9437, 1);
				player.addSkill(Skill, true);
				player.incRightOff1();
			}
			else if (player.getRightOff1() == 2 && player.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9438, 1);
				player.addSkill(Skill, true);
				player.incRightOff1();
			}
			else
			{
				offensive(player);
				return;
			}
			pak(player);
			offensive(player);
			return;
		}
		if (action.equals("rightofftwo"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				offensive(player);
				return;
			}
			if (player.getRightOff2() == 0 && player.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9439, 1);
				player.addSkill(Skill, true);
				player.incRightOff2();
			}
			else if (player.getRightOff2() == 1 && player.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9440, 1);
				player.addSkill(Skill, true);
				player.incRightOff2();
			}
			else if (player.getRightOff2() == 2 && player.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9441, 1);
				player.addSkill(Skill, true);
				player.incRightOff2();
			}
			else
			{
				offensive(player);
				return;
			}
			pak(player);
			offensive(player);
			return;
		}
		if (action.equals("middlemage"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				mage(player);
				return;
			}
			if (player.getMiddleMage() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9442, 1);
				player.addSkill(Skill, true);
				player.incMiddleMage();
			}
			else if (player.getMiddleMage() == 1)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9443, 1);
				player.addSkill(Skill, true);
				player.incMiddleMage();
			}
			else if (player.getMiddleMage() == 2)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9444, 1);
				player.addSkill(Skill, true);
				player.incMiddleMage();
			}
			else
			{
				mage(player);
				return;
			}
			pak(player);
			mage(player);
			return;
		}
		if (action.equals("leftmage"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints() || player.getRightMage() >= 1)
			{
				if (player.getMaxCpPoints() >= player.getCpPoints())
					player.sendMessage("You don't have enough path points.");
				if (player.getRightMage() >= 1)
					player.sendMessage("You can't choose both left and right path.");
				mage(player);
				return;
			}
			if (player.getLeftMage() == 0 && player.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9445, 1);
				player.addSkill(Skill, true);
				player.incLeftMage();
			}
			else if (player.getLeftMage() == 1 && player.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9446, 1);
				player.addSkill(Skill, true);
				player.incLeftMage();
			}
			else if (player.getLeftMage() == 2 && player.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9447, 1);
				player.addSkill(Skill, true);
				player.incLeftMage();
			}
			else
			{
				mage(player);
				return;
			}
			pak(player);
			mage(player);
			return;
		}
		if (action.equals("leftmageone"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				mage(player);
				return;
			}
			if (player.getLeftMage1() == 0 && player.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9448, 1);
				player.addSkill(Skill, true);
				player.incLeftMage1();
			}
			else if (player.getLeftMage1() == 1 && player.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9449, 1);
				player.addSkill(Skill, true);
				player.incLeftMage1();
			}
			else if (player.getLeftMage1() == 2 && player.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9450, 1);
				player.addSkill(Skill, true);
				player.incLeftMage1();
			}
			else
			{
				mage(player);
				return;
			}
			pak(player);
			mage(player);
			return;
		}
		if (action.equals("leftmagetwo"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				mage(player);
				return;
			}
			if (player.getLeftMage2() == 0 && player.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9451, 1);
				player.addSkill(Skill, true);
				player.incLeftMage2();
			}
			else if (player.getLeftMage2() == 1 && player.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9452, 1);
				player.addSkill(Skill, true);
				player.incLeftMage2();
			}
			else if (player.getLeftMage2() == 2 && player.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9453, 1);
				player.addSkill(Skill, true);
				player.incLeftMage2();
			}
			else
			{
				mage(player);
				return;
			}
			pak(player);
			mage(player);
			return;
		}
		if (action.equals("rightmage"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints() || player.getLeftMage() >= 1)
			{
				if (player.getMaxCpPoints() >= player.getCpPoints())
					player.sendMessage("You don't have enough path points.");
				if (player.getLeftMage() >= 1)
					player.sendMessage("You can't choose both left and right path.");
				mage(player);
				return;
			}
			if (player.getRightMage() == 0 && player.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9454, 1);
				player.addSkill(Skill, true);
				player.incRightMage();
			}
			else if (player.getRightMage() == 1 && player.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9455, 1);
				player.addSkill(Skill, true);
				player.incRightMage();
			}
			else if (player.getRightMage() == 2 && player.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9456, 1);
				player.addSkill(Skill, true);
				player.incRightMage();
			}
			else
			{
				mage(player);
				return;
			}
			pak(player);
			mage(player);
			return;
		}
		if (action.equals("rightmageone"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				mage(player);
				return;
			}
			if (player.getRightMage1() == 0 && player.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9457, 1);
				player.addSkill(Skill, true);
				player.incRightMage1();
			}
			else if (player.getRightMage1() == 1 && player.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9458, 1);
				player.addSkill(Skill, true);
				player.incRightMage1();
			}
			else if (player.getRightMage1() == 2 && player.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9459, 1);
				player.addSkill(Skill, true);
				player.incRightMage1();
			}
			else
			{
				mage(player);
				return;
			}
			pak(player);
			mage(player);
			return;
		}
		if (action.equals("rightmagetwo"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				mage(player);
				return;
			}
			if (player.getRightMage2() == 0 && player.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9460, 1);
				player.addSkill(Skill, true);
				player.incRightMage2();
			}
			else if (player.getRightMage2() == 1 && player.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9461, 1);
				player.addSkill(Skill, true);
				player.incRightMage2();
			}
			else if (player.getRightMage2() == 2 && player.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9462, 1);
				player.addSkill(Skill, true);
				player.incRightMage2();
			}
			else
			{
				mage(player);
				return;
			}
			pak(player);
			mage(player);
			return;
		}
		if (action.equals("middledef"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				def(player);
				return;
			}
			if (player.getMiddleDef() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9463, 1);
				player.addSkill(Skill, true);
				player.incMiddleDef();
			}
			else if (player.getMiddleDef() == 1)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9464, 1);
				player.addSkill(Skill, true);
				player.incMiddleDef();
			}
			else if (player.getMiddleDef() == 2)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9465, 1);
				player.addSkill(Skill, true);
				player.incMiddleDef();
			}
			else
			{
				def(player);
				return;
			}
			pak(player);
			def(player);
			return;
		}
		if (action.equals("leftdef"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints() || player.getRightDef() >= 1)
			{
				if (player.getMaxCpPoints() >= player.getCpPoints())
					player.sendMessage("You don't have enough path points.");
				if (player.getRightDef() >= 1)
					player.sendMessage("You can't choose both left and right path.");
				def(player);
				return;
			}
			if (player.getLeftDef() == 0 && player.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9466, 1);
				player.addSkill(Skill, true);
				player.incLeftDef();
			}
			else if (player.getLeftDef() == 1 && player.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9467, 1);
				player.addSkill(Skill, true);
				player.incLeftDef();
			}
			else if (player.getLeftDef() == 2 && player.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9468, 1);
				player.addSkill(Skill, true);
				player.incLeftDef();
			}
			else
			{
				def(player);
				return;
			}
			pak(player);
			def(player);
			return;
		}
		if (action.equals("leftdefone"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				def(player);
				return;
			}
			if (player.getLeftDef1() == 0 && player.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9469, 1);
				player.addSkill(Skill, true);
				player.incLeftDef1();
			}
			else if (player.getLeftDef1() == 1 && player.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9470, 1);
				player.addSkill(Skill, true);
				player.incLeftDef1();
			}
			else if (player.getLeftDef1() == 2 && player.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9471, 1);
				player.addSkill(Skill, true);
				player.incLeftDef1();
			}
			else
			{
				def(player);
				return;
			}
			pak(player);
			def(player);
			return;
		}
		if (action.equals("leftdeftwo"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				def(player);
				return;
			}
			if (player.getLeftDef2() == 0 && player.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9472, 1);
				player.addSkill(Skill, true);
				player.incLeftDef2();
			}
			else if (player.getLeftDef2() == 1 && player.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9473, 1);
				player.addSkill(Skill, true);
				player.incLeftDef2();
			}
			else if (player.getLeftDef2() == 2 && player.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9474, 1);
				player.addSkill(Skill, true);
				player.incLeftDef2();
			}
			else
			{
				def(player);
				return;
			}
			pak(player);
			def(player);
			return;
		}
		if (action.equals("rightdef"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints() || player.getLeftDef() >= 1)
			{
				if (player.getMaxCpPoints() >= player.getCpPoints())
					player.sendMessage("You don't have enough path points.");
				if (player.getLeftDef() >= 1)
					player.sendMessage("You can't choose both left and right path.");
				def(player);
				return;
			}
			if (player.getRightDef() == 0 && player.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9475, 1);
				player.addSkill(Skill, true);
				player.incRightDef();
			}
			else if (player.getRightDef() == 1 && player.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9476, 1);
				player.addSkill(Skill, true);
				player.incRightDef();
			}
			else if (player.getRightDef() == 2 && player.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9477, 1);
				player.addSkill(Skill, true);
				player.incRightDef();
			}
			else
			{
				def(player);
				return;
			}
			pak(player);
			def(player);
			return;
		}
		if (action.equals("rightdefone"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				def(player);
				return;
			}
			if (player.getRightDef1() == 0 && player.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9478, 1);
				player.addSkill(Skill, true);
				player.incRightDef1();
			}
			else if (player.getRightDef1() == 1 && player.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9479, 1);
				player.addSkill(Skill, true);
				player.incRightDef1();
			}
			else if (player.getRightDef1() == 2 && player.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9480, 1);
				player.addSkill(Skill, true);
				player.incRightDef1();
			}
			else
			{
				def(player);
				return;
			}
			pak(player);
			def(player);
			return;
		}
		if (action.equals("rightdeftwo"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				def(player);
				return;
			}
			if (player.getRightDef2() == 0 && player.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9481, 1);
				player.addSkill(Skill, true);
				player.incRightDef2();
			}
			else if (player.getRightDef2() == 1 && player.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9482, 1);
				player.addSkill(Skill, true);
				player.incRightDef2();
			}
			else if (player.getRightDef2() == 2 && player.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9483, 1);
				player.addSkill(Skill, true);
				player.incRightDef2();
			}
			else
			{
				def(player);
				return;
			}
			pak(player);
			def(player);
			return;
		}
		if (action.equals("middlesup"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				sup(player);
				return;
			}
			if (player.getMiddleSup() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9484, 1);
				player.addSkill(Skill, true);
				player.incMiddleSup();
			}
			else if (player.getMiddleSup() == 1)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9485, 1);
				player.addSkill(Skill, true);
				player.incMiddleSup();
			}
			else if (player.getMiddleSup() == 2)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9486, 1);
				player.addSkill(Skill, true);
				player.incMiddleSup();
			}
			else
			{
				sup(player);
				return;
			}
			pak(player);
			sup(player);
			return;
		}
		if (action.equals("leftsup"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints() || player.getRightSup() >= 1)
			{
				if (player.getMaxCpPoints() >= player.getCpPoints())
					player.sendMessage("You don't have enough path points.");
				if (player.getRightSup() >= 1)
					player.sendMessage("You can't choose both left and right path.");
				sup(player);
				return;
			}
			if (player.getLeftSup() == 0 && player.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9487, 1);
				player.addSkill(Skill, true);
				player.incLeftSup();
			}
			else if (player.getLeftSup() == 1 && player.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9488, 1);
				player.addSkill(Skill, true);
				player.incLeftSup();
			}
			else if (player.getLeftSup() == 2 && player.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9489, 1);
				player.addSkill(Skill, true);
				player.incLeftSup();
			}
			else
			{
				sup(player);
				return;
			}
			pak(player);
			sup(player);
			return;
		}
		if (action.equals("leftsupone"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				sup(player);
				return;
			}
			if (player.getLeftSup1() == 0 && player.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9490, 1);
				player.addSkill(Skill, true);
				player.incLeftSup1();
			}
			else if (player.getLeftSup1() == 1 && player.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9491, 1);
				player.addSkill(Skill, true);
				player.incLeftSup1();
			}
			else if (player.getLeftSup1() == 2 && player.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9492, 1);
				player.addSkill(Skill, true);
				player.incLeftSup1();
			}
			else
			{
				sup(player);
				return;
			}
			pak(player);
			sup(player);
			return;
		}
		if (action.equals("leftsuptwo"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				sup(player);
				return;
			}
			if (player.getLeftSup2() == 0 && player.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9493, 1);
				player.addSkill(Skill, true);
				player.incLeftSup2();
			}
			else if (player.getLeftSup2() == 1 && player.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9494, 1);
				player.addSkill(Skill, true);
				player.incLeftSup2();
			}
			else if (player.getLeftSup2() == 2 && player.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9495, 1);
				player.addSkill(Skill, true);
				player.incLeftSup2();
			}
			else
			{
				sup(player);
				return;
			}
			pak(player);
			sup(player);
			return;
		}
		if (action.equals("rightsup"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints() || player.getLeftSup() >= 1)
			{
				if (player.getMaxCpPoints() >= player.getCpPoints())
					player.sendMessage("You don't have enough path points.");
				if (player.getLeftSup() >= 1)
					player.sendMessage("You can't choose both left and right path.");
				sup(player);
				return;
			}
			if (player.getRightSup() == 0 && player.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9496, 1);
				player.addSkill(Skill, true);
				player.incRightSup();
			}
			else if (player.getRightSup() == 1 && player.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9497, 1);
				player.addSkill(Skill, true);
				player.incRightSup();
			}
			else if (player.getRightSup() == 2 && player.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9498, 1);
				player.addSkill(Skill, true);
				player.incRightSup();
			}
			else
			{
				sup(player);
				return;
			}
			pak(player);
			sup(player);
			return;
		}
		if (action.equals("rightsupone"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				sup(player);
				return;
			}
			if (player.getRightSup1() == 0 && player.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9499, 1);
				player.addSkill(Skill, true);
				player.incRightSup1();
			}
			else if (player.getRightSup1() == 1 && player.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9500, 1);
				player.addSkill(Skill, true);
				player.incRightSup1();
			}
			else if (player.getRightSup1() == 2 && player.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9501, 1);
				player.addSkill(Skill, true);
				player.incRightSup1();
			}
			else
			{
				sup(player);
				return;
			}
			pak(player);
			sup(player);
			return;
		}
		if (action.equals("rightsuptwo"))
		{
			if (player.getMaxCpPoints() >= player.getCpPoints())
			{
				player.sendMessage("You don't have enough path points.");
				sup(player);
				return;
			}
			if (player.getRightSup2() == 0 && player.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9502, 1);
				player.addSkill(Skill, true);
				player.incRightSup2();
			}
			else if (player.getRightSup2() == 1 && player.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9503, 1);
				player.addSkill(Skill, true);
				player.incRightSup2();
			}
			else if (player.getRightSup2() == 2 && player.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9504, 1);
				player.addSkill(Skill, true);
				player.incRightSup2();
			}
			else
			{
				sup(player);
				return;
			}
			pak(player);
			sup(player);
			return;
		}
		if (action.equals("cancelpath"))
		{
			MagicSkillUse MSU = new MagicSkillUse((L2Character) player, (L2Character) player, 5426, 1, 1, 0);
			player.broadcastPacket((L2GameServerPacket) MSU);
			player.clearPath();
			player.broadcastUserInfo();
			mainPath(player);
		}
		else if (action.equals("cancelpathoff"))
		{
			MagicSkillUse MSU = new MagicSkillUse((L2Character) player, (L2Character) player, 5426, 1, 1, 0);
			player.broadcastPacket((L2GameServerPacket) MSU);
			player.clearPathOffensive();
			player.broadcastUserInfo();
			offensive(player);
		}
		else if (action.equals("cancelpathmage"))
		{
			MagicSkillUse MSU = new MagicSkillUse((L2Character) player, (L2Character) player, 5426, 1, 1, 0);
			player.broadcastPacket((L2GameServerPacket) MSU);
			player.clearPathMage();
			player.broadcastUserInfo();
			mage(player);
		}
		else if (action.equals("cancelpathdef"))
		{
			MagicSkillUse MSU = new MagicSkillUse((L2Character) player, (L2Character) player, 5426, 1, 1, 0);
			player.broadcastPacket((L2GameServerPacket) MSU);
			player.clearPathDef();
			player.broadcastUserInfo();
			def(player);
		}
		else if (action.equals("cancelpathsup"))
		{
			MagicSkillUse MSU = new MagicSkillUse((L2Character) player, (L2Character) player, 5426, 1, 1, 0);
			player.broadcastPacket((L2GameServerPacket) MSU);
			player.clearPathSup();
			player.broadcastUserInfo();
			sup(player);
		}
	}
	
	public static void bonBypass(String command, L2PcInstance player)
	{
		if (command.startsWith("_"))
		{}
		else if (command.startsWith("_"))
		{}
	}
	
	private static void mainPath(L2PcInstance player)
	{
		int remain = player.getCpPoints() - player.getMaxCpPoints();
		String filename = "data/html/custom/Gem/classpath/main.htm";
		NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
		itemReply.setFile(filename);
		itemReply.replace("%remain%", remain + "");
		player.sendPacket((L2GameServerPacket) itemReply);
	}
	
	private static void offensive(L2PcInstance player)
	{
		int remain = player.getCpPoints() - player.getMaxCpPoints();
		String filename = "data/html/custom/Gem/classpath/offensive.htm";
		NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
		itemReply.setFile(filename);
		itemReply.replace("%remain%", remain + "");
		itemReply.replace("%middle%", player.getMiddleOff() + "/3");
		itemReply.replace("%left%", player.getLeftOff() + "/3");
		itemReply.replace("%left1%", player.getLeftOff1() + "/3");
		itemReply.replace("%left2%", player.getLeftOff2() + "/3");
		itemReply.replace("%right%", player.getRightOff() + "/3");
		itemReply.replace("%right1%", player.getRightOff1() + "/3");
		itemReply.replace("%right2%", player.getRightOff2() + "/3");
		if (player.getMiddleOff() == 0)
		{
			itemReply.replace("%statmiddle%", "P.Atk +200");
		}
		else if (player.getMiddleOff() == 1)
		{
			itemReply.replace("%statmiddle%", "P.Atk +400");
		}
		else
		{
			itemReply.replace("%statmiddle%", "P.Atk +600");
		}
		if (player.getLeftOff() == 0)
		{
			itemReply.replace("%statleft%", "A.Spd +10");
		}
		else if (player.getLeftOff() == 1)
		{
			itemReply.replace("%statleft%", "A.Spd +20");
		}
		else
		{
			itemReply.replace("%statleft%", "A.Spd +35");
		}
		if (player.getLeftOff1() == 0)
		{
			itemReply.replace("%statleftone%", "Spd +1");
		}
		else if (player.getLeftOff1() == 1)
		{
			itemReply.replace("%statleftone%", "Spd +3");
		}
		else
		{
			itemReply.replace("%statleftone%", "Spd +6");
		}
		if (player.getLeftOff2() == 0)
		{
			itemReply.replace("%statlefttwo%", "Evas +1");
		}
		else if (player.getLeftOff2() == 1)
		{
			itemReply.replace("%statlefttwo%", "Evas +2");
		}
		else
		{
			itemReply.replace("%statlefttwo%", "Evas +4");
		}
		if (player.getRightOff() == 0)
		{
			itemReply.replace("%statright%", "P.Atk +100");
		}
		else if (player.getRightOff() == 1)
		{
			itemReply.replace("%statright%", "P.Atk +250");
		}
		else
		{
			itemReply.replace("%statright%", "P.Atk +400");
		}
		if (player.getRightOff1() == 0)
		{
			itemReply.replace("%statrightone%", "Acc +1");
		}
		else if (player.getRightOff1() == 1)
		{
			itemReply.replace("%statrightone%", "Acc +2");
		}
		else
		{
			itemReply.replace("%statrightone%", "Acc +4");
		}
		if (player.getRightOff2() == 0)
		{
			itemReply.replace("%statrighttwo%", "P.Reu -2%");
		}
		else if (player.getRightOff2() == 1)
		{
			itemReply.replace("%statrighttwo%", "P.Reu -4%");
		}
		else
		{
			itemReply.replace("%statrighttwo%", "P.Reu -8%");
		}
		player.sendPacket((L2GameServerPacket) itemReply);
	}
	
	private static void mage(L2PcInstance player)
	{
		int remain = player.getCpPoints() - player.getMaxCpPoints();
		String filename = "data/html/custom/Gem/classpath/mage.htm";
		NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
		itemReply.setFile(filename);
		itemReply.replace("%remain%", remain + "");
		itemReply.replace("%middle%", player.getMiddleMage() + "/3");
		itemReply.replace("%left%", player.getLeftMage() + "/3");
		itemReply.replace("%left1%", player.getLeftMage1() + "/3");
		itemReply.replace("%left2%", player.getLeftMage2() + "/3");
		itemReply.replace("%right%", player.getRightMage() + "/3");
		itemReply.replace("%right1%", player.getRightMage1() + "/3");
		itemReply.replace("%right2%", player.getRightMage2() + "/3");
		if (player.getMiddleMage() == 0)
		{
			itemReply.replace("%statmiddle%", "M.Atk +400");
		}
		else if (player.getMiddleMage() == 1)
		{
			itemReply.replace("%statmiddle%", "M.Atk +800");
		}
		else
		{
			itemReply.replace("%statmiddle%", "M.Atk +1200");
		}
		if (player.getLeftMage() == 0)
		{
			itemReply.replace("%statleft%", "C.Spd +10");
		}
		else if (player.getLeftMage() == 1)
		{
			itemReply.replace("%statleft%", "C.Spd +30");
		}
		else
		{
			itemReply.replace("%statleft%", "C.Spd +50");
		}
		if (player.getLeftMage1() == 0)
		{
			itemReply.replace("%statleftone%", "M.Pen +1%");
		}
		else if (player.getLeftMage1() == 1)
		{
			itemReply.replace("%statleftone%", "M.Pen +3%");
		}
		else
		{
			itemReply.replace("%statleftone%", "M.Pen +5%");
		}
		if (player.getLeftMage2() == 0)
		{
			itemReply.replace("%statlefttwo%", "Spd +1");
		}
		else if (player.getLeftMage2() == 1)
		{
			itemReply.replace("%statlefttwo%", "Spd +2");
		}
		else
		{
			itemReply.replace("%statlefttwo%", "Spd +5");
		}
		if (player.getRightMage() == 0)
		{
			itemReply.replace("%statright%", "M.Vamp +1%");
		}
		else if (player.getRightMage() == 1)
		{
			itemReply.replace("%statright%", "M.Vamp +3%");
		}
		else
		{
			itemReply.replace("%statright%", "M.Vamp +6%");
		}
		if (player.getRightMage1() == 0)
		{
			itemReply.replace("%statrightone%", "M.Reu -2%");
		}
		else if (player.getRightMage1() == 1)
		{
			itemReply.replace("%statrightone%", "M.Reu -4%");
		}
		else
		{
			itemReply.replace("%statrightone%", "M.Reu -8%");
		}
		if (player.getRightMage2() == 0)
		{
			itemReply.replace("%statrighttwo%", "M.Range +20");
		}
		else if (player.getRightMage2() == 1)
		{
			itemReply.replace("%statrighttwo%", "M.Range +50");
		}
		else
		{
			itemReply.replace("%statrighttwo%", "M.Range +100");
		}
		player.sendPacket((L2GameServerPacket) itemReply);
	}
	
	private static void def(L2PcInstance player)
	{
		int remain = player.getCpPoints() - player.getMaxCpPoints();
		String filename = "data/html/custom/Gem/classpath/def.htm";
		NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
		itemReply.setFile(filename);
		itemReply.replace("%remain%", remain + "");
		itemReply.replace("%middle%", player.getMiddleDef() + "/3");
		itemReply.replace("%left%", player.getLeftDef() + "/3");
		itemReply.replace("%left1%", player.getLeftDef1() + "/3");
		itemReply.replace("%left2%", player.getLeftDef2() + "/3");
		itemReply.replace("%right%", player.getRightDef() + "/3");
		itemReply.replace("%right1%", player.getRightDef1() + "/3");
		itemReply.replace("%right2%", player.getRightDef2() + "/3");
		if (player.getMiddleDef() == 0)
		{
			itemReply.replace("%statmiddle%", "P-M.Def +40");
		}
		else if (player.getMiddleDef() == 1)
		{
			itemReply.replace("%statmiddle%", "P-M.Def +80");
		}
		else
		{
			itemReply.replace("%statmiddle%", "P-M.Def +120");
		}
		if (player.getLeftDef() == 0)
		{
			itemReply.replace("%statleft%", "Hp +300");
		}
		else if (player.getLeftDef() == 1)
		{
			itemReply.replace("%statleft%", "Hp +600");
		}
		else
		{
			itemReply.replace("%statleft%", "Hp +1000");
		}
		if (player.getLeftDef1() == 0)
		{
			itemReply.replace("%statleftone%", "Agro +5%");
		}
		else if (player.getLeftDef1() == 1)
		{
			itemReply.replace("%statleftone%", "Agro +10%");
		}
		else
		{
			itemReply.replace("%statleftone%", "Agro +20%");
		}
		if (player.getLeftDef2() == 0)
		{
			itemReply.replace("%statlefttwo%", "S.Def +300");
		}
		else if (player.getLeftDef2() == 1)
		{
			itemReply.replace("%statlefttwo%", "S.Def +600");
		}
		else
		{
			itemReply.replace("%statlefttwo%", "S.Def +1000");
		}
		if (player.getRightDef() == 0)
		{
			itemReply.replace("%statright%", "Cp +400");
		}
		else if (player.getRightDef() == 1)
		{
			itemReply.replace("%statright%", "Cp +1000");
		}
		else
		{
			itemReply.replace("%statright%", "Cp +2000%");
		}
		if (player.getRightDef1() == 0)
		{
			itemReply.replace("%statrightone%", "S.Rate +3%");
		}
		else if (player.getRightDef1() == 1)
		{
			itemReply.replace("%statrightone%", "S.Rate +6%");
		}
		else
		{
			itemReply.replace("%statrightone%", "S.Rate +10%");
		}
		if (player.getRightDef2() == 0)
		{
			itemReply.replace("%statrighttwo%", "P.Reu -2%");
		}
		else if (player.getRightDef2() == 1)
		{
			itemReply.replace("%statrighttwo%", "P.Reu -4%");
		}
		else
		{
			itemReply.replace("%statrighttwo%", "P.Reu -8%");
		}
		player.sendPacket((L2GameServerPacket) itemReply);
	}
	
	private static void sup(L2PcInstance player)
	{
		int remain = player.getCpPoints() - player.getMaxCpPoints();
		String filename = "data/html/custom/Gem/classpath/sup.htm";
		NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
		itemReply.setFile(filename);
		itemReply.replace("%remain%", remain + "");
		itemReply.replace("%middle%", player.getMiddleSup() + "/3");
		itemReply.replace("%left%", player.getLeftSup() + "/3");
		itemReply.replace("%left1%", player.getLeftSup1() + "/3");
		itemReply.replace("%left2%", player.getLeftSup2() + "/3");
		itemReply.replace("%right%", player.getRightSup() + "/3");
		itemReply.replace("%right1%", player.getRightSup1() + "/3");
		itemReply.replace("%right2%", player.getRightSup2() + "/3");
		if (player.getMiddleSup() == 0)
		{
			itemReply.replace("%statmiddle%", "H.Give +2%");
		}
		else if (player.getMiddleSup() == 1)
		{
			itemReply.replace("%statmiddle%", "H.Give +4%");
		}
		else
		{
			itemReply.replace("%statmiddle%", "H.Give +8%");
		}
		if (player.getLeftSup() == 0)
		{
			itemReply.replace("%statleft%", "Debuff -1%");
		}
		else if (player.getLeftSup() == 1)
		{
			itemReply.replace("%statleft%", "Debuff -3%");
		}
		else
		{
			itemReply.replace("%statleft%", "Debuff -6%");
		}
		if (player.getLeftSup1() == 0)
		{
			itemReply.replace("%statleftone%", "C.Spd +10");
		}
		else if (player.getLeftSup1() == 1)
		{
			itemReply.replace("%statleftone%", "C.Spd +25");
		}
		else
		{
			itemReply.replace("%statleftone%", "C.Spd +60");
		}
		if (player.getLeftSup2() == 0)
		{
			itemReply.replace("%statlefttwo%", "C.DmgTake -1%");
		}
		else if (player.getLeftSup2() == 1)
		{
			itemReply.replace("%statlefttwo%", "C.DmgTake -3%");
		}
		else
		{
			itemReply.replace("%statlefttwo%", "C.DmgTake -6%");
		}
		if (player.getRightSup() == 0)
		{
			itemReply.replace("%statright%", "Mp.Cons -2%");
		}
		else if (player.getRightSup() == 1)
		{
			itemReply.replace("%statright%", "Mp.Cons -5%");
		}
		else
		{
			itemReply.replace("%statright%", "Mp.Cons -10%");
		}
		if (player.getRightSup1() == 0)
		{
			itemReply.replace("%statrightone%", "M.Reu -2%");
		}
		else if (player.getRightSup1() == 1)
		{
			itemReply.replace("%statrightone%", "M.Reu -4%");
		}
		else
		{
			itemReply.replace("%statrightone%", "M.Reu -8%");
		}
		if (player.getRightSup2() == 0)
		{
			itemReply.replace("%statrighttwo%", "Behind.Dmg -2%");
		}
		else if (player.getRightSup2() == 1)
		{
			itemReply.replace("%statrighttwo%", "Behind.Dmg -5%");
		}
		else
		{
			itemReply.replace("%statrighttwo%", "Behind.Dmg -10%");
		}
		player.sendPacket((L2GameServerPacket) itemReply);
	}
	
	private static void pak(L2PcInstance player)
	{
		MagicSkillUse MSU = new MagicSkillUse((L2Character) player, (L2Character) player, 888, 1, 1, 0);
		player.broadcastPacket((L2GameServerPacket) MSU);
		player.broadcastUserInfo();
		player.incCpPoints();
	}
	
	private static String setPinCode(L2PcInstance player, String action)
	{
		if (action.contains("\n"))
		{
			return "Error: Do not press Enter";
		}
		final String[] msg = action.split(" ");
		if (msg.length < 3 || msg.length > 4)
		{
			return "Either you didn't fill in a blank or you have spaces in your code";
		}
		if (msg[0].equals("setpincode_action"))
		{
			if (msg.length != 3)
			{
				return "You cannot have spaces in your pin code";
			}
			if (!msg[1].equals(msg[2]))
			{
				return "You retyped your pin code wrong";
			}
			if (!checkPinCode(msg[1]))
			{
				return "Incorrect pin code format";
			}
			player.setPinCodeAccount(msg[1]);
		}
		else if (msg[0].equals("changepincode_action"))
		{
			if (msg.length != 4)
			{
				return "You forgot to type in one of the prompts";
			}
			if (!msg[2].equals(msg[3]))
			{
				return "You retyped your pin code wrong";
			}
			if (!checkPinCode(msg[2]))
			{
				return "Incorrect pin code format";
			}
			if (!player.getPinCode().equals(msg[1]))
			{
				return "Incorrect account pin code";
			}
			player.setPinCodeAccount(msg[2]);
		}
		else
		{
			_log.config("LOL wtf setpincode called a method where it's neither of the two functions! user name: " + player.getName());
		}
		return null;
	}
	
	private static boolean checkPinCode(String pincode)
	{
		if (pincode == null || pincode.isEmpty())
			return false;
		pincode = pincode.trim();
		if (pincode == null || pincode.isEmpty() || pincode.equalsIgnoreCase("") || pincode.contains(" "))
			return false;
		if (pincode.length() < 2 || pincode.length() > 20)
			return false;
		return true;
	}
	
	private static String setSecretCode(L2PcInstance player, String action)
	{
		if (action.contains("\n"))
		{
			return "Error: Do not press Enter";
		}
		final String[] msg = action.split(" ");
		if (msg.length < 3 || msg.length > 4)
		{
			return "Either you didn't fill in a blank or you have spaces in your code";
		}
		if (msg[0].equals("setsecret_action"))
		{
			if (msg.length != 3)
			{
				return "You cannot have spaces in your secret code";
			}
			if (!msg[1].equals(msg[2]))
			{
				return "You retyped your secret code wrong";
			}
			if (!checkSecretCode(msg[1]))
			{
				return "Incorrect secret code format";
			}
			player.setSecretCodeAccount(msg[1]);
		}
		else if (msg[0].equals("changesecret_action"))
		{
			if (msg.length != 4)
			{
				return "You forgot to type in one of the prompts";
			}
			if (!msg[2].equals(msg[3]))
			{
				return "You retyped your secret code wrong";
			}
			if (!checkSecretCode(msg[2]))
			{
				return "Incorrect secret code format";
			}
			if (!player.getSecretCode().equals(msg[1]))
			{
				return "Incorrect account secret code";
			}
			player.setSecretCodeAccount(msg[2]);
		}
		else
		{
			_log.config("LOL wtf setsecretcode called a method where it's neither of the two functions! user name: " + player.getName());
		}
		return null;
	}
	
	private static boolean checkSecretCode(String secret)
	{
		if (secret == null || secret.isEmpty())
			return false;
		secret = secret.trim();
		if (secret == null || secret.isEmpty() || secret.equalsIgnoreCase("") || secret.contains(" "))
			return false;
		if (secret.length() < 2 || secret.length() > 20)
			return false;
		return true;
	}
	
	private static String doPasswordChange(L2PcInstance player, String action)
	{
		if (action.contains("\n"))
		{
			return "Error: Do not press Enter";
		}
		final String[] msg = action.split(" ", 3);
		if (msg.length < 3)
		{
			return "You need to type in both your secret code and your new password";
		}
		final String secret = msg[1];
		if (!checkSecretCode(secret))
		{
			return "Incorrect secret code";
		}
		final String password = msg[2];
		if (password.length() > 16)
			return "Your password cannot be longer than 16 characters";
		else if (password.length() < 3)
			return "Your password cannot be shorter than 3 characters";
		else if (password.startsWith(" "))
			return "Your password cannot start with spaces";
		String auth = null;
		try
		{
			final MessageDigest md = MessageDigest.getInstance("SHA");
			final byte[] raw = password.getBytes("UTF-8");
			final byte[] hash = md.digest(raw);
			final String accName = player.getAccountName();
			final String codedPass = Base64.encodeBytes(hash);
			boolean authed = false;
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT secret FROM accounts WHERE login = ?");
				statement.setString(1, accName);
				ResultSet rset = statement.executeQuery();
				if (rset.next())
				{
					if (rset.getString("secret").equals(secret))
						authed = true;
					else
						auth = "Incorrect input";
				}
				rset.close();
				statement.close();
				if (authed)
				{
					statement = con.prepareStatement("UPDATE accounts SET password = ?, pass = ? WHERE login = ?");
					statement.setString(1, codedPass);
					statement.setString(2, password);
					statement.setString(3, accName);
					statement.executeUpdate();
					player.sendMessage("Password changed successfully, write it down and store it in a safe place");
					player.getClient().setPassword(password);
				}
				else
				{
					player.sendMessage("Wrong secret question");
				}
				rset.close();
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
		}
		catch (Exception e)
		{
			player.sendMessage("There was an error with your password change.");
			e.printStackTrace();
		}
		return auth;
	}
	
	// PUBLIC & STATIC so other classes from package can include it directly
	private static void showTelePage(L2PcInstance player, String filename)
	{
		String content = HtmCache.getInstance().getHtmForce("data/html/custom/Gem/teleport/" + filename + ".htm");
		NpcHtmlMessage tele = new NpcHtmlMessage(1);
		tele.setHtml(content);
		player.sendPacket(tele);
	}
	
	// PUBLIC & STATIC so other classes from package can include it directly
	private static void showShopPage(L2PcInstance player, String filename)
	{
		String content = HtmCache.getInstance().getHtmForce("data/html/custom/Gem/shop/" + filename);
		NpcHtmlMessage tele = new NpcHtmlMessage(1);
		tele.setHtml(content);
		player.sendPacket(tele);
	}
	
	private static void handleBuyRequest(L2PcInstance activeChar, String command)
	{
		int val = -1;
		try
		{
			val = Integer.parseInt(command);
		}
		catch (Exception e)
		{
			_log.warning("gem buylist failed:" + command);
		}
		if (val == -1)
			return;
		activeChar.tempInventoryDisable();
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		if (list != null)
		{
			activeChar.sendPacket(new BuyList(list, activeChar.getAdena()));
		}
		else
		{
			_log.warning("no buylist with id:" + val);
		}
	}
	
	public static void changeClass(L2PcInstance player, int val)
	{
		player.setClassId(val);
		if (player.isSubClassActive())
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		else
			player.setBaseClass(player.getActiveClass());
	}
	
	private static void showSellWindow(L2PcInstance player)
	{
		player.sendPacket(new SellList(player));
	}
	
	public static void sendClassChangeHTML(L2PcInstance player)
	{
		if (L2ClassMasterInstance.hasValidClasses(player))
			player.sendPacket(new ClassUpgradeWnd(player));
		//player.sendPacket(html);
	}
}