package cz.nxs.events.engine.main.events;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventRewardSystem;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.Loc;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.base.description.EventDescription;
import cz.nxs.events.engine.base.description.EventDescriptionSystem;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.MainEventManager;
import cz.nxs.events.engine.main.base.MainEventInstanceType;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.delegate.ItemData;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class TreasureHunt extends Deathmatch
{
	public enum ChestType
	{
		NORMAL, FAKE, LUCKY, ANCIENT, UNLUCKY, EXPLODING, NUKE
	}
	
	protected class THData extends DMData
	{
		public List<NpcData> _chests;
		private ScheduledFuture<?> _endCheckerFuture = null;
		
		protected THData(int instance)
		{
			super(instance);
			
			_chests = new FastList<NpcData>(); 
		}
		
		public void addChest(NpcData ch)
		{
			_chests.add(ch);
		}
		
		public void removeChest(NpcData ch)
		{
			if(_chests.contains(ch))
				_chests.remove(ch);
		}
	}
	
	protected class THEventInstance extends DMEventInstance
	{
		protected EventState _nextState;
		
		protected THData _data;
		
		public THEventInstance(InstanceData instance)
		{
			super(instance);
			
			_nextState = EventState.START;
			
			_data = createEventData(_instance.getId());
		}
		
		protected void setNextState(EventState state) {_nextState = state;}
		
		@Override
		public boolean isActive() { return _nextState != EventState.INACTIVE;} 
		
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
							if(_allowPvp)
							{
								if(_antifeed)
								{
									for(PlayerEventInfo player : getPlayers(_instance.getId()))
										player.startAntifeedProtection(false);
								}
							}
							
							teleportPlayers(_instance.getId(), SpawnType.Regular, true);
							
							spawnChests(_instance.getId());
							
							disequipWeapons(_instance.getId());
							
							setupTitles(_instance.getId());
							
							enableMarkers(_instance.getId(), true);
							
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
								rewardAllPlayers(_instance.getId(), 0, getInt("killsForReward"));
							
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

	protected enum EventState
	{
		START, FIGHT, END, TELEPORT, INACTIVE
	}

	protected FastMap<Integer, THEventInstance> _matches;
	
	protected boolean _waweRespawn;
	protected boolean _antifeed;
	
	protected int _normalChestChance;
	protected int _fakeChestChance;
	protected int _luckyChestChance;
	protected int _ancientChestChance;
	protected int _unluckyChestChance;
	protected int _explodingChestChance;
	protected int _nukeChestChance;
	
	protected int _normalChestNpcId;
	protected int _fakeChestNpcId;
	protected int _luckyChestNpcId;
	protected int _ancientChestNpcId;
	protected int _unluckyChestNpcId;
	protected int _explodingChestNpcId;
	protected int _nukeChestNpcId;
	
	private int _endCheckInterval;
	
	protected boolean _allowPvp = false;
	
	protected int _countOfChests;

	public TreasureHunt(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Looser, RewardPosition.Tie, RewardPosition.Numbered, RewardPosition.Range, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.ChestReward, RewardPosition.ChestRewardLucky, RewardPosition.ChestRewardAncient });
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		addConfig(new ConfigModel("normalChestChance", "75000", "The chance in percent to spawn a normal chest. 100 000 equals 100%."));
		addConfig(new ConfigModel("luckyChestChance", "10000", "The chance in percent to spawn a lucky chest. 100 000 equals 100%."));
		addConfig(new ConfigModel("ancientChestChance", "2000", "The chance in percent to spawn an ancient chest. 100 000 equals 100%."));
		addConfig(new ConfigModel("unluckyChestChance", "2500", "The chance in percent to spawn an unlucky chest. 100 000 equals 100%."));
		addConfig(new ConfigModel("fakeChestChance", "2500", "The chance in percent to spawn a fake chest. 100 000 equals 100%."));
		addConfig(new ConfigModel("explodingChestChance", "7500", "The chance in percent to spawn a exploding chest. 100 000 equals 100%."));
		addConfig(new ConfigModel("nukeChestChance", "500", "The chance in percent to spawn a nuke chest. 100 000 equals 100%."));
		
		addConfig(new ConfigModel("normalChestNpcId", "8689", "The NpcId in percent to spawn a normal chest. "));
		addConfig(new ConfigModel("luckyChestNpcId", "8688", "The NpcId in percent to spawn a lucky chest. "));
		addConfig(new ConfigModel("ancientChestNpcId", "8687", "The NpcId in percent to spawn a ancient chest. "));
		addConfig(new ConfigModel("unluckyChestNpcId", "8686", "The NpcId in percent to spawn a unlucky chest. "));
		addConfig(new ConfigModel("fakeChestNpcId", "8685", "The NpcId in percent to spawn a fake chest. "));
		addConfig(new ConfigModel("explodingChestNpcId", "8684", "The NpcId in percent to spawn a exploding chest. "));
		addConfig(new ConfigModel("nukeChestNpcId", "8683", "The NpcId in percent to spawn a nuke chest. "));
		
		addConfig(new ConfigModel("checkInactiveDelay", "300", "In seconds. If no chests are opened within this time, the event will be aborted. Eg. if you set this 120 and nobody manages to find and open a chest for 120 seconds, the event will be ended. Disable this by setting 0."));
		
		addConfig(new ConfigModel("scoreForReward", "0", "The minimum of score required to get a reward (includes all possible rewards). Score is gained by killing chests."));
		
		addConfig(new ConfigModel("resDelay", "15", "The delay after which a dead player is resurrected. In seconds."));
		
		if(_allowPvp)
		{
			addConfig(new ConfigModel("waweRespawn", "true", "Enables the wawe-style respawn system.", InputType.Boolean));
			
			addConfig(new ConfigModel("firstBloodMessage", "true", "You can turn off/on the first blood announce in the event (first kill made in the event). This is also rewardable - check out reward type FirstBlood.", InputType.Boolean));
			
			addConfig(new ConfigModel("antifeedProtection", "true", "Enables the special anti-feed protection. This protection changes player's name, title, race, clan/ally crest, class and basically all of his apperance, sometimes also gender.", InputType.Boolean));
		}
		else
		{
			removeConfig("killsForReward");
			removeConfig("waweRespawn");
		}
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		
		if(_allowPvp)
		{
			// storing config value before event start - just to prevent the error that would cause a GM that changes 'waweRespawn' config while the event is running
			_waweRespawn = getBoolean("waweRespawn"); 
			_antifeed = getBoolean("antifeedProtection");
			
			if(_waweRespawn)
				initWaweRespawns(getInt("resDelay"));
		}
		
		_normalChestChance = getInt("normalChestChance");
		_fakeChestChance = getInt("fakeChestChance");
		_luckyChestChance = getInt("luckyChestChance");
		_ancientChestChance = getInt("ancientChestChance");
		_unluckyChestChance = getInt("unluckyChestChance");
		_explodingChestChance = getInt("explodingChestChance");
		_nukeChestChance = getInt("nukeChestChance");
		
		_normalChestNpcId = getInt("normalChestNpcId");
		_fakeChestNpcId = getInt("fakeChestNpcId");
		_luckyChestNpcId = getInt("luckyChestNpcId");
		_ancientChestNpcId = getInt("ancientChestNpcId");
		_unluckyChestNpcId = getInt("unluckyChestNpcId");
		_explodingChestNpcId = getInt("explodingChestNpcId");
		_nukeChestNpcId = getInt("nukeChestNpcId");
		
		_endCheckInterval = getInt("checkInactiveDelay");
		
		_countOfChests = getInt("countOfChests");
		
		_runningInstances = 0;
	}
	
	@Override
	protected int initInstanceTeams(MainEventInstanceType type, int instanceId)
	{
		// creates only one team to store players in
		createTeams(1, type.getInstance().getId());
		return 1;
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
		
		THEventInstance match;
		_matches = new FastMap<Integer, THEventInstance>();
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
	
	public void spawnChest(int instanceId, ChestType type, EventSpawn sp)
	{
		if(sp == null)
			return;
		
		Loc loc;
		NpcData npc;
		
		loc = sp.getLoc();
		loc.addRadius(sp.getRadius()); 
		
		int npcId = getChestId(type);
		
		npc = spawnNPC(loc.getX(), loc.getY(), loc.getZ(), npcId, instanceId, null, null);
		getEventData(instanceId).addChest(npc);
	}
	
	public void disequipWeapons(int instanceId)
	{
		if(_allowPvp)
			return;
			
		for(PlayerEventInfo player : getPlayers(instanceId))
		{
			ItemData wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
			if (wpn != null)
				player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_R_HAND());

			wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_LHAND());
			if (wpn != null)
				player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_L_HAND());
		}
	}
	
	public void spawnChests(int instanceId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: spawning " + _countOfChests + " chests");
		
		for(EventSpawn spawn : _manager.getMap().getSpawns(-1, SpawnType.Chest))
		{
			if(random() < _nukeChestChance)
			{
				spawnChest(instanceId, ChestType.NUKE, spawn);
			}
			else if(random() < _explodingChestChance)
			{
				spawnChest(instanceId, ChestType.EXPLODING, spawn);
			}
			else if(random() < _unluckyChestChance)
			{
				spawnChest(instanceId, ChestType.UNLUCKY, spawn);
			}
			else if(random() < _fakeChestChance)
			{
				spawnChest(instanceId, ChestType.FAKE, spawn);
			}
			else if(random() < _ancientChestChance)
			{
				spawnChest(instanceId, ChestType.ANCIENT, spawn);
			}
			else if(random() < _luckyChestChance)
			{
				spawnChest(instanceId, ChestType.LUCKY, spawn);
			}
			else
			{
				spawnChest(instanceId, ChestType.NORMAL, spawn);
			}
		}
	}
	
	private int random()
	{
		return CallBack.getInstance().getOut().random(100000);
	}
	
	public void unspawnChests(int instanceId)
	{
		for(NpcData npc : getEventData(instanceId)._chests)
		{
			if(npc != null)
			{
				npc.deleteMe();
				getEventData(instanceId).removeChest(npc);
			}
		}
	}
	
	@Override
	public void onEventEnd()
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: onEventEnd()"); 
		
		int minScore = getInt("scoreForReward");
		int minKills = getInt("killsForReward");
		
		rewardAllPlayers(-1, minScore, minKills);
		
		if(_allowPvp && _antifeed)
		{
			for(PlayerEventInfo player : getPlayers(0))
				player.stopAntifeedProtection(false);
		}
	}

	@Override
	protected String getScorebar(int instance)
	{
		TextBuilder tb = new TextBuilder();
		
		int top = 0;
		for(PlayerEventInfo player : getPlayers(instance))
		{
			if(getPlayerData(player).getScore() > top)
				top = getPlayerData(player).getScore();
		}
		
		tb.append("Top score: " + top);
		
		tb.append("   Time: " + _matches.get(instance).getClock().getTime());
		
		return tb.toString();
	}

	@Override
	protected String getTitle(PlayerEventInfo pi)
	{
		if(pi.isAfk())
			return "AFK";
		
		return "Score: " + getPlayerData(pi).getScore();
	}
	
	protected void checkEventEnd(int instance)
	{
		if(getEventData(instance)._endCheckerFuture != null)
		{
			getEventData(instance)._endCheckerFuture.cancel(false);
			getEventData(instance)._endCheckerFuture = null;
		}
		
		if(getEventData(instance)._chests.isEmpty())
		{
			announce("All chests were killed. Event has ended.");
			endInstance(instance, true, false, false);
		}
		else
		{
			chestOpened(instance);
		}
	}
	
	private void chestOpened(int instance)
	{
		if(_endCheckInterval > 0)
			getEventData(instance)._endCheckerFuture = CallBack.getInstance().getOut().scheduleGeneral(new EndChecker(instance), _endCheckInterval * 1000);
	}
	
	private class EndChecker implements Runnable
	{
		int instance;
		public EndChecker(int instance)
		{
			this.instance = instance;
		}
		
		@Override
		public void run()
		{
			announce("Some chests hided so well that nobody managed to find them. Event has ended.");
			endInstance(instance, true, false, false);
		}
	}
	
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null) 
		{
			if(target.getNpc() != null)
			{
				selectChestOnKillAction(player.getInstanceId(), player, target.getNpc());
				getEventData(player.getInstanceId()).removeChest(target.getNpc());
				checkEventEnd(player.getInstanceId());
			}
		}
		else if(_allowPvp)
		{
			tryFirstBlood(player);
			giveOnKillReward(player);
			
			// update player's own stats
			getPlayerData(player).raiseKills(1);
			getPlayerData(player).raiseSpree(1);
			
			// update title
			if(player.isTitleUpdated())
			{
				player.setTitle(getTitle(player), true);
				player.broadcastTitleInfo();
			}
			
			CallbackManager.getInstance().playerKills(getEventType(), player, target.getEventInfo());
			
			// update Stats table
			setKillsStats(player, getPlayerData(player).getKills());
		}
	}
	
	protected void selectChestOnKillAction(int instanceId, PlayerEventInfo player, NpcData npc)
	{
		ChestType type = getChestType(npc);
		if(type != null)
		{
			switch(type)
			{
				case NORMAL:
					
					getPlayerData(player).raiseScore(1);
					
					// update title
					if(player.isTitleUpdated())
					{
						player.setTitle(getTitle(player), true);
						player.broadcastTitleInfo();
					}
					
					player.screenMessage("You have scored!", getEventName(), false);
					
					EventRewardSystem.getInstance().rewardPlayer(getEventType(), 1, player, RewardPosition.ChestReward, null, player.getTotalTimeAfk(), 0, 0);
					
					// update Stats table
					setScoreStats(player, getPlayerData(player).getScore());
					
					break;
				case LUCKY:
					
					getPlayerData(player).raiseScore(2);
					
					// update title
					if(player.isTitleUpdated())
					{
						player.setTitle(getTitle(player), true);
						player.broadcastTitleInfo();
					}
					
					player.screenMessage("You have scored! Double points!", getEventName(), false);
					
					EventRewardSystem.getInstance().rewardPlayer(getEventType(), 1, player, RewardPosition.ChestRewardLucky, null, player.getTotalTimeAfk(), 0, 0);
					
					// update Stats table
					setScoreStats(player, getPlayerData(player).getScore());
					
					break;
				case UNLUCKY:
					
					if(getPlayerData(player).getScore() > 0)
					{
						getPlayerData(player).raiseScore(-1);
						
						// update title
						if(player.isTitleUpdated())
						{
							player.setTitle(getTitle(player), true);
							player.broadcastTitleInfo();
						}
						
						player.screenMessage("Bad chest, -1 score.", getEventName(), false);
						
						// update Stats table
						setScoreStats(player, getPlayerData(player).getScore());
					}
					
					break;
				case FAKE:

					player.screenMessage("This chest wasn't real.", getEventName(), false);
					
					break; 
				case ANCIENT:
					
					getPlayerData(player).raiseScore(5);
					
					// update title
					if(player.isTitleUpdated())
					{
						player.setTitle(getTitle(player), true);
						player.broadcastTitleInfo();
					}
					
					player.screenMessage("You have opened an ancient chest and it gave you 5 points.", getEventName(), false);
					
					EventRewardSystem.getInstance().rewardPlayer(getEventType(), 1, player, RewardPosition.ChestRewardAncient, null, player.getTotalTimeAfk(), 0, 0);
					
					// update Stats table
					setScoreStats(player, getPlayerData(player).getScore());
					
					break; 
				case EXPLODING:
					
					// already handled 
					
					break;
				case NUKE:
					
					player.screenMessage("You opened the chest, there was a nuke.", getEventName(), false);
					
					for(PlayerEventInfo pl : getPlayers(instanceId))
					{
						if(!pl.isDead())
						{
							pl.doDie();
							if(pl.getPlayersId() != player.getPlayersId())
								pl.screenMessage("You have been nuked. Thanks go to " + player.getPlayersName() + ".", "THunt", false);
						}
					}
					
					break;
			}
		}
	}
	
	@Override
	public boolean allowKill(CharacterData target, CharacterData killer)
	{
		System.out.println("debugging: TreasureHunt.allowKill() 0");
		if(target.isNpc() && killer.isPlayer())
		{
			System.out.println("debugging: TreasureHunt.allowKill() 1");
			NpcData npc = target.getNpc();
			PlayerEventInfo player = killer.getEventInfo();
			ChestType type = getChestType(npc);
			
			if(type != null && type == ChestType.EXPLODING)
			{
				System.out.println("debugging: TreasureHunt.allowKill() 2");
				explosionAnimation(npc, player);
				return false;
			}
		}
		
		return true;
	}
	
	protected void explosionAnimation(final NpcData npc, PlayerEventInfo player)
	{
		npc.broadcastSkillUse(npc, player.getCharacterData(), 5430, 1);
		
		player.doDie();
		
		CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				if(npc != null)
					npc.deleteMe();
			}
		}, 250);
	}
	
	protected ChestType getChestType(NpcData npc)
	{
		if(npc != null)
		{
			ChestType type = null;
			
			if(npc.getNpcId() == _normalChestNpcId)
			{
				type = ChestType.NORMAL;
			}
			else if(npc.getNpcId() == _luckyChestNpcId)
			{
				type = ChestType.LUCKY;
			}
			else if(npc.getNpcId() == _unluckyChestNpcId)
			{
				type = ChestType.UNLUCKY;
			}
			else if(npc.getNpcId() == _fakeChestNpcId)
			{
				type = ChestType.FAKE;
			}
			else if(npc.getNpcId() == _ancientChestNpcId)
			{
				type = ChestType.ANCIENT;
			}
			else if(npc.getNpcId() == _explodingChestNpcId)
			{
				type = ChestType.EXPLODING;
			}
			else if(npc.getNpcId() == _nukeChestNpcId)
			{
				type = ChestType.NUKE;
			}
			
			return type;
		}
		else return null;
	}
	
	protected int getChestId(ChestType type)
	{
		int npcId = 0;
		switch(type)
		{
			case NORMAL:
				npcId = _normalChestNpcId;
				break;
			case LUCKY:
				npcId = _luckyChestNpcId;
				break;
			case UNLUCKY:
				npcId = _unluckyChestNpcId;
				break;
			case FAKE:
				npcId = _fakeChestNpcId;
				break; 
			case ANCIENT:
				npcId = _ancientChestNpcId;
				break; 
			case EXPLODING:
				npcId = _explodingChestNpcId;
				break;
			case NUKE:
				npcId = _nukeChestNpcId;
				break;
		}
		return npcId;
	}

	@Override
	public void onDie(PlayerEventInfo player, CharacterData killer)
	{
		/**/ if(NexusLoader.detailedDebug) print("/// Event: onDie - player " + player.getPlayersName() + " (instance " + player.getInstanceId() + "), killer " + killer.getName());
		
		getPlayerData(player).raiseDeaths(1);
		getPlayerData(player).setSpree(0);
		
		setDeathsStats(player, getPlayerData(player).getDeaths());
		
		if(_allowPvp && _waweRespawn)
			_waweScheduler.addPlayer(player);
		else
			scheduleRevive(player, getInt("resDelay") * 1000);
	}
	
	@Override
	public boolean canSupport(PlayerEventInfo player, CharacterData target)
	{
		if(player.getPlayersId() != target.getObjectId())
			return false;
		
		return true;
	}
	
	@Override
	public boolean canAttack(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null) // if the target is a mob, allow atacking
			return true;
		else if(target.getEventInfo().getEvent() != player.getEvent())
			return false;

		return _allowPvp;
	}
	
	@Override
	public boolean onSay(PlayerEventInfo player, String text, int channel)
	{
		if(text.equals(".scheme"))
		{
			EventManager.getInstance().getHtmlManager().showSelectSchemeForEventWindow(player, "none", getEventType().getAltTitle());
			return false;
		}
		
		if(_allowPvp && _antifeed)
		{
			player.sendMessage(LanguageEngine.getMsg("dm_cantChat"));
			return false;
		}
		else
			return true;
	}
	
	@Override
	public boolean canInviteToParty(PlayerEventInfo player, PlayerEventInfo target)
	{
		return false;
	}
	
	@Override
	public boolean canUseItem(PlayerEventInfo player, ItemData item)
	{
		if(!_allowPvp)
		{
			player.sendMessage("Weapons are not allowed in this event.");
			return false;
		}
		
		return true;
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
						unspawnChests(match.getInstance().getId());
						match.abort();
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
			
			if(player.hasAntifeedProtection())
			{
				player.stopAntifeedProtection(false);
			}
			
			if(player.isParalyzed())
				player.setIsParalyzed(false);
			
			if(player.isImmobilized())
				player.unroot();
			
			if(!player.isGM())
				player.setIsInvul(false);
			
			player.removeRadarAllMarkers();
			
			player.setInstanceId(0);
			
			player.restoreData();
			
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
	public String getMissingSpawns(EventMap map)
	{
		if(!map.checkForSpawns(SpawnType.Regular, -1, 1))
			return addMissingSpawn(SpawnType.Regular, 0, 1);
		
		if(!map.checkForSpawns(SpawnType.Chest, -1, 1)) // at least one spawn of type CHEST if required
			return addMissingSpawn(SpawnType.Chest, 0, 1);
		return "";
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
		
		String status = "<font color=ac9887>Top score count: </font><font color=7f7f7f>" + top + "</font>";
		
		return("<table width=510 bgcolor=3E3E3E><tr><td width=510 align=center>" + status + "</td></tr></table>");
	}
	
	@Override
	public boolean isInEvent(CharacterData ch)
	{
		if(ch.isNpc())
		{
			NpcData npc = ch.getNpc();
			if(getChestType(npc) != null)
				return true;
		}
		
		return false;
	}
	
	@Override
	public String getHtmlDescription()
	{
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
	
	@Override
	protected AbstractEventInstance getMatch(int instanceId)
	{
		return _matches.get(instanceId);
	}
	
	@Override
	protected THEventInstance createEventInstance(InstanceData instance)
	{
		return new THEventInstance(instance);
	}
	
	@Override
	protected THData createEventData(int instance)
	{
		return new THData(instance);
	}
	
	@Override
	protected THData getEventData(int instance)
	{
		return _matches.get(instance)._data;
	}
}
