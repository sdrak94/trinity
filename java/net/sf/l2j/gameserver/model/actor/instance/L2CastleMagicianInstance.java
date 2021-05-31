package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.util.Rnd;

public class L2CastleMagicianInstance extends L2NpcInstance
{
protected static final int COND_ALL_FALSE = 0;
protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
protected static final int COND_OWNER = 2;

private final static int[] TalismanIds =
{
	9914,9915,9917,9918,9919,9920,9921,9922,9923,9924,
	9926,9927,9928,9930,9931,9932,9933,9934,9935,9936,
	9937,9938,9939,9940,9941,9942,9943,9944,9945,9946,
	9947,9948,9949,9950,9951,9952,9953,9954,9955,9956,
	9957,9958,9959,9960,9961,9962,9963,9964,9965,9966,
	10141,10142,10158
};

public L2CastleMagicianInstance(int objectId, L2NpcTemplate template)
{
	super(objectId, template);
}

@Override
public void showChatWindow(L2PcInstance player, int val)
{
	player.sendPacket( ActionFailed.STATIC_PACKET );
	String filename = "data/html/castlemagician/magician-no.htm";
	
	int condition = validateCondition(player);
	if (condition > COND_ALL_FALSE)
	{
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			filename = "data/html/castlemagician/magician-busy.htm"; // Busy because of siege
		else if (condition == COND_OWNER)                                    // Clan owns castle
		{
			if (val == 0)
				filename = "data/html/castlemagician/magician.htm";
			else
				filename = "data/html/castlemagician/magician-" + val + ".htm";
		}
	}
	
	NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	html.setFile(filename);
	html.replace("%objectId%", String.valueOf(getObjectId()));
	player.sendPacket(html);
}

@Override
public void onBypassFeedback(L2PcInstance player, String command)
{
	if (command.startsWith("Chat"))
	{
		int val = 0;
		try
		{
			val = Integer.parseInt(command.substring(5));
		}
		catch (IndexOutOfBoundsException ioobe){}
		catch (NumberFormatException nfe){}
		showChatWindow(player, val);
	}
	else if (command.startsWith("ExchangeKE"))
	{
		String filename = null;
		int item = TalismanIds[Rnd.get(TalismanIds.length)];
		
		if (player.destroyItemByItemId("ExchangeKE", 9912, 1000, this, false))
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			msg.addItemName(9912);
			msg.addNumber(1000);
			player.sendPacket(msg);
			
			player.addItem("ExchangeKE", item, 1, player, true);
			
			filename = "data/html/castlemagician/magician-KE-Exchange.htm";
		}
		else
			filename = "data/html/castlemagician/magician-no-KE.htm";
		
		showChatWindow(player, filename);
	}
	else if (command.equals("gotoleader"))
	{
		if (player.getClan() != null)
		{
			L2PcInstance clanLeader = player.getClan().getLeader().getPlayerInstance();
			if (clanLeader == null)
				return;
			
			if (clanLeader.getFirstEffect(L2EffectType.CLAN_GATE) != null)
			{
				if (!validateGateCondition(clanLeader, player))
					return;
				
				player.teleToLocation(clanLeader.getX(), clanLeader.getY(), clanLeader.getZ(), false);
				return;
			}
			String filename = "data/html/castlemagician/magician-nogate.htm";
			showChatWindow(player, filename);
		}
	}
	else
		super.onBypassFeedback(player, command);
}

protected int validateCondition(L2PcInstance player)
{
	if (player.isGM())
		return COND_OWNER;
	if (getCastle() != null && getCastle().getCastleId() > 0)
	{
		if (player.getClan() != null)
		{
			if (getCastle().getSiege().getIsInProgress())
				return COND_BUSY_BECAUSE_OF_SIEGE;                   // Busy because of siege
			else if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
				return COND_OWNER;
		}
	}
	return COND_ALL_FALSE;
}

private static final boolean validateGateCondition(L2PcInstance clanLeader, L2PcInstance player)
{
	if (clanLeader.isAlikeDead())
	{
		// Need retail message if there's one.
		player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
		return false;
	}
	
	if (clanLeader.isInStoreMode())
	{
		// Need retail message if there's one.
		player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
		return false;
	}
	
	if (clanLeader.isRooted() || clanLeader.isInCombat())
	{
		// Need retail message if there's one.
		player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
		return false;
	}
	
	if (clanLeader.isInOlympiadMode())
	{
		// Need retail message if there's one.
		player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
		return false;
	}
	
	if (clanLeader.isFestivalParticipant())
	{
		// Need retail message if there's one.
		player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
		return false;
	}
	
	if (clanLeader.inObserverMode())
	{
		// Need retail message if there's one.
		player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
		return false;
	}
	
	if (clanLeader.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
	{
		// Need retail message if there's one.
		player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
		return false;
	}
	
	if (clanLeader.getInstanceId() > 0)
	{
		if (!Config.ALLOW_SUMMON_TO_INSTANCE
				|| InstanceManager.getInstance().getInstance(player.getInstanceId()).isSummonAllowed())
		{
			// Need retail message if there's one.
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
	}
	
	if (player.isIn7sDungeon())
	{
		final int targetCabal = SevenSigns.getInstance().getPlayerCabal(clanLeader);
		if (SevenSigns.getInstance().isSealValidationPeriod())
		{
			if (targetCabal != SevenSigns.getInstance().getCabalHighestScore())
			{
				// Need retail message if there's one.
				player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
				return false;
			}
		}
		else
		{
			if (targetCabal == SevenSigns.CABAL_NULL)
			{
				// Need retail message if there's one.
				player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
				return false;
			}
		}
	}
	
	
	if (!TvTEvent.onEscapeUse(player.getObjectId()))
	{
		player.sendMessage("You on TvT Event, teleporting disabled.");
		return false;
	}
	
	if (!TvTEvent.onEscapeUse(clanLeader.getObjectId()))
	{
		// Need retail message if there's one.
		player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
		return false;
	}
	
	return true;
}
}