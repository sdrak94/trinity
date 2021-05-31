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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class QuestStateManager
{
	protected static final Logger _log = Logger.getLogger(QuestStateManager.class.getName());
	
	// =========================================================
	// Schedule Task
	public class ScheduleTimerTask implements Runnable
	{
		public void run()
		{
			try
			{
				cleanUp();
				ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), 60000);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	// =========================================================
	// Data Field
	private List<QuestState> _questStates = new FastList<QuestState>();
	
	// =========================================================
	// Constructor
	private QuestStateManager()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), 60000);
	}
	
	// =========================================================
	// Method - Public
	/**
	 * Add QuestState for the specified player instance
	 */
	public void addQuestState(Quest quest, L2PcInstance player, byte state)
	{
		QuestState qs = getQuestState(player);
		if (qs == null)
			qs = new QuestState(player, quest, state);
	}
	
	/**
	 * Remove all QuestState for all player instance that does not exist
	 */
	public void cleanUp()
	{
		for (int i = getQuestStates().size() - 1; i >= 0; i--)
		{
			if (getQuestStates().get(i).getPlayer() == null)
			{
				removeQuestState(getQuestStates().get(i));
				getQuestStates().remove(i);
			}
		}
	}
	
	// =========================================================
	// Method - Private
	/**
	 * Remove QuestState instance
	 */
	private void removeQuestState(QuestState qs)
	{
		qs = null;
	}
	
	// =========================================================
	// Property - Public
	public static final QuestStateManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * Return QuestState for specified player instance
	 */
	public QuestState getQuestState(L2PcInstance player)
	{
		for (QuestState q : getQuestStates())
		{
			if (q.getPlayer() != null && q.getPlayer().getObjectId() == player.getObjectId())
				return q;
		}
		
		return null;
	}
	
	/**
	 * Return all QuestState
	 */
	public List<QuestState> getQuestStates()
	{
		if (_questStates == null)
			_questStates = new FastList<QuestState>();
		return _questStates;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final QuestStateManager _instance = new QuestStateManager();
	}
}
