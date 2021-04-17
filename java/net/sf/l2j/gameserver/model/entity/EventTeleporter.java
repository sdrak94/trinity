package net.sf.l2j.gameserver.model.entity;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class EventTeleporter
{
protected static final Logger _log = Logger.getLogger(EventTeleporter.class.getName());

private L2PcInstance _player;
private Location _teleLoc;
//private int _teleDelay;
private boolean _randomOffset;
private int _instanceId;

public EventTeleporter(L2PcInstance player, Location loc, int delay, boolean randomOffset, int instanceId)
{
	_player = player;
	_teleLoc = loc;
	_randomOffset = randomOffset;
	//_teleDelay = delay;
	_instanceId = instanceId;
	
	doTeleport();
}

private void doTeleport()
{
	if (_player == null)
		return;
	
	L2Summon summon = _player.getPet();
	
	if (summon != null)
		summon.unSummon(_player);
	
	if (_player.isInDuel())
		_player.setDuelState(Duel.DUELSTATE_INTERRUPTED);
	
	_player.doRevive();
	
	for(L2Effect e : _player.getAllEffects())
	{
		// this should clean all debuffs, untested
		if(e != null && e.getSkill() != null && e.getSkill().isDebuff())
			e.exit();
	}
	
	if(_player.isSitting())
		_player.standUp();
	
	_player.setTarget(null);
	
	_player.teleToLocation( _teleLoc, _randomOffset );
	
	if(_instanceId != -1)
	{
		_player.setInstanceId(_instanceId);
	}
	
	_player.setCurrentCp(_player.getMaxCp());
	_player.setCurrentHp(_player.getMaxHp());
	_player.setCurrentMp(_player.getMaxMp());
	
	_player.broadcastStatusUpdate();
	_player.broadcastUserInfo();
}
}