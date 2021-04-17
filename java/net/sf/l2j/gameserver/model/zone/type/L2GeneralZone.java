package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;

public class L2GeneralZone extends L2ZoneType
{
final public static int S_GRADE_ZONE_ID = 99999;
final public static int S80_GRADE_ZONE_ID = 11048;
private static final int HQ_ZONE = 11050;

public L2GeneralZone(final int id)
{
	super(id);
}

@SuppressWarnings("unused")
@Override
protected void onEnter(final L2Character character)
{
	if (character instanceof L2PcInstance)
	{
		final L2PcInstance player = (L2PcInstance)character;
		
		if (player == null) return;
		
		if (_id == S_GRADE_ZONE_ID)
		{
			if (!player.isInSgradeZone())
			{
				player.setIsInSgradeZone(true);
				player.sendMessage("You have entered a newbie zone (Orc Village + CoT), all items above S80 grade are not permitted here.");
				
				if (!player.isGM())
				{
					player.abortAttack();
					
					if (player.isCursedWeaponEquipped())
						CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
					
					boolean update = false;
					
					for (L2ItemInstance item : player.getInventory().getItems())
					{
						if (item != null && item.isEquipped())
						{
							if (item.isARestrictedItemOrcArea())
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
					
					/*for (L2Effect buff : player.getAllEffects())
						{
							if (buff == null) continue;
							
							if (buff.getSkill().isHeroSkill())
								buff.exit();
						}*/
					
					if (player.getClan() != null)
					{
						for(L2Skill skill: player.getClan().getAllSkills())
						{
							if (skill != null)
								player.removeSkill(skill, false);
						}
					}
					
					player.removeSkill(3611);
					player.removeSkill(3612);
					player.removeSkill(3613);
					player.removeSkill(3614);
					player.removeSkill(3615);
					player.removeSkill(3616);
					
					/*if (player.getPet() != null)
						{
							player.getPet().unSummon(player);
							player.sendMessage("Summons are not permitted in newbie zone.");
						}*/
					
					if (update)
						player.broadcastUserInfo();
				}
			}
		}
		if (_id == HQ_ZONE)
		{
			if (!player.isInHqZone())
			{
				player.setisInHqZone(true);
				{
					player.isClanLeader();
					player.sendMessage("You have entered a Headquarters Zone, you may spawn your flag here.");
				}
			}
		}
		
	}
}

@Override
protected void onExit(final L2Character character)
{
	if (_id == S_GRADE_ZONE_ID)
	{
		if (character instanceof L2PcInstance && character.getActingPlayer().isInSgradeZone())
		{
			character.getActingPlayer().setIsInSgradeZone(false);
			character.getActingPlayer().regiveTemporarySkillsSgradeZone();
		}
	}
	else if (_id == S80_GRADE_ZONE_ID)
	{
		if (character instanceof L2PcInstance && character.getActingPlayer().isInS80zone())
		{
			character.getActingPlayer().setIsInS80zone(false);
		}
	}
}

@Override
public void onDieInside(final L2Character character)
{
}

@Override
public void onReviveInside(final L2Character character)
{
}
}