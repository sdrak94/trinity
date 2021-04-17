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

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author Layane
 *
 */
public class L2CabaleBufferInstance extends L2Npc
{
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player)) return;

		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			// The color to display in the select window is White
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2ArtefactInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

    private ScheduledFuture<?> _aiTask;

    private class CabalaAI implements Runnable
    {
        private L2CabaleBufferInstance _caster;

        protected CabalaAI(L2CabaleBufferInstance caster)
        {
            _caster = caster;
        }

        public void run()
        {
            boolean isBuffAWinner = false;
            boolean isBuffALoser = false;

            final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
            int losingCabal = SevenSigns.CABAL_NULL;

            if (winningCabal == SevenSigns.CABAL_DAWN)
                losingCabal = SevenSigns.CABAL_DUSK;
            else if (winningCabal == SevenSigns.CABAL_DUSK)
                losingCabal = SevenSigns.CABAL_DAWN;

            /**
             * For each known player in range, cast either the positive or negative buff.
             * <BR>
             * The stats affected depend on the player type, either a fighter or a mystic.
             * <BR><BR>
             * Curse of Destruction (Loser)<BR>
             *  - Fighters: -25% Accuracy, -25% Effect Resistance<BR>
             *  - Mystics: -25% Casting Speed, -25% Effect Resistance<BR>
             * <BR><BR>
             * Blessing of Prophecy (Winner)
             *  - Fighters: +25% Max Load, +25% Effect Resistance<BR>
             *  - Mystics: +25% Magic Cancel Resist, +25% Effect Resistance<BR>
             */
            Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
            //synchronized (getKnownList().getKnownPlayers())
			{
				for (L2PcInstance player : plrs)
				{
					if (player == null || player.isInvul())
						continue;

					final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
					
					if (playerCabal == winningCabal
					        && playerCabal != SevenSigns.CABAL_NULL
					        && _caster.getNpcId() == SevenSigns.ORATOR_NPC_ID)
					{
						if (!player.isMageClass())
						{
							if (handleCast(player, 4364))
							{
								isBuffAWinner = true;
								continue;
							}
						}
						else
						{
							if (handleCast(player, 4365))
							{
								isBuffAWinner = true;
								continue;
							}
						}
					}
					else if (playerCabal == losingCabal
					        && playerCabal != SevenSigns.CABAL_NULL
					        && _caster.getNpcId() == SevenSigns.PREACHER_NPC_ID)
					{
						if (!player.isMageClass())
						{
							if (handleCast(player, 4361))
							{
								isBuffALoser = true;
								continue;
							}
						}
						else
						{
							if (handleCast(player, 4362))
							{
								isBuffALoser = true;
								continue;
							}
						}
					}
					
					if (isBuffAWinner && isBuffALoser)
						break;
				}
			}
        }

        private boolean handleCast(L2PcInstance player, int skillId)
        {
            int skillLevel = (player.getLevel() > 40) ? 1 : 2;

            if (player.isDead() || !player.isVisible() || !isInsideRadius(player, getDistanceToWatchObject(player), false, false))
                return false;

            L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
            if (player.getFirstEffect(skill) == null)
            {
                skill.getEffects(_caster, player);
                broadcastPacket(new MagicSkillUse(_caster, player, skill.getId(), skillLevel, skill.getHitTime(), 0));
                SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
                sm.addSkillName(skill);
                player.sendPacket(sm);
                return true;
            }

            return false;
        }
    }


    public L2CabaleBufferInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);

        if (_aiTask != null)
        	_aiTask.cancel(true);

        _aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new CabalaAI(this), 3000, 3000);
    }

    @Override
	public void deleteMe()
    {
        if (_aiTask != null)
        {
        	_aiTask.cancel(true);
        	_aiTask = null;
        }

        super.deleteMe();
    }

    @Override
	public int getDistanceToWatchObject(L2Object object)
    {
        return 900;
    }

    @Override
	public boolean isAutoAttackable(L2Character attacker)
    {
        return false;
    }
}
