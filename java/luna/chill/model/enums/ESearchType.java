package luna.chill.model.enums;

public enum ESearchType
{
	Off("FF6363", -1),
	Assist("LEVEL", 0),
	Close("63FF63", 11400),
	Near("63FF63", 11000),
	Far("63FF63", 15800);

	private final String _color;

	private final int _range;
	
	private ESearchType(final String color, final int range)
	{
		_color = color;
		_range = range;
	}
	
	public String getColor()
	{
		return _color;
	}
	
	public int getRange()
	{
		return _range;
	}
}
