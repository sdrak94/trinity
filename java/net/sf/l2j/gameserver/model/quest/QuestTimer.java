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
package net.sf.l2j.gameserver.model.quest;

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class QuestTimer
{
	protected static final Logger _log = Logger.getLogger(QuestTimer.class.getName());
	private final Quest _quest;
	private final String _name;
	private final L2Npc _npc;
	private final L2PcInstance _player;
	private ScheduledFuture<?> _schedular;
	private final boolean _isRepeating;
	private boolean _isActive = true;
	
	QuestTimer(final Quest quest, final String name, final L2Npc npc, final L2PcInstance player, final long time, final boolean repeating)
	{
		_quest = quest;
		_name = name;
		_npc = npc;
		_player = player;
		_isRepeating = repeating;
		if (repeating)
			_schedular = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ScheduleTimerTask(), time, time);
		else
			_schedular = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), time);
	}
	
	final Quest getQuest()
	{
		return _quest;
	}
	
	final String getName()
	{
		return _name;
	}
	
	final L2Npc getNpc()
	{
		return _npc;
	}
	
	final L2PcInstance getPlayer()
	{
		return _player;
	}
	
	final boolean getIsRepeating()
	{
		return _isRepeating;
	}
	
	final boolean getIsActive()
	{
		return _isActive;
	}
	
	@Override
	public final String toString()
	{
		return _name;
	}
	
	final class ScheduleTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!getIsActive())
				return;
			try
			{
				if (!getIsRepeating())
					cancel();
				getQuest().notifyEvent(getName(), getNpc(), getPlayer());
			}
			catch (final Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	public final void cancel()
	{
		_isActive = false;
		if (_schedular != null)
			_schedular.cancel(false);
		_quest.removeQuestTimer(this);
	}
	
	/**
	 * public method to compare if this timer matches with the key attributes passed.
	 * @param quest : Quest instance to which the timer is attached
	 * @param name : Name of the timer
	 * @param npc : Npc instance attached to the desired timer (null if no npc attached)
	 * @param player : Player instance attached to the desired timer (null if no player attached)
	 * @return boolean
	 */
	final boolean equals(final Quest quest, final String name, final L2Npc npc, final L2PcInstance player)
	{
		if (quest == null || quest != _quest)
			return false;
		if (name == null || !name.equals(_name))
			return false;
		return npc == _npc && player == _player;
	}
}