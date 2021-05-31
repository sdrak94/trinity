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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillSpellbookTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2PledgeSkillLearn;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.L2TransformSkillLearn;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2FishermanInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TransformManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2VillageMasterInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutRegister;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestAquireSkill extends L2GameClientPacket
{
	private static final String _C__6C_REQUESTAQUIRESKILL = "[C] 6C RequestAquireSkill";

	private static Logger _log = Logger.getLogger(RequestAquireSkill.class.getName());

	private int _id;

	private int _level;

	private int _skillType;

	@Override
	protected void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if (_level < 1 || _level > 1000 || _id < 1 || _id > 32000)
		{
			_log.warning("Recived Wrong Packet Data in Aquired Skill - id:" + _id + " level:" + _level);
			return;
		}

		final L2Npc trainer = player.getLastFolkNPC();
		if (!(trainer instanceof L2NpcInstance))
			return;

		if (!trainer.canInteract(player) && !player.isGM())
			return;

		if (!Config.ALT_GAME_SKILL_LEARN)
			player.setSkillLearningClassId(player.getClassId());

		if (player.getSkillLevel(_id) >= _level)
		{
			// already knows the skill with this level
			return;
		}

		final L2Skill skill = SkillTable.getInstance().getInfo(_id, _level);

		int counts = 0;
		int _requiredSp = 10000000;

		switch (_skillType)
		{
			case 0:
			{
				if (trainer instanceof L2TransformManagerInstance) // transform skills
				{
					int costid = 0;
					// Skill Learn bug Fix
					L2TransformSkillLearn[] skillst = SkillTreeTable.getInstance().getAvailableTransformSkills(player);

					for (L2TransformSkillLearn s : skillst)
					{
						L2Skill sk = SkillTable.getInstance().getInfo(s.getId(),s.getLevel());

						if (sk == null || sk != skill)
							continue;

						counts++;
						costid = s.getItemId();
						_requiredSp = s.getSpCost();
					}

					if (counts == 0)
					{
						player.sendMessage("You are trying to learn skill that u can't..");
						Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", Config.DEFAULT_PUNISH);
						return;
					}

					if (player.getSp() >= _requiredSp)
					{
						if (!player.destroyItemByItemId("Consume", costid, 1, trainer, false))
						{
							// Haven't spellbook
							player.sendPacket(new SystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
							return;
						}

						SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(costid);
						sm.addItemNumber(1);
						sendPacket(sm);
						sm = null;
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL);
						player.sendPacket(sm);
						sm = null;
						return;
					}
					break;					
				}
				// normal skills
				L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, player.getSkillLearningClassId());

				for (L2SkillLearn s : skills)
				{
					L2Skill sk = SkillTable.getInstance().getInfo(s.getId(),
					s.getLevel());
					if (sk == null || sk != skill)
						continue;
					counts++;
					_requiredSp = SkillTreeTable.getInstance().getSkillCost(player,skill);
				}

				if (counts == 0 && !Config.ALT_GAME_SKILL_LEARN)
				{
					player.sendMessage("You are trying to learn skill that u can't..");
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", Config.DEFAULT_PUNISH);
					return;
				}

				if (player.getSp() >= _requiredSp)
				{
					int spbId = -1;

					// divine inspiration require book for each level
					if (Config.DIVINE_SP_BOOK_NEEDED
							&& skill.getId() == L2Skill.SKILL_DIVINE_INSPIRATION)
						spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill, _level);
					else if (Config.SP_BOOK_NEEDED
							&& skill.getLevel() == 1)
						spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill);

					// spellbook required
					if (spbId > -1)
					{
						L2ItemInstance spb = player.getInventory().getItemByItemId(spbId);

						if (spb == null)
						{
							// Haven't spellbook
							player.sendPacket(new SystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
							return;
						}

						// ok
						player.destroyItem("Consume", spb.getObjectId(), 1, trainer, true);
					}
				}
				else
				{
					SystemMessage sm = new SystemMessage(
							SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL);
					player.sendPacket(sm);
					sm = null;
					return;
				}
				break;
			}
			case 1:
			{
				int costid = 0;
				int costcount = 0;
				// Skill Learn bug Fix
				L2SkillLearn[] skillsc = SkillTreeTable.getInstance().getAvailableSkills(player);

				for (L2SkillLearn s : skillsc)
				{
					L2Skill sk = SkillTable.getInstance().getInfo(s.getId(),s.getLevel());

					if (sk == null || sk != skill)
						continue;

					counts++;
					costid = s.getIdCost();
					costcount = s.getCostCount();
					_requiredSp = s.getSpCost();
				}

				if (counts == 0)
				{
					player.sendMessage("You are trying to learn skill that u can't..");
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", Config.DEFAULT_PUNISH);
					return;
				}

				if (player.getSp() >= _requiredSp)
				{
					if (!player.destroyItemByItemId("Consume", costid, costcount, trainer, false))
					{
						// Haven't spellbook
						player.sendPacket(new SystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
						return;
					}

					SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(costid);
					sm.addItemNumber(costcount);
					sendPacket(sm);
					sm = null;
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL);
					player.sendPacket(sm);
					sm = null;
					return;
				}
				break;
			}
			case 2:
			{
				if (!player.isClanLeader())
				{
					// TODO: Find and add system msg
					player.sendMessage("This feature is available only for the clan leader");
					return;
				}

				int itemId = 0;
				int itemCount = 0;
				int repCost = 100000000;
				// Skill Learn bug Fix
				L2PledgeSkillLearn[] skills = SkillTreeTable.getInstance().getAvailablePledgeSkills(player);

				for (L2PledgeSkillLearn s : skills)
				{
					L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

					if (sk == null || sk != skill)
						continue;

					counts++;
					itemId = s.getItemId();
					itemCount = s.getItemCount();
					repCost = s.getRepCost();
				}

				if (counts == 0)
				{
					player.sendMessage("You are trying to learn skill that u can't..");
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", Config.DEFAULT_PUNISH);
					return;
				}

				if (player.getClan().getReputationScore() >= repCost)
				{
					if (Config.LIFE_CRYSTAL_NEEDED)
					{
						if (!player.destroyItemByItemId("Consume", itemId, itemCount, trainer, false))
						{
							// Haven't spellbook
							player.sendPacket(new SystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
							return;
						}

						SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(itemId);
						sm.addNumber(itemCount);
						sendPacket(sm);
						sm = null;
					}
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE);
					player.sendPacket(sm);
					return;
	            }
				player.getClan().setReputationScore(player.getClan().getReputationScore()-repCost, true);
				player.getClan().addNewSkill(skill);

				if (Config.DEBUG)
					_log.fine("Learned pledge skill " + _id + " for " + _requiredSp + " SP.");

				SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
				cr.addNumber(repCost);
				player.sendPacket(cr);
				SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED);
				sm.addSkillName(_id);
				player.sendPacket(sm);
				sm = null;

				player.getClan().broadcastToOnlineMembers(new PledgeSkillList(player.getClan()));

				for(L2PcInstance member: player.getClan().getOnlineMembers(0)) 
	            {
					member.sendSkillList();
	            }
				L2VillageMasterInstance.showPledgeSkillList(player); //Maybe we shoud add a check here...
	            return;
			}
			case 4:
			{
//				_requiredSp = 0;
//				List<Quest> qlst = trainer.getTemplate().getEventQuests(QuestEventType.ON_SKILL_LEARN);
//				if ((qlst != null) && qlst.siz == 1)
//				{
//					if (!qlst[0].notifyAcquireSkill(trainer, player, skill))
//					{
//						qlst[0].notifyAcquireSkillList(trainer, player);
//						return;
//					}
//				}
//				else
//				{
//					return;
//				}
//				break;
			}
			case 6:
			{
				int costid = 0;
				int costcount = 0;
				// Skill Learn bug Fix
				L2SkillLearn[] skillsc = SkillTreeTable.getInstance().getAvailableSpecialSkills(player);

				for (L2SkillLearn s : skillsc)
				{
					L2Skill sk = SkillTable.getInstance().getInfo(s.getId(),s.getLevel());

					if (sk == null || sk != skill)
						continue;

					counts++;
					costid = s.getIdCost();
					costcount = s.getCostCount();
					_requiredSp = s.getSpCost();
				}

				if (counts == 0)
				{
					player.sendMessage("You are trying to learn skill that u can't..");
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", Config.DEFAULT_PUNISH);
					return;
				}

				if (player.getSp() >= _requiredSp)
				{
					if (!player.destroyItemByItemId("Consume", costid, costcount, trainer, false))
					{
						// Haven't spellbook
						player.sendPacket(new SystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
						return;
					}

					SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(costid);
					sm.addItemNumber(costcount);
					sendPacket(sm);
					sm = null;
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL);
					player.sendPacket(sm);
					sm = null;
					return;
				}
				break;
			}
			default:
			{
				_log.warning("Recived Wrong Packet Data in Aquired Skill - unk1:" + _skillType);
				return;
			}
		}
		

		player.addSkill(skill, true);

		if (Config.DEBUG)
			_log.fine("Learned skill " + _id + " for " + _requiredSp + " SP.");

		player.setSp(player.getSp() - _requiredSp);

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.SP, player.getSp());
		player.sendPacket(su);

		SystemMessage sm = new SystemMessage(SystemMessageId.LEARNED_SKILL_S1);
		sm.addSkillName(_id);
		player.sendPacket(sm);
		sm = null;
		
		// update all the shortcuts to this skill
		if (_level > 1)
		{
			L2ShortCut[] allShortCuts = player.getAllShortCuts();

			for (L2ShortCut sc : allShortCuts)
			{
				if (sc.getId() == _id && sc.getType() == L2ShortCut.TYPE_SKILL)
				{
					L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc
							.getPage(), sc.getType(), sc.getId(), _level, 1);
					player.sendPacket(new ShortCutRegister(newsc));
					player.registerShortCut(newsc);
				}
			}
		}
		player.sendSkillList();
		if (_skillType == 4)
		{
//			Quest[] qlst = trainer.getTemplate().getEventQuests(QuestEventType.ON_SKILL_LEARN);
//			qlst[0].notifyAcquireSkillList(trainer, player);
		}
		else if (trainer instanceof L2FishermanInstance)
			((L2FishermanInstance) trainer).showSkillList(player);
		else if (trainer instanceof L2TransformManagerInstance)
	        ((L2TransformManagerInstance) trainer).showTransformSkillList(player);
		else
			((L2NpcInstance)trainer).showSkillList(player, player.getSkillLearningClassId());

		if (_id >= 1368 && _id <= 1372) // if skill is expand sendpacket :)
		{
			ExStorageMaxCount esmc = new ExStorageMaxCount(player);
			player.sendPacket(esmc);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__6C_REQUESTAQUIRESKILL;
	}
}
