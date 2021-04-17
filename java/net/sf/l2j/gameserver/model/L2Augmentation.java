package net.sf.l2j.gameserver.model;

import javolution.util.FastList;
import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.funcs.FuncAdd;
import net.sf.l2j.gameserver.skills.funcs.LambdaConst;
import net.sf.l2j.gameserver.templates.item.L2Weapon;

public final class L2Augmentation
{
	private int						_effectsId	= 0;
	private AugmentationStatBoni	_boni		= null;
	private L2Skill					_skill		= null;
	
	public L2Augmentation(int effects, L2Skill skill)
	{
		_effectsId = effects;
		_boni = new AugmentationStatBoni(_effectsId);
		_skill = skill;
	}
	
	public L2Augmentation(int effects, int skill, int skillLevel)
	{
		this(effects, SkillTable.getInstance().getInfo(skill, skillLevel));
	}
	// =========================================================
	// Nested Class
	
	public class AugmentationStatBoni
	{
		private final Stats	_stats[];
		private final float	_values[];
		private boolean		_active;
		
		public AugmentationStatBoni(int augmentationId)
		{
			_active = false;
			FastList<AugmentationData.AugStat> as = AugmentationData.getInstance().getAugStatsById(augmentationId);
			_stats = new Stats[as.size()];
			_values = new float[as.size()];
			int i = 0;
			for (AugmentationData.AugStat aStat : as)
			{
				_stats[i] = aStat.getStat();
				_values[i] = aStat.getValue();
				i++;
			}
		}
		
		public void applyBonus(L2PcInstance player, L2ItemInstance item)
		{
			if (_active)
				return;
			boolean weapon = false;
			if (item != null && (item.getItem() instanceof L2Weapon || /* item.getLocationSlot() == L2Item.SLOT_BELT || */ item.getItemId() == 20325)) // plastic hair
				weapon = true;
			/*
			 * if (!weapon)
			 * {
			 * switch (item.getItemId())
			 * {
			 * case 9455:
			 * case 9456:
			 * case 9457:
			 * case 9458: //dynasty jewels
			 * case 9460:
			 * case 14163: //vesper jewels
			 * case 14164:
			 * case 14165:
			 * weapon = true;
			 * }
			 * }
			 */
			if (!weapon)
			{
				switch (item.getItemId())
				{
					case 850000: // Superior Valakas' Fiery
					case 850001: // Superior Antharas' Tremor
					case 850002: // Superior Baium's Anger
					case 850003: // Superior Zaken's Dementia
					case 850004: // Superior Queen's Grasp
					case 850005: // Superior Frintezza's Phylactery
					case 850006: // Superior Beleth's Ring
					case 850007: // Superior Baylor's Earring
					case 850008: // Superior Orfen's Wrath
					case 850009: // Superior Core's Soul
					case 850010: // Superior Helbram's Ring
					case 850011: // Superior Fafurion Necklace
						weapon = true;
				}
			}
			if (weapon)
			{
				for (int i = 0; i < _stats.length; i++)
				{
					if (_stats[i] == Stats.ACCURACY_COMBAT || _stats[i] == Stats.EVASION_RATE)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 1.05)));
					}
					else if (_stats[i] == Stats.CRITICAL_RATE)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i])));
					}
					else if (_stats[i] == Stats.POWER_DEFENCE || _stats[i] == Stats.MAGIC_DEFENCE)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 1.75)));
					}
					else if (_stats[i] == Stats.MAX_HP)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 10.5)));
					}
					else if (_stats[i] == Stats.MAX_CP)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 11.5)));
					}
					else if (_stats[i] == Stats.MAX_MP)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 16.5)));
					}
					else if (_stats[i] == Stats.STAT_STR || _stats[i] == Stats.STAT_INT)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x20, this, new LambdaConst(_values[i])));
					}
					else if (_stats[i] == Stats.STAT_CON || _stats[i] == Stats.STAT_MEN)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x20, this, new LambdaConst(_values[i])));
					}
					else if (_stats[i] == Stats.REGENERATE_CP_RATE || _stats[i] == Stats.REGENERATE_HP_RATE || _stats[i] == Stats.REGENERATE_MP_RATE)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 3.025)));
					}
					else if (_stats[i] == Stats.POWER_ATTACK)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 19.03)));
					}
					else // defaults, essentially just got matk left
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 22)));
					}
				}
			}
			else // non-weapon augments
			{
				for (int i = 0; i < _stats.length; i++)
				{
					if (_stats[i] == Stats.ACCURACY_COMBAT || _stats[i] == Stats.EVASION_RATE)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 0.9)));
					}
					else if (_stats[i] == Stats.CRITICAL_RATE)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] / 1.1)));
					}
					else if (_stats[i] == Stats.POWER_DEFENCE || _stats[i] == Stats.MAGIC_DEFENCE)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 1.591)));
					}
					else if (_stats[i] == Stats.MAX_HP)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 8.345)));
					}
					else if (_stats[i] == Stats.MAX_CP)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 9.045)));
					}
					else if (_stats[i] == Stats.MAX_MP)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 14.545)));
					}
					else if (_stats[i] == Stats.STAT_STR || _stats[i] == Stats.STAT_INT)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x20, this, new LambdaConst(_values[i])));
					}
					else if (_stats[i] == Stats.STAT_CON || _stats[i] == Stats.STAT_MEN)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x20, this, new LambdaConst(_values[i])));
					}
					else if (_stats[i] == Stats.REGENERATE_CP_RATE || _stats[i] == Stats.REGENERATE_HP_RATE || _stats[i] == Stats.REGENERATE_MP_RATE)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 2.75)));
					}
					else if (_stats[i] == Stats.POWER_ATTACK)
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 17.3)));
					}
					else // defaults, essentially just got matk left
					{
						player.addStatFunc(new FuncAdd(_stats[i], 0x45, this, new LambdaConst(_values[i] * 20)));
					}
				}
			}
			_active = true;
		}
		
		public void removeBonus(L2PcInstance player)
		{
			// make sure the bonuses are not removed twice
			if (!_active)
				return;
			player.removeStatsOwner(this);
			_active = false;
		}
	}
	
	public int getAttributes()
	{
		return _effectsId;
	}
	
	/**
	 * Get the augmentation "id" used in serverpackets.
	 * 
	 * @return augmentationId
	 */
	public int getAugmentationId()
	{
		return _effectsId;
	}
	
	public L2Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Applies the bonuses to the player.
	 * 
	 * @param player
	 */
	public void applyBonus(L2PcInstance player, L2ItemInstance item)
	{
		if (item == null)
			return;
		boolean updateTimeStamp = false;
		_boni.applyBonus(player, item);
		// add the skill if any
		if (_skill != null)
		{
			player.addSkill(_skill);
			if (_skill.isActive())
			{
				if (player.getReuseTimeStamp().isEmpty() || !player.getReuseTimeStamp().containsKey(_skill.getId()))
				{
					int equipDelay = _skill.getEquipDelay();
					if (equipDelay > 0)
					{
						player.addTimeStamp(_skill.getId(), equipDelay);
						player.disableSkill(_skill.getId(), equipDelay);
					}
				}
				updateTimeStamp = true;
			}
			player.sendSkillList();
			if (updateTimeStamp)
				player.sendPacket(new SkillCoolTime(player));
		}
	}
	
	/**
	 * Removes the augmentation bonuses from the player.
	 * 
	 * @param player
	 */
	public void removeBonus(L2PcInstance player)
	{
		_boni.removeBonus(player);
		// remove the skill if any
		if (_skill != null)
		{
			if (_skill.isPassive())
				player.removeSkill(_skill);
			else
				player.removeSkill(_skill, false, false);
			player.sendSkillList();
		}
	}
}
