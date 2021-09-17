package luna.custom.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TIntObjectHashMap;
import net.sf.l2j.gameserver.datatables.ItemLists;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.model.L2DropCategory;
import net.sf.l2j.gameserver.model.L2DropData;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2TeleportLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class SpawnUtil
{
	public void dumpSpawn(int npcId, int x, int y, int z, int heading, int respawnDelay)
	{
		StringBuilder sb = new StringBuilder();
		try
		{
			File f = new File("./data/xml/spawns/raid_spawndump.xml");
			if (!f.exists())
				f.createNewFile();
			FileWriter writer = new FileWriter(f, true);
			writer.write("\t\t<npc id=\"" + npcId + "\" x=\"" + x + "\" y=\"" + y + "\" z=\"" + z + "\" heading=\"" + heading + "\" respawnDelay=\"" + respawnDelay + "\" />" + " <!--" + NpcTable.getInstance().getTemplate(npcId).getName() + "-->\n");
			writer.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean isMonster(String npcType)
	{
		switch (npcType)
		{
			case "L2Apc":
			case "L2Monster":
			case "L2RaidBoss":
			case "L2FestivalMonster":
			case "L2SepulcherMonster":
			case "L2Chest":
			case "L2RiftInvader":
				return true;
		}
		return false;
	}

	public boolean isRaid(String npcType)
	{
		switch (npcType)
		{
			case "L2RaidBoss":
				return true;
		}
		return false;
	}
//	public void parseSpawnsToXml() throws SQLException, IOException
//	{
//		final Map<Integer,L2Spawn> spawns = RaidBossSpawnManager.getSpawns();
//		
//		class Sorted implements Comparable<Sorted>
//		{
//			int _level;
//			String xml;
//			
//			public Sorted(int level, StringBuilder sb)
//			{
//				_level = level;
//				xml = sb.toString();
//			}
//			
//			@Override
//			public int compareTo(Sorted arg)
//			{
//				return _level - arg._level;
//			}
//		}
//		
//		ArrayList<Sorted> sortedList = new ArrayList<>(); 
//		for ( int npcId : spawns.keySet() )
//		{
//			try
//			{
//				if (NpcTable.getInstance().getTemplate(npcId) != null)
//				{
//					StringBuilder sb = new StringBuilder();
//					String levelstr = "0";
//					
//					levelstr = "" + NpcTable.getInstance().getTemplate(npcId).getLevel();
//					
//					sb.append("<!-- Level: " + levelstr + " -->\t\t<npc id=\"" + npcId + "\" x=\"" + spawns.get(npcId).getCurX() + "\" y=\"" + spawns.get(npcId).getCurY() + "\" z=\"" + spawns.get(npcId).getCurZ() + "\" heading=\"" + spawns.get(npcId).getHeading() + "\" respawnDelay=\"" + spawns.get(npcId).getRespawnMaxDelay() + "\" respawnDelay=\"" + spawns.get(npcId).getRespawnMaxDelay() + "\" respawnRandom=\"0\" /> <!--" + NpcTable.getInstance().getTemplate(npcId).getName() + "-->\n");
//					
//					int level = Integer.parseInt(levelstr);
//					
//					Sorted sd = new Sorted(level, sb);
//					sortedList.add(sd);
//				}
//				else
//					continue;
//			}
//			catch (Exception e)
//			{
//				e.printStackTrace();
//			}
//			
//		}
//		Collections.sort(sortedList);
//
//		File f = new File("data/xml/droplists/raid_spawndump_level_sort.xml");
//		FileWriter writer = new FileWriter(f, true);
//
//		if (!f.exists())
//			f.createNewFile();
//		
//		for(Sorted st : sortedList)
//		{
//			try
//			{
//				writer.write(st.xml);
//				System.out.println(st.xml);
//			}
//			finally
//			{
//			}
//		}
//	}
	
	
	
	public void parseTeleportsToXml() throws SQLException, IOException
	{
		final Map<Integer,L2TeleportLocation> teleports = TeleportLocationTable.getInstance().getTeleports();
		
		class Sorted implements Comparable<Sorted>
		{
			int _level;
			String xml;
			
			public Sorted(int level, StringBuilder sb)
			{
				_level = level;
				xml = sb.toString();
			}
			
			@Override
			public int compareTo(Sorted arg)
			{
				return _level - arg._level;
			}
		}
		
		ArrayList<Sorted> sortedList = new ArrayList<>(); 
		for ( int teleId : teleports.keySet() )
		{
			try
			{
					L2TeleportLocation tele = TeleportLocationTable.getInstance().getTemplate(teleId);
					StringBuilder sb = new StringBuilder();
					sb.append("\t<teleport id=\"" + tele.getTeleId() + "\" desc=\"" + tele.getDescription() + "\" >\n");
					sb.append("\t\t<set name=\"loc_x\" value=\"" + tele.getLocX() +"\" />\n");
					sb.append("\t\t<set name=\"loc_y\" value=\"" + tele.getLocY() +"\" />\n");
					sb.append("\t\t<set name=\"loc_z\" value=\"" + tele.getLocZ() +"\" />\n");
					sb.append("\t</teleport>\n");
					
					int level = tele.getTeleId();
					
					Sorted sd = new Sorted(level, sb);
					sortedList.add(sd);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
		}
		Collections.sort(sortedList);

		File f = new File("data/xml/droplists/teleports.xml");
		FileWriter writer = new FileWriter(f, true);

		if (!f.exists())
			f.createNewFile();
		
		for(Sorted st : sortedList)
		{
			try
			{
				writer.write(st.xml);
				System.out.println(st.xml);
			}
			finally
			{
			}
		}
	}
	
	public void parseDropsToXml() throws SQLException, IOException
	{
		final Map<Integer, L2NpcTemplate> npcs = NpcTable.getInstance().getNpcs();
		
		class Sorted implements Comparable<Sorted>
		{
			int _level;
			String xml;
			
			public Sorted(int level, StringBuilder sb)
			{
				_level = level;
				xml = sb.toString();
			}
			
			@Override
			public int compareTo(Sorted arg)
			{
				return _level - arg._level;
			}
		}
		
		ArrayList<Sorted> sortedList = new ArrayList<>(); 
		
		
		for ( int npcId : npcs.keySet() )
		{
			if(NpcTable.getInstance().getTemplate(npcId).getType().equalsIgnoreCase("L2RaidBoss"))
			{
				List<L2DropData> allDropData = NpcTable.getInstance().getTemplate(npcId).getAllDropData();
				List<L2DropCategory> dropData = NpcTable.getInstance().getTemplate(npcId).getDropData();
				
				if(allDropData == null || dropData == null)
					continue;
				if(allDropData.isEmpty() && dropData.isEmpty())
					continue;
				
				StringBuilder sb = new StringBuilder();
				
				sb.append("\t<npc npcId=\""+ npcId +"\" npcName=\"" + NpcTable.getInstance().getTemplate(npcId).getName() + "\" npcTitle=\"" + NpcTable.getInstance().getTemplate(npcId).getTitle()  +"\" npcLevel=\"" + NpcTable.getInstance().getTemplate(npcId).getLevel()  +"\" npcType=\"" + NpcTable.getInstance().getTemplate(npcId).getType() + "\">\n");
				sb.append("\t\t<drops>\n");
				
				npcs.get(npcId).getDropData().forEach(cat ->
				{
					sb.append("\t\t\t<category id=\""+ cat.getCategoryType() +"\">\n");
						cat.getAllDrops().forEach(cdrop ->
						{
								String name = "";
								String enchant = "";
								
								if(cdrop.getEnchantLevel() > 0)
									enchant = "+" + cdrop.getEnchantLevel() + " ";
								
								if (cdrop.getItemId() > 1000000)
									name = ItemLists.getInstance().getListName(cdrop.getItemId()) == null? "List Not Found" : enchant + ItemLists.getInstance().getListName(cdrop.getItemId()) + " - " + (float)(cdrop.getChance() / 10000) + "%";
								else
									name = ItemTable.getInstance().getTemplate(cdrop.getItemId()) == null? "Item Not Found" : enchant + ItemTable.getInstance().getTemplate(cdrop.getItemId()).getName() + " - " + (float)(cdrop.getChance() / 10000) + "%";
								
								sb.append("\t\t\t\t<drop id='"+ cdrop.getItemId() +"' min=\""+ cdrop.getMinDrop() + "\"" +" max=\"" + cdrop.getMaxDrop() + "\"" + " chance=\"" + cdrop.getChance() + "\"" + " enchant=\"" + cdrop.getEnchantLevel() + "\"" + " partydropmulti=\"" + cdrop.getPartyDropCount() + "\"" + " /> <!-- " + name + " -->\n");
								
						});
						sb.append("\t\t\t</category>\n");
						
				}
				);
				sb.append("\t\t</drops>\n");
				sb.append("\t</npc>\n");
				int level = NpcTable.getInstance().getTemplate(npcId).getLevel();
				
				Sorted sd = new Sorted(level, sb);
				sortedList.add(sd);
			}
		}
		Collections.sort(sortedList);

		File f = new File("data/xml/droplists/drops_raid_sql.xml");
		FileWriter writer = new FileWriter(f, true);

		if (!f.exists())
			f.createNewFile();
		
		for(Sorted st : sortedList)
		{
			try
			{
				writer.write(st.xml);
			}
			finally
			{
			}
		}
		convertNpcsToMobius();
	}	
		
	public void convertNpcsToMobius() throws SQLException, IOException
	{
		final Map<Integer, L2NpcTemplate> npcs = NpcTable.getInstance().getNpcs();
		
		class Sorted implements Comparable<Sorted>
		{
			int _level;
			String xml;
			
			public Sorted(int level, StringBuilder sb)
			{
				_level = level;
				xml = sb.toString();
			}
			
			@Override
			public int compareTo(Sorted arg)
			{
				return _level - arg._level;
			}
		}
		
		ArrayList<Sorted> sortedList = new ArrayList<>(); 
		
		
		for ( int npcId : npcs.keySet() )
		{
				StringBuilder sb = new StringBuilder();
				
				L2NpcTemplate npc = NpcTable.getInstance().getTemplate(npcId);
				
				//Npc Profile
				
				String name = npc.getName();
				int displayId = npc.getTemplateId();
				String title = npc.getTitle();
				
				String usingServerSideName = npc.serverSideName?"true":"false";
				String usingServerSideTitle = npc.serverSideTitle?"true":"false";
				
				
				int level = npc.getLevel();
				String type = npc.getType();
				String race = npc.getRace().toString().toUpperCase();
				String sex = npc.getSex().toUpperCase();
				
				//Npc Stats
				int STR = npc.baseSTR;
				int INT = npc.baseINT;
				int DEX = npc.baseDEX;
				int WIT = npc.baseWIT;
				int CON = npc.baseCON;
				int MEN = npc.baseMEN;
				
				//Vitals
				float hp = npc.baseHpMax;
				float mp = npc.baseMpMax;
				
				float hpRegen = npc.baseHpReg;
				float mpRegen = npc.baseMpReg;
				
				//Attack
				int attPhysical = npc.basePAtk;
				int attMagical = npc.baseMAtk;
				int random = 20;	
				int critical = npc.baseCritRate;
				int accuracy = 5; //Not Defined in L2Pride structure
				int attackSpeed = npc.basePAtkSpd;
				String wType = "SWORD"; //Weapon Type
				int attRange = npc.baseAtkRange;
				int distance = 80; //default
				int width = 120; //default
				
				
				//Defence
				int defPhysical = npc.basePDef;
				int defMagical = npc.baseMDef;
				

				//Attribute Attack
				int attFire = npc.baseFire;
				int attWater = npc.baseWater;
				int attWind = npc.baseWind;
				int attEarth = npc.baseEarth;
				int attHoly = npc.baseHoly;
				int attDark = npc.baseDark;
				
				//Attribute Defence
				int defFire = npc.baseFireRes;
				int defWater = npc.baseWaterRes;
				int defWind = npc.baseWindRes;
				int defEarth = npc.baseEarthRes;
				int defHoly = npc.baseHolyRes;
				int defDark = npc.baseDarkRes;
				
				//Speed
				int walkSpd = npc.baseWalkSpd;
				int runSpd = npc.baseRunSpd;

				//Abnormal ressist
				int resPhysical = 10;
				int resMagical = 10;
				
				String statusAttackable = "false";
			
				int radius = npc.getCollisionRadius();
				int height = npc.getCollisionHeight();
				long xp = npc.rewardExp;
				int sp = npc.rewardSp;
				
				
				
				boolean hasSkills = true;
				boolean hasDrops = true;
				
				if(npc.getAllDropData() == null || npc.getAllDropData().isEmpty())
				{
					hasDrops = false;
				}

				if(npc.getSkills() == null || npc.getSkills().isEmpty() || npc.getSkills().size() < 1)
				{
					hasSkills = false;
				}
				
				
				String t = "\t";
				String n = "\n";
				
				//Stats Start
				
				sb.append(t + "<npc id=\"" + npcId + "\" displayId=\"" + displayId + "\" name=\"" + name + "\" title=\"" + title + "\" usingServerSideName=\""+ usingServerSideName + "\" usingServerSideTitle=\"" + usingServerSideTitle + "\" level=\"" + level + "\" type=\"" + type.substring(2).toUpperCase() + "\" >");
				sb.append(n);
				sb.append(t+t+ "<race>" + race + "</race>");
				sb.append(n);
				sb.append(t+t+ "<sex>" + sex + "</sex>");
				sb.append(n);
				sb.append(t+t + "<stats str=\"" + STR + "\" int=\"" + INT + "\" dex=\"" +DEX + "\" wit=\"" + WIT + "\" con=\"" + CON + "\" men=\""+ MEN + "\">");
				sb.append(n);
				sb.append(t+t+t+ "<vitals hp=\"" + hp + "\" mp=\"" + mp +"\" hpRegen=\"" + hpRegen + "\" mpRegen=\"" + mpRegen + "\" />" );
				sb.append(n);
				sb.append(t+t+t+ "<attack physical=\"" + attPhysical + "\" magical=\"" + attMagical +"\" attackSpeed=\"" + attackSpeed + "\" range=\"" + attRange + "\" type=\"" + wType + "\" distance=\"" + distance + "\" width=\"" + width + "\" random=\"" + random + "\" critical=\"" + critical + "\" accuracy=\"" + accuracy + "\"/>" );
				sb.append(n);
				sb.append(t+t+t+ "<defence physical=\"" + defPhysical + "\" magical=\"" + defMagical +"\" />" );
				sb.append(n);
				sb.append(t+t+t+ "<attribute>" );
				sb.append(n);
				sb.append(t+t+t+t+ "<defence fire=\"" + defFire + "\" water=\"" + defWater + "\" wind=\"" + defWind + "\" earth=\"" + defEarth + "\" holy=\"" + defHoly + "\" dark=\"" + defDark + "\">");
				sb.append(n);
				if(attFire > 0 || attWater > 0 || attWind > 0 || attEarth > 0 || attHoly > 0 || attDark > 0)
				{
					sb.append(t+t+t+t+ "<attack fire=\"" + attFire + "\" water=\"" + attWater + "\" wind=\"" + attWind + "\" earth=\"" + attEarth + "\" holy=\"" + attHoly + "\" dark=\"" + attDark + "\">");
					sb.append(n);
				}
				sb.append(t+t+t+ "</attribute>" );
				sb.append(n);
				sb.append(t+t+t+ "<speed>" );
				sb.append(n);
				sb.append(t+t+t+t+ "<walk ground=\"" + walkSpd + "\"/>");
				sb.append(n);
				sb.append(t+t+t+t+ "<run ground=\"" + runSpd + "\"/>");
				sb.append(n);
				sb.append(t+t+t+ "</speed>" );
				sb.append(n);
				sb.append(t+t+ "</stats>");
				sb.append(n);
				sb.append(t+t+ "<status attackable=\"" + statusAttackable + "\" />");
				sb.append(n);
				sb.append(t+t+ "<collision>");
				sb.append(n);
				sb.append(t+t+t+ "<radius normal=\"" + radius + "\" />");
				sb.append(n);
				sb.append(t+t+t+ "<height normal=\"" + height + "\" />");
				sb.append(n);
				sb.append(t+t+ "</collision>");
				sb.append(n);
				
				if(npc.rewardExp > 0 || npc.rewardSp > 0)
				{

					sb.append(t+t+ "<acquire exp=\"" + xp + "\" sp=\"" + sp + "\" />");
					sb.append(n);
				}
				//Stats End
				
				
				//Skills Start
				
				if(hasSkills)
				{
					sb.append(t+t+ "<skills>");
					sb.append(n);
					
					for (int skillId : npc.getSkills().keySet())
					{
						L2Skill skill = npc.getSkills().get(skillId);
						
						sb.append(t+t+t+ "<skill id=\"" + skill.getId() + "\" level=\"" + skill.getLevel() + "\" /> <!-- " + skill.getName() + " -->");
						sb.append(n);
					}
					sb.append(t+t+ "</skills>");
					sb.append(n);
				}
				
				//Skills End
				
				
				//Drops Start
				if(hasDrops)
				{
					sb.append(t+t+ "<droplist>");
					sb.append(n);
					sb.append(t+t+t+"<drop>");
					sb.append(n);
					
					npcs.get(npcId).getDropData().forEach(cat ->
					{
							cat.getAllDrops().forEach(cdrop ->
							{
									String xname = "";
									String enchant = "";
									float chance = (float)(cdrop.getChance() / 10000);
									
									if(cdrop.getEnchantLevel() > 0)
										enchant = "+" + cdrop.getEnchantLevel() + " ";
									
									if (cdrop.getItemId() > 1000000)
										xname = ItemLists.getInstance().getListName(cdrop.getItemId()) == null? "List Not Found" : enchant + ItemLists.getInstance().getListName(cdrop.getItemId()) + " - " + (float)(cdrop.getChance() / 10000) + "%";
									else
										xname = ItemTable.getInstance().getTemplate(cdrop.getItemId()) == null? "Item Not Found" : enchant + ItemTable.getInstance().getTemplate(cdrop.getItemId()).getName() + " - " + (float)(cdrop.getChance() / 10000) + "%";
									
									sb.append(t+t+t+t+ "<item id='"+ cdrop.getItemId() +"' min=\""+ cdrop.getMinDrop() + "\"" +" max=\"" + cdrop.getMaxDrop() + "\"" + " chance=\"" + chance + "\"" + " enchant=\"" + cdrop.getEnchantLevel() + "\"" + " partydropmulti=\"" + cdrop.getPartyDropCount() + "\"" + " /> <!-- " + xname + " -->");
									sb.append(n);
							});
							
					}
					);
					sb.append(t+t+t+"</drop>");
					sb.append(n);
					sb.append(t+t+ "</droplist>");
					sb.append(n);
				}

				sb.append(t+ "</npc>");
				sb.append(n);

				Sorted sd = new Sorted(level, sb);
				sortedList.add(sd);
			

		}
		Collections.sort(sortedList);

		File f = new File("data/xml/droplists/npc_mobius_test_.xml");
		FileWriter writer = new FileWriter(f, true);

		if (!f.exists())
			f.createNewFile();
		
		for(Sorted st : sortedList)
		{
			try
			{
				writer.write(st.xml);
			}
			finally
			{
			}
		}
		writer.close();
	}	
	
	public void convertSkillsToMobius() throws SQLException, IOException
	{
		final TIntObjectHashMap<L2Skill> skills = SkillTable.getInstance().getAllSkills();
		
		class Sorted implements Comparable<Sorted>
		{
			int _level;
			String xml;
			
			public Sorted(int level, StringBuilder sb)
			{
				_level = level;
				xml = sb.toString();
			}
			
			@Override
			public int compareTo(Sorted arg)
			{
				return _level - arg._level;
			}
		}
		
		ArrayList<Sorted> sortedList = new ArrayList<>(); 
		
		ArrayList<L2Skill> allSkills = new ArrayList<>();
		
		for (int i : skills.keys())
		{
			allSkills.add(skills.get(i));
		}
		allSkills.forEach(skill -> 
		{
			String name = skill.getName();
			int level = skill.getLevel();
			int abnormalLvl = skill.getAbnormalLvl();
			
		});
		
		Collections.sort(sortedList);

		File f = new File("data/xml/droplists/npc_mobius_test_.xml");
		FileWriter writer = new FileWriter(f, true);

		if (!f.exists())
			f.createNewFile();
		
		for(Sorted st : sortedList)
		{
			try
			{
				writer.write(st.xml);
			}
			finally
			{
			}
		}
		writer.close();
	}	
	
	public static class InstanceHolder
	{
		private static final SpawnUtil _instance = new SpawnUtil();
	}
	
	public static final SpawnUtil getInstance()
	{
		return InstanceHolder._instance;
	}
}
