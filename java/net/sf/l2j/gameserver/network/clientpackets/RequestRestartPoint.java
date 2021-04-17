package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.util.Rnd;

public final class RequestRestartPoint extends L2GameClientPacket
{
private static final String _C__6d_REQUESTRESTARTPOINT = "[C] 6d RequestRestartPoint";
private static Logger _log = Logger.getLogger(RequestRestartPoint.class.getName());

protected int     _requestedPointType;
protected boolean _continuation;

@Override
protected void readImpl()
{
	_requestedPointType = readD();
}

class DeathTask implements Runnable
{
final L2PcInstance activeChar;

DeathTask (L2PcInstance _activeChar)
{
	activeChar = _activeChar;
}

@SuppressWarnings("synthetic-access")
public void run()
{
	if (!activeChar.isRespawnRequested())
		return;
	
	Location loc = null;
	Castle castle = null;
	Fort fort = null;
	Boolean isInDefense = false;
	
	// force jail
	if (activeChar.isInJail())
	{
		_requestedPointType = 27;
	}
	else if (activeChar.isFestivalParticipant())
	{
		_requestedPointType = 5;
	}
	switch (_requestedPointType)
	{
	case 1: // to clanhall
		if (activeChar.getClan() == null || activeChar.getClan().getHasHideout() == 0)
		{
			_log.warning("Player ["+activeChar.getName()+"] called RestartPointPacket - To Clanhall and he doesn't have Clanhall!");
			return;
		}
		loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.ClanHall);
		
		if (ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan())!= null &&
				ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP) != null)
		{
			activeChar.restoreExp(ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl());
		}
		break;
		
	case 2: // to castle
		castle = CastleManager.getInstance().getCastle(activeChar);
		
		if (castle != null && castle.getSiege().getIsInProgress())
		{
			// Siege in progress
			if (castle.getSiege().checkIsDefender(activeChar.getClan()))
				loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Castle);
			// Just in case you lost castle while being dead.. Port to nearest Town.
			else if (castle.getSiege().checkIsAttacker(activeChar.getClan()))
				loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Town);
			else
			{
				_log.warning("Player ["+activeChar.getName()+"] called RestartPointPacket - To Castle and he doesn't have Castle!");
				return;
			}
		}
		else
		{
			if (activeChar.getClan() == null || activeChar.getClan().getHasCastle() == 0)
				return;
			else
				loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Castle);
		}
		if (CastleManager.getInstance().getCastleByOwner(activeChar.getClan())!= null &&
				CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).getFunction(Castle.FUNC_RESTORE_EXP) != null)
		{
			activeChar.restoreExp(CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).getFunction(Castle.FUNC_RESTORE_EXP).getLvl());
		}
		break;
		
	case 3: // to fortress
		fort = FortManager.getInstance().getFort(activeChar);
		
		if ((activeChar.getClan() == null || activeChar.getClan().getHasFort() == 0) && !isInDefense)
		{
			_log.warning("Player ["+activeChar.getName()+"] called RestartPointPacket - To Fortress and he doesn't have Fortress!");
			return;
		}
		loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Fortress);
		if (FortManager.getInstance().getFortByOwner(activeChar.getClan())!= null &&
				FortManager.getInstance().getFortByOwner(activeChar.getClan()).getFunction(Fort.FUNC_RESTORE_EXP) != null)
		{
			activeChar.restoreExp(FortManager.getInstance().getFortByOwner(activeChar.getClan()).getFunction(Fort.FUNC_RESTORE_EXP).getLvl());
		}
		break;
		
	case 4: // to siege HQ
		L2SiegeClan siegeClan = null;
		castle = CastleManager.getInstance().getCastle(activeChar);
		fort = FortManager.getInstance().getFort(activeChar);
		
		if (castle != null && castle.getSiege().getIsInProgress())
			siegeClan = castle.getSiege().getAttackerClan(activeChar.getClan());
		else if (fort != null && fort.getSiege().getIsInProgress())
			siegeClan = fort.getSiege().getAttackerClan(activeChar.getClan());
		
		if (siegeClan == null || siegeClan.getFlag().isEmpty())
		{
			_log.warning("Player ["+activeChar.getName()+"] called RestartPointPacket - To Siege HQ and he doesn't have Siege HQ!");
			return;
		}
		loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.SiegeFlag);
		break;
		
	case 5: // Fixed or Player is a festival participant
		if (!activeChar.isGM() && !activeChar.isFestivalParticipant())
		{
			_log.warning("Player ["+activeChar.getName()+"] called RestartPointPacket - Fixed and he isn't festival participant!");
			return;
		}
		loc = new Location(activeChar.getX(), activeChar.getY(), activeChar.getZ()); // spawn them where they died
		break;
		
	case 27: // to jail
		if (!activeChar.isInJail()) return;
		loc = new Location(-114356, -249645, -2984);
		break;

	default:
			if (activeChar.isInHuntersVillage() || activeChar.isInOrcVillage() || activeChar.isInGludin() || activeChar.isInSiege())
			{
				loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Town);
			}
			else if (activeChar.isInPI())
			{
				int spawn = Rnd.get(10);
				
				switch (spawn)
				{
					case 0:
						loc = new Location(10480, -25364, -3701);
						break;
					case 1:
						loc = new Location(7971, -20252, -3512);
						break;
					case 2:
						loc = new Location(6018, -23672, -3725);
						break;	
					case 3:
						loc = new Location(10465, -26995, -3629);
						break;
					case 4:
						loc = new Location(6018, -23672, -3725);
						break;	
					case 5:
						loc = new Location(10480, -25364, -3701);
						break;
					case 6:
						loc = new Location(7971, -20252, -3512);
						break;
					case 7:	
						loc = new Location(6018, -23672, -3725);
						break;			
					case 8:	
						loc = new Location(10465, -26995, -3629);
						break;	
					case 9:
						loc = new Location(10480, -25364, -3701);
						break;
					default:
						return;	
				}
			}
			else
			{
				int chance = Rnd.get(5);
				switch (chance)
				{
					case 0:
						loc = new Location(83344, 148140, -3404);
						break;
					case 1:
						loc = new Location(83465, 148654, -3404);
						break;
					case 2:
						loc = new Location(83344, 148140, -3404);
						break;
					case 3:
						loc = new Location(82844, 148632, -3471);
						break;
					case 4:
						loc = new Location(82835, 148143, -3468);
						break;
					case 5:
						loc = new Location(83344, 148140, -3404);
						break;
					default:
						loc = new Location(83344, 148140, -3404);
						break;
				}
			}
		break;
	}
	
	if (activeChar.isRespawnRequested())
	{
		// Teleport and revive
		activeChar.setInstanceId(0);
		activeChar.setIsIn7sDungeon(false);
		activeChar.setIsPendingRevive(true);
		activeChar.teleToLocation(loc, true);
		activeChar.setIsRespawnRequested(false);
	}
}
}

@Override
protected void runImpl()
{
	final L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar == null)
		return;
	
	if (activeChar.isInFunEvent() || (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(activeChar.getObjectId())))
		return;
	
	if (activeChar.isFakeDeath())
	{
		activeChar.stopFakeDeath(null);
		return;
	}
	else if(!activeChar.isDead())
	{
		_log.warning("Living player ["+activeChar.getName()+"] called RestartPointPacket! Ban this player!");
		return;
	}
	
	if (activeChar.isRespawnRequested())
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (!activeChar.isGM() && activeChar.getPvpFlag() > 0 && (activeChar.isInGludin())) // physically inside gludin or orv village
	{
		activeChar.setIsRespawnRequested(true);
		ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), 2000);
		//activeChar.sendMessage("You will respawn in 3 seconds");
		return;
	}
	if (!activeChar.isGM() && (activeChar.isInHuntersVillage() || activeChar.isInOrcVillage())) // physically inside gludin or orv village
	{
		activeChar.setIsRespawnRequested(true);
		ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), 2000);
		//activeChar.sendMessage("You will respawn in 3 seconds");
		return;
	} 
	if (!activeChar.isGM() && activeChar.isInPI()) // physically inside gludin or orv village
	{
		activeChar.setIsRespawnRequested(true);
		ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), 2000);
		
		return;
	} 
	/*if (!activeChar.isGM() && activeChar.isInPI() && activeChar.getLastPCKiller() != null) // in PI
		{
			activeChar.setIsRespawnRequested(true);
			ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), 10000);
			activeChar.sendMessage("You will respawn in 10 seconds");
			return;
		}*/
	
	Castle castle = CastleManager.getInstance().getCastle(activeChar.getX(), activeChar.getY(), activeChar.getZ());
	
	if (castle != null && castle.getSiege().getIsInProgress())
	{
		if (activeChar.getClan() != null && castle.getSiege().checkIsAttacker(activeChar.getClan()))
		{
			// Schedule respawn delay for attacker
			activeChar.setIsRespawnRequested(true);
			ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), castle.getSiege().getAttackerRespawnDelay());
			if (castle.getSiege().getAttackerRespawnDelay() > 0)
				activeChar.sendMessage("You will be respawned in " + castle.getSiege().getAttackerRespawnDelay()/1000 + " seconds");
			return;
		}
	}
	
	Fort fort = FortManager.getInstance().getFort(activeChar.getX(), activeChar.getY(), activeChar.getZ());
	
	if (fort != null && fort.getSiege().getIsInProgress())
	{
		activeChar.setIsRespawnRequested(true);
		
		if (activeChar.getClan() != null && fort.getSiege().checkIsAttacker(activeChar.getClan()))
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), 15000);
			activeChar.sendMessage("You will be respawned in 15 seconds");
		}
		else
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), 20000);
			activeChar.sendMessage("You will be respawned in 20 seconds");
		}
		
		return;
	}
	
	// run immediately (no need to schedule)
	activeChar.setIsRespawnRequested(true);
	new DeathTask(activeChar).run();
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__6d_REQUESTRESTARTPOINT;
}
}
