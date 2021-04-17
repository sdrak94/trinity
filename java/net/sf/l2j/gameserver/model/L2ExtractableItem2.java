package net.sf.l2j.gameserver.model;

import java.util.List;


public class L2ExtractableItem2
{
	private final int _itemId;
	private final List<L2ExtractableProductItem2> _products;
	
	public L2ExtractableItem2(final int itemid, final List<L2ExtractableProductItem2> products)
	{
		_itemId = itemid;
		_products = products;
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public List<L2ExtractableProductItem2> getProductItems()
	{
		return _products;
	}
}
