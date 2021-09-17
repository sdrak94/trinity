package ai.fantasy;

import net.sf.l2j.gameserver.instancemanager.QuestManager;

public class StartMCShow implements Runnable {
	@Override
	public void run() {
		try {
		QuestManager.getInstance().getQuest("MC_Show").notifyEvent("Start", null, null);
		} catch (Exception e){}
	}
}
