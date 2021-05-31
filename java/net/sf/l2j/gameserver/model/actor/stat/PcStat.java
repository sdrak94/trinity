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
package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2PetDataTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExVitalityPointInfo;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.skills.Stats;

public class PcStat extends PlayableStat
{
	// private static Logger _log = Logger.getLogger(PcStat.class.getName());
	// =========================================================
	// Data Field
	private int				_oldMaxHp;									// stats watch
	private int				_oldMaxMp;									// stats watch
	private int				_oldMaxCp;									// stats watch
	private float			_vitalityPoints		= 1;
	private byte			_vitalityLevel		= 0;

	private byte _level = 1;
	
	public static final int	VITALITY_LEVELS[]	=
	{
		240, 1800, 14600, 18200, 20000
	};
	public static final int	MAX_VITALITY_POINTS	= VITALITY_LEVELS[4];
	public static final int	MIN_VITALITY_POINTS	= 1;
	
	// =========================================================
	// Constructor
	public PcStat(L2PcInstance activeChar)
	{
		super(activeChar);
	}
	
	// =========================================================
	// Method - Public
	@Override
	public boolean addExp(long value)
	{
		L2PcInstance activeChar = getActiveChar();
		// Allowed to gain exp?
		if (!getActiveChar().getAccessLevel().canGainExp() && getActiveChar().isInParty())
			return false;
		if (!super.addExp(value))
			return false;
		// Set new karma
		if (!activeChar.isCursedWeaponEquipped() && activeChar.getKarma() > 0 && (activeChar.isGM() || !activeChar.isInsideZone(L2Character.ZONE_PVP)))
		{
			int karmaLost = activeChar.calculateKarmaLost(value);
			if (karmaLost > 0)
				activeChar.setKarma(activeChar.getKarma() - karmaLost);
		}
		// EXP status update currently not used in retail
		activeChar.sendPacket(new UserInfo(activeChar));
		activeChar.sendPacket(new ExBrExtraUserInfo(activeChar));
		return true;
	}
	
	/**
	 * Add Experience and SP rewards to the L2PcInstance, remove its Karma (if necessary) and Launch increase level task.<BR>
	 * <BR>
	 * <B><U> Actions </U> :</B><BR>
	 * <BR>
	 * <li>Remove Karma when the player kills L2MonsterInstance</li>
	 * <li>Send a Server->Client packet StatusUpdate to the L2PcInstance</li>
	 * <li>Send a Server->Client System Message to the L2PcInstance</li>
	 * <li>If the L2PcInstance increases it's level, send a Server->Client packet SocialAction (broadcast)</li>
	 * <li>If the L2PcInstance increases it's level, manage the increase level task (Max MP, Max MP, Recommandation, Expertise and beginner skills...)</li>
	 * <li>If the L2PcInstance increases it's level, send a Server->Client packet UserInfo to the L2PcInstance</li><BR>
	 * <BR>
	 *
	 * @param addToExp
	 *            The Experience value to add
	 * @param addToSp
	 *            The SP value to add
	 */
	@Override
	public boolean addExpAndSp(long addToExp, int addToSp)
	{
		float ratioTakenByPet = 0;
		// Allowd to gain exp/sp?
		L2PcInstance activeChar = getActiveChar();
		// if this player has a pet that takes from the owner's Exp, give the pet Exp now
		if (activeChar.getPet() instanceof L2PetInstance)
		{
			L2PetInstance pet = (L2PetInstance) activeChar.getPet();
			ratioTakenByPet = pet.getPetData().getOwnerExpTaken();
			// only give exp/sp to the pet by taking from the owner if the pet has a non-zero, positive ratio
			// allow possible customizations that would have the pet earning more than 100% of the owner's exp/sp
			if (ratioTakenByPet > 0 && !pet.isDead())
				pet.addExpAndSp((long) (addToExp * ratioTakenByPet), (int) (addToSp * ratioTakenByPet));
			// now adjust the max ratio to avoid the owner earning negative exp/sp
			if (ratioTakenByPet > 1)
				ratioTakenByPet = 1;
			addToExp = (long) (addToExp * (1 - ratioTakenByPet));
			addToSp = (int) (addToSp * (1 - ratioTakenByPet));
		}
		if (!super.addExpAndSp(addToExp, addToSp))
			return false;
		// Send a Server->Client System Message to the L2PcInstance
		if ((int) addToExp == 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_SP);
			sm.addNumber(addToSp);
			activeChar.sendPacket(sm);
		}
		else
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_EARNED_S1_EXP_AND_S2_SP);
			sm.addNumber((int) addToExp);
			sm.addNumber(addToSp);
			activeChar.sendPacket(sm);
		}
		return true;
	}
	
	public boolean addExpAndSp(long addToExp, int addToSp, boolean useVitality)
	{
		if (useVitality && Config.ENABLE_VITALITY)
		{
			switch (_vitalityLevel)
			{
				case 1:
					addToExp *= Config.RATE_VITALITY_LEVEL_1;
					addToSp *= Config.RATE_VITALITY_LEVEL_1;
					break;
				case 2:
					addToExp *= Config.RATE_VITALITY_LEVEL_2;
					addToSp *= Config.RATE_VITALITY_LEVEL_2;
					break;
				case 3:
					addToExp *= Config.RATE_VITALITY_LEVEL_3;
					addToSp *= Config.RATE_VITALITY_LEVEL_3;
					break;
				case 4:
					addToExp *= Config.RATE_VITALITY_LEVEL_4;
					addToSp *= Config.RATE_VITALITY_LEVEL_4;
					break;
			}
		}
		return addExpAndSp(addToExp, addToSp);
	}
	
	@Override
	public boolean removeExpAndSp(long addToExp, int addToSp)
	{
		return removeExpAndSp(addToExp, addToSp, true);
	}
	
	public boolean removeExpAndSp(long addToExp, int addToSp, boolean sendMessage)
	{
		if (!super.removeExpAndSp(addToExp, addToSp))
			return false;
		if (sendMessage)
		{
			// Send a Server->Client System Message to the L2PcInstance
			SystemMessage sm = new SystemMessage(SystemMessageId.EXP_DECREASED_BY_S1);
			sm.addNumber((int) addToExp);
			getActiveChar().sendPacket(sm);
			sm = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
			sm.addNumber(addToSp);
			getActiveChar().sendPacket(sm);
		}
		return true;
	}
	
	@Override
	public final boolean addLevel(byte value)
	{
		if (getLevel() + value > Experience.MAX_LEVEL - 1)
			return false;
		boolean levelIncreased = super.addLevel(value);
		if (levelIncreased)
		{
			QuestState qs = getActiveChar().getQuestState("255_Tutorial");
			if (qs != null && qs.getQuest() != null)
				qs.getQuest().notifyEvent("CE40", null, getActiveChar());
			getActiveChar().setCurrentCp(getMaxCp());
			getActiveChar().broadcastPacket(new SocialAction(getActiveChar().getObjectId(), SocialAction.LEVEL_UP));
			getActiveChar().sendPacket(new SystemMessage(SystemMessageId.YOU_INCREASED_YOUR_LEVEL));
			/*
			 * if (getLevel() == 90 && getActiveChar().getBaseClass() == getActiveChar().getActiveClass())
			 * getActiveChar().rewardRacialCirclets();
			 */
		}
		getActiveChar().rewardSkills(); // Give Expertise skill of this level
		if (getActiveChar().getClan() != null)
		{
			getActiveChar().getClan().updateClanMember(getActiveChar());
			getActiveChar().getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(getActiveChar()));
		}
		if (getActiveChar().isInParty())
			getActiveChar().getParty().recalculatePartyLevel(); // Recalculate the party level
		if (getActiveChar().isTransformed() || getActiveChar().isInStance())
			getActiveChar().getTransformation().onLevelUp();
		StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
		su.addAttribute(StatusUpdate.LEVEL, getLevel());
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
		getActiveChar().sendPacket(su);
		// Send a Server->Client packet UserInfo to the L2PcInstance
		getActiveChar().sendPacket(new UserInfo(getActiveChar()));
		getActiveChar().sendPacket(new ExBrExtraUserInfo(getActiveChar()));
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			if (getActiveChar().getCounters().maxPlayerLevelReach < getLevel())
			{
				getActiveChar().getCounters().maxPlayerLevelReach = getLevel();
			}
		}
		return levelIncreased;
	}
	
	@Override
	public boolean addSp(int value)
	{
		if (!super.addSp(value))
			return false;
		StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
		su.addAttribute(StatusUpdate.SP, getSp());
		getActiveChar().sendPacket(su);
		return true;
	}
	
	@Override
	public final long getExpForLevel(int level)
	{
		return Experience.LEVEL[level];
	}
	// =========================================================
	// Method - Private
	
	// =========================================================
	// Property - Public
	@Override
	public final L2PcInstance getActiveChar()
	{
		return (L2PcInstance) super.getActiveChar();
	}
	
	@Override
	public final long getExp()
	{
		if (getActiveChar().isSubClassActive())
			return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getExp();
		return super.getExp();
	}
	
	@Override
	public final void setExp(long value)
	{
		if (getActiveChar().isSubClassActive())
			getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setExp(value);
		else
			super.setExp(value);
	}
	
	@Override
	public final byte getLevel()
	{
		try
		{
			if (getActiveChar().isSubClassActive())
				return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getLevel();
		}
		catch (Exception e)
		{}
		return super.getLevel();
	}

	public byte getBaseLevel()
	{
		return _level;
	}

	public final byte getBaseClassLevel()
	{
		return super.getLevel();
	}
	
	@Override
	public final void setLevel(byte value)
	{
		if (value > Experience.MAX_LEVEL - 1)
			value = Experience.MAX_LEVEL - 1;
		if (getActiveChar().isSubClassActive())
			getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setLevel(value);
		else
		{
			_level = value;
			super.setLevel(value);
		}
	}
	
	@Override
	public final int getMaxCp()
	{
		int val = super.getMaxCp();
		final L2PcInstance player = _activeChar.getActingPlayer();
		if (player == null)
			return 1;
		if (player.getClassId().level() >= 2 && !player.isInOlympiadMode())
		{
			int bonus = 0;
			final int pvps = player.getPvpKills();
			if (pvps <= 1000)
			{
				bonus += pvps;
			}
			else if (pvps <= 2000)
			{
				bonus += (pvps - 1000) / 2;
				bonus += 1000;
			}
			else if (pvps <= 3000)
			{
				bonus += (pvps - 2000) / 4;
				bonus += 1000 / 2;
				bonus += 1000;
			}
			else if (pvps <= 4000)
			{
				bonus += (pvps - 3000) / 8;
				bonus += 1000 / 4;
				bonus += 1000 / 2;
				bonus += 1000;
			}
			else if (pvps <= 5000)
			{
				bonus += (pvps - 4000) / 16;
				bonus += 1000 / 8;
				bonus += 1000 / 4;
				bonus += 1000 / 2;
				bonus += 1000;
			}
			else if (pvps <= 6000)
			{
				bonus += (pvps - 5000) / 32;
				bonus += 1000 / 16;
				bonus += 1000 / 8;
				bonus += 1000 / 4;
				bonus += 1000 / 2;
				bonus += 1000;
			}
			else if (pvps <= 7000)
			{
				bonus += (pvps - 6000) / 64;
				bonus += 1000 / 32;
				bonus += 1000 / 16;
				bonus += 1000 / 8;
				bonus += 1000 / 4;
				bonus += 1000 / 2;
				bonus += 1000;
			}
			else if (pvps <= 8000)
			{
				bonus += (pvps - 7000) / 128;
				bonus += 1000 / 64;
				bonus += 1000 / 32;
				bonus += 1000 / 16;
				bonus += 1000 / 8;
				bonus += 1000 / 4;
				bonus += 1000 / 2;
				bonus += 1000;
			}
			else
			{
				bonus += (pvps - 8000) / 256;
				bonus += 1000 / 128;
				bonus += 1000 / 64;
				bonus += 1000 / 32;
				bonus += 1000 / 16;
				bonus += 1000 / 8;
				bonus += 1000 / 4;
				bonus += 1000 / 2;
				bonus += 1000;
			}
			if (bonus > 0)
			{
				if (player.isArcherClass() || player.isKamaelClass() || player.isBDSWSClass())
					bonus /= 1.12;
				else if (player.isDaggerClass() || player.isMageClass())
					bonus /= 1.24;
				val += bonus;
			}
		}
		if (val != _oldMaxCp)
		{
			_oldMaxCp = val;
			// Launch a regen task if the new Max HP is higher than the old one
			if (getActiveChar().getStatus().getCurrentCp() != val)
				getActiveChar().getStatus().setCurrentCp(getActiveChar().getStatus().getCurrentCp()); // trigger start of regeneration
		}
		return val;
	}
	
	@Override
	public final int getMaxHp()
	{
		// Get the Max HP (base+modifier) of the L2PcInstance
		final int val = super.getMaxHp();
		if (val != _oldMaxHp)
		{
			_oldMaxHp = val;
			// Launch a regen task if the new Max HP is higher than the old one
			if (getActiveChar().getStatus().getCurrentHp() != val)
				getActiveChar().getStatus().setCurrentHp(getActiveChar().getStatus().getCurrentHp()); // trigger start of regeneration
		}
		return val;
	}
	
	@Override
	public final int getMaxMp()
	{
		// Get the Max MP (base+modifier) of the L2PcInstance
		int val = super.getMaxMp();
		if (val != _oldMaxMp)
		{
			_oldMaxMp = val;
			// Launch a regen task if the new Max MP is higher than the old one
			if (getActiveChar().getStatus().getCurrentMp() != val)
				getActiveChar().getStatus().setCurrentMp(getActiveChar().getStatus().getCurrentMp()); // trigger start of regeneration
		}
		return val;
	}
	
	@Override
	public final int getSp()
	{
		if (getActiveChar().isSubClassActive())
			return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getSp();
		return super.getSp();
	}
	
	@Override
	public final void setSp(int value)
	{
		if (getActiveChar().isSubClassActive())
			getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setSp(value);
		else
			super.setSp(value);
	}
	
	@Override
	public int getRunSpeed()
	{
		if (getActiveChar() == null)
			return 1;
		int val = super.getRunSpeed();
		L2PcInstance player = getActiveChar();
		if (player.isMounted())
		{
			int baseRunSpd = L2PetDataTable.getInstance().getPetData(player.getMountNpcId(), player.getMountLevel()).getPetSpeed();
			val = (int) calcStat(Stats.RUN_SPEED, baseRunSpd, null, null);
		}
		val += Config.RUN_SPD_BOOST;
		// Apply max run speed cap.
		if (val > Config.MAX_RUN_SPEED && !getActiveChar().isGM())
			return Config.MAX_RUN_SPEED;
		return val;
	}
	
	@Override
	public int getPAtkSpd(L2Skill skill)
	{
		int val = super.getPAtkSpd(skill);
		if (getActiveChar().isInOlympiadMode() && getActiveChar().isProphet())
			val /= 1.6;
		if (!getActiveChar().isGM())
		{
			final int max = (int) (getActiveChar().calcStat(Stats.POWER_ATTACK_SPEED_MAX_ADD, Config.MAX_PATK_SPEED, null, skill));
			if (val > max)
				return max;
		}
		return val;
	}
	
	@Override
	public int getEvasionRate(L2Character target)
	{
		int val = super.getEvasionRate(target);
		if (val > Config.MAX_EVASION && !getActiveChar().isGM())
			return Config.MAX_EVASION;
		return val;
	}
	
	@Override
	public int getMAtkSpd(L2Skill skill)
	{
		int val = super.getMAtkSpd(skill);
		if (getActiveChar().isInOlympiadMode())
		{
			if (getActiveChar().isProphet())
				val /= 2.5;
		}
		if (!getActiveChar().isGM() && val > getActiveChar().calcStat(Stats.CAST_SPEED_MAX_ADD, Config.MAX_MATK_SPEED, null, skill))
			return (int) (getActiveChar().calcStat(Stats.CAST_SPEED_MAX_ADD, Config.MAX_MATK_SPEED, null, skill));
		return val;
	}
	
	@Override
	public float getMovementSpeedMultiplier()
	{
		if (getActiveChar() == null)
			return 1;
		if (getActiveChar().isMounted())
			return getRunSpeed() * 1f / L2PetDataTable.getInstance().getPetData(getActiveChar().getMountNpcId(), getActiveChar().getMountLevel()).getPetSpeed();
		return super.getMovementSpeedMultiplier();
	}
	
	@Override
	public int getWalkSpeed()
	{
		if (getActiveChar() == null)
			return 1;
		return (getRunSpeed() * 70) / 100;
	}
	
	private void updateVitalityLevel(boolean quiet)
	{
		final byte level;
		if (_vitalityPoints <= VITALITY_LEVELS[0])
			level = 0;
		else if (_vitalityPoints <= VITALITY_LEVELS[1])
			level = 1;
		else if (_vitalityPoints <= VITALITY_LEVELS[2])
			level = 2;
		else if (_vitalityPoints <= VITALITY_LEVELS[3])
			level = 3;
		else
			level = 4;
		if (!quiet && level != _vitalityLevel)
		{
			if (level < _vitalityLevel)
				getActiveChar().sendPacket(new SystemMessage(SystemMessageId.VITALITY_HAS_DECREASED));
			else
				getActiveChar().sendPacket(new SystemMessage(SystemMessageId.VITALITY_HAS_INCREASED));
			if (level == 0)
				getActiveChar().sendPacket(new SystemMessage(SystemMessageId.VITALITY_IS_EXHAUSTED));
			else if (level == 4)
				getActiveChar().sendPacket(new SystemMessage(SystemMessageId.VITALITY_IS_AT_MAXIMUM));
		}
		_vitalityLevel = level;
	}
	
	/*
	 * Return current vitality points in integer format
	 */
	public int getVitalityPoints()
	{
		return (int) _vitalityPoints;
	}
	
	/*
	 * Set current vitality points to this value
	 * if quiet = true - does not send system messages
	 */
	public void setVitalityPoints(int points, boolean quiet)
	{
		points = Math.min(Math.max(points, MIN_VITALITY_POINTS), MAX_VITALITY_POINTS);
		if (points == _vitalityPoints)
			return;
		_vitalityPoints = points;
		updateVitalityLevel(quiet);
		getActiveChar().sendPacket(new ExVitalityPointInfo(getVitalityPoints()));
	}
	
	public void updateVitalityPoints(float points, boolean useRates, boolean quiet)
	{
		if (points == 0 || !Config.ENABLE_VITALITY)
			return;
		if (useRates)
		{
			byte level = getLevel();
			if (level < 10)
				return;
			if (points < 0) // vitality consumed
			{
				int stat = (int) calcStat(Stats.VITALITY_CONSUME_RATE, 1, getActiveChar(), null);
				if (stat == 0) // is vitality consumption stopped ?
					return;
				if (stat < 0) // is vitality gained ?
					points = -points;
			}
			if (level >= 79)
				points *= 2;
			else if (level >= 76)
				points += points / 2;
			if (points > 0)
			{
				// vitality increased
				points *= Config.RATE_VITALITY_GAIN;
			}
			else
			{
				// vitality decreased
				points *= Config.RATE_VITALITY_LOST;
			}
		}
		if (points > 0)
		{
			points = Math.min(_vitalityPoints + points, MAX_VITALITY_POINTS);
		}
		else
		{
			points = Math.max(_vitalityPoints + points, MIN_VITALITY_POINTS);
		}
		if (points == _vitalityPoints)
			return;
		_vitalityPoints = points;
		updateVitalityLevel(quiet);
	}
	
}
