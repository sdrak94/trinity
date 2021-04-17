package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class ChristmasTree implements IItemHandler
{
public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
{
	L2PcInstance activeChar = (L2PcInstance) playable;
	L2NpcTemplate template1 = null;
	
	switch (item.getItemId())
	{
	case 5560:
		template1 = NpcTable.getInstance().getTemplate(13006);
		break;
	case 5561:
		template1 = NpcTable.getInstance().getTemplate(13007);
		break;
	}
	
	if (template1 == null)
		return;
	
	L2Object target = activeChar.getTarget();
	
	if (target == null)
		target = activeChar;
	
	try
	{
		L2Spawn spawn = new L2Spawn(template1);
		spawn.setId(IdFactory.getInstance().getNextId());
		spawn.setLocx(target.getX());
		spawn.setLocy(target.getY());
		spawn.setLocz(target.getZ());
		spawn.setInstanceId(activeChar.getInstanceId());
		L2World.getInstance().storeObject(spawn.spawnOne(false));
		
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
		
		activeChar.sendMessage("Created " + template1.name + " at x: " + spawn.getLocx() + " y: " + spawn.getLocy() + " z: " + spawn.getLocz());
	}
	catch (Exception e)
	{
		activeChar.sendMessage("Target is not ingame.");
	}
}
}