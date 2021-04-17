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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.AccessLevels;

/**
 * @author FBIagent<br>
 */
public class L2AccessLevel
{
	/** The logger<br> */
	private static Logger _log = Logger.getLogger( L2AccessLevel.class.getName() );
	/** The access level<br> */
	private int _accessLevel = 0;
	/** The access level name<br> */
	private String _name = null;
	/** Child access levels */
	L2AccessLevel[] _childsAccessLevel = null;
	/** Child access levels */
	private String _childs = null;
	/** The name color for the access level<br> */
	private int _nameColor = 0;
	/** The title color for the access level<br> */
	private int _titleColor = 0;
	/** Flag to determine if the access level has gm access<br> */
	private boolean _isGm = false;
	/** Flag for peace zone attack */
	private boolean _allowPeaceAttack = false;
	/** Flag for fixed res */
	private boolean _allowFixedRes = false;
	/** Flag for transactions */
	private boolean _allowTransaction = false;
	/** Flag for AltG commands */
	private boolean _allowAltG = false;
	/** Flag to give damage */
	private boolean _giveDamage = false;
	/** Flag to take aggro */
	private boolean _takeAggro = false;
	/** Flag to gain exp in party */
	private boolean _gainExp = false;

	/**
	 * Initializes members<br><br>
	 * 
	 * @param accessLevel as int<br>
	 * @param name as String<br>
	 * @param nameColor as int<br>
	 * @param titleColor as int<br>
	 * @param childs as String<br>
	 * @param isGm as boolean<br>
	 * @param allowPeaceAttack as boolean<br>
	 * @param allowFixedRes as boolean<br>
	 * @param allowTransaction as boolean<br>
	 * @param allowAltG as boolean<br>
	 * @param giveDamage as boolean<br>
	 * @param takeAggro as boolean<br>
	 * @param gainExp as boolean<br>
	 */
	public L2AccessLevel(int accessLevel, String name, int nameColor, int titleColor, String childs, boolean isGm,
			boolean allowPeaceAttack, boolean allowFixedRes, boolean allowTransaction, boolean allowAltG, boolean giveDamage, boolean takeAggro, boolean gainExp)
	{
		_accessLevel = accessLevel;
		_name = name;
		_nameColor = nameColor;
		_titleColor = titleColor;
		_childs = childs;
		_isGm = isGm;
		_allowPeaceAttack = allowPeaceAttack;
		_allowFixedRes = allowFixedRes;
		_allowTransaction = allowTransaction;
		_allowAltG = allowAltG; 
		_giveDamage = giveDamage;
		_takeAggro = takeAggro;
		_gainExp = gainExp;
	}
	
	

	/**
	 * Returns the access level<br><br>
	 * 
	 * @return int: access level<br>
	 */
	public int getLevel()
	{
		return _accessLevel;
	}

	/**
	 * Returns the access level name<br><br>
	 * 
	 * @return String: access level name<br>
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * Returns the name color of the access level<br><br>
	 * 
	 * @return int: the name color for the access level<br>
	 */
	public int getNameColor()
	{
		return _nameColor;
	}

	/**
	 * Returns the title color color of the access level<br><br>
	 * 
	 * @return int: the title color for the access level<br>
	 */
	public int getTitleColor()
	{
		return _titleColor;
	}

	/**
	 * Retuns if the access level has gm access or not<br><br>
	 * 
	 * @return boolean: true if access level have gm access, otherwise false<br>
	 */
	public boolean isGm()
	{
		return _isGm;
	}

	/**
	 * Returns if the access level is allowed to attack in peace zone or not<br><br>
	 * 
	 * @return boolean: true if the access level is allowed to attack in peace zone, otherwise false<br>
	 */
	public boolean allowPeaceAttack()
	{
		return _allowPeaceAttack;
	}

	/**
	 * Retruns if the access level is allowed to use fixed res or not<br><br>
	 * 
	 * @return: true if the access level is allowed to use fixed res, otherwise false<br>
	 */
	public boolean allowFixedRes()
	{
		return _allowFixedRes;
	}

	/**
	 * Returns if the access level is allowed to perform transactions or not<br><br>
	 *  
	 * @return boolean: true if access level is allowed to perform transactions, otherwise false<br>
	 */
	public boolean allowTransaction()
	{
		return _allowTransaction;
	}

	/**
	 * Returns if the access level is allowed to use AltG commands or not<br><br>
	 *  
	 * @return boolean: true if access level is allowed to use AltG commands, otherwise false<br>
	 */
	public boolean allowAltG()
	{
		return _allowAltG;
	}

	/**
	 * Returns if the access level can give damage or not<br><br>
	 * 
	 * @return boolean: true if the access level can give damage, otherwise false<br>
	 */
	public boolean canGiveDamage()
	{
		return _giveDamage;
	}

	/**
	 * Returns if the access level can take aggro or not<br><br>
	 * 
	 * @return boolean: true if the access level can take aggro, otherwise false<br>
	 */
	public boolean canTakeAggro()
	{
		return _takeAggro;
	}

	/**
	 * Returns if the access level can gain exp or not<br><br>
	 * 
	 * @return boolean: true if the access level can gain exp, otherwise false<br>
	 */
	public boolean canGainExp()
	{
		return _gainExp;
	}

	/**
	 * Returns if the access level contains allowedAccess as child<br><br>
	 *
	 * @param accessLevel as AccessLevel<br><br>
	 * 
	 * @return boolean: true if a child access level is equals to allowedAccess, otherwise false<br>
	 */
	public boolean hasChildAccess(L2AccessLevel accessLevel)
	{
		if (_childsAccessLevel == null)
		{
			if (_childs == null)
				return false;
			
			setChildAccess(_childs);
			for (L2AccessLevel childAccess : _childsAccessLevel)
			{
				if (childAccess != null && (childAccess.getLevel() == accessLevel.getLevel() || childAccess.hasChildAccess(accessLevel)))
					return true;
			}
		}
		else
		{
			for (L2AccessLevel childAccess : _childsAccessLevel)
			{
				if (childAccess != null && (childAccess.getLevel() == accessLevel.getLevel() || childAccess.hasChildAccess(accessLevel)))
					return true;
			}
		}
		return false;
	}
	
	private void setChildAccess(String childs)
	{
		String[] childsSplit = childs.split(";");

		_childsAccessLevel = new L2AccessLevel[childsSplit.length];

		for (int i = 0;i < childsSplit.length;++ i)
		{
			L2AccessLevel accessLevelInst = AccessLevels.getInstance().getAccessLevel(Integer.parseInt(childsSplit[i]));

			if (accessLevelInst == null)
			{
				_log.warning("AccessLevel: Undefined child access level " + childsSplit[i]);
				continue;
			}

			if (accessLevelInst.hasChildAccess(this))
			{
				_log.warning("AccessLevel: Child access tree overlapping for " + _name + " and " + accessLevelInst.getName());
				continue;
			}

			_childsAccessLevel[i] = accessLevelInst;
		}
	}
}