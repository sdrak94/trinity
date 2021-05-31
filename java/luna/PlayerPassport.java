package luna;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;

public class PlayerPassport implements IPlayerInfo
{
	private static final int[] emptyInfo = new int[] { 0, 0, 0 };
	
	private final int _objectId;
	private final OfflineInfo _offlineInfo;
	
	private L2PcInstance _cachedInstance;
	private boolean _online;
	
	public PlayerPassport(final int objectId, final OfflineInfo offlineInfo)
	{
		_objectId = objectId;
		_offlineInfo = offlineInfo;
	}
		
	public L2PcInstance getPlayer()
	{
		return _cachedInstance;
	}
	
	public L2PcInstance getOnlinePlayer()
	{
		final var player = getPlayer();
		if ( player == null || player.isOnline() != 1 )
			return null;
		return player;
	}
	
	public L2PcInstance getKnownPlayer()
	{
		return _cachedInstance;
	}
	
	public boolean isOnline()
	{
		return _online;
	}
	
	public void login(final L2PcInstance player)
	{
		_offlineInfo.update(player);
		_online = true;
		_cachedInstance = player;
	}
	
	public void reset(L2PcInstance player)
	{
		_offlineInfo.update(player);
		_online = true;
		_cachedInstance = player;
	}

	public void logout(final L2PcInstance player)
	{
		_offlineInfo.update(player);
		_online = false;
		_cachedInstance = null;
	}
	
	@Override
	public int getObjectId()
	{
		return _objectId;
	}
	
	@Override
	public String getPlayerName()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getName();
		return _offlineInfo.getPlayerName();
	}
	
	@Override
	public String getPlayerTitle()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getTitle();
		return _offlineInfo.getPlayerTitle();
	}
	
	@Override
	public String getClanName()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getClanName();
		return _offlineInfo.getClanName();
	}

	@Override
	public int getClanId()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getClanId();
		return _offlineInfo.getClanId();
	}

	@Override
	public boolean isClanLeader()
	{
		if (_cachedInstance != null)
			return _cachedInstance.isClanLeader();
		return _offlineInfo.isClanLeader();
	}
	
	@Override
	public int getFame()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getFame();
		return _offlineInfo.getFame();
	}
	
	@Override
	public int getPvp()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getPvpKills();
		return _offlineInfo.getPvp();
	}
	
	@Override
	public int getPk()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getPkKills();
		return _offlineInfo.getPk();
	}

	@Override
	public int getCurrClassId()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getClassId().getId();
		return _offlineInfo.getCurrClassId();
	}

	@Override
	public int getBaseClassId()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getBaseClass();
		return _offlineInfo.getBaseClassId();
	}
	
	@Override
	public String toString()
	{
		return getPlayerName() + "'s Passport";
	}

	@Override
	public int getCurrLevel()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getLevel();
		return _offlineInfo.getCurrLevel();
	}

	@Override
	public int getBaseLevel()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getStat().getBaseClassLevel();
		return _offlineInfo.getBaseLevel();
	}
	
	public void sendMessage(final String message)
	{
		final L2PcInstance player = getPlayer();
		if (player != null)
			player.sendMessage(message);
	}
	
//	public PlayerDriver createDriver()
//	{
//		final var player = getPlayer();
//		if (player == null)
//			return null;
//		return new PlayerDriver(player);
//	}
	
	@Override
	public int[] getPaperdollInfo(int indx)
	{
		final var player = getPlayer();
		if (player != null)
		{
			final var item = player.getInventory().getPaperdollItem(indx);
			if (item != null)
				return new int[] {item.getItemId() , item.getEnchantLevel()};
			//return new int[] {item.getItemId() , item.getEnchantLevel(), item.getProgress()};
			else
				return emptyInfo;
		}
		
		return _offlineInfo.getPaperdollInfo(indx);
	}

	@Override
	public long getBaseClassExp()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getStat().getExp();
		return _offlineInfo.getBaseClassExp();
	}

	@Override
	public long getCurrClassExp()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getExp();
		return _offlineInfo.getCurrClassExp();
	}
	
	@Override
	public int getFaceStyle()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getAppearance().getFace();
		return _offlineInfo.getFaceStyle();
	}

	@Override
	public int getHairStyle()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getAppearance().getHairStyle();
		return _offlineInfo.getHairStyle();
	}

	@Override
	public int getHairColor()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getAppearance().getHairColor();
		return _offlineInfo.getHairColor();
	}

	@Override
	public Race getRace()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getRace();
		return _offlineInfo.getRace();
	}

	@Override
	public int getSex()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getAppearance().getSex() ? 1 : 0;
		return _offlineInfo.getSex();
	}

	@Override
	public int getAccessLevel()
	{
		if (_cachedInstance != null)
			return _cachedInstance.getAccessLevel().getLevel();
		return _offlineInfo.getAccessLevel();
	}
	
	public boolean isAccessZero()
	{
		return getAccessLevel() == 0;
	}
}
