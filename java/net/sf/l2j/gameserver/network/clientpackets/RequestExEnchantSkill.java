package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn.EnchantSkillDetail;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutRegister;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.util.Rnd;

/**
 * Format (ch) dd
 * c: (id) 0xD0
 * h: (subid) 0x06
 * d: skill id
 * d: skill lvl
 * @author -Wooden-
 *
 */
public final class RequestExEnchantSkill extends L2GameClientPacket
{
private static final String _C__D0_07_REQUESTEXENCHANTSKILL = "[C] D0:07 RequestExEnchantSkill";
private static final Logger _log = Logger.getLogger(RequestAquireSkill.class.getName());
private static final Logger _logEnchant = Logger.getLogger("enchant");

private int _skillId;
private int _skillLvl;

@Override
protected void readImpl()
{
	_skillId = readD();
	_skillLvl = readD();
}

@Override
protected void runImpl()
{
	final L2PcInstance player = getClient().getActiveChar();
	if (player == null)
		return;
	
	if (player.getClassId().level() < 3) // requires to have 3rd class quest completed
	return;
	
	if (player.getLevel() < 76)
		return;
	
	if (player.isAccountLockedDown())
	{
		player.sendMessage("Your account is in lockdown");
		return;
	}
	
	L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);
	
	if (skill == null)
	{
		return;
	}
	
	final int costMultiplier = SkillTreeTable.NORMAL_ENCHANT_COST_MULTIPLIER;
	final int reqItemId = SkillTreeTable.NORMAL_ENCHANT_BOOK;
	
	L2EnchantSkillLearn s = SkillTreeTable.getInstance().getSkillEnchantmentBySkillId(_skillId);
	
	if (s == null)
	{
		return;
	}
	
	EnchantSkillDetail esd = s.getEnchantSkillDetail(_skillLvl);
	
	if (player.getSkillLevel(_skillId) != esd.getMinSkillLevel())
	{
		return;
	}
	
	int requiredSp = esd.getSpCost() * costMultiplier;
	int requiredExp = esd.getExp() * costMultiplier;
	int rate = esd.getRate(player);
	
	if (player.getSp() >= requiredSp)
	{
		long expAfter = player.getExp() - requiredExp;
		
		if (player.getExp() >= requiredExp && expAfter >= Experience.LEVEL[player.getLevel()])
		{
			// only first lvl requires book
			boolean usesBook = _skillLvl % 100 == 1; // 101, 201, 301 ...
			L2ItemInstance spb = player.getInventory().getItemByItemId(reqItemId);
			if (Config.ES_SP_BOOK_NEEDED && usesBook)
			{
				if (spb == null)// Haven't spellbook
				{
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL));
					return;
				}
			}
			
			boolean check;
			check = player.getStat().removeExpAndSp(requiredExp, requiredSp, false);
			if (Config.ES_SP_BOOK_NEEDED && usesBook)
			{
				check &= player.destroyItem("Consume", spb.getObjectId(), 1, null, true);
			}
			
			if (!check)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL));
				return;
			}
			
			// ok.  Destroy ONE copy of the book
			if (Rnd.get(100) <= rate)
			{
				if (Config.LOG_SKILL_ENCHANTS)
				{
					LogRecord record = new LogRecord(Level.INFO, "Success");
					record.setParameters(new Object[]{player, skill, spb, rate});
					record.setLoggerName("skill");
					_logEnchant.log(record);
				}
				
				player.addSkill(skill, true);
				
				if (Config.DEBUG)
				{
					_log.fine("Learned skill ID: "+_skillId+" Level: "+_skillLvl+" for "+requiredSp+" SP, "+requiredExp+" EXP.");
				}
				
				player.sendPacket(new UserInfo(player));
				player.sendPacket(new ExBrExtraUserInfo(player));
				
				SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_ENCHANTING_THE_SKILL_S1);
				sm.addSkillName(_skillId);
				player.sendPacket(sm);
			}
			else
			{
				player.addSkill(SkillTable.getInstance().getInfo(_skillId, s.getBaseLevel()), true);
				player.sendSkillList();
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_ENCHANT_THE_SKILL_S1));
				
				if (Config.LOG_SKILL_ENCHANTS)
				{
					LogRecord record = new LogRecord(Level.INFO, "Fail");
					record.setParameters(new Object[]{player, skill, spb, rate});
					record.setLoggerName("skill");
					_logEnchant.log(record);
				}
			}
			
			L2NpcInstance.showEnchantSkillList(player, false);
			
			updateSkillShortcuts(player);
		}
		else
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DONT_HAVE_ENOUGH_EXP_TO_ENCHANT_THAT_SKILL);
			player.sendPacket(sm);
		}
	}
	else
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DONT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL);
		player.sendPacket(sm);
	}
}

private void updateSkillShortcuts(L2PcInstance player)
{
	// update all the shortcuts to this skill
	L2ShortCut[] allShortCuts = player.getAllShortCuts();
	
	for (L2ShortCut sc : allShortCuts)
	{
		if (sc.getId() == _skillId && sc.getType() == L2ShortCut.TYPE_SKILL)
		{
			L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), player.getSkillLevel(_skillId), 1);
			player.sendPacket(new ShortCutRegister(newsc));
			player.registerShortCut(newsc);
		}
	}
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.BasePacket#getType()
 */
@Override
public String getType()
{
	return _C__D0_07_REQUESTEXENCHANTSKILL;
}
}