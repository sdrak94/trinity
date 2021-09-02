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

import java.util.Iterator;
import java.util.Set;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Clan.SubPledge;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2PledgeSkillLearn;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.PlayerClass;
import net.sf.l2j.gameserver.model.base.SubClass;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AcquireSkillList;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.StringUtil;
import net.sf.l2j.gameserver.util.Util;



/**
 * The generic villagemaster. Some childs instances depends of it for race/classe restriction.
 */
public class L2VillageMasterInstance extends L2NpcInstance
{
	public L2VillageMasterInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public String getHtmlPath(final int npcId, final int val)
	{
		String filename = "";
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		return "data/html/villagemaster/" + filename + ".htm";
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		if (player.isParalyzed())
			return;
		final String[] commandStr = command.split(" ");
		final String actualCommand = commandStr[0];
		String cmdParams = "";
		String cmdParams2 = "";
		if (commandStr.length >= 2)
			cmdParams = commandStr[1];
		if (commandStr.length >= 3)
			cmdParams2 = commandStr[2];
		if (actualCommand.equalsIgnoreCase("create_clan"))
		{
			if (cmdParams.isEmpty())
				return;
			ClanTable.getInstance().createClan(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("create_academy"))
		{
			if (cmdParams.isEmpty())
				return;
			createSubPledge(player, cmdParams, null, L2Clan.SUBUNIT_ACADEMY, 5);
		}
		else if (actualCommand.equalsIgnoreCase("rename_pledge"))
		{
			if (cmdParams.isEmpty() || cmdParams2.isEmpty())
				return;
			renameSubPledge(player, Integer.valueOf(cmdParams), cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_royal"))
		{
			if (cmdParams.isEmpty())
				return;
			createSubPledge(player, cmdParams, cmdParams2, L2Clan.SUBUNIT_ROYAL1, 6);
		}
		else if (actualCommand.equalsIgnoreCase("create_knight"))
		{
			if (cmdParams.isEmpty())
				return;
			createSubPledge(player, cmdParams, cmdParams2, L2Clan.SUBUNIT_KNIGHT1, 7);
		}
		else if (actualCommand.equalsIgnoreCase("assign_subpl_leader"))
		{
			if (cmdParams.isEmpty())
				return;
			assignSubPledgeLeader(player, cmdParams, cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_ally"))
		{
			if (cmdParams.isEmpty())
				return;
			if (player.getClan() == null)
				player.sendPacket(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE);
			else
				player.getClan().createAlly(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_ally"))
			player.getClan().dissolveAlly(player);
		else if (actualCommand.equalsIgnoreCase("dissolve_clan"))
			dissolveClan(player, player.getClanId());
		else if (actualCommand.equalsIgnoreCase("change_clan_leader"))
		{
			if (cmdParams.isEmpty())
				return;
			changeClanLeader(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("recover_clan"))
			recoverClan(player, player.getClanId());
		else if (actualCommand.equalsIgnoreCase("increase_clan_level"))
		{
			if (player.getClan().levelUpClan(player))
				player.broadcastPacket(new MagicSkillUse(player, player, 5103, 1, 0, 0));
		}
		else if (actualCommand.equalsIgnoreCase("learn_clan_skills"))
			showPledgeSkillList(player);
		else if (command.startsWith("Subclass"))
		{
			// Subclasses
			// may not be
			// changed while
			// a skill is in
			// use.
			if (player.isCastingNow() || player.isAllSkillsDisabled())
			{
				player.sendPacket(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE);
				return;
			}
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			int cmdChoice = 0;
			int paramOne = 0;
			int paramTwo = 0;
			try
			{
				cmdChoice = Integer.parseInt(command.substring(9, 10).trim());
				int endIndex = command.indexOf(' ', 11);
				if (endIndex == -1)
					endIndex = command.length();
				paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
				if (command.length() > endIndex)
					paramTwo = Integer.parseInt(command.substring(endIndex).trim());
			}
			catch (final Exception NumberFormatException)
			{
			}
			switch (cmdChoice)
			{
				case 0: // Subclass change menu
					html.setFile("data/html/villagemaster/SubClass.htm");
					break;
				case 1: // Add Subclass Initial Subclasses may not be added while a summon is active.
					if (player.getPet() != null)
					{
						player.sendPacket(SystemMessageId.CANT_SUBCLASS_WITH_SUMMONED_SERVITOR);
						return;
					}
					if (player.getTotalSubClasses() >= 36)
					{
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
						break;
					}
					html.setFile("data/html/villagemaster/SubClass_Add.htm");
					final StringBuilder content1 = StringUtil.startAppend(200);
					Set<PlayerClass> subsAvailable = getAvailableSubClasses(player);
					if (subsAvailable != null && !subsAvailable.isEmpty())
						for (final PlayerClass subClass : subsAvailable)
						{
							if (subClass.ordinal() == player._activeClass)
								continue;
							StringUtil.append(content1, "<a action=\"bypass -h npc_%objectId%_Subclass 4 ", String.valueOf(subClass.ordinal()), "\" msg=\"1268;", formatClassForDisplay(subClass), "\">", formatClassForDisplay(subClass), subClass.ordinal() == player._baseClass ? "</a> (Base Class)" : "", "</a><br>");
							
						}
					else
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					html.replace("%list%", content1.toString());
					break;
				case 2: // Change Class - Initial Subclasses may not be changed while a summon is active.
					if (player.getPet() != null)
					{
						player.sendPacket(SystemMessageId.CANT_SUBCLASS_WITH_SUMMONED_SERVITOR);
						return;
					}
					// Subclasses may not be changed while a you are over your weight limit.
					// if
					// (!player.isInventoryUnder80(true)
					// ||
					// player.getWeightPenalty()
					// > 0)
					// {
					// player.sendPacket(SystemMessageId.NOT_SUBCLASS_WHILE_OVERWEIGHT);
					// return;
					// }
					if (player.getSubClasses().isEmpty())
						html.setFile("data/html/villagemaster/SubClass_ChangeNo.htm");
					else
					{
						final String base = "Base Lv. " + player.getStat().getBaseLevel() + " " + CharTemplateTable.getInstance().getClassNameById(player.getBaseClass());
						
						final StringBuilder content2 = StringUtil.startAppend(1800);
						if (player.getActiveClass() != player.getBaseClass() && checkVillageMaster(player.getBaseClass()))
							StringUtil.append(content2, String.format("<table><tr><td><button value=\"%s\" action=\"bypass -h npc_%s_Subclass 5 0 \" width=156 height=28 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>", "(BASE) Lv. " + player.getStat().getBaseLevel() + " " + CharTemplateTable.getInstance().getClassNameById(player.getBaseClass()), "%objectId%"));
						else

							StringUtil.append(content2, String.format("<table><tr><td><button value=\"%s\" action=\"\" width=156 height=28 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df_down\"></td></tr></table>", "(BASE) Lv. " + player.getStat().getBaseLevel() + " " + CharTemplateTable.getInstance().getClassNameById(player.getBaseClass())));
							//StringUtil.append(content2, "<table width=100%><tr><td align=center><button value=\\" + base  + "\" action=\"\" width=156 height=28 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df_down\"></td></tr></table><br>");
							//StringUtil.append(content2, "<table width=100%><tr><td align=center>Lv. " + player.getStat().getBaseLevel() + " " + CharTemplateTable.getInstance().getClassNameById(player.getBaseClass()) + "</td></tr></table>");
						//StringUtil.append(content2, "<a action=\"bypass -h npc_%objectId%_Subclass 5 0\">", CharTemplateTable.getInstance().getClassNameById(player.getBaseClass()) + "</a><font color=LEVEL>   (Base Class)</font>", "</a><br>");
						StringUtil.append(content2, "<table>");
						for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
						{
//							final var sub = subList.next();
							
							final SubClass subClass = subList.next();
							if (subClass.getClassId() != player.getActiveClass())
							{
								if (checkVillageMaster(subClass.getClassDefinition()))
									StringUtil.append(content2, String.format("<tr><td><button value=\"%s\" action=\"bypass -h npc_%s_Subclass 5 %d \" width=156 height=28 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>", " Lv. " + subClass.getLevel() + " " + formatClassForDisplay(subClass.getClassDefinition()), "%objectId%", subClass.getClassIndex()));

							}
							else
							{
								StringUtil.append(content2, String.format("<tr><td><button value=\"%s\" action=\"\" width=156 height=28 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df_down\"></td>", "(ACTIVE) Lv. " + subClass.getLevel() + " " + formatClassForDisplay(subClass.getClassDefinition()), "%objectId%", subClass.getClassIndex()));
								//StringUtil.append(content2, String.format("<tr><td align=center> Lv. " + subClass.getLevel() + " " + formatClassForDisplay(subClass.getClassDefinition()) + "</td>"));
							}
						
							
							if (subList.hasNext())
							{
								final var subClass2 = subList.next();
								if (subClass2.getClassId() != player.getActiveClass())
								{							
									if (checkVillageMaster(subClass2.getClassDefinition()))
										StringUtil.append(content2, String.format("<td><button value=\"%s\" action=\"bypass -h npc_%s_Subclass 5 %d \" width=156 height=28 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>", " Lv. " + subClass2.getLevel() + " " + formatClassForDisplay(subClass2.getClassDefinition()), "%objectId%", subClass2.getClassIndex()));
								}

								else
								{

									StringUtil.append(content2, String.format("<td><button value=\"%s\" action=\"\" width=156 height=28 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>", "(ACTIVE) Lv. " + subClass2.getLevel() + " " + formatClassForDisplay(subClass2.getClassDefinition()), "%objectId%"));
									//StringUtil.append(content2, String.format("<td align=center> Lv. " + subClass2.getLevel() + " " + formatClassForDisplay(subClass2.getClassDefinition()) + "</td>"));
								}
								
							}
							
							StringUtil.append(content2, "</tr>");
						}
						
						StringUtil.append(content2, "</table>");

						if (content2.length() > 0)
						{
							html.setFile("data/html/villagemaster/SubClass_Change.htm");
							html.replace("%list%", content2.toString());
						}
						else
							html.setFile("data/html/villagemaster/SubClass_ChangeNotFound.htm");
					}
					break;
				case 3: // Change/Cancel Subclass - Initial
					if (player.getSubClasses() == null || player.getSubClasses().isEmpty())
					{
						html.setFile("data/html/villagemaster/SubClass_ModifyEmpty.htm");
						break;
					}
					html.setFile("data/html/villagemaster/SubClass_Modify.htm");
//					String s = "";
//					int i = 1;
//					while (player.getSubClasses().containsKey(i))
//					{
//						s += "Sub-class " + String.valueOf(i) + "<br><a action=\"bypass -h npc_%objectId%_Subclass 6 " + String.valueOf(i) + "\">" + CharTemplateTable.getInstance().getClassNameById(player.getSubClasses().get(i).getClassId()) + "</a><br><br>";
//						i++;
//					}

					final StringBuilder content2 = StringUtil.startAppend(800);
					
					StringUtil.append(content2, "<table>");
					for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
					{
						
						final SubClass subClass = subList.next();
						{
							if (checkVillageMaster(subClass.getClassDefinition()))
								StringUtil.append(content2, String.format("<tr><td><button value=\"%s\" action=\"bypass -h npc_%s_Subclass 6 %d \" width=156 height=28 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>", " Lv. " + subClass.getLevel() + " " + formatClassForDisplay(subClass.getClassDefinition()), "%objectId%", subClass.getClassIndex()));

						}
					
						
						if (subList.hasNext())
						{
							final var subClass2 = subList.next();
							{							
								if (checkVillageMaster(subClass2.getClassDefinition()))
									StringUtil.append(content2, String.format("<td><button value=\"%s\" action=\"bypass -h npc_%s_Subclass 6 %d \" width=156 height=28 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>", " Lv. " + subClass2.getLevel() + " " + formatClassForDisplay(subClass2.getClassDefinition()), "%objectId%", subClass2.getClassIndex()));
							}

							
						}
						
						StringUtil.append(content2, "</tr>");
					}
					
					StringUtil.append(content2, "</table>");
					
					html.replace("%text%", content2.toString());
					break;
				case 4: // Add Subclass - Action (Subclass 4 x[x])
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("addSubclass"))
						return;
					boolean allowAddition = true;
					if (player.getTotalSubClasses() >= 24)
						allowAddition = false;
					if (player.getLevel() < 75)
						allowAddition = false;
					if (allowAddition)
						if (!player.getSubClasses().isEmpty())
							for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
							{
								final SubClass subClass = subList.next();
								if (subClass.getLevel() < 75)
								{
									allowAddition = false;
									break;
								}
							}
					/*
					 * If quest checking is enabled , verify if the character has completed the Mimir 's Elixir (Path to Subclass ) and Fate 's Whisper (A Grade Weapon ) quests by checking for instances of their unique reward items . If they both exist , remove both unique items and continue with
					 * adding the sub -class .
					 */
					if (allowAddition && isValidNewSubClass(player, paramOne))
					{
						if (!player.addSubClass(paramOne, player.getTotalSubClasses() + 1))
							return;
						
						if (player.setActiveClass(player.getTotalSubClasses()))
						{
							player.registerShortCut(new L2ShortCut(0, 0, 3, 2, -1, 1)); // attack shortcut
							
//							final L2ItemInstance cubic = player.getInventory().getItemByItemId(L2Item.CUBIC_ID);
//							if (cubic != null) player.registerShortCut(new L2ShortCut(11, 0, 1, cubic.getObjectId(), 0, 1));
//							
//							final L2ItemInstance hppot = player.getInventory().getItemByItemId(L2Item.HP_POT_ID);
//							if (hppot != null) player.registerShortCut(new L2ShortCut(10, 1, 1, hppot.getObjectId(), 0, 1));
//							final L2ItemInstance mppot = player.getInventory().getItemByItemId(L2Item.MP_POT_ID);
//							if (mppot != null) player.registerShortCut(new L2ShortCut(11, 1, 1, mppot.getObjectId(), 0, 1));
//							
//							final L2ItemInstance ressc = player.getInventory().getItemByItemId(L2Item.RES_SCROLL_ID);
//							if (ressc != null) player.registerShortCut(new L2ShortCut(0, 1, 1, ressc.getObjectId(), 0, 1));
							
							player.sendPacket(new ShortCutInit(player));
							
							html.setFile("data/html/villagemaster/SubClass_AddOk.htm");
							player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS);
						}
						else 
							return;
					}
					else
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
					break;
				case 5: // Change Class - Action
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("changeSubclass"))
						return;
					if (player.getClassIndex() == paramOne)
					{
						html.setFile("data/html/villagemaster/SubClass_Current.htm");
						break;
					}
					if (paramOne == 0)
					{
						if (!checkVillageMaster(player.getBaseClass()))
							return;
					}
					else
						try
						{
							if (!checkVillageMaster(player.getSubClasses().get(paramOne).getClassDefinition()))
								return;
						}
						catch (final NullPointerException e)
						{
							return;
						}
					if (player.setActiveClass(paramOne))
						player.sendPacket(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED); // Transfer completed.
					return;
				case 6: // Change/Cancel
					if (paramOne < 1 || paramOne > 12)
						return;
					subsAvailable = getAvailableSubClasses(player);
					if (subsAvailable == null || subsAvailable.isEmpty())
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					final StringBuilder content6 = StringUtil.startAppend(200);
					for (final PlayerClass subClass : subsAvailable)
						StringUtil.append(content6, "<a action=\"bypass -h npc_%objectId%_Subclass 7 ", String.valueOf(paramOne), " ", String.valueOf(subClass.ordinal()), "\" msg=\"1445;", "\">", formatClassForDisplay(subClass), "</a><br>");
					switch (paramOne)
					{
						case 1:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice1.htm");
							break;
						case 2:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice2.htm");
							break;
						case 3:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice3.htm");
							break;
						default:
							html.setFile("data/html/villagemaster/SubClass_ModifyChoice.htm");
					}
					html.replace("%list%", content6.toString());
					break;
				case 7: // Change Subclass - Action
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("changeSubclass"))
						return;
					if (!isValidNewSubClass(player, paramTwo))
						return;
					if (player.modifySubClass(paramOne, paramTwo))
					{
						player.abortCast();
						player.stopAllEffectsExceptThoseThatLastThroughDeath();
						player.removeCubics();
						player.setActiveClass(paramOne);
						html.setFile("data/html/villagemaster/SubClass_ModifyOk.htm");
						player.sendPacket(SystemMessageId.ADD_NEW_SUBCLASS); // Subclass added.
						
						player.registerShortCut(new L2ShortCut(0, 0, 3, 2, -1, 1)); // attack shortcut
//						
//						final L2ItemInstance cubic = player.getInventory().getItemByItemId(L2Item.CUBIC_ID);
//						if (cubic != null) player.registerShortCut(new L2ShortCut(11, 0, 1, cubic.getObjectId(), 0, 1));
//						
//						final L2ItemInstance hppot = player.getInventory().getItemByItemId(L2Item.HP_POT_ID);
//						if (hppot != null) player.registerShortCut(new L2ShortCut(10, 1, 1, hppot.getObjectId(), 0, 1));
//						final L2ItemInstance mppot = player.getInventory().getItemByItemId(L2Item.MP_POT_ID);
//						if (mppot != null) player.registerShortCut(new L2ShortCut(11, 1, 1, mppot.getObjectId(), 0, 1));
//						
//						final L2ItemInstance ressc = player.getInventory().getItemByItemId(L2Item.RES_SCROLL_ID);
//						if (ressc != null) player.registerShortCut(new L2ShortCut(0, 1, 1, ressc.getObjectId(), 0, 1));
						
						player.sendPacket(new ShortCutInit(player));
					}
					else
					{
//						player.setActiveClass(0); 
//						player.sendMessage("The sub class could not be added, you have been reverted to your base class.");
						return;
					}
					break;
			}
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%cname%", player.getClassId().getName());
			player.sendPacket(html);
		}
		else
			// this class dont know any other commands, let forward the command to the parent class
			super.onBypassFeedback(player, command);
	}
	
	protected boolean checkQuests(final L2PcInstance player)
	{
		// Noble players can add subbclasses without quests
		if (player.isNoble())
			return true;
		QuestState qs = player.getQuestState("Q234_FatesWhisper");
		if (qs == null || !qs.isCompleted())
			return false;
		qs = player.getQuestState("Q235_MimirsElixir");
		if (qs == null || !qs.isCompleted())
			return false;
		return true;
	}
	
	private final static Set<PlayerClass> getAvailableSubClasses(final L2PcInstance player)
	{
		// get player base class
		final int currentBaseId = player.getBaseClass();
//		final ClassId baseCID = ClassId.values()[currentBaseId];
		// we need 2nd occupation ID
//		final int baseClassId = currentBaseId;
//		if (baseCID.level() > 2)
//			baseClassId = baseCID.getParent().ordinal();
//		else
//			baseClassId = currentBaseId;
//		final ClassId newCID = ClassId.values()[baseClassId];
//		System.out.println(arg0);
		/**
		 * If the race of your main class is Elf or Dark Elf, you may not select each class as a subclass to the other class, and you may not select Overlord and Warsmith class as a subclass. You may not select a similar class as the subclass. The occupations classified as similar classes are as
		 * follows: Treasure Hunter, Plainswalker and Abyss Walker Hawkeye, Silver Ranger and Phantom Ranger Paladin, Dark Avenger, Temple Knight and Shillien Knight Warlocks, Elemental Summoner and Phantom Summoner Elder and Shillien Elder Swordsinger and Bladedancer Sorcerer, Spellsinger and
		 * Spellhowler
		 */
		final Set<PlayerClass> availSubs = PlayerClass.values()[currentBaseId].getAvailableSubclasses(player);
		if (availSubs != null && !availSubs.isEmpty())
			for (final Iterator<PlayerClass> availSub = availSubs.iterator(); availSub.hasNext();)
			{
				final PlayerClass pclass = availSub.next();
				// scan for already used subclasses
				final int availClassId = pclass.ordinal();
				final ClassId cid = ClassId.values()[availClassId];
				for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					final SubClass prevSubClass = subList.next();
					final ClassId subClassId = ClassId.values()[prevSubClass.getClassId()];
					if (subClassId.equalsOrChildOf(cid))
					{
						availSub.remove();
						break;
					}
				}
			}
		return availSubs;
	}
	
	/*
	 * Check new subclass classId for validity (villagemaster race/type is not contains in previous subclasses, but in allowed subclasses) Base class not added into allowed subclasses.
	 */
	private final static boolean isValidNewSubClass(final L2PcInstance player, final int classId)
	{
		if (!checkVillageMaster(classId))
			return false;
		final ClassId cid = ClassId.values()[classId];
		for (final Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
		{
			final SubClass sub = subList.next();
			final ClassId subClassId = ClassId.values()[sub.getClassId()];
			if (subClassId.equalsOrChildOf(cid))
				return false;
		}
		// get player base class
		final int currentBaseId = player.getBaseClass();
//		final ClassId baseCID = ClassId.values()[currentBaseId];
		// we need 2nd occupation ID
//		final int baseClassId;
//		if (baseCID.level() > 2)
//			baseClassId = baseCID.getParent().ordinal();
//		else
//			baseClassId = currentBaseId;
		final Set<PlayerClass> availSubs = PlayerClass.values()[currentBaseId].getAvailableSubclasses(player);
		if (availSubs == null || availSubs.isEmpty())
			return false;
		boolean found = false;
		for (final PlayerClass pclass : availSubs)
			if (pclass.ordinal() == classId)
			{
				found = true;
				break;
			}
		return found;
	}
	
	protected boolean checkVillageMasterRace(final PlayerClass pclass)
	{
		return true;
	}
	
	protected boolean checkVillageMasterTeachType(final PlayerClass pclass)
	{
		return true;
	}
	
	/*
	 * Returns true if this classId allowed for master
	 */
	public final static boolean checkVillageMaster(final int classId)
	{
		return true;
	}
	
	/*
	 * Returns true if this PlayerClass is allowed for master
	 */
	public final static boolean checkVillageMaster(final PlayerClass pclass)
	{
		return true;
	}
	
	private static final String formatClassForDisplay(final PlayerClass className)
	{
		return className.toString();
	}
	
	public static final Iterator<SubClass> iterSubClasses(final L2PcInstance player)
	{
		return player.getSubClasses().values().iterator();
	}
	
	private static final void dissolveClan(final L2PcInstance player, final int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		final L2Clan clan = player.getClan();
		if (clan.getAllyId() != 0)
		{
			player.sendPacket(SystemMessageId.CANNOT_DISPERSE_THE_CLANS_IN_ALLY);
			return;
		}
		if (clan.isAtWar())
		{
			player.sendPacket(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_WAR);
			return;
		}
		if (clan.getHasCastle() != 0 || clan.getHasHideout() != 0 || clan.getHasFort() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_OWNING_CLAN_HALL_OR_CASTLE));
			return;
		}
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, castle.getCastleId()))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE));
				return;
			}
		}
		for (Fort fort : FortManager.getInstance().getForts())
		{
			if (FortSiegeManager.getInstance().checkIsRegistered(clan, fort.getFortId()))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE));
				return;
			}
		}
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.DISSOLUTION_IN_PROGRESS);
			return;
		}
		if (Config.ALT_CLAN_DISSOLVE_DAYS > 0)
		{
			clan.setDissolvingExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_DISSOLVE_DAYS * 86400000L); // 24*60*60*1000
																													// =
																													// 86400000
			clan.updateClanInDB();
			ClanTable.getInstance().scheduleRemoveClan(clan.getClanId());
		}
		else
			ClanTable.getInstance().destroyClan(clan.getClanId());
		// The clan leader should take the XP penalty of a full death.
		//player.deathPenalty(false, _isAPC, _isAPC);
	}
	
	private static final void recoverClan(final L2PcInstance player, final int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		final L2Clan clan = player.getClan();
		clan.setDissolvingExpiryTime(0);
		clan.updateClanInDB();
	}
	
	private static final void changeClanLeader(final L2PcInstance player, final String target)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		if (player.getName().equalsIgnoreCase(target))
			return;
		// little exploit fix
		if (player.isFlying())
		{
			player.sendMessage("You must dismount the wyvern to change the clan leader.");
			return;
		}
		final L2Clan clan = player.getClan();
		final L2ClanMember member = clan.getClanMember(target);
		if (member == null)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
			sm.addString(target);
			player.sendPacket(sm);
			return;
		}
		if (!member.isOnline())
		{
			player.sendPacket(SystemMessageId.INVITED_USER_NOT_ONLINE);
			return;
		}
		final L2ItemInstance crown = player.getInventory().getItemByItemId(6841);
		if (crown != null)
		{
			if (crown.isEquipped())
				player.getInventory().unEquipItemInSlot(crown.getLocationSlot());
			player.destroyItemByItemId("CastleCrownRemoval", 6841, 1, player, true);
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addRemovedItem(crown);
			player.sendPacket(iu);
		}
		clan.setNewLeader(member);
	}
	
	private static final void createSubPledge(final L2PcInstance player, final String clanName, final String leaderName, final int pledgeType, final int minClanLvl)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		final L2Clan clan = player.getClan();
		if (clan.getLevel() < minClanLvl)
		{
			if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN_ACADEMY);
			else
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_MILITARY_UNIT);
			return;
		}
		if (!Util.isAlphaNumeric(clanName))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		if (clanName.length() < 2 || clanName.length() > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		for (final L2Clan tempClan : ClanTable.getInstance().getClans())
			if (tempClan.getSubPledge(clanName) != null)
			{
				if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
				{

					SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
					sm.addString(clanName);
					player.sendPacket(sm);
				}
				else
					player.sendPacket(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME);
				return;
			}
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
			if (clan.getClanMember(leaderName) == null || clan.getClanMember(leaderName).getPledgeType() != 0)
			{
				if (pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
				else if (pledgeType >= L2Clan.SUBUNIT_ROYAL1)
					player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
				return;
			}
		final int leaderId = pledgeType != L2Clan.SUBUNIT_ACADEMY ? clan.getClanMember(leaderName).getObjectId() : 0;
		if (clan.createSubPledge(player, pledgeType, leaderId, clanName) == null)
			return;
		SystemMessage sm;
		if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
		{

			sm = new SystemMessage(SystemMessageId.THE_S1S_CLAN_ACADEMY_HAS_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
		{

			sm = new SystemMessage(SystemMessageId.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= L2Clan.SUBUNIT_ROYAL1)
		{

			sm = new SystemMessage(SystemMessageId.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else
		{

			sm = new SystemMessage(SystemMessageId.CLAN_CREATED);
		}
		player.sendPacket(sm);
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			final L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
			final L2PcInstance leaderPlayer = leaderSubPledge.getPlayerInstance();
			if (leaderPlayer != null)
			{
				leaderPlayer.setPledgeClass(L2ClanMember.calculatePledgeClass(leaderPlayer));
				leaderPlayer.sendPacket(new UserInfo(leaderPlayer));
			}
		}
	}
	
	private static final void renameSubPledge(final L2PcInstance player, final int pledgeType, final String pledgeName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		final L2Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(pledgeType);
		if (subPledge == null)
		{
			player.sendMessage("Pledge doesn't exist.");
			return;
		}
		if (!Util.isAlphaNumeric(pledgeName))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		if (pledgeName.length() < 2 || pledgeName.length() > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		subPledge.setName(pledgeName);
		clan.updateSubPledgeInDB(subPledge.getId());
		clan.broadcastClanStatus();
		player.sendMessage("Pledge name have been changed to: " + pledgeName);
	}
	
	private static final void assignSubPledgeLeader(final L2PcInstance player, final String clanName, final String leaderName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		if (leaderName.length() > 16)
		{
			player.sendPacket(SystemMessageId.NAMING_CHARNAME_UP_TO_16CHARS);
			return;
		}
		if (player.getName().equals(leaderName))
		{
			player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			return;
		}
		final L2Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(clanName);
		if (null == subPledge || subPledge.getId() == L2Clan.SUBUNIT_ACADEMY)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_INCORRECT);
			return;
		}
		if (clan.getClanMember(leaderName) == null || clan.getClanMember(leaderName).getPledgeType() != 0)
		{
			if (subPledge.getId() >= L2Clan.SUBUNIT_KNIGHT1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
			else if (subPledge.getId() >= L2Clan.SUBUNIT_ROYAL1)
				player.sendPacket(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			return;
		}
		subPledge.setLeaderId(clan.getClanMember(leaderName).getObjectId());
		clan.updateSubPledgeInDB(subPledge.getId());
		final L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
		final L2PcInstance leaderPlayer = leaderSubPledge.getPlayerInstance();
		if (leaderPlayer != null)
		{
			leaderPlayer.setPledgeClass(L2ClanMember.calculatePledgeClass(leaderPlayer));
			leaderPlayer.sendPacket(new UserInfo(leaderPlayer));
		}
		clan.broadcastClanStatus();

		final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_SELECTED_AS_CAPTAIN_OF_S2);
		sm.addString(player.getClan().getName());
		sm.addString(leaderName);
		sm.addString(clanName);
		clan.broadcastToOnlineMembers(sm);
	}
	
	/**
	 * this displays PledgeSkillList to the player.
	 * @param player
	 */
	public static final void showPledgeSkillList(final L2PcInstance player)
	{
		if (player.getClan() == null || !player.isClanLeader())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/villagemaster/NotClanLeader.htm");
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Clan);
		boolean empty = true;
		for (final L2PledgeSkillLearn psl : SkillTreeTable.getInstance().getAvailablePledgeSkills(player))
		{
			final L2Skill sk = SkillTable.getInstance().getInfo(psl.getId(), psl.getLevel());
			if (sk == null)
				continue;
			asl.addSkill(psl.getId(), psl.getLevel(), psl.getLevel(), psl.getRepCost(), 0);
			empty = false;
		}
		if (empty)
		{
			if (player.getClan().getLevel() < 8)
			{
				//final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);

				final SystemMessage sm = new SystemMessage(SystemMessageId.DEBUFF_SLOT_FAILED);
				sm.addString("You do not have any further skills to learn. Come back when you have reached Level ");
				sm.addNumber(Math.max(player.getClan().getLevel() + 1, 5));
				player.sendPacket(sm);
			}
			else
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/villagemaster/NoMoreSkills.htm");
				player.sendPacket(html);
			}
		}
		else
			player.sendPacket(asl);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}