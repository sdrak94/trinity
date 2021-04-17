package cz.nxs.events.engine.main.events;

import java.util.Collections;
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
import cz.nxs.events.engine.stats.GlobalStatsModel;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class VIPTeamVsTeam extends TeamVsTeam
{
	protected class TvTVIPEventData extends TvTEventData
	{
		protected final Map<Integer, List<PlayerEventInfo>> _vips;
		
		public TvTVIPEventData(int instance)
		{
			super(instance);
			
			_vips = new FastMap<Integer, List<PlayerEventInfo>>();
		}
	}
	
	protected class VIPEventInstance extends TvTEventInstance
	{
		protected VIPEventInstance(InstanceData instance)
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
						
						scheduleSelectVips(_instance.getId(), -1, true, false);

						_clock.startClock(_manager.getRunTime());
						
						break;
					}
					case END:
					{
						_clock.setTime(0, true);
						
						for(PlayerEventInfo player : getPlayers(_instance.getId()))
						{
							if(getPlayerData(player).isVip())
								cleanVip(player);
						}
						
						setNextState(EventState.INACTIVE);
						
						if(!instanceEnded() && _canBeAborted)
						{
							if(_canRewardIfAborted)
								rewardAllTeams(_instance.getId(), 0, getInt("killsForReward"));
							
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
	
	private int _vipsCount;
	private int _pointsForKillingVip;
	private int _pointsForKillingNonVip;
	private int _chooseFromTopPercent;
	
	private String _transformId;
	
	private int _healingRadius;
	private int _healingInterval;
	private boolean _healingVisualEffect;
	
	private String _healingPowerHp;
	private String _healingPowerMp;
	private String _healingPowerCp;
	
	private boolean _isHealInPercentHp;
	private boolean _isHealInPercentMp;
	private boolean _isHealInPercentCp;
	
	private int _vipRespawnDelay;
	private Map<Integer, Integer> _skillsForVip;
	
	public VIPTeamVsTeam(EventType type, MainEventManager manager)
	{
		super(type, manager);
		
		setRewardTypes(new RewardPosition[]{ RewardPosition.Winner, RewardPosition.Looser, RewardPosition.Tie, RewardPosition.FirstBlood, RewardPosition.FirstRegistered, RewardPosition.OnKill, RewardPosition.KillingSpree });
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		addConfig(new ConfigModel("vipsCount", "3", "The number of VIP players in each team."));
		addConfig(new ConfigModel("pointsForKillingVip", "5", "The number of score points obtained by killing a VIP player."));
		addConfig(new ConfigModel("pointsForKillingNonVip", "1", "The number of score points obtained by killing a NON VIP player. Useful when you want this event to be based only on killing VIPs."));
		addConfig(new ConfigModel("chooseVipFromTopPercent", "30", "The VIP players will be randomly selected from the top players (Level or PvPs, depends on <font color=LEVEL>divideToTeamsMethod</font> config) in the team. Use this config to specify (in percent) how many players will be 'marked as TOP'. FOr example, if you set this value to '30' and the team has 100 players, the VIPs will be randomly selected from the top 30 players in the team."));

		addConfig(new ConfigModel("transformationId", "0", "You can specify if the player, who becames VIP, will be transformed into a transformation (eg. Zariche). Use this format to select the transformation ID per each team: <font color=5C8D5F>TEAM_ID</font>-<font color=635694>TRANSFORMATION_ID</font>,<font color=5C8D5F>TEAM_ID</font>-<font color=635694>TRANSFORMATION_ID</font> (eg. <font color=5C8D5F>1</font>-<font color=635694>301</font>,<font color=5C8D5F>2</font>-<font color=635694>302</font> will make team 1 (blue) VIPs to transform into Zariches and team 2 (red) VIPs to transform into Akamanahs). Put 0 to disable this feature."));
		
		addConfig(new ConfigModel("minPlayers", "4", "The minimum count of players required to start one instance of the event. <font color=FF0000>Minimum 4 is required for this event, otherwise this event will not start!</font>"));
		
		addConfig(new ConfigModel("vipHealRadius", "800", "The max. radius in which the VIP player can heal all nearby players. Each player can be healed only by one VIP."));
		addConfig(new ConfigModel("healInterval", "3", "Put here how often will the player be healed by the VIP (HP/MP/CP heal). Value in seconds - setting it to eg. 3 will heal player each 3 seconds, if he's standing near the VIP. Put 0 to turn the healing off."));
		addConfig(new ConfigModel("healVisualEffect", "true", "Put true to show some visual effects for players standing near the VIP. Works only if teams count = 2.", InputType.Boolean));
		
		addConfig(new ConfigModel("vipHpHealPower", "0.5%", "Put here how much will the player's HP be healed, if the player stands near his team's VIP. Value can be a decimal and can also end with % - that will make the value in percent."));
		addConfig(new ConfigModel("vipMpHealPower", "1%", "Put here how much will the player's MP be healed, if the player stands near his team's VIP. Value can be a decimal and can also end with % - that will make the value in percent."));
		addConfig(new ConfigModel("vipCpHealPower", "10", "Put here how much will the player's CP be healed, if the player stands near his team's VIP. Value can be a decimal and can also end with % - that will make the value in percent."));
		
		addConfig(new ConfigModel("vipSpecialSkills", "395-1,396-1,1374-1,1375-1,1376-1,7065-1", "You can specify which skills will be given to all VIPs here. Format - SKILLID-LEVEL (eg. 25-2 (skill id 25, lvl 2). Default: All hero skills <font color=4f4f4f>(395, 396, 1374, 1375, 1376)</font>; custom skill to slow to 110 speed + lower the power of heal skills done on the VIP (by 75%) + raise max CP (+30000) + CP reg rate (x2) <font color=4f4f4f>(7065)</font>.", InputType.MultiAdd));
		
		addConfig(new ConfigModel("vipRespawnDelay", "10", "You can specify the delay after which new VIPs will be selected, if the old vips died. In seconds."));
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		
		_vipsCount = getInt("vipsCount");
		_pointsForKillingVip = getInt("pointsForKillingVip");
		_pointsForKillingNonVip = getInt("pointsForKillingNonVip");
		_chooseFromTopPercent = getInt("chooseVipFromTopPercent");
		
		_transformId = getString("transformationId");
		
		_healingRadius = getInt("vipHealRadius");
		_healingInterval = getInt("healInterval");
		
		if(_teamsCount == 2)
			_healingVisualEffect = getBoolean("healVisualEffect");
		else
			_healingVisualEffect = false;
		
		_healingPowerHp = getString("vipHpHealPower");
		_healingPowerMp = getString("vipMpHealPower");
		_healingPowerCp = getString("vipCpHealPower");
		
		_isHealInPercentHp = _healingPowerHp.endsWith("%");
		_isHealInPercentMp = _healingPowerMp.endsWith("%");
		_isHealInPercentCp = _healingPowerCp.endsWith("%");
		
		_vipRespawnDelay = getInt("vipRespawnDelay") * 1000;
		
		String skills = getString("vipSpecialSkills");
		
		if(skills != null && !skills.isEmpty())
		{
			_skillsForVip = new FastMap<Integer, Integer>();
			for(String skill : skills.split(","))
			{
				try
				{
					_skillsForVip.put(Integer.parseInt(skill.split("-")[0]), Integer.parseInt(skill.split("-")[1]));
				}
				catch (Exception e)
				{
					NexusLoader.debug("Wrong format for the vipSpecialSkills config of TvTA event.", Level.WARNING);
					
					e.printStackTrace();
					_skillsForVip = null;
					
					break;
				}
			}
		}
	}
	
	@Override
	public void runEvent()
	{
		super.runEvent();
	}
	
	private void scheduleSelectVips(int instance, int teamId, boolean eventStart, boolean shortDelay)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: scheduling select vips for team id " + teamId + " in instance " + instance + ". event start = " + eventStart + ", short delay = " + shortDelay);
		
  		if(eventStart)
			announce(instance, LanguageEngine.getMsg("vip_selectNew", _vipRespawnDelay/1000));
		
		//pi.screenMessage(msg, getEvent().getEventName(), false);
		
		int delay = _vipRespawnDelay;
		if(shortDelay)
			delay /= 2;
		
		CallBack.getInstance().getOut().scheduleGeneral(new SelectVipsTask(instance, teamId), delay);
	}
	
	private class SelectVipsTask implements Runnable
	{
		final int instance, teamId;
		public SelectVipsTask(int instance, int teamId)
		{
			this.instance = instance;
			this.teamId = teamId;
		}
		
		@Override
		public void run()
		{
			if(_matches.get(instance)._state == EventState.END)
			{
				try
				{
					selectVips(instance, teamId);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					/**/ if(NexusLoader.detailedDebug) print("Event: error while selecting new vips: " + NexusLoader.getTraceString(e.getStackTrace()));
					announce("Sorry, an error occured in this event.");
					clearEvent();
				}
			}
		}
	}
	
	protected synchronized void selectVips(int instanceId, int teamId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: selecting VIPs of instanceId " + instanceId + " for team " + teamId); 

		List<PlayerEventInfo> newVips = new FastList<PlayerEventInfo>();
		List<PlayerEventInfo> temp = new FastList<PlayerEventInfo>();
		List<PlayerEventInfo> possibleVips = new FastList<PlayerEventInfo>();
		int currentVipsCount;
		
		for(EventTeam team : _teams.get(instanceId).values())
		{
			if((teamId != -1 && teamId != team.getTeamId()) || team.getPlayers().isEmpty())
				continue;
			
			currentVipsCount = 0;
			for(PlayerEventInfo player : team.getPlayers())
			{
				if(getPlayerData(player).isVIP)
				{
					currentVipsCount ++;
					continue;
				}
				
				temp.add(player);
			}
			
			int count = _vipsCount - currentVipsCount;
			
			/**/ if(NexusLoader.detailedDebug) print("Event: selecting vips: team " + team.getTeamName() + "(" + team.getTeamId() + ") needs " + count + " VIPs.");
			
			String s = getString("divideToTeamsMethod");
			
			Collections.sort(temp, EventManager.getInstance().compareByLevels);
			if(s.startsWith("PvPs"))
				Collections.sort(temp, EventManager.getInstance().compareByPvps);
			
			int from = 0;
			int to = (int) Math.ceil(temp.size() * ((double)_chooseFromTopPercent / (double)100));
			
			int i = 0;
			while(count > 0 && i < temp.size())
			{
				possibleVips = temp.subList(from, Math.min(to + i, temp.size()));
				Collections.shuffle(possibleVips);
				
				for(PlayerEventInfo possibleVip : possibleVips)
				{
					if(possibleVip != null && !possibleVip.isDead() && !possibleVip.isAfk() && !getPlayerData(possibleVip).wasVIP)
					{
						temp.remove(possibleVip);
						newVips.add(possibleVip);
						count --;
						
						if(count <= 0)
							break;
					}
				}
				
				i++;
			}
			
			/**/ if(NexusLoader.detailedDebug) print("Event: selecting vips part 2, count = " + count); 
			
			//All players from the team have been VIPs already
			if(count > 0)
			{
				for(PlayerEventInfo player : temp)
				{
					getPlayerData(player).wasVIP = false;
				}
				
				from = 0;
				to = (int) Math.ceil(temp.size() * ((double)_chooseFromTopPercent / (double)100));
				
				i = 0;
				while(count > 0 && i < temp.size())
				{
					possibleVips = temp.subList(from, Math.min(to + i, temp.size()));
					Collections.shuffle(possibleVips);
					
					for(PlayerEventInfo possibleVip : possibleVips)
					{
						if(possibleVip != null && !possibleVip.isDead() && !possibleVip.isAfk() && !getPlayerData(possibleVip).wasVIP)
						{
							temp.remove(possibleVip);
							newVips.add(possibleVip);
							count --;
							
							if(count <= 0)
								break;
						}
					}
					
					i++;
				}
			}
			
			/**/ if(NexusLoader.detailedDebug) print("Event: selecting vips part 3, count = " + count); 
			
			// all players from the team are dead and cannot currently become a VIP - schedule next call of this method in 10 seconds
			if(count > 0)
			{
				scheduleSelectVips(instanceId, team.getTeamId(), false, true);
			}
			
			temp.clear();
		}
		
		EventSpawn spawn;
		Loc loc;
		for(PlayerEventInfo player : newVips)
		{
			markVip(player);
			
			spawn = getSpawn(SpawnType.VIP, player.getTeamId());
			
			if(spawn == null)
			{
				NexusLoader.debug("Missing spawn VIP for team " + (_teams.get(instanceId).size() == 1 ? -1 : player.getTeamId()) + ", map " + _manager.getMap().getMapName() + ", event " + getEventType().getAltTitle() + " !!", Level.SEVERE);
				// and let's throw an npe - qq stupid gms
			}
			
			loc = new Loc(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ());
			loc.addRadius(spawn.getRadius());
			
			player.teleport(loc, 0, true, instanceId);
		}
	}
	
	private void transform(PlayerEventInfo player)
	{
		if(_transformId == null || _transformId.equals("0"))
			return;
		
		int id = 0;
		try
		{
			id = Integer.parseInt(_transformId);
		}
		catch (Exception e) { id = 0;}
		
		if(id > 0)
		{
			player.transform(id);
			return;
		}
		else
		{
			String[] s = _transformId.split(",");
			
			for(String d : s)
			{
				try
				{
					if(Integer.parseInt(d.split("-")[0]) == player.getTeamId())
					{
						player.transform(Integer.parseInt(d.split("-")[1]));
					}
				}
				catch (Exception e) {}
			}
		}
	}
	
	protected void markVip(PlayerEventInfo player)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: marking " + player.getPlayersName() + " in instance " + player.getInstanceId() + " as VIP.");

		if(!getPlayerData(player).isVip())
		{
			transform(player);
			
			vipSkills(player, true);
			player.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
			
			getPlayerData(player).setVIP(true);
			
			if(!getEventData(player.getInstanceId())._vips.containsKey(player.getTeamId()))
				getEventData(player.getInstanceId())._vips.put(player.getTeamId(), new FastList<PlayerEventInfo>());
			
			getEventData(player.getInstanceId())._vips.get(player.getTeamId()).add(player);
			
			player.setTitle(getTitle(player), true);
			player.broadcastTitleInfo();
			
			announce(player.getInstanceId(), "* " + LanguageEngine.getMsg("vip_becomeVip", player.getPlayersName()), player.getTeamId());
		}
	}
	
	protected void vipSkills(PlayerEventInfo player, boolean add)
	{
		if(_skillsForVip != null)
		{
			for(Entry<Integer, Integer> sk : _skillsForVip.entrySet())
			{
				SkillData skill = new SkillData(sk.getKey(), sk.getValue());
				
				if(add)
					player.addSkill(skill, false);
				else
					player.removeSkill(skill.getId());
			}
		}
		if(add)
		{
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
		}
	}
	
	protected void cleanVip(PlayerEventInfo player)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: removing/cleaning " + player.getPlayersName() + " in instance " + player.getInstanceId() + " from VIP.");
		
		if(getPlayerData(player).isVip())
		{
			vipSkills(player, false);
			player.untransform(true);
			
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REAL_TARGET());
			
			getPlayerData(player).setVIP(false);
			
			if(getEventData(player.getInstanceId())._vips.containsKey(player.getTeamId()))
				getEventData(player.getInstanceId())._vips.get(player.getTeamId()).remove(player);
			
			player.setTitle(getTitle(player), true);
			player.broadcastTitleInfo();
		}
	}
	
	@Override
	public void onEventEnd()
	{
		super.onEventEnd();
	}

	@Override
	protected String getTitle(PlayerEventInfo pi)
	{
		if(_hideTitles)
			return "";
		
		if(getPlayerData(pi).isVip())
			return "[VIP]";
		
		if(pi.isAfk())
			return "AFK";
		
		return LanguageEngine.getMsg("event_title_scoredeath", getPlayerData(pi).getScore(), getPlayerData(pi).getDeaths());
	}
	
	public VIPTvTPlayerData data;
	
	@Override
	public void onKill(PlayerEventInfo player, CharacterData target)
	{
		if(target.getEventInfo() == null)
			return;
		
		PlayerEventInfo targetInfo = target.getEventInfo();
		
		if (player.getTeamId() != targetInfo.getTeamId())
		{
			tryFirstBlood(player);
			
			// target is VIP
			if(getPlayerData(targetInfo).isVip())
			{
				giveOnKillReward(player);
				
				// update player's team's stats
				player.getEventTeam().raiseScore(_pointsForKillingVip);
				player.getEventTeam().raiseKills(_pointsForKillingVip);
				
				// update player's own stats
				getPlayerData(player).raiseScore(_pointsForKillingVip);
				getPlayerData(player).raiseKills(_pointsForKillingVip);
				getPlayerData(player).raiseSpree(1);
				
				giveKillingSpreeReward(getPlayerData(player));
				
				CallbackManager.getInstance().playerKillsVip(getEventType(), player, target.getEventInfo());
			}
			else
			{
				giveOnKillReward(player);
				
				// update player's team's stats
				player.getEventTeam().raiseScore(_pointsForKillingNonVip);
				player.getEventTeam().raiseKills(_pointsForKillingNonVip);
				
				// update player's own stats
				getPlayerData(player).raiseScore(_pointsForKillingNonVip);
				getPlayerData(player).raiseKills(_pointsForKillingNonVip);
				getPlayerData(player).raiseSpree(1);
				
				giveKillingSpreeReward(getPlayerData(player));
				
				CallbackManager.getInstance().playerKills(getEventType(), player, target.getEventInfo());
			}
			
			// update title
			if(player.isTitleUpdated())
			{
				player.setTitle(getTitle(player), true);
				player.broadcastTitleInfo();
			}
			
			// update Stats table
			setScoreStats(player, getPlayerData(player).getScore());
			setKillsStats(player, getPlayerData(player).getKills());
		}
		
		if(getPlayerData(targetInfo).isVip())
		{
			// remove target's VIP status
			announceToAllTeamsBut(targetInfo.getInstanceId(), "[+] " + LanguageEngine.getMsg("vip_vipDied", targetInfo.getPlayersName(), targetInfo.getEventTeam().getTeamName()), targetInfo.getTeamId());
			announce(targetInfo.getInstanceId(), "[-] " + LanguageEngine.getMsg("vip_vipKilled", targetInfo.getPlayersName()), targetInfo.getTeamId());
			
			cleanVip(targetInfo);
			scheduleSelectVips(targetInfo.getInstanceId(), targetInfo.getTeamId(), false, false);
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
	public int allowTransformationSkill(PlayerEventInfo playerEventInfo, SkillData skillData)
	{
		if(_skillsForVip.containsKey(skillData.getId()))
			return 1;
		else 
			return 0;
	}
	
	@Override
	public void playerWentAfk(PlayerEventInfo player, boolean warningOnly, int afkTime)
	{
		if(warningOnly)
		{
			player.sendMessage(LanguageEngine.getMsg("event_afkWarning_kill", (PlayerEventInfo.AFK_WARNING_DELAY / 1000), (PlayerEventInfo.AFK_KICK_DELAY /1000)));
		}
		else
		{
			if(_matches.get(player.getInstanceId())._state == EventState.END)
			{
				if(getPlayerData(player).isVIP)
				{
					announce(player.getInstanceId(), "* " + LanguageEngine.getMsg("vip_vipAfk", player.getPlayersName()), player.getTeamId());
					announceToAllTeamsBut(player.getInstanceId(), "* " + LanguageEngine.getMsg("vip_enemyVipAfk", player.getPlayersName()), player.getTeamId());
					player.doDie();
				}
			}
		} 
	}
	
	@Override
	public EventPlayerData createPlayerData(PlayerEventInfo player)
	{
		return new VIPTvTPlayerData(player, this);
	}

	@Override
	public VIPTvTPlayerData getPlayerData(PlayerEventInfo player)
	{
		return (VIPTvTPlayerData) player.getEventData();
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
						
						for(PlayerEventInfo player : getPlayers(match.getInstance().getId()))
						{
							if(getPlayerData(player).isVip())
							{
								cleanVip(player);
							}
						}
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
			
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_IMPRISIONING_1());
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REDCIRCLE());
			
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
	public void onDisconnect(PlayerEventInfo player)
	{
		if(getPlayerData(player).isVip())
		{
			cleanVip(player);
			announce(player.getInstanceId(), "* " + LanguageEngine.getMsg("vip_vipDisconnected", player.getPlayersName(), player.getEventTeam().getTeamName()));
			scheduleSelectVips(player.getInstanceId(), player.getTeamId(), false, true);
		}
		
		super.onDisconnect(player);
	}
	
	@Override
	protected boolean checkIfEventCanContinue(int instanceId, PlayerEventInfo disconnectedPlayer)
	{
		int teamsOn = 0;
		int temp;
		
		for(EventTeam team : _teams.get(instanceId).values())
		{
			temp = 0;
			for(PlayerEventInfo pi : team.getPlayers())
			{
				if(pi != null && pi.isOnline())
				{
					temp ++;
				}
			}
			
			if(temp >= 2) // at least two players are required from one team
				teamsOn ++;
		}
		
		return teamsOn >= 2;
	}
	
	@Override
	protected void respawnPlayer(PlayerEventInfo pi, int instance)
	{
		/**/ if(NexusLoader.detailedDebug) print("/// Event: respawning player " + pi.getPlayersName() + ", instance " + instance);
		
		EventSpawn spawn;
		
		if(getPlayerData(pi).isVip())
			spawn = getSpawn(SpawnType.VIP, pi.getTeamId());
		else
			spawn = getSpawn(SpawnType.Regular, pi.getTeamId());
		
		if(spawn != null)
		{
			Loc loc = new Loc(spawn.getLoc().getX(), spawn.getLoc().getY(), spawn.getLoc().getZ());
			loc.addRadius(spawn.getRadius());
			
			pi.teleport(loc, 0, true, instance);
			
			pi.sendMessage(LanguageEngine.getMsg("event_respawned"));
		}
		else
		{
			//abortEvent("An error occured with Event Map.");
			debug("Error on respawnPlayer - no spawn type REGULAR or VIP, team " + pi.getTeamId() + " has been found. Event aborted.");
		}
	}
	
	int tick = 0;
	List<PlayerEventInfo> playersEffects = new FastList<PlayerEventInfo>();
	
	@Override
	protected void clockTick()
	{
		final int healingRadius = (int) Math.pow(_healingRadius, 2);
		
		tick ++;
		
		int teamId;
		double value;
		for(TvTEventInstance match : _matches.values())
		{
			for(Entry<Integer, List<PlayerEventInfo>> e : getEventData(match.getInstance().getId())._vips.entrySet())
			{
				teamId = e.getKey();
				
				for(PlayerEventInfo vip : e.getValue())
				{
					for(PlayerEventInfo player : getPlayers(match.getInstance().getId()))
					{
						if(player.getTeamId() == teamId && !getPlayerData(player).isVIP && getPlayerData(player).canHeal() && !player.isDead())
						{
							if(player.getPlanDistanceSq(vip.getX(), vip.getY()) <= healingRadius)
							{
								if(_healingVisualEffect)
								{
									playersEffects.add(player);
								}
								
								if(_healingInterval > 0 && tick % (_healingInterval) == 0)
								{
									// HP heal for nearby players
									if(_isHealInPercentHp)
									{
										value = Double.parseDouble(_healingPowerHp.substring(0, _healingPowerHp.length() - 1));
										if(value > 0 && player.getCurrentHp() < player.getMaxHp())
										{
											value = (player.getMaxHp()/100*value);
											player.setCurrentHp((int) (player.getCurrentHp() + value));
										}
									}
									else
									{
										value = Double.parseDouble(_healingPowerHp);
										if(value > 0 && player.getCurrentHp() < player.getMaxHp())
											player.setCurrentHp((int) (player.getCurrentHp() + value));
									}
									
									// MP heal for nearby players
									if(_isHealInPercentMp)
									{
										value = Double.parseDouble(_healingPowerMp.substring(0, _healingPowerMp.length() - 1));
										if(value > 0 && player.getCurrentMp() < player.getMaxMp())
										{
											value = (player.getMaxMp()/100*value);
											player.setCurrentMp((int) (player.getCurrentMp() + value));
										}
									}
									else
									{
										value = Double.parseDouble(_healingPowerMp);
										if(value > 0 && player.getCurrentMp() < player.getMaxMp())
											player.setCurrentMp((int) (player.getCurrentMp() + value));
									}
									
									// CP heal for nearby players
									if(_isHealInPercentCp)
									{
										value = Double.parseDouble(_healingPowerCp.substring(0, _healingPowerCp.length() - 1));
										if(value > 0 && player.getCurrentCp() < player.getMaxCp())
										{
											value = (player.getMaxCp()/100*value);
											player.setCurrentCp((int) (player.getCurrentCp() + value));
										}
									}
									else
									{
										value = Double.parseDouble(_healingPowerCp);
										if(value > 0 && player.getCurrentCp() < player.getMaxCp())
											player.setCurrentCp((int) (player.getCurrentCp() + value));
									}
								}
							}
						}
					}
				}
			}
			
			for(PlayerEventInfo player : getPlayers(match.getInstance().getId()))
			{
				getPlayerData(player).tickEnd();
				
				if(playersEffects.contains(player) || getPlayerData(player).isVip())
				{
					startPlayerEffects(player, player.getTeamId());
				}
				else
				{
					startPlayerEffects(player, 0);
				}
			}
			playersEffects.clear();
		}
	}
	
	private void startPlayerEffects(PlayerEventInfo player, int teamId)
	{
		if(teamId == 1)
		{
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REDCIRCLE()); // red circle effect
			player.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_IMPRISIONING_1()); // imprissing 1 effect
		}
		else if(teamId == 2)
		{
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_IMPRISIONING_1()); // imprissing 1 effect
			player.startAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REDCIRCLE()); // red circle effect
		}
		else // stop effects
		{
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_REDCIRCLE()); // red circle effect
			player.stopAbnormalEffect(CallBack.getInstance().getValues().ABNORMAL_IMPRISIONING_1()); // imprissing 1 effect
		}
	}
	
	public class VIPTvTPlayerData extends PvPEventPlayerData
	{
		protected boolean	isVIP		= false;
		protected boolean	wasVIP		= false;
		protected boolean	tickHealed	= false;
		public VIPTvTPlayerData(PlayerEventInfo owner, EventGame event)
		{
			super(owner, event, new GlobalStatsModel(getEventType()));
		}
		
		public boolean isVip()
		{
			return isVIP;
		}
		
		public boolean wasVIP()
		{
			return wasVIP;
		}
		
		public void setVIP(boolean b)
		{
			if(isVIP && !b)
				wasVIP = true;
			
			isVIP = b;
		}
		
		public boolean canHeal()
		{
			if(tickHealed)
				return false;
			else
				tickHealed = true;
			return true;
		}
		
		public void tickEnd()
		{
			tickHealed = false;
		}
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
	
	@Override
	public String getMissingSpawns(EventMap map)
	{
		TextBuilder tb = new TextBuilder();
		
		for(int i = 0; i < getTeamsCount(); i++)
		{
			if(!map.checkForSpawns(SpawnType.Regular, (i+1), 1))
				tb.append(addMissingSpawn(SpawnType.Regular, i+1, 1));
			
			if(!map.checkForSpawns(SpawnType.VIP, (i+1), 1))
				tb.append(addMissingSpawn(SpawnType.VIP, i+1, 1));
		}
		
		return tb.toString();
	}
	
	@Override
	protected TvTEventData createEventData(int instanceId)
	{
		return new TvTVIPEventData(instanceId);
	}
	
	@Override
	protected VIPEventInstance createEventInstance(InstanceData instance)
	{
		return new VIPEventInstance(instance);
	}
	
	@Override
	protected TvTVIPEventData getEventData(int instance)
	{
		return (TvTVIPEventData) _matches.get(instance)._data;
	}
}
