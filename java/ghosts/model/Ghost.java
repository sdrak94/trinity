package ghosts.model;


import inertia.controller.InertiaController;
import inertia.model.IInertiaBehave;
import inertia.model.Inertia;
import inertia.model.behave.GhostBehave;
import inertia.model.behave.MultiBehave;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.BBSSchemeBufferInstance;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.FortSiege;
import net.sf.l2j.gameserver.model.entity.Siege;
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
import net.sf.l2j.gameserver.network.GhostClient;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.clientpackets.EnterWorld;
import net.sf.l2j.gameserver.network.serverpackets.CharInfo;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.templates.chars.L2PcTemplate;

public class Ghost extends L2PcInstance 
{
	private final GhostTemplate _ghostTemplate;
	private GhostClient _client;
	
	public Ghost(int objectId, L2PcTemplate template, String accountName, PcAppearance app, final GhostTemplate ghostTemplate)
	{
		super(objectId, template, accountName, app);
		_ghostTemplate = ghostTemplate;
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		// TODO Auto-generated method stub
		super.sendInfo(activeChar);
		for (L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			if (player == null || player == this)
				continue;
			final int relation = getRelation(player);
			if (getKnownList().getKnownRelations() != null && getKnownList().getKnownRelations().get(player.getObjectId()) != null && getKnownList().getKnownRelations().get(player.getObjectId()) != relation)
			{
				player.sendPacket(new RelationChanged(this, relation, player.isAutoAttackable(this)));
				if (this.getPet() != null)
					player.sendPacket(new RelationChanged(this.getPet(), relation, player.isAutoAttackable(this)));
			}
			player.sendPacket(new CharInfo(this));
		}
	}
	
	@Override
	public void sendSkillList()
	{
	}

	@Override
	public void setClient(L2GameClient client)
	{
		_client = new GhostClient(null);
	}
	
	@Override
	public GhostClient getClient()
	{
		return _client;
	}
	
	@Override
	public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
	}
	
//	@Override
//	public void closeNetConnection()
//	{
//	}
	
	@Override
	public void sendMessage(String message)
	{
	}

	
	public GhostTemplate getGhostTemplate()
	{
		return _ghostTemplate;
	}
	
	
	public void buffSelf()
	{
		if (isMageClass())
		{
			BBSSchemeBufferInstance.getInstance().onBypass(this, "_bbsbufferbypass_giveBuffSet mage 0 0");
			return;
		}
		else
		{
			BBSSchemeBufferInstance.getInstance().onBypass(this, "_bbsbufferbypass_giveBuffSet figher 0 0");
			return;
		}
	}
	
	public void setupAutoChill()
	{
		Inertia chill = InertiaController.getInstance().fetchChill(this);
		
		
		final var inertiaConf = _ghostTemplate.getInertiaConfiguration();
		
		final var inertiaActions = inertiaConf.getActions();
		
		chill.setAutoAttack(inertiaConf.getAutoAttack());
		chill.setMoveType(inertiaConf.getMoveType());
		chill.setSearchTarget(inertiaConf.getSearchType());
		
		for (final var inertiaAction : inertiaActions.values())
		{
			final int priority = inertiaAction.getPriority();
			if (priority > -1)
			{
				chill.setChillAction(inertiaAction.getPriority(), inertiaAction.getSkillId(), true);
			}
		}
		chill.setRunning(true);

	}
	
	
	@Override
	public IInertiaBehave createInertiaBehavior()
	{
		
		if (_ghostTemplate.hasExtensions())
		{
			final IInertiaBehave multiBehave = new MultiBehave();
			multiBehave.expand(new GhostBehave());
			_ghostTemplate.addExtensions(multiBehave);
			return multiBehave;
		}
		else
		{
			return new GhostBehave();
		}

	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public void EnterWorld()
	{

		//updatePvpTitleAndColor(false);

		setNameColorsDueToPVP();
		// Set dead status if applies
		if (getCurrentHp() < 0.5)
			setIsDead(true);

		canSendUserInfo = true;
		final var clan = getClan();
		
		if (clan != null)
		{
			EnterWorld.notifyClanMembers(this);
			
			
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				if (!siege.getIsInProgress())
					continue;
				
				if (siege.checkIsAttacker(clan))
				{
					setSiegeState((byte) 1);
					
				}
				
				else if (siege.checkIsDefender(clan))
				{
					setSiegeState((byte) 2);
					
				}
			}
			
			for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
			{
				if (!siege.getIsInProgress())
					continue;
				
				if (siege.checkIsAttacker(clan))
				{
					setSiegeState((byte) 1);
					
				}
				
				else if (siege.checkIsDefender(clan))
				{
					setSiegeState((byte) 2);
					
				}
			}
			

			
			
			// Residential skills support
			if (clan.getCastle() != null)
				CastleManager.getInstance().getCastleByOwner(clan).giveResidentialSkills(this);
			
			if (clan.getHasFort() > 0)
				FortManager.getInstance().getFortByOwner(clan).giveResidentialSkills(this);
			
			Quest.playerEnter(this);
			
			if (Config.PLAYER_SPAWN_PROTECTION > 0)
				setProtection(true);

			spawnMe(getX(), getY(), getZ());

			ThreadPoolManager.getInstance().scheduleGeneral(new teleportTask(this), 250);
			
		}
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
					_player.updateAndBroadcastStatus(2);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
