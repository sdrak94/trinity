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
package scripts.ai.fantasy;

import java.text.SimpleDateFormat;
import java.util.Map;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;


/**
 * MC show script
 * @author Kerberos
 */
public class MC_Show extends Quest
{
	private static int mc = 32433;
	private static int[] singers = {32431,32432};
	private static int[] circus = {32442,32443,32444,32445,32446};
	private static int[] individuals = {32439,32440,32441};
	private static int[] showstuff = {32424,32425,32426,32427,32428};
	private static boolean isStarted = false;
	private static String[] text = {
		"How come people are not here... We are about to start the show.. Hmm",
		"Ugh, I have butterflies in my stomach.. The show starts soon...",
		"Thank you all for coming here tonight.","It is an honor to have the special show today.",
		"Our Fantasy Isle is fully committed to your happiness.",
		"Now I'd like to introduce the most beautiful singer in Aden. Please welcome Leyla Mira!",
		"Here she comes!","Thank you very much, Leyla. Next is",
		"It was very difficult to invite this first group that just came back from their world tour. Let's welcome the Fantasy Isle Circus!",
		"Come on ~ everyone","Did you like it? That was so amazing.",
		"Now we also invited individuals with special talents.","Let's welcome the first person here!",
		";;;;;;Oh","Okay, now here comes the next person. Come on up please.",
		"Oh, it looks like something great is going to happen, right?", "Oh, my ;;;;",
		"That's g- .. great. Now, here comes the last person.","Now this is the end of today's show.",
		"How was it? I am not sure if you really enjoyed it.",
		"Please remember that Fantasy Isle is always planning a lot of great shows for you.",
		"Well, I wish I could continue all night long, but this is it for today. Thank you."
	};
	
	private static Map<String, Object[]> talks = new FastMap<String, Object[]>();
	private static Map<String, Object[]> walks = new FastMap<String, Object[]>();
	
	private void load()
	{
		talks.put("1", new Object[]{text[1],"2",1000});
        talks.put("2", new Object[]{text[2],"3",6000});
        talks.put("3", new Object[]{text[3],"4",4000});
        talks.put("4", new Object[]{text[4],"5",5000});
        talks.put("5", new Object[]{text[5],"6",3000});
        talks.put("8", new Object[]{text[8],"9",5000});
        talks.put("9", new Object[]{text[9],"10",5000});
        talks.put("12", new Object[]{text[11],"13",5000});
        talks.put("13", new Object[]{text[12],"14",5000});
        talks.put("15", new Object[]{text[13],"16",5000});
        talks.put("16", new Object[]{text[14],"17",5000});
        talks.put("18", new Object[]{text[16],"19",5000});
        talks.put("19", new Object[]{text[17],"20",5000});
        talks.put("21", new Object[]{text[18],"22",5000});
        talks.put("22", new Object[]{text[19],"23",400});
        talks.put("25", new Object[]{text[20],"26",5000});
        talks.put("26", new Object[]{text[21],"27",5400});
        
        walks.put("npc1_1" , new Object[]{-56546,-56384,-2008,"npc1_2",1200});
        walks.put("npc1_2" , new Object[]{-56597,-56384,-2008,"npc1_3",1200});
     	walks.put("npc1_3" , new Object[]{-56596,-56428,-2008,"npc1_4",1200});
     	walks.put("npc1_4" , new Object[]{-56593,-56474,-2008,"npc1_5",1000});
     	walks.put("npc1_5" , new Object[]{-56542,-56474,-2008,"npc1_6",1000});
     	walks.put("npc1_6" , new Object[]{-56493,-56473,-2008,"npc1_7",2000});
     	walks.put("npc1_7" , new Object[]{-56495,-56425,-2008,"npc1_1",4000});
     	walks.put("npc2_1" , new Object[]{-56550,-56291,-2008,"npc2_2",1200});
     	walks.put("npc2_2" , new Object[]{-56601,-56293,-2008,"npc2_3",1200});
     	walks.put("npc2_3" , new Object[]{-56603,-56247,-2008,"npc2_4",1200});
     	walks.put("npc2_4" , new Object[]{-56605,-56203,-2008,"npc2_5",1000});
     	walks.put("npc2_5" , new Object[]{-56553,-56202,-2008,"npc2_6",1100});
     	walks.put("npc2_6" , new Object[]{-56504,-56200,-2008,"npc2_7",2000});
     	walks.put("npc2_7" , new Object[]{-56503,-56243,-2008,"npc2_1",4000});
     	walks.put("npc3_1" , new Object[]{-56500,-56290,-2008,"npc3_2",1200});
     	walks.put("npc3_2" , new Object[]{-56551,-56313,-2008,"npc3_3",1200});
     	walks.put("npc3_3" , new Object[]{-56601,-56293,-2008,"npc3_4",1200});
     	walks.put("npc3_4" , new Object[]{-56651,-56294,-2008,"npc3_5",1200});
     	walks.put("npc3_5" , new Object[]{-56653,-56250,-2008,"npc3_6",1200});
     	walks.put("npc3_6" , new Object[]{-56654,-56204,-2008,"npc3_7",1200});
     	walks.put("npc3_7" , new Object[]{-56605,-56203,-2008,"npc3_8",1200});
     	walks.put("npc3_8" , new Object[]{-56554,-56202,-2008,"npc3_9",1200});
     	walks.put("npc3_9" , new Object[]{-56503,-56200,-2008,"npc3_10",1200});
     	walks.put("npc3_10", new Object[]{-56502,-56244,-2008,"npc3_1",900});
     	walks.put("npc4_1" , new Object[]{-56495,-56381,-2008,"npc4_2",1200});
     	walks.put("npc4_2" , new Object[]{-56548,-56383,-2008,"npc4_3",1200});
     	walks.put("npc4_3" , new Object[]{-56597,-56383,-2008,"npc4_4",1200});
     	walks.put("npc4_4" , new Object[]{-56643,-56385,-2008,"npc4_5",1200});
     	walks.put("npc4_5" , new Object[]{-56639,-56436,-2008,"npc4_6",1200});
     	walks.put("npc4_6" , new Object[]{-56639,-56473,-2008,"npc4_7",1200});
     	walks.put("npc4_7" , new Object[]{-56589,-56473,-2008,"npc4_8",1200});
     	walks.put("npc4_8" , new Object[]{-56541,-56473,-2008,"npc4_9",1200});
     	walks.put("npc4_9" , new Object[]{-56496,-56473,-2008,"npc4_10",1200});
     	walks.put("npc4_10", new Object[]{-56496,-56429,-2008,"npc4_1",900});
     	walks.put("npc5_1" , new Object[]{-56549,-56335,-2008,"npc5_2",1000});
     	walks.put("npc5_2" , new Object[]{-56599,-56337,-2008,"npc5_3",2000});
     	walks.put("npc5_3" , new Object[]{-56649,-56341,-2008,"npc5_4",26000});
     	walks.put("npc5_4" , new Object[]{-56600,-56341,-2008,"npc5_5",1000});
     	walks.put("npc5_5" , new Object[]{-56553,-56341,-2008,"npc5_6",1000});
     	walks.put("npc5_6" , new Object[]{-56508,-56331,-2008,"npc5_2",8000});
     	walks.put("npc6_1" , new Object[]{-56595,-56428,-2008,"npc6_2",1000});
     	walks.put("npc6_2" , new Object[]{-56596,-56383,-2008,"npc6_3",1000});
     	walks.put("npc6_3" , new Object[]{-56648,-56384,-2008,"npc6_4",1000});
     	walks.put("npc6_4" , new Object[]{-56645,-56429,-2008,"npc6_5",1000});
     	walks.put("npc6_5" , new Object[]{-56644,-56475,-2008,"npc6_6",1000});
     	walks.put("npc6_6" , new Object[]{-56595,-56473,-2008,"npc6_7",1000});
     	walks.put("npc6_7" , new Object[]{-56542,-56473,-2008,"npc6_8",1000});
     	walks.put("npc6_8" , new Object[]{-56492,-56472,-2008,"npc6_9",1200});
     	walks.put("npc6_9" , new Object[]{-56495,-56426,-2008,"npc6_10",2000});
     	walks.put("npc6_10", new Object[]{-56540,-56426,-2008,"npc6_1",3000});
     	walks.put("npc7_1" , new Object[]{-56603,-56249,-2008,"npc7_2",1000});
     	walks.put("npc7_2" , new Object[]{-56601,-56294,-2008,"npc7_3",1000});
     	walks.put("npc7_3" , new Object[]{-56651,-56295,-2008,"npc7_4",1000});
     	walks.put("npc7_4" , new Object[]{-56653,-56248,-2008,"npc7_5",1000});
     	walks.put("npc7_5" , new Object[]{-56605,-56203,-2008,"npc7_6",1000});
     	walks.put("npc7_6" , new Object[]{-56554,-56202,-2008,"npc7_7",1000});
     	walks.put("npc7_7" , new Object[]{-56504,-56201,-2008,"npc7_8",1000});
     	walks.put("npc7_8" , new Object[]{-56502,-56247,-2008,"npc7_9",1200});
     	walks.put("npc7_9" , new Object[]{-56549,-56248,-2008,"npc7_10",2000});
     	walks.put("npc7_10", new Object[]{-56549,-56248,-2008,"npc7_1",3000});
     	walks.put("npc8_1" , new Object[]{-56493,-56426,-2008,"npc8_2",1000});
     	walks.put("npc8_2" , new Object[]{-56497,-56381,-2008,"npc8_3",1200});
     	walks.put("npc8_3" , new Object[]{-56544,-56381,-2008,"npc8_4",1200});
     	walks.put("npc8_4" , new Object[]{-56596,-56383,-2008,"npc8_5",1200});
     	walks.put("npc8_5" , new Object[]{-56594,-56428,-2008,"npc8_6",900});
     	walks.put("npc8_6" , new Object[]{-56645,-56429,-2008,"npc8_7",1200});
     	walks.put("npc8_7" , new Object[]{-56647,-56384,-2008,"npc8_8",1200});
     	walks.put("npc8_8" , new Object[]{-56649,-56362,-2008,"npc8_9",9200});
     	walks.put("npc8_9" , new Object[]{-56654,-56429,-2008,"npc8_10",1200});
     	walks.put("npc8_10", new Object[]{-56644,-56474,-2008,"npc8_11",900});
     	walks.put("npc8_11", new Object[]{-56593,-56473,-2008,"npc8_12",1100});
     	walks.put("npc8_12", new Object[]{-56543,-56472,-2008,"npc8_13",1200});
     	walks.put("npc8_13", new Object[]{-56491,-56471,-2008,"npc8_1",1200});
     	walks.put("npc9_1" , new Object[]{-56505,-56246,-2008,"npc9_2",1000});
     	walks.put("npc9_2" , new Object[]{-56504,-56291,-2008,"npc9_3",1200});
     	walks.put("npc9_3" , new Object[]{-56550,-56291,-2008,"npc9_4",1200});
     	walks.put("npc9_4" , new Object[]{-56600,-56292,-2008,"npc9_5",1200});
     	walks.put("npc9_5" , new Object[]{-56603,-56248,-2008,"npc9_6",900});
     	walks.put("npc9_6" , new Object[]{-56653,-56249,-2008,"npc9_7",1200});
     	walks.put("npc9_7" , new Object[]{-56651,-56294,-2008,"npc9_8",1200});
     	walks.put("npc9_8" , new Object[]{-56650,-56316,-2008,"npc9_9",9200});
     	walks.put("npc9_9" , new Object[]{-56660,-56250,-2008,"npc9_10",1200});
     	walks.put("npc9_10", new Object[]{-56656,-56205,-2008,"npc9_11",900});
     	walks.put("npc9_11", new Object[]{-56606,-56204,-2008,"npc9_12",1100});
     	walks.put("npc9_12", new Object[]{-56554,-56203,-2008,"npc9_13",1200});
     	walks.put("npc9_13", new Object[]{-56506,-56203,-2008,"npc9_1",1200});
     	walks.put("24"     , new Object[]{-56730,-56340,-2008,"25",1800});
     	walks.put("27"     , new Object[]{-56702,-56340,-2008,"29",1800});
	}
	
	public MC_Show(int id,String name,String descr)
	{
        super(id,name,descr);
        addEventId(32433, QuestEventType.ON_SPAWN);
        addEventId(32431, QuestEventType.ON_SPAWN);
        addEventId(32432, QuestEventType.ON_SPAWN);
        addEventId(32442, QuestEventType.ON_SPAWN);
        addEventId(32443, QuestEventType.ON_SPAWN);
        addEventId(32444, QuestEventType.ON_SPAWN);
        addEventId(32445, QuestEventType.ON_SPAWN);
        addEventId(32446, QuestEventType.ON_SPAWN);
        addEventId(32424, QuestEventType.ON_SPAWN);
        addEventId(32425, QuestEventType.ON_SPAWN);
        addEventId(32426, QuestEventType.ON_SPAWN);
        addEventId(32427, QuestEventType.ON_SPAWN);
        addEventId(32428, QuestEventType.ON_SPAWN);
        load();
        scheduleTimer();
	}
	
	private int getGameTime()
	{
		return GameTimeController.getInstance().getGameTime();
	}
	
	private void scheduleTimer()
	{
		int gameTime = getGameTime();
		int hours = (gameTime/60)%24;
		int minutes = gameTime%60;
		int hourDiff, minDiff;
		hourDiff = (20 - hours);
		if (hourDiff < 0)
			hourDiff = 24-(hourDiff*=-1);
		minDiff = (30 - minutes);
		if (minDiff < 0)
			minDiff = 60-(minDiff*=-1);
		long diff;
		hourDiff*=60*60*1000;
		minDiff*=60*1000;
		diff = hourDiff+minDiff;
		if (Config.DEBUG)
		{
			SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			_log.info("Fantasy Isle: MC show script starting at " + format.format(System.currentTimeMillis()+diff)+ " and is scheduled each next 4 hours.");
		}
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new StartMCShow(), diff, 14400000L);
		
	}
	
	private void autoChat(L2Npc npc, String text, int type)
	{
		npc.broadcastPacket(new NpcSay(npc.getObjectId(), type, npc.getNpcId(), text));
	}
	
	public String onSpawn(L2Npc npc)
	{
		if (isStarted)
		{
			switch (npc.getNpcId())
			{
				case 32433:
					autoChat(npc,text[0],1);
					startQuestTimer("1",30000, npc, null);
					break;
				case 32431:
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,new L2CharPosition(-56657,-56338,-2008,33102));
					startQuestTimer("social1",6000, npc, null, true);
					startQuestTimer("7",215000, npc, null);
					break;
				case 32432:
					startQuestTimer("social1",6000, npc, null, true);
					startQuestTimer("7",215000, npc, null);
					break;
				case 32442:
				case 32443:
				case 32444:
				case 32445:
				case 32446:
					startQuestTimer("11",100000, npc, null);
					break;
				case 32424:
				case 32425:
				case 32426:
				case 32427:
				case 32428:
					startQuestTimer("social1",5500, npc, null);
					startQuestTimer("social1",12500, npc, null);
					startQuestTimer("28",19700, npc, null);
					break;
			}
		}
		return super.onSpawn(npc);
	}
	public String onAdvEvent (String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("Start"))
		{
			isStarted = true;
			addSpawn(mc,-56698,-56430,-2008,32768,false,0);
		}
		else if (npc != null && isStarted)
		{
			if (event.equalsIgnoreCase("6"))
			{
				autoChat(npc,text[6],1);
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(-56511,-56647,-2008,36863));
				npc.broadcastPacket(new PlaySound(1, "NS22_F", 0, 0, 0, 0, 0));
				addSpawn(singers[0],-56344,-56328,-2008,32768,false,224000);
				addSpawn(singers[1],-56552,-56245,-2008,36863,false,224000);
				addSpawn(singers[1],-56546,-56426,-2008,28672,false,224000);
				addSpawn(singers[1],-56570,-56473,-2008,28672,false,224000);
				addSpawn(singers[1],-56594,-56516,-2008,28672,false,224000);
				addSpawn(singers[1],-56580,-56203,-2008,36863,false,224000);
				addSpawn(singers[1],-56606,-56157,-2008,36863,false,224000);
				startQuestTimer("7",215000, npc, null);
			}
			else if (event.equalsIgnoreCase("7"))
			{
				switch (npc.getNpcId())
				{
					case 32433:
						autoChat(npc,text[7],1);
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,new L2CharPosition(-56698,-56430,-2008,32768));
						startQuestTimer("8",12000, npc, null);
						break;
					default:
						cancelQuestTimer("social1", npc, null);
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,new L2CharPosition(-56594,-56064,-2008,32768));
						break;
				}
			}
			else if (event.equalsIgnoreCase("10"))
			{
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,new L2CharPosition(-56483,-56665,-2034,32768));
				npc.broadcastPacket(new PlaySound(1, "TP05_F", 0, 0, 0, 0, 0));
				startQuestTimer("npc1_1",3000, addSpawn(circus[0],-56495,-56375,-2008,32768,false,101000), null);
				startQuestTimer("npc2_1",3000, addSpawn(circus[0],-56491,-56289,-2008,32768,false,101000), null);
				startQuestTimer("npc3_1",3000, addSpawn(circus[1],-56502,-56246,-2008,32768,false,101000), null);
				startQuestTimer("npc4_1",3000, addSpawn(circus[1],-56496,-56429,-2008,32768,false,101000), null);
				startQuestTimer("npc5_1",3500, addSpawn(circus[2],-56505,-56334,-2008,32768,false,101000), null);
				startQuestTimer("npc6_1",4000, addSpawn(circus[3],-56545,-56427,-2008,32768,false,101000), null);
				startQuestTimer("npc7_1",4000, addSpawn(circus[3],-56552,-56248,-2008,32768,false,101000), null);
				startQuestTimer("npc8_1",3000, addSpawn(circus[4],-56493,-56473,-2008,32768,false,101000), null);
				startQuestTimer("npc9_1",3000, addSpawn(circus[4],-56504,-56201,-2008,32768,false,101000), null);
				startQuestTimer("11",100000, npc, null);
			}
			else if (event.equalsIgnoreCase("11"))
			{
				switch (npc.getNpcId())
				{
					case 32433:
						autoChat(npc,text[10],1);
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,new L2CharPosition(-56698,-56430,-2008,32768));
						startQuestTimer("12",5000, npc, null);
						break;
					default:
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,new L2CharPosition(-56343,-56330,-2008,32768));
						break;
				}
			}
			else if (event.equalsIgnoreCase("14"))
			{
				startQuestTimer("social1",2000, addSpawn(individuals[0],-56700,-56385,-2008,32768,false,49000), null);
				startQuestTimer("15",7000, npc, null);
			}
			else if (event.equalsIgnoreCase("17"))
			{
				autoChat(npc,text[15],1);
				startQuestTimer("social1",2000,addSpawn(individuals[1],-56700,-56340,-2008,32768,false,32000), null);
				startQuestTimer("18",9000, npc, null);
			}
			else if (event.equalsIgnoreCase("20"))
			{
				startQuestTimer("social1",2000, addSpawn(individuals[2],-56703,-56296,-2008,32768,false,13000), null);
				startQuestTimer("21",8000, npc, null);
			}
			else if (event.equalsIgnoreCase("23"))
			{
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,new  L2CharPosition(-56702,-56340,-2008,32768));
				startQuestTimer("24",2800, npc, null);
				addSpawn(showstuff[0],-56672,-56406,-2000,32768,false,20900);
			    addSpawn(showstuff[1],-56648,-56368,-2000,32768,false,20900);
			    addSpawn(showstuff[2],-56608,-56338,-2000,32768,false,20900);
			    addSpawn(showstuff[3],-56652,-56307,-2000,32768,false,20900);
			    addSpawn(showstuff[4],-56672,-56272,-2000,32768,false,20900);
			}
			else if (event.equalsIgnoreCase("28"))
			{
				autoChat(npc,"We love you.",0);
				startQuestTimer("social1",1, npc, null);
			}
			else if (event.equalsIgnoreCase("29"))
			{
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,new  L2CharPosition(-56730,-56340,-2008,32768));
				startQuestTimer("clean_npc",4100, npc, null);
				startQuestTimer("timer_check",60000, null, null, true);
			}
			else if (event.equalsIgnoreCase("social1"))
				npc.broadcastPacket(new SocialAction(npc.getObjectId(),1));
			else if (event.equalsIgnoreCase("clean_npc"))
			{
				isStarted = false;
				npc.deleteMe();
			}
			else
			{
				if (talks.containsKey(event))
				{
					String text = (String) talks.get(event)[0];
					String nextEvent = (String) talks.get(event)[1];
					int time = (Integer) talks.get(event)[2];
					autoChat(npc,text,1);
					startQuestTimer(nextEvent,time, npc, null);
				}
				else if (walks.containsKey(event))
				{
					int x = (Integer) walks.get(event)[0];
					int y = (Integer) walks.get(event)[1];
					int z = (Integer) walks.get(event)[2];
					String nextEvent = (String) walks.get(event)[3];
					int time = (Integer) walks.get(event)[4];
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,new L2CharPosition(x,y,z,0));
					startQuestTimer(nextEvent,time, npc, null);
				}
			}
		}
		return null;
	}
	
    public static void main(String[] args)
    {
    	// now call the constructor (starts up the ai)
    	new MC_Show(-1,"MC_Show","fantasy_isle");
    }
}