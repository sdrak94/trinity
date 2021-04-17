package net.sf.l2j.gameserver.model.events;

import java.util.ArrayList;
import java.util.Collections;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.skills.AbnormalEffect;

public class KoreanRoom implements Runnable
{
	int								roomId;
	private int						currentRound				= 1;
	private boolean					activeRoom					= true;
	private ArrayList<L2PcInstance>	blueTeam					= new ArrayList<L2PcInstance>();
	private ArrayList<L2PcInstance>	redTeam						= new ArrayList<L2PcInstance>();
	private ArrayList<L2PcInstance>	blueTeamInitial				= new ArrayList<L2PcInstance>();
	private ArrayList<L2PcInstance>	redTeamInitial				= new ArrayList<L2PcInstance>();
	private ArrayList<L2PcInstance>	players						= new ArrayList<L2PcInstance>();
	private boolean					currentRoundDisconnected	= false;
	private boolean					currentRoundEndedWithKill	= false;
	private L2PcInstance			currentBluePlayer;
	private L2PcInstance			currentRedPlayer;
	private Location				locFightingBlue				= new Location(185086, -11884, -5497);
	private Location				locFightingRed				= new Location(185093, -13324, -5494);
	private final Location[]		BLUE_LOCATIONS				=
	{
		new Location(184288, -11970, -5494),
		new Location(184288, -12070, -5494),
		new Location(184288, -12170, -5494),
		new Location(184288, -12270, -5494),
		new Location(184288, -12370, -5494),
		new Location(184288, -12470, -5494),
		new Location(184288, -12570, -5494),
		new Location(184288, -12670, -5494),
		new Location(184288, -12770, -5494),
		new Location(184288, -12870, -5494),
	};
	private final Location[]		RED_LOCATIONS				=
	{
		new Location(185864, -11938, -5494),
		new Location(185864, -12038, -5494),
		new Location(185864, -12138, -5494),
		new Location(185864, -12238, -5494),
		new Location(185864, -12338, -5494),
		new Location(185864, -12438, -5494),
		new Location(185864, -12538, -5494),
		new Location(185864, -12638, -5494),
		new Location(185864, -12738, -5494),
		new Location(185864, -12838, -5494),
	};
	
	public KoreanRoom(int _roomId, ArrayList<L2PcInstance> _players)
	{
		this.roomId = _roomId;
		for (L2PcInstance p : _players)
		{
			players.add(p);
		}
	}
	
	public KoreanRoom()
	{}
	
	public int getRoomId()
	{
		return roomId;
	}
	
	public ArrayList<L2PcInstance> getAllPlayers()
	{
		return players;
	}
	
	public KoreanRoom getRoom()
	{
		return this;
	}
	
	public void executeRoom(int _roomId, ArrayList<L2PcInstance> _players)
	{
		InstanceManager.getInstance().createInstance(roomId + 50000);
		for (L2PcInstance p : players)
		{
			if (p != null)
			{
				p.setInstanceId(roomId + 50000);
				p.setIsInActiveKoreanRoom(true);
			}
		}
		Collections.shuffle(players);
		for (int i = 0; i < (players.size()); i++)
		{
			if (players.get(i) == null)
				continue;
			if (i % 2 == 0)
			{
				redTeamInitial.add(players.get(i));
				redTeam.add(players.get(i));
				players.get(i).setKoreanTeam("red");
				players.get(i).broadcastUserInfo();
			}
			else
			{
				blueTeamInitial.add(players.get(i));
				blueTeam.add(players.get(i));
				players.get(i).setKoreanTeam("blue");
				players.get(i).broadcastUserInfo();
			}
		}
		if (redTeam.size() == 0 || blueTeam.size() == 0)
		{
			endRoom();
			return;
		}
		currentRedPlayer = redTeam.get(0);
		currentBluePlayer = blueTeam.get(0);
		for (L2PcInstance p : players)
		{
			if (p == null)
				continue;

			p.startAbnormalEffect(AbnormalEffect.HOLD_1);
			p.setIsParalyzed(true);
			StopMove sm = new StopMove(p);
			sm = new StopMove(p);
			p.sendPacket(sm);
			p.broadcastPacket(sm);
			ExShowScreenMessage message = new ExShowScreenMessage("You will be teleported in Korean Arena in 5 seconds", 5000);
			p.sendPacket(message);
			// p.setInstanceId(roomId);
		}
		waitSecs(5);
		for (int i = 0; i < blueTeamInitial.size(); i++)
		{
			if (blueTeamInitial.get(i) != null)
			{
				if (blueTeamInitial.get(i).getObjectId() == currentBluePlayer.getObjectId())
					blueTeamInitial.get(i).teleToLocation(locFightingBlue, false);
				else
					blueTeamInitial.get(i).teleToLocation(BLUE_LOCATIONS[i], false);
			}
		}
		for (int i = 0; i < redTeamInitial.size(); i++)
		{
			if (redTeamInitial.get(i) != null)
			{
				if (redTeamInitial.get(i).getObjectId() == currentRedPlayer.getObjectId())
					redTeamInitial.get(i).teleToLocation(locFightingRed, false);
				else
					redTeamInitial.get(i).teleToLocation(RED_LOCATIONS[i], false);
			}
		}
		// blue para
		currentBluePlayer.startAbnormalEffect(AbnormalEffect.HOLD_1);
		currentBluePlayer.setIsParalyzed(true);
		StopMove sm = new StopMove(currentBluePlayer);
		currentBluePlayer.sendPacket(sm);
		currentBluePlayer.broadcastPacket(sm);
		// red para
		currentRedPlayer.startAbnormalEffect(AbnormalEffect.HOLD_1);
		currentRedPlayer.setIsParalyzed(true);
		sm = new StopMove(currentRedPlayer);
		currentRedPlayer.sendPacket(sm);
		currentRedPlayer.broadcastPacket(sm);
		waitSecs(5);// just to make sure everone teleported
		if (currentRound < players.size() + 1)
		{
			checkForDisconnects();
			prepareForBattleAndSpectate();
		}
		while (currentRound < players.size() + 1)
		{
			if (blueTeam.size() == 0 || redTeam.size() == 0)// almost pointless check
			{
				endRoom();
				break;
			}
			executeRound(currentRound);
			currentRound++;
		}
		endRoom();
	}
	
	public void executeRound(int round)
	{
		// checkForDisconnects();
		// prepareForBattleAndSpectate();//teleport players to the proper spots
		for (int i = 0; i < 100; i++)
		{
			waitSecs(3);
			checkForDisconnects();
			if (currentRoundDisconnected)
			{
				currentRoundDisconnected = false;
				prepareForBattleAndSpectate();
				return;
			}
			checkForDeadPlayers();
			if (currentRoundEndedWithKill)
			{
				currentRoundEndedWithKill = false;
				prepareForBattleAndSpectate();
				return;
			}
		}
		roundEndedWithoutKillOrDisconnect(round);// player with bigger % hp wins
		prepareForBattleAndSpectate();
	}
	
	private void prepareForBattleAndSpectate()
	{
		if (blueTeam.size() == 0 || redTeam.size() == 0)
			return;
		for (int i = 0; i < blueTeamInitial.size(); i++)
		{
			if (blueTeamInitial.get(i) == null)
				continue;
			if (blueTeamInitial.get(i).getObjectId() == currentBluePlayer.getObjectId())// fighting blue player
			{
				if (blueTeamInitial.get(i).eventSitForced)
				{
					blueTeamInitial.get(i).eventSitForced = false;
					blueTeamInitial.get(i).standUp();
					blueTeamInitial.get(i).broadcastUserInfo();
					waitSecs(3);
				}
				blueTeamInitial.get(i).teleToLocation(locFightingBlue, false);
				// heal blue player before he fights
				blueTeamInitial.get(i).setCurrentCp(blueTeamInitial.get(i).getMaxCp());
				blueTeamInitial.get(i).setCurrentHp(blueTeamInitial.get(i).getMaxHp());
				blueTeamInitial.get(i).setCurrentMp(blueTeamInitial.get(i).getMaxMp());

				for (L2Skill skill : blueTeamInitial.get(i).getAllSkills())
				{
					blueTeamInitial.get(i).enableSkill(skill.getId());
				}

				blueTeamInitial.get(i).sendPacket(new SkillCoolTime(blueTeamInitial.get(i)));
				blueTeamInitial.get(i).sendMessage("Your skills have been reseted.");
				
				blueTeamInitial.get(i).setForceNoSpawnProtection(true);
				blueTeamInitial.get(i).broadcastUserInfo();
				// blue player reconnects
			}
			else // make the rest sit
			{
				if (!blueTeamInitial.get(i).eventSitForced)
				{
					blueTeamInitial.get(i).eventSitForced = true;
					blueTeamInitial.get(i).teleToLocation(BLUE_LOCATIONS[i], false);
					blueTeamInitial.get(i).startAbnormalEffect(AbnormalEffect.HOLD_1);
					blueTeamInitial.get(i).setIsParalyzed(true);
					StopMove sm = new StopMove(currentBluePlayer);
					blueTeamInitial.get(i).sendPacket(sm);
					blueTeamInitial.get(i).broadcastPacket(sm);
					waitSecs(1);
					blueTeamInitial.get(i).broadcastUserInfo();
					blueTeamInitial.get(i).sitDown();
				}
			}
			blueTeamInitial.get(i).broadcastUserInfo();
			blueTeamInitial.get(i).broadcastStatusUpdate();
		}
		for (int i = 0; i < redTeamInitial.size(); i++)
		{
			if (redTeamInitial.get(i) == null)
				continue;
			if (redTeamInitial.get(i).getObjectId() == currentRedPlayer.getObjectId())// fighting red player
			{
				if (redTeamInitial.get(i).eventSitForced)
				{
					redTeamInitial.get(i).eventSitForced = false;
					redTeamInitial.get(i).standUp();
					currentRedPlayer.broadcastUserInfo();
					waitSecs(3);
				}
				redTeamInitial.get(i).teleToLocation(locFightingRed, false);
				// heal red player before he fights
				redTeamInitial.get(i).setCurrentCp(redTeamInitial.get(i).getMaxCp());
				redTeamInitial.get(i).setCurrentHp(redTeamInitial.get(i).getMaxHp());
				redTeamInitial.get(i).setCurrentMp(redTeamInitial.get(i).getMaxMp());

				for (L2Skill skill : redTeamInitial.get(i).getAllSkills())
				{
					redTeamInitial.get(i).enableSkill(skill.getId());
				}

				redTeamInitial.get(i).sendPacket(new SkillCoolTime(redTeamInitial.get(i)));
				redTeamInitial.get(i).sendMessage("Your skills have been reseted.");
				
				redTeamInitial.get(i).setForceNoSpawnProtection(true);
				redTeamInitial.get(i).broadcastUserInfo();
				// red player reconnects
				if (!redTeamInitial.get(i).getKoreanTeam().equalsIgnoreCase("red"))
				{
					redTeamInitial.get(i).setKoreanTeam("red");
					redTeamInitial.get(i).broadcastUserInfo();
				}
			}
			else // make the rest sit
			{
				if (!redTeamInitial.get(i).eventSitForced)
				{
					redTeamInitial.get(i).eventSitForced = true;
					redTeamInitial.get(i).teleToLocation(RED_LOCATIONS[i], false);
					
					redTeamInitial.get(i).startAbnormalEffect(AbnormalEffect.HOLD_1);
					redTeamInitial.get(i).setIsParalyzed(true);
					StopMove sm = new StopMove(currentBluePlayer);
					redTeamInitial.get(i).sendPacket(sm);
					redTeamInitial.get(i).broadcastPacket(sm);
					
					waitSecs(1);
					redTeamInitial.get(i).sitDown();
					redTeamInitial.get(i).broadcastUserInfo();
				}
			}
			redTeamInitial.get(i).broadcastUserInfo();
			redTeamInitial.get(i).broadcastStatusUpdate();
		}
		// blue para
		currentBluePlayer.startAbnormalEffect(AbnormalEffect.HOLD_1);
		currentBluePlayer.setIsParalyzed(true);
		StopMove sm = new StopMove(currentBluePlayer);
		currentBluePlayer.sendPacket(sm);
		currentBluePlayer.broadcastPacket(sm);
		// red para
		currentRedPlayer.startAbnormalEffect(AbnormalEffect.HOLD_1);
		currentRedPlayer.setIsParalyzed(true);
		sm = new StopMove(currentRedPlayer);
		currentRedPlayer.sendPacket(sm);
		currentRedPlayer.broadcastPacket(sm);
		
		for (L2PcInstance p : players)
		{
			if (p == null)
				continue;
			ExShowScreenMessage message = new ExShowScreenMessage("(RED)"+currentRedPlayer.getName() +"  -VS-  " + "(BLUE)"+currentBluePlayer.getName() , 4000);
			p.sendPacket(message);
		}
		waitSecs(4);
		for (int i = 10; i > 0; i--)
		{
			for (L2PcInstance p : players)
			{
				if (p == null)
					continue;
				ExShowScreenMessage message = new ExShowScreenMessage("" + i, 999);
				p.sendPacket(message);
			}
			waitSecs(1);
		}
		for (L2PcInstance p : players)
		{
			if (p == null)
				continue;
			ExShowScreenMessage message = new ExShowScreenMessage("Let's go!", 999);
			p.sendPacket(message);
		}
		// blue unpara
		currentBluePlayer.stopAbnormalEffect(AbnormalEffect.HOLD_1);
		currentBluePlayer.setIsParalyzed(false);
		// red unpara
		currentRedPlayer.stopAbnormalEffect(AbnormalEffect.HOLD_1);
		currentRedPlayer.setIsParalyzed(false);
	}
	
	private void checkForDeadPlayers()
	{
		if (currentBluePlayer.isDead())
		{
			L2PcInstance oldPlayer = currentBluePlayer;
			ThreadPoolManager.getInstance().scheduleGeneral(() ->
			{
				if (oldPlayer != null)
				{
					oldPlayer.doRevive();
					oldPlayer.broadcastStatusUpdate();
					oldPlayer.broadcastUserInfo();
				}
			}, 2500);
			blueTeam.remove(currentBluePlayer);
			if (blueTeam.size() > 0)
			{
				currentBluePlayer = blueTeam.get(0);
			}
			else
			{
				endRoom();
				return;
			}
			currentRoundEndedWithKill = true;
		}
		if (currentRedPlayer.isDead())
		{
			L2PcInstance oldPlayer = currentRedPlayer;
			ThreadPoolManager.getInstance().scheduleGeneral(() ->
			{
				if (oldPlayer != null)
				{
					oldPlayer.doRevive();
					oldPlayer.broadcastStatusUpdate();
					oldPlayer.broadcastUserInfo();
				}
			}, 2500);
			redTeam.remove(currentRedPlayer);
			if (redTeam.size() > 0)
			{
				currentRedPlayer = redTeam.get(0);
			}
			else
			{
				endRoom();
				return;
			}
			currentRoundEndedWithKill = true;
		}
	}
	
	private void roundEndedWithoutKillOrDisconnect(int round)
	{
		boolean disconnected = false;
		if (currentBluePlayer == null)
		{
			disconnected = true;
			blueTeam.remove(currentBluePlayer);
			if (blueTeam.size() > 0)
			{
				currentBluePlayer = blueTeam.get(0);
			}
			else
			{
				endRoom();
				return;
			}
		}
		if (currentRedPlayer == null)
		{
			disconnected = true;
			redTeam.remove(currentRedPlayer);
			if (redTeam.size() > 0)
			{
				currentRedPlayer = redTeam.get(0);
			}
			else
			{
				endRoom();
				return;
			}
		}
		if (disconnected)
			return;
		if (currentBluePlayer.getCurrentHp() / currentBluePlayer.getMaxHp() > currentRedPlayer.getCurrentHp() / currentRedPlayer.getMaxHp())
		{
			redTeam.remove(currentRedPlayer);
			if (redTeam.size() > 0)
			{
				currentRedPlayer = redTeam.get(0);
			}
			else
			{
				endRoom();
				return;
			}
		}
		else
		{
			blueTeam.remove(currentBluePlayer);
			if (blueTeam.size() > 0)
			{
				currentBluePlayer = blueTeam.get(0);
			}
			else
			{
				endRoom();
				return;
			}
		}
	}
	
	private void checkForDisconnects()
	{
		if (currentBluePlayer.isOnline() == 0 || currentBluePlayer.getActingPlayer() == null)
		{
			currentRoundDisconnected = true;
			blueTeam.remove(currentBluePlayer);
			if (blueTeam.size() > 0)
			{
				currentBluePlayer = blueTeam.get(0);
			}
			else
			{
				endRoom();
				return;
			}
		}
		if (currentRedPlayer.getActingPlayer() == null || currentRedPlayer.isOnline() == 0)
		{
			currentRoundDisconnected = true;
			redTeam.remove(currentRedPlayer);
			if (redTeam.size() > 0)
			{
				currentRedPlayer = redTeam.get(0);
			}
			else
			{
				endRoom();
				return;
			}
		}
	}
	
	private void endRoom()
	{
		if (!activeRoom)
			return;
		// first reward the winners
		if (blueTeam.size() == 0)
		{
			for (L2PcInstance p : redTeamInitial)
			{
				if (p != null)
					p.addItem("KoreanReward", 57, 10000, p, true);
			}
		}
		else
		{
			for (L2PcInstance p : blueTeamInitial)
			{
				if (p != null)
					p.addItem("KoreanReward", 57, 10000, p, true);
			}
		}
		for (L2PcInstance p : players)
		{
			if (p == null)
				continue;
			p.eventSitForced = false;
			
			p.stopAbnormalEffect(AbnormalEffect.HOLD_1);
			p.setIsParalyzed(false);
			StopMove sm = new StopMove(p);
			sm = new StopMove(p);
			p.sendPacket(sm);
			p.broadcastPacket(sm);
			ExShowScreenMessage message = new ExShowScreenMessage("You will be teleported in Giran Town in 10 seconds", 5000);
			p.sendPacket(message);
		}
		waitSecs(10);
		// then teleport players back and reset their values
		for (L2PcInstance p : players)
		{
			if (p == null)
				continue;
			p.teleToLocation(-81975, 150815, -3129);
			p.setInstanceId(0);
			p.setIsInKoreanEvent(false);
			p.setKoreanTeam("");
			p.broadcastUserInfo();
			p.setIsInActiveKoreanRoom(false);
			p._koreanKills = 0;
		}
		activeRoom = false;
		clearRoom();// give some empty space to the server
	}
	
	private void waitSecs(int i)
	{
		try
		{
			Thread.sleep(i * 1000);
		}
		catch (InterruptedException ie)
		{
			ie.printStackTrace();
		}
	}
	
	private void clearRoom()
	{
		this.blueTeam.clear();
		this.redTeam.clear();
		this.currentBluePlayer = null;
		this.currentRedPlayer = null;
		this.blueTeamInitial.clear();
		this.redTeamInitial.clear();
		this.players.clear();// flag
		this.currentRound = 1;
		this.activeRoom = false;
		Korean.getInstance().rooms.remove(this);
	}
	
	@Override
	public void run()
	{
		executeRoom(roomId, players);
	}
}