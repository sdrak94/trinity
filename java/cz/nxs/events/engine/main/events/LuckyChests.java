package cz.nxs.events.engine.main.events;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import cz.nxs.events.EventGame;
import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventPlayerData;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.Loc;
import cz.nxs.events.engine.base.PvPEventPlayerData;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.base.description.EventDescription;
import cz.nxs.events.engine.base.description.EventDescriptionSystem;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.MainEventManager;
import cz.nxs.events.engine.main.events.LuckyChests.LuckyChestsPlayerData.SlotInfo;
import cz.nxs.events.engine.stats.GlobalStatsModel;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.delegate.ItemData;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.NpcTemplateData;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.interf.delegate.ShortCutData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class LuckyChests extends Deathmatch
{
	protected class LuckyChestsData extends DMData
	{
		private Map<ChestType, FastList<NpcData>> _chests;
		
		protected LuckyChestsData(int instance)
		{
			super(instance);
			
			_chests = new FastMap<ChestType, FastList<NpcData>>();
			for(ChestType ch : ChestType.values())
				_chests.put(ch, new FastList<NpcData>());
		}
	}
	
	protected class LuckyChestsEventInstance extends DMEventInstance
	{
		public LuckyChestsEventInstance(InstanceData instance)
		{
			super(instance);
		}
		
		@SuppressWarnings("incomplete-switch")
		@Override
		public void run()
		{
			try
			{
				/**/ if(NexusLoader.detailedDebug) print("Event: running task of state " + _nextState.toString() + "...");
				
				switch (_nextState)
				{
					case START:
					{
						if(checkPlayers(_instance.getId()))
						{
							teleportPlayers(_instance.getId(), SpawnType.Regular, true);
							
							setupTitles(_instance.getId());
							
							enableMarkers(_instance.getId(), true);
							
							clearShortcuts(_instance.getId());
							preparePlayers(_instance.getId());
							
							spawnChests(_instance.getId());
							
							forceSitAll(_instance.getId());
							
							setNextState(EventState.FIGHT);
							scheduleNextTask(10000);
						}
						
						break;
					}
					case FIGHT:
					{
						forceStandAll(_instance.getId());
						
						setNextState(EventState.END);

						_clock.startClock(_manager.getRunTime());
						break;
					}
					case END:
					{
						_clock.setTime(0, true);
						
						unspawnChests(_instance.getId());
						
						setNextState(EventState.INACTIVE);
						
						if(!instanceEnded() && _canBeAborted)
						{
							if(_canRewardIfAborted)
								rewardAllPlayers(_instance.getId(), getInt("scoreForReward"), 0);
							
							clearEvent(_instance.getId());
						}
						
						break;
					}
				}
				
				/**/ if(NexusLoader.detailedDebug) print("Event: ... finished running task. next state " + _nextState.toString());
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				_manager.endDueToError(LanguageEngine.getMsg("event_error"));
			}
		}
	}
	
	private enum ChestType
	{
		CLASSIC, SHABBY, LUXURIOUS, BOX, NEXUSED
	}
	
	private enum TransformType
	{
		BUNNY, FROG, PIG, YETI
	}
	
	private enum WeaponType
	{
		SWORD,
		FASTSWORD,
		BOW,
		SUPERBOW,
		POLEARM,
		DAGGER,
		HAMMER,
		BIGHAMMER,
		BIGSWORD
	}
	
	private enum LuckyItem
	{
		//no grade
		GLADIUS(WeaponType.SWORD, 1),
		DIRK(WeaponType.DAGGER, 1),
		BOW(WeaponType.BOW, 1),
		LANCE(WeaponType.POLEARM, 1),
		HAMMER(WeaponType.HAMMER, 1),
		ZWEIHANDER(WeaponType.BIGSWORD, 1),
		SHIELD(null, 1),
		
		//d grade
		KNIGHTSWORD(WeaponType.SWORD, 2),
		DAGGER_CRAFTED(WeaponType.DAGGER, 2),
		LONGBOW(WeaponType.BOW, 2),
		PIKE(WeaponType.POLEARM, 2),
		HEAVYSWORD(WeaponType.BIGSWORD, 2),
		
		REINFORCED_BOW(WeaponType.SUPERBOW, 1),
		HEAVYHAMMER(WeaponType.BIGHAMMER, 1),
		SABER(WeaponType.FASTSWORD, 1);
		
		public WeaponType _type;
		public int _grade;
		LuckyItem(WeaponType type, int grade)
		{
			_type = type;
			_grade = grade;
		}
		
		private boolean isWeapon()
		{
			if(this == SHIELD)
				return false;
			return true;
		}
	}
	
	private String _scorebarInfo;
	
	private boolean _customShortcuts;
	
	private int _classicChestId;
	private int _shabbyChestId;
	private int _luxuriousChestId;
	private int _boxChestId;
	private int _nexusedChestId;
	
	// Counts of chests
	private int _classicChestsCountMin;
	private int _classicChestsCountMax;
	
	private int _shabbyChestsCountMin;
	private int _shabbyChestsCountMax;
	
	private int _luxuriousChestsCountMin;
	private int _luxuriousChestsCountMax;
	
	private int _nexusedChestsCountMin;
	private int _nexusedChestsCountMax;
	
	private int _boxChestsCountMin;
	private int _boxChestsCountMax;
	//
	
	private boolean _shabbyChestEnabled;
	private boolean _luxuriousChestEnabled;
	private boolean _boxChestEnabled;
	private boolean _nexusedChestEnabled;
	
	// items
	private int _gladiusItemId;
	private int _dirkItemId;
	private int _bowItemId, _bowArrowId;
	private int _lanceItemId;
	private int _hammerItemId;
	private int _zweihanderItemId;
	private int _shieldItemId;
	
	private int _knightswordItemId;
	private int _craftedDaggerItemId;
	private int _longBowItemId, _longBowArrowItemId;
	private int _pikeItemId;
	private int _heavyswordItemId;
	private int _reinforcedBowItemId, _reinforcedArrowItemId;
	private int _heavyHammerItemId;
	private int _saberItemId;
	
	private int _arrowCount;
	//
	
	private boolean _allowTransformations;
	private int _bunnyTransformId;
	private int _frogTransformId;
	private int _pigTransformId;
	private int _yetiTransformId;
	
	private int _bunnyTransformDuration;
	private int _frogTransformDuration;
	private int _pigTransformDuration;
	private int _yetiTransformDuration;
	
	private boolean _transformationHalfResTime;
	
	// 1000 ~ 100%; 1% by default
	private int _jokerChanceOnHit;
	
	// 100 ~ 100%
	private int _jokerTeleportChance;
	
	private int _bunnyKilledScore;
	private int _frogKilledScore;
	private int _pigKilledScore;
	private int _yetiKilledScore;
	
	
	// chances configs:
	private int classicPositiveEffect;
	private int classicObtainWeaponChance;
	
	private int luxPositiveEffect;
	private int luxObtainWeaponChance;
	
	private int shabbyTransformChance;
	private int shabbyShieldChance;
	
	private int nexusedMainEffectChance;
	
	
	private boolean _enableFearFireworking;
	private boolean _explosionShieldResetKillstreak;
	private boolean _bombShieldProtectsParalyzation;
	private boolean _bombShieldProtectsFear = true;
	
	private int _aggressionSkillId;
	private int _whirlwindSkill;
	private int _rushSkill;
	private int _stunSkill;
	private int _backstabSkill;
	private String _jokerChestName;
	
	private String[] _jokerChestTexts;
	
	private FastMap<Integer, Integer> _skillsForAll;
	private FastMap<LuckyItem, FastMap<Integer, Integer>> _skillsForItems;
	
	public LuckyChests(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Looser, RewardPosition.Tie, RewardPosition.Numbered, RewardPosition.Range, RewardPosition.FirstRegistered });
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		final String weaponTypes = "GLADIUS, DIRK, BOW, LANCE, HAMMER, ZWEIHANDER, SHIELD, KNIGHTSWORD, DAGGER_CRAFTED, LONGBOW, PIKE, HEAVYSWORD, REINFORCED_BOW, HEAVYHAMMER, SABER";
		final String weaponSkillsDefaultVal = "DIRK-35003-1,BOW-35001-1,LANCE-35002-1,HAMMER-35004-1,ZWEIHANDER-35005-1,SHIELD-35006-1,KNIGHTSWORD-35007-1,DAGGER_CRAFTED-35008-1,LONGBOW-35009-1,PIKE-35010-1,HEAVYSWORD-35011-1,REINFORCED_BOW-35012-1,HEAVYHAMMER-35013-1,SABER-35014-1";
		final String jokerDefault = "hahahaha,hahahahahaha,hehehe,ha... ha... ha...,hihihi,ahahaha,ehehehe,eki eki,keh keh,muhahaha,nihaha,puhahaha,uhahahaha,zuhahaha,eheheh,moaha ha,kahkahkah,tee hee hee!,LOL!";
		
		addConfig(new ConfigModel("scoreForReward", "0", "The minimum score required to get a reward (includes all possible rewards)."));
		removeConfig("killsForReward"); // no killing on this event - you only 'kill' chests
		removeConfig("antifeedProtection"); // antifeed not needed for this event
		removeConfig("waweRespawn"); // wawe respawn not needed for this event
		removeConfig("firstBloodMessage"); // no first blood as this is not a PvP event
		removeConfig("allowScreenScoreBar"); // no scorebar on this event
		
		addConfig("Chests", new ConfigModel("classicChestId", "8993", "The NPC ID of the classic chest in this event."));
		addConfig("Chests", new ConfigModel("shabbyChestId", "8994", "The NPC ID of the shabby chest in this event. Put 0 to disable this chest type on this event."));
		addConfig("Chests", new ConfigModel("luxuriousChestId", "8995", "The NPC ID of the luxurious chest in this event. Put 0 to disable this chest type on this event."));
		addConfig("Chests", new ConfigModel("boxChestId", "8996", "The NPC ID of the bonus crate in this event. Put 0 to disable this chest type on this event."));
		addConfig("Chests", new ConfigModel("nexusedChestId", "8997", "The NPC ID of the nexused chest in this event. Put 0 to disable this chest type on this event."));
		
		addConfig("Chests", new ConfigModel("classicChestCount", "40-60", "Count of Classic chests spawned in the event. Format: MIN-MAX. For example: '30-60' will choose randomly value between 30 and 60."));
		addConfig("Chests", new ConfigModel("shabbyChestCount", "15-25", "Count of Shabby chests spawned in the event. Format: MIN-MAX. For example: '15-25' will choose randomly value between 15 and 25."));
		addConfig("Chests", new ConfigModel("luxuriousChestCount", "8-12", "Count of Luxurious chests spawned in the event. Format: MIN-MAX. For example: '8-12' will choose randomly value between 8 and 12."));
		addConfig("Chests", new ConfigModel("boxChestCount", "5-10", "Count of Box chests spawned in the event. Format: MIN-MAX. For example: '5-10' will choose randomly value between 5 and 10."));
		addConfig("Chests", new ConfigModel("nexusedChestCount", "1-3", "Count of Nexused chests spawned in the event. Format: MIN-MAX. For example: '1-3' will choose randomly value between 1 and 3."));

		addConfig("Chests", new ConfigModel("classicPositiveEffect", "70", "The chance (in percent) for positive effect (effect, which gives score) when the player kills a classic chest."));
		addConfig("Chests", new ConfigModel("classicObtainWeaponChance", "3", "When the player kills a classic chest and positive effect is selected, this defines the chance (in percent) to receive a new weapon."));
		addConfig("Chests", new ConfigModel("luxPositiveEffect", "85", "The chance (in percent) for positive effect (effect, which gives score) when the player kills a luxurious chest."));
		addConfig("Chests", new ConfigModel("luxObtainWeaponChance", "5", "When the player kills a luxurious chest and positive effect is selected, this defines the chance (in percent) to receive a new weapon."));
		addConfig("Chests", new ConfigModel("shabbyTransformChance", "20", "The chance (in percent) that the Shabby chest will transform the player when he kills it."));
		addConfig("Chests", new ConfigModel("shabbyShieldChance", "30", "The chance (in percent) that the Shabby chest will give the player a one-bomb shield."));
		addConfig("Chests", new ConfigModel("nexusedMainEffectChance", "60", "The chance (in percent) that Nexused chest will reward the player with either Rush skill or x4 score."));
		
		addConfig(new ConfigModel("skillsForAllPlayers", "35000-1", "IDs of skills which will be given to every player on the event. The purpose of this is to make all players equally strong. Format: <font color=LEVEL>SKILLID-LEVEL</font> (eg. '35000-1').", InputType.MultiAdd));
		
		addConfig(new ConfigModel("scorebarInfoType", "TopScore", "You can specify what kind of information (beside Time) will be shown in the scorebar in player's screen.", InputType.Enum).addEnumOptions(new String[]{ "TopScore", "ChestsLeft"}));
		addConfig(new ConfigModel("customShortcuts", "true", "True to turn on the custom shortcuts engine, which deletes all player's shortcuts during the event run time and puts there it's own custom shortcuts. When the event ends, player's shortcuts will be restored back.", InputType.Boolean));
		
		addConfig("Transforms", new ConfigModel("allowTransformations", "true", "Enable/disable random transformations on this event. Sometimes, when a player kills a chest, he gets transformed to a randomly chosen transformation and is usually allowed to kill the other players (and gets score for it).", InputType.Boolean));
		
		addConfig("Transforms", new ConfigModel("transformShortResTime", "true", "If true, resurrection delay will be /2 if the player died transformed. It will make players less emo.", InputType.Boolean));
		addConfig("Chests", new ConfigModel("fearLaunchesFireworks", "true", "If true, all players feared by a chest will continously launch fireworks as they run away.", InputType.Boolean));
		addConfig("Chests", new ConfigModel("explosionShieldResetKillstreak", "false", "If enabled, the player's killstreak will be reset after the chest he kills explodes, and the fact if the player was protected by a shield or not doesn't matter. Basically, setting this to true will make killstreaks harder.", InputType.Boolean));
		
		addConfig("Chests", new ConfigModel("bombShieldProtectsParalyzation", "true", "If enabled, the bomb shield will protect player from being paralysed by a chest.", InputType.Boolean));
		addConfig("Chests", new ConfigModel("bombShieldProtectsFear", "true", "If enabled, the bomb shield will protect player from being feared by a chest.", InputType.Boolean));
	
		addConfig("Chests", new ConfigModel("jokerChestName", "Joker Chest", "The name of the joker chest visible in its title."));
		addConfig("Chests", new ConfigModel("jokerActivationChance", "10", "The chance for activating joker - the chest will laugh and disappear and possibly also stun and teleport the player away, giving him no score. <font color=LEVEL>Activated everytime the player hits a chest. 10 equals 1% (1000 equals 100%).</font>"));
		addConfig("Chests", new ConfigModel("jokerTeleportChance", "50", "When the joker (config 'jokerActivationChance') is activated, this is the chance that the chest will teleport (blink) player away and stun him for a few seconds. <font color=LEVEL>In percent - 50 equals 50%. Doesn't work for Interlude version.</font>"));
		addConfig("Chests", new ConfigModel("jokerPhrases", jokerDefault, "Write here things that might be said by the chest if the joker is activated.", InputType.MultiAdd));
		
		addConfig(new ConfigModel("aggressionSkillId", "980", "The ID of Aggression skill ID given to the player."));
		addConfig(new ConfigModel("whirlwindSkillId", "36", "The ID of Whirlwind skill ID given to the player (if he's obtained polearm)."));
		addConfig(new ConfigModel("rushSkillId", "484", "The ID of Rush skill ID given to the player from Nexused chest."));
		addConfig(new ConfigModel("stunSkillId", "260", "The ID of Stun skill ID given to the player with hammer."));
		addConfig(new ConfigModel("backstabSkillId", "30", "The ID of Backstab skill ID given to the player with dagger."));
		
		addConfig("Transforms", new ConfigModel("bunnyKillScore", "2", "Score given to the player for killing a bunny. If the player is Yeti, he will always receive only 1 point."));
		addConfig("Transforms", new ConfigModel("pigKillScore", "3", "Score given to the player for killing a pig. If the player is Yeti, he will always receive only 1 point."));
		addConfig("Transforms", new ConfigModel("yetiKillScore", "6", "Score given to the player for killing a yeti."));
		addConfig("Transforms", new ConfigModel("frogKillScore", "3", "Score given to the player for killing a frog. The frog dies on 1 hit only, but it is difficult to catch it."));
		
		addConfig("Transforms", new ConfigModel("bunnyTransformId", "105", "The ID of the bunny transformation. Default value is fine unless your core is super-modified."));
		addConfig("Transforms", new ConfigModel("pigTransformId", "104", "The ID of the bunny transformation. Default value is fine unless your core is super-modified."));
		addConfig("Transforms", new ConfigModel("yetiTransformId", "102", "The ID of the bunny transformation. Default value is fine unless your core is super-modified."));
		addConfig("Transforms", new ConfigModel("frogTransformId", "111", "The ID of the bunny transformation. Default value is fine unless your core is super-modified."));
		
		addConfig("Transforms", new ConfigModel("bunnyTransformDuration", "60", "Specify how long will the bunny transformation last on the player till it gets removed. In seconds. Note that it will also disappear if the player dies."));
		addConfig("Transforms", new ConfigModel("pigTransformDuration", "45", "Specify how long will the pig transformation last on the player till it gets removed. In seconds. Note that it will also disappear if the player dies."));
		addConfig("Transforms", new ConfigModel("yetiTransformDuration", "60", "Specify how long will the yeti transformation last on the player till it gets removed. In seconds. Note that it will also disappear if the player dies."));
		addConfig("Transforms", new ConfigModel("frogTransformDuration", "7", "Specify how long will the frog transformation last on the player till it gets removed. In seconds. Note that it will also disappear if the player dies."));
		
		addConfig("Weapons", new ConfigModel("weaponSkills", weaponSkillsDefaultVal, "IDs of skills which will be given to players holding certain weapon type (<font color=7f7f7f>" + weaponTypes + "</font>). Format: <font color=LEVEL>WEAPON_TYPE-SKILL_ID-LEVEL</font> (eg. 'SWORD-255-2'). Also, all skills written in this list will be allowed to use in this event (all other skills are disabled on this event, including heals).", InputType.MultiAdd));
		
		// weapon ids
		addConfig("Weapons", new ConfigModel("gladiusItemId", "66", "The item ID of the sword weapon. This is the basic weapon owned by all players in the event."));
		addConfig("Weapons", new ConfigModel("dirkItemId", "216", "The item ID of the dirk weapon. Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("bowItemId", "14-17", "The item ID of the bow weapon. Format <font color=LEVEL>BOW_ID-ARROW_ID</font> (eg. '14-17'). Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("lanceItemId", "97", "The item ID of the lance weapon. Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("hammerItemId", "87", "The item ID of the hammer weapon. Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("zweihanderItemId", "5284", "The item ID of the zweihander weapon. Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("shieldItemId", "102", "The item ID of the shield weapon. Put -1 to disable this weapon on the event."));
		
		addConfig("Weapons", new ConfigModel("knightswordItemId", "128", "The item ID of the knight sword weapon. Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("craftedDaggerItemId", "220", "The item ID of the crafted dagger weapon. Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("longBowItemId", "275-1341", "The item ID of the long bow weapon. Format <font color=LEVEL>BOW_ID-ARROW_ID</font> (eg. '14-17'). Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("pikeItemId", "292", "The item ID of the pike weapon. Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("heavyswordItemId", "5285", "The item ID of the heavy sword weapon. Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("reinforcedBowItemId", "279-1341", "The item ID of the reinforced bow weapon. Format <font color=LEVEL>BOW_ID-ARROW_ID</font> (eg. '14-17'). Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("heavyHammerItemId", "187", "The item ID of the heavy hammer weapon. Put -1 to disable this weapon on the event."));
		addConfig("Weapons", new ConfigModel("saberItemId", "123", "The item ID of the saber weapon. Put -1 to disable this weapon on the event."));
		
		addConfig("Weapons", new ConfigModel("arrowCount", "300", "The count of arrows given to the player, when he obtains a bow."));
	}
	
	@Override
	public void initEvent()
	{
		_waweRespawn = false;
		_antifeed = false;
		_allowSchemeBuffer = false;
		
		super.initEvent();
		
		_allowScoreBar = false;
		_scorebarInfo = getString("scorebarInfoType");
		_customShortcuts = getBoolean("customShortcuts");
		
		_transformationHalfResTime = getBoolean("transformShortResTime");
		_explosionShieldResetKillstreak = getBoolean("explosionShieldResetKillstreak");
		_bombShieldProtectsParalyzation = getBoolean("bombShieldProtectsParalyzation");
		_bombShieldProtectsFear = getBoolean("bombShieldProtectsFear");
		
		_aggressionSkillId = getInt("aggressionSkillId");
		_whirlwindSkill = getInt("whirlwindSkillId");
		_rushSkill = getInt("rushSkillId");
		_stunSkill = getInt("stunSkillId");
		_backstabSkill = getInt("backstabSkillId");
		
		_jokerChestName = getString("jokerChestName");
		_jokerChanceOnHit = getInt("jokerActivationChance");
		_jokerTeleportChance = getInt("jokerTeleportChance");
		
		_enableFearFireworking = getBoolean("fearLaunchesFireworks");
		
		_allowTransformations = getBoolean("allowTransformations");
		
		classicPositiveEffect = getInt("classicPositiveEffect");
		classicObtainWeaponChance = getInt("classicObtainWeaponChance");
		
		luxPositiveEffect = getInt("luxPositiveEffect");
		luxObtainWeaponChance = getInt("luxObtainWeaponChance");
		
		shabbyTransformChance = getInt("shabbyTransformChance");
		shabbyShieldChance = getInt("shabbyShieldChance");
		
		nexusedMainEffectChance = getInt("nexusedMainEffectChance");
		
		_bunnyKilledScore = getInt("bunnyKillScore");
		_frogKilledScore = getInt("pigKillScore");
		_pigKilledScore = getInt("yetiKillScore");
		_yetiKilledScore = getInt("frogKillScore");
		
		_bunnyTransformId = getInt("bunnyTransformId");
		_pigTransformId = getInt("pigTransformId");
		_yetiTransformId = getInt("yetiTransformId");
		_frogTransformId = getInt("frogTransformId");
		
		_bunnyTransformDuration = getInt("bunnyTransformDuration");
		_pigTransformDuration = getInt("pigTransformDuration");
		_yetiTransformDuration = getInt("yetiTransformDuration");
		_frogTransformDuration = getInt("frogTransformDuration");
		
		_classicChestId = getInt("classicChestId");
		_shabbyChestId = getInt("shabbyChestId");
		_luxuriousChestId = getInt("luxuriousChestId");
		_boxChestId = getInt("boxChestId");
		_nexusedChestId = getInt("nexusedChestId");
		
		try
		{
			String s = getString("bowItemId");
			_bowItemId = Integer.parseInt(s.split("-")[0]);
			_bowArrowId = Integer.parseInt(s.split("-")[1]);
			
			s = getString("longBowItemId");
			_longBowItemId = Integer.parseInt(s.split("-")[0]);
			_longBowArrowItemId = Integer.parseInt(s.split("-")[1]);
			
			s = getString("reinforcedBowItemId");
			_reinforcedBowItemId = Integer.parseInt(s.split("-")[0]);
			_reinforcedArrowItemId = Integer.parseInt(s.split("-")[1]);
		}
		catch (Exception e)
		{
			NexusLoader.debug("Error while loading Bows in Lucky chests event. Check out their configs - " + e.toString(), Level.WARNING);
			
			_bowItemId = -1;
			_longBowItemId = -1;
			_reinforcedBowItemId = -1;
		}
		
		_gladiusItemId = getInt("gladiusItemId");
		_dirkItemId = getInt("dirkItemId");
		_lanceItemId = getInt("lanceItemId");
		_hammerItemId = getInt("hammerItemId");
		_zweihanderItemId = getInt("zweihanderItemId");
		_shieldItemId = getInt("shieldItemId");
		_knightswordItemId = getInt("knightswordItemId");
		_craftedDaggerItemId = getInt("craftedDaggerItemId");
		_pikeItemId = getInt("pikeItemId");
		_heavyswordItemId = getInt("heavyswordItemId");
		_heavyHammerItemId = getInt("heavyHammerItemId");
		_saberItemId = getInt("saberItemId");
		
		_arrowCount = getInt("arrowCount");
		
		try
		{
			String s = getString("classicChestCount");
			_classicChestsCountMin = Integer.parseInt(s.split("-")[0]);
			_classicChestsCountMax = Integer.parseInt(s.split("-")[1]);
			
			s = getString("shabbyChestCount");
			_shabbyChestsCountMin = Integer.parseInt(s.split("-")[0]);
			_shabbyChestsCountMax = Integer.parseInt(s.split("-")[1]);
			
			s = getString("luxuriousChestCount");
			_luxuriousChestsCountMin = Integer.parseInt(s.split("-")[0]);
			_luxuriousChestsCountMax = Integer.parseInt(s.split("-")[1]);
			
			s = getString("boxChestCount");
			_boxChestsCountMin = Integer.parseInt(s.split("-")[0]);
			_boxChestsCountMax = Integer.parseInt(s.split("-")[1]);
			
			s = getString("nexusedChestCount");
			_nexusedChestsCountMin = Integer.parseInt(s.split("-")[0]);
			_nexusedChestsCountMax = Integer.parseInt(s.split("-")[1]);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			clearEvent();
			
			NexusLoader.debug("Event: wrong format for a config that specifies count of chests.");
			/**/ if(NexusLoader.detailedDebug) print("Event: wrong format for a config that specifies count of chests.");
		}
		
		_shabbyChestEnabled = _shabbyChestId > 0;
		_luxuriousChestEnabled = _luxuriousChestId > 0;
		_boxChestEnabled = _boxChestId > 0;
		_nexusedChestEnabled = _nexusedChestId > 0;
		
		if(!checkNpcs())
		{
			clearEvent();
			announce("Missing NPC Templates for chests event.");
		}
		
		// load joker strings
		if(!getString("jokerPhrases").equals(""))
		{
			String[] splits = getString("jokerPhrases").split(",");
			_jokerChestTexts = splits;
		}
		
		// load skills for all players
		if(!getString("skillsForAllPlayers").equals(""))
		{
			String[] splits = getString("skillsForAllPlayers").split(",");
			_skillsForAll = new FastMap<Integer, Integer>();
			
			try
			{
				String id, level;
				for(int i = 0; i < splits.length; i++)
				{
					id = splits[i].split("-")[0];
					level = splits[i].split("-")[1];
					_skillsForAll.put(Integer.parseInt(id), Integer.parseInt(level));
				}
			}
			catch (Exception e)
			{
				NexusLoader.debug("Error while loading config 'skillsForAllPlayers' for event " + getEventName() + " - " + e.toString(), Level.SEVERE);
			}
		}
		
		
		// load skills for certain weapons
		if(!getString("weaponSkills").equals(""))
		{
			String[] splits = getString("weaponSkills").split(",");
			_skillsForItems = new FastMap<LuckyItem, FastMap<Integer, Integer>>();
			
			try
			{
				String type, id, level;
				LuckyItem wType;
				for(int i = 0; i < splits.length; i++)
				{
					type = splits[i].split("-")[0];
					id = splits[i].split("-")[1];
					level = splits[i].split("-")[2];
					
					try
					{
						wType = LuckyItem.valueOf(type);
						
						if(wType == null)
						{
							NexusLoader.debug("LuckyItem type " + wType + " doesn't exist (lucky chests config).");
							continue;
						}
					}
					catch (Exception e)
					{
						continue;
					}
					
					if(!_skillsForItems.containsKey(wType))
						_skillsForItems.put(wType, new FastMap<Integer, Integer>());
					
					_skillsForItems.get(wType).put(Integer.parseInt(id), Integer.parseInt(level));
				}
			}
			catch (Exception e)
			{
				NexusLoader.debug("Error while loading config 'weaponSkills' for event " + getEventName() + " - " + e.toString(), Level.SEVERE);
			}
		}
	}
	
	@Override
	public void runEvent()
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: started runEvent()");
		
		if(!dividePlayers())
		{
			clearEvent();
			return;
		}
		
		DMEventInstance match;
		_matches = new FastMap<Integer, DMEventInstance>();
		for(InstanceData instance : _instances)
		{
			/**/ if(NexusLoader.detailedDebug) print("Event: creating eventinstance for instance " + instance.getId());
			
			match = createEventInstance(instance);
			_matches.put(instance.getId(), match);
			
			_runningInstances ++;
			
			match.scheduleNextTask(0);
			
			/**/ if(NexusLoader.detailedDebug) print("Event: event instance started");
		}
		
		/**/ if(NexusLoader.detailedDebug) print("Event: finished runEvent()");
	}
	
	@Override
	public void onDamageGive(CharacterData cha, CharacterData target, int damage, boolean isDOT)
	{
		if(cha.isPlayer() && getPlayerData(cha.getEventInfo()).getTransformation() == null)
		{
			PlayerEventInfo player = cha.getEventInfo();
			
			if(CallBack.getInstance().getOut().random(1000) < _jokerChanceOnHit && target.isNpc() && getType(target.getNpc().getNpcId()) != null)
			{
				if(effect(player, target.getNpc(), EffectType.Laugh, null, null).success)
				{
					if(CallBack.getInstance().getOut().random(100) < _jokerTeleportChance)
					{
						player.broadcastSkillUse(null, null, 628, 1);
						player.getSkillEffects(35015, 1);
					}
				}
			}
		}
	}
	
	@Override
	public boolean isInEvent(CharacterData ch)
	{
		if(ch.isNpc())
		{
			NpcData npc = ch.getNpc();
			if(getType(npc.getNpcId()) != null)
				return true;
		}
		
		return false;
	}
	
	@Override
	public boolean allowKill(CharacterData target, CharacterData killer)
	{
		if(target.isNpc() && killer.isPlayer())
		{
			NpcData npc = target.getNpc();
			PlayerEventInfo player = killer.getEventInfo();
			
			if(!canServerKillChest(npc, player))
				return false;
		}
		
		return true;
	}
	
	private void giveSkill(PlayerEventInfo player, SkillData skill)
	{
		player.addSkill(skill, false);
		player.sendSkillList();
		
		if(_customShortcuts)
		{
			if(getPlayerData(player).getSkillShortcut(skill) == null)
			{
				SlotInfo slot = getPlayerData(player).getNextFreeShortcutSlot(false);
				if(slot != null)
				{
					ShortCutData sh = player.createSkillShortcut(slot.slot, slot.page, skill);
					getPlayerData(player).addSkillShortcut(skill, sh);
					
					player.registerShortcut(sh, true);
				}
			}
		}
	}
	
	private void giveSkillForWeapon(PlayerEventInfo player, LuckyItem item, int id, int level)
	{
		getPlayerData(player).addSkillForWeapon(id, level, item);
		
		updateSkillsForWeapon(player);
	}
	
	private void updateSkillsForWeapon(PlayerEventInfo player)
	{
		LuckyItem currentWeapon = getPlayerData(player).getActiveWeapon();
		
		if(currentWeapon == getPlayerData(player).getActiveWeaponWithSkills())
			return;
		
		SkillData skill = null;
		
		if(getPlayerData(player).getActiveWeaponWithSkills() != null)
		{
			for(Entry<Integer, Integer> e : getPlayerData(player).getSkillsForWeapon(getPlayerData(player).getActiveWeaponWithSkills()).entrySet())
			{
				skill = new SkillData(e.getKey(), e.getValue());
				
				// remove skills of previous weapon (if any)
				if(skill.exists())
				{
					player.removeBuff(skill.getId());
					player.removeSkill(skill.getId());
					player.sendSkillList();
					
					if(_customShortcuts)
					{
						ShortCutData sh = getPlayerData(player).getSkillShortcut(skill);
						if(sh != null)
						{
							player.removeShortCut(sh, true);
							getPlayerData(player).removeSkillShortcut(skill, sh);
						}
					}
				}
			}
		}
		
		getPlayerData(player).setActiveWeaponWithSkills(currentWeapon);
		
		for(Entry<Integer, Integer> e : getPlayerData(player).getSkillsForWeapon(currentWeapon).entrySet())
		{
			skill = new SkillData(e.getKey(), e.getValue());
			
			// give skills of newly equiped weapon
			if(skill.exists())
			{
				player.addSkill(skill, false);
				player.sendSkillList();
				
				if(_customShortcuts)
				{
					if(getPlayerData(player).getSkillShortcut(skill) == null)
					{
						SlotInfo slot = getPlayerData(player).getNextFreeShortcutSlot(false);
						if(slot != null)
						{
							ShortCutData sh = player.createSkillShortcut(slot.slot, slot.page, skill);
							getPlayerData(player).addSkillShortcut(skill, sh);
							
							player.registerShortcut(sh, true);
						}
					}
				}
			}
		}
	}
	
	private void giveWeapon(PlayerEventInfo player, LuckyItem itemType, boolean equip, boolean forceOverride)
	{
		if(!getPlayerData(player).hasWeapon(itemType))
		{
			LuckyItem stackingItem = getPlayerData(player).getWeaponOfType(itemType._type);
			if(stackingItem != null)
			{
				if(stackingItem._grade < itemType._grade || forceOverride)
				{
					removeWeapon(player, stackingItem);
				}
				else
				{
					return;
				}
			}
			
			int itemId = getItemId(itemType);
			
			if(itemId == -1)
				return;
			
			ItemData item = player.addItem(itemId, 1, true);
			
			if(item == null)
			{
				NexusLoader.debug("Item ID " + itemId + " for lucky chests event doesn't exist. Please edit it in configs!!");
				return;
			}
			
			//give arrows
			if(itemType == LuckyItem.BOW)
				player.addItem(_bowArrowId, _arrowCount, true);
			else if(itemType == LuckyItem.LONGBOW)
				player.addItem(_longBowArrowItemId, _arrowCount, true);
			else if(itemType == LuckyItem.REINFORCED_BOW)
				player.addItem(_reinforcedArrowItemId, _arrowCount, true);
			
			if(equip)
			{
				ItemData wpn;
				if(itemType != LuckyItem.SHIELD)
				{
					wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
					if (wpn != null)
						player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_R_HAND());
				}

				wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_LHAND());
				if (wpn != null)
					player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_L_HAND());

				player.equipItem(item);

				LuckyItem oldWeapon = getPlayerData(player).getActiveWeapon();
				if(itemType.isWeapon())
				{
					getPlayerData(player).setActiveWeapon(itemType);
					weaponSkills(player, oldWeapon, itemType);
				}
				else
				{
					weaponSkills(player, null, itemType);
				}
			}
			
			player.broadcastUserInfo();
			
			getPlayerData(player).addWeapon(itemType);
			
			if(_customShortcuts)
			{
				SlotInfo slot = getPlayerData(player).getNextFreeShortcutSlot(true);
				if(slot != null)
				{
					ShortCutData sh = player.createItemShortcut(slot.slot, slot.page, item);
					getPlayerData(player).addWeaponShortcut(itemType, sh);
					
					player.registerShortcut(sh, true);
				}
			}
			
			if(_skillsForItems != null && _skillsForItems.containsKey(itemType))
			{
				SkillData sk;
				for(Entry<Integer, Integer> e : _skillsForItems.get(itemType).entrySet())
				{
					sk = new SkillData(e.getKey(), e.getValue());
					if(sk.exists())
						player.addSkill(sk, false);
				}
				
				player.sendSkillList();
			}
			
			checkShield(player);
		}
	}
	
	private void removeSkill(PlayerEventInfo player, SkillData skill, SkillType type, boolean updateSkillList)
	{
		if(getPlayerData(player).hasSkill(skill.getId(), type))
		{
			player.removeBuff(skill.getId());
			player.removeSkill(skill.getId());
			
			if(updateSkillList)
				player.sendSkillList();
			
			if(_customShortcuts && updateSkillList)
			{
				ShortCutData sh = getPlayerData(player).getSkillShortcut(skill);
				if(sh != null)
				{
					player.removeShortCut(sh, true);
					getPlayerData(player).removeSkillShortcut(skill, sh);
				}
			}
		}
	}
	
	private void removeWeapon(PlayerEventInfo player, LuckyItem type)
	{
		if(getPlayerData(player).hasWeapon(type))
		{
			int itemId = getItemId(type);
			
			// unequip if equipped
			ItemData wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
			if (wpn.exists() && wpn.getItemId() == itemId)
			{
				ItemData[] unequiped = player.unEquipItemInBodySlotAndRecord(wpn.getBodyPart());
				player.inventoryUpdate(unequiped);
			}
			
			wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_LHAND());
			if (wpn.exists() && wpn.getItemId() == itemId)
			{
				ItemData[] unequiped = player.unEquipItemInBodySlotAndRecord(wpn.getBodyPart());
				player.inventoryUpdate(unequiped);
			}
			
			if(_customShortcuts)
			{
				ShortCutData sh = getPlayerData(player).getWeaponShortCut(type);
				if(sh != null)
				{
					player.removeShortCut(sh, true);
					getPlayerData(player).removeWeaponShortcut(type, sh);
				}
			}
			
			getPlayerData(player).removeWeapon(type);
			
			if(getPlayerData(player).getActiveWeapon() == type)
				getPlayerData(player).setActiveWeapon(null);
			
			// destroy the item itself
			player.destroyItemByItemId(itemId, 1);
			
			if(_skillsForItems != null && _skillsForItems.containsKey(type))
			{
				SkillData sk;
				for(Entry<Integer, Integer> e : _skillsForItems.get(type).entrySet())
				{
					sk = new SkillData(e.getKey(), e.getValue());
					if(sk.exists())
						player.removeSkill(sk.getId());
				}
				
				player.sendSkillList();
			}
			
			checkShield(player);
		}
	}
	
	@Override
	public void onItemUse(PlayerEventInfo player, ItemData item)
	{
		LuckyItem itemType = getWeaponType(item.getItemId());
		
		if(itemType.isWeapon())
		{
			if(item.isEquipped() && item.isWeapon())
			{
				LuckyItem oldWeapon = getPlayerData(player).getActiveWeapon();

				if(oldWeapon == itemType)
				{
					checkShield(player);
					return;
				}
				
				// set new weapon type
				getPlayerData(player).setActiveWeapon(itemType);
				
				weaponSkills(player, oldWeapon, itemType);
			}
		}
		else // shield, armor
		{
			if(item.isEquipped())
			{
				weaponSkills(player, null, itemType);
			}
			else
			{
				weaponSkills(player, itemType, null);
			}
		}
		
		updateSkillsForWeapon(player);
		
		checkShield(player);
		
		checkEventEnd(player.getInstanceId()); //TODO
	}
	
	private void checkShield(PlayerEventInfo player)
	{
		if(hasShieldEquipped(player))
		{
			if(!getPlayerData(player).hasShield())
			{
				weaponSkills(player, null, LuckyItem.SHIELD);
				getPlayerData(player).setHasShield(true);
			}
		}
		else
		{
			if(getPlayerData(player).hasShield())
			{
				weaponSkills(player, LuckyItem.SHIELD, null);
				getPlayerData(player).setHasShield(false);
			}
		}
		
		if(getPlayerData(player).getActiveWeapon() != null)
		{
			if(!hasWeaponEquipped(player, getPlayerData(player).getActiveWeapon())) // main weapon was unequipped somehow
			{
				weaponSkills(player, getPlayerData(player).getActiveWeapon(), null);
			}
		}
	}
	
	private boolean hasWeaponEquipped(PlayerEventInfo player, LuckyItem item)
	{
		ItemData i = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
		if (i.exists() && getWeaponType(i.getItemId()) == item)
			return true;
		return false;
	}
	
	private boolean hasShieldEquipped(PlayerEventInfo player)
	{
		ItemData shield = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_LHAND());
		if (shield.exists() && getWeaponType(shield.getItemId()) == LuckyItem.SHIELD)
			return true;
		return false;
	}
	
	private void weaponSkills(PlayerEventInfo player, LuckyItem oldWeapon, LuckyItem newWeapon)
	{
		if(_skillsForItems != null)
		{
			SkillData sk;
			
			if(oldWeapon != null)
			{
				if(_skillsForItems.containsKey(oldWeapon))
				{
					for(Entry<Integer, Integer> e : _skillsForItems.get(oldWeapon).entrySet())
					{
						sk = new SkillData(e.getKey(), e.getValue());
						if(sk.exists())
							player.removeSkill(sk.getId());
					}
				}
			}
			
			if(newWeapon != null)
			{
				if(_skillsForItems.containsKey(newWeapon))
				{
					for(Entry<Integer, Integer> e : _skillsForItems.get(newWeapon).entrySet())
					{
						sk = new SkillData(e.getKey(), e.getValue());
						if(sk.exists())
							player.addSkill(sk, false);
					}
				}
			}
			
			player.sendSkillList();
		}
	}
	
	protected boolean hasWeapon(PlayerEventInfo player, LuckyItem type)
	{
		return getPlayerData(player).hasWeapon(type);
	}
	
	private int random(int max)
	{
		return CallBack.getInstance().getOut().random(max);
	}
	
	private int random(int min, int max)
	{
		return CallBack.getInstance().getOut().random(min, max);
	}
	
	private enum EffectType 
	{
		// positive
		Score,
		ScoreFirework,
		ScoreLargeFirework,
		
		WindWalkTillDie,
		
		SkillAggression,
		SkillWhirlwind,
		SkillRush,
		
		BombShieldOneBomb,
		IncreaseCritRate,
		SpawnBonusChests,
		
		Weapon,
		
		// negative
		Laugh,
		Explode,
		BigHead,
		ParalyzeNoPoint,
		FearNoPoint,
		AggressiveBunny,
		
		TransformToBunny,
		TransformToPig,
		TransformToYeti,
		TransformToFrog
	}
	
	private synchronized ActionData selectAction(PlayerEventInfo player, NpcData npc, ChestType type)
	{
		EffectResult result;
		
		switch(type)
		{
			case CLASSIC:
			{
				if(random(100) < classicPositiveEffect) // 70% for positive effect
				{
					if(random(100) < classicObtainWeaponChance) // 3% for a new weapon
					{
						boolean given = false;
						
						if(random(3) == 0)
						{
							LuckyItem item = LuckyItem.HAMMER;
							
							if(!getPlayerData(player).hasWeapon(item))
							{
								giveWeapon(player, item, true, false);
								giveSkillForWeapon(player, item, _stunSkill, 20);
								
								player.screenMessage(LanguageEngine.getMsg("chests_obtainedWeapon_hammer"), getEventName(), false);
								given = true;
							}
						}
						
						if(!given)
						{
							LuckyItem item = LuckyItem.BOW;
							
							if(!getPlayerData(player).hasWeapon(item))
							{
								giveWeapon(player, item, true, false);
								
								player.screenMessage(LanguageEngine.getMsg("chests_obtainedWeapon_bow"), getEventName(), false);
								given = true;
							}
						}
						
						if(effect(player, npc, EffectType.Score, null, null).success)
							return new ActionData(true, true, true, false);
					}
					else // 97% for regular effect
					{
						int chance = random(100);
						
						if(chance < 2) // bonus chests 2%
						{
							if(effect(player, npc, EffectType.SpawnBonusChests, random(2, 3), true).success)
								return new ActionData(true, true, true, false);
						}
						else if(chance >= 2 && chance < 12) // bomb shield 10%
						{
							if(effect(player, npc, EffectType.BombShieldOneBomb, null, null).success)
								return new ActionData(true, true, true, false);
						}
						else if(chance >= 12 && chance < 16) // wind walk 4%
						{
							if(effect(player, npc, EffectType.WindWalkTillDie, null, null).success)
								return new ActionData(true, true, true, false);
						}
						else if(chance >= 16 && chance < 20) // x4 score - 4%
						{
							if(effect(player, npc, EffectType.ScoreLargeFirework, null, null).success)
								return new ActionData(true, true, true, false);
						}
						else if(chance >= 20 && chance < 30) // x2 score - 10%
						{
							if(effect(player, npc, EffectType.ScoreFirework, null, null).success)
								return new ActionData(true, true, true, false);
						}
						else // x1 score - 70%
						{
							if(effect(player, npc, EffectType.Score, null, null).success)
								return new ActionData(true, true, true, false);
						}
					}
				}
				else // negative effect (30%)
				{
					int chance = random(100);
					
					if(chance < 4) // 4% paralyze
					{
						result = effect(player, npc, EffectType.FearNoPoint, null, null); 
						if(result.success)
							return new ActionData(true, false, false, result.resetKillstreak);
					}
					else if(chance >= 4 && chance < 8) // 4% fear
					{
						result = effect(player, npc, EffectType.ParalyzeNoPoint, null, null);
						if(result.success)
							return new ActionData(true, false, false, result.resetKillstreak);
					}
					else if(chance >= 8 && chance < 18 && _allowTransformations) // 10% transform to frog
					{
						result = effect(player, npc, EffectType.TransformToFrog, null, null);
						if(result.success)
							return new ActionData(true, false, false, true);
					}
					else // 82% explode
					{
						result = effect(player, npc, EffectType.Explode, null, null);
						if(result.success)
							return new ActionData(false, false, false, result.resetKillstreak);
					}
				}
				
				break;
			}
			case SHABBY:
			{
				if(random(100) < shabbyTransformChance && _allowTransformations)
				{
					int chance = random(100);
					
					if(chance < 40)
					{
						if(effect(player, npc, EffectType.TransformToPig, null, null).success)
							return new ActionData(true, true, false, false);
					}
					else if(chance >= 40 && chance < 80)
					{
						if(effect(player, npc, EffectType.TransformToBunny, null, null).success)
							return new ActionData(true, true, false, false);
					}
					else
					{
						if(effect(player, npc, EffectType.TransformToYeti, null, null).success)
							return new ActionData(true, true, false, false);
					}
				}
				else if(random(100) < shabbyShieldChance)
				{
					if(effect(player, npc, EffectType.BombShieldOneBomb, null, null).success)
						return new ActionData(true, true, true, false);
				}
				else
				{
					if(effect(player, npc, EffectType.Score, null, null).success)
						return new ActionData(true, true, true, false);
				}
				
				break;
			}
			case LUXURIOUS:
			{
				if(random(100) < luxPositiveEffect) // 85% for positive effect
				{
					if(random(100) < luxObtainWeaponChance) //5% for a new weapon
					{
						boolean given = false;
						
						if(random(3) == 0)
						{
							LuckyItem item = LuckyItem.HAMMER;
							
							if(!getPlayerData(player).hasWeapon(item))
							{
								giveWeapon(player, item, true, false);
								giveSkillForWeapon(player, item, _stunSkill, 20);
								
								player.screenMessage(LanguageEngine.getMsg("chests_obtainedWeapon_hammer"), getEventName(), false);
								given = true;
							}
						}
						
						if(!given)
						{
							LuckyItem item = LuckyItem.BOW;
							
							if(!getPlayerData(player).hasWeapon(item))
							{
								giveWeapon(player, item, true, false);
								
								player.screenMessage(LanguageEngine.getMsg("chests_obtainedWeapon_bow"), getEventName(), false);
								given = true;
							}
						}
						
						if(effect(player, npc, EffectType.Score, null, null).success)
							return new ActionData(true, true, true, false);
					}
					else // 95% for regular effect
					{
						int chance = random(100);
						
						if(chance < 5) // bonus chests 5%
						{
							if(effect(player, npc, EffectType.SpawnBonusChests, random(2, 3), true).success)
								return new ActionData(true, true, true, false);
						}
						else if(chance >= 5 && chance < 10) // crit rate buff 5%
						{
							if(effect(player, npc, EffectType.IncreaseCritRate, 4, true).success)
								return new ActionData(true, true, true, false);
						}
						else if(chance >= 10 && chance < 14) // bomb shield 4%
						{
							if(effect(player, npc, EffectType.BombShieldOneBomb, null, null).success)
								return new ActionData(true, true, true, false);
						}
						else if(chance >= 14 && chance < 22) // wind walk 8%
						{
							if(effect(player, npc, EffectType.WindWalkTillDie, null, null).success)
								return new ActionData(true, true, true, false);
						}
						else if(chance >= 22 && chance < 30) // x4 score - 8%
						{
							if(effect(player, npc, EffectType.ScoreLargeFirework, null, null).success)
								return new ActionData(true, true, true, false);
						}
						else if(chance >= 30 && chance < 50) // x2 score - 20%
						{
							if(effect(player, npc, EffectType.ScoreFirework, null, null).success)
								return new ActionData(true, true, true, false);
						}
						else // x1 score - 50%
						{
							if(effect(player, npc, EffectType.Score, null, null).success)
								return new ActionData(true, true, true, false);
						}
					}
				}
				else
				{
					int chance = random(100);
					
					if(chance < 30 && _allowTransformations) // 30% transform to frog
					{
						result = effect(player, npc, EffectType.TransformToFrog, null, null);
						if(result.success)
							return new ActionData(true, false, false, true);
					}
					else // 70% explode
					{
						result = effect(player, npc, EffectType.Explode, null, null);
						if(result.success)
							return new ActionData(false, false, false, result.resetKillstreak);
					}
				}
				
				break;
			}
			case BOX:
			{
				switch(getPlayerData(player).getActiveWeapon())
				{
					case GLADIUS:
						if(effect(player, npc, EffectType.Weapon, LuckyItem.KNIGHTSWORD, null).success)
							return new ActionData(true, false, false, false);
					case DIRK:
						if(effect(player, npc, EffectType.Weapon, LuckyItem.DAGGER_CRAFTED, null).success)
							return new ActionData(true, false, false, false);
					case BOW:
						if(effect(player, npc, EffectType.Weapon, LuckyItem.LONGBOW, null).success)
							return new ActionData(true, false, false, false);
					case LANCE:
						if(effect(player, npc, EffectType.Weapon, LuckyItem.PIKE, null).success)
							return new ActionData(true, false, false, false);
					case HAMMER:
						if(effect(player, npc, EffectType.Weapon, LuckyItem.HEAVYHAMMER, null).success)
							return new ActionData(true, false, false, false);
					case ZWEIHANDER:
						if(effect(player, npc, EffectType.Weapon, LuckyItem.HEAVYSWORD, null).success)
							return new ActionData(true, false, false, false);
					case SABER:
						if(effect(player, npc, EffectType.Weapon, LuckyItem.SHIELD, null).success)
							return new ActionData(true, false, false, false);
					default:
						
						int chance = random(100);
						boolean given = false;
						if(chance < 5)
						{
							if(effect(player, npc, EffectType.Weapon, LuckyItem.REINFORCED_BOW, null).success)
							{
								given = true;
								return new ActionData(true, false, false, false);
							}
						}
						
						if(!given && chance >= 5 && chance < 15)
						{
							if(effect(player, npc, EffectType.Weapon, LuckyItem.SABER, null).success)
							{
								given = true;
								return new ActionData(true, false, false, false);
							}
						}
						
						if(!given)
						{
							if(effect(player, npc, EffectType.SpawnBonusChests, random(3, 4), false).success)
								return new ActionData(true, false, false, false);
						}
				}
				
				break;
			}
			case NEXUSED:
			{
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
				
				int chance = random(100);
				
				if(chance < nexusedMainEffectChance)
				{
					if(effect(player, npc, EffectType.SkillRush, null, null).success)
						return new ActionData(true, true, true, false);
					else
					{
						if(effect(player, npc, EffectType.ScoreLargeFirework, null, null).success)
							return new ActionData(true, true, true, false);
					}
				}
				else
				{
					if(effect(player, npc, EffectType.SpawnBonusChests, 3, false).success)
						return new ActionData(true, false, false, false);
				}
				
				break;
			}
		}
		
		return null;
	}
	
	private EffectResult effect(final PlayerEventInfo player, final NpcData npc, EffectType type, Object param, Object param2)
	{
		switch(type)
		{
			case Score:
			{
				getPlayerData(player).raiseScore(1);
				
				player.screenMessage(LanguageEngine.getMsg("chests_player_scored"), getEventName(), true);
				
				// update title
				if(player.isTitleUpdated())
				{
					player.setTitle(getTitle(player), true);
					player.broadcastTitleInfo();
				}
				
				// update Stats table
				setScoreStats(player, getPlayerData(player).getScore());
				
				CallbackManager.getInstance().playerScores(getEventType(), player, 1);
				
				return new EffectResult(true);
			}
			case ScoreFirework:
			{
				SkillData skill = new SkillData(5965, 1);
				if(skill.exists())
					player.broadcastSkillUse(null, null, skill.getId(), skill.getLevel());
				
				player.screenMessage(LanguageEngine.getMsg("chests_player_scored_x2"), getEventName(), true);
				
				getPlayerData(player).raiseScore(2);
				
				// update title
				if(player.isTitleUpdated())
				{
					player.setTitle(getTitle(player), true);
					player.broadcastTitleInfo();
				}
				
				// update Stats table
				setScoreStats(player, getPlayerData(player).getScore());
				
				CallbackManager.getInstance().playerScores(getEventType(), player, 2);
				
				return new EffectResult(true);
			}
			case ScoreLargeFirework:
			{
				SkillData skill = new SkillData(5966, 1);
				if(skill.exists())
					player.broadcastSkillUse(null, null, skill.getId(), skill.getLevel());
				
				player.screenMessage(LanguageEngine.getMsg("chests_player_scored_x4"), getEventName(), true);
				
				getPlayerData(player).raiseScore(4);
				
				// update title
				if(player.isTitleUpdated())
				{
					player.setTitle(getTitle(player), true);
					player.broadcastTitleInfo();
				}
				
				// update Stats table
				setScoreStats(player, getPlayerData(player).getScore());
				
				CallbackManager.getInstance().playerScores(getEventType(), player, 4);
				
				return new EffectResult(true);
			}
			case WindWalkTillDie:
			{
				final int id = 35018;
				final boolean raiseLevel = param2 == null ? false : (Boolean)param2;
				
				if(!getPlayerData(player).hasSkill(id, SkillType.TILL_DIE))
				{
					int level;
					
					if(raiseLevel)
						level = 1;
					else
						level = param == null ? 1 : (Integer)param;
					
					SkillData skill = new SkillData(id, level);
					if(skill.exists())
						player.getSkillEffects(skill.getId(), skill.getLevel());
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_windWalk", level), getEventName(), true);
					
					getPlayerData(player).addSkill(id, level, SkillType.TILL_DIE);
				}
				else
				{
					boolean addedBuff = false;
					
					if(raiseLevel && param != null && param instanceof Integer)
					{
						int newLevel = getPlayerData(player).getLevel(id, SkillType.TILL_DIE) + 1;
						int maxLevel = (Integer)param;
						
						if(newLevel <= maxLevel)
						{
							if(getPlayerData(player).getLevel(id, SkillType.TILL_DIE) < newLevel)
							{
								SkillData skill = new SkillData(id, newLevel);
								if(skill.exists())
								{
									player.removeBuff(skill.getId()); // remove old effect before adding new one
									player.getSkillEffects(skill.getId(), skill.getLevel());
									player.screenMessage(LanguageEngine.getMsg("chests_player_windWalk_upgrade", skill.getLevel()), getEventName(), false);
									
									getPlayerData(player).addSkill(id, newLevel, SkillType.TILL_DIE);
									addedBuff = true;
								}
							}
						}
					}
					
					if(!addedBuff)
						break;
				}
				
				getPlayerData(player).raiseScore(1);
				
				// update title
				if(player.isTitleUpdated())
				{
					player.setTitle(getTitle(player), true);
					player.broadcastTitleInfo();
				}
				
				// update Stats table
				setScoreStats(player, getPlayerData(player).getScore());
				
				CallbackManager.getInstance().playerScores(getEventType(), player, 1);
				
				return new EffectResult(true);
			}
			case SkillAggression:
			{
				final int id = _aggressionSkillId;
				if(!getPlayerData(player).hasSkill(id, SkillType.PERMANENT) && (getPlayerData(player).hasWeapon(LuckyItem.LANCE) || getPlayerData(player).hasWeapon(LuckyItem.PIKE)))
				{
					final int level = 1;
					
					SkillData skill = new SkillData(id, level);
					if(skill.exists())
						giveSkill(player, skill);
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_hateAura"), getEventName(), true);
					
					getPlayerData(player).addSkill(id, level, SkillType.PERMANENT);
					getPlayerData(player).raiseScore(1);
					
					// update title
					if(player.isTitleUpdated())
					{
						player.setTitle(getTitle(player), true);
						player.broadcastTitleInfo();
					}
					
					// update Stats table
					setScoreStats(player, getPlayerData(player).getScore());
					
					CallbackManager.getInstance().playerScores(getEventType(), player, 1);
					
					return new EffectResult(true);
				}
				
				break;
			}
			case SkillWhirlwind:
			{
				final int id = _whirlwindSkill;
				if(!getPlayerData(player).hasSkill(id, SkillType.PERMANENT) && (getPlayerData(player).hasWeapon(LuckyItem.LANCE) || getPlayerData(player).hasWeapon(LuckyItem.PIKE)))
				{
					final int level = 20;
					
					SkillData skill = new SkillData(id, level);
					if(skill.exists())
						giveSkill(player, skill);
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_whirlWind"), getEventName(), true);
					
					getPlayerData(player).addSkill(id, level, SkillType.PERMANENT);
					getPlayerData(player).raiseScore(1);
					
					// update title
					if(player.isTitleUpdated())
					{
						player.setTitle(getTitle(player), true);
						player.broadcastTitleInfo();
					}
					
					// update Stats table
					setScoreStats(player, getPlayerData(player).getScore());
					
					CallbackManager.getInstance().playerScores(getEventType(), player, 1);
					
					return new EffectResult(true);
				}
				
				break;
			}
			case SkillRush:
			{
				final int id = _rushSkill;
				if(!getPlayerData(player).hasSkill(id, SkillType.PERMANENT))
				{
					final int level = 1;
					
					SkillData skill = new SkillData(id, level);
					if(skill.exists())
						giveSkill(player, skill);
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_rushSkill"), getEventName(), true);
					
					getPlayerData(player).addSkill(id, level, SkillType.PERMANENT);
					getPlayerData(player).raiseScore(1);
					
					// update title
					if(player.isTitleUpdated())
					{
						player.setTitle(getTitle(player), true);
						player.broadcastTitleInfo();
					}
					
					// update Stats table
					setScoreStats(player, getPlayerData(player).getScore());
					
					CallbackManager.getInstance().playerScores(getEventType(), player, 1);
					
					return new EffectResult(true);
				}
				
				break;
			}
			case BombShieldOneBomb:
			{
				getPlayerData(player).raiseScore(1);
				
				if(getPlayerData(player).hasBombShield() <= 3)
				{
					getPlayerData(player).raiseBombShield(1);
					updateBombShield(player);
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_bombShield"), getEventName(), true);
					if(getPlayerData(player).hasBombShield() > 1)
						player.screenMessage(LanguageEngine.getMsg("chests_player_bombShield_info", getPlayerData(player).hasBombShield()), "Shield info", false);
				}
				
				// update title
				if(player.isTitleUpdated())
				{
					player.setTitle(getTitle(player), true);
					player.broadcastTitleInfo();
				}
				
				// update Stats table
				setScoreStats(player, getPlayerData(player).getScore());
				
				CallbackManager.getInstance().playerScores(getEventType(), player, 1);
				
				return new EffectResult(true);
			}
			case IncreaseCritRate:
			{
				final int id = 35019;
				final boolean raiseLevel = param2 == null ? false : (Boolean)param2;
				
				if(!getPlayerData(player).hasSkill(id, SkillType.PERMANENT))
				{
					int level;
					
					if(raiseLevel)
						level = 1;
					else
						level = param == null ? 1 : (Integer)param;
					
					SkillData skill = new SkillData(id, level);
					if(skill.exists())
						player.getSkillEffects(skill.getId(), skill.getLevel());
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_criticalRateBuff", level), getEventName(), true);
					
					getPlayerData(player).addSkill(id, level, SkillType.PERMANENT);
				}
				else
				{
					boolean addedBuff = false;
					
					if(raiseLevel && param != null && param instanceof Integer)
					{
						int newLevel = getPlayerData(player).getLevel(id, SkillType.PERMANENT) + 1;
						int maxLevel = (Integer)param;
						
						if(newLevel <= maxLevel)
						{
							if(getPlayerData(player).getLevel(id, SkillType.PERMANENT) < newLevel)
							{
								SkillData skill = new SkillData(id, newLevel);
								if(skill.exists())
								{
									player.removeBuff(skill.getId()); // remove old effect before adding new one
									player.getSkillEffects(skill.getId(), skill.getLevel());
									player.screenMessage(LanguageEngine.getMsg("chests_player_criticalRateBuff_levelUp", skill.getLevel()), getEventName(), false);
									
									getPlayerData(player).addSkill(id, newLevel, SkillType.PERMANENT);
									addedBuff = true;
								}
							}
						}
					}
					
					if(!addedBuff)
						break;
				}
				
				getPlayerData(player).raiseScore(1);
				
				// update title
				if(player.isTitleUpdated())
				{
					player.setTitle(getTitle(player), true);
					player.broadcastTitleInfo();
				}
				
				// update Stats table
				setScoreStats(player, getPlayerData(player).getScore());
				
				CallbackManager.getInstance().playerScores(getEventType(), player, 1);
				
				return new EffectResult(true);
			}
			case SpawnBonusChests:
			{
				boolean score = param2 != null && param2 instanceof Boolean ? (Boolean)param2 : true;
				
				if(score)
				{
					// score
					getPlayerData(player).raiseScore(1);
					
					// update title
					if(player.isTitleUpdated())
					{
						player.setTitle(getTitle(player), true);
						player.broadcastTitleInfo();
					}
					
					// update Stats table
					setScoreStats(player, getPlayerData(player).getScore());
					
					CallbackManager.getInstance().playerScores(getEventType(), player, 1);
					
				}
				
				//TODO set chests' id; set chests' positive chance, etc. - MAKE THEM BONUS!
				
				//.. and spawn chests
				final int instanceId = player.getInstanceId();
				final int count = param != null && param instanceof Integer ? (Integer) param : (random(2,5)); 
				
				Loc loc;
				NpcData newChest;
				
				int chestId = _classicChestId; //TODO select chest ID
				for (int i = 0; i < count; i++)
				{
					loc = new Loc(player.getX(), player.getY(), player.getZ());
					loc.addRadius(param2 != null && param2 instanceof Integer ? (Integer)param2 : 150); 
					
					newChest = spawnNPC(loc.getX(), loc.getY(), loc.getZ(), chestId, instanceId, null, null);
					newChest.broadcastSkillUse(newChest, newChest, 5965, 1);
					
					getEventData(instanceId)._chests.get(getType(chestId)).add(newChest);
				}
				
				return new EffectResult(true);
			}
			case Weapon:
			{
				if(param != null && param instanceof LuckyItem)
				{
					LuckyItem item = (LuckyItem) param;
					
					if(getPlayerData(player).hasWeapon(item))
						return new EffectResult(false);
						
					Boolean equip = (param != null && param instanceof Boolean) ? (Boolean)param : null;
					
					giveWeapon(player, item, equip != null ? equip : true, false);
					return new EffectResult(true);
				}
				
				break;
			}
			// negative effects
			case Laugh:
			{
				// possible only if NPC is not dead (it does this while the player hits it)
				if(!npc.isDead())
				{
					String s = _jokerChestTexts[CallBack.getInstance().getOut().random(_jokerChestTexts.length)];
					npc.creatureSay(0, _jokerChestName, s);
					player.screenMessage(s, getEventName(), true);
					ChestType chestType = getType(npc.getNpcId());
					
					try
					{
						// remove NPC from the list
						for(NpcData ch : getEventData(player.getInstanceId())._chests.get(chestType))
						{
							if(ch != null && ch.getObjectId() == npc.getObjectId())
							{
								synchronized(getEventData(player.getInstanceId())._chests)
								{
									getEventData(player.getInstanceId())._chests.get(chestType).remove(ch);
								}
								
								checkEventEnd(player.getInstanceId());
								
								break;
							}
						}
					}
					catch (Exception e)
					{
					}
					
					npc.deleteMe();
					return new EffectResult(true);
				}
				
				break;
			}
			case Explode:
			{
				npc.broadcastSkillUse(npc, player.getCharacterData(), 5430, 1);
				//npc.broadcastLaunchSkill(npc, 5430, 1, new ObjectData[]{player.getCharacterData()});
			
				boolean wasProtected = false;
				
				if(getPlayerData(player).hasWeapon(LuckyItem.BOW) || getPlayerData(player).hasWeapon(LuckyItem.LONGBOW) || getPlayerData(player).hasWeapon(LuckyItem.REINFORCED_BOW))
				{
					wasProtected = true;
				}
				else if(getPlayerData(player).hasShield())
				{
					player.screenMessage(LanguageEngine.getMsg("chests_player_itemShieldSuccess"), getEventName(), true);
					wasProtected = true;
				}
				else if(getPlayerData(player).hasBombShield() > 0)
				{
					getPlayerData(player).decreaseBombShield(1);
					updateBombShield(player);
					player.screenMessage(LanguageEngine.getMsg("chests_player_bombShieldSuccess"), getEventName(), true);
					
					wasProtected = true;
					
					if(getPlayerData(player).hasBombShield() > 0)
						player.screenMessage(LanguageEngine.getMsg("chests_player_bombShield_info2", getPlayerData(player).hasBombShield()), getEventName(), false);
				}
				else if(getPlayerData(player).hasDeathstreakShield())
				{
					player.screenMessage(LanguageEngine.getMsg("chests_player_deathStreakShieldSuccess"), getEventName(), true);
					wasProtected = true;
				}
				else
					player.doDie();
				
				CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						npc.deleteMe();
					}
				}, 200);
				
				
				if(wasProtected)
					return new EffectResult(true, false, false, _explosionShieldResetKillstreak);
				else
					return new EffectResult(true, false, false, true);
			}
			case BigHead:
			{
				if(!getPlayerData(player).hasBigHead())
				{
					getPlayerData(player).setHasBigHead(true);
					player.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_BIG_HEAD());
					
					if(player.isTitleUpdated())
					{
						player.setTitle("owned", true);
						player.broadcastTitleInfo();
					}
					
					CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
					{
						@Override
						public void run()
						{
							if(player != null && player.isOnline())
							{
								player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_BIG_HEAD());
								
								getPlayerData(player).setHasBigHead(false);
								
								// update title
								if(player.isTitleUpdated())
								{
									player.setTitle(getTitle(player), true);
									player.broadcastTitleInfo();
								}
							}
						}
					}, 60000);
					
					return new EffectResult(true);
				}
				
				break;
			}
			case ParalyzeNoPoint:
			{
				boolean wasProtected = false;
				
				if(getPlayerData(player).hasBombShield() > 0 && _bombShieldProtectsParalyzation)
				{
					getPlayerData(player).decreaseBombShield(1);
					updateBombShield(player);
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_bombShieldSuccess_curse"), getEventName(), true);
					
					wasProtected = true;
					
					if(getPlayerData(player).hasBombShield() > 0)
						player.screenMessage(LanguageEngine.getMsg("chests_player_bombShield_info2", getPlayerData(player).hasBombShield()), getEventName(), false);
				}
				else if(getPlayerData(player).hasDeathstreakShield())
				{
					player.screenMessage(LanguageEngine.getMsg("chests_player_deathStreakShieldSuccess_curse"), getEventName(), true);
					
					wasProtected = true;
				}
				else
				{
					player.screenMessage(LanguageEngine.getMsg("chests_player_paralyzed"), getEventName(), true);
					player.getSkillEffects(35016, 1);
				}
				
				if(wasProtected)
					return new EffectResult(true, false, false, false);
				else
					return new EffectResult(true, false, false, true);
			}
			case FearNoPoint:
			{
				boolean wasProtected = false;
				
				npc.setName(LanguageEngine.getMsg("chests_player_horrifyingChest"));
				npc.broadcastNpcInfo();
				
				if(getPlayerData(player).hasBombShield() > 0 && _bombShieldProtectsFear)
				{
					getPlayerData(player).decreaseBombShield(1);
					updateBombShield(player);
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_bombShieldSuccess_curse"), getEventName(), true);
					
					wasProtected = true;
					
					if(getPlayerData(player).hasBombShield() > 0)
						player.screenMessage(LanguageEngine.getMsg("chests_player_bombShield_info2", getPlayerData(player).hasBombShield()), getEventName(), false);
				}
				else if(getPlayerData(player).hasDeathstreakShield())
				{
					player.screenMessage(LanguageEngine.getMsg("chests_player_deathStreakShieldSuccess_curse"), getEventName(), true);
					
					wasProtected = true;
				}
				else
				{
					player.screenMessage("!!!", getEventName(), true);
					
					player.getSkillEffects(35017, 1); //slow
					player.getSkillEffects(1092, 1); // fear
					
					if(_enableFearFireworking)
					{
						for(int i = 1; i <= 4; i++)
						{
							CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
							{
								@Override
								public void run()
								{
									if(player.isOnline() && getMatch(player.getInstanceId()).isActive() && !player.isDead() && player.isAfraid())
									{
										player.broadcastSkillUse(null, null, 5965, 1);
									}
								}
							}, i*2000);
						}
					}
				}
				
				if(wasProtected)
					return new EffectResult(true, false, false, false);
				else
					return new EffectResult(true, false, false, true);
			}
			case AggressiveBunny:
			{
				break;
			}
			case TransformToBunny:
			{
				final int transformLasts = _bunnyTransformDuration;
				final int transformId = _bunnyTransformId;
				final int rabbitStatsSkill = 35021;
				if(transformId > 0 && getPlayerData(player).getTransformation() == null)
				{
					getPlayerData(player).setTransformed(TransformType.BUNNY);
					transformPlayer(player, transformId);
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_transform_bunny1"), getEventName(), true);
					player.screenMessage(LanguageEngine.getMsg("chests_player_transform_bunny2"), getEventName(), false);
					
					player.sendMessage("* " + LanguageEngine.getMsg("chests_player_transform_bunny3", transformLasts));
					
					//player.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
					
					player.setTitle(getTitle(player), true);
					player.broadcastTitleInfo();
					
					sysMsgToAll(player.getInstanceId(), LanguageEngine.getMsg("chests_player_transform_bunny_announce", player.getPlayersName()));
					
					// give rabbit skills
					SkillData skill = new SkillData(rabbitStatsSkill, 1);
					if(skill.exists())
						player.addSkill(skill, false);
					
					getPlayerData(player).addSkill(rabbitStatsSkill, 1, SkillType.TRANSFORM);
					player.sendSkillList();
					
					player.setCurrentHp(player.getMaxHp());
					player.setCurrentMp(player.getMaxMp());
					player.setCurrentCp(player.getMaxCp());
					
					scheduleUntransform(player, TransformType.BUNNY, transformLasts * 1000);
					
					return new EffectResult(true);
				}
				
				break;
			}
			case TransformToFrog:
			{
				final int transformLasts = _frogTransformDuration;
				final int transformId = _frogTransformId;
				final int frogStatsSkill = 35022;
				if(transformId > 0 && getPlayerData(player).getTransformation() == null)
				{
					getPlayerData(player).setTransformed(TransformType.FROG);
					transformPlayer(player, transformId);
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_transform_frog1"), getEventName(), true);
					player.screenMessage(LanguageEngine.getMsg("chests_player_transform_frog2"), getEventName(), false);
					
					player.sendMessage("* " + LanguageEngine.getMsg("chests_player_transform_frog3", transformLasts));
					
					player.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
					
					player.setTitle(getTitle(player), true);
					player.broadcastTitleInfo();
					
					sysMsgToAll(player.getInstanceId(), LanguageEngine.getMsg("chests_player_transform_frog_announce", player.getPlayersName()));
					
					// give frog skills
					SkillData skill = new SkillData(frogStatsSkill, 1);
					if(skill.exists())
						player.addSkill(skill, false);
					
					getPlayerData(player).addSkill(frogStatsSkill, 1, SkillType.TRANSFORM);
					player.sendSkillList();
					
					player.setCurrentHp(player.getMaxHp());
					player.setCurrentMp(player.getMaxMp());
					player.setCurrentCp(player.getMaxCp());
					
					startFrogTask(player, transformLasts * 1000);
					scheduleUntransform(player, TransformType.FROG, transformLasts * 1000);
					
					return new EffectResult(true);
				}
				
				break;
			}
			case TransformToPig:
			{
				final int transformLasts = _pigTransformDuration;
				final int transformId = _pigTransformId;
				final int pigStatsSkill = 35023;
				if(transformId > 0 && getPlayerData(player).getTransformation() == null)
				{
					getPlayerData(player).setTransformed(TransformType.PIG);
					transformPlayer(player, transformId);
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_transform_pig1"), getEventName(), true);
					player.screenMessage(LanguageEngine.getMsg("chests_player_transform_pig2"), getEventName(), false);
					
					player.sendMessage("* " + LanguageEngine.getMsg("chests_player_transform_pig3", transformLasts));
					
					player.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
					
					player.setTitle(getTitle(player), true);
					player.broadcastTitleInfo();
					
					sysMsgToAll(player.getInstanceId(), LanguageEngine.getMsg("chests_player_transform_pig_announce", player.getPlayersName()));
					
					// give pig skills
					SkillData skill = new SkillData(pigStatsSkill, 1);
					if(skill.exists())
						player.addSkill(skill, false);
					
					getPlayerData(player).addSkill(pigStatsSkill, 1, SkillType.TRANSFORM);
					player.sendSkillList();
					
					player.setCurrentHp(player.getMaxHp());
					player.setCurrentMp(player.getMaxMp());
					player.setCurrentCp(player.getMaxCp());
					
					scheduleUntransform(player, TransformType.PIG, transformLasts * 1000);
					
					return new EffectResult(true);
				}
				
				break;
			}
			case TransformToYeti:
			{
				final int transformLasts = _yetiTransformDuration;
				final int transformId = _yetiTransformId;
				final int yetiStatsSkill = 35024;
				if(transformId > 0 && getPlayerData(player).getTransformation() == null)
				{
					getPlayerData(player).setTransformed(TransformType.YETI);
					transformPlayer(player, transformId);
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_transform_yeti1"), getEventName(), true);
					player.screenMessage(LanguageEngine.getMsg("chests_player_transform_yeti2"), getEventName(), false);
					
					player.sendMessage("* " + LanguageEngine.getMsg("chests_player_transform_yeti3", transformLasts));
					
					player.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
					
					player.setTitle(getTitle(player), true);
					player.broadcastTitleInfo();
					
					announce(player.getInstanceId(), LanguageEngine.getMsg("chests_player_transform_yeti_announce", player.getPlayersName()));
					
					// give yeti skills
					SkillData skill = new SkillData(yetiStatsSkill, 1);
					if(skill.exists())
						player.addSkill(skill, false);
					
					getPlayerData(player).addSkill(yetiStatsSkill, 1, SkillType.TRANSFORM);
					player.sendSkillList();
					
					player.setCurrentHp(player.getMaxHp());
					player.setCurrentMp(player.getMaxMp());
					player.setCurrentCp(player.getMaxCp());
					
					startYetiTask(player, transformLasts * 1000);
					scheduleUntransform(player, TransformType.YETI, transformLasts * 1000);
					
					return new EffectResult(true);
				}
				
				break;
			}
		}
		
		return new EffectResult(false);
	}
	
	protected void checkEventEnd(int instance)
	{
		boolean empty = true;
				
		for(FastList<NpcData> e : getEventData(instance)._chests.values())
		{
			if(!e.isEmpty())
			{
				empty = false;
				break;
			}
		}
		
		if(empty)
		{
			announce("All chests were killed. Event has ended.");
			endInstance(instance, true, false, false);
		}
	}
	
	private void scheduleUntransform(final PlayerEventInfo player, final TransformType type, final int delay)
	{
		CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				if(player.isOnline() && !player.isDead() && getPlayerData(player) != null && getPlayerData(player).getTransformation() == type)
				{
					untransformPlayer(player);
					
					player.setCurrentHp(player.getMaxHp());
					player.setCurrentMp(player.getMaxMp());
					player.setCurrentCp(player.getMaxCp());
				}
			}
		}, delay);
	}
	
	private void startYetiTask(final PlayerEventInfo player, int duration)
	{
		final int interval = 2500;
		
		for(int i = interval; i < duration; i += interval)
		{
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					if(player.isOnline() && !player.isDead() && getPlayerData(player).getTransformation() == TransformType.YETI)
					{
						player.setTitle(getTitle(player), true);
						player.broadcastTitleInfo();
					}
				}
			}, i);
		}
	}
	
	private void startFrogTask(final PlayerEventInfo player, int duration)
	{
		final int interval = 3000;
		
		for(int i = interval; i < duration; i += interval)
		{
			CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					if(player.isOnline() && !player.isDead() && getPlayerData(player).getTransformation() == TransformType.FROG)
					{
						player.broadcastSkillUse(null, null, 5965, 1);
					}
				}
			}, i);
		}
	}
	
	private void transformPlayer(PlayerEventInfo player, int transformId)
	{
		player.untransform(true);
		
		player.transform(transformId);
	}
	
	private void untransformPlayer(PlayerEventInfo player)
	{
		for(Entry<Integer, Integer> e : getPlayerData(player).getSkills(SkillType.TRANSFORM).entrySet())
		{
			SkillData skill = new SkillData(e.getKey(), e.getValue());
			if(skill.exists())
				removeSkill(player, skill, SkillType.TRANSFORM, true);
		}
		
		getPlayerData(player).removeSkills(SkillType.TRANSFORM);
		
		getPlayerData(player).setTransformed(null);
		
		player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
		
		player.untransform(true);
		
		player.setTitle(getTitle(player), true);
		player.broadcastTitleInfo();
	}
	
	@Override
	public int allowTransformationSkill(PlayerEventInfo player, SkillData skill)
	{
		if(getPlayerData(player).getSkills(SkillType.TRANSFORM).containsKey(skill.getId()))
			return 1;
		return 0;
	}
	
	private void updateBombShield(PlayerEventInfo player)
	{
		if(getPlayerData(player).hasDeathstreakShield())
		{
			player.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_S_INVINCIBLE());
		}
		else
		{
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_S_INVINCIBLE());
			
			if(getPlayerData(player).hasBombShield() > 0)
				player.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_INVULNERABLE());
			else
				player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_INVULNERABLE());
		}
	}
	
	private int getItemId(LuckyItem type)
	{
		int itemId = -1;
		switch(type)
		{
			case GLADIUS:
				return _gladiusItemId;
			case DIRK:
				return _dirkItemId;
			case BOW:
				return _bowItemId;
			case LANCE:
				return _lanceItemId;
			case HAMMER:
				return _hammerItemId;
			case ZWEIHANDER:
				return _zweihanderItemId;
			case SHIELD:
				return _shieldItemId;
			case KNIGHTSWORD:
				return _knightswordItemId;
			case SABER:
				return _saberItemId;
			case LONGBOW:
				return _longBowItemId;
			case REINFORCED_BOW:
				return _reinforcedBowItemId;
			case PIKE:
				return _pikeItemId;
			case DAGGER_CRAFTED:
				return _craftedDaggerItemId;
			case HEAVYSWORD:
				return _heavyswordItemId;
			case HEAVYHAMMER:
				return _heavyHammerItemId;
		}
		return itemId;
	}
	
	private LuckyItem getWeaponType(int itemId)
	{
		for(LuckyItem t : LuckyItem.values())
		{
			if(getItemId(t) == itemId)
				return t;
		}
		return null;
	}
	
	protected void spawnChests(int instance)
	{
		EventSpawn sp;
		Loc loc;
		NpcData npc;
		
		// Spawn classic chests
		int count = CallBack.getInstance().getOut().random(_classicChestsCountMin, _classicChestsCountMax);
		
		/**/ if(NexusLoader.detailedDebug) print("Event: spawning " + count + " classic chests");
		
		for (int i = 0; i < count; i++)
		{
			sp = getSpawn(SpawnType.Chest, -1);
			loc = sp.getLoc();
			loc.addRadius(sp.getRadius()); 
			
			npc = spawnNPC(loc.getX(), loc.getY(), loc.getZ(), _classicChestId, instance, null, null);
			
			getEventData(instance)._chests.get(ChestType.CLASSIC).add(npc);
		}
		
		
		// Spawn shabby chests
		if(_shabbyChestEnabled)
		{
			count = CallBack.getInstance().getOut().random(_shabbyChestsCountMin, _shabbyChestsCountMax);
			
			/**/ if(NexusLoader.detailedDebug) print("Event: spawning " + count + " shabby chests");
			
			for (int i = 0; i < count; i++)
			{
				sp = getSpawn(SpawnType.Chest, -1);
				loc = sp.getLoc();
				loc.addRadius(sp.getRadius()); 
				
				npc = spawnNPC(loc.getX(), loc.getY(), loc.getZ(), _shabbyChestId, instance, null, null);
				
				getEventData(instance)._chests.get(ChestType.SHABBY).add(npc);
			}
		}
		
		
		// Spawn luxurious chests
		if(_luxuriousChestEnabled)
		{
			count = CallBack.getInstance().getOut().random(_luxuriousChestsCountMin, _luxuriousChestsCountMax);
			
			/**/ if(NexusLoader.detailedDebug) print("Event: spawning " + count + " luxurious chests");
			
			for (int i = 0; i < count; i++)
			{
				sp = getSpawn(SpawnType.Chest, -1);
				loc = sp.getLoc();
				loc.addRadius(sp.getRadius()); 
				
				npc = spawnNPC(loc.getX(), loc.getY(), loc.getZ(), _luxuriousChestId, instance, null, null);
				
				getEventData(instance)._chests.get(ChestType.LUXURIOUS).add(npc);
			}
		}
		
		
		// Spawn box chests
		if(_boxChestEnabled)
		{
			count = CallBack.getInstance().getOut().random(_boxChestsCountMin, _boxChestsCountMax);
			
			/**/ if(NexusLoader.detailedDebug) print("Event: spawning " + count + " box chests");
			
			for (int i = 0; i < count; i++)
			{
				sp = getSpawn(SpawnType.Chest, -1);
				loc = sp.getLoc();
				loc.addRadius(sp.getRadius()); 
				
				npc = spawnNPC(loc.getX(), loc.getY(), loc.getZ(), _boxChestId, instance, null, null);
				
				getEventData(instance)._chests.get(ChestType.BOX).add(npc);
			}
		}
		
		
		// Spawn nexused chests
		if(_nexusedChestEnabled)
		{
			count = CallBack.getInstance().getOut().random(_nexusedChestsCountMin, _nexusedChestsCountMax);
			
			/**/ if(NexusLoader.detailedDebug) print("Event: spawning " + count + " nexused chests");
			
			for (int i = 0; i < count; i++)
			{
				sp = getSpawn(SpawnType.Chest, -1);
				loc = sp.getLoc();
				loc.addRadius(sp.getRadius()); 
				
				npc = spawnNPC(loc.getX(), loc.getY(), loc.getZ(), _nexusedChestId, instance, null, null);
				
				getEventData(instance)._chests.get(ChestType.NEXUSED).add(npc);
			}
		}
	}
	
	protected void unspawnChests(int instance)
	{
		if(getEventData(instance)._chests == null)
			return;
		
		for(Entry<ChestType, FastList<NpcData>> e : getEventData(instance)._chests.entrySet())
		{
			for(NpcData ch : e.getValue())
			{
				if(ch != null)
					ch.deleteMe();
			}
		}
		
		getEventData(instance)._chests.clear();
		getEventData(instance)._chests = null;
	}
	
	protected void preparePlayers(int instanceId)
	{
		SkillData skill;
		for(PlayerEventInfo player : getPlayers(instanceId))
		{
			giveWeapon(player, LuckyItem.GLADIUS, true, false);
			
			if(_skillsForAll != null)
			{
				for(Entry<Integer, Integer> e : _skillsForAll.entrySet())
				{
					skill = new SkillData(e.getKey(), e.getValue());
					
					if(skill.exists())
						player.addSkill(skill, false);
				}
				
				player.sendSkillList();
			}
			
			player.untransform(true);
			
			player.removeBuffs();
			player.removeCubics();
			player.removeSummon();
			
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
		}
	}
	
	protected void clearShortcuts(int instanceId)
	{
		for(PlayerEventInfo player : getPlayers(instanceId))
		{
			player.removeOriginalShortcuts();
		}
	}
	
	protected void restorePlayers(int instanceId)
	{
		for(PlayerEventInfo player : getPlayers(instanceId))
		{
			for(LuckyItem t : LuckyItem.values())
			{
				removeWeapon(player, t);
			}
			
			SkillData skill;
			
			if(_skillsForAll != null)
			{
				for(Entry<Integer, Integer> e : _skillsForAll.entrySet())
				{
					skill = new SkillData(e.getKey(), e.getValue());
					if(skill.exists())
						player.removeSkill(skill.getId());
				}
				
			}
			
			for(Entry<SkillType, Map<Integer, Integer>> customSkills : getPlayerData(player).getAllSkills().entrySet())
			{
				for(Entry<Integer, Integer> e : customSkills.getValue().entrySet())
				{
					skill = new SkillData(e.getKey(), e.getValue());
					if(skill.exists())
						removeSkill(player, skill, customSkills.getKey(), false);
				}
			}
			
			for(Entry<LuckyItem, Map<Integer, Integer>> customSkills : getPlayerData(player).getAllSkillsForWeapons().entrySet())
			{
				for(Entry<Integer, Integer> e : customSkills.getValue().entrySet())
				{
					skill = new SkillData(e.getKey(), e.getValue());
					if(skill.exists())
					{
						player.removeBuff(skill.getId());
						player.removeSkill(skill.getId());
					}
				}
			}
			
			player.sendSkillList();
			
			player.restoreOriginalShortcuts();
			
			untransformPlayer(player);
			
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
		}
	}
	
	@Override
	public void onEventEnd()
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: onEventEnd()"); 
		
		int minScore = getInt("scoreForReward");
		rewardAllPlayers(-1, minScore, 0);
	}

	@Override
	protected String getScorebar(int instance)
	{
		TextBuilder tb = new TextBuilder();
		
		if(_scorebarInfo.equals("TopScore"))
		{
			int top = 0;
			for(PlayerEventInfo player : getPlayers(instance))
			{
				if(getPlayerData(player).getScore() > top)
					top = getPlayerData(player).getScore();
			}
			
			tb.append(LanguageEngine.getMsg("chests_scorebar_topScore") + " " + top + " ");
		}
		else
		{
			int count = 0;
			for(FastList<NpcData> e : getEventData(instance)._chests.values())
				count += e.size();
			
			tb.append(LanguageEngine.getMsg("chests_scorebar_chestsLeft") + " " + count);
		}
		
		tb.append("   " + LanguageEngine.getMsg("event_scorebar_time", _matches.get(instance).getClock().getTime()));
		
		return tb.toString();
	}

	@Override
	protected String getTitle(PlayerEventInfo pi)
	{
		if(_hideTitles)
			return "";
		
		if(pi.isAfk())
			return "AFK";
		
		if(getPlayerData(pi).getTransformation() != null)
		{
			switch(getPlayerData(pi).getTransformation())
			{
				case BUNNY:
					return LanguageEngine.getMsg("chests_player_transform_bunny_title");
				case FROG:
					return LanguageEngine.getMsg("chests_player_transform_frog_title");
				case PIG:
					return LanguageEngine.getMsg("chests_player_transform_pig_title");
				case YETI:
					int hp = (int) Math.round(pi.getCurrentHp() / pi.getMaxHp() * 100);
					return LanguageEngine.getMsg("chests_player_transform_yeti_title", hp); 
			}
		}
		
		return "Score: " + getPlayerData(pi).getScore();
	}
	
	private ChestType getType(int id)
	{
		if(id == _classicChestId)
			return ChestType.CLASSIC;
		if(id == _shabbyChestId)
			return ChestType.SHABBY;
		if(id == _luxuriousChestId)
			return ChestType.LUXURIOUS;
		if(id == _boxChestId)
			return ChestType.BOX;
		if(id == _nexusedChestId)
			return ChestType.NEXUSED;
		else 
			return null;
	}
	
	private void resetDeathStreak(PlayerEventInfo player)
	{
		if(getPlayerData(player).hasDeathstreakShield())
		{
			getPlayerData(player).removeDeathstreakShield();
			player.sendMessage(LanguageEngine.getMsg("chests_player_deathStreakShieldRemoved"));
			updateBombShield(player);
		}
		
		// remove TILL_KILL skills
		for(Entry<Integer, Integer> e : getPlayerData(player).getSkills(SkillType.TILL_KILL).entrySet())
		{
			SkillData skill = new SkillData(e.getKey(), e.getValue());
			if(skill.exists())
			{
				player.screenMessage(LanguageEngine.getMsg("chests_player_buffRemoved", skill.getName(), skill.getLevel()), getEventName(), false);
				removeSkill(player, skill, SkillType.TILL_KILL, true);
			}
		}
		getPlayerData(player).removeSkills(SkillType.TILL_KILL);
		
		if(getPlayerData(player).getDeathStreak() > 0)
			player.sendMessage(LanguageEngine.getMsg("chests_player_deathstreakReset"));
		
		getPlayerData(player).resetDeathStreak();
	}
	
	private void addKillStreak(PlayerEventInfo player, NpcData npc)
	{
		getPlayerData(player).addKillStreak(1);
		
		int streak = getPlayerData(player).getKillStreak();
		int rnd;
		
		if(streak >= 3)
		{
			switch(streak)
			{
				case 3:
					
					if(criticalRateBuff(player, 1))
					{
						player.screenMessage(LanguageEngine.getMsg("chests_player_killStreak_killedInRow", 3), getEventName(), true);
					}
					
					break;
				case 5:
					
					getPlayerData(player).raiseBombShield(1);
					updateBombShield(player);
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_killStreak_bombShield"), getEventName(), true);
					
					if(getPlayerData(player).hasBombShield() > 1)
						player.screenMessage(LanguageEngine.getMsg("chests_player_bombShield_info", getPlayerData(player).hasBombShield()), "Shield info", false);
					
					break;
				case 6:
					
					rnd = CallBack.getInstance().getOut().random(100);
					boolean given = false;
					
					if(rnd < 30) // 30% lance
					{
						LuckyItem item = LuckyItem.LANCE;
						
						if(!getPlayerData(player).hasWeapon(item))
						{
							giveWeapon(player, item, true, false);
							giveSkillForWeapon(player, item, _whirlwindSkill, 20);
							given = true;
						}
					}
					
					if(!given && rnd >= 30 && rnd < 80) // 50% dirk
					{
						LuckyItem item = LuckyItem.DIRK;
						
						if(!getPlayerData(player).hasWeapon(item))
						{
							giveWeapon(player, item, true, false);
							giveSkillForWeapon(player, item, 30, 20);
							given = true;
						}
					}
					
					if(!given)// 20% saber
					{
						LuckyItem item = LuckyItem.SABER;
						
						if(!getPlayerData(player).hasWeapon(item))
						{
							giveWeapon(player, item, true, false);
							given = true;
						}
					}
					
					if(given)
					{
						player.screenMessage(LanguageEngine.getMsg("chests_player_killStreak_weapon"), getEventName(), true);
					}
					
					break;
				case 8:
					
					if(criticalRateBuff(player, 2))
					{
						player.screenMessage(LanguageEngine.getMsg("chests_player_killStreak_killedInRow2", 8), getEventName(), true);
					}
					
					break;
				case 10:
					
					boolean givenSword = false;
					
					LuckyItem item = LuckyItem.ZWEIHANDER;
					
					if(!getPlayerData(player).hasWeapon(item))
					{
						giveWeapon(player, item, true, false);
						givenSword = true;
					}
					
					if(givenSword)
						player.screenMessage(LanguageEngine.getMsg("chests_player_killStreak_sword"), getEventName(), true);
					
					break;
				case 13:
					
					boolean givenBow = false;
					
					LuckyItem bow = LuckyItem.REINFORCED_BOW;
					
					if(!getPlayerData(player).hasWeapon(bow))
					{
						giveWeapon(player, bow, true, false);
						givenSword = true;
					}
					
					if(givenBow)
						player.screenMessage(LanguageEngine.getMsg("chests_player_killStreak_bow"), getEventName(), true);
					
					break;
				case 15:
					
					boolean givenShield = false;
					
					LuckyItem shield = LuckyItem.SHIELD;
					
					if(!getPlayerData(player).hasWeapon(shield))
					{
						giveWeapon(player, shield, true, false);
						givenShield = true;
					}
					
					if(givenShield)
					{
						player.screenMessage(LanguageEngine.getMsg("chests_player_killStreak_itemShield"), getEventName(), true);
						player.screenMessage(LanguageEngine.getMsg("chests_player_killStreak_killedInRow3", streak), getEventName(), false);
					}
					
					break;
				default:
					
					player.screenMessage(LanguageEngine.getMsg("chests_player_killStreak_killedInRow", streak), getEventName(), false);
			}
		}
		
		
		
		/*final int killStreakSkill = 35025;
		int level = 1; 
		
		getPlayerData(player).addKillStreak(1);
		
		SkillData skill = new SkillData(killStreakSkill, level);
		if(skill.exists())
			player.getSkillEffects(skill.getId(), skill.getLevel());
		
		getPlayerData(player).addSkill(killStreakSkill, level, SkillType.KILLSTREAK);*/
	}
	
	private boolean criticalRateBuff(PlayerEventInfo player, int maxLevel)
	{
		final int id = 35019;
		
		if(!getPlayerData(player).hasSkill(id, SkillType.PERMANENT))
		{
			int level = 1;
			
			SkillData skill = new SkillData(id, level);
			if(skill.exists())
			{
				player.getSkillEffects(skill.getId(), skill.getLevel());
				
				player.screenMessage(LanguageEngine.getMsg("chests_player_criticalRateBuff", level), getEventName(), true);
				
				getPlayerData(player).addSkill(id, level, SkillType.PERMANENT);
				return true;
			}
			else return false;
		}
		else
		{
			boolean addedBuff = false;
			
			int newLevel = getPlayerData(player).getLevel(id, SkillType.PERMANENT) + 1;
				
			if(newLevel <= maxLevel)
			{
				if(getPlayerData(player).getLevel(id, SkillType.PERMANENT) < newLevel)
				{
					SkillData skill = new SkillData(id, newLevel);
					if(skill.exists())
					{
						player.removeBuff(skill.getId()); // remove old effect before adding new one
						player.getSkillEffects(skill.getId(), skill.getLevel());
						player.screenMessage(LanguageEngine.getMsg("chests_player_criticalRateBuff_levelUp", skill.getLevel()), getEventName(), false);
						
						getPlayerData(player).addSkill(id, newLevel, SkillType.PERMANENT);
						addedBuff = true;
					}
				}
			}
			
			return addedBuff;
		}
	}
	
	private void resetKillStreak(PlayerEventInfo player, NpcData npc)
	{
		if(getPlayerData(player).getKillStreak() > 1)
		{
			player.sendMessage(LanguageEngine.getMsg("chests_player_killStreakReset"));
		}
		
		getPlayerData(player).resetKillStreak();
		
		for(Entry<Integer, Integer> e : getPlayerData(player).getSkills(SkillType.KILLSTREAK).entrySet())
		{
			SkillData skill = new SkillData(e.getKey(), e.getValue());
			if(skill.exists())
				removeSkill(player, skill, SkillType.KILLSTREAK, true);
		}
		
		getPlayerData(player).removeSkills(SkillType.KILLSTREAK);
	}
	
	private class ActionData
	{
		private boolean canServerKill, resetDeathstreak, addKillStreak, resetKillStreak;
		public ActionData(boolean canServerKillTheChest, boolean resetDeathstreak, boolean addKillStreak, boolean resetKillStreak)
		{
			this.canServerKill = canServerKillTheChest;
			this.resetDeathstreak = resetDeathstreak;
			
			this.addKillStreak = addKillStreak;
			this.resetKillStreak = resetKillStreak;
		}
	}
	
	private class EffectResult
	{
		@SuppressWarnings("unused")
		boolean success, resetDeathstreak, addKillstreak, resetKillstreak;
		public EffectResult(boolean success, boolean resetDeathstreak, boolean addKillstreak, boolean resetKillstreak)
		{
			this.success = success;
			this.resetDeathstreak = resetDeathstreak;
			this.addKillstreak = addKillstreak;
			this.resetDeathstreak = resetDeathstreak;
		}
		
		public EffectResult(boolean success)
		{
			this.success = success;
		}
	}
	
	public boolean canServerKillChest(NpcData npc, PlayerEventInfo killer)
	{
		if(getMatch(killer.getInstanceId()).isActive())
		{
			ChestType type = getType(npc.getNpcId());
			
			if(type == null)
				return true;
			
			boolean chestAlive = false;
			
			// check if NPC is in the list
			for(NpcData ch : getEventData(killer.getInstanceId())._chests.get(type))
			{
				if(ch != null && ch.getObjectId() == npc.getObjectId())
				{
					chestAlive = true;
					break;
				}
			}
			
			if(!chestAlive)
				return false;
			
			ActionData data = selectAction(killer, npc, type);
			
			// remove NPC from the list
			for(NpcData ch : getEventData(killer.getInstanceId())._chests.get(type))
			{
				if(ch != null && ch.getObjectId() == npc.getObjectId())
				{
					synchronized(getEventData(killer.getInstanceId())._chests)
					{
						getEventData(killer.getInstanceId())._chests.get(type).remove(ch);
					}
					
					checkEventEnd(killer.getInstanceId());
					
					break;
				}
			}
			
			if(data != null)
			{
				if(data.resetDeathstreak)
					resetDeathStreak(killer);
				
				if(data.addKillStreak)
					addKillStreak(killer, npc);
				else if(data.resetKillStreak)
					resetKillStreak(killer, npc);
				
				return data.canServerKill;
			}
		}
		
		return true;
	}
	
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null)
			return;
		
		int score = 1;
		
		if(getPlayerData(player).getTransformation() != null)
		{
			// update player's own stats
			switch(getPlayerData(player).getTransformation())
			{
				case BUNNY:
					player.screenMessage(LanguageEngine.getMsg("chests_player_transform_bunny_scoreMsg"), getEventName(), true);
					break;
				case PIG:
					player.screenMessage(LanguageEngine.getMsg("chests_player_transform_pig_scoreMsg"), getEventName(), true);
					break;
				case YETI:
					player.screenMessage(LanguageEngine.getMsg("chests_player_transform_yeti_scoreMsg"), getEventName(), true);
					break;
				case FROG:
					return;
			}
		}
		
		if(getPlayerData(player).getTransformation() == TransformType.YETI)
		{
			
		}
		else
		{
			if(getPlayerData(target.getEventInfo()).getTransformation() != null)
			{
				switch(getPlayerData(target.getEventInfo()).getTransformation())
				{
					case BUNNY:
						player.sendMessage(LanguageEngine.getMsg("chests_player_transform_bunny_diedMsg", _bunnyKilledScore));
						score = _bunnyKilledScore;
						break;
					case PIG:
						player.sendMessage(LanguageEngine.getMsg("chests_player_transform_pig_diedMsg", _pigKilledScore));
						score = _pigKilledScore;
						break;
					case YETI:
						player.sendMessage(LanguageEngine.getMsg("chests_player_transform_yeti_diedMsg", _yetiKilledScore));
						score = _yetiKilledScore; 
						break;
					case FROG:
						player.sendMessage(LanguageEngine.getMsg("chests_player_transform_frog_diedMsg", _frogKilledScore));
						score = _frogKilledScore;
						return;
				}
			}
		}
		
		getPlayerData(player).raiseScore(score);
		getPlayerData(player).raiseKills(score);
		getPlayerData(player).raiseSpree(1);
		
		// update title
		if(player.isTitleUpdated())
		{
			player.setTitle(getTitle(player), true);
			player.broadcastTitleInfo();
		}
		
		// update Stats table
		setScoreStats(player, getPlayerData(player).getScore());
		setKillsStats(player, getPlayerData(player).getKills());
		
		CallbackManager.getInstance().playerScores(getEventType(), player, 1);
	}
	
	@Override
	public void onDie(PlayerEventInfo player, CharacterData killer)
	{
		int resDelay = getInt("resDelay") * 1000;
		
		if(getPlayerData(player).getTransformation() != null)
		{
			untransformPlayer(player);
			
			if(_transformationHalfResTime)
				resDelay /= 2;
		}
		
		for(Entry<Integer, Integer> e : getPlayerData(player).getSkills(SkillType.TILL_DIE).entrySet())
		{
			SkillData skill = new SkillData(e.getKey(), e.getValue());
			if(skill.exists())
			{
				player.screenMessage(LanguageEngine.getMsg("chests_player_buffRemoved", skill.getName(), skill.getLevel()), getEventName(), false);
				removeSkill(player, skill, SkillType.TILL_DIE, true);
			}
		}
		
		getPlayerData(player).removeSkills(SkillType.TILL_DIE);
		
		getPlayerData(player).raiseDeaths(1);
		getPlayerData(player).setSpree(0);
		setDeathsStats(player, getPlayerData(player).getDeaths());
		
		// Death streak system
		int ds = getPlayerData(player).getDeathStreak();
		
		if(ds == 1) // dead 2 times in a row w/o any score
		{
			player.screenMessage(LanguageEngine.getMsg("chests_player_respawnDelayDecreased", "50%"), getEventName(), false);
			resDelay *= 0.5F;
		}
		else if(ds == 2) // dead 3 times in a row w/o any score
		{
			player.screenMessage(LanguageEngine.getMsg("chests_player_respawnDelayDecreased", "75"), getEventName(), false);
			resDelay *= 0.25F;
		}
		else if(ds == 3) // dead 4 times in a row w/o any score
		{
			player.screenMessage(LanguageEngine.getMsg("chests_player_respawnDelayDecreased", "75%"), getEventName(), false);
			resDelay *= 0.25F;
		}
		
		getPlayerData(player).addDeathStreak();
		
		scheduleRevive(player, resDelay);
	}
	
	@Override
	protected void respawnPlayer(PlayerEventInfo pi, int instance)
	{
		super.respawnPlayer(pi, instance);
		
		switch(getPlayerData(pi).getDeathStreak())
		{
			case 3:
				final int id = 35020;
				
				SkillData skill = new SkillData(id, 1);
				if(skill.exists())
					pi.getSkillEffects(skill.getId(), skill.getLevel());
				
				getPlayerData(pi).addSkill(id, 1, SkillType.TILL_KILL);
				
				pi.screenMessage(LanguageEngine.getMsg("chests_player_deathStreakWindWalk", 3), getEventName(), false);
				break;
			case 4:
				getPlayerData(pi).giveDeathstreakShield();
				updateBombShield(pi);
				
				pi.screenMessage(LanguageEngine.getMsg("chests_player_deathStreakSuperiorShield", 4), getEventName(), false);
				break;
		}
	}
	
	@Override
	public boolean canSupport(PlayerEventInfo player, CharacterData target)
	{
		if(player.getPlayersId() == target.getObjectId())
			return true;
		return false;
	}
	
	@Override
	public boolean canAttack(PlayerEventInfo player, CharacterData target)
	{
		/*if(getPlayerData(player).getActiveWeapon() == LuckyItem.HEAVYSWORD)
		{
			heavySwordAttack(player, target);
			return false;
		}*/

		if(target.getEventInfo() != null) // player
		{
			boolean playerTransformed;
			boolean targetTransformed;
			
			try
			{
				playerTransformed = getPlayerData(player).getTransformation() != null && getPlayerData(player).getTransformation() != TransformType.FROG;
				targetTransformed = getPlayerData(target.getEventInfo()).getTransformation() != null;
			}
			catch (Exception e) // happens when the event starts, PlayerEventData are not loaded yet
			{
				playerTransformed = false;
				targetTransformed = false;
			}
			
			if(playerTransformed && targetTransformed && getPlayerData(player).getTransformation() == getPlayerData(target.getEventInfo()).getTransformation())
			{
				player.sendMessage(LanguageEngine.getMsg("chests_player_sameSpecie"));
				return false;
			}
			else if(playerTransformed)
				return true;
			else if(targetTransformed)
				return true;
			else
			{
				player.sendMessage(LanguageEngine.getMsg("chests_player_cantAttackPlayers"));
				return false;
			}
		}
		else // chest
		{
			if(getPlayerData(player).getTransformation() != null)
			{
				player.sendMessage(LanguageEngine.getMsg("chests_player_cantAttackChests"));
				return false;
			}
			else
				return true;
		}
	}
	
	@SuppressWarnings("unused")
	private void heavySwordAttack(final PlayerEventInfo player, final CharacterData target)
	{
		//player.broadcastSkillUse(null, player.getTarget(), 315, 1);
		player.broadcastSkillLaunched(null, player.getTarget(), 315, 1);
		
		/*CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				if(!target.isDead())
					target.doDie(player.getCharacterData());
			}
		}, 1800);*/
	}
	
	@Override
	public boolean onSay(PlayerEventInfo player, String text, int channel)
	{
		if(text.equals(".scheme"))
		{
			EventManager.getInstance().getHtmlManager().showSelectSchemeForEventWindow(player, "none", getEventType().getAltTitle());
			return false;
		}
		
		if(player.isGM())
		{
			try
			{
				LuckyItem it = LuckyItem.valueOf(text.toUpperCase());
				if(it != null)
				{
					if(!getPlayerData(player).hasWeapon(it))
						giveWeapon(player, it, true, true);
					else
						removeWeapon(player, it);
					
					updateSkillsForWeapon(player);
				}
			}
			catch (Exception e)
			{
			}
			
		}
		return true;
	}
	
	@Override
	public boolean canUseItem(PlayerEventInfo player, ItemData item)
	{
		if(item.isWeapon() || item.getBodyPart() == CallBack.getInstance().getValues().SLOT_L_HAND())
		{
			//TODO delay check - can change weapons only each 2 seconds
			
			
			if(getWeaponType(item.getItemId()) != null && !item.isEquipped())
			{
				if(!getPlayerData(player).hasWeapon(getWeaponType(item.getItemId())))
				{
					player.sendMessage(LanguageEngine.getMsg("chests_player_cantUseWeapon"));
					return false;
				}
				
				//TODO - can't switch weapon when fighting a chest
				
				return true;
			}
			else if(getWeaponType(item.getItemId()) != LuckyItem.SHIELD)
			{
				player.sendMessage(LanguageEngine.getMsg("event_itemNotAllowed"));
				return false;
			}
		}
		
		return super.canUseItem(player, item);
	}
	
	@Override
	public boolean canDestroyItem(PlayerEventInfo player, ItemData item)
	{
		player.sendMessage(LanguageEngine.getMsg("chests_player_cantDestroyWeapon"));
		return false;
	}
	
	@Override
	public boolean canUseSkill(PlayerEventInfo player, SkillData skill)
	{
		/*if(!super.canUseSkill(player, skill))
		return false;*/
		
		if(_skillsForAll != null && _skillsForAll.containsKey(skill.getId()))
			return true;
		
		if(skill.getId() == _rushSkill)
		{
			if(getPlayerData(player).hasSkill(_rushSkill, SkillType.PERMANENT))
				return true;
			else
				return false;
		}
		
		if(skill.getId() == _aggressionSkillId || skill.getId() == _whirlwindSkill)
		{
			if(getPlayerData(player).getActiveWeapon() == LuckyItem.PIKE || getPlayerData(player).getActiveWeapon() == LuckyItem.LANCE)
				return true;
			else
			{
				player.sendMessage(LanguageEngine.getMsg("chests_player_skillOnlyWith", "polarm"));
				return false;
			}
		}
		
		if(skill.getId() == _backstabSkill)
		{
			if(getPlayerData(player).getActiveWeapon() == LuckyItem.DAGGER_CRAFTED || getPlayerData(player).getActiveWeapon() == LuckyItem.DIRK)
				return true;
			else
			{
				player.sendMessage(LanguageEngine.getMsg("chests_player_skillOnlyWith", "dagger"));
				return false;
			}
		}
		
		if(skill.getId() == _stunSkill)
		{
			if(getPlayerData(player).getActiveWeapon() == LuckyItem.HAMMER)
				return true;
			else
			{
				player.sendMessage(LanguageEngine.getMsg("chests_player_skillOnlyWith", "hammer"));
				return false;
			}
		}
		
		if(_skillsForItems != null)
		{
			LuckyItem activeWeapon = getPlayerData(player).getActiveWeapon();
			
			for(Entry<LuckyItem, FastMap<Integer, Integer>> e : _skillsForItems.entrySet())
			{
				if(e.getValue().containsKey(skill.getId()))
				{
					if(activeWeapon == e.getKey())
						return true;
					else
					{
						player.sendMessage(LanguageEngine.getMsg("chests_player_skillOnlyWith", e.getKey().toString().toLowerCase()));
						return false;
					}
				}
			}
		}
		
		player.sendMessage(LanguageEngine.getMsg("event_skillNotAllowed"));
		return false;
	}
	
	@Override
	public boolean canBeDisarmed(PlayerEventInfo player)
	{
		return false;
	}
	
	@Override
	public boolean canSaveShortcuts(PlayerEventInfo player)
	{
		return !_customShortcuts;
	}
	
	@Override
	public EventPlayerData createPlayerData(PlayerEventInfo player)
	{
		return new LuckyChestsPlayerData(player, this);
	}

	@Override
	public LuckyChestsPlayerData getPlayerData(PlayerEventInfo player)
	{
		return (LuckyChestsPlayerData) player.getEventData();
	}
	
	@Override
	public synchronized void clearEvent(int instanceId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: called CLEAREVENT for instance " + instanceId);
		
		try
		{
			if(_matches != null)
			{
				for(DMEventInstance match : _matches.values())
				{
					if(instanceId == 0 || instanceId == match.getInstance().getId())
					{
						match.abort();
						unspawnChests(match.getInstance().getId());
						restorePlayers(match.getInstance().getId());
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		for (PlayerEventInfo player : getPlayers(instanceId))
		{
			if(!player.isOnline())
				continue;
			
			if(player.isParalyzed())
				player.setIsParalyzed(false);
			
			if(player.isImmobilized())
				player.unroot();
			
			if(!player.isGM())
				player.setIsInvul(false);
			
			player.removeRadarAllMarkers();
			
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_BIG_HEAD());
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_INVULNERABLE());
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_S_INVINCIBLE());
			
			player.setInstanceId(0);
			
			player.restoreData();
			
			EventManager.getInstance().removeEventSkills(player);
			
			player.teleport(player.getOrigLoc(), 0, true, 0);
			player.sendMessage(LanguageEngine.getMsg("event_teleportBack"));
			
			if (player.getParty() != null)
			{
				PartyData party = player.getParty();
				party.removePartyMember(player);
			}
			
			player.broadcastUserInfo();
		}
		
		clearPlayers(true, instanceId);
	}
	
	@Override
	public int getTeamsCount()
	{
		return 1;
	}
	
	@Override
	public String getMissingSpawns(EventMap map)
	{
		TextBuilder tb = new TextBuilder();
		
		if(!map.checkForSpawns(SpawnType.Regular, -1, 1))
			tb.append(addMissingSpawn(SpawnType.Regular, 0, 1));
		
		if(!map.checkForSpawns(SpawnType.Chest, -1, 1))
			tb.append(addMissingSpawn(SpawnType.Chest, 0, 1));
		
		return tb.toString();
	}
	
	private boolean checkNpcs()
	{
		NpcTemplateData template;
		
		template = new NpcTemplateData(_classicChestId);
		if(!template.exists())
		{
			NexusLoader.debug("Lucky chests: missing template for CLASSIC CHEST - ID " + _classicChestId);
			/**/ if(NexusLoader.detailedDebug) print("Lucky Chests: missing template for CLASSIC CHEST - ID " + _classicChestId);
			
			return false;
		}
		
		if(_shabbyChestEnabled)
		{
			template = new NpcTemplateData(_shabbyChestId);
			if(!template.exists())
			{
				NexusLoader.debug("Lucky chests: missing template for SHABBY CHEST - ID " + _shabbyChestId);
				/**/ if(NexusLoader.detailedDebug) print("Lucky Chests: missing template for SHABBY CHEST - ID " + _shabbyChestId);
				
				return false;
			}
		}
		
		if(_luxuriousChestEnabled)
		{
			template = new NpcTemplateData(_luxuriousChestId);
			if(!template.exists())
			{
				NexusLoader.debug("Lucky chests: missing template for LUXURIOUS CHEST - ID " + _luxuriousChestId);
				/**/ if(NexusLoader.detailedDebug) print("Lucky Chests: missing template for LUXURIOUS CHEST - ID " + _luxuriousChestId);
				
				return false;
			}
		}
		
		if(_boxChestEnabled)
		{
			template = new NpcTemplateData(_boxChestId);
			if(!template.exists())
			{
				NexusLoader.debug("Lucky chests: missing template for BOX CHEST - ID " + _boxChestId);
				/**/ if(NexusLoader.detailedDebug) print("Lucky Chests: missing template for BOX CHEST - ID " + _boxChestId);
				
				return false;
			}
		}
		
		if(_nexusedChestEnabled)
		{
			template = new NpcTemplateData(_nexusedChestId);
			if(!template.exists())
			{
				NexusLoader.debug("Lucky chests: missing template for NEXUSED CHEST - ID " + _nexusedChestId);
				/**/ if(NexusLoader.detailedDebug) print("Lucky Chests: missing template for NEXUSED CHEST - ID " + _nexusedChestId);
				
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	protected String addExtraEventInfoCb(int instance)
	{
		int top = 0;
		for(PlayerEventInfo player : getPlayers(instance))
		{
			if(getPlayerData(player).getScore() > top)
				top = getPlayerData(player).getScore();
		}
		
		String status = "<font color=ac9887>Top score: </font><font color=7f7f7f>" + top + "</font>";
		
		return("<table width=510 bgcolor=3E3E3E><tr><td width=510 align=center>" + status + "</td></tr></table>");
	}
	
	@Override
	public String getHtmlDescription()
	{
		//TODO
		if(_htmlDescription == null)
		{
			EventDescription desc = EventDescriptionSystem.getInstance().getDescription(getEventType());
			if(desc != null)
			{
				_htmlDescription = desc.getDescription(getConfigs());
			}
			else
			{
				_htmlDescription = "No information about this event yet.";
			}
		}
		return _htmlDescription;
	}
	
	private enum SkillType
	{
		TILL_DIE,
		TILL_KILL,
		KILLSTREAK,
		PERMANENT,
		TRANSFORM,
		WEAPON
	}
	
	public class LuckyChestsPlayerData extends PvPEventPlayerData
	{
		List<LuckyItem> _weapons = new FastList<LuckyItem>();
		Map<LuckyItem, ShortCutData> _weaponShortcuts = new FastMap<LuckyItem, ShortCutData>();
		Map<Integer, ShortCutData> _skillShortcuts = new FastMap<Integer, ShortCutData>();
		
		Map<SkillType, Map<Integer, Integer>> _skills = new FastMap<SkillType, Map<Integer, Integer>>();
		Map<LuckyItem, Map<Integer, Integer>> _skillsForWeapons = new FastMap<LuckyItem, Map<Integer, Integer>>();
		
		private LuckyItem _activeWeapon = null;
		private LuckyItem _lastSkillsOfWeapon = null;
		
		private boolean _hasShield = false;
		private boolean _bigHead = false;
		private int _deathStreak = 0;
		private int _killStreak = 0;
		private int _bombShield = 0;
		private boolean _deathstreakShield = false;
		
		private TransformType _transformed = null;
		
		public LuckyChestsPlayerData(PlayerEventInfo owner, EventGame event)
		{
			super(owner, event, new GlobalStatsModel(getEventType()));
			
			for(SkillType type : SkillType.values())
			{
				_skills.put(type, new FastMap<Integer, Integer>());
			}
		}
		
		private void setTransformed(TransformType type)
		{
			_transformed = type;
		}
		
		private TransformType getTransformation()
		{
			return _transformed;
		}
		
		private void addKillStreak(int i)
		{
			_killStreak += i;
		}
		
		@SuppressWarnings("unused")
		private void decreaseKillStreak(int i)
		{
			_killStreak -= i;
			if(_killStreak < 0)
				_killStreak = 0;
		}
		
		private void resetKillStreak()
		{
			_killStreak = 0;
		}
		
		private int getKillStreak()
		{
			return _killStreak;
		}
		
		private void addSkill(int id, int level, SkillType type)
		{
			_skills.get(type).put(id, level);
		}
		
		private void addSkillForWeapon(int id, int level, LuckyItem item)
		{
			if(!_skillsForWeapons.containsKey(item))
				_skillsForWeapons.put(item, new FastMap<Integer, Integer>());
			
			_skillsForWeapons.get(item).put(id, level);
		}
		
		private void setActiveWeaponWithSkills(LuckyItem item)
		{
			_lastSkillsOfWeapon = item;
		}
		
		private LuckyItem getActiveWeaponWithSkills()
		{
			return _lastSkillsOfWeapon;
		}
		
		private Map<Integer, Integer> getSkillsForWeapon(LuckyItem item)
		{
			if(!_skillsForWeapons.containsKey(item))
				return new FastMap<Integer, Integer>();
			
			return _skillsForWeapons.get(item);
		}
		
		private Map<LuckyItem, Map<Integer, Integer>> getAllSkillsForWeapons()
		{
			return _skillsForWeapons;
		}
		
		@SuppressWarnings("unused")
		private void removeBuff(int id, SkillType type)
		{
			_skills.get(type).remove(id);
		}
		
		@SuppressWarnings("unused")
		private void removeSkillsForWeapon(LuckyItem item)
		{
			if(_skillsForWeapons.containsKey(item))
				_skillsForWeapons.get(item).clear();
		}
		
		@SuppressWarnings("unused")
		private boolean hasSkillForWeapon(int id, LuckyItem item)
		{
			return getLevelOfSkillForWeapon(id, item) > 0;
		}
		
		private int getLevelOfSkillForWeapon(int id, LuckyItem item)
		{
			if(!_skillsForWeapons.containsKey(item))
				return 0;
			
			return (_skillsForWeapons.get(item).containsKey(id) ? _skills.get(item).get(id) : 0);
		}
		
		private void removeSkills(SkillType type)
		{
			_skills.get(type).clear();
		}
		
		private int getLevel(int id, SkillType type)
		{
			return (_skills.get(type).containsKey(id) ? _skills.get(type).get(id) : 0);
		}
		
		private Map<Integer, Integer> getSkills(SkillType type)
		{
			return _skills.get(type);
		}
		
		private Map<SkillType, Map<Integer, Integer>> getAllSkills()
		{
			return _skills;
		}
		
		private boolean hasSkill(int id, SkillType type)
		{
			return getLevel(id, type) > 0;
		}
		
		private void giveDeathstreakShield()
		{
			_deathstreakShield = true;
		}
		
		private boolean hasDeathstreakShield()
		{
			return _deathstreakShield;
		}
		
		private void removeDeathstreakShield()
		{
			_deathstreakShield = false;
		}
		
		private void raiseBombShield(int i)
		{
			_bombShield += i;
		}
		
		private int hasBombShield()
		{
			return _bombShield;
		}
		
		private void decreaseBombShield(int i)
		{
			_bombShield -= i;
		}
		
		private boolean hasBigHead()
		{
			return _bigHead;
		}
		
		private void setHasBigHead(boolean b)
		{
			_bigHead = b;
		}
		
		private void setHasShield(boolean b)
		{
			_hasShield = b;
		}
		
		private boolean hasShield()
		{
			return _hasShield;
		}
		
		private void addWeapon(LuckyItem w)
		{
			_weapons.add(w);
		}
		
		private void removeWeapon(LuckyItem w)
		{
			_weapons.remove(w);
		}
		
		private boolean hasWeapon(LuckyItem w)
		{
			return _weapons.contains(w);
		}
		
		private LuckyItem getWeaponOfType(WeaponType type)
		{
			for(LuckyItem it : _weapons)
			{
				if(it._type == type)
					return it;
			}
			return null;
		}
		
		private void addWeaponShortcut(LuckyItem type, ShortCutData sh)
		{
			_weaponShortcuts.put(type, sh);
		}
		
		private void addSkillShortcut(SkillData skill, ShortCutData sh)
		{
			_skillShortcuts.put(skill.getId(), sh);
		}
		
		private void removeWeaponShortcut(LuckyItem type, ShortCutData sh)
		{
			_weaponShortcuts.remove(type);
		}
		
		private void removeSkillShortcut(SkillData skill, ShortCutData sh)
		{
			_skillShortcuts.remove(skill.getId());
		}
		
		@SuppressWarnings("unused")
		private ShortCutData getShortCut(int slot)
		{
			for(ShortCutData sh : _weaponShortcuts.values())
			{
				if(sh.getSlot() == slot)
					return sh;
			}
			return null;
		}
		
		private ShortCutData getWeaponShortCut(LuckyItem type)
		{
			for(Entry<LuckyItem, ShortCutData> sh : _weaponShortcuts.entrySet())
			{
				if(sh.getKey() == type)
					return sh.getValue();
			}
			return null;
		}
		
		private ShortCutData getSkillShortcut(SkillData skill)
		{
			for(Entry<Integer, ShortCutData> sh : _skillShortcuts.entrySet())
			{
				if(sh.getKey() == skill.getId())
					return sh.getValue();
			}
			return null;
		}
		
		private void addDeathStreak()
		{
			_deathStreak ++;
		}
		
		private void resetDeathStreak()
		{
			_deathStreak = 0;
		}
		
		private int getDeathStreak()
		{
			return _deathStreak;
		}
		
		private LuckyItem getActiveWeapon()
		{
			return _activeWeapon;
		}
		
		private void setActiveWeapon(LuckyItem w)
		{
			_activeWeapon = w;
		}
		
		protected class SlotInfo
		{
			public int slot, page;
			public SlotInfo(int slot, int page)
			{
				this.slot = slot;
				this.page = page;
			}
		}
		
		private SlotInfo getNextFreeShortcutSlot(boolean weapon)
		{
			final int maxPages = 9;
			
			SlotInfo freeSlot = null;

			boolean existsInSlot;
			for(int page = 0; page < maxPages; page++)
			{
				if(freeSlot != null)
					break;
				
				for(int slot = weapon ? 0 : 11; weapon ? (slot < 11) : (slot > 0);)
				{
					existsInSlot = false;
					
					for(Entry<LuckyItem, ShortCutData> sh : _weaponShortcuts.entrySet())
					{
						if(sh.getValue().getPage() == page && sh.getValue().getSlot() == slot)
						{
							existsInSlot = true;
							break;
						}
					}
					
					for(Entry<Integer, ShortCutData> sh : _skillShortcuts.entrySet())
					{
						if(sh.getValue().getPage() == page && sh.getValue().getSlot() == slot)
						{
							existsInSlot = true;
							break;
						}
					}
					
					if(existsInSlot)
					{
						if(weapon) slot++;
						else slot--;
						
						continue;
					}
					else
					{
						freeSlot = new SlotInfo(slot, page);
						break;
					}
				}
			}
			
			return freeSlot;
		}
	}
	
	@Override
	protected LuckyChestsData createEventData(int instance)
	{
		return new LuckyChestsData(instance);
	}
	
	@Override
	protected LuckyChestsEventInstance createEventInstance(InstanceData instance)
	{
		return new LuckyChestsEventInstance(instance);
	}
	
	@Override
	protected LuckyChestsData getEventData(int instance)
	{
		try
		{
			return (LuckyChestsData) _matches.get(instance)._data;
		}
		catch (Exception e)
		{
			NexusLoader.debug("Error on getEventData for instance " + instance);
			e.printStackTrace();
			return null;
		}
	}
}
