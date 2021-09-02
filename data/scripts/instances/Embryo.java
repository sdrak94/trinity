package instances;

import net.sf.l2j.Config;
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

public class Embryo extends Quest
{
	
    //NPCs
    private static int DEVICE = 60006;
    private static int TELEPORTER = 60007;
    private static int TELEPORTER2 = 60008;


    //stronger MOBS
    private static final int[] MOBS = {95634, 95124, 110009, 110015, 110018, 110019, 110020, 110022, 110026, 110027, 110029, 110032, 110033, 110034, 110048, 110049, 110051, 110052, 110053, 110054, 110058, 110059, 110060, 110062, 110068, 110069, 110072, 110073, 110074, 110075, 110076, 110078, 110080, 110083, 110091, 110093, 110094, 110095, 110151, 1000040, 1000041, 1000042, 1000043, 1000044};

    private static String qn = "Embryo";
    private static final int INSTANCEID = 2001;

    private boolean debug = true;
    private static int levelReq = Config.EMBRYO_LEVELS;
    private static int pvpReq = Config.EMBRYO_PVPS; //Previously 50
    private static int healerPvpReq = Config.EMBRYO_SUPPORT_PVPS;
//    private static float gearLvlReq = 50;
/*
	//coords
	private static int[] INITIAL_SPAWN_POINT = {-76435, -185543, -11003};
	private static int[] BOSS_ROOM_SPAWN_POINT = {-55580, -219857, -8117};*/

    private class teleCoord
    {
    	int instanceId; int x; int y; int z;
    }

    public class EmbryoWorld extends InstanceWorld
    {
        private int stage = 0;
        private int liveMobs = 0;

        public void incStage()
        {
            stage++;
        }

        public int getStage()
        {
            return stage;
        }

        public void incLiveMobs()
        {
            liveMobs++;
        }

        public void decLiveMobs()
        {
            liveMobs--;

            if (liveMobs < 0)
            {
                _log.warning("Error: Embryo mobs went into negative. ");
            }
        }

        public int getLiveMobs()
        {
            return liveMobs;
        }

        public EmbryoWorld()
        {
            InstanceManager.getInstance().super();
        }
    }

    public Embryo(int questId, String name, String descr)
    {
    	
        super(questId, name, descr);

        addStartNpc(DEVICE);
        addTalkId(DEVICE);
        addTalkId(TELEPORTER);
        addTalkId(TELEPORTER2);


        for (int mob : MOBS)
            addKillId(mob);
    }

    public static void main(String[] args)
    {
        new Embryo(-1, qn, "instances");
    }
    
    public static void exec()
    {
    	new Embryo(-1, qn, "instances");
    }
    
    private boolean checkConditions(L2PcInstance player, boolean single)
    {
        if (debug || player.isGM())
            return true;
        else
        {
            final L2Party party = player.getParty();

            if (!single && party != null)
            {
                if (party.getMemberCount() < 8)
                {
                    player.sendMessage("This is a 8 player instance; you cannot enter with a party size < 8 people");
                    return true;
                }

                if (player.getObjectId() != party.getPartyLeaderOID())
                {
                    player.sendPacket(new SystemMessage(2185));
                    return false;
                }


                boolean canEnter = true;

                for (L2PcInstance ptm : party.getPartyMembers())
                {
                    if (ptm == null) return false;

                    if (System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(ptm.getAccountName(), INSTANCEID))
                    {
                        ptm.sendMessage("You have cooldown for this instance.");
                        canEnter = false;
                    }
                    else if (ptm.getLevel() < levelReq)
                    {
                        ptm.sendMessage("You must be level "+levelReq+" to enter this instance");
                        canEnter = false;
                    }
                    else if (!ptm.isSurferLee() && ptm.getPvpKills() < pvpReq)
                    {
                        ptm.sendMessage("You must have "+pvpReq+" PvPs to enter this instance");
                        canEnter = false;
                    }
                    else if (ptm.isSurferLee() && ptm.getPvpKills() < healerPvpReq)
                    {
                        ptm.sendMessage("Support classes must have "+healerPvpReq+" PvPs to enter this instance");
                        canEnter = false;
                    }/*
                    else if (ptm.getGearLevel() < gearLvlReq)
                    {
                        ptm.sendMessage("Your gear level should be equal or more than "+gearLvlReq+" in order to enter this instance");
                        canEnter = false;
                    }*/
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
                    else if (!ptm.isInsideRadius(player, 500, true, false))
                    {
                        ptm.sendMessage("One of your party members is too far away");
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
                if (!single)
                {
                    player.sendMessage("This is a 8 player instance; you cannot enter with a party size < 8 people");
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
        return;
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
            world = new EmbryoWorld();
            world.instanceId = instanceId;
            world.templateId = INSTANCEID;
            InstanceManager.getInstance().addWorld(world);
            _log.info("Embryo: new " + template + " Instance: " + instanceId + " created by player: " + player.getName());

            final L2Party party = player.getParty();

            if (party != null)
            {
                for (L2PcInstance ptm : party.getPartyMembers())
                {

    				QuestState st = player.getQuestState(qn);
    				
    				if (st == null)
    					st = newQuestState(player);
    				
    				if (ptm == null) continue;
    				
                    InstanceManager.getInstance().setInstanceTime(ptm.getAccountName(), INSTANCEID, getNextInstanceTime(TWODAYS));

                    // teleport players
                    teleto.instanceId = instanceId;
                    world.allowed.add(ptm.getObjectId());
                    auditInstances(ptm, template, instanceId);
                    teleportplayer(ptm,teleto);
                }
            }
            else
            {
                InstanceManager.getInstance().setInstanceTime(player.getAccountName(), INSTANCEID, getNextInstanceTime(TWODAYS));

                // teleport players
                teleto.instanceId = instanceId;
                world.allowed.add(player.getObjectId());
                auditInstances(player, template, instanceId);
                teleportplayer(player,teleto);
            }

            spawn1stMobs((EmbryoWorld) world, player);

            return instanceId;
        }
    }


    protected void exitInstance(L2PcInstance player, teleCoord tele)
    {
	final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
	InstanceManager.getInstance().destroyInstance(world.instanceId);
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

        if (st != null)
        {
	        if (npcId == DEVICE)
	        {
	            final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
	            final EmbryoWorld kamWorld = (EmbryoWorld)world;
	            
	        	if (kamWorld == null)
	        	{
		            teleCoord teleto = new teleCoord();
		            teleto.x = -249267;
		            teleto.y = 139724;
		            teleto.z = 1562;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}	
	        	else if (kamWorld.getStage() == 1)
	        	{
	                teleCoord teleto = new teleCoord();
	                teleto.x = -254632;
	                teleto.y = 143559;
	                teleto.z = 2071;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}	
	        	else if(kamWorld.getStage() == 2)
	        	{
	                teleCoord teleto = new teleCoord();
					teleto.x = -254649;
					teleto.y = 143507;
					teleto.z = 2074;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}
	        	else if(kamWorld.getStage() == 3)
	        	{
	                teleCoord teleto = new teleCoord();
	                teleto.x = -256947;
					teleto.y = 149633;
					teleto.z = 4120;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}
	        	else if(kamWorld.getStage() == 4)
	        	{
	                teleCoord teleto = new teleCoord();
					teleto.x = -235305;
					teleto.y = 149888;
					teleto.z = 3610;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}
	        	else if(kamWorld.getStage() == 5)
	        	{
	                teleCoord teleto = new teleCoord();
					teleto.x = -243035;
					teleto.y = 142583;
					teleto.z = 2083;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}
	        	else if(kamWorld.getStage() == 6)
	        	{
	                teleCoord teleto = new teleCoord();
					teleto.x = -249350;
					teleto.y = 142947;
					teleto.z = 2595;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}
	        	else if(kamWorld.getStage() == 7)
	        	{
	                teleCoord teleto = new teleCoord();
					teleto.x = -248569;
					teleto.y = 145958;
					teleto.z = 3098;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}
	        	else if(kamWorld.getStage() == 8)
	        	{
	                teleCoord teleto = new teleCoord();
					teleto.x = -241523;
					teleto.y = 147231;
					teleto.z = 4634;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}
	        	else if(kamWorld.getStage() == 9)
	        	{
	                teleCoord teleto = new teleCoord();				
					teleto.x = -253970;
					teleto.y = 149849;
					teleto.z = 7194;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}
	        	else if(kamWorld.getStage() == 10)
	        	{
	                teleCoord teleto = new teleCoord();
					teleto.x = -241921;
					teleto.y = 154337;
					teleto.z = 5146;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}
	        	else if(kamWorld.getStage() == 11)
	        	{
	                teleCoord teleto = new teleCoord();
					teleto.x = -245756;
					teleto.y = 148048;
					teleto.z = 4666;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}
	        	else if(kamWorld.getStage() == 12)
	        	{
	                teleCoord teleto = new teleCoord();
					teleto.x = -245759;
					teleto.y = 148999;
					teleto.z = 11839;
		            enterInstance(player, "Embryo.xml", teleto);
	        	}
	        	
	        }
			else if (npcId == TELEPORTER)
			{
            final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);

            if (world == null || !(world instanceof EmbryoWorld))
                return null;

            final L2Party party = player.getParty();

            final EmbryoWorld kamWorld = (EmbryoWorld)world;

            if (kamWorld.getStage() == 0)
            {
                if (party != null)
                {
                    for (L2PcInstance ptm : party.getPartyMembers())
                    {
                        if (ptm == null) continue;
                        ptm.teleToLocation(-249267, 139724, 1562, false);
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                else
                {
                    player.teleToLocation(-249267, 139724, 1562, false);
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                npc.deleteMe();
                spawn1stMobs(kamWorld, player);
            }
            else if (kamWorld.getStage() == 1)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-254632, 143559, 2071, false);
                	player.sendServerMessage("-254632, 143559, 2071");
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-254632, 143559, 2071, false);
                    	player.sendServerMessage("-254632, 143559, 2071");
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else if (kamWorld.getStage() == 2)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-254649, 143507, 2074, false);
                	player.sendServerMessage("-254649, 143507, 2074");
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-254649, 143507, 2074, false);
                    	player.sendServerMessage("-254649, 143507, 2074");
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else if (kamWorld.getStage() == 3)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-256947, 149633, 4120, false);
                	player.sendServerMessage("-256947, 149633, 4120");
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-256947, 149633, 4120, false);
                    	player.sendServerMessage("-256947, 149633, 4120");
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else if (kamWorld.getStage() == 4)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-235305, 149888, 3610, false);
                	player.sendServerMessage("-235305, 149888, 3610");
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-235305, 149888, 3610, false);
                    	player.sendServerMessage("-235305, 149888, 3610");
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else if (kamWorld.getStage() == 5)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-243035, 142583, 2083, false);
                	player.sendServerMessage("-243035, 142583, 2083");
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-243035, 142583, 2083, false);
                    	player.sendServerMessage("-243035, 142583, 2083");
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else if (kamWorld.getStage() == 6)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-249350, 142947, 2595, false);
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-249350, 142947, 2595, false);
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else if (kamWorld.getStage() == 7)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-248569, 145958, 3098, false);
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-248569, 145958, 3098, false);
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else if (kamWorld.getStage() == 8)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-241523, 147231, 4634, false);
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-241523, 147231, 4634, false);
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else if (kamWorld.getStage() == 9)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-253970, 149849, 7194, false);
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-253970, 149849, 7194, false);
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else if (kamWorld.getStage() == 10)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-241921, 154337, 5146, false);
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-241921, 154337, 5146, false);
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else if (kamWorld.getStage() == 11)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-245756, 148048, 4666, false);
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-245756, 148048, 4666, false);
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else if (kamWorld.getStage() == 12)
            {

                if (/*!player.isGM() || */player.getParty() == null)
                {
                   // exitInstance(player, teleto);
                	player.teleToLocation(-245759, 148999, 11839, false);
                    player.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                }
                else
                {
                    for (L2PcInstance ptm : player.getParty().getPartyMembers())
                    {
                    	ptm.teleToLocation(-245759, 148999, 11839, false);
                        ptm.sendPacket(new ExShowScreenMessage("Stage: "+String.valueOf(kamWorld.stage), 6000));
                    }
                }
                npc.deleteMe();
                //st.exitQuest(true);
            }
            else
            {
                _log.warning("LOL wtf kamworld stage is fucked up!");
            }
        }
			else if (npcId == TELEPORTER2)
			{
            final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);

            if (world == null || !(world instanceof EmbryoWorld))
                return null;

            final EmbryoWorld kamWorld = (EmbryoWorld)world;

            if (kamWorld.getStage() == 12)
            {
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
        			player.sendPacket(new ExShowScreenMessage("You have completed the Embryo instance", 6000));
					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
					{
						player.getCounters().embryoDone++;
					}
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
        				ptm.sendPacket(new ExShowScreenMessage("You have completed the Embryo instance", 6000));
    					if (Config.L2JMOD_ACHIEVEMENT_SYSTEM)
    					{
    						ptm.getCounters().embryoDone++;
    					}
        			}
        		}
            }
            else
            {
                _log.warning("LOL wtf kamworld stage is fucked up!");
            }

            st.exitQuest(true);
        }

		}

        return null;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
    {
        final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(killer);

        if (world == null || !(world instanceof EmbryoWorld))
            return null;

        final EmbryoWorld kamWorld = (EmbryoWorld)world;

        kamWorld.decLiveMobs();

    	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(kamWorld.liveMobs));
    	
        final L2Party party = killer.getParty();

        if (party != null)
        {
            for (L2PcInstance ptm : party.getPartyMembers())
            {
                if (ptm == null) continue;
                ptm.sendPacket(new ExShowScreenMessage(1, -1, 6, 0, 1, 0, 0, true, 12000, 1, "Stage:"+String.valueOf(kamWorld.stage)+" Mobs: "+String.valueOf(kamWorld.liveMobs)));
            }
        }
        else
        {
        	killer.sendPacket(new ExShowScreenMessage(1, -1, 6, 0, 1, 0, 0, true, 12000, 1, "Stage:"+String.valueOf(kamWorld.stage)+" Mobs: "+String.valueOf(kamWorld.liveMobs)));
        }
        
        if (kamWorld.getLiveMobs() <= 0)
        {
            kamWorld.liveMobs = 0;

            final int stage = kamWorld.getStage();

            switch (stage)
            {
                case 0: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Stage0: " + String.valueOf(stage));
                    spawn1stMobs(kamWorld, killer);
                    spawnGK1(kamWorld, killer);
                    break;
                case 1: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 1: STAGE: " + String.valueOf(stage));
                    spawn2ndMobs(kamWorld, killer);
                    spawnGK1(kamWorld, killer);
                    break;
                case 2: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 2: STAGE: " + String.valueOf(stage));
                    spawn3rdMobs(kamWorld, killer);
                    spawnGK2(kamWorld, killer);
                    break;
                case 3: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 3: STAGE: " + String.valueOf(stage));
                    spawn4thMobs(kamWorld, killer);
                    spawnGK3(kamWorld, killer);
                    break;
                case 4: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 4: STAGE: " + String.valueOf(stage));
                    spawn5thMobs(kamWorld, killer);
                    spawnGK4(kamWorld, killer);
                    break;
                case 5: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 5: STAGE: " + String.valueOf(stage));
                    spawn6thMobs(kamWorld, killer);
                    spawnGK5(kamWorld, killer);
                    break;
                case 6: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 5: STAGE: " + String.valueOf(stage));
                    spawn7thMobs(kamWorld, killer);
                    spawnGK6(kamWorld, killer);
                    break;
                case 7: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 5: STAGE: " + String.valueOf(stage));
                    spawn8thMobs(kamWorld, killer);
                    spawnGK7(kamWorld, killer);
                    break;
                case 8: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 5: STAGE: " + String.valueOf(stage));
                    spawn9thMobs(kamWorld, killer);
                    spawnGK8(kamWorld, killer);
                    break;
                case 9: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 5: STAGE: " + String.valueOf(stage));
                    spawn10thMobs(kamWorld, killer);
                    spawnGK9(kamWorld, killer);
                    break;
                case 10: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 5: STAGE: " + String.valueOf(stage));
                    spawn11thMobs(kamWorld, killer);
                    spawnGK10(kamWorld, killer);
                    break;
                case 11: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 5: STAGE: " + String.valueOf(stage));
                    spawn12thMobs(kamWorld, killer);
                    spawnGK11(kamWorld, killer);
                    break;
                case 12: //shouldn't happen
                	//Announcements.getInstance().announceToAll("Case 5: STAGE: " + String.valueOf(stage));
                    //spawn12thMobs(kamWorld, killer);
                    spawnGK12(kamWorld, killer);
                    break;
            }
        }

        return null;
    }

    protected int collectDamage(L2PcInstance player, String template)
    {

        final L2Party party = player.getParty();
    	party.getMemberCount();
    	
    	
		return 0;
	}   
    public void spawnGK1(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER, -252659, 141514, 1562, 58429, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawnGK2(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER, -256812, 148573, 2072, 51210, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawnGK3(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER, -255768, 155467, 4120, 44789, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawnGK4(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER, -235567, 152870, 3608, 50874, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawnGK5(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER, -247654, 142356, 2072, 64134, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawnGK6(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER, -251945, 144845, 2584, 54788, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawnGK7(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER, -243918, 145497, 3096, 35467, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawnGK8(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER, -240542, 150170, 4632, 14603, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawnGK9(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER, -253050, 154273, 7192, 43370, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawnGK10(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER, -243223, 151740, 5144, 10557, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawnGK11(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER, -245764, 151570, 4664, 49151, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawnGK12(EmbryoWorld world, L2PcInstance player)
	{
    	addSpawn(TELEPORTER2, -245759, 149060, 11840, 16257, false, 0, false, world.instanceId); //Floor Teleporter
	}
    public void spawn1stMobs(EmbryoWorld world, L2PcInstance player)//not used
    {
        if (world.getStage() == 0)
        {
            addSpawn(110053, -249425, 140294, 1568, 57343, false, 0, false, world.instanceId); //Underground Knight
            world.incLiveMobs();
            addSpawn(110053, -250239, 139553, 1568, 57343, false, 0, false, world.instanceId); //Underground Knight
            world.incLiveMobs();
            addSpawn(110053, -250249, 140666, 1568, 57343, false, 0, false, world.instanceId); //Underground Knight
            world.incLiveMobs();
            addSpawn(110053, -251820, 140341, 1568, 57343, false, 0, false, world.instanceId); //Underground Knight
            world.incLiveMobs();
            addSpawn(110053, -251685, 141507, 1568, 57343, false, 0, false, world.instanceId); //Underground Knight
            world.incLiveMobs();
            addSpawn(110053, -252725, 140945, 1568, 57343, false, 0, false, world.instanceId); //Underground Knight
            world.incLiveMobs();

            addSpawn(110054, -249787, 139373, 1568, 57343, false, 0, false, world.instanceId); //Nedawn
            world.incLiveMobs();
            addSpawn(110054, -249871, 140499, 1568, 57343, false, 0, false, world.instanceId); //Nedawn
            world.incLiveMobs();
            addSpawn(110054, -250734, 139811, 1559, 57343, false, 0, false, world.instanceId); //Nedawn
            world.incLiveMobs();
            addSpawn(110054, -251298, 141199, 1568, 57343, false, 0, false, world.instanceId); //Nedawn
            world.incLiveMobs();
            addSpawn(110054, -252295, 140645, 1568, 57343, false, 0, false, world.instanceId); //Nedawn
            world.incLiveMobs();
            addSpawn(110054, -252124, 141790, 1568, 57343, false, 0, false, world.instanceId); //Nedawn
            world.incLiveMobs();

            addSpawn(110078, -249844, 139966, 1568, 57343, false, 0, false, world.instanceId); //Gladront
            world.incLiveMobs();
            addSpawn(110078, -252185, 141209, 1568, 57343, false, 0, false, world.instanceId); //Gladront
            world.incLiveMobs();

            addSpawn(110020, -250292, 140177, 1568, 57343, false, 0, false, world.instanceId); //Winged Terror
            world.incLiveMobs();
            addSpawn(110020, -251807, 140966, 1568, 57343, false, 0, false, world.instanceId); //Winged Terror
            world.incLiveMobs();


            addSpawn(110058, -251031, 140473, 1568, 57343, false, 0, false, world.instanceId); //Mormo
            world.incLiveMobs();

            addSpawn(110009, -251306, 140023, 1568, 57343, false, 0, false, world.instanceId); //Blazed Assassin
            world.incLiveMobs();
            addSpawn(110009, -250830, 140923, 1568, 57343, false, 0, false, world.instanceId); //Blazed Assassin
            world.incLiveMobs();

            world.incStage();
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
    public void spawn2ndMobs(EmbryoWorld world, L2PcInstance player)
    {
        if (world.getStage() == 1)
        {
	        addSpawn(110078, -255191, 143422, 2072, 55468, false, 0, false, world.instanceId); //Gladront
	        world.incLiveMobs();
	        addSpawn(110078, -255737, 144215, 2072, 54541, false, 0, false, world.instanceId); //Gladront
	        world.incLiveMobs();
	        addSpawn(110078, -256568, 145916, 2072, 51326, false, 0, false, world.instanceId); //Gladront
	        world.incLiveMobs();
	        addSpawn(110078, -255406, 145817, 2072, 52708, false, 0, false, world.instanceId); //Gladront
	        world.incLiveMobs();
	        addSpawn(110078, -255817, 146788, 2072, 52844, false, 0, false, world.instanceId); //Gladront
	        world.incLiveMobs();
	        addSpawn(110078, -254657, 144439, 2072, 54788, false, 0, false, world.instanceId); //Gladront
	        world.incLiveMobs();
	        addSpawn(110009, -255509, 143842, 2072, 53375, false, 0, false, world.instanceId); //Blazed Assassin
	        world.incLiveMobs();
	        addSpawn(110009, -256360, 145402, 2072, 51468, false, 0, false, world.instanceId); //Blazed Assassin
	        world.incLiveMobs();
	        addSpawn(110009, -256788, 146393, 2072, 52584, false, 0, false, world.instanceId); //Blazed Assassin
	        world.incLiveMobs();
	        addSpawn(110009, -256670, 147885, 2072, 51746, false, 0, false, world.instanceId); //Blazed Assassin
	        world.incLiveMobs();
	        addSpawn(110009, -254374, 144056, 2072, 53308, false, 0, false, world.instanceId); //Blazed Assassin
	        world.incLiveMobs();
	        addSpawn(110009, -254871, 144774, 2072, 54303, false, 0, false, world.instanceId); //Blazed Assassin
	        world.incLiveMobs();
	        addSpawn(110009, -255608, 146330, 2072, 55285, false, 0, false, world.instanceId); //Blazed Assassin
	        world.incLiveMobs();
	        addSpawn(110020, -256130, 144836, 2072, 4003, false, 0, false, world.instanceId); //Winged Terror
	        world.incLiveMobs();
	        addSpawn(110020, -257022, 146979, 2072, 2280, false, 0, false, world.instanceId); //Winged Terror
	        world.incLiveMobs();
	        addSpawn(110020, -255226, 145322, 2072, 37468, false, 0, false, world.instanceId); //Winged Terror
	        world.incLiveMobs();
	        addSpawn(110020, -256009, 147281, 2072, 35133, false, 0, false, world.instanceId); //Winged Terror
	        world.incLiveMobs();
	        addSpawn(110053, -257150, 147561, 2072, 49458, false, 0, false, world.instanceId); //Underground Knight
	        world.incLiveMobs();
	        addSpawn(110053, -256027, 145942, 2072, 52894, false, 0, false, world.instanceId); //Underground Knight
	        world.incLiveMobs();
	        addSpawn(110053, -255205, 144382, 2072, 55595, false, 0, false, world.instanceId); //Underground Knight
	        world.incLiveMobs();
	        addSpawn(110053, -256238, 148287, 2072, 47429, false, 0, false, world.instanceId); //Underground Knight
	        world.incLiveMobs();
	        addSpawn(110054, -257251, 148125, 2072, 52311, false, 0, false, world.instanceId); //Nedawn
	        world.incLiveMobs();
	        addSpawn(110054, -256179, 146300, 2072, 55097, false, 0, false, world.instanceId); //Nedawn
	        world.incLiveMobs();
	        addSpawn(110054, -254952, 144003, 2072, 54240, false, 0, false, world.instanceId); //Nedawn
	        world.incLiveMobs();
	        addSpawn(110054, -256154, 147816, 2072, 48723, false, 0, false, world.instanceId); //Nedawn
	        world.incLiveMobs();
	        addSpawn(110073, -256516, 147138, 2072, 50449, false, 0, false, world.instanceId); //Jungle Fam
	        world.incLiveMobs();
	        addSpawn(110059, -255655, 145065, 2072, 53691, false, 0, false, world.instanceId); //Termo
	        world.incLiveMobs();

            world.incStage();
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
    public void spawn3rdMobs(EmbryoWorld world, L2PcInstance player)
    {
        if (world.getStage() == 2)
        {
        	addSpawn(110051, -256458, 149897, 4120, 47133, false, 0, false, world.instanceId); //Mictian Yez
        	world.incLiveMobs();
        	addSpawn(110051, -256391, 150847, 4120, 45938, false, 0, false, world.instanceId); //Mictian Yez
        	world.incLiveMobs();
        	addSpawn(110051, -256016, 154874, 4120, 44241, false, 0, false, world.instanceId); //Mictian Yez
        	world.incLiveMobs();
        	addSpawn(110051, -256693, 152813, 4120, 46596, false, 0, false, world.instanceId); //Mictian Yez
        	world.incLiveMobs();
        	addSpawn(110051, -257486, 150426, 4120, 48351, false, 0, false, world.instanceId); //Mictian Yez
        	world.incLiveMobs();
        	addSpawn(110052, -256419, 150373, 4120, 47886, false, 0, false, world.instanceId); //Queen's Guard
        	world.incLiveMobs();
        	addSpawn(110052, -256786, 152357, 4120, 46093, false, 0, false, world.instanceId); //Queen's Guard
        	world.incLiveMobs();
        	addSpawn(110052, -257479, 149893, 4120, 48046, false, 0, false, world.instanceId); //Queen's Guard
        	world.incLiveMobs();
        	addSpawn(110052, -256393, 155382, 4120, 43094, false, 0, false, world.instanceId); //Queen's Guard
        	world.incLiveMobs();

        	addSpawn(110060, -256448, 151429, 4120, 31415, false, 0, false, world.instanceId); //Slither Set
        	world.incLiveMobs();
        	addSpawn(110060, -256335, 151944, 4120, 46871, false, 0, false, world.instanceId); //Slither Set
        	world.incLiveMobs();
        	addSpawn(110060, -256117, 152939, 4120, 45322, false, 0, false, world.instanceId); //Slither Set
        	world.incLiveMobs();
        	addSpawn(110060, -257253, 152673, 4120, 47364, false, 0, false, world.instanceId); //Slither Set
        	world.incLiveMobs();
        	addSpawn(110060, -257484, 151539, 4120, 63477, false, 0, false, world.instanceId); //Slither Set
        	world.incLiveMobs();

        	addSpawn(110034, -256206, 152451, 4120, 47585, false, 0, false, world.instanceId); //Wild Xercat
        	world.incLiveMobs();
        	addSpawn(110034, -256948, 150143, 4120, 49400, false, 0, false, world.instanceId); //Wild Xercat
        	world.incLiveMobs();
        	addSpawn(110034, -257345, 152149, 4120, 47085, false, 0, false, world.instanceId); //Wild Xercat
        	world.incLiveMobs();

        	addSpawn(110073, -256003, 153501, 4120, 29513, false, 0, false, world.instanceId); //Jungle Fam
        	world.incLiveMobs();
        	addSpawn(110073, -257021, 153808, 4120, 63079, false, 0, false, world.instanceId); //Jungle Fam
        	world.incLiveMobs();

        	addSpawn(110053, -255810, 154018, 4120, 45916, false, 0, false, world.instanceId); //Underground Knight
        	world.incLiveMobs();
        	addSpawn(110076, -255641, 154460, 4120, 41136, false, 0, false, world.instanceId); //Saxon Namaah
        	world.incLiveMobs();
        	addSpawn(110076, -256954, 151491, 4120, 48112, false, 0, false, world.instanceId); //Saxon Namaah
        	world.incLiveMobs();

        	addSpawn(110068, -256955, 150676, 4120, 48375, false, 0, false, world.instanceId); //Rusty Rico
        	world.incLiveMobs();
        	addSpawn(110068, -257170, 153167, 4120, 48161, false, 0, false, world.instanceId); //Rusty Rico
        	world.incLiveMobs();

        	addSpawn(110054, -255431, 154914, 4120, 43943, false, 0, false, world.instanceId); //Nedawn
        	world.incLiveMobs();
        	addSpawn(110059, -256156, 154534, 4120, 44460, false, 0, false, world.instanceId); //Termo
        	world.incLiveMobs();
        	addSpawn(110049, -256499, 153666, 4120, 46999, false, 0, false, world.instanceId); //Fenrimor
        	world.incLiveMobs();
        	addSpawn(110020, -256788, 154395, 4120, 44992, false, 0, false, world.instanceId); //Winged Terror
        	world.incLiveMobs();
        	addSpawn(110009, -256587, 154872, 4120, 45365, false, 0, false, world.instanceId); //Blazed Assassin
        	world.incLiveMobs();

            world.incStage();
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
    public void spawn4thMobs(EmbryoWorld world, L2PcInstance player)
    {
        if (world.getStage() == 3)
        {
        	addSpawn(110074, -234641, 150939, 3608, 33579, false, 0, false, world.instanceId); //Shelldo
        	world.incLiveMobs();
        	addSpawn(110074, -235884, 150873, 3608, 65188, false, 0, false, world.instanceId); //Shelldo
        	world.incLiveMobs();
        	addSpawn(110083, -234552, 151470, 3608, 45077, false, 0, false, world.instanceId); //Grudon
        	world.incLiveMobs();
        	addSpawn(110083, -235987, 151347, 3608, 49151, false, 0, false, world.instanceId); //Grudon
        	world.incLiveMobs();
        	addSpawn(110073, -234423, 152114, 3608, 53529, false, 0, false, world.instanceId); //Jungle Fam
        	world.incLiveMobs();
        	addSpawn(110073, -235121, 152029, 3608, 49715, false, 0, false, world.instanceId); //Jungle Fam
        	world.incLiveMobs();
        	addSpawn(110073, -235752, 151897, 3608, 49995, false, 0, false, world.instanceId); //Jungle Fam
        	world.incLiveMobs();
        	addSpawn(110073, -236342, 151757, 3608, 49895, false, 0, false, world.instanceId); //Jungle Fam
        	world.incLiveMobs();
        	addSpawn(110072, -234817, 152628, 3608, 51905, false, 0, false, world.instanceId); //Simian Pazzo
        	world.incLiveMobs();
        	addSpawn(110072, -236195, 152396, 3608, 50553, false, 0, false, world.instanceId); //Simian Pazzo
        	world.incLiveMobs();
        	addSpawn(110015, -235283, 151177, 3608, 49151, false, 0, false, world.instanceId); //Hell's Keeper
        	world.incLiveMobs();
        	addSpawn(110094, -235462, 152180, 3608, 51109, false, 0, false, world.instanceId); //Jeilriss Ryzzunty
        	world.incLiveMobs();

            world.incStage();
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
    public void spawn5thMobs(EmbryoWorld world, L2PcInstance player)
    {
        if (world.getStage() == 4)
        {
        	addSpawn(95124, -243663, 142885, 2072, 2246, false, 0, false, world.instanceId); //Disfigured One
        	world.incLiveMobs();
        	addSpawn(95124, -245070, 142164, 2072, 33276, false, 0, false, world.instanceId); //Disfigured One
        	world.incLiveMobs();
        	addSpawn(95124, -246337, 142165, 2072, 65014, false, 0, false, world.instanceId); //Disfigured One
        	world.incLiveMobs();
        	addSpawn(95124, -244669, 142685, 2072, 1297, false, 0, false, world.instanceId); //Disfigured One
        	world.incLiveMobs();
        	addSpawn(110022, -244161, 142803, 2072, 2232, false, 0, false, world.instanceId); //Terrifying Gardener
        	world.incLiveMobs();
        	addSpawn(110022, -243431, 142017, 2072, 3475, false, 0, false, world.instanceId); //Terrifying Gardener
        	world.incLiveMobs();
        	addSpawn(110022, -244527, 141794, 2072, 63976, false, 0, false, world.instanceId); //Terrifying Gardener
        	world.incLiveMobs();
        	addSpawn(110022, -245965, 142167, 2072, 56, false, 0, false, world.instanceId); //Terrifying Gardener
        	world.incLiveMobs();
        	addSpawn(110033, -245030, 142693, 2072, 49441, false, 0, false, world.instanceId); //Lizardude
        	world.incLiveMobs();
        	addSpawn(110033, -244898, 141654, 2072, 18387, false, 0, false, world.instanceId); //Lizardude
        	world.incLiveMobs();
        	addSpawn(110033, -246836, 141659, 2072, 63739, false, 0, false, world.instanceId); //Lizardude
        	world.incLiveMobs();
        	addSpawn(110062, -245566, 142735, 2072, 65229, false, 0, false, world.instanceId); //Majesty Slayer
        	world.incLiveMobs();
        	addSpawn(110062, -243697, 142412, 2072, 2289, false, 0, false, world.instanceId); //Majesty Slayer
        	world.incLiveMobs();
        	addSpawn(110062, -245987, 141616, 2072, 780, false, 0, false, world.instanceId); //Majesty Slayer
        	world.incLiveMobs();
        	addSpawn(110062, -246313, 142735, 2072, 64650, false, 0, false, world.instanceId); //Majesty Slayer
        	world.incLiveMobs();
        	addSpawn(110069, -245949, 142736, 2072, 65149, false, 0, false, world.instanceId); //Demongon
        	world.incLiveMobs();
        	addSpawn(110069, -244263, 142291, 2072, 967, false, 0, false, world.instanceId); //Demongon
        	world.incLiveMobs();
        	addSpawn(110069, -246415, 141611, 2072, 771, false, 0, false, world.instanceId); //Demongon
        	world.incLiveMobs();
        	addSpawn(110069, -245544, 141608, 2072, 297, false, 0, false, world.instanceId); //Demongon
        	world.incLiveMobs();
        	addSpawn(110075, -247074, 142845, 2072, 62113, false, 0, false, world.instanceId); //Abaddon's Guard
        	world.incLiveMobs();
        	addSpawn(110075, -244811, 142197, 2072, 496, false, 0, false, world.instanceId); //Abaddon's Guard
        	world.incLiveMobs();
        	addSpawn(110075, -247279, 141737, 2072, 64249, false, 0, false, world.instanceId); //Abaddon's Guard
        	world.incLiveMobs();
        	addSpawn(110048, -247079, 142276, 2072, 64265, false, 0, false, world.instanceId); //Flak Wyrr
        	world.incLiveMobs();
        	addSpawn(110048, -245574, 142162, 2072, 65250, false, 0, false, world.instanceId); //Flak Wyrr
        	world.incLiveMobs();
        	addSpawn(110032, -246693, 142768, 2072, 64856, false, 0, false, world.instanceId); //Tizjar
        	world.incLiveMobs();

            world.incStage(); //set the stage to 6 so teleporter will port to 6
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
    public void spawn6thMobs(EmbryoWorld world, L2PcInstance player)
    {
        if (world.getStage() == 5)
        {
        	addSpawn(110048, -249493, 143597, 2584, 43150, false, 0, false, world.instanceId); //Flak Wyrr
        	world.incLiveMobs();
	        addSpawn(110026, -249856, 143909, 2584, 59170, false, 0, false, world.instanceId); //King's Guard
        	world.incLiveMobs();
	        addSpawn(110032, -250172, 144130, 2584, 58498, false, 0, false, world.instanceId); //Tizjar
        	world.incLiveMobs();
	        addSpawn(110027, -250477, 144334, 2584, 59134, false, 0, false, world.instanceId); //Amon
        	world.incLiveMobs();
	        addSpawn(110019, -250773, 144579, 2584, 57556, false, 0, false, world.instanceId); //Underworld's Terror
        	world.incLiveMobs();
	        addSpawn(110026, -251037, 144841, 2584, 57499, false, 0, false, world.instanceId); //King's Guard
        	world.incLiveMobs();
	        addSpawn(110069, -251295, 145102, 2584, 57161, false, 0, false, world.instanceId); //Demongon
        	world.incLiveMobs();
	        addSpawn(110018, -250031, 143348, 2584, 59940, false, 0, false, world.instanceId); //Jeratrix
        	world.incLiveMobs();
	        addSpawn(110022, -250312, 143521, 2584, 58696, false, 0, false, world.instanceId); //Terrifying Gardener
        	world.incLiveMobs();
	        addSpawn(95124, -250565, 143680, 2584, 58871, false, 0, false, world.instanceId); //Disfigured One
        	world.incLiveMobs();
	        addSpawn(110026, -250809, 143212, 2584, 59727, false, 0, false, world.instanceId); //King's Guard
        	world.incLiveMobs();
	        addSpawn(110027, -250450, 142969, 2584, 58936, false, 0, false, world.instanceId); //Amon
        	world.incLiveMobs();
	        addSpawn(110026, -250091, 142733, 2584, 59790, false, 0, false, world.instanceId); //King's Guard
        	world.incLiveMobs();
	        addSpawn(110019, -250924, 143943, 2584, 57625, false, 0, false, world.instanceId); //Underworld's Terror
        	world.incLiveMobs();
	        addSpawn(110029, -251307, 143605, 2584, 8843, false, 0, false, world.instanceId); //Deformed Drake
        	world.incLiveMobs();
	        addSpawn(110075, -251065, 144074, 2584, 24333, false, 0, false, world.instanceId); //Abaddon's Guard
        	world.incLiveMobs();
	        addSpawn(110018, -251286, 144279, 2584, 57101, false, 0, false, world.instanceId); //Jeratrix
        	world.incLiveMobs();
	        addSpawn(110048, -251660, 144620, 2584, 58314, false, 0, false, world.instanceId); //Flak Wyrr
        	world.incLiveMobs();
	        addSpawn(110075, -252114, 144336, 2584, 57561, false, 0, false, world.instanceId); //Abaddon's Guard
        	world.incLiveMobs();
	        addSpawn(110062, -251806, 144033, 2584, 57466, false, 0, false, world.instanceId); //Majesty Slayer
        	world.incLiveMobs();
	        addSpawn(110018, -252524, 144819, 2584, 7497, false, 0, false, world.instanceId); //Jeratrix
        	world.incLiveMobs();
	        addSpawn(110027, -252918, 145319, 2584, 56398, false, 0, false, world.instanceId); //Amon
        	world.incLiveMobs();
	        addSpawn(110083, -253112, 145726, 2584, 59286, false, 0, false, world.instanceId); //Grudon
        	world.incLiveMobs();
	        addSpawn(110075, -252632, 145547, 2584, 56337, false, 0, false, world.instanceId); //Abaddon's Guard
        	world.incLiveMobs();

            world.incStage(); //set the stage to 6 so teleporter will port to 6
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
    public void spawn7thMobs(EmbryoWorld world, L2PcInstance player)
    {
        if (world.getStage() == 6)
        {
        	addSpawn(110062, -248656, 145372, 3096, 17976, false, 0, false, world.instanceId); //Majesty Slayer
        	world.incLiveMobs();
        	addSpawn(110027, -248327, 145187, 3096, 28991, false, 0, false, world.instanceId); //Amon
        	world.incLiveMobs();
        	addSpawn(110069, -247976, 145042, 3096, 26797, false, 0, false, world.instanceId); //Demongon
        	world.incLiveMobs();
        	addSpawn(95124, -248155, 145521, 3096, 28576, false, 0, false, world.instanceId); //Disfigured One
        	world.incLiveMobs();
        	addSpawn(110026, -247835, 145385, 3096, 27931, false, 0, false, world.instanceId); //King's Guard
        	world.incLiveMobs();
        	addSpawn(110018, -247643, 145086, 3096, 29412, false, 0, false, world.instanceId); //Jeratrix
        	world.incLiveMobs();
        	addSpawn(110029, -247477, 145427, 3096, 27931, false, 0, false, world.instanceId); //Deformed Drake
        	world.incLiveMobs();
        	addSpawn(110022, -247988, 145858, 3096, 27931, false, 0, false, world.instanceId); //Terrifying Gardener
        	world.incLiveMobs();
        	addSpawn(110075, -247685, 145733, 3096, 29197, false, 0, false, world.instanceId); //Abaddon's Guard
        	world.incLiveMobs();
        	addSpawn(110048, -247413, 145795, 3096, 30892, false, 0, false, world.instanceId); //Flak Wyrr
        	world.incLiveMobs();
        	addSpawn(110019, -248103, 146335, 3096, 35936, false, 0, false, world.instanceId); //Underworld's Terror
        	world.incLiveMobs();
        	addSpawn(110033, -247821, 146193, 3096, 28000, false, 0, false, world.instanceId); //Lizardude
        	world.incLiveMobs();
        	addSpawn(110032, -247550, 146079, 3096, 28343, false, 0, false, world.instanceId); //Tizjar
        	world.incLiveMobs();
        	addSpawn(110026, -247176, 145143, 3096, 29968, false, 0, false, world.instanceId); //King's Guard
        	world.incLiveMobs();
        	addSpawn(110062, -246822, 145076, 3096, 30445, false, 0, false, world.instanceId); //Majesty Slayer
        	world.incLiveMobs();
        	addSpawn(110069, -246468, 145009, 3096, 30873, false, 0, false, world.instanceId); //Demongon
        	world.incLiveMobs();
        	addSpawn(110048, -246117, 144970, 3096, 32420, false, 0, false, world.instanceId); //Flak Wyrr
        	world.incLiveMobs();
        	addSpawn(110033, -245761, 144972, 3096, 33070, false, 0, false, world.instanceId); //Lizardude
        	world.incLiveMobs();
        	addSpawn(110032, -245404, 144971, 3096, 31910, false, 0, false, world.instanceId); //Tizjar
        	world.incLiveMobs();
        	addSpawn(110069, -245053, 145004, 3096, 34878, false, 0, false, world.instanceId); //Demongon
        	world.incLiveMobs();
        	addSpawn(95124, -244712, 145073, 3096, 35216, false, 0, false, world.instanceId); //Disfigured One
        	world.incLiveMobs();
        	addSpawn(95124, -247073, 145506, 3096, 30744, false, 0, false, world.instanceId); //Disfigured One
        	world.incLiveMobs();
        	addSpawn(110022, -246761, 145444, 3096, 31013, false, 0, false, world.instanceId); //Terrifying Gardener
        	world.incLiveMobs();
        	addSpawn(110075, -246421, 145379, 3096, 30709, false, 0, false, world.instanceId); //Abaddon's Guard
        	world.incLiveMobs();
        	addSpawn(110027, -246099, 145346, 3096, 32136, false, 0, false, world.instanceId); //Amon
        	world.incLiveMobs();
        	addSpawn(110075, -245767, 145348, 3096, 32944, false, 0, false, world.instanceId); //Abaddon's Guard
        	world.incLiveMobs();
        	addSpawn(110026, -245424, 145345, 3096, 33093, false, 0, false, world.instanceId); //King's Guard
        	world.incLiveMobs();
        	addSpawn(110062, -245102, 145379, 3096, 36045, false, 0, false, world.instanceId); //Majesty Slayer
        	world.incLiveMobs();
        	addSpawn(110022, -244778, 145438, 3096, 36221, false, 0, false, world.instanceId); //Terrifying Gardener
        	world.incLiveMobs();
        	addSpawn(1000040, -244250, 145265, 3096, 36426, false, 0, false, world.instanceId); //Urthadar
        	world.incLiveMobs();
        	addSpawn(1000041, -244330, 145451, 3096, 34302, false, 0, false, world.instanceId); //Sonilak
        	world.incLiveMobs();

            world.incStage(); //set the stage to 6 so teleporter will port to 6
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
    public void spawn8thMobs(EmbryoWorld world, L2PcInstance player)
    {
        if (world.getStage() == 7)
        {
        	addSpawn(110018, -240420, 148159, 4632, 27728, false, 0, false, world.instanceId); //Jeratrix
        	world.incLiveMobs();
        	addSpawn(110026, -240766, 148298, 4632, 44741, false, 0, false, world.instanceId); //King's Guard
        	world.incLiveMobs();
        	addSpawn(110026, -241107, 148451, 4632, 44789, false, 0, false, world.instanceId); //King's Guard
        	world.incLiveMobs();
        	addSpawn(110018, -241442, 148600, 4632, 62545, false, 0, false, world.instanceId); //Jeratrix
        	world.incLiveMobs();
        	addSpawn(110048, -240519, 148969, 4632, 47971, false, 0, false, world.instanceId); //Flak Wyrr
        	world.incLiveMobs();
        	addSpawn(110048, -240885, 149070, 4632, 46898, false, 0, false, world.instanceId); //Flak Wyrr
        	world.incLiveMobs();
        	addSpawn(110091, -240631, 149750, 4632, 46800, false, 0, false, world.instanceId); //Nyldrig Moghum
        	world.incLiveMobs();
        	addSpawn(1000042, -240827, 148637, 4632, 45676, false, 0, false, world.instanceId); //Moghum
        	world.incLiveMobs();

            world.incStage(); //set the stage to 6 so teleporter will port to 6
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
    public void spawn9thMobs(EmbryoWorld world, L2PcInstance player)
    {
        if (world.getStage() == 8)
        {
        	addSpawn(95124, -253410, 150201, 7192, 49586, false, 0, false, world.instanceId); //Disfigured One
        	world.incLiveMobs();
        	addSpawn(110069, -253407, 150571, 7192, 48112, false, 0, false, world.instanceId); //Demongon
        	world.incLiveMobs();
        	addSpawn(110062, -253774, 150581, 7192, 49445, false, 0, false, world.instanceId); //Majesty Slayer
        	world.incLiveMobs();
        	addSpawn(110069, -254151, 150591, 7192, 48636, false, 0, false, world.instanceId); //Demongon
        	world.incLiveMobs();
        	addSpawn(110062, -254530, 150596, 7192, 49151, false, 0, false, world.instanceId); //Majesty Slayer
        	world.incLiveMobs();
        	addSpawn(110022, -254527, 150160, 7192, 50206, false, 0, false, world.instanceId); //Terrifying Gardener
        	world.incLiveMobs();
        	addSpawn(1000041, -253973, 150339, 7192, 48161, false, 0, false, world.instanceId); //Sonilak
        	world.incLiveMobs();
        	addSpawn(1000040, -253947, 151268, 7192, 47047, false, 0, false, world.instanceId); //Urthadar
        	world.incLiveMobs();
        	addSpawn(110048, -253498, 151138, 7192, 31155, false, 0, false, world.instanceId); //Flak Wyrr
        	world.incLiveMobs();
        	addSpawn(110048, -254481, 151270, 7192, 64496, false, 0, false, world.instanceId); //Flak Wyrr
        	world.incLiveMobs();
        	addSpawn(110026, -253300, 151686, 7192, 46596, false, 0, false, world.instanceId); //King's Guard
        	world.incLiveMobs();
        	addSpawn(110075, -253907, 151565, 7192, 15229, false, 0, false, world.instanceId); //Abaddon's Guard
        	world.incLiveMobs();
        	addSpawn(110033, -254395, 151873, 7192, 47550, false, 0, false, world.instanceId); //Lizardude
        	world.incLiveMobs();
        	addSpawn(110019, -253224, 152057, 7192, 47454, false, 0, false, world.instanceId); //Underworld's Terror
        	world.incLiveMobs();
        	addSpawn(110018, -253781, 152070, 7192, 47822, false, 0, false, world.instanceId); //Jeratrix
        	world.incLiveMobs();
        	addSpawn(110027, -254311, 152295, 7192, 47671, false, 0, false, world.instanceId); //Amon
        	world.incLiveMobs();
        	addSpawn(110019, -253166, 152629, 7192, 29990, false, 0, false, world.instanceId); //Underworld's Terror
        	world.incLiveMobs();
        	addSpawn(110022, -253687, 152646, 7192, 47371, false, 0, false, world.instanceId); //Terrifying Gardener
        	world.incLiveMobs();
        	addSpawn(110019, -254148, 152934, 7192, 62575, false, 0, false, world.instanceId); //Underworld's Terror
        	world.incLiveMobs();
        	addSpawn(110075, -253611, 152908, 7192, 12449, false, 0, false, world.instanceId); //Abaddon's Guard
        	world.incLiveMobs();
        	addSpawn(95124, -252894, 153137, 7192, 44022, false, 0, false, world.instanceId); //Disfigured One
        	world.incLiveMobs();
        	addSpawn(110026, -253379, 153407, 7192, 45601, false, 0, false, world.instanceId); //King's Guard
        	world.incLiveMobs();
        	addSpawn(110069, -253942, 153544, 7192, 43842, false, 0, false, world.instanceId); //Demongon
        	world.incLiveMobs();
        	addSpawn(110062, -252752, 153485, 7192, 44022, false, 0, false, world.instanceId); //Majesty Slayer
        	world.incLiveMobs();
        	addSpawn(110029, -253164, 153943, 7192, 45671, false, 0, false, world.instanceId); //Deformed Drake
        	world.incLiveMobs();
        	addSpawn(110027, -253776, 153935, 7192, 43878, false, 0, false, world.instanceId); //Amon
        	world.incLiveMobs();

            world.incStage(); //set the stage to 6 so teleporter will port to 6
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
    public void spawn10thMobs(EmbryoWorld world, L2PcInstance player)
    {
        if (world.getStage() == 9)
        {
        	addSpawn(110083, -242610, 152688, 5144, 8191, false, 0, false, world.instanceId); //Grudon
        	world.incLiveMobs();
        	addSpawn(110080, -242357, 152207, 5144, 4594, false, 0, false, world.instanceId); //Flavelish
        	world.incLiveMobs();
        	addSpawn(110083, -242114, 151717, 5144, 5187, false, 0, false, world.instanceId); //Grudon
        	world.incLiveMobs();
        	addSpawn(1000043, -242958, 152331, 5144, 6649, false, 0, false, world.instanceId); //Gradyon
        	world.incLiveMobs();
        	addSpawn(1000044, -242586, 151631, 5144, 3921, false, 0, false, world.instanceId); //Elysia
        	world.incLiveMobs();

            world.incStage(); //set the stage to 6 so teleporter will port to 6
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
    public void spawn11thMobs(EmbryoWorld world, L2PcInstance player)
    {
        if (world.getStage() == 10)
        {
        	addSpawn(110093, -245556, 151123, 4661, 47893, false, 0, false, world.instanceId); //Zamorak
        	world.incLiveMobs();
        	addSpawn(110095, -245895, 151134, 4663, 47893, false, 0, false, world.instanceId); //Mighuss Raki
        	world.incLiveMobs();

            world.incStage(); //set the stage to 6 so teleporter will port to 6
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
    public void spawn12thMobs(EmbryoWorld world, L2PcInstance player)
    {
        if (world.getStage() == 11)
        {
        	addSpawn(95634, -245764, 151752, 11848, 48351, false, 0, false, world.instanceId); //Darion
        	world.incLiveMobs();

            world.incStage(); //set the stage to 6 so teleporter will port to 6
        	//Announcements.getInstance().announceToAll("Spawned "+String.valueOf(world.liveMobs)+" mobs and Increased Stage to stage: " + String.valueOf(world.stage));
        	//Announcements.getInstance().announceToAll("Alive mobs: " + String.valueOf(world.liveMobs));
        }
    }
}