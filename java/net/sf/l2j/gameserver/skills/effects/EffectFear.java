package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Decoy;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2FortCommanderInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FortSiegeGuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeFlagInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeGuardInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.util.Rnd;

public class EffectFear extends L2Effect
{
public static final int FEAR_RANGE = 500;

private int _dX = -1;
private int _dY = -1;

public EffectFear(Env env, EffectTemplate template, Boolean ignoreBoost)
{
	super(env, template, ignoreBoost);
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#getEffectType()
 */
@Override
public L2EffectType getEffectType()
{
	return L2EffectType.FEAR;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onStart()
 */
@Override
public boolean onStart()
{
	// Fear skills cannot be used l2pcinstance to l2pcinstance. Heroic
	// Dread, Curse: Fear, Fear, Horror, Sword Symphony, Word of Fear and
	// Mass Curse Fear are the exceptions.
	/*		if (getEffected() instanceof L2PcInstance && getEffector() instanceof L2PcInstance
		        && getSkill().getId() != 1376 && getSkill().getId() != 1169
		        && getSkill().getId() != 65 && getSkill().getId() != 1092
		        && getSkill().getId() != 98 && getSkill().getId() != 1272
		        && getSkill().getId() != 1381)
			return false;*/
	
	if (getEffected() instanceof L2NpcInstance
			|| getEffected() instanceof L2SiegeGuardInstance
			|| getEffected() instanceof L2FortSiegeGuardInstance
			|| getEffected() instanceof L2FortCommanderInstance
			|| getEffected() instanceof L2SiegeFlagInstance
			|| getEffected() instanceof L2SiegeSummonInstance)
		return false;
	
	if (getEffected() instanceof L2MonsterInstance)
	{
		if (Rnd.get(100) > 50)
			return false;
	}
	
	if (getEffected() instanceof L2PcInstance)
	{
		if (!(getEffector() instanceof L2Playable && getEffector().getActingPlayer().isGM()))
		{
			L2PcInstance player = (L2PcInstance)getEffected();
			final int effectorId = (getEffector() instanceof L2Playable || getEffector() instanceof L2Decoy) ? getEffector().getActingPlayer().getObjectId() : 0;
			
			if (player != null)
			{
				if (getEffectTemplate()._ignoreProtection ? !player.canBeFeared(effectorId) : !player.canBeFeared(0))
				{
					getEffector().sendMessage(player.getDisplayName()+" is not affected by your "+getSkill().getName()+" because of 20 second fear/mute protection");
					return false;
				}
			}
			else return false;
			
			player.setFearProtectionTime(effectorId);
		}
	}
	
	if (getEffected().getX() > getEffector().getX())
		_dX = 1;
	if (getEffected().getY() > getEffector().getY())
		_dY = 1;
	
	super.onStart();
	
	getEffected().startFear();
	onActionTime();
	return true;
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onExit()
 */
@Override
public void onExit()
{
	getEffected().stopFear(this);
	super.onExit();
}

/**
 * 
 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
 */
@Override
public boolean onActionTime()
{
	int posX = getEffected().getX();
	int posY = getEffected().getY();
	int posZ = getEffected().getZ();
	
	posX += _dX * FEAR_RANGE;
	posY += _dY * FEAR_RANGE;
	
	if (Config.GEODATA > 0)
	{
		Location destiny = GeoData.getInstance().moveCheck(getEffected().getX(), getEffected().getY(), getEffected().getZ(), posX, posY, posZ, getEffected().getInstanceId());
		posX = destiny.getX();
		posY = destiny.getY();
	}
	
	if (!(getEffected() instanceof L2PetInstance))
		getEffected().setRunning();
	
	getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(posX, posY, posZ, 0));
	return true;
}
}
