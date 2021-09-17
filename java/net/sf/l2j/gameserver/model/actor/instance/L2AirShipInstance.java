/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.actor.instance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.instancemanager.AirShipManager;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.knownlist.AirShipKnownList;
import net.sf.l2j.gameserver.network.serverpackets.ExAirShipInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExGetOffAirShip;
import net.sf.l2j.gameserver.network.serverpackets.ExGetOnAirShip;
import net.sf.l2j.gameserver.network.serverpackets.ExMoveToLocationAirShip;
import net.sf.l2j.gameserver.network.serverpackets.ExStopMoveAirShip;
import net.sf.l2j.gameserver.templates.chars.L2CharTemplate;
import net.sf.l2j.gameserver.templates.item.L2Weapon;

/**
 * Flying airships. Very similar to Maktakien boats (see L2BoatInstance) but these do fly :P
 *
 * @author  DrHouse
 */
public class L2AirShipInstance extends L2Character
{
	public float boatSpeed;
	protected final FastList<L2PcInstance> _passengers = new FastList<L2PcInstance>();
	protected static final Logger _airShiplog = Logger.getLogger(L2AirShipInstance.class.getName());
	
	private class L2AirShipTrajet
	{
		private Map<Integer, L2AirShipPoint> _path;
		public int idWaypoint;
		public int max;
		
		protected class L2AirShipPoint
		{
			public int speed1 = 350;
			@SuppressWarnings("unused")
			public int speed2 = 4000;
			public int x;
			public int y;
			public int z;
			public int time;
		}
		/**
		 * @param idWaypoint

		 */
		public L2AirShipTrajet(int pIdWaypoint)
		{
			idWaypoint = pIdWaypoint;
			loadBoatPath();
		}
		/**
		 * @param line
		 * @return
		 */
		public void parseLine(String line)
		{
			_path = new FastMap<Integer, L2AirShipPoint>();
			StringTokenizer st = new StringTokenizer(line, ";");
			Integer.parseInt(st.nextToken());
			max = Integer.parseInt(st.nextToken());
			for (int i = 0; i < max; i++)
			{
				L2AirShipPoint bp = new L2AirShipPoint();
				bp.x = Integer.parseInt(st.nextToken());
				bp.y = Integer.parseInt(st.nextToken());
				bp.z = Integer.parseInt(st.nextToken());
				bp.time = Integer.parseInt(st.nextToken());
				_path.put(i, bp);
			}
		}
		/**
		 * 
		 */
		private void loadBoatPath()
		{
			LineNumberReader lnr = null;
			try
			{
				File doorData = new File(Config.DATAPACK_ROOT, "data/airshippath.csv");
				lnr = new LineNumberReader(new BufferedReader(new FileReader(doorData)));
				
				String line = null;
				while ((line = lnr.readLine()) != null)
				{
					if (line.trim().length() == 0
					        || !line.startsWith(idWaypoint + ";"))
						continue;
					parseLine(line);
					return;
				}
				_airShiplog.warning("No path for airShip!!!");
			}
			catch (FileNotFoundException e)
			{
				_airShiplog.warning("airship.csv is missing in data folder");
			}
			catch (Exception e)
			{
				_airShiplog.warning("error while creating airship table " + e);
			}
			finally
			{
				try
				{
					lnr.close();
				}
				catch (Exception e1)
				{ /* ignore problems */
				}
			}
		}
	
		/**
		 * @param state
		 * @return
		 */
        public int state(int state, L2AirShipInstance _boat)
		{
			if (state < max)
			{
				L2AirShipPoint bp = _path.get(state);
				
				_boat._easi = new ExMoveToLocationAirShip(L2AirShipInstance.this,bp.x, bp.y, bp.z);
				// _boat.getTemplate().baseRunSpd = bp.speed1;
				boatSpeed = bp.speed1;
				_boat.moveAirShipToLocation(bp.x, bp.y, bp.z, bp.speed1);
				Collection<L2PcInstance> knownPlayers = _boat.getKnownList().getKnownPlayers().values();
				if (bp.time == 0)
				{
					bp.time = 1;
				}
				if (knownPlayers == null || knownPlayers.isEmpty())
					return bp.time;
				//synchronized (_boat.getKnownList().getKnownPlayers())
				{
					for (L2PcInstance player : knownPlayers)
					{
						player.sendPacket(_boat._easi);
					}
				}
				return bp.time;
			}
			else
			{
				return 0;
			}
		}
		
	}
	
	protected L2AirShipTrajet _t1;
	protected L2AirShipTrajet _t2;
	protected L2AirShipTrajet _t3;
	protected L2AirShipTrajet _t4;
	protected int _cycle = 0;
	protected int _runstate = 0;
	protected ExMoveToLocationAirShip _easi = null;
	
	private Map<Integer, L2PcInstance> _inAirShip;
	
	public L2AirShipInstance(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
		setAI(new L2CharacterAI(new AIAccessor()));
	}	
	
	@Override
    public void initKnownList()
    {
		setKnownList(new AirShipKnownList(this));
    }
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 */
	public void moveAirShipToLocation(int x, int y, int z, float speed)
	{
		final int curX = getX();
		final int curY = getY();
		
		// Calculate distance (dx,dy) between current position and destination
		final int dx = (x - curX);
		final int dy = (y - curY);
		double distance = Math.sqrt(dx * dx + dy * dy);
		
		if (Config.DEBUG)
			_airShiplog.fine("distance to target:" + distance);
		
		// Define movement angles needed
		// ^
		// | X (x,y)
		// | /
		// | /distance
		// | /
		// |/ angle
		// X ---------->
		// (curx,cury)
		
		double cos;
		double sin;
		sin = dy / distance;
		cos = dx / distance;
		// Create and Init a MoveData object
		MoveData m = new MoveData();
		
		// Caclulate the Nb of ticks between the current position and the
		// destination
		int ticksToMove = (int) (GameTimeController.TICKS_PER_SECOND * distance / speed);
		
		// Calculate and set the heading of the L2Character
		int heading = (int) (Math.atan2(-sin, -cos) * 10430.378350470452724949566316381);
		heading += 32768;
		getPosition().setHeading(heading);
		
		if (Config.DEBUG)
			_airShiplog.fine("dist:" + distance + "speed:" + speed + " ttt:"
			        + ticksToMove + " heading:" + heading);
		
		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z; // this is what was requested from client
		m._heading = 0; // initial value for coordinate sync
		m.onGeodataPathIndex = -1; // Initialize not on geodata path
		m._moveStartTime = GameTimeController.getGameTicks();
		
		if (Config.DEBUG)
			_airShiplog.fine("time to target:" + ticksToMove);
		
		// Set the L2Character _move object to MoveData object
		_move = m;
		
		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingObject(this);
	}
	
	class AirShipCaptain implements Runnable
	{
		private L2AirShipInstance _airShip;
		
		/**
		 * @param i
		 * @param instance
		 */
		public AirShipCaptain(L2AirShipInstance instance)
		{
			_airShip = instance;
		}
		
		public void run()
		{
			_airShip.begin();
		}
	}
	class AirShiprun implements Runnable
	{
		private int _state;
		
		private L2AirShipInstance _airShip;
		
		/**
		 * @param i
		 * @param instance
		 */
		public AirShiprun(int i, L2AirShipInstance instance)
		{
			_state = i;
			_airShip = instance;
		}
		
		public void run()
		{
			_airShip._easi = null;
			if (_airShip._cycle == 1)
			{
				int time = _airShip._t1.state(_state, _airShip);
				if (time == 0)
				{
					_airShip._cycle = 2;
					teleportAirShip(-167874,256731,-509,41035);
					AirShiprun asr = new AirShiprun(0, _airShip);
					ThreadPoolManager.getInstance().scheduleGeneral(asr, 5000);
				}
				else
					_state++;
			}
			else if (_airShip._cycle == 2)
			{
				int time = _airShip._t2.state(_state, _airShip);
				if (time == 0)
				{
					setIsInDock(true);
					for (L2PcInstance player : _passengers)
					{
						if (player == null)
							continue;
						oustPlayer(player);
					}
					airShipControllerShout(32607,true);
					AirShipCaptain asc = new AirShipCaptain(_airShip);
					ThreadPoolManager.getInstance().scheduleGeneral(asc, 60000);
				}
				else
					_state++;
			}
			else if (_airShip._cycle == 3)
			{
				int time = _airShip._t3.state(_state, _airShip);
				if (time == 0)
				{
					_airShip._cycle = 4;
					teleportAirShip(-157261,255664,221,64781);
					AirShiprun asr = new AirShiprun(0, _airShip);
					ThreadPoolManager.getInstance().scheduleGeneral(asr, 5000);

				}
				else
					_state++;
			}
			else if (_airShip._cycle == 4)
			{
				int time = _airShip._t4.state(_state, _airShip);
				if (time == 0)
				{
					setIsInDock(true);
					for (L2PcInstance player : _passengers)
					{
						if (player == null)
							continue;
						oustPlayer(player);
					}
					airShipControllerShout(32609,true);
					AirShipCaptain asc = new AirShipCaptain(_airShip);
					ThreadPoolManager.getInstance().scheduleGeneral(asc, 60000);
				}
				else
					_state++;
			}
			_airShip._runstate = _state;
		}
	}
	
	/**
	 * 
	 */
	public void evtArrived()
	{
		if (_runstate != 0)
		{
			AirShiprun asr = new AirShiprun(_runstate, this);
			ThreadPoolManager.getInstance().executeTask(asr);
		}
	}
	
	public void airShipControllerShout(int npcId, boolean isArraived)
	{
		String message = "";
		switch (npcId)
		{
			case 32607:
				if (isArraived)
					message = "The regurarly scheduled airship has arrived. It will depart for the Aden continent in 1 minute. ";
				else
					message = "The regurarly scheduled airship that flies to the Aden continent has departed.";
				break;
			case 32609:
				if (isArraived)
					message = "The regurarly scheduled airship has arrived. It will depart for the Gracia continent in 1 minute. ";
				else
					message = "The regurarly scheduled airship that flies to the Gracia continent has departed.";
				break;
			default:
				_log.warning("Invalid AirShipController npcId: " + npcId);
				return;
		}
		for (L2AirShipControllerInstance asci : AirShipManager.getInstance().getATCs())
			if (asci != null && asci.getNpcId() == npcId)
			{
				asci.setIsBoardAllowed(isArraived);
				asci.broadcastMessage(message);
			}
	}

	public ExMoveToLocationAirShip getAirShipInfo()
	{
		return _easi;
	}
	
	public void beginCycle()
	{
		AirShipCaptain asc = new AirShipCaptain( this);
		ThreadPoolManager.getInstance().scheduleGeneral(asc, 60000);
	}
	
	/**
	 * @param destination
	 * @param destination2
	 * @param destination3
	 */
	private int lastx = -1;
	private int lasty = -1;
	private boolean _isInDock = true;
	
	public void updatePeopleInTheAirShip(int x, int y, int z)
	{
		
		if (_inAirShip != null)
		{
			if ((lastx == -1) || (lasty == -1))
			{
				lastx = x;
				lasty = y;
			}
			else if ((x - lastx) * (x - lastx) + (y - lasty) * (y - lasty) > 2250000) // 1500 * 1500 =  2250000 
			{
				lastx = x;
				lasty = y;
			}
			for (int i = 0; i < _inAirShip.size(); i++)
			{
				L2PcInstance player = _inAirShip.get(i);
				if (player != null && player.isInAirShip())
				{
					if (player.getAirShip() == this)
					{
						// player.getKnownList().addKnownObject(this);
						player.getPosition().setXYZ(x, y, z);
						player.revalidateZone(false);
					}
				}
			}
		}
		
	}
	/**
	 * @param i
	 */
	public void begin()
	{
		_cycle++;
		if (_cycle == 1 || _cycle == 5)
		{
			_cycle = 1;
			setIsInDock(false);
			airShipControllerShout(32609,false);
			AirShiprun asr = new AirShiprun(0, this);
			ThreadPoolManager.getInstance().executeTask(asr);
		}
		else if (_cycle == 3)
		{
			setIsInDock(false);
			airShipControllerShout(32607,false);
			AirShiprun asr = new AirShiprun(0, this);
			ThreadPoolManager.getInstance().executeTask(asr);
		}
	}
	
	public void spawn()
	{
		Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
		_cycle = 0;
		beginCycle();
		if (knownPlayers == null || knownPlayers.isEmpty())
			return;
		ExAirShipInfo easi = new ExAirShipInfo(this);
		for (L2PcInstance player : knownPlayers)
			player.sendPacket(easi);
	}
	
	/**
	 * @param idWaypoint1
	 */
	public void setTrajet1(int idWaypoint1)
	{
		_t1 = new L2AirShipTrajet(idWaypoint1);
	}
	
	public void setTrajet2(int idWaypoint2)
	{
		_t2 = new L2AirShipTrajet(idWaypoint2);
	}
	
	public void setTrajet3(int idWaypoint3)
	{
		_t3 = new L2AirShipTrajet(idWaypoint3);
	}
	public void setTrajet4(int idWaypoint4)
	{
		_t4 = new L2AirShipTrajet(idWaypoint4);
	}
	
	public void setIsInDock(boolean val)
	{
		_isInDock = val;
	}
	public boolean isInDock()
	{
		return _isInDock;
	}
	public void onPlayerBoarding(L2PcInstance player)
	{
		// cannot board
		if (!isInDock()	|| _passengers.contains(player))
			return;
			
		_passengers.add(player);
		player.setAirShip(this);
		player.broadcastPacket(new ExGetOnAirShip(player, this));
		//player.sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE));
	}

	public void oustPlayer(L2PcInstance player)
	{
		int x,y,z;
		if (_cycle == 1 || _cycle == 4)
		{
			x = -149379;
			y = 255246;
			z = -80;
		}
		else
		{
			x = -186563;
			y = 243590;
			z = 2608;			
		}
		_passengers.remove(player);
		player.broadcastPacket(new ExGetOffAirShip(player, this, x ,y ,z));
		player.setAirShip(null);
	}
	
	public void teleportAirShip(int x, int y, int z,int heading)
	{
		teleToLocation(x, y, z, heading, false);
		for (L2PcInstance player : _passengers)
		{
			if (player == null)
				continue;
			player.sendPacket(new ExStopMoveAirShip(this));
			player.teleToLocation(x,y,z, heading,false);
			player.sendPacket(new ExAirShipInfo(this));
		}
	}
	@Override
	public void updateAbnormalEffect()
	{
	}
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	@Override
	public int getLevel()
	{
		return 0;
	}
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
	@Override
	public void setAI(L2CharacterAI newAI)
	{
		if (_ai == null)
			_ai = newAI;
	}
	public class AIAccessor extends L2Character.AIAccessor
	{
		@Override
		public void detachAI()
		{}
	}

    public int getSpeed1()
    {
	    return 300;
    }
    public int getSpeed2()
    {
	    return 4000;
    }
    
    @Override
    public void sendInfo(L2PcInstance activeChar)
    {
    	if(this != activeChar.getAirShip())
    	{
    		activeChar.sendPacket(new ExAirShipInfo(this));
    	}
    }

}