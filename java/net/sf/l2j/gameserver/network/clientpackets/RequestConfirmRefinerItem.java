package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExPutIntensiveResultForVariationMake;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class RequestConfirmRefinerItem extends AbstractRefinePacket
{
	private int					_targetItemObjId;
	private int					_refinerItemObjId;
	
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
	}
	
	protected void runImpl()
	{
		L2PcInstance activeChar = ((L2GameClient) getClient()).getActiveChar();
		if (activeChar == null)
			return;
		L2ItemInstance targetItem = (L2ItemInstance) L2World.getInstance().findObject(_targetItemObjId);
		if (targetItem == null)
			return;
		L2ItemInstance refinerItem = (L2ItemInstance) L2World.getInstance().findObject(_refinerItemObjId);
		if (refinerItem == null)
		{
			return;
		}
		if (!isValid(activeChar, targetItem, refinerItem))
		{
			activeChar.sendPacket((L2GameServerPacket) new SystemMessage(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}
		int refinerItemId = refinerItem.getItem().getItemId();
		activeChar.sendPacket((L2GameServerPacket) new ExPutIntensiveResultForVariationMake(_refinerItemObjId, refinerItemId, 57, 0));
		activeChar.sendMessage("Put any item in Gemstone slot");
	}
	
	public String getType()
	{
		return "[C] D0:2A RequestConfirmRefinerItem";
	}
}
