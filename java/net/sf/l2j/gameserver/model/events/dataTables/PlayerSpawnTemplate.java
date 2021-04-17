package net.sf.l2j.gameserver.model.events.dataTables;

public class PlayerSpawnTemplate
{
	private final int[]	_spawn;
	private final int	_range;
	
	public PlayerSpawnTemplate(int[] spawn, int range)
	{
		_spawn = spawn;
		_range = range;
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
	
	public int getRange()
	{
		return _range;
	}
}