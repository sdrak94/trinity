package luna;

import java.sql.ResultSet;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;

public class OfflineInfo implements IPlayerInfo
{
	private final int _objectId;
	private String _charName;
	private String _charTitle;
	
	private int _baseLevel;
	private long _baseExp;
	
	private int _currLevel;
	private long _currExp;
	
	private int _fame;
	private int _pvp;
	private int _pk;
	
	private int _currClassId;
	private int _baseClassId;
	
	private int _clanId;
	private String _clanName;
	
	private boolean inventoryLoaded = false;

	private final int[][] OFFLINE_PAPERDOLL = new int[Inventory.PAPERDOLL_TOTALSLOTS][4];
	
	private int _faceStyle;
	private int _hairStyle;
	private int _hairColor;
	private Race _race;
	private Race _vrace;
	private int _sex;
	private int _accessLevel;
	
	public OfflineInfo(final int objectId)
	{
		_objectId = objectId;
	}
	
	public OfflineInfo(final ResultSet rs) throws Exception
	{
		_objectId = rs.getInt("obj_Id");
		_charName = rs.getString("char_name");
		_charTitle = rs.getString("title");
		
		_baseLevel = rs.getInt("baselevel");
		_baseExp = rs.getLong("baseexp");
		
		_currLevel = rs.getInt("currlevel");
		_currExp = rs.getLong("currexp");
		
		_fame = rs.getInt("fame");
		_pvp = rs.getInt("pvpkills");
		_pk = rs.getInt("pkkills");
		
		_currClassId = rs.getInt("currClassId");
		_baseClassId = rs.getInt("baseClassId");
		
		_clanId = rs.getInt("clan_id");
		_clanName = rs.getString("clan_name");
		
		_faceStyle = rs.getInt("face_style");
		_hairStyle = rs.getInt("hair_style");
		_hairColor = rs.getInt("hair_color");
		_race = Race.values()[rs.getInt("race")];
		_vrace = Race.values()[rs.getInt("vrace")];
		
		_sex = rs.getInt("sex");
		_accessLevel = rs.getInt("access_level");
	}
	
	@Override
	public int getObjectId()
	{
		return _objectId;
	}
	
	@Override	
	public String getPlayerName()
	{
		return _charName;
	}
	
	@Override	
	public String getPlayerTitle()
	{
		return _charTitle;
	}
	
	@Override
	public int getFame()
	{
		return _fame;
	}
	
	@Override
	public int getPvp()
	{
		return _pvp;
	}
	
	@Override
	public int getPk()
	{
		return _pk;
	}

	@Override
	public int getCurrClassId()
	{
		return _currClassId;
	}
	
	@Override
	public int getBaseClassId()
	{
		return _baseClassId;
	}
	
	@Override
	public int getCurrLevel()
	{
		return _currLevel;
	}	
	
	@Override
	public int getBaseLevel()
	{
		return _baseLevel;
	}
	
	public void update(final L2PcInstance player)
	{
		_charName = player.getName();
		_charTitle = player.getTitle();
		_clanName = player.getClanName();
		_currClassId = player.getClassId().getId();
		_fame = player.getFame();
		_pvp = player.getPvpKills();
		_pk = player.getPkKills();
		_currLevel = player.getLevel();
		_baseLevel = player.getStat().getBaseClassLevel();
		
		for (int i = 0; i < OFFLINE_PAPERDOLL.length; i++)
		{
			final var item = player.getInventory().getPaperdollItem(i);

			if (item == null)
			{
				OFFLINE_PAPERDOLL[i][0] = 0;
				OFFLINE_PAPERDOLL[i][1] = 0;
				OFFLINE_PAPERDOLL[i][2] = 0;
				OFFLINE_PAPERDOLL[i][3] = 0;
			}
			else
			{
				OFFLINE_PAPERDOLL[i][0] = item.getItemId();
				OFFLINE_PAPERDOLL[i][1] = item.getEnchantLevel();
				//OFFLINE_PAPERDOLL[i][2] = item.getProgress();
				final var augm = item.getAugmentation();
				OFFLINE_PAPERDOLL[i][3] = augm == null ? 0 : augm.getAugmentationId();
			}
		}
		inventoryLoaded = true;
	}

	@Override
	public int[] getPaperdollInfo(int indx) // load the inventory from OFFLINE player
	{
		if (inventoryLoaded)
			return OFFLINE_PAPERDOLL[indx];
		
		try(final var con = L2DatabaseFactory.getInstance().getConnection();
			final var pst = con.prepareStatement("SELECT * FROM character_inventory_viewer WHERE owner_id = ? AND loc = ?"))
		{
			pst.setInt(1, _objectId);
			pst.setString(2, "PAPERDOLL");
			
			final var rs = pst.executeQuery();
			
			while (rs.next())
			{
				final int locData = rs.getInt("loc_data");
				
				OFFLINE_PAPERDOLL[locData][0] = rs.getInt("item_Id");
				OFFLINE_PAPERDOLL[locData][1] = rs.getInt("enchant_level");
				OFFLINE_PAPERDOLL[locData][2] = rs.getInt("progress");
				OFFLINE_PAPERDOLL[locData][3] = rs.getInt("attributes");
			}
			
			inventoryLoaded = true;
			
			return OFFLINE_PAPERDOLL[indx];
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public long getBaseClassExp()
	{
		return _baseExp;
	}

	@Override
	public long getCurrClassExp()
	{
		return _currExp;
	}
	
	@Override
	public int getClanId()
	{
		return _clanId;
	}
	
	@Override 
	public boolean isClanLeader()
	{
		return false;
	}
	
	@Override
	public String getClanName()
	{
		return _clanName;
	}

	@Override
	public int getFaceStyle()
	{
		return _faceStyle;
	}

	@Override
	public int getHairStyle()
	{
		return _hairStyle;
	}

	@Override
	public int getHairColor()
	{
		return _hairColor;
	}

	@Override
	public Race getRace()
	{
		return _vrace == Race.Dummy ? _race : _vrace;
	}

	@Override
	public int getSex()
	{
		return _sex;
	}
	
	@Override
	public int getAccessLevel()
	{
		return _accessLevel;
	}
}