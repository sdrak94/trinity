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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.DayNightSpawnManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.model.AutoChatHandler;
import net.sf.l2j.gameserver.model.AutoSpawnHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.StringUtil;

/**
 * This class handles following admin commands: - show_spawns = shows menu -
 * spawn_index lvl = shows menu for monsters with respective level -
 * spawn_monster id = spawns monster id on target
 *
 * @version $Revision: 1.2.2.5.2.5 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminSpawn implements IAdminCommandHandler
{

private static final String[] ADMIN_COMMANDS =
{
	"admin_show_spawns",
	"admin_spawn",
	"admin_spawn_monster",
	"admin_spawn_index",
	"admin_unspawnall",
	"admin_respawnall",
	"admin_spawn_reload",
	"admin_npc_index",
	"admin_spawn_once",
	"admin_show_npcs",
	"admin_teleport_reload",
	"admin_spawnnight",
	"admin_spawnday"
};
public static Logger _log = Logger.getLogger(AdminSpawn.class.getName());

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	if (command.equals("admin_show_spawns"))
		AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
	else if (command.startsWith("admin_spawn_index"))
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		try
		{
			st.nextToken();
			int level = Integer.parseInt(st.nextToken());
			int from = 0;
			try
			{
				from = Integer.parseInt(st.nextToken());
			}
			catch (NoSuchElementException nsee)
			{
			}
			showMonsters(activeChar, level, from);
		}
		catch (Exception e)
		{
			AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
		}
	}
	else if (command.equals("admin_show_npcs"))
		AdminHelpPage.showHelpPage(activeChar, "npcs.htm");
	else if (command.startsWith("admin_npc_index"))
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		try
		{
			st.nextToken();
			String letter = st.nextToken();
			int from = 0;
			try
			{
				from = Integer.parseInt(st.nextToken());
			}
			catch (NoSuchElementException nsee)
			{
			}
			showNpcs(activeChar, letter, from);
		}
		catch (Exception e)
		{
			AdminHelpPage.showHelpPage(activeChar, "npcs.htm");
		}
	}
	else if (command.startsWith("admin_spawn"))
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		try
		{
			String cmd = st.nextToken();
			String id = st.nextToken();
			int daynight = 0;
			int respawnTime = 200;
			int mobCount = 1;
			int location = 0;
			
			if (st.hasMoreTokens())
			{
				mobCount = Integer.parseInt(st.nextToken());
				
				if (mobCount > 12)
				{
					activeChar.sendMessage("lol you spawn too many");
					return true;
				}
			}
			if (st.hasMoreTokens())
				respawnTime = Integer.parseInt(st.nextToken());
			if (st.hasMoreTokens())
				daynight = Integer.parseInt(st.nextToken());
			
			if (daynight < 0 || daynight > 2)
				daynight = 0;
			
			if (st.hasMoreTokens())
				location = Integer.parseInt(st.nextToken());
			
			if (cmd.equalsIgnoreCase("admin_spawn_once"))
				spawnMonster(activeChar, id, respawnTime, mobCount, false, daynight, location);
			else
				spawnMonster(activeChar, id, respawnTime, mobCount, true, daynight, location);
		}
		catch (Exception e)
		{ // Case of wrong or missing monster data
			AdminHelpPage.showHelpPage(activeChar, "spawns.htm");
		}
	}
	else if (command.startsWith("admin_unspawnall"))
	{
		Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.NPC_SERVER_NOT_OPERATING));
		RaidBossSpawnManager.getInstance().cleanUp();
		DayNightSpawnManager.getInstance().cleanUp();
		L2World.getInstance().deleteVisibleNpcSpawns();
		GmListTable.broadcastMessageToGMs("NPC Unspawn completed!");

		GmListTable.broadcastMessageToGMs("Event NPC Unspawn completed!");
	}
	else if (command.startsWith("admin_spawnday"))
		DayNightSpawnManager.getInstance().spawnDayCreatures();
	else if (command.startsWith("admin_spawnnight"))
		DayNightSpawnManager.getInstance().spawnNightCreatures();
	else if (command.startsWith("admin_respawnall") || command.startsWith("admin_spawn_reload"))
	{
		// make sure all spawns are deleted
		RaidBossSpawnManager.getInstance().cleanUp();
		DayNightSpawnManager.getInstance().cleanUp();
		L2RaidBossInstance.cleanupLists();
		L2World.getInstance().deleteVisibleNpcSpawns();
		// now respawn all
		NpcTable.getInstance().reloadAllNpc();
		SpawnTable.getInstance().reloadAll();
		RaidBossSpawnManager.getInstance().reloadBosses();
		AutoSpawnHandler.getInstance().reload();
		AutoChatHandler.getInstance().reload();
		//SevenSigns.getInstance().spawnSevenSignsNPC();
		//QuestManager.getInstance().reloadAllQuests();
		GmListTable.broadcastMessageToGMs("NPC Respawn completed!");
	}
	else if (command.startsWith("admin_teleport_reload"))
	{
		TeleportLocationTable.getInstance().reloadAll();
		GmListTable.broadcastMessageToGMs("Teleport List Table reloaded.");
	}
	return true;
}


public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}

private void spawnMonster(L2PcInstance activeChar, String monsterId, int respawnTime, int mobCount, boolean permanent, int dayNight, int location)
{
	L2Object target = activeChar.getTarget();
	if (target == null)
		target = activeChar;
	
	L2NpcTemplate template1;
	if (monsterId.matches("[0-9]*"))
	{
		//First parameter was an ID number
		int monsterTemplate = Integer.parseInt(monsterId);
		template1 = NpcTable.getInstance().getTemplate(monsterTemplate);
	}
	else
	{
		//First parameter wasn't just numbers so go by name not ID
		monsterId = monsterId.replace('_', ' ');
		template1 = NpcTable.getInstance().getTemplateByName(monsterId);
	}
	
	if (template1.getType().equalsIgnoreCase("L2Minion"))
	{
		activeChar.sendMessage("Can't spawn minions");
		return;
	}
	
	if (template1 != null)
	{
		if ((template1.getType() == "L2Monster" || template1.getType() == "L2RaidBoss") && !AdminCommandAccessRights.getInstance().hasAccess("admin_spawn_monster", activeChar))
		{
			_log.warning("Character " + activeChar.getName() + " tryed to use admin command admin_spawn_monster, but have no access to it!");
			return;
		}
	}
	
	try
	{
		L2Spawn spawn = new L2Spawn(template1);
		if (Config.SAVE_GMSPAWN_ON_CUSTOM)
			spawn.setCustom(true);
		if (location == 1)
			spawn.setSpawnType(1);
		spawn.setLocx(target.getX());
		spawn.setLocy(target.getY());
		spawn.setLocz(target.getZ()+5);
		spawn.setAmount(mobCount);
		spawn.setHeading(activeChar.getHeading());
		spawn.setRespawnDelay(respawnTime);
		
		if (dayNight == 1)
			DayNightSpawnManager.getInstance().addDayCreature(spawn);
		else if (dayNight == 2)
			DayNightSpawnManager.getInstance().addNightCreature(spawn);
		
		if (activeChar.getInstanceId() > 0)
		{
			spawn.setInstanceId(activeChar.getInstanceId());
			permanent = false;
		}
		else
			spawn.setInstanceId(0);
		
		// TODO add checks for GrandBossSpawnManager
		if (RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcid()))
		{
			activeChar.sendMessage(template1.name + " is already spawned, so this one won't be permanent");
			permanent = false;
			
			/*if (pop < 10 || pop <= L2World.getInstance().getAllPlayersCount())*/
			spawn.init();
			
			if (!permanent)
				spawn.stopRespawn();
			
			spawn.getLastSpawn().setIsTempSpawn();
			
			activeChar.sendMessage("Created " + template1.name +"("+template1.getNpcId()+ ") as " + target.getObjectId());
		}
		else
		{
			if (RaidBossSpawnManager.getInstance().getValidTemplate(spawn.getNpcid()) != null)
			{
				if(NpcTable.getInstance().getTemplate(spawn.getNpcid()).level >= 94)
				{	//72-96 hours
					spawn.setRespawnMinDelay(259200);
					spawn.setRespawnMaxDelay(345600);
				}
				else
				{
					 //40-50 hours
					spawn.setRespawnMinDelay(144000);
					spawn.setRespawnMaxDelay(180000);
				}
				RaidBossSpawnManager.getInstance().addNewSpawn(spawn, 0, template1.baseHpMax, template1.baseMpMax, permanent);
			}
			else
			{
				SpawnTable.getInstance().addNewSpawn(spawn, permanent);
				
				/*if (pop < 10 || pop <= L2World.getInstance().getAllPlayersCount())*/
				spawn.init();
			}
			
			if (!permanent)
				spawn.stopRespawn();
			
			activeChar.sendMessage("Created " + template1.name +"("+template1.getNpcId()+ ") as " + target.getObjectId());
		}
	}
	catch (Exception e)
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
	}
}

private void showMonsters(L2PcInstance activeChar, int level, int from)
{
	L2NpcTemplate[] mobs = NpcTable.getInstance().getAllMonstersOfLevel(level);
	final StringBuilder tb = StringUtil.startAppend(500 + mobs.length * 80,
			"<html><title>Spawn Monster:</title><body><p> Level ",
			String.valueOf(level),
			":<br>Total Npc's : ",
			String.valueOf(mobs.length),
			"<br>"
			);
	
	// Start
	String end1 = "<br><center><button value=\"Next\" action=\"bypass -h admin_spawn_index " + level + " $from$\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>";
	String end2 = "<br><center><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>";
	
	// Loop
	boolean ended = true;
	for (int i = from; i < mobs.length; i++)
	{
		StringUtil.append(tb,
				"<a action=\"bypass -h admin_spawn_monster ",
				String.valueOf(mobs[i].npcId),
				"\">",
				mobs[i].name,
				"</a><br1>"
				);
		
		if ((tb.length() + end2.length()) > 8192) {
			end1 = end1.replace("$from$", "" + i);
			ended = false;
			break;
		}
	}
	
	// End
	if (ended)
		tb.append(end2);
	else
		tb.append(end1);
	
	activeChar.sendPacket(new NpcHtmlMessage(5, tb.toString()));
}

private void showNpcs(L2PcInstance activeChar, String starting, int from)
{
	L2NpcTemplate[] mobs = NpcTable.getInstance().getAllNpcStartingWith(starting);
	final StringBuilder tb = StringUtil.startAppend(500 + mobs.length * 80,
			"<html><title>Spawn Monster:</title><body><p> There are ",
			String.valueOf(mobs.length),
			" Npcs whose name starts with ",
			starting,
			":<br>"
			);
	
	// Start
	String end1 = "<br><center><button value=\"Next\" action=\"bypass -h admin_npc_index " + starting + " $from$\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>";
	String end2 = "<br><center><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>";
	
	// Loop
	boolean ended = true;
	for (int i = from; i < mobs.length; i++) {
		StringUtil.append(tb,
				"<a action=\"bypass -h admin_spawn_monster ",
				String.valueOf(mobs[i].npcId),
				"\">",
				mobs[i].name,
				"</a><br1>"
				);
		
		if ((tb.length() + end2.length()) > 8192)
		{
			end1 = end1.replace("$from$", "" + i);
			ended = false;
			break;
		}
	}
	// End
	if (ended)
		tb.append(end2);
	else
		tb.append(end1);
	activeChar.sendPacket(new NpcHtmlMessage(5, tb.toString()));
}
}
