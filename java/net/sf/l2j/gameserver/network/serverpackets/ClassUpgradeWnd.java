package net.sf.l2j.gameserver.network.serverpackets;

import static net.sf.l2j.gameserver.model.actor.instance.L2ClassMasterInstance.validateClassId;

import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.util.StringUtil;

public final class ClassUpgradeWnd extends L2GameServerPacket
{
	private final String _html;
	
	public ClassUpgradeWnd(final L2PcInstance player)
	{
		final ClassId currentClassId = player.getClassId();
		
		StringBuilder sb = new StringBuilder(100);
		sb.append("<html><title>Class Manager</title><center><br><br>Choose your <font color=LEVEL>Base Class</font><br1><font color=FF0000>Warning:</font> Can't be changed!<br><br><br>");
		for (final ClassId cid : ClassId.values())
			if (validateClassId(currentClassId, cid) && cid.level() == 3)
				StringUtil.append(sb, "<button action=\"bypass -h trinity_change_class ", String.valueOf(cid.getId()), "\" value=\"", CharTemplateTable.getInstance().getClassNameById(cid.getId()), "\" width=110 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"/><br>");
		sb.append("</center></html>");
//		System.out.println(sb.toString());
		_html = sb.toString();
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x19);
		writeD(0);
		writeS(_html);
		writeD(0);
	}
	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return null;
	}
}