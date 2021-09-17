package net.sf.l2j.gameserver.skills;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.Elementals;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2BufferInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MinionInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TrapInstance;
import net.sf.l2j.gameserver.model.base.PlayerState;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.newEvents.NewTvT;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.conditions.ConditionPlayerState;
import net.sf.l2j.gameserver.skills.conditions.ConditionUsingItemType;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.templates.chars.L2PcTemplate;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public final class Formulas
{
	/** Regen Task period */
	protected static final Logger	_log							= Logger.getLogger(L2Character.class.getName());
	private static final int		HP_REGENERATE_PERIOD			= 3000;											// 3 secs
	public static final byte		SHIELD_DEFENSE_FAILED			= 0;											// no shield defense
	public static final byte		SHIELD_DEFENSE_SUCCEED			= 1;											// normal shield defense
	public static final byte		SHIELD_DEFENSE_PERFECT_BLOCK	= 2;											// perfect block
	public static final byte		SKILL_REFLECT_FAILED			= 0;											// no reflect
	public static final byte		SKILL_REFLECT_SUCCEED			= 1;											// normal reflect, some damage reflected
	// some other not
	public static final byte		SKILL_REFLECT_VENGEANCE			= 2;											// 100% of the damage affect both
	private static final byte		MELEE_ATTACK_RANGE				= 70;
	public static final int			MAX_STAT_VALUE					= 200;
	private static final double[]	STRCompute						= new double[]
	{
		1.036, 34.845
	};																												// {1.016, 28.515};
	// for C1
	private static final double[]	INTCompute						= new double[]
	{
		1.020, 31.375
	};																												// {1.020, 31.375};
	// for C1
	private static final double[]	DEXCompute						= new double[]
	{
		1.009, 19.360
	};																												// {1.009, 19.360};
	// for C1
	private static final double[]	WITCompute						= new double[]
	{
		1.030, 19.800
	};																												// {1.040, 19.000};
	// for C1
	private static final double[]	CONCompute						= new double[]
	{
		1.030, 27.552
	};																												// {1.015, 12.488};
	// for C1
	private static final double[]	MENCompute						= new double[]
	{
		1.010, -0.060
	};																												// {1.010, -0.060};
	// for C1
	protected static final double[]	WITbonus						= new double[MAX_STAT_VALUE];
	protected static final double[]	MENbonus						= new double[MAX_STAT_VALUE];
	protected static final double[]	INTbonus						= new double[MAX_STAT_VALUE];
	public static final double[]	STRbonus						= new double[MAX_STAT_VALUE];
	public static final double[]	DEXbonus						= new double[MAX_STAT_VALUE];
	protected static final double[]	CONbonus						= new double[MAX_STAT_VALUE];
	// These values are 100% matching retail tables, no need to change and no need add
	// calculation into the stat bonus when accessing (not efficient),
	// better to have everything precalculated and use values directly (saves CPU)
	static
	{
		for (int i = 0; i < STRbonus.length; i++)
			STRbonus[i] = Math.floor(Math.pow(STRCompute[0], i - STRCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < INTbonus.length; i++)
			INTbonus[i] = Math.floor(Math.pow(INTCompute[0], i - INTCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < DEXbonus.length; i++)
			DEXbonus[i] = Math.floor(Math.pow(DEXCompute[0], i - DEXCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < WITbonus.length; i++)
			WITbonus[i] = Math.floor(Math.pow(WITCompute[0], i - WITCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < CONbonus.length; i++)
			CONbonus[i] = Math.floor(Math.pow(CONCompute[0], i - CONCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < MENbonus.length; i++)
			MENbonus[i] = Math.floor(Math.pow(MENCompute[0], i - MENCompute[1]) * 100 + .5d) / 100;
	}
	
	static class FuncAddLevel3 extends Func
	{
		static final FuncAddLevel3[] _instancies = new FuncAddLevel3[Stats.NUM_STATS];
		
		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();
			if (_instancies[pos] == null)
				_instancies[pos] = new FuncAddLevel3(stat);
			return _instancies[pos];
		}
		
		private FuncAddLevel3(Stats pStat)
		{
			super(pStat, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			env.value += env.player.getLevel(true) / 3.0;
		}
	}
	
	static class FuncMultLevelMod extends Func
	{
		static final FuncMultLevelMod[] _instancies = new FuncMultLevelMod[Stats.NUM_STATS];
		
		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();
			if (_instancies[pos] == null)
				_instancies[pos] = new FuncMultLevelMod(stat);
			return _instancies[pos];
		}
		
		private FuncMultLevelMod(Stats pStat)
		{
			super(pStat, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			env.value *= env.player.getLevelMod();
		}
	}
	
	static class FuncMultRegenResting extends Func
	{
		static final FuncMultRegenResting[] _instancies = new FuncMultRegenResting[Stats.NUM_STATS];
		
		/**
		 * Return the Func object corresponding to the state concerned.<BR>
		 * <BR>
		 */
		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();
			if (_instancies[pos] == null)
				_instancies[pos] = new FuncMultRegenResting(stat);
			return _instancies[pos];
		}
		
		/**
		 * Constructor of the FuncMultRegenResting.<BR>
		 * <BR>
		 */
		private FuncMultRegenResting(Stats pStat)
		{
			super(pStat, 0x20, null);
			setCondition(new ConditionPlayerState(PlayerState.RESTING, true));
		}
		
		/**
		 * Calculate the modifier of the state concerned.<BR>
		 * <BR>
		 */
		@Override
		public void calc(Env env)
		{
			if (!cond.test(env))
				return;
			env.value *= 1.45;
		}
	}
	
	static class FuncPAtkMod extends Func
	{
		static final FuncPAtkMod _fpa_instance = new FuncPAtkMod();
		
		static Func getInstance()
		{
			return _fpa_instance;
		}
		
		private FuncPAtkMod()
		{
			super(Stats.POWER_ATTACK, 0x30, null);
		}
		
		@Override
		public void calc(Env env)
		{
			env.value *= STRbonus[env.player.getSTR()] * env.player.getLevelMod();
			if (env.player instanceof L2GuardInstance)
				env.value += 85000;
			else if (env.player instanceof L2PcInstance)
			{
				if (env.player.getActingPlayer().getClassId().isSummoner())
				{
					env.value *= 1.18;
				}
				if (env.player.getActingPlayer().isInOlympiadMode())
				{
					if (env.player.getActingPlayer().isArcherClass())
						env.value += 600;
				}
			}
			/*
			 * else if (env.player instanceof L2Summon)
			 * {
			 * if (env.player.getActingPlayer().isInSgradeZone())
			 * {
			 * env.value /= 2.5;
			 * }
			 * else if (env.player.getActingPlayer().isInOlympiadMode())
			 * {
			 * env.value /= 4.5;
			 * }
			 * }
			 */
			env.baseValue = env.value;
		}
	}
	
	static class FuncMAtkMod extends Func
	{
		static final FuncMAtkMod _fma_instance = new FuncMAtkMod();
		
		static Func getInstance()
		{
			return _fma_instance;
		}
		
		private FuncMAtkMod()
		{
			super(Stats.MAGIC_ATTACK, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			double intb = INTbonus[env.player.getINT()];
			double lvlb = env.player.getLevelMod();
			env.value *= (lvlb * lvlb) * (intb * intb);
			if (env.player instanceof L2PcInstance)
			{
				if (env.player.getActingPlayer().getClassId().isSummoner())
				{
					env.value *= 1.1;
				}
			}
			/*
			 * else if (env.player instanceof L2Summon)
			 * {
			 * if (env.player.getActingPlayer().isInSgradeZone())
			 * {
			 * env.value /= 2.5;
			 * }
			 * else if (env.player.getActingPlayer().isInOlympiadMode())
			 * {
			 * env.value /= 4.5;
			 * }
			 * }
			 */
			env.baseValue = env.value;
		}
	}
	
	static class FuncMDefMod extends Func
	{
		static final FuncMDefMod _fmm_instance = new FuncMDefMod();
		
		static Func getInstance()
		{
			return _fmm_instance;
		}
		
		private FuncMDefMod()
		{
			super(Stats.MAGIC_DEFENCE, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			if (env.player instanceof L2PcInstance)
			{
				L2PcInstance p = (L2PcInstance) env.player;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER) != null)
					env.value -= 5;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER) != null)
					env.value -= 5;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR) != null)
					env.value -= 9;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR) != null)
					env.value -= 9;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK) != null)
					env.value -= 13;
			}
			/*
			 * else if (env.player instanceof L2Summon)
			 * {
			 * if (env.player.getActingPlayer().isInSgradeZone())
			 * {
			 * env.value /= 4;
			 * }
			 * else if (env.player.getActingPlayer().isInOlympiadMode())
			 * {
			 * env.value /= 3;
			 * }
			 * }
			 */
			env.value *= MENbonus[env.player.getMEN()] * env.player.getLevelMod();
			env.baseValue = env.value;
		}
	}
	
	static class FuncPDefMod extends Func
	{
		static final FuncPDefMod _fmm_instance = new FuncPDefMod();
		
		static Func getInstance()
		{
			return _fmm_instance;
		}
		
		private FuncPDefMod()
		{
			super(Stats.POWER_DEFENCE, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			if (env.player instanceof L2PcInstance)
			{
				L2PcInstance p = (L2PcInstance) env.player;
				boolean hasMagePDef = (p.getClassId().isMage() || p.getClassId().getId() == 0x31); // orc mystics are a special case
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD) != null)
					env.value -= 12;
				L2ItemInstance chest = p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
				if (chest != null)
					env.value -= hasMagePDef ? 15 : 31;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) != null || (chest != null && chest.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR))
					env.value -= hasMagePDef ? 8 : 18;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) != null)
					env.value -= 8;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) != null)
					env.value -= 7;
			}
			/*
			 * else if (env.player instanceof L2Summon)
			 * {
			 * if (env.player.getActingPlayer().isInSgradeZone())
			 * {
			 * env.value /= 4;
			 * }
			 * else if (env.player.getActingPlayer().isInOlympiadMode())
			 * {
			 * env.value /= 3;
			 * }
			 * }
			 */
			env.value *= env.player.getLevelMod();
			env.baseValue = env.value;
		}
	}
	
	static class FuncGatesPDefMod extends Func
	{
		static final FuncGatesPDefMod _fmm_instance = new FuncGatesPDefMod();
		
		static Func getInstance()
		{
			return _fmm_instance;
		}
		
		private FuncGatesPDefMod()
		{
			super(Stats.POWER_DEFENCE, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
				env.value *= Config.ALT_SIEGE_DAWN_GATES_PDEF_MULT;
			else if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
				env.value *= Config.ALT_SIEGE_DUSK_GATES_PDEF_MULT;
		}
	}
	
	static class FuncGatesMDefMod extends Func
	{
		static final FuncGatesMDefMod _fmm_instance = new FuncGatesMDefMod();
		
		static Func getInstance()
		{
			return _fmm_instance;
		}
		
		private FuncGatesMDefMod()
		{
			super(Stats.MAGIC_DEFENCE, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
				env.value *= Config.ALT_SIEGE_DAWN_GATES_MDEF_MULT;
			else if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
				env.value *= Config.ALT_SIEGE_DUSK_GATES_MDEF_MULT;
		}
	}
	
	static class FuncBowAtkRange extends Func
	{
		private static final FuncBowAtkRange _fbar_instance = new FuncBowAtkRange();
		
		static Func getInstance()
		{
			return _fbar_instance;
		}
		
		private FuncBowAtkRange()
		{
			super(Stats.POWER_ATTACK_RANGE, 0x10, null);
			setCondition(new ConditionUsingItemType(L2WeaponType.BOW.mask()));
		}
		
		@Override
		public void calc(Env env)
		{
			if (!cond.test(env))
				return;
			// default is 40 and with bow should be 500
			env.value += 460;
			env.baseValue = env.value;
		}
	}
	
	static class FuncCrossBowAtkRange extends Func
	{
		private static final FuncCrossBowAtkRange _fcb_instance = new FuncCrossBowAtkRange();
		
		static Func getInstance()
		{
			return _fcb_instance;
		}
		
		private FuncCrossBowAtkRange()
		{
			super(Stats.POWER_ATTACK_RANGE, 0x10, null);
			setCondition(new ConditionUsingItemType(L2WeaponType.CROSSBOW.mask()));
		}
		
		@Override
		public void calc(Env env)
		{
			if (!cond.test(env))
				return;
			// default is 40 and with crossbow should be 400
			env.value += 360;
			env.baseValue = env.value;
		}
	}
	
	static class FuncAtkAccuracy extends Func
	{
		static final FuncAtkAccuracy _faa_instance = new FuncAtkAccuracy();
		
		static Func getInstance()
		{
			return _faa_instance;
		}
		
		private FuncAtkAccuracy()
		{
			super(Stats.ACCURACY_COMBAT, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;
			// [Square(DEX)]*6 + lvl + weapon hitbonus;
			env.value += Math.sqrt(p.getDEX()) * 6.4;
			env.value += p.getLevel(true);
			if (p instanceof L2Summon)
				env.value += (p.getLevel(true) < 70) ? 8 : 40;
			else if (p instanceof L2MonsterInstance)
			{
				env.value += 50;
				if (((L2MonsterInstance) p).getRare() > 0)
					env.value += 12;
				if (((L2MonsterInstance) p).getElite() > 0)
					env.value += 12;
			}
			else if (p instanceof L2GuardInstance)
				env.value += 80;
			else if (p instanceof L2PcInstance)
			{
				if (p.getActingPlayer().getClassId().isSummoner())
					env.value += 5;
				env.value += 1;
			}
			env.baseValue = env.value;
		}
	}
	
	static class FuncAtkEvasion extends Func
	{
		static final FuncAtkEvasion _fae_instance = new FuncAtkEvasion();
		
		static Func getInstance()
		{
			return _fae_instance;
		}
		
		private FuncAtkEvasion()
		{
			super(Stats.EVASION_RATE, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;
			// [Square(DEX)]*6 + lvl;
			env.value += Math.sqrt(p.getDEX()) * 6;
			env.value += p.getLevel(true);
			if (p instanceof L2MonsterInstance)
			{
				env.value += 15;
				if (p instanceof L2RaidBossInstance || p.getLevel(true) >= 89)
					env.value += 8;
				else if (p instanceof L2MonsterInstance && p.getLevel(true) < 80)
					env.value = 1;
			}
			else if (p instanceof L2PcInstance && p.getActingPlayer().isInOlympiadMode())
			{
				env.value += 1;
				if (p.getActingPlayer().isArcherClass())
					env.value += 3;
			}
			else if (p instanceof L2Summon)
			{
				env.value += (p.getLevel(true) < 70) ? 0 : 30;
				if (((L2Summon) p).getNpcId() == 91000) // fang of eva
					env.value += 12;
				else if (((L2Summon) p).getName().contains("Dark Panther")) // dark panther
					env.value += 11;
			}
			env.baseValue = env.value;
		}
	}
	
	static class FuncAtkCritical extends Func
	{
		static final FuncAtkCritical _fac_instance = new FuncAtkCritical();
		
		static Func getInstance()
		{
			return _fac_instance;
		}
		
		private FuncAtkCritical()
		{
			super(Stats.CRITICAL_RATE, 0x09, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;
			if (p instanceof L2Summon)
				env.value = 140;
			else if (p instanceof L2PcInstance && p.getActiveWeaponInstance() == null)
				env.value = 40;
			else if (p instanceof L2Attackable) // monsters get 2.5x crit than before
			{
				env.value *= DEXbonus[p.getDEX()];
				env.value *= 25;
				if (p instanceof L2RaidBossInstance)
				{
					env.value *= 2.5;
				}
			}
			else
			{
				env.value *= DEXbonus[p.getDEX()];
				env.value *= 10;
			}
			env.baseValue = env.value;
		}
	}
	
	static class FuncMAtkCritical extends Func
	{
		static final FuncMAtkCritical _fac_instance = new FuncMAtkCritical();
		
		static Func getInstance()
		{
			return _fac_instance;
		}
		
		private FuncMAtkCritical()
		{
			super(Stats.MCRITICAL_RATE, 0x30, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;
			if (p instanceof L2Summon)
				env.value = 70; // TODO: needs retail value
			else if (p instanceof L2PcInstance && p.getActiveWeaponInstance() != null)
				env.value *= WITbonus[Math.max(p.getWIT() - 7, 0)];
			env.baseValue = env.value;
		}
	}
	
	static class FuncMoveSpeed extends Func
	{
		static final FuncMoveSpeed _fms_instance = new FuncMoveSpeed();
		
		static Func getInstance()
		{
			return _fms_instance;
		}
		
		private FuncMoveSpeed()
		{
			super(Stats.RUN_SPEED, 0x30, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= DEXbonus[p.getDEX()];
			if (p.isInOlympiadMode())
			{
				if (p.isDaggerClass())
					env.value += 6;
				else if (p.isArcherClass())
					env.value += 9;
			}
			env.baseValue = env.value;
		}
	}
	
	static class FuncPAtkSpeed extends Func
	{
		static final FuncPAtkSpeed _fas_instance = new FuncPAtkSpeed();
		
		static Func getInstance()
		{
			return _fas_instance;
		}
		
		private FuncPAtkSpeed()
		{
			super(Stats.POWER_ATTACK_SPEED, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= DEXbonus[p.getDEX()];
			env.baseValue = env.value;
		}
	}
	
	static class FuncMAtkSpeed extends Func
	{
		static final FuncMAtkSpeed _fas_instance = new FuncMAtkSpeed();
		
		static Func getInstance()
		{
			return _fas_instance;
		}
		
		private FuncMAtkSpeed()
		{
			super(Stats.MAGIC_ATTACK_SPEED, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= WITbonus[p.getWIT()];
			env.baseValue = env.value;
		}
	}
	
	static class FuncHennaSTR extends Func
	{
		static final FuncHennaSTR _fh_instance = new FuncHennaSTR();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaSTR()
		{
			super(Stats.STAT_STR, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			// L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatSTR();
		}
	}
	
	static class FuncHennaDEX extends Func
	{
		static final FuncHennaDEX _fh_instance = new FuncHennaDEX();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaDEX()
		{
			super(Stats.STAT_DEX, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			// L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatDEX();
		}
	}
	
	static class FuncHennaINT extends Func
	{
		static final FuncHennaINT _fh_instance = new FuncHennaINT();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaINT()
		{
			super(Stats.STAT_INT, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			// L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatINT();
		}
	}
	
	static class FuncHennaMEN extends Func
	{
		static final FuncHennaMEN _fh_instance = new FuncHennaMEN();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaMEN()
		{
			super(Stats.STAT_MEN, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			// L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatMEN();
		}
	}
	
	static class FuncHennaCON extends Func
	{
		static final FuncHennaCON _fh_instance = new FuncHennaCON();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaCON()
		{
			super(Stats.STAT_CON, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			// L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatCON();
		}
	}
	
	static class FuncHennaWIT extends Func
	{
		static final FuncHennaWIT _fh_instance = new FuncHennaWIT();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaWIT()
		{
			super(Stats.STAT_WIT, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			// L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
				env.value += pc.getHennaStatWIT();
		}
	}
	
	static class FuncMaxHpAdd extends Func
	{
		static final FuncMaxHpAdd _fmha_instance = new FuncMaxHpAdd();
		
		static Func getInstance()
		{
			return _fmha_instance;
		}
		
		private FuncMaxHpAdd()
		{
			super(Stats.MAX_HP, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel(true) - t.classBaseLevel;
			double hpmod = t.lvlHpMod * lvl;
			double hpmax = (t.lvlHpAdd + hpmod) * lvl;
			double hpmin = (t.lvlHpAdd * lvl) + hpmod;
			env.value += (hpmax + hpmin) / 2;
		}
	}
	
	static class FuncMaxHpMul extends Func
	{
		static final FuncMaxHpMul _fmhm_instance = new FuncMaxHpMul();
		
		static Func getInstance()
		{
			return _fmhm_instance;
		}
		
		private FuncMaxHpMul()
		{
			super(Stats.MAX_HP, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= CONbonus[p.getCON()];
			env.baseValue = env.value;
		}
	}
	
	static class FuncMaxCpAdd extends Func
	{
		static final FuncMaxCpAdd _fmca_instance = new FuncMaxCpAdd();
		
		static Func getInstance()
		{
			return _fmca_instance;
		}
		
		private FuncMaxCpAdd()
		{
			super(Stats.MAX_CP, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel(true) - t.classBaseLevel;
			double cpmod = t.lvlCpMod * lvl;
			double cpmax = (t.lvlCpAdd + cpmod) * lvl;
			double cpmin = (t.lvlCpAdd * lvl) + cpmod;
			env.value += (cpmax + cpmin) / 2;
		}
	}
	
	static class FuncMaxCpMul extends Func
	{
		static final FuncMaxCpMul _fmcm_instance = new FuncMaxCpMul();
		
		static Func getInstance()
		{
			return _fmcm_instance;
		}
		
		private FuncMaxCpMul()
		{
			super(Stats.MAX_CP, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= CONbonus[p.getCON()];
			env.baseValue = env.value;
		}
	}
	
	static class FuncMaxMpAdd extends Func
	{
		static final FuncMaxMpAdd _fmma_instance = new FuncMaxMpAdd();
		
		static Func getInstance()
		{
			return _fmma_instance;
		}
		
		private FuncMaxMpAdd()
		{
			super(Stats.MAX_MP, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel(true) - t.classBaseLevel;
			double mpmod = t.lvlMpMod * lvl;
			double mpmax = (t.lvlMpAdd + mpmod) * lvl;
			double mpmin = (t.lvlMpAdd * lvl) + mpmod;
			env.value += (mpmax + mpmin) / 2;
		}
	}
	
	static class FuncMaxMpMul extends Func
	{
		static final FuncMaxMpMul _fmmm_instance = new FuncMaxMpMul();
		
		static Func getInstance()
		{
			return _fmmm_instance;
		}
		
		private FuncMaxMpMul()
		{
			super(Stats.MAX_MP, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= MENbonus[p.getMEN()];
			env.baseValue = env.value;
		}
	}
	
	/**
	 * Return the period between 2 regenerations task (3s for L2Character, 5 min for
	 * L2DoorInstance).<BR>
	 * <BR>
	 */
	public static int getRegeneratePeriod(L2Character cha)
	{
		if (cha instanceof L2DoorInstance)
			return HP_REGENERATE_PERIOD * 100; // 5 mins
		return HP_REGENERATE_PERIOD; // 3s
	}
	
	/**
	 * Return the standard NPC Calculator set containing ACCURACY_COMBAT and EVASION_RATE.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A calculator is created to manage and dynamically calculate the effect of a character
	 * property (ex : MAX_HP, REGENERATE_HP_RATE...). In fact, each calculator is a table of Func
	 * object in which each Func represents a mathematic function : <BR>
	 * <BR>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR>
	 * <BR>
	 * To reduce cache memory use, L2NPCInstances who don't have skills share the same Calculator
	 * set called <B>NPC_STD_CALCULATOR</B>.<BR>
	 * <BR>
	 */
	public static Calculator[] getStdNPCCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];
		// Add the FuncAtkAccuracy to the Standard Calculator of ACCURACY_COMBAT
		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
		// Add the FuncAtkEvasion to the Standard Calculator of EVASION_RATE
		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
		return std;
	}
	
	public static Calculator[] getStdDoorCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];
		// Add the FuncAtkAccuracy to the Standard Calculator of ACCURACY_COMBAT
		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
		// Add the FuncAtkEvasion to the Standard Calculator of EVASION_RATE
		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
		// SevenSigns PDEF Modifier
		std[Stats.POWER_DEFENCE.ordinal()].addFunc(FuncGatesPDefMod.getInstance());
		// SevenSigns MDEF Modifier
		std[Stats.MAGIC_DEFENCE.ordinal()].addFunc(FuncGatesMDefMod.getInstance());
		return std;
	}
	
	/**
	 * Add basics Func objects to L2PcInstance and L2Summon.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A calculator is created to manage and dynamically calculate the effect of a character
	 * property (ex : MAX_HP, REGENERATE_HP_RATE...). In fact, each calculator is a table of Func
	 * object in which each Func represents a mathematic function : <BR>
	 * <BR>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR>
	 * <BR>
	 * 
	 * @param cha
	 *            L2PcInstance or L2Summon that must obtain basic Func objects
	 */
	public static void addFuncsToNewCharacter(L2Character cha)
	{
		if (cha instanceof L2PcInstance)
		{
			cha.addStatFunc(FuncMaxHpAdd.getInstance());
			cha.addStatFunc(FuncMaxHpMul.getInstance());
			cha.addStatFunc(FuncMaxCpAdd.getInstance());
			cha.addStatFunc(FuncMaxCpMul.getInstance());
			cha.addStatFunc(FuncMaxMpAdd.getInstance());
			cha.addStatFunc(FuncMaxMpMul.getInstance());
			// cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_HP_RATE));
			// cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_CP_RATE));
			// cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_MP_RATE));
			cha.addStatFunc(FuncBowAtkRange.getInstance());
			cha.addStatFunc(FuncCrossBowAtkRange.getInstance());
			// cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.POWER_ATTACK));
			// cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.POWER_DEFENCE));
			// cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.MAGIC_DEFENCE));
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());
			cha.addStatFunc(FuncMoveSpeed.getInstance());
			cha.addStatFunc(FuncHennaSTR.getInstance());
			cha.addStatFunc(FuncHennaDEX.getInstance());
			cha.addStatFunc(FuncHennaINT.getInstance());
			cha.addStatFunc(FuncHennaMEN.getInstance());
			cha.addStatFunc(FuncHennaCON.getInstance());
			cha.addStatFunc(FuncHennaWIT.getInstance());
		}
		else if (cha instanceof L2PetInstance)
		{
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
		}
		else if (cha instanceof L2Summon)
		{
			// cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_HP_RATE));
			// cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_MP_RATE));
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
		}
	}
	
	/**
	 * Calculate the HP regen rate (base + modifiers).<BR>
	 */
	public static final double calcHpRegen(L2Character cha)
	{
		double init = cha.getTemplate().baseHpReg;
		if (init <= 0)
			return 0;
		double regen = cha.calcStat(Stats.REGENERATE_HP_RATE, init, null, null);
		if (regen <= 0)
			return 0;
		double hpRegenMultiplier = cha.isRaid() ? Config.RAID_HP_REGEN_MULTIPLIER : Config.HP_REGEN_MULTIPLIER;
		double hpRegenBonus = 0;
		if (Config.L2JMOD_CHAMPION_ENABLE && cha.isChampion())
			hpRegenMultiplier *= Config.L2JMOD_CHAMPION_HP_REGEN;
		if (cha instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) cha;
			if (!player.isInOlympiadMode())
			{
				// Calculate correct baseHpReg value for certain level of PC
				regen += (player.getLevel(true) > 10) ? ((player.getLevel(true) - 1) / 10.0) : 0.5;
				// SevenSigns Festival modifier
				if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
					hpRegenMultiplier *= calcFestivalRegenModifier(player);
				else
				{
					double siegeModifier = calcSiegeRegenModifer(player);
					if (siegeModifier > 0)
						hpRegenMultiplier *= siegeModifier;
				}
				// Mother Tree effect is calculated at last
				if (player.isInsideZone(L2Character.ZONE_MOTHERTREE))
					hpRegenBonus += 2;
				else if (player.getClan() != null)
				{
					if (player.isInsideZone(L2Character.ZONE_CLANHALL))
					{
						int clanHallIndex = player.getClan().getHasHideout();
						if (clanHallIndex > 0)
						{
							ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
							if (clansHall != null)
								if (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null)
									hpRegenMultiplier *= 1 + (double) clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100;
						}
					}
					else if (player.isInsideZone(L2Character.ZONE_CASTLE))
					{
						int castleIndex = player.getClan().getHasCastle();
						if (castleIndex > 0)
						{
							Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
							if (castle != null)
								if (castle.getFunction(Castle.FUNC_RESTORE_HP) != null)
									hpRegenMultiplier *= 1 + (double) castle.getFunction(Castle.FUNC_RESTORE_HP).getLvl() / 100;
						}
					}
					else if (player.isInsideZone(L2Character.ZONE_FORT))
					{
						int fortIndex = player.getClan().getHasFort();
						if (fortIndex > 0)
						{
							Fort fort = FortManager.getInstance().getFortById(fortIndex);
							if (fort != null)
								if (fort.getFunction(Fort.FUNC_RESTORE_HP) != null)
									hpRegenMultiplier *= 1 + (double) fort.getFunction(Fort.FUNC_RESTORE_HP).getLvl() / 100;
						}
					}
				}
			}
			if (player.isInCombat())
			{
				if (player.isRunning())
					hpRegenMultiplier *= 0.7; // Running
			}
			else
			{
				if (!player.isCursedWeaponEquipped())
				{
					if (player.isSitting()) // sitting
					{
						if (!player.isInHellbound() && !player.isInOlympiadMode())
							hpRegenMultiplier *= 24;
						else
							hpRegenMultiplier *= 6;
					}
					else if (player.isRunning())
						hpRegenMultiplier *= 0.7; // Running
					else if (!player.isInHellbound() && !player.isInOlympiadMode())
						hpRegenMultiplier *= 5; // standing still
				}
			}
			// Add CON bonus
			regen *= cha.getLevelMod() * CONbonus[cha.getCON()];
		}
		else if (cha instanceof L2PetInstance)
			regen = ((L2PetInstance) cha).getPetData().getPetRegenHP();
		return regen * hpRegenMultiplier + hpRegenBonus;
	}
	
	/**
	 * Calculate the MP regen rate (base + modifiers).<BR>
	 */
	public static final double calcMpRegen(L2Character cha)
	{
		double init = cha.getTemplate().baseMpReg;
		if (init <= 0)
			return 0;
		double regen = cha.calcStat(Stats.REGENERATE_MP_RATE, init, null, null);
		if (regen <= 0)
			return 0;
		double mpRegenMultiplier = cha.isRaid() ? Config.RAID_MP_REGEN_MULTIPLIER : Config.MP_REGEN_MULTIPLIER;
		double mpRegenBonus = 0;
		if (cha instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) cha;
			if (!player.isInOlympiadMode())
			{
				// Calculate correct baseMpReg value for certain level of PC
				regen += 0.3 * ((player.getLevel(true) - 1) / 10.0);
				// Add MEN bonus
				regen *= cha.getLevelMod() * MENbonus[cha.getMEN()];
				// SevenSigns Festival modifier
				if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
					mpRegenMultiplier *= calcFestivalRegenModifier(player);
				// Mother Tree effect is calculated at last
				if (player.isInsideZone(L2Character.ZONE_MOTHERTREE))
					mpRegenBonus += 1;
				else if (player.getClan() != null)
				{
					if (player.isInsideZone(L2Character.ZONE_CLANHALL))
					{
						int clanHallIndex = player.getClan().getHasHideout();
						if (clanHallIndex > 0)
						{
							ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
							if (clansHall != null)
								if (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null)
									mpRegenMultiplier *= 1 + (double) clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100;
						}
					}
					else if (player.isInsideZone(L2Character.ZONE_CASTLE))
					{
						int castleIndex = player.getClan().getHasCastle();
						if (castleIndex > 0)
						{
							Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
							if (castle != null)
								if (castle.getFunction(Castle.FUNC_RESTORE_MP) != null)
									mpRegenMultiplier *= 1 + (double) castle.getFunction(Castle.FUNC_RESTORE_MP).getLvl() / 100;
						}
					}
					else if (player.isInsideZone(L2Character.ZONE_FORT))
					{
						int fortIndex = player.getClan().getHasFort();
						if (fortIndex > 0)
						{
							Fort fort = FortManager.getInstance().getFortById(fortIndex);
							if (fort != null)
								if (fort.getFunction(Fort.FUNC_RESTORE_MP) != null)
									mpRegenMultiplier *= 1 + (double) fort.getFunction(Fort.FUNC_RESTORE_MP).getLvl() / 100;
						}
					}
				}
			}
			final boolean combat = player.isInCombat();
			final boolean running = player.isRunning();
			final boolean moving = player.isMoving();
			final boolean sitting = player.isSitting();
			if (!player.isCursedWeaponEquipped())
			{
				if (sitting)
				{
					if (!player.isInHellbound() && !player.isInOlympiadMode())
						mpRegenMultiplier *= 10;
					else
						mpRegenMultiplier *= 5;
				}
				else if (moving)
				{
					if (running)
						mpRegenMultiplier *= 0.5;
					else
						mpRegenMultiplier *= 1;
				}
				else // standing in place
				{
					mpRegenMultiplier *= 2;
				}
			}
			if (combat)
				mpRegenMultiplier *= 0.5;
			final double percRemaining = player.getCurrentMp() / player.getMaxMp();
			if (percRemaining < 0.02)
				mpRegenMultiplier *= 0.33;
			else if (percRemaining < 0.2)
				mpRegenMultiplier *= 0.5;
			else if (percRemaining < 0.5)
				mpRegenMultiplier *= 0.75;
		}
		else if (cha instanceof L2PetInstance)
			regen = ((L2PetInstance) cha).getPetData().getPetRegenMP();
		if (regen < 1)
			regen = 1;
		return regen * mpRegenMultiplier + mpRegenBonus;
	}
	
	/**
	 * Calculate the CP regen rate (base + modifiers).<BR>
	 * <BR>
	 */
	public static final double calcCpRegen(L2Character cha)
	{
		double init = cha.getTemplate().baseHpReg;
		if (init <= 0)
			return 0;
		double regen = cha.calcStat(Stats.REGENERATE_CP_RATE, init, null, null);
		if (regen <= 0)
			return 0;
		double cpRegenMultiplier = Config.CP_REGEN_MULTIPLIER;
		if (cha instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) cha;
			if (!player.isInOlympiadMode())
			{
				// Calculate correct baseHpReg value for certain level of PC
				regen += (player.getLevel(true) > 10) ? ((player.getLevel(true) - 1) / 10.0) : 0.5;
				if (!player.isCursedWeaponEquipped())
				{
					// Calculate Movement bonus
					if (player.isSitting() && !player.isInCombat())
					{
						if (!player.isInHellbound() && !player.isInOlympiadMode())
							cpRegenMultiplier *= 18; // Sitting
						else
							cpRegenMultiplier *= 6;
					}
					else if (!player.isMoving())
						cpRegenMultiplier *= 1.5; // Staying
					else if (player.isRunning())
						cpRegenMultiplier *= 0.7; // Running
				}
			}
		}
		// Apply CON bonus
		regen *= cha.getLevelMod() * CONbonus[cha.getCON()];
		if (regen < 1)
			regen = 1;
		return regen * cpRegenMultiplier;
	}
	
	@SuppressWarnings("deprecation")
	public static final double calcFestivalRegenModifier(L2PcInstance activeChar)
	{
		final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(activeChar);
		final int oracle = festivalInfo[0];
		final int festivalId = festivalInfo[1];
		int[] festivalCenter;
		// If the player isn't found in the festival, leave the regen rate as it is.
		if (festivalId < 0)
			return 0;
		// Retrieve the X and Y coords for the center of the festival arena the player is in.
		if (oracle == SevenSigns.CABAL_DAWN)
			festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		else
			festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];
		// Check the distance between the player and the player spawn point, in the center of the
		// arena.
		double distToCenter = activeChar.getDistance(festivalCenter[0], festivalCenter[1]);
		if (Config.DEBUG)
			_log.info("Distance: " + distToCenter + ", RegenMulti: " + (distToCenter * 2.5) / 50);
		return 1.0 - (distToCenter * 0.0005); // Maximum Decreased Regen of ~ -65%;
	}
	
	public static final double calcSiegeRegenModifer(L2PcInstance activeChar)
	{
		if (activeChar == null || activeChar.getClan() == null)
			return 0;
		Siege siege = SiegeManager.getInstance().getSiege(activeChar.getPosition().getX(), activeChar.getPosition().getY(), activeChar.getPosition().getZ());
		if (siege == null || !siege.getIsInProgress())
			return 0;
		L2SiegeClan siegeClan = siege.getAttackerClan(activeChar.getClan().getClanId());
		if (siegeClan == null || siegeClan.getFlag().isEmpty() || !Util.checkIfInRange(200, activeChar, siegeClan.getFlag().get(0), true))
			return 0;
		return 1.5; // If all is true, then modifer will be 50% more
	}
	
	/** Calculate blow damage based on cAtk */
	@SuppressWarnings("incomplete-switch")
	public static double calcBlowDamage(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean ss, boolean crit)
	{
		if (skill == null)
		{
			_log.warning("LOL WTF calc blow dmg but no skill sent");
			return 0;
		}
		double damage = attacker.getPAtk(target);
		damage += calcValakasAttribute(attacker, target, skill);
		damage *= 2; // soulshot
		double power = skill.getPower(attacker);
		if (skill.isStaticPower())
			power = 6000;
		else if (skill.getSSBoost() > 1)
		{
			damage += damage * (skill.getSSBoost() - 1) * 0.3;
			power += power * (skill.getSSBoost() - 1) * 0.7;
		}
		double defence = target.getPDef(attacker);
		final int pdefIgnore = (int) attacker.calcStat(Stats.PDEF_IGNORE, 0, target, skill);
		if (pdefIgnore > 0)
		{
			if (defence > pdefIgnore)
				defence = pdefIgnore;
		}
		defence = attacker.calcStat(Stats.PDEF_REDUCE, defence, target, skill);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				defence += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}
		damage = (damage + power) * attacker.getCriticalDmg(target, 1, skill);
		damage *= calcElemental(attacker, target, skill);
		damage += attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 7;
		if (crit)
		{
			damage *= target.calcStat(Stats.CRIT_VULN, target.getTemplate().baseCritVuln, target, skill);
		}
		// defence modifier depending of the attacker weapon
		final L2Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		if (weapon == null)
		{
			if (attacker instanceof L2Summon)
			{
				stat = Stats.BLUNT_WPN_VULN;
				try
				{
					if (((L2Summon) attacker).getName().contains("Feline King"))
						stat = Stats.SWORD_WPN_VULN;
					else if (((L2Summon) attacker).getName().contains("Spectral Lord"))
						stat = Stats.DUALDAGGER_WPN_VULN;
					else
						stat = Stats.BLUNT_WPN_VULN;
				}
				catch (Exception e)
				{}
			}
		}
		boolean dagger = false;
		if (weapon != null)
		{
			damage *= calcWeaponResistanceModifier(weapon, target, skill);
			switch (weapon.getItemType())
			{
				case BOW:
					stat = Stats.BOW_WPN_VULN;
					break;
				case CROSSBOW:
					stat = Stats.CROSSBOW_WPN_VULN;
					break;
				case BLUNT:
					stat = Stats.BLUNT_WPN_VULN;
					break;
				case DAGGER:
					stat = Stats.DAGGER_WPN_VULN;
					dagger = true;
					break;
				case DUAL:
					stat = Stats.DUAL_WPN_VULN;
					break;
				case DUALFIST:
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				case ETC:
					stat = Stats.ETC_WPN_VULN;
					break;
				case FIST:
					stat = Stats.FIST_WPN_VULN;
					break;
				case POLE:
					stat = Stats.POLE_WPN_VULN;
					break;
				case SWORD:
					stat = Stats.SWORD_WPN_VULN;
					break;
				case BIGSWORD:
					stat = Stats.BIGSWORD_WPN_VULN;
					break;
				case BIGBLUNT:
					stat = Stats.BIGBLUNT_WPN_VULN;
					break;
				case DUAL_DAGGER:
					stat = Stats.DUALDAGGER_WPN_VULN;
					dagger = true;
					break;
				case RAPIER:
					stat = Stats.RAPIER_WPN_VULN;
					break;
				case ANCIENT_SWORD:
					stat = Stats.ANCIENT_WPN_VULN;
					break;
				case PET:
					stat = Stats.PET_WPN_VULN;
					break;
			}
		}
		if (stat != null)
		{
			damage *= target.calcStat(stat, 1, target, null);
			if (target instanceof L2Npc)
			{
				// get the natural vulnerability for the template
				damage *= ((L2Npc) target).getTemplate().getVulnerability(stat);
			}
		}
		damage *= 70. / defence;
		damage += Rnd.get() * attacker.getRandomDamage(target);
		// Dmg bonusses in PvP fight
		if (attacker instanceof L2Playable && target instanceof L2Playable)
		{
			damage *= target.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_VUL, 1, attacker, null);
			damage *= attacker.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_DMG, 0.86, target, null);
			damage *= skill.getPvpMulti();
			if (attacker.getActingPlayer().getRace() == target.getActingPlayer().getRace())
			{
				damage *= target.getActingPlayer().calcStat(Stats.SAME_RACE_DMG_VUL, 1, null, null);
			}
			else if (attacker.getActingPlayer().hatesRace(target))
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.HATED_RACE_DMG_BOOST, 1, null, null);
			}
		}
		else
		{
			if (attacker instanceof L2Playable && target instanceof L2MonsterInstance)
			{
				switch (((L2Npc) target).getTemplate().getRace())
				{
					case UNDEAD:
						damage *= attacker.getPAtkUndead(target);
						break;
					case DEMON:
						damage *= attacker.getPAtkDemons(target);
						break;
					case BEAST:
						damage *= attacker.getPAtkMonsters(target);
						break;
					case ANIMAL:
						damage *= attacker.getPAtkAnimals(target);
						break;
					case PLANT:
						damage *= attacker.getPAtkPlants(target);
						break;
					case DRAGON:
						damage *= attacker.getPAtkDragons(target);
						break;
					case BUG:
						damage *= attacker.getPAtkInsects(target);
						break;
					case GIANT:
						damage *= attacker.getPAtkGiants(target);
						break;
					case MAGICCREATURE:
					case SPIRIT:
						damage *= attacker.getPAtkMCreatures(target);
						break;
					default:
						break;
				}
				if (!dagger)
					damage *= attacker.getActingPlayer().calcStat(Stats.PVM_DAMAGE, 2.0, null, null); // the 2 here already factors in the PVM skill boost
				else
					damage *= attacker.getActingPlayer().calcStat(Stats.PVM_DAMAGE, 1.23, null, null); // the 2 here already factors in the PVM skill boost
				damage *= skill.getPvmMulti();
			}
			else if (attacker instanceof L2MonsterInstance && target instanceof L2Playable)
			{
				damage *= target.getActingPlayer().calcStat(Stats.PVM_DAMAGE_VUL, 1, null, null);
			}
		}
		damage *= attacker.calcStat(Stats.SKILL_DAM_MULTI, 1, target, skill);
		double hpdam = attacker.calcStat(Stats.INC_DAM_HP, 0, target, skill);
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentHp() / target.getMaxHp()) * hpdam;
		hpdam = attacker.calcStat(Stats.INC_DAM_MP, 0, target, skill);
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentMp() / target.getMaxMp()) * hpdam;
		if (target instanceof L2PcInstance)
		{
			hpdam = attacker.calcStat(Stats.INC_DAM_CP, 0, target, skill);
			if (hpdam != 0)
				damage += damage * (1 - target.getCurrentCp() / target.getMaxCp()) * hpdam;
			final L2PcInstance targetPlayer = (L2PcInstance) target;
			/*
			 * final L2Armor armor = targetPlayer.getActiveChestArmorItem();
			 */
			/*
			 * if (armor != null)
			 * {
			 */
			if (targetPlayer.isWearingHeavyArmor())
			{
				damage *= attacker.calcStat(Stats.HEAVY_DAM_MUL, 1, target, skill);
			}
			else if (targetPlayer.isWearingLightArmor())
			{
				damage *= attacker.calcStat(Stats.LIGHT_DAM_MUL, 1, target, skill);
			}
			else
			{
				damage *= attacker.calcStat(Stats.ROBE_DAM_MUL, 1, target, skill);
				damage += 150;
			}
			/* } */
		}
		if (attacker.isBehind(target))
			damage = target.calcStat(Stats.POWER_DEFENCE_BEHIND, damage, attacker, skill);
		if (attacker instanceof L2Attackable && target instanceof L2PcInstance && target.getActingPlayer().isSitting())
			damage *= 3;

		if (attacker instanceof L2PcInstance && target instanceof L2Playable)
			damage = calcConfigsPvPDamage(attacker.getActingPlayer(), damage);
		else if (attacker instanceof L2PcInstance && target instanceof L2MonsterInstance)
			damage = calcConfigsPvEDamage(attacker.getActingPlayer(), damage);
		damage += attacker.calcStat(Stats.DMG_ADD, 0, target, skill);
		damage -= target.calcStat(Stats.DMG_REMOVE, 0, attacker, skill);
		if (shld > 0)
			damage -= target.calcStat(Stats.DMG_REMOVE_SHIELD, 0, attacker, skill);
		damage = damage < 1 ? 1. : damage;
		if (target.calcStat(Stats.PDAM_MAX, 0, target, skill) > 0)
		{
			damage = Math.min(damage, target.calcStat(Stats.PDAM_MAX, 0, attacker, skill));
		}
		return damage;
	}
	
	/**
	 * Calculated damage caused by ATTACK of attacker on target, called separatly for each weapon,
	 * if dual-weapon is used.
	 * 
	 * @param attacker
	 *            player or NPC that makes ATTACK
	 * @param target
	 *            player or NPC, target of ATTACK
	 * @param miss
	 *            one of ATTACK_XXX constants
	 * @param crit
	 *            if the ATTACK have critical success
	 * @param dual
	 *            if dual weapon is used
	 * @param ss
	 *            if weapon item was charged by soulshot
	 * @return damage points
	 */
	@SuppressWarnings("incomplete-switch")
	public static final double calcPhysDam(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean crit, boolean dual, boolean ss)
	{
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		final int pdefIgnore = (int) attacker.calcStat(Stats.PDEF_IGNORE, 0, target, skill);
		if (pdefIgnore > 0)
		{
			if (defence > pdefIgnore)
				defence = pdefIgnore;
		}
		defence = attacker.calcStat(Stats.PDEF_REDUCE, defence, target, skill);
		damage += calcValakasAttribute(attacker, target, skill);
		if (ss)
			damage *= 2;
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				if (!Config.ALT_GAME_SHIELD_BLOCKS)
					defence += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1.;
		}
		if (skill != null)
		{
			if (skill.isStaticPower())
				damage = 6000;
			else if (attacker instanceof L2TrapInstance)
				damage = (attacker.getActingPlayer().getMAtk(target, skill) + attacker.getActingPlayer().getPAtk(target)) * 1.6;
			double power = skill.getPower(attacker);
			float ssboost = skill.getSSBoost();
			if (ssboost <= 1)
			{
				damage += power;
			}
			else
			{
				damage += damage * (skill.getSSBoost() - 1) * 0.25;
				power += power * (skill.getSSBoost() - 1) * 0.72;
				damage += power;
			}
			if (target instanceof L2MonsterInstance)
			{
				switch (skill.getSkillType())
				{
					case PDAM:
					case FATAL:
					case CHARGEDAM:
					{
						/* if (skill.getId() != 345 && skill.getId() != 346 && skill.getId() != 939) */
						damage *= 1.5;
					}
				}
			}
			/*
			 * else
			 * {
			 * if (skill.getId() == 345 && skill.getId() == 346 && skill.getId() == 939)
			 * damage *= 0.8;
			 * }
			 */
			damage *= attacker.calcStat(Stats.SKILL_DAM_MULTI, 1, target, skill);
		}
		else
		{
			if (attacker instanceof L2PcInstance)
			{
				int charges = attacker.getActingPlayer().getCharges();
				if (charges >= 1)
				{
					final double damMulti = attacker.calcStat(Stats.INC_PHYSDAM_CHARGES, 0, null, null);
					if (damMulti != 0)
						damage *= 1 + (damMulti * charges);
				}
				else
				{
					charges = attacker.getActingPlayer().getSouls();
					if (charges >= 1)
					{
						final double damMulti = attacker.calcStat(Stats.INC_PHYSDAM_SOULS, 0, null, null);
						if (damMulti != 0)
							damage *= 1 + (damMulti * charges);
					}
				}
			}
		}
		// defence modifier depending of the attacker weapon
		final L2Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		/* boolean rangedWep = false; */
		if (weapon != null)
		{
			damage *= calcWeaponResistanceModifier(weapon, target, skill);
			switch (weapon.getItemType())
			{
				case BOW:
					stat = Stats.BOW_WPN_VULN;
					/* rangedWep = true; */
					break;
				case CROSSBOW:
					stat = Stats.CROSSBOW_WPN_VULN;
					/* rangedWep = true; */
					break;
				case BLUNT:
					stat = Stats.BLUNT_WPN_VULN;
					break;
				case DAGGER:
					stat = Stats.DAGGER_WPN_VULN;
					break;
				case DUAL:
					stat = Stats.DUAL_WPN_VULN;
					break;
				case DUALFIST:
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				case ETC:
					stat = Stats.ETC_WPN_VULN;
					break;
				case FIST:
					stat = Stats.FIST_WPN_VULN;
					break;
				case POLE:
					stat = Stats.POLE_WPN_VULN;
					break;
				case SWORD:
					stat = Stats.SWORD_WPN_VULN;
					break;
				case BIGSWORD:
					stat = Stats.BIGSWORD_WPN_VULN;
					break;
				case BIGBLUNT:
					stat = Stats.BIGBLUNT_WPN_VULN;
					break;
				case DUAL_DAGGER:
					stat = Stats.DUALDAGGER_WPN_VULN;
					break;
				case RAPIER:
					stat = Stats.RAPIER_WPN_VULN;
					break;
				case ANCIENT_SWORD:
					stat = Stats.ANCIENT_WPN_VULN;
					break;
				case PET:
					stat = Stats.PET_WPN_VULN;
					break;
			}
		}
		else
		{
			if (attacker instanceof L2Summon)
			{
				stat = Stats.BLUNT_WPN_VULN;
				try
				{
					if (((L2Summon) attacker).getName().contains("Feline King"))
						stat = Stats.SWORD_WPN_VULN;
					else if (((L2Summon) attacker).getName().contains("Spectral Lord"))
						stat = Stats.DUALDAGGER_WPN_VULN;
					else
						stat = Stats.BLUNT_WPN_VULN;
				}
				catch (Exception e)
				{}
			}
		}
		if (crit)
		{
			// Crit dmg add is almost useless in normal hits...
			damage += attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 2;
			// Finally retail like formula
			damage = attacker.getCriticalDmg(target, 1.66, skill) * target.calcStat(Stats.CRIT_VULN, target.getTemplate().baseCritVuln, target, null) * (70 * damage / defence);
		}
		else
		{
			damage = 70 * damage / defence;
		}
		if (stat != null)
		{
			// get the vulnerability due to skills (buffs, passives, toggles, etc)
			damage *= target.calcStat(stat, 1, target, null);
			if (target instanceof L2Npc)
			{
				// get the natural vulnerability for the template
				damage *= ((L2Npc) target).getTemplate().getVulnerability(stat);
				if (stat == Stats.BOW_WPN_VULN && target instanceof L2RaidBossInstance)
					damage *= 0.75;
			}
			float rangedAtkBoost = 0;
			if (skill == null)
				rangedAtkBoost = (float) attacker.calcStat(Stats.RANGE_DMG_DIST_BOOST, 0, target, null); // given in PERCENT, non skill
			else
				rangedAtkBoost = (float) attacker.calcStat(Stats.RANGE_DMG_DIST_BOOST_SKILL, 0, target, skill); // given in PERCENT
			if (rangedAtkBoost > 0)
			{
				int distToTarg = (int) Util.calculateDistance(attacker, target, false);
				if (distToTarg > 40)
				{
					distToTarg -= 40;
					distToTarg = (int) Math.pow(distToTarg, 1.15);
					damage *= distToTarg * rangedAtkBoost + 1;
				}
			}
		}
		damage += Rnd.nextDouble() * damage / 10;
		if (shld > 0 && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if (damage < 0)
				damage = 0;
		}
		if (target instanceof L2Npc)
		{
			switch (((L2Npc) target).getTemplate().getRace())
			{
				case UNDEAD:
					damage *= attacker.getPAtkUndead(target);
					break;
				case DEMON:
					damage *= attacker.getPAtkDemons(target);
					break;
				case BEAST:
					damage *= attacker.getPAtkMonsters(target);
					break;
				case ANIMAL:
					damage *= attacker.getPAtkAnimals(target);
					break;
				case PLANT:
					damage *= attacker.getPAtkPlants(target);
					break;
				case DRAGON:
					damage *= attacker.getPAtkDragons(target);
					break;
				case BUG:
					damage *= attacker.getPAtkInsects(target);
					break;
				case GIANT:
					damage *= attacker.getPAtkGiants(target);
					break;
				case MAGICCREATURE:
				case SPIRIT:
					damage *= attacker.getPAtkMCreatures(target);
					break;
				default:
					break;
			}
		}
		else if (target instanceof L2PcInstance)
		{
			if (((L2PcInstance) target).isWearingHeavyArmor())
			{
				damage *= attacker.calcStat(Stats.HEAVY_DAM_MUL, 1, null, skill);
			}
			else if (((L2PcInstance) target).isWearingLightArmor())
			{
				damage *= attacker.calcStat(Stats.LIGHT_DAM_MUL, 1, null, skill);
			}
			else/* if (((L2PcInstance)target).isWearingMagicArmor()) */
			{
				damage *= attacker.calcStat(Stats.ROBE_DAM_MUL, 1, null, skill);
				if (weapon != null && !attacker.isTransformed())
				{
					if (weapon.getItemType() == L2WeaponType.BOW)
					{
						damage *= 1.06;
						damage += 170;
					}
					else if (weapon.getItemType() == L2WeaponType.CROSSBOW)
					{
						damage *= 1.05;
						damage += 90;
					}
				}
				if (attacker instanceof L2RaidBossInstance)
					damage *= 2;
			}
		}
		if (damage > 0 && damage < 1)
		{
			damage = 1;
		}
		else if (damage < 0)
		{
			damage = 0;
		}
		// Dmg bonusses in PvP fight
		if (attacker instanceof L2Playable && target instanceof L2Playable || target.getTargetId() == 95600 || target.getTargetId() == 95601)
		{
			if (skill == null)
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVP_PHYSICAL_DMG, 0.87, target, null);
				damage *= target.getActingPlayer().calcStat(Stats.PVP_PHYSICAL_VUL, 1, attacker, null);
			}
			else
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_DMG, 0.86, target, skill);
				damage *= target.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_VUL, 1, attacker, skill);
				damage *= skill.getPvpMulti();
			}
			if (attacker.getActingPlayer().getRace() == target.getActingPlayer().getRace())
			{
				damage *= target.getActingPlayer().calcStat(Stats.SAME_RACE_DMG_VUL, 1, null, null);
			}
			else if (attacker.getActingPlayer().hatesRace(target))
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.HATED_RACE_DMG_BOOST, 1, null, null);
			}
		}
		else
		{
			if (attacker instanceof L2Playable && target instanceof L2MonsterInstance)
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVM_DAMAGE, 1, target, skill);
				if (skill != null)
					damage *= skill.getPvmMulti();
			}
			else if (attacker instanceof L2MonsterInstance && target instanceof L2Playable)
			{
				damage *= target.getActingPlayer().calcStat(Stats.PVM_DAMAGE_VUL, 1, target, skill);
				if (target instanceof L2Summon)
				{
					if (attacker instanceof L2RaidBossInstance)
						damage *= 2.2;
					else
						damage *= 1.5;
					damage += 100;
				}
			}
		}
		damage *= calcElemental(attacker, target, skill);
		// DAMAGE ELEMENT DEBUG START
		boolean enableDebug = false;
		if (enableDebug)
		{
			double elementdmg = calcElemental(attacker, target, skill);
			int finalelementdmg = (int) ((damage * (double) elementdmg) - damage);
			int damage2 = (int) (damage);
			int puredamage = damage2 - finalelementdmg;
			byte atkEleType = attacker.getAttackElement();
			String atkEleValue = String.valueOf(attacker.getAttackElementValue(atkEleType));
			byte TargetAtkEleType2 = target.getAttackElement();
			String TargetDefEleValue = String.valueOf(target.getDefenseElementValue(atkEleType));
			Byte EleType = attacker.getAttackElement();
			String EleTypeStr = "";
			switch (EleType)
			{
				case -1:
					EleTypeStr = "None";
					break;
				case 0:
					EleTypeStr = "Fire";
					break;
				case 1:
					EleTypeStr = "Water";
					break;
				case 2:
					EleTypeStr = "Wind";
					break;
				case 3:
					EleTypeStr = "Earth";
					break;
				case 4:
					EleTypeStr = "Holy";
					break;
				case 5:
					EleTypeStr = "Dark";
					break;
				default:
					break;
			}
			attacker.sendMessage("Total Damage: " + String.valueOf(damage2) + " Pure Damage: " + String.valueOf(puredamage));
			attacker.sendMessage(" Elemental Damage: " + String.valueOf(finalelementdmg) + " Element Percent: " + elementdmg);
			attacker.sendMessage("Attacker Element Type: " + EleTypeStr + " - Atk Ele Value: " + atkEleValue);
			attacker.sendMessage("Target's Element Type: " + EleTypeStr + " - Def Ele Value: " + TargetDefEleValue);
		}
		// DAMAGE ELEMENT DEBUG END
		double hpdam = attacker.calcStat(Stats.INC_DAM_HP, 0, target, skill);
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentHp() / target.getMaxHp()) * hpdam;
		hpdam = attacker.calcStat(Stats.INC_DAM_MP, 0, target, skill);
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentMp() / target.getMaxMp()) * hpdam;
		if (target instanceof L2PcInstance)
		{
			hpdam = attacker.calcStat(Stats.INC_DAM_CP, 0, target, skill);
			if (hpdam != 0)
				damage += damage * (1 - target.getCurrentCp() / target.getMaxCp()) * hpdam;
		}
		if (attacker.isBehind(target))
			damage = target.calcStat(Stats.POWER_DEFENCE_BEHIND, damage, attacker, skill);
		if (attacker instanceof L2Attackable && target instanceof L2PcInstance && target.getActingPlayer().isSitting())
			damage *= 3;
		if (attacker instanceof L2PcInstance && target instanceof L2Playable)
			damage = calcConfigsPvPDamage(attacker.getActingPlayer(), damage);
		else if (attacker instanceof L2PcInstance && target instanceof L2MonsterInstance)
			damage = calcConfigsPvEDamage(attacker.getActingPlayer(), damage);
		if (skill != null)
		{
			damage += attacker.calcStat(Stats.DMG_ADD, 0, target, skill);
			damage -= target.calcStat(Stats.DMG_REMOVE, 0, attacker, skill);
			if (shld > 0)
				damage -= target.calcStat(Stats.DMG_REMOVE_SHIELD, 0, attacker, skill);
		}
		if (damage < 0)
			return 0;
		if (target.calcStat(Stats.PDAM_MAX, 0, target, skill) > 0)
		{
			damage = Math.min(damage, target.calcStat(Stats.PDAM_MAX, 0, attacker, skill));
		}
		return damage;
	}
	
	public static final double calcConfigsPvPDamage(L2PcInstance player, double damage)
	{
		String ActiveClass = player.getClassId().getName();
		switch (ActiveClass)
		{
			case "Duelist":
				damage *= Config.PVP_CLASS_BALANCE_DUELIST;
				break;
			case "Dreadnought":
				damage *= Config.PVP_CLASS_BALANCE_DREADNOUGHT;
				break;
			case "Phoenix Knight":
				damage *= Config.PVP_CLASS_BALANCE_PHOENIX_KNIGHT;
				break;
			case "Hell Knight":
				damage *= Config.PVP_CLASS_BALANCE_HELL_KNIGHT;
				break;
			case "Sagittarius":
				damage *= Config.PVP_CLASS_BALANCE_SAGITTARIUS;
				break;
			case "Adventurer":
				damage *= Config.PVP_CLASS_BALANCE_ADVENTURER;
				break;
			case "Hierophant":
				damage *= Config.PVP_CLASS_BALANCE_HIEROPHANT;
				break;
			case "Eva Templar":
				damage *= Config.PVP_CLASS_BALANCE_EVA_TEMPLAR;
				break;
			case "Sword Muse":
				damage *= Config.PVP_CLASS_BALANCE_SWORD_MUSE;
				break;
			case "Wind Rider":
				damage *= Config.PVP_CLASS_BALANCE_WIND_RIDER;
				break;
			case "Moonlight Sentinel":
				damage *= Config.PVP_CLASS_BALANCE_MOONLIGHT_SENTINEL;
				break;
			case "Shillien Templar":
				damage *= Config.PVP_CLASS_BALANCE_SHILLIEN_TEMPLAR;
				break;
			case "Spectral Dancer":
				damage *= Config.PVP_CLASS_BALANCE_SPECTRAL_DANCER;
				break;
			case "Ghost Hunter":
				damage *= Config.PVP_CLASS_BALANCE_GHOST_HUNTER;
				break;
			case "Ghost Sentinel":
				damage *= Config.PVP_CLASS_BALANCE_GHOST_SENTINEL;
				break;
			case "Titan":
				damage *= Config.PVP_CLASS_BALANCE_TITAN;
				break;
			case "Grand Khauatari":
				damage *= Config.PVP_CLASS_BALANCE_GRAND_KHAUATARI;
				break;
			case "Fortune Seeker":
				damage *= Config.PVP_CLASS_BALANCE_FORTUNE_SEEKER;
				break;
			case "Maestro":
				damage *= Config.PVP_CLASS_BALANCE_MAESTRO;
				break;
			case "Doombringer":
				damage *= Config.PVP_CLASS_BALANCE_DOOMBRINGER;
				break;
			case "Male Soulhound":
				damage *= Config.PVP_CLASS_BALANCE_MALE_SOULHOUND;
				break;
			case "Female Soulhound":
				damage *= Config.PVP_CLASS_BALANCE_FEMALE_SOULHOUND;
				break;
			case "Trickster":
				damage *= Config.PVP_CLASS_BALANCE_TRICKSTER;
				break;
			case "Inspector":
				damage *= Config.PVP_CLASS_BALANCE_INSPECTOR;
				break;
			case "Judicator":
				damage *= Config.PVP_CLASS_BALANCE_JUDICATOR;
				break;
			case "Archmage":
				damage *= Config.PVP_CLASS_BALANCE_ARCHMAGE;
				break;
			case "Soultaker":
				damage *= Config.PVP_CLASS_BALANCE_SOULTAKER;
				break;
			case "Arcana Lord":
				damage *= Config.PVP_CLASS_BALANCE_ARCANA_LORD;
				break;
			case "Cardinal":
				damage *= Config.PVP_CLASS_BALANCE_CARDINAL;
				break;
			case "Mystic Muse":
				damage *= Config.PVP_CLASS_BALANCE_MYSTIC_MUSE;
				break;
			case "Elemental Master":
				damage *= Config.PVP_CLASS_BALANCE_ELEMENTAL_MASTER;
				break;
			case "Eva Saint":
				damage *= Config.PVP_CLASS_BALANCE_EVA_SAINT;
				break;
			case "Storm Screamer":
				damage *= Config.PVP_CLASS_BALANCE_STORM_SCREAMER;
				break;
			case "Spectral Master":
				damage *= Config.PVP_CLASS_BALANCE_SPECTRAL_MASTER;
				break;
			case "Shillien Saint":
				damage *= Config.PVP_CLASS_BALANCE_SHILLIEN_SAINT;
				break;
			case "Dominator":
				damage *= Config.PVP_CLASS_BALANCE_DOMINATOR;
				break;
			case "Doomcryer":
				damage *= Config.PVP_CLASS_BALANCE_DOOMCRYER;
				break;
		}
		return damage;
	}
	
	public static final double calcConfigsPvEDamage(L2PcInstance player, double damage)
	{
		String ActiveClass = player.getClassId().getName();
		switch (ActiveClass)
		{
			case "Duelist":
				damage *= Config.PVE_CLASS_BALANCE_DUELIST;
				break;
			case "Dreadnought":
				damage *= Config.PVE_CLASS_BALANCE_DREADNOUGHT;
				break;
			case "Phoenix Knight":
				damage *= Config.PVE_CLASS_BALANCE_PHOENIX_KNIGHT;
				break;
			case "Hell Knight":
				damage *= Config.PVE_CLASS_BALANCE_HELL_KNIGHT;
				break;
			case "Sagittarius":
				damage *= Config.PVE_CLASS_BALANCE_SAGITTARIUS;
				break;
			case "Adventurer":
				damage *= Config.PVE_CLASS_BALANCE_ADVENTURER;
				break;
			case "Hierophant":
				damage *= Config.PVE_CLASS_BALANCE_HIEROPHANT;
				break;
			case "Eva Templar":
				damage *= Config.PVE_CLASS_BALANCE_EVA_TEMPLAR;
				break;
			case "Sword Muse":
				damage *= Config.PVE_CLASS_BALANCE_SWORD_MUSE;
				break;
			case "Wind Rider":
				damage *= Config.PVE_CLASS_BALANCE_WIND_RIDER;
				break;
			case "Moonlight Sentinel":
				damage *= Config.PVE_CLASS_BALANCE_MOONLIGHT_SENTINEL;
				break;
			case "Shillien Templar":
				damage *= Config.PVE_CLASS_BALANCE_SHILLIEN_TEMPLAR;
				break;
			case "Spectral Dancer":
				damage *= Config.PVE_CLASS_BALANCE_SPECTRAL_DANCER;
				break;
			case "Ghost Hunter":
				damage *= Config.PVE_CLASS_BALANCE_GHOST_HUNTER;
				break;
			case "Ghost Sentinel":
				damage *= Config.PVE_CLASS_BALANCE_GHOST_SENTINEL;
				break;
			case "Titan":
				damage *= Config.PVE_CLASS_BALANCE_TITAN;
				break;
			case "Grand Khauatari":
				damage *= Config.PVE_CLASS_BALANCE_GRAND_KHAUATARI;
				break;
			case "Fortune Seeker":
				damage *= Config.PVE_CLASS_BALANCE_FORTUNE_SEEKER;
				break;
			case "Maestro":
				damage *= Config.PVE_CLASS_BALANCE_MAESTRO;
				break;
			case "Doombringer":
				damage *= Config.PVE_CLASS_BALANCE_DOOMBRINGER;
				break;
			case "Male Soulhound":
				damage *= Config.PVE_CLASS_BALANCE_MALE_SOULHOUND;
				break;
			case "Female Soulhound":
				damage *= Config.PVE_CLASS_BALANCE_FEMALE_SOULHOUND;
				break;
			case "Trickster":
				damage *= Config.PVE_CLASS_BALANCE_TRICKSTER;
				break;
			case "Inspector":
				damage *= Config.PVE_CLASS_BALANCE_INSPECTOR;
				break;
			case "Judicator":
				damage *= Config.PVE_CLASS_BALANCE_JUDICATOR;
				break;
			case "Archmage":
				damage *= Config.PVE_CLASS_BALANCE_ARCHMAGE;
				break;
			case "Soultaker":
				damage *= Config.PVE_CLASS_BALANCE_SOULTAKER;
				break;
			case "Arcana Lord":
				damage *= Config.PVE_CLASS_BALANCE_ARCANA_LORD;
				break;
			case "Cardinal":
				damage *= Config.PVE_CLASS_BALANCE_CARDINAL;
				break;
			case "Mystic Muse":
				damage *= Config.PVE_CLASS_BALANCE_MYSTIC_MUSE;
				break;
			case "Elemental Master":
				damage *= Config.PVE_CLASS_BALANCE_ELEMENTAL_MASTER;
				break;
			case "Eva Saint":
				damage *= Config.PVE_CLASS_BALANCE_EVA_SAINT;
				break;
			case "Storm Screamer":
				damage *= Config.PVE_CLASS_BALANCE_STORM_SCREAMER;
				break;
			case "Spectral Master":
				damage *= Config.PVE_CLASS_BALANCE_SPECTRAL_MASTER;
				break;
			case "Shillien Saint":
				damage *= Config.PVE_CLASS_BALANCE_SHILLIEN_SAINT;
				break;
			case "Dominator":
				damage *= Config.PVE_CLASS_BALANCE_DOMINATOR;
				break;
			case "Doomcryer":
				damage *= Config.PVE_CLASS_BALANCE_DOOMCRYER;
				break;
		}
		return damage;
	}
	
	public static final double calcMagicDam(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean ss, boolean bss, boolean mcrit)
	{
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		final int mDefIgnore = (int) attacker.calcStat(Stats.MDEF_IGNORE, 0, target, skill);
		if (mDefIgnore > 0)
		{
			if (mDef > mDefIgnore)
				mDef = mDefIgnore;
		}
		mDef = attacker.calcStat(Stats.MDEF_REDUCE, mDef, target, skill);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef(); // kamael
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}
		if (bss)
			mAtk *= 3.5;
		else if (ss)
			mAtk *= 2;
		if (skill.isStaticPower())
			mAtk = 14000;
		else if (attacker instanceof L2TrapInstance)
			mAtk = (attacker.getActingPlayer().getMAtk(target, skill) + attacker.getActingPlayer().getPAtk(target)) * 2.65;
		double damage = 95 * Math.sqrt(mAtk) / mDef * skill.getPower(attacker);
		if ((bss || ss) && skill.getSSBoost() > 0)
			damage *= skill.getSSBoost();
		// Failure calculation
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess())
		{
			if (attacker instanceof L2PcInstance)
			{
				if (skill.getSkillType() == L2SkillType.DRAIN)
					attacker.sendPacket(new SystemMessage(SystemMessageId.DRAIN_HALF_SUCCESFUL));
				else
					attacker.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
				damage /= 2;
			}
		}
		else if (mcrit)
		{
			if (attacker instanceof L2TrapInstance)
				damage *= attacker.getActingPlayer().calcStat(Stats.MAGIC_CRITICAL_DAMAGE, 1.4, target, skill);
			else
				damage *= attacker.calcStat(Stats.MAGIC_CRITICAL_DAMAGE, 1.75, target, skill);
		}
		damage += Rnd.get() * attacker.getRandomDamage(target);
		// Pvp bonusses for dmg
		if (attacker instanceof L2Playable && target instanceof L2Playable)
		{
			if (skill.isMagic())
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVP_MAGICAL_DMG, 0.9, target, null);
				damage *= target.getActingPlayer().calcStat(Stats.PVP_MAGICAL_VUL, 1, attacker, null);
			}
			else
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_DMG, 0.86, target, null);
				damage *= target.getActingPlayer().calcStat(Stats.PVP_PHYS_SKILL_VUL, 1, attacker, null);
			}
			damage *= skill.getPvpMulti();
			if (attacker.getActingPlayer().getRace() == target.getActingPlayer().getRace())
			{
				damage *= target.getActingPlayer().calcStat(Stats.SAME_RACE_DMG_VUL, 1, null, null);
			}
			else if (attacker.getActingPlayer().hatesRace(target))
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.HATED_RACE_DMG_BOOST, 1, null, null);
			}
		}
		else
		{
			if (attacker instanceof L2Playable && target instanceof L2MonsterInstance)
			{
				damage *= attacker.getActingPlayer().calcStat(Stats.PVM_DAMAGE, 1, target, null);
				damage *= skill.getPvmMulti();
			}
			else if (attacker instanceof L2MonsterInstance && target instanceof L2Playable)
			{
				damage *= target.getActingPlayer().calcStat(Stats.PVM_DAMAGE_VUL, 1, attacker, null);
			}
		}
		if (target instanceof L2PcInstance)
		{
			if ((attacker instanceof L2Playable))
			{
				int LastSkillCasted = attacker.getLastSkillCast().getId();
				if (Config.BALANCE_SKILL_LIST.containsKey(LastSkillCasted))
				{
					damage *= Config.BALANCE_SKILL_LIST.get(LastSkillCasted);
				}
			}
			if (((L2PcInstance) target).isWearingHeavyArmor())
			{
				damage *= attacker.calcStat(Stats.HEAVY_DAM_MUL, 1, null, skill);
			}
			else if (((L2PcInstance) target).isWearingLightArmor())
			{
				damage *= attacker.calcStat(Stats.LIGHT_DAM_MUL, 1, null, skill);
			}
			else
				damage *= attacker.calcStat(Stats.ROBE_DAM_MUL, 1, null, skill);
			/* } */
		}
		float rangedAtkBoost = (float) attacker.calcStat(Stats.RANGE_DMG_DIST_BOOST_SKILL, 0, target, skill); // given in PERCENT;
		if (rangedAtkBoost > 0)
		{
			int distToTarg = (int) Util.calculateDistance(attacker, target, false);
			if (distToTarg > 40)
			{
				distToTarg -= 40;
				distToTarg = (int) Math.pow(distToTarg, 1.15);
				damage *= distToTarg * rangedAtkBoost + 1;
			}
		}
		damage *= attacker.calcStat(Stats.SKILL_DAM_MULTI, 1, target, skill);
		// random magic damage
		float rnd = Rnd.get(-20, 20) / 100 + 1;
		damage *= rnd;
		// CT2.3 general magic vuln
		damage *= target.calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, skill);
		if (attacker.getActiveWeaponItem() != null)
			damage *= calcWeaponResistanceModifier(attacker.getActiveWeaponItem(), target, skill);
		damage *= calcElemental(attacker, target, skill);
		double hpdam = attacker.calcStat(Stats.INC_DAM_HP, 0, target, skill);
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentHp() / target.getMaxHp()) * hpdam;
		hpdam = attacker.calcStat(Stats.INC_DAM_MP, 0, target, skill);
		if (hpdam != 0)
			damage += damage * (1 - target.getCurrentMp() / target.getMaxMp()) * hpdam;
		if (target instanceof L2PcInstance)
		{
			hpdam = attacker.calcStat(Stats.INC_DAM_CP, 0, target, skill);
			if (hpdam != 0)
				damage += damage * (1 - target.getCurrentCp() / target.getMaxCp()) * hpdam;
		}
		if (attacker.isBehind(target))
			damage = target.calcStat(Stats.MAGIC_DEFENCE_BEHIND, damage, attacker, skill);
		if (attacker instanceof L2Attackable && target instanceof L2PcInstance && target.getActingPlayer().isSitting())
			damage *= 3;
		if (attacker instanceof L2PcInstance && target instanceof L2Playable)
			damage = calcConfigsPvPDamage(attacker.getActingPlayer(), damage);
		else if (attacker instanceof L2PcInstance && target instanceof L2MonsterInstance)
			damage = calcConfigsPvEDamage(attacker.getActingPlayer(), damage);
		damage += attacker.calcStat(Stats.DMG_ADD, 0, target, skill);
		damage -= target.calcStat(Stats.DMG_REMOVE, 0, attacker, skill);
		if (shld > 0)
			damage -= target.calcStat(Stats.DMG_REMOVE_SHIELD, 0, attacker, skill);
		if (damage < 0)
			return 0;
		if (target.calcStat(Stats.MDAM_MAX, 0, target, skill) > 0)
		{
			damage = Math.min(damage, target.calcStat(Stats.MDAM_MAX, 0, attacker, skill));
		}
		return damage;
	}
	
	public static final double calcMagicDam(L2CubicInstance attacker, L2Character target, L2Skill skill, boolean mcrit, byte shld)
	{
		double mAtk = attacker.getMAtk();
		if (skill.isStaticPower())
			mAtk = 10000;
		double mDef = target.getMDef(attacker.getOwner(), skill);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef(); // kamael
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}
		L2PcInstance owner = attacker.getOwner();
		double damage = 91 * Math.sqrt(mAtk) / mDef * skill.getPower(owner);
		// Failure calculation
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess())
		{
			if (skill.getSkillType() == L2SkillType.DRAIN)
				owner.sendPacket(new SystemMessage(SystemMessageId.DRAIN_HALF_SUCCESFUL));
			else
				owner.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
			damage /= 2;
		}
		else if (mcrit)
			damage *= 2.5;
		if (target instanceof L2Playable)
			damage *= skill.getPvpMulti();
		else if (target instanceof L2Attackable)
			damage *= skill.getPvmMulti();
		damage *= attacker.getOwner().calcStat(Stats.SKILL_DAM_MULTI, 1, target, skill);
		// CT2.3 general magic vuln
		damage *= target.calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, skill);
		damage *= calcElemental(owner, target, skill);
		if (owner.isBehind(target))
			damage = target.calcStat(Stats.MAGIC_DEFENCE_BEHIND, damage, attacker.getOwner(), skill);
		if (target.calcStat(Stats.MDAM_MAX, 0, target, skill) > 0)
		{
			damage = Math.min(damage, owner.calcStat(Stats.MDAM_MAX, 0, null, skill));
		}
		return damage;
	}
	
	/** Returns true in case of critical hit */
	public static final boolean calcCrit(final L2Character attacker, double rate, L2Character target, L2Skill skill)
	{
		if (rate < 1)
			return false;
		int chance = Rnd.get(1000);
		if (attacker.isBehind(target, 60))
			chance -= 60;
		else if (!attacker.isInFrontOf(target))
			chance -= 35;
		final boolean success = rate > chance;
		// support for critical damage evasion
		if (success)
		{
			if (target == null)
				return false; // no effect
			if (skill == null)
			{
				final int critDmgEvas = (int) target.calcStat(Stats.CRIT_DAMAGE_EVASION, 0, attacker, null);
				if (critDmgEvas == 0)
					return true;
				return critDmgEvas <= Rnd.get(100);
			}
			else
			{
				final int critDmgEvas = (int) target.calcStat(Stats.SKILL_CRIT_DAMAGE_EVASION, 0, attacker, skill);
				if (critDmgEvas == 0)
					return true;
				return critDmgEvas <= Rnd.get(100);
			}
		}
		return false;
	}
	
	public static final boolean calcCrit(final L2Character attacker, double rate, L2Character target)
	{
		return calcCrit(attacker, rate, target, null);
	}
	
	/** Calculate value of blow success */
	public static final int calcBlowChance(L2Character activeChar, L2Character target, int chance)
	{
		if (target instanceof L2PcInstance && target.getActingPlayer().isInOlympiadMode())
		{
			if (target.getActingPlayer().isWearingHeavyArmor())
			{
				if (activeChar.isBehind(target))
					return 100;
			}
		}
		else if (target instanceof L2Attackable)
			return (int) activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 19) / 100), target, null);
		return (int) activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 21) / 100), target, null);
	}
	
	/** Calculate value of blow success */
	public static final boolean calcBlow(L2Character activeChar, L2Character target, int chance)
	{
		if (target instanceof L2PcInstance && target.getActingPlayer().isInOlympiadMode())
		{
			if (target.getActingPlayer().isWearingHeavyArmor())
			{
				if (activeChar.isBehind(target))
					return true;
				else if (Rnd.get(100) < 50)
					return true;
			}
		}
		else if (target instanceof L2Attackable)
			return activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 19) / 100), target, null) > Rnd.get(100);
		return activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 21) / 100), target, null) > Rnd.get(100);
	}
	
	/** Calculate value of lethal chance */
	public static final double calcLethal(L2Character activeChar, L2Character target, int baseLethal, int magiclvl)
	{
		double chance = 0;
		int level = 85;
		if (magiclvl > 0)
		{
			int delta = ((magiclvl + level) / 2) - 1 - level;
			// delta [-3,infinite)
			if (delta >= -3)
			{
				chance = (baseLethal * ((double) level / level));
			}
			// delta [-9, -3[
			else if (delta < -3 && delta >= -9)
			{
				// baseLethal
				// chance = -1 * -----------
				// (delta / 3)
				chance = (-3) * (baseLethal / (delta));
			}
			// delta [-infinite,-9[
			else
			{
				chance = baseLethal / 15;
			}
		}
		else
		{
			chance = (baseLethal * ((double) level / level));
		}
		return 10 * activeChar.calcStat(Stats.LETHAL_RATE, chance, target, null);
	}
	
	public static final boolean calcLethalHit(L2Character activeChar, L2Character target, L2Skill skill)
	{
		if (target.calcStat(Stats.LETHAL_IMMUNITY, 0, null, null) > 0)
			return false;
		if (target instanceof L2Playable)
		{
			if (target.getActingPlayer().isInvul())
				return false;
			if (activeChar instanceof L2Playable)
			{
				if (!activeChar.getActingPlayer().isGM())
				{
					if (!target.getActingPlayer().isDebuffable(activeChar.getActingPlayer()))
						return false;
				}
			}
			final int chance = Rnd.get(1000);
			// 2nd lethal effect activate (cp,hp to 1 or if target is npc then hp to 1)
			if (skill.getLethalChance2() > 0 && chance < calcLethal(activeChar, target, skill.getLethalChance2(), skill.getMagicLevel()))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
				if (target instanceof L2PcInstance) // If is a active player set his HP and CP to 1
				{
					target.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE));
					if (!target.getActingPlayer().isCursedWeaponEquipped())
						target.setCurrentHp(1);
					else
						target.setCurrentHp(target.getCurrentHp() / 2);
				}
				else
					target.reduceCurrentHp(target.getCurrentHp() - 1, activeChar, skill);
			}
			else if (skill.getLethalChance1() > 0 && chance < calcLethal(activeChar, target, skill.getLethalChance1(), skill.getMagicLevel()))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.HALF_KILL));
				if (target instanceof L2PcInstance)
				{
					target.sendPacket(new SystemMessage(SystemMessageId.CP_DISAPPEARS_WHEN_HIT_WITH_A_HALF_KILL_SKILL));
					if (!target.getActingPlayer().isCursedWeaponEquipped())
						target.setCurrentCp(1); // Set CP to 1
					else
						target.setCurrentCp(target.getCurrentCp() / 2);
				}
				else // If is a monster remove first damage and after 50% of current hp
					target.reduceCurrentHp(target.getCurrentHp() / 2, activeChar, skill);
			}
			else
				return false;
		}
		return false;
	}
	
	public static final boolean calcMCrit(double mRate, L2Character target)
	{
		final int evade = (int) target.calcStat(Stats.M_CRIT_DAMAGE_EVASION, 0, null, null);
		boolean evaded = evade >= 100;
		if (!evaded && evade > 0)
		{
			evaded = evade > Rnd.get(100);
		}
		return !evaded && mRate > Rnd.get(1000);
	}
	
	public static final boolean calcMCritHeal(double mRate)
	{
		return mRate > Rnd.get(1500);
	}
	
	/** Calculate delay (in milliseconds) before next ATTACK */
	public static final int calcPAtkSpd(L2Character attacker, L2Character target, double rate)
	{
		// measured Oct 2006 by Tank6585, formula by Sami
		// attack speed 312 equals 1500 ms delay... (or 300 + 40 ms delay?)
		if (rate < 2)
			return 2700;
		else
			return (int) (561000 / rate);
	}
	
	/** Calculate delay (in milliseconds) for skills cast */
	public static final int calcSkillCastTime(L2Character attacker, L2Skill skill, double skillTime)
	{
		if (skill.isMagic())
		{
			if (attacker instanceof L2PcInstance)
			{
				if (!attacker.getActingPlayer().isMageClass())
				{
					if (attacker.getActingPlayer().isInOlympiadMode())
						return (int) (skillTime * 320 / attacker.getMAtkSpd(skill));
					else
						return (int) (skillTime * 170 / attacker.getMAtkSpd(skill));
				}
			}
			else
			{
				return (int) (skillTime * 160 / attacker.getMAtkSpd(skill));
			}
			return (int) (skillTime * 290 / attacker.getMAtkSpd(skill));
		}
		if (attacker instanceof L2Attackable)
			return (int) (skillTime * 350 / attacker.getPAtkSpd(skill));
		return (int) (skillTime * 440 / attacker.getPAtkSpd(skill));
	}
	
	public static boolean calcHitMiss(L2Character attacker, L2Character target)
	{
		return calcHitMiss(attacker, target, 0);
	}
	
	public static boolean calcHitMiss(L2Character attacker, L2Character target, int accuracyPenalty)
	{
		final int absolute_evasion = (int) target.calcStat(Stats.EVASION_ABSOLUTE, 0, attacker, null);
		if (absolute_evasion > 0)
		{
			if (Rnd.get(100) < absolute_evasion)
				return true;
		}
		final int delta = attacker.getAccuracy(target) - target.getEvasionRate(attacker) - accuracyPenalty;
		int chance;
		if (delta >= 10)
			chance = 980;
		else
		{
			switch (delta)
			{
				case 9:
					chance = 975;
					break;
				case 8:
					chance = 970;
					break;
				case 7:
					chance = 965;
					break;
				case 6:
					chance = 960;
					break;
				case 5:
					chance = 955;
					break;
				case 4:
					chance = 945;
					break;
				case 3:
					chance = 935;
					break;
				case 2:
					chance = 925;
					break;
				case 1:
					chance = 915;
					break;
				case 0:
					chance = 905;
					break;
				case -1:
					chance = 890;
					break;
				case -2:
					chance = 875;
					break;
				case -3:
					chance = 860;
					break;
				case -4:
					chance = 845;
					break;
				case -5:
					chance = 830;
					break;
				case -6:
					chance = 815;
					break;
				case -7:
					chance = 800;
					break;
				case -8:
					chance = 785;
					break;
				case -9:
					chance = 770;
					break;
				case -10:
					chance = 755;
					break;
				case -11:
					chance = 735;
					break;
				case -12:
					chance = 715;
					break;
				case -13:
					chance = 695;
					break;
				case -14:
					chance = 675;
					break;
				case -15:
					chance = 655;
					break;
				case -16:
					chance = 625;
					break;
				case -17:
					chance = 595;
					break;
				case -18:
					chance = 565;
					break;
				case -19:
					chance = 535;
					break;
				case -20:
					chance = 505;
					break;
				case -21:
					chance = 455;
					break;
				case -22:
					chance = 405;
					break;
				case -23:
					chance = 355;
					break;
				case -24:
					chance = 305;
					break;
				default:
				{
					if (target.calcStat(Stats.IMPROVED_EVASION, 0, attacker, null) > 0)
					{
						switch (delta)
						{
							case -25:
								chance = 255;
								break;
							case -26:
								chance = 205;
								break;
							case -27:
								chance = 155;
								break;
							case -28:
								chance = 105;
								break;
							case -29:
								chance = 55;
								break;
							default:
								return true;
						}
					}
					else
						chance = 255;
				}
			}
		}
		if (!attacker.isInFrontOf(target))
		{
			if (attacker.isBehind(target))
				chance *= 1.2;
			else
				// side
				chance *= 1.1;
			if (chance > 980)
				chance = 980;
		}
		return chance < Rnd.get(1000);
	}
	
	/** Returns true in case when ATTACK is canceled due to hit */
	public static final boolean calcAtkBreak(L2Character attacker, L2Character target, int damage)
	{
		if (attacker == null || target == null || attacker == target)
			return false;
		if (target.isRaid())
			return false; // No attack break
		final int dmgThreashold = 1700;
		if (attacker instanceof L2Attackable || damage >= dmgThreashold)
		{
			double init = 0;
			if (Config.ALT_GAME_CANCEL_CAST && target.isCastingNow() && target.canAbortCast())
			{
				final L2Skill skill = target.getLastSkillCast();
				if (!(skill != null && skill.isMagic()))
					return false;
				if (skill.getSkillType() == L2SkillType.FUSION)
					return true;
				init = Config.SPELL_CANCEL_CHANCE;
				if (attacker instanceof L2Attackable)
				{
					if (skill.isHeal())
					{
						if (target.getActingPlayer().isWearingMagicArmor())
							init += 60;
						else
							init += 30;
					}
					else if (damage < dmgThreashold)
						return false;
				}
				init += attacker.calcStat(Stats.SPELL_CANCEL_ADD, 0, target, null);
			}
			else if (Config.ALT_GAME_CANCEL_BOW && damage >= dmgThreashold && target.isAttackingNow() && target.getActiveWeaponItem() != null && (target.getActiveWeaponItem().getItemType() == L2WeaponType.BOW || target.getActiveWeaponItem().getItemType() == L2WeaponType.CROSSBOW))
			{
				init = Config.ATTACK_CANCEL_CHANCE;
				init += attacker.calcStat(Stats.ATTACK_CANCEL_ADD, 0, target, null);
			}
			else
				return false;
			init -= target.calcStat(Stats.SPELL_CANCEL_RES, 0, attacker, null);
			if (init < 1)
				return false;
			if (init > 99)
				return true;
			return Rnd.get(100) < init;
		}
		return false;
	}
	
	/**
	 * Returns:<br>
	 * 0 = shield defense doesn't succeed<br>
	 * 1 = shield defense succeed<br>
	 * 2 = perfect block<br>
	 * 
	 * @param attacker
	 * @param target
	 * @param sendSysMsg
	 * @return
	 */
	public static byte calcShldUse(L2Character attacker, L2Character target, L2Skill skill, boolean sendSysMsg)
	{
		if (skill != null && skill.ignoreShield())
			return 0;
		if (target.isStunned() || target.isSleeping() || target.isParalyzed() || target.isAfraid() || target.isBluffed() || target.isConfused())
			return 0;
		if (attacker.calcStat(Stats.IGNORE_SHIELD, 0, null, null) > Rnd.get(100))
			return 0;
		final byte overpower = calcOverpower(attacker, target, skill);
		if (overpower > 0)
		{
			if (overpower == 1)
			{
				if (target instanceof L2PcInstance)
					target.sendPacket(new SystemMessage(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL));
				return SHIELD_DEFENSE_SUCCEED;
			}
			else
			{
				if (target instanceof L2PcInstance)
					target.sendPacket(new SystemMessage(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS));
				return SHIELD_DEFENSE_PERFECT_BLOCK;
			}
		}
		double shldRate = target.getShldRate(attacker, skill);
		if (shldRate < 1)
			return 0;
		final int degreeside = (int) target.calcStat(Stats.SHIELD_DEFENCE_ANGLE, 120, null, null);
		if (degreeside < 360 && (!target.isFacing(attacker, degreeside)))
			return 0;
		if (target.isAttackingNow() || target.isCastingNow())
			shldRate /= 1.28;
		shldRate = Math.min(shldRate, target.calcStat(Stats.BLOCK_RATE_MAX, 80, attacker, skill));
		if (attacker instanceof L2RaidBossInstance || attacker instanceof L2MinionInstance)
			shldRate /= 1.7;
		if (target.isMoving() && target.isRunning())
			shldRate /= 1.5;
		byte shldSuccess = SHIELD_DEFENSE_FAILED;
		L2Weapon at_weapon = attacker.getActiveWeaponItem();
		if (at_weapon != null)
		{
			if (at_weapon.getItemType() == L2WeaponType.BOW)
				shldRate *= 1.33;
			else if (at_weapon.getItemType() == L2WeaponType.CROSSBOW)
				shldRate *= 1.15;
		}
		if (shldRate > 0 && 100 - Config.ALT_PERFECT_SHLD_BLOCK - target.calcStat(Stats.PERF_BLOCK_ADD, 0, null, null) < Rnd.get(100))
		{
			shldSuccess = SHIELD_DEFENSE_PERFECT_BLOCK;
		}
		else if (shldRate > Rnd.get(100))
		{
			shldSuccess = SHIELD_DEFENSE_SUCCEED;
		}
		if (sendSysMsg && target instanceof L2PcInstance)
		{
			L2PcInstance enemy = (L2PcInstance) target;
			switch (shldSuccess)
			{
				case SHIELD_DEFENSE_SUCCEED:
					enemy.sendPacket(new SystemMessage(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL));
					break;
				case SHIELD_DEFENSE_PERFECT_BLOCK:
					enemy.sendPacket(new SystemMessage(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS));
					break;
			}
		}
		return shldSuccess;
	}
	
	private static byte calcOverpower(L2Character attacker, L2Character target, L2Skill skill)
	{
		if (attacker instanceof L2Playable && !attacker.getActingPlayer().isInOlympiadMode())
		{
			if (attacker instanceof L2PcInstance)
			{
				final L2Item wep = attacker.getActiveWeaponItem();
				if (wep != null)
				{
					switch (wep.getItemId())
					{
						case Config.AURAFANG:
						case Config.RAYBLADE:
						case Config.WAVEBRAND:
							if (attacker.getActingPlayer()._hasTehForce)
								return 0;
					}
				}
			}
			int overpower = (int) target.calcStat(Stats.OVERPOWER, 0, attacker, skill);
			if (skill != null && !skill.isMagic())
				overpower /= 2;
			if (overpower > 0)
			{
				final int attackerOverpower = (int) attacker.calcStat(Stats.OVERPOWER, 0, target, skill);
				overpower -= attackerOverpower;
				if (overpower > 0)
				{
					try
					{
						if ((target.isAttackingNowOverpower() && target.getAI().getAttackTarget() == attacker) || (target.isCastingNow() && target.getAI().getSkill().isDamaging() && target.getAI().getCastTarget() == attacker))
						{
							if (Rnd.get(100) < overpower)
							{
								if (skill != null && skill.isMagic())
									return 1;
								return 2;
							}
						}
					}
					catch (Exception e)
					{}
				}
			}
		}
		return 0;
	}
	
	public static byte calcShldUse(L2Character attacker, L2Character target, L2Skill skill)
	{
		return calcShldUse(attacker, target, skill, true);
	}
	
	public static byte calcShldUse(L2Character attacker, L2Character target)
	{
		return calcShldUse(attacker, target, null, true);
	}
	
	public static boolean calcMagicAffected(L2Character actor, L2Character target, L2Skill skill)
	{
		L2SkillType type = skill.getSkillType();
		double defence = 0;
		if (skill.isActive() && skill.isOffensive() && !skill.isNeutral())
			defence = target.getMDef(actor, skill);
		double attack = 2 * actor.getMAtk(target, skill) * calcSkillVulnerability(actor, target, skill);
		double d = (attack - defence) / (attack + defence);
		if (target.isRaid())
		{
			switch (type)
			{
				case CONFUSION:
				case MUTE:
				case PARALYZE:
				case ROOT:
				case FEAR:
				case SLEEP:
				case STUN:
				case DEBUFF:
				case AGGDEBUFF:
					if (d > 0 && Rnd.get(1000) == 1)
						return true;
					else
						return false;
				default:
					break;
			}
		}
		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}
	
	@SuppressWarnings("incomplete-switch")
	public static double calcSkillVulnerability(L2Character attacker, L2Character target, L2Skill skill)
	{
		double multiplier = 1;
		// Get the skill type to calculate its effect in function of base stats of the L2Character target
		if (skill != null)
		{
			// first, get the natural template vulnerability values for the target
			Stats stat = skill.getStat();
			if (stat != null)
			{
				switch (stat)
				{
					case AGGRESSION:
						multiplier *= target.getTemplate().baseAggressionVuln;
						break;
					case BLEED:
						multiplier *= target.getTemplate().baseBleedVuln;
						break;
					case POISON:
						multiplier *= target.getTemplate().basePoisonVuln;
						break;
					case STUN:
						multiplier *= target.getTemplate().baseStunVuln;
						break;
					case ROOT:
						multiplier *= target.getTemplate().baseRootVuln;
						break;
					case MOVEMENT:
						multiplier *= target.getTemplate().baseMovementVuln;
						break;
					case CONFUSION:
						multiplier *= target.getTemplate().baseConfusionVuln;
						break;
					case SLEEP:
						multiplier *= target.getTemplate().baseSleepVuln;
						break;
				}
			}
			final L2SkillType type = skill.getSkillType();
			if (type != null)
			{
				switch (type)
				{
					case BLEED:
						multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, null, skill);
						break;
					case POISON:
						multiplier = target.calcStat(Stats.POISON_VULN, multiplier, null, skill);
						break;
					case STUN:
						multiplier = target.calcStat(Stats.STUN_VULN, multiplier, null, skill);
						break;
					case PARALYZE:
						multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, null, skill);
						break;
					case ROOT:
						multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, null, skill);
						break;
					case SLEEP:
						multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, null, skill);
						break;
					case MUTE:
					case FEAR:
					case BETRAY:
					case AGGREDUCE:
					case AGGREDUCE_CHAR:
						multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, null, skill);
						break;
					case CONFUSION:
					case CONFUSE_MOB_ONLY:
						multiplier = target.calcStat(Stats.CONFUSION_VULN, multiplier, null, skill);
						break;
					case DEBUFF:
					case WEAKNESS:
						multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, null, skill);
						break;
					case BUFF:
						multiplier = target.calcStat(Stats.BUFF_VULN, multiplier, null, skill);
						break;
					case DISARM:
						multiplier = target.calcStat(Stats.DISARM_VULN, multiplier, null, skill);
						break;
					default:
				}
			}
			if (target instanceof L2Attackable)
				multiplier *= 0.5;
			if (skill.getElement() > 0)
				multiplier *= Math.sqrt(calcElemental(attacker, target, skill));
			/*
			 * // Finally, calculate skilltype vulnerabilities
			 * L2SkillType type = skill.getSkillType();
			 * // For additional effects on PDAM and MDAM skills (like STUN, SHOCK, PARALYZE...)
			 * if (type != null && (type == L2SkillType.PDAM || type == L2SkillType.MDAM || type == L2SkillType.DRAIN || type == L2SkillType.BLOW
			 * || type == L2SkillType.DEATHLINK || type == L2SkillType.CHARGEDAM || type == L2SkillType.CPDAM || type == L2SkillType.FATAL))
			 * type = skill.getEffectType();
			 * multiplier = calcSkillTypeVulnerability(multiplier, target, type);
			 */
		}
		return multiplier;
	}
	
	@SuppressWarnings("incomplete-switch")
	public static double calcSkillTypeVulnerability(double multiplier, L2Character target, L2SkillType type)
	{
		if (type != null)
		{
			switch (type)
			{
				case BLEED:
					multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, null, null);
					break;
				case POISON:
					multiplier = target.calcStat(Stats.POISON_VULN, multiplier, null, null);
					break;
				case STUN:
					multiplier = target.calcStat(Stats.STUN_VULN, multiplier, null, null);
					break;
				case PARALYZE:
					multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, null, null);
					break;
				case ROOT:
					multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, null, null);
					break;
				case SLEEP:
					multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, null, null);
					break;
				case MUTE:
				case FEAR:
				case BETRAY:
				case AGGREDUCE_CHAR:
					multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, null, null);
					break;
				case CONFUSION:
				case CONFUSE_MOB_ONLY:
					multiplier = target.calcStat(Stats.CONFUSION_VULN, multiplier, null, null);
					break;
				case DEBUFF:
				case WEAKNESS:
					multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, null, null);
					break;
				case BUFF:
					multiplier = target.calcStat(Stats.BUFF_VULN, multiplier, null, null);
					break;
				case DISARM:
					multiplier = target.calcStat(Stats.DISARM_VULN, multiplier, null, null);
					break;
			}
		}
		return multiplier;
	}
	
	public static double calcSkillProficiency(L2Skill skill, L2Character attacker, L2Character target)
	{
		double multiplier = 1; // initialize...
		if (skill != null)
		{
			// Calculate skilltype vulnerabilities
			L2SkillType type = skill.getSkillType();
			// For additional effects on PDAM and MDAM skills (like STUN, SHOCK, PARALYZE...)
			if (type != null && (type == L2SkillType.PDAM || type == L2SkillType.PDAMPERC || type == L2SkillType.MDAM || type == L2SkillType.DRAIN || type == L2SkillType.BLOW || type == L2SkillType.DEATHLINK || type == L2SkillType.CHARGEDAM || type == L2SkillType.CPDAM || type == L2SkillType.FATAL))
				type = skill.getEffectType();
			multiplier = calcSkillTypeProficiency(multiplier, attacker, target, type);
		}
		return multiplier;
	}
	
	public static double calcSkillTypeProficiency(double multiplier, L2Character attacker, L2Character target, L2SkillType type)
	{
		if (type != null)
		{
			switch (type)
			{
				case BLEED:
					multiplier = attacker.calcStat(Stats.BLEED_PROF, multiplier, target, null);
					break;
				case POISON:
					multiplier = attacker.calcStat(Stats.POISON_PROF, multiplier, target, null);
					break;
				case STUN:
					multiplier = attacker.calcStat(Stats.STUN_PROF, multiplier, target, null);
					break;
				case PARALYZE:
					multiplier = attacker.calcStat(Stats.PARALYZE_PROF, multiplier, target, null);
					break;
				case ROOT:
					multiplier = attacker.calcStat(Stats.ROOT_PROF, multiplier, target, null);
					break;
				case SLEEP:
					multiplier = attacker.calcStat(Stats.SLEEP_PROF, multiplier, target, null);
					break;
				case MUTE:
				case FEAR:
				case BETRAY:
				case AGGREDUCE_CHAR:
					multiplier = attacker.calcStat(Stats.DERANGEMENT_PROF, multiplier, target, null);
					break;
				case CONFUSION:
				case CONFUSE_MOB_ONLY:
					multiplier = attacker.calcStat(Stats.CONFUSION_PROF, multiplier, target, null);
					break;
				case DEBUFF:
				case WEAKNESS:
					multiplier = attacker.calcStat(Stats.DEBUFF_PROF, multiplier, target, null);
					break;
				default:
			}
		}
		return multiplier;
	}
	
	public static double calcSkillStatModifier(L2SkillType type, L2Character target)
	{
		double multiplier = 1;
		if (type == null)
			return multiplier;
		try
		{
			switch (type)
			{
				case STUN:
				case BLEED:
				case POISON:
					multiplier = 2 - Math.sqrt(CONbonus[target.getCON()]);
					break;
				case SLEEP:
				case DEBUFF:
				case WEAKNESS:
				case ERASE:
				case ROOT:
				case MUTE:
				case FEAR:
				case BETRAY:
				case CONFUSION:
				case CONFUSE_MOB_ONLY:
				case AGGREDUCE_CHAR:
				case PARALYZE:
					multiplier = 2 - Math.sqrt(MENbonus[target.getMEN()]);
					break;
				default:
					return multiplier;
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			_log.warning("Character " + target.getName() + " has been set (by a GM?) a MEN or CON stat value out of accepted range");
		}
		if (multiplier < 0)
			multiplier = 0;
		return multiplier;
	}
	
	public static boolean calcEffectSuccess(L2Character attacker, L2Character target, EffectTemplate effect, L2Skill skill, byte shld)
	{
		final int rate = calcEffectSuccessChance(attacker, target, effect, skill, shld);
		if (rate <= 0)
			return false;
		return (Rnd.get(100) < rate);
	}
	
	public static int calcEffectSuccessChance(L2Character attacker, L2Character target, EffectTemplate effect, L2Skill skill, byte shld)
	{
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK || target.isPreventedFromReceivingBuffs()) // perfect block
			return 0;
		if (skill.isOffensive() && target.isPreventedFromReceivingDebuffs())
			return 0;
		/*
		 * if (attacker instanceof L2Playable && target instanceof L2Playable)
		 * {
		 * if (!attacker.getActingPlayer().isGM())
		 * {
		 * if (!target.getActingPlayer().isDebuffable(attacker.getActingPlayer()))
		 * return 0;
		 * }
		 * }
		 */
		final L2SkillType type = effect.effectType != null ? effect.effectType : skill.getSkillType();
		double resmodifier = calcSkillTypeVulnerability(1, target, type);
		if (resmodifier <= 0)
			return 0;
		if (attacker instanceof L2MonsterInstance)
		{
			if (attacker instanceof L2RaidBossInstance || attacker.getLevel() >= 90)
				return 100;
			else if (attacker.getLevel() < 84)
				return 0;
		}
		else
		{
			if (skill.getMagicLevel() > 0 && skill.getMagicLevel() < 74)
				resmodifier *= 0.5;
		}
		int value = (int) effect.effectPower;
		value = (int) attacker.calcStat(Stats.EFFECT_POWER_BOOST, value, null, skill);
		double statmodifier = calcSkillStatModifier(type, target);
		int ssmodifier = 160;
		// Calculate BaseRate.
		int rate = (int) ((value * statmodifier));// + lvlmodifier));
		// Add Matk/Mdef Bonus
		if (skill.isMagic())
		{
			if (attacker instanceof L2PcInstance && !attacker.getActingPlayer().isMageClass())
				rate = (int) (rate * Math.pow((double) attacker.getMAtk(target, skill) + 9000 / (target.getMDef(attacker, skill) + (shld == 1 ? target.getShldDef() : 0)), 0.2));
			else
				rate = (int) (rate * Math.pow((double) attacker.getMAtk(target, skill) + 4000 / (target.getMDef(attacker, skill) + (shld == 1 ? target.getShldDef() : 0)), 0.2));
		}
		if (rate > 10000 / (100 + ssmodifier))
			rate = 100 - (100 - rate) * 100 / ssmodifier;
		else
			rate = rate * ssmodifier / 100;
		if (rate > 90)
			rate = 90;
		else if (rate < 10)
			rate = 10;
		// Finaly apply resists.
		rate *= resmodifier * calcSkillTypeProficiency(1, attacker, target, type);
		final int maxLandChance = skill.getMaxLandChance();
		final int minLandChance = skill.getMinLandChance();
		boolean isRB = false;
		if (target instanceof L2Attackable)
		{
			if (target instanceof L2RaidBossInstance || target.getLevel() >= 91)
				isRB = true;
		}
		boolean lionheart = false;
		if (target != null && (target.getFirstEffectById(287) != null || target.getFirstEffectById(499) != null || (target instanceof L2PcInstance && target.getActingPlayer().getCurrentSkill() != null && target.getActingPlayer().getCurrentSkill().getSkillId() == 246)))
			lionheart = true;
		final int maxChance = maxLandChance == 0 ? (int) attacker.calcStat(Stats.MAX_LAND_RATE, 69, target, skill) : (int) attacker.calcStat(Stats.MAX_LAND_RATE, maxLandChance, target, skill);
		final int minChance = minLandChance == 0 ? (int) target.calcStat(Stats.MIN_LAND_RATE, 21, attacker, skill) : (int) target.calcStat(Stats.MIN_LAND_RATE, minLandChance, attacker, skill);
		if (rate > maxChance)
			rate = maxChance;
		else if (rate < minChance && !isRB && !lionheart)
			rate = minChance;
		if (isRB)
			rate /= 1.5;
		if (skill.getOlyNerf() > 0 && target instanceof L2PcInstance && target.getActingPlayer().isInOlympiadMode())
		{
			if (skill.isDamaging())
				rate -= skill.getOlyNerf() / 100;
			else
				rate -= skill.getOlyNerf();
			if (rate < 30)
				rate = 30;
		}
		return rate;
	}
	
	public static boolean calcSkillSuccess(L2Character attacker, L2Character target, L2Skill skill, byte shld)
	{
		final int rate = calcSkillSuccessChance(attacker, target, skill, shld);
		if (rate <= 0)
			return false;
		return (Rnd.get(100) < rate);
	}
	
	public static int calcSkillSuccessChance(L2Character attacker, L2Character target, L2Skill skill, byte shld)
	{
		if (skill.ignoreResists())
			return (int) (skill.getPower(attacker));
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK || target.isPreventedFromReceivingBuffs()) // perfect block
			return 0;
		if (skill.isOffensive() && target.isPreventedFromReceivingDebuffs())
			return 0;
		double resmodifier = calcSkillVulnerability(attacker, target, skill);
		if (resmodifier <= 0)
			return 0;
		if (attacker instanceof L2MonsterInstance)
		{
			if (attacker instanceof L2RaidBossInstance || attacker.getLevel() >= 95)
				return 100;
			else if (attacker.getLevel() < 84)
				return 0;
		}
		else
		{
			if (skill.getMagicLevel() > 0 && skill.getMagicLevel() < 74)
				resmodifier *= 0.5;
		}
		final L2SkillType type = skill.getSkillType();
		int value = (int) skill.getPower(attacker);
		if (value == 0)
			value = (type == L2SkillType.PARALYZE) ? 40 : (type == L2SkillType.FEAR) ? 50 : 92;
		double statmodifier = calcSkillStatModifier(type, target);
		int ssmodifier = 163;
		// Calculate BaseRate.
		int rate = (int) ((value * statmodifier));// + lvlmodifier));
		// Add Matk/Mdef Bonus
		if (skill.isMagic())
		{
			if (attacker instanceof L2PcInstance && !attacker.getActingPlayer().isMageClass())
				rate = (int) (rate * Math.pow((double) attacker.getMAtk(target, skill) + 9000 / (target.getMDef(attacker, skill) + (shld == 1 ? target.getShldDef() : 0)), 0.2));
			else
				rate = (int) (rate * Math.pow((double) attacker.getMAtk(target, skill) + 5000 / (target.getMDef(attacker, skill) + (shld == 1 ? target.getShldDef() : 0)), 0.2));
		}
		// Add Bonus for Sps/SS
		if (ssmodifier != 100)
		{
			if (rate > 10000 / (100 + ssmodifier))
				rate = 100 - (100 - rate) * 100 / ssmodifier;
			else
				rate = rate * ssmodifier / 100;
		}
		if (rate > 95)
			rate = 95;
		else if (rate < 10)
			rate = 10;
		// Finaly apply resists
		int maxLandChance = skill.getMaxLandChance();
		int minLandChance = skill.getMinLandChance();
		boolean isRB = false;
		boolean isLonis = false;
		if (target instanceof L2Attackable)
		{
			if (target instanceof L2RaidBossInstance || target.getLevel() >= 91)
				isRB = true;
		}
		if (target instanceof L2Attackable)
		{
			if (target instanceof L2RaidBossInstance)
				isLonis = true;
		}
		boolean lionheart = false;
		if (target.getFirstEffectById(287) != null || target.getFirstEffectById(499) != null || (target instanceof L2PcInstance && target.getActingPlayer().getCurrentSkill() != null && target.getActingPlayer().getCurrentSkill().getSkillId() == 246))
			lionheart = true;
		final int maxChance = maxLandChance == 0 ? (int) attacker.calcStat(Stats.MAX_LAND_RATE, 69, target, skill) : (int) attacker.calcStat(Stats.MAX_LAND_RATE, maxLandChance, target, skill);
		if (minLandChance > maxChance)
		{
			minLandChance = maxChance / 2;
		}
		int minChance = minLandChance == 0 ? (int) target.calcStat(Stats.MIN_LAND_RATE, 21, attacker, skill) : (int) target.calcStat(Stats.MIN_LAND_RATE, minLandChance, attacker, skill);
		if (rate > maxChance)
			rate = maxChance;
		rate *= resmodifier * calcSkillProficiency(skill, attacker, target);
		if (rate > maxChance)
			rate = maxChance;
		if (minChance < 21)
			minChance = 21;
		if (rate < minChance && !isRB && !lionheart)
			rate = minChance;
		if (isRB)
			rate /= 1.5;
		if (skill.getOlyNerf() > 0 && target instanceof L2PcInstance && target.getActingPlayer().isInOlympiadMode())
		{
			if (skill.isDamaging())
				rate -= skill.getOlyNerf() / 100;
			else
				rate -= skill.getOlyNerf();
			if (rate < 10)
				rate = 10;
		}
		if (target instanceof L2MonsterInstance && !isLonis)
		{
			if (rate < 25)
				rate = 25;
		}
		if (rate == 69)
		{
			rate = 55;
		}
		return rate;
	}
	
	@SuppressWarnings("incomplete-switch")
	public static boolean calcCubicSkillSuccess(L2CubicInstance attacker, L2Character target, L2Skill skill, byte shld)
	{
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK || target.isPreventedFromReceivingBuffs()) // perfect block
			return false;
		if (skill.isOffensive() && target.isPreventedFromReceivingDebuffs())
			return false;
		final double resmodifier = calcSkillVulnerability(attacker.getOwner(), target, skill);
		if (resmodifier <= 0)
			return false;
		L2SkillType type = skill.getSkillType();
		// these skills should not work on RaidBoss
		if (target.isRaid())
		{
			switch (type)
			{
				case CONFUSION:
				case ROOT:
				case STUN:
				case MUTE:
				case FEAR:
				case DEBUFF:
				case PARALYZE:
				case SLEEP:
				case AGGDEBUFF:
					return false;
			}
		}
		// if target reflect this skill then the effect will fail
		if (calcSkillReflect(null, target, skill) != SKILL_REFLECT_FAILED)
			return false;
		int value = (int) skill.getPower();
		int lvlDepend = skill.getLevelDepend();
		// TODO: Temporary fix for skills with Power = 0 or LevelDepend not set
		if (value == 0)
			value = (type == L2SkillType.PARALYZE) ? 50 : (type == L2SkillType.FEAR) ? 40 : 80;
		if (lvlDepend == 0)
			lvlDepend = (type == L2SkillType.PARALYZE || type == L2SkillType.FEAR) ? 1 : 2;
		// TODO: Temporary fix for NPC skills with MagicLevel not set
		// int lvlmodifier = (skill.getMagicLevel() - target.getLevel()) * lvlDepend;
		// int lvlmodifier = ((skill.getMagicLevel() > 0 ? skill.getMagicLevel() :
		// attacker.getOwner().getLevel()) - target.getLevel())
		// * lvlDepend;
		double statmodifier = calcSkillStatModifier(type, target);
		int rate = (int) ((value * statmodifier) * resmodifier);
		if (skill.isMagic())
			rate += (int) (Math.pow((double) attacker.getMAtk() / (target.getMDef(attacker.getOwner(), skill) + (shld == 1 ? target.getShldDef() : 0)), 0.2) * 100) - 100;
		if (rate > 99)
			rate = 99;
		else if (rate < 1)
			rate = 1;
		if (Config.DEVELOPER)
			_log.info(skill.getName() + ": " + value + ", " + statmodifier + ", " + resmodifier + ", " + ((int) (Math.pow((double) attacker.getMAtk() / target.getMDef(attacker.getOwner(), skill), 0.2) * 100) - 100) + " ==> " + rate);
		if (skill.getOlyNerf() > 0 && target instanceof L2PcInstance && target.getActingPlayer().isInOlympiadMode())
		{
			if (skill.isDamaging())
				rate -= skill.getOlyNerf() / 100;
			else
				rate -= skill.getOlyNerf();
			if (rate < 10)
				rate = 10;
		}
		return (Rnd.get(100) < rate);
	}
	
	public static boolean calcMagicSuccess()
	{
		return (Rnd.get(100) > 6);
	}
	
	public static boolean calculateUnlockChance(L2Skill skill)
	{
		int level = skill.getLevel();
		int chance = 0;
		switch (level)
		{
			case 1:
				chance = 30;
				break;
			case 2:
				chance = 50;
				break;
			case 3:
				chance = 75;
				break;
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
				chance = 100;
				break;
		}
		if (Rnd.get(120) > chance)
			return false;
		return true;
	}
	
	public static double calcManaDam(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean bss)
	{
		// Mana Burnt = (SQR(M.Atk)*Power*(Target Max MP/97))/M.Def
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		double mp = target.getMaxMp();
		if (bss)
			mAtk *= 4;
		else if (ss)
			mAtk *= 2;
		double damage = (Math.sqrt(mAtk) * skill.getPower(attacker) * (mp / 97)) / mDef;
		damage *= calcSkillVulnerability(attacker, target, skill);
		return damage;
	}
	
	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, int casterWIT)
	{
		if (baseRestorePercent == 0 || baseRestorePercent == 100)
			return baseRestorePercent;
		double restorePercent = baseRestorePercent * WITbonus[casterWIT];
		if (restorePercent - baseRestorePercent > 20.0)
			restorePercent += 20.0;
		restorePercent = Math.max(restorePercent, baseRestorePercent);
		restorePercent = Math.min(restorePercent, 90.0);
		return restorePercent;
	}
	
	public static double getSTRBonus(L2Character activeChar)
	{
		return STRbonus[activeChar.getSTR()];
	}
	
	public static boolean calcPhysicalSkillEvasion(L2Character caster, L2Character target, L2Skill skill)
	{
		if (skill.isMagic() && skill.getSkillType() != L2SkillType.BLOW)
			return false;
		int bonus = 0;
		if (caster.isBehind(target))
			bonus = 6;
		return Rnd.get(100) < target.calcStat(Stats.P_SKILL_EVASION, 0, caster, skill) - caster.calcStat(Stats.P_SKILL_EVASION_REDUCTION, 0, target, skill) - bonus;
	}
	
	public static boolean calcMagicalSkillEvasion(L2Character caster, L2Character target, L2Skill skill)
	{
		if (!skill.isMagic())
			return false;
		int bonus = 0;
		if (caster.isBehind(target))
			bonus = 6;
		return Rnd.get(100) < target.calcStat(Stats.M_SKILL_EVASION, 0, caster, skill) - caster.calcStat(Stats.M_SKILL_EVASION_REDUCTION, 0, target, skill) - bonus;
	}
	
	public static boolean calcSkillMastery(L2Character actor, L2Skill sk)
	{
		if (sk.getSkillType() == L2SkillType.FISHING || sk.isPotion())
			return false;
		if (actor instanceof L2BufferInstance)
			return false;
		if (sk.isToggle() || sk.getSkillType() == L2SkillType.CONT)
			return false;
		double val = actor.getStat().calcStat(Stats.SKILL_MASTERY, 0, null, sk);
		if (actor instanceof L2PcInstance && ((L2PcInstance) actor).isMageClass())
			val *= INTbonus[actor.getINT()];
		else
			val *= STRbonus[actor.getSTR()];
		return Rnd.get(100) < val;
	}
	
	public static double calcValakasAttribute(L2Character attacker, L2Character target, L2Skill skill)
	{
		double calcPower = 0;
		double calcDefen = 0;
		if (skill != null && skill.getAttributeName().contains("valakas"))
		{
			calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
			calcDefen = target.calcStat(Stats.VALAKAS_RES, calcDefen, target, skill);
		}
		else
		{
			calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
			if (calcPower > 0)
			{
				calcPower = attacker.calcStat(Stats.VALAKAS, calcPower, target, skill);
				calcDefen = target.calcStat(Stats.VALAKAS_RES, calcDefen, target, skill);
			}
		}
		return calcPower - calcDefen;
	}
	
	public static double calcElemental(L2Character attacker, L2Character target, L2Skill skill)
	{
		if (attacker instanceof L2Playable)
		{
			// if (/*attacker.getActingPlayer().isInHuntersVillage() ||*/ (DM._started && attacker.getActingPlayer()._inEventDM)*/)
			// return 1;
			if (target instanceof L2Playable)
			{
				if (attacker.getActingPlayer().isInOlympiadMode() || target.getActingPlayer().isInOlympiadMode())
					return 1;
				if (TvT._started && TvT._sGrade)
				{
					if (attacker.getActingPlayer()._inEventTvT || target.getActingPlayer()._inEventTvT)
						return 1.2;
				}
				if (NewTvT._started && NewTvT._sGrade)
				{
					if (attacker.getActingPlayer()._inEventTvT || target.getActingPlayer()._inEventTvT)
						return 1.2;
				}
			}
		}
		int calcPower = 0;
		int calcDefen = 0;
		int calcTotal = 0;
		double result = 1.0;
		byte element;
		if (skill != null)
		{
			element = skill.getElement();
			if (element >= 0)
			{
				calcPower = skill.getElementPower();
				final boolean negated = attacker.calcStat(Stats.IGNORE_ELEMENTALS, 0, null, null) > 0 || target.calcStat(Stats.IGNORE_ELEMENTALS, 0, null, null) > 0;
				/*
				 * if (calcPower > 0)
				 * {
				 * if (negated) //ignore ele
				 * return calcPower * 0.0038 + 1;
				 * }
				 * else
				 */ if (negated)
					return 1;
				calcDefen = target.getDefenseElementValue(element);
				if (target instanceof L2MonsterInstance)
				{
					if (element == Elementals.DARK)
						calcDefen += 30;
					else if (element == Elementals.HOLY)
						calcDefen += 10;
				}
				if (attacker.getAttackElement() == element)
					calcPower += attacker.getAttackElementValue(element);
				calcTotal = calcPower - calcDefen;
				if (calcTotal > 0)
				{
					if (calcTotal < 75)
						result += calcTotal * 0.0035;
					else if (calcTotal < 150)
						result = 1.28;
					else if (calcTotal < 200)
						result = 1.31;
					else if (calcTotal < 250)
						result = 1.34;
					else if (calcTotal < 290)
						result = 1.37;
					else if (calcTotal < 300)
						result = 1.4;
					else if (calcTotal < 330)
						result = 1.43;
					else if (calcTotal < 360)
						result = 1.46;
					else if (calcTotal < 410)
						result = 1.49;
					else if (calcTotal < 500)
						result = 1.52;
					else if (calcTotal < 700)
						result = 1.55;
					else
						result = 1.58;
				}
			}
		}
		else
		{
			if (attacker.calcStat(Stats.IGNORE_ELEMENTALS, 0, null, null) > 0 || target.calcStat(Stats.IGNORE_ELEMENTALS, 0, null, null) > 0) // ignore ele
				return 1;
			element = attacker.getAttackElement();
			if (element >= 0)
			{
				calcPower = attacker.getAttackElementValue(element);
				calcDefen = target.getDefenseElementValue(element);
				calcTotal = Math.max(calcPower - calcDefen, -20);
				calcTotal = Math.min(calcTotal, 100);
				L2WeaponType type = null;
				if (attacker.getActiveWeaponItem() != null)
					type = attacker.getActiveWeaponItem().getItemType();
				if (type != null && type == L2WeaponType.BOW)
					result += calcTotal * 0.00335;
				else
					// result += calcTotal * 0.00346;
					result += ((calcPower - calcDefen) / 1000.);
			}
		}
		return result;
	}
	
	/**
	 * Calculate skill reflection according these three possibilities:
	 * <li>Reflect failed</li>
	 * <li>
	 * Mormal reflect (just effects). <U>Only possible for skilltypes: BUFF, REFLECT, HEAL_PERCENT,
	 * MANAHEAL_PERCENT, HOT, CPHOT, MPHOT</U></li>
	 * <li>vengEance reflect (100% damage reflected but
	 * damage is also dealt to actor). <U>This is only possible for skills with skilltype PDAM,
	 * BLOW, CHARGEDAM, MDAM or DEATHLINK</U></li> <br>
	 * <br>
	 * 
	 * @param actor
	 * @param target
	 * @param skill
	 * @return SKILL_REFLECTED_FAILED, SKILL_REFLECT_SUCCEED or SKILL_REFLECT_VENGEANCE
	 */
	@SuppressWarnings("incomplete-switch")
	public static byte calcSkillReflect(L2Character attacker, L2Character target, L2Skill skill)
	{
		if (skill == null || skill.ignoreResists() || !skill.canBeReflected() || (attacker != null && attacker.isInvul()))
			return SKILL_REFLECT_FAILED;
		if (skill.getCastRange(attacker) == -1)
			return SKILL_REFLECT_FAILED;
		if (attacker instanceof L2MonsterInstance)
		{
			if (attacker.getLevel() >= 90 || attacker instanceof L2RaidBossInstance)
				return SKILL_REFLECT_FAILED;
		}
		final int range = (int) Util.calculateDistance(attacker, target, false);
		boolean rangedPhysicalSkill = range > MELEE_ATTACK_RANGE;
		byte reflect = SKILL_REFLECT_FAILED;
		// check for non-reflected skilltypes, need additional retail check
		switch (skill.getSkillType())
		{
			case BUFF:
			case REFLECT:
			case HEAL_PERCENT:
			case MANAHEAL_PERCENT:
			case HOT:
			case CPHOT:
			case MPHOT:
			case UNDEAD_DEFENSE:
			case AGGDEBUFF:
			case CONT:
				return SKILL_REFLECT_FAILED;
			// these skill types can deal damage
			case PDAM:
			case BLOW:
			case MDAM:
			case DEATHLINK:
			case CHARGEDAM:
			case CPDAM:
			case MANADAM:
			case FATAL:
				final Stats stat = skill.isMagic() ? Stats.VENGEANCE_SKILL_MAGIC_DAMAGE : rangedPhysicalSkill ? Stats.VENGEANCE_RANGED_SKILL_DAMAGE : Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE;
				final double venganceChance = target.calcStat(stat, 0, attacker, skill);
				if (venganceChance - (attacker != null ? attacker.calcStat(Stats.REFLECT_RES, 0, target, skill) : 0) > Rnd.get(100))
				{
					reflect |= SKILL_REFLECT_VENGEANCE;
					if (attacker != null && target != null && target instanceof L2PcInstance && skill.isDamaging())
						target.sendMessage("You reflected " + attacker.getDisplayName() + "'s " + skill.getName() + "'s damage");
				}
				break;
		}
		final double reflectChance = target.calcStat(skill.isMagic() ? Stats.REFLECT_SKILL_MAGIC : rangedPhysicalSkill ? Stats.VENGEANCE_RANGED_SKILL_DAMAGE : Stats.REFLECT_SKILL_PHYSIC, 0, attacker, skill);
		if (Rnd.get(100) < reflectChance)
		{
			reflect |= SKILL_REFLECT_SUCCEED;
			if (attacker != null && target != null && target instanceof L2PcInstance && (skill.isDebuff() || (skill.isOffensive() && !skill.isDamaging())))
				target.sendMessage("You reflected " + attacker.getDisplayName() + "'s " + skill.getName());
		}
		return reflect;
	}
	
	public static float calcWeaponResistanceModifier(L2Weapon weapon, L2Character target, L2Skill skill)
	{
		float multi = 1;
		if (weapon.getCrystalType() == L2Item.CRYSTAL_S)
		{
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_S, 0, target, skill)) / 100;
		}
		else if (weapon.getCrystalType() == L2Item.CRYSTAL_S80)
		{
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_S80, 0, target, skill)) / 100;
		}
		else if (weapon.getCrystalType() == L2Item.CRYSTAL_S84)
		{
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_S84, 0, target, skill)) / 100;
		}
		if (weapon.isStandardShopItem() && weapon.getCrystalType() == L2Item.CRYSTAL_S)
		{
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_NORMAL_S, 0, target, skill)) / 100;
		}
		else if (weapon.getName().contains("Dynasty"))
		{
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_DYNASTY, 0, target, skill)) / 100;
		}
		else if (weapon.getName().contains("Icarus"))
		{
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_ICARUS, 0, target, skill)) / 100;
		}
		else if (weapon.getName().contains("Vesper"))
		{
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_VESPER, 0, target, skill)) / 100;
		}
		else if (weapon.getName().contains("Titanium"))
		{
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_TITANIUM, 0, target, skill)) / 100;
		}
		else if (weapon.getName().contains("Dread"))
		{
			multi *= (100 - target.calcStat(Stats.RESIST_WEAPON_DREAD, 0, target, skill)) / 100;
		}
		return multi;
	}
}
