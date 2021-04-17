package luna.discord;

import java.util.ArrayList;

import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.events.dataTables.NpcSpawnTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.RewardsTemplate;
import net.sf.l2j.gameserver.model.events.dataTables.TeamTemplate;

public class Event
{
	private int							evId;
	private String						evName;
	private String						evDesc;
	private String						evJoinLocName;
	private int							evMinLvl;
	private int							evMaxLvl;
	private int							evRegNpcId;
	private Location					evRegNpcLoc;
	private int							evJoinTime;
	private int							evEventTime;
	private int							evMinPl;
	private int							evMaxPl;
	private ArrayList<TeamTemplate>		evTeams		= new ArrayList<>();
	private ArrayList<NpcSpawnTemplate>	evNpcSpawns	= new ArrayList<>();
	private ArrayList<RewardsTemplate>	evRewards	= new ArrayList<>();
	
	public Event(int id, String name, String desc, String joinLocName, int minLvl, int maxLvl, int regNpcId, Location regNpcLoc, int joinTime, int eventTime, int minPl, int maxPl, ArrayList<TeamTemplate> teams, ArrayList<NpcSpawnTemplate> npcSpawns, ArrayList<RewardsTemplate> rewards)
	{
		evId = id;
		evName = name;
		evDesc = desc;
		evJoinLocName = joinLocName;
		evMinLvl = minLvl;
		evMaxLvl = maxLvl;
		evRegNpcId = regNpcId;
		evRegNpcLoc = regNpcLoc;
		evJoinTime = joinTime;
		evEventTime = eventTime;
		evMinPl = minPl;
		evMaxPl = maxPl;
		evTeams = teams;
		evNpcSpawns = npcSpawns;
		evRewards = rewards;
	}
	
	public int getId()
	{
		return evId;
	}
	
	public String getName()
	{
		return evName;
	}
	
	public String getDesc()
	{
		return evDesc;
	}
	
	public String getJoinLocName()
	{
		return evJoinLocName;
	}
	
	public int getMinLvl()
	{
		return evMinLvl;
	}
	
	public int getMaxLvl()
	{
		return evMaxLvl;
	}
	
	public int getRegNpcId()
	{
		return evRegNpcId;
	}
	
	public Location getRegNpcLoc()
	{
		return evRegNpcLoc;
	}
	
	public int getJoinTime()
	{
		return evJoinTime;
	}
	
	public int getEventTime()
	{
		return evEventTime;
	}
	
	public int getMinPl()
	{
		return evMinPl;
	}
	
	public int getMaxPl()
	{
		return evMaxPl;
	}
	
	public ArrayList<TeamTemplate> getEvTeams()
	{
		return evTeams;
	}
	
	public ArrayList<NpcSpawnTemplate> getEvSpawns()
	{
		return evNpcSpawns;
	}
	
	public ArrayList<RewardsTemplate> getEvRewards()
	{
		return evRewards;
	}
}