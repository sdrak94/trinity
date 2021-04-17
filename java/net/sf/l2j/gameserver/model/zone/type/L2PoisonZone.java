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
package net.sf.l2j.gameserver.model.zone.type;

import java.util.Collection;
import java.util.concurrent.Future;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.util.Rnd;

/**
 * another type of damage zone with skills
 *
 * @author  kerberos
 */
public class L2PoisonZone extends L2ZoneType
{
	private int _skillId;
	private int _chance;
	private int _initialDelay;
	private int _skillLvl;
	private int _reuse;
	private boolean _enabled;
	private String _target;
	private Future<?> _task;
	
	public L2PoisonZone(int id)
	{
		super(id);
		
		// Setup default skill
		_skillId = 4070;
		_skillLvl = 1;
		_chance = 100;
		_initialDelay = 0;
		_reuse = 30000;
		_enabled = true;
		_target = "pc";
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("skillId"))
		{
			_skillId = Integer.parseInt(value);
		}
		else if (name.equals("skillLvl"))
		{
			_skillLvl = Integer.parseInt(value);
		}
		else if (name.equals("chance"))
		{
			_chance = Integer.parseInt(value);
		}
		else if (name.equals("initialDelay"))
		{
			_initialDelay = Integer.parseInt(value);
		}
		else if (name.equals("default_enabled"))
		{
			_enabled = Boolean.parseBoolean(value);
		}
		else if (name.equals("target"))
		{
			_target = String.valueOf(value);
		}
		else if (name.equals("reuse"))
		{
			_reuse = Integer.parseInt(value);
		}
		else
			super.setParameter(name, value);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (((character instanceof L2Playable) && _target.equalsIgnoreCase("pc")) || ((character instanceof L2PcInstance) && _target.equalsIgnoreCase("pc_only"))
				|| ((character instanceof L2MonsterInstance) && _target.equalsIgnoreCase("npc")))
		{
			if (_task == null)
			{
				_task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ApplySkill(this), _initialDelay, _reuse);
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (_characterList.isEmpty() && _task != null)
		{
			_task.cancel(true);
			_task = null;
		}
	}
	
	public L2Skill getSkill()
	{
		return SkillTable.getInstance().getInfo(_skillId, _skillLvl);
	}
	
	public String getTargetType()
	{
		return _target;
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public int getChance()
	{
		return _chance;
	}
	
	public void setZoneEnabled(boolean val)
	{
		_enabled = val;
	}
	
	protected Collection<L2Character> getCharacterList()
	{
		return _characterList.values();
	}
	
	class ApplySkill implements Runnable
	{
		private L2PoisonZone _poisonZone;
		
		ApplySkill(L2PoisonZone zone)
		{
			_poisonZone = zone;
		}
		
		public void run()
		{
			if (isEnabled())
			{
				for (L2Character temp : _poisonZone.getCharacterList())
				{
					if (temp != null && !temp.isDead())
					{
						if (((temp instanceof L2Playable && getTargetType().equalsIgnoreCase("pc")) || (temp instanceof L2PcInstance && getTargetType().equalsIgnoreCase("pc_only")) || (temp instanceof L2MonsterInstance && getTargetType().equalsIgnoreCase("npc")))
								&& Rnd.get(100) < getChance())
						{
							getSkill().getEffects(temp, temp);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
	
}
