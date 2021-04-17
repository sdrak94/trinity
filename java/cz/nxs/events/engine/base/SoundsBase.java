/**
 * 
 */
package cz.nxs.events.engine.base;

import java.util.Collection;

import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 *
 */
public enum SoundsBase
{
	PLAY("eventSnd.n.Play"),
	
	ZOMBIES_NEW("eventSnd.n.new_mutant"),
	ZOMBIES_YOU_HAVE_MUTATED("eventSnd.n.you_have_mutated"),
	
	BLUE_WINS("eventSnd.n.blue_team_is_the_winner"),
	RED_WINS("eventSnd.n.red_team_is_the_winner"),
	BLUE_WINSROUND("eventSnd.n.blue_team_wins_the_round"),
	RED_WINSROUND("eventSnd.n.red_team_wins_the_round"),
	TEAMS_YOU_HAVE_LOST_THE_MATCH("eventSnd.teams.You_Have_lost_the_Match"),
	ROUND_END("eventSnd.rounds.EndOfRound"),
	ROUND_NEWIN("eventSnd.rounds.NewRoundIn"),
	
	SPREE_KILLING_SPREE("eventSnd.kills.Killing_Spree"),
	SPREE_DOMINATING("eventSnd.kills.Dominating"),
	SPREE_DOUBLE_KILL("eventSnd.kills.Double_Kill"),
	SPREE_FIRST_BLOOD("eventSnd.kills.first_blood"),
	SPREE_GODLIKE("eventSnd.kills.GodLike"),
	SPREE_HEADSHOT("eventSnd.kills.Headshot"),
	SPREE_HOLYSHIT("eventSnd.kills.HolyShit"),
	SPREE_HOLYSHIT_F("eventSnd.kills.HolyShit_F"),
	SPREE_HUMILIATING_DEFEAT("eventSnd.kills.Humiliating_defeat"),
	SPREE_LUDICROUSKILL("eventSnd.kills.LudicrousKill"),
	SPREE_LUDICROUSKILL_F("eventSnd.kills.LudicrousKill_F"),
	SPREE_MEGAKILL("eventSnd.kills.MegaKill"),
	SPREE_MONSTERKILL("eventSnd.kills.MonsterKill"),
	SPREE_MONSTERKILL_F("eventSnd.kills.MonsterKill_F"),
	SPREE_MULTIKILL("eventSnd.kills.MultiKill"),
	SPREE_RAMPAGE("eventSnd.kills.Rampage"),
	SPREE_ULTRAKILL("eventSnd.kills.UltraKill"),
	SPREE_UNSTOPPABLE("eventSnd.kills.Unstoppable"),
	SPREE_WICKEDSICK("eventSnd.kills.WhickedSick"),
	
	FLAG_BLUE_DROPPED("eventSnd.flags.Blue_Flag_Dropped"),
	FLAG_BLUE_RETURNED("eventSnd.flags.Blue_Flag_Returned"),
	FLAG_BLUE_TAKEN("eventSnd.flags.Blue_Flag_Taken"),
	FLAG_RED_DROPPED("eventSnd.flags.Red_Flag_Dropped"),
	FLAG_RED_RETURNED("eventSnd.flags.Red_Flag_Returned"),
	FLAG_RED_TAKEN("eventSnd.flags.Red_Flag_Taken"),
	
	DM_LAST_MAN_STANDING("eventSnd.dm.last_man_standing"),
	DM_YOUWON("eventSnd.dm.You_Have_Won_the_Match"),
	
	COUNTDOWN_01("eventSnd.countdown.01"),
	COUNTDOWN_02("eventSnd.countdown.02"),
	COUNTDOWN_03("eventSnd.countdown.03"),
	COUNTDOWN_04("eventSnd.countdown.04"),
	COUNTDOWN_05("eventSnd.countdown.05"),
	COUNTDOWN_06("eventSnd.countdown.06"),
	COUNTDOWN_07("eventSnd.countdown.07"),
	COUNTDOWN_08("eventSnd.countdown.08"),
	COUNTDOWN_09("eventSnd.countdown.09"),
	COUNTDOWN_10("eventSnd.countdown.10"),
	COUNTDOWN_20("eventSnd.countdown.20"),
	COUNTDOWN_30_SECS_REMAINING("eventSnd.countdown.30_seconds_remain"),
	COUNTDOWN_FIVE_MINUTE_WARNING("eventSnd.countdown.five_minute_warning"),
	COUNTDOWN_ONE_MINUTE_REMAINING("eventSnd.countdown.one_minute_remains"),
	COUNTDOWN_THREE_MINUTE_REMAINING("eventSnd.countdown.three_minutes_remain"),
	COUNTDOWN_TWO_MINUTE_REMAINING("eventSnd.countdown.two_minutes_remain"),
	
	NEXT_WAVE_IN("eventSnd.n.next_wave_in");
	
	String fileName;
	SoundsBase(String fileName)
	{
		this.fileName = fileName;
	}
	
	public String getFile()
	{
		return fileName;
	}
	
	public void play(PlayerEventInfo player)
	{
		player.playSound(getFile());
	}
	
	public void play(Collection<PlayerEventInfo> players)
	{
		for(PlayerEventInfo player : players)
		{
			try
			{
				player.playSound(getFile());
			}
			catch (Exception e)
			{
				continue;
			}
		}
	}
}
