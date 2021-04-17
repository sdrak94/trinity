package luna.custom.ranking;

import java.sql.ResultSet;

import net.sf.l2j.Config;


public class TopData
{
	private final int _charId;
	private final String _charName;
	private final String _charTitle;
	private final String _className;
	private final int _pvpKills;
	private final int _pkKills;
	private final int _fame;
	private final int _raidPoints;
	private final int _accesslevel;
	private final String _clanName;
	private final String _clanCrest;
	private final String _allyCrest;
	
	public TopData(ResultSet rs) throws Exception
	{
		_charId = rs.getInt(1);
		_charName = rs.getString(2);
		_charTitle = rs.getString(3);
//		_className = rs.getString(4);
		_accesslevel = rs.getInt(5);
		_pvpKills = rs.getInt(6);
		_pkKills = rs.getInt(7);
		_fame = rs.getInt(8);
		_clanName = rs.getString(9);
		_clanCrest = rs.getString(10);
		_allyCrest = rs.getString(11);
		_raidPoints = rs.getInt(12);
		
		
		final String[] _explode = rs.getString(4).split("_");
		_className = _explode[_explode.length - 1];
	}
	
	public int getCharId()
	{
		return _charId;
	}
	
	public String getCharName()
	{
		return _charName;
	}
	
	public String getCharTitle()
	{
		return _charTitle == null ? "&nbsp;" : _charTitle;
	}
	
	public String getClassName()
	{
		return _className;
	}
	
	public int getPvpKills()
	{
		return _pvpKills;
	}
	
	public int getPkKills() 
	{
		return _pkKills;
	}
	
	public int getFame() 
	{
		return _fame;
	}
	
	public int getRaidPoints()
	{
		return _raidPoints;
	}
	
	public String getClanName()
	{
		return _clanName == null ? "<center>-</center>" : "<center>"+_clanName +"</center>" ;
	}

	public String getClanCrest()
	{
		return String.valueOf(_clanCrest) != null ? "Crest.crest_" + Config.REQUEST_ID + "_" + String.valueOf(_clanCrest) : "L2UI_CH3.ssq_bar1back";
	}

	public String getAllyCrest()
	{
		return String.valueOf(_allyCrest) != null ? "Crest.crest_" + Config.REQUEST_ID + "_" + String.valueOf(_allyCrest) : "L2UI_CH3.ssq_bar2back";
	}
	@Override
	public String toString()
	{
		return _charName;
	}

	public int get_accesslevel()
	{
		return _accesslevel;
	}
}
