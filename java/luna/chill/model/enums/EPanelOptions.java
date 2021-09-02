//package luna.chill.model.enums;
//
//import java.util.function.Consumer;
//
//import luna.chill.model.AutoChill;
//
//public enum EPanelOptions
//{
//	Target_Filter(EPanelOptions::renderTargetFilter);
//	
//	private final Consumer<AutoChill> _chillConsumer;
//	
//	private EPanelOptions(final Consumer<AutoChill> chillConsumer)
//	{
//		_chillConsumer = chillConsumer;
//	}
//	
//	@Override
//	public String toString()
//	{
//		return (ordinal() + 1) + " " + super.toString().replace('_', ' ');
//	}
//	
//	public void render(final AutoChill autoChill)
//	{
//		_chillConsumer.accept(autoChill);
//	}
//	
//	private static void renderTargetFilter(final AutoChill autoChill)
//	{
//		autoChill.renderTargetFilter();
//	}
//
//}
