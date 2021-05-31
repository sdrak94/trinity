package luna.chill;

import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import luna.IBypassHandler;
import luna.PassportManager;
import luna.PlayerPassport;
import luna.chill.model.AutoChill;
import luna.chill.model.enums.EAutoAttack;
import luna.chill.model.enums.EMoveType;
import luna.chill.model.enums.ESearchType;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.clientpackets.RequestBypassToServer;


public class ChillController implements IBypassHandler
{
	private static final int TICKS = 50;
	
	public final ConcurrentHashMap<PlayerPassport, AutoChill> _playerChills = new ConcurrentHashMap<>();
	
	private ChillController()
	{
		RequestBypassToServer.register(this);

		ThreadPoolManager.getInstance().schedule(new ChillTask(), 1000);
	}
	
	private class ChillTask implements Runnable
	{
		@Override
		public void run()
		{
			for (final var autoChill : _playerChills.values()) if (autoChill.isRunning())
			{
				try
				{
					autoChill.tick(TICKS);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			
			ThreadPoolManager.getInstance().schedule(this, TICKS);
		}
	}
	
	public AutoChill fetchChill(final L2PcInstance player)
	{
		final var playerPassport = player.getPassport();
		
		var autoChill = _playerChills.get(playerPassport);
		
		if (autoChill == null)
		{
			autoChill = new AutoChill(playerPassport);
			_playerChills.put(playerPassport, autoChill);
		}
		
		return autoChill;
		
	}
	
	public AutoChill getAutoChill(final L2PcInstance player)
	{
		return _playerChills.get(player.getPassport());
	}

	@Override
	public boolean handleBypass(L2PcInstance player, String cmd)
	{
		final var autoChill = fetchChill(player);

		if (cmd.startsWith("chill_start"))
		{
			autoChill.setRunning(true);
			autoChill.render();
			return true;
		}
		else if (cmd.startsWith("chill_stop"))
		{
			autoChill.setRunning(false);
			autoChill.render();
			return true;
		}
		else if (cmd.startsWith("chill_reset"))
		{
			autoChill.reset();
			autoChill.render();
			return true;
		}
		else if (cmd.startsWith("chill_refresh"))
		{
			autoChill.render();
	
			return true;
		}
		else if (cmd.startsWith("chill_attack_type"))
		{
			final StringTokenizer st = new StringTokenizer(cmd);
			st.nextToken();

			if (st.hasMoreTokens())
			{
				String strType = st.nextToken();
				while (st.hasMoreTokens())
					strType += "_" + st.nextToken();
				final EAutoAttack attackType = Enum.valueOf(EAutoAttack.class, strType);
				autoChill.setAutoAttack(attackType);
				autoChill.render();
			}
		}
		else if (cmd.startsWith("chill_move_type"))
		{
			final StringTokenizer st = new StringTokenizer(cmd);
			st.nextToken();

			if (st.hasMoreTokens())
			{
				String strType = st.nextToken();
				while (st.hasMoreTokens())
					strType += "_" + st.nextToken();
				final EMoveType attackType =  strType.contains("Follow") ? EMoveType.Follow_Target : Enum.valueOf(EMoveType.class, strType);
				autoChill.setMoveType(attackType);
				autoChill.render();
			}
			
			return true;
		}
		else if (cmd.startsWith("chill_search_type"))
		{
			final StringTokenizer st = new StringTokenizer(cmd);
			st.nextToken();

			if (st.hasMoreTokens())
			{
				final ESearchType searchType = Enum.valueOf(ESearchType.class, st.nextToken());
				autoChill.setSearchTarget(searchType);
				autoChill.render();
			}
			
			return true;
		}
		else if (cmd.startsWith("chill_party_target"))
		{
			final StringTokenizer st = new StringTokenizer(cmd);
			st.nextToken();

			if (st.hasMoreTokens())
			{
				String name = st.nextToken();
				while (st.hasMoreTokens())
					name += "_" + st.nextToken();
				
				final var targetPassport = PassportManager.getInstance().getByName(name);
				
				autoChill.setPartyTarget(targetPassport);
				autoChill.render();
//				if (name.equalsIgnoreCase("Not Set"))
					
					
			}
		}
		else if (cmd.startsWith("chill_action_edit"))
		{
			final StringTokenizer st = new StringTokenizer(cmd);
			st.nextToken();
			
			final int slot = Integer.parseInt(st.nextToken());
			
			int page = 0;
			if (st.hasMoreTokens())
				page = Integer.parseInt(st.nextToken());

			autoChill.renderActionEdit(slot, page);
		}
		else if (cmd.startsWith("chill_action_set"))
		{
			final StringTokenizer st = new StringTokenizer(cmd);
			st.nextToken();

			final int slot = Integer.parseInt(st.nextToken());
			final int acid = Integer.parseInt(st.nextToken());
			
			autoChill.setChillAction(slot, acid, true);

			autoChill.renderActionEdit(slot, 0);
		}
		else if (cmd.startsWith("chill_reuse_set"))
		{
			final StringTokenizer st = new StringTokenizer(cmd);
			st.nextToken();

			final int slot = Integer.parseInt(st.nextToken());
			final double reus = Double.parseDouble(st.nextToken());
			
			final var action = autoChill.getChillAction(slot, true);
			if (action != null)
				action.setReuse(reus);
			
			autoChill.renderActionEdit(slot, 0);
		}
		else if (cmd.startsWith("chill_hpp_set"))
		{
			final StringTokenizer st = new StringTokenizer(cmd);
			st.nextToken();

			final int slot = Integer.parseInt(st.nextToken());
			final double userHp = Double.parseDouble(st.nextToken());
			
			final var action = autoChill.getChillAction(slot, true);
			if (action != null)
				action.setUserHP(userHp);
			
			autoChill.renderActionEdit(slot, 0);
		}
		else if (cmd.startsWith("chill_slot_set"))
		{
			final StringTokenizer st = new StringTokenizer(cmd);
			st.nextToken();

			final int slot0 = Integer.parseInt(st.nextToken());
			final int slot1 = Integer.parseInt(st.nextToken()) - 1;
			
			final var action = autoChill.getChillAction(slot0, true);
			
			if (autoChill.swapChillAction(slot0, slot1, action.isSkill()))
				autoChill.renderActionEdit(slot1, 0);
		}
		else if (cmd.startsWith("chill_mobs"))
		{
			autoChill.renderBannableMobs();
		}
		else if (cmd.startsWith("chill_ban_mob"))
		{
			final StringTokenizer st = new StringTokenizer(cmd);
			st.nextToken();
			
			final int npcId = Integer.parseInt(st.nextToken());
			

			autoChill.banMob(npcId, true);
		}

		else if (cmd.startsWith("chill_unban_mob"))
		{
			final StringTokenizer st = new StringTokenizer(cmd);
			st.nextToken();
			
			final int npcId = Integer.parseInt(st.nextToken());
			

			autoChill.banMob(npcId, false);
		}
		return false;
	}
	
	public void renderChill(final L2PcInstance player)
	{
		final var autoChill = fetchChill(player);
		autoChill.render();
	}
	
	private static class InstanceHolder
	{
		private static final ChillController _instance = new ChillController();
	}
	
	public static ChillController getInstance()
	{
		return InstanceHolder._instance;
	}

	@Override
	public void exception(Exception e)
	{
		e.printStackTrace();
	}

}
