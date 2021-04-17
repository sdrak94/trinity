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
package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;

public class ConditionTargetActiveEffectId extends Condition
{
    
    public final int _effectId;
    public final int _effectLvl;
    
    public ConditionTargetActiveEffectId(int effectId)
    {
        _effectId = effectId;
        _effectLvl = -1;
    }
    
    public ConditionTargetActiveEffectId(int effectId, int effectLevel)
    {
    	_effectId = effectId;
    	_effectLvl = effectLevel;
    }
    
    @Override
    public boolean testImpl(Env env)
    {
        for (L2Effect e : env.target.getAllEffects())
        {
            if (e != null)
            {
                if (e.getSkill().getId() == _effectId)
                {
                	if (_effectLvl == -1 || _effectLvl <= e.getSkill().getLevel())
                		return true;
                }
            }
        }
        return false;
    }
}
