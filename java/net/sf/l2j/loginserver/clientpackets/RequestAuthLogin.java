/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.loginserver.clientpackets;

import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import net.sf.l2j.Config;
import net.sf.l2j.loginserver.GameServerTable.GameServerInfo;
import net.sf.l2j.loginserver.HackingException;
import net.sf.l2j.loginserver.L2LoginClient;
import net.sf.l2j.loginserver.L2LoginClient.LoginClientState;
import net.sf.l2j.loginserver.LoginController;
import net.sf.l2j.loginserver.LoginController.AuthLoginResult;
import net.sf.l2j.loginserver.serverpackets.AccountKicked;
import net.sf.l2j.loginserver.serverpackets.AccountKicked.AccountKickedReason;
import net.sf.l2j.loginserver.serverpackets.LoginFail.LoginFailReason;
import net.sf.l2j.loginserver.serverpackets.LoginOk;
import net.sf.l2j.loginserver.serverpackets.ServerList;

/**
 * Format: x
 * 0 (a leading null)
 * x: the rsa encrypted block with the login an password
 */
public class RequestAuthLogin extends L2LoginClientPacket
{
private static Logger _log = Logger.getLogger(RequestAuthLogin.class.getName());

private byte[] _raw = new byte[128];

private String _user;
private String _password;
private int _ncotp;

/**
 * @return
 */
public String getPassword()
{
	return _password;
}

/**
 * @return
 */
public String getUser()
{
	return _user;
}

public int getOneTimePassword()
{
	return _ncotp;
}

@Override
public boolean readImpl()
{
	if (super._buf.remaining() >= 128)
	{
		readB(_raw);
		return true;
	}
	else
	{
		return false;
	}
}

@Override
public void run()
{
	byte[] decrypted = null;
	try
	{
		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
		rsaCipher.init(Cipher.DECRYPT_MODE, getClient().getRSAPrivateKey());
		decrypted = rsaCipher.doFinal(_raw, 0x00, 0x80 );
	}
	catch (GeneralSecurityException e)
	{
		e.printStackTrace();
		return;
	}
	
	_user = new String(decrypted, 0x5E, 14 ).trim();
	_user = _user.toLowerCase();
	_password = new String(decrypted, 0x6C, 16).trim();
	_ncotp = decrypted[0x7c];
	_ncotp |= decrypted[0x7d] << 8;
	_ncotp |= decrypted[0x7e] << 16;
	_ncotp |= decrypted[0x7f] << 24;
	
	final LoginController lc = LoginController.getInstance();
	final L2LoginClient client = getClient();
	try
	{
		AuthLoginResult result = lc.tryAuthLogin(_user, _password, client);
		
		switch (result)
		{
		case AUTH_SUCCESS:
			client.setAccount(_user);
			client.setState(LoginClientState.AUTHED_LOGIN);
			client.setSessionKey(lc.assignSessionKeyToClient(_user, client));
			if (Config.SHOW_LICENCE)
			{
				client.sendPacket(new LoginOk(client.getSessionKey()));
			}
			else
			{
				getClient().sendPacket(new ServerList(client));
			}
			break;
		case INVALID_PASSWORD:
			client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
			break;
		case ACCOUNT_BANNED:
			client.close(new AccountKicked(AccountKickedReason.REASON_PERMANENTLY_BANNED));
			break;
		case ALREADY_ON_LS:
			final L2LoginClient oldClient = lc.getAuthedClient(_user);

			
			if (oldClient != null)
			{
				// kick the other client
				lc.removeAuthedLoginClient(_user);
				oldClient.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
				// Not proper place... must be placed whenever a character is kicked when the same account is logged in again.
				//if (player.getLoginData() != null)
				//{
				//	player.getLoginData().updateLog("Kicked by another player logging in the same account.");
				//}
			}
			else
			{
				_log.warning("LOL WTF ALREADY_ON_LS returned null for client: "+_user);
			}
			
			// kick also current client
			client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
			break;
		case ALREADY_ON_GS:
			final GameServerInfo gsi = lc.getAccountOnGameServer(_user);
			
			if (gsi != null)
			{
				// kick from there
				if (gsi.isAuthed())
				{
					gsi.getGameServerThread().kickPlayer(_user);
				}
			}
			else
			{
				_log.warning("LOL WTF ALREADY_ON_GS returned null for account: "+_user);
			}
			
			client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
			break;
		}
	}
	catch (HackingException e)
	{
		InetAddress address = getClient().getConnection().getInetAddress();
		lc.addBanForAddress(address, Config.LOGIN_BLOCK_AFTER_BAN*1000);
		_log.info("Banned ("+address+") for "+Config.LOGIN_BLOCK_AFTER_BAN+" seconds, due to "+e.getConnects()+" incorrect login attempts.");
	}
}
}
