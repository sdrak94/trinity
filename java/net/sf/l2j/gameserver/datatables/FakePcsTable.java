package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2ArmorSet;
import net.sf.l2j.gameserver.model.actor.FakePc;

public class FakePcsTable
{
private static Logger _log = Logger.getLogger(FakePcsTable.class.getName());

private FastMap< Integer, FakePc > _fakePcs = new FastMap< Integer, FakePc >();

public void load()
{
	_fakePcs = new FastMap< Integer, FakePc >();
	
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		PreparedStatement stmt = con.prepareStatement("SELECT * FROM `fake_pcs`");
		ResultSet rset = stmt.executeQuery();
		
		FakePc fpc = null;
		
		while ( rset.next() )
		{
			fpc = new FakePc();
			
			int npcId = rset.getInt("npc_id");
			fpc.race = rset.getInt("race");
			fpc.sex = rset.getInt("sex");
			fpc.title = rset.getString("title");
			fpc.titleColor = rset.getString("title_color");
			fpc.name = rset.getString("name");
			fpc.nameColor = rset.getString("name_color");
			fpc.hairStyle = rset.getInt("hair_style");
			fpc.hairColor = rset.getInt("hair_color");
			fpc.face = rset.getInt("face");
			fpc.mount = rset.getByte("mount");
			fpc.team = rset.getByte("team");
			fpc.armorset = rset.getInt("armorset");
			fpc.pdHead = rset.getInt("helm");
			fpc.pdRHand = rset.getInt("weapon");
			fpc.pdRHandAug = rset.getInt("weapon_aug");
			fpc.pdLHand = rset.getInt("shield");
			fpc.pdLHandAug = rset.getInt("shield_aug");
			fpc.pdGloves = rset.getInt("gloves");
			fpc.pdChest = rset.getInt("chest");
			fpc.pdLegs = rset.getInt("legs");
			fpc.pdFeet = rset.getInt("boots");
			fpc.pdBack = rset.getInt("back");
			fpc.pdHair = rset.getInt("hair");
			fpc.pdHair2 = rset.getInt("hair2");
			fpc.enchantEffect = rset.getByte("enchant_effect");
			fpc.hero = rset.getByte("hero");
			fpc.abnormal = rset.getInt("abnormal");
			fpc.special = rset.getInt("special");
			fpc.pvpFlag = rset.getInt("pvp_flag");
			fpc.karma = rset.getInt("karma");
			fpc.fishing = rset.getByte("fishing");
			fpc.fishingX = rset.getInt("fishing_x");
			fpc.fishingY = rset.getInt("fishing_y");
			fpc.fishingZ = rset.getInt("fishing_z");
			fpc.invisible = rset.getByte("invisible");
			fpc.clanid = rset.getInt("clanid");
			fpc.clancrestid = rset.getInt("clancrestid");
			fpc.allyid = rset.getInt("allyid");
			fpc.allycrestid = rset.getInt("allycrestid");
			
			if (fpc.armorset > 0)
			{
				final L2ArmorSet set = ArmorSetsTable.getInstance().getSet(fpc.armorset);
				
				if (set != null)
				{
					fpc.pdGloves = set._gloves;
					fpc.pdFeet = set._feet;
					fpc.pdChest = set._chest;
					fpc.pdLegs = set._legs;
				}
			}
			
			_fakePcs.put(npcId, fpc);
		}
		
		rset.close();
		stmt.close();
	}
	catch (SQLException e)
	{
		_log.warning("AccessFakePcsTable: Error loading from database:"+e);
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

private FakePcsTable()
{
	load();
}

public FakePc getFakePc(int npcId)
{
	return _fakePcs.get(npcId);
}

public static FakePcsTable getInstance()
{
	return SingletonHolder._instance;
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final FakePcsTable _instance = new FakePcsTable();
}
}