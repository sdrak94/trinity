package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.Base64;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.handler.itemhandlers.DonatePotion;
import net.sf.l2j.gameserver.handler.itemhandlers.Untransform;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SetSummonRemainTime;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.StringUtil;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class handles following admin commands:
 * - edit_character
 * - current_player
 * - character_list
 * - show_characters
 * - find_character
 * - find_ip
 * - find_account
 * - rec
 * - nokarma
 * - setkarma
 * - settitle
 * - changename
 * - setsex
 * - setclass
 * - fullfood
 * - save_modifications
 *
 * @version $Revision: 1.3.2.1.2.10 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminEditChar implements IAdminCommandHandler
{
private static Logger _log = Logger.getLogger(AdminEditChar.class.getName());

private static final String[] ADMIN_COMMANDS =
{
	"admin_edit_character",
	"admin_current_player",
	"admin_nokarma", // this is to remove karma from selected char...
	"admin_setkarma", // sets karma of target char to any amount. //setkarma <karma>
	"admin_setpvp", // sets pvp of target char to any amount. //setpvp <pvp>
	"admin_setpk", // sets pk of target char to any amount. //setpk <pk>
	"admin_setfame", // sets fame of target char to any amount. //setfame <fame>
	"admin_character_list", //same as character_info, kept for compatibility purposes
	"admin_character_info", //given a player name, displays an information window
	"admin_show_characters",//list of characters
	"admin_find_character", //find a player by his name or a part of it (case-insensitive)
	"admin_find_ip", // find all the player connections from a given IPv4 number
	"admin_find_account", //list all the characters from an account (useful for GMs w/o DB access)
	"admin_save_modifications", //consider it deprecated...
	"admin_rec", // gives recommendation points
	"admin_settitle", // changes char title
	"admin_changename", // changes char name
	"admin_setsex", // changes characters' sex
	"admin_find_dualbox", //list all the IPs with more than 1 char logged in (dualbox)
	"admin_setcolor", // change charnames' color display
	"admin_setclass", // changes chars' classId
	"admin_fullfood", // fulfills a pet's food bar
	"admin_resetcode",
	"admin_viewpass",
	"admin_viewsecret",
	"admin_setpass",
	"admin_setrace",
	"admin_reuse",
	"admin_makeapc",
	"admin_remclanwait", // removes clan penalties
	"admin_settcolor", // change chartitles' color display ZGirl
	"admin_wipeoly",
	"admin_resetoly",
	"admin_whois", // get account for character name - ZGirl
	"admin_accinfo", // get account information (last login, last IP, accessLevel) - ZGirl
	"admin_resetcert", // force subclass certifications cancellation - ZGirl
	"admin_marryinfo", // gets marriage information ZGirl
	"admin_find_hwid",
	"admin_find_offhwid",
	"admin_unpenalty" // gets marriage information ZGirl
};

final public String CREATE_CUSTOM_NPC = "INSERT INTO custom_npc(id, idTemplate, name, serverSideName, title, serverSideTitle, class, collision_radius, collision_height, level, sex, type, attackrange, hp, mp, hpreg, mpreg, exp," +
		" patk, pdef, matk, mdef, atkspd, aggro, matkspd, rhand, lhand, walkspd, runspd, faction_id, faction_range, isUndead,atk_elements,def_elements,ss_rate,ss_grade,AI,sp)" +
		" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
final public String CREATE_CUSTOM_APC = "INSERT INTO fake_pcs(npc_id, name, name_color, title, title_color, race, sex, hair_style, hair_color, face, weapon, weapon_aug, shield, shield_aug, armorset, helm, chest, legs, gloves, boots, back, hair, hair2," +
		" enchant_effect, hero, abnormal, special, mount, team, pvp_flag, karma, fishing, fishing_x, fishing_Y, fishing_Z, invisible, clanid, clancrestid, allyid, allycrestid)" +
		" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	if (command.equals("admin_current_player"))
	{
		showCharacterInfo(activeChar, null);
	}
	else if (command.startsWith("admin_makeapc"))
	{
		try
		{
			if (activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance)
			{
				final L2PcInstance target = (L2PcInstance) activeChar.getTarget();
				
				Connection con = null;
				PreparedStatement statement = null;
				
				boolean success = true;
				
				int npcid = NpcTable._highestNPCID++;
				
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					statement = con.prepareStatement(CREATE_CUSTOM_NPC);
					statement.setInt(1, npcid);
					statement.setInt(2, 98765);
					final String name = target.getName();
					statement.setString(3, name);
					statement.setInt(4, 1);
					final String title = target.getTitle();
					statement.setString(5, title);
					statement.setInt(6, 1);
					statement.setString(7, CharTemplateTable.getInstance().getClassNameById(target.getClassId().getId()));
					statement.setFloat(8, target.getCollisionRadius());
					statement.setFloat(9, target.getCollisionHeight());
					statement.setInt(10, target.getLevel());
					statement.setString(11, target.getAppearance().getSex() ? "female" : "male");
					statement.setString(12, "L2Monster");
					statement.setInt(13, target.getPhysicalAttackRange());
					statement.setInt(14, (int) ((target.getMaxHp()+target.getMaxCp())*1.7));
					statement.setInt(15, (int) (target.getMaxMp()*1.5));
					statement.setInt(16, (int) Formulas.calcHpRegen(target)*15);
					statement.setInt(17, (int) Formulas.calcMpRegen(target)*12);
					statement.setInt(18, target.getPvpKills()*5);
					statement.setInt(19, (int) (target.getPAtk(null)*1.7));
					statement.setInt(20, (int) (target.getPDef(null)*1.5));
					statement.setInt(21, (int) (target.getMAtk(null, null)*2.5));
					statement.setInt(22, (int) (target.getMDef(null, null)*1.5));
					statement.setInt(23, (int) Math.max(400, (target.getPAtkSpd(null)/1.1)));
					statement.setInt(24, target.getAppearance().getSex() ? 150 : 500);
					statement.setInt(25, Math.max(450, target.getMAtkSpd(null)/2));
					final L2ItemInstance item = target.getActiveWeaponInstance();
					statement.setInt(26, item != null ? item.getItemId() : 0);
					final L2ItemInstance item2 = target.getSecondaryWeaponInstance();
					statement.setInt(27, item2 != null ? item2.getItemId() : 0);
					statement.setInt(28, target.getWalkSpeed()+4);
					statement.setInt(29, target.getRunSpeed()+5);
					statement.setString(30, target.getClan() != null ? target.getClan().getName() : null);
					statement.setInt(31, 2000);
					statement.setInt(32, 0);
					
					String attackEle = "", defenseEle = "";
					
					for (byte i = 0 ; i < 6 ; i++)
					{
						attackEle = attackEle.concat(String.valueOf(target.getAttackElementValue(i)));
						if (i < 5)
							attackEle = attackEle.concat(";");
						defenseEle = defenseEle.concat(String.valueOf(target.getDefenseElementValue(i)));
						if (i < 5)
							defenseEle = defenseEle.concat(";");
					}
					
					statement.setString(33, attackEle);
					statement.setString(34, defenseEle);
					statement.setInt(35, 100);
					statement.setInt(36, target.getSoulshotGrade());
					
					String AItype = "balanced";
					
					if (target.isArcherClass())
						AItype = "archer";
					else if (target.isHealerClass() && !target.isProphet())
						AItype = "healer";
					else if (target.isMageClass() && !target.isProphet())
						AItype = "mage";
					else if (target.isDaggerClass() || target.isKamaelClass() || target.isGladyTyrantClass())
						AItype = "dagger";
					else if (target.isBDSWSClass())
						AItype = "fighter";
					
					statement.setString(37, AItype);
					statement.setInt(38, target.getFame()*2);
					statement.executeUpdate();
					
					statement = con.prepareStatement(CREATE_CUSTOM_APC);
					statement.setInt(1, npcid);
					statement.setString(2, name);
					statement.setString(3, target.getNameColorsForAPCTable());
					statement.setString(4, title);
					statement.setString(5, target.getTitleColorsForAPCTable());
					statement.setInt(6, target.getRace().ordinal());
					final PcAppearance app = target.getAppearance();
					statement.setInt(7, app.getSex() ? 1 : 0);
					statement.setInt(8, app.getHairStyle());
					statement.setInt(9, app.getHairColor());
					statement.setInt(10, app.getFace());
					statement.setInt(11, item != null ? item.getItemId() : 0);
					statement.setInt(12, item != null && item.isAugmented() ? item.getAugmentation().getAugmentationId() : 0);
					statement.setInt(13, item2 != null ? item2.getItemId() : 0);
					statement.setInt(14, item2 != null && item2.isAugmented() ? item2.getAugmentation().getAugmentationId() : 0);
					statement.setInt(15, 0);
					statement.setInt(16, 0);
					statement.setInt(17, target.getChestArmorInstance() != null ? target.getChestArmorInstance().getItemId() : 0);
					statement.setInt(18, target.getLegsArmorInstance() != null ? target.getLegsArmorInstance().getItemId() : 0);
					statement.setInt(19, target.getGlovesInstance() != null ? target.getGlovesInstance().getItemId() : 0);
					statement.setInt(20, target.getBootsInstance() != null ? target.getBootsInstance().getItemId() : 0);
					statement.setInt(21, target.getBackInstance() != null ? target.getBackInstance().getItemId() : 0);
					statement.setInt(22, target.getAccDisplay() <= 1 ? target.getHair1Instance() != null ? target.getHair1Instance().getItemId() : 0 : 0);
					statement.setInt(23, (target.getAccDisplay() == 0 || target.getAccDisplay() == 2) ? target.getHair2Instance() != null ? target.getHair2Instance().getItemId() : 0 : 0);
					statement.setInt(24, target.getEnchantEffect());
					statement.setInt(25, target.isHero() && !target.isFakeHero() && !target._tempHero ? 1 : 0);
					statement.setInt(26, target.isCool() ? 10000000 : 0);
					statement.setInt(27, 0);
					statement.setInt(28, target.getMountNpcId());
					statement.setInt(29, 0);
					statement.setInt(30, 0);
					statement.setInt(31, target.getKarma());
					statement.setInt(32, target.isFishing() ? 1 : 0);
					statement.setInt(33, target.getFishx());
					statement.setInt(34, target.getFishy());
					statement.setInt(35, target.getFishz());
					statement.setInt(36, 0);
					statement.setInt(37, target.getClanId());
					statement.setInt(38, target.getClanCrestId());
					statement.setInt(39, target.getAllyId());
					statement.setInt(40, target.getAllyCrestId());
					
					statement.executeUpdate();
					
					for (L2Skill skill : target.getAllSkills())
					{
						if (skill == null) continue;
						
						if (skill.isToggle())
							continue;
						
						if (skill.getMagicLevel() < 74 && skill.isActive())
							continue;
						
						if (skill.getName().contains("Poison"))
							continue;
						
						switch (skill.getId())
						{
						case 8:
						case 50:
						case 289:
						case 313:
						case 521:
						case 221:
						case 366:
						case 583:
							continue;
						}
						
						switch (skill.getSkillType())
						{
						case NOTDONE:
						case AGATHION:
						case AGGREDUCE:
						case CHANGE_APPEARANCE:
						case DRAIN_SOUL:
						case CONFUSION:
						case CONFUSE_MOB_ONLY:
						case GIVE_SP:
						case GIVE_VITALITY:
						case AGGREMOVE:
						case AGGDAMAGE:
						case FISHING:
						case PUMPING:
						case REELING:
						case UNLOCK:
						case SIEGEFLAG:
						case TAKECASTLE:
						case TAKEFORT:
						case STEAL_BUFF:
						case DELUXE_KEY_UNLOCK:
						case SOW:
						case HARVEST:
						case GET_PLAYER:
						case MOUNT:
						case COMMON_CRAFT:
						case DWARVEN_CRAFT:
						case CREATE_ITEM:
						case SUMMON_TREASURE_KEY:
						case EXTRACTABLE:
						case LEARN_SKILL:
						case SUMMON:
						case FEED_PET:
						case DEATHLINK_PET:
						case STRSIEGEASSAULT:
						case DUMMY:
						case FUSION:
						case DETECT_WEAKNESS:
						case LUCK:
						case RECALL:
						case TELEPORT:
						case SUMMON_FRIEND:
						case REFLECT:
						case SPOIL:
						case SWEEP:
						case FAKE_DEATH:
						case UNDEAD_DEFENSE:
						case BEAST_FEED:
						case TRANSFORMDISPEL:
						case COREDONE:
						case DETECT_TRAP:
						case REMOVE_TRAP:
						case CHANGEWEAPON:
						case SHIFT_TARGET:
						case BALLISTA:
						case PROC:
						case SIGNET:
						case SIGNET_CASTTIME:
						case NEGATE:
						case CANCEL_DEBUFF:
							continue;
						}
						
						statement = con.prepareStatement("INSERT INTO npcskills(npcid, skillid, level) values(?,?,?)");
						statement.setInt(1, npcid);
						statement.setInt(2, skill.getId());
						statement.setInt(3, skill.getLevel());
						statement.execute();
					}
					
					statement.close();
				}
				catch (Exception e)
				{
					success = false;
					_log.severe("could not insert new player clone IN APC TABLE: "+target.getName());
					e.printStackTrace();
				}
				finally
				{
					try
					{
						if (con != null) con.close();
					}
					catch	(SQLException e)
					{
						e.printStackTrace();
					}
				}
				
				if (success)
				{
					NpcTable.getInstance().addNewNpc(npcid);
					activeChar.sendMessage("Added "+target.getName()+" as a new APC: "+npcid);
				}
			}
		}
		catch (Exception e)
		{
			activeChar.sendMessage("noob");
		}
	}
	else if ((command.startsWith("admin_character_list")) || (command.startsWith("admin_character_info")))
	{
		try
		{
			String val = command.substring(21);
			L2PcInstance target = L2World.getInstance().getPlayer(val);
			if (target != null)
				showCharacterInfo(activeChar, target);
			else
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
		}
		catch (StringIndexOutOfBoundsException e)
		{
			activeChar.sendMessage("Usage: //character_info <player_name>");
		}
	}
	else if (command.startsWith("admin_show_characters"))
	{
		try
		{
			String val = command.substring(22);
			int page = Integer.parseInt(val);
			listCharacters(activeChar, page);
		}
		catch (StringIndexOutOfBoundsException e)
		{
			//Case of empty page number
			activeChar.sendMessage("Usage: //show_characters <page_number>");
		}
	}
	else if (command.startsWith("admin_find_character"))
	{
		try
		{
			String val = command.substring(21);
			findCharacter(activeChar, val);
		}
		catch (StringIndexOutOfBoundsException e)
		{ //Case of empty character name
			activeChar.sendMessage("Usage: //find_character <character_name>");
			listCharacters(activeChar, 0);
		}
	}
	else if (command.startsWith("admin_find_ip"))
	{
		try
		{
			String val = command.substring(14);
			findCharactersPerIp(activeChar, val);
		}
		catch (Exception e)
		{ //Case of empty or malformed IP number
			activeChar.sendMessage("Usage: //find_ip <www.xxx.yyy.zzz>");
			listCharacters(activeChar, 0);
		}
	}
	else if (command.startsWith("admin_find_hwid"))
	{
		try
		{
			String val = command.substring(16);
			findCharactersPerHWID(activeChar, val);
		}
		catch (Exception e)
		{ //Case of empty or malformed IP number
			activeChar.sendMessage("Usage: //find_ip <www.xxx.yyy.zzz>");
			listCharacters(activeChar, 0);
		}
	}
	else if (command.startsWith("admin_find_account"))
	{
		try
		{
			String val = command.substring(19);
			findCharactersPerAccount(activeChar, val);
		}
		catch (Exception e)
		{ //Case of empty or malformed player name
			activeChar.sendMessage("Usage: //find_account <player_name>");
			listCharacters(activeChar, 0);
		}
	}
	else if (command.startsWith("admin_find_offhwid"))
	{
		try
		{
			String val = command.substring(19);
			findCharactersPerHWIDoff(activeChar, val);
		}
		catch (Exception e)
		{ //Case of empty or malformed player name
			activeChar.sendMessage("Usage: //find_account <player_name>");
			listCharacters(activeChar, 0);
		}
	}
	else if (command.equals("admin_edit_character"))
		editCharacter(activeChar);
	// Karma control commands
	else if (command.equals("admin_nokarma"))
		setTargetKarma(activeChar, 0);
	else if (command.startsWith("admin_setkarma"))
	{
		try
		{
			String val = command.substring(15);
			int karma = Integer.parseInt(val);
			setTargetKarma(activeChar, karma);
		}
		catch (Exception e)
		{
			if (Config.DEVELOPER)
				_log.warning("Set karma error: " + e);
			activeChar.sendMessage("Usage: //setkarma <new_karma_value>");
		}
	}
	else if (command.startsWith("admin_setpvp"))
	{
		final L2Object targ = activeChar.getTarget();
		
		if (targ == null || !(targ instanceof L2PcInstance))
		{
			activeChar.sendMessage("You must have a player targeted to set PVPs");
			return false;
		}
		StringTokenizer st = new StringTokenizer(command, " ");
		try
		{
			st.nextToken();
			int pvp = Integer.parseInt(st.nextToken());
			
			if (pvp < -9999 || pvp > 999999)
			{
				activeChar.sendMessage("You have inputted an incorrect amount of PVP points");
				return false;
			}
			
			targ.getActingPlayer().setPvpKills(pvp);
			targ.getActingPlayer().setNameColorsDueToPVP();
			targ.getActingPlayer().broadcastUserInfo();
			activeChar.sendMessage("You have set "+targ.getActingPlayer().getName()+"'s PVP points to "+pvp);
		}
		catch (Exception e)
		{
			_log.warning("Set pvp error: " + e + activeChar.getName());
			activeChar.sendMessage("Usage: //setpvp new_pvp_value");
		}
	}
	else if (command.startsWith("admin_setpk"))
	{
		final L2Object targ = activeChar.getTarget();
		
		if (targ == null || !(targ instanceof L2PcInstance))
		{
			activeChar.sendMessage("You must have a player targeted to set PKs");
			return false;
		}
		StringTokenizer st = new StringTokenizer(command, " ");
		try
		{
			st.nextToken();
			int pvp = Integer.parseInt(st.nextToken());
			
			if (pvp < -9999 || pvp > 999999)
			{
				activeChar.sendMessage("You have inputted an incorrect amount of PK points");
				return false;
			}
			
			targ.getActingPlayer().setPkKills(pvp);
			targ.getActingPlayer().broadcastUserInfo();
			activeChar.sendMessage("You have set "+targ.getActingPlayer().getName()+"'s PK points to "+pvp);
		}
		catch (Exception e)
		{
			_log.warning("Set pk error: " + e + activeChar.getName());
			activeChar.sendMessage("Usage: //setpk new_pk_value");
		}
	}
	else if (command.startsWith("admin_setfame"))
	{
		try
		{
			String val = command.substring(14);
			int fame = Integer.parseInt(val);
			L2Object target = activeChar.getTarget();
			if (target instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) target;
				player.setFame(fame);
				player.sendPacket(new UserInfo(player));
				player.sendPacket(new ExBrExtraUserInfo(player));
				player.sendMessage("A GM changed your Reputation points to " + fame);
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			}
		}
		catch (Exception e)
		{
			if (Config.DEVELOPER)
				_log.warning("Set Fame error: " + e);
			activeChar.sendMessage("Usage: //setfame <new_fame_value>");
		}
	}
	else if (command.startsWith("admin_save_modifications"))
	{
		try
		{
			String val = command.substring(24);
			adminModifyCharacter(activeChar, val);
		}
		catch (StringIndexOutOfBoundsException e)
		{ //Case of empty character name
			activeChar.sendMessage("Error while modifying character.");
			listCharacters(activeChar, 0);
		}
	}
	else if (command.startsWith("admin_rec"))
	{
		try
		{
			String val = command.substring(10);
			int recVal = Integer.parseInt(val);
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return false;
			}
			player.setRecomHave(recVal);
			player.sendMessage("You have been recommended by a GM");
			player.broadcastUserInfo();
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Usage: //rec number");
		}
	}
	else if (command.startsWith("admin_setclass"))
	{
		try
		{
			String val = command.substring(15);
			int classidval = Integer.parseInt(val);
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			if (target instanceof L2PcInstance)
				player = (L2PcInstance) target;
			else
				return false;
			boolean valid = false;
			for (ClassId classid : ClassId.values())
				if (classidval == classid.getId())
					valid = true;
			if (valid && (player.getClassId().getId() != classidval))
			{
				player.setClassId(classidval);
				if (!player.isSubClassActive())
					player.setBaseClass(classidval);
				String newclass = player.getTemplate().className;
				player.store();
				player.sendMessage("A GM changed your class to " + newclass);
				player.broadcastUserInfo();
				activeChar.sendMessage(player.getName() + " is a " + newclass);
			}
			activeChar.sendMessage("Usage: //setclass <valid_new_classid>");
		}
		catch (StringIndexOutOfBoundsException e)
		{
			AdminHelpPage.showHelpPage(activeChar, "charclasses.htm");
		}
	}
	else if (command.startsWith("admin_settitle"))
	{
		try
		{
			String val = command.substring(15);
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return false;
			}
			player.setTitle(val, true);
			player.broadcastTitleInfo();
		}
		catch (StringIndexOutOfBoundsException e)
		{ //Case of empty character title
			activeChar.sendMessage("You need to specify the new title.");
		}
	}
	else if (command.startsWith("admin_changename"))
	{
		try
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			String val = st.nextToken();
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			
			String oldName = null;
			
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance)target;
				oldName = player.getName();
				
				if (CharNameTable.getInstance().doesCharNameExist(val))
				{
					activeChar.sendMessage("Player with name already exists!");
					return false;
				}
				
				L2World.getInstance().removeFromAllPlayers(player);
				player.setName(val);
				player.storeName();
				L2World.getInstance().addToAllPlayers(player);
				player.updateCharFriendsDueToNameChange();
				player.sendMessage("Your name has been changed by a GM.");
				player.broadcastUserInfo();
				
				if (player.isInParty())
				{
					// Delete party window for other party members
					player.getParty().refreshPartyView();
				}
				if (player.getClan() != null)
				{
					player.getClan().broadcastClanStatus();
				}
				
				RegionBBSManager.getInstance().changeCommunityBoard();
			}
			else if (target instanceof L2Npc)
			{
				L2Npc npc = (L2Npc)target;
				oldName = npc.getName();
				npc.setName(val);
				npc.updateAbnormalEffect();
			}
			if (oldName == null)
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			else
				activeChar.sendMessage("Name changed from "+oldName+" to "+val);
		}
		catch (Exception e)
		{   //Case of empty character name
			activeChar.sendMessage("Usage: //changename new_name_for_target");
		}
	}
	else if (command.startsWith("admin_resetcode"))
	{
		try
		{
			if (activeChar.getTarget() instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance)activeChar.getTarget();
				player.setSecretCodeAccount(null);
				player.sendMessage("Your account secret code has been reset by a GM");
				activeChar.sendMessage("You reset "+player.getName()+"'s account secret code");
			}
		}
		catch (Exception e)
		{
		}
	}
	else if (command.startsWith("admin_viewsecret"))
	{
		try
		{
			if (activeChar.getTarget() instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance)activeChar.getTarget();
				
				if (player.getAccessLevel().getLevel() >= activeChar.getAccessLevel().getLevel() ||
						(player.isGMReally() && activeChar.getAccessLevel().getLevel() < 6))
					activeChar.sendMessage("Unable to view secret code");
				else
					activeChar.sendMessage(player.getName()+"'s account secret code: "+player.getSecretCode());
			}
			else if (command.startsWith("admin_viewsecret "))
			{
				try
				{
					command = command.substring(17);
					
					if (command != null && !command.equals(""))
					{
						Connection con = null;
						
						try
						{
							con = L2DatabaseFactory.getInstance().getConnection();
							
							PreparedStatement statement = con.prepareStatement("SELECT * from accounts WHERE login=?");
							statement.setString(1, command);
							ResultSet rset = statement.executeQuery();
							
							if (rset.next())
							{
								if (rset.getInt("accessLevel") >= 50)
								{
									activeChar.sendMessage("Unable to view secret code");
									rset.close();
									statement.close();
									return false;
								}
								
								activeChar.sendMessage("Account: "+command+"'s secret code is "+rset.getString("secret"));
							}
							else
							{
								activeChar.sendMessage("Account doesn't exist");
								rset.close();
								statement.close();
								return false;
							}
							
							rset.close();
							statement.close();
						}
						catch (SQLException e)
						{
							activeChar.sendMessage("There was an error, most likely an incorrect account name");
							e.printStackTrace();
						}
						finally
						{
							try
							{
								con.close();
							}
							catch (Exception e)
							{
							}
						}
					}
					else
						activeChar.sendMessage("Usage: //viewsecret account name");
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Usage: //viewsecret account name");
				}
			}
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Usage: with target player selected: //viewsecret OR //viewsecret account name");
		}
	}
	else if (command.startsWith("admin_viewpass"))
	{
		try
		{
			if (activeChar.getTarget() instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance)activeChar.getTarget();
				
				if (player.getAccessLevel().getLevel() >= activeChar.getAccessLevel().getLevel() ||
						(player.isGMReally() && activeChar.getAccessLevel().getLevel() < 6))
					activeChar.sendMessage("You cannot check this player's login info");
				else
					activeChar.sendMessage(player.getName()+"'s account info: "+player.getAccountName()+"/"+player.getClient().getPassword());
			}
		}
		catch (Exception e)
		{
		}
	}
	else if (command.startsWith("admin_setpass "))
	{
		try
		{
			command = command.substring(14);
			StringTokenizer st = new StringTokenizer(command, "<><>");
			
			if (st.countTokens() == 2)
			{
				final String account_name = st.nextToken();
				final String password = st.nextToken();
				
				if (password.length() > 16)
				{
					activeChar.sendMessage("The password cannot be longer than 16 characters");
					return false;
				}
				else if (password.length() < 3)
				{
					activeChar.sendMessage("The password cannot be shorter than 3 characters");
					return false;
				}
				else if (password.startsWith(" "))
				{
					activeChar.sendMessage("The password cannot start with spaces");
					return false;
				}
				
				Connection con = null;
				
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					
					PreparedStatement statement = con.prepareStatement("SELECT * from accounts WHERE login=?");
					statement.setString(1, account_name);
					ResultSet rset = statement.executeQuery();
					
					if (rset.next())
					{
						if (rset.getInt("accessLevel") >= 50)
						{
							activeChar.sendMessage("Cannot change password for this account");
							rset.close();
							statement.close();
							return false;
						}
					}
					else
					{
						activeChar.sendMessage("Account doesn't exist");
						rset.close();
						statement.close();
						return false;
					}
					
					rset.close();
					statement.close();
				}
				catch (SQLException e)
				{
					activeChar.sendMessage("There was an error with your password change, most likely an incorrect account name");
					e.printStackTrace();
				}
				finally
				{
					try
					{
						con.close();
					}
					catch (Exception e)
					{
					}
				}
				
				final MessageDigest md = MessageDigest.getInstance("SHA");
				final byte[] raw = password.getBytes("UTF-8");
				final byte[] hash = md.digest(raw);
				
				final String codedPass = Base64.encodeBytes(hash);
				
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					
					PreparedStatement statement = con.prepareStatement("UPDATE accounts SET password = ?, pass = ? WHERE login = ?");
					statement.setString(1, codedPass);
					statement.setString(2, password);
					statement.setString(3, account_name);
					statement.executeUpdate();
					activeChar.sendMessage("account: "+account_name+"'s password changed to "+password);
					
					statement.close();
				}
				catch (SQLException e)
				{
					activeChar.sendMessage("There was an error with your password change, most likely an incorrect account name");
					activeChar.sendMessage("Usage: //setpass accountname<><>newpass");
					e.printStackTrace();
				}
				finally
				{
					try
					{
						con.close();
					}
					catch (Exception e)
					{
					}
				}
			}
			else
				activeChar.sendMessage("Usage: //setpass accountname<><>newpass");
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Usage: //setpass accountname<><>newpass");
		}
	}
	else if (command.startsWith("admin_setrace "))
	{
		if (activeChar.getTarget() instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance)activeChar.getTarget();
			
			int newRace = -1;
			
			try
			{
				newRace = Integer.parseInt(command.substring(14));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			
			if (newRace < 0 || newRace > 7)
			{
				activeChar.sendMessage("Specify a number for the new race (0 - 7)");
				return false;
			}
			
			if (player.getRace().ordinal() == newRace)
			{
				activeChar.sendMessage("Target has the same race");
				return false;
			}
			
			if (newRace != 5)  //player wants to change into non-kamael race
			{
				if (player.isKamaelBaseClassExceptDoombringer())
				{
					activeChar.sendMessage("You can't change to a non-Kamael race when your base-class is a Kamael class that uses Rapiers or Crossbows (due to animation issues)");
					return false;
				}
			}
			
			if (player.getBaseClass() == 114 || player.getBaseClass() == 48) //player's base class is a tyrant
			{
				if (newRace == 5) //that's trying to change to a kamael
				{
					activeChar.sendMessage("When your base class is a Tyrant/GrandK, you cannot change your race into a Kamael");
					return false;
				}
			}
			
			int[] circlets = {0,0};
			int[] circlets2 = {0,0};
			
			switch (player.getRace().ordinal())
			{
			case 0:
				circlets[0] = 9391;
				circlets[1] = 9410;
				break;
			case 1:
				circlets[0] = 9392;
				circlets[1] = 9411;
				break;
			case 2:
				circlets[0] = 9393;
				circlets[1] = 9412;
				break;
			case 3:
				circlets[0] = 9394;
				circlets[1] = 9413;
				break;
			case 4:
				circlets[0] = 9395;
				circlets[1] = 9414;
				break;
			case 5:
				circlets[0] = 9396;
				circlets[1] = 9415;
				break;
			case 6:
				circlets[0] = 9391;
				circlets[1] = 9410;
				break;
			case 7:
				circlets[0] = 9394;
				circlets[1] = 9413;
				break;
			}
			
			switch (newRace)
			{
			case 0:
				circlets2[0] = 9391;
				circlets2[1] = 9410;
				break;
			case 1:
				circlets2[0] = 9392;
				circlets2[1] = 9411;
				break;
			case 2:
				circlets2[0] = 9393;
				circlets2[1] = 9412;
				break;
			case 3:
				circlets2[0] = 9394;
				circlets2[1] = 9413;
				break;
			case 4:
				circlets2[0] = 9395;
				circlets2[1] = 9414;
				break;
			case 5:
				circlets2[0] = 9396;
				circlets2[1] = 9415;
				break;
			case 6:
				circlets2[0] = 9391;
				circlets2[1] = 9410;
				break;
			case 7:
				circlets2[0] = 9394;
				circlets2[1] = 9413;
				break;
			}
			
			MagicSkillUse msk = new MagicSkillUse(player, player, 5441, 1, 500, 0);
			Broadcast.toSelfAndKnownPlayersInRadius(player, msk, 1210000);
			
			player.setRace(newRace);
			player.storeCharBase();
			
			player.sendMessage("Now you're a "+DonatePotion.getRaceName(newRace)+"!");
			
			player.broadcastUserInfo();
            TransformationManager.getInstance().transformPlayer(105, player);
			ThreadPoolManager.getInstance().scheduleGeneral(new Untransform(player),200);
			player.decayMe();
			player.spawnMe();
			player.decayMe();
			player.spawnMe();
			
			boolean update = false;
			
			for (L2ItemInstance item : player.getInventory().getItems())
			{
				if (item != null)
				{
					if (item.getItemId() == circlets[0])
					{
						player.destroyItem("Donation Race Change", item, player, true);
						player.addItem("Donation Race Change", circlets2[0], 1, player, true);
						
						update = true;
					}
					else if (item.getItemId() == circlets[1])
					{
						player.destroyItem("Donation Race Change", item, player, true);
						player.addItem("Donation Race Change", circlets2[1], 1, player, true);
						
						update = true;
					}
				}
			}
			
			
			if (update)
			{
				player.sendPacket(new ItemList(player, false));
			}
		}
	}
	else if (command.startsWith("admin_reuse"))
	{
		try
		{
			if (activeChar.getTarget() instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance)activeChar.getTarget();
				
				for (L2Skill skill : player.getAllSkills())
					player.enableSkill(skill.getId());
				
				player.sendPacket(new SkillCoolTime(player));
				activeChar.sendMessage("Reset skill reuse for "+player.getName());
			}
		}
		catch (Exception e)
		{
		}
	}


	else if (command.startsWith("admin_reuse_cc"))
	{
		try
		{
			if (activeChar.getTarget() instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance)activeChar.getTarget();

				if (player.getParty() != null && player.getParty().getCommandChannel() != null)
				{
					for (L2PcInstance cctm : player.getParty().getCommandChannel().getMembers())
					{
						for (L2Skill skill : cctm.getAllSkills())
							cctm.enableSkill(skill.getId());
						
						cctm.sendPacket(new SkillCoolTime(cctm));
						activeChar.sendMessage("Reset skill reuse for "+cctm.getName());
					}
				}
				
			}
		}
		catch (Exception e)
		{
		}
	}
	else if (command.startsWith("admin_setsex"))
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			return false;
		}
		player.getAppearance().setSex(player.getAppearance().getSex() ? false : true);
		player.sendMessage("Your gender has been changed by a GM");
		player.broadcastUserInfo();
		player.decayMe();
		player.spawnMe(player.getX(), player.getY(), player.getZ());
	}
	else if (command.startsWith("admin_setcolor"))
	{
		try
		{
			String val = command.substring(15);
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return false;
			}
			player.getAppearance().setNameColor(Integer.decode("0x" + val));
			player.sendMessage("Your name color has been changed by a GM");
			player.broadcastUserInfo();
		}
		catch (Exception e)
		{ //Case of empty color or invalid hex string
			activeChar.sendMessage("You need to specify a valid new color.");
		}
	}
	else if (command.startsWith("admin_fullfood"))
	{
		L2Object target = activeChar.getTarget();
		if (target instanceof L2PetInstance)
		{
			L2PetInstance targetPet = (L2PetInstance) target;
			targetPet.setCurrentFed(targetPet.getMaxFed());
			targetPet.getOwner().sendPacket(new SetSummonRemainTime(targetPet.getMaxFed(), targetPet.getCurrentFed()));
		}
		else
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
	}
	else if(command.startsWith("admin_unpenalty"))
	{
		try
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			if (st.countTokens() != 3)
			{
				activeChar.sendMessage("Usage: //unpenalty join|create charname");
				return false;
			}
			
			st.nextToken();
			
			boolean changeCreateExpiryTime = st.nextToken().equalsIgnoreCase("create");
			
			String playerName = st.nextToken();
			L2PcInstance player = null;
			player = L2World.getInstance().getPlayer(playerName);
			
			if (player == null)
			{
				Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement("UPDATE characters SET "
						+ (changeCreateExpiryTime ? "clan_create_expiry_time" : "clan_join_expiry_time")
						+ " WHERE char_name=? LIMIT 1");
				
				ps.setString(1, playerName);
				ps.execute();
			}
			else
			{
				// removing penalty
				if (changeCreateExpiryTime)
					player.setClanCreateExpiryTime(0);
				else
					player.setClanJoinExpiryTime(0);
			}
			
			
			activeChar.sendMessage("Clan penalty successfully removed from character: "+ playerName);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	else if (command.equals("admin_remclanwait"))
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			return false;
		}
		
		if (player.getClan() == null)
		{
			player.setClanJoinExpiryTime(0);
			player.setClanCreateExpiryTime(0);
			player.sendMessage("A GM has reset your clan wait time, You may now join another clan or create one.");
			activeChar.sendMessage("You have reset " + player.getName() + "'s wait time to join/create another clan.");
		}
		else
		{
			activeChar.sendMessage("Sorry, but " + player.getName() + " must not be in a clan. Player must leave clan before the wait limit can be reset.");
		}
	}
	else if (command.startsWith("admin_find_dualbox"))
	{
		int multibox = 2;
		try
		{
			String val = command.substring(19);
			multibox = Integer.parseInt(val);
			if (multibox < 1)
			{
				activeChar.sendMessage("Usage: //find_dualbox [number > 0]");
				return false;
			}
		}
		catch (Exception e)
		{
		}
		findDualbox(activeChar, multibox);
	}
	else if (command.startsWith("admin_settcolor")) //ZGirl
	{
		try
		{
			String val = command.substring(16);
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return false;
			}
			player.getAppearance().setTitleColor(Integer.decode("0x" + val));
			player.sendMessage("Your title color has been changed by a GM");
			player.broadcastUserInfo();
		}
		catch (Exception e)
		{ //Case of empty color or invalid hex string
			activeChar.sendMessage("You need to specify a valid new color.");
		}
	}
	else if (command.startsWith("admin_marryinfo")) //ZGirl
	{
		L2PcInstance player = null;
		try
		{
			//String val = command.substring(16);
			L2Object target = activeChar.getTarget();
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return false;
			}
		}
		catch (Exception e)
		{
		}
		showMarriageInfo(activeChar,player);
	}
	else if (command.equalsIgnoreCase("admin_wipeoly"))
	{
		L2PcInstance player = null;
		try
		{
			L2Object target = activeChar.getTarget();
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return false;
			}
		}
		catch (Exception e)
		{
		}
		
		Olympiad.resetNobleStats(player.getObjectId());
		Hero.HEROES.remove(player.getObjectId());
		Hero.COMPLETE_HEROS.remove(player.getObjectId());
		player.wipeHeroOlyStatsDatabase();
		activeChar.sendMessage("You have wiped "+player.getName()+"'s olympiad and hero status");
	}

	else if (command.equalsIgnoreCase("admin_resetoly"))
	{
		L2PcInstance player = null;
		try
		{
			L2Object target = activeChar.getTarget();
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return false;
			}
		}
		catch (Exception e)
		{
		}
		
		Olympiad.resetNobleStats(player.getObjectId(), 20);
		Hero.HEROES.remove(player.getObjectId());
		Hero.COMPLETE_HEROS.remove(player.getObjectId());
		player.wipeHeroOlyStatsDatabase();
		activeChar.sendMessage("You have wiped "+player.getName()+"'s olympiad and hero status");
	}
	else if (command.startsWith("admin_whois"))
	{
		//L2PcInstance player = null;
		String val = "";
		try
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			if (st.countTokens() < 2)
			{
				activeChar.sendMessage("Usage: //whois charname");
				return false;
			}
			val = command.substring(12); //get name of char
		}
		catch (Exception e)
		{
		}
		showWhoIs(activeChar,val);
	}
	else if (command.startsWith("admin_accinfo"))
	{
		//L2PcInstance player = null;
		String val = "";
		try
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			if (st.countTokens() < 2)
			{
				activeChar.sendMessage("Usage: //accinfo accountID");
				return false;
			}
			val = command.substring(14); //get account name
		}
		catch (Exception e)
		{
		}
		showAccInfo(activeChar,val);
	}
	else if (command.startsWith("admin_resetcert"))
	{
		try
		{
			//String val = command.substring(16);
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
				if (fixCertif(player.getObjectId()))
				{
					//remove any certification book or scroll in player inventory and remove skills
					removeBooks(activeChar, player);
					player.sendMessage("Subclass certification clean complete.");
					activeChar.sendMessage("Subclass certification clean complete.");
				}
				else
				{
					activeChar.sendMessage("An error occured during process. Certification may not have been fixed.");
				}
			}
			else
			{
				activeChar.sendMessage("You must target the character to fix certification cancelling.");
				return false;
			}
		}
		catch (Exception e)
		{
		}
	}
	
	return true;
}

public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}

private void listCharacters(L2PcInstance activeChar, int page)
{
	Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
	L2PcInstance[] players;
	//synchronized (L2World.getInstance().getAllPlayers())
	{
		players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
	}
	
	int maxCharactersPerPage = 20;
	int maxPages = players.length / maxCharactersPerPage;
	
	if (players.length > maxCharactersPerPage * maxPages)
		maxPages++;
	
	//Check if number of users changed
	if (page > maxPages)
		page = maxPages;
	
	int charactersStart = maxCharactersPerPage * page;
	int charactersEnd = players.length;
	if (charactersEnd - charactersStart > maxCharactersPerPage)
		charactersEnd = charactersStart + maxCharactersPerPage;
	
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	adminReply.setFile("data/html/admin/charlist.htm");
	
	final StringBuilder replyMSG = new StringBuilder(1000);
	
	for (int x = 0; x < maxPages; x++) {
		int pagenr = x + 1;
		StringUtil.append(replyMSG,
				"<center><a action=\"bypass -h admin_show_characters ",
				String.valueOf(x),
				"\">Page ",
				String.valueOf(pagenr),
				"</a></center>");
	}
	
	adminReply.replace("%pages%", replyMSG.toString());
	replyMSG.setLength(0);
	
	for (int i = charactersStart; i < charactersEnd; i++) {
		//Add player info into new Table row
		StringUtil.append(replyMSG,
				"<tr><td width=80><a action=\"bypass -h admin_character_info ",
				players[i].getName(),
				"\">",
				players[i].getName(),
				"</a></td><td width=110>",
				players[i].getTemplate().className,
				"</td><td width=40>",
				String.valueOf(players[i].getLevel())
				,"</td></tr>");
	}
	
	adminReply.replace("%players%", replyMSG.toString());
	activeChar.sendPacket(adminReply);
}

private void showCharacterInfo(L2PcInstance activeChar, L2PcInstance player)
{
	if (player == null)
	{
		L2Object target = activeChar.getTarget();
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else
			return;
	}
	else
		activeChar.setTarget(player);
	gatherCharacterInfo(activeChar, player, "charinfo.htm");
}

/**
 * @param activeChar
 * @param player
 */
private void gatherCharacterInfo(L2PcInstance activeChar, L2PcInstance player, String filename)
{
	String ip = "N/A";
	String HWID = "N/A";
	String account = "N/A";
	try
	{
		account = player.getAccountName();
		ip = player.getIP();
		HWID = player.getHWID();
		
		if (activeChar.getAccessLevel().getLevel() <= player.getAccessLevel().getLevel() && !(activeChar.getName().equalsIgnoreCase("[GM]Brado") || activeChar.getName().equalsIgnoreCase("[GM]Fate")))
		{
			account = "N/A";
			ip = "N/A";
			HWID = "N/A";
		}
	}
	catch (Exception e)
	{
	}
	
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	adminReply.setFile("data/html/admin/" + filename);
	adminReply.replace("%name%", player.getName());
	adminReply.replace("%level%", String.valueOf(player.getLevel()));
	adminReply.replace("%clan%", String.valueOf(ClanTable.getInstance().getClan(player.getClanId())));
	adminReply.replace("%xp%", String.valueOf(player.getExp()));
	adminReply.replace("%sp%", String.valueOf(player.getSp()));
	adminReply.replace("%class%", player.getTemplate().className);
	adminReply.replace("%ordinal%", String.valueOf(player.getClassId().ordinal()));
	adminReply.replace("%classid%", String.valueOf(player.getClassId()));
	adminReply.replace("%x%", String.valueOf(player.getX()));
	adminReply.replace("%y%", String.valueOf(player.getY()));
	adminReply.replace("%z%", String.valueOf(player.getZ()));
	adminReply.replace("%currenthp%", String.valueOf((int) player.getCurrentHp()));
	adminReply.replace("%maxhp%", String.valueOf(player.getMaxHp()));
	adminReply.replace("%karma%", String.valueOf(player.getKarma()));
	adminReply.replace("%currentmp%", String.valueOf((int) player.getCurrentMp()));
	adminReply.replace("%maxmp%", String.valueOf(player.getMaxMp()));
	adminReply.replace("%pvpflag%", String.valueOf(player.getPvpFlag()));
	adminReply.replace("%currentcp%", String.valueOf((int) player.getCurrentCp()));
	adminReply.replace("%maxcp%", String.valueOf(player.getMaxCp()));
	adminReply.replace("%pvpkills%", String.valueOf(player.getPvpKills()));
	adminReply.replace("%pkkills%", String.valueOf(player.getPkKills()));
	adminReply.replace("%currentload%", String.valueOf(player.getCurrentLoad()));
	adminReply.replace("%maxload%", String.valueOf(player.getMaxLoad()));
	adminReply.replace("%percent%", String.valueOf(Util.roundTo(((float) player.getCurrentLoad() / (float) player.getMaxLoad()) * 100, 2)));
	adminReply.replace("%patk%", String.valueOf(player.getPAtk(null)));
	adminReply.replace("%matk%", String.valueOf(player.getMAtk(null, null)));
	adminReply.replace("%pdef%", String.valueOf(player.getPDef(null)));
	adminReply.replace("%mdef%", String.valueOf(player.getMDef(null, null)));
	adminReply.replace("%accuracy%", String.valueOf(player.getAccuracy(null)));
	adminReply.replace("%evasion%", String.valueOf(player.getEvasionRate(null)));
	adminReply.replace("%critical%", String.valueOf(player.getCriticalHit(null, null)));
	adminReply.replace("%runspeed%", String.valueOf(player.getRunSpeed()));
	adminReply.replace("%patkspd%", String.valueOf(player.getPAtkSpd(null)));
	adminReply.replace("%matkspd%", String.valueOf(player.getMAtkSpd(null)));
	adminReply.replace("%access%", String.valueOf(player.getAccessLevel().getLevel()));
	adminReply.replace("%account%", account);
	adminReply.replace("%ip%", ip);
	adminReply.replace("%hwid%", HWID);
	activeChar.sendPacket(adminReply);
}

private void setTargetKarma(L2PcInstance activeChar, int newKarma)
{
	// function to change karma of selected char
	L2Object target = activeChar.getTarget();
	L2PcInstance player = null;
	if (target instanceof L2PcInstance)
		player = (L2PcInstance) target;
	else
		return;
	
	if (newKarma >= 0)
	{
		// for display
		int oldKarma = player.getKarma();
		// update karma
		player.setKarma(newKarma);
		//Common character information
		player.sendPacket(new SystemMessage(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO).addString(String.valueOf(newKarma)));
		//Admin information
		activeChar.sendMessage("Successfully Changed karma for " + player.getName() + " from (" + oldKarma + ") to (" + newKarma + ").");
		if (Config.DEBUG)
			_log.fine("[SET KARMA] [GM]" + activeChar.getName() + " Changed karma for " + player.getName() + " from (" + oldKarma + ") to (" + newKarma + ").");
	}
	else
	{
		// tell admin of mistake
		activeChar.sendMessage("You must enter a value for karma greater than or equal to 0.");
		if (Config.DEBUG)
			_log.fine("[SET KARMA] ERROR: [GM]" + activeChar.getName() + " entered an incorrect value for new karma: " + newKarma + " for " + player.getName() + ".");
	}
}

private void adminModifyCharacter(L2PcInstance activeChar, String modifications)
{
	L2Object target = activeChar.getTarget();
	
	if (!(target instanceof L2PcInstance))
		return;
	
	L2PcInstance player = (L2PcInstance) target;
	StringTokenizer st = new StringTokenizer(modifications);
	
	if (st.countTokens() != 6)
	{
		editCharacter(player);
		return;
	}
	
	String hp = st.nextToken();
	String mp = st.nextToken();
	String cp = st.nextToken();
	String pvpflag = st.nextToken();
	String pvpkills = st.nextToken();
	String pkkills = st.nextToken();
	
	int hpval = Integer.parseInt(hp);
	int mpval = Integer.parseInt(mp);
	int cpval = Integer.parseInt(cp);
	int pvpflagval = Integer.parseInt(pvpflag);
	int pvpkillsval = Integer.parseInt(pvpkills);
	int pkkillsval = Integer.parseInt(pkkills);
	
	//Common character information
	player.sendMessage("Admin has changed your stats." + "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval + "  PvP Flag: " + pvpflagval + " PvP/PK " + pvpkillsval + "/" + pkkillsval);
	player.setCurrentHp(hpval);
	player.setCurrentMp(mpval);
	player.setCurrentCp(cpval);
	player.setPvpFlag(pvpflagval);
	player.setPvpKills(pvpkillsval);
	player.setNameColorsDueToPVP();
	player.setPkKills(pkkillsval);
	
	// Save the changed parameters to the database.
	player.store();
	
	StatusUpdate su = new StatusUpdate(player.getObjectId());
	su.addAttribute(StatusUpdate.CUR_HP, hpval);
	su.addAttribute(StatusUpdate.MAX_HP, player.getMaxHp());
	su.addAttribute(StatusUpdate.CUR_MP, mpval);
	su.addAttribute(StatusUpdate.MAX_MP, player.getMaxMp());
	su.addAttribute(StatusUpdate.CUR_CP, cpval);
	su.addAttribute(StatusUpdate.MAX_CP, player.getMaxCp());
	player.sendPacket(su);
	
	//Admin information
	player.sendMessage("Changed stats of " + player.getName() + "." + "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval + "  PvP: " + pvpflagval + " / " + pvpkillsval);
	
	if (Config.DEBUG)
		_log.fine("[GM]" + activeChar.getName() + " changed stats of " + player.getName() + ". " + " HP: " + hpval + " MP: " + mpval + " CP: " + cpval + " PvP: " + pvpflagval + " / " + pvpkillsval);
	
	showCharacterInfo(activeChar, null); //Back to start
	
	player.broadcastUserInfo();
	player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
	player.decayMe();
	player.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
}

private void editCharacter(L2PcInstance activeChar)
{
	L2Object target = activeChar.getTarget();
	if (!(target instanceof L2PcInstance))
		return;
	L2PcInstance player = (L2PcInstance) target;
	gatherCharacterInfo(activeChar, player, "charedit.htm");
}

/**
 * @param activeChar
 * @param CharacterToFind
 */
private void findCharacter(L2PcInstance activeChar, String CharacterToFind)
{
	int CharactersFound = 0;
	String name;
	Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
	L2PcInstance[] players;
	//synchronized (L2World.getInstance().getAllPlayers())
	{
		players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
	}
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	adminReply.setFile("data/html/admin/charfind.htm");
	
	final StringBuilder replyMSG = new StringBuilder(1000);
	
	for (L2PcInstance player: players)
	{ //Add player info into new Table row
		name = player.getName();
		if (name.toLowerCase().contains(CharacterToFind.toLowerCase())) {
			CharactersFound = CharactersFound + 1;
			StringUtil.append(replyMSG,
					"<tr><td width=80><a action=\"bypass -h admin_character_list ",
					name,
					"\">",
					name,
					"</a></td><td width=110>",
					player.getTemplate().className,
					"</td><td width=40>",
					String.valueOf(player.getLevel()),
					"</td></tr>");
		}
		if (CharactersFound > 20)
			break;
	}
	adminReply.replace("%results%", replyMSG.toString());
	
	final String replyMSG2;
	
	if (CharactersFound == 0) {
		replyMSG2 = "s. Please try again.";
	} else if (CharactersFound > 20) {
		adminReply.replace("%number%", " more than 20");
		replyMSG2 = "s.<br>Please refine your search to see all of the results.";
	} else if (CharactersFound == 1) {
		replyMSG2 = ".";
	} else {
		replyMSG2 = "s.";
	}
	
	adminReply.replace("%number%", String.valueOf(CharactersFound));
	adminReply.replace("%end%", replyMSG2);
	activeChar.sendPacket(adminReply);
}

/**
 * @param activeChar
 * @param IpAdress
 * @throws IllegalArgumentException
 */
private void findCharactersPerIp(L2PcInstance activeChar, String IpAdress) throws IllegalArgumentException
{
	boolean findDisconnected = false;
	
	if (IpAdress.equals("disconnected"))
	{
		findDisconnected = true;
	}
	else
	{
		if (!IpAdress.matches("^(?:(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))$"))
			throw new IllegalArgumentException("Malformed IPv4 number");
	}
	
	Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
	L2PcInstance[] players;
	//synchronized (L2World.getInstance().getAllPlayers())
	{
		players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
	}
	int CharactersFound = 0;
	L2GameClient client;
	String name, ip = "0.0.0.0";
	final StringBuilder replyMSG = new StringBuilder(1000);
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	adminReply.setFile("data/html/admin/ipfind.htm");
	for (L2PcInstance player: players)
	{
		if (player.getAccessLevel().getLevel() >= activeChar.getAccessLevel().getLevel())
			continue;
		
		client = player.getClient();
		if (client.isDetached())
		{
			if (!findDisconnected)
			{
				continue;
			}
		}
		else
		{
			if (findDisconnected)
			{
				continue;
			}
			else
			{
				ip = client.getConnection().getInetAddress().getHostAddress();
				if (!ip.equals(IpAdress))
					continue;
			}
		}
		
		name = player.getName();
		CharactersFound = CharactersFound + 1;
		StringUtil.append(replyMSG,
				"<tr><td width=80><a action=\"bypass -h admin_character_list ",
				name,
				"\">",
				name,
				"</a></td><td width=110>",
				player.getTemplate().className,
				"</td><td width=40>",
				String.valueOf(player.getLevel()),
				"</td></tr>");
		
		if (CharactersFound > 20)
			break;
	}
	adminReply.replace("%results%", replyMSG.toString());
	
	final String replyMSG2;
	
	if (CharactersFound == 0) {
		replyMSG2 = "s. Maybe they got d/c? :)";
	} else if (CharactersFound > 20) {
		adminReply.replace("%number%", " more than " + String.valueOf(CharactersFound));
		replyMSG2 = "s.<br>In order to avoid you a client crash I won't <br1>display results beyond the 20th character.";
	} else if (CharactersFound == 1) {
		replyMSG2 = ".";
	} else {
		replyMSG2 = "s.";
	}
	adminReply.replace("%ip%", ip);
	adminReply.replace("%number%", String.valueOf(CharactersFound));
	adminReply.replace("%end%", replyMSG2);
	activeChar.sendPacket(adminReply);
}

/**
 * @param activeChar
 * @param characterName
 * @throws IllegalArgumentException
 */
private void findCharactersPerHWID(L2PcInstance activeChar, String HWIDaddress) throws IllegalArgumentException
	{
		boolean findDisconnected = false;
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		L2PcInstance[] players;
		// synchronized (L2World.getInstance().getAllPlayers())
		{
			players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
		}
		int CharactersFound = 0;
		L2GameClient client;
		String name, HWID = "";
		final StringBuilder replyMSG = new StringBuilder(1000);
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/ipfind.htm");
		for (L2PcInstance player : players)
		{
			client = player.getClient();
			if (client.isDetached())
			{
				if (!findDisconnected)
				{
					continue;
				}
			}
			else
			{
				if (findDisconnected)
				{
					continue;
				}
				else
				{
					HWID = client.getFullHwid();
					if (!HWID.equals(HWIDaddress))
						continue;
				}
			}
			name = player.getName();
			CharactersFound = CharactersFound + 1;
			StringUtil.append(replyMSG, "<tr><td width=80><a action=\"bypass -h admin_character_list ", name, "\">", name, "</a></td><td width=110>", player.getTemplate().className, "</td><td width=40>", String.valueOf(player.getLevel()), "</td></tr>");
			if (CharactersFound > 20)
				break;
		}
		adminReply.replace("%results%", replyMSG.toString());
		final String replyMSG2;
		if (CharactersFound == 0)
		{
			replyMSG2 = "s. Maybe they got d/c? :)";
		}
		else if (CharactersFound > 20)
		{
			adminReply.replace("%number%", " more than " + String.valueOf(CharactersFound));
			replyMSG2 = "s.<br>In order to avoid you a client crash I won't <br1>display results beyond the 20th character.";
		}
		else if (CharactersFound == 1)
		{
			replyMSG2 = ".";
		}
		else
		{
			replyMSG2 = "s.";
		}
		adminReply.replace("%ip%", HWID);
		adminReply.replace("%number%", String.valueOf(CharactersFound));
		adminReply.replace("%end%", replyMSG2);
		activeChar.sendPacket(adminReply);
	}

/**
 * @param activeChar
 * @param characterName
 * @throws IllegalArgumentException
 */
private void findCharactersPerAccount(L2PcInstance activeChar, String characterName) throws IllegalArgumentException
{
	String account = null;
	Map<Integer, String> chars;
	L2PcInstance player = L2World.getInstance().getPlayer(characterName);
	if (player == null)
		throw new IllegalArgumentException("Player doesn't exist");
	chars = player.getAccountChars();
	account = player.getAccountName();
	final StringBuilder replyMSG =
			new StringBuilder(chars.size() * 20);
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	adminReply.setFile("data/html/admin/accountinfo.htm");
	for (String charname : chars.values()) {
		StringUtil.append(replyMSG,
				charname,
				"<br1>");
	}
	
	adminReply.replace("%characters%", replyMSG.toString());
	adminReply.replace("%account%", account);
	adminReply.replace("%player%", characterName);
	activeChar.sendPacket(adminReply);
}
private void findCharactersPerHWIDoff(L2PcInstance activeChar, String characterName) throws IllegalArgumentException
{
	String account = null;
	Map<String, String> chars;
	L2PcInstance player = L2World.getInstance().getPlayer(characterName);
	if (player == null)
		throw new IllegalArgumentException("Player doesn't exist");
	chars = player.getHwidChars();
	account = player.getAccountName();
	final StringBuilder replyMSG =
			new StringBuilder(chars.size() * 20);
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	adminReply.setFile("data/html/admin/accountinfo.htm");
	for (String charname : chars.values()) {
		StringUtil.append(replyMSG,
				charname,
				"<br1>");
	}
	
	adminReply.replace("%characters%", replyMSG.toString());
	adminReply.replace("%account%", account);
	adminReply.replace("%player%", characterName);
	activeChar.sendPacket(adminReply);
}
/**
 * @param activeChar
 * @throws IllegalArgumentException
 */
private void findDualbox(L2PcInstance activeChar, int multibox) throws IllegalArgumentException
{
	Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
	L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
	
	Map<String, List<L2PcInstance>> ipMap = new HashMap<String, List<L2PcInstance>>();
	
	String ip = "0.0.0.0";
	L2GameClient client;
	
	final Map<String, Integer> dualboxIPs = new HashMap<String, Integer>();
	
	for (L2PcInstance player : players)
	{
		client = player.getClient();
		if (client.isDetached())
			continue;
		else
		{
			ip = client.getConnection().getInetAddress().getHostAddress();
			
			/*if (client.getAccountName().equalsIgnoreCase("olympian789"))
			{
				if (activeChar.getAccessLevel().getLevel() < 6)
					continue;
			}
			else if (client.getAccountName().equalsIgnoreCase("ayxhitos200"))
			{
				if (activeChar.getAccessLevel().getLevel() < 6)
					continue;
			}
			else*/ if (activeChar.getAccessLevel().getLevel() <= player.getAccessLevel().getLevel())
			{
				continue;
			}
			
			if (ipMap.get(ip) == null)
				ipMap.put(ip, new ArrayList<L2PcInstance>());
			ipMap.get(ip).add(player);
			
			if (ipMap.get(ip).size() >= multibox)
			{
				Integer count = dualboxIPs.get(ip);
				if (count == null)
					dualboxIPs.put(ip, multibox);
				else
					dualboxIPs.put(ip, count + 1);
			}
		}
	}
	
	List<String> keys = new ArrayList<String>(dualboxIPs.keySet());
	Collections.sort(keys, new Comparator<String>() {
		public int compare(String left, String right)
		{
			return dualboxIPs.get(left).compareTo(dualboxIPs.get(right));
		}
	});
	Collections.reverse(keys);
	
	final StringBuilder results = new StringBuilder();
	for (String dualboxIP : keys)
	{
		StringUtil.append(results, "<a action=\"bypass -h admin_find_ip " + dualboxIP + "\">" + dualboxIP + " (" + dualboxIPs.get(dualboxIP) + ")</a><br1>");
	}
	
	NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
	adminReply.setFile("data/html/admin/dualbox.htm");
	adminReply.replace("%multibox%", String.valueOf(multibox));
	adminReply.replace("%results%", results.toString());
	activeChar.sendPacket(adminReply);
}
private void showMarriageInfo(L2PcInstance activeChar, L2PcInstance player) //ZGirl
{
	//Check if player is married
	if (!player.isThisCharacterMarried())
	{
		activeChar.sendMessage("Player is not married.");
	}
	else
	{
		//Get the Couple ID
		int _Id = player.getCoupleId();
		
		//Get the couple information from database
		Connection con = null;
		try{
			PreparedStatement statement;
			ResultSet rs;
			int _player1Id = 0;
			int _player2Id = 0;
			String player1Name="";
			String player2Name="";
			Calendar _weddingDate = Calendar.getInstance();
			Calendar currentDate = Calendar.getInstance();
			Long diffDays;
			SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm Z");
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("Select * from mods_wedding where id = ?");
			statement.setInt(1, _Id);
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				_player1Id = rs.getInt("player1Id");
				_player2Id = rs.getInt("player2Id");
				_weddingDate.setTimeInMillis(rs.getLong("weddingDate"));
			}
			statement.close();
			
			//Get the couple names from database
			statement = con.prepareStatement("Select char_name from characters where charId = ?");
			statement.setInt(1, _player1Id);
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				player1Name = rs.getString("char_name");
			}
			statement.close();
			
			statement = con.prepareStatement("Select char_name from characters where charId = ?");
			statement.setInt(1, _player2Id);
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				player2Name = rs.getString("char_name");
			}
			statement.close();
			
			//calculate number of days
			diffDays = (currentDate.getTimeInMillis() - _weddingDate.getTimeInMillis()) / (24*60*60*1000);
			
			//Print marriage information
			activeChar.sendMessage(player1Name + " - " + player2Name);
			activeChar.sendMessage("Married on: "+ sdf.format(_weddingDate.getTime()));
			activeChar.sendMessage("Married for "+ diffDays + " days");
		}
		catch (Exception e)
		{
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
}
private void showWhoIs(L2PcInstance activeChar, String charName) //ZGirl
{
	//Connect to database and query for the account name
	Connection con = null;
	try{
		PreparedStatement statement;
		ResultSet rs;
		String _accountname = "";
		int _accessLevel = 0;
		
		con = L2DatabaseFactory.getInstance().getConnection();
		
		statement = con.prepareStatement("Select account_name, accesslevel from characters where char_name = ?");
		statement.setString(1, charName);
		rs = statement.executeQuery();
		
		while (rs.next())
		{
			_accountname = rs.getString("account_name");
			_accessLevel = rs.getInt("accesslevel");
		}
		statement.close();
		
		//Print information
		if (_accountname==""){
			activeChar.sendMessage("Character: " + charName + " doesn't exist.");
		}
		else{
			if (_accessLevel>0) { //doesn't work for GM characters
				activeChar.sendMessage("Can't show information.");
			}
			else{
				activeChar.sendMessage("Character: " + charName + " is in account: " + _accountname);
			}
		}
	}
	catch (Exception e)
	{
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
}
private void showAccInfo(L2PcInstance activeChar, String accName) //ZGirl
{
	//Connect to database and query for the account details
	Connection con = null;
	try{
		PreparedStatement statement;
		ResultSet rs;
		String _lastIP = "";
		int _accessLevel = -321;
		Calendar _lastLoginDate = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm Z");
		
		
		con = L2DatabaseFactory.getInstance().getConnection();
		
		statement = con.prepareStatement("Select lastactive, accessLevel, lastIP from accounts where login = ?");
		statement.setString(1, accName);
		rs = statement.executeQuery();
		
		while (rs.next())
		{
			_lastIP = rs.getString("lastIP");
			_accessLevel = rs.getInt("accessLevel");
			_lastLoginDate.setTimeInMillis(rs.getLong("lastactive"));
		}
		statement.close();
		
		//Print information
		if (_accessLevel==-321){
			activeChar.sendMessage("Account: " + accName + " doesn't exist.");
		}
		else{
			if(_accessLevel>0){ //doesn't work for GM accounts
				activeChar.sendMessage("Can't show information.");
			}
			else{
				activeChar.sendMessage("*********************");
				activeChar.sendMessage("Information for account: " + accName);
				activeChar.sendMessage("Last login date: " + sdf.format(_lastLoginDate.getTime()));
				activeChar.sendMessage("Last IP: " + _lastIP);
				activeChar.sendMessage("Access Level: " + _accessLevel);
			}
		}
	}
	catch (Exception e)
	{
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
}
private Boolean fixCertif(int characterID)
{
	//Connect to database and run update query for the character
	Boolean result = true;
	Connection con = null;
	try{
		PreparedStatement statement;
		con = L2DatabaseFactory.getInstance().getConnection();
		
		statement = con.prepareStatement("UPDATE character_quest_global_data SET value=? WHERE charId=? and (var like ? or var like ? or var like ? or var like ?)");
		statement.setString(1,"0");
		statement.setInt(2,characterID);
		statement.setString(3,"EmergentAbility90-%");
		statement.setString(4,"EmergentAbility91-%");
		statement.setString(5,"ClassAbility92-%");
		statement.setString(6,"ClassAbility93-%");
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		result = false;
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
			result = false;
		}
	}
	return result;
}

private void removeBooks(L2PcInstance activeChar, L2PcInstance player)
{
	long numBooks = 0;
	int[] bookIDs = {10280,10281,10282,10283,10284,10285,10286,10287,10288,10289,10290,10291,10292,10293,10294,10612};
	//int[] certSkills = {631,632,633,634,637,638,639,640,799,800,801,650,651,804,641,652,644,645,653,802,646,654,803,648,1490,643,1489,1491,642,647,655,656,657,658,659,661,660,662};
	//L2Skill skill;
	L2ItemInstance[] ownedBooks;
	
	//search inventory for certification books items
	for (int itm : bookIDs){
		ownedBooks = player.getInventory().getAllItemsByItemId(itm);
		if ( ownedBooks != null && ownedBooks.length>0){
			//get ammount
			if (ownedBooks[0].isStackable()){
				numBooks = ownedBooks[0].getCount();
			}
			else{
				numBooks = ownedBooks.length;
			}
			//destroy books from inventory
			player.destroyItemByItemId("fixcertif", itm, numBooks, activeChar, true);
		}
		else{
			//check in warehouse
			numBooks = player.getWarehouse().getInventoryItemCount(itm,-1);
			//destroy books from warehouse
			if (numBooks > 0){
				player.getWarehouse().destroyItemByItemId("fixcertif", itm, numBooks, player, activeChar);
			}
		}
		
	}
	
	// Search and remove all certification skills on player
	/*              // This only worked for active class skills. Replaced with direct database update.
        for (int idSkill : certSkills){
                skill = player.getKnownSkill(idSkill);
                if ( skill!= null){
                        player.removeSkill(skill);
                }
        }
	 */
	
	Connection con = null;
	
	try
	{
		// Remove or update a L2PcInstance skill from the character_skills table of the database
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("DELETE FROM character_skills WHERE charId=? AND skill_id IN (631,632,633,634,637,638,639,640,799,800,801,650,651,804,641,652,644,645,653,802,646,654,803,648,1490,643,1489,1491,642,647,655,656,657,658,659,661,660,662)");
		
		statement.setInt(1, player.getObjectId());
		statement.execute();
		statement.close();
	}
	catch (Exception e)
	{
		activeChar.sendMessage("Error could not delete skills: " + e);
	}
	finally
	{
		try { con.close(); } catch (Exception e) {}
	}
	
}
}