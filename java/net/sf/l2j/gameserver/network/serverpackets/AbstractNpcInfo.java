package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.FakePcsTable;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.TownManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Transformation;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.FakePc;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2FameManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2OlympiadManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SymbolMakerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TeleporterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TrapInstance;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.events.Domination;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.chars.L2PcTemplate;
import net.sf.l2j.util.Rnd;

public abstract class AbstractNpcInfo extends L2GameServerPacket
{
	private static final String	_S__22_NPCINFO	= "[S] 0c NpcInfo";
	protected int				_x, _y, _z, _heading;
	protected int				_idTemplate;
	protected boolean			_isAttackable, _isSummoned;
	protected int				_mAtkSpd, _pAtkSpd;
	protected int				_rhand, _lhand, _chest;
	protected float				_collisionHeight;
	protected float				_collisionRadius;
	protected String			_name			= "";
	protected String			_title			= "";
	/**
	 * Run speed, swimming run speed and flying run speed
	 */
	protected int				_runSpd;
	/**
	 * Walking speed, swimming walking speed and flying walking speed
	 */
	protected int				_walkSpd;
	
	public AbstractNpcInfo(L2Character cha)
	{
		_isSummoned = cha.isShowSummonAnimation();
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_heading = cha.getHeading();
		_mAtkSpd = cha.getMAtkSpd(null);
		_pAtkSpd = cha.getPAtkSpd(null);
		_runSpd = cha.getRunSpeed();
		_walkSpd = cha.getWalkSpeed();
	}
	
	@Override
	public String getType()
	{
		return _S__22_NPCINFO;
	}
	
	/**
	 * Packet for Npcs
	 */
	public static class NpcInfo extends AbstractNpcInfo
	{
		private final L2Npc	_npc;
		private int			_clanCrest				= 0;
		private int			_allyCrest				= 0;
		private int			_allyId					= 0;
		private int			_clanId					= 0;
		private int			_pledgeclass			= 4;
		private int			_pledgetype				= 0;
		private boolean		_demoniclyMoving;
		private float		_moveMultiplier			= 1;
		private float		_attackSpeedMultiplier	= 1;
		private int			_class					= 0, _race = 0;
		
		public NpcInfo(L2Npc cha, L2Character attacker)
		{
			super(cha);
			_npc = cha;
			_idTemplate = cha.getTemplate().idTemplate; // On every subclass
			_rhand = cha.getRightHandItem(); // On every subclass
			_lhand = cha.getLeftHandItem(); // On every subclass
			_collisionHeight = cha.getCurrCollisionHeight();// On every subclass
			_collisionRadius = cha.getCurrCollisionRadius();// On every subclass
			_isAttackable = cha.isAutoAttackable(attacker);
			if (cha.getTemplate().serverSideName)
				_name = cha.getTemplate().name;// On every subclass
			if (Config.L2JMOD_CHAMPION_ENABLE && cha.isChampion())
				_title = (Config.L2JMOD_CHAMP_TITLE); // On every subclass
			else if (cha.getTemplate().serverSideTitle)
				_title = cha.getTemplate().title; // On every subclass
			else
				_title = cha.getTitle(); // On every subclass
			_invisible = cha.isInvisible();
			if (_npc.isAPC())
			{
				final FakePc fpc = FakePcsTable.getInstance().getFakePc(_npc.getNpcId());
				if (fpc != null)
				{
					switch (fpc.race)
					{
						case 1: // elf
							_class = 21;
							_race = 1;
							break;
						case 2: // dark elf
							_class = 34;
							_race = 2;
							break;
						case 3: // orc fighter
							_class = 48;
							_race = 3;
							break;
						case 4: // dwarf
							_class = 54;
							_race = 4;
							break;
						case 5: // kamael
							_class = 136;
							_race = 5;
							break;
						case 6: // human mystic
							_class = 16;
							_race = 0;
							break;
						case 7: // orc mystic
							_class = 52;
							_race = 3;
							break;
						default: // human fighter
							_class = 2;
							_race = 0;
							break;
					}
					final L2PcTemplate pctmpl = CharTemplateTable.getInstance().getTemplate(_class);
					_attackSpeedMultiplier = _npc.getStat().getFakeAttackSpeedMultiplier(pctmpl.basePAtkSpd);
					_moveMultiplier = _npc.getStat().getFakeMovementSpeedMultiplier(pctmpl.baseRunSpd);
					if (_moveMultiplier == 0)
						_moveMultiplier = 1;
					_runSpd /= _moveMultiplier;
					_walkSpd /= _moveMultiplier;
					_demoniclyMoving = _npc.calcStat(Stats.PHAZE_MOVEMENT, 0, null, null) > 0;
					final L2PcInstance player = L2World.getInstance().getPlayer(_npc.getName());
					if (player != null)
					{
						_pledgeclass = player.getPledgeClass();
						_pledgetype = player.getPledgeType();
					}
				}
			}
//			if (_npc instanceof L2MuseumStatueInstance)
//			{
//				L2MuseumStatueInstance statue = (L2MuseumStatueInstance) _npc;
//				if (statue != null)
//				{
//					switch (statue.getCharLooks().getRace())
//					{
//						case 1: // elf
//							_class = 21;
//							_race = 1;
//							break;
//						case 2: // dark elf
//							_class = 34;
//							_race = 2;
//							break;
//						case 3: // orc fighter
//							_class = 48;
//							_race = 3;
//							break;
//						case 4: // dwarf
//							_class = 54;
//							_race = 4;
//							break;
//						case 5: // kamael
//							_class = 136;
//							_race = 5;
//							break;
//						case 6: // human mystic
//							_class = 16;
//							_race = 0;
//							break;
//						case 7: // orc mystic
//							_class = 52;
//							_race = 3;
//							break;
//						default: // human fighter
//							_class = 2;
//							_race = 0;
//							break;
//					}
//					
//					if (statue.getCharLooks().getSex() == 0)
//					{
//						switch (statue.getCharLooks().getRace()) // fake male r
//						{
//							case 1:
//								_collisionRadius = (float) 7.5;
//								_collisionHeight = 24;
//							case 2:
//								_collisionRadius =  (float) 7.5;
//								_collisionHeight = 24;
//							case 3:
//								_collisionRadius =  11;
//								_collisionHeight = 28;
//							case 4:
//								_collisionRadius =  9;
//								_collisionHeight = 18;
//							case 5:
//								_collisionRadius =  (float) 7.5;
//								_collisionHeight = (float) 24.5;
//							case 6:
//								_collisionRadius =  (float) 7.5;
//								_collisionHeight = (float) 22.5;
//							case 7:
//								_collisionRadius =  7;
//								_collisionHeight = 27;
//							default:
//								_collisionRadius =  9;
//								_collisionHeight = 23;
//						}
//					}
//					else
//					{
//						switch (statue.getCharLooks().getRace()) // fake female r
//						{
//							case 1:
//								_collisionRadius =  7;
//								_collisionHeight = 23;
//							case 2:
//								_collisionRadius =  (float) 7.5;
//								_collisionHeight = 23;
//							case 3:
//								_collisionRadius =  (float) 7.5;
//								_collisionHeight = 27;
//							case 4:
//								_collisionRadius =  5;
//								_collisionHeight = 19;
//							case 5:
//								_collisionRadius =  (float) 7.5;
//								_collisionHeight = 22;
//							case 6:
//								_collisionRadius =  7;
//								_collisionHeight = 22;
//							case 7:
//								_collisionRadius =  8;
//								_collisionHeight = 25;
//							default:
//								_collisionRadius =  8;
//								_collisionHeight = 23;
//						}
//					}
//					final L2PcTemplate pctmpl = CharTemplateTable.getInstance().getTemplate(_class);
//					_attackSpeedMultiplier = _npc.getStat().getFakeAttackSpeedMultiplier(pctmpl.basePAtkSpd);
//					_moveMultiplier = _npc.getStat().getFakeMovementSpeedMultiplier(pctmpl.baseRunSpd);
//					if (_moveMultiplier == 0)
//						_moveMultiplier = 1;
//					_runSpd /= _moveMultiplier;
//					_walkSpd /= _moveMultiplier;
//					_demoniclyMoving = _npc.calcStat(Stats.PHAZE_MOVEMENT, 0, null, null) > 0;
//					final L2PcInstance player = L2World.getInstance().getPlayer(_npc.getName());
//					if (player != null)
//					{
//						_pledgeclass = player.getPledgeClass();
//						_pledgetype = player.getPledgeType();
//					}
//				}
//			}
			else
			{
				_attackSpeedMultiplier = _npc.getAttackSpeedMultiplier();
				_moveMultiplier = _npc.getMovementSpeedMultiplier();
			}
			if (Config.SHOW_NPC_LVL && _npc instanceof L2MonsterInstance && ((L2Attackable) _npc).canShowLevelInTitle())
			{
				String t = "Lv " + cha.getLevel() + (cha.getAggroRange() > 0 ? "*" : "");
				if (_title != null)
					t += " " + _title;
				if (t.length() < 24)
					_title = t;
			}
			if (cha instanceof L2NpcInstance || cha instanceof L2GuardInstance || cha instanceof L2TeleporterInstance || cha instanceof L2SymbolMakerInstance || cha instanceof L2FameManagerInstance || cha instanceof L2OlympiadManagerInstance)
			{
				final ClanHall chall = ClanHallManager.getInstance().getNearbyClanHall(cha.getX(), cha.getY(), 70);
				if (chall != null && chall.getOwnerId() > 0)
				{
					final L2Clan clan = ClanTable.getInstance().getClan(chall.getOwnerId());
					_clanCrest = clan.getCrestId();
					_clanId = clan.getClanId();
					_allyCrest = clan.getAllyCrestId();
					_allyId = clan.getAllyId();
				}
				else if (cha.isInsideZone(L2Character.ZONE_TOWN) && cha.getCastle().getOwnerId() != 0)
				{
					if (TownManager.getTown(_x, _y, _z) != null)
					{
						final int townId = TownManager.getTown(_x, _y, _z).getTownId();
						if (townId != 33 && townId != 22 && townId != 5 && townId != 11)
						{
							final L2Clan clan = ClanTable.getInstance().getClan(cha.getCastle().getOwnerId());
							_clanCrest = clan.getCrestId();
							_clanId = clan.getClanId();
							_allyCrest = clan.getAllyCrestId();
							_allyId = clan.getAllyId();
						}
						else
						{
							_clanCrest = 0;
							_clanId = 0;
							_allyCrest = 0;
							_allyId = 0;
						}
					}
				}
			}
		}
		
		private static final int[] PAPERDOLL_ORDER = new int[]
		{
			Inventory.PAPERDOLL_RHAND,
			Inventory.PAPERDOLL_LHAND,
			Inventory.PAPERDOLL_GLOVES,
			Inventory.PAPERDOLL_CHEST,
			Inventory.PAPERDOLL_LEGS,
			Inventory.PAPERDOLL_FEET,
			Inventory.PAPERDOLL_BACK,
			Inventory.PAPERDOLL_RHAND,
			Inventory.PAPERDOLL_HAIR,
			Inventory.PAPERDOLL_HAIR2,
		};
		
		@Override
		protected void writeImpl()
		{
			final FakePc fpc = FakePcsTable.getInstance().getFakePc(_npc.getNpcId());
			if (fpc != null)
			{
				writeC(0x31);
				writeD(_x);
				writeD(_y);
				if (_demoniclyMoving)
					writeD(_z);
				else
					writeD(_z + 24);
				writeD(_heading);
				writeD(_npc.getObjectId());
				writeS(fpc.name);
				writeD(_race);
				writeD(fpc.sex);
				writeD(_class);
				writeD(0x00);
				writeD(fpc.pdHead);
				writeD(fpc.pdRHand);
				writeD(_race != 5 ? fpc.pdLHand : 0);
				writeD(fpc.pdGloves);
				writeD(fpc.pdChest);
				writeD(fpc.pdLegs);
				writeD(fpc.pdFeet);
				writeD(fpc.pdBack);
				writeD(fpc.pdRHand);
				writeD(fpc.pdHair);
				writeD(fpc.pdHair2);
				// T1 new d's
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				// end of t1 new d's
				// CT2.3
				// c6 new h's
				writeD(0x00);
				writeD(0x00);
				writeD(fpc.pdRHandAug);
				writeD(_race != 5 ? fpc.pdLHandAug : 0);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(fpc.pdRHandAug);
				writeD(0x00);
				writeD(0x00);
				// T1 new h's
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				// end of t1 new h's
				// CT2.3
				writeD(fpc.pvpFlag);
				writeD(fpc.karma);
				writeD(_mAtkSpd);
				writeD(_pAtkSpd);
				writeD(fpc.pvpFlag);
				writeD(fpc.karma);
				writeD(_runSpd);
				writeD(_walkSpd);
				writeD(_runSpd); // swimspeed
				writeD(_walkSpd); // swimspeed
				writeD(_runSpd);
				writeD(_walkSpd);
				writeD(_runSpd);
				writeD(_walkSpd);
				writeF(_moveMultiplier); // _activeChar.getProperMultiplier()
				writeF(_attackSpeedMultiplier); // _activeChar.getAttackSpeedMultiplier()
				writeF(_npc.getCollisionRadius());
				writeF(_npc.getCollisionHeight());
				writeD(fpc.hairStyle);
				writeD(fpc.hairColor);
				writeD(fpc.face);
				writeS(fpc.title);
				writeD(fpc.clanid);
				writeD(fpc.clancrestid);
				writeD(fpc.allyid);
				writeD(fpc.allycrestid);
				// In UserInfo leader rights and siege flags, but here found nothing??
				// Therefore RelationChanged packet with that info is required
				writeD(0);
				writeC(0x01); // standing = 1 sitting = 0
				writeC(_npc.isRunning() ? 1 : 0); // running = 1 walking = 0
				writeC(_npc.isInCombat() ? 1 : 0);
				writeC(_npc.isAlikeDead() ? 1 : 0);
				writeC((fpc.invisible == 1 || _invisible) ? 1 : 0); // invisible = 1 visible =0
				writeC(fpc.mount); // 1 on strider 2 on wyvern 3 on Great Wolf 0 no mount
				writeC(0x00); // 1 - sellshop
				writeH(0x00); // cubic count
				// for (int id : allCubics)
				// writeH(id);
				writeC(0x00); // find party members
				writeD(_npc.getAbnormalEffectAPC(fpc));
				writeC(0x00); // Changed by Thorgrim
				writeH(0x00); // Blue value for name (0 = white, 255 = pure blue)
				writeD(_class); // clazz
				writeD(0x00);// max cp
				writeD(0x00);// cur cp
				writeC(fpc.enchantEffect);
				writeC(fpc.team); // team circle around feet 1= Blue, 2 = red
				writeD(0x00);
				writeC(0x00); // Symbol on char menu ctrlI
				writeC(fpc.hero); // Hero Aura
				writeC(fpc.fishing); // 0x01: Fishing Mode (Cant be undone by setting back to 0)
				writeD(fpc.fishingX);
				writeD(fpc.fishingY);
				writeD(fpc.fishingZ);
				writeD(Integer.decode("0x" + fpc.nameColor));
				writeD(_heading); // isRunning() as in UserInfo?
				writeD(_pledgeclass); // pledge class
				writeD(_pledgetype); // pledge type
				writeD(Integer.decode("0x" + fpc.titleColor));
				// writeD(0x00); // ??
				writeD(0x00); // cursed weapon level
				writeD(3000); // clan reputation score
				// T1
				writeD(0x00); // transformation id
				writeD(0x00); // agathion id
				// T2
				writeD(0x01);
				// T2.3
				writeD(_npc.getSpecialEffectAPC(fpc));
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
			}
//			if (_npc instanceof L2MuseumStatueInstance)
//			{
//				L2MuseumStatueInstance statue = (L2MuseumStatueInstance) _npc;
//				writeC(0x31);
//				writeD(_x);
//				writeD(_y);
//				writeD(_z);
//				writeD(0);
//				writeD(_npc.getObjectId());
//				writeS(statue.getCharLooks().getName());
//				writeD(_race);
//				writeD(statue.getCharLooks().getSex());
//				writeD(_class);
//				writeD(0x00);
//				writeD(statue.getCharLooks().getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
//				writeD(statue.getCharLooks().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
//				writeD(statue.getCharLooks().getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
//				writeD(statue.getCharLooks().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
//				writeD(statue.getCharLooks().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
//				writeD(statue.getCharLooks().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
//				writeD(statue.getCharLooks().getPaperdollItemId(Inventory.PAPERDOLL_BACK));
//				writeD(statue.getCharLooks().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
//				writeD(statue.getCharLooks().getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
//				writeD(statue.getCharLooks().getPaperdollItemId(Inventory.PAPERDOLL_HAIR2));
//				// T1 new d's
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				// end of t1 new d's
//				// CT2.3
//				// c6 new h's
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				// T1 new h's
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				// end of t1 new h's
//				// CT2.3
//				writeD(0x00);
//				writeD(0x00);
//				writeD(_mAtkSpd);
//				writeD(_pAtkSpd);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(_runSpd);
//				writeD(_walkSpd);
//				writeD(_runSpd); // swimspeed
//				writeD(_walkSpd); // swimspeed
//				writeD(_runSpd);
//				writeD(_walkSpd);
//				writeD(_runSpd);
//				writeD(_walkSpd);
//				writeF(_moveMultiplier); // _activeChar.getProperMultiplier()
//				writeF(_attackSpeedMultiplier); // _activeChar.getAttackSpeedMultiplier()
//				L2PcTemplate cl = CharTemplateTable.getInstance().getTemplate(statue.getCharLooks().getBaseClassId());
//				writeF(_collisionRadius);
//				writeF(_collisionHeight);
//				writeD(statue.getCharLooks().getHairStyle());
//				writeD(statue.getCharLooks().getHairColor());
//				writeD(statue.getCharLooks().getFace());
//				writeS(_npc.getTitle());
//				writeD(0x00);// clanid
//				writeD(0x00);// clanCrest
//				writeD(0x00);// allyId
//				writeD(0x00);// allyCrest
//				// In UserInfo leader rights and siege flags, but here found nothing??
//				// Therefore RelationChanged packet with that info is required
//				writeD(0);
//				writeC(0x01); // standing = 1 sitting = 0
//				writeC(0x01); // running = 1 walking = 0
//				writeC(0x00);
//				writeC(0x00);
//				writeC(0x00); // invisible = 1 visible =0
//				writeC(0x00); // 1 on strider 2 on wyvern 3 on Great Wolf 0 no mount
//				writeC(0x00); // 1 - sellshop
//				writeH(0x00); // cubic count
//				// for (int id : allCubics)
//				// writeH(id);
//				writeC(0x00); // find party members
//				writeD(0x00);
//				writeC(0x00); // Changed by Thorgrim
//				writeH(0x00); // Blue value for name (0 = white, 255 = pure blue)
//				writeD(_class); // clazz
//				writeD(0x00);// max cp
//				writeD(0x00);// cur cp
//				writeC(statue.getCharLooks().getEnchantEffect());
//				writeC(0x00); // team circle around feet 1= Blue, 2 = red
//				writeD(0x00);
//				writeC(0x00); // Symbol on char menu ctrlI
//				writeC(0x00); // Hero Aura
//				writeC(0x00); // 0x01: Fishing Mode (Cant be undone by setting back to 0)
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0xFFFFFF);
//				writeD(_heading); // isRunning() as in UserInfo?
//				writeD(_pledgeclass); // pledge class
//				writeD(_pledgetype); // pledge type
//				writeD(0x7d7d7b);
//				// writeD(0x00); // ??
//				writeD(0x00); // cursed weapon level
//				writeD(3000); // clan reputation score
//				// T1
//				writeD(0x00); // transformation id
//				writeD(0x00); // agathion id
//				// T2
//				writeD(0x01);
//				// T2.3
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//				writeD(0x00);
//			}
			else
			{
				writeC(0x0c);
				writeD(_npc.getObjectId());
				writeD(_idTemplate + 1000000); // npctype id
				writeD(_isAttackable ? 1 : 0);
				writeD(_x);
				writeD(_y);
				writeD(_z);
				writeD(_heading);
				writeD(0x00);
				writeD(_mAtkSpd);
				writeD(_pAtkSpd);
				writeD(_runSpd);
				writeD(_walkSpd);
				writeD(_runSpd); // swimspeed
				writeD(_walkSpd); // swimspeed
				writeD(_runSpd);
				writeD(_walkSpd);
				writeD(_runSpd);
				writeD(_walkSpd);
				writeF(_moveMultiplier);
				writeF(_attackSpeedMultiplier);
				writeF(_collisionRadius);
				writeF(_collisionHeight);
				writeD(_rhand); // right hand weapon
				writeD(_chest);
				writeD(_lhand); // left hand weapon
				writeC(0x01); // name above char 1=true ... ??
				writeC(_npc.isRunning() ? 1 : 0);
				writeC(_npc.isInCombat() ? 1 : 0);
				writeC(_npc.isAlikeDead() ? 1 : 0);
				writeC(_isSummoned ? 2 : 0); // 0=teleported 1=default 2=summoned
				writeS(_name);
				writeS(_title);
				writeD(0x00); // Title color 0=client default - maybe it's show or not show title
				writeD(0x00); // unknown
				if (_npc.getRare() > 0)
				{
					if (_npc.getElite() > 0)
						writeD(99999); // karma amount
					else
						writeD(0x01); // karma amount
				}
				else
					writeD(0x00); // karma amount
				writeD(_npc.getAbnormalEffect()); // C2
				writeD(_clanId); // clan id
				writeD(_clanCrest); // crest id
				writeD(_allyId); // ally id
				writeD(_allyCrest); // all crest
				writeC(_npc.isFlying() ? 2 : 0); // C2
				if (Domination.getInstance().state.equals(Domination.State.ACTIVE) && _npc.getNpcId() == 55555)
				{
					if (Domination.getInstance().getLastWinningTeam() == "blue")
						writeC(0x01);
					else if (Domination.getInstance().getLastWinningTeam() == "red")
						writeC(0x02);
					else
						writeC(0x00);
				}
				else
				{
					writeC(0x00); // title color 0=client
				}
				writeF(_collisionRadius);
				writeF(_collisionHeight);
				writeD(0x00); // C4
				writeD(_npc.isFlying() ? 1 : 0); // C6
				writeD(0x00);
				writeD(0x00);// CT1.5 Pet form and skills
				writeC(0x01);
				writeC(0x01);
				writeD(_npc.getSpecialEffect());
			}
		}
	}
	
	public static class TrapInfo extends AbstractNpcInfo
	{
		private final L2TrapInstance _trap;
		
		public TrapInfo(L2TrapInstance cha, L2Character attacker)
		{
			super(cha);
			_trap = cha;
			_idTemplate = cha.getTemplate().idTemplate;
			_isAttackable = cha.isAutoAttackable(attacker);
			_rhand = 0;
			_lhand = 0;
			_collisionHeight = _trap.getCollisionHeight();
			/* _collisionRadius = _trap.getTemplate().collisionRadius; */
			_title = cha.getOwner().getDisplayName();
			_runSpd = _trap.getRunSpeed();
			_walkSpd = _trap.getWalkSpeed();
		}
		
		@Override
		protected void writeImpl()
		{
			writeC(0x0c);
			writeD(_trap.getObjectId());
			writeD(_idTemplate + 1000000); // npctype id
			writeD(0); // isattackable
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_heading);
			writeD(0x00);
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_runSpd); // swimspeed
			writeD(_walkSpd); // swimspeed
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeF(1);
			writeF(1);
			writeF(0); // collisionradius
			writeF(_collisionHeight);
			writeD(_rhand); // right hand weapon
			writeD(_chest);
			writeD(_lhand); // left hand weapon
			writeC(1); // name above char 1=true ... ??
			writeC(1);
			writeC(_trap.isInCombat() ? 1 : 0);
			writeC(_trap.isAlikeDead() ? 1 : 0);
			writeC(_isSummoned ? 2 : 0); // 0=teleported 1=default 2=summoned
			writeS(_name);
			writeS(_title);
			writeD(0x00); // title color 0 = client default
			writeD(0x00);
			writeD(0x00); // pvp flag
			writeD(_trap.getAbnormalEffect()); // C2
			writeD(0x00); // clan id
			writeD(0x00); // crest id
			writeD(0000); // C2
			writeD(0000); // C2
			writeD(0000); // C2
			writeC(0000); // C2
			writeC(0x00); // Title color 0=client default
			writeF(0); // collisionradius
			writeF(_collisionHeight);
			writeD(0x00); // C4
			writeD(0x00); // C6
			writeD(0x00);
			writeD(0);// CT1.5 Pet form and skills
			writeC(0x01);
			writeC(0x01);
			writeD(0x00);
		}
	}
	
	/**
	 * Packet for Decoys
	 */
	public static class DecoyInfo extends AbstractNpcInfo
	{
		private final L2Decoy	_decoy;
		private float			_moveMultiplier			= 1;
		private float			_attackSpeedMultiplier	= 1;
		
		public DecoyInfo(L2Decoy cha)
		{
			super(cha);
			_idTemplate = cha.getTemplate().idTemplate;
			_decoy = cha;
			_heading = cha.getOwner().getHeading();
			_pAtkSpd = cha.getOwner().getPAtkSpd(null);
			_runSpd = cha.getOwner().getRunSpeed();
			_walkSpd = cha.getOwner().getWalkSpeed();
			_moveMultiplier = _decoy.getOwner().getMovementSpeedMultiplier();
			_attackSpeedMultiplier = _decoy.getOwner().getAttackSpeedMultiplier();
			/*
			 * if (_idTemplate < 13071 || _idTemplate > 13076 )
			 * {
			 * if (Config.ASSERT)
			 * throw new AssertionError("Using DecoyInfo packet with an unsupported decoy template: "+_idTemplate);
			 * else
			 * throw new IllegalArgumentException("Using DecoyInfo packet with an unsupported decoy template: "+_idTemplate);
			 * }
			 */
		}
		
		@Override
		protected void writeImpl()
		{
			writeC(0x31);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_heading);
			writeD(_decoy.getObjectId());
			writeS(_decoy.getOwner().getDisplayName());
			writeD(_decoy.getOwner().getRace().getRealOrdinal());
			writeD(_decoy.getOwner().getAppearance().getSex() ? 1 : 0);
			if (_decoy.getOwner().getRace().ordinal() == 6)// human mage
				writeD(0x10); // bishop
			else if (_decoy.getOwner().getRace().ordinal() == 7)// orc mystic
				writeD(0x34); // warcryer
			else if (_decoy.getOwner().getRace().ordinal() == 3)// orc fighter
				writeD(0x2e); // destroyer
			else if (_decoy.getOwner().getRace().ordinal() == 0)// human fighter
				writeD(0x58); // duelist
			else if (_decoy.getOwner().getRace().ordinal() == 1)// elf
				writeD(0x63); // eva templar
			else if (_decoy.getOwner().getRace().ordinal() == 2)// dark elf
				writeD(0x6a); // shillien templar
			else if (_decoy.getOwner().getRace().ordinal() == 4)// dwarf
				writeD(0x75); // fortune seeker
			else if (_decoy.getOwner().getRace().ordinal() == 5)// kamael
				writeD(0x86); // Kamael - Trickster
			else
				writeD(0x87); // kamael - inspector
			
			//System.out.println("DECOY INFO: " + _decoy.getOwner().getRace().ordinal());
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_HAIRALL));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_HEAD));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_RHAND));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_LHAND));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_GLOVES));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_CHEST));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_LEGS));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_FEET));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_BACK));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_LRHAND));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_HAIR));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_HAIR2));
			// T1 new d's
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_RBRACELET));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_LBRACELET));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO1));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO2));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO3));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO4));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO5));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO6));
			// end of t1 new d's
			// CT2.3
			writeD(0x00);
			// c6 new h's
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_UNDER));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_HEAD));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_RHAND));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_LHAND));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_GLOVES));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_CHEST));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_LEGS));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_FEET));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_BACK));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_LRHAND));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_HAIR));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_HAIR2));
			// T1 new h's
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_RBRACELET));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_LBRACELET));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO1));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO2));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO3));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO4));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO5));
			writeD(_decoy.getOwner().getInventory().getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_DECO6));
			// end of t1 new h's
			// CT2.3
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(_decoy.getOwner().getPvpFlag());
			writeD(_decoy.getOwner().getKarma());
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			writeD(_decoy.getOwner().getPvpFlag());
			writeD(_decoy.getOwner().getKarma());
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(50); // swimspeed
			writeD(50); // swimspeed
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeF(_moveMultiplier); // _activeChar.getProperMultiplier()
			writeF(_attackSpeedMultiplier); // _activeChar.getAttackSpeedMultiplier()
			L2Summon pet = _decoy.getOwner().getPet();
			L2Transformation trans;
			if (_decoy.getOwner().getMountType() != 0 && pet instanceof L2SummonInstance && pet != null)
			{
				writeF(pet.getCollisionRadius());
				writeF(pet.getCollisionHeight());
			}
			else if ((trans = _decoy.getOwner().getTransformation()) != null)
			{
				writeF(trans.getCollisionRadius());
				writeF(trans.getCollisionHeight());
			}
			else
			{
				writeF(_decoy.getOwner().getCollisionRadius());
				writeF(_decoy.getOwner().getCollisionHeight());
			}
			writeD(_decoy.getOwner().getAppearance().getHairStyle());
			writeD(_decoy.getOwner().getAppearance().getHairColor());
			writeD(_decoy.getOwner().getAppearance().getFace());
			writeS(_decoy.getOwner().getAppearance().getVisibleTitle());
			writeD(_decoy.getOwner().getClanId());
			writeD(_decoy.getOwner().getClanCrestId());
			writeD(_decoy.getOwner().getAllyId());
			writeD(_decoy.getOwner().getAllyCrestId());
			// In UserInfo leader rights and siege flags, but here found nothing??
			// Therefore RelationChanged packet with that info is required
			writeD(0);
			writeC(_decoy.getOwner().isSitting() ? 0 : 1); // standing = 1 sitting = 0
			writeC(_decoy.getOwner().isRunning() ? 1 : 0); // running = 1 walking = 0
			writeC(_decoy.getOwner().isInCombat() ? 1 : 0);
			writeC(_decoy.getOwner().isAlikeDead() ? 1 : 0);
			writeC(0); // invisible = 1 visible = 0
			writeC(_decoy.getOwner().getMountType()); // 1 on strider 2 on wyvern 3 on Great Wolf 0 no mount
			writeC(_decoy.getOwner().getPrivateStoreType()); // 1 - sellshop
			writeH(_decoy.getOwner().getCubics().size());
			for (int id : _decoy.getOwner().getCubics().keySet())
				writeH(id);
			writeC(0x00); // find party members
			writeD(_decoy.getOwner().getAbnormalEffectDecoy());
			writeC(_decoy.getOwner().getRecomLeft()); // Changed by Thorgrim
			writeH(_decoy.getOwner().getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
			writeD(_decoy.getOwner().getClassId().getId());
			//writeD(0x00);
			writeD(_decoy.getOwner().getMaxCp());
			writeD((int) _decoy.getOwner().getCurrentCp());
			writeC(_decoy.getOwner().isMounted() ? 0 : _decoy.getOwner().isDisguised() ? Math.max(_decoy.getOwner().getEnchantEffect() + Rnd.get(-5, 1), 0) : _decoy.getOwner().getEnchantEffect());
			if (_decoy.getOwner().getTeam() == 1)
				writeC(0x01); // team circle around feet 1= Blue, 2 = red
			else if (_decoy.getOwner().getTeam() == 2)
				writeC(0x02); // team circle around feet 1= Blue, 2 = red
			else
				writeC(0x00); // team circle around feet 1= Blue, 2 = red
			writeD(_decoy.getOwner().getClanCrestLargeId());
			writeC(_decoy.getOwner().isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
			writeC(_decoy.getOwner().isHero() ? 1 : 0); // Hero Aura
			writeC(_decoy.getOwner().isFishing() ? 1 : 0); // 0x01: Fishing Mode (Cant be undone by setting back to 0)
			writeD(_decoy.getOwner().getFishx());
			writeD(_decoy.getOwner().getFishy());
			writeD(_decoy.getOwner().getFishz());
			writeD(_decoy.getOwner().getAppearance().getNameColor());
			writeD(0x00); // isRunning() as in UserInfo?
			//writeD(0x00);
			writeD(_decoy.getOwner().getPledgeClass());
			writeD(0x00); // ??
			writeD(_decoy.getOwner().getAppearance().getTitleColor());
			// writeD(0x00); // ??
			if (_decoy.getOwner().isCursedWeaponEquipped())
				writeD(CursedWeaponsManager.getInstance().getLevel(_decoy.getOwner().getCursedWeaponEquippedId()));
			else
				writeD(0x00);
			// T1
			writeD(0x00);
			writeD(_decoy.getOwner().getTransformationId());
		}
	}
	
	/**
	 * Packet for summons
	 */
	public static class SummonInfo extends AbstractNpcInfo
	{
		private final L2Summon	_summon;
		private int				_form					= 0;
		private int				_val					= 0;
		private float			_moveMultiplier			= 1;
		private float			_attackSpeedMultiplier	= 1;
		
		public SummonInfo(L2Summon cha, L2Character attacker, int val)
		{
			super(cha);
			_summon = cha;
			_val = val;
			int npcId = cha.getTemplate().npcId;
			if (npcId == 16041 || npcId == 16042)
			{
				if (cha.getLevel() > 84)
					_form = 3;
				else if (cha.getLevel() > 79)
					_form = 2;
				else if (cha.getLevel() > 74)
					_form = 1;
			}
			else if (npcId == 16025 || npcId == 16037)
			{
				if (cha.getLevel() > 69)
					_form = 3;
				else if (cha.getLevel() > 64)
					_form = 2;
				else if (cha.getLevel() > 59)
					_form = 1;
			}
			// fields not set on AbstractNpcInfo
			_isAttackable = cha.isAutoAttackable(attacker);
			_rhand = cha.getWeapon();
			_lhand = 0;
			_chest = cha.getArmor();
			_name = cha.getName();
			_title = cha.getOwner() != null ? (cha.getOwner().isOnline() == 0 ? "" : cha.getOwner().getDisplayName()) : ""; // when owner online, summon will show in title owner name
			_idTemplate = cha.getTemplate().idTemplate;
			_collisionHeight = cha.getCollisionHeight();
			_collisionRadius = cha.getCollisionRadius();
			_invisible = cha.getOwner() != null ? cha.getOwner().isInvisible() && !cha.getOwner().isInOlympiadMode() : false;
			// few fields needing fix from AbstractNpcInfo
			_runSpd = _summon.getPetSpeed();
			_walkSpd = _summon.isMountable() ? 45 : 30;
			_moveMultiplier = _summon.getMovementSpeedMultiplier();
			_attackSpeedMultiplier = _summon.getAttackSpeedMultiplier();
		}
		
		@Override
		protected void writeImpl()
		{
			boolean gmSeeInvis = false;
			if (_invisible)
			{
				L2PcInstance tmp = getClient().getActiveChar();
				if (tmp != null && tmp.isGM())
					gmSeeInvis = true;
			}
			writeC(0x0c);
			writeD(_summon.getObjectId());
			writeD(_idTemplate + 1000000); // npctype id
			writeD(_isAttackable ? 1 : 0);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_heading);
			writeD(0x00);
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_runSpd); // swimspeed
			writeD(_walkSpd); // swimspeed
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeF(_moveMultiplier);
			writeF(_attackSpeedMultiplier);
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(_rhand); // right hand weapon
			writeD(_chest);
			writeD(_lhand); // left hand weapon
			writeC(1); // name above char 1=true ... ??
			writeC(1); // always running 1=running 0=walking
			writeC(_summon.isInCombat() ? 1 : 0);
			writeC(_summon.isAlikeDead() ? 1 : 0);
			writeC(_val); // 0=teleported 1=default 2=summoned
			writeS(_name);
			writeS(_title);
			writeD(0x01);// Title color 0=client default
			writeD(_summon.getPvpFlag());
			writeD(_summon.getKarma());
			if (gmSeeInvis)
				writeD(_summon.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask());
			else
				writeD(_summon.getAbnormalEffect()); // C2
			writeD(0x00); // clan id
			writeD(0x00); // crest id
			writeD(0000); // C2
			writeD(0000); // C2
			writeC(0000); // C2
			writeC(_summon.getTeam());// Title color 0=client default
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(0x00); // C4
			writeD(0x00); // C6
			writeD(0x00);
			writeD(_form);// CT1.5 Pet form and skills
			writeC(0x01);
			writeC(0x01);
			writeD(0x00);
		}
	}
	
	public static class TowerInfo extends AbstractNpcInfo
	{
		private final L2Decoy	_summon;
		private int				_form	= 0;
		private int				_val	= 0;
		
		public TowerInfo(L2Decoy cha, L2Character attacker, int val)
		{
			super(cha);
			_summon = cha;
			_val = val;
			int npcId = cha.getTemplate().npcId;
			if (npcId == 16041 || npcId == 16042)
			{
				if (cha.getLevel() > 84)
					_form = 3;
				else if (cha.getLevel() > 79)
					_form = 2;
				else if (cha.getLevel() > 74)
					_form = 1;
			}
			else if (npcId == 16025 || npcId == 16037)
			{
				if (cha.getLevel() > 69)
					_form = 3;
				else if (cha.getLevel() > 64)
					_form = 2;
				else if (cha.getLevel() > 59)
					_form = 1;
			}
			// fields not set on AbstractNpcInfo
			_isAttackable = cha.isAutoAttackable(attacker);
			_rhand = 0;
			_lhand = 0;
			_chest = 0;
			_name = cha.getTemplate().name;
			_title = cha.getOwner() != null ? (cha.getOwner().isOnline() == 0 ? "" : cha.getOwner().getDisplayName()) : ""; // when owner online, summon will show in title owner name
			_idTemplate = cha.getTemplate().idTemplate;
			_collisionHeight = cha.getCollisionHeight();
			_collisionRadius = cha.getCollisionRadius();
			_invisible = cha.getOwner() != null ? cha.getOwner().isInvisible() : false;
			// few fields needing fix from AbstractNpcInfo
			_runSpd = 0;
			_walkSpd = 0;
		}
		
		@Override
		protected void writeImpl()
		{
			boolean gmSeeInvis = false;
			if (_invisible)
			{
				L2PcInstance tmp = getClient().getActiveChar();
				if (tmp != null && tmp.isGM())
					gmSeeInvis = true;
			}
			writeC(0x0c);
			writeD(_summon.getObjectId());
			writeD(_idTemplate + 1000000); // npctype id
			writeD(_isAttackable ? 1 : 0);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_heading);
			writeD(0x00);
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_runSpd); // swimspeed
			writeD(_walkSpd); // swimspeed
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_runSpd);
			writeD(_walkSpd);
			writeF(1);
			writeF(1);
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(_rhand); // right hand weapon
			writeD(_chest);
			writeD(_lhand); // left hand weapon
			writeC(1); // name above char 1=true ... ??
			writeC(1); // always running 1=running 0=walking
			writeC(_summon.isInCombat() ? 1 : 0);
			writeC(_summon.isAlikeDead() ? 1 : 0);
			writeC(_val); // 0=teleported 1=default 2=summoned
			writeS(_name);
			writeS(_title);
			writeD(0x01);// Title color 0=client default
			writeD(_summon.getOwner().getPvpFlag());
			writeD(_summon.getOwner().getKarma());
			if (gmSeeInvis)
				writeD(_summon.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask());
			else
				writeD(_summon.getAbnormalEffect()); // C2
			writeD(0x00); // clan id
			writeD(0x00); // crest id
			writeD(0000); // C2
			writeD(0000); // C2
			writeC(0000); // C2
			writeC(_summon.getOwner().getTeam());// Title color 0=client default
			writeF(_collisionRadius);
			writeF(_collisionHeight);
			writeD(0x00); // C4
			writeD(0x00); // C6
			writeD(0x00);
			writeD(_form);// CT1.5 Pet form and skills
			writeC(0x01);
			writeC(0x01);
			writeD(0x00);
		}
	}
}
