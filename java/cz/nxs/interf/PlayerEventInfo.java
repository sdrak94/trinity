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
package cz.nxs.interf;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import cz.nxs.events.EventGame;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.base.EventPlayerData;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.base.Loc;
import cz.nxs.events.engine.base.PvPEventPlayerData;
import cz.nxs.events.engine.html.PartyMatcher;
import cz.nxs.events.engine.main.events.AbstractMainEvent;
import cz.nxs.events.engine.mini.MiniEventGame;
import cz.nxs.events.engine.mini.MiniEventManager;
import cz.nxs.events.engine.stats.EventStatsManager;
import cz.nxs.events.engine.team.EventTeam;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.ItemData;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.PartyData;
import cz.nxs.interf.delegate.ShortCutData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import cz.nxs.l2j.IPlayerEventInfo;
import cz.nxs.l2j.IValues;
import javolution.util.FastList;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassType;
import net.sf.l2j.gameserver.model.base.PlayerClass;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutRegister;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author hNoke
 *
 */
@SuppressWarnings("unused")
public class PlayerEventInfo implements IPlayerEventInfo
{
	public static final boolean AFK_CHECK_ENABLED = EventConfig.getInstance().getGlobalConfigBoolean("afkChecksEnabled");
	public static final int AFK_WARNING_DELAY = EventConfig.getInstance().getGlobalConfigInt("afkWarningDelay");
	public static final int AFK_KICK_DELAY = EventConfig.getInstance().getGlobalConfigInt("afkKickDelay");

	// Main variables
	private L2PcInstance _owner;
	private int _playersId;
	private boolean _isInEvent;
	private boolean _isRegistered; 
	private boolean _isRegisteredPvpZone; 
	private boolean _isInFFAEvent;
	private boolean _isSpectator;
	private boolean _canBuff;
	private boolean _canParty = true;
	
	private boolean _antifeedProtection;
	//
	private boolean _titleUpdate;
	private boolean _disableAfkCheck;
	
	// Original data, which will be restored when the event ends
	private int _origNameColor;
	private Location _origLoc;
	private String _origTitle;	
	
	// Event data
	private EventPlayerData _eventData;
	
	private int _status;
	
	// Active event and teams
	private EventGame _activeEvent;
	private EventTeam _eventTeam;
	
	private MiniEventManager _registeredMiniEvent;
	private EventType _registeredMainEvent;
	
	private AfkChecker _afkChecker;
	private Radar _radar;
	
	private String _tempName;
	private int _tempLevel;
	
	public PlayerEventInfo(L2PcInstance owner)
	{
		_owner = owner;
		_playersId = owner == null ? -1 : owner.getObjectId();
		
		_isRegistered = false;
		_isInEvent = false;
		_isInFFAEvent = false;
		
		_status = 0;
		
		_disableAfkCheck = false;
		_titleUpdate = true;
		
		_afkChecker = null;
	}
	
	public PlayerEventInfo(String name, int level)
	{
		_tempName = name;
		_tempLevel = level;
	}
	
	public PlayerEventInfo(String name, int level, cz.nxs.l2j.ClassType t)
	{
		_tempName = name;
		_tempLevel = level;
	}
	
	// =========================================================
	// ======== GENERAL EVENT METHODS ==========================
	// =========================================================
	
	@Override
	public void initOrigInfo()
	{
		_origNameColor = _owner.getAppearance().getNameColor();
		_origTitle = _owner.getTitle();
		_origLoc = new Location(_owner.getX(), _owner.getY(), _owner.getZ(), _owner.getHeading());
	}
	
	@Override
	public void restoreData()
	{
		_owner.getAppearance().setNameColor(_origNameColor);
		_owner.setTitle(_origTitle);
		_owner.getAppearance().setVisibleTitle(_origTitle);
		_owner.broadcastTitleInfo();
		_owner.broadcastUserInfo();
		
		clean();
	}
	
	@Override
	public void onEventStart(EventGame event)
	{
		initOrigInfo();
		
		_isInEvent = true;
		_activeEvent = event;
		
		_eventData = event.createPlayerData(this);
		
		if(AFK_CHECK_ENABLED)
			_afkChecker = new AfkChecker(this);
	}
	
	@Override
	public void clean()
	{
		if(_afkChecker != null)
			_afkChecker.stop();
		
		if(_radar != null)
			_radar.disable();
		
		_isRegistered = false;
		_isRegisteredPvpZone = false;
		_isInEvent = false;
		_isInFFAEvent = false;
		
		_registeredMiniEvent = null;
		_registeredMainEvent = null;
		
		_activeEvent = null;
		_eventTeam = null;
		
		_canParty = true;
		
		_eventData = null;
		
		_status = 0;
	}
	
	private class Teleport implements Runnable
	{
		final L2PcInstance owner;
		Loc loc;
		//int delay;
		boolean randomOffset;
		int instanceId;
		
		Teleport(L2PcInstance owner, Loc loc, int delay, boolean randomOffset, int instanceId)
		{
			this.owner = owner;
			this.loc = loc;
			//this.delay = delay;
			this.randomOffset = randomOffset;
			this.instanceId = instanceId;
			
			if(delay == 0)
				CallBack.getInstance().getOut().executeTask(this);
			else
				CallBack.getInstance().getOut().scheduleGeneral(this, delay);
		}

		@Override
		public void run()
		{
			L2PcInstance player = owner;
			
			if (player == null)
				return;
			
			L2Summon summon = player.getPet();
			
			player.abortCast();
			
			if (summon != null)
				summon.unSummon(player);

			if (player.isInDuel())
				player.setDuelState(Duel.DUELSTATE_INTERRUPTED);
			
			player.doRevive();
			
			for(L2Effect e : player.getAllEffects())
			{
				if(e != null && e.getSkill() != null && e.getSkill().isDebuff())
					e.exit();
			}
			
			if(player.isSitting())
				player.standUp();
			
			player.teleToLocation( loc.getX(), loc.getY(), loc.getZ(), randomOffset );
			
			player.setTarget(null);
			
			if(instanceId != -1)
				player.setInstanceId(instanceId);

			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			
			player.broadcastStatusUpdate();
			player.broadcastUserInfo();
		}
	}
	
	@Override
	public void teleport(Loc loc, int delay, boolean randomOffset, int instanceId)
	{
		new Teleport(_owner, loc, delay, randomOffset, instanceId);
	}
	
	@Override
	public void teleToLocation(Loc loc, boolean randomOffset)
	{
		_owner.teleToLocation(loc.getX(), loc.getY(), loc.getZ(), randomOffset);
	}
	
	@Override
	public void teleToLocation(int x, int y, int z, boolean randomOffset)
	{
		_owner.teleToLocation(x, y, z, randomOffset);
	}
	
	@Override
	public void teleToLocation(int x, int y, int z, int heading, boolean randomOffset)
	{
		_owner.teleToLocation(x, y, z, heading, randomOffset);
	}
	
	@Override
	public void setXYZInvisible(int x, int y, int z)
	{
		_owner.setXYZInvisible(x, y, z);
	}
	
	@Override
	public void setFame(int count)
	{
		_owner.setFame(count);
	}
	
	@Override
	public int getFame()
	{
		return _owner.getFame();
	}
	
	// =========================================================
	// ======== L2PcInstance ACTIONS ===========================
	// =========================================================
	
	protected void notifyKill(L2Character target)
	{
		if(_activeEvent != null && !_isSpectator)
			_activeEvent.onKill(this, new CharacterData(target));
	}
	
	protected void notifyDie(L2Character killer)
	{
		if(_activeEvent != null && !_isSpectator)
			_activeEvent.onDie(this, new CharacterData(killer));
	}
	
	protected void notifyDisconnect()
	{
		PartyMatcher.onDisconnect(this);
		
		if(_activeEvent != null && !_isSpectator)
			_activeEvent.onDisconnect(this);
		
		if(_registeredMainEvent != null)
		{
			EventManager.getInstance().getMainEventManager().unregisterPlayer(this, true);
		}
		else if(_registeredMiniEvent != null)
		{
			// already handled
		}
		
		EventStatsManager.getInstance().onDisconnect(this);
		PlayerBase.getInstance().eventEnd(this);
	}
	
	protected boolean canAttack(L2Character target)
	{
		if(_activeEvent != null && !_isSpectator)
			return _activeEvent.canAttack(this, new CharacterData(target));
		
		return true;
	}
	
	protected boolean canSupport(L2Character target)
	{
		if(_activeEvent != null && !_isSpectator)
			return _activeEvent.canSupport(this, new CharacterData(target));
		
		return true;
	}
	
	public void onAction()
	{
		if(_afkChecker != null)
			_afkChecker.onAction();
	}
	
	protected void onDamageGive(L2Character target, int ammount, boolean isDOT)
	{
		if(_activeEvent != null && !_isSpectator)
			_activeEvent.onDamageGive(getCharacterData(), new CharacterData(target), ammount, isDOT);
	}
	
	/** returning false will make the text not shown */
	protected boolean notifySay(String text, int channel)
	{
		if(_activeEvent != null)
			return _activeEvent.onSay(this, text, channel);
		return true;
	}
	
	/** false means that a html page has been already sent by Nexus engine */
	protected boolean notifyNpcAction(L2Npc npc)
	{
		if(_isSpectator)
			return true;
		
		if(EventManager.getInstance().showNpcHtml(this, new NpcData(npc)))
			return true;
		
		if(_activeEvent != null)
			return _activeEvent.onNpcAction(this, new NpcData(npc));
		
		return false;
	}
	
	protected boolean canUseItem(L2ItemInstance item)
	{
		if(_isSpectator)
			return false;
		
		if(_activeEvent != null)
			return _activeEvent.canUseItem(this, new ItemData(item));
		
		return true;
	}
	
	protected void notifyItemUse(L2ItemInstance item)
	{
		if(_activeEvent != null)
			_activeEvent.onItemUse(this, new ItemData(item));
	}
	
	protected boolean canUseSkill(L2Skill skill)
	{
		if(_isSpectator)
			return false;
		
		if(_activeEvent != null)
			return _activeEvent.canUseSkill(this, new SkillData(skill));
		return true;
	}
	
	protected void onUseSkill(L2Skill skill)
	{
		if(_activeEvent != null)
			_activeEvent.onSkillUse(this, new SkillData(skill));
	}
	
	protected boolean canShowToVillageWindow()
	{
		//if(_isInEvent) // this is already checked
			return false;
	}
	
	protected boolean canDestroyItem(L2ItemInstance item)
	{
		if(_activeEvent != null)
			return _activeEvent.canDestroyItem(this, new ItemData(item));
		return true;
	}
	
	protected boolean canInviteToParty(PlayerEventInfo player, PlayerEventInfo target)
	{
		if(_activeEvent != null)
			return _activeEvent.canInviteToParty(player, target);
		return true;
	}
	
	protected boolean canTransform(PlayerEventInfo player)
	{
		if(_activeEvent != null)
			return _activeEvent.canTransform(player);
		return true;
	}
	
	protected boolean canBeDisarmed(PlayerEventInfo player)
	{
		if(_activeEvent != null)
			return _activeEvent.canBeDisarmed(player);
		return true;
	}
	
	protected int allowTransformationSkill(L2Skill s)
	{
		if(_activeEvent != null)
			return _activeEvent.allowTransformationSkill(this, new SkillData(s));
		return 0;
	}
	
	protected boolean canSaveShortcuts()
	{
		if(_activeEvent != null)
			return _activeEvent.canSaveShortcuts(this);
		return true;
	}
	
	// =========================================================
	// ======== L2PcInstance GENERAL METHODS ===================
	// =========================================================
	
	@Override
	public void setInstanceId(int id)
	{
		_owner.setInstanceId(id);
	}
	
	@Override
	public void sendPacket(String html)
	{
		sendHtmlText(html);
	}
	
	@Override
	public void screenMessage(String message, String name, boolean special)
	{
		L2GameServerPacket packet;
		
		if(special)
			packet = new ExShowScreenMessage(message, 5000);
		else
			packet = new CreatureSay(0, Say2.PARTYROOM_COMMANDER, name, message);
		
		if(_owner != null)
			_owner.sendPacket(packet);
	}
	
	@Override
	public void creatureSay(String message, String announcer, int channel)
	{
		if(_owner != null)
			_owner.sendPacket(new CreatureSay(0, channel, announcer, message));
	}
	
	@Override
	public void sendMessage(String message)
	{
		if(_owner != null)
			_owner.sendMessage(message);
	}
	
	@Override
	public void sendEventScoreBar(String text)
	{
		if(_owner != null)
			_owner.sendPacket(new ExShowScreenMessage(1, -1, 3, 0, 1, 0, 0, true, 2000, 0, text));
	}
	
	@Override
	public void broadcastUserInfo()
	{
		if(_owner != null)
			_owner.broadcastUserInfo();
	}
	
	@Override
	public void broadcastTitleInfo()
	{
		if(_owner != null)
			_owner.broadcastTitleInfo();
	}
	
	@Override
	public void sendSkillList()
	{
		_owner.sendSkillList();
	}
	
	@Override
	public void transform(int transformId)
	{
		if(_owner != null)
			TransformationManager.getInstance().transformPlayer(transformId, _owner);
	}
	
	@Override
	public boolean isTransformed()
	{
		if(_owner != null && _owner.isTransformed())
			return true;
		return false;
	}
	
	@Override
	public void untransform(boolean removeEffects)
	{
		if(_owner != null && _owner.isTransformed())
			_owner.untransform();
	}
	
	@Override
	public ItemData addItem(int id, int ammount, boolean msg)
	{
		if(_owner != null)
			return new ItemData(_owner.addItem("Event Reward", id, ammount, null, msg));
		return null;
	}
	
	@Override
	public void addExpAndSp(long exp, int sp)
	{
		_owner.addExpAndSp(exp, sp);
	}
	
	@Override
	public void doDie()
	{
		_owner.doDie(_owner);
	}
	
	@Override
	public void doDie(CharacterData killer)
	{
		_owner.doDie(killer.getOwner());
	}
	
	@Override
	public ItemData[] getItems()
	{
		List<ItemData> items = new FastList<ItemData>();
		
		for(L2ItemInstance item : _owner.getInventory().getItems())
		{
			items.add(new ItemData(item));
		}
		
		return items.toArray(new ItemData[items.size()]);
	}
	
	@Override
	public void getPetSkillEffects(int skillId, int level)
	{
		if(_owner.getPet() != null)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);
			if (skill != null)
			{
				skill.getEffects(_owner.getPet(), _owner.getPet());
			}
		}
	}
	
	@Override
	public void getSkillEffects(int skillId, int level)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);
		if (skill != null)
			skill.getEffects(_owner, _owner);
	}
	
	@Override
	public void addSkill(SkillData skill, boolean store)
	{
		getOwner().addSkill(SkillTable.getInstance().getInfo(skill.getId(), skill.getLevel()), store);
	}
	
	@Override
	public void removeSkill(int id)
	{
		getOwner().removeSkill(id);
	}
	
	@Override
	public void removeCubics()
	{
		if (!_owner.getCubics().isEmpty())
		{
			for (L2CubicInstance cubic : _owner.getCubics().values())
			{
				cubic.stopAction(); // this propably won't be used as well
			}
			
			_owner.getCubics().clear();
		}
	}
	
	@Override
	public void removeSummon()
	{
		if(_owner.getPet() != null)
			_owner.getPet().unSummon(_owner);
	}
	
	@Override 
	public boolean hasPet()
	{
		return _owner.getPet() != null;
	}
	
	@Override
	public void removeBuffsFromPet()
	{
		if(_owner != null && _owner.getPet() != null)
			_owner.getPet().stopAllEffects();
	}
	
	@Override
	public void removeBuffs()
	{
		if(_owner != null)
			_owner.stopAllEffects();
	}
	
	@Override
	public int getBuffsCount()
	{
		return _owner.getBuffCount();
	}
	
	@Override
	public int getDancesCount()
	{
		return 0;
	}
	
	@Override
	public int getPetBuffCount()
	{
		if(_owner.getPet() != null)
			return _owner.getPet().getBuffCount();
		else
			return 0;
	}
	
	@Override
	public int getPetDanceCount()
	{
		return 0;
	}
	
	@Override
	public int getMaxBuffCount()
	{
		return _owner.getMaxBuffCount();
	}
	
	@Override
	public int getMaxDanceCount()
	{
		return 0;
	}
	
	@Override
	public void removeBuff(int id)
	{
		if(_owner != null)
			_owner.stopSkillEffects(id);
	}
	
	@Override
	public void abortCasting()
	{
		if(_owner.isCastingNow())
			_owner.abortCast();
		
		if(_owner.isAttackingNow())
			_owner.abortAttack();
	}
	
	@Override
	public void playSound(String file)
	{
		_owner.sendPacket(new PlaySound(file));
	}
	
	@Override
	public void setVisible()
	{
		_owner.isVisible();
	}
	
	@Override
	public void rebuffPlayer()
	{
		//TODO
		/*if(getActiveGame() != null)
		{
			for(AbstractFeature feature : getActiveGame().getEvent().getMode().getFeatures())
			{
				if(feature.getType() == FeatureType.Buffer)
				{
					if(Manager.getInstance().getBoolean("eventBufferEnabled") && ((BufferFeature) feature).canRebuff())
						Buffer.getInstance().buffPlayer(_owner);
					
					((BufferFeature) feature).buffPlayer(_owner);
				}
			}
		}*/
	}
	
	@Override
	public void enableAllSkills()
	{
		for(L2Skill skill : _owner.getAllSkills())
		{
			if (skill.getReuseDelay() <= 900000)
				_owner.enableSkill(skill.getId());
		}
		_owner.sendPacket(new SkillCoolTime(_owner));
	}
	
	@Override
	public void sendSetupGauge(int time)
	{
		SetupGauge sg = new SetupGauge(0, time);
		_owner.sendPacket(sg);
	}
	
	@Override
	public void root()
	{
		_owner.setIsImmobilized(true);
		_owner.startAbnormalEffect(AbnormalEffect.STEALTH);
	}
	
	@Override
	public void unroot()
	{
		if(_owner.isImmobilized())
			_owner.setIsImmobilized(false);
		_owner.stopAbnormalEffect(AbnormalEffect.STEALTH);
	}
	
	@Override
	public void paralizeEffect(boolean b)
	{
		if(b) 
			getOwner().startAbnormalEffect(AbnormalEffect.HOLD_1);
		else 
			getOwner().stopAbnormalEffect(AbnormalEffect.HOLD_1);
	}
	
	@Override
	public void setIsParalyzed(boolean b)
	{
		_owner.setIsParalyzed(b);
	}
	
	@Override
	public void setIsInvul(boolean b)
	{
		_owner.setIsInvul(b);
	}
	
	@Override
	public void setCanInviteToParty(boolean b)
	{
		_canParty = b;
	}
	
	@Override
	public boolean canInviteToParty()
	{
		return _canParty;
	}
	
	@SuppressWarnings("static-access")
	@Override
	public void showEventEscapeEffect()
	{
		_owner.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		_owner.setTarget(_owner);
		_owner.disableAllSkills();
		
		MagicSkillUse msk = new MagicSkillUse(_owner, 1050, 1, 10000, 0);
		Broadcast.toSelfAndKnownPlayersInRadius(_owner, msk, 810000/*900*/);
		SetupGauge sg = new SetupGauge(0, 10000);
		_owner.sendPacket(sg);
		
		_owner.forceIsCasting(GameTimeController.getInstance().getGameTicks() + 10000 / GameTimeController.MILLIS_IN_TICK);
	}
	
	@Override
	public void startAntifeedProtection(boolean broadcast)
	{
		_owner.startAntifeedProtection(true, broadcast);
		_antifeedProtection = true;
		
		if(broadcast)
			broadcastUserInfo();
	}
	
	@Override
	public void stopAntifeedProtection(boolean broadcast)
	{
		_owner.startAntifeedProtection(false, broadcast);
		_antifeedProtection = false;
		
		if(broadcast)
			broadcastUserInfo();
	}
	
	@Override
	public boolean hasAntifeedProtection()
	{
		return _antifeedProtection;
	}
	
	/** 
	 * @param owner - null to put there this PlayerEventInfo
	 * @param target - null to put there this PlayerEventInfo
	 * @param skillId
	 * @param level
	 */
	@Override
	public void broadcastSkillUse(CharacterData owner, CharacterData target, int skillId, int level)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);
		
		if (skill != null)
			getOwner().broadcastPacket(new MagicSkillUse(owner == null ? getOwner() : owner.getOwner(), target == null ? getOwner() : target.getOwner(), skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
	}
	
	@Override
	public void broadcastSkillLaunched(CharacterData owner, CharacterData target, int skillId, int level)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);
		
		if (skill != null)
			getOwner().broadcastPacket(new MagicSkillLaunched(owner == null ? getOwner() : owner.getOwner(), skill.getId(), skill.getLevel(), new L2Object[]{target.getOwner()}));
	}
	
	@Override
	public void enterObserverMode(int x, int y, int z)
	{
		_owner.enterOlympiadObserverMode(new Location(x, y, z), 0);
	}
	
	@Override
	public void removeObserveMode()
	{
		setIsSpectator(false);
		setActiveGame(null);
		
		//
		_owner.leaveOlympiadObserverMode();
		_owner.setInstanceId(0);
		_owner.teleToLocation(getOrigLoc().getX(), getOrigLoc().getY(), getOrigLoc().getZ(), true);
	}
	
	@Override
	public void sendStaticPacket()
	{
		_owner.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void sendHtmlText(String text)
	{
		NpcHtmlMessage msg = new NpcHtmlMessage(0);
		msg.setHtml(text);
		
		_owner.sendPacket(msg);
	}
	
	@Override
	public void sendHtmlPage(String path)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(null, path);

		_owner.sendPacket(html);
		sendStaticPacket();
	}
	
	@Override
	public void startAbnormalEffect(int mask)
	{
		IValues val = CallBack.getInstance().getValues();
		if(mask == val.ABNORMAL_S_INVINCIBLE())
		{
			_owner.startSpecialEffect(mask);
		}
		else
			_owner.startAbnormalEffect(mask);
	}
	
	@Override
	public void stopAbnormalEffect(int mask)
	{
		IValues val = CallBack.getInstance().getValues();
		if(mask == val.ABNORMAL_S_INVINCIBLE())
		{
			_owner.stopSpecialEffect(mask);
		}
		else
			_owner.stopAbnormalEffect(mask);
	}
	
	// Nexus ShortCut engine
	
	private List<ShortCutData> _customShortcuts = new FastList<ShortCutData>();
	
	/**
	 * restore back removed shortcuts by calling restoreOriginalShortcuts()
	 */
	@Override
	public void removeOriginalShortcuts()
	{
		if(_owner == null) return;
		
		_owner.removeAllShortcuts();
		_owner.sendPacket(new ShortCutInit(_owner)); 
	}
	
	@Override
	public void restoreOriginalShortcuts()
	{
		if(_owner == null) return;
		
		_owner.restoreShortCuts();
		_owner.sendPacket(new ShortCutInit(_owner)); 
	}
	
	@Override
	public void removeCustomShortcuts()
	{
		if(_owner == null) return;
		
		for(ShortCutData sh : _customShortcuts)
		{
			_owner.deleteShortCut(sh.getSlot(), sh.getPage(), false);
		}
		
		_customShortcuts.clear();
	}
	
	/**
	 * @param shortcut - the shortcut you wanna register
	 * @param eventShortcut - if the shortcut exists only during the event; can be deleted by calling removeCustomShortcuts(); won't be saved to db
	 */
	@Override
	public void registerShortcut(ShortCutData shortcut, boolean eventShortcut)
	{
		if(eventShortcut)
			_customShortcuts.add(shortcut);
		
		if(_owner != null)
		{
			L2ShortCut sh = new L2ShortCut(shortcut.getSlot(), shortcut.getPage(), shortcut.getType(), shortcut.getId(), shortcut.getLevel(), shortcut.getCharacterType());
			
			_owner.sendPacket(new ShortCutRegister(sh));
			_owner.registerShortCut(sh, !eventShortcut);
		}
	}
	
	/**
	 * @param shortcut - the shortcut you wanna unregister
	 */
	@Override
	public void removeShortCut(ShortCutData shortcut, boolean eventShortcut)
	{
		if(eventShortcut && _customShortcuts.contains(shortcut))
			_customShortcuts.remove(shortcut);
		
		if(_owner != null)
			_owner.deleteShortCut(shortcut.getSlot(), shortcut.getPage(), !eventShortcut);
	}
	
	@Override
	public ShortCutData createItemShortcut(int slotId, int pageId, ItemData item)
	{
		return new ShortCutData(slotId, pageId, Values.getInstance().TYPE_ITEM(), item.getObjectId(), 0, 1);
	}
	
	@Override
	public ShortCutData createSkillShortcut(int slotId, int pageId, SkillData skill)
	{
		return new ShortCutData(slotId, pageId, Values.getInstance().TYPE_SKILL(), skill.getId(), skill.getLevel(), 1);
	}
	
	@Override
	public ShortCutData createActionShortcut(int slotId, int pageId, int actionId)
	{
		return new ShortCutData(slotId, pageId, Values.getInstance().TYPE_ACTION(), actionId, 0, 1);
	}
	
	// =========================================================
	// ======== L2PcInstance GET METHODS =======================
	// =========================================================
	
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	@Override
	public boolean isOnline()
	{
		return isOnline(false);
	}
	
	@Override
	public boolean isOnline(boolean strict)
	{
		if(strict)
			return _owner != null && _owner.isOnline() == 1;
		else
			return _owner != null;
	}
	
	@Override
	public boolean isDead()
	{
		return _owner.isDead();
	}
	
	@Override
	public boolean isVisible()
	{
		return _owner.isVisible();
	}
	
	@Override
	public void doRevive()
	{
		_owner.doRevive();
	}
	
	@Override
	public CharacterData getTarget()
	{
		if(_owner.getTarget() == null || !(_owner.getTarget() instanceof L2Character))
			return null;
		else
			return new CharacterData((L2Character) _owner.getTarget());
	}
	
	@Override
	public String getPlayersName()
	{
		if(_owner != null)
			return _owner.getName();
		else
			return _tempName;
	}
	
	@Override
	public int getLevel()
	{
		if(_owner != null)
			return _owner.getLevel();
		return _tempLevel;
	}
	
	@Override
	public int getPvpKills()
	{
		return _owner.getPvpKills();
	}
	
	@Override
	public int getPkKills()
	{
		return _owner.getPkKills();
	}
	
	@Override
	public int getMaxHp() { return _owner.getMaxHp(); }
	@Override
	public int getMaxCp() { return _owner.getMaxCp(); }
	@Override
	public int getMaxMp() { return _owner.getMaxMp(); }
	
	@Override
	public void setCurrentHp(int hp) { _owner.setCurrentHp(hp); }
	@Override
	public void setCurrentCp(int cp) { _owner.setCurrentCp(cp); }
	@Override
	public void setCurrentMp(int mp) { _owner.setCurrentMp(mp); }
	
	@Override
	public double getCurrentHp() { return _owner.getCurrentHp(); }
	@Override
	public double getCurrentCp() { return _owner.getCurrentCp(); }
	@Override
	public double getCurrentMp() { return _owner.getCurrentMp(); }
	
	@Override
	public void healPet()
	{
		if(_owner != null && _owner.getPet() != null)
		{
			_owner.getPet().setCurrentHp(_owner.getPet().getMaxHp());
			_owner.getPet().setCurrentMp(_owner.getPet().getMaxMp());
			//_owner.getPet().setCurrentCp(_owner.getPet().getMaxCp());
		}
	}
	
	@Override
	public void setTitle(String title, boolean updateVisible)
	{
		_owner.setTitle(title);
		if(updateVisible)
			_owner.getAppearance().setVisibleTitle(_owner.getTitle());
	}
	
	/** returns true if player is not mage class */
	@Override
	public boolean isMageClass()
	{
		return _owner.isMageClass();
	}
	
	@Override
	public int getClassIndex()
	{
		if(_owner != null)
			return _owner.getClassIndex();
		return 0;
	}
	
	@Override
	public int getActiveClass()
	{
		if(_owner != null)
			return _owner.getActiveClass();
		return 0;
	}
	
	@Override
	public String getClassName()
	{
		return _owner.getTemplate().className;
	}
	
	@Override
	public PartyData getParty()
	{
		if(_owner.getParty() == null)
			return null;
		return new PartyData(_owner.getParty());
	}
	
	@Override
	public boolean isFighter()
	{
		return PlayerClass.values()[_owner.getActiveClass()].isOfType(ClassType.MELEE);
	}
	
	/** returns true if player is of Priest class type (not nuker) */
	@Override
	public boolean isPriest()
	{
		if(PlayerClass.values()[_owner.getActiveClass()] == PlayerClass.Cardinal || PlayerClass.values()[_owner.getActiveClass()] == PlayerClass.Bishop)
			return true;
		
		return false;
	}
	
	/** returns true if player is of Mystic (nuke) class type, not healer of buffer */
	@Override
	public boolean isMystic()
	{
		return PlayerClass.values()[_owner.getActiveClass()].isOfType(ClassType.MAGE);
	}
	
	@Override
	public cz.nxs.l2j.ClassType getClassType()
	{
		if(isFighter()) return cz.nxs.l2j.ClassType.Fighter;
		else if(isMystic()) return cz.nxs.l2j.ClassType.Mystic;
		else /*if(isPriest())*/ return cz.nxs.l2j.ClassType.Priest;
	}
	
	@Override
	public int getX()
	{
		return _owner.getX();
	}
	
	@Override
	public int getY()
	{
		return _owner.getY();
	}
	
	@Override
	public int getZ()
	{
		return _owner.getZ();
	}
	
	@Override
	public int getHeading()
	{
		return _owner.getHeading();
	}
	
	@Override
	public int getInstanceId()
	{
		return _owner.getInstanceId();
	}
	
	@Override
	public int getClanId()
	{
		return _owner.getClanId();
	}
	
	@Override
	public boolean isGM()
	{
		return _owner.isGM();
	}
	
	@Override
	public String getIp()
	{
		return _owner.getClient().getConnection().getInetAddress().getHostAddress();
	}
	
	@Override
	public boolean isInJail() { return _owner.isInJail(); }

	@Override
	public boolean isInSiege() { return _owner.isInSiege(); }

	@Override
	public boolean isInDuel() { return _owner.isInDuel(); }

	@Override
	public boolean isInOlympiadMode() { return _owner.isInOlympiadMode(); }

	@Override
	public int getKarma() { return _owner.getKarma(); }

	@Override
	public boolean isCursedWeaponEquipped() { return _owner.isCursedWeaponEquipped(); }

	@Override
	public boolean isImmobilized() { return _owner.isImmobilized(); }

	@Override
	public boolean isParalyzed() { return _owner.isParalyzed(); }

	@Override
	public boolean isAfraid() { return _owner.isAfraid(); }

	@Override
	public boolean isOlympiadRegistered()
	{
		return Olympiad.getInstance().isRegistered(_owner);
	}
	
	@Override
	public void sitDown()
	{
		if(_owner == null)
			return;
		
		_owner.sitDown();
		_owner.eventSitForced = true;
	}
	
	@Override
	public void standUp()
	{
		if(_owner == null)
			return;
		
		_owner.eventSitForced = false;
		_owner.standUp();
	}
	
	@Override
	public List<SkillData> getSkills()
	{
		List<SkillData> list = new FastList<SkillData>();
		for(L2Skill skill : getOwner().getAllSkills())
		{
			list.add(new SkillData(skill));
		}
		return list;
	}
	
	@Override
	public List<Integer> getSkillIds()
	{
		List<Integer> list = new FastList<Integer>();
		for(L2Skill skill : getOwner().getAllSkills())
		{
			list.add(skill.getId());
		}
		return list;
	}
	
	@Override
	public double getPlanDistanceSq(int targetX, int targetY)
	{
		return _owner.getPlanDistanceSq(targetX, targetY);
	}
	
	@Override
	public double getDistanceSq(int targetX, int targetY, int targetZ)
	{
		return _owner.getDistanceSq(targetX, targetY, targetZ);
	}
	
	// =========================================================
	// ======== EVENT RELATED GET/SET METHODS ==================
	// =========================================================

	@Override
	public boolean isRegistered()
	{
		return _isRegistered;
	}
	
	public boolean isRegisteredToPvpZone()
	{
		return _isRegisteredPvpZone;
	}
	
	@Override
	public boolean isInEvent()
	{
		return _isInEvent;
	}
	
	@Override
	public EventPlayerData getEventData()
	{
		return _eventData;
	}

	@Override
	public void setNameColor(int color)
	{
		_owner.getAppearance().setNameColor(color);
		_owner.broadcastUserInfo();
	}
	
	@Override
	public void setCanBuff(boolean canBuff)
	{
		_canBuff = canBuff;
	}

	@Override
	public boolean canBuff()
	{
		return _canBuff;
	}
	
	@Override
	public int getPlayersId()
	{
		return _playersId;
	}
	
	@Override
	public int getKills()
	{
		return _eventData instanceof PvPEventPlayerData ? ((PvPEventPlayerData)_eventData).getKills() : 0;
	}
	
	@Override
	public int getDeaths()
	{
		return _eventData instanceof PvPEventPlayerData ? ((PvPEventPlayerData)_eventData).getDeaths() : 0;
	}
	
	@Override
	public int getScore()
	{
		return _eventData.getScore();
	}
	
	@Override
	public int getStatus()
	{
		return _status;
	}
	
	@Override
	public void raiseKills(int count)
	{
		if(_eventData instanceof PvPEventPlayerData)
			((PvPEventPlayerData)_eventData).raiseKills(count);
	}
	
	@Override
	public void raiseDeaths(int count)
	{
		if(_eventData instanceof PvPEventPlayerData)
			((PvPEventPlayerData)_eventData).raiseDeaths(count);
	}
	
	@Override
	public void raiseScore(int count)
	{
		_eventData.raiseScore(count);
	}
	
	@Override
	public void setScore(int count)
	{
		_eventData.setScore(count);
	}
	
	@Override
	public void setStatus(int count)
	{
		_status = count;
	}
	
	@Override
	public void setKills(int count)
	{
		if(_eventData instanceof PvPEventPlayerData)
			((PvPEventPlayerData)_eventData).setKills(count);
	}
	
	@Override
	public void setDeaths(int count)
	{
		if(_eventData instanceof PvPEventPlayerData)
			((PvPEventPlayerData)_eventData).setDeaths(count);
	}
	
	@Override
	public boolean isInFFAEvent()
	{
		return _isInFFAEvent;
	}
	
	@Override
	public void setIsRegisteredToMiniEvent(boolean b, MiniEventManager minievent)
	{
		_isRegistered = b;
		_registeredMiniEvent = minievent;
	}
	
	@Override
	public MiniEventManager getRegisteredMiniEvent()
	{
		return _registeredMiniEvent;
	}
	
	@Override
	public void setIsRegisteredToMainEvent(boolean b, EventType event)
	{
		_isRegistered = b;
		_registeredMainEvent = event;
	}
	
	public void setIsRegisteredToPvpZone(boolean b)
	{
		_isRegistered = b;
		_isRegisteredPvpZone = b;
	}
	
	@Override
	public EventType getRegisteredMainEvent()
	{
		return _registeredMainEvent;
	}
	
	@Override
	public MiniEventGame getActiveGame()
	{
		if(_activeEvent instanceof MiniEventGame)
			return (MiniEventGame) _activeEvent;
		else
			return null;
	}
	
	@Override
	public AbstractMainEvent getActiveEvent()
	{
		if(_activeEvent instanceof AbstractMainEvent)
			return (AbstractMainEvent) _activeEvent;
		else
			return null;
	}
	
	@Override
	public EventGame getEvent()
	{
		return _activeEvent;
	}
	
	@Override
	public void setActiveGame(MiniEventGame game)
	{
		_activeEvent = game;
	}
	
	@Override
	public void setEventTeam(EventTeam team)
	{
		_eventTeam = team;
	}
	
	@Override
	public EventTeam getEventTeam()
	{
		return _eventTeam;
	}
	
	@Override
	public int getTeamId()
	{
		if(_eventTeam != null)
			return _eventTeam.getTeamId();
		else
			return -1;
	}
	
	@Override
	public Loc getOrigLoc()
	{
		return new Loc(_origLoc.getX(), _origLoc.getY(), _origLoc.getZ());
	}

	@Override
	public void setIsSpectator(boolean _isSpectator)
	{
		this._isSpectator = _isSpectator;
	}

	@Override
	public boolean isSpectator()
	{
		return _isSpectator;
	}
	
	@Override
	public boolean isEventRooted()
	{
		return _disableAfkCheck;
	}
	
	@Override
	public boolean isTitleUpdated()
	{
		return _titleUpdate;
	}
	
	@Override
	public void setTitleUpdated(boolean b)
	{
		_titleUpdate = b;
	}
	
	@Override
	public ItemData getPaperdollItem(int slot)
	{
		return new ItemData(getOwner().getInventory().getPaperdollItem(slot));
	}
	
	@Override
	public void equipItem(ItemData item)
	{
		getOwner().getInventory().equipItemAndRecord(item.getOwner());
	}
	
	public int getGearScore()
	{
		return 0;
	}
	
	@Override
	public ItemData[] unEquipItemInBodySlotAndRecord(int slot)
	{
		ItemData[] items;
		
		L2ItemInstance[] is = getOwner().getInventory().unEquipItemInBodySlotAndRecord(slot);
		items = new ItemData[is.length];
		
		for(int i = 0; i < is.length; i ++)
		{
			items[i] = new ItemData(is[i]);
		}
		
		return items;
	}
	
	@Override
	public void destroyItemByItemId(int id, int count)
	{
		getOwner().getInventory().destroyItemByItemId("", id, count, null, null);
	}
	
	@Override
	public void inventoryUpdate(ItemData[] items)
	{
		InventoryUpdate iu = new InventoryUpdate();
		for (ItemData element : items)
			iu.addModifiedItem(element.getOwner());
		getOwner().sendPacket(iu);
		getOwner().sendPacket(new ItemList(getOwner(), false));
		getOwner().broadcastUserInfo();
	}
	
	@Override
	public Radar getRadar()
	{
		return _radar;
	}
	
	@Override
	public void createRadar()
	{
		_radar = new Radar(this);
	}
	
	@Override
	public void addRadarMarker(int x, int y, int z)
	{
		if(_owner != null)
			_owner.getRadar().addMarker(x, y, z);
	}

	@Override
	public void removeRadarMarker(int x, int y, int z)
	{
		if(_owner != null)
			_owner.getRadar().removeMarker(x, y, z);
	}
	
	@Override
	public void removeRadarAllMarkers()
	{
		if(_owner != null)
			_owner.getRadar().removeAllMarkers();
	}
	
	// =========================================================
	// ======== AFK Protection =================================
	// =========================================================
	
	@Override
	public void disableAfkCheck(boolean b)
	{
		_disableAfkCheck = b;
		
		if(!b && _afkChecker != null)
			_afkChecker.check();
	}

	@Override
	public int getTotalTimeAfk()
	{
		if(_afkChecker == null)
			return 0;
		
		return Math.max(0, _afkChecker.totalTimeAfk);
	}
	
	@Override
	public boolean isAfk()
	{
		if(_afkChecker != null)
			return _afkChecker.isAfk;
		return false;
	}
	
	@Override
	public AfkChecker getAfkChecker()
	{
		return _afkChecker;
	}
	
	@Override
	public CharacterData getCharacterData()
	{
		return new CharacterData(getOwner());
	}
	
	public class Radar
	{
		private final PlayerEventInfo player;
		private ScheduledFuture<?> refresh;
		
		private boolean enabled;
		private boolean repeat = false;
		private int newX, newY, newZ;
		private int currentX, currentY, currentZ;
		
		private boolean hasRadar;
		
		public Radar(PlayerEventInfo player)
		{
			this.player = player;
			this.refresh = null;
			
			enabled = false;
			hasRadar = false;
		}
		
		public void setLoc(int x, int y, int z)
		{
			newX = x;
			newY = y;
			newZ = z;
		}
		
		public void enable()
		{
			enabled = true;
			applyRadar();
		}
		
		public void disable()
		{
			enabled = false;
			
			if(hasRadar)
			{
				player.removeRadarMarker(currentX, currentY, currentZ);
				hasRadar = false;
			}
		}
		
		public void setRepeat(boolean nextRepeatPolicy)
		{
			if(!enabled || (repeat && !nextRepeatPolicy))
			{
				// cancel repeating
				if(refresh != null)
				{
					refresh.cancel(false);
					refresh = null;
				}
			}
			else if(!repeat && nextRepeatPolicy)
			{
				if(refresh != null)
				{
					refresh.cancel(false);
					refresh = null;
				}
				
				// schedule next repeat
				refresh = CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						applyRadar();
					}
				}, 10000);
			}
			
			repeat = nextRepeatPolicy;
		}
		
		private void applyRadar()
		{
			if(enabled)
			{
				if(hasRadar)
				{
					player.removeRadarMarker(currentX, currentY, currentZ);
					hasRadar = false;
				}
			
				player.addRadarMarker(newX, newY, newZ);
				
				currentX = newX;
				currentY = newY;
				currentZ = newZ;
				
				hasRadar = true;
				
				if(repeat)
					schedule();
			}
		}
		
		private void schedule()
		{
			// schedule next repeat
			refresh = CallBack.getInstance().getOut().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					applyRadar();
				}
			}, 10000);
		}
		
		public boolean isEnabled()
		{
			return enabled;
		}
		
		public boolean isRepeating()
		{
			return repeat;
		}
	}

	public class AfkChecker implements Runnable
	{
		private final PlayerEventInfo player;
		private ScheduledFuture<?> _nextTask;
		
		private boolean isAfk;
		private int totalTimeAfk;
		private int tempTimeAfk;
		
		private boolean isWarned;
		
		public AfkChecker(PlayerEventInfo player)
		{
			this.player = player;
			
			isWarned = false;
			isAfk = false;
			
			totalTimeAfk = 0;
			tempTimeAfk = 0;
			
			check();
		}
		
		public void onAction()
		{
			if(!isInEvent())
				return;
			
			if(_nextTask != null)
				_nextTask.cancel(false);
			
			tempTimeAfk = 0;
			isWarned = false;
			
			if(isAfk)
			{
				_owner.sendMessage("Welcome back. Total time spent AFK so far: " + totalTimeAfk);
				isAfk = false;
				
				if(_activeEvent != null)
					_activeEvent.playerReturnedFromAfk(player);
			}
			
			check();
		}
		
		@Override
		public synchronized void run()
		{
			if(!isInEvent())
				return;
			
			if(isWarned) // a warning has already been sent to this player
			{
				if(!_disableAfkCheck && !_owner.isDead()) // isDead needs to be fixed TODO
				{
					if(isAfk)
					{
						totalTimeAfk += 10;
						tempTimeAfk += 10;
					}
					else 
						isAfk = true;
					
					if(_activeEvent != null)
						_activeEvent.playerWentAfk(player, false, tempTimeAfk);
				}
				
				check(10000);
			}
			else // send a warning
			{
				if(!_disableAfkCheck && !_owner.isDead()) // isDead needs to be fixed TODO
				{
					isWarned = true;
					
					if(getActiveGame() != null)
						getActiveGame().playerWentAfk(player, true, 0);
					
					if(getActiveEvent() != null)
						getActiveEvent().playerWentAfk(player, true, 0);
				}
				
				check();
			}
		}
		
		private synchronized void check()
		{
			if(_disableAfkCheck)
				return;
			
			if(_nextTask != null)
				_nextTask.cancel(false);
			
			_nextTask = ThreadPoolManager.getInstance().scheduleGeneral(this, isWarned ? AFK_KICK_DELAY : AFK_WARNING_DELAY);
		}
		
		private synchronized void check(long delay)
		{
			if(_disableAfkCheck)
				return;
			
			if(_nextTask != null)
				_nextTask.cancel(false);
			
			if(isAfk)
				_nextTask = ThreadPoolManager.getInstance().scheduleGeneral(this, delay);
		}
		
		// called on the end of event
		public void stop()
		{
			if(_nextTask != null)
				_nextTask.cancel(false);
			
			_nextTask = null;
			
			isAfk = false;
			isWarned = false;
			
			totalTimeAfk = 0;
			tempTimeAfk = 0;
		}
	}
}
