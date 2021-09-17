package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import Alpha.skillGuard.StuckSubGuard;
import guard.HwidController;
import luna.custom.captcha.instancemanager.BotsPreventionManager;
import luna.custom.email.EmailRegistration;
import luna.custom.guard.LunaSkillGuard;
import luna.custom.handler.DressMeTimeChecker;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.TaskPriority;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.datatables.CharSchemesTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.CrownManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2ClassMasterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TeleporterInstance;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Couple;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.entity.FortSiege;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import net.sf.l2j.gameserver.model.events.manager.EventVarHolder;
import net.sf.l2j.gameserver.model.events.newEvents.NewCTF;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDomination;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ClassUpgradeWnd;
import net.sf.l2j.gameserver.network.serverpackets.Die;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExBasicActionList;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import net.sf.l2j.gameserver.network.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.network.serverpackets.FriendList;
import net.sf.l2j.gameserver.network.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.network.serverpackets.PledgeStatusChanged;
import net.sf.l2j.gameserver.network.serverpackets.QuestList;
import net.sf.l2j.gameserver.network.serverpackets.SSQInfo;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

public class EnterWorld extends L2GameClientPacket
{
	private static final String _C__03_ENTERWORLD = "[C] 03 EnterWorld";
	
	public TaskPriority getPriority()
	{
		return TaskPriority.PR_URGENT;
	}
	
	@Override
	protected void readImpl()
	{}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			_log.warning("EnterWorld failed! activeChar returned 'null'.");
			getClient().closeNow();
			return;
		}
		if (!activeChar.getClient().getFullHwid().isEmpty())
		{
			L2GameClient client = activeChar.getClient();
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement;
				statement = con.prepareStatement("UPDATE accounts set lasthwid = ? where login = ?");
				// TODO Local Changes
				statement.setString(1, client.getFullHwid());
				// statement.setString(1, "LOCAL HWID");
				statement.setString(2, activeChar.getAccountName());
				statement.execute();
				statement.close();
				activeChar.sendMessage("Hwid Successfully parsed.");
			}
			catch (Exception e)
			{
				_log.severe("error on parsing hwid");
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
			// activeChar.sendPacket(new ServerToClientCommunicationPacket("L2Trinity"));
		}
		if (!activeChar.getClient().getFullHwid().isEmpty())
		{
			HwidController.getInstance().storeHwid(activeChar.getAccountName(), activeChar.getName(), activeChar.getHWID(), System.currentTimeMillis());
		}
		// Restore to instanced area if enabled
		if (Config.RESTORE_PLAYER_INSTANCE)
			activeChar.setInstanceId(InstanceManager.getInstance().getPlayerInstanceId(activeChar.getObjectId()));
		else
		{
			int instanceId = InstanceManager.getInstance().getPlayerInstanceId(activeChar.getObjectId());
			if (instanceId > 0)
				InstanceManager.getInstance().getInstance(instanceId).removePlayer(activeChar.getObjectId());
		}
		if (L2World.getInstance().findObject(activeChar.getObjectId()) != null)
		{
			if (Config.DEBUG)
				_log.warning("User already exists in Object ID map! User " + activeChar.getName() + " is a character clone.");
		}
		// Apply special GM properties to the GM when entering
		DressMeTimeChecker.getInstance().checkForDressTimers(activeChar);
		if (activeChar.isGM())
		{
			if (Config.GM_STARTUP_INVULNERABLE && AdminCommandAccessRights.getInstance().hasAccess("admin_invul", activeChar))
				activeChar.setIsInvul(true);
			if (Config.GM_STARTUP_INVISIBLE && AdminCommandAccessRights.getInstance().hasAccess("admin_invisible", activeChar))
				activeChar.setInvisible(true);
			if (Config.GM_STARTUP_SILENCE && AdminCommandAccessRights.getInstance().hasAccess("admin_silence", activeChar) || activeChar.getAccessLevel().getLevel() > 6)
				activeChar.setMessageRefusal(true);
			if (activeChar.getAccessLevel().getLevel() > 1)
			{
				BlockList.setBlockAll(activeChar, true);
				activeChar.setTradeRefusal(true);
			}
			if (Config.GM_STARTUP_AUTO_LIST && AdminCommandAccessRights.getInstance().hasAccess("admin_gmliston", activeChar) && activeChar.getAccessLevel().getLevel() < 7)
				GmListTable.getInstance().addGm(activeChar, false);
			else
				GmListTable.getInstance().addGm(activeChar, true);
		}
		else
		{
			activeChar.getFloodProtectors().getTradeChat().tryPerformAction("trade chat");
			activeChar.getFloodProtectors().getShout().tryPerformAction("shout");
			activeChar.getFloodProtectors().getHeroVoice().tryPerformAction("hero voice");
		}
		activeChar.cklastlogin();
		// activeChar.setNameColorsMethod();
		activeChar.setNameColorsDueToPVP();
		// Set dead status if applies
		if (activeChar.getCurrentHp() < 0.5)
			activeChar.setIsDead(true);
		// Set Hero status if it applies
		if (Hero.getInstance().getHeroes() != null && Hero.getInstance().getHeroes().containsKey(activeChar.getObjectId()))
		{
			activeChar.setHero(true);
			activeChar._previousMonthOlympiadGamesPlayed = Olympiad.getInstance().getLastNobleOlympiadGamesPlayed(activeChar.getObjectId());
		}
		setPledgeClass(activeChar);
		boolean showClanNotice = false;
		// Clan related checks are here
		if (activeChar.getClan() != null)
		{
			activeChar.sendPacket(new PledgeSkillList(activeChar.getClan()));
			notifyClanMembers(activeChar);
			notifySponsorOrApprentice(activeChar);
			if (activeChar.isClanLeader())
			{
				final ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan());
				if (clanHall != null)
				{
					if (!clanHall.getPaid())
						activeChar.sendPacket(new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW));
				}
			}
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				if (!siege.getIsInProgress())
					continue;
				if (siege.checkIsAttacker(activeChar.getClan()))
					activeChar.setSiegeState((byte) 1);
				else if (siege.checkIsDefender(activeChar.getClan()))
					activeChar.setSiegeState((byte) 2);
			}
			for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
			{
				if (!siege.getIsInProgress())
					continue;
				if (siege.checkIsAttacker(activeChar.getClan()))
					activeChar.setSiegeState((byte) 1);
				else if (siege.checkIsDefender(activeChar.getClan()))
					activeChar.setSiegeState((byte) 2);
			}
			sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
			sendPacket(new PledgeStatusChanged(activeChar.getClan()));
			// Residential skills support
			if (activeChar.getClan().getHasCastle() > 0)
				CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).giveResidentialSkills(activeChar);
			if (activeChar.getClan().getHasFort() > 0)
				FortManager.getInstance().getFortByOwner(activeChar.getClan()).giveResidentialSkills(activeChar);
			showClanNotice = activeChar.getClan().isNoticeEnabled();
			if (activeChar.getClan() != null)
			{
				showClanNotice = activeChar.getClan().isNoticeEnabled();
			}
		}
		sendPacket(new SSQInfo());
		// Updating Seal of Strife Buff/Debuff
		if (SevenSigns.getInstance().isSealValidationPeriod() && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL)
		{
			if (SevenSigns.getInstance().getPlayerCabal(activeChar) != SevenSigns.CABAL_NULL)
			{
				if (SevenSigns.getInstance().getPlayerCabal(activeChar) == SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
					activeChar.addSkill(SkillTable.getInstance().getInfo(5074, 1));
				else
					activeChar.addSkill(SkillTable.getInstance().getInfo(5075, 1));
			}
		}
		else
		{
			activeChar.removeSkill(SkillTable.getInstance().getInfo(5074, 1));
			activeChar.removeSkill(SkillTable.getInstance().getInfo(5075, 1));
		}
		if (Config.ENABLE_VITALITY && Config.RECOVER_VITALITY_ON_RECONNECT)
		{
			float points = Config.RATE_RECOVERY_ON_RECONNECT * (System.currentTimeMillis() - activeChar.getLastAccess()) / 60000;
			if (points > 0)
				activeChar.updateVitalityPoints(points, false, true);
		}
		/// final File mainText = new File(Config.DATAPACK_ROOT, "data/html/welcome.htm"); // Return the pathfile of the HTML file
		/*
		 * if (mainText.exists())
		 * {
		 * NpcHtmlMessage html = new NpcHtmlMessage(1);
		 * html.setFile("data/html/welcome.htm");
		 * html.replace("%name%", activeChar.getName()); // replaces %name% with activeChar.getName(), so you can say like "welcome to the server %name%"
		 * sendPacket(html);
		 * }
		 */
		// Send Macro List
		activeChar.getMacroses().sendUpdate();
		/*
		 * if (activeChar.isNoble() && activeChar.getInventory().getItemByItemId(7694) == null && activeChar.getWarehouse().getItemByItemId(7694) == null)
		 * activeChar.addItem("Noblesse Circlet", 7694, 1, activeChar, true);
		 */
		// Send GG check
		/* activeChar.queryGameGuard(); */
		// Send Teleport Bookmark List
		sendPacket(new ExGetBookMarkInfoPacket(activeChar));
		// Wedding Checks
		if (Config.L2JMOD_ALLOW_WEDDING)
		{
			engage(activeChar);
			notifyPartner(activeChar, activeChar.getPartnerId());
			activeChar.giveMarriageSkills();
		}
		// check for crowns
		CrownManager.checkCrowns(activeChar);
		// Send Item List
		sendPacket(new ItemList(activeChar, false));
		activeChar.sendSkillList();
		LunaSkillGuard.getInstance().checkForIncorrectSkills(activeChar);
		// Send Shortcuts
		sendPacket(new ShortCutInit(activeChar));
		// Send Action list
		activeChar.sendPacket(ExBasicActionList.DEFAULT_ACTION_LIST);
		// Send Dye Information
		activeChar.sendPacket(new HennaInfo(activeChar));
		sendPacket(new UserInfo(activeChar));
		sendPacket(new ExBrExtraUserInfo(activeChar));
		activeChar.canSendUserInfo = true;
		Quest.playerEnter(activeChar);
		loadTutorial(activeChar);
		getClient().loadMarriageStatus();
		for (Quest quest : QuestManager.getInstance().getAllManagedScripts())
		{
			if (quest != null && quest.getOnEnterWorld())
				quest.notifyEnterWorld(activeChar);
		}
		activeChar.sendPacket(new QuestList());
		if (activeChar.isInFT() && !GameTimeController.getInstance().isNowNight() && !activeChar.isGM())
		{
			activeChar.setInsideZone(L2Character.ZONE_CHAOTIC, false);
			activeChar.getActingPlayer().setIsInFT(false);
			activeChar.sendMessage("Pagan's Temple disabled atm.");
			activeChar.setIsPendingRevive(true);
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					try
					{
						L2TeleporterInstance.doTeleport(activeChar.getActingPlayer(), 40006);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}, 5000);
			return;
		}
		if (!activeChar.isInsideZone(L2Character.ZONE_FARM))
		{
			if (Config.PLAYER_SPAWN_PROTECTION > 0)
				activeChar.setProtection(true);
		}
		activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		if (L2Event.active && L2Event.connectionLossData.containsKey(activeChar.getName()) && L2Event.isOnEvent(activeChar))
			L2Event.restoreChar(activeChar);
		else if (L2Event.connectionLossData.containsKey(activeChar.getName()))
			L2Event.restoreAndTeleChar(activeChar);
		sendPacket(new SystemMessage(SystemMessageId.WELCOME_TO_LINEAGE));
		activeChar.updateEffectIcons();
		activeChar.sendPacket(new EtcStatusUpdate(activeChar));
		// Expand Skill
		activeChar.sendPacket(new ExStorageMaxCount(activeChar));
		sendPacket(new FriendList(activeChar));
		sendPacket(new SkillCoolTime(activeChar));
		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
		Announcements.getInstance().showAnnouncements(activeChar);
		if (Config.MAIL_ENABLED)
		{
			if (activeChar.hasEmailRegistered() && !EmailRegistration.checkBinds(activeChar))
			{
				EmailRegistration.showWindow(activeChar, 4);
				showClanNotice = false;
			}
			else if (!activeChar.hasEmailRegistered())
			{
				EmailRegistration.showWindow(activeChar, 1);
				showClanNotice = false;
			}
			else if (EmailRegistration.checkBinds(activeChar))
			{
				if (activeChar.getLockdownTime() != 0)
				{
					activeChar.setLockdownTime(0);
					activeChar.doUnLockdown();
				}
			}
		}
		if (showClanNotice)
		{
			NpcHtmlMessage notice = new NpcHtmlMessage(1);
			notice.setFile("data/html/clanNotice.htm");
			notice.replace("%clan_name%", activeChar.getClan().getName());
			String notice1 = activeChar.getClan().getNotice();
			notice1 = notice1.replaceAll("\r\n", "<br>");
			notice1 = notice1.replace("[", "\\[");
			notice1 = notice1.replace("]", "\\]");
			notice1 = notice1.replace("(", "\\(");
			notice1 = notice1.replace(")", "\\)");
			notice1 = notice1.replace("&", "\\&");
			notice1 = notice1.replace("@", "\\@");
			notice1 = notice1.replace("{", "\\{");
			notice1 = notice1.replace("}", "\\}");
			notice1 = notice1.replace("?", "\\?");
			notice1 = notice1.replace("+", "\\+");
			notice1 = notice1.replace("-", "\\-");
			notice1 = notice1.replace("=", "\\=");
			notice1 = notice1.replace("^", "\\^");
			notice.replace("%notice_text%", notice1);
			sendPacket(notice);
		}
		else if (Config.SERVER_NEWS)
		{
			String serverNews = HtmCache.getInstance().getHtm("data/html/servnews.htm");
			if (serverNews != null)
				sendPacket(new NpcHtmlMessage(1, serverNews));
		}
		if (Config.PETITIONING_ALLOWED)
			PetitionManager.getInstance().checkPetitionMessages(activeChar);
		if (activeChar.isAlikeDead()) // dead or fake dead
		{
			// no broadcast needed since the player will already spawn dead to others
			sendPacket(new Die(activeChar));
		}
		notifyFriends(activeChar);
		CharSchemesTable.getInstance().onPlayerLogin(activeChar.getObjectId());
		// classpaths
		checkForClassPaths(activeChar.getObjectId());
		activeChar.cleanUpPathSkills();
		loadClassPathPoints(activeChar);
		loadClassPathPoints2(activeChar);
		boolean foundCupidBow = false;
		for (L2ItemInstance i : activeChar.getInventory().getItems())
		{
			// if(i.isFromPvPNpc() && i.getUntradeableTime() > 0)
			// {
			// i.setUntradeableTimer(0);
			// activeChar.sendMessage(i.getItemName() + " is now tradeable.");
			// }
			if (i.isHeroItem())
			{
				if (!activeChar.isHero() || (!activeChar.canUseHeroItems() && i.isWeapon()))
				{
					activeChar.destroyItem("Removing Hero Item", i, activeChar, false);
				}
			}
			else if (i.isTimeLimitedItem())
			{
				i.scheduleLifeTimeTask();
			}
			else if (i.getItemId() == 9140)
			{
				foundCupidBow = true;
				if (!activeChar.isGM() && !activeChar.isThisCharacterMarried())
				{
					activeChar.destroyItem("Removing Cupid Bow", i, activeChar, false);
					activeChar.getInventory().updateDatabase();
				}
			}
		}
		if (!foundCupidBow)
		{
			if (activeChar.isThisCharacterMarried())
			{
				if (activeChar.getWarehouse().getItemByItemId(9140) == null)
				{
					activeChar.addItem("Cupid Bow", 9140, 1, activeChar, true);
					activeChar.getInventory().updateDatabase();
				}
			}
			else if (!activeChar.isGM())
			{
				final L2ItemInstance bow = activeChar.getWarehouse().getItemByItemId(9140);
				if (bow != null)
				{
					activeChar.getWarehouse().destroyItem("Removing Cupid Bow", bow, activeChar, activeChar);
					activeChar.getWarehouse().updateDatabase();
				}
			}
		}
		for (L2ItemInstance i : activeChar.getWarehouse().getItems())
		{
			if (i.isHeroItem())
			{
				if (!activeChar.isHero() || (!activeChar.canUseHeroItems() && i.isWeapon()))
				{
					activeChar.destroyItem("Removing Hero Item", i, activeChar, false);
				}
			}
			else if (i.isTimeLimitedItem())
			{
				i.scheduleLifeTimeTask();
			}
		}
		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED));
		// remove combat flag before teleporting
		if (activeChar.getInventory().getItemByItemId(9819) != null)
		{
			Fort fort = FortManager.getInstance().getFort(activeChar);
			if (fort != null)
				FortSiegeManager.getInstance().dropCombatFlag(activeChar);
			else
			{
				int slot = activeChar.getInventory().getSlotFromItem(activeChar.getInventory().getItemByItemId(9819));
				activeChar.getInventory().unEquipItemInBodySlotAndRecord(slot);
				activeChar.destroyItem("CombatFlag", activeChar.getInventory().getItemByItemId(9819), null, true);
			}
		}
		RegionBBSManager.getInstance().changeCommunityBoard();
		TvTEvent.onLogin(activeChar);
		
		if (L2ClassMasterInstance.hasValidClasses(activeChar))
			activeChar.sendPacket(new ClassUpgradeWnd(activeChar));
		if (BotsPreventionManager.get_validation() != null && BotsPreventionManager.get_validation().containsKey(activeChar.getObjectId()))
		{
			BotsPreventionManager.getInstance().validationtasks(activeChar);
		}

		StuckSubGuard.getInstance().checkPlayer(activeChar);
		sendPacket(ActionFailed.STATIC_PACKET);
		ThreadPoolManager.getInstance().scheduleGeneral(new teleportTask(activeChar), 250);
		// DonationManager.getInstance().checkRewards(activeChar);
		// activeChar.checkForIncorrectSkills();
	}
	
	public class teleportTask implements Runnable
	{
		final L2PcInstance _player;
		
		public teleportTask(L2PcInstance player)
		{
			_player = player;
		}
		
		public void run()
		{
			try
			{
				if (_player != null)
				{
					if ((TvT._started || TvT._teleport) && TvT._savePlayers.contains(_player.getObjectId()))
					{
						TvT.addDisconnectedPlayer(_player);
					}
					if ((NewTvT._started || NewTvT._teleport) && NewTvT._savePlayers.contains(_player.getObjectId()))
					{
						NewTvT.addDisconnectedPlayer(_player);
					}
					if ((NewDomination._started || NewDomination._teleport) && NewDomination._savePlayers.contains(_player.getObjectId()))
					{
						NewDomination.addDisconnectedPlayer(_player, EventVarHolder.getInstance().getRunningEventId());
					}
					else if ((CTF._started || CTF._teleport) && CTF._savePlayers.contains(_player.getObjectId()))
					{
						CTF.addDisconnectedPlayer(_player);
					}
					else if ((NewCTF._started || NewCTF._teleport) && NewCTF._savePlayers.contains(_player.getObjectId()))
					{
						NewCTF.addDisconnectedPlayer(_player);
					}
					else if (((FOS._started || NewFOS._started) || FOS._teleport) && FOS._savePlayers.contains(_player.getObjectId()))
					{
						FOS.addDisconnectedPlayer(_player);
					}
					else if (((NewFOS._started) || NewFOS._teleport) && NewFOS._savePlayers.contains(_player.getObjectId()))
					{
						NewFOS.addDisconnectedPlayer(_player);
					}
					else if ((DM._started || DM._teleport) && DM._savePlayers.contains(_player.getObjectId()))
					{
						DM.addDisconnectedPlayer(_player);
					}
					else if ((NewDM._started || NewDM._teleport) && NewDM._savePlayers.contains(_player.getObjectId()))
					{
						NewDM.addDisconnectedPlayer(_player);
					}
					else if ((NewHuntingGrounds._started || NewHuntingGrounds._teleport) && NewHuntingGrounds._savePlayers.contains((_player.getObjectId())))
					{
						NewHuntingGrounds.addDisconnectedPlayer(_player);
					}
					else if (VIP._savePlayers.contains(_player.getObjectId()))
					{
						VIP.addDisconnectedPlayer(_player);
					}
					else if (!_player.isGM())
					{
						_player.getWorldRegion().revalidateZones(_player);
						if (DimensionalRiftManager.getInstance().checkIfInRiftZone(_player.getX(), _player.getY(), _player.getZ(), false))
						{
							DimensionalRiftManager.getInstance().teleportToWaitingRoom(_player);
						}
						else if (Olympiad.getInstance().playerInStadia(_player))
						{
							_player.sendMessage("You are being ported to town because you are in an Olympiad stadium");
							_player.setIsPendingRevive(true);
							_player.teleToLocation(TeleportWhereType.Town);
						}
						else if (_player.getSiegeState() < 2 && _player.isInsideZone(L2Character.ZONE_SIEGE))
						{
							_player.sendMessage("You are being ported to town because you are in an active siege zone");
							_player.setIsPendingRevive(true);
							_player.teleToLocation(TeleportWhereType.Town);
						}
						else if (_player.isInHuntersVillage())
						{
							_player.setIsPendingRevive(true);
							_player.teleToLocation(TeleportWhereType.Town);
						}
						else if (_player.isInOrcVillage())
						{
							_player.setIsPendingRevive(true);
							_player.teleToLocation(TeleportWhereType.Town);
						}
						else if (_player.isInsideZone(L2Character.ZONE_PEACE) || _player.isInsideZone(L2Character.ZONE_CLANHALL) || _player.isInsideZone(L2Character.ZONE_FORT) || _player.isInsideZone(L2Character.ZONE_TOWN) || _player.isInsideZone(L2Character.ZONE_CASTLE) || _player.isInJail() || _player.isInGludin())
						{
							// do nothing
						}
						else if (_player.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
						{
							_player.sendMessage("You are being ported to town because you are in an no-recall zone");
							_player.setIsPendingRevive(true);
							_player.teleToLocation(TeleportWhereType.Town);
						}
						else if (System.currentTimeMillis() - _player.getLastAccess() >= 2700000) // 45 mins of not logging in
						{
							_player.sendMessage("You are being ported to town due to inactivity");
							_player.setIsPendingRevive(true);
							_player.teleToLocation(83477, 148638, -3404);
						}
						else if (System.currentTimeMillis() - _player.getLastAccess() >= 600000 && _player.isInsideZone(L2Character.ZONE_RAID)) // 10 mins of not logging in
						{
							_player.sendMessage("You are being ported to town due to inactivity");
							_player.setIsPendingRevive(true);
							_player.teleToLocation(83477, 148638, -3404);
						}
					}
					_player.onPlayerEnter();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @param activeChar
	 */
	private void engage(L2PcInstance cha)
	{
		int _chaid = cha.getObjectId();
		for (Couple cl : CoupleManager.getInstance().getCouples())
		{
			if (cl.getPlayer1Id() == _chaid || cl.getPlayer2Id() == _chaid)
			{
				if (cl.getMaried())
					cha.setIsThisCharacterMarried(true);
				cha.setCoupleId(cl.getId());
				if (cl.getPlayer1Id() == _chaid)
					cha.setPartnerId(cl.getPlayer2Id());
				else
					cha.setPartnerId(cl.getPlayer1Id());
			}
		}
	}
	
	/**
	 * @param activeChar
	 *            partnerid
	 */
	private void notifyPartner(L2PcInstance cha, int partnerId)
	{
		if (cha.getPartnerId() != 0)
		{
			L2PcInstance partner;
			int objId = cha.getPartnerId();
			try
			{
				partner = (L2PcInstance) L2World.getInstance().findObject(cha.getPartnerId());
				if (partner != null)
					partner.sendMessage("Your Partner has logged in.");
				partner = null;
			}
			catch (ClassCastException cce)
			{
				_log.warning("Wedding Error: ID " + objId + " is now owned by a(n) " + L2World.getInstance().findObject(objId).getClass().getSimpleName());
			}
		}
	}
	
	public static void checkForClassPaths(int objid)
	{
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			{
				statement = con.prepareStatement("SELECT * FROM class_paths WHERE objid = ?");
				statement.setInt(1, objid);
				ResultSet rset = statement.executeQuery();
				if (!rset.next())
				{
					PreparedStatement statement2 = con.prepareStatement("INSERT INTO class_paths VALUES (?,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);");
					statement2.setInt(1, objid);
					statement2.executeUpdate();
					statement2.close();
					rset.close();
					statement2.close();
					statement.close();
				}
				else
					return;
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed inserting classpath  on player: " + String.valueOf(objid), e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	private void loadClassPathPoints(L2PcInstance activeChar)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			String selectedvalues = "MiddleOff,LeftOff,LeftOff1,LeftOff2,LeftOff1_1,LeftOff1_2,LeftOff2_1,LeftOff2_2,RightOff,RightOff1,RightOff2,RightOff1_1,RightOff1_2,RightOff2_1,RightOff2_2,MiddleMage,LeftMage,LeftMage1,LeftMage2,RightMage,RightMage1,RightMage2,RightMage1_1,RightMage1_2,RightMage2_1,RightMage2_2,LeftMage1_1,LeftMage1_2,LeftMage2_1,LeftMage2_2,MiddleDef,LeftDef,LeftDef1,LeftDef2,RightDef,RightDef1,RightDef2,RightDef1_1,RightDef1_2,RightDef2_1,RightDef2_2,LeftDef1_1,LeftDef1_2,LeftDef2_1,LeftDef2_2,MiddleSup,LeftSup,LeftSup1,LeftSup2,RightSup,RightSup1,RightSup2,RightSup1_1,RightSup1_2,RightSup2_1,RightSup2_2,LeftSup1_1,LeftSup1_2,LeftSup2_1,LeftSup2_2";
			PreparedStatement statement = con.prepareStatement("SELECT " + selectedvalues + " FROM class_paths where objid =?");
			statement.setInt(1, activeChar.getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				int MiddleOff = rset.getInt("MiddleOff");
				int LeftOff = rset.getInt("LeftOff");
				int LeftOff1 = rset.getInt("LeftOff1");
				int LeftOff2 = rset.getInt("LeftOff2");
				int LeftOff1_1 = rset.getInt("LeftOff1_1");
				int LeftOff1_2 = rset.getInt("LeftOff1_2");
				int LeftOff2_1 = rset.getInt("LeftOff2_1");
				int LeftOff2_2 = rset.getInt("LeftOff2_2");
				int RightOff = rset.getInt("RightOff");
				int RightOff1 = rset.getInt("RightOff1");
				int RightOff2 = rset.getInt("RightOff2");
				int RightOff1_1 = rset.getInt("RightOff1_1");
				int RightOff1_2 = rset.getInt("RightOff1_2");
				int RightOff2_1 = rset.getInt("RightOff2_1");
				int RightOff2_2 = rset.getInt("RightOff2_2");
				int MiddleMage = rset.getInt("MiddleMage");
				int LeftMage = rset.getInt("LeftMage");
				int LeftMage1 = rset.getInt("LeftMage1");
				int LeftMage2 = rset.getInt("LeftMage2");
				int RightMage = rset.getInt("RightMage");
				int RightMage1 = rset.getInt("RightMage1");
				int RightMage2 = rset.getInt("RightMage2");
				int RightMage1_1 = rset.getInt("RightMage1_1");
				int RightMage1_2 = rset.getInt("RightMage1_2");
				int RightMage2_1 = rset.getInt("RightMage2_1");
				int RightMage2_2 = rset.getInt("RightMage2_2");
				int LeftMage1_1 = rset.getInt("LeftMage1_1");
				int LeftMage1_2 = rset.getInt("LeftMage1_2");
				int LeftMage2_1 = rset.getInt("LeftMage2_1");
				int LeftMage2_2 = rset.getInt("LeftMage2_2");
				int MiddleDef = rset.getInt("MiddleDef");
				int LeftDef = rset.getInt("LeftDef");
				int LeftDef1 = rset.getInt("LeftDef1");
				int LeftDef2 = rset.getInt("LeftDef2");
				int RightDef = rset.getInt("RightDef");
				int RightDef1 = rset.getInt("RightDef1");
				int RightDef2 = rset.getInt("RightDef2");
				int RightDef1_1 = rset.getInt("RightDef1_1");
				int RightDef1_2 = rset.getInt("RightDef1_2");
				int RightDef2_1 = rset.getInt("RightDef2_1");
				int RightDef2_2 = rset.getInt("RightDef2_2");
				int LeftDef1_1 = rset.getInt("LeftDef1_1");
				int LeftDef1_2 = rset.getInt("LeftDef1_2");
				int LeftDef2_1 = rset.getInt("LeftDef2_1");
				int LeftDef2_2 = rset.getInt("LeftDef2_2");
				int MiddleSup = rset.getInt("MiddleSup");
				int LeftSup = rset.getInt("LeftSup");
				int LeftSup1 = rset.getInt("LeftSup1");
				int LeftSup2 = rset.getInt("LeftSup2");
				int RightSup = rset.getInt("RightSup");
				int RightSup1 = rset.getInt("RightSup1");
				int RightSup2 = rset.getInt("RightSup2");
				int RightSup1_1 = rset.getInt("RightSup1_1");
				int RightSup1_2 = rset.getInt("RightSup1_2");
				int RightSup2_1 = rset.getInt("RightSup2_1");
				int RightSup2_2 = rset.getInt("RightSup2_2");
				int LeftSup1_1 = rset.getInt("LeftSup1_1");
				int LeftSup1_2 = rset.getInt("LeftSup1_2");
				int LeftSup2_1 = rset.getInt("LeftSup2_1");
				int LeftSup2_2 = rset.getInt("LeftSup2_2");
				if (MiddleOff == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9421, 1);
					activeChar.addSkill(Skill, true);
					activeChar.decCpPoints();
					activeChar.setMiddleOff(MiddleOff);
				}
				if (MiddleOff == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9421, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9422, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setMiddleOff(MiddleOff);
				}
				if (MiddleOff == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9421, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9422, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9423, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setMiddleOff(MiddleOff);
				}
				if (LeftOff == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9424, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftOff(LeftOff);
				}
				if (LeftOff == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9424, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9425, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftOff(LeftOff);
				}
				if (LeftOff == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9424, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9425, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9426, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftOff(LeftOff);
				}
				if (LeftOff1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9427, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftOff1(LeftOff1);
				}
				if (LeftOff1 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9427, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9428, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftOff1(LeftOff1);
				}
				if (LeftOff1 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9427, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9428, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9429, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftOff1(LeftOff1);
				}
				if (LeftOff2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9430, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftOff2(LeftOff2);
				}
				if (LeftOff2 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9430, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9431, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftOff2(LeftOff2);
				}
				if (LeftOff2 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9430, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9431, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9432, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftOff2(LeftOff2);
				}
				if (LeftOff1_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94270, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftOff1_1(LeftOff1_1);
				}
				if (LeftOff1_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94271, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftOff1_2(LeftOff1_2);
				}
				if (LeftOff2_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94300, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftOff2_1(LeftOff2_1);
				}
				if (LeftOff2_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94302, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftOff2_2(LeftOff2_2);
				}
				if (RightOff1_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94350, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightOff1_1(RightOff1_1);
				}
				if (RightOff1_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94351, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightOff1_2(RightOff1_2);
				}
				if (RightOff2_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94390, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightOff2_1(RightOff2_1);
				}
				if (RightOff2_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94391, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightOff2_2(RightOff2_2);
				}
				if (RightOff == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9433, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightOff(RightOff);
				}
				if (RightOff == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9433, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9434, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightOff(RightOff);
				}
				if (RightOff == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9433, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9434, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9435, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightOff(RightOff);
				}
				if (RightOff1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9436, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightOff1(RightOff1);
				}
				if (RightOff1 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9436, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9437, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightOff1(RightOff1);
				}
				if (RightOff1 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9436, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9437, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9438, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightOff1(RightOff1);
				}
				if (RightOff2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9439, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightOff2(RightOff2);
				}
				if (RightOff2 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9440, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9441, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightOff2(RightOff2);
				}
				if (RightOff2 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9439, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9440, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9441, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightOff2(RightOff2);
				}
				if (MiddleMage == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9442, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setMiddleMage(MiddleMage);
				}
				if (MiddleMage == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9442, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9443, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setMiddleMage(MiddleMage);
				}
				if (MiddleMage == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9442, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9443, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9444, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setMiddleMage(MiddleMage);
				}
				if (LeftMage == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9445, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftMage(LeftMage);
				}
				if (LeftMage == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9445, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9446, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftMage(LeftMage);
				}
				if (LeftMage == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9445, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9446, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9447, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftMage(LeftMage);
				}
				if (LeftMage1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9448, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftMage1(LeftMage1);
				}
				if (LeftMage1 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9448, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9449, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftMage1(LeftMage1);
				}
				if (LeftMage1 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9448, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9449, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9450, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftMage1(LeftMage1);
				}
				if (LeftMage2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9451, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftMage2(LeftMage2);
				}
				if (LeftMage2 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9451, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9452, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftMage2(LeftMage2);
				}
				if (LeftMage2 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9451, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9452, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9453, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftMage2(LeftMage2);
				}
				if (LeftMage1_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94501, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftMage1_1(LeftMage1_1);
				}
				if (LeftMage1_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94504, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftMage1_2(LeftMage1_2);
				}
				if (LeftMage2_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94510, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftMage2_1(LeftMage2_1);
				}
				if (LeftMage2_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94512, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftMage2_2(LeftMage2_2);
				}
				if (RightMage1_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94570, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightMage1_1(RightMage1_1);
				}
				if (RightMage1_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94571, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightMage1_2(RightMage1_2);
				}
				if (RightMage2_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94600, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightMage2_1(RightMage2_1);
				}
				if (RightMage2_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94603, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightMage2_2(RightMage2_2);
				}
				if (RightMage == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9454, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightMage(RightMage);
				}
				if (RightMage == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9454, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9455, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightMage(RightMage);
				}
				if (RightMage == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9454, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9455, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9456, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightMage(RightMage);
				}
				if (RightMage1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9457, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightMage1(RightMage1);
				}
				if (RightMage1 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9457, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9458, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightMage1(RightMage1);
				}
				if (RightMage1 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9457, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9458, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9459, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightMage1(RightMage1);
				}
				if (RightMage2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9460, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightMage2(RightMage2);
				}
				if (RightMage2 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9460, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9461, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightMage2(RightMage2);
				}
				if (RightMage2 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9460, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9461, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9462, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightMage2(RightMage2);
				}
				if (MiddleDef == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9463, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setMiddleDef(MiddleDef);
				}
				if (MiddleDef == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9463, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9464, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setMiddleDef(MiddleDef);
				}
				if (MiddleDef == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9463, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9464, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9465, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setMiddleDef(MiddleDef);
				}
				if (LeftDef == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9466, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftDef(LeftDef);
				}
				if (LeftDef == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9466, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9467, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftDef(LeftDef);
				}
				if (LeftDef == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9466, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9467, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9468, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftDef(LeftDef);
				}
				if (LeftDef1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9469, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftDef1(LeftDef1);
				}
				if (LeftDef1 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9469, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9470, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftDef1(LeftDef1);
				}
				if (LeftDef1 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9469, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9470, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9471, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftDef1(LeftDef1);
				}
				if (LeftDef2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9472, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftDef2(LeftDef2);
				}
				if (LeftDef2 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9472, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9473, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftDef2(LeftDef2);
				}
				if (LeftDef2 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9472, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9473, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9474, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftDef2(LeftDef2);
				}
				if (LeftDef1_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94710, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftDef1_1(LeftDef1_1);
				}
				if (LeftDef1_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94711, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftDef1_2(LeftDef1_2);
				}
				if (LeftDef2_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94740, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftDef2_1(LeftDef2_1);
				}
				if (LeftDef2_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94741, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftDef2_2(LeftDef2_2);
				}
				if (RightDef1_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94770, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightDef1_1(RightDef1_1);
				}
				if (RightDef1_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94772, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightDef1_2(RightDef1_2);
				}
				if (RightDef2_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94830, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightDef2_1(RightDef2_1);
				}
				if (RightDef2_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94832, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightDef2_2(RightDef2_2);
				}
				if (RightDef == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9475, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightDef(RightDef);
				}
				if (RightDef == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9475, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9476, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightDef(RightDef);
				}
				if (RightDef == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9475, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9476, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9477, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightDef(RightDef);
				}
				if (RightDef1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9478, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightDef1(RightDef1);
				}
				if (RightDef1 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9478, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9479, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightDef1(RightDef1);
				}
				if (RightDef1 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9478, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9479, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9480, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightDef1(RightDef1);
				}
				if (RightDef2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9481, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightDef2(RightDef2);
				}
				if (RightDef2 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9481, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9482, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightDef2(RightDef2);
				}
				if (RightDef2 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9481, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9482, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9483, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightDef2(RightDef2);
				}
				if (MiddleSup == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9484, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setMiddleSup(MiddleSup);
				}
				if (MiddleSup == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9484, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9485, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setMiddleSup(MiddleSup);
				}
				if (MiddleSup == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9484, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9485, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9486, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setMiddleSup(MiddleSup);
				}
				if (LeftSup == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9487, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftSup(LeftSup);
				}
				if (LeftSup == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9487, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9488, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftSup(LeftSup);
				}
				if (LeftSup == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9487, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9488, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9489, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftSup(LeftSup);
				}
				if (LeftSup1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9490, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftSup1(LeftSup1);
				}
				if (LeftSup1 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9490, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9491, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftSup1(LeftSup1);
				}
				if (LeftSup1 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9490, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9491, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9492, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftSup1(LeftSup1);
				}
				if (LeftSup2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9493, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftSup2(LeftSup2);
				}
				if (LeftSup2 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9493, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9494, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setLeftSup2(LeftSup2);
				}
				if (LeftSup2 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9493, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9494, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9495, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setLeftSup2(LeftSup2);
				}
				if (LeftSup1_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94920, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftSup1_1(LeftSup1_1);
				}
				if (LeftSup1_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94922, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftSup1_2(LeftSup1_2);
				}
				if (LeftSup2_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94950, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftSup2_1(LeftSup2_1);
				}
				if (LeftSup2_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(94952, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setLeftSup2_2(LeftSup2_2);
				}
				if (RightSup1_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(95010, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightSup1_1(RightSup1_1);
				}
				if (RightSup1_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(95012, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightSup1_2(RightSup1_2);
				}
				if (RightSup2_1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(95040, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightSup2_1(RightSup2_1);
				}
				if (RightSup2_2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(95041, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightSup2_2(RightSup2_2);
				}
				if (RightSup == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9496, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightSup(RightSup);
				}
				if (RightSup == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9496, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9497, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightSup(RightSup);
				}
				if (RightSup == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9496, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9497, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9498, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightSup(RightSup);
				}
				if (RightSup1 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9499, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightSup1(RightSup1);
				}
				if (RightSup1 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9499, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9500, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightSup1(RightSup1);
				}
				if (RightSup1 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9499, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9500, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9501, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightSup1(RightSup1);
				}
				if (RightSup2 == 1)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9502, 1);
					activeChar.addSkill(Skill, true);
					activeChar.setRightSup2(RightSup2);
				}
				if (RightSup2 == 2)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9502, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9503, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.setRightSup2(RightSup2);
				}
				if (RightSup2 == 3)
				{
					L2Skill Skill = SkillTable.getInstance().getInfo(9502, 1);
					L2Skill Skill2 = SkillTable.getInstance().getInfo(9503, 1);
					L2Skill Skill3 = SkillTable.getInstance().getInfo(9504, 1);
					activeChar.addSkill(Skill, true);
					activeChar.addSkill(Skill2, true);
					activeChar.addSkill(Skill3, true);
					activeChar.setRightSup2(RightSup2);
				}
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not load class path skills: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	private void loadClassPathPoints2(L2PcInstance activeChar)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("select sum(MiddleOff+LeftOff+LeftOff1+LeftOff2+LeftOff1_1+LeftOff1_2+LeftOff2_1+LeftOff2_2+RightOff+RightOff1+RightOff2+RightOff1_1+RightOff1_2+RightOff2_1+RightOff2_2+MiddleMage+LeftMage+LeftMage1+LeftMage2+RightMage+RightMage1+RightMage2+RightMage1_1+RightMage1_2+RightMage2_1+RightMage2_2+LeftMage1_1+LeftMage1_2+LeftMage2_1+LeftMage2_2+MiddleDef+LeftDef+LeftDef1+LeftDef2+RightDef+RightDef1+RightDef2+RightDef1_1+RightDef1_2+RightDef2_1+RightDef2_2+LeftDef1_1+LeftDef1_2+LeftDef2_1+LeftDef2_2+MiddleSup+LeftSup+LeftSup1+LeftSup2+RightSup+RightSup1+RightSup2+RightSup1_1+RightSup1_2+RightSup2_1+RightSup2_2+LeftSup1_1+LeftSup1_2+LeftSup2_1+LeftSup2_2) as total from class_paths where objid=?;");
			statement.setInt(1, activeChar.getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				int cp_points = rset.getInt(1);
				int final_points = cp_points;
				activeChar.setCpPoints(final_points);
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not load class path skills: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	/**
	 * @param activeChar
	 */
	private void notifyFriends(L2PcInstance cha)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT friend_name FROM character_friends WHERE charId=?");
			statement.setInt(1, cha.getObjectId());
			ResultSet rset = statement.executeQuery();
			L2PcInstance friend;
			String friendName;
			SystemMessage sm = new SystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN);
			sm.addString(cha.getName());
			while (rset.next())
			{
				friendName = rset.getString("friend_name");
				friend = L2World.getInstance().getPlayer(friendName);
				if (friend != null) // friend logged in.
				{
					friend.sendPacket(new FriendList(friend));
					friend.sendPacket(sm);
				}
			}
			sm = null;
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error restoring friend data.", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	/**
	 * @param activeChar
	 */
	public static void notifyClanMembers(L2PcInstance activeChar)
	{
		L2Clan clan = activeChar.getClan();
		// This null check may not be needed anymore since notifyClanMembers is called from within a null check already. Please remove if we're certain it's ok to do so.
		if (clan != null)
		{
			clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);
			SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN);
			msg.addString(activeChar.getName());
			clan.broadcastToOtherOnlineMembers(msg, activeChar);
			msg = null;
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(activeChar), activeChar);
		}
	}
	
	/**
	 * @param activeChar
	 */
	private void notifySponsorOrApprentice(L2PcInstance activeChar)
	{
		if (activeChar.getSponsor() != 0)
		{
			L2PcInstance sponsor = (L2PcInstance) L2World.getInstance().findObject(activeChar.getSponsor());
			if (sponsor != null)
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN);
				msg.addString(activeChar.getName());
				sponsor.sendPacket(msg);
			}
		}
		else if (activeChar.getApprentice() != 0)
		{
			L2PcInstance apprentice = (L2PcInstance) L2World.getInstance().findObject(activeChar.getApprentice());
			if (apprentice != null)
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_SPONSOR_C1_HAS_LOGGED_IN);
				msg.addString(activeChar.getName());
				apprentice.sendPacket(msg);
			}
		}
	}
	
//	/*	*//**
//			 * @param string
//			 * @return
//			 * @throws UnsupportedEncodingException
//			 *//*
//				 * private String getText(String string)
//				 * {
//				 * try
//				 * {
//				 * String result = new String(Base64.decode(string), "UTF-8");
//				 * return result;
//				 * }
//				 * catch (UnsupportedEncodingException e)
//				 * {
//				 * return null;
//				 * }
//				 * }
//				 */
//	public static void loadTutorial(L2PcInstance player)
//	{
//		QuestState q = player.getQuestState("255_Tutorial");
//		if (q != null)
//		{
//			/*
//			 * else if (player.getLevel() == 1 || Rnd.get(10) == 1)
//			 * {
//			 * player.processQuestEvent(q.getName(), "ProposePass", null, false);
//			 * }
//			 * else
//			 * {
//			 * player.processQuestEvent(q.getName(), "UC", null, false);
//			 * }
//			 */
//		}
//	}
	
	private void loadTutorial(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("255_Tutorial");
		if (qs != null)
			qs.getQuest().notifyEvent("UC", null, player);
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__03_ENTERWORLD;
	}
	
	private void setPledgeClass(L2PcInstance activeChar)
	{
		int pledgeClass = 0;
		// This null check may not be needed anymore since setPledgeClass is called from within a null check already. Please remove if we're certain it's ok to do so.
		if (activeChar.getClan() != null)
			pledgeClass = activeChar.getClan().getClanMember(activeChar.getObjectId()).calculatePledgeClass(activeChar);
		if (activeChar.isNoble() && pledgeClass < 5)
			pledgeClass = 5;
		if (activeChar.isHero() && pledgeClass < 8)
			pledgeClass = 8;
		activeChar.setPledgeClass(pledgeClass);
	}
}