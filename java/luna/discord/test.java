package luna.discord;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.events.dataTables.NpcSpawnTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.RewardsTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.TeamTemplate;
import net.sf.l2j.util.Util;

public class test
{
	public static void main(String[] args) throws Exception
	{
		List<Event> events2 = new CopyOnWriteArrayList<>();
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
				final ArrayList<TeamTemplate> teamTemps = new ArrayList<>();
				final ArrayList<NpcSpawnTemplate> npcSpawns = new ArrayList<>();
				final ArrayList<RewardsTemplate> rewards = new ArrayList<>();
				if (n.getNodeName().equalsIgnoreCase("list"))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if (d.getNodeName().equalsIgnoreCase("event"))
						{
							int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
							String name = d.getAttributes().getNamedItem("Name").getNodeValue();
							String desc = d.getAttributes().getNamedItem("Description").getNodeValue();
							String joinLocName = d.getAttributes().getNamedItem("JoinLocName").getNodeValue();
							int minLvl = Integer.parseInt(d.getAttributes().getNamedItem("minLvL").getNodeValue());
							int maxLvl = Integer.parseInt(d.getAttributes().getNamedItem("maxLvL").getNodeValue());
							int npcId = 0;
							Location regNpcLoc = null;
							int joinTime = 0;
							int eventTime = 0;
							int minPlayers = 0;
							int maxPlayers = 0;
							Map<Integer, Location> locs = new HashMap<Integer, Location>();
							for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
							{
								if (e.getNodeName().equalsIgnoreCase("spawnRegNpc"))
								{
									npcId = Integer.parseInt(e.getAttributes().getNamedItem("npcId").getNodeValue());
									regNpcLoc = new Location(Integer.parseInt(e.getAttributes().getNamedItem("spawnX").getNodeValue()), Integer.parseInt(e.getAttributes().getNamedItem("spawnY").getNodeValue()), Integer.parseInt(e.getAttributes().getNamedItem("spawnZ").getNodeValue()));
								}
								else if (e.getNodeName().equalsIgnoreCase("timers"))
								{
									joinTime = Integer.parseInt(e.getAttributes().getNamedItem("joinTime").getNodeValue());
									eventTime = Integer.parseInt(e.getAttributes().getNamedItem("eventTime").getNodeValue());
								}
								else if (e.getNodeName().equalsIgnoreCase("players"))
								{
									minPlayers = Integer.parseInt(e.getAttributes().getNamedItem("minPlayers").getNodeValue());
									maxPlayers = Integer.parseInt(e.getAttributes().getNamedItem("maxPlayers").getNodeValue());
								}
								else if ("teams".equalsIgnoreCase(e.getNodeName()))
								{
									for (Node n4 = e.getFirstChild(); n4 != null; n4 = n4.getNextSibling())
									{
										if ("team".equalsIgnoreCase(n4.getNodeName()))
										{
											final NamedNodeMap nnm2 = n4.getAttributes();
											final int teamId = Integer.parseInt(nnm2.getNamedItem("id").getNodeValue());
											final String teamName = nnm2.getNamedItem("name").getNodeValue();
											final int color = Integer.parseInt(nnm2.getNamedItem("color").getNodeValue(), 16);
											final int[] spawn = Util.toIntArray(nnm2.getNamedItem("spawn").getNodeValue());
											//teamTemps.add(new TeamTemplate(teamId, teamName, color, spawn));
										}
									}
								}
								else if (e.getNodeName().equalsIgnoreCase("npcSpawns"))
								{
									for (Node g = e.getFirstChild(); g != null; g = g.getNextSibling())
									{
										if (g.getNodeName().equalsIgnoreCase("spawn"))
										{
											int spawnNpcId = Integer.parseInt(g.getAttributes().getNamedItem("npcId").getNodeValue());
											Location npcSpawnLoc = new Location(Integer.parseInt(g.getAttributes().getNamedItem("spawnX").getNodeValue()), Integer.parseInt(g.getAttributes().getNamedItem("spawnY").getNodeValue()), Integer.parseInt(g.getAttributes().getNamedItem("spawnZ").getNodeValue()));
											npcSpawns.add(new NpcSpawnTemplate(spawnNpcId, npcSpawnLoc, "title"));
										}
									}
								}
								else if (e.getNodeName().equalsIgnoreCase("rewards"))
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
													int itemId = Integer.parseInt(h.getAttributes().getNamedItem("itemId").getNodeValue());
													int ammount = Integer.parseInt(h.getAttributes().getNamedItem("ammount").getNodeValue());
													int cance = Integer.parseInt(h.getAttributes().getNamedItem("cance").getNodeValue());
													rewards.add(new RewardsTemplate(rewardType, itemId, ammount, cance, false));
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
													int cance = Integer.parseInt(h.getAttributes().getNamedItem("cance").getNodeValue());
													rewards.add(new RewardsTemplate(rewardType, itemId, ammount, cance, false));
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
													int cance = Integer.parseInt(h.getAttributes().getNamedItem("cance").getNodeValue());
													rewards.add(new RewardsTemplate(rewardType, itemId, ammount, cance, false));
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
													int cance = Integer.parseInt(h.getAttributes().getNamedItem("cance").getNodeValue());
													rewards.add(new RewardsTemplate(rewardType, itemId, ammount, cance, false));
												}
											}
										}
									}
								}
							}
							Event temp = new Event(id, name, desc, joinLocName, minLvl, maxLvl, npcId, regNpcLoc, joinTime, eventTime, minPlayers, maxPlayers, teamTemps, npcSpawns, rewards);
							if (temp != null)
								events2.add(temp);
							System.out.println("Event Id: " + events2.get(0).getId() + " Name: " + events2.get(0).getName() + " Desc: " + events2.get(0).getDesc() + " JoinLocName: " + events2.get(0).getJoinLocName());
							System.out.println("minLvL: " + events2.get(0).getMinLvl() + " maxLvl: " + events2.get(0).getMaxLvl());
							System.out.println("RegNpcId: " + events2.get(0).getRegNpcId() + " regNpcLoc: " + events2.get(0).getRegNpcLoc().getX() + "," + events2.get(0).getRegNpcLoc().getY() + "," + events2.get(0).getRegNpcLoc().getZ());
							System.out.println("Timers: " + "Join Time: " + events2.get(0).getJoinTime() + " Event Time: " + events2.get(0).getEventTime());
							System.out.println("Players: " + "Min Players: " + events2.get(0).getMinPl() + " Max Players: " + events2.get(0).getMaxPl());
							System.out.println("Team Size: " + events2.get(0).getEvTeams().size());
							// Teams
							for (int team = 0; team < events2.get(0).getEvTeams().size(); team++)
							{
								System.out.println("Team Name: " + events2.get(0).getEvTeams().get(team).getName() + " Team ID: " + events2.get(0).getEvTeams().get(team).getId() + " Team Color: " + events2.get(0).getEvTeams().get(team).getNameColor());

										//int x = events2.get(0).getEvTeams().get(team).getSpawnX();
										//int y = events2.get(0).getEvTeams().get(team).getSpawnY();
										//int z = events2.get(0).getEvTeams().get(team).getSpawnZ();
								//System.out.println("Team ID: " + events2.get(0).getEvTeams().get(team).getId() + " Spawn " + 2 + ": " + " Location: " + x + " " + y + " " + z + " ");
							}
							for (int npc = 0; npc < events2.get(0).getEvSpawns().size(); npc++)
							{
										int spawnNpcId = events2.get(0).getEvSpawns().get(npc).getId();
										int nx = events2.get(0).getEvSpawns().get(npc).getLoc().getX();
										int ny = events2.get(0).getEvSpawns().get(npc).getLoc().getY();
										int nz = events2.get(0).getEvSpawns().get(npc).getLoc().getZ();	
								System.out.println("Npc ID: " + spawnNpcId + ": " + " Location: " + nx + " " + ny + " " + nz + " ");
							}
							for (int reward = 0; reward < events2.get(0).getEvRewards().size(); reward++)
							{
										int rewardType = events2.get(0).getEvRewards().get(reward).getRewardType();
										int rewardId = events2.get(0).getEvRewards().get(reward).getItemId();
										int ammount = events2.get(0).getEvRewards().get(reward).getAmmount();
										int chance = events2.get(0).getEvRewards().get(reward).getChance();
										
								System.out.println("Reward ID: " + rewardId + ": " + " ammount: " + ammount + " Chance: " + chance + " Type: " + rewardType + " ");
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
	}
}
