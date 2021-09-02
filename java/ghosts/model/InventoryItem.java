package ghosts.model;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import net.sf.l2j.util.Rnd;

public class InventoryItem
{
	
	private final int _itemId;
	
	private final int _count;

	private final int _minEnch;
	private final int _maxEnch;
	
	private final int _minEle;
	private final int _maxEle;
	
	private final boolean _isEquipped;
	
	private final boolean _isTemp;
	
	public InventoryItem(final int itemId, final Node n)
	{

		final NamedNodeMap nnm = n.getAttributes();
		_itemId = itemId;//Integer.parseInt(nnm.getNamedItem("id").getNodeValue());
		
		final Node nCount = nnm.getNamedItem("count");
		_count = nCount == null ? 1 : Integer.parseInt(nCount.getNodeValue());
		

		final Node nEnchant = nnm.getNamedItem("enchant");
		final String[] enchantsStr = nEnchant == null ? null : nEnchant.getNodeValue().split("-");
		_minEnch = enchantsStr == null ? 0 : Integer.parseInt(enchantsStr[0]);
		_maxEnch = enchantsStr == null ? 0 : enchantsStr.length == 1 ? _minEnch : Integer.parseInt(enchantsStr[1]);
		
		final Node nEle = nnm.getNamedItem("element");

		final String[] elesStr = nEnchant == null ? null : nEnchant.getNodeValue().split("-");
		_minEle = elesStr == null ? 0 : Integer.parseInt(elesStr[0]);
		_maxEle = elesStr == null ? 0 : elesStr.length == 1 ? _minEnch : Integer.parseInt(elesStr[1]);

		if (_minEnch > _maxEnch)
			throw new RuntimeException("minEnch > maxEnch");
		

		if (_minEle > _maxEle)
			throw new RuntimeException("minEle > maxele");
		
		final Node nEqu = nnm.getNamedItem("equipped");
		_isEquipped = nEqu == null? false : Boolean.parseBoolean(nEqu.getNodeValue());

		final Node nTemp = nnm.getNamedItem("temp");
		_isTemp = nTemp == null? false : Boolean.parseBoolean(nTemp.getNodeValue());
		
		
		
	}

	
	public boolean isTemp()
	{
		return _isTemp;
	}
	
	public boolean isIsEquipped()
	{
		return _isEquipped;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public int getCount()
	{
		return _count;
	}

	public int getMinEnch()
	{
		return _minEnch;
	}

	public int getMaxEnch()
	{
		return _maxEnch;
	}

	public int getMinEle()
	{
		return _minEle;
	}

	public int getMaxEle()
	{
		return _maxEle;
	}

	public int pickEnchant()
	{
		if (_minEnch == _maxEnch)
			return _minEnch;
		
		return Rnd.get(_minEnch, _maxEnch);
	}

}
