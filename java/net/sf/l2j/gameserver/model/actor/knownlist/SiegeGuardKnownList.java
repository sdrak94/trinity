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
package net.sf.l2j.gameserver.model.actor.knownlist;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeGuardInstance;

public class SiegeGuardKnownList extends AttackableKnownList
{
    // =========================================================
    // Data Field

    // =========================================================
    // Constructor
    public SiegeGuardKnownList(L2SiegeGuardInstance activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    @Override
	public boolean addKnownObject(L2Object object, boolean force)
    {
        if (!super.addKnownObject(object, force)) return false;

        // Check if siege is in progress
        if (getActiveChar().getCastle() != null && getActiveChar().getCastle().getSiege().getIsInProgress())
        {
            L2PcInstance player = null;
            if (object instanceof L2PcInstance)
                player = (L2PcInstance) object;
            else if (object instanceof L2Summon)
                player = ((L2Summon)object).getOwner();

            // Check if player is not the defender
            if (player != null && (player.getClan() == null || getActiveChar().getCastle().getSiege().getAttackerClan(player.getClan()) != null))
            {
                //if (Config.DEBUG) _log.fine(getObjectId()+": PK "+player.getObjectId()+" entered scan range");
                if (getActiveChar().getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
                    getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);//(L2Character)object);
            }

        }

        return true;
    }
    @Override
	public boolean addKnownObject(L2Object object)
    {
        return addKnownObject(object, false);
    }

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    @Override
	public final L2SiegeGuardInstance getActiveChar() { return (L2SiegeGuardInstance)super.getActiveChar(); }
}
