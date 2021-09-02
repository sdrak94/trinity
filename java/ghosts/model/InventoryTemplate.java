package ghosts.model;

import java.util.ArrayList;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import net.sf.l2j.gameserver.datatables.ArmorSetsTable;
import net.sf.l2j.gameserver.model.L2ArmorSet;

public class InventoryTemplate
{
	final ArrayList<InventoryItem> _inventoryItems = new ArrayList<>();

	public InventoryTemplate(final Node n)
	{
		for (Node n1 = n.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
		{
			if ("item".equalsIgnoreCase(n1.getNodeName()))
			{
				final NamedNodeMap nnm = n1.getAttributes();
				final int itemId = Integer.parseInt(nnm.getNamedItem("id").getNodeValue());
				
				
				final InventoryItem inventoryItem = new InventoryItem(itemId, n1);
				
				_inventoryItems.add(inventoryItem);
			}
			else if ("armorset".equalsIgnoreCase(n1.getNodeName()))
			{
				final NamedNodeMap nnm = n1.getAttributes();
				final int armorsetId = Integer.parseInt(nnm.getNamedItem("id").getNodeValue());
				
				final L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(armorsetId);
				
				for (final int itemId : armorSet.getAllBaseParts())
				{
					final InventoryItem inventoryItem = new InventoryItem(itemId, n1);
					_inventoryItems.add(inventoryItem);
				}
			}
		}
	}
	
	public ArrayList<InventoryItem> getInventoryItems()
	{
		return _inventoryItems;
	}


}
