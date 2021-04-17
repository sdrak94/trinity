package net.sf.l2j.gameserver.model.events.dataTables;

public class TeamSpawnTemplate
{
	private final String _id;
	private final int[] _spawn;

	public TeamSpawnTemplate(String id, int[] spawn)
	{
		_id = id;
		_spawn = spawn;
	}
	public String getId()
	{
		return _id;
	}
	public int getSpawnX()
	{
		return _spawn[0];
	}

	public int getSpawnY()
	{
		return _spawn[1];
	}

	public int getSpawnZ()
	{
		return _spawn[2];
	}
}
