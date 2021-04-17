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
package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.datatables.AccessLevels;

/**
 * @author FBIagent<br>
 */
public class L2AdminCommandAccessRight
{
	/** The admin command<br> */
	private String _adminCommand = null;
	/** The access levels which can use the admin command<br> */
	private L2AccessLevel[] _accessLevels = null;

	/**
	 * Initialized members
	 * 
	 * @param adminCommand as String
	 * @param accessLevels as String
	 */
	public L2AdminCommandAccessRight(String adminCommand, String accessLevels)
	{
		_adminCommand = adminCommand;
		
		String[] accessLevelsSplit = accessLevels.split(",");
		int numLevels = accessLevelsSplit.length;
		
		_accessLevels = new L2AccessLevel[numLevels];
		
		for (int i = 0; i < numLevels; ++i)
		{
			try
			{
				_accessLevels[i] = AccessLevels.getInstance().getAccessLevel(Integer.parseInt(accessLevelsSplit[i]));
			}
			catch (NumberFormatException nfe)
			{
				_accessLevels[i] = null;
			}
		}
	}

	/**
	 * Returns the admin command the access right belongs to<br><br>
	 * 
	 * @return String: the admin command the access right belongs to<br>
	 */
	public String getAdminCommand()
	{
		return _adminCommand;
	}

	/**
	 * Checks if the given characterAccessLevel is allowed to use the admin command which belongs to this access right<br><br>
	 * 
	 * @param characterAccessLevel<br><br>
	 * 
	 * @return boolean: true if characterAccessLevel is allowed to use the admin command which belongs to this access right, otherwise false<br>
	 */
	public boolean hasAccess(L2AccessLevel characterAccessLevel)
	{
		for (int i = 0; i < _accessLevels.length; ++i)
		{
			L2AccessLevel accessLevel = _accessLevels[i];
			
			if (accessLevel != null
					&& (accessLevel.getLevel() == characterAccessLevel.getLevel() || characterAccessLevel.hasChildAccess(accessLevel)))
				return true;
		}
		
		return false;
	}
}