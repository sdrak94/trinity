/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.zone.type;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * A castle zone
 *
 * @author durgus
 */
public class L2CastleZone extends L2ZoneType
{
	private int			_gearLimit	= -1;
	private int			_castleId;
	private Castle		_castle		= null;
	private final int[]	_spawnLoc;
	private int[]		_portInLoc;
	
	public L2CastleZone(int id)
	{
		super(id);
		_spawnLoc = new int[3];
		_portInLoc = new int[3];
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
		}
		else if (name.equals("spawnX"))
		{
			_spawnLoc[0] = Integer.parseInt(value);
		}
		else if (name.equals("spawnY"))
		{
			_spawnLoc[1] = Integer.parseInt(value);
		}
		else if (name.equals("spawnZ"))
		{
			_spawnLoc[2] = Integer.parseInt(value);
		}
		else if (name.equals("portInX"))
		{
			_portInLoc[0] = Integer.parseInt(value);
		}
		else if (name.equals("portInY"))
		{
			_portInLoc[1] = Integer.parseInt(value);
		}
		else if (name.equals("portInZ"))
		{
			_portInLoc[2] = Integer.parseInt(value);
		}
		else if (name.equals("gearLimit"))
		{
			_gearLimit = Integer.parseInt(value);
		}
		else
			super.setParameter(name, value);
		if (_portInLoc[0] == 0) // portin unset
		{
			_portInLoc = _spawnLoc;
		}
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (getCastle() != null && getCastle().getSiege().getIsInProgress())
		{
			character.setInsideZone(L2Character.ZONE_PVP, true);
			character.setInsideZone(L2Character.ZONE_SIEGE, true);
			character.setInsideZone(L2Character.ZONE_CASTLE, true);
			character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, true);
			if (character instanceof L2PcInstance)
			{
				boolean canParticipate = (character.getActingPlayer().getClan() != null && character.getActingPlayer().getClan().getHasCastle() == 0 && (_castle.getSiege().checkIsAttacker(((L2PcInstance) character).getClan()) || _castle.getSiege().checkIsDefender(((L2PcInstance) character).getClan()))) || (character.getActingPlayer().isHero() && !character.getActingPlayer().isFakeHero() && !character.getActingPlayer()._tempHero);
				if (!canParticipate && character.getActingPlayer().getClan() != null)
				{
					if (_castle.getOwnerId() == character.getActingPlayer().getClanId() || _castle.getSiege().checkIsAttacker(((L2PcInstance) character).getClan()) || _castle.getSiege().checkIsDefender(((L2PcInstance) character).getClan()))
					{
						canParticipate = true;
					}
					else if (character.getActingPlayer().getClan().getHasCastle() == 0)
					{
						for (L2SiegeClan clan : _castle.getSiege().getAttackerClans())
						{
							try
							{
								if (ClanTable.getInstance().getClan(clan.getClanId()).getLeader().getPlayerInstance().isAlliedWith(character))
								{
									canParticipate = true;
									break;
								}
							}
							catch (Exception e)
							{}
						}
						if (!canParticipate)
						{
							for (L2SiegeClan clan : _castle.getSiege().getDefenderClans())
							{
								try
								{
									if (ClanTable.getInstance().getClan(clan.getClanId()).getLeader().getPlayerInstance().isAlliedWith(character))
									{
										canParticipate = true;
										break;
									}
								}
								catch (Exception e)
								{}
							}
						}
					}
				}
				if (canParticipate)
				{
					((L2PcInstance) character).setIsInSiege(true);
					character.setTitle("", false);
					SystemMessage sm = new SystemMessage(SystemMessageId.TITLE_CHANGED);
					character.sendPacket(sm);
					character.getActingPlayer().broadcastTitleInfo();
					((L2PcInstance) character).startFameTask(Config.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, Config.CASTLE_ZONE_FAME_AQUIRE_POINTS);
				}
				else if (!character.getActingPlayer().inObserverMode() && !character.getActingPlayer().isGM())
				{
					character.sendMessage("You cannot participate in this siege");
					character.teleToLocation(TeleportWhereType.Town);
					return;
				}
				else if (((L2PcInstance) character).getClan() != null && !(_castle.getSiege().checkIsAttacker(((L2PcInstance) character).getClan()) || _castle.getSiege().checkIsDefender(((L2PcInstance) character).getClan())))
				{
					character.sendMessage("You cannot participate in this siege");
					character.teleToLocation(83380, 148107, -3404, true);
					return;
				}
				((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
				if (_gearLimit >= 0)
				{
					if (!character.isGM())
						character.getActingPlayer().checkItemRestrictionZone(_gearLimit);
					character.getActingPlayer()._gearLimit = _gearLimit;
					String name = null;
					switch (_gearLimit)
					{
						case 0:
							name = "S grade only zone";
							break;
						case 1:
							name = "S80 and below zone";
							break;
						case 2:
							name = "Vesper and below zone";
							break;
						case 3:
							name = "Titanium and below zone";
							break;
						case 4:
							name = "Dread and below zone";
							break;
						case 5:
							name = "Forbidden and below zone";
							break;
					}
					character.getActingPlayer().sendMessage("You have entered a " + name);
				}
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			if (_gearLimit >= 0)
			{
				character.getActingPlayer()._gearLimit = -1;
			}
		}
		if (getCastle() != null && getCastle().getSiege().getIsInProgress())
		{
			character.setInsideZone(L2Character.ZONE_PVP, false);
			character.setInsideZone(L2Character.ZONE_SIEGE, false);
			character.setInsideZone(L2Character.ZONE_CASTLE, false);
			character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, false);
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
				((L2PcInstance) character).stopFameTask();
				((L2PcInstance) character).setIsInSiege(false);
			}
		}
		if (character instanceof L2SiegeSummonInstance)
		{
			((L2SiegeSummonInstance) character).unSummon(((L2SiegeSummonInstance) character).getOwner());
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
		if (getCastle() != null && getCastle().getSiege().getIsInProgress())
		{
			// debuff participants only if they die inside siege zone
			if (character instanceof L2PcInstance && ((L2PcInstance) character).getClan() != null)
			{
				int lvl = 1;
				for (L2Effect effect : character.getAllEffects())
				{
					if (effect != null && effect.getSkill().getId() == 5660)
					{
						lvl = lvl + effect.getLevel();
						if (lvl > 5)
							lvl = 5;
						break;
					}
				}
				L2Skill skill;
				if (getCastle().getSiege().getAttackerClans().contains(((L2PcInstance) character).getClan()) || getCastle().getSiege().getDefenderClans().contains(((L2PcInstance) character).getClan()))
				{
					skill = SkillTable.getInstance().getInfo(5660, lvl);
					if (skill != null)
						skill.getEffects(character, character);
				}
			}
		}
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{}
	
	public void updateZoneStatusForCharactersInside()
	{
		if (getCastle() != null && getCastle().getSiege().getIsInProgress())
		{
			for (L2Character character : _characterList.values())
			{
				try
				{
					onEnter(character);
				}
				catch (NullPointerException e)
				{}
			}
		}
		else
		{
			for (L2Character character : _characterList.values())
			{
				try
				{
					character.setInsideZone(L2Character.ZONE_PVP, false);
					character.setInsideZone(L2Character.ZONE_SIEGE, false);
					character.setInsideZone(L2Character.ZONE_CASTLE, false);
					character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, false);
					if (character instanceof L2PcInstance)
					{
						((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
						((L2PcInstance) character).stopFameTask();
					}
					if (character instanceof L2SiegeSummonInstance)
					{
						((L2SiegeSummonInstance) character).unSummon(((L2SiegeSummonInstance) character).getOwner());
					}
				}
				catch (NullPointerException e)
				{}
			}
		}
	}
	
	/**
	 * Removes all foreigners from the castle
	 * 
	 * @param owningClanId
	 */
	public void banishForeigners(int owningClanId)
	{
		for (L2Character temp : _characterList.values())
		{
			if (!(temp instanceof L2PcInstance))
				continue;
			if (temp.isGM())
				continue;
			if (!temp.getActingPlayer().isCursedWeaponEquipped() && ((L2PcInstance) temp).getClanId() == owningClanId)
				continue;
			((L2PcInstance) temp).teleToLocation(MapRegionTable.TeleportWhereType.Town);
		}
	}
	
	/**
	 * Sends a message to all players in this zone
	 * 
	 * @param message
	 */
	public void announceToPlayers(String message)
	{
		for (L2Character temp : _characterList.values())
		{
			if (temp instanceof L2PcInstance)
				((L2PcInstance) temp).sendMessage(message);
		}
	}
	
	/**
	 * Returns all players within this zone
	 * 
	 * @return
	 */
	public FastList<L2PcInstance> getAllPlayers()
	{
		FastList<L2PcInstance> players = new FastList<L2PcInstance>();
		for (L2Character temp : _characterList.values())
		{
			if (temp instanceof L2PcInstance)
				players.add((L2PcInstance) temp);
		}
		return players;
	}
	
	public int getCastleId()
	{
		return _castleId;
	}
	
	private final Castle getCastle()
	{
		if (_castle == null)
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		return _castle;
	}
	
	/**
	 * Get the castles defender spawn
	 * 
	 * @return
	 */
	public int[] getSpawn()
	{
		return _spawnLoc;
	}
	
	public int[] getPortInLoc()
	{
		return _portInLoc;
	}
}
