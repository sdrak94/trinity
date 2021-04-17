package luna.custom;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import luna.custom.email.CodeGenerator;

public class RefundManager
{
	
	public static void main(String[] args) throws Exception
	{
		Connection con = null;
		Connection con2 = null;
		Connection con3 = null;
		try
		{
			
			//Class.forName("com.mysql.jdbc.Driver");
			
			System.out.println("Connecting to database...");
			
			con = DriverManager.getConnection("jdbc:mysql://localhost/trinity_local", "root", "");
			// con = _source.getConnection();
			PreparedStatement statement = con.prepareStatement("select distinct email from donations;");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				String email = rset.getString(1);
				out("email:" + email);
				con2 = DriverManager.getConnection("jdbc:mysql://localhost/trinity_local", "root", "");
				PreparedStatement statement2 = con2.prepareStatement("select sum(payment_amount) from donations where email=?");
				statement2.setString(1, email);
				ResultSet rset2 = statement2.executeQuery();
				if (rset2.next())
				{
					int ammount = rset2.getInt(1);
					con3 = DriverManager.getConnection("jdbc:mysql://localhost/trinity_local", "root", "");
					PreparedStatement statement3 = con3.prepareStatement("replace into donations_refund values (?,?,?,?,?,?,?,?,?,?,?,?)");
					String code = CodeGenerator.getInstance().startDonateRefund();
					statement3.setString(1, code); // txn_id
					statement3.setInt(2, ammount); // payment_amount
					statement3.setInt(3, 0); // received_amount
					statement3.setString(4, "false"); // retrieved_refund
					statement3.setString(5, ""); // retriever_ip
					statement3.setString(6, ""); // retriever_acct
					statement3.setString(7, ""); // retriever_charobjid
					statement3.setString(8, ""); // retriever_char
					statement3.setString(9, ""); // retrieval_date
					statement3.setString(10, email); // email
					statement3.setString(11, ""); // hwid
					statement3.setString(12, ""); // hwid
					statement3.executeQuery();
					out("Email: " + email + "\t\t | Ammount =" + ammount + "\t\t | Code" + code);
					statement3.close();
					con3.close();
				}
				rset2.close();
				con2.close();
			}
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			out("could not check existing char number:" + e.getMessage());
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
		try
		{
			
			//Class.forName("com.mysql.jdbc.Driver");
			
			System.out.println("Connecting to database...");
			
			con = DriverManager.getConnection("jdbc:mysql://localhost/trinity_local", "root", "");
			// con = _source.getConnection();
			PreparedStatement statement = con.prepareStatement("select email from paypal_disputers;");
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				String email = rset.getString(1);
				//out("email:" + email);
				con2 = DriverManager.getConnection("jdbc:mysql://localhost/trinity_local", "root", "");
				PreparedStatement statement2 = con2.prepareStatement("UPDATE donations_refund set disputer='true' where email=?");
				statement2.setString(1, email);
				statement2.executeUpdate();
				out(email + " has been marked as disputer");
				con2.close();
			}
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			out("could not check existing char number:" + e.getMessage());
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
	}

	private static void out(String out)
	{
		System.out.println("NANOS : " + out);
	}
}
