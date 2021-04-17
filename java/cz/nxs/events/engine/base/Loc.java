package cz.nxs.events.engine.base;

import cz.nxs.l2j.CallBack;

public class Loc
{
	private int _x;
	private int _y;
	private int _z;
	private int _heading;

	public Loc(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}

	public Loc(int x, int y, int z, int heading)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
	}
	
	public void addRadius(int radius)
	{
		_x += (CallBack.getInstance().getOut().random(radius * 2) - radius);
		_y += (CallBack.getInstance().getOut().random(radius * 2) - radius);
	}

	public int getX()
	{
		return _x;
	}

	public int getY()
	{
		return _y;
	}

	public int getZ()
	{
		return _z;
	}

	public int getHeading()
	{
		return _heading;
	}
}
