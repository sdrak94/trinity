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
public class OnyxBeast extends L2Transformation
{
	public OnyxBeast()
	{
		// id, duration (secs), colRadius, colHeight
		super(1, 1800, 14.0, 15.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 1 || getPlayer().isCursedWeaponEquipped())
			return;

		// give transformation skills
		transformedSkills();
	}

	public void transformedSkills()
	{
		// Power Claw
		getPlayer().addSkill(SkillTable.getInstance().getInfo(584, 1), false);
		// Fast Moving
		getPlayer().addSkill(SkillTable.getInstance().getInfo(585, 1), false);
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
		// Power Claw
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(584, 1), false);
		// Fast Moving
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(585, 1), false, false);
		// Transfrom Dispel
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(619, 1), false);
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(5491, 1), false);

	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new OnyxBeast());
	}
}
