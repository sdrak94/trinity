package net.sf.l2j.gameserver.communitybbs.Manager.lunaservices;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

//import instances.Ultraverse;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.handler.UserCommandHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2TeleportLocation;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TeleporterInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class TeleBBSManager
{
	static final Logger _log = Logger.getLogger(TeleBBSManager.class.getName());
	
	public static TeleBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	static class EscapeFinalizer implements Runnable
	{
		private final L2PcInstance	_player;
		private L2TeleportLocation	_list;
		private int					_val	= 0;
		private int					_x		= 0;
		private int					_y		= 0;
		private int					_z		= 0;
		
		EscapeFinalizer(L2PcInstance player, L2TeleportLocation list)
		{
			_player = player;
			_list = list;
		}
		
		EscapeFinalizer(L2PcInstance player, int val, int x, int y, int z)
		{
			_player = player;
			_val = val;
			_x = x;
			_y = y;
			_z = z;
		}
		
		public void run()
		{
			if (_player.isAlikeDead())
				return;
			_player.setIsIn7sDungeon(false);
			_player.enableAllSkills();
			_player.setIsCastingNow(false);
			/* _player.setInstanceId(0); */
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
				if (_x != 0)
				{
					_player.teleToLocation(_x, _y, _z, true);
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
	
	private void teleportTo(L2PcInstance activeChar, String Cords)
	{
		try
		{
			StringTokenizer st = new StringTokenizer(Cords);
			String x1 = st.nextToken();
			int x = Integer.parseInt(x1);
			String y1 = st.nextToken();
			int y = Integer.parseInt(y1);
			String z1 = st.nextToken();
			int z = Integer.parseInt(z1);
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			activeChar.teleToLocation(x, y, z, false);
			activeChar.sendMessage("You have been teleported to " + Cords);
		}
		catch (NoSuchElementException nsee)
		{
			activeChar.sendMessage("Wrong or no Coordinates given.");
		}
	}
	
	final public static void doTeleport(L2PcInstance player, int val, int x, int y, int z, boolean gemTeleport)
	{
		if (!L2TeleporterInstance.checkIfCanTeleport(player))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		final L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
		if (list != null || val == 50000 || val == 50001 || val == 50002 || x != 0)
		{
			if (list != null && !player.isGM())
			{
				if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && player.getKarma() > 0) // karma
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
				final boolean isinPeace = player.isInsideZone(L2Character.ZONE_PEACE);
				int unstuckTimer = Config.UNSTUCK_INTERVAL * 1000;
				if (player.getPvpFlag() != 0 || player.getKarma() > 0 || player.isInCombat())
				{
					unstuckTimer *= 1.5;
					if (player.isCursedWeaponEquipped())
						unstuckTimer *= 4;
				}
				else if (isinPeace)
					unstuckTimer = 2200;
				if (player.isInsideZone(L2Character.ZONE_CLANHALL) && !player.isInCombat())
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

				player.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(new EscapeFinalizer(player, val, x, y, z), unstuckTimer));
				// Continue execution later
				//player.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(ef, unstuckTimer));
			}
			else
			{
				if (val != 50000 && val != 50001 && val != 50002 && val != 0)
					player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), true);
				if (val == 0 && x != 0)
				{
					player.teleToLocation(x, y, z, true);
				}
				else if (player.isGM())
				{
					player.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(new EscapeFinalizer(player, val, x, y, z), 1000));
				}
			}
		}
		else
		{
			_log.warning("No teleport destination with id:" + val);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void parsecmd(String command, L2PcInstance activeChar)
	{
			String path = "data/html/CommunityBoard/gk/";
			String filepath = "";
			String content = "";
			if (command.equals("_bbstele"))
			 		{

				filepath = path +"teleport.htm";
				content = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), filepath);
				separateAndSend(content, activeChar);
			 		}
			else if (command.startsWith("_bbsteleto_page"))
			{
				StringTokenizer st = new StringTokenizer(command, "_");
				st.nextToken();
				st.nextToken();
				String name = st.nextToken();
				
				filepath = path+name;
				content = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), filepath);
				separateAndSend(content, activeChar);
			}
			else if (command.startsWith("_bbsteleto_exloc"))
			{
				try
				{
					StringTokenizer tokenizer = new StringTokenizer(command.substring(17), ",");
					int x = 0;
					int y = 0;
					int z = 0;
					while (tokenizer.hasMoreTokens())
					{
						x = Integer.parseInt(tokenizer.nextToken());
						y = Integer.parseInt(tokenizer.nextToken());
						z = Integer.parseInt(tokenizer.nextToken());
			        }        
					doTeleport(activeChar, 0, x, y, z, true);
					//teleportTo(activeChar, val);
				}
				catch (StringIndexOutOfBoundsException e)
				{
					//Case of empty or missing coordinates
					//AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
				}
				catch (NumberFormatException nfe)
				{
					activeChar.sendMessage("Lul something went bad. Contact Admin Luna with the bugged location name, he's gonna instantly fix it.");
					//AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
				}
			}
			else if (command.startsWith("_bbsteleto_loc"))
			{
				/*StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				st.nextToken();*/
				int loc = 0;
				try
				{
					loc = Integer.parseInt(command.substring(15));
					//loc = Integer.parseInt(st.nextToken());
				}
				
				catch (NumberFormatException nfe)
				{
				}
				
			if (activeChar != null)
			{
				if (activeChar.isInFunEvent())
				{
					activeChar.sendMessage("Cannot use while in an event");
					return;
				}
				if (activeChar.isFlying() || activeChar.isFlyingMounted() || activeChar.isInJail())
				{
					activeChar.sendMessage("Denied");
					return;
				}
				if (command.substring(15).equalsIgnoreCase("unstuck"))
				{
					IUserCommandHandler handler = UserCommandHandler.getInstance().getUserCommandHandler(52);
					if (handler != null)
						handler.useUserCommand(52, activeChar); //unstuck command
				}
				else
				{
					if (activeChar.getInstanceId() > 0 && (activeChar.getInstanceId() == 1 || (InstanceManager.getInstance().getPlayerWorld(activeChar) != null && InstanceManager.getInstance().getPlayerWorld(activeChar).templateId != InstanceManager.ULTRAVERSE_ID)))
					{
						activeChar.sendMessage("Cannot use while in an instance");
						return;
					}
					L2TeleporterInstance.doTeleport(activeChar, loc, true);
				}
			}
			}
		
			 	else
			 		{
			ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", "101");
			activeChar.sendPacket(sb);
			activeChar.sendPacket(new ShowBoard(null, "102"));
			activeChar.sendPacket(new ShowBoard(null, "103"));
			 		}
	}
	
	protected void separateAndSend(String html, L2PcInstance acha)
	{
		if (html == null)
			return;
		acha.sendPacket(new ShowBoard(html, "101"));
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final TeleBBSManager _instance = new TeleBBSManager();
	}
}
