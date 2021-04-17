package net.sf.l2j.gameserver.model.events.newEvents;

public class RussianRullette
{
	public static class Circle
	{
		public static void DrawMeACircle(int posX, int posY, int radius)
		{
			for (int i = 0; i <= posX + radius; i++)
			{
				for (int j = 1; j <= posY + radius; j++)
				{
					int xSquared = (i - posX) * (i - posX);
					int ySquared = (j - posY) * (j - posY);
					if (Math.abs(xSquared + ySquared - radius * radius) < radius)
					{
						System.out.print("#");
					}
					else
					{
						System.out.print(" ");
					}
				}
				System.out.println();
			}
		}
		
		public static void main(String[] args)
		{
			DrawMeACircle(5, 8, 10);
		}
	}
}
