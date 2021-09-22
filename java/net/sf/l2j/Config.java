package net.sf.l2j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.events.AutomatedTvT;
import net.sf.l2j.gameserver.util.FloodProtectorConfig;
import net.sf.l2j.gameserver.util.StringUtil;
import net.sf.l2j.util.L2Properties;

public final class Config
{
	protected static final Logger				_log									= Logger.getLogger(Config.class.getName());
	// --------------------------------------------------
	// L2J Property File Definitions
	// --------------------------------------------------
	public static final String					CHARACTER_CONFIG_FILE					= "./config/Character.properties";
	public static final String					EXTENSIONS_CONFIG_FILE					= "./config/extensions.properties";
	public static final String					FEATURE_CONFIG_FILE						= "./config/Feature.properties";
	public static final String					FORTSIEGE_CONFIGURATION_FILE			= "./config/fortsiege.properties";
	public static final String					GENERAL_CONFIG_FILE						= "./config/General.properties";
	public static final String					HEXID_FILE								= "./config/hexid.txt";
	public static final String					ID_CONFIG_FILE							= "./config/idfactory.properties";
	public static final String					SERVER_VERSION_FILE						= "./config/l2j-version.properties";
	public static final String					DATAPACK_VERSION_FILE					= "./config/l2jdp-version.properties";
	public static final String					L2JMOD_CONFIG_FILE						= "./config/l2jmods.properties";
	public static final String					LOGIN_CONFIGURATION_FILE				= "./config/loginserver.properties";
	public static final String					NPC_CONFIG_FILE							= "./config/NPC.properties";
	public static final String					PVP_CONFIG_FILE							= "./config/pvp.properties";
	public static final String					RATES_CONFIG_FILE						= "./config/rates.properties";
	public static final String					CONFIGURATION_FILE						= "./config/server.properties";
	public static final String					SIEGE_CONFIGURATION_FILE				= "./config/siege.properties";
	public static final String					TELNET_FILE								= "./config/telnet.properties";
	public static final String					FLOOD_PROTECTOR_FILE					= "./config/floodprotector.properties";
	public static final String					MMO_CONFIG_FILE							= "./config/mmo.properties";
	public static final String					EVENTS_CONFIG_FILE						= "./config/events.properties";
	public static final String					AUTO_EVENTS_CONFIG_FILE					= "./config/events_auto.properties";
	public static final String					RAIDBOSS_DROP_CHANCES_FILE				= "./config/raids.properties";
	public static final String					LUNA									= "./config/Luna.properties";
	private static final String					DONATION_CONFIG_FILE					= "./config/Donation.ini";
	public static final String					SMART_CB								= "./config/SmartCB.properties";
	public static final String					CHILL_FILE								= "./config/chill.properties";
	public static final String					NPCBUFFER_CONFIG_FILE					= "./config/npcbuffer.ini";
	public static final String					INSTANCES_FILE							= "./config/instances.ini";
	// --------------------------------------------------
	// Chill settings
	// --------------------------------------------------
	/** Auto Chill */
	public static int							CHILL_SLEEP_TICKS;
	public static int							DAILY_CREDIT;
	public static int							EVENT_CREDIT;
	public static int							INERTIA_RT;
	public static int							LAG_NEW_TARGET;
	public static int							LAG_DIE_TARGET;
	public static int							LAG_KIL_TARGET;
	public static int							LAG_ASI_TARGET;
	public static int							FOLLOW_INIT_RANGE;
	public static int							RANGE_CLOSE;
	public static int							RANGE_NEAR;
	public static int							RANGE_FAR;
	public static String						DAILY_CREDIT_TIME;
	// Smart Community Board Definitions
	// --------------------------------------------------
	public static int							TOP_PLAYER_ROW_HEIGHT;
	public static int							TOP_PLAYER_RESULTS;
	public static int							RAID_LIST_ROW_HEIGHT;
	public static int							RAID_LIST_RESULTS;
	public static boolean						RAID_LIST_SORT_ASC;
	public static boolean						ALLOW_REAL_ONLINE_STATS;
	// --------------------------------------------------
	public static final int						AURAFANG								= 81101;
	public static final int						RAYBLADE								= 81102;
	public static final int						WAVEBRAND								= 81108;
	// --------------------------------------------------
	// L2J Variable Definitions
	// --------------------------------------------------
	public static int							MASTERACCESS_LEVEL;
	public static int							MASTERACCESS_NAME_COLOR;
	public static int							MASTERACCESS_TITLE_COLOR;
	public static boolean						ALT_GAME_DELEVEL;
	public static double						ALT_WEIGHT_LIMIT;
	public static int							RUN_SPD_BOOST;
	public static int							DEATH_PENALTY_CHANCE;
	public static double						RESPAWN_RESTORE_CP;
	public static double						RESPAWN_RESTORE_HP;
	public static double						RESPAWN_RESTORE_MP;
	public static boolean						ALT_GAME_TIREDNESS;
	public static boolean						ENABLE_MODIFY_SKILL_DURATION;
	public static Map<Integer, Integer>			SKILL_DURATION_LIST;
	public static boolean						ENABLE_MODIFY_SKILL_REUSE;
	public static Map<Integer, Integer>			SKILL_REUSE_LIST;
	public static boolean						AUTO_LEARN_SKILLS;
	public static boolean						AUTO_LOOT_HERBS;
	public static byte							BUFFS_MAX_AMOUNT;
	public static boolean						AUTO_LEARN_DIVINE_INSPIRATION;
	public static boolean						ALT_GAME_CANCEL_BOW;
	public static boolean						ALT_GAME_CANCEL_CAST;
	public static boolean						EFFECT_CANCELING;
	public static boolean						ALT_GAME_MAGICFAILURES;
	public static int							PLAYER_FAKEDEATH_UP_PROTECTION;
	public static boolean						STORE_SKILL_COOLTIME;
	public static boolean						SUBCLASS_STORE_SKILL_COOLTIME;
	public static boolean						ALT_GAME_SHIELD_BLOCKS;
	public static int							ALT_PERFECT_SHLD_BLOCK;
	public static boolean						ALLOW_CLASS_MASTERS;
	public static boolean						LIFE_CRYSTAL_NEEDED;
	public static boolean						SP_BOOK_NEEDED;
	public static boolean						ES_SP_BOOK_NEEDED;
	public static boolean						DIVINE_SP_BOOK_NEEDED;
	public static boolean						ALT_GAME_SKILL_LEARN;
	public static boolean						ALT_GAME_SUBCLASS_WITHOUT_QUESTS;
	public static int							MAX_RUN_SPEED;
	public static int							MAX_PCRIT_RATE;
	public static int							MAX_MCRIT_RATE;
	public static int							MAX_PATK_SPEED;
	public static int							MAX_MATK_SPEED;
	public static int							MAX_EVASION;
	public static byte							MAX_SUBCLASS;
	public static byte							MAX_SUBCLASS_LEVEL;
	public static int							MAX_PVTSTORESELL_SLOTS_DWARF;
	public static int							MAX_PVTSTORESELL_SLOTS_OTHER;
	public static int							MAX_PVTSTOREBUY_SLOTS_DWARF;
	public static int							MAX_PVTSTOREBUY_SLOTS_OTHER;
	public static int							INVENTORY_MAXIMUM_NO_DWARF;
	public static int							INVENTORY_MAXIMUM_DWARF;
	public static int							INVENTORY_MAXIMUM_GM;
	public static int							WAREHOUSE_SLOTS_DWARF;
	public static int							WAREHOUSE_SLOTS_NO_DWARF;
	public static int							WAREHOUSE_SLOTS_CLAN;
	public static int							FREIGHT_SLOTS;
	public static boolean						ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE;
	public static boolean						ALT_GAME_KARMA_PLAYER_CAN_SHOP;
	public static boolean						ALT_GAME_KARMA_PLAYER_CAN_TELEPORT;
	public static boolean						ALT_GAME_KARMA_PLAYER_CAN_USE_GK;
	public static boolean						ALT_GAME_KARMA_PLAYER_CAN_TRADE;
	public static boolean						ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE;
	public static int							MAX_PERSONAL_FAME_POINTS;
	public static int							FORTRESS_ZONE_FAME_TASK_FREQUENCY;
	public static int							FORTRESS_ZONE_FAME_AQUIRE_POINTS;
	public static int							CASTLE_ZONE_FAME_TASK_FREQUENCY;
	public static int							CASTLE_ZONE_FAME_AQUIRE_POINTS;
	public static boolean						IS_CRAFTING_ENABLED;
	public static boolean						CRAFT_MASTERWORK;
	public static int							DWARF_RECIPE_LIMIT;
	public static int							COMMON_RECIPE_LIMIT;
	public static boolean						ALT_GAME_CREATION;
	public static double						ALT_GAME_CREATION_SPEED;
	public static double						ALT_GAME_CREATION_XP_RATE;
	public static double						ALT_GAME_CREATION_RARE_XPSP_RATE;
	public static double						ALT_GAME_CREATION_SP_RATE;
	public static boolean						ALT_BLACKSMITH_USE_RECIPES;
	public static int							ALT_CLAN_JOIN_DAYS;
	public static int							ALT_CLAN_CREATE_DAYS;
	public static int							ALT_CLAN_DISSOLVE_DAYS;
	public static int							ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
	public static int							ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
	public static int							ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
	public static int							ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
	public static int							ALT_MAX_NUM_OF_CLANS_IN_ALLY;
	public static int							ALT_CLAN_MEMBERS_FOR_WAR;
	public static boolean						ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
	public static boolean						REMOVE_CASTLE_CIRCLETS;
	public static int							ALT_PARTY_RANGE;
	public static int							ALT_PARTY_RANGE2;
	public static long							STARTING_ADENA;
	public static byte							STARTING_LEVEL;
	public static int							STARTING_SP;
	public static boolean						AUTO_LOOT;
	public static boolean						AUTO_LOOT_RAIDS;
	public static int							UNSTUCK_INTERVAL;
	public static int							PLAYER_SPAWN_PROTECTION;
	public static boolean						RESPAWN_RANDOM_ENABLED;
	public static int							RESPAWN_RANDOM_MAX_OFFSET;
	public static boolean						RESTORE_PLAYER_INSTANCE;
	public static boolean						ALLOW_SUMMON_TO_INSTANCE;
	public static boolean						PETITIONING_ALLOWED;
	public static int							MAX_PETITIONS_PER_PLAYER;
	public static int							MAX_PETITIONS_PENDING;
	public static boolean						ALT_GAME_FREIGHTS;
	public static int							ALT_GAME_FREIGHT_PRICE;
	public static boolean						ALT_GAME_FREE_TELEPORT;
	public static boolean						ALT_RECOMMEND;
	public static int							DELETE_DAYS;
	public static float							ALT_GAME_EXPONENT_XP;
	public static float							ALT_GAME_EXPONENT_SP;
	public static String						PARTY_XP_CUTOFF_METHOD;
	public static double						PARTY_XP_CUTOFF_PERCENT;
	public static int							PARTY_XP_CUTOFF_LEVEL;
	/*
	 * public static float ALT_DAGGER_DMG_VS_HEAVY; // Alternative damage for dagger skills VS heavy
	 * public static float ALT_DAGGER_DMG_VS_ROBE; // Alternative damage for dagger skills VS robe
	 * public static float ALT_DAGGER_DMG_VS_LIGHT; // Alternative damage for dagger skills VS light
	 */
	// --------------------------------------------------
	// ClanHall Settings
	// --------------------------------------------------
	public static long							CH_TELE_FEE_RATIO;
	public static int							CH_TELE1_FEE;
	public static int							CH_TELE2_FEE;
	public static long							CH_ITEM_FEE_RATIO;
	public static int							CH_ITEM1_FEE;
	public static int							CH_ITEM2_FEE;
	public static int							CH_ITEM3_FEE;
	public static long							CH_MPREG_FEE_RATIO;
	public static int							CH_MPREG1_FEE;
	public static int							CH_MPREG2_FEE;
	public static int							CH_MPREG3_FEE;
	public static int							CH_MPREG4_FEE;
	public static int							CH_MPREG5_FEE;
	public static long							CH_HPREG_FEE_RATIO;
	public static int							CH_HPREG1_FEE;
	public static int							CH_HPREG2_FEE;
	public static int							CH_HPREG3_FEE;
	public static int							CH_HPREG4_FEE;
	public static int							CH_HPREG5_FEE;
	public static int							CH_HPREG6_FEE;
	public static int							CH_HPREG7_FEE;
	public static int							CH_HPREG8_FEE;
	public static int							CH_HPREG9_FEE;
	public static int							CH_HPREG10_FEE;
	public static int							CH_HPREG11_FEE;
	public static int							CH_HPREG12_FEE;
	public static int							CH_HPREG13_FEE;
	public static long							CH_EXPREG_FEE_RATIO;
	public static int							CH_EXPREG1_FEE;
	public static int							CH_EXPREG2_FEE;
	public static int							CH_EXPREG3_FEE;
	public static int							CH_EXPREG4_FEE;
	public static int							CH_EXPREG5_FEE;
	public static int							CH_EXPREG6_FEE;
	public static int							CH_EXPREG7_FEE;
	public static long							CH_SUPPORT_FEE_RATIO;
	public static int							CH_SUPPORT1_FEE;
	public static int							CH_SUPPORT2_FEE;
	public static int							CH_SUPPORT3_FEE;
	public static int							CH_SUPPORT4_FEE;
	public static int							CH_SUPPORT5_FEE;
	public static int							CH_SUPPORT6_FEE;
	public static int							CH_SUPPORT7_FEE;
	public static int							CH_SUPPORT8_FEE;
	public static long							CH_CURTAIN_FEE_RATIO;
	public static int							CH_CURTAIN1_FEE;
	public static int							CH_CURTAIN2_FEE;
	public static long							CH_FRONT_FEE_RATIO;
	public static int							CH_FRONT1_FEE;
	public static int							CH_FRONT2_FEE;
	// --------------------------------------------------
	// Castle Settings
	// --------------------------------------------------
	public static long							CS_TELE_FEE_RATIO;
	public static int							CS_TELE1_FEE;
	public static int							CS_TELE2_FEE;
	public static long							CS_MPREG_FEE_RATIO;
	public static int							CS_MPREG1_FEE;
	public static int							CS_MPREG2_FEE;
	public static int							CS_MPREG3_FEE;
	public static int							CS_MPREG4_FEE;
	public static long							CS_HPREG_FEE_RATIO;
	public static int							CS_HPREG1_FEE;
	public static int							CS_HPREG2_FEE;
	public static int							CS_HPREG3_FEE;
	public static int							CS_HPREG4_FEE;
	public static int							CS_HPREG5_FEE;
	public static long							CS_EXPREG_FEE_RATIO;
	public static int							CS_EXPREG1_FEE;
	public static int							CS_EXPREG2_FEE;
	public static int							CS_EXPREG3_FEE;
	public static int							CS_EXPREG4_FEE;
	public static long							CS_SUPPORT_FEE_RATIO;
	public static int							CS_SUPPORT1_FEE;
	public static int							CS_SUPPORT2_FEE;
	public static int							CS_SUPPORT3_FEE;
	public static int							CS_SUPPORT4_FEE;
	public static List<String>					CL_SET_SIEGE_TIME_LIST;
	public static List<Integer>					SIEGE_HOUR_LIST_MORNING;
	public static List<Integer>					SIEGE_HOUR_LIST_AFTERNOON;
	// --------------------------------------------------
	// Fortress Settings
	// --------------------------------------------------
	public static long							FS_TELE_FEE_RATIO;
	public static int							FS_TELE1_FEE;
	public static int							FS_TELE2_FEE;
	public static long							FS_MPREG_FEE_RATIO;
	public static int							FS_MPREG1_FEE;
	public static int							FS_MPREG2_FEE;
	public static long							FS_HPREG_FEE_RATIO;
	public static int							FS_HPREG1_FEE;
	public static int							FS_HPREG2_FEE;
	public static long							FS_EXPREG_FEE_RATIO;
	public static int							FS_EXPREG1_FEE;
	public static int							FS_EXPREG2_FEE;
	public static long							FS_SUPPORT_FEE_RATIO;
	public static int							FS_SUPPORT1_FEE;
	public static int							FS_SUPPORT2_FEE;
	public static int							FS_BLOOD_OATH_COUNT;
	public static int							FS_BLOOD_OATH_FRQ;
	// --------------------------------------------------
	// Feature Settings
	// --------------------------------------------------
	public static int							TAKE_FORT_POINTS;
	public static int							LOOSE_FORT_POINTS;
	public static int							TAKE_CASTLE_POINTS;
	public static int							LOOSE_CASTLE_POINTS;
	public static int							CASTLE_DEFENDED_POINTS;
	public static int							FESTIVAL_WIN_POINTS;
	public static int							HERO_POINTS;
	public static int							ROYAL_GUARD_COST;
	public static int							KNIGHT_UNIT_COST;
	public static int							KNIGHT_REINFORCE_COST;
	public static int							BALLISTA_POINTS;
	public static int							BLOODALLIANCE_POINTS;
	public static int							BLOODOATH_POINTS;
	public static int							KNIGHTSEPAULETTE_POINTS;
	public static int							REPUTATION_SCORE_PER_KILL;
	public static int							JOIN_ACADEMY_MIN_REP_SCORE;
	public static int							JOIN_ACADEMY_MAX_REP_SCORE;
	public static int							RAID_RANKING_1ST;
	public static int							RAID_RANKING_2ND;
	public static int							RAID_RANKING_3RD;
	public static int							RAID_RANKING_4TH;
	public static int							RAID_RANKING_5TH;
	public static int							RAID_RANKING_6TH;
	public static int							RAID_RANKING_7TH;
	public static int							RAID_RANKING_8TH;
	public static int							RAID_RANKING_9TH;
	public static int							RAID_RANKING_10TH;
	public static int							RAID_RANKING_UP_TO_50TH;
	public static int							RAID_RANKING_UP_TO_100TH;
	public static int							CLAN_LEVEL_6_COST;
	public static int							CLAN_LEVEL_7_COST;
	public static int							CLAN_LEVEL_8_COST;
	public static int							CLAN_LEVEL_9_COST;
	public static int							CLAN_LEVEL_10_COST;
	// --------------------------------------------------
	// General Settings
	// --------------------------------------------------
	public static boolean						EVERYBODY_HAS_ADMIN_RIGHTS;
	public static boolean						DISPLAY_SERVER_VERSION;
	public static boolean						SERVER_LIST_BRACKET;
	public static boolean						SERVER_LIST_CLOCK;
	public static boolean						SERVER_GMONLY;
	public static boolean						GM_HERO_AURA;
	public static boolean						GM_STARTUP_INVULNERABLE;
	public static boolean						GM_STARTUP_INVISIBLE;
	public static boolean						GM_STARTUP_SILENCE;
	public static boolean						GM_STARTUP_AUTO_LIST;
	public static boolean						GM_STARTUP_DIET_MODE;
	public static String						GM_ADMIN_MENU_STYLE;
	public static boolean						GM_ITEM_RESTRICTION;
	public static boolean						GM_SKILL_RESTRICTION;
	public static boolean						GM_TRADE_RESTRICTED_ITEMS;
	public static boolean						BYPASS_VALIDATION;
	public static boolean						GAMEGUARD_ENFORCE;
	public static boolean						GAMEGUARD_PROHIBITACTION;
	public static boolean						LOG_CHAT;
	public static boolean						LOG_ITEMS;
	public static boolean						LOG_ITEM_ENCHANTS;
	public static boolean						LOG_SKILL_ENCHANTS;
	public static boolean						GMAUDIT;
	public static boolean						LOG_GAME_DAMAGE;
	public static boolean						DEBUG;
	public static boolean						PACKET_HANDLER_DEBUG;
	public static boolean						ASSERT;
	public static boolean						DEVELOPER;
	public static boolean						ACCEPT_GEOEDITOR_CONN;
	public static boolean						TEST_SERVER;
	public static boolean						ALT_DEV_NO_QUESTS;
	public static boolean						ALT_DEV_NO_SPAWNS;
	public static boolean						SERVER_LIST_TESTSERVER;
	public static int							THREAD_P_EFFECTS;
	public static int							THREAD_P_GENERAL;
	public static int							GENERAL_PACKET_THREAD_CORE_SIZE;
	public static int							IO_PACKET_THREAD_CORE_SIZE;
	public static int							GENERAL_THREAD_CORE_SIZE;
	public static int							AI_MAX_THREAD;
	public static boolean						DEADLOCK_DETECTOR;
	public static int							DEADLOCK_CHECK_INTERVAL;
	public static boolean						RESTART_ON_DEADLOCK;
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_USE_ITEM				= new FloodProtectorConfig("UseItemFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_ROLL_DICE				= new FloodProtectorConfig("RollDiceFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_FIREWORK				= new FloodProtectorConfig("FireworkFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_ITEM_PET_SUMMON			= new FloodProtectorConfig("ItemPetSummonFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_HERO_VOICE				= new FloodProtectorConfig("HeroVoiceFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_SHOUT					= new FloodProtectorConfig("ShoutFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_TRADE_CHAT				= new FloodProtectorConfig("TradeChatFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_PARTY_ROOM				= new FloodProtectorConfig("PartyRoomFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_PARTY_ROOM_COMMANDER	= new FloodProtectorConfig("PartyRoomCommanderFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_SUBCLASS				= new FloodProtectorConfig("SubclassFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_DROP_ITEM				= new FloodProtectorConfig("DropItemFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_SERVER_BYPASS			= new FloodProtectorConfig("ServerBypassFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_MULTISELL				= new FloodProtectorConfig("MultiSellFloodProtector");
	public static final FloodProtectorConfig	FLOOD_PROTECTOR_TRANSACTION				= new FloodProtectorConfig("TransactionFloodProtector");
	public static File							SCRIPT_ROOT;
	public static boolean						SERVER_LOCAL							= false;
	public static boolean						ALLOW_DISCARDITEM;
	public static int							AUTODESTROY_ITEM_AFTER;
	public static int							HERB_AUTO_DESTROY_TIME;
	public static String						PROTECTED_ITEMS;
	public static List<Integer>					LIST_PROTECTED_ITEMS					= new FastList<Integer>();
	public static int							CHAR_STORE_INTERVAL;
	public static boolean						LAZY_ITEMS_UPDATE;
	public static boolean						UPDATE_ITEMS_ON_CHAR_STORE;
	public static boolean						DESTROY_DROPPED_PLAYER_ITEM;
	public static boolean						DESTROY_EQUIPABLE_PLAYER_ITEM;
	public static boolean						SAVE_DROPPED_ITEM;
	public static boolean						EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD;
	public static int							SAVE_DROPPED_ITEM_INTERVAL;
	public static boolean						CLEAR_DROPPED_ITEM_TABLE;
	public static boolean						AUTODELETE_INVALID_QUEST_DATA;
	public static boolean						PRECISE_DROP_CALCULATION;
	public static boolean						MULTIPLE_ITEM_DROP;
	public static boolean						FORCE_INVENTORY_UPDATE;
	public static boolean						LAZY_CACHE;
	public static int							MIN_NPC_ANIMATION;
	public static int							MAX_NPC_ANIMATION;
	public static int							MIN_MONSTER_ANIMATION;
	public static int							MAX_MONSTER_ANIMATION;
	public static int							COORD_SYNCHRONIZE;
	public static boolean						GRIDS_ALWAYS_ON;
	public static int							GRID_NEIGHBOR_TURNON_TIME;
	public static int							GRID_NEIGHBOR_TURNOFF_TIME;
	public static Path							GEODATA_PATH;
	public static boolean						TRY_LOAD_UNSPECIFIED_REGIONS;
	public static Map<String, Boolean>			GEODATA_REGIONS;
	public static int							GEODATA;
	public static boolean						GEODATA_CELLFINDING;
	public static boolean						FORCE_GEODATA;
	public static boolean						MOVE_BASED_KNOWNLIST;
	public static long							KNOWNLIST_UPDATE_INTERVAL;
	public static int							ZONE_TOWN;
	public static boolean						ACTIVATE_POSITION_RECORDER;
	public static String						DEFAULT_GLOBAL_CHAT;
	public static String						DEFAULT_TRADE_CHAT;
	public static boolean						ALLOW_WAREHOUSE;
	public static boolean						WAREHOUSE_CACHE;
	public static int							WAREHOUSE_CACHE_TIME;
	public static boolean						ALLOW_FREIGHT;
	public static boolean						ALLOW_WEAR;
	public static int							WEAR_DELAY;
	public static int							WEAR_PRICE;
	public static boolean						ALLOW_LOTTERY;
	public static boolean						ALLOW_RACE;
	public static boolean						ALLOW_WATER;
	public static boolean						ALLOW_RENTPET;
	public static boolean						ALLOWFISHING;
	public static boolean						ALLOW_BOAT;
	public static boolean						ALLOW_CURSED_WEAPONS;
	public static boolean						ALLOW_MANOR;
	public static boolean						ALLOW_NPC_WALKERS;
	public static boolean						ALLOW_PET_WALKERS;
	public static boolean						SERVER_NEWS;
	public static int							COMMUNITY_TYPE;
	public static boolean						BBS_SHOW_PLAYERLIST;
	public static String						BBS_DEFAULT;
	public static boolean						SHOW_LEVEL_COMMUNITYBOARD;
	public static boolean						SHOW_STATUS_COMMUNITYBOARD;
	public static int							NAME_PAGE_SIZE_COMMUNITYBOARD;
	public static int							NAME_PER_ROW_COMMUNITYBOARD;
	public static int							ALT_OLY_START_TIME;
	public static int							ALT_OLY_MIN;
	public static long							ALT_OLY_CPERIOD;
	public static long							ALT_OLY_BATTLE;
	public static long							ALT_OLY_WPERIOD;
	public static long							ALT_OLY_VPERIOD;
	public static int							ALT_OLY_CLASSED;
	public static int							ALT_OLY_NONCLASSED;
	public static int							ALT_OLY_REG_DISPLAY;
	public static int							ALT_OLY_BATTLE_REWARD_ITEM;
	public static int							ALT_OLY_CLASSED_RITEM_C;
	public static int							ALT_OLY_NONCLASSED_RITEM_C;
	public static int							ALT_OLY_COMP_RITEM;
	public static int							ALT_OLY_GP_PER_POINT;
	public static int							ALT_OLY_HERO_POINTS;
	public static int							ALT_OLY_RANK1_POINTS;
	public static int							ALT_OLY_RANK2_POINTS;
	public static int							ALT_OLY_RANK3_POINTS;
	public static int							ALT_OLY_RANK4_POINTS;
	public static int							ALT_OLY_RANK5_POINTS;
	public static int							ALT_OLY_MAX_POINTS;
	public static boolean						ALT_OLY_LOG_FIGHTS;
	public static boolean						ALT_OLY_SHOW_MONTHLY_WINNERS;
	public static boolean						ALT_OLY_ANNOUNCE_GAMES;
	public static List<Integer>					LIST_OLY_RESTRICTED_ITEMS				= new FastList<Integer>();
	public static int							ALT_OLY_ENCHANT_LIMIT;
	public static int							ALT_MANOR_REFRESH_TIME;
	public static int							ALT_MANOR_REFRESH_MIN;
	public static int							ALT_MANOR_APPROVE_TIME;
	public static int							ALT_MANOR_APPROVE_MIN;
	public static int							ALT_MANOR_MAINTENANCE_PERIOD;
	public static boolean						ALT_MANOR_SAVE_ALL_ACTIONS;
	public static int							ALT_MANOR_SAVE_PERIOD_RATE;
	public static long							ALT_LOTTERY_PRIZE;
	public static long							ALT_LOTTERY_TICKET_PRICE;
	public static float							ALT_LOTTERY_5_NUMBER_RATE;
	public static float							ALT_LOTTERY_4_NUMBER_RATE;
	public static float							ALT_LOTTERY_3_NUMBER_RATE;
	public static long							ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
	public static int							FS_TIME_ATTACK;
	public static int							FS_TIME_COOLDOWN;
	public static int							FS_TIME_ENTRY;
	public static int							FS_TIME_WARMUP;
	public static int							FS_PARTY_MEMBER_COUNT;
	public static int							RIFT_MIN_PARTY_SIZE;
	public static int							RIFT_SPAWN_DELAY;
	public static int							RIFT_MAX_JUMPS;
	public static int							RIFT_AUTO_JUMPS_TIME_MIN;
	public static int							RIFT_AUTO_JUMPS_TIME_MAX;
	public static float							RIFT_BOSS_ROOM_TIME_MUTIPLY;
	public static int							RIFT_ENTER_COST_RECRUIT;
	public static int							RIFT_ENTER_COST_SOLDIER;
	public static int							RIFT_ENTER_COST_OFFICER;
	public static int							RIFT_ENTER_COST_CAPTAIN;
	public static int							RIFT_ENTER_COST_COMMANDER;
	public static int							RIFT_ENTER_COST_HERO;
	public static int							DEFAULT_PUNISH;
	public static int							DEFAULT_PUNISH_PARAM;
	public static boolean						ONLY_GM_ITEMS_FREE;
	public static boolean						JAIL_IS_PVP;
	public static boolean						JAIL_DISABLE_CHAT;
	public static boolean						CUSTOM_SPAWNLIST_TABLE;
	public static boolean						SAVE_GMSPAWN_ON_CUSTOM;
	public static boolean						DELETE_GMSPAWN_ON_CUSTOM;
	public static boolean						CUSTOM_NPC_TABLE;
	public static boolean						CUSTOM_ITEM_TABLES;
	public static boolean						CUSTOM_ARMORSETS_TABLE;
	public static boolean						CUSTOM_TELEPORT_TABLE;
	public static boolean						CUSTOM_DROPLIST_TABLE;
	public static boolean						CUSTOM_MERCHANT_TABLES;
	// --------------------------------------------------
	// L2JMods Settings
	// --------------------------------------------------
	public static boolean						L2JMOD_CHAMPION_ENABLE;
	public static boolean						L2JMOD_CHAMPION_PASSIVE;
	public static int							L2JMOD_CHAMPION_FREQUENCY;
	public static String						L2JMOD_CHAMP_TITLE;
	public static int							L2JMOD_CHAMP_MIN_LVL;
	public static int							L2JMOD_CHAMP_MAX_LVL;
	public static int							L2JMOD_CHAMPION_HP;
	public static int							L2JMOD_CHAMPION_REWARDS;
	public static float							L2JMOD_CHAMPION_ADENAS_REWARDS;
	public static float							L2JMOD_CHAMPION_HP_REGEN;
	public static float							L2JMOD_CHAMPION_ATK;
	public static float							L2JMOD_CHAMPION_SPD_ATK;
	public static int							L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE;
	public static int							L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE;
	public static int							L2JMOD_CHAMPION_REWARD_ID;
	public static int							L2JMOD_CHAMPION_REWARD_QTY;
	public static boolean						L2JMOD_CHAMPION_ENABLE_VITALITY;
	public static boolean						TVT_EVENT_ENABLED;
	public static boolean						TVT_EVENT_IN_INSTANCE;
	public static String						TVT_EVENT_INSTANCE_FILE;
	public static String[]						TVT_EVENT_INTERVAL;
	public static int							TVT_EVENT_PARTICIPATION_TIME;
	public static int							TVT_EVENT_RUNNING_TIME;
	public static int							TVT_EVENT_PARTICIPATION_NPC_ID;
	public static int[]							TVT_EVENT_PARTICIPATION_NPC_COORDINATES	= new int[3];
	public static int[]							TVT_EVENT_PARTICIPATION_FEE				= new int[2];
	public static int							TVT_EVENT_MIN_PLAYERS_IN_TEAMS;
	public static int							TVT_EVENT_MAX_PLAYERS_IN_TEAMS;
	public static int							TVT_EVENT_RESPAWN_TELEPORT_DELAY;
	public static int							TVT_EVENT_START_LEAVE_TELEPORT_DELAY;
	public static String						TVT_EVENT_TEAM_1_NAME;
	public static int[]							TVT_EVENT_TEAM_1_COORDINATES			= new int[3];
	public static String						TVT_EVENT_TEAM_2_NAME;
	public static int[]							TVT_EVENT_TEAM_2_COORDINATES			= new int[3];
	public static List<int[]>					TVT_EVENT_REWARDS						= new FastList<int[]>();
	public static boolean						TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED;
	public static boolean						TVT_EVENT_SCROLL_ALLOWED;
	public static boolean						TVT_EVENT_POTIONS_ALLOWED;
	public static boolean						TVT_EVENT_SUMMON_BY_ITEM_ALLOWED;
	public static List<Integer>					TVT_DOORS_IDS_TO_OPEN					= new ArrayList<Integer>();
	public static List<Integer>					TVT_DOORS_IDS_TO_CLOSE					= new ArrayList<Integer>();
	public static boolean						TVT_REWARD_TEAM_TIE;
	public static byte							TVT_EVENT_MIN_LVL;
	public static byte							TVT_EVENT_MAX_LVL;
	public static int							TVT_EVENT_EFFECTS_REMOVAL;
	public static boolean						L2JMOD_ALLOW_WEDDING;
	public static int							L2JMOD_WEDDING_PRICE;
	public static boolean						L2JMOD_WEDDING_PUNISH_INFIDELITY;
	public static boolean						L2JMOD_WEDDING_TELEPORT;
	public static int							L2JMOD_WEDDING_TELEPORT_PRICE;
	public static int							L2JMOD_WEDDING_TELEPORT_DURATION;
	public static boolean						L2JMOD_WEDDING_SAMESEX;
	public static boolean						L2JMOD_WEDDING_FORMALWEAR;
	public static int							L2JMOD_WEDDING_DIVORCE_COSTS;
	public static boolean						BANKING_SYSTEM_ENABLED;
	public static int							BANKING_SYSTEM_GOLDBARS;
	public static int							BANKING_SYSTEM_ADENA;
	public static boolean						L2JMOD_ENABLE_WAREHOUSESORTING_CLAN;
	public static boolean						L2JMOD_ENABLE_WAREHOUSESORTING_PRIVATE;
	public static boolean						L2JMOD_ENABLE_WAREHOUSESORTING_FREIGHT;
	public static boolean						OFFLINE_TRADE_ENABLE;
	public static boolean						OFFLINE_CRAFT_ENABLE;
	public static boolean						OFFLINE_SET_NAME_COLOR;
	public static int							OFFLINE_NAME_COLOR;
	public static boolean						L2JMOD_ENABLE_MANA_POTIONS_SUPPORT;
	public static boolean						L2JMOD_ACHIEVEMENT_SYSTEM;
	// --------------------------------------------------
	// NPC Settings
	// --------------------------------------------------
	public static boolean						ANNOUNCE_MAMMON_SPAWN;
	public static boolean						ALT_MOB_AGRO_IN_PEACEZONE;
	public static boolean						ALT_ATTACKABLE_NPCS;
	public static boolean						ALT_GAME_VIEWNPC;
	public static int							MAX_DRIFT_RANGE;
	public static boolean						DEEPBLUE_DROP_RULES;
	public static boolean						DEEPBLUE_DROP_RULES_RAID;
	public static boolean						SHOW_NPC_LVL;
	public static boolean						GUARD_ATTACK_AGGRO_MOB;
	public static boolean						ALLOW_WYVERN_UPGRADER;
	public static String						PET_RENT_NPC;
	public static List<Integer>					LIST_PET_RENT_NPC						= new FastList<Integer>();
	public static double						RAID_HP_REGEN_MULTIPLIER;
	public static double						RAID_MP_REGEN_MULTIPLIER;
	public static double						RAID_PDEFENCE_MULTIPLIER;
	public static double						RAID_MDEFENCE_MULTIPLIER;
	public static double						RAID_MINION_RESPAWN_TIMER;
	public static float							RAID_MIN_RESPAWN_MULTIPLIER;
	public static float							RAID_MAX_RESPAWN_MULTIPLIER;
	public static boolean						RAID_DISABLE_CURSE;
	public static int							INVENTORY_MAXIMUM_PET;
	// --------------------------------------------------
	// PvP Settings
	// --------------------------------------------------
	public static int							KARMA_MIN_KARMA;
	public static int							KARMA_MAX_KARMA;
	public static int							KARMA_XP_DIVIDER;
	public static int							KARMA_LOST_BASE;
	public static boolean						KARMA_DROP_GM;
	public static boolean						KARMA_AWARD_PK_KILL;
	public static int							KARMA_PK_LIMIT;
	public static String						KARMA_NONDROPPABLE_PET_ITEMS;
	public static String						KARMA_NONDROPPABLE_ITEMS;
	public static int[]							KARMA_LIST_NONDROPPABLE_PET_ITEMS;
	public static int[]							KARMA_LIST_NONDROPPABLE_ITEMS;
	// --------------------------------------------------
	// Rate Settings
	// --------------------------------------------------
	public static float							RATE_XP;
	public static float							RATE_SP;
	public static float							RATE_PARTY_XP;
	public static float							RATE_PARTY_SP;
	public static float							RATE_QUESTS_REWARD;
	public static float							RATE_DROP_ADENA;
	public static float							RATE_CONSUMABLE_COST;
	public static float							RATE_EXTR_FISH;
	public static float							RATE_DROP_ITEMS;
	public static float							RATE_DROP_ITEMS_BY_RAID;
	public static float							RATE_DROP_SPOIL;
	public static int							RATE_DROP_MANOR;
	public static float							RATE_DROP_QUEST;
	public static float							RATE_KARMA_EXP_LOST;
	public static float							RATE_SIEGE_GUARDS_PRICE;
	public static float							RATE_DROP_COMMON_HERBS;
	public static float							RATE_DROP_MP_HP_HERBS;
	public static float							RATE_DROP_GREATER_HERBS;
	public static float							RATE_DROP_SUPERIOR_HERBS;
	public static float							RATE_DROP_SPECIAL_HERBS;
	public static int							PLAYER_DROP_LIMIT;
	public static int							PLAYER_RATE_DROP;
	public static int							PLAYER_RATE_DROP_ITEM;
	public static int							PLAYER_RATE_DROP_EQUIP;
	public static int							PLAYER_RATE_DROP_EQUIP_WEAPON;
	public static float							PET_XP_RATE;
	public static int							PET_FOOD_RATE;
	public static float							SINEATER_XP_RATE;
	public static int							KARMA_DROP_LIMIT;
	public static int							KARMA_RATE_DROP;
	public static int							KARMA_RATE_DROP_ITEM;
	public static int							KARMA_RATE_DROP_EQUIP;
	public static int							KARMA_RATE_DROP_EQUIP_WEAPON;
	public static double[]						PLAYER_XP_PERCENT_LOST;
	// --------------------------------------------------
	// Seven Signs Settings
	// --------------------------------------------------
	public static boolean						ALT_GAME_CASTLE_DAWN;
	public static boolean						ALT_GAME_CASTLE_DUSK;
	public static boolean						ALT_GAME_REQUIRE_CLAN_CASTLE;
	public static int							ALT_FESTIVAL_MIN_PLAYER;
	public static int							ALT_MAXIMUM_PLAYER_CONTRIB;
	public static long							ALT_FESTIVAL_MANAGER_START;
	public static long							ALT_FESTIVAL_LENGTH;
	public static long							ALT_FESTIVAL_CYCLE_LENGTH;
	public static long							ALT_FESTIVAL_FIRST_SPAWN;
	public static long							ALT_FESTIVAL_FIRST_SWARM;
	public static long							ALT_FESTIVAL_SECOND_SPAWN;
	public static long							ALT_FESTIVAL_SECOND_SWARM;
	public static long							ALT_FESTIVAL_CHEST_SPAWN;
	public static double						ALT_SIEGE_DAWN_GATES_PDEF_MULT;
	public static double						ALT_SIEGE_DUSK_GATES_PDEF_MULT;
	public static double						ALT_SIEGE_DAWN_GATES_MDEF_MULT;
	public static double						ALT_SIEGE_DUSK_GATES_MDEF_MULT;
	// --------------------------------------------------
	// Server Settings
	// --------------------------------------------------
	public static int							PORT_GAME;
	public static int							PORT_LOGIN;
	public static String						LOGIN_BIND_ADDRESS;
	public static int							LOGIN_TRY_BEFORE_BAN;
	public static int							LOGIN_BLOCK_AFTER_BAN;
	public static String						GAMESERVER_HOSTNAME;
	public static String						DATABASE_DRIVER;
	public static String						DATABASE_URL;
	public static String						DATABASE_LOGIN;
	public static String						DATABASE_PASSWORD;
	public static int							DATABASE_MAX_CONNECTIONS;
	public static int							DATABASE_MAX_IDLE_TIME;
	public static int							MAXIMUM_ONLINE_USERS;
	public static String						CNAME_TEMPLATE;
	public static String						PET_NAME_TEMPLATE;
	public static int							MAX_CHARACTERS_NUMBER_PER_ACCOUNT;
	public static File							DATAPACK_ROOT;
	public static boolean						ACCEPT_ALTERNATE_ID;
	public static int							REQUEST_ID;
	public static boolean						RESERVE_HOST_ON_LOGIN					= false;
	public static int							MIN_PROTOCOL_REVISION;
	public static int							MAX_PROTOCOL_REVISION;
	public static boolean						LOG_LOGIN_CONTROLLER;
	/** ************************************************** **/
	/** MMO Settings - Begin **/
	public static int							MMO_SELECTOR_SLEEP_TIME;
	public static int							MMO_MAX_SEND_PER_PASS;
	public static int							MMO_MAX_READ_PER_PASS;
	public static int							MMO_HELPER_BUFFER_COUNT;
	public static int							MMO_IO_SELECTOR_THREAD_COUNT;
	// --------------------------------------------------
	// Vitality Settings
	// --------------------------------------------------
	public static boolean						ENABLE_VITALITY;
	public static boolean						RECOVER_VITALITY_ON_RECONNECT;
	public static boolean						ENABLE_DROP_VITALITY_HERBS;
	public static float							RATE_VITALITY_LEVEL_1;
	public static float							RATE_VITALITY_LEVEL_2;
	public static float							RATE_VITALITY_LEVEL_3;
	public static float							RATE_VITALITY_LEVEL_4;
	public static float							RATE_DROP_VITALITY_HERBS;
	public static float							RATE_RECOVERY_VITALITY_PEACE_ZONE;
	public static float							RATE_VITALITY_LOST;
	public static float							RATE_VITALITY_GAIN;
	public static float							RATE_RECOVERY_ON_RECONNECT;
	// --------------------------------------------------
	// No classification assigned to the following yet
	// --------------------------------------------------
	public static int							MAX_ITEM_IN_PACKET;
	public static boolean						CHECK_KNOWN;
	public static int							GAME_SERVER_LOGIN_PORT;
	public static String						GAME_SERVER_LOGIN_HOST;
	public static String						INTERNAL_HOSTNAME;
	public static String						EXTERNAL_HOSTNAME;
	public static String						ROUTER_HOSTNAME;
	public static int							PATH_NODE_RADIUS;
	public static int							NEW_NODE_ID;
	public static int							SELECTED_NODE_ID;
	public static int							LINKED_NODE_ID;
	public static String						NEW_NODE_TYPE;
	public static int							IP_UPDATE_TIME;
	public static String						SERVER_VERSION;
	public static String						SERVER_BUILD_DATE;
	public static String						DATAPACK_VERSION;
	public static String						NONDROPPABLE_ITEMS;
	public static List<Integer>					LIST_NONDROPPABLE_ITEMS					= new FastList<Integer>();
	public static int							PVP_NORMAL_TIME;
	public static int							PVP_PVP_TIME;
	public static boolean						COUNT_PACKETS							= false;
	public static boolean						DUMP_PACKET_COUNTS						= false;
	public static int							DUMP_INTERVAL_SECONDS					= 60;
	
	public static enum IdFactoryType
	{
		Compaction,
		BitSet,
		Stack
	}
	
	public static IdFactoryType	IDFACTORY_TYPE;
	public static boolean		BAD_ID_CHECKING;
	
	public static enum ObjectMapType
	{
		L2ObjectHashMap,
		WorldObjectMap
	}
	
	public static enum ObjectSetType
	{
		L2ObjectHashSet,
		WorldObjectSet
	}
	
	public static ObjectMapType			MAP_TYPE;
	public static ObjectSetType			SET_TYPE;
	public static int					ENCHANT_CHANCE_WEAPON;
	public static int					ENCHANT_CHANCE_ARMOR;
	public static int					ENCHANT_CHANCE_JEWELRY;
	public static int					ENCHANT_CHANCE_ELEMENT;
	public static int					BLESSED_ENCHANT_CHANCE_WEAPON;
	public static int					BLESSED_ENCHANT_CHANCE_ARMOR;
	public static int					BLESSED_ENCHANT_CHANCE_JEWELRY;
	public static int					CRYSTAL_ENCHANT_CHANCE_WEAPON;
	public static int					CRYSTAL_ENCHANT_CHANCE_ARMOR;
	public static int					CRYSTAL_ENCHANT_CHANCE_JEWELRY;
	public static int					ENCHANT_MAX_WEAPON;
	public static int					ENCHANT_MAX_ARMOR;
	public static int					ENCHANT_MAX_JEWELRY;
	public static int					ENCHANT_SAFE_MAX;
	public static int					ENCHANT_SAFE_MAX_FULL;
	public static int					AUGMENTATION_NG_SKILL_CHANCE;
	public static int					AUGMENTATION_NG_GLOW_CHANCE;
	public static int					AUGMENTATION_MID_SKILL_CHANCE;
	public static int					AUGMENTATION_MID_GLOW_CHANCE;
	public static int					AUGMENTATION_HIGH_SKILL_CHANCE;
	public static int					AUGMENTATION_HIGH_GLOW_CHANCE;
	public static int					AUGMENTATION_TOP_SKILL_CHANCE;
	public static int					AUGMENTATION_TOP_GLOW_CHANCE;
	public static int					AUGMENTATION_BASESTAT_CHANCE;
	public static int					AUGMENTATION_ACC_SKILL_CHANCE;
	public static int[]					AUGMENTATION_BLACKLIST;
	public static double				HP_REGEN_MULTIPLIER;
	public static double				MP_REGEN_MULTIPLIER;
	public static double				CP_REGEN_MULTIPLIER;
	public static boolean				IS_TELNET_ENABLED;
	public static boolean				SHOW_LICENCE;
	public static boolean				FORCE_GGAUTH;
	public static boolean				ACCEPT_NEW_GAMESERVER;
	public static int					SERVER_ID;
	public static byte[]				HEX_ID;
	public static boolean				AUTO_CREATE_ACCOUNTS;
	public static boolean				FLOOD_PROTECTION;
	public static int					FAST_CONNECTION_LIMIT;
	public static int					NORMAL_CONNECTION_TIME;
	public static int					FAST_CONNECTION_TIME;
	public static int					MAX_CONNECTION_PER_IP;
	/** ************************************************** **/
	/** Automatic events - begin **/
	/** ************************************************** **/
	public static String				CTF_EVEN_TEAMS;
	public static boolean				CTF_ALLOW_INTERFERENCE;
	public static boolean				CTF_ALLOW_POTIONS;
	public static boolean				CTF_ALLOW_SUMMON;
	public static boolean				CTF_ON_START_REMOVE_ALL_EFFECTS;
	public static boolean				CTF_ON_START_UNSUMMON_PET;
	public static boolean				CTF_ANNOUNCE_TEAM_STATS;
	public static boolean				CTF_ANNOUNCE_REWARD;
	public static boolean				CTF_JOIN_CURSED;
	public static boolean				CTF_REVIVE_RECOVERY;
	public static long					CTF_REVIVE_DELAY;
	public static String				TVT_EVEN_TEAMS;
	public static boolean				TVT_ALLOW_INTERFERENCE;
	public static boolean				TVT_ALLOW_POTIONS;
	public static boolean				TVT_ALLOW_SUMMON;
	public static boolean				TVT_ON_START_REMOVE_ALL_EFFECTS;
	public static boolean				TVT_ON_START_UNSUMMON_PET;
	public static boolean				TVT_REVIVE_RECOVERY;
	public static boolean				TVT_ANNOUNCE_TEAM_STATS;
	public static boolean				TVT_ANNOUNCE_REWARD;
	public static boolean				TVT_PRICE_NO_KILLS;
	public static boolean				TVT_JOIN_CURSED;
	public static long					TVT_REVIVE_DELAY;
	public static boolean				DM_ALLOW_INTERFERENCE;
	public static boolean				DM_ALLOW_POTIONS;
	public static boolean				DM_ALLOW_SUMMON;
	public static boolean				DM_ON_START_REMOVE_ALL_EFFECTS;
	public static boolean				DM_ON_START_UNSUMMON_PET;
	public static long					DM_REVIVE_DELAY;
	public static boolean				VIP_ALLOW_INTERFERENCE;
	public static boolean				VIP_ALLOW_POTIONS;
	public static boolean				VIP_ON_START_REMOVE_ALL_EFFECTS;
	public static int					VIP_MIN_LEVEL;
	public static int					VIP_MAX_LEVEL;
	public static int					VIP_MIN_PARTICIPANTS;
	public static boolean				FALLDOWNONDEATH;
	public static boolean				ARENA_ENABLED;
	public static int					ARENA_INTERVAL;
	public static int					ARENA_REWARD_ID;
	public static int					ARENA_REWARD_COUNT;
	public static boolean				FISHERMAN_ENABLED;
	public static int					FISHERMAN_INTERVAL;
	public static int					FISHERMAN_REWARD_ID;
	public static int					FISHERMAN_REWARD_COUNT;
	public static String				TVTI_INSTANCE_XML;
	public static boolean				TVTI_ALLOW_TIE;
	public static boolean				TVTI_CHECK_WEIGHT_AND_INVENTORY;
	public static boolean				TVTI_ALLOW_INTERFERENCE;
	public static boolean				TVTI_ALLOW_POTIONS;
	public static boolean				TVTI_ALLOW_SUMMON;
	public static boolean				TVTI_ON_START_REMOVE_ALL_EFFECTS;
	public static boolean				TVTI_ON_START_UNSUMMON_PET;
	public static boolean				TVTI_REVIVE_RECOVERY;
	public static boolean				TVTI_ANNOUNCE_TEAM_STATS;
	public static boolean				TVTI_ANNOUNCE_REWARD;
	public static boolean				TVTI_PRICE_NO_KILLS;
	public static boolean				TVTI_JOIN_CURSED;
	public static boolean				TVTI_SHOW_STATS_PAGE;
	public static int					TVTI_SORT_TEAMS;
	public static int					TVTI_JOIN_NPC_SKILL;
	public static long					TVTI_REVIVE_DELAY;
	public static long					TVTI_JOIN_NPC_DO_SKILL_AGAIN;
	public static Map<Integer, Double>	BALANCE_SKILL_LIST;
	public static boolean				KAMALOKA_DROPS_TO_FULL_PARTY_ONLY;
	public static double				PVP_CLASS_BALANCE_DUELIST;
	public static double				PVP_CLASS_BALANCE_DREADNOUGHT;
	public static double				PVP_CLASS_BALANCE_PHOENIX_KNIGHT;
	public static double				PVP_CLASS_BALANCE_HELL_KNIGHT;
	public static double				PVP_CLASS_BALANCE_SAGITTARIUS;
	public static double				PVP_CLASS_BALANCE_ADVENTURER;
	public static double				PVP_CLASS_BALANCE_ARCHMAGE;
	public static double				PVP_CLASS_BALANCE_SOULTAKER;
	public static double				PVP_CLASS_BALANCE_ARCANA_LORD;
	public static double				PVP_CLASS_BALANCE_CARDINAL;
	public static double				PVP_CLASS_BALANCE_HIEROPHANT;
	public static double				PVP_CLASS_BALANCE_EVA_TEMPLAR;
	public static double				PVP_CLASS_BALANCE_SWORD_MUSE;
	public static double				PVP_CLASS_BALANCE_WIND_RIDER;
	public static double				PVP_CLASS_BALANCE_MOONLIGHT_SENTINEL;
	public static double				PVP_CLASS_BALANCE_MYSTIC_MUSE;
	public static double				PVP_CLASS_BALANCE_ELEMENTAL_MASTER;
	public static double				PVP_CLASS_BALANCE_EVA_SAINT;
	public static double				PVP_CLASS_BALANCE_SHILLIEN_TEMPLAR;
	public static double				PVP_CLASS_BALANCE_SPECTRAL_DANCER;
	public static double				PVP_CLASS_BALANCE_GHOST_HUNTER;
	public static double				PVP_CLASS_BALANCE_GHOST_SENTINEL;
	public static double				PVP_CLASS_BALANCE_STORM_SCREAMER;
	public static double				PVP_CLASS_BALANCE_SPECTRAL_MASTER;
	public static double				PVP_CLASS_BALANCE_SHILLIEN_SAINT;
	public static double				PVP_CLASS_BALANCE_TITAN;
	public static double				PVP_CLASS_BALANCE_GRAND_KHAUATARI;
	public static double				PVP_CLASS_BALANCE_DOMINATOR;
	public static double				PVP_CLASS_BALANCE_DOOMCRYER;
	public static double				PVP_CLASS_BALANCE_FORTUNE_SEEKER;
	public static double				PVP_CLASS_BALANCE_MAESTRO;
	public static double				PVP_CLASS_BALANCE_DOOMBRINGER;
	public static double				PVP_CLASS_BALANCE_MALE_SOULHOUND;
	public static double				PVP_CLASS_BALANCE_FEMALE_SOULHOUND;
	public static double				PVP_CLASS_BALANCE_TRICKSTER;
	public static double				PVP_CLASS_BALANCE_INSPECTOR;
	public static double				PVP_CLASS_BALANCE_JUDICATOR;
	public static double				PVE_CLASS_BALANCE_DUELIST;
	public static double				PVE_CLASS_BALANCE_DREADNOUGHT;
	public static double				PVE_CLASS_BALANCE_PHOENIX_KNIGHT;
	public static double				PVE_CLASS_BALANCE_HELL_KNIGHT;
	public static double				PVE_CLASS_BALANCE_SAGITTARIUS;
	public static double				PVE_CLASS_BALANCE_ADVENTURER;
	public static double				PVE_CLASS_BALANCE_ARCHMAGE;
	public static double				PVE_CLASS_BALANCE_SOULTAKER;
	public static double				PVE_CLASS_BALANCE_ARCANA_LORD;
	public static double				PVE_CLASS_BALANCE_CARDINAL;
	public static double				PVE_CLASS_BALANCE_HIEROPHANT;
	public static double				PVE_CLASS_BALANCE_EVA_TEMPLAR;
	public static double				PVE_CLASS_BALANCE_SWORD_MUSE;
	public static double				PVE_CLASS_BALANCE_WIND_RIDER;
	public static double				PVE_CLASS_BALANCE_MOONLIGHT_SENTINEL;
	public static double				PVE_CLASS_BALANCE_MYSTIC_MUSE;
	public static double				PVE_CLASS_BALANCE_ELEMENTAL_MASTER;
	public static double				PVE_CLASS_BALANCE_EVA_SAINT;
	public static double				PVE_CLASS_BALANCE_SHILLIEN_TEMPLAR;
	public static double				PVE_CLASS_BALANCE_SPECTRAL_DANCER;
	public static double				PVE_CLASS_BALANCE_GHOST_HUNTER;
	public static double				PVE_CLASS_BALANCE_GHOST_SENTINEL;
	public static double				PVE_CLASS_BALANCE_STORM_SCREAMER;
	public static double				PVE_CLASS_BALANCE_SPECTRAL_MASTER;
	public static double				PVE_CLASS_BALANCE_SHILLIEN_SAINT;
	public static double				PVE_CLASS_BALANCE_TITAN;
	public static double				PVE_CLASS_BALANCE_GRAND_KHAUATARI;
	public static double				PVE_CLASS_BALANCE_DOMINATOR;
	public static double				PVE_CLASS_BALANCE_DOOMCRYER;
	public static double				PVE_CLASS_BALANCE_FORTUNE_SEEKER;
	public static double				PVE_CLASS_BALANCE_MAESTRO;
	public static double				PVE_CLASS_BALANCE_DOOMBRINGER;
	public static double				PVE_CLASS_BALANCE_MALE_SOULHOUND;
	public static double				PVE_CLASS_BALANCE_FEMALE_SOULHOUND;
	public static double				PVE_CLASS_BALANCE_TRICKSTER;
	public static double				PVE_CLASS_BALANCE_INSPECTOR;
	public static double				PVE_CLASS_BALANCE_JUDICATOR;
	public static double				titanium_default_bow_default;
	public static double				titanium_default_cross_default;
	public static double				titanium_default_bigb_default;
	public static double				titanium_default_dual_default;
	public static double				titanium_default_val;
	public static double				titanium_over_bow_default;
	public static double				titanium_over_cross_default;
	public static double				titanium_over_bigb_default;
	public static double				titanium_over_dual_default;
	public static double				titanium_over_default;
	public static double				titanium_super_bow_default;
	public static double				titanium_super_cross_default;
	public static double				titanium_super_bigb_default;
	public static double				titanium_super_dual_default;
	public static double				titanium_super_default;
	public static double				dread_default_bow_default;
	public static double				dread_default_cross_default;
	public static double				dread_default_bigb_default;
	public static double				dread_default_dual_default;
	public static double				dread_default_val;
	public static double				dread_over_bow_default;
	public static double				dread_over_cross_default;
	public static double				dread_over_bigb_default;
	public static double				dread_over_dual_default;
	public static double				dread_over_default;
	public static double				dread_super_bow_default;
	public static double				dread_super_cross_default;
	public static double				dread_super_bigb_default;
	public static double				dread_super_dual_default;
	public static double				dread_super_default;
	public static double				corrupted_default_bow_default;
	public static double				corrupted_default_cross_default;
	public static double				corrupted_default_bigb_default;
	public static double				corrupted_default_dual_default;
	public static double				corrupted_default_val;
	public static double				corrupted_over_bow_default;
	public static double				corrupted_over_cross_default;
	public static double				corrupted_over_bigb_default;
	public static double				corrupted_over_dual_default;
	public static double				corrupted_over_default;
	public static double				corrupted_super_bow_default;
	public static double				corrupted_super_cross_default;
	public static double				corrupted_super_bigb_default;
	public static double				corrupted_super_dual_default;
	public static double				corrupted_super_default;
	public static int					SUMMON_PATK_MODIFIER;
	public static int					SUMMON_MATK_MODIFIER;
	public static boolean				DOUBLE_PVP;
	public static boolean				DOUBLE_PVP_WEEKEND;
	public static boolean				PVP_PROTECTIONS;
	public static boolean				ALLOW_CLAN_WAREHOUSE;
	public static boolean				ALLOW_CASTLE_WAREHOUSE;
	public static boolean				ALLOW_FREIGHT_WAREHOUSE;
	public static boolean				ENABLE_SUBCLASS_LOGS;
	public static int					PVP_TOKEN_CHANCE;
	public static int					PVP_TOKEN_CHANCE_HOT_ZONES;
	public static int					PVP_TOKEN_CHANCE_EVENTS;
	public static int					RARE_PVP_TOKEN_CHANCE;
	public static int					RARE_PVP_TOKEN_CHANCE_HOT_ZONES;
	public static int					RARE_PVP_TOKEN_CHANCE_EVENTS;
	public static int					CLAN_ESSENCE_CHANCE;
	public static int					CLAN_ESSENCE_CHANCE_HOT_ZONES;
	public static int					CLAN_ESSENCE_CHANCE_SIEGES;
	public static int					PVP_CHEST_POOL_SIZE;
	/** Captcha */
	public static boolean				BOTS_PREVENTION;
	public static int					KILLS_COUNTER;
	public static int					KILLS_COUNTER_RANDOMIZATION;
	public static int					VALIDATION_TIME;
	public static int					PUNISHMENT;
	public static int					PUNISHMENT_TIME;
	public static int					PUNISHMENT_TIME_BONUS_1;
	public static int					PUNISHMENT_TIME_BONUS_2;
	public static int					PUNISHMENT_TIME_BONUS_3;
	public static int					PUNISHMENT_TIME_BONUS_4;
	public static int					PUNISHMENT_TIME_BONUS_5;
	public static int					PUNISHMENT_TIME_BONUS_6;
	public static int					PUNISHMENT_REPORTS1;
	public static int					PUNISHMENT_REPORTS2;
	public static int					PUNISHMENT_REPORTS3;
	public static int					PUNISHMENT_REPORTS4;
	public static int					PUNISHMENT_REPORTS5;
	public static int					PUNISHMENT_REPORTS6;
	public static int					ESCAPE_PUNISHMENT_REPORTS_COUNT;
	public static int					KICK_PUNISHMENT_REPORTS_COUNT;
	public static int					JAIL_PUNISHMENT_REPORTS_COUNT;
	public static int					ENCHANT_BONUS_TIER_2;
	public static int					ENCHANT_BONUS_TIER_2_5;
	public static int					ENCHANT_BONUS_TIER_3;
	public static int					ENCHANT_BONUS_TIER_4;
	public static int					ENCHANT_BONUS_TIER_4_5;
	public static int					ENCHANT_CLUTCH_TIER_0;
	public static int					ENCHANT_CLUTCH_TIER_1;
	public static int					ENCHANT_CLUTCH_TIER_1_5;
	public static int					ENCHANT_CLUTCH_TIER_2;
	public static int					ENCHANT_CLUTCH_TIER_2_5;
	public static int					ENCHANT_CLUTCH_TIER_3_DYNASTY;
	public static int					ENCHANT_CLUTCH_TIER_3_VESPER;
	public static int					ENCHANT_CLUTCH_TIER_3_VESPER_JEWS;
	public static int					ENCHANT_CLUTCH_TIER_3_DEFAULT;
	public static int					ENCHANT_CLUTCH_TIER_3_5;
	public static int					ENCHANT_CLUTCH_TIER_4;
	public static int					ENCHANT_CLUTCH_TIER_4_5;
	public static int					ENCHANT_CLUTCH_TIER_5;
	public static boolean				MAIL_ENABLED				= false;
	public static String				MAIL_USER;
	public static String				MAIL_PASSWORD;
	public static String				DONATE_MAIL_USER;
	public static String				DONATE_MAIL_PASSWORD;
	public static boolean				HWID_FARMZONES_CHECK;
	public static boolean				HWID_EVENTS_CHECK;
	public static boolean				HWID_EVENTZONES_CHECK;
	public static boolean				HWID_FARMWHILEEVENT_CHECK;
	public static boolean				ENABLE_OLD_NIGHT;
	public static boolean				ENABLE_OLD_OLY;
	public static boolean				EVENTS_LIMIT_IPS;
	public static int					EVENTS_LIMIT_IPS_NUM;
	public static int					SYNERGY_CHANCE_ON_PVP;
	public static int					SYNERGY_RADIUS;
	public static double				SYNERGY_BOOST_2_SUPPORTS;
	public static double				SYNERGY_BOOST_3_SUPPORTS;
	public static double				PVP_EXP_MUL;
	public static double				SUP_PVP_EXP_MUL;
	public static double				KARMA_EXP_LOST_MUL;
	public static boolean				MULTISELL_UNTRADEABLE_SOURCE_ITEMS_PVP_SERVER;
	public static int					CHANCE_EFFECT_DISPLAY;
	public static boolean				ENABLE_SKILL_ANIMATIONS;
	public static boolean				ENABLE_BOT_CAPTCHA;
	public static boolean				ENABLE_BONUS_PVP;
	public static boolean				ENABLE_CLAN_WAR_BONUS_PVP;
	public static int					BONUS_PVP_AMMOUNT;
	public static int					BONUS_PVP_AMMOUNT_2_SIDE;
	public static int					BONUS_PVP_AMMOUNT_1_SIDE;
	public static int					BONUS_CLAN_REP_AMMOUNT_2_SIDE;
	public static int					BONUS_CLAN_REP_AMMOUNT_1_SIDE;
	public static double				EVENT_HEAL_MUL;
	public static double				EVENT_MPCONSUME_MUL;

	public static boolean				AUTO_TVT_ENABLED;
	public static int[][]				AUTO_TVT_TEAM_LOCATIONS;
	public static boolean				AUTO_TVT_TEAM_COLORS_RANDOM;
	public static int[]					AUTO_TVT_TEAM_COLORS;
	public static boolean				AUTO_TVT_OVERRIDE_TELE_BACK;
	public static int[]					AUTO_TVT_DEFAULT_TELE_BACK;
	public static int					AUTO_TVT_REWARD_MIN_POINTS;
	public static int[]					AUTO_TVT_REWARD_IDS;
	public static int[]					AUTO_TVT_REWARD_COUNT;
	public static int					AUTO_TVT_LEVEL_MAX;
	public static int					AUTO_TVT_LEVEL_MIN;
	public static int					AUTO_TVT_PARTICIPANTS_MAX;
	public static int					AUTO_TVT_PARTICIPANTS_MIN;
	public static long					AUTO_TVT_DELAY_INITIAL_REGISTRATION;
	public static long					AUTO_TVT_DELAY_BETWEEN_EVENTS;
	public static long					AUTO_TVT_PERIOD_LENGHT_REGISTRATION;
	public static long					AUTO_TVT_PERIOD_LENGHT_PREPARATION;
	public static long					AUTO_TVT_PERIOD_LENGHT_EVENT;
	public static long					AUTO_TVT_PERIOD_LENGHT_REWARDS;
	public static int					AUTO_TVT_REGISTRATION_ANNOUNCEMENT_COUNT;
	public static boolean				AUTO_TVT_REGISTER_CURSED;
	public static boolean				AUTO_TVT_REGISTER_HERO;
	public static boolean				AUTO_TVT_REGISTER_CANCEL;
	public static boolean				AUTO_TVT_REGISTER_AFTER_RELOG;
	public static int[]					AUTO_TVT_DISALLOWED_ITEMS;
	public static boolean				AUTO_TVT_START_CANCEL_BUFFS;
	public static boolean				AUTO_TVT_START_CANCEL_CUBICS;
	public static boolean				AUTO_TVT_START_CANCEL_SERVITORS;
	public static boolean				AUTO_TVT_START_CANCEL_TRANSFORMATION;
	public static boolean				AUTO_TVT_START_CANCEL_PARTY;
	public static boolean				AUTO_TVT_START_RECOVER;
	public static boolean				AUTO_TVT_GODLIKE_SYSTEM;
	public static boolean				AUTO_TVT_GODLIKE_ANNOUNCE;
	public static int					AUTO_TVT_GODLIKE_MIN_KILLS;
	public static int					AUTO_TVT_GODLIKE_POINT_MULTIPLIER;
	public static String				AUTO_TVT_GODLIKE_TITLE;
	public static boolean				AUTO_TVT_REVIVE_SELF;
	public static boolean				AUTO_TVT_REVIVE_RECOVER;
	public static long					AUTO_TVT_REVIVE_DELAY;
	public static int					AUTO_TVT_SPAWN_PROTECT;
	public static String				FortressSiege_EVEN_TEAMS;
	public static boolean				FortressSiege_SAME_IP_PLAYERS_ALLOWED;
	public static boolean				FortressSiege_ALLOW_INTERFERENCE;
	public static boolean				FortressSiege_ALLOW_POTIONS;
	public static boolean				FortressSiege_ALLOW_SUMMON;
	public static boolean				FortressSiege_ON_START_REMOVE_ALL_EFFECTS;
	public static boolean				FortressSiege_ON_START_UNSUMMON_PET;
	public static boolean				FortressSiege_ANNOUNCE_TEAM_STATS;
	public static boolean				FortressSiege_JOIN_CURSED;
	public static boolean				FortressSiege_REVIVE_RECOVERY;
	public static boolean				FortressSiege_PRICE_NO_KILLS;
	public static long					FortressSiege_REVIVE_DELAY;
	public static boolean				FortressSiege_HWID_CHECK;
	/** ************************************************** **/
	/** Automatic events - end **/
	/** ************************************************** **/
	public static int					RAIDBOSS_CHANCE_1;
	public static int					RAIDBOSS_CHANCE_2;
	public static int					RAIDBOSS_CHANCE_3;
	public static int					RAIDBOSS_CHANCE_4;
	public static int					RAIDBOSS_CHANCE_5;
	public static int					RAIDBOSS_CHANCE_6;
	public static int					RAIDBOSS_CHANCE_7;
	public static int					RAIDBOSS_CHANCE_8;
	public static int					RAIDBOSS_CHANCE_9;
	public static int					RAIDBOSS_CHANCE_10;
	public static int					RAIDBOSS_CHANCE_11;
	public static int					RAIDBOSS_CHANCE_12;
	public static int					RAIDBOSS_CHANCE_13;
	public static int					RAIDBOSS_CHANCE_14;
	public static int					RAIDBOSS_CHANCE_15;
	public static long					UNTRADEABLE_GM_ENCHANT;
	public static long					UNTRADEABLE_GM_TRADE;
	public static long					UNTRADEABLE_DONATE;
	public static long					CHANCE_SKILL_DELAY;
	public static long					CHANCE_SKILL_BOW_DELAY;
	public static int					SPELL_CANCEL_CHANCE;
	public static int					ATTACK_CANCEL_CHANCE;
	public static int					APC_ATTACK_ROW;
	public static int					APC_ATTACK_ROW_BALANCED;
	public static int					APC_PATHFIND;
	public static boolean				NpcBuffer_VIP;
	public static int					NpcBuffer_VIP_ALV;
	public static boolean				NpcBuffer_EnableBuff;
	public static boolean				NpcBuffer_EnableScheme;
	public static boolean				NpcBuffer_EnableHeal;
	public static boolean				NpcBuffer_EnableBuffs;
	public static boolean				NpcBuffer_EnableResist;
	public static boolean				NpcBuffer_EnableSong;
	public static boolean				NpcBuffer_EnableDance;
	public static boolean				NpcBuffer_EnableChant;
	public static boolean				NpcBuffer_EnableOther;
	public static boolean				NpcBuffer_EnableSpecial;
	public static boolean				NpcBuffer_EnableCubic;
	public static boolean				NpcBuffer_EnableCancel;
	public static boolean				NpcBuffer_EnableBuffSet;
	public static boolean				NpcBuffer_EnableBuffPK;
	public static boolean				NpcBuffer_EnableFreeBuffs;
	public static boolean				NpcBuffer_EnableTimeOut;
	public static int					NpcBuffer_TimeOutTime;
	public static int					NpcBuffer_MinLevel;
	public static int					NpcBuffer_PriceCancel;
	public static int					NpcBuffer_PriceHeal;
	public static int					NpcBuffer_PriceBuffs;
	public static int					NpcBuffer_PriceResist;
	public static int					NpcBuffer_PriceSong;
	public static int					NpcBuffer_PriceDance;
	public static int					NpcBuffer_PriceChant;
	public static int					NpcBuffer_PriceOther;
	public static int					NpcBuffer_PriceSpecial;
	public static int					NpcBuffer_PriceCubic;
	public static int					NpcBuffer_PriceSet;
	public static int					NpcBuffer_PriceScheme;
	public static int					NpcBuffer_MaxScheme;
	public static boolean				SCHEME_ALLOW_FLAG;
	public static List<int[]>			NpcBuffer_BuffSetMage		= new ArrayList<int[]>();
	public static List<int[]>			NpcBuffer_BuffSetFighter	= new ArrayList<int[]>();
	public static List<int[]>			NpcBuffer_BuffSetDagger		= new ArrayList<int[]>();
	public static List<int[]>			NpcBuffer_BuffSetSupport	= new ArrayList<int[]>();
	public static List<int[]>			NpcBuffer_BuffSetTank		= new ArrayList<int[]>();
	public static List<int[]>			NpcBuffer_BuffSetArcher		= new ArrayList<int[]>();
	
	
	public static int					KAMALOKA_PVPS;
	public static int					KAMALOKA_SUPPORT_PVPS;
	public static int					KAMALOKA_LEVELS;
	
	public static int					KAMALOKA_HARD_PVPS;
	public static int					KAMALOKA_HARD_SUPPORT_PVPS;
	public static int					KAMALOKA_HARD_LEVELS;
	
	public static int					EMBRYO_PVPS;
	public static int					EMBRYO_SUPPORT_PVPS;
	public static int					EMBRYO_LEVELS;
	
	public static int					SOLO_PVPS;
	public static int					SOLO_SUPPORT_PVPS;
	public static int					SOLO_LEVELS;
	

	public static int					FAFURION_PVPS;
	public static int					FAFURION_SUPPORT_PVPS;
	public static int					FAFURION_LEVELS;
	public static int					FAFURION_HARD_PVPS;
	public static int					FAFURION_HARD_SUPPORT_PVPS;
	public static int					FAFURION_HARD_LEVELS;
	
	public static int					ZAKEN_PVPS;
	public static int					ZAKEN_SUPPORT_PVPS;
	public static int					ZAKEN_LEVELS;
	public static int					ZAKEN_HARD_PVPS;
	public static int					ZAKEN_HARD_SUPPORT_PVPS;
	public static int					ZAKEN_HARD_LEVELS;
	
	public static int					FRINTEZZA_PVPS;
	public static int					FRINTEZZA_SUPPORT_PVPS;
	public static int					FRINTEZZA_LEVELS;
	public static int					FRINTEZZA_HARD_PVPS;
	public static int					FRINTEZZA_HARD_SUPPORT_PVPS;
	public static int					FRINTEZZA_HARD_LEVELS;
	
	public static int					FREYA_PVPS;
	public static int					FREYA_SUPPORT_PVPS;
	public static int					FREYA_LEVELS;
	public static int					FREYA_HARD_PVPS;
	public static int					FREYA_HARD_SUPPORT_PVPS;
	public static int					FREYA_HARD_LEVELS;
	
	public static int					ADEN_PVPS;
	public static int					ADEN_SUPPORT_PVPS;
	public static int					ADEN_LEVELS;
	
	public static ExProperties load(String filename)
	{
		return load(new File(filename));
	}
	
	public static ExProperties load(File file)
	{
		ExProperties result = new ExProperties();
		try
		{
			result.load(file);
			_log.log(Level.INFO, "Successfully Loaded " + file.getName());
		}
		catch (IOException e)
		{
			_log.log(Level.SEVERE, "Error loading config : " + file.getName() + "!", e);
		}
		return result;
	}
	
	public static void loadInstances()
	{
		ExProperties INSTANCES = load(INSTANCES_FILE);
		
//		ZAKEN_PVPS = INSTANCES.getProperty("zaken_pvps", 1);
//		ZAKEN_SUPPORT_PVPS = INSTANCES.getProperty("zaken_support_pvps", 1);
//		ZAKEN_LEVELS = INSTANCES.getProperty("zaken_levels", 1);
//		
//		ZAKEN_HARD_PVPS = INSTANCES.getProperty("zaken_hard_pvps", 1);
//		ZAKEN_HARD_SUPPORT_PVPS = INSTANCES.getProperty("zaken_hard_support_pvps", 1);
//		ZAKEN_HARD_LEVELS = INSTANCES.getProperty("zaken_hard_levels", 1);
//		
//		
//		FRINTEZZA_PVPS = INSTANCES.getProperty("frintezza_pvps", 1);
//		FRINTEZZA_SUPPORT_PVPS = INSTANCES.getProperty("frintezza_support_pvps", 1);
//		FRINTEZZA_LEVELS = INSTANCES.getProperty("frintezza_levels", 1);
//		
//		FRINTEZZA_HARD_PVPS = INSTANCES.getProperty("frintezza_hard_pvps", 1);
//		FRINTEZZA_HARD_SUPPORT_PVPS = INSTANCES.getProperty("frintezza_hard_support_pvps", 1);
//		FRINTEZZA_HARD_LEVELS = INSTANCES.getProperty("frintezza_hard_levels", 1);
//		
//		
//		FREYA_PVPS = INSTANCES.getProperty("freya_pvps", 1);
//		FREYA_SUPPORT_PVPS = INSTANCES.getProperty("freya_support_pvps", 1);
//		FREYA_LEVELS = INSTANCES.getProperty("freya_levels", 1);
//		
//		FREYA_HARD_PVPS = INSTANCES.getProperty("freya_hard_pvps", 1);
//		FREYA_HARD_SUPPORT_PVPS = INSTANCES.getProperty("freya_hard_support_pvps", 1);
//		FREYA_HARD_LEVELS = INSTANCES.getProperty("freya_hard_levels", 1);



		KAMALOKA_PVPS = INSTANCES.getProperty("kamaloka_pvps", 70);
		KAMALOKA_SUPPORT_PVPS = INSTANCES.getProperty("kamaloka_support_pvps", 35);
		KAMALOKA_LEVELS = INSTANCES.getProperty("kamaloka_level", 87);

		KAMALOKA_HARD_PVPS = INSTANCES.getProperty("kamaloka_hard_pvps", 1000);
		KAMALOKA_HARD_SUPPORT_PVPS = INSTANCES.getProperty("kamaloka_hard_support_pvps", 600);
		KAMALOKA_HARD_LEVELS = INSTANCES.getProperty("kamaloka_hard_level", 91);
		
		EMBRYO_PVPS = INSTANCES.getProperty("embryo_pvps", 500);
		EMBRYO_SUPPORT_PVPS = INSTANCES.getProperty("embryo_support_pvps", 250);
		EMBRYO_LEVELS = INSTANCES.getProperty("embryo_level", 90);
		
		SOLO_PVPS = INSTANCES.getProperty("solo_pvps", 50);
		SOLO_SUPPORT_PVPS = INSTANCES.getProperty("solo_support_pvps", 50);
		SOLO_LEVELS = INSTANCES.getProperty("solo_level", 86);
		
		FAFURION_PVPS = INSTANCES.getProperty("fafurion_pvps", 800);
		FAFURION_SUPPORT_PVPS = INSTANCES.getProperty("fafurion_support_pvps", 400);
		FAFURION_LEVELS = INSTANCES.getProperty("fafurion_level", 90);
		
		FAFURION_HARD_PVPS = INSTANCES.getProperty("fafurion_hard_pvps", 800);
		FAFURION_HARD_SUPPORT_PVPS = INSTANCES.getProperty("fafurion_hard_support_pvps", 400);
		FAFURION_HARD_LEVELS = INSTANCES.getProperty("fafurion_hard_level", 90);
		
		ADEN_PVPS = INSTANCES.getProperty("aden_pvps", 1000);
		ADEN_SUPPORT_PVPS = INSTANCES.getProperty("aden_support_pvps", 500);
		ADEN_LEVELS = INSTANCES.getProperty("aden_level", 91);
		
		ZAKEN_PVPS = INSTANCES.getProperty("zaken_pvps", 1);
		ZAKEN_SUPPORT_PVPS = INSTANCES.getProperty("zaken_support_pvps", 1);
		ZAKEN_LEVELS = INSTANCES.getProperty("zaken_levels", 1);
		ZAKEN_HARD_PVPS = INSTANCES.getProperty("zaken_hard_pvps", 1);
		ZAKEN_HARD_SUPPORT_PVPS = INSTANCES.getProperty("zaken_hard_support_pvps", 1);
		ZAKEN_HARD_LEVELS = INSTANCES.getProperty("zaken_hard_levels", 1);
		
		
		FRINTEZZA_PVPS = INSTANCES.getProperty("frintezza_pvps", 1);
		FRINTEZZA_SUPPORT_PVPS = INSTANCES.getProperty("frintezza_support_pvps", 1);
		FRINTEZZA_LEVELS = INSTANCES.getProperty("frintezza_levels", 1);
		FRINTEZZA_HARD_PVPS = INSTANCES.getProperty("frintezza_hard_pvps", 1);
		FRINTEZZA_HARD_SUPPORT_PVPS = INSTANCES.getProperty("frintezza_hard_support_pvps", 1);
		FRINTEZZA_HARD_LEVELS = INSTANCES.getProperty("frintezza_hard_levels", 1);
		
		
		FREYA_PVPS = INSTANCES.getProperty("freya_pvps", 1);
		FREYA_SUPPORT_PVPS = INSTANCES.getProperty("freya_support_pvps", 1);
		FREYA_LEVELS = INSTANCES.getProperty("freya_levels", 1);
		FREYA_HARD_PVPS = INSTANCES.getProperty("freya_hard_pvps", 1);
		FREYA_HARD_SUPPORT_PVPS = INSTANCES.getProperty("freya_hard_support_pvps", 1);
		FREYA_HARD_LEVELS = INSTANCES.getProperty("freya_hard_levels", 1);
	}
	
	
	public static void loadSchemeBuffer()
	{
		ExProperties npcbuffer = load(NPCBUFFER_CONFIG_FILE);
		NpcBuffer_VIP = npcbuffer.getProperty("EnableVIP", false);
		NpcBuffer_VIP_ALV = npcbuffer.getProperty("VipAccesLevel", 1);
		NpcBuffer_EnableBuff = npcbuffer.getProperty("EnableBuffSection", true);
		NpcBuffer_EnableScheme = npcbuffer.getProperty("EnableScheme", true);
		NpcBuffer_EnableHeal = npcbuffer.getProperty("EnableHeal", true);
		NpcBuffer_EnableBuffs = npcbuffer.getProperty("EnableBuffs", true);
		NpcBuffer_EnableResist = npcbuffer.getProperty("EnableResist", true);
		NpcBuffer_EnableSong = npcbuffer.getProperty("EnableSongs", true);
		NpcBuffer_EnableDance = npcbuffer.getProperty("EnableDances", true);
		NpcBuffer_EnableChant = npcbuffer.getProperty("EnableChants", true);
		NpcBuffer_EnableOther = npcbuffer.getProperty("EnableOther", true);
		NpcBuffer_EnableSpecial = npcbuffer.getProperty("EnableSpecial", true);
		NpcBuffer_EnableCubic = npcbuffer.getProperty("EnableCubic", false);
		NpcBuffer_EnableCancel = npcbuffer.getProperty("EnableRemoveBuffs", true);
		NpcBuffer_EnableBuffSet = npcbuffer.getProperty("EnableBuffSet", true);
		NpcBuffer_EnableBuffPK = npcbuffer.getProperty("EnableBuffForPK", false);
		NpcBuffer_EnableFreeBuffs = npcbuffer.getProperty("EnableFreeBuffs", true);
		NpcBuffer_EnableTimeOut = npcbuffer.getProperty("EnableTimeOut", true);
		SCHEME_ALLOW_FLAG = npcbuffer.getProperty("EnableBuffforFlag", false);
		NpcBuffer_TimeOutTime = npcbuffer.getProperty("TimeoutTime", 10);
		NpcBuffer_MinLevel = npcbuffer.getProperty("MinimumLevel", 20);
		NpcBuffer_PriceCancel = npcbuffer.getProperty("RemoveBuffsPrice", 100000);
		NpcBuffer_PriceHeal = npcbuffer.getProperty("HealPrice", 100000);
		NpcBuffer_PriceBuffs = npcbuffer.getProperty("BuffsPrice", 100000);
		NpcBuffer_PriceResist = npcbuffer.getProperty("ResistPrice", 100000);
		NpcBuffer_PriceSong = npcbuffer.getProperty("SongPrice", 100000);
		NpcBuffer_PriceDance = npcbuffer.getProperty("DancePrice", 100000);
		NpcBuffer_PriceChant = npcbuffer.getProperty("ChantsPrice", 100000);
		NpcBuffer_PriceOther = npcbuffer.getProperty("OtherPrice", 100000);
		NpcBuffer_PriceSpecial = npcbuffer.getProperty("SpecialPrice", 100000);
		NpcBuffer_PriceCubic = npcbuffer.getProperty("CubicPrice", 100000);
		NpcBuffer_PriceSet = npcbuffer.getProperty("SetPrice", 100000);
		NpcBuffer_PriceScheme = npcbuffer.getProperty("SchemePrice", 100000);
		NpcBuffer_MaxScheme = npcbuffer.getProperty("MaxScheme", 4);
		String[] parts;
		String[] skills = npcbuffer.getProperty("BuffSetMage", "192,1").split(";");
		for (String sk : skills)
		{
			parts = sk.split(",");
			NpcBuffer_BuffSetMage.add(new int[]
			{
				Integer.parseInt(parts[0]), Integer.parseInt(parts[1])
			});
		}
		skills = npcbuffer.getProperty("BuffSetFighter", "192,1").split(";");
		for (String sk : skills)
		{
			parts = sk.split(",");
			NpcBuffer_BuffSetFighter.add(new int[]
			{
				Integer.parseInt(parts[0]), Integer.parseInt(parts[1])
			});
		}
		skills = npcbuffer.getProperty("BuffSetDagger", "192,1").split(";");
		for (String sk : skills)
		{
			parts = sk.split(",");
			NpcBuffer_BuffSetDagger.add(new int[]
			{
				Integer.parseInt(parts[0]), Integer.parseInt(parts[1])
			});
		}
		skills = npcbuffer.getProperty("BuffSetSupport", "192,1").split(";");
		for (String sk : skills)
		{
			parts = sk.split(",");
			NpcBuffer_BuffSetSupport.add(new int[]
			{
				Integer.parseInt(parts[0]), Integer.parseInt(parts[1])
			});
		}
		skills = npcbuffer.getProperty("BuffSetTank", "192,1").split(";");
		for (String sk : skills)
		{
			parts = sk.split(",");
			NpcBuffer_BuffSetTank.add(new int[]
			{
				Integer.parseInt(parts[0]), Integer.parseInt(parts[1])
			});
		}
		skills = npcbuffer.getProperty("BuffSetArcher", "192,1").split(";");
		for (String sk : skills)
		{
			parts = sk.split(",");
			NpcBuffer_BuffSetArcher.add(new int[]
			{
				Integer.parseInt(parts[0]), Integer.parseInt(parts[1])
			});
		}
	}
	
	/**
	 * This class initializes all global variables for configuration.<br>
	 * If the key doesn't appear in properties file, a default value is set by this class.
	 * 
	 * @see CONFIGURATION_FILE (properties file) for configuring your server.
	 */
	public static void load()
	{
		if (Server.serverMode == Server.MODE_GAMESERVER)
		{
			_log.info("Loading GameServer Configuration Files...");
			loadSchemeBuffer();
			loadInstances();
			InputStream is = null;
			try
			{
				try
				{
					L2Properties RAID = new L2Properties();
					is = new FileInputStream(new File(RAIDBOSS_DROP_CHANCES_FILE));
					RAID.load(is);
					RAIDBOSS_CHANCE_1 = Integer.parseInt(RAID.getProperty("raid1", "0"));
					RAIDBOSS_CHANCE_2 = Integer.parseInt(RAID.getProperty("raid2", "0"));
					RAIDBOSS_CHANCE_3 = Integer.parseInt(RAID.getProperty("raid3", "0"));
					RAIDBOSS_CHANCE_4 = Integer.parseInt(RAID.getProperty("raid4", "0"));
					RAIDBOSS_CHANCE_5 = Integer.parseInt(RAID.getProperty("raid5", "0"));
					RAIDBOSS_CHANCE_6 = Integer.parseInt(RAID.getProperty("raid6", "0"));
					RAIDBOSS_CHANCE_7 = Integer.parseInt(RAID.getProperty("raid7", "0"));
					RAIDBOSS_CHANCE_8 = Integer.parseInt(RAID.getProperty("raid8", "0"));
					RAIDBOSS_CHANCE_9 = Integer.parseInt(RAID.getProperty("raid9", "0"));
					RAIDBOSS_CHANCE_10 = Integer.parseInt(RAID.getProperty("raid10", "0"));
					RAIDBOSS_CHANCE_11 = Integer.parseInt(RAID.getProperty("raid11", "0"));
					RAIDBOSS_CHANCE_12 = Integer.parseInt(RAID.getProperty("raid12", "0"));
					RAIDBOSS_CHANCE_13 = Integer.parseInt(RAID.getProperty("raid13", "0"));
					RAIDBOSS_CHANCE_14 = Integer.parseInt(RAID.getProperty("raid14", "0"));
					RAIDBOSS_CHANCE_15 = Integer.parseInt(RAID.getProperty("raid15", "0"));
					UNTRADEABLE_GM_ENCHANT = Long.parseLong(RAID.getProperty("untradeGMenchant", "504")); // default in hours - 336 = 2 weeks
					UNTRADEABLE_GM_TRADE = Long.parseLong(RAID.getProperty("untradeGMtrade", "504")); // default in hours - 336 = 2 weeks
					UNTRADEABLE_DONATE = Long.parseLong(RAID.getProperty("untradeDonate", "504")); // default in hours - 336 = 2 weeks
					CHANCE_SKILL_DELAY = Long.parseLong(RAID.getProperty("chanceSkillDelayMili", "5"));
					CHANCE_SKILL_BOW_DELAY = Long.parseLong(RAID.getProperty("chanceSkillBowDelayMili", "300"));
					SPELL_CANCEL_CHANCE = Integer.parseInt(RAID.getProperty("spellCancelChance", "10"));
					ATTACK_CANCEL_CHANCE = Integer.parseInt(RAID.getProperty("attackCancelChance", "5"));
					APC_ATTACK_ROW = Integer.parseInt(RAID.getProperty("apcMinAttackRow", "3"));
					APC_ATTACK_ROW_BALANCED = Integer.parseInt(RAID.getProperty("apcMinAttackRowBalanced", "3"));
					APC_PATHFIND = Integer.parseInt(RAID.getProperty("apcPathfind", "0"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + RAIDBOSS_DROP_CHANCES_FILE + " File.");
				}
				try
				{
					L2Properties serverSettings = new L2Properties();
					is = new FileInputStream(new File(CONFIGURATION_FILE));
					serverSettings.load(is);
					GAMESERVER_HOSTNAME = serverSettings.getProperty("GameserverHostname");
					PORT_GAME = Integer.parseInt(serverSettings.getProperty("GameserverPort", "7777"));
					EXTERNAL_HOSTNAME = serverSettings.getProperty("ExternalHostname", "*");
					INTERNAL_HOSTNAME = serverSettings.getProperty("InternalHostname", "*");
					GAME_SERVER_LOGIN_PORT = Integer.parseInt(serverSettings.getProperty("LoginPort", "9014"));
					GAME_SERVER_LOGIN_HOST = serverSettings.getProperty("LoginHost", "127.0.0.1");
					REQUEST_ID = Integer.parseInt(serverSettings.getProperty("RequestServerID", "0"));
					ACCEPT_ALTERNATE_ID = Boolean.parseBoolean(serverSettings.getProperty("AcceptAlternateID", "True"));
					DATABASE_DRIVER = serverSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
					DATABASE_URL = serverSettings.getProperty("URL", "jdbc:mysql://localhost/l2jdb");
					DATABASE_LOGIN = serverSettings.getProperty("Login", "root");
					DATABASE_PASSWORD = serverSettings.getProperty("Password", "");
					DATABASE_MAX_CONNECTIONS = Integer.parseInt(serverSettings.getProperty("MaximumDbConnections", "10"));
					DATABASE_MAX_IDLE_TIME = Integer.parseInt(serverSettings.getProperty("MaximumDbIdleTime", "0"));
					DATAPACK_ROOT = new File(serverSettings.getProperty("DatapackRoot", ".")).getCanonicalFile();
					CNAME_TEMPLATE = serverSettings.getProperty("CnameTemplate", ".*");
					PET_NAME_TEMPLATE = serverSettings.getProperty("PetNameTemplate", ".*");
					MAX_CHARACTERS_NUMBER_PER_ACCOUNT = Integer.parseInt(serverSettings.getProperty("CharMaxNumber", "0"));
					MAXIMUM_ONLINE_USERS = Integer.parseInt(serverSettings.getProperty("MaximumOnlineUsers", "100"));
					MIN_PROTOCOL_REVISION = Integer.parseInt(serverSettings.getProperty("MinProtocolRevision", "660"));
					MAX_PROTOCOL_REVISION = Integer.parseInt(serverSettings.getProperty("MaxProtocolRevision", "665"));
					SERVER_LOCAL = Boolean.parseBoolean(serverSettings.getProperty("ServerLocal", "True"));
					try
					{
						SCRIPT_ROOT = new File(serverSettings.getString("ScriptRoot", "./data/scripts").replaceAll("\\\\", "/")).getCanonicalFile();
					}
					catch (Exception e)
					{
						_log.warning("Error setting script root!");
						SCRIPT_ROOT = new File(".");
					}
					if (MIN_PROTOCOL_REVISION > MAX_PROTOCOL_REVISION)
					{
						throw new Error("MinProtocolRevision is bigger than MaxProtocolRevision in server configuration file.");
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + CONFIGURATION_FILE + " File.");
				}
				// Load Smart CB Properties file (if exists)
				final File smartcb = new File(SMART_CB);
				try (InputStream iss = new FileInputStream(smartcb))
				{
					L2Properties smartCB = new L2Properties();
					smartCB.load(iss);
					TOP_PLAYER_ROW_HEIGHT = Integer.parseInt(smartCB.getProperty("TopPlayerRowHeight", "19"));
					TOP_PLAYER_RESULTS = Integer.parseInt(smartCB.getProperty("TopPlayerResults", "20"));
					RAID_LIST_ROW_HEIGHT = Integer.parseInt(smartCB.getProperty("RaidListRowHeight", "18"));
					RAID_LIST_RESULTS = Integer.parseInt(smartCB.getProperty("RaidListResults", "20"));
					RAID_LIST_SORT_ASC = Boolean.parseBoolean(smartCB.getProperty("RaidListSortAsc", "True"));
					ALLOW_REAL_ONLINE_STATS = Boolean.parseBoolean(smartCB.getProperty("AllowRealOnlineStats", "True"));
				}
				catch (Exception e)
				{
					_log.warning("Config: " + e.getMessage());
					throw new Error("Failed to Load " + SMART_CB + " File.");
				}
				// Load Feature Properties file (if exists)
				try
				{
					L2Properties Feature = new L2Properties();
					is = new FileInputStream(new File(FEATURE_CONFIG_FILE));
					Feature.load(is);
					CH_TELE_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallTeleportFunctionFeeRatio", "604800000"));
					CH_TELE1_FEE = Integer.parseInt(Feature.getProperty("ClanHallTeleportFunctionFeeLvl1", "7000"));
					CH_TELE2_FEE = Integer.parseInt(Feature.getProperty("ClanHallTeleportFunctionFeeLvl2", "14000"));
					CH_SUPPORT_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallSupportFunctionFeeRatio", "86400000"));
					CH_SUPPORT1_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl1", "2500"));
					CH_SUPPORT2_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl2", "5000"));
					CH_SUPPORT3_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl3", "7000"));
					CH_SUPPORT4_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl4", "11000"));
					CH_SUPPORT5_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl5", "21000"));
					CH_SUPPORT6_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl6", "36000"));
					CH_SUPPORT7_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl7", "37000"));
					CH_SUPPORT8_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl8", "52000"));
					CH_MPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallMpRegenerationFunctionFeeRatio", "86400000"));
					CH_MPREG1_FEE = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl1", "2000"));
					CH_MPREG2_FEE = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl2", "3750"));
					CH_MPREG3_FEE = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl3", "6500"));
					CH_MPREG4_FEE = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl4", "13750"));
					CH_MPREG5_FEE = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl5", "20000"));
					CH_HPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallHpRegenerationFunctionFeeRatio", "86400000"));
					CH_HPREG1_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl1", "700"));
					CH_HPREG2_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl2", "800"));
					CH_HPREG3_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl3", "1000"));
					CH_HPREG4_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl4", "1166"));
					CH_HPREG5_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl5", "1500"));
					CH_HPREG6_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl6", "1750"));
					CH_HPREG7_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl7", "2000"));
					CH_HPREG8_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl8", "2250"));
					CH_HPREG9_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl9", "2500"));
					CH_HPREG10_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl10", "3250"));
					CH_HPREG11_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl11", "3270"));
					CH_HPREG12_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl12", "4250"));
					CH_HPREG13_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl13", "5166"));
					CH_EXPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallExpRegenerationFunctionFeeRatio", "86400000"));
					CH_EXPREG1_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl1", "3000"));
					CH_EXPREG2_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl2", "6000"));
					CH_EXPREG3_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl3", "9000"));
					CH_EXPREG4_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl4", "15000"));
					CH_EXPREG5_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl5", "21000"));
					CH_EXPREG6_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl6", "23330"));
					CH_EXPREG7_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl7", "30000"));
					CH_ITEM_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallItemCreationFunctionFeeRatio", "86400000"));
					CH_ITEM1_FEE = Integer.parseInt(Feature.getProperty("ClanHallItemCreationFunctionFeeLvl1", "30000"));
					CH_ITEM2_FEE = Integer.parseInt(Feature.getProperty("ClanHallItemCreationFunctionFeeLvl2", "70000"));
					CH_ITEM3_FEE = Integer.parseInt(Feature.getProperty("ClanHallItemCreationFunctionFeeLvl3", "140000"));
					CH_CURTAIN_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallCurtainFunctionFeeRatio", "604800000"));
					CH_CURTAIN1_FEE = Integer.parseInt(Feature.getProperty("ClanHallCurtainFunctionFeeLvl1", "2000"));
					CH_CURTAIN2_FEE = Integer.parseInt(Feature.getProperty("ClanHallCurtainFunctionFeeLvl2", "2500"));
					CH_FRONT_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallFrontPlatformFunctionFeeRatio", "259200000"));
					CH_FRONT1_FEE = Integer.parseInt(Feature.getProperty("ClanHallFrontPlatformFunctionFeeLvl1", "1300"));
					CH_FRONT2_FEE = Integer.parseInt(Feature.getProperty("ClanHallFrontPlatformFunctionFeeLvl2", "4000"));
					CL_SET_SIEGE_TIME_LIST = new FastList<String>();
					SIEGE_HOUR_LIST_MORNING = new FastList<Integer>();
					SIEGE_HOUR_LIST_AFTERNOON = new FastList<Integer>();
					String[] sstl = Feature.getProperty("CLSetSiegeTimeList", "").split(",");
					if (sstl.length != 0)
					{
						boolean isHour = false;
						for (String st : sstl)
						{
							if (st.equalsIgnoreCase("day") || st.equalsIgnoreCase("hour") || st.equalsIgnoreCase("minute"))
							{
								if (st.equalsIgnoreCase("hour"))
									isHour = true;
								CL_SET_SIEGE_TIME_LIST.add(st.toLowerCase());
							}
							else
							{
								_log.warning(StringUtil.concat("[CLSetSiegeTimeList]: invalid config property -> CLSetSiegeTimeList \"", st, "\""));
							}
						}
						if (isHour)
						{
							String[] shl = Feature.getProperty("SiegeHourList", "").split(",");
							for (String st : shl)
							{
								if (!st.equalsIgnoreCase(""))
								{
									int val = Integer.parseInt(st);
									if (val > 23 || val < 0)
										_log.warning(StringUtil.concat("[SiegeHourList]: invalid config property -> SiegeHourList \"", st, "\""));
									else if (val < 12)
										SIEGE_HOUR_LIST_MORNING.add(val);
									else
									{
										val -= 12;
										SIEGE_HOUR_LIST_AFTERNOON.add(val);
									}
								}
							}
							if (Config.SIEGE_HOUR_LIST_AFTERNOON.isEmpty() && Config.SIEGE_HOUR_LIST_AFTERNOON.isEmpty())
							{
								_log.warning("[SiegeHourList]: invalid config property -> SiegeHourList is empty");
								CL_SET_SIEGE_TIME_LIST.remove("hour");
							}
						}
					}
					CS_TELE_FEE_RATIO = Long.parseLong(Feature.getProperty("CastleTeleportFunctionFeeRatio", "604800000"));
					CS_TELE1_FEE = Integer.parseInt(Feature.getProperty("CastleTeleportFunctionFeeLvl1", "7000"));
					CS_TELE2_FEE = Integer.parseInt(Feature.getProperty("CastleTeleportFunctionFeeLvl2", "14000"));
					CS_SUPPORT_FEE_RATIO = Long.parseLong(Feature.getProperty("CastleSupportFunctionFeeRatio", "86400000"));
					CS_SUPPORT1_FEE = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl1", "7000"));
					CS_SUPPORT2_FEE = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl2", "21000"));
					CS_SUPPORT3_FEE = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl3", "37000"));
					CS_SUPPORT4_FEE = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl4", "52000"));
					CS_MPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("CastleMpRegenerationFunctionFeeRatio", "86400000"));
					CS_MPREG1_FEE = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl1", "2000"));
					CS_MPREG2_FEE = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl2", "6500"));
					CS_MPREG3_FEE = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl3", "13750"));
					CS_MPREG4_FEE = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl4", "20000"));
					CS_HPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("CastleHpRegenerationFunctionFeeRatio", "86400000"));
					CS_HPREG1_FEE = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl1", "1000"));
					CS_HPREG2_FEE = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl2", "1500"));
					CS_HPREG3_FEE = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl3", "2250"));
					CS_HPREG4_FEE = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl4", "3270"));
					CS_HPREG5_FEE = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl5", "5166"));
					CS_EXPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("CastleExpRegenerationFunctionFeeRatio", "86400000"));
					CS_EXPREG1_FEE = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl1", "9000"));
					CS_EXPREG2_FEE = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl2", "15000"));
					CS_EXPREG3_FEE = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl3", "21000"));
					CS_EXPREG4_FEE = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl4", "30000"));
					FS_TELE_FEE_RATIO = Long.parseLong(Feature.getProperty("FortressTeleportFunctionFeeRatio", "604800000"));
					FS_TELE1_FEE = Integer.parseInt(Feature.getProperty("FortressTeleportFunctionFeeLvl1", "1000"));
					FS_TELE2_FEE = Integer.parseInt(Feature.getProperty("FortressTeleportFunctionFeeLvl2", "10000"));
					FS_SUPPORT_FEE_RATIO = Long.parseLong(Feature.getProperty("FortressSupportFunctionFeeRatio", "86400000"));
					FS_SUPPORT1_FEE = Integer.parseInt(Feature.getProperty("FortressSupportFeeLvl1", "7000"));
					FS_SUPPORT2_FEE = Integer.parseInt(Feature.getProperty("FortressSupportFeeLvl2", "17000"));
					FS_MPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("FortressMpRegenerationFunctionFeeRatio", "86400000"));
					FS_MPREG1_FEE = Integer.parseInt(Feature.getProperty("FortressMpRegenerationFeeLvl1", "6500"));
					FS_MPREG2_FEE = Integer.parseInt(Feature.getProperty("FortressMpRegenerationFeeLvl2", "9300"));
					FS_HPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("FortressHpRegenerationFunctionFeeRatio", "86400000"));
					FS_HPREG1_FEE = Integer.parseInt(Feature.getProperty("FortressHpRegenerationFeeLvl1", "2000"));
					FS_HPREG2_FEE = Integer.parseInt(Feature.getProperty("FortressHpRegenerationFeeLvl2", "3500"));
					FS_EXPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("FortressExpRegenerationFunctionFeeRatio", "86400000"));
					FS_EXPREG1_FEE = Integer.parseInt(Feature.getProperty("FortressExpRegenerationFeeLvl1", "9000"));
					FS_EXPREG2_FEE = Integer.parseInt(Feature.getProperty("FortressExpRegenerationFeeLvl2", "10000"));
					FS_BLOOD_OATH_COUNT = Integer.parseInt(Feature.getProperty("FortressBloodOathCount", "1"));
					FS_BLOOD_OATH_FRQ = Integer.parseInt(Feature.getProperty("FortressBloodOathFrequency", "360"));
					ALT_GAME_CASTLE_DAWN = Boolean.parseBoolean(Feature.getProperty("AltCastleForDawn", "True"));
					ALT_GAME_CASTLE_DUSK = Boolean.parseBoolean(Feature.getProperty("AltCastleForDusk", "True"));
					ALT_GAME_REQUIRE_CLAN_CASTLE = Boolean.parseBoolean(Feature.getProperty("AltRequireClanCastle", "False"));
					ALT_FESTIVAL_MIN_PLAYER = Integer.parseInt(Feature.getProperty("AltFestivalMinPlayer", "5"));
					ALT_MAXIMUM_PLAYER_CONTRIB = Integer.parseInt(Feature.getProperty("AltMaxPlayerContrib", "1000000"));
					ALT_FESTIVAL_MANAGER_START = Long.parseLong(Feature.getProperty("AltFestivalManagerStart", "120000"));
					ALT_FESTIVAL_LENGTH = Long.parseLong(Feature.getProperty("AltFestivalLength", "1080000"));
					ALT_FESTIVAL_CYCLE_LENGTH = Long.parseLong(Feature.getProperty("AltFestivalCycleLength", "2280000"));
					ALT_FESTIVAL_FIRST_SPAWN = Long.parseLong(Feature.getProperty("AltFestivalFirstSpawn", "120000"));
					ALT_FESTIVAL_FIRST_SWARM = Long.parseLong(Feature.getProperty("AltFestivalFirstSwarm", "300000"));
					ALT_FESTIVAL_SECOND_SPAWN = Long.parseLong(Feature.getProperty("AltFestivalSecondSpawn", "540000"));
					ALT_FESTIVAL_SECOND_SWARM = Long.parseLong(Feature.getProperty("AltFestivalSecondSwarm", "720000"));
					ALT_FESTIVAL_CHEST_SPAWN = Long.parseLong(Feature.getProperty("AltFestivalChestSpawn", "900000"));
					ALT_SIEGE_DAWN_GATES_PDEF_MULT = Double.parseDouble(Feature.getProperty("AltDawnGatesPdefMult", "1.1"));
					ALT_SIEGE_DUSK_GATES_PDEF_MULT = Double.parseDouble(Feature.getProperty("AltDuskGatesPdefMult", "0.8"));
					ALT_SIEGE_DAWN_GATES_MDEF_MULT = Double.parseDouble(Feature.getProperty("AltDawnGatesMdefMult", "1.1"));
					ALT_SIEGE_DUSK_GATES_MDEF_MULT = Double.parseDouble(Feature.getProperty("AltDuskGatesMdefMult", "0.8"));
					TAKE_FORT_POINTS = Integer.parseInt(Feature.getProperty("TakeFortPoints", "200"));
					LOOSE_FORT_POINTS = Integer.parseInt(Feature.getProperty("LooseFortPoints", "400"));
					TAKE_CASTLE_POINTS = Integer.parseInt(Feature.getProperty("TakeCastlePoints", "1500"));
					LOOSE_CASTLE_POINTS = Integer.parseInt(Feature.getProperty("LooseCastlePoints", "3000"));
					CASTLE_DEFENDED_POINTS = Integer.parseInt(Feature.getProperty("CastleDefendedPoints", "750"));
					FESTIVAL_WIN_POINTS = Integer.parseInt(Feature.getProperty("FestivalOfDarknessWin", "200"));
					HERO_POINTS = Integer.parseInt(Feature.getProperty("HeroPoints", "1000"));
					ROYAL_GUARD_COST = Integer.parseInt(Feature.getProperty("CreateRoyalGuardCost", "5000"));
					KNIGHT_UNIT_COST = Integer.parseInt(Feature.getProperty("CreateKnightUnitCost", "10000"));
					KNIGHT_REINFORCE_COST = Integer.parseInt(Feature.getProperty("ReinforceKnightUnitCost", "5000"));
					BALLISTA_POINTS = Integer.parseInt(Feature.getProperty("KillBallistaPoints", "30"));
					BLOODALLIANCE_POINTS = Integer.parseInt(Feature.getProperty("BloodAlliancePoints", "500"));
					BLOODOATH_POINTS = Integer.parseInt(Feature.getProperty("BloodOathPoints", "200"));
					KNIGHTSEPAULETTE_POINTS = Integer.parseInt(Feature.getProperty("KnightsEpaulettePoints", "20"));
					REPUTATION_SCORE_PER_KILL = Integer.parseInt(Feature.getProperty("ReputationScorePerKill", "1"));
					JOIN_ACADEMY_MIN_REP_SCORE = Integer.parseInt(Feature.getProperty("CompleteAcademyMinPoints", "190"));
					JOIN_ACADEMY_MAX_REP_SCORE = Integer.parseInt(Feature.getProperty("CompleteAcademyMaxPoints", "650"));
					RAID_RANKING_1ST = Integer.parseInt(Feature.getProperty("1stRaidRankingPoints", "1250"));
					RAID_RANKING_2ND = Integer.parseInt(Feature.getProperty("2ndRaidRankingPoints", "900"));
					RAID_RANKING_3RD = Integer.parseInt(Feature.getProperty("3rdRaidRankingPoints", "700"));
					RAID_RANKING_4TH = Integer.parseInt(Feature.getProperty("4thRaidRankingPoints", "600"));
					RAID_RANKING_5TH = Integer.parseInt(Feature.getProperty("5thRaidRankingPoints", "450"));
					RAID_RANKING_6TH = Integer.parseInt(Feature.getProperty("6thRaidRankingPoints", "350"));
					RAID_RANKING_7TH = Integer.parseInt(Feature.getProperty("7thRaidRankingPoints", "300"));
					RAID_RANKING_8TH = Integer.parseInt(Feature.getProperty("8thRaidRankingPoints", "200"));
					RAID_RANKING_9TH = Integer.parseInt(Feature.getProperty("9thRaidRankingPoints", "150"));
					RAID_RANKING_10TH = Integer.parseInt(Feature.getProperty("10thRaidRankingPoints", "100"));
					RAID_RANKING_UP_TO_50TH = Integer.parseInt(Feature.getProperty("UpTo50thRaidRankingPoints", "25"));
					RAID_RANKING_UP_TO_100TH = Integer.parseInt(Feature.getProperty("UpTo100thRaidRankingPoints", "12"));
					CLAN_LEVEL_6_COST = Integer.parseInt(Feature.getProperty("ClanLevel6Cost", "10000"));
					CLAN_LEVEL_7_COST = Integer.parseInt(Feature.getProperty("ClanLevel7Cost", "20000"));
					CLAN_LEVEL_8_COST = Integer.parseInt(Feature.getProperty("ClanLevel8Cost", "40000"));
					CLAN_LEVEL_9_COST = Integer.parseInt(Feature.getProperty("ClanLevel9Cost", "40000"));
					CLAN_LEVEL_10_COST = Integer.parseInt(Feature.getProperty("ClanLevel10Cost", "40000"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + FEATURE_CONFIG_FILE + " File.");
				}
				// Load Character Properties file (if exists)
				try
				{
					L2Properties Character = new L2Properties();
					is = new FileInputStream(new File(CHARACTER_CONFIG_FILE));
					Character.load(is);
					MASTERACCESS_LEVEL = Integer.parseInt(Character.getProperty("MasterAccessLevel", "127"));
					MASTERACCESS_NAME_COLOR = Integer.decode(StringUtil.concat("0x", Character.getProperty("MasterNameColor", "00FF00")));
					MASTERACCESS_TITLE_COLOR = Integer.decode(StringUtil.concat("0x", Character.getProperty("MasterTitleColor", "00FF00")));
					ALT_GAME_DELEVEL = Boolean.parseBoolean(Character.getProperty("Delevel", "true"));
					ALT_WEIGHT_LIMIT = Double.parseDouble(Character.getProperty("AltWeightLimit", "1"));
					RUN_SPD_BOOST = Integer.parseInt(Character.getProperty("RunSpeedBoost", "0"));
					DEATH_PENALTY_CHANCE = Integer.parseInt(Character.getProperty("DeathPenaltyChance", "20"));
					RESPAWN_RESTORE_CP = Double.parseDouble(Character.getProperty("RespawnRestoreCP", "0")) / 100;
					RESPAWN_RESTORE_HP = Double.parseDouble(Character.getProperty("RespawnRestoreHP", "70")) / 100;
					RESPAWN_RESTORE_MP = Double.parseDouble(Character.getProperty("RespawnRestoreMP", "70")) / 100;
					HP_REGEN_MULTIPLIER = Double.parseDouble(Character.getProperty("HpRegenMultiplier", "100")) / 100;
					MP_REGEN_MULTIPLIER = Double.parseDouble(Character.getProperty("MpRegenMultiplier", "100")) / 100;
					CP_REGEN_MULTIPLIER = Double.parseDouble(Character.getProperty("CpRegenMultiplier", "100")) / 100;
					ALT_GAME_TIREDNESS = Boolean.parseBoolean(Character.getProperty("AltGameTiredness", "false"));
					/*
					 * ALT_DAGGER_DMG_VS_HEAVY = Float.parseFloat(Character.getProperty("DaggerVSHeavy", "1"));
					 * ALT_DAGGER_DMG_VS_ROBE = Float.parseFloat(Character.getProperty("DaggerVSRobe", "1"));
					 * ALT_DAGGER_DMG_VS_LIGHT = Float.parseFloat(Character.getProperty("DaggerVSLight", "1"));
					 */
					ENABLE_MODIFY_SKILL_DURATION = Boolean.parseBoolean(Character.getProperty("EnableModifySkillDuration", "false"));
					// Create Map only if enabled
					if (ENABLE_MODIFY_SKILL_DURATION)
					{
						SKILL_DURATION_LIST = new FastMap<Integer, Integer>();
						String[] propertySplit;
						propertySplit = Character.getProperty("SkillDurationList", "").split(";");
						for (String skill : propertySplit)
						{
							String[] skillSplit = skill.split(",");
							if (skillSplit.length != 2)
								_log.warning(StringUtil.concat("[SkillDurationList]: invalid config property -> SkillDurationList \"", skill, "\""));
							else
							{
								try
								{
									SKILL_DURATION_LIST.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
								}
								catch (NumberFormatException nfe)
								{
									if (!skill.isEmpty())
									{
										_log.warning(StringUtil.concat("[SkillDurationList]: invalid config property -> SkillList \"", skillSplit[0], "\"", skillSplit[1]));
									}
								}
							}
						}
					}
					ENABLE_MODIFY_SKILL_REUSE = Boolean.parseBoolean(Character.getProperty("EnableModifySkillReuse", "false"));
					// Create Map only if enabled
					if (ENABLE_MODIFY_SKILL_REUSE)
					{
						SKILL_REUSE_LIST = new FastMap<Integer, Integer>();
						String[] propertySplit;
						propertySplit = Character.getProperty("SkillReuseList", "").split(";");
						for (String skill : propertySplit)
						{
							String[] skillSplit = skill.split(",");
							if (skillSplit.length != 2)
								_log.warning(StringUtil.concat("[SkillReuseList]: invalid config property -> SkillReuseList \"", skill, "\""));
							else
							{
								try
								{
									SKILL_REUSE_LIST.put(Integer.valueOf(skillSplit[0]), Integer.valueOf(skillSplit[1]));
								}
								catch (NumberFormatException nfe)
								{
									if (!skill.isEmpty())
										_log.warning(StringUtil.concat("[SkillReuseList]: invalid config property -> SkillList \"", skillSplit[0], "\"", skillSplit[1]));
								}
							}
						}
					}
					AUTO_LEARN_SKILLS = Boolean.parseBoolean(Character.getProperty("AutoLearnSkills", "false"));
					AUTO_LOOT_HERBS = Boolean.parseBoolean(Character.getProperty("AutoLootHerbs", "false"));
					BUFFS_MAX_AMOUNT = Byte.parseByte(Character.getProperty("maxbuffamount", "20"));
					AUTO_LEARN_DIVINE_INSPIRATION = Boolean.parseBoolean(Character.getProperty("AutoLearnDivineInspiration", "false"));
					ALT_GAME_CANCEL_BOW = Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("bow") || Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
					ALT_GAME_CANCEL_CAST = Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("cast") || Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
					EFFECT_CANCELING = Boolean.parseBoolean(Character.getProperty("CancelLesserEffect", "True"));
					ALT_GAME_MAGICFAILURES = Boolean.parseBoolean(Character.getProperty("MagicFailures", "true"));
					PLAYER_FAKEDEATH_UP_PROTECTION = Integer.parseInt(Character.getProperty("PlayerFakeDeathUpProtection", "0"));
					STORE_SKILL_COOLTIME = Boolean.parseBoolean(Character.getProperty("StoreSkillCooltime", "true"));
					SUBCLASS_STORE_SKILL_COOLTIME = Boolean.parseBoolean(Character.getProperty("SubclassStoreSkillCooltime", "false"));
					ALT_GAME_SHIELD_BLOCKS = Boolean.parseBoolean(Character.getProperty("AltShieldBlocks", "false"));
					ALT_PERFECT_SHLD_BLOCK = Integer.parseInt(Character.getProperty("AltPerfectShieldBlockRate", "10"));
					ALLOW_CLASS_MASTERS = Boolean.parseBoolean(Character.getProperty("AllowClassMasters", "False"));
					LIFE_CRYSTAL_NEEDED = Boolean.parseBoolean(Character.getProperty("LifeCrystalNeeded", "true"));
					SP_BOOK_NEEDED = Boolean.parseBoolean(Character.getProperty("SpBookNeeded", "false"));
					ES_SP_BOOK_NEEDED = Boolean.parseBoolean(Character.getProperty("EnchantSkillSpBookNeeded", "true"));
					DIVINE_SP_BOOK_NEEDED = Boolean.parseBoolean(Character.getProperty("DivineInspirationSpBookNeeded", "true"));
					ALT_GAME_SKILL_LEARN = Boolean.parseBoolean(Character.getProperty("AltGameSkillLearn", "false"));
					ALT_GAME_SUBCLASS_WITHOUT_QUESTS = Boolean.parseBoolean(Character.getProperty("AltSubClassWithoutQuests", "False"));
					MAX_RUN_SPEED = Integer.parseInt(Character.getProperty("MaxRunSpeed", "250"));
					ENABLE_VITALITY = Boolean.parseBoolean(Character.getProperty("EnableVitality", "False"));
					RECOVER_VITALITY_ON_RECONNECT = Boolean.parseBoolean(Character.getProperty("RecoverVitalityOnReconnect", "False"));
					MAX_PCRIT_RATE = Integer.parseInt(Character.getProperty("MaxPCritRate", "500"));
					MAX_MCRIT_RATE = Integer.parseInt(Character.getProperty("MaxMCritRate", "200"));
					MAX_PATK_SPEED = Integer.parseInt(Character.getProperty("MaxPAtkSpeed", "1400"));
					MAX_MATK_SPEED = Integer.parseInt(Character.getProperty("MaxMAtkSpeed", "1700"));
					MAX_EVASION = Integer.parseInt(Character.getProperty("MaxEvasion", "200"));
					MAX_SUBCLASS = Byte.parseByte(Character.getProperty("MaxSubclass", "3"));
					MAX_SUBCLASS_LEVEL = Byte.parseByte(Character.getProperty("MaxSubclassLevel", "80"));
					MAX_PVTSTORESELL_SLOTS_DWARF = Integer.parseInt(Character.getProperty("MaxPvtStoreSellSlotsDwarf", "4"));
					MAX_PVTSTORESELL_SLOTS_OTHER = Integer.parseInt(Character.getProperty("MaxPvtStoreSellSlotsOther", "3"));
					MAX_PVTSTOREBUY_SLOTS_DWARF = Integer.parseInt(Character.getProperty("MaxPvtStoreBuySlotsDwarf", "5"));
					MAX_PVTSTOREBUY_SLOTS_OTHER = Integer.parseInt(Character.getProperty("MaxPvtStoreBuySlotsOther", "4"));
					INVENTORY_MAXIMUM_NO_DWARF = Integer.parseInt(Character.getProperty("MaximumSlotsForNoDwarf", "80"));
					INVENTORY_MAXIMUM_DWARF = Integer.parseInt(Character.getProperty("MaximumSlotsForDwarf", "100"));
					INVENTORY_MAXIMUM_GM = Integer.parseInt(Character.getProperty("MaximumSlotsForGMPlayer", "250"));
					MAX_ITEM_IN_PACKET = Math.max(INVENTORY_MAXIMUM_NO_DWARF, Math.max(INVENTORY_MAXIMUM_DWARF, INVENTORY_MAXIMUM_GM));
					WAREHOUSE_SLOTS_DWARF = Integer.parseInt(Character.getProperty("MaximumWarehouseSlotsForDwarf", "120"));
					WAREHOUSE_SLOTS_NO_DWARF = Integer.parseInt(Character.getProperty("MaximumWarehouseSlotsForNoDwarf", "100"));
					WAREHOUSE_SLOTS_CLAN = Integer.parseInt(Character.getProperty("MaximumWarehouseSlotsForClan", "150"));
					FREIGHT_SLOTS = Integer.parseInt(Character.getProperty("MaximumFreightSlots", "20"));
					ENCHANT_CHANCE_WEAPON = Integer.parseInt(Character.getProperty("EnchantChanceWeapon", "50"));
					ENCHANT_CHANCE_ARMOR = Integer.parseInt(Character.getProperty("EnchantChanceArmor", "50"));
					ENCHANT_CHANCE_JEWELRY = Integer.parseInt(Character.getProperty("EnchantChanceJewelry", "50"));
					ENCHANT_CHANCE_ELEMENT = Integer.parseInt(Character.getProperty("EnchantChanceElement", "50"));
					BLESSED_ENCHANT_CHANCE_WEAPON = Integer.parseInt(Character.getProperty("BlessedEnchantChanceWeapon", "50"));
					BLESSED_ENCHANT_CHANCE_ARMOR = Integer.parseInt(Character.getProperty("BlessedEnchantChanceArmor", "50"));
					BLESSED_ENCHANT_CHANCE_JEWELRY = Integer.parseInt(Character.getProperty("BlessedEnchantChanceJewelry", "50"));
					CRYSTAL_ENCHANT_CHANCE_WEAPON = Integer.parseInt(Character.getProperty("CrystalEnchantChanceWeapon", "80"));
					CRYSTAL_ENCHANT_CHANCE_ARMOR = Integer.parseInt(Character.getProperty("CrystalEnchantChanceArmor", "80"));
					CRYSTAL_ENCHANT_CHANCE_JEWELRY = Integer.parseInt(Character.getProperty("CrystalEnchantChanceJewelry", "80"));
					ENCHANT_MAX_WEAPON = Integer.parseInt(Character.getProperty("EnchantMaxWeapon", "0"));
					ENCHANT_MAX_ARMOR = Integer.parseInt(Character.getProperty("EnchantMaxArmor", "0"));
					ENCHANT_MAX_JEWELRY = Integer.parseInt(Character.getProperty("EnchantMaxJewelry", "0"));
					ENCHANT_SAFE_MAX = Integer.parseInt(Character.getProperty("EnchantSafeMax", "3"));
					ENCHANT_SAFE_MAX_FULL = Integer.parseInt(Character.getProperty("EnchantSafeMaxFull", "4"));
					AUGMENTATION_NG_SKILL_CHANCE = Integer.parseInt(Character.getProperty("AugmentationNGSkillChance", "15"));
					AUGMENTATION_NG_GLOW_CHANCE = Integer.parseInt(Character.getProperty("AugmentationNGGlowChance", "0"));
					AUGMENTATION_MID_SKILL_CHANCE = Integer.parseInt(Character.getProperty("AugmentationMidSkillChance", "30"));
					AUGMENTATION_MID_GLOW_CHANCE = Integer.parseInt(Character.getProperty("AugmentationMidGlowChance", "40"));
					AUGMENTATION_HIGH_SKILL_CHANCE = Integer.parseInt(Character.getProperty("AugmentationHighSkillChance", "45"));
					AUGMENTATION_HIGH_GLOW_CHANCE = Integer.parseInt(Character.getProperty("AugmentationHighGlowChance", "70"));
					AUGMENTATION_TOP_SKILL_CHANCE = Integer.parseInt(Character.getProperty("AugmentationTopSkillChance", "60"));
					AUGMENTATION_TOP_GLOW_CHANCE = Integer.parseInt(Character.getProperty("AugmentationTopGlowChance", "100"));
					AUGMENTATION_BASESTAT_CHANCE = Integer.parseInt(Character.getProperty("AugmentationBaseStatChance", "1"));
					AUGMENTATION_ACC_SKILL_CHANCE = Integer.parseInt(Character.getProperty("AugmentationAccSkillChance", "0"));
					String[] array = Character.getProperty("AugmentationBlackList", "6656,6657,6658,6659,6660,6661,6662,8191,10170,10314").split(",");
					AUGMENTATION_BLACKLIST = new int[array.length];
					for (int i = 0; i < array.length; i++)
						AUGMENTATION_BLACKLIST[i] = Integer.parseInt(array[i]);
					Arrays.sort(AUGMENTATION_BLACKLIST);
					ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanBeKilledInPeaceZone", "false"));
					ALT_GAME_KARMA_PLAYER_CAN_SHOP = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanShop", "true"));
					ALT_GAME_KARMA_PLAYER_CAN_TELEPORT = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanTeleport", "true"));
					ALT_GAME_KARMA_PLAYER_CAN_USE_GK = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanUseGK", "false"));
					ALT_GAME_KARMA_PLAYER_CAN_TRADE = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanTrade", "true"));
					ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanUseWareHouse", "true"));
					MAX_PERSONAL_FAME_POINTS = Integer.parseInt(Character.getProperty("MaxPersonalFamePoints", "65535"));
					FORTRESS_ZONE_FAME_TASK_FREQUENCY = Integer.parseInt(Character.getProperty("FortressZoneFameTaskFrequency", "300"));
					FORTRESS_ZONE_FAME_AQUIRE_POINTS = Integer.parseInt(Character.getProperty("FortressZoneFameAquirePoints", "31"));
					CASTLE_ZONE_FAME_TASK_FREQUENCY = Integer.parseInt(Character.getProperty("CastleZoneFameTaskFrequency", "300"));
					CASTLE_ZONE_FAME_AQUIRE_POINTS = Integer.parseInt(Character.getProperty("CastleZoneFameAquirePoints", "125"));
					IS_CRAFTING_ENABLED = Boolean.parseBoolean(Character.getProperty("CraftingEnabled", "true"));
					CRAFT_MASTERWORK = Boolean.parseBoolean(Character.getProperty("CraftMasterwork", "True"));
					DWARF_RECIPE_LIMIT = Integer.parseInt(Character.getProperty("DwarfRecipeLimit", "50"));
					COMMON_RECIPE_LIMIT = Integer.parseInt(Character.getProperty("CommonRecipeLimit", "50"));
					ALT_GAME_CREATION = Boolean.parseBoolean(Character.getProperty("AltGameCreation", "false"));
					ALT_GAME_CREATION_SPEED = Double.parseDouble(Character.getProperty("AltGameCreationSpeed", "1"));
					ALT_GAME_CREATION_XP_RATE = Double.parseDouble(Character.getProperty("AltGameCreationXpRate", "1"));
					ALT_GAME_CREATION_SP_RATE = Double.parseDouble(Character.getProperty("AltGameCreationSpRate", "1"));
					ALT_GAME_CREATION_RARE_XPSP_RATE = Double.parseDouble(Character.getProperty("AltGameCreationRareXpSpRate", "2"));
					ALT_BLACKSMITH_USE_RECIPES = Boolean.parseBoolean(Character.getProperty("AltBlacksmithUseRecipes", "true"));
					ALT_CLAN_JOIN_DAYS = Integer.parseInt(Character.getProperty("DaysBeforeJoinAClan", "1"));
					ALT_CLAN_CREATE_DAYS = Integer.parseInt(Character.getProperty("DaysBeforeCreateAClan", "10"));
					ALT_CLAN_DISSOLVE_DAYS = Integer.parseInt(Character.getProperty("DaysToPassToDissolveAClan", "7"));
					ALT_ALLY_JOIN_DAYS_WHEN_LEAVED = Integer.parseInt(Character.getProperty("DaysBeforeJoinAllyWhenLeaved", "1"));
					ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED = Integer.parseInt(Character.getProperty("DaysBeforeJoinAllyWhenDismissed", "1"));
					ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED = Integer.parseInt(Character.getProperty("DaysBeforeAcceptNewClanWhenDismissed", "1"));
					ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED = Integer.parseInt(Character.getProperty("DaysBeforeCreateNewAllyWhenDissolved", "10"));
					ALT_MAX_NUM_OF_CLANS_IN_ALLY = Integer.parseInt(Character.getProperty("AltMaxNumOfClansInAlly", "3"));
					ALT_CLAN_MEMBERS_FOR_WAR = Integer.parseInt(Character.getProperty("AltClanMembersForWar", "15"));
					ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = Boolean.parseBoolean(Character.getProperty("AltMembersCanWithdrawFromClanWH", "false"));
					REMOVE_CASTLE_CIRCLETS = Boolean.parseBoolean(Character.getProperty("RemoveCastleCirclets", "true"));
					ALT_PARTY_RANGE = Integer.parseInt(Character.getProperty("AltPartyRange", "1600"));
					ALT_PARTY_RANGE2 = Integer.parseInt(Character.getProperty("AltPartyRange2", "1400"));
					STARTING_ADENA = Long.parseLong(Character.getProperty("StartingAdena", "0"));
					STARTING_LEVEL = Byte.parseByte(Character.getProperty("StartingLevel", "1"));
					STARTING_SP = Integer.parseInt(Character.getProperty("StartingSP", "0"));
					AUTO_LOOT = Boolean.parseBoolean(Character.getProperty("AutoLoot", "false"));
					AUTO_LOOT_RAIDS = Boolean.parseBoolean(Character.getProperty("AutoLootRaids", "false"));
					UNSTUCK_INTERVAL = Integer.parseInt(Character.getProperty("UnstuckInterval", "300"));
					PLAYER_SPAWN_PROTECTION = Integer.parseInt(Character.getProperty("PlayerSpawnProtection", "0"));
					RESPAWN_RANDOM_ENABLED = Boolean.parseBoolean(Character.getProperty("RespawnRandomInTown", "True"));
					RESPAWN_RANDOM_MAX_OFFSET = Integer.parseInt(Character.getProperty("RespawnRandomMaxOffset", "50"));
					RESTORE_PLAYER_INSTANCE = Boolean.parseBoolean(Character.getProperty("RestorePlayerInstance", "False"));
					ALLOW_SUMMON_TO_INSTANCE = Boolean.parseBoolean(Character.getProperty("AllowSummonToInstance", "True"));
					PETITIONING_ALLOWED = Boolean.parseBoolean(Character.getProperty("PetitioningAllowed", "True"));
					MAX_PETITIONS_PER_PLAYER = Integer.parseInt(Character.getProperty("MaxPetitionsPerPlayer", "5"));
					MAX_PETITIONS_PENDING = Integer.parseInt(Character.getProperty("MaxPetitionsPending", "25"));
					ALT_GAME_FREIGHTS = Boolean.parseBoolean(Character.getProperty("AltGameFreights", "true"));
					ALT_GAME_FREIGHT_PRICE = Integer.parseInt(Character.getProperty("AltGameFreightPrice", "1000"));
					ALT_GAME_FREE_TELEPORT = Boolean.parseBoolean(Character.getProperty("AltFreeTeleporting", "False"));
					ALT_RECOMMEND = Boolean.parseBoolean(Character.getProperty("AltRecommend", "False"));
					DELETE_DAYS = Integer.parseInt(Character.getProperty("DeleteCharAfterDays", "7"));
					ALT_GAME_EXPONENT_XP = Float.parseFloat(Character.getProperty("AltGameExponentXp", "0."));
					ALT_GAME_EXPONENT_SP = Float.parseFloat(Character.getProperty("AltGameExponentSp", "0."));
					PARTY_XP_CUTOFF_METHOD = Character.getProperty("PartyXpCutoffMethod", "auto");
					PARTY_XP_CUTOFF_PERCENT = Double.parseDouble(Character.getProperty("PartyXpCutoffPercent", "3."));
					PARTY_XP_CUTOFF_LEVEL = Integer.parseInt(Character.getProperty("PartyXpCutoffLevel", "30"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + CHARACTER_CONFIG_FILE + " file.");
				}
				// Load L2J Server Version Properties file (if exists)
				try
				{
					L2Properties serverVersion = new L2Properties();
					is = new FileInputStream(new File(SERVER_VERSION_FILE));
					serverVersion.load(is);
					SERVER_VERSION = serverVersion.getProperty("version", "Unsupported Custom Version.");
					SERVER_BUILD_DATE = serverVersion.getProperty("builddate", "Undefined Date.");
				}
				catch (Exception e)
				{
					// Ignore Properties file if it doesnt exist
					SERVER_VERSION = "Unsupported Custom Version.";
					SERVER_BUILD_DATE = "Undefined Date.";
				}
				// Load L2J Datapack Version Properties file (if exists)
				try
				{
					L2Properties serverVersion = new L2Properties();
					is = new FileInputStream(new File(DATAPACK_VERSION_FILE));
					serverVersion.load(is);
					DATAPACK_VERSION = serverVersion.getProperty("version", "Unsupported Custom Version.");
				}
				catch (Exception e)
				{
					// Ignore Properties file if it doesnt exist
					DATAPACK_VERSION = "Unsupported Custom Version.";
				}
				// Load Telnet Properties file (if exists)
				try
				{
					L2Properties telnetSettings = new L2Properties();
					is = new FileInputStream(new File(TELNET_FILE));
					telnetSettings.load(is);
					IS_TELNET_ENABLED = Boolean.parseBoolean(telnetSettings.getProperty("EnableTelnet", "false"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + TELNET_FILE + " File.");
				}
				// MMO
				try
				{
					_log.info("Loading " + MMO_CONFIG_FILE.replaceAll("./config/", ""));
					L2Properties mmoSettings = new L2Properties();
					is = new FileInputStream(new File(MMO_CONFIG_FILE));
					mmoSettings.load(is);
					MMO_SELECTOR_SLEEP_TIME = Integer.parseInt(mmoSettings.getProperty("SleepTime", "20"));
					MMO_IO_SELECTOR_THREAD_COUNT = Integer.parseInt(mmoSettings.getProperty("IOSelectorThreadCount", "2"));
					MMO_MAX_SEND_PER_PASS = Integer.parseInt(mmoSettings.getProperty("MaxSendPerPass", "12"));
					MMO_MAX_READ_PER_PASS = Integer.parseInt(mmoSettings.getProperty("MaxReadPerPass", "12"));
					MMO_HELPER_BUFFER_COUNT = Integer.parseInt(mmoSettings.getProperty("HelperBufferCount", "20"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + MMO_CONFIG_FILE + " File.");
				}
				// Load IdFactory Properties file (if exists)
				try
				{
					L2Properties idSettings = new L2Properties();
					is = new FileInputStream(new File(ID_CONFIG_FILE));
					idSettings.load(is);
					MAP_TYPE = ObjectMapType.valueOf(idSettings.getProperty("L2Map", "WorldObjectMap"));
					SET_TYPE = ObjectSetType.valueOf(idSettings.getProperty("L2Set", "WorldObjectSet"));
					IDFACTORY_TYPE = IdFactoryType.valueOf(idSettings.getProperty("IDFactory", "Compaction"));
					BAD_ID_CHECKING = Boolean.parseBoolean(idSettings.getProperty("BadIdChecking", "True"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + ID_CONFIG_FILE + " file.");
				}
				// Load General Properties file (if exists)
				try
				{
					L2Properties General = new L2Properties();
					is = new FileInputStream(new File(GENERAL_CONFIG_FILE));
					General.load(is);
					EVERYBODY_HAS_ADMIN_RIGHTS = Boolean.parseBoolean(General.getProperty("EverybodyHasAdminRights", "false"));
					DISPLAY_SERVER_VERSION = Boolean.parseBoolean(General.getProperty("DisplayServerRevision", "True"));
					SERVER_LIST_BRACKET = Boolean.parseBoolean(General.getProperty("ServerListBrackets", "false"));
					SERVER_LIST_CLOCK = Boolean.parseBoolean(General.getProperty("ServerListClock", "false"));
					SERVER_GMONLY = Boolean.parseBoolean(General.getProperty("ServerGMOnly", "false"));
					GM_HERO_AURA = Boolean.parseBoolean(General.getProperty("GMHeroAura", "False"));
					GM_STARTUP_INVULNERABLE = Boolean.parseBoolean(General.getProperty("GMStartupInvulnerable", "False"));
					GM_STARTUP_INVISIBLE = Boolean.parseBoolean(General.getProperty("GMStartupInvisible", "False"));
					GM_STARTUP_SILENCE = Boolean.parseBoolean(General.getProperty("GMStartupSilence", "False"));
					GM_STARTUP_AUTO_LIST = Boolean.parseBoolean(General.getProperty("GMStartupAutoList", "False"));
					GM_STARTUP_DIET_MODE = Boolean.parseBoolean(General.getProperty("GMStartupDietMode", "False"));
					GM_ADMIN_MENU_STYLE = General.getProperty("GMAdminMenuStyle", "modern");
					GM_ITEM_RESTRICTION = Boolean.parseBoolean(General.getProperty("GMItemRestriction", "True"));
					GM_SKILL_RESTRICTION = Boolean.parseBoolean(General.getProperty("GMSkillRestriction", "True"));
					GM_TRADE_RESTRICTED_ITEMS = Boolean.parseBoolean(General.getProperty("GMTradeRestrictedItems", "False"));
					BYPASS_VALIDATION = Boolean.parseBoolean(General.getProperty("BypassValidation", "True"));
					GAMEGUARD_ENFORCE = Boolean.parseBoolean(General.getProperty("GameGuardEnforce", "False"));
					GAMEGUARD_PROHIBITACTION = Boolean.parseBoolean(General.getProperty("GameGuardProhibitAction", "False"));
					LOG_CHAT = Boolean.parseBoolean(General.getProperty("LogChat", "false"));
					LOG_ITEMS = Boolean.parseBoolean(General.getProperty("LogItems", "false"));
					LOG_ITEM_ENCHANTS = Boolean.parseBoolean(General.getProperty("LogItemEnchants", "false"));
					LOG_SKILL_ENCHANTS = Boolean.parseBoolean(General.getProperty("LogSkillEnchants", "false"));
					GMAUDIT = Boolean.parseBoolean(General.getProperty("GMAudit", "False"));
					LOG_GAME_DAMAGE = Boolean.parseBoolean(General.getProperty("LogGameDamage", "False"));
					DEBUG = Boolean.parseBoolean(General.getProperty("Debug", "false"));
					PACKET_HANDLER_DEBUG = Boolean.parseBoolean(General.getProperty("PacketHandlerDebug", "false"));
					ASSERT = Boolean.parseBoolean(General.getProperty("Assert", "false"));
					DEVELOPER = Boolean.parseBoolean(General.getProperty("Developer", "false"));
					ACCEPT_GEOEDITOR_CONN = Boolean.parseBoolean(General.getProperty("AcceptGeoeditorConn", "false"));
					TEST_SERVER = Boolean.parseBoolean(General.getProperty("TestServer", "false"));
					SERVER_LIST_TESTSERVER = Boolean.parseBoolean(General.getProperty("ListTestServers", "false"));
					ALT_DEV_NO_QUESTS = Boolean.parseBoolean(General.getProperty("AltDevNoQuests", "False"));
					ALT_DEV_NO_SPAWNS = Boolean.parseBoolean(General.getProperty("AltDevNoSpawns", "False"));
					THREAD_P_EFFECTS = Integer.parseInt(General.getProperty("ThreadPoolSizeEffects", "10"));
					THREAD_P_GENERAL = Integer.parseInt(General.getProperty("ThreadPoolSizeGeneral", "13"));
					IO_PACKET_THREAD_CORE_SIZE = Integer.parseInt(General.getProperty("UrgentPacketThreadCoreSize", "2"));
					GENERAL_PACKET_THREAD_CORE_SIZE = Integer.parseInt(General.getProperty("GeneralPacketThreadCoreSize", "4"));
					GENERAL_THREAD_CORE_SIZE = Integer.parseInt(General.getProperty("GeneralThreadCoreSize", "4"));
					AI_MAX_THREAD = Integer.parseInt(General.getProperty("AiMaxThread", "6"));
					DEADLOCK_DETECTOR = Boolean.parseBoolean(General.getProperty("DeadLockDetector", "False"));
					DEADLOCK_CHECK_INTERVAL = Integer.parseInt(General.getProperty("DeadLockCheckInterval", "20"));
					RESTART_ON_DEADLOCK = Boolean.parseBoolean(General.getProperty("RestartOnDeadlock", "False"));
					ALLOW_DISCARDITEM = Boolean.parseBoolean(General.getProperty("AllowDiscardItem", "True"));
					AUTODESTROY_ITEM_AFTER = Integer.parseInt(General.getProperty("AutoDestroyDroppedItemAfter", "600"));
					HERB_AUTO_DESTROY_TIME = Integer.parseInt(General.getProperty("AutoDestroyHerbTime", "15")) * 1000;
					PROTECTED_ITEMS = General.getProperty("ListOfProtectedItems", "0");
					LIST_PROTECTED_ITEMS = new FastList<Integer>();
					for (String id : PROTECTED_ITEMS.split(","))
					{
						LIST_PROTECTED_ITEMS.add(Integer.parseInt(id));
					}
					CHAR_STORE_INTERVAL = Integer.parseInt(General.getProperty("CharacterDataStoreInterval", "15"));
					LAZY_ITEMS_UPDATE = Boolean.parseBoolean(General.getProperty("LazyItemsUpdate", "false"));
					UPDATE_ITEMS_ON_CHAR_STORE = Boolean.parseBoolean(General.getProperty("UpdateItemsOnCharStore", "false"));
					DESTROY_DROPPED_PLAYER_ITEM = Boolean.parseBoolean(General.getProperty("DestroyPlayerDroppedItem", "false"));
					DESTROY_EQUIPABLE_PLAYER_ITEM = Boolean.parseBoolean(General.getProperty("DestroyEquipableItem", "false"));
					SAVE_DROPPED_ITEM = Boolean.parseBoolean(General.getProperty("SaveDroppedItem", "false"));
					EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = Boolean.parseBoolean(General.getProperty("EmptyDroppedItemTableAfterLoad", "false"));
					SAVE_DROPPED_ITEM_INTERVAL = Integer.parseInt(General.getProperty("SaveDroppedItemInterval", "60")) * 60000;
					CLEAR_DROPPED_ITEM_TABLE = Boolean.parseBoolean(General.getProperty("ClearDroppedItemTable", "false"));
					AUTODELETE_INVALID_QUEST_DATA = Boolean.parseBoolean(General.getProperty("AutoDeleteInvalidQuestData", "False"));
					PRECISE_DROP_CALCULATION = Boolean.parseBoolean(General.getProperty("PreciseDropCalculation", "True"));
					MULTIPLE_ITEM_DROP = Boolean.parseBoolean(General.getProperty("MultipleItemDrop", "True"));
					FORCE_INVENTORY_UPDATE = Boolean.parseBoolean(General.getProperty("ForceInventoryUpdate", "False"));
					LAZY_CACHE = Boolean.parseBoolean(General.getProperty("LazyCache", "True"));
					MIN_NPC_ANIMATION = Integer.parseInt(General.getProperty("MinNPCAnimation", "10"));
					MAX_NPC_ANIMATION = Integer.parseInt(General.getProperty("MaxNPCAnimation", "20"));
					MIN_MONSTER_ANIMATION = Integer.parseInt(General.getProperty("MinMonsterAnimation", "5"));
					MAX_MONSTER_ANIMATION = Integer.parseInt(General.getProperty("MaxMonsterAnimation", "20"));
					MOVE_BASED_KNOWNLIST = Boolean.parseBoolean(General.getProperty("MoveBasedKnownlist", "False"));
					KNOWNLIST_UPDATE_INTERVAL = Long.parseLong(General.getProperty("KnownListUpdateInterval", "1250"));
					GRIDS_ALWAYS_ON = Boolean.parseBoolean(General.getProperty("GridsAlwaysOn", "False"));
					GRID_NEIGHBOR_TURNON_TIME = Integer.parseInt(General.getProperty("GridNeighborTurnOnTime", "1"));
					GRID_NEIGHBOR_TURNOFF_TIME = Integer.parseInt(General.getProperty("GridNeighborTurnOffTime", "90"));
					GEODATA_PATH = Paths.get(General.getString("GeoDataPath", "./data/geodata"));
					TRY_LOAD_UNSPECIFIED_REGIONS = General.getBoolean("TryLoadUnspecifiedRegions", true);
					GEODATA_REGIONS = new HashMap<>();
					for (int regionX = L2World.TILE_X_MIN; regionX <= L2World.TILE_X_MAX; regionX++)
					{
						for (int regionY = L2World.TILE_Y_MIN; regionY <= L2World.TILE_Y_MAX; regionY++)
						{
							String key = regionX + "_" + regionY;
							if (General.containsKey(regionX + "_" + regionY))
							{
								GEODATA_REGIONS.put(key, General.getBoolean(key, false));
							}
						}
					}
					GEODATA = Integer.parseInt(General.getProperty("GeoData", "0"));
					GEODATA_CELLFINDING = Boolean.parseBoolean(General.getProperty("CellPathFinding", "False"));
					FORCE_GEODATA = Boolean.parseBoolean(General.getProperty("ForceGeodata", "True"));
					COORD_SYNCHRONIZE = Integer.parseInt(General.getProperty("CoordSynchronize", "-1"));
					ZONE_TOWN = Integer.parseInt(General.getProperty("ZoneTown", "0"));
					ACTIVATE_POSITION_RECORDER = Boolean.parseBoolean(General.getProperty("ActivatePositionRecorder", "False"));
					DEFAULT_GLOBAL_CHAT = General.getProperty("GlobalChat", "ON");
					DEFAULT_TRADE_CHAT = General.getProperty("TradeChat", "LIMITED");
					ALLOW_WAREHOUSE = Boolean.parseBoolean(General.getProperty("AllowWarehouse", "True"));
					WAREHOUSE_CACHE = Boolean.parseBoolean(General.getProperty("WarehouseCache", "False"));
					WAREHOUSE_CACHE_TIME = Integer.parseInt(General.getProperty("WarehouseCacheTime", "15"));
					ALLOW_FREIGHT = Boolean.parseBoolean(General.getProperty("AllowFreight", "True"));
					ALLOW_WEAR = Boolean.parseBoolean(General.getProperty("AllowWear", "False"));
					WEAR_DELAY = Integer.parseInt(General.getProperty("WearDelay", "5"));
					WEAR_PRICE = Integer.parseInt(General.getProperty("WearPrice", "10"));
					ALLOW_LOTTERY = Boolean.parseBoolean(General.getProperty("AllowLottery", "True"));
					ALLOW_RACE = Boolean.parseBoolean(General.getProperty("AllowRace", "True"));
					ALLOW_WATER = Boolean.parseBoolean(General.getProperty("AllowWater", "True"));
					ALLOW_RENTPET = Boolean.parseBoolean(General.getProperty("AllowRentPet", "False"));
					ALLOW_DISCARDITEM = Boolean.parseBoolean(General.getProperty("AllowDiscardItem", "True"));
					ALLOWFISHING = Boolean.parseBoolean(General.getProperty("AllowFishing", "True"));
					ALLOW_MANOR = Boolean.parseBoolean(General.getProperty("AllowManor", "True"));
					ALLOW_BOAT = Boolean.parseBoolean(General.getProperty("AllowBoat", "True"));
					ALLOW_CURSED_WEAPONS = Boolean.parseBoolean(General.getProperty("AllowCursedWeapons", "True"));
					ALLOW_NPC_WALKERS = Boolean.parseBoolean(General.getProperty("AllowNpcWalkers", "true"));
					ALLOW_PET_WALKERS = Boolean.parseBoolean(General.getProperty("AllowPetWalkers", "True"));
					SERVER_NEWS = Boolean.parseBoolean(General.getProperty("ShowServerNews", "False"));
					COMMUNITY_TYPE = Integer.parseInt(General.getProperty("CommunityType", "1"));
					BBS_SHOW_PLAYERLIST = Boolean.parseBoolean(General.getProperty("BBSShowPlayerList", "false"));
					BBS_DEFAULT = General.getProperty("BBSDefault", "_bbshome");
					SHOW_LEVEL_COMMUNITYBOARD = Boolean.parseBoolean(General.getProperty("ShowLevelOnCommunityBoard", "False"));
					SHOW_STATUS_COMMUNITYBOARD = Boolean.parseBoolean(General.getProperty("ShowStatusOnCommunityBoard", "True"));
					NAME_PAGE_SIZE_COMMUNITYBOARD = Integer.parseInt(General.getProperty("NamePageSizeOnCommunityBoard", "50"));
					NAME_PER_ROW_COMMUNITYBOARD = Integer.parseInt(General.getProperty("NamePerRowOnCommunityBoard", "5"));
					ALT_OLY_START_TIME = Integer.parseInt(General.getProperty("AltOlyStartTime", "18"));
					ALT_OLY_MIN = Integer.parseInt(General.getProperty("AltOlyMin", "00"));
					ALT_OLY_CPERIOD = Long.parseLong(General.getProperty("AltOlyCPeriod", "21600000"));
					ALT_OLY_BATTLE = Long.parseLong(General.getProperty("AltOlyBattle", "360000"));
					ALT_OLY_WPERIOD = Long.parseLong(General.getProperty("AltOlyWPeriod", "604800000"));
					ALT_OLY_VPERIOD = Long.parseLong(General.getProperty("AltOlyVPeriod", "86400000"));
					ALT_OLY_CLASSED = Integer.parseInt(General.getProperty("AltOlyClassedParticipants", "5"));
					ALT_OLY_NONCLASSED = Integer.parseInt(General.getProperty("AltOlyNonClassedParticipants", "9"));
					ALT_OLY_REG_DISPLAY = Integer.parseInt(General.getProperty("AltOlyRegistrationDisplayNumber", "100"));
					ALT_OLY_BATTLE_REWARD_ITEM = Integer.parseInt(General.getProperty("AltOlyBattleRewItem", "6651"));
					ALT_OLY_CLASSED_RITEM_C = Integer.parseInt(General.getProperty("AltOlyClassedRewItemCount", "50"));
					ALT_OLY_NONCLASSED_RITEM_C = Integer.parseInt(General.getProperty("AltOlyNonClassedRewItemCount", "30"));
					ALT_OLY_COMP_RITEM = Integer.parseInt(General.getProperty("AltOlyCompRewItem", "13722"));
					ALT_OLY_GP_PER_POINT = Integer.parseInt(General.getProperty("AltOlyGPPerPoint", "1000"));
					ALT_OLY_HERO_POINTS = Integer.parseInt(General.getProperty("AltOlyHeroPoints", "180"));
					ALT_OLY_RANK1_POINTS = Integer.parseInt(General.getProperty("AltOlyRank1Points", "120"));
					ALT_OLY_RANK2_POINTS = Integer.parseInt(General.getProperty("AltOlyRank2Points", "80"));
					ALT_OLY_RANK3_POINTS = Integer.parseInt(General.getProperty("AltOlyRank3Points", "55"));
					ALT_OLY_RANK4_POINTS = Integer.parseInt(General.getProperty("AltOlyRank4Points", "35"));
					ALT_OLY_RANK5_POINTS = Integer.parseInt(General.getProperty("AltOlyRank5Points", "20"));
					ALT_OLY_MAX_POINTS = Integer.parseInt(General.getProperty("AltOlyMaxPoints", "10"));
					ALT_OLY_LOG_FIGHTS = Boolean.parseBoolean(General.getProperty("AlyOlyLogFights", "false"));
					ALT_OLY_SHOW_MONTHLY_WINNERS = Boolean.parseBoolean(General.getProperty("AltOlyShowMonthlyWinners", "true"));
					ALT_OLY_ANNOUNCE_GAMES = Boolean.parseBoolean(General.getProperty("AltOlyAnnounceGames", "true"));
					LIST_OLY_RESTRICTED_ITEMS = new FastList<Integer>();
					for (String id : General.getProperty("AltOlyRestrictedItems", "0").split(","))
					{
						LIST_OLY_RESTRICTED_ITEMS.add(Integer.parseInt(id));
					}
					ALT_OLY_ENCHANT_LIMIT = Integer.parseInt(General.getProperty("AltOlyEnchantLimit", "-1"));
					ALT_MANOR_REFRESH_TIME = Integer.parseInt(General.getProperty("AltManorRefreshTime", "20"));
					ALT_MANOR_REFRESH_MIN = Integer.parseInt(General.getProperty("AltManorRefreshMin", "00"));
					ALT_MANOR_APPROVE_TIME = Integer.parseInt(General.getProperty("AltManorApproveTime", "6"));
					ALT_MANOR_APPROVE_MIN = Integer.parseInt(General.getProperty("AltManorApproveMin", "00"));
					ALT_MANOR_MAINTENANCE_PERIOD = Integer.parseInt(General.getProperty("AltManorMaintenancePeriod", "360000"));
					ALT_MANOR_SAVE_ALL_ACTIONS = Boolean.parseBoolean(General.getProperty("AltManorSaveAllActions", "false"));
					ALT_MANOR_SAVE_PERIOD_RATE = Integer.parseInt(General.getProperty("AltManorSavePeriodRate", "2"));
					ALT_LOTTERY_PRIZE = Long.parseLong(General.getProperty("AltLotteryPrize", "50000"));
					ALT_LOTTERY_TICKET_PRICE = Long.parseLong(General.getProperty("AltLotteryTicketPrice", "2000"));
					ALT_LOTTERY_5_NUMBER_RATE = Float.parseFloat(General.getProperty("AltLottery5NumberRate", "0.6"));
					ALT_LOTTERY_4_NUMBER_RATE = Float.parseFloat(General.getProperty("AltLottery4NumberRate", "0.2"));
					ALT_LOTTERY_3_NUMBER_RATE = Float.parseFloat(General.getProperty("AltLottery3NumberRate", "0.2"));
					ALT_LOTTERY_2_AND_1_NUMBER_PRIZE = Long.parseLong(General.getProperty("AltLottery2and1NumberPrize", "200"));
					FS_TIME_ATTACK = Integer.parseInt(General.getProperty("TimeOfAttack", "50"));
					FS_TIME_COOLDOWN = Integer.parseInt(General.getProperty("TimeOfCoolDown", "5"));
					FS_TIME_ENTRY = Integer.parseInt(General.getProperty("TimeOfEntry", "3"));
					FS_TIME_WARMUP = Integer.parseInt(General.getProperty("TimeOfWarmUp", "2"));
					FS_PARTY_MEMBER_COUNT = Integer.parseInt(General.getProperty("NumberOfNecessaryPartyMembers", "4"));
					if (FS_TIME_ATTACK <= 0)
						FS_TIME_ATTACK = 50;
					if (FS_TIME_COOLDOWN <= 0)
						FS_TIME_COOLDOWN = 5;
					if (FS_TIME_ENTRY <= 0)
						FS_TIME_ENTRY = 3;
					if (FS_TIME_ENTRY <= 0)
						FS_TIME_ENTRY = 3;
					if (FS_TIME_ENTRY <= 0)
						FS_TIME_ENTRY = 3;
					RIFT_MIN_PARTY_SIZE = Integer.parseInt(General.getProperty("RiftMinPartySize", "5"));
					RIFT_MAX_JUMPS = Integer.parseInt(General.getProperty("MaxRiftJumps", "4"));
					RIFT_SPAWN_DELAY = Integer.parseInt(General.getProperty("RiftSpawnDelay", "10000"));
					RIFT_AUTO_JUMPS_TIME_MIN = Integer.parseInt(General.getProperty("AutoJumpsDelayMin", "480"));
					RIFT_AUTO_JUMPS_TIME_MAX = Integer.parseInt(General.getProperty("AutoJumpsDelayMax", "600"));
					RIFT_BOSS_ROOM_TIME_MUTIPLY = Float.parseFloat(General.getProperty("BossRoomTimeMultiply", "1.5"));
					RIFT_ENTER_COST_RECRUIT = Integer.parseInt(General.getProperty("RecruitCost", "18"));
					RIFT_ENTER_COST_SOLDIER = Integer.parseInt(General.getProperty("SoldierCost", "21"));
					RIFT_ENTER_COST_OFFICER = Integer.parseInt(General.getProperty("OfficerCost", "24"));
					RIFT_ENTER_COST_CAPTAIN = Integer.parseInt(General.getProperty("CaptainCost", "27"));
					RIFT_ENTER_COST_COMMANDER = Integer.parseInt(General.getProperty("CommanderCost", "30"));
					RIFT_ENTER_COST_HERO = Integer.parseInt(General.getProperty("HeroCost", "33"));
					DEFAULT_PUNISH = Integer.parseInt(General.getProperty("DefaultPunish", "2"));
					DEFAULT_PUNISH_PARAM = Integer.parseInt(General.getProperty("DefaultPunishParam", "0"));
					ONLY_GM_ITEMS_FREE = Boolean.parseBoolean(General.getProperty("OnlyGMItemsFree", "True"));
					JAIL_IS_PVP = Boolean.parseBoolean(General.getProperty("JailIsPvp", "True"));
					JAIL_DISABLE_CHAT = Boolean.parseBoolean(General.getProperty("JailDisableChat", "True"));
					CUSTOM_SPAWNLIST_TABLE = Boolean.valueOf(General.getProperty("CustomSpawnlistTable", "false"));
					SAVE_GMSPAWN_ON_CUSTOM = Boolean.valueOf(General.getProperty("SaveGmSpawnOnCustom", "false"));
					DELETE_GMSPAWN_ON_CUSTOM = Boolean.valueOf(General.getProperty("DeleteGmSpawnOnCustom", "false"));
					CUSTOM_NPC_TABLE = Boolean.valueOf(General.getProperty("CustomNpcTable", "false"));
					CUSTOM_ITEM_TABLES = Boolean.valueOf(General.getProperty("CustomItemTables", "false"));
					CUSTOM_ARMORSETS_TABLE = Boolean.valueOf(General.getProperty("CustomArmorSetsTable", "false"));
					CUSTOM_TELEPORT_TABLE = Boolean.valueOf(General.getProperty("CustomTeleportTable", "false"));
					CUSTOM_DROPLIST_TABLE = Boolean.valueOf(General.getProperty("CustomDroplistTable", "false"));
					CUSTOM_MERCHANT_TABLES = Boolean.valueOf(General.getProperty("CustomMerchantTables", "false"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + GENERAL_CONFIG_FILE + " File.");
				}
				// Load FloodProtector Properties file
				try
				{
					L2Properties security = new L2Properties();
					is = new FileInputStream(new File(FLOOD_PROTECTOR_FILE));
					security.load(is);
					loadFloodProtectorConfigs(security);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + FLOOD_PROTECTOR_FILE);
				}
				// Load NPC Properties file (if exists)
				try
				{
					L2Properties NPC = new L2Properties();
					is = new FileInputStream(new File(NPC_CONFIG_FILE));
					NPC.load(is);
					ANNOUNCE_MAMMON_SPAWN = Boolean.parseBoolean(NPC.getProperty("AnnounceMammonSpawn", "False"));
					ALT_MOB_AGRO_IN_PEACEZONE = Boolean.parseBoolean(NPC.getProperty("AltMobAgroInPeaceZone", "True"));
					ALT_ATTACKABLE_NPCS = Boolean.parseBoolean(NPC.getProperty("AltAttackableNpcs", "True"));
					ALT_GAME_VIEWNPC = Boolean.parseBoolean(NPC.getProperty("AltGameViewNpc", "False"));
					MAX_DRIFT_RANGE = Integer.parseInt(NPC.getProperty("MaxDriftRange", "300"));
					DEEPBLUE_DROP_RULES = Boolean.parseBoolean(NPC.getProperty("UseDeepBlueDropRules", "True"));
					DEEPBLUE_DROP_RULES_RAID = Boolean.parseBoolean(NPC.getProperty("UseDeepBlueDropRulesRaid", "True"));
					SHOW_NPC_LVL = Boolean.parseBoolean(NPC.getProperty("ShowNpcLevel", "False"));
					ENABLE_DROP_VITALITY_HERBS = Boolean.parseBoolean(NPC.getProperty("EnableVitalityHerbs", "False"));
					GUARD_ATTACK_AGGRO_MOB = Boolean.parseBoolean(NPC.getProperty("GuardAttackAggroMob", "False"));
					ALLOW_WYVERN_UPGRADER = Boolean.parseBoolean(NPC.getProperty("AllowWyvernUpgrader", "False"));
					PET_RENT_NPC = NPC.getProperty("ListPetRentNpc", "30827");
					LIST_PET_RENT_NPC = new FastList<Integer>();
					for (String id : PET_RENT_NPC.split(","))
					{
						LIST_PET_RENT_NPC.add(Integer.parseInt(id));
					}
					RAID_HP_REGEN_MULTIPLIER = Double.parseDouble(NPC.getProperty("RaidHpRegenMultiplier", "100")) / 100;
					RAID_MP_REGEN_MULTIPLIER = Double.parseDouble(NPC.getProperty("RaidMpRegenMultiplier", "100")) / 100;
					RAID_PDEFENCE_MULTIPLIER = Double.parseDouble(NPC.getProperty("RaidPDefenceMultiplier", "100")) / 100;
					RAID_MDEFENCE_MULTIPLIER = Double.parseDouble(NPC.getProperty("RaidMDefenceMultiplier", "100")) / 100;
					RAID_MIN_RESPAWN_MULTIPLIER = Float.parseFloat(NPC.getProperty("RaidMinRespawnMultiplier", "1.0"));
					RAID_MAX_RESPAWN_MULTIPLIER = Float.parseFloat(NPC.getProperty("RaidMaxRespawnMultiplier", "1.0"));
					RAID_MINION_RESPAWN_TIMER = Integer.parseInt(NPC.getProperty("RaidMinionRespawnTime", "300000"));
					RAID_DISABLE_CURSE = Boolean.parseBoolean(NPC.getProperty("DisableRaidCurse", "False"));
					INVENTORY_MAXIMUM_PET = Integer.parseInt(NPC.getProperty("MaximumSlotsForPet", "12"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + NPC_CONFIG_FILE + " File.");
				}
				try
				{
					L2Properties Luna = new L2Properties();
					is = new FileInputStream(new File(LUNA));
					Luna.load(is);
					BALANCE_SKILL_LIST = new FastMap<Integer, Double>();
					String[] propertySplit;
					propertySplit = Luna.getProperty("Balance_Skill_List", "").split(";");
					for (String skillId : propertySplit)
					{
						String[] skillSplit = skillId.split(",");
						if (skillSplit.length != 2)
							_log.warning(StringUtil.concat("[SkillBalanceList]: invalid config property -> SkillBalanceList \"", skillId, "\""));
						else
						{
							try
							{
								BALANCE_SKILL_LIST.put(Integer.parseInt(skillSplit[0]), Double.parseDouble(skillSplit[1]));
							}
							catch (NumberFormatException nfe)
							{
								if (!skillId.isEmpty())
								{
									_log.warning(StringUtil.concat("[SkillBalanceList]: invalid config property -> SkillList \"", skillSplit[0], "\"", skillSplit[1]));
								}
							}
						}
					}
					KAMALOKA_DROPS_TO_FULL_PARTY_ONLY = Boolean.parseBoolean(Luna.getProperty("Kamaloka_drop_to_full_party_only", "True"));
					PVP_CLASS_BALANCE_DUELIST = Double.parseDouble(Luna.getProperty("pvp_duelist", "1"));
					PVP_CLASS_BALANCE_DREADNOUGHT = Double.parseDouble(Luna.getProperty("pvp_dreadnought", "1"));
					PVP_CLASS_BALANCE_PHOENIX_KNIGHT = Double.parseDouble(Luna.getProperty("pvp_phoenix_knight", "1"));
					PVP_CLASS_BALANCE_HELL_KNIGHT = Double.parseDouble(Luna.getProperty("pvp_hell_knight", "1"));
					PVP_CLASS_BALANCE_SAGITTARIUS = Double.parseDouble(Luna.getProperty("pvp_sagittarius", "1"));
					PVP_CLASS_BALANCE_ADVENTURER = Double.parseDouble(Luna.getProperty("pvp_adventurer", "1"));
					PVP_CLASS_BALANCE_ARCHMAGE = Double.parseDouble(Luna.getProperty("pvp_archmage", "1"));
					PVP_CLASS_BALANCE_SOULTAKER = Double.parseDouble(Luna.getProperty("pvp_soultaker", "1"));
					PVP_CLASS_BALANCE_ARCANA_LORD = Double.parseDouble(Luna.getProperty("pvp_arcana_lord", "1"));
					PVP_CLASS_BALANCE_CARDINAL = Double.parseDouble(Luna.getProperty("pvp_cardinal", "1"));
					PVP_CLASS_BALANCE_HIEROPHANT = Double.parseDouble(Luna.getProperty("pvp_hierophant", "1"));
					PVP_CLASS_BALANCE_EVA_TEMPLAR = Double.parseDouble(Luna.getProperty("pvp_eva_templar", "1"));
					PVP_CLASS_BALANCE_SWORD_MUSE = Double.parseDouble(Luna.getProperty("pvp_sword_muse", "1"));
					PVP_CLASS_BALANCE_WIND_RIDER = Double.parseDouble(Luna.getProperty("pvp_wind_rider", "1"));
					PVP_CLASS_BALANCE_MOONLIGHT_SENTINEL = Double.parseDouble(Luna.getProperty("pvp_moonlight_sentinel", "1"));
					PVP_CLASS_BALANCE_MYSTIC_MUSE = Double.parseDouble(Luna.getProperty("pvp_mystic_muse", "1"));
					PVP_CLASS_BALANCE_ELEMENTAL_MASTER = Double.parseDouble(Luna.getProperty("pvp_elemental_master", "1"));
					PVP_CLASS_BALANCE_EVA_SAINT = Double.parseDouble(Luna.getProperty("pvp_eva_saint", "1"));
					PVP_CLASS_BALANCE_SHILLIEN_TEMPLAR = Double.parseDouble(Luna.getProperty("pvp_shillien_templar", "1"));
					PVP_CLASS_BALANCE_SPECTRAL_DANCER = Double.parseDouble(Luna.getProperty("pvp_spectral_dancer", "1"));
					PVP_CLASS_BALANCE_GHOST_HUNTER = Double.parseDouble(Luna.getProperty("pvp_ghost_hunter", "1"));
					PVP_CLASS_BALANCE_GHOST_SENTINEL = Double.parseDouble(Luna.getProperty("pvp_ghost_sentinel", "1"));
					PVP_CLASS_BALANCE_STORM_SCREAMER = Double.parseDouble(Luna.getProperty("pvp_storm_screamer", "1"));
					PVP_CLASS_BALANCE_SPECTRAL_MASTER = Double.parseDouble(Luna.getProperty("pvp_spectral_master", "1"));
					PVP_CLASS_BALANCE_SHILLIEN_SAINT = Double.parseDouble(Luna.getProperty("pvp_shillien_saint", "1"));
					PVP_CLASS_BALANCE_TITAN = Double.parseDouble(Luna.getProperty("pvp_titan", "1"));
					PVP_CLASS_BALANCE_GRAND_KHAUATARI = Double.parseDouble(Luna.getProperty("pvp_grand_khauatari", "1"));
					PVP_CLASS_BALANCE_DOMINATOR = Double.parseDouble(Luna.getProperty("pvp_dominator", "1"));
					PVP_CLASS_BALANCE_DOOMCRYER = Double.parseDouble(Luna.getProperty("pvp_doomcryer", "1"));
					PVP_CLASS_BALANCE_FORTUNE_SEEKER = Double.parseDouble(Luna.getProperty("pvp_fortune_seeker", "1"));
					PVP_CLASS_BALANCE_MAESTRO = Double.parseDouble(Luna.getProperty("pvp_maestro", "1"));
					PVP_CLASS_BALANCE_DOOMBRINGER = Double.parseDouble(Luna.getProperty("pvp_doombringer", "1"));
					PVP_CLASS_BALANCE_MALE_SOULHOUND = Double.parseDouble(Luna.getProperty("pvp_male_soulhound", "1"));
					PVP_CLASS_BALANCE_FEMALE_SOULHOUND = Double.parseDouble(Luna.getProperty("pvp_female_soulhound", "1"));
					PVP_CLASS_BALANCE_TRICKSTER = Double.parseDouble(Luna.getProperty("pvp_trickster", "1"));
					PVP_CLASS_BALANCE_INSPECTOR = Double.parseDouble(Luna.getProperty("pvp_inspector", "1"));
					PVP_CLASS_BALANCE_JUDICATOR = Double.parseDouble(Luna.getProperty("pvp_judicator", "1"));
					PVE_CLASS_BALANCE_DUELIST = Double.parseDouble(Luna.getProperty("pve_duelist", "1"));
					PVE_CLASS_BALANCE_DREADNOUGHT = Double.parseDouble(Luna.getProperty("pve_dreadnought", "1"));
					PVE_CLASS_BALANCE_PHOENIX_KNIGHT = Double.parseDouble(Luna.getProperty("pve_phoenix_knight", "1"));
					PVE_CLASS_BALANCE_HELL_KNIGHT = Double.parseDouble(Luna.getProperty("pve_hell_knight", "1"));
					PVE_CLASS_BALANCE_SAGITTARIUS = Double.parseDouble(Luna.getProperty("pve_sagittarius", "1"));
					PVE_CLASS_BALANCE_ADVENTURER = Double.parseDouble(Luna.getProperty("pve_adventurer", "1"));
					PVE_CLASS_BALANCE_ARCHMAGE = Double.parseDouble(Luna.getProperty("pve_archmage", "1"));
					PVE_CLASS_BALANCE_SOULTAKER = Double.parseDouble(Luna.getProperty("pve_soultaker", "1"));
					PVE_CLASS_BALANCE_ARCANA_LORD = Double.parseDouble(Luna.getProperty("pve_arcana_lord", "1"));
					PVE_CLASS_BALANCE_CARDINAL = Double.parseDouble(Luna.getProperty("pve_cardinal", "1"));
					PVE_CLASS_BALANCE_HIEROPHANT = Double.parseDouble(Luna.getProperty("pve_hierophant", "1"));
					PVE_CLASS_BALANCE_EVA_TEMPLAR = Double.parseDouble(Luna.getProperty("pve_eva_templar", "1"));
					PVE_CLASS_BALANCE_SWORD_MUSE = Double.parseDouble(Luna.getProperty("pve_sword_muse", "1"));
					PVE_CLASS_BALANCE_WIND_RIDER = Double.parseDouble(Luna.getProperty("pve_wind_rider", "1"));
					PVE_CLASS_BALANCE_MOONLIGHT_SENTINEL = Double.parseDouble(Luna.getProperty("pve_moonlight_sentinel", "1"));
					PVE_CLASS_BALANCE_MYSTIC_MUSE = Double.parseDouble(Luna.getProperty("pve_mystic_muse", "1"));
					PVE_CLASS_BALANCE_ELEMENTAL_MASTER = Double.parseDouble(Luna.getProperty("pve_elemental_master", "1"));
					PVE_CLASS_BALANCE_EVA_SAINT = Double.parseDouble(Luna.getProperty("pve_eva_saint", "1"));
					PVE_CLASS_BALANCE_SHILLIEN_TEMPLAR = Double.parseDouble(Luna.getProperty("pve_shillien_templar", "1"));
					PVE_CLASS_BALANCE_SPECTRAL_DANCER = Double.parseDouble(Luna.getProperty("pve_spectral_dancer", "1"));
					PVE_CLASS_BALANCE_GHOST_HUNTER = Double.parseDouble(Luna.getProperty("pve_ghost_hunter", "1"));
					PVE_CLASS_BALANCE_GHOST_SENTINEL = Double.parseDouble(Luna.getProperty("pve_ghost_sentinel", "1"));
					PVE_CLASS_BALANCE_STORM_SCREAMER = Double.parseDouble(Luna.getProperty("pve_storm_screamer", "1"));
					PVE_CLASS_BALANCE_SPECTRAL_MASTER = Double.parseDouble(Luna.getProperty("pve_spectral_master", "1"));
					PVE_CLASS_BALANCE_SHILLIEN_SAINT = Double.parseDouble(Luna.getProperty("pve_shillien_saint", "1"));
					PVE_CLASS_BALANCE_TITAN = Double.parseDouble(Luna.getProperty("pve_titan", "1"));
					PVE_CLASS_BALANCE_GRAND_KHAUATARI = Double.parseDouble(Luna.getProperty("pve_grand_khauatari", "1"));
					PVE_CLASS_BALANCE_DOMINATOR = Double.parseDouble(Luna.getProperty("pve_dominator", "1"));
					PVE_CLASS_BALANCE_DOOMCRYER = Double.parseDouble(Luna.getProperty("pve_doomcryer", "1"));
					PVE_CLASS_BALANCE_FORTUNE_SEEKER = Double.parseDouble(Luna.getProperty("pve_fortune_seeker", "1"));
					PVE_CLASS_BALANCE_MAESTRO = Double.parseDouble(Luna.getProperty("pve_maestro", "1"));
					PVE_CLASS_BALANCE_DOOMBRINGER = Double.parseDouble(Luna.getProperty("pve_doombringer", "1"));
					PVE_CLASS_BALANCE_MALE_SOULHOUND = Double.parseDouble(Luna.getProperty("pve_male_soulhound", "1"));
					PVE_CLASS_BALANCE_FEMALE_SOULHOUND = Double.parseDouble(Luna.getProperty("pve_female_soulhound", "1"));
					PVE_CLASS_BALANCE_TRICKSTER = Double.parseDouble(Luna.getProperty("pve_trickster", "1"));
					PVE_CLASS_BALANCE_INSPECTOR = Double.parseDouble(Luna.getProperty("pve_inspector", "1"));
					PVE_CLASS_BALANCE_JUDICATOR = Double.parseDouble(Luna.getProperty("pve_judicator", "1"));
					// CAPTCHA ANTIBOT
					BOTS_PREVENTION = Boolean.parseBoolean(Luna.getProperty("EnableBotsPrevention", "false"));
					KILLS_COUNTER = Integer.parseInt(Luna.getProperty("KillsCounter", "60"));
					KILLS_COUNTER_RANDOMIZATION = Integer.parseInt(Luna.getProperty("KillsCounterRandomization", "50"));
					VALIDATION_TIME = Integer.parseInt(Luna.getProperty("ValidationTime", "60"));
					PUNISHMENT = Integer.parseInt(Luna.getProperty("Punishment", "0"));
					PUNISHMENT_TIME = Integer.parseInt(Luna.getProperty("PunishmentTime", "60"));
					PUNISHMENT_TIME_BONUS_1 = Integer.parseInt(Luna.getProperty("PunishmentTimeBonus1", "15"));
					PUNISHMENT_TIME_BONUS_2 = Integer.parseInt(Luna.getProperty("PunishmentTimeBonus2", "20"));
					PUNISHMENT_TIME_BONUS_3 = Integer.parseInt(Luna.getProperty("PunishmentTimeBonus3", "25"));
					PUNISHMENT_TIME_BONUS_4 = Integer.parseInt(Luna.getProperty("PunishmentTimeBonus4", "30"));
					PUNISHMENT_TIME_BONUS_5 = Integer.parseInt(Luna.getProperty("PunishmentTimeBonus5", "45"));
					PUNISHMENT_TIME_BONUS_6 = Integer.parseInt(Luna.getProperty("PunishmentTimeBonus6", "125"));
					PUNISHMENT_REPORTS1 = Integer.parseInt(Luna.getProperty("PunishmentReports1", "3"));
					PUNISHMENT_REPORTS2 = Integer.parseInt(Luna.getProperty("PunishmentReports2", "5"));
					PUNISHMENT_REPORTS3 = Integer.parseInt(Luna.getProperty("PunishmentReports3", "7"));
					PUNISHMENT_REPORTS4 = Integer.parseInt(Luna.getProperty("PunishmentReports4", "9"));
					PUNISHMENT_REPORTS5 = Integer.parseInt(Luna.getProperty("PunishmentReports5", "10"));
					PUNISHMENT_REPORTS6 = Integer.parseInt(Luna.getProperty("PunishmentReports6", "15"));
					ESCAPE_PUNISHMENT_REPORTS_COUNT = Integer.parseInt(Luna.getProperty("EscapeReports", "1"));
					KICK_PUNISHMENT_REPORTS_COUNT = Integer.parseInt(Luna.getProperty("KickReports", "2"));
					JAIL_PUNISHMENT_REPORTS_COUNT = Integer.parseInt(Luna.getProperty("JailReports", "3"));
					titanium_default_bow_default = Double.parseDouble(Luna.getProperty("titanium_default_bow_default", "0.0"));
					titanium_default_cross_default = Double.parseDouble(Luna.getProperty("titanium_default_cross_default", "0.0"));
					titanium_default_bigb_default = Double.parseDouble(Luna.getProperty("titanium_default_bigb_default", "0.0"));
					titanium_default_dual_default = Double.parseDouble(Luna.getProperty("titanium_default_dual_default", "0.0"));
					titanium_default_val = Double.parseDouble(Luna.getProperty("titanium_default_val", "0.0"));
					titanium_over_bow_default = Double.parseDouble(Luna.getProperty("titanium_over_bow_default", "0.0"));
					titanium_over_cross_default = Double.parseDouble(Luna.getProperty("titanium_over_cross_default", "0.0"));
					titanium_over_bigb_default = Double.parseDouble(Luna.getProperty("titanium_over_bigb_default", "0.0"));
					titanium_over_dual_default = Double.parseDouble(Luna.getProperty("titanium_over_dual_default", "0.0"));
					titanium_over_default = Double.parseDouble(Luna.getProperty("titanium_over_default", "0.0"));
					titanium_super_bow_default = Double.parseDouble(Luna.getProperty("titanium_super_bow_default", "0.0"));
					titanium_super_cross_default = Double.parseDouble(Luna.getProperty("titanium_super_cross_default", "0.0"));
					titanium_super_bigb_default = Double.parseDouble(Luna.getProperty("titanium_super_bigb_default", "0.0"));
					titanium_super_dual_default = Double.parseDouble(Luna.getProperty("titanium_super_dual_default", "0.0"));
					titanium_super_default = Double.parseDouble(Luna.getProperty("titanium_super_default", "0.0"));
					dread_default_bow_default = Double.parseDouble(Luna.getProperty("dread_default_bow_default", "0.0"));
					dread_default_cross_default = Double.parseDouble(Luna.getProperty("dread_default_cross_default", "0.0"));
					dread_default_bigb_default = Double.parseDouble(Luna.getProperty("dread_default_bigb_default", "0.0"));
					dread_default_dual_default = Double.parseDouble(Luna.getProperty("dread_default_dual_default", "0.0"));
					dread_default_val = Double.parseDouble(Luna.getProperty("dread_default_val", "0.0"));
					dread_over_bow_default = Double.parseDouble(Luna.getProperty("dread_over_bow_default", "0.0"));
					dread_over_cross_default = Double.parseDouble(Luna.getProperty("dread_over_cross_default", "0.0"));
					dread_over_bigb_default = Double.parseDouble(Luna.getProperty("dread_over_bigb_default", "0.0"));
					dread_over_dual_default = Double.parseDouble(Luna.getProperty("dread_over_dual_default", "0.0"));
					dread_over_default = Double.parseDouble(Luna.getProperty("dread_over_default", "0.0"));
					dread_super_bow_default = Double.parseDouble(Luna.getProperty("dread_super_bow_default", "0.0"));
					dread_super_cross_default = Double.parseDouble(Luna.getProperty("dread_super_cross_default", "0.0"));
					dread_super_bigb_default = Double.parseDouble(Luna.getProperty("dread_super_bigb_default", "0.0"));
					dread_super_dual_default = Double.parseDouble(Luna.getProperty("dread_super_dual_default", "0.0"));
					dread_super_default = Double.parseDouble(Luna.getProperty("dread_super_default", "0.0"));
					corrupted_default_bow_default = Double.parseDouble(Luna.getProperty("corrupted_default_bow_default", "0.0"));
					corrupted_default_cross_default = Double.parseDouble(Luna.getProperty("corrupted_default_cross_default", "0.0"));
					corrupted_default_bigb_default = Double.parseDouble(Luna.getProperty("corrupted_default_bigb_default", "0.0"));
					corrupted_default_dual_default = Double.parseDouble(Luna.getProperty("corrupted_default_dual_default", "0.0"));
					corrupted_default_val = Double.parseDouble(Luna.getProperty("corrupted_default_val", "0.0"));
					corrupted_over_bow_default = Double.parseDouble(Luna.getProperty("corrupted_over_bow_default", "0.0"));
					corrupted_over_cross_default = Double.parseDouble(Luna.getProperty("corrupted_over_cross_default", "0.0"));
					corrupted_over_bigb_default = Double.parseDouble(Luna.getProperty("corrupted_over_bigb_default", "0.0"));
					corrupted_over_dual_default = Double.parseDouble(Luna.getProperty("corrupted_over_dual_default", "0.0"));
					corrupted_over_default = Double.parseDouble(Luna.getProperty("corrupted_over_default", "0.0"));
					corrupted_super_bow_default = Double.parseDouble(Luna.getProperty("corrupted_super_bow_default", "0.0"));
					corrupted_super_cross_default = Double.parseDouble(Luna.getProperty("corrupted_super_cross_default", "0.0"));
					corrupted_super_bigb_default = Double.parseDouble(Luna.getProperty("corrupted_super_bigb_default", "0.0"));
					corrupted_super_dual_default = Double.parseDouble(Luna.getProperty("corrupted_super_dual_default", "0.0"));
					corrupted_super_default = Double.parseDouble(Luna.getProperty("corrupted_super_default", "0.0"));
					ENCHANT_BONUS_TIER_2 = Integer.parseInt(Luna.getProperty("EnchantBonus_Tier_2", "20"));
					ENCHANT_BONUS_TIER_2_5 = Integer.parseInt(Luna.getProperty("EnchantBonus_Tier_2_5", "17"));
					ENCHANT_BONUS_TIER_3 = Integer.parseInt(Luna.getProperty("EnchantBonus_Tier_3", "16"));
					ENCHANT_BONUS_TIER_4 = Integer.parseInt(Luna.getProperty("EnchantBonus_Tier_4", "11"));
					ENCHANT_BONUS_TIER_4_5 = Integer.parseInt(Luna.getProperty("EnchantBonus_Tier_4_5", "8"));
					ENCHANT_CLUTCH_TIER_0 = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_0", "20"));
					ENCHANT_CLUTCH_TIER_1 = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_1", "20"));
					ENCHANT_CLUTCH_TIER_1_5 = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_1_5", "20"));
					ENCHANT_CLUTCH_TIER_2 = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_2", "20"));
					ENCHANT_CLUTCH_TIER_2_5 = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_2_5", "17"));
					ENCHANT_CLUTCH_TIER_3_DYNASTY = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_3_Dynasty", "18"));
					ENCHANT_CLUTCH_TIER_3_VESPER = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_3_Vesper", "14"));
					ENCHANT_CLUTCH_TIER_3_VESPER_JEWS = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_3_Vesper_Jews", "15"));
					ENCHANT_CLUTCH_TIER_3_DEFAULT = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_3_Default", "18"));
					ENCHANT_CLUTCH_TIER_3_5 = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_3_5", "14"));
					ENCHANT_CLUTCH_TIER_4 = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_4", "12"));
					ENCHANT_CLUTCH_TIER_4_5 = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_4_5", "8"));
					ENCHANT_CLUTCH_TIER_5 = Integer.parseInt(Luna.getProperty("EnchantClutch_Tier_5", "4"));
					SUMMON_PATK_MODIFIER = Integer.parseInt(Luna.getProperty("SummonPAtkModifier", "1"));
					SUMMON_MATK_MODIFIER = Integer.parseInt(Luna.getProperty("SummonMAtkModifier", "1"));
					MAIL_ENABLED = Boolean.parseBoolean(Luna.getProperty("EnableEmail", "false"));
					MAIL_USER = Luna.getProperty("MailUsername", "");
					MAIL_PASSWORD = Luna.getProperty("MailPassowrd", "");
					DONATE_MAIL_USER = Luna.getProperty("DonateMailUsername", "");
					DONATE_MAIL_PASSWORD = Luna.getProperty("DonateMailPassowrd", "");
					HWID_FARMZONES_CHECK = Boolean.parseBoolean(Luna.getProperty("hwidfarmzone_check", "true"));
					HWID_EVENTS_CHECK = Boolean.parseBoolean(Luna.getProperty("hwidevents_check", "true"));
					HWID_EVENTZONES_CHECK = Boolean.parseBoolean(Luna.getProperty("hwideventzone_check", "true"));
					HWID_FARMWHILEEVENT_CHECK = Boolean.parseBoolean(Luna.getProperty("hwidfarmwhileevent_check", "true"));
					EVENTS_LIMIT_IPS = Boolean.parseBoolean(Luna.getProperty("events_limit_ips_enable", "true"));
					EVENTS_LIMIT_IPS_NUM = Integer.parseInt(Luna.getProperty("events_max_ips", "3"));
					ENABLE_OLD_NIGHT = Boolean.parseBoolean(Luna.getProperty("enable_old_night", "true"));
					ENABLE_OLD_OLY = Boolean.parseBoolean(Luna.getProperty("enable_old_oly", "true"));
					SYNERGY_CHANCE_ON_PVP = Integer.parseInt(Luna.getProperty("synergy_chance_on_pvp", "35"));
					CHANCE_EFFECT_DISPLAY = Integer.parseInt(Luna.getProperty("chance_effect_display_events", "25"));
					ENABLE_SKILL_ANIMATIONS = Boolean.parseBoolean(Luna.getProperty("enable_skill_animations", "false"));
					ENABLE_BOT_CAPTCHA = Boolean.parseBoolean(Luna.getProperty("enable_captcha", "true"));
					ENABLE_BONUS_PVP = Boolean.parseBoolean(Luna.getProperty("enable_bonus_pvp", "false"));
					ENABLE_CLAN_WAR_BONUS_PVP = Boolean.parseBoolean(Luna.getProperty("enable_clan_war_bonus_pvp", "false"));
					BONUS_PVP_AMMOUNT = Integer.parseInt(Luna.getProperty("bonus_pvp_ammount", "1"));
					BONUS_PVP_AMMOUNT_2_SIDE = Integer.parseInt(Luna.getProperty("bonus_pvp_ammount_2_side", "1"));
					BONUS_PVP_AMMOUNT_2_SIDE = Integer.parseInt(Luna.getProperty("bonus_pvp_ammount_1_side", "1"));
					BONUS_CLAN_REP_AMMOUNT_2_SIDE = Integer.parseInt(Luna.getProperty("bonus_clan_rep_ammount_2_side", "1"));
					BONUS_CLAN_REP_AMMOUNT_1_SIDE = Integer.parseInt(Luna.getProperty("bonus_clan_rep_ammount_1_side", "1"));
					EVENT_HEAL_MUL = Double.parseDouble(Luna.getProperty("event_heal_mul", "1"));
					EVENT_MPCONSUME_MUL = Double.parseDouble(Luna.getProperty("event_heal_mpCons_mul", "3.5"));
					DOUBLE_PVP = Boolean.parseBoolean(Luna.getProperty("double_pvp_weekends", "true"));
					PVP_PROTECTIONS = Boolean.parseBoolean(Luna.getProperty("pvp_protections", "true"));
					// NEW CONFIGS
					SYNERGY_RADIUS = Integer.parseInt(Luna.getProperty("synergy_radius", "1200"));
					SYNERGY_BOOST_2_SUPPORTS = Double.parseDouble(Luna.getProperty("synergy_mul_2_sup", "1.25"));
					SYNERGY_BOOST_3_SUPPORTS = Double.parseDouble(Luna.getProperty("synergy_mul_3_and_more_sup", "1.5"));
					KARMA_EXP_LOST_MUL = Double.parseDouble(Luna.getProperty("karma_exp_lost_mul", "1.0"));
					PVP_EXP_MUL = Double.parseDouble(Luna.getProperty("pvp_exp_mul", "1.0"));
					SUP_PVP_EXP_MUL = Double.parseDouble(Luna.getProperty("sup_pvp_exp_mul", "1.0"));
					PVP_TOKEN_CHANCE = Integer.parseInt(Luna.getProperty("chance_pvp_token", "15"));
					PVP_TOKEN_CHANCE_HOT_ZONES = Integer.parseInt(Luna.getProperty("chance_pvp_token_hot_zones", "25"));
					PVP_TOKEN_CHANCE_EVENTS = Integer.parseInt(Luna.getProperty("chance_pvp_token_events", "35"));
					RARE_PVP_TOKEN_CHANCE = Integer.parseInt(Luna.getProperty("chance_rare_pvp_token", "15"));
					RARE_PVP_TOKEN_CHANCE_HOT_ZONES = Integer.parseInt(Luna.getProperty("chance_rare_pvp_token_hot_zones", "25"));
					RARE_PVP_TOKEN_CHANCE_EVENTS = Integer.parseInt(Luna.getProperty("chance_rare_pvp_token_events", "35"));
					CLAN_ESSENCE_CHANCE = Integer.parseInt(Luna.getProperty("chance_clan_essence", "1"));
					CLAN_ESSENCE_CHANCE_HOT_ZONES = Integer.parseInt(Luna.getProperty("chance_clan_essence_hot_zones", "15"));
					CLAN_ESSENCE_CHANCE_SIEGES = Integer.parseInt(Luna.getProperty("chance_clan_essence_siege", "20"));
					PVP_CHEST_POOL_SIZE = Integer.parseInt(Luna.getProperty("size_pool_pvp_chest", "2000"));
					MULTISELL_UNTRADEABLE_SOURCE_ITEMS_PVP_SERVER = Boolean.parseBoolean(Luna.getProperty("multisell_untreadeable_source_extra_items", "false"));
					ALLOW_CLAN_WAREHOUSE = Boolean.parseBoolean(Luna.getProperty("allow_clan_warehouse", "false"));
					ALLOW_CASTLE_WAREHOUSE = Boolean.parseBoolean(Luna.getProperty("allow_castle_warehouse", "true"));
					ALLOW_FREIGHT_WAREHOUSE = Boolean.parseBoolean(Luna.getProperty("allow_freight_warehouse", "true"));
					// instances

					
					// logger
					ENABLE_SUBCLASS_LOGS = Boolean.parseBoolean(Luna.getProperty("enable_subclass_logs", "true"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + LUNA + " File.");
				}
				// Load events Properties file (if exists)
				try
				{
					L2Properties Events = new L2Properties();
					is = new FileInputStream(new File(EVENTS_CONFIG_FILE));
					Events.load(is);
					CTF_EVEN_TEAMS = Events.getProperty("CTFEvenTeams", "BALANCE");
					CTF_ALLOW_INTERFERENCE = Boolean.parseBoolean(Events.getProperty("CTFAllowInterference", "false"));
					CTF_ALLOW_POTIONS = Boolean.parseBoolean(Events.getProperty("CTFAllowPotions", "false"));
					CTF_ALLOW_SUMMON = Boolean.parseBoolean(Events.getProperty("CTFAllowSummon", "false"));
					CTF_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(Events.getProperty("CTFOnStartRemoveAllEffects", "true"));
					CTF_ON_START_UNSUMMON_PET = Boolean.parseBoolean(Events.getProperty("CTFOnStartUnsummonPet", "true"));
					CTF_ANNOUNCE_TEAM_STATS = Boolean.parseBoolean(Events.getProperty("CTFAnnounceTeamStats", "false"));
					CTF_ANNOUNCE_REWARD = Boolean.parseBoolean(Events.getProperty("CTFAnnounceReward", "false"));
					CTF_JOIN_CURSED = Boolean.parseBoolean(Events.getProperty("CTFJoinWithCursedWeapon", "true"));
					CTF_REVIVE_RECOVERY = Boolean.parseBoolean(Events.getProperty("CTFReviveRecovery", "false"));
					CTF_REVIVE_DELAY = Long.parseLong(Events.getProperty("CTFReviveDelay", "20000"));
					if (CTF_REVIVE_DELAY < 1000)
						CTF_REVIVE_DELAY = 1000; // can't be set less then 1 second
					TVT_EVEN_TEAMS = Events.getProperty("TvTEvenTeams", "BALANCE");
					TVT_ALLOW_INTERFERENCE = Boolean.parseBoolean(Events.getProperty("TvTAllowInterference", "false"));
					TVT_ALLOW_POTIONS = Boolean.parseBoolean(Events.getProperty("TvTAllowPotions", "false"));
					TVT_ALLOW_SUMMON = Boolean.parseBoolean(Events.getProperty("TvTAllowSummon", "false"));
					TVT_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(Events.getProperty("TvTOnStartRemoveAllEffects", "true"));
					TVT_ON_START_UNSUMMON_PET = Boolean.parseBoolean(Events.getProperty("TvTOnStartUnsummonPet", "true"));
					TVT_REVIVE_RECOVERY = Boolean.parseBoolean(Events.getProperty("TvTReviveRecovery", "false"));
					TVT_ANNOUNCE_TEAM_STATS = Boolean.parseBoolean(Events.getProperty("TvTAnnounceTeamStats", "false"));
					TVT_ANNOUNCE_REWARD = Boolean.parseBoolean(Events.getProperty("TvTAnnounceReward", "false"));
					TVT_PRICE_NO_KILLS = Boolean.parseBoolean(Events.getProperty("TvTPriceNoKills", "false"));
					TVT_JOIN_CURSED = Boolean.parseBoolean(Events.getProperty("TvTJoinWithCursedWeapon", "true"));
					TVT_REVIVE_DELAY = Long.parseLong(Events.getProperty("TVTReviveDelay", "20000"));
					if (TVT_REVIVE_DELAY < 1000)
						TVT_REVIVE_DELAY = 1000; // can't be set less then 1 second
					DM_ALLOW_INTERFERENCE = Boolean.parseBoolean(Events.getProperty("DMAllowInterference", "false"));
					DM_ALLOW_POTIONS = Boolean.parseBoolean(Events.getProperty("DMAllowPotions", "false"));
					DM_ALLOW_SUMMON = Boolean.parseBoolean(Events.getProperty("DMAllowSummon", "false"));
					DM_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(Events.getProperty("DMOnStartRemoveAllEffects", "true"));
					DM_ON_START_UNSUMMON_PET = Boolean.parseBoolean(Events.getProperty("DMOnStartUnsummonPet", "true"));
					DM_REVIVE_DELAY = Long.parseLong(Events.getProperty("DMReviveDelay", "20000"));
					if (DM_REVIVE_DELAY < 1000)
						DM_REVIVE_DELAY = 1000; // can't be set less then 1 second
					VIP_ALLOW_INTERFERENCE = Boolean.parseBoolean(Events.getProperty("VIPAllowInterference", "false"));
					VIP_ALLOW_POTIONS = Boolean.parseBoolean(Events.getProperty("VIPAllowPotions", "false"));
					VIP_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(Events.getProperty("VIPOnStartRemoveAllEffects", "true"));
					VIP_MIN_LEVEL = Integer.parseInt(Events.getProperty("VIPMinLevel", "1"));
					if (VIP_MIN_LEVEL < 1)
						VIP_MIN_LEVEL = 1; // can't be set less then lvl 1
					VIP_MAX_LEVEL = Integer.parseInt(Events.getProperty("VIPMaxLevel", "85"));
					if (VIP_MAX_LEVEL < VIP_MIN_LEVEL)
						VIP_MAX_LEVEL = VIP_MIN_LEVEL + 1; // can't be set less then Min Level
					VIP_MIN_PARTICIPANTS = Integer.parseInt(Events.getProperty("VIPMinParticipants", "10"));
					if (VIP_MIN_PARTICIPANTS < 10)
						VIP_MIN_PARTICIPANTS = 10; // can't be set less then lvl 10
					FALLDOWNONDEATH = Boolean.parseBoolean(Events.getProperty("FallDownOnDeath", "true"));
					ARENA_ENABLED = Boolean.parseBoolean(Events.getProperty("ArenaEnabled", "false"));
					ARENA_INTERVAL = Integer.parseInt(Events.getProperty("ArenaInterval", "60"));
					ARENA_REWARD_ID = Integer.parseInt(Events.getProperty("ArenaRewardId", "57"));
					ARENA_REWARD_COUNT = Integer.parseInt(Events.getProperty("ArenaRewardCount", "100"));
					FISHERMAN_ENABLED = Boolean.parseBoolean(Events.getProperty("FishermanEnabled", "false"));
					FISHERMAN_INTERVAL = Integer.parseInt(Events.getProperty("FishermanInterval", "60"));
					FISHERMAN_REWARD_ID = Integer.parseInt(Events.getProperty("FishermanRewardId", "57"));
					FISHERMAN_REWARD_COUNT = Integer.parseInt(Events.getProperty("FishermanRewardCount", "100"));
					TVTI_INSTANCE_XML = Events.getProperty("TvTIInstanceXML", "TvTI.xml");
					TVTI_ALLOW_TIE = Boolean.parseBoolean(Events.getProperty("TvTIAllowTie", "false"));
					TVTI_CHECK_WEIGHT_AND_INVENTORY = Boolean.parseBoolean(Events.getProperty("TvTICheckWeightAndInventory", "true"));
					TVTI_ALLOW_INTERFERENCE = Boolean.parseBoolean(Events.getProperty("TvTIAllowInterference", "false"));
					TVTI_ALLOW_POTIONS = Boolean.parseBoolean(Events.getProperty("TvTIAllowPotions", "false"));
					TVTI_ALLOW_SUMMON = Boolean.parseBoolean(Events.getProperty("TvTIAllowSummon", "false"));
					TVTI_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(Events.getProperty("TvTIOnStartRemoveAllEffects", "true"));
					TVTI_ON_START_UNSUMMON_PET = Boolean.parseBoolean(Events.getProperty("TvTIOnStartUnsummonPet", "true"));
					TVTI_REVIVE_RECOVERY = Boolean.parseBoolean(Events.getProperty("TvTIReviveRecovery", "false"));
					TVTI_ANNOUNCE_TEAM_STATS = Boolean.parseBoolean(Events.getProperty("TvTIAnnounceTeamStats", "false"));
					TVTI_ANNOUNCE_REWARD = Boolean.parseBoolean(Events.getProperty("TvTIAnnounceReward", "false"));
					TVTI_PRICE_NO_KILLS = Boolean.parseBoolean(Events.getProperty("TvTIPriceNoKills", "false"));
					TVTI_JOIN_CURSED = Boolean.parseBoolean(Events.getProperty("TvTIJoinWithCursedWeapon", "true"));
					TVTI_SHOW_STATS_PAGE = Boolean.parseBoolean(Events.getProperty("TvTIShowStatistics", "true"));
					TVTI_SORT_TEAMS = Integer.parseInt(Events.getProperty("TvTISortTeams", "0"));
					TVTI_JOIN_NPC_SKILL = Integer.parseInt(Events.getProperty("TvTIJoinNpcSkill", "1034"));
					TVTI_REVIVE_DELAY = Long.parseLong(Events.getProperty("TvTIReviveDelay", "20000"));
					if (TVTI_REVIVE_DELAY < 1000)
						TVTI_REVIVE_DELAY = 1000; // can't be set less then 1 second
					TVTI_JOIN_NPC_DO_SKILL_AGAIN = Long.parseLong(Events.getProperty("TvTIJoinNpcDoSkillAgain", "0"));
					if (TVTI_JOIN_NPC_DO_SKILL_AGAIN < 1000 && TVTI_JOIN_NPC_DO_SKILL_AGAIN != 0)
						TVTI_JOIN_NPC_DO_SKILL_AGAIN = 1000; // can't be set less then 1 second
					FortressSiege_EVEN_TEAMS = Events.getProperty("FortressSiegeEvenTeams", "BALANCE");
					FortressSiege_SAME_IP_PLAYERS_ALLOWED = Boolean.parseBoolean(Events.getProperty("FortressSiegeSameIPPlayersAllowed", "false"));
					FortressSiege_ALLOW_INTERFERENCE = Boolean.parseBoolean(Events.getProperty("FortressSiegeAllowInterference", "false"));
					FortressSiege_ALLOW_POTIONS = Boolean.parseBoolean(Events.getProperty("FortressSiegeAllowPotions", "false"));
					FortressSiege_ALLOW_SUMMON = Boolean.parseBoolean(Events.getProperty("FortressSiegeAllowSummon", "false"));
					FortressSiege_ON_START_REMOVE_ALL_EFFECTS = Boolean.parseBoolean(Events.getProperty("FortressSiegeOnStartRemoveAllEffects", "true"));
					FortressSiege_ON_START_UNSUMMON_PET = Boolean.parseBoolean(Events.getProperty("FortressSiegeOnStartUnsummonPet", "true"));
					FortressSiege_REVIVE_RECOVERY = Boolean.parseBoolean(Events.getProperty("FortressSiegeReviveRecovery", "false"));
					FortressSiege_ANNOUNCE_TEAM_STATS = Boolean.parseBoolean(Events.getProperty("FortressSiegeAnnounceTeamStats", "false"));
					FortressSiege_PRICE_NO_KILLS = Boolean.parseBoolean(Events.getProperty("FortressSiegePriceNoKills", "false"));
					FortressSiege_JOIN_CURSED = Boolean.parseBoolean(Events.getProperty("FortressSiegeJoinWithCursedWeapon", "true"));
					FortressSiege_REVIVE_DELAY = Long.parseLong(Events.getProperty("FortressSiegeReviveDelay", "50000"));
					FortressSiege_HWID_CHECK = Boolean.parseBoolean(Events.getProperty("FortressSiegeHwidCheck", "true"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + EVENTS_CONFIG_FILE + " File.");
				}
				// Load auto events Properties file (if exists)
				try
				{
					L2Properties AutoEvents = new L2Properties();
					is = new FileInputStream(new File(AUTO_EVENTS_CONFIG_FILE));
					AutoEvents.load(is);
					AUTO_TVT_ENABLED = Boolean.parseBoolean(AutoEvents.getProperty("EnableAutoTvT", "false"));
					if (AUTO_TVT_ENABLED)
						AutomatedTvT.startIfNecessary();
					StringTokenizer coords;
					StringTokenizer locations = new StringTokenizer(AutoEvents.getProperty("TvTTeamLocations", ""), ";");
					AUTO_TVT_TEAM_LOCATIONS = new int[locations.countTokens()][3];
					for (int i = 0; i < AUTO_TVT_TEAM_LOCATIONS.length; i++)
					{
						coords = new StringTokenizer(locations.nextToken(), ",");
						if (coords.countTokens() == 3)
						{
							AUTO_TVT_TEAM_LOCATIONS[i] = new int[3];
							AUTO_TVT_TEAM_LOCATIONS[i][0] = Integer.parseInt(coords.nextToken());
							AUTO_TVT_TEAM_LOCATIONS[i][1] = Integer.parseInt(coords.nextToken());
							AUTO_TVT_TEAM_LOCATIONS[i][2] = Integer.parseInt(coords.nextToken());
						}
						else
							throw new IllegalArgumentException("Cannot parse TvTTeamLocations!");
					}
					AUTO_TVT_TEAM_COLORS_RANDOM = Boolean.parseBoolean(AutoEvents.getProperty("TvTTeamColorsRandom", "true"));
					AUTO_TVT_TEAM_COLORS = new int[AUTO_TVT_TEAM_LOCATIONS.length];
					if (!AUTO_TVT_TEAM_COLORS_RANDOM)
					{
						coords = new StringTokenizer(AutoEvents.getProperty("TvTTeamColors", ""), ",");
						if (coords.countTokens() == AUTO_TVT_TEAM_COLORS.length)
						{
							for (int i = 0; i < AUTO_TVT_TEAM_COLORS.length; i++)
								AUTO_TVT_TEAM_COLORS[i] = Integer.decode("0x" + coords.nextToken());
						}
						else
							throw new IllegalArgumentException("Cannot parse TvTTeamColors!");
					}
					AUTO_TVT_OVERRIDE_TELE_BACK = Boolean.parseBoolean(AutoEvents.getProperty("TvTTeleportAfterEventSpecial", "false"));
					coords = new StringTokenizer(AutoEvents.getProperty("TvTTeleportSpecialLocation", ""), ",");
					if (coords.countTokens() == 3)
					{
						AUTO_TVT_DEFAULT_TELE_BACK = new int[3];
						AUTO_TVT_DEFAULT_TELE_BACK[0] = Integer.parseInt(coords.nextToken());
						AUTO_TVT_DEFAULT_TELE_BACK[1] = Integer.parseInt(coords.nextToken());
						AUTO_TVT_DEFAULT_TELE_BACK[2] = Integer.parseInt(coords.nextToken());
					}
					else
						throw new IllegalArgumentException("Cannot parse TvTTeleportSpecialLocation!");
					AUTO_TVT_REWARD_MIN_POINTS = Integer.parseInt(AutoEvents.getProperty("TvTRewardedMinScore", "1"));
					locations = new StringTokenizer(AutoEvents.getProperty("TvTRewards", ""), ";");
					AUTO_TVT_REWARD_IDS = new int[locations.countTokens()];
					AUTO_TVT_REWARD_COUNT = new int[locations.countTokens()];
					for (int i = 0; i < AUTO_TVT_REWARD_IDS.length; i++)
					{
						coords = new StringTokenizer(locations.nextToken(), ",");
						if (coords.countTokens() == 2)
						{
							AUTO_TVT_REWARD_IDS[i] = Integer.parseInt(coords.nextToken());
							AUTO_TVT_REWARD_COUNT[i] = Integer.parseInt(coords.nextToken());
						}
						else
							throw new IllegalArgumentException("Cannot parse TvTRewards!");
					}
					AUTO_TVT_LEVEL_MAX = Integer.parseInt(AutoEvents.getProperty("TvTMaxLevel", "85"));
					AUTO_TVT_LEVEL_MIN = Integer.parseInt(AutoEvents.getProperty("TvTMinLevel", "1"));
					AUTO_TVT_PARTICIPANTS_MAX = Integer.parseInt(AutoEvents.getProperty("TvTMaxParticipants", "25"));
					AUTO_TVT_PARTICIPANTS_MIN = Integer.parseInt(AutoEvents.getProperty("TvTMinParticipants", "6"));
					AUTO_TVT_DELAY_INITIAL_REGISTRATION = Long.parseLong(AutoEvents.getProperty("TvTDelayInitial", "900000"));
					AUTO_TVT_DELAY_BETWEEN_EVENTS = Long.parseLong(AutoEvents.getProperty("TvTDelayBetweenEvents", "900000"));
					AUTO_TVT_PERIOD_LENGHT_REGISTRATION = Long.parseLong(AutoEvents.getProperty("TvTLengthRegistration", "300000"));
					AUTO_TVT_PERIOD_LENGHT_PREPARATION = Long.parseLong(AutoEvents.getProperty("TvTLengthPreparation", "50000"));
					AUTO_TVT_PERIOD_LENGHT_EVENT = Long.parseLong(AutoEvents.getProperty("TvTLengthCombat", "240000"));
					AUTO_TVT_PERIOD_LENGHT_REWARDS = Long.parseLong(AutoEvents.getProperty("TvTLengthRewards", "15000"));
					AUTO_TVT_REGISTRATION_ANNOUNCEMENT_COUNT = Integer.parseInt(AutoEvents.getProperty("TvTAnnounceRegistration", "3"));
					AUTO_TVT_REGISTER_CURSED = Boolean.parseBoolean(AutoEvents.getProperty("TvTRegisterCursedWeaponOwner", "false"));
					AUTO_TVT_REGISTER_HERO = Boolean.parseBoolean(AutoEvents.getProperty("TvTRegisterHero", "true"));
					AUTO_TVT_REGISTER_CANCEL = Boolean.parseBoolean(AutoEvents.getProperty("TvTCanCancelRegistration", "false"));
					AUTO_TVT_REGISTER_AFTER_RELOG = Boolean.parseBoolean(AutoEvents.getProperty("TvTRegisterOnRelogin", "true"));
					coords = new StringTokenizer(AutoEvents.getProperty("TvTItemsNotAllowed", ""), ",");
					AUTO_TVT_DISALLOWED_ITEMS = new int[coords.countTokens()];
					for (int i = 0; i < AUTO_TVT_DISALLOWED_ITEMS.length; i++)
						AUTO_TVT_DISALLOWED_ITEMS[i] = Integer.parseInt(coords.nextToken());
					AUTO_TVT_START_CANCEL_BUFFS = Boolean.parseBoolean(AutoEvents.getProperty("TvTOnStartCancelBuffs", "false"));
					AUTO_TVT_START_CANCEL_CUBICS = Boolean.parseBoolean(AutoEvents.getProperty("TvTOnStartCancelCubics", "false"));
					AUTO_TVT_START_CANCEL_SERVITORS = Boolean.parseBoolean(AutoEvents.getProperty("TvTOnStartCancelServitors", "true"));
					AUTO_TVT_START_CANCEL_TRANSFORMATION = Boolean.parseBoolean(AutoEvents.getProperty("TvTOnStartCancelTransformation", "true"));
					AUTO_TVT_START_CANCEL_PARTY = Boolean.parseBoolean(AutoEvents.getProperty("TvTOnStartDisbandParty", "true"));
					AUTO_TVT_START_RECOVER = Boolean.parseBoolean(AutoEvents.getProperty("TvTOnStartRecover", "true"));
					AUTO_TVT_GODLIKE_SYSTEM = Boolean.parseBoolean(AutoEvents.getProperty("TvTGodlikeSystem", "true"));
					AUTO_TVT_GODLIKE_ANNOUNCE = Boolean.parseBoolean(AutoEvents.getProperty("TvTGodlikeAnnounce", "true"));
					AUTO_TVT_GODLIKE_MIN_KILLS = Integer.parseInt(AutoEvents.getProperty("TvTGodlikeMinKills", "5"));
					AUTO_TVT_GODLIKE_POINT_MULTIPLIER = Integer.parseInt(AutoEvents.getProperty("TvTGodlikeKillPoints", "3"));
					AUTO_TVT_GODLIKE_TITLE = AutoEvents.getProperty("TvTGodlikeTitle", "God-like").trim();
					AUTO_TVT_REVIVE_SELF = Boolean.parseBoolean(AutoEvents.getProperty("TvTReviveSelf", "false"));
					AUTO_TVT_REVIVE_RECOVER = Boolean.parseBoolean(AutoEvents.getProperty("TvTReviveRecover", "true"));
					AUTO_TVT_REVIVE_DELAY = Long.parseLong(AutoEvents.getProperty("TvTReviveDelay", "5000"));
					AUTO_TVT_SPAWN_PROTECT = Integer.parseInt(AutoEvents.getProperty("TvTSpawnProtection", "0"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + AUTO_EVENTS_CONFIG_FILE + " File.");
				}
				// Load Rates Properties file (if exists)
				try
				{
					L2Properties ratesSettings = new L2Properties();
					is = new FileInputStream(new File(RATES_CONFIG_FILE));
					ratesSettings.load(is);
					RATE_XP = Float.parseFloat(ratesSettings.getProperty("RateXp", "1."));
					RATE_SP = Float.parseFloat(ratesSettings.getProperty("RateSp", "1."));
					RATE_PARTY_XP = Float.parseFloat(ratesSettings.getProperty("RatePartyXp", "1."));
					RATE_PARTY_SP = Float.parseFloat(ratesSettings.getProperty("RatePartySp", "1."));
					RATE_QUESTS_REWARD = Float.parseFloat(ratesSettings.getProperty("RateQuestsReward", "1."));
					RATE_DROP_ADENA = Float.parseFloat(ratesSettings.getProperty("RateDropAdena", "1."));
					RATE_CONSUMABLE_COST = Float.parseFloat(ratesSettings.getProperty("RateConsumableCost", "1."));
					RATE_EXTR_FISH = Float.parseFloat(ratesSettings.getProperty("RateExtractFish", "1."));
					RATE_DROP_ITEMS = Float.parseFloat(ratesSettings.getProperty("RateDropItems", "1."));
					RATE_DROP_ITEMS_BY_RAID = Float.parseFloat(ratesSettings.getProperty("RateRaidDropItems", "1."));
					RATE_DROP_SPOIL = Float.parseFloat(ratesSettings.getProperty("RateDropSpoil", "1."));
					RATE_DROP_MANOR = Integer.parseInt(ratesSettings.getProperty("RateDropManor", "1"));
					RATE_DROP_QUEST = Float.parseFloat(ratesSettings.getProperty("RateDropQuest", "1."));
					RATE_KARMA_EXP_LOST = Float.parseFloat(ratesSettings.getProperty("RateKarmaExpLost", "1."));
					RATE_SIEGE_GUARDS_PRICE = Float.parseFloat(ratesSettings.getProperty("RateSiegeGuardsPrice", "1."));
					RATE_DROP_COMMON_HERBS = Float.parseFloat(ratesSettings.getProperty("RateCommonHerbs", "15."));
					RATE_DROP_MP_HP_HERBS = Float.parseFloat(ratesSettings.getProperty("RateHpMpHerbs", "10."));
					RATE_DROP_GREATER_HERBS = Float.parseFloat(ratesSettings.getProperty("RateGreaterHerbs", "4."));
					RATE_DROP_SUPERIOR_HERBS = Float.parseFloat(ratesSettings.getProperty("RateSuperiorHerbs", "0.8")) * 10;
					RATE_DROP_SPECIAL_HERBS = Float.parseFloat(ratesSettings.getProperty("RateSpecialHerbs", "0.2")) * 10;
					PLAYER_DROP_LIMIT = Integer.parseInt(ratesSettings.getProperty("PlayerDropLimit", "3"));
					PLAYER_RATE_DROP = Integer.parseInt(ratesSettings.getProperty("PlayerRateDrop", "5"));
					PLAYER_RATE_DROP_ITEM = Integer.parseInt(ratesSettings.getProperty("PlayerRateDropItem", "70"));
					PLAYER_RATE_DROP_EQUIP = Integer.parseInt(ratesSettings.getProperty("PlayerRateDropEquip", "25"));
					PLAYER_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(ratesSettings.getProperty("PlayerRateDropEquipWeapon", "5"));
					PET_XP_RATE = Float.parseFloat(ratesSettings.getProperty("PetXpRate", "1."));
					RATE_RECOVERY_ON_RECONNECT = Float.parseFloat(ratesSettings.getProperty("RateRecoveryOnReconnect", "4."));
					PET_FOOD_RATE = Integer.parseInt(ratesSettings.getProperty("PetFoodRate", "1"));
					SINEATER_XP_RATE = Float.parseFloat(ratesSettings.getProperty("SinEaterXpRate", "1."));
					KARMA_DROP_LIMIT = Integer.parseInt(ratesSettings.getProperty("KarmaDropLimit", "10"));
					KARMA_RATE_DROP = Integer.parseInt(ratesSettings.getProperty("KarmaRateDrop", "70"));
					RATE_DROP_VITALITY_HERBS = Float.parseFloat(ratesSettings.getProperty("RateVitalityHerbs", "2."));
					KARMA_RATE_DROP_ITEM = Integer.parseInt(ratesSettings.getProperty("KarmaRateDropItem", "50"));
					KARMA_RATE_DROP_EQUIP = Integer.parseInt(ratesSettings.getProperty("KarmaRateDropEquip", "40"));
					KARMA_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(ratesSettings.getProperty("KarmaRateDropEquipWeapon", "10"));
					// Initializing table
					PLAYER_XP_PERCENT_LOST = new double[Byte.MAX_VALUE + 1];
					// Default value
					for (int i = 0; i <= Byte.MAX_VALUE; i++)
						PLAYER_XP_PERCENT_LOST[i] = 1.;
					// Now loading into table parsed values
					try
					{
						String[] values = ratesSettings.getProperty("PlayerXPPercentLost", "0,39-7.0;40,75-4.0;76,76-2.5;77,77-2.0;78,78-1.5").split(";");
						for (String s : values)
						{
							int min;
							int max;
							double val;
							String[] vals = s.split("-");
							String[] mM = vals[0].split(",");
							min = Integer.parseInt(mM[0]);
							max = Integer.parseInt(mM[1]);
							val = Double.parseDouble(vals[1]);
							for (int i = min; i <= max; i++)
								PLAYER_XP_PERCENT_LOST[i] = val;
						}
					}
					catch (Exception e)
					{
						_log.warning("Error while loading Player XP percent lost");
						e.printStackTrace();
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + RATES_CONFIG_FILE + " File.");
				}
				try
				{
					L2Properties chill = new L2Properties();
					is = new FileInputStream(new File(CHILL_FILE));
					chill.load(is);
					CHILL_SLEEP_TICKS = Integer.parseInt(chill.getProperty("SleepTicks", "333"));
					DAILY_CREDIT = Integer.parseInt(chill.getProperty("DailyCredit", "8"));
					EVENT_CREDIT = Integer.parseInt(chill.getProperty("EventCredit", "3600000"));
					INERTIA_RT = Integer.parseInt(chill.getProperty("InertiaResponsetime", "6"));
					LAG_NEW_TARGET = Integer.parseInt(chill.getProperty("LagNewTarget", "2000"));
					LAG_DIE_TARGET = Integer.parseInt(chill.getProperty("LagDieTarget", "1000"));
					LAG_KIL_TARGET = Integer.parseInt(chill.getProperty("LagKilTarget", "1000"));
					LAG_ASI_TARGET = Integer.parseInt(chill.getProperty("LagAsiTarget", "1000"));
					FOLLOW_INIT_RANGE = Integer.parseInt(chill.getProperty("FollowInitRange", "400"));
					RANGE_CLOSE = Integer.parseInt(chill.getProperty("RangeClose", "400"));
					RANGE_NEAR = Integer.parseInt(chill.getProperty("RangeNear", "800"));
					RANGE_FAR = Integer.parseInt(chill.getProperty("RangeFar", "1400"));
					DAILY_CREDIT_TIME = chill.getProperty("DailyCreditTime", "2:00");
				}
				catch (Exception e)
				{
					System.out.println("FIXME: CHILL.PROPERTIES ERROR");
					e.printStackTrace();
					System.exit(1);
				}
				// Load L2JMod Properties file (if exists)
				try
				{
					L2Properties L2JModSettings = new L2Properties();
					is = new FileInputStream(new File(L2JMOD_CONFIG_FILE));
					L2JModSettings.load(is);
					L2JMOD_CHAMPION_ENABLE = Boolean.parseBoolean(L2JModSettings.getProperty("ChampionEnable", "false"));
					L2JMOD_CHAMPION_PASSIVE = Boolean.parseBoolean(L2JModSettings.getProperty("ChampionPassive", "false"));
					L2JMOD_CHAMPION_FREQUENCY = Integer.parseInt(L2JModSettings.getProperty("ChampionFrequency", "0"));
					L2JMOD_CHAMP_TITLE = L2JModSettings.getProperty("ChampionTitle", "Champion");
					L2JMOD_CHAMP_MIN_LVL = Integer.parseInt(L2JModSettings.getProperty("ChampionMinLevel", "20"));
					L2JMOD_CHAMP_MAX_LVL = Integer.parseInt(L2JModSettings.getProperty("ChampionMaxLevel", "60"));
					L2JMOD_CHAMPION_HP = Integer.parseInt(L2JModSettings.getProperty("ChampionHp", "7"));
					L2JMOD_CHAMPION_HP_REGEN = Float.parseFloat(L2JModSettings.getProperty("ChampionHpRegen", "1."));
					L2JMOD_CHAMPION_REWARDS = Integer.parseInt(L2JModSettings.getProperty("ChampionRewards", "8"));
					L2JMOD_CHAMPION_ADENAS_REWARDS = Float.parseFloat(L2JModSettings.getProperty("ChampionAdenasRewards", "1"));
					L2JMOD_CHAMPION_ATK = Float.parseFloat(L2JModSettings.getProperty("ChampionAtk", "1."));
					L2JMOD_CHAMPION_SPD_ATK = Float.parseFloat(L2JModSettings.getProperty("ChampionSpdAtk", "1."));
					L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE = Integer.parseInt(L2JModSettings.getProperty("ChampionRewardLowerLvlItemChance", "0"));
					L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE = Integer.parseInt(L2JModSettings.getProperty("ChampionRewardHigherLvlItemChance", "0"));
					L2JMOD_CHAMPION_REWARD_ID = Integer.parseInt(L2JModSettings.getProperty("ChampionRewardItemID", "6393"));
					L2JMOD_CHAMPION_REWARD_QTY = Integer.parseInt(L2JModSettings.getProperty("ChampionRewardItemQty", "1"));
					L2JMOD_CHAMPION_ENABLE_VITALITY = Boolean.parseBoolean(L2JModSettings.getProperty("ChampionEnableVitality", "False"));
					TVT_EVENT_ENABLED = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventEnabled", "false"));
					TVT_EVENT_IN_INSTANCE = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventInInstance", "false"));
					TVT_EVENT_INSTANCE_FILE = L2JModSettings.getProperty("TvTEventInstanceFile", "coliseum.xml");
					TVT_EVENT_INTERVAL = L2JModSettings.getProperty("TvTEventInterval", "20:00").split(",");
					TVT_EVENT_PARTICIPATION_TIME = Integer.parseInt(L2JModSettings.getProperty("TvTEventParticipationTime", "3600"));
					TVT_EVENT_RUNNING_TIME = Integer.parseInt(L2JModSettings.getProperty("TvTEventRunningTime", "1800"));
					TVT_EVENT_PARTICIPATION_NPC_ID = Integer.parseInt(L2JModSettings.getProperty("TvTEventParticipationNpcId", "0"));
					L2JMOD_ALLOW_WEDDING = Boolean.parseBoolean(L2JModSettings.getProperty("AllowWedding", "False"));
					L2JMOD_WEDDING_PRICE = Integer.parseInt(L2JModSettings.getProperty("WeddingPrice", "250000000"));
					L2JMOD_WEDDING_PUNISH_INFIDELITY = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingPunishInfidelity", "True"));
					L2JMOD_WEDDING_TELEPORT = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingTeleport", "True"));
					L2JMOD_WEDDING_TELEPORT_PRICE = Integer.parseInt(L2JModSettings.getProperty("WeddingTeleportPrice", "50000"));
					L2JMOD_WEDDING_TELEPORT_DURATION = Integer.parseInt(L2JModSettings.getProperty("WeddingTeleportDuration", "60"));
					L2JMOD_WEDDING_SAMESEX = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingAllowSameSex", "False"));
					L2JMOD_WEDDING_FORMALWEAR = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingFormalWear", "True"));
					L2JMOD_WEDDING_DIVORCE_COSTS = Integer.parseInt(L2JModSettings.getProperty("WeddingDivorceCosts", "20"));
					L2JMOD_ENABLE_WAREHOUSESORTING_CLAN = Boolean.valueOf(L2JModSettings.getProperty("EnableWarehouseSortingClan", "False"));
					L2JMOD_ENABLE_WAREHOUSESORTING_PRIVATE = Boolean.valueOf(L2JModSettings.getProperty("EnableWarehouseSortingPrivate", "False"));
					L2JMOD_ENABLE_WAREHOUSESORTING_FREIGHT = Boolean.valueOf(L2JModSettings.getProperty("EnableWarehouseSortingFreight", "False"));
					if (TVT_EVENT_PARTICIPATION_NPC_ID == 0)
					{
						TVT_EVENT_ENABLED = false;
						_log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationNpcId");
					}
					else
					{
						String[] propertySplit = L2JModSettings.getProperty("TvTEventParticipationNpcCoordinates", "0,0,0").split(",");
						if (propertySplit.length < 3)
						{
							TVT_EVENT_ENABLED = false;
							_log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationNpcCoordinates");
						}
						else
						{
							TVT_EVENT_REWARDS = new FastList<int[]>();
							TVT_DOORS_IDS_TO_OPEN = new ArrayList<Integer>();
							TVT_DOORS_IDS_TO_CLOSE = new ArrayList<Integer>();
							TVT_EVENT_PARTICIPATION_NPC_COORDINATES = new int[3];
							TVT_EVENT_TEAM_1_COORDINATES = new int[3];
							TVT_EVENT_TEAM_2_COORDINATES = new int[3];
							TVT_EVENT_PARTICIPATION_NPC_COORDINATES[0] = Integer.parseInt(propertySplit[0]);
							TVT_EVENT_PARTICIPATION_NPC_COORDINATES[1] = Integer.parseInt(propertySplit[1]);
							TVT_EVENT_PARTICIPATION_NPC_COORDINATES[2] = Integer.parseInt(propertySplit[2]);
							TVT_EVENT_MIN_PLAYERS_IN_TEAMS = Integer.parseInt(L2JModSettings.getProperty("TvTEventMinPlayersInTeams", "1"));
							TVT_EVENT_MAX_PLAYERS_IN_TEAMS = Integer.parseInt(L2JModSettings.getProperty("TvTEventMaxPlayersInTeams", "20"));
							TVT_EVENT_MIN_LVL = (byte) Integer.parseInt(L2JModSettings.getProperty("TvTEventMinPlayerLevel", "1"));
							TVT_EVENT_MAX_LVL = (byte) Integer.parseInt(L2JModSettings.getProperty("TvTEventMaxPlayerLevel", "80"));
							TVT_EVENT_RESPAWN_TELEPORT_DELAY = Integer.parseInt(L2JModSettings.getProperty("TvTEventRespawnTeleportDelay", "20"));
							TVT_EVENT_START_LEAVE_TELEPORT_DELAY = Integer.parseInt(L2JModSettings.getProperty("TvTEventStartLeaveTeleportDelay", "20"));
							TVT_EVENT_EFFECTS_REMOVAL = Integer.parseInt(L2JModSettings.getProperty("TvTEventEffectsRemoval", "0"));
							TVT_EVENT_TEAM_1_NAME = L2JModSettings.getProperty("TvTEventTeam1Name", "Team1");
							propertySplit = L2JModSettings.getProperty("TvTEventTeam1Coordinates", "0,0,0").split(",");
							if (propertySplit.length < 3)
							{
								TVT_EVENT_ENABLED = false;
								_log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventTeam1Coordinates");
							}
							else
							{
								TVT_EVENT_TEAM_1_COORDINATES[0] = Integer.parseInt(propertySplit[0]);
								TVT_EVENT_TEAM_1_COORDINATES[1] = Integer.parseInt(propertySplit[1]);
								TVT_EVENT_TEAM_1_COORDINATES[2] = Integer.parseInt(propertySplit[2]);
								TVT_EVENT_TEAM_2_NAME = L2JModSettings.getProperty("TvTEventTeam2Name", "Team2");
								propertySplit = L2JModSettings.getProperty("TvTEventTeam2Coordinates", "0,0,0").split(",");
								if (propertySplit.length < 3)
								{
									TVT_EVENT_ENABLED = false;
									_log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventTeam2Coordinates");
								}
								else
								{
									TVT_EVENT_TEAM_2_COORDINATES[0] = Integer.parseInt(propertySplit[0]);
									TVT_EVENT_TEAM_2_COORDINATES[1] = Integer.parseInt(propertySplit[1]);
									TVT_EVENT_TEAM_2_COORDINATES[2] = Integer.parseInt(propertySplit[2]);
									propertySplit = L2JModSettings.getProperty("TvTEventParticipationFee", "0,0").split(",");
									try
									{
										TVT_EVENT_PARTICIPATION_FEE[0] = Integer.parseInt(propertySplit[0]);
										TVT_EVENT_PARTICIPATION_FEE[1] = Integer.parseInt(propertySplit[1]);
									}
									catch (NumberFormatException nfe)
									{
										if (propertySplit.length > 0)
											_log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationFee");
									}
									propertySplit = L2JModSettings.getProperty("TvTEventReward", "57,100000").split(";");
									for (String reward : propertySplit)
									{
										String[] rewardSplit = reward.split(",");
										if (rewardSplit.length != 2)
											_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventReward \"", reward, "\""));
										else
										{
											try
											{
												TVT_EVENT_REWARDS.add(new int[]
												{
													Integer.parseInt(rewardSplit[0]),
													Integer.parseInt(rewardSplit[1])
												});
											}
											catch (NumberFormatException nfe)
											{
												if (!reward.isEmpty())
													_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventReward \"", reward, "\""));
											}
										}
									}
									TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventTargetTeamMembersAllowed", "true"));
									TVT_EVENT_SCROLL_ALLOWED = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventScrollsAllowed", "false"));
									TVT_EVENT_POTIONS_ALLOWED = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventPotionsAllowed", "false"));
									TVT_EVENT_SUMMON_BY_ITEM_ALLOWED = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventSummonByItemAllowed", "false"));
									TVT_REWARD_TEAM_TIE = Boolean.parseBoolean(L2JModSettings.getProperty("TvTRewardTeamTie", "false"));
									propertySplit = L2JModSettings.getProperty("TvTDoorsToOpen", "").split(";");
									for (String door : propertySplit)
									{
										try
										{
											TVT_DOORS_IDS_TO_OPEN.add(Integer.parseInt(door));
										}
										catch (NumberFormatException nfe)
										{
											if (!door.equals(""))
												_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTDoorsToOpen \"", door, "\""));
										}
									}
									propertySplit = L2JModSettings.getProperty("TvTDoorsToClose", "").split(";");
									for (String door : propertySplit)
									{
										try
										{
											TVT_DOORS_IDS_TO_CLOSE.add(Integer.parseInt(door));
										}
										catch (NumberFormatException nfe)
										{
											if (!door.isEmpty())
												_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTDoorsToClose \"", door, "\""));
										}
									}
								}
							}
						}
					}
					BANKING_SYSTEM_ENABLED = Boolean.parseBoolean(L2JModSettings.getProperty("BankingEnabled", "false"));
					BANKING_SYSTEM_GOLDBARS = Integer.parseInt(L2JModSettings.getProperty("BankingGoldbarCount", "1"));
					BANKING_SYSTEM_ADENA = Integer.parseInt(L2JModSettings.getProperty("BankingAdenaCount", "500000000"));
					OFFLINE_TRADE_ENABLE = Boolean.parseBoolean(L2JModSettings.getProperty("OfflineTradeEnable", "false"));
					OFFLINE_CRAFT_ENABLE = Boolean.parseBoolean(L2JModSettings.getProperty("OfflineCraftEnable", "false"));
					OFFLINE_SET_NAME_COLOR = Boolean.parseBoolean(L2JModSettings.getProperty("OfflineSetNameColor", "false"));
					OFFLINE_NAME_COLOR = Integer.decode("0x" + L2JModSettings.getProperty("OfflineNameColor", "808080"));
					L2JMOD_ENABLE_MANA_POTIONS_SUPPORT = Boolean.parseBoolean(L2JModSettings.getProperty("EnableManaPotionSupport", "false"));
					L2JMOD_ACHIEVEMENT_SYSTEM = Boolean.parseBoolean(L2JModSettings.getProperty("AllowAchievementSystem", "false"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + L2JMOD_CONFIG_FILE + " File.");
				}
				// Load PvP Properties file (if exists)
				try
				{
					L2Properties pvpSettings = new L2Properties();
					is = new FileInputStream(new File(PVP_CONFIG_FILE));
					pvpSettings.load(is);
					KARMA_MIN_KARMA = Integer.parseInt(pvpSettings.getProperty("MinKarma", "240"));
					KARMA_MAX_KARMA = Integer.parseInt(pvpSettings.getProperty("MaxKarma", "10000"));
					KARMA_XP_DIVIDER = Integer.parseInt(pvpSettings.getProperty("XPDivider", "260"));
					KARMA_LOST_BASE = Integer.parseInt(pvpSettings.getProperty("BaseKarmaLost", "0"));
					KARMA_DROP_GM = Boolean.parseBoolean(pvpSettings.getProperty("CanGMDropEquipment", "false"));
					KARMA_AWARD_PK_KILL = Boolean.parseBoolean(pvpSettings.getProperty("AwardPKKillPVPPoint", "true"));
					KARMA_PK_LIMIT = Integer.parseInt(pvpSettings.getProperty("MinimumPKRequiredToDrop", "5"));
					KARMA_NONDROPPABLE_PET_ITEMS = pvpSettings.getProperty("ListOfPetItems", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650,9882");
					KARMA_NONDROPPABLE_ITEMS = pvpSettings.getProperty("ListOfNonDroppableItems", "57,1147,425,1146,461,10,2368,7,6,2370,2369,6842,6611,6612,6613,6614,6615,6616,6617,6618,6619,6620,6621,7694,8181,5575,7694,9388,9389,9390");
					String[] array = KARMA_NONDROPPABLE_PET_ITEMS.split(",");
					KARMA_LIST_NONDROPPABLE_PET_ITEMS = new int[array.length];
					for (int i = 0; i < array.length; i++)
						KARMA_LIST_NONDROPPABLE_PET_ITEMS[i] = Integer.parseInt(array[i]);
					array = KARMA_NONDROPPABLE_ITEMS.split(",");
					KARMA_LIST_NONDROPPABLE_ITEMS = new int[array.length];
					for (int i = 0; i < array.length; i++)
						KARMA_LIST_NONDROPPABLE_ITEMS[i] = Integer.parseInt(array[i]);
					// sorting so binarySearch can be used later
					Arrays.sort(KARMA_LIST_NONDROPPABLE_PET_ITEMS);
					Arrays.sort(KARMA_LIST_NONDROPPABLE_ITEMS);
					PVP_NORMAL_TIME = Integer.parseInt(pvpSettings.getProperty("PvPVsNormalTime", "120000"));
					PVP_PVP_TIME = Integer.parseInt(pvpSettings.getProperty("PvPVsPvPTime", "60000"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + PVP_CONFIG_FILE + " File.");
				}
				try
				{
					L2Properties Settings = new L2Properties();
					is = new FileInputStream(HEXID_FILE);
					Settings.load(is);
					SERVER_ID = Integer.parseInt(Settings.getProperty("ServerID"));
					HEX_ID = new BigInteger(Settings.getProperty("HexID"), 16).toByteArray();
				}
				catch (Exception e)
				{
					_log.warning("Could not load HexID file (" + HEXID_FILE + "). Hopefully login will give us one.");
				}
				loadDonationConfigs(DONATION_CONFIG_FILE);
			}
			finally
			{
				try
				{
					is.close();
				}
				catch (Exception e)
				{}
			}
		}
		else if (Server.serverMode == Server.MODE_LOGINSERVER)
		{
			_log.info("loading login config");
			InputStream is = null;
			try
			{
				try
				{
					L2Properties serverSettings = new L2Properties();
					is = new FileInputStream(new File(LOGIN_CONFIGURATION_FILE));
					serverSettings.load(is);
					GAME_SERVER_LOGIN_HOST = serverSettings.getProperty("LoginHostname", "*");
					GAME_SERVER_LOGIN_PORT = Integer.parseInt(serverSettings.getProperty("LoginPort", "9013"));
					LOGIN_BIND_ADDRESS = serverSettings.getProperty("LoginserverHostname", "*");
					PORT_LOGIN = Integer.parseInt(serverSettings.getProperty("LoginserverPort", "2106"));
					DEBUG = Boolean.parseBoolean(serverSettings.getProperty("Debug", "false"));
					PACKET_HANDLER_DEBUG = Boolean.parseBoolean(serverSettings.getProperty("PacketHandlerDebug", "false"));
					DEVELOPER = Boolean.parseBoolean(serverSettings.getProperty("Developer", "false"));
					ASSERT = Boolean.parseBoolean(serverSettings.getProperty("Assert", "false"));
					ACCEPT_NEW_GAMESERVER = Boolean.parseBoolean(serverSettings.getProperty("AcceptNewGameServer", "True"));
					REQUEST_ID = Integer.parseInt(serverSettings.getProperty("RequestServerID", "0"));
					ACCEPT_ALTERNATE_ID = Boolean.parseBoolean(serverSettings.getProperty("AcceptAlternateID", "True"));
					LOGIN_TRY_BEFORE_BAN = Integer.parseInt(serverSettings.getProperty("LoginTryBeforeBan", "10"));
					LOGIN_BLOCK_AFTER_BAN = Integer.parseInt(serverSettings.getProperty("LoginBlockAfterBan", "600"));
					LOG_LOGIN_CONTROLLER = Boolean.parseBoolean(serverSettings.getProperty("LogLoginController", "true"));
					DATAPACK_ROOT = new File(serverSettings.getProperty("DatapackRoot", ".")).getCanonicalFile(); // FIXME:
					INTERNAL_HOSTNAME = serverSettings.getProperty("InternalHostname", "localhost");
					EXTERNAL_HOSTNAME = serverSettings.getProperty("ExternalHostname", "localhost");
					ROUTER_HOSTNAME = serverSettings.getProperty("RouterHostname", "");
					DATABASE_DRIVER = serverSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
					DATABASE_URL = serverSettings.getProperty("URL", "jdbc:mysql://localhost/l2jdb");
					DATABASE_LOGIN = serverSettings.getProperty("Login", "root");
					DATABASE_PASSWORD = serverSettings.getProperty("Password", "");
					DATABASE_MAX_CONNECTIONS = Integer.parseInt(serverSettings.getProperty("MaximumDbConnections", "10"));
					DATABASE_MAX_IDLE_TIME = Integer.parseInt(serverSettings.getProperty("MaximumDbIdleTime", "0"));
					SHOW_LICENCE = Boolean.parseBoolean(serverSettings.getProperty("ShowLicence", "true"));
					IP_UPDATE_TIME = Integer.parseInt(serverSettings.getProperty("IpUpdateTime", "15"));
					FORCE_GGAUTH = Boolean.parseBoolean(serverSettings.getProperty("ForceGGAuth", "false"));
					AUTO_CREATE_ACCOUNTS = Boolean.parseBoolean(serverSettings.getProperty("AutoCreateAccounts", "True"));
					FLOOD_PROTECTION = Boolean.parseBoolean(serverSettings.getProperty("EnableFloodProtection", "True"));
					FAST_CONNECTION_LIMIT = Integer.parseInt(serverSettings.getProperty("FastConnectionLimit", "15"));
					NORMAL_CONNECTION_TIME = Integer.parseInt(serverSettings.getProperty("NormalConnectionTime", "700"));
					FAST_CONNECTION_TIME = Integer.parseInt(serverSettings.getProperty("FastConnectionTime", "350"));
					MAX_CONNECTION_PER_IP = Integer.parseInt(serverSettings.getProperty("MaxConnectionPerIP", "50"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + LOGIN_CONFIGURATION_FILE + " File.");
				}
				// MMO
				try
				{
					_log.info("Loading " + MMO_CONFIG_FILE.replaceAll("./config/", ""));
					L2Properties mmoSettings = new L2Properties();
					is = new FileInputStream(new File(MMO_CONFIG_FILE));
					mmoSettings.load(is);
					MMO_SELECTOR_SLEEP_TIME = Integer.parseInt(mmoSettings.getProperty("SleepTime", "20"));
					MMO_IO_SELECTOR_THREAD_COUNT = Integer.parseInt(mmoSettings.getProperty("IOSelectorThreadCount", "2"));
					MMO_MAX_SEND_PER_PASS = Integer.parseInt(mmoSettings.getProperty("MaxSendPerPass", "12"));
					MMO_MAX_READ_PER_PASS = Integer.parseInt(mmoSettings.getProperty("MaxReadPerPass", "12"));
					MMO_HELPER_BUFFER_COUNT = Integer.parseInt(mmoSettings.getProperty("HelperBufferCount", "20"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + MMO_CONFIG_FILE + " File.");
				}
				// Load Telnet Properties file (if exists)
				try
				{
					L2Properties telnetSettings = new L2Properties();
					is = new FileInputStream(new File(TELNET_FILE));
					telnetSettings.load(is);
					IS_TELNET_ENABLED = Boolean.parseBoolean(telnetSettings.getProperty("EnableTelnet", "false"));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new Error("Failed to Load " + TELNET_FILE + " File.");
				}
			}
			finally
			{
				try
				{
					is.close();
				}
				catch (Exception e)
				{}
			}
		}
		else
		{
			_log.severe("Could not Load Config: server mode was not set");
		}
	}
	
	/**
	 * @Donation
	 */
	public static boolean	ENABLE_DONATION_CHECKER;
	public static int		DONATION_CHECKER_INITIAL_DELAY;
	public static int		DONATION_CHECKER_INTERVAL;
	public static String	GMAIL_ADDRESS;
	public static String	GMAIL_PASSWORD;
	
	private static void loadDonationConfigs(String configFile)
	{
		L2Properties config = new L2Properties();
		FileInputStream is;
		try
		{
			is = new FileInputStream(new File(configFile));
			config.load(is);
			ENABLE_DONATION_CHECKER = Boolean.parseBoolean(config.getProperty("EnableDonationChecker", "false"));
			DONATION_CHECKER_INITIAL_DELAY = Integer.parseInt(config.getProperty("DonationCheckerInitialDelay", "1")) * 60000;
			DONATION_CHECKER_INTERVAL = Integer.parseInt(config.getProperty("DonationCheckerInterval", "15")) * 60000;
			GMAIL_ADDRESS = config.getProperty("GmailAddress", "");
			GMAIL_PASSWORD = config.getProperty("GmailPassword", "");
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Set a new value to a game parameter from the admin console.
	 * 
	 * @param pName
	 *            (String) : name of the parameter to change
	 * @param pValue
	 *            (String) : new value of the parameter
	 * @return boolean : true if modification has been made
	 * @link useAdminCommand
	 */
	public static boolean setParameterValue(String pName, String pValue)
	{
		if (pName.equalsIgnoreCase("RateXp"))
			RATE_XP = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RateSp"))
			RATE_SP = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RatePartyXp"))
			RATE_PARTY_XP = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RatePartySp"))
			RATE_PARTY_SP = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RateQuestsReward"))
			RATE_QUESTS_REWARD = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RateDropAdena"))
			RATE_DROP_ADENA = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RateConsumableCost"))
			RATE_CONSUMABLE_COST = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RateExtractFish"))
			RATE_EXTR_FISH = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RateDropItems"))
			RATE_DROP_ITEMS = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RateRaidDropItems"))
			RATE_DROP_ITEMS_BY_RAID = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RateDropSpoil"))
			RATE_DROP_SPOIL = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RateDropManor"))
			RATE_DROP_MANOR = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("RateDropQuest"))
			RATE_DROP_QUEST = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RateKarmaExpLost"))
			RATE_KARMA_EXP_LOST = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("RateSiegeGuardsPrice"))
			RATE_SIEGE_GUARDS_PRICE = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("PlayerDropLimit"))
			PLAYER_DROP_LIMIT = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("PlayerRateDrop"))
			PLAYER_RATE_DROP = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("PlayerRateDropItem"))
			PLAYER_RATE_DROP_ITEM = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("PlayerRateDropEquip"))
			PLAYER_RATE_DROP_EQUIP = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("PlayerRateDropEquipWeapon"))
			PLAYER_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("KarmaDropLimit"))
			KARMA_DROP_LIMIT = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("KarmaRateDrop"))
			KARMA_RATE_DROP = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("KarmaRateDropItem"))
			KARMA_RATE_DROP_ITEM = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("KarmaRateDropEquip"))
			KARMA_RATE_DROP_EQUIP = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("KarmaRateDropEquipWeapon"))
			KARMA_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AutoDestroyDroppedItemAfter"))
			AUTODESTROY_ITEM_AFTER = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("DestroyPlayerDroppedItem"))
			DESTROY_DROPPED_PLAYER_ITEM = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("DestroyEquipableItem"))
			DESTROY_EQUIPABLE_PLAYER_ITEM = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("SaveDroppedItem"))
			SAVE_DROPPED_ITEM = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("EmptyDroppedItemTableAfterLoad"))
			EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("SaveDroppedItemInterval"))
			SAVE_DROPPED_ITEM_INTERVAL = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ClearDroppedItemTable"))
			CLEAR_DROPPED_ITEM_TABLE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("PreciseDropCalculation"))
			PRECISE_DROP_CALCULATION = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("MultipleItemDrop"))
			MULTIPLE_ITEM_DROP = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("CoordSynchronize"))
			COORD_SYNCHRONIZE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("DeleteCharAfterDays"))
			DELETE_DAYS = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AllowDiscardItem"))
			ALLOW_DISCARDITEM = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AllowFreight"))
			ALLOW_FREIGHT = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AllowWarehouse"))
			ALLOW_WAREHOUSE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AllowWear"))
			ALLOW_WEAR = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("WearDelay"))
			WEAR_DELAY = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("WearPrice"))
			WEAR_PRICE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AllowWater"))
			ALLOW_WATER = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AllowRentPet"))
			ALLOW_RENTPET = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AllowBoat"))
			ALLOW_BOAT = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AllowCursedWeapons"))
			ALLOW_CURSED_WEAPONS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AllowManor"))
			ALLOW_MANOR = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AllowNpcWalkers"))
			ALLOW_NPC_WALKERS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AllowPetWalkers"))
			ALLOW_PET_WALKERS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("BypassValidation"))
			BYPASS_VALIDATION = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("CommunityType"))
			COMMUNITY_TYPE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("BBSShowPlayerList"))
			BBS_SHOW_PLAYERLIST = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("BBSDefault"))
			BBS_DEFAULT = pValue;
		else if (pName.equalsIgnoreCase("ShowLevelOnCommunityBoard"))
			SHOW_LEVEL_COMMUNITYBOARD = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("ShowStatusOnCommunityBoard"))
			SHOW_STATUS_COMMUNITYBOARD = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("NamePageSizeOnCommunityBoard"))
			NAME_PAGE_SIZE_COMMUNITYBOARD = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("NamePerRowOnCommunityBoard"))
			NAME_PER_ROW_COMMUNITYBOARD = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ShowServerNews"))
			SERVER_NEWS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("ShowNpcLevel"))
			SHOW_NPC_LVL = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("ForceInventoryUpdate"))
			FORCE_INVENTORY_UPDATE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AutoDeleteInvalidQuestData"))
			AUTODELETE_INVALID_QUEST_DATA = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("MaximumOnlineUsers"))
			MAXIMUM_ONLINE_USERS = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ZoneTown"))
			ZONE_TOWN = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("CheckKnownList"))
			CHECK_KNOWN = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("UseDeepBlueDropRules"))
			DEEPBLUE_DROP_RULES = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AllowGuards"))
			GUARD_ATTACK_AGGRO_MOB = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("CancelLesserEffect"))
			EFFECT_CANCELING = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("MaximumSlotsForNoDwarf"))
			INVENTORY_MAXIMUM_NO_DWARF = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("MaximumSlotsForDwarf"))
			INVENTORY_MAXIMUM_DWARF = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("MaximumSlotsForGMPlayer"))
			INVENTORY_MAXIMUM_GM = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("MaximumWarehouseSlotsForNoDwarf"))
			WAREHOUSE_SLOTS_NO_DWARF = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("MaximumWarehouseSlotsForDwarf"))
			WAREHOUSE_SLOTS_DWARF = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("MaximumWarehouseSlotsForClan"))
			WAREHOUSE_SLOTS_CLAN = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("MaximumFreightSlots"))
			FREIGHT_SLOTS = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("EnchantChanceWeapon"))
			ENCHANT_CHANCE_WEAPON = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("EnchantChanceArmor"))
			ENCHANT_CHANCE_ARMOR = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("EnchantChanceJewelry"))
			ENCHANT_CHANCE_JEWELRY = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("EnchantMaxWeapon"))
			ENCHANT_MAX_WEAPON = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("EnchantMaxArmor"))
			ENCHANT_MAX_ARMOR = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("EnchantMaxJewelry"))
			ENCHANT_MAX_JEWELRY = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("EnchantSafeMax"))
			ENCHANT_SAFE_MAX = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("EnchantSafeMaxFull"))
			ENCHANT_SAFE_MAX_FULL = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AugmentationNGSkillChance"))
			AUGMENTATION_NG_SKILL_CHANCE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AugmentationNGGlowChance"))
			AUGMENTATION_NG_GLOW_CHANCE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AugmentationMidSkillChance"))
			AUGMENTATION_MID_SKILL_CHANCE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AugmentationMidGlowChance"))
			AUGMENTATION_MID_GLOW_CHANCE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AugmentationHighSkillChance"))
			AUGMENTATION_HIGH_SKILL_CHANCE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AugmentationHighGlowChance"))
			AUGMENTATION_HIGH_GLOW_CHANCE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AugmentationTopSkillChance"))
			AUGMENTATION_TOP_SKILL_CHANCE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AugmentationTopGlowChance"))
			AUGMENTATION_TOP_GLOW_CHANCE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AugmentationBaseStatChance"))
			AUGMENTATION_BASESTAT_CHANCE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("HpRegenMultiplier"))
			HP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
		else if (pName.equalsIgnoreCase("MpRegenMultiplier"))
			MP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
		else if (pName.equalsIgnoreCase("CpRegenMultiplier"))
			CP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
		else if (pName.equalsIgnoreCase("RaidHpRegenMultiplier"))
			RAID_HP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
		else if (pName.equalsIgnoreCase("RaidMpRegenMultiplier"))
			RAID_MP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
		else if (pName.equalsIgnoreCase("RaidPDefenceMultiplier"))
			RAID_PDEFENCE_MULTIPLIER = Double.parseDouble(pValue) / 100;
		else if (pName.equalsIgnoreCase("RaidMDefenceMultiplier"))
			RAID_MDEFENCE_MULTIPLIER = Double.parseDouble(pValue) / 100;
		else if (pName.equalsIgnoreCase("RaidMinionRespawnTime"))
			RAID_MINION_RESPAWN_TIMER = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("StartingAdena"))
			STARTING_ADENA = Long.parseLong(pValue);
		else if (pName.equalsIgnoreCase("UnstuckInterval"))
			UNSTUCK_INTERVAL = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("PlayerSpawnProtection"))
			PLAYER_SPAWN_PROTECTION = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("PlayerFakeDeathUpProtection"))
			PLAYER_FAKEDEATH_UP_PROTECTION = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("RestorePlayerInstance"))
			RESTORE_PLAYER_INSTANCE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AllowSummonToInstance"))
			ALLOW_SUMMON_TO_INSTANCE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("PartyXpCutoffMethod"))
			PARTY_XP_CUTOFF_METHOD = pValue;
		else if (pName.equalsIgnoreCase("PartyXpCutoffPercent"))
			PARTY_XP_CUTOFF_PERCENT = Double.parseDouble(pValue);
		else if (pName.equalsIgnoreCase("PartyXpCutoffLevel"))
			PARTY_XP_CUTOFF_LEVEL = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("RespawnRestoreCP"))
			RESPAWN_RESTORE_CP = Double.parseDouble(pValue) / 100;
		else if (pName.equalsIgnoreCase("RespawnRestoreHP"))
			RESPAWN_RESTORE_HP = Double.parseDouble(pValue) / 100;
		else if (pName.equalsIgnoreCase("RespawnRestoreMP"))
			RESPAWN_RESTORE_MP = Double.parseDouble(pValue) / 100;
		else if (pName.equalsIgnoreCase("MaxPvtStoreSellSlotsDwarf"))
			MAX_PVTSTORESELL_SLOTS_DWARF = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("MaxPvtStoreSellSlotsOther"))
			MAX_PVTSTORESELL_SLOTS_OTHER = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("MaxPvtStoreBuySlotsDwarf"))
			MAX_PVTSTOREBUY_SLOTS_DWARF = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("MaxPvtStoreBuySlotsOther"))
			MAX_PVTSTOREBUY_SLOTS_OTHER = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("StoreSkillCooltime"))
			STORE_SKILL_COOLTIME = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("SubclassStoreSkillCooltime"))
			SUBCLASS_STORE_SKILL_COOLTIME = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AnnounceMammonSpawn"))
			ANNOUNCE_MAMMON_SPAWN = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltGameTiredness"))
			ALT_GAME_TIREDNESS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltGameCreation"))
			ALT_GAME_CREATION = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltGameCreationSpeed"))
			ALT_GAME_CREATION_SPEED = Double.parseDouble(pValue);
		else if (pName.equalsIgnoreCase("AltGameCreationXpRate"))
			ALT_GAME_CREATION_XP_RATE = Double.parseDouble(pValue);
		else if (pName.equalsIgnoreCase("AltGameCreationRareXpSpRate"))
			ALT_GAME_CREATION_RARE_XPSP_RATE = Double.parseDouble(pValue);
		else if (pName.equalsIgnoreCase("AltGameCreationSpRate"))
			ALT_GAME_CREATION_SP_RATE = Double.parseDouble(pValue);
		else if (pName.equalsIgnoreCase("AltWeightLimit"))
			ALT_WEIGHT_LIMIT = Double.parseDouble(pValue);
		else if (pName.equalsIgnoreCase("AltBlacksmithUseRecipes"))
			ALT_BLACKSMITH_USE_RECIPES = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltGameSkillLearn"))
			ALT_GAME_SKILL_LEARN = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("RemoveCastleCirclets"))
			REMOVE_CASTLE_CIRCLETS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("ReputationScorePerKill"))
			REPUTATION_SCORE_PER_KILL = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AltGameCancelByHit"))
		{
			ALT_GAME_CANCEL_BOW = pValue.equalsIgnoreCase("bow") || pValue.equalsIgnoreCase("all");
			ALT_GAME_CANCEL_CAST = pValue.equalsIgnoreCase("cast") || pValue.equalsIgnoreCase("all");
		}
		else if (pName.equalsIgnoreCase("AltShieldBlocks"))
			ALT_GAME_SHIELD_BLOCKS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltPerfectShieldBlockRate"))
			ALT_PERFECT_SHLD_BLOCK = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("Delevel"))
			ALT_GAME_DELEVEL = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("MagicFailures"))
			ALT_GAME_MAGICFAILURES = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltMobAgroInPeaceZone"))
			ALT_MOB_AGRO_IN_PEACEZONE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltGameExponentXp"))
			ALT_GAME_EXPONENT_XP = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("AltGameExponentSp"))
			ALT_GAME_EXPONENT_SP = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("AllowClassMasters"))
			ALLOW_CLASS_MASTERS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltGameFreights"))
			ALT_GAME_FREIGHTS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltGameFreightPrice"))
			ALT_GAME_FREIGHT_PRICE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AltPartyRange"))
			ALT_PARTY_RANGE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AltPartyRange2"))
			ALT_PARTY_RANGE2 = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("CraftingEnabled"))
			IS_CRAFTING_ENABLED = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("CraftMasterwork"))
			CRAFT_MASTERWORK = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("LifeCrystalNeeded"))
			LIFE_CRYSTAL_NEEDED = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("SpBookNeeded"))
			SP_BOOK_NEEDED = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AutoLoot"))
			AUTO_LOOT = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AutoLootRaids"))
			AUTO_LOOT_RAIDS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AutoLootHerbs"))
			AUTO_LOOT_HERBS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltKarmaPlayerCanBeKilledInPeaceZone"))
			ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltKarmaPlayerCanShop"))
			ALT_GAME_KARMA_PLAYER_CAN_SHOP = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltKarmaPlayerCanUseGK"))
			ALT_GAME_KARMA_PLAYER_CAN_USE_GK = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltKarmaPlayerCanTeleport"))
			ALT_GAME_KARMA_PLAYER_CAN_TELEPORT = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltKarmaPlayerCanTrade"))
			ALT_GAME_KARMA_PLAYER_CAN_TRADE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltKarmaPlayerCanUseWareHouse"))
			ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("MaxPersonalFamePoints"))
			MAX_PERSONAL_FAME_POINTS = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("FortressZoneFameTaskFrequency"))
			FORTRESS_ZONE_FAME_TASK_FREQUENCY = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("FortressZoneFameAquirePoints"))
			FORTRESS_ZONE_FAME_AQUIRE_POINTS = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("CastleZoneFameTaskFrequency"))
			CASTLE_ZONE_FAME_TASK_FREQUENCY = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("CastleZoneFameAquirePoints"))
			CASTLE_ZONE_FAME_AQUIRE_POINTS = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AltCastleForDawn"))
			ALT_GAME_CASTLE_DAWN = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltCastleForDusk"))
			ALT_GAME_CASTLE_DUSK = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltRequireClanCastle"))
			ALT_GAME_REQUIRE_CLAN_CASTLE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltFreeTeleporting"))
			ALT_GAME_FREE_TELEPORT = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltSubClassWithoutQuests"))
			ALT_GAME_SUBCLASS_WITHOUT_QUESTS = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AltMembersCanWithdrawFromClanWH"))
			ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("DwarfRecipeLimit"))
			DWARF_RECIPE_LIMIT = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("CommonRecipeLimit"))
			COMMON_RECIPE_LIMIT = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ChampionEnable"))
			L2JMOD_CHAMPION_ENABLE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("ChampionFrequency"))
			L2JMOD_CHAMPION_FREQUENCY = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ChampionMinLevel"))
			L2JMOD_CHAMP_MIN_LVL = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ChampionMaxLevel"))
			L2JMOD_CHAMP_MAX_LVL = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ChampionHp"))
			L2JMOD_CHAMPION_HP = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ChampionHpRegen"))
			L2JMOD_CHAMPION_HP_REGEN = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("ChampionRewards"))
			L2JMOD_CHAMPION_REWARDS = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ChampionAdenasRewards"))
			L2JMOD_CHAMPION_ADENAS_REWARDS = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("ChampionAtk"))
			L2JMOD_CHAMPION_ATK = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("ChampionSpdAtk"))
			L2JMOD_CHAMPION_SPD_ATK = Float.parseFloat(pValue);
		else if (pName.equalsIgnoreCase("ChampionRewardLowerLvlItemChance"))
			L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ChampionRewardHigherLvlItemChance"))
			L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ChampionRewardItemID"))
			L2JMOD_CHAMPION_REWARD_ID = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("ChampionRewardItemQty"))
			L2JMOD_CHAMPION_REWARD_QTY = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("AllowWedding"))
			L2JMOD_ALLOW_WEDDING = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("WeddingPrice"))
			L2JMOD_WEDDING_PRICE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("WeddingPunishInfidelity"))
			L2JMOD_WEDDING_PUNISH_INFIDELITY = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("WeddingTeleport"))
			L2JMOD_WEDDING_TELEPORT = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("WeddingTeleportPrice"))
			L2JMOD_WEDDING_TELEPORT_PRICE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("WeddingTeleportDuration"))
			L2JMOD_WEDDING_TELEPORT_DURATION = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("WeddingAllowSameSex"))
			L2JMOD_WEDDING_SAMESEX = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("WeddingFormalWear"))
			L2JMOD_WEDDING_FORMALWEAR = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("WeddingDivorceCosts"))
			L2JMOD_WEDDING_DIVORCE_COSTS = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("TvTEventEnabled"))
			TVT_EVENT_ENABLED = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("TvTEventInterval"))
			TVT_EVENT_INTERVAL = pValue.split(",");
		else if (pName.equalsIgnoreCase("TvTEventParticipationTime"))
			TVT_EVENT_PARTICIPATION_TIME = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("TvTEventRunningTime"))
			TVT_EVENT_RUNNING_TIME = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("TvTEventParticipationNpcId"))
			TVT_EVENT_PARTICIPATION_NPC_ID = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("EnableWarehouseSortingClan"))
			L2JMOD_ENABLE_WAREHOUSESORTING_CLAN = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("EnableWarehouseSortingPrivate"))
			L2JMOD_ENABLE_WAREHOUSESORTING_PRIVATE = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("EnableWarehouseSortingFreight"))
			L2JMOD_ENABLE_WAREHOUSESORTING_FREIGHT = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("EnableManaPotionSupport"))
			L2JMOD_ENABLE_MANA_POTIONS_SUPPORT = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("MinKarma"))
			KARMA_MIN_KARMA = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("MaxKarma"))
			KARMA_MAX_KARMA = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("XPDivider"))
			KARMA_XP_DIVIDER = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("BaseKarmaLost"))
			KARMA_LOST_BASE = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("CanGMDropEquipment"))
			KARMA_DROP_GM = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("AwardPKKillPVPPoint"))
			KARMA_AWARD_PK_KILL = Boolean.parseBoolean(pValue);
		else if (pName.equalsIgnoreCase("MinimumPKRequiredToDrop"))
			KARMA_PK_LIMIT = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("PvPVsNormalTime"))
			PVP_NORMAL_TIME = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("PvPVsPvPTime"))
			PVP_PVP_TIME = Integer.parseInt(pValue);
		else if (pName.equalsIgnoreCase("GlobalChat"))
			DEFAULT_GLOBAL_CHAT = pValue;
		else if (pName.equalsIgnoreCase("TradeChat"))
			DEFAULT_TRADE_CHAT = pValue;
		else if (pName.equalsIgnoreCase("GMAdminMenuStyle"))
			GM_ADMIN_MENU_STYLE = pValue;
		else
			return false;
		return true;
	}
	
	private Config()
	{}
	
	/**
	 * Save hexadecimal ID of the server in the L2Properties file.
	 * 
	 * @param string
	 *            (String) : hexadecimal ID of the server to store
	 * @see HEXID_FILE
	 * @see saveHexid(String string, String fileName)
	 * @link LoginServerThread
	 */
	public static void saveHexid(int serverId, String string)
	{
		Config.saveHexid(serverId, string, HEXID_FILE);
	}
	
	/**
	 * Save hexadecimal ID of the server in the properties file.
	 * 
	 * @param hexId
	 *            (String) : hexadecimal ID of the server to store
	 * @param fileName
	 *            (String) : name of the L2Properties file
	 */
	public static void saveHexid(int serverId, String hexId, String fileName)
	{
		try
		{
			L2Properties hexSetting = new L2Properties();
			File file = new File(fileName);
			// Create a new empty file only if it doesn't exist
			file.createNewFile();
			OutputStream out = new FileOutputStream(file);
			hexSetting.setProperty("ServerID", String.valueOf(serverId));
			hexSetting.setProperty("HexID", hexId);
			hexSetting.store(out, "the hexID to auth into login");
			out.close();
		}
		catch (Exception e)
		{
			_log.warning(StringUtil.concat("Failed to save hex id to ", fileName, " File."));
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads flood protector configurations.
	 * 
	 * @param properties
	 *            properties file reader
	 */
	private static void loadFloodProtectorConfigs(final L2Properties properties)
	{
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_USE_ITEM, "UseItem", "4");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_ROLL_DICE, "RollDice", "42");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_FIREWORK, "Firework", "42");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_ITEM_PET_SUMMON, "ItemPetSummon", "16");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_HERO_VOICE, "HeroVoice", "100");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_SHOUT, "Shout", "1000");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_TRADE_CHAT, "TradeChat", "100000");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_PARTY_ROOM, "PartyRoom", "3000");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_PARTY_ROOM_COMMANDER, "PartyRoomCommander", "300");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_SUBCLASS, "Subclass", "20");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_DROP_ITEM, "DropItem", "10");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_SERVER_BYPASS, "ServerBypass", "5");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_MULTISELL, "MultiSell", "1");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_TRANSACTION, "Transaction", "10");
	}
	
	/**
	 * Loads single flood protector configuration.
	 * 
	 * @param properties
	 *            properties file reader
	 * @param config
	 *            flood protector configuration instance
	 * @param configString
	 *            flood protector configuration string that determines for which flood protector
	 *            configuration should be read
	 * @param defaultInterval
	 *            default flood protector interval
	 */
	private static void loadFloodProtectorConfig(final L2Properties properties, final FloodProtectorConfig config, final String configString, final String defaultInterval)
	{
		config.FLOOD_PROTECTION_INTERVAL = Integer.parseInt(properties.getProperty(StringUtil.concat("FloodProtector", configString, "Interval"), defaultInterval));
		config.LOG_FLOODING = Boolean.parseBoolean(properties.getProperty(StringUtil.concat("FloodProtector", configString, "LogFlooding"), "False"));
		config.PUNISHMENT_LIMIT = Integer.parseInt(properties.getProperty(StringUtil.concat("FloodProtector", configString, "PunishmentLimit"), "0"));
		config.PUNISHMENT_TYPE = properties.getProperty(StringUtil.concat("FloodProtector", configString, "PunishmentType"), "none");
		config.PUNISHMENT_TIME = Integer.parseInt(properties.getProperty(StringUtil.concat("FloodProtector", configString, "PunishmentTime"), "0"));
	}
}