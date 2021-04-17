package luna.custom.ranking;
public enum Rank 
{
	CHALLENGER("ead054",	"L2VeronicaIcons.Unranked_Emblem_s",		"",	"Challenger"), //challenger
	GRANDMASTER("f8161f",	"L2VeronicaIcons.Grandmaster_Emblem_s",		"",	"Grand Master"), //Grandmaster
	MASTER("984cc5",      	"L2VeronicaIcons.Master_Emblem_s",			"",	"Master"), //Master
	DIAMOND("7270da",		"L2VeronicaIcons.Diamond_Emblem_s",			"",	"Diamond"), //Diamon
	PLATINUM("3f8188",		"L2VeronicaIcons.Platinum_Emblem_s",		"",	"Platinum"), //Platinum
	GOLD("dc904e",			"L2VeronicaIcons.Gold_Emblem_s",			"",	"Gold"), //Gold
	SILVER("a6b9c0",		"L2VeronicaIcons.Silver_Emblem_s",			"",	"Silver"), //Silver
	BRONZE("944b2e",		"L2VeronicaIcons.Bronze_Emblem_s",			"",	"Bronze"), //Bronze
	IRON("695f5a",			"L2VeronicaIcons.Iron_Emblem_s",			"",	"Iron"), //Iron
	UNRANKED("427879",		"L2VeronicaIcons.Unranked_Emblem_s",		"",	"Unranked"); //Unranked
	
	public String _color;
	public String _icon;
	public String _iconl;
	public String _bgcolor;
	public String _dispname;
	
	private Rank(String color, String icon, String bgcolor, String dispname)
	{
		_color = color;
		_icon = icon;
		_iconl = icon.replace("_s", "_l");
		_bgcolor = bgcolor;
		_dispname = dispname;
	}
	
	@Override
	public String toString()
	{
		return _dispname;
	}
};