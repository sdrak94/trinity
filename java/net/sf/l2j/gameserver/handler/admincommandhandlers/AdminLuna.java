package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import ghosts.controller.GhostController;
import ghosts.controller.GhostTemplateTable;
import ghosts.model.Ghost;
import inertia.controller.InertiaController;
import inertia.model.Inertia;
import luna.custom.LunaVariables;
import luna.custom.email.DonationCodeGenerator;
import luna.custom.handler.items.bonanzo.BonanzoData;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.GMAudit;
import net.sf.l2j.util.Rnd;

public class AdminLuna implements IAdminCommandHandler
{
	private static final String[]			ADMIN_COMMANDS	=
	{
		"admin_send_donate", "admin_flagall", "admin_flag",
		"admin_unlockdown", "admin_reward_all", "admin_reward_all_range", "admin_setfarmevent",
		"admin_announcescreen", "admin_kickfromfarmevent", "admin_reward_pt", "admin_reward_cc",
		"admin_korean_cubics_denied", "admin_korean_res_denied", "admin_korean_hero_denied",
		"admin_korean_marriage_denied", "admin_enable_custom_pvp_event", "admin_custom_pvp_event_zoneid",
		"admin_checksb", "admin_bonanzo_reload", "admin_checksb2", "admin_spawnrandom", "admin_delete_ghost",
		"admin_renderchill", "admin_addtime"
	};
	private static List<L2FenceInstance>	_fences			= new ArrayList<L2FenceInstance>();
	
	private void auditAction(String fullCommand, L2PcInstance activeChar, String target)
	{
		if (!Config.GMAUDIT)
			return;
		String[] command = fullCommand.split(" ");
		GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", command[0], (target.equals("") ? "no-target" : target), (command.length > 2 ? command[2] : ""));
	}
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_send_donate"))
		{
			try
			{
				String val = command.substring(18);
				if (!adminSendDonate(activeChar, val))
					activeChar.sendMessage("1Usage: //send_donate email ammount");
				auditAction(command, activeChar, val);
			}
			catch (StringIndexOutOfBoundsException e)
			{ // Case of missing
				// parameter
				activeChar.sendMessage("2Usage: //send_donate email ammount");
			}
		}
		else if (command.startsWith("admin_addtime"))
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			if (st.countTokens() != 2)
			{
				activeChar.sendMessage("Usage: //addtime name time");
				return false;
			}
			else
			{
				String name = st.nextToken();
				String ammount = st.nextToken();
				try
				{
					try
					{
						L2PcInstance player = L2World.getInstance().getPlayer(name);
						int time = Integer.parseInt(ammount);
						Inertia inertia = InertiaController.getInstance().fetchChill(player);
						inertia.addCredit(time * 3_600_000);
						activeChar.sendMessage("You added " + time + " hours to " + player.getName());
					}
					catch (NullPointerException e)
					{
						activeChar.sendMessage("the character: " + name + " doesn't exist.");
					}
					catch (NumberFormatException ee)
					{
						activeChar.sendMessage("Specify a numeric number after name");
					}
				}
				catch (Exception e)
				{
					activeChar.sendMessage("you fucked it up");
				}
			}
			return true;
		}
		else if (command.startsWith("admin_delete_ghost"))
		{
			if (!(activeChar.getTarget() instanceof Ghost))
			{
				activeChar.sendMessage("Target must be a ghost");
				return false;
			}
			if (activeChar.getTarget() == null)
			{
				final String[] data = command.split(" ");
				if ((data.length > 1))
				{
					int objId = Integer.parseInt(data[0]);
					Ghost player = (Ghost) L2World.getInstance().getPlayer(objId);
					GhostController.getInstance().deleteGhost(player);
				}
			}
			else
			{
				if ((activeChar.getTarget() instanceof Ghost))
					GhostController.getInstance().deleteGhost((Ghost) activeChar.getTarget());
				else
					activeChar.sendMessage("You need a ghost as target");
			}
			return true;
		}
		else if (command.startsWith("admin_renderchill"))
		{
			if (activeChar.getTarget() == null || !(activeChar.getTarget() instanceof L2PcInstance))
			{
				final String[] data = command.split(" ");
				if ((data.length > 1))
				{
					int objId = Integer.parseInt(data[0]);
					L2PcInstance player = (L2PcInstance) L2World.getInstance().getPlayer(objId);
					InertiaController.getInstance().fetchChill(player).render(activeChar);
				}
			}
			else
			{
				if ((activeChar.getTarget() instanceof L2PcInstance))
					InertiaController.getInstance().fetchChill((L2PcInstance) activeChar.getTarget()).render(activeChar);
				else
					activeChar.sendMessage("You ned a player as target");
			}
			return true;
		}
		else if (command.startsWith("admin_spawnrandom"))
		{
			int count = 1;
			if (command.contains(" "))
			{
				String ammount = command.split(" ")[1];
				count = Integer.parseInt(ammount);
			}
			for (int i = 0; i < count; i++)
			{
				ArrayList<String> _types = new ArrayList<>();
				// TODO Add method
				_types.add("DUELIST_STARTER");
				_types.add("ADVENTURER_STARTER");
				_types.add("NECRO_STARTER");
				_types.add("SAGITTARIUS_STARTER");
				_types.add("ARCHMAGE_STARTER");
				_types.add("MYSTICMUSE_STARTER");
				_types.add("STORMSCREAMER_STARTER");
				final var template = GhostTemplateTable.getInstance().getById(Rnd.get(_types));
				final var ghost = GhostController.getInstance().createGhost(template);
				GhostController.getInstance().spawnGhost(ghost, activeChar.getX(), activeChar.getY(), activeChar.getZ());
			}
			return true;
		}
		else if (command.startsWith("admin_checksb"))
		{
			if (activeChar.getTarget() != null)
			{
				L2Object target = activeChar.getTarget();
				if (target instanceof L2PcInstance)
				{
					target.getActingPlayer().checkForIncorrectSkills();
				}
			}
		}
		else if (command.startsWith("admin_checksb2"))
		{
			if (activeChar.getTarget() != null)
			{
				L2Object target = activeChar.getTarget();
				if (target instanceof L2PcInstance)
				{
					target.getActingPlayer().checkForIncorrectSkillsAndRemove();
				}
			}
		}
		else if (command.startsWith("admin_enable_custom_pvp_event"))
		{
			try
			{
				String val = command.substring(30);
				if (val.equalsIgnoreCase("true"))
				{
					LunaVariables.getInstance().set_enableCustomPvPZone(true);
					int zoneId = LunaVariables.getInstance().get_customPvPZoneId();
					if (zoneId != 0)
					{
						for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
						{
							if (player != null)
							{
								if (player.getWorldRegion().containsZone(zoneId))
								{
									player.updatePvPFlag(1);
									player.setInPvPCustomEventZone(true);
								}
							}
							else
								continue;
						}
					}
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getCustomPvPZoneStatus()));
				}
				else if (val.equalsIgnoreCase("false"))
				{
					LunaVariables.getInstance().set_enableCustomPvPZone(false);
					int zoneId = LunaVariables.getInstance().get_customPvPZoneId();
					if (zoneId != 0)
					{
						for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
						{
							if (player != null)
							{
								if (player.getWorldRegion().containsZone(zoneId))
								{
									if (!player.isInPI() || !player.isInGludin() || !player.isInHuntersVillage())
									{
										player.updatePvPFlag(0);
									}
									player.setInPvPCustomEventZone(true);
								}
								else
									continue;
							}
						}
					}
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getCustomPvPZoneStatus()));
				}
				else
				{
					activeChar.sendMessage("You fucked up, //enable_custom_pvp_event true/false");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You fucked up, //enable_custom_pvp_event true/false");
			}
		}
		else if (command.startsWith("admin_custom_pvp_event_zoneid"))
		{
			try
			{
				String val = command.substring(30);
				int zoneId = Integer.parseInt(val);
				if (zoneId != 0)
				{
					LunaVariables.getInstance().set_customPvPZoneId(zoneId);
				}
				else
				{
					activeChar.sendMessage("You fucked up, //enable_custom_pvp_event true/false");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You fucked up, //enable_custom_pvp_event true/false");
			}
		}
		else if (command.startsWith("admin_bonanzo_reload"))
		{
			try
			{
				BonanzoData.getInstance().ReloadBonanzo();
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You fucked up, //enable_custom_pvp_event true/false");
			}
		}
		else if (command.startsWith("admin_reward_pt"))
		{
			final L2Object target = activeChar.getTarget();
			if (target != null && target instanceof L2PcInstance)
			{
				if (target != activeChar)
				{
					if (!activeChar.getAccessLevel().allowTransaction())
					{
						activeChar.sendMessage("Transactions are disable for your Access Level");
						return false;
					}
				}
				try
				{
					String val = command.substring(15);
					StringTokenizer st = new StringTokenizer(val);
					if (target.getActingPlayer().getParty() == null)
					{
						activeChar.sendMessage(target.getName() + " Is not in a Party");
						return false;
					}
					else if (st.countTokens() == 3)
					{
						String id = st.nextToken();
						int idval = Integer.parseInt(id);
						String num = st.nextToken();
						long numval = Long.parseLong(num);
						String enchant = st.nextToken();
						int enchantval = Integer.parseInt(enchant);
						for (L2PcInstance targets : target.getActingPlayer().getParty().getPartyMembers())
						{
							giveItem(activeChar, (L2PcInstance) targets, idval, numval, enchantval, command.startsWith("admin_reward_pt"));
						}
					}
					else if (st.countTokens() == 2)
					{
						String id = st.nextToken();
						int idval = Integer.parseInt(id);
						String num = st.nextToken();
						long numval = Long.parseLong(num);
						for (L2PcInstance targets : target.getActingPlayer().getParty().getPartyMembers())
						{
							giveItem(activeChar, (L2PcInstance) targets, idval, numval, 0, command.startsWith("admin_reward_pt"));
						}
					}
					else if (st.countTokens() == 1)
					{
						String id = st.nextToken();
						int idval = Integer.parseInt(id);
						for (L2PcInstance targets : target.getActingPlayer().getParty().getPartyMembers())
						{
							giveItem(activeChar, (L2PcInstance) targets, idval, 1, 0, command.startsWith("admin_reward_pt"));
						}
					}
				}
				catch (StringIndexOutOfBoundsException e)
				{
					activeChar.sendMessage("Usage: //admin_reward_cc <itemId> [amount]");
				}
				catch (NumberFormatException nfe)
				{
					activeChar.sendMessage("Specify a valid number.");
				}
			}
			else
			{
				activeChar.sendMessage("Invalid target.");
			}
			// AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		}
		else if (command.startsWith("admin_reward_cc"))
		{
			final L2Object target = activeChar.getTarget();
			if (target != null && target instanceof L2PcInstance)
			{
				if (target != activeChar)
				{
					if (!activeChar.getAccessLevel().allowTransaction())
					{
						activeChar.sendMessage("Transactions are disable for your Access Level");
						return false;
					}
				}
				try
				{
					String val = command.substring(15);
					StringTokenizer st = new StringTokenizer(val);
					if (target.getActingPlayer().getParty().getCommandChannel() == null)
					{
						activeChar.sendMessage(target.getName() + " Is not in a Command Channel");
						return false;
					}
					for (L2PcInstance targets : target.getActingPlayer().getParty().getCommandChannel().getMembers())
					{
						if (st.countTokens() == 3)
						{
							String id = st.nextToken();
							int idval = Integer.parseInt(id);
							String num = st.nextToken();
							long numval = Long.parseLong(num);
							String enchant = st.nextToken();
							int enchantval = Integer.parseInt(enchant);
							giveItem(activeChar, (L2PcInstance) targets, idval, numval, enchantval, command.startsWith("admin_reward_cc"));
						}
						else if (st.countTokens() == 2)
						{
							String id = st.nextToken();
							int idval = Integer.parseInt(id);
							String num = st.nextToken();
							long numval = Long.parseLong(num);
							giveItem(activeChar, (L2PcInstance) targets, idval, numval, 0, command.startsWith("admin_reward_cc"));
						}
						else if (st.countTokens() == 1)
						{
							String id = st.nextToken();
							int idval = Integer.parseInt(id);
							giveItem(activeChar, (L2PcInstance) targets, idval, 1, 0, command.startsWith("admin_reward_cc"));
						}
					}
				}
				catch (StringIndexOutOfBoundsException e)
				{
					activeChar.sendMessage("Usage: //admin_reward_cc <itemId> [amount]");
				}
				catch (NumberFormatException nfe)
				{
					activeChar.sendMessage("Specify a valid number.");
				}
			}
			else
			{
				activeChar.sendMessage("Invalid target.");
			}
			// AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		}
		else if (command.startsWith("admin_korean_cubics_denied"))
		{
			try
			{
				String val = command.substring(27);
				if (val.equalsIgnoreCase("true"))
				{
					LunaVariables.getInstance().setKoreanCubicSkillsPrevented(true);
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getKoreanCubicSkillsPrevented()));
				}
				else if (val.equalsIgnoreCase("false"))
				{
					LunaVariables.getInstance().setKoreanCubicSkillsPrevented(false);
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getKoreanCubicSkillsPrevented()));
				}
				else
				{
					activeChar.sendMessage("You fucked up, //korean_cubics_denied true/false");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You fucked up, //setfarmevent true/false");
			}
		}
		else if (command.startsWith("admin_korean_hero_denied"))
		{
			try
			{
				String val = command.substring(25);
				if (val.equalsIgnoreCase("true"))
				{
					LunaVariables.getInstance().setKoreanHeroSkillsPrevented(true);
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getKoreanHeroSkillsPrevented()));
				}
				else if (val.equalsIgnoreCase("false"))
				{
					LunaVariables.getInstance().setKoreanHeroSkillsPrevented(false);
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getKoreanHeroSkillsPrevented()));
				}
				else
				{
					activeChar.sendMessage("You fucked up, //korean_hero_denied true/false");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You fucked up, //korean_hero_denied true/false");
			}
		}
		else if (command.startsWith("admin_korean_marriage_denied"))
		{
			try
			{
				String val = command.substring(29);
				if (val.equalsIgnoreCase("true"))
				{
					LunaVariables.getInstance().setKoreanMarriageSkillsPrevented(true);
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getKoreanMarriageSkillsPrevented()));
				}
				else if (val.equalsIgnoreCase("false"))
				{
					LunaVariables.getInstance().setKoreanMarriageSkillsPrevented(false);
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getKoreanMarriageSkillsPrevented()));
				}
				else
				{
					activeChar.sendMessage("You fucked up, //korean_marriage_denied true/false");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You fucked up, //korean_marriage_denied true/false");
			}
		}
		else if (command.startsWith("admin_korean_res_denied"))
		{
			try
			{
				String val = command.substring(24);
				if (val.equalsIgnoreCase("true"))
				{
					LunaVariables.getInstance().setKoreanRessurectionSkillsPrevented(true);
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getKoreanRessurectionSkillsPrevented()));
				}
				else if (val.equalsIgnoreCase("false"))
				{
					LunaVariables.getInstance().setKoreanRessurectionSkillsPrevented(false);
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getKoreanRessurectionSkillsPrevented()));
				}
				else
				{
					activeChar.sendMessage("You fucked up, //korean_res_denied true/false");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You fucked up, //korean_marriage_denied true/false");
			}
		}
		else if (command.startsWith("admin_announce_enchant_messages"))
		{
			try
			{
				String val = command.substring(32);
				if (val.equalsIgnoreCase("true"))
				{
					LunaVariables.getInstance().setAnnounceEnchantMessagesToPlayers(true);
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getAnnounceEnchantMessagesToPlayers()));
				}
				else if (val.equalsIgnoreCase("false"))
				{
					LunaVariables.getInstance().setAnnounceEnchantMessagesToPlayers(false);
					activeChar.sendMessage(String.valueOf(LunaVariables.getInstance().getAnnounceEnchantMessagesToPlayers()));
				}
				else
				{
					activeChar.sendMessage("You fucked up, //announce_enchant_messages true/false");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You fucked up, //korean_marriage_denied true/false");
			}
		}
		else if (command.startsWith("admin_setfarmevent"))
		{
			try
			{
				String val = command.substring(19);
				if (val.equalsIgnoreCase("true"))
				{
					LunaVariables.getInstance().setFarmEventZoneStatus(true);
				}
				else if (val.equalsIgnoreCase("false"))
				{
					LunaVariables.getInstance().setFarmEventZoneStatus(false);
				}
				else
				{
					activeChar.sendMessage("You fucked up, //setfarmevent true/false");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You fucked up, //setfarmevent true/false");
			}
		}
		else if (command.startsWith("admin_announcescreen"))
		{
			try
			{
				String val = command.substring(21);
				if (val.equalsIgnoreCase("") || val == null)
				{
					return false;
				}
				else
				{
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						player.sendPacket(new ExShowScreenMessage(1, -1, 2, 0, 0, 0, 0, true, 5000, 0, val));
					}
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You fucked up, //announcescreen text");
			}
		}
		else if (command.startsWith("admin_kickfromfarmevent"))
		{
			try
			{
				for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (player.isInFarmEvent() && !player.isGM())
					{
						player.sendPacket(new ExShowScreenMessage(1, -1, 2, 0, 0, 0, 0, true, 5000, 0, "The Event is now over.\r\n\r\n   Thank you for participating."));
						player.teleToLocation(83380, 148107, -3404, true);
					}
					else
						continue;
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("You fucked up, //kickfromfarmevent");
			}
		}
		else if (command.startsWith("admin_flagall"))
		{
			try
			{
				String val = command.substring(14);
				adminFlagPplInRange(activeChar, val);
				auditAction(command, activeChar, val);
			}
			catch (StringIndexOutOfBoundsException e)
			{ // Case of missing
				// parameter
				activeChar.sendMessage("2Usage: //admin_flagppl range");
			}
		}
		else if (command.startsWith("admin_flag"))
		{
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			if (target instanceof L2PcInstance)
				player = (L2PcInstance) target;
			else
				return false;
			player.updatePvPStatus();
			auditAction(command, activeChar, player.getName());
		}
		else if (command.startsWith("admin_unlockdown"))
		{
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			if (target instanceof L2PcInstance)
				player = (L2PcInstance) target;
			else
				return false;
			player.setLockdownTime(0);
			player.doUnLockdown();
			auditAction(command, activeChar, player.getName());
		}
		else if (command.startsWith("admin_flag_pt"))
		{
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			if (target instanceof L2PcInstance)
				player = (L2PcInstance) target;
			else
				return false;
			if (target.getActingPlayer().getParty() != null)
			{
				for (L2PcInstance plr : target.getActingPlayer().getParty().getPartyMembers())
				{
					player.updatePvPStatus();
					auditAction(command, activeChar, player.getName());
				}
			}
		}
		else if (command.startsWith("admin_reward_all"))
		{
			try
			{
				final String val = command.substring(17);
				final StringTokenizer st = new StringTokenizer(val);
				int idval = 0;
				int numval = 0;
				if (st.countTokens() == 2)
				{
					final String id = st.nextToken();
					idval = Integer.parseInt(id);
					final String num = st.nextToken();
					numval = Integer.parseInt(num);
				}
				else if (st.countTokens() == 1)
				{
					final String id = st.nextToken();
					idval = Integer.parseInt(id);
					numval = 1;
				}
				final L2Item template = ItemTable.getInstance().getTemplate(idval);
				if (template == null)
				{
					activeChar.sendMessage("This item doesn't exist.");
					return false;
				}
				if (numval > 1 && !template.isStackable())
				{
					activeChar.sendMessage("This item doesn't stack - Creation aborted.");
					return false;
				}
				List<String> rewarded = new ArrayList<>();
				for (final L2PcInstance plr : L2World.getInstance().getAllPlayers().values())
				{
					if (plr == null)
					{
						continue;
					}
					if (plr.isOnline() == 0)
					{
						continue;
					}
					if (plr == activeChar)
					{
						continue;
					}
					if (plr.getClient() == null)
					{
						continue;
					}
					if (plr.getClient().isDetached())
					{
						continue;
					}
					if (plr.isGM())
					{
						continue;
					}
					final String id = plr.getClient().getFullHwid();
					// final String id = plr.getClient().getConnection().getInetAddress().getHostAddress();
					if (rewarded.contains(id))
					{
						plr.sendMessage("you are already rewarded");
						continue;
					}
					rewarded.add(id);
					plr.getInventory().addItem("Admin", idval, numval, plr, activeChar);
					plr.sendMessage("A GM spawned " + numval + " " + template.getName() + " in your inventory.");
				}
				auditAction(command, activeChar, "all");
				activeChar.sendMessage(rewarded.size() + " players rewarded with " + template.getName());
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //reward_all <itemId> [amount]");
			}
			AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		}
		else if (command.startsWith("admin_reward_all_range"))
		{
			try
			{
				final String val = command.substring(23);
				final StringTokenizer st = new StringTokenizer(val);
				int idval = 0;
				int numval = 0;
				int rangeval = 0;
				if (st.countTokens() != 3)
				{
					final String id = st.nextToken();
					idval = Integer.parseInt(id);
					final String num = st.nextToken();
					numval = Integer.parseInt(num);
					final String range = st.nextToken();
					rangeval = Integer.parseInt(range);
				}
				int counter = 0;
				final L2Item template = ItemTable.getInstance().getTemplate(idval);
				if (template == null)
				{
					activeChar.sendMessage("This item doesn't exist.");
					return false;
				}
				if (numval > 1 && !template.isStackable())
				{
					activeChar.sendMessage("This item doesn't stack - Creation aborted.");
					return false;
				}
				List<String> rewarded = new ArrayList<>();
				for (final L2PcInstance plr : activeChar.getKnownList().getKnownPlayersInRadius(rangeval))
				{
					if (plr == null)
					{
						continue;
					}
					if (plr.isOnline() == 0)
					{
						continue;
					}
					if (plr == activeChar)
					{
						continue;
					}
					if (plr.getClient() == null)
					{
						continue;
					}
					if (plr.getClient().isDetached())
					{
						continue;
					}
					if (plr.isGM())
					{
						continue;
					}
					final String id = plr.getClient().getFullHwid();
					// final String id = plr.getClient().getConnection().getInetAddress().getHostAddress();
					if (rewarded.contains(id))
					{
						plr.sendMessage("you are already rewarded");
						continue;
					}
					rewarded.add(id);
					plr.getInventory().addItem("Admin", idval, numval, plr, activeChar);
					plr.sendMessage("A GM spawned " + numval + " " + template.getName() + " in your inventory.");
					auditAction(command, activeChar, plr.getName());
				}
				activeChar.sendMessage(rewarded.size() + " players rewarded with " + template.getName());
				auditAction(command, activeChar, rewarded.size() + " players");
			}
			catch (final Exception e)
			{
				activeChar.sendMessage("Usage: //reward_all <itemId> [amount] (range)");
			}
			AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		}
		return true;
	}
	
	private boolean adminSendDonate(L2PcInstance activeChar, String mail_ammount)
	{
		StringTokenizer st = new StringTokenizer(mail_ammount);
		if (st.countTokens() != 2)
		{
			activeChar.sendMessage("3Usage: //send_donate email ammount");
			return false;
		}
		else
		{
			String mail = st.nextToken();
			String ammount = st.nextToken();
			String mailval = "";
			int ammountval = 0;
			try
			{
				mailval = mail;
				ammountval = Integer.parseInt(ammount);
			}
			catch (Exception e)
			{
				return false;
			}
			if (mailval != null || ammountval != 0 || !mailval.equalsIgnoreCase(""))
			{
				DonationCodeGenerator.getInstance();
				DonationCodeGenerator.storeCode(mailval, ammountval);
				activeChar.sendMessage("Send " + ammountval + " Donation tokens to: " + mailval);
				auditAction(mailval, activeChar, String.valueOf(ammountval));
			}
		}
		return true;
	}
	
	private boolean adminFlagPplInRange(L2PcInstance activeChar, String range)
	{
		StringTokenizer st = new StringTokenizer(range);
		if (st.countTokens() != 1)
		{
			activeChar.sendMessage("3Usage: //admin_flagall range time(sec)");
			return false;
		}
		else
		{
			String rangestr = st.nextToken();
			int rangestrval = 0;
			try
			{
				rangestrval = Integer.parseInt(rangestr);
			}
			catch (Exception e)
			{
				return false;
			}
			if (rangestrval != 0)
			{
				try
				{
					for (L2PcInstance player : activeChar.getKnownList().getKnownPlayersInRadius(rangestrval))
					{
						if (!player.isGM())
						{
							player.updatePvPStatus();
						}
					}
				}
				catch (Exception e)
				{}
			}
		}
		return true;
	}
	
	private void giveItem(L2PcInstance activeChar, L2PcInstance target, int id, long num, int enchant, boolean bind)
	{
		final L2Item template = ItemTable.getInstance().getTemplate(id);
		if (template == null)
		{
			activeChar.sendMessage("This item doesn't exist.");
			return;
		}
		if (num > 30)
		{
			if (!template.isStackable())
			{
				activeChar.sendMessage("This item does not stack - Creation aborted.");
				return;
			}
		}
		String itemName = template.getName();
		String process = "giveItem";
		if (id == L2Item.DONATION_TOKEN)
		{
			if (!(activeChar.getName().equalsIgnoreCase("[GM]Brado") || activeChar.getName().equalsIgnoreCase("[GM]Fate") || activeChar.getName().equalsIgnoreCase("[GM]Alfie")))
				return;
			else
				process = "donation_token";
		}
		L2ItemInstance newItem = target.getInventory().addItem(process, id, num, target, activeChar);
		if (enchant > 0 && newItem.isEnchantable())
		{
			newItem.setEnchantLevel(enchant);
			itemName = "+" + enchant + " " + itemName;
		}
		if (!newItem.isStackable())
		{
			if (bind)
			{
				newItem.setUntradeableTimer(9999999910000L);
			}
			else
			{
				final long untTime = newItem.getUntradeableTime();
				if (untTime < 9999999900000L)
				{
					final long newTime = System.currentTimeMillis() + (Config.UNTRADEABLE_GM_TRADE * 60 * 60 * 1000);
					if (untTime + 3600000 < newTime)
					{
						newItem.setUntradeableTimer(newTime);
					}
				}
			}
		}
		ItemList il = new ItemList(target, true);
		target.sendPacket(il);
		activeChar.sendMessage("You have spawned " + num + " item(s) number " + id + " (" + itemName + ") in " + target.getName() + "'s inventory.");
		if (activeChar != target)
			target.sendMessage("Admin has given you " + num + " " + itemName + " in your inventory.");
	}
	
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
