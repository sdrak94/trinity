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
package net.sf.l2j.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;

public class L2PetDataTable
{
	private static Logger _log = Logger.getLogger(L2PetInstance.class.getName());
	
	//private static final int[] PET_LIST = { 12077, 12312, 12313, 12311, 12527, 12528, 12526 };
	private static Map<Integer, Map<Integer, L2PetData>> _petTable;
	
	public static L2PetDataTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private L2PetDataTable()
	{
		_petTable = new FastMap<Integer, Map<Integer, L2PetData>>();
	}
	
	public void loadPetsData()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT typeID, level, expMax, hpMax, mpMax, patk, pdef, matk, mdef, acc, evasion, crit, speed, atk_speed, cast_speed, feedMax, feedbattle, feednormal, loadMax, hpregen, mpregen, owner_exp_taken FROM pets_stats");
			ResultSet rset = statement.executeQuery();
			
			int petId, petLevel;
			
			while (rset.next())
			{
				petId = rset.getInt("typeID");
				petLevel = rset.getInt("level");
				
				//build the petdata for this level
				L2PetData petData = new L2PetData();
				petData.setPetID(petId);
				petData.setPetLevel(petLevel);
				petData.setPetMaxExp(rset.getLong("expMax"));
				petData.setPetMaxHP(rset.getInt("hpMax"));
				petData.setPetMaxMP(rset.getInt("mpMax"));
				petData.setPetPAtk(rset.getInt("patk"));
				petData.setPetPDef(rset.getInt("pdef"));
				petData.setPetMAtk(rset.getInt("matk"));
				petData.setPetMDef(rset.getInt("mdef"));
				petData.setPetAccuracy(rset.getInt("acc"));
				petData.setPetEvasion(rset.getInt("evasion"));
				petData.setPetCritical(rset.getInt("crit"));
				petData.setPetSpeed(rset.getInt("speed"));
				petData.setPetAtkSpeed(rset.getInt("atk_speed"));
				petData.setPetCastSpeed(rset.getInt("cast_speed"));
				petData.setPetMaxFeed(rset.getInt("feedMax"));
				petData.setPetFeedNormal(rset.getInt("feednormal"));
				petData.setPetFeedBattle(rset.getInt("feedbattle"));
				petData.setPetMaxLoad(rset.getInt("loadMax"));
				petData.setPetRegenHP(rset.getInt("hpregen"));
				petData.setPetRegenMP(rset.getInt("mpregen"));
				petData.setOwnerExpTaken(rset.getFloat("owner_exp_taken"));
				
				// if its the first data for this petid, we initialize its level FastMap
				if (!_petTable.containsKey(petId))
					_petTable.put(petId, new FastMap<Integer, L2PetData>());
				
				_petTable.get(petId).put(petLevel, petData);
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not load pets stats: " + e);
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
	
	public void addPetData(L2PetData petData)
	{
		Map<Integer, L2PetData> h = _petTable.get(petData.getPetID());
		
		if (h == null)
		{
			Map<Integer, L2PetData> statTable = new FastMap<Integer, L2PetData>();
			statTable.put(petData.getPetLevel(), petData);
			_petTable.put(petData.getPetID(), statTable);
			return;
		}
		
		h.put(petData.getPetLevel(), petData);
	}
	
	public void addPetData(L2PetData[] petLevelsList)
	{
		for (L2PetData petData : petLevelsList)
			addPetData(petData);
	}
	
	public L2PetData getPetData(int petID, int petLevel)
	{
		//_log.info("Getting id "+petID+" level "+ petLevel);
		return _petTable.get(petID).get(petLevel);
	}
	
	/**
	 * Pets stuffs
	 */
	public static boolean isWolf(int npcId)
	{
		return npcId == 12077;
	}
	
	public static boolean isEvolvedWolf(int npcId)
	{
		return npcId == 16030 || npcId == 16037 || npcId == 16025 || npcId == 16041 || npcId == 16042;
	}
	
	public static boolean isSinEater(int npcId)
	{
		return npcId == 12564;
	}
	
	public static boolean isHatchling(int npcId)
	{
		return npcId > 12310 && npcId < 12314;
	}
	
	public static boolean isStrider(int npcId)
	{
		return (npcId > 12525 && npcId < 12529) || (npcId > 16037 && npcId < 16041);
	}
	
	public static boolean isWyvern(int npcId)
	{
		return npcId == 12621;
	}
	
	public static boolean isBaby(int npcId)
	{
		return npcId > 12779 && npcId < 12783;
	}
	
	public static boolean isImprovedBaby(int npcId)
	{
		return npcId > 16033 && npcId < 16037;
	}
	
	public static boolean isPetFood(int itemId)
	{
		switch (itemId)
		{
			case 2515:
			case 4038:
			case 5168:
			case 5169:
			case 6316:
			case 7582:
			case 9668:
			case 10425:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	public static boolean isWolfFood(int itemId)
	{
		return itemId == 2515;
	}
	
	public static boolean isEvolvedWolfFood(int itemId)
	{
		return itemId == 9668;
	}
	
	public static boolean isSinEaterFood(int itemId)
	{
		return itemId == 2515;
	}
	
	public static boolean isHatchlingFood(int itemId)
	{
		return itemId == 4038;
	}
	
	public static boolean isStriderFood(int itemId)
	{
		return itemId == 5168 || itemId == 5169;
	}
	
	public static boolean isWyvernFood(int itemId)
	{
		return itemId == 6316;
	}
	
	public static boolean isBabyFood(int itemId)
	{
		return itemId == 7582;
	}
	
	public static boolean isImprovedBabyFood(int itemId)
	{
		return itemId == 10425;
	}
	
	public static int[] getFoodItemId(int npcId)
	{
		switch (npcId)
		{
			case 12077:// Wolf
			case 12564://Sin Eater
				return new int[] { 2515 };
				
			case 16030:// Great Wolf 
			case 16025:// Black Wolf
			case 16037:// White Great Wolf	
			case 16041:// Fenrir  
			case 16042:// White Fenrir  
				return new int[] { 9668 };
				
			case 12311:// hatchling of wind
			case 12312:// hatchling of star
			case 12313:// hatchling of twilight
				return new int[] { 4038 };
				
			case 12526:// wind strider
			case 12527:// Star strider
			case 12528:// Twilight strider
			case 16038:// red wind strider
			case 16039:// red Star strider
			case 16040:// red Twilight strider
				return new int[] { 5168, 5169 };
				
			case 12621: // wyvern
				return new int[] { 6316 };
				
			case 12780:// Baby Buffalo
			case 12782:// Baby Cougar
			case 12781:// Baby Kookaburra
			
				return new int[] { 7582 };
			case 16034:// Improved Baby Buffalo
			case 16036:// Improved Baby Cougar	
			case 16035:// Improved Baby Kookaburra
				return new int[] { 10425 };
				
			default:
				return new int[] { 0 };
		}
	}
	
	public static boolean isPetItem(int itemId)
	{
		switch (itemId)
		{
			case 2375: // Wolf
			case 3500: // hatchling of wind
			case 3501: // hatchling of star
			case 3502: // hatchling of twilight
			case 4422: // strider of wind
			case 4423: // strider of star
			case 4424: // strider of dusk
			case 4425: // Sin Eater
			case 6648: // baby buffalo
			case 6649: // baby cougar
			case 6650: // baby kookaburra
			case 8663: // Wyvern
			case 9882: // Great Wolf
			case 10163: // Black Wolf
			case 10307: // Great Snow Wolf
			case 10308: // red strider of wind
			case 10309: // red strider of star
			case 10310: // red strider of dusk
			case 10311: // improved buffalo
			case 10312: // improved cougar
			case 10313: // improved kookaburra
			case 10426: // Fenrir
			case 10611: // White Fenrir
			
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	public static int[] getPetItemsByNpc(int npcId)
	{
		switch (npcId)
		{
			case 12077:// Wolf
				return new int[] { 2375 };
			case 16025:// Great Wolf
				return new int[] { 9882 };
			case 16030:// Black Wolf 
				return new int[] { 10163 };
			case 16037:// White Great Wolf 
				return new int[] { 10307 };
			case 16041:// Fenrir 
				return new int[] { 10426 };
			case 16042:// White Fenrir 
				return new int[] { 10611 };
			case 12564://Sin Eater
				return new int[] { 4425 };
				
			case 12311:// hatchling of wind
			case 12312:// hatchling of star
			case 12313:// hatchling of twilight
				return new int[] { 3500, 3501, 3502 };
				
			case 12526:// wind strider
			case 12527:// Star strider
			case 12528:// Twilight strider
			case 16038: // red strider of wind
			case 16039: // red strider of star
			case 16040: // red strider of dusk
				return new int[] { 4422, 4423, 4424, 10308, 10309, 10310 };
				
			case 12621:// Wyvern
				return new int[] { 8663 };
				
			case 12780:// Baby Buffalo
			case 12782:// Baby Cougar
			case 12781:// Baby Kookaburra
				return new int[] { 6648, 6649, 6650 };
				
			case 16034:// Improved Baby Buffalo
			case 16036:// Improved Baby Cougar
			case 16035:// Improved Baby Kookaburra	
				return new int[] { 10311, 10312, 10313 };
				
				// unknown item id.. should never happen
			default:
				return new int[] { 0 };
		}
	}
	
	public static boolean isMountable(int npcId)
	{
		return npcId == 12526 // wind strider
				|| npcId == 12527 // star strider
				|| npcId == 12528 // twilight strider
				|| npcId == 12621 // wyvern
				|| npcId == 16037 // Great Snow Wolf
				|| npcId == 16041 // Fenrir Wolf
				|| npcId == 16042 // White Fenrir Wolf
				|| npcId == 16038 // Red Wind Strider
				|| npcId == 16039 // Red Star Strider
				|| npcId == 16040; // Red Twilight Strider
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final L2PetDataTable _instance = new L2PetDataTable();
	}
}