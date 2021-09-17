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
package Alpha.autopots;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.itemhandlers.ItemSkills;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

public class AutoPots implements IItemHandler
{
	private static final int	MANA_POT_CD		= 3;
	private static final int	HEALING_POT_CD	= 11;
	private static final int	CP_POT_CD		= 2;
	
	@Override
	public void useItem(final L2Playable playable, final L2ItemInstance item, final boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		final L2PcInstance activeChar = (L2PcInstance) playable;
		final int itemId = item.getItemId();
		if (forceUse)
		{
			if (itemId == 5283 || itemId == 1539 || itemId == 5592)
			{
				switch (itemId)
				{
					case 5283: // mana potion
					{
						if (activeChar.isAutoPot(5283))
						{
							activeChar.sendPacket(new ExAutoSoulShot(5283, 0));
							activeChar.sendMessage("Deactivated auto Rice Cakes.");
							activeChar.setAutoPot(5283, null, false);
						}
						else
						{
							if (activeChar.getInventory().getItemByItemId(5283) != null)
							{
									activeChar.sendPacket(new ExAutoSoulShot(5283, 1));
									activeChar.sendMessage("Activated auto Rice Cakes.");
									activeChar.setAutoPot(5283, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(5283, activeChar), 1000, MANA_POT_CD * 1000), true);
							}
						}
						break;
					}
					case 1539: // greater healing potion
					{
						if (activeChar.isAutoPot(1539))
						{
							activeChar.sendPacket(new ExAutoSoulShot(1539, 0));
							activeChar.sendMessage("Deactivated auto healing potions.");
							activeChar.setAutoPot(1539, null, false);
						}
						else
						{
							if (activeChar.getInventory().getItemByItemId(1539) != null)
							{
								activeChar.sendPacket(new ExAutoSoulShot(1539, 1));
								activeChar.sendMessage("Activated auto healing potions.");
								activeChar.setAutoPot(1539, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(1539, activeChar), 1000, HEALING_POT_CD * 1000), true);
							}
						}
						break;
					}
					case 5592: // greater cp potion
					{
						if (activeChar.isAutoPot(5592))
						{
							activeChar.sendPacket(new ExAutoSoulShot(5592, 0));
							activeChar.sendMessage("Deactivated auto cp potions.");
							activeChar.setAutoPot(5592, null, false);
						}
						else
						{
							if (activeChar.getInventory().getItemByItemId(5592) != null)
							{
								activeChar.sendPacket(new ExAutoSoulShot(5592, 1));
								activeChar.sendMessage("Activated auto cp potions.");
								activeChar.setAutoPot(5592, ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoPot(5592, activeChar), 1000, CP_POT_CD * 1000), true);
							}
						}
						break;
					}
				}
			}
			return;
		}
		else
		{
			if (itemId == 5283 || itemId == 1539 || itemId == 5592)
			{
				if (!activeChar.getFloodProtectors().getUseItem().tryPerformAction("use item"))
				{
					return;
				}
				switch (itemId)
				{
					case 5283: // mana potion
					{
						ItemSkills is = new ItemSkills();
						is.useItem(activeChar, activeChar.getInventory().getItemByItemId(5283), true);
						break;
					}
					case 1539: // greater healing potion
					{
						ItemSkills is = new ItemSkills();
						is.useItem(activeChar, activeChar.getInventory().getItemByItemId(1539), true);
						break;
					}
					case 5592: // greater cp potion
					{
						ItemSkills is = new ItemSkills();
						is.useItem(activeChar, activeChar.getInventory().getItemByItemId(5592), true);
						break;
					}
				}
			}
		}
		// activeChar.sendPacket(SystemMessageId.ENABLED_SOULSHOT);
	}
	
	private class AutoPot implements Runnable
	{
		private int				id;
		private L2PcInstance	activeChar;
		
		public AutoPot(int id, L2PcInstance activeChar)
		{
			this.id = id;
			this.activeChar = activeChar;
		}
		
		@Override
		public void run()
		{
			if (activeChar.getInventory().getItemByItemId(id) == null)
			{
				activeChar.sendPacket(new ExAutoSoulShot(id, 0));
				activeChar.setAutoPot(id, null, false);
				return;
			}
			int perc = 90;
			switch (id)
			{
				case 5283:
				{
					Float minv = 0.90f;
					if (activeChar.getVar("perc_autopot_mp") != null)
					{
						try
						{
							perc = Integer.parseInt(activeChar.getVar("perc_autopot_mp"));
							String p = perc < 10 ? "0" + perc : perc + "";
							p = "0." + p;
							minv = Float.parseFloat(p);
						}
						catch (NumberFormatException e)
						{
							e.printStackTrace();
						}
					}
					if (activeChar.getCurrentMp() < minv * activeChar.getMaxMp())
					{
						MagicSkillUse msu = new MagicSkillUse(activeChar, activeChar, 2279, 2, 0, 100);
						activeChar.broadcastPacket(msu);
						ItemSkills is = new ItemSkills();
						activeChar.useItem(activeChar.getInventory().getItemByItemId(5283).getObjectId(), false);
						// is.useItem(activeChar, activeChar.getInventory().getItemByItemId(5283), true);
					}
					break;
				}
				case 1539:
				{
					Float minv = 0.95f;
					if (activeChar.getVar("perc_autopot_hp") != null)
					{
						try
						{
							perc = Integer.parseInt(activeChar.getVar("perc_autopot_hp"));
							String p = perc < 10 ? "0" + perc : perc + "";
							p = "0." + p;
							minv = Float.parseFloat(p);
						}
						catch (NumberFormatException e)
						{
							e.printStackTrace();
						}
					}
					if (activeChar.getCurrentHp() < minv * activeChar.getMaxHp())
					{
						MagicSkillUse msu = new MagicSkillUse(activeChar, activeChar, 2037, 1, 0, 100);
						activeChar.broadcastPacket(msu);
						ItemSkills is = new ItemSkills();
						activeChar.useItem(activeChar.getInventory().getItemByItemId(1539).getObjectId(), false);
						// is.useItem(activeChar, activeChar.getInventory().getItemByItemId(1539), true);
					}
					break;
				}
				case 5592:
				{
					Float minv = 0.95f;
					if (activeChar.getVar("perc_autopot_cp") != null)
					{
						try
						{
							perc = Integer.parseInt(activeChar.getVar("perc_autopot_cp"));
							String p = perc < 10 ? "0" + perc : perc + "";
							p = "0." + p;
							minv = Float.parseFloat(p);
						}
						catch (NumberFormatException e)
						{
							e.printStackTrace();
						}
					}
					if (activeChar.getCurrentCp() < (minv * activeChar.getMaxCp()))
					{
						MagicSkillUse msu = new MagicSkillUse(activeChar, activeChar, 2166, 2, 0, 100);
						activeChar.broadcastPacket(msu);
						ItemSkills is = new ItemSkills();
						activeChar.useItem(activeChar.getInventory().getItemByItemId(5592).getObjectId(), false);
						// is.useItem(activeChar, activeChar.getInventory().getItemByItemId(5592), true);
					}
					break;
				}
			}
			if (activeChar.getInventory().getItemByItemId(id) == null)
			{
				activeChar.sendPacket(new ExAutoSoulShot(id, 0));
				activeChar.setAutoPot(id, null, false);
			}
		}
	}
}
