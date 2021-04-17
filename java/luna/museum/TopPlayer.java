package luna.museum;

public class TopPlayer
{
	int _objectId;
	long _count;
	String _name;
	
	public TopPlayer(final int objectId, final String name, final long count)
	{
		_objectId = objectId;
		_name = name;
		_count = count;
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public long getCount()
	{
		return _count;
	}
}
