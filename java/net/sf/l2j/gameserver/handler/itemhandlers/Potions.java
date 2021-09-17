package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class Potions implements IItemHandler
{
	public synchronized void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		L2PcInstance activeChar; // use activeChar only for L2PcInstance checks where cannot be used PetInstance
		boolean res = false;
		if (playable instanceof L2PcInstance)
			activeChar = (L2PcInstance) playable;
		else if (playable instanceof L2PetInstance)
			activeChar = ((L2PetInstance) playable).getOwner();
		else
			return;
		if (!TvTEvent.onPotionUse(playable.getObjectId()) || playable.isAllSkillsDisabled())
		{
			playable.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		// if (activeChar.getCurrentInstance().getPotionRestriction() == 1)
		// {
		// activeChar.sendMessage("Potions are restricted here");
		// playable.sendPacket(ActionFailed.STATIC_PACKET);
		// return;
		// }
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}
		int itemId = item.getItemId();
		switch (itemId)
		{
			case 726: // Custom Mana Drug, xml: 9007
				if (Config.L2JMOD_ENABLE_MANA_POTIONS_SUPPORT)
					usePotion(activeChar, 9007, 1);
				else
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
				break;
			case 728: // Custom Mana Potion, xml: 9008
				/* usePotion(activeChar, 9008, 1); */
				break;
			case 727: // Healing_potion, xml: 2032
			case 1061:
				if (!isEffectReplaceable(playable, L2EffectType.HEAL_OVER_TIME, itemId))
					return;
				res = usePotion(playable, 2032, 1);
				break;
			case 1060: // Lesser Healing Potion
			case 1073: // Beginner's Potion, xml: 2031
				if (!isEffectReplaceable(activeChar, L2EffectType.HEAL_OVER_TIME, itemId))
					return;
				res = usePotion(playable, 2031, 1);
				break;
			case 10409: // Empty Bottle of Souls
				if (activeChar.getActiveClass() >= 123 && activeChar.getActiveClass() <= 136) // Kamael classes only
				{
					if (activeChar.getSouls() >= 6)
						res = usePotion(activeChar, 2498, 1);
					else
						playable.sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NOT_ENOUGH_SOUL));
				}
				else
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
				break;
			case 10410: // 5 Souls Bottle
			case 10411:
				if (activeChar.getActiveClass() >= 123 && activeChar.getActiveClass() <= 136) // Kamael classes only
					res = usePotion(activeChar, 2499, 1);
				else
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
				break;
			case 20393: // Sweet Fruit Cocktail
				res = usePotion(playable, 22056, 1);
				usePotion(playable, 22057, 1);
				usePotion(playable, 22058, 1);
				usePotion(playable, 22059, 1);
				usePotion(playable, 22060, 1);
				usePotion(playable, 22061, 1);
				usePotion(playable, 22064, 1);
				usePotion(playable, 22065, 1);
				break;
			case 20394: // Fresh Fruit Cocktail
				res = usePotion(playable, 22062, 1);
				usePotion(playable, 22063, 1);
				usePotion(playable, 22065, 1);
				usePotion(playable, 22066, 1);
				usePotion(playable, 22067, 1);
				usePotion(playable, 22068, 1);
				usePotion(playable, 22069, 1);
				usePotion(playable, 22070, 1);
				break;
			case 4416:
			case 7061:
				res = usePotion(playable, 2073, 1);
				break;
			case 8515:
			case 8516:
			case 8517:
			case 8518:
			case 8519:
			case 8520:
				res = usePotion(playable, 5041, 1);
				break;
			case 10143:
				res = usePotion(playable, 2379, 1);
				usePotion(playable, 2380, 1);
				usePotion(playable, 2381, 1);
				usePotion(playable, 2382, 1);
				usePotion(playable, 2383, 1);
				break;
			case 10144:
				res = usePotion(playable, 2379, 1);
				usePotion(playable, 2380, 1);
				usePotion(playable, 2381, 1);
				usePotion(playable, 2384, 1);
				usePotion(playable, 2385, 1);
				break;
			case 10145:
				res = usePotion(playable, 2379, 1);
				usePotion(playable, 2380, 1);
				usePotion(playable, 2381, 1);
				usePotion(playable, 2384, 1);
				usePotion(playable, 2386, 1);
				break;
			case 10146:
				res = usePotion(playable, 2379, 1);
				usePotion(playable, 2387, 1);
				usePotion(playable, 2381, 1);
				usePotion(playable, 2388, 1);
				usePotion(playable, 2383, 1);
				break;
			case 10147:
				res = usePotion(playable, 2379, 1);
				usePotion(playable, 2387, 1);
				usePotion(playable, 2381, 1);
				usePotion(playable, 2383, 1);
				usePotion(playable, 2389, 1);
				break;
			case 10148:
				res = usePotion(playable, 2390, 1);
				usePotion(playable, 2391, 1);
				break;
			default:
		}
		if (itemId != 1539 && itemId != 1831 && itemId != 1060 && itemId != 1061 && itemId != 1832 && itemId != 1833 && itemId != 1834 && itemId != 727 && itemId != 728)
		{
			if (res)
				playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
	}
	
	/**
	 * @param activeChar
	 * @param effectType
	 * @param itemId
	 * @return
	 */
	private boolean isEffectReplaceable(L2Playable playable, L2EffectType effectType, int itemId)
	{
		L2Effect e = playable.getFirstEffect(effectType);
		if (e == null)
			return true;
		// One can reuse pots after 2/3 of their duration is over.
		// It would be faster to check if its > 10 but that would screw custom pot durations...
		if (e.getTaskTime() > (e.getSkill().getBuffDuration() * 67) / 100000)
			return true;
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
		sm.addItemName(itemId);
		playable.sendPacket(sm);
		return false;
	}
	
	/**
	 * @param activeChar
	 * @param magicId
	 * @param level
	 * @return
	 */
	public boolean usePotion(L2Playable activeChar, int magicId, int level)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(magicId, level);
		if (skill != null)
		{
			if (!skill.checkCondition(activeChar, activeChar, false))
				return false;
			// Return false if potion is in reuse so it is not destroyed from inventory
			if (activeChar.isSkillDisabled(skill.getId()))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
				sm.addSkillName(skill);
				activeChar.sendPacket(sm);
				return false;
			}
			if (skill.isPotion())
				activeChar.doSimultaneousCast(skill);
			else
				activeChar.doCast(skill);
			if (activeChar instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) activeChar;
				// Only for Heal potions
				if (magicId == 2031 || magicId == 2032 || magicId == 2037)
					player.shortBuffStatusUpdate(magicId, level, 15);
				if (!(player.isSitting() && !skill.isPotion()))
					return true;
			}
			else if (activeChar instanceof L2PetInstance)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.PET_USES_S1);
				sm.addString(skill.getName());
				((L2PetInstance) (activeChar)).getOwner().sendPacket(sm);
				return true;
			}
		}
		return false;
	}
}
