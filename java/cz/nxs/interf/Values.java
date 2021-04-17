package cz.nxs.interf;

import cz.nxs.interf.delegate.ItemData;
import cz.nxs.l2j.CallBack;
import cz.nxs.l2j.IValues;
import cz.nxs.l2j.WeaponType;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;

public class Values implements IValues
{
	public void load()
	{
		CallBack.getInstance().setValues(this);
	}

	@Override
	public int PAPERDOLL_UNDER() { return Inventory.PAPERDOLL_UNDER; }
	@Override
	public int PAPERDOLL_HEAD() { return Inventory.PAPERDOLL_HEAD; }
	@Override
	public int PAPERDOLL_HAIR() { return Inventory.PAPERDOLL_HAIR; }
	@Override
	public int PAPERDOLL_HAIR2() { return Inventory.PAPERDOLL_HAIR2; }
	@Override
	public int PAPERDOLL_NECK() { return Inventory.PAPERDOLL_NECK; }
	@Override
	public int PAPERDOLL_RHAND() { return Inventory.PAPERDOLL_RHAND; }
	@Override
	public int PAPERDOLL_CHEST() { return Inventory.PAPERDOLL_CHEST; }
	@Override
	public int PAPERDOLL_LHAND() { return Inventory.PAPERDOLL_LHAND; }
	@Override
	public int PAPERDOLL_REAR() { return Inventory.PAPERDOLL_REAR; }
	@Override
	public int PAPERDOLL_LEAR() { return Inventory.PAPERDOLL_LEAR; }
	@Override
	public int PAPERDOLL_GLOVES() { return Inventory.PAPERDOLL_GLOVES; }
	@Override
	public int PAPERDOLL_LEGS() { return Inventory.PAPERDOLL_LEGS; }
	@Override
	public int PAPERDOLL_FEET() { return Inventory.PAPERDOLL_FEET; }
	@Override
	public int PAPERDOLL_RFINGER() { return Inventory.PAPERDOLL_RFINGER; }
	@Override
	public int PAPERDOLL_LFINGER() { return Inventory.PAPERDOLL_LFINGER; }
	@Override
	public int PAPERDOLL_LBRACELET() { return Inventory.PAPERDOLL_LBRACELET; }
	@Override
	public int PAPERDOLL_RBRACELET() { return Inventory.PAPERDOLL_RBRACELET; }
	@Override
	public int PAPERDOLL_DECO1() { return Inventory.PAPERDOLL_DECO1; }
	@Override
	public int PAPERDOLL_DECO2() { return Inventory.PAPERDOLL_DECO2; }
	@Override
	public int PAPERDOLL_DECO3() { return Inventory.PAPERDOLL_DECO3; }
	@Override
	public int PAPERDOLL_DECO4() { return Inventory.PAPERDOLL_DECO4; }
	@Override
	public int PAPERDOLL_DECO5() { return Inventory.PAPERDOLL_DECO5; }
	@Override
	public int PAPERDOLL_DECO6() { return Inventory.PAPERDOLL_DECO6; }
	@Override
	public int PAPERDOLL_CLOAK() { return Inventory.PAPERDOLL_BACK; }
	@Override
	public int PAPERDOLL_BELT() { return Inventory.PAPERDOLL_BELT; }
	@Override
	public int PAPERDOLL_TOTALSLOTS() { return Inventory.PAPERDOLL_TOTALSLOTS; }
	
	@Override
	public int SLOT_NONE() { return L2Item.SLOT_NONE; }
	@Override
	public int SLOT_UNDERWEAR() { return L2Item.SLOT_UNDERWEAR; }
	@Override
	public int SLOT_R_EAR() { return L2Item.SLOT_R_EAR; }
	@Override
	public int SLOT_L_EAR() { return L2Item.SLOT_L_EAR; }
	@Override
	public int SLOT_LR_EAR() { return L2Item.SLOT_LR_EAR; }
	@Override
	public int SLOT_NECK() { return L2Item.SLOT_NECK; }
	@Override
	public int SLOT_R_FINGER() { return L2Item.SLOT_R_FINGER; }
	@Override
	public int SLOT_L_FINGER() { return L2Item.SLOT_L_FINGER; }
	@Override
	public int SLOT_LR_FINGER() { return L2Item.SLOT_LR_FINGER; }
	@Override
	public int SLOT_HEAD() { return L2Item.SLOT_HEAD; }
	@Override
	public int SLOT_R_HAND() { return L2Item.SLOT_R_HAND; }
	@Override
	public int SLOT_L_HAND() { return L2Item.SLOT_L_HAND; }
	@Override
	public int SLOT_GLOVES() { return L2Item.SLOT_GLOVES; }
	@Override
	public int SLOT_CHEST() { return L2Item.SLOT_CHEST; }
	@Override
	public int SLOT_LEGS() { return L2Item.SLOT_LEGS; }
	@Override
	public int SLOT_FEET() { return L2Item.SLOT_FEET; }
	@Override
	public int SLOT_BACK() { return L2Item.SLOT_BACK; }
	@Override
	public int SLOT_LR_HAND() { return L2Item.SLOT_LR_HAND; }
	@Override
	public int SLOT_FULL_ARMOR() { return L2Item.SLOT_FULL_ARMOR; }
	@Override
	public int SLOT_HAIR() { return L2Item.SLOT_HAIR; }
	@Override
	public int SLOT_ALLDRESS() { return L2Item.SLOT_ALLDRESS; }
	@Override
	public int SLOT_HAIR2() { return L2Item.SLOT_HAIR2; }
	@Override
	public int SLOT_HAIRALL() { return L2Item.SLOT_HAIRALL; }
	@Override
	public int SLOT_R_BRACELET() { return L2Item.SLOT_R_BRACELET; }
	@Override
	public int SLOT_L_BRACELET() { return L2Item.SLOT_L_BRACELET; }
	@Override
	public int SLOT_DECO() { return L2Item.SLOT_DECO; }
	@Override
	public int SLOT_BELT() { return L2Item.SLOT_BELT; }
	@Override
	public int SLOT_WOLF() { return L2Item.SLOT_WOLF; }
	@Override
	public int SLOT_HATCHLING() { return L2Item.SLOT_HATCHLING; }
	@Override
	public int SLOT_STRIDER() { return L2Item.SLOT_STRIDER; }
	@Override
	public int SLOT_BABYPET() { return L2Item.SLOT_BABYPET; }
	@Override
	public int SLOT_GREATWOLF() { return L2Item.SLOT_GREATWOLF; }
	
	@Override
	public int CRYSTAL_NONE() { return L2Item.CRYSTAL_NONE; }
	@Override
	public int CRYSTAL_D() { return L2Item.CRYSTAL_D; }
	@Override
	public int CRYSTAL_C() { return L2Item.CRYSTAL_C; }
	@Override
	public int CRYSTAL_B() { return L2Item.CRYSTAL_B; }
	@Override
	public int CRYSTAL_A() { return L2Item.CRYSTAL_A; }
	@Override
	public int CRYSTAL_S() { return L2Item.CRYSTAL_S; }
	@Override
	public int CRYSTAL_S80() { return L2Item.CRYSTAL_S80; }
	@Override
	public int CRYSTAL_S84() { return L2Item.CRYSTAL_S84; }
	
	@Override
	public int TYPE_ITEM() { return L2ShortCut.TYPE_ITEM; }
	@Override
	public int TYPE_SKILL() { return L2ShortCut.TYPE_SKILL; }
	@Override
	public int TYPE_ACTION() { return L2ShortCut.TYPE_ACTION; }
	@Override
	public int TYPE_MACRO() { return L2ShortCut.TYPE_MACRO; }
	@Override
	public int TYPE_RECIPE() { return L2ShortCut.TYPE_RECIPE; }
	@Override
	public int TYPE_TPBOOKMARK() { return L2ShortCut.TYPE_TPBOOKMARK; }

	public cz.nxs.l2j.WeaponType getWeaponType(ItemData item)
	{
		L2WeaponType origType = ((L2Weapon) item.getTemplate()).getItemType();
		switch(origType)
		{
			case SWORD:
				return WeaponType.SWORD;
			case BLUNT:
				return WeaponType.BLUNT;
			case DAGGER:
				return WeaponType.DAGGER;
			case BOW:
				return WeaponType.BOW;
			case POLE:
				return WeaponType.POLE;
			case NONE:
				return WeaponType.NONE;
			case DUAL:
				return WeaponType.DUAL;
			case ETC:
				return WeaponType.ETC;
			case FIST:
				return WeaponType.FIST;
			case DUALFIST:
				return WeaponType.DUALFIST;
			case ROD:
				return WeaponType.FISHINGROD;
			case RAPIER:
				return WeaponType.RAPIER;
			case ANCIENT_SWORD:
				return WeaponType.ANCIENTSWORD;
			case CROSSBOW:
				return WeaponType.CROSSBOW;
			case DUAL_DAGGER:
				return WeaponType.DUALDAGGER;
			case BIGBLUNT:
				return WeaponType.BIGBLUNT;
			case BIGSWORD:
				return WeaponType.BIGSWORD;
			default:
				return null;
		}
	}
	
	@Override
	public int ABNORMAL_NULL() { return AbnormalEffect.NULL.getMask(); }
	@Override
	public int ABNORMAL_BLEEDING() { return AbnormalEffect.BLEEDING.getMask(); }
	@Override
	public int ABNORMAL_POISON() { return AbnormalEffect.POISON.getMask(); }
	@Override
	public int ABNORMAL_REDCIRCLE() { return AbnormalEffect.REDCIRCLE.getMask(); }
	@Override
	public int ABNORMAL_ICE() { return AbnormalEffect.ICE.getMask(); }
	@Override
	public int ABNORMAL_WIND() { return AbnormalEffect.WIND.getMask(); }
	@Override
	public int ABNORMAL_FEAR() { return AbnormalEffect.FEAR.getMask(); }
	@Override
	public int ABNORMAL_STUN() { return AbnormalEffect.STUN.getMask(); }
	@Override
	public int ABNORMAL_SLEEP() { return AbnormalEffect.SLEEP.getMask(); }
	@Override
	public int ABNORMAL_MUTED() { return AbnormalEffect.MUTED.getMask(); }
	@Override
	public int ABNORMAL_ROOT() { return AbnormalEffect.ROOT.getMask(); }
	@Override
	public int ABNORMAL_HOLD_1() { return AbnormalEffect.HOLD_1.getMask(); }
	@Override
	public int ABNORMAL_HOLD_2() { return AbnormalEffect.HOLD_2.getMask(); }
	@Override
	public int ABNORMAL_UNKNOWN_13() { return AbnormalEffect.UNKNOWN_13.getMask(); }
	@Override
	public int ABNORMAL_BIG_HEAD() { return AbnormalEffect.BIG_HEAD.getMask(); }
	@Override
	public int ABNORMAL_FLAME() { return AbnormalEffect.FLAME.getMask(); }
	@Override
	public int ABNORMAL_UNKNOWN_16() { return AbnormalEffect.UNKNOWN_16.getMask(); }
	@Override
	public int ABNORMAL_GROW() { return AbnormalEffect.GROW.getMask(); }
	@Override
	public int ABNORMAL_FLOATING_ROOT() { return AbnormalEffect.FLOATING_ROOT.getMask(); }
	@Override
	public int ABNORMAL_DANCE_STUNNED() { return AbnormalEffect.DANCE_STUNNED.getMask(); }
	@Override
	public int ABNORMAL_FIREROOT_STUN() { return AbnormalEffect.FIREROOT_STUN.getMask(); }
	@Override
	public int ABNORMAL_STEALTH() { return AbnormalEffect.STEALTH.getMask(); }
	@Override
	public int ABNORMAL_IMPRISIONING_1() { return AbnormalEffect.IMPRISIONING_1.getMask(); }
	@Override
	public int ABNORMAL_IMPRISIONING_2() { return AbnormalEffect.IMPRISIONING_2.getMask(); }
	@Override
	public int ABNORMAL_MAGIC_CIRCLE() { return AbnormalEffect.MAGIC_CIRCLE.getMask(); }
	@Override
	public int ABNORMAL_ICE2() { return AbnormalEffect.ICE2.getMask(); }
	@Override
	public int ABNORMAL_EARTHQUAKE() { return AbnormalEffect.EARTHQUAKE.getMask(); }
	@Override
	public int ABNORMAL_UNKNOWN_27() { return AbnormalEffect.UNKNOWN_27.getMask(); }
	@Override
	public int ABNORMAL_INVULNERABLE() { return AbnormalEffect.INVULNERABLE.getMask(); }
	@Override
	public int ABNORMAL_VITALITY() { return AbnormalEffect.VITALITY.getMask(); }
	@Override
	public int ABNORMAL_REAL_TARGET() { return AbnormalEffect.REAL_TARGET.getMask(); }
	@Override
	public int ABNORMAL_DEATH_MARK() { return AbnormalEffect.DEATH_MARK.getMask(); }
	@Override
	public int ABNORMAL_SKULL_FEAR() { return AbnormalEffect.UNKNOWN_32.getMask(); }
	//CONFUSED("confused", 0x0020.getMask(); }
	
	// special effects
	@Override
	public int ABNORMAL_S_INVINCIBLE() { return AbnormalEffect.S_INVULNERABLE.getMask(); }
	@Override
	public int ABNORMAL_S_AIR_STUN() { return AbnormalEffect.S_AIR_STUN.getMask(); }
	@Override
	public int ABNORMAL_S_AIR_ROOT() { return AbnormalEffect.S_AIR_ROOT.getMask(); }
	@Override
	public int ABNORMAL_S_BAGUETTE_SWORD() { return AbnormalEffect.S_BAGUETTE_SWORD.getMask(); }
	@Override
	public int ABNORMAL_S_YELLOW_AFFRO() { return AbnormalEffect.S_YELLOW_AFFRO.getMask(); }
	@Override
	public int ABNORMAL_S_PINK_AFFRO() { return AbnormalEffect.S_PINK_AFFRO.getMask(); }
	@Override
	public int ABNORMAL_S_BLACK_AFFRO() { return AbnormalEffect.S_BLACK_AFFRO.getMask(); }
	@Override
	public int ABNORMAL_S_UNKNOWN8() { return AbnormalEffect.S_UNKNOWN8.getMask(); }
	@Override
	public int ABNORMAL_S_STIGMA_SHILIEN() { return AbnormalEffect.S_UNKNOWN9.getMask(); }
	@Override
	public int ABNORMAL_S_STAKATOROOT() { return -1; }
	@Override
	public int ABNORMAL_S_FREEZING() { return -1; }
	@Override
	public int ABNORMAL_S_VESPER() { return -1; }
	
	// event effects
	@Override
	public int ABNORMAL_E_AFRO_1() { return -1; }
	@Override
	public int ABNORMAL_E_AFRO_2() { return -1; }
	@Override
	public int ABNORMAL_E_AFRO_3() { return -1; }
	@Override
	public int ABNORMAL_E_EVASWRATH() { return -1; }
	@Override
	public int ABNORMAL_E_HEADPHONE() { return -1; }
	@Override
	public int ABNORMAL_E_VESPER_1() { return -1; }
	@Override
	public int ABNORMAL_E_VESPER_2() { return -1; }
	@Override
	public int ABNORMAL_E_VESPER_3() { return -1; }
	
	public static Values getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final Values _instance = new Values();
	}
}
