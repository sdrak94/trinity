/**
 * 
 */
package cz.nxs.interf.delegate;

import net.sf.l2j.gameserver.model.L2ShortCut;

/**
 * @author hNoke
 *
 */
public class ShortCutData
{
	private L2ShortCut _shortcut;
	
	public ShortCutData(int slotId, int pageId, int shortcutType, int shortcutId, int shortcutLevel, int characterType)
	{
		_shortcut = new L2ShortCut(slotId, pageId, shortcutType, shortcutId, shortcutLevel, characterType);
	}
	
	public int getId()
    {
        return _shortcut.getId();
    }

    public int getLevel()
    {
        return _shortcut.getLevel();
    }

    public int getPage()
    {
        return _shortcut.getPage();
    }

    public int getSlot()
    {
        return _shortcut.getSlot();
    }

    public int getType()
    {
        return _shortcut.getType();
    }
    
    public int getCharacterType()
    {
    	return _shortcut.getCharacterType();
    }
}
