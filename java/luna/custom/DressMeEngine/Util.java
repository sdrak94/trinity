package luna.custom.DressMeEngine;

import java.text.NumberFormat;
import java.util.Locale;

import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;



public class Util
{
	public static String getItemName(int itemId)
	{
		if (itemId == -300)
		{
			return "Fame";
		}
		
		else if (itemId == -200)
		{
			return "Clan reputation";
		}
		else
		{
			return ItemTable.getInstance().getTemplate(itemId).getName();
		}
	}
	
	public static String getItemIcon(int itemId)
	{
		return ItemTable.getInstance().getTemplate(itemId) == null ? "Icon.Default" : ItemTable.getInstance().getTemplate(itemId).getIcon();
	}
	
	public static String formatPay(L2PcInstance player, long count, int item)
	{
		if (count > 0)
		{
			return formatAdena(count) + " " + getItemName(item);
		}
		return "Free";
	}
	
	private static NumberFormat adenaFormatter = NumberFormat.getIntegerInstance(Locale.FRANCE);
	
	/**
	 * Return amount of adena formatted with " " delimiter
	 * @param amount
	 * @return String formatted adena amount
	 */
	public static String formatAdena(long amount)
	{
		return adenaFormatter.format(amount);
	}
}