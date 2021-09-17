/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import luna.custom.captcha.RandomString;
import luna.custom.captcha.instancemanager.BotsPreventionManager;
import luna.custom.captcha.instancemanager.BotsPreventionManager.PlayerData;
import luna.custom.handler.items.bonanzo.BonanzoData;
import luna.custom.handler.items.bonanzo.BonanzoData.RewardData;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.serverpackets.TutorialCloseHtml;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.Util;

public class RequestTutorialPassCmdToServer extends L2GameClientPacket
{
	String _bypass = null;
	
	protected void readImpl()
	{
		_bypass = readS();
	}
	
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(!_bypass.contains("bonanzo"))
		{
			String _AntibotCommand1 = RandomString.getInstance().getRandomString1() + "_";
			String _AntibotCommand2 = RandomString.getInstance().getRandomString2() + "_";
			Map<Integer, PlayerData> _validation = null;
			Map<Integer, Future<?>> _beginvalidation = null;
			_validation = new HashMap<>();
			_beginvalidation = new HashMap<>();
		}
		if (player == null)
			return;
		QuestState qs = player.getQuestState("255_Tutorial");
		if (qs != null)
			qs.getQuest().notifyEvent(_bypass, null, player);
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			qs = player.getQuestState("Achievements");
			if (qs != null)
				qs.getQuest().notifyEvent(_bypass, null, player);
		}
		if (_bypass.contains("_continue"))
		{
			// player.setIsCaptchaValidating(true);
			// player.sendMessage(String.valueOf(player._IsCaptchaValidating));
			qs.getQuest().notifyEvent(_bypass, null, player);
			// player.sendPacket(new TutorialCloseHtml());
			// BotsPreventionManager.getInstance().validationwindow(player);
			BotsPreventionManager.getInstance().AnalyseBypass(_bypass, player);
		}
		else if (_bypass.startsWith("bonanzo_open"))
		{
			if (_bypass.contains(" "))
			{
				String[] split = _bypass.split(" ");
				if (split[1].length() > 1)
				{
					if (Util.isDigit(split[1]))
					{
						int itemId = Integer.parseInt(split[1]);
						if (BonanzoData.getInstance().containsBox(itemId))
						{
							if (player.getInventory().getItemByItemId(itemId).getCount() > 0)
							{
								if (player.destroyItemByItemId("BoxPayment", itemId, 1, player, true))
								{
									RewardData item = BonanzoData.getInstance().getRandomReward(itemId);
									int enchant = item.getRndEnc();
									
									String EnchDisp = enchant != 0? "+" + String.valueOf(enchant)+" " : "";
									int chance = item.getChanceToDisplay();
									if (chance <= 100)
									{
										L2Item reward = ItemTable.getInstance().getTemplate(item.getItemId());
										
//										for (L2PcInstance allPlayers : L2World.getInstance().getAllPlayers().values())
//										{
//											if(player.isGM())
//												break;
//											allPlayers.sendPacket(new ExShowScreenMessage(1, -1, 2, 0, 1, 0, 0, true, 5000, 0, player.getName() +" has obtained "+ EnchDisp+ reward.getName() +" from " + item.getTitleToDisplay()));
//										}
									}
									if(item.getAug())
									{
										player.addItem("AugBonanzoReward", item.getItemId(), item.getAmount(), player, true, enchant);
									}
									else
									{
										player.addItem("BonanzoReward", item.getItemId(), item.getAmount(), player, true, enchant);
									}
									BonanzoData.getInstance().itemResult(player, item.getItemId(), enchant, item.getAmount());
									
									player.getInventory().updateDatabase();

			    					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
			    					{
			    						player.getCounters().bonanzoOpened++;
			    					}
									
									//player.sendPacket(new TutorialCloseHtml());
								}
								else
								{
									System.out.println(player.getName() + " if (player.destroyItemByItemId(\"BoxPayment\", itemId, 1, player, true)): " + _bypass);
								}
							}
							else
							{
								System.out.println(player.getName() + " if (player.getInventory().getItemByItemId(itemId).getCount() > 0): " + _bypass);
							}
						}
						else
						{
							System.out.println(player.getName() + " if (BonanzoData.getInstance().containsBox(itemId)): " + _bypass);
						}
					}
					else
					{
						System.out.println(player.getName() + " if (Util.isDigit(split[1])): " + _bypass);
					}
				}
				else
				{
					System.out.println(player.getName() + " if (split[1].length() > 1): " + _bypass);
				}
			}
			else
			{
				System.out.println(player.getName() + " if (_bypass.contains(\" \")): " + _bypass);
			}
		}
		else if (_bypass.equals("bonanzo_close"))
		{
			player.sendPacket(new TutorialCloseHtml());
			if (qs != null)
			{
				qs.getQuest().notifyEvent(_bypass, null, player);
			}
			return;
		}
		
		if (_bypass.contains("_close"))
		{
			if (qs != null)
			{
				qs.getQuest().notifyEvent(_bypass, null, player);
			}
			player.sendPacket(new TutorialCloseHtml());
			BotsPreventionManager.getInstance().validationwindow(player);
		}
	}
	
	public String getType()
	{
		return "[C] 86 RequestTutorialPassCmdToServer";
	}
}
