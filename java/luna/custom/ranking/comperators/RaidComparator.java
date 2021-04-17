package luna.custom.ranking.comperators;

import java.util.Comparator;

import luna.custom.ranking.TopData;

public class RaidComparator implements Comparator<TopData>
{
	@Override
	public int compare(TopData o1, TopData o2)
	{
		return o2.getRaidPoints() - o1.getRaidPoints();
	}
}