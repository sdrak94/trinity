package instances.Fortress;

import javolution.util.FastMap;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.model.quest.jython.QuestJython;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/**
 * 
 * @author Vital
 *
 */
public class Dungeon extends QuestJython
{
	private final FastMap<Integer, Prison>	_prisons				= new FastMap<Integer, Prison>();
	//private FastMap<Integer, Integer>	_fortInstances;

	private final String						_default				= "<html><body>You are either not on a quest that involves this NPC, or you don't meet"
																		+ " this NPC's minimum quest requirements.</body></html>";
	private final String						_noParty				= "<html><body>You must be in a party to enter.</body></html>";
	private final String						_notPartyLeader			= "<html><body>You must be the party leader to enter.</body></html>";
	//private String 						_notEnough 				= "<html><body>Not enough members in your party have completed the required quest.</body></html>";
	private final String						_canNotEnterYet			= "<html><body>The 4 hour time limit to re-enter is not over yet.</body></html>";

	private final int					SOUL_HUNTER_CHAKUNDEL	= 25552;
	private final int					RANGER_KARANKAWA		= 25557;
	private final int					JAX_THE_DESTROYER		= 25569;

	public static void main(String[] args)
	{
		new Dungeon(-1, "DungeonInstance", "custom");
	}

	public Dungeon(int questId, String name, String descr)
	{
		super(questId, name, descr);
		int[] mobs =
		{ SOUL_HUNTER_CHAKUNDEL, RANGER_KARANKAWA, JAX_THE_DESTROYER };
		// Detention Camp Wardens
		int[] npcs =
		{ 35666, 35698, 35735, 35767, 35804, 35835, 35867, 35904, 35936, 35974, 36011, 36043, 36081, 36118, 36149, 36181, 36219, 36257, 36294, 36326, 36364 };

		for (int npcId : npcs)
			addStartNpc(npcId);
		for (int npcId : npcs)
			addTalkId(npcId);
		for (int npcId : mobs)
			addKillId(npcId);
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState("DungeonInstance");
		if (st == null)
			return _default;
		Prison prison = null;
		Fort fort = FortManager.getInstance().getFort(npc);
		L2Clan clan = player.getClan();
		L2Party party = player.getParty();
		if (party == null)
			return _noParty;
		if (player != party.getLeader())
			return _notPartyLeader;
		if (clan == null || fort == null)
			return _default;
		if (clan.getClanId() != fort.getOwnerClan().getClanId())
			return _default;

		if (!_prisons.isEmpty())
		{
			if (_prisons.containsKey(fort.getFortId()))
			{
				prison = _prisons.get(fort.getFortId());
				if (prison.getCanEnter())
					return _canNotEnterYet;
			}
		}

		if (prison == null)
		{
			prison = new Prison(fort.getFortId());
			_prisons.put(prison.getFortId(), prison);
		}

		for (L2PcInstance partyMember : party.getPartyMembers())
		{
			//if (partyMember.getQuestState("").isCompleted())
			partyMember.setInstanceId(prison.getInstanceId());
			partyMember.teleToLocation(11740, -49148, -3000, true);
		}

		prison.initSpawn();

		return super.onTalk(npc, player);
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		int npcId = npc.getNpcId();
		Prison prison = null;

		for (Prison p : _prisons.values())
			if (p.getInstanceId() == player.getInstanceId())
				prison = p;

		// TODO: check what should happen
		if (prison == null)
			return null;

		switch (npcId)
		{
		case SOUL_HUNTER_CHAKUNDEL:
		case RANGER_KARANKAWA:
		case JAX_THE_DESTROYER:
			prison.setState(prison.getState() + 1);
			prison.initSpawn();
			break;
		default:
			break;
		}
		return super.onKill(npc, player, isPet);
	}

	/*
	private Prision getPrision(int id)
	{
		return _prisions.get(id);
	}
	*/
	private class Prison
	{
		private int							_fortId;
		private int							_instanceId;
		private int							_state;
		private boolean						_canEnter;

//		protected FastList<L2NpcInstance>	_rbList	= new FastList<L2NpcInstance>();

		private final int					STATE_4	= 4;
		private final int					STATE_1	= 1;
		private final int					STATE_2	= 2;
		private final int					STATE_3	= 3;

		private class ReEntrenceTimerTask implements Runnable
		{
			public void run()
			{
				_canEnter = true;
			}
		}

		private class PrisonSpawnTask implements Runnable
		{
			public void run()
			{
				switch (getState())
				{
				case STATE_4:

					break;
				case STATE_1:
					spawn(SOUL_HUNTER_CHAKUNDEL);
					break;
				case STATE_2:
					spawn(RANGER_KARANKAWA);
					break;
				case STATE_3:
					spawn(JAX_THE_DESTROYER);
					break;
				}
			}
		}

		public Prison(int id)
		{
			try
			{
				_fortId = id;
				_state = STATE_1;
				_canEnter = true;
				_instanceId = InstanceManager.getInstance().createDynamicInstance("Prison.xml");
				ThreadPoolManager.getInstance().scheduleGeneral(new ReEntrenceTimerTask(), 14400000);
			}
			catch (RuntimeException e)
			{
				_log.warning(e.toString());
			}
		}

		private void spawn(int npcId)
		{
			L2Spawn spawnDat;
			L2NpcTemplate template;
			try
			{
				template = NpcTable.getInstance().getTemplate(npcId);
				if (template != null)
				{
					spawnDat = new L2Spawn(template);
					spawnDat.setAmount(1);
					spawnDat.setLocx(11740);
					spawnDat.setLocy(-49148);
					spawnDat.setLocz(-3000);
					spawnDat.setHeading(0);
					spawnDat.setInstanceId(getInstanceId());
					spawnDat.setRespawnDelay(60);
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					spawnDat.doSpawn();
					spawnDat.stopRespawn();
				}
				else
				{
					_log.warning("PrisonDungeon: Data missing in NPC table for ID 0");
				}
			}
			catch (Exception e)
			{
				_log.warning("PrisonDungeon: Spawns could not be initialized: "+e.toString());
			}
		}

		public void initSpawn()
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new PrisonSpawnTask(), 300000);
		}

		public int getInstanceId()
		{
			return _instanceId;
		}

		public int getFortId()
		{
			return _fortId;
		}

		public int getState()
		{
			return _state;
		}

		public void setState(int val)
		{
			_state = val;
		}

		public boolean getCanEnter()
		{
			return _canEnter;
		}
	}
}