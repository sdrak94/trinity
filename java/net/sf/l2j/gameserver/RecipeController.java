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
package net.sf.l2j.gameserver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2ManufactureItem;
import net.sf.l2j.gameserver.model.L2RecipeInstance;
import net.sf.l2j.gameserver.model.L2RecipeList;
import net.sf.l2j.gameserver.model.L2RecipeStatInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.RecipeBookItemList;
import net.sf.l2j.gameserver.network.serverpackets.RecipeItemMakeInfo;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopItemInfo;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public class RecipeController
{
protected static final Logger _log = Logger.getLogger(RecipeController.class.getName());

private Map<Integer, L2RecipeList> _lists;
protected static final Map<L2PcInstance, RecipeItemMaker> _activeMakers = Collections.synchronizedMap(new WeakHashMap<L2PcInstance, RecipeItemMaker>());
private static final String RECIPES_FILE = "recipes.xml";

public static RecipeController getInstance()
{
	return SingletonHolder._instance;
}

private RecipeController()
{
	_lists = new FastMap<Integer, L2RecipeList>();
	
	try
	{
		loadFromXML();
		_log.info("RecipeController: Loaded " + _lists.size() + " recipes.");
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Failed loading recipe list", e);
	}
}

public int getRecipesCount()
{
	return _lists.size();
}

public L2RecipeList getRecipeList(int listId)
{
	return _lists.get(listId);
}

public L2RecipeList getRecipeByItemId(int itemId)
{
	for (L2RecipeList find : _lists.values())
	{
		if (find.getRecipeId() == itemId)
			return find;
	}
	return null;
}

public int[] getAllItemIds()
{
	int[] idList = new int[_lists.size()];
	int i = 0;
	for (L2RecipeList rec : _lists.values())
	{
		idList[i++] = rec.getRecipeId();
	}
	return idList;
}

public synchronized void requestBookOpen(L2PcInstance player, boolean isDwarvenCraft)
{
	RecipeItemMaker maker = null;
	if (Config.ALT_GAME_CREATION)
		maker = _activeMakers.get(player);
	
	if (maker == null)
	{
		RecipeBookItemList response = new RecipeBookItemList(isDwarvenCraft, player.getMaxMp());
		response.addRecipes(isDwarvenCraft ? player.getDwarvenRecipeBook() : player.getCommonRecipeBook());
		player.sendPacket(response);
		return;
	}
	
	player.sendPacket(new SystemMessage(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING));
}

public synchronized void requestMakeItemAbort(L2PcInstance player)
{
	_activeMakers.remove(player); // TODO:  anything else here?
}

public synchronized void requestManufactureItem(L2PcInstance manufacturer, int recipeListId, L2PcInstance player)
{
	L2RecipeList recipeList = getValidRecipeList(player, recipeListId);
	
	if (recipeList == null)
		return;
	
	List<L2RecipeList> dwarfRecipes = Arrays.asList(manufacturer.getDwarvenRecipeBook());
	List<L2RecipeList> commonRecipes = Arrays.asList(manufacturer.getCommonRecipeBook());
	
	if (!dwarfRecipes.contains(recipeList) && !commonRecipes.contains(recipeList))
	{
		Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName()
				+ " sent a false recipe id.", Config.DEFAULT_PUNISH);
		return;
	}
	
	RecipeItemMaker maker;
	
	if (Config.ALT_GAME_CREATION && (maker = _activeMakers.get(manufacturer)) != null) // check if busy
	{
		player.sendMessage("Manufacturer is busy, please try later.");
		return;
	}
	
	maker = new RecipeItemMaker(manufacturer, recipeList, player);
	if (maker._isValid)
	{
		if (Config.ALT_GAME_CREATION)
		{
			_activeMakers.put(manufacturer, maker);
			ThreadPoolManager.getInstance().scheduleGeneral(maker, 100);
		}
		else
			maker.run();
	}
}

public synchronized void requestMakeItem(L2PcInstance player, int recipeListId)
{
	if (player.isInDuel())
	{
		player.sendPacket(new SystemMessage(SystemMessageId.CANT_CRAFT_DURING_COMBAT));
		return;
	}
	
	L2RecipeList recipeList = getValidRecipeList(player, recipeListId);
	
	if (recipeList == null)
		return;
	
	List<L2RecipeList> dwarfRecipes = Arrays.asList(player.getDwarvenRecipeBook());
	List<L2RecipeList> commonRecipes = Arrays.asList(player.getCommonRecipeBook());
	
	if (!dwarfRecipes.contains(recipeList) && !commonRecipes.contains(recipeList))
	{
		Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName()
				+ " sent a false recipe id.", Config.DEFAULT_PUNISH);
		return;
	}
	
	RecipeItemMaker maker;
	
	// check if already busy (possible in alt mode only)
	if (Config.ALT_GAME_CREATION && ((maker = _activeMakers.get(player)) != null))
	{
		SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1);
		sm.addItemName(recipeList.getItemId());
		sm.addString("You are busy creating");
		player.sendPacket(sm);
		return;
	}
	
	maker = new RecipeItemMaker(player, recipeList, player);
	if (maker._isValid)
	{
		if (Config.ALT_GAME_CREATION)
		{
			_activeMakers.put(player, maker);
			ThreadPoolManager.getInstance().scheduleGeneral(maker, 100);
		}
		else
			maker.run();
	}
}

private void loadFromXML() throws SAXException, IOException, ParserConfigurationException
{
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	factory.setValidating(false);
	factory.setIgnoringComments(true);
	File file = new File(Config.DATAPACK_ROOT + "/data/" + RECIPES_FILE);
	if (file.exists())
	{
		Document doc = factory.newDocumentBuilder().parse(file);
		List<L2RecipeInstance> recipePartList = new FastList<L2RecipeInstance>();
		List<L2RecipeStatInstance> recipeStatUseList = new FastList<L2RecipeStatInstance>();
		List<L2RecipeStatInstance> recipeAltStatChangeList = new FastList<L2RecipeStatInstance>();
		
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				recipesFile: for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						recipePartList.clear();
						recipeStatUseList.clear();
						recipeAltStatChangeList.clear();
						NamedNodeMap attrs = d.getAttributes();
						Node att;
						int id = -1;
						boolean haveRare = false;
						StatsSet set = new StatsSet();
						
						att = attrs.getNamedItem("id");
						if (att == null)
						{
							_log.severe("Missing id for recipe item, skipping");
							continue;
						}
						id = Integer.parseInt(att.getNodeValue());
						set.set("id", id);
						
						att = attrs.getNamedItem("recipeId");
						if (att == null)
						{
							_log.severe("Missing recipeId for recipe item id: " + id + ", skipping");
							continue;
						}
						set.set("recipeId", Integer.parseInt(att.getNodeValue()));
						
						att = attrs.getNamedItem("name");
						if (att == null)
						{
							_log.severe("Missing name for recipe item id: " + id + ", skipping");
							continue;
						}
						set.set("recipeName", att.getNodeValue());
						
						att = attrs.getNamedItem("craftLevel");
						if (att == null)
						{
							_log.severe("Missing level for recipe item id: " + id + ", skipping");
							continue;
						}
						set.set("craftLevel", Integer.parseInt(att.getNodeValue()));
						
						att = attrs.getNamedItem("type");
						if (att == null)
						{
							_log.severe("Missing type for recipe item id: " + id + ", skipping");
							continue;
						}
						set.set("isDwarvenRecipe", att.getNodeValue().equalsIgnoreCase("dwarven"));
						
						att = attrs.getNamedItem("successRate");
						if (att == null)
						{
							_log.severe("Missing successRate for recipe item id: " + id + ", skipping");
							continue;
						}
						set.set("successRate", Integer.parseInt(att.getNodeValue()));
						
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("statUse".equalsIgnoreCase(c.getNodeName()))
							{
								String statName = c.getAttributes().getNamedItem("name").getNodeValue();
								int value = Integer.parseInt(c.getAttributes().getNamedItem("value").getNodeValue());
								try
								{
									recipeStatUseList.add(new L2RecipeStatInstance(statName, value));
								}
								catch (Exception e)
								{
									_log.severe("Error in StatUse parameter for recipe item id: " + id + ", skipping");
									continue recipesFile;
								}
							}
							else if ("altStatChange".equalsIgnoreCase(c.getNodeName()))
							{
								String statName = c.getAttributes().getNamedItem("name").getNodeValue();
								int value = Integer.parseInt(c.getAttributes().getNamedItem("value").getNodeValue());
								try
								{
									recipeAltStatChangeList.add(new L2RecipeStatInstance(statName, value));
								}
								catch (Exception e)
								{
									_log.severe("Error in AltStatChange parameter for recipe item id: " + id + ", skipping");
									continue recipesFile;
								}
							}
							else if ("ingredient".equalsIgnoreCase(c.getNodeName()))
							{
								int ingId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
								int ingCount = Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue());
								recipePartList.add(new L2RecipeInstance(ingId, ingCount));
							}
							else if ("production".equalsIgnoreCase(c.getNodeName()))
							{
								set.set("itemId", Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue()));
								set.set("count", Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue()));
							}
							else if ("productionRare".equalsIgnoreCase(c.getNodeName()))
							{
								set.set("rareItemId", Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue()));
								set.set("rareCount", Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue()));
								set.set("rarity", Integer.parseInt(c.getAttributes().getNamedItem("rarity").getNodeValue()));
								haveRare = true;
							}
						}
						
						L2RecipeList recipeList = new L2RecipeList(set, haveRare);
						for (L2RecipeInstance recipePart : recipePartList)
						{
							recipeList.addRecipe(recipePart);
						}
						for (L2RecipeStatInstance recipeStatUse : recipeStatUseList)
						{
							recipeList.addStatUse(recipeStatUse);
						}
						for (L2RecipeStatInstance recipeAltStatChange : recipeAltStatChangeList)
						{
							recipeList.addAltStatChange(recipeAltStatChange);
						}
						
						_lists.put(id, recipeList);
					}
				}
			}
		}
	}
	else
	{
		_log.severe("Recipes file (" + file.getAbsolutePath() + ") doesnt exists.");
	}
}

private class RecipeItemMaker implements Runnable
{
protected boolean _isValid;
protected List<TempItem> _items = null;
protected final L2RecipeList _recipeList;
protected final L2PcInstance _player; // "crafter"
protected final L2PcInstance _target; // "customer"
protected final L2Skill _skill;
protected final int _skillId;
protected final int _skillLevel;
protected int _creationPasses = 1;
protected int _itemGrab;
protected int _exp = -1;
protected int _sp = -1;
protected long _price;
protected int _totalItems;
@SuppressWarnings("unused")
protected int _materialsRefPrice;
protected int _delay;

public RecipeItemMaker(L2PcInstance pPlayer, L2RecipeList pRecipeList, L2PcInstance pTarget)
{
	_player = pPlayer;
	_target = pTarget;
	_recipeList = pRecipeList;
	
	_isValid = false;
	_skillId = _recipeList.isDwarvenRecipe() ? L2Skill.SKILL_CREATE_DWARVEN : L2Skill.SKILL_CREATE_COMMON;
	_skillLevel = _player.getSkillLevel(_skillId);
	_skill = _player.getKnownSkill(_skillId);
	
	_player.isInCraftMode(true);
	
	if (_player.isAlikeDead())
	{
		_player.sendPacket(ActionFailed.STATIC_PACKET);
		abort();
		return;
	}
	
	if (_target.isAlikeDead())
	{
		_target.sendPacket(ActionFailed.STATIC_PACKET);
		abort();
		return;
	}
	
	if (_target.isProcessingTransaction())
	{
		_target.sendPacket(ActionFailed.STATIC_PACKET);
		abort();
		return;
	}
	
	if (_player.isProcessingTransaction())
	{
		_player.sendPacket(ActionFailed.STATIC_PACKET);
		abort();
		return;
	}
	
	// validate recipe list
	if (_recipeList.getRecipes().length == 0)
	{
		_player.sendPacket(ActionFailed.STATIC_PACKET);
		abort();
		return;
	}
	
	// validate skill level
	if (_recipeList.getLevel() > _skillLevel)
	{
		_player.sendPacket(ActionFailed.STATIC_PACKET);
		abort();
		return;
	}
	
	// check that customer can afford to pay for creation services
	if (_player != _target)
	{
		for (L2ManufactureItem temp : _player.getCreateList().getList())
		{
			if (temp.getRecipeId() == _recipeList.getId()) // find recipe for item we want manufactured
			{
				_price = temp.getCost();
				if (_target.getAdena() < _price) // check price
				{
					_target.sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
					abort();
					return;
				}
				break;
			}
		}
	}
	
	// make temporary items
	if ((_items = listItems(false)) == null)
	{
		abort();
		return;
	}
	
	// calculate reference price
	for (TempItem i : _items)
	{
		_materialsRefPrice += i.getReferencePrice() * i.getQuantity();
		_totalItems += i.getQuantity();
	}
	
	// initial statUse checks
	if (!calculateStatUse(false, false))
	{
		abort();
		return;
	}
	
	// initial AltStatChange checks
	if (Config.ALT_GAME_CREATION)
		calculateAltStatChange();
	
	updateMakeInfo(true);
	updateCurMp();
	updateCurLoad();
	
	_player.isInCraftMode(false);
	_isValid = true;
}

public void run()
{
	if (!Config.IS_CRAFTING_ENABLED)
	{
		_target.sendMessage("Item creation is currently disabled.");
		abort();
		return;
	}
	
	if (_player == null || _target == null)
	{
		_log.warning("player or target == null (disconnected?), aborting" + _target + _player);
		abort();
		return;
	}
	
	if (_player.isOnline() == 0 || _target.isOnline() == 0)
	{
		_log.warning("player or target is not online, aborting " + _target + _player);
		abort();
		return;
	}
	
	if (Config.ALT_GAME_CREATION && _activeMakers.get(_player) == null)
	{
		if (_target != _player)
		{
			_target.sendMessage("Manufacture aborted");
			_player.sendMessage("Manufacture aborted");
		}
		else
		{
			_player.sendMessage("Item creation aborted");
		}
		
		abort();
		return;
	}
	
	if (Config.ALT_GAME_CREATION && !_items.isEmpty())
	{
		
		if (!calculateStatUse(true, true))
			return; // check stat use
		updateCurMp(); // update craft window mp bar
		
		grabSomeItems(); // grab (equip) some more items with a nice msg to player
		
		// if still not empty, schedule another pass
		if (!_items.isEmpty())
		{
			// divided by RATE_CONSUMABLES_COST to remove craft time increase on higher consumables rates
			_delay = (int) (Config.ALT_GAME_CREATION_SPEED * _player.getMReuseRate(_skill) * GameTimeController.TICKS_PER_SECOND / Config.RATE_CONSUMABLE_COST)
			* GameTimeController.MILLIS_IN_TICK;
			
			// FIXME: please fix this packet to show crafting animation (somebody)
			MagicSkillUse msk = new MagicSkillUse(_player, _skillId, _skillLevel, _delay, 0);
			_player.broadcastPacket(msk);
			
			_player.sendPacket(new SetupGauge(0, _delay));
			ThreadPoolManager.getInstance().scheduleGeneral(this, 100 + _delay);
		}
		else
		{
			// for alt mode, sleep delay msec before finishing
			_player.sendPacket(new SetupGauge(0, _delay));
			
			try
			{
				Thread.sleep(_delay);
			}
			catch (InterruptedException e)
			{
			}
			finally
			{
				finishCrafting();
			}
		}
	} // for old craft mode just finish
	else
		finishCrafting();
}

private void finishCrafting()
{
	if (!Config.ALT_GAME_CREATION)
		calculateStatUse(false, true);
	
	// first take adena for manufacture
	if ((_target != _player) && _price > 0) // customer must pay for services
	{
		// attempt to pay for item
		L2ItemInstance adenatransfer = _target.transferItem("PayManufacture", _target.getInventory().getAdenaInstance().getObjectId(), _price, _player.getInventory(), _player);
		
		if (adenatransfer == null)
		{
			_target.sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			abort();
			return;
		}
	}
	
	if ((_items = listItems(true)) == null) // this line actually takes materials from inventory
	{ // handle possible cheaters here
		// (they click craft then try to get rid of items in order to get free craft)
	}
	else if (Rnd.get(100) < _recipeList.getSuccessRate())
	{
		rewardPlayer(); // and immediately puts created item in its place
		updateMakeInfo(true);
	}
	else
	{
		if (_target != _player)
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.CREATION_OF_S2_FOR_C1_AT_S3_ADENA_FAILED);
			msg.addString(_target.getName());
			msg.addItemName(_recipeList.getItemId());
			msg.addItemNumber(_price);
			_player.sendPacket(msg);
			
			msg = new SystemMessage(SystemMessageId.C1_FAILED_TO_CREATE_S2_FOR_S3_ADENA);
			msg.addString(_player.getName());
			msg.addItemName(_recipeList.getItemId());
			msg.addItemNumber(_price);
			_target.sendPacket(msg);
		}
		else
			_target.sendPacket(new SystemMessage(SystemMessageId.ITEM_MIXING_FAILED));
		updateMakeInfo(false);
		
		if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
		{
			_target.getCounters().recipesFailed++;
		}
	}
	// update load and mana bar of craft window
	updateCurMp();
	updateCurLoad();
	_activeMakers.remove(_player);
	_player.isInCraftMode(false);
	_target.sendPacket(new ItemList(_target, false));
}

private void updateMakeInfo(boolean success)
{
	if (_target == _player)
		_target.sendPacket(new RecipeItemMakeInfo(_recipeList.getId(), _target, success));
	else
		_target.sendPacket(new RecipeShopItemInfo(_player.getObjectId(), _recipeList.getId()));
}

private void updateCurLoad()
{
	StatusUpdate su = new StatusUpdate(_target.getObjectId());
	su.addAttribute(StatusUpdate.CUR_LOAD, _target.getCurrentLoad());
	_target.sendPacket(su);
}

private void updateCurMp()
{
	StatusUpdate su = new StatusUpdate(_target.getObjectId());
	su.addAttribute(StatusUpdate.CUR_MP, (int) _target.getCurrentMp());
	_target.sendPacket(su);
}

private void grabSomeItems()
{
	int grabItems = _itemGrab;
	while (grabItems > 0 && !_items.isEmpty())
	{
		TempItem item = _items.get(0);
		
		int count = item.getQuantity();
		if (count >= grabItems)
			count = grabItems;
		
		item.setQuantity(item.getQuantity() - count);
		if (item.getQuantity() <= 0)
			_items.remove(0);
		else
			_items.set(0, item);
		
		grabItems -= count;
		
		if (_target == _player)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2_EQUIPPED); // you equipped ...
			sm.addItemNumber(count);
			sm.addItemName(item.getItemId());
			_player.sendPacket(sm);
		}
		else
			_target.sendMessage("Manufacturer " + _player.getName() + " used " + count + " " + item.getItemName());
	}
}

// AltStatChange parameters make their effect here
private void calculateAltStatChange()
{
	_itemGrab = _skillLevel;
	
	for (L2RecipeStatInstance altStatChange : _recipeList.getAltStatChange())
	{
		if (altStatChange.getType() == L2RecipeStatInstance.statType.XP)
		{
			_exp = altStatChange.getValue();
		}
		else if (altStatChange.getType() == L2RecipeStatInstance.statType.SP)
		{
			_sp = altStatChange.getValue();
		}
		else if (altStatChange.getType() == L2RecipeStatInstance.statType.GIM)
		{
			_itemGrab *= altStatChange.getValue();
		}
	}
	// determine number of creation passes needed
	_creationPasses = (_totalItems / _itemGrab) + ((_totalItems % _itemGrab) != 0 ? 1 : 0);
	if (_creationPasses < 1)
		_creationPasses = 1;
}

// StatUse
private boolean calculateStatUse(boolean isWait, boolean isReduce)
{
	boolean ret = true;
	for (L2RecipeStatInstance statUse : _recipeList.getStatUse())
	{
		double modifiedValue = statUse.getValue() / _creationPasses;
		if (statUse.getType() == L2RecipeStatInstance.statType.HP)
		{
			// we do not want to kill the player, so its CurrentHP must be greater than the reduce value
			if (_player.getCurrentHp() <= modifiedValue)
			{
				// rest (wait for HP)
				if (Config.ALT_GAME_CREATION && isWait)
				{
					_player.sendPacket(new SetupGauge(0, _delay));
					ThreadPoolManager.getInstance().scheduleGeneral(this, 100 + _delay);
				}
				else
					// no rest - report no hp
				{
					_target.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_HP));
					abort();
				}
				ret = false;
			}
			else if (isReduce)
				_player.reduceCurrentHp(modifiedValue, _player, _skill);
		}
		else if (statUse.getType() == L2RecipeStatInstance.statType.MP)
		{
			if (_player.getCurrentMp() < modifiedValue)
			{
				// rest (wait for MP)
				if (Config.ALT_GAME_CREATION && isWait)
				{
					_player.sendPacket(new SetupGauge(0, _delay));
					ThreadPoolManager.getInstance().scheduleGeneral(this, 100 + _delay);
				}
				else
					// no rest - report no mana
				{
					_target.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
					abort();
				}
				ret = false;
			}
			else if (isReduce)
				_player.reduceCurrentMp(modifiedValue);
		}
		else
		{
			// there is an unknown StatUse value
			_target.sendMessage("Recipe error!!!, please tell this to your GM.");
			ret = false;
			abort();
		}
	}
	return ret;
}

private List<TempItem> listItems(boolean remove)
{
	L2RecipeInstance[] recipes = _recipeList.getRecipes();
	Inventory inv = _target.getInventory();
	List<TempItem> materials = new FastList<TempItem>();
	SystemMessage sm;
	
	for (L2RecipeInstance recipe : recipes)
	{
		int quantity = _recipeList.isConsumable() ? (int) (recipe.getQuantity() * Config.RATE_CONSUMABLE_COST) : recipe.getQuantity();
		
		if (quantity > 0)
		{
			L2ItemInstance item = inv.getItemByItemId(recipe.getItemId());
			long itemQuantityAmount = item == null ? 0 : item.getCount();
			
			// check materials
			if (itemQuantityAmount < quantity)
			{
				sm = new SystemMessage(SystemMessageId.MISSING_S2_S1_TO_CREATE);
				sm.addItemName(recipe.getItemId());
				sm.addItemNumber(quantity - itemQuantityAmount);
				_target.sendPacket(sm);
				
				abort();
				return null;
			}
			
			// make new temporary object, just for counting purposes
			
			TempItem temp = new TempItem(item, quantity);
			materials.add(temp);
		}
	}
	
	if (remove)
	{
		for (TempItem tmp : materials)
		{
			inv.destroyItemByItemId("Manufacture", tmp.getItemId(), tmp.getQuantity(), _target, _player);
			sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			sm.addItemName(tmp.getItemId());
			sm.addItemNumber(tmp.getQuantity());
			_target.sendPacket(sm);
		}
	}
	return materials;
}

private void abort()
{
	updateMakeInfo(false);
	_player.isInCraftMode(false);
	_activeMakers.remove(_player);
}

/**
 * FIXME: This class should be in some other file, but I don't know where
 *
 * Class explanation:
 * For item counting or checking purposes. When you don't want to modify inventory
 * class contains itemId, quantity, ownerId, referencePrice, but not objectId
 */
private class TempItem
{ // no object id stored, this will be only "list" of items with it's owner
private int _itemId;
private int _quantity;
private int _ownerId;
private int _referencePrice;
private String _itemName;

/**
 * @param item
 * @param quantity of that item
 */
public TempItem(L2ItemInstance item, int quantity)
{
	super();
	_itemId = item.getItemId();
	_quantity = quantity;
	_ownerId = item.getOwnerId();
	_itemName = item.getItem().getName();
	_referencePrice = item.getReferencePrice();
}

/**
 * @return Returns the quantity.
 */
public int getQuantity()
{
	return _quantity;
}

/**
 * @param quantity The quantity to set.
 */
public void setQuantity(int quantity)
{
	_quantity = quantity;
}

public int getReferencePrice()
{
	return _referencePrice;
}

/**
 * @return Returns the itemId.
 */
public int getItemId()
{
	return _itemId;
}

/**
 * @return Returns the ownerId.
 */
@SuppressWarnings("unused")
public int getOwnerId()
{
	return _ownerId;
}

/**
 * @return Returns the itemName.
 */
public String getItemName()
{
	return _itemName;
}
}

private void rewardPlayer()
{
	int rareProdId = _recipeList.getRareItemId();
	int itemId = _recipeList.getItemId();
	int itemCount = _recipeList.getCount();
	L2Item template = ItemTable.getInstance().getTemplate(itemId);
	
	// check that the current recipe has a rare production or not
	if (rareProdId != -1 && (rareProdId == itemId || Config.CRAFT_MASTERWORK))
	{
		if (Rnd.get(100) < _recipeList.getRarity())
		{
			itemId = rareProdId;
			itemCount = _recipeList.getRareCount();
		}
	}
	
	_target.getInventory().addItem("Manufacture", itemId, itemCount, _target, _player);
	
	// inform customer of earned item
	SystemMessage sm = null;
	if (_target != _player)
	{
		// inform manufacturer of earned profit
		if (itemCount == 1)
		{
			sm = new SystemMessage(SystemMessageId.S2_CREATED_FOR_C1_FOR_S3_ADENA);
			sm.addCharName(_target);
			sm.addItemName(itemId);
			sm.addItemNumber(_price);
			_player.sendPacket(sm);
			
			sm = new SystemMessage(SystemMessageId.C1_CREATED_S2_FOR_S3_ADENA);
			sm.addCharName(_player);
			sm.addItemName(itemId);
			sm.addItemNumber(_price);
			_target.sendPacket(sm);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.S2_S3_S_CREATED_FOR_C1_FOR_S4_ADENA);
			sm.addCharName(_target);
			sm.addNumber(itemCount);
			sm.addItemName(itemId);
			sm.addItemNumber(_price);
			_player.sendPacket(sm);
			
			sm = new SystemMessage(SystemMessageId.C1_CREATED_S2_S3_S_FOR_S4_ADENA);
			sm.addCharName(_player);
			sm.addNumber(itemCount);
			sm.addItemName(itemId);
			sm.addItemNumber(_price);
			_target.sendPacket(sm);
		}
	}
	
	if (itemCount > 1)
	{
		sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
		sm.addItemName(itemId);
		sm.addItemNumber(itemCount);
		_target.sendPacket(sm);
	}
	else
	{
		sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
		sm.addItemName(itemId);
		_target.sendPacket(sm);
	}
	
	if (Config.ALT_GAME_CREATION)
	{
		int recipeLevel = _recipeList.getLevel();
		if (_exp < 0)
		{
			_exp = template.getReferencePrice() * itemCount;
			_exp /= recipeLevel;
		}
		if (_sp < 0)
			_sp = _exp / 10;
		if (itemId == rareProdId)
		{
			_exp *= Config.ALT_GAME_CREATION_RARE_XPSP_RATE;
			_sp *= Config.ALT_GAME_CREATION_RARE_XPSP_RATE;
		}
		
		// one variation
		
		// exp -= materialsRefPrice;   // mat. ref. price is not accurate so other method is better
		
		if (_exp < 0)
			_exp = 0;
		if (_sp < 0)
			_sp = 0;
		
		for (int i = _skillLevel; i > recipeLevel; i--)
		{
			_exp /= 4;
			_sp /= 4;
		}
		
		// Added multiplication of Creation speed with XP/SP gain
		// slower crafting -> more XP,  faster crafting -> less XP
		// you can use ALT_GAME_CREATION_XP_RATE/SP to
		// modify XP/SP gained (default = 1)
		
		_player.addExpAndSp((int) _player.calcStat(Stats.EXPSP_RATE, _exp * Config.ALT_GAME_CREATION_XP_RATE
				* Config.ALT_GAME_CREATION_SPEED, null, null), (int) _player.calcStat(Stats.EXPSP_RATE, _sp
						* Config.ALT_GAME_CREATION_SP_RATE * Config.ALT_GAME_CREATION_SPEED, null, null));
	}
	updateMakeInfo(true); // success
	
	if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
	{
		if (itemId == rareProdId)
		{
			_target.getCounters().foundationItemsMade++;
		}
		
		if (_recipeList.isDwarvenRecipe() && (_recipeList.getSuccessRate() < 100))
		{
			_target.getCounters().recipesSucceeded++;
		}
	}
}
}

private L2RecipeList getValidRecipeList(L2PcInstance player, int id)
{
	L2RecipeList recipeList = getRecipeList(id);
	
	if ((recipeList == null) || (recipeList.getRecipes().length == 0))
	{
		player.sendMessage("No recipe for: " + id);
		player.isInCraftMode(false);
		return null;
	}
	return recipeList;
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final RecipeController _instance = new RecipeController();
}
}
