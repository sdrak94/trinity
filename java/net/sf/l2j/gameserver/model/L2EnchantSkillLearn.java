package net.sf.l2j.gameserver.model;

import java.util.List;

import javolution.util.FastTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.2.4.2 $ $Date: 2005/03/27 15:29:33 $
 */
@SuppressWarnings("unchecked")
public final class L2EnchantSkillLearn
{
    private final int _id;
    private final int _baseLvl;
    
    private List<EnchantSkillDetail>[] _enchantDetails = new List[0];

    public L2EnchantSkillLearn(int id, int baseLvl)
    {
        _id = id;
        _baseLvl = baseLvl;
    }

    /**
     * @return Returns the id.
     */
    public int getId()
    {
        return _id;
    }

    /**
     * @return Returns the minLevel.
     */
    public int getBaseLevel()
    {
        return _baseLvl;
    }
    
    public void addEnchantDetail(EnchantSkillDetail esd)
    {
        int enchantType = L2EnchantSkillLearn.getEnchantType(esd.getLevel());
        
        if (enchantType < 0)
        {
            throw new IllegalArgumentException("Skill enchantments should have level higher then 100");
        }
        else
        {
            if (enchantType >= _enchantDetails.length)
            {
                List<EnchantSkillDetail>[] newArray = new List[enchantType+1];
                System.arraycopy(_enchantDetails, 0, newArray, 0, _enchantDetails.length);
                _enchantDetails = newArray;
                _enchantDetails[enchantType] = new FastTable<EnchantSkillDetail>();
            }
            int index = L2EnchantSkillLearn.getEnchantIndex(esd.getLevel());
            _enchantDetails[enchantType].add(index, esd);
        }
    }
    
    public List<EnchantSkillDetail>[] getEnchantRoutes()
    {
        return _enchantDetails;
    }
    
    public EnchantSkillDetail getEnchantSkillDetail(int level)
    {
        int enchantType = L2EnchantSkillLearn.getEnchantType(level);
        if (enchantType < 0 || enchantType >= _enchantDetails.length)
        {
            return null;
        }
        int index = L2EnchantSkillLearn.getEnchantIndex(level);
        if (index < 0 || index >= _enchantDetails[enchantType].size())
        {
            return null;
        }
        return _enchantDetails[enchantType].get(index);
    }
    
    public static int getEnchantIndex(int level)
    {
        return (level % 100) - 1;
    }
    
    public static int getEnchantType(int level)
    {
        return ((level - 1) / 100) - 1;
    }
    
    public static class EnchantSkillDetail
    {
        private final int _level;
        private final int _spCost;
        private final int _minSkillLevel;
        private final int _exp;
        private final byte _rate76;

        
        public EnchantSkillDetail(int lvl, int minSkillLvl, int cost, int exp, byte rate76, byte rate77, byte rate78, byte rate79, byte rate80, byte rate81, byte rate82, byte rate83, byte rate84, byte rate85)
        {
            _level = lvl;
            _minSkillLevel = minSkillLvl;
            _spCost = cost;
            _exp = exp;
            _rate76 = rate76;
        }
        
        /**
         * @return Returns the level.
         */
        public int getLevel()
        {
            return _level;
        }
        
        /**
         * @return Returns the minSkillLevel.
         */
        public int getMinSkillLevel()
        {
            return _minSkillLevel;
        }

        /**
         * @return Returns the spCost.
         */
        public int getSpCost()
        {
            return _spCost;
        }
        public int getExp()
        {
            return _exp;
        }

        public byte getRate(L2PcInstance ply)
        {
        	return _rate76;
        }
    }
}