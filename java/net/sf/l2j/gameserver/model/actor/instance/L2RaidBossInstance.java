package net.sf.l2j.gameserver.model.actor.instance;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.instancemanager.RaidBossPointsManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate.AIType;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.util.Rnd;

public class L2RaidBossInstance extends L2MonsterInstance
{
	private static final int					RAIDBOSS_MAINTENANCE_INTERVAL	= 15000;								// 15 sec
	public static ArrayList<L2RaidBossInstance>	_alive85Raids					= new ArrayList<L2RaidBossInstance>();
	public static ArrayList<L2RaidBossInstance>	_alive86Raids					= new ArrayList<L2RaidBossInstance>();
	public static ArrayList<L2RaidBossInstance>	_alive87Raids					= new ArrayList<L2RaidBossInstance>();
	public static ArrayList<L2RaidBossInstance>	_alive88Raids					= new ArrayList<L2RaidBossInstance>();
	public static ArrayList<L2RaidBossInstance>	_alive89Raids					= new ArrayList<L2RaidBossInstance>();
	public static ArrayList<L2RaidBossInstance>	_alive90Raids					= new ArrayList<L2RaidBossInstance>();
	public static ArrayList<L2RaidBossInstance>	_alive91Raids					= new ArrayList<L2RaidBossInstance>();
	public static ArrayList<L2RaidBossInstance>	_alive92Raids					= new ArrayList<L2RaidBossInstance>();
	public static ArrayList<L2RaidBossInstance>	_alive93Raids					= new ArrayList<L2RaidBossInstance>();
	public static ArrayList<L2RaidBossInstance>	_alive94Raids					= new ArrayList<L2RaidBossInstance>();
	public static ArrayList<L2RaidBossInstance>	_alive95Raids					= new ArrayList<L2RaidBossInstance>();
	public static ArrayList<L2RaidBossInstance>	_aliveRaids						= new ArrayList<L2RaidBossInstance>();
	public static int							_rbCounter						= 0;
	public static int							_rbCounter85					= 0;
	public static int							_rbCounter86					= 0;
	public static int							_rbCounter87					= 0;
	public static int							_rbCounter88					= 0;
	public static int							_rbCounter89					= 0;
	public static int							_rbCounter90					= 0;
	public static int							_rbCounter91					= 0;
	public static int							_rbCounter92					= 0;
	public static int							_rbCounter93					= 0;
	public static int							_rbCounter94					= 0;
	public static int							_rbCounter95					= 0;
	public static int							_alive_rbCounter				= 0;
	private boolean								_dead							= true;
	private ScheduledFuture<?>					_lifeCheck35;
	private ScheduledFuture<?>					_lifeCheck75;
	private ScheduledFuture<?>					_lifeCheck99;
	/*
	 * private ScheduledFuture<?> _announceRBSpawnTask = null;
	 * private class announceRBSpawnTask implements Runnable
	 * {
	 * L2RaidBossInstance _boss;
	 * public announceRBSpawnTask(L2RaidBossInstance boss)
	 * {
	 * _boss = boss;
	 * }
	 * public void run()
	 * {
	 * if (_boss != null && !_boss.isDead() && !_boss.isInCombat() && _boss.getCurrentHp() > _boss.getMaxHp() * 0.92 && _boss.isLevelOneRaidboss())
	 * {
	 * try
	 * {
	 * SystemMessage sm = new SystemMessage(SystemMessageId.A_RAIDBOSS_SPAWNED_IN_S1_REGION);
	 * sm.addZoneName(_boss.getX(), _boss.getY(), _boss.getZ()); // Region Name
	 * Announcements.getInstance().announceToAll(sm);
	 * }
	 * catch (Throwable t)
	 * {
	 * }
	 * }
	 * }
	 * }
	 * private ScheduledFuture<?> _announceRBSpawnTaskClan = null;
	 * private class announceRBSpawnTaskClan implements Runnable
	 * {
	 * L2RaidBossInstance _boss;
	 * public announceRBSpawnTaskClan(L2RaidBossInstance boss)
	 * {
	 * _boss = boss;
	 * }
	 * public void run()
	 * {
	 * if (_boss != null && !_boss.isDead() && !_boss.isInCombat()
	 * && _boss.getCurrentHp() > _boss.getMaxHp() * 0.92)
	 * {
	 * try
	 * {
	 * final Castle castle = CastleManager.getInstance().getCastleById(MapRegionTable.getInstance().getAreaCastle(_boss));
	 * if (castle == null)
	 * return;
	 * final L2Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
	 * if (clan == null)
	 * return;
	 * CreatureSay say = new CreatureSay(0, Say2.PARTYROOM_ALL, castle.getName()
	 * + " Castle", _boss.getName()
	 * + " has appeared in a region controlled by your castle.");
	 * clan.broadcastToOnlineMembers(say);
	 * }
	 * catch (Throwable t)
	 * {
	 * }
	 * }
	 * }
	 * }
	 */
	private RaidBossSpawnManager.StatusEnum		_raidStatus;
	
	/**
	 * Constructor of L2RaidBossInstance (use L2Character and L2NpcInstance constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to set the _template of the L2RaidBossInstance (copy
	 * skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li>
	 * <li>Set the
	 * name of the L2RaidBossInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched
	 * after the calculated delay if the server allow it</li><BR>
	 * <BR>
	 * 
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param L2NpcTemplate
	 *            Template to apply to the NPC
	 */
	public L2RaidBossInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	private volatile boolean	announce35	= true;
	private volatile boolean	announce75	= true;
	private volatile boolean	announce99	= true;
	
	@Override
	public synchronized void onSpawn()
	{
		if (!_dead)
			return;
		_dead = false;
		if (getInstanceId() < 1000)
		{
			_alive_rbCounter++;
		}
		setIsRaid(true);
		setIsNoRndWalk(true);
		super.onSpawn();
		if (getInstanceId() < 1000)
		{
			if (_lifeCheck35 == null)
			{
				_lifeCheck35 = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(() ->
				{
					if (announce35 && getHpPercent() < 35)
					{
						taunt();
						announce35 = false;
						_lifeCheck35.cancel(false);
						showDamageMeter();
						if(!isLevelOneRaidboss() && Rnd.get(100) < 25)
						{
							flagEveryone();
						}
					}
				}, 1000, 1000);
			}
			if (_lifeCheck75 == null)
			{
				_lifeCheck75 = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(() ->
				{
					if (announce75 && getHpPercent() < 75)
					{
						taunt();
						announce75 = false;
						_lifeCheck75.cancel(false);
						showDamageMeter();
						if(!isLevelOneRaidboss() && Rnd.get(100) < 70)
						{
							flagEveryone();
						}
					}
				}, 1000, 1000);
			}
			if (getLevel() >= 95 && _lifeCheck99 == null)
			{
				_lifeCheck99 = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(() ->
				{
					if (announce99 && getHpPercent() < 100)
					{
						taunt();
						announce99 = false;
						_lifeCheck99.cancel(false);
						flagEveryone();
					}
				}, 1000, 1000);
			}
		}
		if (getInstanceId() < 1000)
		{
			if (isLevelOneRaidboss())
			{
				_alive85Raids.add(this);
				_aliveRaids.add(this);
				_rbCounter85++;
				_rbCounter++;
			}
			else if (isLevelTwoRaidboss())
			{
				_alive86Raids.add(this);
				_aliveRaids.add(this);
				_rbCounter86++;
				_rbCounter++;
			}
			else if (isLevelThreeRaidboss())
			{
				_alive87Raids.add(this);
				_aliveRaids.add(this);
				_rbCounter87++;
				_rbCounter++;
			}
			else if (isLevelFourRaidboss())
			{
				_alive88Raids.add(this);
				_aliveRaids.add(this);
				_rbCounter88++;
				_rbCounter++;
			}
			else if (isLevelFiveRaidboss())
			{
				_alive89Raids.add(this);
				_aliveRaids.add(this);
				_rbCounter89++;
				_rbCounter++;
			}
			else if (isLevelSixRaidboss())
			{
				_alive90Raids.add(this);
				_aliveRaids.add(this);
				_rbCounter90++;
				_rbCounter++;
			}
			else if (isLevelSevenRaidboss())
			{
				_alive91Raids.add(this);
				_aliveRaids.add(this);
				_rbCounter91++;
				_rbCounter++;
			}
			else if (isLevelEightRaidboss())
			{
				_alive92Raids.add(this);
				_aliveRaids.add(this);
				_rbCounter92++;
				_rbCounter++;
			}
			else if (isLevelNineRaidboss())
			{
				_alive93Raids.add(this);
				_aliveRaids.add(this);
				_rbCounter93++;
				_rbCounter++;
			}
			else if (isLevelTenRaidboss())
			{
				_alive94Raids.add(this);
				_aliveRaids.add(this);
				_rbCounter94++;
				_rbCounter++;
			}
			else if (isLevelElevenRaidboss())
			{
				_alive95Raids.add(this);
				_aliveRaids.add(this);
				_rbCounter95++;
				_rbCounter++;
			}
		}
	}
	
	public static int getRbCount85()
	{
		return _rbCounter85;
	}
	
	public static int getRbCount86()
	{
		return _rbCounter86;
	}
	
	public static int getRbCount87()
	{
		return _rbCounter87;
	}
	
	public static int getRbCount88()
	{
		return _rbCounter88;
	}
	
	public static int getRbCount89()
	{
		return _rbCounter89;
	}
	
	public static int getRbCount90()
	{
		return _rbCounter90;
	}
	
	public static int getRbCount91()
	{
		return _rbCounter91;
	}
	
	public static int getRbCount92()
	{
		return _rbCounter92;
	}
	
	public static int getRbCount93()
	{
		return _rbCounter93;
	}
	
	public static int getRbCount94()
	{
		return _rbCounter94;
	}
	
	public static int getRbCount95()
	{
		return _rbCounter95;
	}
	
	public static int getTotalRbCount()
	{
		return _rbCounter;
	}
	
	public static int getAliveRbCount()
	{
		return _alive_rbCounter;
	}
	
	@Override
	protected int getMaintenanceInterval()
	{
		return RAIDBOSS_MAINTENANCE_INTERVAL;
	}
	
	public static void cleanupLists()
	{
		_rbCounter85 = 0;
		_rbCounter86 = 0;
		_rbCounter87 = 0;
		_rbCounter88 = 0;
		_rbCounter89 = 0;
		_rbCounter90 = 0;
		_rbCounter91 = 0;
		_rbCounter92 = 0;
		_rbCounter93 = 0;
		_rbCounter94 = 0;
		_rbCounter95 = 0;
		_rbCounter = 0;
		_alive85Raids.clear();
		_alive86Raids.clear();
		_alive87Raids.clear();
		_alive88Raids.clear();
		_alive89Raids.clear();
		_alive90Raids.clear();
		_alive91Raids.clear();
		_alive92Raids.clear();
		_alive93Raids.clear();
		_alive94Raids.clear();
		_alive95Raids.clear();
		_aliveRaids.clear();
	}
	
	@Override
	public synchronized boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		if (_dead)
			return true;
		_dead = true;
		L2PcInstance player = null;
		if (killer instanceof L2PcInstance)
			player = (L2PcInstance) killer;
		else if (killer instanceof L2Summon)
			player = ((L2Summon) killer).getOwner();
		if (getInstanceId() < 1000)
		{
			if (player != null)
			{
				broadcastPacket(new SystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL));
				if (player.getParty() != null)
				{
					for (L2PcInstance member : player.getParty().getPartyMembers())
					{
						RaidBossPointsManager.addPoints(member, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
						member.setRaidKills(member.getRaidKills() + 1);
					}
				}
				else
					RaidBossPointsManager.addPoints(player, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
				player.setRaidKills(player.getRaidKills() + 1);
			}
		}
		if (getInstanceId() < 1000)
		{
			if (getLevel() >= 85)
			{
				String clan = "";
				if (player.getClan() != null)
					clan = " of clan " + player.getClan().getName();
				_alive_rbCounter--;
				Announcements.getInstance().announceToAll(getName() + " has been defeated by " + player.getDisplayName() + clan);
			}
			if (!isTempSpawn())
			{
				RaidBossSpawnManager.getInstance().updateStatus(this, true);
				getSpawn().setDeathDate(System.currentTimeMillis());
			}
		}
		Collection<L2PcInstance> ppl = L2World.getInstance().getAllPlayers().values();
		for (L2PcInstance p : ppl)
		{
			if (p.isInsideRadius(this, 1000, false, false))
			{
				showDamagePage(p);
			}
		}
		/*
		 * if (isLevelOneRaidboss())
		 * {
		 * _alive85Raids.remove(this);
		 * _rbCounter--;
		 * if (_announceRBSpawnTask != null)
		 * _announceRBSpawnTask.cancel(true);
		 * if (_announceRBSpawnTaskClan != null)
		 * _announceRBSpawnTaskClan.cancel(true);
		 * if (!player.isGM())
		 * {
		 * FastList<Integer> clans = new FastList<Integer>();
		 * if (player.getClan() != null)
		 * {
		 * player.getClan().incRBkills();
		 * clans.add(player.getClanId());
		 * }
		 * if (player.getParty() != null)
		 * {
		 * for (L2PcInstance noob : player.getParty().getPartyMembers())
		 * {
		 * if (noob != null && !noob.isGM() && noob.getClan() != null && !clans.contains(noob.getClanId()) && Util.checkIfInRange(1800, player, noob, true))
		 * {
		 * noob.getClan().incRBkills();
		 * clans.add(noob.getClanId());
		 * }
		 * }
		 * }
		 * }
		 * }
		 * else if (isLevelTwoRaidboss())
		 * {
		 * _alive91Raids.remove(this);
		 * if (_announceRBSpawnTaskClan != null)
		 * _announceRBSpawnTaskClan.cancel(true);
		 * if (!player.isGM())
		 * {
		 * FastList<Integer> clans = new FastList<Integer>();
		 * if (player.getClan() != null)
		 * {
		 * player.getClan().incRBkills();
		 * clans.add(player.getClanId());
		 * }
		 * if (player.getParty() != null)
		 * {
		 * for (L2PcInstance noob : player.getParty().getPartyMembers())
		 * {
		 * if (noob != null && !noob.isGM() && noob.getClan() != null && !clans.contains(noob.getClanId()) && Util.checkIfInRange(1800, player, noob, true))
		 * {
		 * noob.getClan().incRBkills();
		 * clans.add(noob.getClanId());
		 * }
		 * }
		 * }
		 * }
		 * }
		 */
		return true;
	}
	
	protected void showDamageMeter()
	{
		Collection<L2PcInstance> ppl = L2World.getInstance().getAllPlayers().values();
		for (L2PcInstance p : ppl)
		{
			if (p.isInsideRadius(this, 1000, false, false))
			{
				showDamagePage(p);
			}
		}
	}
	
	protected void flagEveryone()
	{
		Collection<L2PcInstance> ppl = L2World.getInstance().getAllPlayers().values();
		for (L2PcInstance p : ppl)
		{
			if (p.isInsideRadius(this, 2000, false, false))
			{

				p.sendPacket(new ExShowScreenMessage(1, -1, 2, 0, 0, 0, 0, true, 10000, 0, "A dark force has updated your flag status.\r\n\r\n   Killing this raid won't be easy!"));
				p.updatePvPStatus();
			}
		}
	}
	
	/**
	 * Spawn all minions at a regular interval Also if boss is too far from home location at the
	 * time of this check, teleport it home
	 */
	@Override
	protected void manageMinions()
	{
		if (_minionList != null)
			_minionList.spawnMinions();
		_minionMaintainTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
		{
			public void run()
			{
				// teleport raid boss home if it's too far from home location
				final L2Spawn bossSpawn = getSpawn();
				int radius = 3500;
				if (L2RaidBossInstance.this.isAPC())
					radius = 5000;
				if (L2RaidBossInstance.this.getTemplate().AI == AIType.MAGE || L2RaidBossInstance.this.getTemplate().AI == AIType.ARCHER)
					radius += 5000;
				if (L2RaidBossInstance.this.getInstanceId() > 0)
					radius *= 4;
				if (!isInsideRadius(bossSpawn.getCurX(), bossSpawn.getCurY(), bossSpawn.getCurZ(), radius, true, false))
				{
					teleToLocation(bossSpawn.getCurX(), bossSpawn.getCurY(), bossSpawn.getCurZ(), false);
					healFull(); // prevents minor exploiting with it
					_minionList.recallMinions();
					_minionList.maintainMinions();
					return;
				}
				_minionList.maintainMinions();
				for (L2PcInstance player : getKnownList().getKnownPlayers().values())
				{
					if (player == null || player.isGM())
						continue;
					for (L2Effect e : player.getAllEffects())
					{
						if (e == null)
							continue;
						int id = e.getSkill().getId();
						if (id == 176 || id == 292 || id == 538 || id == 929 || id == 930 || id == 931) // frenzy, bison, final form, summoner transforms
							e.exit();
					}
				}
				int val = 5;
				if (getLevel() > 85)
					val = 3;
				if (getNpcId() != 25325)
				{
					if (getTarget() instanceof L2Playable && getTarget().getActingPlayer() != null)
					{
						if (getTarget().getActingPlayer().getClan() != null)
						{
							if (getTarget().getActingPlayer().getClan().getLevel() < 6)
								val -= 1;
						}
						else
							val -= 1;
					}
					// if ((getAI().getAttackTarget() != null || getAI().getFollowTarget() != null) && L2RaidBossInstance.this.getInstanceId() == 0)
					// taunt();
				}
			}
		}, 60000, getMaintenanceInterval() + Rnd.get(5000));
	}
	
	protected void taunt()
	{
		L2PcInstance target = getAI().getAttackTarget().getActingPlayer();
		if (target == null)
			target = getAI().getFollowTarget().getActingPlayer();
		if (target == null)
			target = getTarget().getActingPlayer();
		if (target == null)
			return;
		if (target.isGM())
			return;
		final String name = target.getDisplayName();
		final String[] msgs =
		{
			name + "! You think you can defeat me?",
			"Enjoy the hells " + name + "!",
			name + "! I will show you my power, and then you can tremble before my might as I crush your puny bones to bits!",
			"You are really stupid to have challenged me... " + name + "!" + " Get ready!",
			"I really have no items, I swear!",
			name + "! I will destroy you!",
			name + "! I will make you realize what true power is!",
			name + ", You are on the way to destruction.",
			name + "! You have no chance to survive make your time.",
			"So we meet again, " + name + "! This time you die!",
			"Vegeta! What does the scouter say about " + name + "'s powerlevel?",
			"How brave of you to walk straight into death, " + name,
			name + "! I've braved the fires of hell, stood against the attrition of time, and single-handedly unified this region, you think a little scrub like you can defeat me?",
			name + "! How inconsiderate of you, won't your friends miss you after you die?",
			"Start the clock! " + name + ", you will die in 5 minutes!",
			"It's time to sharpen my blade with human flesh!",
			name + "! You will not win! Get out of here while you can!",
			name + "! Even with all this zerg here I will still kill you all by myself!",
			"This one here named " + name + " will be no more soon...",
			name + "! You will die here like the rest before you!",
			name + "! I will show you the punishment that follows folly!",
			"Why do you attempt the futile, " + name + "?" + " Throwing your life away carelessly like this...",
			"Today is a sad day for you! " + name + "!",
			"Today is a good day to die!... for " + name + "!",
			"Haha! The ant is trying to fight the lion!",
			name + " is a fool! Fools die!",
			"Death looms near for " + name,
			"I **** YOUR MOTHER " + name.toUpperCase() + "!",
			name + "! You should be proud that you will die by my hands!",
			name + "! What is the meaning of death? well, I'll show it to you!",
			name + ", I think you need more healers!",
			name + ", You sure the loot is set on finders keepers?",
			name + "! I will kill you with my left hand and your friends with my right! Muhahaha!",
			"Brethren prior to WENCHES!",
			"I hath ninety-nine difficulties, nary a wench among them!",
			"HALT! The hour of the hammer has begun!",
			"Compelling tale, male sibling.",
			"Bare thy bosoms or make hasty egress!",
			"Relocate yourself, hound of the female gender. Be certain not to block the direction of which I am travelling.",
			"Fornicate this excrement!",
			"Cease all activities thou art engaged in - The hour of the hammer is upon us.",
			"Female canine I beg of you!",
			"Allow the carcasses to make contact with the ground!",
			"Advance towards me in an aggressive manner, male sibling!",
			"I so happen to be attracted to large posteriors, and I am inclined to be completely factual on the matter.",
			"Make thy brethren a priority before enjoying the company of loose women.",
			"Fecal matter tends to occur...",
			"Insert thy reproductive organ into thine own self!"
		};
		if (target.getName().equalsIgnoreCase("jux"))
		{
			if (Rnd.get(100) < 7)
				Broadcast.shoutChat("I'll take care of you Jux", getName(), this);
			else
				Broadcast.shoutChat(msgs[Rnd.get(msgs.length)], getName(), this);
		}
		else
		{
			Broadcast.shoutChat(msgs[Rnd.get(msgs.length)], getName(), this);
		}
	}
	
	public void setRaidStatus(RaidBossSpawnManager.StatusEnum status)
	{
		_raidStatus = status;
	}
	
	public RaidBossSpawnManager.StatusEnum getRaidStatus()
	{
		return _raidStatus;
	}
	
	/**
	 * Reduce the current HP of the L2Attackable, update its _aggroList and launch the doDie Task if
	 * necessary.<BR>
	 * <BR>
	 */
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	public void healFull()
	{
		super.setCurrentHp(super.getMaxHp());
		super.setCurrentMp(super.getMaxMp());
	}
	
	public static int showNumberOfaliveRbs()
	{
		return _alive_rbCounter;
	}
	
	public static int showNumberOfRbs()
	{
		int count = 0;
		for (L2RaidBossInstance boss : _alive85Raids)
		{
			if (boss == null)
			{
				continue;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive86Raids)
		{
			if (boss == null)
			{
				continue;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive87Raids)
		{
			if (boss == null)
			{
				continue;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive88Raids)
		{
			if (boss == null)
			{
				continue;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive89Raids)
		{
			if (boss == null)
			{
				continue;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive90Raids)
		{
			if (boss == null)
			{
				continue;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive91Raids)
		{
			if (boss == null)
			{
				continue;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive92Raids)
		{
			if (boss == null)
			{
				continue;
			}
			count++;
		}
		return count;
	}
	
	public static void showRaidsThatAreUp(final L2PcInstance activeChar)
	{
		int count = 0;
		for (L2RaidBossInstance boss : _alive85Raids)
		{
			if (boss == null)
				continue;
			activeChar.sendMessage(count + ": " + boss.getName() + " (" + boss.getLevel() + ")");
			count++;
		}
		for (L2RaidBossInstance boss : _alive86Raids)
		{
			if (boss == null)
				continue;
			activeChar.sendMessage(count + ": " + boss.getName() + " (" + boss.getLevel() + ")");
			count++;
		}
		for (L2RaidBossInstance boss : _alive87Raids)
		{
			if (boss == null)
				continue;
			activeChar.sendMessage(count + ": " + boss.getName() + " (" + boss.getLevel() + ")");
			count++;
		}
		for (L2RaidBossInstance boss : _alive88Raids)
		{
			if (boss == null)
				continue;
			activeChar.sendMessage(count + ": " + boss.getName() + " (" + boss.getLevel() + ")");
			count++;
		}
		for (L2RaidBossInstance boss : _alive89Raids)
		{
			if (boss == null)
				continue;
			activeChar.sendMessage(count + ": " + boss.getName() + " (" + boss.getLevel() + ")");
			count++;
		}
		for (L2RaidBossInstance boss : _alive90Raids)
		{
			if (boss == null)
				continue;
			activeChar.sendMessage(count + ": " + boss.getName() + " (" + boss.getLevel() + ")");
			count++;
		}
		for (L2RaidBossInstance boss : _alive91Raids)
		{
			if (boss == null)
				continue;
			activeChar.sendMessage(count + ": " + boss.getName() + " (" + boss.getLevel() + ")");
			count++;
		}
		for (L2RaidBossInstance boss : _alive92Raids)
		{
			if (boss == null)
				continue;
			activeChar.sendMessage(count + ": " + boss.getName() + " (" + boss.getLevel() + ")");
			count++;
		}
		for (L2RaidBossInstance boss : _alive93Raids)
		{
			if (boss == null)
				continue;
			activeChar.sendMessage(count + ": " + boss.getName() + " (" + boss.getLevel() + ")");
			count++;
		}
		for (L2RaidBossInstance boss : _alive94Raids)
		{
			if (boss == null)
				continue;
			activeChar.sendMessage(count + ": " + boss.getName() + " (" + boss.getLevel() + ")");
			count++;
		}
		for (L2RaidBossInstance boss : _alive95Raids)
		{
			if (boss == null)
				continue;
			activeChar.sendMessage(count + ": " + boss.getName() + " (" + boss.getLevel() + ")");
			count++;
		}
	}
	
	public static void goToRaid(final L2PcInstance activeChar, final int number)
	{
		int count = 0;
		for (L2RaidBossInstance boss : _alive85Raids)
		{
			if (boss == null)
				continue;
			if (count == number)
			{
				activeChar.teleToLocation(boss.getX(), boss.getY(), boss.getZ());
				return;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive86Raids)
		{
			if (boss == null)
				continue;
			if (count == number)
			{
				activeChar.teleToLocation(boss.getX(), boss.getY(), boss.getZ());
				return;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive87Raids)
		{
			if (boss == null)
				continue;
			if (count == number)
			{
				activeChar.teleToLocation(boss.getX(), boss.getY(), boss.getZ());
				return;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive88Raids)
		{
			if (boss == null)
				continue;
			if (count == number)
			{
				activeChar.teleToLocation(boss.getX(), boss.getY(), boss.getZ());
				return;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive89Raids)
		{
			if (boss == null)
				continue;
			if (count == number)
			{
				activeChar.teleToLocation(boss.getX(), boss.getY(), boss.getZ());
				return;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive90Raids)
		{
			if (boss == null)
				continue;
			if (count == number)
			{
				activeChar.teleToLocation(boss.getX(), boss.getY(), boss.getZ());
				return;
			}
			count++;
		}
		for (L2RaidBossInstance boss : _alive91Raids)
		{
			if (boss == null)
				continue;
			if (count == number)
			{
				activeChar.teleToLocation(boss.getX(), boss.getY(), boss.getZ());
				return;
			}
			count++;
		}
	}
	
	@Override
	public float getVitalityPoints(int damage)
	{
		return -super.getVitalityPoints(damage) / 100;
	}
	
	@Override
	public boolean useVitalityRate()
	{
		return false;
	}
	
	private static class comp1 implements Comparator<Data>
	{
		@Override
		public int compare(Data d1, Data d2)
		{
			return ((Integer) d2.getDamage()).compareTo(((Integer) d1.getDamage()));
		}
	}
	
	public static class Data
	{
		private String	_name;
		private int		_damage;
		
		public Data(String name, int damage)
		{
			_name = name;
			_damage = damage;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int getDamage()
		{
			return _damage;
		}
	}
	
	private void showDamagePage(L2PcInstance player)// TODO: rework the html part. It sucks
	{
		String content = HtmCache.getInstance().getHtm("data/html/custom/DamageCounter/RBdamage.htm");
		NpcHtmlMessage dmgHTM = new NpcHtmlMessage(1);
		String list = "";
		boolean lol = true;
		String color = "FFF8C6";
		int damageContribution = 0;
		for (L2Character attackers : getRaidAttackerByList())
		{
			damageContribution += attackers.getDamageCount();
		}
		List<Data> sorted = new ArrayList<>();
		for (L2Character attacker : getRaidAttackerByList())
		{
			if (attacker == null || attacker.isGM())
			{
				continue;
			}
			sorted.add(new Data(attacker.getName(), attacker.getDamageCount()));
		}
		Collections.sort(sorted, new comp1());
		int limit = 20;
		int i = 0;
		for (Data atkr : sorted)
		{
			i++;
			if (i > limit)
			{
				continue;
			}
			lol = !lol;
			if (lol)
				color = "00080b";
			else
				color = "011118";
			list += "<table width=346 border=0 bgcolor=" + color + ">";
			list += "<tr>";
			list += "<td width=115 align=center><font name=__SystemeditBoxFont color=bdccd4>" + atkr.getName() + "</font></td>";
			NumberFormat nf2 = NumberFormat.getInstance(new Locale("da", "DK"));
			String val2 = nf2.format(atkr.getDamage());
			list += "<td width=115 align=center><font name=__SystemeditBoxFont color=bdccd4>" + val2 + "</font></td>";
			int damagePerc = (int) Math.round((atkr.getDamage() * 100.0) / damageContribution);
			list += "<td width=115 align=center><font name=__SystemeditBoxFont color=bdccd4>" + String.valueOf(damagePerc) + "%</font></td>";
			list += "</tr>	";
			list += "</table>";
			// list += "<tr><td align=left><font color=\"LEVEL\">"+attackerName+"</font></td>";
			// list += "<td align=left><font color=\"LEVEL\">"+val2+"</font></td>";
			// list += "<td align=left><font color=\"LEVEL\">"+String.valueOf(damagePerc)+"%</font></td></tr>";
		}
		content = content.replace("%RBname%", getName());
		content = content.replace("%list%", list);
		dmgHTM.setHtml(content);
		// player.sendPacket(new TutorialShowHtml(content));
		// player.sendPacket(new TutorialShowQuestionMark(player.getObjectId()));
		player.sendPacket(dmgHTM);
	}
}