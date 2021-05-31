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
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.util.Util;

public class HeroWeapon extends Quest
{
	public static final int[] WEAPONS =
	{
		6611, // Infinity Blade
		6612, // Infinity Cleaver
		6613, // Infinity Axe
		6614, // Infinity Rod
		6615, // Infinity Crusher
		6616, // Infinity Scepter
		6617, // Infinity Stinger
		6618, // Infinity Fang
		6619, // Infinity Bow
		6620, // Infinity Wing
		6621, // Infinity Spear
		9388, // Infinity Rapier
		9389, // Infinity Sword
		9390, // Infinity Shooter
	};
	
	public HeroWeapon()
	{
		super(-1, "HeroWeapon", "custom");
		addStartNpc(31690, 31769, 31770, 31771, 31772, 31773);
		addTalkId(31690, 31769, 31770, 31771, 31772, 31773);
	}
	
	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player)
	{
		final QuestState st = player.getQuestState(getName());
		final int weaponId = Integer.valueOf(event);
		if (Util.contains(WEAPONS, weaponId))
			st.giveItems(weaponId, 1, Hero.getInstance().isInactiveHero(player.getObjectId()) ? 18 : 16);
		st.exitQuest(true);
		return null;
	}
	
	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player)
	{
		String htmltext = "";
		final QuestState st = player.getQuestState(getName());
		if (st == null)
			newQuestState(player);
		if (st != null)
			if (player.isHero())//if (Hero.getInstance().isInactiveHero(player.getObjectId()))
			{
				if (hasHeroWeapon(player))
				{
					htmltext = "already_have_weapon.htm";
					st.exitQuest(true);
				}
				else
					htmltext = "weapon_list.htm";
			}
			else
			{
				htmltext = "no_hero_weapon.htm";
				st.exitQuest(true);
			}
		return htmltext;
	}
	
	private static boolean hasHeroWeapon(final L2PcInstance player)
	{
		for (final int i : WEAPONS)
			if (player.getInventory().getItemByItemId(i) != null)
				return true;
		return false;
	}
}