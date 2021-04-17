package cz.nxs.events.engine.mini.features;

import java.util.Arrays;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.mini.EventMode;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.SkillData;

/**
 * @author hNoke
 *
 */
public class SkillsFeature extends AbstractFeature
{
	private boolean disableSkills = false;
	private boolean allowResSkills = false;
	private boolean allowHealSkills = false;
	
	private int[] disabledSkills = null;
	
	public SkillsFeature(EventType event, PlayerEventInfo gm, String parametersString)
	{
		super(event);
		
		addConfig("DisableSkills", "If 'true', then all skills will be disabled for this mode. Put 'false' to enable them.", 1);
		addConfig("AllowResurrections", "Put 'false' to disable all resurrection-type skills. Put 'true' to enable them.", 1);
		addConfig("AllowHeals", "Put 'false' to disable all heal-type skills. Put 'true' to enable them. This config doesn't affect self-heals.", 1);
		
		addConfig("DisabledSkills", "Specify here which skills will be disabled for this mode. Write their IDs and separate by SPACE. Eg. <font color=LEVEL>50 150 556</font>. Put <font color=LEVEL>0</font> to disable this config.", 2);
		
		if(parametersString == null)
			parametersString = "false,false,true,0";
		
		_params = parametersString;
		initValues();
	}
	
	@Override
	protected void initValues()
	{
		String[] params = splitParams(_params);
		
		try
		{
			disableSkills = Boolean.parseBoolean(params[0]);
			allowResSkills = Boolean.parseBoolean(params[1]);
			allowHealSkills = Boolean.parseBoolean(params[2]);
			
			String splitted[] = params[3].split(" ");
			disabledSkills = new int[splitted.length];
			
			for(int i = 0; i < splitted.length; i++)
			{
				disabledSkills[i] = Integer.parseInt(splitted[i]);
			}
			
			Arrays.sort(disabledSkills);
		} 
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	public boolean checkSkill(PlayerEventInfo player, SkillData skill)
	{
		if(disableSkills)
			return false;
		
		if(skill.isResSkill())
		{
			if(!allowResSkills)
				return false;
		}
		
		if(skill.isHealSkill())
		{
			if(!allowHealSkills)
				return false;
		}
		
		if (Arrays.binarySearch(disabledSkills, skill.getId()) >= 0)
			return false;
		
		return true;
	}
	
	@Override
	public boolean checkPlayer(PlayerEventInfo player)
	{
		return true;
	}
	
	@Override
	public FeatureType getType()
	{
		return EventMode.FeatureType.Skills;
	}
}
