package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.StringTokenizer;

import luna.custom.email.DonationCodeGenerator;
import luna.custom.logger.LunaLogger;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.GMAudit;

public class L2DonationMerchantInstance extends L2MerchantInstance
{
	private static final double donationPercentage = 0.3;
	public static final int CLAN_SKILLS[] = {370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389 , 390, 391, 392, 393, 394, 397, 398, 399} ;
	
	public L2DonationMerchantInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		if (actualCommand.equalsIgnoreCase("donation_take"))
		{
			if (st.hasMoreTokens())
			{
				String transaction_id = st.nextToken();
				if (transaction_id.startsWith("#"))
				{
					transaction_id = transaction_id.substring(1);
				}
				if (transaction_id.length() != 17)
				{
					sendErrorPage(player, 1);
					return;
				}
				transaction_id = transaction_id.toUpperCase();
				if (!retrieveDonation(transaction_id, player))
				{
					// send fail page
					return;
				}
				else
				{
					// send ty page - handled by retrieveDonation method
					return;
				}
			}
			sendErrorPage(player, 2);
			return;
		}
		if (actualCommand.equalsIgnoreCase("donation_refund_email"))
		{
			if (st.hasMoreTokens())
			{
				String email = st.nextToken();
				
				
				//email = email.toUpperCase();
				if (!retrieveDonationRefundEmail(email, player))
				{
					// send fail page
					return;
				}
				else
				{
					// send ty page - handled by retrieveDonation method
					return;
				}
			}
			sendErrorPage(player, 2);
			return;
		}
		if (actualCommand.equalsIgnoreCase("donation_refund"))
		{
			if (st.hasMoreTokens())
			{
				String email = st.nextToken();
				
				//email = email.toUpperCase();
				if (!retrieveDonationRefund(email, player))
				{
					// send fail page
					return;
				}
				else
				{
					// send ty page - handled by retrieveDonation method
					return;
				}
			}
			sendErrorPage(player, 2);
			return;
		}
		if (actualCommand.equalsIgnoreCase("donate_clan"))
		{
			boolean clvlbool = false;
			boolean skillsbool = false;
			int clvl = 0;
			int idcost = 0;
			int cost = 0;
			int slvl = 0;
			boolean ok = false;
			if (st.hasMoreTokens())
			{
				String lvlboolStr = st.nextToken();
				String clvlStr = st.nextToken();
				String skillsboolStr = st.nextToken();
				String slvlStr = st.nextToken();
				String idcostStr = st.nextToken();
				String costStr = st.nextToken();
				if (lvlboolStr.equalsIgnoreCase("true"))
				{
					clvlbool = true;
				}
				if (skillsboolStr.equalsIgnoreCase("true"))
				{
					skillsbool = true;
				}
				try
				{
					clvl = Integer.parseInt(clvlStr);
					slvl = Integer.parseInt(slvlStr);
					idcost = Integer.parseInt(idcostStr);
					cost = Integer.parseInt(costStr);
				}
				catch (Exception e)
				{
					return;
				}
				ok = true;
			}
			if (ok)
			{
				donateClanLvLPoints(player, clvlbool, clvl, skillsbool, slvl, idcost, cost);
			}
			return;
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	private static void sendErrorPage(L2PcInstance player, int code)
	{
		String filename = "99999-8.htm";
		switch (code)
		{
			case 1:
				filename = "99999-8.htm"; // general error
				break;
			case 2:
				filename = "99999-12.htm"; // enter transaction #
				break;
			case 3:
				filename = "99999-9.htm"; // already retrieved
				break;
			case 5:
				filename = "99999-6.htm"; // thank you
				break;
		}
		String content = HtmCache.getInstance().getHtmForce("data/html/merchant/" + filename);
		NpcHtmlMessage tele = new NpcHtmlMessage(1);
		tele.setHtml(content);
		player.sendPacket(tele);
	}

	private static void donateClanLvLPoints(L2PcInstance plr, boolean lvl, int clevel, boolean skills, int skillLvl, int idcost, int cost)
	{
		if (plr.getClan() != null)
		{
			int clanlvl = plr.getClan().getLevel();
			if ((plr.getInventory().getItemByItemId(idcost) == null) || (plr.getInventory().getItemByItemId(idcost).getCount() < cost))
			{
				plr.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			if (lvl && clevel != 0)
			{
				if (clanlvl >= clevel)
				{
					plr.sendMessage("Your clan is already " + String.valueOf(clanlvl));
				}
				plr.getClan().changeLevel(clevel);
				for (L2PcInstance member : plr.getClan().getOnlineMembers(0))
				{
					String text = ": " + plr.getName() + " donated for Level: " + clevel + " clan.";
					CreatureSay cs = new CreatureSay(0, Say2.CLAN, plr.getClan().getName(), text);
					member.sendSkillList();
					member.sendPacket(cs);
				}
			}
			if (skills)
			{
				final L2Skill[] list = plr.getClan().getAllSkills();
				for (int skillId : CLAN_SKILLS)
				{
					boolean f = false;
					for (L2Skill sk : list)
					{
						if (sk.getId() != skillId)
						{
							continue;
						}
						if (sk.getLevel() >= skillLvl)
						{
							f = true;
							break;
						}
					}
					if (f)
					{
						// plr.sendMessage("ignore " + i);
						continue;
					}
					// check here if clan got already the skill on higher level
					final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
					String skillname = skill.getName();
					SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED);
					sm.addSkillName(skill);
					plr.sendPacket(sm);
					plr.getClan().broadcastToOnlineMembers(sm);
					plr.getClan().addNewSkill(skill);
					for (L2PcInstance member : plr.getClan().getOnlineMembers(0))
					{
						String text = ": " + plr.getName() + " > " + skillname + " Lvl."+skillLvl+" to the clan.";
						CreatureSay cs = new CreatureSay(0, Say2.CLAN, plr.getClan().getName(), text);
						member.sendPacket(cs);
					}
				}
				for (L2PcInstance member : plr.getClan().getOnlineMembers(0))
				{
					member.sendSkillList();
				}
			}
			plr.destroyItemByItemId("Donate Clan", idcost, cost, plr, true);
		}
	}
	
	private synchronized static final boolean retrieveDonation(String txn_id, L2PcInstance player)
	{
		if (txn_id == null || player == null)
		{
			return false;
		}
		System.out.println(txn_id);
		int payment_amount = 0;
		int received_amount = 0;
		int bonus_amount = 0;
		String payment_status = null;
		boolean retrieved = true;
		boolean exit = false;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM donations WHERE txn_id=?");
			statement.setString(1, txn_id);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				payment_amount = rset.getInt("payment_amount");
				bonus_amount = rset.getInt("bonus_amount");
				received_amount = rset.getInt("received_amount");
				// System.out.println(payment_amount);
				if (received_amount < 1)
					exit = true;
				else if (received_amount > 15000)
					exit = true;
				payment_status = rset.getString("payment_status");
				// System.out.println(payment_status);
				if (!payment_status.equals("Completed"))
					exit = true;
				retrieved = rset.getBoolean("retrieved");
				// System.out.println("has this donation been retrieved before? "+retrieved);
				if (retrieved)
					exit = true;
			}
			else
			{
				exit = true;
				retrieved = false;
			}
			rset.close();
			statement.close();
			if (exit)
			{
				if (retrieved)
					sendErrorPage(player, 3);
				else
					sendErrorPage(player, 1);
				con.close();
				return false;
			}
			try
			{
				String today = GMAudit._formatter.format(new Date());
				statement = con.prepareStatement("UPDATE donations set retrieved=1, retriever_ip=?, retriever_acct=?, retriever_char=?, retrieval_date=?, hwid=? WHERE txn_id=?");
				statement.setString(1, player.getIP());
				statement.setString(2, player.getAccountName());
				statement.setString(3, player.getName());
				statement.setString(4, today);
				statement.setString(5, player.getHWID());
				statement.setString(6, txn_id);
				statement.execute();
			}
			catch (Exception e)
			{
				_log.warning("could not update retreival from false to true on id: " + txn_id + " with player name: " + player.getName() + " " + e.getMessage());
				exit = true;
			}
			finally
			{
				statement.close();
			}
			if (exit)
			{
				con.close();
				return false;
			}
			if (received_amount >= 700)
			{
				_log.severe(player.getName() + " has initiated a donation retrieval of more than 700 dollars with id " + txn_id);
			}
			_log.config(player.getName() + " has retrieved a donation of " + received_amount + " dollars with id " + txn_id);
			try
			{
				player.addItem("donation_token", L2Item.DONATION_TOKEN, received_amount, player, true);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				exit = true;
			}
			if (exit)
			{
				con.close();
				return false;
			}
			String content = HtmCache.getInstance().getHtmForce("data/html/merchant/99999-6.htm");
			NpcHtmlMessage tele = new NpcHtmlMessage(1);
			tele.setHtml(content);
			tele.replace("!paid!", String.valueOf(payment_amount));
			tele.replace("!bonus!", String.valueOf(bonus_amount));
			tele.replace("!received!", String.valueOf(received_amount));
			player.sendPacket(tele);
			return true;
		}
		catch (SQLException e)
		{
			_log.warning("could not check existing char number:" + e.getMessage());
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		return false;
	}

	private synchronized static final boolean retrieveDonationRefundEmail(String email, L2PcInstance player)
	{
		if (email == null || player == null)
		{
			return false;
		}
		String date = GMAudit._formatter.format(new Date()) +" - ";
		
		String emailStart = date +"["+ player.getObjectId()+"] / "+ player.getName() +" / "+ player.getIP() +" / "+ player.getHWID() +" / "+ " - " + email ;
		String message = "Message";
		//System.out.println(email);
		int payment_amount = 0;
		String code = "";
		String disputer = "";
		boolean retrieved = false;
		boolean disp = false;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT payment_amount,txn_id,disputer,retrieved_refund FROM donations_refund WHERE email=?");
			statement.setString(1, email);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				payment_amount = rset.getInt(1);
				code = rset.getString(2);
				disputer = rset.getString(3);
				
				if (rset.getString(4).equalsIgnoreCase("true"))
				{
					retrieved = true;
				}
				if (disputer.equalsIgnoreCase("true"))
				{
					player.sendMessage("You are marked as a disputer. If you need further assistance contact [GM]Brado.");
					message = "Disputer!";
					disp = true;
					LunaLogger.getInstance().log("donationMerchant_refunds_disputers_claim",emailStart +" - attempted to claim refund." );
					return false;
				}
				if (retrieved)
				{
					player.sendMessage("Your donation has already been retrieved.");
					message = "Your donation has already been retrieved";
					return false;
				}
				else
				{
					player.sendMessage("You have donated in total: "+payment_amount);
					player.sendMessage("You will receive: 30%: "+(int) (payment_amount*donationPercentage));
					message = "An email has been sent Check your inbox.";
					LunaLogger.getInstance().log("donationMerchant_refunds_claims_ok",emailStart +" - an email has been sent to him OK!" );
				}
			}
			else
			{
				message = "Email not found in our past donation list";
				player.sendMessage("Email not found in our past donation list.");
				LunaLogger.getInstance().log("donationMerchant_refunds_emails_wrong",emailStart +" - tried to claim a refund that doesn't exist in DB." );
				return false;
			}
			rset.close();
			statement.close();
			try
			{
				if(!disp)
				{
					DonationCodeGenerator.getInstance();
					DonationCodeGenerator.sendEmail(email, "Thanks for supporting L2Trinity<br>This is your donation transaction code:#"+ code +" for: "+(int) (payment_amount*donationPercentage)+" euro <br> If there's a bonus on the amount you donated it will be displayed in game.");
					LunaLogger.getInstance().log("donationMerchant_refunds_mails_ok",emailStart +" - Thanks for supporting L2Trinity This is your donation transaction code:#"+ code +" for: "+(int) (payment_amount*donationPercentage)+" euro <br> If there's a bonus on the amount you donated it will be displayed in game." );
				}
			}
			catch (Exception e)
			{
				//_log.warning("could not update retreival from false to true on id: " + txn_id + " with player name: " + player.getName() + " " + e.getMessage());
				
			}
			finally
			{
				statement.close();
			}
			try
			{
				//player.addItem("donation_token", L2Item.DONATION_TOKEN, received_amount, player, true);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			String content = HtmCache.getInstance().getHtmForce("data/html/merchant/99999-13.htm");
			NpcHtmlMessage tele = new NpcHtmlMessage(1);
			tele.setHtml(content);
			tele.replace("!paid!", String.valueOf(payment_amount));
			tele.replace("%message%", message);
			//tele.replace("!bonus!", String.valueOf(bonus_amount));
			//tele.replace("!received!", String.valueOf(received_amount));
			player.sendPacket(tele);
			return true;
		}
		catch (SQLException e)
		{
			_log.warning("could not check existing char number:" + e.getMessage());
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		return false;
	}

	private synchronized static final boolean retrieveDonationRefund(String txn_id, L2PcInstance player)
	{
		if (txn_id == null || player == null)
		{
			return false;
		}
		String date = GMAudit._formatter.format(new Date());
		//System.out.println(txn_id);
		int payment_amount = 0;
		int received_amount = 0;
		String retrieved_refund = "";
		int bonus_amount = 0;
		String email = "";
		String emailStart = date +"["+ player.getObjectId()+"] / "+ player.getName() +" / "+ player.getIP() +" / "+ player.getHWID() +" / "+ " - " + email ;
		boolean retrieved = false;
		boolean exit = false;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM donations_refund WHERE txn_id=?");
			statement.setString(1, txn_id);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				payment_amount = rset.getInt("payment_amount");
				retrieved_refund = rset.getString("retrieved_refund");
				email = rset.getString("email");
				received_amount = (int) (payment_amount*donationPercentage);
				
				if (payment_amount < 1)
					exit = true;
				if (retrieved_refund.equalsIgnoreCase("true"))
				{
					exit = true;
					retrieved = true;
				}
			}
			else
			{
				exit = true;
				retrieved = false;
			}
			rset.close();
			statement.close();
			if (exit)
			{
				if (retrieved)
					sendErrorPage(player, 3);
				else
					sendErrorPage(player, 1);
				con.close();
				return false;
			}
			try
			{
				if(!retrieved)
				{
					String today = GMAudit._formatter.format(new Date());
					statement = con.prepareStatement("UPDATE donations_refund set received_amount=?, retrieved_refund=?, retriever_ip=?, retriever_acct=?, retriever_charobjid=?, retriever_char=?, retrieval_date=?, hwid=?  WHERE txn_id=?");
					statement.setInt(1, received_amount);
					statement.setString(2, "true");
					statement.setString(3, player.getIP());
					statement.setString(4, player.getAccountName());
					statement.setInt(5, player.getObjectId());
					statement.setString(6, player.getName());
					statement.setString(7, today);
					statement.setString(8, player.getHWID());
					statement.setString(9, txn_id);
					statement.execute();
				}
			}
			catch (Exception e)
			{
				_log.warning("could not update retreival from false to true on id: " + txn_id + " with player name: " + player.getName() + " " + e.getMessage());
				exit = true;
			}
			finally
			{
				statement.close();
			}
			if (exit)
			{
				con.close();
				return false;
			}
			try
			{
				player.addItem("donation_token", L2Item.DONATION_TOKEN, received_amount, player, true);
				LunaLogger.getInstance().log("donationMerchant_refunds_success",emailStart + " - has retrieved a donation of " + received_amount + " dollars with id " + txn_id);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				exit = true;
			}
			if (exit)
			{
				con.close();
				return false;
			}
			String content = HtmCache.getInstance().getHtmForce("data/html/merchant/99999-6.htm");
			NpcHtmlMessage tele = new NpcHtmlMessage(1);
			tele.setHtml(content);
			tele.replace("!paid!", String.valueOf(payment_amount));
			tele.replace("!bonus!", String.valueOf(bonus_amount));
			tele.replace("!received!", String.valueOf(received_amount));
			player.sendPacket(tele);
			return true;
		}
		catch (SQLException e)
		{
			_log.warning("could not check existing char number:" + e.getMessage());
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		return false;
	}	
	
}