package luna.custom.guard;

import java.util.ArrayList;
import java.util.List;

import luna.custom.logger.LunaLogger;
import luna.custom.skilltrees.SkillTreesParser;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class LunaSkillGuard
{
	private static final int[]	PATH_SKILLS				= new int[]
	{
		9421, 9422, 9423, 9424, 9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433, 9434, 9435, 9436, 9437, 9438, 9439, 9440, 9441, 9442, 9443, 9444, 9445, 9446, 9447, 9448, 9449, 9450, 9451, 9452, 9453, 9454, 9455, 9456, 9457, 9458, 9459, 9460, 9461, 9462, 9463, 9464, 9465, 9466, 9467, 9468, 9469, 9470, 9471, 9472, 9473, 9474, 9475, 9476, 9477, 9478, 9479, 9480, 9481, 9482, 9483, 9484, 9485, 9486, 9487, 9488, 9489, 9490, 9491, 9492, 9493, 9494, 9495, 9496, 9497, 9498, 9499, 9500, 9501, 9502, 9503, 9504, 94270, 94271, 94300, 94302, 94350, 94351, 94390, 94391, 94501, 94504, 94510, 94512, 94570, 94571, 94600, 94603, 94710, 94711, 94740, 94741, 94770, 94772, 94830, 94832, 94920, 94922, 94950, 94952, 95010, 95012, 95040, 95041
	};
	private static final int[]	CERTIFICATION_SKILLS	= new int[]
	{
		35200, 35202, 35204, 35206, 35208, 35210, 35212, 35214, 35216, 35218, 35220, 35222, 35224, 35226, 35228, 35230, 35232, 35234, 35236, 35238, 35240, 35242, 35244, 35246, 35248, 35250, 35252, 35254, 35256, 35258, 35260, 35262, 35264
	};
	public static final int[]	HERO_SKILLS				=
	{
		395, 396, 1374, 1375, 1376, 12504, 12503, 12502, 12505, 12501, 12506, 12511, 12509, 12508, 12510
	};
	final static public int[]	MARRIAGE_SKILLS			=
	{
		12000, 12005, 12001, 12002, 12003, 12004
	};
	final static public int[]	PVP_SKILLS				=
	{
		7045, 426, 427, 7046, 7041, 7042, 7049, 7054, 7050, 7056,
		7055, 7064, 15000, 15001, 15002, 15003, 15004, 15005, 15006, 15007, 15008, 15009, 15010
	};
	final static public int[]	PVP_TRANSFORM_SKILLS	=
	{
		2428, 617, 3337, 674, 2631, 670, 2394, 618, 2670, 2671, 3336, 671, 3335, 673, 672, 5655, 546, 2511, 2632, 8246, 552, 555, 5261
	};
	final static public int[]	NOBLE_SKILLS			=
	{
		2428, 617, 3337, 674, 2631, 670, 2394, 618, 2670, 2671, 3336, 671, 3335, 673, 672, 5655, 546, 2511, 2632, 8246, 552, 555, 5261
	};
	final static public int[]	CLASSES					=
	{
		88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 131, 132, 133, 134, 136
	};
	
	public void checkForIncorrectSkills(L2PcInstance player)
	{
		boolean validClass = false;
		for (int id : CLASSES)
		{
			if (player.getClassId().getId() == id)
				validClass = true;
		}
		if (!validClass)
			return;
		List<Integer> pvp_skills = new ArrayList<>();
		for (int id : PVP_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : PVP_TRANSFORM_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : PATH_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : CERTIFICATION_SKILLS)
		{
			pvp_skills.add(id);
		}
		final ArrayList<L2Skill> skills = SkillTreesParser.getInstance().getClassSkills(player);
		final ArrayList<Integer> classSkillIds = new ArrayList<>();
		final ArrayList<Integer> allSkills = SkillTreesParser.getInstance().getAllSkills(player);
		final ArrayList<Integer> itemSkills = SkillTreesParser.getInstance().getAllItemSkills(player);
		skills.forEach(skill ->
		{
			classSkillIds.add(skill.getId());
		});
		if (!skills.isEmpty())
		{
			for (L2Skill skill : player.getAllSkills())
			{
				boolean isFromSkillTrees = false;
				if (classSkillIds.contains(skill.getId()))
				{
					continue;
				}
				if (pvp_skills.contains(skill.getId()))
				{
					continue;
				}
				if (itemSkills.contains(skill.getId()))
				{
					continue;
				}
				if (allSkills.contains(skill.getId()))
				{
					isFromSkillTrees = true;
				}
				if (!classSkillIds.contains(skill.getId()))
				{
					// System.out.println("removed skill: "+ skill.getName());
					//LunaLogger.getInstance().log("incorrect_skills", "Player: " + player.getName() + " has incorrect skill: "+ skill.getName() + " ID: " + skill.getId() + " on class: " +player.getClassId().getName());
					//player.removeSkill(skill.getId());
					//player.sendSkillList();
					continue;
				}
			}
		}
		player.sendSkillList();
	}
	
	public void checkForIncorrectSkillsSkillList(L2PcInstance player)
	{
		boolean validClass = false;
		for (int id : CLASSES)
		{
			if (player.getClassId().getId() == id)
				validClass = true;
		}
		if (!validClass)
			return;
		List<Integer> my_skills = new ArrayList<>();
		List<Integer> pvp_skills = new ArrayList<>();
		for (int id : PVP_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : PVP_TRANSFORM_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : PATH_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : CERTIFICATION_SKILLS)
		{
			pvp_skills.add(id);
		}
		final ArrayList<L2Skill> skills = SkillTreesParser.getInstance().getClassSkills(player);
		final ArrayList<Integer> allSkills = SkillTreesParser.getInstance().getAllSkills(player);
		final ArrayList<Integer> classSkillIds = new ArrayList<>();
		skills.forEach(skill ->
		{
			classSkillIds.add(skill.getId());
		});
		if (!skills.isEmpty())
		{
			for (L2Skill skill : player.getAllSkills())
			{
				if (classSkillIds.contains(skill.getId()))
				{
					continue;
				}
				if (pvp_skills.contains(skill.getId()))
				{
					continue;
				}
				if (!allSkills.contains(skill.getId()))
				{
					continue;
				}
				if (!classSkillIds.contains(skill.getId()))
				{
					// System.out.println("removed skill: "+ skill.getName());
					player.removeSkill(skill.getId());
					//player.getAllSkillsNew().remove(skill.getId());
					continue;
				}
			}
		}
	}
	
	public boolean checkSkill(L2PcInstance player, int SkillId)
	{
		boolean validClass = false;
		for (int id : CLASSES)
		{
			if (player.getClassId().getId() == id)
				validClass = true;
		}
		if (!validClass)
			return true;
		List<Integer> pvp_skills = new ArrayList<>();
		for (int id : PVP_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : PVP_TRANSFORM_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : PATH_SKILLS)
		{
			pvp_skills.add(id);
		}
		for (int id : CERTIFICATION_SKILLS)
		{
			pvp_skills.add(id);
		}
		final ArrayList<L2Skill> skills = SkillTreesParser.getInstance().getClassSkills(player);
		final ArrayList<Integer> itemSkills = SkillTreesParser.getInstance().getAllItemSkills(player);
		final ArrayList<Integer> classSkillIds = new ArrayList<>();
		SkillTreesParser.getInstance().getClassSkills(player).forEach(skill -> 
		{
			classSkillIds.add(skill.getId());
		});
		
			if (classSkillIds.contains(SkillId))
			{
				return true;
			}
			if (pvp_skills.contains(SkillId))
			{
				return true;
			}
			if (itemSkills.contains(SkillId))
			{
				return true;
			}
			if (!classSkillIds.contains(SkillId))
			{
				return false;
			}
		return false;
	}
	
	public static LunaSkillGuard getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		private static final LunaSkillGuard INSTANCE = new LunaSkillGuard();
	}
}
