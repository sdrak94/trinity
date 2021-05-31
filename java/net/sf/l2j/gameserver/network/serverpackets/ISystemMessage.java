package net.sf.l2j.gameserver.network.serverpackets;


public interface ISystemMessage
{
	public int getId();
	
	public String getName();
	
	public  int getParamCount();
	
	public void setParamCount(final int params);
	
	public SystemMessage getStaticSystemMessage();
	
	public void setStaticSystemMessage(final SystemMessage sm);
}
