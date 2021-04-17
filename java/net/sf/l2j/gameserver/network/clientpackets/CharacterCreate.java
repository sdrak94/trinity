package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.stat.PcStat;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.CharCreateFail;
import net.sf.l2j.gameserver.network.serverpackets.CharCreateOk;
import net.sf.l2j.gameserver.templates.chars.L2PcTemplate;
import net.sf.l2j.gameserver.templates.chars.L2PcTemplate.PcTemplateItem;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

/**
 * This class ...
 *
 * @version $Revision: 1.9.2.3.2.8 $ $Date: 2005/03/27 15:29:30 $
 */
@SuppressWarnings("unused")
public final class CharacterCreate extends L2GameClientPacket
{
	private static final String	_C__0B_CHARACTERCREATE	= "[C] 0B CharacterCreate";
	private static Logger		_log					= Logger.getLogger(CharacterCreate.class.getName());
	// cSdddddddddddd
	private String				_name;
	private int					_race;
	private byte				_sex;
	private int					_classId;
	private int					_int;
	private int					_str;
	private int					_con;
	private int					_men;
	private int					_dex;
	private int					_wit;
	private byte				_hairStyle;
	private byte				_hairColor;
	private byte				_face;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
		_race = readD();
		_sex = (byte) readD();
		_classId = readD();
		_int = readD();
		_str = readD();
		_con = readD();
		_men = readD();
		_dex = readD();
		_wit = readD();
		_hairStyle = (byte) readD();
		_hairColor = (byte) readD();
		_face = (byte) readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (_name.length() < 2 || _name.length() > 16)
		{
			if (Config.DEBUG)
			{
				_log.fine("charname: " + _name + " is invalid. creation failed.");
			}
			sendPacket(new CharCreateFail(CharCreateFail.REASON_16_ENG_CHARS));
			return;
		}
		if (!Util.isAlphaNumeric(_name) || !isValidName(_name))
		{
			if (Config.DEBUG)
			{
				_log.fine("Character Creation Failure: Character name " + _name + " is invalid. Message generated: Incorrect name. Please try again.");
			}
			sendPacket(new CharCreateFail(CharCreateFail.REASON_INCORRECT_NAME));
			return;
		}
		L2PcInstance newChar = null;
		L2PcTemplate template = null;
		/*
		 * DrHouse: Since checks for duplicate names are done using SQL,
		 * lock must be held until data is written to DB as well.
		 */
		synchronized (CharNameTable.getInstance())
		{
			if (CharNameTable.getInstance().accountCharNumber(getClient().getAccountName()) >= Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT && Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT != 0)
			{
				if (Config.DEBUG)
				{
					_log.fine("Max number of characters reached. Creation failed.");
				}
				sendPacket(new CharCreateFail(CharCreateFail.REASON_TOO_MANY_CHARACTERS));
				return;
			}
			else if (CharNameTable.getInstance().doesCharNameExist(_name))
			{
				if (Config.DEBUG)
				{
					_log.fine("charname: " + _name + " already exists. creation failed.");
				}
				sendPacket(new CharCreateFail(CharCreateFail.REASON_NAME_ALREADY_EXISTS));
				return;
			}
			template = CharTemplateTable.getInstance().getTemplate(_classId);
			if (Config.DEBUG)
			{
				_log.fine("charname: " + _name + " classId: " + _classId + " template: " + template);
			}
			if (template == null || template.classBaseLevel > 1)
			{
				sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
				return;
			}
			int objectId = IdFactory.getInstance().getNextId();
			newChar = L2PcInstance.create(objectId, template, getClient().getAccountName(), _name, _hairStyle, _hairColor, _face, _sex != 0);
		}
		newChar.setCurrentHp(template.baseHpMax + 400);
		newChar.setCurrentCp(template.baseCpMax + 400);
		newChar.setCurrentMp(template.baseMpMax + 400);
		// send acknowledgement
		CharCreateOk cco = new CharCreateOk();
		sendPacket(cco);
		initNewChar(getClient(), newChar);
	}
	
	public static boolean isValidName(String text)
	{
		return isValidName(text, false);
	}
	
	public static boolean isValidName(String text, boolean fromDonate)
	{
		boolean result = true;
		String test = text;
		Pattern pattern;
		try
		{
			if (fromDonate)
			{
				pattern = Pattern.compile("[A-Za-z0-9]{2,23}");
			}
			else
			{
				pattern = Pattern.compile(Config.CNAME_TEMPLATE);
			}
		}
		catch (PatternSyntaxException e) // case of illegal pattern
		{
			_log.warning("ERROR : Character name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		Matcher regexp = pattern.matcher(test);
		if (!regexp.matches())
		{
			result = false;
		}
		return result;
	}
	
	private void initNewChar(L2GameClient client, L2PcInstance newChar)
	{
		if (Config.DEBUG)
		{
			_log.fine("Character init start");
		}
		L2World.getInstance().storeObject(newChar);
		L2PcTemplate template = newChar.getTemplate();
		newChar.addAdena("Init", Config.STARTING_ADENA, null, false);
		int chance = Rnd.get(5);
		switch (chance)
		{
			case 0:
				newChar.setXYZInvisible(83344, 148140, -3404);
			case 1:
				newChar.setXYZInvisible(83465, 148654, -3404);
			case 2:
				newChar.setXYZInvisible(83344, 148140, -3404);
			case 3:
				newChar.setXYZInvisible(82844, 148632, -3471);
			case 4:
				newChar.setXYZInvisible(82835, 148143, -3468);
			case 5:
				newChar.setXYZInvisible(83344, 148140, -3404);
			default:
				newChar.setXYZInvisible(83344, 148140, -3404);
		}
		newChar.setTitle("");
		newChar.setVitalityPoints(PcStat.MAX_VITALITY_POINTS, true);
		if (Config.STARTING_LEVEL > 1)
		{
			newChar.getStat().addLevel((byte) (Config.STARTING_LEVEL - 1));
		}
		if (Config.STARTING_SP > 0)
		{
			newChar.getStat().addSp(Config.STARTING_SP);
		}
		L2ShortCut shortcut;
		// add attack shortcut
		shortcut = new L2ShortCut(0, 0, 3, 2, 0, 1);
		newChar.registerShortCut(shortcut);
		// add take shortcut
		shortcut = new L2ShortCut(3, 0, 3, 5, 0, 1);
		newChar.registerShortCut(shortcut);
		// add sit shortcut
		shortcut = new L2ShortCut(10, 0, 3, 0, 0, 1);
		newChar.registerShortCut(shortcut);
		for (PcTemplateItem ia : template.getItems())
		{
			L2ItemInstance item = newChar.getInventory().addItem("Init", ia.getItemId(), ia.getAmount(), newChar, null);
			// add wondrous cubic shortcut
			if (item.getItemId() == 60000)
			{
				shortcut = new L2ShortCut(11, 0, 1, item.getObjectId(), 0, 1);
				newChar.registerShortCut(shortcut);
			}
			if (item.isWeapon() || item.isJewelry())
			{
				item.setEnchantLevel(12);
				item.setAugmentation(AugmentationData.getInstance().generateRandomAugmentation(84, 3, item.getItem().getBodyPart(), false));
			}
			else if (item.isArmor())
			{
				item.setEnchantLevel(6);
			}
			if (item.isTradeable())
			{
				item.setUntradeableTimer(9999999900003L);
			}
			if (item.isEquipable() && ia.isEquipped())
			{
				if (newChar.isMageClass())
				{
					if (item.getItemId() >= 6379 && item.getItemId() <= 6382 || item.getItemId() == 6583 || item.getItemId() == 6594)
					{
						continue;
					}
				}
				else
				{
					if (item.getItemId() >= 6383 && item.getItemId() <= 6386 || item.getItemId() == 6608 || item.getItemId() == 6594)
					{
						continue;
					}
				}
				newChar.getInventory().equipItemAndRecord(item);
			}
		}
		for (L2SkillLearn skill : SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId()))
		{
			newChar.addSkill(SkillTable.getInstance().getInfo(skill.getId(), skill.getLevel()), true);
			if (skill.getId() == 1001 || skill.getId() == 1177)
			{
				shortcut = new L2ShortCut(1, 0, 2, skill.getId(), skill.getLevel(), 1);
				newChar.registerShortCut(shortcut);
			}
			if (skill.getId() == 1216)
			{
				shortcut = new L2ShortCut(10, 0, 2, skill.getId(), skill.getLevel(), 1);
				newChar.registerShortCut(shortcut);
			}
			if (Config.DEBUG)
			{
				_log.fine("Adding starter skill:" + skill.getId() + " / " + skill.getLevel());
			}
		}
		startTutorialQuest(newChar);
		L2GameClient.saveCharToDisk(newChar);
		newChar.deleteMe(); // release the world of this character and it's inventory
		if (Config.DEBUG)
		{
			_log.fine("Character init end");
		}
	}
	
	public void startTutorialQuest(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("255_Tutorial");
		Quest q = null;
		if (qs == null)
		{
			q = QuestManager.getInstance().getQuest("255_Tutorial");
		}
		if (q != null)
		{
			q.newQuestState(player);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__0B_CHARACTERCREATE;
	}
}