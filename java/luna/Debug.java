package luna;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import net.sf.l2j.gameserver.Announcements;


public class Debug
{
	public static void append(Object obj)
	{
		
		if (obj == null)
			obj = "null";
		Announcements.getInstance().announceToAll("[D]: " + obj.toString());
		System.err.println("[D]: " + obj.toString());
	}
	
	public static void toClipboard(final String cp)
	{
		
		StringSelection selection = new StringSelection(cp);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
	}
	
	public static <V> void debugArray(V[] vs)
	{
		
		for (V v : vs)
			System.err.print(v + " ");
		System.err.println();
	}

	public static void debugArray(int[] vs)
	{
		
		for (int v : vs)
			System.err.print(v + " ");
		System.err.println();
	}

	public static void debugArray(double[] vs)
	{
		
		for (var v : vs)
			System.err.print(v + " ");
		System.err.println();
	}
}
