package cz.nxs.events.engine.base;

/**
 * @author hNoke
 * represents a GlobalConfig
 * (will be replaced by ConfigModel later)
 */
public class GlobalConfigModel
{
	private String category;
	private String key;
	private String value;
	private String description;
	private int inputType;
	
	public GlobalConfigModel(String category, String key, String value, String desc, int input)
	{
		this.category = category;
		this.key = key;
		this.value = value;
		this.description = desc;
		this.inputType = input;
	}
	
	public String getCategory()
	{
		return category;
	}
	
	public String getKey()
	{
		return key;
	}
	
	public String getValue()
	{
		return value;
	}
	
	public void setValue(String value)
	{
		this.value = value;
	}
	
	public String getDesc()
	{
		return description;
	}
	
	public int getInputType()
	{
		return inputType;
	}
}