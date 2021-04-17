/**
 * 
 */
package cz.nxs.events.engine.base;

/**
 * @author hNoke
 * an enum defining possible Positions in the end of event
 */
public enum RewardPosition
{
	None(null, ""),
	Winner(PositionType.General, "The best player/team."),
	Looser(PositionType.General, "All non-winners."),
	Tie(PositionType.General, ""),
	Tie_TimeLimit(PositionType.General, ""),
	
	Numbered(PositionType.Numbered, ""),
	Range(PositionType.Range, ""),
	
	KillingSpree(PositionType.Numbered, "Rewards players who do X kills in a row."),
	
	// event specific rewards
	OnKill(PositionType.EventSpecific, "Reward for killing another player."),
	FirstRegistered(PositionType.EventSpecific, "Reward for first X (configurable) players in the event."),
	FirstBlood(PositionType.EventSpecific, "Reward for the player who makes first kill in event."),
	FlagScore(PositionType.EventSpecific, "Reward for scoring with the flag."),
	FlagReturn(PositionType.EventSpecific, "Reward for player who returns his team's flag back."),
	ChestReward(PositionType.EventSpecific, "Reward when a regular chest is killed."),
	ChestRewardLucky(PositionType.EventSpecific, "Reward when a lucky chest is killed."),
	ChestRewardAncient(PositionType.EventSpecific, "Reward when a ancient chest is killed.");
	
	public PositionType posType;
	public String description;
	RewardPosition(PositionType posType, String description)
	{
		this.posType = posType;
		this.description = description;
	}
	
	public enum PositionType
	{
		General,
		Numbered,
		Range,
		EventSpecific
	}
	
	public static RewardPosition getPosition(String name)
	{
		for(RewardPosition p : RewardPosition.values())
		{
			if(p.toString().equalsIgnoreCase(name))
				return p;
		}
		return null;
	}
	
	public void setDescription(String d)
	{
		description = d;
	}
}
