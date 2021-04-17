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
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillLearnSkill extends L2Skill
{
public final int[] _learnSkillId;
public final int[] _learnSkillLvl;

public L2SkillLearnSkill(StatsSet set)
{
	super(set);
	
	String[] ar = set.getString("learnSkillId", "0").split(",");
	int[] ar2 = new int[ar.length];
	
	for (int i = 0; i < ar.length; i++)
		ar2[i] = Integer.parseInt(ar[i]);
	
	_learnSkillId = ar2;
	
	ar = set.getString("learnSkillLvl", "1").split(",");
	ar2 = new int[_learnSkillId.length];
	
	for (int i = 0; i < _learnSkillId.length; i++)
		ar2[i] = 1;
	
	for (int i = 0; i < ar.length; i++)
		ar2[i] = Integer.parseInt(ar[i]);
	
	_learnSkillLvl = ar2;
}

@Override
public void useSkill(L2Character activeChar, L2Object[] targets)
{
	if (!(activeChar instanceof L2PcInstance))
		return;
	
	final L2PcInstance player = ((L2PcInstance)activeChar);
	L2Skill newSkill;
	
	for (int i = 0; i < _learnSkillId.length; i++)
	{
		if (_learnSkillId[i] != 0 && player.getSkillLevel(_learnSkillId[i]) < _learnSkillLvl[i])
		{
			newSkill = SkillTable.getInstance().getInfo(_learnSkillId[i], _learnSkillLvl[i]);
			if (newSkill != null)
			{
				player.addSkill(newSkill, true);
				player.sendSkillList();
			}
		}
	}
}
}