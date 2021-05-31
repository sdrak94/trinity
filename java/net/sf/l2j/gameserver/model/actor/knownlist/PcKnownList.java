package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TrapInstance;
import net.sf.l2j.gameserver.network.serverpackets.DeleteObject;
import net.sf.l2j.gameserver.network.serverpackets.SpawnItem;

public class PcKnownList extends PlayableKnownList
{
// =========================================================
// Data Field

// =========================================================
// Constructor
public PcKnownList(L2PcInstance activeChar)
{
	super(activeChar);
}

// =========================================================
// Method - Public
/**
 * Add a visible L2Object to L2PcInstance _knownObjects and _knownPlayer (if necessary) and send Server-Client Packets needed to inform the L2PcInstance of its state and actions in progress.<BR><BR>
 *
 * <B><U> object is a L2ItemInstance </U> :</B><BR><BR>
 * <li> Send Server-Client Packet DropItem/SpawnItem to the L2PcInstance </li><BR><BR>
 *
 * <B><U> object is a L2DoorInstance </U> :</B><BR><BR>
 * <li> Send Server-Client Packets DoorInfo and DoorStatusUpdate to the L2PcInstance </li>
 * <li> Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance </li><BR><BR>
 *
 * <B><U> object is a L2NpcInstance </U> :</B><BR><BR>
 * <li> Send Server-Client Packet NpcInfo to the L2PcInstance </li>
 * <li> Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance </li><BR><BR>
 *
 * <B><U> object is a L2Summon </U> :</B><BR><BR>
 * <li> Send Server-Client Packet NpcInfo/PetItemList (if the L2PcInstance is the owner) to the L2PcInstance </li>
 * <li> Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance </li><BR><BR>
 *
 * <B><U> object is a L2PcInstance </U> :</B><BR><BR>
 * <li> Send Server-Client Packet CharInfo to the L2PcInstance </li>
 * <li> If the object has a private store, Send Server-Client Packet PrivateStoreMsgSell to the L2PcInstance </li>
 * <li> Send Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance </li><BR><BR>
 *
 * @param object The L2Object to add to _knownObjects and _knownPlayer
 * @param dropper The L2Character who dropped the L2Object
 */
@Override
public boolean addKnownObject(L2Object object, boolean force)
{
	if (object instanceof L2TrapInstance)
	{
		final L2TrapInstance trap = (L2TrapInstance)object;
		
		if (!(trap.isStopped() || trap.isDetected() || getActiveChar() == trap.getOwner() || (!trap.getOwner().isGMReally() && getActiveChar().canSeeInvisiblePeople())))
		{
			if (getActiveChar().getInSameClanAllyAs(trap.getOwner()) > 0)
			{}
			else if (getActiveChar().getParty() != null && getActiveChar().getParty().getPartyMembers().contains(trap.getOwner()))
			{}
			else if (getActiveChar().inObserverMode())
			{}
			else return false;
		}
	}
	if (getActiveChar().getVarB("hideStores") && (object != null) && (object instanceof L2PcInstance))
	{
		if ((object.getActingPlayer().isInStoreMode()))
		{
			return false;
		}
	}
	else if (object instanceof L2PcInstance)
	{
		if (!object.getActingPlayer().canSendUserInfo)
			return false;
	}
	
	if (!super.addKnownObject(object, force)) return false;
	
	if (object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item"))
	{
		getActiveChar().sendPacket(new SpawnItem(object));
	}
	
	else
	{
		
		object.sendInfo(getActiveChar());
		
		if (object instanceof L2Character)
		{
			// Update the state of the L2Character object client side by sending Server->Client packet MoveToPawn/CharMoveToLocation and AutoAttackStart to the L2PcInstance
			L2Character obj = (L2Character) object;
			if (obj.getAI() != null)
				obj.getAI().describeStateToPlayer(getActiveChar());
		}
	}
	return true;
}

@Override
public boolean addKnownObject(L2Object object)
{
	return addKnownObject(object, false);
}

/**
 * Remove a L2Object from L2PcInstance _knownObjects and _knownPlayer (if necessary) and send Server-Client Packet DeleteObject to the L2PcInstance.<BR><BR>
 *
 * @param object The L2Object to remove from _knownObjects and _knownPlayer
 *
 */
@Override
public boolean removeKnownObject(L2Object object)
{
	if (!super.removeKnownObject(object)) return false;
	// Send Server-Client Packet DeleteObject to the L2PcInstance
	getActiveChar().sendPacket(new DeleteObject(object));
	if (Config.CHECK_KNOWN && object instanceof L2Npc) getActiveChar().sendMessage("Removed NPC: "+((L2Npc)object).getName());
	return true;
}

// =========================================================
// Method - Private

// =========================================================
// Property - Public
@Override
public final L2PcInstance getActiveChar() { return (L2PcInstance)super.getActiveChar(); }

@Override
public int getDistanceToForgetObject(L2Object object)
{
	final double instanceBoost = getActiveChar().isInUniqueInstance() ? 1.3 : 1;
	// when knownlist grows, the distance to forget should be at least
	// the same as the previous watch range, or it becomes possible that
	// extra charinfo packets are being sent (watch-forget-watch-forget)
	/*final int knownlistSize = getKnownObjects().size();*/
	
	return (int) (4400*instanceBoost);
	/*	if (knownlistSize <= 25) return (int) (5000*instanceBoost);
	if (knownlistSize <= 35) return (int) (4000*instanceBoost);
	if (knownlistSize <= 70) return (int) (3200*instanceBoost);
	else return (int) (2430*instanceBoost);*/
}

@Override
public int getDistanceToWatchObject(L2Object object)
{
	final double instanceBoost = getActiveChar().isInUniqueInstance() ? 1.3 : 1;
	
	/*final int knownlistSize = getKnownObjects().size();*/
	
	return (int) (3400*instanceBoost);
	/*	if (knownlistSize <= 25) return (int) (3900*instanceBoost);
	if (knownlistSize <= 35) return (int) (3100*instanceBoost);
	if (knownlistSize <= 70) return (int) (2400*instanceBoost);
	else return (int) (1750*instanceBoost); // Siege, TOI, city
	 */}

}