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
package net.sf.l2j.gameserver.model.base;

/**
 * $ Rewrite 06.12.06 - Yesod
 * */
public class SoulCrystal
{
    public static final int[][] HighSoulConvert =
    {
     {4639, 5577}, //RED 10 - 11
     {5577, 5580}, //RED 11 - 12
     {5580, 5908}, //RED 12 - 13
     {5908, 9570}, //RED 13 - 14

     {4650, 5578}, //GRN 10 - 11
     {5578, 5581}, //GRN 11 - 12
     {5581, 5911}, //GRN 12 - 13
     {5911, 9572}, //GRN 13 - 14

     {4661, 5579}, //BLU 10 - 11
     {5579, 5582}, //BLU 11 - 12
     {5582, 5914},  //BLU 12 - 13
     {5914, 9571}  //BLU 13 - 14 
    };

   /**
    * "First line is for Red Soul Crystals, second is Green and third is Blue Soul Crystals,
    *  ordered by ascending level, from 0 to 13..."
    */
   public static final int[] SoulCrystalTable =
   {
     4629, 4630, 4631, 4632, 4633, 4634, 4635, 4636, 4637, 4638, 4639, 5577, 5580, 5908, 9570,
     4640, 4641, 4642, 4643, 4644, 4645, 4646, 4647, 4648, 4649, 4650, 5578, 5581, 5911, 9572,
     4651, 4652, 4653, 4654, 4655, 4656, 4657, 4658, 4659, 4660, 4661, 5579, 5582, 5914, 9571
   };

   public static final int MAX_CRYSTALS_LEVEL = 14;
   public static final int LEVEL_CHANCE = 32;

   public static final int RED_NEW_CRYSTAL = 4629;
   public static final int GRN_NEW_CYRSTAL = 4640;
   public static final int BLU_NEW_CRYSTAL = 4651;
	
}
