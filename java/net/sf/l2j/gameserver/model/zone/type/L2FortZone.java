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
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * A castle zone
 *
 * @author durgus
 */
public class L2FortZone extends L2ZoneType
{
	private int			_fortId;
	private Fort		_fort	= null;
	private final int[]	_spawnLoc;
	
	public L2FortZone(int id)
	{
		super(id);
		_spawnLoc = new int[3];
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("fortId"))
		{
			_fortId = Integer.parseInt(value);
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
		else
			super.setParameter(name, value);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_FORT, true);
		if (getFort() != null && getFort().getSiege().getIsInProgress())
		{
			character.setInsideZone(L2Character.ZONE_PVP, true);
			character.setInsideZone(L2Character.ZONE_SIEGE, true);
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
				if (((L2PcInstance) character).getClan() != null && (_fort.getSiege().checkIsAttacker(((L2PcInstance) character).getClan()) || _fort.getSiege().checkIsDefender(((L2PcInstance) character).getClan())))
				{
					((L2PcInstance) character).startFameTask(Config.FORTRESS_ZONE_FAME_TASK_FREQUENCY * 1000, Config.FORTRESS_ZONE_FAME_AQUIRE_POINTS);
					((L2PcInstance) character).setIsInSiege(true);
				}
				else
				{
					if (!character.isGM())
					{
						if(!((L2PcInstance) character).isHero())
						{
							character.teleToLocation(83380, 148107, -3404, true);
							((L2PcInstance) character).setIsInSiege(false);
							character.setInsideZone(L2Character.ZONE_PVP, false);
							character.setInsideZone(L2Character.ZONE_SIEGE, false);
							character.setInsideZone(L2Character.ZONE_FORT, false);
						}
					}
				}
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_FORT, false);
		if (getFort() != null && getFort().getSiege().getIsInProgress())
		{
			character.setInsideZone(L2Character.ZONE_PVP, false);
			character.setInsideZone(L2Character.ZONE_SIEGE, false);
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
				((L2PcInstance) character).stopFameTask();
				((L2PcInstance) character).setIsInSiege(false);
				/*
				 * // Set pvp flag
				 * if (((L2PcInstance) character).getPvpFlag() == 0)
				 * ((L2PcInstance) character).startPvPFlag();
				 */
			}
		}
		if (character instanceof L2SiegeSummonInstance)
		{
			((L2SiegeSummonInstance) character).unSummon(((L2SiegeSummonInstance) character).getOwner());
		}
		if (character instanceof L2PcInstance)
		{
			L2PcInstance activeChar = ((L2PcInstance) character);
			if (activeChar.getInventory().getItemByItemId(9819) != null)
			{
				Fort fort = FortManager.getInstance().getFort(activeChar);
				if (fort != null)
				{
					FortSiegeManager.getInstance().dropCombatFlag(activeChar);
				}
				else
				{
					int slot = activeChar.getInventory().getSlotFromItem(activeChar.getInventory().getItemByItemId(9819));
					activeChar.getInventory().unEquipItemInBodySlotAndRecord(slot);
					activeChar.destroyItem("CombatFlag", activeChar.getInventory().getItemByItemId(9819), null, true);
				}
			}
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
		if (getFort() != null && getFort().getSiege().getIsInProgress())
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
				L2Clan clan;
				L2Skill skill;
				if (getFort().getOwnerClan() == ((L2PcInstance) character).getClan())
				{
					skill = SkillTable.getInstance().getInfo(5660, lvl);
					if (skill != null)
						skill.getEffects(character, character);
				}
				else
				{
					for (L2SiegeClan siegeclan : getFort().getSiege().getAttackerClans())
					{
						if (siegeclan == null)
							continue;
						clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
						if (((L2PcInstance) character).getClan() == clan)
						{
							skill = SkillTable.getInstance().getInfo(5660, lvl);
							if (skill != null)
								skill.getEffects(character, character);
							break;
						}
					}
				}
			}
		}
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{}
	
	public void updateZoneStatusForCharactersInside()
	{
		if (getFort() != null && getFort().getSiege().getIsInProgress())
		{
			for (L2Character character : _characterList.values())
			{
				try
				{
					onEnter(character);
				}
				catch (NullPointerException e)
				{
					e.printStackTrace();
				}
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
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Removes all foreigners from the fort
	 * 
	 * @param owningClan
	 */
	public void banishForeigners(L2Clan owningClan)
	{
		for (L2Character temp : _characterList.values())
		{
			if (!(temp instanceof L2PcInstance))
				continue;
			if (temp.isGM())
				continue;
			if (!temp.getActingPlayer().isCursedWeaponEquipped() && ((L2PcInstance) temp).getClanId() == owningClan.getClanId())
				continue;
			((L2PcInstance) temp).teleToLocation(MapRegionTable.TeleportWhereType.Town); // TODO: shouldnt be town, its outside of fort
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
	
	public int getFortId()
	{
		return _fortId;
	}
	
	private final Fort getFort()
	{
		if (_fort == null)
			_fort = FortManager.getInstance().getFortById(_fortId);
		return _fort;
	}
	
	/**
	 * Get the forts defender spawn
	 * 
	 * @return
	 */
	public int[] getSpawn()
	{
		return _spawnLoc;
	}
}
