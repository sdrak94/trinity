package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.L2Character;

public final class MagicSkillUse
  extends L2GameServerPacket
{
  private static final String _S__5A_MAGICSKILLUSE = "[S] 48 MagicSkillUse";
  private int _targetId;
  private int _tx;
  private int _ty;
  private int _tz;
  private int _skillId;
  private int _skillLevel;
  private int _hitTime;
  private int _reuseDelay;
  private int _charObjId;
  private int _x;
  private int _y;
  private int _z;
  
  public MagicSkillUse(L2Character cha, L2Character target, int skillId, int skillLevel, int hitTime, int reuseDelay)
  {
    this._charObjId = cha.getObjectId();
    if (target == null) {
      this._targetId = cha.getObjectId();
    } else {
      this._targetId = target.getObjectId();
    }
    this._skillId = skillId;
    this._skillLevel = skillLevel;
    this._hitTime = hitTime;
    this._reuseDelay = reuseDelay;
    this._x = cha.getX();
    this._y = cha.getY();
    this._z = cha.getZ();
    this._tx = target.getX();
    this._ty = target.getY();
    this._tz = target.getZ();
  }
  
  public MagicSkillUse(L2Character cha, int skillId, int skillLevel, int hitTime, int reuseDelay)
  {
    this._charObjId = cha.getObjectId();
    this._targetId = cha.getTargetId();
    this._skillId = skillId;
    this._skillLevel = skillLevel;
    this._hitTime = hitTime;
    this._reuseDelay = reuseDelay;
    this._x = cha.getX();
    this._y = cha.getY();
    this._z = cha.getZ();
    this._tx = cha.getX();
    this._ty = cha.getY();
    this._tz = cha.getZ();
  }
  public int getSkillId()
  {
	  return _skillId;
  }
  protected final void writeImpl()
  {
    writeC(72);
    writeD(this._charObjId);
    writeD(this._targetId);
    writeD(this._skillId);
    writeD(this._skillLevel);
    writeD(this._hitTime);
    writeD(this._reuseDelay);
    writeD(this._x);
    writeD(this._y);
    writeD(this._z);
    writeD(0);
    writeD(this._tx);
    writeD(this._ty);
    writeD(this._tz);
  }
  
  public String getType()
  {
    return "[S] 48 MagicSkillUser";
  }
}
