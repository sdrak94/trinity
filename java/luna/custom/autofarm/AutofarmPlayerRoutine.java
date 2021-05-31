package luna.custom.autofarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.GeoEngine;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public class AutofarmPlayerRoutine
{
	private final L2PcInstance	player;
	private L2Character			committedTarget	= null;
	
	public AutofarmPlayerRoutine(L2PcInstance player)
	{
		this.player = player;
	}
	
	public void executeRoutine()
	{
		// checkSpoil();
		targetEligibleCreature();
		checkManaPots();
		checkHealthPots();
		attack();
	}
	
	private void attack()
	{
		boolean shortcutsContainAttack = shotcutsContainAttack();
		if (shortcutsContainAttack)
		{
			//physicalAttack();
		}
		useAppropriateSpell();
		if (shortcutsContainAttack)
		{
			//physicalAttack();
		}
	}
	
	private void useAppropriateSpell()
	{
		L2Skill attackSkill = null;
		for ( Integer skillId : getAttackSpells() )
		{
			L2Skill skill = player.getKnownSkill(skillId);
			if ( skill == null )
				continue;
			
			if ( !player.checkDoCastConditions(skill) )
				continue;
			if ( !player.checkUseMagicConditions(skill, false, false) )
			{
				player.setIsCastingNow(false);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				continue;
			}
			
			attackSkill = skill;
		}
		if ( attackSkill != null )
		{
			useMagicSkill(attackSkill, false);
			
			return;
		}
	}

	private void checkHealthPots()
	{
		if (getHpPercentage() <= AutofarmConstants.useHpPotsPercentageThreshold)
		{
			if (player.getFirstEffect(AutofarmConstants.hpPotSkillId) != null)
			{
				return;
			}
			L2ItemInstance hpPots = player.getInventory().getItemByItemId(AutofarmConstants.hpPotItemId);
			if (hpPots != null)
			{
				useItem(hpPots);
			}
		}
	}
	
	private void checkManaPots()
	{
		if (getMpPercentage() <= AutofarmConstants.useMpPotsPercentageThreshold)
		{
			L2ItemInstance mpPots = player.getInventory().getItemByItemId(AutofarmConstants.mpPotItemId);
			if (mpPots != null)
			{
				useItem(mpPots);
			}
		}
	}

	
	private Double getHpPercentage()
	{
		return player.getCurrentHp() * 100.0f / player.getMaxHp();
	}
	
	private Double getMpPercentage()
	{
		return player.getCurrentMp() * 100.0f / player.getMaxMp();
	}

	
	
	private List<Integer> getAttackSpells()
	{
		return getSpellsInSlots();
	}
	
	private List<Integer> getSpellsInSlots()
	{
		List<Integer> spells = new ArrayList<Integer>();
		for(L2ShortCut spell : player.getAllShortCuts())
		{
			if (spell.getPage() == 8 && spell.getType() == L2ShortCut.TYPE_SKILL)
			{
				spells.add(spell.getId());
			}
		}
		return spells;
	}
	
	private boolean shotcutsContainAttack()
	{
		return Arrays.stream(player.getAllShortCuts()).anyMatch(shortcut ->
		/* shortcut.getPage() == 0 && */shortcut.getType() == L2ShortCut.TYPE_ACTION && shortcut.getId() == 2);
	}
	
	private void castSpellWithAppropriateTarget(L2Skill skill, Boolean forceOnSelf)
	{
		if (forceOnSelf)
		{
			L2Object oldTarget = player.getTarget();
			player.setTarget(player);
			
			player.useMagic(skill, false, false);
			player.setTarget(oldTarget);
			return;
		}
		player.useMagic(skill, false, false);
	}
	
	private void physicalAttack()
	{
		if (!(player.getTarget() instanceof L2MonsterInstance))
		{
			return;
		}
		L2MonsterInstance target = (L2MonsterInstance) player.getTarget();
		if (target.isAutoAttackable(player))
		{
			if (GeoEngine.getInstance().canSeeTarget(player, target))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
				player.onActionRequest();
			}
		}
		else
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			if (GeoEngine.getInstance().canSeeTarget(player, target))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, target);
		}
	}
	
	public void targetEligibleCreature()
	{
		if (committedTarget != null)
		{
			if (!committedTarget.isDead() && GeoData.getInstance().canSeeTarget(player, committedTarget)/* && !player.isMoving() */)
			{
				//Announcements.getInstance().announceToAll(committedTarget.getName()  + " --> Can see");
				return;
			}
			else
			{
				//Announcements.getInstance().announceToAll(committedTarget.getName()  + " --> Can't see");
				committedTarget = null;
				player.setTarget(null);
			}
		}
		List<L2MonsterInstance> targets = getKnownMonstersInRadius(player, 1200, creature -> GeoData.getInstance().canSeeTarget(player, creature) && !creature.isDead());
		
		if (targets.isEmpty())
		{
			return;
		}
		double minDist = Double.MAX_VALUE;
		L2MonsterInstance minMob = null;
		
		for(L2MonsterInstance mob : targets)
		{
			double distMob = Util.calculateDistance(player, mob, false);
			
			if ( distMob < minDist)
			{
				minDist = distMob;
				minMob = mob;
			}
		}
		if ( minMob != null)
		{
			committedTarget = minMob;
		}
		player.setTarget(committedTarget);
	}
	
	public final List<L2MonsterInstance> getKnownMonstersInRadius(L2PcInstance player, int radius, Function<L2MonsterInstance, Boolean> condition)
	{
		final L2WorldRegion region = player.getWorldRegion();
		if (region == null)
			return Collections.emptyList();
		final List<L2MonsterInstance> result = new ArrayList<>();
		for (L2WorldRegion reg : region.getSurroundingRegions())
		{
			for (L2Object obj : reg.getVisibleObjects().values())
			{
				if (!(obj instanceof L2MonsterInstance) || !Util.checkIfInRange(radius, player, obj, true) || !condition.apply((L2MonsterInstance) obj))
					continue;
				result.add((L2MonsterInstance) obj);
			}
		}
		return result;
	}
	
	public L2MonsterInstance getMonsterTarget()
	{
		if (!(player.getTarget() instanceof L2MonsterInstance))
		{
			return null;
		}
		return (L2MonsterInstance) player.getTarget();
	}
	
	private void useMagicSkill(L2Skill skill, Boolean forceOnSelf)
	{
		if (skill.getSkillType() == L2SkillType.RECALL)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (skill.isToggle() && player.isMounted())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else
		{
			castSpellWithAppropriateTarget(skill, forceOnSelf);
		}
	}
	
	public void useItem(L2ItemInstance item)
	{
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.ITEMS_UNAVAILABLE_FOR_STORE_MANUFACTURE);
			return;
		}
		if (item == null)
			return;
		if (player.isAlikeDead() || player.isStunned() || player.isSleeping() || player.isParalyzed() || player.isAfraid())
			return;
		if (!item.isEquipped())
		{
			if (!item.getItem().checkCondition(player, player, true))
				return;
		}
		if (item.isEquipable())
		{
			if (player.isCastingNow() || player.isCastingSimultaneouslyNow())
			{
				player.sendPacket(SystemMessageId.CANNOT_USE_ITEM_WHILE_USING_MAGIC);
				return;
			}
			switch (item.getItem().getBodyPart())
			{
				case L2Item.SLOT_LR_HAND:
				case L2Item.SLOT_L_HAND:
				case L2Item.SLOT_R_HAND:
				{
					if (player.isMounted())
					{
						player.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
						return;
					}
					if (player.isCursedWeaponEquipped())
						return;
					break;
				}
			}
			if (player.isCursedWeaponEquipped() && item.getItemId() == 6408)
				return;
			if (player.isAttackingNow())
				ThreadPool.schedule(() ->
				{
					final L2ItemInstance itemToTest = player.getInventory().getItemByObjectId(item.getObjectId());
					if (itemToTest == null)
						return;
					player.useEquippableItem(itemToTest, false, false);
				}, player.getAttackEndTime() - System.currentTimeMillis());
			else
				player.useEquippableItem(item, true, true);
		}
		else
		{}
	}
}
