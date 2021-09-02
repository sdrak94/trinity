/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestMagicSkillUse extends L2GameClientPacket
{
private static final String _C__2F_REQUESTMAGICSKILLUSE = "[C] 2F RequestMagicSkillUse";
private static Logger _log = Logger.getLogger(RequestMagicSkillUse.class.getName());

private int _magicId;
private boolean _ctrlPressed;
private boolean _shiftPressed;

@Override
protected void readImpl()
{
	_magicId      = readD();              // Identifier of the used skill
	_ctrlPressed  = readD() != 0;         // True if it's a ForceAttack : Ctrl pressed
	_shiftPressed = readC() != 0;         // True if Shift pressed
}

@Override
protected void runImpl()
{
	// Get the current L2PcInstance of the player
	L2PcInstance activeChar = getClient().getActiveChar();
	SystemMessage sm;
	
	if (activeChar == null)
		return;
	
	// Get the level of the used skill
	final int level = activeChar.getSkillLevel(_magicId);
	
	if (level <= 0)
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (activeChar.isAfraid())
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	final L2Object target = activeChar.getTarget();
	if (target instanceof L2Character)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(_magicId, level);
		final L2Character targetChar = target.getActingPlayer();
		// if (activeChar.getTarget() instanceof L2MonsterInstance &&
		// Math.abs(activeChar.getTarget().getZ() - activeChar.getZ()) >
		// 100)
		// {
		// activeChar.sendMessage("You can't attack monsters that are higher/lower height than you.");
		// activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		// activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		// return;
		// }
		if (_magicId == 30 && !activeChar.isBehindTarget())
		{
			activeChar.sendMessage("Backstab may only be used from behind your target.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (skill.getTargetType(activeChar) == SkillTargetType.TARGET_ONE || skill.getTargetType(activeChar)  == SkillTargetType.TARGET_PARTY_MEMBER)
		{
			if (skill.getSkillType() != null) switch (skill.getSkillType())
			{
				case HEAL:
				case HEAL_PERCENT:
				{
					if (targetChar instanceof L2Npc)
					{
						activeChar.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					break;
				}
				case BUFF:
				{
					if (targetChar instanceof L2Npc)
					{
						activeChar.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					if (targetChar instanceof L2Summon)
					{
						activeChar.sendMessage("Only the owner can buff that pet!");
						activeChar.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
			case AGATHION:
				break;
			case AGGDAMAGE:
				break;
			case AGGDEBUFF:
				break;
			case AGGREDUCE:
				break;
			case AGGREDUCE_CHAR:
				break;
			case AGGREMOVE:
				break;
			case BALANCE_LIFE:
				break;
			case BALLISTA:
				break;
			case BEAST_FEED:
				break;
			case BETRAY:
				break;
			case BLEED:
				break;
			case BLOW:
				break;
			case CANCEL:
				break;
			case CANCEL_DEBUFF:
				break;
			case CANCEL_STATS:
				break;
			case CHANGEWEAPON:
				break;
			case CHANGE_APPEARANCE:
				break;
			case CHARGEDAM:
				break;
			case CHARGESOUL:
				break;
			case COMBATPOINTHEAL:
				break;
			case COMMON_CRAFT:
				break;
			case CONFUSE_MOB_ONLY:
				break;
			case CONFUSION:
				break;
			case CONT:
				break;
			case COREDONE:
				break;
			case CPDAM:
				break;
			case CPDAMPERCENT:
				break;
			case CPHEAL_PERCENT:
				break;
			case CPHOT:
				break;
			case CREATE_ITEM:
				break;
			case DEATHLINK:
				break;
			case DEATHLINK_PET:
				break;
			case DEBUFF:
				break;
			case DECOY:
				break;
			case DELUXE_KEY_UNLOCK:
				break;
			case DETECTION:
				break;
			case DETECT_TRAP:
				break;
			case DETECT_WEAKNESS:
				break;
			case DISARM:
				break;
			case DOT:
				break;
			case DRAIN:
				break;
			case DRAIN_SOUL:
				break;
			case DUMMY:
				break;
			case DWARVEN_CRAFT:
				break;
			case ENCHANT_ARMOR:
				break;
			case ENCHANT_ATTRIBUTE:
				break;
			case ENCHANT_WEAPON:
				break;
			case ERASE:
				break;
			case EXTRACTABLE:
				break;
			case FAKE_DEATH:
				break;
			case FATAL:
				break;
			case FEAR:
				break;
			case FEED_PET:
				break;
			case FISHING:
				break;
			case FUSION:
				break;
			case GET_PLAYER:
				break;
			case GIVE_SP:
				break;
			case GIVE_VITALITY:
				break;
			case HARVEST:
				break;
			case HEAL_STATIC:
				break;
			case HOT:
				break;
			case INSTANT_JUMP:
				break;
			case INVISIBLE:
				break;
			case LEARN_SKILL:
				break;
			case LUCK:
				break;
			case MAGE_BANE:
				break;
			case MANADAM:
				break;
			case MANAHEAL:
				break;
			case MANAHEAL_PERCENT:
				break;
			case MANARECHARGE:
				break;
			case MANA_BY_LEVEL:
				break;
			case MDAM:
				break;
			case MDOT:
				break;
			case MHOT:
				break;
			case MOUNT:
				break;
			case MPHOT:
				break;
			case MUTE:
				break;
			case NEGATE:
				break;
			case NOTDONE:
				break;
			case PARALYZE:
				break;
			case PASSIVE:
				break;
			case PDAM:
				break;
			case PDAMPERC:
				break;
			case POISON:
				break;
			case PROC:
				break;
			case PUMPING:
				break;
			case RECALL:
				break;
			case REELING:
				break;
			case REFLECT:
				break;
			case REMOVE_TRAP:
				break;
			case RESURRECT:
				break;
			case ROOT:
				break;
			case SHIFT_TARGET:
				break;
			case SIEGEFLAG:
				break;
			case SIGNET:
				break;
			case SIGNET_CASTTIME:
				break;
			case SLEEP:
				break;
			case SLOW:
				break;
			case SOW:
				break;
			case SPOIL:
				break;
			case STEAL_BUFF:
				break;
			case STRSIEGEASSAULT:
				break;
			case STUN:
				break;
			case SUMMON:
				break;
			case SUMMON_FRIEND:
				break;
			case SUMMON_TRAP:
				break;
			case SUMMON_TREASURE_KEY:
				break;
			case SUPER_HEAL:
				break;
			case SWEEP:
				break;
			case SWITCH:
				break;
			case TAKECASTLE:
				break;
			case TAKEFORT:
				break;
			case TELEPORT:
				break;
			case TRANSFORMDISPEL:
				break;
			case UNDEAD_DEFENSE:
				break;
			case UNLOCK:
				break;
			case WARRIOR_BANE:
				break;
			case WEAKNESS:
				break;
			case WEAPON_SA:
				break;
			default:
				break;
			}
		}
		
		//if (activeChar.getInstanceWorld().handleSkill(activeChar, activeChar.getTarget(), skill))
		//{
		//	activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		//	return;
		//}
		if (activeChar.getTarget() instanceof L2NpcInstance && _ctrlPressed && skill.isOffensive())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
	}

	// Get the L2Skill template corresponding to the skillID received from the client
	final L2Skill skill = SkillTable.getInstance().getInfo(_magicId, level);

		if (skill.getSkillType() == L2SkillType.RESURRECT_PET)
			{
				if (activeChar.getPet() == null)
				{
					sm = new SystemMessage(SystemMessageId.DEBUFF_SLOT_FAILED);
					sm.addString("You don't have a summoned pet?");
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					activeChar.sendPacket(sm);
					sm =null;
					return;
				}
				if (!activeChar.getPet().isDead())
				{
					sm = new SystemMessage(SystemMessageId.DEBUFF_SLOT_FAILED);
					sm.addString("Your pet is alive fool, wtf are you tryin to do?");
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					activeChar.sendPacket(sm);
					sm =null;
					return;
				}
			}
		 if (activeChar.isDisguised() && skill.getId() == 1427)
		 {
			 sm = new SystemMessage(SystemMessageId.DEBUFF_SLOT_FAILED);
			 sm.addString("You cannot use that skill over here.");
			 activeChar.sendPacket(sm);
			 sm =null;
			 return; 
		 }
	// Check the validity of the skill
	if (skill != null && skill.getSkillType() != L2SkillType.NOTDONE)
	{
		// _log.fine("	skill:"+skill.getName() + " level:"+skill.getLevel() + " passive:"+skill.isPassive());
		// _log.fine("	range:"+skill.getCastRange()+" targettype:"+skill.getTargetType()+" optype:"+skill.getOperateType()+" power:"+skill.getPower());
		//activeChar.sendMessage("	reusedelay:"+skill.getReuseDelay()+" hittime:"+skill.getHitTime());
		//activeChar.sendMessage("	reusedelay:"+skill.getReuseDelay()+" hittime:"+Formulas.calcSkillCastTime(activeChar, skill, skill.getHitTime()));
		// _log.fine("	currentState:"+activeChar.getCurrentState());	//for debug
		
		// If Alternate rule Karma punishment is set to true, forbid skill Return to player with Karma
		if (skill.getSkillType() == L2SkillType.RECALL && !Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// players mounted on pets cannot use any toggle skills
		if (skill.isToggle() && activeChar.isMounted())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		boolean allow = true;
		
		// activeChar.stopMove();
		if (!allow)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else
			activeChar.useMagic(skill, _ctrlPressed, _shiftPressed);
	}
	else
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
}

@Override
public String getType()
{
	return _C__2F_REQUESTMAGICSKILLUSE;
}

@Override
protected boolean triggersOnActionRequest()
{
	return true;
}
}