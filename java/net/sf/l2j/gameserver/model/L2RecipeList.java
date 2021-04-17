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

import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * This class describes a Recipe used by Dwarf to craft Item.
 * All L2RecipeList are made of L2RecipeInstance (1 line of the recipe : Item-Quantity needed).<BR><BR>
 *
 */
public class L2RecipeList
{
	/** The table containing all L2RecipeInstance (1 line of the recipe : Item-Quantity needed) of the L2RecipeList */
	private L2RecipeInstance[] _recipes;
	
	/** The table containing all L2RecipeStatInstance for the statUse parameter of the L2RecipeList */
	private L2RecipeStatInstance[] _statUse;
	
	/** The table containing all L2RecipeStatInstance for the altStatChange parameter of the L2RecipeList */
	private L2RecipeStatInstance[] _altStatChange;
	
	/** The Identifier of the Instance */
	private int _id;
	
	/** The crafting level needed to use this L2RecipeList */
	private int _level;

	/** The Identifier of the L2RecipeList */
	private int _recipeId;

	/** The name of the L2RecipeList */
	private String _recipeName;

	/** The crafting success rate when using the L2RecipeList */
	private int _successRate;

	/** The Identifier of the Item crafted with this L2RecipeList */
	private int _itemId;

	/** The quantity of Item crafted when using this L2RecipeList */
	private int _count;

	/** The Identifier of the Rare Item crafted with this L2RecipeList */
	private int _rareItemId;

	/** The quantity of Rare Item crafted when using this L2RecipeList */
	private int _rareCount;

	/** The chance of Rare Item crafted when using this L2RecipeList */
	private int _rarity;

	/** If this a common or a dwarven recipe */
	private boolean _isDwarvenRecipe;

	/**
	 * Constructor of L2RecipeList (create a new Recipe).<BR><BR>
	 */
	public L2RecipeList(StatsSet set, boolean haveRare)
	{
		_recipes = new L2RecipeInstance[0];
		_statUse = new L2RecipeStatInstance[0];
		_altStatChange = new L2RecipeStatInstance[0];
		_id = set.getInteger("id");
		_level = set.getInteger("craftLevel");
		_recipeId = set.getInteger("recipeId");
		_recipeName = set.getString("recipeName");
		_successRate = set.getInteger("successRate");
		_itemId = set.getInteger("itemId");
		_count = set.getInteger("count");
		if (haveRare)
		{
			_rareItemId = set.getInteger("rareItemId");
			_rareCount = set.getInteger("rareCount");
			_rarity = set.getInteger("rarity");			
		}
		_isDwarvenRecipe = set.getBool("isDwarvenRecipe");
	}
	
	/**
	 * Add a L2RecipeInstance to the L2RecipeList (add a line Item-Quantity needed to the Recipe).<BR><BR>
	 */
	public void addRecipe(L2RecipeInstance recipe)
	{
		int len = _recipes.length;
		L2RecipeInstance[] tmp = new L2RecipeInstance[len+1];
		System.arraycopy(_recipes, 0, tmp, 0, len);
		tmp[len] = recipe;
		_recipes = tmp;
	}
	
	/**
	 * Add a L2RecipeStatInstance of the statUse parameter to the L2RecipeList.<BR><BR>
	 */
	public void addStatUse(L2RecipeStatInstance statUse)
	{
		int len = _statUse.length;
		L2RecipeStatInstance[] tmp = new L2RecipeStatInstance[len+1];
		System.arraycopy(_statUse, 0, tmp, 0, len);
		tmp[len] = statUse;
		_statUse = tmp;
	}
	
	/**
	 * Add a L2RecipeStatInstance of the altStatChange parameter to the L2RecipeList.<BR><BR>
	 */
	public void addAltStatChange(L2RecipeStatInstance statChange)
	{
		int len = _altStatChange.length;
		L2RecipeStatInstance[] tmp = new L2RecipeStatInstance[len+1];
		System.arraycopy(_altStatChange, 0, tmp, 0, len);
		tmp[len] = statChange;
		_altStatChange = tmp;
	}
	
	/**
	 * Return the Identifier of the Instance.<BR><BR>
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Return the crafting level needed to use this L2RecipeList.<BR><BR>
	 */
	public int getLevel()
	{
		return _level;
	}

	/**
	 * Return the Identifier of the L2RecipeList.<BR><BR>
	 */
	public int getRecipeId()
	{
		return _recipeId;
	}

	/**
	 * Return the name of the L2RecipeList.<BR><BR>
	 */
	public String getRecipeName()
	{
		return _recipeName;
	}

	/**
	 * Return the crafting success rate when using the L2RecipeList.<BR><BR>
	 */
	public int getSuccessRate()
	{
		return _successRate;
	}

	/**
	 * Return rue if the Item crafted with this L2RecipeList is consumable (shot, arrow,...).<BR><BR>
	 */
	public boolean isConsumable()
	{
		return ((_itemId >= 1463 && _itemId <= 1467) // Soulshots
				|| (_itemId >= 2509 && _itemId <= 2514) // Spiritshots
				|| (_itemId >= 3947 && _itemId <= 3952) // Blessed Spiritshots
				|| (_itemId >= 1341 && _itemId <= 1345) // Arrows
		);
	}

	/**
	 * Return the Identifier of the Item crafted with this L2RecipeList.<BR><BR>
	 */
	public int getItemId()
	{
		return _itemId;
	}

	/**
	 * Return the quantity of Item crafted when using this L2RecipeList.<BR><BR>
	 */
	public int getCount()
	{
		return _count;
	}

	/**
	 * Return the Identifier of the Rare Item crafted with this L2RecipeList.<BR><BR>
	 */
	public int getRareItemId()
	{
		return _rareItemId;
	}

	/**
	 * Return the quantity of Rare Item crafted when using this L2RecipeList.<BR><BR>
	 */
	public int getRareCount()
	{
		return _rareCount;
	}

	/**
	 * Return the chance of Rare Item crafted when using this L2RecipeList.<BR><BR>
	 */
	public int getRarity()
	{
		return _rarity;
	}

	/**
	 * Return <B>true</B> if this a Dwarven recipe or <B>false</B> if its a Common recipe
	 */
	public boolean isDwarvenRecipe()
	{
		return _isDwarvenRecipe;
	}

	/**
	 * Return the table containing all L2RecipeInstance (1 line of the recipe : Item-Quantity needed) of the L2RecipeList.<BR><BR>
	 */
	public L2RecipeInstance[] getRecipes()
	{
		return _recipes;
	}

	/**
	 * Return the table containing all L2RecipeStatInstance of the statUse parameter of the L2RecipeList.<BR><BR>
	 */
	public L2RecipeStatInstance[] getStatUse()
	{
		return _statUse;
	}

	/**
	 * Return the table containing all L2RecipeStatInstance of the AltStatChange parameter of the L2RecipeList.<BR><BR>
	 */
	public L2RecipeStatInstance[] getAltStatChange()
	{
		return _altStatChange;
	}
}

