/**
 * 
 */
package cz.nxs.events.engine.main.pvpzone;

import java.util.concurrent.ScheduledFuture;

import cz.nxs.events.EventGame;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;


/**
 * @author hNoke
 *
 */
public abstract class PvpZone implements EventGame
{
	public PvpZone()
	{
		
	}
	
	public abstract String getName();
	public abstract void start();
	public abstract void end();
	
	public abstract boolean canRegister(PlayerEventInfo player);
	public abstract boolean canUnregister(PlayerEventInfo player);
	public abstract void addPlayer(PlayerEventInfo player);
	public abstract void removePlayer(PlayerEventInfo player);
	
	public abstract int getPlayersCountForHtml();
	public abstract String getStateNameForHtml();
	
	public abstract void scheduledCheck();
	
	protected Checker _checker;
	
	public class Checker implements Runnable
	{
		int duration = 0;
		boolean enabled;
		
		ScheduledFuture<?> _future;
		
		public Checker(int repeatDuration)
		{
			this.duration = repeatDuration;
			enabled = false;
		}
		
		public void setDuration(int duration)
		{
			this.duration = duration; 
		}
		
		public void start()
		{
			enabled = true;
			scheduleNextCheck();
		}
		
		private void scheduleNextCheck()
		{
			if(enabled)
				_future = CallBack.getInstance().getOut().scheduleGeneral(this, duration);
		}
		
		public void disable()
		{
			if(_future != null)
			{
				_future.cancel(false);
				_future = null;
			}
			
			enabled = false;
		}
		
		@Override
		public void run()
		{
			try
			{
				scheduledCheck();
				scheduleNextCheck();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
