package net.sf.l2j.gameserver.network.serverpackets;

import Alpha.autopots.Utils;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.Elementals;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewFOS;
import net.sf.l2j.gameserver.model.events.newEvents.NewHuntingGrounds;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/**
 * 0000: 04 03 15 00 00 77 ff 00 00 80 f1 ff ff 00 00 00    .....w..........
 * 0010: 00 2a 89 00 4c 43 00 61 00 6c 00 61 00 64 00 6f    .*..LC.a.l.a.d.o
 * 0020: 00 6e 00 00 00 01 00 00 00 00 00 00 00 19 00 00    .n..............
 * 0030: 00 0d 00 00 00 ee 81 02 00 15 00 00 00 18 00 00    ................
 * 0040: 00 19 00 00 00 25 00 00 00 17 00 00 00 28 00 00    .....%.......(..
 * 0050: 00 14 01 00 00 14 01 00 00 02 01 00 00 02 01 00    ................
 * 0060: 00 fa 09 00 00 81 06 00 00 26 34 00 00 2e 00 00    .........&4.....
 * 0070: 00 00 00 00 00 db 9f a1 41 93 26 64 41 de c8 31    ........A.&dA..1
 * 0080: 41 ca 73 c0 41 d5 22 d0 41 83 bd 41 41 81 56 10    A.s.A.".A..AA.V.
 * 0090: 41 00 00 00 00 27 7d 30 41 69 aa e0 40 b4 fb d3    A....'}0Ai..@...
 * 00a0: 41 91 f9 63 41 00 00 00 00 81 56 10 41 00 00 00    A..cA.....V.A...
 * 00b0: 00 71 00 00 00 71 00 00 00 76 00 00 00 74 00 00    .q...q...v...t..
 * 00c0: 00 74 00 00 00 2a 00 00 00 e8 02 00 00 00 00 00    .t...*..........
 * 00d0: 00 5f 04 00 00 ac 01 00 00 cf 01 00 00 62 04 00    ._...........b..
 * 00e0: 00 00 00 00 00 e8 02 00 00 0b 00 00 00 52 01 00    .............R..
 * 00f0: 00 4d 00 00 00 2a 00 00 00 2f 00 00 00 29 00 00    .M...*.../...)..
 * 0100: 00 12 00 00 00 82 01 00 00 52 01 00 00 53 00 00    .........R...S..
 * 0110: 00 00 00 00 00 00 00 00 00 7a 00 00 00 55 00 00    .........z...U..
 * 0120: 00 32 00 00 00 32 00 00 00 00 00 00 00 00 00 00    .2...2..........
 * 0130: 00 00 00 00 00 00 00 00 00 a4 70 3d 0a d7 a3 f0    ..........p=....
 * 0140: 3f 64 5d dc 46 03 78 f3 3f 00 00 00 00 00 00 1e    ?d].F.x.?.......
 * 0150: 40 00 00 00 00 00 00 38 40 02 00 00 00 01 00 00    @......8@.......
 * 0160: 00 00 00 00 00 00 00 00 00 00 00 c1 0c 00 00 01    ................
 * 0170: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00    ................
 * 0180: 00 00 00 00                                        ....
 *
 *
 * dddddSdddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddffffddddSdddcccdd (h)
 * dddddSddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd  ffffddddSdddddcccddh (h) c dc hhdh
 * dddddSdddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddffffddddSdddddcccddh (h) c dc hhdh ddddc c dcc cddd d (from 654)
 * but it actually reads
 * dddddSdddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddffffddddSdddddcccddh (h) c dc *dddddddd* hhdh ddddc dcc cddd d
 * 																					*...*: here i am not sure at least it looks like it reads that much data (32 bytes), not sure about the format inside because it is not read thanks to the ususal parsing function
 *
 * dddddSddddQddddddddddddddddddddddddddddddddddddddddddddddddhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhddddddddddddddddddddffffddddSdddddcccddh [h] c dc d hhdh ddddc c dcc cddd d c dd d d

 * @version $Revision: 1.14.2.4.2.12 $ $Date: 2005/04/11 10:05:55 $
 */
public final class UserInfo extends L2GameServerPacket
{
private static final String _S__04_USERINFO = "[S] 32 UserInfo";
private final L2PcInstance _activeChar;
private final int _runSpd, _walkSpd;
private int _relation;
private final float _moveMultiplier;
private final byte _displayAcc;
private final byte _displayCloak;
private final boolean _ignoreSize;

/**
 * @param _characters
 */
public UserInfo(L2PcInstance character, boolean ignoreSize)
{
	_activeChar = character;
	
	final boolean fos = _activeChar._inEventFOS && (FOS._started || NewFOS._started);
	final boolean fosLeader = _activeChar.isAFOSLeader();
	
	_moveMultiplier = _activeChar.getMovementSpeedMultiplier();
	_runSpd = (int) (_activeChar.getRunSpeed() / _moveMultiplier);
	_walkSpd = (int) (_activeChar.getWalkSpeed() / _moveMultiplier);
	_relation = fos ? fosLeader ? 0x40 : 0 : _activeChar.isClanLeader() ? 0x40 : 0;
	if (_activeChar.getSiegeState() == 1) _relation |= 0x180;
	if (_activeChar.getSiegeState() == 2) _relation |= 0x80;
	_displayAcc = _activeChar.getAccDisplay();
	_displayCloak = _activeChar.getCloakDisplay();
	_ignoreSize = ignoreSize;
}

public UserInfo(L2PcInstance character)
{

	this(character, false);
}

@Override
protected final void writeImpl()
{

	//Announcements.getInstance().announceToAll("-User info called");
	
	
	String name = _activeChar.getName();
	
	if (_activeChar.isDisguised() && !(_activeChar.isHealerClass() || _activeChar.isMageClass()))
		name = "Unknown Warrior";
	else if (_activeChar.isDisguised() && (_activeChar.isHealerClass() || _activeChar.isMageClass()))
		name = "Unknown Wizzard";
	
	writeC(0x32);
	
	writeD(_activeChar.getX());
	writeD(_activeChar.getY());
	writeD(_activeChar.getZ());
	writeD(_activeChar.isInAirShip() ? _activeChar.getAirShip().getObjectId() : 0x00); // heading from CT2.3 no longer used inside userinfo, here is now vehicle id (boat,airship)
	writeD(_activeChar.getObjectId());
	writeS(name);
	writeD(_activeChar.getRace().getRealOrdinal());
	writeD(_activeChar.getAppearance().getSex()? 1 : 0);
	
	if (_activeChar.getRace().ordinal() == 6)
		writeD(0x10); //bishop
	else if (_activeChar.getRace().ordinal() == 7)
		writeD(0x34); //warcryer
	else if (_activeChar.getRace().ordinal() == 3)
		writeD(0x2e); //destroyer
	else if (_activeChar.getRace().ordinal() == 0)
		writeD(0x03); //gladiator
	else if (_activeChar.getClassIndex() == 0)
		writeD(_activeChar.getClassId().getId());
	else
		writeD(_activeChar.getBaseClassId());
	
	writeD(_activeChar.getLevel());
	writeQ(_activeChar.getExp());
	writeD(_activeChar.getSTR());
	writeD(_activeChar.getDEX());
	writeD(_activeChar.getCON());
	writeD(_activeChar.getINT());
	writeD(_activeChar.getWIT());
	writeD(_activeChar.getMEN());
	writeD(_activeChar.getMaxHp());
	writeD((int) _activeChar.getCurrentHp());
	writeD(_activeChar.getMaxMp());
	writeD((int) _activeChar.getCurrentMp());
	writeD(_activeChar.getSp());
	writeD(0);
	writeD(1);
	
	writeD(_activeChar.getActiveWeaponItem() != null ? 40 : 20); // 20 no weapon, 40 weapon equipped
	
	int hair1item, hair2item, hair1obj, hair2obj, hair1aug, hair2aug, backobj, backitem, backaug;
	
	switch (_displayAcc)
	{
	case 1: //display hair
	{
		hair1obj = _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR);
		hair1item = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR);
		hair1aug = _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR);
		hair2obj = 0;
		hair2item = 0;
		hair2aug = 0;
		break;
	}
	case 2: //display face
	{
		hair1obj = 0;
		hair1item = 0;
		hair1aug = 0;
		hair2obj = _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR2);
		hair2item = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR2);
		hair2aug = _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR2);
		break;
	}
	case 3: //display none
	{
		hair1obj = 0;
		hair1item = 0;
		hair1aug = 0;
		hair2obj = 0;
		hair2item = 0;
		hair2aug = 0;
		break;
	}
	default: //display all
	{
		hair1obj = _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR);
		hair1item = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR);
		hair1aug = _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR);
		hair2obj = _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR2);
		hair2item = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR2);
		hair2aug = _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR2);
	}
	}
	switch (_displayCloak)
	{
	case 1: //display hair
	{
		backobj = _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BACK);
		backitem = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK);
		backaug = _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_BACK);
		break;
	}
	case 2: //display none
	{
		backobj = 0;
		backitem = 0;
		backaug = 0;
		break;
	}
	default: //display all
	{
		backobj = _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BACK);
		backitem = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK);
		backaug = _activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_BACK);
	}
	}
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_UNDER));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
	//writeD(_activeChar.isInOlympiadMode() || !_activeChar.getVarB("showVisualChange") ? _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND) : _activeChar.getInventory().getPaperdollItemVisualDisplayId(Inventory.PAPERDOLL_RHAND));
	//writeD(_activeChar.isInOlympiadMode() || !_activeChar.getVarB("showVisualChange") ? _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND) : _activeChar.getInventory().getPaperdollItemVisualDisplayId(Inventory.PAPERDOLL_LHAND));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
	writeD(backobj);
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
	writeD(hair1obj);
	writeD(hair2obj);
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RBRACELET));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LBRACELET));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO1));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO2));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO3));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO4));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO5));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO6));
	writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BELT)); // CT2.3
	
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_UNDER));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_REAR));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_NECK));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
	if(_activeChar._inEventHG && NewHuntingGrounds._started)
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
		int rhand = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND);
		int lhand = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LHAND);
		if (_activeChar.getRace() != Race.Kamael && _activeChar.getRace() != Race.DarkElf)
		{
			rhand = Utils.fixIdForNonKamaelDelf(_activeChar, rhand);
		}
		writeD(rhand);
		writeD(lhand);
//		writeD(rhand);
//		writeD(lhand);
	}
	if(_activeChar._inEventHG && NewHuntingGrounds._started)
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
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FEET));
		writeD(backitem);
	}

	if(_activeChar._inEventHG && NewHuntingGrounds._started)
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
		int lrhand = _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LRHAND);
		
		if (_activeChar.getRace() != Race.Kamael && _activeChar.getRace() != Race.DarkElf)
		{
			lrhand = Utils.fixIdForNonKamaelDelf(_activeChar, lrhand);
		}
		writeD(lrhand);
	}
	if(_activeChar._inEventHG && NewHuntingGrounds._started)
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
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RBRACELET));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LBRACELET));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_DECO1));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_DECO2));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_DECO3));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_DECO4));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_DECO5));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_DECO6));
	writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BELT)); // CT2.3
	
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_UNDER));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_REAR));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LEAR));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_NECK));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RFINGER));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LFINGER));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_HEAD));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LHAND));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_GLOVES));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_CHEST));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LEGS));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_FEET));
	writeD(backaug);
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LRHAND));
	writeD(hair1aug);
	writeD(hair2aug);
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RBRACELET));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LBRACELET));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO1));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO2));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO3));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO4));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO5));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO6));
	writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_BELT)); // CT2.3
	writeD(_activeChar.getInventory().getMaxTalismanCount()); // CT2.3
	writeD(0x01); // Cloak slot disabled
	writeD(_activeChar.getPAtk(null));
	writeD(_activeChar.getPAtkSpd(null));
	writeD(_activeChar.getPDef(null));
	writeD(_activeChar.getEvasionRate(null));
	writeD(_activeChar.getAccuracy(null));
	writeD(_activeChar.getCriticalHit(null, null));
	writeD(_activeChar.getMAtk(null, null));
	
	writeD(_activeChar.getMAtkSpd(null));
	writeD(_activeChar.getPAtkSpd(null));
	
	writeD(_activeChar.getMDef(null, null));
	
	writeD(_activeChar.getPvpFlag()); // 0-non-pvp  1-pvp = violett name
	writeD(_activeChar.getKarma());
	
	writeD(_runSpd);
	writeD(_walkSpd);
	writeD(_runSpd); // swim run speed
	writeD(_walkSpd); // swim walk speed
	writeD(0);
	writeD(0);
	writeD(_activeChar.isFlying() ? _runSpd : 0); // fly speed
	writeD(_activeChar.isFlying() ? _walkSpd : 0); // fly speed
	writeF(_moveMultiplier);
	writeF(_activeChar.getAttackSpeedMultiplier());
	
	L2Summon pet = _activeChar.getPet();
	
	if (_activeChar.getMountType() != 0 && pet != null)
	{
		writeF(pet.getCollisionRadius());
		writeF(pet.getCollisionHeight());
	}
	else if (_activeChar.isTransformed())
	{
		writeF(_activeChar.getTransformation().getCollisionRadius());
		writeF(_activeChar.getTransformation().getCollisionHeight());
	}
	else
	{
		writeF(_activeChar.getCollisionRadius() * (_ignoreSize ? 1 : _activeChar.getWeaponSizeMultiplier()));
		writeF(_activeChar.getCollisionHeight());
	}
	
	writeD(_activeChar.getAppearance().getHairStyle());
	writeD(_activeChar.getAppearance().getHairColor());
	writeD(_activeChar.getAppearance().getFace());
	writeD(_activeChar.isGM() ? 1 : _activeChar.getTurnedGMOff() > 0 ? 1 : 0); // builder level
	
	String title = _activeChar.getAppearance().getVisibleTitle();
	
	if (_activeChar.calcStat(Stats.PHAZE_MOVEMENT, 0, null, null) > 0)
	{
		if (_activeChar.isInvisible())
			title = "Invisible Shifting";
		else
			title = "Phaze Shifting";
	}
	else if (_activeChar.isInvisible())
		title = "Invisible";
	else if (_activeChar.getPoly().isMorphed())
	{
		L2NpcTemplate polyObj = NpcTable.getInstance().getTemplate(_activeChar.getPoly().getPolyId());
		if(polyObj != null)
			title += " - " + polyObj.name;
	}
	
	writeS(title);
	
	writeD(_activeChar.getClanId());
	writeD(_activeChar.getClanCrestId());
	writeD(_activeChar.getAllyId());
	writeD(_activeChar.getAllyCrestId()); // ally crest id
	// 0x40 leader rights
	// siege flags: attacker - 0x180 sword over name, defender - 0x80 shield, 0xC0 crown (|leader), 0x1C0 flag (|leader)
	writeD(_relation);
	writeC(_activeChar.getMountType()); // mount type
	writeC(_activeChar.getPrivateStoreType());
	writeC(_activeChar.hasDwarvenCraft() ? 1 : 0);
	writeD(_activeChar.getPkKills());
	writeD(_activeChar.getPvpKills());
	
	writeH(_activeChar.getCubics().size());
	for (int id : _activeChar.getCubics().keySet())
		writeH(id);
	
	writeC(0x00); //1-find party members
	
	writeD(_activeChar.getAbnormalEffect());
	writeC(_activeChar.isFlyingMounted() ? 2 : 0);
	
	writeD(_activeChar.getClanPrivileges());
	
	writeH(_activeChar.getRecomLeft()); //c2  recommendations remaining
	writeH(_activeChar.getRecomHave()); //c2  recommendations received
	writeD(_activeChar.getMountNpcId() > 0 ? _activeChar.getMountNpcId() + 1000000 : 0);
	writeH(_activeChar.getInventoryLimit());
	
	writeD(_activeChar.getClassId().getId());
	writeD(0x00); // special effects? circles around player...
	writeD(_activeChar.getMaxCp());
	writeD((int) _activeChar.getCurrentCp());
	writeC(_activeChar.isMounted() ? 0 : _activeChar.getEnchantEffect());
	
	if(_activeChar.getTeam()==1)
		writeC(0x01); //team circle around feet 1= Blue, 2 = red
	else if(_activeChar.getTeam()==2)
		writeC(0x02); //team circle around feet 1= Blue, 2 = red
	else
		writeC(0x00); //team circle around feet 1= Blue, 2 = red

	writeD(_activeChar.getClanCrestLargeId());
	writeC(_activeChar.isNoble() ? 1 : 0); //0x01: symbol on char menu ctrl+I
	writeC(_activeChar.isHero() || (_activeChar.isGM() && Config.GM_HERO_AURA) ? 1 : 0); //0x01: Hero Aura
	
	writeC(_activeChar.isFishing() ? 1 : 0); //Fishing Mode
	writeD(_activeChar.getFishx()); //fishing x
	writeD(_activeChar.getFishy()); //fishing y
	writeD(_activeChar.getFishz()); //fishing z
	writeD(_activeChar.getAppearance().getNameColor());
	
	//new c5
	writeC(_activeChar.isRunning() ? 0x01 : 0x00); //changes the Speed display on Status Window
	
	writeD(_activeChar.getPledgeClass()); //changes the text above CP on Status Window
	writeD(_activeChar.getPledgeType());
	
	writeD(_activeChar.getAppearance().getTitleColor());
	
	if (_activeChar.isCursedWeaponEquipped())
		writeD(CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquippedId()));
	else
		writeD(0x00);
	
	// T1 Starts
	writeD(_activeChar.isTransformed() ? _activeChar.getTransformationId() : 0);
	
	byte attackAttribute = _activeChar.getAttackElement();
	writeH(attackAttribute);
	writeH(_activeChar.getAttackElementValue(attackAttribute));
	writeH(_activeChar.getDefenseElementValue(Elementals.FIRE));
	writeH(_activeChar.getDefenseElementValue(Elementals.WATER));
	writeH(_activeChar.getDefenseElementValue(Elementals.WIND));
	writeH(_activeChar.getDefenseElementValue(Elementals.EARTH));
	writeH(_activeChar.getDefenseElementValue(Elementals.HOLY));
	writeH(_activeChar.getDefenseElementValue(Elementals.DARK));
	
	writeD(_activeChar.getAgathionId());
	
	// T2 Starts
	writeD(_activeChar.getFame());  // Fame
	writeD(0x01); // Unknown
	writeD(_activeChar.getVitalityPoints());  // Vitality Points
	writeD(_activeChar.getSpecialEffect());
	writeD(0x00); // CT2.3
	writeD(0x00); // CT2.3
	
}
/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
 */
@Override
public String getType()
{
	return _S__04_USERINFO;
}
}
