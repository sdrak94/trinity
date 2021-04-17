package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Henna;
import net.sf.l2j.gameserver.util.StringUtil;

public class L2SymbolMakerInstance extends L2Npc
{
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		if (command.equals("Draw"))
		{
			/*L2HennaInstance[] henna = HennaTreeTable.getInstance().getAvailableHenna(player.getClassId());
			HennaEquipList hel = new HennaEquipList(player, henna);
			player.sendPacket(hel);*/
		}
		else if (command.equals("RemoveList"))
        {
			/*showRemoveChat(player);*/
		}
		else if (command.startsWith("Remove "))
		{
			/*int slot = Integer.parseInt(command.substring(7));
			player.removeHenna(slot);*/
		}
		else
        {
			super.onBypassFeedback(player, command);
		}
	}
	
	public static void showRemoveChat(L2PcInstance player)
	{
		final StringBuilder html1 = StringUtil.startAppend(250, "<html><body>Select symbol you would like to remove:<br><br>");
		boolean hasHennas = false;
		
		for (int i = 1; i <= 3; i++)
		{
			L2Henna henna = player.getHenna(i);
			
			if (henna != null)
			{
				hasHennas = true;
				StringUtil.append(html1, "<a action=\"bypass -h gem_symbol_Remove ", String.valueOf(i), "\">", henna.symbolName, "</a><br>");
			}
		}
		
		if (!hasHennas)
		{
			html1.append("You don't have any symbol to remove.");
		}
		
		html1.append("</body></html>");
		
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(html1.toString());
		player.sendPacket(html);
	}
	
	public L2SymbolMakerInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return "data/html/symbolmaker/SymbolMaker.htm";
	}

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.model.L2Object#isAttackable()
     */
    @Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}}