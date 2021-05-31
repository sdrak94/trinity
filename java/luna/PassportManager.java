package luna;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;


public class PassportManager
{
	private final ConcurrentHashMap<Integer, PlayerPassport> _passports = new ConcurrentHashMap<>();

//	private final HashMap<Comparator<PlayerPassport>, ArrayList<PlayerPassport>> _passportCache = new HashMap<>();
	private final HashMap<Comparator<PlayerPassport>, CopyOnWriteArrayList<PlayerPassport>> _passportCache2 = new HashMap<>();

	public PlayerPassport fetch(final int objectId, OfflineInfo offlineInfo)
	{
		PlayerPassport passport = _passports.get(objectId);
		if (passport == null)
		{
			if (offlineInfo == null)
				offlineInfo = new OfflineInfo(objectId);
			passport = new PlayerPassport(objectId, offlineInfo);
			
			registerPassport(objectId, passport);

			return passport;
		}
		return passport;
	}
	
	public PlayerPassport getByName(final String playerName)
	{
		if (playerName == null)
			return null;
		
		return _passports.values().stream().filter((p) ->
		{
			return p.getPlayerName().equalsIgnoreCase(playerName);
		}).findAny().orElse(null);
	}
	
	public PlayerPassport fetch(final int objectId)
	{
		return fetch(objectId, null);
	}
	
	public void registerPassport(final int objectId, final PlayerPassport playerPassport)
	{
		_passports.put(objectId, playerPassport);
		
		for (final var cached : _passportCache2.values())
			cached.add(playerPassport);
	}
	
	public PlayerPassport fetch(final L2PcInstance player)
	{
		final PlayerPassport passport = fetch(player.getObjectId(), null);
		passport.reset(player);
		return passport;
	}
	
	public PlayerPassport[] copySorted(final Comparator<PlayerPassport> comparator)
	{
		final var cached = sortedCached2(comparator);
		final var ret = cached.stream().filter(PlayerPassport::isAccessZero).toArray(PlayerPassport[]::new);
		
		return ret;
	}
	
	public PlayerPassport[] copySorted(final Comparator<PlayerPassport> comparator, Predicate<PlayerPassport> filter)
	{
		return _passports.values().parallelStream().filter(filter).sorted(comparator).toArray(PlayerPassport[]::new);
	}
	
	public int getPosition(final PlayerPassport playePassport, final Comparator<PlayerPassport> comparator)
	{
		final var cached = sortedCached2(comparator);
		final var ret = cached.stream().filter(PlayerPassport::isAccessZero).toArray(PlayerPassport[]::new);
		
		
		return Arrays.binarySearch(ret, playePassport, comparator);
	}

//	private ArrayList<PlayerPassport> sortedCached(final Comparator<PlayerPassport> comparator)
//	{
//		var cached = _passportCache.get(comparator);
//		if (cached == null)
//		{
//			cached = new ArrayList<>(_passports.values());
//			_passportCache.put(comparator, cached);
//		}
//		cached.sort(comparator.thenComparing(TopComparator.FAME));
//		return cached;
//	}
	
	private CopyOnWriteArrayList<PlayerPassport> sortedCached2(final Comparator<PlayerPassport> comparator)
	{
		var cached = _passportCache2.get(comparator);
		if (cached == null)
		{
			cached = new CopyOnWriteArrayList<>(_passports.values());
			_passportCache2.put(comparator, cached);
		}
		cached.sort(comparator);
		return cached;
	}
	
	private static final class InstanceHolder
	{
		private static final PassportManager _instance = new PassportManager();
	}
	
	public static PassportManager getInstance()
	{
		return InstanceHolder._instance;
	}
}
