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
package net.sf.l2j.gameserver.templates.item;



/**
 * @author mkizub
 * <BR>Description of Weapon Type
 */
public enum L2WeaponType implements ItemType
{
	NONE(1, "Shield"), // Shields!!!  2
	SWORD(2, "Sword"), //4
	BLUNT(3, "Blunt"),  //8
	DAGGER(4, "Dagger"),  //16
	BOW(5, "Bow"),      //32
	POLE(6, "Pole"),    //64
	ETC(7, "Etc"),      //128
	FIST(8, "Fist"),    //256
	DUAL(9, "Dual Sword"), //512
	DUALFIST(10, "Dual Fist"), //1024
	BIGSWORD(11, "Big Sword"), // Two Handed Swords 2048
	PET(12, "Pet"), //4096
	ROD(13, "Rod"), //8192
	BIGBLUNT(14, "Big Blunt"), //16384
	ANCIENT_SWORD(15, "Ancient"), //32768
	CROSSBOW(16, "Crossbow"),   //65536
	RAPIER(17, "Rapier"),    //131072
	DUAL_DAGGER(18, "Dual Dagger"); //262144
	
	private final int _id;
	private final String _name;
	
	/**
	 * Constructor of the L2WeaponType.
	 * @param id : int designating the ID of the WeaponType
	 * @param name : String designating the name of the WeaponType
	 */
	private L2WeaponType(int id, String name)
	{
		_id = id;
		_name = name;
	}
	
	/**
	 * Returns the ID of the item after applying the mask.
	 * @return int : ID of the item
	 */
	public int mask()
	{
		return 1 << _id;
	}
	
	/**
	 * Returns the name of the WeaponType
	 * @return String
	 */
	@Override
	public String toString()
	{
		return _name;
	}
	
}
