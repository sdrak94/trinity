/**
 * 
 */
package cz.nxs.l2j.delegate;

import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.ObjectData;

/**
 * @author hNoke
 *
 */
public interface INpcData
{
	public ObjectData getObjectData();
	
	public void setName(String name);
	public void setTitle(String t);
	
	public int getNpcId();
	
	public void setEventTeam(int team);
	public int getEventTeam();
	
	public void broadcastNpcInfo();
	public void broadcastSkillUse(CharacterData owner, CharacterData target, int skillId, int level);
	
	public void deleteMe();
}
