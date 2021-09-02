//Update by rocknow
package transformations;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Transformation;


public class DwarfMercenary extends L2Transformation
{
	public DwarfMercenary()
	{
		// id, duration (secs), colRadius, colHeight
		super(14, 1800, 13.0, 29.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 14 || getPlayer().isCursedWeaponEquipped())
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
		TransformationManager.getInstance().registerTransformation(new DwarfMercenary());
	}
}
