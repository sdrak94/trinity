package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

//import instances.Ultraverse;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2TeleportLocation;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author NightMarez
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 * */
public final class L2TeleporterInstance extends L2Npc
{
private static final int COND_ALL_FALSE = 0;
private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
private static final int COND_OWNER = 2;
private static final int COND_REGULAR = 3;

public L2TeleporterInstance(int objectId, L2NpcTemplate template)
{
	super(objectId, template);
}

@Override
public void onBypassFeedback(L2PcInstance player, String command)
{
	player.sendPacket(ActionFailed.STATIC_PACKET);
	
	final int condition = validateCondition(player);
	
	StringTokenizer st = new StringTokenizer(command, " ");
	String actualCommand = st.nextToken(); // Get actual command
	
	if (actualCommand.equalsIgnoreCase("goto"))
	{
		int npcId = getTemplate().npcId;
		
		switch (npcId)
		{
		case 31095: //
		case 31096: //
		case 31097: //
		case 31098: // Enter Necropolises
		case 31099: //
		case 31100: //
		case 31101: //
		case 31102: //
			
		case 31114: //
		case 31115: //
		case 31116: // Enter Catacombs
		case 31117: //
		case 31118: //
		case 31119: //
			player.setIsIn7sDungeon(true);
			break;
		case 31103: //
		case 31104: //
		case 31105: //
		case 31106: // Exit Necropolises
		case 31107: //
		case 31108: //
		case 31109: //
		case 31110: //
			
		case 31120: //
		case 31121: //
		case 31122: // Exit Catacombs
		case 31123: //
		case 31124: //
		case 31125: //
			player.setIsIn7sDungeon(false);
			break;
		}
		
		if (st.countTokens() <= 0)
		{
			return;
		}
		final int whereTo = Integer.parseInt(st.nextToken());
		
		if (condition == COND_REGULAR)
		{
			doTeleport(player, whereTo);
			return;
		}
		else if (condition == COND_OWNER)
		{
			int minPrivilegeLevel = 0; // NOTE: Replace 0 with highest level when privilege level is implemented
			if (st.countTokens() >= 1)
			{
				minPrivilegeLevel = Integer.parseInt(st.nextToken());
			}
			if (10 >= minPrivilegeLevel) // NOTE: Replace 10 with privilege level of player
				doTeleport(player, whereTo);
			else
				player.sendMessage("You don't have the sufficient access level to teleport there.");
			return;
		}
	}
	else if (command.startsWith("Chat"))
	{
		int val = 0;
		try
		{
			val = Integer.parseInt(command.substring(5));
		}
		catch (IndexOutOfBoundsException ioobe)
		{}
		catch (NumberFormatException nfe)
		{}
		
		if (val == 1 && player.getLevel() < 41)
		{
			showNewbieHtml(player);
			return;
		}
		showChatWindow(player, val);
	}
	super.onBypassFeedback(player, command);
}

@Override
public String getHtmlPath(int npcId, int val)
{
	String pom = "";
	if (val == 0)
	{
		pom = "" + npcId;
	}
	else
	{
		pom = npcId + "-" + val;
	}
	
	return "data/html/teleporter/" + pom + ".htm";
}

private void showNewbieHtml(L2PcInstance player)
{
	if (player == null)
		return;
	
	NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	
	String filename = "data/html/teleporter/free/" + getTemplate().npcId + ".htm";
	if (!HtmCache.getInstance().isLoadable(filename))
		filename = "data/html/teleporter/" + getTemplate().npcId + "-1.htm";
	
	html.setFile(filename);
	html.replace("%objectId%", String.valueOf(getObjectId()));
	html.replace("%npcname%", getName());
	player.sendPacket(html);
}

@Override
public void showChatWindow(L2PcInstance player)
{
	String filename = "data/html/teleporter/castleteleporter-no.htm";
	
	int condition = validateCondition(player);
	if (condition == COND_REGULAR)
	{
		super.showChatWindow(player);
		return;
	}
	else if (condition > COND_ALL_FALSE)
	{
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			filename = "data/html/teleporter/castleteleporter-busy.htm"; // Busy because of siege
		else if (condition == COND_OWNER) // Clan owns castle
		{
			if (player.getLevel()<40)
				filename = getHtmlPath(getNpcId(), 0); // Owner message window
		}
	}
	
	NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
	html.setFile(filename);
	html.replace("%objectId%", String.valueOf(getObjectId()));
	html.replace("%npcname%", getName());
	player.sendPacket(html);
}
public static void doTeleport(L2PcInstance player, int val)
{
	doTeleport(player, val, false);
}
final public static void doTeleport(L2PcInstance player, int val, boolean gemTeleport)
{
	if (!checkIfCanTeleport(player))
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	final L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
	
	if (list != null || val == 50000 || val == 50001 || val == 50002)
	{
		if (list != null && !player.isGM())
		{
			/*if (SiegeManager.getInstance().getSiege(list.getLocX(), list.getLocY(), list.getLocZ()) != null)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.NO_PORT_THAT_IS_IN_SIGE));
					return;
				}
				else if (TownManager.townHasCastleInSiege(list.getLocX(), list.getLocY()))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.NO_PORT_THAT_IS_IN_SIGE));
					return;
				}
				else*/ if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && player.getKarma() > 0) //karma
				{
					player.sendMessage("Go away, you're not welcome here.");
					return;
				}
				else if (list.getIsForNoble() && !player.isNoble())
				{
					String filename = "data/html/teleporter/nobleteleporter-no.htm";
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setFile(filename);
					html.replace("%objectId%", String.valueOf(1));
					html.replace("%npcname%", "Teleporation");
					player.sendPacket(html);
					return;
				}
		}
		
		if (!gemTeleport && !player.isGM())
		{
			if (player.getPvpFlag() != 0 || player.isInCombat())
			{
				player.sendMessage("You cannot teleport via NPCs while flagged or in combat mode.");
				return;
			}
		}
		
		if (gemTeleport && !player.isGM())
		{
			final boolean isinPeace = player.isInsideZone(ZONE_PEACE);
			
			int unstuckTimer = Config.UNSTUCK_INTERVAL*1000;
			
			if (player.getPvpFlag() != 0 || player.getKarma() > 0 || player.isInCombat())
			{
				unstuckTimer *= 1.5;
				
				if (player.isCursedWeaponEquipped())
					unstuckTimer *= 4;
			}
			else if (isinPeace)
				unstuckTimer = 2200;

			if (player.isInsideZone(ZONE_CLANHALL) && !player.isInCombat())
				unstuckTimer = 2200;
			
			if (player.isSpawnProtected() && player.isInGludin())
				unstuckTimer = 9000;
			
			player.abortCast();
			player.abortAttack();
			player.forceIsCasting(GameTimeController.getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
			
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			player.setTarget(player);
			player.disableAllSkills();
			
			player.broadcastPacket(new MagicSkillUse(player, 1050, 1, unstuckTimer, 0));
			player.sendPacket(new SetupGauge(0, unstuckTimer));
			
			EscapeFinalizer ef;
			
			if (list != null)
				ef = new EscapeFinalizer(player, list);
			else
				ef = new EscapeFinalizer(player, val);
			// Continue execution later
			player.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(ef, unstuckTimer));
		}
		else
		{
			if (val != 50000 && val != 50001 && val != 50002)
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), true);
			else if (player.isGM())
			{
				player.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(new EscapeFinalizer(player, val), 1000));
			}
		}
	}
	else
	{
		_log.warning("No teleport destination with id:" + val);
	}
	
	player.sendPacket(ActionFailed.STATIC_PACKET);
}

public static boolean checkIfCanTeleport(L2PcInstance activeChar)
{
	if (activeChar.isGM()) return true;
	
	if (activeChar.isSitting())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
		return false;
	}
	if (activeChar.isInOlympiadMode())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
		return false;
	}
	/*// Check to see if the current player is in TvT , CTF or ViP events.
		if (activeChar.isInFunEvent())
		{
			activeChar.sendMessage("You may not escape from an Event.");
			return false;
		}*/
	
	if (GrandBossManager.getInstance().getZone(activeChar) != null && !activeChar.isGM())
	{
		activeChar.sendMessage("You may not use an escape command in a Boss Zone.");
		return false;
	}
	
	// Check to see if the player is in a festival.
	if (activeChar.isFestivalParticipant())
	{
		activeChar.sendMessage("You may not use an escape command in a festival.");
		return false;
	}
	
	// Check to see if player is in jail
	if (activeChar.isInJail())
	{
		activeChar.sendMessage("You can not escape from jail.");
		return false;
	}
	
	if (activeChar.inObserverMode())
	{
		activeChar.sendMessage("You cannot escape during Observation Mode.");
		return false;
	}
	
	// Check to see if player is in a duel
	if (activeChar.isInDuel())
	{
		activeChar.sendMessage("You cannot escape during a duel.");
		return false;
	}
	
	if (activeChar.getInstanceId() > 0 && (!activeChar.isInUniqueInstance() || InstanceManager.getInstance().getPlayerWorld(activeChar).templateId != InstanceManager.ULTRAVERSE_ID)&& !activeChar.isInActiveFunEvent())
	{
		activeChar.sendMessage("You cannot teleport while in an instance.");
		return false;
	}
	
	if (activeChar.isCastingNow() || activeChar.isMovementDisabled() || activeChar.isAlikeDead())
		return false;
	
	return true;
}

private int validateCondition(L2PcInstance player)
{
	if (CastleManager.getInstance().getCastleIndex(this) < 0) // Teleporter isn't on castle ground
		return COND_REGULAR; // Regular access
	else if (getCastle().getSiege().getIsInProgress()) // Teleporter is on castle ground and siege is in progress
		return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
	else if (player.getClan() != null) // Teleporter is on castle ground and player is in a clan
	{
		if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
			return COND_OWNER; // Owner
	}
	
	return COND_ALL_FALSE;
}

static class EscapeFinalizer implements Runnable
{
private final L2PcInstance	_player;
private L2TeleportLocation _list;
private int _val = 0;

EscapeFinalizer(L2PcInstance player, L2TeleportLocation list)
{
	_player = player;
	_list = list;
}
EscapeFinalizer(L2PcInstance player, int val)
{
	_player = player;
	_val = val;
}

public void run()
{
	if (_player.isAlikeDead())
		return;
	
	_player.setIsIn7sDungeon(false);
	_player.enableAllSkills();
	_player.setIsCastingNow(false);
	/*_player.setInstanceId(0);*/
	
	try
	{
		if (_val > 0)
		{
			if (_player.getInstanceId() > 2)
				return;
			
			switch (_val)
			{
			case 50000:
				if (_player.getClan() != null && CastleManager.getInstance().getCastleByOwner(_player.getClan()) != null)
					_player.teleToLocation(TeleportWhereType.Castle);
				else
					_player.teleToLocation(TeleportWhereType.Town);
				break;
			case 50001:
				if (_player.getClan() != null && FortManager.getInstance().getFortByOwner(_player.getClan()) != null)
					_player.teleToLocation(TeleportWhereType.Fortress);
				else
					_player.teleToLocation(TeleportWhereType.Town);
				break;
			case 50002:
				if (_player.getClan() != null && ClanHallManager.getInstance().getClanHallByOwner(_player.getClan()) != null)
					_player.teleToLocation(TeleportWhereType.ClanHall);
				else
					_player.teleToLocation(TeleportWhereType.Town);
				break;
			}
		}
		else
			_player.teleToLocation(_list.getLocX(), _list.getLocY(), _list.getLocZ(), true);
	}
	catch (Exception e)
	{
		_log.warning(e.getMessage());
	}
}
}
}
