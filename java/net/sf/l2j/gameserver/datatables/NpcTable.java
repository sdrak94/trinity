package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEditNpc;
import net.sf.l2j.gameserver.model.L2DropCategory;
import net.sf.l2j.gameserver.model.L2DropData;
import net.sf.l2j.gameserver.model.L2MinionData;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

public class NpcTable
{
private static Logger _log = Logger.getLogger(NpcTable.class.getName());

private Map<Integer, L2NpcTemplate> _npcs;
public final Map<Integer, L2NpcTemplate> getNpcs()
{
	return _npcs;
}

private boolean _initialized = false;

public static int _highestNPCID = 0;

public static NpcTable getInstance()
{
	return SingletonHolder._instance;
}

private NpcTable()
{
	_npcs = new FastMap<Integer, L2NpcTemplate>();
	
	restoreNpcData();
}

@SuppressWarnings("resource")
private void restoreNpcData()
{
	Connection con = null;
	
	try
	{
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT "
					+ L2DatabaseFactory.getInstance().safetyString(new String[] { "id", "idTemplate", "name", "serverSideName",
							"title", "serverSideTitle", "elite", "rare", "class", "collision_radius", "collision_height", "level", "sex", "type",
							"attackrange", "hp", "mp", "hpreg", "mpreg", "str", "con", "dex", "int", "wit", "men", "exp", "sp", "patk",
							"pdef", "matk", "mdef", "atkspd", "aggro", "matkspd", "rhand", "lhand", "armor","can_move", "walkspd", "runspd",
							"faction_id", "faction_range", "isUndead", "absorb_level", "absorb_type", "atk_elements", "def_elements", "randomwalkrange", "solomob", "ss_rate", "ss_grade", "AI",
					"drop_herbs" }) + " FROM npc");
			ResultSet npcdata = statement.executeQuery();
			
			fillNpcTable(npcdata, false, true);
			npcdata.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error creating NPC table.", e);
		}
		if (Config.CUSTOM_NPC_TABLE) // reload certain NPCs
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement;
				statement = con.prepareStatement("SELECT "
						+ L2DatabaseFactory.getInstance().safetyString(new String[] { "id", "idTemplate", "name", "serverSideName",
								"title", "serverSideTitle", "elite", "rare", "class", "collision_radius", "collision_height", "level", "sex", "type",
								"attackrange", "hp", "mp", "hpreg", "mpreg", "str", "con", "dex", "int", "wit", "men", "exp", "sp",
								"patk", "pdef", "matk", "mdef", "atkspd", "aggro", "matkspd", "rhand", "lhand", "armor","can_move", "walkspd",
								"runspd", "faction_id", "faction_range", "isUndead", "absorb_level", "absorb_type", "atk_elements", "def_elements", "randomwalkrange", "solomob",
								"ss_rate", "ss_grade", "AI", "drop_herbs" }) + " FROM custom_npc");
				ResultSet npcdata = statement.executeQuery();
				
				fillNpcTable(npcdata, true, true);
				npcdata.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "NPCTable: Error creating custom NPC table.", e);
			}
		}
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT npcid, skillid, level FROM npcskills");
			ResultSet npcskills = statement.executeQuery();
			L2NpcTemplate npcDat = null;
			L2Skill npcSkill = null;
			
			while (npcskills.next())
			{
				int mobId = npcskills.getInt("npcid");
				npcDat = _npcs.get(mobId);
				
				if (npcDat == null)
					continue;
				
				int skillId = npcskills.getInt("skillid");
				int level = npcskills.getInt("level");
				
				if (npcDat.race == null && skillId == 4416)
				{
					npcDat.setRace(level);
					continue;
				}
				
				npcSkill = SkillTable.getInstance().getInfo(skillId, level);
				
				if (npcSkill == null)
					continue;
				
				npcDat.addSkill(npcSkill);
			}
			
			npcskills.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error reading NPC skills table.", e);
		}
		//TODO DROPLIST HERE
		try
		{
			/*
File file = new File("data/xml/droplists/droplist.xml");
final Document doc = XMLDocumentFactory.getInstance().loadDocument(file);

//L2DropData dropDat = null;
L2NpcTemplate npcDat = null;

//dropDat = new L2DropData();


for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
{
	if (n.getNodeName().equalsIgnoreCase("droplist"))
	{
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (d.getNodeName().equalsIgnoreCase("npc"))
			{
				final int npcId = Integer.parseInt(d.getAttributes().getNamedItem("npcId").getNodeValue());
				String npcName = d.getAttributes().getNamedItem("npcName").getNodeValue();
				String npcType = d.getAttributes().getNamedItem("npcType").getNodeValue();
				npcDat = _npcs.get(npcId);


				for (Node drops = d.getFirstChild(); drops != null; drops = drops.getNextSibling())
				{
					if ("drops".equalsIgnoreCase(drops.getNodeName()))
					{
						for (Node cat = drops.getFirstChild(); cat != null; cat = cat.getNextSibling())
						{
							if ("category".equalsIgnoreCase(cat.getNodeName()))
							{
								int category = Integer.parseInt(cat.getAttributes().getNamedItem("id").getNodeValue());

								for (Node drop = cat.getFirstChild(); drop != null; drop = drop.getNextSibling())
								{
									if ("drop".equalsIgnoreCase(drop.getNodeName()))
									{

										L2DropData dropDat = new L2DropData();
										dropDat.setItemId(Integer.parseInt(drop.getAttributes().getNamedItem("id").getNodeValue()));
										dropDat.setMinDrop(Integer.parseInt(drop.getAttributes().getNamedItem("min").getNodeValue()));
										dropDat.setMaxDrop(Integer.parseInt(drop.getAttributes().getNamedItem("max").getNodeValue()));
										dropDat.setChance(Integer.parseInt(drop.getAttributes().getNamedItem("chance").getNodeValue()));
										dropDat.setEnchantLevel(Integer.parseInt(drop.getAttributes().getNamedItem("enchant").getNodeValue()));
										dropDat.setPartyDropCount(Integer.parseInt(drop.getAttributes().getNamedItem("partydropmulti").getNodeValue()));
										final Node fkc = drop.getAttributes().getNamedItem("fakechance");
										if (fkc != null)
										dropDat.setFakeChance(Double.parseDouble(fkc.getNodeValue().replace(",", ".")));
										
										npcDat.addDropData(dropDat, category);
									}
								}
							}

						}
					}
				}
			}
		}
	}
}

}*/
	PreparedStatement statement2 = con.prepareStatement("SELECT "
					+ L2DatabaseFactory.getInstance().safetyString(new String[] { "mobId", "itemId", "min", "max", "category", "chance", "enchant", "partydropmulti" })
					+ " FROM droplist ORDER BY mobId DESC, chance DESC");
			ResultSet dropData = statement2.executeQuery();
			L2DropData dropDat = null;
			L2NpcTemplate npcDat = null;
			
			while (dropData.next())
			{
				int mobId = dropData.getInt("mobId");
				npcDat = _npcs.get(mobId);
				if (npcDat == null)
				{
					_log.warning("NPCTable: Drop data for undefined NPC. npcId: " + mobId);
					continue;
				}
				dropDat = new L2DropData();
				
				dropDat.setItemId(dropData.getInt("itemId"));
				dropDat.setMinDrop(dropData.getInt("min"));
				dropDat.setMaxDrop(dropData.getInt("max"));
				dropDat.setChance(dropData.getInt("chance"));
				dropDat.setEnchantLevel(dropData.getInt("enchant"));
				dropDat.setPartyDropCount(dropData.getInt("partydropmulti"));

				
				
				int category = dropData.getInt("category");

				//int chance = dropData.getInt("chance");
				
				npcDat.addDropData(dropDat, category);
			}
		}
			
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error reading NPC dropdata. ", e);
		}
		
		if (Config.CUSTOM_DROPLIST_TABLE)
		{
			try
			{
				PreparedStatement statement2 = con.prepareStatement("SELECT "
						+ L2DatabaseFactory.getInstance().safetyString(new String[] { "mobId", "itemId", "min", "max", "category",
								"chance", "enchant" }) + " FROM custom_droplist ORDER BY mobId, chance DESC");
				ResultSet dropData = statement2.executeQuery();
				L2DropData dropDat = null;
				L2NpcTemplate npcDat = null;
				int cCount = 0;
				while (dropData.next())
				{
					int mobId = dropData.getInt("mobId");
					npcDat = _npcs.get(mobId);
					if (npcDat == null)
					{
						_log.warning("NPCTable: CUSTOM DROPLIST: Drop data for undefined NPC. npcId: " + mobId);
						continue;
					}
					dropDat = new L2DropData();
					dropDat.setItemId(dropData.getInt("itemId"));
					dropDat.setMinDrop(dropData.getInt("min"));
					dropDat.setMaxDrop(dropData.getInt("max"));
					dropDat.setChance(dropData.getInt("chance"));
					dropDat.setEnchantLevel(dropData.getInt("enchant"));
					
					int category = dropData.getInt("category");
					npcDat.addDropData(dropDat, category);
					cCount++;
				}
				dropData.close();
				statement2.close();
				_log.info("CustomDropList: Added " + cCount + " custom droplist.");
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "NPCTable: Error reading NPC custom dropdata.", e);
			}
		}
		
		try
		{
			PreparedStatement statement3 = con.prepareStatement("SELECT "
					+ L2DatabaseFactory.getInstance().safetyString(new String[] { "npc_id", "class_id" }) + " FROM skill_learn");
			ResultSet learndata = statement3.executeQuery();
			
			while (learndata.next())
			{
				int npcId = learndata.getInt("npc_id");
				int classId = learndata.getInt("class_id");
				L2NpcTemplate npc = getTemplate(npcId);
				
				if (npc == null)
				{
					_log.warning("NPCTable: Error getting NPC template ID " + npcId + " while trying to load skill trainer data.");
					continue;
				}
				
				npc.addTeachInfo(ClassId.values()[classId]);
			}
			
			learndata.close();
			statement3.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error reading NPC trainer data.", e);
		}
		
		try
		{
			PreparedStatement statement4 = con.prepareStatement("SELECT "
					+ L2DatabaseFactory.getInstance().safetyString(new String[] { "boss_id", "minion_id", "amount_min", "amount_max" })
					+ " FROM minions");
			ResultSet minionData = statement4.executeQuery();
			L2MinionData minionDat = null;
			L2NpcTemplate npcDat = null;
			int cnt = 0;
			
			while (minionData.next())
			{
				int raidId = minionData.getInt("boss_id");
				npcDat = _npcs.get(raidId);
				if (npcDat == null)
				{
					_log.warning("Minion references undefined boss NPC. Boss NpcId: " + raidId);
					continue;
				}
				minionDat = new L2MinionData();
				minionDat.setMinionId(minionData.getInt("minion_id"));
				minionDat.setAmountMin(minionData.getInt("amount_min"));
				minionDat.setAmountMax(minionData.getInt("amount_max"));
				npcDat.addRaidData(minionDat);
				cnt++;
			}
			
			minionData.close();
			statement4.close();
			_log.config("NpcTable: Loaded " + cnt + " Minions.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error loading minion data.", e);
		}
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			// nothing
		}
	}
	
	_initialized = true;
}

public void loadMinionList(int npcID)
{
	L2NpcTemplate temp = getTemplate(npcID);
	
	try
	{
		if (temp != null)
			temp.getMinionData().clear();
	}
	catch (Exception e1)
	{
	}
	
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement4 = con.prepareStatement("SELECT "
				+ L2DatabaseFactory.getInstance().safetyString(new String[] { "minion_id", "amount_min", "amount_max" })
				+ " FROM minions WHERE boss_id=?");
		statement4.setInt(1, npcID);
		ResultSet minionData = statement4.executeQuery();
		L2MinionData minionDat = null;
		L2NpcTemplate npcDat = null;
		int cnt = 0;
		
		while (minionData.next())
		{
			npcDat = _npcs.get(npcID);
			if (npcDat == null)
			{
				_log.warning("Minion references undefined boss NPC. Boss NpcId: " + npcID);
				continue;
			}
			minionDat = new L2MinionData();
			minionDat.setMinionId(minionData.getInt("minion_id"));
			minionDat.setAmountMin(minionData.getInt("amount_min"));
			minionDat.setAmountMax(minionData.getInt("amount_max"));
			npcDat.addRaidData(minionDat);
			cnt++;
		}
		
		minionData.close();
		statement4.close();
		_log.config("Reloaded " + cnt + " Minions.");
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "NPCTable: Error Reloading minion data.", e);
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
		}
	}
}

private void fillNpcTable(ResultSet NpcData, boolean customData, boolean updateLastNPC) throws Exception
{
	int count = 0;
	while (NpcData.next())
	{
		StatsSet npcDat = new StatsSet();
		int id = NpcData.getInt("id");
		
		npcDat.set("npcId", id);
		npcDat.set("idTemplate", NpcData.getInt("idTemplate"));
		int level = NpcData.getInt("level");
		npcDat.set("level", level);
		npcDat.set("jClass", NpcData.getString("class"));
		
		npcDat.set("baseShldDef", 0);
		npcDat.set("baseShldRate", 0);
		npcDat.set("baseCritRate", 38);
		
		final String name = NpcData.getString("name");
		npcDat.set("name", name);
		npcDat.set("serverSideName", NpcData.getBoolean("serverSideName"));
		npcDat.set("title", NpcData.getString("title"));
		npcDat.set("serverSideTitle", NpcData.getBoolean("serverSideTitle"));
		npcDat.set("elite", NpcData.getInt("elite"));
		npcDat.set("rare", NpcData.getInt("rare"));
		npcDat.set("collision_radius", NpcData.getFloat("collision_radius"));
		npcDat.set("collision_height", NpcData.getFloat("collision_height"));
		npcDat.set("fcollision_radius", NpcData.getFloat("collision_radius"));
		npcDat.set("fcollision_height", NpcData.getFloat("collision_height"));
		npcDat.set("sex", NpcData.getString("sex"));
		npcDat.set("type", NpcData.getString("type"));
		npcDat.set("baseAtkRange", NpcData.getInt("attackrange"));
		npcDat.set("rewardExp", NpcData.getInt("exp"));
		npcDat.set("rewardSp", NpcData.getInt("sp"));
		npcDat.set("basePAtkSpd", NpcData.getInt("atkspd"));
		npcDat.set("baseMAtkSpd", NpcData.getInt("matkspd"));
		npcDat.set("aggroRange", NpcData.getInt("aggro"));
		npcDat.set("rhand", NpcData.getInt("rhand"));
		npcDat.set("lhand", NpcData.getInt("lhand"));
		npcDat.set("armor", NpcData.getInt("armor"));
		npcDat.set("can_move", NpcData.getBoolean("can_move"));
		npcDat.set("baseWalkSpd", NpcData.getInt("walkspd"));
		npcDat.set("baseRunSpd", NpcData.getInt("runspd"));
		
		// constants, until we have stats in DB
		npcDat.safeSet("baseSTR", NpcData.getInt("str"), 0, Formulas.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseCON", NpcData.getInt("con"), 0, Formulas.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseDEX", NpcData.getInt("dex"), 0, Formulas.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseINT", NpcData.getInt("int"), 0, Formulas.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseWIT", NpcData.getInt("wit"), 0, Formulas.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseMEN", NpcData.getInt("men"), 0, Formulas.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		
		npcDat.set("baseHpMax", NpcData.getInt("hp"));
		npcDat.set("baseCpMax", 0);
		npcDat.set("baseMpMax", NpcData.getInt("mp"));
		npcDat.set("baseHpReg", NpcData.getFloat("hpreg") > 0 ? NpcData.getFloat("hpreg") : 1.5 + ((level - 1) / 10.0));
		npcDat.set("baseMpReg", NpcData.getFloat("mpreg") > 0 ? NpcData.getFloat("mpreg") : 0.9 + 0.3 * ((level - 1) / 10.0));
		npcDat.set("basePAtk", NpcData.getInt("patk"));
		npcDat.set("basePDef", NpcData.getInt("pdef"));
		npcDat.set("baseMAtk", NpcData.getInt("matk"));
		npcDat.set("baseMDef", NpcData.getInt("mdef"));
		
		npcDat.set("factionId", NpcData.getString("faction_id"));
		npcDat.set("factionRange", NpcData.getInt("faction_range"));
		
		npcDat.set("isUndead", NpcData.getString("isUndead"));
		
		npcDat.set("absorb_level", NpcData.getString("absorb_level"));
		npcDat.set("absorb_type", NpcData.getString("absorb_type"));
		
		npcDat.set("atk_elements", NpcData.getString("atk_elements"));
		npcDat.set("def_elements", NpcData.getString("def_elements"));
		
		npcDat.set("randomwalkrange", NpcData.getInt("randomwalkrange"));
		npcDat.set("solomob", NpcData.getInt("solomob"));
		npcDat.set("ssRate", NpcData.getInt("ss_rate"));
		npcDat.set("ssGrade", NpcData.getInt("ss_grade"));
		
		npcDat.set("AI", NpcData.getString("AI"));
		
		npcDat.set("drop_herbs", Boolean.valueOf(NpcData.getString("drop_herbs")));
		
		L2NpcTemplate template = new L2NpcTemplate(npcDat);
		template.addVulnerability(Stats.BOW_WPN_VULN, 1);
		template.addVulnerability(Stats.CROSSBOW_WPN_VULN, 1);
		template.addVulnerability(Stats.BLUNT_WPN_VULN, 1);
		template.addVulnerability(Stats.DAGGER_WPN_VULN, 1);
		
		if (template.isLevelOneRB() || template.isLevelTwoRB())
		{
			template.addSkill(SkillTable.getInstance().getInfo(4197, 12)); //single target root
		}
		else if (template.getFactionId() != null && template.getFactionId().equalsIgnoreCase("dvc") && Rnd.get(2) == 0)
		{
			template.addSkill(SkillTable.getInstance().getInfo(4197, 1)); //single target root lvl 1
		}

		if(name.equals("Reanimated Man"))
		{
			template.addSkill(SkillTable.getInstance().getInfo(9283, 1));

		}
		else
		if(name.equals("Corrupted Man"))
		{
			template.addSkill(SkillTable.getInstance().getInfo(9284, 1));

		}
		else
		if(name.equals("Cursed Man"))
		{
			template.addSkill(SkillTable.getInstance().getInfo(9285, 1));

		}
		else
		if(name.equals("Dark Panther"))
		{
			template.addSkill(SkillTable.getInstance().getInfo(9286, 1));

		}
		else
		if(name.equals("Fang of Eva"))
		{
			template.addSkill(SkillTable.getInstance().getInfo(9287, 1));

		}
		else
		if(name.equals("Mechanic Golem"))
		{
			template.addSkill(SkillTable.getInstance().getInfo(9288, 1));

		}
		
		_npcs.put(id, template);
		
		if (updateLastNPC)
		{
			if (id > _highestNPCID)
				_highestNPCID = id;
		}
	}
	
	if (!customData)
		_log.config("NpcTable: (Re)Loaded " + count + " NPC template(s).");
	else
		_log.config("NpcTable: (Re)Loaded " + count + " custom NPC template(s).");
}

public void reloadNpc(int id)
{
	Connection con = null;
	
	try
	{
		// save a copy of the old data
		L2NpcTemplate old = getTemplate(id);
		Map<Integer, L2Skill> skills = new FastMap<Integer, L2Skill>();
		
		if (old.getSkills() != null)
			skills.putAll(old.getSkills());
		
		FastList<L2DropCategory> categories = new FastList<L2DropCategory>();
		
		if (old.getDropData() != null)
			categories.addAll(old.getDropData());
		
		ClassId[] classIds = null;
		
		if (old.getTeachInfo() != null)
			classIds = old.getTeachInfo().clone();
		
		/*		List<L2MinionData> minions = new FastList<L2MinionData>();
		
		if (old.getMinionData() != null)
			minions.addAll(old.getMinionData());*/
		
		// reload the NPC base data
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement st = con.prepareStatement("SELECT "
				+ L2DatabaseFactory.getInstance().safetyString(new String[] { "id", "idTemplate", "name", "serverSideName", "title",
						"serverSideTitle", "elite", "rare", "class", "collision_radius", "collision_height", "level", "sex", "type", "attackrange",
						"hp", "mp", "hpreg", "mpreg", "str", "con", "dex", "int", "wit", "men", "exp", "sp", "patk", "pdef", "matk",
						"mdef", "atkspd", "aggro", "matkspd", "rhand", "lhand", "armor","can_move", "walkspd", "runspd", "faction_id",
						"faction_range", "isUndead", "absorb_level", "absorb_type", "atk_elements", "def_elements", "randomwalkrange", "solomob", "ss_rate", "ss_grade", "AI", "drop_herbs" })
						+ " FROM npc WHERE id=?");
		st.setInt(1, id);
		ResultSet rs = st.executeQuery();
		fillNpcTable(rs, false, true);
		if (Config.CUSTOM_NPC_TABLE) // reload certain NPCs
		{
			st = con.prepareStatement("SELECT "
					+ L2DatabaseFactory.getInstance().safetyString(new String[] { "id", "idTemplate", "name", "serverSideName",
							"title", "serverSideTitle", "elite", "rare", "class", "collision_radius", "collision_height", "level", "sex", "type",
							"attackrange", "hp", "mp", "hpreg", "mpreg", "str", "con", "dex", "int", "wit", "men", "exp", "sp", "patk",
							"pdef", "matk", "mdef", "atkspd", "aggro", "matkspd", "rhand", "lhand", "armor","can_move", "walkspd", "runspd",
							"faction_id", "faction_range", "isUndead", "absorb_level", "absorb_type", "atk_elements", "def_elements", "randomwalkrange", "solomob", "ss_rate", "ss_grade", "AI",
					"drop_herbs" }) + " FROM custom_npc WHERE id=?");
			st.setInt(1, id);
			rs = st.executeQuery();
			fillNpcTable(rs, true, true);
		}
		rs.close();
		st.close();
		
		// restore additional data from saved copy
		L2NpcTemplate created = getTemplate(id);
		
		for (L2Skill skill : skills.values())
			created.addSkill(skill);
		
		if (classIds != null)
			for (ClassId classId : classIds)
				created.addTeachInfo(classId);
		
		loadMinionList(id);
		
		/*		for (L2MinionData minion : minions)
			created.addRaidData(minion);*/
	}
	catch (Exception e)
	{
		_log.warning("NPCTable: Could not reload data for NPC " + id + ": " + e);
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
}

public void addNewNpc(int id)
{
	Connection con = null;
	PreparedStatement st;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		st = con.prepareStatement("SELECT "
				+ L2DatabaseFactory.getInstance().safetyString(new String[] { "id", "idTemplate", "name", "serverSideName",
						"title", "serverSideTitle", "elite", "rare", "class", "collision_radius", "collision_height", "level", "sex", "type",
						"attackrange", "hp", "mp", "hpreg", "mpreg", "str", "con", "dex", "int", "wit", "men", "exp", "sp", "patk",
						"pdef", "matk", "mdef", "atkspd", "aggro", "matkspd", "rhand", "lhand", "armor","can_move", "walkspd", "runspd",
						"faction_id", "faction_range", "isUndead", "absorb_level", "absorb_type", "atk_elements", "def_elements", "randomwalkrange", "solomob", "ss_rate", "ss_grade", "AI",
				"drop_herbs" }) + " FROM custom_npc WHERE id=?");
		st.setInt(1, id);
		ResultSet rs = st.executeQuery();
		fillNpcTable(rs, true, true);
		
		rs.close();
		st.close();
		
		FakePcsTable.getInstance().load();
		AdminEditNpc.reLoadNpcSkillList(id);
		loadMinionList(id);
	}
	catch (Exception e)
	{
		_log.warning("NPCTable: Could not finalize the addition of new NPC " + id + ": " + e);
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
}

// just wrapper
public void reloadAllNpc()
{
	restoreNpcData();
}

public void saveNpc(StatsSet npc)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		Map<String, Object> set = npc.getSet();
		
		int length = 0;
		
		for (Object obj : set.keySet())
		{
			// 15 is just guessed npc name length
			length += ((String) obj).length() + 7 + 15;
		}
		
		final StringBuilder sbValues = new StringBuilder(length);
		
		for (Object obj : set.keySet())
		{
			final String name = (String) obj;
			
			if (!name.equalsIgnoreCase("npcId"))
			{
				if (sbValues.length() > 0)
				{
					sbValues.append(", ");
				}
				
				sbValues.append(name);
				sbValues.append(" = '");
				sbValues.append(set.get(name));
				sbValues.append('\'');
			}
		}
		
		
		int updated = 0;
		if (Config.CUSTOM_NPC_TABLE)
		{
			final StringBuilder sbQuery = new StringBuilder(sbValues.length() + 28);
			sbQuery.append("UPDATE custom_npc SET ");
			sbQuery.append(sbValues.toString());
			sbQuery.append(" WHERE id = ?");
			PreparedStatement statement = con.prepareStatement(sbQuery.toString());
			statement.setInt(1, npc.getInteger("npcId"));
			updated = statement.executeUpdate();
			statement.close();
		}
		if (updated == 0)
		{
			final StringBuilder sbQuery = new StringBuilder(sbValues.length() + 28);
			sbQuery.append("UPDATE npc SET ");
			sbQuery.append(sbValues.toString());
			sbQuery.append(" WHERE id = ?");
			PreparedStatement statement = con.prepareStatement(sbQuery.toString());
			statement.setInt(1, npc.getInteger("npcId"));
			statement.executeUpdate();
			statement.close();
		}
	}
	catch (Exception e)
	{
		_log.warning("NPCTable: Could not store new NPC data in database: " + e);
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
}

public boolean isInitialized()
{
	return _initialized;
}

public void replaceTemplate(L2NpcTemplate npc)
{
	_npcs.put(npc.npcId, npc);
}

public L2NpcTemplate getTemplate(int id)
{
	return _npcs.get(id);
}

public L2NpcTemplate getTemplateByName(String name)
{
	for (L2NpcTemplate npcTemplate : _npcs.values())
		if (npcTemplate.name.equalsIgnoreCase(name))
			return npcTemplate;
	
	return null;
}

public L2NpcTemplate[] getAllOfLevel(int lvl)
{
	List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
	
	for (L2NpcTemplate t : _npcs.values())
		if (t.level == lvl)
			list.add(t);
	
	return list.toArray(new L2NpcTemplate[list.size()]);
}

public L2NpcTemplate[] getAllMonstersOfLevel(int lvl)
{
	List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
	
	for (L2NpcTemplate t : _npcs.values())
		if (t.level == lvl && "L2Monster".equals(t.type))
			list.add(t);
	
	return list.toArray(new L2NpcTemplate[list.size()]);
}

public L2NpcTemplate[] getAllNpcStartingWith(String letter)
{
	List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
	
	for (L2NpcTemplate t : _npcs.values())
		if (t.name.startsWith(letter) && "L2Npc".equals(t.type))
			list.add(t);
	
	return list.toArray(new L2NpcTemplate[list.size()]);
}

/**
 * @return
 */
public Set<Integer> getAllNpcOfClassType(String classType)
{
	return null;
}

/**
 * @return
 */
public Set<Integer> getAllNpcOfL2jClass(Class<?> clazz)
{
	return null;
}

/**
 * @return
 */
public Set<Integer> getAllNpcOfAiType(String aiType)
{
	return null;
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final NpcTable _instance = new NpcTable();
}
}
