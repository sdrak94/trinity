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
package net.sf.l2j.gameserver.ai2;

import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;

/**
 *
 * @author -Wooden-
 *
 */
public class AiParameters
{
	private Queue<AiEvent> _eventQueue;
	private EnumSet<AiEventType> _inhibitions;
	private L2Npc _actor;
	private List<Hated> _hated;
	private List<Liked> _liked;
	
	public class Hated
	{
		public L2Character character;
		public HateReason reason;
		public int degree;
		
	}
	
	public class Liked
	{
		public L2Character character;
		public LikeReason reason;
		public int degree;
	}
	
	public enum HateReason
	{
		GAVE_DAMMAGE,
		HEALS_ENNEMY,
		GAVE_DAMMAGE_TO_FRIEND,
		IS_ENNEMY
	}
	
	public enum LikeReason
	{
		FRIEND,
		HEALED,
		HEALED_FRIEND,
		GAVE_DAMMAGE_TO_ENNEMY
	}
	
	public AiParameters(L2Npc actor)
	{
		_eventQueue = new PriorityBlockingQueue<AiEvent>();
		_hated = new FastList<Hated>();
		_liked = new FastList<Liked>();
		_actor = actor;
		_inhibitions = EnumSet.noneOf(AiEventType.class);
	}
	
	/**
	 * @return
	 */
	public boolean hasEvents()
	{
		return _eventQueue.isEmpty();
	}
	
	/**
	 * @return
	 */
	public AiEvent nextEvent()
	{
		return _eventQueue.poll();
	}
	
	public void queueEvents(AiEvent set)
	{
		_eventQueue.offer(set);
	}
	
	public L2Npc getActor()
	{
		return _actor;
	}
	
	public List<Hated> getHated()
	{
		return _hated;
	}
	
	public List<Liked> getLiked()
	{
		return _liked;
	}
	
	public void addLiked(Liked cha)
	{
		_liked.add(cha);
	}
	
	public void addHated(Hated cha)
	{
		_hated.add(cha);
	}
	
	public void clear()
	{
		_hated.clear();
		_liked.clear();
		_eventQueue.clear();
		_inhibitions.clear();
	}
	
	public void inhibit(AiEventType type)
	{
		_inhibitions.add(type);
	}
	
	public void deInhibit(AiEventType type)
	{
		_inhibitions.remove(type);
	}
	
	/**
	 * @param type
	 * @return
	 */
	public boolean isEventInhibited(AiEventType type)
	{
		return _inhibitions.contains(type);
	}
	
}
