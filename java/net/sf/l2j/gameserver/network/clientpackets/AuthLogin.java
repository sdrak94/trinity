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
import java.util.logging.Logger;

import org.strixplatform.StrixPlatform;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.LoginServerThread.SessionKey;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.LeaveWorld;
import net.sf.l2j.gameserver.network.serverpackets.ServerClose;

public final class AuthLogin extends L2GameClientPacket
{
	private static Logger	_log	= Logger.getLogger(AuthLogin.class.getName());
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
	
	@Override
	protected void readImpl()
	{
		_loginName = readS().toLowerCase();
		_playKey2 = readD();
		_playKey1 = readD();
		_loginKey1 = readD();
		_loginKey2 = readD();
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
			}
			else
				client.close(new LeaveWorld());
        if(StrixPlatform.getInstance().isPlatformEnabled())
        {
            if(client.getStrixClientData() != null)
            {
                client.getStrixClientData().setClientAccount(_loginName);
                if(StrixPlatform.getInstance().isAuthLogEnabled())
                {
                	_log.warning("Account: [" + _loginName + "] HWID: [" + client.getStrixClientData().getClientHWID() + "] SessionID: [" + client.getStrixClientData().getSessionId() + "] entered to Game Server");
                }
            }
            else
            {
                client.close(ServerClose.STATIC_PACKET);
                return;
            }
        }
	}	
//	@Override
//	protected void runImpl()
//	{
//		final L2GameClient client = getClient();
//		if (_loginName.isEmpty() || !client.isProtocolOk())
//		{
//			client.close(new LeaveWorld());
//			return;
//		}
//		SessionKey key = new SessionKey(_loginKey1, _loginKey2, _playKey1, _playKey2);
//		if (Config.DEBUG)
//		{
//			_log.info("user:" + _loginName);
//			_log.info("key:" + key);
//		}
//		// avoid potential exploits
//		if (client.getAccountName() != null)
//		{
//			return;
//		}
//		
//        if (LoginServerThread.getInstance().addGameServerLogin(_loginName, client))
//        {
//                client.setAccountName(_loginName);
//                LoginServerThread.getInstance().addWaitingClientAndSendRequest(_loginName, client, key);
//                getPassword(client);
//        }
//        else
//        {
//        	client.close(new LeaveWorld());
//        }
//	}
	
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
