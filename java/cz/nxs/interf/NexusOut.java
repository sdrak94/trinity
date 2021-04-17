package cz.nxs.interf;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import cz.nxs.events.engine.base.Loc;
import cz.nxs.interf.delegate.DoorData;
import cz.nxs.interf.delegate.FenceData;
import cz.nxs.interf.delegate.InstanceData;
import cz.nxs.interf.handlers.AdminCommandHandlerInstance;
import cz.nxs.l2j.CallBack;
import cz.nxs.l2j.INexusOut;
import javolution.util.FastList;
import javolution.util.FastSet;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceNexusInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.util.Rnd;

/**
 * @author hNoke
 */
@SuppressWarnings("unused")
public class NexusOut implements INexusOut
{
	public void load()
	{
		CallBack.getInstance().setNexusOut(this);
	}
	
	@Override
	public ScheduledFuture<?> scheduleGeneral(Runnable task, long delay)
	{
		return ThreadPoolManager.getInstance().scheduleGeneral(task, delay);
	}
	
	@Override
	public ScheduledFuture<?> scheduleGeneralAtFixedRate(Runnable task, long initial, long delay)
	{
		return ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(task, initial, delay);
	}
	
	@Override
	public void executeTask(Runnable task)
	{
		ThreadPoolManager.getInstance().executeTask(task);
	}
	
	@Override
	public void purge()
	{
		ThreadPoolManager.getInstance().purge();
	}
	
	@Override
	public int getNextObjectId()
	{
		return IdFactory.getInstance().getNextId();
	}
	
	@Override
	public int random(int min, int max)
	{
		return Rnd.get(min, max);
	}
	
	@Override
	public int random(int max)
	{
		return Rnd.get(max);
	}
	
	@Override
	public Connection getConnection()
	{
		try
		{
			return L2DatabaseFactory.getInstance().getConnection();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public InstanceData createInstance(String name, int duration, int emptyDestroyTime, boolean isPvp, String template)
	{
		int instanceId = InstanceManager.getInstance().createDynamicInstance(template);
		InstanceManager.getInstance().getInstance(instanceId).setName(name);
		InstanceManager.getInstance().getInstance(instanceId).setAllowSummon(false);
		InstanceManager.getInstance().getInstance(instanceId).setDuration(duration);
		if (emptyDestroyTime > 0)
			InstanceManager.getInstance().getInstance(instanceId).setEmptyDestroyTime(emptyDestroyTime);
		InstanceManager.getInstance().getInstance(instanceId).setPvPInstance(isPvp);
		InstanceManager.getInstance().getInstance(instanceId).disableMessages();
		return new InstanceData(InstanceManager.getInstance().getInstance(instanceId));
	}
	
	@Override
	public InstanceData createInstance(String name, int duration, int emptyDestroyTime, boolean isPvp)
	{
		int instanceId = InstanceManager.getInstance().createDynamicInstance(null);
		InstanceManager.getInstance().getInstance(instanceId).setName(name);
		InstanceManager.getInstance().getInstance(instanceId).setAllowSummon(false);
		InstanceManager.getInstance().getInstance(instanceId).setDuration(duration);
		if (emptyDestroyTime > 0)
			InstanceManager.getInstance().getInstance(instanceId).setEmptyDestroyTime(emptyDestroyTime);
		InstanceManager.getInstance().getInstance(instanceId).setPvPInstance(isPvp);
		InstanceManager.getInstance().getInstance(instanceId).disableMessages();
		return new InstanceData(InstanceManager.getInstance().getInstance(instanceId));
	}
	
	@Override
	public void addDoorToInstance(int instanceId, int doorId, boolean opened)
	{
		InstanceManager.getInstance().getInstance(instanceId).addDoor(doorId, opened);
	}
	
	@Override
	public DoorData[] getInstanceDoors(int instanceId)
	{
		List<DoorData> doors = new FastList<DoorData>();
		for (L2DoorInstance d : InstanceManager.getInstance().getInstance(instanceId).getDoors())
		{
			doors.add(new DoorData(d));
		}
		return doors.toArray(new DoorData[doors.size()]);
	}
	
	@Override
	public void registerAdminHandler(AdminCommandHandlerInstance handler)
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(handler);
	}
	// ***
	
	@Override
	public PlayerEventInfo getPlayer(int playerId)
	{
		try
		{
			return L2World.getInstance().getPlayer(playerId).getEventInfo();
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	@Override
	public PlayerEventInfo getPlayer(String name)
	{
		try
		{
			return L2World.getInstance().getPlayer(name).getEventInfo();
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	@Override
	public String getClanName(int clanId)
	{
		try
		{
			return ClanTable.getInstance().getClan(clanId).getName();
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	@Override
	public String getAllyName(int clanId)
	{
		try
		{
			return ClanTable.getInstance().getClan(clanId).getAllyName();
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	@Override
	public void announceToAllScreenMessage(String message, String announcer)
	{
		Broadcast.toAllOnlinePlayers(new CreatureSay(0, 18, "", announcer + ": " + message));
	}
	
	@Override
	public String getHtml(String path)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		if (!html.setFile(null, path))
			return null;
		return html.getText();
	}
	
	@Override
	public String getEventHtml(String path)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		if (!html.setEventHtml(path)) // same as setFile, but doesn't show warnings if file not found
			return null;
		return html.getText();
	}
	
	@Override
	public void reloadHtmls()
	{
		HtmCache.getInstance().reload();
	}
	
	@Override
	public String getItemName(int id)
	{
		try
		{
			return ItemTable.getInstance().getTemplate(id).getName();
		}
		catch (Exception e)
		{
			return "Unknown item";
		}
	}
	
	@Override
	public boolean doorExists(int id)
	{
		return DoorTable.getInstance().getDoor(id) != null;
	}
	
	@Override
	public FenceData createFence(int type, int width, int length, int x, int y, int z, int eventId)
	{
		return new FenceData(new L2FenceInstance(getNextObjectId(), "Nexus", type, x, y, z, width, length, 1, eventId));
	}
	
	@Override
	public void spawnFences(List<FenceData> list, int instance)
	{
		for (FenceData fence : list)
		{
			if (fence.getOwner() != null)
			{
				if (instance > 0)
					fence.getOwner().setInstanceId(instance);
				fence.getOwner().spawnMe(fence.getOwner().getX(), fence.getOwner().getY(), fence.getOwner().getZ());
			}
		}
	}
	
	@Override
	public void unspawnFences(List<FenceData> list)
	{
		for (FenceData fence : list)
		{
			if (fence != null)
			{
				L2WorldRegion region = fence.getOwner().getWorldRegion();
				fence.getOwner().decayMe();
				if (region != null)
					region.removeVisibleObject(fence.getOwner());
				fence.getOwner().getKnownList().removeAllKnownObjects();
				L2World.getInstance().removeObject(fence.getOwner());
			}
		}
	}
	
	@Override
	public int getGradeFromFirstLetter(String s)
	{
		if (s.equalsIgnoreCase("n") || s.equalsIgnoreCase("ng") || s.equalsIgnoreCase("no"))
		{
			return L2Item.CRYSTAL_NONE;
		}
		else if (s.equalsIgnoreCase("d"))
		{
			return L2Item.CRYSTAL_D;
		}
		else if (s.equalsIgnoreCase("c"))
		{
			return L2Item.CRYSTAL_C;
		}
		else if (s.equalsIgnoreCase("b"))
		{
			return L2Item.CRYSTAL_B;
		}
		else if (s.equalsIgnoreCase("a"))
		{
			return L2Item.CRYSTAL_A;
		}
		else if (s.equalsIgnoreCase("s"))
		{
			return L2Item.CRYSTAL_S;
		}
		else if (s.equalsIgnoreCase("s80"))
		{
			return L2Item.CRYSTAL_S80;
		}
		else if (s.equalsIgnoreCase("s84"))
		{
			return L2Item.CRYSTAL_S84;
		}
		return 0;
	}
	
	@Override
	public Set<Integer> getAllWeaponsId()
	{
		Set<Integer> set = new FastSet<Integer>();
		set.addAll(ItemTable.getInstance().getAllWeaponsId());
		return set;
	}
	
	@Override
	public Set<Integer> getAllArmorsId()
	{
		Set<Integer> set = new FastSet<Integer>();
		set.addAll(ItemTable.getInstance().getAllArmorsId());
		return set;
	}
	
	@Override
	public Integer[] getAllClassIds()
	{
		List<Integer> idsList = new FastList<Integer>();
		for (ClassId id : ClassId.values())
			idsList.add(id.getId());
		return idsList.toArray(new Integer[idsList.size()]);
	}
	
	@Override
	public PlayerEventInfo[] getAllPlayers()
	{
		Collection<L2PcInstance> players = L2World.getInstance().getAllPlayers().values();
		List<PlayerEventInfo> eventInfos = new FastList<PlayerEventInfo>();
		for (L2PcInstance player : players)
		{
			eventInfos.add(player.getEventInfo());
		}
		return eventInfos.toArray(new PlayerEventInfo[eventInfos.size()]);
	}
	
	@Override
	public void loadNextPvpZone()
	{
		// TODO
	}
	
	protected static NexusOut getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final NexusOut _instance = new NexusOut();
	}
	
	/*
	 * (non-Javadoc)
	 * @see cz.nxs.l2j.INexusOut#getPvpZoneMainEventNpcLoc()
	 */
	@Override
	public Loc getPvpZoneMainEventNpcLoc()
	{
		return null;
	}
}
