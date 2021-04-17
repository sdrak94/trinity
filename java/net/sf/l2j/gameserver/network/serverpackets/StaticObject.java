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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;

/**
 * 
 *
 * @author  KenM
 */
public class StaticObject extends L2GameServerPacket
{
    private final int _staticObjectId;
    private final int _objectId;
    private final int _type;
    private final boolean _isTargetable;
    private final int _meshIndex;
    private final boolean _isClosed;
    private final boolean _isEnemy;
    private final int _maxHp;
    private final int _currentHp;
    private final boolean _showHp;
    private final int _damageGrade;
    
    public StaticObject(L2StaticObjectInstance staticObject)
    {
        _staticObjectId = staticObject.getStaticObjectId();
        _objectId = staticObject.getObjectId();
        _type = 0;
        _isTargetable = true;
        _meshIndex = staticObject.getMeshIndex();
        _isClosed = false;
        _isEnemy = false;
        _maxHp = 0;
        _currentHp = 0;
        _showHp = false;
        _damageGrade = 0;
    }
    
    public StaticObject(L2DoorInstance door, boolean showHp)
    {
        _staticObjectId = door.getDoorId();
        _objectId = door.getObjectId();
        _type = 1;
        _isTargetable = true;
        _meshIndex = 1;
        _isClosed = !door.getOpen();
        _isEnemy = door.isEnemy();
        _maxHp = door.getMaxHp();
        _currentHp = (int) door.getCurrentHp();
        _showHp = showHp;
        _damageGrade = door.getDamage();
    }
    
    @Override
    protected final void writeImpl()
    {
        writeC(0x9f);
        writeD(_staticObjectId);
        writeD(_objectId);
        writeD(_type);
        writeD(_isTargetable ? 1 : 0);
        writeD(_meshIndex);
        writeD(_isClosed ? 1 : 0);
        writeD(_isEnemy ? 1 : 0);
        writeD(_currentHp);
        writeD(_maxHp);
        writeD(_showHp ? 1 : 0);
        writeD(_damageGrade);
    }
    
    
    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    @Override
    public String getType()
    {
        return "[S] 9f StaticObject";
    }
}
