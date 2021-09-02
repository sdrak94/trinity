package inertia.model.behave;

import inertia.model.IInertiaBehave;
import inertia.model.Inertia;

public abstract class AbstractBehave implements IInertiaBehave
{
	protected Inertia _autoChill;
	
	public void setAutoChill(final Inertia autoChill)
	{
		_autoChill = autoChill;
	}
	
	public Inertia getAutoChill()
	{
		return _autoChill;
	}
}
