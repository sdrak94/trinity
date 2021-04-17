package cz.nxs.events.engine.main.base;

import java.util.concurrent.ScheduledFuture;

import cz.nxs.events.engine.main.events.AbstractMainEvent.Clock;
import cz.nxs.interf.delegate.InstanceData;

/**
 * @author hNoke
 *
 */
public interface IEventInstance
{
	public InstanceData getInstance();
	public ScheduledFuture<?> scheduleNextTask(int time);
	public Clock getClock();
	public boolean isActive();
}
