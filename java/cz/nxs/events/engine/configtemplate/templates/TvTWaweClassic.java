package cz.nxs.events.engine.configtemplate.templates;

import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.configtemplate.ConfigTemplate;

/**
 * @author hNoke
 *
 */
public class TvTWaweClassic extends ConfigTemplate
{
	private SetConfig[] configs = new SetConfig[]
			{
				//TvT event configs
				new SetConfig("killsForReward", "1"), // at least one kill required for a reward
				new SetConfig("resDelay", "25"), // 25 seconds interval in wawe spawn
				new SetConfig("waweRespawn", "true"), // wawe spawn on
				new SetConfig("createParties", "true"), // we wanna create parties
				new SetConfig("maxPartySize", "10"), // max party size is 10
				new SetConfig("teamsCount", "2"), // 2 teams; feel free to set more if you have a big server
				
				// AbstractMainEvent configs
				new SetConfig("allowScreenScoreBar", "true"),
				new SetConfig("divideToTeamsMethod", "LevelOnly"), // sort players by their level
				new SetConfig("balanceHealersInTeams", "true"), // balancing healers is cool
				new SetConfig("minLvl", "20"), // min level to register to the event; make sure you have more made InstanceTypes
				new SetConfig("maxLvl", "85"), // max level to register
				new SetConfig("minPlayers", "4"), // at least 4 players to make this TvT
				new SetConfig("maxPlayers", "500"), // max 500 players, feel free to add more if you want
				new SetConfig("playersInInstance", "0"), // not used atm
				new SetConfig("allowPotions", "false"), // no potions
				new SetConfig("removeBuffsOnStart", "true"), // removal of buffs on start of the event
				new SetConfig("removeBuffsOnRespawn", "false"), // no removing buffs on respawn
				new SetConfig("notAllowedSkills", "0"), // no skills disabled; some of them such as Hide should be, tho, so this will be finished later TODO
			};
	

	@Override
	public String getName()
	{
		return "Team vs Team classic wawe";
	}
	
	@Override
	public EventType getEventType() 
	{
		return EventType.TvT;
	}

	@Override
	public String getDescription() 
	{
		return "Classic settings for a regular TvT event, with wawe-style spawn. " +
				"Don't forget to setup apropriate InstanceTypes for your server, to make sure all players can play in a balanced event.";
	}

	@Override
	public SetConfig[] getConfigs()
	{
		return configs;
	}
}
