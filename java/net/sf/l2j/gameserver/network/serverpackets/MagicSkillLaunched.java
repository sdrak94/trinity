package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;

public class MagicSkillLaunched
  extends L2GameServerPacket
{
  private static final String _S__8E_MAGICSKILLLAUNCHED = "[S] 54 MagicSkillLaunched";
  private int _charObjId;
  private int _skillId;
  private int _skillLevel;
  private int _numberOfTargets;
  private L2Object[] _targets;
  private int _singleTargetId;
  
  public MagicSkillLaunched(L2Character cha, int skillId, int skillLevel, L2Object[] targets)
  {
    this._charObjId = cha.getObjectId();
    this._skillId = skillId;
    this._skillLevel = skillLevel;
    if (targets != null)
    {
      this._numberOfTargets = targets.length;
      this._targets = targets;
    }
    else
    {
      this._numberOfTargets = 1;
      L2Object[] objs = { cha };
      this._targets = objs;
    }
    this._singleTargetId = 0;
  }
  
  public MagicSkillLaunched(L2Character cha, int skillId, int skillLevel)
  {
    this._charObjId = cha.getObjectId();
    this._skillId = skillId;
    this._skillLevel = skillLevel;
    this._numberOfTargets = 1;
    this._singleTargetId = cha.getTargetId();
  }
  
  protected final void writeImpl()
  {
    writeC(84);
    writeD(this._charObjId);
    writeD(this._skillId);
    writeD(this._skillLevel);
    writeD(this._numberOfTargets);
    if ((this._singleTargetId != 0) || (this._numberOfTargets == 0)) {
      writeD(this._singleTargetId);
    } else {
      for (L2Object target : this._targets) {
        writeD(target.getObjectId());
      }
    }
  }
  
  public String getType()
  {
    return "[S] 54 MagicSkillLaunched";
  }
}
