package luna.custom.ranking;

public class Rank
{
//	CHALLENGER("Challenger", "L2VeronicaIcons.Challenger_Emblem", 1),
//	GRAND_MASTER("Grand Master", "L2VeronicaIcons.Grandmaster_Emblem", 2),
//	MASTER("Master", "L2VeronicaIcons.Master_Emblem", 2),
//	DIAMOND("Diamond", "L2VeronicaIcons.Diamond_Emblem", 5),
//	PLATINUM("Platinum", "L2VeronicaIcons.Platinum_Emblem", 10),
//	GOLD("Gold", "L2VeronicaIcons.Gold_Emblem", 20),
//	SILVER("Silver", "L2VeronicaIcons.Silver_Emblem", 20),
//	BRONZE("Bronze", "L2VeronicaIcons.Bronze_Emblem", 20),
//	IRON("Iron", "L2VeronicaIcons.Iron_Emblem", 20),
//	UNRANKED("Unranked", "L2VeronicaIcons.Unranked_Emblem", 0);
	
	private final String _name;
	private final String _colr;
	private final String _icon;
	private final    int _pool;
	
	public Rank(String name, String colr, String icon, int pool)
	{
		_name = name;
		_colr = colr;
		_icon = icon;
		_pool = pool;
	}

	public String getName()
	{
		return _name;
	}
	
	public String getColor()
	{
		return _colr;
	}
	
	public String getIconSmall()
	{
		return _icon + "_s";
	}
	
	public String getIconBig()
	{
		return _icon + "_l";
	}
	
	public int getPool()
	{
		return _pool;
	}
}