package cz.nxs.events.engine.mini.features;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.mini.EventMode;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.ItemData;

/**
 * @author hNoke
 *
 */
public class EnchantFeature extends AbstractFeature
{
	private int maxEnchantWeapon = 0;
	private int maxEnchantArmor = 0;
	private int maxEnchantJewel = 0;
	
	private int minEnchantWeapon = 0;
	private int minEnchantArmor = 0;
	private int minEnchantJewel = 0;
	
	private int autosetEnchantWeapon = -1;
	private int autosetEnchantArmor = -1;
	private int autosetEnchantJewel = -1;
	
	public EnchantFeature(EventType event, PlayerEventInfo gm, String parametersString)
	{
		super(event);
		
		addConfig("MaxEnchantWeapon", "The max enchant limi for weapons. If the item has bigger enchant level, then the player will not be able to participate till he puts the item to his WH. -1 to disable this config, 0 to disallow enchants.", 1);
		addConfig("MaxEnchantArmor", "The max enchant limit for armors. If the item has bigger enchant level, then the player will not be able to participate till he puts the item to his WH. -1 to disable this config, 0 to disallow enchants.", 1);
		addConfig("MaxEnchantJewel", "The max enchant limit for jewels. If the item has bigger enchant level, then the player will not be able to participate till he puts the item to his WH. -1 to disable this config, 0 to disallow enchants.", 1);
		
		addConfig("MinEnchantWeapon", "The min enchant weapon must have otherwise it will be unusable during the event. 0 to disable this config.", 1);
		addConfig("MinEnchantArmor", "The min enchant armor must have otherwise it will be unusable during the event. 0 to disable this config.", 1);
		addConfig("MinEnchantJewel", "The min enchant jewel must have otherwise it will be unusable during the event. 0 to disable this config.", 1);
		
		addConfig("AutoEnchantWeap", "All weapons, if their enchant is higher than this value, will be lowered to this value. -1 to disable this config.", 1);
		addConfig("AutoEnchantArmor", "All armors, if their enchant is higher than this value, will be lowered to this value. -1 to disable this config.", 1);
		addConfig("AutoEnchantJewel", "All jewels, if their enchant is higher than this value, will be lowered to this value. -1 to disable this config.", 1);
		
		if(parametersString == null)
			parametersString = "-1,-1,-1,0,0,0,-1,-1,-1";
		
		_params = parametersString;
		initValues();
	}
	
	@Override
	protected void initValues()
	{
		String[] params = splitParams(_params);
		
		try
		{
			maxEnchantWeapon = Integer.parseInt(params[0]);
			maxEnchantArmor = Integer.parseInt(params[1]);
			maxEnchantJewel = Integer.parseInt(params[2]);
			
			minEnchantWeapon = Integer.parseInt(params[3]);
			minEnchantArmor = Integer.parseInt(params[4]);
			minEnchantJewel = Integer.parseInt(params[5]);
			
			autosetEnchantWeapon = Integer.parseInt(params[6]);
			autosetEnchantArmor = Integer.parseInt(params[7]);
			autosetEnchantJewel = Integer.parseInt(params[8]);
		} 
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	public int getMaxEnchantWeapon()
	{
		return maxEnchantWeapon;
	}
	
	public int getMaxEnchantArmor()
	{
		return maxEnchantArmor;
	}
	
	public int getMaxEnchantJewel()
	{
		return maxEnchantJewel;
	}
	
	public int getMinEnchantWeapon()
	{
		return minEnchantWeapon;
	}
	
	public int getMinEnchantArmor()
	{
		return minEnchantArmor;
	}
	
	public int getMinEnchantJewel()
	{
		return minEnchantJewel;
	}
	
	public int getAutoEnchantWeapon()
	{
		return autosetEnchantWeapon;
	}
	
	public int getAutoEnchantArmor()
	{
		return autosetEnchantArmor;
	}
	
	public int getAutoEnchantJewel()
	{
		return autosetEnchantJewel;
	}
	
	public boolean checkItem(PlayerEventInfo player, ItemData item)
	{
		if(item.isType2Accessory())
		{
			if(maxEnchantJewel > -1 && item.getEnchantLevel() > maxEnchantJewel)
				return false;
			
			if(minEnchantJewel > item.getEnchantLevel())
				return false;
		}
		else if(item.isType2Armor())
		{
			if(maxEnchantArmor > -1 && item.getEnchantLevel() > maxEnchantArmor)
				return false;
			
			if(minEnchantArmor > item.getEnchantLevel())
				return false;
		}
		else if(item.isType2Weapon())
		{
			if(maxEnchantWeapon > -1 && item.getEnchantLevel() > maxEnchantWeapon)
				return false;
			
			if(minEnchantWeapon > item.getEnchantLevel())
				return false;
		}
		
		return true;
	}
	
	@Override
	public boolean checkPlayer(PlayerEventInfo player)
	{
		boolean canJoin = true;
		for(ItemData item : player.getItems())
		{
			if(!checkItem(player, item))
			{
				canJoin = false;
				player.sendMessage("Please put item " + item.getItemName() + " to your warehouse before participating. It is not allowed for this event.");
			}
		}
		
		return canJoin;
	}
	
	@Override
	public FeatureType getType()
	{
		return EventMode.FeatureType.Enchant;
	}
}
