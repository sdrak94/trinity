package net.sf.l2j.gameserver.communitybbs.boards;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import javolution.text.TextBuilder;
import net.sf.l2j.L2DatabaseFactory;

public class CastleStatus
{
	private TextBuilder _playerList = new TextBuilder();
	
	public CastleStatus()
	{
		loadFromDB();
	}
	
	private void loadFromDB()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			for (int i = 1; i < 9; i++)
			{
				PreparedStatement statement = con.prepareStatement("SELECT clan_name, clan_level FROM clan_data WHERE hasCastle=" + i + ";");
				ResultSet result = statement.executeQuery();
				
				PreparedStatement statement2 = con.prepareStatement("SELECT name, siegeDate, taxPercent FROM castle WHERE id=" + i + ";");
				ResultSet result2 = statement2.executeQuery();
				
				while (result.next())
				{
					String owner = result.getString("clan_name");
					int level = result.getInt("clan_level");
					
					while (result2.next())
					{
						String name = result2.getString("name");
						long someLong = result2.getLong("siegeDate");
						int tax = result2.getInt("taxPercent");
						Date anotherDate = new Date(someLong);
						String DATE_FORMAT = "dd-MMM-yyyy HH:mm";
						SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
						
						addCastleToList(name, owner, level, tax, sdf.format(anotherDate));
					}
					
					result2.close();
					statement2.close();
				}
				
				result.close();
				statement.close();
			}
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
	
	private void addCastleToList(String name, String owner, int level, int tax, String siegeDate)
	{
		_playerList.append("<table border=0 cellspacing=0 cellpadding=2 width=750>");
		_playerList.append("<tr>");
		_playerList.append("<td FIXWIDTH=10></td>");
		_playerList.append("<td FIXWIDTH=100>" + name + "</td>");
		_playerList.append("<td FIXWIDTH=100>" + owner + "</td>");
		_playerList.append("<td FIXWIDTH=80>" + level + "</td>");
		_playerList.append("<td FIXWIDTH=40>" + tax + "</td>");
		_playerList.append("<td FIXWIDTH=180>" + siegeDate + "</td>");
		_playerList.append("<td FIXWIDTH=5></td>");
		_playerList.append("</tr>");
		_playerList.append("</table>");
		_playerList.append("<img src=\"L2UI.Squaregray\" width=\"740\" height=\"1\">");
	}
	
	public String loadCastleList()
	{
		return _playerList.toString();
	}
}