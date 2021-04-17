package luna.museum;

import java.util.HashMap;
import java.util.Map;

public class MuseumPlayer
{
	int _objectId;
	String _name;
	HashMap<String, long[]> _data;
	
	public MuseumPlayer(final int objectId, final String name, final HashMap<String, long[]> data)
	{
		_objectId = objectId;
		_name = name;
		_data = data;
	}
	
	public long getData(final RefreshTime time, final String type)
	{
		if (!_data.containsKey(type))
		{
			return 0L;
		}
		return _data.get(type)[time.ordinal()];
	}
	
	public long[] getData(final String type)
	{
		if (!_data.containsKey(type))
		{
			return null;
		}
		return _data.get(type);
	}
	
	public void resetData(final RefreshTime time)
	{
		if (time.equals(RefreshTime.Total))
		{
			return;
		}
		final HashMap<String, long[]> data = new HashMap<>();
		long[] d =
		{
			0L,
			0L,
			0L,
			0L
		};
		for (final Map.Entry<String, long[]> entry : _data.entrySet())
		{
			d = entry.getValue();
			d[time.ordinal()] = 0L;
			data.put(entry.getKey(), d);
		}
		_data = data;
	}
	
	public HashMap<String, long[]> getData()
	{
		return _data;
	}
	
	public void addData(final String type, final long data)
	{
		long[] d =
		{
			0L,
			0L,
			0L,
			0L
		};
		if (this.getData(type) != null)
		{
			d = this.getData(type);
		}
		final long[] array = d;
		final int n = 0;
		array[n] += data;
		final long[] array2 = d;
		final int n2 = 1;
		array2[n2] += data;
		final long[] array3 = d;
		final int n3 = 2;
		array3[n3] += data;
		final long[] array4 = d;
		final int n4 = 3;
		array4[n4] += data;
		_data.put(type, d);
	}
}
