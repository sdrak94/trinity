package net.sf.l2j.gameserver.network.clientpackets;

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.Elementals;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellEntry;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellIngredient;
import net.sf.l2j.gameserver.model.L2Multisell.MultiSellListContainer;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2DonationMerchantInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.templates.item.L2Armor;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.util.Rnd;

public class MultiSellChoose extends L2GameClientPacket
{
	private static final String	_C__A7_MULTISELLCHOOSE	= "[C] A7 MultiSellChoose";
	private static Logger		_log					= Logger.getLogger(MultiSellChoose.class.getName());
	private int					_listId;
	private int					_entryId;
	private long				_amount;
	private int					_enchantment;
	private long				_transactionTax;															// local handling of taxation
	@SuppressWarnings("unused")
	private int					_unk1;
	@SuppressWarnings("unused")
	private int					_unk2;
	@SuppressWarnings("unused")
	private int					_unk3;
	@SuppressWarnings("unused")
	private int					_unk7;
	@SuppressWarnings("unused")
	private int					_unk4;
	@SuppressWarnings("unused")
	private int					_unk5;
	@SuppressWarnings("unused")
	private int					_unk6;
	@SuppressWarnings("unused")
	private int					_unk8;
	@SuppressWarnings("unused")
	private int					_unk9;
	@SuppressWarnings("unused")
	private int					_unk10;
	@SuppressWarnings("unused")
	private int					_unk11;
	
	@Override
	protected void readImpl()
	{
		_listId = readD();
		_entryId = readD();
		_amount = readQ();
		_unk1 = readH();
		_unk2 = readD();
		_unk3 = readD();
		_unk4 = readH(); // elemental attributes
		_unk5 = readH();// elemental attributes
		_unk6 = readH();// elemental attributes
		_unk7 = readH();// elemental attributes
		_unk8 = readH();// elemental attributes
		_unk9 = readH();// elemental attributes
		_unk10 = readH();// elemental attributes
		_unk11 = readH();// elemental attributes
		_enchantment = _entryId % 100000;
		_entryId = _entryId / 100000;
		_transactionTax = 0; // initialize tax amount to 0...
	}
	
	@Override
	public void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		if (!player.getFloodProtectors().getMultiSell().tryPerformAction("multisell choose"))
			return;
		if (_amount < 1 || _amount > 1000)
			return;
		final MultiSellListContainer list = L2Multisell.getInstance().getList(_listId);
		if (list == null)
			return;
		final L2Object target = player.getTarget();
		if (!player.isGM() && !player.isBeta() && (target == null || !(target instanceof L2Npc) || !list.checkNpcId(((L2Npc) target).getNpcId()) || !((L2Npc) target).canInteract(player)))
			return;
		if (player.isAccountLockedDown())
		{
			player.sendMessage("Your account is in lockdown");
			return;
		}
		final boolean allowGem = list.isAllowGem();
		final boolean allowSell = list.isAllowSell();
		final double allowSellMulti = allowSell ? list.getAllowSellMulti() : 1;
		for (MultiSellEntry entry : list.getEntries())
		{
			if (entry.getEntryId() == _entryId)
			{
				doExchange(player, entry, list.getApplyTaxes(), list.getMaintainEnchantment(), _enchantment, allowSell, allowSellMulti, allowGem);
				return;
			}
		}
	}
	
	private void doExchange(L2PcInstance player, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantment, boolean allowSell, double allowSellMulti, boolean allowGem)
	{
		final PcInventory inv = player.getInventory();
		// given the template entry and information about maintaining enchantment and applying taxes
		// re-create the instance of the entry that will be used for this exchange
		// i.e. change the enchantment level of select ingredient/products and adena amount appropriately.
		final L2Npc merchant = (player.getTarget() instanceof L2Npc) ? (L2Npc) player.getTarget() : null;
		if (merchant == null)
			return;
		final MultiSellEntry entry = prepareEntry(merchant, templateEntry, applyTaxes, maintainEnchantment, enchantment);
		boolean swapped = false;
		boolean isShadowItem = false;
		boolean containGlit = false;
		boolean gemmed = false;
		if (_amount == 963)
		{
			if (allowSell)
			{
				if (player.isAccountLockedDown())
				{
					player.sendMessage("Your account is in lockdown");
					return;
				}
				entry.swap();
				swapped = true;
				_amount = 1;
			}
		}
		if (_amount == 962)
		{
			if (allowGem)
			{
				if (player.isAccountLockedDown())
				{
					player.sendMessage("Your account is in lockdown");
					return;
				}
				entry.swap();
				gemmed = true;
				_amount = 1;
			}
		}
		if (!player.isGM() && !player.isBeta())
		{
			int slots = 0;
			for (MultiSellIngredient e : entry.getProducts())
			{
				if (e.getItemId() < 0)
					continue;
				if (e.getItemId() >= 959 && e.getItemId() <= 962)
				{
					if (e.getItemCount() * _amount > PcInventory.MAX_SCROLL)
					{
						player.sendMessage("You cannot buy more than 100 enchant scrolls");
						return;
					}
				}
				L2Item template = ItemTable.getInstance().getTemplate(e.getItemId());
				if (template == null)
					continue;
				if (!template.isStackable())
					slots += e.getItemCount() * _amount;
				else if (player.getInventory().getItemByItemId(e.getItemId()) == null)
					slots++;
			}
			if (!inv.validateCapacity(slots))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
				return;
			}
		}
		// Generate a list of distinct ingredients and counts in order to check if the correct item-counts
		// are possessed by the player
		FastList<MultiSellIngredient> _ingredientsList = new FastList<MultiSellIngredient>();
		boolean newIng = true;
		for (MultiSellIngredient e : entry.getIngredients())
		{
			newIng = true;
			// at this point, the template has already been modified so that enchantments are properly included
			// whenever they need to be applied. Uniqueness of items is thus judged by item id AND enchantment level
			for (MultiSellIngredient ex : _ingredientsList)
			{
				// if the item was already added in the list, merely increment the count
				// this happens if 1 list entry has the same ingredient twice (example 2 swords = 1 dual)
				if ((ex.getItemId() == e.getItemId()) && (ex.getEnchantmentLevel() == e.getEnchantmentLevel()))
				{
					if ((double) ex.getItemCount() + e.getItemCount() > Integer.MAX_VALUE)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
						_ingredientsList.clear();
						_ingredientsList = null;
						return;
					}
					ex.setItemCount(ex.getItemCount() + e.getItemCount());
					newIng = false;
				}
			}
			if (newIng)
			{
				// if it's a new ingredient, just store its info directly (item id, count, enchantment)
				_ingredientsList.add(L2Multisell.getInstance().new MultiSellIngredient(e));
			}
		}
		FastMap<Integer, Integer> _itemList = new FastMap<Integer, Integer>();
		// now check if the player has sufficient items in the inventory to cover the ingredients' expences
		for (MultiSellIngredient e : _ingredientsList)
		{
			if (e.getItemId() != 57 && (double) e.getItemCount() * _amount > Integer.MAX_VALUE)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
				_ingredientsList.clear();
				_ingredientsList = null;
				_itemList.clear();
				_itemList = null;
				return;
			}
			switch (e.getItemId())
			{
				case -200: // Clan Reputation Score
				{
					if (player.getClan() == null)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER));
						_ingredientsList.clear();
						_ingredientsList = null;
						_itemList.clear();
						_itemList = null;
						return;
					}
					if (!player.isClanLeader())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED));
						_ingredientsList.clear();
						_ingredientsList = null;
						_itemList.clear();
						_itemList = null;
						return;
					}
					if (player.getClan().getReputationScore() < e.getItemCount() * _amount)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW));
						_ingredientsList.clear();
						_ingredientsList = null;
						_itemList.clear();
						_itemList = null;
						return;
					}
					break;
				}
				case -300: // Player Fame
				{
					if (player.getFame() < e.getItemCount() * _amount)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_FAME_POINTS));
						_ingredientsList.clear();
						_ingredientsList = null;
						_itemList.clear();
						_itemList = null;
						return;
					}
					break;
				}
				default:
				{
					final long inventoryCount = inv.getInventoryItemCountMultisell(e.getItemId(), e.getEnchantmentLevel()) - (_itemList.containsKey(e.getItemId()) ? _itemList.get(e.getItemId()) : 0);
					// if this is not a list that maintains enchantment, check the count of all items that have the given id // otherwise, check only the count of items with exactly the needed enchantment level
					if (!player.isGM() && !player.isBeta() && inventoryCount < ((Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMantainIngredient()) ? (e.getItemCount() * _amount) : e.getItemCount()))
					{
						player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_REQUIRED_ITEMS));// Update by rocknow
						_ingredientsList.clear();
						_ingredientsList = null;
						_itemList.clear();
						_itemList = null;
						return;
					}
					if (!player.isGM() && !player.isBeta() && inv.getAllItemsByItemIdMultisell(e.getItemId(), e.getEnchantmentLevel())[0].isEnchantable())
					{
						if (_itemList.containsKey(e.getItemId()))
						{
							final int amount = _itemList.get(e.getItemId());
							_itemList.put(e.getItemId(), (int) e.getItemCount() * (int) _amount + amount);
						}
						else
						{
							_itemList.put(e.getItemId(), (int) e.getItemCount() * (int) _amount);
						}
					}
					break;
				}
			}
		}
		_ingredientsList.clear();
		_ingredientsList = null;
		_itemList.clear();
		_itemList = null;
		FastList<L2Augmentation> augmentation = new FastList<L2Augmentation>();
		Elementals elemental = null;
		boolean staticEnch = false;
		/** All ok, remove items and add final product */
		// int enchantlvl = 0;
		boolean update = false;
		boolean usedKamalokaBuco = false;
		for (MultiSellIngredient e : entry.getIngredients())
		{
			switch (e.getItemId())
			{
				case -200: // Clan Reputation Score
				{
					final int repCost = (int) (player.getClan().getReputationScore() - (e.getItemCount() * _amount));
					player.getClan().setReputationScore(repCost, true);
					SystemMessage smsg = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					smsg.addItemNumber(e.getItemCount() * _amount);
					player.sendPacket(smsg);
					break;
				}
				case -300: // Player Fame
				{
					final int newFame = (int) (player.getFame() - (e.getItemCount() * _amount));
					assert (newFame >= 0);
					player.setFame(newFame);
					player.sendPacket(new UserInfo(player));
					player.sendPacket(new ExBrExtraUserInfo(player));
					break;
				}
				default:
				{
					L2ItemInstance itemToTake = inv.getItemByItemId(e.getItemId()); // initialize and initial guess for the item to take.
					if (!player.isGM() && !player.isBeta() && itemToTake == null)
					{ // this is a cheat, transaction will be aborted and if any items already taken will not be returned back to inventory!
						_log.severe("Character: " + player.getName() + " is trying to cheat in multisell, merchatnt id:" + merchant.getNpcId());
						return;
					}
					/*
					 * if (itemToTake.isEquipped())
					 * { //this is a cheat, transaction will be aborted and if any items already taken will not be returned back to inventory!
					 * _log.severe("Character: " + player.getName() + " is trying to cheat in multisell, exchanging equipped item, merchatnt id:" + merchant.getNpcId());
					 * return;
					 * }
					 */
					if (!player.isGM() && !player.isBeta() && itemToTake.isWear())
					{// Player trying to buy something from the Multisell store with an item that's just being used from the Wear option from merchants.
						_log.severe("Character: " + player.getName() + " is trying to cheat in multisell, merchatnt id:" + merchant.getNpcId());
						return;
					}
					if (e.getItemId() == 13002)
						usedKamalokaBuco = true;
					if (Config.MULTISELL_UNTRADEABLE_SOURCE_ITEMS_PVP_SERVER)
					{
						if (e.getItemId() == 97002 || e.getItemId() == 97000 || e.getItemId() == 50024 || e.getItemId() == 4676)
							usedKamalokaBuco = true;
					}
					if (Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMantainIngredient())
					{
						// if it's a stackable item, just reduce the amount from the first (only) instance that is found in the inventory
						if (!player.isGM() && !player.isBeta() && itemToTake.isStackable())
						{
							if (!player.destroyItem("Multisell", itemToTake.getObjectId(), (e.getItemCount() * _amount), player.getTarget(), true))
								return;
						}
						else
						{
							// for non-stackable items, one of two scenaria are possible:
							// a) list maintains enchantment: get the instances that exactly match the requested enchantment level
							// b) list does not maintain enchantment: get the instances with the LOWEST enchantment level
							// a) if enchantment is maintained, then get a list of items that exactly match this enchantment
							if (maintainEnchantment)
							{
								final List<L2ItemInstance> inventoryContents = inv.getAllItemsByItemIdNew(e.getItemId(), true);
								for (int i = 0; i < (e.getItemCount() * _amount); i++)
								{
									int eEnch = itemToTake.getEnchantLevel();
									int ic = inventoryContents.get(i).getEnchantLevel();
									if (ic < eEnch)
									{
										player.sendMessage("Not proper enchantment level");
										return;
									}
									if (inventoryContents.get(i).isAugmented())
									{
										augmentation.add(inventoryContents.get(i).getAugmentation());
									}
									if (inventoryContents.get(i).getElementals() != null)
									{
										elemental = inventoryContents.get(i).getElementals();
									}
									if (!player.destroyItem("Multisell", inventoryContents.get(i).getObjectId(), 1, player.getTarget(), true))
									{
										return;
									}
								}
								// // loop through this list and remove (one by one) each item until the required amount is taken.
								// L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId(), e.getEnchantmentLevel());
								// for (int i = 0; i < (e.getItemCount() * _amount); i++)
								// {
								// if (inventoryContents[i].isAugmented())
								// augmentation.add(inventoryContents[i].getAugmentation());
								// if (inventoryContents[i].getElementals() != null)
								// elemental = inventoryContents[i].getElementals();
								// if (!player.destroyItem("Multisell", inventoryContents[i].getObjectId(), 1, player.getTarget(), true))
								// return;
								// }
							}
							else // b) enchantment is not maintained. Get the instances with the LOWEST enchantment level
							{
								/*
								 * NOTE: There are 2 ways to achieve the above goal.
								 * 1) Get all items that have the correct itemId, loop through them until the lowest enchantment
								 * level is found. Repeat all this for the next item until proper count of items is reached.
								 * 2) Get all items that have the correct itemId, sort them once based on enchantment level,
								 * and get the range of items that is necessary.
								 * Method 1 is faster for a small number of items to be exchanged.
								 * Method 2 is faster for large amounts.
								 * EXPLANATION:
								 * Worst case scenario for algorithm 1 will make it run in a number of cycles given by:
								 * m*(2n-m+1)/2 where m is the number of items to be exchanged and n is the total
								 * number of inventory items that have a matching id.
								 * With algorithm 2 (sort), sorting takes n*log(n) time and the choice is done in a single cycle
								 * for case b (just grab the m first items) or in linear time for case a (find the beginning of items
								 * with correct enchantment, index x, and take all items from x to x+m).
								 * Basically, whenever m > log(n) we have: m*(2n-m+1)/2 = (2nm-m*m+m)/2 >
								 * (2nlogn-logn*logn+logn)/2 = nlog(n) - log(n*n) + log(n) = nlog(n) + log(n/n*n) =
								 * nlog(n) + log(1/n) = nlog(n) - log(n) = (n-1)log(n)
								 * So for m < log(n) then m*(2n-m+1)/2 > (n-1)log(n) and m*(2n-m+1)/2 > nlog(n)
								 * IDEALLY:
								 * In order to best optimize the performance, choose which algorithm to run, based on whether 2^m > n
								 * if ( (2<<(e.getItemCount() * _amount)) < inventoryContents.length )
								 * // do Algorithm 1, no sorting
								 * else
								 * // do Algorithm 2, sorting
								 * CURRENT IMPLEMENTATION:
								 * In general, it is going to be very rare for a person to do a massive exchange of non-stackable items
								 * For this reason, we assume that algorithm 1 will always suffice and we keep things simple.
								 * If, in the future, it becomes necessary that we optimize, the above discussion should make it clear
								 * what optimization exactly is necessary (based on the comments under "IDEALLY").
								 */
								// choice 1. Small number of items exchanged. No sorting.
								if (!player.isGM() && !player.isBeta())
									for (int i = 1; i <= (e.getItemCount() * _amount); i++)
									{
										final L2ItemInstance[] inventoryContents = inv.getAllItemsByItemIdMultisell(e.getItemId(), e.getEnchantmentLevel());
										itemToTake = inventoryContents[0];
										// get item with the LOWEST enchantment level from the inventory... +0 is lowest by default...
										/*
										 * if (swapped)
										 * {
										 */
										if (itemToTake.getRemainingTime() > 0)
										{
											isShadowItem = true;
											player.sendMessage("You can't issue a refund on shadow items");
											return;
										}
										if (e.getKeepElement())
										{
											if (itemToTake.getElementals() != null)
											{
												elemental = itemToTake.getElementals();
											}
										}
//										if (elemental != null && e.getKeepElement() && (e.getElementalPenalty() > 0 || e.getElementalPenaltyMul() > 0))
//										{
//											if (e.getElementalPenalty() > 0)
//											{
//												if (elemental.getValue() - e.getElementalPenalty() > 1)
//												{
//													elemental.setValue(elemental.getValue() - e.getElementalPenalty());
//												}
//												else
//												{
//													elemental = null;
//												}
//											}
//											if (e.getElementalPenaltyMul() > 0)
//											{
//												if (elemental.getValue() * e.getElementalPenaltyMul() > 1)
//												{
//													elemental.setValue((int) (elemental.getValue() * e.getElementalPenaltyMul()));
//												}
//												else
//												{
//													elemental = null;
//												}
//											}
//											if (itemToTake.getElementals() != null)
//											{
//												elemental = itemToTake.getElementals();
//											}
//										}
										if (e.getKeepAug())
										{
											if (itemToTake.isAugmented())
											{
												augmentation.add(itemToTake.getAugmentation());
											}
										}
										if (e.getStaticEnchant())
										{
											staticEnch = true;
										}
										if (itemToTake.getEnchantLevel() > 0)
										{
											for (L2ItemInstance item : inventoryContents)
											{
												if (item.getEnchantLevel() < itemToTake.getEnchantLevel())
												{
													itemToTake = item;
													// nothing will have enchantment less than 0. If a zero-enchanted
													// item is found, just take it
													if (itemToTake.getEnchantLevel() == 0)
														break;
												}
											}
											/*
											 * if (enchantlvl == 0 && itemToTake.isEquipable())
											 * {
											 * if (itemToTake.getEnchantLevel() >= 2)
											 * {
											 * enchantlvl = (int) (itemToTake.getEnchantLevel()/1.5);
											 * assert (enchantlvl >= 1);
											 * }
											 * }
											 */}
										/// }
										if (!update && itemToTake.isEquipped())
											update = true;
										if (!player.destroyItem("Multisell", itemToTake.getObjectId(), 1, player.getTarget(), true))
											return;
									}
							}
						}
					}
					break;
				}
			}
		}
		if (update)
			player.broadcastUserInfo();
		if (!swapped)
			allowSellMulti = 1;
		if (isShadowItem)
		{
			player.sendMessage("You cant issue a refund on shadow items");
			return;
		}
		for (MultiSellIngredient e : entry.getProducts())
		{
			long amount = 0;
			if (isShadowItem)
			{
				player.sendMessage("You cant issue a refund on shadow items");
				break;
			}
			if (allowSellMulti < (double) 1 / 3)
				amount = (long) (e.getItemCount() * _amount * allowSellMulti);
			else
				amount = (long) Math.max(e.getItemCount() * _amount * allowSellMulti, 1);
			if (amount < 1)
				continue;
			if (gemmed)
			{
				if (e.getItemId() == 6393 || e.getItemId() == 4356)
				{
					amount = (long) (e.getItemCount() / 3);
					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
					{
						player.getCounters().gemedItems++;
					}
				}
				if (amount < 1)
					continue;
			}
			int motherluck = 0;
			switch (e.getItemId())
			{
				case -200: // Clan Reputation Score
				{
					if (player.getClan() != null)
						player.getClan().setReputationScore((int) (player.getClan().getReputationScore() + amount), true);
					break;
				}
				case -300: // Player Fame
				{
					player.setFame((int) (player.getFame() + amount));
					player.sendPacket(new UserInfo(player));
					player.sendPacket(new ExBrExtraUserInfo(player));
					break;
				}
				default:
				{
					L2ItemInstance tempItem = ItemTable.getInstance().createDummyItem(e.getItemId());
					if (tempItem == null)
					{
						_log.severe("Problem with multisell ID:" + _listId + " entry ID:" + _entryId + " - Product ID:" + e.getItemId() + " not exist.");
						return;
					}
					if (tempItem.isStackable())
					{
						if (gemmed)
						{
							if (tempItem.getItemId() == 6393 || tempItem.getItemId() == 4356)
							{
								inv.addItem("Multisell", 800100, amount, player, player.getTarget());
							}
							else
								continue;
						}
						else
							inv.addItem("Multisell", e.getItemId(), amount, player, player.getTarget());
					}
					else
					{
						L2ItemInstance product = null;
						for (int i = 0; i < amount; i++)
						{
							product = inv.addItem("Multisell", e.getItemId(), 1, player, player.getTarget());
							if (maintainEnchantment)
							{
								if (i < augmentation.size())
									product.setAugmentation(new L2Augmentation(augmentation.get(i).getAugmentationId(), augmentation.get(i).getSkill()));
								if (elemental != null)
									product.setElementAttr(elemental.getElement(), elemental.getValue());
								product.setEnchantLevel(e.getEnchantmentLevel());
								product.updateDatabase();
							}
							else
							{
								if (elemental != null)
								{
									product.setElementAttr(elemental.getElement(), elemental.getValue());
									product.updateDatabase();
								}
								if (augmentation.size() > 1)
								{
									product.setAugmentation(new L2Augmentation(augmentation.get(i).getAugmentationId(), augmentation.get(i).getSkill()));
									product.updateDatabase();
								}
								if (e.getEnchantmentLevel() >= 1 && product.isEquipable())
								{
									if (!swapped && !(merchant instanceof L2DonationMerchantInstance || merchant.getNpcId() == 90004))
									{
										final int chance = Rnd.get(110);
										if (product.isStandardShopItem())
										{
											if (chance < 1 && product.getUniqueness() < 4.5)
												motherluck = 5;
											else if (chance < 5)
												motherluck = 4;
											else if (chance < 15)
												motherluck = 3;
											else if (chance < 30)
												motherluck = 2;
											else if (chance < 50)
												motherluck = 1;
										}
										else
										{
											if (chance < 1 && product.getUniqueness() < 4.5)
												motherluck = 3;
											else if (chance < 5)
												motherluck = 2;
											else if (chance < 15)
												motherluck = 1;
											/*
											 * else if (chance < 30)
											 * motherluck = 2;
											 * else if (chance < 50)
											 * motherluck = 1;
											 */
										}
										if (motherluck >= 2)
										{
											if (product.getUniqueness() > 3)
												_log.info(player.getName() + " got lucky at " + merchant.getName() + " with purchase of " + product.getName() + " luck:" + motherluck);
										}
									}
									if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
									{
										if (product.isTalisman())
										{
											player.getCounters().talismansBought++;
										}
									}
									if (!staticEnch)
									{
										product.setEnchantLevel(e.getEnchantmentLevel() + motherluck);
									}
									else
									{
										product.setEnchantLevel(e.getEnchantmentLevel());
									}
									// player.sendMessage("Your "+product.getName()+" has been set to an enchant level of "+enchantlvl);
									// enchantlvl = 0;
								}
							}
							if (usedKamalokaBuco)
							{
								if (product.isTradeable())
									player.sendMessage("Your " + product.getName() + " is now untradeable");
								product.setUntradeableTimer(9999999900003L);
							}
						}
					}
					// msg part
					SystemMessage sm;
					if (gemmed)
					{
						if (tempItem.getItemId() == 6393 || tempItem.getItemId() == 4356)
						{
							if (amount > 1)
							{
								sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
								sm.addItemName(800100);
								sm.addItemNumber(amount);
								player.sendPacket(sm);
								sm = null;
							}
							else
							{
								if (maintainEnchantment || e.getEnchantmentLevel() > 0)
								{
									sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
									sm.addItemNumber(e.getEnchantmentLevel() + motherluck);
									sm.addItemName(800100);
								}
								else
								{
									sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
									sm.addItemName(800100);
								}
								player.sendPacket(sm);
								sm = null;
							}
						}
						else
							continue;
					}
					else
					{
						if (amount > 1)
						{
							sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
							sm.addItemName(e.getItemId());
							sm.addItemNumber(amount);
							player.sendPacket(sm);
							sm = null;
						}
						else
						{
							if (maintainEnchantment || e.getEnchantmentLevel() > 0)
							{
								if (!staticEnch)
								{
									sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
									sm.addItemNumber(e.getEnchantmentLevel() + motherluck);
									sm.addItemName(e.getItemId());
								}
								else
								{
									sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
									sm.addItemNumber(e.getEnchantmentLevel());
									sm.addItemName(e.getItemId());
								}
							}
							else
							{
								sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
								sm.addItemName(e.getItemId());
							}
							player.sendPacket(sm);
							sm = null;
						}
					}
				}
			}
		}
		player.sendPacket(new ItemList(player, false));
		// finally, give the tax to the castle...
		if (merchant != null && merchant.getIsInTown() && merchant.getCastle().getOwnerId() > 0)
			merchant.getCastle().addToTreasury(_transactionTax * _amount);
	}
	
	// Regarding taxation, the following appears to be the case:
	// a) The count of aa remains unchanged (taxes do not affect aa directly).
	// b) 5/6 of the amount of aa is taxed by the normal tax rate.
	// c) the resulting taxes are added as normal adena value.
	// d) normal adena are taxed fully.
	// e) Items other than adena and ancient adena are not taxed even when the list is taxable.
	// example: If the template has an item worth 120aa, and the tax is 10%,
	// then from 120aa, take 5/6 so that is 100aa, apply the 10% tax in adena (10a)
	// so the final price will be 120aa and 10a!
	private MultiSellEntry prepareEntry(L2Npc merchant, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel)
	{
		MultiSellEntry newEntry = L2Multisell.getInstance().new MultiSellEntry();
		newEntry.setEntryId(templateEntry.getEntryId());
		long totalAdenaCount = 0;
		boolean hasIngredient = false;
		for (MultiSellIngredient ing : templateEntry.getIngredients())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = L2Multisell.getInstance().new MultiSellIngredient(ing);
			if (newIngredient.getItemId() == 57 && newIngredient.isTaxIngredient())
			{
				double taxRate = 0.0;
				if (applyTaxes)
				{
					if (merchant != null && merchant.getIsInTown())
						taxRate = merchant.getCastle().getTaxRate();
				}
				_transactionTax = Math.round(newIngredient.getItemCount() * taxRate);
				totalAdenaCount += _transactionTax;
				continue; // do not yet add this adena amount to the list as non-taxIngredient adena might be entered later (order not guaranteed)
			}
			else if (ing.getItemId() == 57) // && !ing.isTaxIngredient()
			{
				totalAdenaCount += newIngredient.getItemCount();
				continue; // do not yet add this adena amount to the list as taxIngredient adena might be entered later (order not guaranteed)
			}
			// if it is an armor/weapon, modify the enchantment level appropriately, if necessary
			// not used for clan reputation and fame
			else if (maintainEnchantment && newIngredient.getItemId() > 0)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
					hasIngredient = true;
				}
			}
			// finally, add this ingredient to the entry
			newEntry.addIngredient(newIngredient);
		}
		// Next add the adena amount, if any
		if (totalAdenaCount > 0)
			newEntry.addIngredient(L2Multisell.getInstance().new MultiSellIngredient(57, totalAdenaCount, false, false, false, false, false, -1, -1));
		// Now modify the enchantment level of products, if necessary
		for (MultiSellIngredient ing : templateEntry.getProducts())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = L2Multisell.getInstance().new MultiSellIngredient(ing);
			if (maintainEnchantment && hasIngredient)
			{
				// if it is an armor/weapon, modify the enchantment level appropriately
				// (note, if maintain enchantment is "false" this modification will result to a +0)
				L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
					newIngredient.setEnchantmentLevel(enchantLevel);
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}
	
	@Override
	public String getType()
	{
		return _C__A7_MULTISELLCHOOSE;
	}
}
