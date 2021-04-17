/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gsregistering;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import net.sf.l2j.loginserver.GameServerTable;

public class GameServerRegister extends BaseGameServerRegister
{
	private LineNumberReader _in;
	
	public static void main(String[] args)
	{
		// Backwards compatibility, redirect to the new one
		BaseGameServerRegister.main(args);
	}
	
	/**
	 * 
	 * @param bundle
	 */
	public GameServerRegister(ResourceBundle bundle)
	{
		super(bundle);
		this.load();
		
		int size = GameServerTable.getInstance().getServerNames().size();
		if (size == 0)
		{
			System.out.println(this.getBundle().getString("noServerNames"));
			System.exit(1);
		}
	}
	
	public void consoleUI() throws IOException
	{
		_in = new LineNumberReader(new InputStreamReader(System.in));
		boolean choiceOk = false;
		String choice;
		
		while (true)
		{
			this.hr();
			System.out.println("GSRegister");
			System.out.println('\n');
			System.out.println("1 - "+this.getBundle().getString("cmdMenuRegister"));
			System.out.println("2 - "+this.getBundle().getString("cmdMenuListNames"));
			System.out.println("3 - "+this.getBundle().getString("cmdMenuRemoveGS"));
			System.out.println("4 - "+this.getBundle().getString("cmdMenuRemoveAll"));
			System.out.println("5 - "+this.getBundle().getString("cmdMenuExit"));
			
			do
			{
				System.out.print(this.getBundle().getString("yourChoice")+' ');
				choice = _in.readLine();
				try
				{
					int choiceNumber = Integer.parseInt(choice);
					choiceOk = true;
					
					switch (choiceNumber)
					{
						case 1:
							this.registerNewGS();
							break;
						case 2:
							this.listGSNames();
							break;
						case 3:
							this.unregisterSingleGS();
							break;
						case 4:
							this.unregisterAllGS();
							break;
						case 5:
							System.exit(0);
							break;
						default:
							System.out.printf(this.getBundle().getString("invalidChoice")+'\n', choice);
							choiceOk = false;
					}
					
				}
				catch (NumberFormatException nfe)
				{
					System.out.printf(this.getBundle().getString("invalidChoice")+'\n', choice);
				}
			}
			while (!choiceOk);
		}
	}
	
	/**
     * 
     */
    private void hr()
    {
    	System.out.println("_____________________________________________________\n");
    }

	/**
	 * 
	 */
	private void listGSNames()
	{
		int idMaxLen = 0;
		int nameMaxLen = 0;
		for (Entry<Integer, String> e : GameServerTable.getInstance().getServerNames().entrySet())
		{
			if (e.getKey().toString().length() > idMaxLen)
			{
				idMaxLen = e.getKey().toString().length();
			}
			if (e.getValue().length() > nameMaxLen)
			{
				nameMaxLen = e.getValue().length();
			}
		}
		idMaxLen += 2;
		nameMaxLen += 2;
		
		String id;
		boolean inUse;
		String gsInUse = this.getBundle().getString("gsInUse");
		String gsFree = this.getBundle().getString("gsFree");
		int gsStatusMaxLen = Math.max(gsInUse.length(), gsFree.length()) + 2;
		for (Entry<Integer, String> e : GameServerTable.getInstance().getServerNames().entrySet())
		{
			id = e.getKey().toString();
			System.out.print(id);
			
			for (int i = id.length(); i < idMaxLen; i++)
			{
				System.out.print(' ');
			}
			System.out.print("| ");
			
			System.out.print(e.getValue());
			
			for (int i = e.getValue().length(); i < nameMaxLen; i++)
			{
				System.out.print(' ');
			}
			System.out.print("| ");
			
			inUse = GameServerTable.getInstance().hasRegisteredGameServerOnId(e.getKey());
			String inUseStr = (inUse ? gsInUse : gsFree);
			System.out.print(inUseStr);
			
			for (int i = inUseStr.length(); i < gsStatusMaxLen; i++)
			{
				System.out.print(' ');
			}
			System.out.println('|');
		}
	}
	
	/**
	 * @throws IOException 
	 * 
	 */
	private void unregisterAllGS() throws IOException
	{
		if (this.yesNoQuestion(this.getBundle().getString("confirmRemoveAllText")))
		{
			try
			{
				BaseGameServerRegister.unregisterAllGameServers();
				System.out.println(this.getBundle().getString("unregisterAllOk"));
			}
			catch (SQLException e)
			{
				this.showError(this.getBundle().getString("sqlErrorUnregisterAll"), e);
			}
		}
	}
	
	private boolean yesNoQuestion(String question) throws IOException
	{
		
		do
		{
			this.hr();
			System.out.println(question);
			System.out.println("1 - "+this.getBundle().getString("yes"));
			System.out.println("2 - "+this.getBundle().getString("no"));
			System.out.print(this.getBundle().getString("yourChoice")+' ');
			String choice;
			choice = _in.readLine();
			if (choice.equals("1"))
			{
				return true;
			}
			else if (choice.equals("2"))
			{
				return false;
			}
			else
			{
				System.out.printf(this.getBundle().getString("invalidChoice")+'\n', choice);
			}
		}
		while (true);
	}
	
	/**
	 * @throws IOException 
	 * 
	 */
	private void unregisterSingleGS() throws IOException
	{
		String line;
		int id = Integer.MIN_VALUE;
		
		do
		{
			System.out.print(this.getBundle().getString("enterDesiredId")+' ');
			line = _in.readLine();
			try
			{
				id = Integer.parseInt(line);
			}
			catch (NumberFormatException e)
			{
				System.out.printf(this.getBundle().getString("invalidChoice")+'\n', line);
			}
		}
		while (id == Integer.MIN_VALUE);
		
		String name = GameServerTable.getInstance().getServerNameById(id);
		if (name == null)
		{
			System.out.printf(this.getBundle().getString("noNameForId")+'\n', id);
		}
		else
		{
			if (GameServerTable.getInstance().hasRegisteredGameServerOnId(id))
			{
				System.out.printf(this.getBundle().getString("confirmRemoveText")+'\n', id, name);
				try
				{
					BaseGameServerRegister.unregisterGameServer(id);
					System.out.printf(this.getBundle().getString("unregisterOk")+'\n', id);
				}
				catch (SQLException e)
				{
					this.showError(this.getBundle().getString("sqlErrorUnregister"), e);
				}
				
			}
			else
			{
				System.out.printf(this.getBundle().getString("noServerForId")+'\n', id);
			}
		}
		
		
	}
	
	private void registerNewGS() throws IOException
	{
		String line;
		int id = Integer.MIN_VALUE;
		
		do
		{
			System.out.println(this.getBundle().getString("enterDesiredId"));
			line = _in.readLine();
			try
			{
				id = Integer.parseInt(line);
			}
			catch (NumberFormatException e)
			{
				System.out.printf(this.getBundle().getString("invalidChoice")+'\n', line);
			}
		}
		while (id == Integer.MIN_VALUE);
		
		
		String name = GameServerTable.getInstance().getServerNameById(id);
		if (name == null)
		{
			System.out.printf(this.getBundle().getString("noNameForId")+'\n', id);
		}
		else
		{
			if (GameServerTable.getInstance().hasRegisteredGameServerOnId(id))
			{
				System.out.println(this.getBundle().getString("idIsNotFree"));
			}
			else
			{
				try
				{
					BaseGameServerRegister.registerGameServer(id, ".");
				}
				catch (IOException e)
				{
					this.showError(getBundle().getString("ioErrorRegister"), e);
				}
			}
		}
	}
	
	/**
	 * @see net.sf.l2j.gsregistering.BaseGameServerRegister#showError(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void showError(String msg, Throwable t)
	{
		String title;
		if (this.getBundle() != null)
		{
			title = this.getBundle().getString("error");
			msg += '\n'+this.getBundle().getString("reason")+' '+t.getLocalizedMessage();
		}
		else
		{
			title = "Error";
			msg += "\nCause: "+t.getLocalizedMessage();
		}
		System.out.println(title+": "+msg);
	}
}