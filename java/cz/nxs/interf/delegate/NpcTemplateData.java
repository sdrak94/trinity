package cz.nxs.interf.delegate;

import cz.nxs.l2j.delegate.INpcTemplateData;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author hNoke
 *
 */
public class NpcTemplateData implements INpcTemplateData
{
	private L2NpcTemplate _template;
	
	private String _spawnName = null;
	private String _spawnTitle = null;
	
	public NpcTemplateData(int id)
	{
		_template = NpcTable.getInstance().getTemplate(id);
	}
	
	@Override
	public void setSpawnName(String name)
	{
		_spawnName = name;
	}
	
	@Override
	public void setSpawnTitle(String title)
	{
		_spawnTitle = title;
	}
	
	@Override
	public boolean exists()
	{
		return _template != null;
	}
	
	@Override
	public NpcData doSpawn(int x, int y, int z, int ammount, int instanceId)
	{
		return doSpawn(x, y, z, ammount, 0, instanceId);
	}
	
	@Override
	public NpcData doSpawn(int x, int y, int z, int ammount, int heading, int instanceId)
	{
		return doSpawn(x, y, z, ammount, heading, 0, instanceId);
	}
	
	@Override
	public NpcData doSpawn(int x, int y, int z, int ammount, int heading, int respawn, int instanceId)
	{
		if(_template == null)
			return null;
		
		L2Spawn spawn;
		try
		{
			spawn = new L2Spawn(_template);
			
			spawn.setLocx(x);
			spawn.setLocy(y);
			spawn.setLocz(z);
			spawn.setAmount(1);
			spawn.setHeading(heading);
			spawn.setRespawnDelay(respawn);
			
			spawn.setInstanceId(instanceId);
			
			L2Npc npc = spawn.doSpawn();
			NpcData npcData = new NpcData(npc);
			
			boolean update = false;
			if(_spawnName != null)
			{
				npc.setName(_spawnName);
				update = true;
			}
			if(_spawnTitle != null)
			{
				npc.setTitle(_spawnTitle);
				update = true;
			}
			
			if(update)
				npcData.broadcastNpcInfo();
			
			return npcData;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
}
