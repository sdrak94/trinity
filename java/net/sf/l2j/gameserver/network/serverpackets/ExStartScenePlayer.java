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
 * 
 * @author JIV
 */

public class ExStartScenePlayer extends L2GameServerPacket
{
    private static final String _S__FE99_PLAYQUESTMOVIE = "[S] FE:99 ExStartScenePlayer";

	 private int _movieId;
	    public static final int LINDVIOR = 1;
	    public static final int EKIMUS_OPENING = 2;
	    public static final int EKIMUS_SUCCESS = 3;
	    public static final int EKIMUS_FAIL = 4;
	    public static final int TIAT_OPENING = 5;
	    public static final int TIAT_SUCCESS = 6;
	    public static final int TIAT_FAIL = 7;
	    public static final int SSQ_SUSPECIOUS_DEATHS = 8;
	    public static final int SSQ_DYING_MASSAGE = 9;
	    public static final int SSQ_CONTRACT_OF_MAMMON = 10;
	    public static final int SSQ_RITUAL_OF_PRIEST = 11;
	    public static final int SSQ_SEALING_EMPEROR_1ST = 12;
	    public static final int SSQ_SEALING_EMPEROR_2ND = 13;
	    public static final int SSQ_EMBRYO = 14;
	    public static final int LAND_KSERTH_A = 1000;
	    public static final int LAND_KSERTH_B = 1001;
	    public static final int LAND_UNDEAD_A = 1002;
	    public static final int LAND_DISTRUCTION_A = 1004;
	         
	    public ExStartScenePlayer(int id)
	    {
	    	_movieId = id;
	    }

	    @Override
		public void writeImpl()
	    {
	        writeC(0xfe);
	        writeH(0x99);
	        writeD(_movieId);
	    }

	    @Override
		public String getType()
	    {
	        return _S__FE99_PLAYQUESTMOVIE;
	    }
}
