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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Point3D;

public final class RequestExMagicSkillUseGround extends L2GameClientPacket
{
    private static final String _C__D0_2F_REQUESTEXMAGICSKILLUSEGROUND = "[C] D0:2F RequestExMagicSkillUseGround";
    private static Logger _log = Logger.getLogger(RequestExMagicSkillUseGround.class.getName());
    
    private int _x;
    private int _y;
    private int _z;
    private int _skillId;
    private boolean _ctrlPressed;
    private boolean _shiftPressed;
    
    @Override
    protected void readImpl()
    {
        _x = readD();
        _y = readD();
        _z = readD();
        _skillId = readD();
        _ctrlPressed = readD() != 0;
        _shiftPressed = readC() != 0;
    }
    
    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    protected void runImpl()
    {
        // Get the current L2PcInstance of the player
        L2PcInstance activeChar = getClient().getActiveChar();
        
        if (activeChar == null)
            return;

        // Get the level of the used skill
        int level = activeChar.getSkillLevel(_skillId);
        if (level <= 0) 
        {
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
        
        // Get the L2Skill template corresponding to the skillID received from the client
        L2Skill skill = SkillTable.getInstance().getInfo(_skillId, level);
        
        // Check the validity of the skill
        if (skill != null)
        {
            activeChar.setCurrentSkillWorldPosition(new Point3D(_x , _y, _z));

            // normally magicskilluse packet turns char client side but for these skills, it doesn't (even with correct target)
            activeChar.setHeading(Util.calculateHeadingFrom(activeChar.getX(), activeChar.getY(), _x , _y));
            activeChar.broadcastPacket(new ValidateLocation(activeChar));    	

            activeChar.useMagic(skill, _ctrlPressed, _shiftPressed);
        }
        else
        {
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            _log.warning("No skill found with id " + _skillId + " and level " + level + " !!");
        }
    }
    
    /**
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _C__D0_2F_REQUESTEXMAGICSKILLUSEGROUND;
    }
}
