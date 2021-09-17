package net.sf.l2j.gameserver.model.actor;

import java.util.logging.Level;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ItemsAutoDestroy;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2AttackableAI;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2FortSiegeGuardAI;
import net.sf.l2j.gameserver.ai.L2SiegeGuardAI;
import net.sf.l2j.gameserver.datatables.EventDroplist;
import net.sf.l2j.gameserver.datatables.EventDroplist.DateDrop;
import net.sf.l2j.gameserver.datatables.ItemLists;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2CommandChannel;
import net.sf.l2j.gameserver.model.L2DropCategory;
import net.sf.l2j.gameserver.model.L2DropData;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2DecoyInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FortSiegeGuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MinionInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeGuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TesterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TrapInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.AttackableKnownList;
import net.sf.l2j.gameserver.model.actor.status.AttackableStatus;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.Quest.QuestEventType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class L2Attackable extends L2Npc
{
	public static boolean RAID_SYSTEM_ENABLED = true;
	/*
	 * private final static int clanRepEggs[] = {9814, 9815, 9816, 9817, 9818};
	 * private final static int protectionBooks[] = {10550, 10551, 10549, 10552,
	 * 10553, 14219};
	 */
	
	/**
	 * This class contains all AggroInfo of the L2Attackable against the
	 * attacker L2Character.
	 * Data: attacker : The attacker L2Character concerned by this AggroInfo of
	 * this L2Attackable hate : Hate level of this L2Attackable against the
	 * attacker L2Character (hate = damage) damage : Number of damages that the
	 * attacker L2Character gave to this L2Attackable
	 */
	public final class AggroInfo
	{
		protected L2Character	_attacker;
		protected int			_aggro;
		
		AggroInfo(L2Character pAttacker)
		{
			_attacker = pAttacker;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj instanceof AggroInfo)
				return (((AggroInfo) obj)._attacker == _attacker);
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return _attacker.getObjectId();
		}
	}
	
	@Override
	public boolean isDebuffable(L2PcInstance attacker)
	{
		if (getLevel() >= 95)
			return false;
		return true;
	}
	
	/**
	 * This class contains all RewardInfo of the L2Attackable against the any
	 * attacker L2Character, based on amount of damage done.
	 * Data: attacker : The attacker L2Character concerned by this RewardInfo of
	 * this L2Attackable dmg : Total amount of damage done by the attacker to
	 * this L2Attackable (summon + own)
	 */
	protected final class RewardInfo
	{
		protected L2Character	_attacker;
		protected int			_dmg	= 0;
		
		public RewardInfo(L2Character pAttacker, int pDmg)
		{
			_attacker = pAttacker;
			_dmg = pDmg;
		}
		
		public void addDamage(int pDmg)
		{
			_dmg += pDmg;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj instanceof RewardInfo)
				return (((RewardInfo) obj)._attacker == _attacker);
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return _attacker.getObjectId();
		}
	}
	
	/**
	 * This class contains all AbsorberInfo of the L2Attackable against the
	 * absorber L2Character.
	 * Data: absorber : The attacker L2Character concerned by this AbsorberInfo
	 * of this L2Attackable
	 */
	public final class AbsorberInfo
	{
		protected L2PcInstance	_absorber;
		protected int			_crystalId;
		protected double		_absorbedHP;
		
		AbsorberInfo(L2PcInstance attacker, int pCrystalId, double pAbsorbedHP)
		{
			_absorber = attacker;
			_crystalId = pCrystalId;
			_absorbedHP = pAbsorbedHP;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj instanceof AbsorberInfo)
				return (((AbsorberInfo) obj)._absorber == _absorber);
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return _absorber.getObjectId();
		}
	}
	
	public final class RewardItem
	{
		protected int	_itemId;
		protected int	_count;
		protected int	_enchantLevel	= 0;
		protected int	_partyDropCount	= 0;
		protected int _chance = 0;
		
		public RewardItem(int itemId, int count)
		{
			_itemId = itemId;
			_count = count;
			_enchantLevel = 0;
			_partyDropCount = 0;
		}
		
		public RewardItem(int itemId, int count, int enchant, int partydrop, int chance)
		{
			_itemId = itemId;
			_count = count;
			_enchantLevel = enchant;
			_partyDropCount = partydrop;
			_chance = chance;
		}
		
		public int getChance()
		{
			return _chance;
		}
		public final int getPartyDropCount()
		{
			return _partyDropCount;
		}
		
		public final void setPartyDropCount(int partyDropCount)
		{
			_partyDropCount = partyDropCount;
		}
		
		public int getItemId()
		{
			return _itemId;
		}
		
		public int getCount()
		{
			return _count;
		}
		
		public int getEnchantLevel()
		{
			return _enchantLevel;
		}
		
		public void setItemId(int itemId)
		{
			_itemId = itemId;
		}
	}
	
	private final FastMap<L2Character, AggroInfo> _aggroList = new FastMap<L2Character, AggroInfo>().shared();
	
	public final FastMap<L2Character, AggroInfo> getAggroList()
	{
		return _aggroList;
	}
	
	private boolean _isReturningToSpawnPoint = false;
	
	public final boolean isReturningToSpawnPoint()
	{
		return _isReturningToSpawnPoint;
	}
	
	public final void setisReturningToSpawnPoint(boolean value)
	{
		_isReturningToSpawnPoint = value;
	}
	
	private boolean _canReturnToSpawnPoint = true;
	
	public final boolean canReturnToSpawnPoint()
	{
		return _canReturnToSpawnPoint;
	}
	
	public final void setCanReturnToSpawnPoint(boolean value)
	{
		_canReturnToSpawnPoint = value;
	}
	
	private RewardItem[]								_sweepItems;
	private RewardItem[]								_harvestItems;
	private boolean										_seeded;
	private int											_seedType						= 0;
	private L2PcInstance								_seeder							= null;
	private boolean										_overhit;
	private double										_overhitDamage;
	private L2Character									_overhitAttacker;
	private L2CommandChannel							_firstCommandChannelAttacked	= null;
	private CommandChannelTimer							_commandChannelTimer			= null;
	private boolean										_absorbed;
	private final FastMap<L2PcInstance, AbsorberInfo>	_absorbersList					= new FastMap<L2PcInstance, AbsorberInfo>().shared();
	private boolean										_mustGiveExpSp;
	private boolean										_eventMob;
	
	/**
	 * Constructor of L2Attackable (use L2Character and L2NpcInstance
	 * constructor).
	 * Actions: Call the L2Character constructor to set the _template of the
	 * L2Attackable (copy skills from template to object and link _calculators
	 * to NPC_STD_CALCULATOR) Set the name of the L2Attackable Create a
	 * RandomAnimation Task that will be launched after the calculated delay if
	 * the server allow it
	 * 
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param L2NpcTemplate
	 *            Template to apply to the NPC
	 */
	public L2Attackable(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setIsInvul(false);
		_mustGiveExpSp = true;
	}
	
	@Override
	public AttackableKnownList getKnownList()
	{
		return (AttackableKnownList) super.getKnownList();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new AttackableKnownList(this));
	}
	
	@Override
	public AttackableStatus getStatus()
	{
		return (AttackableStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new AttackableStatus(this));
	}
	
	/**
	 * Return the L2Character AI of the L2Attackable and if its null create a
	 * new one.
	 */
	@Override
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new L2AttackableAI(new AIAccessor());
				return _ai;
			}
		}
		return ai;
	}
	
	/**
	 * Not used. get condition to hate, actually isAggressive() is checked by
	 * monster and karma by guards in motheds that overwrite this one.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public boolean getCondition2(L2Character target)
	{
		if (target instanceof L2NpcInstance || target instanceof L2DoorInstance)
			return false;
		if (target.isAlikeDead() || !isInsideRadius(target, getAggroRange(), false, false) || Math.abs(getZ() - target.getZ()) > 100)
			return false;
		return !target.isInvul();
	}
	
	/**
	 * Reduce the current HP of the L2Attackable.
	 * 
	 * @param damage
	 *            The HP decrease value
	 * @param attacker
	 *            The L2Character who attacks
	 */
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, L2Skill skill)
	{
		reduceCurrentHp(damage, attacker, true, false, skill);
	}
	
	/**
	 * Reduce the current HP of the L2Attackable, update its _aggroList and
	 * launch the doDie Task if necessary.
	 * 
	 * @param i
	 *            The HP decrease value
	 * @param attacker
	 *            The L2Character who attacks
	 * @param awake
	 *            The awake state (If True : stop sleeping)
	 */
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		if (this instanceof L2TesterInstance)
			return;
		if (_commandChannelTimer == null && isRaid())
		{
			if (attacker != null && attacker.getParty() != null && attacker.getParty().isInCommandChannel() && attacker.getParty().getCommandChannel().meetRaidWarCondition(this))
			{
				_firstCommandChannelAttacked = attacker.getParty().getCommandChannel();
				_commandChannelTimer = new CommandChannelTimer(this, attacker.getParty().getCommandChannel());
				ThreadPoolManager.getInstance().scheduleGeneral(_commandChannelTimer, 300000); // 5
																								// min
				_firstCommandChannelAttacked.broadcastToChannelMembers(new CreatureSay(0, Say2.PARTYROOM_ALL, "", "You have looting rights!"));
			}
		}
		if (isEventMob)
			return;
		// Add damage and hate to the attacker AggroInfo of the L2Attackable
		// _aggroList
		if (attacker != null)
		{
			float damageMulti = 1;
			if (this instanceof L2RaidBossInstance && !isAPC())
			{
				final L2PcInstance player = attacker.getActingPlayer();
				if (player != null)
				{
					if (player.isArcherClass())
						damageMulti = (float) 2.5;
					else if (player.isMageClass())
						damageMulti = (float) 1.8;
				}
			}
			addDamage(attacker, (int) (damage * damageMulti), skill);
		}
		// If this L2Attackable is a L2MonsterInstance and it has spawned
		// minions, call its minions to battle
		if (this instanceof L2MonsterInstance)
		{
			L2MonsterInstance master = (L2MonsterInstance) this;
			if (this instanceof L2MinionInstance)
			{
				master = ((L2MinionInstance) this).getLeader();
				if (master != null && !master.isDead() && (!master.isInCombat() || isAPC()))
				{
					final L2PcInstance player = attacker.getActingPlayer();
					final L2Character attackTarget = master.getAI().getAttackTarget();
					if (player != null && attackTarget != null && attackTarget instanceof L2Playable)
					{
						if (!(player.getInSameClanAllyAs(attackTarget.getActingPlayer()) == 2 || ((player.getParty() != null && player.getParty().getPartyMembers().contains(attackTarget.getActingPlayer())))))
							damage /= 2;
					}
					master.addDamageHate(attacker, (int) Math.max(1, isAPC() ? damage / 4 : damage / 6));
					master.callMinionsToAssist(attacker, (int) Math.max(1, isAPC() ? damage / 3 : damage / 4), this);
				}
			}
			else if (master.hasMinions())
				master.callMinionsToAssist(attacker, (int) Math.max(1, damage / 2), null);
		}
		// Reduce the current HP of the L2Attackable and launch the doDie Task
		// if necessary
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	public synchronized void setMustRewardExpSp(boolean value)
	{
		_mustGiveExpSp = value;
	}
	
	public synchronized boolean getMustRewardExpSP()
	{
		return _mustGiveExpSp;
	}
	
	/**
	 * Kill the L2Attackable (the corpse disappeared after 7 seconds),
	 * distribute rewards (EXP, SP, Drops...) and notify Quest Engine.
	 * Actions: Distribute Exp and SP rewards to L2PcInstance (including Summon
	 * owner) that hit the L2Attackable and to their Party members Notify the
	 * Quest Engine of the L2Attackable death if necessary Kill the
	 * L2NpcInstance (the corpse disappeared after 7 seconds)
	 * Caution: This method DOESN'T GIVE rewards to L2PetInstance
	 * 
	 * @param killer
	 *            The L2Character that has killed the L2Attackable
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		// Kill the L2NpcInstance (the corpse disappeared after 7 seconds)
		if (!super.doDie(killer))
			return false;
		try
		{
			L2PcInstance player = null;
			if (killer != null)
				player = killer.getActingPlayer();
			if (player != null)
			{
				if (getTemplate().getEventQuests(QuestEventType.ON_KILL) != null)
					for (Quest quest : getTemplate().getEventQuests(QuestEventType.ON_KILL))
						ThreadPoolManager.getInstance().scheduleEffect(new OnKillNotifyTask(this, quest, player, killer instanceof L2Summon), 5000);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
		return true;
	}
	
	public boolean canShowLevelInTitle()
	{
		return !(getName().equals("Chest"));
	}
	
	class OnKillNotifyTask implements Runnable
	{
		private final L2Attackable	_attackable;
		private final Quest			_quest;
		private final L2PcInstance	_killer;
		private final boolean		_isPet;
		
		public OnKillNotifyTask(L2Attackable attackable, Quest quest, L2PcInstance killer, boolean isPet)
		{
			_attackable = attackable;
			_quest = quest;
			_killer = killer;
			_isPet = isPet;
		}
		
		public void run()
		{
			_quest.notifyKill(_attackable, _killer, _isPet);
		}
	}
	
	/**
	 * Distribute Exp and SP rewards to L2PcInstance (including Summon owner)
	 * that hit the L2Attackable and to their Party members.
	 * Actions: Get the L2PcInstance owner of the L2SummonInstance (if
	 * necessary) and L2Party in progress Calculate the Experience and SP
	 * rewards in function of the level difference Add Exp and SP rewards to
	 * L2PcInstance (including Summon penalty) and to Party members in the known
	 * area of the last attacker
	 * Caution : This method DOESN'T GIVE rewards to L2PetInstance
	 * 
	 * @param lastAttacker
	 *            The L2Character that has killed the L2Attackable
	 */
	@Override
	protected void calculateRewards(L2Character lastAttacker)
	{
		if (lastAttacker == null)
			return;
		// Creates an empty list of rewards
		FastList<L2PcInstance> peopleToRewardExpSp = new FastList<L2PcInstance>();
		try
		{
			if (getAggroList().isEmpty())
				return;
			// Manage Base, Quests and Sweep drops of the L2Attackable
			doItemDrop(lastAttacker);
			// Manage drop of Special Events created by GM for a defined period
			doEventDrop(lastAttacker);
			if (!getMustRewardExpSP())
				return;
			final L2PcInstance playera = lastAttacker.getActingPlayer();
			if (playera == null)
				return;
			L2Character attacker;
			L2PcInstance ddealer;
			final L2Party party = playera.getParty();
			// While Interating over This Map Removing Object is Not Allowed
			synchronized (getAggroList())
			{
				// Go through the _aggroList of the L2Attackable
				for (AggroInfo info : getAggroList().values())
				{
					if (info == null)
						continue;
					// Get the L2Character corresponding to this attacker
					attacker = info._attacker;
					// Prevent unwanted behavior
					if (info._aggro >= 1)
					{
						if ((attacker instanceof L2SummonInstance) || ((attacker instanceof L2PetInstance) && ((L2PetInstance) attacker).getPetData().getOwnerExpTaken() > 0))
							ddealer = ((L2Summon) attacker).getOwner();
						else if (attacker instanceof L2PcInstance)
							ddealer = (L2PcInstance) attacker;
						else
							continue;
						if (party != null)
						{
							if (!party.getPartyMembers().contains(ddealer))
								continue;
						}
						else if (ddealer != playera)
							continue;
						if (ddealer.isInFunEvent())
							continue;
						// Check if ddealer isn't too far from this (killed
						// monster)
						if (!ddealer.isInUniqueInstance() && !Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, ddealer, true))
							continue;
						if (!peopleToRewardExpSp.contains(ddealer))
							peopleToRewardExpSp.add(ddealer);
					}
				}
			}
			if (!peopleToRewardExpSp.isEmpty())
			{
				int levelDiff;
				long[] tmp;
				long exp;
				int sp;
				final int partyRewardSize = peopleToRewardExpSp.size();
				if (partyRewardSize == 1)
				{
					if (playera.getKnownList().knowsObject(this))
					{
						levelDiff = playera.getLevel() - getLevel();
						if (this instanceof L2RaidBossInstance)
						{
							if (levelDiff > 0)
								levelDiff = 0;
						}
						tmp = calculateExpAndSp(levelDiff);
						exp = tmp[0];
						sp = (int) tmp[1];
						if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
						{
							exp *= Config.L2JMOD_CHAMPION_REWARDS;
							sp *= Config.L2JMOD_CHAMPION_REWARDS;
						}
						// Check for an over-hit enabled strike
						if (lastAttacker instanceof L2PcInstance)
						{
							L2PcInstance player = (L2PcInstance) lastAttacker;
							if (isOverhit() && lastAttacker == getOverhitAttacker())
							{
								player.sendPacket(new SystemMessage(SystemMessageId.OVER_HIT));
								exp += calculateOverhitExp(exp);
							}
						}
						// Distribute the Exp and SP between the L2PcInstance
						// and its L2Summon
						if (!playera.isDead())
						{
							long addexp = Math.round(playera.calcStat(Stats.EXPSP_RATE, exp, null, null));
							if (playera.getLevel() < 85)
							{
								if (playera.isSupportClass())
									addexp *= 3;
								else
									addexp *= 1.75;
							}
							int addsp = (int) playera.calcStat(Stats.EXPSP_RATE, sp, null, null);
							if (playera.getLevel() < 85 && addexp > Experience.LEVEL[85] - playera.getExp())
								addexp = Experience.LEVEL[85] - playera.getExp();
							if (lastAttacker instanceof L2PcInstance)
							{
								if (((L2PcInstance) lastAttacker).getSkillLevel(467) > 0)
								{
									L2Skill skill = SkillTable.getInstance().getInfo(467, ((L2PcInstance) lastAttacker).getSkillLevel(467));
									if (skill.getExpNeeded() <= addexp)
										((L2PcInstance) lastAttacker).absorbSoul(skill, this);
								}
								playera.addExpAndSp(addexp, addsp, useVitalityRate());
								if (addexp > 0)
									playera.updateVitalityPoints(getVitalityPoints(getMaxHp()), true, false);
							}
							else
								playera.addExpAndSp(addexp, addsp);
							final L2Summon pet = playera.getPet();
							if (pet != null && pet instanceof L2PetInstance && pet.isInsideRadius(playera, 500, false, false))
							{
								if (((L2PetInstance) pet).getPetData().getOwnerExpTaken() > 0)
									pet.addExpAndSp(addexp / 350, 0);
								else
									pet.addExpAndSp(addexp / 100, 0);
							}
						}
					}
				}
				else
				{
					int partyLevel = 0;
					for (L2PcInstance dude : peopleToRewardExpSp)
					{
						if (dude != null)
						{
							if (dude.getLevel() < 85)
								partyLevel += 85;
							else
								partyLevel += dude.getLevel();
						}
					}
					partyLevel = partyLevel / partyRewardSize;
					levelDiff = partyLevel - getLevel();
					if (this instanceof L2RaidBossInstance)
					{
						if (levelDiff > 0)
							levelDiff = 0;
					}
					tmp = calculateExpAndSp(levelDiff);
					exp = tmp[0] / partyRewardSize;
					sp = (int) tmp[1] / partyRewardSize;
					if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
					{
						exp *= Config.L2JMOD_CHAMPION_REWARDS;
						sp *= Config.L2JMOD_CHAMPION_REWARDS;
					}
					if (lastAttacker instanceof L2PcInstance)
					{
						L2PcInstance player = (L2PcInstance) lastAttacker;
						if (isOverhit() && lastAttacker == getOverhitAttacker())
						{
							player.sendPacket(new SystemMessage(SystemMessageId.OVER_HIT));
							exp += calculateOverhitExp(exp);
						}
						if (((L2PcInstance) lastAttacker).getSkillLevel(467) > 0)
						{
							L2Skill skill = SkillTable.getInstance().getInfo(467, ((L2PcInstance) lastAttacker).getSkillLevel(467));
							((L2PcInstance) lastAttacker).absorbSoul(skill, this);
						}
					}
					for (L2PcInstance dude : peopleToRewardExpSp)
					{
						if (dude != null && !dude.isDead())
						{
							long addexp = Math.round(dude.calcStat(Stats.EXPSP_RATE, exp * 1.2, null, null));
							int addsp = (int) dude.calcStat(Stats.EXPSP_RATE, sp, null, null);
							if (dude.getLevel() < 85 && addexp > Experience.LEVEL[85] - dude.getExp())
								addexp = Experience.LEVEL[85] - dude.getExp();
							dude.addExpAndSp(addexp, addsp);
							final L2Summon pet = dude.getPet();
							if (pet != null && pet instanceof L2PetInstance && pet.isInsideRadius(dude, 500, false, false))
							{
								if (((L2PetInstance) pet).getPetData().getOwnerExpTaken() > 0)
									pet.addExpAndSp(addexp / 350, 0);
								else
									pet.addExpAndSp(addexp / 100, 0);
							}
						}
					}
				}
			}
			peopleToRewardExpSp = null;
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable
	 * _aggroList.
	 * 
	 * @param attacker
	 *            The L2Character that gave damages to this L2Attackable
	 * @param damage
	 *            The number of damages given by the attacker L2Character
	 */
	public void addDamage(L2Character attacker, int damage, L2Skill skill)
	{
		// Notify the L2Attackable AI with EVT_ATTACKED
		if (!isDead())
		{
			try
			{
				L2PcInstance player = attacker.getActingPlayer();
				if (player != null)
				{
					if (getTemplate().getEventQuests(QuestEventType.ON_ATTACK) != null)
						for (Quest quest : getTemplate().getEventQuests(QuestEventType.ON_ATTACK))
							quest.notifyAttack(this, player, damage, (attacker instanceof L2Summon || attacker instanceof L2Decoy), skill);
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable
	 * _aggroList.
	 * 
	 * @param attacker
	 *            The L2Character that gave damages to this L2Attackable
	 * @param damage
	 *            The number of damages given by the attacker L2Character
	 * @param aggro
	 *            The hate (=damage) given by the attacker L2Character
	 */
	public void addDamageHate(final L2Character attacker, final int damage, int aggro)
	{
		addDamageHate(attacker, damage, aggro);
	}
	public void addDamageHate(L2Character attacker, int aggro)
	{
		if (attacker == null)
			return;
		if (aggro > 1)
		{
			aggro = (int) calcStat(Stats.AGGRESSION_VULN, aggro, attacker, null);
			if (aggro < 1)
				aggro = 1;
		}
		// Get the AggroInfo of the attacker L2Character from the _aggroList of
		// the L2Attackable
		AggroInfo ai = getAggroList().get(attacker);
		if (ai == null)
		{
			ai = new AggroInfo(attacker);
			getAggroList().put(attacker, ai);
			ai._aggro = 0;
		}
		ai._aggro += aggro;
		L2PcInstance targetPlayer = attacker.getActingPlayer();
		if (targetPlayer != null && aggro == 0)
		{
			if (getTemplate().getEventQuests(QuestEventType.ON_AGGRO_RANGE_ENTER) != null)
				for (Quest quest : getTemplate().getEventQuests(QuestEventType.ON_AGGRO_RANGE_ENTER))
					quest.notifyAggroRangeEnter(this, targetPlayer, (attacker instanceof L2Summon || attacker instanceof L2Decoy));
		}
		// Set the intention to the L2Attackable to AI_INTENTION_ACTIVE
		if (aggro > 0 && getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	}
	
	public void reduceHate(L2Character target, int amount)
	{
		if (getAI() instanceof L2SiegeGuardAI || getAI() instanceof L2FortSiegeGuardAI)
		{
			// TODO: this just prevents error until siege guards are handled
			// properly
			stopHating(target);
			setTarget(null);
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			return;
		}
		if (target == null) // whole aggrolist
		{
			L2Character mostHated = getMostHated();
			if (mostHated == null) // makes target passive for a moment more
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-15);
				return;
			}
			else
			{
				for (L2Character aggroed : getAggroList().keySet())
				{
					AggroInfo ai = getAggroList().get(aggroed);
					if (ai == null)
						return;
					ai._aggro -= amount;
				}
			}
			amount = getHating(mostHated);
			if (amount <= 0)
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-15);
				clearAggroList();
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				setWalking();
			}
			return;
		}
		AggroInfo ai = getAggroList().get(target);
		if (ai == null)
			return;
		ai._aggro -= amount;
		if (ai._aggro <= 0)
		{
			if (getMostHated() == null)
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-10);
				clearAggroList();
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				setWalking();
			}
		}
	}
	
	/**
	 * Clears _aggroList hate of the L2Character without removing from the list.
	 */
	public void stopHating(L2Character target)
	{
		if (target == null)
			return;
		AggroInfo ai = getAggroList().get(target);
		if (ai == null)
			return;
		ai._aggro = 0;
	}
	
	/**
	 * Return the most hated L2Character of the L2Attackable _aggroList.
	 */
	public L2Character getMostHated()
	{
		if (getAggroList().isEmpty() || isAlikeDead())
			return null;
		L2Character mostHated = null;
		int maxHate = 0;
		// While Interating over This Map Removing Object is Not Allowed
		synchronized (getAggroList())
		{
			// Go through the aggroList of the L2Attackable
			for (AggroInfo ai : getAggroList().values())
			{
				if (ai == null)
					continue;
				if (ai._attacker.isAlikeDead() || !getKnownList().knowsObject(ai._attacker) || !ai._attacker.isVisible())
				{
					ai._aggro = 0;
				}
				else
				{
					int hating = ai._aggro;
					if (hating > 10)
					{
						final L2PcInstance playa = ai._attacker.getActingPlayer();
						if (playa != null)
						{
							final L2Party party = playa.getParty();
							if (party != null)
							{
								for (L2PcInstance player : party.getPartyMembers())
								{
									if (player == playa)
										continue;
									final int hate = getHating(player) / 2;
									if (hate > 2000)
										hating += hate;
								}
							}
						}
					}
					if (hating > maxHate)
					{
						mostHated = ai._attacker;
						maxHate = hating;
					}
				}
			}
		}
		return mostHated;
	}
	
	/**
	 * Return the 2 most hated L2Character of the L2Attackable _aggroList.
	 */
	public FastList<L2Character> get2MostHated()
	{
		if (getAggroList().isEmpty() || isAlikeDead())
			return null;
		L2Character mostHated = null;
		L2Character secondMostHated = null;
		int maxHate = 0;
		int secondMaxHate = 0;
		FastList<L2Character> result = new FastList<L2Character>();
		// While iterating over this map removing objects is not allowed
		synchronized (getAggroList())
		{
			// Go through the aggroList of the L2Attackable
			for (AggroInfo ai : getAggroList().values())
			{
				if (ai == null)
					continue;
				if (ai._attacker.isAlikeDead() || !getKnownList().knowsObject(ai._attacker) || !ai._attacker.isVisible())
				{
					ai._aggro = 0;
				}
				else
				{
					int hating = ai._aggro;
					if (hating > 10)
					{
						final L2PcInstance playa = ai._attacker.getActingPlayer();
						if (playa != null)
						{
							final L2Party party = playa.getParty();
							if (party != null)
							{
								for (L2PcInstance player : party.getPartyMembers())
								{
									if (player == playa)
										continue;
									final int hate = getHating(player) / 2;
									if (hate > 2000)
										hating += hate;
								}
							}
						}
					}
					if (hating > maxHate)
					{
						secondMostHated = mostHated;
						secondMaxHate = maxHate;
						mostHated = ai._attacker;
						maxHate = hating;
					}
					else if (hating > secondMaxHate)
					{
						secondMostHated = ai._attacker;
						secondMaxHate = hating;
					}
				}
			}
		}
		result.add(mostHated);
		if (secondMostHated != null/*
									 * &&
									 * getAttackByList().contains(secondMostHated
									 * )
									 */)
			result.add(secondMostHated);
		else
			result.add(null);
		return result;
	}
	
	/**
	 * Return the hate level of the L2Attackable against this L2Character
	 * contained in _aggroList.
	 * 
	 * @param target
	 *            The L2Character whose hate level must be returned
	 */
	public int getHating(L2Character target)
	{
		if (getAggroList().isEmpty())
			return 0;
		AggroInfo ai;
		if (target instanceof L2DecoyInstance)
			ai = getAggroList().get(target.getActingPlayer());
		else
			ai = getAggroList().get(target);
		if (ai == null)
			return 0;
		if (ai._attacker instanceof L2PcInstance && (((L2PcInstance) ai._attacker).isInvisible() || ai._attacker.isInvul()) && !canSeeInvisiblePeople())
		{
			/*
			 * //Remove Object Should Use This Method and Can be Blocked While
			 * Interating getAggroList().remove(target);
			 */
			return 0;
		}
		if (!ai._attacker.isVisible())
		{
			getAggroList().remove(target);
			return 0;
		}
		if (ai._attacker.isAlikeDead())
		{
			ai._aggro = 0;
			return 0;
		}
		return ai._aggro;
	}
	
	/**
	 * Calculates quantity of items for specific drop acording to current
	 * situation
	 * 
	 * @param drop
	 *            The L2DropData count is being calculated for
	 * @param lastAttacker
	 *            The L2PcInstance that has killed the L2Attackable
	 * @param deepBlueDrop
	 *            Factor to divide the drop chance
	 * @param levelModifier
	 *            level modifier in %'s (will be subtracted from drop chance)
	 */
	private RewardItem calculateRewardItem(L2PcInstance lastAttacker, L2DropData drop, int levelModifier, boolean isSweep)
	{
		// Get default drop chance
		float dropChance = drop.getChance();
		int deepBlueDrop = 1;
		final int itemId = drop.getItemId();
		if ((!isRaid() && Config.DEEPBLUE_DROP_RULES) || (isRaid() && Config.DEEPBLUE_DROP_RULES_RAID))
		{
			if (levelModifier > 0)
			{
				// We should multiply by the server's drop rate, so we always
				// get a low chance of drop for deep blue mobs.
				// NOTE: This is valid only for adena drops! Others drops will
				// still obey server's rate
				deepBlueDrop = 3;
				if (itemId == 57)
					deepBlueDrop *= isRaid() && !isRaidMinion() ? (int) Config.RATE_DROP_ITEMS_BY_RAID : (int) Config.RATE_DROP_ITEMS;
			}
		}
		// Avoid dividing by 0
		if (deepBlueDrop == 0)
			deepBlueDrop = 1;
		// Check if we should apply our maths so deep blue mobs will not drop
		// that easy
		if ((!isRaid() && Config.DEEPBLUE_DROP_RULES) || (isRaid() && Config.DEEPBLUE_DROP_RULES_RAID))
			dropChance = ((drop.getChance() - ((drop.getChance() * levelModifier) / 100)) / deepBlueDrop);
		// Applies Drop rates
		if (itemId == 57)
			dropChance *= Config.RATE_DROP_ADENA;
		else if (isSweep)
			dropChance *= Config.RATE_DROP_SPOIL;
		else
			dropChance *= isRaid() && !isRaidMinion() ? Config.RATE_DROP_ITEMS_BY_RAID : Config.RATE_DROP_ITEMS;
		if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
			dropChance *= Config.L2JMOD_CHAMPION_REWARDS;
		// Round drop chance
		dropChance = Math.round(dropChance);
		// Set our limits for chance of drop
		if (dropChance < 1)
			dropChance = 1;
		// Get min and max Item quantity that can be dropped in one time
		int minCount = drop.getMinDrop();
		int maxCount = drop.getMaxDrop();
		int itemCount = 0;
		// Count and chance adjustment for high rate servers
		if (dropChance > L2DropData.MAX_CHANCE && !Config.PRECISE_DROP_CALCULATION)
		{
			int multiplier = (int) dropChance / L2DropData.MAX_CHANCE;
			if (minCount < maxCount)
				itemCount += Rnd.get(minCount * multiplier, maxCount * multiplier);
			else if (minCount == maxCount)
				itemCount += minCount * multiplier;
			else
				itemCount += multiplier;
			dropChance = dropChance % L2DropData.MAX_CHANCE;
		}
		// Check if the Item must be dropped
		int random = Rnd.get(L2DropData.MAX_CHANCE);
		while (random < dropChance)
		{
			// Get the item quantity dropped
			if (minCount < maxCount)
				itemCount += Rnd.get(minCount, maxCount);
			else if (minCount == maxCount)
				itemCount += minCount;
			else
				itemCount++;
			// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
			dropChance -= L2DropData.MAX_CHANCE;
		}
		if (Config.L2JMOD_CHAMPION_ENABLE)
			// TODO (April 11, 2009): Find a way not to hardcode these values.
			if ((itemId == 57 || (itemId >= 6360 && itemId <= 6362)) && isChampion())
				itemCount *= Config.L2JMOD_CHAMPION_ADENAS_REWARDS;
		if (itemCount > 0)
			return new RewardItem(itemId, itemCount, drop.getEnchantLevel(), drop.getPartyDropCount(), (int)dropChance);
		else if (itemCount == 0 && Config.DEBUG)
			_log.fine("Roll produced no drops.");
		return null;
	}
	
	/**
	 * Calculates quantity of items for specific drop CATEGORY according to
	 * current situation Only a max of ONE item from a category is allowed to be
	 * dropped.
	 * 
	 * @param drop
	 *            The L2DropData count is being calculated for
	 * @param lastAttacker
	 *            The L2PcInstance that has killed the L2Attackable
	 * @param deepBlueDrop
	 *            Factor to divide the drop chance
	 * @param levelModifier
	 *            level modifier in %'s (will be subtracted from drop chance)
	 */
	private RewardItem calculateCategorizedRewardItem(L2PcInstance lastAttacker, L2DropCategory categoryDrops, int levelModifier)
	{
		if (categoryDrops == null)
			return null;
		double nerf = 1;
		double amountNerf = 1;
		if (lastAttacker.isInUniqueInstance())
		{
			Instance world = InstanceManager.getInstance().getPlayerInstance(lastAttacker.getObjectId());
			if (world != null)
			{
				if (world.getName().contains("Kamaloka"))
				{
					amountNerf = (double) world.getPlayers().size() / 3;
					if (lastAttacker.getParty() == null)
						amountNerf = 1 / 3;
					else
					{
						amountNerf = Math.min(amountNerf, (double) lastAttacker.getParty().getMemberCount() / 3);
					}
					if (amountNerf > 1)
					{
						_log.warning("LOL WTF " + lastAttacker.getName() + "'s kamaloka instance has more than 3 peole in it!");
						amountNerf = 1;
					}
					else if (amountNerf < 1)
					{
						/*
						 * _log.warning(lastAttacker.getName()+
						 * "'s Kamaloka instance had a drop nerf of "
						 * +amountNerf);
						 */
					}
					if (lastAttacker.getParty() != null)
					{
						for (L2PcInstance player : lastAttacker.getParty().getPartyMembers())
						{
							if (player.getInstanceId() == 0)
								continue;
							if (player.getInstanceId() != lastAttacker.getInstanceId())
							{
								lastAttacker.sendMessage("Drop has been not received. Reason:" + player.getName() + "'s instance is different.");
								amountNerf = 0;
								break;
							}
						}
					}
				}
			}
			else
			{}
		}
		// Get default drop chance for the category (that's the sum of chances
		// for all items in the category)
		// keep track of the base category chance as it'll be used later, if an
		// item is drop from the category.
		// for everything else, use the total "categoryDropChance"
		int categoryDropChance = categoryDrops.getCategoryChance();
		if (categoryDropChance < 2)
			return null;
		int deepBlueDrop = 1;
		if ((!isRaid() && Config.DEEPBLUE_DROP_RULES) || (isRaid() && Config.DEEPBLUE_DROP_RULES_RAID))
		{
			// We should multiply by the server's drop rate, so we always get a
			// low chance of drop for deep blue mobs.
			// NOTE: This is valid only for adena drops! Others drops will still
			// obey server's rate
			if (levelModifier > 0)
				deepBlueDrop = 3;
		}
		// Avoid dividing by 0
		if (deepBlueDrop == 0)
			deepBlueDrop = 1;
		// Check if we should apply our maths so deep blue mobs will not drop
		// that easy
		if ((!isRaid() && Config.DEEPBLUE_DROP_RULES) || (isRaid() && Config.DEEPBLUE_DROP_RULES_RAID))
			categoryDropChance = ((categoryDropChance - ((categoryDropChance * levelModifier) / 100)) / deepBlueDrop);
		// Applies Drop rates
		categoryDropChance *= isRaid() && !isRaidMinion() ? Config.RATE_DROP_ITEMS_BY_RAID : Config.RATE_DROP_ITEMS;
		if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
			categoryDropChance *= Config.L2JMOD_CHAMPION_REWARDS;
		if (categoryDropChance < L2DropData.MAX_CHANCE)
			categoryDropChance *= nerf * amountNerf;
		// Round drop chance
		categoryDropChance = Math.round(categoryDropChance);
		// Set our limits for chance of drop
		if (categoryDropChance < 1)
			categoryDropChance = 1;
		// Check if an Item from this category must be dropped
		if (Rnd.get(L2DropData.MAX_CHANCE) < categoryDropChance)
		{
			L2DropData drop = categoryDrops.dropOne(isRaid() && !isRaidMinion());
			if (drop == null)
				return null;
			// Now decide the quantity to drop based on the rates and penalties.
			// To get this value
			// simply divide the modified categoryDropChance by the base
			// category chance. This
			// results in a chance that will dictate the drops amounts: for each
			// amount over 100
			// that it is, it will give another chance to add to the min/max
			// quantities.
			// For example, If the final chance is 120%, then the item should
			// drop between
			// its min and max one time, and then have 20% chance to drop again.
			// If the final
			// chance is 330%, it will similarly give 3 times the min and max,
			// and have a 30%
			// chance to give a 4th time.
			// At least 1 item will be dropped for sure. So the chance will be
			// adjusted to 100%
			// if smaller.
			final int itemId = drop.getItemId();
			int dropChance = drop.getChance();
			int dropChanceToDisp = drop.getChance();
			if (itemId == 57)
				dropChance *= Config.RATE_DROP_ADENA;
			else
				dropChance *= isRaid() && !isRaidMinion() ? Config.RATE_DROP_ITEMS_BY_RAID : Config.RATE_DROP_ITEMS;
			if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
				dropChance *= Config.L2JMOD_CHAMPION_REWARDS;
			dropChance = Math.round(dropChance);
			if (dropChance < L2DropData.MAX_CHANCE)
				dropChance = L2DropData.MAX_CHANCE;
			// Get min and max Item quantity that can be dropped in one time
			int min = drop.getMinDrop();
			int max = drop.getMaxDrop();
			// Get the item quantity dropped
			int itemCount = 0;
			// Count and chance adjustment for high rate servers
			if (dropChance > L2DropData.MAX_CHANCE && !Config.PRECISE_DROP_CALCULATION)
			{
				int multiplier = dropChance / L2DropData.MAX_CHANCE;
				if (min < max)
					itemCount += Rnd.get(min * multiplier, max * multiplier);
				else if (min == max)
					itemCount += min * multiplier;
				else
					itemCount += multiplier;
				dropChance = dropChance % L2DropData.MAX_CHANCE;
			}
			// Check if the Item must be dropped
			int random = Rnd.get(L2DropData.MAX_CHANCE);
			while (random < dropChance)
			{
				// Get the item quantity dropped
				if (min < max)
					itemCount += Rnd.get(min, max);
				else if (min == max)
					itemCount += min;
				else
					itemCount++;
				// Prepare for next iteration if dropChance >
				// L2DropData.MAX_CHANCE
				dropChance -= L2DropData.MAX_CHANCE;
			}
			if (Config.L2JMOD_CHAMPION_ENABLE)
				// TODO (April 11, 2009): Find a way not to hardcode these
				// values.
				if ((itemId == 57 || (itemId >= 6360 && itemId <= 6362)) && isChampion())
					itemCount *= Config.L2JMOD_CHAMPION_ADENAS_REWARDS;
			itemCount *= amountNerf;
			if (!Config.MULTIPLE_ITEM_DROP && !isStackable(itemId) && itemCount > 1)
				itemCount = 1;
			if (itemCount > 0)
				return new RewardItem(itemId, itemCount, drop.getEnchantLevel(), drop.getPartyDropCount(), (int)dropChanceToDisp);
			else if (itemCount == 0 && Config.DEBUG)
				_log.fine("Roll produced no drops.");
		}
		return null;
	}
	
	private static boolean isStackable(int itemId)
	{
		if (itemId > 1000000)
			return true;
		final L2Item temp = ItemTable.getInstance().getTemplate(itemId);
		return temp != null && temp.isStackable();
	}
	
	/**
	 * Calculates the level modifier for drop
	 * 
	 * @param lastAttacker
	 *            The L2PcInstance that has killed the L2Attackable
	 */
	private int calculateLevelModifierForDrop(L2PcInstance lastAttacker)
	{
		if ((!isRaid() && Config.DEEPBLUE_DROP_RULES) || (isRaid() && Config.DEEPBLUE_DROP_RULES_RAID))
		{
			int highestLevel = lastAttacker.getLevel();
			// Check to prevent very high level player to nearly kill mob and
			// let low level player do the last hit.
			if (getAttackByList() != null && !getAttackByList().isEmpty())
			{
				for (L2Character atkChar : getAttackByList())
					if (atkChar != null && atkChar.getLevel() > highestLevel)
						highestLevel = atkChar.getLevel();
			}
			// According to official data (Prima), deep blue mobs are 9 or more
			// levels below players
			if (highestLevel - 9 >= getLevel())
				return ((highestLevel - (getLevel() + 8)) * 9);
		}
		return 0;
	}
	
	public void doItemDrop(L2Character lastAttacker)
	{
		doItemDrop(getTemplate(), lastAttacker);
	}
	
	/**
	 * Manage Base, Quests and Special Events drops of L2Attackable (called by
	 * calculateRewards).
	 * Concept: During a Special Event all L2Attackable can drop extra Items.
	 * Those extra Items are defined in the table allNpcDateDrops of the
	 * EventDroplist. Each Special Event has a start and end date to stop to
	 * drop extra Items automaticaly.
	 * Actions: Manage drop of Special Events created by GM for a defined period
	 * Get all possible drops of this L2Attackable from L2NpcTemplate and add it
	 * Quest drops For each possible drops (base + quests), calculate which one
	 * must be dropped (random) Get each Item quantity dropped (random) Create
	 * this or these L2ItemInstance corresponding to each Item Identifier
	 * dropped If the autoLoot mode is actif and if the L2Character that has
	 * killed the L2Attackable is a L2PcInstance, Give the item(s) to the
	 * L2PcInstance that has killed the L2Attackable If the autoLoot mode isn't
	 * actif or if the L2Character that has killed the L2Attackable is not a
	 * L2PcInstance, add this or these item(s) in the world as a visible object
	 * at the position where mob was last
	 * 
	 * @param lastAttacker
	 *            The L2Character that has killed the L2Attackable
	 */
	public void doItemDrop(L2NpcTemplate npcTemplate, L2Character lastAttacker)
	{
		if (lastAttacker == null)
			return;
		L2PcInstance player = lastAttacker.getActingPlayer();
		// Don't drop anything if the last attacker or owner isn't L2PcInstance
		if (player == null)
			return;
		/*
		 * if (isLevelOneRaidboss() || isLevelTwoRaidboss()) { final L2Clan clan
		 * = player.getClan();
		 * if (clan != null && clan.getRBkills() >= 9) {
		 * player.setPunishLevel(L2PcInstance.PunishLevel.JAIL, 45);
		 * player.sendMessage(
		 * "You are jailed for killing a raidboss when your clan has already killed 8 or more raidbosses"
		 * ); return; } }
		 */
		// level modifier in %'s (will be subtracted from drop chance)
		int levelModifier = calculateLevelModifierForDrop(player);
		CursedWeaponsManager.getInstance().checkDrop(this, player);
		// now throw all categorized drops and handle spoil.
		if (npcTemplate.getDropData() != null)
		{
			for (L2DropCategory cat : npcTemplate.getDropData())
			{
				RewardItem item = null;
				if (cat.isSweep())
				{
					// according to sh1ny, seeded mobs CAN be spoiled and swept.
					if (isSpoil()/* && !isSeeded() */)
					{
						FastList<RewardItem> sweepList = new FastList<RewardItem>();
						for (L2DropData drop : cat.getAllDrops())
						{
							item = calculateRewardItem(player, drop, levelModifier, true);
							if (item == null)
								continue;
							if (Config.DEBUG)
								_log.fine("Item id to spoil: " + item.getItemId() + " amount: " + item.getCount());
							sweepList.add(item);
						}
						// Set the table _sweepItems of this L2Attackable
						if (!sweepList.isEmpty())
							_sweepItems = sweepList.toArray(new RewardItem[sweepList.size()]);
					}
				}
				else
				{
					if (isSeeded())
					{
						L2DropData drop = cat.dropSeedAllowedDropsOnly();
						if (drop == null)
							continue;
						item = calculateRewardItem(player, drop, levelModifier, false);
					}
					else
						item = calculateCategorizedRewardItem(player, cat, levelModifier);
					if (item != null)
					{
						final int id = item.getItemId();
						if (id > 1000000)
						{
							item.setItemId(ItemLists.getInstance().generateRandomItemFromList(id));
						}
						if (Config.DEBUG)
							_log.fine("Item id to drop: " + item.getItemId() + " amount: " + item.getCount());
						// Check if the autoLoot mode is active
						if (isFlying() || (!isRaid() && Config.AUTO_LOOT) || (isRaid() && Config.AUTO_LOOT_RAIDS) || item.getPartyDropCount() >= 1 || item.getItemId() == 98020 || item.getItemId() == 98021 || item.getItemId() == 98022)
							player.doAutoLoot(this, item); // Give the item(s)
															// to the
															// L2PcInstance that
															// has killed the
															// L2Attackable
						else
							dropItem(player, item); // drop the item on the
													// ground
						// Broadcast message if RaidBoss was defeated
						if (isRaid() && !isRaidMinion())
						{
							SystemMessage sm;
							sm = new SystemMessage(SystemMessageId.C1_DIED_DROPPED_S3_S2);
							sm.addCharName(this);
							sm.addItemName(item.getItemId());
							sm.addItemNumber(item.getCount());
							broadcastPacket(sm);
						}
					}
				}
			}
		}
		// Apply Special Item drop with random(rnd) quantity(qty) for champions.
		if (Config.L2JMOD_CHAMPION_ENABLE && isChampion() && (Config.L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE > 0 || Config.L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE > 0))
		{
			int champqty = Rnd.get(Config.L2JMOD_CHAMPION_REWARD_QTY);
			RewardItem item = new RewardItem(Config.L2JMOD_CHAMPION_REWARD_ID, ++champqty);
			if (player.getLevel() <= getLevel() && (Rnd.get(100) < Config.L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE))
			{
				if (Config.AUTO_LOOT || isFlying())
					player.addItem("ChampionLoot", item.getItemId(), item.getCount(), this, true); // Give
																									// the
																									// item(s)
																									// to
																									// the
																									// L2PcInstance
																									// that
																									// has
																									// killed
																									// the
																									// L2Attackable
				else
					dropItem(player, item);
			}
			else if (player.getLevel() > getLevel() && (Rnd.get(100) < Config.L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE))
			{
				if (Config.AUTO_LOOT || isFlying())
					player.addItem("ChampionLoot", item.getItemId(), item.getCount(), this, true); // Give
																									// the
																									// item(s)
																									// to
																									// the
																									// L2PcInstance
																									// that
																									// has
																									// killed
																									// the
																									// L2Attackable
				else
					dropItem(player, item);
			}
		}
		/*
		 * else if (isLevelOneRaidboss()) { RewardItem item = new
		 * RewardItem(4357, 110); //110 Silver shilen
		 * distributeCommonDropToParty(player, item);
		 * item = new RewardItem(4357, 100);
		 * for (int i = 0; i < 5; i++) { if (Config.AUTO_LOOT_RAIDS)
		 * player.doAutoLoot(this, item); else dropItem(player, item); }
		 * item = new RewardItem(-300, 30); //30 fame
		 * distributeCommonDropToParty(player, item);
		 * final L2Clan clan = player.getClan();
		 * if (clan != null) { clan.setReputationScore(clan.getReputationScore()
		 * + 50, true); clan.broadcastToOnlineMembers(new
		 * SystemMessage(SystemMessageId.ACQUIRED_50_CLAN_FAME_POINTS)); }
		 * int chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_1) { item = new RewardItem(4356,
		 * 1); //1 gold
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_2) { item = new RewardItem(8752,
		 * 1); //1 high grade life stone lvl 76
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_3) { item = new RewardItem(9574,
		 * 1); //1 mid grade life stone lvl 80
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); } else { item = new RewardItem(9573, 1); //1
		 * no grade life stone lvl 80
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * for (int i = 0; i < 2; i++) { item = new
		 * RewardItem(elementalStones[Rnd.get(elementalStones.length)], 1); //2
		 * random elemental stones, second one at 45%.
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item);
		 * if (Rnd.get(100) < Config.RAIDBOSS_CHANCE_4) break; }
		 * chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_5) { item = new
		 * RewardItem(elementalCrystals[Rnd.get(elementalCrystals.length)], 1);
		 * //1 random elemental crystal
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * item = new RewardItem(6622, 1); //3 giant's codex's guaranteed
		 * for (int i = 0; i < 3; i++) { if (Config.AUTO_LOOT_RAIDS)
		 * player.doAutoLoot(this, item); else dropItem(player, item); }
		 * chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_6) { item = new RewardItem(9590,
		 * 1); //1 Bronze Bracelet
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * item = new RewardItem(6673, 1); //festival adena
		 * for (int i = 0; i < 4; i++) { if (Config.AUTO_LOOT_RAIDS)
		 * player.doAutoLoot(this, item); else dropItem(player, item); }
		 * chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_7) { item = new RewardItem(9591,
		 * 1); //1 steel Bracelet
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * item = new RewardItem(962, 4); //crystal armor S
		 * for (int i = 0; i < 5; i++) { if (Config.AUTO_LOOT_RAIDS)
		 * player.doAutoLoot(this, item); else dropItem(player, item); }
		 * chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_8) { item = new
		 * RewardItem(protectionBooks[Rnd.get(protectionBooks.length)], 1); //1
		 * random protection book
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_9) { chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_9/3) item = new RewardItem(13939,
		 * 1); //1 cloth belt - regular magic else item = new RewardItem(13894,
		 * 1); //1 cloth belt
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_10) { chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_10) item = new RewardItem(13943,
		 * 1); //1 magic leather belt else item = new RewardItem(13895, 1); //1
		 * leather belt
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item);
		 * Broadcast.announceToOnlinePlayersGlobal(
		 * "Owww dude 4 strength 4 stam leather belt? AHHHHHHHH!", "Joe");
		 * Broadcast.announceToOnlinePlayersGlobal("Level 18? AHHHHHH OHHHHHH",
		 * "Joe"); }
		 * chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_11) { item = new
		 * RewardItem(clanRepEggs[Rnd.get(clanRepEggs.length)], 1); //1 clan rep
		 * egg
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_12) { chance = Rnd.get(100);
		 * if (chance < 50) item = new
		 * RewardItem(icarusWeapons[Rnd.get(icarusWeapons.length)], 1);
		 * //ICARIUS WEAPONS! else item = new RewardItem(10413, 1); //essence of
		 * dynasty/dynasty essence II
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); } } else if (isLevelTwoRaidboss()) {
		 * RewardItem item = new RewardItem(4357, 200); //200 Silver shilen
		 * distributeCommonDropToParty(player, item);
		 * item = new RewardItem(4358, 1); // 1x bloody paggrio to everyone in
		 * party distributeCommonDropToParty(player, item);
		 * for (int i = 0; i < 4; i++) { if (Config.AUTO_LOOT_RAIDS)
		 * player.doAutoLoot(this, item); else dropItem(player, item); }
		 * item = new RewardItem(-300, 100); //100 fame
		 * distributeCommonDropToParty(player, item);
		 * final L2Clan clan = player.getClan();
		 * if (clan != null) { clan.setReputationScore(clan.getReputationScore()
		 * + 200, true);
		 * clan.broadcastToOnlineMembers("Clan reputation increased by 200 points."
		 * ); }
		 * int chance = Rnd.get(50);
		 * if (chance < Config.RAIDBOSS_CHANCE_1) { item = new RewardItem(4356,
		 * 1); //1 gold
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(50);
		 * if (chance < Config.RAIDBOSS_CHANCE_2) { item = new RewardItem(8752,
		 * 1); //1 high grade life stone lvl 76
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(50);
		 * if (chance < Config.RAIDBOSS_CHANCE_3) { item = new RewardItem(9574,
		 * 1); //1 mid grade life stone lvl 80
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); } else { item = new RewardItem(9573, 1); //1
		 * no grade life stone lvl 80
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * for (int i = 0; i < 3; i++) { item = new
		 * RewardItem(elementalStones[Rnd.get(elementalStones.length)], 1); //2
		 * random elemental stones, second one at 45%.
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(50);
		 * if (chance < Config.RAIDBOSS_CHANCE_5) { item = new
		 * RewardItem(elementalCrystals[Rnd.get(elementalCrystals.length)], 1);
		 * //1 random elemental crystal
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * item = new RewardItem(6622, 1); //3 giant's codex's guaranteed
		 * for (int i = 0; i < 4; i++) { if (Config.AUTO_LOOT_RAIDS)
		 * player.doAutoLoot(this, item); else dropItem(player, item); }
		 * chance = Rnd.get(50);
		 * if (chance < Config.RAIDBOSS_CHANCE_6) { item = new RewardItem(9590,
		 * 1); //1 Bronze Bracelet
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * item = new RewardItem(6673, 1); //festival adena
		 * for (int i = 0; i < 4; i++) { if (Config.AUTO_LOOT_RAIDS)
		 * player.doAutoLoot(this, item); else dropItem(player, item); }
		 * chance = Rnd.get(50);
		 * if (chance < Config.RAIDBOSS_CHANCE_7) { item = new RewardItem(9591,
		 * 1); //1 steel Bracelet
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * item = new RewardItem(962, 4); //crystal armor S
		 * for (int i = 0; i < 5; i++) { if (Config.AUTO_LOOT_RAIDS)
		 * player.doAutoLoot(this, item); else dropItem(player, item); }
		 * chance = Rnd.get(50);
		 * if (chance < Config.RAIDBOSS_CHANCE_8) { item = new
		 * RewardItem(protectionBooks[Rnd.get(protectionBooks.length)], 1); //1
		 * random protection book
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(50);
		 * if (chance < Config.RAIDBOSS_CHANCE_9) { chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_9/3) item = new RewardItem(13939,
		 * 1); //1 cloth belt - regular magic else item = new RewardItem(13894,
		 * 1); //1 cloth belt
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(50);
		 * if (chance < Config.RAIDBOSS_CHANCE_10) { chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_10) item = new RewardItem(13943,
		 * 1); //1 magic leather belt else item = new RewardItem(13895, 1); //1
		 * leather belt
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item);
		 * Broadcast.announceToOnlinePlayersGlobal(
		 * "Owww dude 4 strength 4 stam leather belt? AHHHHHHHH!", "Joe");
		 * Broadcast.announceToOnlinePlayersGlobal("Level 18? AHHHHHH OHHHHHH",
		 * "Joe"); }
		 * chance = Rnd.get(50);
		 * if (chance < Config.RAIDBOSS_CHANCE_11) { item = new
		 * RewardItem(clanRepEggs[Rnd.get(clanRepEggs.length)], 1); //1 clan rep
		 * egg
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(45);
		 * if (chance < Config.RAIDBOSS_CHANCE_12) { chance = Rnd.get(100);
		 * if (chance < 50) item = new
		 * RewardItem(icarusWeapons[Rnd.get(icarusWeapons.length)], 1);
		 * //ICARIUS WEAPONS! else item = new RewardItem(10413, 1); //essence of
		 * dynasty/dynasty essence II
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); }
		 * chance = Rnd.get(100);
		 * if (chance < Config.RAIDBOSS_CHANCE_13) { item = new
		 * RewardItem(sealedVesperArmorPieces
		 * [Rnd.get(sealedVesperArmorPieces.length)], 1); //one random sealed
		 * vesper armor
		 * if (Config.AUTO_LOOT_RAIDS) player.doAutoLoot(this, item); else
		 * dropItem(player, item); } }
		 */
		// Instant Item Drop :>
		// TODO (April 11, 2009): Try to change herb drop to switch/case. Very
		// ugly code right now.
		if (getTemplate().dropherb)
		{
			boolean _hp = false;
			boolean _mp = false;
			boolean _spec = false;
			// Herb of Warrior
			int random = Rnd.get(1000);
			if ((random < Config.RATE_DROP_SPECIAL_HERBS) && !_spec)
			{
				RewardItem item = new RewardItem(8612, 1);
				if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
					player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
				else
					dropItem(player, item);
				_spec = true;
			}
			else
				for (int i = 0; i < 5; i++)
				{
					random = Rnd.get(100);
					if (random < Config.RATE_DROP_COMMON_HERBS)
					{
						RewardItem item = null;
						if (i == 0)
							item = new RewardItem(8606, 1); // Herb of Power
						if (i == 1)
							item = new RewardItem(8608, 1); // Herb of Atk. Spd.
						if (i == 2)
							item = new RewardItem(8610, 1); // Herb of Critical
															// Attack - Rate
						if (i == 3)
							item = new RewardItem(10655, 1); // Herb of Life
																// Force
																// Absorption
						if (i == 4)
							item = new RewardItem(10656, 1); // Herb of Critical
																// Attack -
																// Power
						if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						else
							dropItem(player, item);
						break;
					}
				}
			// Herb of Mystic
			random = Rnd.get(1000);
			if ((random < Config.RATE_DROP_SPECIAL_HERBS) && !_spec)
			{
				RewardItem item = new RewardItem(8613, 1);
				if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
					player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
				else
					dropItem(player, item);
				_spec = true;
			}
			else
				for (int i = 0; i < 2; i++)
				{
					random = Rnd.get(100);
					if (random < Config.RATE_DROP_COMMON_HERBS)
					{
						RewardItem item = null;
						if (i == 0)
							item = new RewardItem(8607, 1); // Herb of Magic
						if (i == 1)
							item = new RewardItem(8609, 1); // Herb of Casting
															// Speed
						if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						else
							dropItem(player, item);
						break;
					}
				}
			// Herb of Recovery
			random = Rnd.get(1000);
			if ((random < Config.RATE_DROP_SPECIAL_HERBS) && !_spec)
			{
				RewardItem item = new RewardItem(8614, 1);
				if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
					player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
				else
					dropItem(player, item);
				_mp = true;
				_hp = true;
				_spec = true;
			}
			// Herb of Life
			if (!_hp)
			{
				random = Rnd.get(100);
				if (random < Config.RATE_DROP_MP_HP_HERBS)
				{
					RewardItem item = new RewardItem(8600, 1);
					if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					else
						dropItem(player, item);
					_hp = true;
				}
			}
			// Greater Herb of Life
			if (!_hp)
			{
				random = Rnd.get(100);
				if (random < Config.RATE_DROP_GREATER_HERBS)
				{
					RewardItem item = new RewardItem(8601, 1);
					if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					else
						dropItem(player, item);
					_hp = true;
				}
			}
			// Superior Herb of Life
			if (!_hp)
			{
				random = Rnd.get(1000);
				if (random < Config.RATE_DROP_SUPERIOR_HERBS)
				{
					RewardItem item = new RewardItem(8602, 1);
					if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					else
						dropItem(player, item);
				}
			}
			// Herb of Mana
			if (!_mp)
			{
				random = Rnd.get(100);
				if (random < Config.RATE_DROP_MP_HP_HERBS)
				{
					RewardItem item = new RewardItem(8603, 1);
					if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					else
						dropItem(player, item);
					_mp = true;
				}
			}
			// Greater Herb of Mana
			if (!_mp)
			{
				random = Rnd.get(100);
				if (random < Config.RATE_DROP_GREATER_HERBS)
				{
					RewardItem item = new RewardItem(8604, 1);
					if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					else
						dropItem(player, item);
					_mp = true;
				}
			}
			// Superior Herb of Mana
			if (!_mp)
			{
				random = Rnd.get(1000);
				if (random < Config.RATE_DROP_SUPERIOR_HERBS)
				{
					RewardItem item = new RewardItem(8605, 1);
					if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					else
						dropItem(player, item);
				}
			}
			// speed enhance type
			random = Rnd.get(100);
			if (random < Config.RATE_DROP_COMMON_HERBS)
			{
				RewardItem item = new RewardItem(8611, 1); // Herb of Speed
				if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
					player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
				else
					dropItem(player, item);
			}
			// Enlarge Head type
			random = Rnd.get(100);
			if (random < Config.RATE_DROP_COMMON_HERBS)
			{
				RewardItem item = new RewardItem(10657, 1); // Herb of Doubt
				if ((Config.AUTO_LOOT || isFlying()) && Config.AUTO_LOOT_HERBS)
					player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
				else
					dropItem(player, item);
			}
			// Vitality Herb
			if (Config.ENABLE_VITALITY && Config.ENABLE_DROP_VITALITY_HERBS)
			{
				random = Rnd.get(100);
				if (random < Config.RATE_DROP_VITALITY_HERBS)
				{
					RewardItem item = new RewardItem(13028, 1);
					if (Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					else
						dropItem(player, item);
				}
			}
		}
	}
	
	final public void distributeCommonDropToParty(final L2PcInstance player, final RewardItem item)
	{
		boolean fame = false;
		if (item.getItemId() == -300)
			fame = true;
		final L2Party party = player.getParty();
		if (party != null)
		{
			final FastList<String> playerIPs = new FastList<String>();
			for (L2PcInstance dude : party.getPartyMembers())
			{
				if (dude == null)
					continue;
				if (dude.getLevel() < 82)
					continue;
				if (!dude.isInsideRadius(this, 1400, true, true))
					continue;
				if (!getAggroList().containsKey(dude))
					continue;
				final String playerIP = dude.getIP();
				if (playerIP == null || playerIP.equalsIgnoreCase("") || playerIP.length() < 7)
					continue;
				if (playerIPs.contains(playerIP))
					continue;
				playerIPs.add(playerIP);
				if (fame)
				{
					dude.setFame(dude.getFame() + item.getCount());
					dude.sendMessage("Your fame has increased by " + item.getCount() + " for defeating this Raidboss.");
				}
				else
					dude.addItem("RB Common Drop", item.getItemId(), item.getCount(), this, true);
			}
		}
		else
		{
			if (fame)
			{
				player.setFame(player.getFame() + item.getCount());
				player.sendMessage("Your fame has increased by " + item.getCount() + " for defeating this Raidboss.");
			}
			else
				player.addItem("RB Common Drop", item.getItemId(), item.getCount(), this, true);
		}
	}
	
	/**
	 * Manage Special Events drops created by GM for a defined period.
	 * Concept: During a Special Event all L2Attackable can drop extra Items.
	 * Those extra Items are defined in the table allNpcDateDrops of the
	 * EventDroplist. Each Special Event has a start and end date to stop to
	 * drop extra Items automaticaly.
	 * Actions: <I>If an extra drop must be generated</I> Get an Item Identifier
	 * (random) from the DateDrop Item table of this Event Get the Item quantity
	 * dropped (random) Create this or these L2ItemInstance corresponding to
	 * this Item Identifier If the autoLoot mode is actif and if the L2Character
	 * that has killed the L2Attackable is a L2PcInstance, Give the item(s) to
	 * the L2PcInstance that has killed the L2Attackable If the autoLoot mode
	 * isn't actif or if the L2Character that has killed the L2Attackable is not
	 * a L2PcInstance, add this or these item(s) in the world as a visible
	 * object at the position where mob was last
	 * 
	 * @param lastAttacker
	 *            The L2Character that has killed the L2Attackable
	 */
	public void doEventDrop(L2Character lastAttacker)
	{
		L2PcInstance player = null;
		if (lastAttacker instanceof L2PcInstance)
			player = (L2PcInstance) lastAttacker;
		else if (lastAttacker instanceof L2Summon)
			player = ((L2Summon) lastAttacker).getOwner();
		else if (lastAttacker instanceof L2TrapInstance)
			player = ((L2TrapInstance) lastAttacker).getOwner();
		// Don't drop anything if the last attacker or owner isn't L2PcInstance
		if (player == null)
			return;
		if (player.getLevel() - getLevel() > 9)
			return;
		// Go through DateDrop of EventDroplist allNpcDateDrops within the date
		// range
		for (DateDrop drop : EventDroplist.getInstance().getAllDrops())
		{
			if (Rnd.get(L2DropData.MAX_CHANCE) < drop.chance)
			{
				RewardItem item = new RewardItem(drop.items[Rnd.get(drop.items.length)], Rnd.get(drop.min, drop.max));
				if (Config.AUTO_LOOT || isFlying())
					player.doAutoLoot(this, item); // Give the item(s) to the
													// L2PcInstance that has
													// killed the L2Attackable
				else
					dropItem(player, item); // drop the item on the ground
			}
		}
	}
	
	/**
	 * Drop reward item.
	 */
	public L2ItemInstance dropItem(L2PcInstance lastAttacker, RewardItem item)
	{
		switch (item.getItemId())
		{
			case 98020:
			case 98021:
			case 98022:
				return null;
		}
		int randDropLim = 70;
		L2ItemInstance ditem = null;
		for (int i = 0; i < item.getCount(); i++)
		{
			// Randomize drop position
			int newX = getX() + Rnd.get(randDropLim * 2 + 1) - randDropLim;
			int newY = getY() + Rnd.get(randDropLim * 2 + 1) - randDropLim;
			int newZ = Math.max(getZ(), lastAttacker.getZ()) + 20; // TODO: temp
																	// hack, do
																	// somethign
																	// nicer
																	// when we
																	// have
																	// geodatas
			if (ItemTable.getInstance().getTemplate(item.getItemId()) != null)
			{
				// Init the dropped L2ItemInstance and add it in the world as a
				// visible object at the position where mob was last
				ditem = ItemTable.getInstance().createItem("Loot", item.getItemId(), item.getCount(), lastAttacker, this);
				if (item.getEnchantLevel() > 0 && ditem.isEnchantable())
					ditem.setEnchantLevel(item.getEnchantLevel());
				ditem.addAutoAugmentation();
				ditem.dropMe(this, newX, newY, newZ);
				// Add drop to auto destroy item task
				if (!Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
				{
					if ((Config.AUTODESTROY_ITEM_AFTER > 0 && ditem.getItemType() != L2EtcItemType.HERB) || (Config.HERB_AUTO_DESTROY_TIME > 0 && ditem.getItemType() == L2EtcItemType.HERB))
						ItemsAutoDestroy.getInstance().addItem(ditem);
				}
				ditem.setProtected(false);
				// If stackable, end loop as entire count is included in 1
				// instance of item
				if (ditem.isStackable() || !Config.MULTIPLE_ITEM_DROP)
					break;
			}
			else
				_log.log(Level.SEVERE, "Item doesn't exist so cannot be dropped. Item ID: " + item.getItemId());
		}
		return ditem;
	}
	
	public L2ItemInstance dropItem(L2PcInstance lastAttacker, int itemId, int itemCount)
	{
		return dropItem(lastAttacker, new RewardItem(itemId, itemCount));
	}
	
	/**
	 * Return the active weapon of this L2Attackable (= null).
	 */
	public L2ItemInstance getActiveWeapon()
	{
		return null;
	}
	
	/**
	 * Return True if the _aggroList of this L2Attackable is Empty.
	 */
	public boolean noTarget()
	{
		return getAggroList().isEmpty();
	}
	
	/**
	 * Return True if the _aggroList of this L2Attackable contains the
	 * L2Character.
	 * 
	 * @param player
	 *            The L2Character searched in the _aggroList of the L2Attackable
	 */
	public boolean containsTarget(L2Character player)
	{
		return getAggroList().containsKey(player);
	}
	
	/**
	 * Clear the _aggroList of the L2Attackable.
	 */
	public void clearAggroList()
	{
		getAggroList().clear();
		// clear overhit values
		_overhit = false;
		_overhitDamage = 0;
		_overhitAttacker = null;
	}
	
	/**
	 * Return True if a Dwarf use Sweep on the L2Attackable and if item can be
	 * spoiled.
	 */
	public boolean isSweepActive()
	{
		return _sweepItems != null;
	}
	
	/**
	 * Return table containing all L2ItemInstance that can be spoiled.
	 */
	public synchronized RewardItem[] takeSweep()
	{
		RewardItem[] sweep = _sweepItems;
		_sweepItems = null;
		return sweep;
	}
	
	/**
	 * Return table containing all L2ItemInstance that can be harvested.
	 */
	public synchronized RewardItem[] takeHarvest()
	{
		RewardItem[] harvest = _harvestItems;
		_harvestItems = null;
		return harvest;
	}
	
	/**
	 * Set the over-hit flag on the L2Attackable.
	 * 
	 * @param status
	 *            The status of the over-hit flag
	 */
	public void overhitEnabled(boolean status)
	{
		_overhit = status;
	}
	
	/**
	 * Set the over-hit values like the attacker who did the strike and the
	 * amount of damage done by the skill.
	 * 
	 * @param attacker
	 *            The L2Character who hit on the L2Attackable using the over-hit
	 *            enabled skill
	 * @param damage
	 *            The ammount of damage done by the over-hit enabled skill on
	 *            the L2Attackable
	 */
	public void setOverhitValues(L2Character attacker, double damage)
	{
		// Calculate the over-hit damage
		// Ex: mob had 10 HP left, over-hit skill did 50 damage total, over-hit
		// damage is 40
		double overhitDmg = ((getCurrentHp() - damage) * (-1));
		if (overhitDmg < 0)
		{
			// we didn't killed the mob with the over-hit strike. (it wasn't
			// really an over-hit strike)
			// let's just clear all the over-hit related values
			overhitEnabled(false);
			_overhitDamage = 0;
			_overhitAttacker = null;
			return;
		}
		overhitEnabled(true);
		_overhitDamage = overhitDmg;
		_overhitAttacker = attacker;
	}
	
	/**
	 * Return the L2Character who hit on the L2Attackable using an over-hit
	 * enabled skill.
	 * 
	 * @return L2Character attacker
	 */
	public L2Character getOverhitAttacker()
	{
		return _overhitAttacker;
	}
	
	/**
	 * Return the ammount of damage done on the L2Attackable using an over-hit
	 * enabled skill.
	 * 
	 * @return double damage
	 */
	public double getOverhitDamage()
	{
		return _overhitDamage;
	}
	
	/**
	 * Return True if the L2Attackable was hit by an over-hit enabled skill.
	 */
	public boolean isOverhit()
	{
		return _overhit;
	}
	
	/**
	 * Activate the absorbed soul condition on the L2Attackable.
	 */
	public void absorbSoul()
	{
		_absorbed = true;
	}
	
	/**
	 * Return True if the L2Attackable had his soul absorbed.
	 */
	public boolean isAbsorbed()
	{
		return _absorbed;
	}
	
	/**
	 * Adds an attacker that successfully absorbed the soul of this L2Attackable
	 * into the _absorbersList.
	 * Params: attacker - a valid L2PcInstance condition - an integer indicating
	 * the event when mob dies. This should be: = 0 - "the crystal scatters"; =
	 * 1 - "the crystal failed to absorb. nothing happens"; = 2 -
	 * "the crystal resonates because you got more than 1 crystal on you"; = 3 -
	 * "the crystal cannot absorb the soul because the mob level is too low"; =
	 * 4 - "the crystal successfuly absorbed the soul";
	 */
	public void addAbsorber(L2PcInstance attacker, int crystalId)
	{
		// This just works for targets like L2MonsterInstance
		if (!(this instanceof L2MonsterInstance))
			return;
		// The attacker must not be null
		if (attacker == null)
			return;
		// This L2Attackable must be of one type in the _absorbingMOBS_levelXX
		// tables.
		// OBS: This is done so to avoid triggering the absorbed conditions for
		// mobs that can't be absorbed.
		if (getAbsorbLevel() == 0)
			return;
		// If we have no _absorbersList initiated, do it
		AbsorberInfo ai = _absorbersList.get(attacker);
		// If the L2Character attacker isn't already in the _absorbersList of
		// this L2Attackable, add it
		if (ai == null)
		{
			ai = new AbsorberInfo(attacker, crystalId, getCurrentHp());
			_absorbersList.put(attacker, ai);
		}
		else
		{
			ai._absorber = attacker;
			ai._crystalId = crystalId;
			ai._absorbedHP = getCurrentHp();
		}
		// Set this L2Attackable as absorbed
		absorbSoul();
	}
	
	/**
	 * Calculate the leveling chance of Soul Crystals based on the attacker that
	 * killed this L2Attackable
	 * 
	 * @param attacker
	 *            The player that last killed this L2Attackable $ Rewrite
	 *            06.12.06 - Yesod
	 */
	/*
	 * private void levelSoulCrystals(L2Character attacker) { // Only
	 * L2PcInstance can absorb a soul if (!(attacker instanceof L2PcInstance) &&
	 * !(attacker instanceof L2Summon)) { resetAbsorbList(); return; }
	 * int maxAbsorbLevel = getAbsorbLevel(); int minAbsorbLevel = 0;
	 * // If this is not a valid L2Attackable, clears the _absorbersList and
	 * just return if (maxAbsorbLevel == 0) { resetAbsorbList(); return; }
	 * // All boss mobs with maxAbsorbLevel 13 have minAbsorbLevel of 12 else 10
	 * if (maxAbsorbLevel > 10) minAbsorbLevel = maxAbsorbLevel > 12 ? 12 : 10;
	 * //Init some useful vars boolean isSuccess = true; boolean doLevelup =
	 * true; boolean isBossMob = maxAbsorbLevel > 10 ? true : false;
	 * L2NpcTemplate.AbsorbCrystalType absorbType = getTemplate().absorbType;
	 * L2PcInstance killer = (attacker instanceof L2Summon)?
	 * ((L2Summon)attacker).getOwner() : (L2PcInstance)attacker;
	 * // If this mob is a boss, then skip some checkings if (!isBossMob) { //
	 * Fail if this L2Attackable isn't absorbed or there's no one in its
	 * _absorbersList if (!isAbsorbed() || _absorbersList == null) {
	 * resetAbsorbList(); return; }
	 * // Fail if the killer isn't in the _absorbersList of this L2Attackable
	 * and mob is not boss AbsorberInfo ai = _absorbersList.get(killer); if (ai
	 * == null || ai._absorber.getObjectId() != killer.getObjectId()) isSuccess
	 * = false;
	 * // Check if the soul crystal was used when HP of this L2Attackable wasn't
	 * higher than half of it if (ai != null && ai._absorbedHP >
	 * (getMaxHp()/2.0)) isSuccess = false;
	 * if (!isSuccess) { resetAbsorbList(); return; } }
	 * String[] crystalNFO = null;
	 * int dice = Rnd.get(100); int crystalQTY = 0; int crystalLVL = 0; int
	 * crystalOLD = 0; int crystalNEW = 0;
	 * // Now we have four choices: // 1- The Monster level is too low for the
	 * crystal. Nothing happens. // 2- Everything is correct, but it failed.
	 * Nothing happens. (57.5%) // 3- Everything is correct, the crystal level
	 * up. A sound event is played. (32.5%) List<L2PcInstance> players = new
	 * FastList<L2PcInstance>();
	 * if (absorbType == L2NpcTemplate.AbsorbCrystalType.FULL_PARTY &&
	 * killer.isInParty()) players = killer.getParty().getPartyMembers(); else
	 * if (absorbType == L2NpcTemplate.AbsorbCrystalType.PARTY_ONE_RANDOM &&
	 * killer.isInParty()) { // This is a naive method for selecting a random
	 * member. It gets any random party member and // then checks if the member
	 * has a valid crystal. It does not select the random party member // among
	 * those who have crystals, only. However, this might actually be correct
	 * (same as retail).
	 * players.add(killer.getParty().getPartyMembers().get(Rnd.
	 * get(killer.getParty().getMemberCount()))); } else players.add(killer);
	 * for (L2PcInstance player : players) { if (player == null) continue;
	 * QuestState st = player.getQuestState("350_EnhanceYourWeapon");
	 * if (st == null) continue;
	 * if (st.getState() != State.STARTED) continue;
	 * crystalQTY = 0;
	 * L2ItemInstance[] inv = player.getInventory().getItems(); for
	 * (L2ItemInstance item : inv) { int itemId = item.getItemId(); for (int id
	 * : SoulCrystal.SoulCrystalTable) { if (id == itemId) { // Keep count but
	 * make sure the player has no more than 1 crystal if (++crystalQTY > 1) {
	 * isSuccess = false;break; }
	 * // Validate if the crystal has already leveled if (id !=
	 * SoulCrystal.RED_NEW_CRYSTAL && id != SoulCrystal.GRN_NEW_CYRSTAL && id !=
	 * SoulCrystal.BLU_NEW_CRYSTAL) { try { if
	 * (item.getItem().getName().contains("Grade")) { // Split the name of the
	 * crystal into 'name' & 'level' crystalNFO =
	 * item.getItem().getName().trim().replace(" Grade ", "-").split("-"); //
	 * Set Level to 13 crystalLVL = 13; } else { // Split the name of the
	 * crystal into 'name' & 'level' crystalNFO =
	 * item.getItem().getName().trim().replace(" Stage ", "").split("-"); // Get
	 * Level crystalLVL = Integer.parseInt(crystalNFO[1].trim()); } // Allocate
	 * current and levelup ids' for higher level crystals if (crystalLVL > 9) {
	 * for(int i = 0; i < SoulCrystal.HighSoulConvert.length; i++) // Get the
	 * next stage above 10 using array. if (id ==
	 * SoulCrystal.HighSoulConvert[i][0]) { crystalNEW =
	 * SoulCrystal.HighSoulConvert[i][1]; break; } } else crystalNEW = id+1; }
	 * catch (NumberFormatException nfe) { _log.log(Level.WARNING,
	 * "An attempt to identify a soul crystal failed, verify the names have not changed in etcitem table."
	 * , nfe); player.sendMessage(
	 * "There has been an error handling your soul crystal. Please notify your server admin."
	 * ); isSuccess = false; break; } catch (Exception e) { e.printStackTrace();
	 * isSuccess = false; break; } } else crystalNEW = id+1;
	 * // Done crystalOLD = id; break; } } if (!isSuccess) break; }
	 * // If the crystal level is way too high for this mob, say that we can't
	 * increase it if ((crystalLVL < minAbsorbLevel) || (crystalLVL >=
	 * maxAbsorbLevel)) doLevelup = false;
	 * // The player doesn't have any crystals with him get to the next player.
	 * if (crystalQTY < 1 || crystalQTY > 1 || !isSuccess || !doLevelup) { //
	 * Too many crystals in inventory. if (crystalQTY > 1) player.sendPacket(new
	 * SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_FAILED_RESONATION));
	 * // The soul crystal stage of the player is way too high else if
	 * (!doLevelup && crystalQTY > 0) player.sendPacket(new
	 * SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_REFUSED));
	 * crystalQTY = 0; continue; }
	 * TODO: Confirm boss chance for crystal level up and for crystal breaking.
	 * It is known that bosses with FULL_PARTY crystal level ups have 100%
	 * success rate, but this is not the case for the other bosses (one-random
	 * or last-hit). While not confirmed, it is most reasonable that crystals
	 * leveled up at bosses will never break. Also, the chance to level up is
	 * guessed as around 70% if not higher.
	 * int chanceLevelUp = isBossMob? 70:SoulCrystal.LEVEL_CHANCE;
	 * // If succeeds or it is a full party absorb, level up the crystal. if
	 * (((absorbType == L2NpcTemplate.AbsorbCrystalType.FULL_PARTY) &&
	 * doLevelup) || (dice <= chanceLevelUp)) exchangeCrystal(player,
	 * crystalOLD, crystalNEW, false);
	 * else player.sendPacket(new
	 * SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_FAILED)); } }
	 */
	/*
	 * private void exchangeCrystal(L2PcInstance player, int takeid, int giveid,
	 * boolean broke) { L2ItemInstance Item =
	 * player.getInventory().destroyItemByItemId("SoulCrystal", takeid, 1,
	 * player, this);
	 * if (Item != null) { // Prepare inventory update packet InventoryUpdate
	 * playerIU = new InventoryUpdate(); playerIU.addRemovedItem(Item);
	 * // Add new crystal to the killer's inventory Item =
	 * player.getInventory().addItem("SoulCrystal", giveid, 1, player, this);
	 * playerIU.addItem(Item);
	 * // Send a sound event and text message to the player if (broke)
	 * player.sendPacket(new SystemMessage(SystemMessageId.SOUL_CRYSTAL_BROKE));
	 * else player.sendPacket(new
	 * SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_SUCCEEDED));
	 * // Send system message SystemMessage sms = new
	 * SystemMessage(SystemMessageId.EARNED_ITEM); sms.addItemName(giveid);
	 * player.sendPacket(sms);
	 * // Send inventory update packet player.sendPacket(playerIU); } }
	 */
	private void resetAbsorbList()
	{
		_absorbed = false;
		_absorbersList.clear();
	}
	
	/**
	 * Calculate the Experience and SP to distribute to attacker (L2PcInstance,
	 * L2SummonInstance or L2Party) of the L2Attackable.
	 * 
	 * @param diff
	 *            The difference of level between attacker (L2PcInstance,
	 *            L2SummonInstance or L2Party) and the L2Attackable
	 * @param damage
	 *            The damages given by the attacker (L2PcInstance,
	 *            L2SummonInstance or L2Party)
	 */
	private long[] calculateExpAndSp(int diff)
	{
		long xp;
		long sp;
		if (diff < -5)
			diff = -5; // makes possible to use ALT_GAME_EXPONENT configuration
		xp = getExpReward();
		if (Config.ALT_GAME_EXPONENT_XP != 0)
			xp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_XP);
		sp = getSpReward();
		if (Config.ALT_GAME_EXPONENT_SP != 0)
			sp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_SP);
		if (Config.ALT_GAME_EXPONENT_XP == 0 && Config.ALT_GAME_EXPONENT_SP == 0)
		{
			if (diff > 5) // formula revised May 07
			{
				double pow = Math.pow((double) 5 / 6, diff - 5);
				xp = (long) (xp * pow);
				sp = (long) (sp * pow);
			}
			if (xp <= 0)
			{
				xp = 0;
				sp = 0;
			}
			else if (sp <= 0)
				sp = 0;
		}
		long[] tmp =
		{
			xp, sp
		};
		return tmp;
	}
	
	public long calculateOverhitExp(long normalExp)
	{
		// Get the percentage based on the total of extra (over-hit) damage done
		// relative to the total (maximum) ammount of HP on the L2Attackable
		double overhitPercentage = ((getOverhitDamage() * 100) / getMaxHp());
		// Over-hit damage percentages are limited to 25% max
		if (overhitPercentage > 25)
			overhitPercentage = 25;
		// Get the overhit exp bonus according to the above over-hit damage
		// percentage
		// (1/1 basis - 13% of over-hit damage, 13% of extra exp is given, and
		// so on...)
		double overhitExp = ((overhitPercentage / 100) * normalExp);
		// Return the rounded ammount of exp points to be added to the player's
		// normal exp reward
		long bonusOverhit = Math.round(overhitExp);
		return bonusOverhit;
	}
	
	/**
	 * Return True.
	 */
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		setChampion(false);
		if (Config.L2JMOD_CHAMPION_ENABLE)
		{
			// Set champion on next spawn
			if (this instanceof L2MonsterInstance && !getTemplate().isQuestMonster && !isRaid() && !isRaidMinion() && Config.L2JMOD_CHAMPION_FREQUENCY > 0 && getLevel() >= Config.L2JMOD_CHAMP_MIN_LVL && getLevel() <= Config.L2JMOD_CHAMP_MAX_LVL && getTemplate().baseHpMax < 1000000 && !isAPC())
			{
				int random = Rnd.get(200);
				if (random < Config.L2JMOD_CHAMPION_FREQUENCY)
					setChampion(true);
			}
		}
		super.onSpawn();
		// Clear mob spoil, seed
		setSpoil(false);
		// Clear all aggro char from list
		clearAggroList();
		// Clear Harvester Rewrard List
		_harvestItems = null;
		// Clear mod Seeded stat
		setSeeded(false);
		_sweepItems = null;
		resetAbsorbList();
		if (isAPC())
			setRunning();
		else
			setWalking();
		// check the region where this mob is, do not activate the AI if region
		// is inactive.
		if (!isInActiveRegion() && hasAI())
		{
			if (this instanceof L2SiegeGuardInstance)
				((L2SiegeGuardAI) getAI()).stopAITask();
			else if (this instanceof L2FortSiegeGuardInstance)
				((L2FortSiegeGuardAI) getAI()).stopAITask();
			else
				((L2AttackableAI) getAI()).stopAITask();
		}
	}
	
	/**
	 * Sets state of the mob to seeded. Paramets needed to be set before.
	 */
	public void setSeeded()
	{
		if (_seedType != 0 && _seeder != null)
			setSeeded(_seedType, _seeder.getLevel());
	}
	
	/**
	 * Sets the seed parametrs, but not the seed state
	 * 
	 * @param id
	 *            - id of the seed
	 * @param seeder
	 *            - player who is sowind the seed
	 */
	public void setSeeded(int id, L2PcInstance seeder)
	{
		if (!_seeded)
		{
			_seedType = id;
			_seeder = seeder;
		}
	}
	
	public void setSeeded(int id, int seederLvl)
	{
		_seeded = true;
		_seedType = id;
		int count = 1;
		/*
		 * Map<Integer, L2Skill> skills = getTemplate().getSkills();
		 * if (skills != null) { for (int skillId : skills.keySet()) { switch
		 * (skillId) { case 4303: //Strong type x2 count *= 2; break; case 4304:
		 * //Strong type x3 count *= 3; break; case 4305: //Strong type x4 count
		 * *= 4; break; case 4306: //Strong type x5 count *= 5; break; case
		 * 4307: //Strong type x6 count *= 6; break; case 4308: //Strong type x7
		 * count *= 7; break; case 4309: //Strong type x8 count *= 8; break;
		 * case 4310: //Strong type x9 count *= 9; break; } } }
		 */
		int diff = (getLevel() - (L2Manor.getInstance().getSeedLevel(_seedType) - 5));
		// hi-lvl mobs bonus
		if (diff > 0)
			count += diff;
		FastList<RewardItem> harvested = new FastList<RewardItem>();
		harvested.add(new RewardItem(L2Manor.getInstance().getCropType(_seedType), count * Config.RATE_DROP_MANOR));
		_harvestItems = harvested.toArray(new RewardItem[harvested.size()]);
	}
	
	public void setSeeded(boolean seeded)
	{
		_seeded = seeded;
	}
	
	public L2PcInstance getSeeder()
	{
		return _seeder;
	}
	
	public int getSeedType()
	{
		return _seedType;
	}
	
	public boolean isSeeded()
	{
		return _seeded;
	}
	
	private int getAbsorbLevel()
	{
		return getTemplate().absorbLevel;
	}
	
	/**
	 * Check if the server allows Random Animation.
	 */
	// This is located here because L2Monster and L2FriendlyMob both extend this
	// class. The other non-pc instances extend either L2NpcInstance or
	// L2MonsterInstance.
	@Override
	public boolean hasRandomAnimation()
	{
		return ((Config.MAX_MONSTER_ANIMATION > 0) && !(this instanceof L2GrandBossInstance));
	}
	
	@Override
	public boolean isMob()
	{
		return true; // This means we use MAX_MONSTER_ANIMATION instead of
						// MAX_NPC_ANIMATION
	}
	
	protected void setCommandChannelTimer(CommandChannelTimer commandChannelTimer)
	{
		_commandChannelTimer = commandChannelTimer;
	}
	
	public CommandChannelTimer getCommandChannelTimer()
	{
		return _commandChannelTimer;
	}
	
	public L2CommandChannel getFirstCommandChannelAttacked()
	{
		return _firstCommandChannelAttacked;
	}
	
	public void setFirstCommandChannelAttacked(L2CommandChannel firstCommandChannelAttacked)
	{
		_firstCommandChannelAttacked = firstCommandChannelAttacked;
	}
	
	private class CommandChannelTimer implements Runnable
	{
		private final L2Attackable		_monster;
		private final L2CommandChannel	_channel;
		
		public CommandChannelTimer(L2Attackable monster, L2CommandChannel channel)
		{
			_monster = monster;
			_channel = channel;
		}
		
		/**
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			_monster.setCommandChannelTimer(null);
			_monster.setFirstCommandChannelAttacked(null);
			for (L2Character player : _monster.getAggroList().keySet())
			{
				if (player.isInParty() && player.getParty().isInCommandChannel())
				{
					if (player.getParty().getCommandChannel().equals(_channel))
					{
						// if a player which is in first attacked
						// CommandChannel, restart the timer ;)
						_monster.setCommandChannelTimer(this);
						_monster.setFirstCommandChannelAttacked(_channel);
						ThreadPoolManager.getInstance().scheduleGeneral(this, 300000); // 5
																						// min
						break;
					}
				}
			}
		}
	}
	
	public void returnHome()
	{
		clearAggroList();
		if (hasAI() && getSpawn() != null)
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(getSpawn().getCurX(), getSpawn().getCurY(), getSpawn().getCurZ(), 0));
	}
	
	/*
	 * Return vitality points decrease (if positive) or increase (if negative)
	 * based on damage. Maximum for damage = maxHp.
	 */
	public float getVitalityPoints(int damage)
	{
		// sanity check
		if (damage <= 0)
			return 0;
		final float divider = getTemplate().baseVitalityDivider;
		if (divider == 0)
			return 0;
		// negative value - vitality will be consumed
		return -Math.min(damage, getMaxHp()) / divider;
	}
	
	/*
	 * True if vitality rate for exp and sp should be applied
	 */
	public boolean useVitalityRate()
	{
		if (isChampion() && !Config.L2JMOD_CHAMPION_ENABLE_VITALITY)
			return false;
		return true;
	}
	
	public boolean isLevelOneRaidboss()
	{
		return (this instanceof net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance && getLevel() == 85 && getNpcId() != 25325);
	}
	
	public boolean isLevelTwoRaidboss()
	{
		return (this instanceof net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance && getLevel() == 86 && getNpcId() != 25325);
	}
	
	public boolean isLevelThreeRaidboss()
	{
		return (this instanceof net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance && getLevel() == 87 && getNpcId() != 25325);
	}
	
	public boolean isLevelFourRaidboss()
	{
		return (this instanceof net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance && getLevel() == 88 && getNpcId() != 25325);
	}
	
	public boolean isLevelFiveRaidboss()
	{
		return (this instanceof net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance && getLevel() == 89 && getNpcId() != 25325);
	}
	
	public boolean isLevelSixRaidboss()
	{
		return (this instanceof net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance && getLevel() == 90 && getNpcId() != 25325);
	}
	
	public boolean isLevelSevenRaidboss()
	{
		return (this instanceof net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance && getLevel() == 91 && getNpcId() != 25325);
	}
	
	public boolean isLevelEightRaidboss()
	{
		return (this instanceof net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance && getLevel() == 92 && getNpcId() != 25325);
	}
	
	public boolean isLevelNineRaidboss()
	{
		return (this instanceof net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance && getLevel() == 93 && getNpcId() != 25325);
	}
	
	public boolean isLevelTenRaidboss()
	{
		return (this instanceof net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance && getLevel() == 94 && getNpcId() != 25325);
	}
	
	public boolean isLevelElevenRaidboss()
	{
		return (this instanceof net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance && getLevel() == 95 && getNpcId() != 25325);
	}
	
	public boolean isSoloMob()
	{
		return getTemplate()._soloMob == 123;
	}
	
	public int getRndWalkRange()
	{
		return getTemplate()._randomWalkRange;
	}
	
	public void setIsEventMob(boolean b)
	{
		_eventMob = b;
	}
	
	public boolean isEventMob()
	{
		return _eventMob;
	}
}
