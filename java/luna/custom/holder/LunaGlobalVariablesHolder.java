package luna.custom.holder;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class LunaGlobalVariablesHolder
{
	private boolean	_doublePvPs			= false;
	private boolean	_doublePvPsHunter	= false;
	private boolean	_doublePvPsGludin	= false;
	private boolean	_autoFlagGludin		= false;
	private boolean	_doublePvPsPI		= false;
	
	public void setDoublePvPs(boolean val)
	{
		_doublePvPs = val;
	}
	
	public boolean getDoublePvPs()
	{
		return _doublePvPs;
	}
	
	public void setDoublePvPsHunter(boolean val)
	{
		_doublePvPsHunter = val;
	}
	
	public boolean getDoublePvPsHunter()
	{
		return _doublePvPsHunter;
	}
	
	public void setDoublePvPsGludin(boolean val)
	{
		_doublePvPsGludin = val;
	}
	
	public boolean getDoublePvPsGludin()
	{
		return _doublePvPsGludin;
	}
	
	public void setDoublePvPsPI(boolean val)
	{
		_doublePvPsPI = val;
	}
	
	public boolean getDoublePvPsPI()
	{
		return _doublePvPsPI;
	}
	
	public void endEventDoublePvP()
	{
		setDoublePvPsHunter(false);
		setDoublePvPsGludin(false);
		setAutoFlagGludin(false);
		setDoublePvPsPI(false);
	}
	
	public void setAutoFlagGludin(boolean val)
	{
		_autoFlagGludin = val;
		updateGludinPlayers(val);
	}
	
	public boolean getAutoFlagGludin()
	{
		return _autoFlagGludin;
	}
	
	public void updateGludinPlayers(boolean val)
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player.getClient().isDetached())
			{
				continue;
			}
			if (player.isGM())
			{
				continue;
			}
			if (!player.isInGludin())
			{
				continue;
			}
			if(val)
			{
				player.updatePvPFlag(1);
				player.sendMessage("PvP Flag status updated");
				player.broadcastUserInfo();
			}
			else
			{
				player.setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
				player.startPvPFlag();
				player.broadcastUserInfo();
			}
		}
	}
	
	public void startDoublePvP(int hours)
	{
		_doublePvPs = true;
		Announcements.getInstance().announceToAll("Double PvPs is activated for " + hours + " hours.");
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				_doublePvPs = false;
				Announcements.getInstance().announceToAll("Double PvPs is over.");
			}
		}, hours * 60 * 60 * 1000);
	}
	
	public static LunaGlobalVariablesHolder getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LunaGlobalVariablesHolder INSTANCE = new LunaGlobalVariablesHolder();
	}
}
