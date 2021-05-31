package luna.chill.model.enums;

public enum EActionPriority
{

	Highest,
	High,
	Medium,
	Low,
	Lowest;
	
	@Override
	public String toString()
	{
		return (ordinal() + 1) + " " + super.toString();
	}
}
