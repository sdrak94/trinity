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
 * @author durgus
 *
 */
public class DragonBomberWeak extends L2Transformation
{
	public DragonBomberWeak()
	{
		// id, duration (secs), colRadius, colHeight
		super(218, 1800, 8.0, 22.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 218 || getPlayer().isCursedWeaponEquipped())
			return;

		// give transformation skills
		transformedSkills();
	}

	public void transformedSkills()
	{
		// Death Blow
		getPlayer().addSkill(SkillTable.getInstance().getInfo(580, 2), false);
		// Sand Cloud
		getPlayer().addSkill(SkillTable.getInstance().getInfo(581, 2), false);
		// Scope Bleed
		getPlayer().addSkill(SkillTable.getInstance().getInfo(582, 2), false);
		// Assimilation
		getPlayer().addSkill(SkillTable.getInstance().getInfo(583, 2), false);
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
		// Death Blow
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(580, 2), false);
		// Sand Cloud
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(581, 2), false);
		// Scope Bleed
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(582, 2), false);
		// Assimilation
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(583, 2), false, false);
		// Transfrom Dispel
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(619, 1), false);
		// Decrease Bow/Crossbow Attack Speed
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(5491, 1), false);
	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new DragonBomberWeak());
	}
}
