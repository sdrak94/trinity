package inertia.model.enums;

import java.util.function.Consumer;

import inertia.model.Inertia;

public enum EPanelOptions
{
	Target_Filter(EPanelOptions::renderTargetFilter);
	
	private final Consumer<Inertia> _chillConsumer;
	
	private EPanelOptions(final Consumer<Inertia> chillConsumer)
	{
		_chillConsumer = chillConsumer;
	}
	
	@Override
	public String toString()
	{
		return (ordinal() + 1) + " " + super.toString().replace('_', ' ');
	}
	
	public void render(final Inertia autoChill)
	{
		_chillConsumer.accept(autoChill);
	}
	
	private static void renderTargetFilter(final Inertia autoChill)
	{
		autoChill.renderTargetFilter();
	}

}
