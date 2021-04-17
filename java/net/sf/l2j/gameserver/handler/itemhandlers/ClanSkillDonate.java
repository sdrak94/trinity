package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class ClanSkillDonate implements IItemHandler
{
	public static final int	CLAN_SKILLS[]	=
	{
		370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 397, 398, 399
	};
	int						_skillLvl;
	
	@Override
	public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse)
	{
		final L2PcInstance activeChar = (L2PcInstance) playable;
		if (activeChar.getClan() == null)
		{
			activeChar.sendMessage("You must be in a clan to use it");
			return;
		}
		int itemId = item.getItemId();
		switch (itemId)
		{
			case 600051:
				_skillLvl = 1;
				break;
			case 600052:
				_skillLvl = 2;
				break;
			case 600053:
				_skillLvl = 3;
				break;
		}
		final L2Skill[] list = activeChar.getClan().getAllSkills();
		for (int skillId : CLAN_SKILLS)
		{
			boolean f = false;
			for (L2Skill sk : list)
			{
				if (sk.getId() != skillId)
				{
					continue;
				}
				if (sk.getLevel() >= _skillLvl)
				{
					f = true;
					break;
				}
			}
			if (f)
			{
				// plr.sendMessage("ignore " + i);
				continue;
			}
			// check here if clan got already the skill on higher level
			final L2Skill skill = SkillTable.getInstance().getInfo(skillId, _skillLvl);
			String skillname = skill.getName();
			SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED);
			sm.addSkillName(skill);
			activeChar.sendPacket(sm);
			activeChar.getClan().broadcastToOnlineMembers(sm);
			activeChar.getClan().addNewSkill(skill);
			for (L2PcInstance member : activeChar.getClan().getOnlineMembers(0))
			{
				String text = ": " + activeChar.getName() + " > " + skillname + " Lvl." + String.valueOf(_skillLvl) + " to the clan.";
				CreatureSay cs = new CreatureSay(0, Say2.CLAN, activeChar.getClan().getName(), text);
				member.sendPacket(cs);
			}
		}
		for (L2PcInstance member : activeChar.getClan().getOnlineMembers(0))
		{
			member.sendSkillList();
		}
		activeChar.destroyItemByItemId("Donate Clan", item.getItemId(), 1, activeChar, true);
	}
}
