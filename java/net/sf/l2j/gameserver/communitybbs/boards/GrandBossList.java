package net.sf.l2j.gameserver.communitybbs.boards;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.text.TextBuilder;
import net.sf.l2j.L2DatabaseFactory;

public class GrandBossList
{
	private TextBuilder _GrandBossList = new TextBuilder();
	
	public GrandBossList()
	{
		loadFromDB();
	}
	
	private void loadFromDB()
	{
		Connection con = null;
		int pos = 0;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT boss_id, status FROM grandboss_data");
			ResultSet result = statement.executeQuery();
			
			nextnpc:
			while (result.next())
			{
				int npcid = result.getInt("boss_id");
				int status = result.getInt("status");
				if (npcid == 29066 || npcid == 29067 || npcid == 29068 || npcid == 29118)
					continue nextnpc;
				
				PreparedStatement statement2 = con.prepareStatement("SELECT name FROM npc WHERE id=" + npcid);
				ResultSet result2 = statement2.executeQuery();
				
				while (result2.next())
				{
					pos++;
					boolean rstatus = false;
					if (status == 0)
						rstatus = true;
					String npcname = result2.getString("name");
					addGrandBossToList(pos, npcname, rstatus);
				}
				result2.close();
				statement2.close();
			}
			
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
			
		}
	}
	
	private void addGrandBossToList(int pos, String npcname, boolean rstatus)
	{
		_GrandBossList.append("<table border=0 cellspacing=0 cellpadding=2>");
		_GrandBossList.append("<tr>");
		_GrandBossList.append("<td FIXWIDTH=5></td>");
		_GrandBossList.append("<td FIXWIDTH=50>" + pos + "</td>");
		_GrandBossList.append("<td FIXWIDTH=130>" + npcname + "</td>");
		_GrandBossList.append("<td FIXWIDTH=60 align=center>" + ((rstatus) ? "<font color=99FF00>Alive</font>" : "<font color=CC0000>Dead</font>") + "</td>");
		_GrandBossList.append("<td FIXWIDTH=5></td>");
		_GrandBossList.append("</tr>");
		_GrandBossList.append("</table>");
		_GrandBossList.append("<img src=\"L2UI.Squaregray\" width=\"250\" height=\"1\">");
	}
	
	public String loadGrandBossList()
	{
		return _GrandBossList.toString();
	}
}