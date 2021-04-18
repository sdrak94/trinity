package custom.AugmentShop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;

/**
 * 
 * @author Rizel
 *
 */
public class AugmentShop extends Quest
{
	private final static int ITEM_ID = 57;
	private final static int ITEM_COUNT = 1000000;
	private final static String qn = "AugmentShop";
	private final static int NPC = 105987;
	
	public AugmentShop(int questId, String name, String descr) 
	{
		super(questId, name, descr);
		addFirstTalkId(NPC);
		addStartNpc(NPC);
		addTalkId(NPC);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = "";
		
		if (event.equalsIgnoreCase("active"))
		{
			htmltext = "active.htm";
		}
		
		else if (event.equalsIgnoreCase("passive"))
		{
			htmltext = "passive.htm";
		}
		
		else if (event.equalsIgnoreCase("chance"))
		{
			htmltext = "chance.htm";
		}
		
		else
		{
			
			updateAugment(player, Integer.parseInt(event.substring(0,5)), Integer.parseInt(event.substring(6,10)), Integer.parseInt(event.substring(11,13)));
		}

		
		return htmltext;
	}

	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = "";
		QuestState qs = player.getQuestState(qn);
		if (qs == null)
			qs = newQuestState(player);
		htmltext = "main.htm";
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new AugmentShop(-1, qn, "AugmentShop");
	}
	
	
	private static void updateAugment(L2PcInstance player, int attribute, int skill, int level)
	{
		L2ItemInstance item = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND) == null)
			
		{
			player.sendMessage("You have to equip a weapon.");
			return;
		}
			
		if (player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND).isAugmented())
		{
			player.sendMessage("The weapon is already augmented.");
			return;
		}
		
		if (player.getInventory().getInventoryItemCount(ITEM_ID, -1) < ITEM_COUNT)
		{
			player.sendMessage("You dont have enough item.");
			return;
		}
			
		Connection con = null;
		try
		{
			player.destroyItemByItemId("Consume", ITEM_ID, ITEM_COUNT, player, true);
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("REPLACE INTO item_attributes VALUES(?,?,?,?,?,?)");
			statement.setInt(1, item.getObjectId());

				statement.setInt(2, attribute*65536+1);
				statement.setInt(3, skill);
				statement.setInt(4, level);
			
			if (item.getElementals() == null)
			{
				statement.setByte(5, (byte) -1);
				statement.setInt(6, -1);
			}
			else
			{
				statement.setByte(5, item.getElementals().getElement());
				statement.setInt(6, item.getElementals().getValue());
			}
			statement.executeUpdate();
			player.broadcastUserInfo();
			player.sendMessage("Succesfully augmented. You have to relog now.");
			statement.close();
			
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not augment item: "+item.getObjectId()+" ", e);
		}
		finally
		{
			//L2DatabaseFactory.close(con);
		}
	}
	
	
}