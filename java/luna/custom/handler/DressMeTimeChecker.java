package luna.custom.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import luna.custom.DressMeEngine.DressMeHandler;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;

public class DressMeTimeChecker
{
	private static DressMeTimeChecker	_instance	= null;
	SimpleDateFormat					dateFormat	= new SimpleDateFormat("HH:mm -- dd/MM/yyyy zzz");
	
	public static DressMeTimeChecker getInstance()
	{
		if (_instance == null)
			_instance = new DressMeTimeChecker();
		return _instance;
	}
	
	public void checkForDressTimers(L2PcInstance activeChar)
	{
		if (activeChar == null)
			return;
		int objId = activeChar.getObjectId();
		Map<Integer, Long> _dressMeExpiryDates = L2PcInstance.getDressMeExpiryDates();
		List<String> _toBeWiped = new ArrayList<>();
		StringBuilder sql = new StringBuilder("UPDATE items SET visual_item_id = 0,visual_item_limitedTime = 0 WHERE object_id IN(");
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT object_id,item_id,visual_item_id,visual_item_limitedTime FROM items where owner_id = ? and visual_item_limitedTime IS NOT NULL and visual_item_limitedTime > 0");
			statement.setInt(1, objId);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				int itemObjId = rset.getInt(1);
				int itemId = rset.getInt(2);
				int dressId = rset.getInt(3);
				long time = rset.getLong(4);
				Inventory inv = activeChar.getInventory();
				L2ItemInstance item = inv.getItemByObjectId(itemObjId);
				if (time < System.currentTimeMillis())
				{
					Date wipeTime = new Date(time);
					String itemName = ItemTable.getInstance().getTemplate(itemId).getName();
					String dressName = ItemTable.getInstance().getTemplate(dressId).getName();
					activeChar.sendMessage("[" + dressName + "]" + itemName + " armorset expired on: " + dateFormat.format(wipeTime));
					DressMeHandler.visuality(activeChar, item, 0, 0);
					_toBeWiped.add(String.valueOf(itemObjId));
				}
				else
				{
					Date wipeTime = new Date(time);
					String itemName = ItemTable.getInstance().getTemplate(itemId).getName();
					activeChar.sendMessage(itemName + " is set to expiry on: " + dateFormat.format(wipeTime));
					_dressMeExpiryDates.put(dressId, time);
					
					//_toBeWiped.add(String.valueOf(itemObjId));
					//DressMeHandler.visuality(activeChar, item, 0, 0);
				}
			}
			rset.next();
			rset.close();
			statement.close();
			if (_toBeWiped.size() != 0)
			{
				for (int i = 0; i < _toBeWiped.size(); i++)
				{
					sql.append(i == _toBeWiped.size() - 1 ? _toBeWiped.get(i) + ")" : _toBeWiped.get(i) + ",");
				}
				String state = String.valueOf(sql);
				statement = con.prepareStatement(state);
				statement.execute();
				statement.close();
				_toBeWiped.clear();
				activeChar.broadcastUserInfo();
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
				statement.close();
			}
			catch (Exception e)
			{}
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
}