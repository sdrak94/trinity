package cz.nxs.events;

import java.util.Map;

import cz.nxs.events.engine.base.ConfigModel;
import cz.nxs.events.engine.base.EventMap;
import cz.nxs.events.engine.base.RewardPosition;
import cz.nxs.events.engine.base.SpawnType;
import javolution.util.FastList;

/**
 * @author hNoke
 * configurable part of an event (MiniEventManager, AbstractMainEvent)
 */
public interface Configurable
{
	public void loadConfigs();
	public void clearConfigs();
	public FastList<String> getCategories();
	
	public Map<String, ConfigModel> getConfigs();
	public Map<String, ConfigModel> getMapConfigs();
	public RewardPosition[] getRewardTypes();
	public Map<SpawnType, String> getAviableSpawnTypes();
	
	public void setConfig(String key, String value, boolean addToValue);
	public String getDescriptionForReward(RewardPosition reward);
	
	public int getTeamsCount();
	public boolean canRun(EventMap map);
	public String getMissingSpawns(EventMap map);
}
