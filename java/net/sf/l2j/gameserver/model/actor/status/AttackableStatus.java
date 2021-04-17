package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.util.Util;

public class AttackableStatus extends NpcStatus
{
public AttackableStatus(L2Attackable activeChar)
{
	super(activeChar);
}

@Override
public final void reduceHp(double value, L2Character attacker)
{
	reduceHp(value, attacker, true, false, false, false);
}

@Override
public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHpConsumption, boolean bypassCP)
{
	if (getActiveChar().isDead())
		return;
	
	if (attacker != null && getActiveChar() instanceof L2RaidBossInstance && getActiveChar().getNpcId() != 25325)
	{
		if (!L2Attackable.RAID_SYSTEM_ENABLED && attacker instanceof L2Playable && !attacker.getActingPlayer().isGM())
			return;
		
		final L2PcInstance playa = attacker.getActingPlayer();
		
		if (playa == null)
			return;
		
		final L2Clan clan = playa.getClan();
		
		if (clan != null && clan.getRBkills() >= 8)
			return;
	}
	if (value > 0)
	{
		if (getActiveChar().isOverhit())
			getActiveChar().setOverhitValues(attacker, value);
		else
			getActiveChar().overhitEnabled(false);
	}
	else
		getActiveChar().overhitEnabled(false);
	
	super.reduceHp(value, attacker, awake, isDOT, isHpConsumption, false);
	
	if (!getActiveChar().isDead())	// And the attacker's hit didn't kill the mob, clear the over-hit flag
	{
		getActiveChar().overhitEnabled(false);
	}
	else //is dead
	{
		if (getActiveChar().getNpcId() == 25325) //barakiel
		{
			final L2Party party = attacker.getActingPlayer().getParty();
			
			if (party != null)
			{
				for (L2PcInstance player : party.getPartyMembers())
				{
					if (player != null && !player.isNoble())
					{
						if (player.getLevel() >= 76 && player.getClassId().level() == 3)
						{
							if (Util.checkIfInRange(1200, getActiveChar(), player, false))
							{
								player.sendMessage("Congratulations! You are now a Noblesse!");
								player.playSound("ItemSound.quest_finish");
								player.broadcastPacket(new SocialAction(player.getObjectId(), SocialAction.LEVEL_UP));
								player.setNoble(true);
								player.setFame(player.getFame()+50);
								player.sendMessage("Your fame increased by 50");
								player.healHP();
								player.addItem("Noblesse Circlet", 7694, 1, player, true);
								
								if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
								{
									player.getCounters().timesNoble++;
								}
							}
						}
					}
				}
			}
			else
			{
				final L2PcInstance attacka = attacker.getActingPlayer();
				
				if (attacka != null && !attacka.isNoble())
				{
					if (attacka.getLevel() >= 76 && attacka.getClassId().level() == 3)
					{
						attacka.sendMessage("Congratulations! You are now a Noblesse!");
						attacka.playSound("ItemSound.quest_finish");
						attacka.broadcastPacket(new MagicSkillUse(attacka, 5103, 1, 1000, 0));
						attacka.setNoble(true);
						attacka.setFame(attacka.getFame()+50);
						attacka.sendMessage("Your fame has increased by 50 for defeating Barakiel");
						attacka.healHP();
						attacka.addItem("Noblesse Circlet", 7694, 1, attacka, true);
						attacka.sendPacket(new UserInfo(attacka));
						attacka.sendPacket(new ExBrExtraUserInfo(attacka));
						
						if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
						{
							attacka.getCounters().timesNoble++;
						}
					}
				}
			}
		}
	}
}

@Override
public L2Attackable getActiveChar()
{
	return (L2Attackable)super.getActiveChar();
}
}