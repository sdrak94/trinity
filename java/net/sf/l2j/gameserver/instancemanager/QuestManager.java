/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.instancemanager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import luna.custom.loader.Loader;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.scripting.ScriptManager;

public class QuestManager extends ScriptManager<Quest>
{
	protected static final Logger	_log	= Logger.getLogger(QuestManager.class.getName());
	private final List<Quest>		_quests	= new ArrayList<>();
	
	public static final QuestManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public void loadQuests()
	{
		report();
	}
	
	public final void report()
	{
		_log.info("QuestManager: Loaded " + _quests.size() + " quests.");
	}
	
	public final void save()
	{
		for (final Quest q : _quests)
			q.saveGlobalData();
	}
	
	// FIXME: ADD & GET & REMOVE
	/**
	 * Add new quest to quest list. Reloads the quest, if exists.
	 * 
	 * @param quest
	 *            : Quest to be add.
	 */
	public final void addQuest(final Quest quest)
	{
		// Quest does not exist, return.
		if (quest == null)
			return;
		// Quest already loaded, unload id.
		final Quest old = getQuest(quest.getQuestId());
		if (old != null && old.isRealQuest())
		{
			old.unload();
			_log.info("QuestManager: Replaced: (" + old.getName() + ") with a new version (" + quest.getName() + ").");
		}
		// Add new quest.
		_quests.add(quest);
	}
	
	/**
	 * Removes the quest from the list.
	 * 
	 * @param quest
	 *            : Quest to be removed.
	 * @return boolean : True if removed sucessfully, false otherwise.
	 */
	public final boolean removeQuest(final Quest quest)
	{
		return _quests.remove(quest);
	}
	
	/**
	 * Returns the quest by given quest name.
	 * 
	 * @param questName
	 *            : The name of the quest.
	 * @return Quest : Quest to be returned, null if quest does not exist.
	 */
	public final Quest getQuest(final String questName)
	{
		// Check all quests.
		for (final Quest q : _quests)
			// If quest found, return him.
			if (q.getName().equals(questName))
				return q;
		// Otherwise return null.
		return null;
	}
	
	/**
	 * Returns the quest by given quest id.
	 * 
	 * @param questId
	 *            : The id of the quest.
	 * @return Quest : Quest to be returned, null if quest does not exist.
	 */
	public final Quest getQuest(final int questId)
	{
		// Check all quests.
		for (final Quest q : _quests)
			// If quest found, return him.
			if (q.getQuestId() == questId)
				return q;
		// Otherwise return null.
		return null;
	}
	
	// FIXME: MODIFIY QUEST
	/**
	 * Reloads the quest given by quest id.
	 * 
	 * @param questId
	 *            : The id of the quest to be reloaded.
	 * @return boolean : True if reload was successful, false otherwise.
	 */
	public final boolean reload(final int questId)
	{
		// Get quest by questId.
		final Quest q = getQuest(questId);
		// Quest does not exist, return.
		if (q == null)
			return false;
		// Reload the quest.
		return q.reload();
	}
	
	/**
	 * Reloads all quests. Simply reloads all quests according to the scripts.cfg.
	 */
	public final void reloadAllQuests()
	{
		_log.info("QuestManager: Reloading scripts.");
		for (Quest quest : this._quests)
		{
			if (quest != null)
				quest.unload(false);
		}
		this._quests.clear();
		Loader.getInstance().loadQuests();
		report();
	}
	
	// FIXME: SCRIPT MANAGER
	/**
	 * @see l2.ae.pvp.gameserver.scripting.ScriptManager#getAllManagedScripts()
	 */
	@Override
	public List<Quest> getAllManagedScripts()
	{
		return _quests;
	}
	
	/**
	 * @see l2.ae.pvp.gameserver.scripting.ScriptManager#unload(l2.ae.pvp.gameserver.scripting.ManagedScript)
	 */
	@Override
	public boolean unload(final Quest quest)
	{
		quest.saveGlobalData();
		return removeQuest(quest);
	}
	
	/**
	 * @see l2.ae.pvp.gameserver.scripting.ScriptManager#getScriptManagerName()
	 */
	@Override
	public String getScriptManagerName()
	{
		return "QuestManager";
	}
	
	private static class SingletonHolder
	{
		protected static final QuestManager _instance = new QuestManager();
	}
}