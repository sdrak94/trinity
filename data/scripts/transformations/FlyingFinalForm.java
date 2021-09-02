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
public class FlyingFinalForm extends L2Transformation
{
	public FlyingFinalForm()
	{
		// id, duration (secs), colRadius, colHeight
		super(260, -1, 9.0, 38.0);
	}

	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 260 || getPlayer().isCursedWeaponEquipped())
			return;
		getPlayer().setIsFlyingMounted(true);
		// 	give transformation skills
		transformedSkills();
	}

	public void transformedSkills()
	{
		// Transfrom Dispel
		getPlayer().addSkill(SkillTable.getInstance().getInfo(619, 1), false);
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
	}

	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new FlyingFinalForm());
	}
}