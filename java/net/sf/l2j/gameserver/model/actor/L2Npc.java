package net.sf.l2j.gameserver.model.actor;

import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.FakePcsTable;
import net.sf.l2j.gameserver.datatables.IconsTable;
import net.sf.l2j.gameserver.datatables.ItemLists;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.TownManager;
import net.sf.l2j.gameserver.instancemanager.games.Lottery;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2DropCategory;
import net.sf.l2j.gameserver.model.L2DropData;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.MobGroupTable;
import net.sf.l2j.gameserver.model.actor.instance.L2BufferInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2ClanHallManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2ControlTowerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2ControllableMobInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoormenInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FestivalGuideInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FishermanInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MinionInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2OlympiadManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TeleporterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2WarehouseInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.NpcKnownList;
import net.sf.l2j.gameserver.model.actor.stat.NpcStat;
import net.sf.l2j.gameserver.model.actor.status.NpcStatus;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.Korean;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.model.itemcontainer.NpcInventory;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.Quest.QuestEventType;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.model.zone.type.L2TownZone;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExShowBaseAttributeCancelWindow;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.RadarControl;
import net.sf.l2j.gameserver.network.serverpackets.ServerObjectInfo;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate.AIType;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.util.StringUtil;
import net.sf.l2j.util.Rnd;

/**
 * This class represents a Non-Player-Character in the world. It can be a monster or a friendly character.
 * It also uses a template to fetch some static values. The templates are hardcoded in the client, so we can rely on them.<BR>
 * <BR>
 * L2Character :<BR>
 * <BR>
 * <li>L2Attackable</li>
 * <li>L2BoxInstance</li>
 * <li>L2FolkInstance</li>
 *
 * @version $Revision: 1.32.2.7.2.24 $ $Date: 2005/04/11 10:06:09 $
 */
public class L2Npc extends L2Character
{
	// private static Logger _log = Logger.getLogger(L2NpcInstance.class.getName());
	/** The interaction distance of the L2NpcInstance(is used as offset in MovetoLocation method) */
	public static final int			INTERACTION_DISTANCE	= 150;
	/** The L2Spawn object that manage this L2NpcInstance */
	private L2Spawn					_spawn;
	public boolean					_isAPC;
	private NpcInventory			_inventory				= null;
	/** The flag to specify if this L2NpcInstance is busy */
	private boolean					_isBusy					= false;
	/** The busy message for this L2NpcInstance */
	private String					_busyMessage			= "";
	/** True if endDecayTask has already been called */
	volatile boolean				_isDecayed				= false;
	protected NpcHtmlMessage		_dropHTML				= null;
	/** True if a Dwarf has used Spoil on this L2NpcInstance */
	private boolean					_isSpoil				= false;
	/** The castle index in the array of L2Castle this L2NpcInstance belongs to */
	private int						_castleIndex			= -2;
	/** The fortress index in the array of L2Fort this L2NpcInstance belongs to */
	private int						_fortIndex				= -2;
	public boolean					isEventMob				= false, _isEventMobTvT = false, _isEventMobDM = false, _isEventMobFOS = false,
	_isEventMobCTF = false, _isCTF_throneSpawn = false, _isCTF_Flag = false, _isFOS_Artifact = false,
	_isEventVIPNPC = false, _isEventVIPNPCEnd = false, _isEventKorean = false, _isEventDomination = false;
	public boolean					_isEventMobTvTi			= false;
	public boolean					_isEventMobSH			= false;
	public boolean					_isLunaEventNpc			= false;
	public String					_CTF_FlagTeamName;
	private boolean					_isInTown				= false;
	private int						_isSpoiledBy			= 0;
	/** Time of last social packet broadcast */
	private long					_lastSocialBroadcast	= 0;
	/** Minimum interval between social packets */
	private final int				_minimalSocialInterval	= 6000;
	protected RandomAnimationTask	_rAniTask				= null;
	private int						_currentLHandId;																						// normally this shouldn't change from the template, but there exist exceptions
	private int						_currentRHandId;																						// normally this shouldn't change from the template, but there exist exceptions
	private float					_currentCollisionHeight	= 0;																			// used for npc grow effect skills
	private float					_currentCollisionRadius	= 0;																			// used for npc grow effect skills
	private boolean					_tempSpawn				= false;
	public String[]					button					=
	{
		"Registration", "Name"
	};
	
	public final boolean isTempSpawn()
	{
		return _tempSpawn;
	}
	
	/** Task launching the function onRandomAnimation() */
	protected class RandomAnimationTask implements Runnable
	{
		public void run()
		{
			try
			{
				if (isAPC() || getNpcId() == 95627)
					return;
				if (this != _rAniTask)
					return; // Shouldn't happen, but who knows... just to make sure every active npc has only one timer.
				if (isMob())
				{
					// Cancel further animation timers until intention is changed to ACTIVE again.
					if (getAI().getIntention() != AI_INTENTION_ACTIVE)
						return;
				}
				else
				{
					if (!isInActiveRegion()) // NPCs in inactive region don't run this task
						return;
				}
				if (!(isDead() || isStunned() || isSleeping() || isParalyzed()))
					onRandomAnimation();
				startRandomAnimationTimer();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2NpcInstance and create a new RandomAnimation Task.<BR>
	 * <BR>
	 */
	public void onRandomAnimation()
	{
		// Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2NpcInstance
		long now = System.currentTimeMillis();
		if (now - _lastSocialBroadcast > _minimalSocialInterval)
		{
			_lastSocialBroadcast = now;
			broadcastPacket(new SocialAction(getObjectId(), Rnd.get(2, 3)));
		}
	}
	
	public static String formatString(long val)
	{
		if (val >= 1000000)
		{
			double valK = (double) val / 1000000;
			return String.valueOf(new DecimalFormat("0.#").format(valK)) + "kk";
		}
		else if (val >= 10000)
		{
			double valK = (double) val / 1000;
			return String.valueOf(new DecimalFormat("0.#").format(valK)) + "k";
		}
		return String.valueOf(val);
	}
	
	private final static char[] c =
	{
		'K',
		'M',
		'B',
		'T'
	};
	
	private static String coolFormat(final double n, final int iteration)
	{
		if (n < 1000)
			return String.valueOf((int) n);
		final double d = (long) n / 100 / 10.0;
		final boolean isRound = d * 10 % 10 == 0;
		return d < 1000 ? (d > 99.9 || isRound || !isRound && d > 9.99 ? (int) d * 10 / 10 : d + "") + "" + c[iteration] : coolFormat(d, iteration + 1);
	}
	
	double					maxhp	= getMaxHp();
	double					curhp	= (getCurrentHp());
	double					value	= (curhp);
	double					percent	= (100 * value / maxhp);
	@SuppressWarnings("unused")
	private L2GameClient	client;
	
	public final double getMobCurrentHp()
	{
		return getCurrentHp();
	}
	
	public final double getCurrentHpRatio()
	{
		return getMobCurrentHp() / getMaxHp();
	}
	
	public final double getCurrentHpPercents()
	{
		return getCurrentHpRatio() * 100;
	}
	
	public String getFactionIdString()
	{
		if (getFactionId() == null || getFactionId().equalsIgnoreCase("NULL") || getFactionId() == "")
			return "NONE";
		return getFactionId();
	}
	
	private String getAttackElementString()
	{
		byte ele = getAttackElement();
		if (ele < 0)
		{
			return "None";
		}
		String element;
		String fontColor;
		switch (ele)
		{
			case 0:
				element = "Fire";
				fontColor = "<font color=FF0000>";
				break;
			case 1:
				element = "Water";
				fontColor = "<font color=7777FF>";
				break;
			case 2:
				element = "Wind";
				fontColor = "<font color=6CC417>";
				break;
			case 3:
				element = "Earth";
				fontColor = "<font color=7F5A58>";
				break;
			case 4:
				element = "Holy";
				fontColor = "<font color=FFF8C6>";
				break;
			case 5:
				element = "Dark";
				fontColor = "<font color=702270>";
				break;
			default:
				element = "lolwtf";
				fontColor = "<font color=702270>";
				break;
		}
		return fontColor + getAttackElementValue(ele) + " " + element + "</font>";
	}
	
	private String getDefenseElementString()
	{
		String elemental = "";
		String fontColor;
		for (int i = 0; i < 6; i++)
		{
			switch (i)
			{
				case 0:
					fontColor = "<font color=FF0000>";
					break;
				case 1:
					fontColor = "<font color=7777FF>";
					break;
				case 2:
					fontColor = "<font color=6CC417>";
					break;
				case 3:
					fontColor = "<font color=7F5A58>";
					break;
				case 4:
					fontColor = "<font color=FFF8C6>";
					break;
				case 5:
					fontColor = "<font color=702270>";
					break;
				default:
					fontColor = "<font color=702270>";
					break;
			}
			elemental = elemental.concat(fontColor + getDefenseElementValue((byte) i) + "</font> ");
		}
		return elemental.trim();
	}
	
	private static String formatRaidbossRespawnTimes(L2Spawn spawn)
	{
		if (spawn == null)
		{
			return "N/A";
		}
		
		double min = spawn.getRespawnMinDelay();
		double max = spawn.getRespawnMaxDelay();
		String minHours = new DecimalFormat("0.#").format(min / 3600);
		String maxHours = new DecimalFormat("0.#").format(max / 3600);
		return minHours + " - " + maxHours + " hrs";
	}
	
	private static String formatMonsterRespawnTime(L2Spawn spawn)
	{
		double time = spawn.getRespawnDelay() / 1000;
		if (time >= 7200)
		{
			String timeHours = new DecimalFormat("0.#").format(time / 3600);
			return timeHours + " hrs";
		}
		else if (time >= 300)
		{
			String timeMins = new DecimalFormat("0.#").format(time / 60);
			return timeMins + " min";
		}
		return new DecimalFormat("0").format(time) + " sec";
	}
	
	/**
	 * Create a RandomAnimation Task that will be launched after the calculated delay.<BR>
	 * <BR>
	 */
	public void startRandomAnimationTimer()
	{
		if (!hasRandomAnimation())
			return;
		int minWait = isMob() ? Config.MIN_MONSTER_ANIMATION : Config.MIN_NPC_ANIMATION;
		int maxWait = isMob() ? Config.MAX_MONSTER_ANIMATION : Config.MAX_NPC_ANIMATION;
		// Calculate the delay before the next animation
		int interval = Rnd.get(minWait, maxWait) * 1000;
		// Create a RandomAnimation Task that will be launched after the calculated delay
		_rAniTask = new RandomAnimationTask();
		ThreadPoolManager.getInstance().scheduleGeneral(_rAniTask, interval);
	}
	
	/**
	 * Check if the server allows Random Animation.<BR>
	 * <BR>
	 */
	public boolean hasRandomAnimation()
	{
		return (!isAPC()) && (Config.MAX_NPC_ANIMATION > 0 && !getTemplate().AI.equals(AIType.CORPSE) && getNpcId() != 95627);
	}
	
	/**
	 * Constructor of L2NpcInstance (use L2Character constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to set the _template of the L2Character (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li>
	 * <li>Set the name of the L2Character</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it</li><BR>
	 * <BR>
	 *
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param template
	 *            The L2NpcTemplate to apply to the NPC
	 */
	public L2Npc(int objectId, L2NpcTemplate template)
	{
		// Call the L2Character constructor to set the _template of the L2Character, copy skills from template to object
		// and link _calculators to NPC_STD_CALCULATOR
		super(objectId, template);
		initCharStatusUpdateValues();
		// initialize the "current" equipment
		_currentLHandId = getTemplate().lhand;
		_currentRHandId = getTemplate().rhand;
		// initialize the "current" collisions
		/*
		 * _currentCollisionHeight = getCollisionHeight();
		 * _currentCollisionRadius = getCollisionRadius();
		 */
		if (template == null)
		{
			_log.severe("No template for Npc. Please check your datapack is setup correctly.");
			return;
		}
		// Set the name of the L2Character
		setName(template.name);
		if (/* (template.ss > 0 || template.bss > 0) && */ template.ssRate > 0)
			_inventory = new NpcInventory(this);
		_isAPC = FakePcsTable.getInstance().getFakePc(getNpcId()) != null;
	}
	
	@Override
	public NpcKnownList getKnownList()
	{
		return (NpcKnownList) super.getKnownList();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new NpcKnownList(this));
	}
	
	@Override
	public NpcStat getStat()
	{
		return (NpcStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new NpcStat(this));
	}
	
	@Override
	public NpcStatus getStatus()
	{
		return (NpcStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new NpcStatus(this));
	}
	
	/** Return the L2NpcTemplate of the L2NpcInstance. */
	@Override
	public final L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) super.getTemplate();
	}
	
	/**
	 * Return the generic Identifier of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 */
	public int getNpcId()
	{
		return getTemplate().npcId;
	}
	
	@Override
	public boolean isAttackable()
	{
		return Config.ALT_ATTACKABLE_NPCS;
	}
	
	/**
	 * Return the faction Identifier of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * If a NPC belows to a Faction, other NPC of the faction inside the Faction range will help it if it's attacked<BR>
	 * <BR>
	 */
	public final String getFactionId()
	{
		return getTemplate().factionId;
	}
	
	/**
	 * Return the Level of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 */
	@Override
	public final int getLevel()
	{
		return getTemplate().level;
	}
	
	/**
	 * Return True if the L2NpcInstance is agressive (ex : L2MonsterInstance in function of aggroRange).<BR>
	 * <BR>
	 */
	public boolean isAggressive()
	{
		return false;
	}
	
	/**
	 * Return the Aggro Range of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 */
	public int getAggroRange()
	{
		return getTemplate().aggroRange;
	}
	
	/**
	 * Return the Faction Range of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 */
	public int getFactionRange()
	{
		return getTemplate().factionRange;
	}
	
	/**
	 * Return True if this L2NpcInstance is undead in function of the L2NpcTemplate.<BR>
	 * <BR>
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead;
	}
	
	/**
	 * Send a packet NpcInfo with state of abnormal effect to all L2PcInstance in the _KnownPlayers of the L2NpcInstance.<BR>
	 * <BR>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		// Send a Server->Client packet NpcInfo with state of abnormal effect to all L2PcInstance in the _KnownPlayers of the L2NpcInstance
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		// synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				if (getRunSpeed() == 0)
					player.sendPacket(new ServerObjectInfo(this, player));
				else
					player.sendPacket(new AbstractNpcInfo.NpcInfo(this, player));
			}
		}
	}
	
	/**
	 * Return the distance under which the object must be add to _knownObject in
	 * function of the object type.<BR>
	 * <BR>
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li>object is a L2FolkInstance : 0 (don't remember it)</li>
	 * <li>object is a L2Character : 0 (don't remember it)</li>
	 * <li>object is a L2PlayableInstance : 1500</li>
	 * <li>others : 500</li>
	 * <BR>
	 * <BR>
	 * <B><U> Override in </U> :</B><BR>
	 * <BR>
	 * <li>L2Attackable</li>
	 * <BR>
	 * <BR>
	 * 
	 * @param object
	 *            The Object to add to _knownObject
	 */
	public int getDistanceToWatchObject(L2Object object)
	{
		if (object instanceof L2FestivalGuideInstance)
			return 10000;
		if (object instanceof L2NpcInstance || !(object instanceof L2Character))
			return 0;
		if (object instanceof L2Playable)
			return 1500;
		return 500;
	}
	
	/**
	 * Return the distance after which the object must be remove from _knownObject in function of the object type.<BR>
	 * <BR>
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li>object is not a L2Character : 0 (don't remember it)</li>
	 * <li>object is a L2FolkInstance : 0 (don't remember it)</li>
	 * <li>object is a L2PlayableInstance : 3000</li>
	 * <li>others : 1000</li><BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2Attackable</li><BR>
	 * <BR>
	 *
	 * @param object
	 *            The Object to remove from _knownObject
	 */
	public int getDistanceToForgetObject(L2Object object)
	{
		return 2 * getDistanceToWatchObject(object);
	}
	
	/**
	 * Return False.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2MonsterInstance : Check if the attacker is not another L2MonsterInstance</li>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
	
	/**
	 * Return the Identifier of the item in the left hand of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 */
	public int getLeftHandItem()
	{
		return _currentLHandId;
	}
	
	/**
	 * Return the Identifier of the item in the right hand of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 */
	public int getRightHandItem()
	{
		return _currentRHandId;
	}
	
	/**
	 * Return True if this L2NpcInstance has drops that can be sweeped.<BR>
	 * <BR>
	 */
	public boolean isSpoil()
	{
		return _isSpoil;
	}
	
	/**
	 * Set the spoil state of this L2NpcInstance.<BR>
	 * <BR>
	 */
	public void setSpoil(boolean isSpoil)
	{
		_isSpoil = isSpoil;
	}
	
	public final int getIsSpoiledBy()
	{
		return _isSpoiledBy;
	}
	
	public final void setIsSpoiledBy(int value)
	{
		_isSpoiledBy = value;
	}
	
	/**
	 * Return the busy status of this L2NpcInstance.<BR>
	 * <BR>
	 */
	public final boolean isBusy()
	{
		return _isBusy;
	}
	
	/**
	 * Set the busy status of this L2NpcInstance.<BR>
	 * <BR>
	 */
	public void setBusy(boolean isBusy)
	{
		_isBusy = isBusy;
	}
	
	/**
	 * Return the busy message of this L2NpcInstance.<BR>
	 * <BR>
	 */
	public final String getBusyMessage()
	{
		return _busyMessage;
	}
	
	/**
	 * Set the busy message of this L2NpcInstance.<BR>
	 * <BR>
	 */
	public void setBusyMessage(String message)
	{
		_busyMessage = message;
	}
	
	/**
	 * Return true if this L2Npc instance can be warehouse manager.<BR>
	 * <BR>
	 */
	public boolean isWarehouse()
	{
		return false;
	}
	
	protected boolean canTarget(L2PcInstance player)
	{
		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		if (player.isLockedTarget() && player.getLockedTarget() != this)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		// TODO: More checks...
		return true;
	}
	
	public boolean canInteract(L2PcInstance player)
	{
		if (player.isCastingNow() || player.isCastingSimultaneouslyNow())
			return false;
		if (player.isDead() || player.isFakeDeath())
			return false;
		if (player.isSitting())
			return false;
		if (player.getPrivateStoreType() != 0)
			return false;
		if (!isInsideRadius(player, INTERACTION_DISTANCE, true, false))
			return false;
		if (player.getInstanceId() != getInstanceId() && player.getInstanceId() != -1)
			return false;
		return true;
	}
	
	/**
	 * Manage actions when a player click on the L2NpcInstance.<BR>
	 * <BR>
	 * <B><U> Actions on first click on the L2NpcInstance (Select it)</U> :</B><BR>
	 * <BR>
	 * <li>Set the L2NpcInstance as target of the L2PcInstance player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li>
	 * <li>If L2NpcInstance is autoAttackable, send a Server->Client packet StatusUpdate to the L2PcInstance in order to update L2NpcInstance HP bar</li>
	 * <li>Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client</li><BR>
	 * <BR>
	 * <B><U> Actions on second click on the L2NpcInstance (Attack it/Intercat with it)</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li>
	 * <li>If L2NpcInstance is autoAttackable, notify the L2PcInstance AI with AI_INTENTION_ATTACK (after a height verification)</li>
	 * <li>If L2NpcInstance is NOT autoAttackable, notify the L2PcInstance AI with AI_INTENTION_INTERACT (after a distance verification) and show message</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Each group of Server->Client packet must be terminated by a ActionFailed packet in order to avoid
	 * that client wait an other packet</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : Action, AttackRequest</li><BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2ArtefactInstance : Manage only fisrt click to select Artefact</li><BR>
	 * <BR>
	 * <li>L2GuardInstance :</li><BR>
	 * <BR>
	 *
	 * @param player
	 *            The L2PcInstance that start an action on the L2NpcInstance
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;
		player.setLastFolkNPC(this);
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			if (Config.DEBUG)
				_log.fine("new target selected:" + getObjectId());
			if (this instanceof L2MonsterInstance)
			{
				if (GameTimeController.getGameTicks() - player.getKillL2AttackableTick() < 6)
				{
					player.incl2nettargetselectTick();
					if (player.getl2nettargetselectTick() > 2)
					{
						// GmListTable.broadcastMessageToGMs("User "+player.getName()+" might be botting! Go check him out - "+player.getl2nettargetselectTick());
					}
				}
				else
					player.setl2nettargetselectTick(0);
			}
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				int diff = player.getLevel() - getLevel();
				if (getLevel() >= 78)
				{
					if (diff > 1)
						diff = 1;
				}
				// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
				// The player.getLevel() - getLevel() permit to display the correct color in the select window
				MyTargetSelected my = new MyTargetSelected(getObjectId(), diff);
				player.sendPacket(my);
				// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			else
			{
				// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
				MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
				player.sendPacket(my);
			}
			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			player.sendPacket(new ValidateLocation(this));
			// Check if the player is attackable (without a forced attack) and isn't dead
			if (isAutoAttackable(player) && !isAlikeDead())
			{
				// Check the height difference
				if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth difference might need some tweaking
				{
					// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
					// player.startAttack(this);
				}
				else
				{
					// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			else if (!isAutoAttackable(player))
			{
				// Calculate the distance between the L2PcInstance and the L2NpcInstance
				if (!canInteract(player))
				{
					// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Send a Server->Client packet SocialAction to the all L2PcInstance on the _knownPlayer of the L2NpcInstance
					// to display a social action of the L2NpcInstance on their client
					long now = System.currentTimeMillis();
					if (now - _lastSocialBroadcast > _minimalSocialInterval && !getTemplate().AI.equals(AIType.CORPSE))
					{
						_lastSocialBroadcast = now;
						broadcastPacket(new SocialAction(getObjectId(), Rnd.get(8)));
					}
					// Open a chat window on client with the text of the L2NpcInstance
					if (isEventMob)
						L2Event.showEventHtml(player, String.valueOf(getObjectId()));
					else if (_isEventMobTvT && (TvT._joining || TvT._started))
						TvT.showEventHtml(player, String.valueOf(getObjectId()));
					else if (_isEventMobTvT && (NewTvT._joining || NewTvT._started))
						NewTvT.showEventHtml(player, String.valueOf(getObjectId()));
					else if (_isEventMobFOS && FOS._started)
						FOS.showEventHtml(player, String.valueOf(getObjectId()));
					else if (_isFOS_Artifact && NewFOS._started)
						NewFOS.showArtifactHtml(player, String.valueOf(getObjectId()));
					else if (_isEventMobDM)
						DM.showEventHtml(player, String.valueOf(getObjectId()));
					else if (_isEventMobCTF)
						CTF.showEventHtml(player, String.valueOf(getObjectId()));
					else if (CTF._started && _isCTF_Flag && player._inEventCTF)
					{
						CTF.showFlagHtml(player, String.valueOf(getObjectId()), _CTF_FlagTeamName);
					}
					else if (NewCTF._started && _isCTF_Flag && player._inEventCTF)
					{
						NewCTF.showFlagHtml(player, String.valueOf(getObjectId()), _CTF_FlagTeamName);
					}
					else if (CTF._started && _isCTF_throneSpawn)
						CTF.CheckRestoreFlags();
					else if (NewCTF._started && _isCTF_throneSpawn)
						NewCTF.CheckRestoreFlags();
					else if (_isEventVIPNPC)
						VIP.showJoinHTML(player, String.valueOf(getObjectId()));
					else if (_isEventVIPNPCEnd)
						VIP.showEndHTML(player, String.valueOf(getObjectId()));
					else if (_isEventKorean)
						Korean.showChatWindow(player);
					else if (_isEventDomination)
						NewDomination.showEventHtml(player, String.valueOf(getObjectId()));
					else if (_isLunaEventNpc)
					{
						String stateFile = "registering.htm";
						Event e = EventEngine.getInstance().getActiveEvent();
						if (e != null)
						{
							if ((NewHuntingGrounds._joining && NewHuntingGrounds._started) || (NewDomination._joining && NewDomination._started) || (NewTvT._joining && NewTvT._started) || (NewCTF._joining && NewCTF._started) || (NewFOS._joining && NewFOS._started) || (NewDM._joining && NewDM._started))
							{
								stateFile = "registering.htm";
							}
							if (NewHuntingGrounds._started || NewDomination._started || NewTvT._started || NewCTF._started || NewFOS._started || NewDM._started)
							{
								stateFile = "started.htm";
							}
							String[] but = generateEventButton(player);
							String button = but[0];
							String buttonName = but[1];
							String html = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), "data/html/custom/Event/" + stateFile);
							html = html.replace("%eventName%", "[" + e.getType() + "] " + e.getName());
							html = html.replace("%minLevel%", String.valueOf(e.getMinLvl()));
							html = html.replace("%maxLevel%", String.valueOf(e.getMaxLvl()));
							html = html.replace("%minPvP%", "%minPvP%");
							html = html.replace("%minFame%", "%minFame%");
							html = html.replace("%time%", Communicator.getInstance().getTimeRemaining());
							html = html.replace("%regPlayers%", String.valueOf(Communicator.getInstance().getRegPlayers()));
							html = html.replace("%maxPlayers%", String.valueOf(e.getMaxPl()));
							html = html.replace("%objectId%", String.valueOf(getObjectId()));
							html = html.replace("%button%", button);
							html = html.replace("%buttonName%", buttonName);
							html = html.replace("%description%", Communicator.getInstance().getEventDescription());
							NpcHtmlMessage msg = new NpcHtmlMessage(5);
							msg.setHtml(html);
							player.sendPacket(msg);
							player.sendPacket(ActionFailed.STATIC_PACKET);
						}
						else
							player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					// else if (_isEventKorean)
					// L2KoreanRegInstance
					else
					{
						// Quest[] qlsa = getTemplate().getEventQuests(QuestEventType.QUEST_START);
						// if ((qlsa != null) && qlsa.length > 0)
						// player.setLastQuestNpcObject(getObjectId());
						// Quest[] qlst = getTemplate().getEventQuests(QuestEventType.ON_FIRST_TALK);
						// if ((qlst != null) && qlst.length == 1)
						// qlst[0].notifyFirstTalk(this, player);
						// else
						showChatWindow(player, 0);
					}
				}
			}
			else
				player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public String[] generateEventButton(L2PcInstance p)
	{
		Event e = EventEngine.getInstance().getActiveEvent();
		if (e != null)
		{
			switch (e.getType())
			{
				case "HuntingGrounds":
					if (NewHuntingGrounds._joining && !NewHuntingGrounds._playersShuffle.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewHuntingGrounds._joining && NewHuntingGrounds._playersShuffle.contains(p))
					{
						button[0] = "bypass -h event_unjoin_";
						button[1] = "Remove Participation";
					}
					break;
				case "Domination":
					if (NewDomination._joining && !NewDomination._playersShuffle.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewDomination._joining && NewDomination._playersShuffle.contains(p))
					{
						button[0] = "bypass -h event_unjoin_";
						button[1] = "Remove Participation";
					}
					break;
				case "TeamVsTeam":
					if (NewTvT._joining && !NewTvT._playersShuffle.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewTvT._joining && NewTvT._playersShuffle.contains(p))
					{
						button[0] = "bypass -h event_unjoin_";
						button[1] = "Remove Participation";
					}
					break;
				case "CTF":
					if (NewCTF._joining && !NewCTF._playersShuffle.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewCTF._joining && NewCTF._playersShuffle.contains(p))
					{
						button[0] = "bypass -h event_unjoin_";
						button[1] = "Remove Participation";
					}
					break;
				case "SiegeEvent":
					if (NewFOS._joining && !NewFOS._playersShuffle.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewFOS._joining && NewFOS._playersShuffle.contains(p))
					{
						button[0] = "bypass -h event_unjoin_";
						button[1] = "Remove Participation";
					}
					break;
				case "DM":
					if (NewDM._joining && !NewDM._players.contains(p))
					{
						button[0] = "bypass -h event_join_";
						button[1] = "Participate";
					}
					if (NewDM._joining && NewDM._players.contains(p))
					{
						button[0] = "bypass -h event_unjoin_";
						button[1] = "Remove Participation";
					}
					break;
			}
		}
		return button;
	}
	
	public String getDropHtml(L2PcInstance player)
	{
		StringBuilder npcinfo = new StringBuilder();
		npcinfo.append("<html><body><title>Info of " + getName() + "</title>").append("					<table width=344 >").append("					<tr><td align=center><button value=\"Combat Stats\" width=300 height=25 action=\"\" fore=\"L2UI_CT1_CN.Deco.Gametip_bg\" back=\"L2UI_CT1_CN.Deco.Gametip_bg\"></td></tr>").append("					</table>").append("					<img src=\"L2UI_CT1_CN.Deco.accordion_btn_glow\" width=300 height=5>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<table width=344 >").append("					<tr><td width=30><font color=696969>Current HP %</font></font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getCurrentHpPercents(), 0) + "%</font>").append("					</td><td width=30><font color=696969>Current HP</font></font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getCurrentHp(), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>Max.HP</font></font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat((getMaxHp()), 0) + "</font>").append("					</td><td width=30><font color=696969>Max.MP</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat((getMaxMp()), 0) + "</font>").append("					</td></tr>").append("					<td width=30></td><td width=50></td></tr>").append("					<tr><td width=30><font color=696969>P.Atk.</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getPAtk(null), 0) + "</font>").append("					</td><td width=30><font color=696969>M.Atk.</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getMAtk(null, null), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>P.Def.</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getPDef(null), 0) + "</font>").append("					</td><td width=30><font color=696969>M.Def.</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getMDef(null, null), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=70><font color=696969>Acc</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getAccuracy(null), 0) + "</font>").append("					</td><td width=30><font color=696969>Evasion</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getEvasionRate(null), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>R.Crit</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getCriticalHit(null, null), 0) + "</font>").append("					</td><td width=30><font color=696969>Speed</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getRunSpeed(), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>Atk.Spd</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getPAtkSpd(null), 0) + "</font>").append("					</td><td width=30><font color=696969>Cst.Spd</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getMAtkSpd(null), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>Faction</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + getFactionIdString() + "</font>").append("					</td><td width=30><font color=696969>Exp.</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getExpReward(), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>Faction Range</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + formatString(getFactionRange()) + "</font>").append("					</td><td width=30><font color=696969>Aggro Range</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + formatString(getAggroRange()) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>Atk.Ele</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + getAttackElementString() + "</font>").append("					</td><td width=30><font color=696969>Def.Ele</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + getDefenseElementString() + "</font>").append("					</td></tr>").append("					</table>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>");
		final L2Spawn spawn = getSpawn();
		String respawntime;
		if (this instanceof L2RaidBossInstance)
		{
			respawntime = formatRaidbossRespawnTimes(spawn);
		}
		else if (this instanceof L2MinionInstance)
		{
			return "";
		}
		else
		{
			respawntime = formatMonsterRespawnTime(spawn);
		}
		npcinfo.append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<table width=344 >").append("					<tr><td align=center><button value=\"Respawn Time & Special Modifiers\" width=330 height=25 action=\"\" fore=\"L2UI_CT1_CN.Deco.Gametip_bg\" back=\"L2UI_CT1_CN.Deco.Gametip_bg\"></td></tr>").append("					</table>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<table width=344 >").append("					<tr><td align=center><font color=696969>    Respawn:  </font>").append("					<font color=dad1b4>" + respawntime + "</font>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2><br></td></tr>");
		for (L2Skill skill : getAllSkills())
		{
			if (skill != null)
			{
				final String desc = skill.getDescription();
				if (desc != null && !desc.equalsIgnoreCase(""))
				{
					StringUtil.append(npcinfo, "					<tr><td align=center<font color=dad1b4>" + desc + "</font>");
					StringUtil.append(npcinfo, "					</td></tr>");
				}
			}
		}
		npcinfo.append("					</table>");
		if (getTemplate().getDropData() != null)
		{
			npcinfo.append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<table width=344 >").append("					<tr><td align=center ><button value=\"Drop Info\" width=300 height=25 action=\"\" fore=\"L2UI_CT1_CN.Deco.Gametip_bg\" back=\"L2UI_CT1_CN.Deco.Gametip_bg\"></td></tr>").append("					</tr>").append("					<tr><td align=center>Rates:   <font color=\"55ff55\">100%</font>   <font color=\"33ffff\">50%+</font>   <font color=\"3366ff\">15%+</font>   ").append("					<font color=\"3239ff\">3%+</font>   <font color=\"ff5194\">1%+</font>   <font color=\"ff3310\">1%-</font></td></table>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>");
			int categorySlot = 1;
			for (L2DropCategory cat : getTemplate().getDropData())
			{
				for (L2DropData drop : cat.getAllDrops())
				{
					boolean list = false;
					if (drop.getItemId() > 1000000)
						list = true;
					L2Item item = null;
					String itemName;
					String itemIcon = null;
					if (!list)
					{
						item = ItemTable.getInstance().getTemplate(drop.getItemId());
						if (item == null)
							continue;
						itemName = item.getName();
						itemIcon = item.getIcon();
					}
					else
					{
						itemName = ItemLists.getInstance().getListName(drop.getItemId());
						itemIcon = IconsTable.getInstance().getItemIcon(drop.getItemId());
					}
					final String color;
					if (drop.getChance() >= 1000000)
						color = "55ff55";
					else if (drop.getChance() >= 500000)
						color = "33ffff";
					else if (drop.getChance() >= 150000)
						color = "3366ff";
					else if (drop.getChance() >= 30000)
						color = "3239ff";
					else if (drop.getChance() >= 10000)
						color = "ff5194";
					else
						color = "ff3310";
					if (drop.getEnchantLevel() > 0)
					{
						if (list || item.isEnchantable())
							itemName = "+" + drop.getEnchantLevel() + " " + itemName;
					}
					else
					{
						if (item != null && item.isEnchantable())
							itemName = "+0" + " " + itemName;
					}
					int min = drop.getMinDrop();
					int max = drop.getMaxDrop();
					final String dropcount;
					if (min < max)
					{
						dropcount = drop.getPartyDropCount() >= 1 ? drop.getPartyDropCount() + " x " + coolFormat(drop.getMinDrop(), 0) : coolFormat(drop.getMinDrop(), 0) + " - " + coolFormat(drop.getMaxDrop(), 0);
					}
					else
					{
						dropcount = drop.getPartyDropCount() >= 1 ? drop.getPartyDropCount() + " x " + coolFormat(drop.getMinDrop(), 0) : coolFormat(drop.getMinDrop(), 0) + " - " + coolFormat(drop.getMaxDrop(), 0);
						// dropcount = drop.getPartyDropCount() >= 1 ? drop.getPartyDropCount() + " x " +coolFormat(drop.getMinDrop(), 0) : coolFormat(drop.getMinDrop(), 0);
					}
					DecimalFormat df = new DecimalFormat("##.##");
					final String[] msgs =
					{
						("never"),
						("Low AF"),
						("QQ"),
						(":'(")
					};
					final String dropchance;
					if (drop.getFakeChance() > 0)
					{
						dropchance = (df.format(drop.getFakeChance()) + "%" + (player.isGM() ? " (" + df.format((double) drop.getChance() / 10000) + "%)" : ""));
					}
					else if (drop.getChance() < 100)
					{
						dropchance = (msgs[Rnd.get(msgs.length)]);
					}
					else
					{
						dropchance = ((df.format((double) drop.getChance() / 10000)) + "%");
					}
					npcinfo.append("<table width=360>" + "    <tr>" + "        <td width=44 height=36 align=center>" + "            <table cellpadding=6 cellspacing=-5>" + "                <tr>" + "                    <td>" + "                        <button width=32 height=32 back=" + itemIcon + " fore=" + itemIcon + ">" + "                    </td>" + "                </tr>" + "            </table>" + "        </td>" + "        <td width=250><font color=" + color + ">" + /*
																																																																																																																								*/ itemName + "</font>" + "            <br>" + "		</td>" + "        <td width=80>" + "			<font> " + dropcount + "</font>" + "            <br1>" + "            <br>" + "		</td>" + "        <td width=80>" + "			<font> [" + String.valueOf(categorySlot) + "]</font>" + "            <br1>" + "            <br>" + "		</td>" + "    </tr>");
				}
				categorySlot++;
			}
			npcinfo.append("</table>").append("<img src=\"L2UI_CT1_CN.Deco.Underline\" width=355 height=2>");
		}
		npcinfo.append("</body></html>");
		return npcinfo.toString();
	}
	
	/**
	 * Manage and Display the GM console to modify the L2NpcInstance (GM only).<BR>
	 * <BR>
	 * <B><U> Actions (If the L2PcInstance is a GM only)</U> :</B><BR>
	 * <BR>
	 * <li>Set the L2NpcInstance as target of the L2PcInstance player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li>
	 * <li>If L2NpcInstance is autoAttackable, send a Server->Client packet StatusUpdate to the L2PcInstance in order to update L2NpcInstance HP bar</li>
	 * <li>Send a Server->Client NpcHtmlMessage() containing the GM console about this L2NpcInstance</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Each group of Server->Client packet must be terminated by a ActionFailed packet in order to avoid
	 * that client wait an other packet</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : Action</li><BR>
	 * <BR>
	 * 
	 * @param client
	 *            The thread that manage the player that pessed Shift and click on the L2NpcInstance
	 */
	@Override
	public void onActionShift(L2GameClient client)
	{
		// Get the L2PcInstance corresponding to the thread
		final L2PcInstance player = client.getActiveChar();
		if (player == null)
			return;
		player.sendPacket(ActionFailed.STATIC_PACKET);
		// Check if the L2PcInstance is a GM
		if (player.isGM() && !player.isHidedGMView())
		{
			player.setTarget(this);
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			// The player.getLevel() - getLevel() permit to display the correct color in the select window
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			// Send a Server->Client NpcHtmlMessage() containing the GM console about this L2NpcInstance
			NpcHtmlMessage html = new NpcHtmlMessage(0);
			String className = getClass().getName().substring(43);
			final StringBuilder html1 = StringUtil.startAppend(500, "<html><body><center><font color=\"LEVEL\">NPC Information</font></center>" + "<br>" + "Instance Type: ", className, "<br1>Faction: ", getFactionId() != null ? getFactionId() : "null", "<br1>");
			StringUtil.append(html1, "Coords ", String.valueOf(getX()), ",", String.valueOf(getY()), ",", String.valueOf(getZ()), "<br1>");
			if (getSpawn() != null)
				StringUtil.append(html1, "Spawn ", String.valueOf(getSpawn().getLocx()), ",", String.valueOf(getSpawn().getLocy()), ",", String.valueOf(getSpawn().getLocz()), " Loc ID: ", String.valueOf(getSpawn().getLocation()), "<br1>", "Distance from spawn 2D ", String.valueOf((int) Math.sqrt(getPlanDistanceSq(getSpawn().getLocx(), getSpawn().getLocy()))), " 3D ", String.valueOf((int) Math.sqrt(getDistanceSq(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz()))), "<br1>");
			if (this instanceof L2ControllableMobInstance)
			{
				StringUtil.append(html1, "Mob Group: ", String.valueOf(MobGroupTable.getInstance().getGroupForMob((L2ControllableMobInstance) this).getGroupId()), "<br>");
			}
			else
			{
				StringUtil.append(html1, "Respawn Time: ", (getSpawn() != null ? String.valueOf(getSpawn().getRespawnDelay() / 1000) : "?"), "  Seconds<br>");
			}
			StringUtil.append(html1, "<center><table><tr><td><button value=\"Edit NPC\" action=\"bypass -h admin_edit_npc ", String.valueOf(getTemplate().npcId), "\" width=80 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" + "<td><button value=\"Show DropList\" action=\"bypass -h admin_show_droplist ", String.valueOf(getTemplate().npcId), "\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" + "<td><button value=\"Skill list\" action=\"bypass -h admin_show_skilllist_npc ", String.valueOf(getTemplate().npcId), "\" width=80 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" + "</table></center><br>");
			StringUtil.append(html1, "<table border=\"0\" width=\"100%\">" + "<tr><td>Object ID</td><td>", String.valueOf(getObjectId()), "</td><td>NPC ID</td><td>", String.valueOf(getTemplate().npcId), "</td></tr>" + "<tr><td>Castle</td><td>" + String.valueOf(getCastle().getCastleId()), "</td><td>AI </td><td>", (hasAI() ? String.valueOf(getAI().getIntention().name()) : "NULL"), "</td></tr>" + "<tr><td>Level</td><td>", String.valueOf(getLevel()), "</td><td>Aggro</td><td>", String.valueOf((this instanceof L2Attackable) ? ((L2Attackable) this).getAggroRange() : 0), "</td></tr>" + "</table><br>" + "<font color=\"LEVEL\">Combat</font>" + "<table border=\"0\" width=\"100%\">" + "<tr><td>Current HP</td><td>", String.valueOf(getCurrentHp()), "</td><td>Current MP</td><td>", String.valueOf(getCurrentMp()), "</td></tr>" + "<tr><td>Max.HP</td><td>", String.valueOf((int) (getMaxHp() / getStat().calcStat(Stats.MAX_HP, 1, this, null))), "*", String.valueOf((int) (getStat().calcStat(Stats.MAX_HP, 1, this, null))), "</td><td>Max.MP</td><td>", String.valueOf(getMaxMp()), "</td></tr>" + "<tr><td>P.Atk.</td><td>", String.valueOf(getPAtk(null)), "</td><td>M.Atk.</td><td>", String.valueOf(getMAtk(null, null)), "</td></tr>" + "<tr><td>P.Def.</td><td>", String.valueOf(getPDef(null)), "</td><td>M.Def.</td><td>", String.valueOf(getMDef(null, null)), "</td></tr>" + "<tr><td>Accuracy</td><td>" + String.valueOf(getAccuracy(null)), "</td><td>Evasion</td><td>", String.valueOf(getEvasionRate(null)), "</td></tr>" + "<tr><td>Critical</td><td>", String.valueOf(getCriticalHit(null, null)), "</td><td>Speed</td><td>", String.valueOf(getRunSpeed()), "</td></tr>" + "<tr><td>Atk.Speed</td><td>", String.valueOf(getPAtkSpd(null)), "</td><td>Cast.Speed</td><td>", String.valueOf(getMAtkSpd(null)), "</td></tr>" + "</table><br>" + "<font color=\"LEVEL\">Basic Stats</font>" + "<table border=\"0\" width=\"100%\">" + "<tr><td>STR</td><td>", String.valueOf(getSTR()), "</td><td>DEX</td><td>", String.valueOf(getDEX()), "</td><td>CON</td><td>", String.valueOf(getCON()), "</td></tr>" + "<tr><td>INT</td><td>", String.valueOf(getINT()), "</td><td>WIT</td><td>", String.valueOf(getWIT()), "</td><td>MEN</td><td>", String.valueOf(getMEN()), "</td></tr>" + "</table>" + "</body></html>");
			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else
		{
			if (player.isHidedGMView() || !player.isGM() && this instanceof L2MonsterInstance && !(this instanceof L2MinionInstance))
			{
				player.setTarget(this);
				// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
				// The player.getLevel() - getLevel() permit to display the correct color in the select window
				MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
				player.sendPacket(my);
				// Check if the player is attackable (without a forced attack)
				if (isAutoAttackable(player))
				{
					// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
					StatusUpdate su = new StatusUpdate(getObjectId());
					su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
					su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
					player.sendPacket(su);
				}
				NpcHtmlMessage html = new NpcHtmlMessage(0);
				StringBuilder npcinfo = new StringBuilder();
				npcinfo.append("<html><body><title>Info of " + getName() + "</title>")
				// .append(" <img src=\"L2UI.SquareBlank\" width=300 height=3>")
				.append("					<table width=344 >")
				// .append(" <tr><td width=32 align=center><img src=\"L2UI_CT1_CN.AutoShotItem.header_weapon\" width=32 height=32></td><td width=50 align=center><font color=ac9887>[Combat Stats]</font></td><td width=32 align=center><img src=\"L2UI_CT1_CN.AutoShotItem.header_weapon\" width=32 height=32></td></tr>")
				.append("					<tr><td align=center><button value=\"Combat Stats\" width=300 height=25 action=\"\" fore=\"L2UI_CT1_CN.Deco.Gametip_bg\" back=\"L2UI_CT1_CN.Deco.Gametip_bg\"></td></tr>").append("					</table>").append("					<img src=\"L2UI_CT1_CN.Deco.accordion_btn_glow\" width=300 height=5>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<table width=344 >").append("					<tr><td width=30><font color=696969>Current HP %</font></font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getCurrentHpPercents(), 0) + "%</font>").append("					</td><td width=30><font color=696969>Current HP</font></font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getCurrentHp(), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>Max.HP</font></font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat((getMaxHp()), 0) + "</font>").append("					</td><td width=30><font color=696969>Max.MP</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat((getMaxMp()), 0) + "</font>").append("					</td></tr>").append("					<td width=30></td><td width=50></td></tr>").append("					<tr><td width=30><font color=696969>P.Atk.</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getPAtk(null), 0) + "</font>").append("					</td><td width=30><font color=696969>M.Atk.</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getMAtk(null, null), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>P.Def.</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getPDef(null), 0) + "</font>").append("					</td><td width=30><font color=696969>M.Def.</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getMDef(null, null), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=70><font color=696969>Acc</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getAccuracy(null), 0) + "</font>").append("					</td><td width=30><font color=696969>Evasion</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getEvasionRate(null), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>R.Crit</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getCriticalHit(null, null), 0) + "</font>").append("					</td><td width=30><font color=696969>Speed</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getRunSpeed(), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>Atk.Spd</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getPAtkSpd(null), 0) + "</font>").append("					</td><td width=30><font color=696969>Cst.Spd</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getMAtkSpd(null), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>Faction</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + getFactionIdString() + "</font>").append("					</td><td width=30><font color=696969>Exp.</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + coolFormat(getExpReward(), 0) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>Faction Range</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + formatString(getFactionRange()) + "</font>").append("					</td><td width=30><font color=696969>Aggro Range</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + formatString(getAggroRange()) + "</font>").append("					</td></tr>").append("					<tr><td width=30><font color=696969>Atk.Ele</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + getAttackElementString() + "</font>").append("					</td><td width=30><font color=696969>Def.Ele</font></td><td align=left  width=50>").append("					<font color=dad1b4> " + getDefenseElementString() + "</font>").append("					</td></tr>").append("					</table>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>");
				final L2Spawn spawn = getSpawn();
				// int time = DayNightSpawnManager.getInstance().getNightCreatures().containsKey(spawn) ? 2 :
				// DayNightSpawnManager.getInstance().getDayCreatures().containsKey(spawn) ? 1 : 0;
				String respawntime;
				if (this instanceof L2RaidBossInstance)
				{
					respawntime = formatRaidbossRespawnTimes(spawn);
				}
				else if (this instanceof L2MinionInstance)
				{
					return;
				}
				else
				{
					respawntime = formatMonsterRespawnTime(spawn);
				}
				npcinfo.append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<table width=344 >").append("					<tr><td align=center><button value=\"Respawn Time & Special Modifiers\" width=330 height=25 action=\"\" fore=\"L2UI_CT1_CN.Deco.Gametip_bg\" back=\"L2UI_CT1_CN.Deco.Gametip_bg\"></td></tr>").append("					</table>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<table width=344 >")
				// .append(" <tr><td width=30px><font color=696969>Elite:</font></td><td>")
				// .append(" <font color=dad1b4> " + String.valueOf((getElite() > 0)) + "</font>")
				// .append(" </td><td width=30px><font color=696969>Rare:</font></td><td>")
				// .append(" <font color=dad1b4> " + String.valueOf((getRare() > 0)) + "</font>")
				// .append(" </td></tr>")
				// .append(" <tr><td width=30px><font color=696969>Random X:</font></td><td>")
				// .append(" <font color=dad1b4>" + String.valueOf(spawn != null ? spawn.getSpawnType() == 1 ? "at a random pc" : spawn.getRandomX() : "N/A") +"</font>")
				// .append(" </td><td width=30px><font color=696969>Random Y:</font></td><td>")
				// .append(" <font color=dad1b4>" + String.valueOf(spawn != null ? spawn.getSpawnType() == 1 ? "at a random pc" : spawn.getRandomY() : "N/A") +"</font>")
				// .append(" </td></tr>")
				// .append(" <tr><td width=30px><font color=696969>Spawn Time:</font></td><td>")
				// .append(" <font color=dad1b4>" + String.valueOf(time == 2 ? "Night" : time == 1 ? "Day" : "Always") + "</font>")
				// .append(" </td>")
				.append("					<tr><td align=center><font color=696969>    Respawn:  </font>").append("					<font color=dad1b4>" + respawntime + "</font>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2><br></td></tr>");
				// .append(" <td width=30><font color=696969>Faction</font></td><td width=50>")
				// .append(" <font color=dad1b4>" + getFactionIdString() + "</font></td>")
				// .append(" <td width=30><font color=696969>AggroRange</font></td><td width=50>")
				// .append(" <font color=dad1b4>" + formatString(getAggroRange()) + "</font>")
				// .append(" </td></tr>")
				// .append(" <tr><td width=30><font color=696969>FactionRange</font></td><td width=50>")
				// .append(" <font color=dad1b4>" + formatString(getFactionRange()) + "</font>")
				// .append(" </td></tr>")
				// .append(" </table>")
				if (getSpawn().getMinPopRequiredToSpawn() >= 10)
					StringUtil.append(npcinfo, "<tr><td align=center><font color=977932>Spawns only if > " + getSpawn().getMinPopRequiredToSpawn() + " players online</font></td></tr>");
				for (L2Skill skill : getAllSkills())
				{
					if (skill != null)
					{
						final String desc = skill.getDescription();
						if (desc != null && !desc.equalsIgnoreCase(""))
						{
							StringUtil.append(npcinfo, "					<tr><td align=center<font color=dad1b4>" + desc + "</font>");
							StringUtil.append(npcinfo, "					</td></tr>");
						}
					}
				}
				npcinfo.append("					</table>");
				if (getTemplate().getDropData() != null)
				{
					npcinfo.append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<table width=344 >").append("					<tr><td align=center ><button value=\"Drop Info\" width=300 height=25 action=\"\" fore=\"L2UI_CT1_CN.Deco.Gametip_bg\" back=\"L2UI_CT1_CN.Deco.Gametip_bg\"></td></tr>").append("					</tr>").append("					<tr><td align=center>Rates:   <font color=\"55ff55\">100%</font>   <font color=\"33ffff\">50%+</font>   <font color=\"3366ff\">15%+</font>   ").append("					<font color=\"3239ff\">3%+</font>   <font color=\"ff5194\">1%+</font>   <font color=\"ff3310\">1%-</font></td></table>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI_CT1_CN.Deco.Underline\" width=300 height=2>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>").append("					<img src=\"L2UI.SquareBlank\" width=300 height=3>");
					int categorySlot = 1;
					for (L2DropCategory cat : getTemplate().getDropData())
					{
						for (L2DropData drop : cat.getAllDrops())
						{
							boolean list = false;
							if (drop.getItemId() > 1000000)
								list = true;
							L2Item item = null;
							String itemName;
							String itemIcon = null;
							if (!list)
							{
								item = ItemTable.getInstance().getTemplate(drop.getItemId());
								if (item == null)
									continue;
								itemName = item.getName();
								itemIcon = item.getIcon();
							}
							else
							{
								itemName = ItemLists.getInstance().getListName(drop.getItemId());
								itemIcon = IconsTable.getInstance().getItemIcon(drop.getItemId());
							}
							final String color;
							/*
							 * if (drop.getChance() >= 1000000)
							 * color = "b6aa7e";
							 * else if (drop.getChance() >= 500000)
							 * color = "afb7d2";
							 * else if (drop.getChance() >= 150000)
							 * color = "afd2ca";
							 * else if (drop.getChance() >= 30000)
							 * color = "d2afb7";
							 * else if (drop.getChance() >= 10000)
							 * color = "945463";
							 * else
							 * color = "945463";
							 */
							if (drop.getChance() >= 1000000)
								color = "55ff55";
							else if (drop.getChance() >= 500000)
								color = "33ffff";
							else if (drop.getChance() >= 150000)
								color = "3366ff";
							else if (drop.getChance() >= 30000)
								color = "3239ff";
							else if (drop.getChance() >= 10000)
								color = "ff5194";
							else
								color = "ff3310";
							if (drop.getEnchantLevel() > 0)
							{
								if (list || item.isEnchantable())
									itemName = "+" + drop.getEnchantLevel() + " " + itemName;
							}
							else
							{
								if (item != null && item.isEnchantable())
									itemName = "+0" + " " + itemName;
							}
							int min = drop.getMinDrop();
							int max = drop.getMaxDrop();
							final String dropcount;
							if (min < max)
							{
								dropcount = drop.getPartyDropCount() >= 1 ? drop.getPartyDropCount() + " x " + coolFormat(drop.getMinDrop(), 0) : coolFormat(drop.getMinDrop(), 0) + " - " + coolFormat(drop.getMaxDrop(), 0);
							}
							else
							{
								dropcount = drop.getPartyDropCount() >= 1 ? drop.getPartyDropCount() + " x " + coolFormat(drop.getMinDrop(), 0) : coolFormat(drop.getMinDrop(), 0) + " - " + coolFormat(drop.getMaxDrop(), 0);
								// dropcount = drop.getPartyDropCount() >= 1 ? drop.getPartyDropCount() + " x " +coolFormat(drop.getMinDrop(), 0) : coolFormat(drop.getMinDrop(), 0);
							}
							DecimalFormat df = new DecimalFormat("##.##");
							final String[] msgs =
							{
								("never"),
								("Low AF"),
								("QQ"),
								(":'(")
							};
							final String dropchance;
							if (drop.getFakeChance() > 0)
							{
								dropchance = (df.format(drop.getFakeChance()) + "%" + (player.isGM() ? " (" + df.format((double) drop.getChance() / 10000) + "%)" : ""));
							}
							else if (drop.getChance() < 100)
							{
								dropchance = (msgs[Rnd.get(msgs.length)]);
							}
							else
							{
								dropchance = ((df.format((double) drop.getChance() / 10000)) + "%");
							}
							npcinfo.append("<table width=360>" + "    <tr>" + "        <td width=44 height=36 align=center>" + "            <table cellpadding=6 cellspacing=-5>" + "                <tr>" + "                    <td>" + "                        <button width=32 height=32 back=" + itemIcon + " fore=" + itemIcon + ">" + "                    </td>" + "                </tr>" + "            </table>" + "        </td>" + "        <td width=250><font color=" + color + ">" + /*
																																																																																																																										 * ["+ dropcount +"
																																																																																																																										 * ]
																																																																																																																										 * "
																																																																																																																										 * +
																																																																																																																										 */ itemName + "</font>" + "            <br>" + "		</td>" + "        <td width=80>" + "			<font> " + dropcount + "</font>" + "            <br1>" + "            <br>" + "		</td>" + "        <td width=80>" + "			<font> [" + String.valueOf(categorySlot) + "]</font>" + "            <br1>" + "            <br>" + "		</td>" + "    </tr>");
							// .append("<table width=320 >")
							// .append("<tr><td height=36 width=32><img src="+ itemIcon +" width=32 height=32></td>"
							// + "<td width=80><font color=" + color + ">[" + dropchance +"]</font></td>"
							// + "<td width=100><font color=" + color + ">" + itemName +"</font></td>"
							// + "<td></font></td><td align=left width=80>" + dropcount + "</td>"
							// + "<td width=35>" + String.valueOf(categorySlot) + "</td></tr>");
						}
						categorySlot++;
					}
					npcinfo.append("</table>").append("<img src=\"L2UI_CT1_CN.Deco.Underline\" width=355 height=2>");
				}
				npcinfo.append("</body></html>");
				html.setHtml(npcinfo.toString());
				player.sendPacket(html);
			}
			else
			{
				if (!canTarget(player))
					return;
				// Check if the L2PcInstance is confused
				if (player.isOutOfControl())
					return;
				// Aggression target lock effect
				if (player.isLockedTarget() && player.getLockedTarget() != this)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
					return;
				}
				// Check if the L2PcInstance already target the L2NpcInstance
				if (this != player.getTarget())
				{
					if (Config.DEBUG)
						_log.fine("new target selected:" + getObjectId());
					// Set the target of the L2PcInstance player
					player.setTarget(this);
					// Check if the player is attackable (without a forced attack)
					if (isAutoAttackable(player))
					{
						// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
						// The player.getLevel() - getLevel() permit to display the correct color in the select window
						MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
						player.sendPacket(my);
						// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
						StatusUpdate su = new StatusUpdate(getObjectId());
						su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
						su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
						player.sendPacket(su);
					}
					else
					{
						// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
						MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
						player.sendPacket(my);
					}
					// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
					player.sendPacket(new ValidateLocation(this));
				}
				else
				{
					player.sendPacket(new ValidateLocation(this));
					if (Config.ALT_GAME_VIEWNPC && this instanceof L2Attackable)
					{
						player.sendPacket(_dropHTML);
					}
					else if (!isAutoAttackable(player))
					{
						// Calculate the distance between the L2PcInstance and the L2NpcInstance
						if (canInteract(player))
						{
							// Send a Server->Client packet SocialAction to the all L2PcInstance on the _knownPlayer of the L2NpcInstance
							// to display a social action of the L2NpcInstance on their client
							SocialAction sa = new SocialAction(getObjectId(), Rnd.get(8));
							broadcastPacket(sa);
							// Open a chat window on client with the text of the L2NpcInstance
							if (isEventMob)
							{
								L2Event.showEventHtml(player, String.valueOf(getObjectId()));
							}
							else
							{
								// Quest[] qlsa = getTemplate().getEventQuests(QuestEventType.QUEST_START);
								// if ((qlsa != null) && qlsa.length > 0)
								// player.setLastQuestNpcObject(getObjectId());
								// Quest[] qlst = getTemplate().getEventQuests(QuestEventType.ON_FIRST_TALK);
								// if ((qlst != null) && qlst.length == 1)
								// qlst[0].notifyFirstTalk(this, player);
								// else
								showChatWindow(player, 0);
							}
						}
					}
				}
			}
		}
	}
	
	/** Return the L2Castle this L2NpcInstance belongs to. */
	public final Castle getCastle()
	{
		// Get castle this NPC belongs to (excluding L2Attackable)
		if (_castleIndex < 0)
		{
			L2TownZone town = TownManager.getTown(getX(), getY(), getZ());
			if (town != null)
				_castleIndex = CastleManager.getInstance().getCastleIndex(town.getTaxById());
			if (_castleIndex < 0)
			{
				_castleIndex = CastleManager.getInstance().findNearestCastleIndex(this);
			}
			else
				_isInTown = true; // Npc was spawned in town
		}
		if (_castleIndex < 0)
			return null;
		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}
	
	/** Return the L2Fort this L2NpcInstance belongs to. */
	public final Fort getFort()
	{
		// Get Fort this NPC belongs to (excluding L2Attackable)
		if (_fortIndex < 0)
		{
			Fort fort = FortManager.getInstance().getFort(getX(), getY(), getZ());
			if (fort != null)
				_fortIndex = FortManager.getInstance().getFortIndex(fort.getFortId());
			if (_fortIndex < 0)
				_fortIndex = FortManager.getInstance().findNearestFortIndex(this);
		}
		if (_fortIndex < 0)
			return null;
		return FortManager.getInstance().getForts().get(_fortIndex);
	}
	
	public final boolean getIsInTown()
	{
		if (_castleIndex < 0)
			getCastle();
		return _isInTown;
	}
	
	/**
	 * Open a quest or chat window on client with the text of the L2NpcInstance in function of the command.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : RequestBypassToServer</li><BR>
	 * <BR>
	 *
	 * @param command
	 *            The command string received from client
	 */
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		// if (canInteract(player))
		{
			if (isBusy() && getBusyMessage().length() > 0)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/npcbusy.htm");
				html.replace("%busymessage%", getBusyMessage());
				html.replace("%npcname%", getName());
				html.replace("%playername%", player.getName());
				player.sendPacket(html);
			}
			else if (command.equalsIgnoreCase("TerritoryStatus"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				{
					if (getCastle().getOwnerId() > 0)
					{
						html.setFile("data/html/territorystatus.htm");
						L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
						html.replace("%clanname%", clan.getName());
						html.replace("%clanleadername%", clan.getLeaderName());
					}
					else
					{
						html.setFile("data/html/territorynoclan.htm");
					}
				}
				html.replace("%castlename%", getCastle().getName());
				html.replace("%taxpercent%", "" + getCastle().getTaxPercent());
				html.replace("%objectId%", String.valueOf(getObjectId()));
				{
					if (getCastle().getCastleId() > 6)
					{
						html.replace("%territory%", "The Kingdom of Elmore");
					}
					else
					{
						html.replace("%territory%", "The Kingdom of Aden");
					}
				}
				player.sendPacket(html);
			}
			else if (command.startsWith("Quest"))
			{
				String quest = "";
				try
				{
					quest = command.substring(5).trim();
				}
				catch (final IndexOutOfBoundsException ioobe)
				{}
				if (quest.isEmpty())
					showQuestWindowGeneral(player, this);
				else
					showQuestWindowSingle(player, this, QuestManager.getInstance().getQuest(quest));
			}
			else if (command.startsWith("Chat"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch (IndexOutOfBoundsException ioobe)
				{}
				catch (NumberFormatException nfe)
				{}
				showChatWindow(player, val);
			}
			else if (command.startsWith("Link"))
			{
				String path = command.substring(5).trim();
				if (path.indexOf("..") != -1)
					return;
				String filename = "data/html/" + path;
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
			}
			else if (command.startsWith("NobleTeleport"))
			{
				if (!player.isNoble())
				{
					String filename = "data/html/teleporter/nobleteleporter-no.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
					return;
				}
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch (IndexOutOfBoundsException ioobe)
				{}
				catch (NumberFormatException nfe)
				{}
				showChatWindow(player, val);
			}
			else if (command.startsWith("Loto"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch (IndexOutOfBoundsException ioobe)
				{}
				catch (NumberFormatException nfe)
				{}
				if (val == 0)
				{
					// new loto ticket
					for (int i = 0; i < 5; i++)
						player.setLoto(i, 0);
				}
				showLotoWindow(player, val);
			}
			else if (command.startsWith("CPRecovery"))
			{
				makeCPRecovery(player);
			}
			else if (command.startsWith("SupportMagicServitor"))
			{
				player.sendMessage("Disabled");
			}
			else if (command.startsWith("SupportMagic"))
			{
				player.sendMessage("Disabled");
			}
			else if (command.startsWith("GiveBlessing"))
			{
				player.sendMessage("Disabled");
			}
			else if (command.startsWith("multisell"))
			{
				int listId = Integer.parseInt(command.substring(9).trim());
				L2Multisell.getInstance().separateAndSend(listId, player, getNpcId(), false, getCastle().getTaxRate());
			}
			else if (command.startsWith("exc_multisell"))
			{
				int listId = Integer.parseInt(command.substring(13).trim());
				L2Multisell.getInstance().separateAndSend(listId, player, getNpcId(), true, getCastle().getTaxRate());
			}
			else if (command.startsWith("Augment"))
			{
				/*
				 * int cmdChoice = Integer.parseInt(command.substring(8, 9).trim());
				 * switch (cmdChoice)
				 * {
				 * case 1:
				 * player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED));
				 * player.sendPacket(new ExShowVariationMakeWindow());
				 * break;
				 * case 2:
				 * player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION));
				 * player.sendPacket(new ExShowVariationCancelWindow());
				 * break;
				 * }
				 */
				player.sendMessage("Use the cubic");
			}
			else if (command.startsWith("npcfind_byid"))
			{
				try
				{
					L2Spawn spawn = SpawnTable.getInstance().getTemplate(Integer.parseInt(command.substring(12).trim()));
					if (spawn != null)
					{
						player.sendPacket(new RadarControl(2, 2, spawn.getCurX(), spawn.getCurY(), spawn.getCurZ()));
						player.sendPacket(new RadarControl(0, 1, spawn.getCurX(), spawn.getCurY(), spawn.getCurZ()));
					}
				}
				catch (NumberFormatException nfe)
				{
					player.sendMessage("Wrong command parameters");
				}
			}
			else if (command.startsWith("EnterRift"))
			{
				try
				{
					Byte b1 = Byte.parseByte(command.substring(10)); // Selected Area: Recruit, Soldier etc
					DimensionalRiftManager.getInstance().start(player, b1, this);
				}
				catch (Exception e)
				{}
			}
			else if (command.startsWith("ChangeRiftRoom"))
			{
				if (player.isInParty() && player.getParty().isInDimensionalRift())
				{
					player.getParty().getDimensionalRift().manualTeleport(player, this);
				}
				else
				{
					DimensionalRiftManager.getInstance().handleCheat(player, this);
				}
			}
			else if (command.startsWith("remove_dp"))
			{
				int cmdChoice = Integer.parseInt(command.substring(10, 11).trim());
				int[] pen_clear_price =
				{
					3600, 8640, 25200, 50400, 86400, 144000, 144000, 144000
				};
				switch (cmdChoice)
				{
					case 1:
						String filename = "data/html/default/30981-1.htm";
						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(filename);
						html.replace("%objectId%", String.valueOf(getObjectId()));
						html.replace("%dp_price%", String.valueOf(pen_clear_price[player.getExpertiseIndex()]));
						player.sendPacket(html);
						break;
					case 2:
						NpcHtmlMessage Reply = new NpcHtmlMessage(getObjectId());
						final StringBuilder replyMSG = StringUtil.startAppend(400, "<html><body>Black Judge:<br>");
						if (player.getDeathPenaltyBuffLevel() > 0)
						{
							if (player.getAdena() >= pen_clear_price[player.getExpertiseIndex()])
							{
								if (!player.reduceAdena("DeathPenality", pen_clear_price[player.getExpertiseIndex()], this, true))
									return;
								player.setDeathPenaltyBuffLevel(player.getDeathPenaltyBuffLevel() - 1);
								player.sendPacket(new SystemMessage(SystemMessageId.DEATH_PENALTY_LIFTED));
								player.sendPacket(new EtcStatusUpdate(player));
								return;
							}
							else
							{
								replyMSG.append("The wound you have received from death's touch is too deep to be healed for the money you have to give me. Find more money if you wish death's mark to be fully removed from you.");
							}
						}
						else
						{
							replyMSG.append("You have no more death wounds that require healing.<br>" + "Go forth and fight, both for this world and your own glory.");
						}
						replyMSG.append("</body></html>");
						Reply.setHtml(replyMSG.toString());
						player.sendPacket(Reply);
						break;
				}
			}
			else if (command.startsWith("ExitRift"))
			{
				if (player.isInParty() && player.getParty().isInDimensionalRift())
				{
					player.getParty().getDimensionalRift().manualExitRift(player, this);
				}
				else
				{
					DimensionalRiftManager.getInstance().handleCheat(player, this);
				}
			}
			else if (command.startsWith("ReleaseAttribute"))
			{
				player.sendPacket(new ExShowBaseAttributeCancelWindow(player));
			}
		}
	}
	
	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR>
	 * <BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		// regular NPCs dont have weapons instancies
		return null;
	}
	
	/**
	 * Return the weapon item equiped in the right hand of the L2NpcInstance or null.<BR>
	 * <BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		int weaponId = 0;
		if (isAPC())
			weaponId = FakePcsTable.getInstance().getFakePc(getNpcId()).pdRHand;
		else
			weaponId = getTemplate().rhand;
		if (weaponId < 1)
			return null;
		L2Item item = ItemTable.getInstance().getTemplate(weaponId);
		if (!(item instanceof L2Weapon))
			return null;
		return (L2Weapon) item;
	}
	
	public void giveBlessingSupport(L2PcInstance player)
	{
		if (player == null)
			return;
		// Blessing of protection - author kerberos_20. Used codes from Rayan - L2Emu project.
		// Prevent a cursed weapon weilder of being buffed - I think no need of that becouse karma check > 0
		// if (player.isCursedWeaponEquiped())
		// return;
		int player_level = player.getLevel();
		// Select the player
		setTarget(player);
		// If the player is too high level, display a message and return
		if (player_level > 39 || player.getClassId().level() >= 2)
		{
			String content = "<html><body>Newbie Guide:<br>I'm sorry, but you are not eligible to receive the protection blessing.<br1>It can only be bestowed on <font color=\"LEVEL\">characters below level 39 who have not made a seccond transfer.</font></body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}
		L2Skill skill = SkillTable.getInstance().getInfo(5182, 1);
		doCast(skill);
	}
	
	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR>
	 * <BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		// regular NPCs dont have weapons instancies
		return null;
	}
	
	/**
	 * Return the weapon item equiped in the left hand of the L2NpcInstance or null.<BR>
	 * <BR>
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		// Get the weapon identifier equiped in the right hand of the L2NpcInstance
		int weaponId = getTemplate().lhand;
		if (weaponId < 1)
			return null;
		// Get the weapon item equiped in the right hand of the L2NpcInstance
		L2Item item = ItemTable.getInstance().getTemplate(getTemplate().lhand);
		if (!(item instanceof L2Weapon))
			return null;
		return (L2Weapon) item;
	}
	
	/**
	 * Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance.<BR>
	 * <BR>
	 * 
	 * @param player
	 *            The L2PcInstance who talks with the L2NpcInstance
	 * @param content
	 *            The text of the L2NpcMessage
	 */
	public void insertObjectIdAndShowChatWindow(L2PcInstance player, String content)
	{
		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		content = content.replaceAll("%objectId%", String.valueOf(getObjectId()));
		NpcHtmlMessage npcReply = new NpcHtmlMessage(getObjectId());
		npcReply.setHtml(content);
		player.sendPacket(npcReply);
	}
	
	/**
	 * Return the pathfile of the selected HTML file in function of the npcId and of the page number.<BR>
	 * <BR>
	 * <B><U> Format of the pathfile </U> :</B><BR>
	 * <BR>
	 * <li>if the file exists on the server (page number = 0) : <B>data/html/default/12006.htm</B> (npcId-page number)</li>
	 * <li>if the file exists on the server (page number > 0) : <B>data/html/default/12006-1.htm</B> (npcId-page number)</li>
	 * <li>if the file doesn't exist on the server : <B>data/html/npcdefault.htm</B> (message : "I have nothing to say to you")</li><BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2GuardInstance : Set the pathfile to data/html/guard/12006-1.htm (npcId-page number)</li><BR>
	 * <BR>
	 * 
	 * @param npcId
	 *            The Identifier of the L2NpcInstance whose text must be display
	 * @param val
	 *            The number of the page to display
	 */
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;
		String temp = "data/html/default/" + pom + ".htm";
		if (!Config.LAZY_CACHE)
		{
			// If not running lazy cache the file must be in the cache or it doesnt exist
			if (HtmCache.getInstance().contains(temp))
				return temp;
		}
		else
		{
			if (HtmCache.getInstance().isLoadable(temp))
				return temp;
		}
		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}
	
	/**
	 * Open a choose quest window on client with all quests available of the L2NpcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li><BR>
	 * <BR>
	 * 
	 * @param player
	 *            The L2PcInstance that talk with the L2NpcInstance
	 * @param quests
	 *            The table containing quests of the L2NpcInstance
	 */
	public void showQuestChooseWindow(L2PcInstance player, Quest[] quests)
	{
		final StringBuilder sb = StringUtil.startAppend(150, "<html><body>");
		// for (Quest q : quests)
		// {
		// StringUtil.append(sb, "<a action=\"bypass -h npc_", String.valueOf(getObjectId()), "_Quest ", q.getName(), "\">[", q.getDescr());
		// QuestState qs = player.getQuestState(q.getScriptName());
		// if (qs != null)
		// {
		// if (qs.getState() == State.STARTED && qs.getInt("cond") > 0)
		// {
		// sb.append(" (In Progress)");
		// }
		// else if (qs.getState() == State.COMPLETED)
		// {
		// sb.append(" (Done)");
		// }
		// }
		// sb.append("]</a><br>");
		// }
		sb.append("</body></html>");
		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		insertObjectIdAndShowChatWindow(player, sb.toString());
	}
	
	/**
	 * Open a quest window on client with the text of the L2NpcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the text of the quest state in the folder data/scripts/quests/questId/stateId.htm</li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet</li><BR>
	 * <BR>
	 * 
	 * @param player
	 *            The L2PcInstance that talk with the L2NpcInstance
	 * @param questId
	 *            The Identifier of the quest to display the message
	 */
	public void showQuestWindow(L2PcInstance player, String questId)
	{
		String content = null;
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public static void showQuestWindowGeneral(final L2PcInstance player, final L2Npc npc)
	{
		final List<Quest> quests = new ArrayList<>();
		final List<Quest> awaits = npc.getTemplate().getEventQuests(QuestEventType.ON_TALK);
		if (awaits != null)
			for (final Quest quest : awaits)
			{
				if (quest == null || !quest.isRealQuest() || quests.contains(quest))
					continue;
				final QuestState qs = player.getQuestState(quest.getName());
				if (qs == null || qs.isCreated())
					continue;
				quests.add(quest);
			}
		final List<Quest> starts = npc.getTemplate().getEventQuests(QuestEventType.QUEST_START);
		if (starts != null)
			for (final Quest quest : starts)
			{
				if (quest == null || !quest.isRealQuest() || quests.contains(quest))
					continue;
				quests.add(quest);
			}
		if (quests.isEmpty())
			showQuestWindowSingle(player, npc, null);
		else if (quests.size() == 1)
			showQuestWindowSingle(player, npc, quests.get(0));
		else
			showQuestWindowChoose(player, npc, quests);
	}
	
	public static void showQuestWindowSingle(final L2PcInstance player, final L2Npc npc, final Quest quest)
	{
		if (quest == null)
		{
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc.getObjectId());
			npcReply.setHtml(Quest.getNoQuestMsg());
			npcReply.replace("%objectId%", String.valueOf(npc.getObjectId()));
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		QuestState qs = player.getQuestState(quest.getName());
		if (qs == null)
		{
			if (quest.isRealQuest() && player.getAllQuests(false).size() > 40)
			{
				player.sendPacket(SystemMessageId.TOO_MANY_QUESTS);
				return;
			}
			final List<Quest> qlst = npc.getTemplate().getEventQuests(QuestEventType.QUEST_START);
			if (qlst != null && qlst.contains(quest))
				qs = quest.newQuestState(player);
		}
		if (qs != null)
			quest.notifyTalk(npc, qs.getPlayer());
	}
	
	/**
	 * Shows the list of available quest of the L2Npc.
	 * 
	 * @param player
	 *            The L2PcInstance that talk with the L2Npc.
	 * @param npc
	 *            The L2Npc instance.
	 * @param quests
	 *            The list containing quests of the L2Npc.
	 */
	public static void showQuestWindowChoose(final L2PcInstance player, final L2Npc npc, final List<Quest> quests)
	{
		final StringBuilder sb = StringUtil.startAppend(150, "<html><body>");
		for (final Quest q : quests)
		{
			if (q == null)
				continue;
			StringUtil.append(sb, "<a action=\"bypass -h npc_", String.valueOf(npc.getObjectId()), "_Quest ", q.getName(), "\">[", q.getDescr());
			final QuestState qs = player.getQuestState(q.getName());
			if (qs != null && qs.isStarted())
				sb.append(" (In Progress)]</a><br>");
			else if (qs != null && qs.isCompleted())
				sb.append(" (Done)]</a><br>");
			else
				sb.append("]</a><br>");
		}
		sb.append("</body></html>");
		final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc.getObjectId());
		npcReply.setHtml(sb.toString());
		npcReply.replace("%objectId%", String.valueOf(npc.getObjectId()));
		player.sendPacket(npcReply);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Collect awaiting quests/start points and display a QuestChooseWindow (if several available) or QuestWindow.<BR>
	 * <BR>
	 * 
	 * @param player
	 *            The L2PcInstance that talk with the L2NpcInstance
	 */
	public void showQuestWindow(L2PcInstance player)
	{
		// collect awaiting quests and start points
		List<Quest> options = new FastList<Quest>();
		QuestState[] awaits = player.getQuestsForTalk(getTemplate().npcId);
		// Quest[] starts = getTemplate().getEventQuests(QuestEventType.QUEST_START);
		// Quests are limited between 1 and 999 because those are the quests that are supported by the client.
		// By limiting them there, we are allowed to create custom quests at higher IDs without interfering
		if (awaits != null)
		{
			for (QuestState x : awaits)
			{
				if (!options.contains(x.getQuest()))
					if ((x.getQuest().getQuestId() > 0) && (x.getQuest().getQuestId() < 20000))
						options.add(x.getQuest());
			}
		}
		// if (starts != null)
		// {
		// for (Quest x : starts)
		// {
		// if (!options.contains(x))
		// if ((x.getQuestId() > 0) && (x.getQuestId() < 20000))
		// options.add(x);
		// }
		// }
		// Display a QuestChooseWindow (if several quests are available) or QuestWindow
		if (options.size() > 1)
		{
			showQuestChooseWindow(player, options.toArray(new Quest[options.size()]));
		}
		else if (options.size() == 1)
		{
			showQuestWindow(player, options.get(0).getName());
		}
		else
		{
			showQuestWindow(player, "");
		}
	}
	
	/**
	 * Open a Loto window on client with the text of the L2NpcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet</li><BR>
	 * 
	 * @param player
	 *            The L2PcInstance that talk with the L2NpcInstance
	 * @param val
	 *            The number of the page of the L2NpcInstance to display
	 */
	// 0 - first buy lottery ticket window
	// 1-20 - buttons
	// 21 - second buy lottery ticket window
	// 22 - selected ticket with 5 numbers
	// 23 - current lottery jackpot
	// 24 - Previous winning numbers/Prize claim
	// >24 - check lottery ticket by item object id
	public void showLotoWindow(L2PcInstance player, int val)
	{
		int npcId = getTemplate().npcId;
		String filename;
		SystemMessage sm;
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (val == 0) // 0 - first buy lottery ticket window
		{
			filename = (getHtmlPath(npcId, 1));
			html.setFile(filename);
		}
		else if (val >= 1 && val <= 21) // 1-20 - buttons, 21 - second buy lottery ticket window
		{
			if (!Lottery.getInstance().isStarted())
			{
				// tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD));
				return;
			}
			if (!Lottery.getInstance().isSellableTickets())
			{
				// tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE));
				return;
			}
			filename = (getHtmlPath(npcId, 5));
			html.setFile(filename);
			int count = 0;
			int found = 0;
			// counting buttons and unsetting button if found
			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == val)
				{
					// unsetting button
					player.setLoto(i, 0);
					found = 1;
				}
				else if (player.getLoto(i) > 0)
				{
					count++;
				}
			}
			// if not rearched limit 5 and not unseted value
			if (count < 5 && found == 0 && val <= 20)
				for (int i = 0; i < 5; i++)
					if (player.getLoto(i) == 0)
					{
						player.setLoto(i, val);
						break;
					}
			// setting pusshed buttons
			count = 0;
			for (int i = 0; i < 5; i++)
				if (player.getLoto(i) > 0)
				{
					count++;
					String button = String.valueOf(player.getLoto(i));
					if (player.getLoto(i) < 10)
						button = "0" + button;
					String search = "fore=\"L2UI.lottoNum" + button + "\" back=\"L2UI.lottoNum" + button + "a_check\"";
					String replace = "fore=\"L2UI.lottoNum" + button + "a_check\" back=\"L2UI.lottoNum" + button + "\"";
					html.replace(search, replace);
				}
			if (count == 5)
			{
				String search = "0\">Return";
				String replace = "22\">The winner selected the numbers above.";
				html.replace(search, replace);
			}
		}
		else if (val == 22) // 22 - selected ticket with 5 numbers
		{
			if (!Lottery.getInstance().isStarted())
			{
				// tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD));
				return;
			}
			if (!Lottery.getInstance().isSellableTickets())
			{
				// tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE));
				return;
			}
			long price = Config.ALT_LOTTERY_TICKET_PRICE;
			int lotonumber = Lottery.getInstance().getId();
			int enchant = 0;
			int type2 = 0;
			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == 0)
					return;
				if (player.getLoto(i) < 17)
					enchant += Math.pow(2, player.getLoto(i) - 1);
				else
					type2 += Math.pow(2, player.getLoto(i) - 17);
			}
			if (player.getAdena() < price)
			{
				sm = new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				player.sendPacket(sm);
				return;
			}
			if (!player.reduceAdena("Loto", price, this, true))
				return;
			Lottery.getInstance().increasePrize(price);
			sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
			sm.addNumber(lotonumber);
			sm.addItemName(4442);
			player.sendPacket(sm);
			L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), 4442);
			item.setCount(1);
			item.setCustomType1(lotonumber);
			item.setEnchantLevel(enchant);
			item.setCustomType2(type2);
			player.getInventory().addItem("Loto", item, player, this);
			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(item);
			L2ItemInstance adenaupdate = player.getInventory().getItemByItemId(57);
			iu.addModifiedItem(adenaupdate);
			player.sendPacket(iu);
			filename = (getHtmlPath(npcId, 3));
			html.setFile(filename);
		}
		else if (val == 23) // 23 - current lottery jackpot
		{
			filename = (getHtmlPath(npcId, 3));
			html.setFile(filename);
		}
		else if (val == 24) // 24 - Previous winning numbers/Prize claim
		{
			filename = (getHtmlPath(npcId, 4));
			html.setFile(filename);
			int lotonumber = Lottery.getInstance().getId();
			String message = "";
			for (L2ItemInstance item : player.getInventory().getItems())
			{
				if (item == null)
					continue;
				if (item.getItemId() == 4442 && item.getCustomType1() < lotonumber)
				{
					message = message + "<a action=\"bypass -h npc_%objectId%_Loto " + item.getObjectId() + "\">" + item.getCustomType1() + " Event Number ";
					int[] numbers = Lottery.getInstance().decodeNumbers(item.getEnchantLevel(), item.getCustomType2());
					for (int i = 0; i < 5; i++)
					{
						message += numbers[i] + " ";
					}
					long[] check = Lottery.getInstance().checkTicket(item);
					if (check[0] > 0)
					{
						switch ((int) check[0])
						{
							case 1:
								message += "- 1st Prize";
								break;
							case 2:
								message += "- 2nd Prize";
								break;
							case 3:
								message += "- 3th Prize";
								break;
							case 4:
								message += "- 4th Prize";
								break;
						}
						message += " " + check[1] + "a.";
					}
					message += "</a><br>";
				}
			}
			if (message.isEmpty())
			{
				message += "There is no winning lottery ticket...<br>";
			}
			html.replace("%result%", message);
		}
		else if (val > 24) // >24 - check lottery ticket by item object id
		{
			int lotonumber = Lottery.getInstance().getId();
			L2ItemInstance item = player.getInventory().getItemByObjectId(val);
			if (item == null || item.getItemId() != 4442 || item.getCustomType1() >= lotonumber)
				return;
			long[] check = Lottery.getInstance().checkTicket(item);
			sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			sm.addItemName(4442);
			sm.addItemNumber(1);
			player.sendPacket(sm);
			long adena = check[1];
			if (adena > 0)
				player.addAdena("Loto", adena, this, true);
			player.destroyItem("Loto", item, this, false);
			return;
		}
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%race%", "" + Lottery.getInstance().getId());
		html.replace("%adena%", "" + Lottery.getInstance().getPrize());
		html.replace("%ticket_price%", "" + Config.ALT_LOTTERY_TICKET_PRICE);
		html.replace("%prize5%", "" + (Config.ALT_LOTTERY_5_NUMBER_RATE * 100));
		html.replace("%prize4%", "" + (Config.ALT_LOTTERY_4_NUMBER_RATE * 100));
		html.replace("%prize3%", "" + (Config.ALT_LOTTERY_3_NUMBER_RATE * 100));
		html.replace("%prize2%", "" + Config.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE);
		html.replace("%enddate%", "" + DateFormat.getDateInstance().format(Lottery.getInstance().getEndDate()));
		player.sendPacket(html);
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void makeCPRecovery(L2PcInstance player)
	{
		if (getNpcId() != 31225 && getNpcId() != 31226)
			return;
		if (player.isCursedWeaponEquipped())
		{
			player.sendMessage("Go away, you're not welcome here.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		int neededmoney = 100;
		if (!player.reduceAdena("RestoreCP", neededmoney, player.getLastFolkNPC(), true))
			return;
		L2Skill skill = SkillTable.getInstance().getInfo(4380, 1);
		if (skill != null)
		{
			setTarget(player);
			doCast(skill);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Add Newbie helper buffs to L2Player according to its level.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the range level in wich player must be to obtain buff</li>
	 * <li>If player level is out of range, display a message and return</li>
	 * <li>According to player level cast buff</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> Newbie Helper Buff list is define in sql table helper_buff_list</B></FONT><BR>
	 * <BR>
	 * 
	 * @param player
	 *            The L2PcInstance that talk with the L2NpcInstance
	 */
	public void makeSupportMagic(L2PcInstance player, boolean isSummon)
	{
		/*
		 * if (player == null)
		 * return;
		 * // Prevent a cursed weapon weilder of being buffed
		 * if (player.isCursedWeaponEquipped())
		 * return;
		 * int player_level = player.getLevel();
		 * int lowestLevel = 0;
		 * int highestLevel = 0;
		 * if (isSummon)
		 * {
		 * if (player.getPet() == null || !(player.getPet() instanceof L2SummonInstance))
		 * {
		 * String content = "<html><body>Only servitors can receive this Support Magic. If you do not have a servitor, you cannot access these spells.</body></html>";
		 * insertObjectIdAndShowChatWindow(player, content);
		 * return;
		 * }
		 * setTarget(player.getPet());
		 * }
		 * else
		 * // Select the player
		 * setTarget(player);
		 * if (isSummon)
		 * {
		 * lowestLevel = HelperBuffTable.getInstance().getServitorLowestLevel();
		 * highestLevel = HelperBuffTable.getInstance().getServitorHighestLevel();
		 * }
		 * else
		 * {
		 * // Calculate the min and max level between which the player must be to obtain buff
		 * if (player.isMageClass())
		 * {
		 * lowestLevel = HelperBuffTable.getInstance().getMagicClassLowestLevel();
		 * highestLevel = HelperBuffTable.getInstance().getMagicClassHighestLevel();
		 * }
		 * else
		 * {
		 * lowestLevel = HelperBuffTable.getInstance().getPhysicClassLowestLevel();
		 * highestLevel = HelperBuffTable.getInstance().getPhysicClassHighestLevel();
		 * }
		 * }
		 * // If the player is too high level, display a message and return
		 * if (player_level > highestLevel)
		 * {
		 * String content = "<html><body>Newbie Guide:<br>Only a <font color=\"LEVEL\">novice character of level " + highestLevel
		 * + " or less</font> can receive my support magic.<br>Your novice character is the first one that you created and raised in this world.</body></html>";
		 * insertObjectIdAndShowChatWindow(player, content);
		 * return;
		 * }
		 * // If the player is too low level, display a message and return
		 * if (player_level < lowestLevel)
		 * {
		 * String content = "<html><body>Come back here when you have reached level " + lowestLevel + ". I will give you support magic then.</body></html>";
		 * insertObjectIdAndShowChatWindow(player, content);
		 * return;
		 * }
		 * L2Skill skill = null;
		 * if (isSummon)
		 * {
		 * for (L2HelperBuff helperBuffItem : HelperBuffTable.getInstance().getHelperBuffTable())
		 * {
		 * if (helperBuffItem.isForSummon())
		 * {
		 * skill = SkillTable.getInstance().getInfo(helperBuffItem.getSkillID(), helperBuffItem.getSkillLevel());
		 * if (skill != null)
		 * doCast(skill);
		 * }
		 * }
		 * }
		 * else
		 * {
		 * // Go through the Helper Buff list define in sql table helper_buff_list and cast skill
		 * for (L2HelperBuff helperBuffItem : HelperBuffTable.getInstance().getHelperBuffTable())
		 * {
		 * if (helperBuffItem.isMagicClassBuff() == player.isMageClass())
		 * {
		 * if (player_level >= helperBuffItem.getLowerLevel() && player_level <= helperBuffItem.getUpperLevel())
		 * {
		 * skill = SkillTable.getInstance().getInfo(helperBuffItem.getSkillID(), helperBuffItem.getSkillLevel());
		 * if (skill.getSkillType() == L2SkillType.SUMMON)
		 * player.doSimultaneousCast(skill);
		 * else
		 * doCast(skill);
		 * }
		 * }
		 * }
		 * }
		 */
	}
	
	public void showChatWindow(L2PcInstance player)
	{
		showChatWindow(player, 0);
	}
	
	/**
	 * Returns true if html exists
	 * 
	 * @param player
	 * @param type
	 * @return boolean
	 */
	private boolean showPkDenyChatWindow(L2PcInstance player, String type)
	{
		String html = HtmCache.getInstance().getHtm("data/html/" + type + "/" + getNpcId() + "-pk.htm");
		if (html != null)
		{
			NpcHtmlMessage pkDenyMsg = new NpcHtmlMessage(getObjectId());
			pkDenyMsg.setHtml(html);
			player.sendPacket(pkDenyMsg);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		return false;
	}
	
	/**
	 * Open a chat window on client with the text of the L2NpcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li>
	 * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet</li><BR>
	 * 
	 * @param player
	 *            The L2PcInstance that talk with the L2NpcInstance
	 * @param val
	 *            The number of the page of the L2NpcInstance to display
	 */
	public void showChatWindow(L2PcInstance player, int val)
	{
		if (player.isCursedWeaponEquipped() && (!(player.getTarget() instanceof L2ClanHallManagerInstance) || !(player.getTarget() instanceof L2DoormenInstance)))
		{
			player.setTarget(player);
			return;
		}
		if (player.getKarma() > 0)
		{
			if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof L2MerchantInstance)
			{
				if (showPkDenyChatWindow(player, "merchant"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && this instanceof L2TeleporterInstance)
			{
				if (showPkDenyChatWindow(player, "teleporter"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && this instanceof L2WarehouseInstance)
			{
				if (showPkDenyChatWindow(player, "warehouse"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof L2FishermanInstance)
			{
				if (showPkDenyChatWindow(player, "fisherman"))
					return;
			}
		}
		if ("L2Auctioneer".equals(getTemplate().type) && val == 0)
			return;
		int npcId = getTemplate().npcId;
		/* For use with Seven Signs implementation */
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
		int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
		boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
		int compWinner = SevenSigns.getInstance().getCabalHighestScore();
		switch (npcId)
		{
			case 31078:
			case 31079:
			case 31080:
			case 31081:
			case 31082: // Dawn Priests
			case 31083:
			case 31084:
			case 31168:
			case 31692:
			case 31694:
			case 31997:
				switch (playerCabal)
				{
					case SevenSigns.CABAL_DAWN:
						if (isSealValidationPeriod)
							if (compWinner == SevenSigns.CABAL_DAWN)
								if (compWinner != sealGnosisOwner)
									filename += "dawn_priest_2c.htm";
								else
									filename += "dawn_priest_2a.htm";
							else
								filename += "dawn_priest_2b.htm";
						else
							filename += "dawn_priest_1b.htm";
						break;
					case SevenSigns.CABAL_DUSK:
						if (isSealValidationPeriod)
							filename += "dawn_priest_3b.htm";
						else
							filename += "dawn_priest_3a.htm";
						break;
					default:
						if (isSealValidationPeriod)
							if (compWinner == SevenSigns.CABAL_DAWN)
								filename += "dawn_priest_4.htm";
							else
								filename += "dawn_priest_2b.htm";
						else
							filename += "dawn_priest_1a.htm";
						break;
				}
				break;
			case 31085:
			case 31086:
			case 31087:
			case 31088: // Dusk Priest
			case 31089:
			case 31090:
			case 31091:
			case 31169:
			case 31693:
			case 31695:
			case 31998:
				switch (playerCabal)
				{
					case SevenSigns.CABAL_DUSK:
						if (isSealValidationPeriod)
							if (compWinner == SevenSigns.CABAL_DUSK)
								if (compWinner != sealGnosisOwner)
									filename += "dusk_priest_2c.htm";
								else
									filename += "dusk_priest_2a.htm";
							else
								filename += "dusk_priest_2b.htm";
						else
							filename += "dusk_priest_1b.htm";
						break;
					case SevenSigns.CABAL_DAWN:
						if (isSealValidationPeriod)
							filename += "dusk_priest_3b.htm";
						else
							filename += "dusk_priest_3a.htm";
						break;
					default:
						if (isSealValidationPeriod)
							if (compWinner == SevenSigns.CABAL_DUSK)
								filename += "dusk_priest_4.htm";
							else
								filename += "dusk_priest_2b.htm";
						else
							filename += "dusk_priest_1a.htm";
						break;
				}
				break;
			/*
			 * case 31095: //
			 * case 31096: //
			 * case 31097: //
			 * case 31098: // Enter Necropolises
			 * case 31099: //
			 * case 31100: //
			 * case 31101: //
			 * case 31102: //
			 * if (isSealValidationPeriod)
			 * {
			 * if (playerCabal != compWinner || sealAvariceOwner != compWinner)
			 * {
			 * switch (compWinner)
			 * {
			 * case SevenSigns.CABAL_DAWN:
			 * player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
			 * filename += "necro_no.htm";
			 * break;
			 * case SevenSigns.CABAL_DUSK:
			 * player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
			 * filename += "necro_no.htm";
			 * break;
			 * case SevenSigns.CABAL_NULL:
			 * filename = (getHtmlPath(npcId, val)); // do the default!
			 * break;
			 * }
			 * }
			 * else
			 * filename = (getHtmlPath(npcId, val)); // do the default!
			 * }
			 * else
			 * {
			 * if (playerCabal == SevenSigns.CABAL_NULL)
			 * filename += "necro_no.htm";
			 * else
			 * filename = (getHtmlPath(npcId, val)); // do the default!
			 * }
			 * break;
			 * case 31114: //
			 * case 31115: //
			 * case 31116: // Enter Catacombs
			 * case 31117: //
			 * case 31118: //
			 * case 31119: //
			 * if (isSealValidationPeriod)
			 * {
			 * if (playerCabal != compWinner || sealGnosisOwner != compWinner)
			 * {
			 * switch (compWinner)
			 * {
			 * case SevenSigns.CABAL_DAWN:
			 * player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
			 * filename += "cata_no.htm";
			 * break;
			 * case SevenSigns.CABAL_DUSK:
			 * player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
			 * filename += "cata_no.htm";
			 * break;
			 * case SevenSigns.CABAL_NULL:
			 * filename = (getHtmlPath(npcId, val)); // do the default!
			 * break;
			 * }
			 * }
			 * else
			 * filename = (getHtmlPath(npcId, val)); // do the default!
			 * }
			 * else
			 * {
			 * if (playerCabal == SevenSigns.CABAL_NULL)
			 * filename += "cata_no.htm";
			 * else
			 * filename = (getHtmlPath(npcId, val)); // do the default!
			 * }
			 * break;
			 */
			case 31111: // Gatekeeper Spirit (Disciples)
				if (playerCabal == sealAvariceOwner && playerCabal == compWinner)
				{
					switch (sealAvariceOwner)
					{
						case SevenSigns.CABAL_DAWN:
							filename += "spirit_dawn.htm";
							break;
						case SevenSigns.CABAL_DUSK:
							filename += "spirit_dusk.htm";
							break;
						case SevenSigns.CABAL_NULL:
							filename += "spirit_null.htm";
							break;
					}
				}
				else
				{
					filename += "spirit_null.htm";
				}
				break;
			case 31112: // Gatekeeper Spirit (Disciples)
				filename += "spirit_exit.htm";
				break;
			case 31127: //
			case 31128: //
			case 31129: // Dawn Festival Guides
			case 31130: //
			case 31131: //
				filename += "festival/dawn_guide.htm";
				break;
			case 31137: //
			case 31138: //
			case 31139: // Dusk Festival Guides
			case 31140: //
			case 31141: //
				filename += "festival/dusk_guide.htm";
				break;
			case 31092: // Black Marketeer of Mammon
				filename += "blkmrkt_1.htm";
				break;
			case 31113: // Merchant of Mammon
				switch (compWinner)
				{
					case SevenSigns.CABAL_DAWN:
						if (playerCabal != compWinner || playerCabal != sealAvariceOwner)
						{
							player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
							player.sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						break;
					case SevenSigns.CABAL_DUSK:
						if (playerCabal != compWinner || playerCabal != sealAvariceOwner)
						{
							player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
							player.sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						break;
				}
				filename += "mammmerch_1.htm";
				break;
			case 31126: // Blacksmith of Mammon
				switch (compWinner)
				{
					case SevenSigns.CABAL_DAWN:
						if (playerCabal != compWinner || playerCabal != sealGnosisOwner)
						{
							player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
							player.sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						break;
					case SevenSigns.CABAL_DUSK:
						if (playerCabal != compWinner || playerCabal != sealGnosisOwner)
						{
							player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
							player.sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						break;
				}
				filename += "mammblack_1.htm";
				break;
			case 31132:
			case 31133:
			case 31134:
			case 31135:
			case 31136: // Festival Witches
			case 31142:
			case 31143:
			case 31144:
			case 31145:
			case 31146:
				filename += "festival/festival_witch.htm";
				break;
			case 31688:
			case 1000176:
			case 1000177:
			case 1000178:
			case 1000179:
				if (player.isNoble())
					filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
				else
					filename = (getHtmlPath(npcId, val));
				break;
			case 31690:
			case 31769:
			case 31770:
			case 31771:
			case 31772:
				if (player.isHero() && !player.isFakeHero() && !player._tempHero)
					filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm";
				else
					filename = (getHtmlPath(npcId, val));
				break;
			case 36402:
				if (player.olyBuff > 0)
					filename = (player.olyBuff == 5 ? Olympiad.OLYMPIAD_HTML_PATH + "olympiad_buffs.htm" : Olympiad.OLYMPIAD_HTML_PATH + "olympiad_5buffs.htm");
				else
					filename = Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm";
				break;
			default:
				if (npcId >= 31865 && npcId <= 31918)
				{
					if (val == 0)
						filename += "rift/GuardianOfBorder.htm";
					else
						filename += "rift/GuardianOfBorder-" + val + ".htm";
					break;
				}
				if ((npcId >= 31093 && npcId <= 31094) || (npcId >= 31172 && npcId <= 31201) || (npcId >= 31239 && npcId <= 31254))
					return;
				// Get the text of the selected HTML file in function of the npcId and of the page number
				filename = (getHtmlPath(npcId, val));
				break;
		}
		// Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		// String word = "npc-"+npcId+(val>0 ? "-"+val : "" )+"-dialog-append";
		if (this instanceof L2MerchantInstance)
			if (Config.LIST_PET_RENT_NPC.contains(npcId))
				html.replace("_Quest", "_RentPet\">Rent Pet</a><br><a action=\"bypass -h npc_%objectId%_Quest");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStr());
		if (this instanceof L2BufferInstance)
		{
			final int curBuffs = player.getBuffCount();
			html.replace("%lol%", String.valueOf(curBuffs));
			final int maxBuffs = player.getMaxBuffCount();
			html.replace("%nig%", String.valueOf(maxBuffs));
		}
		if (this instanceof L2OlympiadManagerInstance)
		{
			final int matches = Olympiad.getInstance().getCompetitionDone(player.getObjectId());
			final int wins = Olympiad.getInstance().getCompetitionWon(player.getObjectId());
			final int loses = Olympiad.getInstance().getCompetitionLost(player.getObjectId());
			final int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
			html.replace("%matches%", String.valueOf(matches));
			html.replace("%wins%", String.valueOf(wins));
			html.replace("%loses%", String.valueOf(loses));
			html.replace("%points%", String.valueOf(points));
		}
		player.sendPacket(html);
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Open a chat window on client with the text specified by the given file name and path,<BR>
	 * relative to the datapack root.
	 * <BR>
	 * <BR>
	 * Added by Tempy
	 * 
	 * @param player
	 *            The L2PcInstance that talk with the L2NpcInstance
	 * @param filename
	 *            The filename that contains the text to send
	 */
	public void showChatWindow(L2PcInstance player, String filename)
	{
		// Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		final int curBuffs = player.getBuffCount();
		html.replace("%lol%", String.valueOf(curBuffs));
		final int maxBuffs = player.getMaxBuffCount();
		html.replace("%nig%", String.valueOf(maxBuffs));
		player.sendPacket(html);
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Return the Exp Reward of this L2NpcInstance contained in the L2NpcTemplate (modified by RATE_XP).<BR>
	 * <BR>
	 */
	public long getExpReward()
	{
		return (long) (getTemplate().rewardExp * Config.RATE_XP);
	}
	
	/**
	 * Return the SP Reward of this L2NpcInstance contained in the L2NpcTemplate (modified by RATE_SP).<BR>
	 * <BR>
	 */
	public int getSpReward()
	{
		return (int) (getTemplate().rewardSp * Config.RATE_SP);
	}
	
	/**
	 * Kill the L2NpcInstance (the corpse disappeared after 7 seconds).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Create a DecayTask to remove the corpse of the L2NpcInstance after 7 seconds</li>
	 * <li>Set target to null and cancel Attack or Cast</li>
	 * <li>Stop movement</li>
	 * <li>Stop HP/MP/CP Regeneration task</li>
	 * <li>Stop all active skills effects in progress on the L2Character</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform</li>
	 * <li>Notify L2Character AI</li><BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2Attackable</li><BR>
	 * <BR>
	 * 
	 * @param killer
	 *            The L2Character who killed it
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		// normally this wouldn't really be needed, but for those few exceptions,
		// we do need to reset the weapons back to the initial templated weapon.
		_currentLHandId = getTemplate().lhand;
		_currentRHandId = getTemplate().rhand;
		/*
		 * _currentCollisionHeight = getCollisionHeight();
		 * _currentCollisionRadius = getCollisionRadius();
		 */
		DecayTaskManager.getInstance().addDecayTask(this);
		return true;
	}
	
	/**
	 * Set the spawn of the L2NpcInstance.<BR>
	 * <BR>
	 * 
	 * @param spawn
	 *            The L2Spawn that manage the L2NpcInstance
	 */
	public void setSpawn(L2Spawn spawn)
	{
		_spawn = spawn;
	}
	
	@Override
	public void onSpawn()
	{
		if (_inventory != null)
			_inventory.reset();
		super.onSpawn();
	}
	
	/**
	 * Remove the L2NpcInstance from the world and update its spawn object (for a complete removal use the deleteMe method).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2NpcInstance from the world when the decay task is launched</li>
	 * <li>Decrease its spawn counter</li>
	 * <li>Manage Siege task (killFlag, killCT)</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR>
	 * <BR>
	 */
	@Override
	public void onDecay()
	{
		if (isDecayed())
			return;
		setDecayed(true);
		// Manage Life Control Tower
		if (this instanceof L2ControlTowerInstance)
			((L2ControlTowerInstance) this).onDeath();
		// Remove the L2NpcInstance from the world when the decay task is launched
		super.onDecay();
		// Decrease its spawn counter
		if (_spawn != null)
			_spawn.decreaseCount(this);
	}
	
	/**
	 * Remove PROPERLY the L2NpcInstance from the world.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2NpcInstance from the world and update its spawn object</li>
	 * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2NpcInstance then cancel Attack or Cast and notify AI</li>
	 * <li>Remove L2Object object from _allObjects of L2World</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR>
	 * <BR>
	 */
	public void deleteMe()
	{
		L2WorldRegion oldRegion = getWorldRegion();
		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed decayMe().", e);
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
		if (oldRegion != null)
			oldRegion.removeFromZones(this);
		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed removing cleaning knownlist.", e);
		}
		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
	}
	
	/**
	 * Return the L2Spawn object that manage this L2NpcInstance.<BR>
	 * <BR>
	 */
	public L2Spawn getSpawn()
	{
		return _spawn;
	}
	
	@Override
	public String toString()
	{
		return getTemplate().name;
	}
	
	public boolean isDecayed()
	{
		return _isDecayed;
	}
	
	public void setDecayed(boolean decayed)
	{
		_isDecayed = decayed;
	}
	
	public void endDecayTask()
	{
		if (!isDecayed())
		{
			DecayTaskManager.getInstance().cancelDecayTask(this);
			onDecay();
		}
	}
	
	public boolean isMob() // rather delete this check
	{
		return false; // This means we use MAX_NPC_ANIMATION instead of MAX_MONSTER_ANIMATION
	}
	
	// Two functions to change the appearance of the equipped weapons on the NPC
	// This is only useful for a few NPCs and is most likely going to be called from AI
	public void setLHandId(int newWeaponId)
	{
		_currentLHandId = newWeaponId;
		updateAbnormalEffect();
	}
	
	public void setRHandId(int newWeaponId)
	{
		_currentRHandId = newWeaponId;
		updateAbnormalEffect();
	}
	
	public void setLRHandId(int newLWeaponId, int newRWeaponId)
	{
		_currentRHandId = newRWeaponId;
		_currentLHandId = newLWeaponId;
		updateAbnormalEffect();
	}
	
	public void setCollisionHeight(float height)
	{
		_currentCollisionHeight = height;
	}
	
	public void setCollisionRadius(float f)
	{
		_currentCollisionRadius = f;
	}
	
	public float getCurrCollisionHeight()
	{
		return _currentCollisionHeight;
	}
	
	public float getCurrCollisionRadius()
	{
		return _currentCollisionRadius;
	}
	
	public boolean isUsingShot(boolean d)
	{
		return isUsingShot();
	}
	
	public boolean isUsingShot()
	{
		if (getTemplate().ssRate == 0)
			return false;
		if (getTemplate().ssRate >= Rnd.get(100))
			return true;
		return false;
	}
	
	@Override
	public NpcInventory getInventory()
	{
		return _inventory;
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (Config.CHECK_KNOWN)
			activeChar.sendMessage("Added NPC: " + getName());
		if (getRunSpeed() == 0)
			activeChar.sendPacket(new ServerObjectInfo(this, activeChar));
		else
			activeChar.sendPacket(new AbstractNpcInfo.NpcInfo(this, activeChar));
	}
	
	public int getElite()
	{
		return getTemplate().elite;
	}
	
	public int getRare()
	{
		return getTemplate().rare;
	}
	
	public void setIsTempSpawn()
	{
		_tempSpawn = true;
	}

	
	public L2Npc scheduleDespawn(final long delay)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(this.new DespawnTask(), delay);
		return this;
	}
	
	protected class DespawnTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!isDecayed())
				deleteMe();
		}
	}
	
	public void broadcastNpcInfo()
	{
		Collection<L2PcInstance> plrs = this.getKnownList().getKnownPlayers().values();
		{
			for (L2PcInstance player : plrs)
			{
				((L2Npc) this).sendInfo(player);
			}
		}
	}
}
