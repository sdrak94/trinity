package luna.museum;

import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;

public class MuseumReward
{
	String type;
	int id;
	int itemId;
	int minCount;
	int maxCount;
	double chance;
	
	public MuseumReward(final int _id, final String _type, final int _itemId, final int _minCount, final int _maxCount, final double _chance)
	{
		id = _id;
		type = _type;
		itemId = _itemId;
		minCount = _minCount;
		maxCount = _maxCount;
		chance = _chance;
	}
	
	public int getId()
	{
		return id;
	}
	
	public String getType()
	{
		return type;
	}
	
	public void setType(final String _type)
	{
		type = _type;
	}
	
	public int getItemId()
	{
		return itemId;
	}
	
	public void setItemId(final int _itemId)
	{
		itemId = _itemId;
	}
	
	public int getMinCount()
	{
		return minCount;
	}
	
	public void setMinCount(final int _minCount)
	{
		minCount = _minCount;
	}
	
	public int getMaxCount()
	{
		return maxCount;
	}
	
	public void setMaxCount(final int _maxCount)
	{
		maxCount = _maxCount;
	}
	
	public double getChance()
	{
		return chance;
	}
	
	public void setChance(final double _chance)
	{
		chance = _chance;
	}
	
	public void giveReward(final L2PcInstance player)
	{
		if (getType().equalsIgnoreCase("item"))
		{
			giveItems(player);
		}
		else if (getType().equalsIgnoreCase("clanpoints"))
		{
			giveClanPoints(player);
		}
		else if (getType().equalsIgnoreCase("skillpoints"))
		{
			giveSkillPoints(player);
		}
		else if (getType().equalsIgnoreCase("experience"))
		{
			giveExperience(player);
		}
	}
	
	private void giveClanPoints(final L2PcInstance player)
	{
		if (player.getClan() == null)
		{
			return;
		}
		final double _chance = getChance() * 1000.0;
		final int min = getMinCount();
		int max = getMaxCount();
		if (min > max)
		{
			max = min;
		}
		final int count = Rnd.get(min, max);
		if ((Rnd.get(0, 100000) < _chance) && (player.getClan() != null))
		{
			player.getClan().setReputationScore(player.getClan().getReputationScore() + count, true);
		}
	}
	
	private void giveSkillPoints(final L2PcInstance player)
	{
		final double _chance = getChance() * 1000.0;
		final int min = getMinCount();
		int max = getMaxCount();
		if (min > max)
		{
			max = min;
		}
		final int count = Rnd.get(min, max);
		if (Rnd.get(0, 100000) <= _chance)
		{
			player.getStat().addExpAndSp(0L, count, false);
		}
	}
	
	private void giveExperience(final L2PcInstance player)
	{
		final double _chance = getChance() * 1000.0;
		final int min = getMinCount();
		int max = getMaxCount();
		if (min > max)
		{
			max = min;
		}
		final long count = Rnd.get(min, max);
		if (Rnd.get(0, 100000) <= _chance)
		{
			player.getStat().addExpAndSp(count, 0, false);
		}
	}
	
	private void giveItems(final L2PcInstance player)
	{
		final double _chance = getChance() * 1000.0;
		final int _itemId = getItemId();
		final int min = getMinCount();
		int max = getMaxCount();
		if (min > max)
		{
			max = min;
		}
		final int count = Rnd.get(min, max);
		if (Rnd.get(0, 100000) < _chance)
		{
			final L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), _itemId);
			item.setCount(count);
			player.addItem("RewardItem", item, player, true);
		}
	}
}
