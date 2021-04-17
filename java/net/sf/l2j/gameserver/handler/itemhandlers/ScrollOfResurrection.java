package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class ScrollOfResurrection implements IItemHandler
{
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		
		L2PcInstance activeChar = (L2PcInstance) playable;
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			return;
		}
		if (activeChar.isMovementDisabled())
			return;
		
		int itemId = item.getItemId();
		//boolean blessedScroll = (itemId != 737);
		boolean petScroll = (itemId == 6387);
		
		// SoR Animation section
		L2Character target = (L2Character) activeChar.getTarget();
		
		if (target != null && target.isDead())
		{
			L2PcInstance targetPlayer = null;
			
			if (target instanceof L2PcInstance)
				targetPlayer = (L2PcInstance) target;
			
			L2PetInstance targetPet = null;
			
			if (target instanceof L2PetInstance)
				targetPet = (L2PetInstance) target;
			
			if (targetPlayer != null || targetPet != null)
			{
				//check target is not in a active siege zone
				Castle castle = null;
				
				if (targetPlayer != null)
					castle = CastleManager.getInstance().getCastle(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
				else
					castle = CastleManager.getInstance().getCastle(targetPet.getOwner().getX(), targetPet.getOwner().getY(), targetPet.getOwner().getZ());
				
				if (castle != null && castle.getSiege().getIsInProgress())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE));
					return;
				}
				
				if (targetPet != null)
				{
					if (targetPet.getOwner() != activeChar)
					{
						if (targetPet.getOwner().isReviveRequested())
						{
							if (targetPet.getOwner().isRevivingPet())
								activeChar.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
							else
								activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_RES_PET2)); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
							return;
						}
					}
				}
				else
				{
//					if (!activeChar.canResInCurrentInstance())
//					{
//						activeChar.sendMessage("Your group has already used up all "+activeChar.getCurrentInstance().getResLimit()+
//						" of your allowed ressurections in this instance");
//						return;
//					}
					if (targetPlayer.isFestivalParticipant()) // Check to see if the current player target is in a festival.
					{
						activeChar.sendMessage("You may not resurrect participants in a festival.");
						return;
					}
					else if (targetPlayer.isReviveRequested())
					{
						if (targetPlayer.isRevivingPet())
							activeChar.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
						else
							activeChar.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
						return;
					}
					else if (petScroll)
					{
						activeChar.sendMessage("You do not have the correct scroll");
						return;
					}
				}
				
				int skillId = 0;
				int skillLevel = 1;
				
				switch (itemId)
				{
					case 737:
						skillId = 2014;
						break; // Scroll of Resurrection
					case 3936:
						skillId = 2049;
						break; // Blessed Scroll of Resurrection
					case 3959:
						skillId = 2062;
						break; // L2Day - Blessed Scroll of Resurrection
					case 6387:
						skillId = 2179;
						break; // Blessed Scroll of Resurrection: For Pets
					case 9157:
						skillId = 2321;
						break; // Blessed Scroll of Resurrection Event
					case 10150:
						skillId = 2393;
						break; // Blessed Scroll of Battlefield Resurrection
					case 13259:
						skillId = 2596;
						break; // Gran Kain's Blessed Scroll of Resurrection
				}
				
				if (skillId != 0)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
					activeChar.useMagic(skill, true, true);
					
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(item);
					activeChar.sendPacket(sm);
					
					if (activeChar.isInvisible() && !activeChar.isGM())
						activeChar.stopEffects(L2EffectType.INVISIBLE);
				}
			}
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
		}
	}
}