package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.sf.l2j.L2DatabaseFactory;

public class NpcBufferTable
{
	protected static Logger _log = Logger.getLogger(NpcBufferTable.class.getName());
	
	private class NpcBufferSkills
	{
		private final TIntIntHashMap _skillId = new TIntIntHashMap();
		private final TIntIntHashMap _skillLevels = new TIntIntHashMap();
		private final TIntIntHashMap _skillFeeIds = new TIntIntHashMap();
		private final TIntIntHashMap _skillFeeAmounts = new TIntIntHashMap();
		
		public NpcBufferSkills(int npcId)
		{
		}
		
		public void addSkill(int skillId, int skillLevel, int skillFeeId, int skillFeeAmount, int buffGroup)
		{
			_skillId.put(buffGroup, skillId);
			_skillLevels.put(buffGroup, skillLevel);
			_skillFeeIds.put(buffGroup, skillFeeId);
			_skillFeeAmounts.put(buffGroup, skillFeeAmount);
		}
		
		public int[] getSkillGroupInfo(int buffGroup)
		{
			Integer skillId = _skillId.get(buffGroup);
			Integer skillLevel = _skillLevels.get(buffGroup);
			Integer skillFeeId = _skillFeeIds.get(buffGroup);
			Integer skillFeeAmount = _skillFeeAmounts.get(buffGroup);
			
			if (skillId == null || skillLevel == null || skillFeeId == null
			        || skillFeeAmount == null)
				return null;
			
			return new int[] { skillId, skillLevel, skillFeeId, skillFeeAmount };
		}
	}
	
	private final TIntObjectHashMap<NpcBufferSkills> _buffers = new TIntObjectHashMap<NpcBufferSkills>();
	
	private NpcBufferTable()
	{
		Connection con = null;
		int skillCount = 0;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement("SELECT `npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group` FROM `npc_buffer` ORDER BY `npc_id` ASC");
			ResultSet rset = statement.executeQuery();
			
			int lastNpcId = 0;
			NpcBufferSkills skills = null;
			
			while (rset.next())
			{
				int npcId = rset.getInt("npc_id");
				int skillId = rset.getInt("skill_id");
				int skillLevel = rset.getInt("skill_level");
				int skillFeeId = rset.getInt("skill_fee_id");
				int skillFeeAmount = rset.getInt("skill_fee_amount");
				int buffGroup = rset.getInt("buff_group");
				
				if (npcId != lastNpcId)
				{
					if (lastNpcId != 0)
						_buffers.put(lastNpcId, skills);
					
					skills = new NpcBufferSkills(npcId);
					skills.addSkill(skillId, skillLevel, skillFeeId, skillFeeAmount, buffGroup);
				}
				else
					skills.addSkill(skillId, skillLevel, skillFeeId, skillFeeAmount, buffGroup);
				
				lastNpcId = npcId;
				skillCount++;
			}
			
			_buffers.put(lastNpcId, skills);
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NpcBufferSkillIdsTable: Error reading npc_buffer table: "
			        + e.getMessage(), e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		
		_log.info("NpcBufferSkillIdsTable: Loaded " + _buffers.size() + " buffers and "
		        + skillCount + " skills.");
	}
	
	public static NpcBufferTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public int[] getSkillInfo(int npcId, int buffGroup)
	{
		NpcBufferSkills skills = _buffers.get(npcId);
		
		if (skills == null)
			return null;
		
		return skills.getSkillGroupInfo(buffGroup);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final NpcBufferTable _instance = new NpcBufferTable();
	}
}