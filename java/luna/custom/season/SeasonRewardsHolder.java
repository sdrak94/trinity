/*package luna.custom.season;

import java.util.ArrayDeque;

import luna.custom.data.xml.AbstractHolder;
import luna.custom.ranking.xml.data.RankData;


public final class SeasonRewardsHolder extends AbstractHolder
{
	private static final SeasonRewardsHolder _instance = new SeasonRewardsHolder();
	
	public static SeasonRewardsHolder getInstance()
	{
		return _instance;
	}
	
	private ArrayDeque<SeasonRewardData> _rewards = new ArrayDeque<>();
	
	public void addReward(SeasonRewardData _rewards)
	{
		_rewards.add(_rewards);
	}
	

	public ArrayDeque<SeasonRewardData> getRewards()
	{
		return _rewards;
	}
	
	public int size()
	{
		return _rewards.size();
	}
	
	public void clear()
	{
		_rewards.clear();
	}

	public SeasonRewardData getDefault()
	{
		return _rewards.getLast();
	}
}*/