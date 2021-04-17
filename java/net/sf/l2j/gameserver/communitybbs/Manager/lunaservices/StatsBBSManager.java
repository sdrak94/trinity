package net.sf.l2j.gameserver.communitybbs.Manager.lunaservices;

import java.text.DecimalFormat;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;

public class StatsBBSManager
{
	public static StatsBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		String path = "data/html/CommunityBoard/stats/";
		String filepath = "";
		String content = "";
		if (command.equals("_bbsstats"))
		{
			filepath = path + "stats.htm";
			content = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), filepath);
			content = replaceVars(activeChar, content);
			separateAndSend(content, activeChar);
		}
		else
		{
			ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", "101");
			activeChar.sendPacket(sb);
			activeChar.sendPacket(new ShowBoard(null, "102"));
			activeChar.sendPacket(new ShowBoard(null, "103"));
		}
	}
	
	private String replaceVars(L2PcInstance activeChar, String content)
	{
		final int combinedCritRate = (int) (activeChar.calcStat(Stats.SKILL_CRITICAL_CHANCE_INCREASE, 15 * (activeChar.isDaggerClass() ? Formulas.STRbonus[activeChar.getSTR()] : Formulas.DEXbonus[activeChar.getDEX()]), null, null));
		final int shldRate = (int) Math.min(activeChar.getShldRate(null, null), activeChar.calcStat(Stats.BLOCK_RATE_MAX, 80, null, null));
		final int atkCount = (int) (activeChar.getStat().calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null));
		final L2Item weapon = activeChar.getActiveWeaponItem();
		int VampRate = 0;
		{
			if ((weapon != null))
			{
				if ((weapon.getItemType() == L2WeaponType.BOW || weapon.getItemType() == L2WeaponType.CROSSBOW))
					VampRate = 0;
				else
					VampRate = 1;
			}
		}
		String FvR = String.valueOf(VampRate);
		content = content.replace("%cdm%", new DecimalFormat("0.##").format(activeChar.getCriticalDmg(null, 1.66, null)) + "x +" + activeChar.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, null, null));
		content = content.replace("%mcr%", Math.round(activeChar.getMCriticalHit(null, null) / 10) + "%");
		content = content.replace("%scr%", combinedCritRate + "%");
		content = content.replace("%srd%", (int) (activeChar.getStat().getMReuseRateGem(false) * 100) + "%");
		content = content.replace("%mrd%", (int) (activeChar.getStat().getMReuseRateGem(true) * 100) + "%");
		content = content.replace("%ard%", (int) (activeChar.getAtkReuse(100)) + "%");
		content = content.replace("%sbr%", shldRate + "%");
		content = content.replace("%sd%", activeChar.getShldDef() + "");
		content = content.replace("%sda%", (shldRate >= 1 ? (int) activeChar.calcStat(Stats.SHIELD_DEFENCE_ANGLE, 120, null, null) : "N/A") + "");
		content = content.replace("%hbr%", (int) (activeChar.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null)) + "%");
		content = content.replace("%hpg%", (int) (activeChar.calcStat(Stats.HEAL_PROFICIENCY, 100, null, null)) + "%");
		content = content.replace("%pvpahd%", new DecimalFormat("0.##").format(activeChar.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null)) + "x");
		content = content.replace("%pvppsd%", new DecimalFormat("0.##").format(activeChar.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null)) + "x");
		content = content.replace("%pvpmd%", new DecimalFormat("0.##").format(activeChar.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null)) + "x");
		content = content.replace("%pvedb%", new DecimalFormat("0.##").format(activeChar.calcStat(Stats.PVM_DAMAGE, 1, null, null)) + "x");
		content = content.replace("%pvedv%", new DecimalFormat("0.##").format(activeChar.calcStat(Stats.PVM_DAMAGE_VUL, 1, null, null)) + "x");
		content = content.replace("%psd%", (int) (activeChar.calcStat(Stats.P_SKILL_EVASION, 0, null, null)) + "%");
		content = content.replace("%msd%", (int) (activeChar.calcStat(Stats.M_SKILL_EVASION, 0, null, null)) + "%");
		content = content.replace("%arng%", activeChar.getPhysicalAttackRange() + "");
		content = content.replace("%crng%", activeChar.getStat().getMagicalRangeBoost() + "");
		content = content.replace("%dr%", (int) (activeChar.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null)) + "%");
		content = content.replace("%sr%", (int) (activeChar.getStat().calcStat(Stats.REFLECT_SKILL_PHYSIC, 0, null, null)) + "%");
		content = content.replace("%mr%", (int) (activeChar.getStat().calcStat(Stats.REFLECT_SKILL_MAGIC, 0, null, null)) + "%");
		content = content.replace("%hpreg%", (int) (Formulas.calcHpRegen(activeChar)) + " per tick");
		content = content.replace("%mpreg%", (int) (Formulas.calcMpRegen(activeChar)) + " per tick");
		content = content.replace("%va%", (int) (activeChar.getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null)) + "%");
		content = content.replace("%vc%", FvR +"/" + activeChar.getVampRate(activeChar));
		content = content.replace("%sva%", (int) (activeChar.getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT_SKILL, 0, null, null)) + "%");
		content = content.replace("%cdr%", (int) (1 - activeChar.getStat().calcStat(Stats.CRIT_VULN, 1, null, null)) * 100 + "%");
		content = content.replace("%chn%", activeChar.calcStat(Stats.CRIT_DAMAGE_EVASION, 0, null, null) + "%");
		content = content.replace("%mdr%", (int) (1 - activeChar.getStat().calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, null)) * 100 + "%");
		content = content.replace("%mcdm%", new DecimalFormat("0.##").format(activeChar.getStat().calcStat(Stats.MAGIC_CRITICAL_DAMAGE, 2, null, null)) + "x");
		content = content.replace("%ac%", atkCount + "");
		content = content.replace("%aaoea%", (atkCount > 1 ? (int) (activeChar.getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null)) : "N/A") + "");
		content = content.replace("%aec%", (int) (activeChar.getStat().calcStat(Stats.EVASION_ABSOLUTE, 0, null, null)) + "%");
		return content;
	}
	protected void separateAndSend(String html, L2PcInstance acha)
	{
		if (html == null)
			return;
		acha.sendPacket(new ShowBoard(html, "101"));
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final StatsBBSManager _instance = new StatsBBSManager();
	}
}
