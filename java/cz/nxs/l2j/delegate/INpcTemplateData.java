/**
 * 
 */
package cz.nxs.l2j.delegate;

import cz.nxs.interf.delegate.NpcData;

/**
 * @author hNoke
 *
 */
public interface INpcTemplateData
{
	public void setSpawnName(String name);
	public void setSpawnTitle(String title);
	
	public boolean exists();
	
	public NpcData doSpawn(int x, int y, int z, int ammount, int instanceId);
	public NpcData doSpawn(int x, int y, int z, int ammount, int heading, int instanceId);
	public NpcData doSpawn(int x, int y, int z, int ammount, int heading, int respawn, int instanceId);
}
