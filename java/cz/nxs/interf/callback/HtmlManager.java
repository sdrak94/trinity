/**
 * 
 */
package cz.nxs.interf.callback;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import cz.nxs.events.NexusLoader;
import cz.nxs.events.engine.EventBuffer;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.html.EventHtmlManager;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import javolution.text.TextBuilder;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;

/**
 * @author hNoke
 *
 */
public class HtmlManager extends EventHtmlManager
{
	private Map<Integer, Long> _healTimes = new FastMap<Integer, Long>();
	private Map<Integer, Long> _healTimesPets = new FastMap<Integer, Long>();
	public static int HEAL_DELAY_MS = EventConfig.getInstance().getGlobalConfigInt("bufferHealDelay") * 1000;
	public static int MAX_BUFFS_COUNT = EventConfig.getInstance().getGlobalConfigInt("maxBuffsCount");
	//public static int MAX_DANCES_COUNT = EventConfig.getInstance().getGlobalConfigInt("maxDancesCount");
	
	public HtmlManager()
	{
		super();
	}
	
	public static void load()
	{
		CallBack.getInstance().setHtmlManager(new HtmlManager());
	}
	
	@Override
	public boolean showNpcHtml(PlayerEventInfo player, NpcData npc)
	{
		if(npc.getNpcId() == EventConfig.getInstance().getGlobalConfigInt("assignedNpcId"))
		{
			showCustomBufferMenu(player);
			return true;
		}
		
		return super.showNpcHtml(player, npc);
	}

	@Override
	public boolean onBypass(PlayerEventInfo player, String bypass)
	{
		if(bypass.startsWith("npcbuffer"))
		{
			String action = bypass.substring(10);
			
			if(player.getTarget() != null && player.getTarget().isNpc() && player.getTarget().getNpc().getNpcId() == EventConfig.getInstance().getGlobalConfigInt("assignedNpcId") && player.getOwner().isInsideRadius(player.getTarget().getOwner(), 150, false, false) || bypass.contains("_bbs"))
			{
				if(action.startsWith("menu"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					showCustomBufferMenu(player);
					return true;
				}
				else if(action.startsWith("bufferinfo"))
				{
					showBufferIntoPage(player);
					return true;
				}
				else if(action.startsWith("reload"))
				{
					if(player.isGM())
					{
						HEAL_DELAY_MS = EventConfig.getInstance().getGlobalConfigInt("bufferHealDelay") * 1000;
						MAX_BUFFS_COUNT = EventConfig.getInstance().getGlobalConfigInt("maxBuffsCount");
						//MAX_DANCES_COUNT = EventConfig.getInstance().getGlobalConfigInt("maxDancesCount");
						player.sendMessage("Reloaded.");
						return true;
					}
				}
				else if(action.startsWith("singlemenu"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					String target = st.nextToken();
					String category = st.nextToken();
					
					showSingleBuffsMenu(player, target, category);
					return true;
				}
				else if(action.startsWith("singlebuff"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					String target = st.nextToken();
					String category = st.nextToken();
					int buffId;
					
					try
					{
						buffId = Integer.parseInt(st.nextToken());
						giveBuff(player, target, buffId);
					}
					catch (Exception e)
					{
						player.sendMessage("Wrong buff.");
						showSingleBuffsMenu(player, target, category);
						return true;
					}
					
					showSingleBuffsMenu(player, target, category);
					return true;
				}
				else if(action.startsWith("single_selectcategory"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					String target = st.nextToken();
					String category = st.nextToken();
					
					showSingleBuffsMenu(player, target, category);
					return true;
				}
				else if(action.startsWith("edit_schemes"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					String returnPage = st.nextToken();
					
					showSelectSchemeMenu(player, returnPage, 0, null);
					return true;
				}
				else if(action.startsWith("buffmenu"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					String target = st.nextToken();
					if (action.contains("_bbs"))
					{
						showBuffMeWindowBbs(player, target);
					}
					else
					showBuffMeWindow(player, target);
					return true;
				}
				else if(action.startsWith("cancelbuffs"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					String target = st.nextToken();
					String returnPage = "buffmenu";
					if(st.hasMoreTokens())
						returnPage = st.nextToken();
					
					if(target.equals("player"))
						player.removeBuffs();
					else if(target.equals("pet"))
						player.removeBuffsFromPet();
					
					if(returnPage.equals("buffmenu"))
						showBuffMeWindow(player, target);
					else if(returnPage.equals("menu"))
						showCustomBufferMenu(player);
					
					return true;
				}
				else if(action.startsWith("heal"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					String target = st.nextToken();
					String returnPage = "buffmenu";
					if(st.hasMoreTokens())
						returnPage = st.nextToken();
					
					healPlayer(player, target);
					
					if(returnPage.equals("buffmenu"))
						showBuffMeWindow(player, target);
					else if(returnPage.equals("menu"))
						showCustomBufferMenu(player);
					
					return true;
				}		
				else if(action.startsWith("buff"))
				{
					StringTokenizer st = new StringTokenizer(action);
					st.nextToken();
					
					String target = st.nextToken();
					
					if(!st.hasMoreTokens())
					{
						player.sendMessage("You need to specify a scheme.");
						return true;
					}
					
					String scheme = st.nextToken();
					
					buffPlayer(player, scheme, target);
					return true;
				}
			}
		}
		
		return super.onBypass(player, bypass);
	}
	
	private void showSelectSchemeMenu(PlayerEventInfo player, String returnPage, int page, String selectedCategory)
	{
		String html;
		
		final String scheme = EventBuffer.getInstance().getPlayersCurrentScheme(player.getPlayersId());
		
		TextBuilder tb = new TextBuilder();
		
		tb.append("<html><title>Event Buffer</title><body>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=3>");
		
		tb.append("<table width=280 border=0 bgcolor=484848><tr>");
		tb.append("<td width=220 align=center> <font color=8f8f8f>Scheme management menu</font></td>");
		tb.append("<td width=65 align=right><button value=\"Back\" action=\"bypass -h nxs_" + returnPage + "_menu\" width=65 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		tb.append("</tr></table>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=5>");
		
		tb.append("<br>");
		
		tb.append("<br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32 align=left><br>");
		
		// draw player's schemes
		
		if(!EventBuffer.getInstance().getSchemes(player).isEmpty())
		{
			tb.append("<table width=283 bgcolor=2E2E2E>");
			
			for(Entry<String, List<Integer>> schemes : EventBuffer.getInstance().getSchemes(player))
			{
				tb.append("<tr>");
				tb.append("<td width=150 align=left><font color=ac9887>" + (scheme != null && scheme.equals(schemes.getKey()) ? "*" : "") + " " + schemes.getKey() + " </font><font color=7f7f7f>(" + schemes.getValue().size() + " buffs)</font></td>");
				tb.append("<td width=65 align=center><font color=B04F51><a action=\"bypass -h nxs_buffer_delete_scheme " + schemes.getKey() + " " + returnPage + "\">Delete</a></font></td>");
				tb.append("<td width=75 align=right><font color=9f9f9f><a action=\"bypass -h nxs_buffer_select_scheme " + schemes.getKey() + " " + returnPage + "\">Edit scheme</a></font></td>");
				//tb.append("<td width=70 align=right><button value=\"Edit\" action=\"bypass -h nxs_buffer_select_scheme " + schemes.getKey() + " " + returnPage + "\" width=70 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
//				tb.append("<td width=65 align=right><button value=\"Delete\" action=\"bypass -h nxs_buffer_delete_scheme " + schemes.getKey() + " " + returnPage + "\" width=65 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				tb.append("</tr>");
			}
			
			tb.append("</table>");
		}
		else
		{
			tb.append("<table width=283 bgcolor=2E2E2E>");
			tb.append("<tr><td width=280 align=center><font color=ac9887>You don't have any scheme.</font></td></tr>");
			tb.append("</table>");
		}
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=3>");
		
		tb.append("<table width=283 bgcolor=2E2E2E");
		tb.append("<tr><td width=115><edit var=\"name\" width=115 height=15></td><td width=150 align=right><button value=\"Create scheme\" action=\"bypass -h nxs_buffer_create_scheme " + returnPage + " $name\" width=105 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		tb.append("</table>");
		
		tb.append("<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32 align=left><br>");
		
		tb.append("</body></html>");

		html = tb.toString();
		
		player.sendHtmlText(html);
		player.sendStaticPacket();
	}
	protected void showBuffMeWindowBbs(PlayerEventInfo player, String target)
	{

		if(player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player.getOwner()) || Olympiad.getInstance().isRegisteredInComp(player.getOwner()))
			return;
		
		if(target.equals("pet"))
		{
			if(!player.hasPet())
			{
				player.sendMessage("You have no pet/summon.");
				return;
			}
		}
		
		String html;

		TextBuilder tb = new TextBuilder();
		
		tb.append("<html><title>Event Buffer</title><body>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=3>");
		
		tb.append("<table width=280 border=0 bgcolor=484848><tr>");
		tb.append("<td width=220 align=center> <font color=8f8f8f>Buff " + target + "</font></td>");
		tb.append("<td width=65 align=right><button value=\"Back\" action=\"bypass -h nxs_npcbuffer_menu\" width=65 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		tb.append("</tr></table>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=5>");
		
		int playerBuffs = player.getBuffsCount();
		
		final int maxBuffs = getMaxBuffs(player);
		
		//tb.append("<center>You currently have </center>");
		
		tb.append("<br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32 align=left><br>");
		
		// draw player's schemes
		
		if(!EventBuffer.getInstance().getSchemes(player).isEmpty())
		{
			tb.append("<table width=283 bgcolor=2E2E2E>");
			
			for(Entry<String, List<Integer>> schemes : EventBuffer.getInstance().getSchemes(player))
			{
				tb.append("<tr>");
				tb.append("<td width=150 align=left><font color=ac9887>" + schemes.getKey() + " </font><font color=7f7f7f>(" + schemes.getValue().size() + " buffs)</font></td>");
				tb.append("<td width=65 align=center><font color=B04F51><a action=\"bypass -h nxs_npcbuffer_buff " + target + " " + schemes.getKey() + " _bbs\">Buff " + target + " </a></font></td>");
				tb.append("</tr>");
			}
			
			tb.append("</table>");
		}
		else
		{
			tb.append("<table width=283 bgcolor=2E2E2E>");
			tb.append("<tr><td width=280 align=center><font color=ac9887>You don't have any scheme.</font></td></tr>");
			tb.append("<tr><td width=280 align=center><font color=BF8380>You must make one first.</font></td></tr>");
			tb.append("</table>");
		}
		
		tb.append("<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32 align=left><br>");
		
		
		tb.append("<center><table width=280>");
		
		if(target.equals("player"))
		{
			tb.append("<tr><td width=120 align=center><font color=9f9f9f>You currently have:</font></td><td width=140 align=center><font color=ac9887>" + playerBuffs + " buffs</font></td></tr>");
		}
		
		if(target.equals("pet"))
		{
			tb.append("<tr><td width=120 align=center><font color=9f9f9f>Your pet currently has:</font></td><td width=140 align=center><font color=ac9887>" + 0 + " buffs</font></td></tr>");
		}
		
		tb.append("<tr><td width=140 align=center><font color=9f9f9f>Max buffs #:</font></td><td width=140 align=center><font color=ac9887>" + maxBuffs + " buffs</font></td></tr>");
		
		tb.append("</table></center><br>");
		
		tb.append("<table width=280><tr>");
		tb.append("<td width=140 align=left><center><button value=\"Heal Disabled\"  action=\" " + target + "\" _bbs width=130 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></td>");
		tb.append("<td width=140 align=right><center><button value=\"Cancel buffs\" action=\"bypass -h nxs_npcbuffer_cancelbuffs " + target + " _bbs\"  width=130 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></td>");
		
		tb.append("</tr></table>");
		
		tb.append("</body></html>");
		
		html = tb.toString();
		
		player.sendHtmlText(html);
		player.sendStaticPacket();
	}	
	protected void showBuffMeWindow(PlayerEventInfo player, String target)
	{
		if(target.equals("pet"))
		{
			if(!player.hasPet())
			{
				player.sendMessage("You have no pet/summon.");
				return;
			}
		}
		
		String html;

		TextBuilder tb = new TextBuilder();
		
		tb.append("<html><title>Event Buffer</title><body>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=3>");
		
		tb.append("<table width=280 border=0 bgcolor=484848><tr>");
		tb.append("<td width=220 align=center> <font color=8f8f8f>Buff " + target + "</font></td>");
		tb.append("<td width=65 align=right><button value=\"Back\" action=\"bypass -h nxs_npcbuffer_menu\" width=65 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		tb.append("</tr></table>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=5>");
		
		int playerBuffs = player.getBuffsCount();
		
		final int maxBuffs = getMaxBuffs(player);
		
		//tb.append("<center>You currently have </center>");
		
		tb.append("<br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32 align=left><br>");
		
		// draw player's schemes
		
		if(!EventBuffer.getInstance().getSchemes(player).isEmpty())
		{
			tb.append("<table width=283 bgcolor=2E2E2E>");
			
			for(Entry<String, List<Integer>> schemes : EventBuffer.getInstance().getSchemes(player))
			{
				tb.append("<tr>");
				tb.append("<td width=150 align=left><font color=ac9887>" + schemes.getKey() + " </font><font color=7f7f7f>(" + schemes.getValue().size() + " buffs)</font></td>");
				tb.append("<td width=65 align=center><font color=B04F51><a action=\"bypass -h nxs_npcbuffer_buff " + target + " " + schemes.getKey() + " \">Buff " + target + "</a></font></td>");
				tb.append("</tr>");
			}
			
			tb.append("</table>");
		}
		else
		{
			tb.append("<table width=283 bgcolor=2E2E2E>");
			tb.append("<tr><td width=280 align=center><font color=ac9887>You don't have any scheme.</font></td></tr>");
			tb.append("<tr><td width=280 align=center><font color=BF8380>You must make one first.</font></td></tr>");
			tb.append("</table>");
		}
		
		tb.append("<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32 align=left><br>");
		
		
		tb.append("<center><table width=280>");
		
		if(target.equals("player"))
		{
			tb.append("<tr><td width=120 align=center><font color=9f9f9f>You currently have:</font></td><td width=140 align=center><font color=ac9887>" + playerBuffs + " buffs</font></td></tr>");
		}
		
		if(target.equals("pet"))
		{
			tb.append("<tr><td width=120 align=center><font color=9f9f9f>Your pet currently has:</font></td><td width=140 align=center><font color=ac9887>" + 0 + " buffs</font></td></tr>");
		}
		
		tb.append("<tr><td width=140 align=center><font color=9f9f9f>Max buffs #:</font></td><td width=140 align=center><font color=ac9887>" + maxBuffs + " buffs</font></td></tr>");
		
		tb.append("</table></center><br>");
		
		tb.append("<table width=280><tr>");
		tb.append("<td width=140 align=left><center><button value=\"Heal " + target + "\" action=\"bypass -h nxs_npcbuffer_heal " + target + "\" width=130 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></td>");
		tb.append("<td width=140 align=right><center><button value=\"Cancel buffs\" action=\"bypass -h nxs_npcbuffer_cancelbuffs " + target + "\" width=130 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></td>");
		
		tb.append("</tr></table>");
		
		tb.append("</body></html>");
		
		html = tb.toString();
		
		player.sendHtmlText(html);
		player.sendStaticPacket();
	}
	
	@Override
	protected int getMaxBuffs(PlayerEventInfo player)
	{
		if(MAX_BUFFS_COUNT == -1)
			return player.getMaxBuffCount();
		else return MAX_BUFFS_COUNT;
	}
	
	@Override
	protected int getMaxDances(PlayerEventInfo player)
	{
//		if(MAX_DANCES_COUNT == -1)
//			return player.getMaxDanceCount();
//		else return MAX_DANCES_COUNT;
		return 999;
	}
	
	protected void showSingleBuffsMenu(PlayerEventInfo player, String target, String selectedCategory)
	{
		String html;
		
		TextBuilder tb = new TextBuilder();
		
		tb.append("<html><title>Nexus Buffer</title><body>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=3>");
		
		String status = "";
		
		if(target.equalsIgnoreCase("player"))
		{
			status += "<font color=9f9f9f>" + player.getBuffsCount() + "/" + (getMaxBuffs(player) + Math.max(0, getMaxDances(player))) + " buffs</font>"; 
		}
		else if(target.equalsIgnoreCase("pet"))
		{
			status += "<font color=9f9f9f>" + player.getPetBuffCount() + "/" + (getMaxBuffs(player) + Math.max(0, getMaxDances(player))) + " buffs</font>";
		}
		
		tb.append("<table width=280 border=0 bgcolor=484848><tr>");
		tb.append("<td width=220 align=center> <font color=LEVEL>Buffing " + target + "" + status + "</font></td>");
		tb.append("<td width=65 align=right><button value=\"Back\" action=\"bypass -h nxs_npcbuffer_menu\" width=65 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		tb.append("</tr></table>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=5>");
		
		tb.append("<table width=281 bgcolor=2E2E2E>");
		tb.append("<tr>");
		
		String form = "";
		
		if(target.equalsIgnoreCase("player"))
		{
			if(player.hasPet())
				form = "<combobox width=75 height=17 var=target list=\"Player;Pet\">";
			else
				form = "<combobox width=75 height=17 var=target list=\"Player\">";
		}
		else
		{
			if(player.hasPet())
				form = "<combobox width=75 height=17 var=target list=\"Pet;Player\">";
			else
				form = "<combobox width=75 height=17 var=target list=\"Player\">";
		}
		
		tb.append("<td width=170><font color=696969>Buff target:</font></td><td align=left width=80>" + form + "</td><td width=45 align=right><button value=\"Set\" action=\"bypass -h nxs_npcbuffer_singlemenu $target " + selectedCategory + "\" width=45 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_CT1.button_df\"></td>");
		
		tb.append("</tr>");
		tb.append("</table>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=4>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=4>");
		
		tb.append("<table width=283 bgcolor=4E370E");
		
		int i = 0;
		for(String category : EventBuffer.getInstance().getAviableBuffs().keySet())
		{
			if(i == 0)
				tb.append("<tr>");
			
			if(selectedCategory != null && selectedCategory.equals(category))
				tb.append("<td align=center width=93><button value=\"" + category + "\" action=\"bypass -h nxs_npcbuffer_singlemenu " + target + " " + category + "\" width=90 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_CT1.Button_DF_Down\"></td>");
			else
				tb.append("<td align=center width=93><button value=\"" + category + "\" action=\"bypass -h nxs_npcbuffer_singlemenu " + target + " " + category + "\" width=90 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			
			if(++i == 3)
			{
				tb.append("</tr>");
				i = 0;
			}
		}
		
		tb.append("</table>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=1>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=6>");
		
		tb.append("<table width=281 bgcolor=2E2E2E");
		
		String category, name, icon;
		int id, level;
		int count = 0;
		i = 0;
		for(Entry<String, Map<Integer, Integer>> e : EventBuffer.getInstance().getAviableBuffs().entrySet())
		{
			category = e.getKey();
			
			if(!category.equals(selectedCategory))
				continue;
			
			for(Entry<Integer, Integer> buff : e.getValue().entrySet())
			{
				count ++;
				id = buff.getKey();
				level = buff.getValue();
				name = new SkillData(id, level).getName();
				
				name = trimName(name);
				
				if(i == 0)
					tb.append("<tr>");
				
				icon = formatSkillIcon("0000", id);
				
				tb.append("<td width=33 align=left><img src=\"" + icon + "\" width=32 height=32></td>");
				tb.append("<td width=95 align=left><button action=\"bypass -h nxs_npcbuffer_singlebuff " + target + " " + selectedCategory + " " + id + "\" value=\"" + name + "\" width=95 height=32 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				
				i++;
				if(i == 2)
				{
					tb.append("</tr>");
					i = 0;
				}
			}
		}
		
		tb.append("</table>");
		
		if(count == 0)
		{
			tb.append("<br><center><font color=AB7878>Please select a category first.</font></center>");
		}
		
		tb.append("<br>");
		
		tb.append("<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32 align=left><br>");
		
		tb.append("</body></html>");

		html = tb.toString();
		
		player.sendHtmlText(html);
		player.sendStaticPacket();
	}
	
	public void showCustomBufferMenu(PlayerEventInfo player)
	{
		String html;

		TextBuilder tb = new TextBuilder();
		
		tb.append("<html><title>NPC Buffer</title><body>");
		
		tb.append("<table width=280 border=0 bgcolor=383838><tr>");
		tb.append("<td width=280 align=center> <font color=ac9887><a action=\"bypass -h nxs_npcbuffer_bufferinfo\">Nexus Engine Buffer</a></font></td>");
		tb.append("</tr></table>");
		
		tb.append("<br1><center>");
		
		tb.append("<br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		
		tb.append("<button value=\"Buff - Player\" action=\"bypass -h nxs_npcbuffer_buffmenu player\" width=130 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br1>");
		//tb.append("<button value=\"Buff - Summon\" action=\"bypass -h nxs_npcbuffer_buffmenu pet\" width=130 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br><br>");
		tb.append("<button value=\"Edit Schemes\" action=\"bypass -h nxs_npcbuffer_edit_schemes npcbuffer\" width=130 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br><br>");
		
		tb.append("<button value=\"Single buffs\" action=\"bypass -h nxs_npcbuffer_singlemenu player null\" width=130 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br>");
		
		tb.append("<button value=\"Heal me\" action=\"bypass -h nxs_npcbuffer_heal player menu\" width=130 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br1>");
		tb.append("<button value=\"Cancel buffs\" action=\"bypass -h nxs_npcbuffer_cancelbuffs player menu\" width=130 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br1>");
		
		if(player.hasPet())
		{
			tb.append("<button value=\"Cancel summon buffs\" action=\"bypass -h nxs_npcbuffer_cancelbuffs pet menu\" width=130 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br1>");
		}
		
		tb.append("<br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		
		tb.append("</center>");
		
		
		tb.append("<font color=3c3c3c>______________</font> <font color=ae9977>Buffs Info</font> <font color=3c3c3c>______________</font><br1>");
		
		tb.append("<font color=9f9f9f>Number of buffs you have now: <font color=9f9f9f></font> <font color=ac9775>" + player.getBuffsCount() + " buffs</font></font><br1>");
		tb.append("<font color=9f9f9f>Your summon/pet's buff count: <font color=9f9f9f></font> <font color=ac9775>" + player.getPetBuffCount() + " buffs</font></font><br1>");
		tb.append("<font color=9f9f9f>Your class is allowed to have: <font color=9f9f9f></font> <font color=ac9775>" + getMaxBuffs(player) + " buffs</font></font><br1>");
		
		
		tb.append("</body></html>");

		html = tb.toString();
		
		player.sendHtmlText(html);
		player.sendStaticPacket();
	}
	
	protected void showBufferIntoPage(PlayerEventInfo player)
	{
		String html;

		TextBuilder tb = new TextBuilder();
		
		tb.append("<html><title>Mini Events</title><body>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=3>");
		
		tb.append("<table width=280 border=0 bgcolor=484848><tr>");
		tb.append("<td width=90 align=left> <font color=696969> Powered by:</font></td>");
		tb.append("<td width=130 align=left><font color=63AA1C><a action=\"bypass -h nxs_npcbuffer_bufferinfo\">Nexus Event Engine</a></font></td>");
		
		tb.append("<td width=65 align=right><button value=\"Back\" action=\"bypass -h nxs_npcbuffer_menu\" width=65 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		
		tb.append("</tr></table>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=280 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=280 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=270 height=1>");
		
		tb.append("<br><br><br><br><br><br><br><br><br><br>");
		
		tb.append("<center>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=278 height=1>");
		tb.append("<img src=\"L2UI.SquareGray\" width=278 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=278 height=3>");
		
		tb.append("<table width=100% bgcolor=3f3f3f>");
		tb.append("<tr><td width=100% align=center><font color=9f9f9f>This server is using <font color=9FBF80>Nexus Event engine</font></font></td></tr>");
		tb.append("<tr><td width=100% align=center><font color=9f9f9f>of version <font color=797979>" + NexusLoader.version + "</font>, developed by <font color=BEA481>hNoke</font>.</font><br></td></tr>");
		tb.append("<tr><td width=280 align=center><font color=9f9f9f>For more informations visit</font></td></tr><tr><td width=100% align=center><font color=BEA481>www.nexus-engine.net</font></td></tr>");
		tb.append("</table>");
		
		tb.append("<img src=\"L2UI.SquareBlank\" width=278 height=3>");
		tb.append("<img src=\"L2UI.SquareGray\" width=278 height=2>");
		tb.append("<img src=\"L2UI.SquareBlank\" width=278 height=1>");
		
		tb.append("<br><br><br><br><br><br><br><br><br><br>");
		
		tb.append("<center><font color=5F5F5F>If you find any problems, <br1>please contact me on my website.</font></center>");
		
		tb.append("</center>");
		
		tb.append("</body></html>");

		html = tb.toString();
		
		player.sendHtmlText(html);
		player.sendStaticPacket();
	}
	
	protected void buffPlayer(PlayerEventInfo player, String scheme, String target)
	{
		if(player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player.getOwner())  || Olympiad.getInstance().isRegisteredInComp(player.getOwner()))
			return;
		if(target.equals("player"))
		{
			try
			{
				for(int buffId : EventBuffer.getInstance().getBuffs(player, scheme))
				{
					player.getSkillEffects(buffId, EventBuffer.getInstance().getLevelFor(buffId));
				}
			}
			catch (Exception e)
			{
				player.sendMessage("wrong scheme");
			}
		}
		else if(target.equals("pet"))
		{
			try
			{
				for(int buffId : EventBuffer.getInstance().getBuffs(player, scheme))
				{
					player.getPetSkillEffects(buffId, EventBuffer.getInstance().getLevelFor(buffId));
				}
			}
			catch (Exception e)
			{
				player.sendMessage("wrong scheme");
			}
		}
	}
	
	protected void giveBuff(PlayerEventInfo player, String target, int id)
	{
		if(target.equalsIgnoreCase("player"))
		{
			try
			{
				player.getSkillEffects(id, EventBuffer.getInstance().getLevelFor(id));
			}
			catch (Exception e)
			{
				player.sendMessage("wrong scheme");
			}
		}
		else if(target.equalsIgnoreCase("pet"))
		{
			try
			{
				player.getPetSkillEffects(id, EventBuffer.getInstance().getLevelFor(id));
			}
			catch (Exception e)
			{
				player.sendMessage("wrong scheme");
			}
		}
	}
	
	protected void healPlayer(PlayerEventInfo player, String target)
	{
		if(target.equals("player"))
		{
			if(canHeal(player, false))
			{
				player.sendMessage("You've been healed.");
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
			}
		}
		else if(target.equals("pet"))
		{
			if(canHeal(player, true))
			{
				player.sendMessage("Your pet has been healed.");
				player.healPet();
			}
		}
	}
	
	private boolean canHeal(PlayerEventInfo player, boolean pet)
	{
		long time = System.currentTimeMillis();
		final int id = player.getPlayersId();
		if(!pet)
		{
			if(_healTimes.containsKey(id))
			{
				long healedTime = _healTimes.get(id);
				if(healedTime + HEAL_DELAY_MS > time)
				{
					long toWait = ((healedTime + HEAL_DELAY_MS) - time) / 1000;
					
					if(toWait > 60)
					{
						player.sendMessage("You must still wait about " + (toWait/60 +1) + " minutes.");
					}
					else
					{
						player.sendMessage("You must still wait " + (toWait) + " seconds.");
					}
					return false;
				}
				else
				{
					_healTimes.put(id, time);
					return true;
				}
			}
			else
			{
				_healTimes.put(id, time);				
				return true;
			}
		}
		else
		{
			if(_healTimesPets.containsKey(id))
			{
				long healedTime = _healTimesPets.get(id);
				if(healedTime + HEAL_DELAY_MS > time)
				{
					long toWait = ((healedTime + HEAL_DELAY_MS) - time) / 1000;
					
					if(toWait > 60)
					{
						player.sendMessage("You must still wait about " + (toWait/60 +1) + " minutes.");
					}
					else
					{
						player.sendMessage("You must still wait " + (toWait) + " seconds.");
					}
					return false;
				}
				else
				{
					_healTimesPets.put(id, time);
					return true;
				}
			}
			else
			{
				_healTimesPets.put(id, time);				
				return true;
			}
		}
	}

	@Override
	public boolean onCbBypass(PlayerEventInfo player, String bypass)
	{
		return super.onCbBypass(player, bypass);
	}
}
