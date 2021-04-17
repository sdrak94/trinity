//Update by rocknow
package transformations;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Transformation;


public class ElfTransform extends L2Transformation
{
	public ElfTransform()
	{
		// id, duration (secs), colRadius, colHeight
		super(997, 300, 16.0, 33.5);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 997 || getPlayer().isCursedWeaponEquipped())
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
		getPlayer().addSkill(SkillTable.getInstance().getInfo(10026, 1), false); // Greater Hydro
		getPlayer().addSkill(SkillTable.getInstance().getInfo(38050, 1), false); // Elemental Summoner-Transform Stats
		getPlayer().addSkill(SkillTable.getInstance().getInfo(1235, 1), false); // Hydro Blast
		getPlayer().addSkill(SkillTable.getInstance().getInfo(1236, 10), false); // Frost Bolt
		getPlayer().addSkill(SkillTable.getInstance().getInfo(4, 2), false); // Dash
		getPlayer().addSkill(SkillTable.getInstance().getInfo(1479, 1), false); // Magic Impulse
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
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(100026, 1), false); // Greater Hydro
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(38050, 1), false); // Elemental Summoner-Transform Stats
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(1236, 10), false); // Frost Bolt
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(4, 2), false); // Dash
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(1479, 1), false); // Magic Impulse
	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new ElfTransform());
	}
}
