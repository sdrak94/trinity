package net.sf.l2j.gameserver.util;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public final class FloodProtectors
{
/**
 * Use-item flood protector.
 */
private final FloodProtectorAction _useItem;
/**
 * Roll-dice flood protector.
 */
private final FloodProtectorAction _rollDice;
/**
 * Firework flood protector.
 */
private final FloodProtectorAction _firework;
/**
 * Item-pet-summon flood protector.
 */
private final FloodProtectorAction _itemPetSummon;
/**
 * Hero-voice flood protector.
 */
private final FloodProtectorAction _heroVoice;
/**
 * shout flood protector.
 */
private final FloodProtectorAction _shout;
/**
 * trade chat flood protector.
 */
private final FloodProtectorAction _tradeChat;
/**
 * partyroom flood protector.
 */
private final FloodProtectorAction _partyRoom;
/**
 * partyroom commander flood protector.
 */
private final FloodProtectorAction _partyRoomCommander;
/**
 * Subclass flood protector.
 */
private final FloodProtectorAction _subclass;
/**
 * Drop-item flood protector.
 */
private final FloodProtectorAction _dropItem;
/**
 * Server-bypass flood protector.
 */
private final FloodProtectorAction _serverBypass;
/**
 * Multisell flood protector.
 */
private final FloodProtectorAction _multiSell;
/**
 * Transaction flood protector.
 */
private final FloodProtectorAction _transaction;

/**
 * Creates new instance of FloodProtectors.
 * 
 * @param player
 *            player for which the collection of flood protectors is being created.
 */
public FloodProtectors(final L2PcInstance player)
{
	super();
	_useItem = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_USE_ITEM);
	_rollDice = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_ROLL_DICE);
	_firework = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_FIREWORK);
	_itemPetSummon = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_ITEM_PET_SUMMON);
	_heroVoice = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_HERO_VOICE);
	_shout = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_SHOUT);
	_tradeChat = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_TRADE_CHAT);
	_partyRoom = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_PARTY_ROOM);
	_partyRoomCommander = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_PARTY_ROOM_COMMANDER);
	_subclass = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_SUBCLASS);
	_dropItem = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_DROP_ITEM);
	_serverBypass = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_SERVER_BYPASS);
	_multiSell = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_MULTISELL);
	_transaction = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_TRANSACTION);
}

/**
 * Returns {@link #_useItem}.
 * 
 * @return {@link #_useItem}
 */
public FloodProtectorAction getUseItem()
{
	return _useItem;
}

/**
 * Returns {@link #_rollDice}.
 * 
 * @return {@link #_rollDice}
 */
public FloodProtectorAction getRollDice()
{
	return _rollDice;
}

/**
 * Returns {@link #_firework}.
 * 
 * @return {@link #_firework}
 */
public FloodProtectorAction getFirework()
{
	return _firework;
}

/**
 * Returns {@link #_itemPetSummon}.
 * 
 * @return {@link #_itemPetSummon}
 */
public FloodProtectorAction getItemPetSummon()
{
	return _itemPetSummon;
}

/**
 * Returns {@link #_heroVoice}.
 * 
 * @return {@link #_heroVoice}
 */
public FloodProtectorAction getHeroVoice()
{
	return _heroVoice;
}

public FloodProtectorAction getShout()
{
	return _shout;
}

public FloodProtectorAction getTradeChat()
{
	return _tradeChat;
}

public FloodProtectorAction getPartyRoom()
{
	return _partyRoom;
}

public FloodProtectorAction getPartyRoomCommander()
{
	return _partyRoomCommander;
}
/**
 * Returns {@link #_subclass}.
 * 
 * @return {@link #_subclass}
 */
public FloodProtectorAction getSubclass()
{
	return _subclass;
}

/**
 * Returns {@link #_dropItem}.
 * 
 * @return {@link #_dropItem}
 */
public FloodProtectorAction getDropItem()
{
	return _dropItem;
}

/**
 * Returns {@link #_serverBypass}.
 * 
 * @return {@link #_serverBypass}
 */
public FloodProtectorAction getServerBypass()
{
	return _serverBypass;
}

/**
 * Returns {@link #_multisell}.
 * 
 * @return {@link #_multisell}
 */
public FloodProtectorAction getMultiSell()
{
	return _multiSell;
}

/**
 * Returns {@link #_transaction}.
 * 
 * @return {@link #_transaction}
 */
public FloodProtectorAction getTransaction()
{
	return _transaction;
}

}
