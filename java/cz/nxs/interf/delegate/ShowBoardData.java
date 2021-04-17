package cz.nxs.interf.delegate;

import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.delegate.IShowBoardData;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

/**
 * @author hNoke
 *
 */
public class ShowBoardData implements IShowBoardData
{
	private ShowBoard _board;
	
	public ShowBoardData(ShowBoard sb)
	{
		_board = sb;
	}
	
	public ShowBoardData(String text, String id)
	{
		_board = new ShowBoard(text, id);
	}
	
	@Override
	public void sendToPlayer(PlayerEventInfo player)
	{
		player.getOwner().sendPacket(_board);
	}
}
