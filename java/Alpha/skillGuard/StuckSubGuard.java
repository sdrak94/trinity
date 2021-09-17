package Alpha.skillGuard;

import net.sf.l2j.gameserver.datatables.ArmorSetsTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.CharEffectList;
import net.sf.l2j.gameserver.model.L2ArmorSet;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;

public class StuckSubGuard
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
	
	public void checkPlayer(L2PcInstance player)
	{
		//checkForSkills(player);
		checkForBuffs(player);
	}
	
	public void checkForBuffs(L2PcInstance player)
	{
		for (var ef : player.getAllEffects())
		{
			if (ef.getPeriod() < CharEffectList.BUFFER_BUFFS_DURATION)
			{
				if (ef.getSkill().getTargetType(player) == SkillTargetType.TARGET_SELF)
				{
					if (allowedSkill(player, ef.getSkill().getId()))
						continue;
					else
					{
						ef.exit();
						continue;
					}
				}
				else
					continue;
			}
		}
	}
	
	public void checkForSkills(L2PcInstance player)
	{
		for (var skill : player.getAllSkills())
		{
			if (allowedSkill(player, skill.getId()))
				continue;
			else
				player.removeSkill(skill);
		}
		player.sendSkillList();
	}
	
	public boolean allowedSkill(L2PcInstance player, int skillId)
	{
		for (var sk : SkillTreeTable.getInstance().getAllowedSkills(player.getClassId()))
		{
			if (sk.getId() == skillId)
				return true;
		}
		if (isAllowedId(player, skillId))
		{
			return true;
		}
		
		
		return false;
	}
	
	public boolean isAllowedId(L2PcInstance player, int id)
	{
		for (int i : PATH_SKILLS)
		{
			if (i == id)
				return true;
		}
		for (int i : CERTIFICATION_SKILLS)
		{
			if (i == id)
				return true;
		}
		for (int i : HERO_SKILLS)
		{
			if (i == id)
				return true;
		}
		for (int i : MARRIAGE_SKILLS)
		{
			if (i == id)
				return true;
		}
		for (int i : PVP_SKILLS)
		{
			if (i == id)
				return true;
		}
		for (int i : PVP_TRANSFORM_SKILLS)
		{
			if (i == id)
				return true;
		}
		for (int i : NOBLE_SKILLS)
		{
			if (i == id)
				return true;
		}
		for (var item : player.getInventory().getEquipedItems())
		{
			L2Item itm = ItemTable.getInstance().getTemplate(item.getItemId());
			if (itm.getItemSkills() != null)
			{
				for (var skill : itm.getItemSkills())
				{
					if (skill.getId() == id)
						return true;
					else
						continue;
				}
			}
		}
		if (player.getActiveChestArmorItem() != null)
		{
			L2ArmorSet as = ArmorSetsTable.getInstance().getSet(player.getActiveChestArmorItem().getItemId());
			if (as != null)
			{
				if (as.getSkills() != null)
				{
					for (String skillInfo : as.getSkills())
					{
						int skillId = 0;
						int skillLvl = 0;
						String[] skill = skillInfo.split("-");
						if (skill != null && skill.length == 2)
						{
							try
							{
								skillId = Integer.parseInt(skill[0]);
								skillLvl = Integer.parseInt(skill[1]);
							}
							catch (NumberFormatException e)
							{
								/* _log.warning("Inventory.ArmorSetListener: Incorrect skill: "+skillInfo+"."); */
								if (skillId > 0)
									skillLvl = 1;
							}
						}
						if (skillId == id)
							return true;
					}
				}
				if (id == as.getEnchant16skillId())
					return true;
				if (id == as.getShieldSkillId() && as.containShield(player.getActiveShieldItem().getItemId()))
					return true;
				if (id == L2ArmorSet.getHighEnchantSkillId(player))
					return true;
			}
		}
		if (player.getClan() != null)
		{
			if (player.getClan().getAllSkills() != null)
			{
				for (var a : player.getClan().getAllSkills())
				{
					if (id == a.getId())
						return true;
				}
			}
		}
		if (player.isTransformed())
		{
			if (player.getTransformation().getAllowedSkills() != null)
			{
				for (var b : player.getTransformation().getAllowedSkills())
				{
					if (b == id)
						return true;
				}
			}
		}
		return false;
	}
	
	public static StuckSubGuard getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final StuckSubGuard _instance = new StuckSubGuard();
	}
}
