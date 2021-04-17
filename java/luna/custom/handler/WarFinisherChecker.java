/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package luna.custom.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ClanTable;

public class WarFinisherChecker
{
	private static WarFinisherChecker	_instance		= null;
	private static final Logger	_log			= Logger.getLogger(WarFinisherChecker.class.getName());
	public static final String	NAME			= "two_sided_wars_checker";
	static Map<Integer, Long>	_times			= new FastMap<Integer, Long>();
	List<Integer>				ntoBeDeleted	= new ArrayList<Integer>();
	
	public static WarFinisherChecker getInstance()
	{
		if (_instance == null)
			_instance = new WarFinisherChecker();
		return _instance;
	}
	
	public void init()
	{
		System.out.println("-- Launching ClanWarFinisher");
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Checker(), 1000, 2700000);
	}
	
	class Checker implements Runnable
	{
		public void run()
		{
			checkForTimers();
			checkForEndingWars();
			System.out.println("ClanWarFinisher checking now: "+  LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy")));
		}
	}
	
	class EndWar implements Runnable
	{
		int _uniqueWarId;
		
		public EndWar(int uniqueWarId)
		{
			_uniqueWarId = uniqueWarId;
		}
		
		public void run()
		{
			ClanTable.getInstance().endTwoSidedWarByUniqueWarId(_uniqueWarId);
		}
	}
	
	private void checkForTimers()
	{
		_times.clear();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT warUniqueId,warfinishtime from clan_wars_two_sided");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				_times.put(rset.getInt(1), rset.getLong(2));
			}
			rset.next();
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error checking for clan wars finish time data.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	private void checkForEndingWars()
	{
		long currentTime;
		long timerTime;
		long hoursInMil = 300000;
		for (int UniqueWarId : _times.keySet())
		{
			timerTime = _times.get(UniqueWarId);
			currentTime = System.currentTimeMillis();
			System.out.println("K: " + UniqueWarId + " V: " + timerTime + " ");
			if (currentTime > timerTime)
			{
				ClanTable.getInstance().endTwoSidedWarByUniqueWarId(UniqueWarId);
			}
			else if (currentTime > timerTime + hoursInMil)
			{
				continue;
			}
			else if (currentTime + hoursInMil < timerTime)
			{
				long deleteTime = timerTime - currentTime;
				ThreadPoolManager.getInstance().scheduleGeneral(new EndWar(UniqueWarId), deleteTime);
			}
		}
	}
}
