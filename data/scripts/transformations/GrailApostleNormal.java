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
public class GrailApostleNormal extends L2Transformation
{
	public GrailApostleNormal()
	{
		// id, duration (secs), colRadius, colHeight
		super(202, 1800, 8.0, 30.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 202 || getPlayer().isCursedWeaponEquipped())
			return;

		// give transformation skills
		transformedSkills();
	}

	public void transformedSkills()
	{
		// Spear
		getPlayer().addSkill(SkillTable.getInstance().getInfo(559, 3), false);
		// Power Slash
		getPlayer().addSkill(SkillTable.getInstance().getInfo(560, 3), false);
		// Bless of Angel
		getPlayer().addSkill(SkillTable.getInstance().getInfo(561, 3), false);
		// Wind of Angel
		getPlayer().addSkill(SkillTable.getInstance().getInfo(562, 3), false);
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
		// Spear
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(559, 3), false);
		// Power Slash
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(560, 3), false);
		// Bless of Angel
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(561, 3), false, false);
		// Wind of Angel
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(562, 3), false, false);
		// Transfrom Dispel
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(619, 1), false);
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(5491, 1), false);

	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new GrailApostleNormal());
	}
}
