package net.sf.l2j.gameserver.model.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class Domination
{
	private static ArrayList<L2PcInstance>	blueTeam				= new ArrayList<L2PcInstance>();
	private static ArrayList<L2PcInstance>	redTeam					= new ArrayList<L2PcInstance>();
	private static ArrayList<L2PcInstance>	disconnectedBlueTeam	= new ArrayList<L2PcInstance>();
	private static ArrayList<L2PcInstance>	disconnectedRedTeam		= new ArrayList<L2PcInstance>();
	private static ArrayList<L2PcInstance>	players					= new ArrayList<L2PcInstance>();
	private Location						blueTeamLoc;
	private Location						blueTeamLoc1			= new Location(-184141, 238708, 1436);
	private Location						blueTeamLoc2			= new Location(-183706, 239432, 1284);
	private Location						blueTeamLoc3			= new Location(-184930, 239056, 1287);
	private Location						redTeamLoc;
	private Location						redTeamLoc1				= new Location(-187053, 246016, 1284);
	private Location						redTeamLoc2				= new Location(-185791, 246401, 1287);
	private Location						redTeamLoc3				= new Location(-186620, 246859, 1436);

	private Location						redBufferLoc1				= new Location(-185715, 246431, 1286);
	private Location						redBufferLoc2				= new Location(-187128, 245997, 1287);
	private Location						redBufferLoc3				= new Location(-186725, 247204, 1436);
	private Location						blueBufferLoc1				= new Location(-185015, 239028, 1287);
	private Location						blueBufferLoc2				= new Location(-184005, 238243, 1436);
	private Location						blueBufferLoc3				= new Location(-183602, 239458, 1289);
		
	
	private static L2Spawn					base;
	private int								redPlayersCloseToBase	= 0;
	private int								bluePlayersCloseToBase	= 0;
	private int								redPoints				= 0;
	private int								bluePoints				= 0;
	private String							lastWinningTeam			= "";
	int										instanceId				= 50;
	public static L2Spawn					_npcSpawn;
	public static int						_npcId					= 865000,
	_npcX = -82081,
	_npcY = 150824,
	_npcZ = -3129,
	_npcHeading = 32116;
	
	public enum State
	{
		ACTIVE,
		INACTIVE,
		REGISTER
	}
	
	public static State state = State.INACTIVE;
	
	public class Start implements Runnable
	{
		@Override
		public void run()
		{
			if (state == State.INACTIVE)
			{
				EventWaiter(15 * 60 * 1000);
				startEvent();
			}
		}
	}
	private  void EventWaiter(long interval)
	{
		long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000);
		while (startWaiterTime + interval > System.currentTimeMillis())
		{
			seconds--; // here because we don't want to see two time announce at
						// the same time
			if (state == State.REGISTER || state == State.ACTIVE)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						if (state == State.REGISTER)
						{
							Announcements.getInstance().announceToAll("(Domination): Joinable in Gludin Village!");
							Announcements.getInstance().announceToAll("Domination Event: " + seconds / 60 / 60 + " hour(s) till registration ends!");
						}
						else if (state == State.ACTIVE)
							Announcements.getInstance().announceToAll("(Domination): " + seconds / 60 / 60 + " hour(s) till event ends!");
						break;
					case 1800: // 30 minutes left
					case 900: // 15 minutes left
					case 600: // 10 minutes left
					case 300: // 5 minutes left
					case 120: // 2 minutes left
					case 60: // 1 minute left
						if (state == State.REGISTER)
						{
							//removeOfflinePlayers();
							Announcements.getInstance().announceToAll("(Domination): Joinable in Gludin Village!");
							Announcements.getInstance().announceToAll("(Domination): " + seconds / 60 + " minute(s) till registration ends!");
						}
						else if (state == State.ACTIVE)
							Announcements.getInstance().announceToAll("(Domination): " + seconds / 60 + " minute(s) till event ends!");
						break;
					case 30: // 30 seconds left
					case 10: // 10 seconds left
					case 3: // 3 seconds left
					case 2: // 2 seconds left
					case 1: // 1 seconds left
						if (state == State.REGISTER)
							Announcements.getInstance().announceToAll("(Domination): " + seconds + " second(s) till registration ends!");
						else if (state == State.ACTIVE)
							Announcements.getInstance().announceToAll("(Domination): " + seconds + " second(s) till event ends!");
						break;
				}
			}

			if(state != State.ACTIVE)
				break;
			seconds--; // here because we don't want to see two time announce at the same time
			String mins = "" + seconds / 60;
			String secs = (seconds % 60 < 10 ? "0" + seconds % 60 : "" + seconds % 60);
			String text = "" + mins + ":" + secs + "  ";
			text += "Red Team: " + redPoints + " Blue Team: " + bluePoints;
			if (state == State.ACTIVE)
			{
				checkDistances();
				for (L2PcInstance player : players)
				{
					if (player == null || player.isOnline() == 0)
						continue;
					player.sendPacket(new ExShowScreenMessage(1, -1, 3, 0, 1, 0, 0, true, 2000, 0, text));
				}
			}
			long startOneSecondWaiterStartTime = System.currentTimeMillis();
			// only the try catch with Thread.sleep(1000) give bad countdown on high wait times
			while (startOneSecondWaiterStartTime + 1000 > System.currentTimeMillis())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException ie)
				{}
			}
		
		}
	}
	public State getState()
	{
		return state;
	}
	
	public Location rndLocPickerBlue()
	{
		int rnd = Rnd.get(1, 3);
		switch (rnd)
		{
			case 1:
				blueTeamLoc = blueTeamLoc1;
				break;
			case 2:
				blueTeamLoc = blueTeamLoc2;
				break;
			case 3:
				blueTeamLoc = blueTeamLoc3;
				break;
		}
		return blueTeamLoc;
	}
	
	public Location rndLocPickerRed()
	{
		int rnd = Rnd.get(1, 3);
		switch (rnd)
		{
			case 1:
				redTeamLoc = redTeamLoc1;
				break;
			case 2:
				redTeamLoc = redTeamLoc2;
				break;
			case 3:
				redTeamLoc = redTeamLoc3;
				break;
		}
		return redTeamLoc;
	}
	
	private void regAll()
	{
		final Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
		for (L2PcInstance p : pls)
		{
			register(p);
		}
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
			if (!players.contains(p))
			{
				btnName = "Register";
				regCommand = ".joindomi";
			}
			else
			{
				btnName = "Remove Registration";
				regCommand = ".leavedomi";
			}
			htmContent = htmContent.replace("%btnName%", btnName);
			htmContent = htmContent.replace("%vcCommand%", regCommand);
			NpcHtmlMessage msg = new NpcHtmlMessage(5);
			msg.setHtml(htmContent);
			p.sendPacket(msg);
		}
		p.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private  void spawnBuffers()
	{
		final L2NpcTemplate template = NpcTable.getInstance().getTemplate(CTF.NPC_BUFFER_ID);
		for (int i = 1; i <= 6; i++)
		{
			Location bufferLoc =			null;
			switch (i)
			{
				case 1: bufferLoc = redBufferLoc1;
				break;
				case 2: bufferLoc = redBufferLoc2;
				break;
				case 3: bufferLoc = redBufferLoc3;
				break;
				case 4: bufferLoc = blueBufferLoc1;
				break;
				case 5: bufferLoc = blueBufferLoc2;
				break;
				case 6: bufferLoc = blueBufferLoc3;
				break;
			}
			try
			{
				
				final L2Spawn spawn = new L2Spawn(template);
				spawn.setLocx(bufferLoc.getX());
				spawn.setLocy(bufferLoc.getY());
				spawn.setLocz(bufferLoc.getZ());
				spawn.setAmount(1);
				spawn.setHeading(0);
				spawn.setRespawnDelay(10);
				spawn.setInstanceId(instanceId);
				SpawnTable.getInstance().addNewSpawn(spawn, false);
				spawn.init();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private static void spawnEventNpc()
	{
		L2NpcTemplate regTmpl = NpcTable.getInstance().getTemplate(_npcId);
		try
		{
			_npcSpawn = new L2Spawn(regTmpl);
			_npcSpawn.setLocx(_npcX);
			_npcSpawn.setLocy(_npcY);
			_npcSpawn.setLocz(_npcZ);
			_npcSpawn.setAmount(1);
			_npcSpawn.setHeading(_npcHeading);
			_npcSpawn.setRespawnDelay(1);
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			_npcSpawn.init();
			_npcSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			_npcSpawn.getLastSpawn().setTitle("Domination Event");
			_npcSpawn.getLastSpawn()._isEventDomination = true;
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
	
	public void startEvent()
	{
		//state = State.REGISTER;
		//spawnEventNpc();
		//regAll();
		Announcements.getInstance().announceToAll("Domination event participation started");
		Announcements.getInstance().announceToAll("5 minutes till Domination event registration close");
		Announcements.getInstance().announceToAll("Register command: .joindomi || Leave command: .leavedomi");
		wait(1);
		// Announcements.getInstance().announceToAll("4 minutes till Domination event registration close");
		// wait(1);
		// Announcements.getInstance().announceToAll("3 minutes till Domination event registration close");
		// wait(1);
		// Announcements.getInstance().announceToAll("2 minutes till Domination event registration close");
		// wait(1);
		// Announcements.getInstance().announceToAll("1 minute till Domination event registration close");
		// wait(1);
		if (players.size() >= 2)
		{
			state = State.ACTIVE;
			Collections.shuffle(players);
			for (int i = 0; i < (players.size()); i++)
			{
				if (players.get(i) == null)
					continue;
				if (i % 2 == 0)
				{
					redTeam.add(players.get(i));
					players.get(i).setDominationTeam("red");
					players.get(i).broadcastUserInfo();
				}
				else
				{
					blueTeam.add(players.get(i));
					players.get(i).setDominationTeam("blue");
					players.get(i).broadcastUserInfo();
				}
			}
			for (L2PcInstance p : players)
			{
				if (p == null)
					continue;
				p.startAbnormalEffect(AbnormalEffect.HOLD_1);
				p.setIsParalyzed(true);
				StopMove sm = new StopMove(p);
				sm = new StopMove(p);
				p.sendPacket(sm);
				p.broadcastPacket(sm);
				ExShowScreenMessage message = new ExShowScreenMessage("You will be teleported in Domination Arena in 10 seconds", 5000);
				p.sendPacket(message);
				InstanceManager.getInstance().createInstance(instanceId);
				p.setInstanceId(instanceId);
			}
			spawnBuffers();
			spawnBase();
			waitSecs(10);
			// teleport red team
			for (L2PcInstance p : redTeam)
			{
				if (p == null)
					continue;
				p.setIsInActiveDominationEvent(true);
				p.teleToLocation(rndLocPickerRed(), true);
			}
			// teleport blue team
			for (L2PcInstance p : blueTeam)
			{
				if (p == null)
					continue;
				p.setIsInActiveDominationEvent(true);
				p.teleToLocation(rndLocPickerBlue(), true);
			}
			preparePlayers();
			waiter(240000);
			// for (int i = 0; i < 132; i++)
			// {
			// if(state != State.ACTIVE)
			// break;
			// checkDistances();
			// waitSecs(5);
			// }
			end();
		}
		else
		{
			Announcements.getInstance().announceToAll("Domination event has been cancelled due to lack of participants");
			state = State.INACTIVE;
			cleanEvent();
		}
	}
	
	public void preparePlayers()
	{
		for (int i = 10; i > 0; i--)
		{
			for (L2PcInstance p : players)
			{
				if (p == null)
					continue;
				ExShowScreenMessage message = new ExShowScreenMessage("Event will begin in " + i, 999);
				p.sendPacket(message);
			}
			waitSecs(1);
		}
		for (L2PcInstance p : players)
		{
			if (p == null)
				continue;
			if (p.isDead())
				onDeath(p);
			p.setCurrentCp(p.getMaxCp());
			p.setCurrentHpMp(p.getMaxHp(), p.getMaxMp());
			p.stopAbnormalEffect(AbnormalEffect.HOLD_1);
			p.setIsParalyzed(false);
		}
	}
	
	public void onDeath(L2PcInstance p)
	{
		if (getState() == Domination.State.ACTIVE)
		{
			p.sendMessage("You will be revived in 5 seconds!");
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					p.doRevive();
					if (p.dominationTeam == "blue")
						p.teleToLocation(rndLocPickerBlue(), true);
					else
						p.teleToLocation(rndLocPickerRed(), true);
					p.broadcastStatusUpdate();
					p.broadcastUserInfo();
				}
			}, 5000);
		}
	}
	
	public void removeDisconnectedPlayer(L2PcInstance p)
	{
		if (players.contains(p))
		{
			System.out.println("1");
			players.remove(p);
			if (blueTeam.contains(p))
			{
				disconnectedBlueTeam.add(p);
				blueTeam.remove(p);
				if (blueTeam.size() == 0)
				{
					end();
				}
				System.out.println("2");
			}
			else
			{
				System.out.println("3");
				redTeam.remove(p);
				disconnectedRedTeam.add(p);
				if (redTeam.size() == 0)
				{
					end();
				}
				System.out.println("4");
			}
		}
		else
		{
			System.out.println("WTF? not in _players ?");
		}
	}
	
	public void onReconnect(L2PcInstance p)
	{
		boolean existsBlue = false;
		boolean existsRed = false;
		for (int i = 0; i < disconnectedBlueTeam.size(); i++)
		{
			if (disconnectedBlueTeam.get(i).getObjectId() == p.getObjectId())
			{
				existsBlue = true;
				break;
			}
		}
		if (existsBlue)
		{
			blueTeam.add(p);
			players.add(p);
			p.teleToLocation(rndLocPickerBlue(), true);
			p.setInstanceId(instanceId);
			p.setDominationTeam("blue");
			p.broadcastUserInfo();
			disconnectedBlueTeam.remove(p);
		}
		for (int i = 0; i < disconnectedRedTeam.size(); i++)
		{
			if (disconnectedRedTeam.get(i).getObjectId() == p.getObjectId())
			{
				existsRed = true;
				break;
			}
		}
		if (existsRed)
		{
			redTeam.add(p);
			players.add(p);
			p.teleToLocation(rndLocPickerRed(), true);
			p.setInstanceId(instanceId);
			p.setDominationTeam("red");
			p.broadcastUserInfo();
			disconnectedRedTeam.remove(p);
		}
		else
			System.out.println(".....");
		// if (disconnectedBlueTeam.contains(p))
		// {
		// blueTeam.add(p);
		// players.add(p);
		// p.teleToLocation(blueTeamLoc, true);
		// p.setInstanceId(instanceId);
		// p.setDominationTeam("blue");
		// p.broadcastUserInfo();
		// disconnectedBlueTeam.remove(p);
		// }
		// if (disconnectedRedTeam.contains(p))
		// {
		// redTeam.add(p);
		// players.add(p);
		// p.teleToLocation(redTeamLoc, true);
		// p.setInstanceId(instanceId);
		// p.setDominationTeam("red");
		// p.broadcastUserInfo();
		// disconnectedRedTeam.remove(p);
		// }
	}
	
	private void spawnBase()
	{
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(865001);// base id
		try
		{
			base = new L2Spawn(tmpl);
			base.setLocx(-185720); // loc x
			base.setLocy(242614); // loc y
			base.setLocz(1684); // loc z
			base.setAmount(1);
			base.setHeading(0);
			base.setRespawnDelay(1);
			base.setInstanceId(instanceId);
			SpawnTable.getInstance().addNewSpawn(base, false);
			base.init();
			base.getLastSpawn().setTitle("Owner: NONE");
			base.getLastSpawn().isAggressive();
			base.getLastSpawn().decayMe();
			base.getLastSpawn().spawnMe(base.getLastSpawn().getX(), base.getLastSpawn().getY(), base.getLastSpawn().getZ());
		}
		catch (Exception e)
		{
			System.out.println("Couldn't spawn Base for Domination Event");
		}
	}
	
	private void waiter(long interval)
	{
		long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000);
		while (startWaiterTime + interval > System.currentTimeMillis())
		{
			if(state != State.ACTIVE)
				break;
			seconds--; // here because we don't want to see two time announce at the same time
			String mins = "" + seconds / 60;
			String secs = (seconds % 60 < 10 ? "0" + seconds % 60 : "" + seconds % 60);
			String text = "" + mins + ":" + secs + "  ";
			text += "Red Team: " + redPoints + " Blue Team: " + bluePoints;
			if (state == State.ACTIVE)
			{
				checkDistances();
				for (L2PcInstance player : players)
				{
					if (player == null || player.isOnline() == 0)
						continue;
					player.sendPacket(new ExShowScreenMessage(1, -1, 3, 0, 1, 0, 0, true, 2000, 0, text));
				}
			}
			long startOneSecondWaiterStartTime = System.currentTimeMillis();
			// only the try catch with Thread.sleep(1000) give bad countdown on high wait times
			while (startOneSecondWaiterStartTime + 1000 > System.currentTimeMillis())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException ie)
				{}
			}
		}
	}
	
	private void unspawnBase()
	{
		if (base == null || base.getLastSpawn() == null)
			return;
		base.getLastSpawn().deleteMe();
		base.stopRespawn();
		SpawnTable.getInstance().deleteSpawn(base, true);
	}
	
	private void checkDistances()
	{
		for (L2PcInstance p : redTeam)
		{
			if (p == null || p.isDead())
				continue;
			if (Util.calculateDistance(p.getX(), p.getY(), p.getZ(), base.getLocx(), base.getLocy(), base.getLocz(), false) <= 200)
			{
				redPlayersCloseToBase++;
			}
		}
		for (L2PcInstance p : blueTeam)
		{
			if (p == null || p.isDead())
				continue;
			if (Util.calculateDistance(p.getX(), p.getY(), p.getZ(), base.getLocx(), base.getLocy(), base.getLocz(), false) <= 200)
			{
				bluePlayersCloseToBase++;
			}
		}
		if (redPlayersCloseToBase > bluePlayersCloseToBase)
		{
			for (L2PcInstance p : redTeam)
			{
				if (p == null || p.isDead())
					continue;
				if (Util.calculateDistance(p.getX(), p.getY(), p.getZ(), base.getLocx(), base.getLocy(), base.getLocz(), false) <= 200)
				{
					p._dominationScore++;
					p.broadcastTitleInfo();
					p.broadcastUserInfo();
				}
			}
			redPoints++;
			lastWinningTeam = "red";
			base.getLastSpawn().stopAbnormalEffect(AbnormalEffect.IMPRISIONING_1);
			base.getLastSpawn().startAbnormalEffect(AbnormalEffect.VITALITY);
			base.getLastSpawn().setTitle("Owned by Red");
		}
		else if (redPlayersCloseToBase < bluePlayersCloseToBase)
		{
			for (L2PcInstance p : blueTeam)
			{
				if (p == null || p.isDead())
					continue;
				if (Util.calculateDistance(p.getX(), p.getY(), p.getZ(), base.getLocx(), base.getLocy(), base.getLocz(), false) <= 200)
				{
					p._dominationScore++;
					p.broadcastTitleInfo();
					p.broadcastUserInfo();
				}
			}
			bluePoints++;
			// bluePoints++;
			lastWinningTeam = "blue";
			base.getLastSpawn().stopAbnormalEffect(AbnormalEffect.VITALITY);
			base.getLastSpawn().startAbnormalEffect(AbnormalEffect.IMPRISIONING_1);
			base.getLastSpawn().setTitle("Owned by Blue");
		}
		else if (redPlayersCloseToBase == bluePlayersCloseToBase)
		{
			base.getLastSpawn().stopAbnormalEffect(AbnormalEffect.IMPRISIONING_1);
			base.getLastSpawn().stopAbnormalEffect(AbnormalEffect.VITALITY);
			base.getLastSpawn().setTitle("No owner");
			lastWinningTeam = "";
		}
		else
		{
			base.getLastSpawn().stopAbnormalEffect(AbnormalEffect.IMPRISIONING_1);
			base.getLastSpawn().stopAbnormalEffect(AbnormalEffect.VITALITY);
			base.getLastSpawn().setTitle("No owner");
			lastWinningTeam = "";
		}
		if (base != null)
			base.getLastSpawn().updateAbnormalEffect();
		// for (L2PcInstance p : players)
		// {
		// if (p == null)
		// continue;
		// p.sendPacket(new ExShowScreenMessage(1, -1, 3, 0, 1, 0, 0, true, 5000, 0, "Red Team: " + redPoints + " Blue Team: " + bluePoints));
		// }
		redPlayersCloseToBase = 0;
		bluePlayersCloseToBase = 0;
	}
	
	public String getLastWinningTeam()
	{
		return lastWinningTeam;
	}
	
	public void end()
	{
		if (state == State.INACTIVE)
			return;
		unspawnBase();
		rewardWinner();
		for (L2PcInstance p : players)
		{
			if (p == null)
				continue;
			ExShowScreenMessage message = new ExShowScreenMessage("You will be teleported in Giran Town in 5 seconds", 5000);
			p.sendPacket(message);
		}
		waitSecs(5);
		for (L2PcInstance p : players)
		{
			if (p == null)
				continue;
			p.teleToLocation(83450, 148608, -3405);
			p.setInstanceId(0);
			p.setIsInDominationEvent(false);
			p.setIsInActiveDominationEvent(false);
			p._dominationKills = 0;
			p._dominationScore = 0;
			p.setDominationTeam("");
			p.broadcastUserInfo();
		}
		cleanEvent();
		unspawnEventNpc();
		state = State.INACTIVE;
	}
	
	private void cleanEvent()
	{
		redTeam.clear();
		blueTeam.clear();
		players.clear();
		redPlayersCloseToBase = 0;
		bluePlayersCloseToBase = 0;
		redPoints = 0;
		bluePoints = 0;
		lastWinningTeam = "";
		base = null;
		disconnectedRedTeam.clear();
		disconnectedBlueTeam.clear();
		InstanceManager.getInstance().destroyInstance(instanceId);
	}
	
	public void register(L2PcInstance p)
	{
		if (p.isInOlympiadMode())
		{
			p.sendMessage("You can't join Domination Event in Olympiad mode");
			return;
		}
		if (p.isInSiege())
		{
			p.sendMessage("You can't join Domination Event during sieges");
			return;
		}
		if (!players.contains(p))
		{
			players.add(p);
			p.setIsInDominationEvent(true);
			p.sendMessage("You have succesfully registered");
		}
		else
		{
			p.sendMessage("You have already registered");
		}
	}
	
	public void unregister(L2PcInstance p)
	{
		if (players.contains(p))
		{
			players.remove(p);
			p.setIsInDominationEvent(false);
			p.sendMessage("You have succesfully unregistered");
		}
		else
			p.sendMessage("You 're not in the registration list");
	}
	
	public void rewardWinner()
	{
		if (blueTeam.size() == 0)// every trash blue disconnected
		{
			for (L2PcInstance p : redTeam)
			{
				if (p == null)
					continue;
				p.addItem("Domination", 57, 10000, p, true);
			}
			Announcements.getInstance().announceToAll("Domination event is over, red team is victorious!");
		}
		else if (redTeam.size() == 0)// every trash red disconnected
		{
			for (L2PcInstance p : blueTeam)
			{
				if (p == null)
					continue;
				p.addItem("Domination", 57, 10000, p, true);
			}
			Announcements.getInstance().announceToAll("Domination event is over, blue team is victorious!");
		}
		else if (redPoints > bluePoints) // reward red team
		{
			for (L2PcInstance p : redTeam)
			{
				if (p == null)
					continue;
				p.addItem("Domination", 57, 10000, p, true);
			}
			Announcements.getInstance().announceToAll("Domination event is over, red team is victorious!");
		}
		else if (redPoints < bluePoints)// reward blue team
		{
			for (L2PcInstance p : blueTeam)
			{
				if (p == null)
					continue;
				p.addItem("Domination", 57, 10000, p, true);
			}
			Announcements.getInstance().announceToAll("Domination event is over, blue team is victorious!");
		}
		else // reward in case of draw
		{
			for (L2PcInstance p : players)
			{
				if (p == null)
					continue;
				p.addItem("Domination", 57, 10000, p, true);
			}
			Announcements.getInstance().announceToAll("Domination event is over, the game ended in a draw!");
		}
	}
	
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
	
	private static void closeDoor(int i)
	{
		L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(i);
		if (doorInstance != null)
		{
			doorInstance.closeMe();
		}
	}
	
	private static void openDoor(int i)
	{
		L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(i);
		if (doorInstance != null)
		{
			doorInstance.openMe();
		}
	}
	
	public Domination()
	{}
	
	public static Domination getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final Domination _instance = new Domination();
	}
}