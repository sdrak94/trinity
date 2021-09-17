package net.sf.l2j.gameserver.network.clientpackets;

import static net.sf.l2j.gameserver.model.actor.instance.L2ClassMasterInstance.checkAndChangeClass;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;

import luna.IBypassHandler;
import luna.custom.captcha.RandomString;
import luna.custom.captcha.instancemanager.BotsPreventionManager;
import luna.custom.email.EmailRegistration;
import luna.custom.handler.LunaDetailStats;
import luna.custom.handler.UpdateLunaDetailStats;
import luna.custom.handler.items.bdoBox.BdoData;
import luna.custom.newBie.NewbieHelper;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.BBSSchemeBufferInstance;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.VoicedCommandHandler;
import net.sf.l2j.gameserver.handler.itemhandlers.DonatePotion;
import net.sf.l2j.gameserver.handler.itemhandlers.ElementalExchanger;
import net.sf.l2j.gameserver.handler.itemhandlers.ExtractableItems;
import net.sf.l2j.gameserver.handler.itemhandlers.Gem;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.PvPColorChanger;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.AlphaOpenUrl;
import net.sf.l2j.gameserver.network.serverpackets.ExHeroList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.GMAudit;

/**
 * This class ...
 *
 * @version $Revision: 1.12.4.5 $ $Date: 2005/04/11 10:06:11 $
 */
public final class RequestBypassToServer extends L2GameClientPacket
{
	private static final String	_C__21_REQUESTBYPASSTOSERVER	= "[C] 21 RequestBypassToServer";
	// S

	private static ArrayList<IBypassHandler> _handlers = new ArrayList<>();

	public static void register(IBypassHandler handler)
	{
		_handlers.add(handler);
	}
	
	
	private String				_command;
	
	/**
	 * @param decrypt
	 */
	@Override
	protected void readImpl()
	{
		_command = readS();
	}
	
	
	@Override
	protected void runImpl()
	{
		if(Config.SERVER_LOCAL)
			_log.info("RBS: -> "+ _command);
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		if (!activeChar.getFloodProtectors().getServerBypass().tryPerformAction(_command))
			return;
		try
		{
			for (final var handler : _handlers)
			{
				try
				{
					if (handler.handleBypass(activeChar, _command))
						return;
				}
				catch (Exception e)
				{
					handler.exception(e);
					return;
				}
			}
			
			String _AntibotCommand1 = RandomString.getInstance().getRandomString1() + "_";
			String _AntibotCommand2 = RandomString.getInstance().getRandomString2() + "_";

			if (_command.startsWith("admin_"))
			{
				String command = _command.split(" ")[0];
				IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(command);
				if (ach == null)
				{
					if (activeChar.isGM())
					{
						activeChar.sendMessage("The command " + command.substring(6) + " does not exist!");
						_log.warning("No handler registered for admin command '" + command + "'");
					}
					return;
				}
				if (!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar))
				{
					_log.warning("Character " + activeChar.getName() + " tryed to use admin command " + command + ", but have no access to it!");
					return;
				}
				if (Config.GMAUDIT)
					GMAudit.auditGMAction(activeChar.getAccountName() + " - " + activeChar.getName(), _command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"), "");
				ach.useAdminCommand(_command, activeChar);
			}
			else if (_command.equals("come_here") && (activeChar.isGM()))
			{
				comeHere(activeChar);
			}
			else if (_command.startsWith("player_help "))
			{
				playerHelp(activeChar, _command.substring(12));
			}
			else if (_command.equalsIgnoreCase("_l2trinity_newbie"))
			{
				NewbieHelper.getInstance().giveNewbieRewards(activeChar);
			}
			else if (_command.equalsIgnoreCase("detailstats"))
			{
				activeChar.sendPacket(new LunaDetailStats("test", activeChar));
			}
			else if (_command.equalsIgnoreCase("updatedetailstats"))
			{
				activeChar.sendPacket(new UpdateLunaDetailStats("test", activeChar));
			}
			else if (_command.startsWith("url_ "))
			{
				activeChar.sendPacket(new AlphaOpenUrl(_command.substring(5)));
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			}
			else if (_command.startsWith("trinity_change_class"))
			{
				final int id = Integer.parseInt(_command.split(" ")[1]);
				if (checkAndChangeClass(activeChar, id))
				{
					// self-animation
					activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 5103, 1, 0, 0));
					//final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					//html.setFile("data/html/classmaster/ok.htm");
					//html.replace("%name%", CharTemplateTable.getInstance().getClassNameById(val));
				}
			}
			else if (_command.startsWith("bdoBox_getItem_ "))
			{
				StringTokenizer st = new StringTokenizer(_command);
				st.nextToken();
				int gain = -1;
				int itemId = -1;
				if (st.hasMoreTokens())
				{
					gain = Integer.parseInt(st.nextToken());
					itemId = Integer.parseInt(st.nextToken());
					if (gain == -1 || itemId == -1)
					{
						return;
					}
					BdoData.getInstance().getChosenReward(activeChar, gain, itemId);
				}
			}
			else if (_command.startsWith("event_join_"))
			{
				Event e = EventEngine.getInstance().getActiveEvent();
				if (e != null)
				{
					switch (e.getType())
					{
						case "HuntingGrounds":
							if (NewHuntingGrounds._joining)
							{
								NewHuntingGrounds.addPlayer(activeChar, "eventShuffle");
							}
							break;
						case "Domination":
							if (NewDomination._joining)
							{
								NewDomination.addPlayer(activeChar, "eventShuffle");
							}
							break;
						case "TeamVsTeam":
							if (NewTvT._joining)
							{
								NewTvT.addPlayer(activeChar, "eventShuffle");
							}
							break;
						case "CTF":
							if (NewCTF._joining)
							{
								NewCTF.addPlayer(activeChar, "eventShuffle");
							}
							break;
						case "SiegeEvent":
							if (NewFOS._joining)
							{
								NewFOS.addPlayer(activeChar, "eventShuffle");
							}
							break;
						case "DM":
							if (NewDM._joining)
							{
								NewDM.addPlayer(activeChar);
							}
							break;
					}
				}
			}
			else if (_command.startsWith("event_unjoin_"))
			{
				Event e = EventEngine.getInstance().getActiveEvent();
				if (e != null)
				{
					switch (e.getType())
					{
						case "HuntingGrounds":
							if (NewHuntingGrounds._joining)
							{
								NewHuntingGrounds.removePlayer(activeChar);
							}
							break;
						case "Domination":
							if (NewDomination._joining)
							{
								NewDomination.removePlayer(activeChar);
							}
							break;
						case "TeamVsTeam":
							if (NewTvT._joining)
							{
								NewTvT.removePlayer(activeChar);
							}
							break;
						case "CTF":
							if (NewCTF._joining)
							{
								NewCTF.removePlayer(activeChar);
							}
							break;
						case "SiegeEvent":
							if (NewFOS._joining)
							{
								NewFOS.removePlayer(activeChar);
							}
							break;
						case "DM":
							if (NewDM._joining)
							{
								NewDM.addPlayer(activeChar);
							}
							break;
					}
				}
			}
			else if (_command.startsWith("npc_"))
			{
				if (!activeChar.validateBypass(_command))
					return;
				final int endOfId = _command.indexOf('_', 5);
				String id;
				if (endOfId > 0)
					id = _command.substring(4, endOfId);
				else
					id = _command.substring(4);
				try
				{
					final L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));
					if (object != null && object instanceof L2Npc && endOfId > 0 && activeChar.isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
						((L2Npc) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (final NumberFormatException nfe)
				{
				}
			}
			else if (_command.startsWith("npc_"))
			{
				if (!activeChar.validateBypass(_command))
					return;
				int endOfId = _command.indexOf('_', 5);
				String id;
				if (endOfId > 0)
					id = _command.substring(4, endOfId);
				else
					id = _command.substring(4);
				try
				{
					L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));
					if (_command.substring(endOfId + 1).startsWith("event_participate"))
						L2Event.inscribePlayer(activeChar);
					else if (_command.substring(endOfId + 1).startsWith("vip_joinVIPTeam"))
						VIP.addPlayerVIP(activeChar);
					else if (_command.substring(endOfId + 1).startsWith("vip_joinNotVIPTeam"))
						VIP.addPlayerNotVIP(activeChar);
					else if (_command.substring(endOfId + 1).startsWith("vip_finishVIP"))
						VIP.vipWin(activeChar);
					else if (_command.substring(endOfId + 1).startsWith("tvt_player_join "))
					{
						String teamName = _command.substring(endOfId + 1).substring(16);
						if (TvT._joining)
							TvT.addPlayer(activeChar, teamName);
						else
							activeChar.sendMessage("The event is already started. You can not join now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("tvt_player_leave"))
					{
						if (TvT._joining)
							TvT.removePlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can not leave now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("fos_player_join "))
					{
						String teamName = _command.substring(endOfId + 1).substring(16);
						if (FOS._joining)
							FOS.addPlayer(activeChar, teamName);
						else
							activeChar.sendMessage("The event has already begun. You can not join now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("fos_player_leave"))
					{
						if (FOS._joining)
							FOS.removePlayer(activeChar);
						else
							activeChar.sendMessage("The event has already begun. You can not withdraw your participation now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("dmevent_player_join"))
					{
						if (DM._joining)
							DM.addPlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can not join now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("dmevent_player_leave"))
					{
						if (DM._joining)
							DM.removePlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can not leave now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("ctf_player_join "))
					{
						String teamName = _command.substring(endOfId + 1).substring(16);
						if (CTF._joining)
							CTF.addPlayer(activeChar, teamName);
						else
							activeChar.sendMessage("The event is already started. You can not join now!");
					}
					else if (_command.substring(endOfId + 1).startsWith("ctf_player_leave"))
					{
						if (CTF._joining)
							CTF.removePlayer(activeChar);
						else
							activeChar.sendMessage("The event is already started. You can not leave now!");
					}
					else if (object instanceof L2Npc && endOfId > 0 && activeChar.isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
					{
						((L2Npc) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));
					}
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{}
			}
			else if (_command.startsWith("summon_"))
			{
				if (!activeChar.validateBypass(_command))
					return;
				int endOfId = _command.indexOf('_', 8);
				String id;
				if (endOfId > 0)
					id = _command.substring(7, endOfId);
				else
					id = _command.substring(7);
				try
				{
					L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));
					if (object instanceof L2MerchantSummonInstance && endOfId > 0 && activeChar.isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
						((L2MerchantSummonInstance) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{}
			}
			else if (_command.startsWith("loot_"))
			{
				String action = _command.substring(5);
				String parameters = _command.substring(16);
				final int objId = Integer.parseInt(parameters);
				final L2ItemInstance item = activeChar.getInventory().getItemByObjectId(objId);
				if (item == null)
					return;
				final IItemHandler ih = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
				if (ih == null || !(ih instanceof ExtractableItems))
					return;
				if (action.startsWith("extractOne"))
				{
					try
					{
						((ExtractableItems) ih).doExtract(activeChar, item, 1);
					}
					catch (final Exception e)
					{
						e.printStackTrace();
					}
				}
				else if (action.startsWith("extractAll"))
				{
					((ExtractableItems) ih).doExtract(activeChar, item, (int) item.getCount());
				}
			}
			else if (_command.startsWith("gem_"))
			{
				String action = _command.substring(4);
				try
				{
					Gem.onBypass(activeChar, action);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{}
			}
			else if (_command.startsWith("bonanzo_"))
			{
				String action = _command.substring(8);
				try
				{
					Gem.onBypass(activeChar, action);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{}
			}
			else if (_command.startsWith("ele_"))
			{
				String action = _command.substring(4);
				try
				{
					ElementalExchanger.onBypass(activeChar, action);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{}
			}
			else if (_command.startsWith("pot_"))
			{
				String action = _command.substring(4);
				try
				{
					DonatePotion.onBypass(activeChar, action);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{}
			}
			else if (_command.startsWith("email"))
			{
				String action = _command.substring(5);
				try
				{
					EmailRegistration.onBypass(activeChar, action);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{}
			}
			else if (_command.startsWith("pvpcolor"))
			{
				String action = _command.substring(8);
				try
				{
					PvPColorChanger.onBypass(activeChar, action);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{}
			}
			else if (_command.startsWith("_useCPPOT"))
			{
				String action = _command.substring(10);
				try
				{
					activeChar.useItem(0, false);
				}
				catch (NumberFormatException nfe)
				{}
			}
			/*
			 * else if (!generatedRandomizedString1.equalsIgnoreCase(liveRandomizedString1))
			 * {
			 * BotsPreventionManager.getInstance().prevalidationwindow(activeChar);
			 * }
			 * else if (!generatedRandomizedString2.equalsIgnoreCase(liveRandomizedString2))
			 * {
			 * BotsPreventionManager.getInstance().prevalidationwindow(activeChar);
			 * }
			 */
			else if (_command.startsWith(_AntibotCommand1))
			{
				BotsPreventionManager.getInstance().AnalyseBypass(_command, activeChar);
			}
			else if (_command.startsWith(_AntibotCommand2))
			{
				BotsPreventionManager.getInstance().AnalyseBypass(_command, activeChar);
			}
			// Draw a Symbol
			else if (_command.equals("menu_select?ask=-16&reply=1"))
			{
				L2Object object = activeChar.getTarget();
				if (object instanceof L2Npc)
					((L2Npc) object).onBypassFeedback(activeChar, _command);
			}
			else if (_command.equals("menu_select?ask=-16&reply=2"))
			{
				L2Object object = activeChar.getTarget();
				if (object instanceof L2Npc)
					((L2Npc) object).onBypassFeedback(activeChar, _command);
			}
			// Navigate through Manor windows
			else if (_command.startsWith("manor_menu_select?"))
			{
				L2Object object = activeChar.getTarget();
				if (object instanceof L2Npc)
					((L2Npc) object).onBypassFeedback(activeChar, _command);
			}

			if (_command.startsWith("_bbsbufferbypass_"))
			{
				BBSSchemeBufferInstance.getInstance().onBypass(activeChar, _command);
				return;
			}
			else if (_command.startsWith("_bbsbuffer"))
			{
				BBSSchemeBufferInstance.getInstance().showWindow(activeChar);
				return;
			}
			else if (_command.startsWith("_bbs") || _command.contains("_maillist_0_1_0_") || _command.contains("_friendlist_0_") || _command.startsWith("bbs"))
			{
				CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if (_command.startsWith("_bbs"))
			{
				CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if (_command.startsWith("Quest "))
			{
				if (!activeChar.validateBypass(_command))
					return;
				L2PcInstance player = getClient().getActiveChar();
				if (player == null)
					return;
				String p = _command.substring(6).trim();
				int idx = p.indexOf(' ');
				if (idx < 0)
					player.processQuestEvent(p, "");
				else
					player.processQuestEvent(p.substring(0, idx), p.substring(idx).trim());
			}
			else if (_command.startsWith("achievements"))
			{
				if (!Config.L2JMOD_ACHIEVEMENT_SYSTEM)
				{
					return;
				}
				final QuestState qs = activeChar.getQuestState("Achievements");
				if (qs != null)
				{
					qs.getQuest().notifyEvent(_command, null, activeChar);
				}
			}
			else if (_command.startsWith("OlympiadArenaChange"))
			{
				Olympiad.bypassChangeArena(_command, activeChar);
			}
			else if (_command.startsWith("_herolist"))
			{
				activeChar.sendPacket(new ExHeroList());
			}
			else if (_command.startsWith("voice") && (_command.charAt(6) == '.') && (_command.length() > 7))
			{
				final String vc, vparams;
				int endOfCommand = _command.indexOf(" ", 7);
				if (endOfCommand > 0)
				{
					vc = _command.substring(7, endOfCommand).trim();
					vparams = _command.substring(endOfCommand).trim();
				}
				else
				{
					vc = _command.substring(7).trim();
					vparams = null;
				}
				if (vc.length() > 0)
				{
					IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(vc);
					if (vch != null)
					{
						vch.useVoicedCommand(vc, activeChar, vparams);
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Bad RequestBypassToServer: ", e);
		}
	}
	
	/**
	 * @param client
	 */
	private void comeHere(L2PcInstance activeChar)
	{
		L2Object obj = activeChar.getTarget();
		if (obj == null)
			return;
		if (obj instanceof L2Npc)
		{
			L2Npc temp = (L2Npc) obj;
			temp.setTarget(activeChar);
			temp.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 0));
		}
	}
	
	private void playerHelp(L2PcInstance activeChar, String path)
	{
		if (path.indexOf("..") != -1)
			return;
		StringTokenizer st = new StringTokenizer(path);
		String[] cmd = st.nextToken().split("#");
		if (cmd.length > 1)
		{
			int itemId = 0;
			itemId = Integer.parseInt(cmd[1]);
			String filename = "data/html/help/" + cmd[0];
			NpcHtmlMessage html = new NpcHtmlMessage(1, itemId);
			html.setFile(filename);
			html.disableValidation();
			activeChar.sendPacket(html);
		}
		else
		{
			String filename = "data/html/help/" + path;
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile(filename);
			html.disableValidation();
			activeChar.sendPacket(html);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__21_REQUESTBYPASSTOSERVER;
	}
}
