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
package net.sf.l2j.gameserver.model;

/**
 * This class ...
 *
 * @version $Revision: 1.2.4.1 $ $Date: 2005/03/27 15:29:32 $
 */
public final class TradeItem
{
    private int _objectId;
    private int _itemId;
    private long _price;
    private long _storePrice;
    private long _count;
	private int _enchantLevel;

	public TradeItem() {}

    public void setObjectId(int id)
    {
        _objectId = id;
    }

    public int getObjectId()
    {
        return _objectId;
    }

    public void setItemId(int id)
    {
        _itemId = id;
    }

    public int getItemId()
    {
        return _itemId;
    }

    public void setOwnersPrice(long price)
    {
        _price = price;
    }

    public long getOwnersPrice()
    {
        return _price;
    }

    public void setstorePrice(long price)
    {
        _storePrice = price;
    }

    public long getStorePrice()
    {
        return _storePrice;
    }

    public void setCount(long count)
    {
        _count = count;
    }

    public long getCount()
    {
        return _count;
    }

    public void setEnchantLevel(int enchant)
    {
		_enchantLevel = enchant;
    }

    public int getEnchantLevel()
    {
        return _enchantLevel;
    }

}
