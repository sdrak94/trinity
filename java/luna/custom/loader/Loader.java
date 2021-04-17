package luna.custom.loader;

import java.util.logging.Logger;

import luna.custom.globalScheduler.DelaysController;
import luna.custom.globalScheduler.GlobalEventsParser;
import luna.custom.globalScheduler.RealTimeController;
import luna.custom.globalScheduler.ScheduleExecutioner;
import luna.custom.handler.WarFinisherChecker;
import luna.custom.handler.commands.AdminStopWar;
import luna.custom.handler.commands.UserGem;
import luna.custom.handler.items.bdoBox.BDOBox;
import luna.custom.handler.items.bonanzo.Bonanzo;
import luna.custom.handler.items.capsuledItems.Capsule;
import luna.custom.handler.items.lootBox.LootBox;
import luna.custom.skilltrees.SkillTreesParser;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.UserCommandHandler;
import net.sf.l2j.gameserver.model.events.Communicator;

public class Loader
{
	private static final Logger _log = Logger.getLogger(Loader.class.getName());
	
	public void load()
	{
		_log.info("Initializing Luna Loader");
		loadItems();
		loadCommands();
		loadGlobalEvents();
		SkillTreesParser.getInstance().Reload(null);
		WarFinisherChecker.getInstance().init();
	}
	
	private void loadItems()
	{
		ItemHandler.getInstance().registerItemHandler(new Bonanzo());
		ItemHandler.getInstance().registerItemHandler(new LootBox());
		ItemHandler.getInstance().registerItemHandler(new Capsule());
		ItemHandler.getInstance().registerItemHandler(new BDOBox());
		_log.info("Luna Item Handlers Loaded");
	}
	
	private void loadCommands()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminStopWar());
		UserCommandHandler.getInstance().registerUserCommandHandler(new UserGem());
		_log.info("Luna Admin Command Handlers Loaded");
	}
	
	private void loadGlobalEvents()
	{
		GlobalEventsParser.getInstance().reload();
		// load controllers
		ScheduleExecutioner.getInstance();
		DelaysController.getInstance();
		RealTimeController.load();
		Communicator.getInstance().getTodayGlobalEvents();
		System.out.println("Loaded Global Event Engine.");
	}
	
	public static Loader getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final Loader INSTANCE = new Loader();
	}
}
