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

import javolution.util.FastList;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn.EnchantSkillDetail;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExEnchantSkillList.EnchantSkillType;

public final class ExEnchantSkillInfo extends L2GameServerPacket
{
    private static final String _S__FE_18_EXENCHANTSKILLINFO = "[S] FE:2a ExEnchantSkillInfo";
    private FastList<SkillEnchantDetailElement> _routes;
    
    private final int _id;
    private final EnchantSkillType _type;
    private final int _xpSpCostMultiplier;
    
    public ExEnchantSkillInfo(EnchantSkillType type, int id)
    {
        _routes = new FastList<SkillEnchantDetailElement>();
        _id = id;
        _type = type;
        _xpSpCostMultiplier = (type == EnchantSkillType.SAFE ? SkillTreeTable.SAFE_ENCHANT_COST_MULTIPLIER : SkillTreeTable.NORMAL_ENCHANT_COST_MULTIPLIER);
    }
    
    static class SkillEnchantDetailElement
    {
        public final int _level;
        public final int _rate;
        public final int _spCost;
        public final int _expCost;
        
        public SkillEnchantDetailElement(int level, int rate, int spCost, int expCost)
        {
            _level = level;
            _rate = rate;
            _spCost = spCost;
            _expCost = expCost;
        }
        
        public SkillEnchantDetailElement(L2PcInstance cha, EnchantSkillDetail esd)
        {
            this(esd.getLevel(), esd.getRate(cha), esd.getSpCost(), esd.getExp());
        }
        
        public SkillEnchantDetailElement(int rate, EnchantSkillDetail esd)
        {
            this(esd.getLevel(), rate, esd.getSpCost(), esd.getExp());
        }
    }

    
    public void addEnchantSkillDetail(L2PcInstance cha, EnchantSkillDetail esd)
    {
        _routes.add(new SkillEnchantDetailElement(cha, esd));
    }
    
    public void addEnchantSkillDetail(int rate, EnchantSkillDetail esd)
    {
        _routes.add(new SkillEnchantDetailElement(rate, esd));
    }
    
    public void addEnchantSkillDetail(int level, int rate, int spCost, int expCost)
    {
        _routes.add(new SkillEnchantDetailElement(level, rate, spCost, expCost));
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
     */
    @Override
	protected void writeImpl()
    {
        writeC(0xfe);
        writeH(0x2a);

        writeD(_type.ordinal()); // safe enchant
        writeD(_routes.size());
        
        for (SkillEnchantDetailElement sede : _routes)
        {
            writeD(_id);
            writeD(sede._level);
            writeD(sede._rate);
            writeD(sede._spCost * _xpSpCostMultiplier);
            writeQ(sede._expCost * _xpSpCostMultiplier);
            writeQ(0); // required item count
            writeD(0); // req type?
            writeD(0); // required itemId
            writeD(0); // ?
        }

    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.BasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _S__FE_18_EXENCHANTSKILLINFO;
    }

}