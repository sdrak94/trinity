package net.sf.l2j.gameserver.model.events.dataTables;

import net.sf.l2j.gameserver.model.Location;

public class NpcSpawnTemplate
{
	private final int _id;
	Location _spawn;
	String _title;

	public NpcSpawnTemplate(int id, Location spawn, String title)
	{
		_id = id;
		_spawn = spawn;
		_title = title;
	}
	public int getId()
	{
		return _id;
	}

	public Location getLoc()
	{
		return _spawn;
	}
	
	public String getTitle()
	{
		return _title;
	}
}