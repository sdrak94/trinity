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
		Long time_solo = times.get(Integer.valueOf(InstanceManager.SOLO));
		Long time_kama = times.get(Integer.valueOf(InstanceManager.KAMALOKA_ID));
		Long time_embryo = times.get(Integer.valueOf(InstanceManager.EMBRYO_ID));
		Long time_ultraverse = times.get(Integer.valueOf(InstanceManager.ULTRAVERSE_ID));
		Long time_fafurion = times.get(Integer.valueOf(InstanceManager.FAFURION_ID));
		Long time_zaken = times.get(Integer.valueOf(InstanceManager.ZAKEN_ID));
		Long time_frintezza = times.get(Integer.valueOf(InstanceManager.FRINTEZZA_ID));
		Long time_freya = times.get(Integer.valueOf(InstanceManager.FREYA_ID));
		
		
		int soloinstance = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), InstanceManager.SOLO) - System.currentTimeMillis()) / 1000;
		int kamaloka = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), InstanceManager.KAMALOKA_ID) - System.currentTimeMillis()) / 1000;
		int embryo = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), InstanceManager.EMBRYO_ID) - System.currentTimeMillis()) / 1000;
		int ultraverse = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), InstanceManager.ULTRAVERSE_ID) - System.currentTimeMillis()) / 1000;
		int fafurion = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), InstanceManager.FAFURION_ID) - System.currentTimeMillis()) / 1000;
		int zaken = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), InstanceManager.ZAKEN_ID) - System.currentTimeMillis()) / 1000;
		int frintezza = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), InstanceManager.FRINTEZZA_ID) - System.currentTimeMillis()) / 1000;
		int freya = (int) (InstanceManager.getInstance().getInstanceTime(acha.getAccountName(), InstanceManager.FREYA_ID) - System.currentTimeMillis()) / 1000;
		int aliveRaids = L2RaidBossInstance.showNumberOfaliveRbs();
		
		int uptime = (int) acha.getUptime() / 1000;
		
		int h = uptime / 3600;
		int m = (uptime - h * 3600) / 60;
		int s = uptime - h * 3600 - m * 60;
		
		
		int hsolo = soloinstance / 3600;
		int msolo = (soloinstance - hsolo * 3600) / 60;
		int ssolo = soloinstance - hsolo * 3600 - msolo * 60;
		
		int hembryo = embryo / 3600;
		int membryo = (embryo - hembryo * 3600) / 60;
		int sembryo = embryo - hembryo * 3600 - membryo * 60;
		
		int hkamaloka = kamaloka / 3600;
		int mkamaloka = (kamaloka - hkamaloka * 3600) / 60;
		int skamaloka = kamaloka - hkamaloka * 3600 - mkamaloka * 60;

		int hultraverse = ultraverse / 3600;
		int multraverse = (ultraverse - hultraverse * 3600) / 60;
		int sultraverse = ultraverse - hultraverse * 3600 - multraverse * 60;
		
		int hfafurion = fafurion / 3600;
		int mfafurion = (fafurion - hfafurion * 3600) / 60;
		int sfafurion = fafurion - hfafurion * 3600 - mfafurion * 60;
		
		int hzaken = zaken / 3600;
		int mzaken = (zaken - hzaken * 3600) / 60;
		int szaken = zaken - hzaken * 3600 - mzaken * 60;
		
		int hfrintezza = frintezza / 3600;
		int mfrintezza = (frintezza - hfrintezza * 3600) / 60;
		int sfrintezza = frintezza - hfrintezza * 3600 - mfrintezza * 60;
		
		int hfreya = freya / 3600;
		int mfreya = (freya - hfreya * 3600) / 60;
		int sfreya = freya - hfreya * 3600 - mfreya * 60;
		
		
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

		if (time_solo != null && time_solo.longValue() > 0L)
		{
			html = html.replace("%soloinstance%", "Solo Instance: " + hsolo + " : " + msolo + " : " + ssolo);
		}
		else
		{
			html = html.replace("%soloinstance%", "Ready");
		}
		
		if (time_embryo != null && time_embryo.longValue() > 0L)
		{
			html = html.replace("%embryo%", hembryo + " : " + membryo + " :  " + sembryo);
		}
		else
		{
			html = html.replace("%embryo%", "Ready");
		}
		
		if (time_kama != null && time_kama.longValue() > 0L)
		{
			html = html.replace("%kamaloka%", hkamaloka + " : " + mkamaloka + " :  " + skamaloka);
		}
		else
		{
			html = html.replace("%kamaloka%", "Ready");
		}
		
		if (time_fafurion != null && time_fafurion.longValue() > 0L)
		{
			html = html.replace("%fafurion%", hfafurion + " : " + mfafurion + " :  " + sfafurion);
		}
		else
		{
			html = html.replace("%fafurion%", "Ready");
		}
		
		if (time_ultraverse != null && time_ultraverse.longValue() > 0L)
		{
			html = html.replace("%ultraverse%", hultraverse + " : " + multraverse + " : " + sultraverse);
		}
		else
		{
			html = html.replace("%ultraverse%", "Ready");
		}
		
		if (time_zaken != null && time_zaken.longValue() > 0L)
		{
			html = html.replace("%zaken%", hzaken + " : " + mzaken + " : " + szaken);
		}
		else
		{
			html = html.replace("%zaken%", "Ready");
		}

		if (time_frintezza != null && time_frintezza.longValue() > 0L)
		{
			html = html.replace("%frintezza%", hfrintezza + " : " + mfrintezza + " : " + sfrintezza);
		}
		else
		{
			html = html.replace("%frintezza%", "Ready");
		}
		
		if (time_freya != null && time_freya.longValue() > 0L)
		{
			html = html.replace("%freya%", hfreya + " : " + mfreya + " : " + sfreya);
		}
		else
		{
			html = html.replace("%freya%", "Ready");
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
