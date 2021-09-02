package inertia.model.extensions.templates.model;

import inertia.model.extensions.templates.InertiaActionTemplate;

public class InertiaAction
{
	
	private final InertiaActionTemplate _actionTemplate;
	
	public InertiaAction(final InertiaActionTemplate actionTemplate)
	{
		_actionTemplate = actionTemplate;
	}

	public InertiaActionTemplate getTemplate()
	{
		return _actionTemplate;
	}
}
