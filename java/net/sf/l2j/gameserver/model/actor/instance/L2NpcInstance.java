package net.sf.l2j.gameserver.model.actor.instance;


import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2EnchantSkillLearn;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.status.FolkStatus;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AcquireSkillList;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExEnchantSkillList;
import net.sf.l2j.gameserver.network.serverpackets.ExEnchantSkillList.EnchantSkillType;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.effects.EffectBuff;
import net.sf.l2j.gameserver.skills.effects.EffectDebuff;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.StringUtil;

public class L2NpcInstance extends L2Npc
{
	private final ClassId[] _classesToTeach;

	public L2NpcInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setIsInvul(false);
		_classesToTeach = template.getTeachInfo();
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		super.onAction(player);
	}
	
	@Override
	public FolkStatus getStatus()
	{
		return (FolkStatus)super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new FolkStatus(this));
	}
	
	@Override
	public void addEffect(L2Effect newEffect)
	{
		if (newEffect instanceof EffectDebuff || newEffect instanceof EffectBuff)
			super.addEffect(newEffect);
		else if (newEffect != null)
			newEffect.stopEffectTask();
	}

	/**
	 * this displays SkillList to the player.
	 * @param player
	 */
	public void showSkillList(L2PcInstance player, ClassId classId)
	{
		if (Config.DEBUG)
			_log.fine("SkillList activated on: "+getObjectId());

		int npcId = getTemplate().npcId;

		if (npcId == 32611)
		{
			L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSpecialSkills(player);
			AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Special);
			
			int counts = 0;

	        for (L2SkillLearn s : skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

				if (sk == null)
					continue;

				counts++;
				asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), 0, 1);
			}

			if (counts == 0) // No more skills to learn, come back when you level.

			    player.sendPacket(new SystemMessage(SystemMessageId.NO_MORE_SKILLS_TO_LEARN));
			else
			    player.sendPacket(asl);

			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (_classesToTeach == null)
		{
			return;
		}
		
		if (!getTemplate().canTeach(classId))
		{
			showNoTeachHtml(player);
			return;
		}
		
		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, classId);
		AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Usual);
		int counts = 0;

		for (L2SkillLearn s: skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

			if (sk == null)
				continue;

			int cost = SkillTreeTable.getInstance().getSkillCost(player, sk);
			counts++;

			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}

		if (counts == 0)
		{
		    int minlevel = SkillTreeTable.getInstance().getMinLevelForNewSkill(player, classId);

		    if (minlevel > 0)
		    {
		        SystemMessage sm = new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN);
		        sm.addNumber(minlevel);
		        player.sendPacket(sm);
		    }
		    else
		    {
		        SystemMessage sm = new SystemMessage(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
		        player.sendPacket(sm);
		    }
		}
		else
		{
            player.sendPacket(asl);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
     * this displays EnchantSkillList to the player.
     * @param player
     */
    public static void showEnchantSkillList(L2PcInstance player, boolean isSafeEnchant)
    {
        if (Config.DEBUG)
        {
            _log.fine("EnchantSkillList activated on: "+player.getName());
        }
        
        if (player.getClassId().level() < 3)
        {
        	NpcHtmlMessage html = new NpcHtmlMessage(1);
            html.setHtml(
                    "<html><body>" +
                    "You must have 3rd class change completed." +
                    "</body></html>");
            player.sendPacket(html);

            return;
        }
        
        final int playerLevel = player.getLevel();
        
        if (playerLevel >= 76)
        {
            final ExEnchantSkillList esl = new ExEnchantSkillList(isSafeEnchant ? EnchantSkillType.SAFE : EnchantSkillType.NORMAL);
            final L2Skill[] charSkills = player.getAllSkills();
            int counts = 0;
            for  (L2Skill skill : charSkills)
            {
                final L2EnchantSkillLearn enchantLearn = SkillTreeTable.getInstance().getSkillEnchantmentForSkill(skill);
                
                if (enchantLearn != null)
                {
                    esl.addSkill(skill.getId(), skill.getLevel());
                    counts++;
                }
            }
            
            if (counts == 0)
            {
                player.sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT));
            }
            else
            {
                player.sendPacket(esl);
            }
        }
        else
        {
            player.sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT));
        }
        
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }
    
    /**
     * Show the list of enchanted skills for changing enchantment route
     * 
     * @param player
     * @param classId
     */
    public static void showEnchantChangeSkillList(L2PcInstance player)
    {        
        if (player.getClassId().level() < 3)
        {
            NpcHtmlMessage html = new NpcHtmlMessage(1);
            html.setHtml(
                    "<html><body>" +
                    "You must have 3rd class change completed." +
                    "</body></html>");
            player.sendPacket(html);

            return;
        }
        
        final int playerLevel = player.getLevel();
        
        if (playerLevel >= 76)
        {
            final ExEnchantSkillList esl = new ExEnchantSkillList(EnchantSkillType.CHANGE_ROUTE);
            final L2Skill[] charSkills = player.getAllSkills();
            
            for  (L2Skill skill : charSkills)
            {
                // is enchanted?
                if (skill.getLevel() > 100)
                {
                    esl.addSkill(skill.getId(), skill.getLevel());
                }
            }
            
            player.sendPacket(esl);
        }
        else
        {
            player.sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT));
        }
        
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }
    
    /**
     * Show the list of enchanted skills for untraining
     * 
     * @param player
     * @param classId
     */
    public static void showEnchantUntrainSkillList(L2PcInstance player, ClassId classId)
    {
        if (player.getClassId().level() < 3)
        {
            NpcHtmlMessage html = new NpcHtmlMessage(1);
            html.setHtml(
                    "<html><body>" +
                    "You must have 3rd class change completed." +
                    "</body></html>");
            player.sendPacket(html);

            return;
        }
        
        final int playerLevel = player.getLevel();
        
        if (playerLevel >= 76)
        {
            final ExEnchantSkillList esl = new ExEnchantSkillList(EnchantSkillType.UNTRAIN);
            final L2Skill[] charSkills = player.getAllSkills();
            
            for  (L2Skill skill : charSkills)
            {
                // is enchanted?
                if (skill.getLevel() > 100)
                {
                    esl.addSkill(skill.getId(), skill.getLevel());
                }
            }
            
            player.sendPacket(esl);
        }
        else
        {
            player.sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT));
        }
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("SkillList"))
		{
			if (Config.ALT_GAME_SKILL_LEARN)
			{
				String id = command.substring(9).trim();

				if (id.length() != 0)
                {
					player.setSkillLearningClassId(ClassId.values()[Integer.parseInt(id)]);
					showSkillList(player, ClassId.values()[Integer.parseInt(id)]);
				}
                else
                {
					boolean own_class = false;

					if (_classesToTeach != null)
                    {
						for (ClassId cid : _classesToTeach)
                        {
							if (cid.equalsOrChildOf(player.getClassId()))
                            {
								own_class = true;
								break;
							}
						}
					}

					String text = "<html><body><center>Skill learning:</center><br>";

					if (!own_class)
                    {
						String charType = player.getClassId().isMage() ? "fighter" : "mage";
						text +=
							"Skills of your class are the easiest to learn.<br>"+
							"Skills of another class of your race are a little harder.<br>"+
							"Skills for classes of another race are extremely difficult.<br>"+
							"But the hardest of all to learn are the  "+ charType +"skills!<br>";
					}

					// make a list of classes
					if (_classesToTeach != null)
                    {
						int count = 0;
						ClassId classCheck = player.getClassId();

						while ((count == 0) && (classCheck != null))
						{
						    for (ClassId cid : _classesToTeach)
						    {
						        if (cid.level() > classCheck.level()) 
						            continue;

						        if (SkillTreeTable.getInstance().getAvailableSkills(player, cid).length == 0)
						            continue;

						        text += "<a action=\"bypass -h npc_%objectId%_SkillList "+cid.getId()+"\">Learn "+cid+"'s class Skills</a><br>\n";
						        count++;
						    }
						    classCheck = classCheck.getParent();
						}
						classCheck = null;
                    }
                    else
                    {
                        text += "No Skills.<br>";
                    }

					text +=
						"</body></html>";

					insertObjectIdAndShowChatWindow(player, text);
					player.sendPacket( ActionFailed.STATIC_PACKET );
				}
			}
            else
            {
				player.setSkillLearningClassId(player.getClassId());
				showSkillList(player, player.getClassId());
			}
		}
		else
		{
			// this class dont know any other commands, let forward
			// the command to the parent class

			super.onBypassFeedback(player, command);
		}
	}
	
	public static void onBypass(final L2PcInstance player, final String command)
	{
		if (command.startsWith("EnchantSkillList"))
		{
			showEnchantSkillList(player, false);
		}
		else if (command.startsWith("SafeEnchantSkillList"))
		{
			showEnchantSkillList(player, true);
		}
		else if (command.startsWith("ChangeEnchantSkillList"))
		{
			showEnchantChangeSkillList(player);
		}
		else if (command.startsWith("UntrainEnchantSkillList"))
		{
			showEnchantUntrainSkillList(player, player.getClassId());
		}
		else
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	private void showNoTeachHtml(L2PcInstance player)
	{
		int npcId = getNpcId();
		String html = "";

		if (this instanceof L2WarehouseInstance)
			html = HtmCache.getInstance().getHtm("data/html/warehouse/" + npcId + "-noteach.htm");
		else if (this instanceof L2TrainerInstance)
			html = HtmCache.getInstance().getHtm("data/html/trainer/" + npcId + "-noteach.htm");

		if (html == null)
		{
			_log.warning("Npc " + npcId + " missing noTeach html!");
			NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
			final String sb = StringUtil.concat(
					"<html><body>" +
					"I cannot teach you any skills.<br>You must find your current class teachers.",
					"</body></html>"
			);
			msg.setHtml(sb);
			player.sendPacket(msg);
			return;
		}
		else
		{
			NpcHtmlMessage noTeachMsg = new NpcHtmlMessage(getObjectId());
			noTeachMsg.setHtml(html);
			noTeachMsg.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(noTeachMsg);
		}
	}
}
