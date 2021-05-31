package luna.chill.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import luna.PlayerPassport;
import luna.chill.model.enums.EActionPriority;
import luna.chill.model.enums.EAutoAttack;
import luna.chill.model.enums.EMoveType;
import luna.chill.model.enums.ESearchType;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.IconsTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.ILocational;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;



public class AutoChill implements Runnable
{
	private final long INIT_TICKS = 14 * 3_600_000;
	
	private final PlayerPassport _playerPassport;
	
	
	private boolean _running;
	
	private long _lagTicks;
	
	private long _remainingTicks = INIT_TICKS;
	
	private EAutoAttack _autoAttack;
	private EMoveType _moveType;
	private ESearchType _searchType;
	
	private PlayerPassport _assistPassport;
	private Location _lastSavedLocation;

	private final ChillAction[] _chillSkills = new ChillAction[5];
	private final ChillAction[] _chillItems  = new ChillAction[2];
	

	private final DistanceComperator distanceComperator = new DistanceComperator();
	private final TargetFilter targetFilter = new TargetFilter();
	private final ActionFilter actionFilter = new ActionFilter();
	private final SkillFilter skillFilter = new SkillFilter();
	private final AvailSkillActionFilter availSkillFilter = new AvailSkillActionFilter();
	
	private final ArrayList<Integer> listedMonsters = new ArrayList<Integer>();
	private final ArrayList<Integer> bannedMonsters = new ArrayList<Integer>();
	
	public void setChillAction(int slot, int actionId, boolean isSkill)
	{
		final var chillSlots = isSkill ? _chillSkills : _chillItems;
		
		final int slotsLen = chillSlots.length;
		
		if (slot >= slotsLen)
			return;
		
		chillSlots[slot] = new ChillAction(actionId, isSkill);
	}
	
	public ChillAction getChillAction(int slot, final boolean isSkill)
	{
		final var chillActions = isSkill ? _chillSkills : _chillItems;
		if (slot < 0 || slot >= chillActions.length)
			return null;
		return chillActions[slot];
	}
	
	public boolean swapChillAction(final int slot0, final int slot1, final boolean isSkill)
	{
		final var chillActions = isSkill ? _chillSkills : _chillItems;

		if (slot0 < 0 || slot0 >= chillActions.length)
			return false;
		
		if (slot1 < 0 || slot1 >= chillActions.length)
			return false;
		
		final var chillAction0 = chillActions[slot0];
		chillActions[slot0] = chillActions[slot1];
		chillActions[slot1] = chillAction0;
		
		return true;
	}
	
	public AutoChill(final PlayerPassport playerPassport)
	{
		_playerPassport = playerPassport;
		
		reset();
	}
	
	public void tick(final long ticks)
	{

		final var player = getActivePlayer();
		
		if (_remainingTicks - ticks < 0 || player == null)
		{
			_remainingTicks = 0;
			setRunning(false);
			return;
		}

		_remainingTicks -= ticks;
		
		if (_lagTicks > 0)
		{
			_lagTicks = Math.max(0, _lagTicks - ticks);
			return;
		}
		
		final var oldTarget = player.getTargetChar();
		if (oldTarget != null && oldTarget.isAlikeDead())
		{
			if (player.isCastingNow())
				player.breakCast();
			
			if (player.isAttackingNow())
				player.breakAttack();
			
			player.setTarget(null);
			_lagTicks += 0;
			return;
		}
		
		final var party = player.getParty();
		
		if (party == null)
		{
			if (_moveType == EMoveType.Follow_Target)
			{
				setMoveType(EMoveType.Not_Set);
				render();
			}
			
			if (_searchType == ESearchType.Assist)
			{
				setSearchTarget(ESearchType.Off);
				render();
			}
		}
		
		final var assistPlayer = getAssistPlayer();
		if (assistPlayer != null && !assistPlayer.isSamePartyWith(player))
			_assistPassport = null;
		
		if (_searchType != ESearchType.Off)
		{
			if (_moveType == EMoveType.Not_Set)
				renderRange();
			
			final var currTarget = player.getTargetChar();

			if (currTarget != null && currTarget.isAlikeDead())
			{
				player.setTarget(null);
				return;
			}
			
			if (currTarget == null || !(currTarget instanceof L2MonsterInstance) )
			{
				final var newTarget = searchTarget();
				if (newTarget != null)
				{
//					if (_lastSavedLocation != null &&_moveType == EMoveType.Saved_Location)
//					{
//						final int targetDist = (int) Util.calculateDistance(_lastSavedLocation, newTarget, true);
//						if (targetDist > _searchType.getRange())
//							return;
//					}
					
					player.setTarget(newTarget);
					player.sendPacket(new MyTargetSelected(player, newTarget));
					StatusUpdate su = new StatusUpdate(newTarget.getObjectId());
					su.addAttribute(StatusUpdate.CUR_HP, (int) newTarget.getCurrentHp());
					su.addAttribute(StatusUpdate.MAX_HP, newTarget.getMaxHp());
					player.sendPacket(su);

					player.sendPacket(new ValidateLocation(player));
					_lagTicks += 0;
					return;
				}
			}
		}
		
		final var actualTarget = player.getTargetChar();
		
		if (actualTarget == null)
			return;
		
		if (_autoAttack == EAutoAttack.Always || (player.isAllSkillsDisabled() && _autoAttack == EAutoAttack.Skills_Reuse))
		{
			startAutoAttack(actualTarget);
			return;
		}
		else
		{
			
			
			final var avail = getAvailSkillActions().filter(availSkillFilter).findFirst();
			
			if (avail != null && avail.isPresent())
			{
				final var availAction = avail.get();
				if (availAction != null)
				{
					final var availSkill = SkillTable.getSkill(availAction);
					if (availSkill != null)
					{
						if (player.testDoCastConditions(availSkill))
						{
							player.useMagic(availSkill, false, false);
							availAction.initReuse();
						}
						if(!GeoData.getInstance().canSeeTarget(player, player.getTarget()))
						{
							startAutoAttack(actualTarget);
						}
					}
				}
			}
			else if (_autoAttack == EAutoAttack.Skills_Reuse)
			{
				startAutoAttack(actualTarget);
				return;
			}
		}
		
	}
	
	public void onLogout()
	{
		_running = false;
	}
	
	public void onKill(final L2Character target)
	{
		_lagTicks += 0;
	}
	
	private void startAutoAttack(final L2Character actualTarget)
	{
		final var player = getActivePlayer();
		if (player != null && player.getTarget() instanceof L2MonsterInstance)
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, actualTarget);
		if(!GeoData.getInstance().canSeeTarget(player, player.getTarget()))
		{
			player.setTarget(null);
		}
	}
	
	private class SkillFilter implements Predicate<L2Skill>
	{
		@Override
		public boolean test(L2Skill t)
		{
			return true;
		}
		
	}
	
	private Stream<ChillAction> getAvailSkillActions()
	{
		return Stream.of(_chillSkills).
				filter(actionFilter);
	}
	
	public L2Character searchTarget()
	{
		switch (_searchType)
		{
			case Assist:
				return getTargetByAssist();
		}
		
		return getTargetByRange(_searchType.getRange());
	}
	
	public L2Character getTargetByAssist()
	{
		final var assistPlayer = getAssistPlayer();
		
		if (assistPlayer == null)
			return null;
		
		return assistPlayer.getTargetChar();
	}
	public boolean canInterract(L2PcInstance player, L2Attackable attackable)
	{
		if(bannedMonsters.contains(attackable.getNpcId())	)
		{
			return false;
		}
		return GeoData.getInstance().canMove(player, attackable);
	}
	
	public L2Character getTargetByRange(final int range)
	{
		final var player = getActivePlayer();
		
		return player.getKnownList().getKnownTypeInRadius(L2Attackable.class, range, bannedMonsters).stream().
		filter(L2Attackable::isAlikeAlive).
		filter((attackable) -> {return canInterract(player, attackable)
		;}).
		sorted(new DistanceComperator()).
		findFirst().orElse(null);
	}
//	public L2Character getTargetByRange(final int range)
//	{
//		final var player = getActivePlayer();
//		
//		return player.getKnownList().getKnownTypeInRadius(L2Attackable.class, range).stream().
//		filter(L2Attackable::isAlikeAlive).
//		filter((attackable) -> {return GeoData.getInstance().canMove(player, attackable);}).
//		sorted(new DistanceComperator()).
//		
//		findFirst().orElse(null);
//	}
	public ILocational getSearchLocation()
	{
		final var loc = _moveType == EMoveType.Saved_Location ? _lastSavedLocation : getActivePlayer();
		return loc;
	}
	
	private class TargetFilter implements Predicate<L2Attackable>
	{
		@Override
		public boolean test(L2Attackable target)
		{
			if (target.isAlikeDead())
				return false;
			//if (target.getMaxHp() > 100_000 && !getActivePlayer().isInParty())
			//	return false;

			final var loc = getSearchLocation();
			if (!target.isInsideRadius(loc, _searchType.getRange(), true, true))
				return false;
			
			if (!GeoData.getInstance().canSeeTarget(target, loc))
				return false;
			
			return true;
		}
		
	}
	
	private class ActionFilter implements Predicate<ChillAction>
	{
		@Override
		public boolean test(final ChillAction t)
		{
			if (t == null)
				return false;
			
			if (t.isReuse())
				return false;
			
			final var player = getActivePlayer();
			
			if (player == null)
				return false;
			
			if (!t.isUserHp(player))
				return false;
			
			
			return true;
		}
		
	}
	
	private class AvailSkillActionFilter implements Predicate<ChillAction>
	{
		@Override
		public boolean test(final ChillAction chillAction)
		{
			if (chillAction == null)
				return false;
			
			final var skill = SkillTable.getSkill(chillAction);
			
			if (skill == null)
				return false;
			
			final var player = getActivePlayer();
			
			if (!player.checkDoCastConditions(skill))
				return false;
			
			return true;
		}
		
	}
	
	
	
	private class DistanceComperator implements Comparator<L2Character>
	{
		@Override
		public int compare(L2Character o1, L2Character o2)
		{
			final var loc = getSearchLocation();
			
			final double d1 = Util.calculateDistance(loc, o1, true);
			final double d2 = Util.calculateDistance(loc, o2, true);

			if (d1 > d2)
				return 1;
			return -1;
		}
		
	}

	public L2PcInstance getActivePlayer()
	{
		final var player = _playerPassport.getOnlinePlayer();

		return player;
	}

	public L2PcInstance getAssistPlayer()
	{
		if (_assistPassport == null)
			return null;
		return _assistPassport.getOnlinePlayer();
	}
	
	public void reset()
	{
		_running = false;
		
		_autoAttack = EAutoAttack.Never;
		_searchType = ESearchType.Off;
		_moveType = EMoveType.Not_Set;
		
		_assistPassport = null;
		_lastSavedLocation = null;

		Util.clearArray(_chillSkills);
		Util.clearArray(_chillItems);
		
		renderRange();
	}
	
	
	public void setPartyTarget(final PlayerPassport targetPassport)
	{
		if (targetPassport == _assistPassport || targetPassport == _playerPassport)
			return;
		

		final var player = getActivePlayer();
		
		if (targetPassport != null)
		{
			final var targetPlayer = targetPassport.getPlayer();
			
			if (targetPlayer == null)
				return;

			if (player.isSameHWID(targetPlayer))
			{
				_assistPassport = null;
				player.sendMessage("Same IP chilling is prohibited and a bannable offense.");
				return;
			}
		}
		
		_assistPassport = targetPassport;
		
		if (_assistPassport == null)
		{
			player.sendMessage("ChillMode PartyTarget changed to -> UNSET");
			if (_moveType == EMoveType.Follow_Target)
				setMoveType(EMoveType.Not_Set);
		}
		else
			player.sendMessage("ChillMode PartyTarget changed to -> [" + _assistPassport.getPlayerName() + "]");
			
	}

	public void setAutoAttack(final EAutoAttack autoAttack)
	{
		if (autoAttack == _autoAttack)
			return;
		
		_autoAttack = autoAttack;

		final var player = getActivePlayer();
		player.sendMessage("ChillMode AttackType changed to -> [" + _autoAttack + "]");
	}
	

	public void setMoveType(EMoveType moveType)
	{
		final var player = getActivePlayer();
		
		if (moveType == EMoveType.Current_Location)
		{
			_lastSavedLocation = new Location(player.getLocation());
			player.sendMessage("Updated search location to current position.");
			moveType = EMoveType.Saved_Location;
		}
		
		renderRange();
		
		if (moveType == _moveType)
			return;
		
		_moveType = moveType;

		player.sendMessage("ChillMode MoveType changed to -> [" + _moveType + "]");
	
		renderRange();
	}
	
	public void setSearchTarget(final ESearchType searchType)
	{
		if (searchType == _searchType)
			return;
		
		_searchType = searchType;
		
		final var player = getActivePlayer();
		player.sendMessage("ChillMode SearchType changed to -> [" + _searchType + "]");

		renderRange();
	}
	
//	private static String STOPPED = "<td align=center><button value=\"Start\" action=\"bypass chill_start\" width=80 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td align=center><font name=hs10 color=\"FF6363\">Stopped</font></td>";
//	private static String RUNNING = "<td align=center><font name=hs10 color=\"63FF63\">Running</font></td><td align=center><button value=\"Stop\" action=\"bypass chill_stop\" width=80 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>";
	
	public void render()
	{
		String STOPPED = "<td align=center><button value=\"Start\" action=\"bypass chill_start\" width=70 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td align=center><font name=hs12 color=\"FF6363\">Stopped</font></td>";
		String RUNNING = "<td align=center><font name=hs12 color=\"63FF63\">Running</font></td><td align=center><button value=\"Stop\" action=\"bypass chill_stop\" width=70 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>";

		
		final var player = getActivePlayer();
		if (player != null)
		{
			final var npcHtml = new NpcHtmlMessage(1);
			npcHtml.setFile("data/html/custom/chill/autochill.htm");
			
			//state
			npcHtml.replace("%state%", _running ? RUNNING : STOPPED);
			npcHtml.replace("%attack%", buildAutoAttack());
			npcHtml.replace("%move%", buildMoveType());
			npcHtml.replace("%party%", buildParty());
			
			npcHtml.replace("%search%", buildSearch());
			npcHtml.replace("%time%", buildTime());

			npcHtml.replace("%ask%", buildActions(_chillSkills));
			npcHtml.replace("%ait%", buildActions(_chillItems));
			
			player.sendPacket(npcHtml);
		}
		
	}
	
	public void banMob(int npcId, boolean ban)
	{
		L2NpcTemplate npc = NpcTable.getInstance().getNpcs().get(npcId);

		final var player = getActivePlayer();
		
		if(npc == null)
		{
			player.sendMessage("There was an error with the selected npc");
			return;
		}
		if(ban)
		{
			if(!bannedMonsters.contains(npc))
			{
				bannedMonsters.add(npc.getNpcId());

				player.sendMessage("You have disabled " + npc.getName() +" from your target list.");
			}
		}
		else
		{
			if(bannedMonsters.contains(npcId))
			{
				for (int i = 0; i < bannedMonsters.size(); i++)
				{
					if (bannedMonsters.get(i) == npcId)
					{
						bannedMonsters.remove(i);
						break;
					}
					else
						continue;
				}
				//bannedMonsters.remove(npcId);
				player.sendMessage("You have enabled " + npc.getName() +" on your target list.");
			}
		}
		renderBannableMobs();
	}
	public void renderBannableMobs()
	{
		StringBuilder sb = new StringBuilder(1024);
		
		final var player = getActivePlayer();
		if (player != null)
		{
			if(_searchType == ESearchType.Assist || _searchType == ESearchType.Off)
			{
				player.sendMessage("You need to set a search range.");
				render();
				return;
			}
			
			final var npcHtml = new NpcHtmlMessage(1);
			npcHtml.setFile("data/html/custom/chill/autochillmobs.htm");
			listedMonsters.clear();
			for(L2Npc npc : player.getKnownList().getKnownMonsterssInRadius(_searchType.getRange()))
			{
				if(npc instanceof L2MonsterInstance)
				{
					if (listedMonsters.contains(npc.getNpcId()))
						continue;
					else
					{
						listedMonsters.add(npc.getNpcId());	
					}
				}
			}
			listedMonsters.stream().forEach(mob -> {
				sb.append("<tr>"+
				   "<td align=left>"+
					 String.format("<button value=\"\" action=\"%s\" width=32 height=32 back=\"L2UI_ct1.MiniMap_DF_%s_over\" fore=\"L2UI_ct1.MiniMap_DF_%s\">", bannedMonsters.contains(mob) ? "bypass chill_unban_mob "  + mob : "bypass chill_ban_mob "  + mob, bannedMonsters.contains(mob) ? "PlusBtn_Blue" : "MinusBtn_Red", bannedMonsters.contains(mob) ? "PlusBtn_Blue" : "MinusBtn_Red" )+
				   "</td>"+
				   "<td align=left>"+
				   		String.format("<font name=\"hs15\" color=\"%s\">"+ NpcTable.getInstance().getTemplate(mob).getName() +" </font>", bannedMonsters.contains(mob) ? "863737" : "2E807C") +
				   "</td>"+
				"</tr>");
				
			});
			
			//state
			npcHtml.replace("%npcs%", sb.toString());
			
			player.sendPacket(npcHtml);
		}
		
	}
	
	private String buildTime()
	{
		final long hours = _remainingTicks / 3_600_000;
		final long minutes = (_remainingTicks - (3_600_000 * hours)) / 60_000;
		final long seconds = _remainingTicks - hours * 3_600_000 - minutes * 60_000;
		return String.format("%02d Hours %02d Minutes %02d Seconds", hours, minutes, seconds / 1000);
	}

	private String buildAutoAttack()
	{
		final var eattackTypes = EAutoAttack.values();
		final List<EAutoAttack> attackTypes = new ArrayList<>(eattackTypes.length);

		for (final var eattackType : eattackTypes) if (eattackType != _autoAttack)
			attackTypes.add(eattackType);
		
		String ret = _autoAttack.toString();
		for (final var attackType : attackTypes)
			ret += ";" + attackType;
		
		return ret;
	}
	

	private String buildMoveType()
	{
		final var emoveTypes = EMoveType.values();
		final List<EMoveType> moveTypes = new ArrayList<>(emoveTypes.length);

		for (final var emoveType : emoveTypes) if (emoveType != _moveType)
			moveTypes.add(emoveType);
		
		String ret = _moveType.toString();
		for (final var moveType : moveTypes)
		{
			if (moveType == EMoveType.Saved_Location && _lastSavedLocation == null)
				continue;
			
			if (moveType != EMoveType.Follow_Target || _assistPassport != null)
				ret += ";" + moveType;
		}

		ret = ret.replace("Target", _assistPassport == null ? "Target" : _assistPassport.getPlayerName());
		
		return ret;
	}
	
	private String buildParty()
	{
		final var player = getActivePlayer();
		
		final var party = player.getParty();
		if (party == null)
			return "Not Set";
		
		
		String ret = _assistPassport == null ? "Not Set" : _assistPassport.getPlayerName() + ";Not Set";
		
		for (final var member : party.getPartyMembers()) 
		{
			final var memberPassport = member.getPassport();
			if (memberPassport != _assistPassport && memberPassport != _playerPassport)
				ret += ";" + member.getName();
		}
		
		return ret;
		
	}
	
	private String buildSearch()
	{
		final String search = "<td align=center width=50><button value=\"%s\" action=\"bypass chill_search_type %s\" width=62 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>";
		final String searcs = "<td align=center width=50><font name=hs12 color=\"%s\">%s</font></td>";
		final StringBuilder sb = new StringBuilder(512);
		
		for (final var esearch : ESearchType.values())
		{
			if (esearch == _searchType)
				sb.append(String.format(searcs, esearch.getColor(), esearch.toString(), esearch.toString()));
			else
				sb.append(String.format(search, esearch.toString(), esearch.toString()));
		}
		
		return sb.toString();
	}
	
	private static final String actionTemplate = "<td align=center width=50><table height=34 cellspacing=0 cellpadding=0 background=%s><tr><td><table cellspacing=0 cellpadding=0><tr><td><button action=\"bypass chill_action_edit %s\" width=34 height=34 back=L2UI_CH3.menu_outline_Down fore=L2UI_CH3.menu_outline></td></tr></table></td></tr></table></td>";
	private static final String actionTemplate2 = "<td align=center width=50><table height=34 cellspacing=0 cellpadding=0><tr><td><table cellspacing=0 cellpadding=0><tr><td><button action=\"bypass chill_action_edit %s\" width=32 height=32 back=%s fore=%s></td></tr></table></td></tr></table></td>";

	private String buildActions(final ChillAction[] chillActions)
	{
		
		final StringBuilder sb = new StringBuilder(1024);
		
		int aid = 0;
		for (final var chillAction : chillActions)
		{
			if (chillAction != null)
				sb.append(String.format(actionTemplate2, String.valueOf(aid++), chillAction.getIcon(), chillAction.getIcon()));
			else
				sb.append(String.format(actionTemplate2, String.valueOf(aid++), "L2UI_CT1.Inventory_DF_CloakSlot_Disable", "L2UI_CT1.Inventory_DF_CloakSlot_Disable"));
		}
		
		return sb.toString();
	}
	
	public void renderActionEdit(final int slot, final int page)
	{
		final int SKILLS_PER_PAGE = 9;
		
		final var npcHtml = new NpcHtmlMessage(1);
		npcHtml.setFile("data/html/custom/chill/actionedit.htm");
		
		final StringBuilder sb = new StringBuilder();

		npcHtml.replace("%tit%", "Chill Action " + slot);
		
		final var player = getActivePlayer();

		final ArrayList<L2Skill> availSkills = new ArrayList<>();

		for (final var skill : player.getAllSkills())
		{
			if (skill.isActive() && !skill.isNoble() && skill.isChillAllow())
				availSkills.add(skill);
		}
		
		final int skillsLen = availSkills.size();
		
		for (int i = 0; i < SKILLS_PER_PAGE; i++)
		{
			final int indx = SKILLS_PER_PAGE * page + i;
			if (indx < skillsLen)
			{
				final var skill = availSkills.get(indx);
				sb.append(String.format(actionTemplate2.replace("chill_action_edit", "chill_action_set"), slot + " " + skill.getId(), skill.getIcon(), skill.getIcon()));
				//sb.append(String.format(actionTemplate.replace("chill_action_edit", "chill_action_set"), skill.getIcon(), slot + " " + skill.getId()));
			}
			
		}
		
		npcHtml.replace("%ask%", sb.toString());
		
		
		
		
		//pages

		final int pages = skillsLen < SKILLS_PER_PAGE ? 1 : skillsLen / SKILLS_PER_PAGE + ((skillsLen % SKILLS_PER_PAGE) > 0 ? 1 : 0);
		
		sb.setLength(0);
		
		for (int i = 0; i < pages; i++)
		{
			if (page == i)
				sb.append(String.format("<td align=center>Page %d</td>", i+1));
			else
				sb.append(String.format("<td align=center><a action=\"bypass chill_action_edit %d %d\">Page %d</a></td>", slot, i, i+1));
		}
		

		npcHtml.replace("%pages1%", sb.toString());
		
		//
		
		final var action = _chillSkills[slot];
		
		if (action != null)
		{
			final var skill = SkillTable.getInstance().getInfoLevelMax(action.getActionId());
			npcHtml.replace("%sic%", skill.getIcon());
			npcHtml.replace("%sna%", skill.getName());
			
			npcHtml.replace("%reu%", String.format("%.2fs", action.getReuse()));
			npcHtml.replace("%hpp%", String.format("%05.2f%%", action.getUserHp()));
			
			final var epriorities = EActionPriority.values();
			final var priority = epriorities[slot];

			String spr = priority.toString();
			for (final var pr : epriorities) if (pr != priority)
				spr += ";" + pr.toString();
			
			npcHtml.replace("%pr%", spr);
		}
		else
		{
			npcHtml.replace("%sic%", "L2UI_CT1.Inventory_DF_CloakSlot_Disable");
			npcHtml.replace("%sna%", "Empty");

			npcHtml.replace("%reu%", "?");
			npcHtml.replace("%hpp%", "?");
			npcHtml.replace("%pr%", "");
		}

		npcHtml.replace("%priority%", "" + slot+1);
		npcHtml.replace("%slot%", "" +  slot);

		
		player.sendPacket(npcHtml);
	}
	
	private void renderRange()
	{
		final var player = getActivePlayer();
		
		final int searchRange = _searchType.getRange();
		
		final ILocational renderLoc = _moveType == EMoveType.Saved_Location ? _lastSavedLocation : player;
		
		final ExServerPrimitive renderRange = new ExServerPrimitive("SearchRange", new Location(player.getX(), player.getY(), player.getZ() - 200));
		if ((_moveType != EMoveType.Follow_Target) && (searchRange > 1))
		{
			final var color = _running ? Color.GREEN : Color.RED;
			renderRange.addCircle(color, searchRange, 30, -20);
			renderRange.addCircle(color, 5, 4, -20);
		}
		else
			renderRange.addCircle(Color.GREEN, 1, 1, -5);

		player.sendPacket(renderRange);
	}

	public boolean isRunning()
	{
		return _running;
	}
	
	public void setRunning(final boolean running)
	{
		_running = running;
		renderRange();
	}
	
	@Override
	public void run()
	{
	}
	
	public L2Character getTargetAssist()
	{
		return null;
	}
	
	public L2Character getTargetRange(final int range)
	{
		return null;
	}
	
	public static class ChillAction
	{
		private final int _actionId;
		
		private final boolean _isSkill;
		
		private double _userHp = 100;
		
		private long _reuse;
		
		private long _lastUse;
		
		public ChillAction(final int actionId, final boolean isSkill)
		{
			_actionId = actionId;
			_isSkill = isSkill;
		}
		
		public int getActionId()
		{
			return _actionId;
		}

		public String getIcon()
		{
			return _isSkill ? IconsTable.getInstance().getSkillIcon(_actionId) : IconsTable.getInstance().getItemIcon(_actionId);
		}
		
		public boolean isReuse()
		{
			return _lastUse + _reuse > System.currentTimeMillis();
		}
		
		public boolean isUserHp(final L2PcInstance player)
		{
			return player.getHpPercent() <= _userHp;
		}
		
		public double getUserHp()
		{
			return _userHp;
		}
		
		public void initReuse()
		{
			_lastUse = System.currentTimeMillis();
		}
		
		public double getReuse()
		{
			return _reuse / 1000d;
		}
		
		public void setReuse(final double reuseSec)
		{
			_reuse = (long) (reuseSec * 1000L);
		}
		
		public void setUserHP(final double userHp)
		{
			_userHp = userHp;
		}
		
		public boolean isSkill()
		{
			return _isSkill;
		}
	}
	
	private static class ActionPair
	{
		
	}
}
