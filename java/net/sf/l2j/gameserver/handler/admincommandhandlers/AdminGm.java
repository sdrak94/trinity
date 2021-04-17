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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.datatables.AccessLevels;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class handles following admin commands:
 * - gm = turns gm mode on/off
 *
 * @version $Revision: 1.2.4.4 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminGm implements IAdminCommandHandler
{
private static Logger _log = Logger.getLogger(AdminGm.class.getName());
private static final String[] ADMIN_COMMANDS =
{
	"admin_gm"
};

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	
	if (command.equals("admin_gm"))
		handleGm(activeChar);
	
	return true;
}

public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}

private void handleGm(L2PcInstance activeChar)
{
	if (activeChar.isGM())
	{
		activeChar.setTurnedGMOff(activeChar.getAccessLevel().getLevel());
		
		GmListTable.getInstance().deleteGm(activeChar);
		activeChar.setAccessLevel(AccessLevels._userAccessLevelNum);
		activeChar.setNameColorsDueToPVP();
		activeChar.broadcastUserInfo();
		activeChar.sendMessage("You no longer have GM status. This is not permanent. Type //gm again to reenable your status.");
		
		_log.fine("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") turned his GM status off");
	}
	else
	{
		if (activeChar.getTurnedGMOff() > 0)
		{
			activeChar.setTurnedGMOff(0);
			_log.fine("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") turned his GM status ON");
			
			Connection con = null;
			
			try
			{
				// Retrieve the L2PcInstance from the characters table of the database
				con = L2DatabaseFactory.getInstance().getConnection();
				
				PreparedStatement statement = con.prepareStatement("select accesslevel from characters WHERE charId=?");
				statement.setInt(1, activeChar.getObjectId());
				ResultSet rset = statement.executeQuery();
				
				if (rset.next())
				{
					activeChar.setAccessLevel(rset.getInt("accesslevel"));
					
					if (activeChar.isGM())
					{
						activeChar.getAppearance().setNameColor(activeChar.getAccessLevel().getNameColor());
						activeChar.getAppearance().setTitleColor(activeChar.getAccessLevel().getTitleColor());
						activeChar.broadcastUserInfo();
						activeChar.sendMessage("You now have GM back");
						GmListTable.getInstance().addGm(activeChar, false);
					}
					else
					{
						_log.severe("L O L when trying to reenable gm "+activeChar.getName()+" got owned!");
					}
				}
				else
				{
					_log.severe("L O L when trying to reenable gm it doesn't work!");
				}
				
				rset.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed loading GM accesslevel", e);
			}
			finally
			{
				try { con.close(); } catch (Exception e) {}
			}
		}
	}
}
}
