package luna;

import net.sf.l2j.gameserver.model.base.Race;

public interface IPlayerInfo 
{
	public int getObjectId();
	
	public String getPlayerName();
	
	public String getPlayerTitle();
	
	public int getFame();
	
	public int getPvp();
	
	public int getPk();

	public int getCurrClassId();
	
	public int getBaseClassId();
	
	public long getBaseClassExp();
	
	public long getCurrClassExp();
	
	public int[] getPaperdollInfo(final int indx);
	
	public int getClanId();
	
	public boolean isClanLeader();
	
	public String getClanName();
	
	public int getBaseLevel();
	
	public int getCurrLevel();
	
	public int getFaceStyle();
	
	public int getHairStyle();
	
	public int getHairColor();
	
	public Race getRace();
	
	public int getSex();
	
	public int getAccessLevel();
	
}
