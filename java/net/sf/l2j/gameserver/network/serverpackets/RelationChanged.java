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

import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 * @author  Luca Baldi
 */
public final class RelationChanged extends L2GameServerPacket
{
	public static final int RELATION_PARTY1       = 0x00001; // party member
	public static final int RELATION_PARTY2       = 0x00002; // party member
	public static final int RELATION_PARTY3       = 0x00004; // party member
	public static final int RELATION_PARTY4       = 0x00008; // party member (for information, see L2PcInstance.getRelation())
	public static final int RELATION_PARTYLEADER  = 0x00010; // true if is party leader
	public static final int RELATION_HAS_PARTY    = 0x00020; // true if is in party
	public static final int RELATION_CLAN_MEMBER  = 0x00040; // true if is in clan
	public static final int RELATION_LEADER 	  = 0x00080; // true if is clan leader
	public static final int RELATION_INSIEGE   	  = 0x00200; // true if in siege
	public static final int RELATION_ATTACKER     = 0x00400; // true when attacker
	public static final int RELATION_ALLY         = 0x00800; // blue siege icon, cannot have if red
	public static final int RELATION_ENEMY        = 0x01000; // true when red icon, doesn't matter with blue
	public static final int RELATION_1SIDED_WAR   = 0x08000; // single fist
	public static final int RELATION_MUTUAL_WAR   = 0x04000; // double fist

	private static final String _S__CE_RELATIONCHANGED = "[S] ce RelationChanged";

	private int _objId, _relation, _autoAttackable, _karma, _pvpFlag;

	public RelationChanged(L2Playable activeChar, int relation, boolean autoattackable)
	{
		_objId = activeChar.getObjectId();
		_relation = relation;
		_autoAttackable = autoattackable ? 1 : 0;

		if (activeChar instanceof L2PcInstance)
		{
			_karma = ((L2PcInstance)activeChar).getKarma();
			_pvpFlag = ((L2PcInstance)activeChar).getPvpFlag();
			_invisible = ((L2PcInstance)activeChar).isInvisible();
		}
		else if (activeChar instanceof L2Summon)
		{
			_karma =  ((L2Summon)activeChar).getOwner().getKarma();
			_pvpFlag = ((L2Summon)activeChar).getOwner().getPvpFlag();
			_invisible = ((L2Summon)activeChar).getOwner().isInvisible();
		}
	}

	/**
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected final void writeImpl()
	{
		writeC(0xce);
		writeD(_objId);
		writeD(_relation);
		writeD(_autoAttackable);
		writeD(_karma);
		writeD(_pvpFlag);
	}

	/**
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__CE_RELATIONCHANGED;
	}

}
