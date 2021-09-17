package net.sf.l2j.gameserver.model.actor;

import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static net.sf.l2j.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import ghosts.model.Ghost;
import javolution.util.FastList;
import javolution.util.FastMap;
import luna.custom.handler.AchievementBp;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2AttackableAI;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.FakePcsTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.handler.skillhandlers.Blow;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.ChanceSkillList;
import net.sf.l2j.gameserver.model.CharEffectList;
import net.sf.l2j.gameserver.model.FusionSkill;
import net.sf.l2j.gameserver.model.IChanceSkillTrigger;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2AirShipInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2BoatInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DecoyInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2EventMapGuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MinionInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcWalkerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.SkillDat;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RebirthMasterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RiftInvaderInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TrapInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.CharKnownList;
import net.sf.l2j.gameserver.model.actor.position.CharPosition;
import net.sf.l2j.gameserver.model.actor.stat.CharStat;
import net.sf.l2j.gameserver.model.actor.status.CharStatus;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.Quest.QuestEventType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.Attack;
import net.sf.l2j.gameserver.network.serverpackets.AutoAttackStart;
import net.sf.l2j.gameserver.network.serverpackets.ChangeMoveType;
import net.sf.l2j.gameserver.network.serverpackets.ChangeWaitType;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillCanceld;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.MoveToLocation;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.Revive;
import net.sf.l2j.gameserver.network.serverpackets.ServerObjectInfo;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.TeleportToLocation;
import net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc;
import net.sf.l2j.gameserver.pathfinding.PathFinding;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Calculator;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.effects.EffectChanceSkillTrigger;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillAgathion;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillLearnSkill;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillMount;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSummon;
import net.sf.l2j.gameserver.templates.chars.L2CharTemplate;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Point3D;
import net.sf.l2j.util.Rnd;

/**
 * Mother class of all character objects of the world (PC, NPC...)<BR>
 * <BR>
 * L2Character :<BR>
 * <BR>
 * <li>L2CastleGuardInstance</li>
 * <li>L2DoorInstance</li>
 * <li>L2NpcInstance</li>
 * <li>L2PlayableInstance</li><BR>
 * <BR>
 * <B><U> Concept of L2CharTemplate</U> :</B><BR>
 * <BR>
 * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...).
 * All of those properties are stored in a different template for each type of L2Character.
 * Each template is loaded once in the server cache memory (reduce memory use).
 * When a new instance of L2Character is spawned, server just create a link between the instance and the template.
 * This link is stored in <B>_template</B><BR>
 * <BR>
 *
 * @version $Revision: 1.53.2.45.2.34 $ $Date: 2005/04/11 10:06:08 $
 */
public abstract class L2Character extends L2Object
{
	public static final Logger				_log						= Logger.getLogger(L2Character.class.getName());
	// =========================================================
	// Data Field
	private List<L2Character>				_attackByList;
	private volatile boolean				_isCastingNow				= false;
	private volatile boolean				_isCastingSimultaneouslyNow	= false;
	private L2Skill							_lastSkillCast;
	private L2Skill							_lastSimultaneousSkillCast;
	private boolean							_isAfraid					= false;										// Flee in a random direction
	private boolean							_isConfused					= false;										// Attack anyone randomly
	private boolean							_isMuted					= false;										// Cannot use magic
	private boolean							_isPhysicalMuted			= false;										// Cannot use physical skills
	private boolean							_isPhysicalAttackMuted		= false;										// Cannot use attack
	private boolean							_isDead						= false;
	private boolean							_isImmobilized				= false;
	/* private boolean _isOverloaded = false; // the char is carrying too much */
	private boolean							_isSubbing					= false;
	private boolean							_isParalyzed				= false;
	private boolean							_isDisarmed					= false;
	private boolean							_doubleShotted				= false;
	private boolean							_isPendingRevive			= false;
	private boolean							_isRooted					= false;										// Cannot move until root timed out
	private boolean							_isRunning					= false;
	private boolean							_isNoRndWalk				= false;										// Is no random walk
	private boolean							_isImmobileUntilAttacked	= false;										// Is in immobile until attacked.
	private boolean							_isSleeping					= false;										// Cannot move/attack until sleep timed out or monster is attacked
	private boolean							_isStunned					= false;										// Cannot move/attack until stun timed out
	private boolean							_isBetrayed					= false;										// Betrayed by own summon
	private boolean							_isBluffed					= false;										// bluffed
	protected boolean						_showSummonAnimation		= false;
	protected boolean						_isTeleporting				= false;
	protected boolean						_isInvul					= false;
	private boolean							_block_buffs				= false;
	private int								_lastHealAmount				= 0;
	private CharStat						_stat;
	private CharStatus						_status;
	private L2CharTemplate					_template;																	// The link on the L2CharTemplate object containing generic and static properties of this L2Character type (ex : Max HP, Speed...)
	private String							_title;
	private String							_aiClass					= "default";
	private double							_hpUpdateIncCheck			= .0;
	private double							_hpUpdateDecCheck			= .0;
	private double							_hpUpdateInterval			= .0;
	private boolean							_champion					= false;
	private boolean							_invisible					= false;
	/** Table of Calculators containing all used calculator */
	private Calculator[]					_calculators;
	/** FastMap(Integer, L2Skill) containing all skills of the L2Character */
	private final Map<Integer, L2Skill>		_skills;
	protected final Map<Integer, Integer[]>	_retrySkills				= new FastMap<Integer, Integer[]>().shared();
	/** FastMap containing the active chance skills on this character */
	protected ChanceSkillList				_chanceSkills;
	/** Current force buff this caster is casting to a target */
	protected FusionSkill					_fusionSkill;
	/** Zone system */
	public static final byte				ZONE_PVP					= 0;
	public static final byte				ZONE_PEACE					= 1;
	public static final byte				ZONE_SIEGE					= 2;
	public static final byte				ZONE_MOTHERTREE				= 3;
	public static final byte				ZONE_CLANHALL				= 4;
	public static final byte				ZONE_LANDING				= 5;
	public static final byte				ZONE_NOLANDING				= 6;
	public static final byte				ZONE_WATER					= 7;
	public static final byte				ZONE_JAIL					= 8;
	public static final byte				ZONE_MONSTERTRACK			= 9;
	public static final byte				ZONE_CASTLE					= 10;
	public static final byte				ZONE_SWAMP					= 11;
	public static final byte				ZONE_NOSUMMONFRIEND			= 12;
	public static final byte				ZONE_FORT					= 13;
	public static final byte				ZONE_NOSTORE				= 14;
	public static final byte				ZONE_TOWN					= 15;
	public static final byte				ZONE_SCRIPT					= 16;
	public static final byte				ZONE_NOHQ					= 17;
	public static final byte				ZONE_GENERAL				= 18;
	public static final byte				ZONE_CHAOTIC				= 19;
	public static final byte				ZONE_HQ						= 20;
	public static final byte				ZONE_FARM					= 21;
	public static final byte				ZONE_EVENT					= 22;
	public static final byte				ZONE_NIGHT					= 23;
	public static final byte				ZONE_RAID					= 24;
	public static final byte				ZONE_PRIMEVAL				= 25;
	private static final List<Integer>		IgnoreSkillIdsForMessage	= Arrays.asList(1312, 2099, 2100);
	public int								_demonicMovement			= 0;
	public int								_distanceToTargetCurrSkill	= 0;
	private final byte[]					_zones						= new byte[26];
	protected byte							_zoneValidateCounter		= 4;
	private boolean							_isRaid						= false;
	private boolean							_isFlying;
	private int								knockedbackTimer			= 0;
	private List<L2Character>				_raidDamageAttackers;
	
	public int getKnockedbackTimer()
	{
		return knockedbackTimer;
	}
	
	public void setKnockedbackTimer(int ticks)
	{
		knockedbackTimer = ticks;
	}
	
	/**
	 * Returns character inventory, default null, overridden in L2Playable types and in L2NPcInstance
	 */
	public Inventory getInventory()
	{
		return null;
	}
	
	public boolean destroyItemByItemId(String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
		return true;
	}
	
	public boolean destroyItem(String process, int objectId, long count, L2Object reference, boolean sendMessage)
	{
		return true;
	}
	
	/**
	 * @param zone
	 * @return
	 */
	public final boolean isInsideZone(final byte zone)
	{
		switch (zone)
		{
			case ZONE_PVP:
				if (InstanceManager.getInstance().getInstance(getInstanceId()).isPvPInstance())
					return true;
				return _zones[ZONE_PVP] > 0 && _zones[ZONE_PEACE] == 0;
			case ZONE_PEACE:
			{
				if (this instanceof L2Playable)
				{
					final L2PcInstance player = getActingPlayer();
					if (player == null)
						return true;
					if (player.getInEventPeaceZone())
						return true;
				}
				if (InstanceManager.getInstance().getInstance(getInstanceId()).isPvPInstance())
					return false;
			}
		}
		return _zones[zone] > 0;
	}
	
	/**
	 * @param zone
	 * @param state
	 */
	public final void setInsideZone(final byte zone, final boolean state)
	{
		if (state)
			_zones[zone]++;
		else
		{
			_zones[zone]--;
			if (_zones[zone] < 0)
				_zones[zone] = 0;
			if (zone == L2Character.ZONE_PVP && this instanceof L2PcInstance)
			{
				if (!isGM())
				{
					if (((L2PcInstance) this).getPvpFlag() == 0)
						((L2PcInstance) this).startPvPFlag();
				}
			}
		}
	}
	
	/**
	 * This will return true if the player is transformed,<br>
	 * but if the player is not transformed it will return false.
	 * 
	 * @return transformation status
	 */
	public boolean isTransformed()
	{
		return false;
	}
	
	/**
	 * This will untransform a player if they are an instance of L2Pcinstance
	 * and if they are transformed.
	 * 
	 * @return untransform
	 */
	public void untransform()
	{
		// Just a place holder
	}
	
	/**
	 * This will return true if the player is GM,<br>
	 * but if the player is not GM it will return false.
	 * 
	 * @return GM status
	 */
	public boolean isGM()
	{
		return false;
	}
	
	public boolean isGMReally()
	{
		return false;
	}
	
	// =========================================================
	// Constructor
	/**
	 * Constructor of L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...).
	 * All of those properties are stored in a different template for each type of L2Character.
	 * Each template is loaded once in the server cache memory (reduce memory use).
	 * When a new instance of L2Character is spawned, server just create a link between the instance and the template
	 * This link is stored in <B>_template</B><BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the _template of the L2Character</li>
	 * <li>Set _overloaded to false (the charcater can take more items)</li><BR>
	 * <BR>
	 * <li>If L2Character is a L2NPCInstance, copy skills from template to object</li>
	 * <li>If L2Character is a L2NPCInstance, link _calculators to NPC_STD_CALCULATOR</li><BR>
	 * <BR>
	 * <li>If L2Character is NOT a L2NPCInstance, create an empty _skills slot</li>
	 * <li>If L2Character is a L2PcInstance or L2Summon, copy basic Calculator set to object</li><BR>
	 * <BR>
	 *
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param template
	 *            The L2CharTemplate to apply to the object
	 */
	public L2Character(int objectId, L2CharTemplate template)
	{
		super(objectId);
		initCharStat();
		initCharStatus();
		// Set its template to the new L2Character
		_template = template;
		if (template != null && this instanceof L2Npc)
		{
			// Copy the Standard Calcultors of the L2NPCInstance in _calculators
			if (this instanceof L2DoorInstance)
				_calculators = Formulas.getStdDoorCalculators();
			else
				_calculators = NPC_STD_CALCULATOR;
			// Copy the skills of the L2NPCInstance from its template to the L2Character Instance
			// The skills list can be affected by spell effects so it's necessary to make a copy
			// to avoid that a spell affecting a L2NPCInstance, affects others L2NPCInstance of the same type too.
			_skills = ((L2NpcTemplate) template).getSkills();
			if (_skills != null)
			{
				for (Map.Entry<Integer, L2Skill> skill : _skills.entrySet())
					addStatFuncs(skill.getValue().getStatFuncs(null, this));
			}
		}
		else
		{
			// If L2Character is a L2PcInstance or a L2Summon, create the basic calculator set
			_calculators = new Calculator[Stats.NUM_STATS];
			if (this instanceof L2Summon)
			{
				// Copy the skills of the L2Summon from its template to the L2Character Instance
				// The skills list can be affected by spell effects so it's necessary to make a copy
				// to avoid that a spell affecting a L2Summon, affects others L2Summon of the same type too.
				_skills = ((L2NpcTemplate) template).getSkills();
				if (_skills != null)
				{
					for (Map.Entry<Integer, L2Skill> skill : _skills.entrySet())
						addStatFuncs(skill.getValue().getStatFuncs(null, this));
				}
			}
			else
			{
				// Initialize the FastMap _skills to null
				_skills = new FastMap<Integer, L2Skill>().shared();
			}
			Formulas.addFuncsToNewCharacter(this);
		}
		setIsInvul(true);
	}
	
	protected void initCharStatusUpdateValues()
	{
		_hpUpdateInterval = getMaxHp() / 352.0; // MAX_HP div MAX_HP_BAR_PX
		_hpUpdateIncCheck = getMaxHp();
		_hpUpdateDecCheck = getMaxHp() - _hpUpdateInterval;
	}
	
	// =========================================================
	// Event - Public
	/**
	 * Remove the L2Character from the world when the decay task is launched.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR>
	 * <BR>
	 */
	public void onDecay()
	{
		L2WorldRegion reg = getWorldRegion();
		decayMe();
		if (reg != null)
			reg.removeFromZones(this);
		_effects.clear();
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		revalidateZone(true);
	}
	
	public void onTeleported()
	{
		if (!isTeleporting())
			return;
		try
		{
			if (this instanceof L2Summon)
				((L2Summon) this).getOwner().sendPacket(new TeleportToLocation(this, getPosition().getX(), getPosition().getY(), getPosition().getZ(), getPosition().getHeading()));
			spawnMeTeleport(getX(), getY(), getZ());
			setIsTeleporting(false);
			if (_isPendingRevive)
				doRevive();
			if (this instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) this;
				if ((Config.PLAYER_SPAWN_PROTECTION > 0) && !player.isInOlympiadMode())
				{
					if (player.isForceNoSpawnProtection())
						player.setForceNoSpawnProtection(false);
					else
						player.setProtection(true);
				}
				// Trained beast is after teleport lost
				if (player.getTrainedBeast() != null)
				{
					player.getTrainedBeast().decayMe();
					player.setTrainedBeast(null);
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
				TvTEvent.onTeleported((L2PcInstance) this);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	// =========================================================
	// Method - Public
	/**
	 * Add L2Character instance that is attacking to the attacker list.<BR>
	 * <BR>
	 * 
	 * @param player
	 *            The L2Character that attacks this one
	 */
	public void addAttackerToAttackByList(L2Character player)
	{
		if (player == null || player == this || getAttackByList() == null || getAttackByList().contains(player))
			return;
		getAttackByList().add(player);
	}
	
	public void addRaidAttackerToList(L2Character player)
	{
		if (player == null || player == this || getRaidAttackerByList() == null || getRaidAttackerByList().contains(player))
			return;
		getRaidAttackerByList().add(player);
	}
	
	/**
	 * Send a packet to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet<BR>
	 * <BR>
	 */
	public void broadcastPacket(L2GameServerPacket mov)
	{
		boolean sendPacket = true;
		if (!getKnownList().getKnownPlayers().values().isEmpty())
		{
			for (L2PcInstance player : getKnownList().getKnownPlayers().values())
			{
				if (player == null || player == this)
					continue;
				sendPacket = true;
				if (this instanceof L2PcInstance)
				{
					if (mov instanceof MagicSkillUse || mov instanceof MagicSkillLaunched)
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
	}
	
	public void broadcastPacket(L2GameServerPacket mov, L2PcInstance receiver)
	{
		if (receiver == null || receiver == this)
			return;
		receiver.sendPacket(mov);
	}
	
	/**
	 * Send a packet to the L2Character AND to all L2PcInstance in the radius (max knownlist radius) from the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to send Server->Client Packet<BR>
	 * <BR>
	 */
	public void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist)
	{
		if (!getKnownList().getKnownPlayers().values().isEmpty())
		{
			for (L2PcInstance player : getKnownList().getKnownPlayers().values())
			{
				boolean sendPacket = true;
				if (player != null && isInsideRadius(player, radiusInKnownlist, false, false))
				{
					sendPacket = true;
					if (player.getVarB("hideSkillsAnim"))
					{
						sendPacket = false;
					}
					if (sendPacket)
						player.sendPacket(mov);
				}
				else
					continue;
			}
		}
	}
	
	final public int getShldRate(L2Character attacker, L2Skill skill)
	{
		return (int) (calcStat(Stats.SHIELD_RATE, 0, attacker, skill) * Formulas.DEXbonus[Math.max(0, getDEX() - 6)]);
	}
	
	/**
	 * Returns true if hp update should be done, false if not
	 * 
	 * @return boolean
	 */
	protected boolean needHpUpdate(int barPixels)
	{
		double currentHp = getCurrentHp();
		if (currentHp <= 1.0 || getMaxHp() < barPixels)
			return true;
		if (currentHp <= _hpUpdateDecCheck || currentHp >= _hpUpdateIncCheck)
		{
			if (currentHp == getMaxHp())
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Create the Server->Client packet StatusUpdate with current HP and MP</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all
	 * L2Character called _statusListener that must be informed of HP/MP updates of this L2Character</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND CP information</B></FONT><BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance : Send current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other L2PcInstance of the Party</li><BR>
	 * <BR>
	 */
	public void broadcastStatusUpdate()
	{
		if (getStatus().getStatusListener().isEmpty())
			return;
		if (!needHpUpdate(352))
			return;
		if (Config.DEBUG)
			_log.fine("Broadcast Status Update for " + getObjectId() + "(" + getName() + "). HP: " + getCurrentHp());
		// Create the Server->Client packet StatusUpdate with current HP and MP
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
		// Go through the StatusListener
		// Send the Server->Client packet StatusUpdate with current HP and MP
		synchronized (getStatus().getStatusListener())
		{
			for (L2Character temp : getStatus().getStatusListener())
			{
				try
				{
					temp.sendPacket(su);
				}
				catch (NullPointerException e)
				{}
			}
		}
	}
	
	/**
	 * Not Implemented.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	public void sendPacket(L2GameServerPacket mov)
	{
		// default implementation
	}
	
	/**
	 * Not Implemented.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	public void sendMessage(String text)
	{
		// default implementation
	}
	
	public void sendServerMessage(String text)
	{
		// default implementation
	}
	
	public Future<?> _immobileAutoAttackTask = null;
	
	public void startImmobileautoAttackTask(int angle, int heading)
	{
		stopImmobileautoAttackTask();
		_immobileAutoAttackTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActionTask(this, angle, heading), 500);
	}
	
	public void stopImmobileautoAttackTask()
	{
		if (_immobileAutoAttackTask != null)
		{
			_immobileAutoAttackTask.cancel(true);
		}
		_immobileAutoAttackTask = null;
	}
	
	public static class ActionTask implements Runnable
	{
		private final L2Character	_char;
		private int					_angle;
		private int					_originalheading;
		
		public ActionTask(L2Character activeChar, int angle, int heading)
		{
			_char = activeChar;
			_angle = angle;
			_originalheading = heading;
		}
		
		public void run()
		{
			final L2Weapon wep = _char.getActiveWeaponItem();
			final int attackReuse = _char.calculateReuseTime(null, wep);
			try
			{
				Collection<L2Character> list = _char.getKnownList().getKnownCharactersInRadius(_char.getPhysicalAttackRange());
				if (list != null && list.size() > 0)
				{
					FastList<L2Character> attackables = new FastList<L2Character>();
					for (L2Character target : list)
					{
						if (target == null || target == _char || (_char instanceof L2Playable && target instanceof L2Playable && target.getActingPlayer() == _char.getActingPlayer()) || target.isAlikeDead())
							continue;
						if (!_char.isFacing(_originalheading, target, _angle))
							continue;
						if (target.isInvisible() && (target.isGMReally() || !_char.canSeeInvisiblePeople()))
							continue;
						if (!GeoData.getInstance().canSeeTarget(_char, target))
							continue;
						if (target instanceof L2Playable && _char instanceof L2Playable)
						{
							if (!_char.getActingPlayer().canAttack(target.getActingPlayer(), false))
								continue;
						}
						else
						{
							if (target instanceof L2Attackable)
							{
								if (!_char.canAttackDueToSoloMob(target))
									continue;
							}
						}
						if (!target.isAutoAttackable(_char))
							continue;
						attackables.add(target);
					}
					if (attackables.size() > 0)
					{
						_char.doAttack(attackables.get(Rnd.get(attackables.size())));
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				_char._immobileAutoAttackTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActionTask(_char, _angle, _originalheading), _char.calculateTimeBetweenAttacks(null, wep) + attackReuse);
			}
		}
	}
	
	/**
	 * Teleport a L2Character and its pet if necessary.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Stop the movement of the L2Character</li>
	 * <li>Set the x,y,z position of the L2Object and if necessary modify its _worldRegion</li>
	 * <li>Send a Server->Client packet TeleportToLocationt to the L2Character AND to all L2PcInstance in its _KnownPlayers</li>
	 * <li>Modify the position of the pet if necessary</li><BR>
	 * <BR>
	 */
	public void teleToLocation(int x, int y, int z, int heading, boolean allowRandomOffset)
	{
		// Stop movement
		/* stopMove(null, false); */
		_move = null;
		abortAttack();
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		abortCast();
		setTarget(null);
		setIsTeleporting(true);
		if (Config.RESPAWN_RANDOM_ENABLED && allowRandomOffset)
		{
			x += Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET, Config.RESPAWN_RANDOM_MAX_OFFSET);
			y += Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET, Config.RESPAWN_RANDOM_MAX_OFFSET);
		}
		z += 5;
		if (Config.DEBUG)
			_log.fine("Teleporting to: " + x + ", " + y + ", " + z);
		// remove the object from its old location
		decayMeTeleport(x, y, z);
		// Send a Server->Client packet TeleportToLocationt to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
		broadcastPacket(new TeleportToLocation(this, x, y, z, heading));
		// Set the x,y,z position of the L2Object and if necessary modify its _worldRegion
		getPosition().setXYZTele(x, y, z);
		// temporary fix for heading on teleports
		if (heading != 0)
			getPosition().setHeading(heading);
		if (this instanceof Ghost)
		{
			onTeleported();
			revalidateZone(true);
		}
		if (!(this instanceof L2PcInstance) || (((L2PcInstance) this).getClient() != null && ((L2PcInstance) this).getClient().isDetached()))
		{
			onTeleported();
			revalidateZone(true);
		}
	}
	
	public void teleToLocation(int x, int y, int z)
	{
		teleToLocation(x, y, z, getHeading(), false);
	}
	
	public void teleToLocation(int x, int y, int z, boolean allowRandomOffset)
	{
		teleToLocation(x, y, z, getHeading(), allowRandomOffset);
	}
	
	public void teleToLocation(Location loc, boolean allowRandomOffset)
	{
		int x = loc.getX();
		int y = loc.getY();
		int z = loc.getZ();
		if (this instanceof L2PcInstance && DimensionalRiftManager.getInstance().checkIfInRiftZone(getX(), getY(), getZ(), true)) // true -> ignore waiting room :)
		{
			L2PcInstance player = (L2PcInstance) this;
			player.sendMessage("You have been sent to the waiting room.");
			if (player.isInParty() && player.getParty().isInDimensionalRift())
			{
				player.getParty().getDimensionalRift().usedTeleport(player);
			}
			int[] newCoords = DimensionalRiftManager.getInstance().getRoom((byte) 0, (byte) 0).getTeleportCoords();
			x = newCoords[0];
			y = newCoords[1];
			z = newCoords[2];
		}
		L2PcInstance player = (L2PcInstance) this;
		teleToLocation(x, y, z, getHeading(), allowRandomOffset);
		player.broadcastUserInfo();
	}
	
	public void teleToLocation(TeleportWhereType teleportWhere)
	{
		teleToLocation(MapRegionTable.getInstance().getTeleToLocation(this, teleportWhere), true);
	}
	
	// =========================================================
	// Method - Private
	/**
	 * Launch a physical attack against a target (Simple, Bow, Pole or Dual).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the active weapon (always equiped in the right hand)</li><BR>
	 * <BR>
	 * <li>If weapon is a bow, check for arrows, MP and bow re-use delay (if necessary, equip the L2PcInstance with arrows in left hand)</li>
	 * <li>If weapon is a bow, consume MP and set the new period of bow non re-use</li><BR>
	 * <BR>
	 * <li>Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)</li>
	 * <li>Select the type of attack to start (Simple, Bow, Pole or Dual) and verify if SoulShot are charged then start calculation</li>
	 * <li>If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character</li>
	 * <li>Notify AI with EVT_READY_TO_ACT</li><BR>
	 * <BR>
	 *
	 * @param target
	 *            The L2Character targeted
	 */
	protected void doAttack(L2Character target)
	{
		if (target == null || isAlikeDead() || (this instanceof L2NpcInstance && target.isAlikeDead()) || !getKnownList().knowsObject(target))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (target instanceof L2PcInstance && target.getActingPlayer().eventSitForced)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			sendMessage("You may not attack sitting players during events.");
			return;
		}
		if (this instanceof L2PcInstance)
		{
			if (target.isDead())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			L2PcInstance actor = (L2PcInstance) this;
			// Players riding wyvern or with special (flying) transformations can do melee attacks, only with skills
			if ((actor.isMounted() && actor.getMountNpcId() == 12621) || (actor.isTransformed() && !actor.getTransformation().canDoMeleeAttack()))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		if (isAttackingDisabled() && !(this instanceof L2Decoy))
		{
			if (isPhysicalAttackMuted())
				sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (this instanceof L2PcInstance)
		{
			if (((L2PcInstance) this).inObserverMode())
			{
				sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (target instanceof L2PcInstance)
			{
				if (((L2PcInstance) target).isCursedWeaponEquipped() && ((L2PcInstance) this).getLevel() <= 20)
				{
					((L2PcInstance) this).sendMessage("Can't attack a cursed player when under level 21.");
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (((L2PcInstance) this).isCursedWeaponEquipped() && ((L2PcInstance) target).getLevel() <= 20)
				{
					((L2PcInstance) this).sendMessage("Can't attack a newbie player using a cursed weapon.");
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			/*
			 * else if (!canAttackDueToSoloMob(target))
			 * {
			 * getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			 * sendMessage("This mob already belongs to another player");
			 * return;
			 * }
			 */
			// Checking if target has moved to peace zone
			if (target.isInsidePeaceZone((L2PcInstance) this))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			// TODO: unhardcode this to support boolean if with that weapon u can attack or not (for ex transform weapons)
			if (((L2PcInstance) this).getActiveWeaponItem() != null && ((L2PcInstance) this).getActiveWeaponItem().getItemId() == 9819)
			{
				sendPacket(new SystemMessage(SystemMessageId.THAT_WEAPON_CANT_ATTACK));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		else if (isInsidePeaceZone(this, target))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (this instanceof L2Playable && target instanceof L2Attackable)
		{
			if (!canAttackDueToSoloMob(target))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendMessage("This mob already belongs to another player");
				return;
			}
		}
		// GeoData Los Check here (or dz > 1000)
		if (!GeoData.getInstance().canSeeTarget(this, target))
		{
			sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			/* sendPacket(ActionFailed.STATIC_PACKET); */
			return;
		}
		int delay = 0; // in miliseconds
		boolean rangedWep = isUsingRangedWep();
		if (rangedWep && !(this instanceof L2Decoy))
		{
			if (this instanceof L2PcInstance)
			{
				final int gameTicks = GameTimeController.getGameTicks();
				// Verify if the bow can be use
				if (_disableBowAttackEndTime > gameTicks)
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), (_disableBowAttackEndTime - gameTicks) * GameTimeController.MILLIS_IN_TICK);
					return;
				}
				if (getActiveWeaponItem().getItemId() == 81104) // phantom rain
					delay = (int) Config.CHANCE_SKILL_BOW_DELAY;
				else
					delay = (int) Config.CHANCE_SKILL_BOW_DELAY / 2;
			}
			else if (this instanceof L2NpcInstance)
			{
				if (_disableBowAttackEndTime > GameTimeController.getGameTicks())
					return;
			}
		}
		_move = null;
		// Reduce the current CP if TIREDNESS configuration is activated
		if (Config.ALT_GAME_TIREDNESS)
			setCurrentCp(getCurrentCp() - 10);
		// Verify if soulshots are charged.
		boolean wasSSCharged = false;
		if (this instanceof L2PcInstance)
		{
			if (!((L2PcInstance) this).isCursedWeaponEquipped())
				wasSSCharged = true;
		}
		else
		{
			if (this instanceof L2Npc)
				wasSSCharged = ((L2Npc) this).isUsingShot();
			else
				wasSSCharged = true;
		}
		// Get the active weapon item corresponding to the active weapon instance (always equiped in the right hand)
		final L2Weapon weaponItem = getActiveWeaponItem();
		// Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)
		final int timeAtk = calculateTimeBetweenAttacks(target, weaponItem);
		// the hit is calculated to happen halfway to the animation - might need further tuning e.g. in bow case
		final int timeToHit = timeAtk / 2;
		setAttackEndTime(GameTimeController.getGameTicks() + ((timeAtk + delay) / GameTimeController.MILLIS_IN_TICK) - 1);
		final int reuse = calculateReuseTime(target, weaponItem);
		int ssGrade = getSoulshotGrade();
		final int ignoreAutoTargetChance = (int) calcStat(Stats.IGNORE_AUTOTARGET_ATTACK, 0, null, null);
		// Create a Server->Client packet Attack
		Attack attack = new Attack(this, wasSSCharged, ssGrade, ignoreAutoTargetChance);
		// Set the Attacking Body part to CHEST
		setIsAttacking();
		// Make sure that char is facing selected target
		// also works: setHeading(Util.convertDegreeToClientHeading(Util.calculateAngleFrom(this, target)));
		setHeading(Util.calculateHeadingFrom(this, target));
		boolean hitted = false;
		final boolean cursed = getActingPlayer() != null && getActingPlayer().isCursedWeaponEquipped();
		if (weaponItem == null || (!cursed && isTransformed()))
		{
			if (this instanceof L2Attackable)
			{
				if (calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null) >= 2)
					hitted = doAOEHitbySingleWeapon(attack, target, timeToHit);
				else
					hitted = doAttackHitSimple(attack, target, timeToHit);
			}
			else
				hitted = doAttackHitSimple(attack, target, timeToHit);
		}
		else if (!cursed && weaponItem.getItemType() == L2WeaponType.BOW)
			hitted = doAttackHitByBow(attack, target, timeAtk, reuse, false);
		else if (!cursed && weaponItem.getItemType() == L2WeaponType.CROSSBOW)
			hitted = doAttackHitByBow(attack, target, timeAtk, reuse, true);
		else if (calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null) >= 2) // hit hits 2 or more targets
		{
			if (isUsingDualWeapon())
				hitted = doAOEHitbyDualWeapon(attack, target, timeToHit);
			else
				hitted = doAOEHitbySingleWeapon(attack, target, timeToHit);
		}
		else if (isUsingDualWeapon())
			hitted = doAttackHitByDual(attack, target, timeToHit);
		else
			hitted = doAttackHitSimple(attack, target, timeToHit);
		final boolean doubleShotted = _doubleShotted;
		// If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack
		// to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
		if (attack.hasHits())
		{
			if (_doubleShotted)
				_doubleShotted = false;
			else
				broadcastPacket(attack);
		}
		// Flag the attacker if it's a L2PcInstance outside a PvP area
		final L2PcInstance player = getActingPlayer();
		if (player != null)
		{
			if (player.getPet() != target)
				player.updatePvPStatus(target);
		}
		if (hitted)
		{
			if (player != null && player.isHero() && !player._tempHero && !player._fakeHero)
			{
				if (weaponItem != null && weaponItem.isHeroItem())
				{
					if (target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquipped())
						target.setCurrentCp(0);
				}
			}
			if (wasSSCharged)
			{
				int soulshotId = 2154; // s grade
				switch (ssGrade)
				{
					case 5:
						break;
					case 4:
						soulshotId = 2153;
						break;
					case 3:
						soulshotId = 2152;
						break;
					case 2:
						soulshotId = 2151;
						break;
					case 1:
						soulshotId = 2150;
						break;
					default:
						soulshotId = 2039;
						break;
				}
				double shotTime = 0.4 * timeAtk;
				if (weaponItem != null && weaponItem.getItemType() == L2WeaponType.BOW)
					shotTime = 0.6 * timeAtk;
				else if (isUsingDualWeapon())
					shotTime = 0.5 * timeAtk;
				ThreadPoolManager.getInstance().scheduleEffect(new shotDisplayTask(this, soulshotId, ssGrade != 5), (int) shotTime);
			}
		}
		final int overdrive = (int) calcStat(Stats.OVERDRIVE, 0, null, null);
		if (overdrive >= 1)
		{
			if (isUsingDualWeapon() || doubleShotted)
				reduceCurrentHp(overdrive * 2, this, true, false, null, true);
			else
				reduceCurrentHp(overdrive, this, true, false, null, true);
		}
		if (doubleShotted)
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), (long) ((timeAtk) * 1.5) + delay);
		else
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), (timeAtk + delay));
	}
	
	public int getSoulshotGrade()
	{
		if (this instanceof L2Decoy)
			return ((L2Decoy) this).getActingPlayer()._ssGrade;
		if (this instanceof L2Attackable || this instanceof L2Summon)
			return ((L2NpcTemplate) getTemplate()).ssGrade;
		int ssGrade = 5;
		final L2ItemInstance wep = getActiveWeaponInstance();
		if (wep != null)
		{
			switch (wep.getItemId())
			{
				case Config.AURAFANG:
				case Config.RAYBLADE:
				case Config.WAVEBRAND:
					if (this instanceof L2PcInstance && !getActingPlayer()._hasTehForce)
						ssGrade = 5;
					else
						ssGrade = 1;
					break;
				default:
				{
					if (wep.isHeroItem())
						ssGrade = 0;
					else if ((!wep.isStandardShopItem() && ((wep.isRaidbossItem() && wep.getEnchantLevel() > 10) || wep.getEnchantLevel() > 13 || wep.getUniqueness() >= 4)) || wep.getEnchantLevel() >= wep.getItem().getClutchEnchantLevel())
					{
						switch (wep.getCrystalType())
						{
							case L2Item.CRYSTAL_S84:
								if (wep.isStandardShopItem())
								{
									if (wep.getUniqueness() >= 4 && wep.isWarForged() || wep.isDread() || wep.isMorheim() || wep.isCorrupted())
										ssGrade = 3; // red glows
									else
										ssGrade = 1; // electric lightning
								}
								else
									ssGrade = 4; // white glow
								break;
							case L2Item.CRYSTAL_S80:
								if (wep.getItem().getUniqueness() <= 2)
									ssGrade = 0;
								else if (wep.isRaidbossItem())
									ssGrade = 2; // seven arrows animation
								else if (wep.getItem().getUniqueness() <= 3)
									ssGrade = 0;
								else
									ssGrade = 4;
								break;
							case L2Item.CRYSTAL_S:
								if (wep.getItem().getUniqueness() == 0)
									ssGrade = 0;
								else if (wep.getItem().getUniqueness() <= 3)
									ssGrade = 0;
								else
									ssGrade = 4;
								break;
						}
					}
				}
			}
		}
		if (this instanceof L2PcInstance)
			getActingPlayer()._ssGrade = ssGrade;
		return ssGrade;
	}
	
	public boolean isUsingRangedWep()
	{
		final L2Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem != null && !isTransformed())
		{
			if (weaponItem.getItemType() == L2WeaponType.BOW || weaponItem.getItemType() == L2WeaponType.CROSSBOW)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean canAttackDueToSoloMob(L2Character target)
	{
		if (isGM())
			return true;
		if (target instanceof L2Attackable)
		{
			L2Attackable attackable = (L2Attackable) target;
			if (attackable.isSoloMob())
			{
				L2Character otherGuy = attackable.getAI().getAttackTarget();
				L2PcInstance otherPlayer = null;
				if (otherGuy != null)
					otherPlayer = otherGuy.getActingPlayer();
				else
					return true;
				if (otherPlayer != null && otherPlayer != getActingPlayer())
				{
					if (!otherPlayer.isAlliedWith(this) && !otherPlayer.isInPartyWith(this) && otherPlayer.getTarget() == target)
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private boolean doAttackHitByBow(final Attack attack, L2Character target, int sAtk, int reuse, boolean crossbow)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		final int originalsAtk = sAtk;
		target = findPossibleTank(target);
		int chainedShots = 0;
		final int DoubleShotsChance = (int) calcStat(Stats.CHAIN_SHOT, 0, null, null);
		if (DoubleShotsChance > 0)
		{
			if (DoubleShotsChance >= 100)
			{
				chainedShots = 1;
			}
			else
			{
				if (Rnd.nextInt(100) < DoubleShotsChance)
					chainedShots = 1;
			}
			if (chainedShots > 0)
			{
				sAtk /= 2;
				_doubleShotted = true;
				setAttackEndTime(_attackEndTime + sAtk / GameTimeController.MILLIS_IN_TICK);
			}
		}
		int maxPierced = 0;
		int pierceRating = (int) calcStat(Stats.RANGED_PIERCING, 0, null, null);
		if (pierceRating > 0)
		{
			while (pierceRating >= 100)
			{
				maxPierced++;
				pierceRating -= 100;
			}
			if (pierceRating > 0)
			{
				if (Rnd.nextInt(100) < pierceRating)
					maxPierced++;
			}
		}
		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);
			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(this, getStat().getCriticalHitRate(target, null), target);
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, attack.soulshot);
		}
		// Check if the L2Character is a L2PcInstance
		if (this instanceof L2PcInstance)
		{
			SetupGauge sg = new SetupGauge(SetupGauge.RED, originalsAtk + reuse);
			sendPacket(sg);
		}
		final int aoeRadius = (int) calcStat(Stats.BOW_AOE_RADIUS, 0, null, null);
		float aoeDmgMod = 0;
		if (aoeRadius > 0)
			aoeDmgMod = (float) calcStat(Stats.BOW_AOE_DMG_MOD, 75, null, null);
		if (chainedShots > 0)
		{
			if (!crossbow)
				sendMessage("You fire a second arrow in the same draw!");
			else
				sendMessage("Double shot with the crossbow!");
			doChainAttackHitByBow(target, (int) (sAtk * 1.2), pierceRating, aoeRadius, aoeDmgMod);
		}
		if (maxPierced >= 1 && this instanceof L2PcInstance)
		{
			final boolean isTargettingMob = !(target instanceof L2Playable);
			final int distance = (int) Math.max(1, Util.calculateDistance(this, target, true));
			for (L2Character chara : getKnownList().getKnownCharactersInRadius(distance))
			{
				if (chara == null || chara == this || chara == target)
					continue;
				if (isTargettingMob)
				{
					if (chara instanceof L2Playable)
						continue;
				}
				else
				{
					if (chara instanceof L2MonsterInstance)
						continue;
				}
				if (chara.isAlikeDead())
					continue;
				if (!chara.isAutoAttackable(this))
					continue;
				if (!isFacing(chara, 10))
					continue;
				final int distanceTo = (int) Util.calculateDistance(this, chara, true);
				double sAtkRatio = distanceTo / distance;
				assert (sAtkRatio <= 1);
				sAtkRatio += (1 - sAtkRatio) / 1.5;
				if (sAtkRatio > 0.99)
					sAtkRatio = 0.99;
				if (_doubleShotted)
					doPierceAttackHitByBow(chara, (int) (originalsAtk * sAtkRatio * 1.2), 0, 85);
				else
					doPierceAttackHitByBow(chara, (int) (originalsAtk * sAtkRatio), 0, 85);
				maxPierced--;
				if (maxPierced == 0)
					break;
			}
		}
		if (aoeRadius > 0)
		{
			final boolean isTargettingMob = !(target instanceof L2Playable);
			byte count = 0;
			boolean pvpupdated = false;
			final int distance = (int) Math.max(1, Util.calculateDistance(this, target, true));
			for (L2Character chara : getKnownList().getKnownCharactersInRadius(distance + aoeRadius))
			{
				if (chara == null || chara == this || chara == target)
					continue;
				if (isTargettingMob)
				{
					/*
					 * if (chara instanceof L2Playable)
					 * continue;
					 */
				}
				else
				{
					if (chara instanceof L2MonsterInstance)
						continue;
				}
				if (chara.isAlikeDead())
					continue;
				if (!Util.checkIfInRange(aoeRadius, target, chara, true))
					continue;
				if (!chara.isAutoAttackable(this))
					continue;
				if (!pvpupdated && (chara instanceof L2Playable || chara instanceof L2Decoy))
				{
					if (this instanceof L2PcInstance)
					{
						getActingPlayer().updatePvPStatus(chara);
						pvpupdated = true;
					}
				}
				if (_doubleShotted)
					doPierceAttackHitByBow(chara, (int) (originalsAtk * 1.2), aoeRadius, aoeDmgMod);
				else
					doPierceAttackHitByBow(chara, (originalsAtk), aoeRadius, aoeDmgMod);
				count++;
				if (count >= 10)
					break;
			}
		}
		// Create a new hit task with Medium priority
		if (_doubleShotted)
			ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1, (byte) 1), (long) (originalsAtk * 1.2));
		else
			ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1, (byte) 1), originalsAtk);
		// Calculate and set the disable delay of the bow in function of the Attack Speed
		_disableBowAttackEndTime = Math.max((originalsAtk + reuse) / GameTimeController.MILLIS_IN_TICK + GameTimeController.getGameTicks(), _attackEndTime);
		// Add this hit to the Server-Client packet Attack
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1));
		if (chainedShots > 0)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					broadcastPacket(attack);
				}
			}, (long) (originalsAtk / 1.52));
		}
		// Return true if hit isn't missed
		return !miss1;
	}
	
	final private void doChainAttackHitByBow(L2Character target, int sAtk, int pierceRating, int aoeRadius, float aoeDmgMod)
	{
		int maxPierced = 0;
		if (pierceRating > 0)
		{
			while (pierceRating >= 100)
			{
				maxPierced++;
				pierceRating -= 100;
			}
			if (pierceRating > 0)
			{
				if (Rnd.nextInt(100) < pierceRating)
					maxPierced++;
			}
		}
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target, 3);
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);
			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(this, getStat().getCriticalHitRate(target, null), target);
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, true);
		}
		if (maxPierced >= 1 && this instanceof L2PcInstance)
		{
			final boolean isTargettingMob = !(target instanceof L2Playable);
			final int distance = (int) Math.max(1, Util.calculateDistance(this, target, true));
			for (L2Character chara : getKnownList().getKnownCharactersInRadius(distance))
			{
				if (chara == null || chara == this || chara == target)
					continue;
				if (isTargettingMob)
				{
					if (chara instanceof L2Playable)
						continue;
				}
				else
				{
					if (chara instanceof L2MonsterInstance)
						continue;
				}
				if (chara.isAlikeDead())
					continue;
				if (!chara.isAutoAttackable(this))
					continue;
				if (!isFacing(chara, 14))
					continue;
				final int distanceTo = (int) Util.calculateDistance(this, chara, true);
				double sAtkRatio = distanceTo / distance;
				assert (sAtkRatio <= 1);
				sAtkRatio += (1 - sAtkRatio) / 1.5;
				if (sAtkRatio > 0.99)
					sAtkRatio = 0.99;
				if (_doubleShotted)
					doPierceAttackHitByBow(chara, (int) (sAtk * sAtkRatio * 1.2), 0, 85);
				else
					doPierceAttackHitByBow(chara, (int) (sAtk * sAtkRatio), 0, 85);
				maxPierced--;
				if (maxPierced == 0)
					break;
			}
		}
		if (aoeRadius > 0)
		{
			final boolean isTargettingMob = !(target instanceof L2Playable);
			byte count = 0;
			boolean pvpupdated = false;
			for (L2Character chara : target.getKnownList().getKnownCharactersInRadius(aoeRadius))
			{
				if (chara == null || chara == this || chara == target)
					continue;
				if (isTargettingMob)
				{
					/*
					 * if (chara instanceof L2Playable)
					 * continue;
					 */
				}
				else
				{
					if (chara instanceof L2MonsterInstance)
						continue;
				}
				if (chara.isAlikeDead())
					continue;
				if (!chara.isAutoAttackable(this))
					continue;
				if (!pvpupdated && (chara instanceof L2Playable || chara instanceof L2Decoy))
				{
					if (this instanceof L2PcInstance)
					{
						getActingPlayer().updatePvPStatus(chara);
						pvpupdated = true;
					}
				}
				if (_doubleShotted)
					doPierceAttackHitByBow(chara, (int) (sAtk * 1.2), aoeRadius, aoeDmgMod);
				else
					doPierceAttackHitByBow(chara, (sAtk), aoeRadius, aoeDmgMod);
				count++;
				if (count >= 10)
					break;
			}
		}
		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, true, shld1, (byte) 1), (long) (sAtk * 1.1));
		final int ignoreAutoTargetChance = (int) calcStat(Stats.IGNORE_AUTOTARGET_ATTACK, 0, null, null);
		// Create a Server->Client packet Attack
		Attack attack = new Attack(this, true, 5, ignoreAutoTargetChance);
		// Add this hit to the Server-Client packet Attack
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1));
		broadcastPacket(attack);
	}
	
	final private void doPierceAttackHitByBow(L2Character target, int sAtk, int aoeRadius, float dmgMod)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target, 3);
		if (!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Formulas.calcCrit(this, getStat().getCriticalHitRate(target, null), target);
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, true);
			damage1 *= dmgMod / 100;
		}
		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, true, shld1, aoeRadius == 0), sAtk);
	}
	
	private L2Character findPossibleTank(L2Character target)
	{
		if (this instanceof L2Playable && target instanceof L2Playable)
		{
			final L2PcInstance player = target.getActingPlayer();
			if (player == null)
				return target;
			for (L2PcInstance possibleTank : target.getKnownList().getKnownPlayersInRadius(500))
			{
				if (possibleTank == null || possibleTank == target || possibleTank == this)
					continue;
				if (possibleTank.isTankClass() && !possibleTank.isTransformed() && Rnd.get(100) < possibleTank.calcStat(Stats.TANK_ARCHERY, 0, null, null) && possibleTank.getCurrentHp() >= possibleTank.getMaxHp() * 0.15 && possibleTank.isAutoAttackable(this))
				{
					if (player.getInSameClanAllyAs(possibleTank) > 1 || (player.isInParty() && player.getParty().getPartyMembers().contains(possibleTank)))
					{
						if (possibleTank.isFacing(this, 30) && isFacing(possibleTank, 30) && target.isBehind(possibleTank, 40))
						{
							possibleTank.sendMessage("You have tanked for " + target.getName());
							return possibleTank;
						}
					}
				}
			}
			if (!Util.checkIfInRange(500, this, target, false))
			{
				for (L2PcInstance possibleTank : getKnownList().getKnownPlayersInRadius(70))
				{
					if (possibleTank == null || possibleTank == target || possibleTank == this)
						continue;
					if (possibleTank.isTankClass() && !possibleTank.isTransformed() && Rnd.get(100) < possibleTank.calcStat(Stats.TANK_ARCHERY, 0, null, null) && possibleTank.getCurrentHp() >= possibleTank.getMaxHp() * 0.15 && possibleTank.isAutoAttackable(this))
					{
						if (player.getInSameClanAllyAs(possibleTank) > 1 || (player.isInParty() && player.getParty().getPartyMembers().contains(possibleTank)))
						{
							if (possibleTank.isFacing(this, 35) && isFacing(possibleTank, 35) && target.isBehind(possibleTank, 40))
							{
								possibleTank.sendMessage("You have tanked for " + target.getName());
								return possibleTank;
							}
						}
					}
				}
			}
		}
		return target;
	}
	
	private boolean doAttackHitByDual(Attack attack, L2Character target, int sAtk)
	{
		return doAttackHitByDual(attack, target, 100, sAtk, (byte) 1);
	}
	
	private boolean doAttackHitByDual(Attack attack, L2Character target, double attackpercent, int sAtk, byte absNerf)
	{
		int damage1 = 0;
		int damage2 = 0;
		byte shld1 = 0;
		byte shld2 = 0;
		boolean crit1 = false;
		boolean crit2 = false;
		// Calculate if hits are missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target, attackpercent == 100 ? 0 : 6);
		boolean miss2 = Formulas.calcHitMiss(this, target, attackpercent == 100 ? 0 : 6);
		final int critRate = getStat().getCriticalHitRate(target, null);
		// Check if hit 1 isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient against hit 1
			shld1 = Formulas.calcShldUse(this, target);
			// Calculate if hit 1 is critical
			crit1 = Formulas.calcCrit(this, critRate, target);
			// Calculate physical damages of hit 1
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, true, attack.soulshot);
			if (attackpercent == 100)
			{
				damage1 /= 2;
			}
			else
			{
				if (attackpercent < 1)
					attackpercent = 1;
				damage1 /= (2 / (attackpercent / 100));
			}
		}
		// Check if hit 2 isn't missed
		if (!miss2)
		{
			// Calculate if shield defense is efficient against hit 2
			shld2 = Formulas.calcShldUse(this, target);
			// Calculate if hit 2 is critical
			crit2 = Formulas.calcCrit(this, critRate, target);
			// Calculate physical damages of hit 2
			damage2 = (int) Formulas.calcPhysDam(this, target, null, shld2, crit2, true, attack.soulshot);
			if (attackpercent == 100)
			{
				damage2 /= 2;
			}
			else
			{
				if (attackpercent < 1)
					attackpercent = 1;
				damage2 /= (2 / (attackpercent / 100));
			}
		}
		// Create a new hit task with Medium priority for hit 1
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1, absNerf), sAtk / 2);
		// Create a new hit task with Medium priority for hit 2 with a higher delay
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage2, crit2, miss2, attack.soulshot, shld2, absNerf), sAtk);
		final int extraAttack = (int) calcStat(Stats.EXTRA_ATTACK, 0, target, null);
		if (!miss1 && crit1 && extraAttack > Rnd.get(100))
		{
			/* boolean crit3 = Formulas.calcCrit(this, critRate * 1.33, target); */
			int damage3 = (int) Formulas.calcPhysDam(this, target, null, (byte) 0, true, true, attack.soulshot);
			ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage3, true, false, attack.soulshot, (byte) 0, absNerf, true), (long) ((sAtk / 2) * 1.1));
		}
		if (!miss2 && crit2 && extraAttack > Rnd.get(100))
		{
			/* boolean crit4 = Formulas.calcCrit(this, critRate * 1.33, target); */
			int damage4 = (int) Formulas.calcPhysDam(this, target, null, (byte) 0, true, true, attack.soulshot);
			ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage4, true, false, attack.soulshot, (byte) 0, absNerf, true), (long) (sAtk * 1.1));
		}
		// Add those hits to the Server-Client packet Attack
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1), attack.createHit(target, damage2, miss2, crit2, shld2));
		// Return true if hit 1 or hit 2 isn't missed
		return (!miss1 || !miss2);
	}
	
	private boolean doAOEHitbyDualWeapon(Attack attack, L2Character target, int sAtk)
	{
		double angleChar;
		final int maxAngleDiff = (int) getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);
		if (maxAngleDiff < 355)
		{
			angleChar = Util.convertHeadingToDegree(getHeading());
			if (angleChar <= 0)
				angleChar += 360;
		}
		boolean hitted = doAttackHitByDual(attack, target, sAtk);
		final int attackCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null) - 1;
		assert (attackCountMax >= 1);
		int attackpercent = (int) getStat().calcStat(Stats.AOE_HIT_OTHER_PERCENT, 80, null, null);
		if (attackpercent > 160)
		{
			_log.warning("lol wtf doaeohitbydualweapon has attackpercent greater than 160! with char name " + getName());
			attackpercent = 160;
		}
		// if (isInFunEvent() && getActingPlayer()._inEventDM)
		// attackpercent = 30;
		int attackcount = 0;
		final boolean isTargettingMob = (target instanceof L2Attackable);
		for (L2Character obj : getKnownList().getKnownCharactersInRadius((int) getStat().calcStat(Stats.POWER_ATTACK_RANGE, getTemplate().baseAtkRange, null, null)))
		{
			if (obj == this)
				continue;
			if (obj == target)
				continue;
			if (obj instanceof L2Attackable || obj instanceof L2Playable || obj instanceof L2DoorInstance || obj instanceof L2Decoy)
			{
				if (isTargettingMob)
				{
					if (obj instanceof L2Playable || obj instanceof L2Decoy)
						continue;
					if (!canAttackDueToSoloMob(obj))
						continue;
				}
				else
				{
					if (obj instanceof L2MonsterInstance)
						continue;
				}
				if (obj.isAlikeDead() || (obj.isInvisible() && obj.isGM()))
					continue;
				if (Math.abs(obj.getZ() - getZ()) > 650)
					continue;
				if (!isFacing(obj, maxAngleDiff))
					continue;
				if (!GeoData.getInstance().canSeeTarget(this, obj))
					continue;
				if (obj.calcStat(Stats.EVADE_AOE_HIT, 0, null, null) > Rnd.get(100))
					continue;
				if (obj.isAutoAttackable(this))
				{
					attackcount += 1;
					hitted |= doAttackHitByDual(attack, obj, attackpercent, sAtk, (byte) (attackcount + 1));
					if (attackcount >= attackCountMax)
						break;
				}
			}
		}
		return hitted;
	}
	
	private boolean doAOEHitbySingleWeapon(Attack attack, L2Character target, int sAtk)
	{
		double angleChar;
		final int maxAngleDiff = (int) getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);
		if (maxAngleDiff < 355)
		{
			angleChar = Util.convertHeadingToDegree(getHeading());
			if (angleChar <= 0)
				angleChar += 360;
		}
		final L2Weapon weaponItem = getActiveWeaponItem();
		int maxRadius = 0;
		int attackCountMax = 0;
		int attackpercent = 0;
		int init = getTemplate().baseAtkRange;
		if (weaponItem != null)
		{
			if (weaponItem.getItemType() == L2WeaponType.POLE)
			{
				init = Math.max(66, init);
				maxRadius = (int) calcStat(Stats.POWER_ATTACK_RANGE, init, null, null);
				attackCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 3, null, null) - 1;
				attackpercent = (int) getStat().calcStat(Stats.AOE_HIT_OTHER_PERCENT, 85, null, null);
			}
			else
			{
				maxRadius = (int) calcStat(Stats.POWER_ATTACK_RANGE, init, null, null);
				attackCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null) - 1;
				attackpercent = (int) getStat().calcStat(Stats.AOE_HIT_OTHER_PERCENT, 80, null, null);
			}
		}
		else
		{
			if (this instanceof L2Attackable)
			{
				maxRadius = (int) calcStat(Stats.POWER_ATTACK_RANGE, init, null, null);
				attackCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null) - 1;
				attackpercent = (int) getStat().calcStat(Stats.AOE_HIT_OTHER_PERCENT, 80, null, null);
			}
			else
				return false;
		}
		boolean hitted = doAttackHitSimple(attack, target, sAtk);
		assert (attackCountMax >= 1);
		if (attackpercent > 160)
		{
			_log.warning("lol wtf doaeohitbysingleweapon has attackpercent greater than 160! with char name " + getName());
			attackpercent = 160;
		}
		// if (isInFunEvent() && getActingPlayer()._inEventDM)
		// attackpercent = 38;
		int attackcount = 0;
		final boolean isTargettingMob = (target instanceof L2Attackable);
		for (L2Character obj : getKnownList().getKnownCharactersInRadius(maxRadius))
		{
			if (obj == this)
				continue;
			if (obj == target)
				continue;
			if (obj instanceof L2Attackable || obj instanceof L2Playable || obj instanceof L2DoorInstance || obj instanceof L2Decoy)
			{
				if (isTargettingMob)
				{
					if (obj instanceof L2Playable || obj instanceof L2Decoy)
						continue;
					if (!canAttackDueToSoloMob(obj))
						continue;
				}
				else
				{
					if (obj instanceof L2MonsterInstance)
						continue;
				}
				if (obj.isAlikeDead() || (obj.isInvisible() && obj.isGM()))
					continue;
				if (Math.abs(obj.getZ() - getZ()) > 650)
					continue;
				if (!isFacing(obj, maxAngleDiff))
					continue;
				if (!GeoData.getInstance().canSeeTarget(this, obj))
					continue;
				if (obj.calcStat(Stats.EVADE_AOE_HIT, 0, null, null) > Rnd.get(100))
					continue;
				if (obj.isAutoAttackable(this))
				{
					attackcount += 1;
					hitted |= doAttackHitSimple(attack, obj, attackpercent, sAtk, (byte) (attackcount + 1));
					if (attackcount >= attackCountMax)
						break;
					if (weaponItem != null && (weaponItem.getItemType() == L2WeaponType.POLE))
						attackpercent /= 1.12;
				}
			}
		}
		return hitted;
	}
	
	private boolean doAttackHitSimple(Attack attack, L2Character target, int sAtk)
	{
		return doAttackHitSimple(attack, target, 100, sAtk, (byte) 1);
	}
	
	private boolean doAttackHitSimple(Attack attack, L2Character target, double attackpercent, int sAtk, byte absNerf)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target, attackpercent == 100 ? 0 : 3);
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);
			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(this, getStat().getCriticalHitRate(target, null), target);
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, attack.soulshot);
			if (attackpercent != 100)
				damage1 = (int) (damage1 * attackpercent / 100);
		}
		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1, absNerf), sAtk);
		final int extraAttack = (int) calcStat(Stats.EXTRA_ATTACK, 0, target, null);
		if (!miss1 && crit1 && extraAttack > Rnd.get(100))
		{
			/* boolean crit3 = Formulas.calcCrit(this, getStat().getCriticalHitRate(target, null) * 1.33, target); */
			int damage3 = (int) Formulas.calcPhysDam(this, target, null, (byte) 0, true, true, attack.soulshot);
			ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage3, true, false, attack.soulshot, (byte) 0, absNerf, true), (long) (sAtk * 1.1));
		}
		// Add this hit to the Server-Client packet Attack
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1));
		// Return true if hit isn't missed
		return !miss1;
	}
	
	/**
	 * Manage the casting task (casting and interrupt time, re-use delay...) and display the casting bar and animation on client.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Verify the possibilty of the the cast : skill is a spell, caster isn't muted...</li>
	 * <li>Get the list of all targets (ex : area effects) and define the L2Charcater targeted (its stats will be used in calculation)</li>
	 * <li>Calculate the casting time (base + modifier of MAtkSpd), interrupt time and re-use delay</li>
	 * <li>Send a Server->Client packet MagicSkillUser (to diplay casting animation), a packet SetupGauge (to display casting bar) and a system message</li>
	 * <li>Disable all skills during the casting time (create a task EnableAllSkills)</li>
	 * <li>Disable the skill during the re-use delay (create a task EnableSkill)</li>
	 * <li>Create a task MagicUseTask (that will call method onMagicUseTimer) to launch the Magic Skill at the end of the casting time</li><BR>
	 * <BR>
	 *
	 * @param skill
	 *            The L2Skill to use
	 */
	public void doCast(L2Skill skill)
	{
		beginCast(skill, false);
	}
	
	public void doSimultaneousCast(L2Skill skill)
	{
		beginCast(skill, true);
	}
	
	@SuppressWarnings("incomplete-switch")
	private void beginCast(L2Skill skill, boolean simultaneously)
	{
		broadcastUserInfo();
		if (!checkDoCastConditions(skill))
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);
			if (this instanceof L2PcInstance)
				getAI().setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		// Get all possible targets of the skill in a table in function of the skill target type
		L2Character[] targets = skill.getTargetList(this);
		if (targets == null || targets.length == 0)
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);
			if (this instanceof L2PcInstance)
				getAI().setIntention(AI_INTENTION_ACTIVE);
			return;
		}
		// Set the target of the skill in function of Skill Type and Target Type
		L2Character target = null;
		// AURA skills should always be using caster as target
		switch (skill.getTargetType(this))
		{
			case TARGET_AURA:
			case TARGET_PARTY:
			case TARGET_CLAN:
			case TARGET_ALLY:
			case TARGET_SELF:
			case TARGET_SELF_AND_PET:
			case TARGET_ALL:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_GROUND:
				target = this;
				break;
			default:
				target = targets[0];
		}
		if (target == null)
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);
			if (this instanceof L2PcInstance)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				getAI().setIntention(AI_INTENTION_ACTIVE);
			}
			return;
		}
		else if (!canAttackDueToSoloMob(target))
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			sendMessage("This mob already belongs to another player");
			return;
		}
		// Set the _castInterruptTime and casting status (L2PcInstance already has this true)
		if (!simultaneously)
		{
			setIsCastingNow(true);
			final int castRange = skill.getCastRange(this);
			if (castRange >= 1)
			{
				final int distanceToTarget = (int) Util.calculateDistance(this, target, false);
				_distanceToTargetCurrSkill = Math.max(castRange, distanceToTarget);
			}
			else
				_distanceToTargetCurrSkill = 0;
		}
		// Get the Identifier of the skill
		final Integer magicId = skill.getId();
		// Get the level of the skill
		int level = skill.getLevel();
		final boolean oly = ((this instanceof L2Playable || this instanceof L2Decoy) && getActingPlayer().isInOlympiadMode());
		if (level < 1)
			level = 1;
		else if (level >= 100 && oly)
		{
			level = SkillTable.getInstance().getMaxLevel(magicId);
			skill = SkillTable.getInstance().getInfo(magicId, level);
		}
		if (this instanceof L2Playable)
			getActingPlayer().onActionRequest();
		// Get the Display Identifier for a skill that client can't display
		int displayId = skill.getDisplayId();
		int displayLvl = skill.getDisplayLvl() > 1 ? skill.getDisplayLvl() : level;
		if (displayId == magicId && this instanceof L2PcInstance)
		{
			/*
			 * if (magicId == 1264 || magicId == 1265)
			 * {
			 * displayId = 1450; //solar spark/flare fps dropping fix replacing skill
			 * }
			 */
			if (magicId == 36 && getActingPlayer().getRace() == Race.DarkElf && !getActingPlayer().getAppearance().getSex())
			{
				if (getActingPlayer().isUsingDualWeapon())
					displayId = 84; // whirlwind for male darkelves
			}
			else if (magicId == 495) // blade rush on non-kamael races
			{
				if (getActingPlayer().getRace() == Race.Kamael)
				{
					if (getActingPlayer().isUsingDualWeapon())
						displayLvl = 5;
				}
				else if (getActingPlayer().getRace() != Race.Human)
				{
					displayLvl = 5; // display lvl 5 of blade rush
				}
			}
			else if (magicId == 263 && getActiveWeaponItem() != null && getActiveWeaponItem().getItemType() == L2WeaponType.BOW) // bow using deadly blow
			{
				displayId = 921;
			}
			else if (getActingPlayer().getRace() == Race.Kamael)
			{
				switch (magicId)
				{
					case 16: // mortal blow
						displayId = 468; // fallen attack
						break;
					case 30: // back stab
						displayId = 497; // crushing pain
						break;
					case 263: // deadly strike
						displayId = 478; // double thrust
						break;
					case 344: // lethal blow
						displayId = 506; // check mate
						break;
					case 409: // critical blow
						displayId = 505; // shining edge
						break;
					case 321: // blinding blow
						displayId = 504; // triple thrust
						break;
					case 358: // bluff
						displayId = 1231; // aura flare
						break;
					case 5: // double sonic slash
						displayId = 498; // contagion
						break;
					case 261: // triple sonic slash
						displayId = 505; // shining edge
						break;
					/*
					 * case 928: //dual blow
					 * displayId = 505; //shining edge
					 * break;
					 */
					case 36: // whirlwind
					case 320: // wrath
					case 347: // earthquake
						displayId = 492; // spread wing
						break;
					case 920: // power crush
						displayId = 477; // dark smash
						break;
					case 48: // thunderstorm
						displayId = 504; // triple thrust
						break;
					case 190: // fatal strike
						displayId = 476; // dark strike
						break;
					case 245: // wild sweep
						displayId = 496; // slashing blade
						break;
					case 181: // revival
						displayId = 833; // body reconstruction
						break;
					case 452: // shock stomp
						displayId = 361; // shock blast
						break;
				}
			}
			else if (magicId == 498) // contagion on non-kamael races
				displayId = 505; // shining edge
		}
		if (displayId != magicId)
			displayLvl = skill.getDisplayLvl();
		// Get the casting time of the skill (base)
		int hitTime = skill.getHitTime();
		int coolTime = skill.getCoolTime();
		final boolean effectWhileCasting = skill.getSkillType() == L2SkillType.FUSION || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME;
		// Calculate the casting time of the skill (base + modifier of MAtkSpd)
		// Don't modify the skill time for FORCE_BUFF skills. The skill time for those skills represent the buff time.
		if (!effectWhileCasting)
		{
			switch (skill.getSkillType())
			{
				case PDAM:
				case PDAMPERC:
				case BLOW:
				case CHARGEDAM:
				case FATAL:
				case CPDAM:
				{
					if (coolTime < 300)
						coolTime = 300;
				}
			}
			if (!skill.isStaticHitTime())
			{
				hitTime = Formulas.calcSkillCastTime(this, skill, hitTime);
				if (this instanceof L2Playable)
				{
					if (coolTime > 0)
						coolTime = Formulas.calcSkillCastTime(this, skill, coolTime);
				}
				else
					coolTime = 0;
			}
			hitTime = (int) calcStat(Stats.SKILL_HITTIME_CHANGE, hitTime, target, skill);
			if (isAPC() && skill.isHeal())
				hitTime /= 2;
			if (this instanceof L2PcInstance && skill.getSkillType() == L2SkillType.SUMMON && !skill.getName().contains("Cubic"))
			{
				if (getActingPlayer().isInCombat()) // summoning during combat raises duration by 7x
					hitTime *= 7;
			}
		}
		if (skill.getHitTime() >= 500 && hitTime < 500)
			hitTime = 500;
		// queue herbs and potions
		if (simultaneously)
		{
			if (isCastingSimultaneouslyNow())
			{
				ThreadPoolManager.getInstance().scheduleAi(new UsePotionTask(this, skill), 100);
				return;
			}
			else
				setIsCastingSimultaneouslyNow(true);
		}
		// Make sure that char is facing selected target
		if (target != this)
			setHeading(Util.calculateHeadingFrom(this, target));
		// Note: _castEndTime = GameTimeController.getGameTicks() + (coolTime + hitTime) / GameTimeController.MILLIS_IN_TICK;
		if (!simultaneously)
		{
			_castInterruptTime = GameTimeController.getGameTicks() + hitTime / GameTimeController.MILLIS_IN_TICK;
			setLastSkillCast(skill);
		}
		else
			setLastSimultaneousSkillCast(skill);
		final boolean isRushSkill = skill.getId() != 10011 && skill.getFlyType() != null;
		// Init the reuse time of the skill
		int reuseDelay = skill.getReuseDelay(this);
		final int skillRetries = skill.getRetries(this);
		if (skillRetries >= 1)
		{
			if (_retrySkills.containsKey(magicId))
			{
				final Integer[] info = _retrySkills.get(magicId);
				if (info[1] < GameTimeController.getGameTicks())
				{
					final Integer[] newinfo =
					{
						1, (int) (10 + GameTimeController.getGameTicks() + (1.3 * skill.getReuseDelay(this) / GameTimeController.MILLIS_IN_TICK))
					};
					_retrySkills.put(magicId, newinfo);
					reuseDelay = 0;
					sendMessage(skill.getName() + " can be used " + skillRetries + " more time(s) without reuse delay");
				}
				else
				{
					if (info[0] < skillRetries)
					{
						final Integer[] newinfo =
						{
							info[0] + 1, (int) (10 + GameTimeController.getGameTicks() + (1.3 * skill.getReuseDelay(this) / GameTimeController.MILLIS_IN_TICK))
						};
						_retrySkills.put(magicId, newinfo);
						reuseDelay = 0;
						sendMessage(skill.getName() + " can be used " + (skillRetries - info[0]) + " more time(s) without reuse delay");
					}
					else
					{
						_retrySkills.remove(magicId);
					}
				}
			}
			else
			{
				final Integer[] info =
				{
					1, (int) (10 + GameTimeController.getGameTicks() + (1.3 * skill.getReuseDelay(this) / GameTimeController.MILLIS_IN_TICK))
				};
				_retrySkills.put(magicId, info);
				reuseDelay = 0;
				sendMessage(skill.getName() + " can be used " + skillRetries + " more time(s) without reuse delay");
			}
		}
		if (reuseDelay > 0)
		{
			if (!skill.isStaticReuse())
			{
				if (skill.isMagic())
					reuseDelay = (int) (skill.getReuseDelay(this) * getStat().getMReuseRate(skill, target));
				else
					reuseDelay = (int) (skill.getReuseDelay(this) * getStat().getPReuseRate(skill, target));
				if (isRushSkill)
				{
					if (this instanceof L2PcInstance)
					{
						if (skill.getId() == 495 && !getActingPlayer().isKamaelClass())
							reuseDelay *= 2.1;
					}
				}
			}
			if (this instanceof L2Attackable)
			{
				if (skill.isDamaging())
				{
					if (reuseDelay > 100000)
						reuseDelay /= 2.6;
					else if (reuseDelay > 32000)
						reuseDelay /= 2.1;
					else if (reuseDelay > 10000)
						reuseDelay /= 1.6;
					else
						reuseDelay /= 1.1;
				}
			}
		}
		boolean skillMastery = !oly && Formulas.calcSkillMastery(this, skill);
		// Skill reuse check
		if (reuseDelay > 25000 && !skillMastery)
			addTimeStamp(skill.getId(), reuseDelay);
		int initmpcons = isGM() ? 0 : getStat().getMpInitialConsume(skill);
		if (initmpcons > 0)
		{
			StatusUpdate su = new StatusUpdate(getObjectId());
			if (skill.isDance())
			{
				getStatus().reduceMp(calcStat(Stats.DANCE_MP_CONSUME_RATE, initmpcons, null, null));
			}
			else if (skill.isMagic())
			{
				getStatus().reduceMp(calcStat(Stats.MAGICAL_MP_CONSUME_RATE, initmpcons, null, null));
			}
			else
			{
				getStatus().reduceMp(calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, initmpcons, null, null));
			}
			su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
			sendPacket(su);
		}
		if (skill.getSkillType() == L2SkillType.TAKECASTLE)
		{
			if (((L2PcInstance) this).isInvisible() && !((L2PcInstance) this).isGM())
				stopEffects(L2EffectType.INVISIBLE);
			if (((L2PcInstance) this)._inEventFOS && FOS._started)
			{
				FOS.Announcements(getName() + " has started casting Seal of Ruler!");
			}
			if (((L2PcInstance) this)._inEventFOS && NewFOS._started)
			{
				NewFOS.Announcements(getName() + " has started casting Seal of Ruler!");
			}
			else
			{
				Castle castle = CastleManager.getInstance().getCastle(this);
				if (castle != null)
					castle.getSiege().announceToPlayer(getName() + " has started casting Seal of Ruler!", true);
			}
		}
		else if (skill.getSkillType() == L2SkillType.TAKEFORT)
		{
			if (((L2PcInstance) this).isInvisible() && !((L2PcInstance) this).isGM())
				stopEffects(L2EffectType.INVISIBLE);
			Fort fort = FortManager.getInstance().getFort(this);
			if (fort != null)
				fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.S1_TRYING_RAISE_FLAG), getActingPlayer().getClan().getName());
		}
		if (skill.isHeal())
		{}
		else
		{
			if (this instanceof L2Playable && target instanceof L2Playable)
			{
				if (skill.getId() != 1235 && skill.isDamaging() && (skill.getTargetType(this) == SkillTargetType.TARGET_ONE || skill.getTargetType(this) == SkillTargetType.TARGET_AREA || skill.getTargetType(this) == SkillTargetType.TARGET_BEHIND_AREA || isRushSkill))
				{
					if (skill.getCastRange(this) > 120)
					{
						final L2PcInstance player = target.getActingPlayer();
						if (player == null)
							return;
						for (L2PcInstance possibleTank : target.getKnownList().getKnownPlayersInRadius((int) Util.calculateDistance(this, target, true) + 10))
						{
							if (possibleTank == null || possibleTank == target || possibleTank == this)
								continue;
							if (possibleTank.isTankClass() && !possibleTank.isTransformed() && Rnd.get(100) < possibleTank.calcStat(Stats.TANK_SPELLS, 0, null, null) && possibleTank.getCurrentHp() >= possibleTank.getMaxHp() * 0.15 && possibleTank.isAutoAttackable(this))
							{
								if (player.getInSameClanAllyAs(possibleTank) > 1 || (player.isInParty() && player.getParty().getPartyMembers().contains(possibleTank)))
								{
									if (possibleTank.isFacing(this, 30) && isFacing(possibleTank, 30) && target.isBehind(possibleTank, 40))
									{
										possibleTank.sendMessage("You have tanked for " + target.getDisplayName());
										sendMessage(possibleTank.getDisplayName() + " tanked your " + skill.getName());
										target = possibleTank;
										targets = skill.getTargetList(this, false, possibleTank);
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		boolean broadcastanimation = false;
		// Disable the skill during the re-use delay and create a task EnableSkill with Medium priority to enable it at the end of the re-use delay
		if (reuseDelay > 100)
		{
			if (skillMastery)
			{
				reuseDelay = 100;
				if (getActingPlayer() != null)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_READY_TO_USE_AGAIN);
					getActingPlayer().sendPacket(sm);
					sm = null;
				}
				broadcastanimation = true;
			}
			disableSkill(skill.getId(), reuseDelay);
		}
		// For force buff skills, start the effect as long as the player is casting.
		if (effectWhileCasting)
		{
			// Consume Items if necessary and Send the Server->Client packet InventoryUpdate with Item modification to all the L2Character
			if (skill.getItemConsume() > 0)
			{
				if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, false))
				{
					sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					if (simultaneously)
						setIsCastingSimultaneouslyNow(false);
					else
						setIsCastingNow(false);
					if (this instanceof L2PcInstance)
						getAI().setIntention(AI_INTENTION_ACTIVE);
					return;
				}
			}
			// Consume Souls if necessary
			if (skill.getSoulConsumeCount() > 0 || skill.getMaxSoulConsumeCount(this) > 0)
			{
				if (this instanceof L2PcInstance)
				{
					if (!((L2PcInstance) this).decreaseSouls(skill.getSoulConsumeCount(), skill))
					{
						if (simultaneously)
							setIsCastingSimultaneouslyNow(false);
						else
							setIsCastingNow(false);
						if (this instanceof L2PcInstance)
							getAI().setIntention(AI_INTENTION_ACTIVE);
						return;
					}
				}
			}
			if (skill.getSkillType() == L2SkillType.FUSION)
				startFusionSkill(target, skill);
			else
				callSkill(skill, targets);
		}
		else
		{
			doSkillSoulShotCharge(skill);
			if (skill.isTrap() && (getActingPlayer().isInGludin() || getActingPlayer().isInHuntersVillage() || getActingPlayer().isInPI() || getActingPlayer().isInOrcVillage()))
				getActingPlayer().updatePvPStatus();
		}
		if ((displayId == magicId.intValue()) && (displayLvl == level))
		{
			broadcastPacket(new MagicSkillUse(this, target, displayId, displayLvl, hitTime, reuseDelay));
		}
		else
		{
			sendPacket(new MagicSkillUse(this, target, magicId.intValue(), level, 0, reuseDelay));
			broadcastPacket(new MagicSkillUse(this, target, displayId, displayLvl, hitTime, 0));
		}
		// Send a system message USE_S1 to the L2Character
		if (this instanceof L2PcInstance && magicId != 1312)
		{
			int chance = -1;
			if (skill.isOffensive() && skill.isDebuff())
			{
				if (skill.isDamaging())
				{
					if (skill.hasEffects())
					{
						chance = skill.getEffectsLandChance(this, targets[0]);
					}
					else if (skill.isBlow())
					{
						byte successChance = Blow.SIDE;
						if (isBehind(target))
							successChance = Blow.BEHIND;
						else if (isInFrontOf(target))
							successChance = Blow.FRONT;
						chance = Formulas.calcBlowChance(this, target, successChance);
					}
				}
				else
				{
					chance = Formulas.calcSkillSuccessChance(this, targets[0], skill, (byte) 0);
				}
			}
			else if (skill.isBlow())
			{
				byte successChance = Blow.SIDE;
				if (isBehind(target))
					successChance = Blow.BEHIND;
				else if (isInFrontOf(target))
					successChance = Blow.FRONT;
				chance = Formulas.calcBlowChance(this, target, successChance);
			}
			SystemMessage sm = new SystemMessage(SystemMessageId.USE_S1);
			if (chance >= 0)
				sm.addString(skill.getName() + " (" + chance + "%)");
			else
				sm.addSkillName(skill);
			sendPacket(sm);
		}
		if (isRushSkill)
		{
			int id, x, y, z, tx, ty, tz;
			id = getObjectId();
			x = getX();
			y = getY();
			z = getZ();
			tx = target.getX();
			ty = target.getY();
			tz = target.getZ();
			if (target.isMoving())
			{
				final int targetHeading = target.getHeading();
				final int headingDegree = (int) Util.convertHeadingToDegree(targetHeading);
				final int dx = (int) (Math.cos(Math.toRadians(headingDegree)) * 55);
				final int dy = (int) (Math.sin(Math.toRadians(headingDegree)) * 55);
				tx += dx;
				ty += dy;
			}
			if (x > tx)
				tx++;
			else if (x < tx)
				tx--;
			if (y > ty)
				ty++;
			else if (y < ty)
				ty--;
			if (Config.GEODATA > 0)
			{
				Location destiny = GeoData.getInstance().moveCheck(x, y, z, tx, ty, tz, getInstanceId());
				tx = destiny.getX();
				ty = destiny.getY();
				tz = destiny.getZ();
			}
			broadcastPacket(new FlyToLocation(id, x, y, z, tx, ty, tz, FlyType.valueOf(skill.getFlyType())));
			getPosition().setXYZ(tx, ty, tz);
			final int hamstring = (int) calcStat(Stats.HAMSTRING, 0, null, skill);
			if (hamstring > 0 && skill.getFlyType() != "DUMMY")
			{
				final int distance = (int) Util.calculateDistance(x, y, z, tx, ty);
				reduceCurrentHp(hamstring * distance, this, true, true, null, true);
			}
		}
		// broadcastUserInfo(false);
		if (broadcastanimation)
		{
			if (skill.isMagic())
				broadcastPacket(new MagicSkillUse(this, 2164, 1, 0, 0)); // blessed spirit shot S
			else
				broadcastPacket(new MagicSkillUse(this, 2154, 1, 0, 0)); // soul shot S
		}
		// broadcastUserInfo(true);
		MagicUseTask mut = new MagicUseTask(targets, skill, hitTime, coolTime, simultaneously, 0);
		// launch the magic in hitTime milliseconds
		if (hitTime > 310)
		{
			// Send a Server->Client packet SetupGauge with the color of the gauge and the casting time
			if (this instanceof L2PcInstance && !effectWhileCasting)
			{
				if (hitTime > 500 && !isRushSkill)
					sendPacket(new SetupGauge(SetupGauge.BLUE, hitTime));
			}
			if (skill.getHitCounts() > 0)
			{
				hitTime = hitTime * skill.getHitTimings()[0] / 100;
				if (hitTime < 310)
					hitTime = 310;
			}
			if (effectWhileCasting)
				mut._phase = 2;
			if (simultaneously)
			{
				if (_skillCast2 != null)
				{
					_skillCast2.cancel(true);
					_skillCast2 = null;
				}
				_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime - 300);
			}
			else
			{
				if (_skillCast != null)
				{
					_skillCast.cancel(true);
					_skillCast = null;
				}
				_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime - 300);
			}
		}
		else
		{
			mut._hitTime = 0;
			onMagicLaunchedTimer(mut);
		}
		broadcastUserInfo();
	}
	
	public void doSkillSoulShotCharge(L2Skill skill)
	{
		if (this instanceof L2Attackable && !((L2Attackable) this).isUsingShot())
			return;
		int ssGrade = getSoulshotGrade();
		int soulshotId = 0;
		if (skill.useSpiritShot())
		{
			switch (ssGrade)
			{
				case 5:
					soulshotId = 2164;
					break;
				case 4:
					soulshotId = 2163;
					break;
				case 3:
					soulshotId = 2162;
					break;
				case 2:
					soulshotId = 2161;
					break;
				case 1:
					soulshotId = 2160;
					break;
				default:
					soulshotId = 2047;
					break;
			}
		}
		else if (skill.useSoulShot())
		{
			switch (ssGrade)
			{
				case 5:
					soulshotId = 2154;
					break;
				case 4:
					soulshotId = 2153;
					break;
				case 3:
					soulshotId = 2152;
					break;
				case 2:
					soulshotId = 2151;
					break;
				case 1:
					soulshotId = 2150;
					break;
				default:
					soulshotId = 2039;
					break;
			}
		}
		if (soulshotId != 0)
		{
			if (ssGrade == 5 && this instanceof L2PcInstance)
				sendPacket(new MagicSkillUse(this, soulshotId, 1, 0, 0)); // broadcast soulshot charge animation for tard grs
			else
				broadcastPacket(new MagicSkillUse(this, soulshotId, 1, 0, 0)); // broadcast soulshot charge animation for tard grs
		}
	}
	
	public boolean checkDoCastConditionsNPC(L2Skill skill, L2Object l2Object)
	{
		if (skill.isPassive() || !(l2Object instanceof L2Character) || !checkDoCastConditions(skill))
			return false;
		if (skill != null && skill.checkCondition(this, l2Object, false))
			return true;
		return false;
	}
	
	@SuppressWarnings("incomplete-switch")
	public boolean checkDoCastConditions(L2Skill skill)
	{
		if (skill == null || isSkillDisabled(skill.getId()))
			return false;
		// Check if the caster has enough MP
		if (!isGM() && getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
		{
			// Send a System Message to the caster
			sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
			return false;
		}
		// Check if the caster has enough HP
		if (getCurrentHp() <= skill.getHpConsume())
		{
			if (this instanceof L2PcInstance)
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_HP));
			return false;
		}
		if (!(this instanceof L2Playable))
			return true;
		switch (skill.getSkillType())
		{
			case SUMMON_TRAP:
			{
				if (isInsideZone(ZONE_PEACE))
				{
					if (this instanceof L2PcInstance)
					{
						if (isInFunEvent())
						{
							return true;
						}
						else
						{
							((L2PcInstance) this).sendPacket(new SystemMessage(SystemMessageId.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_PEACE_ZONE));
							return false;
						}
					}
				}
				break;
			}
			case SUMMON:
			{
				if (!((L2SkillSummon) skill).isCubic() && this instanceof L2PcInstance && (((L2PcInstance) this).getPet() != null || ((L2PcInstance) this).isMounted()))
				{
					if (Config.DEBUG)
						_log.fine("player has a pet already. ignore summon skill");
					sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_HAVE_A_PET));
					return false;
				}
				break;
			}
			case LEARN_SKILL:
			{
				boolean exit = true;
				final L2SkillLearnSkill learnSkill = (L2SkillLearnSkill) skill;
				for (int i = 0; i < learnSkill._learnSkillId.length; i++)
				{
					if (learnSkill._learnSkillId[i] != 0 && getSkillLevel(learnSkill._learnSkillId[i]) < learnSkill._learnSkillLvl[i])
					{
						exit = false;
						break;
					}
				}
				if (exit)
				{
					sendMessage("You already know this skill");
					return false;
				}
				break;
			}
		}
		if (!skill.isPotion())
		{
			// Check if the skill is a magic spell and if the L2Character is not muted
			if (skill.isMagic())
			{
				if (isMuted())
					return false;
			}
			else
			{
				// Check if the skill is physical and if the L2Character is not physical_muted
				if (isPhysicalMuted())
					return false;
			}
		}
		// prevent casting signets to peace zone
		if (skill.getSkillType() == L2SkillType.SIGNET || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME)
		{
			L2WorldRegion region = getWorldRegion();
			if (region == null)
				return false;
			boolean canCast = true;
			if (skill.getTargetType(this) == SkillTargetType.TARGET_GROUND && this instanceof L2PcInstance)
			{
				Point3D wp = ((L2PcInstance) this).getCurrentSkillWorldPosition();
				if (!region.checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ()))
					canCast = false;
			}
			else if (!region.checkEffectRangeInsidePeaceZone(skill, getX(), getY(), getZ()))
				canCast = false;
			if (!canCast)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(skill);
				sendPacket(sm);
				return false;
			}
		}
		// Check if the caster owns the weapon needed
		if (!skill.getWeaponDependancy(this))
			return false;
		// Check if the spell consumes an Item
		// TODO: combine check and consume
		if (skill.getItemConsume() > 0 && getInventory() != null)
		{
			// Get the L2ItemInstance consumed by the spell
			L2ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());
			// Check if the caster owns enough consumed Item to cast
			if (requiredItems == null || requiredItems.getCount() < skill.getItemConsume())
			{
				// Checked: when a summon skill failed, server show required consume item count
				if (skill.getSkillType() == L2SkillType.SUMMON)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.SUMMONING_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addNumber(skill.getItemConsume());
					sendPacket(sm);
					return false;
				}
				else
				{
					L2Item itemTemplate = ItemTable.getInstance().getTemplate(skill.getItemConsumeId());
					String itemName = itemTemplate != null ? itemTemplate.getName() : "error inform a GM";
					sendMessage("You need " + skill.getItemConsume() + " " + itemName + " to use " + skill.getName());
					/*
					 * // Send a System Message to the caster
					 * sendPacket(new SystemMessage(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL));
					 */
					return false;
				}
			}
		}
		if (this instanceof L2PcInstance)
		{
			int charges = ((L2PcInstance) this).getCharges();
			// Check if the spell using charges or not in AirShip
			if (charges < skill.getRequiredCharges() || (skill.getMaxCharges() == 0 && charges < skill.getNumCharges()) || ((L2PcInstance) this).isInAirShip())
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(skill);
				sendPacket(sm);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Index according to skill id the current timestamp of use.<br>
	 * <br>
	 *
	 * @param skill
	 *            id
	 * @param reuse
	 *            delay
	 *            <BR>
	 *            <B>Overridden in :</B> (L2PcInstance)
	 */
	public void addTimeStamp(int s, int r)
	{/***/
	}
	
	/**
	 * Index according to skill id the current timestamp of use.<br>
	 * <br>
	 *
	 * @param skill
	 *            id
	 *            <BR>
	 *            <B>Overridden in :</B> (L2PcInstance)
	 */
	public void removeTimeStamp(int s)
	{/***/
	}
	
	public void startFusionSkill(L2Character target, L2Skill skill)
	{
		if (skill.getSkillType() != L2SkillType.FUSION)
			return;
		if (_fusionSkill == null)
			_fusionSkill = new FusionSkill(this, target, skill);
	}
	
	/**
	 * Kill the L2Character.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set target to null and cancel Attack or Cast</li>
	 * <li>Stop movement</li>
	 * <li>Stop HP/MP/CP Regeneration task</li>
	 * <li>Stop all active skills effects in progress on the L2Character</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform</li>
	 * <li>Notify L2Character AI</li><BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2NpcInstance : Create a DecayTask to remove the corpse of the L2NpcInstance after 7 seconds</li>
	 * <li>L2Attackable : Distribute rewards (EXP, SP, Drops...) and notify Quest Engine</li>
	 * <li>L2PcInstance : Apply Death Penalty, Manage gain/loss Karma and Item Drop</li><BR>
	 * <BR>
	 *
	 * @param killer
	 *            The L2Character who killed it
	 */
	public boolean doDie(L2Character killer)
	{
		// killing is only possible one time
		synchronized (this)
		{
			if (isDead())
				return false;
			// now reset currentHp to zero
			setCurrentHp(0);
			setIsDead(true);
		}
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		// Stop movement
		stopMove(null);
		// Stop HP/MP/CP Regeneration task
		getStatus().stopHpMpRegeneration();
		calculateRewards(killer);
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		broadcastStatusUpdate();
		// Notify L2Character AI
		if (getAI() != null)
			getAI().notifyEvent(CtrlEvent.EVT_DEAD);
		stopAllEffectsExceptThoseThatLastThroughDeath();
		if (getWorldRegion() != null)
			getWorldRegion().onDeath(this);
		getAttackByList().clear();
		try
		{
			if (_fusionSkill != null)
				abortCast();
			for (L2Character character : getKnownList().getKnownCharacters())
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();
			if (killer != null && killer.getActingPlayer() != null)
			{
				if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
				{
					AchievementBp.getInstance().onCreatureKill(killer, this);
				}
				// for (QuestState qs : killer.getActingPlayer().getNotifyQuestOfDeath())
				// {
				// if (qs != null)
				// qs.getQuest().notifyCreatureKill(killer, this, qs);
				// }
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		return true;
	}
	
	protected void calculateRewards(L2Character killer)
	{}
	
	/** Sets HP, MP and CP and revives the L2Character. */
	public void doRevive()
	{
		if (!isDead())
		{
			setIsPendingRevive(false);
			return;
		}
		if (!isTeleporting())
		{
			setIsPendingRevive(false);
			setIsDead(false);
			if (this instanceof L2Playable)
				((L2Playable) this).setIgnorePK(false);
			boolean restorefull = false;
			if (this instanceof L2Playable)
			{
				if (((L2Playable) this).isPhoenixBlessed())
				{
					restorefull = true;
					((L2Playable) this).stopPhoenixBlessing(null);
				}
				else if (getActingPlayer().isInClanwarWith(getActingPlayer().getLastPCKiller()))
					restorefull = true;
			}
			if (restorefull)
			{
				_status.setCurrentCp(getMaxCp()); // this is not confirmed...
				_status.setCurrentHp(getMaxHp()); // confirmed
				_status.setCurrentMp(getMaxMp()); // and also confirmed
			}
			else
			{
				_status.setCurrentCp(getMaxCp()); // this is not confirmed...
				_status.setCurrentHp(getMaxHp()); // confirmed
				_status.setCurrentMp(getMaxMp()); // and also confirmed
				// _status.setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);
				//
				// if (Config.RESPAWN_RESTORE_CP > 0)
				// {
				// _status.setCurrentCp(Math.max(getMaxCp() * Config.RESPAWN_RESTORE_CP, getCurrentCp()));
				// }
				//
				// if (Config.RESPAWN_RESTORE_MP > 0)
				// {
				// _status.setCurrentCp(getMaxCp()); //this is not confirmed...
				// _status.setCurrentHp(getMaxHp()); //confirmed
				// _status.setCurrentMp(getMaxMp()); //and also confirmed
				// }
			}
			// Start broadcast status
			broadcastPacket(new Revive(this));
			if (getWorldRegion() != null)
				getWorldRegion().onRevive(this);
		}
		else
			setIsPendingRevive(true);
	}
	
	/** Revives the L2Character using skill. */
	public void doRevive(double revivePower)
	{
		doRevive();
	}
	
	// =========================================================
	// Property - Public
	/**
	 * Return the L2CharacterAI of the L2Character and if its null create a new one.
	 */
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai; // copy handle
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new L2CharacterAI(new AIAccessor());
				return _ai;
			}
		}
		return ai;
	}
	
	public L2CharacterAI getAIWithOutInitializing()
	{
		return _ai;
	}
	
	public void setAI(L2CharacterAI newAI)
	{
		L2CharacterAI oldAI = getAI();
		if (oldAI != null && oldAI != newAI && oldAI instanceof L2AttackableAI)
			((L2AttackableAI) oldAI).stopAITask();
		_ai = newAI;
	}
	
	/** Return True if the L2Character has a L2CharacterAI. */
	public boolean hasAI()
	{
		return _ai != null;
	}
	
	/** Return True if the L2Character is RaidBoss or his minion. */
	public boolean isRaid()
	{
		return _isRaid;
	}
	
	/**
	 * Set this Npc as a Raid instance.<BR>
	 * <BR>
	 * 
	 * @param isRaid
	 */
	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}
	
	/** Return a list of L2Character that attacked. */
	public final List<L2Character> getAttackByList()
	{
		if (_attackByList == null)
			_attackByList = new FastList<L2Character>();
		return _attackByList;
	}
	
	public final List<L2Character> getRaidAttackerByList()
	{
		if (_raidDamageAttackers == null)
			_raidDamageAttackers = new FastList<L2Character>();
		return _raidDamageAttackers;
	}
	
	public final L2Skill getLastSimultaneousSkillCast()
	{
		return _lastSimultaneousSkillCast;
	}
	
	public void setLastSimultaneousSkillCast(L2Skill skill)
	{
		_lastSimultaneousSkillCast = skill;
	}
	
	public final L2Skill getLastSkillCast()
	{
		return _lastSkillCast;
	}
	
	public void setLastSkillCast(L2Skill skill)
	{
		_lastSkillCast = skill;
	}
	
	public final boolean isNoRndWalk()
	{
		return getRunSpeed() < 2 || _isNoRndWalk;
	}
	
	public final void setIsNoRndWalk(boolean value)
	{
		_isNoRndWalk = value;
	}
	
	public final boolean isAfraid()
	{
		return _isAfraid;
	}
	
	public final void setIsAfraid(boolean value)
	{
		_isAfraid = value;
	}
	
	/** Return True if the L2Character can't use its skills (ex : stun, sleep...). */
	public final boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || isImmobileUntilAttacked() || isStunned() || isSleeping() || isParalyzed() || isAlikeDead();
	}
	
	/** Return True if the L2Character can't attack (stun, sleep, attackEndTime, fakeDeath, paralyse, attackMute). */
	public boolean isAttackingDisabled()
	{
		return isFlying() || isStunned() || isImmobileUntilAttacked() || isSleeping() || getAttackEndTime() > GameTimeController.getGameTicks() || isAlikeDead() || isParalyzed() || isPhysicalAttackMuted() || isCoreAIDisabled();
	}
	
	public boolean isAttackingDisabledDecoy()
	{
		return isFlying() || isStunned() || isImmobileUntilAttacked() || isSleeping() || isAlikeDead() || isParalyzed() || isPhysicalAttackMuted();
	}
	
	public final Calculator[] getCalculators()
	{
		return _calculators;
	}
	
	public final boolean isConfused()
	{
		return _isConfused;
	}
	
	public final void setIsConfused(boolean value)
	{
		_isConfused = value;
	}
	
	/** Return True if the L2Character is dead or use fake death. */
	public boolean isAlikeDead()
	{
		return _isDead;
	}
	
	/** Return True if the L2Character is dead. */
	public final boolean isDead()
	{
		return _isDead;
	}
	
	public final void setIsDead(boolean value)
	{
		_isDead = value;
	}
	
	public boolean isImmobilized()
	{
		return _isImmobilized;
	}
	
	public void setIsImmobilized(boolean value)
	{
		_isImmobilized = value;
	}
	
	public final boolean isMuted()
	{
		return _isMuted;
	}
	
	public final void setIsMuted(boolean value)
	{
		_isMuted = value;
	}
	
	public final boolean isPhysicalMuted()
	{
		return _isPhysicalMuted;
	}
	
	public final void setIsPhysicalMuted(boolean value)
	{
		_isPhysicalMuted = value;
	}
	
	public final boolean isPhysicalAttackMuted()
	{
		return _isPhysicalAttackMuted;
	}
	
	public final void setIsPhysicalAttackMuted(boolean value)
	{
		_isPhysicalAttackMuted = value;
	}
	
	/** Return True if the L2Character can't move (stun, root, sleep, overload, paralyzed). */
	public boolean isMovementDisabled()
	{
		// check for isTeleporting to prevent teleport cheating (if appear packet not received)
		return isStunned() || isRooted() || isSleeping() /* || isOverloaded() */ || isParalyzed() || isImmobilized() || isAlikeDead() || isTeleporting();
	}
	
	public boolean isMovementDisabledForKnockback()
	{
		return isRooted() || isInvul() || isAlikeDead() || isTeleporting();
	}
	
	/** Return True if the L2Character can not be controlled by the player (confused, afraid). */
	public final boolean isOutOfControl()
	{
		return isConfused() || isAfraid() || isBluffed() || getKnockedbackTimer() >= GameTimeController.getGameTicks();
	}
	
	/*
	 * public final boolean isOverloaded() { return _isOverloaded; }
	 *//** Set the overloaded status of the L2Character is overloaded (if True, the L2PcInstance can't take more item). *//*
																															 * public final void setIsOverloaded(boolean value) { _isOverloaded = value; }
																															 */
	public final boolean isParalyzed()
	{
		return _isParalyzed;
	}
	
	public final void setIsParalyzed(boolean value)
	{
		_isParalyzed = value;
	}
	
	public final boolean isSubbing()
	{
		return _isSubbing;
	}
	
	public final void setIsSubbing(boolean value)
	{
		_isSubbing = value;
	}
	
	public final boolean isPendingRevive()
	{
		return isDead() && _isPendingRevive;
	}
	
	public final void setIsPendingRevive(boolean value)
	{
		if (value)
			if (!isDead())
				value = false;
		_isPendingRevive = value;
	}
	
	public final boolean isDisarmed()
	{
		return _isDisarmed;
	}
	
	public final void setIsDisarmed(boolean value)
	{
		_isDisarmed = value;
	}
	
	/**
	 * Return the L2Summon of the L2Character.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	public L2Summon getPet()
	{
		return null;
	}
	
	public final boolean isRooted()
	{
		return _isRooted;
	}
	
	public final void setIsRooted(boolean value)
	{
		_isRooted = value;
	}
	
	public final boolean isBluffed()
	{
		return _isBluffed;
	}
	
	public final void setIsBluffed(boolean value)
	{
		_isBluffed = value;
	}
	
	/** Return True if the L2Character is running. */
	public boolean isRunning()
	{
		return _isRunning;
	}
	
	public final void setIsRunning(boolean value)
	{
		_isRunning = value;
		if (getRunSpeed() != 0)
			broadcastPacket(new ChangeMoveType(this));
		if (this instanceof L2PcInstance)
			((L2PcInstance) this).broadcastUserInfo();
		else if (this instanceof L2Summon)
		{
			((L2Summon) this).broadcastStatusUpdate();
		}
		else if (this instanceof L2Npc)
		{
			Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
			// synchronized (character.getKnownList().getKnownPlayers())
			{
				for (L2PcInstance player : plrs)
				{
					if (getRunSpeed() == 0)
						player.sendPacket(new ServerObjectInfo((L2Npc) this, player));
					else
						player.sendPacket(new AbstractNpcInfo.NpcInfo((L2Npc) this, player));
				}
			}
		}
	}
	
	/** Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance. */
	public final void setRunning()
	{
		if (!isRunning())
			setIsRunning(true);
	}
	
	public final boolean isImmobileUntilAttacked()
	{
		return _isImmobileUntilAttacked;
	}
	
	public final void setIsImmobileUntilAttacked(boolean value)
	{
		_isImmobileUntilAttacked = value;
	}
	
	public final boolean isSleeping()
	{
		return _isSleeping;
	}
	
	public final void setIsSleeping(boolean value)
	{
		_isSleeping = value;
	}
	
	public final boolean isStunned()
	{
		return _isStunned;
	}
	
	public final void setIsStunned(boolean value)
	{
		_isStunned = value;
	}
	
	public final boolean isBetrayed()
	{
		return _isBetrayed;
	}
	
	public final void setIsBetrayed(boolean value)
	{
		_isBetrayed = value;
	}
	
	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}
	
	public void setIsTeleporting(boolean value)
	{
		_isTeleporting = value;
	}
	
	public void setIsInvul(boolean b)
	{
		_isInvul = b;
	}
	
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting;
	}
	
	public boolean isUndead()
	{
		return _template.isUndead;
	}
	
	public final boolean isFlying()
	{
		return _isFlying;
	}
	
	public final void setIsFlying(boolean mode)
	{
		_isFlying = mode;
	}
	
	@Override
	public CharKnownList getKnownList()
	{
		return ((CharKnownList) super.getKnownList());
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new CharKnownList(this));
	}
	
	public CharStat getStat()
	{
		return _stat;
	}
	
	/**
	 * Initializes the CharStat class of the L2Object,
	 * is overwritten in classes that require a different CharStat Type.
	 * Removes the need for instanceof checks.
	 */
	public void initCharStat()
	{
		_stat = new CharStat(this);
	}
	
	public final void setStat(CharStat value)
	{
		_stat = value;
	}
	
	public CharStatus getStatus()
	{
		return _status;
	}
	
	/**
	 * Initializes the CharStatus class of the L2Object,
	 * is overwritten in classes that require a different CharStatus Type.
	 * Removes the need for instanceof checks.
	 */
	public void initCharStatus()
	{
		_status = new CharStatus(this);
	}
	
	public final void setStatus(CharStatus value)
	{
		_status = value;
	}
	
	@Override
	public CharPosition getPosition()
	{
		return (CharPosition) super.getPosition();
	}
	
	@Override
	public void initPosition()
	{
		setObjectPosition(new CharPosition(this));
	}
	
	public L2CharTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Set the template of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...).
	 * All of those properties are stored in a different template for each type of L2Character.
	 * Each template is loaded once in the server cache memory (reduce memory use).
	 * When a new instance of L2Character is spawned, server just create a link between the instance and the template
	 * This link is stored in <B>_template</B><BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li>this instanceof L2Character</li><BR>
	 * <BR
	 */
	protected final void setTemplate(L2CharTemplate template)
	{
		_template = template;
	}
	
	/** Return the Title of the L2Character. */
	public final String getTitle()
	{
		return _title;
	}
	
	/** Set the Title of the L2Character. */
	public void setTitle(String value)
	{
		setTitle(value, false);
	}
	
	public void setTitle(String value, boolean updateDB)
	{
		if (value == null)
			_title = "";
		else
			_title = value.length() > 20 ? value.substring(0, 19) : value;
		if (updateDB && this instanceof L2PcInstance)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE characters SET title=? WHERE charId=?");
				statement.setString(1, getTitle());
				statement.setInt(2, getObjectId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Could not store char title: " + e);
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
	
	/** Set the L2Character movement type to walk and send Server->Client packet ChangeMoveType to all others L2PcInstance. */
	public final void setWalking()
	{
		if (isRunning())
		{
			if (isAPC())
				return;
			setIsRunning(false);
		}
	}
	
	/** Task lauching the function enableSkill() */
	class EnableSkill implements Runnable
	{
		int _skillId;
		
		public EnableSkill(int skillId)
		{
			_skillId = skillId;
		}
		
		public void run()
		{
			try
			{
				enableSkill(_skillId);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed executing enableSkill().", e);
			}
		}
	}
	
	class shotDisplayTask implements Runnable
	{
		L2Character	chara;
		int			_skillId;
		boolean		_all;
		
		public shotDisplayTask(L2Character cha, int id, boolean all)
		{
			chara = cha;
			_skillId = id;
			_all = all;
		}
		
		public void run()
		{
			if (_all)
				broadcastPacket(new MagicSkillUse(chara, _skillId, 1, 0, 0)); // broadcast soulshot charge animation for tard grs
			else if (chara instanceof L2PcInstance)
				sendPacket(new MagicSkillUse(chara, _skillId, 1, 0, 0)); // broadcast soulshot charge animation for tard grs
		}
	}
	
	/**
	 * Task lauching the function onHitTimer().<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)</li>
	 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance</li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
	 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li><BR>
	 * <BR>
	 */
	class HitTask implements Runnable
	{
		L2Character		_hitTarget;
		int				_damage;
		boolean			_crit;
		boolean			_miss;
		byte			_shld;
		boolean			_soulshot;
		final boolean	_piercing;
		final byte		_absNerf;
		boolean			_playSound;
		
		public HitTask(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld, byte absNerf)
		{
			_hitTarget = target;
			_damage = damage;
			_crit = crit;
			_shld = shld;
			_miss = miss;
			_soulshot = soulshot;
			_piercing = false;
			_absNerf = absNerf;
			_playSound = false;
		}
		
		public HitTask(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld, final boolean pierced)
		{
			_hitTarget = target;
			_damage = damage;
			_crit = crit;
			_shld = shld;
			_miss = miss;
			_soulshot = soulshot;
			_piercing = pierced;
			_absNerf = 1;
			_playSound = false;
		}
		
		public HitTask(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld, byte absNerf, boolean playSound)
		{
			_hitTarget = target;
			_damage = damage;
			_crit = crit;
			_shld = shld;
			_miss = miss;
			_soulshot = soulshot;
			_piercing = false;
			_absNerf = absNerf;
			_playSound = playSound;
		}
		
		public void run()
		{
			try
			{
				onHitTimer(_hitTarget, _damage, _crit, _miss, _soulshot, _shld, _piercing, _absNerf, _playSound);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed executing HitTask.", e);
			}
		}
	}
	
	/** Task lauching the magic skill phases */
	class MagicUseTask implements Runnable
	{
		L2Object[]	_targets;
		L2Skill		_skill;
		int			_count;
		int			_hitTime;
		int			_coolTime;
		int			_phase;
		boolean		_simultaneously;
		int			_shots;
		int			x, y, z;
		
		public MagicUseTask(L2Object[] targets, L2Skill skill, int hitTime, int coolTime, boolean simultaneously, int shot)
		{
			_targets = targets;
			_skill = skill;
			_hitTime = hitTime;
			_coolTime = coolTime;
			_phase = 1;
			_simultaneously = simultaneously;
			_count = 0;
			_shots = shot;
			if (_skill.getTargetType(L2Character.this) == SkillTargetType.TARGET_AURA || _skill.getTargetType(L2Character.this) == SkillTargetType.TARGET_FRONT_AURA || _skill.getTargetType(L2Character.this) == SkillTargetType.TARGET_BEHIND_AURA)
			{
				x = getX();
				y = getY();
				z = getZ();
			}
			else
			{
				x = targets[0].getX();
				y = targets[0].getY();
				z = targets[0].getZ();
			}
		}
		
		public void run()
		{
			try
			{
				switch (_phase)
				{
					case 1:
						onMagicLaunchedTimer(this);
						break;
					case 2:
						onMagicHitTimer(this);
						break;
					case 3:
						onMagicFinalizer(this);
						break;
					default:
						break;
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed executing MagicUseTask.", e);
				if (_simultaneously)
					setIsCastingSimultaneouslyNow(false);
				else
					setIsCastingNow(false);
			}
		}
	}
	
	/** Task launching the function useMagic() */
	class QueuedMagicUseTask implements Runnable
	{
		L2PcInstance	_currPlayer;
		L2Skill			_queuedSkill;
		boolean			_isCtrlPressed;
		boolean			_isShiftPressed;
		
		public QueuedMagicUseTask(L2PcInstance currPlayer, L2Skill queuedSkill, boolean isCtrlPressed, boolean isShiftPressed)
		{
			_currPlayer = currPlayer;
			_queuedSkill = queuedSkill;
			_isCtrlPressed = isCtrlPressed;
			_isShiftPressed = isShiftPressed;
		}
		
		public void run()
		{
			try
			{
				_currPlayer.useMagic(_queuedSkill, _isCtrlPressed, _isShiftPressed);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed executing QueuedMagicUseTask.", e);
			}
		}
	}
	
	/** Task of AI notification */
	public class NotifyAITask implements Runnable
	{
		private final CtrlEvent _evt;
		
		NotifyAITask(CtrlEvent evt)
		{
			_evt = evt;
		}
		
		public void run()
		{
			try
			{
				getAI().notifyEvent(_evt, null);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "", e);
			}
		}
	}
	// =========================================================
	
	// =========================================================
	// Abnormal Effect - NEED TO REMOVE ONCE L2CHARABNORMALEFFECT IS COMPLETE
	// Data Field
	/** Map 32 bits (0x0000) containing all abnormal effect in progress */
	private int					_AbnormalEffects;
	private int					_SpecialEffects;
	protected CharEffectList	_effects	= new CharEffectList(this);
	
	// Method - Public
	/**
	 * Launch and add L2Effect (including Stack Group management) to L2Character and update client magic icon.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>.
	 * The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * Several same effect can't be used on a L2Character at the same time.
	 * Indeed, effects are not stackable and the last cast will replace the previous in progress.
	 * More, some effects belong to the same Stack Group (ex WindWald and Haste Potion).
	 * If 2 effects of a same group are used at the same time on a L2Character, only the more efficient (identified by its priority order) will be preserve.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Add the L2Effect to the L2Character _effects</li>
	 * <li>If this effect doesn't belong to a Stack Group, add its Funcs to the Calculator set of the L2Character (remove the old one if necessary)</li>
	 * <li>If this effect has higher priority in its Stack Group, add its Funcs to the Calculator set of the L2Character (remove previous stacked effect Funcs if necessary)</li>
	 * <li>If this effect has NOT higher priority in its Stack Group, set the effect to Not In Use</li>
	 * <li>Update active skills in progress icons on player client</li><BR>
	 */
	public void addEffect(L2Effect newEffect)
	{
		_effects.queueEffect(newEffect, false);
		if (this instanceof L2PcInstance && newEffect.getSkill() != null)
		{
			// TODO UpdateLunaDetailStats
			// getActingPlayer().sendPacket(new UpdateLunaDetailStats("test", getActingPlayer()));
		}
	}
	
	/**
	 * Stop and remove L2Effect (including Stack Group management) from L2Character and update client magic icon.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>.
	 * The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * Several same effect can't be used on a L2Character at the same time.
	 * Indeed, effects are not stackable and the last cast will replace the previous in progress.
	 * More, some effects belong to the same Stack Group (ex WindWald and Haste Potion).
	 * If 2 effects of a same group are used at the same time on a L2Character, only the more efficient (identified by its priority order) will be preserve.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove Func added by this effect from the L2Character Calculator (Stop L2Effect)</li>
	 * <li>If the L2Effect belongs to a not empty Stack Group, replace theses Funcs by next stacked effect Funcs</li>
	 * <li>Remove the L2Effect from _effects of the L2Character</li>
	 * <li>Update active skills in progress icons on player client</li><BR>
	 */
	public final void removeEffect(L2Effect effect)
	{
		_effects.queueEffect(effect, true);
		if (this instanceof L2PcInstance && effect.getSkill() != null)
		{ // TODO UpdateLunaDetailStats
			// getActingPlayer().sendPacket(new UpdateLunaDetailStats("test", getActingPlayer()));
		}
	}
	
	/**
	 * Active abnormal effects flags in the binary mask and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startAbnormalEffect(AbnormalEffect mask)
	{
		_AbnormalEffects |= mask.getMask();
		updateAbnormalEffect();
	}
	
	public final void startAbnormalEffect(int mask)
	{
		_AbnormalEffects |= mask;
		updateAbnormalEffect();
	}
	
	/**
	 * Active special effects flags in the binary mask and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startSpecialEffect(AbnormalEffect mask)
	{
		_SpecialEffects |= mask.getMask();
		updateAbnormalEffect();
	}
	
	public final void startSpecialEffect(int mask)
	{
		_SpecialEffects |= mask;
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Confused flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startConfused()
	{
		setIsConfused(true);
		getAI().notifyEvent(CtrlEvent.EVT_CONFUSED);
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Fake Death flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startFakeDeath()
	{
		if (!(this instanceof L2PcInstance))
			return;
		((L2PcInstance) this).setIsFakeDeath(true);
		/* Aborts any attacks/casts if fake dead */
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
	}
	
	/**
	 * Active the abnormal effect Fear flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startFear()
	{
		setIsAfraid(true);
		/* getAI().notifyEvent(CtrlEvent.EVT_AFRAID); */
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Muted flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startMuted()
	{
		setIsMuted(true);
		/* Aborts any casts if muted */
		abortCast();
		getAI().notifyEvent(CtrlEvent.EVT_MUTED);
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Psychical_Muted flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startPhysicalMuted()
	{
		setIsPhysicalMuted(true);
		getAI().notifyEvent(CtrlEvent.EVT_MUTED);
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Root flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startRooted()
	{
		setIsRooted(true);
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_ROOTED);
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Sleep flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startImmobileUntilAttacked()
	{
		setIsImmobileUntilAttacked(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING);
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Sleep flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startSleeping()
	{
		setIsSleeping(true);
		/* Aborts any attacks/casts if sleeped */
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING);
		updateAbnormalEffect();
	}
	
	/**
	 * Launch a Stun Abnormal Effect on the L2Character.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate the success rate of the Stun Abnormal Effect on this L2Character</li>
	 * <li>If Stun succeed, active the abnormal effect Stun flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet</li>
	 * <li>If Stun NOT succeed, send a system message Failed to the L2PcInstance attacker</li><BR>
	 * <BR>
	 */
	public final void startStunning()
	{
		setIsStunned(true);
		/* Aborts any attacks/casts if stunned */
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_STUNNED);
		updateAbnormalEffect();
	}
	
	public final void startParalyze()
	{
		setIsParalyzed(true);
		/* Aborts any attacks/casts if paralyzed */
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		getAI().notifyEvent(CtrlEvent.EVT_PARALYZED);
		updateAbnormalEffect();
	}
	
	public final void startBetray()
	{
		setIsBetrayed(true);
		getAI().notifyEvent(CtrlEvent.EVT_BETRAYED);
		updateAbnormalEffect();
	}
	
	public final void stopBetray()
	{
		stopEffects(L2EffectType.BETRAY);
		setIsBetrayed(false);
		updateAbnormalEffect();
	}
	
	/**
	 * Modify the abnormal effect map according to the mask.<BR>
	 * <BR>
	 */
	public final void stopAbnormalEffect(AbnormalEffect mask)
	{
		/*
		 * int counter = 0;
		 * for (L2Effect effect : getAllEffects())
		 * {
		 * if (effect == null) continue;
		 * if (effect.getEffectTemplate().abnormalEffect == mask)
		 * counter++;
		 * if (counter >= 2)
		 * return;
		 * }
		 */
		_AbnormalEffects &= ~mask.getMask();
		updateAbnormalEffect();
	}
	
	/**
	 * Modify the special effect map according to the mask.<BR>
	 * <BR>
	 */
	public final void stopSpecialEffect(AbnormalEffect mask)
	{
		/*
		 * int counter = 0;
		 * for (L2Effect effect : getAllEffects())
		 * {
		 * if (effect == null) continue;
		 * if (effect.getEffectTemplate().specialEffect == mask)
		 * counter++;
		 * if (counter >= 2)
		 * return;
		 * }
		 */
		_SpecialEffects &= ~mask.getMask();
		updateAbnormalEffect();
	}
	
	public final void stopAbnormalEffect(int mask)
	{
		_AbnormalEffects &= ~mask;
		updateAbnormalEffect();
	}
	
	public final void stopSpecialEffect(int mask)
	{
		_SpecialEffects &= ~mask;
		updateAbnormalEffect();
	}
	
	/**
	 * Stop all active skills effects in progress on the L2Character.<BR>
	 * <BR>
	 */
	public void stopAllEffects()
	{
		_effects.stopAllEffects();
	}
	
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		_effects.stopAllEffectsExceptThoseThatLastThroughDeath();
	}
	
	/**
	 * Stop a specified/all Confused abnormal L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete a specified/all (if effect=null) Confused abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _confused to False</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li><BR>
	 * <BR>
	 */
	public final void stopConfused(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.CONFUSION);
		else
			removeEffect(effect);
		setIsConfused(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	/**
	 * Stop and remove the L2Effects corresponding to the L2Skill Identifier and update client magic icon.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>.
	 * The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 *
	 * @param effectId
	 *            The L2Skill Identifier of the L2Effect to remove from _effects
	 */
	public final void stopSkillEffects(int skillId)
	{
		_effects.stopSkillEffects(skillId);
	}
	
	/**
	 * Stop and remove all L2Effect of the selected type (ex : BUFF, DMG_OVER_TIME...) from the L2Character and update client magic icon.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>.
	 * The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove Func added by this effect from the L2Character Calculator (Stop L2Effect)</li>
	 * <li>Remove the L2Effect from _effects of the L2Character</li>
	 * <li>Update active skills in progress icons on player client</li><BR>
	 * <BR>
	 *
	 * @param type
	 *            The type of effect to stop ((ex : BUFF, DMG_OVER_TIME...)
	 */
	public final void stopEffects(L2EffectType type)
	{
		_effects.stopEffects(type);
	}
	
	/**
	 * Stop a specified/all Fake Death abnormal L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete a specified/all (if effect=null) Fake Death abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _fake_death to False</li>
	 * <li>Notify the L2Character AI</li><BR>
	 * <BR>
	 */
	public final void stopFakeDeath(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.FAKE_DEATH);
		else
			removeEffect(effect);
		// if this is a player instance, start the grace period for this character (grace from mobs only)!
		if (this instanceof L2PcInstance)
		{
			((L2PcInstance) this).setIsFakeDeath(false);
			((L2PcInstance) this).setRecentFakeDeath(true);
		}
		ChangeWaitType revive = new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH);
		broadcastPacket(revive);
		// TODO: Temp hack: players see FD on ppl that are moving: Teleport to someone who uses FD - if he gets up he will fall down again for that client -
		// even tho he is actually standing... Probably bad info in CharInfo packet?
		broadcastPacket(new Revive(this));
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
	}
	
	/**
	 * Stop a specified/all Fear abnormal L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete a specified/all (if effect=null) Fear abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _affraid to False</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li><BR>
	 * <BR>
	 */
	public final void stopFear(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.FEAR);
		else
			removeEffect(effect);
		setIsAfraid(false);
		updateAbnormalEffect();
	}
	
	/**
	 * Stop a specified/all Muted abnormal L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete a specified/all (if effect=null) Muted abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _muted to False</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li><BR>
	 * <BR>
	 */
	public final void stopMuted(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.MUTE);
		else
			removeEffect(effect);
		setIsMuted(false);
		updateAbnormalEffect();
	}
	
	public final void stopPsychicalMuted(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PHYSICAL_MUTE);
		else
			removeEffect(effect);
		setIsPhysicalMuted(false);
		updateAbnormalEffect();
	}
	
	/**
	 * Stop a specified/all Root abnormal L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete a specified/all (if effect=null) Root abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _rooted to False</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li><BR>
	 * <BR>
	 */
	public final void stopRooting(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.ROOT);
		else
			removeEffect(effect);
		setIsRooted(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	/**
	 * Stop immobilization until attacked abnormal L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete a specified/all (if effect=null) immobilization until attacked abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _muted to False</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li><BR>
	 * <BR>
	 */
	public final void stopImmobileUntilAttacked(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.IMMOBILEUNTILATTACKED);
		else
		{
			removeEffect(effect);
			if (effect.getSkill().getNegateId().length != 0)
			{
				for (int i = 0; i < effect.getSkill().getNegateId().length; i++)
				{
					if (effect.getSkill().getNegateId()[i] != 0)
						stopSkillEffects(effect.getSkill().getNegateId()[i]);
				}
			}
		}
		setIsImmobileUntilAttacked(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	/**
	 * Stop a specified/all Sleep abnormal L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete a specified/all (if effect=null) Sleep abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _sleeping to False</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li><BR>
	 * <BR>
	 */
	public final void stopSleeping(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.SLEEP);
		else
			removeEffect(effect);
		setIsSleeping(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	/**
	 * Stop a specified/all Stun abnormal L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete a specified/all (if effect=null) Stun abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _stuned to False</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li><BR>
	 * <BR>
	 */
	public final void stopStunning(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.STUN);
		else
			removeEffect(effect);
		setIsStunned(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	public final void stopParalyze(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PARALYZE);
		else
			removeEffect(effect);
		setIsParalyzed(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	/**
	 * Stop L2Effect: Transformation<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove Transformation Effect</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li><BR>
	 * <BR>
	 */
	public final void stopTransformation(L2Effect effect)
	{
		if (effect == null)
		{
			stopEffects(L2EffectType.TRANSFORMATION);
		}
		else
		{
			removeEffect(effect);
			// if this is a player instance, then untransform, also set the transform_id column equal to 0 if not cursed.
			if (this instanceof L2PcInstance)
			{
				/*
				 * if (((L2PcInstance) this).getTransformation() != null)
				 * {
				 */
				((L2PcInstance) this).untransform();
				/* } */
			}
			else if (this instanceof L2Attackable)
				getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
	}
	
	/**
	 * Not Implemented.<BR>
	 * <BR>
	 * <B><U> Overridden in</U> :</B><BR>
	 * <BR>
	 * <li>L2NPCInstance</li>
	 * <li>L2PcInstance</li>
	 * <li>L2Summon</li>
	 * <li>L2DoorInstance</li><BR>
	 * <BR>
	 */
	public abstract void updateAbnormalEffect();
	
	/**
	 * Update active skills in progress (In Use and Not In Use because stacked) icons on client.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress (In Use and Not In Use because stacked) are represented by an icon on the client.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method ONLY UPDATE the client of the player and not clients of all players in the party.</B></FONT><BR>
	 * <BR>
	 */
	public final void updateEffectIcons()
	{
		updateEffectIcons(false);
	}
	
	/**
	 * Updates Effect Icons for this character(palyer/summon) and his party if any<BR>
	 * Overridden in:<BR>
	 * L2PcInstance<BR>
	 * L2Summon<BR>
	 * 
	 * @param partyOnly
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		// overridden
	}
	
	// Property - Public
	/**
	 * Return a map of 16 bits (0x0000) containing all abnormal effect in progress for this L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * In Server->Client packet, each effect is represented by 1 bit of the map (ex : BLEEDING = 0x0001 (bit 1), SLEEP = 0x0080 (bit 8)...).
	 * The map is calculated by applying a BINARY OR operation on each effect.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Server Packet : CharInfo, NpcInfo, NpcInfoPoly, UserInfo...</li><BR>
	 * <BR>
	 */
	public int getAbnormalEffect()
	{
		int ae = _AbnormalEffects;
		if (!isFlying() && isStunned())
			ae |= AbnormalEffect.STUN.getMask();
		if (!isFlying() && isRooted())
			ae |= AbnormalEffect.ROOT.getMask();
		if (isSleeping())
			ae |= AbnormalEffect.SLEEP.getMask();
		if (isConfused())
			ae |= AbnormalEffect.CONFUSED.getMask();
		if (isMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (isPhysicalMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (this instanceof L2PcInstance && getActingPlayer().isCool())
			ae |= AbnormalEffect.VITALITY.getMask();
		return ae;
	}
	
	public int getAbnormalEffectDecoy()
	{
		int ae = 0;
		if (!isFlying() && isStunned())
			ae |= AbnormalEffect.STUN.getMask();
		if (!isFlying() && isRooted())
			ae |= AbnormalEffect.ROOT.getMask();
		if (isSleeping())
			ae |= AbnormalEffect.SLEEP.getMask();
		if (isConfused())
			ae |= AbnormalEffect.CONFUSED.getMask();
		if (isMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (isPhysicalMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (isBleeding())
			ae |= AbnormalEffect.BLEEDING.getMask();
		if (isPoisoned())
			ae |= AbnormalEffect.POISON.getMask();
		if (this instanceof L2PcInstance && getActingPlayer().isCool())
			ae |= AbnormalEffect.VITALITY.getMask();
		return ae;
	}
	
	public int getAbnormalEffectAPC(FakePc pc)
	{
		int ae = _AbnormalEffects;
		if (!isFlying() && isStunned())
			ae |= AbnormalEffect.STUN.getMask();
		if (!isFlying() && isRooted())
			ae |= AbnormalEffect.ROOT.getMask();
		if (isSleeping())
			ae |= AbnormalEffect.SLEEP.getMask();
		if (isConfused())
			ae |= AbnormalEffect.CONFUSED.getMask();
		if (isMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (isPhysicalMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		ae |= Integer.decode("0x" + pc.abnormal);
		return ae;
	}
	
	/**
	 * Return a map of 32 bits (0x00000000) containing all special effect in progress for this L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * In Server->Client packet, each effect is represented by 1 bit of the map (ex : INVULNERABLE = 0x0001 (bit 1), PINK_AFFRO = 0x0020 (bit 6)...).
	 * The map is calculated by applying a BINARY OR operation on each effect.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Server Packet : CharInfo, UserInfo...</li><BR>
	 * <BR>
	 */
	public int getSpecialEffect()
	{
		int se = _SpecialEffects;
		if (isFlying() && isStunned())
			se |= AbnormalEffect.S_AIR_STUN.getMask();
		if (isFlying() && isRooted())
			se |= AbnormalEffect.S_AIR_ROOT.getMask();
		return se;
	}
	
	public int getSpecialEffectAPC(FakePc pc)
	{
		int se = _SpecialEffects;
		if (isFlying() && isStunned())
			se |= AbnormalEffect.S_AIR_STUN.getMask();
		if (isFlying() && isRooted())
			se |= AbnormalEffect.S_AIR_ROOT.getMask();
		se |= Integer.decode("0x" + pc.special);
		return se;
	}
	
	/**
	 * Return all active skills effects in progress on the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in <B>_effects</B>.
	 * The Integer key of _effects is the L2Skill Identifier that has created the effect.<BR>
	 * <BR>
	 *
	 * @return A table containing all active skills effect in progress on the L2Character
	 */
	public final L2Effect[] getAllEffects()
	{
		return _effects.getAllEffects();
	}
	
	/**
	 * Return L2Effect in progress on the L2Character corresponding to the L2Skill Identifier.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in <B>_effects</B>.
	 *
	 * @param index
	 *            The L2Skill Identifier of the L2Effect to return from the _effects
	 * @return The L2Effect corresponding to the L2Skill Identifier
	 */
	public final L2Effect getFirstEffect(int index)
	{
		return _effects.getFirstEffect(index);
	}
	
	/**
	 * Return the first L2Effect in progress on the L2Character created by the L2Skill.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in <B>_effects</B>.
	 *
	 * @param skill
	 *            The L2Skill whose effect must be returned
	 * @return The first L2Effect created by the L2Skill
	 */
	public final L2Effect getFirstEffect(L2Skill skill)
	{
		return _effects.getFirstEffect(skill);
	}
	
	public final L2Effect getFirstEffectById(int id)
	{
		return _effects.getFirstEffect(id);
	}
	
	/**
	 * Return the first L2Effect in progress on the L2Character corresponding to the Effect Type (ex : BUFF, STUN, ROOT...).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in <B>_effects</B>.
	 *
	 * @param tp
	 *            The Effect Type of skills whose effect must be returned
	 * @return The first L2Effect corresponding to the Effect Type
	 */
	public final L2Effect getFirstEffect(L2EffectType tp)
	{
		return _effects.getFirstEffect(tp);
	}
	
	// =========================================================
	// =========================================================
	// NEED TO ORGANIZE AND MOVE TO PROPER PLACE
	/** This class permit to the L2Character AI to obtain informations and uses L2Character method */
	public class AIAccessor
	{
		public AIAccessor()
		{}
		
		/**
		 * Return the L2Character managed by this Accessor AI.<BR>
		 * <BR>
		 */
		public L2Character getActor()
		{
			return L2Character.this;
		}
		
		/**
		 * Accessor to L2Character moveToLocation() method with an interaction area.<BR>
		 * <BR>
		 */
		public void moveTo(int x, int y, int z, int offset)
		{
			moveToLocation(x, y, z, offset);
		}
		
		/**
		 * Accessor to L2Character moveToLocation() method without interaction area.<BR>
		 * <BR>
		 */
		public void moveTo(int x, int y, int z)
		{
			moveToLocation(x, y, z, 0);
		}
		
		/**
		 * Accessor to L2Character stopMove() method.<BR>
		 * <BR>
		 */
		public void stopMove(L2CharPosition pos)
		{
			L2Character.this.stopMove(pos);
		}
		
		/**
		 * Accessor to L2Character doAttack() method.<BR>
		 * <BR>
		 */
		public void doAttack(L2Character target)
		{
			L2Character.this.doAttack(target);
		}
		
		/**
		 * Accessor to L2Character doCast() method.<BR>
		 * <BR>
		 */
		public void doCast(L2Skill skill)
		{
			L2Character.this.doCast(skill);
		}
		
		/**
		 * Create a NotifyAITask.<BR>
		 * <BR>
		 */
		public NotifyAITask newNotifyTask(CtrlEvent evt)
		{
			return new NotifyAITask(evt);
		}
		
		/**
		 * Cancel the AI.<BR>
		 * <BR>
		 */
		public void detachAI()
		{
			_ai = null;
		}
	}
	
	/**
	 * This class group all mouvement data.<BR>
	 * <BR>
	 * <B><U> Data</U> :</B><BR>
	 * <BR>
	 * <li>_moveTimestamp : Last time position update</li>
	 * <li>_xDestination, _yDestination, _zDestination : Position of the destination</li>
	 * <li>_xMoveFrom, _yMoveFrom, _zMoveFrom : Position of the origin</li>
	 * <li>_moveStartTime : Start time of the movement</li>
	 * <li>_ticksToMove : Nb of ticks between the start and the destination</li>
	 * <li>_xSpeedTicks, _ySpeedTicks : Speed in unit/ticks</li><BR>
	 * <BR>
	 */
	public static class MoveData
	{
		// when we retrieve x/y/z we use GameTimeControl.getGameTicks()
		// if we are moving, but move timestamp==gameticks, we don't need
		// to recalculate position
		public int						_moveStartTime;
		public int						_moveTimestamp;		// last update
		public int						_xDestination;
		public int						_yDestination;
		public int						_zDestination;
		public double					_xAccurate;			// otherwise there would be rounding errors
		public double					_yAccurate;
		public double					_zAccurate;
		public int						_heading;
		public boolean					disregardingGeodata;
		public int						onGeodataPathIndex;
		public List<AbstractNodeLoc>	geoPath;
		public int						geoPathAccurateTx;
		public int						geoPathAccurateTy;
		public int						geoPathGtx;
		public int						geoPathGty;
	}
	
	/** Table containing all skillId that are disabled */
	protected List<Integer>				_disabledSkills;
	private boolean						_allSkillsDisabled;
	// private int _flyingRunSpeed;
	// private int _floatingWalkSpeed;
	// private int _flyingWalkSpeed;
	// private int _floatingRunSpeed;
	/** Movement data of this L2Character */
	protected MoveData					_move;
	/** Orientation of the L2Character */
	private int							_heading;
	/** L2Charcater targeted by the L2Character */
	private L2Object					_target;
	// set by the start of attack, in game ticks
	private int							_attackEndTime;
	private boolean						_attacking;
	private int							_disableBowAttackEndTime;
	/* private int _disableCrossBowAttackEndTime; */
	private int							_castInterruptTime;
	/** Table of calculators containing all standard NPC calculator (ex : ACCURACY_COMBAT, EVASION_RATE */
	private static final Calculator[]	NPC_STD_CALCULATOR;
	private static final int[]			CERTI_SKILLS	= new int[]
	{
		35201, 35205, 35203, 35207, 35209, 35211, 35213, 35215, 35217, 35219, 35221, 35223, 35225, 35227, 35229, 35231, 35233, 35235, 35237, 35239, 35241, 35243, 35245, 35247, 35249, 35251, 35253, 35255, 35257, 35259, 35261, 35263, 35265
	};
	static
	{
		NPC_STD_CALCULATOR = Formulas.getStdNPCCalculators();
	}
	protected L2CharacterAI	_ai;
	/** Future Skill Cast */
	protected Future<?>		_skillCast;
	protected Future<?>		_skillCast2;
	/** Char Coords from Client */
	private int				_clientX;
	private int				_clientY;
	private int				_clientZ;
	private int				_clientHeading;
	
	/**
	 * Add a Func to the Calculator set of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.
	 * Each Calculator (a calculator per state) own a table of Func object.
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).
	 * To reduce cache memory use, L2NPCInstances who don't have skills share the same Calculator set called <B>NPC_STD_CALCULATOR</B>.<BR>
	 * <BR>
	 * That's why, if a L2NPCInstance is under a skill/spell effect that modify one of its state, a copy of the NPC_STD_CALCULATOR
	 * must be create in its _calculators before addind new Func object.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If _calculators is linked to NPC_STD_CALCULATOR, create a copy of NPC_STD_CALCULATOR in _calculators</li>
	 * <li>Add the Func object to _calculators</li><BR>
	 * <BR>
	 *
	 * @param f
	 *            The Func object to add to the Calculator corresponding to the state affected
	 */
	public final void addStatFunc(Func f)
	{
		if (f == null)
			return;
		synchronized (_calculators)
		{
			// Check if Calculator set is linked to the standard Calculator set of NPC
			if (_calculators == NPC_STD_CALCULATOR)
			{
				// Create a copy of the standard NPC Calculator set
				_calculators = new Calculator[Stats.NUM_STATS];
				for (int i = 0; i < Stats.NUM_STATS; i++)
				{
					if (NPC_STD_CALCULATOR[i] != null)
						_calculators[i] = new Calculator(NPC_STD_CALCULATOR[i]);
				}
			}
			// Select the Calculator of the affected state in the Calculator set
			int stat = f.stat.ordinal();
			if (_calculators[stat] == null)
				_calculators[stat] = new Calculator();
			// Add the Func to the calculator corresponding to the state
			_calculators[stat].addFunc(f);
		}
	}
	
	/**
	 * Add a list of Funcs to the Calculator set of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.
	 * Each Calculator (a calculator per state) own a table of Func object.
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...). <BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is ONLY for L2PcInstance</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Equip an item from inventory</li>
	 * <li>Learn a new passive skill</li>
	 * <li>Use an active skill</li><BR>
	 * <BR>
	 *
	 * @param funcs
	 *            The list of Func objects to add to the Calculator corresponding to the state affected
	 */
	public final void addStatFuncs(Func[] funcs)
	{
		FastList<Stats> modifiedStats = new FastList<Stats>();
		for (Func f : funcs)
		{
			modifiedStats.add(f.stat);
			addStatFunc(f);
		}
		broadcastModifiedStats(modifiedStats);
	}
	
	/**
	 * Remove a Func from the Calculator set of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.
	 * Each Calculator (a calculator per state) own a table of Func object.
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).
	 * To reduce cache memory use, L2NPCInstances who don't have skills share the same Calculator set called <B>NPC_STD_CALCULATOR</B>.<BR>
	 * <BR>
	 * That's why, if a L2NPCInstance is under a skill/spell effect that modify one of its state, a copy of the NPC_STD_CALCULATOR
	 * must be create in its _calculators before addind new Func object.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the Func object from _calculators</li><BR>
	 * <BR>
	 * <li>If L2Character is a L2NPCInstance and _calculators is equal to NPC_STD_CALCULATOR,
	 * free cache memory and just create a link on NPC_STD_CALCULATOR in _calculators</li><BR>
	 * <BR>
	 *
	 * @param f
	 *            The Func object to remove from the Calculator corresponding to the state affected
	 */
	public final void removeStatFunc(Func f)
	{
		if (f == null)
			return;
		// Select the Calculator of the affected state in the Calculator set
		int stat = f.stat.ordinal();
		synchronized (_calculators)
		{
			if (_calculators[stat] == null)
				return;
			// Remove the Func object from the Calculator
			_calculators[stat].removeFunc(f);
			if (_calculators[stat].size() == 0)
				_calculators[stat] = null;
			// If possible, free the memory and just create a link on NPC_STD_CALCULATOR
			if (this instanceof L2Npc)
			{
				int i = 0;
				for (; i < Stats.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
						break;
				}
				if (i >= Stats.NUM_STATS)
					_calculators = NPC_STD_CALCULATOR;
			}
		}
	}
	
	/**
	 * Remove a list of Funcs from the Calculator set of the L2PcInstance.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.
	 * Each Calculator (a calculator per state) own a table of Func object.
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...). <BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is ONLY for L2PcInstance</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Unequip an item from inventory</li>
	 * <li>Stop an active skill</li><BR>
	 * <BR>
	 *
	 * @param funcs
	 *            The list of Func objects to add to the Calculator corresponding to the state affected
	 */
	public final void removeStatFuncs(Func[] funcs)
	{
		FastList<Stats> modifiedStats = new FastList<Stats>();
		for (Func f : funcs)
		{
			modifiedStats.add(f.stat);
			removeStatFunc(f);
		}
		broadcastModifiedStats(modifiedStats);
	}
	
	/**
	 * Remove all Func objects with the selected owner from the Calculator set of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.
	 * Each Calculator (a calculator per state) own a table of Func object.
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).
	 * To reduce cache memory use, L2NPCInstances who don't have skills share the same Calculator set called <B>NPC_STD_CALCULATOR</B>.<BR>
	 * <BR>
	 * That's why, if a L2NPCInstance is under a skill/spell effect that modify one of its state, a copy of the NPC_STD_CALCULATOR
	 * must be create in its _calculators before addind new Func object.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove all Func objects of the selected owner from _calculators</li><BR>
	 * <BR>
	 * <li>If L2Character is a L2NPCInstance and _calculators is equal to NPC_STD_CALCULATOR,
	 * free cache memory and just create a link on NPC_STD_CALCULATOR in _calculators</li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Unequip an item from inventory</li>
	 * <li>Stop an active skill</li><BR>
	 * <BR>
	 *
	 * @param owner
	 *            The Object(Skill, Item...) that has created the effect
	 */
	public final void removeStatsOwner(Object owner)
	{
		removeStatsOwner(owner, false);
	}
	
	public final void removeStatsOwner(Object owner, boolean forceBroadcastAnyway)
	{
		FastList<Stats> modifiedStats = null;
		int i = 0;
		// Go through the Calculator set
		synchronized (_calculators)
		{
			for (Calculator calc : _calculators)
			{
				if (calc != null)
				{
					// Delete all Func objects of the selected owner
					if (modifiedStats != null)
						modifiedStats.addAll(calc.removeOwner(owner));
					else
						modifiedStats = calc.removeOwner(owner);
					if (calc.size() == 0)
						_calculators[i] = null;
				}
				i++;
			}
			// If possible, free the memory and just create a link on NPC_STD_CALCULATOR
			if (this instanceof L2Npc)
			{
				i = 0;
				for (; i < Stats.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
						break;
				}
				if (i >= Stats.NUM_STATS)
					_calculators = NPC_STD_CALCULATOR;
			}
			if (forceBroadcastAnyway || (owner instanceof L2Effect && !((L2Effect) owner).preventExitUpdate))
				broadcastModifiedStats(modifiedStats);
		}
	}
	
	private void broadcastModifiedStats(FastList<Stats> stats)
	{
		if (stats == null || stats.isEmpty())
			return;
		boolean broadcastFull = false;
		boolean otherStats = false;
		StatusUpdate su = null;
		for (Stats stat : stats)
		{
			if (this instanceof L2Summon && ((L2Summon) this).getOwner() != null)
			{
				((L2Summon) this).updateAndBroadcastStatus(1);
				break;
			}
			else if (stat == Stats.POWER_ATTACK_SPEED)
			{
				if (su == null)
					su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.ATK_SPD, getPAtkSpd(null));
			}
			else if (stat == Stats.MAGIC_ATTACK_SPEED)
			{
				if (su == null)
					su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CAST_SPD, getMAtkSpd(null));
			}
			// else if (stat==Stats.MAX_HP) // TODO: self only and add more stats...
			// {
			// if (su == null) su = new StatusUpdate(getObjectId());
			// su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			// }
			else if (stat == Stats.MAX_CP)
			{
				if (this instanceof L2PcInstance)
				{
					if (su == null)
						su = new StatusUpdate(getObjectId());
					su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
				}
			}
			// else if (stat==Stats.MAX_MP)
			// {
			// if (su == null) su = new StatusUpdate(getObjectId());
			// su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
			// }
			else if (stat == Stats.RUN_SPEED)
			{
				broadcastFull = true;
			}
			else
				otherStats = true;
		}
		if (this instanceof L2PcInstance)
		{
			if (broadcastFull)
				((L2PcInstance) this).updateAndBroadcastStatus(2);
			else
			{
				if (otherStats)
				{
					((L2PcInstance) this).updateAndBroadcastStatus(1);
					if (su != null)
					{
						Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
						{
							for (L2PcInstance player : plrs)
							{
								try
								{
									player.sendPacket(su);
								}
								catch (NullPointerException e)
								{}
							}
						}
					}
				}
				else if (su != null)
					broadcastPacket(su);
			}
		}
		else if (this instanceof L2Npc)
		{
			if (broadcastFull)
			{
				Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
				// synchronized (getKnownList().getKnownPlayers())
				{
					for (L2PcInstance player : plrs)
					{
						if (getRunSpeed() == 0)
							player.sendPacket(new ServerObjectInfo((L2Npc) this, player));
						else
							player.sendPacket(new AbstractNpcInfo.NpcInfo((L2Npc) this, player));
					}
				}
			}
			else if (su != null)
				broadcastPacket(su);
		}
		else if (su != null)
			broadcastPacket(su);
	}
	
	/**
	 * Return the orientation of the L2Character.<BR>
	 * <BR>
	 */
	public final int getHeading()
	{
		return _heading;
	}
	
	/**
	 * Set the orientation of the L2Character.<BR>
	 * <BR>
	 */
	public final void setHeading(int heading)
	{
		_heading = heading;
	}
	
	/**
	 * Return the X destination of the L2Character or the X position if not in movement.<BR>
	 * <BR>
	 */
	public final int getClientX()
	{
		return _clientX;
	}
	
	public final int getClientY()
	{
		return _clientY;
	}
	
	public final int getClientZ()
	{
		return _clientZ;
	}
	
	public final int getClientHeading()
	{
		return _clientHeading;
	}
	
	public final void setClientX(int val)
	{
		_clientX = val;
	}
	
	public final void setClientY(int val)
	{
		_clientY = val;
	}
	
	public final void setClientZ(int val)
	{
		_clientZ = val;
	}
	
	public final void setClientHeading(int val)
	{
		_clientHeading = val;
	}
	
	public final int getXdestination()
	{
		MoveData m = _move;
		if (m != null)
			return m._xDestination;
		return getX();
	}
	
	/**
	 * Return the Y destination of the L2Character or the Y position if not in movement.<BR>
	 * <BR>
	 */
	public final int getYdestination()
	{
		MoveData m = _move;
		if (m != null)
			return m._yDestination;
		return getY();
	}
	
	/**
	 * Return the Z destination of the L2Character or the Z position if not in movement.<BR>
	 * <BR>
	 */
	public final int getZdestination()
	{
		MoveData m = _move;
		if (m != null)
			return m._zDestination;
		return getZ();
	}
	
	/**
	 * Return True if the L2Character is in combat.<BR>
	 * <BR>
	 */
	public boolean isInCombat()
	{
		return hasAI() && (getAI().getAttackTarget() != null || getAI().isAutoAttacking());
	}
	
	public boolean isInCombatAI()
	{
		return hasAI() && (getAI().getAttackTarget() != null);
	}
	
	/**
	 * Return True if the L2Character is moving.<BR>
	 * <BR>
	 */
	public final boolean isMoving()
	{
		return _move != null;
	}
	
	/**
	 * Return True if the L2Character is travelling a calculated path.<BR>
	 * <BR>
	 */
	public final boolean isOnGeodataPath()
	{
		MoveData m = _move;
		if (m == null)
			return false;
		if (m.onGeodataPathIndex == -1)
			return false;
		if (m.onGeodataPathIndex == m.geoPath.size() - 1)
			return false;
		return true;
	}
	
	/**
	 * Return True if the L2Character is casting.<BR>
	 * <BR>
	 */
	public final boolean isCastingNow()
	{
		return _isCastingNow;
	}
	
	public void setIsCastingNow(boolean value)
	{
		_isCastingNow = value;
	}
	
	public final boolean isCastingSimultaneouslyNow()
	{
		return _isCastingSimultaneouslyNow;
	}
	
	public void setIsCastingSimultaneouslyNow(boolean value)
	{
		_isCastingSimultaneouslyNow = value;
	}
	
	/**
	 * Return True if the cast of the L2Character can be aborted.<BR>
	 * <BR>
	 */
	public final boolean canAbortCast()
	{
		return _castInterruptTime > GameTimeController.getGameTicks();
	}
	
	public int getCastInterruptTime()
	{
		return _castInterruptTime;
	}
	
	/**
	 * Return True if the L2Character is attacking.<BR>
	 * <BR>
	 */
	public boolean isAttackingNow()
	{
		return getAttackEndTime() > GameTimeController.getGameTicks();
	}
	
	public boolean isAttackingNowOverpower()
	{
		return getAttackEndTime() > GameTimeController.getGameTicks() + 2;
	}
	
	/**
	 * Return True if the L2Character has aborted its attack.<BR>
	 * <BR>
	 */
	public final boolean isAttackAborted()
	{
		return !_attacking;
	}
	
	/**
	 * Abort the attack of the L2Character and send Server->Client ActionFailed packet.<BR>
	 * <BR>
	 */
	public final void abortAttack()
	{
		if (isAttackingNow())
		{
			_attacking = false;
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Returns body part (paperdoll slot) we are targeting right now
	 */
	public final int getAttackingBodyPart()
	{
		return 1;
	}
	
	/**
	 * Abort the cast of the L2Character and send Server->Client MagicSkillCanceld/ActionFailed packet.<BR>
	 * <BR>
	 */
	public final void abortCast()
	{
		if (isCastingNow() || isCastingSimultaneouslyNow())
		{
			// cancels the skill hit scheduled task
			if (_skillCast != null)
			{
				try
				{
					_skillCast.cancel(true);
				}
				catch (NullPointerException e)
				{}
				_skillCast = null;
			}
			if (_skillCast2 != null)
			{
				try
				{
					_skillCast2.cancel(true);
				}
				catch (NullPointerException e)
				{}
				_skillCast2 = null;
			}
			if (getFusionSkill() != null)
				getFusionSkill().onCastAbort();
			L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
			if (mog != null)
				mog.exit();
			if (_allSkillsDisabled)
				enableAllSkills(); // this remains for forced skill use, e.g. scroll of escape
			setIsCastingNow(false);
			setIsCastingSimultaneouslyNow(false);
			// safeguard for cannot be interrupt any more
			_castInterruptTime = 0;
			if (this instanceof L2PcInstance)
				getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING); // setting back previous intention
			broadcastPacket(new MagicSkillCanceld(getObjectId())); // broadcast packet to stop animations client-side
			sendPacket(ActionFailed.STATIC_PACKET); // send an "action failed" packet to the caster
		}
	}
	
	/**
	 * Update the position of the L2Character during a movement and return True if the movement is finished.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B> of the L2Character.
	 * The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<BR>
	 * <BR>
	 * When the movement is started (ex : by MovetoLocation), this method will be called each 0.1 sec to estimate and update the L2Character position on the server.
	 * Note, that the current server position can differe from the current client position even if each movement is straight foward.
	 * That's why, client send regularly a Client->Server ValidatePosition packet to eventually correct the gap on the server.
	 * But, it's always the server position that is used in range calculation.<BR>
	 * <BR>
	 * At the end of the estimated movement time, the L2Character position is automatically set to the destination position even if the movement is not finished.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current Z position is obtained FROM THE CLIENT by the Client->Server ValidatePosition Packet.
	 * But x and y positions must be calculated to avoid that players try to modify their movement speed.</B></FONT><BR>
	 * <BR>
	 *
	 * @param gameTicks
	 *            Nb of ticks since the server start
	 * @return True if the movement is finished
	 */
	public boolean updatePosition(int gameTicks)
	{
		// Get movement data
		MoveData m = _move;
		if (m == null)
			return true;
		if (!isVisible())
		{
			_move = null;
			return true;
		}
		// Check if this is the first update
		if (m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
			m._xAccurate = getX();
			m._yAccurate = getY();
		}
		// Check if the position has already been calculated
		if (m._moveTimestamp == gameTicks)
			return false;
		int xPrev = getX();
		int yPrev = getY();
		int zPrev = getZ(); // the z coordinate may be modified by coordinate synchronizations
		double dx, dy, dz, distFraction;
		if (Config.COORD_SYNCHRONIZE == 1)
		// the only method that can modify x,y while moving (otherwise _move would/should be set null)
		{
			dx = m._xDestination - xPrev;
			dy = m._yDestination - yPrev;
		}
		else // otherwise we need saved temporary values to avoid rounding errors
		{
			dx = m._xDestination - m._xAccurate;
			dy = m._yDestination - m._yAccurate;
		}
		// Z coordinate will follow geodata or client values
		if (Config.GEODATA > 0 && Config.COORD_SYNCHRONIZE == 2 && !isFlying() && !isInsideZone(L2Character.ZONE_WATER) && !m.disregardingGeodata && GameTimeController.getGameTicks() % 10 == 0 // once a second to reduce possible cpu load
		&& GeoData.getInstance().hasGeo(xPrev, yPrev) && !(this instanceof L2BoatInstance) && !(this instanceof L2AirShipInstance))
		{
			short geoHeight = GeoData.getInstance().getSpawnHeight(xPrev, yPrev, zPrev - 30, zPrev + 30, getObjectId());
			dz = m._zDestination - geoHeight;
			// quite a big difference, compare to validatePosition packet
			if (this instanceof L2PcInstance && Math.abs(((L2PcInstance) this).getClientZ() - geoHeight) > 200 && Math.abs(((L2PcInstance) this).getClientZ() - geoHeight) < 1500)
			{
				dz = m._zDestination - zPrev; // allow diff
			}
			else if (isInCombat() && Math.abs(dz) > 200 && (dx * dx + dy * dy) < 40000) // allow mob to climb up to pcinstance
			{
				dz = m._zDestination - zPrev; // climbing
			}
			else
			{
				zPrev = geoHeight;
			}
		}
		else
			dz = m._zDestination - zPrev;
		float speed;
		if (this instanceof L2BoatInstance)
		{
			speed = ((L2BoatInstance) this).boatSpeed;
		}
		else if (this instanceof L2AirShipInstance)
		{
			speed = ((L2AirShipInstance) this).boatSpeed;
		}
		else
		{
			speed = getStat().getMoveSpeed();
		}
		double distPassed = speed * (gameTicks - m._moveTimestamp) / GameTimeController.TICKS_PER_SECOND;
		if ((dx * dx + dy * dy) < 10000 && (dz * dz > 2500)) // close enough, allows error between client and server geodata if it cannot be avoided
		{
			distFraction = distPassed / Math.sqrt(dx * dx + dy * dy);
		}
		else
			distFraction = distPassed / Math.sqrt(dx * dx + dy * dy + dz * dz);
		// if (Config.DEVELOPER) _log.warning("Move Ticks:" + (gameTicks - m._moveTimestamp) + ", distPassed:" + distPassed + ", distFraction:" + distFraction);
		if (distFraction > 1) // already there
		{
			// Set the position of the L2Character to the destination
			super.getPosition().setXYZ(m._xDestination, m._yDestination, m._zDestination);
			if (this instanceof L2BoatInstance)
			{
				((L2BoatInstance) this).updatePeopleInTheBoat(m._xDestination, m._yDestination, m._zDestination);
			}
			else if (this instanceof L2AirShipInstance)
			{
				((L2AirShipInstance) this).updatePeopleInTheAirShip(m._xDestination, m._yDestination, m._zDestination);
			}
			else
			{
				revalidateZone(false);
			}
		}
		else
		{
			m._xAccurate += dx * distFraction;
			m._yAccurate += dy * distFraction;
			// Set the position of the L2Character to estimated after parcial move
			super.getPosition().setXYZ((int) (m._xAccurate), (int) (m._yAccurate), zPrev + (int) (dz * distFraction + 0.5));
			if (this instanceof L2BoatInstance)
			{
				((L2BoatInstance) this).updatePeopleInTheBoat((int) (m._xAccurate), (int) (m._yAccurate), zPrev + (int) (dz * distFraction + 0.5));
			}
			else if (this instanceof L2AirShipInstance)
			{
				((L2AirShipInstance) this).updatePeopleInTheAirShip(m._xDestination, m._yDestination, m._zDestination);
			}
			else
			{
				revalidateZone(false);
				if (isRunning())
				{
					final int hamstring = (int) calcStat(Stats.HAMSTRING, 0, null, null);
					if (hamstring > 0)
					{
						reduceCurrentHp(hamstring * distPassed, this, true, true, null, true);
					}
				}
				final int demonic = (int) calcStat(Stats.PHAZE_MOVEMENT, 0, null, null);
				if (demonic > 0)
				{
					if (_demonicMovement == demonic)
					{
						_demonicMovement = 0;
						for (L2PcInstance nigga : getKnownList().getKnownPlayers().values())
						{
							if (nigga != null && nigga.getKnownList() != null && nigga.getKnownList().knowsObject(this))
							{
								nigga.sendPacket(new StopMove(this));
								nigga.getKnownList().removeKnownObject(this);
								nigga.getKnownList().addKnownObject(this);
							}
						}
					}
					else if (_demonicMovement < demonic)
						_demonicMovement++;
					else
						_demonicMovement = 0;
				}
				else
				{
					_demonicMovement = 0;
				}
			}
		}
		// Set the timer of last position update to now
		m._moveTimestamp = gameTicks;
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM && this instanceof L2PcInstance)
		{
			getActingPlayer().getCounters().distanceWalked += distPassed;
		}
		if (this instanceof L2PcInstance)
		{
			if (Config.HWID_FARMWHILEEVENT_CHECK)
			{
				if (getActingPlayer().isInsideZone(ZONE_FARM) && !getActingPlayer().isInActiveFunEvent())
				{
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						String HWID = player.getHWID();
						String HWID2 = getActingPlayer().getHWID();
						if (HWID == null || HWID2 == null)
							continue;
						if (HWID.equalsIgnoreCase(HWID2))
						{
							if (player.isInActiveFunEvent())
							{
								setIsPendingRevive(true);
								teleToLocation(83380, 148107, -3404, true);
								setInsideZone(L2Character.ZONE_FARM, false);
								sendMessage("You can't have multiple windows in Farm/Event/PvP zones.");
								break;
							}
						}
					}
				}
			}
		}
		return (distFraction > 1);
	}
	
	public void revalidateZone(boolean force)
	{
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
		if (getInstanceId() == 0)
			setInstanceId(0);
		getWorldRegion().revalidateZones(this);
	}
	
	/**
	 * Stop movement of the L2Character (Called by AI Accessor only).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete movement data of the L2Character</li>
	 * <li>Set the current position (x,y,z), its current L2WorldRegion if necessary and its heading</li>
	 * <li>Remove the L2Object object from _gmList** of GmListTable</li>
	 * <li>Remove object from _knownObjects and _knownPlayer* of all surrounding L2WorldRegion L2Characters</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T send Server->Client packet StopMove/StopRotation </B></FONT><BR>
	 * <BR>
	 */
	public void stopMove(L2CharPosition pos)
	{
		stopMove(pos, false);
	}
	
	public void stopMove(L2CharPosition pos, boolean updateKnownObjects)
	{
		// Delete movement data of the L2Character
		_move = null;
		// if (getAI() != null)
		// getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		// Set the current position (x,y,z), its current L2WorldRegion if necessary and its heading
		// All data are contained in a L2CharPosition object
		if (pos != null)
		{
			getPosition().setXYZ(pos.x, pos.y, pos.z);
			setHeading(pos.heading);
			revalidateZone(true);
		}
		broadcastPacket(new StopMove(this));
		if (this instanceof L2Npc && hasAI() && getAI() != null)
		{
			if (getAI().getIntention() == CtrlIntention.AI_INTENTION_MOVE_TO)
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
		if (Config.MOVE_BASED_KNOWNLIST && updateKnownObjects)
			getKnownList().findObjects();
	}
	
	/**
	 * @return Returns the showSummonAnimation.
	 */
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}
	
	/**
	 * @param showSummonAnimation
	 *            The showSummonAnimation to set.
	 */
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}
	
	/**
	 * Target a L2Object (add the target to the L2Character _target, _knownObject and L2Character to _KnownObject of the L2Object).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * The L2Object (including L2Character) targeted is identified in <B>_target</B> of the L2Character<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the _target of L2Character to L2Object</li>
	 * <li>If necessary, add L2Object to _knownObject of the L2Character</li>
	 * <li>If necessary, add L2Character to _KnownObject of the L2Object</li>
	 * <li>If object==null, cancel Attak or Cast</li><BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance : Remove the L2PcInstance from the old target _statusListener and add it to the new target if it was a L2Character</li><BR>
	 * <BR>
	 *
	 * @param object
	 *            L2object to target
	 */
	public void setTarget(L2Object object)
	{
		if (object != null && !object.isVisible())
			object = null;
		if (object != null && object != _target)
		{
			getKnownList().addKnownObject(object);
			object.getKnownList().addKnownObject(this);
		}
		_target = object;
	}
	
	/**
	 * Return the identifier of the L2Object targeted or -1.<BR>
	 * <BR>
	 */
	public final int getTargetId()
	{
		if (_target != null)
		{
			return _target.getObjectId();
		}
		return -1;
	}
	
	/**
	 * Return the L2Object targeted or null.<BR>
	 * <BR>
	 */
	public final L2Object getTarget()
	{
		return _target;
	}
	
	public L2Character getTargetChar()
	{
		if (_target instanceof L2Character)
			return (L2Character) _target;
		return null;
	}
	
	// called from AIAccessor only
	/**
	 * Calculate movement data for a move to location action and add the L2Character to movingObjects of GameTimeController (only called by AI Accessor).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B> of the L2Character.
	 * The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<BR>
	 * <BR>
	 * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController that will call the updatePosition method of those L2Character each 0.1s.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get current position of the L2Character</li>
	 * <li>Calculate distance (dx,dy) between current position and destination including offset</li>
	 * <li>Create and Init a MoveData object</li>
	 * <li>Set the L2Character _move object to MoveData object</li>
	 * <li>Add the L2Character to movingObjects of the GameTimeController</li>
	 * <li>Create a task to notify the AI that L2Character arrives at a check point of the movement</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T send Server->Client packet MoveToPawn/CharMoveToLocation </B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>AI : onIntentionMoveTo(L2CharPosition), onIntentionPickUp(L2Object), onIntentionInteract(L2Object)</li>
	 * <li>FollowTask</li><BR>
	 * <BR>
	 *
	 * @param x
	 *            The X position of the destination
	 * @param y
	 *            The Y position of the destination
	 * @param z
	 *            The Y position of the destination
	 * @param offset
	 *            The size of the interaction area of the L2Character targeted
	 */
	protected void moveToLocation(int x, int y, int z, int offset)
	{
		// Get the Move Speed of the L2Charcater
		float speed = getStat().getMoveSpeed();
		if (speed <= 1 || isMovementDisabled())
			return;
		if (this instanceof L2PcInstance)
			getActingPlayer().onActionRequest();
		// Get current position of the L2Character
		final int curX = super.getX();
		final int curY = super.getY();
		final int curZ = super.getZ();
		// Calculate distance (dx,dy) between current position and destination
		// TODO: improve Z axis move/follow support when dx,dy are small compared to dz
		double dx = (x - curX);
		double dy = (y - curY);
		double dz = (z - curZ);
		double distance = Math.sqrt(dx * dx + dy * dy);
		// make water move short and use no geodata checks for swimming chars
		// distance in a click can easily be over 3000
		if (Config.GEODATA > 0 && isInsideZone(ZONE_WATER) && distance > 700)
		{
			double divider = 700 / distance;
			x = curX + (int) (divider * dx);
			y = curY + (int) (divider * dy);
			z = curZ + (int) (divider * dz);
			dx = (x - curX);
			dy = (y - curY);
			dz = (z - curZ);
			distance = Math.sqrt(dx * dx + dy * dy);
		}
		if (Config.DEBUG)
			_log.fine("distance to target:" + distance);
		// Define movement angles needed
		// ^
		// | X (x,y)
		// | /
		// | /distance
		// | /
		// |/ angle
		// X ---------->
		// (curx,cury)
		double cos;
		double sin;
		// Check if a movement offset is defined or no distance to go through
		if (offset > 0 || distance < 1)
		{
			// approximation for moving closer when z coordinates are different
			// TODO: handle Z axis movement better
			offset -= Math.abs(dz);
			if (offset < 5)
				offset = 5;
			// If no distance to go through, the movement is canceled
			if (distance < 1 || distance - offset <= 0)
			{
				if (Config.DEBUG)
					_log.fine("already in range, no movement needed.");
				// Notify the AI that the L2Character is arrived at destination
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				return;
			}
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
			distance -= (offset - 5); // due to rounding error, we have to move a bit closer to be in range
			// Calculate the new destination with offset included
			x = curX + (int) (distance * cos);
			y = curY + (int) (distance * sin);
		}
		else
		{
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
		}
		// Create and Init a MoveData object
		MoveData m = new MoveData();
		// GEODATA MOVEMENT CHECKS AND PATHFINDING
		m.onGeodataPathIndex = -1; // Initialize not on geodata path
		m.disregardingGeodata = false;
		if (Config.GEODATA > 0 && !isFlying() // flying chars not checked - even canSeeTarget doesn't work yet
		&& (!isInsideZone(ZONE_WATER) || isInsideZone(ZONE_SIEGE)) // swimming also not checked unless in siege zone - but distance is limited
		&& !(this instanceof L2NpcWalkerInstance)) // npc walkers not checked
		{
			double originalDistance = distance;
			int originalX = x;
			int originalY = y;
			int originalZ = z;
			int gtx = (originalX - L2World.MAP_MIN_X) >> 4;
			int gty = (originalY - L2World.MAP_MIN_Y) >> 4;
			// Movement checks:
			// when geodata == 2, for all characters except mobs returning home (could be changed later to teleport if pathfinding fails)
			// when geodata == 1, for l2playableinstance and l2riftinstance only
			if ((Config.GEODATA == 2 && Config.APC_PATHFIND >= 1 && (isAPC() && !((L2Attackable) this).isReturningToSpawnPoint())) || this instanceof L2PcInstance || (this instanceof L2Summon && !(getAI().getIntention() == AI_INTENTION_FOLLOW)) // assuming intention_follow only when following owner
			|| isAfraid() || this instanceof L2RiftInvaderInstance)
			{
				if (isOnGeodataPath())
				{
					try
					{
						if (gtx == _move.geoPathGtx && gty == _move.geoPathGty)
							return;
						else
							_move.onGeodataPathIndex = -1; // Set not on geodata path
					}
					catch (NullPointerException e)
					{
						// nothing
					}
				}
				if (curX < L2World.MAP_MIN_X || curX > L2World.MAP_MAX_X || curY < L2World.MAP_MIN_Y || curY > L2World.MAP_MAX_Y)
				{
					// Temporary fix for character outside world region errors
					_log.warning("Character " + getName() + " outside world area, in coordinates x:" + curX + " y:" + curY);
					getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					if (this instanceof L2PcInstance)
						((L2PcInstance) this).deleteMe();
					else if (this instanceof L2Summon)
						return; // preventation when summon get out of world coords, player will not loose him, unsummon handled from pcinstance
					else
						onDecay();
					return;
				}
				Location destiny = GeoData.getInstance().moveCheck(curX, curY, curZ, x, y, z, getInstanceId());
				// location different if destination wasn't reached (or just z coord is different)
				x = destiny.getX();
				y = destiny.getY();
				z = destiny.getZ();
				distance = Math.sqrt((x - curX) * (x - curX) + (y - curY) * (y - curY));
			}
			// Pathfinding checks. Only when geodata setting is 2, the LoS check gives shorter result
			// than the original movement was and the LoS gives a shorter distance than 2000
			// This way of detecting need for pathfinding could be changed.
			if (Config.GEODATA == 2 && (this instanceof L2Playable || (Config.APC_PATHFIND >= 1 && isAPC())) && originalDistance - distance > 100 && distance < 2000 && !isAfraid())
			{
				// Path calculation // Overrides previous movement check
				if (this instanceof L2Playable || isInCombat() || this instanceof L2MinionInstance)
				{
					m.geoPath = PathFinding.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, getInstanceId());
					if (m.geoPath == null || m.geoPath.size() < 2) // No path found
					{
						// * Even though there's no path found (remember geonodes aren't perfect),
						// the mob is attacking and right now we set it so that the mob will go
						// after target anyway, is dz is small enough.
						// * With cellpathfinding this approach could be changed but would require taking
						// off the geonodes and some more checks.
						// * Summons will follow their masters no matter what.
						// * Currently minions also must move freely since L2AttackableAI commands
						// them to move along with their leader
						if (this instanceof L2PcInstance || (this instanceof L2Summon && !((L2Summon) this).getFollowStatus()))
						{
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							return;
						}
						else
						{
							m.disregardingGeodata = true;
							x = originalX;
							y = originalY;
							z = originalZ;
							distance = originalDistance;
						}
					}
					else
					{
						m.onGeodataPathIndex = 0; // on first segment
						m.geoPathGtx = gtx;
						m.geoPathGty = gty;
						m.geoPathAccurateTx = originalX;
						m.geoPathAccurateTy = originalY;
						x = m.geoPath.get(m.onGeodataPathIndex).getX();
						y = m.geoPath.get(m.onGeodataPathIndex).getY();
						z = m.geoPath.get(m.onGeodataPathIndex).getZ();
						// check for doors in the route
						if (DoorTable.getInstance().checkIfDoorsBetween(curX, curY, curZ, x, y, z, getInstanceId()))
						{
							m.geoPath = null;
							getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
							return;
						}
						for (int i = 0; i < m.geoPath.size() - 1; i++)
						{
							if (DoorTable.getInstance().checkIfDoorsBetween(m.geoPath.get(i), m.geoPath.get(i + 1), getInstanceId()))
							{
								m.geoPath = null;
								getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
								return;
							}
						}
						dx = (x - curX);
						dy = (y - curY);
						distance = Math.sqrt(dx * dx + dy * dy);
						sin = dy / distance;
						cos = dx / distance;
					}
				}
			}
			// If no distance to go through, the movement is canceled
			if (distance < 1 && (/* (Config.GEODATA == 2 && this instanceof L2Playable) || */ this instanceof L2Playable || isAPC() || isAfraid() || this instanceof L2RiftInvaderInstance))
			{
				if (this instanceof L2Summon)
					((L2Summon) this).setFollowStatus(false);
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				return;
			}
		}
		// Caclulate the Nb of ticks between the current position and the destination
		// One tick added for rounding reasons
		int ticksToMove = 1 + (int) (GameTimeController.TICKS_PER_SECOND * distance / speed);
		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z; // this is what was requested from client
		// Calculate and set the heading of the L2Character
		m._heading = 0; // initial value for coordinate sync
		setHeading(Util.calculateHeadingFrom(cos, sin));
		if (Config.DEBUG)
			_log.fine("dist:" + distance + "speed:" + speed + " ttt:" + ticksToMove + " heading:" + getHeading());
		m._moveStartTime = GameTimeController.getGameTicks();
		// Set the L2Character _move object to MoveData object
		_move = m;
		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingObject(this);
		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		if (ticksToMove * GameTimeController.MILLIS_IN_TICK > 3000)
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		// the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive
		// to destination by GameTimeController
	}
	
	public boolean moveToNextRoutePoint()
	{
		if (!isOnGeodataPath())
		{
			// Cancel the move action
			_move = null;
			return false;
		}
		// Get the Move Speed of the L2Charcater
		float speed = getStat().getMoveSpeed();
		if (speed <= 0 || isMovementDisabled())
		{
			// Cancel the move action
			_move = null;
			return false;
		}
		MoveData md = _move;
		if (md == null)
			return false;
		// Create and Init a MoveData object
		MoveData m = new MoveData();
		// Update MoveData object
		m.onGeodataPathIndex = md.onGeodataPathIndex + 1; // next segment
		m.geoPath = md.geoPath;
		m.geoPathGtx = md.geoPathGtx;
		m.geoPathGty = md.geoPathGty;
		m.geoPathAccurateTx = md.geoPathAccurateTx;
		m.geoPathAccurateTy = md.geoPathAccurateTy;
		if (md.onGeodataPathIndex == md.geoPath.size() - 2)
		{
			m._xDestination = md.geoPathAccurateTx;
			m._yDestination = md.geoPathAccurateTy;
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		else
		{
			m._xDestination = md.geoPath.get(m.onGeodataPathIndex).getX();
			m._yDestination = md.geoPath.get(m.onGeodataPathIndex).getY();
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		double dx = (m._xDestination - super.getX());
		double dy = (m._yDestination - super.getY());
		double distance = Math.sqrt(dx * dx + dy * dy);
		double sin = dy / distance;
		double cos = dx / distance;
		// Caclulate the Nb of ticks between the current position and the destination
		// One tick added for rounding reasons
		int ticksToMove = 1 + (int) (GameTimeController.TICKS_PER_SECOND * distance / speed);
		// Calculate and set the heading of the L2Character
		int heading = (int) (Math.atan2(-sin, -cos) * 10430.378);
		heading += 32768;
		setHeading(heading);
		m._heading = 0; // initial value for coordinate sync
		m._moveStartTime = GameTimeController.getGameTicks();
		if (Config.DEBUG)
			_log.fine("time to target:" + ticksToMove);
		// Set the L2Character _move object to MoveData object
		_move = m;
		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingObject(this);
		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		if (ticksToMove * GameTimeController.MILLIS_IN_TICK > 3000)
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		// the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive
		// to destination by GameTimeController
		// Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
		MoveToLocation msg = new MoveToLocation(this);
		broadcastPacket(msg);
		return true;
	}
	
	public boolean validateMovementHeading(int heading)
	{
		MoveData m = _move;
		if (m == null)
			return true;
		boolean result = true;
		if (m._heading != heading)
		{
			result = (m._heading == 0); // initial value or false
			m._heading = heading;
		}
		return result;
	}
	
	/**
	 * Return the distance between the current position of the L2Character and the target (x,y).<BR>
	 * <BR>
	 *
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @return the plan distance
	 * @deprecated use getPlanDistanceSq(int x, int y, int z)
	 */
	@Deprecated
	public final double getDistance(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	/**
	 * Return the distance between the current position of the L2Character and the target (x,y).<BR>
	 * <BR>
	 *
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @return the plan distance
	 * @deprecated use getPlanDistanceSq(int x, int y, int z)
	 */
	@Deprecated
	public final double getDistance(int x, int y, int z)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	
	/**
	 * Return the squared distance between the current position of the L2Character and the given object.<BR>
	 * <BR>
	 *
	 * @param object
	 *            L2Object
	 * @return the squared distance
	 */
	public final double getDistanceSq(L2Object object)
	{
		return getDistanceSq(object.getX(), object.getY(), object.getZ());
	}
	
	/**
	 * Return the squared distance between the current position of the L2Character and the given x, y, z.<BR>
	 * <BR>
	 *
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @param z
	 *            Z position of the target
	 * @return the squared distance
	 */
	public final double getDistanceSq(int x, int y, int z)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();
		return (dx * dx + dy * dy + dz * dz);
	}
	
	/**
	 * Return the squared plan distance between the current position of the L2Character and the given object.<BR>
	 * (check only x and y, not z)<BR>
	 * <BR>
	 *
	 * @param object
	 *            L2Object
	 * @return the squared plan distance
	 */
	public final double getPlanDistanceSq(L2Object object)
	{
		return getPlanDistanceSq(object.getX(), object.getY());
	}
	
	/**
	 * Return the squared plan distance between the current position of the L2Character and the given x, y, z.<BR>
	 * (check only x and y, not z)<BR>
	 * <BR>
	 *
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @return the squared plan distance
	 */
	public final double getPlanDistanceSq(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();
		return (dx * dx + dy * dy);
	}
	
	/**
	 * Check if this object is inside the given radius around the given object. Warning: doesn't cover collision radius!<BR>
	 * <BR>
	 *
	 * @param object
	 *            the target
	 * @param radius
	 *            the radius around the target
	 * @param checkZ
	 *            should we check Z axis also
	 * @param strictCheck
	 *            true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 * @see net.sf.l2j.gameserver.model.L2Character.isInsideRadius(int x, int y, int z, int radius, boolean checkZ, boolean strictCheck)
	 */
	public final boolean isInsideRadius(L2Object object, int radius, boolean checkZ, boolean strictCheck)
	{
		return isInsideRadius(object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
	}
	public final boolean isInsideRadius(Location object, int radius, boolean checkZ, boolean strictCheck)
	{
		return isInsideRadius(object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
	}
	/**
	 * Check if this object is inside the given plan radius around the given point. Warning: doesn't cover collision radius!<BR>
	 * <BR>
	 *
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @param radius
	 *            the radius around the target
	 * @param strictCheck
	 *            true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius(int x, int y, int radius, boolean strictCheck)
	{
		return isInsideRadius(x, y, 0, radius, false, strictCheck);
	}
	
	/**
	 * Check if this object is inside the given radius around the given point.<BR>
	 * <BR>
	 *
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @param z
	 *            Z position of the target
	 * @param radius
	 *            the radius around the target
	 * @param checkZ
	 *            should we check Z axis also
	 * @param strictCheck
	 *            true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius(int x, int y, int z, int radius, boolean checkZ, boolean strictCheck)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();
		if (strictCheck)
		{
			if (checkZ)
				return (dx * dx + dy * dy + dz * dz) < radius * radius;
			else
				return (dx * dx + dy * dy) < radius * radius;
		}
		else
		{
			if (checkZ)
				return (dx * dx + dy * dy + dz * dz) <= radius * radius;
			else
				return (dx * dx + dy * dy) <= radius * radius;
		}
	}
	
	/**
	 * Return the Weapon Expertise Penalty of the L2Character.<BR>
	 * <BR>
	 */
	public float getWeaponExpertisePenalty()
	{
		return 1.f;
	}
	
	/**
	 * Return the Armour Expertise Penalty of the L2Character.<BR>
	 * <BR>
	 */
	public float getArmourExpertisePenalty()
	{
		return 1.f;
	}
	
	public void setIsAttacking()
	{
		_attacking = true;
	}
	
	/**
	 * Retun True if arrows are available.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	protected boolean checkAndEquipArrows()
	{
		return true;
	}
	
	/**
	 * Retun True if bolts are available.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	protected boolean checkAndEquipBolts()
	{
		return true;
	}
	
	/**
	 * Add Exp and Sp to the L2Character.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li>
	 * <li>L2PetInstance</li><BR>
	 * <BR>
	 */
	public void addExpAndSp(long addToExp, int addToSp)
	{
		// Dummy method (overridden by players and pets)
	}
	
	/**
	 * Return the active weapon instance (always equiped in the right hand).<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	public abstract L2ItemInstance getActiveWeaponInstance();
	
	/**
	 * Return the active weapon item (always equiped in the right hand).<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	public abstract L2Weapon getActiveWeaponItem();
	
	/**
	 * Return the secondary weapon instance (always equiped in the left hand).<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	public abstract L2ItemInstance getSecondaryWeaponInstance();
	
	/**
	 * Return the secondary weapon item (always equiped in the left hand).<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	public abstract L2Weapon getSecondaryWeaponItem();
	
	private int _damageCount;
	
	public int getDamageCount()
	{
		return _damageCount;
	}
	
	public void incDamageCount(int damage)
	{
		_damageCount += damage;
	}
	
	public void setDamageCount(int damage)
	{
		_damageCount = damage;
	}
	
	/**
	 * Manage hit process (called by Hit Task).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)</li>
	 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance</li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
	 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li><BR>
	 * <BR>
	 *
	 * @param target
	 *            The L2Character targeted
	 * @param damage
	 *            Nb of HP to reduce
	 * @param crit
	 *            True if hit is critical
	 * @param miss
	 *            True if hit is missed
	 * @param soulshot
	 *            True if SoulShot are charged
	 * @param shld
	 *            True if shield is efficient
	 */
	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld, final boolean pierced, byte absNerf, boolean playSound)
	{
		// If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL
		// and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)
		if (target == null || isAlikeDead() || (this instanceof L2Npc && ((L2Npc) this).isEventMob))
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if ((this instanceof L2Npc && target.isAlikeDead()) || target.isDead() || (!getKnownList().knowsObject(target) && !(this instanceof L2DoorInstance)))
		{
			// getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (playSound)
		{
			broadcastPacket(new MagicSkillUse(this, 2158, 1, 0, 0)); // broadcast soulshot charge animation for tard grs
			/*
			 * final int chance = Rnd.get(10);
			 * PlaySound swordSound = new PlaySound("ItemSound.public_sword_shing_"+String.valueOf(chance+1));
			 */
			PlaySound swordSound = new PlaySound(0, "dav.bladeshing", 1, getObjectId(), getX(), getY(), getZ());
			broadcastPacket(swordSound);
		}
		if (this instanceof L2PcInstance && target instanceof L2PcInstance && target.getActingPlayer().getInEventPeaceZone())
		{
			sendMessage("Target is in event safe zone.");
			return;
		}
		if (miss)
		{
			if (this instanceof L2PcInstance)
				sendPacket(new SystemMessage(SystemMessageId.MISSED_TARGET)); // msg miss
			// ON_EVADED_HIT
			if (target.getChanceSkills() != null)
				target.getChanceSkills().onEvadedHit(this);
			if (target instanceof L2PcInstance && (!isInvisible() || (!isGMReally() && target.canSeeInvisiblePeople())))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.AVOIDED_C1_ATTACK);
				sm.addCharName(this);
				target.sendPacket(sm);
			}
		}
		else
		{
			if (pierced)
				sendMessage("Your shot pierces through " + target.getDisplayName());
		}
		// If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance
		if (!isAttackAborted())
		{
			if (target.isRaid() && !Config.RAID_DISABLE_CURSE && getActingPlayer() != null)
			{
				int level = getActingPlayer().getLevel();
				if (level > target.getLevel() + 8)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(4515, 1);
					if (skill != null)
						skill.getEffects(target, this);
					else
						_log.warning("Skill 4515 at level 1 is missing in DP.");
					damage = 0; // prevents messing up drop calculation
				}
			}
			if (!miss)
			{
				if (!target.isInvul())
				{
					if (crit && target.isBleeding())
					{
						final int bleedDmgExtra = (int) calcStat(Stats.CRITICAL_DMG_ADD_BLEEDING, 0, null, null);
						if (bleedDmgExtra > 0)
						{
							damage += bleedDmgExtra;
							sendMessage(target.getDisplayName() + " is bleeding and receives " + bleedDmgExtra + " more critical damage");
						}
					}
				}
				damage += calcStat(Stats.DMG_ADD, 0, target, null);
				damage -= target.calcStat(Stats.DMG_REMOVE, 0, this, null);
				if (shld > 0)
					damage -= target.calcStat(Stats.DMG_REMOVE_SHIELD, 0, this, null);
				if (target instanceof L2RaidBossInstance)
				{
					if (this instanceof L2PcInstance)
					{
						incDamageCount(damage);
						target.addRaidAttackerToList(this);
					}
				}
				if (playSound)
					sendDamageMessage(target, damage, false, crit, miss, true);
				else
					sendDamageMessage(target, damage, false, crit, miss);
			}
			if (!miss && damage > 0)
			{
				// If L2Character target is a L2PcInstance, send a system message
				if (target instanceof L2PcInstance)
				{
					L2PcInstance enemy = (L2PcInstance) target;
					enemy.getAI().clientStartAutoAttack();
				}
				else if (target instanceof L2Decoy)
				{
					((L2Decoy) target).broadcastPacket(new AutoAttackStart(target.getObjectId()));
				}
				else if (target.isAPC())
				{
					target.getAI().clientStartAutoAttack();
				}
				if (!isDead() && !target.isDead())
				{
					if (this instanceof L2PcInstance && target instanceof L2PcInstance && getActingPlayer()._inEventHG && target.getActingPlayer()._inEventHG && NewHuntingGrounds._started)
					{
						target.doDie(getActingPlayer());
						SystemMessage smsg = new SystemMessage(SystemMessageId.DEBUFF_SLOT_FAILED);
						smsg.addString(getName() + " killed you.");
						target.sendPacket(smsg);
					}
					else
					{
						target.reduceCurrentHp(damage, this, null);
					}
				}
				boolean reflect = Rnd.get(3) == 0;
				final L2Weapon weapon = getActiveWeaponItem();
				final boolean isBow = (weapon != null && (weapon.getItemType() == L2WeaponType.BOW || weapon.getItemType() == L2WeaponType.CROSSBOW));
				if (reflect)
				{
					int reflectedDamage = 0;
					if (this instanceof L2Playable)
					{
						if (!isBow) // Do not reflect if weapon is of type bow
						{
							// Reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary
							double reflectPercent = (target.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, this, null) - calcStat(Stats.REFLECT_RES, 0, target, null)) * 2;
							if (reflectPercent > 0)
							{
								reflectedDamage = (int) (reflectPercent / 100. * damage);
								if (reflectedDamage > target.getMaxHp()) // to prevent extreme damage when hitting a low lvl char...
									reflectedDamage = target.getMaxHp();
							}
						}
						else
						{
							double reflectPercent = target.getStat().calcStat(Stats.REFLECT_ARROWS, 0, this, null) * 2;
							if (reflectPercent > 0)
							{
								reflectedDamage = (int) (reflectPercent / 100. * damage);
								if (reflectedDamage > target.getMaxHp()) // to prevent extreme damage when hitting a low lvl char...
									reflectedDamage = target.getMaxHp();
							}
						}
					}
					if (reflectedDamage > 0)
					{
						if (!isDead() && !target.isDead())
						{
							if (this instanceof L2PcInstance)
								getActingPlayer().getStatus().reduceHp(reflectedDamage, target, true, false, false, false, true);
							else
								reduceCurrentHp(reflectedDamage, target, true, false, null);
							if (!isInvisible() || (!isGMReally() && target.canSeeInvisiblePeople()))
							{
								if (target instanceof L2PcInstance)
									((L2PcInstance) target).sendMessage("You reflected " + reflectedDamage + " damage to " + getDisplayName());
								else if (target instanceof L2Summon)
									((L2Summon) target).getOwner().sendMessage("Summon reflected " + reflectedDamage + " damage to " + getDisplayName());
							}
							if (this instanceof L2PcInstance)
								((L2PcInstance) this).sendMessage("Target reflected to you " + reflectedDamage + " damage");
							else if (this instanceof L2Summon)
								((L2Summon) this).getOwner().sendMessage("Target reflected to your summon " + reflectedDamage + " damage");
						}
					}
				}
				final boolean reflectVamp = Rnd.get(3) == 0;
				boolean isUndead = target instanceof L2MonsterInstance && target.isUndead();
				if (!target.isInvul())
				{
					if (!isBow) // Do not absorb if weapon is of type bow
					{
						if (reflectVamp)
						{
							// Absorb HP from the damage inflicted
							double absorbPercent = getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, target, null) * 2;
							if (absorbPercent > 0)
							{
								if (target instanceof L2Playable)
									absorbPercent /= 2;
								else if (isUndead)
								{
									absorbPercent /= 3;
									absorbPercent /= absNerf;
								}
								else
								{
									if (this instanceof L2PcInstance && getActingPlayer().isCursedWeaponEquipped())
										absorbPercent /= 3;
									absorbPercent /= absNerf;
								}
								int maxCanAbsorb = (int) (getMaxHp() - getCurrentHp());
								int absorbDamage = (int) (absorbPercent / 100. * damage);
								if (absorbDamage > maxCanAbsorb)
									absorbDamage = maxCanAbsorb; // Can't absord more than max hp
								if (absorbDamage > 0)
								{
									if (!isDead() && !target.isDead())
										setCurrentHp(getCurrentHp() + absorbDamage);
								}
								sendPacket(new ExShowScreenMessage(1, -1, 2, 0, 1, 0, 0, false, 2500, 0, "Drained " + target.getDisplayName() + " for " + absorbDamage + " HP"));
							}
							// Absorb CP from the damage inflicted
							absorbPercent = getStat().calcStat(Stats.ABSORB_CP_PERCENT, 0, target, null) * 2;
							if (absorbPercent > 0)
							{
								int maxCanAbsorb = (int) (getMaxCp() - getStatus().getCurrentCp());
								int absorbDamage = (int) (absorbPercent / 100. * damage);
								if (absorbDamage > maxCanAbsorb)
									absorbDamage = maxCanAbsorb; // Can't absorb more than max cp
								if (absorbDamage > 0 && !isDead() && !target.isDead())
									getStatus().setCurrentCp(getStatus().getCurrentCp() + absorbDamage);
							}
							// Absorb MP from the damage inflicted
							absorbPercent = getStat().calcStat(Stats.ABSORB_MANA_DAMAGE_PERCENT, 0, target, null) * 2;
							if (absorbPercent > 0)
							{
								int maxCanAbsorb = (int) (getMaxMp() - getCurrentMp());
								int absorbDamage = (int) (absorbPercent / 100. * damage);
								if (absorbDamage > maxCanAbsorb)
									absorbDamage = maxCanAbsorb; // Can't absord more than max hp
								if (absorbDamage > 0)
								{
									if (!isDead() && !target.isDead())
										setCurrentMp(getCurrentMp() + absorbDamage);
								}
							}
						}
					}
				}
				if (!target.isInvul())
				{
					if (!isBow) // Do not absorb if weapon is of type bow
					{
						if (reflectVamp)
						{
							// Absorb HP from the damage inflicted
							double absorbPercent = getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, target, null) * 2;
							if (absorbPercent > 0)
							{
								if (target instanceof L2Playable)
									absorbPercent /= 2;
								else if (isUndead)
								{
									absorbPercent /= 3;
									absorbPercent /= absNerf;
								}
								else
								{
									if (this instanceof L2PcInstance && getActingPlayer().isCursedWeaponEquipped())
										absorbPercent /= 3;
									absorbPercent /= absNerf;
								}
								int maxCanAbsorb = (int) (getMaxHp() - getCurrentHp());
								int absorbDamage = (int) (absorbPercent / 100. * damage);
								if (absorbDamage > maxCanAbsorb)
									absorbDamage = maxCanAbsorb; // Can't absord more than max hp
								if (absorbDamage > 0)
								{
									if (!isDead() && !target.isDead())
										setCurrentHp(getCurrentHp() + absorbDamage);
								}
							}
							// Absorb CP from the damage inflicted
							absorbPercent = getStat().calcStat(Stats.ABSORB_CP_PERCENT, 0, target, null) * 2;
							if (absorbPercent > 0)
							{
								int maxCanAbsorb = (int) (getMaxCp() - getStatus().getCurrentCp());
								int absorbDamage = (int) (absorbPercent / 100. * damage);
								if (absorbDamage > maxCanAbsorb)
									absorbDamage = maxCanAbsorb; // Can't absorb more than max cp
								if (absorbDamage > 0 && !isDead() && !target.isDead())
									getStatus().setCurrentCp(getStatus().getCurrentCp() + absorbDamage);
							}
							// Absorb MP from the damage inflicted
							absorbPercent = getStat().calcStat(Stats.ABSORB_MANA_DAMAGE_PERCENT, 0, target, null) * 2;
							if (absorbPercent > 0)
							{
								int maxCanAbsorb = (int) (getMaxMp() - getCurrentMp());
								int absorbDamage = (int) (absorbPercent / 100. * damage);
								if (absorbDamage > maxCanAbsorb)
									absorbDamage = maxCanAbsorb; // Can't absord more than max hp
								if (absorbDamage > 0)
								{
									if (!isDead() && !target.isDead())
										setCurrentMp(getCurrentMp() + absorbDamage);
								}
							}
						}
					}
					final int staticAbsorb = (int) calcStat(Stats.ABSORB_DAMAGE_STATIC, 0, target, null);
					if (staticAbsorb > 0)
					{
						if (!isDead() && !target.isDead())
							setCurrentHp(getCurrentHp() + staticAbsorb);
					}
				}
				if (Rnd.get(100) > 20 && !isBehind(target))
					stopEffects(L2EffectType.SILENT_MOVE);
				if (isInvisible())
				{
					if (this instanceof L2PcInstance && getActingPlayer().isGM())
					{}
					else
					{
						if (this instanceof L2PcInstance/* ? Rnd.get(100) > 33 : Rnd.get(100) > 75 */)
							stopEffects(L2EffectType.INVISIBLE);
					}
				}
				// Notify AI with EVT_ATTACKED
				if (target.hasAI() && !(target instanceof L2MerchantSummonInstance))
					target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
				getAI().clientStartAutoAttack();
				if (this instanceof L2Summon)
				{
					L2PcInstance owner = ((L2Summon) this).getOwner();
					if (owner != null)
					{
						owner.getAI().clientStartAutoAttack();
					}
				}
				/*
				 * else if (this instanceof L2Decoy)
				 * {
				 * ((L2Decoy)this).getActingPlayer().getAI().clientStartAutoAttack();
				 * }
				 */
				/*
				 * // Manage attack or cast break of the target (calculating rate, sending message...)
				 * if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				 * {
				 * target.breakAttack();
				 * target.breakCast();
				 * }
				 */
				// Maybe launch chance skills on us
				if (_chanceSkills != null)
					_chanceSkills.onHit(target, false, crit);
				// Maybe launch chance skills on target
				if (target.getChanceSkills() != null)
					target.getChanceSkills().onHit(this, true, crit);
				// Launch weapon Special ability effect if available
				final L2ItemInstance activeWeapon = getActiveWeaponInstance();
				if (activeWeapon != null)
				{
					if (this instanceof L2Playable)
					{
						if (!target.isDebuffable(getActingPlayer()))
							return;
					}
					if (((L2Weapon) activeWeapon.getItem()).getSkillEffects(this, target, crit, activeWeapon) && this instanceof L2PcInstance)
					{
						sendPacket(new ExShowScreenMessage(1, -1, 7, 0, 1, 0, 0, false, 3000, 0, target.getDisplayName() + " is affected by weapon special ability!"));
					}
				}
				if (target instanceof L2Playable)
				{
					if (this instanceof L2EventMapGuardInstance)
					{
						doDie(target);
					}
					else
					{
						final int knockbackChance = (int) calcStat(Stats.KNOCKBACK_CHANCE, 0, target, null);
						if (knockbackChance > 0)
						{
							L2PcInstance player = target.getActingPlayer();
							if (player != null)
							{
								if (!player.canBeKnockedBack(0))
								{
									sendMessage(player.getDisplayName() + " is not affected by your weapon knockback because of 15 second knockback protection");
									return;
								}
							}
							else
								return;
							if (Rnd.get(100) < knockbackChance)
							{
								doKnockback(target, null);
							}
						}
					}
				}
			}
			return;
		}
		if (!isCastingNow() && !isCastingSimultaneouslyNow())
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public void doKnockback(final L2Character target, final L2Skill skill)
	{
		if (target.isInvul())
			return;
		if (this instanceof L2PcInstance)
			if (!target.isDebuffable((L2PcInstance) this))
				return;
		if (target.getKnockedbackTimer() >= GameTimeController.getGameTicks()) // in process of being knocked back by another thing
			return;
		if (Rnd.get(100) < target.calcStat(Stats.KNOCKBACK_RESIST, 0, null, null))
			return;
		if (target instanceof L2PcInstance)
		{
			/*
			 * if (!target.getActingPlayer().canBeKnockedBack(getActingPlayer() != null ? getActingPlayer().getObjectId() : 0))
			 * return;
			 */
			target.getActingPlayer().setKnockbackProtectionTime(getActingPlayer() != null ? getActingPlayer().getObjectId() : 0);
		}
		if (target instanceof L2Playable)
			target.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		target.abortAttack();
		target.abortCast();
		if (target.isMoving())
			target.broadcastPacket(new StopMove(target));
		if (skill != null)
			target.setKnockedbackTimer(GameTimeController.getGameTicks() + 18);
		else
			target.setKnockedbackTimer(GameTimeController.getGameTicks() + 18);
		int distance = 20;
		if (skill != null)
			distance = skill.getFlyRadius();
		if (distance < 1)
			distance = 20;
		distance += calcStat(Stats.KNOCKBACK_DISTANCE_ADD, 0, null, null);
		final double angle = Util.calculateAngleFrom(this, target);
		final int dx = (int) (Math.cos(Math.toRadians(angle)) * distance);
		final int dy = (int) (Math.sin(Math.toRadians(angle)) * distance);
		final int sx = target.getX();
		final int sy = target.getY();
		final int sz = target.getZ();
		int x = sx + dx;
		int y = sy + dy;
		int z = sz;
		if (Config.GEODATA > 0)
		{
			int x1, y1, z1;
			Location destiny = GeoData.getInstance().moveCheck(sx, sy, sz, x, y, z, target.getInstanceId());
			x1 = destiny.getX();
			y1 = destiny.getY();
			z1 = destiny.getZ();
			if (x != x1 || y != y1)
			{
				target.reduceCurrentHp(distance * 7, this, true, false, skill, true);
				x = x1;
				y = y1;
				z = z1;
				sendMessage(target.getDisplayName() + " took " + distance * 7 + " damage from hitting a wall during knockback");
			}
		}
		if (!target.isMovementDisabledForKnockback())
		{
			target.broadcastPacket(new StopMove(target));
			final int id = target.getObjectId();
			if (skill != null)
				target.broadcastPacket(new FlyToLocation(id, sx, sy, sz, x, y, z, FlyType.valueOf(skill.getKnockbackType())));
			else
				target.broadcastPacket(new FlyToLocation(id, sx, sy, sz, x, y, z, getAttackKnockbackType(target)));
			target.getPosition().setXYZ(x, y, z);
			final int hamstring = (int) target.calcStat(Stats.HAMSTRING, 0, this, skill);
			if (hamstring > 0)
			{
				distance = (int) Util.calculateDistance(sx, sy, sz, x, y);
				target.reduceCurrentHp(hamstring * distance, this, true, true, null, true);
				sendMessage("You caused an additional " + hamstring * distance + " damage to " + target.getDisplayName() + " due to hemorrhage");
			}
		}
	}
	
	private FlyType getAttackKnockbackType(L2Character target)
	{
		switch ((int) calcStat(Stats.KNOCKBACK_TYPE, 0, target, null))
		{
			case 1:
				return FlyType.THROW_UP;
			case 2:
				return FlyType.CHARGE;
			case 3:
				return FlyType.DUMMY;
		}
		return FlyType.THROW_HORIZONTAL;
	}
	
	public boolean isDebuffable(L2PcInstance attacker)
	{
		return false;
	}
	
	/**
	 * Break an attack and send Server->Client ActionFailed packet and a System Message to the L2Character.<BR>
	 * <BR>
	 */
	public void breakAttack()
	{
		breakAttack(null);
	}
	
	public void breakAttack(L2Character attacker)
	{
		if (isAttackingNow())
		{
			// Abort the attack of the L2Character and send Server->Client ActionFailed packet
			abortAttack();
			if (attacker != null && attacker instanceof L2Playable)
				attacker.getActingPlayer().sendMessage("Interrupted target's attack!");
			if (this instanceof L2PcInstance)
			{
				// Send a system message
				sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
			}
		}
	}
	
	public void breakCast()
	{
		breakCast(null);
	}
	
	public void breakCast(L2Character attacker)
	{
		// damage can only cancel magical skills
		if (isCastingNow() && canAbortCast() && getLastSkillCast() != null && getLastSkillCast().isMagic())
		{
			if (attacker != null && attacker instanceof L2Playable)
				attacker.getActingPlayer().sendMessage("Interrupted target's " + getLastSkillCast().getName() + "!");
			// Abort the cast of the L2Character and send Server->Client MagicSkillCanceld/ActionFailed packet.
			abortCast();
			if (this instanceof L2PcInstance)
			{
				// Send a system message
				sendPacket(new SystemMessage(SystemMessageId.CASTING_INTERRUPTED));
			}
		}
	}
	
	/**
	 * Reduce the arrow number of the L2Character.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	protected void reduceArrowCount(boolean bolts)
	{
		// default is to do nothing
	}
	
	/**
	 * Return True if inside peace zone.<BR>
	 * <BR>
	 */
	public boolean isInsidePeaceZone(L2PcInstance attacker)
	{
		return isInsidePeaceZone(attacker, this);
	}
	
	public boolean isInsidePeaceZone(L2PcInstance attacker, L2Object target)
	{
		return (!attacker.getAccessLevel().allowPeaceAttack() && isInsidePeaceZone((L2Object) attacker, target));
	}
	
	public boolean isInsidePeaceZone(L2Object attacker, L2Object target)
	{
		if (target == null)
			return false;
		if (!(target instanceof L2Playable && attacker instanceof L2Playable))
			return false;
		if (InstanceManager.getInstance().getInstance(getInstanceId()).isPvPInstance())
			return false;
		if (!isInFunEvent() || !attacker.isInFunEvent())
		{
			if (Config.ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE)
			{
				// allows red to be attacked and red to attack flagged players
				if (target.getActingPlayer() != null && target.getActingPlayer().getKarma() > 0)
					return false;
				if (attacker.getActingPlayer() != null && attacker.getActingPlayer().getKarma() > 0 && target.getActingPlayer() != null && target.getActingPlayer().getPvpFlagLasts() >= System.currentTimeMillis())
					return false;
			}
			return (isInsideZone(ZONE_PEACE) || ((L2Character) attacker).isInsideZone(ZONE_PEACE));
		}
		return false;
	}
	
	/**
	 * return true if this character is inside an active grid.
	 */
	public Boolean isInActiveRegion()
	{
		L2WorldRegion region = getWorldRegion();
		return ((region != null) && (region.isActive()));
	}
	
	/**
	 * Return True if the L2Character has a Party in progress.<BR>
	 * <BR>
	 */
	public boolean isInParty()
	{
		return false;
	}
	
	/**
	 * Return the L2Party object of the L2Character.<BR>
	 * <BR>
	 */
	public L2Party getParty()
	{
		return null;
	}
	
	/**
	 * Return the Attack Speed of the L2Character (delay (in milliseconds) before next attack).<BR>
	 * <BR>
	 */
	@SuppressWarnings("incomplete-switch")
	public int calculateTimeBetweenAttacks(L2Character target, L2Weapon weapon)
	{
		final double atkSpd = getPAtkSpd(null);
		if (weapon != null && !isTransformed())
		{
			switch (weapon.getItemType())
			{
				case BOW:
					return (int) (680000 / atkSpd);
				case CROSSBOW:
					return (int) (538500 / atkSpd);
			}
		}
		return Formulas.calcPAtkSpd(this, target, atkSpd);
	}
	
	public int calculateReuseTime(L2Character target, L2Weapon weapon)
	{
		if (weapon == null || isTransformed())
			return 0;
		int reuse = weapon.getAttackReuseDelay();
		if (reuse == 0)
			return 0;
		reuse *= getStat().getWeaponReuseModifier(target);
		final double atkSpd = getStat().getPAtkSpd(null);
		switch (weapon.getItemType())
		{
			case BOW:
			case CROSSBOW:
				return (int) (reuse * 415 / atkSpd);
			default:
				return (int) (reuse * 350 / atkSpd);
		}
	}
	
	/**
	 * Return True if the L2Character use a dual weapon.<BR>
	 * <BR>
	 */
	public boolean isUsingDualWeapon()
	{
		return false;
	}
	
	/**
	 * Add a skill to the L2Character _skills and its Func objects to the calculator set of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li>
	 * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the L2Character</li><BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance : Save update in the character_skills table of the database</li><BR>
	 * <BR>
	 *
	 * @param newSkill
	 *            The L2Skill to add to the L2Character
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	public void addSkill(int i)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(i, 1);
		if (skill != null)
			addSkill(skill);
	}
	
	public L2Skill addSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;
		if (newSkill != null)
		{
			/*
			 * if (newSkill.isPassive() && newSkill.getSkillType() != L2SkillType.NOTDONE && newSkill.getTargetType(this) != SkillTargetType.TARGET_SELF)
			 * {
			 * if (this instanceof L2PcInstance)
			 * {
			 * newSkill.setOwnerObjId(getObjectId());
			 * getActingPlayer().addSkillToPartyPassiveList(newSkill);
			 * }
			 * }
			 */
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);
			// If an old skill has been replaced, remove all its Func objects
			if (oldSkill != null)
			{
				// if skill came with another one, we should delete the other one too.
				if ((oldSkill.triggerAnotherSkill()))
				{
					removeSkill(oldSkill.getTriggeredId(), true);
				}
				removeStatsOwner(oldSkill);
			}
			// Add Func objects of newSkill to the calculator set of the L2Character
			addStatFuncs(newSkill.getStatFuncs(null, this));
			if (oldSkill != null && _chanceSkills != null)
			{
				removeChanceSkill(oldSkill.getId());
			}
			if (newSkill.isChance())
			{
				addChanceSkill(newSkill);
			}
		}
		return oldSkill;
	}
	
	public void addSkillPartyPassive(L2Skill newSkill)
	{
		if (newSkill == null)
			return;
		L2Skill oldSkill = getKnownSkill(newSkill.getId());
		if (oldSkill != null)
			return;
		// Replace oldSkill by newSkill or Add the newSkill
		oldSkill = _skills.put(newSkill.getId(), newSkill);
		// If an old skill has been replaced, remove all its Func objects
		if (oldSkill != null) // should be null here
		{
			// if skill came with another one, we should delete the other one too.
			if ((oldSkill.triggerAnotherSkill()))
			{
				removeSkill(oldSkill.getTriggeredId(), true);
			}
			removeStatsOwner(oldSkill);
		}
		// Add Func objects of newSkill to the calculator set of the L2Character
		addStatFuncs(newSkill.getStatFuncs(null, this));
		if (oldSkill != null && _chanceSkills != null)
		{
			removeChanceSkill(oldSkill.getId());
		}
		if (newSkill.isChance())
		{
			addChanceSkill(newSkill);
		}
	}
	
	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character.<BR>
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
	public L2Skill removeSkill(L2Skill skill)
	{
		if (skill == null)
			return null;
		return removeSkill(skill.getId(), true);
	}
	
	public L2Skill removeSkill(L2Skill skill, boolean cancelEffect)
	{
		if (skill == null)
			return null;
		// Remove the skill from the L2Character _skills
		return removeSkill(skill.getId(), cancelEffect);
	}
	
	public L2Skill removeSkill(int skillId)
	{
		return removeSkill(skillId, true);
	}
	
	public void removeSkillPartyPassive(int skillId, int ownerId)
	{
		L2Skill oldSkill = getKnownSkill(skillId);
		if (oldSkill != null)
		{
			/*
			 * if (oldSkill.getOwnerObjId() != 0) //is a party passive
			 * {
			 * if (oldSkill.getOwnerObjId() != ownerId) //the skill is actually not given by the original skill owner
			 * return;
			 * }
			 * else
			 * {
			 * _log.warning("WTF LOL removeSkillPartyPassive tried removing a skill w/o owner id!!!!!!!!");
			 * }
			 */
			oldSkill = _skills.remove(skillId);
			// this is just a fail-safe againts buggers and gm dummies...
			if ((oldSkill.triggerAnotherSkill()) && oldSkill.getTriggeredId() > 0)
			{
				removeSkill(oldSkill.getTriggeredId(), true);
			}
			// does not abort casting of the transformation dispell
			if (oldSkill.getSkillType() != L2SkillType.TRANSFORMDISPEL)
			{
				// Stop casting if this skill is used right now
				if (getLastSkillCast() != null && isCastingNow())
				{
					if (oldSkill.getId() == getLastSkillCast().getId())
						abortCast();
				}
				if (getLastSimultaneousSkillCast() != null && isCastingSimultaneouslyNow())
				{
					if (oldSkill.getId() == getLastSimultaneousSkillCast().getId())
						abortCast();
				}
			}
			removeStatsOwner(oldSkill, true);
			stopSkillEffects(oldSkill.getId());
			if (oldSkill instanceof L2SkillAgathion && this instanceof L2PcInstance && ((L2PcInstance) this).getAgathionId() > 0)
			{
				((L2PcInstance) this).setAgathionId(0);
				((L2PcInstance) this).broadcastUserInfo();
			}
			if (oldSkill instanceof L2SkillMount && this instanceof L2PcInstance && ((L2PcInstance) this).isMounted())
				((L2PcInstance) this).dismount();
			if (oldSkill.isChance() && _chanceSkills != null)
			{
				removeChanceSkill(oldSkill.getId());
			}
			if (oldSkill instanceof L2SkillSummon && oldSkill.getId() == 710 && this instanceof L2PcInstance && ((L2PcInstance) this).getPet() != null && ((L2PcInstance) this).getPet().getNpcId() == 14870)
			{
				((L2PcInstance) this).getPet().unSummon(((L2PcInstance) this));
			}
		}
	}
	
	public L2Skill removeSkill(int skillId, boolean cancelEffect)
	{
		// Remove the skill from the L2Character _skills
		L2Skill oldSkill = _skills.remove(skillId);
		// Remove all its Func objects from the L2Character calculator set
		if (oldSkill != null)
		{
			/*
			 * if (oldSkill.getOwnerObjId() != 0) //is a party passive
			 * {
			 * if (oldSkill.getOwnerObjId() == getObjectId())
			 * getActingPlayer().removeSkillFromPartyPassiveList(oldSkill);
			 * else
			 * {
			 * _log.warning("WTF????????? removeSkill removed a skill that has owner obj id but is not the same as owner's WTF?");
			 * }
			 * }
			 */
			// this is just a fail-safe againts buggers and gm dummies...
			if ((oldSkill.triggerAnotherSkill()) && oldSkill.getTriggeredId() > 0)
			{
				removeSkill(oldSkill.getTriggeredId(), true);
			}
			// does not abort casting of the transformation dispell
			if (oldSkill.getSkillType() != L2SkillType.TRANSFORMDISPEL)
			{
				// Stop casting if this skill is used right now
				if (getLastSkillCast() != null && isCastingNow())
				{
					if (oldSkill.getId() == getLastSkillCast().getId())
						abortCast();
				}
				if (getLastSimultaneousSkillCast() != null && isCastingSimultaneouslyNow())
				{
					if (oldSkill.getId() == getLastSimultaneousSkillCast().getId())
						abortCast();
				}
			}
			if (cancelEffect || oldSkill.isToggle())
			{
				/*
				 * // for now, to support transformations, we have to let their effects stay when skill is removed
				 * final L2Effect e = getFirstEffect(oldSkill);
				 * if (e == null || e.getEffectType() != L2EffectType.TRANSFORMATION)
				 * {
				 */
				removeStatsOwner(oldSkill);
				stopSkillEffects(oldSkill.getId());
				/* } */
			}
			if (oldSkill instanceof L2SkillAgathion && this instanceof L2PcInstance && ((L2PcInstance) this).getAgathionId() > 0)
			{
				((L2PcInstance) this).setAgathionId(0);
				((L2PcInstance) this).broadcastUserInfo();
			}
			if (oldSkill instanceof L2SkillMount && this instanceof L2PcInstance && ((L2PcInstance) this).isMounted())
				((L2PcInstance) this).dismount();
			if (oldSkill.isChance() && _chanceSkills != null)
			{
				removeChanceSkill(oldSkill.getId());
			}
			if (oldSkill instanceof L2SkillSummon && oldSkill.getId() == 710 && this instanceof L2PcInstance && ((L2PcInstance) this).getPet() != null && ((L2PcInstance) this).getPet().getNpcId() == 14870)
			{
				((L2PcInstance) this).getPet().unSummon(((L2PcInstance) this));
			}
		}
		return oldSkill;
	}
	
	public synchronized void addChanceSkill(L2Skill skill)
	{
		if (_chanceSkills == null)
			_chanceSkills = new ChanceSkillList(this);
		_chanceSkills.put(skill, skill.getChanceCondition());
	}
	
	public synchronized void removeChanceSkill(int id)
	{
		if (_chanceSkills == null)
			return;
		for (IChanceSkillTrigger trigger : _chanceSkills.keySet())
		{
			if (!(trigger instanceof L2Skill))
				continue;
			L2Skill skill = (L2Skill) trigger;
			if (skill.getId() == id)
				_chanceSkills.remove(skill);
		}
		if (_chanceSkills.isEmpty())
			_chanceSkills = null;
	}
	
	public synchronized void addChanceEffect(EffectChanceSkillTrigger effect)
	{
		if (_chanceSkills == null)
			_chanceSkills = new ChanceSkillList(this);
		_chanceSkills.put(effect, effect.getTriggeredChanceCondition());
	}
	
	public synchronized void removeChanceEffect(EffectChanceSkillTrigger effect)
	{
		if (_chanceSkills == null)
			return;
		_chanceSkills.remove(effect);
		if (_chanceSkills.isEmpty())
			_chanceSkills = null;
	}
	
	/**
	 * Return all skills own by the L2Character in a table of L2Skill.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2Character are identified in <B>_skills</B> the L2Character <BR>
	 * <BR>
	 */
	public final L2Skill[] getAllSkills()
	{
		if (_skills == null)
			return new L2Skill[0];
		return _skills.values().toArray(new L2Skill[_skills.values().size()]);
	}
	
	public ChanceSkillList getChanceSkills()
	{
		return _chanceSkills;
	}
	
	/**
	 * Return the level of a skill owned by the L2Character.<BR>
	 * <BR>
	 *
	 * @param skillId
	 *            The identifier of the L2Skill whose level must be returned
	 * @return The level of the L2Skill identified by skillId
	 */
	public int getSkillLevel(int skillId)
	{
		final L2Skill skill = getKnownSkill(skillId);
		if (skill == null)
			return -1;
		return skill.getLevel();
	}
	
	/**
	 * Return True if the skill is known by the L2Character.<BR>
	 * <BR>
	 *
	 * @param skillId
	 *            The identifier of the L2Skill to check the knowledge
	 */
	public final L2Skill getKnownSkill(int skillId)
	{
		if (_skills == null)
			return null;
		return _skills.get(skillId);
	}
	
	/**
	 * Return the number of buffs affecting this L2Character.<BR>
	 * <BR>
	 *
	 * @return The number of Buffs affecting this L2Character
	 */
	public int getBuffCount()
	{
		return _effects.getBuffCount();
	}
	/*
	 * public int getDanceCount()
	 * {
	 * return _effects.getDanceCount();
	 * }
	 */
	
	/**
	 * Manage the magic skill launching task (MP, HP, Item consummation...) and display the magic skill animation on client.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet MagicSkillLaunched (to display magic skill animation) to all L2PcInstance of L2Charcater _knownPlayers</li>
	 * <li>Consumme MP, HP and Item if necessary</li>
	 * <li>Send a Server->Client packet StatusUpdate with MP modification to the L2PcInstance</li>
	 * <li>Launch the magic skill in order to calculate its effects</li>
	 * <li>If the skill type is PDAM, notify the AI of the target with AI_INTENTION_ATTACK</li>
	 * <li>Notify the AI of the L2Character with EVT_FINISH_CASTING</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : A magic skill casting MUST BE in progress</B></FONT><BR>
	 * <BR>
	 *
	 * @param skill
	 *            The L2Skill to use
	 */
	public void onMagicLaunchedTimer(MagicUseTask mut)
	{
		final L2Skill skill = mut._skill;
		L2Object[] targets = mut._targets;
		if (skill == null || targets == null || targets.length <= 0)
		{
			abortCast();
			return;
		}
		// Escaping from under skill's radius and peace zone check. First version, not perfect in AoE skills.
		int escapeRange = 0;
		if (!mut._simultaneously)
		{
			if (_distanceToTargetCurrSkill >= 1)
			{
				if (this instanceof L2Playable && getActingPlayer().isInOlympiadMode())
					escapeRange = (int) (_distanceToTargetCurrSkill * 1.6 + 100);
				else
					escapeRange = (int) Math.max((_distanceToTargetCurrSkill * 1.5) + 250, escapeRange);
				_distanceToTargetCurrSkill = 0;
			}
			else
			{
				if (this instanceof L2Playable && getActingPlayer().isInOlympiadMode())
					escapeRange = (int) (skill.getSkillRadius(this) * 1.5);
				else
					escapeRange = (int) Math.max((skill.getSkillRadius(this) * 1.5) + 40, escapeRange);
			}
		}
		else
		{
			int castRange = skill.getCastRange(this);
			if (castRange >= 1)
			{
				if (this instanceof L2Playable && getActingPlayer().isInOlympiadMode())
					escapeRange = (int) (castRange * 1.4 + 30);
				else
					escapeRange = (int) Math.max((castRange * 1.5) + 225, escapeRange);
			}
			else
			{
				if (this instanceof L2Playable && getActingPlayer().isInOlympiadMode())
					escapeRange = (int) (skill.getSkillRadius(this) * 1.5);
				else
					escapeRange = (int) Math.max((skill.getSkillRadius(this) * 1.5) + 40, escapeRange);
			}
		}
		final boolean targetArea = skill.getTargetType(this) == SkillTargetType.TARGET_AREA;
		if (escapeRange > 0)
		{
			List<L2Character> targetList = new FastList<L2Character>();
			for (L2Object target : targets)
			{
				if (target instanceof L2Character)
				{
					if (targetArea && target != targets[0])
					{
						if ((!Util.checkIfInRange((int) (skill.getSkillRadius(this) * 1.4), targets[0], target, true) || !GeoData.getInstance().canSeeTarget(targets[0], target)))
							continue;
					}
					else if ((!Util.checkIfInRange(escapeRange, this, target, true) || !GeoData.getInstance().canSeeTarget(this, target)))
						continue;
					if (skill.isOffensive() && !skill.isNeutral())
					{
						if (this instanceof L2PcInstance)
						{
							if (((L2Character) target).isInsidePeaceZone((L2PcInstance) this))
								continue;
						}
						else
						{
							if (((L2Character) target).isInsidePeaceZone(this, target))
								continue;
						}
					}
					targetList.add((L2Character) target);
				}
			}
			if (targetList.isEmpty())
			{
				abortCast();
				return;
			}
			else
				targets = targetList.toArray(new L2Character[targetList.size()]);
		}
		// Ensure that a cast is in progress
		// Check if player is using fake death.
		// Potions can be used while faking death.
		if ((mut._simultaneously && !isCastingSimultaneouslyNow()) || (!mut._simultaneously && !isCastingNow()) || (isAlikeDead() && !skill.isPotion()))
		{
			// now cancels both, simultaneous and normal
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		if (!skill.isToggle())
		{
			if (Config.ENABLE_SKILL_ANIMATIONS)
			{
				if (this instanceof L2PcInstance)
				{
					L2PcInstance player = this.getActingPlayer();
					if (player._inEventTvT || player._inEventFOS || player._inEventCTF || player._inEventLunaDomi || player.isInSiege() /* || player.isGM() */)
					{
						int chance = Rnd.get(100);
						if (chance <= Config.CHANCE_EFFECT_DISPLAY)
						{
							broadcastPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getLevel(), targets));
						}
					}
					else
					{
						broadcastPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getLevel(), targets));
					}
				}
				else
					broadcastPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getLevel(), targets));
			}
		}
		mut._phase = 2;
		if (mut._hitTime == 0)
			onMagicHitTimer(mut);
		else
			_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, 400);
	}
	
	/*
	 * Runs in the end of skill casting
	 */
	public void onMagicHitTimer(MagicUseTask mut)
	{
		final L2Skill skill = mut._skill;
		L2Object[] targets = mut._targets;
		if (skill == null || mut._targets == null || mut._targets.length <= 0)
		{
			abortCast();
			return;
		}
		if (mut._count > 0)
		{
			int escapeRange = skill.getEffectRange(this);
			int castRange = skill.getCastRange(this);
			final boolean targetArea = skill.getTargetType(this) == SkillTargetType.TARGET_AREA;
			if (castRange > 0 && !targetArea)
				escapeRange = (int) Math.max(castRange * 1.6, escapeRange);
			else
				escapeRange = (int) (skill.getSkillRadius(this) * 1.15);
			if (escapeRange > 0)
			{
				List<L2Character> targetList = new FastList<L2Character>();
				if (skill.getTargetType(this) == SkillTargetType.TARGET_ONE)
				{
					if ((!Util.checkIfInRange(escapeRange, this, targets[0], true) || !GeoData.getInstance().canSeeTarget(this, targets[0])))
					{
						abortCast();
						return;
					}
					targetList.add((L2Character) targets[0]);
				}
				else
				{
					if (skill.isFollowTarget())
					{
						mut.x = targets[0].getX();
						mut.y = targets[0].getY();
						mut.z = targets[0].getZ();
					}
					for (L2Object target : getKnownList().getKnownCharacters())
					{
						if (target instanceof L2Character)
						{
							if (!target.isAutoAttackable(this))
								continue;
							if (this instanceof L2Playable)
							{
								if (target instanceof L2Playable)
								{
									if (!getActingPlayer().checkAOEPvPSkill(target.getActingPlayer(), skill))
										continue;
								}
								else if (!canAttackDueToSoloMob((L2Character) target))
									continue;
							}
							if (!((L2Character) target).isInsideRadius(mut.x, mut.y, mut.z, escapeRange, false, false))
								continue;
							if (skill.isOffensive() && !skill.isNeutral())
							{
								if (this instanceof L2PcInstance)
								{
									if (((L2Character) target).isInsidePeaceZone((L2PcInstance) this))
										continue;
								}
								else
								{
									if (((L2Character) target).isInsidePeaceZone(this, target))
										continue;
								}
							}
							targetList.add((L2Character) target);
							if (targetList.size() > 15)
								break;
						}
					}
				}
				targets = targetList.toArray(new L2Character[targetList.size()]);
			}
			if (mut._count < skill.getHitCounts())
				doSkillSoulShotCharge(skill);
		}
		if (getFusionSkill() != null)
		{
			if (mut._simultaneously)
			{
				_skillCast2 = null;
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				_skillCast = null;
				setIsCastingNow(false);
			}
			getFusionSkill().onCastAbort();
			if (targets != null)
				notifyQuestEventSkillFinished(skill, targets[0]);
			return;
		}
		L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
		if (mog != null)
		{
			if (mut._simultaneously)
			{
				_skillCast2 = null;
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				_skillCast = null;
				setIsCastingNow(false);
			}
			mog.exit();
			if (targets != null)
				notifyQuestEventSkillFinished(skill, targets[0]);
			return;
		}
		// Get the display identifier of the skill
		int magicId = skill.getDisplayId();
		// Get the level of the skill
		int level = getSkillLevel(skill.getId());
		if (level < 1)
			level = 1;
		// Send a Server->Client packet MagicSkillLaunched to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
		if (!skill.isPotion() && targets != null)
			broadcastPacket(new MagicSkillLaunched(this, magicId, skill.getDisplayLvl(), targets));
		// sendMessage("id:"+ skill.getId() +" Level: " + skill.getLevel() + " hittime: "+ Formulas.calcSkillCastTime(this, skill, skill.getHitTime()));
		try
		{
			// Go through targets table
			for (L2Object tgt : targets)
			{
				if (tgt instanceof L2Playable)
				{
					L2Character target = (L2Character) tgt;
					/*
					 * if (skill.getSkillType() == L2SkillType.BUFF)
					 * {
					 * SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					 * smsg.addSkillName(skill);
					 * target.sendPacket(smsg);
					 * }
					 */
					if (this instanceof L2PcInstance && target instanceof L2Summon)
					{
						((L2Summon) target).updateAndBroadcastStatus(1);
					}
				}
			}
			StatusUpdate su = new StatusUpdate(getObjectId());
			boolean isSendStatus = true;
			if (!isGM())
			{
				// Consume MP of the L2Character and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
				double mpConsume = getStat().getMpConsume(skill);
				if (mpConsume > 0)
				{
					if (skill.isHeal())
					{
						if (this instanceof L2PcInstance)
						{
							if (getActingPlayer().isInGludin())
								mpConsume *= 3.6;
							else if (getActingPlayer().isInFunEvent())
								mpConsume *= Config.EVENT_MPCONSUME_MUL;
							else if (getActingPlayer().getPvpFlag() != 0 || getActingPlayer().getKarma() > 0)
								mpConsume *= 3.5;
							else if (getActingPlayer().isInOlympiadMode())
								mpConsume *= 6;
							else if (getActingPlayer().isInsideZone(L2Character.ZONE_PVP))
								mpConsume *= 3.5;
							else
								mpConsume *= 2;
							mpConsume *= 1.3;
						}
					}
					if (skill.isDance())
					{
						getStatus().reduceMp(calcStat(Stats.DANCE_MP_CONSUME_RATE, mpConsume, null, skill));
					}
					else if (skill.isMagic())
					{
						getStatus().reduceMp(calcStat(Stats.MAGICAL_MP_CONSUME_RATE, mpConsume, null, skill));
					}
					else
					{
						getStatus().reduceMp(calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, mpConsume, null, skill));
					}
					su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
					isSendStatus = true;
				}
				// Consume HP if necessary and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
				if (skill.getHpConsume() > 0)
				{
					double consumeHp;
					consumeHp = calcStat(Stats.HP_CONSUME_RATE, skill.getHpConsume(), null, skill);
					if (consumeHp + 1 >= getCurrentHp())
						consumeHp = getCurrentHp() - 1.0;
					getStatus().reduceHp(consumeHp, this, true);
					su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
					isSendStatus = true;
				}
				if (skill.getHpConsumePercent() > 0)
				{
					int reducedHp = (int) (calcStat(Stats.HP_CONSUME_RATE, skill.getHpConsumePercent(), null, skill) * getCurrentHp() / 1000);
					if (reducedHp < 1)
						reducedHp = 1;
					if (getCurrentHp() <= reducedHp)
						reducedHp = (int) (getCurrentHp() - 1);
					if (reducedHp >= 1)
					{
						getStatus().reduceHp(reducedHp, this, true);
						su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
						isSendStatus = true;
					}
				}
				// Consume CP if necessary and Send the Server->Client packet StatusUpdate with current CP/HP and MP to all other L2PcInstance to inform
				if (skill.getCpConsume() > 0)
				{
					double consumeCp;
					consumeCp = skill.getCpConsume();
					if (consumeCp + 1 >= getCurrentHp())
						consumeCp = getCurrentHp() - 1.0;
					getStatus().reduceCp((int) consumeCp);
					su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
					isSendStatus = true;
				}
				// Send a Server->Client packet StatusUpdate with MP modification to the L2PcInstance
				if (isSendStatus)
					sendPacket(su);
				// Consume Items if necessary and Send the Server->Client packet InventoryUpdate with Item modification to all the L2Character
				if (skill.getItemConsume() > 0)
				{
					boolean jump = false;
					for (int skills : CERTI_SKILLS)
					{
						if (skill.getId() == skills)
						{
							jump = true;
							return;
						}
					}
					if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, false) && !jump)
					{
						sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
						abortCast();
						return;
					}
				}
			}
			if (this instanceof L2PcInstance)
			{
				int charges = ((L2PcInstance) this).getCharges();
				// check for charges
				if (!isGM())
				{
					if (charges < skill.getRequiredCharges() || (skill.getMaxCharges() == 0 && charges < skill.getNumCharges()))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
						sm.addSkillName(skill);
						sendPacket(sm);
						abortCast();
						return;
					}
				}
				// generate charges if any
				if (skill.getNumCharges() > 0)
				{
					if (skill.getMaxCharges() > 0)
						((L2PcInstance) this).increaseCharges(skill.getNumCharges(), (int) (calcStat(Stats.CHARGE_MAX_ADD, skill.getMaxCharges(), null, skill)));
					else
						((L2PcInstance) this).decreaseCharges((int) (skill.getNumCharges() - calcStat(Stats.CHARGE_REDUCE, 0, null, skill)));
				}
				// Consume Souls if necessary
				if (skill.getSoulConsumeCount() > 0 || skill.getMaxSoulConsumeCount(this) > 0)
				{
					if (!isGM())
						if (!((L2PcInstance) this).decreaseSouls(skill.getSoulConsumeCount() > 0 ? skill.getSoulConsumeCount() : skill.getMaxSoulConsumeCount(this), skill))
							return;
				}
			}
			// On each repeat restore shots before cast
			/*
			 * if (mut.count > 0)
			 * {
			 * final L2ItemInstance weaponInst = getActiveWeaponInstance();
			 * if (weaponInst != null)
			 * {
			 * if (mut.skill.useSoulShot())
			 * weaponInst.setChargedSoulshot(mut.shots);
			 * else if (mut.skill.useSpiritShot())
			 * weaponInst.setChargedSpiritshot(mut.shots);
			 * }
			 * }
			 */
			// Launch the magic skill in order to calculate its effects
			if (targets != null)
				callSkill(skill, targets);
		}
		catch (NullPointerException e)
		{}
		finally
		{
			if (mut._hitTime > 0)
			{
				mut._count++;
				if (mut._count < skill.getHitCounts())
				{
					int hitTime = mut._hitTime * skill.getHitTimings()[mut._count] / 100;
					if (mut._simultaneously)
						_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime);
					else
						_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime);
					return;
				}
			}
			mut._phase = 3;
			if (mut._hitTime == 0 || mut._coolTime == 0)
				onMagicFinalizer(mut);
			else
			{
				if (mut._simultaneously)
					_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(mut, mut._coolTime);
				else
					_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, mut._coolTime);
			}
		}
	}
	
	/*
	 * Runs after skill hitTime+coolTime
	 */
	public void onMagicFinalizer(MagicUseTask mut)
	{
		if (mut._simultaneously)
		{
			_skillCast2 = null;
			setIsCastingSimultaneouslyNow(false);
			return;
		}
		else
		{
			_skillCast = null;
			_castInterruptTime = 0;
			setIsCastingNow(false);
		}
		final L2Skill skill = mut._skill;
		final L2Object target = mut._targets.length > 0 ? mut._targets[0] : null;
		if (skill.isSuicideAttack())
		{
			try
			{
				if (this instanceof L2PcInstance)
				{
					if (!((L2PcInstance) this).isGM())
					{
						setIsInvul(false);
					}
				}
				doDie(this);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			if (this instanceof L2Playable)
			{
				if (getAI().getNextIntention() == null && skill.startsAutoAttack())
				{
					if (target != null && target instanceof L2Character && target != this && getTarget() != null && target == getTarget())
					{
						if (target.isAutoAttackable(this))
							getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
					}
				}
			}
		}
		if (skill.isOffensive() && !skill.isNeutral() && !(skill.getSkillType() == L2SkillType.UNLOCK) && !(skill.getSkillType() == L2SkillType.DELUXE_KEY_UNLOCK))
		{
			if (this instanceof L2TrapInstance/* || this instanceof L2Decoy */)
				getActingPlayer().getAI().clientStartAutoAttack();
			else
				getAI().clientStartAutoAttack();
		}
		// Notify the AI of the L2Character with EVT_FINISH_CASTING
		getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
		notifyQuestEventSkillFinished(skill, target);
		/*
		 * If character is a player, then wipe their current cast state and
		 * check if a skill is queued.
		 * If there is a queued skill, launch it and wipe the queue.
		 */
		if (this instanceof L2PcInstance)
		{
			L2PcInstance currPlayer = (L2PcInstance) this;
			SkillDat queuedSkill = currPlayer.getQueuedSkill();
			currPlayer.setCurrentSkill(null, false, false);
			if (queuedSkill != null)
			{
				currPlayer.setQueuedSkill(null, false, false);
				// DON'T USE : Recursive call to useMagic() method
				// currPlayer.useMagic(queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed());
				if (!isAlikeDead())
					ThreadPoolManager.getInstance().executeTask(new QueuedMagicUseTask(currPlayer, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
			}
		}
	}
	
	// Quest event ON_SPELL_FNISHED
	private void notifyQuestEventSkillFinished(L2Skill skill, L2Object target)
	{
		if (this instanceof L2Npc)
		{
			try
			{
				if (((L2NpcTemplate) getTemplate()).getEventQuests(QuestEventType.ON_SPELL_FINISHED) != null)
				{
					L2PcInstance player = target.getActingPlayer();
					for (Quest quest : ((L2NpcTemplate) getTemplate()).getEventQuests(QuestEventType.ON_SPELL_FINISHED))
					{
						quest.notifySpellFinished(((L2Npc) this), player, skill);
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * Enable a skill (remove it from _disabledSkills of the L2Character).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills disabled are identified by their skillId in <B>_disabledSkills</B> of the L2Character <BR>
	 * <BR>
	 *
	 * @param skillId
	 *            The identifier of the L2Skill to enable
	 */
	public void enableSkill(int skillId)
	{
		if (_disabledSkills == null)
			return;
		_disabledSkills.remove(Integer.valueOf(skillId));
		if (this instanceof L2PcInstance)
			removeTimeStamp(skillId);
	}
	
	/**
	 * Disable a skill (add it to _disabledSkills of the L2Character).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills disabled are identified by their skillId in <B>_disabledSkills</B> of the L2Character <BR>
	 * <BR>
	 *
	 * @param skillId
	 *            The identifier of the L2Skill to disable
	 */
	public void disableSkill(int skillId)
	{
		if (_disabledSkills == null)
			_disabledSkills = Collections.synchronizedList(new FastList<Integer>());
		if (!_disabledSkills.contains(skillId))
			_disabledSkills.add(skillId);
	}
	
	/**
	 * Disable this skill id for the duration of the delay in milliseconds.
	 * 
	 * @param skillId
	 * @param delay
	 *            (seconds * 1000)
	 */
	public void disableSkill(int skillId, long delay)
	{
		disableSkill(skillId);
		if (delay > 10)
		{
			ThreadPoolManager.getInstance().scheduleAi(new EnableSkill(skillId), delay);
		}
	}
	
	/**
	 * Check if a skill is disabled.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills disabled are identified by their skillId in <B>_disabledSkills</B> of the L2Character <BR>
	 * <BR>
	 *
	 * @param skillId
	 *            The identifier of the L2Skill to disable
	 */
	public boolean isSkillDisabled(int skillId)
	{
		if (isAllSkillsDisabled())
			return true;
		if (_disabledSkills == null)
			return false;
		return _disabledSkills.contains(skillId);
	}
	
	/**
	 * Disable all skills (set _allSkillsDisabled to True).<BR>
	 * <BR>
	 */
	public void disableAllSkills()
	{
		if (Config.DEBUG)
			_log.fine("all skills disabled");
		_allSkillsDisabled = true;
	}
	
	/**
	 * Enable all skills (set _allSkillsDisabled to False).<BR>
	 * <BR>
	 */
	public void enableAllSkills()
	{
		if (Config.DEBUG)
			_log.fine("all skills enabled");
		_allSkillsDisabled = false;
	}
	
	/**
	 * Launch the magic skill and calculate its effects on each target contained in the targets table.<BR>
	 * <BR>
	 *
	 * @param skill
	 *            The L2Skill to use
	 * @param targets
	 *            The table of L2Object targets
	 */
	public final void callSkill(L2Skill skill, L2Object[] targets)
	{
		try
		{
			// Get the skill handler corresponding to the skill type (PDAM, MDAM, SWEEP...) started in gameserver
			final ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
			final L2ItemInstance activeWeapon = getActiveWeaponInstance();
			// Check if the toggle skill effects are already in progress on the L2Character
			if (skill.isToggle() && getFirstEffect(skill.getId()) != null)
				return;
			final L2PcInstance player = getActingPlayer();
			boolean updatedPvPStatus = false;
			// Initial checks
			for (L2Object trg : targets)
			{
				if (trg instanceof L2Character)
				{
					// Set some values inside target's instance for later use
					final L2Character target = (L2Character) trg;
					// Check if over-hit is possible
					if (skill.isOverhit())
					{
						if (target instanceof L2Attackable)
							((L2Attackable) target).overhitEnabled(true);
					}
					// crafting does not trigger any chance skills // possibly should be unhardcoded
					switch (skill.getSkillType())
					{
						case COMMON_CRAFT:
						case DWARVEN_CRAFT:
							break;
						default:
							// Launch weapon Special ability skill effect if available
							if (activeWeapon != null && !target.isDead() && !target.isInvul())
							{
								if (((L2Weapon) activeWeapon.getItem()).getSkillEffects(this, target, skill, activeWeapon) && this instanceof L2PcInstance)
								{
									sendPacket(new ExShowScreenMessage(1, -1, 7, 0, 1, 0, 0, false, 3000, 0, target.getDisplayName() + " is affected by weapon special ability!"));
								}
							}
							// Maybe launch chance skills on us
							if (_chanceSkills != null)
								_chanceSkills.onSkillHit(target, false, skill);
							// Maybe launch chance skills on target
							if (target.getChanceSkills() != null)
								target.getChanceSkills().onSkillHit(this, true, skill);
					}
					if (player != null)
					{
						if (skill.isNeutral())
						{}
						else if (skill.isOffensive())
						{
							if (target instanceof L2PcInstance || target instanceof L2Summon || target instanceof L2Decoy)
							{
								// Signets are a special case, casted on target_self but don't harm self
								if (skill.getSkillType() != L2SkillType.SIGNET && skill.getSkillType() != L2SkillType.SIGNET_CASTTIME)
								{
									if (skill.getSkillType() != L2SkillType.AGGREDUCE && skill.getSkillType() != L2SkillType.AGGREDUCE_CHAR && skill.getSkillType() != L2SkillType.AGGREMOVE)
									{
										if (target.hasAI())
											target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player);
									}
									if (target instanceof L2PcInstance)
									{
										final L2PcInstance playa = target.getActingPlayer();
										playa.getAI().clientStartAutoAttack();
										if (skill.isDamaging() && playa.getTarget() == null && !playa.isSelectingTarget() && !player.isInvisible() && this != target)
										{
											if (!playa.isOutOfControl() && !playa.isLockedTarget() && !(this instanceof L2TrapInstance))
											{
												final int ignore = (int) player.calcStat(Stats.IGNORE_AUTOTARGET_SKILL, 0, playa, skill);
												if (ignore == 0 || (ignore < 100 && ignore < Rnd.get(100)))
												{
													playa.setIsSelectingTarget(3);
													playa.setTarget(this);
												}
											}
										}
									}
									else if (target instanceof L2Summon)
									{
										L2PcInstance owner = ((L2Summon) target).getOwner();
										if (owner != null)
										{
											owner.getAI().clientStartAutoAttack();
										}
									}
									else if (target instanceof L2Decoy)
									{
										((L2Decoy) target).broadcastPacket(new AutoAttackStart(target.getObjectId()));
									}
									if (!updatedPvPStatus && target.getActingPlayer() != getActingPlayer())
									{
										updatedPvPStatus = true;
										player.updatePvPStatus(target);
									}
								}
							}
							else if (target instanceof L2Attackable)
							{
								if (target.isAPC())
								{
									if (!updatedPvPStatus)
									{
										FakePc fpc = FakePcsTable.getInstance().getFakePc(((L2Npc) target).getNpcId());
										if (fpc.pvpFlag > 0)
										{
											updatedPvPStatus = true;
											player.updatePvPStatus(target);
										}
									}
									target.getAI().clientStartAutoAttack();
								}
								switch (skill.getSkillType())
								{
									case AGGREDUCE:
									case AGGREDUCE_CHAR:
									case AGGREMOVE:
										break;
									default:
										switch (skill.getId())
										{
											case 51: // Lure
											case 511: // Temptation
												break;
											default:
												// add attacker into list
												target.addAttackerToAttackByList(player);
										}
										// notify target AI about the attack
										if (target.hasAI())
											target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player);
										break;
								}
							}
						}
						else
						{
							if (target instanceof L2Playable)
							{
								L2PcInstance targetPlayer = target.getActingPlayer();
								// Casting non offensive skill on player with pvp flag set or with karma
								if (targetPlayer != player && (targetPlayer.getPvpFlagLasts() >= System.currentTimeMillis() || targetPlayer.getKarma() > 0))
								{
									if (!updatedPvPStatus)
									{
										updatedPvPStatus = true;
										player.updatePvPStatus();
									}
								}
							}
							else if (target instanceof L2Attackable && !(skill.getSkillType() == L2SkillType.SUMMON) && !(skill.getSkillType() == L2SkillType.BEAST_FEED) && !(skill.getSkillType() == L2SkillType.UNLOCK) && !(skill.getSkillType() == L2SkillType.DELUXE_KEY_UNLOCK) && (!(target instanceof L2Summon) || player.getPet() != target))
							{
								if (!updatedPvPStatus)
								{
									updatedPvPStatus = true;
									player.updatePvPStatus();
								}
							}
						}
					}
					else // caster is a mob
					{
						if (skill.isNeutral())
						{}
						else if (skill.isOffensive())
						{
							if (target.hasAI())
								target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
						}
					}
				}
				if (skill.isOffensive() && isInvisible())
				{
					if (player != null && player.isGM())
					{}
					else
					{
						if (this instanceof L2PcInstance || Rnd.get(100) > 100)
							stopEffects(L2EffectType.INVISIBLE);
					}
				}
				if (player != null)
				{
					// Mobs in range 1000 see spell
					Collection<L2Object> objs = player.getKnownList().getKnownObjects().values();
					{
						for (L2Object spMob : objs)
						{
							if (spMob instanceof L2Npc)
							{
								L2Npc npcMob = (L2Npc) spMob;
								if ((npcMob.isInsideRadius(player, 1000, true, true)) && (npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE) != null))
									for (Quest quest : npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE))
										quest.notifySkillSee(npcMob, player, skill, targets, this instanceof L2Summon);
							}
						}
					}
				}
			}
			// Launch the magic skill and calculate its effects
			if (handler != null)
				handler.useSkill(this, skill, targets);
			else
				skill.useSkill(this, targets);
			if (skill.getSound() != null)
				broadcastPacket(new PlaySound(0, skill.getSound(), 1, getObjectId(), getX(), getY(), getZ()));
			if (this instanceof L2PcInstance && skill.getTargetType(player) == SkillTargetType.TARGET_HOLY && FOS.checkIfOkToCastSealOfRule(player))
			{
				FOS.Announcements(getName() + " finished casting Seal Of Ruler. " + player._teamNameFOS + " has taken " + FOS._eventName + "!");
				player._countFOSCaps++;
				int rewardAmount = FOS._rewardAmount / 4;
				if (rewardAmount < 1)
					rewardAmount = 1;
				FOS.Announcements(getName() + " has been awarded " + rewardAmount + " " + ItemTable.getInstance().getTemplate(FOS._rewardId).getName());
				player.addItem("Siege Event: " + FOS._eventName, FOS._rewardId, rewardAmount, player, true);
				((L2PcInstance) this).sendPacket(new SocialAction(getObjectId(), 16));
				FOS.doSwap();
			}
			if (this instanceof L2PcInstance && skill.getTargetType(player) == SkillTargetType.TARGET_HOLY && NewFOS.checkIfOkToCastSealOfRule(player))
			{
				NewFOS.Announcements(getName() + " finished casting Seal Of Ruler. " + player._teamNameFOS + " has taken " + NewFOS._eventName + "!");
				player._countFOSCaps++;
				int rewardAmount = 8 / 4;
				if (rewardAmount < 1)
					rewardAmount = 1;
				NewFOS.Announcements(getName() + " has been awarded " + rewardAmount + " " + ItemTable.getInstance().getTemplate(6392).getName());
				player.addItem("Siege Event: " + NewFOS._eventName, 6392, rewardAmount, player, true);
				((L2PcInstance) this).sendPacket(new SocialAction(getObjectId(), 16));
				NewFOS.doSwap();
			}
			if (this instanceof L2PcInstance && skill.getSkillType() == L2SkillType.BUFF && (skill.getTargetType(player) == SkillTargetType.TARGET_SELF || skill.getTargetType(player) == SkillTargetType.TARGET_PARTY || skill.getTargetType(player) == SkillTargetType.TARGET_SELF_AND_COUPLE || skill.getTargetType(player) == SkillTargetType.TARGET_SELF_AND_PET || this.getTarget() == this))
			{
				//// TODO UpdateLunaDetailStats
				// this.getActingPlayer().sendPacket(new UpdateLunaDetailStats("test", this.getActingPlayer()));
			}
			if (skill.getMustNegateId() >= 1)
			{
				int id = skill.getMustNegateId();
				if (id > 50000) // removes on self
				{
					id -= 50000;
					if (id == 441) // stealth - shadow step removes stealth, veil and chameleon and probly hide now
					{
						for (L2Effect e : getAllEffects())
						{
							if (e != null && (e.getSkill().getId() == 441 || e.getSkill().getId() == 106 || e.getSkill().getId() == 296 || e.getSkill().getId() == 922))
								e.exit();
						}
					}
					else
					{
						for (L2Effect e : getAllEffects())
						{
							if (e != null && e.getSkill() != null && e.getSkill().getId() == id)
								e.exit();
						}
					}
				}
				else // removes on target[0]
				{
					if (targets[0] instanceof L2Character)
					{
						for (L2Effect e : ((L2Character) targets[0]).getAllEffects())
						{
							if (e != null && e.getSkill() != null && e.getSkill().getId() == id)
								e.exit();
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "", e);
		}
	}
	
	/**
	 * Return True if the L2Character is behind the target and can't be seen.<BR>
	 * <BR>
	 */
	public boolean isBehind(L2Object target)
	{
		return isBehind(target, calcStat(Stats.BACK_ANGLE_INCREASE, 50, (L2Character) target, null));
	}
	
	public boolean isBehind(L2Object target, double angle)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff = angle;
		if (target == null || maxAngleDiff < 1)
			return false;
		if (maxAngleDiff >= 350)
			return true;
		if (target instanceof L2Character)
		{
			L2Character target1 = (L2Character) target;
			angleChar = Util.calculateAngleFrom(this, target1);
			angleTarget = Util.convertHeadingToDegree(target1.getHeading());
			angleDiff = angleChar - angleTarget;
			if (angleDiff <= -360 + maxAngleDiff)
				angleDiff += 360;
			if (angleDiff >= 360 - maxAngleDiff)
				angleDiff -= 360;
			if (Math.abs(angleDiff) <= maxAngleDiff)
			{
				if (Config.DEBUG)
					_log.info("Char " + getName() + " is behind " + target.getName());
				return true;
			}
		}
		else
		{
			_log.fine("isBehindTarget's target not an L2 Character.");
		}
		return false;
	}
	
	/**
	 * Return True if the target is facing the L2Character.<BR>
	 * <BR>
	 */
	public boolean isInFrontOf(L2Character target)
	{
		if (target == null)
			return false;
		double angleChar, angleTarget, angleDiff, maxAngleDiff = 45;
		angleTarget = Util.calculateAngleFrom(target, this);
		angleChar = Util.convertHeadingToDegree(target.getHeading());
		angleDiff = angleChar - angleTarget;
		if (angleDiff <= -360 + maxAngleDiff)
			angleDiff += 360;
		if (angleDiff >= 360 - maxAngleDiff)
			angleDiff -= 360;
		return (Math.abs(angleDiff) <= maxAngleDiff);
	}
	
	/** Returns true if target is in front of L2Character (shield def etc) */
	public boolean isFacing(L2Object target, int maxAngle)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff = maxAngle / 2;
		if (target == null || maxAngleDiff < 1)
			return false;
		if (maxAngle >= 350)
			return true;
		if (target instanceof L2Character)
		{
			angleTarget = Util.calculateAngleFrom(this, target);
			angleChar = Util.convertHeadingToDegree(getHeading());
			angleDiff = angleChar - angleTarget;
			if (angleDiff <= -360 + maxAngleDiff)
				angleDiff += 360;
			if (angleDiff >= 360 - maxAngleDiff)
				angleDiff -= 360;
			if (Math.abs(angleDiff) <= maxAngleDiff)
				return true;
		}
		else
		{
			_log.fine("isInfrontOfTarget's target not an L2 Character.");
		}
		return false;
	}
	
	public boolean isFacing(int originalHeading, L2Object target, int maxAngle)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff = maxAngle / 2;
		if (target == null || maxAngleDiff < 1)
			return false;
		if (maxAngle >= 350)
			return true;
		if (target instanceof L2Character)
		{
			angleTarget = Util.calculateAngleFrom(this, target);
			angleChar = Util.convertHeadingToDegree(originalHeading);
			angleDiff = angleChar - angleTarget;
			if (angleDiff <= -360 + maxAngleDiff)
				angleDiff += 360;
			if (angleDiff >= 360 - maxAngleDiff)
				angleDiff -= 360;
			if (Math.abs(angleDiff) <= maxAngleDiff)
				return true;
		}
		else
		{
			_log.fine("isInfrontOfTarget's target not an L2 Character.");
		}
		return false;
	}
	
	/**
	 * Return 1.<BR>
	 * <BR>
	 */
	public double getLevelMod()
	{
		return 1;
	}
	
	public final void setSkillCast(Future<?> newSkillCast)
	{
		_skillCast = newSkillCast;
	}
	
	/** Sets _isCastingNow to true and _castInterruptTime is calculated from end time (ticks) */
	public final void forceIsCasting(int newSkillCastEndTick)
	{
		setIsCastingNow(true);
		_castInterruptTime = newSkillCastEndTick;
	}
	
	private boolean	_AIdisabled	= false;
	private boolean	_isMinion	= false;
	
	/**
	 * Return a Random Damage in function of the weapon.<BR>
	 * <BR>
	 */
	public final int getRandomDamage(L2Character target)
	{
		L2Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem == null)
			return 5 + (int) Math.sqrt(getLevel(true));
		return weaponItem.getRandomDamage();
	}
	
	@Override
	public String toString()
	{
		return "mob " + getObjectId();
	}
	
	final public int getAttackEndTime()
	{
		return _attackEndTime;
	}
	
	final public void setAttackEndTime(final int time)
	{
		_attackEndTime = time;
	}
	
	/**
	 * Not Implemented.<BR>
	 * <BR>
	 */
	public abstract int getLevel();
	
	// =========================================================
	// =========================================================
	// Stat - NEED TO REMOVE ONCE L2CHARSTAT IS COMPLETE
	// Property - Public
	// EXP REWARD HERE
	public final double calcStat(Stats stat, double init, L2Character target, L2Skill skill)
	{
		if (stat == Stats.EXPSP_RATE)
		{
			if (getLevel() >= 85)
			{
				final int lvlDiff = getLevel() - 84;
				float expPenalty = 111 - lvlDiff * 10;
				/*
				 * if (getLevel() >= 94)
				 * expPenalty /= 9;
				 */
				if (getLevel() >= 92)
					expPenalty /= 6;
				else if (getLevel() >= 91)
					expPenalty /= 3;
				else if (getLevel() >= 90)
					expPenalty /= 1.5;
				if (expPenalty < 0.0001)
					expPenalty = (float) 0.0001;
				if (this instanceof L2PcInstance)
				{
					final L2PcInstance player = getActingPlayer();
					if (L2RebirthMasterInstance.getRebirthLevel(player) > 0)
						expPenalty /= (1 + (0.4 * L2RebirthMasterInstance.getRebirthLevel(player)));
				}
				if (getLevel() == 85)
					expPenalty *= 4.5;
				else if (getLevel() == 86)
					expPenalty *= 2.2;
				return (getStat().calcStat(stat, init, target, skill) * expPenalty * 1.5) / Config.RATE_XP;
			}
			else
			{
				float expPenalty = 1;
				if (this instanceof L2PcInstance)
				{
					L2PcInstance player = getActingPlayer();
					if (L2RebirthMasterInstance.getRebirthLevel(player) > 0)
						expPenalty /= (1 + (0.4 * L2RebirthMasterInstance.getRebirthLevel(player)));
					else
						expPenalty = 2;
				}
				return getStat().calcStat(stat, init, target, skill) * expPenalty;
			}
		}
		else if (stat == Stats.DROP_COUNT_RATE)
		{
			return getStat().calcStat(stat, init, target, skill);
		}
		else if (stat == Stats.PVP_MAGICAL_DMG || stat == Stats.PVP_PHYS_SKILL_DMG || stat == Stats.PVP_PHYSICAL_DMG || stat == Stats.PVM_DAMAGE)
		{
			if (!(this instanceof L2Playable))
				return 1;
			float multi = 1;
			L2ItemInstance wep = getActingPlayer().getActiveWeaponInstance();
			/*
			 * if (wep != null) { if (wep.getItem().getUniqueness() >= 3 &&
			 * wep.isTimeLimitedItem()) multi = (float) 0.8; } else { wep =
			 * getActingPlayer().getSecondaryWeaponInstance();
			 * if (wep != null && wep.isTimeLimitedItem()) { if
			 * (wep.getItem().getUniqueness() >= 3) multi = (float) 0.8; } }
			 */
			// if (getActingPlayer().isInHuntersVillage())
			// {
			// if (getActingPlayer().isProphet())
			// multi = (float)0.8;
			// }
			return getStat().calcStat(stat, init * multi, target, skill);
		}
		else if (stat == Stats.PVP_MAGICAL_VUL || stat == Stats.PVP_PHYS_SKILL_VUL || stat == Stats.PVP_PHYSICAL_VUL || stat == Stats.PVM_DAMAGE_VUL)
		{
			if (!(this instanceof L2Playable))
				return 1;
			float multi = 1;
			L2ItemInstance armor = getActingPlayer().getChestArmorInstance();
			/*
			 * if (armor != null) { if (armor.getItem().getUniqueness() >= 3 &&
			 * armor.isTimeLimitedItem()) multi = (float) 1.2; }
			 */
			return getStat().calcStat(stat, init * multi, target, skill);
		}
		else if (stat == Stats.ATTACK_COUNT_MAX)
		{
			if (this instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) this;
				if (player._inEventDM && DM._started)
					return Math.min(3, getStat().calcStat(stat, init, target, skill));
				if (player._inEventDM && NewDM._started)
					return Math.min(3, getStat().calcStat(stat, init, target, skill));
			}
		}
		else if (stat == Stats.AOE_HIT_OTHER_PERCENT)
		{
			if (this instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) this;
				// if (player.isInHuntersVillage())
				// {
				// if (player.isBDClass())
				// return getStat().calcStat(stat, init/2, target, skill);
				// else
				// return getStat().calcStat(stat, init/1.66, target, skill);
				// }
				if (player._inEventDM && DM._started)
					return getStat().calcStat(stat, init / 2.2, target, skill);
				if (player._inEventDM && NewDM._started)
					return getStat().calcStat(stat, init / 2.2, target, skill);
			}
		}
		return getStat().calcStat(stat, init, target, skill);
	}
	
	// Property - Public
	public int getAccuracy(L2Character target)
	{
		return getStat().getAccuracy(target);
	}
	
	public final float getAttackSpeedMultiplier()
	{
		return getStat().getAttackSpeedMultiplier();
	}
	
	public int getCON()
	{
		return getStat().getCON();
	}
	
	public int getDEX()
	{
		return getStat().getDEX();
	}
	
	public double getCriticalDmg(L2Character target, double init, L2Skill skill)
	{
		return getStat().getCriticalDmg(target, init, skill);
	}
	
	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getCriticalHitRate(target, skill);
	}
	
	public int getEvasionRate(L2Character target)
	{
		return getStat().getEvasionRate(target);
	}
	
	public int getVampRate(L2Character target)
	{
		return getStat().getVampRate(target);
	}
	
	public int getINT()
	{
		return getStat().getINT();
	}
	
	public final int getMagicalAttackRange(L2Skill skill)
	{
		return getStat().getMagicalAttackRange(skill);
	}
	
	public final int getMaxCp()
	{
		return getStat().getMaxCp();
	}
	
	public int getMAtk(L2Character target, L2Skill skill)
	{
		return getStat().getMAtk(target, skill);
	}
	
	public int getMAtkSpd(L2Skill skill)
	{
		return getStat().getMAtkSpd(skill);
	}
	
	public int getMaxMp()
	{
		return getStat().getMaxMp();
	}
	
	public int getMaxHp()
	{
		return getStat().getMaxHp();
	}
	
	public int getMCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getMCriticalHit(target, skill);
	}
	
	public int getMDef(L2Character target, L2Skill skill)
	{
		int mdef = getStat().getMDef(target, skill);
		if (mdef < 1)
			mdef = 1;
		return mdef;
	}
	
	public int getMEN()
	{
		return getStat().getMEN();
	}
	
	public double getMReuseRate(L2Skill skill)
	{
		return getStat().getMReuseRate(skill, null);
	}
	
	public float getMovementSpeedMultiplier()
	{
		return getStat().getMovementSpeedMultiplier();
	}
	
	public int getPAtk(L2Character target)
	{
		return getStat().getPAtk(target);
	}
	
	public double getPAtkAnimals(L2Character target)
	{
		return getStat().getPAtkAnimals(target);
	}
	
	public double getPAtkDragons(L2Character target)
	{
		return getStat().getPAtkDragons(target);
	}
	
	public double getPAtkInsects(L2Character target)
	{
		return getStat().getPAtkInsects(target);
	}
	
	public double getPAtkMonsters(L2Character target)
	{
		return getStat().getPAtkMonsters(target);
	}
	
	public double getPAtkPlants(L2Character target)
	{
		return getStat().getPAtkPlants(target);
	}
	
	public double getPAtkGiants(L2Character target)
	{
		return getStat().getPAtkGiants(target);
	}
	
	public double getPAtkUndead(L2Character target)
	{
		return getStat().getPAtkUndead(target);
	}
	
	public double getPAtkDemons(L2Character target)
	{
		return getStat().getPAtkDemons(target);
	}
	
	public double getPAtkMCreatures(L2Character target)
	{
		return getStat().getPAtkMCreatures(target);
	}
	
	public double getPDefAnimals(L2Character target)
	{
		return getStat().getPDefAnimals(target);
	}
	
	public double getPDefDragons(L2Character target)
	{
		return getStat().getPDefDragons(target);
	}
	
	public double getPDefInsects(L2Character target)
	{
		return getStat().getPDefInsects(target);
	}
	
	public double getPDefMonsters(L2Character target)
	{
		return getStat().getPDefMonsters(target);
	}
	
	public double getPDefPlants(L2Character target)
	{
		return getStat().getPDefPlants(target);
	}
	
	public double getPDefGiants(L2Character target)
	{
		return getStat().getPDefGiants(target);
	}
	
	public int getPAtkSpd(L2Skill skill)
	{
		return getStat().getPAtkSpd(skill);
	}
	
	public int getPDef(L2Character target)
	{
		return getStat().getPDef(target);
	}
	
	public final int getPhysicalAttackRange()
	{
		return getStat().getPhysicalAttackRange();
	}
	
	public int getRunSpeed()
	{
		return getStat().getRunSpeed();
	}
	
	public int getShldDef()
	{
		return getStat().getShldDef();
	}
	
	public int getSTR()
	{
		return getStat().getSTR();
	}
	
	public final int getWalkSpeed()
	{
		return getStat().getWalkSpeed();
	}
	
	public int getWIT()
	{
		return getStat().getWIT();
	}
	// =========================================================
	
	// =========================================================
	// Status - NEED TO REMOVE ONCE L2CHARTATUS IS COMPLETE
	// Method - Public
	public void addStatusListener(L2Character object)
	{
		getStatus().addStatusListener(object);
	}
	
	public void reduceCurrentHp(double i, L2Character attacker, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, true, false, skill);
	}
	
	/*
	 * public void reduceCurrentHpByDOT(double i, L2Character attacker, L2Skill skill)
	 * {
	 * if (skill != null && (skill.getSkillType() == L2SkillType.POISON || skill.getEffectType() == L2SkillType.POISON))
	 * reduceCurrentHp(i, attacker, false, true, skill);
	 * else
	 * reduceCurrentHp(i, attacker, true, true, skill);
	 * }
	 */
	public void reduceCurrentHpByDOT(double i, L2Character attacker, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, !skill.isToggle(), true, skill);
	}
	
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill, boolean bypassCP)
	{
		if (Config.L2JMOD_CHAMPION_ENABLE && isChampion() && Config.L2JMOD_CHAMPION_HP != 0)
			getStatus().reduceHp(value / Config.L2JMOD_CHAMPION_HP, attacker, awake, isDOT, false, bypassCP);
		else
			getStatus().reduceHp(value, attacker, awake, isDOT, false, bypassCP);
	}
	
	public void reduceCurrentHp(double i, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, awake, isDOT, skill, false);
	}
	
	public void reduceCurrentMp(double i)
	{
		getStatus().reduceMp(i);
	}
	
	public void removeStatusListener(L2Character object)
	{
		getStatus().removeStatusListener(object);
	}
	
	protected void stopHpMpRegeneration()
	{
		getStatus().stopHpMpRegeneration();
	}
	
	// Property - Public
	public final double getCurrentCp()
	{
		return getStatus().getCurrentCp();
	}
	
	public final void setCurrentCp(Double newCp)
	{
		setCurrentCp((double) newCp);
	}
	
	public final void setCurrentCp(double newCp)
	{
		getStatus().setCurrentCp(newCp);
	}
	
	public final double getCurrentHp()
	{
		return getStatus().getCurrentHp();
	}
	
	public final void setCurrentHp(double newHp)
	{
		getStatus().setCurrentHp(newHp);
	}
	
	public final void setCurrentHpMp(double newHp, double newMp)
	{
		getStatus().setCurrentHpMp(newHp, newMp);
	}
	
	public final double getCurrentMp()
	{
		return getStatus().getCurrentMp();
	}
	
	public final void setCurrentMp(Double newMp)
	{
		setCurrentMp((double) newMp);
	}
	
	public final void setCurrentMp(double newMp)
	{
		getStatus().setCurrentMp(newMp);
	}
	// =========================================================
	
	public void setAiClass(String aiClass)
	{
		_aiClass = aiClass;
	}
	
	public String getAiClass()
	{
		return _aiClass;
	}
	
	public void setChampion(boolean champ)
	{
		_champion = champ;
	}
	
	public boolean isChampion()
	{
		return _champion;
	}
	
	public int getLastHealAmount()
	{
		return _lastHealAmount;
	}
	
	public void setLastHealAmount(int hp)
	{
		_lastHealAmount = hp;
	}
	
	final public int getMaxBuffCount()
	{
		if (this instanceof L2PcInstance)
		{
			if (getActingPlayer().isMageClass() && !getActingPlayer().isSummoner() && !getActingPlayer().isWarCryer() && !getActingPlayer().isDomi())
				return Config.BUFFS_MAX_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION));
			if (getActingPlayer().isWarCryer() || getActingPlayer().isDomi())
			{
				return Config.BUFFS_MAX_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION)) + 3;
			}
			if (getActingPlayer().isDaggerClass())
				return Config.BUFFS_MAX_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION)) + 3;
		}
		else if (this instanceof L2Summon)
		{
			if (this instanceof L2SummonInstance)
				return 0;
			return 10 + Math.max(0, ((L2Summon) this).getOwner().getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION));
		}
		return Config.BUFFS_MAX_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION)) + 3;
	}
	/**
	 * Send system message about damage.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance
	 * <li>L2SummonInstance
	 * <li>L2PetInstance</li><BR>
	 * <BR>
	 */
	public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{}
	
	public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss, boolean extra)
	{}
	
	public FusionSkill getFusionSkill()
	{
		return _fusionSkill;
	}
	
	public void setFusionSkill(FusionSkill fb)
	{
		_fusionSkill = fb;
	}
	
	public byte getAttackElement()
	{
		return getStat().getAttackElement();
	}
	
	public int getAttackElementValue(byte attackAttribute)
	{
		return getStat().getAttackElementValue(attackAttribute);
	}
	
	public int getDefenseElementValue(byte defenseAttribute)
	{
		return getStat().getDefenseElementValue(defenseAttribute);
	}
	
	public final void startPhysicalAttackMuted()
	{
		setIsPhysicalAttackMuted(true);
		breakAttack();
	}
	
	public final void stopPhysicalAttackMuted(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PHYSICAL_ATTACK_MUTE);
		else
			removeEffect(effect);
		setIsPhysicalAttackMuted(false);
	}
	
	public void disableCoreAI(boolean val)
	{
		_AIdisabled = val;
	}
	
	public boolean isCoreAIDisabled()
	{
		return _AIdisabled;
	}
	
	/** Task for potion and herb queue */
	private class UsePotionTask implements Runnable
	{
		private final L2Character	_activeChar;
		private final L2Skill		_skill;
		
		UsePotionTask(L2Character activeChar, L2Skill skill)
		{
			_activeChar = activeChar;
			_skill = skill;
		}
		
		public void run()
		{
			try
			{
				_activeChar.doSimultaneousCast(_skill);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "", e);
			}
		}
	}
	
	@Override
	public double getHpPercent()
	{
		return getCurrentHp() / getMaxHp() * 100;
	}
	
	public boolean isRaidMinion()
	{
		return _isMinion;
	}
	
	/**
	 * Set this Npc as a Minion instance.<BR>
	 * <BR>
	 * 
	 * @param val
	 */
	public void setIsRaidMinion(boolean val)
	{
		_isRaid = val;
		_isMinion = val;
	}
	
	public boolean isUsingVanguardStance()
	{
		for (L2Effect e : getAllEffects())
		{
			if (e != null)
			{
				switch (e.getSkill().getId())
				{
					case 810:
					case 811:
					case 812:
					case 813:
						return true;
				}
			}
		}
		return false;
	}
	
	public boolean isUsingInquisitorStance()
	{
		for (L2Effect e : getAllEffects())
		{
			if (e != null)
			{
				switch (e.getSkill().getId())
				{
					case 1520:
					case 1521:
					case 1522:
						return true;
				}
			}
		}
		return false;
	}
	
	public boolean isBleeding()
	{
		for (L2Effect e : getAllEffects())
		{
			if (e != null)
			{
				if (e.getSkill() != null)
				{
					if (e.getSkill().getSkillType() == L2SkillType.BLEED || e.getSkill().getEffectType() == L2SkillType.BLEED)
						return true;
				}
			}
		}
		return false;
	}
	
	public boolean isPoisoned()
	{
		for (L2Effect e : getAllEffects())
		{
			if (e != null)
			{
				if (e.getSkill() != null)
				{
					if (e.getSkill().getSkillType() == L2SkillType.POISON || e.getSkill().getEffectType() == L2SkillType.POISON)
						return true;
				}
			}
		}
		return false;
	}
	
	public void healHP()
	{
		getStatus().setCurrentHp(getMaxHp());
		getStatus().setCurrentMp(getMaxMp());
	}
	
	final public boolean CanAttackDueToInEvent(L2Character target)
	{
		if (!(target instanceof L2Playable))
			return true;
		if (!(this instanceof L2Playable))
			return true;
		final L2PcInstance player1 = target.getActingPlayer();
		final L2PcInstance player2 = getActingPlayer();
		if (player1 == null || player2 == null)
			return false;
		if (player1.isInFunEvent() != player2.isInFunEvent())
			return false;
		if (TvT._started || NewTvT._started || NewHuntingGrounds._started || FOS._started || NewFOS._started || CTF._started || NewCTF._started || VIP._started || DM._started || NewDM._started || NewDomination._started) // when events started it's slightly different
		{
			// if (player1.getInEventPeaceZone())
			// return false;
			if (player1._inEventTvT && player2._inEventTvT)
			{
				if (player1._teamNameTvT == player2._teamNameTvT)
					return false;
				else
					return true;
			}
			if (player1._inEventHG && player2._inEventHG)
			{
				if (player1._teamNameHG == player2._teamNameHG)
					return false;
				else
					return true;
			}
			if (player1._inEventFOS && player2._inEventFOS)
			{
				if (player1._teamNameFOS == player2._teamNameFOS)
					return false;
				else
					return true;
			}
			if (player1._inEventLunaDomi && player2._inEventLunaDomi)
			{
				if (player1._teamNameLunaDomi == player2._teamNameLunaDomi)
					return false;
				else
					return true;
			}
			if (player1._isInActiveDominationEvent && player2._isInActiveDominationEvent)
			{
				if (player1.getDominationTeam().equalsIgnoreCase(player2.getDominationTeam()))
					return false;
				else
					return true;
			}
			if (player1._inEventCTF && player2._inEventCTF)
			{
				if (player1._teamNameCTF == player2._teamNameCTF)
					return false;
				else
					return true;
			}
			else if (player1._inEventVIP && player2._inEventVIP)
			{
				if (player1.isOnSameTeamInVIP(player2))
					return false;
				else
					return true;
			}
			else if (player1._inEventDM && player2._inEventDM)
			{
				return true;
			}
		}
		return true;
	}
	
	public void setPreventedFromReceivingBuffs(boolean value)
	{
		_block_buffs = value;
	}
	
	public boolean isPreventedFromReceivingBuffs()
	{
		return _block_buffs;
	}
	
	public boolean isPreventedFromReceivingDebuffs()
	{
		final int lionheartCasting = (int) calcStat(Stats.LIONHEART, 0, null, null);
		// TODO removed mages protection for receiving debuffs while casting.
		/*
		 * if (lionheartCasting > 0)
		 * {
		 * if (isCastingNow())
		 * return lionheartCasting > Rnd.get(100);
		 * }
		 */
		return false;
	}
	
	public boolean isInStance()
	{
		return false;
	}
	
	public boolean isInAlterverse()
	{
		return getInstanceId() == 1;
	}
	
	public boolean isInUniverse()
	{
		return getInstanceId() == 0;
	}
	
	public float getCollisionRadius()
	{
		if (this instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) this;
			if (!player.getAppearance().getSex())
			{
				switch (player.getRace().ordinal()) // male r
				{
					case 1:
						return (float) 7.5;
					case 2:
						return (float) 7.5;
					case 3:
						return 11;
					case 4:
						return 9;
					case 5:
						return (float) 7.5;
					case 6:
						return (float) 7.5;
					case 7:
						return 7;
					default:
						return 9;
				}
			}
			else
			{
				switch (player.getRace().ordinal()) // female r
				{
					case 1:
						return 7;
					case 2:
						return (float) 7.5;
					case 3:
						return (float) 7.5;
					case 4:
						return 5;
					case 5:
						return (float) 7.5;
					case 6:
						return 7;
					case 7:
						return 8;
					default:
						return 8;
				}
			}
		}
		else
		{
			if (this instanceof L2Npc)
			{
				final FakePc fake = FakePcsTable.getInstance().getFakePc(((L2Npc) this).getNpcId());
				if (fake != null)
				{
					if (fake.sex == 0)
					{
						switch (fake.race) // fake male r
						{
							case 1:
								return (float) 7.5;
							case 2:
								return (float) 7.5;
							case 3:
								return 11;
							case 4:
								return 9;
							case 5:
								return (float) 7.5;
							case 6:
								return (float) 7.5;
							case 7:
								return 7;
							default:
								return 9;
						}
					}
					else
					{
						switch (fake.race) // fake female r
						{
							case 1:
								return 7;
							case 2:
								return (float) 7.5;
							case 3:
								return (float) 7.5;
							case 4:
								return 5;
							case 5:
								return (float) 7.5;
							case 6:
								return 7;
							case 7:
								return 8;
							default:
								return 8;
						}
					}
				}
				// if( this instanceof L2MuseumStatueInstance)
				// {
				// final L2MuseumStatueInstance statue = (L2MuseumStatueInstance) this;
				// if (statue != null)
				// {
				// if (statue.getCharLooks().getSex() == 0)
				// {
				// switch (statue.getCharLooks().getRace()) // fake male r
				// {
				// case 1:
				// return (float) 7.5;
				// case 2:
				// return (float) 7.5;
				// case 3:
				// return 11;
				// case 4:
				// return 9;
				// case 5:
				// return (float) 7.5;
				// case 6:
				// return (float) 7.5;
				// case 7:
				// return 7;
				// default:
				// return 9;
				// }
				// }
				// else
				// {
				// switch (statue.getCharLooks().getRace()) // fake female r
				// {
				// case 1:
				// return 7;
				// case 2:
				// return (float) 7.5;
				// case 3:
				// return (float) 7.5;
				// case 4:
				// return 5;
				// case 5:
				// return (float) 7.5;
				// case 6:
				// return 7;
				// case 7:
				// return 8;
				// default:
				// return 8;
				// }
				// }
				// }
				// }
			}
			return getTemplate().getCollisionRadius();
		}
	}
	
	public float getCollisionHeight()
	{
		if (this instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) this;
			if (!player.getAppearance().getSex())
			{
				switch (player.getRace().ordinal()) // male h
				{
					case 1:
						return 24;
					case 2:
						return 24;
					case 3:
						return 28;
					case 4:
						return 18;
					case 5:
						return (float) 24.5;
					case 6:
						return (float) 22.5;
					case 7:
						return 27;
					default:
						return 23;
				}
			}
			else
			{
				switch (player.getRace().ordinal()) // female h
				{
					case 1:
						return 23;
					case 2:
						return 23;
					case 3:
						return 27;
					case 4:
						return 19;
					case 5:
						return 22;
					case 6:
						return 22;
					case 7:
						return 25;
					default:
						return 23;
				}
			}
		}
		else
		{
			if (this instanceof L2Npc)
			{
				final FakePc fake = FakePcsTable.getInstance().getFakePc(((L2Npc) this).getNpcId());
				if (fake != null)
				{
					if (fake.sex == 0)
					{
						switch (fake.race) // fake male h
						{
							case 1:
								return 24;
							case 2:
								return 24;
							case 3:
								return 28;
							case 4:
								return 18;
							case 5:
								return (float) 24.5;
							case 6:
								return (float) 22.5;
							case 7:
								return 27;
							default:
								return 23;
						}
					}
					else
					{
						switch (fake.race) // fake female h
						{
							case 1:
								return 23;
							case 2:
								return 23;
							case 3:
								return 27;
							case 4:
								return 19;
							case 5:
								return 22;
							case 6:
								return 22;
							case 7:
								return 25;
							default:
								return 23;
						}
					}
				}
			}
			return getTemplate().getCollisionHeight();
		}
	}
	
	public final boolean isAPC()
	{
		return this instanceof L2Npc && ((L2Npc) this)._isAPC;
	}
	
	public int getLevel(boolean checkOly)
	{
		return getLevel();
	}
	
	public boolean canSeeInvisiblePeople()
	{
		return isGM() || calcStat(Stats.SEE_INVISIBLE, 0, null, null) > 0;
	}
	
	public boolean canSeeInvisiblePeopleNotGM()
	{
		return calcStat(Stats.SEE_INVISIBLE, 0, null, null) > 0;
	}
	
	public String getDisplayName()
	{
		if (this instanceof L2PcInstance || this instanceof L2DecoyInstance)
			return getActingPlayer().getAppearance().getVisibleName();
		return getName();
	}
	
	public boolean isDisguised()
	{
		if (getActingPlayer() != null)
			return getActingPlayer().isDisguised();
		return false;
	}
	
	public boolean isInvisible()
	{
		return _invisible;
	}
	
	public void setInvisible(boolean invisible)
	{
		_invisible = invisible;
	}
	
	public boolean isInSameMobGroupAs(L2Character obj)
	{
		if (obj == null)
			return false;
		if (!(this instanceof L2Attackable) || !(obj instanceof L2Attackable))
			return false;
		if (this instanceof L2MonsterInstance)
		{
			L2MonsterInstance master = (L2MonsterInstance) this;
			if (this instanceof L2MinionInstance)
			{
				master = ((L2MinionInstance) this).getLeader();
				if (master == null)
					return false;
				if (master == obj)
					return true;
				for (L2MinionInstance minion : master._minionList.getSpawnedMinions())
				{
					if (minion == null)
						continue;
					if (minion == obj)
						return true;
				}
			}
			else if (master.hasMinions())
			{
				for (L2MinionInstance minion : master._minionList.getSpawnedMinions())
				{
					if (minion == null)
						continue;
					if (minion == obj)
						return true;
				}
			}
		}
		return false;
	}
	
	public boolean isSamePartyWith(final L2Character character)
	{
		return false;
	}
	
	public boolean isAlikeAlive()
	{
		return !isAlikeDead();
	}
	
	public boolean isMaxHpLess(final int num)
	{
		return getMaxHp() < num;
	}
	
	public boolean isMaxHpLess100K()
	{
		return isMaxHpLess(100_000);
	}
	
	public void broadcastUserInfo()
	{}
	
	public boolean testDoCastConditions(final L2Skill skill)
	{
		if (skill == null || isSkillDisabled(skill.getId()) || ((skill.getFlyType() == FlyType.CHARGE.toString()) && isMovementDisabled()))
		{
			return false;
		}
		// Check if the caster has enough MP
		if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill) && skill.isHeal())
		{
			return false;
		}
		// Check if the caster has enough HP
		if (getCurrentHp() <= skill.getHpConsume())
		{
			return false;
		}
		// Verify the different types of silence (magic and physic)
		if (!skill.isPotion() && (skill.isMagic() && isMuted() || !skill.isMagic() && isPhysicalMuted()))
		{
			return false;
		}
		// prevent casting signets to peace zone
		if (skill.getSkillType() == L2SkillType.SIGNET || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME)
		{
			final L2WorldRegion region = getWorldRegion();
			if (region == null)
				return false;
			if (skill.getTargetType(this) == SkillTargetType.TARGET_GROUND && this instanceof L2PcInstance)
			{
				final Location wp = ((L2PcInstance) this).getCurrentSkillWorldPositionLoc();
				if (!region.checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ()))
				{
					return false;
				}
			}
			else if (!region.checkEffectRangeInsidePeaceZone(skill, getX(), getY(), getZ()))
			{
				return false;
			}
		}
		if (!skill.getWeaponDependancy(this, true))
		{
			return false;
		}
		if (skill.getItemConsumeId() > 0 && getInventory() != null)
		{
			final L2ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());
			if (requiredItems == null || requiredItems.getCount() < skill.getItemConsume())
			{
				if (skill.getSkillType() == L2SkillType.SUMMON)
				{
					return false;
				}
				return false;
			}
		}
		return true;
	}
}
