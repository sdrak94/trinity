package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class AdminLocRecorder implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_loc",
		"admin_loc_npc",
		"admin_loc_new_stage"
	};

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		L2Object obj = activeChar.getTarget();
		L2Npc target = (L2Npc) obj;
		
		if (command.equals("admin_loc"))
		{
			File file = new File("data/instancelocs/1_locs.txt");
		    FileWriter fr = null;
			try
			{
				fr = new FileWriter(file, true);
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		    try
		    {
				fr.write("");
			}
		    catch (IOException e2)
		    {
				e2.printStackTrace();
			}
		    try
		    {
				fr.close();
			}
		    catch (IOException e1)
		    {
				e1.printStackTrace();
			}
		    
		    try
		    {
				fr = new FileWriter(file, true);
				fr.write( activeChar.getX() + ", " + activeChar.getY() + ", " + activeChar.getZ() + ", " + activeChar.getHeading() +"\r\n");

			}
		    catch (IOException e)
		    {
				e.printStackTrace();
			}
		    finally
		    {
				try
				{
					fr.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			activeChar.sendMessage("Your heading is: " + activeChar.getHeading());
			StringSelection selection = new StringSelection(activeChar.getX() + ", " + activeChar.getY() + ", " + activeChar.getZ() + ", " + activeChar.getHeading());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		    clipboard.setContents(selection, selection);
		    activeChar.sendMessage("Succesfully saved on Server's clipboard!");
		}
		else if (command.startsWith("admin_loc_npc"))
		{
			   if (activeChar.getTarget() instanceof L2Npc)
			   {
				    File file = new File("data/instancelocs/locs.txt");
				    FileWriter fr = null;
					try
					{
						fr = new FileWriter(file, true);
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				    try
				    {
						fr.write("");
					}
				    catch (IOException e2)
				    {
						e2.printStackTrace();
					}
				    try
				    {
						fr.close();
					}
				    catch (IOException e1)
				    {
						e1.printStackTrace();
					}
				    
				    try
				    {
						fr = new FileWriter(file, true);
						fr.write("addSpawn("+target.getNpcId() + ", " + target.getX() + ", " + target.getY() + ", " + target.getZ() + ", " + target.getHeading() + ", false, 0, false, world.instanceId);"+ " //" + target.getName()+"\r\n");
		
					}
				    catch (IOException e)
				    {
						e.printStackTrace();
					}
				    finally
				    {
						try
						{
							fr.close();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
			   }
				else 
					activeChar.sendServerMessage("Target must be Npc or Monster");
		}

		else if (command.startsWith("admin_loc_new_stage"))
		{
			StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				String stage = st.nextToken();
				
				    File file = new File("data/instancelocs/locs.txt");
				    FileWriter fr = null;
					try
					{
						fr = new FileWriter(file, true);
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				    try
				    {
						fr.write("");
					}
				    catch (IOException e2)
				    {
						e2.printStackTrace();
					}
				    try
				    {
						fr.close();
					}
				    catch (IOException e1)
				    {
						e1.printStackTrace();
					}
				    
				    try
				    {
						fr = new FileWriter(file, true);
						fr.write("====================New Stage: "+stage+" ====================\r\n");
		
					}
				    catch (IOException e)
				    {
						e.printStackTrace();
					}
				    finally
				    {
						try
						{
							fr.close();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
			   }
				else 
					activeChar.sendServerMessage("Target must be Npc or Monster");
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
