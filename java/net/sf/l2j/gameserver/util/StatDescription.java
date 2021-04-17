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
package net.sf.l2j.gameserver.util;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.datatables.ArmorSetsTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.ChanceCondition;
import net.sf.l2j.gameserver.model.ChanceCondition.TriggerType;
import net.sf.l2j.gameserver.model.Elementals;
import net.sf.l2j.gameserver.model.L2ArmorSet;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.skills.conditions.ConditionGameChance;
import net.sf.l2j.gameserver.skills.conditions.ConditionGameTime;
import net.sf.l2j.gameserver.skills.conditions.ConditionGameTime.CheckGameTime;
import net.sf.l2j.gameserver.skills.conditions.ConditionLogicAnd;
import net.sf.l2j.gameserver.skills.conditions.ConditionLogicOr;
import net.sf.l2j.gameserver.skills.conditions.ConditionMinDistance;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerActiveEffectId;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerActiveSkillId;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerClassIdRestriction;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerCp;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerHp;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerIsClanLeader;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerLevel;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerMp;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerRace;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerSex;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerState;
import net.sf.l2j.gameserver.skills.conditions.ConditionSlotItemId;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetAbnormal;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetActiveEffectId;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetActiveSkillId;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetAggro;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetClassIdRestriction;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetLevel;
import net.sf.l2j.gameserver.skills.conditions.ConditionTargetRace;
import net.sf.l2j.gameserver.skills.conditions.ConditionUsingSkill;
import net.sf.l2j.gameserver.skills.funcs.FuncEnchant;
import net.sf.l2j.gameserver.skills.funcs.FuncTemplate;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.item.L2Armor;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * @author Lukas
 *
 */
public class StatDescription
{
	public static String getItemName(int itemId)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		if(item == null)
			return null;
		
		return item.getName();
	}
	
	public static String getItemColor(L2Item item)
	{
//		if(grade == null) return "EAEAEA";
//		if(grade.equalsIgnoreCase("default"))
//		{
//			return "EAEAEA";
//		}
//		else if(grade.equalsIgnoreCase("tier1"))
//		{
//			return "A7A358";
//		}
//		else if(grade.equalsIgnoreCase("tier1.5"))
//		{
//			return "A5825A";
//		}
//		else if(grade.equalsIgnoreCase("tier2"))
//		{
//			return "8EC19A";
//		}
//		else if(grade.equalsIgnoreCase("tier2.5"))
//		{
//			return "8CBBC4";
//		}
//		else if(grade.equalsIgnoreCase("epicjewels"))
//		{
//			return "5B5EA4";
//		}
//		else if(grade.equalsIgnoreCase("rare"))
//		{
//			return "A95656";
//		}
		
		return "9f9f9f";
	}
	
//	private static Color brighterColor(Color c, final double factor)
//	{
//		int r = c.getRed();
//        int g = c.getGreen();
//        int b = c.getBlue();
//        int alpha = c.getAlpha();
//
//        /* From 2D group:
//         * 1. black.brighter() should return grey
//         * 2. applying brighter to blue will always return blue, brighter
//         * 3. non pure color (non zero rgb) will eventually return white
//         */
//        int i = (int)(1.0/(1.0-factor));
//        if ( r == 0 && g == 0 && b == 0) {
//            return new Color(i, i, i, alpha);
//        }
//        if ( r > 0 && r < i ) r = i;
//        if ( g > 0 && g < i ) g = i;
//        if ( b > 0 && b < i ) b = i;
//
//        return new Color(Math.min((int)(r/factor), 255),
//                         Math.min((int)(g/factor), 255),
//                         Math.min((int)(b/factor), 255),
//                         alpha);
//	}
	
	
	public static String getEnchantColor(int level, L2Item item)
	{
//		if(grade == null || grade.equals("default"))
		{
			if(level <= 3)
				return "7f7f7f";
			
			switch(level)
			{
				case 1:
					return "7f7f7f";
				case 2:
					return "7f7f7f";
				case 3:
					return "7f7f7f";
				case 4:
					return "74778B";
				case 5:
					return "6A6B95";
				case 6:
					return "669799";
				case 7:
					return "5A9FA5";
				case 8:
					return "50AFAD";
				case 9:
					return "4DB371";
				case 10:
					return "41BE69";
				case 11:
					return "7CC43C";
				case 12:
					return "C7CB34";
				case 13:
					return "CFA830";
				case 14:
					return "D37B2C";
				case 15:
					return "B12CD3";
				case 16:
					return "EC68E9";
				case 17:
					return "F55C61";
				case 18:
					return "F33A3F";
				case 19:
					return "E7161C";
				case 20:
					return "A01013";
				case 21:
					return "1B09FF";
				case 22:
					return "045300";
				case 23:
					return "533F00";
				case 24:
					return "4A0053";
				case 25:
					return "530002";
				default:
					return "530002";
			}
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	public static String getSkillType(L2SkillType type)
	{
		switch(type)
		{
			case PDAM:
			case MDAM:
			case CPDAM:
			case MANADAM:
			case CPDAMPERCENT:
			case DOT:
			case MDOT:
			case DRAIN_SOUL:
			case DRAIN:
			case DEATHLINK:
			case FATAL:
			case BLOW:
			case SIGNET:
			case SIGNET_CASTTIME:
			case BLEED:
			case POISON:
			case STUN:
			case ROOT:
			case CONFUSION:
			case FEAR:
			case SLEEP:
			case CONFUSE_MOB_ONLY:
			case MUTE:
			case PARALYZE:
			case DISARM:
			case HEAL:
			case HOT:
			case BALANCE_LIFE:
			case HEAL_PERCENT:
			case HEAL_STATIC:
			case COMBATPOINTHEAL:
			case CPHEAL_PERCENT:
			case CPHOT:
			case MANAHEAL:
			case MANA_BY_LEVEL:
			case MANAHEAL_PERCENT:
			case MANARECHARGE:
			case MPHOT:
//			case HPMPCPHEAL_PERCENT:
//			case HPMPHEAL_PERCENT:
//			case HPCPHEAL_PERCENT:
			case GIVE_SP:
//			case GIVE_RECO:
			case GIVE_VITALITY:
			case AGGDAMAGE:
			case AGGREDUCE:
			case AGGREMOVE:
			case AGGREDUCE_CHAR:
			case AGGDEBUFF:
			case FISHING:
			case PUMPING:
			case REELING:
			case UNLOCK:
//			case UNLOCK_SPECIAL:
			case ENCHANT_ARMOR:
			case ENCHANT_WEAPON:
			case ENCHANT_ATTRIBUTE:
//			case SOULSHOT:
//			case SPIRITSHOT:
			case SIEGEFLAG:
			case TAKECASTLE:
			case TAKEFORT:
			case WEAPON_SA:
			case DELUXE_KEY_UNLOCK:
			case SOW:
			case HARVEST:
			case GET_PLAYER:
			case AGATHION:
			case MOUNT:
			case INSTANT_JUMP:
			case DETECTION:
			case DUMMY:
			case COMMON_CRAFT:
			case DWARVEN_CRAFT:
			case CREATE_ITEM:
			case EXTRACTABLE:
//			case EXTRACTABLE_FISH:
			case LEARN_SKILL:
			case SUMMON:
			case FEED_PET:
			case DEATHLINK_PET:
			case STRSIEGEASSAULT:
			case ERASE:
			case BETRAY:
			case DECOY:
//			case SPAWN:
			case CANCEL:
			case CANCEL_STATS:
			case CANCEL_DEBUFF:
			case NEGATE:
			case BUFF:
			case DEBUFF:
			case PASSIVE:
			case CONT:
			case FUSION:
			case RESURRECT:
			case CHARGEDAM:
			case MHOT:
			case DETECT_WEAKNESS:
			case RECALL:
			case TELEPORT:
			case SUMMON_FRIEND:
			case SPOIL:
			case SWEEP:
			case FAKE_DEATH:
			case UNDEAD_DEFENSE:
			case BEAST_FEED:
//			case BEAST_RELEASE:
//			case BEAST_RELEASE_ALL:
//			case BEAST_SKILL:
//			case BEAST_ACCOMPANY:
			case CHARGESOUL:
			case TRANSFORMDISPEL:
			case SUMMON_TRAP:
			case DETECT_TRAP:
			case REMOVE_TRAP:
			case SHIFT_TARGET:
			case CHANGEWEAPON:
			case STEAL_BUFF:
			case COREDONE:
			case CHANGE_APPEARANCE:
//			case REFUEL:
//			case NORNILS_POWER:
				return type.toString();
		}
		
		return type.toString();
	}
	
	@SuppressWarnings("incomplete-switch")
	public static String getPower(L2PcInstance activeChar, L2SkillType type, double power)
	{
		if(type == null || power <= 0)
			return null;
		
		switch(type)
		{
			case COMBATPOINTHEAL:
			case CPDAM:
				return new String((int) power + " CP");
				
			case MANAHEAL:
			case MANARECHARGE:
			case MANADAM:
				return new String((int) power + " MP");
				
			case HEAL:
			case HEAL_STATIC:
				return new String((int) power + " HP");
			
			case FAKE_DEATH:
			case RECALL:
			case AGATHION:
//			case REFUEL:
			case REELING:
			case RESURRECT:
			case PUMPING:
			case PASSIVE:
			case UNLOCK:
//			case UNLOCK_SPECIAL:
			case CHANGE_APPEARANCE:
			case CHANGEWEAPON:
			case TELEPORT:
			case TRANSFORMDISPEL:
			case TAKECASTLE:
			case TAKEFORT:
			case COREDONE:
			case COMMON_CRAFT:
			case CREATE_ITEM:
			case DETECT_WEAKNESS:
			case DUMMY:
			case DWARVEN_CRAFT:
			case ENCHANT_ARMOR:
			case ENCHANT_ATTRIBUTE:
			case ENCHANT_WEAPON:
			case EXTRACTABLE:
//			case EXTRACTABLE_FISH:
			case FISHING:
//			case SOULSHOT:
//			case SPIRITSHOT:
			case SIEGEFLAG:
			case WEAPON_SA:
			case DELUXE_KEY_UNLOCK:
			case SOW:
			case LEARN_SKILL:
			case SUMMON:
			case FEED_PET:
			case SUMMON_FRIEND:
			case SWEEP:
			case HARVEST:
			case GET_PLAYER:
			case CANCEL_DEBUFF:
			case MOUNT:
			case GIVE_VITALITY:
			case BALLISTA:
				return null;
				
			case STEAL_BUFF:
				return new String((int) power + " buffs");
				
			case STUN:
			case CANCEL:
			case ROOT:
			case SLEEP:
			case CONFUSION:
			case PARALYZE:
			case AGGDAMAGE:
			case AGGREDUCE:
			case AGGREDUCE_CHAR:
			case AGGREMOVE:
			case MUTE:
			case CONFUSE_MOB_ONLY:
			case NEGATE:
			case ERASE:
			case BETRAY:
			case DEBUFF:
			case DISARM:
			case HEAL_PERCENT:
			case CPHEAL_PERCENT:
			case MANAHEAL_PERCENT:
//			case HPCPHEAL_PERCENT:
//			case HPMPHEAL_PERCENT:
//			case HPMPCPHEAL_PERCENT:
				if((int) power == 400)
					return new String((int)power/4 + "%");
				
				return new String((int)power + "%");
			
			case PDAM:
			case MDAM:
			case CPDAMPERCENT:
			case DOT:
			case MDOT:
			case DRAIN_SOUL:
			case DRAIN:
			case DEATHLINK:
			case FATAL:
			case BLOW:
			case SIGNET:
			case SIGNET_CASTTIME:
			case BLEED:
			case POISON:
			case FEAR:
			case HOT:
			case BALANCE_LIFE:
			case CPHOT:
			case MANA_BY_LEVEL:
			case MPHOT:
			case GIVE_SP:
//			case GIVE_RECO:
			case AGGDEBUFF:
			case INSTANT_JUMP:
			case DETECTION:
			case DEATHLINK_PET:
			case STRSIEGEASSAULT:
			case DECOY:
//			case SPAWN:
			case CANCEL_STATS:
			case BUFF:
			case CONT:
			case FUSION:
			case CHARGEDAM:
			case MHOT:
			case SPOIL:
			case UNDEAD_DEFENSE:
			case BEAST_FEED:
//			case BEAST_RELEASE:
//			case BEAST_RELEASE_ALL:
//			case BEAST_SKILL:
//			case BEAST_ACCOMPANY:
			case CHARGESOUL:
			case SUMMON_TRAP:
			case DETECT_TRAP:
			case REMOVE_TRAP:
			case SHIFT_TARGET:
//			case NORNILS_POWER:
				return String.valueOf((int)power);
		}
		return String.valueOf((int)power);
	}
	
	@SuppressWarnings("incomplete-switch")
	public static String getSkillDescription(L2PcInstance activeChar, L2SkillType type)
	{
		if(type == null)
			return null;
		
		switch(type)
		{
			case COMBATPOINTHEAL:
				return "Regenerates CP.";
				
			case CPDAM:
				return "Deals damage to CP.";
				
			case MANAHEAL:
				return "Recharges MP.";
			case MANARECHARGE:
				return "Recharges MP.";
			case MANADAM:
				return "Reduces enemy MP.";
				
			case HEAL:
				return "Restores HP.";
			case HEAL_STATIC:
				return "Restores a static ammount of HP.";
			
			case FAKE_DEATH:
				return "You pretend to be dead to avoid mobs from hitting you.";
			case RECALL:
				return "";
			case AGATHION:
				return "Summons something very ugly.";
//			case REFUEL:
//				return null;
			case REELING:
				return null;
			case RESURRECT:
				return "Resurrects a dead target.";
			case PUMPING:
				return null;
			case PASSIVE:
				return null;
			case UNLOCK:
				return null;
//			case UNLOCK_SPECIAL:
//				return null;
			case CHANGE_APPEARANCE:
				return null;
			case CHANGEWEAPON:
				return "Changes your current weapon into its alternative.";
			case TELEPORT:
				return null;
			case TRANSFORMDISPEL:
				return "Transforms you back to your previous state.";
			case TAKECASTLE:
				return "Conquert the castle.";
			case TAKEFORT:
				return "Conquers the fortress";
			case COREDONE:
				return null;
			case COMMON_CRAFT:
				return null;
			case CREATE_ITEM:
				return null;
			case DETECT_WEAKNESS:
				return null;
			case DUMMY:
				return null;
			case DWARVEN_CRAFT:
				return null;
			case ENCHANT_ARMOR:
				return null;
			case ENCHANT_ATTRIBUTE:
				return null;
			case ENCHANT_WEAPON:
				return null;
			case EXTRACTABLE:
				return null;
//			case EXTRACTABLE_FISH:
//				return null;
			case FISHING:
				return null;
//			case SOULSHOT:
//				return null;
//			case SPIRITSHOT:
//				return null;
			case SIEGEFLAG:
				return null;
			case WEAPON_SA:
				return null;
			case DELUXE_KEY_UNLOCK:
				return null;
			case SOW:
				return null;
			case LEARN_SKILL:
				return null;
			case SUMMON:
				return "Summons a creature.";
			case FEED_PET:
				return null;
			case SUMMON_FRIEND:
				return "Teleports friendly player to you.";
			case SWEEP:
				return "Removes extra rewards from spoiled creature.";
			case HARVEST:
				return null;
			case GET_PLAYER:
				return "Teleports your target to yourself.";
			case CANCEL_DEBUFF:
				return "Cancels certain debuffs.";
			case MOUNT:
				return null;
			case GIVE_VITALITY:
				return null;
			case BALLISTA:
				return null;
				
			case STEAL_BUFF:
				return "Steals buffs from enemy.";
				
			case STUN:
				return "Makes target unable to move or attack.";
			case CANCEL:
				return "Temporarily removes buffs from target.";
			case ROOT:
				return "Makes your target unable to move.";
			case SLEEP:
				return "Puts your target asleep. He'll wake-up when hit.";
			case CONFUSION:
				return "";
			case PARALYZE:
				return "Completelly anchors your target.";
			case AGGDAMAGE:
				return "Adds aggression to monster.";
			case AGGREDUCE:
				return "Reduces monster's aggression";
			case AGGREDUCE_CHAR:
				return "Reduces aggression (removes target).";
			case AGGREMOVE:
				return "Removes aggression from monster.";
			case MUTE:
				return "Makes enemy unable to cast skills.";
			case CONFUSE_MOB_ONLY:
				return "Makes a mob attack random target.";
			case NEGATE:
				return "Cures negative effects.";
				
			case ERASE:
				return "Deletes a summon.";
			case BETRAY:
				return "Makes a summon attack his master.";
			case DEBUFF:
				return "Gives target negative stats.";
			case DISARM:
				return "Disarms enemy's weapon from his hands.";
			case HEAL_PERCENT:
				return "Heals for % of HP based on skill's power.";
			case CPHEAL_PERCENT:
				return "Heals for % of CP based on skill's power.";
			case MANAHEAL_PERCENT:
				return "Heals for % of MP based on skill's power.";
//			case HPCPHEAL_PERCENT:
//				return "Heals for % of HP/CP.";
//			case HPMPHEAL_PERCENT:
//				return "Heals for % of HP/MP.";
//			case HPMPCPHEAL_PERCENT:
//				return "Heals for % of CP/HP/MP.";
				
			case AGGDEBUFF:
				return "";
			
			case PDAM:
				return "Deals physical damage.";
			case MDAM:
				return "Deals magical damage.";
			case CPDAMPERCENT:
				return "Removes % of target's CP.";
			case DOT:
				return "Deals continous damage.";
			case MDOT:
				return "Deals magic continous damage.";
			case DRAIN_SOUL:
				return "Drains souls.";
			case DRAIN:
				return "Drains HP from damage.";
			case DEATHLINK:
				return "Deals more damage if your target has low HP.";
			case FATAL:
				return "";
			case BLOW:
				return "Deals strong damage.";
			case SIGNET:
				return null;
			case SIGNET_CASTTIME:
				return null;
			case BLEED:
				return "Physical damage-over-time.";
			case POISON:
				return "'Magical' damage-over-time.";
			case FEAR:
				return "Makes enemy flee from you.";
			case HOT:
				return "Heals you continously.";
			case BALANCE_LIFE:
				return "Equalizes HP of party members.";
			case CPHOT:
				return "Continously heals CP.";
			case MANA_BY_LEVEL:
				return "Mana heal.";
			case MPHOT:
				return "Continously heals MP.";
			case GIVE_SP:
//			case GIVE_RECO:
//				return null;
			case INSTANT_JUMP:
				return "Teleports you to your target.";
			case DETECTION:
				return "Detects nearby invisible players.";
			case DEATHLINK_PET:
				return null;
			case STRSIEGEASSAULT:
				return null;
			case DECOY:
				return "Creates your exact decoy, to bluff enemy.";
//			case SPAWN:
//				return null;
			case CANCEL_STATS:
				return null;
			case BUFF:
				return "Gives positive effects.";
			case CONT:
				return "";
			case FUSION:
				return "Charges your target with energy.";
			case CHARGEDAM:
				return "Deals damage based on charges.";
			case MHOT:
				return null;
			case SPOIL:
				return "Spoils target for extra drop rewards.";
			case UNDEAD_DEFENSE:
				return null;
			case BEAST_FEED:
				return null;
//			case BEAST_RELEASE:
//				return null;
//			case BEAST_RELEASE_ALL:
//				return null;
//			case BEAST_SKILL:
//				return null;
//			case BEAST_ACCOMPANY:
//				return null;
			case CHARGESOUL:
				return "Charges yourself with souls.";
			case SUMMON_TRAP:
				return "Summons a trap.";
			case DETECT_TRAP:
				return "Detects nearby traps.";
			case REMOVE_TRAP:
				return "Removes a trap.";
			case SHIFT_TARGET:
				return "Transfers mob's aggression to nearby players.";
//			case NORNILS_POWER:
//				return null;
		}
		return null;
	}
	
	@SuppressWarnings("incomplete-switch")
	public static String getTargetType(L2PcInstance activeChar, SkillTargetType type)
	{
		switch(type)
		{
			case TARGET_NONE:
				return "No target";
			case TARGET_SELF:
				return "Selfbuff";
			case TARGET_ONE:
				return "Single target";
			case TARGET_PARTY:
				return "Targets party members";
			case TARGET_ALLY:
				return "Targets ally members";
			case TARGET_CLAN:
				return "Targets clan members";
			case TARGET_PET:
				return "Targets a pet";
			case TARGET_SUMMON:
				return "Targets a summon";
			case TARGET_AREA:
				return "Targets players around your target";
			case TARGET_FRONT_AREA:
				return "Targets players in front of your target";
			case TARGET_BEHIND_AREA:
				return "Targets players behind your target";
			case TARGET_AURA:
				return "Targets all around you";
			case TARGET_FRONT_AURA:
				return "Targets all in front of you";
			case TARGET_BEHIND_AURA:
				return "Targets all behind you";
			case TARGET_CORPSE:
				return "Targets a corpse";
			case TARGET_UNDEAD:
				return "Targets only undeads";
			case TARGET_AREA_UNDEAD:
				return "Targets undeads around your target";
			case TARGET_CORPSE_ALLY:
				return "Targets dead ally members";
			case TARGET_CORPSE_CLAN:
				return "Targets dead clan members";
			case TARGET_CORPSE_PLAYER:
				return "Targets a dead player";
			case TARGET_CORPSE_PET:
				return "Targets dead pet";
			case TARGET_AREA_CORPSE_MOB:
				return "Targets dead mobs around your target";
			case TARGET_CORPSE_MOB:
				return "Targets a dead monster";
			case TARGET_UNLOCKABLE:
				return "Targets a chest";
			case TARGET_HOLY:
				return "Targets holy";
			case TARGET_FLAGPOLE:
				return "Targets a flag";
			case TARGET_PARTY_MEMBER:
				return "Targets a party member";
			case TARGET_PARTY_OTHER:
				return "Targets a party member";
//			case TARGET_PARTY_CLAN:
//				return "Targets your clan and party members";
			case TARGET_ENEMY_SUMMON:
				return "Targets enemy summon";
			case TARGET_OWNER_PET:
				return "Targets your pet";
			case TARGET_GROUND:
				return "Dynamic";
//			case TARGET_PARTY_NOTME:
//				return "Targets all party members except you";
//			case TARGET_AREA_SUMMON:
//				return "Targets all around your summon";
//			case TARGET_CLAN_MEMBER:
//				return "Targets a clan member";
		}
		return "Unknown";
	}
	
	@SuppressWarnings("unused")
	public static void showPage(L2PcInstance activeChar, int skillId, int level, int referencedFrom, int prevSkillLvl)
	{
		Locale.setDefault(Locale.US);
		NpcHtmlMessage msg = new NpcHtmlMessage(0);
		
		TextBuilder tb = new TextBuilder();
		
		if(level == 0)
			level = 999;
		
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);
		if(skill == null)
			return;
		
		tb.append("<html><title>" + skill.getName() + ",  Lv" + skill.getLevel() + " skill info</title><body>");
		
		if(referencedFrom > 0)
		{
			if(prevSkillLvl > 0)
			{
				L2Skill prev = SkillTable.getInstance().getInfo(referencedFrom, prevSkillLvl);
				if(prev == null)
					return;
				
				tb.append("<table bgcolor=484848 width=280><tr><td width=220 align=left><font color=806957>(triggered by " + prev.getName() + ")</font></td><td width=60 align=right><button action=\"bypass -h skillinfo " + referencedFrom + " " + prevSkillLvl + "\" value=\"Back\" width=70 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
			}
			else
			{
				L2Item item = ItemTable.getInstance().getTemplate(referencedFrom);
				if(item == null)
					return;
				
				tb.append("<table bgcolor=484848 width=280><tr><td width=220 align=left><font color=806957>(item skill of " + item.getName() + ")</font></td><td width=60 align=right><button action=\"bypass -h iteminfo " + referencedFrom + "\" value=\"Back\" width=70 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
			}
		}
		
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
		tb.append("<table bgcolor=2f2f2f>");
		tb.append("<tr><td align=left width=100><font color=9f9f9f>Skill name:</font></td><td align=left width=170><font color=LEVEL>" + skill.getName() + "</font></td></tr>");
		
		if(activeChar.isGM())
			tb.append("<tr><td align=left width=100><font color=6f6f6f>Skill ID:</font></td><td align=left width=170><font color=6f6f6f>" + skill.getId() + "</font></td></tr>");	
		
		String opType = null;
		
		if(skill.isPassive())
			opType = "Passive";
		else if(skill.isToggle())
			opType = "Toggle";
		else
			opType = "Active";
		
		tb.append("<tr><td align=left width=100><font color=9f9f9f>Skill type:</font></td><td align=left width=170><font color=ac9887>" + getSkillType(skill.getSkillType()).toString().substring(0, 1) + getSkillType(skill.getSkillType()).toString().substring(1).toLowerCase() + ",</font><font color=5f5f5f> " + opType + "</font></td></tr>");
		
		tb.append("</table>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		
		
		tb.append("<br>");
		
		String skillDesc = getSkillDescription(activeChar, skill.getSkillType());
		if(skillDesc != null)
		{
			tb.append("<table width=280 bgcolor=3A3C22><tr><td align=center width=170><font color=989A65>" + skillDesc + "</font></td></tr></table>");
			
			tb.append("<br>");
		}
		
//		if(skill.getPreConditions() != null)
//		{
//			for(Condition c : skill.getPreConditions())
//			{
//				if(c instanceof ConditionPlayerState && ((ConditionPlayerState)c)._check == PlayerState.OLYMPIAD)
//					continue;
//				
//				tb.append("<table width=280 bgcolor=382525><tr><td align=center width=170><font color=ac9887>can be used " + getCondDescription(c) + "</font></td></tr></table>");
//				
//				tb.append("<br>");
//			}
//		}
		
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
		tb.append("<table width=280 bgcolor=2f2f2f>");
		
		DecimalFormat twoDecFormat = new DecimalFormat("0.0#");
		DecimalFormat oneDecFormat = new DecimalFormat("#.#");
		
		String targetType = getTargetType(activeChar, skill.getTargetType(activeChar));
		
		tb.append("<tr><td align=left width=100><font color=7f7f7f>Target type:</font></td><td align=left width=170><font color=806957>" + targetType + "</font></td></tr>");
		
		int reuse = skill.getReuseDelay(activeChar);
		
		if(!skill.isStaticReuse())
		{
			if(skill.isMagic())
				reuse = (int)(reuse * activeChar.getStat().getMReuseRate(skill, null));
			else
				reuse = (int)(reuse * activeChar.getStat().getPReuseRate(skill, null));
		}
		
		int hitTime = Formulas.calcPAtkSpd(activeChar, null, skill.getHitTime());
		if(skill.isMagic())
			hitTime = (int)(0.70 * hitTime);
		
		if(skill.isStaticHitTime())
			hitTime = skill.getHitTime();
		
		if(hitTime > 0)
			tb.append("<tr><td align=left width=100><font color=9f9f9f>Hit time:</font></td><td align=left width=170><font color=ac9887>" + twoDecFormat.format((double)hitTime/1000) + " sec</font><font color=5f5f5f> &nbsp;&nbsp; (default " + twoDecFormat.format((double)skill.getHitTime()/1000) + " sec)</font></td></tr>");
		if(reuse > 0)
			tb.append("<tr><td align=left width=100><font color=9f9f9f>Reuse delay:</font></td><td align=left width=170><font color=ac9887>" + twoDecFormat.format((double)reuse/1000) + " sec</font><font color=5f5f5f> &nbsp;&nbsp; (default " + twoDecFormat.format((double)skill.getReuseDelay()/1000) + " sec)</font></td></tr>");
		
		int baseCrit = skill.getBaseCritRate();
		
		if(baseCrit > 0)
			tb.append("<tr><td align=left width=100><font color=9f9f9f>Crit. rate:</font></td><td align=left width=170><font color=ac9887>" + baseCrit + "%</font></td></tr>");
		
		String weaponsAllowed = "";//skill.getWeaponsAllowedString();
		
		String skillPower = (getPower(activeChar, skill.getSkillType(), skill.getPower(activeChar)));
		
		if(skillPower != null)
			tb.append("<tr><td align=left width=100><font color=9f9f9f>Base power:</font></td><td align=left width=170><font color=B8831F>" + skillPower + "</font></td></tr>");
		
		int castRange = skill.getCastRange(activeChar);
		
		if(castRange > 0)
			tb.append("<tr><td align=left width=100><font color=9f9f9f>Cast range:</font></td><td align=left width=170><font color=ac9887>" + castRange + "</font></td></tr>");
		
		int radius = skill.getSkillRadius(activeChar);
		
		if(radius > 80)
			tb.append("<tr><td align=left width=100><font color=9f9f9f>Skill radius:</font></td><td align=left width=170><font color=ac9887>" + radius + "</font></td></tr>");
		
		
		if(skill.getElement() > -1 && skill.getElementPower() > 0)
		{
			String attr = "None";
			switch(skill.getElement())
			{
				case Elementals.FIRE:
					attr = "<font color=DE4A0C>Fire</font>";
					break;
				case Elementals.DARK:
					attr = "<font color=6B6B6B>Dark</font>";
					break;
				case Elementals.EARTH:
					attr = "<font color=12AF1E>Earth</font>";
					break;
				case Elementals.HOLY:
					attr = "<font color=FCFDC1>Holy</font>";
					break;
				case Elementals.WATER:
					attr = "<font color=3483AD>Water</font>";
					break;
				case Elementals.WIND:
					attr = "<font color=37CEAC>Wind</font>";
					break;
			}
			
			attr += "  (" + skill.getElementPower() + ")";
			tb.append("<tr><td align=left width=100><font color=9f9f9f><font color=9f9f9f>Element:</font></font></td><td align=left width=170>" + attr + "</td></tr>");
		}
		
		if(weaponsAllowed != null && weaponsAllowed.length() > 0)
			tb.append("<tr><td align=left width=100><font color=7f7f7f>Allowed with:</font></td><td align=left width=170><font color=806957>" + weaponsAllowed + "</font></td></tr>");
		
		//int drainAmmount = skill.get
		
	
		tb.append("</table>");
		
//		if(skill.isRemovedOnDamage())
//			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=left><font color=807C57>* Skill is cancelled if you receive damage.</font></td></tr></table>");
//		
//		if(skill.isRemovedOnAnyActionExceptMove())
//			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=left><font color=807C57>* Skill is cancelled if you cast or hit.</font></td></tr></table>");
		
		if(skill.ignoreResists())
			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=left><font color=807C57>* Skill ignores target's resists.</font></td></tr></table>");
		
		if(skill.ignoreShield())
			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=left><font color=807C57>* Skill ignores target's shield.</font></td></tr></table>");
		
//		if(skill.getNegateAbnormals() != null)
//		{
//			List<String> used = new FastList<>();
//			TextBuilder abnormals = new TextBuilder();
//			for(String s : skill.getNegateAbnormals().keySet())
//			{
//				if(used.contains(s))
//					continue;
//				
//				used.add(s);
//				abnormals.append(s + ", ");
//			}
//			
//			String result = abnormals.toString();
//			if(result.length() > 0)
//			{
//				tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=left><font color=807C57>* Negates " + result.substring(0, result.length() - 2) + ".</font></td></tr></table>");
//			}
//		}
		
		if(skill.isStaticPower())
			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=left><font color=807C57>* The power is static, P./M.atk doesn't affect it.</font></td></tr></table>");
		
		int numCharges = skill.getNumCharges();
		if(numCharges > 0)
			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=left><font color=807C57>* Requires you to be charged on lvl" + numCharges + ".</font></td></tr></table>");
		
		int numSouls = skill.getNumSouls();
		if(numSouls > 0)
			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=left><font color=807C57>* Absorbs up to " + numSouls + " souls from target.</font></td></tr></table>");
		
		int soulConsume = skill.getSoulConsumeCount();
		int maxSoulConsume = skill.getMaxSoulConsumeCount(activeChar);
		if(numSouls > 0)
			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=left><font color=807C57>* Consumes max " + Math.max(maxSoulConsume, soulConsume) + " souls to increase power.</font></td></tr></table>");
		
		if(skill.getDmgDirectlyToHP())
			tb.append("<table bgcolor=2f2f2f width=280><tr><td width=280 align=left><font color=807C57>* Damage goes directly to HP (ignores CP).</font></td></tr></table>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		
		Map<String, List<FuncTemplate>> stats = new FastMap<String, List<FuncTemplate>>();
		
		if(skill.getFuncTemplates() != null)
		{
			for(FuncTemplate f : skill.getFuncTemplates())
			{
				if(f.stat != null)
				{
					boolean hasCondition = false;
					if(f.applayCond != null)
					{
						try
						{
							String condDesc = getCondDescription(f.applayCond);
							if(condDesc != null)
							{
								if(!stats.containsKey("cond_" + condDesc))
									stats.put("cond_" + condDesc, new FastList<FuncTemplate>());
								stats.get("cond_" + condDesc).add(f);
								
								hasCondition = true;
							}
						}
						catch (Exception e2)
						{
							e2.printStackTrace();
						}
					}
					
					if(!hasCondition)
					{
						if(!stats.containsKey("stats"))
							stats.put("stats", new FastList<FuncTemplate>());
						stats.get("stats").add(f);
					}
				}
			}
		}
		
		for(Entry<String, List<FuncTemplate>> e : stats.entrySet())
		{
			if(!e.getKey().equals("stats"))
				continue;
			
			if(e.getKey().equals("stats"))
				tb.append("<br><font color=LEVEL>&nbsp;[Stats]:</font>");
			else
				tb.append("<br><font color=LEVEL>&nbsp;" + e.getKey() + "</font>");
			
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<table width=280 bgcolor=2f2f2f>");
			
			for(FuncTemplate f : e.getValue())
			{
				if(f.pFunc.equals("Enchant"))
					continue;
				
				Stats stat = f.stat;
				String desc = stat.getHtmlDesc();
				
				OperationType t = OperationType.getType(f.pFunc);
				
				String power = getPower(f, false);
				if(power == null)
					continue;
				
				if(f.pFunc.equals("Set"))
					tb.append("<tr><td align=left width=200><font color=" + getColorForOperation(t, power, stat) + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
				else
					tb.append("<tr><td align=left width=200><font color=" + stat.getColor().code + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
			}
			
			tb.append("</table>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		}
		
		for(Entry<String, List<FuncTemplate>> e : stats.entrySet())
		{
			if(!e.getKey().startsWith("cond_"))
				continue;
			
			String condName = e.getKey().substring(5);
			
			tb.append("<br><table width=280><tr><td width=280><font color=FF8040>[" + condName + "]:</font></td></tr></table>");
			
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<table width=280 bgcolor=2f2f2f>");
			
			for(FuncTemplate f : e.getValue())
			{
				if(f.pFunc.equals("Enchant"))
					continue;
				
				Stats stat = f.stat;
				String desc = stat.getHtmlDesc();
				
				OperationType t = OperationType.getType(f.pFunc);
				
				String power = getPower(f, false);
				if(power == null)
					continue;
				
				if(f.pFunc.equals("Set"))
					tb.append("<tr><td align=left width=200><font color=" + getColorForOperation(t, power, stat) + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
				else
					tb.append("<tr><td align=left width=200><font color=" + stat.getColor().code + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
			}
			
			tb.append("</table>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		}

		if(skill.hasSelfEffects() || skill.getEffectTemplates() != null)
		{
			tb.append("<br><br><font color=LEVEL>&nbsp;Following effects are provided by the skill:</font>");
		}
		
		if(skill.hasSelfEffects())
		{
			for(EffectTemplate et : skill.getSelfEffectTemplates())
			{
				String effectName = et.funcName;
				
				L2SkillType type = et.effectType;
				String effectPower = getPower(activeChar, type, et.effectPower);
				int duration = et.period * Math.max(et.counter, 1);
				
				String effectDescription = ("<br1><table width=280><tr><td width=280><font color=FF8040>[" + effectName + "]</font><font color=6f6f6f>&nbsp;&nbsp;affects caster |</font>");
				
				if(effectPower != null && et.effectPower != 400)
					effectDescription = effectDescription + ("<font color=6f6f6f>&nbsp;&nbsp;power " + effectPower + " |</font>");
				if(duration > 0 && !skill.isToggle())
					effectDescription = effectDescription + ("<font color=6f6f6f>&nbsp;&nbsp;lasts " + duration + "s |</font>");
				
				if(effectDescription.endsWith(" |</font>"))
					effectDescription = effectDescription.substring(0, effectDescription.length() - 9) + "</font>";
				
				tb.append(effectDescription + "</td></tr></table>");
				
				if(et.getFuncTemplates() != null)
				{
					tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
					
//					if(et.funcName.equals("ChanceSkillTrigger"))
//					{
//						L2Skill triggered = SkillTable.getInstance().getInfo(et.triggeredId, et.triggeredLevel);
//						if(triggered != null)
//						{
//							if(et.chanceCondition != null && et.chanceCondition.getTriggerType() != ChanceCondition.TriggerType. && et.chanceCondition._triggerType != ChanceCondition.TriggerType.ON_START && et.chanceCondition._triggerType != ChanceCondition.TriggerType.ON_ACTION_TIME)
//							{
//								String triggerType = getChanceConditionDescr(et.chanceCondition);
//								tb.append("<br><table bgcolor=2f2f2f width=280><tr><td width=200><font color=FF8040>" + triggerType + "</font></td></tr></table>");
//								
//								tb.append("<table bgcolor=4f4f4f width=280><tr><td width=100><font color=FF8040>>> triggers skill</td><td width=180 align=right><a action=\"bypass -h skillinfo " + triggered.getId() + " " + triggered.getLevel() + " " + skillId + " " + level + "\">" + triggered.getName() + "</a></font></td></tr></table>");
//							}
//							else
//							{
//								tb.append("<table bgcolor=2f2f2f width=280><tr><td width=100><font color=FF8040>triggers skill</td><td width=180 align=right><a action=\"bypass -h skillinfo " + triggered.getId() + " " + triggered.getLevel() + " " + skillId + " " + level + "\">" + triggered.getName() + "</a></font></td></tr></table>");
//							}
//						}
//					}
					
					tb.append("<table width=280 bgcolor=2f2f2f>");
					
					for(FuncTemplate f : et.getFuncTemplates())
					{
						Stats stat = f.stat;
						String desc = stat.getHtmlDesc();
						
						OperationType t = OperationType.getType(f.pFunc);
						
						String power = getPower(f, false);
						if(power == null)
							continue;
						
						if(f.pFunc.equals("Set"))
							tb.append("<tr><td align=left width=200><font color=" + getColorForOperation(t, power, stat) + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
						else
							tb.append("<tr><td align=left width=200><font color=" + stat.getColor().code + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
					}
					
					tb.append("</table>");
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
					tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
				}
				else
				{
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
					tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
				}
			}
		}
		
		if(skill.getEffectTemplates() != null)
		{
			/*List<EffectTemplate> finalEffects = new FastList<EffectTemplate>();
			List<EffectTemplate> addToEffects = null;
			Alter alter = null;
			
			if(!activeChar.getAlters().isEmpty())
			{
				EffectTemplate[] alterEffects = null;
				
				AlterTemplate at;
				
				// get available altered effects for this skill
				for(Entry<Object, List<Alter>> e : activeChar.getAlters().entrySet())
				{
					for(Alter availA : e.getValue())
					{
						at = availA.getTemplate();
						
						if(at.getEffectTemplates() == null)
							continue;
						
						if(at.skillId == skill.getId() && skill.getLevel() >= at.level)
						{
							alterEffects = at.getEffectTemplates();
							alter = availA;
							
							// use new effects from one alter only
							break;
						}
					}
				}
				
				for (EffectTemplate tempEffect : skill.getEffectTemplates())
				{
					EffectTemplate et = tempEffect;
					
					// if any altered effects for this skill exist, replace the original skill effects with altered ones
					if(alterEffects != null)
					{
						// replace the effect with the altereffect
						for(EffectTemplate alterEt : alterEffects)
						{
							if(!alterEt.onlyAddFuncs && alterEt.funcName.equals(tempEffect.funcName))
							{
								et = alterEt;
								break;
							}
						}
					}
					
					finalEffects.add(et); 
				}
				
				// add completelly new effects added by other items/skills
				if(alterEffects != null)
				{
					for(EffectTemplate alterEt : alterEffects)
					{
						if(alterEt.onlyAddFuncs)
						{
							if(addToEffects == null)
								addToEffects = new FastList<>();
								
							addToEffects.add(alterEt);
						}
						else
						{
							if(!finalEffects.contains(alterEt))
								finalEffects.add(alterEt);
						}
					}
				}
			}
			else
			{
				for (EffectTemplate tempEffect : skill.getEffectTemplates())
				{
					finalEffects.add(tempEffect); 
				}
			}*/
			
			
			for(EffectTemplate et : skill.getEffectTemplates())
			{
				String effectName = et.funcName;
				
				L2SkillType type = et.effectType;
				String effectPower = getPower(activeChar, type, et.effectPower);
				int duration = et.period * Math.max(et.counter, 1);
				
				String effectDescription = ("<br1><table width=280><tr><td width=280><font color=FF8040>[" + effectName + "]</font>");
				
				if(effectPower != null && et.effectPower != 400)
					effectDescription = effectDescription + ("<font color=6f6f6f>&nbsp;&nbsp;power " + effectPower + " |</font>");
				if(duration > 0 && !skill.isToggle())
					effectDescription = effectDescription + ("<font color=6f6f6f>&nbsp;&nbsp;lasts " + duration + "s |</font>");
				
				if(effectDescription.endsWith(" |</font>"))
					effectDescription = effectDescription.substring(0, effectDescription.length() - 9) + "</font>";
				
				tb.append(effectDescription + "</td></tr></table>");
				
				if(et.getFuncTemplates() != null)
				{
					tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
					
//					if(et.funcName.equals("ChanceSkillTrigger"))
//					{
//						L2Skill triggered = SkillTable.getInstance().getInfo(et.triggeredId, et.triggeredLevel);
//						if(triggered != null)
//						{
//							if(et.chanceCondition != null && et.chanceCondition.get != ChanceCondition.TriggerType.ON_EXIT && et.chanceCondition._triggerType != ChanceCondition.TriggerType.ON_START && et.chanceCondition._triggerType != ChanceCondition.TriggerType.ON_ACTION_TIME)
//							{
//								String triggerType = getChanceConditionDescr(et.chanceCondition);
//								tb.append("<br><table bgcolor=2f2f2f width=280><tr><td width=200><font color=FF8040>" + triggerType + "</font></td></tr></table>");
//								
//								tb.append("<table bgcolor=4f4f4f width=280><tr><td width=100><font color=FF8040>>> triggers skill</td><td width=180 align=right><a action=\"bypass -h skillinfo " + triggered.getId() + " " + triggered.getLevel() + " " + skillId + " " + level + "\">" + triggered.getName() + "</a></font></td></tr></table>");
//							}
//							else
//							{
//								tb.append("<table bgcolor=2f2f2f width=280><tr><td width=100><font color=FF8040>triggers skill</td><td width=180 align=right><a action=\"bypass -h skillinfo " + triggered.getId() + " " + triggered.getLevel() + " " + skillId + " " + level + "\">" + triggered.getName() + "</a></font></td></tr></table>");
//							}
//						}
//					}
					
					tb.append("<table width=280 bgcolor=2f2f2f>");
					
					for(FuncTemplate f : et.getFuncTemplates())
					{
						Stats stat = f.stat;
						String desc = stat.getHtmlDesc();
						
						OperationType t = OperationType.getType(f.pFunc);
						
						String power = getPower(f, false);
						if(power == null)
							continue;
						
						if(f.pFunc.equals("Set"))
							tb.append("<tr><td align=left width=200><font color=" + getColorForOperation(t, power, stat) + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
						else
							tb.append("<tr><td align=left width=200><font color=" + stat.getColor().code + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
					}
					
					tb.append("</table>");
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
					tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
				}
				else
				{
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
					tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
				}
			}
		}
		
		if(skill.triggerAnotherSkill())
		{
			L2Skill triggered = SkillTable.getInstance().getInfo(skill.getTriggeredId(), skill.getTriggeredLevel());
			if(triggered != null)
			{
				if(skill.isChance())
				{
					String triggerType = getChanceConditionDescr(skill.getTriggeredChanceCondition());
					
					tb.append("<br><table width=280><tr><td width=200><font color=FF8040>" + triggerType + "</font></td></tr></table>");
				}
				
				tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
				tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");

				tb.append("<table bgcolor=2f2f2f width=280><tr><td width=200><font color=FF8040>triggers skill " + triggered.getName() + "</font></td><td width=80 align=right><button value=\"Info\" action=\"bypass -h skillinfo " + triggered.getId() + " " + triggered.getLevel() + " " + skillId + " " + level + "\" width=50 height=18 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
				
				tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
				tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
			}
		}
		
		tb.append("</body></html>");
		
		msg.setHtml(tb.toString());
		activeChar.sendPacket(msg);
	}
	
	public static String getChanceConditionDescr(ChanceCondition cond)
	{
		String toReturn = "";
		switch(cond.getTriggerType())
		{
			case ON_HIT:
				toReturn = cond._chance + "% chance on hit"; 
			case ON_CRIT:
				toReturn = cond._chance + "% chance on crit";
			case ON_CAST:
				toReturn = cond._chance + "% chance on cast";
			case ON_PHYSICAL:
				toReturn = cond._chance + "% chance on fighter skill use";
			case ON_MAGIC:
				toReturn = cond._chance + "% chance on magic skill use";
			case ON_MAGIC_GOOD:
				toReturn = cond._chance + "% chance on support magic use";
			case ON_MAGIC_OFFENSIVE:
				toReturn = cond._chance + "% chance on offensive magic use";
			case ON_ATTACKED:
				toReturn = cond._chance + "% chance when attacked";
			case ON_ATTACKED_HIT:
				toReturn = cond._chance + "% chance when attacked by melee hits";
			case ON_ATTACKED_CRIT:
				toReturn = cond._chance + "% chance when received a crit. hit";
			case ON_HIT_BY_SKILL:
				toReturn = cond._chance + "% chance when hit by a skill";
			case ON_HIT_BY_OFFENSIVE_SKILL:
				toReturn = cond._chance + "% chance when hit by offensive skill";
			case ON_HIT_BY_GOOD_MAGIC:
				toReturn = cond._chance + "% chance when received support magic";
			case ON_EVADED_HIT:
				toReturn = cond._chance + "% chance when you evade a hit";
//			case ON_START:
//				toReturn = cond._chance + "% chance when this skill is casted";
//			case ON_ACTION_TIME:
//				toReturn = cond._chance + "% chance";
//			case ON_EXIT:
//				toReturn = cond._chance + "% chance when this skill finishes";
		}
		
//		if(cond._mindmg > 50)
//		{
//			toReturn = toReturn + " higher than " + cond._mindmg + " dmg";
//		}
		
		if(cond._pvpOnly)
		{
			toReturn = toReturn + " (pvp only)";
		}
		
		if(cond._skillIDs != null)
		{
			toReturn = toReturn + "; triggered when you use ";

			L2Skill sk;
			for(int i : cond._skillIDs)
			{
				sk = SkillTable.getInstance().getInfo(i, 1);
				toReturn = toReturn + sk.getName() + " ";
			}
		}
		
		return toReturn;
	}
	
	public static void showPage(L2PcInstance activeChar, int itemId, L2ItemInstance itemInstance)
	{
		NpcHtmlMessage msg = new NpcHtmlMessage(0);
		
		TextBuilder tb = new TextBuilder();
		
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		if(item == null)
			return;
		
		tb.append("<html><title>" + item.getName() + "</title><body>");
		
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
		tb.append("<table bgcolor=2f2f2f>");
		tb.append("<tr><td align=left width=100><font color=9f9f9f>Item name:</font></td><td align=left width=170><font color=" + getItemColor(item) + ">" + item.getName() + "</font></td></tr>");
		
		if(activeChar.isGM())
		{
				tb.append("<tr><td align=left width=100><font color=6f6f6f>Item ID:</td><td align=left width=170>" + item.getItemId() + "</font></td></tr>");	
		}
		
		String slot = "None";
		
		if(item.getBodyPart() == (L2Item.SLOT_L_FINGER | L2Item.SLOT_R_FINGER))
			slot = "Ring";
		
		else if(item.getBodyPart() == (L2Item.SLOT_L_EAR | L2Item.SLOT_R_EAR))
			slot = "Earring";
		
		switch(item.getBodyPart())
		{
			case L2Item.SLOT_CHEST:
				slot = "Chest armor";
				break;
			case L2Item.SLOT_BACK:
				slot = "Cloak";
				break;
			case L2Item.SLOT_LEGS:
				slot = "Legs armor";
				break;
			case L2Item.SLOT_GLOVES:
				slot = "Gloves";
				break;
			case L2Item.SLOT_FEET:
				slot = "Boots";
				break;
			case L2Item.SLOT_FULL_ARMOR:
				slot = "Fullbody armor";
				break;
			case L2Item.SLOT_L_BRACELET:
			case L2Item.SLOT_R_BRACELET:
				slot = "Bracelet";
				break;
			case L2Item.SLOT_HAIR:
				slot = "Hair accessory (face slot)";
				break;
			case L2Item.SLOT_HAIR2:
				slot = "Hair accessory (hair slot)";
				break;
			case L2Item.SLOT_HAIRALL:
				slot = "Hair accessory (2 slots)";
				break;
			case L2Item.SLOT_NECK:
				slot = "Necklace";
				break;
			case L2Item.SLOT_BELT:
				slot = "Belt";
				break;
			case L2Item.SLOT_DECO:
				slot = "Talisman";
				break;
			case L2Item.SLOT_UNDERWEAR:
				slot = "Underwear";
				break;
			case L2Item.SLOT_L_HAND:
				slot = "Shield / Sigil";
				break;
			case L2Item.SLOT_LR_HAND:
			case L2Item.SLOT_R_HAND:
			{
				if(item instanceof L2Weapon)
				{
					switch(((L2Weapon)item).getItemType())
					{
						case ANCIENT_SWORD:
							slot = "Ancient sword";
							break;
						case BIGBLUNT:
							slot = "2H Blunt";
							break;
						case BIGSWORD:
							slot = "2H Sword";
							break;
						case BLUNT:
							slot = "Blunt";
							break;
						case BOW:
							slot = "Bow";
							break;
						case CROSSBOW:
							slot = "Crossbow";
							break;
						case DAGGER:
							slot = "Dagger";
							break;
						case DUAL:
							slot = "Dual sword";
							break;
						case DUAL_DAGGER:
							slot = "Dual dagger";
							break;
						case DUALFIST:
							slot = "Fists";
							break;
						case ETC:
							slot = "None";
							break;
						case FIST:
							slot = "Fists";
							break;
						case NONE:
							slot = "Shield";
							break;
						case SWORD:
							slot = "1H Sword";
							break;
						case RAPIER:
							slot = "Rapier";
							break;
						case POLE:
							slot = "Polearm";
							break;
						case ROD:
							slot = "Rod";
							break;
						case PET:
							slot = "Pet";
							break;
					}
				}
				
				break;
			}
		}
		tb.append("<tr><td align=left width=100><font color=9f9f9f>Item type:</font></td><td align=left width=170><font color=ac9887>" + slot + "</font></td></tr>");
		
		if(itemInstance != null)
		{
			tb.append("<tr><td align=left width=100><font color=9f9f9f>Enchant level:</font></td><td align=left width=170><font color=" + getEnchantColor(itemInstance.getEnchantLevel(), item) + ">+" + itemInstance.getEnchantLevel() + "</font></td></tr>");
			
			String attr = "None";
			if(itemInstance.getAttackElementPower() > 0)
			{
				switch(itemInstance.getAttackElementType())
				{
					case Elementals.FIRE:
						attr = "<font color=DE4A0C>Fire</font>";
						break;
					case Elementals.DARK:
						attr = "<font color=6B6B6B>Dark</font>";
						break;
					case Elementals.EARTH:
						attr = "<font color=12AF1E>Earth</font>";
						break;
					case Elementals.HOLY:
						attr = "<font color=FCFDC1>Holy</font>";
						break;
					case Elementals.WATER:
						attr = "<font color=3483AD>Water</font>";
						break;
					case Elementals.WIND:
						attr = "<font color=37CEAC>Wind</font>";
						break;
				}
				
				attr += "  (" + itemInstance.getAttackElementPower() + ")";
			}
			tb.append("<tr><td align=left width=100><font color=9f9f9f><font color=9f9f9f>Attack attribute:</font></font></td><td align=left width=170>" + attr + "</td></tr>");
		}
		
		tb.append("</table>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		
		tb.append("<br>");
		
		Map<String, List<FuncTemplate>> stats = new FastMap<String, List<FuncTemplate>>();
		
		if(item.getFuncTemplates() != null)
		{
			// first bonus related to accuracy sets the base accuracy value
			boolean firstAccuracyBonus = false;
			
			for(FuncTemplate f : item.getFuncTemplates())
			{
				if(f.stat != null)
				{
					if(!firstAccuracyBonus && f.stat == Stats.ACCURACY_COMBAT && f.pFunc.equals("Add"))
					{
						firstAccuracyBonus = true;
						
						if(!stats.containsKey("basestats"))
							stats.put("basestats", new FastList<FuncTemplate>());
						stats.get("basestats").add(f);
						
						continue;
					}
					
					
					if((f.pFunc.equals("Set") && f.order < 16) || (f.pFunc.equals("Enchant") && !(item instanceof L2Armor)))
					{
						if(!stats.containsKey("basestats"))
							stats.put("basestats", new FastList<FuncTemplate>());
						stats.get("basestats").add(f);
					}
					else
					{
						if((f.pFunc.equals("Enchant") && !(item instanceof L2Armor)))
							continue;
						
						boolean hasCondition = false;
						if(f.applayCond != null)
						{
							try
							{
								String condDesc = getCondDescription(f.applayCond);
								if(condDesc != null)
								{
									if(!stats.containsKey("cond_" + condDesc))
										stats.put("cond_" + condDesc, new FastList<FuncTemplate>());
									stats.get("cond_" + condDesc).add(f);
									
									hasCondition = true;
								}
							}
							catch (Exception e2)
							{
								e2.printStackTrace();
							}
						}
						
						if(!hasCondition)
						{
							if(!stats.containsKey("stats"))
								stats.put("stats", new FastList<FuncTemplate>());
							stats.get("stats").add(f);
						}
					}
				}
			}
		}
		
		if(item.getItemSkills() != null)
		{
			for(L2Skill skill : item.getItemSkills())
			{
				if(skill == null || skill.getFuncTemplates() == null)
					continue;
				
				for(FuncTemplate f : skill.getFuncTemplates())
				{
					if(f.stat != null)
					{
						if(f.pFunc.equals("Enchant"))
							continue;
						
						boolean hasCondition = false;
						if(f.applayCond != null)
						{
							try
							{
								String condDesc = getCondDescription(f.applayCond);
								if(condDesc != null)
								{
									if(!stats.containsKey("cond_" + condDesc))
										stats.put("cond_" + condDesc, new FastList<FuncTemplate>());
									stats.get("cond_" + condDesc).add(f);
									
									hasCondition = true;
								}
							}
							catch (Exception e2)
							{
								e2.printStackTrace();
							}
						}
						
						if(!hasCondition)
						{
							if(!stats.containsKey("stats"))
								stats.put("stats", new FastList<FuncTemplate>());
							stats.get("stats").add(f);
						}
					}
				}
			}
		}
		
		L2Armor armor = null;
		
		if(item instanceof L2Armor)
		{
			armor = (L2Armor) item;
			L2ArmorSet set = ArmorSetsTable.getInstance().getSet(armor.getItemId());
			
			if(set != null)
			{
				int skillId, skillLvl;
				final String[] skills = set.getSkills();
				
				if (skills != null)
				{
					for (String skillInfo : skills)
					{
						skillId = 0;
						skillLvl = 0;
						String[] skill = skillInfo.split("-");
						if (skill != null && skill.length == 2)
						{
							try
							{
								skillId = Integer.parseInt(skill[0]);
								skillLvl = Integer.parseInt(skill[1]);
							}
							catch (NumberFormatException e)
							{
								e.printStackTrace();
							}
							
							L2Skill sk = SkillTable.getInstance().getInfo(skillId, skillLvl);
							if(sk == null)
								continue;
							
							if(sk.getFuncTemplates() == null)
								continue;
							
							
							for(FuncTemplate f : sk.getFuncTemplates())
							{
								if(f.stat != null)
								{
									if(f.pFunc.equals("Enchant"))
										continue;
									
									if(!stats.containsKey("Set bonus:"))
										stats.put("Set bonus:", new FastList<FuncTemplate>());
									stats.get("Set bonus:").add(f);
								}
							}
						}
					}
				}
			}
		}
		
		//TODO sort them here
		
		// dump base stats here
		
		if(item instanceof L2Armor)
		{
			String pdef = null;
			String mdef = null;
			
			if(stats.containsKey("stats"))
			{
				for(FuncTemplate f : stats.get("stats"))
				{
					if(f.pFunc.equals("Add") || f.pFunc.equals("Enchant"))
					{
						if(f.stat == Stats.MAGIC_DEFENCE)
						{
							String power = getPower(f, true);
							if(power == null)
								continue;
							
							if(itemInstance != null)
							{
								if(mdef == null)
								{
									if(f.pFunc.equals("Add"))
									{
										mdef = power;
										stats.get("stats").remove(f); // don't show this Add stat anymore in Stats info
									}
									else continue;
								}
								else
								{
									try
									{
										if(f.pFunc.equals("Enchant"))
										{
											int patkInt = Integer.parseInt(mdef);
											Env env = new Env();
											env.item = itemInstance;
											
											FuncEnchant func = (FuncEnchant)f.getFunc(env, itemInstance);
											func.calc(env);
											double enchantAdd = env.value;
											
											patkInt += (int) enchantAdd;
											
											mdef = String.valueOf(patkInt);
										}
									}
									catch (Exception e)
									{
									}
								}
							}
							else
							{
								if(f.pFunc.equals("Add"))
									mdef = power;
							}
						}
						
						if(f.stat == Stats.POWER_DEFENCE)
						{
							String power = getPower(f, true);
							if(power == null)
								continue;
							
							if(itemInstance != null)
							{
								if(pdef == null)
								{
									if(f.pFunc.equals("Add"))
									{
										pdef = power;
										stats.get("stats").remove(f); // don't show this Add stat anymore in Stats info
									}
									else continue;
								}
								else
								{
									try
									{
										if(f.pFunc.equals("Enchant"))
										{
											int patkInt = Integer.parseInt(pdef);
											Env env = new Env();
											env.item = itemInstance;
											
											FuncEnchant func = (FuncEnchant)f.getFunc(env, itemInstance);
											func.calc(env);
											double enchantAdd = env.value;
											
											patkInt += (int) enchantAdd;
											
											pdef = String.valueOf(patkInt);
										}
									}
									catch (Exception e)
									{
									}
								}
							}
							else
							{
								if(f.pFunc.equals("Add"))
									pdef = power;
							}
						}
					}
				}
			}
			
			if(mdef != null || pdef != null)
			{
				tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
				tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
				tb.append("<table width=280 bgcolor=2f2f2f>");
				
				if(mdef != null)
					tb.append("<tr><td align=left width=180><font color=9f9f9f>Base M.def</font></td><td align=left width=90><font color=ac9887>" + mdef + "</font></td></tr>");
				
				if(pdef != null)
					tb.append("<tr><td align=left width=180><font color=9f9f9f>Base P.def</font></td><td align=left width=90><font color=ac9887>" + pdef + "</font></td></tr>");
				
				tb.append("</table>");
				tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
				tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
			}
		}
		
		if(stats.containsKey("basestats"))
		{
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<table width=280 bgcolor=2f2f2f>");
			
			if(item instanceof L2Weapon)
			{
				String patk = null;
				String matk = null;
				
				for(FuncTemplate f : stats.get("basestats"))
				{
					if(f.stat == Stats.POWER_ATTACK)
					{
						String power = getPower(f, true);
						if(power == null)
							continue;
						
						if(itemInstance != null)
						{
							
							if(patk == null)
							{
								if(f.pFunc.equals("Set"))
									patk = power;
								else continue;
							}
							else
							{
								try
								{
									if(f.pFunc.equals("Enchant"))
									{
										int patkInt = Integer.parseInt(patk);
										Env env = new Env();
										env.item = itemInstance;
										
										FuncEnchant func = (FuncEnchant)f.getFunc(env, itemInstance);
										func.calc(env);
										double enchantAdd = env.value;
										
										patkInt += (int) enchantAdd;
										
										patk = String.valueOf(patkInt);
									}
								}
								catch (Exception e)
								{
								}
							}
						}
						else
						{
							if(f.pFunc.equals("Set"))
								patk = power;
							else continue;
						}
					}
					
					if(f.stat == Stats.MAGIC_ATTACK)
					{
						String power = getPower(f, true);
						if(power == null)
							continue;
						
						if(itemInstance != null)
						{
							if(matk == null)
							{
								if(f.pFunc.equals("Set"))
									matk = power;
								else continue;
							}
							else
							{
								try
								{
									if(f.pFunc.equals("Enchant"))
									{
										int matkInt = Integer.parseInt(matk);
										Env env = new Env();
										env.item = itemInstance;
										
										FuncEnchant func = (FuncEnchant)f.getFunc(env, itemInstance);
										func.calc(env);
										double enchantAdd = env.value;
										
										matkInt += (int) enchantAdd;
										
										matk = String.valueOf(matkInt);
									}
								}
								catch (Exception e)
								{
								}
							}
						}
						else
						{
							if(f.pFunc.equals("Set"))
								matk = power;
							else continue;
						}
					}
				}
				
				if(patk != null)
					tb.append("<tr><td align=left width=180><font color=9f9f9f>Base P.atk</font></td><td align=left width=90><font color=ac9887>" + patk + "</font></td></tr>");
				
				if(matk != null)
					tb.append("<tr><td align=left width=180><font color=9f9f9f>Base M.atk</font></td><td align=left width=90><font color=ac9887>" + matk + "</font></td></tr>");
				
				for(FuncTemplate f : stats.get("basestats"))
				{
					if(f.stat == Stats.POWER_ATTACK_SPEED)
					{
						String power = getPower(f, true);
						if(power == null)
							continue;
						
						try
						{
							int powerInt = Integer.parseInt(power);
							String val = null;
							
							if(powerInt >= 433)
								val = "Very fast";
							else if(powerInt >= 379)
								val = "Fast";
							else if(powerInt >= 325)
								val = "Normal";
							else if(powerInt >= 293)
								val = "Slow";
							else if(powerInt >= 227)
								val = "Very slow";
							else if(powerInt <= 227)
								val = "Incredibly slow";
							
							if(val != null)
								tb.append("<tr><td align=left width=180><font color=9f9f9f>Attack speed</font></td><td align=left width=90><font color=ac9887>" + val + "</font></td></tr>");
						}
						catch (Exception e)
						{
							continue;
						}
					}
				}
				
				for(FuncTemplate f : stats.get("basestats"))
				{
					if(f.stat == Stats.CRITICAL_RATE)
					{
						String power = getPower(f, true);
						if(power == null)
							continue;
						
						try
						{
							int powerInt = Integer.parseInt(power);
							
							tb.append("<tr><td align=left width=180><font color=9f9f9f>Base crit.</font></td><td align=left width=90><font color=ac9887>" + powerInt + "</font></td></tr>");
						}
						catch (Exception e)
						{
							continue;
						}
					}
				}
				
				for(FuncTemplate f : stats.get("basestats"))
				{
					if(f.stat == Stats.ACCURACY_COMBAT)
					{
						String power = getPower(f, true);
						if(power == null)
							continue;
						
						try
						{
							int powerInt = Integer.parseInt(power);
							
							tb.append("<tr><td align=left width=180><font color=9f9f9f>Base accuracy</font></td><td align=left width=90><font color=ac9887>" + powerInt + "</font></td></tr>");
						}
						catch (Exception e)
						{
							continue;
						}
					}
				}
			}
			
			tb.append("</table>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		}
		
		//TODO
		
		for(Entry<String, List<FuncTemplate>> e : stats.entrySet())
		{
			if(!e.getKey().equals("stats"))
				continue;
			
			if(e.getKey().equals("stats"))
				tb.append("<br><font color=LEVEL>&nbsp;[Stats]:</font>");
			else
				tb.append("<br><font color=LEVEL>&nbsp;" + e.getKey() + "</font>");
			
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<table width=280 bgcolor=2f2f2f>");
			
			for(FuncTemplate f : e.getValue())
			{
				if(f.pFunc.equals("Enchant"))
					continue;
				
				Stats stat = f.stat;
				String desc = stat.getHtmlDesc();
				
				OperationType t = OperationType.getType(f.pFunc);
				
				String power = getPower(f, false);
				if(power == null)
					continue;
				
				if(f.pFunc.equals("Set"))
					tb.append("<tr><td align=left width=200><font color=" + getColorForOperation(t, power, stat) + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
				else
					tb.append("<tr><td align=left width=200><font color=" + stat.getColor().code + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
			}
			
			tb.append("</table>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		}
		
		for(Entry<String, List<FuncTemplate>> e : stats.entrySet())
		{
			if(!e.getKey().startsWith("cond_"))
				continue;
			
			String condName = e.getKey().substring(5);
			
			tb.append("<br><table width=280><tr><td width=280><font color=FF8040>[" + condName + "]:</font></td></tr></table>");
			
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<table width=280 bgcolor=2f2f2f>");
			
			for(FuncTemplate f : e.getValue())
			{
				if(f.pFunc.equals("Enchant"))
					continue;
				
				Stats stat = f.stat;
				String desc = stat.getHtmlDesc();
				
				OperationType t = OperationType.getType(f.pFunc);
				
				String power = getPower(f, false);
				if(power == null)
					continue;
				
				if(f.pFunc.equals("Set"))
					tb.append("<tr><td align=left width=200><font color=" + getColorForOperation(t, power, stat) + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
				else
					tb.append("<tr><td align=left width=200><font color=" + stat.getColor().code + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
			}
			
			tb.append("</table>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		}
		
		for(Entry<String, List<FuncTemplate>> e : stats.entrySet())
		{
			if(!e.getKey().equals("Set bonus:"))
				continue;
			
			tb.append("<br><font color=LEVEL>&nbsp;" + e.getKey() + "</font>");
			
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<table width=280 bgcolor=2f2f2f>");
			
			for(FuncTemplate f : e.getValue())
			{
				if(f.pFunc.equals("Enchant"))
					continue;
				
				Stats stat = f.stat;
				String desc = stat.getHtmlDesc();
				
				OperationType t = OperationType.getType(f.pFunc);
				
				String power = getPower(f, false);
				if(power == null)
					continue;
				
				if(f.pFunc.equals("Set"))
					tb.append("<tr><td align=left width=200><font color=" + getColorForOperation(t, power, stat) + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
				else
					tb.append("<tr><td align=left width=200><font color=" + stat.getColor().code + ">" + desc + "</font></td><td align=left width=70><font color=" + getColorForOperation(t, power, stat) + ">" + power + "</font></td></tr>");
			}
			
			tb.append("</table>");
			tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
			tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		}
		
		//add info about active skills
		
		if(item.getItemSkills() != null)
		{
			for(L2Skill skill : item.getItemSkills())
			{
				if(skill == null)
					continue;
				
				boolean b = true;
				
				// one of the item skills triggers another skill
				if(skill.getTriggeredChanceId() > 1 && skill.isChance())
				{
					L2Skill trig = SkillTable.getInstance().getInfo(skill.getTriggeredChanceId(), skill.getTriggeredChanceLevel());
					if(trig == null)
						continue;
					
					String type = null;
					try
					{
						type = trig.getSkillType().toString().toLowerCase();
					}
					catch (Exception e2)
					{
					}
					
					ChanceCondition cd = skill.getTriggeredChanceCondition();
					if(cd == null)
						continue;
					
					TriggerType tt = cd.getTriggerType();
					String triggerName = getTriggerTypeName(tt);
					
					if(triggerName == null)
						continue;
					
					String target = getSkillTarget(trig);
					
					int chance = cd.getChance();
					
					if(type != null && trig.isActive())
					{
						tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
						tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
						
						
						if(itemInstance == null)
							tb.append("<br><font color=ac9887><table width=280><tr><td>" + chance + "% chance that " + triggerName + ", item triggers skill" + (target != null ? (" on " + target) : "") + ":</td></tr><tr><td align=right> </font><font color=989A65><a action=\"bypass -h skillinfo " + trig.getId() + " " + trig.getLevel() + " " + item.getItemId() + "\">" + trig.getName() + "</a></td></tr></font></table>");
						else
							tb.append("<br><font color=ac9887><table width=280><tr><td>" + chance + "% chance that " + triggerName + ", item triggers skill" + (target != null ? (" on " + target) : "") + ":</td></tr><tr><td align=right> </font><font color=989A65><a action=\"bypass -h skillinfo " + trig.getId() + " " + trig.getLevel() + "\">" + trig.getName() + "</a></td></tr></font></table>");
						
						
						tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
						tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
					}
					
					b = false;
				}
				
				if(b)
				{
					if(skill.isToggle())
					{
						tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
						tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
						
						if(itemInstance == null)
							tb.append("<br><font color=ac9887><table width=280><tr><td>Gives toggle skill:</td><td align=right> <a action=\"bypass -h skillinfo " + skill.getId() + " " + skill.getLevel() + " " + item.getItemId() + "\">" + skill.getName() + (skill.getLevel() > 1 ? " lvl"+skill.getLevel() : "") + "</a></font></td></tr></table>");
						else
							tb.append("<br><font color=ac9887><table width=280><tr><td>Gives toggle skill:</td><td align=right> <a action=\"bypass -h skillinfo " + skill.getId() + " " + skill.getLevel() + "\">" + skill.getName() + (skill.getLevel() > 1 ? " lvl"+skill.getLevel() : "") + "</a></font></td></tr></table>");
						
						tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
						tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
					}
					else if(skill.isActive())
					{
						tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
						tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
						
						if(itemInstance == null)
							tb.append("<br><font color=ac9887><table width=280><tr><td>Gives active skill:</td><td align=right> </font><font color=FF8040><a action=\"bypass -h skillinfo " + skill.getId() + " " + skill.getLevel() + " " + item.getItemId() + "\">" + skill.getName() + (skill.getLevel() > 1 ? " lvl"+skill.getLevel() : "") + "</a></font></td></tr></table>");
						else
							tb.append("<br><font color=ac9887><table width=280><tr><td>Gives active skill:</td><td align=right> </font><font color=FF8040><a action=\"bypass -h skillinfo " + skill.getId() + " " + skill.getLevel() + "\">" + skill.getName() + (skill.getLevel() > 1 ? " lvl"+skill.getLevel() : "") + "</a></font></td></tr></table>");
						
						tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
						tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
					}
				}
			}
		}
		
		// handle weapon's special onCrit and onCast skills
		if(item instanceof L2Weapon)
		{
			L2Weapon weap = (L2Weapon) item;
			
			if(weap.getOnCastSkill() != null)
			{
				L2Skill onCast = weap.getOnCastSkill();
				if(onCast != null)
				{
					Condition cond = weap.getOnCastSkillCondition();
					int chance = 0;
					if(cond instanceof ConditionGameChance)
						chance = ((ConditionGameChance)cond).getChance();
					
					String type = null;
					try
					{
						type = onCast.getSkillType().toString().toLowerCase();
					}
					catch (Exception e2)
					{
					}
					
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
					tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
					
					if(type != null && chance > 0)
						tb.append("<br><font color=ac9887><table width=280><tr><td>" + chance + "% chance that on skill cast, item triggers skill</td></tr><tr><td align=right> </font><font color=FF8040><a action=\"bypass -h skillinfo " + onCast.getId() + " " + onCast.getLevel() + " " + item.getItemId() + "\"> " + onCast.getName() + "</a></font></td></tr></table>");
					
					tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
				}
			}
			
			if(weap.getOnCritSkill() != null)
			{
				L2Skill onCrit = weap.getOnCritSkill();
				if(onCrit != null)
				{
					Condition cond = weap.getOnCritSkillCondition();
					int chance = 0;
					if(cond instanceof ConditionGameChance)
						chance = ((ConditionGameChance)cond).getChance();
					
					String type = null;
					try
					{
						type = onCrit.getSkillType().toString().toLowerCase();
					}
					catch (Exception e2)
					{
					}
					
					String target = getSkillTarget(onCrit);
					
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
					tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
					
					if(type != null && chance > 0)
						tb.append("<br><font color=ac9887><table width=280><tr><td>" + chance + "% chance that on critical hit, item triggers " + type + " skill " + onCrit.getName() + "" + (target != null ? (" on " + target) : "") + ".</font></td></tr></table>");
					
					tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
					tb.append("<img src=\"L2UI.SquareBlank\" width=510 height=2>");
				}
			}
		}
		
		tb.append("</body></html>");
		
		msg.setHtml(tb.toString());
		activeChar.sendPacket(msg);
	}
	
	private static String getCondDescription(Condition c)
	{
		String desc = "";
		
		if(c instanceof ConditionLogicAnd)
		{
			for(Condition subcond : ((ConditionLogicAnd)c).conditions)
			{
				desc = desc + getCondDescription(subcond) + " and ";
			}
			if(desc.length() == 0) return null;
			else
				desc = desc.substring(0, desc.length() - 4);
		}
		else if(c instanceof ConditionLogicOr)
		{
			for(Condition subcond : ((ConditionLogicOr)c).conditions)
			{
				desc = desc + getCondDescription(subcond) + " or ";
			}
			if(desc.length() == 0) return null;
			else
				desc = desc.substring(0, desc.length() - 3);
		}
		else
		{
//			if(c instanceof ConditionEnchant)
//			{
//				desc = "when enchant level is >= " + ((ConditionEnchant)c)._enchantLevel;
//			}
//			else 
			if(c instanceof ConditionGameTime)
			{
				boolean required = ((ConditionGameTime)c)._required;
				CheckGameTime gt = ((ConditionGameTime)c)._check;
				
				if(required)
				{
					if(gt == CheckGameTime.NIGHT)
					{
						desc = "during the night";
					}
					else
					{
						desc = "during the day";
					}
				}
				else
				{
					if(gt == CheckGameTime.NIGHT)
					{
						desc = "during the day";
					}
					else
					{
						desc = "during the night";
					}
				}
			}
			else if(c instanceof ConditionMinDistance)
			{
				desc = "when your distance from target bigger than " + ((int)Math.sqrt(((ConditionMinDistance)c)._sqDistance));
			}
			else if(c instanceof ConditionPlayerActiveEffectId)
			{
				int id = ((ConditionPlayerActiveEffectId)c)._effectId;
				int level = ((ConditionPlayerActiveEffectId)c)._effectLvl;
				
				if(level < 1)
					level = 1;
				
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				
				if(skill == null)
					desc = null;
				else
					desc = "when " + skill.getName() + " effect is active";
			}
			else if(c instanceof ConditionPlayerActiveSkillId)
			{
				int id = ((ConditionPlayerActiveSkillId)c)._skillId;
				int level = ((ConditionPlayerActiveSkillId)c)._skillLevel;
				
				if(level < 1)
					level = 1;
				
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				
				if(skill == null)
					desc = null;
				else
					desc = "when you have skill " + skill.getName() + " of level " + level;
			}
			else if(c instanceof ConditionPlayerClassIdRestriction)
			{
				String classes = arrayToStringClasses(((ConditionPlayerClassIdRestriction)c)._classIds.toArray(new Integer[((ConditionPlayerClassIdRestriction)c)._classIds.size()]));
				
				desc = "if you are " + classes;
			}
			else if(c instanceof ConditionPlayerCp)
			{
				desc = "when you have more than " + ((ConditionPlayerCp)c)._cp + "% CP";
			}
			else if(c instanceof ConditionPlayerHp)
			{
				desc = "when you have less than " + ((ConditionPlayerHp)c)._hp + "% HP";
			}
//			else if(c instanceof ConditionPlayerCharges)
//			{
//				desc = "when your focus/sonic charge level is " + ((ConditionPlayerCharges)c)._charges + "";
//			}
//			else if(c instanceof ConditionPlayerIsHero)
//			{
//				desc = "if you are a hero";
//			}
			else if(c instanceof ConditionPlayerIsClanLeader)
			{
				desc = "if you are a clan leader";
			}
			else if(c instanceof ConditionPlayerLevel)
			{
				desc = "when you are level " + ((ConditionPlayerLevel)c)._level + " or higher";
			}
//			else if(c instanceof ConditionPlayerMovement)
//			{
//				desc = "when you are " + (((ConditionPlayerMovement)c)._running ? " running." : " standing.");
//			}
//			else if(c instanceof ConditionPlayerLevelRange)
//			{
//				desc = "when you are level " + ((ConditionPlayerLevelRange)c)._levels[0] + "-" + ((ConditionPlayerLevelRange)c)._levels[1];
//			}
			else if(c instanceof ConditionPlayerMp)
			{
				desc = "when you have less than " + ((ConditionPlayerMp)c)._mp + "% MP";
			}
			else if(c instanceof ConditionPlayerRace)
			{
				desc = "if your race is " + ((ConditionPlayerRace)c)._race.toString();
			}
//			else if(c instanceof ConditionPlayerServitorNpcId)
//			{
//				desc = "if you have active summon: " + arrayToStringNpc(((ConditionPlayerServitorNpcId)c)._npcIds.toArray(new Integer[((ConditionPlayerServitorNpcId)c)._npcIds.size()]));
//			}
			else if(c instanceof ConditionPlayerSex)
			{
				if(((ConditionPlayerSex)c)._sex == 0)
				{
					desc = "when you are a male";
				}
				else
				{
					desc = "when you are a female";
				}
			}
//			else if(c instanceof ConditionPlayerSouls)
//			{
//				desc = "when you possess at least " + ((ConditionPlayerSouls)c)._souls + " souls (kamael)";
//			}
			else if(c instanceof ConditionPlayerState)
			{
				if(((ConditionPlayerState)c)._required)
				{
					desc = "when you are (in) " + ((ConditionPlayerState)c)._check.toString().toLowerCase();
				}
				else
				{
					desc = "when you are NOT " + ((ConditionPlayerState)c)._check.toString().toLowerCase();
				}
			}
			else if(c instanceof ConditionSlotItemId)
			{
				int itemId = ((ConditionSlotItemId)c)._itemId;
				int enchantLevel = ((ConditionSlotItemId)c)._enchantLevel;
				
				String itemName = null;
				try
				{
					itemName = ItemTable.getInstance().getTemplate(itemId).getName();
				}
				catch (Exception e)
				{
					itemName = "???";
				}
				
				if(enchantLevel == 0)
					desc = "when wearing " + itemName;
				else
					desc = "when wearing +" + enchantLevel + " " + itemName;
			}
			else if(c instanceof ConditionTargetActiveEffectId)
			{
				int id = ((ConditionTargetActiveEffectId)c)._effectId;
				int level = ((ConditionTargetActiveEffectId)c)._effectLvl;
				
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				
				if(skill == null)
					desc = null;
				else
					desc = "when your target has " + skill.getName() + " effect active";
			}
			else if(c instanceof ConditionTargetActiveSkillId)
			{
				int id = ((ConditionTargetActiveSkillId)c)._skillId;
				int level = ((ConditionTargetActiveSkillId)c)._skillLevel;
				
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				
				if(skill == null)
					desc = null;
				else
					desc = "when your target has skill " + skill.getName() + " of level " + level;
			}
//			else if(c instanceof ConditionTargetActiveEffectType)
//			{
//				L2SkillType type = ((ConditionTargetActiveEffectType)c)._effect;
//				
//				desc = "when your target has a " + type.toString() + " active";
//			}
			else if(c instanceof ConditionTargetAbnormal)
			{
				int id = ((ConditionTargetAbnormal)c)._abnormalId;
				
				desc = "when your target is under effect of " + AbnormalEffect.getByMask(id).toString();
			}
			else if(c instanceof ConditionTargetAggro)
			{
				if(((ConditionTargetAggro)c)._isAggro)
				{
					desc = "if your target has karma";
				}
				else
				{
					desc = null;
				}
			}
			else if(c instanceof ConditionTargetClassIdRestriction)
			{
				String classes = arrayToStringClasses(((ConditionTargetClassIdRestriction)c)._classIds.toArray(new Integer[((ConditionTargetClassIdRestriction)c)._classIds.size()]));
				
				desc = "if your target is " + classes;
			}
			else if(c instanceof ConditionTargetLevel)
			{
				desc = "when your target's level >= " + ((ConditionTargetLevel)c)._level;
			}
//			else if(c instanceof ConditionTargetPlayable)
//			{
//				desc = "when your target is a player or summon";
//			}
			else if(c instanceof ConditionTargetRace)
			{
				desc = "if your target is " + ((ConditionTargetRace)c)._race.toString();
			}
//			else if(c instanceof ConditionTargetArmorType)
//			{
//				desc = "if your target wears " + ((ConditionTargetArmorType)c)._armor.toString() + " armor";
//			}
//			else if(c instanceof ConditionPlayerArmorType)
//			{
//				desc = "if you wears " + ((ConditionPlayerArmorType)c)._armor.toString() + " armor";
//			}
//			else if(c instanceof ConditionTargetUsesWeaponKind)
//			{
//				String types = arrayToString(((ConditionTargetUsesWeaponKind)c)._types);
//				
//				desc = "if your target is wearing " + types;
//			}
//			else if(c instanceof ConditionUsingItemType)
//			{
//				String types = arrayToString(((ConditionUsingItemType)c)._typeNames);
//						
//				desc = "when wearing " + types;
//			}
			else if(c instanceof ConditionUsingSkill)
			{
				int id = ((ConditionUsingSkill)c)._skillId;
				
				L2Skill skill = SkillTable.getInstance().getInfo(id, 1);
				
				if(skill == null)
					desc = null;
				else
					desc = "when casting skill " + skill.getName();
			}
//			else if(c instanceof ConditionUsingSkillTargetType)
//			{
//				boolean aoeSkillOnly = ((ConditionUsingSkillTargetType)c).massSkill;
//				
//				if(!aoeSkillOnly)
//					desc = "when casting single-target skill";
//				else
//					desc = "when casting mass-target skill";
//			}
		}
		
		return desc;
	}
	
	private static String arrayToStringClasses(Integer[] array)
	{
		TextBuilder tb = new TextBuilder();
		ClassId id = null;
		
		for(int i = 0; i < array.length; i++)
		{
			id = ClassId.values()[array[i]];
			
			tb.append(readibility(id.toString()));
			
			if(i+2 < array.length)
			{
				tb.append(", ");
			}
			else if(i+1 < array.length)
				tb.append(" or ");
		}
		return tb.toString();
	}
	
	private static String readibility(String orig)
	{
		TextBuilder tb = new TextBuilder();
		
		int i = 0;
		for(char c : orig.toCharArray())
		{
			if(i == 0)
			{
				tb.append(Character.toUpperCase(c));
			}
			else
			{
				if(Character.isUpperCase(c))
				{
					tb.append(" ");
				}
				
				tb.append(c);
			}
			
			i++;
		}
		return tb.toString();
	}
	
	@SuppressWarnings("unused")
	private static String arrayToStringNpc(Integer[] array)
	{
		TextBuilder tb = new TextBuilder();
		int npc = 0;
		for(int i = 0; i < array.length; i++)
		{
			npc = array[i];
			
			try
			{
				tb.append(NpcTable.getInstance().getTemplate(npc).getName());
			}
			catch (Exception e)
			{
				continue;
			}
					
			if(i+2 < array.length)
			{
				tb.append(", ");
			}
			else if(i+1 < array.length)
				tb.append(" or ");
		}
		return tb.toString();
	}
	
	@SuppressWarnings("unused")
	private static String arrayToString(Object[] array)
	{
		TextBuilder tb = new TextBuilder();
		for(int i = 0; i < array.length; i++)
		{
			tb.append(array[i]);
			
			if(i+2 < array.length)
			{
				tb.append(", ");
			}
			else if(i+1 < array.length)
				tb.append(" or ");
		}
		return tb.toString();
	}
	
	@SuppressWarnings("incomplete-switch")
	private static String getSkillTarget(L2Skill skill)
	{
		switch(skill.getTargetType(null))
		{
			case TARGET_SELF:
				return "yourself";
			case TARGET_ALLY:
				return "ally memebrs";
			case TARGET_CLAN:
//			case TARGET_CLAN_MEMBER:
				return "clan members";
			case TARGET_AREA:
			case TARGET_GROUND:
				return "nearby players";
			case TARGET_AURA:
			case TARGET_FRONT_AREA:
			case TARGET_FRONT_AURA:
				return "players in front of you";
			case TARGET_BEHIND_AREA:
			case TARGET_BEHIND_AURA:
				return "players behind you";
			case TARGET_ENEMY_SUMMON:
				return "enemy summon";
			case TARGET_PARTY:
				return "your party members";
//			case TARGET_PARTY_CLAN:
//				return "your clan & party members";
			case TARGET_PARTY_MEMBER:
				return "your party member";
//			case TARGET_PARTY_NOTME:
//				return "your party members except you";
			case TARGET_PET:
				return "your pet";
			case TARGET_SUMMON:
				return "your summon";
			case TARGET_ONE:
				return "your target";
			case TARGET_PARTY_OTHER:
				return ("on your party");
		}
		
		return null;
	}
	
	private static String getTriggerTypeName(TriggerType t)
	{
		switch(t)
		{
			case ON_ATTACKED:
				return "when you are attacked";
			case ON_ATTACKED_CRIT:
				return "when hit by a critical strike";
			case ON_ATTACKED_HIT:
				return "when you are melee-attacked";
			case ON_EVADED_HIT:
				return "when you evade hit";
			case ON_CAST:
				return "when you cast a skill";
			case ON_CRIT:
				return "when hit a critical";
			case ON_HIT:
				return "when you hit";
			case ON_HIT_BY_GOOD_MAGIC:
				return "when you are hit by good magic";
			case ON_HIT_BY_OFFENSIVE_SKILL:
				return "when you are hit by offensive spell";
			case ON_HIT_BY_SKILL:
				return "when you are hit by a skill";
			case ON_MAGIC:
				return "when you cast a spell";
			case ON_MAGIC_GOOD:
				return "when you cast good magic";
			case ON_MAGIC_OFFENSIVE:
				return "when you cast an offensive spell";
			case ON_PHYSICAL:
				return "when you cast a physical skill";
		}
		
		return null;
	}
	
	@SuppressWarnings("incomplete-switch")
	private static String getColorForOperation(OperationType t, String power, Stats stat)
	{
		switch(t)
		{
			case Add:
				return "82BF73";
			case Mul:
			{
				boolean minus = power != null && power.startsWith("-");
				switch(stat)
				{
					case ATK_REUSE:
					case P_REUSE:
					case MAGIC_REUSE_RATE:
//					case ATTACK_CANCEL:
//					case MAGIC_FAILURE_RATE:
//					case SUMMON_PSKILLREUSE:
//					case SUMMON_MSKILLREUSE:

					case AGGRESSION_VULN:
					case BLEED_VULN:
					case POISON_VULN:
					case STUN_VULN:
					case PARALYZE_VULN:
					case ROOT_VULN:
					case SLEEP_VULN:
					case CONFUSION_VULN:
//					case DAMAGE_ZONE_VULN:
					case MOVEMENT_VULN:
					case CANCEL_VULN:
					case DERANGEMENT_VULN:
					case DEBUFF_VULN:
					case BUFF_VULN:
					case CRIT_VULN:
//					case CRIT_ADD_VULN:
					case MAGIC_DAMAGE_VULN:
					
					case NONE_WPN_VULN:
					case SWORD_WPN_VULN:
					case BLUNT_WPN_VULN:
					case DAGGER_WPN_VULN:
					case BOW_WPN_VULN:
					case CROSSBOW_WPN_VULN:
					case POLE_WPN_VULN:
					case ETC_WPN_VULN:
					case FIST_WPN_VULN:
					case DUAL_WPN_VULN:
					case DUALFIST_WPN_VULN:
					case BIGSWORD_WPN_VULN:
					case BIGBLUNT_WPN_VULN:
					case DUALDAGGER_WPN_VULN:
					case RAPIER_WPN_VULN:
					case ANCIENT_WPN_VULN:
					case PET_WPN_VULN:
					
					case PHYSICAL_MP_CONSUME_RATE:
					case MAGICAL_MP_CONSUME_RATE:
					case DANCE_MP_CONSUME_RATE:
					case BOW_MP_CONSUME_RATE:
					case HP_CONSUME_RATE:
					case MP_CONSUME:
						if(minus)
							return "CFCC41"; // yellow
						else
							return "D2513E"; // red
						
					default:
						if(minus)
							return "D2513E"; // red
						else
							return "CFCC41"; // yellow
				}
			}
			case Set:
				return "5C77B4";
			case Sub:
				return "BF7373";
		}
		return "FFFFFF";
	}
	
	enum OperationType
	{
		Add,
		Mul,
		Set,
		Sub,
		EnchantHp;
		
		public static OperationType getType(String o)
		{
			if(o.equalsIgnoreCase("Add"))
			{
				return Add;
			}
			else if(o.equalsIgnoreCase("Sub"))
			{
				return Sub;
			}
			else if(o.equalsIgnoreCase("Set"))
			{
				return Set;
			}
			else if(o.equalsIgnoreCase("Mul"))
			{
				return Mul;
			}
			else if(o.equalsIgnoreCase("EnchantHp"))
			{
				return EnchantHp;
			}
			
			System.out.println("undefined operation: " + o);
			return null;
		}
	}
	
	public static String createSkill(int skillId, int skillLevel)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		if(skill == null)
			return "Wrong skill id";
			
		FuncTemplate[] funcs = skill.getFuncTemplates();
		
		String result = writeBonuses(funcs, true);
		if(result.length() == 0 && (skill.isActive() || skill.isToggle()))
		{
			result = "gives active skill " + skill.getName() + (skill.getLevel() > 1 ? " lvl"+skill.getLevel() : "") + "";
		}
		return result;
	}
	
	private static String getPower(FuncTemplate f, boolean onlyNumber)
	{
		Locale.setDefault(Locale.US);
		
		String power = "";
		
		if(f.pFunc.equals("EnchantHp"))
		{
			return "+ with enchant";
		}
		
		if(f.pFunc.equals("Add"))
		{
			int ammount = (int) f.lambda.calc(null);
			
			switch(f.stat)
			{
				case AGGRESSION_PROF:
				case BLEED_PROF:
				case POISON_PROF:
				case STUN_PROF:
				case PARALYZE_PROF:
				case ROOT_PROF:
				case SLEEP_PROF:
				case PROF:
				case CANCEL_PROF:
				case DERANGEMENT_PROF:
				case DEBUFF_PROF:
//				case CRIT_PROF:
				case AGGRESSION_VULN:
				case BLEED_VULN:
				case POISON_VULN:
				case STUN_VULN:
				case PARALYZE_VULN:
				case ROOT_VULN:
				case SLEEP_VULN:
				case CONFUSION_VULN:
//				case DAMAGE_ZONE_VULN:
				case MOVEMENT_VULN:
				case CANCEL_VULN:
				case DERANGEMENT_VULN:
				case DEBUFF_VULN:
				case BUFF_VULN:
				case CRIT_VULN:
//				case CRIT_ADD_VULN:
				case MAGIC_DAMAGE_VULN:
				{
					if(ammount > 0)
						power = ("+" + (int)((double)ammount / 4) + "%");
					else
						power = ("-" + (int)((double)ammount / 4) + "%");
					break;
				}
				default:
				{
					if(ammount > 0)
						power = ("+" + ammount);
					else
						power = ("-" + ammount*-1);
				}
			}
		}
		
		if(f.pFunc.equals("Sub"))
		{
			int ammount = (int) f.lambda.calc(null);
			
			switch(f.stat)
			{
				case AGGRESSION_PROF:
				case BLEED_PROF:
				case POISON_PROF:
				case STUN_PROF:
				case PARALYZE_PROF:
				case ROOT_PROF:
				case SLEEP_PROF:
				case PROF:
				case CANCEL_PROF:
				case DERANGEMENT_PROF:
				case DEBUFF_PROF:
//				case CRIT_PROF:
				case AGGRESSION_VULN:
				case BLEED_VULN:
				case POISON_VULN:
				case STUN_VULN:
				case PARALYZE_VULN:
				case ROOT_VULN:
				case SLEEP_VULN:
				case CONFUSION_VULN:
//				case DAMAGE_ZONE_VULN:
				case MOVEMENT_VULN:
				case CANCEL_VULN:
				case DERANGEMENT_VULN:
				case DEBUFF_VULN:
				case BUFF_VULN:
				case CRIT_VULN:
//				case CRIT_ADD_VULN:
				case MAGIC_DAMAGE_VULN:
				{
					if(ammount > 0)
						power = ("-" + (int)((double)ammount / 4) + "%");
					else
						power = ("+" + (int)((double)ammount / 4)*-1 + "%");
					break;
				}
				default:
				{
					if(ammount > 0)
						power = ("-" + ammount);
					else
						power = ("+" + (ammount*-1));
				}
			}
		}
		
		if(f.pFunc.equals("Set"))
		{
			int ammount = (int) f.lambda.calc(null);
			
			if(onlyNumber)
				power = (String.valueOf(ammount));
			else
				power = ("set on " + ammount);
		}
		
		if(f.pFunc.equals("Mul"))
		{
			double ammount = f.lambda.calc(null);
			
			if(ammount == 1)
				return null;
			
			DecimalFormat twoDForm = new DecimalFormat("0.0000");
			
			if(ammount > 1)
			{
				ammount = ammount - 1;
				ammount = Double.valueOf(twoDForm.format(ammount));
				
				ammount *= 100;
				
				int decimals = 6;
				
				String d = String.valueOf((int)Math.round(ammount)).substring(0, Math.min(decimals, String.valueOf((int)Math.round(ammount)).length()));
				
				if(d.equals("0"))
					return null;
				else
					power = ("+" + d + "%");
			}
			else
			{
				ammount = Math.abs(ammount - 1);
				ammount = Double.valueOf(twoDForm.format(ammount));
				
				ammount *= 100;
				
				int decimals = 3;
				
				String d = String.valueOf((int)Math.round(ammount)).substring(0, Math.min(decimals, String.valueOf((int)Math.round(ammount)).length()));
				
				if(d.equals("0"))
					return null;
				
				power = ("-" + d + "%");
			}
		}
		
		return power;
	}
	
	public static String writeBonuses(FuncTemplate[] funcs, boolean includeSet)
	{
		Locale.setDefault(Locale.US);
		
		TextBuilder sb = new TextBuilder();
		
		if(funcs == null)
		{
			return sb.toString();
		}
		
		int i = 0;
		for(FuncTemplate f : funcs)
		{
			boolean addDot = false;
			if(i+1 < funcs.length)
				addDot = true;
			
			i++;
			
			if(f.stat != null)
			{
				if(f.pFunc.equals("Enchant"))
					continue;
				
				if(!includeSet && f.pFunc.equals("Set"))
					continue;
				
				Stats stat = f.stat;
				String desc = stat.getHtmlDesc();
				
				TextBuilder add = new TextBuilder();
				add.append(desc + " ");
				String power = getPower(f, false);
				if(power == null)
					continue;
				add.append(power);
				
				if(addDot)
					add.append(", ");
				
				sb.append(add.toString());
			}
		}
		
		return sb.toString();
	}
	
	@SuppressWarnings("unused")
	private static double round(double d, double places)
	{
		int temp=(int)((d*Math.pow(10,d)));
		return (((double)temp)/Math.pow(10,d));
	}
	
	public static String createItemDescription(int itemId)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		
		if(item == null)
			return "Wrong item Id";
		
		FuncTemplate[] funcs = item.getFuncTemplates();
		
		String result = writeBonuses(funcs, false);
		
		if(result.endsWith(", "))
			result = result.substring(0, result.length() - 2);
		
		if(item.getItemSkills() != null)
		{
			for(L2Skill sk : item.getItemSkills())
			{
				result = result + ". " + createSkill(sk.getId(), sk.getLevel());
			}
		}
		
		L2Armor armor = null;
		
		if(item instanceof L2Armor)
		{
			armor = (L2Armor) item;
			L2ArmorSet set = ArmorSetsTable.getInstance().getSet(armor.getItemId());
			
			if(set != null)
			{
				int skillId, skillLvl;
				final String[] skills = set.getSkills();
				
				if (skills != null)
				{
					for (String skillInfo : skills)
					{
						skillId = 0;
						skillLvl = 0;
						String[] skill = skillInfo.split("-");
						if (skill != null && skill.length == 2)
						{
							try
							{
								skillId = Integer.parseInt(skill[0]);
								skillLvl = Integer.parseInt(skill[1]);
							}
							catch (NumberFormatException e)
							{
								e.printStackTrace();
							}
							
							L2Skill sk = SkillTable.getInstance().getInfo(skillId, skillLvl);
							if(sk == null)
								continue;
							
							String s = createSkill(sk.getId(), sk.getLevel());
							
							if(s.length() > 0)
								result = result + ". Set: " + s;
						}
					}
				}
			}
		}
		
		if(result.endsWith(", "))
			result = result.substring(0, result.length() - 2) + ".";
		
		if(!result.endsWith("."))
			result = result + ".";
		
		return result;
	}
}
