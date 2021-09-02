//package luna.chill.model;
//
//import java.awt.Color;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//import java.util.function.Predicate;
//import java.util.stream.Stream;
//
//import gnu.trove.set.hash.TIntHashSet;
//import luna.PlayerPassport;
//import luna.chill.model.enums.EActionPriority;
//import luna.chill.model.enums.EAutoAttack;
//import luna.chill.model.enums.EMoveType;
//import luna.chill.model.enums.EPanelOptions;
//import luna.chill.model.enums.ESearchType;
//import net.sf.l2j.Config;
//import net.sf.l2j.gameserver.GeoData;
//import net.sf.l2j.gameserver.ai.CtrlIntention;
//import net.sf.l2j.gameserver.datatables.IconsTable;
//import net.sf.l2j.gameserver.datatables.NpcTable;
//import net.sf.l2j.gameserver.datatables.SkillTable;
//import net.sf.l2j.gameserver.model.ILocational;
//import net.sf.l2j.gameserver.model.L2Skill;
//import net.sf.l2j.gameserver.model.Location;
//import net.sf.l2j.gameserver.model.actor.L2Attackable;
//import net.sf.l2j.gameserver.model.actor.L2Character;
//import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
//import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
//import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;
//import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
//import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
//import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
//import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
//import net.sf.l2j.gameserver.util.Util;
//
//
//
//public class AutoChill implements Runnable
//{
//	private final long INIT_TICKS = 4 * 3_600_000;
//	
//	private final PlayerPassport _playerPassport;
//	
//	
//	private boolean _running;
//	
//	private long _lagTicks;
//	
//	private long _remainingTicks = INIT_TICKS;
//	
//	private EAutoAttack _autoAttack;
//	private EMoveType _moveType;
//	private ESearchType _searchType;
//	
//	private PlayerPassport _assistPassport;
//	private Location _lastSavedLocation;
//
//	private final ChillAction[] _chillSkills = new ChillAction[5];
//	private final ChillAction[] _chillItems  = new ChillAction[2];
//	
//
//	private final TargetComparator targetComperator = new TargetComparator();
//	private final TargetFilter targetFilter = new TargetFilter();
//	private final VTargetFilter vtargetFilter = new VTargetFilter();
//	private final ActionFilter actionFilter = new ActionFilter();
//
//	private final TIntHashSet _filteredNpcIds = new TIntHashSet();
//	
//	private final AvailSkillActionFilter availSkillFilter = new AvailSkillActionFilter();
//	
//	public void setChillAction(int slot, int actionId, boolean isSkill)
//	{
//		final var chillSlots = isSkill ? _chillSkills : _chillItems;
//		
//		final int slotsLen = chillSlots.length;
//		
//		if (slot >= slotsLen)
//			return;
//		
//		chillSlots[slot] = new ChillAction(actionId, isSkill);
//	}
//	
//	public ChillAction getChillAction(int slot, final boolean isSkill)
//	{
//		final var chillActions = isSkill ? _chillSkills : _chillItems;
//		if (slot < 0 || slot >= chillActions.length)
//			return null;
//		return chillActions[slot];
//	}
//	
//	public boolean swapChillAction(final int slot0, final int slot1, final boolean isSkill)
//	{
//		final var chillActions = isSkill ? _chillSkills : _chillItems;
//
//		if (slot0 < 0 || slot0 >= chillActions.length)
//			return false;
//		
//		if (slot1 < 0 || slot1 >= chillActions.length)
//			return false;
//		
//		final var chillAction0 = chillActions[slot0];
//		chillActions[slot0] = chillActions[slot1];
//		chillActions[slot1] = chillAction0;
//		
//		return true;
//	}
//	
//	public void deleteChillAction(final int slot0, final boolean isSkill)
//	{
//		final var chillActions = isSkill ? _chillSkills : _chillItems;
//		chillActions[slot0] = null;
//	}
//	
//	public AutoChill(final PlayerPassport playerPassport, final long remainingTicks)
//	{
//		_playerPassport = playerPassport;
//		if (remainingTicks > 0)
//			_remainingTicks = remainingTicks;
//		else
//			_remainingTicks = INIT_TICKS;
//		reset();
//	}
//	
//	public void addCredit(final long ticks)
//	{
//		_remainingTicks += ticks;
//	}
//	
//	public long getCredit()
//	{
//		return _remainingTicks;
//	}
//	
//	public void tick(final long ticks)
//	{
//
//		final var player = getActivePlayer();
//		
//		if (_remainingTicks - ticks < 0 || player == null || player.isDead())
//		{
//			_remainingTicks = 0;
//			setRunning(false);
//			render();
//			return;
//		}
//
//		_remainingTicks -= ticks;
//		
//		if (_lagTicks > 0)
//		{
//			_lagTicks = Math.max(0, _lagTicks - ticks);
//			return;
//		}
//		
//		final var oldTarget = player.getTargetChar();
//		if (oldTarget != null && oldTarget.isAlikeDead())
//		{
//			if (player.isCastingNow())
//				player.breakCast();
//			
//			if (player.isAttackingNow())
//				player.breakAttack();
//			
//			player.setTarget(null);
//			_lagTicks += Config.LAG_DIE_TARGET;
//			return;
//		}
//		
//		final var party = player.getParty();
//		
//		final var assistPlayer = getAssistPlayer();
//		if (assistPlayer != null && !assistPlayer.isSamePartyWith(player))
//			_assistPassport = null;
//		
//		if (party == null || _assistPassport == null)
//		{
//			boolean render = false;
//			if (_moveType == EMoveType.Follow_Target)
//			{
//				setMoveType(EMoveType.Not_Set);
//				render = true;
//			}
//			
//			if (_searchType == ESearchType.Assist)
//			{
//				setSearchTarget(ESearchType.Off);
//				render = true;
//			}
//			
//			if (render)
//				render();
//		}
//
//		final var currTarget = player.getTargetChar();
//		
//		if (_searchType != ESearchType.Off)
//		{
//			if (_moveType == EMoveType.Not_Set)
//				renderRange();
//			
//
//			if (currTarget != null && currTarget.isAlikeDead())
//			{
//				player.setTarget(null);
//				return;
//			}
//			
//			else if (currTarget == null || !(currTarget instanceof L2MonsterInstance))
//			{
//				final var newTarget = searchTarget();
//				if (newTarget != null)
//				{
//					
//					player.setTarget(newTarget);
//					player.sendPacket(new MyTargetSelected(player, newTarget));
//					StatusUpdate su = new StatusUpdate(newTarget.getObjectId());
//					su.addAttribute(StatusUpdate.CUR_HP, (int) newTarget.getCurrentHp());
//					su.addAttribute(StatusUpdate.MAX_HP, newTarget.getMaxHp());
//					player.sendPacket(su);
//
//					player.sendPacket(new ValidateLocation(player));
//					_lagTicks += Config.LAG_NEW_TARGET;
//					return;
//				}
//			}
//		}
//		else if (assistPlayer != null)
//		{
//			if (_moveType == EMoveType.Follow_Target && !player.isMoving())  
//			{
//				if (player.isInsideRadius(assistPlayer, 500, false) || player.isInCombat())
//				{
//					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
//				}
//				else
//				{
//					player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, assistPlayer);
//					return;
//				}
//				
//			}
//			
//			if (currTarget == null)
//			{
//				player.setTarget(assistPlayer);
//				player.sendPacket(new MyTargetSelected(player, assistPlayer));
//				_lagTicks += Config.LAG_ASI_TARGET;
//			}
//		}
//		
//		final var actualTarget = player.getTargetChar();
//		
//		if (actualTarget == null)
//			return;
//		
//		final var playerTarget = actualTarget.getActingPlayer();
//		if (playerTarget != null && playerTarget.isSameHWID(player))
//		{
//			player.sendMessage("Its not allowed to auto chill targets from the same IP!");
//			setRunning(false);
//			render();
//		}
//		
//		if (_autoAttack == EAutoAttack.Always || (player.isAllSkillsDisabled() && _autoAttack == EAutoAttack.Skills_Reuse))
//		{
//			startAutoAttack(actualTarget);
//			return;
//		}
//		else
//		{
//			final var avail = getAvailSkillActions().filter(availSkillFilter).findFirst();
//			
//			if (avail != null && avail.isPresent())
//			{
//				final var availAction = avail.get();
//				if (availAction != null)
//				{
//					final var availSkill = SkillTable.getSkill(availAction);
//					if (availSkill != null)
//					{
//						if (player.testDoCastConditions(availSkill))
//						{
//							player.useMagic(availSkill, false, false);
//							availAction.initReuse();
//						}
//					}
//				}
//			}
//			else if (_autoAttack == EAutoAttack.Skills_Reuse)
//			{
//				startAutoAttack(actualTarget);
//				return;
//			}
//		}
//		
//	}
//	
//	public void onLogout()
//	{
//		_running = false;
//	}
//	
//	public void onKill(final L2Character target)
//	{
//		_lagTicks += Config.LAG_KIL_TARGET;
//	}
//	
//	private void startAutoAttack(final L2Character actualTarget)
//	{
//		final var player = getActivePlayer();
//		if (player != null && player.getTarget() instanceof L2MonsterInstance)
//			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, actualTarget);
//	}
//	
//	private Stream<ChillAction> getAvailSkillActions()
//	{
//		return Stream.of(_chillSkills).
//				filter(actionFilter);
//	}
//	
//	public L2Character searchTarget()
//	{
//		switch (_searchType)
//		{
//			case Assist:
//				return getTargetByAssist();
//		}
//		
//		return getTargetByRange(_searchType.getRange());
//	}
//	
//	public L2Character getTargetByAssist()
//	{
//		final var assistPlayer = getAssistPlayer();
//		
//		if (assistPlayer == null)
//			return null;
//		
//		return assistPlayer.getTargetChar();
//	}
//
//	public L2Character getTargetByRange(final int range)
//	{
//		final var player = getActivePlayer();
//		
//		return player.getKnownList().getKnownType(L2Attackable.class).stream().
//		filter(targetFilter).
//		sorted(targetComperator).
//		
//		findFirst().orElse(null);
//	}
//	
//	public ILocational getSearchLocation()
//	{
//		final var loc = _moveType == EMoveType.Saved_Location ? _lastSavedLocation : getActivePlayer();
//		return loc;
//	}
//	
//	private class TargetFilter implements Predicate<L2Attackable>
//	{
//		
//		@Override
//		public boolean test(L2Attackable target)
//		{
//			if (target.isAlikeDead())
//				return false;
//			
//			if (!target.isAutoAttackable(getActivePlayer()))
//				return false;
//			
//			if (_filteredNpcIds.contains(target.getNpcId()))
//				return false;
//			
//			final var loc = getSearchLocation();
//			if (!target.isInsideRadius(loc, _searchType.getRange(), true, true))
//				return false;
//			
//			if (!GeoData.getInstance().canSeeTarget(target, loc))
//				return false;
//			
//			return true;
//		}
//		
//	}
//	
//	private class VTargetFilter implements Predicate<L2Attackable>
//	{
//		
//		@Override
//		public boolean test(L2Attackable target)
//		{
//			if (target.isAlikeDead())
//				return false;
//			
//			if (!target.isAutoAttackable(getActivePlayer()))
//				return false;
//			
//			return true;
//		}
//		
//	}
//	
//	private class ActionFilter implements Predicate<ChillAction>
//	{
//		@Override
//		public boolean test(final ChillAction t)
//		{
//			if (t == null)
//				return false;
//			
//			if (t.isReuse())
//				return false;
//			
//			final var player = getActivePlayer();
//			
//			if (player == null)
//				return false;
//			
//			if (!t.isUserHp(player))
//				return false;
//			
//			final var targetPlayer = player.getTargetChar();
//			if (targetPlayer != null && !t.isTargetHp(targetPlayer))
//				return false;
//			
//			return true;
//		}
//		
//	}
//	
//	private class AvailSkillActionFilter implements Predicate<ChillAction>
//	{
//		@Override
//		public boolean test(final ChillAction chillAction)
//		{
//			if (chillAction == null)
//				return false;
//			
//			final var skill = SkillTable.getSkill(chillAction);
//			
//			if (skill == null)
//				return false;
//			
//			final var player = getActivePlayer();
//			
//			if (!player.testDoCastConditions(skill))
//				return false;
//			
//			return true;
//		}
//		
//	}
//	
//	
//	
//	private class TargetComparator implements Comparator<L2Character>
//	{
//		@Override
//		public int compare(L2Character o1, L2Character o2)
//		{
//			final var loc = getSearchLocation();
//			
//			final double d1 = Util.calculateDistance(loc, o1, true);
//			final double d2 = Util.calculateDistance(loc, o2, true);
//
//			if (d1 > d2)
//				return 1;
//			return -1;
//		}
//	}
//
//	public L2PcInstance getActivePlayer()
//	{
//		final var player = _playerPassport.getOnlinePlayer();
//
//		return player;
//	}
//
//	public L2PcInstance getAssistPlayer()
//	{
//		if (_assistPassport == null)
//			return null;
//		return _assistPassport.getOnlinePlayer();
//	}
//	
//	public void reset()
//	{
//		_running = false;
//		
//		_autoAttack = EAutoAttack.Never;
//		_searchType = ESearchType.Off;
//		_moveType = EMoveType.Not_Set;
//		
//		_assistPassport = null;
//		_lastSavedLocation = null;
//
//		Util.clearArray(_chillSkills);
//		Util.clearArray(_chillItems);
//		
//		renderRange();
//	}
//	
//	
//	public void setPartyTarget(final PlayerPassport targetPassport)
//	{
//		if (targetPassport == _assistPassport || targetPassport == _playerPassport)
//			return;
//		
//
//		final var player = getActivePlayer();
//		
//		if (targetPassport != null)
//		{
//			final var targetPlayer = targetPassport.getPlayer();
//			
//			if (targetPlayer == null)
//				return;
//
//			if (player.isSameHWID(targetPlayer))
//			{
//				_assistPassport = null;
//				player.sendMessage("Same IP chilling is prohibited and a bannable offense.");
//				return;
//			}
//		}
//		
//		_assistPassport = targetPassport;
//		
//		if (_assistPassport == null)
//		{
//			player.sendMessage("ChillMode PartyTarget changed to -> UNSET");
//			if (_moveType == EMoveType.Follow_Target)
//				setMoveType(EMoveType.Not_Set);
//		}
//		else
//			player.sendMessage("ChillMode PartyTarget changed to -> [" + _assistPassport.getPlayerName() + "]");
//			
//	}
//
//	public void setAutoAttack(final EAutoAttack autoAttack)
//	{
//		if (autoAttack == _autoAttack)
//			return;
//		
//		_autoAttack = autoAttack;
//
//		final var player = getActivePlayer();
//		player.sendMessage("ChillMode AttackType changed to -> [" + _autoAttack + "]");
//	}
//	
//
//	public void setMoveType(EMoveType moveType)
//	{
//		final var player = getActivePlayer();
//		
//		if (moveType == EMoveType.Current_Location)
//		{
//			_lastSavedLocation = new Location(player.getLocation());
//			player.sendMessage("Updated search location to current position.");
//			moveType = EMoveType.Saved_Location;
//		}
//		
//		renderRange();
//		
//		if (moveType == _moveType)
//			return;
//		
//		_moveType = moveType;
//
//		player.sendMessage("ChillMode MoveType changed to -> [" + _moveType + "]");
//	
//		renderRange();
//	}
//	
//	public void setSearchTarget(final ESearchType searchType)
//	{
//		if (searchType == _searchType)
//			return;
//		
//		_searchType = searchType;
//		
//		final var player = getActivePlayer();
//		player.sendMessage("ChillMode SearchType changed to -> [" + _searchType + "]");
//
//		renderRange();
//	}
//	
//	private static final String STOPPED = "<td align=center><button value=\"Start\" action=\"bypass chill_start\" width=70 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td align=center><font name=hs12 color=\"FF6363\">Stopped</font></td>";
//	private static final String RUNNING = "<td align=center><font name=hs12 color=\"63FF63\">Running</font></td><td align=center><button value=\"Stop\" action=\"bypass chill_stop\" width=70 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>";
//
//	
//	public void render()
//	{
//		final var player = getActivePlayer();
//		if (player != null)
//		{
//			final var npcHtml = new NpcHtmlMessage(1);
//			npcHtml.setFile("data/html/custom/chill/autochill.htm");
//			
//			//state
//			npcHtml.replace("%state%", _running ? RUNNING : STOPPED);
//			npcHtml.replace("%attack%", buildAutoAttack());
//			npcHtml.replace("%move%", buildMoveType());
//			npcHtml.replace("%party%", buildParty());
//			npcHtml.replace("%opt%", buildOptions());
//			
//			npcHtml.replace("%search%", buildSearch());
//			npcHtml.replace("%time%", buildTime());
//
//			npcHtml.replace("%ask%", buildActions(_chillSkills));
//			npcHtml.replace("%ait%", buildActions(_chillItems));
//			
//			player.sendPacket(npcHtml);
//		}
//		
//	}
//	
//	public void renderTargetFilter()
//	{
//		final String filteredNpcTemplate = "<tr><td><button value=\"\" action=\"bypass chill_filter_target %d\" width=32 height=32 back=\"L2UI_ct1.MiniMap_DF_%s_over\" fore=\"L2UI_ct1.MiniMap_DF_%s\"></td><td><font name=\"hs12\" color=\"%s\">Lv.%d %s</font></td></tr>";
//		
//		final var player = getActivePlayer();
//		if (player == null)
//			return;
//		
//		final var npcHtml = new NpcHtmlMessage(1);
//		npcHtml.setFile("data/html/custom/chill/chillfilter.htm");
//
//		final int range = (_searchType.getRange() < 2 ? 2000 : _searchType.getRange()) / 2;
//		
//		final int[] closeNpcIds = player.getKnownList().getKnownTypeInRadius(L2Attackable.class, range)
//		.stream()
//		.filter(vtargetFilter)
////		.sorted(targetComperator)
//		.mapToInt(L2Attackable::getNpcId)
//		.sorted()
//		.distinct()
//		.toArray();
//		
//		final StringBuilder sb = new StringBuilder(2048);
//		
//		for (final var closeNpcId : closeNpcIds)
//		{
//			final var npcTemplate = NpcTable.getInstance().getTemplate(closeNpcId);
//			if (npcTemplate == null)
//				continue;
//			
//			final boolean isFiltered = _filteredNpcIds.contains(closeNpcId);
//			sb.append(String.format(filteredNpcTemplate, npcTemplate.getNpcId(), isFiltered ? "PlusBtn_Blue" : "MinusBtn_Blue", isFiltered ? "PlusBtn_Blue" : "MinusBtn_Blue", isFiltered ? "863737" : "2E807C", npcTemplate.getLevel(), npcTemplate.getName()));
//		}
//		
//		if (closeNpcIds.length > 11)
//			npcHtml.replace("noscrollbar", "");
//
//		npcHtml.replace("%filt%", sb.toString());
//		npcHtml.replace("%r%", String.valueOf(range));
//		
//		player.sendPacket(npcHtml);
//	}
//	
//	public void toggleFilteredTarget(final int npcTemplateId)
//	{
//		final var npcTemplate = NpcTable.getInstance().getTemplate(npcTemplateId);
//		if (npcTemplate != null)
//		{
//			if (_filteredNpcIds.contains(npcTemplateId))
//				_filteredNpcIds.remove(npcTemplateId);
//			else
//				_filteredNpcIds.add(npcTemplateId);
//			
//			renderTargetFilter();
//		}
//	}
//
//	private String buildTime()
//	{
//		final long hours = _remainingTicks / 3_600_000;
//		final long minutes = (_remainingTicks - (3_600_000 * hours)) / 60_000;
//		final long seconds = _remainingTicks - hours * 3_600_000 - minutes * 60_000;
//		return String.format("%02d Hours %02d Minutes %02d Seconds", hours, minutes, seconds / 1000);
//	}
//
//	private String buildAutoAttack()
//	{
//		final var eattackTypes = EAutoAttack.values();
//		final List<EAutoAttack> attackTypes = new ArrayList<>(eattackTypes.length);
//
//		for (final var eattackType : eattackTypes) if (eattackType != _autoAttack)
//			attackTypes.add(eattackType);
//		
//		String ret = _autoAttack.toString();
//		for (final var attackType : attackTypes)
//			ret += ";" + attackType;
//		
//		return ret;
//	}
//	
//
//	private String buildMoveType()
//	{
//		final var emoveTypes = EMoveType.values();
//		final List<EMoveType> moveTypes = new ArrayList<>(emoveTypes.length);
//
//		for (final var emoveType : emoveTypes) if (emoveType != _moveType)
//			moveTypes.add(emoveType);
//		
//		String ret = _moveType.toString();
//		for (final var moveType : moveTypes)
//		{
//			if (moveType == EMoveType.Saved_Location && _lastSavedLocation == null)
//				continue;
//			
//			if (moveType != EMoveType.Follow_Target || _assistPassport != null)
//				ret += ";" + moveType;
//		}
//
//		ret = ret.replace("Target", _assistPassport == null ? "Target" : _assistPassport.getPlayerName());
//		
//		return ret;
//	}
//	
//	private String buildParty()
//	{
//		final var player = getActivePlayer();
//		
//		final var party = player.getParty();
//		if (party == null)
//			return "Not Set";
//		
//		
//		String ret = _assistPassport == null ? "Not Set" : _assistPassport.getPlayerName() + ";Not Set";
//		
//		for (final var member : party.getPartyMembers()) 
//		{
//			final var memberPassport = member.getPassport();
//			if (memberPassport != _assistPassport && memberPassport != _playerPassport)
//				ret += ";" + member.getName();
//		}
//		
//		return ret;
//		
//	}
//	
//	private String buildSearch()
//	{
//		final String search = "<td align=center width=50><button value=\"%s\" action=\"bypass chill_search_type %s\" width=62 height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>";
//		final String searcs = "<td align=center width=50><font name=hs12 color=\"%s\">%s</font></td>";
//		final StringBuilder sb = new StringBuilder(512);
//		
//		for (final var esearch : ESearchType.values())
//		{
//			if (esearch == _searchType)
//				sb.append(String.format(searcs, esearch.getColor(), esearch.toString(), esearch.toString()));
//			else
//				sb.append(String.format(search, esearch.toString(), esearch.toString()));
//		}
//		
//		return sb.toString();
//	}
//	
//	private String buildOptions()
//	{
//		String opts = "";
//		for (final var opt : EPanelOptions.values())
//			opts += opt.toString();
//		return opts;
//	}
//	
//	private static final String actionTemplate = "<td align=center width=50><table height=34 cellspacing=0 cellpadding=0 background=%s><tr><td><table cellspacing=0 cellpadding=0><tr><td><button action=\"bypass chill_action_edit %s\" width=34 height=34 back=L2UI_CH3.menu_outline_Down fore=L2UI_CH3.menu_outline></td></tr></table></td></tr></table></td>";
//
//	private String buildActions(final ChillAction[] chillActions)
//	{
//		
//		final StringBuilder sb = new StringBuilder(1024);
//		
//		int aid = 0;
//		for (final var chillAction : chillActions)
//		{
//			if (chillAction != null)
//				sb.append(String.format(actionTemplate, chillAction.getIcon(), String.valueOf(aid++)));
//			else
//				sb.append(String.format(actionTemplate, "L2UI_CT1.Inventory_DF_CloakSlot_Disable", String.valueOf(aid++)));
//		}
//		
//		return sb.toString();
//	}
//	
//	public void renderActionEdit(final int slot, final int page)
//	{
//		final int SKILLS_PER_PAGE = 9;
//		
//		final var npcHtml = new NpcHtmlMessage(1);
//		npcHtml.setFile("data/html/custom/chill/actionedit.htm");
//		
//		final StringBuilder sb = new StringBuilder();
//
//		npcHtml.replace("%tit%", "Chill Action " + slot);
//		
//		final var player = getActivePlayer();
//
//		final ArrayList<L2Skill> availSkills = new ArrayList<>();
//
//		for (final var skill : player.getAllSkills())
//		{
//			if (skill.isActive() && !skill.isNoble() && skill.isChillAllow())
//				availSkills.add(skill);
//		}
//		
//		final int skillsLen = availSkills.size();
//		
//		for (int i = 0; i < SKILLS_PER_PAGE; i++)
//		{
//			final int indx = SKILLS_PER_PAGE * page + i;
//			if (indx < skillsLen)
//			{
//				final var skill = availSkills.get(indx);
//				sb.append(String.format(actionTemplate.replace("chill_action_edit", "chill_action_set"), skill.getIcon(), slot + " " + skill.getId()));
//			}
//			
//		}
//		
//		npcHtml.replace("%ask%", sb.toString());
//		
//		
//		
//		
//		//pages
//
//		final int pages = skillsLen < SKILLS_PER_PAGE ? 1 : skillsLen / SKILLS_PER_PAGE + ((skillsLen % SKILLS_PER_PAGE) > 0 ? 1 : 0);
//		
//		sb.setLength(0);
//		
//		for (int i = 0; i < pages; i++)
//		{
//			if (page == i)
//				sb.append(String.format("<td align=center>Page %d</td>", i+1));
//			else
//				sb.append(String.format("<td align=center><a action=\"bypass chill_action_edit %d %d\">Page %d</a></td>", slot, i, i+1));
//		}
//		
//
//		npcHtml.replace("%pages1%", sb.toString());
//		
//		//
//		
//		final var action = _chillSkills[slot];
//		
//		if (action != null)
//		{
//			final var skill = SkillTable.getInstance().getInfoLevelMax(action.getActionId());
//			npcHtml.replace("%sic%", skill.getIcon());
//			npcHtml.replace("%sna%", skill.getName());
//			
//			npcHtml.replace("%reu%", String.format("%.2fs", action.getReuse()));
//			npcHtml.replace("%hpp%", String.format("%05.2f%%", action.getUserHp()));
//			npcHtml.replace("%tpp%", String.format("%05.2f%%", action.getTargetHp()));
//			
//			final var epriorities = EActionPriority.values();
//			final var priority = epriorities[slot];
//
//			String spr = priority.toString();
//			for (final var pr : epriorities) if (pr != priority)
//				spr += ";" + pr.toString();
//			
//			npcHtml.replace("%pr%", spr);
//		}
//		else
//		{
//			npcHtml.replace("%sic%", "L2UI_CT1.Inventory_DF_CloakSlot_Disable");
//			npcHtml.replace("%sna%", "Empty");
//
//			npcHtml.replace("%reu%", "?");
//			npcHtml.replace("%hpp%", "?");
//			npcHtml.replace("%tpp%", "?");
//			npcHtml.replace("%pr%", "");
//		}
//
//		npcHtml.replace("%priority%","" + slot+1);
//		npcHtml.replace("%slot%","" + slot);
//
//		
//		player.sendPacket(npcHtml);
//	}
//	
//	private void renderRange()
//	{
//		final var player = getActivePlayer();
//		
//		final int searchRange = _searchType.getRange();
//		
//		final ILocational renderLoc = _moveType == EMoveType.Saved_Location ? _lastSavedLocation : player;
//		
//		final ExServerPrimitive renderRange = new ExServerPrimitive("SearchRange", renderLoc);
//		if ((_moveType != EMoveType.Follow_Target) && (searchRange > 1))
//		{
//			final var color = _running ? Color.GREEN : Color.RED;
//			renderRange.addCircle(color, searchRange, 30, -20);
//			renderRange.addCircle(color, 5, 4, -20);
//		}
//		else
//			renderRange.addCircle(Color.GREEN, 1, 1, -5);
//
//		player.sendPacket(renderRange);
//	}
//
//	public boolean isRunning()
//	{
//		return _running;
//	}
//	
//	public void setRunning(final boolean running)
//	{
//		_running = running;
//		renderRange();
//	}
//	
//	@Override
//	public void run()
//	{
//	}
//	
//	public L2Character getTargetAssist()
//	{
//		return null;
//	}
//	
//	public L2Character getTargetRange(final int range)
//	{
//		return null;
//	}
//	
//	public static class ChillAction
//	{
//		private final int _actionId;
//		
//		private final boolean _isSkill;
//		
//		private double _userHp = 100;
//		
//		private double _targHp = 100;
//		
//		private long _reuse;
//		
//		private long _lastUse;
//		
//		public ChillAction(final int actionId, final boolean isSkill)
//		{
//			_actionId = actionId;
//			_isSkill = isSkill;
//		}
//		
//		public int getActionId()
//		{
//			return _actionId;
//		}
//
//		public String getIcon()
//		{
//			return _isSkill ? IconsTable.getInstance().getSkillIcon(_actionId) : IconsTable.getInstance().getItemIcon(_actionId);
//		}
//		
//		public boolean isReuse()
//		{
//			return _lastUse + _reuse > System.currentTimeMillis();
//		}
//		
//		public void initReuse()
//		{
//			_lastUse = System.currentTimeMillis();
//		}
//		
//		public double getReuse()
//		{
//			return _reuse / 1000d;
//		}
//		
//		public void setReuse(final double reuseSec)
//		{
//			_reuse = Math.min((long) (reuseSec * 1000L), 300_000);
//		}
//		
//		public void setUserHP(final double userHp)
//		{
//			_userHp = Math.min(100d, userHp);
//		}
//
//		public boolean isUserHp(final L2PcInstance player)
//		{
//			return player.getHpPercent() <= _userHp;
//		}
//		
//		public double getUserHp()
//		{
//			return _userHp;
//		}
//		
//		public void setTargetHP(final double targHp)
//		{
//			_targHp = Math.min(100d, targHp);
//		}
//
//		public boolean isTargetHp(final L2Character target)
//		{
//			return target.getHpPercent() <= _targHp;
//		}
//		
//		public double getTargetHp()
//		{
//			return _targHp;
//		}
//		
//		public boolean isSkill()
//		{
//			return _isSkill;
//		}
//	}
//}
