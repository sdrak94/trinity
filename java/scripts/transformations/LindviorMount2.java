package scripts.transformations;

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
public class LindviorMount2 extends L2Transformation
{
	public LindviorMount2()
	{
		// id, duration (secs), colRadius, colHeight
		super(60016, 1800, 20.0, 34.5);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 60016 || getPlayer().isCursedWeaponEquipped())
			return;
		
		transformedSkills();
	}

	public void transformedSkills()
	{
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().addSkill(SkillTable.getInstance().getInfo(5491, 1), false);
		// Dismount
		getPlayer().addSkill(SkillTable.getInstance().getInfo(839, 1), false);
		//getPlayer().setTransformAllowedSkills(SKILLS);

	}

	public void onUntransform()
	{
		// remove transformation skills
		removeSkills();
	}

	public void removeSkills()
	{
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(5491, 1), false);
		// Dismount
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(839, 1), false);
	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new LindviorMount2());
	}
}