package net.sf.l2j.gameserver.model.zone.type;

import ghosts.model.Ghost;
import javolution.util.FastList;
import luna.custom.holder.LunaGlobalVariablesHolder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.util.Rnd;

public class L2TownZone extends L2ZoneType
{
	private String					_townName;
	private int						_townId;
	private int						_redirectTownId;
	private int						_taxById;
	private boolean					_isPeaceZone;
	private int[]					_spawnLoc;
	private final FastList<int[]>	_respawnPoints;
	
	public L2TownZone(int id)
	{
		super(id);
		_taxById = 0;
		_respawnPoints = new FastList<int[]>();
		_spawnLoc = new int[3];
		// Default to Giran
		_redirectTownId = 9;
		// Default peace zone
		_isPeaceZone = true;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("name"))
		{
			_townName = value;
		}
		else if (name.equals("townId"))
		{
			_townId = Integer.parseInt(value);
		}
		else if (name.equals("redirectTownId"))
		{
			_redirectTownId = Integer.parseInt(value);
		}
		else if (name.equals("taxById"))
		{
			_taxById = Integer.parseInt(value);
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
			_respawnPoints.add(_spawnLoc);
			_spawnLoc = new int[3];
		}
		else if (name.equals("isPeaceZone"))
		{
			_isPeaceZone = Boolean.parseBoolean(value);
		}
		else
			super.setParameter(name, value);
	}
	
	@SuppressWarnings("unused")
	@Override
	protected void onEnter(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) character;
			if (player == null)
				return;
			if (_townId == 5) // gludin
			{
				if (!player.isInGludin())
				{
					player.setIsInGludin(true);
					if (Config.HWID_FARMZONES_CHECK)
					{
						String hwid = ((L2PcInstance) character).getHWID();
						for (L2PcInstance player1 : L2World.getInstance().getAllPlayers().values())
						{

							if (!(player1 instanceof Ghost) && player1.getClient().isDetached())
							{
								continue;
							}
							if (!(player1.isInsideZone(L2Character.ZONE_FARM) || player1.isInsideZone(L2Character.ZONE_RAID) || player1.isInsideZone(L2Character.ZONE_CHAOTIC) || player1.isInsideZone(L2Character.ZONE_EVENT) || player1.isInPI() || player1.isInHuntersVillage()))
							{
								continue;
							}
							if (player1 == character)
							{
								continue;
							}
							if (player1.isGM() || character.isGM())
							{
								continue;
							}
							String plr_hwid = player1.getHWID();
							if (plr_hwid.equalsIgnoreCase(hwid))
							{
								character.setIsPendingRevive(true);
								character.getActingPlayer().setIsInGludin(false);
								character.getActingPlayer().stopPvPFlag();
								character.getActingPlayer().broadcastUserInfo();
								character.getActingPlayer().sendMessage("You have left Gludin Village.");
								character.sendMessage("You have another window in a hwid restricted zone.");
								character.teleToLocation(83380, 148107, -3404, true);
								break;
							}
							else
							{
								if (LunaGlobalVariablesHolder.getInstance().getAutoFlagGludin())
								{
									player.updatePvPFlag(1);
									player.sendMessage("PvP Flag status updated");
									player.broadcastUserInfo();
								}
								if (character.isGM())
								{
									character.sendMessage("You have entered the Gludin Village");
								}
							}
						}
					}
				}
			}
			else if (_townId == 50)
			{
				player.sendMessage("You have entered Primeval Isle");
				if (!player.isInPI())
				{
					player.setIsInPI(true);
					if (!player.isGM())
					{
						player.updatePvPFlag(1);
						player.sendMessage("PvP Flag status updated");
						try
						{
							if (player.isCursedWeaponEquipped())
								CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
						}
						catch (Exception e)
						{}
						// player.setDisguised(true);
						player.broadcastUserInfo();
						if (Config.HWID_FARMZONES_CHECK)
						{
							String hwid = ((L2PcInstance) character).getHWID();
							for (L2PcInstance player1 : L2World.getInstance().getAllPlayers().values())
							{
								if (player1.getClient().isDetached())
								{
									continue;
								}
								if (!(player1.isInsideZone(L2Character.ZONE_FARM) || player1.isInsideZone(L2Character.ZONE_RAID) || player1.isInsideZone(L2Character.ZONE_CHAOTIC) || player1.isInsideZone(L2Character.ZONE_EVENT) || player1.isInPI() || player1.isInHuntersVillage()))
								{
									continue;
								}
								if (player1 == character)
								{
									continue;
								}
								if (player1.isGM() || character.isGM())
								{
									continue;
								}
								String plr_hwid = player1.getHWID();
								if (plr_hwid.equalsIgnoreCase(hwid))
								{
									character.setIsPendingRevive(true);
									character.getActingPlayer().setIsInPI(false);
									character.getActingPlayer().stopPvPFlag();
									character.getActingPlayer().broadcastUserInfo();
									character.getActingPlayer().sendMessage("You have left Primeval Isle.");
									character.sendMessage("You have another window in a hwid restricted zone.");
									character.teleToLocation(83380, 148107, -3404, true);
									break;
								}
								else
								{
									if (character.isGM())
									{
										character.sendMessage("You have entered the Primeval Isle PvP Area");
									}
								}
							}
						}
					}
				}
			}
			/*
			 * else if (_townId == 9) //Giran
			 * {
			 * if (!player.isInGiran())
			 * player.setIsInGiran(true);
			 * }
			 */
			else if (_townId == 11) // Hunters
			{
				player.sendMessage("You have entered Hunter's Village");
				if (!player.isInHuntersVillage())
				{
					player.setInHuntersVillage(true);
					if (!player.isGM())
					{
						if (player.isInParty())
							player.leaveParty();
						player.updatePvPFlag(1);
						player.sendMessage("PvP Flag status updated");
						try
						{
							if (player.isCursedWeaponEquipped())
								CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
						}
						catch (Exception e)
						{}
						if (player.isMounted())
							player.dismount();
						if (player.isTransformed())
							player.stopTransformation(null);
						player.setDisguised(true);
						player.broadcastUserInfo();
						if (Config.HWID_FARMZONES_CHECK)
						{
							String hwid = ((L2PcInstance) character).getHWID();
							for (L2PcInstance player1 : L2World.getInstance().getAllPlayers().values())
							{
								if (player1.getClient().isDetached())
								{
									continue;
								}
								if (!(player1.isInsideZone(L2Character.ZONE_FARM) || player1.isInsideZone(L2Character.ZONE_RAID) || player1.isInsideZone(L2Character.ZONE_CHAOTIC) || player1.isInsideZone(L2Character.ZONE_EVENT) || (player1.isInHuntersVillage())))
								{
									continue;
								}
								if (player1 == character)
								{
									continue;
								}
								if (player1.isGM() || character.isGM())
								{
									continue;
								}
								String plr_hwid = player1.getHWID();
								if (plr_hwid.equalsIgnoreCase(hwid))
								{
									character.setIsPendingRevive(true);
									character.getActingPlayer().setDisguised(false);
									character.getActingPlayer().setInHuntersVillage(false);
									character.getActingPlayer().stopPvPFlag();
									character.getActingPlayer().broadcastUserInfo();
									character.getActingPlayer().sendMessage("You have left Hunter's Village");
									character.sendMessage("You have another window in a hwid restricted zone.");
									character.teleToLocation(83380, 148107, -3404, true);
									break;
								}
								else
								{
									if (character.isGM())
									{
										character.sendMessage("You have entered the Hunter's Village Area");
									}
								}
							}
						}
						if (!player.getAppearance().getSex() && player.isSpawned())
						{
							for (L2PcInstance nigga : player.getKnownList().getKnownPlayers().values())
							{
								if (nigga != null && nigga.getKnownList() != null && nigga.getKnownList().knowsObject(player))
								{
									nigga.getKnownList().removeKnownObject(player);
									nigga.getKnownList().addKnownObject(player);
								}
							}
						}
					}
				}
			}
			else if (_townId == 4) // orc village
			{ // Hunters
				player.sendMessage("You have entered Hunter's Village");
				if (!player.isInOrcVillage())
				{
					player.setIsInS80zone(true);
					player.setInOrcVillage(true);
					if (!player.isGM())
					{
						if (player.isInParty())
							player.leaveParty();
						player.updatePvPFlag(1);
						player.sendMessage("PvP Flag status updated");
						try
						{
							if (player.isCursedWeaponEquipped())
								CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
						}
						catch (Exception e)
						{}
						if (player.isMounted())
							player.dismount();
						if (player.isTransformed())
							player.stopTransformation(null);
						player.setDisguised(true);
						player.broadcastUserInfo();
						if (Config.HWID_FARMZONES_CHECK)
						{
							String hwid = ((L2PcInstance) character).getHWID();
							for (L2PcInstance player1 : L2World.getInstance().getAllPlayers().values())
							{
								if (player1 instanceof Ghost || character instanceof Ghost)
									continue;
								if (player1.getClient().isDetached())
								{
									continue;
								}
								if (!(player1.isInsideZone(L2Character.ZONE_FARM) || player1.isInsideZone(L2Character.ZONE_RAID) || player1.isInsideZone(L2Character.ZONE_CHAOTIC) || player1.isInsideZone(L2Character.ZONE_EVENT) || (player1.isInHuntersVillage())))
								{
									continue;
								}
								if (player1 == character)
								{
									continue;
								}
								if (player1.isGM() || character.isGM())
								{
									continue;
								}
								String plr_hwid = player1.getClient().getFullHwid();
								if (plr_hwid.equalsIgnoreCase(hwid))
								{
									character.setIsPendingRevive(true);
									character.getActingPlayer().setDisguised(false);
									character.getActingPlayer().setInHuntersVillage(false);
									character.getActingPlayer().stopPvPFlag();
									character.getActingPlayer().broadcastUserInfo();
									character.getActingPlayer().sendMessage("You have left Hunter's Village");
									character.sendMessage("You have another window in a hwid restricted zone.");
									character.teleToLocation(83380, 148107, -3404, true);
									break;
								}
								else
								{
									if (character.isGM())
									{
										character.sendMessage("You have entered the Hunter's Village Area");
									}
								}
							}
						}
						boolean update = false;
						for (L2ItemInstance item : player.getInventory().getItems())
						{
							if (item != null && item.isEquipped())
							{
								if (item.getUniqueness() > 3.5)
								{
									if (item.isAugmented())
										item.getAugmentation().removeBonus(player);
									L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(player.getInventory().getSlotFromItem(item));
									InventoryUpdate iu = new InventoryUpdate();
									for (L2ItemInstance element : unequiped)
										iu.addModifiedItem(element);
									player.sendPacket(iu);
									update = true;
								}
							}
						}
						player.sendMessage("You have entered the newbie pvp zone, all items above tier 3.5 are not permitted here.");
						if (update)
							player.broadcastUserInfo();
						if (!player.getAppearance().getSex() && player.isSpawned())
						{
							for (L2PcInstance nigga : player.getKnownList().getKnownPlayers().values())
							{
								if (nigga != null && nigga.getKnownList() != null && nigga.getKnownList().knowsObject(player))
								{
									nigga.getKnownList().removeKnownObject(player);
									nigga.getKnownList().addKnownObject(player);
								}
							}
						}
					}
				}
			}
			/*
			 * else if (_townId == 13) //goddard
			 * {
			 * }
			 */
			// PVP possible during siege, now for siege participants only
			// Could also check if this town is in siege, or if any siege is going on
			if (player.getSiegeState() != 0 && Config.ZONE_TOWN == 1)
				return;
			// ((L2PcInstance)character).sendMessage("You entered "+_townName);
		}
		if (_isPeaceZone && Config.ZONE_TOWN != 2)
			character.setInsideZone(L2Character.ZONE_PEACE, true);
		character.setInsideZone(L2Character.ZONE_TOWN, true);
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (_townId == 5) // gludin
		{
			if (character instanceof L2PcInstance && character.getActingPlayer().isInGludin())
			{
				character.getActingPlayer().setIsInGludin(false);
			}
		}
		else if (_townId == 11)
		{
			if (character instanceof L2PcInstance && character.getActingPlayer().isInHuntersVillage())
			{
				character.getActingPlayer().setDisguised(false);
				character.getActingPlayer().broadcastUserInfo();
				character.getActingPlayer().setInHuntersVillage(false);
				character.getActingPlayer().stopPvPFlag();
				character.getActingPlayer().sendMessage("You have left Hunter's Village");
			}
		}
		else if (_townId == 4) // orc village
		{
			if (character instanceof L2PcInstance && character.getActingPlayer().isInOrcVillage())
			{
				character.getActingPlayer().setDisguised(false);
				character.getActingPlayer().broadcastUserInfo();
				character.getActingPlayer().setInOrcVillage(false);
				character.getActingPlayer().setIsInS80zone(false);
				character.getActingPlayer().stopPvPFlag();
				character.getActingPlayer().sendMessage("You have left Hunter's Village");
			}
		}
		else if (_townId == 50)
		{
			if (character instanceof L2PcInstance && character.getActingPlayer().isInPI())
			{
				character.getActingPlayer().broadcastUserInfo();
				character.getActingPlayer().setIsInPI(false);
				character.getActingPlayer().stopPvPFlag();
				character.getActingPlayer().sendMessage("You have left Primeval Isle");
			}
		}
		if (_isPeaceZone)
		{
			character.setInsideZone(L2Character.ZONE_PEACE, false);
		}
		character.setInsideZone(L2Character.ZONE_TOWN, false);
	}
	
	@Override
	public void onDieInside(L2Character character)
	{}
	
	@Override
	public void onReviveInside(L2Character character)
	{}
	
	/**
	 * Returns this town zones name
	 * 
	 * @return
	 */
	@Deprecated
	public String getName()
	{
		return _townName;
	}
	
	/**
	 * Returns this zones town id (if any)
	 * 
	 * @return
	 */
	public int getTownId()
	{
		return _townId;
	}
	
	/**
	 * Gets the id for this town zones redir town
	 * 
	 * @return
	 */
	@Deprecated
	public int getRedirectTownId()
	{
		return _redirectTownId;
	}
	
	/**
	 * Returns this zones spawn location
	 * 
	 * @return
	 */
	public final int[] getSpawnLoc()
	{
		final int size = _respawnPoints.size();
		if (size == 1)
			return _respawnPoints.get(0);
		else
			return _respawnPoints.get(Rnd.get(size));
	}
	
	/**
	 * Returns this town zones castle id
	 * 
	 * @return
	 */
	public final int getTaxById()
	{
		return _taxById;
	}
	
	public final boolean isPeaceZone()
	{
		return _isPeaceZone;
	}
}
