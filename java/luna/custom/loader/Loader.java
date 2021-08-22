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
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.events.Communicator;
import scripts.transformations.*;

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
		loadTransformations();
		QuestManager.getInstance().reloadAllQuests();
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
	
	private void loadTransformations()
	{
		TransformationManager.getInstance().registerTransformation(new Akamanah());
		TransformationManager.getInstance().registerTransformation(new Anakim());
		TransformationManager.getInstance().registerTransformation(new ArcherCaptain());
		TransformationManager.getInstance().registerTransformation(new ArmoredBuffalo());
		TransformationManager.getInstance().registerTransformation(new ArmoredOstrich());
		TransformationManager.getInstance().registerTransformation(new ArmoredTiger());
		TransformationManager.getInstance().registerTransformation(new AurabirdFalcon());
		TransformationManager.getInstance().registerTransformation(new AurabirdOwl());
		TransformationManager.getInstance().registerTransformation(new Benom());
		TransformationManager.getInstance().registerTransformation(new Buffalo());
		TransformationManager.getInstance().registerTransformation(new Centaur());
		TransformationManager.getInstance().registerTransformation(new ClockCucuru());
		TransformationManager.getInstance().registerTransformation(new DarkElfMercenary());
		TransformationManager.getInstance().registerTransformation(new DelfTransform());
		TransformationManager.getInstance().registerTransformation(new DemonPrince());
		TransformationManager.getInstance().registerTransformation(new DemonRace());
		TransformationManager.getInstance().registerTransformation(new DivineEnchanter());
		TransformationManager.getInstance().registerTransformation(new DivineHealer());
		TransformationManager.getInstance().registerTransformation(new DivineKnight());
		TransformationManager.getInstance().registerTransformation(new DivineRogue());
		TransformationManager.getInstance().registerTransformation(new DivineSummoner());
		TransformationManager.getInstance().registerTransformation(new DivineWarrior());
		TransformationManager.getInstance().registerTransformation(new DivineWizard());
		TransformationManager.getInstance().registerTransformation(new DogMount());
		TransformationManager.getInstance().registerTransformation(new DollBlader());
		TransformationManager.getInstance().registerTransformation(new DoomHorse());
		TransformationManager.getInstance().registerTransformation(new DoomWraith());
		TransformationManager.getInstance().registerTransformation(new DragonBomberNormal());
		TransformationManager.getInstance().registerTransformation(new DragonBomberStrong());
		TransformationManager.getInstance().registerTransformation(new DragonBomberWeak());
		TransformationManager.getInstance().registerTransformation(new DragonHorseMount());
		TransformationManager.getInstance().registerTransformation(new DwarfCopter());
		TransformationManager.getInstance().registerTransformation(new DwarfGolem());
		TransformationManager.getInstance().registerTransformation(new DwarfMercenary());
		TransformationManager.getInstance().registerTransformation(new ElderPegasusMount());
		TransformationManager.getInstance().registerTransformation(new ElfMercenary());
		TransformationManager.getInstance().registerTransformation(new ElfTransform());
		TransformationManager.getInstance().registerTransformation(new ErtheiaFox());
		TransformationManager.getInstance().registerTransformation(new FlyingFinalForm());
		TransformationManager.getInstance().registerTransformation(new FortressCaptain());
		TransformationManager.getInstance().registerTransformation(new Frog());
		TransformationManager.getInstance().registerTransformation(new GolemGuardianNormal());
		TransformationManager.getInstance().registerTransformation(new GolemGuardianStrong());
		TransformationManager.getInstance().registerTransformation(new GolemGuardianWeak());
		TransformationManager.getInstance().registerTransformation(new Gordon());
		TransformationManager.getInstance().registerTransformation(new GrailApostleNormal());
		TransformationManager.getInstance().registerTransformation(new GrailApostleStrong());
		TransformationManager.getInstance().registerTransformation(new GrailApostleWeak());
		TransformationManager.getInstance().registerTransformation(new GriffinMount());
		TransformationManager.getInstance().registerTransformation(new GrizzlyBear());
		TransformationManager.getInstance().registerTransformation(new GuardsoftheDawn());
		TransformationManager.getInstance().registerTransformation(new HeavyTow());
		TransformationManager.getInstance().registerTransformation(new Heretic());
		TransformationManager.getInstance().registerTransformation(new HumanMercenary());
		TransformationManager.getInstance().registerTransformation(new HumanTransform());
		TransformationManager.getInstance().registerTransformation(new InfernoDrakeNormal());
		TransformationManager.getInstance().registerTransformation(new InfernoDrakeStrong());
		TransformationManager.getInstance().registerTransformation(new InfernoDrakeWeak());
		TransformationManager.getInstance().registerTransformation(new InquisitorBishop());
		TransformationManager.getInstance().registerTransformation(new InquisitorElvenElder());
		TransformationManager.getInstance().registerTransformation(new InquisitorShilienElder());
		TransformationManager.getInstance().registerTransformation(new Kamael());
		TransformationManager.getInstance().registerTransformation(new KamaelGuardCaptain());
		TransformationManager.getInstance().registerTransformation(new KamaelMercenary());
		TransformationManager.getInstance().registerTransformation(new Kiyachi());
		TransformationManager.getInstance().registerTransformation(new KnightofDawn());
		TransformationManager.getInstance().registerTransformation(new LavaGolem());
		TransformationManager.getInstance().registerTransformation(new LightPurpleManedHorse());
		TransformationManager.getInstance().registerTransformation(new LilimKnightNormal());
		TransformationManager.getInstance().registerTransformation(new LilimKnightStrong());
		TransformationManager.getInstance().registerTransformation(new LilimKnightWeak());
		TransformationManager.getInstance().registerTransformation(new LindviorMount());
		TransformationManager.getInstance().registerTransformation(new LindviorMount2());
		TransformationManager.getInstance().registerTransformation(new LureTow());
		TransformationManager.getInstance().registerTransformation(new MagicLeader());
		TransformationManager.getInstance().registerTransformation(new MyoRace());
		TransformationManager.getInstance().registerTransformation(new Native());
		TransformationManager.getInstance().registerTransformation(new OlMahum());
		TransformationManager.getInstance().registerTransformation(new OnyxBeast());
		TransformationManager.getInstance().registerTransformation(new OrbisTiger());
		TransformationManager.getInstance().registerTransformation(new OrcMercenary());
		TransformationManager.getInstance().registerTransformation(new OrcSlave());
		TransformationManager.getInstance().registerTransformation(new Pig());
		TransformationManager.getInstance().registerTransformation(new Pixy());
		TransformationManager.getInstance().registerTransformation(new PumpkinGhost());
		TransformationManager.getInstance().registerTransformation(new Rabbit());
		TransformationManager.getInstance().registerTransformation(new Ranku());
		TransformationManager.getInstance().registerTransformation(new RoyalGuardCaptain());
		TransformationManager.getInstance().registerTransformation(new RoyalLion());
		TransformationManager.getInstance().registerTransformation(new SaberTigerMount());
		TransformationManager.getInstance().registerTransformation(new SaberToothTiger());
		TransformationManager.getInstance().registerTransformation(new SalamanderMount());
		TransformationManager.getInstance().registerTransformation(new Scarecrow());
		TransformationManager.getInstance().registerTransformation(new ScrollBlue());
		TransformationManager.getInstance().registerTransformation(new ScrollRed());
		TransformationManager.getInstance().registerTransformation(new SnowKung());
		TransformationManager.getInstance().registerTransformation(new SteamBeatle());
		TransformationManager.getInstance().registerTransformation(new TawnyManedLion());
		TransformationManager.getInstance().registerTransformation(new Teleporter());
		TransformationManager.getInstance().registerTransformation(new Teleporter2());
		TransformationManager.getInstance().registerTransformation(new Timitran());
		TransformationManager.getInstance().registerTransformation(new TinGolem());
		TransformationManager.getInstance().registerTransformation(new Tow());
		TransformationManager.getInstance().registerTransformation(new Unicorniun());
		TransformationManager.getInstance().registerTransformation(new UnicornNormal());
		TransformationManager.getInstance().registerTransformation(new UnicornStrong());
		TransformationManager.getInstance().registerTransformation(new UnicornWeak());
		TransformationManager.getInstance().registerTransformation(new ValeMaster());
		TransformationManager.getInstance().registerTransformation(new VanguardDarkAvenger());
		TransformationManager.getInstance().registerTransformation(new VanguardPaladin());
		TransformationManager.getInstance().registerTransformation(new VanguardShilienKnight());
		TransformationManager.getInstance().registerTransformation(new VanguardTempleKnight());
		TransformationManager.getInstance().registerTransformation(new WarDrake());
		TransformationManager.getInstance().registerTransformation(new WarTiger());
		TransformationManager.getInstance().registerTransformation(new WingedHound());
		TransformationManager.getInstance().registerTransformation(new WingTow());
		TransformationManager.getInstance().registerTransformation(new WizardsMount());
		TransformationManager.getInstance().registerTransformation(new Yeti());
		TransformationManager.getInstance().registerTransformation(new Yeti2());
		TransformationManager.getInstance().registerTransformation(new YoungChild());
		TransformationManager.getInstance().registerTransformation(new Zaken());
		TransformationManager.getInstance().registerTransformation(new Zariche());
		TransformationManager.getInstance().registerTransformation(new Zombie());
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
