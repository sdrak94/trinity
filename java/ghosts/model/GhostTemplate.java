package ghosts.model;

import java.util.ArrayList;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import inertia.model.IInertiaBehave;
import inertia.model.behave.AbstractBehave;
import inertia.model.behave.PlayerBehave;
import inertia.model.extensions.tables.InertiaConfigurationTable;
import inertia.model.extensions.templates.InertiaConfiguration;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.item.L2Henna;
import net.sf.l2j.util.Rnd;

public class GhostTemplate
{
	private final ArrayList<L2Henna>				_hennas				= new ArrayList<>(3);
	private final String						_templateId;
	private final EGhostTier					_ghostTier;
	private final ClassId						_classId;
	private final ArrayList<InventoryTemplate>	_inventoryTemplates	= new ArrayList<>();
	private final ArrayList<Race>				_races				= new ArrayList<>();
	private int[]							_pvps = {0,0};
	private final int							_level;
	private final InertiaConfiguration		_inertiaConfiguration;
	private final ArrayList<Class<?>>			_extensions			= new ArrayList<>();
	
//	private final ArrayList<Supplier<IInertiaBehave>> _extendedBehaviors = new ArrayList<>();
	
	public GhostTemplate(final Node n)
	{
		final NamedNodeMap nnm = n.getAttributes();
		final StatsSet statsSet = new StatsSet();
		_templateId = nnm.getNamedItem("id").getNodeValue();
		final String className = nnm.getNamedItem("classId").getNodeValue();
		_classId = Enum.valueOf(ClassId.class, className);
		final String tierName = nnm.getNamedItem("tier").getNodeValue();
		_ghostTier = Enum.valueOf(EGhostTier.class, tierName);
		for (Node n1 = n.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
		{
			if ("inventory".equalsIgnoreCase(n1.getNodeName()))
			{
				final InventoryTemplate inventoryTemplate = new InventoryTemplate(n1);
				_inventoryTemplates.add(inventoryTemplate);
			}
			else if ("hennas".equalsIgnoreCase(n1.getNodeName()))
			{
				for (Node n2 = n1.getFirstChild(); n2 != null; n2 = n2.getNextSibling())
				{
					if ("henna".equalsIgnoreCase(n2.getNodeName()))
					{
						final NamedNodeMap nnm2 = n2.getAttributes();
						final int hennaId = Integer.parseInt(nnm2.getNamedItem("id").getNodeValue());
						final L2Henna henna = HennaTable.getInstance().getTemplate(hennaId);
						_hennas.add(henna);
					}
				}
			}
			else if ("races".equalsIgnoreCase(n1.getNodeName()))
			{
				for (Node n2 = n1.getFirstChild(); n2 != null; n2 = n2.getNextSibling())
				{
					if ("race".equalsIgnoreCase(n2.getNodeName()))
					{
						final NamedNodeMap nnm2 = n2.getAttributes();
						final Race pcRace = Enum.valueOf(Race.class, nnm2.getNamedItem("name").getNodeValue());
						_races.add(pcRace);
					}
				}
			}
			else if ("stats".equalsIgnoreCase(n1.getNodeName()))
			{
				for (Node n2 = n1.getFirstChild(); n2 != null; n2 = n2.getNextSibling())
				{
					if ("set".equalsIgnoreCase(n2.getNodeName()))
					{
						final NamedNodeMap nnm2 = n2.getAttributes();
						statsSet.set(nnm2.getNamedItem("name").getNodeValue(), nnm2.getNamedItem("value").getNodeValue());
					}
				}
			}
			else if ("InertiaExtensions".equalsIgnoreCase(n1.getNodeName()))
			{
				for (Node n2 = n1.getFirstChild(); n2 != null; n2 = n2.getNextSibling())
				{
					if ("ext".equalsIgnoreCase(n2.getNodeName()))
					{
						final NamedNodeMap nnm2 = n2.getAttributes();
						final String clsName = nnm2.getNamedItem("name").getNodeValue();
						Class<?> extClass;
						try
						{
							extClass = Class.forName("inertia.model.extensions." + clsName);
							if (extClass != null)
								_extensions.add(extClass);
						}
						catch (ClassNotFoundException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
		_pvps = statsSet.getIntegerArray("pvp", "-");
		_level = statsSet.getInt("level", 85);
		final String inertiaConfigurationId = statsSet.getString("inertia_configuration_id", null);
		_inertiaConfiguration = inertiaConfigurationId == null ? null : InertiaConfigurationTable.getInstance().getById(inertiaConfigurationId);
		System.out.println("added " + _extensions.size() + " extensions.");
	}
	
	private Class<? extends PlayerBehave> getClassFromTemplate(String cls)
	{
		Class<? extends PlayerBehave> extClass = null;
		try
		{
			extClass = (Class<? extends PlayerBehave>) Class.forName("drake.controllers.inertia.model.extensions." + cls).asSubclass(PlayerBehave.class);
		}
		catch (ClassNotFoundException | ClassCastException e)
		{
			e.printStackTrace();
		}
		return extClass;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getPvPs()
	{
		return Rnd.get(_pvps[0], _pvps[1]);
	}
	
	public InertiaConfiguration getInertiaConfiguration()
	{
		return _inertiaConfiguration;
	}
	
	public EGhostTier getGhostTier()
	{
		return _ghostTier;
	}
	
	public String getTemplateId()
	{
		return _templateId;
	}
	
	public ClassId getClassId()
	{
		return _classId;
	}
	
	public ArrayList<L2Henna> getHennas()
	{
		return _hennas;
	}
	
	public Race pickRace()
	{
		if (_races.isEmpty())
			return _classId.getRace();
		final Race race = Rnd.get(_races);
		return race.real();
	}
	
	public ArrayList<InventoryTemplate> getInventoryTemplates()
	{
		return _inventoryTemplates;
	}
	
	public boolean hasExtensions()
	{
		return !_extensions.isEmpty();
	}
	
	public void addExtensions(final IInertiaBehave behave)
	{
		for (final var cl : _extensions)
		{
			try
			{
				final IInertiaBehave ext = cl.asSubclass(AbstractBehave.class).getConstructor().newInstance();
				behave.expand(ext);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
