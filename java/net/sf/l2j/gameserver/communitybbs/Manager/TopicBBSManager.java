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
package net.sf.l2j.gameserver.communitybbs.Manager;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.communitybbs.BB.Forum;
import net.sf.l2j.gameserver.communitybbs.BB.Post;
import net.sf.l2j.gameserver.communitybbs.BB.Topic;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.util.StringUtil;

public class TopicBBSManager extends BaseBBSManager
{
private List<Topic> _table;
private Map<Forum, Integer> _maxId;

public static TopicBBSManager getInstance()
{
	return SingletonHolder._instance;
}

private TopicBBSManager()
{
	_table = new FastList<Topic>();
	_maxId = new FastMap<Forum, Integer>();
}

public void addTopic(Topic tt)
{
	_table.add(tt);
}

/**
 * @param topic
 */
public void delTopic(Topic topic)
{
	_table.remove(topic);
}

public void setMaxID(int id, Forum f)
{
	_maxId.remove(f);
	_maxId.put(f, id);
}

public int getMaxID(Forum f)
{
	Integer i = _maxId.get(f);
	if (i == null)
	{
		return 0;
	}
	return i;
}

public Topic getTopicByID(int idf)
{
	for (Topic t : _table)
	{
		if (t.getID() == idf)
		{
			return t;
		}
	}
	return null;
}

/**
 * 
 * @see BaseBBSManager#parsewrite(String, String, String, String, String, String, L2PcInstance)
 */
@Override
public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
{
	if (ar1.equals("crea"))
	{
		Forum f = ForumsBBSManager.getInstance().getForumByID(Integer.parseInt(ar2));
		if (f == null)
		{
			ShowBoard sb = new ShowBoard("<html><body><br><br><center>the forum: " + ar2
					+ " is not implemented yet</center><br><br></body></html>", "101");
			activeChar.sendPacket(sb);
			
		}
		else
		{
			f.vload();
			Topic t = new Topic(Topic.ConstructorType.CREATE, TopicBBSManager.getInstance().getMaxID(f) + 1, Integer.parseInt(ar2), ar5, Calendar.getInstance().getTimeInMillis(), activeChar.getName(), activeChar.getObjectId(), Topic.MEMO, 0);
			f.addTopic(t);
			TopicBBSManager.getInstance().setMaxID(t.getID(), f);
			Post p = new Post(activeChar.getName(), activeChar.getObjectId(), Calendar.getInstance().getTimeInMillis(), t.getID(), f.getID(), ar4);
			PostBBSManager.getInstance().addPostByTopic(p, t);
			parsecmd("_bbsmemo", activeChar);
		}
		
	}
	else if (ar1.equals("del"))
	{
		Forum f = ForumsBBSManager.getInstance().getForumByID(Integer.parseInt(ar2));
		if (f == null)
		{
			ShowBoard sb = new ShowBoard("<html><body><br><br><center>the forum: " + ar2
					+ " does not exist !</center><br><br></body></html>", "101");
			activeChar.sendPacket(sb);
			
		}
		else
		{
			Topic t = f.getTopic(Integer.parseInt(ar3));
			if (t == null)
			{
				ShowBoard sb = new ShowBoard("<html><body><br><br><center>the topic: " + ar3
						+ " does not exist !</center><br><br></body></html>", "101");
				activeChar.sendPacket(sb);
				
			}
			else
			{
				//CPost cp = null;
				Post p = PostBBSManager.getInstance().getGPosttByTopic(t);
				if (p != null)
				{
					p.deleteme(t);
				}
				t.deleteme(f);
				parsecmd("_bbsmemo", activeChar);
			}
		}
	}
	else
	{
		ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + ar1
				+ " is not implemented yet</center><br><br></body></html>", "101");
		activeChar.sendPacket(sb);
		
	}
}

/**
 * 
 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsecmd(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
 */
@Override
public void parsecmd(String command, L2PcInstance activeChar)
{
	if (command.equals("_bbsmemo"))
	{
		showTopics(activeChar.getMemo(), activeChar, 1, activeChar.getMemo().getID());
	}
	else if (command.startsWith("_bbstopics;read"))
	{
		StringTokenizer st = new StringTokenizer(command, ";");
		st.nextToken();
		st.nextToken();
		int idf = Integer.parseInt(st.nextToken());
		String index = null;
		if (st.hasMoreTokens())
		{
			index = st.nextToken();
		}
		int ind = 0;
		if (index == null)
		{
			ind = 1;
		}
		else
		{
			ind = Integer.parseInt(index);
		}
		showTopics(ForumsBBSManager.getInstance().getForumByID(idf), activeChar, ind, idf);
	}
	else if (command.startsWith("_bbstopics;crea"))
	{
		StringTokenizer st = new StringTokenizer(command, ";");
		st.nextToken();
		st.nextToken();
		int idf = Integer.parseInt(st.nextToken());
		showNewTopic(ForumsBBSManager.getInstance().getForumByID(idf), activeChar, idf);
	}
	else if (command.startsWith("_bbstopics;del"))
	{
		StringTokenizer st = new StringTokenizer(command, ";");
		st.nextToken();
		st.nextToken();
		int idf = Integer.parseInt(st.nextToken());
		int idt = Integer.parseInt(st.nextToken());
		Forum f = ForumsBBSManager.getInstance().getForumByID(idf);
		if (f == null)
		{
			ShowBoard sb = new ShowBoard("<html><body><br><br><center>the forum: " + idf
					+ " does not exist !</center><br><br></body></html>", "101");
			activeChar.sendPacket(sb);
			
		}
		else
		{
			Topic t = f.getTopic(idt);
			if (t == null)
			{
				ShowBoard sb = new ShowBoard("<html><body><br><br><center>the topic: " + idt
						+ " does not exist !</center><br><br></body></html>", "101");
				activeChar.sendPacket(sb);
				
			}
			else
			{
				//CPost cp = null;
				Post p = PostBBSManager.getInstance().getGPosttByTopic(t);
				if (p != null)
				{
					p.deleteme(t);
				}
				t.deleteme(f);
				parsecmd("_bbsmemo", activeChar);
			}
		}
	}
	else
	{
		ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command
				+ " is not implemented yet</center><br><br></body></html>", "101");
		activeChar.sendPacket(sb);
		
	}
}

/**
 * @param forumByID
 * @param activeChar
 * @param idf
 */
private void showNewTopic(Forum forum, L2PcInstance activeChar, int idf)
{
	if (forum == null)
	{
		ShowBoard sb = new ShowBoard("<html><body><br><br><center>the forum: " + idf
				+ " is not implemented yet</center><br><br></body></html>", "101");
		activeChar.sendPacket(sb);
		
	}
	else if (forum.getType() == Forum.MEMO)
	{
		showMemoNewTopics(forum, activeChar);
	}
	else
	{
		ShowBoard sb = new ShowBoard("<html><body><br><br><center>the forum: " + forum.getName()
				+ " is not implemented yet</center><br><br></body></html>", "101");
		activeChar.sendPacket(sb);
		
	}
}

/**
 * @param forum
 * @param activeChar
 */
private void showMemoNewTopics(Forum forum, L2PcInstance activeChar)
{
	final String html = StringUtil.concat("<html>"
			+ "<body><br><br>"
			+ "<table border=0 width=610><tr><td width=10></td><td width=600 align=left>"
			+ "<a action=\"bypass _bbshome\">HOME</a>&nbsp;>&nbsp;<a action=\"bypass _bbsmemo\">Memo Form</a>"
			+ "</td></tr>"
			+ "</table>"
			+ "<img src=\"L2UI.squareblank\" width=\"1\" height=\"10\">"
			+ "<center>"
			+ "<table border=0 cellspacing=0 cellpadding=0>"
			+ "<tr><td width=610><img src=\"sek.cbui355\" width=\"610\" height=\"1\"><br1><img src=\"sek.cbui355\" width=\"610\" height=\"1\"></td></tr>"
			+ "</table>" + "<table fixwidth=610 border=0 cellspacing=0 cellpadding=0>"
			+ "<tr><td><img src=\"l2ui.mini_logo\" width=5 height=20></td></tr>" + "<tr>"
			+ "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>" + "<td align=center FIXWIDTH=60 height=29>&$413;</td>"
			+ "<td FIXWIDTH=540><edit var = \"Title\" width=540 height=13></td>"
			+ "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>" + "</tr></table>"
			+ "<table fixwidth=610 border=0 cellspacing=0 cellpadding=0>"
			+ "<tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr>" + "<tr>"
			+ "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>"
			+ "<td align=center FIXWIDTH=60 height=29 valign=top>&$427;</td>"
			+ "<td align=center FIXWIDTH=540><MultiEdit var =\"Content\" width=535 height=313></td>"
			+ "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>" + "</tr>"
			+ "<tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr>" + "</table>"
			+ "<table fixwidth=610 border=0 cellspacing=0 cellpadding=0>"
			+ "<tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr>" + "<tr>"
			+ "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>" + "<td align=center FIXWIDTH=60 height=29>&nbsp;</td>"
			+ "<td align=center FIXWIDTH=70><button value=\"&$140;\" action=\"Write Topic crea ", String.valueOf(forum.getID()), " Title Content Title\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\" ></td>"
			+ "<td align=center FIXWIDTH=70><button value = \"&$141;\" action=\"bypass _bbsmemo\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"> </td>"
			+ "<td align=center FIXWIDTH=400>&nbsp;</td>"
			+ "<td><img src=\"l2ui.mini_logo\" width=5 height=1></td>"
			+ "</tr></table>"
			+ "</center>" + "</body>" + "</html>");
	send1001(html, activeChar);
	send1002(activeChar);
}

/**
 * @param memo
 */
private void showTopics(Forum forum, L2PcInstance activeChar, int index, int idf)
{
	if (forum == null)
	{
		ShowBoard sb = new ShowBoard("<html><body><br><br><center>the forum: " + idf
				+ " is not implemented yet</center><br><br></body></html>", "101");
		activeChar.sendPacket(sb);
		
	}
	else if (forum.getType() == Forum.MEMO)
	{
		showMemoTopics(forum, activeChar, index);
	}
	else
	{
		ShowBoard sb = new ShowBoard("<html><body><br><br><center>the forum: " + forum.getName()
				+ " is not implemented yet</center><br><br></body></html>", "101");
		activeChar.sendPacket(sb);
		
	}
}

/**
 * @param forum
 * @param activeChar
 */
private void showMemoTopics(Forum forum, L2PcInstance activeChar, int index)
{
	forum.vload();
	final StringBuilder html = StringUtil.startAppend(2000, "<html><body><br><br>"
			+ "<table border=0 width=610><tr><td width=10></td><td width=600 align=left>"
			+ "<a action=\"bypass _bbshome\">HOME</a>&nbsp;>&nbsp;<a action=\"bypass _bbsmemo\">Memo Form</a>" + "</td></tr>"
			+ "</table>" + "<img src=\"L2UI.squareblank\" width=\"1\" height=\"10\">" + "<center>"
			+ "<table border=0 cellspacing=0 cellpadding=2 bgcolor=888888 width=610>" + "<tr>" + "<td FIXWIDTH=5></td>"
			+ "<td FIXWIDTH=415 align=center>&$413;</td>" + "<td FIXWIDTH=120 align=center></td>"
			+ "<td FIXWIDTH=70 align=center>&$418;</td>" + "</tr>" + "</table>");
	final DateFormat dateFormat = DateFormat.getInstance();
	
	for (int i = 0, j = getMaxID(forum) + 1; i < 12 * index; j--)
	{
		if (j < 0)
		{
			break;
		}
		Topic t = forum.getTopic(j);
		if (t != null)
		{
			if (i++ >= 12 * (index - 1))
			{
				StringUtil.append(html, "<table border=0 cellspacing=0 cellpadding=5 WIDTH=610>" + "<tr>" + "<td FIXWIDTH=5></td>"
						+ "<td FIXWIDTH=415><a action=\"bypass _bbsposts;read;", String.valueOf(forum.getID()), ";", String.valueOf(t.getID()), "\">", t.getName(), "</a></td>"
						+ "<td FIXWIDTH=120 align=center></td>" + "<td FIXWIDTH=70 align=center>", dateFormat.format(new Date(t.getDate())), "</td>"
						+ "</tr>" + "</table>" + "<img src=\"L2UI.Squaregray\" width=\"610\" height=\"1\">");
			}
		}
	}
	
	html.append("<br>"
			+ "<table width=610 cellspace=0 cellpadding=0>"
			+ "<tr>"
			+ "<td width=50>"
			+ "<button value=\"&$422;\" action=\"bypass _bbsmemo\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\">"
			+ "</td>" + "<td width=510 align=center>" + "<table border=0><tr>");
	
	if (index == 1)
	{
		html.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
	}
	else
	{
		StringUtil.append(html, "<td><button action=\"bypass _bbstopics;read;", String.valueOf(forum.getID()), ";", String.valueOf(index - 1), "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
	}
	
	int nbp;
	nbp = forum.getTopicSize() / 8;
	if (nbp * 8 != ClanTable.getInstance().getClans().length)
	{
		nbp++;
	}
	for (int i = 1; i <= nbp; i++)
	{
		if (i == index)
		{
			StringUtil.append(html, "<td> ", String.valueOf(i), " </td>");
		}
		else
		{
			StringUtil.append(html, "<td><a action=\"bypass _bbstopics;read;", String.valueOf(forum.getID()), ";", String.valueOf(i), "\"> ", String.valueOf(i), " </a></td>");
		}
	}
	if (index == nbp)
	{
		html.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
	}
	else
	{
		StringUtil.append(html, "<td><button action=\"bypass _bbstopics;read;", String.valueOf(forum.getID()), ";", String.valueOf(index + 1), "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
	}
	
	StringUtil.append(html, "</tr></table> </td> " + "<td align=right><button value = \"&$421;\" action=\"bypass _bbstopics;crea;", String.valueOf(forum.getID()), "\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\" ></td></tr>"
			+ "<tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr>"
			+ "<tr> "
			+ "<td></td>"
			+ "<td align=center><table border=0><tr><td></td><td><edit var = \"Search\" width=130 height=11></td>"
			+ "<td><button value=\"&$420;\" action=\"Write 5 -2 0 Search _ _\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"> </td> </tr></table> </td>"
			+ "</tr>" + "</table>" + "<br>" + "<br>" + "<br>" + "</center>" + "</body>" + "</html>");
	separateAndSend(html.toString(), activeChar);
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final TopicBBSManager _instance = new TopicBBSManager();
}
}