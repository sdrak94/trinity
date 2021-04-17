package net.sf.l2j.gameserver.instancemanager;

import java.util.HashMap;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.LifeStone;

public class LifeStoneManager 
	{
		private final HashMap<Integer, LifeStone> _lifeStones = new HashMap<>();
		
		public LifeStoneManager()
		{
			_lifeStones.put(8732, new LifeStone(0, true));
			_lifeStones.put(8742, new LifeStone(1, true));
			_lifeStones.put(8752, new LifeStone(2, true));
			_lifeStones.put(8762, new LifeStone(3, true));
		}
		
		public LifeStone getLifeStone(L2ItemInstance item)
		{
			return _lifeStones.get(item.getItemId());
		}
		
		private static class InstanceHolder
		{
			private static final LifeStoneManager _instance = new LifeStoneManager();
		}
		
		public boolean isLifeStone(final L2ItemInstance item)
		{
			return _lifeStones.containsKey(item.getItemId());
		}
		
		public static LifeStoneManager getInstance()
		{
			return InstanceHolder._instance;
		}
	}
