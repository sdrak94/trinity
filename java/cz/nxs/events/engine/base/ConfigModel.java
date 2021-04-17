package cz.nxs.events.engine.base;

import cz.nxs.events.NexusLoader;
import javolution.text.TextBuilder;

/**
 * @author hNoke
 * - provides comfortable configurations based on more kinds of input 
 */
public class ConfigModel
{
	String category;
	String key;
	String value;
	String desc;
	String defaultVal;
	
	public enum InputType
	{
		TextEdit,
		MultiEdit,
		Boolean,
		Enum,
		MultiAdd
	}
	
	InputType input;
	TextBuilder inputParams;
	
	public ConfigModel(String key, String value, String description)
	{
		this(key, value, description, value);
	}
	
	public ConfigModel(String key, String value, String description, String defaultVal)
	{
		this(key, value, description, defaultVal, InputType.TextEdit);
	}
	
	public ConfigModel(String key, String value, String description, InputType input)
	{
		this(key, value, description, value, input);
	}
	
	public ConfigModel(String key, String value, String description, String defaultVal, InputType input)
	{
		this(key, value, description, value, input, "");
	}
	
	public ConfigModel(String key, String value, String description, String defaultVal, InputType input, String inputParams)
	{
		this.key = key;
		this.value = value;
		this.desc = description;
		this.defaultVal = defaultVal;
		this.input = input;
		this.inputParams = new TextBuilder();
		
		if(this.input == InputType.Boolean)
			addEnumOptions(new String[]{ "True", "False" });
		else
			this.inputParams.append(inputParams);
		
		category = "General";
	}
	
	/** key:'value'; */
	public String encode()
	{
		return new TextBuilder().append(key + ":" + value + ";").toString();
	}
	
	public ConfigModel addEnumOptions(String[] options)
	{
		if(input == InputType.Enum || input == InputType.Boolean)
		{
			int i = 1;
			for(String s : options)
			{
				inputParams.append(s);
				if(i != options.length)
					inputParams.append(";");
				
				i++;
			}
			return this;
		}
		
		NexusLoader.debug("can't add enum options to a non enum config model. (config key = " + key + ")");
		return this;
	}
	
	public ConfigModel setCategory(String cat)
	{
		category = cat;
		return this;
	}
	
	//TODO check if the value is right - add params for only numbers, certain symbols, etc.
	public void setValue(String value)
	{
		//TODO: disallow setting value w/ ":"
		this.value = value;
	}
	
	public void addToValue(String value)
	{
		if(input == InputType.MultiAdd)
		{
			if(this.value.length() > 0)
				this.value += ("," + value);
			else
				this.value = value;
		}
		else
			NexusLoader.debug("can't add MultiAdd options to a non MultiAdd config model. (config key = " + key + ")");
	}
	
	public String getKey()
	{
		return key;
	}
	
	public String getValue()
	{
		return value;
	}
	
	public String getCategory()
	{
		return category;
	}
	
	public String removeMultiAddValueIndex(int index)
	{
		if(input == InputType.MultiAdd)
		{
			String[] values = this.value.split(",");
			for(int i = 0; i < values.length; i++)
			{
				if(i == index)
				{
					values[i] = "";
					break;
				}
			}
			
			String newValue = "";
			for(String v : values)
			{
				if(v.length() > 0)
					newValue += v + ",";
			}
			
			if(newValue.length() > 0)
				this.value = newValue.substring(0, newValue.length() - 1);
			else
				this.value = newValue;
			
			return this.value;
		}
		else
		{
			NexusLoader.debug("can't remove multiadd value by index from a non-MultiAdd config model. (config key = " + key + ")");
			return this.value;
		}
	}
	
	public String getValueShownInHtml()
	{
		switch(input)
		{
			case MultiAdd:
			{
				try
				{
					String[] values = this.value.split(",");
					String toReturn = "";
					toReturn += "<td width=240>";
					for(int i = 0; i < values.length; i++)
					{
						toReturn += "<font color=ac9887><a action=\"bypass -h admin_event_manage remove_multiadd_config_value " + i + "\">" + values[i] + "</a></font>";
						
						if(i+1 < values.length)
							toReturn += " , ";
					}
					toReturn += "</td>";
					
					return toReturn;
				}
				catch (Exception e)
				{
					return "<font color=4f4f4f>No values</font>";
				}
			}
			default:
				return "<td width=240><font color=ac9887>" + value + "</font></td>";
		}
	}
	
	public String getDesc()
	{
		return desc;
	}
	
	public String[] getValues()
	{
		if(input == InputType.MultiEdit)
		{
			return value.split(",");
		}
		else
		{
			NexusLoader.debug("can't call getValues() method for a non-MultiAdd config model. (config key = " + key + ")");
			return new String[]{value};
		}
	}
	
	public String getDefaultVal()
	{
		return defaultVal;
	}
	
	public int getValueInt()
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch (Exception e)
		{
			return -1;
		}
	}
	
	public boolean getValueBoolean()
	{
		try
		{
			return Boolean.parseBoolean(value);
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	public String getInputHtml(int width)
	{
		return getInputHtml(width, 0);
	}
	
	public String getInputHtml(int width, int height)
	{
		switch(input)
		{
			case Enum:
			case Boolean:
			{
				return "<combobox width=" + width + " height=" + (height == 0 ? 17 : height) + " var=" + getKey() + " list=" + inputParams + ">";
			}
			case MultiEdit:
			{
				if(height > 0)
					return "<multiedit var=" + getKey() + " width=" + width + " height=" + height + ">";
				else 
					return "<multiedit var=" + getKey() + " width=" + width + ">";
			}
			case TextEdit:
			{
				if(height > 0)
					return "<edit var=" + getKey() + " width=" + width + " height=" + height + ">";
				else 
					return "<edit var=" + getKey() + " width=" + width + ">";
			}
			case MultiAdd:
			{
				if(height > 0)
					return "<multiedit var=" + getKey() + " width=" + width + " height=" + height + ">";
				else 
					return "<multiedit var=" + getKey() + " width=" + width + " height=15>";
			}
		}
		
		return "Input not available";
	}
	
	public String getAddButtonName()
	{
		switch(input)
		{
			case MultiAdd:
				return "Add";
			default:
				return "Set";
		}
	}
	
	public String getUtilButtonName()
	{
		switch(input)
		{
			case MultiAdd:
				return "Remove all";
			default:
				return "Reset value to default";
		}
	}
	
	public int getUtilButtonWidth()
	{
		switch(input)
		{
			case MultiAdd:
				return 80;
			default:
				return 150;
		}
	}
	
	public String getConfigHtmlNote()
	{
		switch(input)
		{
			case MultiAdd:
				return "(click on a value to remove it)";
			default:
				return "Default: " + getDefaultVal();
		}
	}
	
	public String getAddButtonAction()
	{
		switch(input)
		{
			case MultiAdd:
				return "addto";
			default:
				return "set";
		}
	}
	
	public InputType getInput()
	{
		return input;
	}
	
	public String getInputParams()
	{
		return inputParams.toString();
	}
}
