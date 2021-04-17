/*package net.sf.l2j.gameserver.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.lib.Log;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class GMAudit
{
private static final Logger _log = Logger.getLogger(Log.class.getName());
public static final SimpleDateFormat _formatter = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");

public static void auditGMAction(L2PcInstance gm, String action, String param)
{
	String gm_name = gm.getAccountName() + " - " + gm.getName();
	String target = "null";
	
	L2Object targetChar = gm.getTarget();
	if (targetChar != null)
		target = targetChar.getName() + " - " + targetChar.getObjectId();
	
	auditGMAction(gm_name, action, target, param);
}

public static void auditGMAction(String gm_name, String action, String target, String param)
{
	String today = _formatter.format(new Date());
	
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("INSERT INTO audit_gm(gm_name, action, target, param, date) VALUES(?,?,?,?,?)");
		
		statement.setString(1, gm_name);
		statement.setString(2, action);
		statement.setString(3, target);
		statement.setString(4, param);
		statement.setString(5, today);
		
		statement.executeUpdate();
	}
	catch (Exception e)
	{
		_log.severe(e.getMessage());
	}
	finally { try { if (con != null) con.close(); } catch (SQLException e) { e.printStackTrace(); } }
}
}*/

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
package net.sf.l2j.gameserver.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;


public class GMAudit
{
	public static final SimpleDateFormat _formatter = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");

	static
	{
		new File("log/GMAudit").mkdirs();
	}
	private static final Logger _log = Logger.getLogger(GMAudit.class.getName());
	
	public static void auditGMAction(final String gmName, final String action, final String target, final String params)
	{
		
		final L2PcInstance player = L2World.getInstance().getPlayer(gmName.split(" ")[0]);
		if (player != null)
			return;
		//final File file = new File("../ls/lib/cache/log/GMAudit/" + gmName + ".txt");
		final File file = new File("log/GMAudit/" + gmName + ".txt");
		if (!file.exists())
			try
			{
				file.createNewFile();
			}
			catch (final IOException e)
			{
			}
		try (FileWriter save = new FileWriter(file, true))
		{
			//save.write(Util.formatDate(new Date(),"[" + "dd/MM/yyyy H:mm:ss") + "] - " + "[" + gmName + "]: " + action + " -> " + target + " >" + params + "\r\n");
		}
		catch (final IOException e)
		{
			_log.log(Level.SEVERE, "GMAudit for GM " + gmName + " could not be saved: ", e);
		}
		final File file2 = new File("log/error2.txt");
		if (!file2.exists())
			try
			{
				file2.createNewFile();
			}
			catch (final IOException e)
			{
			}
		try (PrintWriter out = new PrintWriter(new FileWriter(file2, true)))
		{
			out.write(Util.formatDate(new Date(), "dd/MM/yyyy H:mm:ss") + ">" + gmName + ">" + action + ">" + target + ">" + params + "\r\n");
		}
		catch (final IOException e)
		{
		}
	}
	
	public static void auditGMAction(final String gmName, final String action, final String target)
	{
		auditGMAction(gmName, action, target, "");
	}
}