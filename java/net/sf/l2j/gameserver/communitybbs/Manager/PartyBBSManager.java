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
package net.sf.l2j.gameserver.communitybbs.Manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.SubClass;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.RequestJoinParty;
import net.sf.l2j.gameserver.network.serverpackets.AskJoinParty;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

public class PartyBBSManager extends BaseBBSManager
{
	private static final int CHECKED_COUNT = 9; // last checked + 1
	private static final int MAX_PER_PAGE = 14;
	public static final String PATH = "data/html/CommunityBoard";
	
	private static final int[] BUFFERS = { ClassId.inspector.getId(), ClassId.judicator.getId(), ClassId.oracle.getId(), ClassId.orcShaman.getId(), ClassId.prophet.getId(), ClassId.warcryer.getId(), ClassId.overlord.getId(), ClassId.shillienElder.getId(), ClassId.shillienSaint.getId(), ClassId.hierophant.getId(), ClassId.evaSaint.getId(), ClassId.shillienSaint.getId(), ClassId.dominator.getId(), ClassId.doomcryer.getId()};
	private static final int[] DANCERS = {ClassId.bladedancer.getId(), ClassId.spectralDancer.getId()};
	private static final int[] SINGERS = {ClassId.swordSinger.getId(), ClassId.swordMuse.getId()};
	private static final int[] HEALERS = {ClassId.bishop.getId(), ClassId.shillienElder.getId(), ClassId.cardinal.getId(), ClassId.evaSaint.getId(), ClassId.shillienSaint.getId()};
	private static final int[] TANKS = {ClassId.knight.getId(), ClassId.darkAvenger.getId(), ClassId.paladin.getId(), ClassId.palusKnight.getId(), ClassId.shillienKnight.getId(), ClassId.shillienTemplar.getId(), ClassId.phoenixKnight.getId(), ClassId.hellKnight.getId(), ClassId.evaTemplar.getId(), ClassId.shillienTemplar.getId()};
	private static final int[] MAGES = {ClassId.elvenMage.getId(), ClassId.mage.getId(), ClassId.orcShaman.getId(), ClassId.darkMage.getId(), ClassId.wizard.getId(), ClassId.warcryer.getId(), ClassId.overlord.getId(), ClassId.spellsinger.getId(), ClassId.spellhowler.getId(), ClassId.necromancer.getId(), ClassId.sorceror.getId(), ClassId.archmage.getId(), ClassId.soultaker.getId(), ClassId.arcanaLord.getId(), ClassId.mysticMuse.getId(), ClassId.elementalMaster.getId(), ClassId.stormScreamer.getId(), ClassId.spectralMaster.getId(), ClassId.dominator.getId(), ClassId.doomcryer.getId()};
	private static final int[] FIGHTERS = {ClassId.inspector.getId(), ClassId.judicator.getId(), ClassId.abyssWalker.getId(), ClassId.swordSinger.getId(), ClassId.swordMuse.getId(), ClassId.assassin.getId(), ClassId.berserker.getId(), ClassId.bountyHunter.getId(), ClassId.artisan.getId(), ClassId.arbalester.getId(), ClassId.darkFighter.getId(), ClassId.destroyer.getId(), ClassId.doombringer.getId(), ClassId.elvenFighter.getId(), ClassId.darkFighter.getId(), ClassId.dreadnought.getId(), ClassId.warlord.getId(), ClassId.warsmith.getId(), ClassId.warrior.getId(), ClassId.femaleSoldier.getId(), ClassId.bladedancer.getId(), ClassId.spectralDancer.getId(), ClassId.femaleSoulbreaker.getId(), ClassId.femaleSoulhound.getId(), ClassId.maleSoldier.getId(), ClassId.maleSoulbreaker.getId(), ClassId.maleSoulhound.getId(), ClassId.maestro.getId(), ClassId.hawkeye.getId(), ClassId.treasureHunter.getId(), ClassId.titan.getId(), ClassId.trickster.getId(), ClassId.dragoon.getId(), ClassId.tyrant.getId(), ClassId.gladiator.getId(), ClassId.duelist.getId(), ClassId.phantomRanger.getId(), ClassId.plainsWalker.getId(), ClassId.rogue.getId(), ClassId.silverRanger.getId(), ClassId.orcRaider.getId(), ClassId.orcFighter.getId(), ClassId.orcMonk.getId(), ClassId.dreadnought.getId(), ClassId.duelist.getId(), ClassId.adventurer.getId(), ClassId.sagittarius.getId(), ClassId.windRider.getId(), ClassId.moonlightSentinel.getId(), ClassId.ghostHunter.getId(), ClassId.ghostSentinel.getId(), ClassId.titan.getId(), ClassId.grandKhauatari.getId(), ClassId.fortuneSeeker.getId()};
	private static final int[][] CLASS_GROUPS = {Arrays.stream(ClassId.values()).mapToInt(ClassId::getId).toArray(), BUFFERS, DANCERS, SINGERS, HEALERS, TANKS, MAGES, FIGHTERS};
	
	public Set<Integer> _invisiblePlayers;
	
	public PartyBBSManager()
	{
		_invisiblePlayers = new HashSet<>();
	}
	
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command, "_");
		st.nextToken(); // bbspartymatching
		if (!st.hasMoreTokens())
		{
			showMainPage(activeChar, 0, 0, 0, 0, 0);
		}
		else
		{
			int classesSortType = Integer.parseInt(st.nextToken());
			int sortType = Integer.parseInt(st.nextToken());
			int asc = Integer.parseInt(st.nextToken());
			int page = Integer.parseInt(st.nextToken());
			int charObjId = Integer.parseInt(st.nextToken());
			showMainPage(activeChar, classesSortType, sortType, asc, page, charObjId);
			
			if (st.hasMoreTokens())
			{
				int nextNumber = Integer.parseInt(st.nextToken());
				//String NextString = st.nextToken();
				
				if (nextNumber == -1) // Show/Hide on list
				{
					// Player invisible toggled on.
					if (_invisiblePlayers.add(activeChar.getObjectId()))
					{
						activeChar.sendMessage("You are NO LONGER visible on Party Matching list!");
					}
					else // Player invisible toggled off.
					{
						activeChar.sendMessage("You are now visible on Party Matching list!");
						_invisiblePlayers.remove(activeChar.getObjectId());
					}
					showMainPage(activeChar, classesSortType, sortType, asc, page, charObjId);
				}
				if (nextNumber == 3)
				{
					//activeChar.setPartyReason(st.nextToken());
					showMainPage(activeChar, classesSortType, sortType, asc, page, charObjId);
				}
				else // Invite to party
				{
					L2PcInstance invited = L2World.getInstance().getPlayer(charObjId);
					if (invited != null && activeChar != invited && invited.getParty() == null)
					{
						String partyMsg = canJoinParty(invited);
						if (partyMsg == null)
						{
							addTargetToParty(invited, activeChar);
							activeChar.sendMessage("Invitation has been sent!");
						}
						else
						{
							activeChar.sendMessage(partyMsg);
						}
					}
				}
			}
		}
	}
	
	private void showMainPage(L2PcInstance player, int classesSortType, int sortType, int asc, int page, int charObjId)
	{
		String html = HtmCache.getInstance().getHtmForce(PATH + "/bbs_partymatching.htm");
		html = html.replace("%characters%", getCharacters(player, sortType, asc, classesSortType, page, charObjId));
		html = html.replace("%visible%", _invisiblePlayers.contains(player.getObjectId()) ? "Show on list" : "Hide from list");
		html = html.replace("%class%", String.valueOf(classesSortType));
		html = html.replace("%sort%", String.valueOf(sortType));
		html = html.replace("%asc%", String.valueOf(asc));
		html = html.replace("%asc2%", String.valueOf(asc == 0 ? 1 : 0));
		html = html.replace("%page%", String.valueOf(page));
		html = html.replace("%char%", String.valueOf(charObjId));
		
		for (int i = 0; i < CHECKED_COUNT; i++)
			html = html.replace("%checked" + i + "%", getChecked(i, classesSortType));
		
		separateAndSend(html, player);
	}
	
	private void addTargetToParty(L2PcInstance target, L2PcInstance requestor)
	{
		final L2Party party = requestor.getParty();
		
		// Create new party if requestor is not in one.
		if (party == null)
		{
			if (!target.isProcessingRequest())
			{
				requestor.setParty(new L2Party(requestor, L2Party.ITEM_RANDOM_SPOIL));
				
				requestor.onTransactionRequest(target);
				target.sendPacket(new AskJoinParty(requestor.getName(), L2Party.ITEM_RANDOM_SPOIL));
				requestor.getParty().onInviteRequest();
				
				SystemMessage msg = new SystemMessage(SystemMessageId.C1_INVITED_TO_PARTY);
				msg.addString(target.getName());
				requestor.sendPacket(msg);
			}
			else
			{
				requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			}
		}
		else // Invite player to requestor's party.
		{
			RequestJoinParty.addTargetToParty(target, requestor);
		}
	}
	
	private String getCharacters(L2PcInstance visitor, int charSort, int asc, int classSort, int page, int charToView)
	{
		int[] classGroup = classSort < CLASS_GROUPS.length ? CLASS_GROUPS[classSort] : CLASS_GROUPS[0];
		Predicate<L2PcInstance> isPlayerHavingClass = p -> Util.contains(classGroup, p.getBaseClass()) || p.getSubClasses().values().stream().anyMatch(sub -> Util.contains(classGroup, sub.getClassId()));
		
		// Get all players
		List<L2PcInstance> allPlayers = new ArrayList<>();
		if (classSort == 8) // Party
		{
			if (visitor.isInParty())
			{
				allPlayers.addAll(visitor.getParty().getPartyMembers());
			}
			else
			{
				allPlayers.add(visitor);
			}
		}
		else
		{
			L2World.getInstance().getAllPlayers().values().stream().filter(p -> canJoinParty(p) == null).filter(isPlayerHavingClass).forEach(allPlayers::add);
		}
		
		switch (charSort)
		{
			case 0: // Name
				Collections.sort(allPlayers, Comparator.comparing(L2PcInstance::getName));
				break;
			case 1: // Level
				Collections.sort(allPlayers, (p1, p2) -> ((Integer) getMaxLevel(p2, classSort)).compareTo((getMaxLevel(p1, classSort))));
				break;
			case 2: // unlocks
				Collections.sort(allPlayers, (p1, p2) -> ((Integer) getUnlocksSize(p2, classSort)).compareTo((getUnlocksSize(p1, classSort))));
				break;
		}
		
		if (asc == 1)
			Collections.reverse(allPlayers);
		
		StringBuilder html = new StringBuilder();
		int badCharacters = 0;
		boolean isThereNextPage = true;
		
		for (int i = MAX_PER_PAGE * page; i < (MAX_PER_PAGE + badCharacters + page * MAX_PER_PAGE); i++)
		{
			if (allPlayers.size() <= i)
			{
				isThereNextPage = false;
				break;
			}
			L2PcInstance player = allPlayers.get(i);
			
			if (!isPlayerHavingClass.test(player))
			{
				badCharacters++;
				continue;
			}
			
			html.append("<table bgcolor=").append(getLineColor(i)).append(" width=760 border=0 cellpadding=0 cellspacing=0><tr>");
			html.append("<td width=180 height=25><center><font color=").append(getTextColor(i)).append(">").append(player.getName()).append("</font></center></td>");
			html.append("<td width=130><center><font color=").append(getTextColor(i)).append(">").append(player.getClassId().getName()).append("</ClassId></font></center></td>");
			html.append("<td width=75><center><font color=").append(getTextColor(i)).append(">").append(player.getLevel()).append("</font></center></td>");
			//html.append("<td width=75><center><font color=").append(getTextColor(i)).append(">").append(player.getPartyReason()).append("</font></center></td>");
			//html.append("<td width=75><center><font color=").append(getTextColor(i)).append(">").append((player.isSubClassActive() ? "No" : "Yes")).append("</font></center></td>");
			html.append("<td width=180><center><font color=").append(getTextColor(i)).append(">").append((player.getClan() != null ? player.getClan().getName() : "<br>")).append("</font></center></td>");
			if (!player.isInParty() && !player.equals(visitor))
				html.append("<td width=120><center><button value=\"Invite\" action=\"bypass _bbspartymatching_%class%_%sort%_%asc%_%page%_").append(player.getObjectId()).append("_0\" width=70 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.button_df\"><center></td>");
			else
				html.append("<td width=120><br></td>");
			
			html.append("</tr></table>");
		}
		html.append("<center><table><tr>");
		if (page > 0)
			html.append("<td><button value=\"Prev\" action=\"bypass _bbspartymatching_%class%_%sort%_%asc%_").append((page - 1)).append("_%char%\" width=80 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.button_df\"></td>");
		if (isThereNextPage)
			html.append("<td><button value=\"Next\" action=\"bypass _bbspartymatching_%class%_%sort%_%asc%_").append((page + 1)).append("_%char%\" width=80 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.button_df\"></td>");
		html.append("</tr></table></center>");
		
		return html.toString();
	}
	
	private int getMaxLevel(L2PcInstance player, int classSortType)
	{
		int maxLevel = Util.contains(CLASS_GROUPS[classSortType], player.getBaseClass()) ? player.getLevel() : 0;
		int subMaxLevel = player.getSubClasses().values().stream().filter(sub -> Util.contains(CLASS_GROUPS[classSortType], sub.getClassId())).mapToInt(SubClass::getLevel).max().orElse(0);
		return Math.max(maxLevel, subMaxLevel);
	}
	
	private int getUnlocksSize(L2PcInstance player, int classSortType)
	{
		return player.getSubClasses().size();
	}
	
	private String canJoinParty(L2PcInstance player)
	{
		String name = player.getName();
		if (player.getParty() != null)
			return name + " has already found a party.";
		if (player.getClient() == null || player.getClient().isDetached())
			return name + " is offline.";
		if (player.isInOlympiadMode())
			return name + " is currently fighting in the Olympiad.";
		if (player.inObserverMode())
			return name + " is currently observing an Olympiad Match.";
		if (player.getCursedWeaponEquippedId() != 0)
			return name + " cannot join the party because he is holding a cursed weapon.";
		if (_invisiblePlayers.contains(player.getObjectId()))
			return name + " doesn't want to join any party.";
		if (player.getPrivateStoreType() > 0)
			return name + " cannot join the party because he is currently having a private store.";
		return null;
	}
	
	private String getChecked(int i, int classSortType)
	{
		if (classSortType == i)
			return "L2UI.Checkbox_checked";
		
		return "L2UI.CheckBox";
	}
	
	private String getLineColor(int i)
	{
		if (i % 2 == 0)
			return "18191e";
		
		return "22181a";
	}
	
	private String getTextColor(int i)
	{
		if (i % 2 == 0)
			return "8f3d3f";
		
		return "327b39";
	}
	
	@Override
	public void parsewrite(String bypass, String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
	}
	
	public static PartyBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PartyBBSManager _instance = new PartyBBSManager();
	}
}