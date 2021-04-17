package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Manor;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2ChestInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Seed implements IItemHandler
{	
	private int _seedId;
	private L2MonsterInstance _target;
	private L2PcInstance _activeChar;

	public void useItem(L2Playable playable, L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		
		if (CastleManorManager.getInstance().isDisabled())
			return;
		
		_activeChar = (L2PcInstance) playable;
		L2Object target_ = _activeChar.getTarget();
		
		if (!(target_ instanceof L2Npc))
		{
			_activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		L2Character target = ((L2Character)target_);
		if (!(target instanceof L2MonsterInstance) || target instanceof L2ChestInstance || target.isRaid())
		{
			_activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_TARGET_IS_UNAVAILABLE_FOR_SEEDING));
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		_target = (L2MonsterInstance) target;
		
		if (_target == null || _target.isDead())
		{
			_activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (_target.isSeeded())
		{
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		_seedId = item.getItemId();
		
		if (areaValid(MapRegionTable.getInstance().getAreaCastle(_activeChar)))
		{
			_target.setSeeded(_seedId, _activeChar);
			int skillId;
			int skillLvl;
			final String[] skills = item.getEtcItem().getSkills();
			if (skills != null)
			{
				String[] skill = skills[0].split("-");
				skillId = Integer.parseInt(skill[0]);
				skillLvl = Integer.parseInt(skill[1]);
				L2Skill itemskill = SkillTable.getInstance().getInfo(skillId, skillLvl); // Sowing skill
				_activeChar.useMagic(itemskill, false, false);
			}
			
		}
		else
		{
			_activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_SEED_MAY_NOT_BE_SOWN_HERE));
		}
	}

	private boolean areaValid(int castleId)
	{
		return (L2Manor.getInstance().getCastleIdForSeed(_seedId) == castleId);
	}
}