/*
 * L2jFrozen Project - www.l2jfrozen.com 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.datatables;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ExtractableItem2;
import net.sf.l2j.gameserver.model.L2ExtractableProductItem2;

/**
 * @author FBIagent
 */
public class ExtractableItemsData
{
	private static Logger LOGGER = Logger.getLogger(ExtractableItemsData.class);
	
	// Map<itemid, L2ExtractableItem>
	private Map<Integer, L2ExtractableItem2> _items;
	
	private static ExtractableItemsData _instance = null;
	
	public static ExtractableItemsData getInstance()
	{
		if (_instance == null)
		{
			_instance = new ExtractableItemsData();
		}
		
		return _instance;
	}
	
	public ExtractableItemsData()
	{
		_items = new HashMap<>();
		
		Scanner s = null;
		try
		{
			s = new Scanner(new File(Config.DATAPACK_ROOT + "/data/extractable_items.csv"));
			
			int lineCount = 0;
			while (s.hasNextLine())
			{
				lineCount++;
				
				final String line = s.nextLine();
				
				if (line.startsWith("#"))
				{
					continue;
				}
				else if (line.equals(""))
				{
					continue;
				}
				
				final String[] lineSplit = line.split(";");
				int itemID = 0;
				try
				{
					itemID = Integer.parseInt(lineSplit[0]);
				}
				catch (final Exception e)
				{
					
					e.printStackTrace();
					
					LOGGER.info("Extractable items data: Error in line " + lineCount + " -> invalid item id or wrong seperator after item id!");
					LOGGER.info("		" + line);
					return;
				}
				
				final List<L2ExtractableProductItem2> product_temp = new ArrayList<>(lineSplit.length);
				for (int i = 0; i < lineSplit.length - 1; i++)
				{
					String[] lineSplit2 = lineSplit[i + 1].split(",");
					if (lineSplit2.length != 3)
					{
						LOGGER.info("Extractable items data: Error in line " + lineCount + " -> wrong seperator!");
						LOGGER.info("		" + line);
						continue;
					}
					
					int production = 0, amount = 0, chance = 0;
					
					try
					{
						production = Integer.parseInt(lineSplit2[0]);
						amount = Integer.parseInt(lineSplit2[1]);
						chance = Integer.parseInt(lineSplit2[2]);
						lineSplit2 = null;
					}
					catch (final Exception e)
					{
						
							e.printStackTrace();
						
						LOGGER.info("Extractable items data: Error in line " + lineCount + " -> incomplete/invalid production data or wrong seperator!");
						LOGGER.info("		" + line);
						continue;
					}
					
					product_temp.add(new L2ExtractableProductItem2(production, amount, chance));
				}
				
				int fullChances = 0;
				for (final L2ExtractableProductItem2 Pi : product_temp)
				{
					fullChances += Pi.getChance();
				}
				
				if (fullChances > 100)
				{
					LOGGER.info("Extractable items data: Error in line " + lineCount + " -> all chances together are more then 100!");
					LOGGER.info("		" + line);
					continue;
				}
				
				_items.put(itemID, new L2ExtractableItem2(itemID, product_temp));
			}
			
			LOGGER.info("Extractable items data: Loaded " + _items.size() + " extractable items!");
		}
		catch (final Exception e)
		{
			// if(Config.ENABLE_ALL_EXCEPTIONS)
			e.printStackTrace();
			
			LOGGER.info("Extractable items data: Can not find './data/extractable_items.csv'");
			
		}
		finally
		{
			
			if (s != null)
				try
				{
					s.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
		}
	}
	
	public L2ExtractableItem2 getExtractableItem(final int itemID)
	{
		return _items.get(itemID);
	}
	
	public int[] itemIDs()
	{
		final int size = _items.size();
		final int[] result = new int[size];
		int i = 0;
		for (final L2ExtractableItem2 ei : _items.values())
		{
			result[i] = ei.getItemId();
			i++;
		}
		return result;
	}
}
