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
package net.sf.l2j.gameserver.script.faenor;

import java.util.logging.Logger;

import javax.script.ScriptContext;

import org.w3c.dom.Node;

import net.sf.l2j.gameserver.script.Parser;
import net.sf.l2j.gameserver.script.ParserFactory;
import net.sf.l2j.gameserver.script.ScriptEngine;

/**
 * @author Luis Arias
 *
 */
public class FaenorQuestParser extends FaenorParser
{
    protected static final Logger _log = Logger.getLogger(FaenorQuestParser.class.getName());
    @Override
	public void parseScript(Node questNode, ScriptContext context)
    {
        if (DEBUG) 
            _log.info("Parsing Quest.");

        String questID = attribute(questNode, "ID");

        for (Node node = questNode.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (isNodeName(node, "DROPLIST"))
            {
                parseQuestDropList(node.cloneNode(true), questID);
            }
            else if (isNodeName(node, "DIALOG WINDOWS"))
            {
                //parseDialogWindows(node.cloneNode(true));
            }
            else if (isNodeName(node, "INITIATOR"))
            {
                //parseInitiator(node.cloneNode(true));
            }
            else if (isNodeName(node, "STATE"))
            {
                //parseState(node.cloneNode(true));
            }
        }
    }

    private void parseQuestDropList(Node dropList, String questID) throws NullPointerException
    {
        if (DEBUG) 
            _log.info("Parsing Droplist.");

        for (Node node = dropList.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (isNodeName(node, "DROP"))
            {
                parseQuestDrop(node.cloneNode(true), questID);
            }
        }
    }

    private void parseQuestDrop(Node drop, String questID)// throws NullPointerException
    {
        if (DEBUG) 
            _log.info("Parsing Drop.");

        int npcID;
        int itemID;
        int min;
        int max;
        int chance;
        String[] states;
        try
        {
            npcID = getInt(attribute(drop, "NpcID"));
            itemID = getInt(attribute(drop, "ItemID"));
            min = getInt(attribute(drop, "Min"));
            max = getInt(attribute(drop, "Max"));
            chance = getInt(attribute(drop, "Chance"));
            states = (attribute(drop, "States")).split(",");
        }
        catch (NullPointerException e)
        {
            throw new NullPointerException("Incorrect Drop Data");
        }

        if (DEBUG) 
            _log.info("Adding Drop to NpcID: "+npcID);

        _bridge.addQuestDrop(npcID, itemID, min, max, chance, questID, states);
    }

    static class FaenorQuestParserFactory extends ParserFactory
    {
        @Override
		public Parser create()
        {
            return(new FaenorQuestParser());
        }
    }

    static
    {
        ScriptEngine.parserFactories.put(getParserName("Quest"), new FaenorQuestParserFactory());
    }
}
