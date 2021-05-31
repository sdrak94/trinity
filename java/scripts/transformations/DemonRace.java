package scripts.transformations;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Transformation;

public class DemonRace extends L2Transformation
{
	public DemonRace()
	{
		// id, duration (secs), colRadius, colHeight
		super(221, 300, 16.0, 29.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 221 || getPlayer().isCursedWeaponEquipped())
			return;

		// give transformation skills
		transformedSkills();
	}

	public void transformedSkills()
	{
		// Dismount
		getPlayer().addSkill(SkillTable.getInstance().getInfo(619, 1), false);
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().addSkill(SkillTable.getInstance().getInfo(5491, 1), false);
		
		getPlayer().addSkill(SkillTable.getInstance().getInfo(901, 4), false); // 
		getPlayer().addSkill(SkillTable.getInstance().getInfo(902, 4), false); // 
		getPlayer().addSkill(SkillTable.getInstance().getInfo(903, 4), false); // 
		getPlayer().addSkill(SkillTable.getInstance().getInfo(904, 4), false); // 
		getPlayer().addSkill(SkillTable.getInstance().getInfo(905, 4), false); // 		
	}

	public void onUntransform()
	{
		// remove transformation skills
		removeSkills();
	}

	public void removeSkills()
	{
		// Dismount
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(619, 1), false);
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(5491, 1), false);

		getPlayer().removeSkill(SkillTable.getInstance().getInfo(901, 4), false); // Rolling Step
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(902, 4), false); // Double Blast
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(903, 4), false); // Tornado Slash
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(904, 4), false); // Cat Roar
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(905, 4), false); // Energy Blast
	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new DemonRace());
	}
}
