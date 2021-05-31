/*
 * $Header: AdminTest.java, 25/07/2005 17:15:21 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 25/07/2005 17:15:21 $
 * $Revision: 1 $
 * $Log: AdminTest.java,v $
 * Revision 1  25/07/2005 17:15:21  luisantonioa
 * Added copyright notice
 *
 *
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

import java.io.File;

import javax.script.ScriptException;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.L2ScriptEngineManager;

public class AdminQuest implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_quest_reload",
		"admin_script_load"
	};
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.IAdminCommandHandler#useAdminCommand(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
	 */
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (activeChar == null)
			return false;
		
		// syntax will either be:
		//                           //quest_reload <id>
		//                           //quest_reload <questName>
		// The questName MUST start with a non-numeric character for this to work, 
		// regardless which of the two formats is used.
		// Example:  //quest_reload orc_occupation_change_1
		// Example:  //quest_reload chests
		// Example:  //quest_reload SagasSuperclass
		// Example:  //quest_reload 12
		if (command.startsWith("admin_quest_reload"))
		{
			String[] parts = command.split(" ");
			if (parts.length < 2)
			{
				activeChar.sendMessage("Syntax: //quest_reload <questFolder>.<questSubFolders...>.questName> or //quest_reload <id>");
			}
			else
			{
//				// try the first param as id
//				try
//				{
//					int questId = Integer.parseInt(parts[1]);
//					if (QuestManager.getInstance().reload(questId))
//					{
//						activeChar.sendMessage("Quest Reloaded Successfully.");
//					}
//					else
//					{
//						activeChar.sendMessage("Quest Reloaded Failed");
//					}
//				}
//				catch (NumberFormatException e)
//				{
//					if (QuestManager.getInstance().reload(parts[1]))
//					{
//						activeChar.sendMessage("Quest Reloaded Successfully.");
//					}
//					else
//					{
//						activeChar.sendMessage("Quest Reloaded Failed");
//					}
//				}
			}
		}
		// script load should NOT be used in place of reload.  If a script is already loaded
		// successfully, quest_reload ought to be used.  The script_load command should only
		// be used for scripts that failed to load altogether (eg. due to errors) or that 
		// did not at all exist during server boot.  Using script_load to re-load a previously
		// loaded script may cause unpredictable script flow, minor loss of data, and more.
		// This provides a way to load new scripts without having to reboot the server.
		else if (command.startsWith("admin_script_load"))
		{
			String[] parts = command.split(" ");
			if (parts.length < 2)
			{
				//activeChar.sendMessage("Example: //script_load <questFolder>/<questSubFolders...>/<filename>.<ext> ");
				activeChar.sendMessage("Example: //script_load quests/SagasSuperclass/__init__.py");
			}
			else
			{
				File file = new File(L2ScriptEngineManager.SCRIPT_FOLDER, parts[1]);
				if (file.isFile())
				{
					try
					{
						L2ScriptEngineManager.getInstance().executeScript(file);
					}
					catch (ScriptException e)
					{
						activeChar.sendMessage("Failed loading: " + parts[1]);
						L2ScriptEngineManager.getInstance().reportScriptFileError(file, e);
					}
					catch (Exception e)
					{
						activeChar.sendMessage("Failed loading: " + parts[1]);
					}
				}
				else
				{
					activeChar.sendMessage("File Not Found: " + parts[1]);
				}
			}
			
		}
		return true;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.IAdminCommandHandler#getAdminCommandList()
	 */
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
}
