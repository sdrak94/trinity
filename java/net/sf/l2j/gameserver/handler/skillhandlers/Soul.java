package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class Soul implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.CHARGESOUL
	};

	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (!(activeChar instanceof L2PcInstance) || activeChar.isAlikeDead())
			return;
		
		L2PcInstance player = (L2PcInstance) activeChar;
		
		L2Skill soulmastery = SkillTable.getInstance().getInfo(467, player.getSkillLevel(467));
		
		if (soulmastery != null)
		{
			final int totalMaxSouls = (int) (activeChar.calcStat(Stats.SOUL_MAX, soulmastery.getNumSouls(), null, null));

			if (player.getSouls() < totalMaxSouls)
			{
				int count = 0;
				
				if (player.getSouls() + skill.getNumSouls() <= totalMaxSouls)
					count = skill.getNumSouls();
				else
					count = totalMaxSouls - player.getSouls();
				
				if (count > 0)
					player.increaseSouls(count);
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.SOUL_CANNOT_BE_INCREASED_ANYMORE);
				player.sendPacket(sm);
				return;
			}
		}
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.ISkillHandler#getSkillIds()
	 */
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}