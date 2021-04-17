package net.sf.l2j.gameserver.instances.DVC;

import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class DVC extends Quest
{
//Coded by Kronologic
//Coded by Kronologic
//NPCs
private static int MALKION = 96012;
private static int EXIT_TELEPORTER = 90013;

//BOSSES
private static final int[] BOSSES = {95602,95622,95623,95624};

//final bosses
private static final int[] GRAND_BOSSES = {95631,95634,95644};

//MOBS
private static final int[] MOBS   = {95126,95125,95124,95123,95122,95128,95129,95130,95132,95133,95134,95135,95136,95626,95628,95629,95630,95602,95623,95674,95643,95683,95684,95685,95686,95687,95688};

private static String qn = "DVC";
private static final int INSTANCEID = 2001;

//REQUIREMENTS
private static boolean debug = false;
private static int levelReq = 90;
private static int pvpReq = 200;
private static int fameReq = 0;
private static int pkReq = 0;


private class teleCoord {int instanceId; int x; int y; int z;}

public class dvcWorld extends InstanceWorld
{
public dvcWorld()
{
	InstanceManager.getInstance().super();
}
}

public DVC(int questId, String name, String descr)
{
	super(questId, name, descr);
	
	addStartNpc(MALKION);
	addTalkId(MALKION);
	addTalkId(EXIT_TELEPORTER);
	
	for (int boss : BOSSES)
		addKillId(boss);
	
	for (int mob : MOBS)
		addKillId(mob);
	
	for (int mob : GRAND_BOSSES)
		addKillId(mob);
}

public static void main(String[] args)
{
	new DVC(-1, qn, "instances");
}

private boolean checkConditions(L2PcInstance player, boolean single)
{
	if (debug || player.isGM())
		return true;
	else
	{
		final L2Party party = player.getParty();
		
		if (!single && (party == null || party.getMemberCount() < 4 || party.getMemberCount() > 7))
		{
			player.sendMessage("This is a 4-7 player party instance, so you must have a party of 4-7 people");
			return false;
		}
		if (!single && party.getPartyLeaderOID() != player.getObjectId())
		{
			player.sendPacket(new SystemMessage(2185));
			return false;
		}
		
		if (!single)
		{
			/*if (!checkIPs(party))
				return false;*/
			
			boolean canEnter = true;
			
			for (L2PcInstance ptm : party.getPartyMembers())
			{
				if (ptm == null) return false;
				
				if (ptm.getLevel() < levelReq)
				{
					ptm.sendMessage("You must be level "+levelReq+" to enter this instance");
					canEnter = false;
				}
				else if (ptm.getPvpKills() < pvpReq)
				{
					ptm.sendMessage("You must have "+pvpReq+" PvPs to enter this instance");
					canEnter = false;
				}
				else if (ptm.getPvpKills() < pkReq)
				{
					ptm.sendMessage("You must have "+pkReq+" PKs to enter this instance");
					canEnter = false;
				}
				else if (ptm.getPvpKills() < fameReq)
				{
					ptm.sendMessage("You must have "+fameReq+" fame to enter this instance");
					canEnter = false;
				}
				else if (ptm.getPvpFlag() != 0 || ptm.getKarma() > 0)
				{
					ptm.sendMessage("You can't enter the instance while in PVP mode or have karma");
					canEnter = false;
				}
				else if (ptm.isInFunEvent())
				{
					ptm.sendMessage("You can't enter the instance while in an event");
					canEnter = false;
				}
				else if (ptm.isInDuel() || ptm.isInOlympiadMode() || Olympiad.getInstance().isRegistered(ptm))
				{
					ptm.sendMessage("You can't enter the instance while in duel/oly");
					canEnter = false;
				}
				else if (System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(ptm.getAccountName(), INSTANCEID))
				{
					ptm.sendMessage("You can only enter this instance once every day, wait until the next 12AM");
					canEnter = false;
				}
				else if (!ptm.isInsideRadius(player, 500, true, false))
				{
					ptm.sendMessage("You're too far away from your party leader");
					player.sendMessage("One of your party members is too far away");
					canEnter = false;
				}
				else
				{
					final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
					
					if (world != null)
					{
						ptm.sendMessage("You can't enter because you have entered into another instance that hasn't expired yet, try waiting 5 min");
						canEnter = false;
					}
				}
				
				if (!canEnter)
				{
					ptm.sendMessage("You're preventing your party from entering an instance");
					if (ptm != player)
						player.sendMessage(ptm.getName()+" is preventing you from entering the instance");
					return false;
				}
			}
		}
		else
		{
			if (player.getLevel() < levelReq)
			{
				player.sendMessage("You must be level "+levelReq+" to enter this instance");
				return false;
			}
			else if (player.getPvpKills() < pvpReq)
			{
				player.sendMessage("You must have "+pvpReq+" PvPs to enter this instance");
				return false;
			}
			else if (player.getPvpFlag() != 0 || player.getKarma() > 0)
			{
				player.sendMessage("You can't enter the instance while in PVP mode or have karma");
				return false;
			}
			else if (player.isInFunEvent())
			{
				player.sendMessage("You can't enter the instance while in an event");
				return false;
			}
			else if (player.isInDuel() || player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
			{
				player.sendMessage("You can't enter the instance while in duel/oly");
				return false;
			}
		}
		
		return true;
	}
}

private void teleportplayer(L2PcInstance player, teleCoord teleto)
{
	player.setInstanceId(teleto.instanceId);
	player.teleToLocation(teleto.x, teleto.y, teleto.z);
	L2Summon pet = player.getPet();
	if (pet != null)
	{
		pet.setInstanceId(teleto.instanceId);
		pet.teleToLocation(teleto.x, teleto.y, teleto.z);
	}
}

protected int enterInstance(L2PcInstance player, String template, teleCoord teleto)
{
	int instanceId = 0;
	
	//check for existing instances for this player
	InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
	//existing instance
	if (world != null)
	{
		if (world.templateId != INSTANCEID)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_ENTERED_ANOTHER_INSTANCE_CANT_ENTER));
			return 0;
		}
		
		if (!checkConditions(player, true))
			return 0;
		
		teleto.instanceId = world.instanceId;
		teleportplayer(player,teleto);
		return instanceId;
	}
	else  //New instance
	{
		if (!checkConditions(player, false))
			return 0;
		
		instanceId = InstanceManager.getInstance().createDynamicInstance(template);
		world = new dvcWorld();
		world.instanceId = instanceId;
		world.templateId = INSTANCEID;
		InstanceManager.getInstance().addWorld(world);
		_log.info("DVC: new " + template + " Instance: " + instanceId + " created by player: " + player.getName());
		
		// teleport players
		teleto.instanceId = instanceId;
		
		L2Party party = player.getParty();
		
		if (party == null)
		{
			if (!player.isGM())
				return 0;
			
			// this can happen only if debug is true
			InstanceManager.getInstance().setInstanceTime(player.getAccountName(), INSTANCEID, getNextInstanceTime(TWODAYS));
			world.allowed.add(player.getObjectId());
			auditInstances(player, template, instanceId);
			teleportplayer(player,teleto);
		}
		else
		{
			for (L2PcInstance partyMember : party.getPartyMembers())
			{
				partyMember.sendMessage("You have entered the Dragon Valley Caves");
				InstanceManager.getInstance().setInstanceTime(partyMember.getAccountName(), INSTANCEID, getNextInstanceTime(TWODAYS));
				world.allowed.add(partyMember.getObjectId());
				auditInstances(partyMember, template, instanceId);
				teleportplayer(partyMember,teleto);
			}		
		}
		spawnMobs((dvcWorld) world, player);
		spawnExitGK((dvcWorld) world, player);
		return instanceId;
	}
	
}
	
private void spawnMobs(dvcWorld world, L2PcInstance player)
{
	// Tortured man start
        addSpawn(95123, 140839, 114225, -3707, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 140237, 116502, -3912, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 139980, 117423, -3912, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 140823, 117993, -3912, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 147028, 119729, -4379, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 153174, 116598, -5257, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 151041, 115272, -5472, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 151078, 109172, -5139, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 150429, 107511, -4711, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 150197, 107893, -5652, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 149505, 108229, -4473, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 146416, 112518, -3720, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 147083, 112414, -3720, 0, false, 0, false, world.instanceId);//
        addSpawn(95123, 149243, 113956, -3720, 0, false, 0, false, world.instanceId);//
		addSpawn(95123, 140899, 112194, -3708, 0, false, 0, false, world.instanceId);//
		addSpawn(95123, 140759, 113172, -3720, 0, false, 0, false, world.instanceId);//
		addSpawn(95123, 143676, 114764, -3720, 0, false, 0, false, world.instanceId);//
		addSpawn(95123, 144406, 114223, -3720, 0, false, 0, false, world.instanceId);//
		addSpawn(95123, 145062, 115432, -3720, 0, false, 0, false, world.instanceId);//
		addSpawn(95123, 145862, 115588, -3720, 0, false, 0, false, world.instanceId);//
		addSpawn(95123, 146239, 116318, -3720, 0, false, 0, false, world.instanceId);//
		addSpawn(95123, 147434, 116371, -3703, 0, false, 0, false, world.instanceId);//
        // Tortured man end
        //Disfigured man start
        addSpawn(95124, 136824, 114728, -3704, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 138635, 114316, -3720, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 139931, 114817, 3703, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 139410, 114375, -3715, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 140221, 117871, -3912, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 140521, 118725, -3912, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 145866, 120523, -3912, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 144762, 118053, -3912, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 144034, 119876, -3912, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 147023, 119271, -4267, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 151344, 117736, -5175, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 153351, 115212, -5224, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 151496, 114796, -5472, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 151306, 115105, -5472, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 151844, 113296, -5520, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 151768, 112442, -5520, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 153485, 108401, -5152, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 146766, 107923, -3815, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 146409, 110651, -3532, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 148881, 113588, -3720, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 149126, 114474, -3720, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 153329, 121507, -3804, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 153176, 121805, -3804, 0, false, 0, false, world.instanceId);//
        addSpawn(95124, 151775, 121032, -3804, 0, false, 0, false, world.instanceId);//
        //Disfugured man end
        //Soul flayer start
        addSpawn(95125, 136941, 114335, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 139943, 114188, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 140387, 114511, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 140745, 115248, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 140378, 117482, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 141527, 119003, -3915, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 141163, 119227, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 142678, 118965, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 143014, 121042, -3915, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 142539, 121416, -3915, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 143073, 121429, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 147248, 120501, -4631, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 147030, 120244, -4538, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 147504, 121238, -4272, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 150276, 118615, -4843, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 152245, 117284, -5243, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 152483, 116838, -5254, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 151660, 115266, -5472, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 150487, 115639, -5472, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 150147, 114923, -5474, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 150769, 114723, -5475, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 151398, 115752, -5472, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 153428, 112648, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 153927, 112821, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 153145, 111879, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 153006, 113049, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 149424, 110855, -5460, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 151010, 109553, -5152, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 151564, 109377, -5153, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 153659, 107923, -5152, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 153368, 107872, -5152, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 152649, 107819, -5111, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 152480, 107489, -5081, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 149335, 108447, -4429, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 148759, 107939, -4285, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 148203, 107294, -4137, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 147779, 107349, -4062, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 147354, 107328, -3989, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 146720, 108184, -3732, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 146305, 110199, -3471, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 146430, 110324, -3489, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 146184, 110541, -3523, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 147521, 112814, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 146714, 112799, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 149007, 113057, -3723, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 149253, 113417, -3723, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 149328, 114645, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 150752, 117843, -3689, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 150557, 117429, -3698, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 152018, 118981, -3809, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 152172, 119962, -3804, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 150408, 121827, -3804, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 152210, 121606, -3804, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 151883, 121638, -3804, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 152791, 120144, -3807, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 148834, 120694, -4862, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 149201, 121013, -4862, 0, false, 0, false, world.instanceId);
        addSpawn(95125, 148586, 121241, -4862, 0, false, 0, false, world.instanceId);
		addSpawn(95125, 141599, 113062, -3720, 0, false, 0, false, world.instanceId);
		addSpawn(95125, 141515, 112468, -3715, 0, false, 0, false, world.instanceId);
		addSpawn(95125, 141519, 112130, -3720, 0, false, 0, false, world.instanceId);
		addSpawn(95125, 140668, 112561, -3720, 0, false, 0, false, world.instanceId);
		addSpawn(95125, 141819, 114716, -3720, 0, false, 0, false, world.instanceId);
		addSpawn(95125, 142025, 114084, -3720, 0, false, 0, false, world.instanceId);
		addSpawn(95125, 142898, 114000, -3720, 0, false, 0, false, world.instanceId);
		addSpawn(95125, 145110, 114443, -3720, 0, false, 0, false, world.instanceId);
        //Soul Flayer End
        //Mistake of the maker start
        addSpawn(95126, 135393, 114636, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 135660, 114908, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 135985, 115196, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 136305, 114838, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 136440, 113841, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 136104, 113715, -3722, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 135614, 113927, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 135584, 114305, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 135178, 114278, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 137512, 114323, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 137947, 114525, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 145221, 121799, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 146217, 121727, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 145771, 120782, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 145409, 120047, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 145952, 117699, -3865, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 146463, 117897, -3886, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 146438, 118157, -3918, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 146872, 118341, -3991, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 150041, 119194, -4862, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 150046, 116762, -5257, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154280, 116653, -5244, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154552, 116516, -5254, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154809, 116103, -5255, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154541, 116227, -5254, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154731, 115678, -5257, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154457, 115445, -5257, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154804, 115168, -5254, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154613, 114696, -5254, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154612, 115196, -5254, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154450, 114852, -5254, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154235, 114371, -5254, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 154467, 114329, -5254, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 151662, 112559, -5523, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 151368, 108999, -5152, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 146509, 108150, -3703, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 148621, 112804, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 148420, 111849, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95126, 148967, 112384, -3720, 0, false, 0, false, world.instanceId);
        //Mistake of the maker end
        //Magmacoil start
        addSpawn(95127, 140119, 119219, -3899, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 140217, 121535, -3890, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 142079, 118706, -3896, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 143086, 118194, -3895, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 142469, 121115, -3891, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 149773, 120547, -4848, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 142079, 118706, -3896, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 148672, 119692, -4836, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 148646, 120181, -4845, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 150710, 115259, -5460, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 151003, 116036, -5472, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 150399, 114153, -5482, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 153882, 112038, -5500, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 153950, 111359, -5507, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 151252, 111021, -5506, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 150332, 112431, -5508, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 153607, 109613, -5138, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 153155, 109279, -5146, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 153256, 107465, -5135, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 147050, 112637, -3694, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 148361, 111779, -3703, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 148513, 112384, -3720, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 151772, 120608, -3784, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 152103, 121356, -3792, 0, false, 0, false, world.instanceId);
        addSpawn(95127, 142079, 118706, -3896, 0, false, 0, false, world.instanceId);
		addSpawn(95127, 140195, 110468, -3944, 0, false, 0, false, world.instanceId);
		addSpawn(95127, 142426, 113530, -3700, 0, false, 0, false, world.instanceId);
        //Magmacoil end
        // Horror knight start
        addSpawn(95128, 144600, 118657, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95128, 149774, 121663, -4862, 0, false, 0, false, world.instanceId);
        addSpawn(95128, 149211, 119985, -4862, 0, false, 0, false, world.instanceId);
        addSpawn(95128, 148460, 120706, -4862, 0, false, 0, false, world.instanceId);
        addSpawn(95128, 148115, 121234, -4783, 0, false, 0, false, world.instanceId);
		addSpawn(95128, 140894, 109656, -3944, 0, false, 0, false, world.instanceId);
		addSpawn(95128, 147510, 109895, -3944, 0, false, 0, false, world.instanceId);
        // Horror knight end
        // Bone Fiend Start
        addSpawn(95129, 146570, 108623, -3597, 0, false, 0, false, world.instanceId);
        addSpawn(95129, 149858, 116547, -3701, 0, false, 0, false, world.instanceId);
        addSpawn(95129, 150130, 117108, -3693, 0, false, 0, false, world.instanceId);
        // Bone Fiend end
        // Hemagorgon start
        addSpawn(95130, 147648, 112271, -3720, 0, false, 0, false, world.instanceId);
        // Hemagorgon end
        // Puker start
        addSpawn(95132, 140830, 120692, -3898, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 144266, 120351, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 143487, 120573, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 150676, 110752, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 150792, 111213, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 150340, 111368, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 150060, 111783, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 150455, 112079, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 150846, 111744, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 150759, 112266, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 153111, 119234, -3787, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 152316, 120489, -3804, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 152879, 110570, -5520, 0, false, 0, false, world.instanceId);
        addSpawn(95132, 153356, 111380, -5520, 0, false, 0, false, world.instanceId);
		addSpawn(95132, 140172, 109894, -3933, 0, false, 0, false, world.instanceId);
		addSpawn(95132, 141065, 110318, -3944, 0, false, 0, false, world.instanceId);
		addSpawn(95132, 144287, 107877, -3944, 0, false, 0, false, world.instanceId);
		addSpawn(95132, 144422, 107407, -3944, 0, false, 0, false, world.instanceId);
		addSpawn(95132, 141065, 110318, -3944, 0, false, 0, false, world.instanceId);
		addSpawn(95132, 144822, 107999, -3944, 0, false, 0, false, world.instanceId);
		addSpawn(95132, 144718, 108362, -3935, 0, false, 0, false, world.instanceId);
		addSpawn(95132, 145353, 108463, -3944, 0, false, 0, false, world.instanceId);
        // Puker end
        // Stag beast start
        addSpawn(95133, 145563, 120527, -3912, 0, false, 0, false, world.instanceId);
        // Stag BEASST END
        //Oblivion knight start
        addSpawn(95134, 144844, 117144, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95134, 145182, 118564, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95134, 149826, 121099, -4862, 0, false, 0, false, world.instanceId);
        addSpawn(95134, 148930, 121367, -4862, 0, false, 0, false, world.instanceId);
        addSpawn(95134, 149402, 120437, -4862, 0, false, 0, false, world.instanceId);
        addSpawn(95134, 154464, 111748, -5520, 0, false, 0, false, world.instanceId);
		addSpawn(95134, 142519, 109995, -3944, 0, false, 0, false, world.instanceId);
		addSpawn(95134, 141997, 109410, -3944, 0, false, 0, false, world.instanceId);
		addSpawn(95134, 141176, 109005, -3941, 0, false, 0, false, world.instanceId);
		addSpawn(95134, 140860, 107261, -3944, 0, false, 0, false, world.instanceId);
		addSpawn(95134, 147963, 110409, -3944, 0, false, 0, false, world.instanceId);
		addSpawn(95134, 148311, 109413, -3944, 0, false, 0, false, world.instanceId);
		//Oblivion knight end
        // Lost Warden Start
        addSpawn(95135, 141457, 121707, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95135, 141526, 121268, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95135, 149507, 109340, -5203, 0, false, 0, false, world.instanceId);
        addSpawn(95135, 149755, 109367, -5181, 0, false, 0, false, world.instanceId);
        addSpawn(95135, 149321, 109713, -5224, 0, false, 0, false, world.instanceId);
        addSpawn(95135, 149801, 109804, -5212, 0, false, 0, false, world.instanceId);
        addSpawn(95135, 154825, 108393, -5152, 0, false, 0, false, world.instanceId);
        addSpawn(95135, 154795, 108703, -5152, 0, false, 0, false, world.instanceId);
        addSpawn(95135, 151388, 107462, -4888, 0, false, 0, false, world.instanceId);
        addSpawn(95135, 151085, 107267, -4835, 0, false, 0, false, world.instanceId);
        addSpawn(95135, 146503, 109192, -3459, 0, false, 0, false, world.instanceId);
        addSpawn(95135, 146495, 109576, -3422, 0, false, 0, false, world.instanceId);
        // Lost Warden End.
        // Mutation start
        addSpawn(95136, 152578, 109348, -5152, 0, false, 0, false, world.instanceId);
        addSpawn(95136, 153163, 108687, -5130, 0, false, 0, false, world.instanceId);
        addSpawn(95136, 152529, 119245, -3787, 0, false, 0, false, world.instanceId);
        addSpawn(95136, 151353, 118242, -3832, 0, false, 0, false, world.instanceId);
        addSpawn(95136, 153614, 118739, -3804, 0, false, 0, false, world.instanceId);
        // Mutation end.
        // Argekunte start
        addSpawn(95622, 145099, 117766, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95622, 151773, 107518, -4956, 0, false, 0, false, world.instanceId);
        // Argekunte end.
        // Gargantuan Start
        addSpawn(95623, 148634, 114937, -5472, 0, false, 0, false, world.instanceId);
        // Gargantuan End
	// Fullkard start
	addSpawn(95602, 142511, 107231, -3944, 0, false, 0, false, world.instanceId);
	// Fullkard end
        // Dragon battler Start
        addSpawn(95630, 148949, 115761, -3710, 0, false, 0, false, world.instanceId);
        // Dragon battler End
        // Rinma
        addSpawn(95631, 154261, 118855, -3804, 0, false, 0, false, world.instanceId);
        // Rinma
        // Del mars
        addSpawn(95632, 145900, 119351, -3912, 0, false, 0, false, world.instanceId);
        // Del Mars
        // Darion
        addSpawn(95634, 154395, 121229, -3804, 0, false, 0, false, world.instanceId);
        // Darion
        // Forsaken Warlord Start
        addSpawn(95638, 142832, 120195, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95638, 142789, 119966, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95638, 142588, 119903, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95638, 143016, 119977, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95638, 142788, 119830, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95638, 143210, 120082, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95638, 143017, 120411, -3912, 0, false, 0, false, world.instanceId);
        addSpawn(95638, 142734, 120336, -3912, 0, false, 0, false, world.instanceId);
        // Forsaken Warlord end.
        // Quad end
        addSpawn(95644, 146361, 111172, -3564, 0, false, 0, false, world.instanceId);
        // Quad end
        // Nosferatu
        addSpawn(95646, 152301, 110459, -5520, 0, false, 0, false, world.instanceId);
        // Nosferatu
	// Harken start
	addSpawn(95674, 144461, 112319, -3933, 0, false, 0, false, world.instanceId);
	// Harken end
	// Aperion start
	addSpawn(95643, 143100, 108710, -3943, 0, false, 0, false, world.instanceId);
	addSpawn(95643, 140562, 108080, -3948, 0, false, 0, false, world.instanceId);
	// Aperion end
	// Zusamen start
	addSpawn(95607, 144012, 110320, -3923, 0, false, 0, false, world.instanceId);
	// Zusamen end
	// Apcs party start
	addSpawn(95683, 148162, 117799, -3715, 0, false, 0, false, world.instanceId);
	addSpawn(95684, 148275, 117870, -3707, 0, false, 0, false, world.instanceId);
	addSpawn(95685, 148213, 117798, -3707, 0, false, 0, false, world.instanceId);
	addSpawn(95686, 148048, 117801, -3707, 0, false, 0, false, world.instanceId);
	addSpawn(95687, 148201, 117948, -3712, 0, false, 0, false, world.instanceId);
	addSpawn(95688, 148167, 117882, -3707, 0, false, 0, false, world.instanceId);
	// Apcs party end
}

protected void exitInstance(L2PcInstance player, teleCoord tele)
{
	player.setInstanceId(0);
	player.teleToLocation(tele.x, tele.y, tele.z);
	
	L2Summon pet = player.getPet();
	if (pet != null)
	{
		pet.setInstanceId(0);
		pet.teleToLocation(tele.x, tele.y, tele.z);
	}
}

@Override
public String onTalk(L2Npc npc, L2PcInstance player)
{
	final int npcId = npc.getNpcId();
	
	QuestState st = player.getQuestState(qn);
	
	if (st == null)
		st = newQuestState(player);
	
	if (npcId == MALKION)
	{
		teleCoord teleto = new teleCoord();
		teleto.x = 131175;
		teleto.y = 114415;
		teleto.z = -3715;
		enterInstance(player, "DVC.xml", teleto);
	}
	else if (npcId == EXIT_TELEPORTER)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		
		if (world == null || !(world instanceof dvcWorld))
			return null;
		
		teleCoord teleto = new teleCoord();
		teleto.x = -82993;
		teleto.y = 150860;
		teleto.z = -3129;
		
		if (player.getParty() == null)
		{
			exitInstance(player, teleto);
			int instanceId = npc.getInstanceId();
			Instance instance = InstanceManager.getInstance().getInstance(instanceId);
			if (instance.getPlayers().isEmpty())
			{
				InstanceManager.getInstance().destroyInstance(instanceId);
			}
			player.sendPacket(new ExShowScreenMessage("You have completed the DVC instance", 6000));
		}
		else
		{
			for (L2PcInstance ptm : player.getParty().getPartyMembers())
			{
				exitInstance(ptm, teleto);
				int instanceId = npc.getInstanceId();
				Instance instance = InstanceManager.getInstance().getInstance(instanceId);
				if (instance.getPlayers().isEmpty())
				{
					InstanceManager.getInstance().destroyInstance(instanceId);
				}
				player.sendPacket(new ExShowScreenMessage("You have completed the DVC instance", 6000));
			}
		}

		int instanceId = npc.getInstanceId();
		Instance instance = InstanceManager.getInstance().getInstance(instanceId);
		if (instance.getPlayers().isEmpty())
		{
			InstanceManager.getInstance().destroyInstance(instanceId);
		}
		st.exitQuest(true);
	}
	
	return null;
}
@Override
public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
{
	return null;
}
public void spawnExitGK(dvcWorld world, L2PcInstance player)
{
	addSpawn(EXIT_TELEPORTER, 153073, 122107, -3805, 0, false, 0, false, world.instanceId);
}
}