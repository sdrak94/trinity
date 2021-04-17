package cz.nxs.events.engine.base;

import java.util.StringTokenizer;

import cz.nxs.events.engine.EventManager;

/**
 * @author hNoke
 * stores data about one spawn of an EventMap
 */
public class EventSpawn 
{
	private Loc _loc;
	private int _spawnId;
	
	private int _teamId;
	private int _mapId;
	private SpawnType _type;
	
	private int _fenceWidth;
	private int _fenceLength;
	
	private String _note = null;
	
	private boolean _saved;
	
	public EventSpawn(int mapId, int spawnId, Loc loc, int teamId, String type)
	{
		_loc = loc;
		_spawnId = spawnId;
		_teamId = teamId;
		_mapId = mapId;
		_type = assignSpawnType(type);
	}
	
	private static SpawnType assignSpawnType(String typeString)
	{
		for(SpawnType st : SpawnType.values())
		{
			if(st.toString().equalsIgnoreCase(typeString))
				return st;
		}

		return SpawnType.Regular;
	}
	
	public SpawnType getSpawnType()
	{
		return _type;
	}
	
	public int getDoorId()
	{
		if(_type == SpawnType.Door)
			return _loc.getX();
		else
			return -1;
	}
	
	public int getNpcId()
	{
		try
		{
			if(_type == SpawnType.Npc)
				return Integer.parseInt(_note);
			else
				return -1;
		}
		catch (Exception e)
		{
			return -1;
		}
	}
	
	public Loc getLoc()
	{
		return new Loc(_loc.getX(), _loc.getY(), _loc.getZ(), _loc.getHeading());
	}

	public int getMapId()
	{
		return _mapId;
	}
	
	public void setType(String s)
	{
		_type = assignSpawnType(s);
		_saved = false;
	}
	
	/** returns 0 if neutral team */
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
		
		if(_type == SpawnType.Fence)
		{
			try
			{
				StringTokenizer st = new StringTokenizer(note, " ");
				
				_fenceWidth = Integer.parseInt(st.nextToken());
				_fenceLength = Integer.parseInt(st.nextToken());
			} 
			catch (Exception e)
			{
				EventManager.getInstance().debug("The value for fence's length / weight can be only a number! Reseting back to default values.");
				_fenceWidth = 100;
				_fenceLength = 100;
			}
		}
		
		_saved = false;
	}
	
	public void setId(int i)
	{
		_spawnId = i;
		_saved = false;
	}
	
	public void setTeamId(int i)
	{
		_teamId = i;
		_saved = false;
	}
	
	public void setX(int i)
	{
		_loc = new Loc(i, _loc.getY(), _loc.getZ());
		
		_saved = false;
	}
	
	public void setY(int i)
	{
		_loc = new Loc(_loc.getX(),i, _loc.getZ());
		
		_saved = false;
	}
	
	public void setZ(int i)
	{
		_loc = new Loc(_loc.getX(), _loc.getY(), i);
		
		_saved = false;
	}
	
	public int getImportance()
	{
		String note = getNote();
		
		try
		{
			return Integer.parseInt(note.split("-")[0]);
		}
		catch (Exception e)
		{
			setNote("1-false");
			return getImportance();
		}
	}
	
	public boolean canRespawnHere()
	{
		String note = getNote();
		
		try
		{
			return Boolean.parseBoolean(note.split("-")[1]);
		}
		catch (Exception e)
		{
			setNote("1-false");
			return canRespawnHere();
		}
	}
	
	public void setImportance(int i)
	{
		String note = getNote();
		String importance, respawnHere;
		
		try
		{
			importance = note.split("-")[0];
			respawnHere = note.split("-")[1];
		}
		catch (Exception e)
		{
			setNote("1-false");
			note = getNote();
			
			importance = note.split("-")[0];
			respawnHere = note.split("-")[1];
		}
		
		importance = String.valueOf(i);
		
		note = importance + "-" + respawnHere;
		setNote(note);
	}
	
	public void setRespawnHere(boolean b)
	{
		String note = getNote();
		String importance, respawnHere;
		
		try
		{
			importance = note.split("-")[0];
			respawnHere = note.split("-")[1];
		}
		catch (Exception e)
		{
			setNote("1-false");
			note = getNote();
			
			importance = note.split("-")[0];
			respawnHere = note.split("-")[1];
		}
		
		respawnHere = String.valueOf(b);
		
		note = importance + "-" + respawnHere;
		setNote(note);
	}
	
	public int getRadius()
	{
		try
		{
			return Integer.parseInt(_note);
		}
		catch(Exception e)
		{
			return -1;
		}
	}
	
	public int getFenceWidth()
	{
		if(_type == SpawnType.Fence)
		{
			return _fenceWidth;
		}
		return 0;
	}
	
	public int getFenceLength()
	{
		if(_type == SpawnType.Fence)
		{
			return _fenceLength;
		}
		return 0;
	}
	
	public boolean isSaved()
	{
		return _saved;
	}
	
	public void setSaved(boolean b)
	{
		_saved = b;
	}
	
	/**
	 * most often notes:<br>
	 * <b>open / close</b> - handles door actions
	 */
	public String getNote()
	{
		return _note;
	}
}
