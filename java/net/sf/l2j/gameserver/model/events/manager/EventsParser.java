package net.sf.l2j.gameserver.model.events.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.events.dataTables.DoorTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.FenceTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.NpcSpawnTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.PlayerSpawnTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.RewardsTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.TeamFlagTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.TeamSpawnTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.TeamTemplate;
import net.sf.l2j.util.Util;

public class EventsParser
{
	private final EventScheduleTables		calendarTable	= EventScheduleTables.getInstance();
	private final HashMap<String, Event>	eventTemplates	= new HashMap<>();
	public List<Event>						events			= new CopyOnWriteArrayList<>();
	
	private void loadXml(L2PcInstance p)
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = new File("data/xml/events/Luna/events.xml");
		try
		{
			InputSource in = new InputSource(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			in.setEncoding("UTF-8");
			Document doc = factory.newDocumentBuilder().parse(in);
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if (n.getNodeName().equalsIgnoreCase("list"))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("schedule".equalsIgnoreCase(d.getNodeName()))
							calendarTable.loadNode(d);
						if (d.getNodeName().equalsIgnoreCase("event"))
						{
							final ArrayList<TeamTemplate> teamTemps = new ArrayList<>();
							final ArrayList<TeamSpawnTemplate> teamSpawnTemps = new ArrayList<>();
							final ArrayList<PlayerSpawnTemplate> playerSpawnTemps = new ArrayList<>();
							final ArrayList<TeamFlagTemplate> teamFlagSpawnTemps = new ArrayList<>();
							final ArrayList<NpcSpawnTemplate> npcSpawns = new ArrayList<>();
							final ArrayList<RewardsTemplate> rewards = new ArrayList<>();
							final ArrayList<DoorTemplate> doors = new ArrayList<>();
							final ArrayList<FenceTemplate> fences = new ArrayList<>();
							int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
							String type = d.getAttributes().getNamedItem("Type").getNodeValue();
							String name = d.getAttributes().getNamedItem("Name").getNodeValue();
							String desc = d.getAttributes().getNamedItem("Description").getNodeValue();
							String joinLocName = d.getAttributes().getNamedItem("JoinLocName").getNodeValue();
							int instanceId = Integer.parseInt(d.getAttributes().getNamedItem("instanceId").getNodeValue());
							int respawnDelay = Integer.parseInt(d.getAttributes().getNamedItem("respawnDelay").getNodeValue());
							int respawnDelayMul = Integer.parseInt(d.getAttributes().getNamedItem("respawnDelayMul").getNodeValue());
							int minLvl = Integer.parseInt(d.getAttributes().getNamedItem("minLvL").getNodeValue());
							int maxLvl = Integer.parseInt(d.getAttributes().getNamedItem("maxLvL").getNodeValue());
							boolean regAll = false;
							if(d.getAttributes().getNamedItem("regAll") != null)
							{
								if (d.getAttributes().getNamedItem("regAll").getNodeValue().equalsIgnoreCase("true"))
								{
									regAll = true;
								}
							}

							int npcId = 0;
							Location regNpcLoc = null;
							int baseNpcId = 0;
							Location baseNpcLoc = null;
							int joinTime = 0;
							int eventTime = 0;
							int minPlayers = 0;
							int maxPlayers = 0;
							Location centerLoc = null;
							int centerLocOffset = 0;
							Location siegeFlagLoc = null;
							int siegeFlagNpcId = 0;
							int sealersPerSide = 1;
							String startType = "0";
							String sortType = "0";
							String playerNColor = "0";
							String playerTColor = "0";
							String topPlayerNColor = "0";
							String topPlayerTColor = "0";
							String firstPlayerNColor = "0";
							String firstPlayerTColor = "0";
							boolean doublePvPS = false;
							for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
							{

								if (e.getNodeName().equalsIgnoreCase("doublePvPs"))
								{
									if (e.getAttributes().getNamedItem("doublePvPs").getNodeValue().equalsIgnoreCase("true"))
									{
										doublePvPS = true;
									}
								}
								if (e.getNodeName().equalsIgnoreCase("spawnRegNpc"))
								{
									npcId = Integer.parseInt(e.getAttributes().getNamedItem("npcId").getNodeValue());
									regNpcLoc = new Location(Integer.parseInt(e.getAttributes().getNamedItem("spawnX").getNodeValue()), Integer.parseInt(e.getAttributes().getNamedItem("spawnY").getNodeValue()), Integer.parseInt(e.getAttributes().getNamedItem("spawnZ").getNodeValue()));
								}
								if (e.getNodeName().equalsIgnoreCase("centerLoc"))
								{
									final int[] spawn = Util.toIntArray(e.getAttributes().getNamedItem("spawn").getNodeValue());
									centerLoc = new Location(spawn[0], spawn[1], spawn[2]);
									centerLocOffset = Integer.parseInt(e.getAttributes().getNamedItem("range").getNodeValue());
								}
								if (e.getNodeName().equalsIgnoreCase("flagLoc"))
								{
									siegeFlagNpcId = Integer.parseInt(e.getAttributes().getNamedItem("flagId").getNodeValue());
									final int[] spawn = Util.toIntArray(e.getAttributes().getNamedItem("spawn").getNodeValue());
									siegeFlagLoc = new Location(spawn[0], spawn[1], spawn[2]);
								}
								if (e.getNodeName().equalsIgnoreCase("sealers"))
								{
									sealersPerSide = Integer.parseInt(e.getAttributes().getNamedItem("sealers").getNodeValue());
								}
								
								if (e.getNodeName().equalsIgnoreCase("startType"))
								{
									startType = e.getAttributes().getNamedItem("type").getNodeValue();
								}
								if (e.getNodeName().equalsIgnoreCase("sortType"))
								{
									sortType = e.getAttributes().getNamedItem("type").getNodeValue();
								}
								if (e.getNodeName().equalsIgnoreCase("timers"))
								{
									joinTime = Integer.parseInt(e.getAttributes().getNamedItem("joinTime").getNodeValue());
									eventTime = Integer.parseInt(e.getAttributes().getNamedItem("eventTime").getNodeValue());
								}
								if (e.getNodeName().equalsIgnoreCase("players"))
								{
									minPlayers = Integer.parseInt(e.getAttributes().getNamedItem("minPlayers").getNodeValue());
									maxPlayers = Integer.parseInt(e.getAttributes().getNamedItem("maxPlayers").getNodeValue());
								}
								if (e.getNodeName().equalsIgnoreCase("spawnBaseNpc"))
								{
									baseNpcId = Integer.parseInt(e.getAttributes().getNamedItem("npcId").getNodeValue());
									baseNpcLoc = new Location(Integer.parseInt(e.getAttributes().getNamedItem("spawnX").getNodeValue()), Integer.parseInt(e.getAttributes().getNamedItem("spawnY").getNodeValue()), Integer.parseInt(e.getAttributes().getNamedItem("spawnZ").getNodeValue()));
								}
								if (e.getNodeName().equalsIgnoreCase("playerInfo"))
								{
									for (Node n4 = e.getFirstChild(); n4 != null; n4 = n4.getNextSibling())
									{
										if ("player".equalsIgnoreCase(n4.getNodeName()))
										{
											playerNColor = n4.getAttributes().getNamedItem("nameColor").getNodeValue();
											playerTColor = n4.getAttributes().getNamedItem("titleColor").getNodeValue();
										}
										if ("firstPlayer".equalsIgnoreCase(n4.getNodeName()))
										{
											firstPlayerNColor = n4.getAttributes().getNamedItem("nameColor").getNodeValue();
											firstPlayerTColor = n4.getAttributes().getNamedItem("titleColor").getNodeValue();
										}
										if ("topPlayer".equalsIgnoreCase(n4.getNodeName()))
										{
											topPlayerNColor = n4.getAttributes().getNamedItem("nameColor").getNodeValue();
											topPlayerTColor = n4.getAttributes().getNamedItem("titleColor").getNodeValue();
										}
									}
								}
								if ("teams".equalsIgnoreCase(e.getNodeName()))
								{
									for (Node n4 = e.getFirstChild(); n4 != null; n4 = n4.getNextSibling())
									{
										if ("team".equalsIgnoreCase(n4.getNodeName()))
										{
											final NamedNodeMap nnm2 = n4.getAttributes();
											final int teamId = Integer.parseInt(nnm2.getNamedItem("id").getNodeValue());
											final String teamName = nnm2.getNamedItem("name").getNodeValue();
											final String color = nnm2.getNamedItem("nameColor").getNodeValue();
											final String titleColor = nnm2.getNamedItem("titleColor").getNodeValue();
											teamTemps.add(new TeamTemplate(teamId, teamName, color, titleColor));
										}
									}
								}
								if (e.getNodeName().equalsIgnoreCase("teamSpawns"))
								{
									for (Node n4 = e.getFirstChild(); n4 != null; n4 = n4.getNextSibling())
									{
										if (n4.getNodeName().equalsIgnoreCase("spawn"))
										{
											final NamedNodeMap nnm2 = n4.getAttributes();
											final String sTeamId = nnm2.getNamedItem("teamId").getNodeValue();
											final int[] spawn = Util.toIntArray(nnm2.getNamedItem("spawn").getNodeValue());
											teamSpawnTemps.add(new TeamSpawnTemplate(sTeamId, spawn));
										}
									}
								}
								if (e.getNodeName().equalsIgnoreCase("flagSpawns"))
								{
									for (Node n4 = e.getFirstChild(); n4 != null; n4 = n4.getNextSibling())
									{
										if (n4.getNodeName().equalsIgnoreCase("flag"))
										{
											final NamedNodeMap nnm2 = n4.getAttributes();
											final String sTeamId = nnm2.getNamedItem("teamId").getNodeValue();
											final int[] spawn = Util.toIntArray(nnm2.getNamedItem("spawn").getNodeValue());
											teamFlagSpawnTemps.add(new TeamFlagTemplate(sTeamId, spawn));
										}
									}
								}
								if (e.getNodeName().equalsIgnoreCase("doors"))
								{
									for (Node n4 = e.getFirstChild(); n4 != null; n4 = n4.getNextSibling())
									{
										if (n4.getNodeName().equalsIgnoreCase("door"))
										{
											final NamedNodeMap nnm2 = n4.getAttributes();
											final int doorId = Integer.parseInt(nnm2.getNamedItem("doorId").getNodeValue());
											doors.add(new DoorTemplate(doorId));
										}
									}
								}
								if (e.getNodeName().equalsIgnoreCase("areaSpawns"))
								{
									for (Node n4 = e.getFirstChild(); n4 != null; n4 = n4.getNextSibling())
									{
										if (n4.getNodeName().equalsIgnoreCase("spawn"))
										{
											final NamedNodeMap nnm2 = n4.getAttributes();
											final int[] areaSpawn = Util.toIntArray(nnm2.getNamedItem("centerLoc").getNodeValue());
											final int range = Integer.parseInt((nnm2.getNamedItem("centerRange").getNodeValue()));
											playerSpawnTemps.add(new PlayerSpawnTemplate(areaSpawn, range));
										}
									}
								}
								if (e.getNodeName().equalsIgnoreCase("playerSpawns"))
								{
									for (Node n4 = e.getFirstChild(); n4 != null; n4 = n4.getNextSibling())
									{
										if (n4.getNodeName().equalsIgnoreCase("spawn"))
										{
											final NamedNodeMap nnm2 = n4.getAttributes();
											final int[] playerSpawn = Util.toIntArray(nnm2.getNamedItem("spawnLoc").getNodeValue());
											final int pRange = Integer.parseInt((nnm2.getNamedItem("range").getNodeValue()));
											playerSpawnTemps.add(new PlayerSpawnTemplate(playerSpawn, pRange));
										}
									}
								}
								if (e.getNodeName().equalsIgnoreCase("fences"))
								{
									for (Node n4 = e.getFirstChild(); n4 != null; n4 = n4.getNextSibling())
									{
										if (n4.getNodeName().equalsIgnoreCase("fence"))
										{
											final NamedNodeMap nnm2 = n4.getAttributes();
											final int fenceId = Integer.parseInt(nnm2.getNamedItem("fenceId").getNodeValue());
											fences.add(new FenceTemplate(fenceId));
										}
									}
								}
								if (e.getNodeName().equalsIgnoreCase("npcSpawns"))
								{
									for (Node g = e.getFirstChild(); g != null; g = g.getNextSibling())
									{
										if (g.getNodeName().equalsIgnoreCase("spawn"))
										{
											String npcTitle = "";
											int spawnNpcId = Integer.parseInt(g.getAttributes().getNamedItem("npcId").getNodeValue());
											if (g.getAttributes().getNamedItem("title") != null)
											{
												npcTitle = g.getAttributes().getNamedItem("title").getNodeValue();
											}
											if (g.getAttributes().getNamedItem("spawnloc") != null)
											{
												final int[] spawnLoc = Util.toIntArray(g.getAttributes().getNamedItem("spawnloc").getNodeValue());
												Location npcSpawnLoc = new Location(spawnLoc[0], spawnLoc[1], spawnLoc[2]);
												npcSpawns.add(new NpcSpawnTemplate(spawnNpcId, npcSpawnLoc, npcTitle));
											}
											if (g.getAttributes().getNamedItem("spawnX") != null)
											{
												Location npcSpawnLoc = new Location(Integer.parseInt(g.getAttributes().getNamedItem("spawnX").getNodeValue()), Integer.parseInt(g.getAttributes().getNamedItem("spawnY").getNodeValue()), Integer.parseInt(g.getAttributes().getNamedItem("spawnZ").getNodeValue()));
												npcSpawns.add(new NpcSpawnTemplate(spawnNpcId, npcSpawnLoc, npcTitle));
											}
										}
									}
								}
								if (e.getNodeName().equalsIgnoreCase("rewards"))
								{
									for (Node g = e.getFirstChild(); g != null; g = g.getNextSibling())
									{
										if (g.getNodeName().equalsIgnoreCase("winner"))
										{
											for (Node h = g.getFirstChild(); h != null; h = h.getNextSibling())
											{
												if (h.getNodeName().equalsIgnoreCase("reward"))
												{
													int rewardType = 1;
													boolean isStatic = false;
													int itemId = Integer.parseInt(h.getAttributes().getNamedItem("itemId").getNodeValue());
													int ammount = Integer.parseInt(h.getAttributes().getNamedItem("ammount").getNodeValue());
													int chance = Integer.parseInt(h.getAttributes().getNamedItem("chance").getNodeValue());
													if (h.getAttributes().getNamedItem("static").getNodeValue().equalsIgnoreCase("true") && h.getAttributes().getNamedItem("static").getNodeValue() != null)
													{
														isStatic = true;
													}
													rewards.add(new RewardsTemplate(rewardType, itemId, ammount, chance, isStatic));
												}
											}
										}
										if (g.getNodeName().equalsIgnoreCase("loser"))
										{
											for (Node h = g.getFirstChild(); h != null; h = h.getNextSibling())
											{
												if (h.getNodeName().equalsIgnoreCase("reward"))
												{
													int rewardType = 2;
													int itemId = Integer.parseInt(h.getAttributes().getNamedItem("itemId").getNodeValue());
													int ammount = Integer.parseInt(h.getAttributes().getNamedItem("ammount").getNodeValue());
													int chance = Integer.parseInt(h.getAttributes().getNamedItem("chance").getNodeValue());
													boolean isStatic = false;
													if (h.getAttributes().getNamedItem("static").getNodeValue().equalsIgnoreCase("true"))
													{
														isStatic = true;
													}
													rewards.add(new RewardsTemplate(rewardType, itemId, ammount, chance, isStatic));
												}
											}
										}
										if (g.getNodeName().equalsIgnoreCase("tie"))
										{
											for (Node h = g.getFirstChild(); h != null; h = h.getNextSibling())
											{
												if (h.getNodeName().equalsIgnoreCase("reward"))
												{
													int rewardType = 3;
													int itemId = Integer.parseInt(h.getAttributes().getNamedItem("itemId").getNodeValue());
													int ammount = Integer.parseInt(h.getAttributes().getNamedItem("ammount").getNodeValue());
													int chance = Integer.parseInt(h.getAttributes().getNamedItem("chance").getNodeValue());
													boolean isStatic = false;
													if (h.getAttributes().getNamedItem("static").getNodeValue().equalsIgnoreCase("true"))
													{
														isStatic = true;
													}
													rewards.add(new RewardsTemplate(rewardType, itemId, ammount, chance, isStatic));
												}
											}
										}
										if (g.getNodeName().equalsIgnoreCase("earlyReg"))
										{
											for (Node h = g.getFirstChild(); h != null; h = h.getNextSibling())
											{
												if (h.getNodeName().equalsIgnoreCase("reward"))
												{
													int rewardType = 4;
													int itemId = Integer.parseInt(h.getAttributes().getNamedItem("itemId").getNodeValue());
													int ammount = Integer.parseInt(h.getAttributes().getNamedItem("ammount").getNodeValue());
													int chance = Integer.parseInt(h.getAttributes().getNamedItem("chance").getNodeValue());
													boolean isStatic = false;
													if (h.getAttributes().getNamedItem("static").getNodeValue().equalsIgnoreCase("true"))
													{
														isStatic = true;
													}
													rewards.add(new RewardsTemplate(rewardType, itemId, ammount, chance, isStatic));
												}
											}
										}
										if (g.getNodeName().equalsIgnoreCase("topPlayers"))
										{
											for (Node h = g.getFirstChild(); h != null; h = h.getNextSibling())
											{
												if (h.getNodeName().equalsIgnoreCase("reward"))
												{
													int rewardType = 5;
													int itemId = Integer.parseInt(h.getAttributes().getNamedItem("itemId").getNodeValue());
													int ammount = Integer.parseInt(h.getAttributes().getNamedItem("ammount").getNodeValue());
													int chance = Integer.parseInt(h.getAttributes().getNamedItem("chance").getNodeValue());
													boolean isStatic = false;
													if (h.getAttributes().getNamedItem("static").getNodeValue().equalsIgnoreCase("true"))
													{
														isStatic = true;
													}
													rewards.add(new RewardsTemplate(rewardType, itemId, ammount, chance, isStatic));
												}
											}
										}
									}
								}
							}
							Event temp = new Event(id, type, name, desc, regAll, doublePvPS, joinLocName, instanceId, respawnDelay, respawnDelayMul, minLvl, maxLvl, npcId, regNpcLoc, baseNpcId, baseNpcLoc, centerLoc, centerLocOffset, siegeFlagLoc, siegeFlagNpcId, sealersPerSide, startType, sortType, joinTime, eventTime, minPlayers, maxPlayers, teamTemps, teamSpawnTemps, npcSpawns, rewards, doors, fences, teamFlagSpawnTemps, playerSpawnTemps, playerNColor, playerTColor, topPlayerNColor, topPlayerTColor, firstPlayerNColor, firstPlayerTColor);
							if (temp != null)
							{
								eventTemplates.put(String.valueOf(id), temp);
								events.add(temp);
							}
						}
					}
				}
			}
			if (!eventTemplates.isEmpty())
			{
				EventsCollector.getInstance().collectEvents(p);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public class Event
	{
		private int								evId;
		private String							evType;
		private String							evName;
		private String							evDesc;
		private Boolean							evRegAll;
		private Boolean							evDoublePvPs;
		private String							evJoinLocName;
		private int								evInstanceId;
		private int								evRespawnDelay;
		private int								evRespawnDelayMul;
		private int								evMinLvl;
		private int								evMaxLvl;
		private int								evRegNpcId;
		private Location						evRegNpcLoc;
		private int								evBaseNpcId;
		private Location						evBaseNpcLoc;
		private Location						evCenterLoc;
		private int								evCenterLocRange;
		private Location						evSiegeFlagLoc;
		private int								evSiegeFlagId;
		private String							evStartType;
		private String							evSortType;
		private int								evJoinTime;
		private int								evEventTime;
		private int								evMinPl;
		private int								evMaxPl;
		private int								evSealersPerSide;
		private String							evPlayerNColor;
		private String							evPlayerTColor;
		private String							evTopPlayerNColor;
		private String							evTopPlayerTColor;
		private String							evFirstPlayerNColor;
		private String							evFirstPlayerTColor;
		private ArrayList<TeamTemplate>			evTeams			= new ArrayList<>();
		private ArrayList<TeamSpawnTemplate>	evTeamsLoc		= new ArrayList<>();
		private ArrayList<NpcSpawnTemplate>		evNpcSpawns		= new ArrayList<>();
		private ArrayList<RewardsTemplate>		evRewards		= new ArrayList<>();
		private ArrayList<DoorTemplate>			evDoors			= new ArrayList<>();
		private ArrayList<FenceTemplate>		evFences		= new ArrayList<>();
		private ArrayList<TeamFlagTemplate>		evTeamFlags		= new ArrayList<>();
		private ArrayList<PlayerSpawnTemplate>	evPlayerSpawns	= new ArrayList<>();
		
		public Event(int id, String type, String name, String desc, Boolean regAll,Boolean doublePvPS, String joinLocName, int instanceId, int respawnDelay, int respawnDelayMul, int minLvl, int maxLvl, int regNpcId, Location regNpcLoc, int baseNpcId, Location baseNpcLoc, Location centerLoc, int centerLocRange, Location siegeFlagLoc, int siegeFlagId, int sealersPerSide, String startType, String sortType, int joinTime, int eventTime, int minPl, int maxPl, ArrayList<TeamTemplate> teams, ArrayList<TeamSpawnTemplate> teamsLoc, ArrayList<NpcSpawnTemplate> npcSpawns, ArrayList<RewardsTemplate> rewards, ArrayList<DoorTemplate> doors, ArrayList<FenceTemplate> fences, ArrayList<TeamFlagTemplate> teamFlags, ArrayList<PlayerSpawnTemplate> playerSpawns, String playerNColor, String playerTColor, String topPlayerNColor, String topPlayerTColor, String firstPlayerNColor, String firstPlayerTColor)
		{
			evId = id;
			evType = type;
			evName = name;
			evDesc = desc;
			evRegAll = regAll;
			evDoublePvPs = doublePvPS;
			evJoinLocName = joinLocName;
			evInstanceId = instanceId;
			evRespawnDelay = respawnDelay;
			evRespawnDelayMul = respawnDelayMul;
			evMinLvl = minLvl;
			evMaxLvl = maxLvl;
			evRegNpcId = regNpcId;
			evRegNpcLoc = regNpcLoc;
			evBaseNpcId = baseNpcId;
			evBaseNpcLoc = baseNpcLoc;
			evCenterLoc = centerLoc;
			evCenterLocRange = centerLocRange;
			evSiegeFlagLoc = siegeFlagLoc;
			evSiegeFlagId = siegeFlagId;
			evStartType = startType;
			evSortType = sortType;
			evJoinTime = joinTime;
			evEventTime = eventTime;
			evMinPl = minPl;
			evMaxPl = maxPl;
			evTeams = teams;
			evTeamsLoc = teamsLoc;
			evNpcSpawns = npcSpawns;
			evRewards = rewards;
			evDoors = doors;
			evFences = fences;
			evTeamFlags = teamFlags;
			evPlayerSpawns = playerSpawns;
			evPlayerNColor = playerNColor;
			evPlayerTColor = playerTColor;
			evTopPlayerNColor = topPlayerNColor;
			evTopPlayerTColor = topPlayerTColor;
			evFirstPlayerNColor = firstPlayerNColor;
			evFirstPlayerTColor = firstPlayerTColor;
			evSealersPerSide = sealersPerSide;
		}
		
		public int getId()
		{
			return evId;
		}
		
		public String getType()
		{
			return evType;
		}
		
		public String getName()
		{
			return evName;
		}
		
		public String getDesc()
		{
			return evDesc;
		}
		
		public boolean getRegAll()
		{
			return evRegAll;
		}

		public boolean getDoublePvPs()
		{
			return evDoublePvPs;
		}
		
		public int getSealersPerSide()
		{
			return evSealersPerSide;
		}
		
		public String getJoinLocName()
		{
			return evJoinLocName;
		}
		
		public int getInstanceId()
		{
			return evInstanceId;
		}
		
		public int getRespawnDelay()
		{
			return evRespawnDelay;
		}

		public int getRespawnDelayMul()
		{
			return evRespawnDelayMul;
		}
		
		public int getMinLvl()
		{
			return evMinLvl;
		}
		
		public int getMaxLvl()
		{
			return evMaxLvl;
		}
		
		public int getRegNpcId()
		{
			return evRegNpcId;
		}
		
		public Location getRegNpcLoc()
		{
			return evRegNpcLoc;
		}
		
		public int getBaseNpcId()
		{
			return evBaseNpcId;
		}
		
		public Location getBaseNpcLoc()
		{
			return evBaseNpcLoc;
		}
		
		public Location getCenterLoc()
		{
			return evCenterLoc;
		}
		
		public int getCenterLocRange()
		{
			return evCenterLocRange;
		}
		
		public Location getSiegeFlagLoc()
		{
			return evSiegeFlagLoc;
		}
		
		public int getSiegeFlagNpcId()
		{
			return evSiegeFlagId;
		}
		
		public String getStartType()
		{
			return evStartType;
		}
		
		public String getSortType()
		{
			return evSortType;
		}
		
		public int getJoinTime()
		{
			return evJoinTime;
		}
		
		public int getEventTime()
		{
			return evEventTime;
		}
		
		public int getMinPl()
		{
			return evMinPl;
		}
		
		public int getMaxPl()
		{
			return evMaxPl;
		}
		
		public ArrayList<TeamTemplate> getEvTeams()
		{
			return evTeams;
		}
		
		public ArrayList<TeamSpawnTemplate> getEvTeamLocs()
		{
			return evTeamsLoc;
		}
		
		public ArrayList<NpcSpawnTemplate> getEvSpawns()
		{
			return evNpcSpawns;
		}
		
		public ArrayList<RewardsTemplate> getEvRewards()
		{
			return evRewards;
		}
		
		public ArrayList<DoorTemplate> getEvDoors()
		{
			return evDoors;
		}
		
		public ArrayList<FenceTemplate> getEvFences()
		{
			return evFences;
		}
		
		public ArrayList<TeamFlagTemplate> getEvTeamFlags()
		{
			return evTeamFlags;
		}
		
		public ArrayList<PlayerSpawnTemplate> getEvPlayerSpawns()
		{
			return evPlayerSpawns;
		}
		
		public String getEvPlayerNColor()
		{
			return evPlayerNColor;
		}
		
		public String getEvPlayerTColor()
		{
			return evPlayerTColor;
		}
		
		public String getEvTopPlayerNColor()
		{
			return evTopPlayerNColor;
		}
		
		public String getEvTopPlayerTColor()
		{
			return evTopPlayerTColor;
		}
		
		public String getEvFirstPlayerNColor()
		{
			return evFirstPlayerNColor;
		}
		
		public String getEvFirstPlayerTColor()
		{
			return evFirstPlayerTColor;
		}
	}
	
	public List<Event> getEvents()
	{
		return events;
	}
	
	public HashMap<String, Event> getEventTemplates()
	{
		return eventTemplates;
	}
	
	public void Reload(L2PcInstance p)
	{
		events.clear();
		loadXml(p);
		System.out.println("Loaded " + events.size() + " event maps");
		Communicator.getInstance().getTodayEvents();
	}
	
	public static EventsParser getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		private static final EventsParser INSTANCE = new EventsParser();
	}
}
