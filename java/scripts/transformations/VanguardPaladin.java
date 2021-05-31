package scripts.transformations;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Transformation;

public class VanguardPaladin extends L2Transformation
{
	public VanguardPaladin()
	{
		// id, duration (secs), colRadius, colHeight
		super(312);
	}
	
	public void onTransform()
	{
		if (getPlayer().getTransformationId() != 312 || getPlayer().isCursedWeaponEquipped())
			return;

		// give transformation skills
		transformedSkills();
	}
	
	public void transformedSkills()
	{
		// Power Divide
		getPlayer().addSkill(SkillTable.getInstance().getInfo(816, 42), false);
		// Full Swing
		getPlayer().addSkill(SkillTable.getInstance().getInfo(814, 42), false);

		// Shoulder Charge
		getPlayer().addSkill(SkillTable.getInstance().getInfo(494, 37), false);
		// Armor Crush
		getPlayer().addSkill(SkillTable.getInstance().getInfo(362, 1), false);
		// Shock Blast
		getPlayer().addSkill(SkillTable.getInstance().getInfo(361, 1), false);
		// War cry
		getPlayer().addSkill(SkillTable.getInstance().getInfo(78, 2), false);

		// Decrease Bow/Crossbow Attack Speed
		getPlayer().addSkill(SkillTable.getInstance().getInfo(5491, 1), false); 
		// Switch Stance
		getPlayer().addSkill(SkillTable.getInstance().getInfo(838, 1), false);
	}
	
	public void onUntransform()
	{
		// remove transformation skills
		removeSkills();
	}
	
	public void removeSkills()
	{
		// Power Divide
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(816, 42), false);
		// Full Swing
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(814, 42), false);

		// Decrease Bow/Crossbow Attack Speed
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(5491, 1), false); 
		// Switch Stance
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(838, 1), false);
				// Shoulder Charge
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(494, 37), false);
		// Armor Crush
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(362, 1), false);
		// Shock Blast
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(361, 1), false);
		// War cry
		getPlayer().removeSkill(SkillTable.getInstance().getInfo(78, 2), false, true);
		
		if (getPlayer().isSWS())
			getPlayer().addSkill(SkillTable.getInstance().getInfo(78, 1), false);
	}
	
	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new VanguardPaladin());
	}
}