package net.sf.l2j.gameserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.mmocore.network.SelectorConfig;
import org.mmocore.network.SelectorThread;

// import luna.custom.BossEvent.PortalData;
// import luna.custom.BossEvent.PortalScheduler;
import luna.custom.DressMeEngine.DressMeLoader;
import luna.custom.antibot.configsEngine.ConfigsController;
import luna.custom.captcha.RandomString;
import luna.custom.extensions.DonateGiverTaskManager;
import luna.custom.handler.items.bonanzo.BonanzoData;
import luna.custom.loader.Loader;
import luna.custom.ranking.Ranking;
import luna.custom.ranking.xml.data.RanksParser;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.Server;
import net.sf.l2j.gameserver.cache.CrestCache;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.ForumsBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.PvPBBSManager;
import net.sf.l2j.gameserver.datatables.AccessLevels;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.datatables.ArmorSetsTable;
import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.BufferSkillsTable;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharSchemesTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.EnchantHPBonusData;
import net.sf.l2j.gameserver.datatables.EventDroplist;
import net.sf.l2j.gameserver.datatables.ExtractableItemsData;
import net.sf.l2j.gameserver.datatables.ExtractableSkillsData;
import net.sf.l2j.gameserver.datatables.FenceTable;
import net.sf.l2j.gameserver.datatables.FencesTable;
import net.sf.l2j.gameserver.datatables.FishTable;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.datatables.HennaTreeTable;
import net.sf.l2j.gameserver.datatables.IconsTable;
import net.sf.l2j.gameserver.datatables.ItemLists;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.LevelUpData;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.MerchantPriceConfigTable;
import net.sf.l2j.gameserver.datatables.NobleSkillTable;
import net.sf.l2j.gameserver.datatables.NpcBufferTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.NpcWalkerRoutesTable;
import net.sf.l2j.gameserver.datatables.PetSkillsTable;
import net.sf.l2j.gameserver.datatables.ResidentialSkillTable;
import net.sf.l2j.gameserver.datatables.SkillSpellbookTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.datatables.StaticObjects;
import net.sf.l2j.gameserver.datatables.SummonItemsData;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.geoeditorcon.GeoEditorListener;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.ChatHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.handler.UserCommandHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminAdmin;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminAnnouncements;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminAugment;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminBBS;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminBan;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminBuffs;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminCTFEngine;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminCache;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminChangeAccessLevel;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminCreateItem;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminCursedWeapons;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminDMEngine;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminDelete;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminDisconnect;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminDomination;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminDoorControl;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEditChar;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEditNpc;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEffects;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminElement;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEnchant;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEventEngine;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminExpSp;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminFOSEngine;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminFences;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminFightCalculator;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminFortSiege;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminGeoEditor;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminGeodata;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminGm;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminGmChat;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminHeal;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminHelpPage;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminInstance;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminInventory;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminInvul;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminKick;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminKill;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminKorean;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminLastManStanding;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminLevel;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminLocRecorder;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminLogin;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminLuna;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMammon;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminManor;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMenu;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMobGroup;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMonsterRace;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminMovieMaker;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPForge;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPathNode;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPetition;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPledge;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminPolymorph;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminQuest;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminRepairChar;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminRes;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminRide;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSHEngine;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSendHome;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminShop;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminShutdown;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSiege;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSkill;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSpawn;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminSummon;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTarget;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTeleport;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTest;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTvTEngine;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTvTEvent;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminTvTiEngine;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminUnblockIp;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminVIPEngine;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminZombie;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminZone;
import net.sf.l2j.gameserver.handler.chathandlers.ChatAll;
import net.sf.l2j.gameserver.handler.chathandlers.ChatAlliance;
import net.sf.l2j.gameserver.handler.chathandlers.ChatClan;
import net.sf.l2j.gameserver.handler.chathandlers.ChatHeroVoice;
import net.sf.l2j.gameserver.handler.chathandlers.ChatParty;
import net.sf.l2j.gameserver.handler.chathandlers.ChatPartyRoomAll;
import net.sf.l2j.gameserver.handler.chathandlers.ChatPartyRoomCommander;
import net.sf.l2j.gameserver.handler.chathandlers.ChatPetition;
import net.sf.l2j.gameserver.handler.chathandlers.ChatShout;
import net.sf.l2j.gameserver.handler.chathandlers.ChatTell;
import net.sf.l2j.gameserver.handler.chathandlers.ChatTrade;
import net.sf.l2j.gameserver.handler.itemhandlers.BeastSpice;
import net.sf.l2j.gameserver.handler.itemhandlers.Book;
import net.sf.l2j.gameserver.handler.itemhandlers.ClanSkillDonate;
import net.sf.l2j.gameserver.handler.itemhandlers.DonatePotion;
import net.sf.l2j.gameserver.handler.itemhandlers.ElementalExchanger;
import net.sf.l2j.gameserver.handler.itemhandlers.Elixir;
import net.sf.l2j.gameserver.handler.itemhandlers.EnchantAttribute;
import net.sf.l2j.gameserver.handler.itemhandlers.EnchantScrolls;
import net.sf.l2j.gameserver.handler.itemhandlers.ExtractIt;
import net.sf.l2j.gameserver.handler.itemhandlers.ExtractableItems;
import net.sf.l2j.gameserver.handler.itemhandlers.FishShots;
import net.sf.l2j.gameserver.handler.itemhandlers.Gem;
import net.sf.l2j.gameserver.handler.itemhandlers.Harvester;
import net.sf.l2j.gameserver.handler.itemhandlers.ItemSkills;
import net.sf.l2j.gameserver.handler.itemhandlers.LifeStones;
import net.sf.l2j.gameserver.handler.itemhandlers.Maps;
import net.sf.l2j.gameserver.handler.itemhandlers.MercTicket;
import net.sf.l2j.gameserver.handler.itemhandlers.NobleCustomItem;
import net.sf.l2j.gameserver.handler.itemhandlers.PaganKeys;
import net.sf.l2j.gameserver.handler.itemhandlers.PetFood;
import net.sf.l2j.gameserver.handler.itemhandlers.Potions;
import net.sf.l2j.gameserver.handler.itemhandlers.Recipes;
import net.sf.l2j.gameserver.handler.itemhandlers.RollingDice;
import net.sf.l2j.gameserver.handler.itemhandlers.ScrollOfResurrection;
import net.sf.l2j.gameserver.handler.itemhandlers.Seed;
import net.sf.l2j.gameserver.handler.itemhandlers.SevenSignsRecord;
import net.sf.l2j.gameserver.handler.itemhandlers.SoulCrystals;
import net.sf.l2j.gameserver.handler.itemhandlers.SpecialXMas;
import net.sf.l2j.gameserver.handler.itemhandlers.SummonItems;
import net.sf.l2j.gameserver.handler.itemhandlers.TeleportBookmark;
import net.sf.l2j.gameserver.handler.skillhandlers.BalanceLife;
import net.sf.l2j.gameserver.handler.skillhandlers.BallistaBomb;
import net.sf.l2j.gameserver.handler.skillhandlers.BeastFeed;
import net.sf.l2j.gameserver.handler.skillhandlers.Blow;
import net.sf.l2j.gameserver.handler.skillhandlers.Cancel;
import net.sf.l2j.gameserver.handler.skillhandlers.Charge;
import net.sf.l2j.gameserver.handler.skillhandlers.CombatPointHeal;
import net.sf.l2j.gameserver.handler.skillhandlers.Continuous;
import net.sf.l2j.gameserver.handler.skillhandlers.CpDam;
import net.sf.l2j.gameserver.handler.skillhandlers.Craft;
import net.sf.l2j.gameserver.handler.skillhandlers.DeluxeKey;
import net.sf.l2j.gameserver.handler.skillhandlers.Disablers;
import net.sf.l2j.gameserver.handler.skillhandlers.DrainSoul;
import net.sf.l2j.gameserver.handler.skillhandlers.Dummy;
import net.sf.l2j.gameserver.handler.skillhandlers.Extractable;
import net.sf.l2j.gameserver.handler.skillhandlers.Fishing;
import net.sf.l2j.gameserver.handler.skillhandlers.FishingSkill;
import net.sf.l2j.gameserver.handler.skillhandlers.GetPlayer;
import net.sf.l2j.gameserver.handler.skillhandlers.GiveSp;
import net.sf.l2j.gameserver.handler.skillhandlers.Harvest;
import net.sf.l2j.gameserver.handler.skillhandlers.Heal;
import net.sf.l2j.gameserver.handler.skillhandlers.InstantJump;
import net.sf.l2j.gameserver.handler.skillhandlers.ManaHeal;
import net.sf.l2j.gameserver.handler.skillhandlers.Manadam;
import net.sf.l2j.gameserver.handler.skillhandlers.Mdam;
import net.sf.l2j.gameserver.handler.skillhandlers.Pdam;
import net.sf.l2j.gameserver.handler.skillhandlers.PdamPerc;
import net.sf.l2j.gameserver.handler.skillhandlers.Resurrect;
import net.sf.l2j.gameserver.handler.skillhandlers.ResurrectPet;
import net.sf.l2j.gameserver.handler.skillhandlers.ShiftTarget;
import net.sf.l2j.gameserver.handler.skillhandlers.Soul;
import net.sf.l2j.gameserver.handler.skillhandlers.Sow;
import net.sf.l2j.gameserver.handler.skillhandlers.Spoil;
import net.sf.l2j.gameserver.handler.skillhandlers.StrSiegeAssault;
import net.sf.l2j.gameserver.handler.skillhandlers.SummonFriend;
import net.sf.l2j.gameserver.handler.skillhandlers.SummonTreasureKey;
import net.sf.l2j.gameserver.handler.skillhandlers.Sweep;
import net.sf.l2j.gameserver.handler.skillhandlers.TakeCastle;
import net.sf.l2j.gameserver.handler.skillhandlers.TakeFort;
import net.sf.l2j.gameserver.handler.skillhandlers.TransformDispel;
import net.sf.l2j.gameserver.handler.skillhandlers.Trap;
import net.sf.l2j.gameserver.handler.skillhandlers.Unlock;
import net.sf.l2j.gameserver.handler.usercommandhandlers.ChannelDelete;
import net.sf.l2j.gameserver.handler.usercommandhandlers.ChannelLeave;
import net.sf.l2j.gameserver.handler.usercommandhandlers.ChannelListUpdate;
import net.sf.l2j.gameserver.handler.usercommandhandlers.ClanPenalty;
import net.sf.l2j.gameserver.handler.usercommandhandlers.ClanWarsList;
import net.sf.l2j.gameserver.handler.usercommandhandlers.DisMount;
import net.sf.l2j.gameserver.handler.usercommandhandlers.Escape;
import net.sf.l2j.gameserver.handler.usercommandhandlers.InstanceZone;
import net.sf.l2j.gameserver.handler.usercommandhandlers.Loc;
import net.sf.l2j.gameserver.handler.usercommandhandlers.Mount;
import net.sf.l2j.gameserver.handler.usercommandhandlers.OlympiadStat;
import net.sf.l2j.gameserver.handler.usercommandhandlers.PartyInfo;
import net.sf.l2j.gameserver.handler.usercommandhandlers.Time;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.AchievementsVoice;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.DominationCommands;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.FindParty;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.JoinEvent;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.KoreanCommands;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.LastManStandingCommands;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.PingVCmd;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Wedding;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.ZombieCommands;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.castle;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.stats;
import net.sf.l2j.gameserver.idfactory.IdFactory;
// import net.sf.l2j.gameserver.instancemanager.AirShipManager;
import net.sf.l2j.gameserver.instancemanager.AuctionManager;
import net.sf.l2j.gameserver.instancemanager.BoatManager;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.instancemanager.ChatbanManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.CrownManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.DayNightSpawnManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.FourSepulchersManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.MercTicketManager;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossPointsManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.AutoChatHandler;
import net.sf.l2j.gameserver.model.AutoSpawnHandler;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.base.PlayerCounters;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.events.EventScheduler;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.GhostClient;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GamePacketHandler;
import net.sf.l2j.gameserver.pathfinding.PathFinding;
import net.sf.l2j.gameserver.taskmanager.AutoAnnounceTaskManager;
import net.sf.l2j.gameserver.taskmanager.KnownListUpdateTaskManager;
import net.sf.l2j.gameserver.taskmanager.TaskManager;
import net.sf.l2j.gameserver.util.CommonUtil;
import net.sf.l2j.gameserver.util.DynamicExtension;
import net.sf.l2j.status.Status;
import net.sf.l2j.util.DeadLockDetector;

public class GameServer
{
	private static final Logger					_log					= Logger.getLogger(GameServer.class.getName());
	private final SelectorThread<L2GameClient>	_selectorThread;
	private final DeadLockDetector				_deadDetectThread;
	private final IdFactory						_idFactory;
	public static GameServer					gameServer;
	private final LoginServerThread				_loginThread;
	private static Status						_statusServer;
	public static final Calendar				dateTimeServerStarted	= Calendar.getInstance();
	
	public long getUsedMemoryMB()
	{
		return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576; // ;
	}
	
	public SelectorThread<L2GameClient> getSelectorThread()
	{
		return _selectorThread;
	}
	
	public DeadLockDetector getDeadLockDetectorThread()
	{
		return _deadDetectThread;
	}
	
	public GameServer() throws Exception
	{
		long serverLoadStart = System.currentTimeMillis();
		gameServer = this;
		_log.finest("used mem:" + getUsedMemoryMB() + "MB");
		CommonUtil.printSection("ID Factory");
		_idFactory = IdFactory.getInstance();
		if (!_idFactory.isInitialized())
		{
			_log.severe("Could not read object IDs from DB. Please Check Your Data.");
			throw new Exception("Could not initialize the ID factory");
		}
		_log.config("IdFactory: Free ObjectIDs remaining: " + IdFactory.getInstance().size());
		ThreadPoolManager.getInstance();
		new File("./data/crests").mkdirs();
		new File("log/game").mkdirs();
		// load script engines
		CommonUtil.printSection("Scripts");

//		ScriptEngineManager.getInstance();
//		L2ScriptEngineManager.getInstance();
		// start game time control early
		CommonUtil.printSection("GameTime");
		GameTimeController.getInstance();
		// keep the references of Singletons to prevent garbage collection
		CommonUtil.printSection("GamePlay");
		CharNameTable.getInstance();
		SkillTable.getInstance();
		ItemLists.getInstance();
		ItemTable.getInstance();
		if (!ItemTable.getInstance().isInitialized())
		{
			_log.severe("Could not find the extraced files. Please Check Your Data.");
			throw new Exception("Could not initialize the item table");
		}
		// Load clan hall data before zone data and doors table
		ClanHallManager.getInstance();
		CrownManager.class.getClass();
		ExtractableItemsData.getInstance();
		ExtractableSkillsData.getInstance();
		SummonItemsData.getInstance();
		ZoneManager.getInstance();
		MerchantPriceConfigTable.getInstance().loadInstances();
		EnchantHPBonusData.getInstance();
		TradeController.getInstance();
		InstanceManager.getInstance();
		IconsTable.getInstance();
		ChatbanManager.getInstance();
		// DiscordBot.getInstance();
		if (Config.ALLOW_NPC_WALKERS)
		{
			NpcWalkerRoutesTable.getInstance().load();
		}
		NpcBufferTable.getInstance();
		RecipeController.getInstance();
		SkillTreeTable.getInstance();
		PetSkillsTable.getInstance();
		ArmorSetsTable.getInstance();
		FishTable.getInstance();
		SkillSpellbookTable.getInstance();
		CharTemplateTable.getInstance();
		NobleSkillTable.getInstance();
		ResidentialSkillTable.getInstance();
		CommonUtil.printSection("Buffer Related");
		BufferSkillsTable.getInstance();
		CharSchemesTable.getInstance();
		CommonUtil.printSection("Cache");
		// Call to load caches
		HtmCache.getInstance();
		CrestCache.getInstance();
		CommonUtil.printSection("Community Board");
		// forums has to be loaded before clan data, because of last forum id used should have also memo included
		if (Config.COMMUNITY_TYPE > 0)
		{
			ForumsBBSManager.getInstance().initRoot();
			PvPBBSManager.getInstance();
		}
		CommonUtil.printSection("Clans");
		ClanTable.getInstance();
		CommonUtil.printSection("NPCs");
		NpcTable.getInstance();
		if (!NpcTable.getInstance().isInitialized())
		{
			_log.severe("Could not find the extraced files. Please Check Your Data.");
			throw new Exception("Could not initialize the npc table");
		}

		//SpawnUtil.getInstance().parseDropsToXml();
		CommonUtil.printSection("Henna");
		HennaTable.getInstance();
		HennaTreeTable.getInstance();
		CommonUtil.printSection("Geodata");
		GeoData.getInstance();
		if (Config.GEODATA == 2)
			PathFinding.getInstance();
		CommonUtil.printSection("Castles & Sieges");
		CastleManager.getInstance().loadInstances();
		SiegeManager.getInstance();
		FortManager.getInstance().loadInstances();
		FortSiegeManager.getInstance();
		EventScheduler.getInstance();
		CommonUtil.printSection("General Data");
		TeleportLocationTable.getInstance();

//		try
//		{
//			SpawnUtil.getInstance().parseTeleportsToXml();
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
		LevelUpData.getInstance();
		L2World.getInstance();
		SpawnTable.getInstance();
		RaidBossSpawnManager.getInstance();
		DayNightSpawnManager.getInstance().notifyChangeMode();
		GrandBossManager.getInstance().initZones();
		RaidBossPointsManager.init();
		FourSepulchersManager.getInstance().init();
		DimensionalRiftManager.getInstance();
		Announcements.getInstance();
		AutoAnnounceTaskManager.getInstance();
		//GlobalVariablesManager.getInstance();
		MapRegionTable.getInstance();
		EventDroplist.getInstance();
		CommonUtil.printSection("Object Data");
		DoorTable.getInstance();
		StaticObjects.getInstance();
		CommonUtil.printSection("Load Manor data");
		L2Manor.getInstance();
		CommonUtil.printSection("Load Manager");
		AuctionManager.getInstance();
		BoatManager.getInstance();
		CastleManorManager.getInstance();
		MercTicketManager.getInstance();
		// PartyCommandManager.getInstance();
		PetitionManager.getInstance();
		QuestManager.getInstance();
		TransformationManager.getInstance();
		// AirShipManager.getInstance();
		CommonUtil.printSection("DressMe Engine Manager");
		DressMeLoader.load();
		_log.info("Loaded DressMe Engine");
		RandomString.start();
		FencesTable.getInstance();
		FenceTable.getInstance();
//		try
//		{
//			_log.info("Loading Server Scripts");
//			File scripts = new File(Config.DATAPACK_ROOT + "/data/scripts.cfg");
//			if (!Config.ALT_DEV_NO_QUESTS)
//				L2ScriptEngineManager.getInstance().executeScriptList(scripts);
//		}
//		catch (IOException ioe)
//		{
//			_log.severe("Failed loading scripts.cfg, no script going to be loaded");
//		}
//		try
//		{
//			CompiledScriptCache compiledScriptCache = L2ScriptEngineManager.getInstance().getCompiledScriptCache();
//			if (compiledScriptCache == null)
//			{
//				_log.info("Compiled Scripts Cache is disabled.");
//			}
//			else
//			{
//				compiledScriptCache.purge();
//				if (compiledScriptCache.isModified())
//				{
//					compiledScriptCache.save();
//					_log.info("Compiled Scripts Cache was saved.");
//				}
//				else
//				{
//					_log.info("Compiled Scripts Cache is up-to-date.");
//				}
//			}
//		}
//		catch (IOException e)
//		{
//			_log.log(Level.SEVERE, "Failed to store Compiled Scripts Cache.", e);
//		}
		QuestManager.getInstance().report();
		TransformationManager.getInstance().report();
		CommonUtil.printSection("Augment Manager");
		AugmentationData.getInstance();
		if (Config.SAVE_DROPPED_ITEM)
			ItemsOnGroundManager.getInstance();
		if (Config.AUTODESTROY_ITEM_AFTER > 0 || Config.HERB_AUTO_DESTROY_TIME > 0)
			ItemsAutoDestroy.getInstance();
		CommonUtil.printSection("Monster Race");
		MonsterRace.getInstance();
		// SevenSigns.getInstance().spawnSevenSignsNPC();
		// SevenSignsFestival.getInstance();
		AutoSpawnHandler.getInstance();
		AutoChatHandler.getInstance();
		CommonUtil.printSection("Olympiad & Heroes");
		Olympiad.getInstance();
		Hero.getInstance();
//		FaenorScriptEngine.getInstance();
		// Init of a cursed weapon manager
		CommonUtil.printSection("Cursed Weapons");
		CursedWeaponsManager.getInstance();
		CommonUtil.printSection("Trinity Customs Loader");
		Loader.getInstance().load();
		CommonUtil.printSection("Chat Handlers");
		_log.log(Level.CONFIG, "AutoChatHandler: Loaded " + AutoChatHandler.getInstance().size() + " handlers in total.");
		_log.log(Level.CONFIG, "AutoSpawnHandler: Loaded " + AutoSpawnHandler.getInstance().size() + " handlers in total.");
		AdminCommandHandler.getInstance();
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminAdmin());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminAugment());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminAnnouncements());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminBan());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminInventory());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminBBS());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminBuffs());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminCache());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminChangeAccessLevel());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminCreateItem());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminCTFEngine());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminCursedWeapons());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminDelete());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminDisconnect());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminDMEngine());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminDoorControl());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminEditChar());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminEditNpc());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminEffects());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminElement());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminEnchant());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminEventEngine());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminExpSp());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminFightCalculator());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminFortSiege());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminFOSEngine());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminGeodata());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminGeoEditor());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminGm());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminGmChat());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminHeal());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminHelpPage());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminInstance());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminInvul());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminKick());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminKill());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminLevel());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminLogin());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminMammon());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminMovieMaker());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminManor());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminMenu());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminMobGroup());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminMonsterRace());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminPathNode());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminPetition());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminPForge());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminPledge());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminPolymorph());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminQuest());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminRepairChar());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminRes());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminRide());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminSendHome());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminSHEngine());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminShop());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminShutdown());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminSiege());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminSkill());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminSpawn());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminSummon());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminTarget());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminTeleport());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminTest());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminTvTEngine());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminTvTEvent());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminTvTiEngine());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminUnblockIp());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminVIPEngine());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminZone());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminLocRecorder());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminLuna());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminKorean());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminZombie());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminDomination());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminLastManStanding());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminFences());
		_log.config("Loaded " + AdminCommandHandler.getInstance().size() + "  AdminCommandHandlers");
		ChatHandler.getInstance();
		ChatHandler.getInstance().registerChatHandler(new ChatAll());
		ChatHandler.getInstance().registerChatHandler(new ChatAlliance());
		ChatHandler.getInstance().registerChatHandler(new ChatClan());
		ChatHandler.getInstance().registerChatHandler(new ChatHeroVoice());
		ChatHandler.getInstance().registerChatHandler(new ChatParty());
		ChatHandler.getInstance().registerChatHandler(new ChatPartyRoomAll());
		ChatHandler.getInstance().registerChatHandler(new ChatPartyRoomCommander());
		ChatHandler.getInstance().registerChatHandler(new ChatPetition());
		ChatHandler.getInstance().registerChatHandler(new ChatShout());
		ChatHandler.getInstance().registerChatHandler(new ChatTell());
		ChatHandler.getInstance().registerChatHandler(new ChatTrade());
		_log.config("Loaded " + ChatHandler.getInstance().size() + "  ChatHandlers");
		CommonUtil.printSection("Item Handlers");
		ItemHandler.getInstance();
		ItemHandler.getInstance().registerItemHandler(new ScrollOfResurrection());
		ItemHandler.getInstance().registerItemHandler(new PaganKeys());
		ItemHandler.getInstance().registerItemHandler(new Maps());
		ItemHandler.getInstance().registerItemHandler(new Potions());
		ItemHandler.getInstance().registerItemHandler(new Recipes());
		ItemHandler.getInstance().registerItemHandler(new RollingDice());
		ItemHandler.getInstance().registerItemHandler(new EnchantAttribute());
		ItemHandler.getInstance().registerItemHandler(new EnchantScrolls());
		ItemHandler.getInstance().registerItemHandler(new ExtractableItems());
		ItemHandler.getInstance().registerItemHandler(new Book());
		ItemHandler.getInstance().registerItemHandler(new SoulCrystals());
		ItemHandler.getInstance().registerItemHandler(new SevenSignsRecord());
		ItemHandler.getInstance().registerItemHandler(new ItemSkills());
		ItemHandler.getInstance().registerItemHandler(new ExtractIt());
		ItemHandler.getInstance().registerItemHandler(new Seed());
		ItemHandler.getInstance().registerItemHandler(new Harvester());
		ItemHandler.getInstance().registerItemHandler(new MercTicket());
		ItemHandler.getInstance().registerItemHandler(new FishShots());
		ItemHandler.getInstance().registerItemHandler(new PetFood());
		ItemHandler.getInstance().registerItemHandler(new SpecialXMas());
		ItemHandler.getInstance().registerItemHandler(new SummonItems());
		ItemHandler.getInstance().registerItemHandler(new BeastSpice());
		ItemHandler.getInstance().registerItemHandler(new TeleportBookmark());
		ItemHandler.getInstance().registerItemHandler(new Elixir());
		ItemHandler.getInstance().registerItemHandler(new Gem());
		// Moved to luna.custom.nexus.Loader.java
		// ItemHandler.getInstance().registerItemHandler(new Bonanzo());
		// ItemHandler.getInstance().registerItemHandler(new LootBox());
		ItemHandler.getInstance().registerItemHandler(new DonatePotion());
		ItemHandler.getInstance().registerItemHandler(new LifeStones());
		ItemHandler.getInstance().registerItemHandler(new NobleCustomItem());
		ItemHandler.getInstance().registerItemHandler(new ElementalExchanger());
		ItemHandler.getInstance().registerItemHandler(new ClanSkillDonate());
		System.out.println("Loaded " + ItemHandler.getInstance().size() + " ItemHandlers");
		CommonUtil.printSection("Skill Handlers");
		SkillHandler.getInstance();
		SkillHandler.getInstance().registerSkillHandler(new Blow());
		SkillHandler.getInstance().registerSkillHandler(new Pdam());
		SkillHandler.getInstance().registerSkillHandler(new PdamPerc());
		SkillHandler.getInstance().registerSkillHandler(new Mdam());
		SkillHandler.getInstance().registerSkillHandler(new CpDam());
		SkillHandler.getInstance().registerSkillHandler(new Manadam());
		SkillHandler.getInstance().registerSkillHandler(new Heal());
		SkillHandler.getInstance().registerSkillHandler(new CombatPointHeal());
		SkillHandler.getInstance().registerSkillHandler(new ManaHeal());
		SkillHandler.getInstance().registerSkillHandler(new BalanceLife());
		SkillHandler.getInstance().registerSkillHandler(new Charge());
		SkillHandler.getInstance().registerSkillHandler(new Continuous());
		SkillHandler.getInstance().registerSkillHandler(new Resurrect());
		SkillHandler.getInstance().registerSkillHandler(new ResurrectPet());
		SkillHandler.getInstance().registerSkillHandler(new ShiftTarget());
		SkillHandler.getInstance().registerSkillHandler(new Spoil());
		SkillHandler.getInstance().registerSkillHandler(new Sweep());
		SkillHandler.getInstance().registerSkillHandler(new StrSiegeAssault());
		SkillHandler.getInstance().registerSkillHandler(new SummonFriend());
		SkillHandler.getInstance().registerSkillHandler(new SummonTreasureKey());
		SkillHandler.getInstance().registerSkillHandler(new Disablers());
		SkillHandler.getInstance().registerSkillHandler(new Cancel());
		SkillHandler.getInstance().registerSkillHandler(new BallistaBomb());
		SkillHandler.getInstance().registerSkillHandler(new TakeCastle());
		SkillHandler.getInstance().registerSkillHandler(new TakeFort());
		SkillHandler.getInstance().registerSkillHandler(new Unlock());
		SkillHandler.getInstance().registerSkillHandler(new DrainSoul());
		SkillHandler.getInstance().registerSkillHandler(new Craft());
		SkillHandler.getInstance().registerSkillHandler(new Fishing());
		SkillHandler.getInstance().registerSkillHandler(new FishingSkill());
		SkillHandler.getInstance().registerSkillHandler(new BeastFeed());
		SkillHandler.getInstance().registerSkillHandler(new DeluxeKey());
		SkillHandler.getInstance().registerSkillHandler(new Sow());
		SkillHandler.getInstance().registerSkillHandler(new Soul());
		SkillHandler.getInstance().registerSkillHandler(new Harvest());
		SkillHandler.getInstance().registerSkillHandler(new GetPlayer());
		SkillHandler.getInstance().registerSkillHandler(new TransformDispel());
		SkillHandler.getInstance().registerSkillHandler(new Trap());
		SkillHandler.getInstance().registerSkillHandler(new GiveSp());
		SkillHandler.getInstance().registerSkillHandler(new InstantJump());
		SkillHandler.getInstance().registerSkillHandler(new Dummy());
		SkillHandler.getInstance().registerSkillHandler(new Extractable());
		System.out.println("Loaded " + SkillHandler.getInstance().size() + " SkillHandlers");
		CommonUtil.printSection("User Commands");
		UserCommandHandler.getInstance();
		UserCommandHandler.getInstance().registerUserCommandHandler(new ClanPenalty());
		UserCommandHandler.getInstance().registerUserCommandHandler(new ClanWarsList());
		UserCommandHandler.getInstance().registerUserCommandHandler(new DisMount());
		UserCommandHandler.getInstance().registerUserCommandHandler(new Escape());
		UserCommandHandler.getInstance().registerUserCommandHandler(new InstanceZone());
		UserCommandHandler.getInstance().registerUserCommandHandler(new Loc());
		UserCommandHandler.getInstance().registerUserCommandHandler(new Mount());
		UserCommandHandler.getInstance().registerUserCommandHandler(new PartyInfo());
		UserCommandHandler.getInstance().registerUserCommandHandler(new Time());
		UserCommandHandler.getInstance().registerUserCommandHandler(new OlympiadStat());
		UserCommandHandler.getInstance().registerUserCommandHandler(new ChannelLeave());
		UserCommandHandler.getInstance().registerUserCommandHandler(new ChannelDelete());
		UserCommandHandler.getInstance().registerUserCommandHandler(new ChannelListUpdate());
		_log.config("Loaded " + UserCommandHandler.getInstance().size() + " UserHandlers");
		VoicedCommandHandler.getInstance();
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new stats());
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new Wedding());
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new FindParty());
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new PingVCmd());
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new castle());
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new ZombieCommands());
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new KoreanCommands());
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new DominationCommands());
		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new LastManStandingCommands());
		// VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new RespawnVCmd());
		// VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new FightCalculator());
		// VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new Wipe());
		if (Config.AUTO_TVT_ENABLED)
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new JoinEvent());
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new AchievementsVoice());
		}
		_log.config("Loaded " + VoicedCommandHandler.getInstance().size() + " VoicedHandlers");
		CommonUtil.printSection("Access Levels");
		AccessLevels.getInstance();
		AdminCommandAccessRights.getInstance();
		if (Config.L2JMOD_ALLOW_WEDDING)
			CoupleManager.getInstance();
		TaskManager.getInstance();
		//MuseumManager.getInstance();
		GmListTable.getInstance();
		// read pet stats from db
		L2PetDataTable.getInstance().loadPetsData();
		MerchantPriceConfigTable.getInstance().updateReferences();
		CastleManager.getInstance().activateInstances();
		FortManager.getInstance().activateInstances();
		Universe.getInstance();
		if (Config.ACCEPT_GEOEDITOR_CONN)
			GeoEditorListener.getInstance();
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		_log.config("IdFactory: Free ObjectID's remaining: " + IdFactory.getInstance().size());
		// initialize the dynamic extension loader
		try
		{
			DynamicExtension.getInstance();
		}
		catch (Exception ex)
		{
			_log.log(Level.WARNING, "DynamicExtension could not be loaded and initialized", ex);
		}
		// TvTManager.getInstance();
		// AutomatedTvT.getInstance();
		Ranking.start();
		RanksParser.getInstance().load();
		BonanzoData.getInstance();
		ConfigsController.getInstance().reloadSunriseConfigs();
		KnownListUpdateTaskManager.getInstance();
		DonateGiverTaskManager.getInstance();
		EventEngine.load();
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			PlayerCounters.checkTable();
		}
		//OfflineTradersTable.restoreOfflineTraders();
		if (Config.DEADLOCK_DETECTOR)
		{
			_deadDetectThread = new DeadLockDetector();
			_deadDetectThread.setDaemon(true);
			_deadDetectThread.start();
		}
		else
			_deadDetectThread = null;
		System.gc();
		// maxMemory is the upper limit the jvm can use, totalMemory the size of
		// the current allocation pool, freeMemory the unused memory in the
		// allocation pool
		long freeMem = (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1048576;
		long totalMem = Runtime.getRuntime().maxMemory() / 1048576;
		_log.info("GameServer Started, free memory " + freeMem + " Mb of " + totalMem + " Mb");
		_loginThread = LoginServerThread.getInstance();
		_loginThread.start();
		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = Config.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = Config.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = Config.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = Config.MMO_HELPER_BUFFER_COUNT;
		final L2GamePacketHandler gph = new L2GamePacketHandler();

		GhostClient.setGamePacketHandler(gph);
		_selectorThread = new SelectorThread<L2GameClient>(sc, gph, gph, gph, null);
		InetAddress bindAddress = null;
		if (!Config.GAMESERVER_HOSTNAME.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.GAMESERVER_HOSTNAME);
			}
			catch (UnknownHostException e1)
			{
				_log.severe("WARNING: The GameServer bind address is invalid, using all avaliable IPs. Reason: " + e1.getMessage());
				if (Config.DEVELOPER)
				{
					e1.printStackTrace();
				}
			}
		}
		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.PORT_GAME);
		}
		catch (IOException e)
		{
			_log.severe("FATAL: Failed to open server socket. Reason: " + e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
			System.exit(1);
		}
		_selectorThread.start();
		_log.config("Maximum Numbers of Connected Players: " + Config.MAXIMUM_ONLINE_USERS);
		long serverLoadEnd = System.currentTimeMillis();
		_log.info("Server Loaded in " + ((serverLoadEnd - serverLoadStart) / 1000) + " seconds");
		// DonationManager.getInstance();
		// PortalData.getInstance().load();
		// PortalScheduler.getInstance().load();
		_log.info("Portal Scheduler Initialized");
	}
	
	public static void main(String[] args) throws Exception
	{
		Server.serverMode = Server.MODE_GAMESERVER;
		final String LOG_FOLDER = "./log"; // Name of folder for log file
		final String LOG_NAME = "config/log.cfg"; // Name of log file
		// Create log folder
		final File logFolder = new File(LOG_FOLDER);
		logFolder.mkdir();
		// Create input stream for log file -- or store file data into memory
		final InputStream is = new FileInputStream(new File(LOG_NAME));
		LogManager.getLogManager().readConfiguration(is);
		is.close();
		CommonUtil.printSection("Lineage 2 Trinity");
		Server.serverMode = Server.MODE_GAMESERVER;
		// Local Constants
		/*** Main ***/
		// Create input stream for log file -- or store file data into memory
		// InputStream is = new FileInputStream(new File(LOG_NAME));
		// LogManager.getLogManager().readConfiguration(is);
		// is.close();
		// Initialize config
		Config.load();
		L2DatabaseFactory.getInstance();
		gameServer = new GameServer();
		if (Config.IS_TELNET_ENABLED)
		{
			_statusServer = new Status(Server.serverMode);
			_statusServer.start();
		}
		else
		{
			_log.info("Telnet server is currently disabled.");
		}
	}
	
	public static void printSection(String s)
	{
		s = "=[ " + s + " ]";
		while (s.length() < 61)
		{
			s = "-" + s;
		}
		_log.info(s);
	}
}
