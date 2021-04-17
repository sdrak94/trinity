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
package net.sf.l2j.gameserver.network.serverpackets;

/**
 * Send Private (Friend) Message
 *
 * Format: c dSSS
 *
 * d: Unknown
 * S: Sending Player
 * S: Receiving Player
 * S: Message
 *
 * @author Tempy
 */
public class L2FriendSay extends L2GameServerPacket
{
	private static final String _S__FD_FRIENDRECVMSG = "[S] 78 L2FriendSay";

	private String _sender, _receiver, _message;

	public L2FriendSay(String sender, String reciever, String message)
	{
		_sender = sender;
		_receiver = reciever;

		_message = message;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x78);

		writeD(0); // ??
		writeS(_receiver);
		writeS(_sender);
		writeS(_message);
	}

	@Override
	public String getType()
	{
		return _S__FD_FRIENDRECVMSG;
	}
}
