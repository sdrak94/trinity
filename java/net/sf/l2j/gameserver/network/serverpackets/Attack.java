package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;

/**
 * sample
 * 06 8f19904b 2522d04b 00000000 80 950c0000 4af50000 08f2ffff 0000    - 0 damage (missed 0x80)
 * 06 85071048 bc0e504b 32000000 10 fc41ffff fd240200 a6f5ffff 0100 bc0e504b 33000000 10                                     3....

 * format
 * dddc dddh (ddc)
 *
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public class Attack extends L2GameServerPacket
{
public static final int HITFLAG_USESS = 0x10;
public static final int HITFLAG_CRIT = 0x20;
public static final int HITFLAG_SHLD = 0x40;
public static final int HITFLAG_MISS = 0x80;

public final class Hit
{
protected final int _targetId;
protected final int _damage;
protected int _flags;

Hit(L2Character target, int damage, boolean miss, boolean crit, byte shld)
{
	_targetId = target.getObjectId();
	_damage = damage;
	
	if (miss)
	{
		_flags = HITFLAG_MISS;
		return;
	}
	if (soulshot)
		_flags = HITFLAG_USESS | _ssGrade;
	if (crit)
		_flags |= HITFLAG_CRIT;
	if (shld > 0)
		_flags |= HITFLAG_SHLD;
}
}

private static final String _S__06_ATTACK = "[S] 33 Attack";
private final int _attackerObjId;
/*private final int _targetObjId;*/
public final boolean soulshot;
public final int _ssGrade;
private final int _x;
private final int _y;
private final int _z;
/*private final int _tx;
private final int _ty;
private final int _tz;*/
private final int _ignoreChance;
private Hit[] _hits;

/**
 * @param attacker the attacker L2Character
 * @param ss true if useing SoulShots
 */
public Attack(L2Character attacker, boolean useShots, int ssGrade, int ignoreChance)
{
	_attackerObjId = attacker.getObjectId();
	/*_targetObjId = target.getObjectId();*/
	soulshot = useShots;
	_ssGrade = ssGrade;
	_x = attacker.getX();
	_y = attacker.getY();
	_z = attacker.getZ();
	/*	_tx = target.getX();
	_ty = target.getY();
	_tz = target.getZ();*/
	_ignoreChance = ignoreChance;
}

public Hit createHit(L2Character target, int damage, boolean miss, boolean crit, byte shld)
{
	return new Hit( target, damage, miss, crit, shld );
}

public void hit(Hit... hits)
{
	if (_hits == null)
	{
		_hits = hits;
		return;
	}
	
	// this will only happen with pole attacks
	Hit[] tmp = new Hit[hits.length + _hits.length];
	System.arraycopy(_hits, 0, tmp, 0, _hits.length);
	System.arraycopy(hits, 0, tmp, _hits.length, hits.length);
	_hits = tmp;
}

/**
 * Return True if the Server-Client packet Attack conatins at least 1 hit.<BR><BR>
 */
public boolean hasHits()
{
	return _hits != null;
}

@Override
protected final void writeImpl()
{
	if (_ignoreChance > 0)
	{
		if (Rnd.get(100) < _ignoreChance)  //ignored auto target
		{
			final L2PcInstance activeChar = getClient().getActiveChar();
			
			if (activeChar != null && activeChar.getObjectId() == _hits[0]._targetId)  //main target
			{
				if (activeChar.getTarget() == null)
				{
					activeChar.setActionObjIdNoTarget(_attackerObjId);
					activeChar.setActionObjIdNoTargetTicks(GameTimeController.getGameTicks());
				}
			}
		}
	}
	
	writeC(0x33);
	
	writeD(_attackerObjId);
	writeD(_hits[0]._targetId);
	writeD(_hits[0]._damage);
	writeC(_hits[0]._flags);
	writeD(_x);
	writeD(_y);
	writeD(_z);
	writeH(_hits.length - 1);
	
	if(_hits.length > 1) // prevent sending useless packet while there is only one target.
	{
		for (int i=1; i < _hits.length; i++)
		{
			writeD(_hits[i]._targetId);
			writeD(_hits[i]._damage);
			writeC(_hits[i]._flags);
		}
	}
	
	/*	writeD(_tx);
	writeD(_ty);
	writeD(_tz);*/
}

@Override
public String getType()
{
	return _S__06_ATTACK;
}
}