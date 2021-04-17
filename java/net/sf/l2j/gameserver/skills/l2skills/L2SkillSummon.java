package net.sf.l2j.gameserver.skills.l2skills;

import luna.custom.LunaVariables;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2SkillSummon extends L2Skill
{
public static final int SKILL_CUBIC_MASTERY = 143;

private final int     _npcId;
private final float   _expPenalty;

private final boolean _isCubic;

// cubic AI
// Activation time for a cubic
private final int _activationtime;
// Activation chance for a cubic.
private final int _activationchance;

// What is the total lifetime of summons (in millisecs)
private final int _summonTotalLifeTime;
// How much lifetime is lost per second of idleness (non-fighting)
private final int _summonTimeLostIdle;
// How much time is lost per second of activity (fighting)
private final int _summonTimeLostActive;

// item consume time in milliseconds
private final int _itemConsumeTime;
// item consume count over time
private final int _itemConsumeOT;
// item consume id over time
private final int _itemConsumeIdOT;
// how many times to consume an item
private final int _itemConsumeSteps;

public L2SkillSummon(StatsSet set)
{
	super(set);
	
	_npcId      = set.getInteger("npcId", 0); // default for undescribed skills
	_expPenalty = set.getFloat ("expPenalty", 0.f);
	_isCubic    = set.getBool("isCubic", false);
	
	_activationtime= set.getInteger("activationtime", 8);
	_activationchance= set.getInteger("activationchance", 30);
	
	_summonTotalLifeTime= set.getInteger("summonTotalLifeTime", 1200000);  // 20 minutes default
	_summonTimeLostIdle= set.getInteger("summonTimeLostIdle", 0);
	_summonTimeLostActive= set.getInteger("summonTimeLostActive", 0);
	
	_itemConsumeOT = set.getInteger("itemConsumeCountOT", 0);
	_itemConsumeIdOT = set.getInteger("itemConsumeIdOT", 0);
	_itemConsumeTime = set.getInteger("itemConsumeTime", 0);
	_itemConsumeSteps = set.getInteger("itemConsumeSteps", 0);
}

public boolean checkCondition(L2Character activeChar)
{
	if (activeChar instanceof L2PcInstance)
	{
		L2PcInstance player = (L2PcInstance)activeChar;
		
		if (isCubic())
		{
			if (getTargetType(activeChar) != L2Skill.SkillTargetType.TARGET_SELF)
			{
				return true; //Player is always able to cast mass cubic skill
			}
			int mastery = player.getSkillLevel(SKILL_CUBIC_MASTERY);
			if (mastery < 0)
				mastery = 0;
			int count = player.getCubics().size();
			if (count > mastery)
			{
				activeChar.sendMessage("You already have "+count+" cubic(s).");
				return false;
			}
		}
		else
		{
			if (player.inObserverMode())
				return false;
			if (player.getPet() != null)
			{
				activeChar.sendMessage("You already have a pet.");
				return false;
			}
		}
	}
	return super.checkCondition(activeChar, null, false);
}

@Override
public void useSkill(L2Character caster, L2Object[] targets)
{
	if (caster.isAlikeDead() || !(caster instanceof L2PcInstance))
		return;
	
	L2PcInstance activeChar = (L2PcInstance) caster;
	
	if (_npcId == 0)
	{
		activeChar.sendMessage("Summon skill "+getId()+" not described yet");
		return;
	}
	
	if (_isCubic)
	{
		if (LunaVariables.getInstance().getKoreanCubicSkillsPrevented() && activeChar.isInKoreanZone())
		{
			activeChar.sendMessage("Cubics are not allowed during korean");
			return;
		}
		if (targets.length > 1) //Mass cubic skill
		{
			for (L2Object obj: targets)
			{
				if (!(obj instanceof L2PcInstance)) continue;
				L2PcInstance player = ((L2PcInstance)obj);
				int mastery = player.getSkillLevel(SKILL_CUBIC_MASTERY);
				if (mastery < 0)
					mastery = 0;
				if (mastery == 0 && !player.getCubics().isEmpty())
				{
					//Player can have only 1 cubic - we shuld replace old cubic with new one
					for (L2CubicInstance c: player.getCubics().values())
					{
						c.stopAction();
						c = null;
					}
					player.getCubics().clear();
				}
				//TODO: Should remove first cubic summoned and replace with new cubic
				if (player.getCubics().containsKey(_npcId))
				{
					L2CubicInstance cubic = player.getCubic(_npcId);
					if (cubic != null)
					{
						cubic.stopAction();
					}
					player.delCubic(_npcId);
				}
				if (player.getCubics().size() > mastery) continue;
				if (player == activeChar)
					player.addCubic(_npcId, getLevel(), getPower(), _activationtime, _activationchance, _summonTotalLifeTime, false);
				else // given by other player
					player.addCubic(_npcId, getLevel(), getPower(), _activationtime, _activationchance, _summonTotalLifeTime, true);
				player.broadcastUserInfo();
			}
			return;
		}
		else //normal cubic skill
		{
			int mastery = activeChar.getSkillLevel(SKILL_CUBIC_MASTERY);
			if (mastery < 0)
				mastery = 0;
			if (activeChar.getCubics().containsKey(_npcId))
			{
				L2CubicInstance cubic = activeChar.getCubic(_npcId);
				cubic.stopAction();
				activeChar.delCubic(_npcId);
			}
			if (activeChar.getCubics().size() > mastery) {
				if (Config.DEBUG)
					_log.fine("player can't summon any more cubics. ignore summon skill");
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CUBIC_SUMMONING_FAILED));
				return;
			}
			activeChar.addCubic(_npcId, getLevel(), getPower(), _activationtime, _activationchance, _summonTotalLifeTime, false);
			activeChar.broadcastUserInfo();
			return;
		}
	}
	
	if (activeChar.getPet() != null || activeChar.isMounted()) {
		if (Config.DEBUG)
			_log.fine("player has a pet already. ignore summon skill");
		return;
	}
	
	L2SummonInstance summon;
	L2NpcTemplate summonTemplate = NpcTable.getInstance().getTemplate(_npcId);
	if (summonTemplate == null)
	{
		_log.warning("Summon attempt for nonexisting NPC ID:"+_npcId+", skill ID:"+getId());
		return; // npcID doesn't exist
	}
	if (summonTemplate.type.equalsIgnoreCase("L2SiegeSummon"))
		summon = new L2SiegeSummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
	else if (summonTemplate.type.equalsIgnoreCase("L2MerchantSummon"))
		summon = new L2MerchantSummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
	else
		summon = new L2SummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
	
	summon.setName(summonTemplate.name);
	summon.setTitle(activeChar.getDisplayName());
	summon.setExpPenalty(_expPenalty);
	if (summon.getLevel() >= Experience.LEVEL.length)
	{
		summon.getStat().setExp(Experience.LEVEL[Experience.LEVEL.length - 1]);
		_log.warning("Summon ("+summon.getName()+") NpcID: "+summon.getNpcId()+" has a level above 75. Please rectify.");
	}
	else
	{
		summon.getStat().setExp(Experience.LEVEL[(summon.getLevel() % Experience.LEVEL.length)]);
	}
	summon.setCurrentHp(summon.getMaxHp());
	summon.setCurrentMp(summon.getMaxMp());
	summon.setHeading(activeChar.getHeading());
	summon.setRunning();
	summon.setInstanceId(activeChar.getInstanceId());
	if (!(summon instanceof L2MerchantSummonInstance))
		activeChar.setPet(summon);
	
	L2World.getInstance().storeObject(summon);
	summon.spawnMe(activeChar.getX()+50, activeChar.getY()+100, activeChar.getZ());
}

public final boolean isCubic()
{
	return _isCubic;
}

/**
 * @return Returns the itemConsume count over time.
 */
public final int getTotalLifeTime()
{
	return _summonTotalLifeTime;
}

/**
 * @return Returns the itemConsume count over time.
 */
public final int getTimeLostIdle()
{
	return _summonTimeLostIdle;
}

/**
 * @return Returns the itemConsumeId over time.
 */
public final int getTimeLostActive()
{
	return _summonTimeLostActive;
}

/**
 * @return Returns the itemConsume count over time.
 */
public final int getItemConsumeOT()
{
	return _itemConsumeOT;
}

/**
 * @return Returns the itemConsumeId over time.
 */
public final int getItemConsumeIdOT()
{
	return _itemConsumeIdOT;
}

/**
 * @return Returns the itemConsume count over time.
 */
public final int getItemConsumeSteps()
{
	return _itemConsumeSteps;
}

/**
 * @return Returns the itemConsume time in milliseconds.
 */
public final int getItemConsumeTime()
{
	return _itemConsumeTime;
}
}
