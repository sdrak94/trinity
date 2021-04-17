package net.sf.l2j.gameserver.model.actor.instance;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;


public class L2TrinityDonationMerchantInstance extends L2MerchantInstance
{
	public static HashMap<String, Integer> _donatesToday = new HashMap<>();
	
	static
	{
		ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(() -> _donatesToday.clear(), 1800000, 1800000);
	}
	
	public L2TrinityDonationMerchantInstance(final int objectId, final L2NpcTemplate template)
	{	super(objectId, template);
	}
	
	@Override
	public String getHtmlPath(final int npcId, final int val)
	{	return "data/html/L2Trinity/DonationNpc/7074.htm";
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{	final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		if (actualCommand.equalsIgnoreCase("donation_take") && st.hasMoreTokens())
		{	String transaction_id = st.nextToken();
			if (transaction_id.startsWith("#"))
				transaction_id = transaction_id.substring(1);
			retrieveDonation(transaction_id.toUpperCase(), player);
		}
		else if (actualCommand.startsWith("paysafereq"))
		{	if (!incDonations(player))
			{	player.sendMessage("Your IP has exceeded the limit of requested donates for now, try again in 30 minutes.");
				return;
			}
			final String[] commands = command.split(" ");
			if (commands.length != 7)
			{	player.sendMessage("Something got fucked up. Try again...");
				return;
			}
			if (commands[1].length() + commands[2].length() + commands[3].length() + commands[4].length() != 16)
			{	player.sendMessage("Invalid paysafe ID. Try again...");
				return;
			}
			try
			{	Integer.parseInt(commands[1]);
				Integer.parseInt(commands[2]);
				Integer.parseInt(commands[3]);
				Integer.parseInt(commands[4]);
				Integer.parseInt(commands[5]);
			}
			catch (final Exception e)
			{	player.sendMessage("Invalid payafe ID.");
				return;
			}
			if (commands[6].indexOf('@') != -1)
			{	final File file = new File("./data/logs/PaysafeDonations.txt");
				try (PrintWriter out = new PrintWriter(new FileWriter(file, true)))
				{
					out.write("-New Donation: From " + player.getName() + " " + Util.formatDate(new Date(), "dd/MM/yyyy H:mm:ss") + "\r\n");
					out.write("     ID:    " + commands[1] + " " + commands[2] + " " + commands[3] + " " + commands[4] + "\r\n");
					out.write("     Amount:    " + commands[5] + "\r\n");
					out.write("     Email:    " + commands[6] + "\r\n");
					out.write("---------------------------------------------------------------\r\n");
					sendPage(player, 4);
					PaySafeGetAmountById(commands);
				}
				catch (final IOException e)
				{	player.sendMessage("Request failed");
					sendPage(player, 1);
				}
			}
			else
				sendPage(player, 1);
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	private static boolean incDonations(final L2PcInstance player)
	{	final Integer tries = _donatesToday.get(player.getIP());
		if (tries == null)
		{	_donatesToday.put(player.getIP(), 1);
			return true;
		}
		_donatesToday.put(player.getIP(), tries + 1);
		if (tries > 8)
		{	player.sendMessage("Stop spamming, administration informed!");
			ThreadPoolManager.getInstance().scheduleGeneral(() -> player.getClient().closeNow(), 3000);
		}
		return tries < 3;
	}
	
	private static boolean sendPage(final L2PcInstance player, final int code)
	{	switch (code)
		{	case 1:
				sendHtml(player, "7074-8.htm"); // general error
				return false;
			case 2:
				sendHtml(player, "7074-2.htm"); // enter transaction #
				return false;
			case 3:
				sendHtml(player, "7074-9.htm"); // already retrieved
				return false;
			case 4:
				sendHtml(player, "7074-6.htm"); // thank you
				return true;
		}
		return false;
	}
	
	private static void sendHtml(final L2PcInstance player, final String filename)
	{	final String content = HtmCache.getInstance().getHtmForce("data/html/L2Trinity/DonationNpc/" + filename);
		final NpcHtmlMessage tele = new NpcHtmlMessage(1);
		tele.setHtml(content);
		player.sendPacket(tele);
	}
	
	private static synchronized native void PaySafeGetAmountById(String[] ID);
	
	private final static String[] sqlCommands =
	{	"SELECT * FROM donations WHERE transactionID=?",
		"UPDATE donations SET retrieved=?, retriever_ip=?, retriever_acc=?, retriever_char=?, retrieval_date=?, hwid=? WHERE transactionID=?"
	};
	
	private synchronized static final boolean retrieveDonation(final String txn_id, final L2PcInstance player)
	{if (txn_id == null || player == null) return false;
	try (Connection con = L2DatabaseFactory.getInstance()
		    .getConnection(); PreparedStatement st = con.prepareStatement(sqlCommands[0]))
		{
		    st.setString(1, txn_id);
		    try (ResultSet rs = st.executeQuery())
		    {
		        if (!rs.next()) return sendPage(player, 1); // doesn't exist ?
		        if (rs.getString("retrieved")
		            .equalsIgnoreCase("true")) return sendPage(player, 3); // already claimed ?
		        try (PreparedStatement st2 = con.prepareStatement(sqlCommands[1]))
		        {
		            st2.setString(1, "true");
		            st2.setString(2, player.getIP());
		            st2.setString(3, player.getAccountName());
		            st2.setString(4, player.getName());
		            st2.setString(5, Util.formatDate(new Date(), "dd/MM/yyyy H:mm:ss"));
		            st2.setString(6, txn_id);
		            st2.setString(7, player.getHWID());
		            st2.executeUpdate();
		        }
		        final int amount = rs.getInt("amount");
		        if (amount < 1) return sendPage(player, 1); // something got fucked up!
		        player.addItem("Donation", 9901, amount, null, true);
		        return sendPage(player, 4); // thanks
		    }
		}
		catch (final Exception e)
		{
		    e.printStackTrace();
		}
		return sendPage(player, 1);
}
}