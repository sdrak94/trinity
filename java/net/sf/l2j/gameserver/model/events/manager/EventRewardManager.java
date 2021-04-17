package net.sf.l2j.gameserver.model.events.manager;

import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.dataTables.RewardsTemplate;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.util.Rnd;

public class EventRewardManager
{
	private List<RewardsTemplate>	winnerRewards	= new FastList<RewardsTemplate>();
	private List<RewardsTemplate>	loserRewards	= new FastList<RewardsTemplate>();
	private List<RewardsTemplate>	topRewards		= new FastList<RewardsTemplate>();
	private List<RewardsTemplate>	tieRewards		= new FastList<RewardsTemplate>();
	private List<RewardsTemplate>	earlyRewards	= new FastList<RewardsTemplate>();
	Event							e				= EventsParser.getInstance().getEvents().get(EventVarHolder.getInstance().getRunningEventId());
	
	public void loadRewards(Event e)
	{
		winnerRewards.clear();
		loserRewards.clear();
		topRewards.clear();
		tieRewards.clear();
		earlyRewards.clear();
		for (int rewGrp = 0; rewGrp < e.getEvRewards().size(); rewGrp++)
		{
			int rewardType = e.getEvRewards().get(rewGrp).getRewardType();
			int rewardId = e.getEvRewards().get(rewGrp).getItemId();
			int rewardCount = e.getEvRewards().get(rewGrp).getAmmount();
			int chance = e.getEvRewards().get(rewGrp).getChance();
			boolean isStatic = e.getEvRewards().get(rewGrp).isStatic();
			switch (e.getEvRewards().get(rewGrp).getRewardType())
			{
				case 1:
					winnerRewards.add(new RewardsTemplate(rewardType, rewardId, rewardCount, chance, isStatic));
					break;
				case 2:
					loserRewards.add(new RewardsTemplate(rewardType, rewardId, rewardCount, chance, isStatic));
					break;
				case 3:
					tieRewards.add(new RewardsTemplate(rewardType, rewardId, rewardCount, chance, isStatic));
					break;
				case 4:
					earlyRewards.add(new RewardsTemplate(rewardType, rewardId, rewardCount, chance, isStatic));
					break;
				case 5:
					topRewards.add(new RewardsTemplate(rewardType, rewardId, rewardCount, chance, isStatic));
					break;
			}
		}
	}
	
	public List<RewardsTemplate> getRewards(int type)
	{
		List<RewardsTemplate> chosenList = null;
		switch (type)
		{
			case 1:
				chosenList = winnerRewards;
				break;
			case 2:
				chosenList = loserRewards;
				break;
			case 3:
				chosenList = tieRewards;
				break;
			case 5:
				chosenList = topRewards;
				break;
		}
		return chosenList;
	}
	
	public void rewardPlayer(int type, L2PcInstance p, Boolean earlyReg)
	{
		if (type == 1)
		{
			for (int i = 0; i < winnerRewards.size(); i++)
			{
				if (winnerRewards.get(i).getChance() != 100)
				{
					if (Rnd.get(100) <= winnerRewards.get(i).getChance())
					{
						int itemId = winnerRewards.get(i).getItemId();
						int count = winnerRewards.get(i).getAmmount();
						boolean isStatic = winnerRewards.get(i).isStatic();
						if (earlyReg && !isStatic)
						{
							int newcount = (int) (count * 1.25);
							if (newcount == count)
								count++;
							else
								count = newcount;
						}
						p.addItem(e.getName(), itemId, count, null, true);
					}
				}
				else
				{
					int itemId = winnerRewards.get(i).getItemId();
					int count = winnerRewards.get(i).getAmmount();
					boolean isStatic = winnerRewards.get(i).isStatic();
					if (earlyReg && !isStatic)
					{
						int newcount = (int) (count * 1.25);
						if (newcount == count)
							count++;
						else
							count = newcount;
					}
					p.addItem(e.getName(), itemId, count, null, true);
				}
			}
		}
		if (type == 2)
		{
			for (int i = 0; i < loserRewards.size(); i++)
			{
				if (loserRewards.get(i).getChance() != 100)
				{
					if (Rnd.get(100) <= loserRewards.get(i).getChance())
					{
						int itemId = loserRewards.get(i).getItemId();
						int count = loserRewards.get(i).getAmmount();
						boolean isStatic = loserRewards.get(i).isStatic();
						if (earlyReg && !isStatic)
						{
							int newcount = (int) (count * 1.25);
							if (newcount == count)
								count++;
							else
								count = newcount;
						}
						p.addItem(e.getName(), itemId, count, null, true);
					}
				}
				else
				{
					int itemId = loserRewards.get(i).getItemId();
					int count = loserRewards.get(i).getAmmount();
					boolean isStatic = loserRewards.get(i).isStatic();
					if (earlyReg && !isStatic)
					{
						int newcount = (int) (count * 1.25);
						if (newcount == count)
							count++;
						else
							count = newcount;
					}
					p.addItem(e.getName(), itemId, count, null, true);
				}
			}
		}
		if (type == 3)
		{
			for (int i = 0; i < tieRewards.size(); i++)
			{
				if (tieRewards.get(i).getChance() != 100)
				{
					if (Rnd.get(100) <= tieRewards.get(i).getChance())
					{
						int itemId = tieRewards.get(i).getItemId();
						int count = tieRewards.get(i).getAmmount();
						boolean isStatic = tieRewards.get(i).isStatic();
						if (earlyReg && !isStatic)
						{
							int newcount = (int) (count * 1.25);
							if (newcount == count)
								count++;
							else
								count = newcount;
						}
						p.addItem(e.getName(), itemId, count, null, true);
					}
				}
				else
				{
					int itemId = tieRewards.get(i).getItemId();
					int count = tieRewards.get(i).getAmmount();
					boolean isStatic = tieRewards.get(i).isStatic();
					if (earlyReg && !isStatic)
					{
						int newcount = (int) (count * 1.25);
						if (newcount == count)
							count++;
						else
							count = newcount;
					}
					p.addItem(e.getName(), itemId, count, null, true);
				}
			}
		}
		if (type == 4)
		{
			for (int i = 0; i < earlyRewards.size(); i++)
			{
				if (earlyRewards.get(i).getChance() != 100)
				{
					if (Rnd.get(100) <= earlyRewards.get(i).getChance())
					{
						int itemId = earlyRewards.get(i).getItemId();
						int count = earlyRewards.get(i).getAmmount();
						boolean isStatic = earlyRewards.get(i).isStatic();
						if (earlyReg && !isStatic)
						{
							int newcount = (int) (count * 1.25);
							if (newcount == count)
								count++;
							else
								count = newcount;
						}
						p.addItem(e.getName(), itemId, count, null, true);
					}
				}
				else
				{
					int itemId = earlyRewards.get(i).getItemId();
					int count = earlyRewards.get(i).getAmmount();
					boolean isStatic = earlyRewards.get(i).isStatic();
					if (earlyReg && !isStatic)
					{
						int newcount = (int) (count * 1.25);
						if (newcount == count)
							count++;
						else
							count = newcount;
					}
					p.addItem(e.getName(), itemId, count, null, true);
				}
			}
		}
		if (type == 5)
		{
			for (int i = 0; i < topRewards.size(); i++)
			{
				if (topRewards.get(i).getChance() != 100)
				{
					if (Rnd.get(100) <= topRewards.get(i).getChance())
					{
						int itemId = topRewards.get(i).getItemId();
						int count = topRewards.get(i).getAmmount();
						boolean isStatic = topRewards.get(i).isStatic();
						if (earlyReg && !isStatic)
						{
							int newcount = (int) (count * 1.25);
							if (newcount == count)
								count++;
							else
								count = newcount;
						}
						p.addItem(e.getName(), itemId, count, null, true);
					}
				}
				else
				{
					int itemId = topRewards.get(i).getItemId();
					int count = topRewards.get(i).getAmmount();
					boolean isStatic = topRewards.get(i).isStatic();
					if (earlyReg && !isStatic)
					{
						int newcount = (int) (count * 1.25);
						if (newcount == count)
							count++;
						else
							count = newcount;
					}
					p.addItem(e.getName(), itemId, count, null, true);
				}
			}
		}
	}
	
	public static EventRewardManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EventRewardManager _instance = new EventRewardManager();
	}
}
