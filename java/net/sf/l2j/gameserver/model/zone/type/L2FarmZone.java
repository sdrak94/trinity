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

import ghosts.model.Ghost;
import luna.custom.LunaVariables;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;

/**
 * Zone where 'Build Headquarters' is not allowed.
 * 
 * @author Gnat
 */
public class L2FarmZone extends L2ZoneType
{
	final public static int		S80_GRADE_ZONE_ID		= 60000;
	private static final int[]	CERTIFICATION_SKILLS	= new int[]
	{
		35200, 35202, 35204, 35206, 35208, 35210, 35212, 35214, 35216, 35218, 35220, 35222, 35224, 35226, 35228, 35230, 35232, 35234, 35236, 35238, 35240, 35242, 35244, 35246, 35248, 35250, 35252, 35254, 35256, 35258, 35260, 35262, 35264
	};
	
	public L2FarmZone(final int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			if (character.getInstanceId() == 0)
			{
				if (_id == 21000)
				{
					if (LunaVariables.getInstance().getFarmEventStatus() || character.isGM())
					{
						((L2PcInstance) character).setInFarmEvent(true);
						character.sendPacket(new ExShowScreenMessage(1, -1, 2, 0, 0, 0, 0, true, 5000, 0, "Thank you for participating on our Custom Event.\n      Good Luck!!"));
						if (character.isGM())
						{
							character.sendMessage("You have entered the Farm Event Area");
						}
					}
					else
					{
						if (!character.isGM())
						{
							character.setIsPendingRevive(true);
							character.teleToLocation(83380, 148107, -3404, true);
							character.setInsideZone(L2Character.ZONE_FARM, false);
							character.sendMessage("The event in this zone is not currently active.");
							return;
						}
					}
				}
				if (_id == S80_GRADE_ZONE_ID)
				{
					final L2PcInstance player = (L2PcInstance) character;
					if (!player.isInS80zone())
					{
						player.setIsInS80zone(true);
						if (!player.isGM())
						{
							boolean update = false;
							for (L2ItemInstance item : player.getInventory().getItems())
							{
								if (item != null && item.isEquipped())
								{
									if (item.getUniqueness() > 4)
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
//							List<Integer> certis_skills = new ArrayList<>();
							player.sendMessage("You have entered the Newbie Farm Zone (Cave of Trials), all items above tier 4 grade are not permitted here.");
//							for (int id : CERTIFICATION_SKILLS)
//							{
//								certis_skills.add(id);
//							}
//							for (L2Skill skill : player.getAllSkills())
//							{
//								if (skill != null)
//								{
//									if (certis_skills.contains(skill.getId()))
//									{
//										player.removeSkill(skill, false);
//									}
//								}
//							}
							/*
							 * for (L2Effect buff : player.getAllEffects())
							 * {
							 * if (buff == null) continue;
							 * if (buff.getSkill().isHeroSkill())
							 * buff.exit();
							 * }
							 * if (player.getClan() != null)
							 * {
							 * for(L2Skill skill: player.getClan().getAllSkills())
							 * {
							 * if (skill != null)
							 * player.removeSkill(skill, false);
							 * }
							 * }
							 * if (player.getPet() != null)
							 * {
							 * player.getPet().unSummon(player);
							 * player.sendMessage("Summons are not permitted in HV.");
							 * }
							 * if (player.getParty() != null)
							 * player.leaveParty();
							 */
							if (update)
								player.broadcastUserInfo();
						}
					}
				}
				character.setInsideZone(L2Character.ZONE_FARM, true);
				if (Config.HWID_FARMZONES_CHECK)
				{
					String hwid = ((L2PcInstance) character).getHWID();
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						if (character.isInFunEvent() || player.isInFunEvent())
						{
							continue;
						}
						if (!(player instanceof Ghost) && player.getClient().isDetached())
						{
							continue;
						}
						if (!(player.isInsideZone(L2Character.ZONE_FARM) || player.isInsideZone(L2Character.ZONE_RAID) || player.isInsideZone(L2Character.ZONE_CHAOTIC) || player.isInsideZone(L2Character.ZONE_EVENT) || player.isInHuntersVillage() || player.isInPI()))
						{
							continue;
						}
						if (player == character)
						{
							continue;
						}
						if (player.isGM() || character.isGM())
						{
							continue;
						}
						String plr_hwid = player.getHWID();
						// String plr_hwid = character.getName();
						if (plr_hwid.equalsIgnoreCase(hwid))
						{
							character.setIsPendingRevive(true);
							character.teleToLocation(83380, 148107, -3404, true);
							character.setInsideZone(L2Character.ZONE_FARM, false);
							character.sendMessage("You have another window in a hwid restricted zone.");
							break;
						}
						else
						{
							if (character.isGM())
							{
								character.sendMessage("You have entered the Farm Area");
							}
						}
					}
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						// String HWID = player.getClient().getStrixClientData().getClientHWID();
						// String HWID2 = getActingPlayer().getClient().getStrixClientData().getClientHWID();

						if (character instanceof Ghost || player instanceof Ghost)
							continue;
						String HWID = player.getHWID();
						String HWID2 = character.getActingPlayer().getHWID();
						if (HWID == null || HWID2 == null || character.getActingPlayer().getName().equalsIgnoreCase("sirvieta"))
							continue;
						if (HWID.equalsIgnoreCase(HWID2))
						{
							if (player.isInActiveFunEvent())
							{
								character.setIsPendingRevive(true);
								character.teleToLocation(83380, 148107, -3404, true);
								character.setInsideZone(L2Character.ZONE_FARM, false);
								character.sendMessage("You can't have multiple windows in Farm/Event/PvP zones.");
								break;
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			if (character.getInstanceId() == 0)
			{
				if (_id == 21000)
				{
					((L2PcInstance) character).setInFarmEvent(false);
					if (character.isGM())
					{
						character.sendMessage("You have left the Farm Event Area");
					}
				}
				else if (_id == S80_GRADE_ZONE_ID)
				{
					if (character instanceof L2PcInstance && character.getActingPlayer().isInS80zone())
					{
						character.getActingPlayer().setIsInS80zone(false);
					}
				}
				character.getActingPlayer().setInPvPCustomEventZone(false);
				character.setInsideZone(L2Character.ZONE_FARM, false);
				if (character.isGM())
				{
					character.sendMessage("You have left the Farm Area");
				}
			}
		}
	}
	
	@Override
	public void onDieInside(final L2Character character)
	{}
	
	@Override
	public void onReviveInside(final L2Character character)
	{}
}
