package cz.nxs.events;

import cz.nxs.events.engine.EventManager.DisconnectedPlayerData;
import cz.nxs.events.engine.base.EventPlayerData;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.ItemData;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.SkillData;

/**
 * @author hNoke
 * a runnable event match (MiniEventGame, AbstractMainEvent)
 */
public interface EventGame
{
	public EventPlayerData createPlayerData(PlayerEventInfo player);
	public EventPlayerData getPlayerData(PlayerEventInfo player);
	
	public void clearEvent();
	
	public boolean canAttack(PlayerEventInfo player, CharacterData target);
	public boolean onAttack(CharacterData cha, CharacterData target);
	public boolean canSupport(PlayerEventInfo player, CharacterData target);
	public void onKill(PlayerEventInfo player, CharacterData target);
	public void onDie(PlayerEventInfo player, CharacterData killer);
	public void onDamageGive(CharacterData cha, CharacterData target, int damage, boolean isDOT);
	public void onDisconnect(PlayerEventInfo player);
	public boolean addDisconnectedPlayer(PlayerEventInfo player, DisconnectedPlayerData data);
	public boolean onSay(PlayerEventInfo player, String text, int channel);
	public boolean onNpcAction(PlayerEventInfo player, NpcData npc);
	
	public boolean canUseItem(PlayerEventInfo player, ItemData item);
	public void onItemUse(PlayerEventInfo player, ItemData item);
	
	public boolean canUseSkill(PlayerEventInfo player, SkillData skill);
	public void onSkillUse(PlayerEventInfo player, SkillData skill);
	
	public boolean canDestroyItem(PlayerEventInfo player, ItemData item);
	public boolean canInviteToParty(PlayerEventInfo player, PlayerEventInfo target);
	public boolean canTransform(PlayerEventInfo player);
	public boolean canBeDisarmed(PlayerEventInfo player);
	public int allowTransformationSkill(PlayerEventInfo playerEventInfo, SkillData skillData);
	public boolean canSaveShortcuts(PlayerEventInfo player);
	
	public int isSkillOffensive(SkillData skill);
	public boolean isSkillNeutral(SkillData skill);
	
	public void playerWentAfk(PlayerEventInfo player, boolean warningOnly, int afkTime);
	public void playerReturnedFromAfk(PlayerEventInfo player);
}
