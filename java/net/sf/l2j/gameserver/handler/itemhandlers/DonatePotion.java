package net.sf.l2j.gameserver.handler.itemhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ArmorSetsTable;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ItemLists;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.instancemanager.DayNightSpawnManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.Elementals;
import net.sf.l2j.gameserver.model.L2ArmorSet;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.PunishLevel;
import net.sf.l2j.gameserver.model.actor.instance.L2VillageMasterInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.base.SubClass;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.CharacterCreate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutRegister;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2ArmorType;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.StringUtil;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class DonatePotion implements IItemHandler
{
	private static Random	random							= new Random();
	public final static int	NAME_CHANGE_ITEMID				= 99998;
	public final static int	CLASS_CHANGE_ITEMID				= 99997;
	public final static int	SKILL_15_ITEMID					= 99987;
	public final static int	RACE_CHANGE_ITEMID				= 99996;
	public final static int	WEAPON_EXCHANGE_UNIQUE			= 99976;
	public final static int	WEAPON_EXCHANGE_DREAD			= 99990;
	public final static int	WEAPON_EXCHANGE_TITANIUM		= 99977;
	public final static int	WEAPON_EXCHANGE_VESPER			= 99978;
	public final static int	WEAPON_EXCHANGE_EPIC			= 99979;
	public final static int	ARMOR_SET_EXCHANGER_DREAD		= 99975;
	public final static int	ARMOR_SET_EXCHANGER_TITANIUM	= 99989;
	public final static int	ARMOR_SET_EXCHANGER_RYKROS		= 99974;
	
	final public static boolean allowUse(L2PcInstance player)
	{
		if (player.isInJail())
		{
			player.sendMessage("Cannot use while in jail");
			return false;
		}
		if (player.getCursedWeaponEquippedId() != 0)
		{
			player.sendMessage("Cannot use while cursed");
			return false;
		}
		if (player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player) || player.isInDuel() || player.isInFunEvent())
		{
			player.sendMessage("Cannot use while in Olympiad/Duel/Event");
			return false;
		}
		if (player.getPvpFlag() != 0 || player.isInCombat())
		{
			player.sendMessage("Cannot use while in battle");
			return false;
		}
		if (player.isParalyzed())
		{
			player.sendMessage("Cannot use while paralyzed like a statue");
			return false;
		}
		if (player.getActiveEnchantAttrItem() != null || player.getActiveTradeList() != null || player.getActiveEnchantItem() != null)
		{
			player.sendMessage("Cannot use while trading/enchanting");
			return false;
		}
		return true;
	}
	
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		if (playable.isInUniqueInstance())
		{
			playable.sendMessage("You can't use this item while in instance");
			return;
		}
		final L2PcInstance activeChar = (L2PcInstance) playable;
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		int id = 0;
		int alekos;
		int enhanced, alekos1, alekaras;
		try
		{
			id = Integer.parseInt(item.getEtcItem().getSkills()[0]);
		}
		catch (NumberFormatException e)
		{}
		if (id <= 0)
			return;
		if (id != 1 && id != 20 && id != 21 && !allowUse(activeChar))
			return;
		boolean destroyItem = false;
		switch (id)
		{
			case 1: // clear karma
			{
				if (activeChar.getKarma() > 0)
				{
					MagicSkillUse msk = new MagicSkillUse(activeChar, activeChar, 1426, 1, 900, 0);
					Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 810000);
					activeChar.setKarma(0);
					activeChar.sendMessage("Your karma has been cleared");
					destroyItem = true;
				}
				else
				{
					activeChar.sendMessage("You have no karma to clear");
					return;
				}
				break;
			}
			case 2: // raise clan rep by 1000
			{
				final L2Clan clan = activeChar.getClan();
				if (clan != null)
				{
					MagicSkillUse msk = new MagicSkillUse(activeChar, activeChar, 1374, 1, 1200, 0);
					Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 1000000);
					clan.setReputationScore(clan.getReputationScore() + 1000, true);
					clan.broadcastToOnlineMembers("Clan reputation increased by 1000 with the help of " + activeChar.getName() + "!");
					destroyItem = true;
				}
				else
				{
					activeChar.sendMessage("You are not currently in clan.");
					return;
				}
				break;
			}
			case 3: // wipe self PKs
			{
				if (activeChar.getPkKills() > 0)
				{
					activeChar.setPkKills(0);
					activeChar.sendMessage("Your PK count has been set to 0");
					activeChar.broadcastUserInfo();
					destroyItem = true;
				}
				else
				{
					activeChar.sendMessage("You have no Pks to wipe");
					return;
				}
				break;
			}
			case 4: // increase self pvps
			{
				int pvps = activeChar.getPvpKills();
				if (pvps >= 15000)
				{
					pvps += 1000 / 10;
				}
				else if (pvps >= 10000)
				{
					int diff = 15000 - pvps;
					int addPvps = (int) (1000 / 7.5);
					if (addPvps > diff)
						addPvps = (int) (diff + ((1000 - (diff * 7.5)) / 10));
					pvps += addPvps;
				}
				else if (pvps >= 7000)
				{
					int diff = 10000 - pvps;
					int addPvps = (1000 / 5);
					if (addPvps > diff)
						addPvps = (int) (diff + ((1000 - (diff * 5)) / 7.5));
					pvps += addPvps;
				}
				else if (pvps >= 3000)
				{
					int diff = 7000 - pvps;
					int addPvps = (1000 / 3);
					if (addPvps > diff)
						addPvps = (diff + ((1000 - (diff * 3)) / 5));
					pvps += addPvps;
				}
				else
				{
					// int diff = 3000 - pvps;
					int addPvps = (1000 / 1);
					// if (addPvps > diff)
					// addPvps = (diff + ((1000 - (diff * 1)) / 3));
					pvps += addPvps;
				}
				activeChar.setPvpKills(pvps);
				activeChar.setNameColorsDueToPVP();
				activeChar.sendMessage("Your PVP count is now " + pvps);
				activeChar.broadcastUserInfo();
				destroyItem = true;
				break;
			}
			case 5: // level 90
			{
				if (activeChar.getLevel() >= 90)
				{
					activeChar.sendMessage("You're already >= level 90");
					return;
				}
				else
				{
					try
					{
						final long pXp = activeChar.getExp();
						final long tXp = Experience.LEVEL[90];
						activeChar._ignoreLevel = true;
						activeChar.addExpAndSp(Math.max(tXp - pXp, 0), 1000000000);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					finally
					{
						activeChar._ignoreLevel = false;
					}
					destroyItem = true;
				}
				break;
			}
			case 6: // level 95 - remember that ppl can skip the lvl 90 potion
			{
				if (activeChar.getLevel() >= 93)
				{
					activeChar.sendMessage("You're already >= level 93");
					return;
				}
				else
				{
					try
					{
						final long pXp = activeChar.getExp();
						final long tXp = Experience.LEVEL[93];
						activeChar._ignoreLevel = true;
						activeChar.addExpAndSp(Math.max(tXp - pXp, 0), 1000000000);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					finally
					{
						activeChar._ignoreLevel = false;
					}
					destroyItem = true;
				}
				break;
			}
			case 7: // change name
			{
				String filename = "data/html/custom/Donate/namechange.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "");
				activeChar.sendPacket(itemReply);
				break;
			}
			case 8: // main class <> subclass swap
			{
				final StringBuilder classHTML = StringUtil.startAppend(1000, "");
				if (activeChar.getSubClasses().isEmpty())
				{
					StringUtil.append(classHTML, "<font color=0033FF>You can't change your main class when you don't have a sub class to begin with!<br>Go add your desired class as a subclass at a village master first</font>");
				}
				else
				{
					classHTML.append("Which subclass of yours would you like to switch with your main class?<br>");
					for (Iterator<SubClass> subList = L2VillageMasterInstance.iterSubClasses(activeChar); subList.hasNext();)
					{
						SubClass subClass = subList.next();
						final int subClassId = subClass.getClassId();
						StringUtil.append(classHTML, "<a action=\"bypass -h pot_sub_swap ", String.valueOf(subClass.getClassIndex()), "\">", CharTemplateTable.getInstance().getClassNameById(subClassId), "</a><br>");
					}
				}
				String filename = "data/html/custom/Donate/classchange.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", classHTML.toString());
				activeChar.sendPacket(itemReply);
				break;
			}
			case 9: // alter sex
			{
				MagicSkillUse msk = new MagicSkillUse(activeChar, activeChar, 837, 1, 1000, 0);
				Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 1000000);
				activeChar.getAppearance().setSex(!activeChar.getAppearance().getSex());
				activeChar.storeCharBase();
				activeChar.broadcastUserInfo();
				// Transform-untransorm player quickly to force the client to reload the character textures
				TransformationManager.getInstance().transformPlayer(105, activeChar);
				ThreadPoolManager.getInstance().scheduleGeneral(new Untransform(activeChar), 200);
				activeChar.decayMe();
				activeChar.spawnMe();
				destroyItem = true;
				break;
			}
			case 10: // change race
			{
				String filename = "data/html/custom/Donate/racechange.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				final int currRace = activeChar.getRace().ordinal();
				final StringBuilder skillHTML = StringUtil.startAppend(1000, "");
				skillHTML.append("<center><table>");
				for (int i = 0; i < 8; i++)
				{
					if (currRace == i)
						StringUtil.append(skillHTML, "<tr><td width=200>", getRaceName(i) + " (current race)", "</td></tr>");
					else
						StringUtil.append(skillHTML, "<tr><td width=200><a action=\"bypass -h pot_race_change ", String.valueOf(i), "\"><font color=LEVEL>", getRaceName(i), "</font></a></td></tr>");
					skillHTML.append("<br><tr><td><br></td></tr>");
				}
				skillHTML.append("</table></center>");
				itemReply.replace("%dtn%", skillHTML.toString());
				activeChar.sendPacket(itemReply);
				break;
			}
			case 11: // +15 skill
			{
				String filename = "data/html/custom/Donate/skill15.htm";
				final StringBuilder skillHTML = StringUtil.startAppend(1000, "");
				skillHTML.append("<center><table>");
				int counter = 0;
				for (L2Skill skill : activeChar.getAllSkills())
				{
					if (skill != null)
					{
						final int displayedLevel = skill.getLevel() % 100;
						if (skill.getLevel() >= 101 && displayedLevel < 15)
						{
							StringUtil.append(skillHTML, "<tr><td width=200><a action=\"bypass -h pot_skill_15 ", String.valueOf(skill.getId()), "\">", "+" + displayedLevel + " " + skill.getName(), "</a></td></tr>");
							counter++;
						}
					}
				}
				skillHTML.append("</table></center>");
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				if (counter <= 0)
				{
					itemReply.replace("%dtn%", "You don't have any skills that can be enchanted +15." + " Remember that you must have a skill enchanted to at least +1 if you want it to appear in the list. Buy some Giant's Codex.");
				}
				else
				{
					itemReply.replace("%dtn%", skillHTML.toString());
				}
				activeChar.sendPacket(itemReply);
				break;
			}
			case 13: // weapon exchanger
			{
				final L2ItemInstance oldWep = activeChar.getActiveWeaponInstance();
				if (oldWep == null)
				{
					activeChar.sendMessage("You don't have a weapon equipped; You can only change the weapon that you have equipped");
					return;
				}
				boolean canExchange = seeIfCanExchangeWeapon(item, oldWep);
				FastList<Integer> list = null;
				if (canExchange)
				{
					list = ItemLists.getInstance().getFirstListByItemId(oldWep.getItemId());
					if (list == null)
						canExchange = false;
				}
				if (!canExchange)
				{
					activeChar.sendMessage("Your equipped weapon cannot be exchanged at the moment");
					return;
				}
				final int enchantLevel = oldWep.getEnchantLevel();
				final String originalWeaponName = "+" + enchantLevel + " " + oldWep.getName();
				String filename = "data/html/custom/Donate/weaponchange.htm";
				final StringBuilder weaponHTML = StringUtil.startAppend(1000, "");
				weaponHTML.append("<center><table>");
				int counter = 0;
				for (Integer itemId : list)
				{
					if (itemId > 0 && itemId != oldWep.getItemId())
					{
						final L2ItemInstance newDummyItem = ItemTable.getInstance().createDummyItem(itemId);
						if (newDummyItem != null)
						{
							String itemName = "+" + enchantLevel + " " + newDummyItem.getName();
							StringUtil.append(weaponHTML, "<tr><td width=200><a action=\"bypass -h pot_weapon_exchange ", String.valueOf(itemId), " ", String.valueOf(item.getObjectId()), "\">", itemName, "</a></td></tr>");
							counter++;
						}
					}
				}
				weaponHTML.append("</table></center>");
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				if (counter <= 0)
				{
					itemReply.replace("%dtn%", "There are no exchangeable weapons for your weapon");
				}
				else
				{
					itemReply.replace("%gcp%", originalWeaponName.toString());
					itemReply.replace("%dtn%", weaponHTML.toString());
				}
				activeChar.sendPacket(itemReply);
				break;
			}
			case 14: // armor exchanger
			{
				byte dreadTit = 0;
				String originalArmorName;
				if (activeChar.getSkillLevel(8284) > 0) // heavy
				{
					dreadTit = 1;
					originalArmorName = "Titanium Heavy Set";
				}
				else if (activeChar.getSkillLevel(8286) > 0) // light
				{
					dreadTit = 2;
					originalArmorName = "Titanium Light Set";
				}
				else if (activeChar.getSkillLevel(8288) > 0) // robe
				{
					dreadTit = 3;
					originalArmorName = "Titanium Robe Set";
				}
				else if (activeChar.getSkillLevel(13000) > 0) // heavy
				{
					dreadTit = 4;
					originalArmorName = "Dread Heavy Set";
				}
				else if (activeChar.getSkillLevel(13001) > 0) // light
				{
					dreadTit = 5;
					originalArmorName = "Dread Light Set";
				}
				else if (activeChar.getSkillLevel(13002) > 0) // robe
				{
					dreadTit = 6;
					originalArmorName = "Dread Robe Set";
				}
				else
				{
					activeChar.sendMessage("Your armor set cannot be exchanged at the moment");
					return;
				}
				if (!seeIfCanExchangeArmor(item, dreadTit))
				{
					activeChar.sendMessage("Your armor set cannot be exchanged with this item");
					return;
				}
				String filename = "data/html/custom/Donate/armorchange.htm";
				final StringBuilder armorHTML = StringUtil.startAppend(1000, "");
				armorHTML.append("<center><table>");
				int limit = dreadTit < 4 ? 4 : 7;
				int start = dreadTit < 4 ? 1 : 4;
				for (int i = start; i < limit; i++)
				{
					if (i != dreadTit)
					{
						String newArmorSet;
						switch (i)
						{
							case 1:
								newArmorSet = "Titanium Heavy Set";
								break;
							case 2:
								newArmorSet = "Titanium Light Set";
								break;
							case 3:
								newArmorSet = "Titanium Robe Set";
								break;
							case 4:
								newArmorSet = "Dread Heavy Set";
								break;
							case 5:
								newArmorSet = "Dread Light Set";
								break;
							case 6:
								newArmorSet = "Dread Robe Set";
								break;
							default:
							{
								_log.warning(activeChar.getName() + " sent armor exchange function and used the wrong exchanger item (1)");
								activeChar.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
								return;
							}
						}
						StringUtil.append(armorHTML, "<tr><td width=200><a action=\"bypass -h pot_armor_exchange ", String.valueOf(i), " ", String.valueOf(item.getObjectId()), "\">", newArmorSet, "</a></td></tr><tr></tr><br><tr></tr><br>");
					}
				}
				armorHTML.append("</table></center>");
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%gcp%", originalArmorName.toString());
				itemReply.replace("%dtn%", armorHTML.toString());
				activeChar.sendPacket(itemReply);
				break;
			}
			case 20: // free tradechat
			{
				if (!activeChar._bypassTradeChat)
				{
					activeChar._bypassTradeChat = true;
					activeChar.sendMessage("You can do a trade chat (+) now");
					destroyItem = true;
				}
				else
				{
					activeChar.sendMessage("You already can do a free trade chat");
					return;
				}
				break;
			}
			case 21: // free shout
			{
				if (!activeChar._bypassShout)
				{
					activeChar._bypassShout = true;
					activeChar.sendMessage("You can do a shout (!) now");
					destroyItem = true;
				}
				else
				{
					activeChar.sendMessage("You already can do a free shout");
					return;
				}
				break;
			}
			case 22:
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				if (activeChar.isHero())
				{
					activeChar.sendMessage("You are already a hero.");
					return;
				}
				if (activeChar.isDead())
				{
					activeChar.sendMessage("Cannot use while you're dead");
					return;
				}
				activeChar.giveHeroSkills();
				activeChar.setFakeHero(true);
				activeChar.sendMessage("You are now a hero.");
				activeChar.broadcastUserInfo();
				destroyItem = true;
				break;
			case 23:
				if (activeChar.isNoble())
				{
					activeChar.sendMessage("You already are a Noblesse!");
					return;
				}
				activeChar.setNoble(true);
				activeChar.sendMessage("You are now Noblesse!");
				activeChar.broadcastUserInfo();
				destroyItem = true;
				break;
			case 24:
				if (activeChar.isDead())
				{
					activeChar.sendMessage("Cannot use while you're dead");
					return;
				}
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}
				if (activeChar.isTransformed())
				{
					activeChar.sendMessage("Cannot do this while transformed");
					return;
				}
				activeChar.sendPacket(new ExShowVariationMakeWindow());
				activeChar.sendPacket(new ExShowVariationCancelWindow());
				destroyItem = false;
				break;
			case 25:
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}
				if (activeChar.isDead())
				{
					activeChar.sendMessage("Cannot use while you're dead");
					return;
				}
				SkillTable.getInstance().getInfo(37509, 1).getEffects(activeChar, activeChar);
				destroyItem = true;
				break;
			case 26:
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}
				if (activeChar.isDead())
				{
					activeChar.sendMessage("Cannot use while you're dead");
					return;
				}
				SkillTable.getInstance().getInfo(37511, 1).getEffects(activeChar, activeChar);
				destroyItem = true;
				break;
			case 27:
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}
				if (activeChar.isDead())
				{
					activeChar.sendMessage("Cannot use while you're dead");
					return;
				}
				SkillTable.getInstance().getInfo(37512, 1).getEffects(activeChar, activeChar);
				destroyItem = true;
				break;
			case 28:
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}
				if (activeChar.isDead())
				{
					activeChar.sendMessage("Cannot use while you're dead");
					return;
				}
				SkillTable.getInstance().getInfo(37513, 1).getEffects(activeChar, activeChar);
				destroyItem = true;
				break;
			case 29:
				alekos = Rnd.nextInt(1000000);
				if (activeChar.getInventory().getInventoryItemCount(62006, 0) < 1L)
				{
					activeChar.sendMessage("You need a Gift Key to open this box");
					return;
				}
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				activeChar.destroyItemByItemId("Consume", 62006, 1L, activeChar, true);
				if (alekos < 2000)
				{
					activeChar.addItem("Silver", 4357, Rnd.get(2500, 6000), activeChar, true);
					activeChar.addItem("Demon eye", 97003, Rnd.get(1000, 2000), activeChar, true);
					activeChar.addItem("Festival Adena", 6673, Rnd.get(10, 35), activeChar, true);
					activeChar.addItem("Event Medal", 6392, Rnd.get(3, 6), activeChar, true);
					activeChar.addItem("GCM", 9627, Rnd.get(2, 3), activeChar, true);
					activeChar.addItem("BEAS", 6578, 3L, activeChar, true);
					activeChar.addItem("BEWS", 6577, 3L, activeChar, true);
					activeChar.addItem("Gold", 4356, 2L, activeChar, true);
					activeChar.addItem("Exp Buff", 62002, 2L, activeChar, true);
					destroyItem = true;
					break;
				}
				if (alekos < 5000)
				{
					activeChar.addItem("Silver", 4357, Rnd.get(2500, 6000), activeChar, true);
					activeChar.addItem("Demon eye", 97003, Rnd.get(1000, 2000), activeChar, true);
					activeChar.addItem("Festival Adena", 6673, Rnd.get(10, 35), activeChar, true);
					activeChar.addItem("Event Medal", 6392, Rnd.get(3, 6), activeChar, true);
					activeChar.addItem("GCM", 9627, Rnd.get(2, 3), activeChar, true);
					activeChar.addItem("BEAS", 6578, 3L, activeChar, true);
					activeChar.addItem("BEWS", 6577, 2L, activeChar, true);
					activeChar.addItem("Gold", 4356, 1L, activeChar, true);
					activeChar.addItem("Exp Buff", 62002, 1L, activeChar, true);
					destroyItem = true;
					break;
				}
				if (alekos < 10000)
				{
					activeChar.addItem("Silver", 4357, Rnd.get(2500, 6000), activeChar, true);
					activeChar.addItem("Demon eye", 97003, Rnd.get(1000, 2000), activeChar, true);
					activeChar.addItem("Festival Adena", 6673, Rnd.get(10, 35), activeChar, true);
					activeChar.addItem("Event Medal", 6392, Rnd.get(3, 6), activeChar, true);
					activeChar.addItem("GCM", 9627, Rnd.get(2, 3), activeChar, true);
					activeChar.addItem("BEAS", 6578, 2L, activeChar, true);
					activeChar.addItem("BEWS", 6577, 2L, activeChar, true);
					activeChar.addItem("Gold", 4356, 1L, activeChar, true);
					activeChar.addItem("Exp Buff", 62002, 1L, activeChar, true);
					destroyItem = true;
					break;
				}
				if (alekos < 20000)
				{
					activeChar.addItem("Silver", 4357, Rnd.get(2500, 6000), activeChar, true);
					activeChar.addItem("Demon eye", 97003, Rnd.get(1000, 2000), activeChar, true);
					activeChar.addItem("Festival Adena", 6673, Rnd.get(10, 35), activeChar, true);
					activeChar.addItem("Event Medal", 6392, Rnd.get(3, 6), activeChar, true);
					activeChar.addItem("GCM", 9627, Rnd.get(2, 3), activeChar, true);
					activeChar.addItem("BEAS", 6578, 2L, activeChar, true);
					activeChar.addItem("BEWS", 6577, 1L, activeChar, true);
					activeChar.addItem("Gold", 4356, 1L, activeChar, true);
					activeChar.addItem("Exp Buff", 62002, 1L, activeChar, true);
					destroyItem = true;
					break;
				}
				if (alekos <= 50000)
				{
					activeChar.addItem("Silver", 4357, Rnd.get(2500, 6000), activeChar, true);
					activeChar.addItem("Demon eye", 97003, Rnd.get(1000, 2000), activeChar, true);
					activeChar.addItem("Festival Adena", 6673, Rnd.get(10, 35), activeChar, true);
					activeChar.addItem("Event Medal", 6392, Rnd.get(3, 6), activeChar, true);
					activeChar.addItem("GCM", 9627, Rnd.get(2, 3), activeChar, true);
					activeChar.addItem("BEAS", 6578, 1L, activeChar, true);
					activeChar.addItem("BEWS", 6577, 1L, activeChar, true);
					activeChar.addItem("Gold", 4356, 1L, activeChar, true);
					activeChar.addItem("Exp Buff", 62002, 1L, activeChar, true);
					destroyItem = true;
					break;
				}
				if (alekos <= 100000)
				{
					activeChar.addItem("Silver", 4357, Rnd.get(2500, 6000), activeChar, true);
					activeChar.addItem("Demon eye", 97003, Rnd.get(1000, 2000), activeChar, true);
					activeChar.addItem("Festival Adena", 6673, Rnd.get(10, 35), activeChar, true);
					activeChar.addItem("Event Medal", 6392, Rnd.get(3, 6), activeChar, true);
					activeChar.addItem("GCM", 9627, Rnd.get(2, 3), activeChar, true);
					activeChar.addItem("BEAS", 6578, 1L, activeChar, true);
					activeChar.addItem("Gold", 4356, 1L, activeChar, true);
					activeChar.addItem("Exp Buff", 62002, 1L, activeChar, true);
					destroyItem = true;
					break;
				}
				if (alekos <= 180000)
				{
					activeChar.addItem("Silver", 4357, Rnd.get(2500, 6000), activeChar, true);
					activeChar.addItem("Demon eye", 97003, Rnd.get(1000, 2000), activeChar, true);
					activeChar.addItem("Festival Adena", 6673, Rnd.get(10, 35), activeChar, true);
					activeChar.addItem("Event Medal", 6392, Rnd.get(3, 6), activeChar, true);
					activeChar.addItem("GCM", 9627, Rnd.get(2, 3), activeChar, true);
					activeChar.addItem("Exp Buff", 62002, 1L, activeChar, true);
					destroyItem = true;
					break;
				}
				if (alekos <= 400000)
				{
					activeChar.addItem("Silver", 4357, Rnd.get(2500, 6000), activeChar, true);
					activeChar.addItem("Demon eye", 97003, Rnd.get(1000, 2000), activeChar, true);
					activeChar.addItem("Festival Adena", 6673, Rnd.get(10, 35), activeChar, true);
					activeChar.addItem("Event Medal", 6392, Rnd.get(3, 6), activeChar, true);
					activeChar.addItem("GCM", 9627, Rnd.get(2, 3), activeChar, true);
					activeChar.addItem("Exp Buff", 62002, 1L, activeChar, true);
					destroyItem = true;
					break;
				}
				if (alekos <= 1000000)
				{
					activeChar.addItem("Silver", 4357, Rnd.get(1000, 5000), activeChar, true);
					activeChar.addItem("Demon eye", 97003, Rnd.get(500, 1500), activeChar, true);
					activeChar.addItem("Festival Adena", 6673, Rnd.get(5, 25), activeChar, true);
					activeChar.addItem("Event Medal", 6392, Rnd.get(2, 6), activeChar, true);
					activeChar.addItem("GCM", 9627, Rnd.get(1, 3), activeChar, true);
					destroyItem = true;
					break;
				}
				destroyItem = true;
				break;
			case 30:
				if (activeChar.getInventory().getInventoryItemCount(62008, 0) < 1L)
				{
					activeChar.sendMessage("You need an Exclusive Gift Key to open this box");
					return;
				}
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				activeChar.destroyItemByItemId("Consume", 62008, 1L, activeChar, true);
				activeChar.addItem("Exp Buff", 62002, Rnd.get(1, 3), activeChar, true);
				activeChar.addItem("Pve Dmg Buff", 62000, Rnd.get(1, 2), activeChar, true);
				activeChar.addItem("Pve Def Buff", 62001, Rnd.get(1, 2), activeChar, true);
				activeChar.addItem("Festival Adena", 6673, Rnd.get(10, 100), activeChar, true);
				activeChar.addItem("Event Medal", 6392, Rnd.get(5, 15), activeChar, true);
				activeChar.addItem("GCM", 9627, Rnd.get(2, 10), activeChar, true);
				activeChar.addItem("BEAS", 6578, Rnd.get(1, 5), activeChar, true);
				activeChar.addItem("BEWS", 6577, Rnd.get(1, 4), activeChar, true);
				activeChar.addItem("Gold", 4356, Rnd.get(1, 2), activeChar, true);
				if (random.nextInt(100) < 25)
					activeChar.addItem("Valyrian Scroll", 99956, Rnd.get(1, 2), activeChar, true);
				if (random.nextInt(100) < 50)
					activeChar.addItem("TEWS", 99950, Rnd.get(1, 3), activeChar, true);
				if (random.nextInt(100) < 50)
					activeChar.addItem("TEAS", 99951, Rnd.get(1, 5), activeChar, true);
				if (random.nextInt(100) < 20)
					activeChar.addItem("GTEAS", 99953, Rnd.get(1, 2), activeChar, true);
				if (random.nextInt(100) < 20)
					activeChar.addItem("GTEWS", 99952, 1L, activeChar, true);
				if (random.nextInt(100) < 5)
					activeChar.addItem("Glittering Medal", 6393, 1L, activeChar, true);
				if (random.nextInt(100) < 4)
				{
					activeChar.addItem("Rebirth", 99986, 1L, activeChar, true);
				}
				destroyItem = true;
				break;
			case 31:
				alekos1 = random.nextInt(3);
				switch (alekos1)
				{
					case 0:
						spawnMonster(activeChar, "110069", 0, 1, false, 0, 0);
						break;
					case 1:
						spawnMonster(activeChar, "110030", 0, 1, false, 0, 0);
						break;
					case 2:
						spawnMonster(activeChar, "110047", 0, 1, false, 0, 0);
						break;
				}
				destroyItem = true;
				break;
			case 32:
				alekaras = random.nextInt(6);
				enhanced = random.nextInt(20);
				switch (alekaras)
				{
					case 0:
						activeChar.addItem("Draconic Leather Olympiad Armor", 82000, 1L, activeChar, true);
						activeChar.addItem("Draconic Leather Olympiad Gloves", 82001, 1L, activeChar, true);
						activeChar.addItem("Draconic Leather Olympiad Boots", 82002, 1L, activeChar, true);
						activeChar.addItem("Draconic Leather Olympiad Helmet", 82003, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Draconic Leather Olympiad Armor Enhanced", 82503, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Draconic Leather Olympiad Gloves Enhanced", 82504, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Draconic Leather Olympiad Boots Enhanced", 82505, 1L, activeChar, true);
						}
						break;
					case 1:
						activeChar.addItem("Apella Olympiad Helmet", 82013, 1L, activeChar, true);
						activeChar.addItem("Apella Brigandine Olympiad", 82017, 1L, activeChar, true);
						activeChar.addItem("Apella Leather Olympiad Gloves", 82018, 1L, activeChar, true);
						activeChar.addItem("Apella Leather Olympiad Boots", 82019, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Apella Brigandine Olympiad Enhanced", 82512, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Apella Leather Olympiad Gloves Enhanced", 82513, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Apella Leather Olympiad Boots Enhanced", 82514, 1L, activeChar, true);
						}
						break;
					case 2:
						activeChar.addItem("Dark Crystal Olympiad Helmet", 82023, 1L, activeChar, true);
						activeChar.addItem("Dark Crystal Olympiad Leather Armor", 82028, 1L, activeChar, true);
						activeChar.addItem("Dark Crystal Olympiad Leggings", 82029, 1L, activeChar, true);
						activeChar.addItem("Dark Crystal Olympiad Leather Gloves", 82030, 1L, activeChar, true);
						activeChar.addItem("Dark Crystal Olympiad Leather Boots", 82031, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Dark Crystal Olympiad Leather Armor Enhanced", 82521, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Dark Crystal Olympiad Leather Gloves Enhanced", 82522, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Dark Crystal Olympiad Leather Boots Enhanced", 82523, 1L, activeChar, true);
						}
						break;
					case 3:
						activeChar.addItem("Majestic Olympiad Helmet", 82035, 1L, activeChar, true);
						activeChar.addItem("Majestic Olympiad Leather Armor", 82039, 1L, activeChar, true);
						activeChar.addItem("Majestic Olympiad Leather Gloves", 82040, 1L, activeChar, true);
						activeChar.addItem("Majestic Olympiad Leather Boots", 82041, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Majestic Olympiad Leather Armor Enhanced", 82530, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Majestic Olympiad Leather Gloves Enhanced", 82531, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Majestic Olympiad Leather Boots Enhanced", 82532, 1L, activeChar, true);
						}
						break;
					case 4:
						activeChar.addItem("Tallum Olympiad Helmet", 82045, 1L, activeChar, true);
						activeChar.addItem("Tallum Olympiad Leather Armor", 82049, 1L, activeChar, true);
						activeChar.addItem("Tallum Olympiad Leather Gloves", 82050, 1L, activeChar, true);
						activeChar.addItem("Tallum Olympiad Leather Boots", 82051, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Tallum Olympiad Leather Armor Enhanced", 82539, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Tallum Olympiad Leather Gloves Enhanced", 82540, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Tallum Olympiad Leather Boots Enhanced", 82541, 1L, activeChar, true);
						}
						break;
					case 5:
						activeChar.addItem("Nightmare Olympiad Helmet", 82056, 1L, activeChar, true);
						activeChar.addItem("Nightmare Olympiad Leather Armor", 82060, 1L, activeChar, true);
						activeChar.addItem("Nightmare Olympiad Leather Gloves", 82061, 1L, activeChar, true);
						activeChar.addItem("Nightmare Olympiad Leather Boots", 82062, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Nightmare Olympiad Leather Armor Enhanced", 82548, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Nightmare Olympiad Leather Gloves Enhanced", 82549, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Nightmare Olympiad Leather Boots Enhanced", 82550, 1L, activeChar, true);
						}
						break;
				}
				destroyItem = true;
				break;
			case 33:
				alekaras = random.nextInt(6);
				enhanced = random.nextInt(20);
				switch (alekaras)
				{
					case 0:
						activeChar.addItem("Imperial Crusader Olympiad Breastplate", 82004, 1L, activeChar, true);
						activeChar.addItem("Imperial Crusader Olympiad Gaiters", 82005, 1L, activeChar, true);
						activeChar.addItem("Imperial Crusader Olympiad Gauntlets", 82006, 1L, activeChar, true);
						activeChar.addItem("Imperial Crusader Olympiad Boots", 82007, 1L, activeChar, true);
						activeChar.addItem("Imperial Crusader Olympiad Helmet", 82008, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Imperial Crusader Olympiad Breastplate Enhanced", 82500, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Imperial Crusader Olympiad Gauntlets Enhanced", 82501, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Imperial Crusader Olympiad Boots Enhanced", 82502, 1L, activeChar, true);
						}
						break;
					case 1:
						activeChar.addItem("Apella Olympiad Helmet", 82013, 1L, activeChar, true);
						activeChar.addItem("Apella Olympiad Plate Armor", 82014, 1L, activeChar, true);
						activeChar.addItem("Apella Olympiad Gauntlets", 82015, 1L, activeChar, true);
						activeChar.addItem("Apella Olympiad Boots", 82016, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Apella Olympiad Plate Armor Enhanced", 82509, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Apella Olympiad Gauntlets Enhanced", 82510, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Apella Olympiad Boots Enhanced", 82511, 1L, activeChar, true);
						}
						break;
					case 2:
						activeChar.addItem("Dark Crystal Olympiad Helmet", 82023, 1L, activeChar, true);
						activeChar.addItem("Dark Crystal Olympiad Breastplate", 82024, 1L, activeChar, true);
						activeChar.addItem("Dark Crystal Olympiad Gaiters", 82025, 1L, activeChar, true);
						activeChar.addItem("Dark Crystal Olympiad Gloves", 82026, 1L, activeChar, true);
						activeChar.addItem("Dark Crystal Olympiad Boots", 82027, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Dark Crystal Olympiad Breastplate Enhanced", 82518, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Dark Crystal Olympiad Gloves Enhanced", 82519, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Dark Crystal Olympiad Boots Enhanced", 82520, 1L, activeChar, true);
						}
						break;
					case 3:
						activeChar.addItem("Majestic Olympiad Helmet", 82035, 1L, activeChar, true);
						activeChar.addItem("Majestic Olympiad Plate Armor", 82036, 1L, activeChar, true);
						activeChar.addItem("Majestic Olympiad Gauntlets", 82037, 1L, activeChar, true);
						activeChar.addItem("Majestic Olympiad Boots", 82038, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Majestic Olympiad Plate Armor Enhanced", 82527, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Majestic Olympiad Gauntlets Enhanced", 82528, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Majestic Olympiad Boots Enhanced", 82529, 1L, activeChar, true);
						}
						break;
					case 4:
						activeChar.addItem("Tallum Olympiad Helmet", 82045, 1L, activeChar, true);
						activeChar.addItem("Tallum Olympiad Plate Armor", 82046, 1L, activeChar, true);
						activeChar.addItem("Tallum Olympiad Gloves", 82047, 1L, activeChar, true);
						activeChar.addItem("Tallum Olympiad Boots", 82048, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Tallum Olympiad Plate Armor Enhanced", 82536, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Tallum Olympiad Gloves Enhanced", 82537, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Tallum Olympiad Boots Enhanced", 82538, 1L, activeChar, true);
						}
						break;
					case 5:
						activeChar.addItem("Nightmare Olympiad Helmet", 82056, 1L, activeChar, true);
						activeChar.addItem("Nightmare Olympiad Heavy Armor", 82057, 1L, activeChar, true);
						activeChar.addItem("Nightmare Olympiad Gloves", 82058, 1L, activeChar, true);
						activeChar.addItem("Nightmare Olympiad Boots", 82059, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Nightmare Olympiad Heavy Armor Enhanced", 82545, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Nightmare Olympiad Gloves Enhanced", 82546, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Nightmare Olympiad Boots Enhanced", 82547, 1L, activeChar, true);
						}
						break;
				}
				destroyItem = true;
				break;
			case 34:
				alekaras = random.nextInt(6);
				enhanced = random.nextInt(20);
				switch (alekaras)
				{
					case 0:
						activeChar.addItem("Major Arcana Olympiad Robe", 82009, 1L, activeChar, true);
						activeChar.addItem("Major Arcana Olympiad Gloves", 82010, 1L, activeChar, true);
						activeChar.addItem("Major Arcana Olympiad Boots", 82011, 1L, activeChar, true);
						activeChar.addItem("Major Arcana Olympiad Circlet", 82012, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Major Arcana Olympiad Robe Enhanced", 82506, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Major Arcana Olympiad Gloves Enhanced", 82507, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Major Arcana Olympiad Boots Enhanced", 82508, 1L, activeChar, true);
						}
						break;
					case 1:
						activeChar.addItem("Apella Olympiad Helmet", 82013, 1L, activeChar, true);
						activeChar.addItem("Apella Olympiad Robe", 82020, 1L, activeChar, true);
						activeChar.addItem("Apella Olympiad Mittens", 82021, 1L, activeChar, true);
						activeChar.addItem("Apella Olympiad Sandals", 82022, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Apella Olympiad Robe Enhanced", 82515, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Apella Olympiad Mittens Enhanced", 82516, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Apella Olympiad Sandals Enhanced", 82517, 1L, activeChar, true);
						}
						break;
					case 2:
						activeChar.addItem("Dark Crystal Olympiad Helmet", 82023, 1L, activeChar, true);
						activeChar.addItem("Dark Crystal Olympiad Robe", 82032, 1L, activeChar, true);
						activeChar.addItem("Dark Crystal Olympiad Mittens", 82033, 1L, activeChar, true);
						activeChar.addItem("Dark Crystal Olympiad Sandals", 82034, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Dark Crystal Olympiad Robe Enhanced", 82524, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Dark Crystal Olympiad Mittens Enhanced", 82525, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Dark Crystal Olympiad Sandals", 82526, 1L, activeChar, true);
						}
						break;
					case 3:
						activeChar.addItem("Majestic Olympiad Helmet", 82035, 1L, activeChar, true);
						activeChar.addItem("Majestic Olympiad Robe", 82042, 1L, activeChar, true);
						activeChar.addItem("Majestic Olympiad Mittens", 82043, 1L, activeChar, true);
						activeChar.addItem("Majestic Olympiad Sandals", 82044, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Majestic Olympiad Robe Enhanced", 82533, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Majestic Olympiad Mittens Enhanced", 82534, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Majestic Olympiad Sandals", 82535, 1L, activeChar, true);
						}
						break;
					case 4:
						activeChar.addItem("Tallum Olympiad Helmet", 82045, 1L, activeChar, true);
						activeChar.addItem("Tallum Olympiad Tunic", 82052, 1L, activeChar, true);
						activeChar.addItem("Tallum Olympiad Stockings", 82053, 1L, activeChar, true);
						activeChar.addItem("Tallum Olympiad Mittens", 82054, 1L, activeChar, true);
						activeChar.addItem("Tallum Olympiad Sandals", 82055, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Tallum Olympiad Tunic Enhanced", 82542, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Tallum Olympiad Mittens Enhanced", 82543, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Tallum Olympiad Sandals", 82544, 1L, activeChar, true);
						}
						break;
					case 5:
						activeChar.addItem("Nightmare Olympiad Helmet", 82056, 1L, activeChar, true);
						activeChar.addItem("Nightmare Olympiad Robe", 82063, 1L, activeChar, true);
						activeChar.addItem("Nightmare Olympiad Mittens", 82064, 1L, activeChar, true);
						activeChar.addItem("Nightmare Olympiad Sandals", 82065, 1L, activeChar, true);
						if (enhanced == 0)
							activeChar.addItem("Nightmare Olympiad Robe Enhanced", 82551, 1L, activeChar, true);
						if (enhanced == 1)
							activeChar.addItem("Nightmare Olympiad Mittens Enhanced", 82552, 1L, activeChar, true);
						if (enhanced == 2)
						{
							activeChar.addItem("Nightmare Olympiad Sandals Enhanced", 82553, 1L, activeChar, true);
						}
						break;
				}
				destroyItem = true;
				break;
			case 35:
				activeChar.addItem("Forgotten Blade - Focus", 6583, 1L, activeChar, false, 20);
				activeChar.addItem("Draconic Bow - Focus", 7577, 1L, activeChar, false, 20);
				activeChar.addItem("Heavens Divider - Focus", 6607, 1L, activeChar, false, 20);
				activeChar.addItem("Angel Slayer - Crit Dmg", 6590, 1L, activeChar, false, 20);
				activeChar.addItem("Basalt Battlehammer - Health", 6585, 1L, activeChar, false, 20);
				activeChar.addItem("Saint Spear - Haste", 6601, 1L, activeChar, false, 20);
				activeChar.addItem("Demon Splinter - Focus", 6602, 1L, activeChar, false, 20);
				activeChar.addItem("Laevateinn - Focus", 9367, 1L, activeChar, false, 20);
				activeChar.addItem("Gram - Focus", 9371, 1L, activeChar, false, 20);
				activeChar.addItem("Sarunga - Earth - Focus", 11216, 1L, activeChar, false, 20);
				activeChar.addItem("Tallum Blade*Dark Legion's Edge", 6580, 1L, activeChar, false, 20);
				activeChar.addItem("Imperial Crusader Breastplate", 6373, 1L, activeChar, false, 18);
				activeChar.addItem("Imperial Crusader Gaiters", 6374, 1L, activeChar, false, 18);
				activeChar.addItem("Imperial Crusader Gauntlets", 6375, 1L, activeChar, false, 18);
				activeChar.addItem("Imperial Crusader Boots", 6376, 1L, activeChar, false, 18);
				activeChar.addItem("Imperial Crusader Helmet", 6378, 1L, activeChar, false, 18);
				activeChar.addItem("Draconic Leather Armor", 6379, 1L, activeChar, false, 18);
				activeChar.addItem("Draconic Leather Gloves", 6380, 1L, activeChar, false, 18);
				activeChar.addItem("Draconic Leather Boots", 6381, 1L, activeChar, false, 18);
				activeChar.addItem("Draconic Leather Helmet", 6382, 1L, activeChar, false, 18);
				destroyItem = true;
				break;
			case 36:
				activeChar.addItem("Arcana Mace - Acumen", 6608, 1L, activeChar, false, 20);
				activeChar.addItem("Imperial Staff - Empower", 6587, 1L, activeChar, false, 20);
				activeChar.addItem("Major Arcana Robe", 6383, 1L, activeChar, false, 18);
				activeChar.addItem("Major Arcana Gloves", 6384, 1L, activeChar, false, 18);
				activeChar.addItem("Major Arcana Boots", 6385, 1L, activeChar, false, 18);
				activeChar.addItem("Major Arcana Circlet", 6386, 1L, activeChar, false, 18);
				activeChar.addItem("Dark Crystal Robe", 2407, 1L, activeChar, false, 18);
				activeChar.addItem("Dark Crystal Helmet", 512, 1L, activeChar, false, 18);
				activeChar.addItem("Dark Crystal Gloves - Robe", 5767, 1L, activeChar, false, 18);
				activeChar.addItem("Dark Crystal Boots - Robe", 5779, 1L, activeChar, false, 18);
				destroyItem = true;
				break;
			case 37:
				activeChar.addItem("Necklace of Valakas", 6657, 1L, activeChar, false, 16);
				activeChar.addItem("Frintezza's Necklace", 8191, 1L, activeChar, false, 16);
				activeChar.addItem("Earring of Antharas", 6656, 1L, activeChar, false, 16);
				activeChar.addItem("Ring of Queen Ant", 6660, 1L, activeChar, false, 16);
				activeChar.addItem("Earring of Orfen", 6661, 1L, activeChar, false, 16);
				activeChar.addItem("Zaken's Earring", 6659, 1L, activeChar, false, 16);
				activeChar.addItem("Ring of Core", 6662, 1L, activeChar, false, 16);
				activeChar.addItem("Ring of Baium", 6658, 1L, activeChar, false, 16);
				destroyItem = true;
				break;
			case 38:
				enhanced = random.nextInt(54);
				if (enhanced == 0)
					activeChar.addItem("Draconic Leather Olympiad Armor Enhanced", 82503, 1L, activeChar, true);
				if (enhanced == 1)
					activeChar.addItem("Draconic Leather Olympiad Gloves Enhanced", 82504, 1L, activeChar, true);
				if (enhanced == 2)
				{
					activeChar.addItem("Draconic Leather Olympiad Boots Enhanced", 82505, 1L, activeChar, true);
				}
				if (enhanced == 3)
					activeChar.addItem("Apella Brigandine Olympiad Enhanced", 82512, 1L, activeChar, true);
				if (enhanced == 4)
					activeChar.addItem("Apella Leather Olympiad Gloves Enhanced", 82513, 1L, activeChar, true);
				if (enhanced == 5)
				{
					activeChar.addItem("Apella Leather Olympiad Boots Enhanced", 82514, 1L, activeChar, true);
				}
				if (enhanced == 6)
					activeChar.addItem("Dark Crystal Olympiad Leather Armor Enhanced", 82521, 1L, activeChar, true);
				if (enhanced == 7)
					activeChar.addItem("Dark Crystal Olympiad Leather Gloves Enhanced", 82522, 1L, activeChar, true);
				if (enhanced == 8)
				{
					activeChar.addItem("Dark Crystal Olympiad Leather Boots Enhanced", 82523, 1L, activeChar, true);
				}
				if (enhanced == 9)
					activeChar.addItem("Majestic Olympiad Leather Armor Enhanced", 82530, 1L, activeChar, true);
				if (enhanced == 10)
					activeChar.addItem("Majestic Olympiad Leather Gloves Enhanced", 82531, 1L, activeChar, true);
				if (enhanced == 11)
				{
					activeChar.addItem("Majestic Olympiad Leather Boots Enhanced", 82532, 1L, activeChar, true);
				}
				if (enhanced == 12)
					activeChar.addItem("Tallum Olympiad Leather Armor Enhanced", 82539, 1L, activeChar, true);
				if (enhanced == 13)
					activeChar.addItem("Tallum Olympiad Leather Gloves Enhanced", 82540, 1L, activeChar, true);
				if (enhanced == 14)
				{
					activeChar.addItem("Tallum Olympiad Leather Boots Enhanced", 82541, 1L, activeChar, true);
				}
				if (enhanced == 15)
					activeChar.addItem("Nightmare Olympiad Leather Armor Enhanced", 82548, 1L, activeChar, true);
				if (enhanced == 16)
					activeChar.addItem("Nightmare Olympiad Leather Gloves Enhanced", 82549, 1L, activeChar, true);
				if (enhanced == 17)
				{
					activeChar.addItem("Nightmare Olympiad Leather Boots Enhanced", 82550, 1L, activeChar, true);
				}
				if (enhanced == 18)
					activeChar.addItem("Imperial Crusader Olympiad Breastplate Enhanced", 82500, 1L, activeChar, true);
				if (enhanced == 19)
					activeChar.addItem("Imperial Crusader Olympiad Gauntlets Enhanced", 82501, 1L, activeChar, true);
				if (enhanced == 20)
				{
					activeChar.addItem("Imperial Crusader Olympiad Boots Enhanced", 82502, 1L, activeChar, true);
				}
				if (enhanced == 21)
					activeChar.addItem("Apella Olympiad Plate Armor Enhanced", 82509, 1L, activeChar, true);
				if (enhanced == 22)
					activeChar.addItem("Apella Olympiad Gauntlets Enhanced", 82510, 1L, activeChar, true);
				if (enhanced == 23)
				{
					activeChar.addItem("Apella Olympiad Boots Enhanced", 82511, 1L, activeChar, true);
				}
				if (enhanced == 24)
					activeChar.addItem("Dark Crystal Olympiad Breastplate Enhanced", 82518, 1L, activeChar, true);
				if (enhanced == 25)
					activeChar.addItem("Dark Crystal Olympiad Gloves Enhanced", 82519, 1L, activeChar, true);
				if (enhanced == 26)
				{
					activeChar.addItem("Dark Crystal Olympiad Boots Enhanced", 82520, 1L, activeChar, true);
				}
				if (enhanced == 27)
					activeChar.addItem("Majestic Olympiad Plate Armor Enhanced", 82527, 1L, activeChar, true);
				if (enhanced == 28)
					activeChar.addItem("Majestic Olympiad Gauntlets Enhanced", 82528, 1L, activeChar, true);
				if (enhanced == 29)
				{
					activeChar.addItem("Majestic Olympiad Boots Enhanced", 82529, 1L, activeChar, true);
				}
				if (enhanced == 30)
					activeChar.addItem("Tallum Olympiad Plate Armor Enhanced", 82536, 1L, activeChar, true);
				if (enhanced == 31)
					activeChar.addItem("Tallum Olympiad Gloves Enhanced", 82537, 1L, activeChar, true);
				if (enhanced == 32)
				{
					activeChar.addItem("Tallum Olympiad Boots Enhanced", 82538, 1L, activeChar, true);
				}
				if (enhanced == 33)
					activeChar.addItem("Nightmare Olympiad Heavy Armor Enhanced", 82545, 1L, activeChar, true);
				if (enhanced == 34)
					activeChar.addItem("Nightmare Olympiad Gloves Enhanced", 82546, 1L, activeChar, true);
				if (enhanced == 35)
				{
					activeChar.addItem("Nightmare Olympiad Boots Enhanced", 82547, 1L, activeChar, true);
				}
				if (enhanced == 36)
					activeChar.addItem("Major Arcana Olympiad Robe Enhanced", 82506, 1L, activeChar, true);
				if (enhanced == 37)
					activeChar.addItem("Major Arcana Olympiad Gloves Enhanced", 82507, 1L, activeChar, true);
				if (enhanced == 38)
				{
					activeChar.addItem("Major Arcana Olympiad Boots Enhanced", 82508, 1L, activeChar, true);
				}
				if (enhanced == 39)
					activeChar.addItem("Apella Olympiad Robe Enhanced", 82515, 1L, activeChar, true);
				if (enhanced == 40)
					activeChar.addItem("Apella Olympiad Mittens Enhanced", 82516, 1L, activeChar, true);
				if (enhanced == 41)
				{
					activeChar.addItem("Apella Olympiad Sandals Enhanced", 82517, 1L, activeChar, true);
				}
				if (enhanced == 42)
					activeChar.addItem("Dark Crystal Olympiad Robe Enhanced", 82524, 1L, activeChar, true);
				if (enhanced == 43)
					activeChar.addItem("Dark Crystal Olympiad Mittens Enhanced", 82525, 1L, activeChar, true);
				if (enhanced == 44)
				{
					activeChar.addItem("Dark Crystal Olympiad Sandals", 82526, 1L, activeChar, true);
				}
				if (enhanced == 45)
					activeChar.addItem("Majestic Olympiad Robe Enhanced", 82533, 1L, activeChar, true);
				if (enhanced == 46)
					activeChar.addItem("Majestic Olympiad Mittens Enhanced", 82534, 1L, activeChar, true);
				if (enhanced == 47)
				{
					activeChar.addItem("Majestic Olympiad Sandals", 82535, 1L, activeChar, true);
				}
				if (enhanced == 48)
					activeChar.addItem("Tallum Olympiad Tunic Enhanced", 82542, 1L, activeChar, true);
				if (enhanced == 49)
					activeChar.addItem("Tallum Olympiad Mittens Enhanced", 82543, 1L, activeChar, true);
				if (enhanced == 50)
				{
					activeChar.addItem("Tallum Olympiad Sandals", 82544, 1L, activeChar, true);
				}
				if (enhanced == 51)
					activeChar.addItem("Nightmare Olympiad Robe Enhanced", 82551, 1L, activeChar, true);
				if (enhanced == 52)
					activeChar.addItem("Nightmare Olympiad Mittens Enhanced", 82552, 1L, activeChar, true);
				if (enhanced == 53)
				{
					activeChar.addItem("Nightmare Olympiad Sandals Enhanced", 82553, 1L, activeChar, true);
				}
				destroyItem = true;
				break;
			case 39:
				destroyItem = true;
				break;
			case 40:
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}
				if (activeChar.isDead())
				{
					activeChar.sendMessage("Cannot use while you're dead");
					return;
				}
				SkillTable.getInstance().getInfo(37512, 2).getEffects(activeChar, activeChar);
				destroyItem = true;
				break;
			case 41:
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}
				if (activeChar.isDead())
				{
					activeChar.sendMessage("Cannot use while you're dead");
					return;
				}
				SkillTable.getInstance().getInfo(37512, 3).getEffects(activeChar, activeChar);
				destroyItem = true;
				break;
			case 42:
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}
				if (activeChar.isDead())
				{
					activeChar.sendMessage("Cannot use while you're dead");
					return;
				}
				SkillTable.getInstance().getInfo(37513, 1).getEffects(activeChar, activeChar);
				destroyItem = true;
				break;
			case 43:
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}
				if (activeChar.isDead())
				{
					activeChar.sendMessage("Cannot use while you're dead");
					return;
				}
				SkillTable.getInstance().getInfo(37513, 2).getEffects(activeChar, activeChar);
				destroyItem = true;
				break;
			case 44:
				if (activeChar.isInOlympiadMode())
				{
					activeChar.sendMessage("This item cannot be used in olympiad games.");
					return;
				}
				if (activeChar.isInCombat())
				{
					activeChar.sendMessage("Cannot use while in combat");
					return;
				}
				if (activeChar.isDead())
				{
					activeChar.sendMessage("Cannot use while you're dead");
					return;
				}
				SkillTable.getInstance().getInfo(37513, 3).getEffects(activeChar, activeChar);
				destroyItem = true;
				break;
				
		}
		if (destroyItem)
		{
			if (activeChar.destroyItem("Donation", item, 1, activeChar, true))
			{
				activeChar.playSound("ItemSound.quest_finish");
				if (id != 20 && id != 21 && id != 22 && id != 23 && id != 24 && id != 25 && id != 26 && id != 27 && id != 28 && id != 29 && id != 30 && id != 31 && id != 32 && id != 33 && id != 34)
					activeChar.sendMessage("Thank you for helping our server!");
			}
			else
			{
				activeChar.setPunishLevel(PunishLevel.JAIL, 0);
				_log.warning(activeChar.getName() + " sent a donation item usage but it wasn't able to destroy the item!!!!!!!!! MUST BAN HIM NOW");
			}
		}
	}
	
	private static boolean seeIfCanExchangeArmor(L2ItemInstance item, byte dreadTit)
	{
		if (item != null)
		{
			switch (item.getItemId())
			{
				case ARMOR_SET_EXCHANGER_DREAD:
					if (dreadTit > 0 && dreadTit < 7)
						return true;
					break;
				case ARMOR_SET_EXCHANGER_TITANIUM:
					if (dreadTit > 0 && dreadTit < 4)
						return true;
					break;
				case ARMOR_SET_EXCHANGER_RYKROS:
					if (dreadTit > 0 && dreadTit < 10)
						return true;
					break;
			}
		}
		return false;
	}
	
	private static boolean seeIfCanExchangeWeapon(L2ItemInstance item, L2ItemInstance oldWep)
	{
		if (item != null)
		{
			switch (item.getItemId())
			{
				case WEAPON_EXCHANGE_DREAD:
					if (oldWep.isStandardShopItem())
						return true;
					else if (oldWep.getUniqueness() <= 3 && oldWep.getCrystalType() <= L2Item.CRYSTAL_S)
						return true;
					break;
				case WEAPON_EXCHANGE_TITANIUM:
					if (oldWep.isStandardShopItem())
					{
						if (oldWep.getUniqueness() <= 4)
							return true;
					}
					/*
					 * else
					 * {
					 * if (oldWep.getUniqueness() <= 3 && oldWep.getCrystalType() <= L2Item.CRYSTAL_S)
					 * return true;
					 * }
					 */
					break;
				case WEAPON_EXCHANGE_VESPER:
					if (oldWep.isStandardShopItem() && oldWep.getUniqueness() <= 3)
						return true;
					break;
				case WEAPON_EXCHANGE_EPIC:
					if (oldWep.getUniqueness() < 4.5)
						return true;
					break;
				case WEAPON_EXCHANGE_UNIQUE:
					if (oldWep.getUniqueness() < 4)
						return true;
					break;
			}
		}
		return false;
	}
	
	final public static void onBypass(final L2PcInstance player, String action)
	{
		if (!allowUse(player))
			return;
		if (action.startsWith("name_change "))
		{
			final String _name = action.substring(12);
			String errorMsg = null;
			boolean proceed = true;
			if (_name.length() < 2)
			{
				errorMsg = "Names have to be at least 2 characters";
				proceed = false;
			}
			if (_name.length() > 23)
			{
				errorMsg = "Names cannot be longer than 23 characters";
				proceed = false;
			}
			if (!Util.isAlphaNumeric(_name) || !CharacterCreate.isValidName(_name, true))
			{
				errorMsg = "Invalid name";
				proceed = false;
			}
			if (CharNameTable.getInstance().doesCharNameExist(_name))
			{
				if (!(player.getName().equalsIgnoreCase(_name) && !player.getName().equals(_name)))
				{
					errorMsg = "Name already exists";
					proceed = false;
				}
			}
			if (!proceed)
			{
				player.sendMessage(errorMsg);
				String filename = "data/html/custom/Donate/namechange.htm";
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
				return;
			}
			if (player.destroyItemByItemId("Donation Name Change", NAME_CHANGE_ITEMID, 1, player, true))
			{
				player.initiateNameChange(_name);
				player.playSound("ItemSound.quest_finish");
				player.sendMessage("Thank you for helping our server!");
			}
			else
			{
				_log.severe(player.getName() + " REQUESTED A NAME CHANGE W/O ACTUALLY ACTIVATING THE ITEM FIRST!!!!!!!");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
			}
		}
		else if (action.startsWith("race_change "))
		{
			int newRace = -1;
			try
			{
				newRace = Integer.parseInt(action.substring(12));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (newRace < 0 || newRace > 7)
			{
				_log.warning(player.getName() + " sent a race change request with a newRace value that is too low or too high0!!!!!!!!!!!!");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			if (player.getRace().ordinal() == newRace)
			{
				_log.warning(player.getName() + " sent a race change request with the same race !!!!!!!!!");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
//			if (newRace != 5) // player wants to change into non-kamael race
//			{
//				if (player.isKamaelBaseClassExceptDoombringer())
//				{
//					player.sendMessage("You can't change to a non-Kamael race when your base-class is a Kamael class that uses Rapiers or Crossbows (due to animation issues)");
//					return;
//				}
//			}
//			if (player.getBaseClass() == 114 || player.getBaseClass() == 48) // player's base class is a tyrant
//			{
//				if (newRace == 5) // that's trying to change to a kamael
//				{
//					player.sendMessage("When your base class is a Tyrant/GrandK, you cannot change your race into a Kamael");
//					return;
//				}
//			}
			if (player.destroyItemByItemId("Donation Race Change", RACE_CHANGE_ITEMID, 1, player, true))
			{
				int[] circlets =
				{
					0, 0
				};
				int[] circlets2 =
				{
					0, 0
				};
				switch (player.getRace().ordinal())
				{
					case 0:
						circlets[0] = 9391;
						circlets[1] = 9410;
						break;
					case 1:
						circlets[0] = 9392;
						circlets[1] = 9411;
						break;
					case 2:
						circlets[0] = 9393;
						circlets[1] = 9412;
						break;
					case 3:
						circlets[0] = 9394;
						circlets[1] = 9413;
						break;
					case 4:
						circlets[0] = 9395;
						circlets[1] = 9414;
						break;
					case 5:
						circlets[0] = 9396;
						circlets[1] = 9415;
						break;
					case 6:
						circlets[0] = 9391;
						circlets[1] = 9410;
						break;
					case 7:
						circlets[0] = 9394;
						circlets[1] = 9413;
						break;
				}
				switch (newRace)
				{
					case 0:
						circlets2[0] = 9391;
						circlets2[1] = 9410;
						break;
					case 1:
						circlets2[0] = 9392;
						circlets2[1] = 9411;
						break;
					case 2:
						circlets2[0] = 9393;
						circlets2[1] = 9412;
						break;
					case 3:
						circlets2[0] = 9394;
						circlets2[1] = 9413;
						break;
					case 4:
						circlets2[0] = 9395;
						circlets2[1] = 9414;
						break;
					case 5:
						circlets2[0] = 9396;
						circlets2[1] = 9415;
						break;
					case 6:
						circlets2[0] = 9391;
						circlets2[1] = 9410;
						break;
					case 7:
						circlets2[0] = 9394;
						circlets2[1] = 9413;
						break;
				}
				MagicSkillUse msk = new MagicSkillUse(player, player, 837, 1, 1000, 0);
				Broadcast.toSelfAndKnownPlayersInRadius(player, msk, 1210000);
				player.setRace(newRace);
				player.storeCharBase();
				player.broadcastUserInfo();
				player.playSound("ItemSound.quest_finish");
				player.sendMessage("Congratulations! Now you're a " + DonatePotion.getRaceName(newRace) + "!");
				// Transform-untransorm player quickly to force the client to reload the character textures
				TransformationManager.getInstance().transformPlayer(105, player);
				ThreadPoolManager.getInstance().scheduleGeneral(new Untransform(player), 200);
				player.decayMe();
				player.spawnMe();
				boolean update = false;
				for (L2ItemInstance item : player.getInventory().getItems())
				{
					if (item != null)
					{
						if (item.getItemId() == circlets[0])
						{
							player.destroyItem("Donation Race Change", item, player, true);
							player.addItem("Donation Race Change", circlets2[0], 1, player, true);
							update = true;
						}
						else if (item.getItemId() == circlets[1])
						{
							player.destroyItem("Donation Race Change", item, player, true);
							player.addItem("Donation Race Change", circlets2[1], 1, player, true);
							update = true;
						}
					}
				}
				/*
				 * for (L2ItemInstance item : player.getWarehouse().getItems())
				 * {
				 * if (item != null)
				 * {
				 * if (item.getItemId() == circlets[0])
				 * {
				 * player.getWarehouse().destroyItem("Donation Race Change", item, player, player);
				 * player.getWarehouse().addItem("Donation Race Change", circlets2[0], 1, player, player);
				 * update = true;
				 * }
				 * else if (item.getItemId() == circlets[1])
				 * {
				 * player.getWarehouse().destroyItem("Donation Race Change", item, player, player);
				 * player.getWarehouse().addItem("Donation Race Change", circlets2[1], 1, player, player);
				 * update = true;
				 * }
				 * }
				 * }
				 */
				if (update)
				{
					player.sendPacket(new ItemList(player, false));
				}
			}
			else
			{
				_log.severe(player.getName() + " REQUESTED A NAME CHANGE W/O ACTUALLY ACTIVATING THE ITEM FIRST!!!!!!!");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
			}
		}
		else if (action.startsWith("sub_swap "))
		{
			int subclassIndex = 0;
			try
			{
				subclassIndex = Integer.parseInt(action.substring(9));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (subclassIndex <= 0)
			{
				_log.warning(player.getName() + " sent a subswap request with subclassid of 0!!!!!!!!!!!!");
				player.logout();
				return;
			}
			final SubClass sub = player.getSubClasses().get(subclassIndex);
			if (sub == null)
			{
				_log.warning(player.getName() + " sent a subswap request with no subclass!!");
				player.logout();
				return;
			}
			final String subclassName = CharTemplateTable.getInstance().getClassNameById(sub.getClassId());
			final String subclassRace = CharTemplateTable.getInstance().getTemplate(sub.getClassId()).race.name();
			final String mainclassName = CharTemplateTable.getInstance().getClassNameById(player.getBaseClassId());
			String filename = "data/html/custom/Donate/classchangeconfirm.htm";
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setFile(filename);
			itemReply.replace("%sub%", subclassName);
			itemReply.replace("%main%", mainclassName);
			itemReply.replace("%race%", subclassRace);
			String lol = "<td align=center><button action=\"bypass -h pot_sub_swap_confirm " + subclassIndex + "\" value=\"Yes\" width=160 height=30 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td>";
			itemReply.replace("%zht%", lol);
			player.sendPacket(itemReply);
		}
		else if (action.startsWith("sub_swap_confirm "))
		{
			int subclassIndex = 0;
			try
			{
				subclassIndex = Integer.parseInt(action.substring(17));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (player.isInDuel() || player.isInPreDuelState())
			{
				return;
			}
			if (subclassIndex == 0)
			{
				_log.warning(player.getName() + " sent a subswap request with subclassid of 0!!!!!!!!!!!!");
				player.logout();
				return;
			}
			int mainLevel = 0, baseClassId = 0, subLevel = 0, subClassId = 0;
			long mainExp = 0, subExp = 0;
			int newRace = CharTemplateTable.getInstance().getTemplate(player.getSubClasses().get(subclassIndex).getClassId()).race.ordinal();
			final int playerId = player.getObjectId();
			boolean allOk = true;
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT * FROM characters WHERE charId=?");
				statement.setInt(1, playerId);
				ResultSet rset = statement.executeQuery();
				if (rset.next())
				{
					mainLevel = rset.getInt("level");
					mainExp = rset.getLong("exp");
					baseClassId = rset.getInt("base_class");
				}
				else
					allOk = false;
				rset.close();
				statement.close();
				statement = con.prepareStatement("SELECT * FROM character_subclasses WHERE charId=? AND class_index=?");
				statement.setInt(1, playerId);
				statement.setInt(2, subclassIndex);
				rset = statement.executeQuery();
				if (rset.next())
				{
					subLevel = rset.getInt("level");
					subExp = rset.getLong("exp");
					subClassId = rset.getInt("class_id");
				}
				else
					allOk = false;
				rset.close();
				statement.close();
			}
			catch (SQLException e)
			{
				allOk = false;
				e.printStackTrace();
			}
			finally
			{
				try
				{
					if (allOk)
					{
						if (player.destroyItemByItemId("Donation Class Change", CLASS_CHANGE_ITEMID, 1, player, true))
						{
							try
							{
								Olympiad.resetNobleStats(player.getObjectId());
								Hero.HEROES.remove(player.getObjectId());
								Hero.COMPLETE_HEROS.remove(player.getObjectId());
								player.wipeHeroOlyStatsDatabase();
								player.sendMessage("Your hero and olympiad status have been reset");
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
							if (player.getRace().ordinal() != newRace)
							{
								int[] circlets =
								{
									0, 0
								};
								int[] circlets2 =
								{
									0, 0
								};
								switch (player.getRace().ordinal())
								{
									case 0:
										circlets[0] = 9391;
										circlets[1] = 9410;
										break;
									case 1:
										circlets[0] = 9392;
										circlets[1] = 9411;
										break;
									case 2:
										circlets[0] = 9393;
										circlets[1] = 9412;
										break;
									case 3:
										circlets[0] = 9394;
										circlets[1] = 9413;
										break;
									case 4:
										circlets[0] = 9395;
										circlets[1] = 9414;
										break;
									case 5:
										circlets[0] = 9396;
										circlets[1] = 9415;
										break;
									case 6:
										circlets[0] = 9391;
										circlets[1] = 9410;
										break;
									case 7:
										circlets[0] = 9394;
										circlets[1] = 9413;
										break;
								}
								switch (newRace)
								{
									case 0:
										circlets2[0] = 9391;
										circlets2[1] = 9410;
										break;
									case 1:
										circlets2[0] = 9392;
										circlets2[1] = 9411;
										break;
									case 2:
										circlets2[0] = 9393;
										circlets2[1] = 9412;
										break;
									case 3:
										circlets2[0] = 9394;
										circlets2[1] = 9413;
										break;
									case 4:
										circlets2[0] = 9395;
										circlets2[1] = 9414;
										break;
									case 5:
										circlets2[0] = 9396;
										circlets2[1] = 9415;
										break;
									case 6:
										circlets2[0] = 9391;
										circlets2[1] = 9410;
										break;
									case 7:
										circlets2[0] = 9394;
										circlets2[1] = 9413;
										break;
								}
								for (L2ItemInstance item : player.getInventory().getItems())
								{
									if (item != null)
									{
										if (item.getItemId() == circlets[0])
										{
											player.destroyItem("Donation Race Change", item, player, true);
											player.addItem("Donation Race Change", circlets2[0], 1, player, true);
										}
										else if (item.getItemId() == circlets[1])
										{
											player.destroyItem("Donation Race Change", item, player, true);
											player.addItem("Donation Race Change", circlets2[1], 1, player, true);
										}
									}
								}
								/*
								 * for (L2ItemInstance item : player.getWarehouse().getItems())
								 * {
								 * if (item != null)
								 * {
								 * if (item.getItemId() == circlets[0])
								 * {
								 * player.getWarehouse().destroyItem("Donation Race Change", item, player, player);
								 * player.getWarehouse().addItem("Donation Race Change", circlets2[0], 1, player, player);
								 * }
								 * else if (item.getItemId() == circlets[1])
								 * {
								 * player.getWarehouse().destroyItem("Donation Race Change", item, player, player);
								 * player.getWarehouse().addItem("Donation Race Change", circlets2[1], 1, player, player);
								 * }
								 * }
								 * }
								 */
							}
							initiateSubSwap(player, subclassIndex, mainLevel, baseClassId, subLevel, subClassId, mainExp, subExp, newRace, playerId, con);
						}
						else
						{
							_log.severe(player.getName() + " REQUESTED A CLASS CHANGE W/O ACTUALLY ACTIVATING THE ITEM FIRST!!!!!!!");
							player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
						}
					}
					con.close();
				}
				catch (Exception e)
				{
					_log.severe(e.toString());
				}
			}
		}
		else if (action.startsWith("skill_15 "))
		{
			int skillId = 0;
			try
			{
				skillId = Integer.parseInt(action.substring(9));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (skillId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect pot_skill_15 function ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			final int skillLvl = player.getSkillLevel(skillId);
			if (skillLvl <= 100)
			{
				_log.warning(player.getName() + " sent enchanting on a skill that is unenchanted or don't have");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			final int newSkillLvl = skillLvl + (15 - (skillLvl % 100));
			final L2Skill skill = SkillTable.getInstance().getInfo(skillId, newSkillLvl);
			if (skill != null)
			{
				if (player.destroyItemByItemId("+15 skill", SKILL_15_ITEMID, 1, player, true))
				{
					player.addSkill(skill, true);
					player.playSound("ItemSound.quest_finish");
					player.sendMessage("Congratulations! Your " + skill.getName() + " is now +15");
					player.sendSkillList();
					player.sendPacket(new UserInfo(player));
					player.sendPacket(new ExBrExtraUserInfo(player));
					// update all the shortcuts to this skill
					L2ShortCut[] allShortCuts = player.getAllShortCuts();
					for (L2ShortCut sc : allShortCuts)
					{
						if (sc.getId() == skillId && sc.getType() == L2ShortCut.TYPE_SKILL)
						{
							L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), player.getSkillLevel(skillId), 1);
							player.sendPacket(new ShortCutRegister(newsc));
							player.registerShortCut(newsc);
						}
					}
				}
				else
				{
					_log.severe(player.getName() + " REQUESTED A +15 skill without having an item!!!!!!!");
					player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				}
			}
			else
			{
				_log.severe(player.getName() + " REQUESTED A NULL SKILL with skillId of !!!!!!! " + skillId);
			}
		}
		else if (action.startsWith("weapon_exchange "))
		{
			StringTokenizer st = new StringTokenizer(action, " ");
			if (st.countTokens() != 3)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			st.nextToken();
			int itemId = 0;
			try
			{
				itemId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (itemId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			final L2ItemInstance oldWep = player.getActiveWeaponInstance();
			if (oldWep == null)
			{
				player.sendMessage("You don't have a weapon equipped; You can only change the weapon that you have equipped");
				return;
			}
			int exchangerObjId = 0;
			try
			{
				exchangerObjId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (exchangerObjId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			final L2ItemInstance exchangerItem = player.getInventory().getItemByObjectId(exchangerObjId);
			if (exchangerItem == null)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			boolean canExchange = seeIfCanExchangeWeapon(exchangerItem, oldWep);
			FastList<Integer> list = null;
			if (canExchange)
			{
				list = ItemLists.getInstance().getFirstListByItemId(oldWep.getItemId());
				if (list == null)
					canExchange = false;
			}
			if (!canExchange)
			{
				player.sendMessage("Your equipped weapon cannot be exchanged at the moment");
				return;
			}
			if (!list.contains(itemId))
			{
				_log.severe(player.getName() + " JUST TRIED TO HACK THE WEAPON EXCHANGER!!!");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			final L2ItemInstance newItem = ItemTable.getInstance().createDummyItem(itemId);
			if (newItem == null)
			{
				player.sendMessage("Your equipped weapon cannot be exchanged at the moment");
				return;
			}
			final int enchantLevel = oldWep.getEnchantLevel();
			final String oldWepName = "+" + enchantLevel + " " + oldWep.getName();
			final String newWepName = "+" + enchantLevel + " " + newItem.getName();
			String filename = "data/html/custom/Donate/weaponchange2.htm";
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setFile(filename);
			itemReply.replace("%gcp%", oldWepName);
			itemReply.replace("%dtn%", newWepName);
			String lol = "<td align=center><button action=\"bypass -h pot_weapon_exchange_confirm " + itemId + " " + exchangerObjId + "\" value=\"Yes\" width=160 height=30 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td>";
			itemReply.replace("%zht%", lol);
			player.sendPacket(itemReply);
		}
		else if (action.startsWith("weapon_exchange_confirm "))
		{
			StringTokenizer st = new StringTokenizer(action, " ");
			if (st.countTokens() != 3)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			st.nextToken();
			int itemId = 0;
			try
			{
				itemId = Integer.parseInt(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (itemId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange_confirm function ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
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
			int exchangerObjId = 0;
			try
			{
				exchangerObjId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (exchangerObjId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			final L2ItemInstance exchangerItem = player.getInventory().getItemByObjectId(exchangerObjId);
			if (exchangerItem == null)
			{
				_log.warning(player.getName() + " sent incorrect weapon_exchange function ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			boolean canExchange = seeIfCanExchangeWeapon(exchangerItem, oldWep);
			FastList<Integer> list = null;
			if (canExchange)
			{
				list = ItemLists.getInstance().getFirstListByItemId(oldWep.getItemId());
				if (list == null)
					canExchange = false;
			}
			if (!canExchange)
			{
				player.sendMessage("Your equipped weapon cannot be exchanged at the moment");
				return;
			}
			if (!list.contains(itemId))
			{
				_log.severe(player.getName() + " JUST TRIED TO HACK THE WEAPON EXCHANGER!!!");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			final L2ItemInstance newDummyItem = ItemTable.getInstance().createDummyItem(itemId);
			if (newDummyItem == null)
			{
				player.sendMessage("Your equipped weapon cannot be exchanged at the moment");
				return;
			}
			if (player.destroyItem("weapon exchanger base", exchangerItem, 1, player, true))
			{
				final L2Augmentation aug = oldWep.getAugmentation();
				final Elementals ele = oldWep.getElementals();
				final int enchant = oldWep.getEnchantLevel();
				final long untradeableTime = oldWep.getUntradeableTime();
				final String source = oldWep._source;
				final String instance = oldWep._instanceDroppedFrom;
				if (player.destroyItem("weapon exchanger item", oldWep, player, true))
				{
					final L2ItemInstance newItem = player.addItem("weapon exchanger add", itemId, 1, player, true, enchant);
					if (newItem != null)
					{
						newItem.setUntradeableTimer(untradeableTime);
						newItem._source = source;
						newItem._instanceDroppedFrom = instance;
						if (enchant > 0)
							newItem.setEnchantLevel(enchant);
						if (newItem.getItemType() != L2WeaponType.NONE && newItem.getItemType() != L2ArmorType.SIGIL)
						{
							if (aug != null)
								newItem.setAugmentation(aug);
							if (ele != null)
								newItem.setElementAttr(ele.getElement(), ele.getValue());
						}
						player.broadcastUserInfo();
						player.sendPacket(new ItemList(player, true));
						player.playSound("ItemSound.quest_finish");
						player.sendMessage("Congratulations! You have swapped your weapon");
					}
				}
			}
			else
			{
				_log.severe(player.getName() + " REQUESTED weapon exchanger w/o an item!!!!!!!!");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
			}
		}
		else if (action.startsWith("armor_exchange "))
		{
			StringTokenizer st = new StringTokenizer(action, " ");
			if (st.countTokens() != 3)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function (2)");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			st.nextToken();
			int setId = 0;
			try
			{
				setId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (setId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function (3)");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			byte dreadTit = 0;
			String originalArmorName;
			if (player.getSkillLevel(8284) > 0) // heavy
			{
				dreadTit = 1;
				originalArmorName = "Titanium Heavy Set";
			}
			else if (player.getSkillLevel(8286) > 0) // light
			{
				dreadTit = 2;
				originalArmorName = "Titanium Light Set";
			}
			else if (player.getSkillLevel(8288) > 0) // robe
			{
				dreadTit = 3;
				originalArmorName = "Titanium Robe Set";
			}
			else if (player.getSkillLevel(13000) > 0) // heavy
			{
				dreadTit = 4;
				originalArmorName = "Dread Heavy Set";
			}
			else if (player.getSkillLevel(13001) > 0) // light
			{
				dreadTit = 5;
				originalArmorName = "Dread Light Set";
			}
			else if (player.getSkillLevel(13002) > 0) // robe
			{
				dreadTit = 6;
				originalArmorName = "Dread Robe Set";
			}
			else
			{
				player.sendMessage("Your armor set cannot be exchanged at the moment");
				return;
			}
			if (dreadTit == setId)
			{
				_log.warning(player.getName() + " sent armor exchange function to same armorset ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			if (dreadTit < 4)
			{
				if (setId > 3)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange titanium to dread ");
					player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
					return;
				}
			}
			else if (dreadTit < 7)
			{
				if (setId < 4 || setId > 6)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange dread to titanium ");
					player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
					return;
				}
			}
			int exchangerObjId = 0;
			try
			{
				exchangerObjId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (exchangerObjId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function (4)");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			final L2ItemInstance exchangerItem = player.getInventory().getItemByObjectId(exchangerObjId);
			if (exchangerItem == null)
			{
				_log.warning(player.getName() + " sent null armor exchangerItem (5)");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			if (!seeIfCanExchangeArmor(exchangerItem, dreadTit))
			{
				player.sendMessage("Your armor set cannot be exchanged with this item");
				_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item (6)");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			String newArmorSet;
			switch (setId)
			{
				case 1:
					newArmorSet = "Titanium Heavy Set";
					break;
				case 2:
					newArmorSet = "Titanium Light Set";
					break;
				case 3:
					newArmorSet = "Titanium Robe Set";
					break;
				case 4:
					newArmorSet = "Dread Heavy Set";
					break;
				case 5:
					newArmorSet = "Dread Light Set";
					break;
				case 6:
					newArmorSet = "Dread Robe Set";
					break;
				default:
				{
					_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item (7)");
					player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
					return;
				}
			}
			String filename = "data/html/custom/Donate/armorchange2.htm";
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setFile(filename);
			itemReply.replace("%gcp%", originalArmorName);
			itemReply.replace("%dtn%", newArmorSet);
			String lol = "<td align=center><button action=\"bypass -h pot_armor_exchange_confirm " + setId + " " + exchangerObjId + "\" value=\"Yes\" width=160 height=30 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td>";
			itemReply.replace("%zht%", lol);
			player.sendPacket(itemReply);
		}
		else if (action.startsWith("armor_exchange_confirm "))
		{
			StringTokenizer st = new StringTokenizer(action, " ");
			if (st.countTokens() != 3)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function (8)");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			st.nextToken();
			int setId = 0;
			try
			{
				setId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (setId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function (9)");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			byte dreadTit = 0;
			if (player.getSkillLevel(8284) > 0) // heavy
			{
				dreadTit = 1;
			}
			else if (player.getSkillLevel(8286) > 0) // light
			{
				dreadTit = 2;
			}
			else if (player.getSkillLevel(8288) > 0) // robe
			{
				dreadTit = 3;
			}
			else if (player.getSkillLevel(13000) > 0) // heavy
			{
				dreadTit = 4;
			}
			else if (player.getSkillLevel(13001) > 0) // light
			{
				dreadTit = 5;
			}
			else if (player.getSkillLevel(13002) > 0) // robe
			{
				dreadTit = 6;
			}
			else
			{
				player.sendMessage("Your armor set cannot be exchanged at the moment");
				return;
			}
			if (dreadTit == setId)
			{
				_log.warning(player.getName() + " sent armor exchange function to same armorset (10)");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			if (dreadTit < 4)
			{
				if (setId > 3)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange titanium to dread (11)");
					player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
					return;
				}
			}
			else if (dreadTit < 7)
			{
				if (setId < 4 || setId > 6)
				{
					_log.warning(player.getName() + " sent armor exchange function to exchange dread to titanium (12)");
					player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
					return;
				}
			}
			int exchangerObjId = 0;
			try
			{
				exchangerObjId = Integer.valueOf(st.nextToken());
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			if (exchangerObjId <= 0)
			{
				_log.warning(player.getName() + " sent incorrect armor_exchange function ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			final L2ItemInstance exchangerItem = player.getInventory().getItemByObjectId(exchangerObjId);
			if (exchangerItem == null)
			{
				_log.warning(player.getName() + " sent null armor exchangerItem ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			if (!seeIfCanExchangeArmor(exchangerItem, dreadTit))
			{
				player.sendMessage("Your armor set cannot be exchanged with this item");
				_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			L2ArmorSet originalArmorSet;
			switch (dreadTit)
			{
				case 1:
					originalArmorSet = ArmorSetsTable.getInstance().getSet(13435);
					break;
				case 2:
					originalArmorSet = ArmorSetsTable.getInstance().getSet(13436);
					break;
				case 3:
					originalArmorSet = ArmorSetsTable.getInstance().getSet(13437);
					break;
				case 4:
					originalArmorSet = ArmorSetsTable.getInstance().getSet(71001);
					break;
				case 5:
					originalArmorSet = ArmorSetsTable.getInstance().getSet(71011);
					break;
				case 6:
					originalArmorSet = ArmorSetsTable.getInstance().getSet(71021);
					break;
				default:
				{
					_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
					player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
					return;
				}
			}
			if (originalArmorSet == null)
			{
				_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			L2ArmorSet armorSet;
			switch (setId)
			{
				case 1:
					armorSet = ArmorSetsTable.getInstance().getSet(13435);
					break;
				case 2:
					armorSet = ArmorSetsTable.getInstance().getSet(13436);
					break;
				case 3:
					armorSet = ArmorSetsTable.getInstance().getSet(13437);
					break;
				case 4:
					armorSet = ArmorSetsTable.getInstance().getSet(71001);
					break;
				case 5:
					armorSet = ArmorSetsTable.getInstance().getSet(71011);
					break;
				case 6:
					armorSet = ArmorSetsTable.getInstance().getSet(71021);
					break;
				default:
				{
					_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
					player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
					return;
				}
			}
			if (armorSet == null)
			{
				_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			if (armorSet == originalArmorSet)
			{
				_log.warning(player.getName() + " sent armor exchange function and used the wrong exchanger item ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
			if (player.destroyItem("armor exchanger base", exchangerItem, 1, player, true))
			{
				for (Integer slot : ORDER)
				{
					final L2ItemInstance originalItem = player.getInventory().getPaperdollItem(slot);
					if (originalItem == null)
						continue;
					final byte containsMasterwork = originalArmorSet.containItemArmorExchanger(slot, originalItem.getItemId());
					if (containsMasterwork < 1)
						continue;
					final Elementals ele = originalItem.getElementals();
					final int enchant = originalItem.getEnchantLevel();
					final long untradeableTime = originalItem.getUntradeableTime();
					final String source = originalItem._source;
					final String instance = originalItem._instanceDroppedFrom;
					if (player.destroyItem("armor exchanger item", originalItem, 1, player, true))
					{
						final int newItemId = armorSet.getItemIdBySlot(slot, containsMasterwork == 2);
						if (newItemId <= 0)
							continue;
						final L2ItemInstance newItem = player.addItem("armor exchanger add item", newItemId, 1, player, true, enchant);
						if (newItem != null)
						{
							newItem.setUntradeableTimer(untradeableTime);
							newItem._source = source;
							newItem._instanceDroppedFrom = instance;
							if (enchant > 0)
								newItem.setEnchantLevel(enchant);
							if (ele != null)
								newItem.setElementAttr(ele.getElement(), ele.getValue());
						}
					}
					else
					{
						_log.warning(player.getName() + " cannot destroy the armor piece ");
						player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
						return;
					}
				}
				player.broadcastUserInfo();
				player.sendPacket(new ItemList(player, true));
				player.playSound("ItemSound.quest_finish");
				player.sendMessage("Congratulations! You have swapped your equipped armor set!");
			}
			else
			{
				_log.warning(player.getName() + " cannot destroy exchange item! ");
				player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 0);
				return;
			}
		}
	}
	
	static int ORDER[] =
	{
		Inventory.PAPERDOLL_HEAD, Inventory.PAPERDOLL_CHEST, Inventory.PAPERDOLL_LEGS, Inventory.PAPERDOLL_GLOVES, Inventory.PAPERDOLL_FEET
	};
	
	final public static String getRaceName(int i)
	{
		switch (i)
		{
			case 0:
				return "Human Fighter";
			case 1:
				return "Elf";
			case 2:
				return "Dark Elf";
			case 3:
				return "Orc Fighter";
			case 4:
				return "Dwarf";
			case 5:
				return "Kamael";
			case 6:
				return "Human Mystic";
			case 7:
				return "Orc Mystic";
		}
		_log.warning("LOL getRaceName() returned a non-race as race wtf????????????");
		return "LOL WTF?";
	}
	
	protected static void initiateSubSwap(L2PcInstance player, int subclassIndex, int mainLevel, int baseClassId, int subLevel, int subClassId, long mainExp, long subExp, int newRace, final int playerId, Connection con) throws InterruptedException, SQLException
	{
//		for (L2Skill skill : player.getAllSkills())
//		{
//			if (skill.getId() == 755)
//			{
//				player.removeSkill2(skill);
//				player.addItem("Donation Remastery", 10549, 1, player, true);
//				player.sendSkillList();
//				continue;
//			}
//			if (skill.getId() == 756)
//			{
//				player.removeSkill2(skill);
//				player.addItem("Donation Remastery", 10550, 1, player, true);
//				player.sendSkillList();
//				continue;
//			}
//			if (skill.getId() == 757)
//			{
//				player.removeSkill2(skill);
//				player.addItem("Donation Remastery", 10551, 1, player, true);
//				player.sendSkillList();
//				continue;
//			}
//			if (skill.getId() == 1405 && skill.getLevel() == 1)
//			{
//				player.removeSkill2(skill);
//				player.addItem("Donation Remastery", 8618, 1, player, true);
//				player.sendSkillList();
//				continue;
//			}
//			if (skill.getId() == 1405 && skill.getLevel() == 2)
//			{
//				player.removeSkill2(skill);
//				player.addItem("Donation Remastery", 8618, 1, player, true);
//				player.addItem("Donation Remastery", 8619, 1, player, true);
//				player.sendSkillList();
//				continue;
//			}
//			if (skill.getId() == 1405 && skill.getLevel() == 3)
//			{
//				player.removeSkill2(skill);
//				player.addItem("Donation Remastery", 8618, 1, player, true);
//				player.addItem("Donation Remastery", 8619, 1, player, true);
//				player.addItem("Donation Remastery", 8620, 1, player, true);
//				player.sendSkillList();
//				continue;
//			}
//			if (skill.getId() == 1405 && skill.getLevel() == 4)
//			{
//				player.removeSkill2(skill);
//				player.addItem("Donation Remastery", 8618, 1, player, true);
//				player.addItem("Donation Remastery", 8619, 1, player, true);
//				player.addItem("Donation Remastery", 8620, 1, player, true);
//				player.addItem("Donation Remastery", 8621, 1, player, true);
//				player.sendSkillList();
//				continue;
//			}
//			else 
//				continue;
//			
//		}
		player.sendMessage("Initating Class Swap. You BRB!");
		player.logout();
		Thread.sleep(700);
		Olympiad.getInstance().wipeNoble(playerId);
		
		PreparedStatement statement = con.prepareStatement("UPDATE characters set level=?, exp=?, race=?, classid=?, base_class=? WHERE charId=?");
		statement.setInt(1, Math.max(mainLevel, subLevel));
		statement.setLong(2, Math.max(mainExp, subExp));
		statement.setInt(3, newRace);
		statement.setInt(4, subClassId);
		statement.setInt(5, subClassId);
		statement.setInt(6, playerId);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_subclasses set level=?, exp=?, class_id=? WHERE charId=? AND class_index=?");
		statement.setInt(1, Math.min(mainLevel, subLevel));
		statement.setLong(2, Math.min(mainExp, subExp));
		statement.setInt(3, baseClassId);
		statement.setInt(4, playerId);
		statement.setInt(5, subclassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_skills set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, 999);
		statement.setInt(2, playerId);
		statement.setInt(3, 0);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_skills set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, 0);
		statement.setInt(2, playerId);
		statement.setInt(3, subclassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_skills set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, subclassIndex);
		statement.setInt(2, playerId);
		statement.setInt(3, 999);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_skills_save set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, 999);
		statement.setInt(2, playerId);
		statement.setInt(3, 0);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_skills_save set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, 0);
		statement.setInt(2, playerId);
		statement.setInt(3, subclassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_skills_save set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, subclassIndex);
		statement.setInt(2, playerId);
		statement.setInt(3, 999);
		statement.execute();
		statement.close();

		statement = con.prepareStatement("UPDATE character_skills_save set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, subclassIndex);
		statement.setInt(2, playerId);
		statement.setInt(3, 999);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_shortcuts set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, 999);
		statement.setInt(2, playerId);
		statement.setInt(3, 0);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_shortcuts set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, 0);
		statement.setInt(2, playerId);
		statement.setInt(3, subclassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_shortcuts set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, subclassIndex);
		statement.setInt(2, playerId);
		statement.setInt(3, 999);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_hennas set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, 999);
		statement.setInt(2, playerId);
		statement.setInt(3, 0);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_hennas set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, 0);
		statement.setInt(2, playerId);
		statement.setInt(3, subclassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_hennas set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, subclassIndex);
		statement.setInt(2, playerId);
		statement.setInt(3, 999);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_quests set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, 999);
		statement.setInt(2, playerId);
		statement.setInt(3, 0);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_quests set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, 0);
		statement.setInt(2, playerId);
		statement.setInt(3, subclassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_quests set class_index=? WHERE charId=? AND class_index=?");
		statement.setInt(1, subclassIndex);
		statement.setInt(2, playerId);
		statement.setInt(3, 999);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_recipebook set classIndex=? WHERE charId=? AND classIndex=?");
		statement.setInt(1, 999);
		statement.setInt(2, playerId);
		statement.setInt(3, 0);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_recipebook set classIndex=? WHERE charId=? AND classIndex=?");
		statement.setInt(1, 0);
		statement.setInt(2, playerId);
		statement.setInt(3, subclassIndex);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("UPDATE character_recipebook set classIndex=? WHERE charId=? AND classIndex=?");
		statement.setInt(1, subclassIndex);
		statement.setInt(2, playerId);
		statement.setInt(3, 999);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("DELETE from heroes WHERE charId=?");
		statement.setInt(1, playerId);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("DELETE from olympiad_nobles WHERE charId=?");
		statement.setInt(1, playerId);
		statement.execute();
		statement.close();
		
		statement = con.prepareStatement("DELETE from olympiad_nobles_eom WHERE charId=?");
		statement.setInt(1, playerId);
		statement.execute();
		statement.close();
			}
	
	private void spawnMonster(L2PcInstance activeChar, String monsterId, int respawnTime, int mobCount, boolean permanent, int dayNight, int location)
	{
		L2NpcTemplate template1;
		Object object = activeChar.getTarget();
		if (object == null)
		{
			object = activeChar;
		}
		if (monsterId.matches("[0-9]*"))
		{
			int monsterTemplate = Integer.parseInt(monsterId);
			template1 = NpcTable.getInstance().getTemplate(monsterTemplate);
		}
		else
		{
			monsterId = monsterId.replace('_', ' ');
			template1 = NpcTable.getInstance().getTemplateByName(monsterId);
		}
		try
		{
			L2Spawn spawn = new L2Spawn(template1);
			if (Config.SAVE_GMSPAWN_ON_CUSTOM)
				spawn.setCustom(true);
			if (location == 1)
				spawn.setSpawnType(1);
			spawn.setLocx(((L2Object) object).getX());
			spawn.setLocy(((L2Object) object).getY());
			spawn.setLocz(((L2Object) object).getZ() + 5);
			spawn.setAmount(mobCount);
			spawn.setHeading(activeChar.getHeading());
			spawn.setRespawnDelay(respawnTime);
			if (dayNight == 1)
			{
				DayNightSpawnManager.getInstance().addDayCreature(spawn);
			}
			else if (dayNight == 2)
			{
				DayNightSpawnManager.getInstance().addNightCreature(spawn);
			}
			if (activeChar.getInstanceId() > 0)
			{
				spawn.setInstanceId(activeChar.getInstanceId());
				permanent = false;
			}
			else
			{
				spawn.setInstanceId(0);
			}
			if (RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcid()))
			{
				activeChar.sendMessage(template1.name + " is already spawned, so this one won't be permanent");
				permanent = false;
				spawn.init();
				if (!permanent)
				{
					spawn.stopRespawn();
				}
				spawn.getLastSpawn().setIsTempSpawn();
			}
			else
			{
				if (RaidBossSpawnManager.getInstance().getValidTemplate(spawn.getNpcid()) != null)
				{
					spawn.setRespawnMinDelay(86400);
					spawn.setRespawnMaxDelay(172800);
					RaidBossSpawnManager.getInstance().addNewSpawn(spawn, 0L, template1.baseHpMax, template1.baseMpMax, permanent);
				}
				else
				{
					SpawnTable.getInstance().addNewSpawn(spawn, permanent);
					spawn.init();
				}
				if (!permanent)
				{
					spawn.stopRespawn();
				}
				activeChar.sendMessage("Created " + template1.name + "(" + template1.getNpcId() + ") as " + ((L2Object) object).getObjectId());
			}
		}
		catch (Exception e)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
		}
	}
}