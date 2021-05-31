//Update by rocknow
package scripts.transformations;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Transformation;


public class MyoRace extends L2Transformation
{
	public MyoRace()
	{
		// id, duration (secs), colRadius, colHeight
		super(219, 300, 16.0, 24.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 219 || getPlayer().isCursedWeaponEquipped())
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
		
		getPlayer().addSkill(SkillTable.getInstance().getInfo(896, 4), false); // Rolling Step
		getPlayer().addSkill(SkillTable.getInstance().getInfo(897, 4), false); // Double Blast
		getPlayer().addSkill(SkillTable.getInstance().getInfo(898, 4), false); // Tornado Slash
		getPlayer().addSkill(SkillTable.getInstance().getInfo(899, 4), false); // Cat Roar
		getPlayer().addSkill(SkillTable.getInstance().getInfo(900, 4), false); // Energy Blast
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

		getPlayer().removeSkill(SkillTable.getInstance().getInfo(896, 4), false); // Rolling Step
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(897, 4), false); // Double Blast
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(898, 4), false); // Tornado Slash
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(899, 4), false); // Cat Roar
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(900, 4), false); // Energy Blast
	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new MyoRace());
	}
}
