package transformations;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Transformation;

/**
 * Description: <br>
 * This will handle the transformation, giving the skills, and removing them, when the player logs out and is transformed these skills
 * do not save. 
 * When the player logs back in, there will be a call from the enterworld packet that will add all their skills.
 * The enterworld packet will transform a player.
 * 
 * @author Ahmed
 *
 */
public class Zaken extends L2Transformation
{
	public Zaken()
	{
		// id, duration (secs), colRadius, colHeight
		super(305, 1800, 16.0, 32.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 305 || getPlayer().isCursedWeaponEquipped())
			return;

		// give transformation skills
		transformedSkills();
	}

	public void transformedSkills()
	{
		// Transfrom Dispel
		getPlayer().addSkill(SkillTable.getInstance().getInfo(619, 1), false);
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().addSkill(SkillTable.getInstance().getInfo(5491, 1), false);
		getPlayer().addSkill(SkillTable.getInstance().getInfo(715, 1), false);
		getPlayer().addSkill(SkillTable.getInstance().getInfo(716, 1), false);
		getPlayer().addSkill(SkillTable.getInstance().getInfo(717, 1), false);
		getPlayer().addSkill(SkillTable.getInstance().getInfo(718, 1), false);
		getPlayer().addSkill(SkillTable.getInstance().getInfo(719, 1), false);

	}

	public void onUntransform()
	{
		// remove transformation skills
		removeSkills();
	}

	public void removeSkills()
	{
		// Transfrom Dispel
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(619, 1), false);
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(5491, 1), false);
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(715, 1), false);
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(716, 1), false);
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(717, 1), false);
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(718, 1), false);
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(719, 1), false, false);

	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new Zaken());
	}
}