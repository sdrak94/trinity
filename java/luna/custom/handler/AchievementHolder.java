package luna.custom.handler;

import java.util.HashMap;
import java.util.Map;

public class AchievementHolder
{
	public final Map<Integer, Integer> _achievementMaxLevels;
	
	public static AchievementHolder getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final AchievementHolder _instance = new AchievementHolder();
	}
	
	public AchievementHolder()
	{
		_achievementMaxLevels = new HashMap<>();
	}
	
	public void addLevel(int achievementId, int achievementMaxLevel)
	{
		_achievementMaxLevels.put(achievementId, achievementMaxLevel);
	}
	
	public int getMaxLevel(int id)
	{
		int maxLevel;
		maxLevel = _achievementMaxLevels.get(id);
		return maxLevel;
	}
}
