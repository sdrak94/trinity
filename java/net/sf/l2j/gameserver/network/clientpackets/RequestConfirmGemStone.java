package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExPutCommissionResultForVariationMake;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestConfirmGemStone extends AbstractRefinePacket
  {
  private static final String _C__D0_2B_REQUESTCONFIRMGEMSTONE = "[C] D0:2B RequestConfirmGemStone";
  private int _targetItemObjId;
  private int _refinerItemObjId;
  private int _gemstoneItemObjId;
  
  protected void readImpl()
  {
    _targetItemObjId = readD();
    _refinerItemObjId = readD();
    _gemstoneItemObjId = readD();
    readQ();
  }
  protected void runImpl()
  {
    L2PcInstance activeChar = ((L2GameClient)getClient()).getActiveChar();
    if (activeChar == null)
    {
      return;
    }
    L2ItemInstance targetItem = (L2ItemInstance)L2World.getInstance().findObject(_targetItemObjId);
    if (targetItem == null)
    {
      return;
    }
    L2ItemInstance refinerItem = (L2ItemInstance)L2World.getInstance().findObject(_refinerItemObjId);
    
    if (refinerItem == null)
    {
      return;
    }
    
    if (!isValid(activeChar, targetItem, refinerItem))
    {
      
      activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM));
      
      return;
    } 
    
    AbstractRefinePacket.LifeStone ls = getLifeStone(refinerItem.getItemId());
    
    if (ls == null)
    {
      return;
    }
    activeChar.sendPacket(new ExPutCommissionResultForVariationMake(_gemstoneItemObjId, 1L, 2131));
    activeChar.sendPacket(new SystemMessage(SystemMessageId.PRESS_THE_AUGMENT_BUTTON_TO_BEGIN));
  }
  
  public String getType()
  {
	  return "[C] D0:2B RequestConfirmGemStone";
}
}
