//package net.sf.l2j.gameserver.handler.voicedcommandhandlers;
//
//import cz.nxs.interf.NexusEvents;
//import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
//import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
//
///**
// * @author Luna
// */
//public class RespawnVCmd implements IVoicedCommandHandler
//{
//	private static final String[] VOICED_COMMANDS =
//	{
//		"respawn"
//	};
//	
//	@Override
//	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
//	{
//		if(!NexusEvents.isInEvent(activeChar))
//		{
//			return false;
//		}
//		return true;
//	}
//	
//	@Override
//	public String[] getVoicedCommandList()
//	{
//		return VOICED_COMMANDS;
//	}
//}