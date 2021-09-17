package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.7 $ $Date: 2005/03/27 15:29:32 $
 */
public final class L2ClassMasterInstance extends L2NpcInstance
{
	public L2ClassMasterInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(final L2PcInstance player)
	{
		//player.sendPacket(new ClassUpgradeWnd(player));
		//showHtmlMenu(player, getObjectId(), 3);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
//		if (!Config.ALLOW_CLASS_MASTERS)
//			return;
//		if (command.startsWith("1stClass"))
//			showHtmlMenu(player, getObjectId(), 1);
//		else if (command.startsWith("2ndClass"))
//			showHtmlMenu(player, getObjectId(), 2);
//		else if (command.startsWith("3rdClass"))
//			showHtmlMenu(player, getObjectId(), 3);
//		else if (command.startsWith("change_class"))
//		{
//			final int val = Integer.parseInt(command.substring(13));
//			if (checkAndChangeClass(player, val))
//			{
//				// self-animation
//				player.broadcastPacket(new MagicSkillUse(player, player, 5103, 1, 0, 0));
//				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
//				html.setFile("data/html/classmaster/ok.htm");
//				html.replace("%name%", CharTemplateTable.getInstance().getClassNameById(val));
//				player.sendPacket(html);
//			}
//		}
//		else if (command.startsWith("become_noble"))
//		{
//			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
//			if (!player.isNoble())
//			{
//				player.setNoble(true, true);
//				player.sendPacket(new UserInfo(player));
//				html.setFile("data/html/classmaster/nobleok.htm");
//				player.sendPacket(html);
//			}
//			else
//			{
//				html.setFile("data/html/classmaster/alreadynoble.htm");
//				player.sendPacket(html);
//			}
//		}
//		else if (command.startsWith("learn_skills"))
//		{
//			player.giveAvailableSkills();
//			player.sendSkillList();
//		}
//		else
//			super.onBypassFeedback(player, command);
	}
	

	public static final boolean validateClassId(final ClassId oldCID, final ClassId newCID)
	{
		if (newCID == null || newCID.getRace() == null)
			return false;
		if (oldCID.equals(newCID.getParent()))
			return true;
		if (newCID.childOf(oldCID))
			return true;
		if (oldCID == ClassId.maleSoldier && newCID == ClassId.judicator)
			return true;
		return false;
	}
	private static final boolean validateClassId(final ClassId oldCID, final int val)
	{
		try
		{
			return validateClassId(oldCID, ClassId.values()[val]);
		}
		catch (final Exception e)
		{
			// possible ArrayOutOfBoundsException
		}
		return false;
	}
	public static final boolean checkAndChangeClass(final L2PcInstance player, final int val)
	{
		final ClassId currentClassId = player.getClassId();
		if (getMinLevel(currentClassId.level()) > player.getLevel())
			return false;
		if (!validateClassId(currentClassId, val))
			return false;
		final int newJobLevel = currentClassId.level() + 1;
		// check if player have all required items for class transfer
		// get all required items for class transfer
		// reward player with items
		player.setClassId(val);
		if (player.isSubClassActive())
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		else
			player.setBaseClass(player.getActiveClass());
		player.broadcastUserInfo();
		return true;
	}
	
	/**
	 * @param level - current skillId level (0 - start, 1 - first, etc)
	 * @return minimum player level required for next class transfer
	 */
	private static final int getMinLevel(final int level)
	{
		switch (level)
		{
			case 0:
				return 20;
			case 1:
				return 40;
			case 2:
				return 76;
			default:
				return Integer.MAX_VALUE;
		}
	}


	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param val new class index
	 * @return
	 */
//	private static final boolean validateClassId(final ClassId oldCID, final int val)
//	{
//		try
//		{
//			return validateClassId(oldCID, ClassId.values()[val]);
//		}
//		catch (final Exception e)
//		{
//			// possible ArrayOutOfBoundsException
//		}
//		return false;
//	}
//	
	public static final boolean hasValidClasses(final L2PcInstance player)
	{
		final ClassId currentClassId = player.getClassId();
		for (final ClassId cid : ClassId.values())
			if (validateClassId(currentClassId, cid) && cid.level() == 3)
				return true;
		return false;
	}
	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param newCID new ClassId
	 * @return true if class change is possible
	 */
//	public static final boolean validateClassId(final ClassId oldCID, final ClassId newCID)
//	{
//		if (newCID == null || newCID.getRace() == null)
//			return false;
//		if (oldCID.equals(newCID.getParent()))
//			return true;
//		if (Config.ALLOW_ENTIRE_TREE && newCID.childOf(oldCID))
//			return true;
//		if (oldCID == ClassId.maleSoldier && newCID == ClassId.Judicator)
//			return true;
//		return false;
//	}
	
//	private static String getRequiredItems(final int level)
//	{
//		if (Config.CLASS_MASTER_SETTINGS.getRequireItems(level) == null || Config.CLASS_MASTER_SETTINGS.getRequireItems(level).isEmpty())
//			return "<tr><td>none</td></r>";
//		final StringBuilder sb = new StringBuilder();
//		for (final int _itemId : Config.CLASS_MASTER_SETTINGS.getRequireItems(level).keys())
//		{
//			final int _count = Config.CLASS_MASTER_SETTINGS.getRequireItems(level).get(_itemId);
//			sb.append("<tr><td><font color=\"LEVEL\">" + _count + "</font></td><td>" + ItemTable.getInstance().getTemplate(_itemId).getName() + "</td></tr>");
//		}
//		return sb.toString();
//	}
}
