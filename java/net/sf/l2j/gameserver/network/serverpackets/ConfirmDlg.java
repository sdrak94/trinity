/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.serverpackets;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * @author kombat
 * Format: cd d[d s/d/dd/ddd]
 */
public class ConfirmDlg extends L2GameServerPacket
{
private static final String _S__ED_CONFIRMDLG = "[S] f3 ConfirmDlg";
private int _messageId;

private int _skillLvL = 1;

private static final int TYPE_ZONE_NAME = 7;
private static final int TYPE_SKILL_NAME = 4;
private static final int TYPE_ITEM_NAME = 3;
private static final int TYPE_NPC_NAME = 2;
private static final int TYPE_NUMBER = 1;
private static final int TYPE_TEXT = 0;

private final FastList<CnfDlgData> _info = new FastList<CnfDlgData>();

private int _time = 0;
private int _requesterId = 0;

protected class CnfDlgData
{
protected final int type;
protected final Object value;

protected CnfDlgData(int t, Object val)
{
	type = t;
	value = val;
}
}

public ConfirmDlg(int messageId)
{
	_messageId = messageId;
}

public ConfirmDlg addString(String text)
{
	_info.add(new CnfDlgData(TYPE_TEXT, text));
	return this;
}

public ConfirmDlg addNumber(int number)
{
	_info.add(new CnfDlgData(TYPE_NUMBER, number));
	return this;
}

public ConfirmDlg addCharName(L2Character cha)
{
	if (cha instanceof L2Npc)
		return addNpcName((L2Npc)cha);
	if (cha instanceof L2PcInstance)
		return addPcName((L2PcInstance)cha);
	if (cha instanceof L2Summon)
		return addNpcName((L2Summon)cha);
	return addString(cha.getName());
}

public ConfirmDlg addPcName(L2PcInstance pc)
{
	return addString(pc.getDisplayName());
}

public ConfirmDlg addNpcName(L2Npc npc)
{
	return addNpcName(npc.getTemplate());
}

public ConfirmDlg addNpcName(L2Summon npc)
{
	return addNpcName(npc.getNpcId());
}

public ConfirmDlg addNpcName(L2NpcTemplate tpl)
{
	if (tpl.isCustom())
		return addString(tpl.name);
	return addNpcName(tpl.npcId);
}

public ConfirmDlg addNpcName(int id)
{
	_info.add(new CnfDlgData(TYPE_NPC_NAME, id));
	return this;
}

public ConfirmDlg addItemName(L2ItemInstance item)
{
	return addItemName(item.getItem().getItemId());
}

public ConfirmDlg addItemName(L2Item item)
{
	// TODO: template id for items
	return addItemName(item.getItemId());
}

public ConfirmDlg addItemName(int id)
{
	_info.add(new CnfDlgData(TYPE_ITEM_NAME, id));
	return this;
}

public ConfirmDlg addZoneName(int x, int y, int z)
{
	Integer[] coord = {x, y, z};
	_info.add(new CnfDlgData(TYPE_ZONE_NAME, coord));
	return this;
}

public ConfirmDlg addSkillName(L2Effect effect)
{
	return addSkillName(effect.getSkill());
}

public ConfirmDlg addSkillName(L2Skill skill)
{
	if (skill.getId() != skill.getDisplayId()) //custom skill -  need nameId or smth like this.
		return addString(skill.getName());
	return addSkillName(skill.getId(), skill.getLevel());
}

public ConfirmDlg addSkillName(int id)
{
	return addSkillName(id, 1);
}

public ConfirmDlg addSkillName(int id, int lvl)
{
	_info.add(new CnfDlgData(TYPE_SKILL_NAME, id));
	_skillLvL = lvl;
	return this;
}

public ConfirmDlg addTime(int time)
{
	_time = time;
	return this;
}

public ConfirmDlg addRequesterId(int id)
{
	_requesterId = id;
	return this;
}

@Override
protected final void writeImpl()
{
	writeC(0xf3);
	writeD(_messageId);
	
	if (_info.isEmpty())
	{
		writeD(0x00);
		writeD(_time);
		writeD(_requesterId);
	}
	else
	{
		writeD(_info.size());
		
		for (CnfDlgData data : _info)
		{
			writeD(data.type);
			
			switch (data.type)
			{
			case TYPE_TEXT:
				writeS((String)data.value);
				break;
			case TYPE_NUMBER:
			case TYPE_NPC_NAME:
			case TYPE_ITEM_NAME:
				writeD((Integer)data.value);
				break;
			case TYPE_SKILL_NAME:
				writeD((Integer)data.value); // Skill Id
				writeD(_skillLvL); // Skill lvl
				break;
			case TYPE_ZONE_NAME:
				Integer[] array = (Integer[])data.value;
				writeD(array[0]);
				writeD(array[1]);
				writeD(array[2]);
				break;
			}
		}
		if (_time != 0)
			writeD(_time);
		if (_requesterId != 0)
			writeD(_requesterId);
	}
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
 */
@Override
public String getType()
{
	return _S__ED_CONFIRMDLG;
}
}
