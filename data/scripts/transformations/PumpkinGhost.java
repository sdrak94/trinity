package transformations;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Transformation;

public class PumpkinGhost extends L2Transformation
{
	public PumpkinGhost()
	{
		// id, duration (secs), colRadius, colHeight
		super(108, 3600, 18.0, 32.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 108 || getPlayer().isCursedWeaponEquipped())
			return;

		// give transformation skills
		transformedSkills();
	}

	public void transformedSkills()
	{
		// Transfrom Dispel
		getPlayer().addSkill(SkillTable.getInstance().getInfo(619, 1), false);
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().addSkill(SkillTable.getInstance().getInfo(5437, 2), false);

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
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(5437, 2), false);

	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new PumpkinGhost());
	}
}
