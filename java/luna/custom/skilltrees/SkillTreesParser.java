package luna.custom.skilltrees;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class SkillTreesParser
{
	final HashMap<Integer, ClassTemplate>	classSkillTrees	= new HashMap<>();
	final ArrayList<Integer>				_allSkills		= new ArrayList<>();
	final ArrayList<Integer>				_itemSkills		= new ArrayList<>();
	
	private void loadXml(L2PcInstance p)
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = new File("data/xml/skilltrees.xml");
		try
		{
			InputSource in = new InputSource(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			in.setEncoding("UTF-8");
			Document doc = factory.newDocumentBuilder().parse(in);
			for (Node a = doc.getFirstChild(); a != null; a = a.getNextSibling())
			{
				if (a.getNodeName().equalsIgnoreCase("list"))
				{
					for (Node n = a.getFirstChild(); n != null; n = n.getNextSibling())
					{
						if (n.getNodeName().equalsIgnoreCase("classes"))
						{
							for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
							{
								final ArrayList<ClassTemplate> classTemp = new ArrayList<>();
								final ArrayList<L2Skill> _skills = new ArrayList<>();
								if (d.getNodeName().equalsIgnoreCase("class"))
								{
									int id = Integer.parseInt(d.getAttributes().getNamedItem("classid").getNodeValue());
									String name = d.getAttributes().getNamedItem("name").getNodeValue();
									for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
									{
										if ("skill".equalsIgnoreCase(e.getNodeName()))
										{
											final NamedNodeMap nnm2 = e.getAttributes();
											final int skillId = Integer.parseInt(nnm2.getNamedItem("skillId").getNodeValue());
											final int skillLvl = Integer.parseInt(nnm2.getNamedItem("level").getNodeValue());
											L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
											if (skill != null)
											{
												_skills.add(skill);
											}
										}
									}
									classTemp.add(new ClassTemplate(id, name, _skills));
									//System.out.println("Loaded: " + _skills.size() + " skills for class: " + name + " (" + id + ")");
								}
								if (!classTemp.isEmpty())
								{
									classTemp.forEach(classInd ->
									{
										classSkillTrees.put(classInd.getId(), classInd);
										//System.out.println("Added class: " + classInd.getId());
										classInd.getSkills().forEach(skill ->
										{
											if (!_allSkills.contains(skill.getId()))
											{
												_allSkills.add(skill.getId());
											}
										});
									});
								}
							}
							//System.out.println("Loaded: " + _allSkills.size() + " Class skills");
						}
						if (n.getNodeName().equalsIgnoreCase("items"))
						{
							for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
							{
								if (d.getNodeName().equalsIgnoreCase("item"))
								{
									final NamedNodeMap nnm2 = d.getAttributes();
									final int skillId = Integer.parseInt(nnm2.getNamedItem("skill").getNodeValue());
									if (skillId != 0)
									{
										if (!_itemSkills.contains(skillId))
											_itemSkills.add(skillId);
									}
								}
							}
							//System.out.println("Loaded: " + _itemSkills.size() + " item skills");
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void Reload(L2PcInstance p)
	{
		classSkillTrees.clear();
		_allSkills.clear();
		_itemSkills.clear();
		loadXml(p);
		// System.out.println("Loaded " + classSkillTrees.size() + " skilltrees");
		// System.out.println("Loaded " + _allSkills.size() + " Skills");
	}
	
	public ArrayList<L2Skill> getClassSkills(L2PcInstance p)
	{
		// System.out.println(" ID: "+ p.getClassId().getId());
		// classSkillTrees.get(p.getClassId().getId()).getSkills().forEach(skill -> {
		// System.out.println(skill.getName() + " - ");
		// });
		return classSkillTrees.get(p.getClassId().getId()).getSkills();
	}
	
	public ArrayList<Integer> getAllSkills(L2PcInstance p)
	{
		return _allSkills;
	}
	public ArrayList<Integer> getAllItemSkills(L2PcInstance p)
	{
		return _itemSkills;
	}
	public static SkillTreesParser getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		private static final SkillTreesParser INSTANCE = new SkillTreesParser();
	}
}
