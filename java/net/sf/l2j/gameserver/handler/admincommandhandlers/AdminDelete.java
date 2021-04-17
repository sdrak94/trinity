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

import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;

/**
 * This class handles following admin commands: - delete = deletes target
 *
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2005/04/11 10:05:56 $
 */
public class AdminDelete implements IAdminCommandHandler
{
private static final String[] ADMIN_COMMANDS =
{
	"admin_delete"
};

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	if (command.equals("admin_delete"))
		handleDelete(activeChar);
	return true;
}

public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}

// TODO: add possibility to delete any L2Object (except L2PcInstance)
private void handleDelete(L2PcInstance activeChar)
{
	L2Object obj = activeChar.getTarget();
	if (obj instanceof L2Npc)
	{
		boolean tempSpawn = false;
		L2Npc target = (L2Npc) obj;
		if (target.isTempSpawn())
			tempSpawn = true;
		
		L2Spawn spawn = target.getSpawn();
		
		if (spawn != null)
		{
			spawn.stopRespawn();
			
			if (!tempSpawn)
			{
				if (RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcid()))
					RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
				else
					SpawnTable.getInstance().deleteSpawn(spawn, true);
			}
		}
		
		target.deleteMe();
		
		activeChar.sendMessage("Deleted " + target.getName() +"("+target.getNpcId()+ ") as " + target.getObjectId());
	}
	else if (obj instanceof L2StaticObjectInstance)
	{
		L2WorldRegion region = obj.getWorldRegion();
		obj.decayMe();
		
		if (region != null)
			region.removeVisibleObject(obj);
		
		try
		{
			obj.getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
		}
		L2World.getInstance().removeObject(obj);
	}
	else if (obj instanceof L2FenceInstance)
	{
		L2FenceInstance fence = (L2FenceInstance)obj;
		
		L2WorldRegion region = fence.getWorldRegion();
		fence.decayMe();
		
		if (region != null)
			region.removeVisibleObject(fence);
		
		try
		{
			fence.getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
		}
		L2World.getInstance().removeObject(fence);
	}
	else
		activeChar.sendMessage("Incorrect target.");
}
}
