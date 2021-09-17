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

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2DoorAI;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.knownlist.DoorKnownList;
import net.sf.l2j.gameserver.model.actor.stat.DoorStat;
import net.sf.l2j.gameserver.model.actor.status.DoorStatus;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.DoorStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StaticObject;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2CharTemplate;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.util.StringUtil;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 */
public class L2DoorInstance extends L2Character
{
protected static final Logger log = Logger.getLogger(L2DoorInstance.class.getName());

/** The castle index in the array of L2Castle this L2NpcInstance belongs to */
private int _castleIndex = -2;
private int _mapRegion = -1;
/** The fort index in the array of L2Fort this L2NpcInstance belongs to */
private int _fortIndex = -2;

// when door is closed, the dimensions are
private int _rangeXMin = 0;
private int _rangeYMin = 0;
private int _rangeZMin = 0;
private int _rangeXMax = 0;
private int _rangeYMax = 0;
private int _rangeZMax = 0;

// these variables assist in see-through calculation only
private int _A = 0;
private int _B = 0;
private int _C = 0;
private int _D = 0;

protected final int _doorId;
protected final String _name;
private boolean _open;
private boolean _isCommanderDoor;
private final boolean _unlockable;

private ClanHall _clanHall;

protected int _autoActionDelay = -1;
private ScheduledFuture<?> _autoActionTask;

/** This class may be created only by L2Character and only for AI */
public class AIAccessor extends L2Character.AIAccessor
{
protected AIAccessor()
{
}

@Override
public L2DoorInstance getActor()
{
	return L2DoorInstance.this;
}

@Override
public void moveTo(int x, int y, int z, int offset)
{
}

@Override
public void moveTo(int x, int y, int z)
{
}

@Override
public void stopMove(L2CharPosition pos)
{
}

@Override
public void doAttack(L2Character target)
{
}

@Override
public void doCast(L2Skill skill)
{
}
}

@Override
public L2CharacterAI getAI()
{
	L2CharacterAI ai = _ai; // copy handle
	if (ai == null)
	{
		synchronized (this)
		{
			if (_ai == null)
				_ai = new L2DoorAI(new AIAccessor());
			return _ai;
		}
	}
	return ai;
}

class CloseTask implements Runnable
{
public void run()
{
	try
	{
		onClose();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "", e);
	}
}
}

/**
 * Manages the auto open and closing of a door.
 */
class AutoOpenClose implements Runnable
{
public void run()
{
	try
	{
		String doorAction;
		
		if (!getOpen())
		{
			doorAction = "opened";
			openMe();
		}
		else
		{
			doorAction = "closed";
			closeMe();
		}
		
		if (Config.DEBUG)
			_log.info("Auto " + doorAction + " door ID " + _doorId + " (" + _name + ") for " + (_autoActionDelay / 60000) + " minute(s).");
	}
	catch (Exception e)
	{
		_log.warning("Could not auto open/close door ID " + _doorId + " (" + _name + ")");
	}
}
}

/**
 */
public L2DoorInstance(int objectId, L2CharTemplate template, int doorId, String name, boolean unlockable)
{
	super(objectId, template);
	setIsInvul(false);
	_doorId = doorId;
	_name = name;
	_unlockable = unlockable;
}

@Override
public final DoorKnownList getKnownList()
{
	return (DoorKnownList) super.getKnownList();
}

@Override
public void initKnownList()
{
	setKnownList(new DoorKnownList(this));
}

@Override
public final DoorStat getStat()
{
	return (DoorStat) super.getStat();
}

@Override
public void initCharStat()
{
	setStat(new DoorStat(this));
}

@Override
public final DoorStatus getStatus()
{
	return (DoorStatus) super.getStatus();
}

@Override
public void initCharStatus()
{
	setStatus(new DoorStatus(this));
}

public final boolean isUnlockable()
{
	return _unlockable;
}

@Override
public final int getLevel()
{
	return 1;
}

/**
 * @return Returns the doorId.
 */
public int getDoorId()
{
	return _doorId;
}

/**
 * @return Returns the open.
 */
public boolean getOpen()
{
	return _open;
}

/**
 * @param open The open to set.
 */
public void setOpen(boolean open)
{
	_open = open;
}

/**
 * @param val Used for Fortresses to determine if doors can be attacked during siege or not
 */
public void setIsCommanderDoor(boolean val)
{
	_isCommanderDoor = val;
}

/**
 * @return Doors that cannot be attacked during siege
 * these doors will be auto opened if u take control of all commanders buildings
 */
public boolean getIsCommanderDoor()
{
	return _isCommanderDoor;
}

/**
 * Sets the delay in milliseconds for automatic opening/closing
 * of this door instance.
 * <BR>
 * <B>Note:</B> A value of -1 cancels the auto open/close task.
 *
 * @param int actionDelay
 */
public void setAutoActionDelay(int actionDelay)
{
	if (_autoActionDelay == actionDelay)
		return;
	
	if (actionDelay > -1)
	{
		AutoOpenClose ao = new AutoOpenClose();
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(ao, actionDelay, actionDelay);
	}
	else
	{
		if (_autoActionTask != null)
			_autoActionTask.cancel(false);
	}
	
	_autoActionDelay = actionDelay;
}

public int getDamage()
{
	int dmg = 6 - (int) Math.ceil(getCurrentHp() / getMaxHp() * 6);
	if (dmg > 6)
		return 6;
	if (dmg < 0)
		return 0;
	return dmg;
}

public final Castle getCastle()
{
	if (_castleIndex < 0)
		_castleIndex = CastleManager.getInstance().getCastleIndex(this);
	if (_castleIndex < 0)
		return null;
	return CastleManager.getInstance().getCastles().get(_castleIndex);
}

public final Fort getFort()
{
	if (_fortIndex < 0)
		_fortIndex = FortManager.getInstance().getFortIndex(this);
	if (_fortIndex < 0)
		return null;
	return FortManager.getInstance().getForts().get(_fortIndex);
}

public void setClanHall(ClanHall clanhall)
{
	_clanHall = clanhall;
}

public ClanHall getClanHall()
{
	return _clanHall;
}

public boolean isEnemy()
{
	if (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress())
		return true;
	if (getFort() != null && getFort().getFortId() > 0 && getFort().getSiege().getIsInProgress() && !getIsCommanderDoor())
		return true;
	return false;
}

@Override
public boolean isAutoAttackable(L2Character attacker)
{
	if (getInstanceId() == FOS.SIEGE_EVENT_INSTANCE_ID)
	{
		if (FOS.isDoorAttackable(getDoorId(), attacker))
			return true;

		if (NewFOS.isDoorAttackable(getDoorId(), attacker))
			return true;
		return false;
	}
	
	if (isUnlockable() && getFort() == null)
		return true;
	
	// Doors can`t be attacked by NPCs
	if (!(attacker instanceof L2Playable))
		return false;
	
	// Attackable  only during siege by everyone (not owner)
	final boolean isCastle = (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress());
	final boolean isFort = (getFort() != null && getFort().getFortId() > 0 && getFort().getSiege().getIsInProgress() && !getIsCommanderDoor());
	
	if (isFort)
	{
		final L2Clan clan = attacker.getActingPlayer().getClan();
		
		if (clan != null && getFort().getSiege().checkIsAttacker(clan))
			return true;
	}
	else if (isCastle)
	{
		final L2Clan clan = attacker.getActingPlayer().getClan();
		
		if (clan != null && getCastle().getSiege().checkIsAttacker(clan))
			return true;
	}
	
	return false;
}

public boolean isAttackable(L2Character attacker)
{
	return isAutoAttackable(attacker);
}

@Override
public boolean isAttackable()
{
	return true;
}

@Override
public void updateAbnormalEffect()
{
}

public int getDistanceToWatchObject(L2Object object)
{
	if (!(object instanceof L2PcInstance))
		return 0;
	return 2000;
}

/**
 * Return the distance after which the object must be remove from _knownObject according to the type of the object.<BR><BR>
 *
 * <B><U> Values </U> :</B><BR><BR>
 * <li> object is a L2PcInstance : 4000</li>
 * <li> object is not a L2PcInstance : 0 </li><BR><BR>
 *
 */
public int getDistanceToForgetObject(L2Object object)
{
	if (!(object instanceof L2PcInstance))
		return 0;
	
	return 4000;
}

/**
 * Return null.<BR><BR>
 */
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
public void onAction(L2PcInstance player)
{
	if (player == null)
		return;
	
	player.sendPacket(ActionFailed.STATIC_PACKET);
	
	// Check if the L2PcInstance is confused
	if (player.isOutOfControl())
		return;
	
	// Aggression target lock effect
	if (player.isLockedTarget() && player.getLockedTarget() != this)
	{
		player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
		return;
	}
	
	// Check if the L2PcInstance already target the L2NpcInstance
	if (this != player.getTarget())
	{
		// Set the target of the L2PcInstance player
		player.setTarget(this);
		
		// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
		MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
		player.sendPacket(my);
		
		StaticObject su = new StaticObject(this, false);
		
		// send HP amount if doors are inside castle/fortress zone
		// TODO: needed to be added here doors from conquerable clanhalls
		if ((getCastle() != null && getCastle().getCastleId() > 0) || (getFort() != null && getFort().getFortId() > 0 && !getIsCommanderDoor()))
			su = new StaticObject(this, true);
		player.sendPacket(su);
		
		// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
		player.sendPacket(new ValidateLocation(this));
	}
	else
	{
		if (isAutoAttackable(player))
		{
			if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth difference might need some tweaking
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
		}
		else if (player.getClan() != null && getClanHall() != null && player.getClanId() == getClanHall().getOwnerId())
		{
			if (!isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, false, false))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				player.gatesRequest(this);
				if (!getOpen())
				{
					player.sendPacket(new ConfirmDlg(1140));
				}
				else
				{
					player.sendPacket(new ConfirmDlg(1141));
				}
			}
		}
		
		else if (player.getClan() != null && getFort() != null && player.getClan() == getFort().getOwnerClan())
		{
			if (getFort().getSiege() != null && getFort().getSiege().getPreventDoorsFortSiege())
			{
				return;
			}
			if (!isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, false, false))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				player.gatesRequest(this);
				if (!getOpen())
				{
					player.sendPacket(new ConfirmDlg(1140));
				}
				else
				{
					player.sendPacket(new ConfirmDlg(1141));
				}
			}
		}
		else if (player.getClan() != null && getCastle() != null && player.getClanId() == getCastle().getOwnerId())
		{
			if (!isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, false, false))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				player.gatesRequest(this);
				if (!getOpen())
				{
					player.sendPacket(new ConfirmDlg(1140));
				}
				else
				{
					player.sendPacket(new ConfirmDlg(1141));
				}
			}
		}
		else if (getInstanceId() == FOS.SIEGE_EVENT_INSTANCE_ID)
		{
			if (!FOS.isDoorAttackable(getDoorId(), player))
			{
				if (!isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, false, false))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					player.gatesRequest(this);
					
					if (!getOpen())
					{
						player.sendPacket(new ConfirmDlg(1140));
					}
					else
					{
						player.sendPacket(new ConfirmDlg(1141));
					}
				}
			}
		}
	}
}

@Override
public void onActionShift(L2GameClient client)
{
	final L2PcInstance player = client.getActiveChar();
	
	if (player == null)
		return;
	
	player.sendPacket(ActionFailed.STATIC_PACKET);
	
	if (player.isGM())
	{
		player.setTarget(this);
		MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel());
		player.sendPacket(my);
		
		StaticObject su = new StaticObject(this, false);
		
		// send HP amount if doors are inside castle/fortress zone
		// TODO: needed to be added here doors from conquerable clanhalls
		if ((getCastle() != null && getCastle().getCastleId() > 0) || (getFort() != null && getFort().getFortId() > 0 && !getIsCommanderDoor()))
			su = new StaticObject(this, true);
		
		player.sendPacket(su);
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		final String html1 = StringUtil.concat(
				"<html><body><table border=0>" +
						"<tr><td>S.Y.L. Says:</td></tr>" +
						"<tr><td>Current HP  ",
						String.valueOf(getCurrentHp()),
						"</td></tr>" +
								"<tr><td>Max HP      ",
								String.valueOf(getMaxHp()),
								"</td></tr>" +
										"<tr><td>Max X       ",
										String.valueOf(getXMax()),
										"</td></tr>" +
												"<tr><td>Max Y       ",
												String.valueOf(getYMax()),
												"</td></tr>" +
														"<tr><td>Max Z       ",
														String.valueOf(getZMax()),
														"</td></tr>" +
																"<tr><td>Min X       ",
																String.valueOf(getXMin()),
																"</td></tr>" +
																		"<tr><td>Min Y       ",
																		String.valueOf(getYMin()),
																		"</td></tr>" +
																				"<tr><td>Min Z       ",
																				String.valueOf(getZMin()),
																				"</td></tr>" +
																						"<tr><td>Object ID:  ",
																						String.valueOf(getObjectId()),
																						"</td></tr>" +
																								"<tr><td>Door ID: <br>",
																								String.valueOf(getDoorId()),
																								"</td></tr>" +
																										"<tr><td><br></td></tr>" +
																										"<tr><td>Class: ",
																										getClass().getName(),
																										"</td></tr>" +
																												"<tr><td><br></td></tr>" +
																												"</table>" +
																												"<table><tr>" +
																												"<td><button value=\"Open\" action=\"bypass -h admin_open ",
																												String.valueOf(getDoorId()),
																												"\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" +
																														"<td><button value=\"Close\" action=\"bypass -h admin_close ",
																														String.valueOf(getDoorId()),
																														"\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" +
																																"<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" +
																																"<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" +
																																"</tr></table></body></html>"
				);
		html.setHtml(html1);
		player.sendPacket(html);
	}
	else
	{
		// Check if the L2PcInstance is confused
		if (player.isOutOfControl())
			return;
		
		// Aggression target lock effect
		if (player.isLockedTarget() && player.getLockedTarget() != this)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
			return;
		}
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			
			StaticObject su = new StaticObject(this, false);
			
			// send HP amount if doors are inside castle/fortress zone
			// TODO: needed to be added here doors from conquerable clanhalls
			if ((getCastle() != null && getCastle().getCastleId() > 0) || (getFort() != null && getFort().getFortId() > 0 && !getIsCommanderDoor()))
				su = new StaticObject(this, true);
			
			player.sendPacket(su);
			
			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (player.getClan() != null && getClanHall() != null && player.getClanId() == getClanHall().getOwnerId())
			{
				if (isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, false, false))
				{
					player.gatesRequest(this);
					
					if (!getOpen())
					{
						player.sendPacket(new ConfirmDlg(1140));
					}
					else
					{
						player.sendPacket(new ConfirmDlg(1141));
					}
				}
			}
			else if (player.getClan() != null && getFort() != null && player.getClan() == getFort().getOwnerClan() && isUnlockable() && !getFort().getSiege().getIsInProgress())
			{
				if (isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, false, false))
				{
					player.gatesRequest(this);
					
					if (!getOpen())
					{
						player.sendPacket(new ConfirmDlg(1140));
					}
					else
					{
						player.sendPacket(new ConfirmDlg(1141));
					}
				}
			}
		}
	}
}

@Override
public void broadcastStatusUpdate()
{
	Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
	if (knownPlayers == null || knownPlayers.isEmpty())
		return;
	
	StaticObject su = new StaticObject(this, false);
	DoorStatusUpdate dsu = new DoorStatusUpdate(this);
	
	//synchronized (getKnownList().getKnownPlayers())
	{
		for (L2PcInstance player : knownPlayers)
		{
			if ((getCastle() != null && getCastle().getCastleId() > 0) || (getFort() != null && getFort().getFortId() > 0 && !getIsCommanderDoor()))
				su = new StaticObject(this, true);
			
			player.sendPacket(su);
			player.sendPacket(dsu);
		}
	}
}

public void onOpen()
{
	ThreadPoolManager.getInstance().scheduleGeneral(new CloseTask(), 60000);
}

public void onClose()
{
	closeMe();
}

public final void closeMe()
{
	setOpen(false);
	broadcastStatusUpdate();
}

public final void openMe()
{
	setOpen(true);
	broadcastStatusUpdate();
}

@Override
public String toString()
{
	return "door " + _doorId;
}

public String getDoorName()
{
	return _name;
}

public int getXMin()
{
	return _rangeXMin;
}

public int getYMin()
{
	return _rangeYMin;
}

public int getZMin()
{
	return _rangeZMin;
}

public int getXMax()
{
	return _rangeXMax;
}

public int getYMax()
{
	return _rangeYMax;
}

public int getZMax()
{
	return _rangeZMax;
}

public void setRange(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax)
{
	_rangeXMin = xMin;
	_rangeYMin = yMin;
	_rangeZMin = zMin;
	
	_rangeXMax = xMax;
	_rangeYMax = yMax;
	_rangeZMax = zMax;
	
	_A = _rangeYMax * (_rangeZMax - _rangeZMin) + _rangeYMin * (_rangeZMin - _rangeZMax);
	_B = _rangeZMin * (_rangeXMax - _rangeXMin) + _rangeZMax * (_rangeXMin - _rangeXMax);
	_C = _rangeXMin * (_rangeYMax - _rangeYMin) + _rangeXMin * (_rangeYMin - _rangeYMax);
	_D = -1 * (_rangeXMin * (_rangeYMax * _rangeZMax - _rangeYMin * _rangeZMax) + _rangeXMax * (_rangeYMin * _rangeZMin - _rangeYMin * _rangeZMax) + _rangeXMin * (_rangeYMin * _rangeZMax - _rangeYMax * _rangeZMin));
}

public int getMapRegion()
{
	return _mapRegion;
}

public void setMapRegion(int region)
{
	_mapRegion = region;
}

public Collection<L2SiegeGuardInstance> getKnownSiegeGuards()
{
	FastList<L2SiegeGuardInstance> result = new FastList<L2SiegeGuardInstance>();
	
	Collection<L2Object> objs = getKnownList().getKnownObjects().values();
	//synchronized (getKnownList().getKnownObjects())
	{
		for (L2Object obj : objs)
		{
			if (obj instanceof L2SiegeGuardInstance)
				result.add((L2SiegeGuardInstance) obj);
		}
	}
	return result;
}

public Collection<L2FortSiegeGuardInstance> getKnownFortSiegeGuards()
{
	FastList<L2FortSiegeGuardInstance> result = new FastList<L2FortSiegeGuardInstance>();
	
	Collection<L2Object> objs = getKnownList().getKnownObjects().values();
	//synchronized (getKnownList().getKnownObjects())
	{
		for (L2Object obj : objs)
		{
			if (obj instanceof L2FortSiegeGuardInstance)
				result.add((L2FortSiegeGuardInstance) obj);
		}
	}
	return result;
}

public int getA()
{
	return _A;
}

public int getB()
{
	return _B;
}

public int getC()
{
	return _C;
}

public int getD()
{
	return _D;
}

@Override
public boolean doDie(L2Character killer)
{
	if (!super.doDie(killer))
		return false;
	
	boolean isFort = (getFort() != null && getFort().getFortId() > 0 && getFort().getSiege().getIsInProgress()) && !getIsCommanderDoor();
	boolean isCastle = (getCastle() != null	&& getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress());
	
	if (isFort || isCastle)
		broadcastPacket(new SystemMessage(SystemMessageId.CASTLE_GATE_BROKEN_DOWN));
	return true;
}

@Override
public void sendInfo(L2PcInstance activeChar)
{
	activeChar.sendPacket(new StaticObject(this, false));
}


}
