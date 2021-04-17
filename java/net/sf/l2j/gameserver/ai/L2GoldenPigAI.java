package net.sf.l2j.gameserver.ai;

import java.util.logging.Level;

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

public class L2GoldenPigAI extends L2AttackableAI implements Runnable
{

	int totalWalkPointsF;
	int totalWalkPointsLR;
	int totalWalkPointsB;
	int totalWalkPointsBLR;

	L2Character pig = _actor;
	
	public L2GoldenPigAI(L2Character.AIAccessor accessor)
	{
		super(accessor);
	}
	
	@Override
	public void run()
	{
		onEvtThink();
	}
	
	@Override
	public void startAITask()
	{
		String message = "startAITask()";
		L2GameServerPacket packet;
		packet = new CreatureSay(_actor.getObjectId(), Say2.SHOUT, _actor.getName(), message);
		//_actor.broadcastPacket(packet);
	}
	
	
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		
		if(totalWalkPointsF < 400)
		{
			RewardItem item = new RewardItem(57, 1);
			dropItem(attacker.getActingPlayer(), item);
			setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(pig.getX(), pig.getY() + 25, pig.getZ(), pig.getInstanceId()));
			pig.broadcastPacket(new CreatureSay(_actor.getObjectId(), Say2.SHOUT, _actor.getName(), "" + totalWalkPointsF));
			totalWalkPointsF += 25;
		}
		else if (totalWalkPointsLR < 200)
		{
			RewardItem item = new RewardItem(57, 1);
			dropItem(attacker.getActingPlayer(), item);
			setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(pig.getX() + 25, pig.getY(), pig.getZ(), pig.getInstanceId()));
			pig.broadcastPacket(new CreatureSay(_actor.getObjectId(), Say2.SHOUT, _actor.getName(), "" + totalWalkPointsLR));
			totalWalkPointsLR += 25;
		}
		else if(totalWalkPointsB < 400)
		{
			RewardItem item = new RewardItem(57, 1);
			dropItem(attacker.getActingPlayer(), item);
			setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(pig.getX(), pig.getY() - 25, pig.getZ(), pig.getInstanceId()));
			pig.broadcastPacket(new CreatureSay(_actor.getObjectId(), Say2.SHOUT, _actor.getName(), "" + totalWalkPointsB));
			totalWalkPointsB += 25;
		}
		else if(totalWalkPointsBLR < 400)
		{
			RewardItem item = new RewardItem(57, 1);
			dropItem(attacker.getActingPlayer(), item);
			setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(pig.getX() - 25, pig.getY(), pig.getZ(), pig.getInstanceId()));
			pig.broadcastPacket(new CreatureSay(_actor.getObjectId(), Say2.SHOUT, _actor.getName(), "" + totalWalkPointsBLR));
			totalWalkPointsBLR += 25;
		}
		else
		{
			totalWalkPointsF = 0;
			totalWalkPointsLR = 0;
			totalWalkPointsBLR = 0;
			totalWalkPointsB = 0;
			return;
		}
		
		String message = "onEvtAttacked() attacker: " + attacker.getName();
		//_actor.broadcastPacket(new CreatureSay(_actor.getObjectId(), Say2.SHOUT, _actor.getName(), message));
		System.out.println(message);
	}
	
	
	public final class RewardItem
	{
		protected int _itemId;

		protected int _count;

		protected int _enchantLevel = 0;

		protected int _partyDropCount = 0;

		public RewardItem(int itemId, int count)
		{
			_itemId = itemId;
			_count = count;
			_enchantLevel = 0;
			_partyDropCount = 0;
		}

		public RewardItem(int itemId, int count, int enchant, int partydrop)
		{
			_itemId = itemId;
			_count = count;
			_enchantLevel = enchant;
			_partyDropCount = partydrop;
		}

		public final int getPartyDropCount()
		{
			return _partyDropCount;
		}

		public final void setPartyDropCount(int partyDropCount)
		{
			_partyDropCount = partyDropCount;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public int getCount()
		{
			return _count;
		}

		public int getEnchantLevel()
		{
			return _enchantLevel;
		}

		public void setItemId(int itemId)
		{
			_itemId = itemId;
		}

	}
	public L2ItemInstance dropItem(L2PcInstance actChar, RewardItem item)
	{
		L2ItemInstance ditem = null;
		for (int i = 0; i < item.getCount(); i++)
		{
			// Randomize drop position
			int newX = pig.getX();
			int newY = pig.getY();
			int newZ = pig.getZ() + 20;
			
			ditem = ItemTable.getInstance().createItem("pig", item.getItemId(), item.getCount(), actChar.getActingPlayer());
			if (ItemTable.getInstance().getTemplate(item.getItemId()) != null)
			{
				ditem.dropMe(pig, newX, newY, newZ-20);
			}
			else
				_log.log(Level.SEVERE, "Item doesn't exist so cannot be dropped. Item ID: " + item.getItemId());
		}
		return ditem;
	}

	public L2ItemInstance dropItem(L2PcInstance lastAttacker, int itemId, int itemCount)
	{
		return dropItem(lastAttacker, new RewardItem(itemId, itemCount));
	}


}