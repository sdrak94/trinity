package net.sf.l2j.gameserver.taskmanager.tasks;

import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.taskmanager.Task;
import net.sf.l2j.gameserver.taskmanager.TaskManager.ExecutedTask;
import net.sf.l2j.util.Rnd;

public final class TaskEvents extends Task
{
public static final String NAME = "events";

@Override
public String getName()
{
	return NAME;
}

@Override
public void onTimeElapsed(ExecutedTask task)
{
	if (TvT._joining || TvT._started || TvT._teleport || NewTvT._joining || NewTvT._started || NewTvT._teleport || CTF._joining || CTF._started || CTF._teleport || FOS._joining || FOS._started || NewFOS._started || FOS._teleport || VIP._joining || VIP._started || VIP._sitForced || DM._joining || DM._started || DM._sitForced)
		return;
	
	final int chance = Rnd.get(100);
	
	if (chance >= 90) //10% for VIP
	{
		if (!VIP._joining && !VIP._sitForced && !VIP._started)
		{
			if (chance >= 95)
				VIP.setTeam("Human");
			else if (chance >= 93)
				VIP.setTeam("Elf");
			else if (chance >= 92)
				VIP.setTeam("Dark");
			else if (chance >= 91)
				VIP.setTeam("Kamael");
			else
			{
				if (Rnd.get(2) == 0)
					VIP.setTeam("Dwarf");
				else
					VIP.setTeam("Orc");
			}
			
			VIP.startAutoJoin();
		}
	}
	else if (chance >= 52) //38% for TVT
	{
		if (!TvT._joining && !TvT._sitForced && !TvT._started && !TvT._teleport)
		{
			TvT.loadData(0);
			TvT.autoEvent();
		}
	}
	else if (chance >= 30) //22% for CTF
	{
		if (!CTF._joining && !CTF._sitForced && !CTF._started && !CTF._teleport)
		{
			CTF.loadData(0);
			CTF.autoEvent();
		}
	}
	 else  //30% for FOS
	    {
	        if (!FOS._joining && !FOS._sitForced && !FOS._started && !FOS._teleport)
	        {
	            if (Rnd.get(2) == 0)
	                FOS.loadData(0);
	            else
	                FOS.loadData(0);
	            FOS.autoEvent();
	        }
	    }
}
}