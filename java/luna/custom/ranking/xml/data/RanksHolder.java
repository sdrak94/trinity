package luna.custom.ranking.xml.data;

import java.util.ArrayDeque;

import luna.custom.data.xml.AbstractHolder;


public final class RanksHolder extends AbstractHolder
{
	private static final RanksHolder _instance = new RanksHolder();
	
	public static RanksHolder getInstance()
	{
		return _instance;
	}
	
	private ArrayDeque<RankData> _ranks = new ArrayDeque<>();
	
	public void addRank(RankData _name)
	{
		_ranks.add(_name);
	}
	
	public ArrayDeque<RankData> getRanks()
	{
		return _ranks;
	}
	
	public int size()
	{
		return _ranks.size();
	}
	
	public void clear()
	{
		_ranks.clear();
	}

	public RankData getDefault()
	{
		return _ranks.getLast();
	}
}