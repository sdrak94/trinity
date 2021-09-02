package transformations;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Transformation;

public class InquisitorShilienElder extends L2Transformation
{
	public InquisitorShilienElder()
	{
		// id, duration (secs), colRadius, colHeight
		super(318);
	}
	
	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 318 || getPlayer().isCursedWeaponEquipped())
			return;

		// give transformation skills
		transformedSkills();
	}
	
	public void transformedSkills()
	{
			// Divine Punishment
			getPlayer().addSkill(SkillTable.getInstance().getInfo(1523, 42), false);
			// Divine Flash
			getPlayer().addSkill(SkillTable.getInstance().getInfo(1528, 42), false);
			// Holy Weapon
			getPlayer().addSkill(SkillTable.getInstance().getInfo(1043, 1), false);
			// Surrender to the Holy
			getPlayer().addSkill(SkillTable.getInstance().getInfo(1524, 42), false);
			// Divine Curse
			getPlayer().addSkill(SkillTable.getInstance().getInfo(1525, 42), false);

		// Decrease Bow/Crossbow Attack Speed
		getPlayer().addSkill(SkillTable.getInstance().getInfo(5491, 1), false); 
		// Switch Stance
		getPlayer().addSkill(SkillTable.getInstance().getInfo(838, 1), false);
	}
	
	public void onUntransform()
	{
		// remove transformation skills
		removeSkills();
	}
	
	public void removeSkills()
	{
		// Divine Punishment
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(1523, 42), false);
		// Divine Flash
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(1528, 42), false);
		// Holy Weapon
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(1043, 1), false, false);
		// Surrender to the Holy
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(1524, 42), false);
		// Divine Curse
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(1525, 42), false);
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(5491, 1), false); 
		// Switch Stance
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(838, 1), false);
	}
	
	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new InquisitorShilienElder());
	}
}