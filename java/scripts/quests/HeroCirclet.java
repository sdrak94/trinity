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
package scripts.quests;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;

public class HeroCirclet extends Quest
{
	public HeroCirclet()
	{
		super(-1, "HeroCirclet", "custom");
		addStartNpc(31690, 31769, 31770, 31771, 31772);
		addTalkId(31690, 31769, 31770, 31771, 31772);
	}
	
	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
			st = newQuestState(player);
//		if (Hero.getInstance().isActiveHero(player.getObjectId()))
		if (player.isHero())
		{
			if (player.getInventory().getItemByItemId(6842) == null)
				st.giveItems(6842, 1);
			else
				htmltext = "already_have_circlet.htm";
		}
		else
		{
			htmltext = "no_hero_circlet.htm";
			if (player.isHero())
			{
				player.sendMessage("Stay back scum!");
				player.sendMessage("Only real heroes can obtain the supreme accessory.");
			}
			
		}
		st.exitQuest(true);
		return htmltext;
	}
}