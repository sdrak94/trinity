package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.7 $ $Date: 2005/03/27 15:29:32 $
 */
public final class L2ClassMasterInstance extends L2NpcInstance
{
	//private static Logger _log = Logger.getLogger(L2ClassMasterInstance.class.getName());
/*        private static final int[] BASE_CLASS_IDS = {0, 10, 18, 25, 31, 38, 44,
                49, 53};
        private static final int[] FIRST_CLASS_IDS = {1, 4, 7, 11, 15, 19, 22,
                26, 29, 32, 35, 39, 42, 45, 47, 50, 54, 56};
	private static final int[] SECOND_CLASS_IDS = {2, 3, 5, 6, 9, 8, 12, 13,
                14, 16, 17, 20, 21, 23, 24, 27, 28, 30, 33, 34, 36, 37, 40, 41,
                43, 46, 48, 51, 52, 55, 57};
        private static final int[] THIRD_CLASS_IDS = {88, 89, 90, 91, 92, 93,
                94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107,
                108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118};*/

	/**
	 * @param template
	 */
	public L2ClassMasterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player)) return;

		player.setLastFolkNPC(this);

		// Check if the L2PcInstance already target the L2NpcInstance
		if (getObjectId() != player.getTargetId())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		/*else
		{
			if (!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				return;
			}

			if (Config.DEBUG)
				_log.fine("ClassMaster activated");

			ClassId classId = player.getClassId();

			int jobLevel = 0;
			int level = player.getLevel();
			ClassLevel lvl = PlayerClass.values()[classId.getId()].getLevel();
			switch (lvl)
			{
				case First:
					jobLevel = 1;
					break;
				case Second:
					jobLevel = 2;
					break;
				default:
					jobLevel = 3;
			}

			if (!Config.ALLOW_CLASS_MASTERS)
				jobLevel = 3;

			if(player.isGM())
			{
				showChatWindowChooseClass(player);
			}
			else if (((level >= 20 && jobLevel == 1 ) ||
				(level >= 40 && jobLevel == 2 )) && Config.ALLOW_CLASS_MASTERS)
			{
				showChatWindow(player, classId.getId());
			}
			else if (level >= 76 && Config.ALLOW_CLASS_MASTERS && classId.getId() < 88)
			{
				for (int i = 0; i < SECOND_CLASS_IDS.length; i++)
				{
					if (classId.getId() == SECOND_CLASS_IDS[i])
					{
                        NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
                        final String sb = StringUtil.concat(
                                "<html><body<table width=200>" +
                                "<tr><td><center>",
                                String.valueOf(CharTemplateTable.getInstance().getClassNameById(player.getClassId().getId())),
                                " Class Master:</center></td></tr>" +
                                "<tr><td><br></td></tr>" +
                                "<tr><td><a action=\"bypass -h npc_",
                                String.valueOf(getObjectId()),
                                "_change_class ",
                                String.valueOf(88+i),
                                "\">Advance to ",
                                CharTemplateTable.getInstance().getClassNameById(88+i),
                                "</a></td></tr>" +
                                "<tr><td><br></td></tr>" +
                                "</table></body></html>"
                                );
                        html.setHtml(sb);
                        player.sendPacket(html);
                        break;
					}
				}
			}
            else if (level >= 76 && Config.ALLOW_CLASS_MASTERS && ((classId.getId() >= 123 && classId.getId() < 131 ) || classId.getId() == 135)) // this is for Kamael Race 3rd Transfer
            {
                showChatWindow(player, classId.getId());
            }
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
                                final Collection<Quest> quests = Quest.findAllEvents();
                                final StringBuilder sb = new StringBuilder(30 + 50 + quests.size() * 50);
				sb.append("<html><body>");
				switch (jobLevel)
				{
					case 1:
						sb.append("Come back here when you reach level 20 to change your class.<br>");
						break;
					case 2:
						sb.append("Come back here when you reach level 40 to change your class.<br>");
						break;
					case 3:
						sb.append("There are no more class changes for you.<br>");
						break;
				}

				for (Quest q : quests) {
                                    StringUtil.append(sb,
                                            "Event: <a action=\"bypass -h Quest ",
                                            q.getName(),
                                            "\">",
                                            q.getDescr(),
                                            "</a><br>"
                                            );
                                }

				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
			}
		}*/
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return "data/html/classmaster/" + val + ".htm";
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		/*if(command.startsWith("1stClass"))
		{
			if(player.isGM())
			{
				showChatWindow1st(player);
			}
		}
		else if(command.startsWith("2ndClass"))
		{
			if(player.isGM())
			{
				showChatWindow2nd(player);
			}
		}
		else if(command.startsWith("3rdClass"))
		{
			if(player.isGM())
			{
				showChatWindow3rd(player);
			}
		}
		else if(command.startsWith("baseClass"))
		{
			if(player.isGM())
			{
				showChatWindowBase(player);
			}
		}
		else if(command.startsWith("change_class"))
		{
            int val = Integer.parseInt(command.substring(13));

            // Exploit prevention
            ClassId classId = player.getClassId();
            int level = player.getLevel();
            int jobLevel = 0;
            int newJobLevel = 0;

            ClassLevel lvlnow = PlayerClass.values()[classId.getId()].getLevel();

            if(player.isGM())
            {
            	changeClass(player, val);

                if(player.getClassId().level() == 3)
                	player.sendPacket(new SystemMessage(SystemMessageId.THIRD_CLASS_TRANSFER)); // system sound 3rd occupation
                else
                	player.sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER));    // system sound for 1st and 2nd occupation

                NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
                final String sb = StringUtil.concat(
                        "<html><body>" +
                        "You have now become a <font color=\"LEVEL\">",
                        CharTemplateTable.getInstance().getClassNameById(player.getClassId().getId()),
                        "</font>." +
                        "</body></html>"
                        );
                html.setHtml(sb);
                player.sendPacket(html);
            	return;
            }
            switch (lvlnow)
            {
            	case First:
            		jobLevel = 1;
            		break;
            	case Second:
            		jobLevel = 2;
            		break;
            	case Third:
            		jobLevel = 3;
            		break;
            	default:
            		jobLevel = 4;
            }

            if(jobLevel == 4) return; // no more job changes

            ClassLevel lvlnext = PlayerClass.values()[val].getLevel();
            switch (lvlnext)
            {
            	case First:
            		newJobLevel = 1;
            		break;
            	case Second:
            		newJobLevel = 2;
            		break;
            	case Third:
            		newJobLevel = 3;
            		break;
            	default:
            		newJobLevel = 4;
            }

            // prevents changing between same level jobs
            if(newJobLevel != jobLevel + 1) return;

            if (level < 20 && newJobLevel > 1) return;
            if (level < 40 && newJobLevel > 2) return;
            if (level < 75 && newJobLevel > 3) return;
            // -- prevention ends


            changeClass(player, val);

            if(player.getClassId().level() == 3)
            	player.sendPacket(new SystemMessage(SystemMessageId.THIRD_CLASS_TRANSFER)); // system sound 3rd occupation
            else
            	player.sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER));    // system sound for 1st and 2nd occupation
            
            player.rewardSkills();

            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            final String sb = StringUtil.concat(
                    "<html><body>" +
                    "You have now become a <font color=\"LEVEL\">",
                    CharTemplateTable.getInstance().getClassNameById(player.getClassId().getId()),
                    "</font>." +
                    "</body></html>"
                    );
            html.setHtml(sb);
            player.sendPacket(html);
       }
       else
       {
           super.onBypassFeedback(player, command);
       }*/
 }
	/*private void showChatWindowChooseClass(L2PcInstance player) {
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            final String objectIdString = String.valueOf(getObjectId());
            final String sb = StringUtil.concat(
                    "<html>" +
                    "<body>" +
                    "<table width=200>" +
                    "<tr><td><center>GM Class Master:</center></td></tr>" +
                    "<tr><td><br></td></tr>" +
                    "<tr><td><a action=\"bypass -h npc_",
                    objectIdString,
                    "_baseClass\">Base Classes.</a></td></tr>" +
                    "<tr><td><a action=\"bypass -h npc_",
                    objectIdString,
                    "_1stClass\">1st Classes.</a></td></tr>" +
                    "<tr><td><a action=\"bypass -h npc_",
                    objectIdString,
                    "_2ndClass\">2nd Classes.</a></td></tr>" +
                    "<tr><td><a action=\"bypass -h npc_",
                    objectIdString,
                    "_3rdClass\">3rd Classes.</a></td></tr>" +
                    "<tr><td><br></td></tr>" +
                    "</table>" +
                    "<br><font color=\"LEVEL\">Please notice this menu is only available for Game Masters, not for normal players ;)</font>" +
                    "</body>" +
                    "</html>"
                    );
        html.setHtml(sb);
        player.sendPacket(html);
	}

	private void showChatWindow1st(L2PcInstance player) {
                NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setHtml(createGMClassMasterHtml(FIRST_CLASS_IDS));
            player.sendPacket(html);
	}

	private void showChatWindow2nd(L2PcInstance player) {
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setHtml(createGMClassMasterHtml(SECOND_CLASS_IDS));
            player.sendPacket(html);
	}

	private void showChatWindow3rd(L2PcInstance player) {
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setHtml(createGMClassMasterHtml(THIRD_CLASS_IDS));
            player.sendPacket(html);
	}

	private void showChatWindowBase(L2PcInstance player) {
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setHtml(createGMClassMasterHtml(BASE_CLASS_IDS));
            player.sendPacket(html);
	}

	private void changeClass(L2PcInstance player, int val)
	{
		if (Config.DEBUG) _log.fine("Changing class to ClassId:"+val);
        player.setClassId(val);

        if (player.isSubClassActive())
            player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
        else
            player.setBaseClass(player.getActiveClass());

		player.broadcastUserInfo();
	}*/

/*        private String createGMClassMasterHtml(final int[] classIds) {
            final String objectIdString = String.valueOf(getObjectId());
            final CharTemplateTable charTemplateTable =
                    CharTemplateTable.getInstance();
            final StringBuilder sbString =
                    new StringBuilder(100 + classIds.length * 100);
            sbString.append(
                    "<html>" +
                    "<body>" +
                    "<table width=200>" +
                    "<tr><td><center>GM Class Master:</center></td></tr>" +
                    "<tr><td><br></td></tr>");

            for (int classId : classIds) {
                StringUtil.append(sbString,
                        "<tr><td><a action=\"bypass -h npc_",
                        objectIdString,
                        "_change_class ",
                        String.valueOf(classId),
                        "\">Advance to ",
                        charTemplateTable.getClassNameById(classId),
                        "</a></td></tr>"
                        );
            }

            sbString.append(
                    "</table>" +
                    "</body>" +
                    "</html>"
                    );

            return sbString.toString();
        }*/
}
