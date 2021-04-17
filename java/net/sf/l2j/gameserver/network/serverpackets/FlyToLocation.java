package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.L2Character;

public final class FlyToLocation extends L2GameServerPacket
{
	private final int _id, _destX, _destY, _destZ, _chaX, _chaY, _chaZ;
    private final FlyType _type;
    
    public enum FlyType
    {
        THROW_UP,
        THROW_HORIZONTAL,
        DUMMY, // no effect
        CHARGE;
    }    
    public FlyToLocation(int id, int x, int y, int z, int tx, int ty, int tz, FlyType type)
    {
    	_id = id;
    	_chaX = x;
    	_chaY = y;
    	_chaZ = z;
    	_destX = tx;
    	_destY = ty;
    	_destZ = tz;
    	_type = type;
    }    

    public FlyToLocation(L2Character effected, int tx, int ty, int tz, FlyType type)
    {
    	_id = effected.getObjectId();
    	_chaX = effected.getX();
    	_chaY = effected.getY();
    	_chaZ = effected.getZ();
    	_destX = tx;
    	_destY = ty;
    	_destZ = tz;
    	_type = type;
    }

	@Override
    public String getType()
    {
        return "[S] 0xd4 FlyToLocation";
    }
    
    @Override
    protected void writeImpl()
    {
    	writeC(0xd4);
        writeD(_id);
        writeD(_destX);
        writeD(_destY);
        writeD(_destZ);
        writeD(_chaX);
        writeD(_chaY);
        writeD(_chaZ);
        writeD(_type.ordinal());
    }    
}