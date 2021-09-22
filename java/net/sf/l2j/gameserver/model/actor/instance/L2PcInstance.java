package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import Alpha.skillGuard.StuckSubGuard;
import ghosts.model.Ghost;
import inertia.controller.InertiaController;
import inertia.model.IInertiaBehave;
import inertia.model.behave.PlayerBehave;
import javolution.util.FastList;
import javolution.util.FastMap;
import luna.PassportManager;
import luna.PlayerPassport;
import luna.mysql;
import luna.custom.LunaVariables;
import luna.custom.eventengine.LunaEvent;
import luna.custom.eventengine.enums.EventState;
import luna.custom.eventengine.enums.TeamType;
import luna.custom.eventengine.events.LunaTvT;
import luna.custom.eventengine.managers.EventManager;
import luna.custom.handler.UpdateLunaDetailStats;
import luna.custom.holder.LunaGlobalVariablesHolder;
import luna.custom.logger.LunaLogger;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.ItemsAutoDestroy;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.RecipeController;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.Universe;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2PlayerAI;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.cache.WarehouseCacheManager;
import net.sf.l2j.gameserver.communitybbs.BB.Forum;
import net.sf.l2j.gameserver.communitybbs.Manager.ForumsBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.BBSSchemeBufferInstance;
import net.sf.l2j.gameserver.datatables.AccessLevels;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.CharactersTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.FakePcsTable;
import net.sf.l2j.gameserver.datatables.FishTable;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.datatables.HennaTreeTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NobleSkillTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.itemhandlers.Gem;
import net.sf.l2j.gameserver.handler.itemhandlers.ItemSkills;
import net.sf.l2j.gameserver.handler.itemhandlers.PetFood;
import net.sf.l2j.gameserver.handler.itemhandlers.Potions;
import net.sf.l2j.gameserver.handler.itemhandlers.RollingDice;
import net.sf.l2j.gameserver.handler.itemhandlers.ScrollOfResurrection;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.CharEffectList;
import net.sf.l2j.gameserver.model.CursedWeapon;
import net.sf.l2j.gameserver.model.Elementals;
import net.sf.l2j.gameserver.model.FishData;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2AccessLevel;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Fishing;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Macro;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2PetData;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.gameserver.model.L2Radar;
import net.sf.l2j.gameserver.model.L2RecipeList;
import net.sf.l2j.gameserver.model.L2Request;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.L2Transformation;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.MacroList;
import net.sf.l2j.gameserver.model.PlayerVar;
import net.sf.l2j.gameserver.model.ShortCuts;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.FakePc;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.knownlist.PcKnownList;
import net.sf.l2j.gameserver.model.actor.position.PcPosition;
import net.sf.l2j.gameserver.model.actor.stat.PcStat;
import net.sf.l2j.gameserver.model.actor.status.PcStatus;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.ClassLevel;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.base.PlayerClass;
import net.sf.l2j.gameserver.model.base.PlayerCounters;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.base.SubClass;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.entity.events.RaidEvent;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.Domination;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.LastManStanding;
import net.sf.l2j.gameserver.model.events.LastTeamStanding;
import net.sf.l2j.gameserver.model.events.LastTeamStanding.LastTeamStandingTeam;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import net.sf.l2j.gameserver.model.events.Zombie;
import net.sf.l2j.gameserver.model.events.manager.EventVarHolder;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.model.itemcontainer.PcFreight;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.itemcontainer.PcWarehouse;
import net.sf.l2j.gameserver.model.itemcontainer.PetInventory;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.Quest.QuestEventType;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ChangeWaitType;
import net.sf.l2j.gameserver.network.serverpackets.CharInfo;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExFishingEnd;
import net.sf.l2j.gameserver.network.serverpackets.ExFishingStart;
import net.sf.l2j.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import net.sf.l2j.gameserver.network.serverpackets.ExGetOnAirShip;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadMode;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import net.sf.l2j.gameserver.network.serverpackets.ExSetCompassZoneCode;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExSpawnEmitter;
import net.sf.l2j.gameserver.network.serverpackets.ExStartScenePlayer;
import net.sf.l2j.gameserver.network.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.network.serverpackets.ExVitalityPointInfo;
import net.sf.l2j.gameserver.network.serverpackets.FriendList;
import net.sf.l2j.gameserver.network.serverpackets.GMHide;
import net.sf.l2j.gameserver.network.serverpackets.GameGuardQuery;
import net.sf.l2j.gameserver.network.serverpackets.GetOnVehicle;
import net.sf.l2j.gameserver.network.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.LeaveWorld;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NicknameChanged;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ObservationMode;
import net.sf.l2j.gameserver.network.serverpackets.ObservationReturn;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PetInventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreListBuy;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreListSell;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreManageListSell;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreMsgSell;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopMsg;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopSellList;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.network.serverpackets.Ride;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.ShortBuffStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutRegister;
import net.sf.l2j.gameserver.network.serverpackets.ShowCalculator;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SkillList;
import net.sf.l2j.gameserver.network.serverpackets.Snoop;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.TargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.TargetUnselected;
import net.sf.l2j.gameserver.network.serverpackets.TradeDone;
import net.sf.l2j.gameserver.network.serverpackets.TradeOtherDone;
import net.sf.l2j.gameserver.network.serverpackets.TradeStart;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.effects.EffectImmobileAutoAttack;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSiegeFlag;
import net.sf.l2j.gameserver.templates.chars.L2PcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Armor;
import net.sf.l2j.gameserver.templates.item.L2ArmorType;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;
import net.sf.l2j.gameserver.templates.item.L2Henna;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.GMAudit;
import net.sf.l2j.gameserver.util.Strings;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Point3D;
import net.sf.l2j.util.Rnd;

/**
 * This class represents all player characters in the world.
 * There is always a client-thread connected to this (except if a player-store is activated upon logout).<BR>
 * <BR>
 *
 * @version $Revision: 1.66.2.41.2.33 $ $Date: 2005/04/11 10:06:09 $
 */
public class L2PcInstance extends L2Playable
{
	private final Map<String, Object>	quickVars								= new ConcurrentHashMap<>();
	private boolean _isTrying = false;
	private static final int[]			TRANSFORMATION_ALLOWED_SKILLS			=
	{
		3, 8, 9, 10, 18, 22, 28, 33, 65, 67, 78, 86, 98, 110, 144, 190, 196, 197, 223, 278, 279, 283, 912, 9009, 289, 293, 320, 361, 362, 400, 401, 402, 403, 404, 406, 407, 437, 440, 449, 494, 539, 540, 559, 560, 561, 562, 563, 564, 565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 579, 580, 581, 582, 583, 584, 585, 586, 587, 588, 589, 619, 629, 630, 675, 676, 677, 678, 679, 680, 681, 682, 683, 684, 685, 686, 687, 688, 689, 690, 691, 692, 693, 694, 695, 696, 697, 698, 699, 700, 701, 702, 703, 704, 705, 706, 707, 708, 709, 710, 711, 712, 713, 714, 715, 716, 717, 718, 719, 720, 721, 722, 723, 724, 725, 726, 727, 728, 729, 730, 731, 732, 733, 734, 735, 736, 737, 738, 739, 740, 741, 745, 746, 747, 748, 749, 750, 751, 752, 753, 754, 795, 796, 797, 798, 814, 815, 816, 817, 838, 839, 884, 885, 886, 887, 888, 891, 896, 897, 898, 899, 900, 901, 902, 903, 904, 905, 906, 907, 908, 909, 910, 911, 1015, 1016, 1018, 1027, 1028, 1034, 1042, 1043, 1201, 1206, 1217, 1264, 1266,
		1360, 1361, 1400, 1409, 1411, 1417, 1418, 1430, 1303, 1059, 1471, 1472, 1342, 1343, 1508, 1514, 1515, 1523, 1524, 1525, 1528, 1532, 1533, 3328, 3329, 3330, 3331, 3630, 3631, 5437, 5491, 5655, 5656, 5657, 5658, 5659, 8248, 9011, 9012, 16500, 16501, 16502, 16503, 30040
	};
	private static final int[]			SUMMONER_TRANSFORMATION_ALLOWED_SKILLS	=
	{
		896, 897, 898, 899, 900, 38051, 928, 30, 821, 446, 1477, 1514, 38052, 420, 1478, 35, 346, 10026, 38050, 1235, 1236, 4, 1479
	};
	private static final int[]			PATH_SKILLS								= new int[]
	{
		9421, 9422, 9423, 9424, 9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433, 9434, 9435, 9436, 9437, 9438, 9439, 9440, 9441, 9442, 9443, 9444, 9445, 9446, 9447, 9448, 9449, 9450, 9451, 9452, 9453, 9454, 9455, 9456, 9457, 9458, 9459, 9460, 9461, 9462, 9463, 9464, 9465, 9466, 9467, 9468, 9469, 9470, 9471, 9472, 9473, 9474, 9475, 9476, 9477, 9478, 9479, 9480, 9481, 9482, 9483, 9484, 9485, 9486, 9487, 9488, 9489, 9490, 9491, 9492, 9493, 9494, 9495, 9496, 9497, 9498, 9499, 9500, 9501, 9502, 9503, 9504, 94270, 94271, 94300, 94302, 94350, 94351, 94390, 94391, 94501, 94504, 94510, 94512, 94570, 94571, 94600, 94603, 94710, 94711, 94740, 94741, 94770, 94772, 94830, 94832, 94920, 94922, 94950, 94952, 95010, 95012, 95040, 95041
	};
	private static final int[]			CERTIFICATION_SKILLS					= new int[]
	{
		35200, 35202, 35204, 35206, 35208, 35210, 35212, 35214, 35216, 35218, 35220, 35222, 35224, 35226, 35228, 35230, 35232, 35234, 35236, 35238, 35240, 35242, 35244, 35246, 35248, 35250, 35252, 35254, 35256, 35258, 35260, 35262, 35264
	};
	private static final int[]			PATH_SKILLS_OFFENSIVE					= new int[]
	{
		9421, 9422, 9423, 9424, 9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433, 9434, 9435, 9436, 9437, 9438, 9439, 9440, 9441, 94270, 94271, 94300, 94302, 94350, 94351, 94390, 94391
	};
	private static final int[]			PATH_SKILLS_DEF							= new int[]
	{
		9463, 9464, 9465, 9466, 9467, 9468, 9469, 9470, 9471, 9472, 9473, 9474, 9475, 9476, 9477, 9478, 9479, 9480, 9481, 9482, 9483, 94710, 94711, 94740, 94741, 94770, 94772, 94830, 94832
	};
	private static final int[]			PATH_SKILLS_MAGE						= new int[]
	{
		9442, 9443, 9444, 9445, 9446, 9447, 9448, 9449, 9450, 9451, 9452, 9453, 9454, 9455, 9456, 9457, 9458, 9459, 9460, 9461, 9462, 94501, 94504, 94510, 94512, 94570, 94571, 94600, 94603
	};
	private static final int[]			PATH_SKILLS_SUP							= new int[]
	{
		9484, 9485, 9486, 9487, 9488, 9489, 9490, 9491, 9492, 9493, 9494, 9495, 9496, 9497, 9498, 9499, 9500, 9501, 9502, 9503, 9504, 94920, 94922, 94950, 94952, 95010, 95012, 95040, 95041
	};
	public static final int[]			HERO_SKILLS								=
	{
		395, 396, 1374, 1375, 1376, 12504, 12503, 12502, 12505, 12501, 12506, 12511, 12509, 12508, 12510
	};
	public static final int				NEWBIE_LEVEL							= 76;
	// Character Skill SQL String Definitions:
	private static final String			RESTORE_SKILLS_FOR_CHAR					= "SELECT skill_id,skill_level FROM character_skills WHERE charId=? AND class_index=?";
	private static final String			ADD_NEW_SKILL							= "REPLACE INTO character_skills (charId,skill_id,skill_level,skill_name,class_index) VALUES (?,?,?,?,?)";
	private static final String			UPDATE_CHARACTER_SKILL_LEVEL			= "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String			DELETE_SKILL_FROM_CHAR					= "DELETE FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String			DELETE_CHAR_SKILLS						= "DELETE FROM character_skills WHERE charId=? AND class_index=?";
	// Character Skill Save SQL String Definitions:
	private static final String			ADD_SKILL_SAVE							= "REPLACE INTO character_skills_save (charId,skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private static final String			RESTORE_SKILL_SAVE						= "SELECT skill_id,skill_level,effect_count,effect_cur_time, reuse_delay, systime FROM character_skills_save WHERE charId=? AND class_index=? AND restore_type=? ORDER BY buff_index ASC";
	private static final String			DELETE_SKILL_SAVE						= "DELETE FROM character_skills_save WHERE charId=? AND class_index=?";
	// Character Character SQL String Definitions:
	private static final String			INSERT_CHARACTER						= "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,fame,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,newbie,nobless,power_grade,last_recom_date,event_kills,raid_kills,siege_kills,olympiad_wins,cp_points) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String			UPDATE_CHARACTER						= "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,fame=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,clanid=?,race=?,classid=?,deletetime=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,punish_level=?,punish_timer=?,newbie=?,nobless=?,power_grade=?,subpledge=?,last_recom_date=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,death_penalty_level=?,bookmarkslot=?,streak=?,lastKill1=?,lastKill2=?,vitality_points=?,heroWpnDel=?,cancraft=?,nameC=?,titleC=?,event_kills=?,raid_kills=?,siege_kills=?,olympiad_wins=?,cp_points=? WHERE charId=?";
	private static final String			RESTORE_CHARACTER						= "SELECT account_name, charId, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, face, hairStyle, hairColor, sex, heading, x, y, z, exp, expBeforeDeath, sp, karma, fame, pvpkills, pkkills, clanid, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon, punish_level, punish_timer, newbie, nobless, power_grade, subpledge, last_recom_date, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally,clan_join_expiry_time,clan_create_expiry_time,death_penalty_level,bookmarkslot,streak,lastKill1,lastKill2,vitality_points,heroWpnDel,nameC,titleC,event_kills,raid_kills,siege_kills,olympiad_wins,cp_points FROM characters WHERE charId=?";
	// Character Class Path
	private static final String			INSERT_CLASSPATHS						= "INSERT INTO class_paths VALUES (?,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);";
	// Character Teleport Bookmark:
	private static final String			INSERT_TP_BOOKMARK						= "INSERT INTO character_tpbookmark (charId,Id,x,y,z,icon,tag,name) values (?,?,?,?,?,?,?,?)";
	private static final String			UPDATE_TP_BOOKMARK						= "UPDATE character_tpbookmark SET icon=?,tag=?,name=? where charId=? AND Id=?";
	private static final String			RESTORE_TP_BOOKMARK						= "SELECT Id,x,y,z,icon,tag,name FROM character_tpbookmark WHERE charId=?";
	private static final String			DELETE_TP_BOOKMARK						= "DELETE FROM character_tpbookmark WHERE charId=? AND Id=?";
	// Character Subclass SQL String Definitions:
	private static final String			RESTORE_CHAR_SUBCLASSES					= "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE charId=? ORDER BY class_index ASC";
	private static final String			ADD_CHAR_SUBCLASS						= "INSERT INTO character_subclasses (charId,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
	private static final String			UPDATE_CHAR_SUBCLASS					= "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE charId=? AND class_index =?";
	private static final String			DELETE_CHAR_SUBCLASS					= "DELETE FROM character_subclasses WHERE charId=? AND class_index=?";
	// Character Henna SQL String Definitions:
	private static final String			RESTORE_CHAR_HENNAS						= "SELECT slot,symbol_id FROM character_hennas WHERE charId=? AND class_index=?";
	private static final String			ADD_CHAR_HENNA							= "INSERT INTO character_hennas (charId,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String			DELETE_CHAR_HENNA						= "DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?";
	private static final String			DELETE_CHAR_HENNAS						= "DELETE FROM character_hennas WHERE charId=? AND class_index=?";
	// Character Shortcut SQL String Definitions:
	private static final String			DELETE_CHAR_SHORTCUTS					= "DELETE FROM character_shortcuts WHERE charId=? AND class_index=?";
	// Character Recommendation SQL String Definitions:
	private static final String			RESTORE_CHAR_RECOMS						= "SELECT charId,target_id FROM character_recommends WHERE charId=?";
	private static final String			ADD_CHAR_RECOM							= "INSERT INTO character_recommends (charId,target_id) VALUES (?,?)";
	/*
	 * private static final String DELETE_CHAR_RECOMS = "DELETE FROM character_recommends WHERE charId=?";
	 */
	// Character Transformation SQL String Definitions:
	private static final String			SELECT_CHAR_TRANSFORM					= "SELECT transform_id FROM characters WHERE charId=?";
	private static final String			UPDATE_CHAR_TRANSFORM					= "UPDATE characters SET transform_id=? WHERE charId=?";
	public static final int				REQUEST_TIMEOUT							= 15;
	public static final int				STORE_PRIVATE_NONE						= 0;
	public static final int				STORE_PRIVATE_SELL						= 1;
	public static final int				STORE_PRIVATE_BUY						= 3;
	public static final int				STORE_PRIVATE_MANUFACTURE				= 5;
	public static final int				STORE_PRIVATE_PACKAGE_SELL				= 8;
	private L2PcInstance				_lastKiller								= null;
	public boolean						canSendUserInfo							= false;
	private int							_lockdownTime							= 0;
	private long						_lastCaptchaTimeStamp;
	private String						_secretCode								= null;
	private String						_pinCode								= null;
	public String						_reason									= "";
	public int							EMBRYO_DAMAGE_DEALT;
	private long						_museumOnlineTime;
	private final PlayerPassport		_passport;
	static Map<Integer, Long>			_dressMeExpiryDates						= new FastMap<Integer, Long>();
	
	public static Map<Integer, Long> getDressMeExpiryDates()
	{
		return _dressMeExpiryDates;
	}
	
	public void refreshMuseumOnlineTime()
	{
		_museumOnlineTime = System.currentTimeMillis();
	}
	
	public void prevPath()
	{
		for (L2Skill skill : getAllSkills())
		{
			if (skill.getId() == 9421 || skill.getId() == 9422 || skill.getId() == 9423)
			{
				incMiddleOff();
				incCpPoints();
			}
			else if (skill.getId() == 9424 || skill.getId() == 9425 || skill.getId() == 9426)
			{
				incLeftOff();
				incCpPoints();
			}
			else if (skill.getId() == 9427 || skill.getId() == 9428 || skill.getId() == 9429)
			{
				incLeftOff1();
				incCpPoints();
			}
			else if (skill.getId() == 9430 || skill.getId() == 9431 || skill.getId() == 9432)
			{
				incLeftOff2();
				incCpPoints();
			}
			else if (skill.getId() == 9433 || skill.getId() == 9434 || skill.getId() == 9435)
			{
				incRightOff();
				incCpPoints();
			}
			else if (skill.getId() == 9436 || skill.getId() == 9437 || skill.getId() == 9438)
			{
				incRightOff1();
				incCpPoints();
			}
			else if (skill.getId() == 9439 || skill.getId() == 9440 || skill.getId() == 9441)
			{
				incRightOff2();
				incCpPoints();
			}
			else if (skill.getId() == 9442 || skill.getId() == 9443 || skill.getId() == 9444)
			{
				incMiddleMage();
				incCpPoints();
			}
			else if (skill.getId() == 9445 || skill.getId() == 9446 || skill.getId() == 9447)
			{
				incLeftMage();
				incCpPoints();
			}
			else if (skill.getId() == 9448 || skill.getId() == 9449 || skill.getId() == 9450)
			{
				incLeftMage1();
				incCpPoints();
			}
			else if (skill.getId() == 9451 || skill.getId() == 9452 || skill.getId() == 9453)
			{
				incLeftMage2();
				incCpPoints();
			}
			else if (skill.getId() == 9454 || skill.getId() == 9455 || skill.getId() == 9456)
			{
				incRightMage();
				incCpPoints();
			}
			else if (skill.getId() == 9457 || skill.getId() == 9458 || skill.getId() == 9459)
			{
				incRightMage1();
				incCpPoints();
			}
			else if (skill.getId() == 9460 || skill.getId() == 9461 || skill.getId() == 9462)
			{
				incRightMage2();
				incCpPoints();
			}
			else if (skill.getId() == 9463 || skill.getId() == 9464 || skill.getId() == 9465)
			{
				incMiddleDef();
				incCpPoints();
			}
			else if (skill.getId() == 9466 || skill.getId() == 9467 || skill.getId() == 9468)
			{
				incLeftDef();
				incCpPoints();
			}
			else if (skill.getId() == 9469 || skill.getId() == 9470 || skill.getId() == 9471)
			{
				incLeftDef1();
				incCpPoints();
			}
			else if (skill.getId() == 9472 || skill.getId() == 9473 || skill.getId() == 9474)
			{
				incLeftDef2();
				incCpPoints();
			}
			else if (skill.getId() == 9475 || skill.getId() == 9476 || skill.getId() == 9477)
			{
				incRightDef();
				incCpPoints();
			}
			else if (skill.getId() == 9478 || skill.getId() == 9479 || skill.getId() == 9480)
			{
				incRightDef1();
				incCpPoints();
			}
			else if (skill.getId() == 9481 || skill.getId() == 9482 || skill.getId() == 9483)
			{
				incRightDef2();
				incCpPoints();
			}
			else if (skill.getId() == 9484 || skill.getId() == 9485 || skill.getId() == 9486)
			{
				incMiddleSup();
				incCpPoints();
			}
			else if (skill.getId() == 9487 || skill.getId() == 9488 || skill.getId() == 9489)
			{
				incLeftSup();
				incCpPoints();
			}
			else if (skill.getId() == 9490 || skill.getId() == 9491 || skill.getId() == 9492)
			{
				incLeftSup1();
				incCpPoints();
			}
			else if (skill.getId() == 9493 || skill.getId() == 9494 || skill.getId() == 9495)
			{
				incLeftSup2();
				incCpPoints();
			}
			else if (skill.getId() == 9496 || skill.getId() == 9497 || skill.getId() == 9498)
			{
				incRightSup();
				incCpPoints();
			}
			else if (skill.getId() == 9499 || skill.getId() == 9500 || skill.getId() == 9501)
			{
				incRightSup1();
				incCpPoints();
			}
			else if (skill.getId() == 9502 || skill.getId() == 9503 || skill.getId() == 9504)
			{
				incRightSup2();
				incCpPoints();
			}
		}
	}
	
	public void cleanUpPathSkills()
	{
		for (L2Skill skill : getAllSkills())
		{
			for (int skills : PATH_SKILLS)
			{
				if (skill.getId() == skills)
					removeSkill(skill);
			}
		}
	}
	
	public void clearPath()
	{
		for (L2Skill skill : getAllSkills())
		{
			for (int skills : PATH_SKILLS)
			{
				if (skill.getId() == skills)
					removeSkill(skill);
			}
		}
		setCpPoints(0);
		setMiddleOff(0);
		setMiddleMage(0);
		setMiddleDef(0);
		setMiddleSup(0);
		setLeftOff(0);
		setLeftMage(0);
		setLeftDef(0);
		setLeftSup(0);
		setLeftOff1(0);
		setLeftMage1(0);
		setLeftDef1(0);
		setLeftSup1(0);
		setLeftOff2(0);
		setLeftMage2(0);
		setLeftDef2(0);
		setLeftSup2(0);
		setRightOff(0);
		setRightMage(0);
		setRightDef(0);
		setRightSup(0);
		setRightOff1(0);
		setRightMage1(0);
		setRightDef1(0);
		setRightSup1(0);
		setRightOff2(0);
		setRightMage2(0);
		setRightDef2(0);
		setRightSup2(0);
		setLeftOff1_1(0);
		setLeftOff1_2(0);
		setLeftOff2_1(0);
		setLeftOff2_2(0);
		setRightOff1_1(0);
		setRightOff1_2(0);
		setRightOff2_1(0);
		setRightOff2_2(0);
		setLeftMage1_1(0);
		setLeftMage1_2(0);
		setLeftMage2_1(0);
		setLeftMage2_2(0);
		setRightMage1_1(0);
		setRightMage1_2(0);
		setRightMage2_1(0);
		setRightMage2_2(0);
		setLeftDef1_1(0);
		setLeftDef1_2(0);
		setLeftDef2_1(0);
		setLeftDef2_2(0);
		setRightDef1_1(0);
		setRightDef1_2(0);
		setRightDef2_1(0);
		setRightDef2_2(0);
		setLeftSup1_1(0);
		setLeftSup1_2(0);
		setLeftSup2_1(0);
		setLeftSup2_2(0);
		setRightSup1_1(0);
		setRightSup1_2(0);
		setRightSup2_1(0);
		setRightSup2_2(0);
		incClassPaths("MiddleOff", 0, getObjectId());
		incClassPaths("LeftOff", 0, getObjectId());
		incClassPaths("LeftOff1", 0, getObjectId());
		incClassPaths("LeftOff2", 0, getObjectId());
		incClassPaths("RightOff", 0, getObjectId());
		incClassPaths("RightOff1", 0, getObjectId());
		incClassPaths("RightOff2", 0, getObjectId());
		incClassPaths("MiddleMage", 0, getObjectId());
		incClassPaths("LeftMage", 0, getObjectId());
		incClassPaths("LeftMage1", 0, getObjectId());
		incClassPaths("LeftMage2", 0, getObjectId());
		incClassPaths("RightMage", 0, getObjectId());
		incClassPaths("RightMage1", 0, getObjectId());
		incClassPaths("RightMage2", 0, getObjectId());
		incClassPaths("MiddleDef", 0, getObjectId());
		incClassPaths("LeftDef", 0, getObjectId());
		incClassPaths("LeftDef1", 0, getObjectId());
		incClassPaths("LeftDef2", 0, getObjectId());
		incClassPaths("RightDef", 0, getObjectId());
		incClassPaths("RightDef1", 0, getObjectId());
		incClassPaths("RightDef2", 0, getObjectId());
		incClassPaths("MiddleSup", 0, getObjectId());
		incClassPaths("LeftSup", 0, getObjectId());
		incClassPaths("LeftSup1", 0, getObjectId());
		incClassPaths("LeftSup2", 0, getObjectId());
		incClassPaths("RightSup", 0, getObjectId());
		incClassPaths("RightSup1", 0, getObjectId());
		incClassPaths("RightSup2", 0, getObjectId());
		incClassPaths("LeftOff1_1", 0, getObjectId());
		incClassPaths("LeftOff1_2", 0, getObjectId());
		incClassPaths("LeftOff2_1", 0, getObjectId());
		incClassPaths("LeftOff2_2", 0, getObjectId());
		incClassPaths("RightOff1_1", 0, getObjectId());
		incClassPaths("RightOff1_2", 0, getObjectId());
		incClassPaths("RightOff2_1", 0, getObjectId());
		incClassPaths("RightOff2_2", 0, getObjectId());
		incClassPaths("LeftMage1_1", 0, getObjectId());
		incClassPaths("LeftMage1_2", 0, getObjectId());
		incClassPaths("LeftMage2_1", 0, getObjectId());
		incClassPaths("LeftMage2_2", 0, getObjectId());
		incClassPaths("RightMage1_1", 0, getObjectId());
		incClassPaths("RightMage1_2", 0, getObjectId());
		incClassPaths("RightMage2_1", 0, getObjectId());
		incClassPaths("RightMage2_2", 0, getObjectId());
		incClassPaths("LeftDef1_1", 0, getObjectId());
		incClassPaths("LeftDef1_2", 0, getObjectId());
		incClassPaths("LeftDef2_1", 0, getObjectId());
		incClassPaths("LeftDef2_2", 0, getObjectId());
		incClassPaths("RightDef1_1", 0, getObjectId());
		incClassPaths("RightDef1_2", 0, getObjectId());
		incClassPaths("RightDef2_1", 0, getObjectId());
		incClassPaths("RightDef2_2", 0, getObjectId());
		incClassPaths("LeftSup1_1", 0, getObjectId());
		incClassPaths("LeftSup1_2", 0, getObjectId());
		incClassPaths("LeftSup2_1", 0, getObjectId());
		incClassPaths("LeftSup2_2", 0, getObjectId());
		incClassPaths("RightSup1_1", 0, getObjectId());
		incClassPaths("RightSup1_2", 0, getObjectId());
		incClassPaths("RightSup2_1", 0, getObjectId());
		incClassPaths("RightSup2_2", 0, getObjectId());
	}
	
	public void clearPathOffensive()
	{
		for (L2Skill skill : getAllSkills())
		{
			for (int skills : PATH_SKILLS_OFFENSIVE)
			{
				if (skill.getId() == skills)
				{
					removeSkill(skill);
					decCpPoints();
				}
			}
		}
		setMiddleOff(0);
		setLeftOff(0);
		setLeftOff1(0);
		setLeftOff1_1(0);
		setLeftOff1_2(0);
		setLeftOff2_1(0);
		setLeftOff2_2(0);
		setRightOff1_1(0);
		setRightOff1_2(0);
		setRightOff2_1(0);
		setRightOff2_2(0);
		setLeftOff2(0);
		setLeftOff2(0);
		setRightOff(0);
		setRightOff1(0);
		setRightOff2(0);
		incClassPaths("MiddleOff", 0, getObjectId());
		incClassPaths("LeftOff", 0, getObjectId());
		incClassPaths("LeftOff1", 0, getObjectId());
		incClassPaths("LeftOff2", 0, getObjectId());
		incClassPaths("RightOff", 0, getObjectId());
		incClassPaths("RightOff1", 0, getObjectId());
		incClassPaths("RightOff2", 0, getObjectId());
		incClassPaths("LeftOff1_1", 0, getObjectId());
		incClassPaths("LeftOff1_2", 0, getObjectId());
		incClassPaths("LeftOff2_1", 0, getObjectId());
		incClassPaths("LeftOff2_2", 0, getObjectId());
		incClassPaths("RightOff1_1", 0, getObjectId());
		incClassPaths("RightOff1_2", 0, getObjectId());
		incClassPaths("RightOff2_1", 0, getObjectId());
		incClassPaths("RightOff2_2", 0, getObjectId());
	}
	
	public void clearPathMage()
	{
		for (L2Skill skill : getAllSkills())
		{
			for (int skills : PATH_SKILLS_MAGE)
			{
				if (skill.getId() == skills)
				{
					removeSkill(skill);
					decCpPoints();
				}
			}
		}
		setMiddleMage(0);
		setLeftMage(0);
		setLeftMage1(0);
		setLeftMage2(0);
		setRightMage(0);
		setRightMage1(0);
		setRightMage2(0);
		setLeftMage1_1(0);
		setLeftMage1_2(0);
		setLeftMage2_1(0);
		setLeftMage2_2(0);
		setRightMage1_1(0);
		setRightMage1_2(0);
		setRightMage2_1(0);
		setRightMage2_2(0);
		incClassPaths("MiddleMage", 0, getObjectId());
		incClassPaths("LeftMage", 0, getObjectId());
		incClassPaths("LeftMage1", 0, getObjectId());
		incClassPaths("LeftMage2", 0, getObjectId());
		incClassPaths("RightMage", 0, getObjectId());
		incClassPaths("RightMage1", 0, getObjectId());
		incClassPaths("RightMage2", 0, getObjectId());
		incClassPaths("LeftMage1_1", 0, getObjectId());
		incClassPaths("LeftMage1_2", 0, getObjectId());
		incClassPaths("LeftMage2_1", 0, getObjectId());
		incClassPaths("LeftMage2_2", 0, getObjectId());
		incClassPaths("RightMage1_1", 0, getObjectId());
		incClassPaths("RightMage1_2", 0, getObjectId());
		incClassPaths("RightMage2_1", 0, getObjectId());
		incClassPaths("RightMage2_2", 0, getObjectId());
	}
	
	public void clearPathDef()
	{
		for (L2Skill skill : getAllSkills())
		{
			for (int skills : PATH_SKILLS_DEF)
			{
				if (skill.getId() == skills)
				{
					removeSkill(skill);
					decCpPoints();
				}
			}
		}
		setMiddleDef(0);
		setLeftDef(0);
		setLeftDef1(0);
		setLeftDef2(0);
		setRightDef(0);
		setRightDef1(0);
		setRightDef2(0);
		setLeftDef1_1(0);
		setLeftDef1_2(0);
		setLeftDef2_1(0);
		setLeftDef2_2(0);
		setRightDef1_1(0);
		setRightDef1_2(0);
		setRightDef2_1(0);
		setRightDef2_2(0);
		incClassPaths("MiddleDef", 0, getObjectId());
		incClassPaths("LeftDef", 0, getObjectId());
		incClassPaths("LeftDef1", 0, getObjectId());
		incClassPaths("LeftDef2", 0, getObjectId());
		incClassPaths("RightDef", 0, getObjectId());
		incClassPaths("RightDef1", 0, getObjectId());
		incClassPaths("RightDef2", 0, getObjectId());
		incClassPaths("LeftDef1_1", 0, getObjectId());
		incClassPaths("LeftDef1_2", 0, getObjectId());
		incClassPaths("LeftDef2_1", 0, getObjectId());
		incClassPaths("LeftDef2_2", 0, getObjectId());
		incClassPaths("RightDef1_1", 0, getObjectId());
		incClassPaths("RightDef1_2", 0, getObjectId());
		incClassPaths("RightDef2_1", 0, getObjectId());
		incClassPaths("RightDef2_2", 0, getObjectId());
	}
	
	public void clearPathSup()
	{
		for (L2Skill skill : getAllSkills())
		{
			for (int skills : PATH_SKILLS_SUP)
			{
				if (skill.getId() == skills)
				{
					removeSkill(skill);
					decCpPoints();
				}
			}
		}
		setMiddleSup(0);
		setLeftSup(0);
		setLeftSup1(0);
		setLeftSup2(0);
		setRightSup(0);
		setRightSup1(0);
		setRightSup2(0);
		setLeftSup1_1(0);
		setLeftSup1_2(0);
		setLeftSup2_1(0);
		setLeftSup2_2(0);
		setRightSup1_1(0);
		setRightSup1_2(0);
		setRightSup2_1(0);
		setRightSup2_2(0);
		incClassPaths("MiddleSup", 0, getObjectId());
		incClassPaths("LeftSup", 0, getObjectId());
		incClassPaths("LeftSup1", 0, getObjectId());
		incClassPaths("LeftSup2", 0, getObjectId());
		incClassPaths("RightSup", 0, getObjectId());
		incClassPaths("RightSup1", 0, getObjectId());
		incClassPaths("RightSup2", 0, getObjectId());
		incClassPaths("LeftSup1_1", 0, getObjectId());
		incClassPaths("LeftSup1_2", 0, getObjectId());
		incClassPaths("LeftSup2_1", 0, getObjectId());
		incClassPaths("LeftSup2_2", 0, getObjectId());
		incClassPaths("RightSup1_1", 0, getObjectId());
		incClassPaths("RightSup1_2", 0, getObjectId());
		incClassPaths("RightSup2_1", 0, getObjectId());
		incClassPaths("RightSup2_2", 0, getObjectId());
	}
	
	private int	_middleOff		= 0;
	private int	_leftOff		= 0;
	private int	_leftOff1		= 0;
	private int	_leftOff2		= 0;
	private int	_leftOff1_1		= 0;
	private int	_leftOff1_2		= 0;
	private int	_leftOff2_1		= 0;
	private int	_leftOff2_2		= 0;
	private int	_rightOff		= 0;
	private int	_rightOff1		= 0;
	private int	_rightOff2		= 0;
	private int	_rightOff1_1	= 0;
	private int	_rightOff1_2	= 0;
	private int	_rightOff2_1	= 0;
	private int	_rightOff2_2	= 0;
	
	public static void incClassPaths(String path, int value, int objid)
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE class_paths SET " + path + " = ? WHERE objid = ?");
			statement.setInt(1, value);
			statement.setInt(2, objid);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed updating classpath " + value + " on player: " + String.valueOf(objid), e);
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
	
	public void incMiddleOff()
	{
		_middleOff++;
		String strMiddleOff = "MiddleOff";
		incClassPaths(strMiddleOff, _middleOff, getObjectId());
	}
	
	public void setMiddleOff(int middle)
	{
		this._middleOff = middle;
	}
	
	public int getMiddleOff()
	{
		return this._middleOff;
	}
	
	public void incLeftOff()
	{
		_leftOff++;
		String strLeftOff = "LeftOff";
		incClassPaths(strLeftOff, _leftOff, getObjectId());
	}
	
	public void setLeftOff(int left)
	{
		this._leftOff = left;
	}
	
	public int getLeftOff()
	{
		return this._leftOff;
	}
	
	public void incLeftOff1()
	{
		_leftOff1++;
		String strLeftOff1 = "LeftOff1";
		incClassPaths(strLeftOff1, _leftOff1, getObjectId());
	}
	
	public void setLeftOff1(int left)
	{
		this._leftOff1 = left;
	}
	
	public int getLeftOff1()
	{
		return this._leftOff1;
	}
	
	public void incLeftOff1_1()
	{
		_leftOff1_1++;
		String strLeftOff1_1 = "LeftOff1_1";
		incClassPaths(strLeftOff1_1, _leftOff1_1, getObjectId());
	}
	
	public void setLeftOff1_1(int left)
	{
		this._leftOff1_1 = left;
	}
	
	public int getLeftOff1_1()
	{
		return this._leftOff1_1;
	}
	
	public void incLeftOff1_2()
	{
		_leftOff1_2++;
		String strLeftOff1_2 = "LeftOff1_2";
		incClassPaths(strLeftOff1_2, _leftOff1_2, getObjectId());
	}
	
	public void setLeftOff1_2(int left)
	{
		this._leftOff1_2 = left;
	}
	
	public int getLeftOff1_2()
	{
		return this._leftOff1_2;
	}
	
	public void incLeftOff2()
	{
		_leftOff2++;
		String strLeftOff2 = "LeftOff2";
		incClassPaths(strLeftOff2, _leftOff2, getObjectId());
	}
	
	public void setLeftOff2(int left)
	{
		this._leftOff2 = left;
	}
	
	public int getLeftOff2()
	{
		return this._leftOff2;
	}
	
	public void incLeftOff2_1()
	{
		_leftOff2_1++;
		String strLeftOff2_1 = "LeftOff2_1";
		incClassPaths(strLeftOff2_1, _leftOff2_1, getObjectId());
	}
	
	public void setLeftOff2_1(int left)
	{
		this._leftOff2_1 = left;
	}
	
	public int getLeftOff2_1()
	{
		return this._leftOff2_1;
	}
	
	public void incLeftOff2_2()
	{
		_leftOff2_2++;
		String strLeftOff2_2 = "LeftOff2_2";
		incClassPaths(strLeftOff2_2, _leftOff2_2, getObjectId());
	}
	
	public void setLeftOff2_2(int left)
	{
		this._leftOff2_2 = left;
	}
	
	public int getLeftOff2_2()
	{
		return this._leftOff2_2;
	}
	
	public void incRightOff()
	{
		_rightOff++;
		String strRightOff = "RightOff";
		incClassPaths(strRightOff, _rightOff, getObjectId());
	}
	
	public void setRightOff(int right)
	{
		this._rightOff = right;
	}
	
	public int getRightOff()
	{
		return this._rightOff;
	}
	
	public void incRightOff1()
	{
		_rightOff1++;
		String strRightOff1 = "RightOff1";
		incClassPaths(strRightOff1, _rightOff1, getObjectId());
	}
	
	public void setRightOff1(int right)
	{
		this._rightOff1 = right;
	}
	
	public int getRightOff1()
	{
		return this._rightOff1;
	}
	
	public void incRightOff2()
	{
		_rightOff2++;
		String strRightOff2 = "RightOff2";
		incClassPaths(strRightOff2, _rightOff2, getObjectId());
	}
	
	public void setRightOff2(int right)
	{
		this._rightOff2 = right;
	}
	
	public int getRightOff2()
	{
		return this._rightOff2;
	}
	
	public void incRightOff1_1()
	{
		_rightOff1_1++;
		String strRightOff1_1 = "RightOff1_1";
		incClassPaths(strRightOff1_1, _rightOff1_1, getObjectId());
	}
	
	public void setRightOff1_1(int Right)
	{
		this._rightOff1_1 = Right;
	}
	
	public int getRightOff1_1()
	{
		return this._rightOff1_1;
	}
	
	public void incRightOff1_2()
	{
		_rightOff1_2++;
		String strRightOff1_2 = "RightOff1_2";
		incClassPaths(strRightOff1_2, _rightOff1_2, getObjectId());
	}
	
	public void setRightOff1_2(int Right)
	{
		this._rightOff1_2 = Right;
	}
	
	public int getRightOff1_2()
	{
		return this._rightOff1_2;
	}
	
	public void incRightOff2_1()
	{
		_rightOff2_1++;
		String strRightOff2_1 = "RightOff2_1";
		incClassPaths(strRightOff2_1, _rightOff2_1, getObjectId());
	}
	
	public void setRightOff2_1(int Right)
	{
		this._rightOff2_1 = Right;
	}
	
	public int getRightOff2_1()
	{
		return this._rightOff2_1;
	}
	
	public void incRightOff2_2()
	{
		_rightOff2_2++;
		String strRightOff2_2 = "RightOff2_2";
		incClassPaths(strRightOff2_2, _rightOff2_2, getObjectId());
	}
	
	public void setRightOff2_2(int Right)
	{
		this._rightOff2_2 = Right;
	}
	
	public int getRightOff2_2()
	{
		return this._rightOff2_2;
	}
	
	private int	_middleMage		= 0;
	private int	_leftMage		= 0;
	private int	_leftMage1		= 0;
	private int	_leftMage2		= 0;
	private int	_rightMage		= 0;
	private int	_rightMage1		= 0;
	private int	_rightMage2		= 0;
	private int	_leftMage1_1	= 0;
	private int	_leftMage1_2	= 0;
	private int	_leftMage2_1	= 0;
	private int	_leftMage2_2	= 0;
	private int	_RightMage1_1	= 0;
	private int	_RightMage1_2	= 0;
	private int	_RightMage2_1	= 0;
	private int	_RightMage2_2	= 0;
	
	public void incMiddleMage()
	{
		_middleMage++;
		String strMiddleMage = "MiddleMage";
		incClassPaths(strMiddleMage, _middleMage, getObjectId());
	}
	
	public void setMiddleMage(int middle)
	{
		this._middleMage = middle;
	}
	
	public int getMiddleMage()
	{
		return this._middleMage;
	}
	
	public void incLeftMage()
	{
		_leftMage++;
		String strLeftMage = "LeftMage";
		incClassPaths(strLeftMage, _leftMage, getObjectId());
	}
	
	public void setLeftMage(int left)
	{
		this._leftMage = left;
	}
	
	public int getLeftMage()
	{
		return this._leftMage;
	}
	
	public void incLeftMage1()
	{
		_leftMage1++;
		String strLeftMage1 = "LeftMage1";
		incClassPaths(strLeftMage1, _leftMage1, getObjectId());
	}
	
	public void setLeftMage1(int left)
	{
		this._leftMage1 = left;
	}
	
	public int getLeftMage1()
	{
		return this._leftMage1;
	}
	
	public void incLeftMage2()
	{
		_leftMage2++;
		String strLeftMage2 = "LeftMage2";
		incClassPaths(strLeftMage2, _leftMage2, getObjectId());
	}
	
	public void setLeftMage2(int left)
	{
		this._leftMage2 = left;
	}
	
	public int getLeftMage2()
	{
		return this._leftMage2;
	}
	
	public void incLeftMage1_1()
	{
		_leftMage1_1++;
		String strLeftMage1_1 = "LeftMage1_1";
		incClassPaths(strLeftMage1_1, _leftMage1_1, getObjectId());
	}
	
	public void setLeftMage1_1(int left)
	{
		this._leftMage1_1 = left;
	}
	
	public int getLeftMage1_1()
	{
		return this._leftMage1_1;
	}
	
	public void incLeftMage1_2()
	{
		_leftMage1_2++;
		String strLeftMage1_2 = "LeftMage1_2";
		incClassPaths(strLeftMage1_2, _leftMage1_2, getObjectId());
	}
	
	public void setLeftMage1_2(int left)
	{
		this._leftMage1_2 = left;
	}
	
	public int getLeftMage1_2()
	{
		return this._leftMage1_2;
	}
	
	public void incLeftMage2_1()
	{
		_leftMage2_1++;
		String strLeftMage2_1 = "LeftMage2_1";
		incClassPaths(strLeftMage2_1, _leftMage2_1, getObjectId());
	}
	
	public void setLeftMage2_1(int left)
	{
		this._leftMage2_1 = left;
	}
	
	public int getLeftMage2_1()
	{
		return this._leftMage2_1;
	}
	
	public void incLeftMage2_2()
	{
		_leftMage2_2++;
		String strLeftMage2_2 = "LeftMage2_2";
		incClassPaths(strLeftMage2_2, _leftMage2_2, getObjectId());
	}
	
	public void setLeftMage2_2(int left)
	{
		this._leftMage2_2 = left;
	}
	
	public int getLeftMage2_2()
	{
		return this._leftMage2_2;
	}
	
	public void incRightMage()
	{
		_rightMage++;
		String strRightMage = "RightMage";
		incClassPaths(strRightMage, _rightMage, getObjectId());
	}
	
	public void setRightMage(int right)
	{
		this._rightMage = right;
	}
	
	public int getRightMage()
	{
		return this._rightMage;
	}
	
	public void incRightMage1()
	{
		_rightMage1++;
		String strRightMage1 = "RightMage1";
		incClassPaths(strRightMage1, _rightMage1, getObjectId());
	}
	
	public void setRightMage1(int right)
	{
		this._rightMage1 = right;
	}
	
	public int getRightMage1()
	{
		return this._rightMage1;
	}
	
	public void incRightMage2()
	{
		_rightMage2++;
		String strRightMage2 = "RightMage2";
		incClassPaths(strRightMage2, _rightMage2, getObjectId());
	}
	
	public void setRightMage2(int right)
	{
		this._rightMage2 = right;
	}
	
	public int getRightMage2()
	{
		return this._rightMage2;
	}
	
	public void incRightMage1_1()
	{
		_RightMage1_1++;
		String strRightMage1_1 = "RightMage1_1";
		incClassPaths(strRightMage1_1, _RightMage1_1, getObjectId());
	}
	
	public void setRightMage1_1(int Right)
	{
		this._RightMage1_1 = Right;
	}
	
	public int getRightMage1_1()
	{
		return this._RightMage1_1;
	}
	
	public void incRightMage1_2()
	{
		_RightMage1_2++;
		String strRightMage1_2 = "RightMage1_2";
		incClassPaths(strRightMage1_2, _RightMage1_2, getObjectId());
	}
	
	public void setRightMage1_2(int Right)
	{
		this._RightMage1_2 = Right;
	}
	
	public int getRightMage1_2()
	{
		return this._RightMage1_2;
	}
	
	public void incRightMage2_1()
	{
		_RightMage2_1++;
		String strRightMage2_1 = "RightMage2_1";
		incClassPaths(strRightMage2_1, _RightMage2_1, getObjectId());
	}
	
	public void setRightMage2_1(int Right)
	{
		this._RightMage2_1 = Right;
	}
	
	public int getRightMage2_1()
	{
		return this._RightMage2_1;
	}
	
	public void incRightMage2_2()
	{
		_RightMage2_2++;
		String strRightMage2_2 = "RightMage2_2";
		incClassPaths(strRightMage2_2, _RightMage2_2, getObjectId());
	}
	
	public void setRightMage2_2(int Right)
	{
		this._RightMage2_2 = Right;
	}
	
	public int getRightMage2_2()
	{
		return this._RightMage2_2;
	}
	
	private int	_middleDef		= 0;
	private int	_leftDef		= 0;
	private int	_leftDef1		= 0;
	private int	_leftDef2		= 0;
	private int	_rightDef		= 0;
	private int	_rightDef1		= 0;
	private int	_rightDef2		= 0;
	private int	_leftDef1_1		= 0;
	private int	_leftDef1_2		= 0;
	private int	_leftDef2_1		= 0;
	private int	_leftDef2_2		= 0;
	private int	_rightDef1_1	= 0;
	private int	_rightDef1_2	= 0;
	private int	_rightDef2_1	= 0;
	private int	_rightDef2_2	= 0;
	
	public void incMiddleDef()
	{
		_middleDef++;
		String strMiddleDef = "MiddleDef";
		incClassPaths(strMiddleDef, _middleDef, getObjectId());
	}
	
	public void setMiddleDef(int middle)
	{
		this._middleDef = middle;
	}
	
	public int getMiddleDef()
	{
		return this._middleDef;
	}
	
	public void incLeftDef()
	{
		_leftDef++;
		String strLeftDef = "LeftDef";
		incClassPaths(strLeftDef, _leftDef, getObjectId());
	}
	
	public void setLeftDef(int left)
	{
		this._leftDef = left;
	}
	
	public int getLeftDef()
	{
		return this._leftDef;
	}
	
	public void incLeftDef1()
	{
		_leftDef1++;
		String strLeftDef1 = "LeftDef1";
		incClassPaths(strLeftDef1, _leftDef1, getObjectId());
	}
	
	public void setLeftDef1(int left)
	{
		this._leftDef1 = left;
	}
	
	public int getLeftDef1()
	{
		return this._leftDef1;
	}
	
	public void incLeftDef2()
	{
		_leftDef2++;
		String strLeftDef2 = "LeftDef2";
		incClassPaths(strLeftDef2, _leftDef2, getObjectId());
	}
	
	public void setLeftDef2(int left)
	{
		this._leftDef2 = left;
	}
	
	public int getLeftDef2()
	{
		return this._leftDef2;
	}
	
	public void incRightDef()
	{
		_rightDef++;
		String strRightDef = "RightDef";
		incClassPaths(strRightDef, _rightDef, getObjectId());
	}
	
	public void setRightDef(int right)
	{
		this._rightDef = right;
	}
	
	public int getRightDef()
	{
		return this._rightDef;
	}
	
	public void incRightDef1()
	{
		_rightDef1++;
		String strRightDef1 = "RightDef1";
		incClassPaths(strRightDef1, _rightDef1, getObjectId());
	}
	
	public void setRightDef1(int right)
	{
		this._rightDef1 = right;
	}
	
	public int getRightDef1()
	{
		return this._rightDef1;
	}
	
	public void incRightDef2()
	{
		_rightDef2++;
		String strRightDef2 = "RightDef2";
		incClassPaths(strRightDef2, _rightDef2, getObjectId());
	}
	
	public void setRightDef2(int right)
	{
		this._rightDef2 = right;
	}
	
	public int getRightDef2()
	{
		return this._rightDef2;
	}
	
	public void incLeftDef1_1()
	{
		_leftDef1_1++;
		String strLeftDef1_1 = "LeftDef1_1";
		incClassPaths(strLeftDef1_1, _leftDef1_1, getObjectId());
	}
	
	public void setLeftDef1_1(int left)
	{
		this._leftDef1_1 = left;
	}
	
	public int getLeftDef1_1()
	{
		return this._leftDef1_1;
	}
	
	public void incLeftDef1_2()
	{
		_leftDef1_2++;
		String strLeftDef1_2 = "LeftDef1_2";
		incClassPaths(strLeftDef1_2, _leftDef1_2, getObjectId());
	}
	
	public void setLeftDef1_2(int left)
	{
		this._leftDef1_2 = left;
	}
	
	public int getLeftDef1_2()
	{
		return this._leftDef1_2;
	}
	
	public void incLeftDef2_1()
	{
		_leftDef2_1++;
		String strLeftDef2_1 = "LeftDef2_1";
		incClassPaths(strLeftDef2_1, _leftDef2_1, getObjectId());
	}
	
	public void setLeftDef2_1(int left)
	{
		this._leftDef2_1 = left;
	}
	
	public int getLeftDef2_1()
	{
		return this._leftDef2_1;
	}
	
	public void incLeftDef2_2()
	{
		_leftDef2_2++;
		String strLeftDef2_2 = "LeftDef2_2";
		incClassPaths(strLeftDef2_2, _leftDef2_2, getObjectId());
	}
	
	public void setLeftDef2_2(int left)
	{
		this._leftDef2_2 = left;
	}
	
	public int getLeftDef2_2()
	{
		return this._leftDef2_2;
	}
	
	public void incRightDef1_1()
	{
		_rightDef1_1++;
		String strRightDef1_1 = "RightDef1_1";
		incClassPaths(strRightDef1_1, _rightDef1_1, getObjectId());
	}
	
	public void setRightDef1_1(int Right)
	{
		this._rightDef1_1 = Right;
	}
	
	public int getRightDef1_1()
	{
		return this._rightDef1_1;
	}
	
	public void incRightDef1_2()
	{
		_rightDef1_2++;
		String strRightDef1_2 = "RightDef1_2";
		incClassPaths(strRightDef1_2, _rightDef1_2, getObjectId());
	}
	
	public void setRightDef1_2(int Right)
	{
		this._rightDef1_2 = Right;
	}
	
	public int getRightDef1_2()
	{
		return this._rightDef1_2;
	}
	
	public void incRightDef2_1()
	{
		_rightDef2_1++;
		String strRightDef2_1 = "RightDef2_1";
		incClassPaths(strRightDef2_1, _rightDef2_1, getObjectId());
	}
	
	public void setRightDef2_1(int Right)
	{
		this._rightDef2_1 = Right;
	}
	
	public int getRightDef2_1()
	{
		return this._rightDef2_1;
	}
	
	public void incRightDef2_2()
	{
		_rightDef2_2++;
		String strRightDef2_2 = "RightDef2_2";
		incClassPaths(strRightDef2_2, _rightDef2_2, getObjectId());
	}
	
	public void setRightDef2_2(int Right)
	{
		this._rightDef2_2 = Right;
	}
	
	public int getRightDef2_2()
	{
		return this._rightDef2_2;
	}
	
	private int	_middleSup		= 0;
	private int	_leftSup		= 0;
	private int	_leftSup1		= 0;
	private int	_leftSup2		= 0;
	private int	_rightSup		= 0;
	private int	_rightSup1		= 0;
	private int	_rightSup2		= 0;
	private int	_leftSup1_1		= 0;
	private int	_leftSup1_2		= 0;
	private int	_leftSup2_1		= 0;
	private int	_leftSup2_2		= 0;
	private int	_rightSup1_1	= 0;
	private int	_rightSup1_2	= 0;
	private int	_rightSup2_1	= 0;
	private int	_rightSup2_2	= 0;
	
	public void incMiddleSup()
	{
		_middleSup++;
		String strMiddleSup = "MiddleSup";
		incClassPaths(strMiddleSup, _middleSup, getObjectId());
	}
	
	public void setMiddleSup(int middle)
	{
		this._middleSup = middle;
	}
	
	public int getMiddleSup()
	{
		return this._middleSup;
	}
	
	public void incLeftSup()
	{
		_leftSup++;
		String strLeftSup = "LeftSup";
		incClassPaths(strLeftSup, _leftSup, getObjectId());
	}
	
	public void setLeftSup(int left)
	{
		this._leftSup = left;
	}
	
	public int getLeftSup()
	{
		return this._leftSup;
	}
	
	public void incLeftSup1()
	{
		_leftSup1++;
		String strLeftSup1 = "LeftSup1";
		incClassPaths(strLeftSup1, _leftSup1, getObjectId());
	}
	
	public void setLeftSup1(int left)
	{
		this._leftSup1 = left;
	}
	
	public int getLeftSup1()
	{
		return this._leftSup1;
	}
	
	public void incLeftSup2()
	{
		_leftSup2++;
		String strLeftSup2 = "LeftSup2";
		incClassPaths(strLeftSup2, _leftSup2, getObjectId());
	}
	
	public void setLeftSup2(int left)
	{
		this._leftSup2 = left;
	}
	
	public int getLeftSup2()
	{
		return this._leftSup2;
	}
	
	public void incRightSup()
	{
		_rightSup++;
		String strRightSup = "RightSup";
		incClassPaths(strRightSup, _rightSup, getObjectId());
	}
	
	public void setRightSup(int right)
	{
		this._rightSup = right;
	}
	
	public int getRightSup()
	{
		return this._rightSup;
	}
	
	public void incRightSup1()
	{
		_rightSup1++;
		String strRightSup1 = "RightSup1";
		incClassPaths(strRightSup1, _rightSup1, getObjectId());
	}
	
	public void setRightSup1(int right)
	{
		this._rightSup1 = right;
	}
	
	public int getRightSup1()
	{
		return this._rightSup1;
	}
	
	public void incRightSup2()
	{
		_rightSup2++;
		String strRightSup2 = "RightSup2";
		incClassPaths(strRightSup2, _rightSup2, getObjectId());
	}
	
	public void setRightSup2(int right)
	{
		this._rightSup2 = right;
	}
	
	public int getRightSup2()
	{
		return this._rightSup2;
	}
	
	public void incLeftSup1_1()
	{
		_leftSup1_1++;
		String strLeftSup1_1 = "LeftSup1_1";
		incClassPaths(strLeftSup1_1, _leftSup1_1, getObjectId());
	}
	
	public void setLeftSup1_1(int left)
	{
		this._leftSup1_1 = left;
	}
	
	public int getLeftSup1_1()
	{
		return this._leftSup1_1;
	}
	
	public void incLeftSup1_2()
	{
		_leftSup1_2++;
		String strLeftSup1_2 = "LeftSup1_2";
		incClassPaths(strLeftSup1_2, _leftSup1_2, getObjectId());
	}
	
	public void setLeftSup1_2(int left)
	{
		this._leftSup1_2 = left;
	}
	
	public int getLeftSup1_2()
	{
		return this._leftSup1_2;
	}
	
	public void incLeftSup2_1()
	{
		_leftSup2_1++;
		String strLeftSup2_1 = "LeftSup2_1";
		incClassPaths(strLeftSup2_1, _leftSup2_1, getObjectId());
	}
	
	public void setLeftSup2_1(int left)
	{
		this._leftSup2_1 = left;
	}
	
	public int getLeftSup2_1()
	{
		return this._leftSup2_1;
	}
	
	public void incLeftSup2_2()
	{
		_leftSup2_2++;
		String strLeftSup2_2 = "LeftSup2_2";
		incClassPaths(strLeftSup2_2, _leftSup2_2, getObjectId());
	}
	
	public void setLeftSup2_2(int left)
	{
		this._leftSup2_2 = left;
	}
	
	public int getLeftSup2_2()
	{
		return this._leftSup2_2;
	}
	
	public void incRightSup1_1()
	{
		_rightSup1_1++;
		String strRightSup1_1 = "RightSup1_1";
		incClassPaths(strRightSup1_1, _rightSup1_1, getObjectId());
	}
	
	public void setRightSup1_1(int Right)
	{
		this._rightSup1_1 = Right;
	}
	
	public int getRightSup1_1()
	{
		return this._rightSup1_1;
	}
	
	public void incRightSup1_2()
	{
		_rightSup1_2++;
		String strRightSup1_2 = "RightSup1_2";
		incClassPaths(strRightSup1_2, _rightSup1_2, getObjectId());
	}
	
	public void setRightSup1_2(int Right)
	{
		this._rightSup1_2 = Right;
	}
	
	public int getRightSup1_2()
	{
		return this._rightSup1_2;
	}
	
	public void incRightSup2_1()
	{
		_rightSup2_1++;
		String strRightSup2_1 = "RightSup2_1";
		incClassPaths(strRightSup2_1, _rightSup2_1, getObjectId());
	}
	
	public void setRightSup2_1(int Right)
	{
		this._rightSup2_1 = Right;
	}
	
	public int getRightSup2_1()
	{
		return this._rightSup2_1;
	}
	
	public void incRightSup2_2()
	{
		_rightSup2_2++;
		String strRightSup2_2 = "RightSup2_2";
		incClassPaths(strRightSup2_2, _rightSup2_2, getObjectId());
	}
	
	public void setRightSup2_2(int Right)
	{
		_rightSup2_2 = Right;
	}
	
	public int getRightSup2_2()
	{
		return _rightSup2_2;
	}
	
	private int _cp_points = 0;
	
	public void decCpPoints()
	{
		this._cp_points--;
	}
	
	public void incCpPoints()
	{
		_cp_points++;
	}
	
	public void setCpPoints(int alekos)
	{
		this._cp_points = alekos;
	}
	
	public int getCpPoints()
	{
		return this._cp_points;
	}
	
	private int _maxCpPoints = 0;
	
	public void setMaxCpPoints(int alekos)
	{
		this._maxCpPoints = alekos;
	}
	
	public int getMaxCpPoints()
	{
		if (getLevel() == 86)
		{
			_maxCpPoints = 2;
		}
		else if (getLevel() == 87)
		{
			_maxCpPoints = 4;
		}
		else if (getLevel() == 88)
		{
			_maxCpPoints = 6;
		}
		else if (getLevel() == 89)
		{
			_maxCpPoints = 8;
		}
		else if (getLevel() == 90)
		{
			_maxCpPoints = 10;
		}
		else if (getLevel() == 91)
		{
			_maxCpPoints = 12;
		}
		else if (getLevel() == 92)
		{
			_maxCpPoints = 14;
		}
		else if (getLevel() == 93)
		{
			_maxCpPoints = 16;
		}
		else if (getLevel() == 94)
		{
			_maxCpPoints = 18;
		}
		else if (getLevel() == 95)
		{
			_maxCpPoints = 20;
		}
		else
		{
			_maxCpPoints = 0;
		}
		return _maxCpPoints;
	}
	
	private boolean _forceNoSpawnProtection = false;
	
	public boolean isForceNoSpawnProtection()
	{
		return _forceNoSpawnProtection;
	}
	
	public void setForceNoSpawnProtection(boolean forceNoSpawnProtection)
	{
		_forceNoSpawnProtection = forceNoSpawnProtection;
	}
	
	public int		_ssGrade			= 5;
	public int		_itemAug			= 0;
	public int		_lsAug				= 0;
	public int		_gemAug				= 0;
	public int		_equipmentViewer	= 0;
	public boolean	_bypassTradeChat, _bypassShout;
	
	public int getLockdownTime()
	{
		return _lockdownTime;
	}
	
	public void setLockdownTime(int lockdownTime)
	{
		_lockdownTime = lockdownTime;
	}
	
	public long getLastCaptchaTimeStamp()
	{
		return _lastCaptchaTimeStamp;
	}
	
	public void setLastCaptchaTimeStamp(long lastCaptchaTimeStamp)
	{
		_lastCaptchaTimeStamp = lastCaptchaTimeStamp;
	}
	
	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{}
		
		public L2PcInstance getPlayer()
		{
			return L2PcInstance.this;
		}
		
		public void doPickupItem(L2Object object)
		{
			L2PcInstance.this.doPickupItem(object);
		}
		
		public void doInteract(L2Character target)
		{
			L2PcInstance.this.doInteract(target);
		}
		
		@Override
		public void doAttack(L2Character target)
		{
			super.doAttack(target);
			if (isZombie())
			{
				L2PcInstance targetPlayer = target.getActingPlayer();
				if (targetPlayer != null)
					Zombie.getInstance().infect(targetPlayer);
			}
			// cancel the recent fake-death protection instantly if the player attacks or casts spells
			getPlayer().setRecentFakeDeath(false);
		}
		
		@Override
		public void doCast(L2Skill skill)
		{
			super.doCast(skill);
			// cancel the recent fake-death protection instantly if the player attacks or casts spells
			getPlayer().setRecentFakeDeath(false);
		}
	}
	
	private L2GameClient						_client;
	private String								_accountName;
	private long								_deleteTimer;
	public boolean								_ignoreLevel	= false;
	private boolean								_isOnline		= false;
	private long								_onlineTime;
	private long								_onlineBeginTime;
	private CharactersTable.CharacterLoginData	_loginData;
	private long								_lastAccess;
	public long									_uptime;
	private int									_ping			= -1;
	private final ReentrantLock					_subclassLock	= new ReentrantLock();
	protected int								_baseClass;
	protected int								_activeClass;
	protected int								_classIndex		= 0;
	private int									_race			= -1;
	/** data for mounted pets */
	private int									_controlItemId;
	private L2PetData							_data;
	private int									_curFeed;
	protected Future<?>							_mountFeedTask;
	private ScheduledFuture<?>					_dismountTask;
	/** The list of sub-classes this character has. */
	private Map<Integer, SubClass>				_subClasses;
	private PcAppearance						_appearance;
	/** The Identifier of the L2PcInstance */
	private int									_charId			= 0x00030b7a;
	/** The Experience of the L2PcInstance before the last Death Penalty */
	private long								_expBeforeDeath;
	/** The Karma of the L2PcInstance (if higher than 0, the name of the L2PcInstance appears in red) */
	private int									_karma;
	private int									_eventKills;
	private int									_siegeKills;
	private int									_raidKills;
	private int									_olympiadWins;
	/** The number of player killed during a PvP (the player killed was PvP Flagged) */
	private int									_pvpKills;
	/** The PK counter of the L2PcInstance (= Number of non PvP Flagged player killed) */
	private int									_pkKills;
	/** The PvP Flag state of the L2PcInstance (0=White, 1=Purple) */
	private byte								_pvpFlag;
	/** The Fame of this L2PcInstance */
	private int									_fame;
	private ScheduledFuture<?>					_fameTask;
	/** Vitality recovery task */
	private ScheduledFuture<?>					_vitalityTask;
	private int									_turnedGMOff	= 0;
	
	public int getTurnedGMOff()
	{
		return _turnedGMOff;
	}
	
	public void setTurnedGMOff(int turnedGMOff)
	{
		_turnedGMOff = turnedGMOff;
	}
	
	/** The Siege state of the L2PcInstance */
	private byte						_siegeState			= 0;
	private int							_lastCompassZone;										// the last compass zone update send to the client
	private boolean						_isIn7sDungeon		= false;
	public int							_gearLimit			= -1;
	public int							_bookmarkslot		= 0;								// The Teleport Bookmark Slot
	public FastList<TeleportBookmark>	tpbookmark			= new FastList<TeleportBookmark>();
	private final FloodProtectors		_floodProtectors	= new FloodProtectors(this);
	private PunishLevel					_punishLevel		= PunishLevel.NONE;
	private long						_punishTimer		= 0;
	private ScheduledFuture<?>			_punishTask;
	
	public enum PunishLevel
	{
		NONE(0, ""),
		CHAT(1, "chat banned"),
		JAIL(2, "jailed"),
		CHAR(3, "banned"),
		ACC(4, "banned");
		
		private int		punValue;
		private String	punString;
		
		PunishLevel(int value, String string)
		{
			punValue = value;
			punString = string;
		}
		
		public int value()
		{
			return punValue;
		}
		
		public String string()
		{
			return punString;
		}
	}
	
	/** Olympiad */
	private boolean				_inOlympiadMode				= false;
	private boolean				_OlympiadStart				= false;
	private int					_olympiadGameId				= -1;
	private int					_olympiadSide				= -1;
	public int					olyBuff						= 0;
	/** Duel */
	private boolean				_isInDuel					= false;
	private boolean				_preDuelState				= false;
	private int					_duelState					= Duel.DUELSTATE_NODUEL;
	private int					_duelId						= 0;
	private SystemMessageId		_noDuelReason				= SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
	/** Boat */
	private boolean				_inBoat;
	private L2BoatInstance		_boat;
	private Point3D				_inBoatPosition;
	/** AirShip */
	private L2AirShipInstance	_airShip;
	private boolean				_inAirShip;
	private Point3D				_inAirShipPosition;
	public ScheduledFuture<?>	_taskforfish;
	private int					_mountType;
	private int					_mountNpcId;
	private int					_mountLevel;
	/** Store object used to summon the strider you are mounting **/
	private int					_mountObjectID				= 0;
	public int					_telemode					= 0;
	public boolean				_exploring					= false;
	private boolean				_isCool						= false;
	public boolean				_hasTehForce				= false;
	public String				_email;
	public String				_emailTemp;
	public String				_code;
	public String				_donateId;
	private byte				_accDisplay					= 0;
	private byte				_cloakDisplay				= 0;
	private int					_actionObjIdNoTarget		= 0;
	private int					_actionObjIdNoTargetTicks	= 0;
	/* private FastList<L2Skill> _partyPassiveList; */
	
	public boolean hasEmailRegistered()
	{
		if (_email == null || _email.equalsIgnoreCase("") || _email.equalsIgnoreCase("none"))
			return false;
		return true;
	}
	
	final public boolean isCool()
	{
		if (isDisguised())
			return false;
		return _isCool;
	}
	
	final public void setIsCool(final boolean isCool)
	{
		_isCool = isCool;
	}
	
	public void loadEmail()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement offline = con.prepareStatement("SELECT email FROM accounts WHERE login = ?"))
		{
			offline.setString(1, getAccountName());
			try (ResultSet rs = offline.executeQuery())
			{
				while (rs.next())
				{
					setEmail(rs.getString(1));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	final public byte getAccDisplay()
	{
		return _accDisplay;
	}
	
	final public void setAccDisplay(final byte val)
	{
		_accDisplay = val;
	}
	
	final public byte getCloakDisplay()
	{
		return _cloakDisplay;
	}
	
	final public void setCloakDisplay(final byte val)
	{
		_cloakDisplay = val;
	}
	
	private boolean								_inCrystallize;
	private boolean								_inCraftMode;
	private long								_offlineShopStart		= 0;
	private L2Transformation					_transformation;
	private static int							_transformationId		= 0;
	/** The table containing all L2RecipeList of the L2PcInstance */
	private final Map<Integer, L2RecipeList>	_dwarvenRecipeBook		= new FastMap<Integer, L2RecipeList>();
	private final Map<Integer, L2RecipeList>	_commonRecipeBook		= new FastMap<Integer, L2RecipeList>();
	/** True if the L2PcInstance is sitting */
	private boolean								_waitTypeSitting;
	/** True if the L2PcInstance is using the relax skill */
	private boolean								_relax;
	/** Location before entering Observer Mode */
	private int									_obsX;
	private int									_obsY;
	private int									_obsZ;
	private boolean								_observerMode			= false;
	/** Stored from last ValidatePosition **/
	private final Point3D						_lastServerPosition		= new Point3D(0, 0, 0);
	/** Previous coordinate sent to party in ValidatePosition **/
	private final Point3D						_lastPartyPosition		= new Point3D(0, 0, 0);
	/** The number of recommendation obtained by the L2PcInstance */
	private int									_recomHave;														// how much I was recommended by others
	/** The number of recommendation that the L2PcInstance can give */
	private int									_recomLeft;														// how many recommendations I can give to others
	/** Date when recommendation points were updated last time */
	private long								_charCreationTime;
	/** List with the recommendations that I've give */
	private final List<Integer>					_recomChars				= new FastList<Integer>();
	/** The random number of the L2PcInstance */
	// private static final Random _rnd = new Random();
	private final PcInventory					_inventory				= new PcInventory(this);
	private PcWarehouse							_warehouse;
	private PcFreight							_freight;
	private List<PcFreight>						_depositedFreight;
	/** The Private Store type of the L2PcInstance (STORE_PRIVATE_NONE=0, STORE_PRIVATE_SELL=1, sellmanage=2, STORE_PRIVATE_BUY=3, buymanage=4, STORE_PRIVATE_MANUFACTURE=5) */
	private int									_privatestore;
	private TradeList							_activeTradeList;
	private ItemContainer						_activeWarehouse;
	private L2ManufactureList					_createList;
	private TradeList							_sellList;
	private TradeList							_buyList;
	/** Bitmask used to keep track of one-time/newbie quest rewards */
	private int									_newbie;
	private boolean								_beta					= false;
	private boolean								_vip					= false;
	private boolean								_vipPass				= false;
	private boolean								_noble					= false;
	private boolean								_hero					= false;
	private int									_movieId				= 0;
	private int									_useItemRequestItemId;
	/** The L2FolkInstance corresponding to the last Folk wich one the player talked. */
	private L2Npc								_lastFolkNpc			= null;
	/** Last NPC Id talked on a quest */
	private int									_questNpcObject			= 0;
	/** The table containing all Quests began by the L2PcInstance */
	private final List<QuestState>				_quests					= new ArrayList<>();
	/** The list containing all shortCuts of this L2PcInstance */
	private final ShortCuts						_shortCuts				= new ShortCuts(this);
	/** The list containing all macroses of this L2PcInstance */
	private final MacroList						_macroses				= new MacroList(this);
	private final List<L2PcInstance>			_snoopListener			= new FastList<L2PcInstance>();
	private final List<L2PcInstance>			_snoopedPlayer			= new FastList<L2PcInstance>();
	private ClassId								_skillLearningClassId;
	// hennas
	private final L2Henna[]						_henna					= new L2Henna[3];
	private int									_hennaSTR;
	private int									_hennaINT;
	private int									_hennaDEX;
	private int									_hennaMEN;
	private int									_hennaWIT;
	private int									_hennaCON;
	/** The L2Summon of the L2PcInstance */
	private L2Summon							_summon					= null;
	/** The L2Decoy of the L2PcInstance */
	private L2Decoy								_decoy					= null;
	/*	*//** The L2Trap of the L2PcInstance *//*
												 * private L2Trap _trap = null;
												 */
	/** The L2Agathion of the L2PcInstance */
	private int									_agathionId				= 0;
	// apparently, a L2PcInstance CAN have both a summon AND a tamed beast at the same time!!
	private L2TamedBeastInstance				_tamedBeast				= null;
	// client radar
	// TODO: This needs to be better intergrated and saved/loaded
	private L2Radar								_radar;
	// these values are only stored temporarily
	private boolean								_partyMatchingAutomaticRegistration;
	private boolean								_partyMatchingShowLevel;
	private boolean								_partyMatchingShowClass;
	private String								_partyMatchingMemo;
	private boolean								_isDisguised			= false;
	// Clan related attributes
	/** The Clan Identifier of the L2PcInstance */
	private int									_clanId;
	/** The Clan object of the L2PcInstance */
	private L2Clan								_clan;
	/** Apprentice and Sponsor IDs */
	private int									_apprentice				= 0;
	private int									_sponsor				= 0;
	private long								_clanJoinExpiryTime;
	private long								_clanCreateExpiryTime;
	private int									_powerGrade				= 0;
	private int									_clanPrivileges			= 0;
	/** L2PcInstance's pledge class (knight, Baron, etc.) */
	private int									_pledgeClass			= 0;
	private int									_pledgeType				= 0;
	private int									_wantsPeace				= 0;
	// Death Penalty Buff Level
	private int									_deathPenaltyBuffLevel	= 0;
	private byte								_lastConsumedSoulAmount	= 0;
	// self resurrect during siege
	private boolean								_charmOfCourage			= false;
	// charges
	private final AtomicInteger					_charges				= new AtomicInteger();
	/* private ScheduledFuture<?> _chargeTask = null; */
	public ScheduledFuture<?>					_removeTempHeroTask		= null;
	private ScheduledFuture<?>					_kickFromEventTask		= null;
	private ScheduledFuture<?>					_kickFromRaidTask		= null;
	public byte									eventTicker				= 0;
	
	public void startKickFromEventTask()
	{
		stopKickFromEventTask();
		eventTicker = 0;
		_kickFromEventTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new kickFromEventTask(this), 2000, 70000);
	}
	
	public void startKickFromRaidTask()
	{
		stopKickFromEventTask();
		eventTicker = 0;
		_kickFromRaidTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new kickFromRaidTask(this), 2000, 120000);
	}
	
	public void stopKickFromEventTask()
	{
		if (_kickFromEventTask != null)
		{
			_kickFromEventTask.cancel(true);
			_kickFromEventTask = null;
		}
		eventTicker = 0;
	}
	
	public void stopKickFromRaidTask()
	{
		if (_kickFromRaidTask != null)
		{
			_kickFromRaidTask.cancel(true);
			_kickFromRaidTask = null;
		}
		eventTicker = 0;
	}
	
	public void warnAFK()
	{
		sendMessage("You've been afk for 70 seconds! You will be kicked in another 70 seconds if you don't participate in the event!");
	}
	
	public void warnAFKRaid()
	{
		sendMessage("You've been afk for 120 seconds in a raid zone!");
	}
	
	public void kickAFKRaid()
	{
		teleToLocation(MapRegionTable.TeleportWhereType.Town);
		sendMessage("You can't remain afk in a raid zone!");
	}
	
	public void kickAFKMsg()
	{
		sendMessage("You have been kicked from this event for being idle.");
	}
	
	private class kickFromEventTask implements Runnable
	{
		L2PcInstance noob = null;
		
		public kickFromEventTask(L2PcInstance player)
		{
			noob = player;
		}
		
		public void run()
		{
			if (noob != null)
			{
				try
				{
					switch (noob.eventTicker)
					{
						case 0:
						{
							if (noob._inEventTvT && TvT._started)
							{
								noob.eventTicker = 1;
							}
							else if (noob._inEventTvT && NewTvT._started)
							{
								noob.eventTicker = 1;
							}
							else if (noob._inEventHG && NewHuntingGrounds._started)
							{
								noob.eventTicker = 1;
							}
							else if (noob._inEventFOS && (FOS._started || NewFOS._started))
							{
								noob.eventTicker = 1;
							}
							else if (noob._inEventCTF && CTF._started)
							{
								if (!noob._haveFlagCTF)
									noob.eventTicker = 1;
							}
							else if (noob._inEventCTF && NewCTF._started)
							{
								if (!noob._haveFlagCTF)
									noob.eventTicker = 1;
							}
							else if (noob._inEventVIP && VIP._started && !noob._isVIP)
							{
								noob.eventTicker = 1;
							}
							else if (noob.isInsideZone(L2Character.ZONE_RAID))
							{
								noob.eventTicker = 1;
							}
							else
							{
								noob.stopKickFromEventTask();
							}
							break;
						}
						case 1:
						{
							if (noob._inEventTvT && TvT._started)
							{
								noob.warnAFK();
								noob.eventTicker = 2;
							}
							else if (noob._inEventTvT && NewTvT._started)
							{
								noob.warnAFK();
								noob.eventTicker = 2;
							}
							else if (noob._inEventHG && NewHuntingGrounds._started)
							{
								noob.warnAFK();
								noob.eventTicker = 2;
							}
							else if (noob._inEventLunaDomi && NewDomination._started)
							{
								noob.warnAFK();
								noob.eventTicker = 2;
							}
							else if (noob._inEventCTF && NewCTF._started)
							{
								if (!noob._haveFlagCTF)
								{
									noob.warnAFK();
									noob.eventTicker = 2;
								}
								else
									noob.eventTicker = 0;
							}
							else if (noob._inEventFOS && (NewFOS._started))
							{
								noob.warnAFK();
								if (!(NewFOS._team1Sealers.contains(noob.getObjectId()) || NewFOS._team2Sealers.contains(noob.getObjectId())))
									noob.eventTicker = 2;
							}
							else if (noob._inEventFOS && (FOS._started))
							{
								noob.warnAFK();
								if (!(FOS._team1Sealers.contains(noob.getObjectId()) || FOS._team2Sealers.contains(noob.getObjectId())))
									noob.eventTicker = 2;
							}
							else if (noob._inEventCTF && CTF._started)
							{
								if (!noob._haveFlagCTF)
								{
									noob.warnAFK();
									noob.eventTicker = 2;
								}
								else
									noob.eventTicker = 0;
							}
							else if (noob._inEventVIP && VIP._started && !noob._isVIP)
							{
								noob.warnAFK();
								noob.eventTicker = 2;
							}
							else if (noob.isInsideZone(L2Character.ZONE_RAID))
							{
								noob.warnAFKRaid();
								noob.eventTicker = 2;
							}
							else
							{
								noob.stopKickFromEventTask();
							}
							break;
						}
						case 2:
						{
							if (noob._inEventTvT && TvT._started)
							{
								TvT.kickPlayerFromTvt(noob);
								noob.kickAFKMsg();
							}
							else if (noob._inEventFOS && FOS._started)
							{
								if (!(FOS._team1Sealers.contains(noob.getObjectId()) || FOS._team2Sealers.contains(noob.getObjectId())))
								{
									FOS.kickPlayerFromFos(noob);
									noob.kickAFKMsg();
								}
								else
								{
									noob.warnAFK();
									noob.eventTicker = 1;
								}
							}
							else if (noob._inEventCTF && CTF._started)
							{
								if (!noob._haveFlagCTF)
								{
									CTF.kickPlayerFromCTf(noob);
									noob.kickAFKMsg();
								}
								else
									noob.eventTicker = 0;
							}
							else if (noob._inEventVIP && VIP._started && !noob._isVIP)
							{
								VIP.removePlayer(noob);
								noob.kickAFKMsg();
							}
							else if (noob.isInsideZone(L2Character.ZONE_RAID))
							{
								noob.broadcastUserInfo();
								noob.kickAFKRaid();
								// noob.eventTicker = 1;
							}
							else if (noob._inEventTvT && NewTvT._started)
							{
								NewTvT.kickPlayerFromTvt(noob);
								noob.kickAFKMsg();
							}
							else if (noob._inEventHG && NewHuntingGrounds._started)
							{
								NewHuntingGrounds.kickPlayerFromHG(noob);
								noob.kickAFKMsg();
							}
							else if (noob._inEventLunaDomi && NewDomination._started)
							{
								NewDomination.kickPlayerFromDomination(noob);
								noob.kickAFKMsg();
							}
							else if (noob._inEventCTF && NewCTF._started)
							{
								if (!noob._haveFlagCTF)
								{
									NewCTF.kickPlayerFromCTf(noob);
									noob.kickAFKMsg();
								}
								else
									noob.eventTicker = 0;
							}
							else if (noob._inEventFOS && (NewFOS._started))
							{
								if (!(NewFOS._team1Sealers.contains(noob.getObjectId()) || NewFOS._team2Sealers.contains(noob.getObjectId())))
								{
									NewFOS.kickPlayerFromFos(noob);
									noob.kickAFKMsg();
								}
								else
								{
									noob.warnAFK();
									noob.eventTicker = 1;
								}
							}
							noob.stopKickFromEventTask();
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	private class kickFromRaidTask implements Runnable
	{
		L2PcInstance noob = null;
		
		public kickFromRaidTask(L2PcInstance player)
		{
			noob = player;
		}
		
		public void run()
		{
			if (noob != null)
			{
				try
				{
					switch (noob.eventTicker)
					{
						case 0:
						{
							if (noob.isInsideZone(L2Character.ZONE_RAID))
							{
								noob.eventTicker = 1;
							}
							else
							{
								noob.stopKickFromRaidTask();
							}
							break;
						}
						case 1:
						{
							if (noob.isInsideZone(L2Character.ZONE_RAID))
							{
								noob.warnAFKRaid();
								noob.eventTicker = 2;
							}
							else
							{
								noob.stopKickFromRaidTask();
							}
							break;
						}
						case 2:
						{
							if (noob.isInsideZone(L2Character.ZONE_RAID))
							{
								noob.broadcastUserInfo();
								noob.kickAFKRaid();
								// noob.eventTicker = 1;
							}
							noob.stopKickFromRaidTask();
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public void stopTempHeroTask()
	{
		if (_removeTempHeroTask != null)
			_removeTempHeroTask.cancel(true);
	}
	
	public void startTempHeroTask(int minutes)
	{
		stopTempHeroTask();
		if (minutes < 1)
			minutes = 1;
		_removeTempHeroTask = ThreadPoolManager.getInstance().scheduleGeneral(new removeTempHeroTask(this), minutes * 60 * 1000);
	}
	
	public class removeTempHeroTask implements Runnable
	{
		L2PcInstance noob = null;
		
		public removeTempHeroTask(L2PcInstance player)
		{
			noob = player;
		}
		
		public void run()
		{
			if (noob != null && noob.isOnline() == 1)
			{
				try
				{
					if (noob._tempHero) // killing streak hero
					{
						if (noob.getPvpKills() < 340000)
						{
							if (!noob.isFakeHero()) // an actual hero from oly
							{
								noob.removeHeroSkillsOnSubclasses(); // just remove skills if they're on a subclass and keep the hero
							}
							else // hero from killing streaks or hero from fos events
							{
								noob.setHero(false);
								noob.setFakeHero(false);
							}
						}
						noob.sendMessage("The effect of temporary hero has worn off.");
						noob._streak = 0;
						_tempHero = false;
						broadcastUserInfo();
					}
				}
				catch (Throwable t)
				{}
			}
		}
	}
	
	// Absorbed Souls
	private int				_souls				= 0;
	// WorldPosition used by TARGET_SIGNET_GROUND
	private Point3D			_currentSkillWorldPosition;
	private L2AccessLevel	_accessLevel;
	private boolean			_messageRefusal		= false;				// message refusal mode
	private boolean			_dietMode			= false;				// ignore weight penalty
	private boolean			_tradeRefusal		= false;				// Trade refusal
	private boolean			_showSkillEffects	= true;					// Trade refusal
	private L2Party			_party;
	// this is needed to find the inviting player for Party response
	// there can only be one active party request at once
	private L2PcInstance	_activeRequester;
	private int				_requestExpireTime	= 0;
	private final L2Request	_request			= new L2Request(this);
	// Used for protection after teleport
	private long			_protectEndTime		= 0;
	
	public boolean isSpawnProtected()
	{
		return _protectEndTime > GameTimeController.getGameTicks() && getKarma() == 0;
	}
	
	// protects a char from agro mobs when getting up from fake death
	private long						_recentFakeDeathEndTime		= 0;
	private boolean						_isFakeDeath;
	/** The fists L2Weapon of the L2PcInstance (used when no weapon is equiped) */
	private L2Weapon					_fistsWeaponItem;
	private final Map<Integer, String>	_chars						= new FastMap<Integer, String>();
	private final Map<String, String>	_chars2						= new FastMap<String, String>();
	// private byte _updateKnownCounter = 0;
	private boolean						_isEnchanting				= false;
	private L2ItemInstance				_activeEnchantItem			= null;
	private L2ItemInstance				_activeEnchantSupportItem	= null;
	private L2ItemInstance				_activeEnchantAttrItem		= null;
	private long						_activeEnchantTimestamp		= 0;
	private boolean						respawnRequested			= false;
	
	public boolean isRespawnRequested()
	{
		return respawnRequested;
	}
	
	public void setIsRespawnRequested(final boolean val)
	{
		respawnRequested = val;
	}
	
	protected boolean						_inventoryDisable		= false;
	protected Map<Integer, L2CubicInstance>	_cubics					= new FastMap<Integer, L2CubicInstance>().shared();
	/** Event parameters */
	public int								eventX;
	public int								eventY;
	public int								eventZ;
	public int								eventkarma;
	public int								eventpvpkills;
	public int								eventpkkills;
	public String							eventTitle;
	public LinkedList<String>				kills					= new LinkedList<String>();
	public boolean							eventSitForced			= false;
	public boolean							atEvent					= false;
	public boolean							wonEvent				= false;
	/** TvT Engine parameters */
	public String							_teamNameTvT;
	public int								_countTvTkills, _countTvTdies, _originalKarmaTvT, _koreanKills, _dominationScore, _dominationKills;
	public boolean							_inEventTvT				= false;
	/** Hunting Grounds Engine parameters */
	public String							_teamNameHG;
	public int								_countHGkills, _countHGdies, _originalKarmaHG;
	public boolean							_inEventHG				= false;
	// test purposes variables for parsed babis event to pride event tvt
	// START
	public boolean							_inEventLunaDomi		= false;
	public String							_teamNameLunaDomi;
	public int								_countLunaDomiKills, _countLunaDomiDies, _originalKarmaLunaDomi, _scoreLunaDomi;
	/** TvT Instanced Engine parameters */
	public int								_originalKarmaTvTi, _countTvTiKills = 0, _countTvTITeamKills = 0;
	public boolean							_inEventTvTi			= false, _isSitForcedTvTi = false, _joiningTvTi = false;
	/** CTF Engine parameters */
	public String							_teamNameCTF, _teamNameHaveFlagCTF;
	public int								_originalKarmaCTF, _countCTFflags, _countCTFkills, _countCTFDeats;
	public boolean							_inEventCTF				= false, _haveFlagCTF = false;
	public Future<?>						_posCheckerCTF			= null;
	/** Fortress Siege Engine parameters */
	public String							_teamNameFOS;
	public int								_countFOSKills, _countFOSdies, _originalKarmaFOS, _countFOSCaps;
	public boolean							_inEventFOS				= false;
	/** VIP parameters */
	public boolean							_isVIP					= false, _inEventVIP = false, _isNotVIP = false, _isTheVIP = false;
	public int								_originalKarmaVIP;
	/** DM Engine parameters */
	public int								_countDMkills, _countDMDeaths, _DMPos, _originalKarmaDM;
	public boolean							_inEventDM				= false;
	/** new loto ticket **/
	private final int						_loto[]					= new int[5];
	// public static int _loto_nums[] = {0,1,2,3,4,5,6,7,8,9,};
	/** new race ticket **/
	private final int						_monsterRace[]			= new int[2];
	private final BlockList					_blockList				= new BlockList();
	private int								_team					= 0;
	/**
	 * lvl of alliance with ketra orcs or varka silenos, used in quests and aggro checks
	 * [-5,-1] varka, 0 neutral, [1,5] ketra
	 */
	private int								_alliedVarkaKetra		= 0;
	private L2Fishing						_fishCombat;
	private boolean							_fishing				= false;
	private int								_fishx					= 0;
	private int								_fishy					= 0;
	private int								_fishz					= 0;
	private ScheduledFuture<?>				_taskRentPet;
	private ScheduledFuture<?>				_taskWater;
	/** Bypass validations */
	private final List<String>				_validBypass			= new FastList<String>();
	private final List<String>				_validBypass2			= new FastList<String>();
	private Forum							_forumMail;
	private Forum							_forumMemo;
	/**
	 * Current skill in use. Note that L2Character has _lastSkillCast, but
	 * this has the button presses
	 */
	private SkillDat						_currentSkill;
	private SkillDat						_currentPetSkill;
	/** Skills queued because a skill is already in progress */
	private SkillDat						_queuedSkill;
	/* Flag to disable equipment/skills while wearing formal wear **/
	private boolean							_IsWearingFormalWear	= false;
	public boolean							_IsCaptchaValidating	= false;
	public int								_cursedWeaponEquippedId	= 0;
	private boolean							_isCombatFlagEquipped	= false;
	private int								_reviveRequested		= 0;
	private double							_revivePower			= 0;
	private boolean							_revivePet				= false;
	private double							_cpUpdateIncCheck		= .0;
	private double							_cpUpdateDecCheck		= .0;
	private double							_cpUpdateInterval		= .0;
	private double							_mpUpdateIncCheck		= .0;
	private double							_mpUpdateDecCheck		= .0;
	private double							_mpUpdateInterval		= .0;
	private boolean							_isRidingStrider		= false;
	private boolean							_isFlyingMounted		= false;
	/** Herbs Task Time **/
	private int								_herbstask				= 0;
	
	/** Task for Herbs */
	public class HerbTask implements Runnable
	{
		private final String	_process;
		private final int		_itemId;
		private final long		_count;
		private final L2Object	_reference;
		private final boolean	_sendMessage;
		
		HerbTask(String process, int itemId, long count, L2Object reference, boolean sendMessage)
		{
			_process = process;
			_itemId = itemId;
			_count = count;
			_reference = reference;
			_sendMessage = sendMessage;
		}
		
		public void run()
		{
			try
			{
				addItem(_process, _itemId, _count, _reference, _sendMessage);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "", e);
			}
		}
	}
	
	/** ShortBuff clearing Task */
	ScheduledFuture<?> _shortBuffTask = null;
	
	public class ShortBuffTask implements Runnable
	{
		private L2PcInstance _player = null;
		
		public ShortBuffTask(L2PcInstance activeChar)
		{
			_player = activeChar;
		}
		
		public void run()
		{
			if (_player == null)
				return;
			_player.sendPacket(new ShortBuffStatusUpdate(0, 0, 0));
			setShortBuffTaskSkillId(0);
		}
	}
	
	// L2JMOD Wedding
	private boolean	_married		= false;
	private int		_partnerId		= 0;
	private int		_coupleId		= 0;
	private boolean	_engagerequest	= false;
	private int		_engageid		= 0;
	private boolean	_marryrequest	= false;
	private boolean	_marryaccepted	= false;
	
	/** Skill casting information (used to queue when several skills are cast in a short time) **/
	public class SkillDat
	{
		private final L2Skill	_skill;
		private final boolean	_ctrlPressed;
		private final boolean	_shiftPressed;
		
		protected SkillDat(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
		{
			_skill = skill;
			_ctrlPressed = ctrlPressed;
			_shiftPressed = shiftPressed;
		}
		
		public boolean isCtrlPressed()
		{
			return _ctrlPressed;
		}
		
		public boolean isShiftPressed()
		{
			return _shiftPressed;
		}
		
		public L2Skill getSkill()
		{
			return _skill;
		}
		
		public int getSkillId()
		{
			return (getSkill() != null) ? getSkill().getId() : -1;
		}
	}
	
	// summon friend
	private final summonRequest _summonRequest = new summonRequest();
	
	public class summonRequest
	{
		private L2PcInstance	_target	= null;
		private L2Skill			_skill	= null;
		
		public void setTarget(L2PcInstance destination, L2Skill skill)
		{
			_target = destination;
			_skill = skill;
		}
		
		public L2PcInstance getTarget()
		{
			return _target;
		}
		
		public L2Skill getSkill()
		{
			return _skill;
		}
	}
	
	// open/close gates
	private final gatesRequest _gatesRequest = new gatesRequest();
	
	public class gatesRequest
	{
		private L2DoorInstance _target = null;
		
		public void setTarget(L2DoorInstance door)
		{
			_target = door;
		}
		
		public L2DoorInstance getDoor()
		{
			return _target;
		}
	}
	
	/**
	 * Create a new L2PcInstance and add it in the characters table of the database.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Create a new L2PcInstance with an account name</li>
	 * <li>Set the name, the Hair Style, the Hair Color and the Face type of the L2PcInstance</li>
	 * <li>Add the player in the characters table of the database</li><BR>
	 * <BR>
	 *
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param template
	 *            The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName
	 *            The name of the L2PcInstance
	 * @param name
	 *            The name of the L2PcInstance
	 * @param hairStyle
	 *            The hair style Identifier of the L2PcInstance
	 * @param hairColor
	 *            The hair color Identifier of the L2PcInstance
	 * @param face
	 *            The face type Identifier of the L2PcInstance
	 * @return The L2PcInstance added to the database or null
	 */
	public static L2PcInstance create(int objectId, L2PcTemplate template, String accountName, String name, byte hairStyle, byte hairColor, byte face, boolean sex)
	{
		// Create a new L2PcInstance with an account name
		PcAppearance app = new PcAppearance(face, hairColor, hairStyle, sex);
		L2PcInstance player = new L2PcInstance(objectId, template, accountName, app);
		// Set the name of the L2PcInstance
		player.setName(name);
		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getClassId());
		// Kept for backwards compabitility.
		player.setNewbie(1);
		player.setCharCreatedTime(System.currentTimeMillis());
		// Add the player in the characters table of the database
		boolean ok = player.createDb();
		player.createDbForClassPaths();
		if (!ok)
			return null;
		return player;
	}
	
	public static L2PcInstance createDummyPlayer(int objectId, String name)
	{
		// Create a new L2PcInstance with an account name
		L2PcInstance player = new L2PcInstance(objectId);
		player.setName(name);
		return player;
	}
	
	public String getAccountName()
	{
		if ((getClient() == null || getClient().isDetached()) && !(this instanceof Ghost))
			return "disconnected";
		if (this instanceof Ghost)
			return this._accountName;
		else
			return getClient().getAccountName();
	}
	
	public String getAccountNameIgnoreDetached()
	{
		if ((getClient() == null || getClient().isDetached()) && !(this instanceof Ghost))
			return "disconnected";
		if (this instanceof Ghost)
			return this._accountName;
		else
			return getClient().getAccountName();
	}
	
	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}
	
	public Map<String, String> getHwidChars()
	{
		return _chars2;
	}
	/*
	 * public Future<?> getPartyPassiveTask()
	 * {
	 * return _partyPassiveTask;
	 * }
	 * public void stopPartyPassiveTask(boolean interruptifrunning)
	 * {
	 * if (_partyPassiveTask != null)
	 * _partyPassiveTask.cancel(interruptifrunning);
	 * }
	 * private Future<?> _partyPassiveTask;
	 * class PartyPassive implements Runnable
	 * {
	 * final L2PcInstance _owner;
	 * public PartyPassive(L2PcInstance owner)
	 * {
	 * _owner = owner;
	 * }
	 * public void run()
	 * {
	 * try
	 * {
	 * if (_owner == null || _partyPassiveList == null || _partyPassiveList.isEmpty())
	 * return;
	 * final L2Party party = _owner.getParty();
	 * if (party == null)
	 * return;
	 * int distance;
	 * int skillRadius;
	 * for (L2PcInstance partyMember : party.getPartyMembers())
	 * {
	 * if (partyMember == null || partyMember == _owner)
	 * continue;
	 * distance = (int) Util.calculateDistance(_owner, partyMember, true);
	 * for (L2Skill skill : _partyPassiveList)
	 * {
	 * skillRadius = skill.getPartyPassiveRadius();
	 * if (skillRadius <= 0)
	 * {
	 * _log.log(Level.WARNING, skill.getName()+" has error in skill party passive radius");
	 * }
	 * else
	 * {
	 * if (distance <= skillRadius)
	 * {
	 * if (partyMember.getKnownSkill(skill.getId()) == null)
	 * partyMember.addSkillPartyPassive(skill);
	 * }
	 * else
	 * {
	 * partyMember.removeSkillPartyPassive(skill.getId(), _owner.getObjectId());
	 * }
	 * }
	 * }
	 * }
	 * }
	 * catch (Exception e)
	 * {
	 * _log.log(Level.WARNING, "error in party passive task:", e);
	 * }
	 * }
	 * }
	 * public synchronized void addSkillToPartyPassiveList(final L2Skill skill)
	 * {
	 * if (_partyPassiveTask != null)
	 * _partyPassiveTask.cancel(false);
	 * if (_partyPassiveList == null)
	 * _partyPassiveList = new FastList<L2Skill>();
	 * _partyPassiveList.addLast(skill);
	 * _partyPassiveTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PartyPassive(this), 2000, 8000);
	 * }
	 * public synchronized void removeSkillFromPartyPassiveList(final L2Skill skill)
	 * {
	 * if (_partyPassiveList == null || _partyPassiveList.isEmpty())
	 * {
	 * if (_partyPassiveTask != null)
	 * _partyPassiveTask.cancel(false);
	 * return;
	 * }
	 * _partyPassiveList.remove(skill);
	 * if (_partyPassiveList.isEmpty())
	 * {
	 * if (_partyPassiveTask != null)
	 * _partyPassiveTask.cancel(true);
	 * }
	 * final L2Party party = getParty();
	 * if (party == null) return;
	 * for (L2PcInstance player : party.getPartyMembers())
	 * {
	 * if (player == null || player == this)
	 * continue;
	 * player.removeSkillPartyPassive(skill.getId(), getObjectId());
	 * }
	 * }
	 * public FastList<L2Skill> getPartyPassiveList()
	 * {
	 * return _partyPassiveList;
	 * }
	 */
	
	private Future<?>	_PvPRegTask;
	private long		_pvpFlagLasts;
	
	class PvPFlag implements Runnable
	{
		public void run()
		{
			try
			{
				if (System.currentTimeMillis() > getPvpFlagLasts())
				{
					stopPvPFlag();
				}
				else if (System.currentTimeMillis() > (getPvpFlagLasts() - 10000))
				{
					if (getPvpFlag() != 2)
						updatePvPFlag(2);
				}
				else
				{
					if (getPvpFlag() != 1)
						updatePvPFlag(1);
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "error in pvp flag task:", e);
			}
		}
	}
	
	public void setPvpFlagLasts(long time)
	{
		_pvpFlagLasts = time;
	}
	
	public long getPvpFlagLasts()
	{
		return _pvpFlagLasts;
	}
	
	public void startPvPFlag()
	{
		updatePvPFlag(1);
		_PvPRegTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PvPFlag(), 1000, 1000);
	}
	
	public void stopPvpRegTask()
	{
		if (_PvPRegTask != null)
			_PvPRegTask.cancel(true);
	}
	
	public void stopPvPFlag()
	{
		try
		{
			for (L2Character player : getKnownList().getKnownCharacters())
			{
				if (player instanceof L2Playable)
				{
					if (player.getActingPlayer() != this)
					{
						if (player.getAI().getAttackTarget() != null && player.getAI().getAttackTarget() == this)
						{
							if (!isAutoAttackableIgnoreFlagging(player))
							{
								player.abortAttack();
								player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
							}
						}
						else if (player.getAI().getCastTarget() != null && player.getAI().getCastTarget() == this && player.getAI().getSkill().isOffensive())
						{
							if (!isAutoAttackableIgnoreFlagging(player))
							{
								player.abortCast();
								player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{}
		stopPvpRegTask();
		updatePvPFlag(0);
		_PvPRegTask = null;
	}
	
	public boolean isAFOSLeader()
	{
		if ((FOS._started) && _inEventFOS)
		{
			if (FOS._team1Sealers.contains(getObjectId()) || FOS._team2Sealers.contains(getObjectId()))
				return true;
		}
		if ((NewFOS._started) && _inEventFOS)
		{
			if (NewFOS._team1Sealers.contains(getObjectId()) || NewFOS._team2Sealers.contains(getObjectId()))
				return true;
		}
		return false;
	}
	
	public int getRelation(L2PcInstance target)
	{
		int result = 0;
		final boolean fos = (FOS._started || NewFOS._started) && _inEventFOS && target._inEventFOS;
		if (fos)
		{
			if (FOS._started)
			{
				if (_teamNameFOS.equals(target._teamNameFOS))
					result |= RelationChanged.RELATION_CLAN_MEMBER;
				if (FOS._team1Sealers.contains(getObjectId()) || FOS._team2Sealers.contains(getObjectId()))
					result |= RelationChanged.RELATION_LEADER;
			}
			if (NewFOS._started)
			{
				if (_teamNameFOS.equals(target._teamNameFOS))
					result |= RelationChanged.RELATION_CLAN_MEMBER;
				if (NewFOS._team1Sealers.contains(getObjectId()) || NewFOS._team2Sealers.contains(getObjectId()))
					result |= RelationChanged.RELATION_LEADER;
			}
		}
		else
		{
			if (isDisguised())
				return 0;
			if (getClan() != null)
				result |= RelationChanged.RELATION_CLAN_MEMBER;
			if (isClanLeader())
				result |= RelationChanged.RELATION_LEADER;
		}
		if (getParty() != null && getParty() == target.getParty())
		{
			result |= RelationChanged.RELATION_HAS_PARTY;
			for (int i = 0; i < getParty().getPartyMembers().size(); i++)
			{
				if (getParty().getPartyMembers().get(i) != this)
					continue;
				switch (i)
				{
					case 0:
						result |= RelationChanged.RELATION_PARTYLEADER; // 0x10
						break;
					case 1:
						result |= RelationChanged.RELATION_PARTY4; // 0x8
						break;
					case 2:
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1; // 0x7
						break;
					case 3:
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2; // 0x6
						break;
					case 4:
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY1; // 0x5
						break;
					case 5:
						result |= RelationChanged.RELATION_PARTY3; // 0x4
						break;
					case 6:
						result |= RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1; // 0x3
						break;
					case 7:
						result |= RelationChanged.RELATION_PARTY2; // 0x2
						break;
					case 8:
						result |= RelationChanged.RELATION_PARTY1; // 0x1
						break;
				}
			}
		}
		if (getSiegeState() != 0)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			if (getSiegeState() != target.getSiegeState())
				result |= RelationChanged.RELATION_ENEMY;
			else
				result |= RelationChanged.RELATION_ALLY;
			if (getSiegeState() == 1)
				result |= RelationChanged.RELATION_ATTACKER;
		}
		if (!fos)
		{
			if (getClan() != null && target.getClan() != null)
			{
				if (target.getClan().isAtWarWith(getClan().getClanId()))
				{
					result |= RelationChanged.RELATION_1SIDED_WAR;
					if (getClan().isAtWarWith(target.getClan().getClanId()))
						result |= RelationChanged.RELATION_MUTUAL_WAR;
				}
			}
		}
		return result;
	}
	
	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world (call restore method).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Retrieve the L2PcInstance from the characters table of the database</li>
	 * <li>Add the L2PcInstance object in _allObjects</li>
	 * <li>Set the x,y,z position of the L2PcInstance and make it invisible</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR>
	 * <BR>
	 *
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @return The L2PcInstance loaded from the database
	 */
	public static L2PcInstance load(int objectId)
	{
		return restore(objectId);
	}
	
	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}
	
	/**
	 * Constructor of L2PcInstance (use L2Character constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and copy basic Calculator set to this L2PcInstance</li>
	 * <li>Set the name of the L2PcInstance</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the L2PcInstance to 1</B></FONT><BR>
	 * <BR>
	 *
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param template
	 *            The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName
	 *            The name of the account including this L2PcInstance
	 */
	protected L2PcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template);
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();
		_accountName = accountName;
		app.setOwner(this);
		_appearance = app;
		// Create an AI
		_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
		// Create a L2Radar object
		_radar = new L2Radar(this);
		_passport = PassportManager.getInstance().fetch(this);
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills
		// Retrieve from the database all items of this L2PcInstance and add them to _inventory
		getInventory().restore();
		if (!Config.WAREHOUSE_CACHE)
			getWarehouse();
		getFreight();
		buffSchemes = new CopyOnWriteArrayList<>();
	}
	
	private L2PcInstance(int objectId)
	{
		super(objectId, null);
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();
		_passport = PassportManager.getInstance().fetch(this);
		buffSchemes = new CopyOnWriteArrayList<>();
	}
	
	@Override
	public final PcKnownList getKnownList()
	{
		return (PcKnownList) super.getKnownList();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new PcKnownList(this));
	}
	
	@Override
	public final PcStat getStat()
	{
		return (PcStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PcStat(this));
	}
	
	@Override
	public final PcStatus getStatus()
	{
		return (PcStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new PcStatus(this));
	}
	
	@Override
	public PcPosition getPosition()
	{
		return (PcPosition) super.getPosition();
	}
	
	@Override
	public void initPosition()
	{
		setObjectPosition(new PcPosition(this));
	}
	
	public final PcAppearance getAppearance()
	{
		return _appearance;
	}
	
	/**
	 * Return the base L2PcTemplate link to the L2PcInstance.<BR>
	 * <BR>
	 */
	public final L2PcTemplate getBaseTemplate()
	{
		return CharTemplateTable.getInstance().getTemplate(_baseClass);
	}
	
	/** Return the L2PcTemplate link to the L2PcInstance. */
	@Override
	public final L2PcTemplate getTemplate()
	{
		return (L2PcTemplate) super.getTemplate();
	}
	
	public void setTemplate(ClassId newclass)
	{
		super.setTemplate(CharTemplateTable.getInstance().getTemplate(newclass));
	}
	
	/**
	 * Return the AI of the L2PcInstance (create it if necessary).<BR>
	 * <BR>
	 */
	@Override
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai; // copy handle
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
				return _ai;
			}
		}
		return ai;
	}
	
	/**
	 * Calculate a destination to explore the area and set the AI Intention to AI_INTENTION_MOVE_TO.<BR>
	 * <BR>
	 */
	public void explore()
	{
		if (!_exploring)
			return;
		if (getMountType() == 2)
			return;
		// Calculate the destination point (random)
		int x = getX() + Rnd.nextInt(6000) - 3000;
		int y = getY() + Rnd.nextInt(6000) - 3000;
		if (x > Universe.MAX_X)
			x = Universe.MAX_X;
		if (x < Universe.MIN_X)
			x = Universe.MIN_X;
		if (y > Universe.MAX_Y)
			y = Universe.MAX_Y;
		if (y < Universe.MIN_Y)
			y = Universe.MIN_Y;
		int z = getZ();
		L2CharPosition pos = new L2CharPosition(x, y, z, 0);
		// Set the AI Intention to AI_INTENTION_MOVE_TO
		getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, pos);
	}
	
	@Override
	public final int getLevel(boolean checkOly)
	{
		if (checkOly && (isInOlympiadMode() || isInFunEvent()))
			return Math.min(getStat().getLevel(), 85);
		return getStat().getLevel();
	}
	
	@Override
	public final int getLevel()
	{
		return getLevel(false);
	}
	
	/**
	 * Return the _newbie rewards state of the L2PcInstance.<BR>
	 * <BR>
	 */
	public int getNewbie()
	{
		return _newbie;
	}
	
	public boolean isBeta()
	{
		return _beta;
	}
	
	public boolean isVip()
	{
		return _vip;
	}
	
	/**
	 * Set the _newbie rewards state of the L2PcInstance.<BR>
	 * <BR>
	 *
	 * @param newbieRewards
	 *            The Identifier of the _newbie state<BR>
	 *            <BR>
	 */
	public void setNewbie(int newbieRewards)
	{
		_newbie = newbieRewards;
	}
	
	public void setBeta(boolean b)
	{
		_beta = b;
	}
	
	public void setVip(boolean b)
	{
		_vip = b;
	}
	
	public void setVipPass(boolean b)
	{
		_vipPass = b;
	}
	
	public boolean isVipPass()
	{
		return _vipPass;
	}
	
	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}
	
	public void setBaseClass(ClassId classId)
	{
		_baseClass = classId.ordinal();
	}
	
	public boolean isInStoreMode()
	{
		return (getPrivateStoreType() > 0);
	}
	// public boolean isInCraftMode() { return (getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE); }
	
	public boolean isInCraftMode()
	{
		return _inCraftMode;
	}
	
	public void isInCraftMode(boolean b)
	{
		_inCraftMode = b;
	}
	
	/**
	 * Manage Logout Task.<BR>
	 * <BR>
	 */
	public void logout()
	{
		if (getInventory().getItemByItemId(9819) != null)
		{
			Fort fort = FortManager.getInstance().getFort(this);
			if (fort != null)
				FortSiegeManager.getInstance().dropCombatFlag(this);
			else
			{
				int slot = getInventory().getSlotFromItem(getInventory().getItemByItemId(9819));
				getInventory().unEquipItemInBodySlotAndRecord(slot);
				destroyItem("CombatFlag", getInventory().getItemByItemId(9819), null, true);
			}
		}
		closeNetConnection();
	}
	
	/**
	 * Return a table containing all Common L2RecipeList of the L2PcInstance.<BR>
	 * <BR>
	 */
	public L2RecipeList[] getCommonRecipeBook()
	{
		return _commonRecipeBook.values().toArray(new L2RecipeList[_commonRecipeBook.values().size()]);
	}
	
	/**
	 * Return a table containing all Dwarf L2RecipeList of the L2PcInstance.<BR>
	 * <BR>
	 */
	public L2RecipeList[] getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values().toArray(new L2RecipeList[_dwarvenRecipeBook.values().size()]);
	}
	
	/**
	 * Add a new L2RecipList to the table _commonrecipebook containing all L2RecipeList of the L2PcInstance <BR>
	 * <BR>
	 *
	 * @param recipe
	 *            The L2RecipeList to add to the _recipebook
	 */
	public void registerCommonRecipeList(L2RecipeList recipe, boolean saveToDb)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
		if (saveToDb)
			insertNewRecipeData(recipe.getId(), false);
	}
	
	/**
	 * Add a new L2RecipList to the table _recipebook containing all L2RecipeList of the L2PcInstance <BR>
	 * <BR>
	 *
	 * @param recipe
	 *            The L2RecipeList to add to the _recipebook
	 */
	public void registerDwarvenRecipeList(L2RecipeList recipe, boolean saveToDb)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
		if (saveToDb)
			insertNewRecipeData(recipe.getId(), true);
	}
	
	/**
	 * @param RecipeID
	 *            The Identifier of the L2RecipeList to check in the player's recipe books
	 * @return
	 *         <b>TRUE</b> if player has the recipe on Common or Dwarven Recipe book else returns <b>FALSE</b>
	 */
	public boolean hasRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
			return true;
		else if (_commonRecipeBook.containsKey(recipeId))
			return true;
		else
			return false;
	}
	
	/**
	 * Tries to remove a L2RecipList from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all L2RecipeList of the L2PcInstance <BR>
	 * <BR>
	 *
	 * @param RecipeID
	 *            The Identifier of the L2RecipeList to remove from the _recipebook
	 */
	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.remove(recipeId) != null)
			deleteRecipeData(recipeId, true);
		else if (_commonRecipeBook.remove(recipeId) != null)
			deleteRecipeData(recipeId, false);
		else
			_log.warning("Attempted to remove unknown RecipeList: " + recipeId);
		L2ShortCut[] allShortCuts = getAllShortCuts();
		for (L2ShortCut sc : allShortCuts)
		{
			if (sc != null && sc.getId() == recipeId && sc.getType() == L2ShortCut.TYPE_RECIPE)
				deleteShortCut(sc.getSlot(), sc.getPage());
		}
	}
	
	private void insertNewRecipeData(int recipeId, boolean isDwarf)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_recipebook (charId, id, classIndex, type) values(?,?,?,?)");
			statement.setInt(1, getObjectId());
			statement.setInt(2, recipeId);
			statement.setInt(3, isDwarf ? _classIndex : 0);
			statement.setInt(4, isDwarf ? 1 : 0);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			if (_log.isLoggable(Level.SEVERE))
				_log.log(Level.SEVERE, "SQL exception while inserting recipe: " + recipeId + " from character " + getObjectId(), e);
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
	
	private void deleteRecipeData(int recipeId, boolean isDwarf)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=? AND id=? AND classIndex=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, recipeId);
			statement.setInt(3, isDwarf ? _classIndex : 0);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			if (_log.isLoggable(Level.SEVERE))
				_log.log(Level.SEVERE, "SQL exception while deleting recipe: " + recipeId + " from character " + getObjectId(), e);
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
	
	/**
	 * Returns the Id for the last talked quest NPC.<BR>
	 * <BR>
	 */
	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}
	
	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}
	
	/**
	 * Return the QuestState object corresponding to the quest name.<BR>
	 * <BR>
	 *
	 * @param quest
	 *            The name of the quest
	 */
	public QuestState getQuestState(final String name)
	{
		for (final QuestState qs : _quests)
			if (name.equals(qs.getQuest().getName()))
				return qs;
		return null;
	}
	
	/**
	 * Add a QuestState to the table _quest containing all quests began by the L2PcInstance.<BR>
	 * <BR>
	 *
	 * @param qs
	 *            The QuestState to add to _quest
	 */
	public void setQuestState(final QuestState qs)
	{
		_quests.add(qs);
	}
	
	/**
	 * Remove a QuestState from the table _quest containing all quests began by the L2PcInstance.<BR>
	 * <BR>
	 *
	 * @param quest
	 *            The name of the quest
	 */
	public void delQuestState(final QuestState qs)
	{
		_quests.remove(qs);
	}
	
	private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state)
	{
		int len = questStateArray.length;
		QuestState[] tmp = new QuestState[len + 1];
		for (int i = 0; i < len; i++)
			tmp[i] = questStateArray[i];
		tmp[len] = state;
		return tmp;
	}
	
	/**
	 * Return a table containing all Quest in progress from the table _quests.<BR>
	 * <BR>
	 */
	public Quest[] getAllActiveQuests()
	{
		FastList<Quest> quests = new FastList<Quest>();
		_quests.forEach(qs ->
		{
			if (qs != null || qs.getQuest() != null)
			{
				int questId = qs.getQuest().getQuestId();
				if ((questId > 19999) || (questId < 1))
				{
					if (!qs.isStarted() && !Config.DEVELOPER)
					{
						quests.add(qs.getQuest());
					}
				}
			}
		});
		return quests.toArray(new Quest[quests.size()]);
	}
	
	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR>
	 * <BR>
	 *
	 * @param npcId
	 *            The Identifier of the L2Attackable attacked
	 */
	public QuestState[] getQuestsForAttacks(L2Npc npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(QuestEventType.ON_ATTACK))
		{
			// Check if the Identifier of the L2Attackable attck is needed for the current quest
			if (getQuestState(quest.getName()) != null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
					states = new QuestState[]
					{
						getQuestState(quest.getName())
					};
				else
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
			}
		}
		// Return a table containing all QuestState to modify
		return states;
	}
	
	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR>
	 * <BR>
	 *
	 * @param npcId
	 *            The Identifier of the L2Attackable killed
	 */
	public QuestState[] getQuestsForKills(L2Npc npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(QuestEventType.ON_KILL))
		{
			// Check if the Identifier of the L2Attackable killed is needed for the current quest
			if (getQuestState(quest.getName()) != null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
					states = new QuestState[]
					{
						getQuestState(quest.getName())
					};
				else
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
			}
		}
		// Return a table containing all QuestState to modify
		return states;
	}
	
	/**
	 * Return a table containing all QuestState from the table _quests in which the L2PcInstance must talk to the NPC.<BR>
	 * <BR>
	 *
	 * @param npcId
	 *            The Identifier of the NPC
	 */
	public QuestState[] getQuestsForTalk(int npcId)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		// Go through the QuestState of the L2PcInstance quests
		List<Quest> quests = NpcTable.getInstance().getTemplate(npcId).getEventQuests(QuestEventType.ON_TALK);
		if (quests != null)
		{
			for (Quest quest : quests)
			{
				if (quest != null)
				{
					// Copy the current L2PcInstance QuestState in the QuestState table
					if (getQuestState(quest.getName()) != null)
					{
						if (states == null)
							states = new QuestState[]
							{
								getQuestState(quest.getName())
							};
						else
							states = addToQuestStateArray(states, getQuestState(quest.getName()));
					}
				}
			}
		}
		// Return a table containing all QuestState to modify
		return states;
	}
	
	public void processQuestEvent(final String questName, final String event)
	{
		final Quest quest = QuestManager.getInstance().getQuest(questName);
		if (quest == null)
			return;
		final QuestState qs = getQuestState(questName);
		if (qs == null)
			return;
		final L2Object object = L2World.getInstance().findObject(getLastQuestNpcObject());
		if (!(object instanceof L2Npc) || !isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
			return;
		final L2Npc npc = (L2Npc) object;
		final List<Quest> quests = npc.getTemplate().getEventQuests(QuestEventType.ON_TALK);
		if (quests != null)
			for (final Quest onTalk : quests)
			{
				if (onTalk == null || onTalk != quest)
					continue;
				quest.notifyEvent(event, npc, this);
				break;
			}
	}
	
	private void showQuestWindow(String questId, String stateId)
	{
		String path = "data/scripts/quests/" + questId + "/" + stateId + ".htm";
		String content = HtmCache.getInstance().getHtm(path); // TODO path for quests html
		if (content != null)
		{
			if (Config.DEBUG)
				_log.fine("Showing quest window for quest " + questId + " state " + stateId + " html path: " + path);
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			sendPacket(npcReply);
		}
		sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/** List of all QuestState instance that needs to be notified of this L2PcInstance's or its pet's death */
	private List<QuestState>	_NotifyQuestOfDeathList	= new FastList<QuestState>();
	private List<QuestState>	_NotifyQuestOfKillList	= new FastList<QuestState>();
	
	/**
	 * Add QuestState instance that is to be notified of L2PcInstance's death.<BR>
	 * <BR>
	 *
	 * @param qs
	 *            The QuestState that subscribe to this event
	 */
	public void addNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null || _NotifyQuestOfDeathList.contains(qs))
			return;
		_NotifyQuestOfDeathList.add(qs);
	}
	
	/**
	 * Remove QuestState instance that is to be notified of L2PcInstance's death.<BR>
	 * <BR>
	 *
	 * @param qs
	 *            The QuestState that subscribe to this event
	 */
	public void removeNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null || !_NotifyQuestOfDeathList.contains(qs))
			return;
		_NotifyQuestOfDeathList.remove(qs);
	}
	
	/**
	 * Return a list of QuestStates which registered for notify of death of this L2PcInstance.<BR>
	 * <BR>
	 */
	public final List<QuestState> getNotifyQuestOfDeath()
	{
		if (_NotifyQuestOfDeathList == null)
			_NotifyQuestOfDeathList = new FastList<QuestState>();
		return _NotifyQuestOfDeathList;
	}
	
	public void addNotifyQuestOfKill(QuestState qs)
	{
		if (qs == null || _NotifyQuestOfKillList.contains(qs))
			return;
		_NotifyQuestOfKillList.add(qs);
	}
	
	public void removeNotifyQuestOfKill(QuestState qs)
	{
		if (qs == null || !_NotifyQuestOfKillList.contains(qs))
			return;
		_NotifyQuestOfKillList.remove(qs);
	}
	
	public final List<QuestState> getNotifyQuestOfKill()
	{
		if (_NotifyQuestOfKillList == null)
			_NotifyQuestOfKillList = new FastList<QuestState>();
		return _NotifyQuestOfKillList;
	}
	
	/**
	 * Return a table containing all L2ShortCut of the L2PcInstance.<BR>
	 * <BR>
	 */
	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}
	
	/**
	 * Return the L2ShortCut of the L2PcInstance corresponding to the position (page-slot).<BR>
	 * <BR>
	 *
	 * @param slot
	 *            The slot in wich the shortCuts is equiped
	 * @param page
	 *            The page of shortCuts containing the slot
	 */
	public L2ShortCut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}
	
	/**
	 * Add a L2shortCut to the L2PcInstance _shortCuts<BR>
	 * <BR>
	 */
	public void registerShortCut(L2ShortCut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}
	
	public void registerShortCut(L2ShortCut shortcut, boolean storeToDb)
	{
		_shortCuts.registerShortCut(shortcut, storeToDb);
	}
	
	/**
	 * Delete the L2ShortCut corresponding to the position (page-slot) from the L2PcInstance _shortCuts.<BR>
	 * <BR>
	 */
	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}
	
	public void deleteShortCut(int slot, int page, boolean fromDb)
	{
		_shortCuts.deleteShortCut(slot, page, fromDb);
	}
	
	public void restoreShortCuts()
	{
		_shortCuts.restore();
	}
	
	public void removeAllShortcuts()
	{
		_shortCuts.tempRemoveAll();
	}
	
	/**
	 * Add a L2Macro to the L2PcInstance _macroses<BR>
	 * <BR>
	 */
	public void registerMacro(L2Macro macro)
	{
		_macroses.registerMacro(macro);
	}
	
	/**
	 * Delete the L2Macro corresponding to the Identifier from the L2PcInstance _macroses.<BR>
	 * <BR>
	 */
	public void deleteMacro(int id)
	{
		_macroses.deleteMacro(id);
	}
	
	/**
	 * Return all L2Macro of the L2PcInstance.<BR>
	 * <BR>
	 */
	public MacroList getMacroses()
	{
		return _macroses;
	}
	
	/**
	 * Set the siege state of the L2PcInstance.<BR>
	 * <BR>
	 * 1 = attacker, 2 = defender, 0 = not involved
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}
	
	/**
	 * Get the siege state of the L2PcInstance.<BR>
	 * <BR>
	 * 1 = attacker, 2 = defender, 0 = not involved
	 */
	public byte getSiegeState()
	{
		return _siegeState;
	}
	
	/**
	 * Set the PvP Flag of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte) pvpFlag;
	}
	
	public byte getPvpFlag()
	{
		return _pvpFlag;
	}
	
	public void updatePvPFlag(int value)
	{
		if (getPvpFlag() == value)
			return;
		setPvpFlag(value);
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		// If this player has a pet update the pets pvp flag as well
		if (getPet() != null)
			sendPacket(new RelationChanged(getPet(), getRelation(this), false));
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		// synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance target : plrs)
			{
				target.sendPacket(new RelationChanged(this, getRelation(this), isAutoAttackable(target)));
				if (getPet() != null)
					target.sendPacket(new RelationChanged(getPet(), getRelation(this), isAutoAttackable(target)));
			}
		}
	}
	
	@Override
	public void revalidateZone(boolean force)
	{
		// Cannot validate if not in a world region (happens during teleport)
		if (getWorldRegion() == null)
			return;
		// This function is called too often from movement code
		if (force)
			_zoneValidateCounter = 4;
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter < 0)
				_zoneValidateCounter = 4;
			else
				return;
		}
		getWorldRegion().revalidateZones(this);
		if (Config.ALLOW_WATER)
			checkWaterState();
		if (isInsideZone(ZONE_SIEGE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
				return;
			_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2);
			sendPacket(cz);
		}
		else if (isInsideZone(ZONE_PVP))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE)
				return;
			_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE);
			sendPacket(cz);
		}
		else if (isIn7sDungeon())
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE)
				return;
			_lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE);
			sendPacket(cz);
		}
		else if (isInsideZone(ZONE_PEACE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE)
				return;
			_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE);
			sendPacket(cz);
		}
		else
		{
			if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE)
				return;
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
				updatePvPStatus();
			_lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE);
			sendPacket(cz);
		}
	}
	
	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR>
	 * <BR>
	 */
	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN) >= 1;
	}
	
	public int getDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN);
	}
	
	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR>
	 * <BR>
	 */
	public boolean hasCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON) >= 1;
	}
	
	public int getCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON);
	}
	
	/**
	 * Return the PK counter of the L2PcInstance.<BR>
	 * <BR>
	 */
	public int getPkKills()
	{
		return _pkKills;
	}
	
	/**
	 * Set the PK counter of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}
	
	/**
	 * Return the _deleteTimer of the L2PcInstance.<BR>
	 * <BR>
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}
	
	/**
	 * Set the _deleteTimer of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	/**
	 * Return the current weight of the L2PcInstance.<BR>
	 * <BR>
	 */
	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}
	
	/**
	 * Return date of las update of recomPoints
	 */
	public long getCharCreatedTime()
	{
		return _charCreationTime;
	}
	
	public void setCharCreatedTime(long date)
	{
		_charCreationTime = date;
	}
	
	/**
	 * Return the number of recommandation obtained by the L2PcInstance.<BR>
	 * <BR>
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}
	
	/**
	 * Increment the number of recommandation obtained by the L2PcInstance (Max : 255).<BR>
	 * <BR>
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
			_recomHave++;
	}
	
	/**
	 * Set the number of recommandation obtained by the L2PcInstance (Max : 255).<BR>
	 * <BR>
	 */
	public void setRecomHave(int value)
	{
		if (value > 255)
			_recomHave = 255;
		else if (value < 0)
			_recomHave = 0;
		else
			_recomHave = value;
	}
	
	/**
	 * Return the number of recommandation that the L2PcInstance can give.<BR>
	 * <BR>
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}
	
	/**
	 * Increment the number of recommandation that the L2PcInstance can give.<BR>
	 * <BR>
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
			_recomLeft--;
	}
	
	public void giveRecom(L2PcInstance target)
	{
		if (Config.ALT_RECOMMEND)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(ADD_CHAR_RECOM);
				statement.setInt(1, getObjectId());
				statement.setInt(2, target.getObjectId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed updating character recommendations.", e);
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
		target.incRecomHave();
		decRecomLeft();
		_recomChars.add(target.getObjectId());
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			getCounters().recommendsMade++;
		}
	}
	
	public boolean canRecom(L2PcInstance target)
	{
		return !_recomChars.contains(target.getObjectId());
	}
	
	/**
	 * Set the exp of the L2PcInstance before a death
	 * 
	 * @param exp
	 */
	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}
	
	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}
	
	/**
	 * Return the Karma of the L2PcInstance.<BR>
	 * <BR>
	 */
	public int getKarma()
	{
		return _karma;
	}
	
	/**
	 * Set the Karma of the L2PcInstance and send a Server->Client packet StatusUpdate (broadcast).<BR>
	 * <BR>
	 */
	public void setKarma(int karma)
	{
		if (karma < 0)
			karma = 0;
		if (_karma == 0 && karma > 0)
		{
			Collection<L2Object> objs = getKnownList().getKnownObjects().values();
			{
				for (L2Object object : objs)
				{
					if (!(object instanceof L2GuardInstance))
						continue;
					if (((L2GuardInstance) object).getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
						((L2GuardInstance) object).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		else if (_karma > 0 && karma == 0)
		{
			// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast)
			setKarmaFlag(0);
			for (L2Character player : getKnownList().getKnownCharacters())
			{
				if (player != null && player instanceof L2Playable)
				{
					if (player.getActingPlayer() != this)
					{
						if (player.getAI().getAttackTarget() != null && player.getAI().getAttackTarget() == this)
						{
							if (!isAutoAttackableIgnoreFlagging(player))
							{
								player.abortAttack();
								player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
							}
						}
						else if (player.getAI().getCastTarget() != null && player.getAI().getCastTarget() == this && player.getAI().getSkill().isOffensive())
						{
							if (!isAutoAttackableIgnoreFlagging(player))
							{
								player.abortCast();
								player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
							}
						}
					}
				}
			}
		}
		_karma = karma;
		broadcastKarma();
	}
	
	/**
	 * Return the max weight that the L2PcInstance can load.<BR>
	 * <BR>
	 */
	public int getMaxLoad()
	{
		return 131000;
	}
	
	public int getExpertisePenalty()
	{
		return 0;
	}
	
	public int getWeightPenalty()
	{
		return 0;
	}
	
	public void checkIfWeaponIsAllowed()
	{
		// Override for Gamemasters
		if (isGM())
			return;
		// Iterate through all effects currently on the character.
		for (L2Effect currenteffect : getAllEffects())
		{
			L2Skill effectSkill = currenteffect.getSkill();
			// Ignore all buff skills that are party related (ie. songs, dances) while still remaining weapon dependant on cast though.
			if (!effectSkill.isOffensive() && !(effectSkill.getTargetType(this) == SkillTargetType.TARGET_PARTY && effectSkill.getSkillType() == L2SkillType.BUFF))
			{
				// Check to rest to assure current effect meets weapon requirements.
				if (!effectSkill.getWeaponDependancy(this))
				{
					sendMessage(effectSkill.getName() + " cannot be used with this weapon.");
					if (Config.DEBUG)
						_log.info("   | Skill " + effectSkill.getName() + " has been disabled for (" + getName() + "); Reason: Incompatible Weapon Type.");
					currenteffect.exit();
				}
			}
			continue;
		}
	}
	
	public void useEquippableItem(L2ItemInstance item, boolean abortAttack, boolean force)
	{
		// Equip or unEquip
		L2ItemInstance[] items = null;
		final boolean isEquiped = item.isEquipped();
		final int oldInvLimit = getInventoryLimit();
		SystemMessage sm = null;
		L2ItemInstance old = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
		if (old == null)
			old = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (NewHuntingGrounds._started && _inEventHG)
		{
			if (isEquiped)
			{
				sendMessage("Can't unequip " + item.getName());
				return;
			}
			else if (!isEquiped)
			{
				sendMessage("Can't equip " + item.getName());
				return;
			}
			else
				sendMessage("Requested Equip Approved.");
		}
		if (isEquiped)
		{
			if (item.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
				sm.addNumber(item.getEnchantLevel());
				sm.addItemName(item);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(item);
			}
			sendPacket(sm);
			if (item.getUntradeableTime() < 9999999900000L && item.isUntradeableAfterEquip())
			{
				item.setUntradeableTimer(9999999900000L);
				sendMessage("Your " + item.getName() + " is now untradeable");
			}
			else if (item.shouldBeNowSetAsTradeable())
			{
				item.setUntradeableTimer(0);
				sendMessage("Your " + item.getName() + " is now tradeable");
			}
			int slot = getInventory().getSlotFromItem(item);
			// we cant unequip talisman by body slot
			if (slot == L2Item.SLOT_DECO)
				items = getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
			else
				items = getInventory().unEquipItemInBodySlotAndRecord(slot);
		}
		else
		{
			if (item.getUntradeableTime() < 9999999900000L && item.isUntradeableAfterEquip())
			{
				if (!force)
				{
					final String itemName = (item.isEnchantable() ? "+" + item.getEnchantLevel() + " " : "") + item.getName();
					ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S2.getId()).addString("Equipping your " + itemName + " will make it untradeable. Do you still wish to equip it?");
					setUseItemRequestItemId(item.getObjectId());
					sendPacket(dlg);
					return;
				}
				else
				{
					setUseItemRequestItemId(0);
					item.setUntradeableTimer(9999999900000L);
					sendMessage("Your " + item.getName() + " is now untradeable");
				}
			}
			int tempBodyPart = item.getItem().getBodyPart();
			L2ItemInstance tempItem = getInventory().getPaperdollItemByL2ItemId(tempBodyPart);
			// check if the item replaces a wear-item
			if (tempItem != null && tempItem.isWear())
			{
				// dont allow an item to replace a wear-item
				return;
			}
			else if (tempBodyPart == L2Item.SLOT_LR_HAND)
			{
				// this may not remove left OR right hand equipment
				tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if (tempItem != null && tempItem.isWear())
					return;
				tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
				if (tempItem != null && tempItem.isWear())
					return;
			}
			else if (tempBodyPart == L2Item.SLOT_FULL_ARMOR)
			{
				// this may not remove chest or leggins
				tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
				if (tempItem != null && tempItem.isWear())
					return;
				tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
				if (tempItem != null && tempItem.isWear())
					return;
			}
			if (item.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.S1_S2_EQUIPPED);
				sm.addNumber(item.getEnchantLevel());
				sm.addItemName(item);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
				sm.addItemName(item);
			}
			sendPacket(sm);
			items = getInventory().equipItemAndRecord(item);
			// Consume mana - will start a task if required; returns if item is not a shadow item
			item.decreaseMana(false);
		}
		sm = null;
		broadcastUserInfo();
		sendPacket(new UpdateLunaDetailStats("test", this));
		InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(Arrays.asList(items));
		sendPacket(iu);
		if (abortAttack)
			abortAttack();
		if (getInventoryLimit() != oldInvLimit)
			sendPacket(new ExStorageMaxCount(this));
	}
	
	public int getEventKills()
	{
		return _eventKills;
	}
	
	public void setEventKills(int eventKills)
	{
		_eventKills = eventKills;
	}
	
	public int getSiegeKills()
	{
		return _siegeKills;
	}
	
	public void setSiegeKills(int siegeKills)
	{
		_siegeKills = siegeKills;
	}
	
	public int getRaidKills()
	{
		return _raidKills;
	}
	
	public void setRaidKills(int raidKills)
	{
		_raidKills = raidKills;
	}
	
	public int getOlympiadWins()
	{
		return _olympiadWins;
	}
	
	public void setOlympiadWins(int olympiadWins)
	{
		_olympiadWins = olympiadWins;
	}
	
	/**
	 * Return the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR>
	 * <BR>
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}
	
	/**
	 * Set the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR>
	 * <BR>
	 */
	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}
	
	/**
	 * Return the Fame of this L2PcInstance <BR>
	 * <BR>
	 * 
	 * @return
	 */
	public int getFame()
	{
		return _fame;
	}
	
	/**
	 * Set the Fame of this L2PcInstane <BR>
	 * <BR>
	 * 
	 * @param fame
	 */
	public void setFame(int fame)
	{
		final int oldFame = _fame;
		if (fame > Config.MAX_PERSONAL_FAME_POINTS)
			_fame = Config.MAX_PERSONAL_FAME_POINTS;
		else
			_fame = fame;
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			if (fame > oldFame)
			{
				getCounters().fameAcquired += fame - oldFame;
			}
		}
	}
	
	/**
	 * Return the ClassId object of the L2PcInstance contained in L2PcTemplate.<BR>
	 * <BR>
	 */
	public ClassId getClassId()
	{
		return getTemplate().classId;
	}
	
	/**
	 * Set the template of the L2PcInstance.<BR>
	 * <BR>
	 *
	 * @param Id
	 *            The Identifier of the L2PcTemplate to set to the L2PcInstance
	 */
	public void setClassId(int Id)
	{
		if (!_subclassLock.tryLock())
			return;
		try
		{
			if (isSubClassActive())
				getSubClasses().get(_classIndex).setClassId(Id);
			setTarget(this);
			broadcastPacket(new MagicSkillUse(this, 5103, 1, 1000, 0));
			setClassTemplate(Id);
			// Update class icon in party and clan
			if (isInParty())
				getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));
			if (getClan() != null)
				getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
			if (Config.AUTO_LEARN_SKILLS)
				rewardSkills();
		}
		finally
		{
			_subclassLock.unlock();
		}
	}
	
	/** Return the Experience of the L2PcInstance. */
	public long getExp()
	{
		return getStat().getExp();
	}
	
	public void setActiveEnchantAttrItem(L2ItemInstance stone)
	{
		_activeEnchantAttrItem = stone;
	}
	
	public L2ItemInstance getActiveEnchantAttrItem()
	{
		return _activeEnchantAttrItem;
	}
	
	public void setActiveEnchantItem(L2ItemInstance scroll)
	{
		// If we dont have a Enchant Item, we are not enchanting.
		if (scroll == null)
		{
			setActiveEnchantSupportItem(null);
			setActiveEnchantTimestamp(0);
			setIsEnchanting(false);
		}
		_activeEnchantItem = scroll;
	}
	
	public L2ItemInstance getActiveEnchantItem()
	{
		return _activeEnchantItem;
	}
	
	public void setActiveEnchantSupportItem(L2ItemInstance item)
	{
		_activeEnchantSupportItem = item;
	}
	
	public L2ItemInstance getActiveEnchantSupportItem()
	{
		return _activeEnchantSupportItem;
	}
	
	public long getActiveEnchantTimestamp()
	{
		return _activeEnchantTimestamp;
	}
	
	public void setActiveEnchantTimestamp(long val)
	{
		_activeEnchantTimestamp = val;
	}
	
	public void setIsEnchanting(boolean val)
	{
		_isEnchanting = val;
	}
	
	public boolean isEnchanting()
	{
		return _isEnchanting;
	}
	
	/**
	 * Set the fists weapon of the L2PcInstance (used when no weapon is equiped).<BR>
	 * <BR>
	 *
	 * @param weaponItem
	 *            The fists L2Weapon to set to the L2PcInstance
	 */
	public void setFistsWeaponItem(L2Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}
	
	/**
	 * Return the fists weapon of the L2PcInstance (used when no weapon is equiped).<BR>
	 * <BR>
	 */
	public L2Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}
	
	/**
	 * Return the fists weapon of the L2PcInstance Class (used when no weapon is equiped).<BR>
	 * <BR>
	 */
	public L2Weapon findFistsWeaponItem(int classId)
	{
		L2Weapon weaponItem = null;
		if ((classId >= 0x00) && (classId <= 0x09))
		{
			// human fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(246);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			// human mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(251);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			// elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(244);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			// elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(249);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			// dark elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(245);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			// dark elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(250);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			// orc fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(248);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			// orc mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(252);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			// dwarven fists
			L2Item temp = ItemTable.getInstance().getTemplate(247);
			weaponItem = (L2Weapon) temp;
		}
		return weaponItem;
	}
	
	/**
	 * Give Expertise skill of this level and remove beginner Lucky skill.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the Level of the L2PcInstance</li>
	 * <li>If L2PcInstance Level is 5, remove beginner Lucky skill</li>
	 * <li>Add the Expertise skill corresponding to its Expertise level</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T give other free skills (SP needed = 0)</B></FONT><BR>
	 * <BR>
	 */
	public void rewardSkills()
	{
		giveAvailableSkills();
		checkItemRestriction();
		givePvpSkills();
		sendSkillList();
	}
	
	/**
	 * Regive all skills which aren't saved to database, like Noble, Hero, Clan Skills<BR>
	 * <BR>
	 */
	public void regiveTemporarySkills()
	{
		// Do not call this on enterworld or char load
		// Add noble skills if noble
		if (isNoble())
			setNoble(true);
		// Add Hero skills if hero
		if (isHero())
			setHero(true);
		// Add clan skills
		if (getClan() != null)
		{
			L2Clan clan = getClan();
			L2Skill[] skills = clan.getAllSkills();
			for (L2Skill sk : skills)
			{
				if (_clan.getReputationScore() > 0)
					/* if(sk.getMinPledgeClass() <= getPledgeClass()) */
					addSkill(sk, false);
			}
			if (isClanLeader())
				SiegeManager.getInstance().addSiegeSkills(this);
			if (clan.getHasCastle() > 0)
				CastleManager.getInstance().getCastleByOwner(clan).giveResidentialSkills(this);
			if (clan.getHasFort() > 0)
				FortManager.getInstance().getFortByOwner(clan).giveResidentialSkills(this);
		}
		// Reload passive skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();
		// Add Death Penalty Buff Level
		restoreDeathPenaltyBuffLevel();
	}
	
	public void giveMarriageSkills()
	{
		if (isThisCharacterMarried())
		{
			final long marriageDate = CoupleManager.getInstance().getCouple(getCoupleId()).getWeddingDate().getTimeInMillis();
			final long difference = System.currentTimeMillis() - marriageDate;
			if (difference > 345600000) // 4 days
			{
				L2Skill sk = SkillTable.getInstance().getInfo(12000, 1); // love ud
				if (sk != null)
					addSkill(sk, false);
				if (difference > 691200000) // 8 days
				{
					sk = SkillTable.getInstance().getInfo(12005, 1); // zombify
					if (sk != null)
						addSkill(sk, false);
					if (difference > 1382400000) // 16 days
					{
						sk = SkillTable.getInstance().getInfo(12001, 1); // love n rage
						if (sk != null)
							addSkill(sk, false);
						if (difference > 2764800000L) // 32 days
						{
							sk = SkillTable.getInstance().getInfo(12002, 1); // nuptial rush
							if (sk != null)
								addSkill(sk, false);
							if (difference > 5529600000L) // 64 days
							{
								sk = SkillTable.getInstance().getInfo(12003, 1); // honeymoon
								if (sk != null)
									addSkill(sk, false);
								if (difference > 5529600000L) // 128 days
								{
									sk = SkillTable.getInstance().getInfo(12004, 1); // rekindle
									if (sk != null)
										addSkill(sk, false);
								}
							}
						}
					}
				}
			}
		}
	}
	
	public void regiveTemporarySkillsSgradeZone()
	{
		// Add clan skills
		if (getClan() != null)
		{
			L2Clan clan = getClan();
			L2Skill[] skills = clan.getAllSkills();
			for (L2Skill sk : skills)
			{
				if (_clan.getReputationScore() > 0)
					/* if(sk.getMinPledgeClass() <= getPledgeClass()) */
					addSkill(sk, false);
			}
			if (isClanLeader())
				SiegeManager.getInstance().addSiegeSkills(this);
			if (clan.getHasCastle() > 0)
				CastleManager.getInstance().getCastleByOwner(clan).giveResidentialSkills(this);
			if (clan.getHasFort() > 0)
				FortManager.getInstance().getFortByOwner(clan).giveResidentialSkills(this);
		}
		// Reload passive skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();
		// Add Death Penalty Buff Level
		restoreDeathPenaltyBuffLevel();
	}
	
	final static public int[]	MARRIAGE_SKILLS	=
	{
		12000, 12005, 12001, 12002, 12003, 12004
	};
	final static public int[]	PVP_SKILLS		=
	{
		7045, 426, 427, 7046, 7041, 7042, 7049, 7054, 7050, 7056,
		7055, 7064, 15000, 15001, 15002, 15003, 15004, 15005, 15006, 15007, 15008, 15009, 15010
	};
	
	public final void givePvpSkills()
	{
		final int pvps = getPvpKills();
		if (pvps >= 500)
			addSkill(15007); // pvp music
		if (pvps >= 1000)
		{
			if (isMageClass())
				addSkill(427); // spell force
			else
				addSkill(426); // battle force
			if (isProphet())
				addSkill(426); // battle force
		}
		if (pvps >= 2000)
			addSkill(15006); // pvp firecracker
		if (pvps >= 3000)
			addSkill(7045); // master's blessed body
		if (pvps >= 4000)
			addSkill(7046); // master's blessed soul
		if (pvps >= 5000)
			addSkill(60); // fake death
		if (pvps >= 6000)
			addSkill(7041); // master's blessing focus (don't lose target + crit)
		if (pvps >= 7000)
			addSkill(7049); // master's blessing decrease weight (slow person down)
		if (pvps >= 8000)
			addSkill(7042); // master's blessing death whisper (lower healed amount)
		if (pvps >= 9500)
		{
			if (isMageClass())
				addSkill(7054); // master's empower
			else
				addSkill(7050); // master's might
			if (isProphet())
				addSkill(7050); // master's might
		}
		if (pvps >= 11000)
			addSkill(15002); // pvp medusa
		if (pvps >= 13500)
			addSkill(7056); // master's berserker spirit (aggdebuff)
		if (pvps >= 17000)
			addSkill(15000); // resurrection
		if (pvps >= 21000)
			addSkill(7055); // master's windwalk (dash)
		if (pvps >= 25000)
			addSkill(15003); // pvp return
		if (pvps >= 30000)
		{
			if (isMageClass() || isArcherClass())
				addSkill(15005); // pvp blink
			else
				addSkill(15004); // pvp rush
		}
		if (pvps >= 40000)
			addSkill(7064); // pvp victories
		if (pvps >= 50000)
			addSkill(15001); // psycho symphony
		givePvpTransformSkills();
	}
	
	final static public int[] PVP_TRANSFORM_SKILLS =
	{
		2428, 617, 3337, 674, 2631, 670, 2394, 618, 2670, 2671, 3336, 671, 3335, 673, 672, 5655, 546, 2511, 2632, 8246, 552, 555, 5261
	};
	
	final private void givePvpTransformSkills()
	{
		final int pvps = getPvpKills();
		if (pvps >= 65)
			addSkill(2428); // rabbit
		if (pvps >= 200)
			addSkill(617); // onyx beast
		if (pvps >= 400)
			addSkill(3337); // pig
		if (pvps >= 700)
			addSkill(674); // Doll Blader
		if (pvps >= 1040)
			addSkill(2631); // frog
		if (pvps >= 1400)
			addSkill(670); // Heretic
		if (pvps >= 1800)
			addSkill(2394); // pixy
		if (pvps >= 2200)
			addSkill(618); // death blader
		if (pvps >= 2450)
		{
			if (getAppearance().getSex())
				addSkill(2670); // red elf
			else
				addSkill(2671); // blue elf
		}
		if (pvps >= 2700)
			addSkill(3336); // Buffalo
		if (pvps >= 3200)
			addSkill(671); // Vale Master
		if (pvps >= 3700)
			addSkill(3335); // Yeti
		if (pvps >= 4300)
			addSkill(673); // Ole Mahum
		if (pvps >= 4900)
			addSkill(672); // Saber Tooth Tiger
		if (pvps >= 5500)
			addSkill(5655); // Gatekeeper alternate
		if (pvps >= 6100)
			addSkill(546); // Unicorn strong
		if (pvps >= 7000)
			addSkill(2511); // Gatekeeper
		if (pvps >= 7900)
			addSkill(2632); // young child
		if (pvps >= 8800)
			addSkill(8246); // Pumpkin ghost
		if (pvps >= 9800)
			addSkill(552); // Golem Guardian strong
		if (pvps >= 10800)
			addSkill(555); // Inferno Drake strong
		if (getPkKills() > 100)
			addSkill(5261); // zombie
	}
	
	/**
	 * Give all available skills to the player.<br>
	 * <br>
	 */
	private void giveAvailableSkills()
	{
		int unLearnable = 0;
		int skillCounter = 0;
		// Get available skills
		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		while (skills.length > unLearnable)
		{
			for (L2SkillLearn s : skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				if (sk == null || (sk.getId() == L2Skill.SKILL_DIVINE_INSPIRATION && !Config.AUTO_LEARN_DIVINE_INSPIRATION))
				{
					unLearnable++;
					continue;
				}
				if (getSkillLevel(sk.getId()) == -1)
					skillCounter++;
				// fix when learning toggle skills
				if (sk.isToggle())
				{
					L2Effect toggleEffect = getFirstEffect(sk.getId());
					if (toggleEffect != null)
					{
						// stop old toggle skill effect, and give new toggle skill effect back
						toggleEffect.exit();
						sk.getEffects(this, this);
					}
				}
				addSkill(sk, true);
			}
			// Get new available skills
			skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		}
		sendMessage("You have learned " + skillCounter + " new skills.");
	}
	
	/** Set the Experience value of the L2PcInstance. */
	public void setExp(long exp)
	{
		if (exp < 0)
			exp = 0;
		getStat().setExp(exp);
	}
	
	public void setRace(int race)
	{
		_race = race;
	}
	
	public void setRace(Race race)
	{
		_race = race.ordinal();
	}
	
	public Race getRace()
	{
		if (_race < 0)
		{
			if (!isSubClassActive())
				return getTemplate().race;
			L2PcTemplate charTemp = CharTemplateTable.getInstance().getTemplate(_baseClass);
			return charTemp.race;
		}
		switch (_race)
		{
			case 0:
				return Race.Human;
			case 1:
				return Race.Elf;
			case 2:
				return Race.DarkElf;
			case 3:
				return Race.Orc;
			case 4:
				return Race.Dwarf;
			case 5:
				return Race.Kamael;
			case 6:
				return Race.MHuman;
			case 7:
				return Race.MOrc;
			default:
				return Race.Human;
		}
	}
	
	public final boolean getSex()
	{
		return _appearance.getSex();
	}
	
	public L2Radar getRadar()
	{
		return _radar;
	}
	
	/** Return the SP amount of the L2PcInstance. */
	public int getSp()
	{
		return getStat().getSp();
	}
	
	/** Set the SP amount of the L2PcInstance. */
	public void setSp(int sp)
	{
		if (sp < 0)
			sp = 0;
		super.getStat().setSp(sp);
	}
	
	/**
	 * Return true if this L2PcInstance is a clan leader in
	 * ownership of the passed castle
	 */
	public boolean isCastleLord(int castleId)
	{
		L2Clan clan = getClan();
		// player has clan and is the clan leader, check the castle info
		if ((clan != null) && (clan.getLeader().getPlayerInstance() == this))
		{
			// if the clan has a castle and it is actually the queried castle, return true
			Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
			if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId)))
				return true;
		}
		return false;
	}
	
	/**
	 * Return the Clan Identifier of the L2PcInstance.<BR>
	 * <BR>
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Return the Clan Crest Identifier of the L2PcInstance or 0.<BR>
	 * <BR>
	 */
	public int getClanCrestId()
	{
		if (_clan != null && _clan.hasCrest())
			return _clan.getCrestId();
		return 0;
	}
	
	/**
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if (_clan != null && _clan.hasCrestLarge())
			return _clan.getCrestLargeId();
		return 0;
	}
	
	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}
	
	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}
	
	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}
	
	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}
	
	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}
	
	public long getOnlineTime()
	{
		return _onlineTime;
	}
	
	public void setLoginData()
	{
		_loginData = new CharactersTable.CharacterLoginData(this);
	}
	
	public CharactersTable.CharacterLoginData getLoginData()
	{
		return _loginData;
	}
	
	/**
	 * Return the PcInventory Inventory of the L2PcInstance contained in _inventory.<BR>
	 * <BR>
	 */
	@Override
	public PcInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Delete a ShortCut of the L2PcInstance _shortCuts.<BR>
	 * <BR>
	 */
	public void removeItemFromShortCut(int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}
	
	/**
	 * Return True if the L2PcInstance is sitting.<BR>
	 * <BR>
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting;
	}
	
	/**
	 * Set _waitTypeSitting to given value
	 */
	public void setIsSitting(boolean state)
	{
		_waitTypeSitting = state;
	}
	
	/**
	 * Sit down the L2PcInstance, set the AI Intention to AI_INTENTION_REST and send a Server->Client ChangeWaitType packet (broadcast)<BR>
	 * <BR>
	 */
	public void sitDown()
	{
		if ((isCastingNow() || isCastingSimultaneouslyNow()) && !_relax)
		{
			sendMessage("Cannot sit while casting");
			return;
		}
		if (!_waitTypeSitting && !isAttackingDisabled() && !isOutOfControl() && !isImmobilized())
		{
			abortAttack();
			setIsSitting(true);
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
			// Schedule a sit down task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new SitDownTask(this), 2500);
			setIsParalyzed(true);
		}
	}
	
	/**
	 * Sit down Task
	 */
	class SitDownTask implements Runnable
	{
		L2PcInstance _player;
		
		SitDownTask(L2PcInstance player)
		{
			_player = player;
		}
		
		public void run()
		{
			_player.setIsParalyzed(false);
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		}
	}
	
	/**
	 * Stand up Task
	 */
	class StandUpTask implements Runnable
	{
		L2PcInstance _player;
		
		StandUpTask(L2PcInstance player)
		{
			_player = player;
		}
		
		public void run()
		{
			_player.setIsSitting(false);
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
	}
	
	/**
	 * Stand up the L2PcInstance, set the AI Intention to AI_INTENTION_IDLE and send a Server->Client ChangeWaitType packet (broadcast)<BR>
	 * <BR>
	 */
	public void standUp()
	{
		if (eventSitForced && isInKoreanEvent())
		{
			sendMessage("You can not stand up at this moment");
			return;
		}
		else if (TvT._sitForced && _inEventTvT || NewHuntingGrounds._sitForced && _inEventHG || FOS._sitForced && _inEventFOS || NewFOS._sitForced && _inEventFOS || CTF._sitForced && _inEventCTF || NewCTF._sitForced && _inEventCTF || DM._sitForced && _inEventDM || VIP._sitForced && _inEventVIP || _isSitForcedTvTi)
			sendMessage("The GM handles if you sit or stand in this match.");
		else if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead())
		{
			if (_relax)
			{
				setRelax(false);
				stopEffects(L2EffectType.RELAXING);
			}
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			// Schedule a stand up task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(this), 2500);
		}
	}
	
	/**
	 * Set the value of the _relax value. Must be True if using skill Relax and False if not.
	 */
	public void setRelax(boolean val)
	{
		_relax = val;
	}
	
	/**
	 * Return the PcWarehouse object of the L2PcInstance.<BR>
	 * <BR>
	 */
	public PcWarehouse getWarehouse()
	{
		if (_warehouse == null)
		{
			_warehouse = new PcWarehouse(this);
			_warehouse.restore();
		}
		if (Config.WAREHOUSE_CACHE)
			WarehouseCacheManager.getInstance().addCacheTask(this);
		return _warehouse;
	}
	
	/**
	 * Free memory used by Warehouse
	 */
	public void clearWarehouse()
	{
		if (_warehouse != null)
			_warehouse.deleteMe();
		_warehouse = null;
	}
	
	/**
	 * Return the PcFreight object of the L2PcInstance.<BR>
	 * <BR>
	 */
	public PcFreight getFreight()
	{
		if (_freight == null)
		{
			_freight = new PcFreight(this);
			_freight.restore();
		}
		return _freight;
	}
	
	/**
	 * Free memory used by Freight
	 */
	public void clearFreight()
	{
		if (_freight != null)
			_freight.deleteMe();
		_freight = null;
	}
	
	/**
	 * Return deposited PcFreight object for the objectId
	 * or create new if not exist
	 */
	public PcFreight getDepositedFreight(int objectId)
	{
		if (_depositedFreight == null)
			_depositedFreight = new FastList<PcFreight>();
		else
		{
			for (PcFreight freight : _depositedFreight)
			{
				if (freight != null && freight.getOwnerId() == objectId)
					return freight;
			}
		}
		PcFreight freight = new PcFreight(null);
		freight.doQuickRestore(objectId);
		_depositedFreight.add(freight);
		return freight;
	}
	
	/**
	 * Clear memory used by deposited freight
	 */
	public void clearDepositedFreight()
	{
		if (_depositedFreight == null)
			return;
		for (PcFreight freight : _depositedFreight)
		{
			if (freight != null)
			{
				try
				{
					freight.deleteMe();
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "clearDepositedFreight()", e);
				}
			}
		}
		_depositedFreight.clear();
		_depositedFreight = null;
	}
	
	/**
	 * Return the Identifier of the L2PcInstance.<BR>
	 * <BR>
	 */
	public int getCharId()
	{
		return _charId;
	}
	
	/**
	 * Set the Identifier of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void setCharId(int charId)
	{
		_charId = charId;
	}
	
	/**
	 * Return the Adena amount of the L2PcInstance.<BR>
	 * <BR>
	 */
	public long getAdena()
	{
		return _inventory.getAdena();
	}
	
	/**
	 * Return the Ancient Adena amount of the L2PcInstance.<BR>
	 * <BR>
	 */
	public long getAncientAdena()
	{
		return _inventory.getAncientAdena();
	}
	
	public long getBlueEva()
	{
		return _inventory.getBlueEva();
	}
	
	/**
	 * Add adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param count
	 *            : int Quantity of adena to be added
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 */
	public void addAdena(String process, long count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ADENA);
			sm.addItemNumber(count);
			sendPacket(sm);
		}
		if (count > 0)
		{
			_inventory.addAdena(process, count, this, reference);
			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAdenaInstance());
				sendPacket(iu);
			}
			else
				sendPacket(new ItemList(this, false));
		}
	}
	
	/**
	 * Reduce adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param count
	 *            : long Quantity of adena to be reduced
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAdena(String process, long count, L2Object reference, boolean sendMessage)
	{
		if (count > getAdena())
		{
			if (sendMessage)
				sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return false;
		}
		if (count > 0)
		{
			L2ItemInstance adenaItem = _inventory.getAdenaInstance();
			_inventory.reduceAdena(process, count, this, reference);
			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(adenaItem);
				sendPacket(iu);
			}
			else
				sendPacket(new ItemList(this, false));
			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DISAPPEARED_ADENA);
				sm.addItemNumber(count);
				sendPacket(sm);
			}
		}
		return true;
	}
	
	/**
	 * Add ancient adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 *
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param count
	 *            : int Quantity of ancient adena to be added
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 */
	public void addAncientAdena(String process, long count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
			sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
			sm.addItemNumber(count);
			sendPacket(sm);
		}
		if (count > 0)
		{
			_inventory.addAncientAdena(process, count, this, reference);
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAncientAdenaInstance());
				sendPacket(iu);
			}
			else
				sendPacket(new ItemList(this, false));
		}
	}
	
	/**
	 * Reduce ancient adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param count
	 *            : long Quantity of ancient adena to be reduced
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAncientAdena(String process, long count, L2Object reference, boolean sendMessage)
	{
		if (count > getAncientAdena())
		{
			if (sendMessage)
				sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return false;
		}
		if (count > 0)
		{
			L2ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
			_inventory.reduceAncientAdena(process, count, this, reference);
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(ancientAdenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
				sm.addItemNumber(count);
				sendPacket(sm);
			}
		}
		return true;
	}
	
	/**
	 * Adds item to inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param item
	 *            : L2ItemInstance to be added
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		if (item.getCount() > 0)
		{
			// Sends message to client if requested
			if (sendMessage)
			{
				if (item.getCount() > 1)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
					sm.addItemName(item);
					sm.addItemNumber(item.getCount());
					sendPacket(sm);
				}
				else if (item.getEnchantLevel() > 0)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item);
					sendPacket(sm);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
					sm.addItemName(item);
					sendPacket(sm);
				}
			}
			// Add the item to inventory
			L2ItemInstance newitem = _inventory.addItem(process, item, this, reference);
			// Send inventory update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				sendPacket(playerIU);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			// Cursed Weapon
			if (CursedWeaponsManager.getInstance().isCursed(newitem.getItemId()))
			{
				CursedWeaponsManager.getInstance().activate(this, newitem);
			}
			// Combat Flag
			else if (FortSiegeManager.getInstance().isCombat(item.getItemId()))
			{
				if (FortSiegeManager.getInstance().activateCombatFlag(this, item))
				{
					Fort fort = FortManager.getInstance().getFort(this);
					fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.C1_ACQUIRED_THE_FLAG), getName());
				}
			}
		}
	}
	
	/**
	 * Adds item to Inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param itemId
	 *            : int Item Identifier of the item to be added
	 * @param count
	 *            : long Quantity of items to be added
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 */
	public L2ItemInstance addItem(String process, int itemId, long count, L2Object reference, boolean sendMessage, int enchantLvl)
	{
		if (count > 0)
		{
			L2ItemInstance item = null;
			if (ItemTable.getInstance().getTemplate(itemId) != null)
			{
				item = ItemTable.getInstance().createDummyItem(itemId);
			}
			else
			{
				_log.log(Level.SEVERE, "Item doesn't exist so cannot be added. Item ID: " + itemId);
				return null;
			}
			// Sends message to client if requested
			if (sendMessage && ((!isCastingNow() && item.getItemType() == L2EtcItemType.HERB) || item.getItemType() != L2EtcItemType.HERB))
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addItemNumber(count);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
						sm.addItemName(itemId);
						sm.addItemNumber(count);
						sendPacket(sm);
					}
				}
				else
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
				}
			}
			// Auto use herbs - autoloot
			if (item.getItemType() == L2EtcItemType.HERB) // If item is herb dont add it to iv :]
			{
				if (!isCastingNow())
				{
					L2ItemInstance herb = new L2ItemInstance(_charId, itemId);
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(herb.getEtcItem());
					if (handler == null)
						_log.warning("No item handler registered for Herb - item ID " + herb.getItemId() + ".");
					else
					{
						handler.useItem(this, herb, false);
						if (_herbstask >= 100)
							_herbstask -= 100;
					}
				}
				else
				{
					_herbstask += 100;
					ThreadPoolManager.getInstance().scheduleAi(new HerbTask(process, itemId, count, reference, sendMessage), _herbstask);
				}
			}
			else
			{
				// Add the item to inventory
				L2ItemInstance createdItem = _inventory.addItem(process, itemId, count, this, reference);
				boolean update = false;
				if (enchantLvl > 0 && createdItem.isEnchantable())
				{
					// enchantLvl = Math.max(0, enchantLvl-Rnd.get(4));
					createdItem.setEnchantLevel(enchantLvl);
					update = true;
				}
				if (process.equalsIgnoreCase("loot") || process.equalsIgnoreCase("party") || process.equalsIgnoreCase("Extract"))
				{
					createdItem.addAutoAugmentation();
					update = true;
				}
				if (process.equalsIgnoreCase("AugBonanzoReward"))
				{
					createdItem.addAutoAugmentationDonation();
					update = true;
				}
				if (update)
				{
					InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addItem(createdItem);
					sendPacket(playerIU);
				}
				// Cursed Weapon
				if (CursedWeaponsManager.getInstance().isCursed(createdItem.getItemId()))
					CursedWeaponsManager.getInstance().activate(this, createdItem);
				// Combat Flag
				else if (FortSiegeManager.getInstance().isCombat(createdItem.getItemId()))
				{
					if (FortSiegeManager.getInstance().activateCombatFlag(this, item))
					{
						Fort fort = FortManager.getInstance().getFort(this);
						fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.C1_ACQUIRED_THE_FLAG), getName());
					}
				}
				// if (itemId == 57)
				// {
				// getMuseumPlayer().addData("adena", count);
				// }
				return createdItem;
			}
			return item;
		}
		return null;
	}
	
	public L2ItemInstance addItem(String process, int itemId, long count, L2Object reference, boolean sendMessage, int enchantLvl, boolean rndAug)
	{
		if (count > 0)
		{
			L2ItemInstance item = null;
			if (ItemTable.getInstance().getTemplate(itemId) != null)
			{
				item = ItemTable.getInstance().createDummyItem(itemId);
			}
			else
			{
				_log.log(Level.SEVERE, "Item doesn't exist so cannot be added. Item ID: " + itemId);
				return null;
			}
			// Sends message to client if requested
			if (sendMessage && ((!isCastingNow() && item.getItemType() == L2EtcItemType.HERB) || item.getItemType() != L2EtcItemType.HERB))
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addItemNumber(count);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
						sm.addItemName(itemId);
						sm.addItemNumber(count);
						sendPacket(sm);
					}
				}
				else
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
					else
					{
						if (enchantLvl > 0 && !rndAug)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
							sm.addString("+" + enchantLvl + " " + item.getName());
							sendPacket(sm);
						}
						if (enchantLvl > 0 && rndAug)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
							sm.addString("+" + enchantLvl + " Augmented " + item.getName());
							sendPacket(sm);
						}
					}
				}
			}
			// Auto use herbs - autoloot
			if (item.getItemType() == L2EtcItemType.HERB) // If item is herb dont add it to iv :]
			{
				if (!isCastingNow())
				{
					L2ItemInstance herb = new L2ItemInstance(_charId, itemId);
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(herb.getEtcItem());
					if (handler == null)
						_log.warning("No item handler registered for Herb - item ID " + herb.getItemId() + ".");
					else
					{
						handler.useItem(this, herb, false);
						if (_herbstask >= 100)
							_herbstask -= 100;
					}
				}
				else
				{
					_herbstask += 100;
					ThreadPoolManager.getInstance().scheduleAi(new HerbTask(process, itemId, count, reference, sendMessage), _herbstask);
				}
			}
			else
			{
				// Add the item to inventory
				L2ItemInstance createdItem = _inventory.addItem(process, itemId, count, this, reference);
				boolean update = false;
				if (enchantLvl > 0 && createdItem.isEnchantable())
				{
					// enchantLvl = Math.max(0, enchantLvl-Rnd.get(4));
					createdItem.setEnchantLevel(enchantLvl);
					update = true;
				}
				if (process.equalsIgnoreCase("loot") || process.equalsIgnoreCase("party") || process.equalsIgnoreCase("Extract"))
				{
					createdItem.addAutoAugmentation();
					update = true;
				}
				if (process.equalsIgnoreCase("AugBonanzoReward"))
				{
					createdItem.addAutoAugmentationDonation();
					update = true;
				}
				if (rndAug)
				{
					createdItem.addAutoAugmentationDonation();
					update = true;
				}
				if (update)
				{
					InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addItem(createdItem);
					sendPacket(playerIU);
				}
				// Cursed Weapon
				if (CursedWeaponsManager.getInstance().isCursed(createdItem.getItemId()))
					CursedWeaponsManager.getInstance().activate(this, createdItem);
				// Combat Flag
				else if (FortSiegeManager.getInstance().isCombat(createdItem.getItemId()))
				{
					if (FortSiegeManager.getInstance().activateCombatFlag(this, item))
					{
						Fort fort = FortManager.getInstance().getFort(this);
						fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.C1_ACQUIRED_THE_FLAG), getName());
					}
				}
				// if (itemId == 57)
				// {
				// getMuseumPlayer().addData("adena", count);
				// }
				return createdItem;
			}
			return item;
		}
		return null;
	}
	
	public L2ItemInstance addItem(String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
		return addItem(process, itemId, count, reference, sendMessage, 0);
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param item
	 *            : L2ItemInstance to be destroyed
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return destroyItem(process, item, item.getCount(), reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param item
	 *            : L2ItemInstance to be destroyed
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, L2ItemInstance item, long count, L2Object reference, boolean sendMessage)
	{
		item = _inventory.destroyItem(process, item, count, this, reference);
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			return false;
		}
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			sm.addItemName(item);
			sm.addItemNumber(count);
			sendPacket(sm);
		}
		return true;
	}
	
	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param objectId
	 *            : int Item Instance identifier of the item to be destroyed
	 * @param count
	 *            : int Quantity of items to be destroyed
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItem(String process, int objectId, long count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByObjectId(objectId);
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			return false;
		}
		return this.destroyItem(process, item, count, reference, sendMessage);
	}
	
	/**
	 * Destroys shots from inventory without logging and only occasional saving to database.
	 * Sends a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param objectId
	 *            : int Item Instance identifier of the item to be destroyed
	 * @param count
	 *            : int Quantity of items to be destroyed
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItemWithoutTrace(String process, int objectId, long count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByObjectId(objectId);
		if (item == null || item.getCount() < count)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			return false;
		}
		return this.destroyItem(null, item, count, reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param itemId
	 *            : int Item identifier of the item to be destroyed
	 * @param count
	 *            : int Quantity of items to be destroyed
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItemByItemId(String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByItemId(itemId);
		if (item == null || item.getCount() < count || _inventory.destroyItemByItemId(process, itemId, count, this, reference) == null)
		{
			if (sendMessage)
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			return false;
		}
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
			sendPacket(new ItemList(this, false));
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			sm.addItemName(itemId);
			sm.addItemNumber(count);
			sendPacket(sm);
		}
		return true;
	}
	
	/**
	 * Destroy all weared items from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public void destroyWearedItems(String process, L2Object reference, boolean sendMessage)
	{
		// Go through all Items of the inventory
		for (L2ItemInstance item : getInventory().getItems())
		{
			// Check if the item is a Try On item in order to remove it
			if (item.isWear())
			{
				if (item.isEquipped())
					getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
				if (_inventory.destroyItem(process, item, this, reference) == null)
				{
					_log.warning("Player " + getName() + " can't destroy weared item: " + item.getName() + "[ " + item.getObjectId() + " ]");
					continue;
				}
				// Send an Unequipped Message in system window of the player for each Item
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(item);
				sendPacket(sm);
			}
		}
		// Send the ItemList Server->Client Packet to the player in order to refresh its Inventory
		ItemList il = new ItemList(getInventory().getItems(), true);
		sendPacket(il);
		// Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers
		broadcastUserInfo();
		// Sends message to client if requested
		sendMessage("Trying-on mode has ended.");
	}
	
	/**
	 * Transfers item to another ItemContainer and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param itemId
	 *            : int Item Identifier of the item to be transfered
	 * @param count
	 *            : long Quantity of items to be transfered
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance transferItem(String process, int objectId, long count, Inventory target, L2Object reference)
	{
		L2ItemInstance oldItem = checkItemManipulation(objectId, count, "transfer");
		if (oldItem == null)
			return null;
		L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
		if (newItem == null)
			return null;
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			if (oldItem.getCount() > 0 && oldItem != newItem)
				playerIU.addModifiedItem(oldItem);
			else
				playerIU.addRemovedItem(oldItem);
			sendPacket(playerIU);
		}
		else
			sendPacket(new ItemList(this, false));
		// Send target update packet
		if (target instanceof PcInventory)
		{
			L2PcInstance targetPlayer = ((PcInventory) target).getOwner();
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				if (newItem.getCount() > count)
					playerIU.addModifiedItem(newItem);
				else
					playerIU.addNewItem(newItem);
				targetPlayer.sendPacket(playerIU);
			}
			else
				targetPlayer.sendPacket(new ItemList(targetPlayer, false));
		}
		else if (target instanceof PetInventory)
		{
			PetInventoryUpdate petIU = new PetInventoryUpdate();
			if (newItem.getCount() > count)
				petIU.addModifiedItem(newItem);
			else
				petIU.addNewItem(newItem);
			((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
		}
		return newItem;
	}
	
	/**
	 * Drop item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param item
	 *            : L2ItemInstance to be dropped
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		item = _inventory.dropItem(process, item, this, reference);
		if (item == null)
		{
			if (sendMessage)
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			return false;
		}
		item.dropMe(this, getX() + Rnd.get(50) - 25, getY() + Rnd.get(50) - 25, getZ() + 20);
		if (Config.AUTODESTROY_ITEM_AFTER > 0 && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		{
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
				ItemsAutoDestroy.getInstance().addItem(item);
		}
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
				item.setProtected(false);
			else
				item.setProtected(true);
		}
		else
			item.setProtected(true);
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
			sendPacket(new ItemList(this, false));
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}
		return true;
	}
	
	/**
	 * Drop item from inventory by using its <B>objectID</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * 
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param objectId
	 *            : int Item Instance identifier of the item to be dropped
	 * @param count
	 *            : long Quantity of items to be dropped
	 * @param x
	 *            : int coordinate for drop X
	 * @param y
	 *            : int coordinate for drop Y
	 * @param z
	 *            : int coordinate for drop Z
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage
	 *            : boolean Specifies whether to send message to Client about this action
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance dropItem(String process, int objectId, long count, int x, int y, int z, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance invitem = _inventory.getItemByObjectId(objectId);
		L2ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);
		if (item == null)
		{
			if (sendMessage)
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			return null;
		}
		item.dropMe(this, x, y, z);
		if (Config.AUTODESTROY_ITEM_AFTER > 0 && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		{
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
				ItemsAutoDestroy.getInstance().addItem(item);
		}
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
				item.setProtected(false);
			else
				item.setProtected(true);
		}
		else
			item.setProtected(true);
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(invitem);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}
		return item;
	}
	
	public L2ItemInstance checkItemManipulation(int objectId, long count, String action)
	{
		// TODO: if we remove objects that are not visisble from the L2World, we'll have to remove this check
		if (L2World.getInstance().findObject(objectId) == null)
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item not available in L2World");
			return null;
		}
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		if (item == null || item.getOwnerId() != getObjectId())
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return null;
		}
		if (count < 0 || (count > 1 && !item.isStackable()))
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
			return null;
		}
		if (count > item.getCount())
		{
			_log.finest(getObjectId() + ": player tried to " + action + " more items than he owns");
			return null;
		}
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId() + ": player tried to " + action + " item controling pet");
			return null;
		}
		if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			return null;
		}
		if (item.isWear())
		{
			// cannot drop/trade wear-items
			return null;
		}
		// We cannot put a Weapon with Augmention in WH while casting (Possible Exploit)
		if (item.isAugmented() && (isCastingNow() || isCastingSimultaneouslyNow()))
			return null;
		return item;
	}
	
	/**
	 * Set _protectEndTime according settings.
	 */
	public void setProtection(boolean protect)
	{
		if (protect && getKarma() > 0)
			return;
		if (Config.DEVELOPER && (protect || _protectEndTime > 0))
			_log.warning(getName() + ": Protection " + (protect ? "ON " + (GameTimeController.getGameTicks() + Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND) : "OFF") + " (currently " + GameTimeController.getGameTicks() + ")");
		_protectEndTime = protect ? GameTimeController.getGameTicks() + Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}
	
	public int getInSameClanAllyAs(L2PcInstance possibleTank)
	{
		if (possibleTank == null)
			return 0;
		if (getClanId() == 0 || possibleTank.getClanId() == 0)
			return 0;
		if (getClanId() == possibleTank.getClanId())
			return 2;
		if (getAllyId() != 0 && getAllyId() == possibleTank.getAllyId())
			return 1;
		return 0;
	}
	
	/**
	 * Set protection from agro mobs when getting up from fake death, according settings.
	 */
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeController.getGameTicks() + Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}
	
	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeController.getGameTicks();
	}
	
	public final boolean isFakeDeath()
	{
		return _isFakeDeath;
	}
	
	public final void setIsFakeDeath(boolean value)
	{
		_isFakeDeath = value;
	}
	
	@Override
	public final boolean isAlikeDead()
	{
		if (super.isAlikeDead())
			return true;
		return isFakeDeath();
	}
	
	/**
	 * Get the client owner of this char.<BR>
	 * <BR>
	 */
	public L2GameClient getClient()
	{
		return _client;
	}
	
	public void setClient(L2GameClient client)
	{
		_client = client;
	}
	
	/**
	 * Close the active connection with the client.<BR>
	 * <BR>
	 */
	public void closeNetConnection()
	{
		L2GameClient client = _client;
		if (client != null)
		{
			if (client.isDetached())
			{
				client.cleanMe(true);
			}
			else
			{
				if (!client.getConnection().isClosed())
				{
					client.close(new LeaveWorld());
				}
			}
		}
	}
	
	public Location getCurrentSkillWorldPositionLoc()
	{
		return new Location(_currentSkillWorldPosition.getX(), _currentSkillWorldPosition.getY(), _currentSkillWorldPosition.getZ());
	}
	
	public Point3D getCurrentSkillWorldPosition()
	{
		return _currentSkillWorldPosition;
	}
	
	public void setCurrentSkillWorldPosition(Point3D worldPosition)
	{
		_currentSkillWorldPosition = worldPosition;
	}
	
	/**
	 * Manage actions when a player click on this L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Actions on first click on the L2PcInstance (Select it)</U> :</B><BR>
	 * <BR>
	 * <li>Set the target of the player</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li><BR>
	 * <BR>
	 * <B><U> Actions on second click on the L2PcInstance (Follow it/Attack it/Intercat with it)</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li>
	 * <li>If this L2PcInstance has a Private Store, notify the player AI with AI_INTENTION_INTERACT</li>
	 * <li>If this L2PcInstance is autoAttackable, notify the player AI with AI_INTENTION_ATTACK</li><BR>
	 * <BR>
	 * <li>If this L2PcInstance is NOT autoAttackable, notify the player AI with AI_INTENTION_FOLLOW</li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : Action, AttackRequest</li><BR>
	 * <BR>
	 *
	 * @param player
	 *            The player that start an action on this L2PcInstance
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		if (player == null)
			return;
		// See description in TvTEvent.java
		if (!TvTEvent.onAction(player, getObjectId()))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		// Check if the L2PcInstance is confused
		if (player.isOutOfControl())
		{
			// Send a Server->Client packet ActionFailed to the player
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (((TvT._started && !Config.TVT_ALLOW_INTERFERENCE) || (NewTvT._started && !Config.TVT_ALLOW_INTERFERENCE) || (NewHuntingGrounds._started && !Config.TVT_ALLOW_INTERFERENCE) || (NewDomination._started && !Config.TVT_ALLOW_INTERFERENCE) || (NewFOS._started && !Config.FortressSiege_ALLOW_INTERFERENCE) || (FOS._started && !Config.FortressSiege_ALLOW_INTERFERENCE) || (CTF._started && !Config.CTF_ALLOW_INTERFERENCE) || (NewCTF._started && !Config.CTF_ALLOW_INTERFERENCE) || ((player._inEventTvTi || _inEventTvTi) && !Config.TVTI_ALLOW_INTERFERENCE) || (DM._started && !Config.DM_ALLOW_INTERFERENCE) || (NewDM._started && !Config.DM_ALLOW_INTERFERENCE) || (VIP._started && !Config.VIP_ALLOW_INTERFERENCE)) && !player.isGM())
		{
			if ((_inEventTvT && !player._inEventTvT) || (!_inEventTvT && player._inEventTvT))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if ((_inEventHG && !player._inEventHG) || (!_inEventHG && player._inEventHG))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if ((_inEventFOS && !player._inEventFOS) || (!_inEventFOS && player._inEventFOS))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if ((_inEventCTF && !player._inEventCTF) || (!_inEventCTF && player._inEventCTF))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if ((_inEventLunaDomi && !player._inEventLunaDomi) || (!_inEventLunaDomi && player._inEventLunaDomi))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if ((_inEventDM && !player._inEventDM) || (!_inEventDM && player._inEventDM))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if ((_inEventTvTi && !player._inEventTvTi) || (!_inEventTvTi && player._inEventTvTi))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if ((_inEventVIP && !player._inEventVIP) || (!_inEventVIP && player._inEventVIP))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		// Aggression target lock effect
		if ((player.isLockedTarget() && player.getLockedTarget() != this) || (player != this && calcStat(Stats.UNTARGETABLE, 0, null, null) > 0))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
			return;
		}
		// Check if the player already target this L2PcInstance
		if (player.getTarget() != this)
		{
			if (player.isSelectingTarget())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			player.setIsSelectingTarget(4);
			// Set the target of the player
			player.setTarget(this);
			// Send a Server->Client packet MyTargetSelected to the player
			// The color to display in the select window is White
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			if (player != this)
				player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (player != this)
				player.sendPacket(new ValidateLocation(this));
			if (isDead())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			// Check if this L2PcInstance has a Private Store
			if (getPrivateStoreType() != 0)
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				// Check if this L2PcInstance is autoAttackable
				if (isAutoAttackable(player))
				{
					// Player with lvl < 21 can't attack a cursed weapon holder
					// And a cursed weapon holder can't attack players with lvl < 21
					if ((isCursedWeaponEquipped() && player.getLevel() < 50) || (player.isCursedWeaponEquipped() && getLevel() < 50))
					{
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else
					{
						if (Config.GEODATA > 0)
						{
							if (GeoData.getInstance().canSeeTarget(player, this))
							{
								player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
							}
							else
								player.sendPacket(ActionFailed.STATIC_PACKET);
						}
						else
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
						}
					}
				}
				else
				{
					// This Action Failed packet avoids player getting stuck when clicking three or more times
					player.sendPacket(ActionFailed.STATIC_PACKET);
					if (Config.GEODATA > 0)
					{
						if (GeoData.getInstance().canSeeTarget(player, this))
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
						}
					}
					else
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
					}
				}
			}
		}
	}
	
	@Override
	public void onActionShift(L2GameClient client)
	{
		final L2PcInstance player = client.getActiveChar();
		if (player == null)
			return;
		player.sendPacket(ActionFailed.STATIC_PACKET);
		if (player.isGM())
		{
			// Check if the gm already target this l2pcinstance
			if (player.getTarget() != this)
			{
				// Set the target of the L2PcInstance player
				player.setTarget(this);
				// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			}
			// Send a Server->Client packet ValidateLocation to correct the L2PcInstance position and heading on the client
			if (player != this)
				player.sendPacket(new ValidateLocation(this));
			IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler("admin_character_info");
			if (ach != null)
				ach.useAdminCommand("admin_character_info " + getName(), player);
		}
		else
		{
			// See description in TvTEvent.java
			if (!TvTEvent.onAction(player, getObjectId()))
				return;
			// Check if the L2PcInstance is confused
			if (player.isOutOfControl())
				return;
			// Aggression target lock effect
			if ((player.isLockedTarget() && player.getLockedTarget() != this) || (player != this && calcStat(Stats.UNTARGETABLE, 0, null, null) > 0))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
				return;
			}
			// Check if the player already target this L2PcInstance
			if (player.getTarget() != this)
			{
				if (player.isSelectingTarget())
					return;
				player.setIsSelectingTarget(3);
				// Set the target of the player
				player.setTarget(this);
				// Send a Server->Client packet MyTargetSelected to the player
				// The color to display in the select window is White
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));
				if (player != this)
					player.sendPacket(new ValidateLocation(this));
			}
			else
			{
				if (player != this)
					player.sendPacket(new ValidateLocation(this));
			}
		}
	}
	
	/**
	 * Returns true if cp update should be done, false if not
	 * 
	 * @return boolean
	 */
	private boolean needCpUpdate(int barPixels)
	{
		double currentCp = getCurrentCp();
		if (currentCp <= 1.0 || getMaxCp() < barPixels)
			return true;
		if (currentCp <= _cpUpdateDecCheck || currentCp >= _cpUpdateIncCheck)
		{
			if (currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Returns true if mp update should be done, false if not
	 * 
	 * @return boolean
	 */
	private boolean needMpUpdate(int barPixels)
	{
		double currentMp = getCurrentMp();
		if (currentMp <= 1.0 || getMaxMp() < barPixels)
			return true;
		if (currentMp <= _mpUpdateDecCheck || currentMp >= _mpUpdateIncCheck)
		{
			if (currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Send packet StatusUpdate with current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other L2PcInstance of the Party.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance</li><BR>
	 * <li>Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND current HP and MP to all L2PcInstance of the _statusListener</B></FONT><BR>
	 * <BR>
	 */
	@Override
	public void broadcastStatusUpdate()
	{
		// Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
		su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
		sendPacket(su);
		final boolean needCpUpdate = needCpUpdate(352);
		final boolean needHpUpdate = needHpUpdate(352);
		// Check if a party is in progress and party window update is usefull
		if (isInParty() && (needCpUpdate || needHpUpdate || needMpUpdate(352)))
		{
			if (Config.DEBUG)
				_log.fine("Send status for party window of " + getObjectId() + "(" + getName() + ") to his party. CP: " + getCurrentCp() + " HP: " + getCurrentHp() + " MP: " + getCurrentMp());
			// Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party
			PartySmallWindowUpdate update = new PartySmallWindowUpdate(this);
			getParty().broadcastToPartyMembers(this, update);
		}
		if (isInOlympiadMode() && isOlympiadStart() && (needCpUpdate || needHpUpdate))
		{
			L2PcInstance[] players = Olympiad.getInstance().getPlayers(_olympiadGameId);
			if (players != null && players.length == 2)
			{
				ExOlympiadUserInfo olyInfo = new ExOlympiadUserInfo(this, 2);
				for (L2PcInstance player : players)
				{
					if (player != null && player != this)
						player.sendPacket(olyInfo);
				}
			}
			FastList<L2PcInstance> specs = Olympiad.getInstance().getSpectators(_olympiadGameId);
			if (specs != null && !specs.isEmpty())
			{
				ExOlympiadUserInfo olyInfo = new ExOlympiadUserInfo(this, getOlympiadSide());
				for (L2PcInstance spectator : specs)
				{
					if (spectator == null)
						continue;
					spectator.sendPacket(olyInfo);
				}
			}
		}
		// In duel MP updated only with CP or HP
		if (isInDuel() && (needCpUpdate || needHpUpdate))
		{
			ExDuelUpdateUserInfo update = new ExDuelUpdateUserInfo(this);
			DuelManager.getInstance().broadcastToOppositTeam(this, update);
		}
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>.
	 * In order to inform other players of this L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet.
	 * Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR>
	 * <BR>
	 */
	public void broadcastUserInfo()
	{
		if (!canSendUserInfo)
			return;
		// Send a Server->Client packet UserInfo to this L2PcInstance
		sendPacket(new UserInfo(this));
		// Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance
		if (Config.DEBUG)
			_log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + " packet: [S] 03 CharInfo");
		broadcastPacket(new CharInfo(this));
		broadcastPacket(new ExBrExtraUserInfo(this));
	}
	
	public final void broadcastTitleInfo()
	{
		if (!canSendUserInfo)
			return;
		// Send a Server->Client packet UserInfo to this L2PcInstance
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		// Send a Server->Client packet TitleUpdate to all L2PcInstance in _KnownPlayers of the L2PcInstance
		if (Config.DEBUG)
			_log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + " packet: [S] cc TitleUpdate");
		broadcastPacket(new NicknameChanged(this));
	}
	
	@Override
	public void broadcastPacket(L2GameServerPacket mov)
	{
		boolean sendPacket = true;
		if (!canSendUserInfo && !(this instanceof Ghost))
			return;
		if (!(mov instanceof CharInfo))
			sendPacket(mov);
		mov.setInvisible(isInvisible());
		for (L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			if (player == null || player == this)
				continue;
			sendPacket = true;
			if (mov instanceof CharInfo)
			{
				final int relation = getRelation(player);
				if (getKnownList().getKnownRelations() != null && getKnownList().getKnownRelations().get(player.getObjectId()) != null && getKnownList().getKnownRelations().get(player.getObjectId()) != relation)
				{
					player.sendPacket(new RelationChanged(this, relation, player.isAutoAttackable(this)));
					if (getPet() != null)
						player.sendPacket(new RelationChanged(getPet(), relation, player.isAutoAttackable(this)));
				}
			}
			if (mov instanceof MagicSkillUse || mov instanceof MagicSkillLaunched)
			{
				if (getTarget() != player && player.getTarget() != this)
				{
					if (player.getVarB("hideSkillsAnim"))
					{
						sendPacket = false;
					}
				}
			}
			if (sendPacket)
				player.sendPacket(mov);
		}
	}
	
	@Override
	public void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist)
	{
		if (!canSendUserInfo)
			return;
		if (!(mov instanceof CharInfo))
			sendPacket(mov);
		mov.setInvisible(isInvisible());
		for (L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			if (player == null || player == this)
				continue;
			if (!isInsideRadius(player, radiusInKnownlist, false, false))
				continue;
			player.sendPacket(mov);
			if (mov instanceof CharInfo)
			{
				final int relation = getRelation(player);
				if (getKnownList().getKnownRelations().get(player.getObjectId()) != null && getKnownList().getKnownRelations().get(player.getObjectId()) != relation)
				{
					player.sendPacket(new RelationChanged(this, relation, player.isAutoAttackable(this)));
					if (getPet() != null)
						player.sendPacket(new RelationChanged(getPet(), relation, player.isAutoAttackable(this)));
				}
			}
		}
	}
	
	/**
	 * Return the Alliance Identifier of the L2PcInstance.<BR>
	 * <BR>
	 */
	public int getAllyId()
	{
		if (_clan == null)
			return 0;
		else
			return _clan.getAllyId();
	}
	
	public int getAllyCrestId()
	{
		if (getClanId() == 0)
		{
			return 0;
		}
		if (getClan().getAllyId() == 0)
		{
			return 0;
		}
		return getClan().getAllyCrestId();
	}
	
	public void queryGameGuard()
	{
		getClient().setGameGuardOk(false);
		this.sendPacket(new GameGuardQuery());
		if (Config.GAMEGUARD_ENFORCE)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new GameGuardCheck(), 30 * 1000);
		}
	}
	
	class GameGuardCheck implements Runnable
	{
		/**
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			L2GameClient client = getClient();
			if (client != null && !client.isAuthedGG() && isOnline() == 1)
			{
				GmListTable.broadcastMessageToGMs("Client " + client + " failed to reply GameGuard query and is being kicked!");
				_log.info("Client " + client + " failed to reply GameGuard query and is being kicked!");
				client.close(new LeaveWorld());
			}
		}
	}
	
	/**
	 * Send a Server->Client packet StatusUpdate to the L2PcInstance.<BR>
	 * <BR>
	 */
	@Override
	public void sendPacket(L2GameServerPacket packet)
	{
		if (_client != null)
		{
			_client.sendPacket(packet);
		}
	}
	
	/**
	 * Manage Interact Task with another L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the private store is a STORE_PRIVATE_SELL, send a Server->Client PrivateBuyListSell packet to the L2PcInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_BUY, send a Server->Client PrivateBuyListBuy packet to the L2PcInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_MANUFACTURE, send a Server->Client RecipeShopSellList packet to the L2PcInstance</li><BR>
	 * <BR>
	 *
	 * @param target
	 *            The L2Character targeted
	 */
	public void doInteract(L2Character target)
	{
		if (target instanceof L2PcInstance)
		{
			L2PcInstance temp = (L2PcInstance) target;
			sendPacket(ActionFailed.STATIC_PACKET);
			if (temp.getPrivateStoreType() == STORE_PRIVATE_SELL || temp.getPrivateStoreType() == STORE_PRIVATE_PACKAGE_SELL)
			{
				sendPacket(new PrivateStoreListSell(this, temp));
				CreatureSay cs = new CreatureSay(getObjectId(), Say2.PARTYROOM_COMMANDER, getName(), "Private stores use Blue Evas instead of Adenas, even though it says Adena");
				sendPacket(cs);
				sendPacket(new ExShowScreenMessage(1, -1, 5, 0, 0, 0, 0, true, 6000, 0, "Private stores use Blue Evas instead of Adenas, even though it says Adena"));
			}
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_BUY)
			{
				sendPacket(new PrivateStoreListBuy(this, temp));
				CreatureSay cs = new CreatureSay(getObjectId(), Say2.PARTYROOM_COMMANDER, getName(), "Private stores use Blue Evas instead of Adenas, even though it says Adena");
				sendPacket(cs);
				sendPacket(new ExShowScreenMessage(1, -1, 5, 0, 0, 0, 0, true, 6000, 0, "Private stores use Blue Evas instead of Adenas, even though it says Adena"));
			}
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
				sendPacket(new RecipeShopSellList(this, temp));
		}
		else
		{
			// _interactTarget=null should never happen but one never knows ^^;
			if (target != null)
				target.onAction(this);
		}
	}
	
	/**
	 * Manage AutoLoot Task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR>
	 * <BR>
	 *
	 * @param target
	 *            The L2ItemInstance dropped
	 */
	public void doAutoLoot(L2Attackable target, L2Attackable.RewardItem item)
	{
		double dropRateBoost = getStat().calcStat(Stats.DROP_COUNT_RATE, 1, null, null);
		if (target.getNpcId() == 18342 && getLevel() >= 76) // custom edit newbie gremlin
			return;
		if (item.getItemId() == 0)
			return;
		int count = item.getCount();
		// if(item.getChance() < 1000000)
//		if (item.getChance() < 10000)
//		{
//			String itemEnch = item.getEnchantLevel() > 0 ? "+" + item.getEnchantLevel() + " " : "";
//			String itemName = itemEnch + ItemTable.getInstance().getTemplate(item.getItemId()).getName();
//			String msg = getName() + " has obtained " + itemName + " from " + target.getName();
//			for (L2PcInstance allPlayers : L2World.getInstance().getAllPlayers().values())
//			{
//				// if(isGM())
//				// break;
//				allPlayers.sendPacket(new ExShowScreenMessage(1, -1, 2, 0, 1, 0, 0, true, 5000, 0, msg));
//			}
//		}
		if (item.getItemId() == 97003 || item.getItemId() == 4355 && dropRateBoost > 0)
		{
			double rate = calcStat(Stats.DROP_COUNT_RATE, 1, null, null);
			if (isGM())
				sendMessage(ItemTable.getInstance().getTemplate(item.getItemId()).getName() + " Bonus " + "x" + rate + " earned: [Bonus Count:" + ((int) (count * rate) - count) + " ]");
			count = (int) (count * rate);
			// System.out.println("" + count);
		}
		try
		{
			if (isInParty() && ItemTable.getInstance().getTemplate(item.getItemId()).getItemType() != L2EtcItemType.HERB)
				getParty().distributeItem(this, item, false, target);
			else if (item.getItemId() == 57)
				addAdena("Loot", count, target, true);
			else if (item.getItemId() == 98020)
			{
				setFame(getFame() + count);
				sendMessage("Your fame has increased by " + item.getCount());
			}
			else if (item.getItemId() == 98021)
			{
				setPvpKills(getPvpKills() + count);
				sendMessage("Your PvP count has increased by " + item.getCount());
			}
			else if (item.getItemId() == 98022)
			{
				if (getClan() != null)
				{
					getClan().setReputationScore(getClan().getReputationScore() + count, true);
					getClan().broadcastToOnlineMembers("Clan reputation increased by " + item.getCount() + " with the help of " + getName() + "!");
				}
			}
			else
				addItem("Loot", item.getItemId(), count, target, true, item.getEnchantLevel());
		}
		catch (Exception e)
		{
			_log.severe("doAutoLoot() failed in distributing itemId: " + item.getItemId() + " from " + target.getName());
		}
	}
	
	/**
	 * Manage Pickup Task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet StopMove to this L2PcInstance</li>
	 * <li>Remove the L2ItemInstance from the world and send server->client GetItem packets</li>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR>
	 * <BR>
	 *
	 * @param object
	 *            The L2ItemInstance to pick up
	 */
	protected void doPickupItem(L2Object object)
	{
		if (isAlikeDead() || isFakeDeath())
			return;
		// Set the AI Intention to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		// Check if the L2Object to pick up is a L2ItemInstance
		if (!(object instanceof L2ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			_log.warning("trying to pickup wrong target." + getTarget());
			return;
		}
		L2ItemInstance target = (L2ItemInstance) object;
		// Send a Server->Client packet ActionFailed to this L2PcInstance
		sendPacket(ActionFailed.STATIC_PACKET);
		// Send a Server->Client packet StopMove to this L2PcInstance
		StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());
		if (Config.DEBUG)
			_log.fine("pickup pos: " + target.getX() + " " + target.getY() + " " + target.getZ());
		sendPacket(sm);
		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isVisible())
			{
				// Send a Server->Client packet ActionFailed to this L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (((isInParty() && getParty().getLootDistribution() == L2Party.ITEM_LOOTER) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
				return;
			}
			if (isInvul() && !isGM())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			if (target.getOwnerId() != 0 && target.getOwnerId() != getObjectId() && !isInLooterParty(target.getOwnerId()))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				if (target.getItemId() == 57)
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
					smsg.addItemNumber(target.getCount());
					sendPacket(smsg);
				}
				else if (target.getCount() > 1)
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
					smsg.addItemName(target);
					smsg.addItemNumber(target.getCount());
					sendPacket(smsg);
				}
				else
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
					smsg.addItemName(target);
					sendPacket(smsg);
				}
				return;
			}
			// You can pickup only 1 combat flag
			if (FortSiegeManager.getInstance().isCombat(target.getItemId()))
			{
				if (!FortSiegeManager.getInstance().checkIfCanPickup(this))
					return;
			}
			if (target.getItemLootShedule() != null && (target.getOwnerId() == getObjectId() || isInLooterParty(target.getOwnerId())))
				target.resetOwnerTimer();
			// Remove the L2ItemInstance from the world and send server->client GetItem packets
			target.pickupMe(this);
			if (Config.SAVE_DROPPED_ITEM) // item must be removed from ItemsOnGroundManager if is active
				ItemsOnGroundManager.getInstance().removeObject(target);
		}
		// Auto use herbs - pick up
		if (target.getItemType() == L2EtcItemType.HERB)
		{
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getEtcItem());
			if (handler == null)
				_log.fine("No item handler registered for item ID " + target.getItemId() + ".");
			else
				handler.useItem(this, target, false);
			ItemTable.getInstance().destroyItem("Consume", target, this, null);
		}
		// Cursed Weapons are not distributed
		else if (CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
		{
			addItem("Pickup", target, null, true);
		}
		else if (FortSiegeManager.getInstance().isCombat(target.getItemId()))
		{
			addItem("Pickup", target, null, true);
		}
		else
		{
			// if item is instance of L2ArmorType or L2WeaponType broadcast an "Attention" system message
			if (target.getItemType() instanceof L2ArmorType || target.getItemType() instanceof L2WeaponType)
			{
				if (target.getEnchantLevel() > 0)
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ANNOUNCEMENT_C1_PICKED_UP_S2_S3);
					msg.addPcName(this);
					msg.addNumber(target.getEnchantLevel());
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
				else
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ANNOUNCEMENT_C1_PICKED_UP_S2);
					msg.addPcName(this);
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
			}
			// Check if a Party is in progress
			if (isInParty())
				getParty().distributeItemPickUp(this, target);
			// Target is adena
			else if (target.getItemId() == 57 && getInventory().getAdenaInstance() != null)
			{
				addAdena("Pickup", target.getCount(), this, true);
				ItemTable.getInstance().destroyItem("Pickup", target, this, null);
			}
			// Target is regular item
			else
				addItem("Pickup", target, this, true);
		}
	}
	
	public boolean canOpenPrivateStore()
	{
		if (isGM() && getAccessLevel().getLevel() < 4)
			return false;
		if (isAccountLockedDown() || isInJail())
			return false;
		/*
		 * if (!isGM() && getCharCreatedTime() + 24 * 60 * 60 * 1000 > System.currentTimeMillis())
		 * {
		 * sendMessage("You need to wait 24 hours after making a character to use vendor");
		 * return false;
		 * }
		 */
		if (isDisguised())
		{
			sendMessage("Can't make a store while disguised");
			return false;
		}
		return !isAlikeDead() && !isInOlympiadMode() && !isMounted() && !isInFunEvent() && !isInJail();
		// sendPacket(ActionFailed.STATIC_PACKET);
		// return false;
	}
	
	public void tryOpenPrivateBuyStore()
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY || getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY + 1)
			{
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			}
			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE)
			{
				if (isSitting())
				{
					standUp();
				}
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_BUY + 1);
				sendPacket(new PrivateStoreManageListBuy(this));
				CreatureSay cs = new CreatureSay(getObjectId(), Say2.PARTYROOM_COMMANDER, getName(), "Private stores use Blue Evas instead of Adenas, even though it says Adena");
				sendPacket(cs);
				sendPacket(new ExShowScreenMessage(1, -1, 5, 0, 0, 0, 0, true, 6000, 0, "Private stores use Blue Evas instead of Adenas, even though it says Adena"));
			}
		}
		else
		{
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public void tryOpenPrivateSellStore(boolean isPackageSale)
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL + 1 || getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
			{
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			}
			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE)
			{
				if (isSitting())
				{
					standUp();
				}
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_SELL + 1);
				sendPacket(new PrivateStoreManageListSell(this, isPackageSale));
				CreatureSay cs = new CreatureSay(getObjectId(), Say2.PARTYROOM_COMMANDER, getName(), "Private stores use Blue Evas instead of Adenas, even though it says Adena");
				sendPacket(cs);
				sendPacket(new ExShowScreenMessage(1, -1, 5, 0, 0, 0, 0, true, 6000, 0, "Private stores use Blue Evas instead of Adenas, even though it says Adena"));
			}
		}
		else
		{
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	@Override
	public boolean isTransformed()
	{
		return _transformation != null && !_transformation.isStance();
	}
	
	@Override
	public boolean isInStance()
	{
		return _transformation != null && _transformation.isStance();
	}
	
	public void transform(L2Transformation transformation)
	{
		if (_transformation != null)
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN));
			return;
		}
		if (isMounted())
			dismount();
		if (getPet() != null)
			getPet().unSummon(this);
		_transformation = transformation;
		for (L2Effect e : getAllEffects())
		{
			if (e != null && e.getSkill().isToggle())
				e.exit();
		}
		transformation.onTransform();
		broadcastUserInfo();
		sendSkillList();
		sendPacket(new SkillCoolTime(this));
	}
	
	@Override
	public void untransform()
	{
		if (_transformation != null)
		{
			_transformation.onUntransform();
			_transformation = null;
			stopEffects(L2EffectType.TRANSFORMATION);
			broadcastUserInfo();
			sendSkillList();
			sendPacket(new SkillCoolTime(this));
		}
	}
	
	public L2Transformation getTransformation()
	{
		return _transformation;
	}
	
	/**
	 * This returns the transformation Id of the current transformation.
	 * For example, if a player is transformed as a Buffalo, and then picks up the Zariche,
	 * the transform Id returned will be that of the Zariche, and NOT the Buffalo.
	 * 
	 * @return Transformation Id
	 */
	public int getTransformationId()
	{
		return (_transformation == null ? 0 : _transformation.getId());
	}
	
	/**
	 * This returns the transformation Id stored inside the character table, selected by the method: transformSelectInfo()
	 * For example, if a player is transformed as a Buffalo, and then picks up the Zariche,
	 * the transform Id returned will be that of the Buffalo, and NOT the Zariche.
	 * 
	 * @return Transformation Id
	 */
	public int transformId()
	{
		return _transformationId;
	}
	
	/**
	 * This is a simple query that inserts the transform Id into the character table for future reference.
	 */
	public void transformInsertInfo()
	{
		_transformationId = getTransformationId();
		if (_transformationId == L2Transformation.TRANSFORM_AKAMANAH || _transformationId == L2Transformation.TRANSFORM_ZARICHE)
			return;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_TRANSFORM);
			statement.setInt(1, _transformationId);
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Transformation insert info: ", e);
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
	
	/**
	 * This selects the current
	 * 
	 * @return transformation Id
	 */
	public int transformSelectInfo()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_CHAR_TRANSFORM);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			rset.next();
			_transformationId = rset.getInt("transform_id");
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Transformation select info: ", e);
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
		return _transformationId;
	}
	
	/**
	 * Set a target.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character</li>
	 * <li>Add the L2PcInstance to the _statusListener of the new target if it's a L2Character</li>
	 * <li>Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)</li><BR>
	 * <BR>
	 *
	 * @param newTarget
	 *            The L2Object to target
	 */
	@Override
	public void setTarget(L2Object newTarget)
	{
		if (newTarget != null)
		{
			boolean isParty = (((newTarget instanceof L2PcInstance) && isInParty() && getParty().getPartyMembers().contains(newTarget)));
			// Check if the new target is visible
			if (!isParty && !newTarget.isVisible())
				newTarget = null;
			// Prevents /target exploiting
			if (newTarget != null && !isParty && Math.abs(newTarget.getZ() - getZ()) > 1000)
				newTarget = null;
		}
		if (!isGM())
		{
			// Can't target and attack festival monsters if not participant
			if ((newTarget instanceof L2FestivalMonsterInstance) && !isFestivalParticipant())
				newTarget = null;
			// Can't target and attack rift invaders if not in the same room
			else if (isInParty() && getParty().isInDimensionalRift())
			{
				byte riftType = getParty().getDimensionalRift().getType();
				byte riftRoom = getParty().getDimensionalRift().getCurrentRoom();
				if (newTarget != null && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(newTarget.getX(), newTarget.getY(), newTarget.getZ()))
					newTarget = null;
			}
		}
		// Get the current target
		L2Object oldTarget = getTarget();
		if (oldTarget != null)
		{
			if (oldTarget.equals(newTarget))
				return; // no target change
			// Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character
			if (oldTarget instanceof L2Character)
				((L2Character) oldTarget).removeStatusListener(this);
		}
		// Add the L2PcInstance to the _statusListener of the new target if it's a L2Character
		if (newTarget instanceof L2Character)
		{
			((L2Character) newTarget).addStatusListener(this);
			TargetSelected my = new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ());
			broadcastPacket(my);
		}
		if (newTarget == null && getTarget() != null)
		{
			broadcastPacket(new TargetUnselected(this));
		}
		// Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)
		super.setTarget(newTarget);
	}
	
	/**
	 * Return the active weapon instance (always equiped in the right hand).<BR>
	 * <BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}
	
	/**
	 * Return the active weapon item (always equiped in the right hand).<BR>
	 * <BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();
		if (weapon == null)
			return getFistsWeaponItem();
		return (L2Weapon) weapon.getItem();
	}
	
	public L2ItemInstance getActiveCloakItem()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_BACK);
	}
	
	public L2ItemInstance getActiveShieldItem()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}
	
	public L2ItemInstance getChestArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}
	
	public L2ItemInstance getLegsArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
	}
	
	public L2ItemInstance getGlovesInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
	}
	
	public L2ItemInstance getBootsInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);
	}
	
	public L2ItemInstance getBackInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_BACK);
	}
	
	public L2ItemInstance getHair1Instance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
	}
	
	public L2ItemInstance getHair2Instance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
	}
	
	public L2Armor getActiveChestArmorItem()
	{
		L2ItemInstance armor = getChestArmorInstance();
		if (armor == null)
			return null;
		return (L2Armor) armor.getItem();
	}
	
	public L2Armor getActiveLegsArmorItem()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		if (legs == null)
			return null;
		return (L2Armor) legs.getItem();
	}
	
	public boolean isWearingHeavyArmor()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		L2ItemInstance armor = getChestArmorInstance();
		if (armor != null && legs != null)
		{
			if ((L2ArmorType) legs.getItemType() == L2ArmorType.HEAVY && ((L2ArmorType) armor.getItemType() == L2ArmorType.HEAVY))
				return true;
		}
		if (armor != null)
		{
			if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR && (L2ArmorType) armor.getItemType() == L2ArmorType.HEAVY))
				return true;
		}
		return false;
	}
	
	public boolean isWearingLightArmor()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		L2ItemInstance armor = getChestArmorInstance();
		if (armor != null && legs != null)
		{
			if ((L2ArmorType) legs.getItemType() == L2ArmorType.LIGHT && ((L2ArmorType) armor.getItemType() == L2ArmorType.LIGHT))
				return true;
		}
		if (armor != null)
		{
			if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR && (L2ArmorType) armor.getItemType() == L2ArmorType.LIGHT))
				return true;
		}
		return false;
	}
	
	public boolean isWearingMagicArmor()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		L2ItemInstance armor = getChestArmorInstance();
		if (armor != null && legs != null)
		{
			if ((L2ArmorType) legs.getItemType() == L2ArmorType.MAGIC && ((L2ArmorType) armor.getItemType() == L2ArmorType.MAGIC))
				return true;
		}
		if (armor != null)
		{
			if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR && (L2ArmorType) armor.getItemType() == L2ArmorType.MAGIC))
				return true;
		}
		return false;
	}
	
	public boolean isWearingFormalWear()
	{
		return _IsWearingFormalWear;
	}
	
	public void setIsWearingFormalWear(boolean value)
	{
		_IsWearingFormalWear = value;
	}
	
	public boolean isThisCharacterMarried()
	{
		return _married;
	}
	
	public void setIsThisCharacterMarried(boolean state)
	{
		_married = state;
	}
	
	public boolean isEngageRequest()
	{
		return _engagerequest;
	}
	
	public void setEngageRequest(boolean state, int playerid)
	{
		_engagerequest = state;
		_engageid = playerid;
	}
	
	public void setMaryRequest(boolean state)
	{
		_marryrequest = state;
	}
	
	public boolean isMaryRequest()
	{
		return _marryrequest;
	}
	
	public void setMarryAccepted(boolean state)
	{
		_marryaccepted = state;
	}
	
	public boolean isMarryAccepted()
	{
		return _marryaccepted;
	}
	
	public int getEngageId()
	{
		return _engageid;
	}
	
	public int getPartnerId()
	{
		return _partnerId;
	}
	
	public void setPartnerId(int partnerid)
	{
		_partnerId = partnerid;
	}
	
	public int getCoupleId()
	{
		return _coupleId;
	}
	
	public void setCoupleId(int coupleId)
	{
		_coupleId = coupleId;
	}
	
	public void EngageAnswer(int answer)
	{
		if (_engagerequest == false)
			return;
		else if (_engageid == 0)
			return;
		else
		{
			L2PcInstance ptarget = (L2PcInstance) L2World.getInstance().findObject(_engageid);
			setEngageRequest(false, 0);
			if (ptarget != null)
			{
				if (answer == 1)
				{
					CoupleManager.getInstance().createCouple(ptarget, L2PcInstance.this);
					ptarget.sendMessage("Request to Engage has been >ACCEPTED<");
				}
				else
					ptarget.sendMessage("Request to Engage has been >DENIED<!");
			}
		}
	}
	
	/**
	 * Return the secondary weapon instance (always equiped in the left hand).<BR>
	 * <BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}
	
	/**
	 * Return the secondary weapon item (always equiped in the left hand) or the fists weapon.<BR>
	 * <BR>
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		L2ItemInstance weapon = getSecondaryWeaponInstance();
		if (weapon == null)
			return getFistsWeaponItem();
		L2Item item = weapon.getItem();
		if (item instanceof L2Weapon)
			return (L2Weapon) item;
		return null;
	}
	
	public L2PcInstance getLastPCKiller()
	{
		return _lastKiller;
	}
	
	/**
	 * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and Item Drop.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty</li>
	 * <li>If necessary, unsummon the Pet of the killed L2PcInstance</li>
	 * <li>Manage Karma gain for attacker and Karam loss for the killed L2PcInstance</li>
	 * <li>If the killed L2PcInstance has Karma, manage Drop Item</li>
	 * <li>Kill the L2PcInstance</li><BR>
	 * <BR>
	 *
	 * @param i
	 *            The HP decrease value
	 * @param attacker
	 *            The L2Character who attacks
	 * @see net.sf.l2j.gameserver.model.actor.L2Playable#doDie(net.sf.l2j.gameserver.model.actor.L2Character)
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		if (isInEvent())
		{
			LunaEvent activeEvent = EventManager.getInstance().getActiveEvent();
			if (activeEvent != null)
				activeEvent.onDeath(this);
		}
		if (isMounted())
			stopFeed();
		for (L2Effect e : getAllEffects())
		{
			if (e != null)
			{
				if (e.isStolen() /* || e instanceof EffectInvisible */ || e instanceof EffectImmobileAutoAttack)
					e.exit();
			}
		}
		synchronized (this)
		{
			if (isFakeDeath())
				stopFakeDeath(null);
		}
		if (isInLastTeamStandingEvent() && LastTeamStanding.getInstance().getState().equals(LastTeamStanding.State.ACTIVE))
		{
			LastTeamStanding.getInstance().onDeath(this);
		}
		if (isInDominationEvent() && Domination.getInstance().getState() == Domination.State.ACTIVE)
		{
			Domination.getInstance().onDeath(this);
		}
		if (isInLastManStandingEvent())
		{
			LastManStanding.getInstance().removePlayerOnDeath(this);
		}
		if (killer instanceof L2PcInstance && ((L2PcInstance) killer).isInLastManStandingEvent() && killer != this && killer != null)
		{
			LastManStanding.getInstance().setLastKiller((L2PcInstance) killer);
		}
		if (killer != null)
		{
			L2PcInstance pk = killer.getActingPlayer();
			if (isInEvent())
			{
				LunaEvent activeEvent = EventManager.getInstance().getActiveEvent();
				if (activeEvent != null && activeEvent.getState().equals(EventState.ACTIVE))
				{
					if (activeEvent instanceof LunaTvT)
					{
						if (pk.getTeamType().equals(TeamType.BLUE))
							LunaTvT.getInstance().increaseBlueKills();
						else
							LunaTvT.getInstance().increaseRedKills();
					}
				}
			}
			String killerName = killer.getDisplayName();
			if (_streak >= 100)
			{
				Announcements.getInstance().announceToAll(killerName + " has put an end to " + getName() + "'s reign of terror of " + _streak + " straight kills!");
			}
			else if (_streak >= 55)
			{
				Announcements.getInstance().announceToAll(killerName + " has ended " + getName() + "'s rampage of " + _streak + " straight kills!");
			}
			else if (_streak >= 25)
			{
				Announcements.getInstance().announceToAll(killerName + " has stopped " + getName() + "'s killing spree of " + _streak + " kills!");
			}
			if (_streak < 60)
			{
				if (_tempHero && !(isAFOSLeader() || isFOSHero())) // killing streak hero
				{
					stopTempHeroTask();
					if (getPvpKills() < 340000)
					{
						if (!isFakeHero()) // an actual hero from oly
						{
							removeHeroSkillsOnSubclasses(); // just remove skills if they're on a subclass and keep the hero
						}
						else // hero from killing streaks or hero from fos events
						{
							setHero(false);
							setFakeHero(false);
						}
					}
					_tempHero = false;
					broadcastUserInfo();
				}
			}
			_streak = 0;
			if (pk != null)
			{
				TvTEvent.onKill(killer, this);
				if (atEvent)
				{
					pk.kills.add(getName());
				}
				_lastKiller = pk;
			}
			else
				_lastKiller = null;
			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
			if (isInFunEvent())
			{
				if (_inEventTvT && TvT._started)
					TvT.onDeath(this, killer);
				else if (_inEventTvT && NewTvT._started)
					NewTvT.onDeath(this, killer);
				else if (_inEventFOS && FOS._started)
					FOS.onDeath(this, killer);
				else if (_inEventFOS && NewFOS._started)
					NewFOS.onDeath(this, killer);
				else if (_inEventCTF && CTF._started)
					CTF.onDeath(this, killer);
				else if (_inEventCTF && NewCTF._started)
					NewCTF.onDeath(this, killer);
				else if (_inEventVIP)
					VIP.onDeath(this, killer);
				else if (_inEventDM && DM._started)
					DM.onDeath(this, killer);
				else if (_inEventDM && NewDM._started)
					NewDM.onDeath(this, killer);
				else if (_inEventLunaDomi && NewDomination._started)
					NewDomination.onDeath(this, killer, EventVarHolder.getInstance().getRunningEventId());
				else if (_inEventHG)
					NewHuntingGrounds.onDeath(this, killer);
			}
			else if (isCursedWeaponEquipped())// Issues drop of Cursed Weapon.
			{
				if (pk != null)
					pk.increasePvpKills(this, true);
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, killer);
			}
			else if (isCombatFlagEquipped())
			{
				FortSiegeManager.getInstance().dropCombatFlag(this);
			}
			else
			{
				if (pk == null || !pk.isCursedWeaponEquipped())
				{
					onDieDropItem(killer); // Check if any item should be dropped
					if (!((isInsideZone(ZONE_PVP)/* && !isInPI() */) && !isInsideZone(ZONE_SIEGE)))
					{
						if (pk != null && pk.getClan() != null && getClan() != null)
						{
							if (isInClanwarWith(pk) || (isInSiege() && pk.isInSiege()))
							{
								if (getClan().getReputationScore() > 0)
								{
									pk.getClan().setReputationScore(pk.getClan().getReputationScore() + Config.REPUTATION_SCORE_PER_KILL, true);
								}
								/*
								 * if (pk.getClan().getReputationScore() > 0)
								 * {
								 * _clan.setReputationScore(_clan.getReputationScore() - Config.REPUTATION_SCORE_PER_KILL, true);
								 * getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
								 * pk.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(pk.getClan()));
								 * }
								 */
							}
						}
					}
					/*
					 * if (Config.ALT_GAME_DELEVEL)
					 * {
					 * // Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty
					 * // NOTE: deathPenalty +- Exp will update karma
					 * // Penalty is lower if the player is at war with the pk (war has to be declared)
					 * if (getStat().getLevel() > 70)
					 * {
					 * boolean siege_npc = false;
					 * if (killer instanceof L2FortSiegeGuardInstance || killer instanceof L2SiegeGuardInstance || killer instanceof L2FortCommanderInstance)
					 * siege_npc = true;
					 * deathPenalty(pk != null && getClan() != null && getClan().isAtWarWith(pk.getClanId()), pk != null, siege_npc);
					 * }
					 * }
					 * else
					 */
					{
						if (!(isInsideZone(ZONE_PVP) && !isInSiege()) || pk == null)
							onDieUpdateKarma(); // Update karma if delevel is not allowed
					}
				}
			}
		}
		/*
		 * // Untransforms character.
		 * if (!isFlyingMounted() && isTransformed())
		 * untransform();
		 */
		// Unsummon the Pet
		if (getPet() != null)
		{
			// getPet().doDie(_summon);
			if (getStat().calcStat(Stats.PET_NO_UNSUMMON_AFTER_OWNER_DIE, 0, null, null) <= 0)
				getPet().unSummon(this);
		}
		if (_fusionSkill != null)
			abortCast();
		for (L2Character character : getKnownList().getKnownCharacters())
			if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
				character.abortCast();
		if (isInParty() && getParty().isInDimensionalRift())
			getParty().getDimensionalRift().getDeadMemberList().add(this);
		/*
		 * if (getAgathionId() != 0)
		 * setAgathionId(0);
		 */
		stopRentPet();
		stopWaterTask();
		if (isPhoenixBlessed() && !(CTF._started && _inEventCTF))
			reviveRequest(this, null, false);
		return true;
	}
	
	private void onDieDropItem(L2Character killer)
	{
		if (isInFunEvent() || killer == null)
			return;
		final L2PcInstance pk = killer.getActingPlayer();
		if (isInsideZone(L2Character.ZONE_CHAOTIC) || killer.isInsideZone(L2Character.ZONE_CHAOTIC))
			return;
		if (killer instanceof L2Playable && getInSameClanAllyAs(pk) > 0)
			return;
		if ((!isInsideZone(ZONE_PVP) || pk == null) && (!isGMReally() || Config.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			boolean isKillerNpc = (killer instanceof L2Npc);
			int pkLimit = Config.KARMA_PK_LIMIT;
			int dropEquip = 0;
			int dropEquipWeapon = 0;
			int dropItem = 0;
			int dropLimit = 0;
			int dropPercent = 0;
			if (getKarma() > 0 && getPkKills() >= pkLimit)
			{
				isKarmaDrop = true;
				dropPercent = Config.KARMA_RATE_DROP;
				dropEquip = Config.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.KARMA_RATE_DROP_ITEM;
				dropLimit = Config.KARMA_DROP_LIMIT;
			}
			else if (isKillerNpc && getLevel() > 4 && !isFestivalParticipant())
			{
				dropPercent = Config.PLAYER_RATE_DROP;
				dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.PLAYER_RATE_DROP_ITEM;
				dropLimit = Config.PLAYER_DROP_LIMIT;
			}
			if (dropPercent > 0 && Rnd.get(100) < dropPercent)
			{
				int dropCount = 0;
				int itemDropPercent = 0;
				boolean straightOutDestroyItem = false;
				int count = 0;
				for (L2ItemInstance itemDrop : getInventory().getItems())
				{
					if (itemDrop == null || itemDrop.getItemId() == 70100)
						continue;
					straightOutDestroyItem = /* itemDrop.isShadowItem() || itemDrop.isTimeLimitedItem() || */!itemDrop.isTradeable();
					if (!itemDrop.isDropableKarma() || // Don't drop
					itemDrop.isHeroItem() || itemDrop.isCastleItem() || itemDrop.getItemId() == 57 || // Adena
					itemDrop.getItem().getType2() == L2Item.TYPE2_QUEST || // Quest Items
					getPet() != null && getPet().getControlItemId() == itemDrop.getItemId() || // Control Item of active pet
					Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_ITEMS, itemDrop.getItemId()) >= 0 || // Item listed in the non droppable item list
					Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_PET_ITEMS, itemDrop.getItemId()) >= 0 // Item listed in the non droppable pet item list
					)
						continue;
					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
						itemDropPercent = itemDrop.getItem().getType2() == L2Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlotAndRecord(itemDrop.getLocationSlot());
					}
					else
						itemDropPercent = dropItem; // Item in inventory
					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						if (straightOutDestroyItem)
						{
							// if (Rnd.get(100) > 98)
							// {
							// if(itemDrop.isDread())
							// {
							// sendMessage("Your "+itemDrop.getName() + " is protected by server's team and it didn't get penalised.");
							// }
							// else
							// {
							// destroyItem("DieItemDestroy", itemDrop, killer, true);
							// }
							// }
							// else
							count++;
						}
						else
							dropItem("DieDrop", itemDrop, killer, true);
						if (isKarmaDrop)
						{
							if (straightOutDestroyItem)
								_log.warning(getName() + " has karma and destroyed id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());
							else
								_log.warning(getName() + " has karma and dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());
						}
						else
							_log.warning(getName() + " dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());
						if (++dropCount >= dropLimit)
							break;
					}
				}
				if (count > 0 && getLevel() > 87) // exp penalty
				{
					long totalexplost = (long) (count * (Experience.LEVEL[getLevel()] - Experience.LEVEL[getLevel() - 1]) * 0.20);
					removeExpAndSp((long) (totalexplost * Config.KARMA_EXP_LOST_MUL), 0);
					sendMessage("You died with karma and lost experience in proportion to # of untradeable items that were supposed to be dropped");
				}
			}
		}
	}
	
	private void onDieUpdateKarma()
	{
		// Karma lose for server that does not allow delevel
		if (getKarma() > 0)
		{
			double karmaLost = Config.KARMA_LOST_BASE;
			karmaLost = Math.max(karmaLost, getKarma() / 5);
			if (karmaLost < 10)
				karmaLost = 10;
			// Decrease Karma of the L2PcInstance and Send it a Server->Client StatusUpdate packet with Karma and PvP Flag if necessary
			setKarma(getKarma() - (int) karmaLost);
		}
	}
	
	public void onKillUpdatePvPKarma(L2Character target, boolean awardPvp)
	{
		if (target == null)
			return;
		if (!(target instanceof L2Playable))
			return;
		final L2PcInstance targetPlayer = target.getActingPlayer();
		if (targetPlayer == null)
			return; // Target player is null
		if (targetPlayer == this)
			return; // Target player is self
		if (isInFunEvent() && targetPlayer.isInFunEvent())
		{
			// if (_countTvTkills > 19 || _countFOSKills > 19 || _countDMkills > 18)
			// return;
			increasePvpKills(targetPlayer, false);
			return;
		}
		if (isCursedWeaponEquipped())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquippedId);
			if (Rnd.nextInt(100) < 40)
				increasePvpKills(target.getActingPlayer(), false);
			CursedWeapon cw = CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId);
			SystemMessage msg = new SystemMessage(SystemMessageId.THERE_IS_S1_HOUR_AND_S2_MINUTE_LEFT_OF_THE_FIXED_USAGE_TIME);
			int timeLeftInHours = (int) (((cw.getTimeLeft() / 60000) / 60));
			msg.addItemName(_cursedWeaponEquippedId);
			msg.addNumber(timeLeftInHours);
			sendPacket(msg);
			return;
		}
		// If in duel and you kill (only can kill l2summon), do nothing
		if (isInDuel() && targetPlayer.isInDuel())
			return;
		// if same clan member, then do nothing
		if (targetPlayer.getClan() != null && getClan() != null)
		{
			if (targetPlayer.getClanId() == getClanId())
				return;
			if (getAllyId() != 0 && targetPlayer.getAllyId() == getAllyId())
				return;
		}
		if (getParty() != null && getParty().getPartyMembers().contains(targetPlayer))
			return;
		if (targetPlayer.isInsideZone(ZONE_SIEGE) && isInsideZone(ZONE_SIEGE))
		{
			if (awardPvp && target instanceof L2PcInstance)
				increasePvpKills(targetPlayer, false);
			return;
		}
		if (awardPvp && isInPI() && targetPlayer.isInPI())
		{
			if (target instanceof L2PcInstance)
				increasePvpKills(targetPlayer, false);
			return;
		}
		// If in Arena, do nothing
		if (isInsideZone(ZONE_PVP) || targetPlayer.isInsideZone(ZONE_PVP))
			return;
		if (isThisCharacterMarried() && getPartnerId() == targetPlayer.getObjectId())
			return;
		final long time = System.currentTimeMillis();
		// Check if it's pvp
		if (checkIfPvP(targetPlayer)/* && targetPlayer.getPvpFlagLasts() >= (time - 2000) */) // Target player has pvp flag set
		{
			if (awardPvp && target instanceof L2PcInstance)
				increasePvpKills(targetPlayer, false);
		}
		else // Target player doesn't have pvp flag set
		{
			// check about wars
			if (isInClanwarWith(targetPlayer))
			{
				// 'Both way war' -> 'PvP Kill'
				if (awardPvp && target instanceof L2PcInstance)
					increasePvpKills(targetPlayer, false);
				return;
			}
			// 'No war' or 'One way war' -> 'Normal PK'
			if (targetPlayer.getKarma() > 0) // Target player has karma
			{
				if (Config.KARMA_AWARD_PK_KILL)
				{
					if (awardPvp && target instanceof L2PcInstance)
						increasePvpKills(targetPlayer, false);
				}
			}
			else if (targetPlayer.getPvpFlagLasts() < (time - 2000)) // Target player doesn't have karma
			{
				if (canPKdueToOnesidedClanwar(targetPlayer))
					return;
				if (((L2Playable) target).isIgnorePK())
				{
					((L2Playable) target).setIgnorePK(false);
					return;
				}
				increasePkKillsAndKarma(targetPlayer.getLevel(), target instanceof L2PcInstance && !isInOneSideClanwarWith(targetPlayer));
				// Unequip adventurer items
				if (getInventory().getPaperdollItemId(7) >= 7816 && getInventory().getPaperdollItemId(7) <= 7831)
				{
					L2ItemInstance invItem = getInventory().getItemByItemId(getInventory().getPaperdollItemId(7));
					if (invItem.isEquipped())
					{
						L2ItemInstance[] unequiped = getInventory().unEquipItemInSlotAndRecord(invItem.getLocationSlot());
						InventoryUpdate iu = new InventoryUpdate();
						for (L2ItemInstance itm : unequiped)
							iu.addModifiedItem(itm);
						sendPacket(iu);
					}
					sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_UNABLE_TO_EQUIP_THIS_ITEM_WHEN_YOUR_PK_COUNT_IS_GREATER_THAN_OR_EQUAL_TO_ONE));
				}
			}
		}
	}
	
	public boolean canPKdueToOnesidedClanwar(final L2PcInstance targetPlayer)
	{
		return isInOneSideClanwarWith(targetPlayer) && (/* (getClan().getLevel() >= 6 && targetPlayer.getClan().getLevel() >= 7 && targetPlayer.getClan().getMembersCount() > 20) || */targetPlayer.isInOneSideClanwarWith(this));
	}
	
	/**
	 * Increase the pvp kills count and send the info to the player
	 */
	final private void increasePvpKills(final L2PcInstance target, final boolean bonus)
	{
		increasePvpKills(target, bonus, false, null, 1);
	}
	
	private boolean isInHotZone()
	{
		return isInFunEvent() || isInHuntersVillage() || isInOrcVillage() || isInGludin() || isInPI() || isInsideZone(L2Character.ZONE_FARM) || isInSiege() || isInFT() || isInsideZone(L2Character.ZONE_SIEGE);
	}
	
	final private void increasePvpKills(final L2PcInstance target, final boolean bonus, final boolean synergy, final String name, final int fameCarriedOver)
	{
		boolean passedProtections = true;
		String msg = "";
		if (target == null) // custom edit
			return;
		if (Config.PVP_PROTECTIONS && !isInFunEvent() && !isGM())
		{
			if (! (target instanceof Ghost))
			{
				if (target.getObjectId() == _prevKill || target.getObjectId() == _prevKill2)
				{
					passedProtections = false;
					msg = "You have already received a pvp from this player.";
					sendMessage(msg);
					return;
				}

				if (target.getIP().equalsIgnoreCase(_prev1IP) || target.getIP().equalsIgnoreCase(_prev2IP))
				{
					msg = "Same Ip as before, you have already earned +1 from this ip.";
					sendMessage(msg);
					passedProtections = false;
					return;
				}
			}
			if (getParty() != null && getParty().getPartyMembers().contains(target))
			{
				passedProtections = false;
				msg = "You're in the same party wtf are you doin.";
				sendMessage(msg);
				return;
			}
			if (getAllyId() != 0 && getAllyId() == target.getAllyId())
			{
				passedProtections = false;
				msg = "You're in the same ally wtf are you doin.";
				sendMessage(msg);
				return;
			}
			if (getClanId() != 0 && getClanId() == target.getClanId())
			{
				passedProtections = false;
				msg = "You're in the same clan wtf are you doin.";
				sendMessage(msg);
				return;
			}
			if (getIP() != null && target.getIP() != null && getIP().equals(target.getIP()))
			{
				msg = "Same Ip this might be you or someone next to you, be friendly with your locals.";
				sendMessage(msg);
				passedProtections = false;
				return;
			}
		}
		if (target != null && passedProtections)
		{
			// storePvPForRanking(this, target);
			/*
			 * if (isInGludin() || isInOrcVillage() || isInFunEvent())
			 * gearScoreStatus = analyzeGearDiff(this, target);
			 */
			try
			{
				if (bonus && !synergy)
				{
					setPvpKills(getPvpKills() + 10);
					sendMessage("You have gained 10 PvPs for defeating a cursed player");
				}
				setPvpKills(getPvpKills() + 1);
				if (Config.ENABLE_CLAN_WAR_BONUS_PVP)
				{
					if (isInClanwarWith(target))
					{
						final L2Clan clan = getClan();
						clan.setReputationScore(clan.getReputationScore() + Config.BONUS_CLAN_REP_AMMOUNT_2_SIDE, true);
						setPvpKills(getPvpKills() + Config.BONUS_PVP_AMMOUNT_2_SIDE);
						sendMessage("You earned " + String.valueOf(Config.BONUS_CLAN_REP_AMMOUNT_2_SIDE) + " clan rep for killing a war");
						sendMessage("You earned " + String.valueOf(Config.BONUS_PVP_AMMOUNT_2_SIDE) + " pvp(s) for killing a war");
					}
					else if (isInOneSideClanwarWith(target))
					{
						final L2Clan clan = getClan();
						clan.setReputationScore(clan.getReputationScore() + Config.BONUS_CLAN_REP_AMMOUNT_1_SIDE, true);
						setPvpKills(getPvpKills() + Config.BONUS_PVP_AMMOUNT_1_SIDE);
						sendMessage("You earned " + String.valueOf(Config.BONUS_CLAN_REP_AMMOUNT_1_SIDE) + " clan rep for killing a war");
						sendMessage("You earned " + String.valueOf(Config.BONUS_PVP_AMMOUNT_1_SIDE) + " pvp(s) for killing a war");
					}
				}
				Calendar cal = Calendar.getInstance();
				if (Config.DOUBLE_PVP_WEEKEND && (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) && isInHotZone())
				{
					setPvpKills(getPvpKills() + 1);
					sendMessage("You received 1 extra PvP.");
				}
				if (Config.DOUBLE_PVP_WEEKEND && !(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY))
				{
					if (LunaGlobalVariablesHolder.getInstance().getDoublePvPsHunter() && isInHuntersVillage())
					{
						setPvpKills(getPvpKills() + 1);
						sendMessage("You received 1 extra PvP.");
					}
					if (LunaGlobalVariablesHolder.getInstance().getDoublePvPsGludin() && isInGludin())
					{
						setPvpKills(getPvpKills() + 1);
						sendMessage("You received 1 extra PvP.");
					}
					if (LunaGlobalVariablesHolder.getInstance().getDoublePvPsGludin() && isInPI())
					{
						setPvpKills(getPvpKills() + 1);
						sendMessage("You received 1 extra PvP.");
					}
					if (isInFunEvent() && (LunaGlobalVariablesHolder.getInstance().getDoublePvPsGludin() || LunaGlobalVariablesHolder.getInstance().getDoublePvPsHunter() || LunaGlobalVariablesHolder.getInstance().getDoublePvPsPI()))
					{
						setPvpKills(getPvpKills() + 1);
						sendMessage("You received 1 extra PvP.");
					}
				}
				if (!Config.DOUBLE_PVP_WEEKEND)
				{
					if (LunaGlobalVariablesHolder.getInstance().getDoublePvPsHunter() && isInHuntersVillage())
					{
						setPvpKills(getPvpKills() + 1);
						sendMessage("You received 1 extra PvP.");
					}
					if (LunaGlobalVariablesHolder.getInstance().getDoublePvPsGludin() && isInGludin())
					{
						setPvpKills(getPvpKills() + 1);
						sendMessage("You received 1 extra PvP.");
					}
					if (LunaGlobalVariablesHolder.getInstance().getDoublePvPsPI() && isInPI())
					{
						setPvpKills(getPvpKills() + 1);
						sendMessage("You received 1 extra PvP.");
					}
					if (isInFunEvent() && (LunaGlobalVariablesHolder.getInstance().getDoublePvPsGludin() || LunaGlobalVariablesHolder.getInstance().getDoublePvPsHunter() || LunaGlobalVariablesHolder.getInstance().getDoublePvPsPI()))
					{
						setPvpKills(getPvpKills() + 1);
						sendMessage("You received 1 extra PvP.");
					}
				}
				if (isInSiege())
				{
					setSiegeKills(getSiegeKills() + 1);
				}
				if (isInFunEvent())
				{
					setEventKills(getEventKills() + 1);
				}
				setNameColorsDueToPVP();
				if ((!(isInHuntersVillage() || isInOrcVillage()) && !(target.isInHuntersVillage() || target.isInOrcVillage()) || _famestreak < 10))
					_famestreak++;
				_streak++;
				if (_streak == 25)
				{
					sendMessage("You're almost on a killing spree!");
				}
				else if (_streak == 30)
				{
					if (Rnd.nextInt(100) < 3)
						Announcements.getInstance().announceToAll(getName() + " is Seung-Hui Cho!");
					else
						Announcements.getInstance().announceToAll(getName() + " is on a killing spree!");
					if (isHero())
					{
						if (_activeClass != _baseClass && getPvpKills() < 340000)
						{
							sendMessage("You have been awarded hero skills for your current class for your killing spree!");
							giveHeroSkills();
						}
					}
					else
					{
						sendMessage("You have been awarded a temporary hero aura and skills on your current class for your killing spree!");
						giveHeroSkills();
						setFakeHero(true); // Since it's only for the event, we don't want him to get Hero gear from the event manager.
					}
					_tempHero = true;
					broadcastPacket(new SocialAction(getObjectId(), 16));
					broadcastUserInfo();
					startTempHeroTask(60);
				}
				else if (_streak == 60)
					Announcements.getInstance().announceToAll(getName() + " is on a rampage dropping people like flies!");
				else if (_streak == 100)
					Announcements.getInstance().announceToAll(getName() + " is going postal! everybody run!");
				int fameIncrease = 1;
				final boolean war = isInOneSideClanwarWith(target);
				fameIncrease = target.getFameStreak() / 2 + 1;
				if (synergy)
					fameIncrease = fameCarriedOver;
				if (isInFT())
					fameIncrease++;
				if (war)
				{
					if (fameIncrease > 60)
						fameIncrease = 60;
					if (isInClanwarWith(target))
					{
						fameIncrease += 1;
					}
				}
				else
				{
					if (fameIncrease > 15)
						fameIncrease = 15;
				}
				setFame(getFame() + fameIncrease);
				SystemMessage sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
				sm.addNumber(fameIncrease);
				sendPacket(sm);
				fameIncrease = 1;
				if (!isCursedWeaponEquipped() && !isInFunEvent())
				{
					int val = 3;
					if (Rnd.get(val) < 2)
					{
						final L2ItemInstance activeWeapon = getActiveWeaponInstance();
						if (activeWeapon != null && !activeWeapon.isHeroItem())
							activeWeapon.attemptToIncreaseEnchantViaPVP(this);
					}
					final int slot = Rnd.nextInt(5);
					final int type = Rnd.nextInt(2);
					L2ItemInstance toBeProgressed = null;
					switch (slot)
					{
						case 0:
							if (type == 0)
								toBeProgressed = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
							else
								toBeProgressed = getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK);
							break;
						case 1:
							if (type == 0)
							{
								toBeProgressed = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
								if (toBeProgressed == null)
									toBeProgressed = getInventory().getPaperdollItem(Inventory.PAPERDOLL_FULLARMOR);
							}
							else
								toBeProgressed = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR);
							break;
						case 2:
							if (type == 0)
								toBeProgressed = getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD);
							else
								toBeProgressed = getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR);
							break;
						case 3:
							if (type == 0)
								toBeProgressed = getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
							else
								toBeProgressed = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER);
							break;
						case 4:
							if (type == 0)
								toBeProgressed = getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);
							else
								toBeProgressed = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER);
							break;
					}
					if (toBeProgressed != null)
						toBeProgressed.attemptToIncreaseEnchantViaPVP(this);
					if (!bonus)
					{
						if (Rnd.nextInt(17) == 0)
							seeIfAnAlreadyEnchantedSkillWillBeEnchantedHigher();
					}
				}
				givePvpToken(target, synergy);
				giveRarePvpToken(target, synergy);
				givePvPChest(target, synergy);
				if (target.getClan() != null && getClan() != null && !isInHuntersVillage() && !isInFunEvent())
				{
					giveClanEssence(target);
				}
				if (!synergy)
					target.clearFameStreak();
				// if (!isInGludin() && !isInHuntersVillage() && !isInFunEvent())
				// auditPvP(this, target, fameIncrease);
				L2PcInstance mate = null;
				final L2Party party = getParty();
				double chance = Config.SYNERGY_CHANCE_ON_PVP;
				int supports = 0;
				if (!synergy && party != null && !_inEventHG)
				{
					// System.out.println(">");
					List<L2PcInstance> ptsupports = new FastList<L2PcInstance>();
					for (L2PcInstance member : party.getPartyMembers())
					{
						int i = 0;
						i++;
						if (party.getMemberCount() >= i)
						{
							if (member == this || member == null)
								continue;
							if (!(member.isSupportClass() || member.isTankClass()))
								continue;
							if (member.getPvpFlag() < 1)
							{
								if (!(member.isInFunEvent() || member.isInSiege()))
									continue;
							}
							if (member.isInsideRadius(this, Config.SYNERGY_RADIUS, false, false))
							{
								ptsupports.add(member);
								// System.out.println("ptsupports added: " + member.getName());
							}
						}
						else
							break;
					}
					if (!ptsupports.isEmpty())
					{
						// System.out.println(">2");
						supports = ptsupports.size();
						if (supports == 2)
						{
							chance = chance * Config.SYNERGY_BOOST_2_SUPPORTS;
						}
						if (supports >= 3)
						{
							chance = chance * Config.SYNERGY_BOOST_3_SUPPORTS;
						}
						// System.out.println(">3");
						// sendMessage("Chance based on ("+String.valueOf(supports)+") Supports: " +String.valueOf(chance));
					}
					if (Rnd.get(100) < chance) // Start Synergy
					{
						// collects all the supports
						List<L2PcInstance> availableMembers = new FastList<L2PcInstance>();
						for (L2PcInstance mate1 : party.getPartyMembers())
						{
							int i = 0;
							i++;
							if (party.getMemberCount() >= i)
							{
								if (mate1 == this || mate1 == null)
									continue;
								if (!(mate1.isSupportClass() || mate1.isTankClass()))
									continue;
								if (mate1.getPvpFlag() < 1)
								{
									if (!(mate1.isInFunEvent() || mate1.isInSiege()))
										continue;
								}
								if (mate1.isDead())
									continue;
								
								if (mate1.isInsideRadius(this, Config.SYNERGY_RADIUS, false, false))
								{
									availableMembers.add(mate1);
									// System.out.println("availableMembers added: " + mate1.getName());
								}
							}
							else
								break;
						}
						if (!availableMembers.isEmpty())
						{
							mate = availableMembers.get(Rnd.get(availableMembers.size()));
						}
						if (mate != null)
						{
							if (isInSiege())
							{
								setSiegeKills(getSiegeKills() + 1);
							}
							if (isInFunEvent())
							{
								if (_inEventTvT && mate._inEventTvT)
								{
									mate._countTvTkills++;
									mate.sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ()));
									mate.broadcastPacket(new NicknameChanged(mate));
									broadcastPacket(new NicknameChanged(this));
									sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ()));
								}
								if (_inEventCTF && mate._inEventCTF)
								{
									mate._countCTFkills++;
									mate.sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ()));
									mate.broadcastPacket(new NicknameChanged(mate));
									broadcastPacket(new NicknameChanged(this));
									sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ()));
								}
								if (_inEventFOS && mate._inEventFOS)
								{
									mate._countFOSKills++;
									mate.sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ()));
									mate.broadcastPacket(new NicknameChanged(mate));
									broadcastPacket(new NicknameChanged(this));
									sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ()));
								}
								if (_inEventLunaDomi && mate._inEventLunaDomi)
								{
									mate._countLunaDomiKills++;
									mate.sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ()));
									mate.broadcastPacket(new NicknameChanged(mate));
									broadcastPacket(new NicknameChanged(this));
									sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ()));
								}
							}
							switch (Rnd.nextInt(10))
							{
								case 0:
								case 5:
								case 6:
								case 7:
								case 9:
									mate.sendMessage("Synergy kill from " + getName() + "!");
									break;
								case 1:
									mate.sendMessage("You helped " + getName() + " kill " + target.getDisplayName() + ", so the server pretends you got the kill too!");
									break;
								case 2:
									mate.sendMessage("Only on our server does a server take care of support classes like this."); // LOL
									break;
								case 3:
									mate.sendMessage("Healers, Tanks, and Doomcryers are considered support classes (Untransformed).");
									break;
								case 4:
									mate.sendMessage(getName() + " helped you help him!");
									break;
								case 8:
									mate.sendMessage("You support " + getName() + ", " + getName() + " supports you!");
									break;
							}
							sendMessage(mate.getName() + " assisted this PvP!");
							mate.sendMessage("Assisted " + getName() + " for killing " + target.getName() + ".");
							if (mate.getKarma() == 0)
							{
								// mate.givePvpExp(target, fameIncrease);
								// mate.increasePvpKills(target, false);
								// System.out.println(">4");
								mate.increasePvpKills(target, false, true, getName(), fameIncrease);
							}
						}
					}
				}
				if (getKarma() == 0)
					givePvpExp(target, fameIncrease);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
			{
				if (getCounters().pvpKills < getPvpKills())
				{
					getCounters().pvpKills = getPvpKills();
				}
			}
			if (_isInActiveKoreanRoom)
			{
				_koreanKills++;
				broadcastPacket(new NicknameChanged(this));
				sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ()));
			}
			if (_isInActiveDominationEvent)
			{
				_dominationKills++;
				broadcastPacket(new NicknameChanged(this));
				sendPacket(new PlaySound(0, "ItemSound.quest_itemget", 1, getObjectId(), getX(), getY(), getZ()));
			}
			// getMuseumPlayer().addData("pvp_victories", 1);
			// ((L2PcInstance) target).getMuseumPlayer().addData("pvp_defeats", 1);
			// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
			sendPacket(new UserInfo(this));
			sendPacket(new ExBrExtraUserInfo(this));
			_prevKill2 = _prevKill;
			_prev2IP = _prev1IP;
			_prevKill = target.getObjectId();
			_prev1IP = target.getIP();
		}
	}
	
	public int get_streak()
	{
		return _streak;
	}
	
	public void set_streak(int _streak)
	{
		this._streak = _streak;
	}
	
	private static int analyzeGearDiff(L2PcInstance killer, L2PcInstance target)
	{
		int points = 0;
		if (killer.getLevel() - target.getLevel() > 5)
			points++;
		else if (killer.getLevel() - target.getLevel() < -5)
			points--;
		L2ItemInstance itam = killer.getActiveWeaponInstance();
		if (itam != null)
		{
			if (itam.getUniqueness() >= 4)
				points++;
			else if (itam.getUniqueness() < 3 && itam.getEnchantLevel() < 19)
				points--;
		}
		itam = target.getActiveWeaponInstance();
		if (itam != null)
		{
			if (itam.getUniqueness() >= 4)
				points--;
			else if (itam.getUniqueness() < 3 && itam.getEnchantLevel() < 19)
				points++;
		}
		itam = killer.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (itam != null)
		{
			if (itam.getUniqueness() >= 4)
				points++;
			else if (itam.getUniqueness() < 3)
				points--;
		}
		itam = target.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (itam != null)
		{
			if (itam.getUniqueness() >= 4)
				points--;
			else if (itam.getUniqueness() < 3)
				points++;
		}
		itam = killer.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
		if (itam != null)
		{
			if (itam.getUniqueness() >= 4)
				points++;
			/*
			 * else if (itam.getUniqueness() < 3)
			 * points--;
			 */
		}
		itam = target.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
		if (itam != null)
		{
			if (itam.getUniqueness() >= 4)
				points--;
			/*
			 * else if (itam.getUniqueness() < 3)
			 * points++;
			 */
		}
		itam = killer.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
		if (itam != null)
		{
			if (itam.getUniqueness() >= 4)
				points++;
			/*
			 * else if (itam.getUniqueness() < 3)
			 * points--;
			 */
		}
		itam = target.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
		if (itam != null)
		{
			if (itam.getUniqueness() >= 4)
				points--;
			/*
			 * else if (itam.getUniqueness() < 3)
			 * points++;
			 */
		}
		int countKiller = 0;
		int countTarget = 0;
		for (int i : ORDER)
		{
			itam = killer.getInventory().getPaperdollItem(i);
			if (itam != null)
			{
				if (itam.getUniqueness() >= 4)
					countKiller++;
				/*
				 * else if (itam.getUniqueness() < 3)
				 * points--;
				 */
			}
			itam = target.getInventory().getPaperdollItem(i);
			if (itam != null)
			{
				if (itam.getUniqueness() >= 4)
					countTarget++;
				/*
				 * else if (itam.getUniqueness() < 3)
				 * points++;
				 */
			}
		}
		points += countKiller / 2;
		points -= countTarget / 2;
		return points;
	}
	
	private static int[] ORDER =
	{
		1, 2, 4, 5, 6
	};
	
	private void givePvpToken(L2PcInstance target, boolean synergy)
	{
		if (target == null)
			return;
		int chance = Config.PVP_TOKEN_CHANCE;
		int itemId = 0;
		if (isInFunEvent() || isInHuntersVillage() || isInOrcVillage() || isInGludin() || isInPI() || isInsideZone(L2Character.ZONE_FARM) || isInSiege() || isInFT() || isInsideZone(L2Character.ZONE_SIEGE))
		{
			chance = Config.PVP_TOKEN_CHANCE_HOT_ZONES;
			if (isInFunEvent())
			{
				chance = Config.PVP_TOKEN_CHANCE_EVENTS;
				if (Rnd.get(100) <= chance)
				{
					itemId = 99004;
					addItem("PVP token", itemId, 1, target, true);
					if (isSupportClassForSoloInstance() && synergy)
					{
						addItem("PVP token", itemId, 1, target, true);
					}
				}
			}
			else
			{
				if (Rnd.get(100) <= chance)
				{
					itemId = 99004;
					addItem("PVP token", itemId, 1, target, true);
				}
			}
		}
		else
		{
			if (Rnd.get(100) <= chance)
			{
				itemId = 99004;
				addItem("PVP token", itemId, 1, target, true);
				GmListTable.broadcastMessageToAdvancedGMs2(getName() + " earned a pvp token outside the usual zones.!!");
			}
		}
	}
	
	private void giveRarePvpToken(L2PcInstance target, boolean synergy)
	{
		if (target == null)
			return;
		int chance = Config.RARE_PVP_TOKEN_CHANCE;
		int rnd = 1000;
		int itemId = 99005;
		L2ItemInstance item = getInventory().getItemByItemId(itemId);
		if (item == null)
		{
			rnd = 1100;
		}
		else if (item.getCount() > 3)
		{
			rnd = 1100;
		}
		else if (item.getCount() > 5)
		{
			rnd = 1200;
		}
		else if (item.getCount() > 7)
		{
			rnd = 1500;
		}
		else if (isSupportClassForSoloInstance() && synergy)
		{
			rnd /= 2;
		}
		if (isInFunEvent() || isInHuntersVillage() || isInOrcVillage() || isInGludin() || isInPI() || isInsideZone(L2Character.ZONE_FARM) || isInSiege() || isInFT() || isInsideZone(L2Character.ZONE_SIEGE))
		{
			chance = Config.RARE_PVP_TOKEN_CHANCE_HOT_ZONES;
			if (isInFunEvent())
			{
				chance = Config.RARE_PVP_TOKEN_CHANCE_EVENTS;
				if (Rnd.get(rnd) <= chance)
				{
					addItem("Rare PVP token", itemId, 1, target, true);
				}
			}
			else
			{
				if (Rnd.get(rnd) <= chance)
				{
					addItem("Rare PVP token", itemId, 1, target, true);
				}
			}
		}
		else
		{
			if (Rnd.get(4000) <= chance)
			{
				addItem("Rare PVP token", itemId, 1, target, true);
				GmListTable.broadcastMessageToAdvancedGMs2(getName() + " earned a rare pvp token outside the usual zones.!!");
			}
		}
	}
	
	private void givePvPChest(L2PcInstance target, boolean synergy)
	{
		if (target == null)
			return;
		int itemIdTier1 = 800000;
		int itemIdTier2 = 800001;
		int itemIdTier3 = 800002;
		int pool = Config.PVP_CHEST_POOL_SIZE;
		int desiredId = 0;
		boolean found = false;
		if (isInFunEvent() || isInHuntersVillage() || isInOrcVillage() || isInGludin() || isInPI() || isInsideZone(L2Character.ZONE_FARM) || isInSiege() || isInFT() || isInsideZone(L2Character.ZONE_SIEGE))
		{
			if (isSupportClassForSoloInstance() && synergy)
			{
				pool /= 2;
			}
			int roll = Rnd.get(pool);
			if (!found && roll < 1)
			{
				desiredId = itemIdTier3;
				found = true;
			}
			if (!found && roll < 3)
			{
				desiredId = itemIdTier2;
				found = true;
			}
			if (!found && roll < 10)
			{
				desiredId = itemIdTier1;
				found = true;
			}
			// sendMessage(" > " + roll);
		}
		if (found)
		{
			addItem("PvP Chest", desiredId, 1, target, true);
		}
	}
	
	private void giveClanEssence(L2PcInstance target)
	{
		if (target == null)
			return;
		int chance = Config.CLAN_ESSENCE_CHANCE;
		int itemId = 0;
		if (isInFunEvent() || isInHuntersVillage() || isInOrcVillage() || isInGludin() || isInPI() || isInsideZone(L2Character.ZONE_FARM) || isInSiege() || isInFT() || isInsideZone(L2Character.ZONE_SIEGE))
		{
			chance = Config.CLAN_ESSENCE_CHANCE_HOT_ZONES;
			if (isInsideZone(L2Character.ZONE_SIEGE))
			{
				chance = Config.CLAN_ESSENCE_CHANCE_SIEGES;
				if (Rnd.get(100) <= chance)
				{
					itemId = 201000;
				}
				addItem("Clan Essence", itemId, 1, target, true);
			}
			else
			{
				if (Rnd.get(100) <= chance)
				{
					itemId = 201000;
				}
				addItem("Clan Essence", itemId, 1, target, true);
			}
		}
		else
			return;
	}
	
	final private void seeIfAnAlreadyEnchantedSkillWillBeEnchantedHigher()
	{
		final FastList<L2Skill> allSkillsThatAreEnchanted = new FastList<L2Skill>();
		for (L2Skill skill : getAllSkills())
		{
			if (skill != null && skill.getLevel() > 100 && skill.getLevel() % 100 < 15)
				allSkillsThatAreEnchanted.add(skill);
		}
		if (allSkillsThatAreEnchanted.size() > 0)
		{
			final int number = Rnd.nextInt(allSkillsThatAreEnchanted.size());
			final L2Skill skill = allSkillsThatAreEnchanted.get(number);
			if (skill == null)
				return;
			final int skillLvl = skill.getLevel() % 100;
			assert (skillLvl < 100);
			int chance = 0;
			switch (skillLvl)
			{
				case 1:
					chance = 100;
					break;
				case 2:
					chance = 88;
					break;
				case 3:
					chance = 78;
					break;
				case 4:
					chance = 67;
					break;
				case 5:
					chance = 56;
					break;
				case 6:
					chance = 44;
					break;
				case 7:
					chance = 33;
					break;
				case 8:
					chance = 20;
					break;
				case 9:
					chance = 9;
					break;
				case 10:
					chance = 8;
					break;
				case 11:
					chance = 3;
					break;
				case 12:
					chance = 2;
					break;
				case 13:
					chance = 1;
					break;
				case 14:
					chance = 1;
					break;
				/*
				 * case 15:
				 * chance = 1;
				 * break;
				 * case 16:
				 * chance = 0;
				 * break;
				 * case 17:
				 * chance = 0;
				 * break;
				 */
				default:
					return;
			}
			if (Rnd.nextInt(100) < chance)
			{
				final L2Skill enchantedSkill = SkillTable.getInstance().getInfo(skill.getId(), skill.getLevel() + 1);
				if (enchantedSkill != null)
				{
					addSkill(enchantedSkill, true);
					switch (Rnd.nextInt(15) + 1)
					{
						case 1:
							sendMessage("Due to valor in combat, your " + enchantedSkill.getName() + " has increased in level.");
							break;
						case 2:
							sendMessage(enchantedSkill.getName() + " I CHOOOSE YOU!");
							break;
						case 3:
							sendMessage("You couldn't get " + enchantedSkill + " right for the previous couple of days, today, after a good night's sleep, you suddenly realize what you've been doing wrong.");
							break;
						case 4:
							sendMessage("You have improved your " + enchantedSkill.getName());
							break;
						case 5:
							if (getAppearance().getSex())
								sendMessage("You are pretty cool gal. You go out and doesn't afraid of anything.");
							else
								sendMessage("You are pretty cool guy. You go out and doesn't afraid of anything.");
							break;
						case 6:
							sendMessage("BOM CHICKA WAH WAH");
							break;
						case 7:
							sendMessage("YOUR POWERLEVEL IS OVER NINE THOUSAAAAAAAAAAANNNNNNNNNNNNNNNNNNNNNNND!!");
							break;
						case 8:
							sendMessage("Chuck Norris' " + enchantedSkill.getName() + " is only +" + (enchantedSkill.getLevel() - 1));
							break;
						case 9:
							sendMessage("Use " + enchantedSkill.getName() + " well, the force is with you.");
							break;
						case 10:
							sendMessage("Thanks for playing on our server, here's a skill enchant.");
							break;
						case 11:
							sendMessage(enchantedSkill.getName() + " got enchanted wooo hoo!");
							break;
						case 12:
							sendMessage("You've gained a +1 enchant to " + enchantedSkill.getName() + " for participating in PvP.");
							break;
						case 13:
							sendMessage("ding! +1 to " + enchantedSkill.getName() + " GG GJ GF GL");
							break;
						case 14:
							sendMessage("In Soviet Russia skill enchants YOU.");
							break;
						case 15:
							sendMessage("You became more proficient with " + enchantedSkill.getName() + " after use.");
							break;
					}
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_ENCHANTING_THE_SKILL_S1);
					sm.addSkillName(enchantedSkill.getId());
					sendPacket(sm);
					sendSkillList();
					updateSkillShortcuts(enchantedSkill);
				}
			}
		}
	}
	
	private void updateSkillShortcuts(final L2Skill skill)
	{
		// update all the shortcuts to this skill
		L2ShortCut[] allShortCuts = getAllShortCuts();
		for (L2ShortCut sc : allShortCuts)
		{
			if (sc.getId() == skill.getId() && sc.getType() == L2ShortCut.TYPE_SKILL)
			{
				L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), skill.getLevel(), 1);
				sendPacket(new ShortCutRegister(newsc));
				registerShortCut(newsc);
			}
		}
	}
	
	public boolean isSupportClass()
	{
		if (isTransformed() || isInStance())
			return false;
		if (isTankClass() || isHealerClass())
			return true;
		if (getClassId().getId() == 0x88 || getClassId().getId() == 0x74 || getClassId().getId() == 0x73) // judicator and warcryer and ol
			return true;
		return false;
	}
	
	public boolean isSupportClassForInstance()
	{
		if (isTankClass() || isHealerClass())
			return true;
		if (getClassId().getId() == 0x88 || getClassId().getId() == 0x74 || getClassId().getId() == 0x73) // judicator and warcryer and ol
			return true;
		return false;
	}
	
	public boolean isSupportClassForSoloInstance()
	{
		if (isTankClass() || isHealerClass())
			return true;
		if (isDomi()) // Dominator
			return true;
		return false;
	}
	
	public boolean isWarCryer()
	{
		if (getClassId().getId() == 0x74) // warcryer
			return true;
		return false;
	}
	
	public boolean isDomi()
	{
		if (getClassId().getId() == 0x73) // domi
			return true;
		return false;
	}
	
	public boolean isDaggerClass()
	{
		switch (getClassId().getId())
		{
			case 0x08:
			case 0x17:
			case 0x24:
			case 0x5d:
			case 0x65:
			case 0x6c:
				return true;
		}
		return false;
	}
	
	public boolean isKamaelClass()
	{
		switch (getClassId().getId())
		{
			case 0x88:
			case 0x87:
			case 0x86:
			case 0x85:
			case 0x84:
			case 0x83:
			case 0x82:
			case 0x81:
			case 0x80:
			case 0x7F:
			case 0x7E:
			case 0x7D:
			case 0x7C:
			case 0x7B:
				return true;
		}
		return false;
	}
	
	public boolean isKamaelBaseClassExceptDoombringer()
	{
		switch (getBaseClassId())
		{
			case 0x88:
			case 0x87:
			case 0x86:
			case 0x85:
			case 0x84:
			case 0x82:
			case 0x81:
			case 0x80:
			case 0x7E:
			case 0x7C:
			case 0x7B:
				return true;
		}
		return false;
	}
	
	public boolean isArcherClass()
	{
		switch (getClassId().getId())
		{
			case 0x09:
			case 0x18:
			case 0x25:
			case 0x5c:
			case 0x66:
			case 0x6d:
			case 0x86:
			case 0x82:
				return true;
		}
		return false;
	}
	
	public boolean isTankClass()
	{
		switch (getClassId().getId())
		{
			case 0x05:
			case 0x06:
			case 0x13:
			case 0x14:
			case 0x5a:
			case 0x5b:
			case 0x63:
			case 0x6a:
				return true;
		}
		return false;
	}
	
	public boolean isNecroClass()
	{
		switch (getClassId().getId())
		{
			case 0x0d:
			case 0x5f:
				return true;
		}
		return false;
	}
	
	public boolean isSurferLee()
	{
		switch (getClassId().getId())
		{
			case 0x61: // Cardinal
			case 0x69: // Eva Saint
			case 0x70: // Shillien Saint
			case 0x73: // Dominator
			case 0x5a: // Phoenix Knight
			case 0x5b: // Hell Knight
			case 0x63: // Eva's Templar
			case 0x6a: // Shillien Templar
				return true;
		}
		return false;
	}
	
	public boolean isHealerClass()
	{
		switch (getClassId().getId())
		{
			case 0x61:
			case 0x69:
			case 0x70:
			case 0x10:
			case 0x1e:
			case 0x2b:
				return true;
		}
		return false;
	}
	
	public boolean isBishop()
	{
		switch (getClassId().getId())
		{
			case 0x0f:
			case 0x10:
			case 0x61:
				return true;
		}
		return false;
	}
	
	public boolean isElvenElder()
	{
		switch (getClassId().getId())
		{
			case 0x1d:
			case 0x1e:
			case 0x69:
				return true;
		}
		return false;
	}
	
	public boolean isShillienElder()
	{
		switch (getClassId().getId())
		{
			case 0x2a:
			case 0x2b:
			case 0x70:
				return true;
		}
		return false;
	}
	
	public boolean isBDSWSClass()
	{
		switch (getClassId().getId())
		{
			case 0x15:
			case 0x22:
			case 0x64:
			case 0x6b:
				return true;
		}
		return false;
	}
	
	public boolean isBDClass()
	{
		switch (getClassId().getId())
		{
			case 0x22:
			case 0x6b:
				return true;
		}
		return false;
	}
	
	public boolean isSWS()
	{
		switch (getClassId().getId())
		{
			case 0x64:
				return true;
		}
		return false;
	}
	
	public boolean isGladyTyrantClass()
	{
		switch (getClassId().getId())
		{
			case 0x02:
			case 0x58:
			case 0x72:
			case 0x30:
				return true;
		}
		return false;
	}
	
	public boolean isTyrantClass()
	{
		switch (getClassId().getId())
		{
			case 0x72:
			case 0x30:
				return true;
		}
		return false;
	}
	
	public boolean isSE()
	{
		switch (getClassId().getId())
		{
			case 0x70:
				return true;
		}
		return false;
	}
	
	public boolean isSummoner()
	{
		switch (getClassId().getId())
		{
			case 0x60:
			case 0x68:
			case 0x6f:
				return true;
		}
		return false;
	}
	
	final private void clearFameStreak()
	{
		_famestreak = 0;
		if (_famestreak > 0)
			_famestreak = 0;
	}
	
	final private int getFameStreak()
	{
		return _famestreak;
	}
	
	final private static void auditPvP(final L2PcInstance killer, final L2PcInstance target, final int fameGiven)
	{
		final String killer_name = killer.getAccountName() + " - " + killer.getName();
		final String target_name = target.getAccountName() + " - " + target.getName();
		String killer_clan = "";
		if (killer.getClan() != null)
			killer_clan = killer.getClan().getName();
		final String killer_IP = killer.getIP();
		final String target_IP = target.getIP();
		String target_clan = "";
		if (target.getClan() != null)
			target_clan = target.getClan().getName();
		String today = GMAudit._formatter.format(new Date());
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO audit_pvp(killer, killer_IP, killer_clan, target, target_IP, target_clan, famerewarded, date) VALUES(?,?,?,?,?,?,?,?)");
			statement.setString(1, killer_name);
			statement.setString(2, killer_IP);
			statement.setString(3, killer_clan);
			statement.setString(4, target_name);
			statement.setString(5, target_IP);
			statement.setString(6, target_clan);
			statement.setInt(7, fameGiven);
			statement.setString(8, today);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.fine("could not audit PVP action: " + killer.getName() + " " + e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void increasePkKillsAndKarma(int targLVL)
	{
		increasePkKillsAndKarma(targLVL, true);
	}
	
	/**
	 * Increase pk count, karma and send the info to the player
	 *
	 * @param targLVL
	 *            : level of the killed player
	 */
	public void increasePkKillsAndKarma(int targLVL, boolean increasePKs)
	{
		if (isInFunEvent() || isInsideZone(L2Character.ZONE_CHAOTIC) || isInPI() || getInstanceId() != 0)
			return;
		int baseKarma = Config.KARMA_MIN_KARMA;
		int newKarma = baseKarma;
		int karmaLimit = Config.KARMA_MAX_KARMA;
		int pkLVL = getLevel();
		int pkPKCount = getPkKills();
		int lvlDiffMulti = 0;
		double pkCountMulti = 0;
		// Check if the attacker has a PK counter greater than 0
		if (pkPKCount > 0)
			pkCountMulti = (pkPKCount + 2) / 2;
		else
			pkCountMulti = 1;
		if (pkCountMulti < 1)
			pkCountMulti = 1;
		// Calculate the level difference Multiplier between attacker and killed L2PcInstance
		if (pkLVL > targLVL)
			lvlDiffMulti = pkLVL / targLVL;
		else
			lvlDiffMulti = 1;
		if (lvlDiffMulti < 1)
			lvlDiffMulti = 1;
		// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
		newKarma *= pkCountMulti;
		newKarma *= lvlDiffMulti;
		// Make sure newKarma is less than karmaLimit and higher than baseKarma
		if (newKarma < baseKarma)
			newKarma = baseKarma;
		else if (newKarma > karmaLimit)
			newKarma = karmaLimit;
		/*
		 * if (isNearARB())
		 * {
		 * newKarma *= 15;
		 * newKarma += 21000;
		 * if (getLevel() < 87 && getLevel() > 40)
		 * {
		 * long pXp = getExp();
		 * long tXp = Experience.LEVEL[1];
		 * if (pXp > tXp)
		 * {
		 * removeExpAndSp(pXp - tXp, 0);
		 * sendMessage("You have been deleveled to level 1 for PKing when your level is below 87 and near a raidboss");
		 * }
		 * }
		 * }
		 */
		boolean punish = isAccountLockedDown();
		if (!punish)
		{
			L2ItemInstance wep = getActingPlayer().getActiveWeaponInstance();
			if (wep != null && wep.isTimeLimitedItem())
			{
				punish = true;
			}
			else
			{
				wep = getActingPlayer().getSecondaryWeaponInstance();
				if (wep != null && wep.isTimeLimitedItem())
				{
					punish = true;
				}
				else
				{
					L2ItemInstance armor = getActingPlayer().getChestArmorInstance();
					if (armor != null && armor.isTimeLimitedItem())
					{
						punish = true;
					}
				}
			}
			if (punish)
			{
				newKarma *= 3;
				setPunishLevel(L2PcInstance.PunishLevel.JAIL, 15);
			}
		}
		else
		{
			if (!isInJail())
				setPunishLevel(L2PcInstance.PunishLevel.JAIL, (int) Math.max(300, (getLockdownTime() - System.currentTimeMillis() / 1000) / 60));
		}
		/*
		 * if (isInsideZone(L2Character.ZONE_CHAOTIC))
		 * newKarma /= 1.4;
		 */
		// Fix to prevent overflow (=> karma has a max value of 2 147 483 647)
		if (getKarma() > (Integer.MAX_VALUE - newKarma))
			newKarma = Integer.MAX_VALUE - getKarma();
		if (increasePKs || getPkKills() < 5)
			setPkKills(getPkKills() + 1);
		// getMuseumPlayer().addData("pk_victories", 1);
		// ((L2PcInstance) getTarget()).getMuseumPlayer().addData("pk_defeats", 1);
		final int oldKarma = getKarma();
		setKarma(getKarma() + newKarma);
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			if (oldKarma > 0)
			{
				getCounters().pkInARowKills++;
			}
			else
			{
				getCounters().pkInARowKills = 1;
			}
			if (getCounters().highestKarma < getKarma())
			{
				getCounters().highestKarma = getKarma();
			}
		}
		// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
	}
	
	public int calculateKarmaLost(long exp)
	{
		// KARMA LOSS
		// When a PKer gets killed by another player or a L2MonsterInstance, it loses a certain amount of Karma based on their level.
		// this (with defaults) results in a level 1 losing about ~2 karma per death, and a lvl 70 loses about 11760 karma per death...
		// You lose karma as long as you were not in a pvp zone and you did not kill urself.
		// NOTE: exp for death (if delevel is allowed) is based on the players level
		long expGained = Math.abs(exp);
		expGained /= Config.KARMA_XP_DIVIDER;
		// FIXME Micht : Maybe this code should be fixed and karma set to a long value
		int karmaLost = 0;
		if (expGained > Integer.MAX_VALUE)
			karmaLost = Integer.MAX_VALUE;
		else
			karmaLost = (int) expGained;
		if (karmaLost < Config.KARMA_LOST_BASE)
			karmaLost = Config.KARMA_LOST_BASE;
		if (karmaLost > getKarma())
			karmaLost = getKarma();
		return karmaLost;
	}
	
	public void updatePvPStatus()
	{
		if (isInsideZone(ZONE_PVP))
			return;
		if (isInFunEvent())
			return;
		if (isInDuel())
			return;
		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
		if (getPvpFlag() == 0)
			startPvPFlag();
	}
	
	public void updatePvPStatus(L2Character target)
	{
		if (isInFunEvent())
			return;
		if (target instanceof L2Npc)
		{
			if (target.isAPC())
			{
				FakePc fpc = FakePcsTable.getInstance().getFakePc(((L2Npc) target).getNpcId());
				if (fpc != null && fpc.pvpFlag > 0)
				{
					if (isInDuel())
						return;
					if (!isInsideZone(ZONE_PVP))
					{
						setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_PVP_TIME);
						if (getPvpFlag() == 0)
							startPvPFlag();
					}
				}
			}
		}
		else
		{
			L2PcInstance player_target = target.getActingPlayer();
			if (player_target == null)
				return;
			if ((isInDuel() && player_target.getDuelId() == getDuelId()))
				return;
			if ((!isInsideZone(ZONE_PVP) || !player_target.isInsideZone(ZONE_PVP)) && player_target.getKarma() == 0)
			{
				if (checkIfPvP(player_target))
					setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_PVP_TIME);
				else
					setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
				if (getPvpFlag() == 0)
					startPvPFlag();
			}
		}
	}
	
	/**
	 * Restore the specified % of experience this L2PcInstance has
	 * lost and sends a Server->Client StatusUpdate packet.<BR>
	 * <BR>
	 */
	public void restoreExp(double restorePercent)
	{
		if (getExpBeforeDeath() > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp((int) Math.round((getExpBeforeDeath() - getExp()) * restorePercent / 100));
			setExpBeforeDeath(0);
		}
	}
	
	/**
	 * Reduce the Experience (and level if necessary) of the L2PcInstance in function of the calculated Death Penalty.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate the Experience loss</li>
	 * <li>Set the value of _expBeforeDeath</li>
	 * <li>Set the new Experience value of the L2PcInstance and Decrease its level if necessary</li>
	 * <li>Send a Server->Client StatusUpdate packet with its new Experience</li><BR>
	 * <BR>
	 */
	public void deathPenalty(boolean atwar, boolean killed_by_pc, boolean killed_by_siege_npc)
	{
		// TODO Need Correct Penalty
		// Get the level of the L2PcInstance
		final int lvl = getLevel();
		byte level = (byte) getLevel();
		int clan_luck = getSkillLevel(L2Skill.SKILL_CLAN_LUCK);
		double clan_luck_modificator = 1.0;
		if (!killed_by_pc)
		{
			switch (clan_luck)
			{
				case 3:
					clan_luck_modificator = 0.8;
					break;
				case 2:
					clan_luck_modificator = 0.8;
					break;
				case 1:
					clan_luck_modificator = 0.88;
					break;
				default:
					clan_luck_modificator = 1.0;
					break;
			}
		}
		else
		{
			switch (clan_luck)
			{
				case 3:
					clan_luck_modificator = 0.5;
					break;
				case 2:
					clan_luck_modificator = 0.5;
					break;
				case 1:
					clan_luck_modificator = 0.5;
					break;
				default:
					clan_luck_modificator = 1.0;
					break;
			}
		}
		// The death steal you some Exp
		double percentLost = Config.PLAYER_XP_PERCENT_LOST[getLevel()] * clan_luck_modificator;
		switch (level)
		{
			case 78:
				percentLost = (1.5 * clan_luck_modificator);
				break;
			case 77:
				percentLost = (2.0 * clan_luck_modificator);
				break;
			case 76:
				percentLost = (2.5 * clan_luck_modificator);
				break;
			default:
				if (level < 40)
					percentLost = (7.0 * clan_luck_modificator);
				else if (level >= 40 && level <= 75)
					percentLost = (4.0 * clan_luck_modificator);
				break;
		}
		if (getKarma() > 0)
			percentLost *= Config.RATE_KARMA_EXP_LOST;
		if (isFestivalParticipant() || atwar)
			percentLost /= 4.0;
		// Calculate the Experience loss
		long lostExp = 0;
		if (!atEvent)
			if (lvl < Experience.MAX_LEVEL)
				lostExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);
			else
				lostExp = Math.round((getStat().getExpForLevel(Experience.MAX_LEVEL) - getStat().getExpForLevel(Experience.MAX_LEVEL - 1)) * percentLost / 100);
		// Get the Experience before applying penalty
		setExpBeforeDeath(getExp());
		// No xp loss inside pvp zone unless
		// - it's a siege zone and you're NOT participating
		// - you're killed by a non-pc whose not belong to the siege
		if (isInsideZone(ZONE_PVP))
		{
			// No xp loss for siege participants inside siege zone
			if (isInsideZone(ZONE_SIEGE))
			{
				if (isInSiege() && (killed_by_pc || killed_by_siege_npc))
					lostExp = 0;
			}
			else if (killed_by_pc)
				lostExp = 0;
		}
		if (Config.DEBUG)
			_log.fine(getName() + " died and lost " + lostExp + " experience.");
		// Set the new Experience value of the L2PcInstance
		/* getStat().removeExp(lostExp); */
		getStat().addExp(-lostExp);
	}
	
	/**
	 * @param b
	 */
	public void setPartyMatchingAutomaticRegistration(boolean b)
	{
		_partyMatchingAutomaticRegistration = b;
	}
	
	/**
	 * @param b
	 */
	public void setPartyMatchingShowLevel(boolean b)
	{
		_partyMatchingShowLevel = b;
	}
	
	/**
	 * @param b
	 */
	public void setPartyMatchingShowClass(boolean b)
	{
		_partyMatchingShowClass = b;
	}
	
	/**
	 * @param memo
	 */
	public void setPartyMatchingMemo(String memo)
	{
		_partyMatchingMemo = memo;
	}
	
	public boolean isPartyMatchingAutomaticRegistration()
	{
		return _partyMatchingAutomaticRegistration;
	}
	
	public String getPartyMatchingMemo()
	{
		return _partyMatchingMemo;
	}
	
	public boolean isPartyMatchingShowClass()
	{
		return _partyMatchingShowClass;
	}
	
	public boolean isPartyMatchingShowLevel()
	{
		return _partyMatchingShowLevel;
	}
	
	/**
	 * Manage the increase level task of a L2PcInstance (Max MP, Max MP, Recommandation, Expertise and beginner skills...).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client System Message to the L2PcInstance : YOU_INCREASED_YOUR_LEVEL</li>
	 * <li>Send a Server->Client packet StatusUpdate to the L2PcInstance with new LEVEL, MAX_HP and MAX_MP</li>
	 * <li>Set the current HP and MP of the L2PcInstance, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)</li>
	 * <li>Recalculate the party level</li>
	 * <li>Recalculate the number of Recommandation that the L2PcInstance can give</li>
	 * <li>Give Expertise skill of this level and remove beginner Lucky skill</li><BR>
	 * <BR>
	 */
	public void increaseLevel()
	{
		// Set the current HP and MP of the L2Character, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)
		setCurrentHpMp(getMaxHp(), getMaxMp());
		setCurrentCp(getMaxCp());
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the RegenActive flag to False</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li><BR>
	 * <BR>
	 */
	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopWaterTask();
		stopFeed();
		clearPetData();
		storePetFood(_mountNpcId);
		stopRentPet();
		stopPvpRegTask();
		stopPunishTask(true);
		/* stopChargeTask(); */
		stopFameTask();
		stopVitalityTask();
		stopTempHeroTask();
		/* stopPartyPassiveTask(true); */
	}
	
	/**
	 * Return the L2Summon of the L2PcInstance or null.<BR>
	 * <BR>
	 */
	@Override
	public L2Summon getPet()
	{
		return _summon;
	}
	
	/**
	 * Return the L2Decoy of the L2PcInstance or null.<BR>
	 * <BR>
	 */
	public L2Decoy getDecoy()
	{
		return _decoy;
	}
	/*     *//**
				 * Return the L2Trap of the L2PcInstance or null.<BR>
				 * <BR>
				 *//*
					 * public L2Trap getTrap()
					 * {
					 * return _trap;
					 * }
					 */
	
	/**
	 * Set the L2Summon of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void setPet(L2Summon summon)
	{
		_summon = summon;
		// update attack element value display
		if ((_summon == null || _summon instanceof L2SummonInstance) && getClassId().isSummoner() && getAttackElement() != Elementals.NONE)
			sendPacket(new UserInfo(this));
	}
	
	/**
	 * Set the L2Decoy of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void setDecoy(L2Decoy decoy)
	{
		_decoy = decoy;
	}
	
	/**
	 * Return the L2Summon of the L2PcInstance or null.<BR>
	 * <BR>
	 */
	public L2TamedBeastInstance getTrainedBeast()
	{
		return _tamedBeast;
	}
	
	/**
	 * Set the L2Summon of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void setTrainedBeast(L2TamedBeastInstance tamedBeast)
	{
		_tamedBeast = tamedBeast;
	}
	
	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 */
	public L2Request getRequest()
	{
		return _request;
	}
	
	/**
	 * Set the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 */
	public synchronized void setActiveRequester(L2PcInstance requester)
	{
		_activeRequester = requester;
		if (requester == null)
			onTransactionResponse();
	}
	
	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 */
	public L2PcInstance getActiveRequester()
	{
		return _activeRequester;
	}
	
	/**
	 * Return True if a transaction is in progress.<BR>
	 * <BR>
	 */
	public boolean isProcessingRequest()
	{
		if (_activeRequester != null)
		{
			if (isRequestExpired())
				setActiveRequester(null);
		}
		return _activeRequester != null || _requestExpireTime > GameTimeController.getGameTicks();
	}
	
	/**
	 * Return True if a transaction is in progress.<BR>
	 * <BR>
	 */
	public boolean isProcessingTransaction()
	{
		return _activeTradeList != null;
	}
	
	/**
	 * Select the Warehouse to be used in next activity.<BR>
	 * <BR>
	 */
	public void onTransactionRequest(L2PcInstance partner)
	{
		_requestExpireTime = GameTimeController.getGameTicks() + REQUEST_TIMEOUT * GameTimeController.TICKS_PER_SECOND;
		partner._requestExpireTime = _requestExpireTime;
		partner.setActiveRequester(this);
	}
	
	/**
	 * Return true if last request is expired.
	 * 
	 * @return
	 */
	public boolean isRequestExpired()
	{
		return !(_requestExpireTime > GameTimeController.getGameTicks());
	}
	
	/**
	 * Select the Warehouse to be used in next activity.<BR>
	 * <BR>
	 */
	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}
	
	/**
	 * Select the Warehouse to be used in next activity.<BR>
	 * <BR>
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}
	
	/**
	 * Return active Warehouse.<BR>
	 * <BR>
	 */
	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}
	
	/**
	 * Select the TradeList to be used in next activity.<BR>
	 * <BR>
	 */
	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}
	
	/**
	 * Return active TradeList.<BR>
	 * <BR>
	 */
	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}
	
	public void onTradeStart(L2PcInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);
		SystemMessage msg = new SystemMessage(SystemMessageId.BEGIN_TRADE_WITH_C1);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(new TradeStart(this));
	}
	
	public void onTradeConfirm(L2PcInstance partner)
	{
		SystemMessage msg = new SystemMessage(SystemMessageId.C1_CONFIRMED_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(new TradeOtherDone());
	}
	
	public void onTradeCancel(L2PcInstance partner)
	{
		if (_activeTradeList == null)
			return;
		_activeTradeList.lock();
		_activeTradeList = null;
		sendPacket(new TradeDone(0));
		SystemMessage msg = new SystemMessage(SystemMessageId.C1_CANCELED_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
	}
	
	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new TradeDone(1));
		if (successfull)
			sendPacket(new SystemMessage(SystemMessageId.TRADE_SUCCESSFUL));
	}
	
	public void startTrade(L2PcInstance partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}
	
	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
			return;
		L2PcInstance partner = _activeTradeList.getPartner();
		if (partner != null)
			partner.onTradeCancel(this);
		onTradeCancel(this);
	}
	
	/**
	 * Return the _createList object of the L2PcInstance.<BR>
	 * <BR>
	 */
	public L2ManufactureList getCreateList()
	{
		return _createList;
	}
	
	/**
	 * Set the _createList object of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void setCreateList(L2ManufactureList x)
	{
		_createList = x;
	}
	
	/**
	 * Return the _buyList object of the L2PcInstance.<BR>
	 * <BR>
	 */
	public TradeList getSellList()
	{
		if (_sellList == null)
			_sellList = new TradeList(this);
		return _sellList;
	}
	
	/**
	 * Return the _buyList object of the L2PcInstance.<BR>
	 * <BR>
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null)
			_buyList = new TradeList(this);
		return _buyList;
	}
	
	/**
	 * Set the Private Store type of the L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 */
	public void setPrivateStoreType(int type)
	{
		_privatestore = type;
	}
	
	/**
	 * Return the Private Store type of the L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 */
	public int getPrivateStoreType()
	{
		return _privatestore;
	}
	
	/**
	 * Set the _skillLearningClassId object of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void setSkillLearningClassId(ClassId classId)
	{
		_skillLearningClassId = classId;
	}
	
	/**
	 * Return the _skillLearningClassId object of the L2PcInstance.<BR>
	 * <BR>
	 */
	public ClassId getSkillLearningClassId()
	{
		return _skillLearningClassId;
	}
	
	/**
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void setClan(L2Clan clan)
	{
		_clan = clan;
		/* setTitle(""); */
		if (clan == null)
		{
			_clanId = 0;
			_clanPrivileges = 0;
			_pledgeType = 0;
			_powerGrade = 0;
			_apprentice = 0;
			_sponsor = 0;
			return;
		}
		if (!clan.isMember(getObjectId()))
		{
			// char has been kicked from clan
			setClan(null);
			return;
		}
		_clanId = clan.getClanId();
	}
	
	/**
	 * Return the _clan object of the L2PcInstance.<BR>
	 * <BR>
	 */
	public L2Clan getClan()
	{
		return _clan;
	}
	
	public final String getClanName()
	{
		String _name = "";
		if (getClan() != null)
			_name = getClan().getName();
		return _name;
	}
	
	/**
	 * Return True if the L2PcInstance is the leader of its clan.<BR>
	 * <BR>
	 */
	public boolean isClanLeader()
	{
		if (getClan() == null)
		{
			return false;
		}
		else
		{
			return getObjectId() == getClan().getLeaderId();
		}
	}
	
	/**
	 * Reduce the number of arrows/bolts owned by the L2PcInstance and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consummed).<BR>
	 * <BR>
	 */
	@Override
	protected void reduceArrowCount(boolean bolts)
	{
		/*
		 * L2ItemInstance arrows = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		 * if (arrows == null)
		 * {
		 * getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
		 * if (bolts)
		 * _boltItem = null;
		 * else
		 * _arrowItem = null;
		 * sendPacket(new ItemList(this,false));
		 * return;
		 * }
		 * // Adjust item quantity
		 * if (arrows.getCount() > 1)
		 * {
		 * synchronized(arrows)
		 * {
		 * arrows.changeCountWithoutTrace(-1, this, null);
		 * arrows.setLastChange(L2ItemInstance.MODIFIED);
		 * // could do also without saving, but let's save approx 1 of 10
		 * if(GameTimeController.getGameTicks() % 10 == 0)
		 * arrows.updateDatabase();
		 * _inventory.refreshWeight();
		 * }
		 * }
		 * else
		 * {
		 * // Destroy entire item and save to database
		 * _inventory.destroyItem("Consume", arrows, this, null);
		 * getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
		 * if (bolts)
		 * _boltItem = null;
		 * else
		 * _arrowItem = null;
		 * if (Config.DEBUG) _log.fine("removed arrows count");
		 * sendPacket(new ItemList(this,false));
		 * return;
		 * }
		 * if (!Config.FORCE_INVENTORY_UPDATE)
		 * {
		 * InventoryUpdate iu = new InventoryUpdate();
		 * iu.addModifiedItem(arrows);
		 * sendPacket(iu);
		 * }
		 * else sendPacket(new ItemList(this, false));
		 */
	}
	
	/**
	 * Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True.<BR>
	 * <BR>
	 */
	@Override
	protected boolean checkAndEquipArrows()
	{
		/*
		 * // Check if nothing is equiped in left hand
		 * if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		 * {
		 * // Get the L2ItemInstance of the arrows needed for this bow
		 * _arrowItem = getInventory().findArrowForBow(getActiveWeaponItem());
		 * if (_arrowItem != null)
		 * {
		 * // Equip arrows needed in left hand
		 * getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);
		 * // Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
		 * ItemList il = new ItemList(this, false);
		 * sendPacket(il);
		 * }
		 * }
		 * else
		 * {
		 * // Get the L2ItemInstance of arrows equiped in left hand
		 * _arrowItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		 * }
		 * return _arrowItem != null;
		 */
		return true;
	}
	
	/**
	 * Equip bolts needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True.<BR>
	 * <BR>
	 */
	@Override
	protected boolean checkAndEquipBolts()
	{
		/*
		 * // Check if nothing is equiped in left hand
		 * if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		 * {
		 * // Get the L2ItemInstance of the arrows needed for this bow
		 * _boltItem = getInventory().findBoltForCrossBow(getActiveWeaponItem());
		 * if (_boltItem != null)
		 * {
		 * // Equip arrows needed in left hand
		 * getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _boltItem);
		 * // Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
		 * ItemList il = new ItemList(this, false);
		 * sendPacket(il);
		 * }
		 * }
		 * else
		 * {
		 * // Get the L2ItemInstance of arrows equiped in left hand
		 * _boltItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		 * }
		 * return _boltItem != null;
		 */
		return true;
	}
	
	public boolean disarmArmors(L2Character attacker, int count)
	{
		final Inventory inv = getInventory();
		L2ItemInstance toBeUnequipped = null;
		InventoryUpdate iu = new InventoryUpdate();
		int counter = 0;
		List<Integer> list = new FastList<Integer>();
		list.add(Inventory.PAPERDOLL_CHEST);
		list.add(Inventory.PAPERDOLL_LEGS);
		list.add(Inventory.PAPERDOLL_HEAD);
		list.add(Inventory.PAPERDOLL_GLOVES);
		list.add(Inventory.PAPERDOLL_FEET);
		list.add(Inventory.PAPERDOLL_BACK);
		list.add(Inventory.PAPERDOLL_HAIR);
		list.add(Inventory.PAPERDOLL_HAIR2);
		do
		{
			final int slot = list.get(Rnd.get(list.size()));
			toBeUnequipped = inv.getPaperdollItem(slot);
			list.remove(Integer.valueOf(slot));
			if (toBeUnequipped != null && !toBeUnequipped.isWear())
			{
				L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(inv.getSlotFromItem(toBeUnequipped));
				for (L2ItemInstance itm : unequiped)
					iu.addModifiedItem(itm);
				if (unequiped.length > 0)
				{
					attacker.sendMessage("Disarmed " + getDisplayName() + "'s " + unequiped[0].getName());
					SystemMessage sm = null;
					if (unequiped[0].getEnchantLevel() > 0)
					{
						sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
						sm.addNumber(unequiped[0].getEnchantLevel());
						sm.addItemName(unequiped[0]);
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.S1_DISARMED);
						sm.addItemName(unequiped[0]);
					}
					sendPacket(sm);
					counter++;
				}
			}
		}
		while (counter < count && list.size() > 0);
		if (counter > 0)
		{
			sendPacket(iu);
			broadcastUserInfo();
		}
		return true;
	}
	
	/**
	 * Disarm the player's weapon.<BR>
	 * <BR>
	 */
	public boolean disarmWeapons()
	{
		// Don't allow disarming a cursed weapon
		if (isCursedWeaponEquipped())
			return false;
		// Unequip the weapon
		L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
			wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
		if (wpn != null)
		{
			if (wpn.isWear())
				return false;
			L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance itm : unequiped)
				iu.addModifiedItem(itm);
			sendPacket(iu);
			abortAttack();
			broadcastUserInfo();
			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequiped.length > 0)
			{
				SystemMessage sm = null;
				if (unequiped[0].getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0]);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(unequiped[0]);
				}
				sendPacket(sm);
			}
		}
		return true;
	}
	
	/**
	 * Disarm the player's shield.<BR>
	 * <BR>
	 */
	public boolean disarmShield()
	{
		L2ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			if (sld.isWear())
				return false;
			L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance itm : unequiped)
				iu.addModifiedItem(itm);
			sendPacket(iu);
			abortAttack();
			broadcastUserInfo();
			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequiped.length > 0)
			{
				SystemMessage sm = null;
				if (unequiped[0].getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0]);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(unequiped[0]);
				}
				sendPacket(sm);
			}
		}
		return true;
	}
	
	public boolean mount(L2Summon pet)
	{
		if (!disarmWeapons())
			return false;
		if (!disarmShield())
			return false;
		if (isTransformed())
			return false;
		if (!isGM())
		{
			if (isInHuntersVillage() || isInOrcVillage())
				return false;
			if (isInHellbound())
				return false;
			if (isMoving() || !getAI().getIntention().equals(CtrlIntention.AI_INTENTION_IDLE))
			{
				sendMessage("Cannot mount unless you're standing");
				return false;
			}
		}
		for (L2Effect e : getAllEffects())
		{
			if (e != null && e.getSkill().isToggle())
				e.exit();
		}
		Ride mount = new Ride(this, true, pet.getTemplate().npcId);
		setMount(pet.getNpcId(), pet.getLevel(), mount.getMountType());
		setMountObjectID(pet.getControlItemId());
		clearPetData();
		startFeed(pet.getNpcId());
		broadcastPacket(mount);
		// Notify self and others about speed change
		broadcastUserInfo();
		pet.unSummon(this);
		return true;
	}
	
	public boolean mount(int npcId, int controlItemObjId, boolean useFood)
	{
		if (!disarmWeapons())
			return false;
		if (!disarmShield())
			return false;
		if (isTransformed())
			return false;
		if (!isGM())
		{
			if (isInHuntersVillage() || isInOrcVillage())
				return false;
			if (isInHellbound())
				return false;
			if (isMoving() || !getAI().getIntention().equals(CtrlIntention.AI_INTENTION_IDLE))
			{
				sendMessage("Cannot mount unless you're standing");
				return false;
			}
		}
		for (L2Effect e : getAllEffects())
		{
			if (e != null && e.getSkill().isToggle())
				e.exit();
		}
		Ride mount = new Ride(this, true, npcId);
		if (setMount(npcId, getLevel(), mount.getMountType()))
		{
			clearPetData();
			setMountObjectID(controlItemObjId);
			broadcastPacket(mount);
			// Notify self and others about speed change
			broadcastUserInfo();
			if (useFood)
				startFeed(npcId);
			return true;
		}
		return false;
	}
	
	public boolean mountPlayer(L2Summon pet)
	{
		if (pet != null && pet.isMountable() && !isMounted() && !isBetrayed())
		{
			if (isDead())
			{
				// A strider cannot be ridden when dead
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD));
				return false;
			}
			else if (pet.isDead())
			{
				// A dead strider cannot be ridden.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN));
				return false;
			}
			else if (pet.isInCombat() || pet.isRooted())
			{
				// A strider in battle cannot be ridden
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN));
				return false;
			}
			else if (isInCombat())
			{
				// A strider cannot be ridden while in battle
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE));
				return false;
			}
			else if (isSitting())
			{
				// A strider can be ridden only when standing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING));
				return false;
			}
			else if (isInFunEvent())
			{
				// A strider cannot be ridden while in events
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			else if (isFishing())
			{
				// You can't mount, dismount, break and drop items while fishing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_2));
				return false;
			}
			else if (isTransformed() || isCursedWeaponEquipped() || isInHuntersVillage() || isInOrcVillage() || isInHellbound())
			{
				// no message needed, player while transformed doesn't have mount action
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			else if (getInventory().getItemByItemId(9819) != null)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_A_STEED_WHILE_HOLDING_A_FLAG)); // TODO: confirm this message
				return false;
			}
			else if (pet.isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT));
				return false;
			}
			else if (!Util.checkIfInRange(200, this, pet, true))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.TOO_FAR_AWAY_FROM_FENRIR_TO_MOUNT));
				return false;
			}
			else if (isMoving() || !getAI().getIntention().equals(CtrlIntention.AI_INTENTION_IDLE))
			{
				sendMessage("Cannot dismount unless you're standing");
				return false;
			}
			else if (!pet.isDead() && !isMounted())
			{
				mount(pet);
			}
		}
		else if (isRentedPet())
		{
			stopRentPet();
		}
		else if (isMounted())
		{
			if (isMoving() || !getAI().getIntention().equals(CtrlIntention.AI_INTENTION_IDLE))
			{
				sendMessage("Cannot dismount unless you're standing");
				return false;
			}
			else if (getMountType() == 2 && isInsideZone(L2Character.ZONE_NOLANDING))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.NO_DISMOUNT_HERE));
				return false;
			}
			else if (isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT));
				return false;
			}
			else
				dismount();
		}
		return true;
	}
	
	public boolean dismount()
	{
		boolean wasFlying = isFlying();
		sendPacket(new SetupGauge(3, 0, 0));
		int petId = _mountNpcId;
		if (setMount(0, 0, 0))
		{
			stopFeed();
			clearPetData();
			if (wasFlying)
				removeSkill(SkillTable.getInstance().getInfo(4289, 1));
			Ride dismount = new Ride(this, false, 0);
			broadcastPacket(dismount);
			setMountObjectID(0);
			storePetFood(petId);
			// Notify self and others about speed change
			broadcastUserInfo();
			return true;
		}
		return false;
	}
	
	/**
	 * Return True if the L2PcInstance use a dual weapon.<BR>
	 * <BR>
	 */
	@Override
	public boolean isUsingDualWeapon()
	{
		final L2Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem == null)
			return false;
		if (weaponItem.getItemType() == L2WeaponType.DUAL)
			return true;
		else if (weaponItem.getItemType() == L2WeaponType.DUAL_DAGGER)
			return true;
		else if (weaponItem.getItemType() == L2WeaponType.DUALFIST)
			return true;
		else if (weaponItem.getItemId() == 248) // orc fighter fists
			return true;
		else if (weaponItem.getItemId() == 252) // orc mage fists
			return true;
		else
			return false;
	}
	
	public void setUptime(long time)
	{
		_uptime = time;
	}
	
	public long getUptime()
	{
		return System.currentTimeMillis() - _uptime;
	}
	
	/**
	 * Return True if the L2PcInstance is invulnerable.<BR>
	 * <BR>
	 */
	@Override
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting || isSpawnProtected();
	}
	
	public boolean isInvulGM()
	{
		return _isInvul;
	}
	
	/**
	 * Return True if the L2PcInstance has a Party in progress.<BR>
	 * <BR>
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}
	
	/**
	 * Set the _party object of the L2PcInstance (without joining it).<BR>
	 * <BR>
	 */
	public void setParty(L2Party party)
	{
		_party = party;
	}
	
	/**
	 * Set the _party object of the L2PcInstance AND join it.<BR>
	 * <BR>
	 */
	public void joinParty(L2Party party)
	{
		if (party != null)
		{
			// First set the party otherwise this wouldn't be considered
			// as in a party into the L2Character.updateEffectIcons() call.
			_party = party;
			party.addPartyMember(this);
		}
	}
	
	/**
	 * Manage the Leave Party task of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this);
			_party = null;
		}
	}
	
	/**
	 * Return the _party object of the L2PcInstance.<BR>
	 * <BR>
	 */
	@Override
	public L2Party getParty()
	{
		return _party;
	}
	
	/**
	 * Return True if the L2PcInstance is a GM.<BR>
	 * <BR>
	 */
	@Override
	public boolean isGM()
	{
		return getAccessLevel().isGm();
	}
	
	@Override
	public boolean isGMReally()
	{
		return getAccessLevel().isGm() || getTurnedGMOff() > 0;
	}
	
	/**
	 * Set the _accessLevel of the L2PcInstance.<BR>
	 * <BR>
	 */
	public void setAccessLevel(int level)
	{
		if (level == AccessLevels._masterAccessLevelNum)
		{
			/* _log.warning( "Master access level set for character " + getName() + "! Just a warning to be careful ;)" ); */
			_accessLevel = AccessLevels._masterAccessLevel;
		}
		else if (level == AccessLevels._userAccessLevelNum)
			_accessLevel = AccessLevels._userAccessLevel;
		else
		{
			final L2AccessLevel accessLevel = AccessLevels.getInstance().getAccessLevel(level);
			if (accessLevel == null)
			{
				if (level < 0)
				{
					AccessLevels.getInstance().addBanAccessLevel(level);
					_accessLevel = AccessLevels.getInstance().getAccessLevel(level);
				}
				else
				{
					_log.warning("Tryed to set unregistered access level " + level + " to character " + getName() + ". Setting access level without privileges!");
					_accessLevel = AccessLevels._userAccessLevel;
				}
			}
			else
				_accessLevel = accessLevel;
		}
	}
	
	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
	}
	
	/**
	 * Return the _accessLevel of the L2PcInstance.<BR>
	 * <BR>
	 */
	public L2AccessLevel getAccessLevel()
	{
		if (Config.EVERYBODY_HAS_ADMIN_RIGHTS)
			return AccessLevels._masterAccessLevel;
		else if (_accessLevel == null) /* This is here because inventory etc. is loaded before access level on login, so it is not null */
			setAccessLevel(AccessLevels._userAccessLevelNum);
		return _accessLevel;
	}
	
	@Override
	public double getLevelMod()
	{
		return (100.0 - 11 + getLevel(true)) / 100.0;
	}
	
	/**
	 * Update Stats of the L2PcInstance client side by sending Server->Client packet UserInfo/StatusUpdate to this L2PcInstance and CharInfo/StatusUpdate to all L2PcInstance in its _KnownPlayers (broadcast).<BR>
	 * <BR>
	 */
	public void updateAndBroadcastStatus(int broadcastType)
	{
		// Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers (broadcast)
		if (broadcastType == 1)
		{
			sendPacket(new UserInfo(this));
			sendPacket(new ExBrExtraUserInfo(this));
		}
		else if (broadcastType == 2)
			broadcastUserInfo();
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR>
	 * <BR>
	 */
	public void setKarmaFlag(int flag)
	{
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		// synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
				if (getPet() != null)
					player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
			}
		}
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR>
	 * <BR>
	 */
	public void broadcastKarma()
	{
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.KARMA, getKarma());
		sendPacket(su);
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		// synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
				if (getPet() != null)
					player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
			}
		}
	}
	
	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).<BR>
	 * <BR>
	 */
	public void setOnlineStatus(boolean isOnline)
	{
		if (_isOnline != isOnline)
			_isOnline = isOnline;
		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		updateOnlineStatus();
	}
	
	public void setIsIn7sDungeon(boolean isIn7sDungeon)
	{
		_isIn7sDungeon = isIn7sDungeon;
	}
	
	/**
	 * Update the characters table of the database with online status and lastAccess of this L2PcInstance (called when login and logout).<BR>
	 * <BR>
	 */
	public void updateOnlineStatus()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE charId=?");
			statement.setInt(1, isOnline());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed updating character online status.", e);
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
	
	/**
	 * Create a new player in the characters table of the database.<BR>
	 * <BR>
	 */
	private boolean createDb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_CHARACTER);
			statement.setString(1, _accountName);
			statement.setInt(2, getObjectId());
			statement.setString(3, getName());
			statement.setInt(4, getLevel());
			statement.setInt(5, getMaxHp());
			statement.setDouble(6, getCurrentHp());
			statement.setInt(7, getMaxCp());
			statement.setDouble(8, getCurrentCp());
			statement.setInt(9, getMaxMp());
			statement.setDouble(10, getCurrentMp());
			statement.setInt(11, getAppearance().getFace());
			statement.setInt(12, getAppearance().getHairStyle());
			statement.setInt(13, getAppearance().getHairColor());
			statement.setInt(14, getAppearance().getSex() ? 1 : 0);
			statement.setLong(15, getExp());
			statement.setInt(16, getSp());
			statement.setInt(17, getKarma());
			statement.setInt(18, getFame());
			statement.setInt(19, getPvpKills());
			statement.setInt(20, getPkKills());
			statement.setInt(21, getClanId());
			statement.setInt(22, getRace().ordinal());
			statement.setInt(23, getClassId().getId());
			statement.setLong(24, getDeleteTimer());
			statement.setInt(25, getAccDisplay());
			statement.setString(26, getTitle());
			statement.setInt(27, getAccessLevel().getLevel());
			statement.setInt(28, isOnline());
			statement.setInt(29, isIn7sDungeon() ? 1 : 0);
			statement.setInt(30, getClanPrivileges());
			statement.setInt(31, getWantsPeace());
			statement.setInt(32, getBaseClassId());
			statement.setInt(33, getNewbie());
			statement.setInt(34, isNoble() ? 1 : 0);
			statement.setLong(35, 0);
			statement.setLong(36, getCharCreatedTime());
			statement.setInt(37, getEventKills());
			statement.setInt(38, getSiegeKills());
			statement.setInt(39, getRaidKills());
			statement.setInt(40, getOlympiadWins());
			statement.setInt(41, getCpPoints());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.severe("Could not insert char data: " + e);
			return false;
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
		return true;
	}
	
	private boolean createDbForClassPaths()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_CLASSPATHS);
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.severe("Could not insert char classpath data: " + e);
			return false;
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
		return true;
	}
	
	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Retrieve the L2PcInstance from the characters table of the database</li>
	 * <li>Add the L2PcInstance object in _allObjects</li>
	 * <li>Set the x,y,z position of the L2PcInstance and make it invisible</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR>
	 * <BR>
	 *
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @return The L2PcInstance loaded from the database
	 */
	private static L2PcInstance restore(int objectId)
	{
		L2PcInstance player = null;
		Connection con = null;
		try
		{
			// Retrieve the L2PcInstance from the characters table of the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER);
			statement.setInt(1, objectId);
			ResultSet rset = statement.executeQuery();
			double currentCp = 0;
			double currentHp = 0;
			double currentMp = 0;
			while (rset.next())
			{
				final int activeClassId = rset.getInt("classid");
				final boolean female = rset.getInt("sex") != 0;
				final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
				PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);
				player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
				player.setName(rset.getString("char_name"));
				player._lastAccess = rset.getLong("lastAccess");
				player.setRace(rset.getInt("race"));
				player.getStat().setExp(rset.getLong("exp"));
				player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
				player.getStat().setLevel(rset.getByte("level"));
				player.getStat().setSp(rset.getInt("sp"));
				player.setWantsPeace(rset.getInt("wantspeace"));
				player.setHeading(rset.getInt("heading"));
				player.setKarma(rset.getInt("karma"));
				player.setFame(rset.getInt("fame"));
				player.setPvpKills(rset.getInt("pvpkills"));
				player.setPkKills(rset.getInt("pkkills"));
				player.setOnlineTime(rset.getLong("onlinetime"));
				player.setNewbie(rset.getInt("newbie"));
				player.setNoble(rset.getInt("nobless") == 1);
				player.setAccDisplay(rset.getByte("cancraft"));
				player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
				if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
				{
					player.setClanJoinExpiryTime(0);
				}
				player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
				if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
				{
					player.setClanCreateExpiryTime(0);
				}
				player.loadVariables(con);
				int clanId = rset.getInt("clanid");
				player.setPowerGrade((int) rset.getLong("power_grade"));
				player.setPledgeType(rset.getInt("subpledge"));
				player.setCharCreatedTime(rset.getLong("last_recom_date"));
				// player.setApprentice(rset.getInt("apprentice"));
				if (clanId > 0)
				{
					player.setClan(ClanTable.getInstance().getClan(clanId));
				}
				if (player.getClan() != null)
				{
					if (player.getClan().getLeaderId() != player.getObjectId())
					{
						if (player.getPowerGrade() == 0)
						{
							player.setPowerGrade(5);
						}
						player.setClanPrivileges(player.getClan().getRankPrivs(player.getPowerGrade()));
					}
					else
					{
						player.setClanPrivileges(L2Clan.CP_ALL);
						player.setPowerGrade(1);
					}
				}
				else
				{
					player.setClanPrivileges(L2Clan.CP_NOTHING);
				}
				player.setDeleteTimer(rset.getLong("deletetime"));
				player.setTitle(rset.getString("title"));
				player.setAccessLevel(rset.getInt("accesslevel"));
				player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
				player.setUptime(System.currentTimeMillis());
				currentHp = rset.getDouble("curHp");
				currentCp = rset.getDouble("curCp");
				currentMp = rset.getDouble("curMp");
				// Check recs
				player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));
				player._classIndex = 0;
				try
				{
					player.setBaseClass(rset.getInt("base_class"));
				}
				catch (Exception e)
				{
					player.setBaseClass(activeClassId);
				}
				// Restore Subclass Data (cannot be done earlier in function)
				if (restoreSubClassData(player))
				{
					if (activeClassId != player.getBaseClassId())
					{
						for (SubClass subClass : player.getSubClasses().values())
							if (subClass.getClassId() == activeClassId)
								player._classIndex = subClass.getClassIndex();
					}
				}
				if (player.getClassIndex() == 0 && activeClassId != player.getBaseClassId())
				{
					// Subclass in use but doesn't exist in DB -
					// a possible restart-while-modifysubclass cheat has been attempted.
					// Switching to use base class
					player.setClassId(player.getBaseClassId());
					_log.warning("Player " + player.getName() + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
				}
				else
					player._activeClass = activeClassId;
				player.setApprentice(rset.getInt("apprentice"));
				player.setSponsor(rset.getInt("sponsor"));
				player.setIsIn7sDungeon(rset.getInt("isin7sdungeon") == 1);
				player.setPunishLevel(rset.getInt("punish_level"));
				player.setEquipmentViewerHide(rset.getInt("lvl_joined_academy"));
				if (player.getPunishLevel() != PunishLevel.NONE)
					player.setPunishTimer(rset.getLong("punish_timer"));
				else
					player.setPunishTimer(0);
				/* CursedWeaponsManager.getInstance().checkPlayer(player); */
				player.setAllianceWithVarkaKetra(rset.getInt("varka_ketra_ally"));
				player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));
				// Add the L2PcInstance object in _allObjects
				// L2World.getInstance().storeObject(player);
				// Set the x,y,z position of the L2PcInstance and make it invisible
				player.setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));
				// Set Teleport Bookmark Slot
				player.setBookMarkSlot(rset.getInt("BookmarkSlot"));
				player.setFameStreak(rset.getInt("streak"));
				player.setLastKill1(rset.getString("lastKill1"));
				player.setLastKill2(rset.getString("lastKill2"));
				player.setVitalityPoints(rset.getInt("vitality_points"), true);
				player._heroWpnDelCount = rset.getByte("heroWpnDel");
				player.setNameC(rset.getString("nameC"));
				player.setTitleC(rset.getString("titleC"));
				player.setEventKills(rset.getInt("event_kills"));
				player.setRaidKills(rset.getInt("raid_kills"));
				player.setSiegeKills(rset.getInt("siege_kills"));
				player.setOlympiadWins(rset.getInt("olympiad_wins"));
				player.refreshMuseumOnlineTime();
				// Retrieve the name and ID of the other characters assigned to this account.
				PreparedStatement stmt = con.prepareStatement("SELECT charId, char_name FROM characters WHERE account_name=? AND charId<>?");
				stmt.setString(1, player._accountName);
				stmt.setInt(2, objectId);
				ResultSet chars = stmt.executeQuery();
				while (chars.next())
				{
					Integer charId = chars.getInt("charId");
					String charName = chars.getString("char_name");
					player._chars.put(charId, charName);
				}
				chars.close();
				stmt.close();
				BBSSchemeBufferInstance.loadSchemes(player, con);
				// PreparedStatement stmt2 = con.prepareStatement("SELECT charId, char_name, lasthwid FROM characters LEFT JOIN accounts ON characters.account_name = accounts.login where accounts.lasthwid=?");
				// stmt.setString(1, player.getHWID());
				// ResultSet chars2 = stmt.executeQuery();
				// while (chars2.next())
				// {
				// String charName = chars.getString("char_name");
				// String lasthwid = chars.getString("lasthwid");
				// player._chars2.put(charName, lasthwid);
				// }
				// chars2.close();
				// stmt2.close();
				break;
			}
			rset.close();
			statement.close();
			if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
			{
				player.getCounters().load();
			}
			// Retrieve from the database all secondary data of this L2PcInstance
			// and reward expertise/lucky skills if necessary.
			// Note that Clan, Noblesse and Hero skills are given separately and not here.
			player.restoreCharData();
			player.rewardSkills();
			// buff and status icons
			if (Config.STORE_SKILL_COOLTIME)
				player.restoreEffects();
			// Restore current Cp, HP and MP values
			player.setCurrentCp(currentCp);
			player.setCurrentHp(currentHp);
			player.setCurrentMp(currentMp);
			if (currentHp < 0.5)
			{
				player.setIsDead(true);
				player.stopHpMpRegeneration();
			}
			// Restore pet if exists in the world
			player.setPet(L2World.getInstance().getPet(player.getObjectId()));
			if (player.getPet() != null)
				player.getPet().setOwner(player);
			statement = con.prepareStatement("SELECT lockdowntime, secret, pincode, email FROM accounts WHERE login=?");
			statement.setString(1, player._accountName);
			rset = statement.executeQuery();
			if (rset.next())
			{
				long lockdowntime = rset.getInt("lockdowntime");
				if (lockdowntime > player._lockdownTime && lockdowntime * 1000 > System.currentTimeMillis())
					player._lockdownTime = (int) lockdowntime;
				final String secret = rset.getString("secret");
				if (secret == null || secret.equalsIgnoreCase(""))
				{
					player.setSecretCode(null);
				}
				else
				{
					player.setSecretCode(secret);
				}
				final String pincode = rset.getString("pincode");
				if (pincode == null || pincode.equalsIgnoreCase(""))
				{
					player.setPinCode(null);
				}
				else
				{
					player.setPinCode(pincode);
				}
				final String email = rset.getString("email");
				if (email == null || email.equalsIgnoreCase("") || email.equalsIgnoreCase("none"))
				{
					player.setEmail("none");
				}
				else
				{
					player.setEmail(email);
				}
			}
			rset.close();
			statement.close();
			// MuseumManager.getInstance().restoreDataForChar(player);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed loading character.", e);
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
		return player;
	}
	
	private void setEquipmentViewerHide(int int1)
	{
		_equipmentViewer = int1;
	}
	
	public void doUnLockdown()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE accounts SET lockdowntime=? WHERE login=?");
			statement.setInt(1, 0);
			statement.setString(2, getAccountName());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed setting lockdown time", e);
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
		sendMessage("Account is now unlocked");
		sendLockdownTime();
	}
	
	public void doLockdown(double hours)
	{
		if (hours < 0.1 || hours > 504)
		{
			sendMessage("It's a minium of 0.1 and a maximum of 504 hours");
			return;
		}
		if (getLockdownTime() >= System.currentTimeMillis() / 1000)
		{
			sendMessage("Your account is already locked down for another " + (double) ((getLockdownTime() - (System.currentTimeMillis() / 1000))) / 3600 + " hours");
			return;
		}
		final int inSeconds = (int) (hours * 3600);
		final int lockdowntime = (int) (System.currentTimeMillis() / 1000 + inSeconds);
		setLockdownTime(lockdowntime);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE accounts SET lockdowntime=? WHERE login=?");
			statement.setInt(1, lockdowntime);
			statement.setString(2, getAccountName());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed setting lockdown time", e);
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
		sendLockdownTime();
	}
	
	public boolean isAccountLockedDown()
	{
		return getLockdownTime() > 0 && getLockdownTime() > (int) (System.currentTimeMillis() / 1000);
	}
	
	public void sendLockdownTime()
	{
		sendMessage("Your account is locked down for " + (double) ((getLockdownTime() - (System.currentTimeMillis() / 1000))) / 3600 + " hours");
	}
	
	final private void setFameStreak(int streak)
	{
		_famestreak = streak;
	}
	
	/**
	 * @return
	 */
	public Forum getMail()
	{
		if (_forumMail == null)
		{
			setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			if (_forumMail == null)
			{
				ForumsBBSManager.getInstance().createNewForum(getName(), ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY, getObjectId());
				setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			}
		}
		return _forumMail;
	}
	
	/**
	 * @param forum
	 */
	public void setMail(Forum forum)
	{
		_forumMail = forum;
	}
	
	/**
	 * @return
	 */
	public Forum getMemo()
	{
		if (_forumMemo == null)
		{
			setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			if (_forumMemo == null)
			{
				ForumsBBSManager.getInstance().createNewForum(_accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
				setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			}
		}
		return _forumMemo;
	}
	
	/**
	 * @param forum
	 */
	public void setMemo(Forum forum)
	{
		_forumMemo = forum;
	}
	
	/**
	 * Restores sub-class data for the L2PcInstance, used to check the current
	 * class index for the character.
	 */
	private static boolean restoreSubClassData(L2PcInstance player)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES);
			statement.setInt(1, player.getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				SubClass subClass = new SubClass();
				subClass.setClassId(rset.getInt("class_id"));
				subClass.setLevel(rset.getByte("level"));
				subClass.setExp(rset.getLong("exp"));
				subClass.setSp(rset.getInt("sp"));
				subClass.setClassIndex(rset.getInt("class_index"));
				// Enforce the correct indexing of _subClasses against their class indexes.
				player.getSubClasses().put(subClass.getClassIndex(), subClass);
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore classes for " + player.getName() + ": " + e);
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
		return true;
	}
	
	/**
	 * Restores secondary data for the L2PcInstance, based on the current class index.
	 */
	private void restoreCharData()
	{
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills.
		restoreSkills();
		// Retrieve from the database all macroses of this L2PcInstance and add them to _macroses.
		_macroses.restore();
		// Retrieve from the database all shortCuts of this L2PcInstance and add them to _shortCuts.
		_shortCuts.restore();
		// Retrieve from the database all henna of this L2PcInstance and add them to _henna.
		restoreHenna();
		// Retrieve from the database all teleport bookmark of this L2PcInstance and add them to _tpbookmark.
		restoreTeleportBookmark();
		// Retrieve from the database all recom data of this L2PcInstance and add to _recomChars.
		if (Config.ALT_RECOMMEND)
			restoreRecom();
		// Retrieve from the database the recipe book of this L2PcInstance.
		restoreRecipeBook(true);
	}
	
	/**
	 * Restore recipe book data for this L2PcInstance.
	 */
	private void restoreRecipeBook(boolean loadCommon)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			String sql = loadCommon ? "SELECT id, type FROM character_recipebook WHERE charId=? AND classIndex=?" : "SELECT id FROM character_recipebook WHERE charId=? AND classIndex=? AND type = 1";
			PreparedStatement statement = con.prepareStatement(sql);
			statement.setInt(1, getObjectId());
			statement.setInt(2, _classIndex);
			ResultSet rset = statement.executeQuery();
			L2RecipeList recipe;
			while (rset.next())
			{
				recipe = RecipeController.getInstance().getRecipeList(rset.getInt("id"));
				if (loadCommon)
				{
					if (rset.getInt(2) == 1)
						registerDwarvenRecipeList(recipe, false);
					else
						registerCommonRecipeList(recipe, false);
				}
				else
					registerDwarvenRecipeList(recipe, false);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			if (_log.isLoggable(Level.SEVERE))
				_log.warning("Could not restore recipe book data:" + e);
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
	
	/**
	 * Update L2PcInstance stats in the characters table of the database.<BR>
	 * <BR>
	 */
	public synchronized void store(boolean storeActiveEffects, boolean isDuringClassChange)
	{
		storeCharBase();
		storeCharSub();
		storeEffect(storeActiveEffects, isDuringClassChange);
		transformInsertInfo();
		// MuseumManager.getInstance().updateDataForChar(this);
		/* EventStats.getInstance().save(this); */
		if (getLoginData() != null)
		{
			getLoginData().updateOnlineTime();
		}
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			getCounters().save();
		}
	}
	
	public void store()
	{
		store(true, false);
	}
	
	public void storeCharBase()
	{
		Connection con = null;
		try
		{
			// Get the exp, level, and sp of base class to store in base table
			int currentClassIndex = getClassIndex();
			_classIndex = 0;
			long exp = getStat().getExp();
			int level = getStat().getLevel();
			int sp = getStat().getSp();
			_classIndex = currentClassIndex;
			con = L2DatabaseFactory.getInstance().getConnection();
			// Update base class
			PreparedStatement statement = con.prepareStatement(UPDATE_CHARACTER);
			statement.setInt(1, level);
			statement.setInt(2, getMaxHp());
			statement.setDouble(3, getCurrentHp());
			statement.setInt(4, getMaxCp());
			statement.setDouble(5, getCurrentCp());
			statement.setInt(6, getMaxMp());
			statement.setDouble(7, getCurrentMp());
			statement.setInt(8, getAppearance().getFace());
			statement.setInt(9, getAppearance().getHairStyle());
			statement.setInt(10, getAppearance().getHairColor());
			statement.setInt(11, getAppearance().getSex() ? 1 : 0);
			statement.setInt(12, getHeading());
			statement.setInt(13, _observerMode ? _obsX : getX());
			statement.setInt(14, _observerMode ? _obsY : getY());
			statement.setInt(15, _observerMode ? _obsZ : getZ());
			statement.setLong(16, exp);
			statement.setLong(17, getExpBeforeDeath());
			statement.setInt(18, sp);
			statement.setInt(19, getKarma());
			statement.setInt(20, getFame());
			statement.setInt(21, getPvpKills());
			statement.setInt(22, getPkKills());
			statement.setInt(23, getRecomHave());
			statement.setInt(24, getRecomLeft());
			statement.setInt(25, getClanId());
			statement.setInt(26, getRace().ordinal());
			statement.setInt(27, getClassId().getId());
			statement.setLong(28, getDeleteTimer());
			statement.setInt(29, _turnedGMOff > 0 ? _turnedGMOff : getAccessLevel().getLevel());
			statement.setInt(30, isOnline());
			statement.setInt(31, isIn7sDungeon() ? 1 : 0);
			statement.setInt(32, getClanPrivileges());
			statement.setInt(33, getWantsPeace());
			statement.setInt(34, getBaseClassId());
			long totalOnlineTime = _onlineTime;
			if (_onlineBeginTime > 0)
				totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;
			// if (getMuseumPlayer() != null)
			// {
			// getMuseumPlayer().addData("play_duration", (System.currentTimeMillis() - _museumOnlineTime) / 1000);
			// }
			// _museumOnlineTime = System.currentTimeMillis();
			statement.setLong(35, totalOnlineTime);
			statement.setInt(36, getPunishLevel().value());
			statement.setLong(37, getPunishTimer());
			statement.setInt(38, getNewbie());
			statement.setInt(39, isNoble() ? 1 : 0);
			statement.setLong(40, getPowerGrade());
			statement.setInt(41, getPledgeType());
			statement.setLong(42, getCharCreatedTime());
			statement.setInt(43, _equipmentViewer);
			statement.setLong(44, getApprentice());
			statement.setLong(45, getSponsor());
			statement.setInt(46, getAllianceWithVarkaKetra());
			statement.setLong(47, getClanJoinExpiryTime());
			statement.setLong(48, getClanCreateExpiryTime());
			/* statement.setString(50, getName()); */
			statement.setLong(49, getDeathPenaltyBuffLevel());
			statement.setInt(50, getBookMarkSlot());
			statement.setInt(51, getFameStreak());
			statement.setString(52, getLastKill1());
			statement.setString(53, getLastKill2());
			statement.setInt(54, getVitalityPoints());
			statement.setByte(55, _heroWpnDelCount);
			statement.setByte(56, _accDisplay);
			statement.setString(57, _nameC);
			statement.setString(58, _titleC);
			statement.setInt(59, getEventKills());
			statement.setInt(60, getRaidKills());
			statement.setInt(61, getSiegeKills());
			statement.setInt(62, getOlympiadWins());
			statement.setInt(63, getMaxCpPoints() - getCpPoints());
			statement.setInt(64, getObjectId());
			statement.execute();
			statement.close();
			if (getLockdownTime() > 0 && getLockdownTime() * 1000 <= System.currentTimeMillis())
			{
				try
				{
					statement = con.prepareStatement("UPDATE accounts SET lockdowntime=? WHERE login=?");
					statement.setInt(1, 0);
					statement.setString(2, getAccountName());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "Failed resetting lockdown time", e);
				}
			}
		}
		catch (Exception e)
		{
			_log.warning("Could not store char base data: " + e);
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
	// MuseumPlayer _mp = null;
	//
	// public MuseumPlayer getMuseumPlayer()
	// {
	// return _mp;
	// }
	//
	// public void setMuseumPlayer(MuseumPlayer player)
	// {
	// _mp = player;
	// }
	
	public long getTotalOnlineTime()
	{
		long totalOnlineTime = _onlineTime;
		if (_onlineBeginTime > 0)
			totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;
		return totalOnlineTime;
	}
	
	public byte _heroWpnDelCount;
	
	final private String getLastKill1()
	{
		return _prev1IP;
	}
	
	final private String getLastKill2()
	{
		return _prev2IP;
	}
	
	final private void setLastKill1(final String ip)
	{
		_prev1IP = ip;
	}
	
	final private void setLastKill2(final String ip)
	{
		_prev2IP = ip;
	}
	
	final public void setNameC(final String nameC)
	{
		_nameC = nameC;
	}
	
	final public void setTitleC(final String titleC)
	{
		_titleC = titleC;
	}
	
	final private String getNameC()
	{
		return _nameC;
	}
	
	final private String getTitleC()
	{
		return _titleC;
	}
	
	private void storeCharSub()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS);
			if (getTotalSubClasses() > 0)
			{
				for (SubClass subClass : getSubClasses().values())
				{
					statement.setLong(1, subClass.getExp());
					statement.setInt(2, subClass.getSp());
					statement.setInt(3, subClass.getLevel());
					statement.setInt(4, subClass.getClassId());
					statement.setInt(5, getObjectId());
					statement.setInt(6, subClass.getClassIndex());
					statement.execute();
				}
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.warning("Could not store sub class data for " + getName() + ": " + e);
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
	
	@SuppressWarnings("incomplete-switch")
	private void storeEffect(boolean storeEffects, boolean isDuringClassChange)
	{
		if (!Config.STORE_SKILL_COOLTIME)
			return;
		Map<Integer, Integer[]> retrySkills = _retrySkills;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			// Delete all current stored effects for char to avoid dupe
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.execute();
			statement.close();
			int buff_index = 0;
			final List<Integer> storedSkills = new FastList<Integer>();
			// Store all effect data along with calulated remaining
			// reuse delays for matching skills. 'restore_type'= 0.
			statement = con.prepareStatement(ADD_SKILL_SAVE);
			if (storeEffects)
			{
				for (L2Effect effect : getAllEffects())
				{
					if (effect == null || effect.isStolen())
						continue;
					if (isDuringClassChange && effect.getSkill().isStayAfterDeath())
						continue;
					switch (effect.getEffectType())
					{
						case HEAL_OVER_TIME:
						case COMBAT_POINT_HEAL_OVER_TIME:
						case DMG_OVER_TIME:
						case MANA_DMG_OVER_TIME:
						case MANA_HEAL_OVER_TIME:
						case MP_CONSUME_PER_LEVEL:
						case SILENT_MOVE:
						case FAKE_DEATH:
						case INVISIBLE:
						case PREVENT_BUFF:
						case FUSION:
						case STONESKIN:
						case DIMENSIONALWALK:
						case IMMOBILE_BUFF:
							continue;
					}
					L2Skill skill = effect.getSkill();
					int skillId = skill.getId();
					if (storedSkills.contains(skillId))
						continue;
					storedSkills.add(skillId);
					if (effect != null && !effect.isHerbEffect() && effect.getInUse() && !skill.isToggle())
					{
						statement.setInt(1, getObjectId());
						statement.setInt(2, skillId);
						statement.setInt(3, skill.getLevel());
						statement.setInt(4, effect.getCount());
						statement.setInt(5, effect.getElapsedTime());
						if (_reuseTimeStamps.containsKey(skillId))
						{
							TimeStamp t = _reuseTimeStamps.get(skillId);
							statement.setLong(6, t.hasNotPassed() ? t.getReuse() : 0);
							statement.setDouble(7, t.hasNotPassed() ? t.getStamp() : 0);
						}
						else if (retrySkills.containsKey(skillId) && retrySkills.get(skillId)[1] >= GameTimeController.getGameTicks())
						{
							statement.setLong(6, skill.getReuseDelay(this));
							statement.setDouble(7, System.currentTimeMillis() + skill.getReuseDelay(this));
						}
						else
						{
							statement.setLong(6, 0);
							statement.setDouble(7, 0);
						}
						statement.setInt(8, 0);
						statement.setInt(9, getClassIndex());
						statement.setInt(10, ++buff_index);
						statement.execute();
					}
				}
			}
			// Store the reuse delays of remaining skills which
			// lost effect but still under reuse delay. 'restore_type' 1.
			for (TimeStamp t : _reuseTimeStamps.values())
			{
				if (t.hasNotPassed())
				{
					int skillId = t.getSkill();
					if (storedSkills.contains(skillId))
						continue;
					storedSkills.add(skillId);
					statement.setInt(1, getObjectId());
					statement.setInt(2, skillId);
					statement.setInt(3, -1);
					statement.setInt(4, -1);
					statement.setInt(5, -1);
					statement.setLong(6, t.getReuse());
					statement.setDouble(7, t.getStamp());
					statement.setInt(8, 1);
					statement.setInt(9, getClassIndex());
					statement.setInt(10, ++buff_index);
					statement.execute();
				}
			}
			for (Integer skillId : retrySkills.keySet())
			{
				if (retrySkills.get(skillId)[1] < GameTimeController.getGameTicks())
					continue;
				if (storedSkills.contains(skillId))
					continue;
				storedSkills.add(skillId);
				L2Skill skill = getKnownSkill(skillId);
				if (skill == null)
					skill = SkillTable.getInstance().getInfo(skillId, SkillTable.getInstance().getMaxLevel(skillId));
				statement.setInt(1, getObjectId());
				statement.setInt(2, skillId);
				statement.setInt(3, -1);
				statement.setInt(4, -1);
				statement.setInt(5, -1);
				statement.setLong(6, skill.getReuseDelay(this));
				statement.setDouble(7, System.currentTimeMillis() + skill.getReuseDelay(this));
				statement.setInt(8, 1);
				statement.setInt(9, getClassIndex());
				statement.setInt(10, ++buff_index);
				statement.execute();
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not store char effect data: ", e);
		}
		finally
		{
			retrySkills = null;
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	/**
	 * Return True if the L2PcInstance is on line.<BR>
	 * <BR>
	 */
	public int isOnline()
	{
		return (_isOnline ? 1 : 0);
	}
	
	public boolean isIn7sDungeon()
	{
		return _isIn7sDungeon;
	}
	
	/**
	 * Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance and save update in the character_skills table of the database.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2PcInstance are identified in <B>_skills</B><BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li>
	 * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the L2Character</li><BR>
	 * <BR>
	 *
	 * @param newSkill
	 *            The L2Skill to add to the L2Character
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	public L2Skill addSkill(L2Skill newSkill, boolean store)
	{
		// Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance
		L2Skill oldSkill = super.addSkill(newSkill);
		// Add or update a L2PcInstance skill in the character_skills table of the database
		if (store)
		{
			storeSkill(newSkill, oldSkill, -1);
		}
		return oldSkill;
	}
	
	@Override
	public L2Skill removeSkill(L2Skill skill, boolean store)
	{
		if (skill == null)
			return null;
		if (store)
			return removeSkill(skill.getId());
		else
			return super.removeSkill(skill.getId(), true);
	}
	
	public L2Skill removeSkill(L2Skill skill, boolean store, boolean cancelEffect)
	{
		if (skill == null)
			return null;
		if (store)
			return removeSkill(skill.getId());
		else
			return super.removeSkill(skill.getId(), cancelEffect);
	}
	
	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the skill from the L2Character _skills</li>
	 * <li>Remove all its Func objects from the L2Character calculator set</li><BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance : Save update in the character_skills table of the database</li><BR>
	 * <BR>
	 *
	 * @param skill
	 *            The L2Skill to remove from the L2Character
	 * @return The L2Skill removed
	 */
	@Override
	public L2Skill removeSkill(L2Skill skill)
	{
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		L2Skill oldSkill = super.removeSkill(skill);
		Connection con = null;
		try
		{
			// Remove or update a L2PcInstance skill from the character_skills table of the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);
			if (oldSkill != null)
			{
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Error could not delete skill: " + e);
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
		if (transformId() > 0 || isCursedWeaponEquipped())
			return oldSkill;
		L2ShortCut[] allShortCuts = getAllShortCuts();
		for (L2ShortCut sc : allShortCuts)
		{
			if (sc != null && skill != null && sc.getId() == skill.getId() && sc.getType() == L2ShortCut.TYPE_SKILL && !(skill.getId() >= 3000 && skill.getId() < 4000))
				deleteShortCut(sc.getSlot(), sc.getPage());
		}
		return oldSkill;
	}
	
	public L2Skill removeSkill2(L2Skill skill)
	{
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		L2Skill oldSkill = super.removeSkill(skill);
		Connection con = null;
		try
		{
			// Remove or update a L2PcInstance skill from the character_skills table of the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);
			if (oldSkill != null)
			{
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Error could not delete skill: " + e);
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
		if (transformId() > 0 || isCursedWeaponEquipped())
			return oldSkill;
		L2ShortCut[] allShortCuts = getAllShortCuts();
		for (L2ShortCut sc : allShortCuts)
		{
			if (sc != null && skill != null && sc.getId() == skill.getId() && sc.getType() == L2ShortCut.TYPE_SKILL && !(skill.getId() >= 3000 && skill.getId() < 4000))
				deleteShortCut(sc.getSlot(), sc.getPage());
		}
		return oldSkill;
	}
	
	/**
	 * Add or update a L2PcInstance skill in the character_skills table of the database.
	 * <BR>
	 * <BR>
	 * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
	 */
	private void storeSkill(L2Skill newSkill, L2Skill oldSkill, int newClassIndex)
	{
		int classIndex = _classIndex;
		if (newClassIndex > -1)
			classIndex = newClassIndex;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			if (oldSkill != null && newSkill != null)
			{
				statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL);
				statement.setInt(1, newSkill.getLevel());
				statement.setInt(2, oldSkill.getId());
				statement.setInt(3, getObjectId());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
			}
			else if (newSkill != null)
			{
				statement = con.prepareStatement(ADD_NEW_SKILL);
				statement.setInt(1, getObjectId());
				statement.setInt(2, newSkill.getId());
				statement.setInt(3, newSkill.getLevel());
				statement.setString(4, newSkill.getName());
				statement.setInt(5, classIndex);
				statement.execute();
				statement.close();
			}
			else
			{
				_log.warning("could not store new skill. its NULL");
			}
		}
		catch (Exception e)
		{
			removeSkill(newSkill.getId());
			_log.warning( getName() + " Error could not store char skills: " + e);
			//getClient().closeNow();
			//_log.warning("Error could not store char skills: " + e);
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
	
	private boolean storeSkillBool(L2Skill newSkill, L2Skill oldSkill, int newClassIndex)
	{
		boolean ok = false;
		int classIndex = _classIndex;
		if (newClassIndex > -1)
			classIndex = newClassIndex;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			if (oldSkill != null && newSkill != null)
			{
				statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL);
				statement.setInt(1, newSkill.getLevel());
				statement.setInt(2, oldSkill.getId());
				statement.setInt(3, getObjectId());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
				ok = true;
			}
			else if (newSkill != null)
			{
				statement = con.prepareStatement(ADD_NEW_SKILL);
				statement.setInt(1, getObjectId());
				statement.setInt(2, newSkill.getId());
				statement.setInt(3, newSkill.getLevel());
				statement.setString(4, newSkill.getName());
				statement.setInt(5, classIndex);
				statement.execute();
				statement.close();
				ok = true;
			}
			else
			{
				_log.warning("could not store new skill. its NULL");
			}
		}
		catch (Exception e)
		{
			removeSkill(newSkill.getId());

			getClient().closeNow();
			//_log.warning("Error could not store char skills: " + e);
			ok = false;
			this.getClient().closeNow();
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
		return ok;
	}
	
	/**
	 * Retrieve from the database all skills of this L2PcInstance and add them to _skills.<BR>
	 * <BR>
	 */
	private void restoreSkills()
	{
		Connection con = null;
		try
		{
			// Retrieve all skills of this L2PcInstance from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();
			// Go though the recordset of this SQL query
			while (rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");
				/*
				 * if (id > 9000 && id < 9007)
				 * continue; // fake skills for base stats
				 */
				// Create a L2Skill object for each record
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
				super.addSkill(skill);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore character skills: " + e);
		}
		finally
		{
			try
			{
				sendSkillList();
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	/**
	 * Retrieve from the database all skill effects of this L2PcInstance and add them to the player.<BR>
	 * <BR>
	 */
	public void restoreEffects()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;
			/**
			 * Restore Type 0
			 * These skill were still in effect on the character
			 * upon logout. Some of which were self casted and
			 * might still have had a long reuse delay which also
			 * is restored.
			 */
			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.setInt(3, 0);
			rset = statement.executeQuery();
			while (rset.next())
			{
				int skillId = rset.getInt("skill_id");
				int skillLvl = rset.getInt("skill_level");
				int effectCount = rset.getInt("effect_count");
				int effectCurTime = rset.getInt("effect_cur_time");
				long reuseDelay = rset.getLong("reuse_delay");
				long systime = rset.getLong("systime");
				final long remainingTime = systime - System.currentTimeMillis();
				// Just incase the admin minipulated this table incorrectly :x
				if (skillId <= 0 || effectCount < 0 || effectCurTime < 0 || reuseDelay < 0)
					continue;
				L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
				L2Effect[] effects = skill.getEffectsRestore(this, this, null);
				if (remainingTime > 10)
				{
					disableSkill(skillId, remainingTime);
					addTimeStamp(new TimeStamp(skillId, reuseDelay, systime));
				}
				for (L2Effect effect : effects)
				{
					if (effect.getPeriod() < CharEffectList.BUFFER_BUFFS_DURATION && effectCurTime >= effect.getPeriod())
					{
						effect.exit();
						continue;
					}
					effect.setCount(effectCount);
					effect.setFirstTime(effectCurTime);
				}
			}
			rset.close();
			statement.close();
			/**
			 * Restore Type 1
			 * The remaning skills lost effect upon logout but
			 * were still under a high reuse delay.
			 */
			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.setInt(3, 1);
			rset = statement.executeQuery();
			while (rset.next())
			{
				int skillId = rset.getInt("skill_id");
				double reuseDelay = rset.getDouble("reuse_delay");
				double systime = rset.getDouble("systime");
				double remainingTime = systime - System.currentTimeMillis();
				if (remainingTime < 10)
					continue;
				disableSkill(skillId, (long) remainingTime);
				addTimeStamp(new TimeStamp(skillId, (long) reuseDelay, (long) systime));
			}
			rset.close();
			statement.close();
			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore active effect data: " + e);
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
	
	/**
	 * Retrieve from the database all Henna of this L2PcInstance, add them to _henna and calculate stats of the L2PcInstance.<BR>
	 * <BR>
	 */
	private void restoreHenna()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();
			for (int i = 0; i < 3; i++)
				_henna[i] = null;
			while (rset.next())
			{
				int slot = rset.getInt("slot");
				if (slot < 1 || slot > 3)
					continue;
				_henna[slot - 1] = HennaTable.getInstance().getTemplate(rset.getInt("symbol_id"));
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed restoing character hennas.", e);
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
		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
	}
	
	/**
	 * Retrieve from the database all Recommendation data of this L2PcInstance, add to _recomChars and calculate stats of the L2PcInstance.<BR>
	 * <BR>
	 */
	private void restoreRecom()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_RECOMS);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				_recomChars.add(rset.getInt("target_id"));
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed restoring character recommendations.", e);
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
	
	/**
	 * Return the number of Henna empty slot of the L2PcInstance.<BR>
	 * <BR>
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = 1 + getClassId().level();
		for (int i = 0; i < 3; i++)
			if (_henna[i] != null)
				totalSlots--;
		if (totalSlots <= 0)
			return 0;
		return totalSlots;
	}
	
	/**
	 * Remove a Henna of the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR>
	 * <BR>
	 */
	public boolean removeHenna(int slot)
	{
		if (isInOlympiadMode())
			return false;
		if (slot < 1 || slot > 3)
			return false;
		slot--;
		if (_henna[slot] == null)
			return false;
		_henna[slot] = null;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA);
			statement.setInt(1, getObjectId());
			statement.setInt(2, slot + 1);
			statement.setInt(3, getClassIndex());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed remocing character henna.", e);
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
		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
		// Send Server->Client HennaInfo packet to this L2PcInstance
		sendPacket(new HennaInfo(this));
		// Send Server->Client UserInfo packet to this L2PcInstance
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		return true;
	}
	
	/**
	 * Add a Henna to the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR>
	 * <BR>
	 */
	public boolean addHenna(L2Henna henna)
	{
		if (getHennaEmptySlots() <= 0)
		{
			sendMessage("You may not have more than three equipped symbols at a time.");
			return false;
		}
		boolean allow = false;
		for (L2Henna tmp : HennaTreeTable.getInstance().getAvailableHenna(getClassId()))
		{
			if (tmp == henna)
			{
				allow = true;
				break;
			}
		}
		if (!allow)
		{
			sendMessage("Wrong class to add this henna!");
			return false;
		}
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				_henna[i] = henna;
				// Calculate Henna modifiers of this L2PcInstance
				recalcHennaStats();
				Connection con = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA);
					statement.setInt(1, getObjectId());
					statement.setInt(2, henna.getSymbolId());
					statement.setInt(3, i + 1);
					statement.setInt(4, getClassIndex());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "Failed saving character henna.", e);
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
				// Send Server->Client HennaInfo packet to this L2PcInstance
				sendPacket(new HennaInfo(this));
				// Send Server->Client UserInfo packet to this L2PcInstance
				sendPacket(new UserInfo(this));
				sendPacket(new ExBrExtraUserInfo(this));
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Calculate Henna modifiers of this L2PcInstance.<BR>
	 * <BR>
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
				continue;
			_hennaINT += _henna[i].getStatINT();
			_hennaSTR += _henna[i].getStatSTR();
			_hennaMEN += _henna[i].getStatMEM();
			_hennaCON += _henna[i].getStatCON();
			_hennaWIT += _henna[i].getStatWIT();
			_hennaDEX += _henna[i].getStatDEX();
		}
		if (_hennaINT > 5)
			_hennaINT = 5;
		if (_hennaSTR > 5)
			_hennaSTR = 5;
		if (_hennaMEN > 5)
			_hennaMEN = 5;
		if (_hennaCON > 5)
			_hennaCON = 5;
		if (_hennaWIT > 5)
			_hennaWIT = 5;
		if (_hennaDEX > 5)
			_hennaDEX = 5;
	}
	
	/**
	 * Return the Henna of this L2PcInstance corresponding to the selected slot.<BR>
	 * <BR>
	 */
	public L2Henna getHenna(int slot)
	{
		if (slot < 1 || slot > 3)
			return null;
		return _henna[slot - 1];
	}
	
	/**
	 * Return the INT Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 */
	public int getHennaStatINT()
	{
		return _hennaINT;
	}
	
	/**
	 * Return the STR Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 */
	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}
	
	/**
	 * Return the CON Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 */
	public int getHennaStatCON()
	{
		return _hennaCON;
	}
	
	/**
	 * Return the MEN Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 */
	public int getHennaStatMEN()
	{
		/*
		 * if (isDaggerClass())
		 * if (_hennaMEN > 2)
		 * return 2;
		 */
		return _hennaMEN;
	}
	
	/**
	 * Return the WIT Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 */
	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}
	
	/**
	 * Return the DEX Henna modifier of this L2PcInstance.<BR>
	 * <BR>
	 */
	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}
	
	@Override
	public final void stopAllEffects()
	{
		super.stopAllEffects();
		updateAndBroadcastStatus(2);
	}
	
	@Override
	public final void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		super.stopAllEffectsExceptThoseThatLastThroughDeath();
		updateAndBroadcastStatus(2);
	}
	
	/**
	 * Return True if the L2PcInstance is autoAttackable.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Check if the attacker isn't the L2PcInstance Pet</li>
	 * <li>Check if the attacker is L2MonsterInstance</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same party</li>
	 * <li>Check if the L2PcInstance has Karma</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same siege clan (Attacker, Defender)</li><BR>
	 * <BR>
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (attacker == null || attacker == this || attacker == getPet() || isFakeDeath())
			return false;
		if (attacker instanceof L2MonsterInstance)
		{
			if (attacker instanceof L2FriendlyMobInstance)
				return false;
			return true;
		}
		if (attacker instanceof L2Playable)
		{
			final L2PcInstance PCattacker = attacker.getActingPlayer();
			if (PCattacker == null)
				return false;
			if (!attacker.isGM() && (isInvisible() && isGM()))
				return false;
			if (PCattacker.isInOlympiadMode())
			{
				if (isInOlympiadMode() && isOlympiadStart() && PCattacker.getOlympiadGameId() == getOlympiadGameId())
					return true;
				else
					return false;
			}
			if (attacker.isInFunEvent())
			{
				if (isInFunEvent())
				{
					if (attacker.CanAttackDueToInEvent(this))
					{
						return true;
					}
				}
				return false;
			}
			else if (isInFunEvent())
			{
				return false;
			}
			if (isInHuntersVillage() && PCattacker.isInHuntersVillage())
			{
				return true;
			}
			if (isInOrcVillage() && PCattacker.isInOrcVillage())
			{
				return true;
			}
			if (isInDuel())
			{
				if (!isAttackable(PCattacker))
					return false;
				else
					return true;
			}
			else if (PCattacker.isInDuel())
				return false;
			if (isCursedWeaponEquipped() || PCattacker.isCursedWeaponEquipped())
				return true;
			if (!PCattacker.isGM() && (isInsideZone(ZONE_PEACE) || PCattacker.isInsideZone(ZONE_PEACE)))
				return false;
			if (!isDisguised() && !attacker.isDisguised() && getAllyId() != 0 && getAllyId() == PCattacker.getAllyId())
				return false;
			if (!isDisguised() && !attacker.isDisguised() && getClan() != null && getClan().isMember(PCattacker.getObjectId()))
				return false;
			if (getParty() != null && getParty().getPartyMembers().contains(PCattacker))
				return false;
			if (isInsideZone(ZONE_PVP) && PCattacker.isInsideZone(ZONE_PVP))
				return true;
			if (isInPI() && PCattacker.isInPI())
				return true;
			if (_team == 1 && PCattacker._team == 2)
				return true;
			if (_team == 2 && PCattacker._team == 1)
				return true;
			if (PCattacker.canPKdueToOnesidedClanwar(this))
				return true;
			// Check if the L2PcInstance has Karma
			if (getKarma() > 0 || getPvpFlagLasts() >= System.currentTimeMillis())
				return true;
			if (getClan() != null)
			{
				final Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
				if (siege != null)
				{
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Defender clan
					if (siege.checkIsDefender(PCattacker.getClan()) && siege.checkIsDefender(getClan()))
						return false;
					/*
					 * // Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Attacker clan
					 * if (siege.checkIsAttacker(PCattacker.getClan()) && siege.checkIsAttacker(getClan()))
					 * return false;
					 */
				}
			}
		}
		else if (attacker instanceof L2SiegeGuardInstance || attacker instanceof L2FortSiegeGuardInstance)
		{
			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(this);
				return (siege != null && siege.checkIsAttacker(getClan()));
			}
		}
		return false;
	}
	
	public boolean isAutoAttackableIgnoreFlagging(L2Character attacker)
	{
		if (attacker == null || attacker == this || attacker == getPet() || isFakeDeath())
			return false;
		if (attacker instanceof L2MonsterInstance)
		{
			if (attacker instanceof L2FriendlyMobInstance)
				return false;
			return true;
		}
		if (attacker instanceof L2Playable)
		{
			final L2PcInstance PCattacker = attacker.getActingPlayer();
			if (PCattacker == null)
				return false;
			if (!attacker.isGM() && (isInvisible() && isGM()))
				return false;
			if (PCattacker.isInOlympiadMode())
			{
				if (isInOlympiadMode() && isOlympiadStart() && PCattacker.getOlympiadGameId() == getOlympiadGameId())
					return true;
				else
					return false;
			}
			if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getObjectId()))
				return true;
			if (attacker.isInFunEvent())
			{
				if (isInFunEvent())
				{
					if (attacker.CanAttackDueToInEvent(this))
					{
						return true;
					}
				}
				return false;
			}
			else if (isInFunEvent())
			{
				return false;
			}
			if (isInDuel())
			{
				if (!isAttackable(PCattacker))
					return false;
				else
					return true;
			}
			else if (PCattacker.isInDuel())
				return false;
			if (isCursedWeaponEquipped() || PCattacker.isCursedWeaponEquipped())
				return true;
			if (!PCattacker.isGM() && (isInsideZone(ZONE_PEACE) || PCattacker.isInsideZone(ZONE_PEACE)))
				return false;
			if (!isDisguised() && !attacker.isDisguised() && getAllyId() != 0 && getAllyId() == PCattacker.getAllyId())
				return false;
			if (!isDisguised() && !attacker.isDisguised() && getClan() != null && getClan().isMember(PCattacker.getObjectId()))
				return false;
			if (getParty() != null && getParty().getPartyMembers().contains(PCattacker))
				return false;
			if (isInsideZone(ZONE_PVP) && PCattacker.isInsideZone(ZONE_PVP))
				return true;
			if (_team == 1 && PCattacker._team == 2)
				return true;
			if (_team == 2 && PCattacker._team == 1)
				return true;
			if (isInPI() && PCattacker.isInPI())
				return true;
			if (PCattacker.canPKdueToOnesidedClanwar(this))
				return true;
			if (getKarma() > 0)
				return true;
			if (getClan() != null)
			{
				final Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
				if (siege != null)
				{
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Defender clan
					if (siege.checkIsDefender(PCattacker.getClan()) && siege.checkIsDefender(getClan()))
						return false;
					/*
					 * // Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Attacker clan
					 * if (siege.checkIsAttacker(PCattacker.getClan()) && siege.checkIsAttacker(getClan()))
					 * return false;
					 */
				}
			}
		}
		else if (attacker instanceof L2SiegeGuardInstance || attacker instanceof L2FortSiegeGuardInstance)
		{
			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(this);
				return (siege != null && siege.checkIsAttacker(getClan()));
			}
		}
		return false;
	}
	
	@Override
	public boolean isDebuffable(L2PcInstance attacker)
	{
		if (attacker == null || attacker == this)
			return false;
		if (attacker.isGM() && attacker.getAccessLevel().getLevel() > getAccessLevel().getLevel())
			return true;
		if (attacker.isInOlympiadMode())
		{
			if (isInOlympiadMode() && isOlympiadStart() && attacker.getOlympiadGameId() == getOlympiadGameId())
				return true;
			else
				return false;
		}
		if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getObjectId()))
			return true;
		if (attacker.isInFunEvent())
		{
			if (isInFunEvent())
			{
				if (attacker.CanAttackDueToInEvent(this))
					return true;
			}
			return false;
		}
		else if (isInFunEvent())
		{
			return false;
		}
		if (isInDuel())
		{
			if (!isAttackable(attacker))
				return false;
			else
				return true;
		}
		else if (attacker.isInDuel())
			return false;
		if (isCursedWeaponEquipped() || attacker.isCursedWeaponEquipped())
			return true;
		if (isInsideZone(ZONE_PVP) && attacker.isInsideZone(ZONE_PVP))
			return true;
		if (attacker.isInClanwarWith(this))
			return true;
		// Check if the L2PcInstance has Karma
		if (getKarma() > 0 || getPvpFlagLasts() >= System.currentTimeMillis())
			return true;
		return false;
	}
	
	public boolean isAutoAttackableSwitch(L2PcInstance attacker)
	{
		if (attacker == null || attacker == this || isDead())
			return false;
		if (isInvisible())
			return false;
		if (attacker.isInOlympiadMode())
			return false;
		if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getObjectId()))
			return true;
		if (isInDuel())
		{
			if (!isAttackable(attacker))
				return false;
			else
				return true;
		}
		else if (attacker.isInDuel())
			return false;
		if (isCursedWeaponEquipped() || attacker.isCursedWeaponEquipped())
			return true;
		if (TvT._started || NewTvT._started || FOS._started || NewFOS._started || CTF._started || NewCTF._started || VIP._started || NewDomination._started) // when events started it's slightly different
		{
			if (getInEventPeaceZone())
				return false;
			if (attacker.getInEventPeaceZone())
				return false;
			if (attacker._inEventTvT && _inEventTvT)
			{
				if (attacker._teamNameTvT == _teamNameTvT)
				{
					return false;
				}
				else
					return true;
			}
			if (attacker._inEventHG && _inEventHG)
			{
				if (attacker._teamNameHG == _teamNameHG)
				{
					return false;
				}
				else
					return true;
			}
			if (attacker._inEventLunaDomi && _inEventLunaDomi)
			{
				if (attacker._teamNameLunaDomi == _teamNameLunaDomi)
				{
					return false;
				}
				else
					return true;
			}
			else if (attacker._inEventFOS && _inEventFOS)
			{
				if (attacker._teamNameFOS == _teamNameFOS)
				{
					return false;
				}
				else
					return true;
			}
			else if (attacker._inEventCTF && _inEventCTF)
			{
				if (attacker._teamNameCTF == _teamNameCTF)
				{
					return false;
				}
				else
					return true;
			}
			else if (attacker._inEventVIP && _inEventVIP)
			{
				if (attacker.isOnSameTeamInVIP(this))
				{
					return false;
				}
				else
					return true;
			}
		}
		if (!attacker.isGM() && (isInsideZone(ZONE_PEACE) || attacker.isInsideZone(ZONE_PEACE)))
			return false;
		if (!isDisguised() && !attacker.isDisguised() && getClan() != null && getClan().isMember(attacker.getObjectId()))
			return true;
		if (_team == 1 && attacker._team == 2)
			return true;
		if (_team == 2 && attacker._team == 1)
			return true;
		if (isInsideZone(ZONE_PVP) && attacker.isInsideZone(ZONE_PVP))
			return true;
		if (isInPI() && attacker.isInPI())
			return true;
		if (attacker.canPKdueToOnesidedClanwar(this))
			return true;
		// Check if the L2PcInstance has Karma
		if (getKarma() > 0 || getPvpFlagLasts() >= System.currentTimeMillis())
			return true;
		if (getClan() != null)
		{
			final Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
			if (siege != null)
				return true;
		}
		return false;
	}
	
	public boolean isAutoAttackableTargetAll(L2PcInstance attacker, boolean ignoreSelf)
	{
		if (attacker == null || isDead())
			return false;
		if (attacker == this)
		{
			if (ignoreSelf)
				return false;
			else
				return true;
		}
		if (attacker.isInOlympiadMode())
			return false;
		if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getObjectId()))
			return true;
		if (isInDuel())
		{
			if (!isAttackable(attacker))
				return false;
			else
				return true;
		}
		else if (attacker.isInDuel())
			return false;
		if (isCursedWeaponEquipped() || attacker.isCursedWeaponEquipped())
			return true;
		if (TvT._started || NewTvT._started || FOS._started || NewFOS._started || NewCTF._started || CTF._started || VIP._started || NewDomination._started) // when events started it's slightly different
		{
			if (getInEventPeaceZone())
				return false;
			if (attacker.getInEventPeaceZone())
				return false;
			if (attacker._inEventTvT && _inEventTvT)
			{
				if (attacker._teamNameTvT == _teamNameTvT)
				{
					return false;
				}
				else
					return true;
			}
			if (attacker._inEventHG && _inEventHG)
			{
				if (attacker._teamNameHG == _teamNameHG)
				{
					return false;
				}
				else
					return true;
			}
			if (attacker._inEventLunaDomi && _inEventLunaDomi)
			{
				if (attacker._teamNameLunaDomi == _teamNameLunaDomi)
				{
					return false;
				}
				else
					return true;
			}
			else if (attacker._inEventFOS && _inEventFOS)
			{
				if (attacker._teamNameFOS == _teamNameFOS)
				{
					return false;
				}
				else
					return true;
			}
			else if (attacker._inEventCTF && _inEventCTF)
			{
				if (attacker._teamNameCTF == _teamNameCTF)
				{
					return false;
				}
				else
					return true;
			}
			else if (attacker._inEventVIP && _inEventVIP)
			{
				if (attacker.isOnSameTeamInVIP(this))
				{
					return false;
				}
				else
					return true;
			}
		}
		if (!attacker.isGM() && (isInsideZone(ZONE_PEACE) || attacker.isInsideZone(ZONE_PEACE)))
			return false;
		/*
		 * if (getClan() != null && getClan().isMember(attacker.getObjectId()))
		 * return true;
		 */
		if (isInsideZone(ZONE_PVP) && attacker.isInsideZone(ZONE_PVP))
			return true;
		if (attacker.canPKdueToOnesidedClanwar(this))
			return true;
		// Check if the L2PcInstance has Karma
		if (getKarma() > 0 || getPvpFlagLasts() >= System.currentTimeMillis())
			return true;
		if (getClan() != null)
		{
			final Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
			if (siege != null)
				return true;
		}
		return false;
	}
	
	public boolean isAutoAttackableTargetAll(L2PcInstance attacker)
	{
		return isAutoAttackableTargetAll(attacker, false);
	}
	
	public boolean canAttack(L2PcInstance target)
	{
		return canAttack(target, true);
	}
	
	public boolean canAttack(L2PcInstance target, boolean sendMessage)
	{
		if (inObserverMode())
		{
			if (sendMessage)
				sendMessage("Cant attack in observer mode");
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (target.isInvisible() && !isGM() && !canSeeInvisiblePeople())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (target.isCursedWeaponEquipped() && getLevel() < NEWBIE_LEVEL)
		{
			if (sendMessage)
				sendMessage("Cant attack a cursed Player when you are under level 75");
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (isCursedWeaponEquipped() && target.getLevel() < NEWBIE_LEVEL && !isGM())
		{
			if (sendMessage)
				sendMessage("Cant attack a newbie player with Zariche");
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (target.getLevel() < NEWBIE_LEVEL && target.getKarma() == 0 && !isGM())
		{
			if (sendMessage)
				sendMessage("Player under newbie protection.");
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (getLevel() < NEWBIE_LEVEL && !isGM())
		{
			if (sendMessage)
				sendMessage("Your level is to low to participate in player vs player combat.");
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (!isInOlympiadMode() && target.isInOlympiadMode())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (!isAttackable(target))
			return false;
		return true;
	}
	
	public boolean isSelectingTarget()
	{
		return targetSelectTick > GameTimeController.getGameTicks();
	}
	
	public void setIsSelectingTarget(final int time)
	{
		targetSelectTick = GameTimeController.getGameTicks() + time;
	}
	
	private int targetSelectTick = 0;
	
	/**
	 * Check if the active L2Skill can be casted.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Check if the skill isn't toggle and is offensive</li>
	 * <li>Check if the target is in the skill cast range</li>
	 * <li>Check if the skill is Spoil type and if the target isn't already spoiled</li>
	 * <li>Check if the caster owns enought consummed Item, enough HP and MP to cast the skill</li>
	 * <li>Check if the caster isn't sitting</li>
	 * <li>Check if all skills are enabled and this skill is enabled</li><BR>
	 * <BR>
	 * <li>Check if the caster own the weapon needed</li><BR>
	 * <BR>
	 * <li>Check if the skill is active</li><BR>
	 * <BR>
	 * <li>Check if all casting conditions are completed</li><BR>
	 * <BR>
	 * <li>Notify the AI with AI_INTENTION_CAST and target</li><BR>
	 * <BR>
	 *
	 * @param skill
	 *            The L2Skill to use
	 * @param forceUse
	 *            used to force ATTACK on players
	 * @param dontMove
	 *            used to prevent movement, if not in range
	 */
	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		// Check if the skill is active
		if (skill.isPassive()/* || skill.isChance() */)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (_inEventHG && NewHuntingGrounds._started)
		{
			this.sendMessage("You can't use skills in this event");
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (skill.isToggle())
		{
			final L2Effect e = getFirstEffect(skill);
			if (e != null)
			{
				e.exit();
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		if (isSkillDisabledDueToTransformation(skill.getId()))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (skill.isTeleTypeSkill2() && isMovementDisabled())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (isInvisible())
		{
			if (skill.getSkillType() == L2SkillType.SWITCH && isInsideZone(L2Character.ZONE_SIEGE))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("Cannot use switch when invisible and in a siege zone");
				return;
			}
		}
		if (!isGM())
		{
			if (skill.getTransformId() > 0)
			{
				if (isMoving() || !getAI().getIntention().equals(CtrlIntention.AI_INTENTION_IDLE))
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					sendMessage("Cannot transform unless you're standing");
					return;
				}
				switch (skill.getId())
				{
					case 1520:
					case 1521:
					case 1522:
					case 810:
					case 811:
					case 812:
					case 813:
						/* case 538: */
						break;
					default:
						if (isInHuntersVillage() || isInOrcVillage())
						{
							sendPacket(ActionFailed.STATIC_PACKET);
							sendMessage("Cannot use transformation in this zone");
							return;
						}
						else if (isInHellbound())
						{
							sendPacket(ActionFailed.STATIC_PACKET);
							sendMessage("Transformations are not allowed in Hellbound");
							return;
						}
				}
			}
			else if (skill.getSkillType() == L2SkillType.TRANSFORMDISPEL)
			{
				if (isMoving() || !getAI().getIntention().equals(CtrlIntention.AI_INTENTION_IDLE))
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					sendMessage("Cannot untransform unless you're standing");
					return;
				}
			}
		}
		if (skill.getId() == 538) // final form
		{
			if (_inEventDM && (DM._started || NewDM._started))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("Disabled in DM");
				return;
			}
		}
		else if (skill.getId() == 106 || skill.getId() == 296 || skill.getId() == 525 || skill.getId() == 922) // invisible skills
		{
			if (_inEventCTF && CTF._started)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("Cannot go into invisible mode while in CTF");
				return;
			}
			if (_inEventCTF && NewCTF._started)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("Cannot go into invisible mode while in CTF");
				return;
			}
			/*
			 * if (_inEventFOS && FOS._started)
			 * {
			 * sendPacket(ActionFailed.STATIC_PACKET);
			 * sendMessage("Cannot go into invisible mode while in a siege event");
			 * return;
			 * }
			 */
			if (isCombatFlagEquipped())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("Cannot go into invisible mode while holding a combat flag");
				return;
			}
		}
		else if (skill.getId() == 8 || skill.getId() == 50 || skill.getId() == 345 || skill.getId() == 346) // sonic/force focus / sonic/force rage
		{
			if (getCharges() >= calcStat(Stats.CHARGE_MAX_ADD, skill.getMaxCharges(), null, null))
			{
				sendPacket(new SystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED));
				return;
			}
		}
		else if (skill.getId() == 502 || skill.getId() == 625 || skill.getId() == 939) // life to soul / soul gathering // soul rage
		{
			if (getSouls() >= calcStat(Stats.SOUL_MAX, 40, null, null))
			{
				sendPacket(new SystemMessage(SystemMessageId.SOUL_CANNOT_BE_INCREASED_ANYMORE));
				return;
			}
		}
		else if (skill.getId() == 176 || skill.getId() == 139 || skill.getId() == 406 || skill.getId() == 292 || skill.getId() == 420) // frenzy/guts/bison/zealot/angelic icon
		{
			if (skill.getId() == 420 && _inEventDM && DM._started)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage(skill.getName() + " cannot be used in the DM event");
				return;
			}
			/*
			 * boolean canuse = false;
			 * if (isInCombat() && (getPvpFlag() != 0 || isInsideZone(L2Character.ZONE_PVP) || isInDuel()))
			 * canuse = true;
			 * if (!canuse)
			 * {
			 * sendPacket(ActionFailed.STATIC_PACKET);
			 * sendMessage(skill.getName()+" cannot be used unless you are in combat mode");
			 * return;
			 * }
			 */
		}
		/*
		 * else if (skill.getSkillType() == L2SkillType.SUMMON_TRAP)
		 * {
		 * final int lvl = getLevel();
		 * switch (skill.getId())
		 * {
		 * case 514:
		 * if (skill.getLevel() > 2 && lvl < 87)
		 * {
		 * sendPacket(ActionFailed.STATIC_PACKET);
		 * sendMessage("Fire trap becomes useable at level 87");
		 * return;
		 * }
		 * break;
		 * case 518:
		 * if (lvl < 86)
		 * {
		 * sendPacket(ActionFailed.STATIC_PACKET);
		 * sendMessage("Binding trap becomes useable at level 86");
		 * return;
		 * }
		 * break;
		 * case 836:
		 * if (lvl < 88)
		 * {
		 * sendPacket(ActionFailed.STATIC_PACKET);
		 * sendMessage("Oblivion trap becomes useable at level 88");
		 * return;
		 * }
		 * break;
		 * case 516:
		 * if (lvl < 86)
		 * {
		 * sendPacket(ActionFailed.STATIC_PACKET);
		 * sendMessage("Slow trap becomes useable at level 86");
		 * return;
		 * }
		 * break;
		 * }
		 * }
		 */
		else if (_inEventCTF && CTF._started)
		{
			if (skill.isPvpTransformSkill())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("Transform skills cannot be used in the CTF");
				return;
			}
			else if (skill.getSkillType() == L2SkillType.RESURRECT || skill.getId() == 438 || skill.getId() == 1410)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("Resurrection skills cannot be used in the CTF");
				return;
			}
		}
		else if (skill.getSkillType() == L2SkillType.RESURRECT || skill.getId() == 438 || skill.getId() == 1410)
		{
			if (!canResInCurrentInstance())
			{
				sendMessage("Your group has already used up all " + getCurrentInstance().getResLimit() + " of your allowed ressurections in this instance");
				return;
			}
		}
		if (isInOlympiadMode())
		{
			if (skill.isDisabledInOlympiad() || skill.isHeroSkill() || skill.getSkillType() == L2SkillType.RESURRECT)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
				return;
			}
			/*
			 * if (skill.getSkillType() == L2SkillType.SUMMON && !skill.getName().contains("Cubic") && !isSummoner())
			 * {
			 * sendPacket(ActionFailed.STATIC_PACKET);
			 * sendPacket(new SystemMessage(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			 * return;
			 * }
			 */
		}
		else if (isInKoreanZone())
		{
			if (skill.isDisabledInKorean())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("This skill is not allowed in this Custom Event");
				return;
			}
			if (skill.isHeroSkill() && LunaVariables.getInstance().getKoreanHeroSkillsPrevented())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("This skill is not allowed in this Custom Event");
				return;
			}
			if (LunaVariables.getInstance().getKoreanMarriageSkillsPrevented() && (skill.getId() >= 12000 && skill.getId() <= 12005))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("This skill is not allowed in this Custom Event");
				return;
			}
			if (LunaVariables.getInstance().getKoreanRessurectionSkillsPrevented() && skill.getSkillType() == L2SkillType.RESURRECT)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("This skill is not allowed in this Custom Event");
				return;
			}
			/*
			 * if (skill.getSkillType() == L2SkillType.SUMMON && !skill.getName().contains("Cubic") && !isSummoner())
			 * {
			 * sendPacket(ActionFailed.STATIC_PACKET);
			 * sendPacket(new SystemMessage(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			 * return;
			 * }
			 */
		}
		else if (skill.isHeroSkill())
		{
			if (!isGM() && ((_inEventDM && DM._started) || isInSgradeZone()))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("Hero skills are forbidden here");
				return;
			}
		}
		if (skill.getSkillType() == L2SkillType.FUSION)
		{
			if (getTarget() != null && getTarget() instanceof L2MonsterInstance)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return;
			}
		}
		if (skill.getId() == 10010) // dimensional portal
		{
			if (getFirstEffectById(10010) != null)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendMessage("You wanna tear a hole in the universe?");
				return;
			}
			else
			{
				if (_inEventCTF && CTF._started)
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					sendMessage("CTF resists your opening of a dimensional portal");
					return;
				}
				else if (_inEventFOS && (FOS._started || NewFOS._started))
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					sendMessage("Siege event resists your opening of a dimensional portal");
					return;
				}
				else if (_isTheVIP && VIP._started)
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					sendMessage("Your face resists your opening of a dimensional portal");
					return;
				}
				else if (isCombatFlagEquipped())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					sendMessage("Flag resists your opening of a dimensional portal");
					return;
				}
			}
		}
		if (!skill.isSimultaneous())
		{
			// ************************************* Check Casting in Progress *******************************************
			// If a skill is currently being used, queue this one if this is not the same
			if (isCastingNow())
			{
				final SkillDat currentSkill = getCurrentSkill();
				// Check if new skill different from current skill in progress
				if (currentSkill != null)
				{
					if (currentSkill.getSkill().getSkillType() == L2SkillType.TRANSFORMDISPEL)
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					else if (skill.getId() == currentSkill.getSkillId())
					{
						// if (skill.getReuseDelay(this) > 1000)
						// {
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
						// }
					}
					if (Config.DEBUG && getQueuedSkill() != null)
						_log.info(getQueuedSkill().getSkill().getName() + " is already queued for " + getName() + ".");
					// Create a new SkillDat object and queue it in the player _queuedSkill
					setQueuedSkill(skill, forceUse, dontMove);
				}
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			setIsCastingNow(true);
			if (!checkUseMagicConditions(skill, forceUse, dontMove))
			{
				setIsCastingNow(false);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (!checkDoCastConditions(skill))
			{
				setIsCastingNow(false);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (getCursedWeaponEquippedId() != 0)
				forceUse = true;
			setCurrentSkill(skill, forceUse, dontMove);
			if (getQueuedSkill() != null) // wiping out previous values, after casting has been aborted
				setQueuedSkill(null, false, false);
			// Check if the target is correct and Notify the AI with AI_INTENTION_CAST and target
			L2Character target = null;
			switch (skill.getTargetType(this))
			{
				case TARGET_SELF:
				case TARGET_SELF_AND_PET:
				case TARGET_PARTY:
				case TARGET_CLAN:
				case TARGET_ALLY:
				case TARGET_GROUND:
					target = this;
					break;
				default:
					// Get the first target of the list
					target = skill.getFirstOfTargetList(this);
					break;
			}
			if (!(target instanceof L2Playable))
			{
				if (skill.isPositive() && !skill.isNeutral() && !isGM())
					target = null;
			}
			if (target != null)
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
			}
			else
			{
				setIsCastingNow(false);
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			}
		}
		else
		{
			if (!checkUseMagicConditions(skill, forceUse, dontMove))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (!checkDoCastConditions(skill))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			doSimultaneousCast(skill);
		}
	}
	
	public boolean checkUseMagicConditions(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		final L2SkillType sklType = skill.getSkillType();
		// ************************************* Check Player State *******************************************
		// Abnormal effects(ex : Stun, Sleep...) are checked in L2Character useMagic()
		if (isOutOfControl() || isDead())
			return false;
		if (isFishing() && (sklType != L2SkillType.PUMPING && sklType != L2SkillType.REELING && sklType != L2SkillType.FISHING))
		{
			// Only fishing skills are available
			sendPacket(new SystemMessage(SystemMessageId.ONLY_FISHING_SKILLS_NOW));
			return false;
		}
		if (inObserverMode())
		{
			sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
			return false;
		}
		// Check if the caster is sitting
		if (isSitting() && !skill.isPotion())
		{
			// Send a System Message to the caster
			sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			return false;
		}
		// Check if the skill type is TOGGLE
		if (skill.isToggle())
		{
			// Get effects of the skill
			L2Effect effect = getFirstEffect(skill);
			if (effect != null)
			{
				effect.exit();
				return false;
			}
		}
		// Check if the player uses "Fake Death" skill
		// Note: do not check this before TOGGLE reset
		if (isFakeDeath())
			return false;
		if (skill.getSkillType() == L2SkillType.SUMMON) // custom edit
		{
			if (!skill.getName().contains("Cubic"))
			{
				/*
				 * if (isInOrcVillage())
				 * {
				 * sendMessage("Servitors are not allowed in Orc Village.");
				 * return false;
				 * }
				 */
				if (isNecroClass())
				{
					if (isInFunEvent())
					{
						sendMessage("You cannot summon servitors while joined in an event.");
						return false;
					}
					if (TvT._joining && isInsideRadius(TvT._npcX, TvT._npcY, 4000, true))
					{
						sendMessage("You cannot summon servitors here while an event is about to start.");
						return false;
					}
					if (FOS._joining && isInsideRadius(FOS._npcX, FOS._npcY, 4000, true))
					{
						sendMessage("You cannot summon servitors here while an event is about to start.");
						return false;
					}
					if (VIP._joining && isInsideRadius(VIP._joinX, VIP._joinY, 4000, true))
					{
						sendMessage("You cannot summon servitors here while an event is about to start.");
						return false;
					}
					if (CTF._joining && isInsideRadius(CTF._npcX, CTF._npcY, 4000, true))
					{
						sendMessage("You cannot summon servitors here while an event is about to start.");
						return false;
					}
					if (DM._joining && isInsideRadius(DM._npcX, DM._npcY, 4000, true))
					{
						sendMessage("You cannot summon servitors here while an event is about to start.");
						return false;
					}
				}
			}
		}
		// ************************************* Check Target *******************************************
		// Create and set a L2Object containing the target of the skill
		L2Object target = null;
		SkillTargetType sklTargetType = skill.getTargetType(this);
		Point3D worldPosition = getCurrentSkillWorldPosition();
		if (sklTargetType == SkillTargetType.TARGET_GROUND && worldPosition == null)
		{
			_log.info("WorldPosition is null for skill: " + skill.getName() + ", player: " + getName() + ".");
			return false;
		}
		switch (sklTargetType)
		{
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_CORPSE_ALLY:
			case TARGET_CORPSE_CLAN:
			case TARGET_ALL:
				target = skill.getFirstOfTargetList(this);
				break;
			case TARGET_SELF:
			case TARGET_SELF_AND_PET:
			case TARGET_PARTY:
			case TARGET_ALLY:
			case TARGET_CLAN:
			case TARGET_GROUND:
				target = this;
				break;
			case TARGET_PET:
			case TARGET_SUMMON:
				target = getPet();
				break;
			case TARGET_COUPLE:
				if (isThisCharacterMarried())
				{
					final L2PcInstance couple = (L2PcInstance) L2World.getInstance().findObject(getPartnerId());
					if (couple != null && couple.isOnline() == 1 && Util.checkIfInRange(1600, this, couple, false)/* && CanAttackDueToInEvent(couple) */)
						target = couple;
					else
						return false;
				}
				else
					return false;
				break;
			case TARGET_SELF_AND_COUPLE:
				if (isThisCharacterMarried())
				{
					L2Character[] targetList = skill.getTargetList(this);
					if (targetList == null)
						return false;
					target = targetList[1];
				}
				else
					return false;
				break;
			default:
				target = getTarget();
				break;
		}
		// Check the validity of the target
		if (target == null)
		{
			if (sklTargetType == SkillTargetType.TARGET_ONE || sklTargetType == SkillTargetType.TARGET_PARTY_MEMBER || sklTargetType == SkillTargetType.TARGET_ONE_AND_PET)
			{
				if (skill.isPositive() && sklType != L2SkillType.RESURRECT)
					target = this;
			}
			if (target == null)
			{
				sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
				return false;
			}
		}
		else
		{
			if (target instanceof L2Playable && !isAttackable((L2Playable) target))
				return false;
		}
		// skills can be used on Walls and Doors only during siege
		if (target instanceof L2DoorInstance)
		{
			if (!((L2DoorInstance) target).isAutoAttackable(this))
				return false;
		}
		// ************************************* Check skill availability *******************************************
		// Check if it's ok to summon
		// siege golem (13), Wild Hog Cannon (299), Swoop Cannon (448)
		if ((skill.getId() == 13 || skill.getId() == 299 || skill.getId() == 448) && ((!SiegeManager.getInstance().checkIfOkToSummon(this, false) && !FortSiegeManager.getInstance().checkIfOkToSummon(this, false)) || (SevenSigns.getInstance().CheckSummonConditions(this))))
			return false;
		// Check if this skill is enabled (ex : reuse time)
		if (isSkillDisabled(skill.getId()))
		{
			if (!skill.isPotion())
			{
				SystemMessage sm = null;
				FastMap<Integer, TimeStamp> timeStamp = getReuseTimeStamp();
				if (timeStamp != null && timeStamp.containsKey(skill.getId()))
				{
					int remainingTime = (int) (_reuseTimeStamps.get(skill.getId()).getRemaining() / 1000);
					int hours = remainingTime / 3600;
					int minutes = (remainingTime % 3600) / 60;
					int seconds = (remainingTime % 60);
					if (hours > 0)
					{
						sm = new SystemMessage(SystemMessageId.S2_HOURS_S3_MINUTES_S4_SECONDS_REMAINING_FOR_REUSE_S1);
						sm.addSkillName(skill);
						sm.addNumber(hours);
						sm.addNumber(minutes);
					}
					else if (minutes > 0)
					{
						sm = new SystemMessage(SystemMessageId.S2_MINUTES_S3_SECONDS_REMAINING_FOR_REUSE_S1);
						sm.addSkillName(skill);
						sm.addNumber(minutes);
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.S2_SECONDS_REMAINING_FOR_REUSE_S1);
						sm.addSkillName(skill);
					}
					sm.addNumber(seconds);
				}
				else
				{
					/*
					 * sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
					 * sm.addSkillName(skill);
					 */
					return false;
				}
				sendPacket(sm);
			}
			return false;
		}
		// ************************************* Check Consumables *******************************************
		// Check if spell consumes a Soul
		if (skill.getSoulConsumeCount() > 0)
		{
			if (getSouls() < skill.getSoulConsumeCount())
			{
				sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NOT_ENOUGH_SOUL));
				return false;
			}
		}
		// ************************************* Check casting conditions *******************************************
		// Check if all casting conditions are completed
		if (!skill.checkCondition(this, target, false))
			return false;
		// ************************************* Check Skill Type *******************************************
		// Check if this is offensive magic skill
		if (skill.isOffensive())
		{
			if (target instanceof L2Playable && !isGM())
			{
				if (target.getActingPlayer().getInEventPeaceZone())
				{
					sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
					return false;
				}
			}
			if (target instanceof L2Playable && !isGM() && target != null)
			{
				if (target.getActingPlayer().eventSitForced && target.getActingPlayer().isInKoreanEvent())
				{
					sendMessage("Focus on your opponent!");
					return false;
				}
			}
			if ((isInsidePeaceZone(this, target)) && !getAccessLevel().allowPeaceAttack())
			{
				// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				return false;
			}
			if (isInOlympiadMode() && !isOlympiadStart())
				return false;
			// Check if the target is attackable
			if (!target.isAttackable() && !getAccessLevel().allowPeaceAttack())
				return false;
			if (!target.isAutoAttackable(this))
			{
				/*
				 * if (target == this)
				 * return false;
				 */
				if (isInFunEvent() || target.isInFunEvent())
					return false;
				switch (sklTargetType)
				{
					case TARGET_AURA:
					case TARGET_FRONT_AURA:
					case TARGET_BEHIND_AURA:
					case TARGET_CLAN:
					case TARGET_ALLY:
					case TARGET_PARTY:
					case TARGET_SELF:
					case TARGET_SELF_AND_PET:
					case TARGET_GROUND:
						break;
					default:
						if (!forceUse)
							return false;
				}
			}
			// Check if the target is in the skill cast range
			if (dontMove)
			{
				// Calculate the distance between the L2PcInstance and the target
				if (sklTargetType == SkillTargetType.TARGET_GROUND)
				{
					if (!isInsideRadius(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), (int) (skill.getCastRange(this) + getCollisionRadius()), false, false))
					{
						// Send a System Message to the caster
						sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));
						return false;
					}
				}
				else if (skill.getCastRange(this) > 0 && !isInsideRadius(target, (int) (skill.getCastRange(this) + getCollisionRadius()), false, false))
				{
					// Send a System Message to the caster
					sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));
					return false;
				}
			}
		}
		else if (!skill.isNeutral() && this != target.getActingPlayer())
		{
			// beneficial spells
			if (target.isInFunEvent())
			{
				if (isInFunEvent())
				{
					if (target instanceof L2Character && ((L2Character) target).CanAttackDueToInEvent(this))
					{
						sendMessage("You can't help the opposing team");
						return false;
					}
				}
				else
					return false;
			}
			else if (isInFunEvent() && target instanceof L2Playable)
			{
				sendMessage("You can't interact outside of the event");
				return false;
			}
		}
		// Check if the skill is defensive
		if (!skill.isOffensive() && target instanceof L2MonsterInstance && !forceUse && !skill.isNeutral())
		{
			// check if the target is a monster and if force attack is set.. if not then we don't want to cast.
			switch (sklTargetType)
			{
				case TARGET_PET:
				case TARGET_SUMMON:
				case TARGET_AURA:
				case TARGET_FRONT_AURA:
				case TARGET_BEHIND_AURA:
				case TARGET_CLAN:
				case TARGET_SELF:
				case TARGET_SELF_AND_PET:
				case TARGET_PARTY:
				case TARGET_ALLY:
				case TARGET_CORPSE_MOB:
				case TARGET_AREA_CORPSE_MOB:
				case TARGET_GROUND:
					break;
				default:
				{
					switch (sklType)
					{
						case BEAST_FEED:
						case DELUXE_KEY_UNLOCK:
						case UNLOCK:
							break;
						default:
							return false;
					}
					break;
				}
			}
		}
		// Check if the skill is Spoil type and if the target isn't already spoiled
		if (sklType == L2SkillType.SPOIL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return false;
			}
		}
		// Check if the skill is Sweep type and if conditions not apply
		if (sklType == L2SkillType.SWEEP && target instanceof L2Attackable)
		{
			int spoilerId = ((L2Attackable) target).getIsSpoiledBy();
			if (((L2Attackable) target).isDead())
			{
				if (!((L2Attackable) target).isSpoil())
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED));
					return false;
				}
				if (getObjectId() != spoilerId && !isInLooterParty(spoilerId))
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.SWEEP_NOT_ALLOWED));
					return false;
				}
			}
		}
		// Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
		if (sklType == L2SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return false;
			}
		}
		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (sklTargetType)
		{
			case TARGET_PARTY:
			case TARGET_ALLY: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_CLAN: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_GROUND:
			case TARGET_PET:
			case TARGET_SUMMON:
			case TARGET_SELF:
			case TARGET_SELF_AND_PET:
				break;
			default:
				if (!checkPvpSkill(target, skill, forceUse))
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return false;
				}
		}
		if ((sklTargetType == SkillTargetType.TARGET_HOLY && !checkFOS() && !checkIfOkToCastSealOfRule(CastleManager.getInstance().getCastle(this), false))
		/* || (sklTargetType == SkillTargetType.TARGET_FLAGPOLE && !checkIfOkToCastFlagDisplay(FortManager.getInstance().getFort(this), true, skill, getTarget())) */
		|| (sklType == L2SkillType.SIEGEFLAG && !L2SkillSiegeFlag.checkIfOkToPlaceFlag(this, false)) || (sklType == L2SkillType.STRSIEGEASSAULT && !checkIfOkToUseStriderSiegeAssault(false)))
		{
			sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return false;
		}
		final int skillCastRange = skill.getCastRange(this);
		// GeoData Los Check here
		if (skillCastRange > 0)
		{
			if (sklTargetType == SkillTargetType.TARGET_GROUND)
			{
				if (!GeoData.getInstance().canSeeTarget(this, worldPosition))
				{
					sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
					return false;
				}
			}
			else if (!GeoData.getInstance().canSeeTarget(this, target))
			{
				sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
				return false;
			}
			// else if (target != this && target instanceof L2ArtefactInstance && skill.getId() == 246)
			// {
			// final int zDifferential = getZ() - target.getZ();
			// if (zDifferential > 0)
			// {
			// if (zDifferential > skillCastRange * 1.75)
			// {
			// sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			// return false;
			// }
			// }
			// else if (zDifferential < 0)
			// {
			// if (zDifferential * -1 > skillCastRange)
			// {
			// sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			// return false;
			// }
			// }
			// }
			/*
			 * else if (target != this)
			 * {
			 * final int zDifferential = getZ() - target.getZ();
			 * if (zDifferential > 0)
			 * {
			 * if (zDifferential > skillCastRange * 1.75)
			 * {
			 * sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			 * return false;
			 * }
			 * }
			 * else if (zDifferential < 0)
			 * {
			 * if (zDifferential * -1 > skillCastRange)
			 * {
			 * sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			 * return false;
			 * }
			 * }
			 * }
			 */
		}
		// finally, after passing all conditions
		return true;
	}
	
	public boolean checkIfOkToUseStriderSiegeAssault(boolean isCheckOnly)
	{
		Castle castle = CastleManager.getInstance().getCastle(this);
		Fort fort = FortManager.getInstance().getFort(this);
		if ((castle == null) && (fort == null))
			return false;
		if (castle != null)
			return checkIfOkToUseStriderSiegeAssault(castle, isCheckOnly);
		else
			return checkIfOkToUseStriderSiegeAssault(fort, isCheckOnly);
	}
	
	public boolean checkFOS()
	{
		boolean ok = false;
		if (FOS._started)
		{
			ok = FOS.checkIfOkToCastSealOfRule(this);
		}
		if (NewFOS._started)
		{
			ok = NewFOS.checkIfOkToCastSealOfRule(this);
		}
		return ok;
	}
	
	public boolean checkIfOkToUseStriderSiegeAssault(Castle castle, boolean isCheckOnly)
	{
		String text = "";
		if (castle == null || castle.getCastleId() <= 0)
			text = "You must be on castle ground to use strider siege assault";
		else if (!castle.getSiege().getIsInProgress())
			text = "You can only use strider siege assault during a siege.";
		else if (!(getTarget() instanceof L2DoorInstance))
			text = "You can only use strider siege assault on doors and walls.";
		else if (!isRidingStrider())
			text = "You can only use strider siege assault when on strider.";
		else
			return true;
		if (!isCheckOnly)
			sendMessage(text);
		return false;
	}
	
	public boolean checkIfOkToUseStriderSiegeAssault(Fort fort, boolean isCheckOnly)
	{
		String text = "";
		if (fort == null || fort.getFortId() <= 0)
			text = "You must be on fort ground to use strider siege assault";
		else if (!fort.getSiege().getIsInProgress())
			text = "You can only use strider siege assault during a siege.";
		else if (!(getTarget() instanceof L2DoorInstance))
			text = "You can only use strider siege assault on doors and walls.";
		else if (!isRidingStrider())
			text = "You can only use strider siege assault when on strider.";
		else
			return true;
		if (!isCheckOnly)
			sendMessage(text);
		return false;
	}
	
	public boolean checkIfOkToCastSealOfRule(Castle castle, boolean isCheckOnly)
	{
		String text = "";
		if (castle == null || castle.getCastleId() <= 0)
			text = "You must be on castle ground to use this skill";
		else if (!(getTarget() instanceof L2ArtefactInstance))
			text = "You can only use this skill on an artifact";
		else if (!castle.getSiege().getIsInProgress())
			text = "You can only use this skill during a siege.";
		else if (!Util.checkIfInRange(200, this, getTarget(), true))
			text = "You are not in range of the artifact.";
		else if (castle.getSiege().getAttackerClan(getClan()) == null)
			text = "You must be an attacker to use this skill";
		else
		{
			if (!isCheckOnly)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.OPPONENT_STARTED_ENGRAVING);
				castle.getSiege().announceToPlayer(sm, false);
			}
			return true;
		}
		if (!isCheckOnly)
			sendMessage(text);
		return false;
	}
	
	public boolean checkIfOkToCastFlagDisplay(Fort fort, L2Skill skill, L2Object target)
	{
		if (target == null || !(target instanceof L2StaticObjectInstance) || ((L2StaticObjectInstance) target).getType() != 3)
		{
			return false;
		}
		SystemMessage sm;
		if (fort == null || fort.getFortId() <= 0)
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!Util.checkIfInRange(200, this, getTarget(), true))
		{
			sm = new SystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
		}
		else if (fort.getSiege().getAttackerClan(getClan()) == null)
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else
		{
			return true;
		}
		sendPacket(sm);
		return false;
	}
	
	public boolean isInLooterParty(int LooterId)
	{
		L2PcInstance looter = (L2PcInstance) L2World.getInstance().findObject(LooterId);
		// if L2PcInstance is in a CommandChannel
		if (isInParty() && getParty().isInCommandChannel() && looter != null)
			return getParty().getCommandChannel().getMembers().contains(looter);
		if (isInParty() && looter != null)
			return getParty().getPartyMembers().contains(looter);
		return false;
	}
	
	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * 
	 * @param target
	 *            L2Object instance containing the target
	 * @param skill
	 *            L2Skill instance with the skill being casted
	 * @param srcIsSummon
	 *            is L2Summon - caster?
	 * @return False if the skill is a pvpSkill and target is not a valid pvp target
	 */
	public boolean checkPvpSkill(L2Object obj, L2Skill skill, boolean CtrlPressed)
	{
		if (isGM())
			return true;
		if (obj instanceof L2Playable)
		{
			final L2PcInstance target = obj.getActingPlayer();
			if (target == null)
				return false;
			if (this != target && (!target.isVisible() || (target.isInvisible() && target.isGM())))
				return false;
			if (target.getDuelState() == Duel.DUELSTATE_DUELLING || getDuelState() == Duel.DUELSTATE_DUELLING)
			{
				if (target.getDuelId() == getDuelId())
					return true;
				else
					return false;
			}
			if (this.isInHuntersVillage() && target.isInHuntersVillage())
			{
				return true;
			}
			if (this.isInOrcVillage() && target.isInOrcVillage())
			{
				return true;
			}
			if (this.isInPI() && target.isInPI())
			{
				return true;
			}
			if (skill.isOffensive())
			{
				if (isInFunEvent())
				{
					if (target.isInFunEvent())
					{
						if (CanAttackDueToInEvent(target))
							return true;
						else
							return false;
					}
					else
						return false;
				}
				else if (target.isInFunEvent())
				{
					return false;
				}
				if (isInsideZone(ZONE_PEACE) || target.isInsideZone(ZONE_PEACE))
					return false;
			}
			if (isInsideZone(ZONE_PVP) && ((L2Playable) obj).isInsideZone(ZONE_PVP))
				return true;
			// check for PC->PC Pvp status
			if (obj != this)
			{
				if (getCursedWeaponEquippedId() != 0 || target.getCursedWeaponEquippedId() != 0)
					return true;
				if (skill.isPvpSkill()) // pvp skill
				{
					if (isInClanwarWith(target))
						return true;
					if (target.getPvpFlagLasts() < System.currentTimeMillis() && target.getKarma() == 0)
						return false;
				}
				else if (!CtrlPressed && skill.isOffensive())
				{
					if (canPKdueToOnesidedClanwar(target))
						return true;
					if (target.getPvpFlagLasts() < System.currentTimeMillis() && target.getKarma() == 0)
						return false;
				}
			}
		}
		return true;
	}
	
	public final boolean checkAOEPvPSkill(L2PcInstance target, L2Skill skill)
	{
		if (target != null)
		{
			if (!isGM())
			{
				if (!target.isVisible() || (target.isInvisible() && target.isGM()))
					return false;
				if (target.isAlikeDead())
					return false;
				if (isInFunEvent())
				{
					if (target.isInFunEvent())
					{
						if (CanAttackDueToInEvent(target))
							return true;
						else
							return false;
					}
					else
						return false;
				}
				else if (target.isInFunEvent())
				{
					return false;
				}
				if (target.getDuelState() == Duel.DUELSTATE_DUELLING || getDuelState() == Duel.DUELSTATE_DUELLING)
				{
					if (target.getDuelId() == getDuelId())
						return true;
					else
						return false;
				}
				if (isInOlympiadMode())
				{
					if (target.isInOlympiadMode())
						return true;
					return false;
				}
				else if (target.isInOlympiadMode())
					return false;
				if (isInsideZone(ZONE_PEACE) || target.isInsideZone(ZONE_PEACE))
					return false;
				if (getCursedWeaponEquippedId() != 0 || target.getCursedWeaponEquippedId() != 0)
					return true;
				if (getParty() != null && target.getParty() != null && getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID())
					return false;
				if (!isDisguised() && !target.isDisguised() && getAllyId() != 0 && getAllyId() == target.getAllyId())
					return false;
				if (!isDisguised() && !target.isDisguised() && getClanId() != 0 && getClanId() == target.getClanId())
					return false;
				if (getTarget() != null && getTarget() instanceof L2Attackable)
					return false;
				// check for PC->PC Pvp status
				if (target != null && // target not null and
				target != this && // target is not self and
				!(isInDuel() && target.getDuelId() == getDuelId()) && // self is not in a
				// duel and attacking
				// opponent
				!isInsideZone(ZONE_PVP) && // Pc is not in PvP zone
				!target.isInsideZone(ZONE_PVP) // target is not in PvP zone
				)
				{
					if (skill.isPvpSkill()) // pvp skill
					{
						if (isInClanwarWith(target))
							return true; // in clan war player can attack whites even with sleep
						// etc.
						if (target.getPvpFlagLasts() < System.currentTimeMillis() + 10 && // target's
						target.getKarma() == 0 // target has no karma
						)
							return false;
					}
					else
					{
						if (skill.isOffensive())
						{
							if (isInClanwarWith(target))
								return true; // non-pvp (sleep, stun) and direct damaging skills can
							// be directly casted.
							if (target.getPvpFlagLasts() < System.currentTimeMillis() + 10 && // target's
							target.getKarma() == 0 // target has no karma
							)
								return false;
						}
					}
				}
				return true;
			}
			else if (getAccessLevel().getLevel() <= target.getAccessLevel().getLevel())
				return false;
			return true;
		}
		return false;
	}
	
	/**
	 * Return True if the L2PcInstance is a Mage.<BR>
	 * <BR>
	 */
	public boolean isMageClass()
	{
		return getClassId().isMage();
	}
	
	public boolean isMounted()
	{
		return _mountType > 0;
	}
	
	/**
	 * Set the type of Pet mounted (0 : none, 1 : Stridder, 2 : Wyvern) and send a Server->Client packet InventoryUpdate to the L2PcInstance.<BR>
	 * <BR>
	 */
	public boolean checkLandingState()
	{
		// Check if char is in a no landing zone
		if (isInsideZone(ZONE_NOLANDING))
			return true;
		else
		// if this is a castle that is currently being sieged, and the rider is NOT a castle owner
		// he cannot land.
		// castle owner is the leader of the clan that owns the castle where the pc is
		if (isInsideZone(ZONE_SIEGE) && !(getClan() != null && CastleManager.getInstance().getCastle(this) == CastleManager.getInstance().getCastleByOwner(getClan()) && this == getClan().getLeader().getPlayerInstance()))
			return true;
		return false;
	}
	
	// returns false if the change of mount type fails.
	public boolean setMount(int npcId, int npcLevel, int mountType)
	{
		switch (mountType)
		{
			case 0:
				setIsFlying(false);
				/*
				 * not used any more
				 * setIsRidingFenrirWolf(false);
				 * setIsRidingWFenrirWolf(false);
				 * setIsRidingGreatSnowWolf(false);
				 */
				setIsRidingStrider(false);
				break; // Dismounted
			case 1:
				setIsRidingStrider(true);
				if (isNoble())
				{
					L2Skill striderAssaultSkill = SkillTable.getInstance().getInfo(325, 1);
					addSkill(striderAssaultSkill, false); // not saved to DB
				}
				break;
			case 2:
				setIsFlying(true);
				break; // Flying Wyvern
			case 3:
				/*
				 * not used any more
				 * switch (npcId)
				 * {
				 * case 16041:
				 * setIsRidingFenrirWolf(true);
				 * break;
				 * case 16042:
				 * setIsRidingWFenrirWolf(true);
				 * break;
				 * case 16037:
				 * setIsRidingGreatSnowWolf(true);
				 * break;
				 * }
				 */
				break;
		}
		_mountType = mountType;
		_mountNpcId = npcId;
		_mountLevel = npcLevel;
		return true;
	}
	
	/**
	 * Return the type of Pet mounted (0 : none, 1 : Strider, 2 : Wyvern, 3: Wolf).<BR>
	 * <BR>
	 */
	public int getMountType()
	{
		return _mountType;
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>.
	 * In order to inform other players of this L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet.
	 * Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR>
	 * <BR>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		broadcastUserInfo();
	}
	
	/**
	 * Disable the Inventory and create a new task to enable it after 1.5s.<BR>
	 * <BR>
	 */
	public void tempInventoryDisable()
	{
		_inventoryDisable = true;
		ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnable(), 1500);
	}
	
	/**
	 * Return True if the Inventory is disabled.<BR>
	 * <BR>
	 */
	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}
	
	class InventoryEnable implements Runnable
	{
		public void run()
		{
			_inventoryDisable = false;
		}
	}
	
	public Map<Integer, L2CubicInstance> getCubics()
	{
		return _cubics;
	}
	
	/**
	 * Add a L2CubicInstance to the L2PcInstance _cubics.<BR>
	 * <BR>
	 */
	public void addCubic(int id, int level, double matk, int activationtime, int activationchance, int totalLifetime, boolean givenByOther)
	{
		if (Config.DEBUG)
			_log.info("L2PcInstance(" + getName() + "): addCubic(" + id + "|" + level + "|" + matk + ")");
		L2CubicInstance cubic = new L2CubicInstance(this, id, level, (int) matk, activationtime, activationchance, totalLifetime, givenByOther);
		_cubics.put(id, cubic);
	}
	
	/**
	 * Remove a L2CubicInstance from the L2PcInstance _cubics.<BR>
	 * <BR>
	 */
	public void delCubic(int id)
	{
		_cubics.remove(id);
	}
	
	/**
	 * Return the L2CubicInstance corresponding to the Identifier of the L2PcInstance _cubics.<BR>
	 * <BR>
	 */
	public L2CubicInstance getCubic(int id)
	{
		return _cubics.get(id);
	}
	
	@Override
	public String toString()
	{
		return "player " + getName();
	}
	
	public float getWeaponSizeMultiplier()
	{
		final L2ItemInstance wpn = getActiveWeaponInstance();
		if (wpn == null || wpn.getItem().getSize() == 0f)
			return 1f;
		return wpn.getItem().getSize();
	}
	
	/**
	 * Return the modifier corresponding to the Enchant Effect of the Active Weapon (Max : 127).<BR>
	 * <BR>
	 */
	public int getEnchantEffect()
	{
		final L2ItemInstance wpn = getActiveWeaponInstance();
		if (wpn == null && !(NewHuntingGrounds._started && _inEventHG))
			return 0;
		if (NewHuntingGrounds._started && _inEventHG)
		{
			if (_teamNameHG.equalsIgnoreCase("Red"))
			{
				return 13;
			}
			if (_teamNameHG.equalsIgnoreCase("Green"))
			{
				return 9;
			}
		}
		if (NewCTF._started && _inEventCTF && wpn.getItemId() == 61000)
		{
			int team = 0;
			String team1 = NewCTF._teams.get(0);
			String team2 = NewCTF._teams.get(1);
			String oppositeTeam = "";
			int ench = 0;
			if (_teamNameCTF.equalsIgnoreCase(NewCTF._teams.get(0)))
			{
				team = 1;
			}
			if (_teamNameCTF.equalsIgnoreCase(NewCTF._teams.get(1)))
			{
				team = 2;
			}
			switch (team)
			{
				case 1:
					oppositeTeam = team2;
					break;
				case 2:
					oppositeTeam = team1;
					break;
			}
			switch (oppositeTeam)
			{
				case "Red":
					ench = 50;
					break;
				case "Green":
					ench = 39;
					break;
				case "Blue":
					ench = 37;
					break;
				case "Orange":
					ench = 41;
					break;
				case "Purple":
					ench = 46;
					break;
				case "Yellow":
					ench = 40;
					break;
				default:
					ench = 50;
					break;
			}
			return ench;
		}
		switch (wpn.getItemId())
		{
			case Config.AURAFANG:
			case Config.RAYBLADE:
			case Config.WAVEBRAND:
			{
				if (!_hasTehForce)
					return 0;
				int mod = wpn.getEnchantLevel() % 4;
				switch (mod)
				{
					case 1:
						return 36; // red
					case 2:
						return 17; // purple
					case 3:
						return 39; // green
					default:
						return 37; // blue
				}
			}
		}
		if (isInOlympiadMode())
			return 5;
		if (isInFunEvent())
		{
			if (wpn.getCrystalType() <= L2Item.CRYSTAL_S && wpn.getUniqueness() < 3)
			{
				return 25;
			}
		}
		else if (isInSgradeZone())
		{
			if (wpn.getUniqueness() == 3 && !wpn.isStandardShopItem() && !wpn.isRaidbossItem())
			{
				if (wpn.getCrystalType() == L2Item.CRYSTAL_S)
				{
					if (wpn.getEnchantLevel() > 15)
						return 15;
				}
				else
				{
					if (wpn.getEnchantLevel() > 10)
						return 10;
				}
			}
			else if (wpn.getUniqueness() == 2.5)
			{
				if (wpn.getEnchantLevel() > 17)
					return 17;
			}
			else if (wpn.getUniqueness() == 2)
			{
				if (wpn.getEnchantLevel() > 20)
					return 20;
			}
		}
		if (wpn.isMorheim())
			return Math.min(127, wpn.getEnchantLevel() + 50);
		if (wpn.isDread() || wpn.isCorrupted())
			return Math.min(127, wpn.getEnchantLevel() + 6);
		if (wpn.isWarForged())
			return Math.min(127, wpn.getEnchantLevel() + 6);
		return Math.min(127, wpn.getEnchantLevel());
	}
	
	/**
	 * Set the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.<BR>
	 * <BR>
	 */
	public void setLastFolkNPC(L2Npc folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}
	
	/**
	 * Return the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.<BR>
	 * <BR>
	 */
	public L2Npc getLastFolkNPC()
	{
		return _lastFolkNpc;
	}
	
	/**
	 * Return True if L2PcInstance is a participant in the Festival of Darkness.<BR>
	 * <BR>
	 */
	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isParticipant(this);
	}
	
	class RentPetTask implements Runnable
	{
		public void run()
		{
			stopRentPet();
		}
	}
	
	class WaterTask implements Runnable
	{
		public void run()
		{
			double reduceHp = getMaxHp() / 100.0;
			if (reduceHp < 1)
				reduceHp = 1;
			reduceCurrentHp(reduceHp, L2PcInstance.this, false, false, null);
			// reduced hp, becouse not rest
			SystemMessage sm = new SystemMessage(SystemMessageId.DROWN_DAMAGE_S1);
			sm.addNumber((int) reduceHp);
			sendPacket(sm);
		}
	}
	
	class LookingForFishTask implements Runnable
	{
		boolean	_isNoob, _isUpperGrade;
		int		_fishType, _fishGutsCheck, _gutsCheckTime;
		long	_endTaskTime;
		
		protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob, boolean isUpperGrade)
		{
			_fishGutsCheck = fishGutsCheck;
			_endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
			_fishType = fishType;
			_isNoob = isNoob;
			_isUpperGrade = isUpperGrade;
		}
		
		public void run()
		{
			if (System.currentTimeMillis() >= _endTaskTime)
			{
				endFishing(false);
				return;
			}
			if (_fishType == -1)
				return;
			int check = Rnd.get(1000);
			if (_fishGutsCheck > check)
			{
				stopLookingForFishTask();
				startFishCombat(_isNoob, _isUpperGrade);
			}
		}
	}
	
	public int getClanPrivileges()
	{
		return _clanPrivileges;
	}
	
	public void setClanPrivileges(int n)
	{
		_clanPrivileges = n;
	}
	
	// baron etc
	public void setPledgeClass(int classId)
	{
		_pledgeClass = classId;
		checkItemRestriction();
	}
	
	public int getPledgeClass()
	{
		return _pledgeClass;
	}
	
	public void setPledgeType(int typeId)
	{
		_pledgeType = typeId;
	}
	
	public int getPledgeType()
	{
		return _pledgeType;
	}
	
	public int getApprentice()
	{
		return _apprentice;
	}
	
	public void setApprentice(int apprentice_id)
	{
		_apprentice = apprentice_id;
	}
	
	public int getSponsor()
	{
		return _sponsor;
	}
	
	public void setSponsor(int sponsor_id)
	{
		_sponsor = sponsor_id;
	}
	
	public int getBookMarkSlot()
	{
		return _bookmarkslot;
	}
	
	public void setBookMarkSlot(int slot)
	{
		_bookmarkslot = slot;
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	@Override
	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}
	
	@Override
	public void sendServerMessage(String message)
	{
		sendPacket(SystemMessage.sendString("SRVR: " + message));
	}
	
	public void enterObserverMode(int x, int y, int z)
	{
		_obsX = getX();
		_obsY = getY();
		_obsZ = getZ();
		setTarget(null);
		stopMove(null);
		setIsParalyzed(true);
		setIsInvul(true);
		setInvisible(true);
		sendPacket(new GMHide(1));
		sendPacket(new ObservationMode(x, y, z));
		setXYZ(x, y, z);
		_observerMode = true;
		broadcastUserInfo();
	}
	
	public void showQuestMovie(int id)
	{
		if (_movieId > 0) // already in movie
			return;
		abortAttack();
		abortCast();
		stopMove(null);
		_movieId = id;
		sendPacket(new ExStartScenePlayer(id));
	}
	
	public void enterOlympiadObserverMode(Location loc, int id)
	{
		enterOlympiadObserverMode(loc.getX(), loc.getY(), loc.getZ(), id, true);
	}
	
	public void enterOlympiadObserverMode(int x, int y, int z, int id, boolean storeCoords)
	{
		if (getPet() != null)
			getPet().unSummon(this);
		if (!getCubics().isEmpty())
		{
			for (L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction();
			}
			getCubics().clear();
		}
		if (getParty() != null)
			getParty().removePartyMember(this);
		_olympiadGameId = id;
		if (isSitting())
			standUp();
		if (storeCoords)
		{
			_obsX = getX();
			_obsY = getY();
			_obsZ = getZ();
		}
		setTarget(null);
		setIsInvul(true);
		setInvisible(true);
		sendPacket(new GMHide(1));
		teleToLocation(x, y, z, false);
		sendPacket(new ExOlympiadMode(3));
		_observerMode = true;
		broadcastUserInfo();
	}
	
	public void leaveObserverMode()
	{
		setTarget(null);
		setXYZ(_obsX, _obsY, _obsZ);
		setIsParalyzed(false);
		sendPacket(new GMHide(0));
		if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invis", this))
			setInvisible(false);
		if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invul", this))
			setIsInvul(false);
		if (getAI() != null)
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		_observerMode = false;
		sendPacket(new ObservationReturn(this));
		broadcastUserInfo();
	}
	
	public void leaveOlympiadObserverMode()
	{
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
		teleToLocation(_obsX, _obsY, _obsZ, true);
		sendPacket(new GMHide(0));
		if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invis", this))
			setInvisible(false);
		if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invul", this))
			setIsInvul(false);
		if (getAI() != null)
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		if (_olympiadGameId != -1)
			Olympiad.removeSpectator(_olympiadGameId, this);
		_olympiadGameId = -1;
		_observerMode = false;
		broadcastUserInfo();
	}
	
	public void setOlympiadSide(int i)
	{
		_olympiadSide = i;
	}
	
	public int getOlympiadSide()
	{
		return _olympiadSide;
	}
	
	public void setOlympiadGameId(int id)
	{
		_olympiadGameId = id;
	}
	
	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}
	
	public int getObsX()
	{
		return _obsX;
	}
	
	public int getObsY()
	{
		return _obsY;
	}
	
	public int getObsZ()
	{
		return _obsZ;
	}
	
	public boolean inObserverMode()
	{
		return _observerMode;
	}
	
	public int getTeleMode()
	{
		return _telemode;
	}
	
	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}
	
	public void setLoto(int i, int val)
	{
		_loto[i] = val;
	}
	
	public int getLoto(int i)
	{
		return _loto[i];
	}
	
	public void setRace(int i, int val)
	{
		_monsterRace[i] = val;
	}
	
	public int getRace(int i)
	{
		return _monsterRace[i];
	}
	
	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}
	
	public void setMessageRefusal(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}
	
	public boolean getDietMode()
	{
		return _dietMode;
	}
	
	public void setTradeRefusal(boolean mode)
	{
		_tradeRefusal = mode;
		if (_tradeRefusal)
			sendMessage("Rejecting all requests");
		else
			sendMessage("Accepting requests");
	}
	
	public void setShowSkillEffects(boolean toggle)
	{
		_showSkillEffects = toggle;
		if (_showSkillEffects)
			sendMessage("Displaying all skill effects");
		else
			sendMessage("Hiding all skill effects");
	}
	
	public boolean getShowSkillEffects()
	{
		return _showSkillEffects;
	}
	
	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}
	
	public BlockList getBlockList()
	{
		return _blockList;
	}
	
	public void setHero(boolean hero)
	{
		if (hero && _baseClass == _activeClass)
		{
			for (int i : HERO_SKILLS)
				addSkill(i);
		}
		else
		{
			for (int i : HERO_SKILLS)
				removeSkill(i, true);
		}
		_hero = hero;
		sendSkillList();
	}
	
	public void setIsInOlympiadMode(boolean b)
	{
		_inOlympiadMode = b;
	}
	
	public void setIsOlympiadStart(boolean b)
	{
		_OlympiadStart = b;
	}
	
	public boolean isOlympiadStart()
	{
		return _OlympiadStart;
	}
	
	public boolean isHero()
	{
		return _hero;
	}
	
	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}
	
	public boolean isInDuel()
	{
		return _isInDuel;
	}
	
	public int getDuelId()
	{
		return _duelId;
	}
	
	public void setDuelState(int mode)
	{
		_duelState = mode;
	}
	
	public int getDuelState()
	{
		return _duelState;
	}
	
	/**
	 * Sets up the duel state using a non 0 duelId.
	 * 
	 * @param duelId
	 *            0=not in a duel
	 */
	public void setIsInDuel(int duelId)
	{
		if (duelId > 0)
		{
			_isInDuel = true;
			_duelState = Duel.DUELSTATE_DUELLING;
			_duelId = duelId;
		}
		else
		{
			if (_duelState == Duel.DUELSTATE_DEAD)
			{
				enableAllSkills();
				getStatus().startHpMpRegeneration();
			}
			_isInDuel = false;
			_duelState = Duel.DUELSTATE_NODUEL;
			_duelId = 0;
		}
	}
	
	public void setIsInPreDuelState(boolean preDuelState)
	{
		_preDuelState = preDuelState;
		// sendMessage(String.valueOf(preDuelState));
	}
	
	public boolean isInPreDuelState()
	{
		return _preDuelState;
	}
	
	/**
	 * This returns a SystemMessage stating why
	 * the player is not available for duelling.
	 * 
	 * @return S1_CANNOT_DUEL... message
	 */
	public SystemMessage getNoDuelReason()
	{
		SystemMessage sm = new SystemMessage(_noDuelReason);
		sm.addPcName(this);
		_noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
		return sm;
	}
	
	/**
	 * Checks if this player might join / start a duel.
	 * To get the reason use getNoDuelReason() after calling this function.
	 * 
	 * @return true if the player might join/start a duel.
	 */
	public boolean canDuel()
	{
		if (isInCombat() || getPunishLevel() == PunishLevel.JAIL)
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
			return false;
		}
		if (isDead() || isAlikeDead() || (getCurrentHp() < getMaxHp() / 2 || getCurrentMp() < getMaxMp() / 2))
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_HP_OR_MP_IS_BELOW_50_PERCENT;
			return false;
		}
		if (isInDuel())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_ALREADY_ENGAGED_IN_A_DUEL;
			return false;
		}
		if (isInOlympiadMode())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
			return false;
		}
		if (isCursedWeaponEquipped())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_IN_A_CHAOTIC_STATE;
			return false;
		}
		if (getPrivateStoreType() != STORE_PRIVATE_NONE)
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
			return false;
		}
		if (isMounted() || isInBoat())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER;
			return false;
		}
		if (isFishing())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_FISHING;
			return false;
		}
		if (isInsideZone(ZONE_PVP) || isInsideZone(ZONE_PEACE) || isInsideZone(ZONE_SIEGE))
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_C1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA;
			return false;
		}
		if (isInFunEvent())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_C1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA;
			return false;
		}
		return true;
	}
	
	public boolean isNoble()
	{
		return _noble;
	}
	
	public void setNoble(boolean val)
	{
		if (val)
			for (L2Skill s : NobleSkillTable.getInstance().getNobleSkills())
				addSkill(s, false); // Dont Save Noble skills to Sql
		else
			for (L2Skill s : NobleSkillTable.getInstance().getNobleSkills())
				super.removeSkill(s); // Just Remove skills without deleting from Sql
		_noble = val;
		sendSkillList();
	}
	
	public void setTeam(int team)
	{
		_team = team;
		if (getPet() != null)
			getPet().broadcastStatusUpdate();
	}
	
	public int getTeam()
	{
		return _team;
	}
	
	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}
	
	public int getWantsPeace()
	{
		return _wantsPeace;
	}
	
	public boolean isFishing()
	{
		return _fishing;
	}
	
	public void setFishing(boolean fishing)
	{
		_fishing = fishing;
	}
	
	public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance)
	{
		// [-5,-1] varka, 0 neutral, [1,5] ketra
		_alliedVarkaKetra = sideAndLvlOfAlliance;
	}
	
	public int getAllianceWithVarkaKetra()
	{
		return _alliedVarkaKetra;
	}
	
	public boolean isAlliedWithVarka()
	{
		return (_alliedVarkaKetra < 0);
	}
	
	public boolean isLocked()
	{
		return _subclassLock.isLocked();
	}
	
	public boolean isAlliedWithKetra()
	{
		return (_alliedVarkaKetra > 0);
	}
	
	public void sendSkillList()
	{
		SkillList sl = new SkillList();
		if (_transformation != null)
		{
			for (L2Skill s : getAllSkills())
			{
				if (s == null)
					continue;
				if (!s.isPassive())
				{
					if (isSkillDisabledDueToTransformation(s.getId()))
						continue;
				}
				if (s.sendIcon())
					sl.addSkill(s.getId(), s.getLevel(), s.isPassive());
			}
		}
		else
		{
			for (L2Skill s : getAllSkills())
			{
				if (s == null)
					continue;
				if (s.sendIcon())
					sl.addSkill(s.getId(), s.getLevel(), s.isPassive());
				// if (LunaSkillGuard.getInstance().checkSkill(this, s.getId()))
				// {
				// if (s.sendIcon())
				// {
				// sl.addSkill(s.getId(), s.getLevel(), s.isPassive());
				// }
				// }
				// else
				// continue;
			}
		}
		sendPacket(sl);
	}
	
	public void sendOlySkillList()
	{
		SkillList olsl = new SkillList();
		if (_transformation != null)
		{
			for (L2Skill s : getAllSkills())
			{
				if (s == null)
					continue;
				if (isInOlympiadMode())
				{
					if (s.isEnabledInOlympiad())
						olsl.addSkill(s.getId(), s.getLevel(), s.isPassive());
				}
				if (s.sendIcon())
					olsl.addSkill(s.getId(), s.getLevel(), s.isPassive());
			}
		}
		sendPacket(olsl);
	}
	
	/**
	 * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>)
	 * for this character.<BR>
	 * 2. This method no longer changes the active _classIndex of the player. This is only
	 * done by the calling of setActiveClass() method as that should be the only way to do so.
	 *
	 * @param int
	 *            classId
	 * @param int
	 *            classIndex
	 * @return boolean subclassAdded
	 */
	public boolean addSubClass(int classId, int classIndex)
	{
		if (!_subclassLock.tryLock())
			return false;
		try
		{
			// if (isInDuel() || isInOlympiadMode() || isInCombat() || _transformation != null || Olympiad.getInstance().isRegistered(this))
			if (isParalyzed() || isInDuel() || isInOlympiadMode() || isInCombat() || _transformation != null || Olympiad.getInstance().isRegistered(this))
			{
				sendMessage("Cannot modify your class while Paralyzed/in Combat/Olympiad/Transformed.");
				return false;
			}
			if (getTotalSubClasses() == Config.MAX_SUBCLASS)
			{
				sendMessage("You have reach the maximum number of available subclasses.");
				return false;
			}
			if (classIndex == 0)
			{
				_log.warning(getName() + " add subclass classindex = 0");
				return false;
			}
			if (getSubClasses().containsKey(classIndex))
				return false;
			if (cannotChangeSubsDueToInstance())
			{
				sendMessage("You can't change your subclass while you have an instance active");
				return false;
			}
			// Note: Never change _classIndex in any method other than setActiveClass().
			SubClass newClass = new SubClass();
			newClass.setClassId(classId);
			newClass.setClassIndex(classIndex);
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				// Store the basic info about this new sub-class.
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(ADD_CHAR_SUBCLASS);
				statement.setInt(1, getObjectId());
				statement.setInt(2, newClass.getClassId());
				statement.setLong(3, newClass.getExp());
				statement.setInt(4, newClass.getSp());
				statement.setInt(5, newClass.getLevel());
				statement.setInt(6, newClass.getClassIndex()); // <-- Added
				statement.execute();
			}
			catch (Exception e)
			{
				_log.warning("WARNING: Could not add character sub class for " + getName() + ": " + e);
				return false;
			}
			finally
			{
				try
				{
					statement.close();
				}
				catch (Exception e)
				{}
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
			// Commit after database INSERT incase exception is thrown.
			getSubClasses().put(newClass.getClassIndex(), newClass);
			if (Config.DEBUG)
				_log.info(getName() + " added class ID " + classId + " as a sub class at index " + classIndex + ".");
			ClassId subTemplate = ClassId.values()[classId];
			Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(subTemplate);
			if (skillTree == null)
				return true;
			Map<Integer, L2Skill> prevSkillList = new FastMap<Integer, L2Skill>();
			for (L2SkillLearn skillInfo : skillTree)
			{
				if (skillInfo.getMinLevel() <= 40)
				{
					L2Skill prevSkill = prevSkillList.get(skillInfo.getId());
					L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getId(), skillInfo.getLevel());
					if (prevSkill != null && (prevSkill.getLevel() > newSkill.getLevel()))
						continue;
					prevSkillList.put(newSkill.getId(), newSkill);
					storeSkill(newSkill, prevSkill, classIndex);
					// if (!storeSkillBool(newSkill, prevSkill, classIndex))
					// {
					// break;
					// }
					// else
					// {
					// continue;
					// }
				}
			}
			StuckSubGuard.getInstance().checkPlayer(this);
			if (Config.DEBUG)
				_log.info(getName() + " was given " + getAllSkills().length + " skills for their new sub class.");
			return true;
		}
		finally
		{
			_subclassLock.unlock();
		}
	}
	
	/**
	 * 1. Completely erase all existance of the subClass linked to the classIndex.<BR>
	 * 2. Send over the newClassId to addSubClass()to create a new instance on this classIndex.<BR>
	 * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.<BR>
	 *
	 * @param int
	 *            classIndex
	 * @param int
	 *            newClassId
	 * @return boolean subclassAdded
	 */
	public boolean modifySubClass(int classIndex, int newClassId)
	{
		if (!_subclassLock.tryLock())
			return false;
		if (isInDuel() || isInOlympiadMode() || isInCombat() || _transformation != null || Olympiad.getInstance().isRegistered(this))
		{
			sendMessage("Cannot modify your class while Paralyzed/in Combat/Olympiad/Transformed.");
			return false;
		}
		if (cannotChangeSubsDueToInstance())
		{
			sendMessage("You can't change your subclass while you have an instance active");
			return false;
		}
		try
		{
			int oldClassId = getSubClasses().get(classIndex).getClassId();
			if (Config.DEBUG)
				_log.info(getName() + " has requested to modify sub class index " + classIndex + " from class ID " + oldClassId + " to " + newClassId + ".");
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				// Remove all henna info stored for this sub-class.
				statement = con.prepareStatement(DELETE_CHAR_HENNAS);
				statement.setInt(1, getObjectId());
				statement.setInt(2, classIndex);
				statement.execute();
				statement.close();
				// Remove all shortcuts info stored for this sub-class.
				statement = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
				statement.setInt(1, getObjectId());
				statement.setInt(2, classIndex);
				statement.execute();
				statement.close();
				// Remove all effects info stored for this sub-class.
				statement = con.prepareStatement(DELETE_SKILL_SAVE);
				statement.setInt(1, getObjectId());
				statement.setInt(2, classIndex);
				statement.execute();
				statement.close();
				// Remove all skill info stored for this sub-class.
				statement = con.prepareStatement(DELETE_CHAR_SKILLS);
				statement.setInt(1, getObjectId());
				statement.setInt(2, classIndex);
				statement.execute();
				statement.close();
				// Remove all basic info stored about this sub-class.
				statement = con.prepareStatement(DELETE_CHAR_SUBCLASS);
				statement.setInt(1, getObjectId());
				statement.setInt(2, classIndex);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e);
				// This must be done in order to maintain data consistency.
				getSubClasses().remove(classIndex);
				return false;
			}
			finally
			{
				try
				{
					statement.close();
				}
				catch (Exception e)
				{}
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
			getSubClasses().remove(classIndex);
			return addSubClass(newClassId, classIndex);
		}
		finally
		{
			_subclassLock.unlock();
		}
	}
	
	public boolean isSubClassActive()
	{
		return _classIndex > 0;
	}
	
	public Map<Integer, SubClass> getSubClasses()
	{
		if (_subClasses == null)
			_subClasses = new FastMap<Integer, SubClass>();
		return _subClasses;
	}
	
	public int getTotalSubClasses()
	{
		return getSubClasses().size();
	}
	
	public int getBaseClass()
	{
		return getBaseClassId();
	}
	
	public int getBaseClassId()
	{
		return _baseClass;
	}
	
	public int getActiveClass()
	{
		return _activeClass;
	}
	
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	private void setClassTemplate(int classId)
	{
		_activeClass = classId;
		L2PcTemplate t = CharTemplateTable.getInstance().getTemplate(classId);
		if (t == null)
		{
			_log.severe("Missing template for classId: " + classId);
			throw new Error();
		}
		// Set the template of the L2PcInstance
		setTemplate(t);
	}
	
	/**
	 * Changes the character's class based on the given class index.
	 * <BR>
	 * <BR>
	 * An index of zero specifies the character's original (base) class,
	 * while indexes 1-3 specifies the character's sub-classes respectively.
	 * <br>
	 * <br>
	 * <font color="00FF00"/>WARNING: Use only on subclase change</font>
	 *
	 * @param classIndex
	 */
	public void setParalyzedEffect()
	{
		abortAttack();
		abortCast();
		stopMove(null);
		startAbnormalEffect(AbnormalEffect.HOLD_1);
		startAbnormalEffect(AbnormalEffect.HOLD_2);
		setIsParalyzed(true);
		StopMove sm = new StopMove(this);
		sendPacket(sm);
		broadcastPacket(sm);
	}
	
	public void endParalyzedEffect()
	{
		stopAbnormalEffect(AbnormalEffect.HOLD_1);
		stopAbnormalEffect(AbnormalEffect.HOLD_2);
		setIsParalyzed(false);
	}
	
	public boolean setActiveClass(int classIndex)
	{
		if (!_subclassLock.tryLock())
			return false;
		try
		{
			if (isParalyzed() || isInDuel() || isInOlympiadMode() || isInCombat() || _transformation != null || Olympiad.getInstance().isRegistered(this))
			{
				sendMessage("Cannot modify your class while Paralyzed/in Combat/Olympiad/Transformed.");
				return false;
			}
			if (cannotChangeSubsDueToInstance())
			{
				sendMessage("You can't change your subclass while you have an instance active");
				return false;
			}
			setParalyzedEffect();
			// Remove active item skills before saving char to database
			// because next time when choosing this class, weared items can
			// be different
			for (L2ItemInstance temp : getInventory().getAugmentedItems())
				if (temp != null && temp.isEquipped())
					temp.getAugmentation().removeBonus(this);
			// Delete a force buff upon class change.
			if (_fusionSkill != null)
				abortCast();
			// Stop casting for any player that may be casting a force buff on this l2pcinstance.
			for (L2Character character : getKnownList().getKnownCharacters())
			{
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();
			}
			/*
			 * 1. Call store() before modifying _classIndex to avoid skill effects rollover. 2.
			 * Register the correct _classId against applied 'classIndex'.
			 */
			store(Config.SUBCLASS_STORE_SKILL_COOLTIME, true);
			_reuseTimeStamps.clear();
			// clear charges
			_charges.set(0);
			clearSouls();
			if (classIndex == 0)
			{
				setClassTemplate(getBaseClassId());
			}
			else
			{
				try
				{
					setClassTemplate(getSubClasses().get(classIndex).getClassId());
				}
				catch (Exception e)
				{
					_log.info("Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e);
					return false;
				}
			}
			_classIndex = classIndex;
			if (isInParty())
				getParty().recalculatePartyLevel();
			/*
			 * Update the character's change in class status.
			 * 1. Remove any active cubics from the player. 2. Renovate the characters table in the
			 * database with the new class info, storing also buff/effect data. 3. Remove all
			 * existing skills. 4. Restore all the learned skills for the current class from the
			 * database. 5. Restore effect/buff data for the new class. 6. Restore henna data for
			 * the class, applying the new stat modifiers while removing existing ones. 7. Reset
			 * HP/MP/CP stats and send Server->Client character status packet to reflect changes. 8.
			 * Restore shortcut data related to this class. 9. Resend a class change animation
			 * effect to broadcast to all nearby players. 10.Unsummon any active servitor from the
			 * player.
			 */
			final L2Summon pet = getPet();
			if (pet != null)
			{
				if (pet instanceof L2SummonInstance)
					pet.unSummon(this);
				else
					pet.stopAllEffectsExceptThoseThatLastThroughDeath();
			}
			if (!getCubics().isEmpty())
			{
				for (L2CubicInstance cubic : getCubics().values())
				{
					cubic.stopAction();
				}
				getCubics().clear();
			}
			for (L2Skill oldSkill : getAllSkills())
				super.removeSkill(oldSkill);
			stopAllEffectsExceptThoseThatLastThroughDeath();
			if (isSubClassActive())
			{
				_dwarvenRecipeBook.clear();
				// Common recipe book shared for all subclasses for now. TODO confirm this info
				// _commonRecipeBook.clear();
			}
			else
			{
				restoreRecipeBook(false);
			}
			// Restore any Death Penalty Buff
			restoreDeathPenaltyBuffLevel();
			restoreSkills();
			regiveTemporarySkills();
			giveMarriageSkills();
			rewardSkills();
			// Prevents some issues when changing between subclases that shares skills
			if (_disabledSkills != null && !_disabledSkills.isEmpty())
				_disabledSkills.clear();
			restoreEffects();
			updateEffectIcons();
			sendPacket(new EtcStatusUpdate(this));
			// if player has quest 422: Repent Your Sins, remove it
			QuestState st = getQuestState("422_RepentYourSins");
			if (st != null)
			{
				st.exitQuest(true);
			}
			for (int i = 0; i < 3; i++)
				_henna[i] = null;
			restoreHenna();
			sendPacket(new HennaInfo(this));
			InertiaController.getInstance().fetchChill(getActingPlayer()).reset();
			if (getCurrentHp() > getMaxHp())
				setCurrentHp(getMaxHp());
			if (getCurrentMp() > getMaxMp())
				setCurrentMp(getMaxMp());
			if (getCurrentCp() > getMaxCp())
				setCurrentCp(getMaxCp());
			broadcastUserInfo();
			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
			// _macroses.restore();
			// _macroses.sendUpdate();
			_shortCuts.restore();
			sendPacket(new ShortCutInit(this));
			broadcastPacket(new SocialAction(getObjectId(), SocialAction.LEVEL_UP));
			sendPacket(new SkillCoolTime(this));
			sendPacket(new ExStorageMaxCount(this));
			StuckSubGuard.getInstance().checkPlayer(this);
			sendSkillList();
			// LunaSkillGuard.getInstance().checkForIncorrectSkills(this);
			// decayMe();
			// spawnMe(getX(), getY(), getZ());
			if (Config.ENABLE_SUBCLASS_LOGS)
			{
				LunaLogger.getInstance().log("subclass_logs", getName() + " Set Active Class: -> " + getClassId().getName());
			}
			clearPath();
			CreatureSay cs = new CreatureSay(getObjectId(), Say2.MSNCHAT, getName(), "Your classpath trees has been reseted.\r\nSet them up again.");
			CreatureSay cs2 = new CreatureSay(getObjectId(), Say2.PARTYROOM_COMMANDER, getName(), "Your classpath trees has been reseted.\r\n  Do not forget to set them up again.");
			sendPacket(cs2);
			sendPacket(cs);
			return true;
		}
		finally
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					L2PcInstance.this.endParalyzedEffect();
				}
			}, 5000);
			_subclassLock.unlock();
		}
	}
	
	private boolean cannotChangeSubsDueToInstance()
	{
		InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(this);
		if (world != null)
		{
			if (world.templateId == 2000 || world.templateId == 2001 || world.templateId == 5000 || world.templateId == 2005)
				return true;
		}
		return false;
	}
	
	public boolean cannotMakePartyActions()
	{
		InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(this);
		if (world != null)
		{
			if (world.templateId == 5000)
				return true;
		}
		return false;
	}
	
	public void stopRentPet()
	{
		if (_taskRentPet != null)
		{
			// if the rent of a wyvern expires while over a flying zone, tp to down before unmounting
			if (checkLandingState() && getMountType() == 2)
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
			if (dismount()) // this should always be true now, since we teleported already
			{
				_taskRentPet.cancel(true);
				_taskRentPet = null;
			}
		}
	}
	
	public void startRentPet(int seconds)
	{
		if (_taskRentPet == null)
			_taskRentPet = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RentPetTask(), seconds * 1000L, seconds * 1000L);
	}
	
	public boolean isRentedPet()
	{
		if (_taskRentPet != null)
			return true;
		return false;
	}
	
	public void stopWaterTask()
	{
		if (_taskWater != null)
		{
			_taskWater.cancel(false);
			_taskWater = null;
			sendPacket(new SetupGauge(2, 0));
		}
	}
	
	public void startWaterTask()
	{
		if (!isDead() && _taskWater == null)
		{
			int timeinwater = (int) calcStat(Stats.BREATH, 60000, this, null);
			sendPacket(new SetupGauge(2, timeinwater));
			_taskWater = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new WaterTask(), timeinwater, 1000);
		}
	}
	
	public boolean isInWater()
	{
		if (_taskWater != null)
			return true;
		return false;
	}
	
	public void checkWaterState()
	{
		if (isInsideZone(ZONE_WATER))
			startWaterTask();
		else
			stopWaterTask();
	}
	
	public void onPlayerEnter()
	{
		if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
		{
			if (!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(this) != SevenSigns.getInstance().getCabalHighestScore())
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
			}
		}
		else
		{
			if (!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(this) == SevenSigns.CABAL_NULL)
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
			}
		}
		// jail task
		updatePunishState();
		if (isGM())
		{
			if (_isInvul)
				sendServerMessage("Entering world in Invulnerable mode.");
			if (isInvisible())
				sendServerMessage("Entering world in Invisible mode.");
			if (getMessageRefusal())
				sendServerMessage("Entering world in Message Refusal mode.");
			_isHidedGm = false;
		}
		revalidateZone(true);
	}
	
	public long getLastAccess()
	{
		return _lastAccess;
	}
	
	private void checkRecom(int recsHave, int recsLeft)
	{
		Calendar check = Calendar.getInstance();
		check.setTimeInMillis(_charCreationTime);
		check.add(Calendar.DAY_OF_MONTH, 1);
		Calendar min = Calendar.getInstance();
		_recomHave = recsHave;
		_recomLeft = recsLeft;
		if (getStat().getLevel() < 10 || check.after(min))
			return;
		restartRecom();
	}
	
	public void restartRecom()
	{
		/*
		 * if (Config.ALT_RECOMMEND)
		 * {
		 * Connection con = null;
		 * try
		 * {
		 * con = L2DatabaseFactory.getInstance().getConnection();
		 * PreparedStatement statement = con.prepareStatement(DELETE_CHAR_RECOMS);
		 * statement.setInt(1, getObjectId());
		 * statement.execute();
		 * statement.close();
		 * _recomChars.clear();
		 * }
		 * catch (Exception e)
		 * {
		 * _log.log(Level.SEVERE, "Failed cleaning character recommendations.", e);
		 * }
		 * finally
		 * {
		 * try { con.close(); } catch (Exception e) {}
		 * }
		 * }
		 * if (getStat().getLevel() < 20)
		 * {
		 * _recomLeft = 3;
		 * _recomHave--;
		 * }
		 * else if (getStat().getLevel() < 40)
		 * {
		 * _recomLeft = 6;
		 * _recomHave -= 2;
		 * }
		 * else
		 * {
		 * _recomLeft = 9;
		 * _recomHave -= 3;
		 * }
		 * if (_recomHave < 0) _recomHave = 0;
		 * // If we have to update last update time, but it's now before 13, we should set it to yesterday
		 * Calendar update = Calendar.getInstance();
		 * if(update.get(Calendar.HOUR_OF_DAY) < 13) update.add(Calendar.DAY_OF_MONTH,-1);
		 * update.set(Calendar.HOUR_OF_DAY,13);
		 * _charCreationTime = update.getTimeInMillis();
		 */
	}
	
	@Override
	public void doRevive()
	{
		if (isDead())
		{
			setIsRespawnRequested(false);
			super.doRevive();
			sendPacket(new UserInfo(this));
			sendPacket(new ExBrExtraUserInfo(this));
			stopEffects(L2EffectType.CHARMOFCOURAGE);
			updateEffectIcons();
			sendPacket(new EtcStatusUpdate(this));
			_reviveRequested = 0;
			_revivePower = 0;
			if (isMounted())
				startFeed(_mountNpcId);
			if (isInParty() && getParty().isInDimensionalRift())
			{
				if (!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ()))
					getParty().getDimensionalRift().memberRessurected(this);
			}
			if (getPet() != null)
			{
				getPet().doRevive();
			}
			else
			{
				return;
			}
			if (isInFunEvent())
			{
				getStatus().setCurrentHp(getMaxHp());
				getStatus().setCurrentCp(getMaxCp());
				if (_inEventDM)
				{
					getStatus().setCurrentMp(Math.max(getCurrentMp(), getMaxMp() * 0.3));
				}
				else
					getStatus().setCurrentMp(getMaxMp());
			}
		}
	}
	
	@Override
	public void doRevive(double revivePower)
	{
		// Restore the player's lost experience,
		// depending on the % return of the skill used (based on its power).
		if (isDead())
			restoreExp(revivePower);
		doRevive();
	}
	
	public void reviveRequest(L2PcInstance Reviver, L2Skill skill, boolean Pet)
	{
		if (_reviveRequested == 1)
		{
			if (_revivePet == Pet)
			{
				Reviver.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
			}
			else
			{
				if (Pet)
					Reviver.sendPacket(new SystemMessage(SystemMessageId.CANNOT_RES_PET2)); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
				else
					Reviver.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
			}
			return;
		}
		if ((Pet && getPet() != null && getPet().isDead()) || (!Pet && isDead()))
		{
			_reviveRequested = 1;
			int restoreExp = 0;
			if (isPhoenixBlessed())
				_revivePower = 100;
			else if (getCharmOfCourage())
				_revivePower = 0;
			else
				_revivePower = Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), Reviver.getWIT());
			restoreExp = (int) Math.round((getExpBeforeDeath() - getExp()) * _revivePower / 100);
			_revivePet = Pet;
			if (getCharmOfCourage())
			{
				ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.RESURRECT_USING_CHARM_OF_COURAGE.getId());
				dlg.addTime(60000);
				sendPacket(dlg);
				return;
			}
			ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST_BY_C1_FOR_S2_XP.getId());
			dlg.addPcName(Reviver);
			dlg.addString(String.valueOf(restoreExp));
			sendPacket(dlg);
			if (Config.L2JMOD_ACHIEVEMENT_SYSTEM && !Pet && !Reviver.getIP().equals(getIP()))
			{
				Reviver.getCounters().playersRessurected++;
			}
		}
	}
	
	public void reviveAnswer(int answer)
	{
		if (_reviveRequested != 1 || (!isDead() && !_revivePet) || (_revivePet && getPet() != null && !getPet().isDead()))
			return;
		// If character refuses a PhoenixBless autoress, cancel all buffs he had
		if (answer == 0 && isPhoenixBlessed())
		{
			stopPhoenixBlessing(null);
			/* stopAllEffectsExceptThoseThatLastThroughDeath(); */
		}
		if (answer == 1)
		{
			if (!_revivePet)
			{
				if (_revivePower != 0)
					doRevive(_revivePower);
				else
					doRevive();
			} /*
				 * else if (getPet() != null)
				 * {
				 * if (_revivePower != 0)
				 * getPet().doRevive(_revivePower);
				 * else
				 * getPet().doRevive();
				 * }
				 */
			if (isInUniqueInstance())
			{
				getCurrentInstance().incCurrentRes();
			}
		}
		_reviveRequested = 0;
		_revivePower = 0;
	}
	
	public Instance getCurrentInstance()
	{
		return InstanceManager.getInstance().getInstance(getInstanceId());
	}
	
	public boolean isReviveRequested()
	{
		return (_reviveRequested == 1);
	}
	
	public boolean isRevivingPet()
	{
		return _revivePet;
	}
	
	public void removeReviving()
	{
		_reviveRequested = 0;
		_revivePower = 0;
	}
	
	public void onActionRequest()
	{
		setProtection(false);
	}
	
	/**
	 * @param expertiseIndex
	 *            The expertiseIndex to set.
	 */
	public void setExpertiseIndex(int expertiseIndex)
	{}
	
	/**
	 * @return Returns the expertiseIndex.
	 */
	public int getExpertiseIndex()
	{
		return 7;
	}
	
	@Override
	public final void onTeleported()
	{
		super.onTeleported();
		// Force a revalidation
		revalidateZone(true);
		if ((Config.PLAYER_SPAWN_PROTECTION > 0) && !isInOlympiadMode())
			setProtection(true);
		// Trained beast is after teleport lost
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().decayMe();
			setTrainedBeast(null);
		}
		// Modify the position of the pet if necessary
		if (getPet() != null)
		{
			getPet().setFollowStatus(false);
			getPet().teleToLocation(getPosition().getX(), getPosition().getY(), getPosition().getZ(), false);
			((L2SummonAI) getPet().getAI()).setStartFollowController(true);
			getPet().setFollowStatus(true);
			getPet().updateAndBroadcastStatus(0);
		}
	}
	
	public void setLastPartyPosition(int x, int y, int z)
	{
		_lastPartyPosition.setXYZ(x, y, z);
	}
	
	public int getLastPartyPositionDistance(int x, int y, int z)
	{
		double dx = (x - _lastPartyPosition.getX());
		double dy = (y - _lastPartyPosition.getY());
		double dz = (z - _lastPartyPosition.getZ());
		return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	
	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x, y, z);
	}
	
	public Point3D getLastServerPosition()
	{
		return _lastServerPosition;
	}
	
	public boolean checkLastServerPosition(int x, int y, int z)
	{
		return _lastServerPosition.equals(x, y, z);
	}
	
	public int getLastServerDistance(int x, int y, int z)
	{
		double dx = (x - _lastServerPosition.getX());
		double dy = (y - _lastServerPosition.getY());
		double dz = (z - _lastServerPosition.getZ());
		return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	
	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		getStat().addExpAndSp(addToExp, addToSp);
	}
	
	public void addExpAndSp(long addToExp, int addToSp, boolean useVitality)
	{
		getStat().addExpAndSp(addToExp, addToSp, useVitality);
	}
	
	public void removeExpAndSp(long removeExp, int removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp);
	}
	
	@Override
	public void reduceCurrentHp(double i, L2Character attacker, L2Skill skill)
	{
		getStatus().reduceHp(i, attacker);
		// notify the tamed beast of attacks
		if (getTrainedBeast() != null)
			getTrainedBeast().onOwnerGotAttacked(attacker);
	}
	
	@Override
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		getStatus().reduceHp(value, attacker, awake, isDOT, false, false);
		// notify the tamed beast of attacks
		if (getTrainedBeast() != null)
			getTrainedBeast().onOwnerGotAttacked(attacker);
	}
	
	@Override
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill, boolean bypassCP)
	{
		getStatus().reduceHp(value, attacker, awake, isDOT, false, bypassCP);
		// notify the tamed beast of attacks
		if (getTrainedBeast() != null)
			getTrainedBeast().onOwnerGotAttacked(attacker);
	}
	
	public void startVitalityTask()
	{
		if (Config.ENABLE_VITALITY && _vitalityTask == null)
		{
			_vitalityTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new VitalityTask(this), 1000, 60000);
		}
	}
	
	public void stopVitalityTask()
	{
		if (_vitalityTask != null)
		{
			_vitalityTask.cancel(false);
			_vitalityTask = null;
		}
	}
	
	private class VitalityTask implements Runnable
	{
		private final L2PcInstance _player;
		
		protected VitalityTask(L2PcInstance player)
		{
			_player = player;
		}
		
		public void run()
		{
			if (!_player.isInsideZone(L2Character.ZONE_PEACE))
				return;
			if (_player.getVitalityPoints() >= PcStat.MAX_VITALITY_POINTS)
				return;
			_player.updateVitalityPoints(Config.RATE_RECOVERY_VITALITY_PEACE_ZONE, false, false);
			_player.sendPacket(new ExVitalityPointInfo(getVitalityPoints()));
		}
	}
	
	public void broadcastSnoop(int type, String name, String _text)
	{
		if (!_snoopListener.isEmpty())
		{
			Snoop sn = new Snoop(getObjectId(), getName(), type, name, _text);
			for (L2PcInstance pci : _snoopListener)
				if (pci != null)
					pci.sendPacket(sn);
		}
	}
	
	public void addSnooper(L2PcInstance pci)
	{
		if (!_snoopListener.contains(pci))
			_snoopListener.add(pci);
	}
	
	public void removeSnooper(L2PcInstance pci)
	{
		_snoopListener.remove(pci);
	}
	
	public void addSnooped(L2PcInstance pci)
	{
		if (!_snoopedPlayer.contains(pci))
			_snoopedPlayer.add(pci);
	}
	
	public void removeSnooped(L2PcInstance pci)
	{
		_snoopedPlayer.remove(pci);
	}
	
	public synchronized void addBypass(String bypass)
	{
		if (bypass == null)
			return;
		_validBypass.add(bypass);
		// _log.warning("[BypassAdd]"+getName()+" '"+bypass+"'");
	}
	
	public synchronized void addBypass2(String bypass)
	{
		if (bypass == null)
			return;
		_validBypass2.add(bypass);
		// _log.warning("[BypassAdd]"+getName()+" '"+bypass+"'");
	}
	
	public synchronized boolean validateBypass(String cmd)
	{
		if (Config.BYPASS_VALIDATION)
			return true;
		for (String bp : _validBypass)
		{
			if (bp == null)
				continue;
			_log.warning("[BypassValidation]" + getName() + " '" + bp + "'");
			if (bp.equals(cmd))
				return true;
		}
		for (String bp : _validBypass2)
		{
			if (bp == null)
				continue;
			_log.warning("[BypassValidation]" + getName() + " '" + bp + "'");
			if (cmd.startsWith(bp))
				return true;
		}
		/*
		 * _log.warning("[L2PcInstance] player ["+getName()+"] sent invalid bypass '"+cmd+"', ban this player!");
		 */ return false;
	}
	
	/**
	 * Performs following tests:<br>
	 * <li>Inventory contains item
	 * <li>Item owner id == this.owner id
	 * <li>It isnt pet control item while mounting pet or pet summoned
	 * <li>It isnt active enchant item
	 * <li>It isnt cursed weapon/item
	 * <li>It isnt wear item
	 * <br>
	 *
	 * @param objectId:
	 *            item object id
	 * @param action:
	 *            just for login porpouse
	 * @return
	 */
	public boolean validateItemManipulation(int objectId, String action)
	{
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		if (item == null || item.getOwnerId() != getObjectId())
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return false;
		}
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId() + ": player tried to " + action + " item controling pet");
			return false;
		}
		if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			return false;
		}
		if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
		{
			// can not trade a cursed weapon
			return false;
		}
		if (item.isWear())
		{
			// cannot drop/trade wear-items
			return false;
		}
		return true;
	}
	
	public synchronized void clearBypass()
	{
		_validBypass.clear();
		_validBypass2.clear();
	}
	
	/**
	 * @return Returns the inBoat.
	 */
	public boolean isInBoat()
	{
		return _inBoat;
	}
	
	/**
	 * @param inBoat
	 *            The inBoat to set.
	 */
	public void setInBoat(boolean inBoat)
	{
		_inBoat = inBoat;
	}
	
	/**
	 * @return
	 */
	public L2BoatInstance getBoat()
	{
		return _boat;
	}
	
	/**
	 * @param boat
	 */
	public void setBoat(L2BoatInstance boat)
	{
		_boat = boat;
	}
	
	/**
	 * @return Returns the inAirShip.
	 */
	public boolean isInAirShip()
	{
		return _inAirShip;
	}
	
	/**
	 * @param inAirShip
	 *            The inAirShip to set.
	 */
	public void setInAirShip(boolean inAirShip)
	{
		_inAirShip = inAirShip;
	}
	
	/**
	 * @return
	 */
	public L2AirShipInstance getAirShip()
	{
		return _airShip;
	}
	
	/**
	 * @param airShip
	 */
	public void setAirShip(L2AirShipInstance airShip)
	{
		if (airShip != null)
			setInAirShip(true);
		else
			setInAirShip(false);
		_airShip = airShip;
	}
	
	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}
	
	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}
	
	/**
	 * @return
	 */
	public Point3D getInBoatPosition()
	{
		return _inBoatPosition;
	}
	
	public void setInBoatPosition(Point3D pt)
	{
		_inBoatPosition = pt;
	}
	
	/**
	 * @return
	 */
	public Point3D getInAirShipPosition()
	{
		return _inAirShipPosition;
	}
	
	public void setInAirShipPosition(Point3D pt)
	{
		_inAirShipPosition = pt;
	}
	
	public int getVitalityPoints()
	{
		return getStat().getVitalityPoints();
	}
	
	public void setVitalityPoints(int points, boolean quiet)
	{
		getStat().setVitalityPoints(points, quiet);
	}
	
	public synchronized void updateVitalityPoints(float points, boolean useRates, boolean quiet)
	{
		getStat().updateVitalityPoints(points, useRates, quiet);
	}
	
	/**
	 * Manage the delete task of a L2PcInstance (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the L2PcInstance is in observer mode, set its position to its position before entering in observer mode</li>
	 * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li>
	 * <li>Cancel Crafting, Attak or Cast</li>
	 * <li>Remove the L2PcInstance from the world</li>
	 * <li>Stop Party and Unsummon Pet</li>
	 * <li>Update database with items in its inventory and remove them from the world</li>
	 * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI</li>
	 * <li>Close the connection with the client</li><BR>
	 * <BR>
	 */
	@SuppressWarnings("incomplete-switch")
	public void deleteMe()
	{
		try
		{
			abortAttack();
			abortCast();
			stopMove(null);
			/*
			 * // Check if the L2PcInstance is in observer mode to set its position to its position
			 * // before entering in observer mode
			 * if (inObserverMode())
			 * setXYZ(_obsX, _obsY, _obsZ);
			 * else if (isInAirShip())
			 * getAirShip().oustPlayer(this);
			 */
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		if (isCursedWeaponEquipped())
			CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, null);
		try
		{
			if (isFlying())
			{
				removeSkill(SkillTable.getInstance().getInfo(4289, 1));
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			setOnlineStatus(false);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		// Stop the HP/MP/CP Regeneration task (scheduled tasks)
		try
		{
			stopAllTimers();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		try
		{
			setIsTeleporting(false);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		// Stop crafting, if in progress
		try
		{
			RecipeController.getInstance().requestMakeItemAbort(this);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		// Cancel Attak or Cast
		try
		{
			setTarget(null);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		try
		{
			if (_fusionSkill != null)
				abortCast();
			for (L2Character character : getKnownList().getKnownCharacters())
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		try
		{
			stopKickFromEventTask();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			for (L2Effect effect : getAllEffects())
			{
				if (effect.getSkill().isToggle())
				{
					effect.exit();
					continue;
				}
				switch (effect.getEffectType())
				{
					case SIGNET_GROUND:
					case SIGNET_EFFECT:
						effect.exit();
						break;
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		try
		{
			final int arena = Olympiad.getSpectatorArena(this);
			if (arena >= 0)
				Olympiad.removeSpectator(arena, this);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		try
		{
			if (getDecoy() != null)
			{
				getDecoy().unSummon(this);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		try
		{
			// save character
			L2GameClient.saveCharToDisk(this);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		// Remove from world regions zones
		L2WorldRegion oldRegion = getWorldRegion();
		// Remove the L2PcInstance from the world
		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		if (oldRegion != null)
			oldRegion.removeFromZones(this);
		// If a Party is in progress, leave it (and festival party)
		if (isInParty())
			try
			{
				leaveParty();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "deleteMe()", e);
			}
		if (Olympiad.getInstance().isRegistered(this) || getOlympiadGameId() != -1) // handle removal from olympiad game
			Olympiad.getInstance().removeDisconnectedCompetitor(this);
		// If the L2PcInstance has Pet, unsummon it
		if (getPet() != null)
		{
			try
			{
				getPet().unSummon(this);
				// dead pet wasnt unsummoned, broadcast npcinfo changes (pet will be without owner name - means owner offline)
				if (getPet() != null)
					getPet().broadcastNpcInfo(0);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "deleteMe()", e);
			} // returns pet to control item
		}
		if (getClan() != null)
		{
			// set the status for pledge member list to OFFLINE
			try
			{
				L2ClanMember clanMember = getClan().getClanMember(getObjectId());
				if (clanMember != null)
				{
					clanMember.setPlayerInstance(null);
					getClan().broadcastToOtherOnlineMembers("Clan member " + getName() + " has logged off.", this);
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "deleteMe()", e);
			}
			getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
		}
		if (getActiveRequester() != null)
		{
			// deals with sudden exit in the middle of transaction
			getActiveRequester().setActiveRequester(null);
			setActiveRequester(null);
			cancelActiveTrade();
		}
		// If the L2PcInstance is a GM, remove it from the GM List
		if (isGM())
		{
			try
			{
				GmListTable.getInstance().deleteGm(this);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		try
		{
			// Check if the L2PcInstance is in observer mode to set its position to its position
			// before entering in observer mode
			if (inObserverMode())
				setXYZInvisible(_obsX, _obsY, _obsZ);
			else if (isInAirShip())
				getAirShip().oustPlayer(this);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		// remove player from instance and set spawn location if any
		try
		{
			final int instanceId = getInstanceId();
			if (instanceId != 0)
			{
				final Instance inst = InstanceManager.getInstance().getInstance(instanceId);
				if (inst != null)
				{
					inst.removePlayer(getObjectId());
					final int[] spawn = inst.getSpawnLoc();
					if (spawn[0] != 0 && spawn[1] != 0 && spawn[2] != 0)
					{
						final int x = spawn[0] + Rnd.get(-30, 30);
						final int y = spawn[1] + Rnd.get(-30, 30);
						setXYZInvisible(x, y, spawn[2]);
						if (getPet() != null) // dead pet
						{
							getPet().teleToLocation(x, y, spawn[2]);
							getPet().setInstanceId(0);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		// TvT Event removal
		try
		{
			TvTEvent.onLogout(this);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		// Update database with items in its inventory and remove them from the world
		try
		{
			getInventory().deleteMe();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		// Update database with items in its warehouse and remove them from the world
		try
		{
			clearWarehouse();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		if (Config.WAREHOUSE_CACHE)
			WarehouseCacheManager.getInstance().remCacheTask(this);
		// Update database with items in its freight and remove them from the world
		try
		{
			clearFreight();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		try
		{
			clearDepositedFreight();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		try
		{
			if (isInEvent())
			{
				LunaEvent activeEvent = EventManager.getInstance().getActiveEvent();
				if (activeEvent != null)
					activeEvent.onDisconnect(this);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe() - luna events", e);
		}
		try
		{
			if (isInRaidEvent())
			{
				if (RaidEvent.getInstance().getState().equals(RaidEvent.State.REGISTER))
					RaidEvent.getInstance().unregister(this);
				else if (RaidEvent.getInstance().getState().equals(RaidEvent.State.ACTIVE))
					RaidEvent.getInstance().onDisconnect(this);
			}
			if (this.isInZombieEvent())
			{
				if (this == Zombie.getInstance().zombie)
				{
					if (Zombie._infected.size() > 0)
					{
						Zombie.getInstance().zombie = Zombie._infected.get(Rnd.get(Zombie._infected.size()));
					}
					else if (Zombie._notinfected.size() > 0)
					{
						int n = Rnd.get(Zombie._notinfected.size());
						Zombie.getInstance().zombie = Zombie._notinfected.get(n);
						TransformationManager.getInstance().transformPlayer(303, Zombie._notinfected.get(n));
						Zombie._infected.add(Zombie._notinfected.get(n));
						Zombie._notinfected.remove(Zombie._notinfected.get(n));
						if (Zombie._notinfected.size() == 0)
							Zombie.getInstance().end();
					}
				}
				else
				{
					if ((Zombie._notinfected.size() == 0))
					{
						Zombie._notinfected.remove(this);
						Zombie.getInstance().end();
					}
				}
			}
			else if (this.isInLastManStandingEvent() && LastManStanding.getInstance().getState() == LastManStanding.State.REGISTER)
			{
				LastManStanding.getInstance().unregister(this);
			}
			else if (this.isInLastManStandingEvent() && LastManStanding.getInstance().getState() == LastManStanding.State.ACTIVE)
			{
				LastManStanding.getInstance().removePlayerOnDisconnect(this);
			}
			else if (this.isInDominationEvent() && Domination.getInstance().getState() == Domination.State.REGISTER)
			{
				Domination.getInstance().unregister(this);
			}
			else if (this.isInDominationEvent() && Domination.getInstance().getState() == Domination.State.ACTIVE)
			{
				Domination.getInstance().removeDisconnectedPlayer(this);
				// System.out.println("removeDisconnectedPlayer --> " + getName());
			}
			else if (this.isInLastTeamStandingEvent() && LastTeamStanding.getInstance().getState().equals(LastTeamStanding.State.ACTIVE))
			{
				LastTeamStanding.getInstance().removePlayerOnDisconnect(this);
			}
			// onDisconnect
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		for (L2PcInstance player : _snoopedPlayer)
			player.removeSnooper(this);
		for (L2PcInstance player : _snoopListener)
			player.removeSnooped(this);
		if (_chanceSkills != null)
		{
			_chanceSkills.setOwner(null);
			_chanceSkills = null;
		}
		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
		L2World.getInstance().removeFromAllPlayers(this); // force remove in case of crash during teleport
		notifyFriends();
		RegionBBSManager.getInstance().changeCommunityBoard();
	}
	
	private void notifyFriends()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			L2PcInstance friend;
			while (rset.next())
			{
				friend = L2World.getInstance().getPlayer(rset.getInt("friendId"));
				if (friend != null) // friend logged in.
				{
					friend.sendPacket(new FriendList(friend));
				}
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not restore friend data:" + e);
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
	
	private FishData _fish;
	
	/*
	 * startFishing() was stripped of any pre-fishing related checks, namely the fishing zone check.
	 * Also worthy of note is the fact the code to find the hook landing position was also striped. The
	 * stripped code was moved into fishing.java. In my opinion it makes more sense for it to be there
	 * since all other skill related checks were also there. Last but not least, moving the zone check
	 * there, fixed a bug where baits would always be consumed no matter if fishing actualy took place.
	 * startFishing() now takes up 3 arguments, wich are acurately described as being the hook landing
	 * coordinates.
	 */
	public void startFishing(int _x, int _y, int _z)
	{
		stopMove(null);
		setIsImmobilized(true);
		_fishing = true;
		_fishx = _x;
		_fishy = _y;
		_fishz = _z;
		broadcastUserInfo();
		// Starts fishing
		int lvl = GetRandomFishLvl();
		int group = GetRandomGroup();
		int type = GetRandomFishType(group);
		List<FishData> fishs = FishTable.getInstance().getfish(lvl, type, group);
		if (fishs == null || fishs.isEmpty())
		{
			sendMessage("Error - Fishes are not definied");
			endFishing(false);
			return;
		}
		int check = Rnd.get(fishs.size());
		// Use a copy constructor else the fish data may be over-written below
		_fish = new FishData(fishs.get(check));
		fishs.clear();
		fishs = null;
		sendPacket(new SystemMessage(SystemMessageId.CAST_LINE_AND_START_FISHING));
		ExFishingStart efs = null;
		if (!GameTimeController.getInstance().isNowNight() && _lure.isNightLure())
			_fish.setType(-1);
		// sendMessage("Hook x,y: " + _x + "," + _y + " - Water Z, Player Z:" + _z + ", " + getZ()); //debug line, uncoment to show coordinates used in fishing.
		efs = new ExFishingStart(this, _fish.getType(), _x, _y, _z, _lure.isNightLure());
		broadcastPacket(efs);
		startLookingForFishTask();
	}
	
	public void stopLookingForFishTask()
	{
		if (_taskforfish != null)
		{
			_taskforfish.cancel(false);
			_taskforfish = null;
		}
	}
	
	public void startLookingForFishTask()
	{
		if (!isDead() && _taskforfish == null)
		{
			int checkDelay = 0;
			boolean isNoob = false;
			boolean isUpperGrade = false;
			if (_lure != null)
			{
				int lureid = _lure.getItemId();
				isNoob = _fish.getGroup() == 0;
				isUpperGrade = _fish.getGroup() == 2;
				if (lureid == 6519 || lureid == 6522 || lureid == 6525 || lureid == 8505 || lureid == 8508 || lureid == 8511) // low grade
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.33)));
				else if (lureid == 6520 || lureid == 6523 || lureid == 6526 || (lureid >= 8505 && lureid <= 8513) || (lureid >= 7610 && lureid <= 7613) || (lureid >= 7807 && lureid <= 7809) || (lureid >= 8484 && lureid <= 8486)) // medium grade, beginner, prize-winning & quest special bait
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.00)));
				else if (lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510 || lureid == 8513) // high grade
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (0.66)));
			}
			_taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(), isNoob, isUpperGrade), 10000, checkDelay);
		}
	}
	
	private int GetRandomGroup()
	{
		switch (_lure.getItemId())
		{
			case 7807: // green for beginners
			case 7808: // purple for beginners
			case 7809: // yellow for beginners
			case 8486: // prize-winning for beginners
				return 0;
			case 8485: // prize-winning luminous
			case 8506: // green luminous
			case 8509: // purple luminous
			case 8512: // yellow luminous
				return 2;
			default:
				return 1;
		}
	}
	
	private int GetRandomFishType(int group)
	{
		int check = Rnd.get(100);
		int type = 1;
		switch (group)
		{
			case 0: // fish for novices
				switch (_lure.getItemId())
				{
					case 7807: // green lure, preferred by fast-moving (nimble) fish (type 5)
						if (check <= 54)
							type = 5;
						else if (check <= 77)
							type = 4;
						else
							type = 6;
						break;
					case 7808: // purple lure, preferred by fat fish (type 4)
						if (check <= 54)
							type = 4;
						else if (check <= 77)
							type = 6;
						else
							type = 5;
						break;
					case 7809: // yellow lure, preferred by ugly fish (type 6)
						if (check <= 54)
							type = 6;
						else if (check <= 77)
							type = 5;
						else
							type = 4;
						break;
					case 8486: // prize-winning fishing lure for beginners
						if (check <= 33)
							type = 4;
						else if (check <= 66)
							type = 5;
						else
							type = 6;
						break;
				}
				break;
			case 1: // normal fish
				switch (_lure.getItemId())
				{
					case 7610:
					case 7611:
					case 7612:
					case 7613:
						type = 3;
						break;
					case 6519: // all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
					case 8505:
					case 6520:
					case 6521:
					case 8507:
						if (check <= 54)
							type = 1;
						else if (check <= 74)
							type = 0;
						else if (check <= 94)
							type = 2;
						else
							type = 3;
						break;
					case 6522: // all theese lures (purple) are prefered by fat fish (type 0)
					case 8508:
					case 6523:
					case 6524:
					case 8510:
						if (check <= 54)
							type = 0;
						else if (check <= 74)
							type = 1;
						else if (check <= 94)
							type = 2;
						else
							type = 3;
						break;
					case 6525: // all theese lures (yellow) are prefered by ugly fish (type 2)
					case 8511:
					case 6526:
					case 6527:
					case 8513:
						if (check <= 55)
							type = 2;
						else if (check <= 74)
							type = 1;
						else if (check <= 94)
							type = 0;
						else
							type = 3;
						break;
					case 8484: // prize-winning fishing lure
						if (check <= 33)
							type = 0;
						else if (check <= 66)
							type = 1;
						else
							type = 2;
						break;
				}
				break;
			case 2: // upper grade fish, luminous lure
				switch (_lure.getItemId())
				{
					case 8506: // green lure, preferred by fast-moving (nimble) fish (type 8)
						if (check <= 54)
							type = 8;
						else if (check <= 77)
							type = 7;
						else
							type = 9;
						break;
					case 8509: // purple lure, preferred by fat fish (type 7)
						if (check <= 54)
							type = 7;
						else if (check <= 77)
							type = 9;
						else
							type = 8;
						break;
					case 8512: // yellow lure, preferred by ugly fish (type 9)
						if (check <= 54)
							type = 9;
						else if (check <= 77)
							type = 8;
						else
							type = 7;
						break;
					case 8485: // prize-winning fishing lure
						if (check <= 33)
							type = 7;
						else if (check <= 66)
							type = 8;
						else
							type = 9;
						break;
				}
		}
		return type;
	}
	
	private int GetRandomFishLvl()
	{
		L2Effect[] effects = getAllEffects();
		int skilllvl = getSkillLevel(1315);
		for (L2Effect e : effects)
		{
			if (e.getSkill().getId() == 2274)
				skilllvl = (int) e.getSkill().getPower(this);
		}
		if (skilllvl <= 0)
			return 1;
		int randomlvl;
		int check = Rnd.get(100);
		if (check <= 50)
		{
			randomlvl = skilllvl;
		}
		else if (check <= 85)
		{
			randomlvl = skilllvl - 1;
			if (randomlvl <= 0)
			{
				randomlvl = 1;
			}
		}
		else
		{
			randomlvl = skilllvl + 1;
			if (randomlvl > 27)
				randomlvl = 27;
		}
		return randomlvl;
	}
	
	public void startFishCombat(boolean isNoob, boolean isUpperGrade)
	{
		_fishCombat = new L2Fishing(this, _fish, isNoob, isUpperGrade);
	}
	
	public void endFishing(boolean win)
	{
		ExFishingEnd efe = new ExFishingEnd(win, this);
		broadcastPacket(efe);
		_fishing = false;
		_fishx = 0;
		_fishy = 0;
		_fishz = 0;
		broadcastUserInfo();
		if (_fishCombat == null)
			sendPacket(new SystemMessage(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY));
		_fishCombat = null;
		_lure = null;
		// Ends fishing
		sendPacket(new SystemMessage(SystemMessageId.REEL_LINE_AND_STOP_FISHING));
		setIsImmobilized(false);
		stopLookingForFishTask();
		if (win)
		{
			getCounters().fishCaught++;
		}
	}
	
	public L2Fishing getFishCombat()
	{
		return _fishCombat;
	}
	
	public int getFishx()
	{
		return _fishx;
	}
	
	public int getFishy()
	{
		return _fishy;
	}
	
	public int getFishz()
	{
		return _fishz;
	}
	
	public void setLure(L2ItemInstance lure)
	{
		_lure = lure;
	}
	
	public L2ItemInstance getLure()
	{
		return _lure;
	}
	
	public int getInventoryLimit()
	{
		int ivlim;
		if (isGM())
			ivlim = Config.INVENTORY_MAXIMUM_GM;
		else if (getRace() == Race.Dwarf)
			ivlim = Config.INVENTORY_MAXIMUM_DWARF;
		else
			ivlim = Config.INVENTORY_MAXIMUM_NO_DWARF;
		ivlim += (int) getStat().calcStat(Stats.INV_LIM, 0, null, null);
		return ivlim;
	}
	
	public int getWareHouseLimit()
	{
		int whlim;
		if (getRace() == Race.Dwarf)
			whlim = Config.WAREHOUSE_SLOTS_DWARF;
		else
			whlim = Config.WAREHOUSE_SLOTS_NO_DWARF;
		whlim += (int) getStat().calcStat(Stats.WH_LIM, 0, null, null);
		return whlim;
	}
	
	public int getPrivateSellStoreLimit()
	{
		int pslim;
		if (getRace() == Race.Dwarf)
			pslim = Config.MAX_PVTSTORESELL_SLOTS_DWARF;
		else
			pslim = Config.MAX_PVTSTORESELL_SLOTS_OTHER;
		pslim += (int) getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);
		return pslim;
	}
	
	public int getPrivateBuyStoreLimit()
	{
		int pblim;
		if (getRace() == Race.Dwarf)
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_DWARF;
		else
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_OTHER;
		pblim += (int) getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);
		return pblim;
	}
	
	public int getFreightLimit()
	{
		return Config.FREIGHT_SLOTS + (int) getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
	}
	
	public int getDwarfRecipeLimit()
	{
		int recdlim = Config.DWARF_RECIPE_LIMIT;
		recdlim += (int) getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
		return recdlim;
	}
	
	public int getCommonRecipeLimit()
	{
		int recclim = Config.COMMON_RECIPE_LIMIT;
		recclim += (int) getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
		return recclim;
	}
	
	/**
	 * @return Returns the mountNpcId.
	 */
	public int getMountNpcId()
	{
		return _mountNpcId;
	}
	
	/**
	 * @return Returns the mountLevel.
	 */
	public int getMountLevel()
	{
		return _mountLevel;
	}
	
	public void setMountObjectID(int newID)
	{
		_mountObjectID = newID;
	}
	
	public int getMountObjectID()
	{
		return _mountObjectID;
	}
	
	private L2ItemInstance	_lure					= null;
	public int				_shortBuffTaskSkillId	= 0;
	
	/**
	 * Get the current skill in use or return null.<BR>
	 * <BR>
	 */
	public SkillDat getCurrentSkill()
	{
		return _currentSkill;
	}
	
	/**
	 * Create a new SkillDat object and set the player _currentSkill.<BR>
	 * <BR>
	 */
	public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			if (Config.DEBUG)
				_log.info("Setting current skill: NULL for " + getName() + ".");
			_currentSkill = null;
			return;
		}
		if (Config.DEBUG)
			_log.info("Setting current skill: " + currentSkill.getName() + " (ID: " + currentSkill.getId() + ") for " + getName() + ".");
		_currentSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * Get the current pet skill in use or return null.<br>
	 * <br>
	 */
	public SkillDat getCurrentPetSkill()
	{
		return _currentPetSkill;
	}
	
	/**
	 * Create a new SkillDat object and set the player _currentPetSkill.<br>
	 * <br>
	 */
	public void setCurrentPetSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			if (Config.DEBUG)
				_log.info("Setting current pet skill: NULL for " + getName() + ".");
			_currentPetSkill = null;
			return;
		}
		if (Config.DEBUG)
			_log.info("Setting current Pet skill: " + currentSkill.getName() + " (ID: " + currentSkill.getId() + ") for " + getName() + ".");
		_currentPetSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
	}
	
	public SkillDat getQueuedSkill()
	{
		return _queuedSkill;
	}
	
	/**
	 * Create a new SkillDat object and queue it in the player _queuedSkill.<BR>
	 * <BR>
	 */
	public void setQueuedSkill(L2Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (queuedSkill == null)
		{
			if (Config.DEBUG)
				_log.info("Setting queued skill: NULL for " + getName() + ".");
			_queuedSkill = null;
			return;
		}
		if (Config.DEBUG)
			_log.info("Setting queued skill: " + queuedSkill.getName() + " (ID: " + queuedSkill.getId() + ") for " + getName() + ".");
		_queuedSkill = new SkillDat(queuedSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * returns punishment level of player
	 * 
	 * @return
	 */
	public PunishLevel getPunishLevel()
	{
		return _punishLevel;
	}
	
	/**
	 * @return True if player is jailed
	 */
	public boolean isInJail()
	{
		return _punishLevel == PunishLevel.JAIL;
	}
	
	/**
	 * @return True if player is chat banned
	 */
	public boolean isChatBanned()
	{
		return _punishLevel == PunishLevel.CHAT;
	}
	
	public void setPunishLevel(int state)
	{
		switch (state)
		{
			case 0:
			{
				_punishLevel = PunishLevel.NONE;
				break;
			}
			case 1:
			{
				_punishLevel = PunishLevel.CHAT;
				break;
			}
			case 2:
			{
				_punishLevel = PunishLevel.JAIL;
				break;
			}
			case 3:
			{
				/* _punishLevel = PunishLevel.CHAR; */
				break;
			}
			case 4:
			{
				_punishLevel = PunishLevel.ACC;
				break;
			}
		}
	}
	
	/**
	 * Sets punish level for player based on delay
	 * 
	 * @param state
	 * @param delayInMinutes
	 *            0 - Indefinite
	 */
	@SuppressWarnings("incomplete-switch")
	public void setPunishLevel(PunishLevel state, int delayInMinutes)
	{
		long delayInMilliseconds = delayInMinutes * 60000L;
		switch (state)
		{
			case NONE: // Remove Punishments
			{
				switch (_punishLevel)
				{
					case CHAT:
					{
						_punishLevel = state;
						stopPunishTask(true);
						sendPacket(new EtcStatusUpdate(this));
						sendMessage("Your Chat ban has been lifted");
						break;
					}
					case JAIL:
					{
						_punishLevel = state;
						// Open a Html message to inform the player
						NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
						String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_out.htm");
						if (jailInfos != null)
							htmlMsg.setHtml(jailInfos);
						else
							htmlMsg.setHtml("<html><body>You are free for now, respect server rules!</body></html>");
						sendPacket(htmlMsg);
						stopPunishTask(true);
						teleToLocation(17836, 170178, -3507, true); // Floran
						break;
					}
				}
				break;
			}
			case CHAT: // Chat Ban
			{
				_punishLevel = state;
				_punishTimer = 0;
				sendPacket(new EtcStatusUpdate(this));
				// Remove the task if any
				stopPunishTask(false);
				if (delayInMinutes > 0)
				{
					_punishTimer = delayInMilliseconds;
					// start the countdown
					_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
					sendMessage("You are chat banned for " + delayInMinutes + " minutes.");
				}
				else
					sendMessage("You have been chat banned");
				break;
			}
			case JAIL: // Jail Player
			{
				_punishLevel = state;
				_punishTimer = 0;
				// Remove the task if any
				stopPunishTask(false);
				if (delayInMinutes > 0)
				{
					_punishTimer = delayInMilliseconds;
					// start the countdown
					_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
					sendMessage("You are in jail for " + delayInMinutes + " minutes.");
				}
				if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(getObjectId()))
					TvTEvent.removeParticipant(getObjectId());
				if (Olympiad.getInstance().isRegisteredInComp(this))
					Olympiad.getInstance().removeDisconnectedCompetitor(this);
				// Open a Html message to inform the player
				NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
				String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_in.htm");
				if (jailInfos != null)
					htmlMsg.setHtml(jailInfos);
				else
					htmlMsg.setHtml("<html><body>You have been put in jail by an admin.</body></html>");
				sendPacket(htmlMsg);
				setInstanceId(0);
				setIsIn7sDungeon(false);
				teleToLocation(-114356, -249645, -2984, false); // Jail
				break;
			}
			case CHAR: // kick character
			{
				/* setAccessLevel(-100); */
				logout();
				RegionBBSManager.getInstance().changeCommunityBoard();
				break;
			}
			case ACC: // Ban Account
			{
				setAccountAccesslevel(-100);
				logout();
				RegionBBSManager.getInstance().changeCommunityBoard();
				break;
			}
			default:
			{
				_punishLevel = state;
				break;
			}
		}
		// store in database
		storeCharBase();
	}
	
	public long getPunishTimer()
	{
		return _punishTimer;
	}
	
	public void setPunishTimer(long time)
	{
		_punishTimer = time;
	}
	
	private void updatePunishState()
	{
		if (getPunishLevel() != PunishLevel.NONE)
		{
			// If punish timer exists, restart punishtask.
			if (_punishTimer > 0)
			{
				_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
				sendMessage("You are still " + getPunishLevel().string() + " for " + Math.round(_punishTimer / 60000) + " minutes.");
			}
			if (getPunishLevel() == PunishLevel.JAIL)
			{
				// If player escaped, put him back in jail
				if (!isInsideZone(ZONE_JAIL))
					teleToLocation(-114356, -249645, -2984, true);
			}
		}
	}
	
	public void stopPunishTask(boolean save)
	{
		if (_punishTask != null)
		{
			if (save)
			{
				long delay = _punishTask.getDelay(TimeUnit.MILLISECONDS);
				if (delay < 0)
					delay = 0;
				setPunishTimer(delay);
			}
			_punishTask.cancel(false);
			_punishTask = null;
		}
	}
	
	private class PunishTask implements Runnable
	{
		L2PcInstance _player;
		
		protected PunishTask(L2PcInstance player)
		{
			_player = player;
		}
		
		public void run()
		{
			_player.setPunishLevel(PunishLevel.NONE, 0);
		}
	}
	
	public void startFameTask(long delay, int fameFixRate)
	{
		if (getLevel() < 40 || getClassId().level() < 2)
			return;
		if (_fameTask == null)
			_fameTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FameTask(this, fameFixRate), delay, delay);
	}
	
	public void stopFameTask()
	{
		if (_fameTask != null)
		{
			_fameTask.cancel(false);
			_fameTask = null;
		}
	}
	
	private class FameTask implements Runnable
	{
		private final L2PcInstance	_player;
		private final int			_value;
		
		protected FameTask(L2PcInstance player, int value)
		{
			_player = player;
			_value = value;
		}
		
		public void run()
		{
			if (_player == null || _player.isDead())
				return;
			_player.setFame(_player.getFame() + _value);
			SystemMessage sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
			sm.addNumber(_value);
			_player.sendPacket(sm);
		}
	}
	
	/**
	 * @return
	 */
	public int getPowerGrade()
	{
		return _powerGrade;
	}
	
	/**
	 * @return
	 */
	public void setPowerGrade(int power)
	{
		_powerGrade = power;
	}
	
	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquippedId != 0;
	}
	
	public void setCursedWeaponEquippedId(int value)
	{
		_cursedWeaponEquippedId = value;
	}
	
	public int getCursedWeaponEquippedId()
	{
		return _cursedWeaponEquippedId;
	}
	
	public boolean isCombatFlagEquipped()
	{
		return _isCombatFlagEquipped;
	}
	
	public void setCombatFlagEquipped(boolean value)
	{
		_isCombatFlagEquipped = value;
		if (_isCombatFlagEquipped && isInvisible() && !isGM())
			stopEffects(L2EffectType.INVISIBLE);
	}
	
	public boolean getCharmOfCourage()
	{
		return _charmOfCourage;
	}
	
	public void setCharmOfCourage(boolean val)
	{
		_charmOfCourage = val;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public final void setIsRidingStrider(boolean mode)
	{
		_isRidingStrider = mode;
	}
	/*
	 * not used anymore
	 * public final void setIsRidingFenrirWolf(boolean mode)
	 * {
	 * _isRidingFenrirWolf = mode;
	 * }
	 * public final void setIsRidingWFenrirWolf(boolean mode)
	 * {
	 * _isRidingWFenrirWolf = mode;
	 * }
	 * public final void setIsRidingGreatSnowWolf(boolean mode)
	 * {
	 * _isRidingGreatSnowWolf = mode;
	 * }
	 * public final boolean isRidingFenrirWolf()
	 * {
	 * return _isRidingFenrirWolf;
	 * }
	 * public final boolean isRidingWFenrirWolf()
	 * {
	 * return _isRidingWFenrirWolf;
	 * }
	 * public final boolean isRidingGreatSnowWolf()
	 * {
	 * return _isRidingGreatSnowWolf;
	 * }
	 */
	
	public final boolean isRidingStrider()
	{
		return _isRidingStrider;
	}
	
	/**
	 * Returns the Number of Souls this L2PcInstance got.
	 * 
	 * @return
	 */
	public int getSouls()
	{
		return _souls;
	}
	
	/**
	 * Absorbs a Soul from a Npc.
	 * 
	 * @param skill
	 * @param target
	 */
	public void absorbSoul(L2Skill skill, L2Npc npc)
	{
		if (_souls >= calcStat(Stats.SOUL_MAX, skill.getNumSouls(), null, null))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.SOUL_CANNOT_BE_INCREASED_ANYMORE);
			sendPacket(sm);
			return;
		}
		increaseSouls(1);
		if (npc != null)
			broadcastPacket(new ExSpawnEmitter(this, npc), 500);
	}
	
	/**
	 * Increase Souls
	 * 
	 * @param count
	 */
	public void increaseSouls(int count)
	{
		if (count < 0 || count > 55)
			return;
		_souls += count;
		if (getSouls() > 55)
			_souls = 55;
		SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SOUL_HAS_INCREASED_BY_S1_SO_IT_IS_NOW_AT_S2);
		sm.addNumber(count);
		sm.addNumber(_souls);
		sendPacket(sm);
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Decreases existing Souls.
	 * 
	 * @param count
	 */
	public boolean decreaseSouls(int count, L2Skill skill) // SHOULD ONLY BE CALLED AFTER USING A SKILL
	{
		if (getSouls() <= 0)
		{
			if (skill.getSoulConsumeCount() > 0)
			{
				sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NOT_ENOUGH_SOUL));
				return false;
			}
			_lastConsumedSoulAmount = 0;
			return true;
		}
		_lastConsumedSoulAmount = (byte) Math.min(count, getSouls());
		_souls -= _lastConsumedSoulAmount;
		if (_souls < 0)
			_souls = 0;
		sendPacket(new EtcStatusUpdate(this));
		return true;
	}
	
	public byte getLastConsumedSoulAmount()
	{
		return _lastConsumedSoulAmount;
	}
	
	/**
	 * Clear out all Souls from this L2PcInstance
	 */
	public void clearSouls()
	{
		_souls = 0;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * @param magicId
	 * @param level
	 * @param time
	 */
	public void shortBuffStatusUpdate(int magicId, int level, int time)
	{
		if (_shortBuffTask != null)
		{
			try
			{
				_shortBuffTask.cancel(false);
			}
			catch (NullPointerException e)
			{}
			_shortBuffTask = null;
		}
		_shortBuffTask = ThreadPoolManager.getInstance().scheduleGeneral(new ShortBuffTask(this), time * 1000);
		setShortBuffTaskSkillId(magicId);
		sendPacket(new ShortBuffStatusUpdate(magicId, level, time));
	}
	
	public void setShortBuffTaskSkillId(int id)
	{
		_shortBuffTaskSkillId = id;
	}
	
	public int getDeathPenaltyBuffLevel()
	{
		return _deathPenaltyBuffLevel;
	}
	
	public void setDeathPenaltyBuffLevel(int level)
	{
		_deathPenaltyBuffLevel = level;
	}
	
	public void increaseDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() >= 15) // maximum level reached
			return;
		if (getDeathPenaltyBuffLevel() != 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());
			if (skill != null)
				removeSkill(skill, true);
		}
		_deathPenaltyBuffLevel++;
		addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
		sendPacket(new EtcStatusUpdate(this));
		SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
		sm.addNumber(getDeathPenaltyBuffLevel());
		sendPacket(sm);
	}
	
	public void reduceDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() <= 0)
			return;
		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());
		if (skill != null)
			removeSkill(skill, true);
		_deathPenaltyBuffLevel--;
		if (getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
			sendPacket(new EtcStatusUpdate(this));
			SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
			sm.addNumber(getDeathPenaltyBuffLevel());
			sendPacket(sm);
		}
		else
		{
			sendPacket(new EtcStatusUpdate(this));
			sendPacket(new SystemMessage(SystemMessageId.DEATH_PENALTY_LIFTED));
		}
	}
	
	public void restoreDeathPenaltyBuffLevel()
	{
		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());
		if (skill != null)
			removeSkill(skill, true);
		if (getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
		}
	}
	
	private final FastMap<Integer, TimeStamp>	_reuseTimeStamps	= new FastMap<Integer, TimeStamp>().shared();
	private boolean								_canFeed;
	private boolean								_isInSiege;
	
	public Collection<TimeStamp> getReuseTimeStamps()
	{
		return _reuseTimeStamps.values();
	}
	
	public FastMap<Integer, TimeStamp> getReuseTimeStamp()
	{
		return _reuseTimeStamps;
	}
	
	/**
	 * Simple class containing all neccessary information to maintain
	 * valid timestamps and reuse for skills upon relog. Filter this
	 * carefully as it becomes redundant to store reuse for small delays.
	 * 
	 * @author Yesod
	 */
	public static class TimeStamp
	{
		private final int	skill;
		private final long	reuse;
		private final long	stamp;
		
		public TimeStamp(int _skill, long _reuse)
		{
			skill = _skill;
			reuse = _reuse;
			stamp = System.currentTimeMillis() + reuse;
		}
		
		public TimeStamp(int _skill, long _reuse, long _systime)
		{
			skill = _skill;
			reuse = _reuse;
			stamp = _systime;
		}
		
		public long getStamp()
		{
			return stamp;
		}
		
		public int getSkill()
		{
			return skill;
		}
		
		public long getReuse()
		{
			return reuse;
		}
		
		public long getRemaining()
		{
			return Math.max(stamp - System.currentTimeMillis(), 0);
		}
		
		/*
		 * Check if the reuse delay has passed and
		 * if it has not then update the stored reuse time
		 * according to what is currently remaining on
		 * the delay.
		 */
		public boolean hasNotPassed()
		{
			if (System.currentTimeMillis() < stamp)
			{
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Index according to skill id the current
	 * timestamp of use.
	 * 
	 * @param skillid
	 * @param reuse
	 *            delay
	 */
	@Override
	public void addTimeStamp(int s, int r)
	{
		_reuseTimeStamps.put(s, new TimeStamp(s, r));
	}
	
	/**
	 * Index according to skill this TimeStamp
	 * instance for restoration purposes only.
	 * 
	 * @param TimeStamp
	 */
	public void addTimeStamp(TimeStamp ts)
	{
		_reuseTimeStamps.put(ts.getSkill(), ts);
	}
	
	/**
	 * Index according to skill id the current
	 * timestamp of use.
	 * 
	 * @param skillid
	 */
	@Override
	public void removeTimeStamp(int s)
	{
		_reuseTimeStamps.remove(s);
	}
	
	@Override
	public L2PcInstance getActingPlayer()
	{
		return this;
	}
	
	@Override
	public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		sendDamageMessage(target, damage, mcrit, pcrit, miss, false);
	}
	
	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss, boolean extra)
	{
		// Check if hit is missed
		if (miss)
		{
			sendPacket(new SystemMessage(SystemMessageId.C1_ATTACK_WENT_ASTRAY).addString(getDisplayName()));
			return;
		}
		// Check if hit is critical
		if (pcrit)
		{
			if (extra)
				sendPacket(new SystemMessage(SystemMessageId.EXTRA_CRITICAL_HIT));
			else
				sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT));
			if (target instanceof L2Npc && getSkillLevel(467) > 0)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(467, getSkillLevel(467));
				if (Rnd.get(100) < skill.getCritChance())
				{
					absorbSoul(skill, ((L2Npc) target));
				}
			}
		}
		else if (mcrit)
			sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_MAGIC));
		if (isInOlympiadMode() && target instanceof L2PcInstance && ((L2PcInstance) target).isInOlympiadMode() && ((L2PcInstance) target).getOlympiadGameId() == getOlympiadGameId())
		{
			Olympiad.getInstance().notifyCompetitorDamage(this, damage, getOlympiadGameId());
		}
		SystemMessage sm = new SystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED);
		if (target.isInvul())
		{
			if (target.isGM() || (target instanceof L2Playable && target.getActingPlayer().isSpawnProtected()))
			{
				damage = 0;
			}
			else
			{
				damage = (int) (calcStat(Stats.DMG_ADD, 0, target, null) - target.calcStat(Stats.DMG_REMOVE, 0, this, null) - target.calcStat(Stats.DMG_REMOVE_SHIELD, 0, this, null));
			}
		}
		if (damage > 0)
		{
			if (target instanceof L2DoorInstance || target instanceof L2ControlTowerInstance)
			{
				sm = new SystemMessage(SystemMessageId.YOU_DID_S1_DMG);
				sm.addNumber(damage);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.C1_GAVE_C2_DAMAGE_OF_S3);
				sm.addPcName(this);
				if (target.isInvisible() && !canSeeInvisiblePeople())
					sm.addString("something invisible");
				else
					sm.addCharName(target);
				sm.addNumber(damage);
			}
		}
		sendPacket(sm);
	}
	
	public int increaseEmbryoDamageDealt(int damageDealt)
	{
		return EMBRYO_DAMAGE_DEALT += damageDealt;
	}
	
	public int getEmbryoDamageDealt()
	{
		return EMBRYO_DAMAGE_DEALT;
	}
	
	/**
	 * @param npcId
	 */
	public void setAgathionId(int npcId)
	{
		_agathionId = npcId;
	}
	
	/**
	 * @return
	 */
	public int getAgathionId()
	{
		return _agathionId;
	}
	/*    *//**
			 * Returns the VL <BR>
			 * <BR>
			 * 
			 * @return
			 */
	/*
	 * public int getVitalityLevel()
	 * {
	 * return _vitalityLevel;
	 * }
	 *//**
		 * Sets VL of this L2PcInstance<BR>
		 * <BR>
		 * 
		 * @param level
		 *//*
			 * public void setVitalityLevel(int level)
			 * {
			 * if (level > 5)
			 * level = 5;
			 * else if (level < 0)
			 * level = 0;
			 * _vitalityLevel = level;
			 * }
			 */
	
	/*
	 * Function for skill summon friend or Gate Chant.
	 */
	/** Request Teleport **/
	public boolean teleportRequest(L2PcInstance requester, L2Skill skill)
	{
		if (_summonRequest.getTarget() != null && requester != null)
			return false;
		_summonRequest.setTarget(requester, skill);
		return true;
	}
	
	/** Action teleport **/
	public void teleportAnswer(int answer, int requesterId)
	{
		if (_summonRequest.getTarget() == null)
			return;
		if (answer == 1 && _summonRequest.getTarget().getCharId() == requesterId)
		{
			teleToTarget(this, _summonRequest.getTarget(), _summonRequest.getSkill());
		}
		_summonRequest.setTarget(null, null);
	}
	
	public void teleToTarget(L2PcInstance targetChar, L2PcInstance summonerChar, L2Skill summonSkill)
	{
		if (targetChar == null || summonerChar == null || summonSkill == null)
			return;
		if (!checkSummonerStatus(summonerChar))
			return;
		if (!checkTargetStatus(targetChar, summonerChar))
			return;
		int itemConsumeId = summonSkill.getTargetConsumeId();
		int itemConsumeCount = summonSkill.getTargetConsume();
		if (itemConsumeId != 0 && itemConsumeCount != 0)
		{
			// Delete by rocknow
			if (targetChar.getInventory().getInventoryItemCount(itemConsumeId, 0) < itemConsumeCount)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_REQUIRED_FOR_SUMMONING);
				sm.addItemName(summonSkill.getTargetConsumeId());
				targetChar.sendPacket(sm);
				return;
			}
			targetChar.getInventory().destroyItemByItemId("Consume", itemConsumeId, itemConsumeCount, summonerChar, targetChar);
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
			sm.addItemName(summonSkill.getTargetConsumeId());
			targetChar.sendPacket(sm);
		}
		targetChar.teleToLocation(summonerChar.getX(), summonerChar.getY(), summonerChar.getZ(), true);
	}
	
	public static boolean checkSummonerStatus(L2PcInstance summonerChar)
	{
		if (summonerChar == null)
			return false;
		if (summonerChar.isInOlympiadMode())
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return false;
		}
		if (summonerChar.inObserverMode())
		{
			return false;
		}
		if (summonerChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}
		return true;
	}
	
	public static boolean checkTargetStatus(L2PcInstance targetChar, L2PcInstance summonerChar)
	{
		if (targetChar == null)
			return false;
		if (targetChar.isAlikeDead())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}
		if (targetChar.isInStoreMode())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}
		if (targetChar.isRooted() || targetChar.isInCombat())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}
		if (targetChar.isInOlympiadMode())
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD));
			return false;
		}
		if (targetChar.isFestivalParticipant())
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}
		if (targetChar.inObserverMode())
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}
		if (targetChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_IN_SUMMON_BLOCKING_AREA);
			sm.addString(targetChar.getName());
			summonerChar.sendPacket(sm);
			return false;
		}
		// on retail character can enter 7s dungeon with summon friend,
		// but will be teleported away by mobs
		// because currently this is not working in L2J we do not allowing summoning
		if (summonerChar.isIn7sDungeon())
		{
			int targetCabal = SevenSigns.getInstance().getPlayerCabal(targetChar);
			if (SevenSigns.getInstance().isSealValidationPeriod())
			{
				if (targetCabal != SevenSigns.getInstance().getCabalHighestScore())
				{
					summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
					return false;
				}
			}
			else
			{
				if (targetCabal == SevenSigns.CABAL_NULL)
				{
					summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
					return false;
				}
			}
		}
		return true;
	}
	
	public void gatesRequest(L2DoorInstance door)
	{
		_gatesRequest.setTarget(door);
	}
	
	public void gatesAnswer(int answer, int type)
	{
		if (_gatesRequest.getDoor() == null)
			return;
		if (answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 1)
			_gatesRequest.getDoor().openMe();
		else if (answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 0)
			_gatesRequest.getDoor().closeMe();
		_gatesRequest.setTarget(null);
	}
	
	@Override
	public void setIsCastingNow(boolean value)
	{
		if (value == false)
			_currentSkill = null;
		super.setIsCastingNow(value);
	}
	
	public void checkItemRestriction()
	{
		boolean nigger = false;
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			L2ItemInstance equippedItem = getInventory().getPaperdollItem(i);
			if (equippedItem != null && (!equippedItem.getItem().checkCondition(this, this, false) || (isInOlympiadMode() && equippedItem.isOlyRestrictedItem())))
			{
				getInventory().unEquipItemInSlotAndRecord(i);
				if (equippedItem.isWear())
					continue;
				SystemMessage sm = null;
				if (equippedItem.getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(equippedItem.getEnchantLevel());
					sm.addItemName(equippedItem);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(equippedItem);
				}
				sendPacket(sm);
				nigger = true;
			}
		}
		if (nigger)
		{
			if (isInOlympiadMode())
				sendMessage("You can only use S grade items or lower in the Olympiad.");
			broadcastUserInfo();
			// Send the ItemList Server->Client Packet to the player in order to refresh its Inventory
			ItemList il = new ItemList(getInventory().getItems(), true);
			sendPacket(il);
		}
	}
	
	public void checkItemRestrictionZone(int limit)
	{
		boolean update = false;
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			L2ItemInstance equippedItem = getInventory().getPaperdollItem(i);
			if (equippedItem != null && (!equippedItem.getItem().checkCondition(this, this, false) || (equippedItem.isARestrictedItemZone(limit))))
			{
				getInventory().unEquipItemInSlotAndRecord(i);
				if (equippedItem.isWear())
					continue;
				SystemMessage sm = null;
				if (equippedItem.getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(equippedItem.getEnchantLevel());
					sm.addItemName(equippedItem);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(equippedItem);
				}
				sendPacket(sm);
				update = true;
			}
		}
		if (update)
		{
			broadcastUserInfo();
			// Send the ItemList Server->Client Packet to the player in order to refresh its Inventory
			ItemList il = new ItemList(getInventory().getItems(), true);
			sendPacket(il);
		}
	}
	
	/** Section for mounted pets */
	class FeedTask implements Runnable
	{
		public void run()
		{
			try
			{
				if (!isMounted())
				{
					stopFeed();
					return;
				}
				if (getCurrentFeed() > getFeedConsume())
				{
					// eat
					setCurrentFeed(getCurrentFeed() - getFeedConsume());
				}
				else
				{
					// go back to pet control item, or simply said, unsummon it
					setCurrentFeed(0);
					stopFeed();
					dismount();
					sendPacket(new SystemMessage(SystemMessageId.OUT_OF_FEED_MOUNT_CANCELED));
				}
				int[] foodIds = L2PetDataTable.getFoodItemId(getMountNpcId());
				if (foodIds[0] == 0)
					return;
				L2ItemInstance food = null;
				food = getInventory().getItemByItemId(foodIds[0]);
				// use better strider food if exists
				if (L2PetDataTable.isStrider(getMountNpcId()))
				{
					if (getInventory().getItemByItemId(foodIds[1]) != null)
						food = getInventory().getItemByItemId(foodIds[1]);
				}
				if (food != null && isHungry())
				{
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(food.getEtcItem());
					if (handler != null)
					{
						handler.useItem(L2PcInstance.this, food, false);
						SystemMessage sm = new SystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
						sm.addItemName(food.getItemId());
						sendPacket(sm);
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Mounted Pet [NpcId: " + getMountNpcId() + "] a feed task error has occurred", e);
			}
		}
	}
	
	protected synchronized void startFeed(int npcId)
	{
		_canFeed = npcId > 0;
		if (!isMounted())
			return;
		if (getPet() != null)
		{
			setCurrentFeed(((L2PetInstance) getPet()).getCurrentFed());
			_controlItemId = getPet().getControlItemId();
			sendPacket(new SetupGauge(3, getCurrentFeed() * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume()));
			if (!isDead())
			{
				_mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
			}
		}
		else if (_canFeed)
		{
			setCurrentFeed(getMaxFeed());
			SetupGauge sg = new SetupGauge(3, getCurrentFeed() * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume());
			sendPacket(sg);
			if (!isDead())
			{
				_mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
			}
		}
	}
	
	protected synchronized void stopFeed()
	{
		if (_mountFeedTask != null)
		{
			_mountFeedTask.cancel(false);
			_mountFeedTask = null;
			if (Config.DEBUG)
				_log.fine("Pet [#" + _mountNpcId + "] feed task stop");
		}
	}
	
	protected final void clearPetData()
	{
		_data = null;
	}
	
	protected final L2PetData getPetData(int npcId)
	{
		if (_data == null && getPet() != null)
			_data = L2PetDataTable.getInstance().getPetData(getPet().getNpcId(), getPet().getLevel());
		else if (_data == null && npcId > 0)
		{
			_data = L2PetDataTable.getInstance().getPetData(npcId, getLevel());
		}
		return _data;
	}
	
	public int getCurrentFeed()
	{
		return _curFeed;
	}
	
	protected int getFeedConsume()
	{
		// if pet is attacking
		if (isAttackingNow())
			return getPetData(_mountNpcId).getPetFeedBattle();
		else
			return getPetData(_mountNpcId).getPetFeedNormal();
	}
	
	public void setCurrentFeed(int num)
	{
		_curFeed = num > getMaxFeed() ? getMaxFeed() : num;
		SetupGauge sg = new SetupGauge(3, getCurrentFeed() * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume());
		sendPacket(sg);
	}
	
	protected int getMaxFeed()
	{
		return getPetData(_mountNpcId).getPetMaxFeed();
	}
	
	protected boolean isHungry()
	{
		return _canFeed ? (getCurrentFeed() < (0.55 * getPetData(getMountNpcId()).getPetMaxFeed())) : false;
	}
	
	public class dismount implements Runnable
	{
		public void run()
		{
			try
			{
				dismount();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void enteredNoLanding()
	{
		_dismountTask = ThreadPoolManager.getInstance().scheduleGeneral(new L2PcInstance.dismount(), 5000);
	}
	
	public void exitedNoLanding()
	{
		if (_dismountTask != null)
		{
			_dismountTask.cancel(true);
			_dismountTask = null;
		}
	}
	
	public void storePetFood(int petId)
	{
		if (_controlItemId != 0 && petId != 0)
		{
			String req;
			req = "UPDATE pets SET fed=? WHERE item_obj_id = ?";
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(req);
				statement.setInt(1, getCurrentFeed());
				statement.setInt(2, _controlItemId);
				statement.executeUpdate();
				statement.close();
				_controlItemId = 0;
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed to store Pet [NpcId: " + petId + "] data", e);
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
	}
	
	/** End of section for mounted pets */
	/*
	 * @Override
	 * public int getAttackElementValue(byte attribute)
	 * {
	 * int value = super.getAttackElementValue(attribute);
	 * // 50% if summon exist
	 * if (getPet() != null && getClassId().isSummoner() && (getPet() instanceof L2SummonInstance))
	 * return value / 2;
	 * return value;
	 * }
	 */
	private int _eventEffectId = 0;
	
	/**
	 * @return event effect id
	 */
	public int getEventEffectId()
	{
		return _eventEffectId;
	}
	
	public void startEventEffect(int mask)
	{
		_eventEffectId |= mask;
		broadcastUserInfo();
	}
	
	public void stopEventEffect(int mask)
	{
		_eventEffectId &= ~mask;
		broadcastUserInfo();
	}
	
	public void setIsInSiege(boolean b)
	{
		_isInSiege = b;
	}
	
	public boolean isInSiege()
	{
		return _isInSiege;
	}
	
	public FloodProtectors getFloodProtectors()
	{
		return _floodProtectors;
	}
	
	public boolean isFlyingMounted()
	{
		return _isFlyingMounted;
	}
	
	public void setIsFlyingMounted(boolean val)
	{
		_isFlyingMounted = val;
		setIsFlying(val);
	}
	
	/**
	 * Returns the Number of Charges this L2PcInstance got.
	 * 
	 * @return
	 */
	public int getCharges()
	{
		return _charges.get();
	}
	
	final public synchronized void increaseCharges(final int count, final int max)
	{
		if (_charges.get() >= max)
		{
			sendPacket(new SystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED));
			return;
		}
		if (_charges.get() + count >= max)
		{
			_charges.set(max);
			sendPacket(new SystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED));
		}
		else
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1);
			sm.addNumber(_charges.addAndGet(count));
			sendPacket(sm);
		}
		sendPacket(new EtcStatusUpdate(this));
	}
	
	final public synchronized void decreaseCharges(final int count)
	{
		if (_charges.get() <= count)
			_charges.set(0);
		else
			_charges.addAndGet(-count);
		sendPacket(new EtcStatusUpdate(this));
	}
	
	final public void clearCharges()
	{
		_charges.set(0);
		sendPacket(new EtcStatusUpdate(this));
	}
	/*    *//**
			 * Starts/Restarts the ChargeTask to Clear Charges after 10 Mins.
			 *//*
				 * private void restartChargeTask()
				 * {
				 * if (_chargeTask != null)
				 * {
				 * _chargeTask.cancel(false);
				 * _chargeTask = null;
				 * }
				 * _chargeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ChargeTask(this), 600000);
				 * }
				 */
	
	/*    *//**
			 * Stops the Charges Clearing Task.
			 *//*
				 * public void stopChargeTask()
				 * {
				 * if (_chargeTask != null)
				 * {
				 * _chargeTask.cancel(false);
				 * _chargeTask = null;
				 * }
				 * }
				 */
	/*
	 * private class ChargeTask implements Runnable
	 * {
	 * L2PcInstance _player;
	 * protected ChargeTask(L2PcInstance player)
	 * {
	 * _player = player;
	 * }
	 * public void run()
	 * {
	 * _player.clearCharges();
	 * }
	 * }
	 */
	public class TeleportBookmark
	{
		public int		_id, _x, _y, _z, _icon;
		public String	_name, _tag;
		
		TeleportBookmark(int id, int x, int y, int z, int icon, String tag, String name)
		{
			_id = id;
			_x = x;
			_y = y;
			_z = z;
			_icon = icon;
			_name = name;
			_tag = tag;
		}
	}
	
	public void TeleportBookmarkModify(int Id, int icon, String tag, String name)
	{
		int count = 0;
		int size = tpbookmark.size();
		while (size > count)
		{
			if (tpbookmark.get(count)._id == Id)
			{
				tpbookmark.get(count)._icon = icon;
				tpbookmark.get(count)._tag = tag;
				tpbookmark.get(count)._name = name;
				Connection con = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(UPDATE_TP_BOOKMARK);
					statement.setInt(1, icon);
					statement.setString(2, tag);
					statement.setString(3, name);
					statement.setInt(4, getObjectId());
					statement.setInt(5, Id);
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.warning("Could not update character teleport bookmark data: " + e);
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
			count++;
		}
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public void TeleportBookmarkDelete(int Id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_TP_BOOKMARK);
			statement.setInt(1, getObjectId());
			statement.setInt(2, Id);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not delete character teleport bookmark data: " + e);
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
		int count = 0;
		int size = tpbookmark.size();
		while (size > count)
		{
			if (tpbookmark.get(count)._id == Id)
			{
				tpbookmark.remove(count);
				break;
			}
			count++;
		}
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public void TeleportBookmarkGo(int Id)
	{
		if (!TeleportBookmarkCondition(0) || this == null)
			return;
		if (getInventory().getInventoryItemCount(13016, 0) == 0)
		{
			sendPacket(new SystemMessage(2359));
			return;
		}
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(13016);
		sendPacket(sm);
		int count = 0;
		int size = tpbookmark.size();
		while (size > count)
		{
			if (tpbookmark.get(count)._id == Id)
			{
				destroyItem("Consume", getInventory().getItemByItemId(13016).getObjectId(), 1, null, false);
				this.teleToLocation(tpbookmark.get(count)._x, tpbookmark.get(count)._y, tpbookmark.get(count)._z);
				break;
			}
			count++;
		}
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public boolean TeleportBookmarkCondition(int type)
	{
		if (isInCombat())
		{
			sendPacket(new SystemMessage(2348));
			return false;
		}
		else if (isInSiege())
		{
			sendPacket(new SystemMessage(2349));
			return false;
		}
		else if (isInDuel())
		{
			sendPacket(new SystemMessage(2350));
			return false;
		}
		else if (isFlying())
		{
			sendPacket(new SystemMessage(2351));
			return false;
		}
		else if (isInOlympiadMode())
		{
			sendPacket(new SystemMessage(2352));
			return false;
		}
		else if (isParalyzed())
		{
			sendPacket(new SystemMessage(2353));
			return false;
		}
		else if (isDead())
		{
			sendPacket(new SystemMessage(2354));
			return false;
		}
		else if (isInBoat() || isInAirShip() || isInJail() || isInsideZone(ZONE_NOSUMMONFRIEND))
		{
			if (type == 0)
				sendPacket(new SystemMessage(2355));
			else if (type == 1)
				sendPacket(new SystemMessage(2410));
			return false;
		}
		else if (isInWater())
		{
			sendPacket(new SystemMessage(2356));
			return false;
		}
		/*
		 * TODO: Instant Zone still not implement
		 * else if (this.isInsideZone(ZONE_INSTANT))
		 * {
		 * sendPacket(new SystemMessage(2357));
		 * return;
		 * }
		 */
		else
			return true;
	}
	
	public void TeleportBookmarkAdd(int x, int y, int z, int icon, String tag, String name)
	{
		if (!TeleportBookmarkCondition(1))
			return;
		if (tpbookmark.size() >= _bookmarkslot)
		{
			sendPacket(new SystemMessage(2358));
			return;
		}
		if (getInventory().getInventoryItemCount(20033, 0) == 0)
		{
			sendPacket(new SystemMessage(6501));
			return;
		}
		int count = 0;
		int id = 1;
		FastList<Integer> idlist = new FastList<Integer>();
		int size = tpbookmark.size();
		while (size > count)
		{
			idlist.add(tpbookmark.get(count)._id);
			count++;
		}
		for (int i = 1; i < 10; i++)
		{
			if (!idlist.contains(i))
			{
				id = i;
				break;
			}
		}
		TeleportBookmark tpadd = new TeleportBookmark(id, x, y, z, icon, tag, name);
		if (tpbookmark == null)
			tpbookmark = new FastList<TeleportBookmark>();
		tpbookmark.add(tpadd);
		destroyItem("Consume", getInventory().getItemByItemId(20033).getObjectId(), 1, null, false);
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(20033);
		sendPacket(sm);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_TP_BOOKMARK);
			statement.setInt(1, getObjectId());
			statement.setInt(2, id);
			statement.setInt(3, x);
			statement.setInt(4, y);
			statement.setInt(5, z);
			statement.setInt(6, icon);
			statement.setString(7, tag);
			statement.setString(8, name);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not insert character teleport bookmark data: " + e);
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
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public void restoreTeleportBookmark()
	{
		if (tpbookmark == null)
			tpbookmark = new FastList<TeleportBookmark>();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_TP_BOOKMARK);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				tpbookmark.add(new TeleportBookmark(rset.getInt("Id"), rset.getInt("x"), rset.getInt("y"), rset.getInt("z"), rset.getInt("icon"), rset.getString("tag"), rset.getString("name")));
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed restoing character teleport bookmark.", e);
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
	
	private int lastHealedTime = 0;
	
	final public void setLastHealedTime()
	{
		lastHealedTime = GameTimeController.getGameTicks() + 1800;
	}
	
	final public boolean canBeBufferHealed()
	{
		return GameTimeController.getGameTicks() > lastHealedTime;
	}
	
	private int lastBuffedTime = 0;
	
	final public void setLastBuffedTime()
	{
		lastBuffedTime = GameTimeController.getGameTicks() + 30;
	}
	
	final public boolean canBeBufferBuffed()
	{
		return GameTimeController.getGameTicks() > lastBuffedTime;
	}
	
	private String	_token		= null;
	private int		_prevKill	= 0;
	private int		_prevKill2	= 0;
	public int		_streak		= 0;
	private int		_famestreak	= 0;
	public boolean	_tempHero	= false;
	public boolean	_fakeHero	= false;
	public boolean	_fosHero	= false;
	/*
	 * private Future<?> walkBonusDelayTimer = null;
	 * private boolean walkBonus = false;
	 */
	private String	_prev1IP	= "none";
	private String	_prev2IP	= "none";
	private String	_nameC		= "none";
	private String	_titleC		= "none";
	
	final private int getSupportIncreasePvPChance()
	{
		if (isHealerClass())
			return 24;
		if (isTankClass())
			return 10;
		return 7; // should be just judicators and warcryers and ols
	}
	
	public void showClanNotice()
	{
		if (getClan() != null && getClan().isNoticeEnabled())
		{
			NpcHtmlMessage notice = new NpcHtmlMessage(1);
			notice.setFile("data/html/clanNotice.htm");
			notice.replace("%clan_name%", getClan().getName());
			String notice1 = getClan().getNotice();
			notice1 = notice1.replaceAll("\r\n", "<br>");
			notice1 = notice1.replace("[", "\\[");
			notice1 = notice1.replace("]", "\\]");
			notice1 = notice1.replace("(", "\\(");
			notice1 = notice1.replace(")", "\\)");
			notice1 = notice1.replace("&", "\\&");
			notice1 = notice1.replace("@", "\\@");
			notice1 = notice1.replace("{", "\\{");
			notice1 = notice1.replace("}", "\\}");
			notice1 = notice1.replace("?", "\\?");
			notice1 = notice1.replace("+", "\\+");
			notice1 = notice1.replace("-", "\\-");
			notice1 = notice1.replace("=", "\\=");
			notice1 = notice1.replace("^", "\\^");
			notice.replace("%notice_text%", notice1);
			sendPacket(notice);
		}
		else
			return;
	}
	
	public boolean isInClanwarWith(L2PcInstance target)
	{
		if (target == null)
			return false;
		if (getClan() == null || target.getClan() == null)
			return false;
		if (getClan().isAtWarWith(target.getClanId()) && target.getClan().isAtWarWith(getClanId()))
			return true;
		return false;
	}
	
	public boolean isInOneSideClanwarWith(L2PcInstance target)
	{
		if (target == null)
			return false;
		if (getClan() == null || target.getClan() == null)
			return false;
		if (getClan().isAtWarWith(target.getClanId()))
			return true;
		return false;
	}
	
	public String getIP()
	{
		return (this instanceof Ghost) ? String.valueOf(getObjectId()) : getClient().getIP();
	}
	
	public String getHWID()
	{
		if (this instanceof Ghost)
		{
			return String.valueOf(Long.MAX_VALUE - getObjectId());
		}
		if (!getClient().getFullHwid().isEmpty())
		{
			return getClient().getFullHwid();
		}
		else
		{
			System.out.println(getName() + " Temp HWID:" + Rnd.get(0, 99999999));
			return "Temp HWID:" + Rnd.get(0, 99999999);
		}
	}
	
	public void giveHeroSkills()
	{
		for (int i : HERO_SKILLS)
			addSkill(i);
		_hero = true;
	}
	
	private void removeHeroSkillsOnSubclasses()
	{
		if (_baseClass != _activeClass)
			for (int i : HERO_SKILLS)
				removeSkill(i, true);
	}
	
	public void removeHeroSkills()
	{
		for (int i : HERO_SKILLS)
			removeSkill(i, true);
	}
	
	public boolean isFakeHero()
	{
		return _fakeHero;
	}
	
	public void setFakeHero(boolean hero)
	{
		_fakeHero = hero;
	}
	
	public boolean isFOSHero()
	{
		return _fosHero;
	}
	
	public void setFOSHero(boolean foshero)
	{
		_fosHero = foshero;
	}
	
	/**
	 * gives 2.2% exp on a pvp kill
	 */
	private void givePvpExp(L2PcInstance target, int streakill) // custom edit
	{
		// Get the level of the L2PcInstance
		final int lvl = getLevel();
		// The pvp kill gives you some exp
		double percentGained = 23;
		final boolean war = isInOneSideClanwarWith(target);
		if (streakill > 1)
		{
			int max = 15;
			if (war)
				max = 24;
			if (streakill > max)
				streakill = max;
			percentGained *= (streakill / 1.4);
		}
		if (war)
			percentGained *= 2;
		float expPenalty = 1;
		if (L2RebirthMasterInstance.getRebirthLevel(this) > 0)
			expPenalty /= (1 + (0.4 * L2RebirthMasterInstance.getRebirthLevel(this)));
		if (expPenalty < 1)
			percentGained *= expPenalty;
		if (lvl >= 80)
		{
			int levelDifference = lvl - 80;
			percentGained /= (levelDifference * (levelDifference + 5) * 1.5 + Math.pow(1.76, levelDifference));
			if (lvl >= 92)
				percentGained /= 15;
			else if (lvl >= 91)
				percentGained /= 9;
			else if (lvl >= 90)
				percentGained /= 4;
		}
		// Calculate the Experience loss
		long gainedExp = 0;
		if (lvl == 95)
			gainedExp = Math.round((Experience.MAX_LEVEL - Experience.LEVEL[95]) * percentGained / 100);
		else
			gainedExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentGained / 100);
		// Set the new Experience value of the L2PcInstance
		if (isSupportClassForSoloInstance())
		{
			getStat().addExpAndSp((long) (gainedExp * Config.SUP_PVP_EXP_MUL), 40000);
		}
		else
		{
			getStat().addExpAndSp((long) (gainedExp * Config.PVP_EXP_MUL), 40000);
		}
		if (getClan() != null)
		{
			final L2Clan clan = getClan();
			clan.setPvpKills(clan.getPvpKills() + 1, true);
			if (clan.getLevel() <= 5 && clan.getReputationScore() <= 11000 && clan.getAllSkills().length < 5)
			{
				if (Rnd.nextInt(100) < 40)
				{
					clan.setReputationScore(clan.getReputationScore() + 1, true);
					sendMessage("Clan reputation increased by 1");
				}
			}
		}
	}
	
	final public void setBufferPage(String token)
	{
		_token = token;
	}
	
	final public String getBufferPage()
	{
		return _token;
	}
	
	/** Return the weapon attack reuse time. */
	public final double getAtkReuse(double reuse)
	{
		return reuse * calcStat(Stats.ATK_REUSE, 1, null, null);
	}
	
	public void broadcastClassIcon()
	{
		// Update class icon in party and clan
		if (isInParty())
			getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));
		if (getClan() != null)
			getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
	}
	
	private final boolean isSkillDisabledDueToTransformation(int skillId)
	{
		if (isGM())
			return false;
		if (isMounted())
		{
			final L2Skill skill = SkillTable.getInstance().getInfo(skillId, 1);
			if (skill != null)
			{
				if (skill.isPotion() || skill.getSkillType() == L2SkillType.TRANSFORMDISPEL || skill.getSkillType() == L2SkillType.STRSIEGEASSAULT)
				{}
				else
					return true;
			}
			else
				return true;
		}
		if (isTransformed() || isInStance())
		{
			if (getTransformation().getAllowedSkills().contains(skillId))
				return false;
			if (isInStance())
			{
				if (skillId >= 2000)
					return false;
			}
			if (getTransformationId() == 106 || getTransformationId() == 109 || getTransformationId() == 110 || (getTransformationId() >= 60000 && getTransformationId() <= 60005)) // horse, beatle, lion
			{
				if (skillId != 839) // dismount
					return true;
			}
			if (skillId == 60 && !isTransformed()) // Fake death
				return false;
			final L2Skill skill = SkillTable.getInstance().getInfo(skillId, 1);
			if (skill != null)
			{
				if (skill.isDance())
					return false;
			}
			if (getClassId().getId() == 96 || getClassId().getId() == 104 || getClassId().getId() == 111)
			{
				for (int id : L2PcInstance.SUMMONER_TRANSFORMATION_ALLOWED_SKILLS)
				{
					if (skillId == id)
						return false;
				}
			}
			for (int id : L2PcInstance.TRANSFORMATION_ALLOWED_SKILLS)
			{
				if (skillId == id)
					return false;
			}
			for (int id : L2PcInstance.PVP_SKILLS)
			{
				if (skillId == id)
					return false;
			}
			return true;
		}
		return false;
	}
	
	/*
	 * private int bluffProtectionTimerTick = 0; //custom edit
	 */
	private int	stunProtectionTimerTick			= 0;	// custom edit
	private int	stunObjectId;
	private int	stunselfProtectionTimerTick		= 0;	// custom edit
	private int	stunselfObjectId;
	private int	trickProtectionTimerTick		= 0;	// custom edit
	private int	trickObjectId;
	private int	fearProtectionTimerTick			= 0;	// custom edit
	private int	fearObjectId;
	private int	sleepProtectionTimerTick		= 0;	// custom edit
	private int	sleepObjectId;
	private int	disarmProtectionTimerTick		= 0;	// custom edit
	private int	disarmObjectId;
	/* private int muteProtectionTimerTick = 0; */
	private int	rootProtectionTimerTick			= 0;
	private int	rootObjectId;
	private int	knockbackProtectionTimerTick	= 0;
	private int	knockbackObjectId;
	/* private final int paratrifyProtectionTimerTick = 0; */
	/* private int knockbackProtectionTimerTick = 0; */
	/* private int aggressionProtectionTimerTick = 0; */
	/*
	 * final public void setBluffProtectionTime()
	 * {
	 * bluffProtectionTimerTick = GameTimeController.getGameTicks() + 140;
	 * }
	 * final public boolean canBeBluffed()
	 * {
	 * return GameTimeController.getGameTicks() > bluffProtectionTimerTick;
	 * }
	 */
	
	final public void setTrickProtectionTime(int objectid)
	{
		trickProtectionTimerTick = GameTimeController.getGameTicks() + 130;
		trickObjectId = objectid;
	}
	
	final public boolean canBeTricked(int objectid)
	{
		if (objectid != 0)
			return objectid == trickObjectId || GameTimeController.getGameTicks() > trickProtectionTimerTick;
		return GameTimeController.getGameTicks() > trickProtectionTimerTick;
	}
	
	final public void setStunProtectionTime(int objectid)
	{
		stunProtectionTimerTick = GameTimeController.getGameTicks() + 190;
		stunObjectId = objectid;
	}
	
	final public boolean canBeStunned(int objectid)
	{
		if (objectid != 0)
			return objectid == stunObjectId || GameTimeController.getGameTicks() > stunProtectionTimerTick;
		return GameTimeController.getGameTicks() > stunProtectionTimerTick;
	}
	
	final public void setKnockbackProtectionTime(int objectid)
	{
		knockbackProtectionTimerTick = GameTimeController.getGameTicks() + 150;
		knockbackObjectId = objectid;
	}
	
	final public boolean canBeKnockedBack(int objectid)
	{
		if (objectid != 0)
			return objectid == knockbackObjectId || GameTimeController.getGameTicks() > knockbackProtectionTimerTick;
		return GameTimeController.getGameTicks() > knockbackProtectionTimerTick;
	}
	
	final public void setStunselfProtectionTime(int objectid)
	{
		stunselfProtectionTimerTick = GameTimeController.getGameTicks() + 80;
		stunselfObjectId = objectid;
	}
	
	final public boolean canBeStunselfed(int objectid)
	{
		if (objectid != 0)
			return objectid == stunselfObjectId || GameTimeController.getGameTicks() > stunselfProtectionTimerTick;
		return GameTimeController.getGameTicks() > stunselfProtectionTimerTick;
	}
	
	final public void setFearProtectionTime(int objectid)
	{
		fearProtectionTimerTick = GameTimeController.getGameTicks() + 200;
		fearObjectId = objectid;
	}
	
	final public boolean canBeFeared(int objectid)
	{
		if (objectid != 0)
			return objectid == fearObjectId || GameTimeController.getGameTicks() > fearProtectionTimerTick;
		return GameTimeController.getGameTicks() > fearProtectionTimerTick;
	}
	
	final public void setSleepProtectionTime(int objectid)
	{
		sleepProtectionTimerTick = GameTimeController.getGameTicks() + 500;
		sleepObjectId = objectid;
	}
	
	final public boolean canBeSlept(int objectid)
	{
		if (objectid != 0)
			return objectid == sleepObjectId || GameTimeController.getGameTicks() > sleepProtectionTimerTick;
		return GameTimeController.getGameTicks() > sleepProtectionTimerTick;
	}
	
	final public void setDisarmProtectionTime(int objectid)
	{
		disarmProtectionTimerTick = GameTimeController.getGameTicks() + 160;
		disarmObjectId = objectid;
	}
	
	final public boolean canBeDisarmed(int objectid)
	{
		if (objectid != 0)
			return objectid == disarmObjectId || GameTimeController.getGameTicks() > disarmProtectionTimerTick;
		return GameTimeController.getGameTicks() > disarmProtectionTimerTick;
	}
	/*
	 * final public void setMuteProtectionTime()
	 * {
	 * muteProtectionTimerTick = GameTimeController.getGameTicks() + 200;
	 * }
	 * final public boolean canBeMuted()
	 * {
	 * return GameTimeController.getGameTicks() > muteProtectionTimerTick;
	 * }
	 */
	
	final public void setRootProtectionTime(int objectid)
	{
		rootProtectionTimerTick = GameTimeController.getGameTicks() + 180;
		rootObjectId = objectid;
	}
	
	final public boolean canBeRooted(int objectid)
	{
		if (objectid != 0)
			return objectid == rootObjectId || GameTimeController.getGameTicks() > rootProtectionTimerTick;
		return GameTimeController.getGameTicks() > rootProtectionTimerTick;
	}
	
	/*
	 * final public void setParatrifyProtectionTime()
	 * {
	 * paratrifyProtectionTimerTick = GameTimeController.getGameTicks() + 150;
	 * }
	 * final public boolean canBeParatrified()
	 * {
	 * return GameTimeController.getGameTicks() > paratrifyProtectionTimerTick;
	 * }
	 */
	/*
	 * final public void setknockbackProtectionTime()
	 * {
	 * knockbackProtectionTimerTick = GameTimeController.getGameTicks() + 180;
	 * }
	 * final public boolean canBeknockedback()
	 * {
	 * return GameTimeController.getGameTicks() > knockbackProtectionTimerTick;
	 * }
	 */
	/*
	 * final public void setAggressionProtectionTime()
	 * {
	 * aggressionProtectionTimerTick = GameTimeController.getGameTicks() + 90;
	 * }
	 * final public boolean canBeAggroed()
	 * {
	 * return GameTimeController.getGameTicks() > aggressionProtectionTimerTick;
	 * }
	 */
	public void playSound(String sound)
	{
		if (sound == null || sound.equalsIgnoreCase(""))
			return;
		sendPacket(new PlaySound(sound));
	}
	/*
	 * public void rewardRacialCirclets()
	 * {
	 * int circletId = 0;
	 * switch (getRace())
	 * {
	 * case Human:
	 * circletId = 9391;
	 * break;
	 * case Elf:
	 * circletId = 9392;
	 * break;
	 * case DarkElf:
	 * circletId = 9393;
	 * break;
	 * case Dwarf:
	 * circletId = 9395;
	 * break;
	 * case Orc:
	 * circletId = 9394;
	 * break;
	 * case Kamael:
	 * circletId = 9396;
	 * break;
	 * }
	 * if (circletId != 0)
	 * {
	 * if (getInventory().getItemByItemId(circletId) == null)
	 * {
	 * addItem("Racial Circlet", circletId, 1, this, true);
	 * sendMessage("You have been granted your racial circlet for achieving level 90.");
	 * }
	 * }
	 * }
	 */
	
	/**
	 * Sends a SystemMessage without any parameter added. No instancing at all!
	 */
	public void sendPacket(SystemMessageId sm)
	{
		sendPacket(sm.getSystemMessage());
	}
	
	final public boolean getInEventPeaceZone()
	{
		if (!isInActiveFunEvent())
		{
			return false;
		}
		if (TvT._started)
		{
			for (int i = 0; i < TvT._teamsX.size(); i++)
			{
				if (isInsideRadius(TvT._teamsX.get(i), TvT._teamsY.get(i), 100, false))
				{
					if (_inEventTvT && _teamNameTvT.equalsIgnoreCase(TvT._teams.get(i)))
						return true;
				}
			}
		}
		if (NewTvT._started)
		{
			if (!_inEventTvT)
				return false;
			if (_teamNameTvT.equalsIgnoreCase(NewTvT._teams.get(0)))
			{
				for (int i = 0; i < NewTvT._team1Locs.size(); i++)
				{
					if (isInsideRadius(NewTvT._team1Locs.get(i).getX(), NewTvT._team1Locs.get(i).getY(), 100, false))
					{
						if (_inEventTvT)
						{
							return true;
						}
					}
				}
			}
			if (_teamNameTvT.equalsIgnoreCase(NewTvT._teams.get(1)))
			{
				for (int i = 0; i < NewTvT._team2Locs.size(); i++)
				{
					if (isInsideRadius(NewTvT._team2Locs.get(i).getX(), NewTvT._team2Locs.get(i).getY(), 100, false))
					{
						if (_inEventTvT)
						{
							return true;
						}
					}
				}
			}
		}
		if (NewHuntingGrounds._started)
		{
			if (!_inEventHG)
				return false;
			if (_teamNameHG.equalsIgnoreCase(NewHuntingGrounds._teams.get(0)))
			{
				for (int i = 0; i < NewHuntingGrounds._team1Locs.size(); i++)
				{
					if (isInsideRadius(NewHuntingGrounds._team1Locs.get(i).getX(), NewHuntingGrounds._team1Locs.get(i).getY(), 100, false))
					{
						return true;
					}
				}
			}
			if (_teamNameHG.equalsIgnoreCase(NewHuntingGrounds._teams.get(1)))
			{
				for (int i = 0; i < NewHuntingGrounds._team2Locs.size(); i++)
				{
					if (isInsideRadius(NewHuntingGrounds._team2Locs.get(i).getX(), NewHuntingGrounds._team2Locs.get(i).getY(), 100, false))
					{
						return true;
					}
				}
			}
		}
		if (NewDomination._started)
		{
			if (!_inEventLunaDomi)
				return false;
			if (_teamNameLunaDomi.equalsIgnoreCase(NewDomination._teams.get(0)))
			{
				for (int i = 0; i < NewDomination._team1Locs.size(); i++)
				{
					if (isInsideRadius(NewDomination._team1Locs.get(i).getX(), NewDomination._team1Locs.get(i).getY(), 100, false))
					{
						return true;
					}
				}
			}
			if (_teamNameLunaDomi.equalsIgnoreCase(NewDomination._teams.get(1)))
			{
				for (int i = 0; i < NewDomination._team2Locs.size(); i++)
				{
					if (isInsideRadius(NewDomination._team2Locs.get(i).getX(), NewDomination._team2Locs.get(i).getY(), 100, false))
					{
						return true;
					}
				}
			}
		}
		if (CTF._started)
		{
			for (int i = 0; i < CTF._teamsX.size(); i++)
			{
				if (isInsideRadius(CTF._teamsX.get(i), CTF._teamsY.get(i), 100, false))
				{
					if (_inEventCTF && _teamNameCTF.equalsIgnoreCase(CTF._teams.get(i)))
						return true;
				}
			}
		}
		if (NewCTF._started)
		{
			if (!_inEventCTF)
				return false;
			if (_teamNameCTF.equalsIgnoreCase(NewCTF._teams.get(0)))
			{
				for (int i = 0; i < NewCTF._team1Locs.size(); i++)
				{
					if (isInsideRadius(NewCTF._team1Locs.get(i).getX(), NewCTF._team1Locs.get(i).getY(), 100, false))
					{
						return true;
					}
				}
			}
			if (_teamNameCTF.equalsIgnoreCase(NewCTF._teams.get(1)))
			{
				for (int i = 0; i < NewCTF._team2Locs.size(); i++)
				{
					if (isInsideRadius(NewCTF._team2Locs.get(i).getX(), NewCTF._team2Locs.get(i).getY(), 100, false))
					{
						return true;
					}
				}
			}
		}
		if (FOS._started)
		{
			for (int i = 0; i < FOS._teamsX.size(); i++)
			{
				if (isInsideRadius(FOS._teamsX.get(i), FOS._teamsY.get(i), 100, false))
				{
					if (_inEventFOS && _teamNameFOS.equalsIgnoreCase(FOS._teams.get(i)))
						return true;
				}
			}
		}
		if (NewFOS._started)
		{
			if (!_inEventFOS)
				return false;
			if (_teamNameFOS.equalsIgnoreCase(NewFOS._teams.get(0)))
			{
				for (int i = 0; i < NewFOS._team1Locs.size(); i++)
				{
					if (isInsideRadius(NewFOS._team1Locs.get(i).getX(), NewFOS._team1Locs.get(i).getY(), 100, false))
					{
						return true;
					}
				}
			}
			if (_teamNameFOS.equalsIgnoreCase(NewFOS._teams.get(1)))
			{
				for (int i = 0; i < NewFOS._team2Locs.size(); i++)
				{
					if (isInsideRadius(NewFOS._team2Locs.get(i).getX(), NewFOS._team2Locs.get(i).getY(), 100, false))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean isOnSameTeamInVIP(L2PcInstance player)
	{
		if (!_inEventVIP || !player._inEventVIP)
			return false;
		if (_isVIP && player._isVIP)
			return true;
		if (_isNotVIP && player._isNotVIP)
			return true;
		return false;
	}
	
	public void removeCTFFlagOnDie()
	{
		if (CTF._started)
		{
			CTF._flagsTaken.set(CTF._teams.indexOf(_teamNameHaveFlagCTF), false);
			CTF.spawnFlag(_teamNameHaveFlagCTF);
			CTF.removeFlagFromPlayer(this);
			broadcastUserInfo();
			_haveFlagCTF = false;
			CTF.AnnounceToPlayers(false, CTF._eventName + "(CTF): " + _teamNameHaveFlagCTF + "'s flag returned.");
		}
		if (NewCTF._started)
		{
			NewCTF._flagsTaken.set(NewCTF._teams.indexOf(_teamNameHaveFlagCTF), false);
			NewCTF.spawnFlag(_teamNameHaveFlagCTF);
			NewCTF.removeFlagFromPlayer(this);
			broadcastUserInfo();
			_haveFlagCTF = false;
			NewCTF.AnnounceToPlayers(false, NewCTF._eventName + "(CTF): " + _teamNameHaveFlagCTF + "'s flag returned.");
		}
	}
	
	private boolean _isInGludin = false;
	
	public void setIsInGludin(final boolean val)
	{
		_isInGludin = val;
	}
	
	public boolean isInGludin()
	{
		return _isInGludin;
	}
	
	private boolean _isInHuntersVillage = false;
	
	public boolean isInHuntersVillage()
	{
		return _isInHuntersVillage;
	}
	
	public void setInHuntersVillage(boolean isInHuntersVillage)
	{
		_isInHuntersVillage = isInHuntersVillage;
	}
	
	private boolean _isInOrcVillage = false;
	
	public boolean isInOrcVillage()
	{
		return _isInOrcVillage;
	}
	
	public void setInOrcVillage(boolean isInOrcVillage)
	{
		_isInOrcVillage = isInOrcVillage;
	}
	
	private boolean _isInKoreanZone = false;
	
	public void setIsInKoreanZone(boolean isInKoreanZone)
	{
		_isInKoreanZone = isInKoreanZone;
	}
	
	public boolean isInKoreanZone()
	{
		return _isInKoreanZone;
	}
	
	private boolean _isInFarmEvent = false;
	
	public boolean isInFarmEvent()
	{
		return _isInFarmEvent;
	}
	
	public void setInFarmEvent(boolean isInFarmEvent)
	{
		_isInFarmEvent = isInFarmEvent;
	}
	
	public boolean _isInHellbound = false;
	
	public final boolean isInHellbound()
	{
		return _isInHellbound;
	}
	
	public boolean _isInHellboundLowland = false;
	
	public final boolean isInHellboundLowland()
	{
		return _isInHellboundLowland;
	}
	
	public final void setIsInHellbound(boolean isInHellbound)
	{
		_isInHellbound = isInHellbound;
	}
	
	public final void setIsInHellboundLowland(boolean isInHellboundLowland)
	{
		_isInHellboundLowland = isInHellboundLowland;
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (isInBoat())
		{
			getPosition().setWorldPosition(getBoat().getPosition().getWorldPosition());
			activeChar.sendPacket(new CharInfo(this));
			activeChar.sendPacket(new ExBrExtraUserInfo(this));
			int relation1 = getRelation(activeChar);
			int relation2 = activeChar.getRelation(this);
			if (getKnownList().getKnownRelations().get(activeChar.getObjectId()) != null && getKnownList().getKnownRelations().get(activeChar.getObjectId()) != relation1)
			{
				activeChar.sendPacket(new RelationChanged(this, relation1, activeChar.isAutoAttackable(this)));
				if (getPet() != null)
					activeChar.sendPacket(new RelationChanged(getPet(), relation1, activeChar.isAutoAttackable(this)));
			}
			if (activeChar.getKnownList().getKnownRelations().get(getObjectId()) != null && activeChar.getKnownList().getKnownRelations().get(getObjectId()) != relation2)
			{
				sendPacket(new RelationChanged(activeChar, relation2, isAutoAttackable(activeChar)));
				if (activeChar.getPet() != null)
					sendPacket(new RelationChanged(activeChar.getPet(), relation2, isAutoAttackable(activeChar)));
			}
			activeChar.sendPacket(new GetOnVehicle(this, getBoat(), getInBoatPosition().getX(), getInBoatPosition().getY(), getInBoatPosition().getZ()));
			/*
			 * if(getBoat().GetVehicleDeparture() == null)
			 * {
			 * int xboat = getBoat().getX();
			 * int yboat= getBoat().getY();
			 * double modifier = Math.PI/2;
			 * if (yboat == 0)
			 * {
			 * yboat = 1;
			 * }
			 * if(yboat < 0)
			 * {
			 * modifier = -modifier;
			 * }
			 * double angleboat = modifier - Math.atan(xboat/yboat);
			 * int xp = getX();
			 * int yp = getY();
			 * modifier = Math.PI/2;
			 * if (yp == 0)
			 * {
			 * yboat = 1;
			 * }
			 * if(yboat < 0)
			 * {
			 * modifier = -modifier;
			 * }
			 * double anglep = modifier - Math.atan(yp/xp);
			 * double finx = Math.cos(anglep - angleboat)*Math.sqrt(xp *xp +yp*yp ) + Math.cos(angleboat)*Math.sqrt(xboat *xboat +yboat*yboat );
			 * double finy = Math.sin(anglep - angleboat)*Math.sqrt(xp *xp +yp*yp ) + Math.sin(angleboat)*Math.sqrt(xboat *xboat +yboat*yboat );
			 * //getPosition().setWorldPosition(getBoat().getX() - getInBoatPosition().x,getBoat().getY() - getInBoatPosition().y,getBoat().getZ()- getInBoatPosition().z);
			 * getPosition().setWorldPosition((int)finx,(int)finy,getBoat().getZ()- getInBoatPosition().z);
			 * }
			 */
		}
		else if (isInAirShip())
		{
			getPosition().setWorldPosition(getAirShip().getPosition().getWorldPosition());
			activeChar.sendPacket(new CharInfo(this));
			activeChar.sendPacket(new ExBrExtraUserInfo(this));
			int relation1 = getRelation(activeChar);
			int relation2 = activeChar.getRelation(this);
			if (getKnownList().getKnownRelations().get(activeChar.getObjectId()) != null && getKnownList().getKnownRelations().get(activeChar.getObjectId()) != relation1)
			{
				activeChar.sendPacket(new RelationChanged(this, relation1, activeChar.isAutoAttackable(this)));
				if (getPet() != null)
					activeChar.sendPacket(new RelationChanged(getPet(), relation1, activeChar.isAutoAttackable(this)));
			}
			if (activeChar.getKnownList().getKnownRelations().get(getObjectId()) != null && activeChar.getKnownList().getKnownRelations().get(getObjectId()) != relation2)
			{
				sendPacket(new RelationChanged(activeChar, relation2, isAutoAttackable(activeChar)));
				if (activeChar.getPet() != null)
					sendPacket(new RelationChanged(activeChar.getPet(), relation2, isAutoAttackable(activeChar)));
			}
			activeChar.sendPacket(new ExGetOnAirShip(this, getAirShip()));
		}
		else
		{
			activeChar.sendPacket(new CharInfo(this));
			activeChar.sendPacket(new ExBrExtraUserInfo(this));
			int relation1 = getRelation(activeChar);
			int relation2 = activeChar.getRelation(this);
			if (getKnownList().getKnownRelations().get(activeChar.getObjectId()) != null && getKnownList().getKnownRelations().get(activeChar.getObjectId()) != relation1)
			{
				activeChar.sendPacket(new RelationChanged(this, relation1, activeChar.isAutoAttackable(this)));
				if (getPet() != null)
					activeChar.sendPacket(new RelationChanged(getPet(), relation1, activeChar.isAutoAttackable(this)));
			}
			if (activeChar.getKnownList().getKnownRelations().get(getObjectId()) != null && activeChar.getKnownList().getKnownRelations().get(getObjectId()) != relation2)
			{
				sendPacket(new RelationChanged(activeChar, relation2, isAutoAttackable(activeChar)));
				if (activeChar.getPet() != null)
					sendPacket(new RelationChanged(activeChar.getPet(), relation2, isAutoAttackable(activeChar)));
			}
		}
		if (getMountType() == 4)
		{
			// TODO: Remove when horse mounts fixed
			activeChar.sendPacket(new Ride(this, false, 0));
			activeChar.sendPacket(new Ride(this, true, getMountNpcId()));
		}
		switch (getPrivateStoreType())
		{
			case L2PcInstance.STORE_PRIVATE_SELL:
				activeChar.sendPacket(new PrivateStoreMsgSell(this));
				break;
			case L2PcInstance.STORE_PRIVATE_PACKAGE_SELL:
				activeChar.sendPacket(new ExPrivateStoreSetWholeMsg(this));
				break;
			case L2PcInstance.STORE_PRIVATE_BUY:
				activeChar.sendPacket(new PrivateStoreMsgBuy(this));
				break;
			case L2PcInstance.STORE_PRIVATE_MANUFACTURE:
				activeChar.sendPacket(new RecipeShopMsg(this));
				break;
		}
	}
	
	private boolean _isInSgradeZone = false;
	
	final public boolean isInSgradeZone()
	{
		return _isInSgradeZone;
	}
	
	final public void setIsInSgradeZone(final boolean val)
	{
		_isInSgradeZone = val;
	}
	
	private boolean _isInS80zone = false;
	
	final public boolean isInS80zone()
	{
		return _isInS80zone;
	}
	
	final public void setIsInS80zone(final boolean val)
	{
		_isInS80zone = val;
	}
	
	private boolean _isInHqZone = false;
	
	final public boolean isInHqZone()
	{
		return _isInHqZone;
	}
	
	final public void setisInHqZone(final boolean val)
	{
		_isInHqZone = val;
	}
	
	public boolean isProphet()
	{ // actually hierophant
		return getClassId().getId() == 0x62 || getClassId().getId() == 0x11;
	}
	
	public boolean isDwarfClass()
	{
		switch (getClassId().getId())
		{
			case 0x75:
			case 0x76:
			case 0x38:
			case 0x39:
				return true;
		}
		return false;
	}
	
	final public boolean isAlliedWith(final L2Character character)
	{
		if (getAllyId() == 0)
			return false;
		if (!(character instanceof L2Playable))
			return false;
		return getAllyId() == character.getActingPlayer().getAllyId();
	}
	
	public boolean hatesRace(L2Character target)
	{
		if (!(target instanceof L2Playable))
			return false;
		final int targetRace = target.getActingPlayer().getRace().ordinal();
		switch (getRace().ordinal())
		{
			case 0:
				return targetRace == 5;
			case 1:
				return targetRace == 2;
			case 2:
				return targetRace == 1;
			case 3:
				return targetRace == 4;
			case 4:
				return targetRace == 3;
			case 5:
				return targetRace == 0;
		}
		return false;
	}
	
	private boolean _isInPI = false;
	
	final public boolean isInPI()
	{
		return _isInPI;
	}
	
	final public void setIsInPI(final boolean val)
	{
		_isInPI = val;
	}
	
	private boolean _isInPvPCustomEventZone = false;
	
	final public boolean isInPvPCustomEventZone()
	{
		return _isInPvPCustomEventZone;
	}
	
	final public void setInPvPCustomEventZone(final boolean val)
	{
		_isInPvPCustomEventZone = val;
	}
	
	private boolean _isInFT = false;
	
	final public boolean isInFT()
	{
		return _isInFT;
	}
	
	final public void setIsInFT(final boolean val)
	{
		_isInFT = val;
	}
	
	private int	_killL2AttackableTick	= 0;
	private int	_l2netTargetSelectTicks	= 0;
	
	final public void setKillL2AttackableTick(final int tick)
	{
		_killL2AttackableTick = tick;
	}
	
	final public int getKillL2AttackableTick()
	{
		return _killL2AttackableTick;
	}
	
	final public void setl2nettargetselectTick(final int tick)
	{
		_l2netTargetSelectTicks = tick;
	}
	
	final public int getl2nettargetselectTick()
	{
		return _l2netTargetSelectTicks;
	}
	
	final public void incl2nettargetselectTick()
	{
		_l2netTargetSelectTicks++;
	}
	
	public boolean isNearARB()
	{
		for (L2Character boss : getKnownList().getKnownCharacters())
		{
			if (boss != null)
			{
				if (boss instanceof L2RaidBossInstance)
					return true;
			}
		}
		return false;
	}
	
	public int getActionObjIdNoTarget()
	{
		return _actionObjIdNoTarget;
	}
	
	public void setActionObjIdNoTarget(final int id)
	{
		_actionObjIdNoTarget = id;
	}
	
	public int getActionObjIdNoTargetTicks()
	{
		return _actionObjIdNoTargetTicks;
	}
	
	public void setActionObjIdNoTargetTicks(final int ticks)
	{
		_actionObjIdNoTargetTicks = ticks;
	}
	
	public boolean canResInCurrentInstance()
	{
		if (isInUniqueInstance())
		{
			final Instance inst = getCurrentInstance();
			if (inst != null && inst.getResLimit() >= 0)
			{
				if (inst.getCurrentResAmount() >= inst.getResLimit())
					return false;
			}
		}
		return true;
	}
	
	public void initiateNameChange(String name)
	{
		final String beforeName = getName();
		L2World.getInstance().removeFromAllPlayers(this);
		setName(name);
		storeName();
		L2World.getInstance().addToAllPlayers(this);
		updateCharFriendsDueToNameChange();
		sendMessage("Your name has changed to " + name);
		broadcastUserInfo();
		// auditNameChange(this, beforeName, name);
		if (isInParty())
		{
			getParty().refreshPartyView();
		}
		if (getClan() != null)
		{
			getClan().broadcastClanStatus();
		}
		RegionBBSManager.getInstance().changeCommunityBoard();
	}
	
	final private static void auditNameChange(final L2PcInstance player, final String beforeName, final String afterName)
	{
		String killer_clan = "";
		if (player.getClan() != null)
			killer_clan = player.getClan().getName();
		final String killer_IP = player.getIP();
		String today = GMAudit._formatter.format(new Date());
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO audit_pvp(killer, killer_IP, killer_clan, target, target_IP, target_clan, famerewarded, date) VALUES(?,?,?,?,?,?,?,?)");
			statement.setString(1, beforeName);
			statement.setString(2, killer_IP);
			statement.setString(3, killer_clan);
			statement.setString(4, afterName);
			statement.setString(5, killer_IP);
			statement.setString(6, killer_clan);
			statement.setInt(7, 999999);
			statement.setString(8, today);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.fine("could not audit Name Change: " + player.getName() + " " + e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static enum NameColors
	{
		Default(0, "ffffff", 0, "Default white"),
		VLBlue(1, "ffe7b9", 100, "Very Light Blue"),
		VLGreen(2, "A6FFAE", 250, "Very Light Green"),
		VLYellow(3, "a8fff9", 600, "Very Light Yellow"),
		LBlue(4, "ffd362", 1000, "Light Blue"),
		LPink(5, "ffb9ff", 1500, "Light Pink"),
		LGreen(6, "62FF76", 2000, "Light Green"),
		Pink(7, "df8cff", 2500, "Pink"),
		LYellow(8, "59fff2", 3000, "Light Yellow"),
		BluePurple(9, "f49797", 3500, "Violet"),
		Blue(10, "ef9a45", 4000, "Blue"),
		Purple(11, "fe6baa", 5000, "Purple"),
		Green(12, "279e33", 6000, "Green"),
		LightOrange(13, "43a3fa", 7000, "Orange"),
		Orange(14, "1d7ce7", 8000, "Dark Orange"),
		Brown(15, "5E769d", 9000, "Brown"),
		Grey(16, "9e9e9e", 10000, "Grey"),
		DarkPurple(17, "fd4493", 11000, "Dark Purple"),
		DarkBlue(18, "be660e", 12000, "Dark Blue"),
		DarkGreen(19, "1e7927", 13000, "Dark Green"),
		DarkBrown(20, "4e5a85", 14000, "Dark Brown"),
		DarkGrey(21, "616161", 15000, "Dark Grey"),
		DarkRed(22, "000097", 17500, "Dark Red"),
		Red(23, "3838d8", 20000, "Red"),
		KarmaRed(24, "0000dd", 23000, "Karma Red"),
		Black(25, "2c2c2c", 27000, "Black");
		
		public int			id;
		public String		hash;
		public String		htmlHash;
		public int			minPvP;
		public final String	name;
		
		NameColors(int id, String hash, int minPvP, String name)
		{
			this.id = id;
			this.hash = hash;
			this.minPvP = minPvP;
			this.name = name;
			this.htmlHash = hash.substring(4, 6) + hash.substring(2, 4) + hash.substring(0, 2);
		}
		
		public String getColor()
		{
			return hash;
		}
	}
	
	public static enum TitleColors
	{
		Default(0, "ffff77", 0, "Default Light Blue"),
		White(1, "ffffff", 50, "White"),
		LBlue(2, "d5d500", 100, "Light Blue"),
		LGreen(3, "79ff62", 250, "Light Green"),
		LYellow(4, "62fff8", 600, "Light Yellow"),
		Blue(5, "f9b500", 1000, "Blue"),
		LightGray(6, "a6a6a6", 1000, "Light Grey"),
		GrayBlue(7, "bead8d", 1000, "Grey-Blue"),
		GrayPink(8, "b597b3", 1500, "Grey-Pink"),
		GrayGreen(9, "95b796", 2000, "Grey-Green"),
		GreyYellow(10, "8ac1be", 3000, "Grey-Yellow"),
		Grey(11, "888888", 3500, "Grey"),
		DarkBlue(12, "a56d3a", 4000, "Dark Blue"),
		DarkPurple(13, "bc4767", 5000, "Dark Purple"),
		DarkGreen(14, "39753d", 6000, "Dark Green"),
		DarkOrange(15, "034f94", 7000, "Dark Orange"),
		DarkGrey(16, "616161", 8000, "Dark Grey"),
		DarkRed(17, "21218d", 10000, "Dark Red"),
		Red(18, "0e0ebc", 12500, "Red"),
		Black(20, "303030", 15000, "Black");
		
		public int			id;
		public String		hash;
		public String		htmlHash;
		public int			minPvP;
		public final String	name;
		
		TitleColors(int id, String hash, int minPvP, String name)
		{
			this.id = id;
			this.hash = hash;
			this.minPvP = minPvP;
			this.name = name;
			this.htmlHash = hash.substring(4, 6) + hash.substring(2, 4) + hash.substring(0, 2);
		}
	}
	
	boolean _isHidedGm = true;
	
	public boolean isHidedGMView()
	{
		return _isHidedGm;
	}
	
	public void resetHidedGMView()
	{
		if (!isGM())
			return;
		_isHidedGm = !_isHidedGm;
		sendMessage((isHidedGMView() ? "You dissabled" : "You enabled") + " your GM view.");
	}
	
	public void updateNameTitleColors(int choiceName, boolean bool1, boolean bool2)
	{
		if (!bool1 && !bool2)
		{
			String val = null;
			for (NameColors nc : NameColors.values())
			{
				if (nc.id == choiceName)
				{
					val = nc.hash;
					break;
				}
			}
			getAppearance().setNameColor(Integer.decode("0x" + val));
			this.setNameC(val);
			broadcastUserInfo();
		}
		if (bool1 && bool2)
		{
			String val = null;
			for (TitleColors tc : TitleColors.values())
			{
				if (tc.id == choiceName)
				{
					val = tc.hash;
					break;
				}
			}
			getAppearance().setTitleColor(Integer.decode("0x" + val));
			this.setTitleC(val);
			broadcastUserInfo();
		}
	}
	
	public void setNameColorsMethod()
	{
		if (isInFunEvent())
			return;
		if (getClan() != null && getClan().getLevel() >= 5)
		{
			if (isClanLeader()) // set clan leaders title color to dark green
				getAppearance().setTitleColor(Integer.decode("0x009000"));
		}
		String nameC = getNameC();
		String titleC = getTitleC();
		if (!nameC.equals("none"))
		{
			getAppearance().setNameColor(Integer.decode("0x" + nameC));
			getAppearance().setTitleColor(Integer.decode("0x" + titleC));
		}
		else
			setNameColorsDueToPVP();
	}
	
	public void setNameColorsDueToPVP()
	{
		String nameC = getNameC();
		String titleC = getTitleC();
		if (isInFunEvent())
			return;
		if (getClan() != null && getClan().getLevel() >= 5)
		{
			if (isClanLeader()) // set clan leaders title color to dark green
				getAppearance().setTitleColor(Integer.decode("0x009000"));
		}
		if (getPvpKills() >= 1000 && getPvpKills() <= 1999)
		{
			getAppearance().setNameColor(Integer.decode("0xFFAA00"));
		}
		else if (getPvpKills() >= 2000 && getPvpKills() <= 2999)
		{
			getAppearance().setNameColor(Integer.decode("0xFF99FF"));
		}
		else if (getPvpKills() >= 3000 && getPvpKills() <= 3999)
		{
			getAppearance().setNameColor(Integer.decode("0x55FF55"));
		}
		if (getPvpKills() >= 4000 && getPvpKills() <= 4999)
		{
			getAppearance().setNameColor(Integer.decode("0x33FFFF"));
		}
		else if (getPvpKills() >= 5000 && getPvpKills() <= 6499)
		{
			getAppearance().setNameColor(Integer.decode("0xFFFF00"));
			getAppearance().setTitleColor(Integer.decode("0xFF77FF"));
		}
		else if (getPvpKills() >= 6500 && getPvpKills() <= 8499)
		{
			getAppearance().setNameColor(Integer.decode("0xFF5599")); // purple name
			getAppearance().setTitleColor(Integer.decode("0x777777"));
		}
		else if (getPvpKills() >= 8500 && getPvpKills() <= 11999)
		{
			getAppearance().setNameColor(Integer.decode("0x3366ff")); // orange name
			getAppearance().setTitleColor(Integer.decode("0x777777")); // grey title
		}
		else if (getPvpKills() >= 12000 && getPvpKills() <= 24999)
		{
			getAppearance().setNameColor(Integer.decode("0x666666")); // grey name
			getAppearance().setTitleColor(Integer.decode("0x0000FF")); // red title
		}
		else if (getPvpKills() >= 25000)
		{
			getAppearance().setNameColor(Integer.decode("0x0000FF")); // original red
			getAppearance().setTitleColor(Integer.decode("0x666666"));
		}
		if (!nameC.equals("none"))
		{
			getAppearance().setNameColor(Integer.decode("0x" + nameC));
			getAppearance().setTitleColor(Integer.decode("0x" + titleC));
		}
	}
	
	public String getNameColorsForAPCTable()
	{
		if (getPvpKills() >= 1000 && getPvpKills() <= 1999)
		{
			return "FFAA00";
		}
		else if (getPvpKills() >= 2000 && getPvpKills() <= 2999)
		{
			return "FF99FF";
		}
		else if (getPvpKills() >= 3000 && getPvpKills() <= 3999)
		{
			return "55FF55";
		}
		if (getPvpKills() >= 4000 && getPvpKills() <= 4999)
		{
			return "33FFFF";
		}
		else if (getPvpKills() >= 5000 && getPvpKills() <= 6499)
		{
			return "FFFF00";
		}
		else if (getPvpKills() >= 6500 && getPvpKills() <= 8499)
		{
			return "FF5599";
		}
		else if (getPvpKills() >= 8500 && getPvpKills() <= 11999)
		{
			return "3366ff";
		}
		else if (getPvpKills() >= 12000 && getPvpKills() <= 24999)
		{
			return "666666";
		}
		else if (getPvpKills() >= 25000)
		{
			return "0000FF";
		}
		return "FFFFFF";
	}
	
	public String getTitleColorsForAPCTable()
	{
		if (getPvpKills() >= 5000 && getPvpKills() <= 6499)
		{
			return "FF77FF";
		}
		else if (getPvpKills() >= 6500 && getPvpKills() <= 8499)
		{
			return "777777";
		}
		else if (getPvpKills() >= 8500 && getPvpKills() <= 11999)
		{
			return "777777";
		}
		else if (getPvpKills() >= 12000 && getPvpKills() <= 24999)
		{
			return "0000FF";
		}
		else if (getPvpKills() >= 25000)
		{
			return "666666";
		}
		if (getClan() != null && getClan().getLevel() >= 5)
		{
			if (isClanLeader()) // set clan leaders title color to dark green
				return "009000";
		}
		return "FFFF77";
	}
	
	// Nexus Events start
	private L2PcTemplate	_antifeedTemplate	= null;
	private boolean			_antifeedSex;
	
	private L2PcTemplate createRandomAntifeedTemplate()
	{
		Race race = null;
		while (race == null)
		{
			race = Race.values()[Rnd.get(Race.values().length)];
			if (race == getRace() || race == Race.Kamael)
				race = null;
		}
		PlayerClass p;
		for (ClassId c : ClassId.values())
		{
			p = PlayerClass.values()[c.getId()];
			if (p.isOfRace(race) && p.isOfLevel(ClassLevel.Fourth))
			{
				_antifeedTemplate = CharTemplateTable.getInstance().getTemplate(c);
				break;
			}
		}
		if (getRace() == Race.Kamael)
			_antifeedSex = getAppearance().getSex();
		_antifeedSex = Rnd.get(2) == 0 ? true : false;
		return _antifeedTemplate;
	}
	
	public void startAntifeedProtection(boolean start, boolean broadcast)
	{
		if (!start)
		{
			getAppearance().setVisibleName(getName());
			_antifeedTemplate = null;
		}
		else
		{
			getAppearance().setVisibleName("Unknown");
			createRandomAntifeedTemplate();
		}
	}
	
	public L2PcTemplate getAntifeedTemplate()
	{
		return _antifeedTemplate;
	}
	
	public boolean getAntifeedSex()
	{
		return _antifeedSex;
	}
	// Nexus events end
	
	public void setPartyReason(String reason)
	{
		_reason = reason;
	}
	
	public String getPartyReason()
	{
		return _reason;
	}
	
	public void setPinCode(String pinCode)
	{
		_pinCode = pinCode;
	}
	
	public String getPinCode()
	{
		return _pinCode;
	}
	
	public void setPinCodeAccount(String pincode)
	{
		setPinCode(pincode);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE accounts SET pincode = ? WHERE login = ?");
			statement.setString(1, pincode);
			statement.setString(2, getAccountName());
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			sendMessage("there has been a error with setting your pin code");
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
		sendMessage("You have set your account pin code successfully, remember it carefully");
	}
	
	public void setSecretCode(String secretCode)
	{
		_secretCode = secretCode;
	}
	
	public String getSecretCode()
	{
		return _secretCode;
	}
	
	public void setSecretCodeAccount(String secret)
	{
		setSecretCode(secret);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE accounts SET secret = ? WHERE login = ?");
			statement.setString(1, secret);
			statement.setString(2, getAccountName());
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			sendMessage("there has been a error with setting your secret code");
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
		sendMessage("You have set your account secret code successfully, remember it carefully");
	}
	
	public void updateCharFriendsDueToNameChange()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE character_friends SET friend_name = ? WHERE friendId = ?");
			statement.setString(1, getName());
			statement.setInt(2, getObjectId());
			statement.execute();
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
	
	@SuppressWarnings("incomplete-switch")
	public void useItem(final int itemObjectId, final boolean force)
	{
		if (getPrivateStoreType() != 0)
		{
			sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (getActiveTradeList() != null)
			cancelActiveTrade();
		L2ItemInstance item = getInventory().getItemByObjectId(itemObjectId);
		if (item == null)
			return;
		if (item.isWear())
		{
			return;
		}
		if (item.getItem().getType2() == L2Item.TYPE2_QUEST && !(item.getItemId() >= 60078 && item.getItemId() <= 60110) && !(item.getItemId() >= 870000 && item.getItemId() <= 870005) && !(item.getItemId() >= 8618 && item.getItemId() <= 8621) && !(item.getItemId() >= 10549 && item.getItemId() <= 10551))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_USE_QUEST_ITEMS);
			this.sendPacket(sm);
			sm = null;
			return;
		}
		final int itemId = item.getItemId();
		// if (itemId != 1539) // greater healing pot is ignored
		// {
		// // Flood protect UseItem
		// if (!getFloodProtectors().getUseItem().tryPerformAction("use item"))
		// return;
		// }
		/*
		 * Alt game - Karma punishment // SOE
		 * 736 Scroll of Escape
		 * 1538 Blessed Scroll of Escape
		 * 1829 Scroll of Escape: Clan Hall
		 * 1830 Scroll of Escape: Castle
		 * 3958 L2Day - Blessed Scroll of Escape
		 * 5858 Blessed Scroll of Escape: Clan Hall
		 * 5859 Blessed Scroll of Escape: Castle
		 * 6663 Scroll of Escape: Orc Village
		 * 6664 Scroll of Escape: Silenos Village
		 * 7117 Scroll of Escape to Talking Island
		 * 7118 Scroll of Escape to Elven Village
		 * 7119 Scroll of Escape to Dark Elf Village
		 * 7120 Scroll of Escape to Orc Village
		 * 7121 Scroll of Escape to Dwarven Village
		 * 7122 Scroll of Escape to Gludin Village
		 * 7123 Scroll of Escape to the Town of Gludio
		 * 7124 Scroll of Escape to the Town of Dion
		 * 7125 Scroll of Escape to Floran
		 * 7126 Scroll of Escape to Giran Castle Town
		 * 7127 Scroll of Escape to Hardin's Private Academy
		 * 7128 Scroll of Escape to Heine
		 * 7129 Scroll of Escape to the Town of Oren
		 * 7130 Scroll of Escape to Ivory Tower
		 * 7131 Scroll of Escape to Hunters Village
		 * 7132 Scroll of Escape to Aden Castle Town
		 * 7133 Scroll of Escape to the Town of Goddard
		 * 7134 Scroll of Escape to the Rune Township
		 * 7135 Scroll of Escape to the Town of Schuttgart.
		 * 7554 Scroll of Escape to Talking Island
		 * 7555 Scroll of Escape to Elven Village
		 * 7556 Scroll of Escape to Dark Elf Village
		 * 7557 Scroll of Escape to Orc Village
		 * 7558 Scroll of Escape to Dwarven Village
		 * 7559 Scroll of Escape to Giran Castle Town
		 * 7618 Scroll of Escape - Ketra Orc Village
		 * 7619 Scroll of Escape - Varka Silenos Village
		 * 10129 Scroll of Escape : Fortress
		 * 10130 Blessed Scroll of Escape : Fortress
		 */
		if (isCursedWeaponEquipped() || (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && getKarma() > 0))
		{
			switch (itemId)
			{
				case 736:
				case 1538:
				case 1829:
				case 1830:
				case 3958:
				case 5858:
				case 5859:
				case 6663:
				case 6664:
				case 7554:
				case 7555:
				case 7556:
				case 7557:
				case 7558:
				case 7559:
				case 7618:
				case 7619:
				case 10129:
				case 10130:
					return;
			}
			if (itemId >= 7117 && itemId <= 7135)
				return;
		}
		// Items that cannot be used
		if (itemId == 57)
			return;
		if (isFishing() && (itemId < 6535 || itemId > 6540))
		{
			// You cannot do anything else while fishing
			SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
			sendPacket(sm);
			sm = null;
			return;
		}
		// Char cannot use item when dead
		if (isDead())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addItemName(item);
			sendPacket(sm);
			sm = null;
			return;
		}
		// Char cannot use pet items
		if ((item.getItem() instanceof L2Armor && item.getItem().getItemType() == L2ArmorType.PET) || (item.getItem() instanceof L2Weapon && item.getItem().getItemType() == L2WeaponType.PET))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_EQUIP_PET_ITEM); // You cannot equip a pet item.
			sm.addItemName(item);
			getClient().getActiveChar().sendPacket(sm);
			sm = null;
			return;
		}
		if (item.isHeroItem())
		{
			if (!isGM())
			{
				if (!canUseHeroItems() && item.isWeapon())
				{
					sendMessage("You must have 55 or more Olympiad matches in the previous period to use hero weapons");
					return;
				}
			}
		}
		if (Config.DEBUG)
			_log.finest(getObjectId() + ": use item " + itemObjectId);
		if (!item.isEquipped())
		{
			if (!item.getItem().checkCondition(this, this, true))
				return;
		}
		if (item.isEquipable())
		{
			// No unequipping/equipping while the player is in special conditions
			if (isStunned() || isSleeping() || isParalyzed() || isAlikeDead() || isOutOfControl())
			{
				sendMessage("Your status does not allow you to do that.");
				return;
			}
			// Don't allow hero equipment and restricted items during Olympiad
			if (isInOlympiadMode() && item.isOlyRestrictedItem())
			{
				/* this.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_CANT_BE_EQUIPPED_FOR_THE_OLYMPIAD_EVENT)); */
				sendMessage("You can only use S grade items or lower in the Olympiad.");
				return;
			}
			if (item.isWeapon())
			{
				if (_inEventCTF && _haveFlagCTF)
				{
					if (!item.isEquipped())
						sendMessage("Can't equip " + item.getName());
					else
						sendMessage("Can't unequip " + item.getName());
					return;
				}
			}
			if (!isGM()) // custom edit
			{
				if (!item.isEquipped())
				{
					if (isInSgradeZone())
					{
						if (item.isARestrictedItemOrcArea())
						{
							sendMessage("Orc and CoT are S80 grade and below only");
							return;
						}
					}
					if (isInS80zone())
					{
						if (item.isARestrictedItemCotArea())
						{
							sendMessage("This item cannot be used in this zone.");
							return;
						}
					}
					if (isInFT())
					{
						if (item.isARestrictedItemFT())
						{
							sendMessage("Forgotten Temple is S80 grade and above only");
							return;
						}
					}
					if (_gearLimit >= 0)
					{
						if (item.isARestrictedItemZone(_gearLimit))
						{
							sendMessage("You can't equip this item in this zone");
							return;
						}
					}
				}
			}
			switch (item.getItem().getBodyPart())
			{
				case L2Item.SLOT_LR_HAND:
				case L2Item.SLOT_L_HAND:
				case L2Item.SLOT_R_HAND:
				{
					// prevent players to equip weapon while wearing combat flag
					if (getActiveWeaponItem() != null && getActiveWeaponItem().getItemId() == 9819)
					{
						this.sendPacket(new SystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
						return;
					}
					// Prevent player to remove the weapon on special conditions
					if (isCastingNow() || isCastingSimultaneouslyNow())
					{
						this.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ITEM_WHILE_USING_MAGIC));
						return;
					}
					if (isMounted())
					{
						this.sendPacket(new SystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
						return;
					}
					if (isDisarmed())
					{
						this.sendPacket(new SystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
						return;
					}
					// Don't allow weapon/shield equipment if a cursed weapon is equiped
					if (isCursedWeaponEquipped())
						return;
					// Don't allow other Race to Wear Kamael exclusive Weapons.
					// if (!item.isEquipped() && item.getItem() instanceof L2Weapon && !isGM())
					// {
					// L2Weapon wpn = (L2Weapon) item.getItem();
					// switch (this.getRace())
					// {
					// case Human:
					// case MHuman:
					// case Dwarf:
					// case Elf:
					// case DarkElf:
					// case Orc:
					// case MOrc:
					// {
					// switch (wpn.getItemType())
					// {
					// case RAPIER:
					// case CROSSBOW:
					// this.sendPacket(new SystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
					// return;
					// }
					// break;
					// }
					// }
					// }
					break;
				}
				case L2Item.SLOT_CHEST:
				case L2Item.SLOT_BACK:
				case L2Item.SLOT_GLOVES:
				case L2Item.SLOT_FEET:
				case L2Item.SLOT_HEAD:
				case L2Item.SLOT_FULL_ARMOR:
				case L2Item.SLOT_LEGS:
				{
					if (this.getRace() == Race.Kamael && (item.getItem().getItemType() == L2ArmorType.HEAVY || item.getItem().getItemType() == L2ArmorType.MAGIC))
					{
						if (item.getCrystalType() < 4)
						{
							this.sendPacket(new SystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
							return;
						}
					}
					break;
				}
				case L2Item.SLOT_DECO:
				{
					if (!item.isEquipped() && getInventory().getMaxTalismanCount() == 0)
					{
						this.sendPacket(new SystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
						return;
					}
				}
			}
			if (isCursedWeaponEquipped() && itemId == 6408) // Don't allow to put formal wear
				return;
			if (isAttackingNow())
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new WeaponEquipTask(item), (getAttackEndTime() - GameTimeController.getGameTicks()) * GameTimeController.MILLIS_IN_TICK);
				return;
			}
			// Equip or unEquip
			if (FortSiegeManager.getInstance().isCombat(item.getItemId()))
				return; // no message
			useEquippableItem(item, true, force);
		}
		else
		{
			L2Weapon weaponItem = getActiveWeaponItem();
			int itemid = item.getItemId();
			if (itemid == 4393)
			{
				this.sendPacket(new ShowCalculator(4393));
			}
			else if ((weaponItem != null && weaponItem.getItemType() == L2WeaponType.ROD) && ((itemid >= 6519 && itemid <= 6527) || (itemid >= 7610 && itemid <= 7613) || (itemid >= 7807 && itemid <= 7809) || (itemid >= 8484 && itemid <= 8486) || (itemid >= 8505 && itemid <= 8513)))
			{
				getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
				broadcastUserInfo();
				// Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
				ItemList il = new ItemList(this, false);
				sendPacket(il);
				return;
			}
			else
			{
				IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
				if (handler == null)
				{
					if (Config.DEBUG)
						_log.warning("No item handler registered for item ID " + item.getItemId() + ".");
				}
				else
				{
					if (isAccountLockedDown() || isInJail())
					{
						if (handler instanceof Gem || handler instanceof Potions || handler instanceof ItemSkills || handler instanceof PetFood || handler instanceof ScrollOfResurrection || handler instanceof RollingDice)
						{}
						else
						{
							sendMessage("Your account is in lockdown");
							return;
						}
					}
					if (handler instanceof ItemSkills)
					{
						if (item.getName().contains("Forgotten") || item.getName().contains("Forbidden"))
						{
							if (!force)
							{
								final String itemName = item.getName();
								ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S2.getId()).addString("Do you wish to use your " + itemName + " and learn the skill?");
								setUseItemRequestItemId(item.getObjectId());
								sendPacket(dlg);
								return;
							}
							else
							{
								setUseItemRequestItemId(0);
							}
						}
					}
					handler.useItem(this, item, force);
				}
			}
		}
	}
	
	public void setUseItemRequestItemId(int useItemRequestItemId)
	{
		_useItemRequestItemId = useItemRequestItemId;
	}
	
	public int getUseItemRequestItemId()
	{
		return _useItemRequestItemId;
	}
	
	/** Weapon Equip Task */
	public class WeaponEquipTask implements Runnable
	{
		L2ItemInstance item;
		
		public WeaponEquipTask(L2ItemInstance it)
		{
			item = it;
		}
		
		public void run()
		{
			// If character is still engaged in strike we should not change weapon
			if (isAttackingNow())
				return;
			// Equip or unEquip
			if (item == null)
				return;
			useEquippableItem(item, false, false);
		}
	}
	
	public ScheduledFuture<?>	_displaySkillTask;
	public int					_displaySkillId	= 1;
	public int					_previousMonthOlympiadGamesPlayed;
	
	public boolean isInAZoneThatDoesntAllowShops()
	{
		for (L2ZoneType zone : ZoneManager.getInstance().getZones(this))
		{
			if (zone != null)
			{
				if (!zone._allowShops)
					return true;
			}
		}
		return false;
	}
	
	public boolean isInSameClanOrAllianceAs(L2PcInstance playerTwo)
	{
		if (playerTwo == null)
			return false;
		final L2Clan clan1 = getClan();
		if (clan1 == null)
			return false;
		final L2Clan clan2 = playerTwo.getClan();
		if (clan2 == null)
			return false;
		if (clan1.getClanId() == clan2.getClanId())
			return true;
		if (clan1.getAllyId() != 0 && clan1.getAllyId() == clan2.getAllyId())
			return true;
		return false;
	}
	
	public void storeName()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET char_name=? WHERE charId=?");
			statement.setString(1, getName());
			statement.setInt(2, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not store char name: " + e);
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
	
	public void wipeHeroOlyStatsDatabase()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM heroes WHERE charId=?");
			statement.setInt(1, getObjectId());
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM olympiad_nobles WHERE charId=?");
			statement.setInt(1, getObjectId());
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM olympiad_nobles_eom WHERE charId=?");
			statement.setInt(1, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not wipe char herooly data " + e + getName());
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
	
	public static void wipeHeroOlyStatsDatabase(int objId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM heroes WHERE charId=?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM olympiad_nobles WHERE charId=?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM olympiad_nobles_eom WHERE charId=?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not wipe char herooly2 data " + e + objId);
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
	
	public boolean canUseHeroItems()
	{
		if (isSupportClassForInstance())
		{
			return _previousMonthOlympiadGamesPlayed >= 20;
		}
		else
			return _previousMonthOlympiadGamesPlayed >= 40;
	}
	
	public void setDisguised(boolean isDisguised)
	{
		_isDisguised = isDisguised;
	}
	
	@Override
	public boolean isDisguised()
	{
		if (isInHuntersVillage() || isInOrcVillage())
			return true;
		return _isDisguised;
	}
	
	private PlayerCounters _playerCountersExtension = null;
	
	public PlayerCounters getCounters()
	{
		if (!Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			return PlayerCounters.DUMMY_COUNTER;
		}
		if (_playerCountersExtension == null)
		{
			synchronized (this)
			{
				if (_playerCountersExtension == null)
				{
					_playerCountersExtension = new PlayerCounters(this);
				}
			}
		}
		return _playerCountersExtension;
	}
	
	public long cklastlogin()
	{
		long chklogin = 0;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT lastactive FROM accounts WHERE login=?");
			statement.setString(1, getAccountName());
			ResultSet rset = statement.executeQuery();
			// Go though the recordset of this SQL query
			while (rset.next())
			{
				chklogin = rset.getLong("lastactive");
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				Date date = new Date(chklogin);
				sendMessage("Last Login: " + dateFormat.format(date));
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore Last Login: " + e);
		}
		return chklogin;
	}
	
	public String getHtmlPrefix()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	private final FastMap<Integer, Long>	_partyRequests	= new FastMap<>();
	private long							_lookingForPartyMembers;
	/** Anti Bot System */
	private String							_botAnswer;
	private String							_farmBotCode;
	private String							_enchantBotCode;
	private int								_Kills			= -1;
	private int								_enchants		= -1;
	private boolean							_farmBot		= false;
	private boolean							_enchantBot		= false;
	private double							_enchantChance	= Config.ALT_GAME_CREATION_SP_RATE;
	public ScheduledFuture<?>				_jailTimer;
	public ScheduledFuture<?>				_enchantChanceTimer;
	
	public void sendChatMessage(int objectId, int messageType, String charName, String text)
	{
		sendPacket(new CreatureSay(objectId, messageType, charName, text));
	}
	
	public boolean isBehind(final L2Object target)
	{
		return isBehind(target, calcStat(Stats.BACK_ANGLE_INCREASE, 50, (L2Character) target, null));
	}
	
	public boolean isBehind(final L2Object target, final double angle)
	{
		double angleChar, angleTarget, angleDiff;
		final double maxAngleDiff = angle;
		if (target == null || maxAngleDiff < 1)
			return false;
		if (maxAngleDiff >= 350)
			return true;
		if (target instanceof L2Character)
		{
			final L2Character target1 = (L2Character) target;
			angleChar = Util.calculateAngleFrom(this, target1);
			angleTarget = Util.convertHeadingToDegree(target1.getHeading());
			angleDiff = angleChar - angleTarget;
			if (angleDiff <= -360 + maxAngleDiff)
				angleDiff += 360;
			if (angleDiff >= 360 - maxAngleDiff)
				angleDiff -= 360;
			if (Math.abs(angleDiff) <= maxAngleDiff)
			{
				return true;
			}
		}
		else
			_log.fine("isBehindTarget's target not an L2 Character.");
		return false;
	}
	
	public boolean isBehindTarget()
	{
		final L2Object target = getTarget();
		if (target == null)
			return false;
		if (target instanceof L2Character)
			return isBehind(target);
		return false;
	}
	
	/**
	 * @param target
	 *            Target to check.
	 * @return True if the target is facing the L2Character.
	 */
	public boolean isInFrontOf(final L2Character target)
	{
		if (target == null)
			return false;
		final double maxAngleDiff = 60;
		final double angleTarget = Util.calculateAngleFrom(target, this);
		final double angleChar = Util.convertHeadingToDegree(target.getHeading());
		double angleDiff = angleChar - angleTarget;
		if (angleDiff <= -360 + maxAngleDiff)
			angleDiff += 360;
		if (angleDiff >= 360 - maxAngleDiff)
			angleDiff -= 360;
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	/**
	 * @param target
	 *            Target to check.
	 * @param maxAngle
	 *            The angle to check.
	 * @return true if target is in front of L2Character (shield def etc)
	 */
	public boolean isFacing(final L2Object target, final int maxAngle)
	{
		if (target == null)
			return false;
		final double maxAngleDiff = maxAngle / 2;
		final double angleTarget = Util.calculateAngleFrom(this, target);
		final double angleChar = Util.convertHeadingToDegree(getHeading());
		double angleDiff = angleChar - angleTarget;
		if (angleDiff <= -360 + maxAngleDiff)
			angleDiff += 360;
		if (angleDiff >= 360 - maxAngleDiff)
			angleDiff -= 360;
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	public boolean isInFrontOfTarget()
	{
		final L2Object target = getTarget();
		if (target == null)
			return false;
		if (target instanceof L2Character)
			return isInFrontOf((L2Character) target);
		return false;
	}
	
	public void setPartyFindValid(boolean a)
	{
		_lookingForPartyMembers = a ? System.currentTimeMillis() : 0;
	}
	
	public boolean isPartyRequestValid(int i)
	{
		if (!_partyRequests.containsKey(i))
		{
			return false;
		}
		if ((System.currentTimeMillis() - _partyRequests.get(i)) < 30000)
		{
			return true;
		}
		_partyRequests.remove(i);
		return false;
	}
	
	public synchronized void addPartyRequest(int i)
	{
		_partyRequests.put(i, System.currentTimeMillis());
	}
	
	public synchronized void removePartyRequest(int i)
	{
		_partyRequests.remove(i);
	}
	
	public boolean isPartyFindValid()
	{
		if ((System.currentTimeMillis() - _lookingForPartyMembers) < 30000)
		{
			return true;
		}
		return false;
	}
	
	// ============================================== //
	// Antibot Engine By L][Sunrise Team //
	// ============================================== //
	public String getBotAnswer()
	{
		return _botAnswer;
	}
	
	public void setBotAnswer(String botAnswer)
	{
		_botAnswer = botAnswer;
	}
	
	public void setFarmBotCode(StringBuilder finalString)
	{
		_farmBotCode = finalString.toString();
	}
	
	public String getFarmBotCode()
	{
		return _farmBotCode;
	}
	
	public void setEnchantBotCode(StringBuilder finalString)
	{
		_enchantBotCode = finalString.toString();
	}
	
	public String getEnchantBotCode()
	{
		return _enchantBotCode;
	}
	
	public void setFarmBot(boolean farmBot)
	{
		_farmBot = farmBot;
	}
	
	public boolean isFarmBot()
	{
		return _farmBot;
	}
	
	public void setEnchantBot(boolean enchantBot)
	{
		_enchantBot = enchantBot;
	}
	
	public boolean isEnchantBot()
	{
		return _enchantBot;
	}
	
	public double getEnchantChance()
	{
		return _enchantChance;
	}
	
	public void setEnchantChance(double enchantChance)
	{
		_enchantChance = enchantChance;
	}
	
	public int getEnchants()
	{
		return _enchants;
	}
	
	public void setEnchants(int enchants)
	{
		_enchants = enchants;
	}
	
	public void setKills(int antiBotKills)
	{
		_Kills = antiBotKills;
	}
	
	public int getKills()
	{
		return _Kills;
	}
	
	private int Tries = 5;
	
	public void setTries(int AntiBotTries)
	{
		Tries = AntiBotTries;
	}
	
	public boolean IsCaptchaValidating()
	{
		return _IsCaptchaValidating;
	}
	
	public void setIsCaptchaValidating(boolean value)
	{
		_IsCaptchaValidating = value;
	}
	
	public int getTries()
	{
		return Tries;
	}
	
	public int getPing()
	{
		return _ping;
	}
	
	public void setPing(int ping)
	{
		_ping = ping;
	}
	
	public String getEmailTemp()
	{
		return _emailTemp;
	}
	
	public void setEmailTemp(String emailTemp)
	{
		_emailTemp = emailTemp;
	}
	
	public String getCode()
	{
		return _code;
	}
	
	public void setCode(String code)
	{
		_code = code;
	}
	
	public String getEmail()
	{
		return _email;
	}
	
	public void setEmail(String email)
	{
		_email = email;
	}
	
	public String getDonateId()
	{
		return _donateId;
	}
	
	public void setDonateId(String donateId)
	{
		_donateId = donateId;
	}
	
	/** Korean Event */
	public boolean	_isInKoreanEvent	= false;
	private String	koreanTeam			= "";
	
	public String getKoreanTeam()
	{
		return koreanTeam;
	}
	
	public void setKoreanTeam(String s)
	{
		koreanTeam = s;
	}
	
	public void setIsInKoreanEvent(boolean val)
	{
		_isInKoreanEvent = val;
	}
	
	public boolean isInKoreanEvent()
	{
		return _isInKoreanEvent;
	}
	
	/** Zombie Event */
	public boolean	_zombie				= false;
	public boolean	_isInZombieEvent	= false;
	
	public void setIsInZombieEvent(boolean val)
	{
		_isInZombieEvent = val;
	}
	
	public boolean isInZombieEvent()
	{
		return _isInZombieEvent;
	}
	
	public void setZombie(boolean val)
	{
		_zombie = val;
	}
	
	public boolean isZombie()
	{
		return _zombie;
	}
	
	/** Last Team Standing Event */
	private boolean					_isInLastTeamStandingEvent	= false;
	private LastTeamStandingTeam	_lastTeamStandingTeam		= LastTeamStanding.LastTeamStandingTeam.NONE;
	
	public boolean isInLastTeamStandingEvent()
	{
		return _isInLastTeamStandingEvent;
	}
	
	public void setIsInLastTeamStandingEvent(boolean val)
	{
		_isInLastTeamStandingEvent = val;
	}
	
	public LastTeamStandingTeam getLastTeamStandingTeam()
	{
		return _lastTeamStandingTeam;
	}
	
	public void setLastTeamStandingTeam(LastTeamStandingTeam team)
	{
		_lastTeamStandingTeam = team;
	}
	
	/** Last Man Standing Event */
	private boolean _isInLastManStandingEvent = false;
	
	public boolean isInLastManStandingEvent()
	{
		return _isInLastManStandingEvent;
	}
	
	public void setIsInLastManStandingEvent(boolean val)
	{
		_isInLastManStandingEvent = val;
	}
	
	/** Domination Event */
	private boolean	_isInDominationEvent	= false;
	public String	dominationTeam			= "";
	
	public String getDominationTeam()
	{
		return dominationTeam;
	}
	
	public void setDominationTeam(String s)
	{
		dominationTeam = s;
	}
	
	public int dominationScore;
	
	public boolean isInDominationEvent()
	{
		return _isInDominationEvent;
	}
	
	public void setIsInDominationEvent(boolean val)
	{
		_isInDominationEvent = val;
	}
	
	private boolean isInEvent;
	
	public void setIsInEvent(boolean val)
	{
		isInEvent = val;
	}
	
	public boolean isInEvent()
	{
		return isInEvent;
	}
	
	private TeamType teamType = TeamType.NONE;
	
	public TeamType getTeamType()
	{
		return teamType;
	}
	
	public void setTeamType(TeamType tt)
	{
		teamType = tt;
	}
	
	public void checkForIncorrectSkillsAndRemove()
	{
		List<Integer> my_skills = new ArrayList<>();
		List<Integer> pvp_skills = new ArrayList<>();
		for (int id : PVP_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : PVP_TRANSFORM_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (L2Skill skill : this.getAllSkills())
		{
			if (my_skills.contains(skill.getId()))
			{
				continue;
			}
			if (!SkillTreeTable.getInstance().getAllSkills().contains(skill.getId()))
			{
				GmListTable.broadcastMessageToAdvancedGMs2("ignored " + skill.getName());
				continue;
			}
			if (pvp_skills.contains(skill.getId()))
			{
				GmListTable.broadcastMessageToAdvancedGMs2("ignored " + skill.getName());
				continue;
			}
			my_skills.add(skill.getId());
		}
		for (L2SkillLearn skill : SkillTreeTable.getInstance().getSkillTrees().get(getClassId()).values())
		{
			if (my_skills.contains(skill.getId()))
			{
				my_skills.remove((Integer) skill.getId());
			}
			else
			{
				GmListTable.broadcastMessageToAdvancedGMs2("?? " + skill.getName());
			}
		}
		for (Integer id : my_skills)
		{
			GmListTable.broadcastMessageToAdvancedGMs2("removed skill " + SkillTable.getInstance().getInfo(id, 1).getName());
			removeSkill(id);
		}
		if (my_skills.size() > 0)
		{
			sendSkillList();
		}
	}
	
	public void checkForIncorrectSkills()
	{
		List<Integer> my_skills = new ArrayList<>();
		List<Integer> pvp_skills = new ArrayList<>();
		for (int id : PVP_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : PVP_TRANSFORM_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : PATH_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : CERTIFICATION_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (L2Skill skill : this.getAllSkills())
		{
			if (my_skills.contains(skill.getId()))
			{
				continue;
			}
			if (!SkillTreeTable.getInstance().getAllSkills().contains(skill.getId()))
			{
				GmListTable.broadcastMessageToAdvancedGMs2("ignored " + skill.getName());
				continue;
			}
			if (pvp_skills.contains(skill.getId()))
			{
				GmListTable.broadcastMessageToAdvancedGMs2("ignored " + skill.getName());
				continue;
			}
			my_skills.add(skill.getId());
		}
		for (L2SkillLearn skill : SkillTreeTable.getInstance().getSkillTrees().get(getClassId()).values())
		{
			if (my_skills.contains(skill.getId()))
			{
				my_skills.remove((Integer) skill.getId());
			}
			else
			{
				// GmListTable.broadcastMessageToAdvancedGMs2("?? " + skill.getName());
			}
		}
		for (Integer id : my_skills)
		{
			GmListTable.broadcastMessageToAdvancedGMs2("removed skill " + SkillTable.getInstance().getInfo(id, 1).getName());
			// removeSkill(id);
		}
		if (my_skills.size() > 0)
		{
			sendSkillList();
		}
	}
	
	public void removeCubics()
	{
		if (!getCubics().isEmpty())
		{
			for (L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction(); // this propably won't be used as well
			}
			getCubics().clear();
		}
	}
	
	/** Raid Event */
	private boolean	_isInRaidEvent	= false;
	public boolean	_isInActiveKoreanRoom;
	
	public boolean isInActiveKoreanRoom()
	{
		return _isInActiveKoreanRoom;
	}
	
	public void setIsInActiveKoreanRoom(boolean val)
	{
		_isInActiveKoreanRoom = val;
	}
	
	public boolean _isInActiveDominationEvent;
	
	public boolean isInActiveDominationEvent()
	{
		return _isInActiveDominationEvent;
	}
	
	public void setIsInActiveDominationEvent(boolean val)
	{
		_isInActiveDominationEvent = val;
	}
	
	public boolean isInRaidEvent()
	{
		return _isInRaidEvent;
	}
	
	public void setIsInRaidEvent(boolean val)
	{
		_isInRaidEvent = val;
	}
	
	private boolean _isEnchantAllAttribute = false;
	
	public void setIsEnchantAllAttribute(boolean isEnchantAllAttribute)
	{
		_isEnchantAllAttribute = isEnchantAllAttribute;
	}
	
	public boolean isEnchantAllAttribute()
	{
		return _isEnchantAllAttribute;
	}
	
	public boolean checkIfThirdClass()
	{
		final ClassId classId = getClassId();
		final int jobLevel = classId.level();
		switch (jobLevel)
		{
			case 0:
			case 1:
			case 2:
				sendMessage("Make 3rd class first.");
				Gem.sendClassChangeHTML(this);
				return false;
		}
		return true;
	}
	
	@Override
	public PlayerPassport getPassport()
	{
		return _passport;
	}
	
	@Override
	public boolean isSamePartyWith(final L2Character character)
	{
		if (character == this)
			return true;
		if (character == null)
			return false;
		final L2Party myParty = getParty();
		if (myParty == null)
			return false;
		final L2Party hisParty = character.getParty();
		if (hisParty == null)
			return false;
		return myParty == hisParty;
	}
	
	public boolean isSameHWID(final L2PcInstance other)
	{
		if (other == null)
			return false;
		final String myHWID = getHWID();
		final String otHWID = other.getHWID();
		return myHWID == otHWID;
	}
	
	
	public List<Quest> getAllQuests(final boolean completed)
	{
		final List<Quest> quests = new ArrayList<>();
		for (final QuestState qs : _quests)
		{
			if (qs == null || completed && qs.isCreated() || !completed && !qs.isStarted())
				continue;
			final Quest quest = qs.getQuest();
			if (quest == null || !quest.isRealQuest())
				continue;
			quests.add(quest);
		}
		return quests;
	}
	
	public int getSexNumber()
	{
		return getSex() ? 1 : 0;
	}
	
	public int getRaceId()
	{
		return _race;
	}
	
	public IInertiaBehave createInertiaBehavior()
	{
		return new PlayerBehave();
	}
	
	public void setLvl(int lvl)
	{
		if (lvl >= 1 && lvl <= Experience.MAX_LEVEL)
		{
			long pXp = getExp();
			long tXp = Experience.LEVEL[lvl];
			if (pXp > tXp)
			{
				removeExpAndSp(pXp - tXp, 0);
			}
			else if (pXp < tXp)
			{
				_ignoreLevel = true;
				addExpAndSp(tXp - pXp, 1000000000);
			}
		}
	}
	
	private final Map<String, PlayerVar> user_variables = new ConcurrentHashMap<String, PlayerVar>();
	
	public static void setVarOffline(int playerObjId, String name, String value, long expireDate)
	{
		L2PcInstance player = L2World.getInstance().getPlayer(playerObjId);
		if (player != null)
			player.setVar(name, value, expireDate);
		else
			mysql.set("REPLACE INTO character_custom_variables (obj_id, type, name, value, expire_time) VALUES (?,'user-var',?,?,?)", playerObjId, name, value, expireDate);
	}
	
	public static void setVarOffline(int playerObjId, String name, int value, long expireDate)
	{
		setVarOffline(playerObjId, name, String.valueOf(value), expireDate);
	}
	
	public static void setVarOffline(int playerObjId, String name, int value)
	{
		setVarOffline(playerObjId, name, String.valueOf(value), -1);
	}
	
	public static void setVarOffline(int playerObjId, String name, long value, long expireDate)
	{
		setVarOffline(playerObjId, name, String.valueOf(value), expireDate);
	}
	
	public static void setVarOffline(int playerObjId, String name, long value)
	{
		setVarOffline(playerObjId, name, String.valueOf(value), -1);
	}
	
	public static void unsetVarOffline(int playerObjId, String name)
	{
		L2PcInstance player = L2World.getInstance().getPlayer(playerObjId);
		if (player != null)
			player.unsetVar(name);
		else
			mysql.set("DELETE FROM `character_custom_variables` WHERE `obj_id`=? AND `type`='user-var' AND `name`=? LIMIT 1", playerObjId, name);
	}
	
	public void setVar(String name, String value, long expireDate)
	{
		if (user_variables.containsKey(name))
		{
			getVarObject(name).stopExpireTask();
		}
		user_variables.put(name, new PlayerVar(this, name, value, expireDate));
		mysql.set("REPLACE INTO character_custom_variables (obj_id, type, name, value, expire_time) VALUES (?,'user-var',?,?,?)", getObjectId(), name, value, expireDate);
	}
	
	public void setVar(String name, String value)
	{
		setVar(name, value, -1);
	}
	
	public void setVar(String name, int value, long expireDate)
	{
		setVar(name, String.valueOf(value), expireDate);
	}
	
	public void setVar(String name, int value)
	{
		setVar(name, String.valueOf(value), -1);
	}
	
	public void setVar(String name, long value, long expireDate)
	{
		setVar(name, String.valueOf(value), expireDate);
	}
	
	public void setVar(String name, long value)
	{
		setVar(name, String.valueOf(value), -1);
	}
	
	public void unsetVar(String name)
	{
		if (name == null)
			return;
		PlayerVar pv = user_variables.remove(name);
		if (pv != null)
		{
			pv.stopExpireTask();
			mysql.set("DELETE FROM `character_custom_variables` WHERE `obj_id`=? AND `type`='user-var' AND `name`=? LIMIT 1", getObjectId(), name);
		}
	}
	
	public String getVar(String name)
	{
		PlayerVar pv = getVarObject(name);
		if (pv == null)
		{
			return null;
		}
		return pv.getValue();
	}
	
	public long getVarTimeToExpire(String name)
	{
		try
		{
			return getVarObject(name).getTimeToExpire();
		}
		catch (NullPointerException npe)
		{}
		return 0;
	}
	
	public PlayerVar getVarObject(String name)
	{
		return user_variables.get(name);
	}
	
	public boolean getVarB(String name, boolean defaultVal)
	{
		PlayerVar pv = getVarObject(name);
		if (pv == null)
		{
			return defaultVal;
		}
		return pv.getValueBoolean();
	}
	
	public boolean getVarB(String name)
	{
		return getVarB(name, false);
	}
	
	public long getVarLong(String name)
	{
		return getVarLong(name, 0L);
	}
	
	public long getVarLong(String name, long defaultVal)
	{
		long result = defaultVal;
		String var = getVar(name);
		if (var != null)
		{
			result = Long.parseLong(var);
		}
		return result;
	}
	
	public int getVarInt(String name)
	{
		return getVarInt(name, 0);
	}
	
	public int getVarInt(String name, int defaultVal)
	{
		int result = defaultVal;
		String var = getVar(name);
		if (var != null)
		{
			result = Integer.parseInt(var);
		}
		return result;
	}
	
	public Map<String, PlayerVar> getVars()
	{
		return user_variables;
	}
	
	private void loadVariables(Connection con)
	{
		try (PreparedStatement offline = con.prepareStatement("SELECT * FROM character_custom_variables WHERE obj_id = ?"))
		{
			offline.setInt(1, getObjectId());
			try (ResultSet rs = offline.executeQuery())
			{
				while (rs.next())
				{
					String name = rs.getString("name");
					String value = Strings.stripSlashes(rs.getString("value"));
					long expire_time = rs.getLong("expire_time");
					long curtime = System.currentTimeMillis();
					if ((expire_time <= curtime) && (expire_time > 0))
					{
						continue;
					}
					user_variables.put(name, new PlayerVar(this, name, value, expire_time));
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public static String getVarFromPlayer(int objId, String var)
	{
		String value = null;
		try (Connection con = L2DatabaseFactory.getConnectionS(); PreparedStatement offline = con.prepareStatement("SELECT value FROM character_custom_variables WHERE obj_id = ? AND name = ?"))
		{
			offline.setInt(1, objId);
			offline.setString(2, var);
			try (ResultSet rs = offline.executeQuery())
			{
				if (rs.next())
				{
					value = Strings.stripSlashes(rs.getString("value"));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return value;
	}
	
	/**
	 * Adding Variable to Map<Name, Value>. It's not saved to database.
	 * Value can be taken back by {@link #getQuickVarO(String, Object...)} method.
	 * 
	 * @param name
	 *            key
	 * @param value
	 *            value
	 */
	public void addQuickVar(String name, Object value)
	{
		if (quickVars.containsKey(name))
			quickVars.remove(name);
		quickVars.put(name, value);
	}
	
	/**
	 * Getting back String Value located in quickVars Map<Name, Value>.
	 * If value doesn't exist, defaultValue is returned.
	 * If value isn't String type, throws Error
	 * 
	 * @param name
	 *            key
	 * @param defaultValue
	 *            Value returned when <code>name</code> key doesn't exist
	 * @return value
	 */
	public String getQuickVarS(String name, String... defaultValue)
	{
		if (!quickVars.containsKey(name))
		{
			if (defaultValue.length > 0)
				return defaultValue[0];
			return null;
		}
		return (String) quickVars.get(name);
	}
	
	/**
	 * Getting back String Value located in quickVars Map<Name, Value>.
	 * If value doesn't exist, defaultValue is returned.
	 * If value isn't Boolean type, throws Error
	 * 
	 * @param name
	 *            key
	 * @param defaultValue
	 *            Value returned when <code>name</code> key doesn't exist
	 * @return value
	 */
	public boolean getQuickVarB(String name, boolean... defaultValue)
	{
		if (!quickVars.containsKey(name))
		{
			if (defaultValue.length > 0)
				return defaultValue[0];
			return false;
		}
		return ((Boolean) quickVars.get(name)).booleanValue();
	}
	
	/**
	 * Getting back Integer Value located in quickVars Map<Name, Value>.
	 * If value doesn't exist, defaultValue is returned.
	 * If value isn't Integer type, throws Error
	 * 
	 * @param name
	 *            key
	 * @param defaultValue
	 *            Value returned when <code>name</code> key doesn't exist
	 * @return value
	 */
	public int getQuickVarI(String name, int... defaultValue)
	{
		if (!quickVars.containsKey(name))
		{
			if (defaultValue.length > 0)
				return defaultValue[0];
			return -1;
		}
		return ((Integer) quickVars.get(name)).intValue();
	}
	
	/**
	 * Getting back Long Value located in quickVars Map<Name, Value>.
	 * If value doesn't exist, defaultValue is returned.
	 * If value isn't Long type, throws Error
	 * 
	 * @param name
	 *            key
	 * @param defaultValue
	 *            Value returned when <code>name</code> key doesn't exist
	 * @return value
	 */
	public long getQuickVarL(String name, long... defaultValue)
	{
		if (!quickVars.containsKey(name))
		{
			if (defaultValue.length > 0)
				return defaultValue[0];
			return -1L;
		}
		return ((Long) quickVars.get(name)).longValue();
	}
	
	/**
	 * Getting back Object Value located in quickVars Map<Name, Value>.
	 * If value doesn't exist, defaultValue is returned.
	 * 
	 * @param name
	 *            key
	 * @param defaultValue
	 *            Value returned when <code>name</code> key doesn't exist
	 * @return value
	 */
	public Object getQuickVarO(String name, Object... defaultValue)
	{
		if (!quickVars.containsKey(name))
		{
			if (defaultValue.length > 0)
				return defaultValue[0];
			return null;
		}
		return quickVars.get(name);
	}
	
	/**
	 * Checking if quickVars Map<Name, Value> contains a name as a Key
	 * 
	 * @param name
	 *            key
	 * @return contains name
	 */
	public boolean containsQuickVar(String name)
	{
		return quickVars.containsKey(name);
	}
	
	/**
	 * Removing Key from quickVars Map
	 * 
	 * @param name
	 *            - key
	 */
	public void deleteQuickVar(String name)
	{
		quickVars.remove(name);
	}
	
	private Map<String, String> _userSession;
	
	public String getSessionVar(String key)
	{
		if (_userSession == null)
		{
			return null;
		}
		return _userSession.get(key);
	}
	
	public void setSessionVar(String key, String val)
	{
		if (_userSession == null)
		{
			_userSession = new ConcurrentHashMap<String, String>();
		}
		if ((val == null) || val.isEmpty())
		{
			_userSession.remove(key);
		}
		else
		{
			_userSession.put(key, val);
		}
	}
	
	private final List<BBSSchemeBufferInstance.PlayerScheme> buffSchemes;
	
	public List<BBSSchemeBufferInstance.PlayerScheme> getBuffSchemes()
	{
		return buffSchemes;
	}
	
	public BBSSchemeBufferInstance.PlayerScheme getBuffSchemeById(int id)
	{
		for (BBSSchemeBufferInstance.PlayerScheme scheme : buffSchemes)
			if (scheme.schemeId == id)
				return scheme;
		return null;
	}
	
	public BBSSchemeBufferInstance.PlayerScheme getBuffSchemeByName(String name)
	{
		for (BBSSchemeBufferInstance.PlayerScheme scheme : buffSchemes)
			if (scheme.schemeName.equals(name))
				return scheme;
		return null;
	}
	
	public void broadcastSkillOrSocialAnimation(int id, int level, int hitTime, int lockActivityTime)
	{
		if (isAlikeDead())
			return;
		boolean performSocialAction = (level < 1);
		if (!performSocialAction)
			broadcastPacket(new MagicSkillUse(this, this, id, level, hitTime, 0));
		else
			broadcastPacket(new SocialAction(getObjectId(), id));
	}
	
	private Map<Integer, Future<?>> _autoPotTasks = new HashMap<>();
	
	public boolean isAutoPot(int id)
	{
		return _autoPotTasks.keySet().contains(id);
	}
	
	public void setAutoPot(int id, Future<?> task, boolean add)
	{
		if (add)
			_autoPotTasks.put(id, task);
		else
		{
			_autoPotTasks.get(id).cancel(true);
			_autoPotTasks.remove(id);
		}
	}
	public boolean isTrying()
	{
		return _isTrying;
	}
	public void setIsTrying(boolean val)
	{
		_isTrying = val;
	}	
}