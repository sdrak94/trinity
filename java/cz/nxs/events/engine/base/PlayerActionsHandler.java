package cz.nxs.events.engine.base;

import cz.nxs.interf.PlayerEventInfo;

/**
 * @author hNoke
 * not used at the moment
 */
public class PlayerActionsHandler
{
	@SuppressWarnings("unused")
	private final PlayerEventInfo player;
	
	public PlayerActionsHandler(PlayerEventInfo player)
	{
		this.player = player;
	}
	
	// Actions
	/*public void notifyKill(L2Character target)
	{
		if(player.getActiveEvent() != null && !player.isSpectator())
			player.getActiveEvent().onKill(player, new CharacterData(target));
	}
	
	public void notifyDie(L2Character killer)
	{
		if(player.getActiveEvent() != null && !player.isSpectator())
			player.getActiveEvent().onDie(player, new CharacterData(killer));
	}
	
	// actions to core
	
	public void notifyDisconnect()
	{
		if(player.getActiveEvent() != null && !player.isSpectator())
			player.getActiveEvent().onDisconnect(player);
		
		if(player.getRegisteredMainEvent() != null)
		{
			EventManager.getInstance().getMainEventManager().unregisterPlayer(player, true);
		}
		else if(player.getRegisteredMiniEvent() != null)
		{
			// already handled
		}
		
		PlayerBase.getInstance().eventEnd(player);
	}
	
	public boolean canAttack(L2Character target)
	{
		if(player.getActiveEvent() != null && !player.isSpectator())
			return player.getActiveEvent().canAttack(player, new CharacterData(target));
		
		return true;
	}
	
	public boolean canSupport(L2Character target)
	{
		if(player.getActiveEvent() != null && !player.isSpectator())
			return player.getActiveEvent().canSupport(player, new CharacterData(target));
		
		return true;
	}
	
	public void onAction()
	{
		if(player.getAfkChecker() != null)
			player.getAfkChecker().onAction();
	}
	
	public void onDamageGive(L2Character target, int ammount, boolean isDOT)
	{
		if(player.getActiveEvent() != null && !player.isSpectator())
			player.getActiveEvent().onDamageGive(player, new CharacterData(target), ammount, isDOT);
	}*/
	
	/** returning false will make the text not shown */
	/*public boolean notifySay(String text, int channel)
	{
		if(player.getActiveEvent() != null)
			return player.getActiveEvent().onSay(player, text, channel);
		return true;
	}*/
	
	/** false means that a html page has been already sent by Nexus engine */
	/*public boolean notifyNpcAction(L2Npc npc)
	{
		if(player.isSpectator())
			return true;
		
		if(EventManager.getInstance().showNpcHtml(player, new NpcData(npc)))
			return true;
		
		if(player.getActiveEvent() != null)
			return player.getActiveEvent().onNpcAction(player, new NpcData(npc));
		
		return false;
	}
	
	public boolean canUseItem(L2ItemInstance item)
	{
		if(player.isSpectator())
			return false;
		
		if(player.getActiveEvent() != null)
			return player.getActiveEvent().canUseItem(player, new ItemData(item));
		
		return true;
	}
	
	public void notifyItemUse(L2ItemInstance item)
	{
		if(player.getActiveEvent() != null)
			player.getActiveEvent().onItemUse(player, new ItemData(item));
	}
	
	public boolean canUseSkill(L2Skill skill)
	{
		if(player.isSpectator())
			return false;
		
		if(player.getActiveEvent() != null)
			return player.getActiveEvent().canUseSkill(player, new SkillData(skill));
		return true;
	}
	
	public void onUseSkill(L2Skill skill)
	{
		if(player.getActiveEvent() != null)
			player.getActiveEvent().onSkillUse(player, new SkillData(skill));
	}
	
	public boolean canShowToVillageWindow()
	{
		//if(_isInEvent) // this is already checked
			return false;
	}
	
	public boolean canDestroyItem(L2ItemInstance item)
	{
		if(player.getActiveEvent() != null)
			return player.getActiveEvent().canDestroyItem(player, new ItemData(item));
		return true;
	}*/
}
