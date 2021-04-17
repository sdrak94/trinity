/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.communitybbs.Manager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javolution.util.FastList;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.communitybbs.Manager.lunaservices.EventBBSManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.events.Communicator;
import net.sf.l2j.gameserver.model.events.manager.EventEngine;
import net.sf.l2j.gameserver.model.events.manager.EventsParser.Event;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public abstract class BaseBBSManager
{
	public abstract void parsecmd(String command, L2PcInstance activeChar);
	
	public abstract void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar);
	
	protected void separateAndSend(String html, L2PcInstance acha)
	{
		if (html == null)
			return;
		Map<Integer, Long> times = InstanceManager.getInstance().getAllInstanceTimes(acha.getAccountName());
		Long soloi = times.get(Integer.valueOf(2010));
		Long partyi = times.get(Integer.valueOf(2001));
		Long kamai = times.get(Integer.valueOf(2000));
		Long fafui = times.get(Integer.valueOf(2005));
		Long rbi = times.get(Integer.valueOf(2002));
		Long satani = times.get(Integer.valueOf(2050));
		Long godi = times.get(Integer.valueOf(2011));
		int soloinstance = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), 2010) - System.currentTimeMillis()) / 1000;
		int partyinstance = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), 2001) - System.currentTimeMillis()) / 1000;
		int kamaloka = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), 2000) - System.currentTimeMillis()) / 1000;
		int rbuniverse = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), 2002) - System.currentTimeMillis()) / 1000;
		int satan = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), 2050) - System.currentTimeMillis()) / 1000;
		int god = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), 2011) - System.currentTimeMillis()) / 1000;
		int fafurion = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), 2005) - System.currentTimeMillis()) / 1000;
		int aliveRaids = L2RaidBossInstance.showNumberOfaliveRbs();
		int uptime = (int) acha.getUptime() / 1000;
		int h = uptime / 3600;
		int m = (uptime - h * 3600) / 60;
		int s = uptime - h * 3600 - m * 60;
		int hsatan = satan / 3600;
		int msatan = (satan - hsatan * 3600) / 60;
		int ssatan = satan - hsatan * 3600 - msatan * 60;
		int hsolo = soloinstance / 3600;
		int msolo = (soloinstance - hsolo * 3600) / 60;
		int ssolo = soloinstance - hsolo * 3600 - msolo * 60;
		int hparty = partyinstance / 3600;
		int mparty = (partyinstance - hparty * 3600) / 60;
		int sparty = partyinstance - hparty * 3600 - mparty * 60;
		int hgod = god / 3600;
		int mgod = (god - hgod * 3600) / 60;
		int sgod = god - hgod * 3600 - mgod * 60;
		int hkamaloka = kamaloka / 3600;
		int mkamaloka = (kamaloka - hkamaloka * 3600) / 60;
		int skamaloka = kamaloka - hkamaloka * 3600 - mkamaloka * 60;

		int hfafurion = fafurion / 3600;
		int mfafurion = (fafurion - hfafurion * 3600) / 60;
		int sfafurion = fafurion - hfafurion * 3600 - mfafurion * 60;
		
		int hrb = rbuniverse / 3600;
		int mrb = (rbuniverse - hrb * 3600) / 60;
		int srb = rbuniverse - hrb * 3600 - mrb * 60;
		int tg = GameTimeController.getInstance().getGameTime();
		String hg = "" + (tg / 60) % 24;
		String mg;
		if (tg % 60 < 10)
			mg = "0" + tg % 60;
		else
			mg = "" + tg % 60;
		String day = "Day: ";
		
		if (GameTimeController.getInstance().isNowNight())
		{
			day = "Night: " ;
		}
		else
		{
			day = "Day: ";
		}
		String GameTime = day + hg +":"+ mg ;
		EventBBSManager.getInstance().generateScheduleLine();
		
		Date currentTime = new Date(System.currentTimeMillis());
		
		int totalClans = ClanTable.getInstance().getClans().length;
		
		Date date = new Date(GameServer.dateTimeServerStarted.getTimeInMillis());
		TimeZone athensTimeZone = TimeZone.getTimeZone("UTC");
		
		Calendar calendar = Calendar.getInstance(athensTimeZone);
		
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(" EE/MM/ HH:mm zzz", Locale.UK);
		simpleDateFormat.setTimeZone(athensTimeZone);
		
		calendar.setTimeInMillis(date.getTime());
		Date Date = calendar.getTime();

		simpleDateFormat.format(Date);

		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm || dd/MM/yyyy zzz");
		SimpleDateFormat dateFormat2 = new SimpleDateFormat("HH:mm || dd/MM/yyyy zzz");

		String event = Communicator.getInstance().getNextEvent();
		String eventIcon = Communicator.getInstance().getNextEventIcon();
		String next = "Next Event:";
		
		if(EventEngine.getInstance()._isEventActive)
		{
			Event e = EventEngine.getInstance().getActiveEvent();
			if(e!=null)
			{
				event = e.getType() + " " + e.getName();
				eventIcon = Communicator.getInstance().generateIcon(e.getType());
				next = "Active Event: ";
			}
		}

		html = html.replace("%next%", next);
		html = html.replace("%nextEventIcon%", eventIcon);
		html = html.replace("%nextEvent%", event);
		html = html.replace("%rbs%", String.valueOf(aliveRaids));
		html = html.replace("%name%", acha.getName());
		html = html.replace("%lvl%", " " + acha.getLevel());
		html = html.replace("%ip%", " " + acha.getIP());
		html = html.replace("%uptime%", " " + h + " : " + m + " : " + s);
		html = html.replace("%class%", " " + (acha.getTemplate()).className);
		html = html.replace("%pvp%", " " + acha.getPvpKills());
		html = html.replace("%fame%", " " + acha.getFame());
		html = html.replace("%pk%", " " + acha.getPkKills());
		html = html.replace("%spree%", " " + acha.get_streak());
		html = html.replace("%olypoints%", " " + Olympiad.getInstance().getNoblePoints(acha.getObjectId()));
		html = html.replace("%serverr%", String.valueOf(dateFormat.format(Date)));
		html = html.replace("%gameTime%",  GameTime);
		html = html.replace("%time%", String.valueOf(dateFormat2.format(currentTime)));
		html = html.replace("%clans%", String.valueOf(totalClans));
		html = html.replace("%onlinecount%", String.valueOf(L2World.getInstance().getAllPlayersCount()));
		if (acha.getClan() != null)
		{
			html = html.replace("%clan%", "Clan: " + acha.getClan().getName());
		}
		else
		{
			html = html.replace("%clan%", "Clan: None");
		}
		if (satani != null && satani.longValue() > 0L)
		{
			html = html.replace("%satan%", "Satan's World: " + hsatan + " : " + msatan + " : " + ssatan);
		}
		else
		{
			html = html.replace("%satan%", "Satan's World: Ready");
		}
		if (godi != null && godi.longValue() > 0L)
		{
			html = html.replace("%god%", "God's World: " + hgod + " : " + mgod + " : " + sgod);
		}
		else
		{
			html = html.replace("%god%", "God's World: Ready");
		}
		if (soloi != null && soloi.longValue() > 0L)
		{
			html = html.replace("%soloinstance%", "Solo Instance: " + hsolo + " : " + msolo + " : " + ssolo);
		}
		else
		{
			html = html.replace("%soloinstance%", "SoloInstance: Ready");
		}
		if (partyi != null && partyi.longValue() > 0L)
		{
			html = html.replace("%partyinstance%", hparty + " : " + mparty + " :  " + sparty);
		}
		else
		{
			html = html.replace("%partyinstance%", "Ready");
		}
		if (kamai != null && kamai.longValue() > 0L)
		{
			html = html.replace("%kamaloka%", hkamaloka + " : " + mkamaloka + " :  " + skamaloka);
		}
		else
		{
			html = html.replace("%kamaloka%", "Ready");
		}
		if (fafui != null && fafui.longValue() > 0L)
		{
			html = html.replace("%fafurion%", hfafurion + " : " + mfafurion + " :  " + sfafurion);
		}
		else
		{
			html = html.replace("%fafurion%", "Ready");
		}
		if (rbi != null && rbi.longValue() > 0L)
		{
			html = html.replace("%rbuniverse%", hrb + " : " + mrb + " : " + srb);
		}
		else
		{
			html = html.replace("%rbuniverse%", "Ready");
		}
		acha.sendPacket(new ShowBoard(html, "101"));
	}
	
	/**
	 * @param html
	 */
	protected void send1001(String html, L2PcInstance acha)
	{
		if (html.length() < 16360)
		{
			acha.sendPacket(new ShowBoard(html, "1001"));
		}
	}
	
	/**
	 * @param i
	 */
	protected void send1002(L2PcInstance acha)
	{
		send1002(acha, " ", " ", "0");
	}
	
	/**
	 * @param activeChar
	 * @param string
	 * @param string2
	 */
	protected void send1002(L2PcInstance activeChar, String string, String string2, String string3)
	{
		List<String> _arg = new FastList<String>();
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add(activeChar.getName());
		_arg.add(Integer.toString(activeChar.getObjectId()));
		_arg.add(activeChar.getAccountName());
		_arg.add("9");
		_arg.add(string2);
		_arg.add(string2);
		_arg.add(string);
		_arg.add(string3);
		_arg.add(string3);
		_arg.add("0");
		_arg.add("0");
		activeChar.sendPacket(new ShowBoard(_arg));
	}
}
