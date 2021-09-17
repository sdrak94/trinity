package Alpha.autopots;

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;

public class Utils
{
	public static int fixIdForNonKamaelDelf(L2PcInstance player, int id)
	{
		L2Weapon weap = (L2Weapon) ItemTable.getInstance().getTemplate(id);
		if (weap == null)
			return id;
		if (weap.getItemType() == L2WeaponType.CROSSBOW)
		{
			if (player == null)
				return id;
			if (player.getRace() != Race.Kamael && player.getRace() != Race.DarkElf)
			{
				if (weap.getNonKamaelDisplayId() > 0)
				{
					return weap.getNonKamaelDisplayId();
				}
			}
		}
		return id;
	}
}
