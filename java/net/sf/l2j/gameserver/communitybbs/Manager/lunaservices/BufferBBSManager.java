//package net.sf.l2j.gameserver.communitybbs.Manager.lunaservices;
//
//import java.util.NoSuchElementException;
//import java.util.StringTokenizer;
//
//import javolution.text.TextBuilder;
//import javolution.util.FastList;
//import javolution.util.FastMap;
//import net.sf.l2j.gameserver.cache.HtmCache;
//import net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager;
//import net.sf.l2j.gameserver.datatables.BufferSkillsTable;
//import net.sf.l2j.gameserver.datatables.CharSchemesTable;
//import net.sf.l2j.gameserver.datatables.SkillTable;
//import net.sf.l2j.gameserver.model.L2Effect;
//import net.sf.l2j.gameserver.model.L2Skill;
//import net.sf.l2j.gameserver.model.actor.L2Character;
//import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
//import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
//import net.sf.l2j.gameserver.model.olympiad.Olympiad;
//import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
//import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
//
///**
// * <b><font size=3>NPC Buffer instance handler</font></b><br>
// * <br>
// * This class contains some methods that can be sorted by different types and functions:<br>
// * <br>
// * - Methods that overrides to superclass' (L2FolkInstance):
// * <li>onAction
// * <li>
// * onBypassFeedback
// * <li>onActionShift <br>
// * <br>
// * - Methods to show html windows:
// * <li>showGiveBuffsWindow
// * <li>
// * showManageSchemeWindow
// * <li>showEditSchemeWindow <br>
// * </br>
// * - Methods to get and build info (Strings, future html content) from character schemes, state,
// * etc.
// * <li>getPlayerSchemeListFrame: Returns a table with player's schemes names
// * <li>
// * getGroupSkillListFrame: Returns a table with skills available in the skill_group
// * <li>
// * getPlayerSkillListFrame: Returns a table with skills already in player's scheme (scheme_key)
// * <br>
// * <br>
// * 
// * @author House
// */
//public class BufferBBSManager extends BaseBBSManager
//{
//	public static final String PATH = "data/html/CommunityBoard/Buffer/";
//	private static final String	PARENT_DIR				= "data/html/CommunityBoard/Buffer/";
//	private final static int[]	FIGHTER_SHIELD_ZERK		=
//	{
//		1035, 1268, 1068, 1040, 1204, 1077, 1086, 1242, 1240, 1243, 1304, 1045, 1036, 1388, 1062, 1363, 4700, 271, 272, 274, 275, 264, 267, 268, 269, 304, 349
//	};
//	private final static int[]	FIGHTER_EVASION_ZERK	=
//	{
//		1035, 1268, 1068, 1040, 1204, 1077, 1086, 1242, 1240, 1087, 1045, 1036, 1389, 1062, 1357, 4699, 310, 271, 272, 274, 275, 266, 264, 267, 268, 269, 304, 364
//	};
//	private final static int[]	FIGHTER_NONE_ZERK		=
//	{
//		1035, 1268, 1068, 1040, 1204, 1077, 1086, 1242, 1240, 1045, 1036, 1388, 1062, 1363, 4700, 271, 272, 274, 275, 310, 264, 267, 268, 269, 304, 364, 1362, 364
//	};
//	private final static int[]	FIGHTER_SHIELD_NOZERK	=
//	{
//		264, 267, 268, 270, 304, 306, 308, 529, 349, 530, 275, 1362, 1035, 1036, 1040, 1045, 1044, 1086, 1204, 1191, 1356, 1243, 4703, 1352, 1389, 1078, 1304, 1259
//	};
//	private final static int[]	FIGHTER_EVASION_NOZERK	=
//	{
//		1035, 1268, 1068, 1040, 1204, 1077, 1086, 1242, 1240, 1087, 1045, 1036, 1389, 1357, 4699, 310, 271, 272, 274, 275, 266, 264, 267, 268, 269, 304, 364
//	};
//	private final static int[]	FIGHTER_NONE_NOZERK		=
//	{
//		1035, 1078, 1268, 1068, 1040, 1204, 1077, 1086, 1242, 1240, 1045, 1036, 1388, 1363, 4700, 271, 272, 274, 275, 310, 264, 267, 268, 269, 304, 364, 1362, 349
//	};
//	private final static int[]	MAGE_ZERK				=
//	{
//		1078, 1389, 1352, 1040, 1045, 1085, 1059, 1303, 1204, 1355, 1062, 1036, 1035, 1259, 4703, 529, 273, 276, 365, 530, 264, 267, 268, 304, 349, 1362
//	};
//	private final static int[]	MAGE_NOZERK				=
//	{
//		1078, 1389, 1352, 1040, 1045, 1085, 1059, 1303, 1204, 1355, 1036, 1035, 1259, 4703, 273, 529, 276, 365, 530, 264, 267, 268, 304, 349, 1362
//	};
//	
//	public BufferBBSManager()
//	{
//		super();
//	}
//	
//	public void onBypassFeedback(L2PcInstance player, String command)
//	{
//		if(player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player) || Olympiad.getInstance().isRegisteredInComp(player))
//		{
//			player.sendMessage("Can't use while in oly mode");
//			return;
//		}
//		StringTokenizer st = new StringTokenizer(command, " ");
//		String currentCommand = st.nextToken();
//		if (player.getClassId().level() != 3)
//		{
//			player.sendMessage("You must be on your third class before you can use the buffer.");
//			player.sendPacket(ActionFailed.STATIC_PACKET);
//			return;
//		}
//		// initial menu
//		if (currentCommand.equalsIgnoreCase("_bbsbuffer_menu"))
//		{
//			NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
//			html.setFile(PARENT_DIR + "menu.htm");
//			final int curBuffs = player.getBuffCount();
//			html.replace("%lol%", String.valueOf(curBuffs));
//			final int maxBuffs = player.getMaxBuffCount();
//			html.replace("%nig%", String.valueOf(maxBuffs));
//			sendHtmlMessage(player, html);
//		}
////		else if (currentCommand.startsWith("_bbsbuffer_heal"))
////		{
////			if (player.isInCombat())
////			{
////				player.sendMessage("You cannot be healed while in combat");
////				return;
////			}
////			if (player.getPvpFlagLasts() > (System.currentTimeMillis() + 20000))
////			{
////				player.sendMessage("You cannot be healed while in PVP");
////				return;
////			}
////			if (player.getKarma() > 0)
////			{
////				player.sendMessage("You cannot be healed while having karma");
////				return;
////			}
////			if (!player.canBeBufferHealed())
////			{
////				player.sendMessage("You must wait at least 3 minutes since your last heal to be healed");
////				return;
////			}
////			player.setLastHealedTime();
////			player.getStatus().setCurrentCp(player.getMaxCp());
////			player.getStatus().setCurrentHp(player.getMaxHp());
////			/* player.getStatus().setCurrentHpMp(player.getMaxHp(), player.getMaxMp()); */
////			if (player.getPet() != null)
////			{
////				final L2Summon pet = player.getPet();
////				pet.getStatus().setCurrentHpMp(pet.getMaxHp(), pet.getMaxMp());
////			}
////			NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
////			html.setFile(PARENT_DIR + "menu.htm");
////			final int curBuffs = player.getBuffCount();
////			html.replace("%lol%", String.valueOf(curBuffs));
////			final int maxBuffs = player.getMaxBuffCount();
////			html.replace("%nig%", String.valueOf(maxBuffs));
////			sendHtmlMessage(player, html);
////		}
//		else if (currentCommand.startsWith("_bbsbuffer_cancel"))
//		{
//			for (L2Effect effect : player.getAllEffects())
//			{
//				if (effect != null && effect.getSkill() != null && effect.getSkill().isPositive() && effect.getSkill().getId() != 12005 && effect.getSkill().getId() != 2672 && effect.getSkill().getId() != 2672 && !effect.isCustomEffectToNotBeRemoved())
//					effect.exit();
//			}
//			NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
//			html.setFile(PARENT_DIR + "menu.htm");
//			final int curBuffs = player.getBuffCount();
//			html.replace("%lol%", String.valueOf(curBuffs));
//			final int maxBuffs = player.getMaxBuffCount();
//			html.replace("%nig%", String.valueOf(maxBuffs));
//			sendHtmlMessage(player, html);
//		}
//		else if (currentCommand.startsWith("_bbsbuffer_goto"))
//		{
//			String token = st.nextToken();
//			String content = HtmCache.getInstance().getHtmForce(PARENT_DIR + token + ".htm");
//			if (content == null)
//			{
//				NpcHtmlMessage html = new NpcHtmlMessage(1);
//				html.setHtml("<html><body>My Text is missing</body></html>");
//				player.sendPacket(html);
//			}
//			else
//			{
//				NpcHtmlMessage tele = new NpcHtmlMessage(0, 1);
//				tele.setHtml(content);
//				final int curBuffs = player.getBuffCount();
//				tele.replace("%lol%", String.valueOf(curBuffs));
//				final int maxBuffs = player.getMaxBuffCount();
//				tele.replace("%nig%", String.valueOf(maxBuffs));
//				sendHtmlMessage(player, tele);
//				player.setBufferPage(token);
//			}
//		}
//		else if (currentCommand.startsWith("_bbsbuffer_gBuff"))
//		{
//			final String token = st.nextToken();
//			if (token != null)
//			{
//				final int buff = Integer.valueOf(token);
//				if (buff >= 0 && buff <= 7)
//				{
//					int[] buffs = null;
//					switch (buff)
//					{
//						case 0:
//							buffs = FIGHTER_EVASION_ZERK;
//							break;
//						case 1:
//							buffs = FIGHTER_SHIELD_ZERK;
//							break;
//						case 2:
//							buffs = FIGHTER_NONE_ZERK;
//							break;
//						case 3:
//							buffs = FIGHTER_EVASION_NOZERK;
//							break;
//						case 4:
//							buffs = FIGHTER_SHIELD_NOZERK;
//							break;
//						case 5:
//							buffs = FIGHTER_NONE_NOZERK;
//							break;
//						case 6:
//							buffs = MAGE_ZERK;
//							break;
//						case 7:
//							buffs = MAGE_NOZERK;
//							break;
//						default:
//							return;
//					}
//					if (buffs == null)
//						return;
//					if (player.canBeBufferBuffed())
//					{
//						player.setLastBuffedTime();
//						for (Integer skillId : buffs)
//						{
//							final L2Skill skill = SkillTable.getInstance().getInfo(skillId, SkillTable.getInstance().getMaxLevel(skillId));
//							if (skill != null)
//								skill.getEffects(player, player);
//						}
//					}
//					else
//					{
//						player.sendMessage("You must wait 3 seconds between multi buffs");
//					}
//					NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
//					html.setFile(PARENT_DIR + "menu.htm");
//					final int curBuffs = player.getBuffCount();
//					html.replace("%lol%", String.valueOf(curBuffs));
//					final int maxBuffs = player.getMaxBuffCount();
//					html.replace("%nig%", String.valueOf(maxBuffs));
//					sendHtmlMessage(player, html);
//				}
//			}
//		}
//		else if (currentCommand.startsWith("_bbsbuffer_cast"))
//		{
//			final String buff = st.nextToken();
//			if (buff != null)
//			{
//				// Chant of Battle
//				if (buff.equals("1"))
//				{
//					SkillTable.getInstance().getInfo(1007, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Shielding
//				else if (buff.equals("2"))
//				{
//					SkillTable.getInstance().getInfo(1009, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Fire
//				else if (buff.equals("3"))
//				{
//					SkillTable.getInstance().getInfo(1006, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Flame
//				else if (buff.equals("4"))
//				{
//					SkillTable.getInstance().getInfo(1002, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Life
//				else if (buff.equals("5"))
//				{
//					SkillTable.getInstance().getInfo(1229, 18).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Fury
//				else if (buff.equals("6"))
//				{
//					SkillTable.getInstance().getInfo(1251, 2).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Evasion
//				else if (buff.equals("7"))
//				{
//					SkillTable.getInstance().getInfo(1252, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Rage
//				else if (buff.equals("8"))
//				{
//					SkillTable.getInstance().getInfo(1253, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Revenge
//				else if (buff.equals("9"))
//				{
//					SkillTable.getInstance().getInfo(1284, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Vampire
//				else if (buff.equals("10"))
//				{
//					SkillTable.getInstance().getInfo(1310, 4).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Eagle
//				else if (buff.equals("11"))
//				{
//					SkillTable.getInstance().getInfo(1309, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Predator
//				else if (buff.equals("12"))
//				{
//					SkillTable.getInstance().getInfo(1308, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Spirit
//				else if (buff.equals("13"))
//				{
//					SkillTable.getInstance().getInfo(1362, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Chant of Victory
//				else if (buff.equals("14"))
//				{
//					SkillTable.getInstance().getInfo(1363, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Earth
//				else if (buff.equals("15"))
//				{
//					SkillTable.getInstance().getInfo(264, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Life
//				else if (buff.equals("16"))
//				{
//					SkillTable.getInstance().getInfo(265, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Water
//				else if (buff.equals("17"))
//				{
//					SkillTable.getInstance().getInfo(266, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Warding
//				else if (buff.equals("18"))
//				{
//					SkillTable.getInstance().getInfo(267, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Wind
//				else if (buff.equals("19"))
//				{
//					SkillTable.getInstance().getInfo(268, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Hunter
//				else if (buff.equals("20"))
//				{
//					SkillTable.getInstance().getInfo(269, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Invocation
//				else if (buff.equals("21"))
//				{
//					SkillTable.getInstance().getInfo(270, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Meditation
//				else if (buff.equals("22"))
//				{
//					SkillTable.getInstance().getInfo(363, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Renewal
//				else if (buff.equals("23"))
//				{
//					SkillTable.getInstance().getInfo(349, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Champion
//				else if (buff.equals("24"))
//				{
//					SkillTable.getInstance().getInfo(364, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Vitality
//				else if (buff.equals("25"))
//				{
//					SkillTable.getInstance().getInfo(304, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Vengeance
//				else if (buff.equals("26"))
//				{
//					SkillTable.getInstance().getInfo(305, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Flame Guard
//				else if (buff.equals("27"))
//				{
//					SkillTable.getInstance().getInfo(306, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Song of Storm Guard
//				else if (buff.equals("28"))
//				{
//					SkillTable.getInstance().getInfo(308, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Shield
//				else if (buff.equals("29"))
//				{
//					SkillTable.getInstance().getInfo(1040, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Might
//				else if (buff.equals("30"))
//				{
//					SkillTable.getInstance().getInfo(1068, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Bless Shield
//				else if (buff.equals("31"))
//				{
//					SkillTable.getInstance().getInfo(1243, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Guidance
//				else if (buff.equals("32"))
//				{
//					SkillTable.getInstance().getInfo(1240, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Bless the Body
//				else if (buff.equals("33"))
//				{
//					SkillTable.getInstance().getInfo(1045, 6).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Bless the Soul
//				else if (buff.equals("34"))
//				{
//					SkillTable.getInstance().getInfo(1048, 6).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Focus
//				else if (buff.equals("35"))
//				{
//					SkillTable.getInstance().getInfo(1077, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Magic Barrier
//				else if (buff.equals("36"))
//				{
//					SkillTable.getInstance().getInfo(1036, 2).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Berserker Spirit
//				else if (buff.equals("37"))
//				{
//					SkillTable.getInstance().getInfo(1062, 2).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Regeneration
//				else if (buff.equals("38"))
//				{
//					SkillTable.getInstance().getInfo(1044, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Mental Shield
//				else if (buff.equals("39"))
//				{
//					SkillTable.getInstance().getInfo(1035, 4).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Greater Empower
//				else if (buff.equals("40"))
//				{
//					SkillTable.getInstance().getInfo(1059, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Death Whisper
//				else if (buff.equals("41"))
//				{
//					SkillTable.getInstance().getInfo(1242, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Greater Concentration
//				else if (buff.equals("42"))
//				{
//					SkillTable.getInstance().getInfo(1078, 6).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Haste
//				else if (buff.equals("43"))
//				{
//					SkillTable.getInstance().getInfo(1086, 2).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Greater Acumen
//				else if (buff.equals("44"))
//				{
//					SkillTable.getInstance().getInfo(1085, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Wind Walk
//				else if (buff.equals("45"))
//				{
//					SkillTable.getInstance().getInfo(1204, 2).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Wild Magic
//				else if (buff.equals("46"))
//				{
//					SkillTable.getInstance().getInfo(1303, 2).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Arcane Protection
//				else if (buff.equals("47"))
//				{
//					SkillTable.getInstance().getInfo(1354, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Body of Avatar
//				else if (buff.equals("48"))
//				{
//					/* SkillTable.getInstance().getInfo(1311, 6).getEffects(player, player); */
//					showReturnPage(player);
//				}
//				// Prophecy of Fire
//				else if (buff.equals("49"))
//				{
//					SkillTable.getInstance().getInfo(1356, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Prophecy of Water
//				else if (buff.equals("50"))
//				{
//					SkillTable.getInstance().getInfo(1355, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Prophecy of Wind
//				else if (buff.equals("51"))
//				{
//					SkillTable.getInstance().getInfo(1357, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Warrior
//				else if (buff.equals("52"))
//				{
//					SkillTable.getInstance().getInfo(271, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Inspiration
//				else if (buff.equals("53"))
//				{
//					SkillTable.getInstance().getInfo(272, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Mystic
//				else if (buff.equals("54"))
//				{
//					SkillTable.getInstance().getInfo(273, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Fire
//				else if (buff.equals("55"))
//				{
//					SkillTable.getInstance().getInfo(274, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Fury
//				else if (buff.equals("56"))
//				{
//					SkillTable.getInstance().getInfo(275, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Concentration
//				else if (buff.equals("57"))
//				{
//					SkillTable.getInstance().getInfo(276, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Light
//				else if (buff.equals("58"))
//				{
//					SkillTable.getInstance().getInfo(277, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Aqua Guard
//				else if (buff.equals("59"))
//				{
//					SkillTable.getInstance().getInfo(307, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Earth Guard
//				else if (buff.equals("60"))
//				{
//					SkillTable.getInstance().getInfo(309, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Vampire
//				else if (buff.equals("61"))
//				{
//					SkillTable.getInstance().getInfo(310, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Protection
//				else if (buff.equals("62"))
//				{
//					SkillTable.getInstance().getInfo(311, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Holy Weapon
//				else if (buff.equals("63"))
//				{
//					SkillTable.getInstance().getInfo(1043, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Dance of Siren
//				else if (buff.equals("64"))
//				{
//					SkillTable.getInstance().getInfo(365, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Greater Might
//				else if (buff.equals("65"))
//				{
//					SkillTable.getInstance().getInfo(1388, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Greater Shield
//				else if (buff.equals("66"))
//				{
//					SkillTable.getInstance().getInfo(1389, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Advanced Block
//				else if (buff.equals("67"))
//				{
//					SkillTable.getInstance().getInfo(1304, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Elemental Protection
//				else if (buff.equals("68"))
//				{
//					SkillTable.getInstance().getInfo(1352, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Divine Protection
//				else if (buff.equals("69"))
//				{
//					SkillTable.getInstance().getInfo(1353, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Resist Shock
//				else if (buff.equals("70"))
//				{
//					SkillTable.getInstance().getInfo(1259, 4).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// War Chant
//				else if (buff.equals("71"))
//				{
//					SkillTable.getInstance().getInfo(1390, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Earth Chant
//				else if (buff.equals("72"))
//				{
//					SkillTable.getInstance().getInfo(1391, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// ing of Noblesse
//				else if (buff.equals("73"))
//				{
//					/*
//					 * SkillTable.getInstance().getInfo(1323, 1).getEffects(player, player);
//					 * showReturnPage(player);
//					 */
//				}
//				// Seed of Water
//				else if (buff.equals("74"))
//				{
//					SkillTable.getInstance().getInfo(1286, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Seed of Fire
//				else if (buff.equals("75"))
//				{
//					SkillTable.getInstance().getInfo(1285, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Seed of Wind
//				else if (buff.equals("76"))
//				{
//					SkillTable.getInstance().getInfo(1287, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Recover my HP
//				else if (buff.equals("77"))
//				{
//					SkillTable.getInstance().getInfo(10002, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Recover my MP
//				else if (buff.equals("78"))
//				{
//					SkillTable.getInstance().getInfo(10003, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Blessing of Queen
//				else if (buff.equals("79"))
//				{
//					SkillTable.getInstance().getInfo(4699, 13).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Gift of Queen
//				else if (buff.equals("80"))
//				{
//					SkillTable.getInstance().getInfo(4700, 13).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Blessing of Seraphim
//				else if (buff.equals("81"))
//				{
//					SkillTable.getInstance().getInfo(4702, 13).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Gift of Seraphim
//				else if (buff.equals("82"))
//				{
//					SkillTable.getInstance().getInfo(4703, 13).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// Agility
//				else if (buff.equals("83"))
//				{
//					SkillTable.getInstance().getInfo(1087, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// song of elemental
//				else if (buff.equals("84"))
//				{
//					SkillTable.getInstance().getInfo(529, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// dance of alignment
//				else if (buff.equals("85"))
//				{
//					SkillTable.getInstance().getInfo(530, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// invigor
//				else if (buff.equals("86"))
//				{
//					SkillTable.getInstance().getInfo(1032, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// resist fire
//				else if (buff.equals("87"))
//				{
//					SkillTable.getInstance().getInfo(1191, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// resist water
//				else if (buff.equals("88"))
//				{
//					SkillTable.getInstance().getInfo(1182, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// resist wind
//				else if (buff.equals("89"))
//				{
//					SkillTable.getInstance().getInfo(1189, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// holy resistance
//				else if (buff.equals("90"))
//				{
//					SkillTable.getInstance().getInfo(1392, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// unholy resistance
//				else if (buff.equals("91"))
//				{
//					SkillTable.getInstance().getInfo(1393, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// vampiric rage
//				else if (buff.equals("92"))
//				{
//					SkillTable.getInstance().getInfo(1268, 4).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// mana gain
//				else if (buff.equals("93"))
//				{
//					SkillTable.getInstance().getInfo(1460, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// clarity
//				else if (buff.equals("94"))
//				{
//					SkillTable.getInstance().getInfo(1397, 3).getEffects(player, player);
//					showReturnPage(player);
//				}
//				// chant of magnus
//				else if (buff.equals("95"))
//				{
//					SkillTable.getInstance().getInfo(1413, 1).getEffects(player, player);
//					showReturnPage(player);
//				}
//			}
//		} // handles giving effects {support player, support pet, givebuffs}
//		else if (currentCommand.startsWith("_bbsbuffer_support"))
//		{
//			String targettype = st.nextToken();
//			showGiveBuffsWindow(player, targettype);
//		}
//		else if (currentCommand.startsWith("_bbsbuffer_givebuffs"))
//		{
//			String targettype = st.nextToken();
//			String scheme_key = st.nextToken();
//			int cost = Integer.parseInt(st.nextToken());
//			if (player.getLevel() < 85)
//				cost = 0;
//			if (cost == 0 || cost <= player.getInventory().getAdena())
//			{
//				L2Character target = player;
//				if (targettype.equalsIgnoreCase("pet"))
//				{
//					target = player.getPet();
//					if (target != null && target instanceof L2SummonInstance)
//					{
//						player.sendMessage("Summons are not buffed by the buffer, their stats are already tweaked as if buffed.");
//						target = null;
//						// go to main menu
//						NpcHtmlMessage html = new NpcHtmlMessage(1);
//						html.setFile(PARENT_DIR + "menu.htm");
//						final int curBuffs = player.getBuffCount();
//						html.replace("%lol%", String.valueOf(curBuffs));
//						final int maxBuffs = player.getMaxBuffCount();
//						html.replace("%nig%", String.valueOf(maxBuffs));
//						sendHtmlMessage(player, html);
//						return;
//					}
//				}
//				if (target != null)
//				{
//					if (player.canBeBufferBuffed())
//					{
//						player.setLastBuffedTime();
//						final int maxBuffs = target.getMaxBuffCount();
//						int counter = 0;
//						for (int skId : CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key))
//						{
//							final int maxlevel = SkillTable.getInstance().getMaxLevel(skId);
//							final L2Skill skill = SkillTable.getInstance().getInfo(skId, maxlevel);
//							if (skill == null)
//							{
//								System.out.println("WTF? NO skill found in buffer LOL");
//							}
//							else
//							{
//								counter++;
//								skill.getEffects(target, target);
//								if (counter >= maxBuffs)
//									break;
//							}
//						}
//						player.reduceAdena("NPC Buffer", cost, target, true);
//					}
//					else
//					{
//						player.sendMessage("You must wait 3 seconds between multi buffs");
//						showGiveBuffsWindow(player, targettype);
//					}
//				}
//				else
//				{
//					player.sendMessage("Incorrect Target");
//					// go to main menu
//					NpcHtmlMessage html = new NpcHtmlMessage(1);
//					html.setFile(PARENT_DIR + "menu.htm");
//					final int curBuffs = player.getBuffCount();
//					html.replace("%lol%", String.valueOf(curBuffs));
//					final int maxBuffs = player.getMaxBuffCount();
//					html.replace("%nig%", String.valueOf(maxBuffs));
//					sendHtmlMessage(player, html);
//				}
//			}
//			else
//			{
//				player.sendMessage("Not enough adena");
//				showGiveBuffsWindow(player, targettype);
//			}
//		} // handles edit schemes {skillselect, skillunselect}
//		else if (currentCommand.startsWith("editscheme"))
//		{
//			String skill_group = st.nextToken();
//			String scheme_key = null;
//			try
//			{
//				scheme_key = st.nextToken();
//			}
//			catch (Exception e)
//			{
//				// ignored...
//			}
//			showEditSchemeWindow(player, skill_group, scheme_key);
//		}
//		else if (currentCommand.startsWith("skill"))
//		{
//			String skill_group = st.nextToken();
//			String scheme_key = st.nextToken();
//			int skill_id = Integer.parseInt(st.nextToken());
//			if (currentCommand.startsWith("skillselect") && !scheme_key.equalsIgnoreCase("unselected"))
//			{
//				final int maxBuffs = player.getMaxBuffCount();
//				if (CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key).size() < maxBuffs)
//					CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key).add(skill_id);
//				else
//				{
//					player.sendMessage("This scheme has reached the maximum amount of buffs you can have currently (" + maxBuffs + ")");
//				}
//			}
//			else if (currentCommand.startsWith("skillunselect"))
//			{
//				int position = 0;
//				for (int skillID : CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key))
//				{
//					if (skillID == skill_id)
//					{
//						CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key).remove(position);
//						break;
//					}
//					position++;
//				}
//			}
//			showEditSchemeWindow(player, skill_group, scheme_key);
//		}
//		// manage schemes {create, delete, clear}
//		else if (currentCommand.startsWith("manageschemes"))
//		{
//			showManageSchemeWindow(player);
//		}
//		// handles creation
//		else if (currentCommand.startsWith("createscheme"))
//		{
//			if (st != null)
//			{
//				try
//				{
//					String name = st.nextToken();
//					if (name != null)
//					{
//						if (name.length() > 14)
//						{
//							player.sendMessage("Error: Scheme's name must contain up to 14 chars without any spaces");
//							showManageSchemeWindow(player);
//						}
//						else if (CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()) != null && CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()).size() == 6)
//						{
//							player.sendMessage("Error: Maximum scheme amount reached, please delete a scheme before creating a new one");
//							showManageSchemeWindow(player);
//						}
//						else if (CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()) != null && CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()).containsKey(name))
//						{
//							player.sendMessage("Error: duplicate entry. Please use another name");
//							showManageSchemeWindow(player);
//						}
//						else
//						{
//							if (CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()) == null)
//								CharSchemesTable.getInstance().getSchemesTable().put(player.getObjectId(), new FastMap<String, FastList<Integer>>(6 + 1));
//							CharSchemesTable.getInstance().setScheme(player.getObjectId(), name.trim(), new FastList<Integer>(33 + 1));
//							showManageSchemeWindow(player);
//						}
//					}
//				}
//				catch (NoSuchElementException e)
//				{
//					return;
//				}
//			}
//		}
//		//onBypassFeedback(player, command);
//	}
//	
//	public void onAction(L2PcInstance player)
//	{
//				NpcHtmlMessage html = new NpcHtmlMessage(1);
//				html.setFile(PARENT_DIR + "menu.htm");
//				final int curBuffs = player.getBuffCount();
//				html.replace("%lol%", String.valueOf(curBuffs));
//				final int maxBuffs = player.getMaxBuffCount();
//				html.replace("%nig%", String.valueOf(maxBuffs));
//				sendHtmlMessage(player, html);
//		// Send a Server->Client ActionFailed to the L2PcInstance in order to
//		// avoid that the client wait another packet
//	}
//	/*
//	 * @Override public void onActionShift(L2GameClient client) { L2PcInstance player =
//	 * client.getActiveChar(); if (player == null) return;
//	 * if (player.getAccessLevel() >= Config.GM_ACCESSLEVEL) { TextBuilder tb = new TextBuilder();
//	 * tb.append("<html><title>NPC Buffer - Admin</title>");
//	 * tb.append("<body>Changing buffs feature is not implemented yet. :)<br>"); tb.append(
//	 * "<br>Please report any bug/impression/suggestion/etc at http://l2jserver.com/forum. " +
//	 * "<br>Contact <font color=\"00FF00\">House</font></body></html>");
//	 * NpcHtmlMessage html = new NpcHtmlMessage(1); html.setHtml(tb.toString());
//	 * sendHtmlMessage(player, html);
//	 * } player.sendPacket(ActionFailed.STATIC_PACKET); }
//	 */
//	
//	private void sendHtmlMessage(L2PcInstance player, NpcHtmlMessage html)
//	{
//		separateAndSend(html.getText(), player);
//	}
//	
//	/**
//	 * Sends an html packet to player with Give Buffs menu info for player and pet, depending on
//	 * targettype parameter {player, pet}
//	 * 
//	 * @param player
//	 * @param targettype
//	 */
//	private void showGiveBuffsWindow(L2PcInstance player, String targettype)
//	{
//		TextBuilder tb = new TextBuilder();
//		tb.append("<html><title>Buffer - Giving buffs to " + targettype + "</title>");
//		tb.append("<body>Here are your defined schemes, click on a scheme to receive the buffs<br>");
//		FastMap<String, FastList<Integer>> map = CharSchemesTable.getInstance().getAllSchemes(player.getObjectId());
//		if (CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()) == null || CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()).isEmpty())
//		{
//			tb.append("You have not defined any schemes yet, please go back and click Manage Schemes to create one");
//			tb.append("<button action=\"bypass -h npc_%objectId%_menu\" value=\"Back\" width=90 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
//		}
//		else
//		{
//			int cost;
//			tb.append("<table>");
//			for (FastMap.Entry<String, FastList<Integer>> e = map.head(), end = map.tail(); (e = e.getNext()) != end;)
//			{
//				cost = getFee(e.getValue(), player);
//				tb.append("<tr><td width=\"90\"><a action=\"bypass -h npc_%objectId%_givebuffs " + targettype + " " + e.getKey() + " " + String.valueOf(cost) + "\">" + e.getKey() + "</a></td><td>Fee: " + String.valueOf(cost) + "</td></tr>");
//			}
//			tb.append("</table>");
//		}
//		tb.append("</body></html>");
//		NpcHtmlMessage html = new NpcHtmlMessage(1);
//		html.setHtml(tb.toString());
//		sendHtmlMessage(player, html);
//	}
//	
//	/**
//	 * Sends an html packet to player with Manage scheme menu info. This allows player to
//	 * create/delete/clear schemes
//	 * 
//	 * @param player
//	 */
//	private void showManageSchemeWindow(L2PcInstance player)
//	{
//		TextBuilder tb = new TextBuilder();
//		tb.append("<html><title>Buffer - Manage Schemes</title>");
//		tb.append("<body><br>");
//		if (CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()) == null || CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()).isEmpty())
//		{
//			tb.append("<font color=\"LEVEL\">You have not created any schemes yet</font><br>");
//		}
//		else
//		{
//			tb.append("Here is a list of your schemes. To delete a scheme, click on the drop button. To create, enter your scheme name into the name box and press create. " + "Each scheme must have different name. Name must have up to 14 chars. Spaces (\" \") are not allowed. DO NOT click on Create until you have filled in the name<br>");
//			tb.append("<table>");
//			for (FastMap.Entry<String, FastList<Integer>> e = CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()).head(), end = CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()).tail(); (e = e.getNext()) != end;)
//			{
//				tb.append("<tr><td width=\"140\">" + e.getKey() + " (" + String.valueOf(CharSchemesTable.getInstance().getScheme(player.getObjectId(), e.getKey()).size()) + " skill(s))</td>");
//				tb.append("<td width=\"60\"><button value=\"Clear\" action=\"bypass -h npc_%objectId%_clearscheme " + e.getKey() + "\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
//				tb.append("<td width=\"60\"><button value=\"Drop\" action=\"bypass -h npc_%objectId%_deletescheme " + e.getKey() + "\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
//			}
//		}
//		tb.append("<br><table width=240>");
//		tb.append("<tr><td><edit var=\"name\" width=120 height=15></td><td><button value=\"create\" action=\"bypass -h npc_%objectId%_createscheme $name\" width=50 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
//		tb.append("</table>");
//		tb.append("<br><font color=\"LEVEL\">Max schemes per player: " + String.valueOf(6) + "</font>");
//		tb.append("<br><br>");
//		tb.append("<button action=\"bypass -h npc_%objectId%_menu\" value=\"Back\" width=90 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
//		tb.append("</body></html>");
//		NpcHtmlMessage html = new NpcHtmlMessage(1);
//		html.setHtml(tb.toString());
//		sendHtmlMessage(player, html);
//	}
//	
//	/**
//	 * This sends an html packet to player with Edit Scheme Menu info. This allows player to edit
//	 * each created scheme (add/delete skills)
//	 * 
//	 * @param player
//	 * @param skill_group
//	 * @param scheme_key
//	 */
//	private void showEditSchemeWindow(L2PcInstance player, String skill_group, String scheme_key)
//	{
//		NpcHtmlMessage html = new NpcHtmlMessage(1);
//		html.setFile(PARENT_DIR + "schememenu.htm");
//		html.replace("%typesframe%", getTypesFrame(scheme_key));
//		if (skill_group.equalsIgnoreCase("unselected"))
//		{
//			html.replace("%schemelistframe%", getPlayerSchemeListFrame(player, skill_group, scheme_key));
//			html.replace("%skilllistframe%", getGroupSkillListFrame(player, null, null));
//			html.replace("%myschemeframe%", getPlayerSkillListFrame(player, skill_group, scheme_key));
//		}
//		else
//		{
//			html.replace("%schemelistframe%", getPlayerSchemeListFrame(player, skill_group, scheme_key));
//			html.replace("%skilllistframe%", getGroupSkillListFrame(player, skill_group, scheme_key));
//			html.replace("%myschemeframe%", getPlayerSkillListFrame(player, skill_group, scheme_key));
//		}
//		int buffs = player.getMaxBuffCount();
//		int maxBuffs = buffs;
//		if (CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key) != null)
//			buffs -= CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key).size();
//		html.replace("%xa%", String.valueOf(buffs));
//		html.replace("%nig%", String.valueOf(maxBuffs));
//		sendHtmlMessage(player, html);
//	}
//	
//	/**
//	 * Returns a table with info about player's scheme list.<br>
//	 * If player scheme list is null, it returns a warning message
//	 */
//	private String getPlayerSchemeListFrame(L2PcInstance player, String skill_group, String scheme_key)
//	{
//		if (CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()) == null || CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()).isEmpty())
//			return "Please create at least one scheme";
//		else
//		{
//			if (skill_group == null)
//				skill_group = "def";
//			if (scheme_key == null)
//				scheme_key = "def";
//			TextBuilder tb = new TextBuilder();
//			tb.append("<table>");
//			int count = 0;
//			for (FastMap.Entry<String, FastList<Integer>> e = CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()).head(), end = CharSchemesTable.getInstance().getAllSchemes(player.getObjectId()).tail(); (e = e.getNext()) != end;)
//			{
//				if (count == 0)
//					tb.append("<tr>");
//				tb.append("<td width=\"90\"><a action=\"bypass -h npc_%objectId%_editschemes " + skill_group + " " + e.getKey() + "\">" + e.getKey() + "</a></td>");
//				count++;
//				if (count == 3)
//				{
//					tb.append("</tr>");
//					count = 0;
//				}
//			}
//			if (!tb.toString().endsWith("</tr>"))
//				tb.append("</tr>");
//			tb.append("</table>");
//			return tb.toString();
//		}
//	}
//	
//	/**
//	 * @param player
//	 * @param skill_group
//	 * @param scheme_key
//	 * @return a table with info about skills stored in each skill_group
//	 */
//	private String getGroupSkillListFrame(L2PcInstance player, String skill_group, String scheme_key)
//	{
//		if (skill_group == null || skill_group == "unselected")
//			return "<br>Select a skill group first";
//		else if (scheme_key == null || scheme_key == "unselected")
//			return "<br>Select or create a scheme first";
//		TextBuilder tb = new TextBuilder();
//		tb.append("<table>");
//		int count = 0;
//		for (Integer skId : BufferSkillsTable.getInstance().getSkillsByType(skill_group))
//		{
//			if (CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key) != null && !CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key).isEmpty() && CharSchemesTable.getInstance().getSchemeContainsSkill(player.getObjectId(), scheme_key, skId))
//			{
//				continue;
//			}
//			if (count == 0)
//				tb.append("<tr>");
//			int maxlevel = SkillTable.getInstance().getMaxLevel(skId);
//			L2Skill skill = SkillTable.getInstance().getInfo(skId, maxlevel);
//			tb.append("<td width=\"100\"><a action=\"bypass -h npc_%objectId%_skillselect " + skill_group + " " + scheme_key + " " + String.valueOf(skId) + "\">" + skill.getName() + " (" + String.valueOf(maxlevel) + ")</a></td>");
//			if (count == 3)
//			{
//				tb.append("</tr>");
//				count = -1;
//			}
//			count++;
//		}
//		if (!tb.toString().endsWith("</tr>"))
//			tb.append("</tr>");
//		tb.append("</table>");
//		return tb.toString();
//	}
//	
//	/**
//	 * @param player
//	 * @param skill_group
//	 * @param scheme_key
//	 * @return a table with info about selected skills
//	 */
//	private String getPlayerSkillListFrame(L2PcInstance player, String skill_group, String scheme_key)
//	{
//		if (skill_group == null || skill_group == "unselected")
//			return "<br>Select a skill group first";
//		else if (scheme_key == null || scheme_key == "unselected")
//			return "<br>Select or create a scheme first";
//		if (CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key) == null)
//			return "Please choose your Scheme";
//		if (CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key).isEmpty())
//			return "Empty Scheme";
//		TextBuilder tb = new TextBuilder();
//		tb.append("Scheme: " + scheme_key + "<br>");
//		tb.append("<table>");
//		int count = 0;
//		for (int skId : CharSchemesTable.getInstance().getScheme(player.getObjectId(), scheme_key))
//		{
//			if (count == 0)
//				tb.append("<tr>");
//			tb.append("<td><a action=\"bypass -h npc_%objectId%_skillunselect " + skill_group + " " + scheme_key + " " + String.valueOf(skId) + "\">" + SkillTable.getInstance().getInfo(skId, 1).getName() + "</a></td>");
//			count++;
//			if (count == 3)
//			{
//				tb.append("</tr>");
//				count = 0;
//			}
//		}
//		if (!tb.toString().endsWith("<tr>"))
//			tb.append("<tr>");
//		tb.append("</table>");
//		return tb.toString();
//	}
//	
//	/**
//	 * @param scheme_key
//	 * @return an string with skill_groups table.
//	 */
//	private String getTypesFrame(String scheme_key)
//	{
//		TextBuilder tb = new TextBuilder();
//		tb.append("<table>");
//		int count = 0;
//		if (scheme_key == null)
//			scheme_key = "unselected";
//		for (String s : BufferSkillsTable.getInstance().getSkillsTypeList())
//		{
//			if (count == 0)
//				tb.append("<tr>");
//			tb.append("<td width=\"90\"><a action=\"bypass -h npc_%objectId%_editscheme " + s + " " + scheme_key + "\">" + s + "</a></td>");
//			if (count == 2)
//			{
//				tb.append("</tr>");
//				count = -1;
//			}
//			count++;
//		}
//		if (!tb.toString().endsWith("</tr>"))
//			tb.append("</tr>");
//		tb.append("</table>");
//		return tb.toString();
//	}
//	
//	/**
//	 * @param list
//	 * @return fee for all skills contained in list.
//	 */
//	private int getFee(FastList<Integer> list, L2PcInstance player)
//	{
//		if (player.getLevel() < 85)
//			return 0;
//		return (9000 + player.getPvpKills() / 25);
//	}
//	
//	private void showReturnPage(L2PcInstance player)
//	{
//		String content = HtmCache.getInstance().getHtmForce(PARENT_DIR + player.getBufferPage() + ".htm");
//		if (content == null)
//		{
//			NpcHtmlMessage html = new NpcHtmlMessage(1);
//			html.setHtml("<html><body>My Text is missing</body></html>");
//			player.sendPacket(html);
//		}
//		else
//		{
//			NpcHtmlMessage tele = new NpcHtmlMessage(0,1);
//			tele.setHtml(content);
//			sendHtmlMessage(player, tele);
//		}
//	}
//	
//	@Override
//	public void parsecmd(String command, L2PcInstance activeChar)
//	{
//		onBypassFeedback(activeChar, command);
//	}
//	
//	@Override
//	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
//	{
//		// TODO Auto-generated method stub
//	}
//
//	private static final class SingletonHolder
//	{
//		protected static final BufferBBSManager _instance = new BufferBBSManager();
//	}
//	
//	public static BufferBBSManager getInstance()
//	{
//		return SingletonHolder._instance;
//	}
//}
