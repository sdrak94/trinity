package net.sf.l2j.gameserver.instancemanager;

import java.util.Collection;
import java.util.Map;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SpecialCamera;

/**
 * @author KKnD
 */
public class MovieMakerManager
{
	private static MovieMakerManager _instance;
	protected Map<Integer, Sequence> _sequence = new FastMap<Integer, Sequence>();
		
	public static MovieMakerManager getInstance()
	{
		if ( _instance == null )
			_instance = new MovieMakerManager();
		
		return _instance;
	}
	
	protected class Sequence
	{
		protected int sequenceId;
		protected int _objid;
		protected int _dist;
		protected int _yaw;
		protected int _pitch;
		protected int _time;
		protected int _duration;
		protected int _turn;
		protected int _rise;
		protected int _widescreen;
	}
	
	public void main_txt(L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(5);
        TextBuilder sb = new TextBuilder();
        sb.append("<html><title>Movie Maker</title><body>");
        sb.append("<table width=270>");
        sb.append("<tr><td>Sequences:</td></tr>");
        if (!_sequence.isEmpty())
        {
        	sb.append("</table>");
        	sb.append("<table width=270>");
        	for (Sequence s : _sequence.values())
            {
        		sb.append("<tr>");
                sb.append("<td>Sequence Id: "+s.sequenceId+"</td>");
                sb.append("</tr>");
            }
        	sb.append("</table>");
        	sb.append("<BR>");
        	sb.append("<BR>");
        	sb.append("<table width=270>");
        	sb.append("<tr>");
        	sb.append("<td>Sequence Id:</td>");
        	sb.append("<td><edit var=\"tsId\" width=120 height=15></td>");
        	sb.append("</tr>");
        	sb.append("</table>");
        	sb.append("<table width=270>");
        	sb.append("<tr>");
            sb.append("<td><button value=\"Edit\" width=80 action=\"bypass -h admin_editsequence $tsId\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
            sb.append("<td><button value=\"Delete\" width=80 action=\"bypass -h admin_delsequence $tsId\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
            sb.append("<td><button value=\"Play\" width=80 action=\"bypass -h admin_playseqq $tsId\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("<BR>");
            sb.append("<table width=270>");
            sb.append("<tr>");
            sb.append("<td>Broadcast to others nearby</td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("<table width=270>");
            sb.append("<tr>");
            sb.append("<td><button value=\"Broadcast\" width=80 action=\"bypass -h admin_broadcast $tsId\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
            sb.append("</tr>");
        	sb.append("</table>");
        	sb.append("<BR>");
        	sb.append("<table width=270>");
        	sb.append("<tr>");
            sb.append("<td>Broadcast movie to others nearby or play to yourself</td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("<table width=270>");
            sb.append("<tr>");
            sb.append("<td><button value=\"Broadcast\" width=80 action=\"bypass -h admin_broadmovie\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
            sb.append("<td><button value=\"Play\" width=80 action=\"bypass -h admin_playmovie\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
            sb.append("</tr>");
            sb.append("</table>");
        }
        sb.append("<table width=270>");
        sb.append("<tr>");
        sb.append("<td><button value=\"Add sequence\" width=100 action=\"bypass -h admin_addseq\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
        sb.append("</tr>");
        sb.append("</table></body></html>");
        html.setHtml(sb.toString());
        player.sendPacket(html);
	}
	
	public void play_sequence(int id, L2PcInstance pc)
	{
		if (_sequence.containsKey(id))
		{
			Sequence s = new Sequence();
			s = _sequence.get(id);
			pc.sendPacket(new SpecialCamera(s._objid, s._dist, s._yaw, s._pitch, s._time, s._duration, s._turn, s._rise, s._widescreen, 0));
		}
		else
		{
			pc.sendMessage("Wrong sequence Id.");
			main_txt(pc);
		}
	}
	
	public void brodcast_sequence(int id, L2PcInstance pc)
	{
		if (_sequence.containsKey(id))
		{
			Sequence s = new Sequence();
			s = _sequence.get(id);
			
			final Collection<L2Object> objs = pc.getKnownList().getKnownObjects().values();			
			for (L2Object object : objs)
			{
				if (object instanceof L2PcInstance)
					((L2PcInstance) object).sendPacket(new SpecialCamera(s._objid, s._dist, s._yaw, s._pitch, s._time, s._duration, s._turn, s._rise, s._widescreen, 0));
			}
			pc.sendPacket(new SpecialCamera(s._objid, s._dist, s._yaw, s._pitch, s._time, s._duration, s._turn, s._rise, s._widescreen, 0));
		}
		else
		{
			pc.sendMessage("Wrong sequence Id.");
			main_txt(pc);
		}
	}
	
	public void play_sequence(L2PcInstance pc, int objid, int dist, int yaw, int pitch, int time, int duration, int turn, int rise, int screen)
	{
		pc.sendPacket(new SpecialCamera(objid, dist, yaw, pitch, time, duration, turn, rise, screen, 0));
	}
	
	public void add_sequence(L2PcInstance pc, int seqId, int objid, int dist, int yaw, int pitch, int time, int duration, int turn, int rise, int screen)
	{
		if (!_sequence.containsKey(seqId))
		{
			Sequence s = new Sequence();
			s.sequenceId = seqId;
			s._objid = objid;
			s._dist = dist;
			s._yaw = yaw;
			s._pitch = pitch;
			s._time = time;
			s._duration = duration;
			s._turn = turn;
			s._rise = rise;
			s._widescreen = screen;
			_sequence.put(seqId, s);
			main_txt(pc);
		}
		else
		{
			pc.sendMessage("Sequence already exists.");
			main_txt(pc);
		}
	}
	
	public void add_seq(L2PcInstance pc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(5);
        TextBuilder sb = new TextBuilder();
        sb.append("<html><title>Movie Maker</title><body>");
        sb.append("<table width=270>");
        sb.append("<tr></tr>");
        sb.append("<tr><td>New Sequence:</td></tr>");
        sb.append("<tr><td>Sequence Id: </td></tr>");
        sb.append("<tr><td><td><edit var=\"tsId\"></td></tr>");
        sb.append("<tr><td>Distance: </td></tr>");
        sb.append("<tr><td><td><edit var=\"tdist\"></td></tr>");
        sb.append("<tr><td>Yaw: </td></tr>");
        sb.append("<tr><td><td><edit var=\"tyaw\"></td></tr>");
        sb.append("<tr><td>Pitch: </td></tr>");
        sb.append("<tr><td><td><edit var=\"tpitch\"></td></tr>");
        sb.append("<tr><td>Time: </td></tr>");
        sb.append("<tr><td><td><edit var=\"ttime\"></td></tr>");
        sb.append("<tr><td>Duration: </td></tr>");
        sb.append("<tr><td><td><edit var=\"tdur\"></td></tr>");
        sb.append("<tr><td>Turn: </td></tr>");
        sb.append("<tr><td><td><edit var=\"tturn\"></td></tr>");
        sb.append("<tr><td>Rise: </td></tr>");
        sb.append("<tr><td><td><edit var=\"trise\"></td></tr>");
        sb.append("<tr><td>WideScreen: </td></tr>");
        sb.append("<tr><td><td><combobox width=75 var=tscreen list=0;1></td></tr>");
        sb.append("<tr>");
        sb.append("</tr>");
        sb.append("</table>");
        sb.append("<BR>");
        sb.append("<BR>");
        sb.append("<table width=270>");
        sb.append("<tr>");
        sb.append("<td><button value=\"Add sequence\" width=100 action=\"bypass -h admin_addsequence $tsId $tdist $tyaw $tpitch $ttime $tdur $tturn $trise $tscreen\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
        sb.append("<td><button value=\"Play sequence\" width=100 action=\"bypass -h admin_playsequence $tdist $tyaw $tpitch $ttime $tdur $tturn $trise $tscreen\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
        sb.append("</tr>");
        sb.append("<tr>");
        sb.append("</tr>");
        sb.append("</table></body></html>");
        html.setHtml(sb.toString());
        pc.sendPacket(html);
	}
	
	public void edit_seq(int id, L2PcInstance pc)
	{
		if (_sequence.containsKey(id))
		{
			Sequence s = new Sequence();
			s = _sequence.get(id);
			NpcHtmlMessage html = new NpcHtmlMessage(5);
	        TextBuilder sb = new TextBuilder();
	        sb.append("<html><title>Movie Maker</title><body>");
	        sb.append("<table width=270>");
	        sb.append("<tr></tr>");
	        sb.append("<tr><td>Modyfy Sequence:</td></tr>");
	        sb.append("<tr><td>Sequence Id: "+s.sequenceId+"</td></tr>");
	        sb.append("<tr><td>Distance: "+s._dist+"</td></tr>");
	        sb.append("<tr><td><td><edit var=\"tdist\"></td></tr>");
	        sb.append("<tr><td>Yaw: "+s._yaw+"</td></tr>");
	        sb.append("<tr><td><td><edit var=\"tyaw\"></td></tr>");
	        sb.append("<tr><td>Pitch: "+s._pitch+"</td></tr>");
	        sb.append("<tr><td><td><edit var=\"tpitch\"></td></tr>");
	        sb.append("<tr><td>Time: "+s._time+"</td></tr>");
	        sb.append("<tr><td><td><edit var=\"ttime\"></td></tr>");
	        sb.append("<tr><td>Duration: "+s._duration+"</td></tr>");
	        sb.append("<tr><td><td><edit var=\"tdur\"></td></tr>");
	        sb.append("<tr><td>Turn: "+s._turn+"</td></tr>");
	        sb.append("<tr><td><td><edit var=\"tturn\"></td></tr>");
	        sb.append("<tr><td>Rise: "+s._rise+"</td></tr>");
	        sb.append("<tr><td><td><edit var=\"trise\"></td></tr>");
	        sb.append("<tr><td>WideScreen: "+s._widescreen+"</td></tr>");
	        sb.append("<tr><td><td><combobox width=75 var=tscreen list=0;1></td></tr>");
	        sb.append("</table>");
	        sb.append("<BR>");
	        sb.append("<BR>");
	        sb.append("<table width=270>");
	        sb.append("<tr>");
	        sb.append("<td><button value=\"Update sequence\" width=100 action=\"bypass -h admin_updatesequence "+s.sequenceId+" $tdist $tyaw $tpitch $ttime $tdur $tturn $trise $tscreen\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	        sb.append("<td><button value=\"Play sequence\" width=100 action=\"bypass -h admin_playsequence $tdist $tyaw $tpitch $ttime $tdur $tturn $trise $tscreen\" height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
	        sb.append("</tr>");
	        sb.append("</table></body></html>");
	        html.setHtml(sb.toString());
	        pc.sendPacket(html);
		}
	}
	
	public void update_sequence(L2PcInstance pc, int seqId, int objid, int dist, int yaw, int pitch, int time, int duration, int turn, int rise, int screen)
	{
		if (_sequence.containsKey(seqId))
		{
			_sequence.remove(seqId);
			Sequence s = new Sequence();
			s.sequenceId = seqId;
			s._objid = objid;
			s._dist = dist;
			s._yaw = yaw;
			s._pitch = pitch;
			s._time = time;
			s._duration = duration;
			s._turn = turn;
			s._rise = rise;
			s._widescreen = screen;
			_sequence.put(seqId, s);
			main_txt(pc);
		}
		else
		{
			pc.sendMessage("Sequence doesn't exist.");
			main_txt(pc);
		}
	}
	
	public void delete_sequence(int id, L2PcInstance pc)
	{
		if (_sequence.containsKey(id))
		{
			_sequence.remove(id);
			main_txt(pc);
		}
		else
		{
			pc.sendMessage("Sequence Id doesn't exist.");
			main_txt(pc);
		}
	}
	
	public void play_movie(int brodcast, L2PcInstance pc)
	{
		if (!_sequence.isEmpty())
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new play(1,brodcast,pc), 500);
		}
		else
		{
			pc.sendMessage("There are no sequences to play, please create one first.");
			main_txt(pc);
		}
	}
	
	private class play implements Runnable
	{
		private int _id;
		private int brod;
		private L2PcInstance _pc;
		
		public play(int id, int brodcast, L2PcInstance pc)
		{
			_id = id;
			brod = brodcast;
			_pc = pc;
		}
		
		public void run()
		{
			int id = 0;
			Sequence sec = new Sequence();
			if (_sequence.containsKey(_id))
			{
				id = _id+1;
				sec = _sequence.get(_id);
				if (brod == 1)
				{
					final Collection<L2Object> objs = _pc.getKnownList().getKnownObjects().values();					
					for (L2Object object : objs)
					{
						if (object instanceof L2PcInstance)
							((L2PcInstance) object).sendPacket(new SpecialCamera(sec._objid, sec._dist, sec._yaw, sec._pitch, sec._time, sec._duration, sec._turn, sec._rise, sec._widescreen, 0));
					}
					_pc.sendPacket(new SpecialCamera(sec._objid, sec._dist, sec._yaw, sec._pitch, sec._time, sec._duration, sec._turn, sec._rise, sec._widescreen, 0));
				}
				else
					_pc.sendPacket(new SpecialCamera(sec._objid, sec._dist, sec._yaw, sec._pitch, sec._time, sec._duration, sec._turn, sec._rise, sec._widescreen, 0));
				
				ThreadPoolManager.getInstance().scheduleGeneral(new play(id,brod,_pc), (sec._duration - 100));
			}
			else
			{
				_pc.sendMessage("Movie ended on "+(_id - 1)+" Sequence.");
				main_txt(_pc);
			}
		}
	}
}