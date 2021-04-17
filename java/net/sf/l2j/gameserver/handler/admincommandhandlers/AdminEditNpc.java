/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.TradeController;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.FakePcsTable;
import net.sf.l2j.gameserver.datatables.ItemLists;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2DropCategory;
import net.sf.l2j.gameserver.model.L2DropData;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2TradeList;
import net.sf.l2j.gameserver.model.L2TradeList.L2TradeItem;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.StringUtil;

/**
 * @author terry
 * 
 */
public class AdminEditNpc implements IAdminCommandHandler
{
private static Logger _log = Logger.getLogger(AdminEditChar.class.getName());
private final static int PAGE_LIMIT = 7;

private static final String[] ADMIN_COMMANDS =
{
	"admin_edit_npc",
	"admin_aggro",
	"admin_save_npc",
	"admin_show_droplist",
	"admin_edit_drop",
	"admin_add_drop",
	"admin_del_drop",
	"admin_showShop",
	"admin_showShopList",
	"admin_addShopItem",
	"admin_delShopItem",
	"admin_editShopItem",
	"admin_show_skilllist_npc",
	"admin_add_skill_npc",
	"admin_edit_skill_npc",
	"admin_del_skill_npc",
	"admin_shift_click",
	"admin_itemlist_debug",
	"admin_addraiddrop",
	"admin_addraiddropmenu",
	"admin_close_window"
};

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	//TODO: Tokenize and protect arguments parsing. Externalize HTML.
	if (command.startsWith("admin_showShop "))
	{
		String[] args = command.split(" ");
		if (args.length > 1)
			showShop(activeChar, Integer.parseInt(command.split(" ")[1]));
	}
	if (command.startsWith("admin_showShopList "))
	{
		String[] args = command.split(" ");
		if (args.length > 2)
			showShopList(activeChar, Integer.parseInt(command.split(" ")[1]), Integer.parseInt(command.split(" ")[2]));
	}
	else if (command.startsWith("admin_edit_npc "))
	{
		try
		{
			String[] commandSplit = command.split(" ");
			int npcId = Integer.parseInt(commandSplit[1]);
			L2NpcTemplate npc = NpcTable.getInstance().getTemplate(npcId);
			Show_Npc_Property(activeChar, npc);
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Wrong usage: //edit_npc <npcId>");
		}
	}
	else if (command.startsWith("admin_show_droplist "))
	{
		int npcId = 0;
		try
		{
			npcId = Integer.parseInt(command.substring(20).trim());
		}
		catch (Exception e)
		{
		}
		
		if (npcId > 0)
			showNpcDropList(activeChar, npcId);
		else
			activeChar.sendMessage("Usage: //show_droplist <npc_id>");
	}
	else if (command.startsWith("admin_addShopItem "))
	{
		String[] args = command.split(" ");
		if (args.length > 1)
			addShopItem(activeChar, args);
	}
	else if (command.startsWith("admin_delShopItem "))
	{
		String[] args = command.split(" ");
		if (args.length > 2)
			delShopItem(activeChar, args);
	}
	else if (command.startsWith("admin_editShopItem "))
	{
		String[] args = command.split(" ");
		if (args.length > 2)
			editShopItem(activeChar, args);
	}
	else if (command.startsWith("admin_save_npc "))
	{
		try
		{
			save_npc_property(activeChar, command);
		}
		catch (StringIndexOutOfBoundsException e)
		{
		}
	}
	else if (command.startsWith("admin_edit_drop "))
	{
		int npcId = -1, itemId = 0, category = -1000, enchant = 0;
		try
		{
			StringTokenizer st = new StringTokenizer(command.substring(16).trim());
			if (st.countTokens() == 4)
			{
				try
				{
					npcId = Integer.parseInt(st.nextToken());
					itemId = Integer.parseInt(st.nextToken());
					category = Integer.parseInt(st.nextToken());
					enchant = Integer.parseInt(st.nextToken());
					showEditDropData(activeChar, npcId, itemId, category, enchant);
				}
				catch (Exception e)
				{
				}
			}
			else if (st.countTokens() == 8)
			{
				try
				{
					npcId = Integer.parseInt(st.nextToken());
					itemId = Integer.parseInt(st.nextToken());
					category = Integer.parseInt(st.nextToken());
					enchant = Integer.parseInt(st.nextToken());
					int min = Integer.parseInt(st.nextToken());
					int max = Integer.parseInt(st.nextToken());
					int chance = Integer.parseInt(st.nextToken());
					int partydrop = Integer.parseInt(st.nextToken());
					
					updateDropData(activeChar, npcId, itemId, min, max, category, chance, partydrop, enchant);
				}
				catch (Exception e)
				{
					_log.fine("admin_edit_drop parements error: " + command);
				}
			}
			else
				activeChar.sendMessage("Usage: //edit_drop <npc_id> <item_id> <category> [<min> <max> <chance>]");
		}
		catch (StringIndexOutOfBoundsException e)
		{
			activeChar.sendMessage("Usage: //edit_drop <npc_id> <item_id> <category> [<min> <max> <chance>]");
		}
	}
	else if (command.startsWith("admin_add_drop "))
	{
		int npcId = -1;
		try
		{
			StringTokenizer st = new StringTokenizer(command.substring(15).trim());
			if (st.countTokens() == 1)
			{
				try
				{
					String[] input = command.substring(15).split(" ");
					if (input.length < 1)
						return true;
					npcId = Integer.parseInt(input[0]);
				}
				catch (Exception e)
				{
				}
				
				if (npcId > 0)
				{
					L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
					showAddDropData(activeChar, npcData);
				}
			}
			else if (st.countTokens() == 6)
			{
				try
				{
					npcId = Integer.parseInt(st.nextToken());
					int itemId = Integer.parseInt(st.nextToken());
					int category = Integer.parseInt(st.nextToken());
					int min = Integer.parseInt(st.nextToken());
					int max = Integer.parseInt(st.nextToken());
					int chance = Integer.parseInt(st.nextToken());
					
					addDropData(activeChar, npcId, itemId, min, max, category, chance, 0, 0);
				}
				catch (Exception e)
				{
					_log.fine("admin_add_drop parements error: " + command);
				}
			}
			else if (st.countTokens() == 7)
			{
				try
				{
					npcId = Integer.parseInt(st.nextToken());
					int itemId = Integer.parseInt(st.nextToken());
					int category = Integer.parseInt(st.nextToken());
					int min = Integer.parseInt(st.nextToken());
					int max = Integer.parseInt(st.nextToken());
					int chance = Integer.parseInt(st.nextToken());
					int partydrop = Integer.parseInt(st.nextToken());
					
					addDropData(activeChar, npcId, itemId, min, max, category, chance, partydrop, 0);
				}
				catch (Exception e)
				{
					_log.fine("admin_add_drop parements error: " + command);
				}
			}
			else if (st.countTokens() == 8)
			{
				try
				{
					npcId = Integer.parseInt(st.nextToken());
					int itemId = Integer.parseInt(st.nextToken());
					int category = Integer.parseInt(st.nextToken());
					int min = Integer.parseInt(st.nextToken());
					int max = Integer.parseInt(st.nextToken());
					int chance = Integer.parseInt(st.nextToken());
					int partydrop = Integer.parseInt(st.nextToken());
					int enchant = Integer.parseInt(st.nextToken());
					
					addDropData(activeChar, npcId, itemId, min, max, category, chance, partydrop, enchant);
				}
				catch (Exception e)
				{
					_log.fine("admin_add_drop parements error: " + command);
				}
			}
			else
				activeChar.sendMessage("Usage: //add_drop <npc_id> [<item_id> <category> <min> <max> <chance>]");
		}
		catch (StringIndexOutOfBoundsException e)
		{
			activeChar.sendMessage("Usage: //add_drop <npc_id> [<item_id> <category> <min> <max> <chance>]");
		}
	}
	else if (command.startsWith("admin_show_skilllist_npc ")) // L2J_JP ADD
	{
		StringTokenizer st = new StringTokenizer(command.substring(25), " ");
		try
		{
			int npcId = -1;
			int page = 0;
			if (st.countTokens() <= 2)
			{
				if (st.hasMoreTokens())
					npcId = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
					page = Integer.parseInt(st.nextToken());
			}
			
			if (npcId > 0)
			{
				showNpcSkillList(activeChar, npcId, page);
			}
			else
				activeChar.sendMessage("Usage: //show_skilllist_npc <npc_id> <page>");
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Usage: //show_skilllist_npc <npc_id> <page>");
		}
	}
	else if (command.startsWith("admin_edit_skill_npc ")) // L2J_JP ADD
	{
		int npcId = -1, skillId = -1;
		try
		{
			StringTokenizer st = new StringTokenizer(command.substring(21).trim(), " ");
			if (st.countTokens() == 2)
			{
				try
				{
					npcId = Integer.parseInt(st.nextToken());
					skillId = Integer.parseInt(st.nextToken());
					showNpcSkillEdit(activeChar, npcId, skillId);
				}
				catch (Exception e)
				{
				}
			}
			else if (st.countTokens() == 3)
			{
				try
				{
					npcId = Integer.parseInt(st.nextToken());
					skillId = Integer.parseInt(st.nextToken());
					int level = Integer.parseInt(st.nextToken());
					
					updateNpcSkillData(activeChar, npcId, skillId, level);
				}
				catch (Exception e)
				{
					_log.warning("admin_edit_skill_npc parements error: " + command);
				}
			}
			else
			{
				activeChar.sendMessage("Usage: //edit_skill_npc <npc_id> <item_id> [<level>]");
			}
		}
		catch (StringIndexOutOfBoundsException e)
		{
			activeChar.sendMessage("Usage: //edit_skill_npc <npc_id> <item_id> [<level>]");
		}
	}
	else if (command.startsWith("admin_add_skill_npc ")) // L2J_JP ADD
	{
		int npcId = -1, skillId = -1;
		try
		{
			StringTokenizer st = new StringTokenizer(command.substring(20).trim(), " ");
			if (st.countTokens() == 1)
			{
				try
				{
					String[] input = command.substring(20).split(" ");
					if (input.length < 1)
						return true;
					npcId = Integer.parseInt(input[0]);
				}
				catch (Exception e)
				{
				}
				
				if (npcId > 0)
				{
					L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
					showNpcSkillAdd(activeChar, npcData);
				}
			}
			else if (st.countTokens() == 3)
			{
				try
				{
					npcId = Integer.parseInt(st.nextToken());
					skillId = Integer.parseInt(st.nextToken());
					int level = Integer.parseInt(st.nextToken());
					
					addNpcSkillData(activeChar, npcId, skillId, level);
				}
				catch (Exception e)
				{
					_log.warning("admin_add_skill_npc parements error: " + command);
				}
			}
			else
			{
				activeChar.sendMessage("Usage: //add_skill_npc <npc_id> [<level>]");
			}
		}
		catch (StringIndexOutOfBoundsException e)
		{
			activeChar.sendMessage("Usage: //add_skill_npc <npc_id> [<level>]");
		}
	}
	else if (command.startsWith("admin_del_skill_npc ")) // [L2J_JP ADD]
	{
		int npcId = -1, skillId = -1;
		try
		{
			String[] input = command.substring(20).split(" ");
			if (input.length >= 2)
			{
				npcId = Integer.parseInt(input[0]);
				skillId = Integer.parseInt(input[1]);
			}
		}
		catch (Exception e)
		{
		}
		
		if (npcId > 0)
		{
			deleteNpcSkillData(activeChar, npcId, skillId);
		}
		else
		{
			activeChar.sendMessage("Usage: //del_skill_npc <npc_id> <skill_id>");
		}
	}
	else if (command.startsWith("admin_del_drop "))
	{
		int npcId = -1, itemId = -1, category = -1000, enchant = 0;
		try
		{
			String[] input = command.substring(15).split(" ");
			if (input.length >= 3)
			{
				npcId = Integer.parseInt(input[0]);
				itemId = Integer.parseInt(input[1]);
				category = Integer.parseInt(input[2]);
				enchant = Integer.parseInt(input[3]);
			}
		}
		catch (Exception e)
		{
		}
		
		if (npcId > 0)
			deleteDropData(activeChar, npcId, itemId, category, enchant);
		else
			activeChar.sendMessage("Usage: //del_drop <npc_id> <item_id> <category> <enchant>");
	}
	else if (command.startsWith("admin_shift_click"))
	{
		try
		{
			L2Object obj = activeChar.getTarget();
			
			if (obj != null)
				obj.onActionShift(activeChar.getClient());
		}
		catch (StringIndexOutOfBoundsException e)
		{
		}
	}
	else if (command.equalsIgnoreCase("admin_aggro"))
	{
		L2Object obj = activeChar.getTarget();
		
		if (obj != null && obj instanceof L2Attackable)
		{
			((L2Attackable) obj).getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, Integer.MAX_VALUE);
		}
		else
		{
			activeChar.sendMessage("target a mob");
		}
	}
	else if (command.equals("admin_addraiddropmenu"))
	{
		showMainPage(activeChar);
	}
	else if (command.startsWith("admin_addraiddrop"))
    {
        try
        {
            StringTokenizer st = new StringTokenizer(command.substring(18).trim());
            if (st.countTokens() == 1)
            {
                try
                {
                    int number = Integer.parseInt(st.nextToken());
                    if (number == 85)
                    {
                        int count85 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive85Raids)
                        {
                        	activeChar.sendMessage("Adding dropdata on " + boss.getName());
                        	addDropData(activeChar, boss.getNpcId(),  97003,  1500,  1500,  1,  1000004,  10,  0);
                        	addDropData(activeChar, boss.getNpcId(),  4355,  1200,  1200,  2,  1000003,  10,  0);
                        	addDropData(activeChar, boss.getNpcId(),  6673,  10,  10,  8,  1000000,  10,  0);
                        	addDropData(activeChar, boss.getNpcId(),  98022,  15,  15,  15,  1000000,  10,  0);
                        	addDropData(activeChar, boss.getNpcId(),  13761,  10,  10,  10,  1000000,  10,  0);
                        	addDropData(activeChar, boss.getNpcId(),  1000038,  2,  2,  5,  1000000,  10,  0);
                        	addDropData(activeChar, boss.getNpcId(),  1000001,  3,  3,  4,  1000000,  10,  0);
                        	addDropData(activeChar, boss.getNpcId(),  9627,  1,  1,  16,  500000,  10,  0);
                        	addDropData(activeChar, boss.getNpcId(),  4356,  1,  2,  32,  500000,  0,  0);
                        	addDropData(activeChar, boss.getNpcId(),  6578,  1,  2,  17,  150000,  0,  0);
                        	addDropData(activeChar, boss.getNpcId(),  6577,  2,  2,  30,  80000,  0,  0);
                        	addDropData(activeChar, boss.getNpcId(),  1000002,  2,  2,  31,  70000,  0,  0);
                        	addDropData(activeChar, boss.getNpcId(),  62002,  1,  1,  20,  75000,  0,  0);
                        	addDropData(activeChar, boss.getNpcId(),  62000,  1,  1,  21,  65000,  0,  0);
                        	addDropData(activeChar, boss.getNpcId(),  97009,  1,  1,  22,  50000,  0,  0);
                        	addDropData(activeChar, boss.getNpcId(),  1000006,  1,  1,  12,  30000,  0,  25);
                        	addDropData(activeChar, boss.getNpcId(),  1000007,  1,  1,  25,  10000,  0,  0);
                            count85++;
                        }
                    	activeChar.sendMessage("Added dropdata on " + count85 + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 86)
                    {
                        int count86 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive86Raids)
                        {
                        	activeChar.sendMessage("Adding dropdata on " + boss.getName());
                            addDropData(activeChar, boss.getNpcId(), 4357, 2500, 2500, 1, 1000000, 10, 0);
                            count86++;
                        }
                    	activeChar.sendMessage("Added dropdata on " + count86 + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 87)
                    {
                        int count87 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive87Raids)
                        {
                        	activeChar.sendMessage("Adding dropdata on " + boss.getName());
                            addDropData(activeChar, boss.getNpcId(), 4357, 2500, 2500, 1, 1000000, 10, 0);
                            count87++;
                        }
                    	activeChar.sendMessage("Added dropdata on " + count87 + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 88)
                    {
                        int count88 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive88Raids)
                        {
                        	activeChar.sendMessage("Adding dropdata on " + boss.getName());
                            addDropData(activeChar, boss.getNpcId(), 4357, 2500, 2500, 1, 1000000, 10, 0);
                            count88++;
                        }
                    	activeChar.sendMessage("Added dropdata on " + count88 + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 89)
                    {
                        int count89 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive89Raids)
                        {
                        	activeChar.sendMessage("Adding dropdata on " + boss.getName());
                            addDropData(activeChar, boss.getNpcId(), 4357, 2500, 2500, 1, 1000000, 10, 0);
                            count89++;
                        }
                    	activeChar.sendMessage("Added dropdata on " + count89 + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 90)
                    {
                        int count90 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive90Raids)
                        {
                        	activeChar.sendMessage("Adding dropdata on " + boss.getName());
                            addDropData(activeChar, boss.getNpcId(), 4357, 2500, 2500, 1, 1000000, 10, 0);
                            count90++;
                        }
                    	activeChar.sendMessage("Added dropdata on " + count90 + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 91)
                    {
                        int count91 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive91Raids)
                        {
                        	activeChar.sendMessage("Adding dropdata on " + boss.getName());
                            addDropData(activeChar, boss.getNpcId(), 4357, 5000, 5000, 1, 1000000, 10, 0);
                            count91++;
                        }
                    	activeChar.sendMessage("Added dropdata on " + count91 + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 92)
                    {
                        int count92 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive92Raids)
                        {
                        	activeChar.sendMessage("Adding dropdata on " + boss.getName());
                            addDropData(activeChar, boss.getNpcId(), 4357, 5000, 5000, 1, 1000000, 10, 0);
                            count92++;
                        }
                    	activeChar.sendMessage("Added dropdata on " + count92 + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 93)
                    {
                        int count93 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive93Raids)
                        {
                        	activeChar.sendMessage("Adding dropdata on " + boss.getName());
                            addDropData(activeChar, boss.getNpcId(), 4357, 5000, 5000, 1, 1000000, 10, 0);
                            count93++;
                        }
                    	activeChar.sendMessage("Added dropdata on " + count93 + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 94)
                    {
                        int count94 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive94Raids)
                        {
                        	activeChar.sendMessage("Adding dropdata on " + boss.getName());
                            addDropData(activeChar, boss.getNpcId(), 4357, 100, 100, 1, 1000000, 10, 0);
                            count94++;
                        }
                    	activeChar.sendMessage("Added dropdata on " + count94 + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 95)
                    {
                        int count95 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive95Raids)
                        {
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName());
                            addDropData(activeChar, boss.getNpcId(), 4357, 100, 100, 1, 1000000, 10, 0);
                            count95++;
                        }
                    	activeChar.sendMessage("Added dropdata on " + count95 + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 96385)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive85Raids)
                        {
                            L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                            if (npcData.getDropData() != null)
                            {
                                for (L2DropCategory cat: npcData.getDropData())
                                {
                                	if (cat != null)
                                	{
	                                    for (L2DropData drop: cat.getAllDrops())
	                                    {
	                                    	deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
	                                        count_drops++;
	                                    }
                                	}
                                }
                            }
                            count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 96386)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive86Raids)
                        {
                            L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                            if (npcData.getDropData() != null)
                            {
                                for (L2DropCategory cat: npcData.getDropData())
                                {
                                    for (L2DropData drop: cat.getAllDrops())
                                    {
                                        deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
                                        count_drops++;
                                    }
                                }
                            }
                            count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 96387)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive87Raids)
                        {
                            L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                            if (npcData.getDropData() != null)
                            {
                                for (L2DropCategory cat: npcData.getDropData())
                                {
                                    for (L2DropData drop: cat.getAllDrops())
                                    {
                                        deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
                                        count_drops++;
                                    }
                                }
                            }
                            count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 96388)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive88Raids)
                        {
                            L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                            if (npcData.getDropData() != null)
                            {
                                for (L2DropCategory cat: npcData.getDropData())
                                {
                                    for (L2DropData drop: cat.getAllDrops())
                                    {
                                        deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
                                        count_drops++;
                                    }
                                }
                            }
                            count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 96389)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        int count96389 = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive89Raids)
                        {
                            L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                            if (npcData.getDropData() != null)
                            {
                                for (L2DropCategory cat: npcData.getDropData())
                                {
                                    for (L2DropData drop: cat.getAllDrops())
                                    {
                                        deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
                                        count_drops++;
                                    }
                                }
                            }
                            count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 96390)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive90Raids)
                        {
                        L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                        if (npcData.getDropData() != null)
                        {
                            for (L2DropCategory cat: npcData.getDropData())
                            {
                                for (L2DropData drop: cat.getAllDrops())
                                {
                                    deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
                                    count_drops++;
                                }
                            }
                        }
                        count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 96391)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive91Raids)
                        {
                            L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                            if (npcData.getDropData() != null)
                            {
                                for (L2DropCategory cat: npcData.getDropData())
                                {
                                    for (L2DropData drop: cat.getAllDrops())
                                    {
                                        deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
                                        count_drops++;
                                    }
                                }
                            }
                            count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 96392)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive92Raids)
                        {
                            L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                            if (npcData.getDropData() != null)
                            {
                                for (L2DropCategory cat: npcData.getDropData())
                                {
                                    for (L2DropData drop: cat.getAllDrops())
                                    {
                                        deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
                                        count_drops++;
                                    }
                                }
                            }
                            count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 96393)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive93Raids)
                        {
                            L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                            if (npcData.getDropData() != null)
                            {
                                for (L2DropCategory cat: npcData.getDropData())
                                {
                                    for (L2DropData drop: cat.getAllDrops())
                                    {
                                        deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
                                        count_drops++;
                                    }
                                }
                            }
                            count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 96394)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive94Raids)
                        {
                            L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                            if (npcData.getDropData() != null)
                            {
                                for (L2DropCategory cat: npcData.getDropData())
                                {
                                    for (L2DropData drop: cat.getAllDrops())
                                    {
                                        deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
                                        count_drops++;
                                    }
                                }
                            }
                            count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 96395)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive95Raids)
                        {
                            L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                            if (npcData.getDropData() != null)
                            {
                                for (L2DropCategory cat: npcData.getDropData())
                                {
                                    for (L2DropData drop: cat.getAllDrops())
                                    {
                                        deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
                                        count_drops++;
                                    }
                                }
                            }
                            count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 963963)
                    {
                    	int count_drops = 0;
                    	int count_raids = 0;
                        for (L2RaidBossInstance boss: L2RaidBossInstance._aliveRaids)
                        {
                            L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(boss.getNpcId());
                            if (npcData.getDropData() != null)
                            {
                                for (L2DropCategory cat: npcData.getDropData())
                                {
                                    for (L2DropData drop: cat.getAllDrops())
                                    {
                                        deleteAllDropData(activeChar, boss.getNpcId(), drop.getItemId(), cat.getCategoryType(), drop.getEnchantLevel());
                                        count_drops++;
                                    }
                                }
                            }
                            count_raids++;
                        }
                    	activeChar.sendMessage("Deleted ["+count_drops+"] dropdata from " + count_raids + " RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10085)
                    {
                        int count85 = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============ Level 85 Raids =================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive85Raids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                            count85++;
                        }
                    	activeChar.sendMessage("A total of [ " + count85 + " ] level 85 RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10086)
                    {
                        int count86 = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============ Level 86 Raids =================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive86Raids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                            count86++;
                        }
                    	activeChar.sendMessage("A total of [ " + count86 + " ] level 86 RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10087)
                    {
                        int count87 = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============ Level 87 Raids =================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive87Raids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                            count87++;
                        }
                    	activeChar.sendMessage("A total of [ " + count87 + " ] level 87 RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10088)
                    {
                        int count88 = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============ Level 88 Raids =================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive88Raids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                            count88++;
                        }
                    	activeChar.sendMessage("A total of [ " + count88 + " ] level 88 RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10089)
                    {
                        int count89 = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============ Level 89 Raids =================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive89Raids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                            count89++;
                        }
                    	activeChar.sendMessage("A total of [ " + count89 + " ] level 89 RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10090)
                    {
                        int count90 = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============ Level 90 Raids =================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive90Raids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                            count90++;
                        }
                    	activeChar.sendMessage("A total of [ " + count90 + " ] level 90 RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10091)
                    {
                        int count91 = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============ Level 91 Raids =================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive91Raids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                            count91++;
                        }
                    	activeChar.sendMessage("A total of [ " + count91 + " ] level 91 RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10092)
                    {
                        int count92 = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============ Level 92 Raids =================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive92Raids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                            count92++;
                        }
                    	activeChar.sendMessage("A total of [ " + count92 + " ] level 92 RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10093)
                    {
                        int count93 = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============ Level 93 Raids =================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive93Raids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                            count93++;
                        }
                    	activeChar.sendMessage("A total of [ " + count93 + " ] level 93 RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10094)
                    {
                        int count94 = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============ Level 94 Raids =================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive94Raids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                            count94++;
                        }
                    	activeChar.sendMessage("A total of [ " + count94 + " ] level 94 RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10095)
                    {
                        int count95 = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============ Level 95 Raids =================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._alive95Raids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                            count95++;
                        }
                    	activeChar.sendMessage("A total of [ " + count95 + " ] level 95 RaidBosses.");
                		showMainPage(activeChar);
                    }
                    if (number == 10000)
                    {
                        int countall = 0;
                    	activeChar.sendMessage("=======================================");
                    	activeChar.sendMessage("============= All Raids =====================");
                    	activeChar.sendMessage("=======================================");
                        for (L2RaidBossInstance boss: L2RaidBossInstance._aliveRaids)
                        {
                        	String status = "";
                        	if (boss.getHpPercent()>1)
                        		status = " (Alive)";
                        	else
                        		status = " (Dead)";
                        	activeChar.sendMessage("[ "+boss.getNpcId() +" ] - " + boss.getName() + status);
                        	countall++;
                        }
                    	activeChar.sendMessage("A total of [ " + countall + " ] RaidBosses.");
                		showMainPage(activeChar);
                    }
                }
                catch (Exception e)
                {
                    _log.fine("parements error: " + command);
                }
            }
            else
            {
                activeChar.sendMessage("Usage: //admin_addraiddrop 85/90/91/94]");
            }
        }
        catch (StringIndexOutOfBoundsException e)
        {
            activeChar.sendMessage("Usage: //add_drop <npc_id> [<item_id> <category> <min> <max> <chance>]");
        }
    }
	else if (command.startsWith("admin_itemlist_debug"))
	{
		ItemLists.getInstance().debug();
	}
	
	return true;
}

private void showMainPage(L2PcInstance activeChar)
{
	NpcHtmlMessage html = new NpcHtmlMessage(5);
	html.setFile("data/html/admin/raiddrop.htm");

	int rbcounter85 = L2RaidBossInstance.getRbCount85();
	int rbcounter86 = L2RaidBossInstance.getRbCount86();
	int rbcounter87 = L2RaidBossInstance.getRbCount87();
	int rbcounter88 = L2RaidBossInstance.getRbCount88();
	int rbcounter89 = L2RaidBossInstance.getRbCount89();
	int rbcounter90 = L2RaidBossInstance.getRbCount90();
	int rbcounter91 = L2RaidBossInstance.getRbCount91();
	int rbcounter92 = L2RaidBossInstance.getRbCount92();
	int rbcounter93 = L2RaidBossInstance.getRbCount93();
	int rbcounter94 = L2RaidBossInstance.getRbCount94();
	int rbcounter95 = L2RaidBossInstance.getRbCount94();
	int rbcounterall = L2RaidBossInstance.getTotalRbCount();

	html.replace("%raid85%", String.valueOf(rbcounter85));
	html.replace("%raid86%", String.valueOf(rbcounter86));
	html.replace("%raid87%", String.valueOf(rbcounter87));
	html.replace("%raid88%", String.valueOf(rbcounter88));
	html.replace("%raid89%", String.valueOf(rbcounter89));
	html.replace("%raid90%", String.valueOf(rbcounter90));
	html.replace("%raid91%", String.valueOf(rbcounter91));
	html.replace("%raid92%", String.valueOf(rbcounter92));
	html.replace("%raid93%", String.valueOf(rbcounter93));
	html.replace("%raid94%", String.valueOf(rbcounter94));
	html.replace("%raid95%", String.valueOf(rbcounter95));
	html.replace("%raidall%", String.valueOf(rbcounterall));
	
	activeChar.sendPacket(html);
}

private static String getItemName(int itemId)
{
	if (itemId > 1000000)
	{
		return ItemLists.getInstance().getListName(itemId);
	}
	
	return ItemTable.getInstance().getTemplate(itemId).getName();
}

private void editShopItem(L2PcInstance activeChar, String[] args)
{
	int tradeListID = Integer.parseInt(args[1]);
	int itemID = Integer.parseInt(args[2]);
	L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
	
	L2Item item = ItemTable.getInstance().getTemplate(itemID);
	if (tradeList.getPriceForItemId(itemID) < 0)
	{
		return;
	}
	
	if (args.length > 3)
	{
		long price = Long.parseLong(args[3]);
		int order = findOrderTradeList(itemID, tradeList.getPriceForItemId(itemID), tradeListID);
		
		tradeList.replaceItem(itemID, Long.parseLong(args[3]));
		updateTradeList(itemID, price, tradeListID, order);
		
		activeChar.sendMessage("Updated price for " + item.getName() + " in Trade List " + tradeListID);
		showShopList(activeChar, tradeListID, 1);
		return;
	}
	
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	
	final String replyMSG = StringUtil.concat(
			"<html><title>Merchant Shop Item Edit</title>" +
			"<body>" +
			"<br>Edit an entry in merchantList." +
			"<br>Editing Item: ",
			item.getName(),
			"<table>" +
			"<tr><td width=100>Property</td><td width=100>Edit Field</td><td width=100>Old Value</td></tr>" +
			"<tr><td><br></td><td></td></tr>" +
			"<tr><td>Price</td><td><edit var=\"price\" width=80></td><td>",
			String.valueOf(tradeList.getPriceForItemId(itemID)),
			"</td></tr>" +
			"</table>" +
			"<center><br><br><br>" +
			"<button value=\"Save\" action=\"bypass -h admin_editShopItem ",
			String.valueOf(tradeListID),
			" ",
			String.valueOf(itemID),
			" $price\"  width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"<br><button value=\"Back\" action=\"bypass -h admin_showShopList ",
			String.valueOf(tradeListID),
			" 1\"  width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"</center>" +
			"</body></html>"
	);
	
	adminReply.setHtml(replyMSG);
	activeChar.sendPacket(adminReply);
}

private void delShopItem(L2PcInstance activeChar, String[] args)
{
	int tradeListID = Integer.parseInt(args[1]);
	int itemID = Integer.parseInt(args[2]);
	L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
	
	if (tradeList.getPriceForItemId(itemID) < 0)
		return;
	
	if (args.length > 3)
	{
		int order = findOrderTradeList(itemID, tradeList.getPriceForItemId(itemID), tradeListID);
		
		tradeList.removeItem(itemID);
		deleteTradeList(tradeListID, order);
		
		activeChar.sendMessage("Deleted " + getItemName(itemID) + " from Trade List " + tradeListID);
		showShopList(activeChar, tradeListID, 1);
		return;
	}
	
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	
	final String replyMSG = StringUtil.concat(
			"<html><title>Merchant Shop Item Delete</title>" +
			"<body>" +
			"<br>Delete entry in merchantList." +
			"<br>Item to Delete: ",
			getItemName(itemID),
			"<table>" +
			"<tr><td width=100>Property</td><td width=100>Value</td></tr>" +
			"<tr><td><br></td><td></td></tr>" +
			"<tr><td>Price</td><td>",
			String.valueOf(tradeList.getPriceForItemId(itemID)),
			"</td></tr>" +
			"</table>" +
			"<center><br><br><br>" +
			"<button value=\"Confirm\" action=\"bypass -h admin_delShopItem ",
			String.valueOf(tradeListID),
			" ",
			String.valueOf(itemID),
			" 1\"  width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"<br><button value=\"Back\" action=\"bypass -h admin_showShopList ",
			String.valueOf(tradeListID),
			" 1\"  width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">",
			"</center>" +
			"</body></html>"
	);
	
	adminReply.setHtml(replyMSG);
	activeChar.sendPacket(adminReply);
}

private void addShopItem(L2PcInstance activeChar, String[] args)
{
	int tradeListID = Integer.parseInt(args[1]);
	
	L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
	if (tradeList == null)
	{
		activeChar.sendMessage("TradeList not found!");
		return;
	}
	
	if (args.length > 3)
	{
		int order = tradeList.getItems().size() + 1; // last item order + 1
		int itemID = Integer.parseInt(args[2]);
		long price = Long.parseLong(args[3]);
		
		L2TradeItem newItem = new L2TradeItem(itemID);
		newItem.setPrice(price);
		newItem.setMaxCount(-1);
		tradeList.addItem(newItem);
		storeTradeList(itemID, price, tradeListID, order);
		
		activeChar.sendMessage("Added " + getItemName(itemID) + " to Trade List " + tradeList.getListId());
		showShopList(activeChar, tradeListID, 1);
		return;
	}
	
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	
	final String replyMSG = StringUtil.concat(
			"<html><title>Merchant Shop Item Add</title>" +
			"<body>" +
			"<br>Add a new entry in merchantList." +
			"<table>" +
			"<tr><td width=100>Property</td><td>Edit Field</td></tr>" +
			"<tr><td><br></td><td></td></tr>" +
			"<tr><td>ItemID</td><td><edit var=\"itemID\" width=80></td></tr>" +
			"<tr><td>Price</td><td><edit var=\"price\" width=80></td></tr>" +
			"</table>" +
			"<center><br><br><br>" +
			"<button value=\"Save\" action=\"bypass -h admin_addShopItem ",
			String.valueOf(tradeListID),
			" $itemID $price\"  width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"<br><button value=\"Back\" action=\"bypass -h admin_showShopList ",
			String.valueOf(tradeListID),
			" 1\"  width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"</center>" +
			"</body></html>"
	);
	
	adminReply.setHtml(replyMSG);
	activeChar.sendPacket(adminReply);
}

private void showShopList(L2PcInstance activeChar, int tradeListID, int page)
{
	L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
	if (page > tradeList.getItems().size() / PAGE_LIMIT + 1 || page < 1)
		return;
	
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	adminReply.setHtml(itemListHtml(tradeList, page));
	activeChar.sendPacket(adminReply);
	
}

private String itemListHtml(L2TradeList tradeList, int page)
{
	final StringBuilder replyMSG = new StringBuilder();
	
	StringUtil.append(replyMSG,
			"<html><title>Merchant Shop List Page: ",
			String.valueOf(page),
			"</title>" +
			"<body>" +
			"<br>Edit, add or delete entries in a merchantList." +
			"<table>" +
			"<tr><td width=150>Item Name</td><td width=60>Price</td><td width=40>Delete</td></tr>"
	);
	
	int start = ((page - 1) * PAGE_LIMIT);
	int end = Math.min(((page - 1) * PAGE_LIMIT) + (PAGE_LIMIT - 1), tradeList.getItems().size() - 1);
	for (L2TradeItem item : tradeList.getItems(start, end + 1))
	{
		StringUtil.append(replyMSG,
				"<tr><td><a action=\"bypass -h admin_editShopItem ",
				String.valueOf(tradeList.getListId()),
				" ",
				String.valueOf(item.getItemId()),
				"\">",
				getItemName(item.getItemId()),
				"</a></td>" +
				"<td>",
				String.valueOf(item.getPrice()),
				"</td>" +
				"<td><button value=\"Del\" action=\"bypass -h admin_delShopItem ",
				String.valueOf(tradeList.getListId()),
				" ",
				String.valueOf(item.getItemId()),
				"\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" +
				"</tr>"
		);
	}//*/
	StringUtil.append(replyMSG, "<tr>");
	int min = 1;
	int max = tradeList.getItems().size() / PAGE_LIMIT + 1;
	if (page > 1)
	{
		StringUtil.append(replyMSG,
				"<td><button value=\"Page",
				String.valueOf(page - 1),
				"\" action=\"bypass -h admin_showShopList ",
				String.valueOf(tradeList.getListId()),
				" ",
				String.valueOf(page - 1),
				"\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>"
		);
	}
	if (page < max)
	{
		if (page <= min) {
			StringUtil.append(replyMSG, "<td></td>");
		}
		
		StringUtil.append(replyMSG,
				"<td><button value=\"Page",
				String.valueOf(page + 1),
				"\" action=\"bypass -h admin_showShopList ",
				String.valueOf(tradeList.getListId()),
				" ",
				String.valueOf(page + 1),
				"\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>"
		);
	}
	
	StringUtil.append(replyMSG,
			"</tr><tr><td>.</td></tr>" +
			"</table>" +
			"<center>" +
			"<button value=\"Add\" action=\"bypass -h admin_addShopItem ",
			String.valueOf(tradeList.getListId()),
			"\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"<button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"</center></body></html>"
	);
	
	return replyMSG.toString();
}

private void showShop(L2PcInstance activeChar, int merchantID)
{
	List<L2TradeList> tradeLists = getTradeLists(merchantID);
	if (tradeLists == null)
	{
		activeChar.sendMessage("Unknown npc template Id: " + merchantID);
		return;
	}
	
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	
	final StringBuilder replyMSG = new StringBuilder();
	StringUtil.append(replyMSG,
			"<html><title>Merchant Shop Lists</title>" +
			"<body>"
	);
	
	if (activeChar.getTarget() instanceof L2MerchantInstance)
	{
		String mpcName = ((L2MerchantInstance) activeChar.getTarget()).getMpc().getName();
		StringUtil.append(replyMSG,
				"<br>NPC: ",
				activeChar.getTarget().getName(),
				"<br>Price Config: ",
				mpcName);
	}
	
	StringUtil.append(replyMSG,
			"<br>Select a list to view" +
			"<table>" +
			"<tr><td>Mecrchant List ID</td></tr>"
	);
	
	for (L2TradeList tradeList : tradeLists)
	{
		if (tradeList != null) {
			StringUtil.append(replyMSG,
					"<tr><td><a action=\"bypass -h admin_showShopList ",
					String.valueOf(tradeList.getListId()),
					" 1\">Trade List ",
					String.valueOf(tradeList.getListId()),
					"</a></td></tr>"
			);
		}
	}
	
	StringUtil.append(replyMSG,
			"</table>" +
			"<center>" +
			"<button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"</center></body></html>"
	);
	
	adminReply.setHtml(replyMSG.toString());
	activeChar.sendPacket(adminReply);
}

private void storeTradeList(int itemID, long price, int tradeListID, int order)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement stmt = con.prepareStatement("INSERT INTO merchant_buylists (`item_id`,`price`,`shop_id`,`order`) VALUES (?,?,?,?)");
		stmt.setInt(1, itemID);
		stmt.setLong(2, price);
		stmt.setInt(3, tradeListID);
		stmt.setInt(4, order);
		stmt.execute();
		stmt.close();
	}
	catch (SQLException esql)
	{
		esql.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}

private void updateTradeList(int itemID, long price, int tradeListID, int order)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement stmt = con.prepareStatement("UPDATE merchant_buylists SET `price` = ? WHERE `shop_id` = ? AND `order` = ?");
		stmt.setLong(1, price);
		stmt.setInt(2, tradeListID);
		stmt.setInt(3, order);
		stmt.execute();
		stmt.close();
	}
	catch (SQLException esql)
	{
		esql.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}

private void deleteTradeList(int tradeListID, int order)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement stmt = con.prepareStatement("DELETE FROM merchant_buylists WHERE `shop_id` = ? AND `order` = ?");
		stmt.setInt(1, tradeListID);
		stmt.setInt(2, order);
		stmt.execute();
		stmt.close();
	}
	catch (SQLException esql)
	{
		esql.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}

private int findOrderTradeList(int itemID, long price, int tradeListID)
{
	Connection con = null;
	int order = 0;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement stmt = con.prepareStatement("SELECT * FROM merchant_buylists WHERE `shop_id` = ? AND `item_id` = ? AND `price` = ?");
		stmt.setInt(1, tradeListID);
		stmt.setInt(2, itemID);
		stmt.setLong(3, price);
		ResultSet rs = stmt.executeQuery();
		rs.first();
		
		order = rs.getInt("order");
		
		stmt.close();
		rs.close();
	}
	catch (SQLException esql)
	{
		esql.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	return order;
}

private List<L2TradeList> getTradeLists(int merchantID)
{
	String target = "npc_%objectId%_Buy";
	
	String content = HtmCache.getInstance().getHtm("data/html/merchant/" + merchantID + ".htm");
	
	if (content == null)
	{
		content = HtmCache.getInstance().getHtm("data/html/merchant/30001.htm");
		if (content == null)
			return null;
	}
	
	List<L2TradeList> tradeLists = new FastList<L2TradeList>();
	
	String[] lines = content.split("\n");
	int pos = 0;
	
	for (String line : lines)
	{
		pos = line.indexOf(target);
		if (pos >= 0)
		{
			int tradeListID = Integer.decode((line.substring(pos + target.length() + 1)).split("\"")[0]);
			tradeLists.add(TradeController.getInstance().getBuyList(tradeListID));
		}
	}
	return tradeLists;
}

public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}

private void Show_Npc_Property(L2PcInstance activeChar, L2NpcTemplate npc)
{
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	String content = HtmCache.getInstance().getHtm("data/html/admin/editnpc.htm");
	
	if (content != null)
	{
		adminReply.setHtml(content);
		adminReply.replace("%npcId%", String.valueOf(npc.npcId));
		adminReply.replace("%templateId%", String.valueOf(npc.idTemplate));
		adminReply.replace("%name%", npc.name);
		adminReply.replace("%serverSideName%", npc.serverSideName == true ? "1" : "0");
		adminReply.replace("%title%", npc.title);
		adminReply.replace("%serverSideTitle%", npc.serverSideTitle == true ? "1" : "0");
		adminReply.replace("%collisionRadius%", String.valueOf(npc.getCollisionRadius()));
		adminReply.replace("%collisionHeight%", String.valueOf(npc.getCollisionHeight()));
		adminReply.replace("%level%", String.valueOf(npc.level));
		adminReply.replace("%type%", String.valueOf(npc.type));
		adminReply.replace("%attackRange%", String.valueOf(npc.baseAtkRange));
		adminReply.replace("%hp%", String.valueOf(npc.baseHpMax));
		adminReply.replace("%mp%", String.valueOf(npc.baseMpMax));
		adminReply.replace("%hpRegen%", String.valueOf(npc.baseHpReg));
		adminReply.replace("%mpRegen%", String.valueOf(npc.baseMpReg));
		adminReply.replace("%exp%", String.valueOf(npc.rewardExp));
		adminReply.replace("%sp%", String.valueOf(npc.rewardSp));
		adminReply.replace("%pAtk%", String.valueOf(npc.basePAtk));
		adminReply.replace("%pDef%", String.valueOf(npc.basePDef));
		adminReply.replace("%mAtk%", String.valueOf(npc.baseMAtk));
		adminReply.replace("%mDef%", String.valueOf(npc.baseMDef));
		adminReply.replace("%pAtkSpd%", String.valueOf(npc.basePAtkSpd));
		adminReply.replace("%mAtkSpd%", String.valueOf(npc.baseMAtkSpd));
		adminReply.replace("%aggro%", String.valueOf(npc.aggroRange));
		adminReply.replace("%rHand%", String.valueOf(npc.rhand));
		adminReply.replace("%lHand%", String.valueOf(npc.lhand));
		adminReply.replace("%walkSpd%", String.valueOf(npc.baseWalkSpd));
		adminReply.replace("%runSpd%", String.valueOf(npc.baseRunSpd));
		adminReply.replace("%factionId%", npc.factionId == null ? "" : npc.factionId);
		adminReply.replace("%factionRange%", String.valueOf(npc.factionRange));
		adminReply.replace("%isUndead%", npc.isUndead ? "1" : "0");
		adminReply.replace("%atkele%", npc.baseFire+";"+npc.baseWater+";"+npc.baseWind+";"+npc.baseEarth+";"+npc.baseHoly+";"+npc.baseDark);
		adminReply.replace("%defele%", npc.baseFireRes+";"+npc.baseWaterRes+";"+npc.baseWindRes+";"+npc.baseEarthRes+";"+npc.baseHolyRes+";"+npc.baseDarkRes);
		adminReply.replace("%rndwalk%", String.valueOf(npc._randomWalkRange));
		adminReply.replace("%solomob%", String.valueOf(npc._soloMob));
		adminReply.replace("%ssrate%", String.valueOf(npc.ssRate));
		adminReply.replace("%ssgrade%", String.valueOf(npc.ssGrade));
		adminReply.replace("%ai%", npc.AI.toString());
	}
	else
		adminReply.setHtml("<html><head><body>File not found: data/html/admin/editnpc.htm</body></html>");
	activeChar.sendPacket(adminReply);
}

private void save_npc_property(L2PcInstance activeChar, String command)
{
	String[] commandSplit = command.split(" ");
	
	if (commandSplit.length < 4)
		return;
	
	StatsSet newNpcData = new StatsSet();
	
	try
	{
		newNpcData.set("npcId", commandSplit[1]);
		
		String statToSet = commandSplit[2];
		String value = commandSplit[3];
		
		if (commandSplit.length > 4)
		{
			for (int i = 0; i < commandSplit.length - 3; i++)
				value += " " + commandSplit[i + 4];
		}
		
		if (statToSet.equals("templateId"))
			newNpcData.set("idTemplate", Integer.parseInt(value));
		else if (statToSet.equals("name"))
			newNpcData.set("name", value);
		else if (statToSet.equals("serverSideName"))
			newNpcData.set("serverSideName", Integer.parseInt(value));
		else if (statToSet.equals("title"))
			newNpcData.set("title", value);
		else if (statToSet.equals("serverSideTitle"))
			newNpcData.set("serverSideTitle", Integer.parseInt(value) == 1 ? 1 : 0);
		else if (statToSet.equals("collisionRadius"))
			newNpcData.set("collision_radius", Integer.parseInt(value));
		else if (statToSet.equals("collisionHeight"))
			newNpcData.set("collision_height", Integer.parseInt(value));
		else if (statToSet.equals("level"))
			newNpcData.set("level", Integer.parseInt(value));
		else if (statToSet.equals("sex"))
		{
			int intValue = Integer.parseInt(value);
			newNpcData.set("sex", intValue == 0 ? "male" : intValue == 1 ? "female" : "etc");
		}
		else if (statToSet.equals("type"))
		{
			Class.forName("net.sf.l2j.gameserver.model.actor.instance." + value + "Instance");
			newNpcData.set("type", value);
		}
		else if (statToSet.equals("attackRange"))
			newNpcData.set("attackrange", Integer.parseInt(value));
		else if (statToSet.equals("hp"))
			newNpcData.set("hp", Integer.parseInt(value));
		else if (statToSet.equals("mp"))
			newNpcData.set("mp", Integer.parseInt(value));
		else if (statToSet.equals("hpRegen"))
			newNpcData.set("hpreg", Integer.parseInt(value));
		else if (statToSet.equals("mpRegen"))
			newNpcData.set("mpreg", Integer.parseInt(value));
		else if (statToSet.equals("str"))
			newNpcData.set("str", Integer.parseInt(value));
		else if (statToSet.equals("con"))
			newNpcData.set("con", Integer.parseInt(value));
		else if (statToSet.equals("dex"))
			newNpcData.set("dex", Integer.parseInt(value));
		else if (statToSet.equals("int"))
			newNpcData.set("int", Integer.parseInt(value));
		else if (statToSet.equals("wit"))
			newNpcData.set("wit", Integer.parseInt(value));
		else if (statToSet.equals("men"))
			newNpcData.set("men", Integer.parseInt(value));
		else if (statToSet.equals("exp"))
			newNpcData.set("exp", Integer.parseInt(value));
		else if (statToSet.equals("sp"))
			newNpcData.set("sp", Integer.parseInt(value));
		else if (statToSet.equals("pAtk"))
			newNpcData.set("patk", Integer.parseInt(value));
		else if (statToSet.equals("pDef"))
			newNpcData.set("pdef", Integer.parseInt(value));
		else if (statToSet.equals("mAtk"))
			newNpcData.set("matk", Integer.parseInt(value));
		else if (statToSet.equals("mDef"))
			newNpcData.set("mdef", Integer.parseInt(value));
		else if (statToSet.equals("pAtkSpd"))
			newNpcData.set("atkspd", Integer.parseInt(value));
		else if (statToSet.equals("aggro"))
			newNpcData.set("aggro", Integer.parseInt(value));
		else if (statToSet.equals("mAtkSpd"))
			newNpcData.set("matkspd", Integer.parseInt(value));
		else if (statToSet.equals("rHand"))
			newNpcData.set("rhand", Integer.parseInt(value));
		else if (statToSet.equals("lHand"))
			newNpcData.set("lhand", Integer.parseInt(value));
		else if (statToSet.equals("armor"))
			newNpcData.set("armor", Integer.parseInt(value));
		else if (statToSet.equals("runSpd"))
			newNpcData.set("runspd", Integer.parseInt(value));
		else if (statToSet.equals("factionId"))
			newNpcData.set("faction_id", value);
		else if (statToSet.equals("factionRange"))
			newNpcData.set("faction_range", Integer.parseInt(value));
		else if (statToSet.equals("isUndead"))
			newNpcData.set("isUndead", Integer.parseInt(value) == 1 ? 1 : 0);
		else if (statToSet.equals("absorbLevel"))
		{
			int intVal = Integer.parseInt(value);
			newNpcData.set("absorb_level", intVal < 0 ? 0 : intVal > 12 ? 0 : intVal);
		}
		else if (statToSet.equals("atk_elements"))
		{
			newNpcData.set("atk_elements", value);
		}
		else if (statToSet.equals("def_elements"))
		{
			newNpcData.set("def_elements", value);
		}
		else if (statToSet.equals("randomwalkrange"))
		{
			newNpcData.set("randomwalkrange", Integer.parseInt(value));
		}
		else if (statToSet.equals("solomob"))
		{
			newNpcData.set("solomob", Integer.parseInt(value));
		}
		else if (statToSet.equals("ssrate"))
		{
			newNpcData.set("ss_rate", Integer.parseInt(value));
		}
		else if (statToSet.equals("ssgrade"))
		{
			newNpcData.set("ss_grade", Integer.parseInt(value));
		}
		else if (statToSet.equals("ai"))
		{
			newNpcData.set("AI", value);
		}
	}
	catch (Exception e)
	{
		_log.warning("Error saving new npc value: " + e);
	}
	
	NpcTable.getInstance().saveNpc(newNpcData);
	
	FakePcsTable.getInstance().load();
	int npcId = newNpcData.getInteger("npcId");
	NpcTable.getInstance().reloadNpc(npcId);
	Show_Npc_Property(activeChar, NpcTable.getInstance().getTemplate(npcId));
}

private void showNpcDropList(L2PcInstance activeChar, int npcId)
{
	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
	if (npcData == null)
	{
		activeChar.sendMessage("unknown npc template id" + npcId);
		return;
	}
	
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	
	final StringBuilder replyMSG = new StringBuilder(2900);
	replyMSG.append("<html><title>");
	replyMSG.append(npcData.name);
	replyMSG.append('(');
	replyMSG.append(npcData.npcId);
	replyMSG.append(")</title>");
	replyMSG.append("<body>");
	replyMSG.append("<table>");
	replyMSG.append("<tr><td width=55>category enchant</td><td width=125>item[id]</td><td>type</td><td>del</td></tr>");
	
	if (npcData.getDropData() != null)
		for (L2DropCategory cat : npcData.getDropData())
			for (L2DropData drop : cat.getAllDrops())
			{
				replyMSG.append("<tr><td width=55><a action=\"bypass -h admin_edit_drop ");
				replyMSG.append(npcData.getNpcId());
				replyMSG.append(' ');
				replyMSG.append(drop.getItemId());
				replyMSG.append(' ');
				replyMSG.append(cat.getCategoryType());
				replyMSG.append(' ');
				replyMSG.append(drop.getEnchantLevel());
				replyMSG.append("\">");
				replyMSG.append(cat.getCategoryType());
				replyMSG.append(' '+"+");
				replyMSG.append(drop.getEnchantLevel());
				replyMSG.append("</a></td><td width=125>");
				replyMSG.append(getItemName(drop.getItemId()));
				replyMSG.append(' ');
				replyMSG.append('[');
				replyMSG.append(drop.getItemId());
				replyMSG.append("]</td><td>");
				replyMSG.append((drop.isQuestDrop() ? "Q" : (cat.isSweep() ? "S" : "D")));
				replyMSG.append("</td><td><a action=\"bypass -h admin_del_drop ");
				replyMSG.append(npcData.npcId);
				replyMSG.append(' ');
				replyMSG.append(drop.getItemId());
				replyMSG.append(' ');
				replyMSG.append(cat.getCategoryType());
				replyMSG.append(' ');
				replyMSG.append(drop.getEnchantLevel());
				replyMSG.append("\">del</a></td></tr>");
			}
	
	replyMSG.append("</table><br>");
	replyMSG.append("<center>");
	replyMSG.append("<button value=\"Add DropData\" action=\"bypass -h admin_add_drop ");
	replyMSG.append(npcId);
	replyMSG.append("\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
	replyMSG.append("<br><button value=\"NPC Info\" action=\"bypass -h admin_shift_click\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
	replyMSG.append("<br><br><button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
	replyMSG.append("</center></body></html>");
	
	adminReply.setHtml(replyMSG.toString());
	activeChar.sendPacket(adminReply);
	
}

private void showEditDropData(L2PcInstance activeChar, int npcId, int itemId, int category, int enchant)
{
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		PreparedStatement statement = con.prepareStatement("SELECT mobId, itemId, min, max, category, chance, enchant, partydropmulti FROM droplist WHERE mobId = ? AND itemId = ? AND category = ? AND enchant = ?");
		statement.setInt(1, npcId);
		statement.setInt(2, itemId);
		statement.setInt(3, category);
		statement.setInt(4, enchant);
		ResultSet dropData = statement.executeQuery();
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		
		final StringBuilder replyMSG = new StringBuilder();
		replyMSG.append("<html><title>dropdata: (");
		replyMSG.append(npcId);
		replyMSG.append(' ');
		replyMSG.append(itemId);
		replyMSG.append(' ');
		replyMSG.append(category);
		replyMSG.append(' ');
		replyMSG.append(enchant);
		replyMSG.append(")</title>");
		replyMSG.append("<body>");
		
		if (dropData.next())
		{
			replyMSG.append("<table>");
			replyMSG.append("<tr><td height=25>NPC Name</td><td>");
			replyMSG.append(NpcTable.getInstance().getTemplate(dropData.getInt("mobId")).name);
			replyMSG.append("</td></tr>");
			replyMSG.append("<tr><td height=25>Item Name</td><td>");
			replyMSG.append(getItemName(dropData.getInt("itemId")));
			replyMSG.append('(');
			replyMSG.append(dropData.getInt("itemId"));
			replyMSG.append(")</td></tr>");
			replyMSG.append("<tr><td height=25>Category</td><td>" + ((category == -1) ? "sweep" : Integer.toString(category)) + "</td></tr>");
			replyMSG.append("<tr><td height=25>MIN(" + dropData.getInt("min") + ")</td><td><edit var=\"min\" width=80></td></tr>");
			replyMSG.append("<tr><td height=25>MAX(" + dropData.getInt("max") + ")</td><td><edit var=\"max\" width=80></td></tr>");
			replyMSG.append("<tr><td height=25>CHANCE(" + dropData.getInt("chance") + ")</td><td><edit var=\"chance\" width=80></td></tr>");
			replyMSG.append("<tr><td height=25>PARTYDROP(" + dropData.getInt("partydropmulti") + ")</td><td><edit var=\"partydrop\" width=80></td></tr>");
			replyMSG.append("<tr><td height=25>Enchant</td><td>" + "+" + Integer.toString(enchant) + "</td></tr>");
			replyMSG.append("</table><br>");
			
			replyMSG.append("<center>");
			replyMSG.append("<button value=\"Save Modify\" action=\"bypass -h admin_edit_drop " + npcId + " " + itemId + " " + category + " " + enchant
					+ " $min $max $chance $partydrop\"  width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			replyMSG.append("<br><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + dropData.getInt("mobId") + "\"  width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			replyMSG.append("</center>");
		}
		
		dropData.close();
		statement.close();
		
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		
		activeChar.sendPacket(adminReply);
	}
	catch (Exception e)
	{
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
}

private void showAddDropData(L2PcInstance activeChar, L2NpcTemplate npcData)
{
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	
	final String replyMSG = StringUtil.concat(
			"<html><title>Add dropdata to ",
			npcData.name,
			"(",
			String.valueOf(npcData.npcId),
			")</title>" +
			"<body>" +
			"<table>" +
			"<tr><td height=25>Item-Id</td><td><edit var=\"itemId\" width=80></td></tr>" +
			"<tr><td height=25>MIN</td><td><edit var=\"min\" width=80></td></tr>" +
			"<tr><td height=25>MAX</td><td><edit var=\"max\" width=80></td></tr>" +
			"<tr><td height=25>CATEGORY(sweep=-1)</td><td><edit var=\"category\" width=80></td></tr>" +
			"<tr><td height=25>CHANCE(0-1000000)</td><td><edit var=\"chance\" width=80></td></tr>" +
			"<tr><td height=25>PARTYDROP(0-10)</td><td><edit var=\"partydrop\" width=80></td></tr>" +
			"<tr><td height=25>ENCHANT</td><td><edit var=\"enchant\" width=80></td></tr>" +
			"</table><br>" +
			"<center>" +
			"<button value=\"SAVE\" action=\"bypass -h admin_add_drop ",
			String.valueOf(npcData.npcId),
			" $itemId $category $min $max $chance $partydrop $enchant\"  width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"<br><button value=\"DropList\" action=\"bypass -h admin_show_droplist ",
			String.valueOf(npcData.npcId),
			"\"  width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
			"</center>" +
			"</body></html>"
	);
	adminReply.setHtml(replyMSG);
	
	activeChar.sendPacket(adminReply);
}

private void updateDropData(L2PcInstance activeChar, int npcId, int itemId, int min, int max, int category, int chance, int partydrop, int enchant)
{
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		PreparedStatement statement = con.prepareStatement("UPDATE droplist SET min=?, max=?, chance=?, partydropmulti=? WHERE mobId=? AND itemId=? AND category=? AND enchant=?");
		statement.setInt(1, min);
		statement.setInt(2, max);
		statement.setInt(3, chance);
		statement.setInt(4, partydrop);
		statement.setInt(5, npcId);
		statement.setInt(6, itemId);
		statement.setInt(7, category);
		statement.setInt(8, enchant);
		
		statement.execute();
		statement.close();
		
		PreparedStatement statement2 = con.prepareStatement("SELECT mobId FROM droplist WHERE mobId=? AND itemId=? AND category=? AND enchant=?");
		statement2.setInt(1, npcId);
		statement2.setInt(2, itemId);
		statement2.setInt(3, category);
		statement2.setInt(4, enchant);
		
		ResultSet npcIdRs = statement2.executeQuery();
		if (npcIdRs.next())
			npcId = npcIdRs.getInt("mobId");
		npcIdRs.close();
		statement2.close();
		
		if (npcId > 0)
		{
			reLoadNpcDropList(npcId);
			
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			final String replyMSG = StringUtil.concat(
					"<html><title>Drop data modify complete!</title>" +
					"<body>" +
					"<center><button value=\"DropList\" action=\"bypass -h admin_show_droplist ",
					String.valueOf(npcId),
					"\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>" +
					"</body></html>"
			);
			
			adminReply.setHtml(replyMSG);
			activeChar.sendPacket(adminReply);
		}
		else
			activeChar.sendMessage("unknown error!");
	}
	catch (Exception e)
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
		{
		}
	}
}

private void addDropData(L2PcInstance activeChar, int npcId, int itemId, int min, int max, int category, int chance, int partydrop, int enchant)
{
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		PreparedStatement statement = con.prepareStatement("INSERT INTO droplist(mobId, itemId, min, max, category, chance, partydropmulti, enchant) values(?,?,?,?,?,?,?,?)");
		statement.setInt(1, npcId);
		statement.setInt(2, itemId);
		statement.setInt(3, min);
		statement.setInt(4, max);
		statement.setInt(5, category);
		statement.setInt(6, chance);
		statement.setInt(7, partydrop);
		statement.setInt(8, enchant);
		statement.execute();
		statement.close();
		
		reLoadNpcDropList(npcId);
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		final String replyMSG = StringUtil.concat(
				"<html><title>Add drop data complete!</title>" +
				"<body>" +
				"<center><button value=\"Continue add\" action=\"bypass -h admin_add_drop ",
				String.valueOf(npcId),
				"\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
				"<br><br><button value=\"DropList\" action=\"bypass -h admin_show_droplist ",
				String.valueOf(npcId),
				"\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
				"</center></body></html>"
		);
		
		adminReply.setHtml(replyMSG);
		activeChar.sendPacket(adminReply);
	}
	catch (Exception e)
	{
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
}
private void deleteAllDropData(L2PcInstance activeChar, int npcId, int itemId, int category, int enchant)
{
    Connection con = null;
    L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
    if (npcData == null)
    {
        return;
    }
    try
    {
        con = L2DatabaseFactory.getInstance().getConnection();
        if (npcId > 0)
        {
            PreparedStatement statement2 = con.prepareStatement("DELETE FROM droplist WHERE mobId=? AND itemId=? AND category=? AND enchant=?");
                statement2.setInt(1, npcId);
                statement2.setInt(2, itemId);
                statement2.setInt(3, category);
                statement2.setInt(4, enchant);
                statement2.execute();
                statement2.close();
            statement2.executeQuery();
            statement2.close();
            reLoadNpcDropList(npcId);
        }
        NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
        String replyMSG = StringUtil.concat(new String[]
        {
            "<html><title>Delete drop data(",
            String.valueOf(npcId),
            ", ",
            String.valueOf(itemId),
            ", ",
            String.valueOf(category),
            ", +",
            String.valueOf(enchant),
            ")complete</title><body><center><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + npcId + "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>" + "</body></html>"
        });
        adminReply.setHtml(replyMSG);
        activeChar.sendPacket(adminReply);
    }
    catch (Exception exception)
    {
        try
        {
            con.close();
        }
        catch (Exception exception1)
        {}
    }
    finally
    {
        try
        {
            con.close();
        }
        catch (Exception exception)
        {}
    }
}
private void deleteDropData(L2PcInstance activeChar, int npcId, int itemId, int category, int enchant)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		if (npcId > 0)
		{
			PreparedStatement statement2 = con.prepareStatement("DELETE FROM droplist WHERE mobId=? AND itemId=? AND category=? AND enchant=?");
			statement2.setInt(1, npcId);
			statement2.setInt(2, itemId);
			statement2.setInt(3, category);
			statement2.setInt(4, enchant);
			statement2.execute();
			statement2.close();
			
			reLoadNpcDropList(npcId);
			
//			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
//			final String replyMSG = StringUtil.concat(
//					"<html><title>Delete drop data(",
//					String.valueOf(npcId),
//					", ",
//					String.valueOf(itemId),
//					", ",
//					String.valueOf(category),
//					", +",
//					String.valueOf(enchant),
//					")complete</title>" +
//					"<body>" +
//					"<center><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + npcId + "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>" +
//					"</body></html>"
//			);
			
			//adminReply.setHtml(replyMSG);
			//activeChar.sendPacket(adminReply);
			//showNpcDropList(activeChar, npcId);
			
		}
	}
	catch (Exception e)
	{
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
	
}
private void delDrop(L2PcInstance activeChar, int npcId, int itemId, int category, int enchant)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		if (npcId > 0)
		{
			PreparedStatement statement2 = con.prepareStatement("DELETE FROM droplist WHERE mobId=? AND itemId=? AND category=? AND enchant=?");
			statement2.setInt(1, npcId);
			statement2.setInt(2, itemId);
			statement2.setInt(3, category);
			statement2.setInt(4, enchant);
			statement2.execute();
			statement2.close();
			
			reLoadNpcDropList(npcId);
			
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			final String replyMSG = StringUtil.concat(
					"<html><title>Delete drop data(",
					String.valueOf(npcId),
					", ",
					String.valueOf(itemId),
					", ",
					String.valueOf(category),
					", +",
					String.valueOf(enchant),
					")complete</title>" +
					"<body>" +
					"<center><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + npcId + "\" width=100 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>" +
					"</body></html>"
			);
			
			//adminReply.setHtml(replyMSG);
			//activeChar.sendPacket(adminReply);
			//showNpcDropList(activeChar, npcId);
			
		}
	}
	catch (Exception e)
	{
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
	
}
private void reLoadNpcDropList(int npcId)
{
	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
	if (npcData == null)
		return;
	
	// reset the drop lists
	npcData.clearAllDropData();
	
	// get the drops
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		L2DropData dropData = null;
		
		npcData.clearAllDropData();
		
		PreparedStatement statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
		                                                                                                                       {
				"mobId", "itemId", "min", "max", "category", "chance", "enchant", "partydropmulti"
		                                                                                                                       }) + " FROM droplist WHERE mobId=?");
		statement.setInt(1, npcId);
		ResultSet dropDataList = statement.executeQuery();
		
		while (dropDataList.next())
		{
			dropData = new L2DropData();
			
			dropData.setItemId(dropDataList.getInt("itemId"));
			dropData.setMinDrop(dropDataList.getInt("min"));
			dropData.setMaxDrop(dropDataList.getInt("max"));
			dropData.setChance(dropDataList.getInt("chance"));
			dropData.setPartyDropCount(dropDataList.getInt("partydropmulti"));
			dropData.setEnchantLevel(dropDataList.getInt("enchant"));
			
			int category = dropDataList.getInt("category");
			npcData.addDropData(dropData, category);
		}
		dropDataList.close();
		statement.close();
	}
	catch (Exception e)
	{
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
}
private void showNpcSkillList(L2PcInstance activeChar, int npcId, int page)
{
	L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
	if (npcData == null)
	{
		activeChar.sendMessage("Template id unknown: " + npcId);
		return;
	}
	
	Map<Integer, L2Skill> skills = new FastMap<Integer, L2Skill>();
	if (npcData.getSkills() != null)
	{
		skills = npcData.getSkills();
	}
	
	int _skillsize = skills.size();
	
	int MaxSkillsPerPage = 10;
	int MaxPages = _skillsize / MaxSkillsPerPage;
	if (_skillsize > MaxSkillsPerPage * MaxPages)
		MaxPages++;
	
	if (page > MaxPages)
		page = MaxPages;
	
	int SkillsStart = MaxSkillsPerPage * page;
	int SkillsEnd = _skillsize;
	if (SkillsEnd - SkillsStart > MaxSkillsPerPage)
		SkillsEnd = SkillsStart + MaxSkillsPerPage;
	
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	
	StringBuffer replyMSG = new StringBuffer("");
	replyMSG.append("<html><title>" + npcData.getName() + " Skillist");
	replyMSG.append("(ID:" + npcData.getNpcId() + "Skills " + _skillsize + ")</title>");
	replyMSG.append("<body>");
	String pages = "<center><table width=270><tr>";
	for (int x = 0; x < MaxPages; x++)
	{
		int pagenr = x + 1;
		if (page == x)
		{
			pages += "<td>Page " + pagenr + "</td>";
		}
		else
		{
			pages += "<td><a action=\"bypass -h admin_show_skilllist_npc " + npcData.getNpcId() + " " + x + "\">Page " + pagenr + "</a></td>";
		}
	}
	pages += "</tr></table></center>";
	replyMSG.append(pages);
	
	replyMSG.append("<table width=270>");
	
	Set<?> skillset = skills.keySet();
	Iterator<?> skillite = skillset.iterator();
	Object skillobj = null;
	
	for (int i = 0; i < SkillsStart; i++)
	{
		if (skillite.hasNext())
		{
			skillobj = skillite.next();
		}
	}
	
	int cnt = SkillsStart;
	while (skillite.hasNext())
	{
		cnt++;
		if (cnt > SkillsEnd)
		{
			break;
		}
		skillobj = skillite.next();
		replyMSG.append("<tr><td><a action=\"bypass -h admin_edit_skill_npc " + npcData.getNpcId() + " " + skills.get(skillobj).getId() + "\">"
				+ skills.get(skillobj).getName() + "[" + skills.get(skillobj).getId() + "]" + "</a></td>" + "<td>" + skills.get(skillobj).getLevel()
				+ "</td>" + "<td><a action=\"bypass -h admin_del_skill_npc " + npcData.getNpcId() + " " + skillobj + "\">Delete</a></td></tr>");
		
	}
	replyMSG.append("</table>");
	replyMSG.append("<br><br>");
	replyMSG.append("<center>");
	replyMSG.append("<button value=\"Add Skill\" action=\"bypass -h admin_add_skill_npc " + npcId
			+ "\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
	replyMSG.append("<button value=\"Droplist\" action=\"bypass -h admin_show_droplist " + npcId
			+ "\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
	replyMSG.append("</center></body></html>");
	
	adminReply.setHtml(replyMSG.toString());
	activeChar.sendPacket(adminReply);
	
}

// [L2J_JP ADD]
private void showNpcSkillEdit(L2PcInstance activeChar, int npcId, int skillId)
{
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		PreparedStatement statement = con.prepareStatement("SELECT npcid, skillid, level FROM npcskills WHERE npcid=" + npcId + " AND skillid=" + skillId);
		ResultSet skillData = statement.executeQuery();
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		
		StringBuffer replyMSG = new StringBuffer("<html><title>(NPC:" + npcId + "SKILL:" + skillId + ")</title>");
		replyMSG.append("<body>");
		
		if (skillData.next())
		{
			L2Skill skill = SkillTable.getInstance().getInfo(skillData.getInt("skillid"), skillData.getInt("level"));
			
			replyMSG.append("<table>");
			replyMSG.append("<tr><td>NPC</td><td>" + NpcTable.getInstance().getTemplate(skillData.getInt("npcid")).getName() + "</td></tr>");
			replyMSG.append("<tr><td>SKILL</td><td>" + skill.getName() + "(" + skillData.getInt("skillid") + ")</td></tr>");
			replyMSG.append("<tr><td>Lv(" + skill.getLevel() + ")</td><td><edit var=\"level\" width=50></td></tr>");
			replyMSG.append("</table>");
			
			replyMSG.append("<center>");
			replyMSG.append("<button value=\"Edit Skill\" action=\"bypass -h admin_edit_skill_npc " + npcId + " " + skillId
					+ " $level\"  width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			replyMSG.append("<br><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId
					+ "\"  width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			replyMSG.append("</center>");
		}
		
		skillData.close();
		statement.close();
		
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		
		activeChar.sendPacket(adminReply);
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

// [L2J_JP ADD]
private void updateNpcSkillData(L2PcInstance activeChar, int npcId, int skillId, int level)
{
	Connection con = null;
	
	try
	{
		L2Skill skillData = SkillTable.getInstance().getInfo(skillId, level);
		if (skillData == null)
		{
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			StringBuffer replyMSG = new StringBuffer("<html><title>Update Npc Skill Data</title>");
			replyMSG.append("<body>");
			replyMSG.append("<center><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId
					+ "\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>");
			replyMSG.append("</body></html>");
			
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
			return;
		}
		
		con = L2DatabaseFactory.getInstance().getConnection();
		
		PreparedStatement statement = con.prepareStatement("UPDATE npcskills SET level=? WHERE npcid=? AND skillid=?");
		statement.setInt(1, level);
		statement.setInt(2, npcId);
		statement.setInt(3, skillId);
		
		statement.execute();
		statement.close();
		
		if (npcId > 0)
		{
			reLoadNpcSkillList(npcId);
			
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			StringBuffer replyMSG = new StringBuffer("<html><title>Update Npc Skill Data</title>");
			replyMSG.append("<body>");
			replyMSG.append("<center><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId
					+ "\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>");
			replyMSG.append("</body></html>");
			
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		}
		else
		{
			activeChar.sendMessage("Unknown error");
		}
		
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

// [L2J_JP ADD]
private void showNpcSkillAdd(L2PcInstance activeChar, L2NpcTemplate npcData)
{
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	
	StringBuffer replyMSG = new StringBuffer("<html><title>Add Skill to " + npcData.getName() + "(ID:" + npcData.getNpcId() + ")</title>");
	replyMSG.append("<body>");
	replyMSG.append("<table>");
	replyMSG.append("<tr><td>SkillId</td><td><edit var=\"skillId\" width=80></td></tr>");
	replyMSG.append("<tr><td>Level</td><td><edit var=\"level\" width=80></td></tr>");
	replyMSG.append("</table><br>");
	
	replyMSG.append("<center>");
	replyMSG.append("<button value=\"Add Skill\" action=\"bypass -h admin_add_skill_npc " + npcData.getNpcId()
			+ " $skillId $level\"  width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
	replyMSG.append("<br><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcData.getNpcId()
			+ "\"  width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
	replyMSG.append("</center>");
	replyMSG.append("</body></html>");
	adminReply.setHtml(replyMSG.toString());
	
	activeChar.sendPacket(adminReply);
}

// [L2J_JP ADD]
private void addNpcSkillData(L2PcInstance activeChar, int npcId, int skillId, int level)
{
	Connection con = null;
	
	try
	{
		// skill check
		L2Skill skillData = SkillTable.getInstance().getInfo(skillId, level);
		if (skillData == null)
		{
			
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			StringBuffer replyMSG = new StringBuffer("<html><title>Add Skill to Npc</title>");
			replyMSG.append("<body>");
			replyMSG.append("<center><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId
					+ "\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>");
			replyMSG.append("</body></html>");
			
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
			return;
		}
		
		con = L2DatabaseFactory.getInstance().getConnection();
		
		PreparedStatement statement = con.prepareStatement("INSERT INTO npcskills(npcid, skillid, level) values(?,?,?)");
		statement.setInt(1, npcId);
		statement.setInt(2, skillId);
		statement.setInt(3, level);
		statement.execute();
		statement.close();
		
		reLoadNpcSkillList(npcId);
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		StringBuffer replyMSG = new StringBuffer("<html><title>Add Skill to Npc (" + npcId + ", " + skillId + ", " + level + ")</title>");
		replyMSG.append("<body>");
		replyMSG.append("<center><button value=\"Add Skill\" action=\"bypass -h admin_add_skill_npc " + npcId
				+ "\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("<br><br><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId
				+ "\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
		replyMSG.append("</center></body></html>");
		
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

// [L2J_JP ADD]
private void deleteNpcSkillData(L2PcInstance activeChar, int npcId, int skillId)
{
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		if (npcId > 0)
		{
			PreparedStatement statement2 = con.prepareStatement("DELETE FROM npcskills WHERE npcid=? AND skillid=?");
			statement2.setInt(1, npcId);
			statement2.setInt(2, skillId);
			statement2.execute();
			statement2.close();
			
			reLoadNpcSkillList(npcId);
			
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			StringBuffer replyMSG = new StringBuffer("<html><title>Delete Skill (" + npcId + ", " + skillId + ")</title>");
			replyMSG.append("<body>");
			replyMSG.append("<center><button value=\"Back to Skillist\" action=\"bypass -h admin_show_skilllist_npc " + npcId
					+ "\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>");
			replyMSG.append("</body></html>");
			
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		}
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

// [L2J_JP ADD]
@SuppressWarnings("unused")
public static void reLoadNpcSkillList(int npcId)
{
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		
		npcData.clearSkills();
		
		// with out race
		String _sql = "SELECT npcid, skillid, level FROM npcskills WHERE npcid=? AND (skillid NOT BETWEEN 4290 AND 4302)";
		
		PreparedStatement statement = con.prepareStatement(_sql);
		statement.setInt(1, npcId);
		ResultSet skillDataList = statement.executeQuery();
		
		int i = 1;
		while (skillDataList.next())
		{
			int idval = skillDataList.getInt("skillid");
			int levelval = skillDataList.getInt("level");
			L2Skill skillData = SkillTable.getInstance().getInfo(idval, levelval);
			if (skillData != null)
			{
				npcData.addSkill(skillData);
			}
			i++;
		}
		skillDataList.close();
		statement.close();
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
}
