/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.MultiSellList;
import net.sf.l2j.gameserver.templates.item.L2Armor;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;

/**
 * Multisell list manager
 */
public class L2Multisell
{
	private static Logger						_log		= Logger.getLogger(L2Multisell.class.getName());
	private final List<MultiSellListContainer>	_entries	= new FastList<MultiSellListContainer>();
	
	public MultiSellListContainer getList(int id)
	{
		synchronized (_entries)
		{
			for (MultiSellListContainer list : _entries)
			{
				if (list.getListId() == id)
					return list;
			}
		}
		_log.warning("[L2Multisell] can't find list with id: " + id);
		return null;
	}
	
	private L2Multisell()
	{
		parseData();
	}
	
	public void reload()
	{
		parseData();
	}
	
	public static L2Multisell getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private void parseData()
	{
		_entries.clear();
		parse();
		_log.config("L2Multisell: Loaded " + _entries.size() + " lists.");
	}
	
	/**
	 * This will generate the multisell list for the items. There exist various
	 * parameters in multisells that affect the way they will appear:
	 * 1) inventory only:
	 * * if true, only show items of the multisell for which the
	 * "primary" ingredients are already in the player's inventory. By "primary"
	 * ingredients we mean weapon and armor.
	 * * if false, show the entire list.
	 * 2) maintain enchantment: presumably, only lists with "inventory only" set to true
	 * should sometimes have this as true. This makes no sense otherwise...
	 * * If true, then the product will match the enchantment level of the ingredient.
	 * if the player has multiple items that match the ingredient list but the enchantment
	 * levels differ, then the entries need to be duplicated to show the products and
	 * ingredients for each enchantment level.
	 * For example: If the player has a crystal staff +1 and a crystal staff +3 and goes
	 * to exchange it at the mammon, the list should have all exchange possibilities for
	 * the +1 staff, followed by all possibilities for the +3 staff.
	 * * If false, then any level ingredient will be considered equal and product will always
	 * be at +0
	 * 3) apply taxes: Uses the "taxIngredient" entry in order to add a certain amount of adena to the ingredients
	 *
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#runImpl()
	 */
	private MultiSellListContainer generateMultiSell(int listId, boolean inventoryOnly, L2PcInstance player, int npcId, double taxRate)
	{
		MultiSellListContainer listTemplate = L2Multisell.getInstance().getList(listId);
		MultiSellListContainer list = new MultiSellListContainer();
		if (listTemplate == null)
			return list;
		list = L2Multisell.getInstance().new MultiSellListContainer();
		list.setListId(listId);
		if (npcId != 0 && !listTemplate.checkNpcId(npcId))
			listTemplate.addNpcId(npcId);
		if (inventoryOnly)
		{
			if (player == null)
				return list;
			L2ItemInstance[] items;
			if (listTemplate.getMaintainEnchantment())
				items = player.getInventory().getUniqueItemsByEnchantLevel(false, false, false);
			else
				items = player.getInventory().getUniqueItems(false, false, false);
			int enchantLevel, elementId, elementValue, augmentId, fireVal, waterVal, windVal, earthVal, holyVal, darkVal;
			for (L2ItemInstance item : items)
			{
				// only do the matchup on equipable items that are not currently equipped
				// so for each appropriate item, produce a set of entries for the multisell list.
				if (!item.isEquipped() && ((item.getItem() instanceof L2Armor) || (item.getItem() instanceof L2Weapon)) || (item.getItemId() >= 77600 && item.getItemId() <= 77605))
				{
					enchantLevel = (listTemplate.getMaintainEnchantment() ? item.getEnchantLevel() : 0);
					augmentId = (listTemplate.getMaintainEnchantment() ? (item.getAugmentation() != null ? item.getAugmentation().getAugmentationId() : 0) : 0);
					elementId = (listTemplate.getMaintainEnchantment() ? item.getAttackElementType() : -2);
					elementValue = (listTemplate.getMaintainEnchantment() ? item.getAttackElementPower() : 0);
					fireVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.FIRE) : 0);
					waterVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.WATER) : 0);
					windVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.WIND) : 0);
					earthVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.EARTH) : 0);
					holyVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.HOLY) : 0);
					darkVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.DARK) : 0);
					// loop through the entries to see which ones we wish to include
					for (MultiSellEntry ent : listTemplate.getEntries())
					{
						boolean doInclude = false;
						// check ingredients of this entry to see if it's an entry we'd like to include.
						for (MultiSellIngredient ing : ent.getIngredients())
						{
							if (item.getItemId() == ing.getItemId())
							{
								doInclude = true;
								break;
							}
						}
						// manipulate the ingredients of the template entry for this particular instance shown
						// i.e: Assign enchant levels and/or apply taxes as needed.
						if (doInclude)
							list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), listTemplate.getMaintainEnchantment(), enchantLevel, augmentId, elementId, elementValue, fireVal, waterVal, windVal, earthVal, holyVal, darkVal, taxRate));
					}
				}
			} // end for each inventory item.
		} // end if "inventory-only"
		else
		// this is a list-all type
		{
			// if no taxes are applied, no modifications are needed
			for (MultiSellEntry ent : listTemplate.getEntries())
				list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), false, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, taxRate));
		}
		if (listTemplate.isAllowSell())
		{
			list.setAllowSell(true);
			list.setAllowSellMulti(listTemplate.getAllowSellMulti());
		}
		if (listTemplate.isAllowGem())
		{
			list.setAllowGem(true);
		}
		return list;
	}
	
	// Regarding taxation, the following is the case:
	// a) The taxes come out purely from the adena TaxIngredient
	// b) If the entry has no adena ingredients other than the taxIngredient, the resulting
	// amount of adena is appended to the entry
	// c) If the entry already has adena as an entry, the taxIngredient is used in order to increase
	// the count for the existing adena ingredient
	private MultiSellEntry prepareEntry(MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel, int augmentId, int elementId, int elementValue, int fireValue, int waterValue, int windValue, int earthValue, int holyValue, int darkValue, double taxRate)
	{
		MultiSellEntry newEntry = L2Multisell.getInstance().new MultiSellEntry();
		newEntry.setEntryId(templateEntry.getEntryId() * 100000 + enchantLevel);
		long adenaAmount = 0;
		for (MultiSellIngredient ing : templateEntry.getIngredients())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = L2Multisell.getInstance().new MultiSellIngredient(ing);
			// if taxes are to be applied, modify/add the adena count based on the template adena/ancient adena count
			if (ing.getItemId() == 57 && ing.isTaxIngredient())
			{
				if (applyTaxes)
					adenaAmount += Math.round(ing.getItemCount() * taxRate);
				continue; // do not adena yet, as non-taxIngredient adena entries might occur next (order not guaranteed)
			}
			else if (ing.getItemId() == 57) // && !ing.isTaxIngredient()
			{
				adenaAmount += ing.getItemCount();
				continue; // do not adena yet, as taxIngredient adena entries might occur next (order not guaranteed)
			}
			// if it is an armor/weapon, modify the enchantment level appropriately, if necessary
			// not used for clan reputation and fame
			else if (maintainEnchantment && newIngredient.getItemId() > 0)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
					newIngredient.setAugmentId(augmentId);
					newIngredient.setElementId(elementId);
					newIngredient.setElementValue(elementValue);
					newIngredient.setFireValue(fireValue);
					newIngredient.setWaterValue(waterValue);
					newIngredient.setWindValue(windValue);
					newIngredient.setEarthValue(earthValue);
					newIngredient.setHolyValue(holyValue);
					newIngredient.setDarkValue(darkValue);
				}
			}
			// finally, add this ingredient to the entry
			newEntry.addIngredient(newIngredient);
		}
		// now add the adena, if any.
		if (adenaAmount > 0)
		{
			newEntry.addIngredient(L2Multisell.getInstance().new MultiSellIngredient(57, adenaAmount, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, false, false, false, false, false, -1, -1));
		}
		// Now modify the enchantment level of products, if necessary
		for (MultiSellIngredient ing : templateEntry.getProducts())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = L2Multisell.getInstance().new MultiSellIngredient(ing);
			if (maintainEnchantment)
			{
				// if it is an armor/weapon, modify the enchantment level appropriately
				// (note, if maintain enchantment is "false" this modification will result to a +0)
				L2Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
					newIngredient.setAugmentId(augmentId);
					newIngredient.setElementId(elementId);
					newIngredient.setElementValue(elementValue);
					newIngredient.setFireValue(fireValue);
					newIngredient.setWaterValue(waterValue);
					newIngredient.setWindValue(windValue);
					newIngredient.setEarthValue(earthValue);
					newIngredient.setHolyValue(holyValue);
					newIngredient.setDarkValue(darkValue);
				}
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}
	
	public void separateAndSend(int listId, L2PcInstance player, int npcId, boolean inventoryOnly, double taxRate)
	{
		MultiSellListContainer list = generateMultiSell(listId, inventoryOnly, player, npcId, taxRate);
		MultiSellListContainer temp = new MultiSellListContainer();
		int page = 1;
		temp.setListId(list.getListId());
		for (MultiSellEntry e : list.getEntries())
		{
			if (temp.getEntries().size() == 40)
			{
				player.sendPacket(new MultiSellList(temp, page++, 0));
				temp = new MultiSellListContainer();
				temp.setListId(list.getListId());
			}
			temp.addEntry(e);
		}
		player.sendPacket(new MultiSellList(temp, page, 1));
		if (list.isAllowGem())
		{
			player.sendMessage("This list allows you to sell back to it and get 1/3 from the amount of Glittering Medals and Golds as Fortune Gems.");
		}
		if (list.isAllowSell())
		{
			if (list.getAllowSellMulti() == (double) 1 / 3)
				player.sendMessage("This list allows you to sell back to it and get 1/3 of the ingredients back by typing 963 in the Quantity field");
			else
				player.sendMessage("This list allows you to sell back to it and get " + new DecimalFormat("0").format(list.getAllowSellMulti() * 100) + "% of the ingredients back by typing 963 in the Quantity field");
		}
	}
	
	public class MultiSellEntry
	{
		private int								_entryId;
		private boolean							_swapped		= false;
		private final List<MultiSellIngredient>	_products		= new FastList<MultiSellIngredient>();
		private final List<MultiSellIngredient>	_ingredients	= new FastList<MultiSellIngredient>();
		
		/**
		 * @param entryId
		 *            The entryId to set.
		 */
		public void setEntryId(int entryId)
		{
			_entryId = entryId;
		}
		
		public void swap()
		{
			_swapped = true;
		}
		
		/**
		 * @return Returns the entryId.
		 */
		public int getEntryId()
		{
			return _entryId;
		}
		
		/**
		 * @param product
		 *            The product to add.
		 */
		public void addProduct(MultiSellIngredient product)
		{
			_products.add(product);
		}
		
		/**
		 * @return Returns the products.
		 */
		public List<MultiSellIngredient> getProducts()
		{
			if (_swapped)
				return _ingredients;
			return _products;
		}
		
		/**
		 * @param ingredients
		 *            The ingredients to set.
		 */
		public void addIngredient(MultiSellIngredient ingredient)
		{
			_ingredients.add(ingredient);
		}
		
		/**
		 * @return Returns the ingredients.
		 */
		public List<MultiSellIngredient> getIngredients()
		{
			if (_swapped)
				return _products;
			return _ingredients;
		}
		
		public int stackable()
		{
			if (_swapped)
			{
				for (MultiSellIngredient p : _ingredients)
				{
					if (p.getItemId() > 0)
					{
						L2Item template = ItemTable.getInstance().getTemplate(p.getItemId());
						if (template != null && !template.isStackable())
							return 0;
					}
				}
			}
			else
			{
				for (MultiSellIngredient p : _products)
				{
					if (p.getItemId() > 0)
					{
						L2Item template = ItemTable.getInstance().getTemplate(p.getItemId());
						if (template != null && !template.isStackable())
							return 0;
					}
				}
			}
			return 1;
		}
	}
	
	public class MultiSellIngredient
	{
		private int		_itemId, _enchantmentLevel, _element, _elementVal, _augment, _fireVal, _waterVal, _windVal, _earthVal, _holyVal, _darkVal, _elementalPenalty;
		private long	_itemCount;
		private boolean	_isTaxIngredient, _mantainIngredient, _keepElement, _keepAugment, _staticEnchant;
		private double	_elementalPenaltyMul;
		
		public MultiSellIngredient(int itemId, long itemCount, boolean isTaxIngredient, boolean mantainIngredient, boolean keepElements, boolean keepAug, boolean staticEnch, double elementalPenaltyMul, int elementalPenalty)
		{
			this(itemId, itemCount, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, isTaxIngredient, mantainIngredient, keepElements, keepAug, staticEnch, elementalPenaltyMul, elementalPenalty);
		}
		
		public MultiSellIngredient(int itemId, long itemCount, int enchantLevel, boolean isTaxIngredient, boolean mantainIngredient, boolean keepElements, boolean keepAug, boolean staticEnch, double elementalPenaltyMul, int elementalPenalty)
		{
			this(itemId, itemCount, enchantLevel, 0, -2, 0, 0, 0, 0, 0, 0, 0, isTaxIngredient, mantainIngredient, keepElements, keepAug, staticEnch, elementalPenaltyMul, elementalPenalty);
		}
		
		public MultiSellIngredient(int itemId, long itemCount, int enchantmentLevel, int augmentId, int elementId, int elementVal, int fireVal, int waterVal, int windVal, int earthVal, int holyVal, int darkVal, boolean isTaxIngredient, boolean mantainIngredient, boolean keepElements, boolean keepAug, boolean staticEnch, double elementalPenaltyMul, int elementalPenalty)
		{
			setItemId(itemId);
			setItemCount(itemCount);
			setEnchantmentLevel(enchantmentLevel);
			setKeepElement(keepElements);
			setElementalPenaltyMul(elementalPenaltyMul);
			setElementalPenalty(elementalPenalty);
			setKeepAug(keepAug);
			setStaticEnchant(staticEnch);
			setAugmentId(augmentId);
			setElementId(elementId);
			setElementValue(elementVal);
			setFireValue(fireVal);
			setWaterValue(waterVal);
			setWindValue(windVal);
			setEarthValue(earthVal);
			setHolyValue(holyVal);
			setDarkValue(darkVal);
			setIsTaxIngredient(isTaxIngredient);
			setMantainIngredient(mantainIngredient);
		}
		
		public MultiSellIngredient(MultiSellIngredient e)
		{
			_itemId = e.getItemId();
			_itemCount = e.getItemCount();
			_enchantmentLevel = e.getEnchantmentLevel();
			_isTaxIngredient = e.isTaxIngredient();
			_mantainIngredient = e.getMantainIngredient();
			_keepElement = e.getKeepElement();
			_keepAugment = e.getKeepAug();
			_staticEnchant = e.getStaticEnchant();
			_augment = e.getAugmentId();
			_element = e.getElementId();
			_elementVal = e.getElementVal();
			_fireVal = e.getFireVal();
			_waterVal = e.getWaterVal();
			_windVal = e.getWindVal();
			_earthVal = e.getEarthVal();
			_holyVal = e.getHolyVal();
			_darkVal = e.getDarkVal();
		}
		
		public void setAugmentId(int augment)
		{
			_augment = augment;
		}
		
		public void setElementId(int element)
		{
			_element = element;
		}
		
		public void setElementValue(int elementVal)
		{
			_elementVal = elementVal;
		}
		
		public void setFireValue(int val)
		{
			_fireVal = val;
		}
		
		public void setWaterValue(int val)
		{
			_waterVal = val;
		}
		
		public void setWindValue(int val)
		{
			_windVal = val;
		}
		
		public void setEarthValue(int val)
		{
			_earthVal = val;
		}
		
		public void setHolyValue(int val)
		{
			_holyVal = val;
		}
		
		public void setDarkValue(int val)
		{
			_darkVal = val;
		}
		
		public int getAugmentId()
		{
			return _augment;
		}
		
		public int getElementId()
		{
			return _element;
		}
		
		public int getElementVal()
		{
			return _elementVal;
		}
		
		public int getFireVal()
		{
			return _fireVal;
		}
		
		public int getWaterVal()
		{
			return _waterVal;
		}
		
		public int getWindVal()
		{
			return _windVal;
		}
		
		public int getEarthVal()
		{
			return _earthVal;
		}
		
		public int getHolyVal()
		{
			return _holyVal;
		}
		
		public int getDarkVal()
		{
			return _darkVal;
		}
		
		/**
		 * @param itemId
		 *            The itemId to set.
		 */
		public void setItemId(int itemId)
		{
			_itemId = itemId;
		}
		
		/**
		 * @return Returns the itemId.
		 */
		public int getItemId()
		{
			return _itemId;
		}
		
		/**
		 * @param itemCount
		 *            The itemCount to set.
		 */
		public void setItemCount(long itemCount)
		{
			_itemCount = itemCount;
		}
		
		/**
		 * @return Returns the itemCount.
		 */
		public long getItemCount()
		{
			return _itemCount;
		}
		
		/**
		 * @param itemCount
		 *            The itemCount to set.
		 */
		public void setEnchantmentLevel(int enchantmentLevel)
		{
			_enchantmentLevel = enchantmentLevel;
		}
		
		/**
		 * @return Returns the itemCount.
		 */
		public int getEnchantmentLevel()
		{
			return _enchantmentLevel;
		}
		
		public void setIsTaxIngredient(boolean isTaxIngredient)
		{
			_isTaxIngredient = isTaxIngredient;
		}
		
		public boolean isTaxIngredient()
		{
			return _isTaxIngredient;
		}
		
		public void setMantainIngredient(boolean mantainIngredient)
		{
			_mantainIngredient = mantainIngredient;
		}
		
		public boolean getMantainIngredient()
		{
			return _mantainIngredient;
		}
		
		public void setKeepElement(boolean keepElement)
		{
			_keepElement = keepElement;
		}
		
		public boolean getKeepElement()
		{
			return _keepElement;
		}
		
		public void setElementalPenaltyMul(double elementalPenaltyMul)
		{
			_elementalPenaltyMul = elementalPenaltyMul;
		}
		
		public double getElementalPenaltyMul()
		{
			return _elementalPenaltyMul;
		}
		
		public void setElementalPenalty(int elementalPenalty)
		{
			_elementalPenalty = elementalPenalty;
		}
		
		public int getElementalPenalty()
		{
			return _elementalPenalty;
		}
		
		public void setKeepAug(boolean keepAug)
		{
			_keepAugment = keepAug;
		}
		
		public boolean getKeepAug()
		{
			return _keepAugment;
		}
		
		public void setStaticEnchant(boolean staticEnchant)
		{
			_staticEnchant = staticEnchant;
		}
		
		public boolean getStaticEnchant()
		{
			return _staticEnchant;
		}
	}
	
	public class MultiSellListContainer
	{
		private int		_listId;
		private boolean	_applyTaxes				= false;
		private boolean	_maintainEnchantment	= false;
		private boolean	_allowSell				= false;
		private boolean	_allowGem				= false;
		private double	_allowSellMulti			= (double) 1 / 3;
		
		public final double getAllowSellMulti()
		{
			return _allowSellMulti;
		}
		
		public final void setAllowSellMulti(double allowSellMulti)
		{
			_allowSellMulti = allowSellMulti;
		}
		
		public final boolean isAllowSell()
		{
			return _allowSell;
		}
		
		public final void setAllowSell(boolean allowSell)
		{
			_allowSell = allowSell;
		}
		
		public final boolean isAllowGem()
		{
			return _allowGem;
		}
		
		public final void setAllowGem(boolean allowGem)
		{
			_allowGem = allowGem;
		}
		
		private List<Integer>	_npcIds;
		List<MultiSellEntry>	_entriesC;
		
		public MultiSellListContainer()
		{
			_entriesC = new FastList<MultiSellEntry>();
		}
		
		/**
		 * @param listId
		 *            The listId to set.
		 */
		public void setListId(int listId)
		{
			_listId = listId;
		}
		
		public void setApplyTaxes(boolean applyTaxes)
		{
			_applyTaxes = applyTaxes;
		}
		
		public void setMaintainEnchantment(boolean maintainEnchantment)
		{
			_maintainEnchantment = maintainEnchantment;
		}
		
		public void addNpcId(int objId)
		{
			_npcIds.add(objId);
		}
		
		/**
		 * @return Returns the listId.
		 */
		public int getListId()
		{
			return _listId;
		}
		
		public boolean getApplyTaxes()
		{
			return _applyTaxes;
		}
		
		public boolean getMaintainEnchantment()
		{
			return _maintainEnchantment;
		}
		
		public boolean checkNpcId(int npcId)
		{
			if (_npcIds == null)
			{
				synchronized (this)
				{
					if (_npcIds == null)
						_npcIds = new FastList<Integer>();
				}
				return false;
			}
			return _npcIds.contains(npcId);
		}
		
		public void addEntry(MultiSellEntry e)
		{
			_entriesC.add(e);
		}
		
		public List<MultiSellEntry> getEntries()
		{
			return _entriesC;
		}
	}
	
	private void hashFiles(String dirname, List<File> hash)
	{
		File dir = new File(Config.DATAPACK_ROOT, "data/" + dirname);
		if (!dir.exists())
		{
			_log.config("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}
		File[] files = dir.listFiles();
		for (File f : files)
		{
			if (f.getName().endsWith(".xml"))
				hash.add(f);
		}
	}
	
	private void parse()
	{
		Document doc = null;
		int id = 0;
		List<File> files = new FastList<File>();
		hashFiles("multisell", files);
		for (File f : files)
		{
			try
			{
				id = Integer.parseInt(f.getName().replaceAll(".xml", ""));
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				doc = factory.newDocumentBuilder().parse(f);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error loading file " + f, e);
				continue;
			}
			try
			{
				MultiSellListContainer list = parseDocument(doc);
				list.setListId(id);
				_entries.add(list);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error in file " + f, e);
			}
		}
	}
	
	protected MultiSellListContainer parseDocument(Document doc)
	{
		MultiSellListContainer list = new MultiSellListContainer();
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				Node attribute;
				Node gem;
				attribute = n.getAttributes().getNamedItem("applyTaxes");
				if (attribute == null)
					list.setApplyTaxes(false);
				else
					list.setApplyTaxes(Boolean.parseBoolean(attribute.getNodeValue()));
				attribute = n.getAttributes().getNamedItem("maintainEnchantment");
				if (attribute == null)
					list.setMaintainEnchantment(false);
				else
					list.setMaintainEnchantment(Boolean.parseBoolean(attribute.getNodeValue()));
				attribute = n.getAttributes().getNamedItem("allowSell");
				if (attribute == null)
					list.setAllowSell(false);
				else
				{
					list.setAllowSell(Boolean.parseBoolean(attribute.getNodeValue()));
					attribute = n.getAttributes().getNamedItem("allowSellMulti");
					if (attribute != null)
					{
						list.setAllowSellMulti(Double.parseDouble(attribute.getNodeValue()));
					}
				}
				gem = n.getAttributes().getNamedItem("allowGem");
				if (gem == null)
					list.setAllowGem(false);
				else
				{
					list.setAllowGem(Boolean.parseBoolean(gem.getNodeValue()));
				}
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						MultiSellEntry e = parseEntry(d);
						list.addEntry(e);
					}
				}
			}
			else if ("item".equalsIgnoreCase(n.getNodeName()))
			{
				MultiSellEntry e = parseEntry(n);
				list.addEntry(e);
			}
		}
		return list;
	}
	
	protected MultiSellEntry parseEntry(Node n)
	{
		int entryId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
		Node first = n.getFirstChild();
		MultiSellEntry entry = new MultiSellEntry();
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("ingredient".equalsIgnoreCase(n.getNodeName()))
			{
				Node attribute;
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				int enchantlevel = -1;
				boolean keepElements = false;
				boolean keepAug = false;
				boolean staticEnch = false;
				double elementalPenaltyMul = -1;
				int elementalPenalty = -1;
				long count = Long.parseLong(n.getAttributes().getNamedItem("count").getNodeValue());
				boolean isTaxIngredient = false, mantainIngredient = false;
				attribute = n.getAttributes().getNamedItem("isTaxIngredient");
				if (attribute != null)
					isTaxIngredient = Boolean.parseBoolean(attribute.getNodeValue());
				attribute = n.getAttributes().getNamedItem("mantainIngredient");
				if (attribute != null)
					mantainIngredient = Boolean.parseBoolean(attribute.getNodeValue());
				attribute = n.getAttributes().getNamedItem("enchant");
				if (attribute != null)
					enchantlevel = Integer.parseInt(attribute.getNodeValue());
				attribute = n.getAttributes().getNamedItem("keepElements");
				if (attribute != null)
					keepElements = Boolean.parseBoolean(attribute.getNodeValue());
				attribute = n.getAttributes().getNamedItem("keepAug");
				if (attribute != null)
					keepAug = Boolean.parseBoolean(attribute.getNodeValue());
				attribute = n.getAttributes().getNamedItem("staticEnch");
				if (attribute != null)
					staticEnch = Boolean.parseBoolean(attribute.getNodeValue());
				attribute = n.getAttributes().getNamedItem("elementalPenaltyMul");
				if (attribute != null)
					elementalPenaltyMul = Double.parseDouble(attribute.getNodeValue());
				attribute = n.getAttributes().getNamedItem("elementalPenalty");
				if (attribute != null)
					elementalPenalty = Integer.parseInt(attribute.getNodeValue());
				MultiSellIngredient e;
				if (enchantlevel > 0)
					e = new MultiSellIngredient(id, count, enchantlevel, isTaxIngredient, mantainIngredient, keepElements, keepAug, staticEnch, elementalPenaltyMul, elementalPenalty);
				else
					e = new MultiSellIngredient(id, count, isTaxIngredient, mantainIngredient, keepElements, keepAug, staticEnch, elementalPenaltyMul, elementalPenalty);
				entry.addIngredient(e);
			}
			else if ("production".equalsIgnoreCase(n.getNodeName()))
			{
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				long count = Long.parseLong(n.getAttributes().getNamedItem("count").getNodeValue());
				int enchantlevel = -1;
				boolean staticEnch = false;
				Node attribute = n.getAttributes().getNamedItem("enchant");
				if (attribute != null)
					enchantlevel = Integer.parseInt(attribute.getNodeValue());
				attribute = n.getAttributes().getNamedItem("staticEnch");
				if (attribute != null)
					staticEnch = Boolean.parseBoolean(attribute.getNodeValue());
				MultiSellIngredient e;
				if (enchantlevel > 0)
					e = new MultiSellIngredient(id, count, enchantlevel, false, false, false, false, staticEnch, -1, -1);
				else
					e = new MultiSellIngredient(id, count, false, false, false, false, false, -1, -1);
				entry.addProduct(e);
			}
		}
		entry.setEntryId(entryId);
		return entry;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final L2Multisell _instance = new L2Multisell();
	}
}
