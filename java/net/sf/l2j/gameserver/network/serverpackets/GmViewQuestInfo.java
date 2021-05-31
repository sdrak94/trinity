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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;

/**
 * Sh (dd) h (dddd)
 * @author Tempy
 */
public class GmViewQuestInfo extends L2GameServerPacket
{
	private static final String _S__AC_GMVIEWQUESTLIST = "[S] 99 GMViewQuestList";

    private L2PcInstance _activeChar;

	public GmViewQuestInfo(L2PcInstance cha)
	{
		_activeChar = cha;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x99);
		writeS(_activeChar.getName());

        Quest[] questList = _activeChar.getAllActiveQuests();

        if (questList.length == 0)
        {
            writeC(0);
            writeH(0);
            writeH(0);
            return;
        }

        writeH(questList.length); // quest count

        for (Quest q : questList)
        {
            writeD(q.getQuestId());

            QuestState qs = _activeChar.getQuestState(q.getName());

            if (qs == null)
            {
                writeD(0);
                continue;
            }

            writeD(qs.getInt("cond"));   // stage of quest progress
        }
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__AC_GMVIEWQUESTLIST;
	}
}
