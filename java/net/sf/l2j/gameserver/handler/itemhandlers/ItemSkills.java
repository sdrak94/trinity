package net.sf.l2j.gameserver.handler.itemhandlers;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance.TimeStamp;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;

public class ItemSkills implements IItemHandler
{
	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
{
	L2PcInstance activeChar; // use activeChar only for L2PcInstance checks where cannot be used			 // PetInstance
	boolean isPet = playable instanceof L2PetInstance;
	if (playable instanceof L2PcInstance)
		activeChar = (L2PcInstance) playable;
	else if (isPet)
		activeChar = ((L2PetInstance) playable).getOwner();
	else
		return;
	if (activeChar.isInOlympiadMode())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
		return;
	}
	if (!TvTEvent.onScrollUse(playable.getObjectId()))
	{
		playable.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	int skillId;
	int skillLvl;
	
	final String[] skills = item.getEtcItem().getSkills();
	if (skills != null)
	{
		for (String skillInfo : skills)
		{
			String[] skill = skillInfo.split("-");
			if (skill != null && skill.length == 2)
			{
				skillId = Integer.parseInt(skill[0]);
				skillLvl = Integer.parseInt(skill[1]);
				if (skillId > 0 && skillLvl > 0)
				{
					L2Skill itemSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);
					if (itemSkill != null)
					{
						if (!itemSkill.checkCondition(playable, playable.getTarget(), false))
							return;
						if (playable.isSkillDisabled(skillId))
						{
							reuse(activeChar, itemSkill);
							return;
						}
						
						if (playable.getActingPlayer().isInHuntersVillage() || playable.getActingPlayer().isInOrcVillage())
						{
							if (itemSkill.getTransformId() > 0)
							{
								playable.sendMessage("You can't transform in this zone");
								return;
							}
						}
						else if (playable.getActingPlayer().isInHellbound())
						{
							if (itemSkill.getTransformId() > 0)
							{
								playable.sendMessage("You can't transform in Hellbound");
								return;
							}
						}
						
						// pets can use items only when they are tradeable
						if (isPet && !item.isTradeable())
							activeChar.sendPacket(new SystemMessage(SystemMessageId.ITEM_NOT_FOR_PETS));
						else
						{
							// send message to owner
							if (isPet)
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.PET_USES_S1);
								sm.addString(itemSkill.getName());
								activeChar.sendPacket(sm);
							}
							else
							{
								switch (skillId)
								{
								// short buff icon for healing potions
								case 2031:
								case 2032:
								case 2037:
								case 26025:
								case 26026:
									int buffId = activeChar._shortBuffTaskSkillId;
									// greater healing potions
									if (skillId == 2037 || skillId == 26025)
										activeChar.shortBuffStatusUpdate(skillId, skillLvl, itemSkill.getBuffDuration() / 1000);
									// healing potions
									else if ((skillId == 2032 || skillId == 26026)
											&& buffId != 2037 && buffId != 26025)
										activeChar.shortBuffStatusUpdate(skillId, skillLvl, itemSkill.getBuffDuration() / 1000);
									// lesser healing potions
									else
									{
										if (buffId != 2037 && buffId != 26025
												&& buffId != 2032 && buffId != 26026)
											activeChar.shortBuffStatusUpdate(skillId, skillLvl, itemSkill.getBuffDuration() / 1000);
									}
									break;
								}
							}
							if (itemSkill.isPotion())
							{
								playable.doSimultaneousCast(itemSkill);
								// Summons should be affected by herbs too, self time effect is
								// handled at L2Effect constructor
								if (!isPet && item.getItemType() == L2EtcItemType.HERB
										&& activeChar.getPet() != null
										&& activeChar.getPet() instanceof L2SummonInstance)
									activeChar.getPet().doSimultaneousCast(itemSkill);
							}
							else
							{
								if (playable.isCastingNow())
									return;
								
								playable.stopMove(null);
								playable.doCast(itemSkill);
							}
							/*								if (itemSkill.getReuseDelay(activeChar) > 0)
									activeChar.addTimeStamp(skillId, itemSkill.getReuseDelay(activeChar));*/
						}
					}
				}
			}
		}
	}
}

private void reuse(L2PcInstance player, L2Skill skill)
{
	if (skill.isPotion())
		return;
	
	SystemMessage sm = null;
	FastMap<Integer, TimeStamp> timeStamp = player.getReuseTimeStamp();
	
	if (timeStamp != null && timeStamp.containsKey(skill.getId()))
	{
		int remainingTime = (int) (player.getReuseTimeStamp().get(skill.getId()).getRemaining() / 1000);
		int hours = remainingTime / 3600;
		int minutes = (remainingTime % 3600) / 60;
		int seconds = (remainingTime % 60);
		if (hours > 0)
		{
			sm = new SystemMessage(SystemMessageId.S2_HOURS_S3_MINUTES_S4_SECONDS_REMAINING_FOR_REUSE_S1);
			sm.addSkillName(skill);
			sm.addNumber(hours);
			sm.addNumber(minutes);
		}
		else if (minutes > 0)
		{
			sm = new SystemMessage(SystemMessageId.S2_MINUTES_S3_SECONDS_REMAINING_FOR_REUSE_S1);
			sm.addSkillName(skill);
			sm.addNumber(minutes);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.S2_SECONDS_REMAINING_FOR_REUSE_S1);
			sm.addSkillName(skill);
		}
		sm.addNumber(seconds);
	}
	else
	{
		sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
		sm.addSkillName(skill);
	}
	player.sendPacket(sm);
}
}