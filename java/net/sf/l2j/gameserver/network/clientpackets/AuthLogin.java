/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.logging.Logger;

import guard.HwidBanController;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.LoginServerThread.SessionKey;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.LeaveWorld;

public final class AuthLogin extends L2GameClientPacket
{
	private static Logger	_log		= Logger.getLogger(AuthLogin.class.getName());
	// loginName + keys must match what the loginserver used.
	private String			_loginName;
	/*
	 * private final long _key1;
	 * private final long _key2;
	 * private final long _key3;
	 * private final long _key4;
	 */
	private int				_playKey1;
	private int				_playKey2;
	private int				_loginKey1;
	private int				_loginKey2;

	private byte[] guardData = null;
	
	@Override
	protected void readImpl()
	{

		//System.out.println(_buf.limit());
		//HexUtil.hexdump(_buf.array(), 77, 0, ' ', 4);
		_loginName = readS().toLowerCase();
		_playKey2 = readD();
		_playKey1 = readD();
		_loginKey1 = readD();
		_loginKey2 = readD();
		readD(); // lang
		if (_buf.remaining() >= 32)
		{
			// last 32 bytes
			guardData = new byte[32];
			guardData = Arrays.copyOfRange(_buf.array(), _buf.limit() - 32, _buf.limit());
		}
	}
	
	@Override
	protected void runImpl()
	{
		final SessionKey key = new SessionKey(_loginKey1, _loginKey2, _playKey1, _playKey2);
		final L2GameClient client = getClient();
		if (client.getAccountName() == null)
			// Preventing duplicate login in case client login server socket was
			// disconnected or this packet was not sent yet
			if (LoginServerThread.getInstance().addGameServerLogin(_loginName, client))
			{
				client.setAccountName(_loginName);
				LoginServerThread.getInstance().addWaitingClientAndSendRequest(_loginName, client, key);
				getPassword(client);
				client.setFullHwid(guardData);
			}
			else
				client.close(new LeaveWorld());
		
		if(!client.getFullHwid().isEmpty())
		{
			if(HwidBanController.getInstance().checkIfBanned(client.getFullHwid()))
			{
				client.closeNow();
			}
		}
	}
	
	private void getPassword(final L2GameClient client)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT pass FROM accounts WHERE login=?");
			statement.setString(1, _loginName);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				client.setPassword(rset.getString("pass"));
			}
			else
			{
				_log.warning("1 LOL WTF " + _loginName + " acct has no password????");
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("2 LOL WTF " + _loginName + " acct has no password????" + e);
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
	
	@Override
	public String getType()
	{
		return "[C] 0x08 AuthLogin";
	}
}
