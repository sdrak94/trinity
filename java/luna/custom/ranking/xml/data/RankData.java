package luna.custom.ranking.xml.data;

public class RankData
{
	private final String _name;
	private final int _pool;
	private final String _color;
	private final String _icon;
	
	public RankData (String name, int pool, String color, String icon)
	{
		_name = name;
		_pool = pool;
		_color = color;
		_icon = icon;
	}
	public String getName()
	{
		return _name;
	}
	public int getPool()
	{
		return _pool;
	}
	public String getColor()
	{
		return _color;
	}
	public String getIcon()
	{
		return _icon + "_s";
	}

	public String getIconLarge()
	{
		return _icon + "_l";
	}
	
	@Override
	public String toString()
	{
		return _name;
	}
}