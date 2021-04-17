package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Iterator;
import java.util.Set;

import luna.custom.logger.LunaLogger;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.handler.itemhandlers.Gem;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Clan.SubPledge;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2PledgeSkillLearn;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.PlayerClass;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.base.SubClass;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.AcquireSkillDone;
import net.sf.l2j.gameserver.network.serverpackets.AcquireSkillList;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.StringUtil;
import net.sf.l2j.gameserver.util.Util;

public final class L2VillageMasterInstance extends L2NpcInstance
{
	public L2VillageMasterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		String[] commandStr = command.split(" ");
		String actualCommand = commandStr[0]; // Get actual command
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
			player.sendMessage("Disabled");
			return;
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
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE));
			else
				player.getClan().createAlly(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_ally"))
		{
			if (!player.isClanLeader() || player.getClan() == null)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER));
				return;
			}
			player.getClan().dissolveAlly(player);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_clan"))
		{
			dissolveClan(player, player.getClanId());
		}
		else if (actualCommand.equalsIgnoreCase("change_clan_leader"))
		{
			if (cmdParams.isEmpty())
				return;
			changeClanLeader(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("recover_clan"))
		{
			recoverClan(player, player.getClanId());
		}
		else if (actualCommand.equalsIgnoreCase("increase_clan_level"))
		{
			if (player.getClan().levelUpClan(player) && player.getClan().getLevel() > 5)
			{
				player.broadcastPacket(new MagicSkillUse(player, 5103, 1, 0, 0));
				player.broadcastPacket(new MagicSkillLaunched(player, 5103, 1));
			}
		}
		else if (actualCommand.equalsIgnoreCase("learn_clan_skills"))
		{
			showPledgeSkillList(player);
		}
		else if (command.startsWith("Subclass"))
		{
			if(!player.checkIfThirdClass())
				return;
			if(Config.ENABLE_SUBCLASS_LOGS)
			{
				String Str_command = command;
				switch (command)
				{
					case "Subclass 1":
						Str_command = "Add Subclass - Initial";
						break;
					case "Subclass 2":
						Str_command = "Change Subclass - Initial";
						break;
					case "Subclass 3":
						Str_command = "Change/Cancel Subclass - Initial";
						break;
					case "Subclass 4":
						Str_command = "Add Subclass - Action (Subclass 4 x[x])";
						break;
					case "Subclass 5":
						Str_command = "Change Class - Action";
						break;
					case "Subclass 6":
						Str_command = "Change/Cancel Subclass - Choice";
						break;
					case "Subclass 7":
						Str_command = "Change Subclass - Action";
						break;
				}
				LunaLogger.getInstance().log("subclass_logs", player.getName() +" -> " + Str_command);
			}
			int cmdChoice = Integer.parseInt(command.substring(9, 10).trim());
			// Subclasses may not be changed while a skill is in use.
			if (player.isCastingNow() || player.isAllSkillsDisabled() || player.isTransformed() || player.isOutOfControl() || player.isInStance())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE));
				return;
			}
			if (player.isInCombat())
			{
				player.sendMessage("You cannot change class while in combat");
				return;
			}
			if (player.isInDuel())
			{
				player.sendMessage("You cannot change class while in Duel");
				return;
			}
			final StringBuilder content = StringUtil.startAppend(1000, "<html><body>");
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			Set<PlayerClass> subsAvailable;
			int paramOne = 0;
			int paramTwo = 0;
			try
			{
				int endIndex = command.indexOf(' ', 11);
				if (endIndex == -1)
					endIndex = command.length();
				paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
				if (command.length() > endIndex)
					paramTwo = Integer.parseInt(command.substring(endIndex).trim());
			}
			catch (Exception NumberFormatException)
			{}
			switch (cmdChoice)
			{
				case 1: // Add Subclass - Initial
					// Avoid giving player an option to add a new sub class, if they have three already.
					if (player.getTotalSubClasses() == Config.MAX_SUBCLASS)
					{
						player.sendMessage("You can now only change one of your current sub classes.");
						return;
					}
					subsAvailable = getAvailableSubClasses(player);
					if (subsAvailable != null && !subsAvailable.isEmpty())
					{
						content.append("Add Subclass:<br>Which sub class do you wish to add?<br>");
						for (PlayerClass subClass : subsAvailable)
						{
							StringUtil.append(content, "<a action=\"bypass -h npc_", String.valueOf(getObjectId()), "_Subclass 4 ", String.valueOf(subClass.ordinal()), "\" msg=\"1268;", formatClassForDisplay(subClass), "\">", formatClassForDisplay(subClass), "</a><br>");
						}
					}
					else
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					break;
				case 2: // Change Class - Initial
					content.append("Change Subclass:<br>");
					final int baseClassId = player.getBaseClassId();
					if (player.getSubClasses().isEmpty())
					{
						StringUtil.append(content, "You can't change sub classes when you don't have a sub class to begin with.<br>" + "<a action=\"bypass -h npc_", String.valueOf(getObjectId()), "_Subclass 1\">Add subclass.</a>");
					}
					else
					{
						content.append("Which class would you like to switch to?<br>");
						if (baseClassId == player.getActiveClass())
						{
							StringUtil.append(content, CharTemplateTable.getInstance().getClassNameById(baseClassId), "&nbsp;<font color=\"LEVEL\">(Base Class)</font><br><br>");
						}
						else
						{
							StringUtil.append(content, "<a action=\"bypass -h npc_", String.valueOf(getObjectId()), "_Subclass 5 0\">", CharTemplateTable.getInstance().getClassNameById(baseClassId), "</a>&nbsp;" + "<font color=\"LEVEL\">(Base Class)</font><br><br>");
						}
						for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
						{
							SubClass subClass = subList.next();
							int subClassId = subClass.getClassId();
							if (subClassId == player.getActiveClass())
							{
								StringUtil.append(content, CharTemplateTable.getInstance().getClassNameById(subClassId), "<br>");
							}
							else
							{
								StringUtil.append(content, "<a action=\"bypass -h npc_", String.valueOf(getObjectId()), "_Subclass 5 ", String.valueOf(subClass.getClassIndex()), "\">", CharTemplateTable.getInstance().getClassNameById(subClassId), "</a><br>");
							}
						}
					}
					break;
				case 3: // Change/Cancel Subclass - Initial
					content.append("Change Subclass:<br>Which of the following sub classes would you like to change?<br>");
					int classIndex = 1;
					for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
					{
						SubClass subClass = subList.next();
						StringUtil.append(content, "Sub-class ", String.valueOf(classIndex++), "<br1>" + "<a action=\"bypass -h npc_", String.valueOf(getObjectId()), "_Subclass 6 ", String.valueOf(subClass.getClassIndex()), "\">", CharTemplateTable.getInstance().getClassNameById(subClass.getClassId()), "</a><br>");
					}
					content.append("<br>If you change a sub class, you'll start at level 40 after the 2nd class transfer.");
					break;
				case 4: // Add Subclass - Action (Subclass 4 x[x])
					boolean allowAddition = true;
					/*
					 * If the character is less than level 75 on any of their previously chosen
					 * classes then disallow them to change to their most recently added sub-class choice.
					 */
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("add subclass"))
					{
						_log.warning("Player " + player.getName() + " has performed a subclass change too fast");
						return;
					}
					if (player.isInPreDuelState() || player.isInDuel())
					{
						player.sendMessage("You may not add a new sub class while participating a duel.");
						allowAddition = false;
					}
					if (player.getLevel() < 75)
					{
						player.sendMessage("You may not add a new sub class before you are level 75 on your previous class.");
						allowAddition = false;
					}
					if (allowAddition)
					{
						if (player.isInDuel() || player.isInPreDuelState())
						{
							return;
						}
						if (!player.getSubClasses().isEmpty())
						{
							for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
							{
								SubClass subClass = subList.next();
								if (subClass.getLevel() < 75)
								{
									player.sendMessage("You may not add a new sub class before you are level 75 on your previous sub class.");
									allowAddition = false;
									break;
								}
							}
						}
					}
					/*
					 * If quest checking is enabled, verify if the character has completed the Mimir's Elixir (Path to Subclass)
					 * and Fate's Whisper (A Grade Weapon) quests by checking for instances of their unique reward items.
					 * If they both exist, remove both unique items and continue with adding the sub-class.
					 */
					if (!Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS)
					{
						QuestState qs = player.getQuestState("234_FatesWhisper");
						if (qs == null || !qs.isCompleted())
						{
							player.sendMessage("You must have completed the Fate's Whisper quest to continue adding your sub class.");
							return;
						}
						if (player.getRace() != Race.Kamael)
						{
							qs = player.getQuestState("235_MimirsElixir");
							if (qs == null || !qs.isCompleted())
							{
								player.sendMessage("You must have completed the Mimir's Elixir quest to continue adding your sub class.");
								return;
							}
						}
						// Kamael have different quest than 235
						// temporarily disabled while quest is missing XD
						else
						{
							qs = player.getQuestState("236_SeedsOfChaos");
							if (qs == null || !qs.isCompleted())
							{
								player.sendMessage("You must have completed the Seeds of Chaos quest to continue adding your sub class.");
								return;
							}
						}
					}
					////////////////// \\\\\\\\\\\\\\\\\\
					if (allowAddition)
					{
						if (player.isInPreDuelState() || player.isInPreDuelState())
						{
							player.sendMessage("Cannot add sub class while in duel state qq.");
							return;
						}
						String className = CharTemplateTable.getInstance().getClassNameById(paramOne);
						if (!player.addSubClass(paramOne, player.getTotalSubClasses() + 1))
						{
							player.sendMessage("The sub class could not be added.");
							return;
						}
						player.setActiveClass(player.getTotalSubClasses());
						player.clearPath();
						CreatureSay cs = new CreatureSay(getObjectId(), Say2.MSNCHAT, getName(), "Your classpath trees has been reseted.\r\nSet them up again.");
						CreatureSay cs2 = new CreatureSay(getObjectId(), Say2.PARTYROOM_COMMANDER, getName(), "Your classpath trees has been reseted.\r\n  Do not forget to set them up again.");
						player.sendPacket(cs2);
						player.sendPacket(cs);
						StringUtil.append(content, "Add Subclass:<br>The sub class of <font color=\"LEVEL\">", className, "</font> has been added.");
						player.sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER)); // Transfer to new class.
					}
					else
					{
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
					}
					break;
				case 5: // Change Class - Action
					/*
					 * If the character is less than level 75 on any of their previously chosen
					 * classes then disallow them to change to their most recently added sub-class choice.
					 * Note: paramOne = classIndex
					 */
					/*
					 * DrHouse: Despite this is not 100% retail like, it is here to avoid some exploits during subclass changes, specially
					 * on small servers. TODO: On retail, each village master doesn't offer any subclass that is not given by itself so player
					 * always has to move to other location to change subclass after changing previously. Thanks Aikimaniac for this info.
					 */
					if (!player.isInCombat())
					{
						if (!player.getFloodProtectors().getSubclass().tryPerformAction("change class"))
						{
							_log.warning("Player " + player.getName() + " has performed a subclass change too fast");
							return;
						}

						if (player.isInPreDuelState() || player.isInPreDuelState())
						{
							player.sendMessage("Cannot add sub class while in duel state qq.");
							return;
						}
						player.setActiveClass(paramOne);
						player.clearPath();
						CreatureSay cs = new CreatureSay(getObjectId(), Say2.MSNCHAT, getName(), "Your classpath trees has been reseted.\r\nSet them up again.");
						CreatureSay cs2 = new CreatureSay(getObjectId(), Say2.PARTYROOM_COMMANDER, getName(), "Your classpath trees has been reseted.\r\n  Do not forget to set them up again.");
						player.sendPacket(cs2);
						player.sendPacket(cs);
						player.sendPacket(new ExShowScreenMessage(1, -1, 2, 0, 0, 0, 0, true, 5000, 0, "Your classpath trees has been reseted.\r\n\r\n   Do not forget to set them up again."));
						StringUtil.append(content, "Change Subclass:<br>Your active sub class is now a <font color=\"LEVEL\">", CharTemplateTable.getInstance().getClassNameById(player.getActiveClass()), "</font>.");
						player.sendPacket(new SystemMessage(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED)); // Transfer completed.
					}
					else
						StringUtil.append(content, "Change Subclass:<br>You can't change sub when you're in combat .");
					break;
				case 6: // Change/Cancel Subclass - Choice
					content.append("Please choose a sub class to change to. If the one you are looking for is not here, " + "please seek out the appropriate master for that class.<br>" + "<font color=\"LEVEL\">Warning!</font> All classes and skills for this class will be removed.<br><br>");
					subsAvailable = getAvailableSubClasses(player);

					if (player.isInPreDuelState() || player.isInPreDuelState())
					{
						player.sendMessage("Cannot add sub class while in duel state qq.");
						return;
					}
					if (subsAvailable != null && !subsAvailable.isEmpty())
					{
						for (PlayerClass subClass : subsAvailable)
						{
							StringUtil.append(content, "<a action=\"bypass -h npc_", String.valueOf(getObjectId()), "_Subclass 7 ", String.valueOf(paramOne), " ", String.valueOf(subClass.ordinal()), "\">", formatClassForDisplay(subClass), "</a><br>");
						}
					}
					else
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					break;
				case 7: // Change Subclass - Action
					/*
					 * Warning: the information about this subclass will be removed from the
					 * subclass list even if false!
					 */

					if (player.isInPreDuelState() || player.isInPreDuelState())
					{
						player.sendMessage("Cannot add sub class while in duel state qq.");
						return;
					}
					if (!player.getFloodProtectors().getSubclass().tryPerformAction("change class"))
					{
						_log.warning("Player " + player.getName() + " has performed a subclass change too fast");
						return;
					}
					if (player.modifySubClass(paramOne, paramTwo))
					{
						player.stopAllEffects(); // all effects from old subclass stopped!
						player.clearPath();
						CreatureSay cs = new CreatureSay(getObjectId(), Say2.MSNCHAT, getName(), "Your classpath trees has been reseted.\r\nSet them up again.");
						CreatureSay cs2 = new CreatureSay(getObjectId(), Say2.PARTYROOM_COMMANDER, getName(), "Your classpath trees has been reseted.\r\n  Do not forget to set them up again.");
						player.sendPacket(cs2);
						player.sendPacket(cs);
						player.setActiveClass(paramOne);
						StringUtil.append(content, "Change Subclass:<br>Your sub class has been changed to <font color=\"LEVEL\">", CharTemplateTable.getInstance().getClassNameById(paramTwo), "</font>.");
						player.sendPacket(new SystemMessage(SystemMessageId.ADD_NEW_SUBCLASS)); // Subclass added.
					}
					else
					{

						if (player.isInPreDuelState() || player.isInPreDuelState())
						{
							player.sendMessage("Cannot add sub class while in duel state qq.");
							return;
						}
						/*
						 * This isn't good! modifySubClass() removed subclass from memory
						 * we must update _classIndex! Else IndexOutOfBoundsException can turn
						 * up some place down the line along with other seemingly unrelated
						 * problems.
						 */
						player.setActiveClass(0); // Also updates _classIndex plus switching _classid to baseclass.
						player.clearPath();
						CreatureSay cs = new CreatureSay(getObjectId(), Say2.MSNCHAT, getName(), "Your classpath trees has been reseted.\r\nSet them up again.");
						CreatureSay cs2 = new CreatureSay(getObjectId(), Say2.PARTYROOM_COMMANDER, getName(), "Your classpath trees has been reseted.\r\n  Do not forget to set them up again.");
						player.sendPacket(cs2);
						player.sendPacket(cs);
						player.sendMessage("The sub class could not be added, you have been reverted to your base class.");
						return;
					}
					break;
			}
			content.append("</body></html>");
			// If the content is greater than for a basic blank page,
			// then assume no external HTML file was assigned.
			if (content.length() > 26)
				html.setHtml(content.toString());
			player.sendPacket(html);
		}
		else
		{
			// this class dont know any other commands, let forward
			// the command to the parent class
			super.onBypassFeedback(player, command);
		}
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;
		return "data/html/villagemaster/" + pom + ".htm";
	}
	
	// Private stuff
	private static final void dissolveClan(L2PcInstance player, int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		final L2Clan clan = player.getClan();
		if (clan.getAllyId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISPERSE_THE_CLANS_IN_ALLY));
			return;
		}
		if (clan.isAtWar())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_WAR));
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
		if (player.isInsideZone(L2PcInstance.ZONE_SIEGE))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE));
			return;
		}
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.DISSOLUTION_IN_PROGRESS));
			return;
		}
		clan.setDissolvingExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_DISSOLVE_DAYS * 86400000L); // 24*60*60*1000 = 86400000
		clan.updateClanInDB();
		ClanTable.getInstance().scheduleRemoveClan(clan.getClanId());
		// The clan leader should take the XP penalty of a full death.
		player.deathPenalty(false, false, false);
	}
	
	private static final void recoverClan(L2PcInstance player, int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		final L2Clan clan = player.getClan();
		clan.setDissolvingExpiryTime(0);
		clan.updateClanInDB();
	}
	
	private static final void changeClanLeader(L2PcInstance player, String target)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		if (player.getName().equalsIgnoreCase(target))
			return;
		/*
		 * Until proper clan leader change support is done, this is a little
		 * exploit fix (leader, while fliying wyvern changes clan leader and the new leader
		 * can ride the wyvern too)
		 * DrHouse
		 */
		if (player.isFlying())
		{
			player.sendMessage("Please, stop flying");
			return;
		}
		final L2Clan clan = player.getClan();
		if (clan == null)
		{
			player.sendMessage("Clan does not exist");
			return;
		}
		for (Siege siege : SiegeManager.getInstance().getSieges())
		{
			if (siege != null && siege.getIsInProgress())
			{
				if (siege.checkIsAttacker(clan) || siege.checkIsDefender(clan) || siege.checkIsDefenderWaiting(clan))
				{
					player.sendMessage("You cannot change leaders now that your castle is in siege");
					return;
				}
			}
		}
		final L2ClanMember member = clan.getClanMember(target);
		if (member == null)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
			sm.addString(target);
			player.sendPacket(sm);
			sm = null;
			return;
		}
		if (!member.isOnline())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INVITED_USER_NOT_ONLINE));
			return;
		}
		if (Util.calculateDistance(player, member.getPlayerInstance(), true) > 200)
		{
			player.sendMessage("The new to-be clan leader must be near you");
			return;
		}
		clan.setNewLeader(member);
	}
	
	private static final void createSubPledge(L2PcInstance player, String clanName, String leaderName, int pledgeType, int minClanLvl)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		final L2Clan clan = player.getClan();
		if (clan.getLevel() < minClanLvl)
		{
			if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN_ACADEMY));
			else
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_MILITARY_UNIT));
			return;
		}
		if (!Util.isAlphaNumeric(clanName) || 2 > clanName.length())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return;
		}
		if (clanName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_TOO_LONG));
			return;
		}
		for (L2Clan tempClan : ClanTable.getInstance().getClans())
		{
			if (tempClan.getSubPledge(clanName) != null)
			{
				if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
					sm.addString(clanName);
					player.sendPacket(sm);
					sm = null;
				}
				else
					player.sendPacket(new SystemMessage(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME));
				return;
			}
		}
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			if (clan.getClanMember(leaderName) == null || clan.getClanMember(leaderName).getPledgeType() != 0)
			{
				if (pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
					player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED));
				else if (pledgeType >= L2Clan.SUBUNIT_ROYAL1)
					player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED));
				return;
			}
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
			sm = new SystemMessage(SystemMessageId.CLAN_CREATED);
		player.sendPacket(sm);
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			final L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
			final L2PcInstance leaderPlayer = leaderSubPledge.getPlayerInstance();
			if (leaderPlayer != null)
			{
				leaderPlayer.setPledgeClass(leaderSubPledge.calculatePledgeClass(leaderPlayer));
				leaderPlayer.sendPacket(new UserInfo(leaderPlayer));
				leaderPlayer.sendPacket(new ExBrExtraUserInfo(leaderPlayer));
			}
		}
	}
	
	private static final void assignSubPledgeLeader(L2PcInstance player, String clanName, String leaderName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		if (leaderName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NAMING_CHARNAME_UP_TO_16CHARS));
			return;
		}
		if (player.getName().equals(leaderName))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED));
			return;
		}
		final L2Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(clanName);
		if (null == subPledge || subPledge.getId() == L2Clan.SUBUNIT_ACADEMY)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return;
		}
		if (clan.getClanMember(leaderName) == null || (clan.getClanMember(leaderName).getPledgeType() != 0))
		{
			if (subPledge.getId() >= L2Clan.SUBUNIT_KNIGHT1)
				player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED));
			else if (subPledge.getId() >= L2Clan.SUBUNIT_ROYAL1)
				player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED));
			return;
		}
		subPledge.setLeaderId(clan.getClanMember(leaderName).getObjectId());
		clan.updateSubPledgeInDB(subPledge.getId());
		final L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
		final L2PcInstance leaderPlayer = leaderSubPledge.getPlayerInstance();
		if (leaderPlayer != null)
		{
			leaderPlayer.setPledgeClass(leaderSubPledge.calculatePledgeClass(leaderPlayer));
			leaderPlayer.sendPacket(new UserInfo(leaderPlayer));
			leaderPlayer.sendPacket(new ExBrExtraUserInfo(leaderPlayer));
		}
		clan.broadcastClanStatus();
		SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_SELECTED_AS_CAPTAIN_OF_S2);
		sm.addString(leaderName);
		sm.addString(clanName);
		clan.broadcastToOnlineMembers(sm);
		sm = null;
	}
	
	private final Set<PlayerClass> getAvailableSubClasses(L2PcInstance player)
	{
		// get player base class
		final int currentBaseId = player.getBaseClassId();
		final ClassId baseCID = ClassId.values()[currentBaseId];
		// we need 2nd occupation ID
		final int baseClassId;
		if (baseCID.level() > 2)
			baseClassId = baseCID.getParent().ordinal();
		else
			baseClassId = currentBaseId;
		/**
		 * If the race of your main class is Elf or Dark Elf,
		 * you may not select each class as a subclass to the other class.
		 * If the race of your main class is Kamael, you may not subclass any other race
		 * If the race of your main class is NOT Kamael, you may not subclass any Kamael class
		 * You may not select Overlord and Warsmith class as a subclass.
		 * You may not select a similar class as the subclass.
		 * The occupations classified as similar classes are as follows:
		 * Treasure Hunter, Plainswalker and Abyss Walker
		 * Hawkeye, Silver Ranger and Phantom Ranger
		 * Paladin, Dark Avenger, Temple Knight and Shillien Knight
		 * Warlocks, Elemental Summoner and Phantom Summoner
		 * Elder and Shillien Elder
		 * Swordsinger and Bladedancer
		 * Sorcerer, Spellsinger and Spellhowler
		 * Also, Kamael have a special, hidden 4 subclass, the inspector, which can
		 * only be taken if you have already completed the other two Kamael subclasses
		 */
		Set<PlayerClass> availSubs = PlayerClass.values()[baseClassId].getAvailableSubclasses(player);
		if (availSubs != null && !availSubs.isEmpty())
		{
			for (Iterator<PlayerClass> availSub = availSubs.iterator(); availSub.hasNext();)
			{
				PlayerClass pclass = availSub.next();
				// scan for already used subclasses
				int availClassId = pclass.ordinal();
				ClassId cid = ClassId.values()[availClassId];
				for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					SubClass prevSubClass = subList.next();
					ClassId subClassId = ClassId.values()[prevSubClass.getClassId()];
					if (subClassId.equalsOrChildOf(cid))
					{
						availSub.remove();
						break;
					}
				}
			}
		}
		return availSubs;
	}
	
	/**
	 * this displays PledgeSkillList to the player.
	 * 
	 * @param player
	 */
	public static final void showPledgeSkillList(L2PcInstance player)
	{
		if (player.getClan() == null || !player.isClanLeader())
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("data/html/villagemaster/NotClanLeader.htm");
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		L2PledgeSkillLearn[] skills = SkillTreeTable.getInstance().getAvailablePledgeSkills(player);
		AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Clan);
		int counts = 0;
		for (L2PledgeSkillLearn s : skills)
		{
			int cost = s.getRepCost();
			int itemCount = s.getItemCount();
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, itemCount);
		}
		if (counts == 0)
		{
			if (player.getClan().getLevel() < 8)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN);
				if (player.getClan().getLevel() < 5)
					sm.addNumber(5);
				else
					sm.addNumber(player.getClan().getLevel() + 1);
				player.sendPacket(sm);
				player.sendPacket(new AcquireSkillDone());
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/villagemaster/NoMoreSkills.htm");
				player.sendPacket(html);
			}
		}
		else
			player.sendPacket(asl);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private static final String formatClassForDisplay(PlayerClass className)
	{
		String classNameStr = className.toString();
		char[] charArray = classNameStr.toCharArray();
		for (int i = 1; i < charArray.length; i++)
		{
			if (Character.isUpperCase(charArray[i]))
				classNameStr = classNameStr.substring(0, i) + " " + classNameStr.substring(i);
		}
		return classNameStr;
	}
	
	public static final Iterator<SubClass> iterSubClasses(L2PcInstance player)
	{
		return player.getSubClasses().values().iterator();
	}
}
