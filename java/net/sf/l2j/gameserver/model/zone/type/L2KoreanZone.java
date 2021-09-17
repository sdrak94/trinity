package net.sf.l2j.gameserver.model.zone.type;

import ghosts.model.Ghost;
import luna.custom.LunaVariables;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class L2KoreanZone  extends L2ZoneType
{

	public L2KoreanZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_PVP, true);
		character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, true);
		if (character instanceof L2PcInstance)
		{
			if (character.getActingPlayer().getInstanceId() == 0)
			{

				character.getActingPlayer().setIsInKoreanZone(true);
				character.getActingPlayer().updatePvPFlag(1);
				
				if (LunaVariables.getInstance().getKoreanCubicSkillsPrevented())
				{
						boolean removed = false;
						for (L2CubicInstance cubic : character.getActingPlayer().getCubics().values())
						{
							cubic.stopAction();
							character.getActingPlayer().delCubic(cubic.getId());
							removed = true;
						}
						
						if (removed)
							character.getActingPlayer().broadcastUserInfo();
				}
				character.setInsideZone(L2Character.ZONE_EVENT, true);
				if (Config.HWID_FARMZONES_CHECK)
				{
					String hwid = ((L2PcInstance) character).getHWID();
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{

						if (!(player instanceof Ghost) && player.getClient().isDetached())
						{
							continue;
						}
						if (!player.isInsideZone(L2Character.ZONE_EVENT))
						{
							continue;
						}
						if (player == character)
						{
							continue;
						}
						if (player.isInKoreanZone())
						{
							continue;
						}
						if (player.isGM() || character.isGM())
						{
							continue;
						}
						
						String plr_hwid = player.getClient().getFullHwid();
						if (plr_hwid.equalsIgnoreCase(hwid))
						{
							character.setIsPendingRevive(true);
							character.teleToLocation(83380, 148107, -3404, true);
							character.setInsideZone(L2Character.ZONE_EVENT, false);
							character.sendMessage("You have another window in the Korean zone.");
							break;
						}
						else
						{
							if(character.isGM())
							{
								character.sendMessage("You have entered the Korean Area");
							}
						}
					}
				}
			}
			((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_PVP, false);
		character.setInsideZone(L2Character.ZONE_EVENT, false);
		character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, false);
		
		if (character instanceof L2PcInstance)
		{
			character.getActingPlayer().setIsInKoreanZone(false);
			character.getActingPlayer().stopPvPFlag();
			
			((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
		}
		
	}

	@Override
	public void onDieInside(L2Character character)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReviveInside(L2Character character)
	{
		// TODO Auto-generated method stub
		
	}}
