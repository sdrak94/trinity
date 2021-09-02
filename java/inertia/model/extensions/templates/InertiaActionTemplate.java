package inertia.model.extensions.templates;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class InertiaActionTemplate
{
	private final String	_alias;
	private final int		_skillId;
	private final int		_priority;
	private final String	_nextAction;
	private final int		_selfHp;
	private final int		_repeat;
	
	public InertiaActionTemplate(final Node n)
	{
		final NamedNodeMap nnm2 = n.getAttributes();
		_alias = nnm2.getNamedItem("alias").getNodeValue();
		_skillId = Integer.parseInt(nnm2.getNamedItem("id").getNodeValue());
		_priority = nnm2.getNamedItem("priority") == null ? -1 : Integer.parseInt(nnm2.getNamedItem("priority").getNodeValue());
		_repeat = nnm2.getNamedItem("repeat") == null ? -1 : Integer.parseInt(nnm2.getNamedItem("repeat").getNodeValue());
		_nextAction = nnm2.getNamedItem("nextAction") == null ? "EMPTY" : nnm2.getNamedItem("nextAction").getNodeValue();
		_selfHp = nnm2.getNamedItem("selfhp") == null ? -1 : Integer.parseInt(nnm2.getNamedItem("selfhp").getNodeValue());
		
	}
	
	public String getAlias()
	{
		return _alias;
	}

	public int getSkillId()
	{
		return _skillId;
	}

	public int getPriority()
	{
		return _priority;
	}

	public String getNextAction()
	{
		return _nextAction;
	}

	public int getSelfHp()
	{
		return _selfHp;
	}

	public int getRepeat()
	{
		return _repeat;
	}
}
