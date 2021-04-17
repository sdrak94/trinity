package cz.nxs.interf;

import cz.nxs.events.EventGame;
import cz.nxs.events.NexusLoader;
import cz.nxs.events.NexusLoader.NexusBranch;
import cz.nxs.events.engine.EventBuffer;
import cz.nxs.events.engine.EventConfig;
import cz.nxs.events.engine.EventManagement;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.base.EventType;
import cz.nxs.events.engine.main.MainEventManager.State;
import cz.nxs.events.engine.mini.EventMode.FeatureType;
import cz.nxs.events.engine.mini.MiniEventManager;
import cz.nxs.events.engine.mini.features.AbstractFeature;
import cz.nxs.events.engine.mini.features.EnchantFeature;
import cz.nxs.interf.callback.HtmlManager;
import cz.nxs.interf.callback.api.DescriptionLoader;
import cz.nxs.interf.delegate.CharacterData;
import cz.nxs.interf.delegate.SkillData;
import cz.nxs.l2j.CallBack;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * @author hNoke
 *
 */
@SuppressWarnings("unused")
public class NexusEvents
{
	public static final String _desc = "L2Jserver Freya";
	public static final NexusBranch _branch = NexusBranch.Freya;
	public static final double _interfaceVersion = 2.1;
	public static final boolean _allowInstances = true;
	public static final String _libsFolder = "../libs/";
	public static final String _serialPath = "config/nexus_serial.txt";
	
	// true if the client has html size limit (interlude has it, maybe some other client has it too) which when is exceeded, client crashes
	// gracia final and above do not seem to have it
	public static final boolean _limitedHtml = false;
	
	public static void start()
	{
		NexusOut.getInstance().load();
		PlayerBase.getInstance().load();
		Values.getInstance().load();
		
		//PvpZoneManager.getInstance();
		
		NexusLoader.init(_branch, _interfaceVersion, _desc, _allowInstances, _libsFolder, _serialPath, _limitedHtml);
	}
	
	public static void loadHtmlManager()
	{
		HtmlManager.load();
		DescriptionLoader.load();
	}
	
	public static void serverShutDown()
	{
		//TODO save data
	}
	
	public static void onLogin(L2PcInstance player)
	{
		EventBuffer.getInstance().loadPlayer(player.getEventInfo());
		EventManager.getInstance().onPlayerLogin(player.getEventInfo());
	}
	
	public static PlayerEventInfo getPlayer(L2PcInstance player)
	{
		return NexusLoader.loaded() ? PlayerBase.getInstance().getPlayer(player) : null;
	}
	
	public static boolean isRegistered(L2PcInstance player)
	{
		PlayerEventInfo pi = getPlayer(player);
		return pi != null && pi.isRegistered();
	}
	
	public static boolean isInEvent(L2PcInstance player)
	{
		PlayerEventInfo pi = getPlayer(player);
		return pi != null && pi.isInEvent();
	}
	
	public static boolean isInEvent(L2Character ch)
	{
		if(ch instanceof L2Playable)
			return isInEvent(ch.getActingPlayer());
		else
			return EventManager.getInstance().isInEvent(new CharacterData(ch));
	}
	
	public static boolean allowDie(L2Character ch, L2Character attacker)
	{
		if(isInEvent(ch) && isInEvent(attacker))
			return EventManager.getInstance().allowDie(new CharacterData(ch), new CharacterData(attacker));
		return true;
	}
	
	public static boolean isInMiniEvent(L2PcInstance player)
	{
		PlayerEventInfo pi = getPlayer(player);
		return pi != null && pi.getActiveGame() != null;
	}
	
	public static boolean isInMainEvent(L2PcInstance player)
	{
		PlayerEventInfo pi = getPlayer(player);
		return pi != null && pi.getActiveEvent() != null;
	}
	
	public static boolean canShowToVillageWindow(L2PcInstance player)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			return pi.canShowToVillageWindow();
		return true;
	}
	
	public static boolean canAttack(L2PcInstance player, L2Character target)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			return pi.canAttack(target);
		return true;
	}
	
	public static boolean onAttack(L2Character cha, L2Character target)
	{
		return EventManager.getInstance().onAttack(new CharacterData(cha), new CharacterData(target));
	}
	
	public static void trySuicide(L2PcInstance player)
	{
		final PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
		{
			if(pi.getActiveEvent() != null && pi.getActiveEvent().getEventType() != EventType.Mutant)
			{
				pi.sendMessage("You will commit a suicide in 30 seconds.");
				
				CallBack.getInstance().getOut().scheduleGeneral(new Runnable() 
				{
					@Override
					public void run() 
					{
						if(pi.getActiveEvent() != null && EventManager.getInstance().getMainEventManager().getState() == State.RUNNING)
						{
							pi.doDie();
						}
					}
				}, 30000);
				
			}
			else
			{
				pi.sendMessage("You cannot do that in this event.");
			}
		}
	}
	
	public static boolean canSupport(L2PcInstance player, L2Character target)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			return pi.canSupport(target);
		return true;
	}
	
	public static boolean canTarget(L2PcInstance player, L2Object target)
	{
		//TODO
		return true;
	}
	
	// ***
	
	public static void onHit(L2PcInstance player, L2Character target, int damage, boolean isDOT)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			pi.onDamageGive(target, damage, isDOT);
	}
	
	public static void onDamageGive(L2Character cha, L2Character target, int damage, boolean isDOT)
	{
		EventManager.getInstance().onDamageGive(new CharacterData(cha), new CharacterData(target), damage, isDOT);
	}
	
	public static void onKill(L2PcInstance player, L2Character target)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			pi.notifyKill(target);
	}
	
	public static void onDie(L2PcInstance player, L2Character killer)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			pi.notifyDie(killer);
	}
	
	/** returning true will make the default NPC actions skipped (showing html, etc. */
	public static boolean onNpcAction(L2PcInstance player, L2Npc target)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			return pi.notifyNpcAction(target);
		return false;
	}
	
	public static boolean canUseItem(L2PcInstance player, L2ItemInstance item)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			return pi.canUseItem(item);
		return true;
	}
	
	public static void onUseItem(L2PcInstance player, L2ItemInstance item)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			pi.notifyItemUse(item);
	}
	
	public static boolean onSay(L2PcInstance player, String text, int channel)
	{
		/*try
		{
			if(text.startsWith("."))
			{
				if(EventManager.getInstance().tryVoicedCommand(player.getEventInfo(), text))
					return false;
				else 
					return true;
			}
		}
		catch (Exception e)
		{
		}*/
		
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			return pi.notifySay(text, channel);
		
		return true;
	}
	
	public static boolean canUseSkill(L2PcInstance player, L2Skill skill)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			return pi.canUseSkill(skill);
		return true;
	}
	
	public static void onUseSkill(L2PcInstance player, L2Skill skill)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			pi.onUseSkill(skill);
	}
	
	public static boolean canDestroyItem(L2PcInstance player, L2ItemInstance item)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			return pi.canDestroyItem(item);
		return true;
	}
	
	public static void pvpZoneSwitched()
	{
		EventManager.getInstance().pvpZoneSwitched();
	}
	
	public static boolean canInviteToParty(L2PcInstance player, L2PcInstance target)
	{
		PlayerEventInfo pi = getPlayer(player);
		PlayerEventInfo targetPi = getPlayer(target);
		if(pi != null)
		{
			if(targetPi == null)
				return false;
			else
				return pi.canInviteToParty(pi, targetPi);
		}
		return true;
	}
	
	public static boolean canTransform(L2PcInstance player)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			return pi.canTransform(pi);
		return true;
	}
	
	/**
	 * @return 0 if the engine doesn't care, -1 if the engine doesn't allow this skill, 1 if the engine allows this skill
	 */
	public static int allowTransformationSkill(L2PcInstance player, L2Skill s)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			return pi.allowTransformationSkill(s);
		return 0;
	}
	
	public static boolean canBeDisarmed(L2PcInstance player)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			return pi.canBeDisarmed(pi);
		return true;
	}
	
	/** returning true will 'mark' the bypass as already assigned and perfomed */
	public static boolean onBypass(L2PcInstance player, String command)
	{
		if(command.startsWith("nxs_"))
			return EventManager.getInstance().onBypass(player.getEventInfo(), command.substring(4));
		return false;
	}
	
	public static void onAdminBypass(PlayerEventInfo player, String command)
	{
		EventManagement.getInstance().onBypass(player, command);
	}
	
	// ***
	
	public static boolean canLogout(L2PcInstance player)
	{
		PlayerEventInfo pi = getPlayer(player);
		return !(pi != null && pi.isInEvent());
	}
	
	public static void onLogout(L2PcInstance player)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null)
			pi.notifyDisconnect();
	}
	
	public static boolean isObserving(L2PcInstance player)
	{
		return player.getEventInfo().isSpectator();
	}
	
	public static void endObserving(L2PcInstance player)
	{
		EventManager.getInstance().removePlayerFromObserverMode(player.getEventInfo());
	}
	
	public static boolean canSaveShortcuts(L2PcInstance activeChar)
	{
		PlayerEventInfo pi = getPlayer(activeChar);
		if(pi != null)
			pi.canSaveShortcuts();
		return true;
	}
	
	public static int getItemAutoEnchantValue(L2PcInstance player, L2ItemInstance item)
	{
		if(isInEvent(player))
		{
			PlayerEventInfo pi = PlayerBase.getInstance().getPlayer(player);
			
			MiniEventManager event = pi.getRegisteredMiniEvent();
			if(event == null)
				return 0;
			
			for(AbstractFeature f : event.getMode().getFeatures())
			{
				if(f.getType() == FeatureType.Enchant)
				{
					switch(item.getItem().getType2())
					{
						case L2Item.TYPE2_WEAPON:
							return ((EnchantFeature) f).getAutoEnchantWeapon();
						case L2Item.TYPE2_SHIELD_ARMOR:
							return ((EnchantFeature) f).getAutoEnchantArmor();
						case L2Item.TYPE2_ACCESSORY:
							return ((EnchantFeature) f).getAutoEnchantJewel();
					}
				}
			}
			
			return 0;
		}
		else
			return 0;
	}
	
	public static boolean removeCubics()
	{
		return EventConfig.getInstance().getGlobalConfigBoolean("removeCubicsOnDie");
	}
	
	public static boolean gainPvpPointsOnEvents()
	{
		return EventConfig.getInstance().getGlobalConfigBoolean("pvpPointsOnKill");
	}
	
	public static boolean cbBypass(L2PcInstance player, String command)
	{
		PlayerEventInfo pi = getPlayer(player);
		if(pi != null && command != null)
			return EventManager.getInstance().getHtmlManager().onCbBypass(pi, command);
		return false;
	}
	
	public static String consoleCommand(String cmd)
	{
		if(cmd.startsWith("reload_globalconfig"))
		{
			EventConfig.getInstance().loadGlobalConfigs();
			return "Global configs reloaded.";
		}
		else return "This command doesn't exist.";
	}
	
	public static boolean adminCommandRequiresConfirm(String cmd)
	{
		if(cmd.split(" ").length > 1)
		{
			String command = cmd.split(" ")[1];
			return EventManagement.getInstance().commandRequiresConfirm(command);
		}
		
		return false;
	}
	
	public static boolean isSkillOffensive(L2PcInstance activeChar, L2Skill skill)
	{
		PlayerEventInfo pi = getPlayer(activeChar);
		if(pi != null)
		{
			if(pi.isInEvent())
			{
				EventGame game = pi.getEvent();
				int val = game.isSkillOffensive(new SkillData(skill));
				if(val == 1)
					return true;
				else if(val == 0)
					return false;
			}
		}
		
		return skill.isOffensive();
	}
	
	public static boolean isSkillNeutral(L2PcInstance activeChar, L2Skill skill)
	{
		PlayerEventInfo pi = getPlayer(activeChar);
		if(pi != null)
		{
			if(pi.isInEvent())
			{
				EventGame game = pi.getEvent();
				return game.isSkillNeutral(new SkillData(skill));
			}
		}
		return false;
	}
	
	public static boolean isPvpZoneActive()
	{
		return EventManager.getInstance().getMainEventManager().getPvpZoneManager().isActive();
	}
	
	public static String getPvpZoneName()
	{
		return EventManager.getInstance().getMainEventManager().getPvpZoneManager().getMapName();
	}
	
	public static String getPvpZoneTimeActive()
	{
		return EventManager.getInstance().getMainEventManager().getPvpZoneManager().timeActive();
	}
	
	public static int getPvpZonePlayersCount()
	{
		return EventManager.getInstance().getMainEventManager().getPvpZoneManager().getPlayersCount();
	}
	
	public static void registerToPvpZone(PlayerEventInfo player)
	{
		EventManager.getInstance().getMainEventManager().getPvpZoneManager().registerPlayer(player);
	}
	
	public static void unregisterFromPvpZone(PlayerEventInfo player)
	{
		EventManager.getInstance().getMainEventManager().getPvpZoneManager().unregisterPlayer(player);
	}
}
