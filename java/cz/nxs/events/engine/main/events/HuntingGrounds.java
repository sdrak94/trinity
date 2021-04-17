package cz.nxs.events.engine.main.events;

import java.util.Map.Entry;
import java.util.logging.Level;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.base.description.EventDescription;
import cz.nxs.events.engine.base.description.EventDescriptionSystem;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.MainEventManager;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.delegate.ItemData;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class HuntingGrounds extends TeamVsTeam
{
	protected class HGEventData extends TvTEventData
	{
		public HGEventData(int instance)
		{
			super(instance);
		}
	}
	
	protected class HGEventInstance extends TvTEventInstance
	{
		protected HGEventInstance(InstanceData instance)
		{
			super(instance);
		}
		
		@SuppressWarnings("incomplete-switch")
		@Override
		public void run()
		{
			try
			{
				/**/ if(NexusLoader.detailedDebug) print("Event: running task of state " + _state.toString() + "...");
				
				switch (_state)
				{
					case START:
					{
						if(checkPlayers(_instance.getId()))
						{
							teleportPlayers(_instance.getId(), SpawnType.Regular, false);
							
							setupTitles(_instance.getId());
							
							// action done for some hardcoded maps
							removeStaticDoors(_instance.getId());
							
							enableMarkers(_instance.getId(), true);
							
							handleWeapons(_instance.getId(), true);
							handleSkills(_instance.getId(), true);
							preparePlayers(_instance.getId(), true);
							
							forceSitAll(_instance.getId());
							
							setNextState(EventState.FIGHT);
							scheduleNextTask(10000);
						}
						
						break;
					}
					case FIGHT:
					{
						forceStandAll(_instance.getId());
						
						if(getBoolean("createParties"))
							createParties(getInt("maxPartySize"));
						
						setNextState(EventState.END);

						_clock.startClock(_manager.getRunTime());
						
						break;
					}
					case END:
					{
						_clock.setTime(0, true);
						
						setNextState(EventState.INACTIVE);
						
						if(!instanceEnded() && _canBeAborted)
						{
							if(_canRewardIfAborted)
								rewardAllTeams(_instance.getId(), getInt("killsForReward"), getInt("killsForReward"));
							
							clearEvent(_instance.getId());
						}
							
						break;
					}
				}
				
				/**/ if(NexusLoader.detailedDebug) print("Event: ... finished running task. next state " + _state.toString());
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				_manager.endDueToError(LanguageEngine.getMsg("event_error"));
			}
		}
	}
	
	protected int _tick;
	
	protected int _bowItemId;
	protected int _arrowItemId;
	
	protected boolean _ammoSystem;
	protected int _ammoAmmount;
	protected int _ammoRegPerTick;
	protected int _tickLength;
	
	private FastMap<Integer, Integer> _skillsForAll;

	public HuntingGrounds(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Winner, RewardPosition.Looser, RewardPosition.Tie, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.OnKill, RewardPosition.KillingSpree });
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		addConfig(new ConfigModel("skillsForAllPlayers", "35100-1", "IDs of skills which will be given to players on the event. The purpose of this is to make all players equally strong. Format: <font color=LEVEL>SKILLID-LEVEL</font> (eg. '35000-1').", InputType.MultiAdd));
		addConfig(new ConfigModel("bowWeaponId", "271", "The ID of the bow item which will be given to all players and will be the only weapon most players will use during the event. This weapon kills players with just one hit."));
		addConfig(new ConfigModel("arrowItemId", "17", "The ID of the arrows which will be given to the player in the event."));
		
		addConfig(new ConfigModel("enableAmmoSystem", "true", "Enable/disable the ammo system based on player's mana. Player's max MP is defaultly modified by a custom passive skill and everytime a player shots and arrow, his MP decreases by a value which is calculated from the ammount of ammo. There is also a MP regeneration system - see the configs below.", InputType.Boolean));
		addConfig(new ConfigModel("ammoAmmount", "10", "Works if ammo system is enabled. Specifies the max ammount of ammo every player can have."));
		addConfig(new ConfigModel("ammoRestoredPerTick", "1", "Works if ammo system is enabled. Defines the ammount of ammo given to every player each <font color=LEVEL>'ammoRegTickInterval'</font> (configurable) seconds."));
		addConfig(new ConfigModel("ammoRegTickInterval", "10", "Works if ammo system is enabled. Defines the interval of restoring player's ammo. The value is in seconds (eg. value 10 will give ammo every 10 seconds to every player - the ammount of restored ammo is configurable (config <font color=LEVEL>ammoRestoredPerTick</font>)."));
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		
		_bowItemId = getInt("bowWeaponId");
		_arrowItemId = getInt("arrowItemId");
		
		_ammoSystem = getBoolean("enableAmmoSystem");
		_ammoAmmount = getInt("ammoAmmount");
		_ammoRegPerTick = getInt("ammoRestoredPerTick");
		_tickLength = getInt("ammoRegTickInterval");
		
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
		
		_tick = 0;
	}
	
	protected void preparePlayers(int instanceId, boolean start)
	{
		for (PlayerEventInfo player : getPlayers(instanceId))
			player.removeCubics();
	}
	
	protected void handleSkills(int instanceId, boolean add)
	{
		if(_skillsForAll != null)
		{
			SkillData skill = null;
			
			for(PlayerEventInfo player : getPlayers(instanceId))
			{
				if(add)
				{
					for(Entry<Integer, Integer> e : _skillsForAll.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						
						if(skill.exists())
							player.addSkill(skill, false);
					}
					
					player.sendSkillList();
				}
				else
				{
					for(Entry<Integer, Integer> e : _skillsForAll.entrySet())
					{
						skill = new SkillData(e.getKey(), e.getValue());
						if(skill.exists())
							player.removeSkill(skill.getId());
					}
				}
			}
		}
	}
	
	protected void handleWeapons(int instanceId, boolean equip)
	{
		for(PlayerEventInfo player : getPlayers(instanceId))
		{
			if(equip)
			{
				ItemData wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
				if (wpn != null)
					player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_R_HAND());

				wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_LHAND());
				if (wpn != null)
					player.unEquipItemInBodySlotAndRecord(CallBack.getInstance().getValues().SLOT_L_HAND());

				ItemData flagItem = player.addItem(_bowItemId, 1, false);
				player.equipItem(flagItem);
				
				player.addItem(_arrowItemId, 400, false);
			}
			else
			{
				ItemData wpn = player.getPaperdollItem(CallBack.getInstance().getValues().PAPERDOLL_RHAND());
				if (wpn.exists())
				{
					ItemData[] unequiped = player.unEquipItemInBodySlotAndRecord(wpn.getBodyPart());
					player.destroyItemByItemId(_bowItemId, 1);
					
					player.inventoryUpdate(unequiped);
				}
			}
		}
	}
	
	@Override
	protected void clockTick() 
	{
		_tick ++;
		if(_tick % _tickLength != 0)
			return;
		
		if(_ammoSystem)
		{
			int toAdd, currentMp, mpToRegenerate;
			int oneAmmoMp = 0;
			
			for(TvTEventInstance match : _matches.values())
			{
				for(PlayerEventInfo player : getPlayers(match.getInstance().getId()))
				{
					try
					{
						oneAmmoMp = player.getMaxMp() / _ammoAmmount;
						mpToRegenerate = _ammoRegPerTick * oneAmmoMp;
						currentMp = (int) player.getCurrentMp();
						
						if(currentMp < player.getMaxMp())
						{
							toAdd = mpToRegenerate;
							
							if(currentMp + mpToRegenerate > player.getMaxMp())
								toAdd = player.getMaxMp() - currentMp;
							
							player.setCurrentMp(currentMp + toAdd);
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
				}
			}
		}
	}
	
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null)
			return;
		
		if (player.getTeamId() != target.getEventInfo().getTeamId())
		{
			tryFirstBlood(player);
			giveOnKillReward(player);
			
			// update player's team's stats
			player.getEventTeam().raiseScore(1);
			player.getEventTeam().raiseKills(1);
			
			// update player's own stats
			getPlayerData(player).raiseScore(1);
			getPlayerData(player).raiseKills(1);
			getPlayerData(player).raiseSpree(1);
			
			giveKillingSpreeReward(getPlayerData(player));
			
			// update title
			if(player.isTitleUpdated())
			{
				player.setTitle(getTitle(player), true);
				player.broadcastTitleInfo();
			}
			
			CallbackManager.getInstance().playerKills(getEventType(), player, target.getEventInfo());
			
			// update Stats table
			setScoreStats(player, getPlayerData(player).getScore());
			setKillsStats(player, getPlayerData(player).getKills());
		}
	}

	@Override
	public void onDie(PlayerEventInfo player, CharacterData killer)
	{
		/**/ if(NexusLoader.detailedDebug) print("/// Event: onDie - player " + player.getPlayersName() + " (instance " + player.getInstanceId() + "), killer " + killer.getName());
		
		getPlayerData(player).raiseDeaths(1);
		getPlayerData(player).setSpree(0);
		
		setDeathsStats(player, getPlayerData(player).getDeaths());
		
		if(_waweRespawn)
			_waweScheduler.addPlayer(player);
		else
			scheduleRevive(player, getInt("resDelay") * 1000);
	}
	
	@Override
	public boolean onAttack(CharacterData cha, CharacterData target)
	{
		if(_ammoSystem && cha.isPlayer() && target.isPlayer())
		{
			final PlayerEventInfo player = cha.getEventInfo();
			final int oneShotMp = player.getMaxMp() / _ammoAmmount;
			
			if(player.getCurrentMp() >= oneShotMp)
			{
				player.setCurrentMp((int) (player.getCurrentMp() - oneShotMp));
			}
			else
			{
				player.sendMessage("Not enought MP.");
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean canUseItem(PlayerEventInfo player, ItemData item)
	{
		if(item.getItemId() == _bowItemId && item.isEquipped())
			return false;
		else if(item.isWeapon())
			return false;
		else if (item.isType2Weapon())
			return false;
		else if (item.isType2Armor())
			return false;
		
		return super.canUseItem(player, item);
	}
	
	@Override
	public void onDamageGive(CharacterData cha, CharacterData target, int damage, boolean isDOT)
	{
		try
		{
			if(cha.isPlayer() && target.isPlayer())
			{
				PlayerEventInfo targetPlayer = target.getEventInfo();
				
				targetPlayer.abortCasting();
				targetPlayer.doDie(cha);
			}
		}
		catch (NullPointerException e)
		{
		}
	}
	
	@Override
	public boolean canDestroyItem(PlayerEventInfo player, ItemData item)
	{
		if(item.getItemId() == _bowItemId)
			return false;
		
		return super.canDestroyItem(player, item);
	}
	
	@Override
	public boolean canSupport(PlayerEventInfo player, CharacterData target)
	{
		return false;
	}
	
	@Override
	public boolean canUseSkill(PlayerEventInfo player, SkillData skill)
	{
		return false;
	}
	
	@Override
	public synchronized void clearEvent(int instanceId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: called CLEAREVENT for instance " + instanceId);
		
		try
		{
			if(_matches != null)
			{
				for(TvTEventInstance match : _matches.values())
				{
					if(instanceId == 0 || instanceId == match.getInstance().getId())
					{
						match.abort();
						
						handleWeapons(match.getInstance().getId(), false);
						handleSkills(match.getInstance().getId(), false);
						preparePlayers(match.getInstance().getId(), false);
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
				_htmlDescription = getInt("teamsCount") + " teams fighting against each other. ";
				_htmlDescription += "Gain score by killing your opponents";
				
				if(getInt("killsForReward") > 0)
				{
					_htmlDescription += " (at least " + getInt("killsForReward") + " kill(s) is required to receive a reward)";
				}
				
				if(getBoolean("waweRespawn"))
					_htmlDescription += " and dead players are resurrected by an advanced wawe-spawn engine each " + getInt("resDelay") + " seconds";
				else
					_htmlDescription += " and if you die, you will be resurrected in " + getInt("resDelay") + " seconds";
				
				
				if(getBoolean("createParties"))
					_htmlDescription += ". The event automatically creates parties on start";
				
				_htmlDescription += ".";
			}
		}
		return _htmlDescription;
	}
	
	@Override
	protected TvTEventData createEventData(int instanceId)
	{
		return new HGEventData(instanceId);
	}
	
	@Override
	protected HGEventInstance createEventInstance(InstanceData instance)
	{
		return new HGEventInstance(instance);
	}
	
	@Override
	protected HGEventData getEventData(int instance)
	{
		return (HGEventData) _matches.get(instance)._data;
	}
}
