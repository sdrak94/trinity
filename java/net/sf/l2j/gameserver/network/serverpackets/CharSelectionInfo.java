/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.serverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.CharSelectInfoPackage;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * This class ...
 *
 * @version $Revision: 1.8.2.4.2.6 $ $Date: 2005/04/06 16:13:46 $
 */
public class CharSelectionInfo extends L2GameServerPacket
{
	// d SdSddddddddddffddddddddddddddddddddddddddddddddddddddddddddddffd
	private static final String		_S__1F_CHARSELECTINFO	= "[S] 09 CharSelectInfo";
	private static Logger			_log					= Logger.getLogger(CharSelectionInfo.class.getName());
	private String					_loginName;
	private int						_sessionId, _activeId;
	private CharSelectInfoPackage[]	_characterPackages;
	
	/**
	 * @param _characters
	 */
	public CharSelectionInfo(String loginName, int sessionId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo();
		_activeId = -1;
	}
	
	public CharSelectionInfo(String loginName, int sessionId, int activeId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo();
		_activeId = activeId;
	}
	
	public CharSelectInfoPackage[] getCharInfo()
	{
		return _characterPackages;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x09);
		int size = (_characterPackages.length);
		writeD(size);
		// Can prevent players from creating new characters (if 0); (if 1, the client will ask if chars may be created (0x13) Response: (0x0D) )
		writeD(0x07);
		writeC(0x00);
		long lastAccess = 0L;
		if (_activeId == -1)
		{
			for (int i = 0; i < size; i++)
			{
				if (lastAccess < _characterPackages[i].getLastAccess())
				{
					lastAccess = _characterPackages[i].getLastAccess();
					_activeId = i;
				}
			}
		}
		for (int i = 0; i < size; i++)
		{
			CharSelectInfoPackage charInfoPackage = _characterPackages[i];
			writeS(charInfoPackage.getName());
			writeD(charInfoPackage.getCharId());
			writeS(_loginName);
			writeD(_sessionId);
			writeD(charInfoPackage.getClanId());
			writeD(0x00); // ??
			writeD(charInfoPackage.getSex());
			final int race = charInfoPackage.getRace();
			if (race == 6)
				writeD(0);
			else if (race == 7)
				writeD(3);
			else
				writeD(race);
			if (race == 6)
				writeD(0x10); // bishop
			else if (race == 7)
				writeD(0x34); // warcryer
			else if (race == 3)
				writeD(0x2e); // destroyer
			else if (race == 0)
				writeD(0x03); // gladiator
			else if (charInfoPackage.getClassId() == charInfoPackage.getBaseClassId())
				writeD(charInfoPackage.getClassId());
			else
				writeD(charInfoPackage.getBaseClassId());
			writeD(0x01); // active ??
			writeD(charInfoPackage.getX()); // x
			writeD(charInfoPackage.getY()); // y
			writeD(charInfoPackage.getZ()); // z
			writeF(charInfoPackage.getCurrentHp()); // hp cur
			writeF(charInfoPackage.getCurrentMp()); // mp cur
			writeD(charInfoPackage.getSp());
			writeQ(charInfoPackage.getExp());
			writeD(charInfoPackage.getLevel());
			writeD(charInfoPackage.getKarma()); // karma
			writeD(charInfoPackage.getPkKills());
			writeD(charInfoPackage.getPvPKills());
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HAIRALL));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_REAR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_NECK));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HAIR2));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RBRACELET));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LBRACELET));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_DECO1));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_DECO2));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_DECO3));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_DECO4));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_DECO5));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_DECO6));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_BELT));
			writeD(charInfoPackage.getHairStyle());
			writeD(charInfoPackage.getHairColor());
			writeD(charInfoPackage.getFace());
			writeF(charInfoPackage.getMaxHp()); // hp max
			writeF(charInfoPackage.getMaxMp()); // mp max
			long deleteTime = charInfoPackage.getDeleteTimer();
			int deletedays = 0;
			if (deleteTime > 0)
				deletedays = (int) ((deleteTime - System.currentTimeMillis()) / 1000);
			writeD(deletedays); // days left before
			// delete .. if != 0
			// then char is inactive
			writeD(charInfoPackage.getClassId());
			if (i == _activeId)
				writeD(0x01);
			else
				writeD(0x00); // c3 auto-select char
			int enchantLevel = charInfoPackage.getEnchantEffect();
			if (charInfoPackage.getWeaponItemId() > 0)
			{
				final L2Item item = ItemTable.getInstance().getTemplate(charInfoPackage.getWeaponItemId());
				if (item != null)
				{
					switch (item.getItemId())
					{
						case Config.AURAFANG:
						case Config.RAYBLADE:
						case Config.WAVEBRAND:
						{
							enchantLevel = 0;
							break;
						}
						default:
							if (item.isMorheim())
								enchantLevel += 50;
							if (item.isDread())
								enchantLevel += 6;
							if (item.isWarForged())
								enchantLevel += 6;
					}
				}
			}
			enchantLevel = Math.min(enchantLevel, 127);
			writeC(enchantLevel);
			writeD(charInfoPackage.getAugmentationId());
			writeD(charInfoPackage.getTransformId()); // Used to display Transformations
			// writeD(0x00); // Currently on retail when you are on character select you don't see your transformation.
		}
	}
	
	private CharSelectInfoPackage[] loadCharacterSelectInfo()
	{
		CharSelectInfoPackage charInfopackage;
		List<CharSelectInfoPackage> characterList = new FastList<CharSelectInfoPackage>();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT account_name, charId, char_name, level, maxHp, curHp, maxMp, curMp, face, hairStyle, hairColor, sex, heading, x, y, z, exp, sp, karma, pvpkills, pkkills, clanid, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, base_class, transform_id FROM characters WHERE account_name=?");
			statement.setString(1, _loginName);
			ResultSet charList = statement.executeQuery();
			while (charList.next())// fills the package
			{
				charInfopackage = restoreChar(charList);
				if (charInfopackage != null)
					characterList.add(charInfopackage);
			}
			charList.close();
			statement.close();
			return characterList.toArray(new CharSelectInfoPackage[characterList.size()]);
		}
		catch (Exception e)
		{
			_log.warning("Could not restore char info: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
		return new CharSelectInfoPackage[0];
	}
	
	private static void loadCharacterSubclassInfo(CharSelectInfoPackage charInfopackage, int ObjectId, int activeClassId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT exp, sp, level FROM character_subclasses WHERE charId=? && class_id=? ORDER BY charId");
			statement.setInt(1, ObjectId);
			statement.setInt(2, activeClassId);
			ResultSet charList = statement.executeQuery();
			if (charList.next())
			{
				charInfopackage.setExp(charList.getLong("exp"));
				charInfopackage.setSp(charList.getInt("sp"));
				charInfopackage.setLevel(charList.getInt("level"));
			}
			charList.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore char subclass info: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{}
		}
	}
	
	public static CharSelectInfoPackage restoreChar(ResultSet chardata) throws Exception
	{
		int objectId = chardata.getInt("charId");
		String name = chardata.getString("char_name");
		// See if the char must be deleted
		long deletetime = chardata.getLong("deletetime");
		if (deletetime > 0)
		{
			if (System.currentTimeMillis() > deletetime)
			{
				L2Clan clan = ClanTable.getInstance().getClan(chardata.getInt("clanid"));
				if (clan != null)
					clan.removeClanMember(objectId, 0);
				L2GameClient.deleteCharByObjId(objectId);
				return null;
			}
		}
		CharSelectInfoPackage charInfopackage = new CharSelectInfoPackage(objectId, name);
		charInfopackage.setLevel(chardata.getInt("level"));
		charInfopackage.setMaxHp(chardata.getInt("maxhp"));
		charInfopackage.setCurrentHp(chardata.getDouble("curhp"));
		charInfopackage.setMaxMp(chardata.getInt("maxmp"));
		charInfopackage.setCurrentMp(chardata.getDouble("curmp"));
		charInfopackage.setKarma(chardata.getInt("karma"));
		charInfopackage.setPkKills(chardata.getInt("pkkills"));
		charInfopackage.setPvPKills(chardata.getInt("pvpkills"));
		charInfopackage.setFace(chardata.getInt("face"));
		charInfopackage.setHairStyle(chardata.getInt("hairstyle"));
		charInfopackage.setHairColor(chardata.getInt("haircolor"));
		charInfopackage.setSex(chardata.getInt("sex"));
		charInfopackage.setExp(chardata.getLong("exp"));
		charInfopackage.setSp(chardata.getInt("sp"));
		charInfopackage.setClanId(chardata.getInt("clanid"));
		charInfopackage.setRace(chardata.getInt("race"));
		final int baseClassId = chardata.getInt("base_class");
		final int activeClassId = chardata.getInt("classid");
		charInfopackage.setX(chardata.getInt("x"));
		charInfopackage.setY(chardata.getInt("y"));
		charInfopackage.setZ(chardata.getInt("z"));
		// if is in subclass, load subclass exp, sp, lvl info
		if (baseClassId != activeClassId)
			loadCharacterSubclassInfo(charInfopackage, objectId, activeClassId);
		charInfopackage.setClassId(activeClassId);
		// Get the augmentation id for equipped weapon
		int weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND);
		if (weaponObjId < 1)
			weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND);
		// Check Transformation
		int cursedWeaponId = CursedWeaponsManager.getInstance().checkOwnsWeaponId(objectId);
		if (cursedWeaponId > 0)
		{
			// cursed weapon transformations
			// if(cursedWeaponId == 8190)
			// charInfopackage.setTransformId(301);
			// else if(cursedWeaponId == 8689)
			// charInfopackage.setTransformId(302);
			// else
			charInfopackage.setTransformId(0);
		}
		else if (chardata.getInt("transform_id") > 0)
		{
			// charInfopackage.setTransformId(chardata.getInt("transform_id"));
			charInfopackage.setTransformId(0);
		}
		else
			charInfopackage.setTransformId(0);
		if (weaponObjId > 0)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT augAttributes FROM item_attributes WHERE itemId=?");
				statement.setInt(1, weaponObjId);
				ResultSet result = statement.executeQuery();
				if (result.next())
				{
					final L2Item item = ItemTable.getInstance().getTemplate(charInfopackage.getWeaponItemId());
					if (item != null)
					{
						switch (charInfopackage.getWeaponItemId())
						{
							case Config.AURAFANG:
							case Config.RAYBLADE:
							case Config.WAVEBRAND:
								charInfopackage.setAugmentationId(0);
								break;
							default:
								if (item.isHeroItem())
									charInfopackage.setAugmentationId(0);
								else if (item.isDread() || item.isWarForged() || item.isMorheim())
									charInfopackage.setAugmentationId(0);
								else
								{
									int augment = result.getInt("augAttributes");
									charInfopackage.setAugmentationId(augment == -1 ? 0 : augment);
								}
						}
					}
				}
				result.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Could not restore augmentation info: " + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{}
			}
		}
		/*
		 * Check if the base class is set to zero and alse doesn't match
		 * with the current active class, otherwise send the base class ID.
		 * This prevents chars created before base class was introduced
		 * from being displayed incorrectly.
		 */
		if (baseClassId == 0 && activeClassId > 0)
			charInfopackage.setBaseClassId(activeClassId);
		else
			charInfopackage.setBaseClassId(baseClassId);
		charInfopackage.setDeleteTimer(deletetime);
		charInfopackage.setLastAccess(chardata.getLong("lastAccess"));
		return charInfopackage;
	}
	
	@Override
	public String getType()
	{
		return _S__1F_CHARSELECTINFO;
	}
}
