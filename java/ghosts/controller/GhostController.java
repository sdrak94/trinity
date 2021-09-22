package ghosts.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import ghosts.model.Ghost;
import ghosts.model.GhostTemplate;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.templates.chars.L2PcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Henna;
import net.sf.l2j.util.Rnd;

public class GhostController
{
	public ConcurrentLinkedDeque<String>	availNames		= new ConcurrentLinkedDeque<>();
	public List<String>							titles			= new ArrayList<>();
	private static final String				GET_AVAIL_NAMES	= "SELECT * FROM ghost_names WHERE ghost_id IS NULL";
	private static final String				GET_ALL_TITLES	= "SELECT * FROM ghost_titles";
	private static final String				SET_AVAIL_NAME	= "UPDATE ghost_names SET ghost_id = ? WHERE name = ?";
	private static final String				DEL_AVAIL_NAME	= "DELETE FROM ghost_names WHERE name = ?";
	
	private GhostController()
	{
		load();
	}
	
	private void load()
	{
		loadNames();
		loadTtitles();
	}
	
	private void loadNames()
	{
		final ArrayList<String> namesList = new ArrayList<>(5000);
		final long t0 = System.currentTimeMillis();
		try (final Connection con = L2DatabaseFactory.getConnectionS(); final Statement st = con.createStatement(); final ResultSet rs = st.executeQuery(GET_AVAIL_NAMES))
		{
			while (rs.next())
			{
				final String availName = rs.getString(1);
				namesList.add(availName);
			}
			Collections.shuffle(namesList);
			availNames.addAll(namesList);
			System.out.println("Loaded " + availNames.size() + " available ghost names in " + (System.currentTimeMillis() - t0) + " ms.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	private void loadTtitles()
	{
		final ArrayList<String> namesList = new ArrayList<>(5000);
		final long t0 = System.currentTimeMillis();
		try (final Connection con = L2DatabaseFactory.getConnectionS(); final Statement st = con.createStatement(); final ResultSet rs = st.executeQuery(GET_ALL_TITLES))
		{
			while (rs.next())
			{
				final String availName = rs.getString(1);
				namesList.add(availName);
			}
			Collections.shuffle(namesList);
			titles.addAll(namesList);
			System.out.println("Loaded " + titles.size() + " available ghost titles in " + (System.currentTimeMillis() - t0) + " ms.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public Ghost createGhost(final GhostTemplate ghostTemplate)
	{
		final int ghostId = IdFactory.getInstance().getNextId();
		final String ghostName = claimName(ghostId);
		final String accountName = "GHOST" + ghostId; // TODO
		final var appearance = createAppearance();
		final var classId = ghostTemplate.getClassId();
		final L2PcTemplate playerTemplate = CharTemplateTable.getInstance().getTemplate(classId);
		final var ghost = new Ghost(ghostId, playerTemplate, accountName, appearance, ghostTemplate);
		ghost.setName(ghostName);
		ghost.setTitle(titles.get(Rnd.get(titles.size() -1)));
		ghost.setPvpKills(ghostTemplate.getPvPs());
		ghost.setUptime(System.currentTimeMillis());
		ghost.setFame((int) (ghost.getPvpKills() * 1.16));
		//ghost.setOnlineStatus(true);
		preprocessGhost(ghost);
		return ghost;
	}
	
	public void spawnGhost(final Ghost ghost, final int x, final int y, final int z)
	{
		ghost.setOnlineStatus(true);
		ghost.canSendUserInfo = true;
		ghost.spawnMe(x, y, z);
		ghost.spawnMe();
		L2World.getInstance().addVisibleObject(ghost, ghost.getPosition().getWorldRegion());
		// ghost.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(ghost.getX() + Rnd.get(-1000, 1000), ghost.getY() + Rnd.get(-1000, 1000), ghost.getZ()));
		ghost.setupAutoChill();
		ghost.setProtection(false);
		ghost.setForceNoSpawnProtection(true);
		RegionBBSManager.getInstance().changeCommunityBoard();
	}
	
	public void deleteGhost(final Ghost ghost)
	{
		ghost.deleteMe();
		RegionBBSManager.getInstance().changeCommunityBoard();
	}
	
	private String claimName(final int ghostId)
	{
		final String availName = availNames.pop();
		boolean nameExists = CharNameTable.getInstance().doesCharNameExist(availName);
		if (nameExists)
		{
			try (final Connection con = L2DatabaseFactory.getConnectionS(); final PreparedStatement pst = con.prepareStatement(DEL_AVAIL_NAME))
			{
				pst.setString(1, availName);
				pst.executeUpdate();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return claimName(ghostId); // recursion
		}
		try (final Connection con = L2DatabaseFactory.getConnectionS(); final PreparedStatement pst = con.prepareStatement(SET_AVAIL_NAME))
		{
			pst.setInt(1, ghostId);
			pst.setString(2, availName);
			pst.executeUpdate();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return availName;
	}
	
	private void preprocessGhost(final Ghost newGhost)
	{
		final GhostTemplate ghostTemplate = newGhost.getGhostTemplate();
		newGhost.setRunning(); // running is default
		// newGhost.standUp(); // standing is default
		// newGhost.setRace(Race.getRandomRace());
		newGhost.setAccessLevel(0);
		newGhost.setBaseClass(newGhost.getClassId());
		newGhost.setLvl(ghostTemplate.getLevel());
		newGhost.setPvpKills(ghostTemplate.getPvPs());
		newGhost.rewardSkills();
		for (final L2Henna henna : ghostTemplate.getHennas())
			newGhost.addHenna(henna);
		giveInitItems(newGhost);
		newGhost.setNameColorsDueToPVP();
		newGhost.buffSelf();
		newGhost.setClient(null);
		
	}
	
	private void giveInitItems(final Ghost ghost)
	{
		final var ghostTemplate = ghost.getGhostTemplate();
		for (final var inventoryTemplate : ghostTemplate.getInventoryTemplates())
		{
			for (final var inventoryItem : inventoryTemplate.getInventoryItems())
			{
				final var item = ItemTable.getInstance().createItem("INIT_" + ghost.getAccountName(), inventoryItem.getItemId(), inventoryItem.getCount(), ghost);
				if(inventoryItem.isTemp())
				{
					item.setFakeTempItem(true);
				}
				item.setEnchantLevel(inventoryItem.pickEnchant());
				ghost.addItem("INIT_" + ghost.getAccountName(), item, ghost, false);
				if (inventoryItem.isIsEquipped())
				{
					ghost.useEquippableItem(item, true, true);
				}
			}
		}
	}
	
	private PcAppearance createAppearance()
	{
		final boolean sex = Rnd.nextBoolean();
		final byte face = (byte) Rnd.get(0, 2);
		final byte hairColor = (byte) Rnd.get(0, 3);
		final byte hairStyle = (byte) (sex ? Rnd.get(0, 6) : Rnd.get(0, 4));
		final var appearance = new PcAppearance(face, hairColor, hairStyle, sex);
		return appearance;
	}
	
	public static class InstanceHolder
	{
		private static final GhostController _instance = new GhostController();
	}
	
	public static GhostController getInstance()
	{
		return InstanceHolder._instance;
	}
}
