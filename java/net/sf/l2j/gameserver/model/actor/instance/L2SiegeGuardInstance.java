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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2SiegeGuardAI;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.knownlist.SiegeGuardKnownList;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

/**
 * This class represents all guards in the world. It inherits all methods from
 * L2Attackable and adds some more such as tracking PK's or custom interactions.
 *
 * @version $Revision: 1.11.2.1.2.7 $ $Date: 2005/04/06 16:13:40 $
 */
public class L2SiegeGuardInstance extends L2Attackable
{
public L2SiegeGuardInstance(int objectId, L2NpcTemplate template)
{
	super(objectId, template);
}

@Override
public SiegeGuardKnownList getKnownList()
{
	return (SiegeGuardKnownList)super.getKnownList();
}

@Override
public void initKnownList()
{
	setKnownList(new SiegeGuardKnownList(this));
}

@Override
public L2CharacterAI getAI()
{
	L2CharacterAI ai = _ai; // copy handle
	if (ai == null)
	{
		synchronized(this)
		{
			if (_ai == null) _ai = new L2SiegeGuardAI(new AIAccessor());
			return _ai;
		}
	}
	return ai;
}

/**
 * Return True if a siege is in progress and the L2Character attacker isn't a Defender.<BR><BR>
 *
 * @param attacker The L2Character that the L2SiegeGuardInstance try to attack
 *
 */
@Override
public boolean isAutoAttackable(L2Character attacker)
{
	attacker = attacker.getActingPlayer();
	
	if (attacker == null)
		return false;
	
	if (!(attacker instanceof L2PcInstance))
		return false;
	
	boolean isCastle = ( getCastle() != null && getCastle().getCastleId() > 0
			&& getCastle().getSiege().getIsInProgress()
			&& !getCastle().getSiege().checkIsDefender(((L2PcInstance)attacker).getClan()));
	
	boolean isFort = ( getFort() != null && getFort().getFortId() > 0
			&& getFort().getSiege().getIsInProgress()
			&& !getFort().getSiege().checkIsDefender(((L2PcInstance)attacker).getClan()));
	
	// Attackable during siege by all except defenders ( Castle or Fort )
	return (isCastle || isFort);
}

@Override
public boolean hasRandomAnimation()
{
	return false;
}

/**
 * This method forces guard to return to home location previously set
 *
 */
@Override
public void returnHome()
{
	if (!isInsideRadius(getSpawn().getCurX(), getSpawn().getCurY(), 40, false))
	{
		if (Config.DEBUG) _log.fine(getObjectId()+": moving home");
		setisReturningToSpawnPoint(true);
		clearAggroList();
		
		if (hasAI())
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(getSpawn().getCurX(), getSpawn().getCurY(), getSpawn().getCurZ(), 0));
	}
}

/**
 * Custom onAction behaviour. Note that super() is not called because guards need
 * extra check to see if a player should interact or ATTACK them when clicked.
 * 
 */
@Override
public void onAction(L2PcInstance player)
{
	if (!canTarget(player)) return;
	
	// Check if the L2PcInstance already target the L2NpcInstance
	if (this != player.getTarget())
	{
		if (Config.DEBUG) _log.fine("new target selected:"+getObjectId());
		
		// Set the target of the L2PcInstance player
		player.setTarget(this);
		
		// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
		MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
		player.sendPacket(my);
		
		// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int)getStatus().getCurrentHp() );
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp() );
		player.sendPacket(su);
		
		// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
		player.sendPacket(new ValidateLocation(this));
	}
	else
	{
		if (isAutoAttackable(player) && !isAlikeDead())
		{
			if (Math.abs(player.getZ() - getZ()) < 600) // this max heigth difference might need some tweaking
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else
			{
				// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
		if(!isAutoAttackable(player))
		{
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				SocialAction sa = new SocialAction(getObjectId(), Rnd.nextInt(8));
				broadcastPacket(sa);
				sendPacket(sa);
				showChatWindow(player, 0);
			}
		}
	}
}

@Override
public void addDamageHate(L2Character attacker, int aggro)
{
	if (attacker == null)
		return;
	
	if (!(attacker instanceof L2SiegeGuardInstance))
	{
		super.addDamageHate(attacker, aggro);
	}
}
}
