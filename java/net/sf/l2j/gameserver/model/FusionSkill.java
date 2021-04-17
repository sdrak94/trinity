package net.sf.l2j.gameserver.model;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.skills.effects.EffectFusion;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.util.Util;

public final class FusionSkill
{
    protected static final Logger _log = Logger.getLogger(FusionSkill.class.getName());

    protected int _skillCastRange;
    protected int _fusionId;
    protected int _originalSkillId;
    protected int _fusionLevel;
    protected L2Character _caster;
    protected L2Character _target;
    protected Future<?> _geoCheckTask;

    public L2Character getCaster()
    {
        return _caster;
    }
    
    public L2Character getTarget()
    {
        return _target;
    }
    
    public FusionSkill(L2Character caster, L2Character target, L2Skill skill)
    {
        _skillCastRange = skill.getEffectRange(caster);
        _originalSkillId = skill.getId();
        _caster = caster;
        _target = target;
        _fusionId = skill.getTriggeredId();
        _fusionLevel = skill.getTriggeredLevel();
        
        L2Effect effect = _target.getFirstEffect(_fusionId);
        if (effect != null && effect.getEffectType() == L2EffectType.FUSION)
            ((EffectFusion)effect).increaseEffect();
        else
        {
            final L2Skill force = SkillTable.getInstance().getInfo(_fusionId, _fusionLevel);
            
            if (force != null)
            {
                force.getEffects(_caster, _target, null);
                skill.getEffectsSelf(caster);
            }
            else
                _log.warning("Triggered skill ["+_fusionId+";"+_fusionLevel+"] not found!");
        }
        
        _geoCheckTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new GeoCheckTask(), 1000, 1000);
        
        if (caster instanceof L2Playable)
        {
        	if (skill.isOffensive())
        		caster.getActingPlayer().updatePvPStatus(target);
        	else if (!skill.isNeutral())
        	{
        		if (target instanceof L2Playable)
        		{
        			if (target.getActingPlayer().getPvpFlag() != 0 || target.getActingPlayer().getKarma() > 0)
        				caster.getActingPlayer().updatePvPStatus(target);
        		}
        	}
        }
    }
    
    public void onCastAbort()
    {
        _caster.setFusionSkill(null);
        final L2Effect effect = _target.getFirstEffect(_fusionId);
        if (effect != null)
        {
        	if (effect.getEffectType() == L2EffectType.FUSION)
        		((EffectFusion)effect).decreaseForce();
        	else
        	{
        		_target.stopSkillEffects(_fusionId);
        		_caster.stopSkillEffects(_originalSkillId);
        	}
        }
        
        _geoCheckTask.cancel(true);
    }
    
    public class GeoCheckTask implements Runnable
    {
        public void run()
        {
            try
            {
                if (!Util.checkIfInRange(_skillCastRange, _caster, _target, true))
                    _caster.abortCast();
                
                if (!GeoData.getInstance().canSeeTarget(_caster, _target))
                    _caster.abortCast();
            }
            catch (Exception e)
            {
            }
        }
    }
}