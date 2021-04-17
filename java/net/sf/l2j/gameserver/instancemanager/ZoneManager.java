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

package net.sf.l2j.gameserver.instancemanager;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.form.ZoneCuboid;
import net.sf.l2j.gameserver.model.zone.form.ZoneCylinder;
import net.sf.l2j.gameserver.model.zone.form.ZoneNPoly;
import net.sf.l2j.gameserver.model.zone.type.L2ArenaZone;
import net.sf.l2j.gameserver.model.zone.type.L2BigheadZone;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.model.zone.type.L2CastleTeleportZone;
import net.sf.l2j.gameserver.model.zone.type.L2CastleZone;
import net.sf.l2j.gameserver.model.zone.type.L2ChaoticZone;
import net.sf.l2j.gameserver.model.zone.type.L2ClanHallZone;
import net.sf.l2j.gameserver.model.zone.type.L2DamageZone;
import net.sf.l2j.gameserver.model.zone.type.L2DerbyTrackZone;
import net.sf.l2j.gameserver.model.zone.type.L2EventZone;
import net.sf.l2j.gameserver.model.zone.type.L2FarmZone;
import net.sf.l2j.gameserver.model.zone.type.L2FishingZone;
import net.sf.l2j.gameserver.model.zone.type.L2FortZone;
import net.sf.l2j.gameserver.model.zone.type.L2GeneralZone;
import net.sf.l2j.gameserver.model.zone.type.L2HqZone;
import net.sf.l2j.gameserver.model.zone.type.L2JailZone;
import net.sf.l2j.gameserver.model.zone.type.L2KoreanZone;
import net.sf.l2j.gameserver.model.zone.type.L2LandingZone;
import net.sf.l2j.gameserver.model.zone.type.L2MotherTreeZone;
import net.sf.l2j.gameserver.model.zone.type.L2NoHqZone;
import net.sf.l2j.gameserver.model.zone.type.L2NoLandingZone;
import net.sf.l2j.gameserver.model.zone.type.L2NoStoreZone;
import net.sf.l2j.gameserver.model.zone.type.L2OlympiadStadiumZone;
import net.sf.l2j.gameserver.model.zone.type.L2PaganZone;
import net.sf.l2j.gameserver.model.zone.type.L2PeaceZone;
import net.sf.l2j.gameserver.model.zone.type.L2PoisonZone;
import net.sf.l2j.gameserver.model.zone.type.L2PrimevalZone;
import net.sf.l2j.gameserver.model.zone.type.L2RaidZone;
import net.sf.l2j.gameserver.model.zone.type.L2ScriptZone;
import net.sf.l2j.gameserver.model.zone.type.L2SwampZone;
import net.sf.l2j.gameserver.model.zone.type.L2TownZone;
import net.sf.l2j.gameserver.model.zone.type.L2WaterZone;

/**
 * This class manages the augmentation data and can also create new
 * augmentations.
 * 
 * @author durgus
 */
public class ZoneManager
{
private static final Logger _log = Logger.getLogger(ZoneManager.class.getName());

private final FastMap<Integer, L2ZoneType> _zones = new FastMap<Integer,L2ZoneType>();
private List<L2ItemInstance> _debugItems;

public static final ZoneManager getInstance()
{
	return SingletonHolder._instance;
}

// =========================================================
// Data Field

// =========================================================
// Constructor
private ZoneManager()
{
	load();
}

public void reload()
{
	// int zoneCount = 0;
	
	// Get the world regions
	int count = 0;
	L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();
	for (L2WorldRegion[] worldRegion : worldRegions)
	{
		for (int y = 0; y < worldRegion.length; y++)
		{
			worldRegion[y].getZones().clear();
			count++;
		}
	}
	GrandBossManager.getInstance().getZones().clear();
	_log.info("Removed zones in " + count + " regions.");
	// Load the zones
	load();
}

// =========================================================
// Method - Private

private final void load()
{
	_log.info("Loading zones...");
	Connection con = null;
	int zoneCount = 0;
	_zones.clear();
	
	// Get the world regions
	L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();
	
	// Load the zone xml
	try
	{
		// Get a sql connection here
		con = L2DatabaseFactory.getInstance().getConnection();
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		
		File file = new File(Config.DATAPACK_ROOT + "/data/zones/zone.xml");
		if (!file.exists())
		{
			if (Config.DEBUG)
				_log.info("The zone.xml file is missing.");
			return;
		}
		
		Document doc = factory.newDocumentBuilder().parse(file);
		
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("zone".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						int zoneId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						int minZ = Integer.parseInt(attrs.getNamedItem("minZ").getNodeValue());
						int maxZ = Integer.parseInt(attrs.getNamedItem("maxZ").getNodeValue());
						String zoneType = attrs.getNamedItem("type").getNodeValue();
						String zoneShape = attrs.getNamedItem("shape").getNodeValue();
						
						// Create the zone
						L2ZoneType temp = null;
						
						if (zoneType.equals("FishingZone"))
							temp = new L2FishingZone(zoneId);
						else if (zoneType.equals("ClanHallZone"))
							temp = new L2ClanHallZone(zoneId);
						else if (zoneType.equals("PeaceZone"))
							temp = new L2PeaceZone(zoneId);
						else if (zoneType.equals("Town"))
							temp = new L2TownZone(zoneId);
						else if (zoneType.equals("OlympiadStadium"))
							temp = new L2OlympiadStadiumZone(zoneId);
						else if (zoneType.equals("CastleZone"))
							temp = new L2CastleZone(zoneId);
						else if (zoneType.equals("CastleTeleportZone"))
							temp = new L2CastleTeleportZone(zoneId);
						else if (zoneType.equals("FortZone"))
							temp = new L2FortZone(zoneId);
						else if (zoneType.equals("DamageZone"))
							temp = new L2DamageZone(zoneId);
						else if (zoneType.equals("PoisonZone"))
							temp = new L2PoisonZone(zoneId);
						else if (zoneType.equals("SwampZone"))
							temp = new L2SwampZone(zoneId);
						else if (zoneType.equals("Arena"))
							temp = new L2ArenaZone(zoneId);
						else if (zoneType.equals("MotherTree"))
							temp = new L2MotherTreeZone(zoneId);
						else if (zoneType.equals("BigheadZone"))
							temp = new L2BigheadZone(zoneId);
						else if (zoneType.equals("LandingZone"))
							temp = new L2LandingZone(zoneId);
						else if (zoneType.equals("NoLandingZone"))
							temp = new L2NoLandingZone(zoneId);
						else if (zoneType.equals("JailZone"))
							temp = new L2JailZone(zoneId);
						else if (zoneType.equals("DerbyTrackZone"))
							temp = new L2DerbyTrackZone(zoneId);
						else if (zoneType.equals("BossZone"))
							temp = new L2BossZone(zoneId);
						else if (zoneType.equals("WaterZone"))
							temp = new L2WaterZone(zoneId);
						else if (zoneType.equals("NoStoreZone"))
							temp = new L2NoStoreZone(zoneId);
						else if (zoneType.equals("GeneralZone"))
							temp = new L2GeneralZone(zoneId);
						else if (zoneType.equals("ScriptZone"))
							temp = new L2ScriptZone(zoneId);
						else if (zoneType.equals("PaganZone"))
							temp = new L2PaganZone(zoneId);
						else if (zoneType.equals("NoHqZone"))
							temp = new L2NoHqZone(zoneId);
						else if (zoneType.equals("ChaoticZone"))
							temp = new L2ChaoticZone(zoneId);
						else if (zoneType.equals("HqZone"))
							temp = new L2HqZone(zoneId);
						else if (zoneType.equals("FarmZone"))
							temp = new L2FarmZone(zoneId);
						else if (zoneType.equals("EventZone"))
							temp = new L2EventZone(zoneId);
						else if (zoneType.equals("RaidZone"))
							temp = new L2RaidZone(zoneId);
						else if (zoneType.equals("PrimevalZone"))
							temp = new L2PrimevalZone(zoneId);
						else if (zoneType.equals("KoreanZone"))
							temp = new L2KoreanZone(zoneId);
						
						// Check for unknown type
						if (temp == null)
						{
							_log.warning("ZoneData: No such zone type: " + zoneType);
							continue;
						}
						
						// Get the zone shape from sql
						try
						{
							PreparedStatement statement = null;
							
							// Set the correct query
							statement = con.prepareStatement("SELECT x,y FROM zone_vertices WHERE id=? ORDER BY 'order' ASC ");
							
							statement.setInt(1, zoneId);
							ResultSet rset = statement.executeQuery();
							
							// Create this zone. Parsing for cuboids is a
							// bit different than for other polygons
							// cuboids need exactly 2 points to be defined.
							// Other polygons need at least 3 (one per
							// vertex)
							if (zoneShape.equalsIgnoreCase("Cuboid"))
							{
								int[] x = { 0, 0 };
								int[] y = { 0, 0 };
								boolean successfulLoad = true;
								
								for (int i = 0; i < 2; i++)
								{
									if (rset.next())
									{
										x[i] = rset.getInt("x");
										y[i] = rset.getInt("y");
									}
									else
									{
										_log.warning("ZoneData: Missing cuboid vertex in sql data for zone: " + zoneId);
										rset.close();
										statement.close();
										successfulLoad = false;
										break;
									}
								}
								
								if (successfulLoad)
									temp.setZone(new ZoneCuboid(x[0], x[1], y[0], y[1], minZ, maxZ));
								else
									continue;
							}
							else if (zoneShape.equalsIgnoreCase("NPoly"))
							{
								FastList<Integer> fl_x = new FastList<Integer>(), fl_y = new FastList<Integer>();
								
								// Load the rest
								while (rset.next())
								{
									fl_x.add(rset.getInt("x"));
									fl_y.add(rset.getInt("y"));
								}
								
								// An nPoly needs to have at least 3
								// vertices
								if ((fl_x.size() == fl_y.size()) && (fl_x.size() > 2))
								{
									// Create arrays
									int[] aX = new int[fl_x.size()];
									int[] aY = new int[fl_y.size()];
									
									// This runs only at server startup so
									// dont complain :>
									for (int i = 0; i < fl_x.size(); i++)
									{
										aX[i] = fl_x.get(i);
										aY[i] = fl_y.get(i);
									}
									
									// Create the zone
									temp.setZone(new ZoneNPoly(aX, aY, minZ, maxZ));
								}
								else
								{
									_log.warning("ZoneData: Bad sql data for zone: " + zoneId);
									rset.close();
									statement.close();
									continue;
								}
							}
							else if (zoneShape.equalsIgnoreCase("Cylinder"))
							{
								// A Cylinder zone requires a centre point
								// at x,y and a radius
								int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
								if (rset.next() && zoneRad > 0)
								{
									int zoneX = rset.getInt("x");
									int zoneY = rset.getInt("y");
									
									// create the zone
									temp.setZone(new ZoneCylinder(zoneX, zoneY, minZ, maxZ, zoneRad));
								}
								else
								{
									_log.warning("ZoneData: Bad sql data for zone: " + zoneId);
									rset.close();
									statement.close();
									continue;
								}
							}
							else
							{
								_log.warning("ZoneData: Unknown shape: " + zoneShape);
								rset.close();
								statement.close();
								continue;
							}
							
							rset.close();
							statement.close();
						}
						catch (Exception e)
						{
							_log.warning("ZoneData: Failed to load zone coordinates: " + e);
						}
						
						// Check for aditional parameters
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("stat".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								String name = attrs.getNamedItem("name").getNodeValue();
								String val = attrs.getNamedItem("val").getNodeValue();
								
								temp.setParameter(name, val);
							}
						}
						addZone(zoneId, temp);
						
						// Register the zone into any world region it
						// intersects with...
						// currently 11136 test for each zone :>
						int ax, ay, bx, by;
						for (int x = 0; x < worldRegions.length; x++)
						{
							for (int y = 0; y < worldRegions[x].length; y++)
							{
								ax = (x - L2World.OFFSET_X) << L2World.SHIFT_BY;
								bx = ((x + 1) - L2World.OFFSET_X) << L2World.SHIFT_BY;
								ay = (y - L2World.OFFSET_Y) << L2World.SHIFT_BY;
								by = ((y + 1) - L2World.OFFSET_Y) << L2World.SHIFT_BY;
								
								if (temp.getZone().intersectsRectangle(ax, bx, ay, by))
								{
									if (Config.DEBUG)
									{
										_log.info("Zone (" + zoneId + ") added to: " + x + " " + y);
									}
									worldRegions[x][y].addZone(temp);
								}
							}
						}
						
						// Special managers for granbosses...
						if (temp instanceof L2BossZone)
							GrandBossManager.getInstance().addZone((L2BossZone) temp);
						
						// Increase the counter
						zoneCount++;
					}
				}
			}
		}
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Error while loading zones.", e);
		return;
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
	
	_log.info("Done: loaded " + zoneCount + " zones.");
}

/**
 * Add new zone
 *
 * @param zone
 */
public void addZone(Integer id,L2ZoneType zone)
{
	_zones.put(id, zone);
}

/**
 * Returns all zones registered with the ZoneManager.
 * To minimise iteration processing retrieve zones from L2WorldRegion for a specific location instead.
 * @return zones
 */
public Collection<L2ZoneType> getAllZones()
{
	return _zones.values();
}

public L2ZoneType getZoneById( int id)
{
	return _zones.get(id);
}

/**
 * Returns all zones from where the object is located
 *
 * @param object
 * @return zones
 */
public FastList<L2ZoneType> getZones(L2Object object)
{
	return getZones(object.getX(), object.getY(), object.getZ());
}

/**
 * Returns all zones from given coordinates (plane)
 * 
 * @param x
 * @param y
 * @return zones
 */
public FastList<L2ZoneType> getZones(int x, int y)
{
	L2WorldRegion region = L2World.getInstance().getRegion(x, y);
	FastList<L2ZoneType> temp = new FastList<L2ZoneType>();
	for (L2ZoneType zone : region.getZones())
	{
		if (zone.isInsideZone(x, y))
			temp.add(zone);
	}
	return temp;
}

/**
 * Returns all zones from given coordinates
 *
 * @param x
 * @param y
 * @param z
 * @return zones
 */
public FastList<L2ZoneType> getZones(int x, int y, int z)
{
	L2WorldRegion region = L2World.getInstance().getRegion(x, y);
	FastList<L2ZoneType> temp = new FastList<L2ZoneType>();
	for (L2ZoneType zone : region.getZones())
	{
		if (zone.isInsideZone(x, y, z))
			temp.add(zone);
	}
	return temp;
}

public final L2ArenaZone getArena(L2Character character)
{
	for (L2ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), character.getZ()))
	{
		if (temp instanceof L2ArenaZone && temp.isCharacterInZone(character))
			return ((L2ArenaZone) temp);
	}
	
	return null;
}

public final L2OlympiadStadiumZone getOlympiadStadium(L2Character character)
{
	for (L2ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), character.getZ()))
	{
		if (temp instanceof L2OlympiadStadiumZone && temp.isCharacterInZone(character))
			return ((L2OlympiadStadiumZone) temp);
	}
	return null;
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final ZoneManager _instance = new ZoneManager();
}

/**
 * General storage for debug items used for visualizing zones.
 * @return list of items
 */
public List<L2ItemInstance> getDebugItems()
{
	if (_debugItems == null)
		_debugItems = new ArrayList<>();
	return _debugItems;
}

/**
 * Remove all debug items from l2world
 */
public void clearDebugItems()
{
	if (_debugItems != null)
	{
		final Iterator<L2ItemInstance> it = _debugItems.iterator();
		while (it.hasNext())
		{
			final L2ItemInstance item = it.next();
			if (item != null)
				item.decayMe();
			it.remove();
		}
	}
}
}