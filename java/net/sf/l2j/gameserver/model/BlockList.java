package net.sf.l2j.gameserver.model;

import java.util.Set;

import javolution.util.FastSet;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class BlockList
{
    private final Set<String> _blockSet;
    private boolean _blockAll;

    public BlockList()
    {
        _blockSet    = new FastSet<String>();
        _blockAll    = false;
    }

    private synchronized void addToBlockList(L2PcInstance character)
    {
        if(character != null)
        {
        	final String name = character.getName();
        	
            _blockSet.add(name);
        }
    }

    private synchronized void removeFromBlockList(L2PcInstance character)
    {
        if(character != null)
        {
        	final String name = character.getName();
        	
            _blockSet.remove(name);
        }
    }

    private boolean isInBlockList(L2PcInstance character)
    {
    	if (character != null)
    	{
    		final String name = character.getName();
    		
            return _blockSet.contains(name);
    	}
    	
    	return false;
    }

    public boolean isBlockAll()
    {
        return _blockAll;
    }

    public static boolean isBlocked(L2PcInstance listOwner, L2PcInstance character)
    {
    	if (listOwner == character) return false;
    	
        BlockList blockList = listOwner.getBlockList();
        return /*blockList.isBlockAll() ||*/ blockList.isInBlockList(character);
    }

    private void setBlockAll(boolean state)
    {
        _blockAll = state;
    }

    private Set<String> getBlockList()
    {
        return _blockSet;
    }

    public static void addToBlockList(L2PcInstance listOwner, L2PcInstance character)
    {
        listOwner.getBlockList().addToBlockList(character);

        SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
        sm.addString(listOwner.getName());
        character.sendPacket(sm);

        sm = new SystemMessage(SystemMessageId.S1_WAS_ADDED_TO_YOUR_IGNORE_LIST);
        sm.addString(character.getName());
        listOwner.sendPacket(sm);
    }

    public static void removeFromBlockList(L2PcInstance listOwner, L2PcInstance character)
    {
        listOwner.getBlockList().removeFromBlockList(character);

        SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_REMOVED_FROM_YOUR_IGNORE_LIST);
        sm.addString(character.getName());
        listOwner.sendPacket(sm);
    }

    public static boolean isInBlockList(L2PcInstance listOwner, L2PcInstance character)
    {
        return listOwner.getBlockList().isInBlockList(character);
    }

    public static void setBlockAll(L2PcInstance listOwner, boolean newValue)
    {
        listOwner.getBlockList().setBlockAll(newValue);
    }

    public static void sendListToOwner(L2PcInstance listOwner)
    {
        for (String playerName : listOwner.getBlockList().getBlockList())
        {
            listOwner.sendMessage(playerName);
        }
    }
}