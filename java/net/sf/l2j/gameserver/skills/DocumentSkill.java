/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.skills;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

/**
 * @author mkizub
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DocumentSkill extends DocumentBase {

    public class Skill
    {
        public int                  id;
        public String               name;
        public StatsSet[]           sets;
        public StatsSet[]           enchsets1;
        public StatsSet[]           enchsets2;
        public StatsSet[]           enchsets3;
        public StatsSet[]           enchsets4;
        public StatsSet[]           enchsets5;
        public StatsSet[]           enchsets6;
        public StatsSet[]           enchsets7;
        public StatsSet[]           enchsets8;
        public int                  currentLevel;
        public List<L2Skill>   skills          = new FastList<L2Skill>();
        public List<L2Skill>   currentSkills   = new FastList<L2Skill>();
    }

    private Skill _currentSkill;
    private List<L2Skill> _skillsInFile  = new FastList<L2Skill>();

	DocumentSkill(File file)
	{
		super(file);
	}

    private void setCurrentSkill(Skill skill)
    {
        _currentSkill    = skill;
    }

    @Override
	protected StatsSet getStatsSet()
	{
		return _currentSkill.sets[_currentSkill.currentLevel];
	}

	protected List<L2Skill> getSkills()
	{
        return _skillsInFile;
	}

	@Override
	protected String getTableValue(String name)
	{
		try
        {
            return _tables.get(name)[_currentSkill.currentLevel];
        }
		catch (RuntimeException e)
        {
            _log.log(Level.SEVERE, "Error in table: "+name+" of Skill Id "+_currentSkill.id, e);
            return "";
        }
	}

	@Override
	protected String getTableValue(String name, int idx)
	{
		try
        {
            return _tables.get(name)[idx-1];
        }
		catch (RuntimeException e)
        {
            _log.log(Level.SEVERE, "wrong level count in skill Id "+_currentSkill.id, e);
            return "";
        }
	}

	@Override
	protected void parseDocument(Document doc)
	{
        for (Node n=doc.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if ("list".equalsIgnoreCase(n.getNodeName()))
            {
                for (Node d=n.getFirstChild(); d != null; d = d.getNextSibling())
                {
                    if ("skill".equalsIgnoreCase(d.getNodeName()))
                    {
                        setCurrentSkill(new Skill());
                        parseSkill(d);
                        _skillsInFile.addAll(_currentSkill.skills);
                        resetTable();
                    }
                }
            }
            else if ("skill".equalsIgnoreCase(n.getNodeName()))
            {
                setCurrentSkill(new Skill());
                parseSkill(n);
                _skillsInFile.addAll(_currentSkill.skills);
            }
        }
	}

    protected void parseSkill(Node n)
	{
    	NamedNodeMap attrs = n.getAttributes();
        int enchantLevels1 = 0;
        int enchantLevels2 = 0;
        int enchantLevels3 = 0;
        int enchantLevels4 = 0;
        int enchantLevels5 = 0;
        int enchantLevels6 = 0;
        int enchantLevels7 = 0;
        int enchantLevels8 = 0;
		int skillId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
		String skillName = attrs.getNamedItem("name").getNodeValue();
		String levels = attrs.getNamedItem("levels").getNodeValue();
		int lastLvl = Integer.parseInt(levels);
        if (attrs.getNamedItem("enchantLevels1") != null)
            enchantLevels1 = Integer.parseInt(attrs.getNamedItem("enchantLevels1").getNodeValue());
        if (attrs.getNamedItem("enchantLevels2") != null)
            enchantLevels2 = Integer.parseInt(attrs.getNamedItem("enchantLevels2").getNodeValue());
        if (attrs.getNamedItem("enchantLevels3") != null)
            enchantLevels3 = Integer.parseInt(attrs.getNamedItem("enchantLevels3").getNodeValue());
        if (attrs.getNamedItem("enchantLevels4") != null)
            enchantLevels4 = Integer.parseInt(attrs.getNamedItem("enchantLevels4").getNodeValue());
        if (attrs.getNamedItem("enchantLevels5") != null)
            enchantLevels5 = Integer.parseInt(attrs.getNamedItem("enchantLevels5").getNodeValue());
        if (attrs.getNamedItem("enchantLevels6") != null)
            enchantLevels6 = Integer.parseInt(attrs.getNamedItem("enchantLevels6").getNodeValue());
        if (attrs.getNamedItem("enchantLevels7") != null)
            enchantLevels7 = Integer.parseInt(attrs.getNamedItem("enchantLevels7").getNodeValue());
        if (attrs.getNamedItem("enchantLevels8") != null)
            enchantLevels8 = Integer.parseInt(attrs.getNamedItem("enchantLevels8").getNodeValue());

        _currentSkill.id     = skillId;
        _currentSkill.name   = skillName;
        _currentSkill.sets   = new StatsSet[lastLvl];
        _currentSkill.enchsets1   = new StatsSet[enchantLevels1];
        _currentSkill.enchsets2   = new StatsSet[enchantLevels2];
        _currentSkill.enchsets3   = new StatsSet[enchantLevels3];
        _currentSkill.enchsets4   = new StatsSet[enchantLevels4];
        _currentSkill.enchsets5   = new StatsSet[enchantLevels5];
        _currentSkill.enchsets6   = new StatsSet[enchantLevels6];
        _currentSkill.enchsets7   = new StatsSet[enchantLevels7];
        _currentSkill.enchsets8   = new StatsSet[enchantLevels8];

		for (int i=0; i < lastLvl; i++)
		{
            _currentSkill.sets[i] = new StatsSet();
            _currentSkill.sets[i].set("skill_id", _currentSkill.id);
            _currentSkill.sets[i].set("level",    i+1);
            _currentSkill.sets[i].set("name",     _currentSkill.name);
		}

		if (_currentSkill.sets.length != lastLvl)
			throw new RuntimeException("Skill id="+skillId+" number of levels missmatch, "+lastLvl+" levels expected");

		Node first = n.getFirstChild();
		for (n=first; n != null; n = n.getNextSibling())
		{
			if ("table".equalsIgnoreCase(n.getNodeName()))
				parseTable(n);
		}
		for (int i=1; i <= lastLvl; i++)
		{
			for (n=first; n != null; n = n.getNextSibling())
			{
				if ("set".equalsIgnoreCase(n.getNodeName()))
					parseBeanSet(n, _currentSkill.sets[i-1], i);
			}
		}
		for (int i=0; i < enchantLevels1; i++)
        {
            _currentSkill.enchsets1[i] = new StatsSet();
            _currentSkill.enchsets1[i].set("skill_id", _currentSkill.id);
            //currentSkill.enchsets1[i] = currentSkill.sets[currentSkill.sets.length-1];
            _currentSkill.enchsets1[i].set("level",    i+101);
            _currentSkill.enchsets1[i].set("name",     _currentSkill.name);
            //currentSkill.enchsets1[i].set("skillType",     "NOTDONE");

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("set".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets1[i], _currentSkill.sets.length);
            }

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant1".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets1[i], i+1);
            }
        }

        if (_currentSkill.enchsets1.length != enchantLevels1)
            throw new RuntimeException("Skill id="+skillId+" number of levels missmatch, "+enchantLevels1+" levels expected");

        for (int i=0; i < enchantLevels2; i++)
        {
            _currentSkill.enchsets2[i] = new StatsSet();
            //currentSkill.enchsets2[i] = currentSkill.sets[currentSkill.sets.length-1];
            _currentSkill.enchsets2[i].set("skill_id", _currentSkill.id);
            _currentSkill.enchsets2[i].set("level",    i+201);
            _currentSkill.enchsets2[i].set("name",     _currentSkill.name);
            //currentSkill.enchsets2[i].set("skillType",     "NOTDONE");

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("set".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets2[i], _currentSkill.sets.length);
            }

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant2".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets2[i], i+1);
            }
        }

        if (_currentSkill.enchsets2.length != enchantLevels2)
            throw new RuntimeException("Skill id="+skillId+" number of levels missmatch, "+enchantLevels2+" levels expected");

        for (int i=0; i < enchantLevels3; i++)
        {
            _currentSkill.enchsets3[i] = new StatsSet();
            _currentSkill.enchsets3[i].set("skill_id", _currentSkill.id);
            _currentSkill.enchsets3[i].set("level",    i+301);
            _currentSkill.enchsets3[i].set("name",     _currentSkill.name);

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("set".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets3[i], _currentSkill.sets.length);
            }

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant3".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets3[i], i+1);
            }
        }

        if (_currentSkill.enchsets3.length != enchantLevels3)
            throw new RuntimeException("Skill id="+skillId+" number of levels missmatch, "+enchantLevels3+" levels expected");

        for (int i=0; i < enchantLevels4; i++)
        {
            _currentSkill.enchsets4[i] = new StatsSet();
            _currentSkill.enchsets4[i].set("skill_id", _currentSkill.id);
            _currentSkill.enchsets4[i].set("level",    i+401);
            _currentSkill.enchsets4[i].set("name",     _currentSkill.name);

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("set".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets4[i], _currentSkill.sets.length);
            }

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant4".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets4[i], i+1);
            }
        }

        if (_currentSkill.enchsets4.length != enchantLevels4)
            throw new RuntimeException("Skill id="+skillId+" number of levels missmatch, "+enchantLevels4+" levels expected");

        for (int i=0; i < enchantLevels5; i++)
        {
            _currentSkill.enchsets5[i] = new StatsSet();
            _currentSkill.enchsets5[i].set("skill_id", _currentSkill.id);
            _currentSkill.enchsets5[i].set("level",    i+501);
            _currentSkill.enchsets5[i].set("name",     _currentSkill.name);

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("set".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets5[i], _currentSkill.sets.length);
            }

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant5".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets5[i], i+1);
            }
        }

        if (_currentSkill.enchsets5.length != enchantLevels5)
            throw new RuntimeException("Skill id="+skillId+" number of levels missmatch, "+enchantLevels5+" levels expected");

        for (int i=0; i < enchantLevels6; i++)
        {
            _currentSkill.enchsets6[i] = new StatsSet();
            _currentSkill.enchsets6[i].set("skill_id", _currentSkill.id);
            _currentSkill.enchsets6[i].set("level",    i+601);
            _currentSkill.enchsets6[i].set("name",     _currentSkill.name);

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("set".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets6[i], _currentSkill.sets.length);
            }

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant6".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets6[i], i+1);
            }
        }

        if (_currentSkill.enchsets6.length != enchantLevels6)
            throw new RuntimeException("Skill id="+skillId+" number of levels missmatch, "+enchantLevels6+" levels expected");
        
        for (int i=0; i < enchantLevels7; i++)
        {
            _currentSkill.enchsets7[i] = new StatsSet();
            _currentSkill.enchsets7[i].set("skill_id", _currentSkill.id);
            _currentSkill.enchsets7[i].set("level",    i+701);
            _currentSkill.enchsets7[i].set("name",     _currentSkill.name);

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("set".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets7[i], _currentSkill.sets.length);
            }

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant7".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets7[i], i+1);
            }
        }

        if (_currentSkill.enchsets7.length != enchantLevels7)
            throw new RuntimeException("Skill id="+skillId+" number of levels missmatch, "+enchantLevels7+" levels expected");
        
        for (int i=0; i < enchantLevels8; i++)
        {
            _currentSkill.enchsets8[i] = new StatsSet();
            _currentSkill.enchsets8[i].set("skill_id", _currentSkill.id);
            _currentSkill.enchsets8[i].set("level",    i+801);
            _currentSkill.enchsets8[i].set("name",     _currentSkill.name);

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("set".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets8[i], _currentSkill.sets.length);
            }

            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant8".equalsIgnoreCase(n.getNodeName()))
                    parseBeanSet(n, _currentSkill.enchsets8[i], i+1);
            }
        }

        if (_currentSkill.enchsets8.length != enchantLevels8)
            throw new RuntimeException("Skill id="+skillId+" number of levels missmatch, "+enchantLevels8+" levels expected");

        makeSkills();
		for (int i=0; i < lastLvl; i++)
		{
            _currentSkill.currentLevel = i;
			for (n=first; n != null; n = n.getNextSibling())
			{
				if ("cond".equalsIgnoreCase(n.getNodeName()))
				{
					Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
					Node msg = n.getAttributes().getNamedItem("msg");
					Node msgId = n.getAttributes().getNamedItem("msgId");
					if (condition != null && msg != null)
						condition.setMessage(msg.getNodeValue());
					else if (condition != null && msgId != null)
					{
						condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
						Node addName = n.getAttributes().getNamedItem("addName");
						if (addName != null && Integer.decode(getValue(msgId.getNodeValue(), null)) > 0)
							condition.addName();
					}
                    _currentSkill.currentSkills.get(i).attach(condition, false);
				}
				if ("for".equalsIgnoreCase(n.getNodeName()))
				{
					parseTemplate(n, _currentSkill.currentSkills.get(i));
				}
			}
		}
        for (int i=lastLvl; i < lastLvl+enchantLevels1; i++)
        {
            _currentSkill.currentLevel = i-lastLvl;
            boolean found = false;
            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant1cond".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                	Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                    Node msg = n.getAttributes().getNamedItem("msg");
                    if (condition != null && msg != null)
                        condition.setMessage(msg.getNodeValue());
                    _currentSkill.currentSkills.get(i).attach(condition,false);
                }
                if ("enchant1for".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                	parseTemplate(n, _currentSkill.currentSkills.get(i));
                }
            }
           	// If none found, the enchanted skill will take effects from maxLvL of norm skill
        	if (!found)
        	{
        		_currentSkill.currentLevel = lastLvl-1;
        		for (n=first; n != null; n = n.getNextSibling())
        		{
        			if ("cond".equalsIgnoreCase(n.getNodeName()))
        			{
        				Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
        				Node msg = n.getAttributes().getNamedItem("msg");
        				if (condition != null && msg != null)
        					condition.setMessage(msg.getNodeValue());
        				_currentSkill.currentSkills.get(i).attach(condition,false);
        			}
        			if ("for".equalsIgnoreCase(n.getNodeName()))
        			{
        				parseTemplate(n, _currentSkill.currentSkills.get(i));
        			}
        		}
        	}
        }
        for (int i=lastLvl+enchantLevels1; i < lastLvl+enchantLevels1+enchantLevels2; i++)
        {
        	boolean found = false;
        	_currentSkill.currentLevel = i-lastLvl-enchantLevels1;
            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant2cond".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                	Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                    Node msg = n.getAttributes().getNamedItem("msg");
                    if (condition != null && msg != null)
                        condition.setMessage(msg.getNodeValue());
                    _currentSkill.currentSkills.get(i).attach(condition,false);
                }
                if ("enchant2for".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                	parseTemplate(n, _currentSkill.currentSkills.get(i));
                }
            }
            // If none found, the enchanted skill will take effects from maxLvL of norm skill
            if(!found)
            {
            	_currentSkill.currentLevel = lastLvl-1;
            	for (n=first; n != null; n = n.getNextSibling())
            	{
            		if ("cond".equalsIgnoreCase(n.getNodeName()))
            		{
            			Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
            			Node msg = n.getAttributes().getNamedItem("msg");
            			if (condition != null && msg != null)
            				condition.setMessage(msg.getNodeValue());
            			_currentSkill.currentSkills.get(i).attach(condition,false);
            		}
            		if ("for".equalsIgnoreCase(n.getNodeName()))
            		{
            			parseTemplate(n, _currentSkill.currentSkills.get(i));
            		}
            	}
            }
        }
        for (int i=lastLvl+enchantLevels1+enchantLevels2; i < lastLvl+enchantLevels1+enchantLevels2+enchantLevels3; i++)
        {
            boolean found = false;
            _currentSkill.currentLevel = i-lastLvl-enchantLevels1-enchantLevels2;
            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant3cond".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                    Node msg = n.getAttributes().getNamedItem("msg");
                    if (condition != null && msg != null)
                        condition.setMessage(msg.getNodeValue());
                    _currentSkill.currentSkills.get(i).attach(condition,false);
                }
                if ("enchant3for".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    parseTemplate(n, _currentSkill.currentSkills.get(i));
                }
            }
            // If none found, the enchanted skill will take effects from maxLvL of norm skill
            if(!found)
            {
                _currentSkill.currentLevel = lastLvl-1;
                for (n=first; n != null; n = n.getNextSibling())
                {
                    if ("cond".equalsIgnoreCase(n.getNodeName()))
                    {
                        Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                        Node msg = n.getAttributes().getNamedItem("msg");
                        if (condition != null && msg != null)
                            condition.setMessage(msg.getNodeValue());
                        _currentSkill.currentSkills.get(i).attach(condition,false);
                    }
                    if ("for".equalsIgnoreCase(n.getNodeName()))
                    {
                        parseTemplate(n, _currentSkill.currentSkills.get(i));
                    }
                }
            }
        }
        for (int i=lastLvl+enchantLevels1+enchantLevels2+enchantLevels3; i < lastLvl+enchantLevels1+enchantLevels2+enchantLevels3+enchantLevels4; i++)
        {
            boolean found = false;
            _currentSkill.currentLevel = i-lastLvl-enchantLevels1-enchantLevels2-enchantLevels3;
            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant4cond".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                    Node msg = n.getAttributes().getNamedItem("msg");
                    if (condition != null && msg != null)
                        condition.setMessage(msg.getNodeValue());
                    _currentSkill.currentSkills.get(i).attach(condition,false);
                }
                if ("enchant4for".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    parseTemplate(n, _currentSkill.currentSkills.get(i));
                }
            }
            // If none found, the enchanted skill will take effects from maxLvL of norm skill
            if(!found)
            {
                _currentSkill.currentLevel = lastLvl-1;
                for (n=first; n != null; n = n.getNextSibling())
                {
                    if ("cond".equalsIgnoreCase(n.getNodeName()))
                    {
                        Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                        Node msg = n.getAttributes().getNamedItem("msg");
                        if (condition != null && msg != null)
                            condition.setMessage(msg.getNodeValue());
                        _currentSkill.currentSkills.get(i).attach(condition,false);
                    }
                    if ("for".equalsIgnoreCase(n.getNodeName()))
                    {
                        parseTemplate(n, _currentSkill.currentSkills.get(i));
                    }
                }
            }
        }
        for (int i=lastLvl+enchantLevels1+enchantLevels2+enchantLevels3+enchantLevels4; i < lastLvl+enchantLevels1+enchantLevels2+enchantLevels3+enchantLevels4+enchantLevels5; i++)
        {
            boolean found = false;
            _currentSkill.currentLevel = i-lastLvl-enchantLevels1-enchantLevels2-enchantLevels3-enchantLevels4;
            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant5cond".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                    Node msg = n.getAttributes().getNamedItem("msg");
                    if (condition != null && msg != null)
                        condition.setMessage(msg.getNodeValue());
                    _currentSkill.currentSkills.get(i).attach(condition,false);
                }
                if ("enchant5for".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    parseTemplate(n, _currentSkill.currentSkills.get(i));
                }
            }
            // If none found, the enchanted skill will take effects from maxLvL of norm skill
            if(!found)
            {
                _currentSkill.currentLevel = lastLvl-1;
                for (n=first; n != null; n = n.getNextSibling())
                {
                    if ("cond".equalsIgnoreCase(n.getNodeName()))
                    {
                        Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                        Node msg = n.getAttributes().getNamedItem("msg");
                        if (condition != null && msg != null)
                            condition.setMessage(msg.getNodeValue());
                        _currentSkill.currentSkills.get(i).attach(condition,false);
                    }
                    if ("for".equalsIgnoreCase(n.getNodeName()))
                    {
                        parseTemplate(n, _currentSkill.currentSkills.get(i));
                    }
                }
            }
        }
        for (int i=lastLvl+enchantLevels1+enchantLevels2+enchantLevels3+enchantLevels4+enchantLevels5; i < lastLvl+enchantLevels1+enchantLevels2+enchantLevels3+enchantLevels4+enchantLevels5+enchantLevels6; i++)
        {
            boolean found = false;
            _currentSkill.currentLevel = i-lastLvl-enchantLevels1-enchantLevels2-enchantLevels3-enchantLevels4-enchantLevels5;
            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant6cond".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                    Node msg = n.getAttributes().getNamedItem("msg");
                    if (condition != null && msg != null)
                        condition.setMessage(msg.getNodeValue());
                    _currentSkill.currentSkills.get(i).attach(condition,false);
                }
                if ("enchant6for".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    parseTemplate(n, _currentSkill.currentSkills.get(i));
                }
            }
            // If none found, the enchanted skill will take effects from maxLvL of norm skill
            if(!found)
            {
                _currentSkill.currentLevel = lastLvl-1;
                for (n=first; n != null; n = n.getNextSibling())
                {
                    if ("cond".equalsIgnoreCase(n.getNodeName()))
                    {
                        Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                        Node msg = n.getAttributes().getNamedItem("msg");
                        if (condition != null && msg != null)
                            condition.setMessage(msg.getNodeValue());
                        _currentSkill.currentSkills.get(i).attach(condition,false);
                    }
                    if ("for".equalsIgnoreCase(n.getNodeName()))
                    {
                        parseTemplate(n, _currentSkill.currentSkills.get(i));
                    }
                }
            }
        }
        for (int i=lastLvl+enchantLevels1+enchantLevels2+enchantLevels3+enchantLevels4+enchantLevels5+enchantLevels6; i < lastLvl+enchantLevels1+enchantLevels2+enchantLevels3+enchantLevels4+enchantLevels5+enchantLevels6+enchantLevels7; i++)
        {
            boolean found = false;
            _currentSkill.currentLevel = i-lastLvl-enchantLevels1-enchantLevels2-enchantLevels3-enchantLevels4-enchantLevels5-enchantLevels6;
            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant7cond".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                    Node msg = n.getAttributes().getNamedItem("msg");
                    if (condition != null && msg != null)
                        condition.setMessage(msg.getNodeValue());
                    _currentSkill.currentSkills.get(i).attach(condition,false);
                }
                if ("enchant7for".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    parseTemplate(n, _currentSkill.currentSkills.get(i));
                }
            }
            // If none found, the enchanted skill will take effects from maxLvL of norm skill
            if(!found)
            {
                _currentSkill.currentLevel = lastLvl-1;
                for (n=first; n != null; n = n.getNextSibling())
                {
                    if ("cond".equalsIgnoreCase(n.getNodeName()))
                    {
                        Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                        Node msg = n.getAttributes().getNamedItem("msg");
                        if (condition != null && msg != null)
                            condition.setMessage(msg.getNodeValue());
                        _currentSkill.currentSkills.get(i).attach(condition,false);
                    }
                    if ("for".equalsIgnoreCase(n.getNodeName()))
                    {
                        parseTemplate(n, _currentSkill.currentSkills.get(i));
                    }
                }
            }
        }
        for (int i=lastLvl+enchantLevels1+enchantLevels2+enchantLevels3+enchantLevels4+enchantLevels5+enchantLevels6+enchantLevels7; i < lastLvl+enchantLevels1+enchantLevels2+enchantLevels3+enchantLevels4+enchantLevels5+enchantLevels6+enchantLevels7+enchantLevels8; i++)
        {
            boolean found = false;
            _currentSkill.currentLevel = i-lastLvl-enchantLevels1-enchantLevels2-enchantLevels3-enchantLevels4-enchantLevels5-enchantLevels6-enchantLevels7;
            for (n=first; n != null; n = n.getNextSibling())
            {
                if ("enchant8cond".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                    Node msg = n.getAttributes().getNamedItem("msg");
                    if (condition != null && msg != null)
                        condition.setMessage(msg.getNodeValue());
                    _currentSkill.currentSkills.get(i).attach(condition,false);
                }
                if ("enchant8for".equalsIgnoreCase(n.getNodeName()))
                {
                    found = true;
                    parseTemplate(n, _currentSkill.currentSkills.get(i));
                }
            }
            // If none found, the enchanted skill will take effects from maxLvL of norm skill
            if(!found)
            {
                _currentSkill.currentLevel = lastLvl-1;
                for (n=first; n != null; n = n.getNextSibling())
                {
                    if ("cond".equalsIgnoreCase(n.getNodeName()))
                    {
                        Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
                        Node msg = n.getAttributes().getNamedItem("msg");
                        if (condition != null && msg != null)
                            condition.setMessage(msg.getNodeValue());
                        _currentSkill.currentSkills.get(i).attach(condition,false);
                    }
                    if ("for".equalsIgnoreCase(n.getNodeName()))
                    {
                        parseTemplate(n, _currentSkill.currentSkills.get(i));
                    }
                }
            }
        }
        _currentSkill.skills.addAll(_currentSkill.currentSkills);
	}

    private void makeSkills()
	{
        int count = 0;
        _currentSkill.currentSkills = new FastList<L2Skill>(_currentSkill.sets.length+_currentSkill.enchsets1.length+_currentSkill.enchsets2.length+_currentSkill.enchsets3.length+_currentSkill.enchsets4.length+_currentSkill.enchsets5.length+_currentSkill.enchsets6.length+_currentSkill.enchsets7.length+_currentSkill.enchsets8.length);

		for (int i=0; i < _currentSkill.sets.length; i++)
        {
            try
            {
            	_currentSkill.currentSkills.add(i, _currentSkill.sets[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.sets[i]));
            	count++;
            }
            catch (Exception e)
            {
                _log.log(Level.SEVERE, "Skill id="+_currentSkill.sets[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.sets[i]).getDisplayId()+"level"+_currentSkill.sets[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.sets[i]).getLevel(), e);
            }
        }
        int _count = count;
        for (int i=0; i < _currentSkill.enchsets1.length; i++)
        {
            try
            {
            	_currentSkill.currentSkills.add(_count+i, _currentSkill.enchsets1[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets1[i]));
            	count++;
            }
            catch (Exception e)
            {
                _log.log(Level.SEVERE, "Skill id="+_currentSkill.enchsets1[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets1[i]).getDisplayId()+" level="+_currentSkill.enchsets1[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets1[i]).getLevel(), e);
            }
        }
        _count = count;
        for (int i=0; i < _currentSkill.enchsets2.length; i++)
        {
            try
            {
            	_currentSkill.currentSkills.add(_count+i, _currentSkill.enchsets2[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets2[i]));
            	count++;
            }
            catch (Exception e)
            {
                _log.log(Level.SEVERE, "Skill id="+_currentSkill.enchsets2[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets2[i]).getDisplayId()+" level="+_currentSkill.enchsets2[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets2[i]).getLevel(), e);
            }
        }
        _count = count;
        for (int i=0; i < _currentSkill.enchsets3.length; i++)
        {
            try
            {
                _currentSkill.currentSkills.add(_count+i, _currentSkill.enchsets3[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets3[i]));
                count++;
            }
            catch (Exception e)
            {
                _log.log(Level.SEVERE, "Skill id="+_currentSkill.enchsets3[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets3[i]).getDisplayId()+" level="+_currentSkill.enchsets3[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets3[i]).getLevel(), e);
            }
        }
        _count = count;
        for (int i=0; i < _currentSkill.enchsets4.length; i++)
        {
            try
            {
                _currentSkill.currentSkills.add(_count+i, _currentSkill.enchsets4[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets4[i]));
                count++;
            }
            catch (Exception e)
            {
                _log.log(Level.SEVERE, "Skill id="+_currentSkill.enchsets4[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets4[i]).getDisplayId()+" level="+_currentSkill.enchsets4[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets4[i]).getLevel(), e);
            }
        }
        _count = count;
        for (int i=0; i < _currentSkill.enchsets5.length; i++)
        {
            try
            {
                _currentSkill.currentSkills.add(_count+i, _currentSkill.enchsets5[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets5[i]));
                count++;
            }
            catch (Exception e)
            {
                _log.log(Level.SEVERE, "Skill id="+_currentSkill.enchsets5[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets5[i]).getDisplayId()+" level="+_currentSkill.enchsets5[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets5[i]).getLevel(), e);
            }
        }
        _count = count;
        for (int i=0; i < _currentSkill.enchsets6.length; i++)
        {
            try
            {
                _currentSkill.currentSkills.add(_count+i, _currentSkill.enchsets6[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets6[i]));
                count++;
            }
            catch (Exception e)
            {
                _log.log(Level.SEVERE, "Skill id="+_currentSkill.enchsets6[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets6[i]).getDisplayId()+" level="+_currentSkill.enchsets6[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets6[i]).getLevel(), e);
            }
        }
        _count = count;
        for (int i=0; i < _currentSkill.enchsets7.length; i++)
        {
            try
            {
                _currentSkill.currentSkills.add(_count+i, _currentSkill.enchsets7[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets7[i]));
                count++;
            }
            catch (Exception e)
            {
                _log.log(Level.SEVERE, "Skill id="+_currentSkill.enchsets7[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets7[i]).getDisplayId()+" level="+_currentSkill.enchsets7[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets7[i]).getLevel(), e);
            }
        }
        _count = count;
        for (int i=0; i < _currentSkill.enchsets8.length; i++)
        {
            try
            {
                _currentSkill.currentSkills.add(_count+i, _currentSkill.enchsets8[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets8[i]));
                count++;
            }
            catch (Exception e)
            {
                _log.log(Level.SEVERE, "Skill id="+_currentSkill.enchsets8[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets8[i]).getDisplayId()+" level="+_currentSkill.enchsets8[i].getEnum("skillType", L2SkillType.class).makeSkill(_currentSkill.enchsets8[i]).getLevel(), e);
            }
        }
	}
}
