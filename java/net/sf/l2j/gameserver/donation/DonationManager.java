package net.sf.l2j.gameserver.donation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;

import com.sun.mail.imap.IMAPFolder;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;

public final class DonationManager
{
	protected final Logger				LOGGER			= Logger.getLogger(DonationManager.class.getName());
	// PAYPAL
	private Map<Integer, PaypalData>	_paypal_data	= new HashMap<>();
	private Map<String, Integer>		_paypal_link	= new HashMap<>();
	private Map<Integer, String>		_paypal_queue	= new HashMap<>();
	// COUNT
	private Map<Integer, Integer>		_purchase_count	= new HashMap<>();
	
	DonationManager()
	{
		loadData();
		if (Config.ENABLE_DONATION_CHECKER)
		{
			if (!Config.GMAIL_ADDRESS.isEmpty() && !Config.GMAIL_PASSWORD.isEmpty())
			{
				ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new DonationTask(), Config.DONATION_CHECKER_INITIAL_DELAY, Config.DONATION_CHECKER_INTERVAL);
			}
		}
	}
	
	private void loadData()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("SELECT * FROM donation_records"))
			{
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						int id = rs.getInt("id");
						String transaction_id = rs.getString("transaction_id");
						String transaction_date = rs.getString("transaction_date");
						String email = rs.getString("email");
						Double amount = rs.getDouble("amount");
						String currency = rs.getString("currency");
						int receiver_id = rs.getInt("receiver_id");
						String receiver_name = rs.getString("receiver_name");
						long claimed_date = rs.getLong("claimed_date");
						PaypalData donation = new PaypalData(id, transaction_id, transaction_date, email, amount, currency, receiver_id, receiver_name, claimed_date);
						_paypal_data.put(id, donation);
						_paypal_link.put(transaction_id, id);
					}
				}
			}
			try (PreparedStatement ps = con.prepareStatement("SELECT * FROM donation_purchase_count"))
			{
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						int id = rs.getInt("id");
						int count = rs.getInt("count");
						_purchase_count.put(id, count);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		_generated_paypal_list = generatePaypalList();
		LOGGER.warning(getClass().getSimpleName() + ": Loaded " + _paypal_data.size() + " paypal records.");
	}
	
	public void queueTransactionId(L2PcInstance player, String transaction_id)
	{
		if (_paypal_queue.containsKey(player.getObjectId()))
		{
			player.sendPacket(new ExShowScreenMessage("You are already in queue.", 5000));
		}
		else if (_paypal_queue.containsValue(transaction_id))
		{
			player.sendPacket(new ExShowScreenMessage("Your Transaction ID was placed in a queue by another person.", 5000));
		}
		else if (_paypal_link.containsKey(transaction_id))
		{
			receivePaypalDonation(player, transaction_id);
		}
		else
		{
			player.sendPacket(new ExShowScreenMessage("Unverified Transaction ID. You have been placed in a queue.", 5000));
			_paypal_queue.put(player.getObjectId(), transaction_id);
		}
	}
	
	public void receivePaypalDonation(L2PcInstance player, String transaction_id)
	{
		final int id = _paypal_link.get(transaction_id);
		final PaypalData donation = _paypal_data.get(id);
		if (donation.isClaimed())
		{
			player.sendPacket(new ExShowScreenMessage("Transaction ID has already been used.", 5000));
			return;
		}
		final long time = System.currentTimeMillis();
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("UPDATE donation_records SET receiver_id=?,receiver_name=?,claimed_date=? WHERE transaction_id=?"))
			{
				ps.setInt(1, player.getObjectId());
				ps.setString(2, player.getName());
				ps.setLong(3, time);
				ps.setString(4, transaction_id);
				ps.executeUpdate();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
		donation.setReceiverId(player.getObjectId());
		donation.setReceiverName(player.getName());
		donation.setClaimed(time);
		player.sendMessage("We would like to express our gratitude for your donation.");
		// player.increaseGamePoints(Math.round(donation.getAmount() * 1000));

		player.addItem("Donation", L2Item.DONATION_TOKEN, (long) donation.getAmount(), player, true);
		long bonus = calculateBonus(donation.getAmount());
		if (bonus > 0)
		{
			player.sendMessage("Here is your bonus :)");

			player.addItem("Donation", L2Item.DONATION_TOKEN, bonus, player, true);
		}
		player.sendPacket(new ExShowScreenMessage("We would like to express our gratitude for your donation.", 5000));
	}
	
	private long calculateBonus(double r)
	{
		if (r >= 25 && r < 50)
		{
			r *= 0.10; // 10%
		}
		else if (r >= 50 && r < 100)
		{
			r *= 0.20; // 20%
		}
		else if (r >= 100 && r < 150)
		{
			r *= 0.25; // 25%
		}
		else if (r >= 150 && r < 250)
		{
			r *= 0.30; // 30%
		}
		else if (r >= 250)
		{
			r *= 0.35; // 35%
		}
		else
		{
			return 0;
		}
		return Math.round(r);
	}
	
	public PaypalData getPaypalData(int val)
	{
		return _paypal_data.get(val);
	}
	
	public class DonationTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!Config.ENABLE_DONATION_CHECKER)
			{
				return;
			}
			if (Config.GMAIL_ADDRESS.isEmpty() || Config.GMAIL_PASSWORD.isEmpty())
			{
				return;
			}
			IMAPFolder folder = null;
			Store store = null;
			try
			{
				Properties props = System.getProperties();
				props.setProperty("mail.store.protocol", "imaps");
				Session session = Session.getDefaultInstance(props, null);
				store = session.getStore("imaps");
				store.connect("smtp.gmail.com", Config.GMAIL_ADDRESS, Config.GMAIL_PASSWORD);
				folder = (IMAPFolder) store.getFolder("Inbox");
				if (!folder.isOpen())
				{
					folder.open(Folder.READ_WRITE);
				}
				try
				{
					handleMessages(folder, store);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			catch (NoSuchProviderException e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": NoSuchProviderException");
			}
			catch (MessagingException e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": MessagingException");
			}
			finally
			{
				try
				{
					if (folder != null)
					{
						if (folder.isOpen())
						{
							folder.close(true);
						}
					}
					if (store != null)
					{
						store.close();
					}
				}
				// catch (MessagingException e)
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public void handleMessages(IMAPFolder folder, Store store) throws Exception
	{
		Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
		//System.out.println(">>>>>>> "+messages.length);
		for (Message message : messages)
		{
			/**
			 * @paypalCheck
			 */
			Address[] from = message.getFrom();
			if (from == null)
			{
				continue;
			}
			if (!from[0].toString().contains("\"service@intl.paypal.com\""))
			{
				message.setFlag(Flags.Flag.SEEN, true);
				continue;
			}
			if (!from[0].toString().contains("<service@intl.paypal.com>"))
			{
				message.setFlag(Flags.Flag.SEEN, true);
				continue;
			}
			/**
			 * @isDonation
			 */
			String subject = message.getSubject();
			if (subject == null)
			{
				continue;
			}
			if (!subject.equals("Notification of Donation Received"))
			{
				message.setFlag(Flags.Flag.SEEN, true);
				continue;
			}
			/**
			 * @sendDate
			 */
			Date sentDate = message.getSentDate();
			if (sentDate == null)
			{
				continue;
			}
			/**
			 * @isMultiPart
			 */
			Object content = message.getContent();
			List<String> c = new ArrayList<>();
			try
			{
				for (String split : content.toString().split("\n"))
				{
					if (split.isEmpty())
					{
						continue;
					}
					if (!split.contains("This email confirms that you have received a donation"))
					{
						continue;
					}
					c.add(split);
				}
			}
			catch (Exception e)
			{
				LOGGER.warning("Exception arise at get Content");
				continue;
			}
			String transaction_id = "";
			String email = "";
			String amount = "";
			String currency = "";
			for (String s : c)
			{
				/**
				 * @Email Address
				 */
				if (s.contains("mailto:"))
				{
					// email = s.split("\\(")[1].split("\\)")[0];
					email = s.split("mailto:")[1].replaceAll("\\?", "").split("utm_source")[0];
				}
				/**
				 * @Total amount:
				 */
				if (s.contains("Total amount:"))
				{
					amount = s.split("Total amount:")[1].split(">")[3].split(";")[1].split("<")[0].split(" ")[0];
				}
				/**
				 * @Currency:
				 */
				if (s.contains("Currency:"))
				{
					currency = s.split("Currency:")[1].split(">")[3].split("<")[0];
				}
				/**
				 * @Confirmation number:
				 */
				if (s.contains("Confirmation number:"))
				{
					transaction_id = s.split("Confirmation number:")[1].split(">")[4].split("<")[0];
				}
				/**
				 * @Quantity:
				 */
				// TODO
				/**
				 * @Contributor:
				 */
				// TODO
			}
			if (transaction_id.isEmpty() || email.isEmpty() || amount.isEmpty() || currency.isEmpty() || transaction_id.length() != 17 || !isDouble(amount) || !currency.equals("Euros"))
			{
				LOGGER.warning("Invalid Donation Info:");
				LOGGER.warning("        id: " + (transaction_id.isEmpty() ? "empty" : transaction_id + " | " + transaction_id.length()));
				LOGGER.warning("     email: " + (email.isEmpty() ? "empty" : email));
				LOGGER.warning("    amount: " + (amount.isEmpty() ? "empty" : amount));
				LOGGER.warning("  currency: " + (currency.isEmpty() ? "empty" : currency));
				continue;
			}
			boolean exists = false;
			for (PaypalData donation : _paypal_data.values())
			{
				if (donation.getTransactionId().equalsIgnoreCase(transaction_id))
				{
					exists = true;
					break;
				}
			}
			if (exists)
			{
				// message.setFlag(Flag.SEEN, false);
				continue;
			}
			final String date = sentDate.toString();
			final double actualAmount = Double.parseDouble(amount);
			int objId = 0;
			String name = "";
			if (_paypal_queue.containsValue(transaction_id))
			{
				for (int obj : _paypal_queue.keySet())
				{
					if (_paypal_queue.get(obj).equalsIgnoreCase(transaction_id))
					{
						objId = obj;
						break;
					}
				}
			}
			if (objId != 0)
			{
				name = CharNameTable.getInstance().getNameById(objId);
			}
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				try (PreparedStatement ps = con.prepareStatement("INSERT INTO donation_records (transaction_id, transaction_date, email, amount, currency, receiver_id, receiver_name, claimed_date) VALUES (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS))
				{
					ps.setString(1, transaction_id);
					ps.setString(2, date);
					ps.setString(3, email);
					ps.setDouble(4, actualAmount);
					ps.setString(5, currency);
					ps.setInt(6, objId);
					ps.setString(7, name);
					ps.setBoolean(8, false);
					ps.executeUpdate();
					try (ResultSet rs = ps.getGeneratedKeys())
					{
						if (rs.next())
						{
							int id = rs.getInt(1);
							PaypalData donation = new PaypalData(id, transaction_id, date, email, actualAmount, currency, objId, name, 0);
							_paypal_data.put(id, donation);
							_paypal_link.put(transaction_id, id);
							_generated_paypal_list = generatePaypalList();
							LOGGER.warning(getClass().getSimpleName() + ": new Donation of " + amount + " " + currency + " by " + email);
						}
					}
				}
			}
			catch (Exception e)
			{
				System.out.println(email);
				e.printStackTrace();
			}
			// message.setFlag(Flag.SEEN, false);
			// List<Message> tempList = new ArrayList<>();
			// tempList.add(message);
			// Message[] tempMessageArray = tempList.toArray(new Message[tempList.size()]);
			// folder.copyMessages(tempMessageArray, store.getFolder("Donation"));
			// message.setFlags(new Flags(Flags.Flag.DELETED), true);
		}
		for (int objId : _paypal_queue.keySet())
		{
			final L2PcInstance player = L2World.getInstance().getPlayer(objId);
			if (player == null)
			{
				continue;
			}
			String transaction_id = _paypal_queue.get(objId);
			if (!_paypal_link.containsKey(transaction_id))
			{
				player.sendPacket(new ExShowScreenMessage("Transaction ID rejected as not valid.", 5000));
				continue;
			}
			receivePaypalDonation(player, transaction_id);
		}
		_paypal_queue.clear();
	}
	
	private boolean isDouble(String text)
	{
		try
		{
			Double.parseDouble(text);
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}
	
	private final int						PAYPAL_ROWS_PER_PAGE	= 10;
	private Map<Integer, List<PaypalData>>	_generated_paypal_list	= new HashMap<>();
	
	private Map<Integer, List<PaypalData>> generatePaypalList()
	{
		return generatePaypalList(0, "");
	}
	
	private Map<Integer, List<PaypalData>> generatePaypalList(int type, String match)
	{
		Map<Integer, List<PaypalData>> generated = new HashMap<>();
		List<PaypalData> list = new ArrayList<>();
		if (match.isEmpty())
		{
			list.addAll(_paypal_data.values());
		}
		else
		{
			for (PaypalData data : _paypal_data.values())
			{
				switch (type)
				{
					case 0: // id
					{
						if (data.getTransactionId().startsWith(match))
						{
							list.add(data);
						}
						continue;
					}
					case 1: // mail
					{
						if (data.getEmail().startsWith(match))
						{
							list.add(data);
						}
						continue;
					}
					case 2: // name
					{
						if (data.getReceiverName().startsWith(match))
						{
							list.add(data);
						}
						continue;
					}
				}
			}
		}
		Collections.reverse(list);
		int pages = (list.size() - 1) / PAYPAL_ROWS_PER_PAGE;
		int from;
		int to;
		for (int i = 0; i <= pages; i++)
		{
			from = i * PAYPAL_ROWS_PER_PAGE;
			to = (i + 1) * PAYPAL_ROWS_PER_PAGE;
			generated.put(i, list.subList(from, to > list.size() ? list.size() : to));
		}
		return generated;
	}
	
	public Map<Integer, List<PaypalData>> getGeneratedPaypalList(int type, String match)
	{
		if (match.isEmpty())
		{
			return _generated_paypal_list;
		}
		return generatePaypalList(type, match);
	}
	
	public void checkRewards(L2PcInstance player)
	{
		for (PaypalData donation : _paypal_data.values())
		{
			if (donation.getReceiverId() != player.getObjectId())
			{
				continue;
			}
			if (donation.isClaimed())
			{
				continue;
			}
			receivePaypalDonation(player, donation.getTransactionId());
		}
	}
	
	public int getPurchaseCount(int val)
	{
		if (_purchase_count.containsKey(val))
		{
			return _purchase_count.get(val);
		}
		return 0;
	}
	
	public void increasePurchaseCount(int id, int count)
	{
		if (_purchase_count.containsKey(id))
		{
			count += _purchase_count.get(id);
		}
		_purchase_count.put(id, count);
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("INSERT INTO donation_purchase_count (id, count) VALUES (?,?) ON DUPLICATE KEY UPDATE id=?, count=?"))
			{
				ps.setInt(1, id);
				ps.setInt(2, count);
				ps.setInt(3, id);
				ps.setInt(4, count);
				ps.executeUpdate();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public class PaypalData
	{
		private final int		_id;
		private final String	_transaction_id;
		private final String	_date;
		private final String	_email;
		private final double	_amount;
		private final String	_currency;
		private int				_receiver_id;
		private String			_receiver_name;
		private long			_claimed_date;
		
		public PaypalData(int id, String transaction_id, String date, String email, double amount, String currency, int receiver_id, String receiver_name, long claimed_date)
		{
			_id = id;
			_transaction_id = transaction_id;
			_date = date;
			_email = email;
			_amount = amount;
			_currency = currency;
			_receiver_id = receiver_id;
			_receiver_name = receiver_name;
			_claimed_date = claimed_date;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public String getTransactionId()
		{
			return _transaction_id;
		}
		
		public String getDate()
		{
			return _date;
		}
		
		public String getEmail()
		{
			return _email;
		}
		
		public double getAmount()
		{
			return _amount;
		}
		
		public String getCurrency()
		{
			return _currency;
		}
		
		public int getReceiverId()
		{
			return _receiver_id;
		}
		
		public String getReceiverName()
		{
			return _receiver_name;
		}
		
		public String getClaimedDate()
		{
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(_claimed_date);
			return cal.getTime().toString();
		}
		
		public boolean isClaimed()
		{
			return _claimed_date > 0;
		}
		
		public void setReceiverId(int id)
		{
			_receiver_id = id;
		}
		
		public void setReceiverName(String name)
		{
			_receiver_name = name;
		}
		
		public void setClaimed(long time)
		{
			_claimed_date = time;
		}
	}
	
	public static DonationManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DonationManager _instance = new DonationManager();
	}
}