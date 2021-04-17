/**
 * 
 */
package cz.nxs.l2j;

import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.html.EventHtmlManager;



/**
 * @author hNoke
 *
 */
public class CallBack
{
	private INexusOut _out = null;
	private IPlayerBase _playerBase = null;
	private IValues _values = null;
	
	public CallBack()
	{
		
	}
	
	// ==========================================
	// GET METHODS
	// ==========================================
	
	public INexusOut getOut()
	{
		return _out;
	}
	
	public IPlayerBase getPlayerBase()
	{
		return _playerBase;
	}
	
	public IValues getValues()
	{
		return _values;
	}
	
	// 
	
	// ==========================================
	// SET METHODS
	// ==========================================
	
	public void setHtmlManager(EventHtmlManager manager)
	{
		EventManager.getInstance().setHtmlManager(manager);
	}
	
	public void setNexusOut(INexusOut out)
	{
		_out = out;
	}
	
	public void setPlayerBase (IPlayerBase base)
	{
		_playerBase = base;
	}
	
	public void setValues(IValues values)
	{
		_values = values;
	}
	
	// ==========================================
	// ==========================================
	
	public static CallBack getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final CallBack _instance = new CallBack();
	}
}
