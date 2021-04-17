/**
 * 
 */
package cz.nxs.l2j.delegate;

import cz.nxs.interf.delegate.FenceData;
import cz.nxs.interf.delegate.NpcData;

/**
 * @author hNoke
 *
 */
public interface IObjectData
{
	public int getObjectId();
	
	public boolean isPlayer();
	public boolean isSummon();
	
	public boolean isFence();
	public FenceData getFence();
	
	public boolean isNpc();
	public NpcData getNpc();
}
