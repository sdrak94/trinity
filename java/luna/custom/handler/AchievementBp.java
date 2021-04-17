package luna.custom.handler;

import java.util.function.Function;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2ChestInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeGuardInstance;
import net.sf.l2j.gameserver.util.Util;

public class AchievementBp
{
	public static AchievementBp getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AchievementBp _instance = new AchievementBp();
	}
	
	public String onCreatureKill(L2Character killer, L2Character victim)
	{
		if (!Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			return null;
		}
		L2PcInstance player = killer == null ? null : killer.getActingPlayer();
		if (player == null)
		{
			return null;
		}
		if (victim instanceof L2PcInstance)
		{
			victim.getActingPlayer().getCounters().timesDied++;
			if (victim.isInsideZone(L2Character.ZONE_SIEGE))
			{
				player.getCounters().playersKilledInSiege++;
			}
		}
		if (victim instanceof L2Npc)
		{
			if (victim instanceof L2ChestInstance)
			{
				player.getCounters().treasureBoxesOpened++;
			}
			else if (victim instanceof L2GuardInstance)
			{
				player.getCounters().townGuardsKilled++;
			}
			else if (victim instanceof L2SiegeGuardInstance)
			{
				player.getCounters().siegeGuardsKilled++;
			}
		}
		if ((player.getLevel() - victim.getLevel()) >= 10)
		{
			return null;
		}
		if (victim instanceof L2MonsterInstance)
		{
			player.getCounters().mobsKilled++;
			switch (((L2MonsterInstance) victim).getNpcId())
			{
				case 96020:
					if (player.getParty() != null)
					{
						for (L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().horrorKilled++;
							}
						}
					}
					else
					{
						player.getCounters().horrorKilled++;
					}
					break;
				case 96019:
					if (player.getParty() != null)
					{
						for (L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().holyKnightKilled++;
							}
						}
					}
					else
					{
						player.getCounters().holyKnightKilled++;
					}
					break;
				case 800001:
					if (player.getParty() != null)
					{
						for (L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().majinHorrorKilled++;
							}
						}
					}
					else
					{
						player.getCounters().majinHorrorKilled++;
					}
					break;
				case 800000:
					if (player.getParty() != null)
					{
						for (L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().majinOblivionKilled++;
							}
						}
					}
					else
					{
						player.getCounters().majinOblivionKilled++;
					}
					break;
				case 95103:
					if (player.getParty() != null)
					{
						for (L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().pusKilled++;
							}
						}
					}
					else
					{
						player.getCounters().pusKilled++;
					}
					break;
				case 960180:
					if (player.getParty() != null)
					{
						for (L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().titaniumDreadKilled++;
							}
						}
					}
					else
					{
						player.getCounters().titaniumDreadKilled++;
					}
					break;
				case 95609:
					if (player.getParty() != null)
					{
						for (L2PcInstance ptm : player.getParty().getPartyMembers())
						{
							if (Util.calculateDistance(ptm, victim, true) <= 5000)
							{
								ptm.getCounters().glabKilled++;
							}
						}
					}
					else
					{
						player.getCounters().glabKilled++;
					}
					break;
				default:
					break;
			}
		}
		if (victim.isRaid() && !victim.isRaidMinion())
		{
			forEachPlayerInGroup(player, plr ->
			{
				if (Util.calculateDistance(plr, victim, true) <= 5000)
				{
					plr.getCounters().raidsKilled++;
				}
				return true;
			});
		}
		if (victim.isChampion())
		{
			player.getCounters().championsKilled++;
		}
		if (victim instanceof L2Npc)
		{
			switch (((L2Npc) victim).getNpcId())
			{
				case 29001: // Queen Ant
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().antQueenKilled++;
						}
						return true;
					});
					break;
				case 29006: // Core
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().coreKilled++;
						}
						return true;
					});
					break;
				case 29014: // Orfen
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().orfenKilled++;
						}
						return true;
					});
					break;
				case 29019: // Antharas
				case 29066: // Antharas
				case 29067: // Antharas
				case 29068: // Antharas
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().antharasKilled++;
						}
						return true;
					});
					break;
				case 29020: // Baium
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().baiumKilled++;
						}
						return true;
					});
					break;
				case 29022: // Zaken Lv. 60
				case 29176: // Zaken Lv. 60
				case 29181: // Zaken Lv. 83
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().zakenKilled++;
						}
						return true;
					});
					break;
				case 29028: // Valakas
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().valakasKilled++;
						}
						return true;
					});
					break;
				case 29047: // Scarlet van Halisha / Frintezza
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().frintezzaKilled++;
						}
						return true;
					});
					break;
				case 29065: // Sailren
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().sailrenKilled++;
						}
						return true;
					});
					break;
				case 29099: // Baylor
				case 29186: // Baylor
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().baylorKilled++;
						}
						return true;
					});
					break;
				case 29118: // Beleth
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().belethKilled++;
						}
						return true;
					});
					break;
				case 29163: // Tiat
				case 29175: // Tiat
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().tiatKilled++;
						}
						return true;
					});
					break;
				case 29179: // Freya Normal
				case 29180: // Freya Hard
					forEachPlayerInGroup(player, plr ->
					{
						if (Util.calculateDistance(plr, victim, true) <= 5000)
						{
							plr.getCounters().freyaKilled++;
						}
						return true;
					});
					break;
			}
		}
		return null;
	}
	
	private void forEachPlayerInGroup(L2PcInstance player, Function<L2PcInstance, Boolean> procedure)
	{
		if (player.isInParty())
		{
			if (player.getParty().isInCommandChannel())
			{
				for (L2PcInstance member : player.getParty().getCommandChannel().getMembers())
				{
					if (!procedure.apply(member))
					{
						break;
					}
				}
				return;
			}
			for (L2PcInstance member : player.getParty().getPartyMembers())
			{
				if (!procedure.apply(member))
				{
					break;
				}
			}
			return;
		}
		procedure.apply(player);
	}
}
