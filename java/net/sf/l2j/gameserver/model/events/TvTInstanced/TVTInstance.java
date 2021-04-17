package net.sf.l2j.gameserver.model.events.TvTInstanced;

import java.util.Vector;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.util.Rnd;

public class TVTInstance
{
	protected static final Logger _log = Logger.getLogger(TVTInstance.class.getName());

	private String					_instanceName	= new String();

	private int						_instanceId		= 0;

	private int						_rewardId		= 0;
	private int						_rewardAmount	= 0;
	private int						_minLvl			= 0;
	private int						_maxLvl			= 0;
	private int						_joinTime		= 0;
	private int						_eventTime		= 0;
	private int						_minPlayers		= 0;
	private int						_maxPlayers		= 0;

	private boolean					_joining		= false;
	private boolean					_teleport		= false;
	private boolean					_started		= false;
	private boolean					_tie			= false;

	private TvTITeam				_winner			= null;

	private final Vector<TvTITeam>		_teams			= new Vector<TvTITeam>();

	private final FastList<L2PcInstance>	_tempJoinList	= new FastList<L2PcInstance>();

	public TVTInstance()
	{
		try
		{
			createInstance();
		}
		catch (RuntimeException e)
		{
			_log.severe("TvTi Engine: Faild creating instance - "+e);
		}
	}

	public void announceToInstance(String announce)
	{
		if (TvTIMain.getAnnounceName().equals(""))
			TvTIMain.setAnnounceName("TvTi");

		CreatureSay cs = new CreatureSay(0, Say2.PARTYROOM_ALL, TvTIMain.getAnnounceName(), announce);
		for (TvTITeam t : _teams)
			for (L2PcInstance player : t.getPlayers())
				player.sendPacket(cs);
	}

	public void teleportStart()
	{
		if (!_joining || _started || _teleport)
			return;

		removeOfflinePlayers();

		if (checkMinPlayers(_tempJoinList.size()))
		{
			int idxPlayer = 0;
			int idxTeam = 0;
			int teamSize = 0;
			int splitSize = 0;
			L2PcInstance tempPlayer;

			switch (Config.TVTI_SORT_TEAMS)
			{
				case 0: // Random
					while (!getPlayers().isEmpty())
					{
						idxPlayer = Rnd.get(getPlayers().size());
						tempPlayer = getPlayers().get(idxPlayer);
						getPlayers().remove(tempPlayer);
						getTeams().get(idxTeam).addPlayer(tempPlayer);
						idxTeam++;
						if (idxTeam >= getTeams().size())
							idxTeam = 0;
					}
					break;
				case 1: // Split
					teamSize = getTeams().size();
					splitSize = (getPlayers().size() / teamSize) + (getPlayers().size() % teamSize);
					for (L2PcInstance player : getPlayers())
					{
						getPlayers().remove(player);
						getTeams().get(idxTeam).addPlayer(player);
						idxPlayer++;
						if (idxPlayer >= splitSize)
							idxTeam++;
						if (getPlayers().size() % teamSize == 1 && idxTeam == 1)
							splitSize--;
					}
					break;
				case 2: // Order split
					for (L2PcInstance player : getPlayers())
					{
						getTeams().get(idxTeam).addPlayer(player);
						idxTeam++;
						if (idxTeam >= getTeams().size())
							idxTeam = 0;
					}
					getPlayers().clear();
					break;
			}

		}
		else
		{
			announceToInstance("Not enough players for event. Min Requested : " + _minPlayers + ", Participating : " + _tempJoinList.size());
			_tempJoinList.clear();
			cleanInstance(false);
			return;
		}
		_joining = false;

		announceToInstance("Teleporting to team spawn in 20 seconds!");

		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
			public void run()
			{
				for (TvTITeam t : _teams)
				{
					t.sit();
					t.removeBuffs();
					t.unsummon();
					t.removeParty();
					t.setInstance(_instanceId);
					t.teleportToSpawn();
					t.setUserData(0);
				}
			}
		}, 20000);
		_teleport = true;
	}

	public boolean teleportAutoStart()
	{
		if (!_joining || _started || _teleport)
			return false;

		removeOfflinePlayers();

		if (checkMinPlayers(_tempJoinList.size()))
		{
			int idxPlayer = 0;
			int idxTeam = 0;
			int teamSize = 0;
			int splitSize = 0;
			L2PcInstance tempPlayer;

			switch (Config.TVTI_SORT_TEAMS)
			{
				case 0: // Random
					while (!getPlayers().isEmpty())
					{
						idxPlayer = Rnd.get(getPlayers().size());
						tempPlayer = getPlayers().get(idxPlayer);
						getPlayers().remove(tempPlayer);
						getTeams().get(idxTeam).addPlayer(tempPlayer);
						idxTeam++;
						if (idxTeam >= getTeams().size())
							idxTeam = 0;
					}
					break;
				case 1: // Split
					teamSize = getTeams().size();
					splitSize = (getPlayers().size() / teamSize) + (getPlayers().size() % teamSize);
					for (L2PcInstance player : getPlayers())
					{
						getPlayers().remove(player);
						getTeams().get(idxTeam).addPlayer(player);
						idxPlayer++;
						if (idxPlayer >= splitSize)
							idxTeam++;
						if (getPlayers().size() % teamSize == 1 && idxTeam == 1)
							splitSize--;
					}
					break;
				case 2: // Order split
					for (L2PcInstance player : getPlayers())
					{
						getTeams().get(idxTeam).addPlayer(player);
						idxTeam++;
						if (idxTeam >= getTeams().size())
							idxTeam = 0;
					}
					getPlayers().clear();
					break;
			}

		}
		else
		{
			announceToInstance("Not enough players for event. Min Requested : " + _minPlayers + ", Participating : " + _tempJoinList.size());
			_tempJoinList.clear();
			return false;
		}

		_joining = false;

		announceToInstance("Teleporting to team spawn in 20 seconds!");

		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
			public void run()
			{
				for (TvTITeam t : _teams)
				{
					t.sit();
					t.removeBuffs();
					t.unsummon();
					t.removeParty();
					t.setInstance(_instanceId);
					t.teleportToSpawn();
					t.setUserData(0);
				}
			}
		}, 20000);
		_teleport = true;
		return true;
	}

	public void startEvent()
	{
		if (_joining || !_teleport || _started)
			return;
		TvT.lastEventTime = System.currentTimeMillis();
		_teleport = false;
		_winner = null;
		_tie = false;
		for (TvTITeam t : _teams)
		{
			t.setTeamScore(0);
			t.sit();
		}
		announceToInstance("Go to kill your enemies!");
		_started = true;
	}

	public boolean startAutoEvent()
	{
		if (_joining || !_teleport || _started)
			return false;
		TvT.lastEventTime = System.currentTimeMillis();
		_teleport = false;
		_winner = null;
		_tie = false;
		for (TvTITeam t : _teams)
		{
			t.setTeamScore(0);
			t.sit();
		}
		announceToInstance("Go to kill your enemies!");
		_started = true;
		return true;
	}

	public void finishEvent()
	{
		if (!_started)
			return;

		_started = false;

		processTopTeam();

		if (_winner.getTeamScore() == 0)
			announceToInstance("No team wins the match(nobody killed).");
		else if (_tie)
		{
			announceToInstance("Tie! Prizes are split!");
			for (TvTITeam t : _teams)
				rewardTeam(t);
			playAnimation(false);
		}
		else
		{
			announceToInstance(_winner.getTeamName() + "'s win the match! " + _winner.getTeamScore() + " Points.");
			rewardTeam(_winner);
			playAnimation(false);
		}

		if (Config.TVTI_ANNOUNCE_TEAM_STATS)
		{
			announceToInstance(getInstanceName() + " Team Statistics:");
			for (TvTITeam t : _teams)
				announceToInstance("Team: " + t.getTeamName() + " - Points: " + t.getTeamScore());
		}

		if (Config.TVTI_SHOW_STATS_PAGE)
		{
			for (TvTITeam t : _teams)
				showStats(t);
		}
		teleportFinish();
	}

	public void teleportFinish()
	{
		announceToInstance(" Teleporting back to participation NPC in 20 seconds!");
		for (TvTITeam t : _teams)
			t.setUserData(1);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
			public void run()
			{
				for (TvTITeam t : _teams)
				{
					t.setInstance(0);
					t.teleportToFinish();
				}

				_log.info("TvT: Teleport done.");
				cleanInstance(false);
			}
		}, 20000);
	}

	public void autoEvent()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new AutoEventTask(), 0);
	}

	public class AutoEventTask implements Runnable
	{

		public AutoEventTask()
		{
		}

		public void run()
		{
			if (_joinTime > 0)
				waiter(_joinTime * 60 * 1000); // Time(in minutes) to join event
			else if (_joinTime <= 0)
			{
				abortEvent();
				return;
			}
			if (teleportAutoStart())
			{
				waiter(30 * 1000); // 60 sec wait time until start fight after
									// teleport
				if (startAutoEvent())
				{
					waiter(_eventTime * 60 * 1000); // Time(in minutes) for the
													// event to last
					finishEvent();
				}
			}
			else if (!teleportAutoStart())
				abortEvent();
		}
	}

	private void waiter(long interval)
	{
		long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000);

		while (startWaiterTime + interval > System.currentTimeMillis())
		{
			seconds--; // here because we don't want to see two time announce at
						// the same time

			if (_joining || _started || _teleport)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						if (_joining)
						{
							TvTIMain.announceToAll("Joinable in " + TvTIMain.getJoinLocName() + "!");
							TvTIMain.announceToAll(seconds / 60 / 60 + " hour(s) till registration ends!");
						}
						else if (_started)
							TvTIMain.announceToAll(seconds / 60 / 60 + " hour(s) till event ends!");
						break;
					case 1800: // 30 minutes left
					case 900: // 15 minutes left
					case 600: // 10 minutes left
					case 300: // 5 minutes left
					case 60: // 1 minute left
						if (_joining)
						{
							removeOfflinePlayers();
							TvTIMain.announceToAll("Joinable in " + TvTIMain.getJoinLocName() + "!");
							TvTIMain.announceToAll(seconds / 60 + " minute(s) till registration ends!");
						}
						else if (_started)
							announceToInstance(seconds / 60 + " minute(s) till event ends!");

						break;
					case 30: // 30 seconds left
					case 10: // 10 seconds left
					case 3: // 3 seconds left
					case 2: // 2 seconds left
					case 1: // 1 seconds left
						if (_joining)
							announceToInstance(seconds + " second(s) till registration ends!");
						else if (_teleport)
							announceToInstance(seconds + " seconds(s) till fight starts!");
						else if (_started)
							announceToInstance(seconds + " second(s) till event ends!");

						break;
				}
			}

			long startOneSecondWaiterStartTime = System.currentTimeMillis();

			// only the try catch with Thread.sleep(1000) give bad countdown on
			// high wait times
			while (startOneSecondWaiterStartTime + 1000 > System.currentTimeMillis())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException ie)
				{
				}
			}
		}
	}

	public void abortEvent()
	{
		if (!_joining && !_teleport && !_started)
			return;
		if (_joining && !_teleport && !_started)
		{
			cleanInstance(false);
			_joining = false;
			announceToInstance("Match aborted!");
			return;
		}
		setJoining(false);
		setTeleport(false);
		setStarted(false);
		announceToInstance(" Match aborted!");
		teleportFinish();
	}

	public void cleanInstance(boolean delTeams)
	{
		_log.info("TvT : Cleaning players.");
		for (TvTITeam t : _teams)
		{
			for (L2PcInstance player : t.getPlayers())
				if (player != null)
				{
					player._joiningTvTi = false;
					player._inEventTvT = false;
				}
			if (!t.getPlayers().isEmpty())
				t.getPlayers().clear();
		}

		if (delTeams)
		{
			_log.info("TvT : Cleaning teams.");
			_teams.clear();
		}
		_winner = null;
		setJoining(false);
		setTeleport(false);
		setStarted(false);

		if (TvTIMain.canUnspawnEventNpc())
			TvTIMain.unspawnEventNpc();

		_tempJoinList.clear();
		_log.info("Cleaning TvT done.");
	}

	public void processTopTeam()
	{
		for (TvTITeam t : _teams)
		{
			if (_winner != null)
			{
				if (_teams.size() == 2 && Config.TVTI_ALLOW_TIE && t.getTeamScore() == _winner.getTeamScore())
				{
					_tie = true;
					return;
				}
				else if (t.getTeamScore() > _winner.getTeamScore())
					_winner = t;
			}
			else
				_winner = t;
		}
	}

	public void rewardTeam(TvTITeam t)
	{
		for (L2PcInstance player : t.getPlayers())
			if (player != null && player.isOnline() != 0 && player._inEventTvTi)
			{
				if (player._countTvTiKills > 0 || Config.TVTI_PRICE_NO_KILLS)
				{
					if (_tie)
					{
						player.addItem("TvTi Event: " + TvTIMain.getEventTitle(), _rewardId, (_rewardAmount / 2) + (_rewardAmount % 2), player, true);
						player.sendMessage("The event has resulted in a tie, prizes are split. Look in your inventory for your reward.");
					}
					else
					{
						player.addItem("TvTi Event: " + TvTIMain.getEventTitle(), _rewardId, _rewardAmount, player, true);
						player.sendMessage("Your team wins the event. Look in your inventory for your reward.");
					}
				}
				else
				{
					player.sendMessage("Your team wins the event. But you did not kill anything, so no reward for you.");
				}
				// Send a Server->Client ActionFailed to the L2PcInstance in
				// order to avoid that the client wait another packet
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
	}

	public void showStats(TvTITeam t)
	{
		for (L2PcInstance player : t.getPlayers())
			if (player != null && player.isOnline() != 0 && player._inEventTvTi)
			{
				boolean bg = false;

				NpcHtmlMessage nhm = new NpcHtmlMessage(5);
				TextBuilder replyMSG = new TextBuilder("");

				replyMSG.append("<html><body>");
				replyMSG.append("<title>Team vs Team Instanced</title>");
				replyMSG.append("Your team stats:<br>");
				replyMSG.append("<table width=\"300\"><tr>");
				replyMSG.append("<table width=\"300\"><tr>");
				replyMSG.append("<td width=\"250\" align=\"center\">Players</td>");
				replyMSG.append("<td width=\"50\" align=\"center\">Kills</td>");
				replyMSG.append("</tr><tr>");
				for (L2PcInstance p : t.getPlayers())
				{
					if (bg)
					{
						replyMSG.append("<td bgcolor=\"000000\" width=\"250\">" + p.getName() + "</td>");
						replyMSG.append("<td bgcolor=\"000000\" width=\"50\">" + p._countTvTiKills + "</td>");
						replyMSG.append("</tr><tr>");
						bg = false;
					}
					else
					{
						replyMSG.append("<td width=\"250\">" + p.getName() + "</td>");
						replyMSG.append("<td width=\"50\">" + p._countTvTiKills + "</td>");
						replyMSG.append("</tr><tr>");
						bg = true;
					}
				}
				replyMSG.append("</tr></table>");
				replyMSG.append("</body></html>");

				nhm.setHtml(replyMSG.toString());
				player.sendPacket(nhm);

				// Send a Server->Client ActionFailed to the L2PcInstance in
				// order to avoid that the client wait another packet
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
	}

	public void playAnimation(boolean tie)
	{
		for (TvTITeam t : _teams)
			for (L2PcInstance player : t.getPlayers())
				if (player != null)
				{
					if (!_winner.getPlayers().contains(player) && !tie)
						player.broadcastPacket(new SocialAction(player.getObjectId(), 7));
					else if (_winner.getPlayers().contains(player) || tie)
						player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				}
	}

	public boolean checkMaxLevel(int maxlvl)
	{
		if (_minLvl >= maxlvl)
			return false;
		return true;
	}

	public boolean checkMinLevel(int minlvl)
	{
		if (_maxLvl <= minlvl)
			return false;
		return true;
	}

	public boolean checkMinPlayers(int players)
	{
		if (_minPlayers <= players)
			return true;
		return false;
	}

	public boolean checkMaxPlayers(int players)
	{
		if (_maxPlayers > players)
			return true;
		return false;
	}

	public boolean checkTeamOk()
	{
		if (_started || _teleport || _joining)
			return false;
		return true;
	}

	public void removeOfflinePlayers()
	{
		for (L2PcInstance player : _tempJoinList)
			if (player.isOnline() == 0)
				_tempJoinList.remove(player);
	}

	public boolean isSetUp()
	{
		if (_teams.size() < 2 || _maxPlayers == 0 || _maxLvl == 0 || _rewardId == 0 || _rewardAmount == 0)
			return false;
		for (TvTITeam t : _teams)
			if (!t.isSetUp())
				return false;
		return true;
	}

	public void createInstance()
	{
		_instanceId = InstanceManager.getInstance().createDynamicInstance(Config.TVTI_INSTANCE_XML);
	}

	public void destroyInstance()
	{
		InstanceManager.getInstance().destroyInstance(_instanceId);
	}

	public int createTeam(String teamName)
	{
		TvTITeam t = new TvTITeam(teamName);
		getTeams().add(t);
		return getTeams().indexOf(t);
	}

	public void createTeam(String teamName, String teamColor, int spawnLocX, int spawnLocY, int spawnLocZ, int spawnRadius)
	{
		TvTITeam t = new TvTITeam(teamName, teamColor, spawnLocX, spawnLocY, spawnLocZ, spawnRadius);
		getTeams().add(t);
	}

	public void createTeam(String teamName, int teamColor, int spawnLocX, int spawnLocY, int spawnLocZ, int spawnRadius)
	{
		TvTITeam t = new TvTITeam(teamName, teamColor, spawnLocX, spawnLocY, spawnLocZ, spawnRadius);
		getTeams().add(t);
	}

	public int getInstanceId()
	{
		return _instanceId;
	}

	public void setInstanceName(String name)
	{
		_instanceName = name;
	}

	public String getInstanceName()
	{
		return _instanceName;
	}

	public void setRewardId(int rewardId)
	{
		_rewardId = rewardId;
	}

	public int getRewardId()
	{
		return _rewardId;
	}

	public void setRewardAmount(int amount)
	{
		_rewardAmount = amount;
	}

	public int getRewardAmount()
	{
		return _rewardAmount;
	}

	public void setMinLvl(int lvl)
	{
		_minLvl = lvl;
	}

	public int getMinLvl()
	{
		return _minLvl;
	}

	public void setMaxLvl(int lvl)
	{
		_maxLvl = lvl;
	}

	public int getMaxLvl()
	{
		return _maxLvl;
	}

	public void setJoinTime(int time)
	{
		_joinTime = time;
	}

	public int getJoinTime()
	{
		return _joinTime;
	}

	public void setEventTime(int time)
	{
		_eventTime = time;
	}

	public int getEventTime()
	{
		return _eventTime;
	}

	public void setMinPlayers(int amount)
	{
		_minPlayers = amount;
	}

	public int getMinPlayers()
	{
		return _minPlayers;
	}

	public void setMaxPlayers(int amount)
	{
		_maxPlayers = amount;
	}

	public int getMaxPlayers()
	{
		return _maxPlayers;
	}

	public void setJoining(boolean joining)
	{
		_joining = joining;
	}

	public boolean isJoining()
	{
		return _joining;
	}

	public void setTeleport(boolean teleport)
	{
		_teleport = teleport;
	}

	public boolean isTeleport()
	{
		return _teleport;
	}

	public void setStarted(boolean started)
	{
		_started = started;
	}

	public boolean isStarted()
	{
		return _started;
	}

	public void addPlayer(L2PcInstance player)
	{
		_tempJoinList.add(player);
	}

	public void removePlayer(L2PcInstance player)
	{
		_tempJoinList.remove(player);
	}

	public FastList<L2PcInstance> getPlayers()
	{
		return _tempJoinList;
	}

	public Vector<TvTITeam> getTeams()
	{
		return _teams;
	}
}
