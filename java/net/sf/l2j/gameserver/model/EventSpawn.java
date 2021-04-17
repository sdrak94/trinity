package net.sf.l2j.gameserver.model;


public class EventSpawn 
{
	public enum SpawnType
	{
		RegularSpawn,
		//RegularSpawn1,
		//Flag,
		Door,
		//DefendingSpawn,
		//CommandersSpawn,
		//TeamSpawnGuardType1,
		//TeamSpawnGuardType2,
	}
	
	private Location _loc;
	private int _spawnId;
	
	// = 0 if no team
	private int _teamId;
	private int _mapId;
	private SpawnType _type;
	
	private String _note = null;
	
	public EventSpawn(int mapId, int spawnId, Location loc, int teamId, String type)
	{
		_loc = loc;
		_spawnId = spawnId;
		_teamId = teamId;
		_mapId = mapId;
		_type = assignSpawnType(type);
	}
	
	public static SpawnType assignSpawnType(String typeString)
	{
		for(SpawnType st : SpawnType.values())
		{
			if(st.toString().equals(typeString))
				return st;
		}
		return SpawnType.RegularSpawn;
	}
	
	public SpawnType getSpawnType()
	{
		return _type;
	}
	
	public Location getLoc()
	{
		return _loc;
	}

	public int getMapId()
	{
		return _mapId;
	}
	
	public int getSpawnTeam()
	{
		return _teamId;
	}
	
	public int getSpawnId()
	{
		return _spawnId;
	}
	
	public void setNote(String note)
	{
		_note = note;
	}
	
	public String getNote()
	{
		return _note;
	}
}
