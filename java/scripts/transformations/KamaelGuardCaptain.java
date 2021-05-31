//Update by rocknow
package scripts.transformations;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Transformation;


public class KamaelGuardCaptain extends L2Transformation
{
	public KamaelGuardCaptain()
	{
		// id, duration (secs), colRadius, colHeight
		super(19, 1800, 12.0, 23.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 19 || getPlayer().isCursedWeaponEquipped())
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

		
	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new KamaelGuardCaptain());
	}
}
