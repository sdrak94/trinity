//package luna.chill.model.enums;
//
//import net.sf.l2j.Config;
//
//public enum ESearchType
//{
//	Off("FF6363"),
//	Assist("LEVEL"),
//	Close("63FF63"),
//	Near("63FF63"),
//	Far("63FF63");
//
//	private final String _color;
//
//	private ESearchType(final String color)
//	{
//		_color = color;
//	}
//	
//	public String getColor()
//	{
//		return _color;
//	}
//	
//	public int getRange()
//	{
//		switch (this)
//		{
//			case Off:
//				return -1;
//			case Assist:
//				return 0;
//			case Close:
//				return Config.RANGE_CLOSE;
//			case Near:
//				return Config.RANGE_NEAR;
//			case Far:
//				return Config.RANGE_FAR;
//		}
//		return 0;
//	}
//}
