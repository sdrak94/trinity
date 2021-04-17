/**
 * 
 */
package cz.nxs.events.engine;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.base.GlobalConfigModel;

/**
 * @author hNoke
 * - currently only inserts missing configs to db
 */
public class EventSQLManager
{
	private static GlobalConfigModel[] _globalConfigTemplates;
	
	private static void init()
	{
		_globalConfigTemplates = new GlobalConfigModel[]
		{ 
			// category: CORE
			new GlobalConfigModel("Core", "devMode", "false", "Enables loading HTML files from folders, etc.", 1),
			new GlobalConfigModel("Core", "debug", "true", "Enables external console for engine's messages.", 1),
			new GlobalConfigModel("Core", "logToFile", "false", "Enables logging all engine's messages (even if you have turned debugging off) to NexusEvents.log file in log directory.", 1),
			new GlobalConfigModel("Core", "detailedDebug", "false", "Enables detailed debugging of every action the events does. Use this if you are having any difficulities with something and then send to hNoke (it is written to log/NexusEvents_detailed.log file).", 1),
			new GlobalConfigModel("Core", "detailedDebugToConsole", "false", "If the 'detailedDebug' config is enabled, this can also make the detailed debug be shown into Debug Console. This is recommended only for developing environment.", 1),
			new GlobalConfigModel("Core", "miniEventsManagerId", "9998", "Mini Events NPC Manager ID.", 1),
			new GlobalConfigModel("Core", "mainEventManagerId", "9999", "Main Events NPC Manager ID.", 1),
			new GlobalConfigModel("Core", "npcBufferId", "-1", "If you want, The NPC ID of your custom buffer NPC. This NPC will be spawned in your mini events on Spawn of type 'Buffer'. To disable spawning buffer NPCs, set this to -1.", 1),
			new GlobalConfigModel("Core", "mapGuardNpcId", "9996", "The NPC ID of the automatic NPC MapGuard. This NPC may be useful to prevent players from escaping the map - It will kill all players on sight! The MapGuard will be spawned on Spawn of type 'MapGuard'. To disable spawning MapGuard NPCs, set this to -1.", 1),
			new GlobalConfigModel("Core", "cbPage", "_bbsgetfav", "Specify here under which table will the Nexus community board be available. Options are: _bbshome ('Home'), _bbsgetfav ('Favorite'), _bbslink ('Homepage'), _bbsloc ('Region'), _bbsmemo ('Memo').", 1),
			
			// category: FEATURES
			new GlobalConfigModel("Features", "afkChecksEnabled", "true", "Put 'true' to enable afk checks on all events.", 1),
			new GlobalConfigModel("Features", "afkWarningDelay", "45000", "The delay after it warns player that he will be marked as AFK if he continues his idling. In ms.", 1),
			new GlobalConfigModel("Features", "afkKickDelay", "45000", "If the player has been warned (<font color=LEVEL>afkWarningDelay</font>) for his inactivity, it will take this time to mark player as AFK. Basically, this config + config 'afkWarningDelay' = the time after it marks player for afking. Put value in ms.", 1),
			new GlobalConfigModel("Features", "eventSchemeBuffer", "true", "Enables automatic scheme-based event buffer. The players are rebuffed with their schemes on event/round start or revive. Put 'true' or 'false'.", 1),
			new GlobalConfigModel("Features", "bufferHealsPlayer", "true", "Put 'true' to automatically heal the player when he receives buffs from the event scheme buffer.", 1),
			new GlobalConfigModel("Features", "removeCubicsOnDie", "true", "You can specify here if the cubics will be removed from the player (while he is in event) when he dies.", 1),
			new GlobalConfigModel("Features", "announceRegNpcPos", "-", "Write - if you don't want the engine to announce the position of your NPC. Otherwise write here the name of the location (eg. Giran Town), which will be announced when the event starts.", 1),
			new GlobalConfigModel("Features", "announce_moreInfoInCb", "true", "True if you want to announce 'More informations in Community board.' message when an event opens registration.", 1),
			new GlobalConfigModel("Features", "enableUnregistrations", "true", "You can permit players to unregister from their event here.", 1),
			new GlobalConfigModel("Features", "antistuckProtection", "false", "Enables the experimental Anti-Stuck protection. On some servers, the players stuck when they get teleported to the event while they are casting a spell.", 1),
			new GlobalConfigModel("Features", "teleToEventDelay", "10000", "The delay to wait to teleport players to the event after the registration ended. In ms. Default 10000, max 60000.", 1),
			new GlobalConfigModel("Features", "pvpPointsOnKill", "true", "You can specify whether the players will receive PvP points for killing on events.", 1),
			new GlobalConfigModel("Features", "maxWarnings", "3", "How many warning points must to player have to disallow him access to all events. Player gets warning points when he for example disconnects from event. Warnings decrease by 1 every day.", 1),
			new GlobalConfigModel("Features", "maxBuffsPerPage", "12", "You can specify here how many buffs in the event buffer (shown in registration NPC) will there be per one page. Default and safe value should be 12, but you may want to decrease it for Interlude.", 1),
			new GlobalConfigModel("Features", "setOffensiveSkills", "", "<font color=5f5f5f>(Requires additional core modifications!)</font> Skills written here will be usable only on player's opponents/enemies (not teammates) during events. Write only IDs and separate by ';', example: <font color=LEVEL>SKILL1;SKILL2;SKILL3</font>.", 2),
			new GlobalConfigModel("Features", "setNotOffensiveSkills", "", "<font color=5f5f5f>(Requires additional core modifications!)</font> Skills written here will be usable only on player's teammates (not opponents/enemies) during events. Write only IDs and separate by ';', example: <font color=LEVEL>SKILL1;SKILL2;SKILL3</font>.", 2),
			new GlobalConfigModel("Features", "setNeutralSkills", "994", "<font color=5f5f5f>(Requires additional core modifications!)</font> Skills written here will be usable on both teammates and enemies. Useful for example for skill Rush (ID 994), which is by default not offensive, and thus the engine doesn't allow the player to cast it on his opponent. Write only IDs and separate by ';', example: <font color=LEVEL>SKILL1;SKILL2;SKILL3</font>.", 2),
			new GlobalConfigModel("Features", "showNextEventTime", "true", "Specify here whether you want to show (in the registration NPC) when does the next scheduled event start.", 1),
			new GlobalConfigModel("Features", "showNextEventName", "true", "Specify here whether you want to show (in the registration NPC) the name of the next scheduled event.", 1),
			
			//TODO maxBuffsPerPage makes mess
			
			new GlobalConfigModel("Features", "enableStatistics", "true", "Enable/disable the whole statistics engine here.", 1),
			new GlobalConfigModel("Features", "enableGlobalStatistics", "true", "Enable/disable the global statistics engine here. The players will still be able to view the personal statistics (personal statistics are easy on resources, global statistics might be a more expensive).", 1),
			new GlobalConfigModel("Features", "globalStatisticsRefresh", "1800", "In seconds. The delay after which data will be reloaded from database and sorted. Do not use small values for big servers.", 1),
			new GlobalConfigModel("Features", "statsSorting", "full", "Define available types of sorting data in the global statistics engine. Write: <font color=LEVEL>simple</font> to allow sorting by name and level; <font color=LEVEL>advanced</font> to allow sorting by name, level, count of played events and K:D ratio; <font color=LEVEL>full</font> to allow full sorting possibilities (including score count, deaths, etc.). The more complex is the sorting method, the more is the engine expensive on resources.", 1),
			new GlobalConfigModel("Features", "statsIgnoreBanned", "true", "True to not show banned players in statistics.", 1),
			new GlobalConfigModel("Features", "statsIgnoreGMs", "true", "True to not show GMs in statistics.", 1),
			new GlobalConfigModel("Features", "statsPlayersPerPage", "12", "Count of players shown on one page of global statistics. Default: 12.", 1),
			new GlobalConfigModel("Features", "statsDetailedPlayerInfo", "true", "Enables some extra statistics about player, such as PvP kills, clan name, ally name, level, class name. Btw, html page looks much better if this is enabled ;).", 1),
			new GlobalConfigModel("Features", "statsShowPkCount", "true", "Specify whether player's PK count will be shown in his statistics.", 1),
			
			new GlobalConfigModel("Features", "allowVoicedCommands", "false", "Put 'true' to allow voiced commands for registration/unregistration to the events.", 1),
			new GlobalConfigModel("Features", "registerVoicedCommand", ".register", "The command to register the player to the event remotely.", 1),
			new GlobalConfigModel("Features", "unregisterVoicedCommand", ".unregister", "The command to unregister the player from the event remotely.", 1),
			
			// category: BUFFER
			new GlobalConfigModel("Buffer", "assignedNpcId", "9997", "The NPC ID of the custom nexus engine buffer (which can be used even outside of events).", 1),
			new GlobalConfigModel("Buffer", "bufferHealDelay", "600", "In seconds. The time you need to wait to get healed from the NPC buffer.", 1),
			new GlobalConfigModel("Buffer", "maxBuffsCount", "-1", "The max count of buffs player can take from nexus buffer.. Put -1 to make this value loaded from the gameserver configs.", 1),
			new GlobalConfigModel("Buffer", "maxDancesCount", "-1", "The max count of dances player can take from nexus buffer. Put -1 to make this value loaded from the gameserver configs. Put 0 to count dances as buffs.", 1),
			
			// category: SCHEDULER
			new GlobalConfigModel("Scheduler", "enableAutomaticScheduler", "true", "Enable / Disable the automatic event scheduler when the server starts (true to enable, false to disable).", 1),
			new GlobalConfigModel("Scheduler", "firstEventDelay", "10", "The delay it takes to start a first main event after the server starts. In minutes.", 1),
			new GlobalConfigModel("Scheduler", "delayBetweenEvents", "20", "The delay it takes to start a new main event after one ended. In minutes.", 1),
			new GlobalConfigModel("Scheduler", "defaultRunTime", "20", "Default event run time, when the event is runned by the Automatic Scheduler. This value can be overriden in Event configs. In minutes.", 1),
			new GlobalConfigModel("Scheduler", "defaultRegTime", "30", "Default event registration time, when the event is runned by the Automatic Scheduler. This value can be overriden in Event configs. In minutes.", 1),
			new GlobalConfigModel("Scheduler", "allowSpawnRegNpc", "true", "Put true to allow engine to spawn the registration NPC when an event is started by the automatic scheduler.", 1),
			new GlobalConfigModel("Scheduler", "spawnRegNpcCords", "83435;148635;-3405", "The x, y, z cords specifiying where the registration NPC will be spawned when an event is started by the automatic scheduler (format: x;y;z).", 2),
			
			// category: GEAR SCORE
			new GlobalConfigModel("GearScore", "enableGearScore", "true", "Enables Gear-score engine.", 1)
		};
	}
	
	public static void addMissingGlobalConfigs()
	{
		init();
		
		int count = 0;
		for(GlobalConfigModel gc : _globalConfigTemplates)
		{
			if(!globalConfigExists(gc.getKey()))
			{
				GlobalConfigModel newConfig = EventConfig.getInstance().addGlobalConfig(gc.getCategory(), gc.getKey(), gc.getDesc(), gc.getValue(), gc.getInputType());
				EventConfig.getInstance().saveGlobalConfig(newConfig);
				
				count ++;
			}
		}
		
		if(count > 0)
			NexusLoader.debug("Nexus Events: Inserted " + count + " missing GlobalConfigs to your database.");
		
		checkOutdatedConfigs();
	}
	
	private static void checkOutdatedConfigs()
	{
		if(EventConfig.getInstance().getGlobalConfig("Core", "nexusNpcBufferId") != null)
			EventConfig.getInstance().removeGlobalConfig("Core", "nexusNpcBufferId");
		
		if(EventConfig.getInstance().getGlobalConfig("Features", "bufferMaxBuffs") != null)
			EventConfig.getInstance().removeGlobalConfig("Features", "bufferMaxBuffs");
	}
	
	private static boolean globalConfigExists(String name)
	{
		return EventConfig.getInstance().globalConfigExists(name);
	}
}
