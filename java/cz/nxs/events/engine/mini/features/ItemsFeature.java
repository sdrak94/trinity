package cz.nxs.events.engine.mini.features;

import java.util.Arrays;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.mini.EventMode;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.ItemData;

/**
 * @author hNoke
 *
 */
public class ItemsFeature extends AbstractFeature
{
	private boolean allowPotions = true;
	private boolean allowScrolls = true;
	
	private int[] disabledItems = null;
	private String[] enabledTiers = null;
	private int maxGearScore = -1;
	
	public ItemsFeature(EventType event, PlayerEventInfo gm, String parametersString)
	{
		super(event);
		
		addConfig("AllowPotions", "Will the potions be enabled for this mode?", 1);
		addConfig("AllowScrolls", "Will the scrolls be enabled for this mode?", 1);
		
		addConfig("DisabledItems", "Specify here which items will be disabled (not usable/equipable) for this mode. Write their IDs and separate by SPACE. Eg. <font color=LEVEL>111 222 525</font>. Put <font color=LEVEL>0</font> to disable this config.", 2);
		
		addConfig("EnabledTiers", "This config is not fully implemented. Requires gameserver support.", 2);
		
		addConfig("MaxGearScore", "Max Gear score for this event.", 2);
		
		if(parametersString == null || parametersString.split(",").length != 3)
			parametersString = "true,false,0,Allitems,-1";
		
		_params = parametersString;
		initValues();
	}
	
	@Override
	protected void initValues()
	{
		String[] params = splitParams(_params);
		
		try
		{
			allowPotions = Boolean.parseBoolean(params[0]);
			allowScrolls = Boolean.parseBoolean(params[1]);
			
			
			
			String splitted[] = params[2].split(" ");
			disabledItems = new int[splitted.length];
			
			for(int i = 0; i < splitted.length; i++)
			{
				disabledItems[i] = Integer.parseInt(splitted[i]);
			}
			
			Arrays.sort(disabledItems);
			
			
			String splitted2[] = params[3].split(" ");
			enabledTiers = new String[splitted2.length];
			for(int i = 0; i < splitted2.length; i++)
			{
				if(splitted2[i].length() > 0)
				{
					enabledTiers[i] = splitted2[i];
				}
			}
			
			Arrays.sort(enabledTiers);
			
			maxGearScore = Integer.parseInt(params[4]);
		} 
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return;
		}
	}
	 
	public boolean checkItem(PlayerEventInfo player, ItemData item)
	{
		if (!allowPotions && item.isPotion())
			return false;
		
		if (!allowScrolls && item.isScroll())
			return false;
		
		if (Arrays.binarySearch(disabledItems, item.getItemId()) >= 0)
			return false;
		
		if(!checkIfAllowed(item))
			return false;
		
		return true;
	}
	
	private boolean checkIfAllowed(ItemData item)
	{
		/*for(String tier : enabledTiers)
		{
			if(tier.equalsIgnoreCase("AllItems"))
			{
				return true;
			}
			//else if()
			else
				return false;
		}*/
		
		return true;
	}
	
	public int getMaxGearScore()
	{
		return maxGearScore;
	}
	
	@Override
	public boolean checkPlayer(PlayerEventInfo player)
	{
		boolean canJoin = true;
		
		if(maxGearScore <= 0)
			return true;
		
		int gearScore = player.getGearScore();
		
		if(gearScore >= maxGearScore)
		{
			player.screenMessage("Your Gear score is too high.", "Event", false);
			player.sendMessage("The max Gear score for this event is " + maxGearScore + ", you have " + gearScore + ". Check out Brain of Nexus -> Stats (click on Gear strength index) for more info.");
			canJoin = false;
		}
		
		return canJoin;
	}
	
	@Override
	public FeatureType getType()
	{
		return EventMode.FeatureType.Items;
	}
}
