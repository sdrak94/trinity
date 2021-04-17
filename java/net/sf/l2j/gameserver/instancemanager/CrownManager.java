package net.sf.l2j.gameserver.instancemanager;

import net.sf.l2j.gameserver.datatables.CrownTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Fort;

public final class CrownManager
{
	public static void checkCrowns(L2Clan clan)
	{
		if (clan == null)
			return;
		
		for (L2ClanMember member : clan.getMembers())
			if (member != null && member.isOnline())
				checkCrowns(member.getPlayerInstance());
	}
	
	public static void checkCrowns(L2PcInstance activeChar)
	{
		if (activeChar == null)
			return;
		
		int cloakId = -1;
		int crownId = -1;
		boolean isLeader = false;
		
		final L2Clan clan = activeChar.getClan();
		
		if (clan != null)
		{
			if (clan.getLeaderId() == activeChar.getObjectId())
				isLeader = true;
			else if (activeChar.isThisCharacterMarried() && activeChar.getPartnerId() == clan.getLeaderId())
				isLeader = true;
			
			final Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
			
			if (castle != null)
			{
				crownId = CrownTable.getCrownId(castle.getCastleId());
				cloakId = CrownTable.getCloakId(castle.getCastleId(), isLeader);
			}
			else
			{
				final Fort fort = FortManager.getInstance().getFortByOwner(clan);
				
				if (fort != null)
				{
					if (fort.getFortId() == 117) //western fortress
						cloakId = CrownTable.FORT_CLOAK;
				}
			}
		}
		
		boolean alreadyFoundCirclet = false;
		boolean alreadyFoundCrown = false;
		
		for (L2ItemInstance item : activeChar.getInventory().getItems())
		{
			if (CrownTable.getCastleCloaks().contains(item.getItemId()))
			{
				if (cloakId != item.getItemId())
				{
					activeChar.destroyItem("Removing Cloak", item, activeChar, true);
					activeChar.getInventory().updateDatabase();
				}
			}
			else
			{
				for (Integer crown : CrownTable.getCrownIds())
				{
					if (crown == item.getItemId())
					{
						if (crownId > 0)
						{
							if (item.getItemId() == crownId)
							{
								if (!alreadyFoundCirclet)
								{
									alreadyFoundCirclet = true;
									continue;
								}
							}
							else if (item.getItemId() == 6841 && isLeader)
							{
								if (!alreadyFoundCrown)
								{
									alreadyFoundCrown = true;
									continue;
								}
							}
						}					

						activeChar.destroyItem("Removing Crown", item, activeChar, true);
						activeChar.getInventory().updateDatabase();
					}
				}
			}
		}
	}
}