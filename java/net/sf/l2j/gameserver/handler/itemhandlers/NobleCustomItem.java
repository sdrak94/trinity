package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

public class NobleCustomItem implements IItemHandler
{

    public NobleCustomItem()
    {

    }

    public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
    {

        if (!(playable instanceof L2PcInstance)) {
            return;
        }

        L2PcInstance activeChar = (L2PcInstance) playable;
        if (activeChar.isNoble())
        {
            activeChar.sendMessage("You are already a nobless!");
        }
        else
        {

            activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 16));
            activeChar.setNoble(true);
            activeChar.sendMessage("You have successfully become nobless!");
            activeChar.broadcastUserInfo();
            playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
        }


    }

    private static final int ITEM_IDS[] = {7679};

    public int[] getItemIds()
    {
        return ITEM_IDS;
    }

}
