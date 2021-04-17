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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.NpcBufferTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2NpcBufferInstance extends L2Npc
{
	static final Logger _log = Logger.getLogger(L2NpcBufferInstance.class.getName());
	
	public L2NpcBufferInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(L2PcInstance playerInstance, int val)
	{
		if (playerInstance == null)
			return;
		
		String htmContent = HtmCache.getInstance().getHtm("data/html/mods/NpcBuffer.htm");
		
		if (val > 0)
			htmContent = HtmCache.getInstance().getHtm("data/html/mods/NpcBuffer-" + val + ".htm");
		
		if (htmContent != null)
		{
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
			
			npcHtmlMessage.setHtml(htmContent);
			npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
			playerInstance.sendPacket(npcHtmlMessage);
		}
		
		playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	int pageVal = 0;
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		// BypassValidation Exploit plug.
		if (player == null || player.getLastFolkNPC() == null || player.getLastFolkNPC().getObjectId() != this.getObjectId())
			return;
		
		int npcId = getNpcId();
		
		if (command.startsWith("Chat"))
		{
			int val = Integer.parseInt(command.substring(5));
			
			pageVal = val;
			
			showChatWindow(player, val);
		}
		else if (command.startsWith("Buff"))
		{
			String[] buffGroupArray = command.substring(5).split(" ");
			
			for (String buffGroupList : buffGroupArray)
			{
				if (buffGroupList == null)
				{
					_log.warning("NPC Buffer Warning: npcId = " + npcId + " has no buffGroup set in the bypass for the buff selected.");
					return;
				}
				
				int buffGroup = Integer.parseInt(buffGroupList);
				
				int[] npcBuffGroupInfo = NpcBufferTable.getInstance().getSkillInfo(npcId, buffGroup);
				
				if (npcBuffGroupInfo == null)
				{
					_log.warning("NPC Buffer Warning: npcId = " + npcId + " Location: " + getX() + ", " + getY() + ", " + getZ() + " Player: " + player.getName() + " has tried to use skill group (" + buffGroup + ") not assigned to the NPC Buffer!");
					return;
				}
				
				int skillId = npcBuffGroupInfo[0];
				int skillLevel = npcBuffGroupInfo[1];
				int skillFeeId = npcBuffGroupInfo[2];
				int skillFeeAmount = npcBuffGroupInfo[3];
				
				if (skillFeeId != 0)
				{
					L2ItemInstance itemInstance = player.getInventory().getItemByItemId(skillFeeId);
					
					if (itemInstance == null || (!itemInstance.isStackable() && player.getInventory().getInventoryItemCount(skillFeeId, -1) < skillFeeAmount))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
						player.sendPacket(sm);
						continue;
					}
					
					if (itemInstance.isStackable())
					{
						if (!player.destroyItemByItemId("Npc Buffer", skillFeeId, skillFeeAmount, player.getTarget(), true))
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
							player.sendPacket(sm);
							continue;
						}
					}
					else
					{
						for (int i = 0; i < skillFeeAmount; ++i)
						{
							player.destroyItemByItemId("Npc Buffer", skillFeeId, 1, player.getTarget(), true);
						}
					}
				}
				
				L2Skill skill;
				skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
				
				if (skill != null)
					skill.getEffects(player, player);
			}
			
			showChatWindow(player, pageVal);
		}
		else if (command.startsWith("Heal"))
		{
			if (!player.isInCombat() && !AttackStanceTaskManager.getInstance().getAttackStanceTask(player))
			{
				String[] healArray = command.substring(5).split(" ");
				
				for (String healType : healArray)
				{
					if (healType.equalsIgnoreCase("HP"))
					{
						player.setCurrentHp(player.getMaxHp());
					}
					else if (healType.equalsIgnoreCase("MP"))
					{
						player.setCurrentMp(player.getMaxMp());
					}
					else if (healType.equalsIgnoreCase("CP"))
					{
						player.setCurrentCp(player.getMaxCp());
					}
				}
			}
			showChatWindow(player, 0); // 0 = main window
		}
		else if (command.startsWith("RemoveBuffs"))
		{
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			showChatWindow(player, 0); // 0 = main window
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}