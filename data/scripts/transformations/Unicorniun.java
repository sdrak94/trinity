package transformations;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Transformation;

public class Unicorniun extends L2Transformation
{
	public Unicorniun()
	{
		// id, duration (secs), colRadius, colHeight
		super(220, 300, 16.0, 27.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 220 || getPlayer().isCursedWeaponEquipped())
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

		getPlayer().addSkill(SkillTable.getInstance().getInfo(906, 4), false); // Rolling Step
		getPlayer().addSkill(SkillTable.getInstance().getInfo(907, 4), false); // Double Blast
		getPlayer().addSkill(SkillTable.getInstance().getInfo(908, 4), false); // Tornado Slash
		getPlayer().addSkill(SkillTable.getInstance().getInfo(909, 4), false); // Cat Roar
		getPlayer().addSkill(SkillTable.getInstance().getInfo(910, 4), false); // Energy Blast
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

		getPlayer().removeSkill(SkillTable.getInstance().getInfo(906, 4), false); // Rolling Step
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(907, 4), false); // Double Blast
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(908, 4), false); // Tornado Slash
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(909, 4), false); // Cat Roar
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(910, 4), false); // Energy Blast
	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new Unicorniun());
	}
}
