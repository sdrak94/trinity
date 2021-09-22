package net.sf.l2j.gameserver;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.util.Rnd;

public class CastleUpdater implements Runnable
{
	protected static final Logger _log = Logger.getLogger(CastleUpdater.class.getName());
	private final L2Clan _clan;
	private int _runCount = 0;
	
	public CastleUpdater(L2Clan clan, int runCount)
	{
		_clan = clan;
		_runCount = runCount;
	}
	
	public void run()
	{
		try
		{
			final ItemContainer warehouse = _clan.getWarehouse();
			
			if (warehouse != null && _clan.getHasCastle() > 0)
			{
				Castle castle = CastleManager.getInstance().getCastleById(_clan.getHasCastle());
				
				warehouse.addItem("Castle", 97003, 3000, null, null); //3000 silver
				warehouse.addItem("Castle", 961, 4, null, null); //4 crystal enchant weapon
				warehouse.addItem("Castle", 962, 20, null, null); //20 crystal enchant armor
				warehouse.addItem("Castle", 5283, 15, null, null); //15 rice cake
				warehouse.addItem("Castle", 9546+Rnd.get(6), 2, null, null); // random elemental stone
				warehouse.addItem("Castle", 4355, 2000, null, null);   //blue eva
				
				if (Rnd.get(100) < 12) warehouse.addItem("Castle", 8752, 1, null, null); // high life stone
				
				if (castle.getCastleId() == 8) //rune
				{
					if (Rnd.get(100) < 50) warehouse.addItem("Castle", 6392, 1, null, null);   //event medal
					if (Rnd.get(100) < 5) warehouse.addItem("Castle", 4356, 1, null, null);   //gold
					
					if (Rnd.get(100) < 5) warehouse.addItem("Castle", 6577, 1, null, null);   //blessed weapon
					if (Rnd.get(100) < 8) warehouse.addItem("Castle", 6578, 1, null, null);   //blessed armor
					
					if (Rnd.get(100) < 1) warehouse.addItem("Castle", 8762, 1, null, null); // top life stone
				}
				else if (castle.getCastleId() == 5) //aden
				{
					if (Rnd.get(100) < 50) warehouse.addItem("Castle", 6392, 1, null, null);   //event medal
					if (Rnd.get(100) < 5) warehouse.addItem("Castle", 4356, 1, null, null);   //gold
					
					if (Rnd.get(100) < 5) warehouse.addItem("Castle", 6577, 1, null, null);   //blessed weapon
					if (Rnd.get(100) < 8) warehouse.addItem("Castle", 6578, 1, null, null);   //blessed armor
					
					if (Rnd.get(100) < 1) warehouse.addItem("Castle", 8762, 1, null, null); // top life stone
				}
				else if (castle.getCastleId() == 3) //giran
				{
					if (Rnd.get(100) < 40) warehouse.addItem("Castle", 6392, 1, null, null);   //event medal
					if (Rnd.get(100) < 4) warehouse.addItem("Castle", 4356, 1, null, null);   //gold
					
					if (Rnd.get(100) < 4) warehouse.addItem("Castle", 6577, 1, null, null);   //blessed weapon
					if (Rnd.get(100) < 6) warehouse.addItem("Castle", 6578, 1, null, null);   //blessed armor
				}
				else if (castle.getCastleId() == 7) //goddard
				{
					if (Rnd.get(100) < 40) warehouse.addItem("Castle", 6392, 1, null, null);   //event medal
					if (Rnd.get(100) < 4) warehouse.addItem("Castle", 4356, 1, null, null);   //gold
					
					/*                	if (Rnd.get(100) < 5) warehouse.addItem("Castle", 6577, 1, null, null);   //blessed weapon
                	if (Rnd.get(100) < 8) warehouse.addItem("Castle", 6578, 1, null, null);   //blessed armor
					 */
				}
				
				if (!Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				{
					if (_runCount % Config.ALT_MANOR_SAVE_PERIOD_RATE == 0)
					{
						castle.saveSeedData();
						castle.saveCropData();
						if (Config.DEBUG)
							_log.info("Manor System: all data for " + castle.getName() + " saved");
					}
				}
				
				CastleUpdater cu = new CastleUpdater(_clan, ++_runCount);
				ThreadPoolManager.getInstance().scheduleGeneral(cu, 86400000);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
