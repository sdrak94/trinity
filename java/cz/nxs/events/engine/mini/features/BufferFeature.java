package cz.nxs.events.engine.mini.features;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.mini.EventMode;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 *
 */
public class BufferFeature extends AbstractFeature
{
	private boolean autoApplySchemeBuffs = true;
	private boolean spawnNpcBuffer = false;
	private int customNpcBuffer = 0;
	
	private int[] autoBuffsFighterIds = null;
	private int[] autoBuffsFighterLevels = null;
	
	private int[] autoBuffsMageIds = null;
	private int[] autoBuffsMageLevels = null;
	
	public BufferFeature(EventType event, PlayerEventInfo gm, String parametersString)
	{
		super(event);
		
		addConfig("ApplyEventBuffs", "If 'true', all players will be rebuffed on start of event/round by their specified scheme. Doesn't work if the auto scheme buffer is disabled from Events.xml.", 1);
		addConfig("SpawnNpcBuffer", "If 'true', then the event will spawn NPC Buffer to each spawn of type Buffer at start of the event/round and the Buffer disappears at the end of wait time.", 1);
		addConfig("CustomBufferId", "You can specify the ID of buffer (or another NPC which will be aviable near players during the wait-time) for this mode. Put '0' to disable.", 1);
		
		addConfig("AutoBuffIdsFighter", "Fighter classes will be buffed with those buffs at start of event/round. Format as 'BUFF_ID-Level'. Separate IDs by SPACE, Eg. <font color=LEVEL>312-1 256-3</font>. Put <font color=LEVEL>0-0</font> to disable this config.", 2);
		addConfig("AutoBuffIdsMage", "Mage classes will be buffed with those buffs at start of event/round.  Format as 'BUFF_ID-Level'. Separate IDs by SPACE, Eg. <font color=LEVEL>312-1 256-3</font>. Put <font color=LEVEL>0-0</font> to disable this config.", 2);
		
		if(parametersString == null)
			parametersString = "true,true,0,0-0,0-0";
		
		_params = parametersString;
		initValues();
	}
	
	@Override
	protected void initValues()
	{
		String[] params = splitParams(_params);
		
		try
		{
			autoApplySchemeBuffs = Boolean.parseBoolean(params[0]);
			spawnNpcBuffer = Boolean.parseBoolean(params[1]);
			customNpcBuffer = Integer.parseInt(params[2]);
			
			String splitted[] = params[3].split(" ");
			autoBuffsFighterIds = new int[splitted.length];
			autoBuffsFighterLevels = new int[splitted.length];
			
			int i = 0;
			for(String s : splitted)
			{
				autoBuffsFighterIds[i] = Integer.parseInt(s.split("-")[0]);
				autoBuffsFighterLevels[i] = Integer.parseInt(s.split("-")[1]);
				i++;
			}
			
			splitted = params[4].split(" ");
			autoBuffsMageIds = new int[splitted.length];
			autoBuffsMageLevels = new int[splitted.length];
			
			i = 0;
			for(String s : splitted)
			{
				autoBuffsMageIds[i] = Integer.parseInt(s.split("-")[0]);
				autoBuffsMageLevels[i] = Integer.parseInt(s.split("-")[1]);
				i++;
			}
		} 
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	public void buffPlayer(PlayerEventInfo player)
	{
		if(player.isMageClass())
		{
			if(autoBuffsMageIds[0] == 0)
				return;
			
			for(int i = 0; i < autoBuffsMageIds.length; i++)
			{
				player.getSkillEffects(autoBuffsMageIds[i], autoBuffsMageLevels[i]);
			}
		}
		else
		{
			if(autoBuffsFighterIds[0] == 0)
				return;
			
			for(int i = 0; i < autoBuffsFighterIds.length; i++)
			{
				player.getSkillEffects(autoBuffsFighterIds[i], autoBuffsFighterLevels[i]);
			}
		}
	}
	
	public boolean canRebuff()
	{
		return autoApplySchemeBuffs;
	}
	
	public boolean canSpawnBuffer()
	{
		return spawnNpcBuffer;
	}
	
	public int getCustomNpcBufferId()
	{
		return customNpcBuffer;
	}
	
	@Override
	public boolean checkPlayer(PlayerEventInfo player)
	{
		// nothing to check
		return true;
	}
	
	@Override
	public FeatureType getType()
	{
		return EventMode.FeatureType.Buffer;
	}
}
