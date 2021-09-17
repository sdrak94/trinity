package luna.custom.loader;

import java.util.logging.Level;
import java.util.logging.Logger;

import Alpha.autopots.AutoPots;
import ghosts.controller.GhostController;
import ghosts.controller.GhostTemplateTable;
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
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.BBSSchemeBufferInstance;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.UserCommandHandler;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.quest.Quest.QuestEventType;
import net.sf.l2j.gameserver.script.mobius.ScriptEngineManager;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import scripts.ai.groupTemplates.L2AttackableAIScript;
import scripts.quests.Alliance;
import scripts.quests.Clan;
import scripts.quests.HeroCirclet;
import scripts.quests.HeroWeapon;
import scripts.quests.Q241_PossessorOfAPreciousSoul;
import scripts.quests.Q242_PossessorOfAPreciousSoul;
import scripts.quests.Q246_PossessorOfAPreciousSoul;
import scripts.quests.Q247_PossessorOfAPreciousSoul;

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
		//loadTransformations();
		loadQuests();
		QuestManager.getInstance().reloadAllQuests();
		GhostTemplateTable.getInstance();
		GhostController.getInstance();
		BBSSchemeBufferInstance.getInstance().load();
		try
		{
			_log.info("Loading server scripts...");
//			ScriptEngineManager.getInstance().executeScript(ScriptEngineManager.MASTER_HANDLER_FILE);
			ScriptEngineManager.getInstance().executeScriptList();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Failed to execute script list!", e);
		}
	}
	
	public void loadQuests()
	{
		QuestManager.getInstance().addQuest(new Clan());
		QuestManager.getInstance().addQuest(new Alliance());
		QuestManager.getInstance().addQuest(new HeroCirclet());
		QuestManager.getInstance().addQuest(new HeroWeapon());
		QuestManager.getInstance().addQuest(new Q241_PossessorOfAPreciousSoul());
		QuestManager.getInstance().addQuest(new Q242_PossessorOfAPreciousSoul());
		QuestManager.getInstance().addQuest(new Q246_PossessorOfAPreciousSoul());
		QuestManager.getInstance().addQuest(new Q247_PossessorOfAPreciousSoul());
	}
	
	private void loadItems()
	{
		ItemHandler.getInstance().registerItemHandler(new AutoPots());
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
	
	private void loadTransformations()
	{

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
