//Update by rocknow
package scripts.transformations;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Transformation;


public class DelfTransform extends L2Transformation
{
	public DelfTransform()
	{
		// id, duration (secs), colRadius, colHeight
		super(996, 300, 16.0, 32.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 996 || getPlayer().isCursedWeaponEquipped())
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
		getPlayer().addSkill(SkillTable.getInstance().getInfo(38051, 1), false); // Spectral Master-Transform
		getPlayer().addSkill(SkillTable.getInstance().getInfo(928, 1), false); // Dual Blow
		getPlayer().addSkill(SkillTable.getInstance().getInfo(30, 47), false); // Backstab
		getPlayer().addSkill(SkillTable.getInstance().getInfo(821, 1), false); // Shadow Step
		getPlayer().addSkill(SkillTable.getInstance().getInfo(446, 1), false); // Dodge
		getPlayer().addSkill(SkillTable.getInstance().getInfo(1477, 2), false); // Vampiric Impulse
		getPlayer().addSkill(SkillTable.getInstance().getInfo(1514, 1), false); // Dodge
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
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(38051, 1), false); // Spectral Master-Transform
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(928, 1), false); // Dual Blow
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(30, 47), false); // Backstab
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(821, 1), false); // Shadow Step
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(446, 1), false); // Dodge
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(1477, 2), false); // Vampiric Impulse
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(1514, 1), false); // Dodge
	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new DelfTransform());
	}
}
