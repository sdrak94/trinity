package luna.custom.handler.commands;

import java.util.Collection;
import java.util.StringTokenizer;

import luna.custom.handler.WarFinisherChecker;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class AdminStopWar implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_stopwar",
		"admin_check"
	};
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_stopwar"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			try
			{
				st.nextToken();
				final String clanName1 = st.nextToken();
				final String clanName2 = st.nextToken();
				if (clanName1 != null && clanName2 != null)
				{
					L2Clan clan1 = ClanTable.getInstance().getClanByName(clanName1);
					if (clan1 == null)
					{
						activeChar.sendMessage(clanName1 + " doesn't exist");
						return false;
					}
					L2Clan clan2 = ClanTable.getInstance().getClanByName(clanName2);
					if (clan2 == null)
					{
						activeChar.sendMessage(clanName2 + " doesn't exist");
						return false;
					}
					ClanTable.getInstance().endTwoSidedWar(clan1.getClanId(), clan2.getClanId());
					Collection<L2PcInstance> players = L2World.getInstance().getAllPlayers().values();
					for (L2PcInstance cha : players)
					{
						L2PcInstance target = null;
						if (cha.getActingPlayer().getTarget() != null)
						{
							target = cha.getTarget().getActingPlayer();
							if (cha.getClan() == clan1 || cha.getClan() == clan2)
							{
								if (target != null)
								{
									if (target instanceof L2PcInstance)
									{
										if (target.getClan() != null)
										{
											if ((target.getClan() == clan1 || target.getClan() == clan2))
											{
												cha.abortAttack();
												cha.abortCast();
											}
										}
									}
								}
							}
						}
						cha.broadcastUserInfo();
					}
					activeChar.sendMessage("War between "+ clanName1 +" and " + clanName2 +" is now over");
				}
				else
					activeChar.sendMessage("NULLLL !!!!! clanName1 != null && clanName2 != null");
			}
			catch (Exception e)
			{
				activeChar.sendMessage("You fucked up something.");
			}
		}
		else if (command.startsWith("admin_check"))
		{
			WarFinisherChecker.getInstance().init();
		}
		return false;
	}
	
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
