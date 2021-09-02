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
 * @author durgus
 *
 */
public class GolemGuardianWeak extends L2Transformation
{
	public GolemGuardianWeak()
	{
		// id, duration (secs), colRadius, colHeight
		super(212, 1800, 8.0, 23.5);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 212 || getPlayer().isCursedWeaponEquipped())
			return;

		// give transformation skills
		transformedSkills();
	}

	public void transformedSkills()
	{
		// Double Slasher
		getPlayer().addSkill(SkillTable.getInstance().getInfo(572, 2), false);
		// Earthquake
		getPlayer().addSkill(SkillTable.getInstance().getInfo(573, 2), false);
		// Bomb Installation
		getPlayer().addSkill(SkillTable.getInstance().getInfo(574, 2), false);
		// Steel Cutter
		getPlayer().addSkill(SkillTable.getInstance().getInfo(575, 2), false);
		// Transfrom Dispel
		getPlayer().addSkill(SkillTable.getInstance().getInfo(619, 1), false);
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().addSkill(SkillTable.getInstance().getInfo(5491, 1), false);

	}

	public void onUntransform()
	{
		// remove transformation skills
		removeSkills();
	}

	public void removeSkills()
	{
		// Double Slasher
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(572, 2), false);
		// Earthquake
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(573, 2), false);
		// Bomb Installation
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(574, 2), false);
		// Steel Cutter
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(575, 2), false);
		// Transfrom Dispel
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(619, 1), false);
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(5491, 1), false);

	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new GolemGuardianWeak());
	}
}
