package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

public class RequestDispel extends L2GameClientPacket
{

private int _skillId;
private int _skillLevel;

/**
 * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#getType()
 */
@Override
public String getType()
{
	return "[C] D0:4E RequestDispel";
}

/**
 * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#readImpl()
 */
@Override
protected void readImpl()
{
	_skillId = readD();
	_skillLevel = readD();
}

/**
 * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#runImpl()
 */
@Override
protected void runImpl()
{
	final L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar != null)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLevel);
		
		if (skill != null && !skill.isDebuff() && skill.canBeDispeled())
		{
			for (L2Effect e : activeChar.getAllEffects())
			{
				if (e != null && e.getSkill().getId() == skill.getId() && e.getEffectType() != L2EffectType.TRANSFORMATION)
				{
					e.exit();
					
					if (skill.getNegateId().length != 0)
					{
						for (int i = 0; i < skill.getNegateId().length; i++)
						{
							if (skill.getNegateId()[i] != 0)
								activeChar.stopSkillEffects(skill.getNegateId()[i]);
						}
					}
				}
			}
		}
	}
}
}
