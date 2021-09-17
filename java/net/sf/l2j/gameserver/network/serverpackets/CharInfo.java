package net.sf.l2j.gameserver.network.serverpackets;

import java.util.logging.Logger;

import Alpha.autopots.Utils;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.newEvents.NewDM;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

/**
 * 0000: 03 32 15 00 00 44 fe 00 00 80 f1 ff ff 00 00 00 .2...D..........
 * <p>
 * 0010: 00 6b b4 c0 4a 45 00 6c 00 6c 00 61 00 6d 00 69 .k..JE.l.l.a.m.i
 * <p>
 * 0020: 00 00 00 01 00 00 00 01 00 00 00 12 00 00 00 00 ................
 * <p>
 * 0030: 00 00 00 2a 00 00 00 42 00 00 00 71 02 00 00 31 ...*...B...q...1
 * <p>
 * 0040: 00 00 00 18 00 00 00 1f 00 00 00 25 00 00 00 00 ...........%....
 * <p>
 * 0050: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f9 ................
 * <p>
 * 0060: 00 00 00 b3 01 00 00 00 00 00 00 00 00 00 00 7d ...............}
 * <p>
 * 0070: 00 00 00 5a 00 00 00 32 00 00 00 32 00 00 00 00 ...Z...2...2....
 * <p>
 * 0080: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 67 ...............g
 * <p>
 * 0090: 66 66 66 66 66 f2 3f 5f 63 97 a8 de 1a f9 3f 00 fffff.?_c.....?.
 * <p>
 * 00a0: 00 00 00 00 00 1e 40 00 00 00 00 00 00 37 40 01 .............7..
 * <p>
 * 00b0: 00 00 00 01 00 00 00 01 00 00 00 00 00 c1 0c 00 ................
 * <p>
 * 00c0: 00 00 00 00 00 00 00 00 00 01 01 00 00 00 00 00 ................
 * <p>
 * 00d0: 00 00
 * <p>
 * <p>
 * dddddSdddddddddddddddddddddddddddffffdddSdddccccccc (h)
 * <p>
 * dddddSdddddddddddddddddddddddddddffffdddSdddddccccccch
 * dddddSddddddddddddddddddddddddddddffffdddSdddddccccccch (h) c (dchd) ddc dcc c cddd d
 * dddddSdddddddddddddddhhhhhhhhhhhhhhhhhhhhhhhhddddddddddddddffffdddSdddddccccccch [h] c (ddhd) ddc c ddc cddd d d dd d d d
 * 
 * @version $Revision: 1.7.2.6.2.11 $ $Date: 2005/04/11 10:05:54 $
 */
public class CharInfo extends L2GameServerPacket
{
	private static final Logger	_log			= Logger.getLogger(CharInfo.class.getName());
	private static final String	_S__03_CHARINFO	= "[S] 31 CharInfo";
	private final L2PcInstance	_activeChar;
	private final Inventory		_inv;
	private final int			_x, _y, _z, _heading;
	private final int			_mAtkSpd, _pAtkSpd;
	private final int			_runSpd, _walkSpd;
	private final byte			_displayAcc;
	private final byte			_displayCloak;
	private final float			_moveMultiplier, _attackSpeedMultiplier;
	private final boolean		_forceNoName;
	
	/**
	 * @param _characters
	 */
	public CharInfo(L2PcInstance cha)
	{
		_activeChar = cha;
		_invisible = cha.isInvisible();
		_inv = cha.getInventory();
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_heading = cha.getHeading();
		_mAtkSpd = cha.getMAtkSpd(null);
		_pAtkSpd = cha.getPAtkSpd(null);
		_moveMultiplier = cha.getMovementSpeedMultiplier();
		_attackSpeedMultiplier = cha.getAttackSpeedMultiplier();
		_runSpd = (int) (cha.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) (cha.getWalkSpeed() / _moveMultiplier);
		_displayAcc = cha.getAccDisplay();
		_displayCloak = cha.getCloakDisplay();
		_forceNoName = false;
	}
	
	public CharInfo(L2PcInstance cha, boolean noname)
	{
		_activeChar = cha;
		_invisible = cha.isInvisible();
		_inv = cha.getInventory();
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_heading = cha.getHeading();
		_mAtkSpd = cha.getMAtkSpd(null);
		_pAtkSpd = cha.getPAtkSpd(null);
		_moveMultiplier = cha.getMovementSpeedMultiplier();
		_attackSpeedMultiplier = cha.getAttackSpeedMultiplier();
		_runSpd = (int) (cha.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) (cha.getWalkSpeed() / _moveMultiplier);
		_displayAcc = cha.getAccDisplay();
		_displayCloak = cha.getCloakDisplay();
		_forceNoName = noname;
	}
	
	public L2PcInstance getCharInfoActiveChar()
	{
		return _activeChar;
	}
	
	@Override
	protected final void writeImpl()
	{
		if (_activeChar == null)
			return;
		/*
		 * boolean gmSeeInvis = false;
		 * if (_invisible)
		 * {
		 * L2PcInstance watcher = getClient().getActiveChar();
		 * if (watcher != null)
		 * {
		 * if (watcher.isGM())
		 * {
		 * if (_isGM)
		 * {
		 * if (_activeChar.getAccessLevel().getLevel() > watcher.getAccessLevel().getLevel())
		 * return;
		 * }
		 * gmSeeInvis = true;
		 * }
		 * else if (!_activeChar.inObserverMode() && !_isGM && watcher.canSeeInvisiblePeople())
		 * gmSeeInvis = true;
		 * else return;
		 * }
		 * else return;
		 * }
		 */
		if (_activeChar.getPoly().isMorphed())
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(_activeChar.getPoly().getPolyId());
			if (template != null)
			{
				writeC(0x0c);
				writeD(_activeChar.getObjectId());
				writeD(_activeChar.getPoly().getPolyId() + 1000000); // npctype id
				writeD(_activeChar.getKarma() > 0 ? 1 : 0);
				writeD(_x);
				writeD(_y);
				writeD(_z);
				writeD(_heading);
				writeD(0x00);
				writeD(_mAtkSpd);
				writeD(_pAtkSpd);
				writeD(_runSpd); // TODO: the order of the speeds should be confirmed
				writeD(_walkSpd);
				writeD(_runSpd); // swim run speed
				writeD(_walkSpd); // swim walk speed
				writeD(_runSpd); // fly run speed
				writeD(_walkSpd); // fly walk speed
				writeD(_runSpd); // fly run speed ?
				writeD(_walkSpd); // fly walk speed ?
				writeF(_moveMultiplier);
				writeF(_attackSpeedMultiplier);
				writeF(template.getCollisionRadius());
				writeF(template.getCollisionHeight());
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND)); // right hand weapon
				writeD(0);
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND)); // left hand weapon
				writeC(_activeChar.isDisguised() ? 0 : 1); // name above char 1=true ... ??
				writeC(_activeChar.isRunning() ? 1 : 0);
				writeC(_activeChar.isInCombat() ? 1 : 0);
				writeC(_activeChar.isAlikeDead() ? 1 : 0);
				writeC(0);
				/*
				 * if (_invisible)
				 * {
				 * writeC(0);
				 * }
				 * else
				 * {
				 * writeC(1); // invisible ?? 0=false 1=true 2=summoned (only works if model has a summon animation)
				 * }
				 */
				writeS(_forceNoName ? "" : _activeChar.getAppearance().getVisibleName());
				if (_invisible)
				{
					writeS(_forceNoName ? "" : "Invisible");
				}
				else
				{
					writeS(_forceNoName ? "" :_activeChar.getAppearance().getVisibleTitle());
				}
				writeD(0);
				writeD(0);
				writeD(0000); // hmm karma ??
				if (_invisible)
				{
					writeD((_activeChar.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()));
				}
				else
				{
					writeD(_activeChar.getAbnormalEffect()); // C2
				}
				writeD(_forceNoName ? 0 : _activeChar.getClanId()); // clan id
				writeD(_forceNoName ? 0 : _activeChar.getClanCrestId()); // crest id
				writeD(0); // C2
				writeD(0); // C2
				writeC(0); // C2
				writeC(0x00); // C3 team circle 1-blue, 2-red
				writeF(template.getCollisionRadius());
				writeF(template.getCollisionHeight());
				writeD(0x00); // C4
				writeD(0x00); // C6
				writeD(0x00);
				writeD(0x00);
				writeC(0x01);
				writeC(0x01);
				writeD(0x00);
			}
			else
			{
				_log.warning("Character " + _activeChar.getName() + " (" + _activeChar.getObjectId() + ") morphed in a Npc (" + _activeChar.getPoly().getPolyId() + ") w/o template.");
			}
		}
		else
		{
			final boolean olympiad = (_activeChar.isInOlympiadMode() || _activeChar.isDisguised() || (_activeChar._inEventDM && (DM._started || NewDM._started)));
			writeC(0x31);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(0x00);
			writeD(_activeChar.getObjectId());
			//writeS(_forceNoName ? "" : _activeChar.getAppearance().getVisibleName());

			writeS(_forceNoName ? "" : getClient() == null? _activeChar.getAppearance().getVisibleName() : getClient().getActiveChar().isGM()?_activeChar.getName():_activeChar.getAppearance().getVisibleName());
			writeD(_activeChar.getRace().getRealOrdinal());
			if (olympiad)
				writeD(Rnd.get(1));
			else
				writeD(_activeChar.getAppearance().getSex() ? 1 : 0);
			if (_activeChar.getRace().ordinal() == 6)
				writeD(0x0c); // bishop
			else if (_activeChar.getRace().ordinal() == 7)
				writeD(0x34); // warcryer
			else if (_activeChar.getRace().ordinal() == 3)
				writeD(0x2e); // destroyer
			else if (_activeChar.getRace().ordinal() == 0)
				writeD(0x03); // gladiator
			else if (_activeChar.getClassIndex() == 0)
				writeD(_activeChar.getClassId().getId());
			else
				writeD(_activeChar.getBaseClassId());
			int hair1item, hair2item, hair1aug, hair2aug, backitem, backaug;
			if (olympiad && !_activeChar.isDisguised())
			{
				hair1item = 0;
				hair1aug = 0;
				hair2item = 0;
				hair2aug = 0;
			}
			else
			{
				switch (_displayAcc)
				{
					case 1: // display hair
					{
						hair1item = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR);
						hair1aug = _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR);
						hair2item = 0;
						hair2aug = 0;
						break;
					}
					case 2: // display face
					{
						hair1item = 0;
						hair1aug = 0;
						hair2item = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR2);
						hair2aug = _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR2);
						break;
					}
					case 3: // display none
					{
						hair1item = 0;
						hair1aug = 0;
						hair2item = 0;
						hair2aug = 0;
						break;
					}
					default: // display all
					{
						hair1item = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR);
						hair1aug = _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR);
						hair2item = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR2);
						hair2aug = _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR2);
					}
				}
			}
			switch (_displayCloak)
			{
				case 1: 
				{
					backitem = getClient().getActiveChar().getVarB("hidecloaks")?0:_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK);
					backaug = getClient().getActiveChar().getVarB("hidecloaks")?0:_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_BACK);
					break;
				}
				case 2: // display none
				{
					backitem = 0;
					backaug = 0;
					break;
				}
				default: // display all
				{
					backitem = getClient().getActiveChar().getVarB("hidecloaks")?0:_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK);
					backaug = getClient().getActiveChar().getVarB("hidecloaks")?0:_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_BACK);
				}
			}
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_UNDER));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			// writeD(_activeChar.isInOlympiadMode() || !_activeChar.getVarB("showVisualChange") ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND) : _inv.getPaperdollItemVisualDisplayId(Inventory.PAPERDOLL_RHAND));
			// writeD(_activeChar.isInOlympiadMode() || !_activeChar.getVarB("showVisualChange") ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND) : _inv.getPaperdollItemVisualDisplayId(Inventory.PAPERDOLL_LHAND));
			if (_activeChar._inEventHG && NewHuntingGrounds._started)
			{
				if (_activeChar._teamNameHG.equalsIgnoreCase("Red"))
				{
					writeD(5611);
					writeD(5611);
				}
				if (_activeChar._teamNameHG.equalsIgnoreCase("Green"))
				{
					writeD(6594);
					writeD(6594);
				}
			}
			else
			{
				int rhand = _inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND);
				int lhand = _inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND);
				
				if (_activeChar.getRace() != Race.Kamael && _activeChar.getRace() != Race.DarkElf)
				{
					rhand = Utils.fixIdForNonKamaelDelf(_activeChar, rhand);
				}
				
				writeD(rhand);
				writeD(lhand);
//				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
//				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			}
			if (_activeChar._inEventHG && NewHuntingGrounds._started)
			{
				if (_activeChar._teamNameHG.equalsIgnoreCase("Red"))
				{
					writeD(32721);
					writeD(32721);
					writeD(32721);
					writeD(32721);
					writeD(0);
				}
				if (_activeChar._teamNameHG.equalsIgnoreCase("Green"))
				{
					writeD(42721);
					writeD(42721);
					writeD(42721);
					writeD(42721);
					writeD(0);
				}
			}
			else
			{
				if(getClient().getActiveChar().getVarB("hidedress"))
				{
					writeD(_inv.getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_GLOVES));
					writeD(_inv.getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_CHEST));
					writeD(_inv.getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_LEGS));
					writeD(_inv.getPaperdollItemIdNoDressed(Inventory.PAPERDOLL_FEET));
					writeD(backitem);
				}
				else
				{
					writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
					writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
					writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
					writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
					writeD(backitem);
				}
			}
			// writeD(_activeChar.isInOlympiadMode() || !_activeChar.getVarB("showVisualChange") ? _inv.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND) : _inv.getPaperdollItemVisualDisplayId((Inventory.PAPERDOLL_LRHAND)));
			if (_activeChar._inEventHG && NewHuntingGrounds._started)
			{
				if (_activeChar._teamNameHG.equalsIgnoreCase("Red"))
				{
					writeD(5611);
				}
				if (_activeChar._teamNameHG.equalsIgnoreCase("Green"))
				{
					writeD(6594);
				}
			}
			else
			{
				int lrhand = _inv.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND);
				
				if (_activeChar.getRace() != Race.Kamael && _activeChar.getRace() != Race.DarkElf)
				{
					lrhand = Utils.fixIdForNonKamaelDelf(_activeChar, lrhand);
				}
				
				writeD(lrhand);
			}
			if (_activeChar._inEventHG && NewHuntingGrounds._started)
			{
				if (_activeChar._teamNameHG.equalsIgnoreCase("Red"))
				{
					writeD(32729);
					writeD(0);
				}
				if (_activeChar._teamNameHG.equalsIgnoreCase("Green"))
				{
					writeD(42722);
					writeD(0);
				}
			}
			else
			{
				writeD(hair1item);
				writeD(hair2item);
			}
			// T1 new d's
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RBRACELET));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LBRACELET));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO1));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO2));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO3));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO4));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO5));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO6));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_BELT));
			// end of t1 new d's
			// c6 new h's
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_UNDER));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_HEAD));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LHAND));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_GLOVES));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_CHEST));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LEGS));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_FEET));
			writeD(backaug);
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LRHAND));
			writeD(hair1aug);
			writeD(hair2aug);
			// T1 new h's
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RBRACELET));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LBRACELET));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO1));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO2));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO3));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO4));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO5));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO6));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_BELT));
			writeD(0x00);
			writeD(0x00);
			// end of t1 new h's
			writeD(_activeChar.getPvpFlag());
			writeD(_activeChar.getKarma());
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			writeD(_activeChar.getPvpFlag());
			writeD(_activeChar.getKarma());
			writeD(_runSpd); // TODO: the order of the speeds should be confirmed
			writeD(_walkSpd);
			writeD(_runSpd); // swim run speed
			writeD(_walkSpd); // swim walk speed
			writeD(_runSpd); // fly run speed
			writeD(_walkSpd); // fly walk speed
			writeD(_runSpd); // fly run speed ?
			writeD(_walkSpd); // fly walk speed ?
			writeF(_moveMultiplier); // _activeChar.getProperMultiplier()
			writeF(_attackSpeedMultiplier); // _activeChar.getAttackSpeedMultiplier()
			if (_activeChar.getMountType() != 0)
			{
				writeF(NpcTable.getInstance().getTemplate(_activeChar.getMountNpcId()).getCollisionRadius());
				writeF(NpcTable.getInstance().getTemplate(_activeChar.getMountNpcId()).getCollisionHeight());
			}
			else if (_activeChar.isTransformed())
			{
				writeF(_activeChar.getTransformation().getCollisionRadius());
				writeF(_activeChar.getTransformation().getCollisionHeight());
			}
			else
			{
				writeF(_activeChar.getCollisionRadius());
				writeF(_activeChar.getCollisionHeight());
			}
			if (olympiad)
			{
				writeD(1);
				writeD(1);
				writeD(1);
			}
			else
			{
				writeD(_activeChar.getAppearance().getHairStyle());
				writeD(_activeChar.getAppearance().getHairColor());
				writeD(_activeChar.getAppearance().getFace());
			}
			if (_invisible)
			{
				writeS(_forceNoName ? "" : "Invisible");
			}
			else
			{
				writeS(_forceNoName ? "" :_activeChar.getAppearance().getVisibleTitle());
			}
			final boolean cursed = _activeChar.isCursedWeaponEquipped();
			if (cursed || olympiad || _forceNoName)
			{
				writeD(0);
				writeD(0);
				writeD(0);
				writeD(0);
			}
			else
			{
				writeD(_activeChar.getClanId());
				writeD(_activeChar.getClanCrestId());
				writeD(_activeChar.getAllyId());
				writeD(_activeChar.getAllyCrestId());
			}
			// In UserInfo leader rights and siege flags, but here found nothing?? // Therefore RelationChanged packet with that info is required
			writeD(0);
			writeC(_activeChar.isSitting() ? 0 : 1); // standing = 1 sitting = 0
			writeC(_activeChar.isRunning() ? 1 : 0); // running = 1 walking = 0
			writeC(_activeChar.isInCombat() ? 1 : 0);
			writeC(_activeChar.isAlikeDead() ? 1 : 0);
			writeC(0); // invisible = 1, here we always put 0 because that's how we handle it

			writeC(_activeChar.getMountType()); // 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
			writeC(_activeChar.getPrivateStoreType()); // 1 - sellshop
			writeH(_activeChar.getCubics().size());
			for (int id : _activeChar.getCubics().keySet())
				writeH(id);
			writeC(0x00); // find party members
			if (_invisible)
			{
				writeD((_activeChar.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()));
			}
			else
			{
				writeD(_activeChar.getAbnormalEffect());
			}
			writeC(_activeChar.isFlying() ? 2 : 0);
			writeH(olympiad ? 0 : _activeChar.getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
			writeD(_activeChar.getMountNpcId() + 1000000);
			writeD(_activeChar.getClassId().getId());
			writeD(0x00); // ?
			writeC(olympiad && !_activeChar.isDisguised() || _activeChar.isMounted() ? 0 : /* _activeChar.isDisguised() ? Math.max(_activeChar.getEnchantEffect() + Rnd.get(-5, 1), 0) : */ getClient().getActiveChar().getVarB("hideEnchAnime")?0:_activeChar.getEnchantEffect());
			if(_activeChar.getTeam()==1)
				writeC(0x01); //team circle around feet 1= Blue, 2 = red
			else if(_activeChar.getTeam()==2)
				writeC(0x02); //team circle around feet 1= Blue, 2 = red
			else
				writeC(0x00); //team circle around feet 1= Blue, 2 = red
			writeD(cursed || olympiad ? 0 : _activeChar.getClanCrestLargeId());
			writeC(cursed || olympiad ? 0 : _activeChar.isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
			writeC(((_activeChar.isHero() || (_activeChar.isGM() && Config.GM_HERO_AURA)) && !olympiad) ? 1 : 0); // Hero Aura
			writeC(_activeChar.isFishing() ? 1 : 0); // 0x01: Fishing Mode (Cant be undone by setting back to 0)
			writeD(_activeChar.getFishx());
			writeD(_activeChar.getFishy());
			writeD(_activeChar.getFishz());
			writeD(_activeChar.getAppearance().getNameColor());
			writeD(_heading);
			writeD(cursed || olympiad ? 0 : _activeChar.getPledgeClass());
			writeD(cursed || olympiad ? 0 : _activeChar.getPledgeType());
			writeD(_activeChar.getAppearance().getTitleColor());
			if (cursed)
				writeD(CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquippedId()));
			else
				writeD(0x00);
			if (_activeChar.getClanId() > 0 && !olympiad)
				writeD(_activeChar.getClan().getReputationScore());
			else
				writeD(0x00);
			// T1
			writeD(_activeChar.isTransformed() ? _activeChar.getTransformationId() : 0);
			writeD(!olympiad ? _activeChar.getAgathionId() : 0);
			// T2
			writeD(0x01);
			// T2.3
			writeD(_activeChar.getSpecialEffect());
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__03_CHARINFO;
	}
}
