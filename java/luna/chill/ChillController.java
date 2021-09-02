//package luna.chill;
//
//import java.util.StringTokenizer;
//import java.util.concurrent.ConcurrentHashMap;
//
//import gnu.trove.map.hash.TIntLongHashMap;
//import gnu.trove.map.hash.TLongObjectHashMap;
//import luna.IBypassHandler;
//import luna.PassportManager;
//import luna.PlayerPassport;
//import luna.chill.model.AutoChill;
//import luna.chill.model.enums.EActionPriority;
//import luna.chill.model.enums.EAutoAttack;
//import luna.chill.model.enums.EMoveType;
//import luna.chill.model.enums.ESearchType;
//import luna.chill.model.enums.EPanelOptions;
//import luna.custom.globalScheduler.ITimeTrigger;
//import luna.custom.globalScheduler.RealTimeController;
//import net.sf.l2j.Config;
//import net.sf.l2j.L2DatabaseFactory;
//import net.sf.l2j.gameserver.Shutdown;
//import net.sf.l2j.gameserver.Shutdown.Savable;
//import net.sf.l2j.gameserver.ThreadPoolManager;
//import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
//import net.sf.l2j.gameserver.network.clientpackets.RequestBypassToServer;
//
//
//
//public class ChillController implements IBypassHandler, Savable, ITimeTrigger
//{
//	private static final int TICKS = Config.CHILL_SLEEP_TICKS;
//	
//	private final TIntLongHashMap _playerCredit = new TIntLongHashMap();
//	
//	private final ConcurrentHashMap<PlayerPassport, AutoChill> _playerChills = new ConcurrentHashMap<>();
//	
//	private ChillController()
//	{
//		RequestBypassToServer.register(this);
//		Shutdown.addShutdownHook(this);
//		RealTimeController.registerHook(this);
//		ThreadPoolManager.getInstance().schedule(new ChillTask(), 1000);
//		load();
//	}
//	
//	private class ChillTask implements Runnable
//	{
//		@Override
//		public void run()
//		{
//			final TLongObjectHashMap<AutoChill> hwids = new TLongObjectHashMap<>();
//			
//			for (final var autoChill : _playerChills.values())
//			{
//				final var player = autoChill.getActivePlayer();
//				
//				if (player != null && autoChill.isRunning())
//				{
//					try
//					{
//						//final long hwid = player.getHWID();
//						//final var oldChill = hwids.get(hwid);
//						
////						if  (oldChill != null)
////						{
////							player.sendMessage("Your HWID is already using AutoChill on a different session!");
////							autoChill.setRunning(false);
////							autoChill.render();
////						}
////						else
////						{
////							hwids.put(hwid, autoChill);
////							autoChill.tick(TICKS);
////						}
//
//						autoChill.tick(TICKS);
//					}
//					catch (Exception e)
//					{
//						e.printStackTrace();
//					}
//				}
//			}
//			
//			ThreadPoolManager.getInstance().schedule(this, TICKS);
//		}
//	}
//	
//	public AutoChill fetchChill(final L2PcInstance player)
//	{
//		final var playerPassport = player.getPassport();
//		
//		var autoChill = _playerChills.get(playerPassport);
//		
//		if (autoChill == null)
//		{
//			autoChill = new AutoChill(playerPassport, _playerCredit.get(playerPassport.getObjectId()));
//			_playerChills.put(playerPassport, autoChill);
//		}
//		
//		return autoChill;
//		
//	}
//	
//	public AutoChill getAutoChill(final L2PcInstance player)
//	{
//		return _playerChills.get(player.getPassport());
//	}
//
//	@Override
//	public boolean handleBypass(L2PcInstance player, String cmd)
//	{
//		if (!cmd.contains("chill"))
//			return false;
//		
//		final var autoChill = fetchChill(player);
//
//		final StringTokenizer st = new StringTokenizer(cmd);
//		st.nextToken();
//		
//		if (cmd.startsWith("chill_start"))
//		{
//			autoChill.setRunning(true);
//			autoChill.render();
//			return true;
//		}
//		else if (cmd.startsWith("chill_stop"))
//		{
//			autoChill.setRunning(false);
//			autoChill.render();
//			return true;
//		}
//		else if (cmd.startsWith("chill_reset"))
//		{
//			autoChill.reset();
//			autoChill.render();
//			return true;
//		}
//		else if (cmd.startsWith("chill_refresh"))
//		{
//			autoChill.render();
//	
//			return true;
//		}
//		else if (cmd.startsWith("chill_attack_type"))
//		{
//			if (st.hasMoreTokens())
//			{
//				String strType = st.nextToken();
//				while (st.hasMoreTokens())
//					strType += "_" + st.nextToken();
//				final EAutoAttack attackType = Enum.valueOf(EAutoAttack.class, strType);
//				autoChill.setAutoAttack(attackType);
//				autoChill.render();
//			}
//		}
//		else if (cmd.startsWith("chill_move_type"))
//		{
//			if (st.hasMoreTokens())
//			{
//				String strType = st.nextToken();
//				while (st.hasMoreTokens())
//					strType += "_" + st.nextToken();
//				final EMoveType attackType =  strType.contains("Follow") ? EMoveType.Follow_Target : Enum.valueOf(EMoveType.class, strType);
//				autoChill.setMoveType(attackType);
//				autoChill.render();
//			}
//			
//			return true;
//		}
//		else if (cmd.startsWith("chill_search_type"))
//		{
//			if (st.hasMoreTokens())
//			{
//				final ESearchType searchType = Enum.valueOf(ESearchType.class, st.nextToken());
//				autoChill.setSearchTarget(searchType);
//				autoChill.render();
//			}
//			
//			return true;
//		}
//		else if (cmd.startsWith("chill_party_target"))
//		{
//			if (st.hasMoreTokens())
//			{
//				String name = st.nextToken();
//				while (st.hasMoreTokens())
//					name += "_" + st.nextToken();
//				
//				final var targetPassport = PassportManager.getInstance().getByName(name);
//				
//				autoChill.setPartyTarget(targetPassport);
//				autoChill.render();
//					
//			}
//		}
//		else if (cmd.startsWith("chill_action_edit"))
//		{
//			final int slot = Integer.parseInt(st.nextToken());
//			
//			int page = 0;
//			if (st.hasMoreTokens())
//				page = Integer.parseInt(st.nextToken());
//
//			autoChill.renderActionEdit(slot, page);
//		}
//		else if (cmd.startsWith("chill_action_set"))
//		{
//			if (st.hasMoreTokens())
//			{
//				final int slot = Integer.parseInt(st.nextToken());
//				if (st.hasMoreTokens())
//				{
//					final int acid = Integer.parseInt(st.nextToken());
//					
//					autoChill.setChillAction(slot, acid, true);
//
//					autoChill.renderActionEdit(slot, 0);
//				}
//			}
//		}
//		else if (cmd.startsWith("chill_reuse_set"))
//		{
//			if (st.hasMoreTokens())
//			{
//				final int slot = Integer.parseInt(st.nextToken());
//				if (st.hasMoreTokens())
//				{
//					final double reus = Double.parseDouble(st.nextToken());
//					
//					final var action = autoChill.getChillAction(slot, true);
//					if (action != null)
//						action.setReuse(reus);
//					
//					autoChill.renderActionEdit(slot, 0);
//				}
//			}
//		}
//		else if (cmd.startsWith("chill_hpp_set"))
//		{
//			if (st.hasMoreTokens())
//			{
//				final int slot = Integer.parseInt(st.nextToken());
//				if (st.hasMoreTokens())
//				{
//					final double userHp = Double.parseDouble(st.nextToken());
//					
//					final var action = autoChill.getChillAction(slot, true);
//					if (action != null)
//						action.setUserHP(userHp);
//					
//					autoChill.renderActionEdit(slot, 0);
//				}
//			}
//		}
//		else if (cmd.startsWith("chill_tpp_set"))
//		{
//			if (st.hasMoreTokens())
//			{
//				final int slot = Integer.parseInt(st.nextToken());
//				if (st.hasMoreTokens())
//				{
//					final double targHp = Double.parseDouble(st.nextToken());
//					
//					final var action = autoChill.getChillAction(slot, true);
//					if (action != null)
//						action.setTargetHP(targHp);
//					
//					autoChill.renderActionEdit(slot, 0);
//				}
//			}
//		}
//		else if (cmd.startsWith("chill_slot_set"))
//		{
//			if (st.hasMoreTokens())
//			{
//				final int slot0 = Integer.parseInt(st.nextToken());
//				if (st.hasMoreTokens())
//				{
//					final int slot1 = Integer.parseInt(st.nextToken()) - 1;
//					
//					final var action = autoChill.getChillAction(slot0, true);
//					
//					final var newPriority = EActionPriority.values()[slot1];
//					if (newPriority == EActionPriority.Remove)
//					{
//						autoChill.deleteChillAction(slot0, action.isSkill());
//						autoChill.render();
//					}
//					else if (autoChill.swapChillAction(slot0, slot1, action.isSkill()))
//						autoChill.renderActionEdit(slot1, 0);
//				}
//			}
//		}
//		else if (cmd.startsWith("chill_open_menu"))
//		{
//			if (st.hasMoreTokens())
//			{
//				final int ord = Integer.parseInt(st.nextToken()) - 1;
//				final var panelOptions = EPanelOptions.values();
//				if (ord < panelOptions.length)
//				{
//					final var panelOption = panelOptions[ord];
//					panelOption.render(autoChill);
//				}
//			}
//		}
//		else if (cmd.startsWith("chill_filter_target"))
//		{
//			if (st.hasMoreTokens())
//			{
//				final int npcId = Integer.parseInt(st.nextToken());
//				autoChill.toggleFilteredTarget(npcId);
//			}
//		}
//
//		return false;
//	}
//	
//	public void renderChill(final L2PcInstance player)
//	{
//		final var autoChill = fetchChill(player);
//		autoChill.render();
//	}
//	
//	private static class InstanceHolder
//	{
//		private static final ChillController _instance = new ChillController();
//	}
//	
//	public static ChillController getInstance()
//	{
//		return InstanceHolder._instance;
//	}
//
//	@Override
//	public void exception(Exception e)
//	{
//		//e.printStackTrace();
//	}
//
//	@Override
//	public void store()
//	{
//		final long t0 = System.currentTimeMillis();
//		
//		try (final var con = L2DatabaseFactory.getConnectionS();
//			 final var pst = con.prepareStatement("INSERT INTO character_chill_credit (owner_id, credits) VALUES (?, ?) ON DUPLICATE KEY UPDATE credits = ?"))
//		{
//			con.setAutoCommit(false);
//			for (final var autoChillSet : _playerChills.entrySet())
//			{
//				final var playerPassport = autoChillSet.getKey();
//				final var autoChill = autoChillSet.getValue();
//
//				pst.setInt(1, playerPassport.getObjectId());
//				pst.setLong(2, autoChill.getCredit());
//				pst.setLong(3, autoChill.getCredit());
//				
//				pst.addBatch();
//				
//			}
//			
//			
//			final int total = pst.executeBatch().length;
//
//			con.commit();
//			
//			final long t1 = System.currentTimeMillis();
//			
//			System.err.println("Updates " + total + " player Auto Chill credits in " + (t1 - t0) + " ms!!!" );
//			
//			
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
//	
//	public void load()
//	{
//		try (final var con = L2DatabaseFactory.getConnectionS();
//			 final var  st = con.createStatement();
//			 final var  rs = st.executeQuery("SELECT * FROM character_chill_credit"))
//		{
//			while (rs.next())
//			{
//				final int ownerId = rs.getInt("owner_id");
//				final var credit = rs.getLong("credits");
//				
//				_playerCredit.put(ownerId, credit);
//			}
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
//
//	@Override
//	public void notify(String dayName, String timeString)
//	{
//		if (timeString.equals(Config.DAILY_CREDIT_TIME))
//		{
//			for (final var autoChill : _playerChills.values())
//			{
//				autoChill.addCredit(Config.DAILY_CREDIT);
//				final var player = autoChill.getActivePlayer();
//				if (player != null)
//					player.sendMessage(String.format("You have been rewarded with %.2f hours of daily auto chill credit.", Config.DAILY_CREDIT / 3_600_000D));
//			}
//		}
//	}
//
//	@Override
//	public void notify(int day, String trigger)
//	{
//	}
//
//}
//
