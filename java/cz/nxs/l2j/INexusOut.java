package cz.nxs.l2j;

import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import cz.nxs.events.engine.base.Loc;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.DoorData;
import cz.nxs.interf.delegate.FenceData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.handlers.AdminCommandHandlerInstance;

/**
 * @author hNoke
 */
public interface INexusOut
{
	public ScheduledFuture<?> scheduleGeneral(Runnable task, long delay);
	public ScheduledFuture<?> scheduleGeneralAtFixedRate(Runnable task, long initial, long delay);
	public void executeTask(Runnable task);
	public void purge();
	
	public int getNextObjectId();
	
	public int random(int min, int max);
	public int random(int max);
	
	public Connection getConnection();
	
	public InstanceData createInstance(String name, int duration, int emptyDestroyTime, boolean isPvp);
	public InstanceData createInstance(String name, int duration, int emptyDestroyTime, boolean isPvp, String template);
	public void addDoorToInstance(int instanceId, int doorId, boolean opened);
	public DoorData[] getInstanceDoors(int instanceId);
	
	public void registerAdminHandler(AdminCommandHandlerInstance handler);
	
	public String getClanName(int clanId);
	public String getAllyName(int allyId);
	public PlayerEventInfo getPlayer(int playerId);
	public PlayerEventInfo getPlayer(String name);
	public Integer[] getAllClassIds();
	public PlayerEventInfo[] getAllPlayers();
	
	public void announceToAllScreenMessage(String message, String announcer);
	
	public String getHtml(String path);
	public String getEventHtml(String path);
	public void reloadHtmls();
	
	public boolean doorExists(int id);
	public FenceData createFence(int type, int width, int length, int x, int y, int z, int eventId);
	public void spawnFences(List<FenceData> list, int instance);
	public void unspawnFences(List<FenceData> list);
	
	public int getGradeFromFirstLetter(String s);
	public String getItemName(int id);
	public Set<Integer> getAllWeaponsId();
	public Set<Integer> getAllArmorsId();
	
	public void loadNextPvpZone();
	public Loc getPvpZoneMainEventNpcLoc();
}
