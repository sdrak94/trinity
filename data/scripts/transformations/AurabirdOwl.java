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
 * @author Kerberos
 *
 */
public class AurabirdOwl extends L2Transformation
{
	public AurabirdOwl()
	{
		// id, duration (secs), colRadius, colHeight
		super(9, -1, 40.0, 19.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 9 || getPlayer().isCursedWeaponEquipped())
			return;
		getPlayer().setIsFlyingMounted(true);
		// 	give transformation skills
		transformedSkills();
	}

	public void transformedSkills()
	{
		// Transfrom Dispel
		getPlayer().addSkill(SkillTable.getInstance().getInfo(619, 1), false);
		
		getPlayer().addSkill(SkillTable.getInstance().getInfo(885, 1), false);
		getPlayer().addSkill(SkillTable.getInstance().getInfo(884, 11), false);
		getPlayer().addSkill(SkillTable.getInstance().getInfo(886, 11), false);
		getPlayer().addSkill(SkillTable.getInstance().getInfo(888, 11), false);
		getPlayer().addSkill(SkillTable.getInstance().getInfo(891, 11), false);
		getPlayer().addSkill(SkillTable.getInstance().getInfo(911, 11), false);
	}

	public void onUntransform()
	{
		getPlayer().setIsFlyingMounted(false);
		// remove transformation skills
		removeSkills();
	}

	public void removeSkills()
	{
		// Transfrom Dispel
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(619, 1), false);
		
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(885, 1), false);
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(884, 11), false);
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(886, 11), false);
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(888, 11), false);
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(891, 11), false);
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(911, 11), false);
	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new AurabirdOwl());
	}
}