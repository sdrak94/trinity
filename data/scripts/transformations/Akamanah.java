package transformations;

import net.sf.l2j.gameserver.model.L2Transformation;


import net.sf.l2j.gameserver.instancemanager.TransformationManager;

/**
 * This is currently only a test of the java script engine
 * 
 * @author durgus
 */
public class Akamanah extends L2Transformation
{
	public Akamanah()
	{
		super(302, Integer.MAX_VALUE, 10.0, 32.73);
		addSkills();
	}
	
	public void onTransform()
	{
		// Set charachter name to transformed name
		getPlayer().getAppearance().setVisibleName("Akamanah");
		getPlayer().getAppearance().setVisibleTitle("Blood Sword");
	}
	
	public void onUntransform()
	{
		// set character back to true name.
		getPlayer().getAppearance().setVisibleName(null);
		getPlayer().getAppearance().setVisibleTitle(null);
	}
	
	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new Akamanah());
	}
	
	public void addSkills()
	{
		addAllowedSkills(16);
	}
}
