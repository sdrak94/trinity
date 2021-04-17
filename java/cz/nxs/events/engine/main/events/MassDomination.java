package cz.nxs.events.engine.main.events;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.ConfigModel.InputType;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.SpawnType;
import cz.nxs.events.engine.base.description.EventDescription;
import cz.nxs.events.engine.base.description.EventDescriptionSystem;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.MainEventManager;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.callback.CallbackManager;
import cz.nxs.interf.delegate.NpcData;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * @author hNoke
 *
 */
public class MassDomination extends Domination
{
	protected class MultipleZoneData extends ZoneData
	{
		private int _order;
		private NpcData[] _zones;
		private int[] _radiuses;
		private int[] _holdingTeams;
		private int[] _holdingTimes;
		protected int _holdingAllZonesFor;
		protected int _dominatingTeam;
		
		protected MultipleZoneData(int instance, int zonesCount)
		{
			super(instance);
			
			_zones = new NpcData[zonesCount];
			_radiuses = new int[zonesCount];
			_holdingTeams = new int[zonesCount];
			_holdingTimes = new int[zonesCount];
			_dominatingTeam = 0;
			_holdingAllZonesFor = 0;
			
			_order = 0;
		}
		
		@Override
		protected void addZone(NpcData zone, int radius)
		{
			if(_order < _zonesCount)
			{
				_zones[_order] = zone;
				_radiuses[_order] = radius > 0 ? ((int) Math.pow(radius, 2)) : _zoneRadius;
				_holdingTeams[_order] = 0;
				_holdingTimes[_order] = 0;
				
				_order ++;
			}
			else
				NexusLoader.debug("too many zones for MultipleZoneData (" + _order + "; " + _zonesCount + ")");
		}
	}
	
	// multiple domination vars
	private int _zonesCount;
	private int _zonesToOwn;
	private int _holdZonesFor;

	public MassDomination(EventType type, MainEventManager manager)
	{
		super(type, manager);
	}
	
	@Override
	public void loadConfigs()
	{
		super.loadConfigs();
		
		addConfig(new ConfigModel("countOfZones", "2", "Specifies how many zones will be in the event. In order to get score, one team must own all zones.", 
				InputType.Enum).addEnumOptions(new String[]{"2", "3", "4", "5"}));
		
		addConfig(new ConfigModel("zonesToOwnToScore", "2", "Count of zones the team needs to own in order to score. Obviously must be lower or equal to <font color=LEVEL>countOfZones</font>."));
		
		removeConfig("holdZoneFor");
		removeConfig("scoreForCapturingZone");
		removeConfig("percentMajorityToScore");
		
		addConfig(new ConfigModel("holdZonesFor", "10", "In seconds. The team needs to own <font color=LEVEL>zonesToOwnToScore</font> zones for this time to get <font color=LEVEL>scoreForCapturingZone</font> points. "));
		
		addConfig(new ConfigModel("scoreForCapturingZone", "1", "The ammount of points team gets each <font color=LEVEL>scoreCheckInterval</font> seconds if owns required zone(s)."));
		addConfig(new ConfigModel("percentMajorityToScore", "50", "In percent. In order to score a point, the team must have more players near at least <font color=LEVEL>zonesToOwnToScore</font> zones, than the other team(s). " +
				"The ammount of players from the scoring team must be higher than ammount of players from the other team(s) by this percent value. " +
				"Put 100 to make that all other team(s)' players in <font color=LEVEL>zoneRadius</font> must be dead to score; or put 0 to make that it will give score to the team that has more players and not care about any percent counting (eg. if team A has 15 players and team B has 16, it will simply reward team B).")); // TODO
	}
	
	@Override
	public void initEvent()
	{
		super.initEvent();
		
		_zonesCount = getInt("countOfZones");
		_zonesToOwn = getInt("zonesToOwnToScore");
		_holdZonesFor = getInt("holdZonesFor");
	}
	
	@Override
	protected void spawnZone(int instanceId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: spawning zones for instanceId " + instanceId); 

		clearMapHistory(-1, SpawnType.Zone);
		
		NpcData zone;
		EventSpawn sp;
		for(int i = 0; i < _zonesCount; i++)
		{
			sp = getSpawn(SpawnType.Zone, -1);
			zone = spawnNPC(sp.getLoc().getX(), sp.getLoc().getY(), sp.getLoc().getZ(), _zoneNpcId, instanceId, "Zone " + (i + 1), "Domination event");
			getEventData(instanceId).addZone(zone, sp.getRadius());
		}
	}
	
	@Override
	protected void unspawnZone(int instanceId)
	{
		/**/ if(NexusLoader.detailedDebug) print("Event: unspawning zones for instanceId " + instanceId);

		for(NpcData zoneNpc : getEventData(instanceId)._zones)
		{
			if(zoneNpc != null)
				zoneNpc.deleteMe();
		}
	}
	
	@Override
	protected void clockTick()
	{
		_tick ++;
		if(_tick % _zoneCheckInterval != 0)
			return;
		
		Map<Integer, List<PlayerEventInfo>> players;
		Map<Integer, List<PlayerEventInfo>> playersNearZones;
		List<PlayerEventInfo> playersWithEffects;
		
		int instanceId;
		int zoneX, zoneY, zoneZ;
		int radius;
		
		MultipleZoneData zoneData;
		NpcData zoneNpc;
		
		Map<Integer, List<NpcData>> ownedZones;
		
		for(DominationEventInstance match : _matches.values())
		{
			instanceId = match.getInstance().getId();
			
			zoneData = getEventData(instanceId);
			
			ownedZones = new FastMap<Integer, List<NpcData>>();
			
			playersNearZones = new FastMap<Integer, List<PlayerEventInfo>>(_teamsCount);
			playersWithEffects = new FastList<PlayerEventInfo>();
			
			for(int i = 0; i < zoneData._zones.length; i++)
			{
				players = new FastMap<Integer, List<PlayerEventInfo>>(_teamsCount);
				
				zoneNpc = zoneData._zones[i];
				radius = zoneData._radiuses[i];
				
				zoneX = zoneNpc.getLoc().getX();
				zoneY = zoneNpc.getLoc().getY();
				zoneZ = zoneNpc.getLoc().getZ();
				
				// calculate players near the zone
				for(PlayerEventInfo player : getPlayers(instanceId))
				{
					if(player.getDistanceSq(zoneX, zoneY, zoneZ) <= radius)
					{
						if(player.isVisible() && !player.isDead())
						{
							if(!players.containsKey(player.getTeamId()))
								players.put(player.getTeamId(), new FastList<PlayerEventInfo>());
							
							if(!playersNearZones.containsKey(player.getTeamId()))
								playersNearZones.put(player.getTeamId(), new FastList<PlayerEventInfo>());
							
							players.get(player.getTeamId()).add(player);
							playersNearZones.get(player.getTeamId()).add(player);
						}
					}
				}
				
				
				// get the team with most players near the zone
				int highestCount = 0;
				int team = 0;
				boolean isThereMajorityTeam = true;
				
				for(Entry<Integer, List<PlayerEventInfo>> teamData : players.entrySet())
				{
					if(teamData.getValue().size() > highestCount)
					{
						highestCount = teamData.getValue().size();
						team = teamData.getKey();
					}
					
					// more teams have the same count of players in near the zone
					else if(highestCount != 0 && teamData.getValue().size() == highestCount)
					{
						isThereMajorityTeam = false;
						break;
					}
				}
				
				// if such team exists..
				if(isThereMajorityTeam && team != 0)
				{
					// calculate if the team really owns zone based on configs provided
					boolean ownsZone = false;
					
					// don't care about percents, this team already has more players than the others
					if(_percentMajorityToScore == 0)
					{
						ownsZone = true;
					}
					
					// check if there are only the team's players in the zone and nobody else
					else if(_percentMajorityToScore == 100)
					{
						boolean teamWithMorePlayers = false;
						for(Entry<Integer, List<PlayerEventInfo>> teamData : players.entrySet())
						{
							if(teamData.getKey() == team)
								continue;
							
							if(teamData.getValue().size() > 0)
							{
								teamWithMorePlayers = true;
								break;
							}
						}
						
						if(!teamWithMorePlayers)
							ownsZone = true;
					}
					
					// else calculate and check percentual majority
					else
					{
						int majorityTeamPlayers = players.get(team).size();
						boolean teamWithMorePlayers = false;
						
						for(Entry<Integer, List<PlayerEventInfo>> teamData : players.entrySet())
						{
							if(teamData.getKey() == team)
								continue;
							
							double d = ((double) teamData.getValue().size() / (double) majorityTeamPlayers);
							int percent = 100 - ((int) (d * 100));
							
							if(percent < _percentMajorityToScore)
							{
								teamWithMorePlayers = true;
								break;
							}
						}
						
						if(!teamWithMorePlayers)
							ownsZone = true;
					}
					
					if(ownsZone)
					{
						// set the owner of the zone and raise owning time
						if(zoneData._holdingTeams[i] != team)
						{
							announce(instanceId, LanguageEngine.getMsg("mDom_gainedZone", _teams.get(instanceId).get(team).getFullName(), (i+1)));
							
							zoneNpc.getNpc().setTitle(LanguageEngine.getMsg("dom_npcTitle_owner", _teams.get(instanceId).get(team).getTeamName()));
							zoneNpc.getNpc().broadcastNpcInfo();
							
							zoneData._holdingTeams[i] = team;
							zoneData._holdingTimes[i] = 0;
							
							setZoneEffects(team, zoneNpc);
						}
						else
						{
							zoneData._holdingTimes[i] += _zoneCheckInterval;
						}
						
						// effects
						if(getBoolean("allowPlayerEffects") && _teamsCount == 2)
						{
							for(PlayerEventInfo player : _teams.get(instanceId).get(team).getPlayers())
							{
								if(player.getDistanceSq(zoneX, zoneY, zoneZ) <= radius && player.isVisible() && !player.isDead())
									playersWithEffects.add(player);
							}
						}
						
						// add this zone to the team's owned zones
						if(!ownedZones.containsKey(team))
							ownedZones.put(team, new FastList<NpcData>());
						
						ownedZones.get(team).add(zoneNpc);
					}
					else
					{
						if(zoneData._holdingTeams[i] != 0)
						{
							announce(instanceId, LanguageEngine.getMsg("mDom_lostZone", _teams.get(instanceId).get(zoneData._holdingTeams[i]).getFullName(), (i+1)));
							zoneNpc.getNpc().setTitle(LanguageEngine.getMsg("dom_npcTitle_noOwner"));
							zoneNpc.getNpc().broadcastNpcInfo();
							
							setZoneEffects(0, zoneNpc);
						}
						
						zoneData._holdingTimes[i] = 0;
						zoneData._holdingTeams[i] = 0;
					}
				}
				else
				{
					if(zoneData._holdingTeams[i] != 0)
					{
						announce(instanceId, LanguageEngine.getMsg("mDom_lostZone", _teams.get(instanceId).get(zoneData._holdingTeams[i]).getFullName(), (i+1)));
						zoneNpc.getNpc().setTitle(LanguageEngine.getMsg("dom_npcTitle_noOwner"));
						zoneNpc.getNpc().broadcastNpcInfo();
						
						setZoneEffects(0, zoneNpc);
					}
					
					zoneData._holdingTimes[i] = 0;
					zoneData._holdingTeams[i] = 0;
				}
			}
			
			if(getBoolean("allowPlayerEffects") && _teamsCount == 2)
			{
				for(PlayerEventInfo player : getPlayers(instanceId))
				{
					if(playersWithEffects.contains(player))
						player.startAbnormalEffect(player.getTeamId() == 1 ? 0x200000 : 0x000004);
					else
						player.stopAbnormalEffect(player.getTeamId() == 1 ? 0x200000 : 0x000004);
				}
			}
			
			boolean ownsAllZones = true;
			
			int teamWithMostZones = 0;
			int mostZones = 0;
			
			for(Entry<Integer, List<NpcData>> e : ownedZones.entrySet())
			{
				if(e.getValue().size() > mostZones)
				{
					teamWithMostZones = e.getKey();
					mostZones = e.getValue().size();
				}
				
				// at least two teams with the same owned zones count
				else if(e.getValue().size() != 0 && e.getValue().size() == mostZones)
				{
					ownsAllZones = false;
					break;
				}
			}
			
			if(ownsAllZones)
				ownsAllZones = mostZones >= _zonesToOwn;
				
			if(ownsAllZones)
			{
				// new owner
				if(teamWithMostZones != zoneData._dominatingTeam)
				{
					announce(instanceId, "++ " + LanguageEngine.getMsg("mDom_dominating", _teams.get(instanceId).get(teamWithMostZones).getFullName(), mostZones));
					
					zoneData._dominatingTeam = teamWithMostZones;
					zoneData._holdingAllZonesFor = 0;
				}
				else // current owner continues in his domination - raise the time he owns it
				{
					zoneData._holdingAllZonesFor += _zoneCheckInterval;
				}
				
				// check if it's been running it enough long to give score
				if(zoneData._holdingAllZonesFor >= _holdZonesFor)
				{
					_teams.get(instanceId).get(teamWithMostZones).raiseScore(_scoreForCapturingZone);
					
					for(PlayerEventInfo player : playersNearZones.get(teamWithMostZones))
					{
						getPlayerData(player).raiseScore(_scoreForCapturingZone);
						setScoreStats(player, getPlayerData(player).getScore());
						
						// update title
						if(player.isTitleUpdated())
						{
							player.setTitle(getTitle(player), true);
							player.broadcastTitleInfo();
						}
						
						CallbackManager.getInstance().playerScores(getEventType(), player, _scoreForCapturingZone);
					}
					
					zoneData._holdingAllZonesFor = 0;
					
					if(_holdZonesFor > 5)
					{
						announce(instanceId, "*** " + LanguageEngine.getMsg("mDom_score", _teams.get(instanceId).get(teamWithMostZones).getTeamName(), mostZones));
						
						if(getBoolean("allowFireworkOnScore"))
						{
							for(NpcData npc : zoneData._zones)
								npc.broadcastSkillUse(npc, npc, 2024, 1);
						}
					}
				}
				else
				{
					int toHold = _holdZonesFor - zoneData._holdingAllZonesFor;
					boolean announce = false;
					
					if(zoneData._holdingAllZonesFor == 0)
						announce = true;
					else
					{
						if(toHold >= 60 && toHold % 60 == 0)
							announce = true;
						else
						{
							switch(toHold)
							{
								case 45:
								case 30:
								case 20:
								case 10:
								case 5:
									announce = true;
									break;
							}
						}
					}
					
					if(announce)
					{
						boolean min = false;
						//if(min)
						//	toHold /= 60;
						
						announce(instanceId, "* " + LanguageEngine.getMsg("mDom_leftToScore", toHold, (min ? "minutes" : "seconds"), _teams.get(instanceId).get(teamWithMostZones).getFullName()));
					}
				}
			}
			else
			{
				if(zoneData._dominatingTeam != 0)
				{
					if(zoneData._holdingAllZonesFor > 0)
					{
						announce(instanceId, "-- " + LanguageEngine.getMsg("mDom_lostDomination", _teams.get(instanceId).get(zoneData._dominatingTeam).getFullName()));
					}
				}
				
				zoneData._dominatingTeam = 0;
				zoneData._holdingAllZonesFor = 0;
			}
		}
	}
	
	@Override
	protected String addExtraEventInfoCb(int instance)
	{
		int owningTeam = getEventData(instance)._dominatingTeam;
		int max = getEventData(instance)._holdingTeams.length;
		int count = 0;
		
		for(int zone : getEventData(instance)._holdingTeams)
		{
			if(zone == owningTeam)
				count ++;
		}
		
		String status = "<font color=ac9887>Zones dominated by:</font> <font color=" + EventManager.getInstance().getDarkColorForHtml(owningTeam) + ">" + EventManager.getInstance().getTeamName(owningTeam) + " team</font>" + (owningTeam > 0 ? " <font color=7f7f7f>(" + count + "/" + max + " zones)</font>" : "");
		
		return("<table width=510 bgcolor=3E3E3E><tr><td width=510 align=center>" + status + "</td></tr></table>");
	}
	
	@Override
	public String getHtmlDescription()
	{
		//if(_htmlDescription == null)
		{
			EventDescription desc = EventDescriptionSystem.getInstance().getDescription(getEventType());
			if(desc != null)
			{
				_htmlDescription = desc.getDescription(getConfigs());
			}
			else
			{
				_htmlDescription = getInt("teamsCount") + " teams fighting against each other. ";
				_htmlDescription += "There are " + getInt("countOfZones") + " zones, each represented by an NPC. ";
				_htmlDescription += "In order to gain a score, your team must own at least " + getInt("zonesToOwnToScore") + " zones. ";
				_htmlDescription += "To own a zone, your team must get close to each of these zones and kill all other enemies standing near the zone too. ";
				
				if(getInt("killsForReward") > 0)
					_htmlDescription += "At least " + getInt("killsForReward") + " kill(s) is required to receive a reward. ";
				
				if(getInt("scoreForReward") > 0)
					_htmlDescription += "At least " + getInt("scoreForReward") + " score (obtained when your team owns the zone and you stand near it) is required to receive a reward. ";
				
				if(getBoolean("waweRespawn"))
					_htmlDescription += "Dead players are resurrected by an advanced wawe-spawn engine each " + getInt("resDelay") + " seconds. ";
				else
					_htmlDescription += "If you die, you will get resurrected in " + getInt("resDelay") + " seconds. ";
				
				if(getBoolean("createParties"))
					_htmlDescription += "The event automatically creates parties on start.";
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
		}
		
		int count = getInt("countOfZones");
		if(!map.checkForSpawns(SpawnType.Zone, -1, count))
			tb.append(addMissingSpawn(SpawnType.Zone, 0, count));
	
		return tb.toString();
	}
	
	@Override
	protected ZoneData createEventData(int instance)
	{
		return new MultipleZoneData(instance, _zonesCount);
	}
	
	@Override
	protected MultipleZoneData getEventData(int instance)
	{
		return (MultipleZoneData) _matches.get(instance)._zoneData;
	}
}
