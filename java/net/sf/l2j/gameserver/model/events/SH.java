package net.sf.l2j.gameserver.model.events;

import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Item;

public class SH
{
	protected static final Logger _log = Logger.getLogger(SH.class.getName());
	private static String			_eventTitle		= new String();
	private static String			_eventDesc		= new String();
	private static String			_location		= new String();
	private static String			_announceName	= new String();
	private static boolean			_started		= false;
	private static L2Spawn			_npcSpawn;
	private static int				_npcId			= 0;
	private static int				_npcX			= 0;
	private static int				_npcY			= 0;
	private static int				_npcZ			= 0;
	private static int				_npcHeading		= 0;

	private static FastList<Items>	_items			= new FastList<Items>();
	private static FastList<Items>	_prizes			= new FastList<Items>();
	private static FastList<String>	_winners		= new FastList<String>();

	public static class Items
	{
		private final L2Item	_item;
		private final int		_count;

		public Items(L2Item item, int count)
		{
			_item = item;
			_count = count;
		}

		public L2Item getItem()
		{
			return _item;
		}

		public int getCount()
		{
			return _count;
		}
	}

	public static void announceToAll(String announce)
	{
		if (getAnnounceName().equals(""))
			setAnnounceName("Scavenger Hunt");

		Announcements.getInstance().announceToAll(announce);
	}

	public static void autoEnd(int minuts)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
			public void run()
			{
				endEvent();
			}
		}, 1000 * 60 * minuts);
	}

	public static void startEvent(L2PcInstance activeChar)
	{
		if (!canStart())
		{
			if (activeChar != null)
				activeChar.sendMessage("Event not set up propertly.");
			return;
		}

		if (_started)
			return;

		spawnEventNpc(activeChar != null ? activeChar : null);

		announceToAll(_eventTitle);
		announceToAll("Look for event NPC at " + _location + " for details!");

		_started = true;
	}

	public static boolean canStart()
	{
		if (_eventTitle.equals("") || _location.equals("") || _npcId == 0 || _npcX == 0 || _npcY == 0 || _npcZ == 0 || _items.size() == 0 || _prizes.size() == 0)
			return false;
		return true;
	}

	public static void checkPlayer(L2PcInstance activeChar)
	{
		int itemCount = 0;

		if (getWinners().contains(activeChar.getName()))
		{
			activeChar.sendMessage("You have already won in this event, cannot win again!");
			return;
		}
		else if (getWinners().size() >= getPrizes().size())
		{
			activeChar.sendMessage("Sorry, there has already been " + getWinners().size() + " winners.");
			return;
		}

		for (Items item : getItems())
		{
			if (item.getCount() <= activeChar.getInventory().getInventoryItemCount(item.getItem().getItemId(), -1))
				itemCount++;
		}

		if (itemCount == getItems().size())
			processWinner(activeChar);
		else
			activeChar.sendMessage("You do not have all the items requested!");
	}

	public static void processWinner(L2PcInstance activeChar)
	{
		if (activeChar == null || activeChar.isOnline() == 0)
			return;
		
		// takeItems(activeChar); still thinking about it
		Items item = getPrizes().get(getWinners().size());
		activeChar.addItem("Scavenger Hunt", item.getItem().getItemId(), item.getCount(), activeChar, true);
		activeChar.broadcastPacket(new MagicSkillUse(activeChar, activeChar, 5103, 1, 1196, 0));
		getWinners().add(activeChar.getName());
		announceToAll(activeChar.getName() + " got " + suffixTool(getWinners().size()) + " place!");

		if (getWinners().size() >= getPrizes().size())
			endEvent();
	}

	public static void endEvent()
	{
		unspawnEventNpc();
		announceToAll("The event is now over!");
		getWinners().clear();
		_started = false;
	}

	private static void spawnEventNpc(L2PcInstance activeChar)
	{
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(_npcId);

		try
		{
			_npcSpawn = new L2Spawn(tmpl);

			_npcSpawn.setLocx(_npcX);
			_npcSpawn.setLocy(_npcY);
			_npcSpawn.setLocz(_npcZ);
			_npcSpawn.setAmount(1);
			_npcSpawn.setHeading(_npcHeading);
			_npcSpawn.setRespawnDelay(1);

			_npcSpawn.init();
			_npcSpawn.getLastSpawn().getStatus().setCurrentHp(999999999);
			_npcSpawn.getLastSpawn().setTitle(_eventTitle);

			_npcSpawn.getLastSpawn()._isEventMobSH = true;
			_npcSpawn.getLastSpawn().isAggressive();
			_npcSpawn.getLastSpawn().decayMe();
			SpawnTable.getInstance().addNewSpawn(_npcSpawn, false);
			_npcSpawn.getLastSpawn().spawnMe(_npcSpawn.getLastSpawn().getX(), _npcSpawn.getLastSpawn().getY(), _npcSpawn.getLastSpawn().getZ());

			_npcSpawn.getLastSpawn().broadcastPacket(new MagicSkillUse(_npcSpawn.getLastSpawn(), _npcSpawn.getLastSpawn(), Config.TVTI_JOIN_NPC_SKILL, 1, 1, 1));
		}
		catch (Exception e)
		{
			if (activeChar == null)
				_log.severe("SH Engine[spawnEventNpc(exception: "+e);
			else
				_log.severe("SH Engine[spawnEventNpc(" + activeChar.getName() + ")]: exception: "+e);
		}
	}

	public static void showEventHtml(L2PcInstance eventPlayer, String objectId)
	{
		try
		{
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("<html><body>");

			replyMSG.append("<title>Scavanger Hunt Event</title>");
			replyMSG.append("<table width=\"300\"><tr>");
			replyMSG.append("<td>Current event...</td>");
			replyMSG.append("<tr></tr>");
			replyMSG.append("<td>    ... name: <font color=\"00FF00\">" + _eventTitle + "</font></td>");
			replyMSG.append("<tr></tr>");
			replyMSG.append("<td>    ... description: <font color=\"00FF00\">" + _eventDesc + "</font></td>");
			replyMSG.append("<tr></tr>");
			replyMSG.append("<td>    ... items to collect:</td>");
			for (Items i : getItems())
			{
				replyMSG.append("<tr></tr>");
				replyMSG.append("<td><font color=\"FFFFFF\">(" + i.getCount() + ") " + i.getItem().getName() + "</font></td>");
			}
			replyMSG.append("<tr></tr>");
			replyMSG.append("<td><center><button value=\"I have the items!\" action=\"bypass -h npc_" + objectId + "_sh_player_has_items\" width=150 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></td>");
			replyMSG.append("<tr></tr>");
			if (getWinners().size() == 0)
				replyMSG.append("<td>There are not current winners</td>");
			else
				replyMSG.append("<td>    ... current winners:</td>");
			for (int i = 0; i < getWinners().size(); i++)
			{
				replyMSG.append("<tr></tr>");
				replyMSG.append("<td><font color=\"FFFFFF\">[" + SH.suffixTool(i + 1) + "] " + getWinners().get(i) + "</font></td>");
			}

			replyMSG.append("<tr></tr>");
			replyMSG.append("<td>    ... prizes:</td>");
			for (int i = 0; i < getPrizes().size(); i++)
			{
				replyMSG.append("<tr></tr>");
				replyMSG.append("<td><font color=\"FFFFFF\">[" + SH.suffixTool(i + 1) + "] (" + SH.getPrizes().get(i).getCount() + ") " + SH.getPrizes().get(i).getItem().getName() + "</font></td>");
			}
			replyMSG.append("</tr></table>");

			replyMSG.append("</body></html>");
			adminReply.setHtml(replyMSG.toString());
			eventPlayer.sendPacket(adminReply);

			// Send a Server->Client ActionFailed to the L2PcInstance in order
			// to avoid that the client wait another packet
			eventPlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
		catch (Exception e)
		{
			_log.warning("SH Engine[showEventHtlm(" + eventPlayer.getName() + ", " + objectId + ")]: exception"+e);
		}
	}

	public static String suffixTool(int value)
	{
		String temp = String.valueOf(value);
		temp = temp.substring(temp.length() - 1);

		switch (Integer.parseInt(temp))
		{
			case 1:
				return value + "st";
			case 2:
				return value + "nd";
			case 3:
				return value + "rd";
			default:
				return value + "th";
		}
	}

	public static void addNewItem(int itemId, int count)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		addItem(new Items(item, count));
	}

	public static void remItem(int idx)
	{
		getItems().remove(idx);
	}

	public static void editItem(int idx, int itemId, int count)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		getItems().set(idx, new Items(item, count));
	}

	public static void addNewPrize(int itemId, int count)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		addPrize(new Items(item, count));
	}

	public static void remPrize(int idx)
	{
		getPrizes().remove(idx);
	}

	public static void editPrize(int idx, int itemId, int count)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		getPrizes().set(idx, new Items(item, count));
	}

	public static void unspawnEventNpc()
	{
		if (_npcSpawn == null)
			return;

		_npcSpawn.getLastSpawn().deleteMe();
		_npcSpawn.stopRespawn();
	}

	public static void setSpawn(int locX, int locY, int locZ)
	{
		_npcX = locX;
		_npcY = locY;
		_npcZ = locZ;
	}

	public static void setSpawn(L2PcInstance activeChar)
	{
		_npcX = activeChar.getX();
		_npcY = activeChar.getY();
		_npcZ = activeChar.getZ();
	}

	public static void setEventTitle(String title)
	{
		_eventTitle = title;
	}

	public static String getEventTitle()
	{
		return _eventTitle;
	}

	public static void setEventDesc(String desc)
	{
		_eventDesc = desc;
	}

	public static String getEventDesc()
	{
		return _eventDesc;
	}

	public static void setLocation(String Loc)
	{
		_location = Loc;
	}

	public static String getLocation()
	{
		return _location;
	}

	public static void setAnnounceName(String name)
	{
		_announceName = name;
	}

	public static String getAnnounceName()
	{
		return _announceName;
	}

	public static boolean isStarted()
	{
		return _started;
	}

	public static L2Spawn getJoinNpc()
	{
		return _npcSpawn;
	}

	public static void setNpcId(int npcId)
	{
		_npcId = npcId;
	}

	public static int getNpcId()
	{
		return _npcId;
	}

	public static int getNpcX()
	{
		return _npcX;
	}

	public static int getNpcY()
	{
		return _npcY;
	}

	public static int getNpcZ()
	{
		return _npcZ;
	}

	public static void addItem(Items item)
	{
		_items.add(item);
	}

	public static FastList<Items> getItems()
	{
		return _items;
	}

	public static void addPrize(Items item)
	{
		_prizes.add(item);
	}

	public static FastList<Items> getPrizes()
	{
		return _prizes;
	}

	public static void addWinner(String winner)
	{
		_winners.add(winner);
	}

	public static FastList<String> getWinners()
	{
		return _winners;
	}
}