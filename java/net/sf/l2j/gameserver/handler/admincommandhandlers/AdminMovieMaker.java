package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.MovieMakerManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author  KKnD
 */
public class AdminMovieMaker implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS = 
	{ 
		"admin_addseq",
		"admin_playseqq",
		"admin_delsequence",
		"admin_editsequence",
		"admin_addsequence",
		"admin_playsequence",
		"admin_movie",
		"admin_updatesequence",
		"admin_brodcast",
		"admin_playmovie",
		"admin_brodmovie"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		if (command.equals("admin_movie"))
		{
			MovieMakerManager.getInstance().main_txt(activeChar);
		}
		else if (command.startsWith("admin_playseqq"))
		{
			final String val = command.substring(15);
			final int id = Integer.parseInt(val);
			MovieMakerManager.getInstance().play_sequence(id, activeChar);
		}
		else if (command.equals("admin_addseq"))
		{
			MovieMakerManager.getInstance().add_seq(activeChar);
		}
		else if (command.startsWith("admin_addsequence"))
		{
			final String[] args = command.split(" ");
			int targ = 0;
			if (args.length < 10)
			{
				activeChar.sendMessage("Not all arguments was set");
				return false;
			}
			if (activeChar.getTarget() != null)
				targ = activeChar.getTarget().getObjectId();
			else
			{
				activeChar.sendMessage("Target for camera is missing");
				MovieMakerManager.getInstance().main_txt(activeChar);
				return false;
			}
			MovieMakerManager.getInstance().add_sequence(activeChar, Integer.parseInt(args[1]), targ, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]));
		}
		else if (command.startsWith("admin_playsequence"))
		{
			final String[] args = command.split(" ");
			int targ = 0;
			if (args.length < 10)
			{
				activeChar.sendMessage("Not all arguments was set");
				return false;
			}
			if (activeChar.getTarget() != null)
				targ = activeChar.getTarget().getObjectId();
			else
			{
				activeChar.sendMessage("Target for camera is missing");
				MovieMakerManager.getInstance().main_txt(activeChar);
				return false;
			}
			MovieMakerManager.getInstance().play_sequence(activeChar, targ, Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]));
		}
		else if (command.startsWith("admin_editsequence"))
		{
			final String val = command.substring(19);
			final int id = Integer.parseInt(val);
			MovieMakerManager.getInstance().edit_seq(id, activeChar);
		}
		else if (command.startsWith("admin_updatesequence"))
		{
			final String[] args = command.split(" ");
			int targ = 0;
			if (args.length < 10)
			{
				activeChar.sendMessage("Not all arguments was set");
				return false;
			}
			if (activeChar.getTarget() != null)
				targ = activeChar.getTarget().getObjectId();
			else
			{
				activeChar.sendMessage("Target for camera is missing");
				MovieMakerManager.getInstance().main_txt(activeChar);
				return false;
			}
			MovieMakerManager.getInstance().update_sequence(activeChar, Integer.parseInt(args[1]), targ, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]));
		}
		else if (command.startsWith("admin_delsequence"))
		{
			final String val = command.substring(18);
			final int id = Integer.parseInt(val);
			MovieMakerManager.getInstance().delete_sequence(id, activeChar);
		}
		else if (command.startsWith("admin_brodcast"))
		{
			final String val = command.substring(15);
			final int id = Integer.parseInt(val);
			MovieMakerManager.getInstance().brodcast_sequence(id, activeChar);
		}
		else if (command.equals("admin_playmovie"))
			MovieMakerManager.getInstance().play_movie(0, activeChar);
		else if (command.equals("admin_brodmovie"))
			MovieMakerManager.getInstance().play_movie(1, activeChar);
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}