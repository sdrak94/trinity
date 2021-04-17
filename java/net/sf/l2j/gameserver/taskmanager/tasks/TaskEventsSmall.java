package net.sf.l2j.gameserver.taskmanager.tasks;

import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.taskmanager.Task;
import net.sf.l2j.gameserver.taskmanager.TaskManager.ExecutedTask;

public final class TaskEventsSmall extends Task
{
public static final String NAME = "events_small";

@Override
public String getName()
{
	return NAME;
}

@Override
public void onTimeElapsed(ExecutedTask task)
{
	if (!DM._joining && !DM._teleport && !DM._started)
	{
		DM.loadData(0);
		DM.autoEvent();
	}
}
}