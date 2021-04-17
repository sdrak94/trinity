package net.sf.l2j.gameserver.model.events.dataTables;

public class TeamTemplate
{
	private final int _id;
	private final String _name;
	private final String _nColor;
	private final String _tColor;

	public TeamTemplate(int id, String name, String nColor, String tColor)
	{
		_id = id;
		_name = name;
		_nColor = nColor;
		_tColor = tColor;
	}
	public int getId()
	{
		return _id;
	}
	public String getName()
	{
		return _name;
	}

	public String getNameColor()
	{
		return _nColor;
	}
	public String getTitleColor()
	{
		return _tColor;
	}
}
