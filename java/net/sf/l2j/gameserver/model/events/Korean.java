package net.sf.l2j.gameserver.model.events;

import java.util.ArrayList;
import java.util.Collection;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class Korean
{
	public static ArrayList<L2PcInstance>	_registeredPlayers	= new ArrayList<L2PcInstance>();
	static ArrayList<KoreanRoom>			rooms				= new ArrayList<KoreanRoom>();
	public static L2Spawn					_npcSpawn;
	public static int						_npcId				= 865000,
											_npcX = -82081,
											_npcY = 150824,
											_npcZ = -3129,
											_npcHeading = 32116;
	
	public static enum State
	{
		ACTIVE,
		INACTIVE,
		REGISTER,
		PREPARING
	}
	
	public static State state = State.INACTIVE;
	
	public class Start implements Runnable
	{
		@Override
		public void run()
		{
			if (state == State.INACTIVE)
			{
				startEvent();
			}
		}
	}
	
	public void startEvent()
	{
		state = State.REGISTER;
		spawnEventNpc();
		regAll();
		Announcements.getInstance().announceToAll("Korean event participation started");
		Announcements.getInstance().announceToAll("Joinable in Gludin!");
		Announcements.getInstance().announceToAll("5 minutes till Korean event registration close");
		// wait(1);
		// Announcements.getInstance().announceToAll("4 minutes till Korean event registration close");
		// wait(1);
		// Announcements.getInstance().announceToAll("3 minutes till Korean event registration close");
		// wait(1);
		// Announcements.getInstance().announceToAll("2 minutes till Korean event registration close");
		// wait(1);
		Announcements.getInstance().announceToAll("1 minute till Korean event registration close");
		wait(1);
		state = State.PREPARING;
		ArrayList<L2PcInstance> _players = new ArrayList<L2PcInstance>();
		int currentRoomId = 1;
		while (_registeredPlayers.size() >= 20)
		{
			for (L2PcInstance p : _registeredPlayers)
			{
				if (p == null)
					continue;
				if (_players.size() == 20)
					break;
				_players.add(p);
			}
			for (L2PcInstance p : _players)
			{
				_registeredPlayers.remove(p);
			}
			KoreanRoom kr = new KoreanRoom(currentRoomId, _players);
			rooms.add(kr);
			currentRoomId++;
			_players.clear();
		}
		if (_registeredPlayers.size() == 0)
		{
			_registeredPlayers.clear(); // clear null (disconnected) players
			_players.clear();
		}
		else if (_registeredPlayers.size() == 1)
		{
			_registeredPlayers.get(0).sendMessage("Unfortunately you are the only player left, here is a small gift for not being able to participate");
			_registeredPlayers.get(0).addItem("reward ths parhgorias", 57, 100000, null, true);
			_registeredPlayers.get(0).setIsInKoreanEvent(false);
			_registeredPlayers.clear();
			_players.clear();
		}
		else
		{
			for (L2PcInstance p : _registeredPlayers)
			{
				if (p == null)
					continue;
				_players.add(p);
			}
			for (L2PcInstance p : _players)
			{
				_registeredPlayers.remove(p);
			}
			// KoreanRoom.getInstance().executeRoom(currentRoomId,_players);
			KoreanRoom kr = new KoreanRoom(currentRoomId, _players);
			rooms.add(kr);
			_players.clear();
		}
		if (rooms.size() > 0)
		{
			state = State.ACTIVE;
			for (KoreanRoom kr : rooms)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(kr, 1000);
			}
		}
		else
		{
			Announcements.getInstance().announceToAll("Korean event has been cancelled due to lack of participants");
			state = State.INACTIVE;
			rooms.clear();
			_registeredPlayers.clear();// almost pointless but you never know
		}
		unspawnEventNpc();
	}
	
	public static void showChatWindow(L2PcInstance p)
	{
		if (p == null)
			return;
		String path = "data/html/custom/event/";
		String file = "registering.htm";
		switch (state)
		{
			case REGISTER:
				file = "registering.htm";
				break;
			case ACTIVE:
				file = "active.htm";
				break;
			default:
				file = "innactive.htm";
				break;
		}
		String htmContent = HtmCache.getInstance().getHtm(path + file);
		if (htmContent != null)
		{
			String regCommand = "";
			String btnName = "";
			if (!_registeredPlayers.contains(p))
			{	btnName = "Register";
				regCommand = ".leavekr";
			}
			else
			{	btnName = "Remove Registration";
				regCommand = ".joinkr";
			}
			htmContent = htmContent.replace("%btnName%", btnName);
			htmContent = htmContent.replace("%vcCommand%", regCommand);
			NpcHtmlMessage msg = new NpcHtmlMessage(5);
			msg.setHtml(htmContent);
			p.sendPacket(msg);
		}
		p.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void regAll()
	{
		final Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();  
		
		for(L2PcInstance p : pls)
		{
				register(p);
		}
	}
	private static void spawnEventNpc()
	{
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_npcId);
		try
		{
			_npcSpawn = new L2Spawn(tmpl);
			_npcSpawn.setLocx(_npcX);
			_npcSpawn.setLocy(_npcY);
			_npcSpawn.setLocz(_npcZ);
			_npcSpawn.setAmount(1);
			_npcSpawn.setHeading(_npcHeading);
			_npcSpawn.setRespawnDelay(1);
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			_npcSpawn.init();
			_npcSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			_npcSpawn.getLastSpawn().setTitle("Korean Event");
			_npcSpawn.getLastSpawn()._isEventKorean = true;
			_npcSpawn.getLastSpawn().isAggressive();
			_npcSpawn.getLastSpawn().decayMe();
			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());
			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public static void unspawnEventNpc()
	{
		if (_npcSpawn == null)
			return;
		_npcSpawn.getLastSpawn().deleteMe();
		_npcSpawn.stopRespawn();
		SpawnTable.getInstance().deleteSpawn(_npcSpawn, true);
	}
	
	static void endEvent()
	{
		if (state == State.INACTIVE)
			return;
		state = State.INACTIVE;
	}
	
	public void register(L2PcInstance p)
	{
		if (p.isInOlympiadMode())
		{
			p.sendMessage("You can't join Korean Event in Olympiad mode");
			return;
		}
		if (p.isInSiege())
		{
			p.sendMessage("You can't join Korean Event during sieges");
			return;
		}
		if (!_registeredPlayers.contains(p))
		{
			_registeredPlayers.add(p);
			p.setIsInKoreanEvent(true);
			p.sendMessage("You have succesfully registered");
		}
		else
		{
			p.sendMessage("You have already registered");
		}
	}
	
	public void unregister(L2PcInstance p)
	{
		if (_registeredPlayers.contains(p))
		{
			_registeredPlayers.remove(p);
			p.setIsInKoreanEvent(false);
			p.sendMessage("You have succesfully unregistered");
		}
		else
			p.sendMessage("You 're not in the registration list");
	}
	
	public void rewardWinner()
	{}
	
	public void waitSecs(int i)
	{
		try
		{
			Thread.sleep(i * 1000);
		}
		catch (InterruptedException ie)
		{
			ie.printStackTrace();
		}
	}
	
	public void wait(int i)
	{
		try
		{
			Thread.sleep(i * 60000);
		}
		catch (InterruptedException ie)
		{
			ie.printStackTrace();
		}
	}
	
	public Korean()
	{}
	
	public static Korean getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final Korean _instance = new Korean();
	}
}