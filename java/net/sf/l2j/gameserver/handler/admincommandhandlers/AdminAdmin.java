package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.awt.Color;
import java.util.StringTokenizer;

import ghosts.controller.GhostTemplateTable;
import inertia.model.extensions.tables.InertiaConfigurationTable;
import luna.custom.DressMeEngine.DressMeLoader;
import luna.custom.handler.items.bdoBox.BdoData;
import luna.custom.handler.items.bonanzo.BonanzoData;
import luna.custom.handler.items.capsuledItems.CapsuleData;
import luna.custom.handler.items.lootBox.LootData;
import luna.custom.ranking.xml.data.RanksParser;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.AccessLevels;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.datatables.BufferSkillsTable;
import net.sf.l2j.gameserver.datatables.EnchantHPBonusData;
import net.sf.l2j.gameserver.datatables.FakePcsTable;
import net.sf.l2j.gameserver.datatables.IconsTable;
import net.sf.l2j.gameserver.datatables.ItemLists;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.NpcWalkerRoutesTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.ChatbanManager;
import net.sf.l2j.gameserver.instancemanager.Manager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.events.EventScheduler;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles following admin commands:
 * - admin|admin1/admin2/admin3/admin4/admin5 = slots for the 5 starting admin menus
 * - gmliston/gmlistoff = includes/excludes active character from /gmlist results
 * - silence = toggles private messages acceptance mode
 * - diet = toggles weight penalty mode
 * - tradeoff = toggles trade acceptance mode
 * - reload = reloads specified component from multisell|skill|npc|htm|item|instancemanager
 * - set/set_menu/set_mod = alters specified server setting
 * - saveolymp = saves olympiad state manually
 * - manualhero = cycles olympiad and calculate new heroes.
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2007/07/28 10:06:06 $
 */
public class AdminAdmin implements IAdminCommandHandler
{

private static final String[] ADMIN_COMMANDS =
{
	"admin_admin",
	"admin_admin1",
	"admin_admin2",
	"admin_admin3",
	"admin_admin4",
	"admin_admin5",
	"admin_gmliston",
	"admin_gmlistoff",
	"admin_silence",
	"admin_diet",
	"admin_tradeoff",
	"admin_reload",
	"admin_set",
	"admin_set_menu",
	"admin_set_mod",
	"admin_saveolymp",
	"admin_manualhero",
	"admin_sethero",
	"admin_killAllRaids",
	"admin_upraids",
	"admin_gotoraid",
	"admin_endolympiad",
	"admin_fakeppl",
	"admin_inv_cleanup",
	"admin_changelog",
	"admin_setphp",
	"admin_sethp",
	"admin_xbanchat",
	"admin_forcesay",
	"admin_addspree",
	"admin_hidegmview",
	"admin_setbeta",
	"admin_night",
	"admin_start_event_engine",
	"admin_stop_event_engine"
};

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	
	if (command.startsWith("admin_admin"))
	{

		showMainPage(activeChar, command);
	}
	else if (command.startsWith("admin_start_event_engine"))
    {

        EventScheduler.getInstance().startEventEngine();
        activeChar.sendMessage("Event engine has been turned on.");


    }
    else if (command.startsWith("admin_stop_event_engine"))
    {

        EventScheduler.getInstance().closeEventEngine();
        activeChar.sendMessage("Event engine has been turned off. This won't abort the currently running events.");


    }
	else if (command.startsWith("admin_gmliston"))
	{
		GmListTable.getInstance().showGm(activeChar);
		activeChar.sendMessage("Registered into gm list");
	}
	else if (command.startsWith("admin_changelog"))
	{
			activeChar.showQuestMovie(11);
	}
	else if (command.startsWith("admin_sethp"))
	{
		try
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			int val = 1000;
			if (st.hasMoreTokens())
				val = Integer.parseInt(st.nextToken());
				
			{
				L2Character target = (L2Character) activeChar.getTarget();
				target.reduceCurrentHp(target.getCurrentHp()-1, activeChar, null);
				target.setCurrentHp(val);
			}
		}
		catch (NumberFormatException nfe)
			{
				activeChar.sendMessage("Usage: //setphp numeric_value  = Sets the current hp of the target to numeric_value");
			}
	}

	else if (command.startsWith("admin_setphp"))
	{
		try
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command

			int val1 = 1000;
			if (st.hasMoreTokens())
			val1 = Integer.parseInt(st.nextToken());
			L2Character target = (L2Character) activeChar.getTarget();

			double hpratio = target.getMaxHp() * val1;
			double hppercent = hpratio / 100;
			target.reduceCurrentHp(target.getCurrentHp()-1, activeChar, null);
			target.setCurrentHp(hppercent);
		}
		catch (NumberFormatException nfe)
		{
			activeChar.sendMessage("Usage: //setphp numeric_value  = Sets the current hp of the target to percent numeric_value%");
		}
	}
	else if (command.startsWith("admin_setbeta"))
	{
		try
		{
			L2PcInstance target = (L2PcInstance) activeChar.getTarget();
			if (target.isBeta())
			{
				target.setBeta(false);
				activeChar.sendMessage(target.getName() +" is no longer beta moded");
				target.sendMessage("You are no longerbeta moded");
			}
			else
			{
				target.setBeta(true);
				activeChar.sendMessage(target.getName() +" is now beta moded");
			}
		}
		catch (NumberFormatException nfe)
		{
			activeChar.sendMessage("Usage: //setphp numeric_value  = Sets the current hp of the target to percent numeric_value%");
		}
	}
	else if (command.startsWith("admin_addspree"))
	{
		int k = 0;
		try
		{
			k = Integer.parseInt(command.substring(15));
		}
		catch (final Exception e)
		{
			activeChar.sendMessage("Invalid usage!");
			return false;
		}
		if (activeChar.getTarget() instanceof L2PcInstance)
			((L2PcInstance) activeChar.getTarget())._streak = k;
		else
			activeChar.sendMessage("Target is incorrect");
	}
	else if (command.startsWith("admin_xbanchat"))
	{
		final StringTokenizer st = new StringTokenizer(command);
		try
		{	st.nextToken();
			final String typename;
			final String name = st.nextToken();
			final int type = Integer.parseInt(st.nextToken());
			if (type > 2)
			{	activeChar.sendMessage("The 2nd parameter must be 0-1-2 HERO-TRADE-GLOBAL");
				return false;
			}
			final int minutes = Integer.parseInt(st.nextToken());
			final L2PcInstance target = L2World.getInstance().getPlayer(name);
			switch(type)
			{
			  case 0:
			    typename = "Hero";
			    break;
			  case 1:
				typename = "Trade";
			    break;
			  case 2:
				typename = "Shout";
			    break;
			  default:
				typename = "YOU FUCKED IT UP";
			}
			if (name != null)
			{
				ChatbanManager.getInstance().setExpiration(target, type, minutes);
				activeChar.sendMessage("You have chat banned "+ name + "'s "+typename+ " chat for "+ minutes+" minutes.");
			}
			else
				activeChar.sendMessage("Sorry idiot, I can't find that person.");
		}
		catch (Exception e)
		{
			activeChar.sendMessage("You fucked up something. Usage //xbanchat charname type(0-1-2 HERO-TRADE-GLOBAL) minutes");
		}
	}
	else if (command.startsWith("admin_forcesay"))
	{
		final L2Object obj = activeChar.getTarget();
		if (obj instanceof L2Npc)
		{	
			final String msg = command.substring(15);
			final L2Npc npc = (L2Npc) obj;
			final CreatureSay cs = new CreatureSay(npc.getObjectId(), 0, npc.getName(), msg);
			for (L2PcInstance player : npc.getKnownList().getKnownType(L2PcInstance.class))
			{
				player.sendPacket(cs);
			}
		}
		else
		{
			activeChar.sendMessage("That is the incorrect target!");
			return false;
		}
	}

	else if (command.startsWith("admin_hidegmview"))
		{
			activeChar.resetHidedGMView();
		}
	else if (command.startsWith("admin_inv_cleanup"))
		{
			final L2Object obj = activeChar.getTarget();
			PcInventory inventory = obj.getActingPlayer().getInventory();
			for (L2ItemInstance item : inventory.getItems())
			{
				if (item.isEquipped() || item.getItemId() == 57)
					continue;

				inventory.destroyItem("Destroy Item", item, activeChar.getTarget().getActingPlayer(), null);
			}
			L2PcInstance target = activeChar.getTarget().getActingPlayer();
			target.sendPacket(new ItemList(target, false));
			activeChar.sendMessage( target.getName()+"'s Inventory was cleaned up.");
		}
	else if (command.startsWith("admin_gmlistoff"))
	{
		if (activeChar.getAccessLevel().getLevel() > 3)
		{
			GmListTable.getInstance().hideGm(activeChar);
			activeChar.sendMessage("Removed from gm list");
		}
	}
	else if (command.startsWith("admin_silence"))
	{
		if (activeChar.getMessageRefusal()) // already in message refusal mode
		{
			activeChar.setMessageRefusal(false);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.MESSAGE_ACCEPTANCE_MODE));
		}
		else
		{
			activeChar.setMessageRefusal(true);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.MESSAGE_REFUSAL_MODE));
		}
	}
	else if (command.startsWith("admin_saveolymp"))
	{
		Olympiad.getInstance().saveOlympiadStatus();
		activeChar.sendMessage("olympiad system saved.");
	}
	else if (command.startsWith("admin_endolympiad"))
	{
		try
		{
			Olympiad.getInstance().manualSelectHeroes();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		activeChar.sendMessage("Heroes formed");
	}
	else if (command.startsWith("admin_manualhero") || command.startsWith("admin_sethero"))
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else
			return false;
		player.setHero(!player.isHero());
		if (player.isHero())
			player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
		player.sendMessage("Admin changed your hero status");
		player.broadcastUserInfo();
	}
	else if (command.startsWith("admin_diet"))
	{
		try
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			if (st.nextToken().equalsIgnoreCase("on"))
			{
				activeChar.setDietMode(true);
				activeChar.sendMessage("Diet mode on");
			}
			else if (st.nextToken().equalsIgnoreCase("off"))
			{
				activeChar.setDietMode(false);
				activeChar.sendMessage("Diet mode off");
			}
		}
		catch (Exception ex)
		{
			if (activeChar.getDietMode())
			{
				activeChar.setDietMode(false);
				activeChar.sendMessage("Diet mode off");
			}
			else
			{
				activeChar.setDietMode(true);
				activeChar.sendMessage("Diet mode on");
			}
		}
	}
	else if (command.startsWith("admin_tradeoff"))
	{
		try
		{
			String mode = command.substring(15);
			if (mode.equalsIgnoreCase("on"))
			{
				activeChar.setTradeRefusal(true);
				activeChar.sendMessage("Trade refusal enabled");
			}
			else if (mode.equalsIgnoreCase("off"))
			{
				activeChar.setTradeRefusal(false);
				activeChar.sendMessage("Trade refusal disabled");
			}
		}
		catch (Exception ex)
		{
			if (activeChar.getTradeRefusal())
			{
				activeChar.setTradeRefusal(false);
				activeChar.sendMessage("Trade refusal disabled");
			}
			else
			{
				activeChar.setTradeRefusal(true);
				activeChar.sendMessage("Trade refusal enabled");
			}
		}
	}
	else if (command.startsWith("admin_reload"))
	{
		if (activeChar.getName().equalsIgnoreCase("[GM]Brado") || activeChar.getName().equalsIgnoreCase("[GM]Fate"))
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			try
			{
				String type = st.nextToken();
				if (type.equals("multisell"))
				{
					L2Multisell.getInstance().reload();
					activeChar.sendMessage("multisell reloaded");
					System.out.println("multisell reloaded by "+activeChar.getName());
				}
				else if (type.startsWith("inertia"))
				{
					InertiaConfigurationTable.getInstance().reload();
					activeChar.sendMessage("Inertia templates have been reloaded.");
				}
				else if (type.startsWith("ghost"))
				{
					InertiaConfigurationTable.getInstance().reload();
					GhostTemplateTable.getInstance().reload();
					activeChar.sendMessage("Ghost templates have been reloaded.");
				}
				else if (type.startsWith("acar"))
				{
					AdminCommandAccessRights.getInstance().reloadAdminCommandAccessRights();
					activeChar.sendMessage("Admin commands rights have been reloaded.");
				}
				else if (type.startsWith("teleport"))
				{
					TeleportLocationTable.getInstance().reloadAll();
					activeChar.sendMessage("teleport location table reloaded");
					System.out.println("teleporation locations reloaded by "+activeChar.getName());
				}
				else if (type.startsWith("dressme"))
				{
					DressMeLoader.getInstance().reload();
					activeChar.sendMessage("Dressme table reloaded");
					System.out.println("Dressme reloaded by "+activeChar.getName());
				}
				else if (type.startsWith("ranks"))
				{
					RanksParser.getInstance().reload();
					activeChar.sendMessage("RanksParser table reloaded");
					System.out.println("RanksParser reloaded by "+activeChar.getName());
				}
				else if (type.startsWith("nexus"))
				{
					activeChar.sendMessage("Nexus Restarted");
					System.out.println("Nexus Restarted "+activeChar.getName());
				}
				else if (type.startsWith("skill"))
				{
					SkillTable.getInstance().reload();
					SkillTreeTable.getInstance();
					activeChar.sendMessage("skills reloaded");
					System.out.println("skills reloaded by "+activeChar.getName());
				}
				else if (type.equalsIgnoreCase("npc") || type.equalsIgnoreCase("npcs"))
				{
					FakePcsTable.getInstance().load();
					NpcTable.getInstance().reloadAllNpc();
					activeChar.sendMessage("npcs reloaded");
					System.out.println("NPCs reloaded by "+activeChar.getName());
				}
				else if (type.startsWith("htm"))
				{
					HtmCache.getInstance().reload();
					activeChar.sendMessage("Cache[HTML]: " + HtmCache.getInstance().getMemoryUsage() + " megabytes on " + HtmCache.getInstance().getLoadedFiles() + " files loaded");
					System.out.println("HTMLs reloaded by "+activeChar.getName());
				}
				else if (type.startsWith("item"))
				{
					ItemTable.getInstance().reload();
					activeChar.sendMessage("Item templates reloaded");
					System.out.println("items reloaded by "+activeChar.getName());
					EnchantHPBonusData.getInstance().reload();
					ItemLists.getInstance().loadLists();
					activeChar.sendMessage("Item lists have been reloaded");
					System.out.println("ItemList table reloaded by "+activeChar.getName());
				}
				else if (type.startsWith("config"))
				{
					Config.load();
					activeChar.sendMessage("All config settings have been reload");
					System.out.println("Configs reloaded by "+activeChar.getName());
				}
				else if (type.startsWith("instancemanager"))
				{
					Manager.reloadAll();
					activeChar.sendMessage("All instance manager has been reloaded");
					System.out.println("InstanceManager reloaded by "+activeChar.getName());
				}
				else if (type.startsWith("npcwalkers"))
				{
					NpcWalkerRoutesTable.getInstance().load();
					activeChar.sendMessage("All NPC walker routes have been reloaded");
					System.out.println("NPCwalkers reloaded by "+activeChar.getName());
				}
				else if (type.startsWith("access"))
				{
					AccessLevels.getInstance().reloadAccessLevels();
					AdminCommandAccessRights.getInstance().reloadAdminCommandAccessRights();
					activeChar.sendMessage("Access Rights have been reloaded");
					System.out.println("Access level rights reloaded by "+activeChar.getName());
				}
				else if (type.startsWith("quests") || type.equalsIgnoreCase("scripts"))
				{
					QuestManager.getInstance().reloadAllQuests();
					activeChar.sendMessage("All Quests/Scripts have been reloaded");
					System.out.println("quests reloaded by "+activeChar.getName());
				}
				else if(type.startsWith("npcbuffer"))
				{
					BufferSkillsTable.reload();
					activeChar.sendMessage("Buffer skills table has been reloaded");
					System.out.println("NPCBuffer table reloaded by "+activeChar.getName());
				}
				else if(type.startsWith("list"))
				{
					ItemLists.getInstance().loadLists();
					activeChar.sendMessage("Item lists have been reloaded");
					System.out.println("ItemList table reloaded by "+activeChar.getName());
				}
				else if(type.startsWith("zone"))
				{
					ZoneManager.getInstance().reload();;
					activeChar.sendMessage("Zones have been reloaded");
					System.out.println("Zones table reloaded by "+activeChar.getName());
				}

				else if(type.startsWith("bonanzo"))
				{
					BonanzoData.getInstance().ReloadBonanzo();
					activeChar.sendMessage("Bonanzo Boxes reloaded.");
					
				}
				else if (type.startsWith("lootboxes"))
				{
					LootData.getInstance().ReloadLootBoxes();
					activeChar.sendMessage("Loot Boxes reloaded.");
				}
				else if (type.startsWith("bdoboxes"))
				{
					BdoData.getInstance().ReloadLootBoxes();
					activeChar.sendMessage("BDO Boxes reloaded.");
				}
				else if (type.startsWith("capsules"))
				{
					CapsuleData.getInstance().reloadCapsules();
					activeChar.sendMessage("Capsules Boxes reloaded.");
				}
				else if (type.startsWith("achievements"))
				{
					//Achievements.main(null);
					activeChar.sendMessage("Achievements Boxes reloaded.");
				}
				else if (type.startsWith("icons"))
				{
					IconsTable.getInstance().reload();
					activeChar.sendMessage("Icons reloaded.");
				}
				
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage:  //reload <multisell|skill|npc|htm|item|teleport|config|instancemanager|npcwalkers|access|quests|npcbuffer>");
			}
		}
		else
		{
			activeChar.sendMessage("Reload complete.");
		}
	}
	else if (command.startsWith("admin_set"))
	{
		StringTokenizer st = new StringTokenizer(command);
		String[] cmd = st.nextToken().split("_");
		try
		{
			String[] parameter = st.nextToken().split("=");
			String pName = parameter[0].trim();
			String pValue = parameter[1].trim();
			if (Config.setParameterValue(pName, pValue))
				activeChar.sendMessage("parameter " + pName + " succesfully set to " + pValue);
			else
				activeChar.sendMessage("Invalid parameter!");
		}
		catch (Exception e)
		{
			if (cmd.length == 2)
				activeChar.sendMessage("Usage: //set parameter=value");
		}
		finally
		{
			if (cmd.length == 3)
			{
				if (cmd[2].equalsIgnoreCase("menu"))
					AdminHelpPage.showHelpPage(activeChar, "settings.htm");
				else if (cmd[2].equalsIgnoreCase("mod"))
					AdminHelpPage.showHelpPage(activeChar, "mods_menu.htm");
			}
		}
	}
	else if (command.startsWith("admin_killAllRaids"))
	{
		try
		{
			RaidBossSpawnManager.getInstance().updateDbKillAll();
		}
		catch (Exception e)
		{
		}
		finally
		{
			activeChar.sendMessage("All Raidbosses have been set to respawn in the database.");
		}
	}
	else if (command.startsWith("admin_upraids"))
	{
		try
		{
			L2RaidBossInstance.showRaidsThatAreUp(activeChar);
		}
		catch (Exception e)
		{
		}
	}
	else if (command.startsWith("admin_gotoraid"))
	{
		try
		{
			StringTokenizer st = new StringTokenizer(command);
			
			st.nextToken();
			int number = 0;
			
			try
			{
				number = Integer.parseInt(st.nextToken());
			}
			catch (Exception e)
			{
				activeChar.sendMessage("You didn't specify a number!");
				return true;
			}
			
			if (number < 0)
				return true;
			
			L2RaidBossInstance.goToRaid(activeChar, number);
		}
		catch (Exception e)
		{
		}
	}
	else if (command.startsWith("admin_fakeppl"))
	{
		try
		{
			StringTokenizer st = new StringTokenizer(command);
			
			st.nextToken();
			int number = 0;
			
			try
			{
				number = Integer.parseInt(st.nextToken());
			}
			catch (Exception e)
			{
				activeChar.sendMessage("You didn't specify a number!");
				return true;
			}
			
			if (number < 0)
				return false;
			
			L2World.FAKE_NUMBER_OF_ONLINE_PEOPLE = number;
			activeChar.sendMessage("Number of online players set to "+number);
		}
		catch (Exception e)
		{
		}
	}
	return true;
}


public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}

private void showMainPage(L2PcInstance activeChar, String command)
{
	final var serverPrimitive = new ExServerPrimitive("Bliz0", activeChar.getLoc());
	serverPrimitive.addCircle(Color.RED, 300, 30	, 1);
	serverPrimitive.addCircle(Color.RED, 300, 30	, 3);
	serverPrimitive.addCircle(Color.RED, 300, 30	, 5);
	
	activeChar.sendPacket(serverPrimitive);
	int mode = 0;
	String filename = null;
	try
	{
		mode = Integer.parseInt(command.substring(11));
	}
	catch (Exception e)
	{
	}
	switch (mode)
	{
	case 1:
		filename = "main";
		break;
	case 2:
		filename = "game";
		break;
	case 3:
		filename = "effects";
		break;
	case 4:
		filename = "server";
		break;
	case 5:
		filename = "mods";
		break;
	default:
		if (Config.GM_ADMIN_MENU_STYLE.equals("modern"))
			filename = "main";
		else
			filename = "classic";
		break;
	}
	AdminHelpPage.showHelpPage(activeChar, filename + "_menu.htm");

	//InertiaController.getInstance().renderChill(activeChar);
}
}
