package ghosts;

import ghosts.controller.GhostController;
import ghosts.controller.GhostTemplateTable;

public class Ghosts
{
	private Ghosts()
	{
		GhostTemplateTable.getInstance();
		GhostController.getInstance();
	}

	public static class InstanceHolder
	{
		private static final Ghosts _instance = new Ghosts();
	}

	public static Ghosts getInstance()
	{
		return InstanceHolder._instance;
	}
	
	public static void main(String[] args)
	{
		getInstance();
	}

}
