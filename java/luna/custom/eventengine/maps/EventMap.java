package luna.custom.eventengine.maps;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.model.Location;

public class EventMap
{
	private Location blueTeamLoc;
	private Location redTeamLoc;
	private List<Location> eventLocations = new ArrayList<>();
	private String mapName;
	private int mapId;
	public EventMap(Location blue, Location red, List<Location> locs, String name, int id)
	{
		blueTeamLoc = blue;
		redTeamLoc = red;
		locs.forEach(l -> eventLocations.add(l));
		mapName = name;
		mapId = id;
	}
	public Location getBlueTeamLocation()
	{
		return blueTeamLoc;
	}
	public Location getRedTeamLocation()
	{
		return redTeamLoc;
	}
	public List<Location> getEventLocations()
	{
		return eventLocations;
	}
	public String getMapName()
	{
		return mapName;
	}
	public int getMapId()
	{
		return mapId;
	}
}