package net.sf.l2j.gameserver.network.clientpackets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;

import luna.custom.captcha.instancemanager.BotsPreventionManager;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.datatables.PetSkillsTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ChairSit;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopManageList;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestActionUse extends L2GameClientPacket
{
private static final String _C__45_REQUESTACTIONUSE = "[C] 45 RequestActionUse";

private int _actionId;
private boolean _ctrlPressed;
private boolean _shiftPressed;

@Override
protected void readImpl()
{
	_actionId = readD();
	_ctrlPressed = (readD() == 1);
	_shiftPressed = (readC() == 1);
}

@Override
protected void runImpl()
{
	L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar == null)
		return;
	
	if (Config.DEBUG)
		_log.finest(activeChar.getName() + " request Action use: id " + _actionId + " 2:" + _ctrlPressed + " 3:" + _shiftPressed);
	
	// dont do anything if player is dead
	if (activeChar.isAlikeDead())
	{
		getClient().sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	// don't do anything if player is confused
	if (activeChar.isOutOfControl() || activeChar.isInJail())
	{
		getClient().sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	// don't allow to do some action if player is transformed
	if (activeChar.isTransformed())
	{
		int[] notAllowedActions = {0, 10, 28, 37, 51, 61};
		if (Arrays.binarySearch(notAllowedActions,_actionId) >= 0)
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
	}
	
	L2Summon pet = activeChar.getPet();
	L2Object target = activeChar.getTarget();
	
	if (Config.DEBUG)
		_log.info("Requested Action ID: " + String.valueOf(_actionId));
	
	if (target != null && target instanceof L2Playable)
		if (!activeChar.isAttackable((L2Playable)target))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
	
	switch (_actionId)
	{
	case 0:
		if (activeChar.getMountType() != 0)
			break;
		
		if (target != null && !activeChar.isSitting() && target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 1 && CastleManager.getInstance().getCastle(target) != null
				&& activeChar.isInsideRadius(target, L2StaticObjectInstance.INTERACTION_DISTANCE, false, false))
		{
			ChairSit cs = new ChairSit(activeChar, ((L2StaticObjectInstance) target).getStaticObjectId());
			activeChar.sendPacket(cs);
			activeChar.sitDown();
			activeChar.broadcastPacket(cs);
			break;
		}
		
		if (activeChar.isSitting())
			activeChar.standUp();
		else
		{
			/*if (activeChar.isGM() || !activeChar.isInCombat())*/
			activeChar.sitDown();
			/*else
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				activeChar.sendMessage("Can't sit down in combat mode");
			}*/
		}
		
		if (Config.DEBUG)
			_log.fine("new wait type: " + (activeChar.isSitting() ? "SITTING" : "STANDING"));
		
		break;
	case 1:
		if (activeChar.isRunning())
			activeChar.setWalking();
		else
			activeChar.setRunning();
		
		if (Config.DEBUG)
			_log.fine("new move type: " + (activeChar.isRunning() ? "RUNNING" : "WALKIN"));
		break;
	case 10:
		// Private Store Sell
		activeChar.tryOpenPrivateSellStore(false);
		break;
	case 28:
		activeChar.tryOpenPrivateBuyStore();
		break;
	case 15:
	case 21: // pet follow/stop
		if (pet != null && !activeChar.isBetrayed())
			((L2SummonAI) pet.getAI()).notifyFollowStatusChange();
		break;
	case 16:
	case 22: // pet attack
		if (target != null && pet != null && pet != target && !pet.isAttackingDisabled() && !pet.isBetrayed())
		{
			if (pet instanceof L2PetInstance && (pet.getLevel() - activeChar.getLevel() > 20))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_TOO_HIGH_TO_CONTROL));
				return;
			}
			
			if (activeChar.isInOlympiadMode() && !activeChar.isOlympiadStart())
			{
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!activeChar.isGM() && target instanceof L2Playable && ((L2Playable)target).isInsidePeaceZone(activeChar))
			{
				if (!activeChar.isInFunEvent() || !((L2Playable)target).isInFunEvent() || ((L2Playable)target).getActingPlayer().eventSitForced)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
					return;
				}
			}
			
			if (pet.getNpcId() == 12564 || pet.getNpcId() == 12621)
			{
				// sin eater and wyvern can't attack with attack button
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (pet.isLockedTarget())
			{
				pet.getOwner().sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
				return;
			}
			
			if (target.isAutoAttackable(activeChar) || _ctrlPressed)
			{
				if (target instanceof L2DoorInstance)
				{
					if (((L2DoorInstance) target).isAttackable(activeChar) && pet.getNpcId() != L2SiegeSummonInstance.SWOOP_CANNON_ID)
						pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
				}
				// siege golem AI doesn't support attacking other than doors at the moment
				else if (pet.getNpcId() != L2SiegeSummonInstance.SIEGE_GOLEM_ID)
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			}
			else
			{
				pet.setFollowStatus(false);
				pet.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, target);
			}
		}
		break;
	case 17:
	case 23: // pet - cancel action
		if (pet != null && !pet.isMovementDisabled() && !activeChar.isBetrayed())
			pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
		break;
	case 19: // pet unsummon
		if (pet != null)
		{
			//returns pet to control item
			if (pet.isDead())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED));
			}
			else if (pet.isAttackingNow() || pet.isInCombat() || pet.isRooted() || pet.isBetrayed())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE));
			}
			else
			{
				// if it is a pet and not a summon
				if (pet instanceof L2PetInstance)
				{
					if (pet.isInCombat())
						activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE));
					else
						pet.unSummon(activeChar);
				}
			}
		}
		break;
	case 38: // pet mount
		activeChar.mountPlayer(pet);
		break;
	case 32: // Wild Hog Cannon - Mode Change
		useSkill(4230);
		break;
	case 36: // Soulless - Toxic Smoke
		useSkill(4259);
		break;
	case 37:
		if (activeChar.isAlikeDead())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			activeChar.broadcastUserInfo();
		}
		if (activeChar.isSitting())
			activeChar.standUp();
		
		if (activeChar.getCreateList() == null)
		{
			activeChar.setCreateList(new L2ManufactureList());
		}
		
		activeChar.sendPacket(new RecipeShopManageList(activeChar, true));
		break;
	case 39: // Soulless - Parasite Burst
		useSkill(4138);
		break;
	case 41: // Wild Hog Cannon - Attack
		useSkill(4230);
		break;
	case 42: // Kai the Cat - Self Damage Shield
		useSkill(4378, activeChar);
		break;
	case 43: // Unicorn Merrow - Hydro Screw
		useSkill(4137);
		break;
	case 44: // Big Boom - Boom Attack
		useSkill(4139);
		break;
	case 45: // Unicorn Boxer - Master Recharge
		useSkill(4025, activeChar);
		break;
	case 46: // Mew the Cat - Mega Storm Strike
		useSkill(4261);
		break;
	case 47: // Silhouette - Steal Blood
		useSkill(4260);
		break;
	case 48: // Mechanic Golem - Mech. Cannon
		useSkill(4068);
		break;
	case 51:
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (activeChar.isAlikeDead())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			activeChar.broadcastUserInfo();
		}
		if (activeChar.isSitting())
			activeChar.standUp();
		
		if (activeChar.getCreateList() == null)
			activeChar.setCreateList(new L2ManufactureList());
		
		activeChar.sendPacket(new RecipeShopManageList(activeChar, false));
		break;
	case 52: // unsummon
		if (pet != null && pet instanceof L2SummonInstance)
		{
			if (pet.isBetrayed())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_REFUSING_ORDER));
			}
			else if (pet.isAttackingNow() || pet.isInCombat())
				activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE));
			else
				pet.unSummon(activeChar);
		}
		break;
	case 53: // move to target
		if (target != null && pet != null && pet != target && !pet.isMovementDisabled() && !pet.isBetrayed())
		{
			pet.setFollowStatus(false);
			pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), 0));
		}
		break;
	case 54: // move to target hatch/strider
		if (target != null && pet != null && pet != target && !pet.isMovementDisabled() && !pet.isBetrayed())
		{
			pet.setFollowStatus(false);
			pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), 0));
		}
		break;
	case 61:
		// Private Store Package Sell
		activeChar.tryOpenPrivateSellStore(true);
		break;
	case 65:
		// Bot report Button.
		if (target == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (target == activeChar)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (target.getActingPlayer().getPvpFlag() > 0)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (target.getActingPlayer().isInHuntersVillage() || target.getActingPlayer().isInOrcVillage() || target.getActingPlayer().isInPI() || target.getActingPlayer().isInsideZone(L2Character.ZONE_TOWN) || target.isInFunEvent())
		{
			activeChar.sendMessage("Target is not in a proper zone, avoid abusing the report button. Use it carefully!");
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (!(target instanceof L2PcInstance))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (activeChar.isInOlympiadMode())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (target.getActingPlayer().isInsidePeaceZone(activeChar))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (target.getActingPlayer().isInFunEvent())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (target.getActingPlayer().isInSameClanOrAllianceAs(activeChar))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (!activeChar.isGM() && target.getActingPlayer().getIP().equalsIgnoreCase(activeChar.getIP()))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (target.getActingPlayer().isSitting())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (!(target.getActingPlayer().isInsideZone(L2Character.ZONE_FARM) || target.getActingPlayer().isInsideZone(L2Character.ZONE_CHAOTIC)))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (target.getActingPlayer().isInsideZone(L2Character.ZONE_EVENT))
		{
			_log.warning("Player " + activeChar.getName() + " tried to report player "+ target.getName()+" during event.");
			String msgContent = activeChar.getName()  + " tried to report player "+ target.getName()+" during event.";
			GmListTable.broadcastToGMs(new CreatureSay(activeChar.getObjectId(), 9, "Event Protection",  msgContent));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!activeChar.isGM() && target.getActingPlayer().getLastCaptchaTimeStamp()+18000 >= GameTimeController.getGameTicks())
		{
			activeChar.sendMessage(target.getName()+" Has already answered correct in the past 30 minutes and cannot be reported again, please try again later.");
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		final int ticks = GameTimeController.getGameTicks();

		if (!activeChar.isGM() && getClient().reportTickTimer >= ticks)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			activeChar.sendMessage("Please wait 10 seconds before reporting again");
			return;
		}
		getClient().reportTickTimer = ticks + 600;
		
		String msgContent = activeChar.getName() + " has submitted a bot report on player " + activeChar.getTarget().getName();
		
		GmListTable.broadcastToGMs(new CreatureSay(activeChar.getObjectId(), 2, "Bot System",  msgContent));
		
		if(Config.ENABLE_BOT_CAPTCHA)
		{
			BotsPreventionManager.getInstance().validationtasks(activeChar.getTarget().getActingPlayer());
		}
		
		//L2Character target = activeChar.getTarget();
		//BotReportTable.getInstance().reportBot(activeChar);
		//AntibotSystem.sendFarmBotSignal((L2Character)target);
		
				SimpleDateFormat formatter;
				formatter = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");
				String date = formatter.format(new Date());
				
				FileWriter save = null;
				try
				{
					File file = new File("log/BotReport.csv");
					
					boolean writeHead = false;
					if (!file.exists())
						writeHead = true;
					
					save = new FileWriter(file, true);
					
					if (writeHead)
					{
						String header = "Date,BotName,Reporter,BotIP,ReporterIP,BotLocationX,BotLocationY,BotLocationZ\r\n";
						save.write(header);
					}
					
					String out = date + "," + activeChar.getTarget().getName() + ","
					        + activeChar.getName() + "," + target.getActingPlayer().getIP() + "," + activeChar.getIP() + ","
					        + activeChar.getTarget().getPosition().getX() + ","
					        + activeChar.getTarget().getPosition().getY() + ","
					        + activeChar.getTarget().getPosition().getZ() + "\r\n";
					save.write(out);
				}
				catch (IOException e)
				{
					_log.log(Level.WARNING, "Bot reporting system: BotReport.csv could not be saved: ", e);
				}
				finally
				{
					try
					{
						save.close();
					}
					catch (Exception e)
					{
					}
				}
		
		activeChar.sendMessage("Target has been reported as a possible bot. The staff will begin investigating the player. Thank you.");
		break;
	case 96: // Quit Party Command Channel
		_log.info("98 Accessed");
		break;
	case 97: // Request Party Command Channel Info
		//if (!PartyCommandManager.getInstance().isPlayerInChannel(activeChar))
		//return;
		_log.info("97 Accessed");
		//PartyCommandManager.getInstance().getActiveChannelInfo(activeChar);
		break;
	case 1000: // Siege Golem - Siege Hammer
		if (target instanceof L2DoorInstance)
			useSkill(4079);
		break;
	case 1001:
		break;
	case 1003: // Wind Hatchling/Strider - Wild Stun
		useSkill(4710);
		break;
	case 1004: // Wind Hatchling/Strider - Wild Defense
		useSkill(4711, activeChar);
		break;
	case 1005: // Star Hatchling/Strider - Bright Burst
		useSkill(4712);
		break;
	case 1006: // Star Hatchling/Strider - Bright Heal
		useSkill(4713, activeChar);
		break;
	case 1007: // Cat Queen - Blessing of Queen
		useSkill(4699, activeChar);
		break;
	case 1008: // Cat Queen - Gift of Queen
		useSkill(4700, activeChar);
		break;
	case 1009: // Cat Queen - Cure of Queen
		useSkill(4701);
		break;
	case 1010: // Unicorn Seraphim - Blessing of Seraphim
		useSkill(4702, activeChar);
		break;
	case 1011: // Unicorn Seraphim - Gift of Seraphim
		useSkill(4703, activeChar);
		break;
	case 1012: // Unicorn Seraphim - Cure of Seraphim
		useSkill(4704);
		break;
	case 1013: // Nightshade - Curse of Shade
		useSkill(4705);
		break;
	case 1014: // Nightshade - Mass Curse of Shade
		useSkill(4706);
		break;
	case 1015: // Nightshade - Shade Sacrifice
		useSkill(4707);
		break;
	case 1016: // Cursed Man - Cursed Blow
		useSkill(4709);
		break;
	case 1017: // Cursed Man - Cursed Strike/Stun
		useSkill(4708);
		break;
	case 1031: // Feline King - Slash
		useSkill(5135);
		break;
	case 1032: // Feline King - Spinning Slash
		useSkill(5136);
		break;
	case 1033: // Feline King - Grip of the Cat
		useSkill(5137);
		break;
	case 1034: // Magnus the Unicorn - Whiplash
		useSkill(5138);
		break;
	case 1035: // Magnus the Unicorn - Tridal Wave
		useSkill(5139);
		break;
	case 1036: // Spectral Lord - Corpse Kaboom
		useSkill(5142);
		break;
	case 1037: // Spectral Lord - Dicing Death
		useSkill(5141);
		break;
	case 1038: // Spectral Lord - Force Curse
		useSkill(5140);
		break;
	case 1039: // Swoop Cannon - Cannon Fodder
		if (!(target instanceof L2DoorInstance))
			useSkill(5110);
		break;
	case 1040: // Swoop Cannon - Big Bang
		if (!(target instanceof L2DoorInstance))
			useSkill(5111);
		break;
	case 1041: // Great Wolf - Bite Attack
		useSkill(5442);
		break;
	case 1042: // Great Wolf - Maul
		useSkill(5444);
		break;
	case 1043: // Great Wolf - Cry of the Wolf
		useSkill(5443);
		break;
	case 1044: // Great Wolf - Awakening
		useSkill(5445);
		break;
	case 1045: // Great Wolf - Howl
		useSkill(5584);
		break;
		//Add by rocknow
	case 1046: // Strider - Roar
		useSkill(5585);
		break;
		// CT2.3
	case 1047: // Divine Beast - Bite
		useSkill(5580);
		break;
	case 1048: // Divine Beast - Stun Attack
		useSkill(5581);
		break;
	case 1049: // Divine Beast - Fire Breath
		useSkill(5582);
		break;
	case 1050: // Divine Beast - Roar
		useSkill(5583);
		break;
	case 1051: //Feline Queen - Bless The Body
		useSkill(5638);
		break;
	case 1052: //Feline Queen - Bless The Soul
		useSkill(5639);
		break;
	case 1053: //Feline Queen - Haste
		useSkill(5640);
		break;
	case 1054: //Unicorn Seraphim - Acumen
		useSkill(5643);
		break;
	case 1055: //Unicorn Seraphim - Clarity
		useSkill(5647);
		break;
	case 1056: //Unicorn Seraphim - Empower
		useSkill(5648);
		break;
	case 1057: //Unicorn Seraphim - Wild Magic
		useSkill(5646);
		break;
	case 1058: //Nightshade - Death Whisper
		useSkill(5652);
		break;
	case 1059: //Nightshade - Focus
		useSkill(5653);
		break;
	case 1060: //Nightshade - Guidance
		useSkill(5654);
		break;
		//Add by rocknow
	case 1061:
		useSkill(5745); // Death blow
		break;
	case 1062:
		useSkill(5746); // Double attack
		break;
	case 1063:
		useSkill(5747); // Spin attack
		break;
	case 1064:
		useSkill(5748); // Meteor Shower
		break;
	case 1065:
		useSkill(5753); // Awakening
		break;
	case 1066:
		useSkill(5749); // Thunder Bolt
		break;
	case 1067:
		useSkill(5750); // Flash
		break;
	case 1068:
		useSkill(5751); // Lightning Wave
		break;
	case 1069:
		useSkill(5752); // Flare
		break;
	case 1070:
		useSkill(5771);	// Buff control
		break;
	case 1071:
		useSkill(5761); // Power Strike
		break;
	case 1072:
		useSkill(6046); // Piercing attack
		break;
	case 1073:
		useSkill(6047); // Whirlwind
		break;
	case 1074:
		useSkill(6048); // Lance Smash
		break;
	case 1075:
		useSkill(6049); // Battle Cry
		break;
	case 1076:
		useSkill(6050); // Power Smash
		break;
	case 1077:
		useSkill(6051); // Energy Burst
		break;
	case 1078:
		useSkill(6052); // Shockwave
		break;
	case 1079:
		useSkill(6053); // Howl
		break;
	case 1080:
		useSkill(6041); // Phoenix Rush
		break;
	case 1081:
		useSkill(6042); // Phoenix Cleanse
		break;
	case 1082:
		useSkill(6043); // Phoenix Flame Feather
		break;
	case 1083:
		useSkill(6044); // Phoenix Flame Beak
		break;
	case 1084:
		useSkill(6054); // Switch State
		break;
	case 1086:
		useSkill(6094); // Panther Cancel
		break;
	case 1087:
		useSkill(6095); // Panther Dark Claw
		break;
	case 1088:
		useSkill(6096); // Panther Fatal Claw
		break;
		// CT2.3 Social Packets
	case 12:
		tryBroadcastSocial(2);
		break;
	case 13:
		tryBroadcastSocial(3);
		break;
	case 14:
		tryBroadcastSocial(4);
		break;
	case 24:
		tryBroadcastSocial(6);
		break;
	case 25:
		tryBroadcastSocial(5);
		break;
	case 26:
		tryBroadcastSocial(7);
		break;
	case 29:
		tryBroadcastSocial(8);
		break;
	case 30:
		tryBroadcastSocial(9);
		break;
	case 31:
		tryBroadcastSocial(10);
		break;
	case 33:
		tryBroadcastSocial(11);
		break;
	case 34:
		tryBroadcastSocial(12);
		break;
	case 35:
		tryBroadcastSocial(13);
		break;
	case 62:
		tryBroadcastSocial(14);
		break;
	case 66:
		tryBroadcastSocial(15);
		break;
	default:
		_log.warning(activeChar.getName() + ": unhandled action type " + _actionId);
	}
}

/*
 * Cast a skill for active pet/servitor.
 * Target is specified as a parameter but can be
 * overwrited or ignored depending on skill type.
 */
private void useSkill(int skillId, L2Object target)
{
	L2PcInstance activeChar = getClient().getActiveChar();
	if (activeChar == null)
		return;
	
	L2Summon activeSummon = activeChar.getPet();
	
	if (activeChar.getPrivateStoreType() != 0)
	{
		activeChar.sendMessage("Cannot use skills while trading");
		return;
	}
	
	if (activeSummon != null && !activeSummon.isBetrayed())
	{
		if (activeSummon instanceof L2PetInstance && (activeSummon.getLevel() - activeChar.getLevel() > 20))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_TOO_HIGH_TO_CONTROL));
			return;
		}
		int lvl = PetSkillsTable.getInstance().getAvailableLevel(activeSummon, skillId);
		if (lvl == 0)
			return;
		
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, lvl);
		if (skill == null)
			return;
		
		if (activeChar.isInOlympiadMode() && skill.isDisabledInOlympiad())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			sendPacket(new SystemMessage(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}
		if (activeChar.isInKoreanZone() && skill.isDisabledInKorean())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			activeChar.sendMessage("This skill is not allowed in this Custom Event");
			return;
		}
		
		activeSummon.setTarget(target);
		activeSummon.useMagic(skill, _ctrlPressed, _shiftPressed);
	}
}

/*
 * Cast a skill for active pet/servitor.
 * Target is retrieved from owner' target,
 * then validated by overloaded method useSkill(int, L2Character).
 */
private void useSkill(int skillId)
{
	L2PcInstance activeChar = getClient().getActiveChar();
	if (activeChar == null)
		return;
	
	useSkill(skillId, activeChar.getTarget());
}


/*
 * Check if player can broadcast SocialAction packet
 */
private void tryBroadcastSocial(int id)
{
	L2PcInstance activeChar = getClient().getActiveChar();
	if (activeChar == null)
		return;
	
	if (Config.DEBUG)
		_log.fine("Social Action:" + id);
	
	if (activeChar.isFishing())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_3));
		return;
	}
	
	if (activeChar.getPrivateStoreType() == 0 && activeChar.getActiveRequester() == null
			&& !activeChar.isAlikeDead() && (!activeChar.isAllSkillsDisabled() || activeChar.isInDuel())
			&& !activeChar.isCastingNow() && !activeChar.isCastingSimultaneouslyNow()
			&& activeChar.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
	{
		activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), id));
	}
}

@Override
public String getType()
{
	return _C__45_REQUESTACTIONUSE;
}

@Override
protected boolean triggersOnActionRequest()
{
	return true;
}
}
