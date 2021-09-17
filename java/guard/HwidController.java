package guard;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.L2DatabaseFactory;

public class HwidController
{
	private ConcurrentHashMap<String, ArrayList<ConcurrentHashMap<String, HashMap<String, String>>>>	_hwids					= new ConcurrentHashMap<>();
	private static final String																			SELECT_HWIDS			= "SELECT * FROM HWID_CONTROLLER";
	private static final String																			SELECT_HWIDS_SPECIFIC	= "SELECT * FROM HWID_CONTROLLER WHERE account_name = ? AND char_name = ? AND hwid = ? ";
	private static final String																			STORE_HWID				= "REPLACE INTO HWID_CONTROLLER values (?,?,?,?)";
	
	public void collectHwids()
	{
		_hwids.clear();
		try (Connection con = L2DatabaseFactory.getConnectionS(); PreparedStatement st = con.prepareStatement(SELECT_HWIDS))
		{
			try (ResultSet rs = st.executeQuery())
			{
				while (rs.next())
				{
					ConcurrentHashMap<String, HashMap<String, String>> hwidInfo = new ConcurrentHashMap<String, HashMap<String, String>>();
					HashMap<String, String> charInfo = new HashMap<String, String>();
					ArrayList<ConcurrentHashMap<String, HashMap<String, String>>> importData = new ArrayList<ConcurrentHashMap<String, HashMap<String, String>>>();
					String account = rs.getString("account_name");
					String char_name = rs.getString("char_name");
					String hwid = rs.getString("hwid");
					Date datetime = rs.getDate("date");
					charInfo.put(char_name, datetime.toString());
					hwidInfo.put(hwid, charInfo);
					importData.add(hwidInfo);
					if (_hwids.containsKey(account))
					{
						_hwids.get(account).addAll(importData);
					}
					else
					{
						_hwids.put(account, importData);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void storeHwid(String account, String charname, String hwid, long date)
	{
		try (Connection con = L2DatabaseFactory.getConnectionS(); PreparedStatement st = con.prepareStatement(SELECT_HWIDS_SPECIFIC))
		{
			st.setString(1, account);
			st.setString(2, charname);
			st.setString(3, hwid);
			try (ResultSet rs = st.executeQuery())
			{
				if (rs.next())
				{
					PreparedStatement stU = con.prepareStatement("UPDATE HWID_CONTROLLER SET date = ? WHERE account_name = ? AND char_name = ? AND hwid = ?");
					stU.setTimestamp(1, new Timestamp(date));
					stU.setString(2, account);
					stU.setString(3, charname);
					stU.setString(4, hwid);
					stU.executeUpdate();
				}
				else
				{
					PreparedStatement stU = con.prepareStatement(STORE_HWID);
					stU.setString(1, account);
					stU.setString(2, charname);
					stU.setString(3, hwid);
					stU.setTimestamp(4, new Timestamp(date));
					stU.executeUpdate();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static class InstanceHolder
	{
		private static final HwidController _instance = new HwidController();
	}
	
	public static HwidController getInstance()
	{
		return InstanceHolder._instance;
	}
}
