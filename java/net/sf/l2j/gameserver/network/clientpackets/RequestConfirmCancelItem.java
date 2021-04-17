package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExPutItemResultForVariationCancel;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;
























public final class RequestConfirmCancelItem
  extends L2GameClientPacket
{
  private static final String _C__D0_2D_REQUESTCONFIRMCANCELITEM = "[C] D0:2D RequestConfirmCancelItem";
  private int _itemId;
  
  protected void readImpl() { _itemId = readD(); }






  
  protected void runImpl() {
    L2PcInstance activeChar = ((L2GameClient)getClient()).getActiveChar();
    L2ItemInstance item = (L2ItemInstance)L2World.getInstance().findObject(_itemId);
    if (activeChar == null || item == null) {
      return;
    }
    if (item.getOwnerId() != activeChar.getObjectId()) {
      
      Util.handleIllegalPlayerAction(((L2GameClient)getClient()).getActiveChar(), "Warning!! Character " + ((L2GameClient)getClient()).getActiveChar().getName() + " of account " + ((L2GameClient)getClient()).getActiveChar().getAccountName() + " tryied to destroy augment on item that doesn't own.", Config.DEFAULT_PUNISH);
      
      return;
    } 
    if (!item.isAugmented()) {
      
      activeChar.sendPacket((L2GameServerPacket)new SystemMessage(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM));
      
      return;
    } 
    int price = 0;
    switch (item.getItem().getCrystalType()) {
      
      case 2:
        if (item.getCrystalCount() < 1720) {
          price = 95000; break;
        }  if (item.getCrystalCount() < 2452) {
          price = 150000; break;
        } 
        price = 210000;
        break;
      case 3:
        if (item.getCrystalCount() < 1746) {
          price = 240000; break;
        } 
        price = 270000;
        break;
      case 4:
        if (item.getCrystalCount() < 2160) {
          price = 330000; break;
        }  if (item.getCrystalCount() < 2824) {
          price = 390000; break;
        } 
        price = 420000;
        break;
      case 5:
        price = 480000;
        break;
      case 6:
      case 7:
        price = 920000;
        break;
      
      default:
        if (item.getItemId() == 20325) {
          
          price = 480000;
          break;
        } 
        return;
    } 
    
    activeChar.sendPacket((L2GameServerPacket)new ExPutItemResultForVariationCancel(_itemId, price));
  }






  
  public String getType() { return "[C] D0:2D RequestConfirmCancelItem"; }
}
