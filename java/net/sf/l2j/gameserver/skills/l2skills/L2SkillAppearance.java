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

import java.util.logging.Level;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillAppearance extends L2Skill 
{
	private final int _faceId;
	private final int _hairColorId;
	private final int _hairStyleId;

    public L2SkillAppearance(StatsSet set)
	{
		super(set);

		_faceId = set.getInteger("faceId", -1);
		_hairColorId = set.getInteger("hairColorId", -1);
		_hairStyleId = set.getInteger("hairStyleId", -1);
	}

	@Override
	public void useSkill(L2Character caster, L2Object[] targets)
	{
		try
		{
			for (L2Object target : targets)
			{
				if (target instanceof L2PcInstance)
				{
					L2PcInstance targetPlayer = (L2PcInstance)target;
					if (_faceId >= 0)
						targetPlayer.getAppearance().setFace(_faceId);
					if (_hairColorId >= 0)
						targetPlayer.getAppearance().setHairColor(_hairColorId);
					if (_hairStyleId >= 0)
						targetPlayer.getAppearance().setHairStyle(_hairStyleId);

					targetPlayer.store();
					targetPlayer.broadcastUserInfo();
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
	}
}