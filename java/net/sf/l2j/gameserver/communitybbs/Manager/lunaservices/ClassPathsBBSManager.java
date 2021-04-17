package net.sf.l2j.gameserver.communitybbs.Manager.lunaservices;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class ClassPathsBBSManager
{
	int ClassPathUID = 10000;
	
	public static ClassPathsBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private void mainPath(L2PcInstance player)
	{
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		String path = "data/html/CommunityBoard/classpath/";
		String filepath = "";
		String content = "";
		filepath = path + "main.htm";
		content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), filepath);
		content = content.replace("%remain%", remain + "");
		separateAndSend(content, player);
	}
	
	private String replaceVarsOff(L2PcInstance player, String content)
	{
		String dis = "_dis";
		String iconmiddle = "l2trinity_cb_cp.cp_patk";
		String iconleft = "l2trinity_cb_cp.cp_rcrit";
		String iconleft1 = "l2trinity_cb_cp.cp_crtdmg";
		String iconleft2 = "l2trinity_cb_cp.cp_atkspd";
		String iconright = "l2trinity_cb_cp.cp_damage";
		String iconright1 = "l2trinity_cb_cp.cp_acc";
		String iconright2 = "l2trinity_cb_cp.cp_preuse";
		String iconult1 = "l2trinity_cb_cp.cp_off_ult_1";
		String iconult2 = "l2trinity_cb_cp.cp_off_ult_2";
		String iconult3 = "l2trinity_cb_cp.cp_off_ult_3";
		String iconult4 = "l2trinity_cb_cp.cp_off_ult_4";
		String iconult5 = "l2trinity_cb_cp.cp_off_ult_5";
		String iconult6 = "l2trinity_cb_cp.cp_off_ult_6";
		String iconult7 = "l2trinity_cb_cp.cp_off_ult_7";
		String iconult8 = "l2trinity_cb_cp.cp_off_ult_8";
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		content = content.replace("%remain%", remain + "");
		content = content.replace("%middle%", player.getMiddleOff() + "/3");
		content = content.replace("%left%", player.getLeftOff() + "/3");
		content = content.replace("%left1%", player.getLeftOff1() + "/3");
		content = content.replace("%left2%", player.getLeftOff2() + "/3");
		content = content.replace("%right%", player.getRightOff() + "/3");
		content = content.replace("%right1%", player.getRightOff1() + "/3");
		content = content.replace("%right2%", player.getRightOff2() + "/3");
		content = content.replace("%left1_1%", player.getLeftOff1_1() + "/1");
		content = content.replace("%left1_2%", player.getLeftOff1_2() + "/1");
		content = content.replace("%left2_1%", player.getLeftOff2_1() + "/1");
		content = content.replace("%left2_2%", player.getLeftOff2_2() + "/1");
		content = content.replace("%right1_1%", player.getRightOff1_1() + "/1");
		content = content.replace("%right1_2%", player.getRightOff1_2() + "/1");
		content = content.replace("%right2_1%", player.getRightOff2_1() + "/1");
		content = content.replace("%right2_2%", player.getRightOff2_2() + "/1");
		if (player.getMiddleOff() == 0)
			content = content.replace("%iconmiddle%", iconmiddle + dis);
		else
			content = content.replace("%iconmiddle%", iconmiddle);
		if (player.getLeftOff() == 0)
			content = content.replace("%iconleft%", iconleft + dis);
		else
			content = content.replace("%iconleft%", iconleft);
		if (player.getLeftOff1() == 0)
			content = content.replace("%iconleft1%", iconleft1 + dis);
		else
			content = content.replace("%iconleft1%", iconleft1);
		if (player.getLeftOff2() == 0)
			content = content.replace("%iconleft2%", iconleft2 + dis);
		else
			content = content.replace("%iconleft2%", iconleft2);
		if (player.getRightOff() == 0)
			content = content.replace("%iconright%", iconright + dis);
		else
			content = content.replace("%iconright%", iconright);
		if (player.getRightOff1() == 0)
			content = content.replace("%iconright1%", iconright1 + dis);
		else
			content = content.replace("%iconright1%", iconright1);
		if (player.getRightOff2() == 0)
			content = content.replace("%iconright2%", iconright2 + dis);
		else
			content = content.replace("%iconright2%", iconright2);
		if (player.getLeftOff1_1() == 0)
			content = content.replace("%iconult1%", iconult1 + dis);
		else
			content = content.replace("%iconult1%", iconult1);
		if (player.getLeftOff1_2() == 0)
			content = content.replace("%iconult2%", iconult2 + dis);
		else
			content = content.replace("%iconult2%", iconult2);
		if (player.getLeftOff1_1() == 0)
			content = content.replace("%iconult1%", iconult1 + dis);
		else
			content = content.replace("%iconult1%", iconult1);
		if (player.getLeftOff2_1() == 0)
			content = content.replace("%iconult3%", iconult3 + dis);
		else
			content = content.replace("%iconult3%", iconult3);
		if (player.getLeftOff2_2() == 0)
			content = content.replace("%iconult4%", iconult4 + dis);
		else
			content = content.replace("%iconult4%", iconult4);
		if (player.getRightOff1_1() == 0)
			content = content.replace("%iconult5%", iconult5 + dis);
		else
			content = content.replace("%iconult5%", iconult5);
		if (player.getRightOff1_2() == 0)
			content = content.replace("%iconult6%", iconult6 + dis);
		else
			content = content.replace("%iconult6%", iconult6);
		if (player.getRightOff2_1() == 0)
			content = content.replace("%iconult7%", iconult7 + dis);
		else
			content = content.replace("%iconult7%", iconult7);
		if (player.getRightOff2_2() == 0)
			content = content.replace("%iconult8%", iconult8 + dis);
		else
			content = content.replace("%iconult8%", iconult8);
		if (player.getMiddleOff() == 0)
		{
			content = content.replace("%statmiddle%", "P.Atk +100");
		}
		else if (player.getMiddleOff() == 1)
		{
			content = content.replace("%statmiddle%", "P.Atk +200");
		}
		else
		{
			content = content.replace("%statmiddle%", "P.Atk +300");
		}
		if (player.getLeftOff() == 0)
		{
			content = content.replace("%statleft%", "R.Crit +10");
		}
		else if (player.getLeftOff() == 1)
		{
			content = content.replace("%statleft%", "R.Crit +20");
		}
		else
		{
			content = content.replace("%statleft%", "R.Crit +30");
		}
		if (player.getLeftOff1() == 0)
		{
			content = content.replace("%statleftone%", "Crt Dmg +5%");
		}
		else if (player.getLeftOff1() == 1)
		{
			content = content.replace("%statleftone%", "Crt Dmg +10%");
		}
		else
		{
			content = content.replace("%statleftone%", "Crt Dmg +15%");
		}
		if (player.getLeftOff2() == 0)
		{
			content = content.replace("%statlefttwo%", "Atk Speed +10");
		}
		else if (player.getLeftOff2() == 1)
		{
			content = content.replace("%statlefttwo%", "Atk Speed +20");
		}
		else
		{
			content = content.replace("%statlefttwo%", "Atk Speed +30");
		}
		if (player.getRightOff() == 0)
		{
			content = content.replace("%statright%", "Dmg +1%");
		}
		else if (player.getRightOff() == 1)
		{
			content = content.replace("%statright%", "Dmg +2%");
		}
		else
		{
			content = content.replace("%statright%", "Dmg +3%");
		}
		if (player.getRightOff1() == 0)
		{
			content = content.replace("%statrightone%", "Acc +1");
		}
		else if (player.getRightOff1() == 1)
		{
			content = content.replace("%statrightone%", "Acc +2");
		}
		else
		{
			content = content.replace("%statrightone%", "Acc +4");
		}
		if (player.getRightOff2() == 0)
		{
			content = content.replace("%statrighttwo%", "P.Reu -2%");
		}
		else if (player.getRightOff2() == 1)
		{
			content = content.replace("%statrighttwo%", "P.Reu -4%");
		}
		else
		{
			content = content.replace("%statrighttwo%", "P.Reu -6%");
		}
		return content;
	}
	
	private String replaceVarsMage(L2PcInstance player, String content)
	{
		String dis = "_dis";
		String iconmiddle = "l2trinity_cb_cp.cp_matk";
		String iconleft = "l2trinity_cb_cp.cp_cspd";
		String iconleft1 = "l2trinity_cb_cp.cp_mreuse";
		String iconleft2 = "l2trinity_cb_cp.cp_csrng";
		String iconright = "l2trinity_cb_cp.cp_mcritrate";
		String iconright1 = "l2trinity_cb_cp.cp_mcritdmg";
		String iconright2 = "l2trinity_cb_cp.cp_magicpen";
		String iconmageult1 = "l2trinity_cb_cp.cp_mage_ult_1";
		String iconmageult2 = "l2trinity_cb_cp.cp_mage_ult_2";
		String iconmageult3 = "l2trinity_cb_cp.cp_mage_ult_3";
		String iconmageult4 = "l2trinity_cb_cp.cp_mage_ult_4";
		String iconmageult5 = "l2trinity_cb_cp.cp_mage_ult_5";
		String iconmageult6 = "l2trinity_cb_cp.cp_mage_ult_6";
		String iconmageult7 = "l2trinity_cb_cp.cp_mage_ult_7";
		String iconmageult8 = "l2trinity_cb_cp.cp_mage_ult_8";
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		content = content.replace("%remain%", remain + "");
		content = content.replace("%middle%", player.getMiddleMage() + "/3");
		content = content.replace("%left%", player.getLeftMage() + "/3");
		content = content.replace("%left1%", player.getLeftMage1() + "/3");
		content = content.replace("%left2%", player.getLeftMage2() + "/3");
		content = content.replace("%right%", player.getRightMage() + "/3");
		content = content.replace("%right1%", player.getRightMage1() + "/3");
		content = content.replace("%right2%", player.getRightMage2() + "/3");
		content = content.replace("%left1_1%", player.getLeftMage1_1() + "/1");
		content = content.replace("%left1_2%", player.getLeftMage1_2() + "/1");
		content = content.replace("%left2_1%", player.getLeftMage2_1() + "/1");
		content = content.replace("%left2_2%", player.getLeftMage2_2() + "/1");
		content = content.replace("%right1_1%", player.getRightMage1_1() + "/1");
		content = content.replace("%right1_2%", player.getRightMage1_2() + "/1");
		content = content.replace("%right2_1%", player.getRightMage2_1() + "/1");
		content = content.replace("%right2_2%", player.getRightMage2_2() + "/1");
		if (player.getMiddleMage() == 0)
			content = content.replace("%iconmiddle%", iconmiddle + dis);
		else
			content = content.replace("%iconmiddle%", iconmiddle);
		if (player.getLeftMage() == 0)
			content = content.replace("%iconleft%", iconleft + dis);
		else
			content = content.replace("%iconleft%", iconleft);
		if (player.getLeftMage1() == 0)
			content = content.replace("%iconleft1%", iconleft1 + dis);
		else
			content = content.replace("%iconleft1%", iconleft1);
		if (player.getLeftMage2() == 0)
			content = content.replace("%iconleft2%", iconleft2 + dis);
		else
			content = content.replace("%iconleft2%", iconleft2);
		if (player.getRightMage() == 0)
			content = content.replace("%iconright%", iconright + dis);
		else
			content = content.replace("%iconright%", iconright);
		if (player.getRightMage1() == 0)
			content = content.replace("%iconright1%", iconright1 + dis);
		else
			content = content.replace("%iconright1%", iconright1);
		if (player.getRightMage2() == 0)
			content = content.replace("%iconright2%", iconright2 + dis);
		else
			content = content.replace("%iconright2%", iconright2);
		if (player.getLeftMage1_1() == 0)
			content = content.replace("%iconmageult1%", iconmageult1 + dis);
		else
			content = content.replace("%iconmageult1%", iconmageult1);
		if (player.getLeftMage1_2() == 0)
			content = content.replace("%iconmageult2%", iconmageult2 + dis);
		else
			content = content.replace("%iconmageult2%", iconmageult2);
		if (player.getLeftMage1_1() == 0)
			content = content.replace("%iconmageult1%", iconmageult1 + dis);
		else
			content = content.replace("%iconmageult1%", iconmageult1);
		if (player.getLeftMage2_1() == 0)
			content = content.replace("%iconmageult3%", iconmageult3 + dis);
		else
			content = content.replace("%iconmageult3%", iconmageult3);
		if (player.getLeftMage2_2() == 0)
			content = content.replace("%iconmageult4%", iconmageult4 + dis);
		else
			content = content.replace("%iconmageult4%", iconmageult4);
		if (player.getRightMage1_1() == 0)
			content = content.replace("%iconmageult5%", iconmageult5 + dis);
		else
			content = content.replace("%iconmageult5%", iconmageult5);
		if (player.getRightMage1_2() == 0)
			content = content.replace("%iconmageult6%", iconmageult6 + dis);
		else
			content = content.replace("%iconmageult6%", iconmageult6);
		if (player.getRightMage2_1() == 0)
			content = content.replace("%iconmageult7%", iconmageult7 + dis);
		else
			content = content.replace("%iconmageult7%", iconmageult7);
		if (player.getRightMage2_2() == 0)
			content = content.replace("%iconmageult8%", iconmageult8 + dis);
		else
			content = content.replace("%iconmageult8%", iconmageult8);
		if (player.getMiddleMage() == 0)
		{
			content = content.replace("%statmiddle%", "M.Atk +400");
		}
		else if (player.getMiddleMage() == 1)
		{
			content = content.replace("%statmiddle%", "M.Atk +800");
		}
		else
		{
			content = content.replace("%statmiddle%", "M.Atk +1200");
		}
		if (player.getLeftMage() == 0)
		{
			content = content.replace("%statleft%", "C.Spd +10");
		}
		else if (player.getLeftMage() == 1)
		{
			content = content.replace("%statleft%", "C.Spd +30");
		}
		else
		{
			content = content.replace("%statleft%", "C.Spd +50");
		}
		if (player.getLeftMage1() == 0)
		{
			content = content.replace("%statleftone%", "M.Reu -2%");
		}
		else if (player.getLeftMage1() == 1)
		{
			content = content.replace("%statleftone%", "M.Reu -4%");
		}
		else
		{
			content = content.replace("%statleftone%", "M.Reu -6%");
		}
		if (player.getLeftMage2() == 0)
		{
			content = content.replace("%statlefttwo%", "C.Range +10");
		}
		else if (player.getLeftMage2() == 1)
		{
			content = content.replace("%statlefttwo%", "C.Range +20");
		}
		else
		{
			content = content.replace("%statlefttwo%", "C.Range +30");
		}
		if (player.getRightMage() == 0)
		{
			content = content.replace("%statright%", "MC.Rate +10");
		}
		else if (player.getRightMage() == 1)
		{
			content = content.replace("%statright%", "MC.Rate +20");
		}
		else
		{
			content = content.replace("%statright%", "MC.Rate +30");
		}
		if (player.getRightMage1() == 0)
		{
			content = content.replace("%statrightone%", "MC.Dmg +3%");
		}
		else if (player.getRightMage1() == 1)
		{
			content = content.replace("%statrightone%", "MC.Dmg +6%");
		}
		else
		{
			content = content.replace("%statrightone%", "MC.Dmg +9%");
		}
		if (player.getRightMage2() == 0)
		{
			content = content.replace("%statrighttwo%", "M.Pen +1%");
		}
		else if (player.getRightMage2() == 1)
		{
			content = content.replace("%statrighttwo%", "M.Pen +1.22%");
		}
		else
		{
			content = content.replace("%statrighttwo%", "M.Pen +1.5%");
		}
		return content;
	}
	
	private String replaceVarsDef(L2PcInstance player, String content)
	{
		String dis = "_dis";
		String iconmiddle = "l2trinity_cb_cp.cp_pmdef";
		String iconleft = "l2trinity_cb_cp.cp_shldrate";
		String iconleft1 = "l2trinity_cb_cp.cp_aggro";
		String iconleft2 = "l2trinity_cb_cp.cp_shlddef";
		String iconright = "l2trinity_cb_cp.cp_hp";
		String iconright1 = "l2trinity_cb_cp.cp_cp";
		String iconright2 = "l2trinity_cb_cp.cp_critvuln";
		String icondefult1 = "l2trinity_cb_cp.cp_aggro";
		String icondefult2 = "l2trinity_cb_cp.cp_aggro";
		String icondefult3 = "l2trinity_cb_cp.cp_def_ult_3";
		String icondefult4 = "l2trinity_cb_cp.cp_def_ult_4";
		String icondefult5 = "l2trinity_cb_cp.cp_def_ult_5";
		String icondefult6 = "l2trinity_cb_cp.cp_def_ult_6";
		String icondefult7 = "l2trinity_cb_cp.cp_def_ult_7";
		String icondefult8 = "l2trinity_cb_cp.cp_def_ult_8";
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		content = content.replace("%remain%", remain + "");
		content = content.replace("%middle%", player.getMiddleDef() + "/3");
		content = content.replace("%left%", player.getLeftDef() + "/3");
		content = content.replace("%left1%", player.getLeftDef1() + "/3");
		content = content.replace("%left2%", player.getLeftDef2() + "/3");
		content = content.replace("%right%", player.getRightDef() + "/3");
		content = content.replace("%right1%", player.getRightDef1() + "/3");
		content = content.replace("%right2%", player.getRightDef2() + "/3");
		content = content.replace("%left1_1%", player.getLeftDef1_1() + "/1");
		content = content.replace("%left1_2%", player.getLeftDef1_2() + "/1");
		content = content.replace("%left2_1%", player.getLeftDef2_1() + "/1");
		content = content.replace("%left2_2%", player.getLeftDef2_2() + "/1");
		content = content.replace("%right1_1%", player.getRightDef1_1() + "/1");
		content = content.replace("%right1_2%", player.getRightDef1_2() + "/1");
		content = content.replace("%right2_1%", player.getRightDef2_1() + "/1");
		content = content.replace("%right2_2%", player.getRightDef2_2() + "/1");
		if (player.getMiddleDef() == 0)
			content = content.replace("%iconmiddle%", iconmiddle + dis);
		else
			content = content.replace("%iconmiddle%", iconmiddle);
		if (player.getLeftDef() == 0)
			content = content.replace("%iconleft%", iconleft + dis);
		else
			content = content.replace("%iconleft%", iconleft);
		if (player.getLeftDef1() == 0)
			content = content.replace("%iconleft1%", iconleft1 + dis);
		else
			content = content.replace("%iconleft1%", iconleft1);
		if (player.getLeftDef2() == 0)
			content = content.replace("%iconleft2%", iconleft2 + dis);
		else
			content = content.replace("%iconleft2%", iconleft2);
		if (player.getRightDef() == 0)
			content = content.replace("%iconright%", iconright + dis);
		else
			content = content.replace("%iconright%", iconright);
		if (player.getRightDef1() == 0)
			content = content.replace("%iconright1%", iconright1 + dis);
		else
			content = content.replace("%iconright1%", iconright1);
		if (player.getRightDef2() == 0)
			content = content.replace("%iconright2%", iconright2 + dis);
		else
			content = content.replace("%iconright2%", iconright2);
		if (player.getLeftDef1_1() == 0)
			content = content.replace("%icondefult1%", icondefult1 + dis);
		else
			content = content.replace("%icondefult1%", icondefult1);
		if (player.getLeftDef1_2() == 0)
			content = content.replace("%icondefult2%", icondefult2 + dis);
		else
			content = content.replace("%icondefult2%", icondefult2);
		if (player.getLeftDef1_1() == 0)
			content = content.replace("%icondefult1%", icondefult1 + dis);
		else
			content = content.replace("%icondefult1%", icondefult1);
		if (player.getLeftDef2_1() == 0)
			content = content.replace("%icondefult3%", icondefult3 + dis);
		else
			content = content.replace("%icondefult3%", icondefult3);
		if (player.getLeftDef2_2() == 0)
			content = content.replace("%icondefult4%", icondefult4 + dis);
		else
			content = content.replace("%icondefult4%", icondefult4);
		if (player.getRightDef1_1() == 0)
			content = content.replace("%icondefult5%", icondefult5 + dis);
		else
			content = content.replace("%icondefult5%", icondefult5);
		if (player.getRightDef1_2() == 0)
			content = content.replace("%icondefult6%", icondefult6 + dis);
		else
			content = content.replace("%icondefult6%", icondefult6);
		if (player.getRightDef2_1() == 0)
			content = content.replace("%icondefult7%", icondefult7 + dis);
		else
			content = content.replace("%icondefult7%", icondefult7);
		if (player.getRightDef2_2() == 0)
			content = content.replace("%icondefult8%", icondefult8 + dis);
		else
			content = content.replace("%icondefult8%", icondefult8);
		if (player.getMiddleDef() == 0)
		{
			content = content.replace("%statmiddle%", "P-M.Def +40");
		}
		else if (player.getMiddleDef() == 1)
		{
			content = content.replace("%statmiddle%", "P-M.Def +80");
		}
		else
		{
			content = content.replace("%statmiddle%", "P-M.Def +120");
		}
		if (player.getLeftDef() == 0)
		{
			content = content.replace("%statleft%", "S.Rate +2%");
		}
		else if (player.getLeftDef() == 1)
		{
			content = content.replace("%statleft%", "S.Rate +4%");
		}
		else
		{
			content = content.replace("%statleft%", "S.Rate +6%");
		}
		if (player.getLeftDef1() == 0)
		{
			content = content.replace("%statleftone%", "Agro +4%");
		}
		else if (player.getLeftDef1() == 1)
		{
			content = content.replace("%statleftone%", "Agro +8%");
		}
		else
		{
			content = content.replace("%statleftone%", "Agro +13%");
		}
		if (player.getLeftDef2() == 0)
		{
			content = content.replace("%statlefttwo%", "S.Def +200");
		}
		else if (player.getLeftDef2() == 1)
		{
			content = content.replace("%statlefttwo%", "S.Def +400");
		}
		else
		{
			content = content.replace("%statlefttwo%", "S.Def +600");
		}
		if (player.getRightDef() == 0)
		{
			content = content.replace("%statright%", "HP +300");
		}
		else if (player.getRightDef() == 1)
		{
			content = content.replace("%statright%", "HP +600");
		}
		else
		{
			content = content.replace("%statright%", "HP +1000");
		}
		if (player.getRightDef1() == 0)
		{
			content = content.replace("%statrightone%", "CP +400");
		}
		else if (player.getRightDef1() == 1)
		{
			content = content.replace("%statrightone%", "CP +800");
		}
		else
		{
			content = content.replace("%statrightone%", "CP +1500");
		}
		if (player.getRightDef2() == 0)
		{
			content = content.replace("%statrighttwo%", "Crt Vuln -1%");
		}
		else if (player.getRightDef2() == 1)
		{
			content = content.replace("%statrighttwo%", "Crt Vuln -2%");
		}
		else
		{
			content = content.replace("%statrighttwo%", "Crt Vuln -4%");
		}
		return content;
	}
	
	private String replaceVarsSup(L2PcInstance player, String content)
	{
		String dis = "_dis";
		String iconmiddle = "l2trinity_cb_cp.cp_mreuse2";
		String iconleft = "l2trinity_cb_cp.cp_mana";
		String iconleft1 = "l2trinity_cb_cp.cp_heal";
		String iconleft2 = "l2trinity_cb_cp.cp_mpcons";
		String iconright = "l2trinity_cb_cp.cp_debeuffres";
		String iconright1 = "l2trinity_cb_cp.cp_critvul";
		String iconright2 = "l2trinity_cb_cp.cp_behinddmg";
		String iconutiult1 = "l2trinity_cb_cp.cp_uti_ult_1";
		String iconutiult2 = "l2trinity_cb_cp.cp_uti_ult_2";
		String iconutiult3 = "l2trinity_cb_cp.cp_uti_ult_3";
		String iconutiult4 = "l2trinity_cb_cp.cp_uti_ult_4";
		String iconutiult5 = "l2trinity_cb_cp.cp_uti_ult_5";
		String iconutiult6 = "l2trinity_cb_cp.cp_uti_ult_6";
		String iconutiult7 = "l2trinity_cb_cp.cp_uti_ult_7";
		String iconutiult8 = "l2trinity_cb_cp.cp_uti_ult_8";
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		content = content.replace("%remain%", remain + "");
		content = content.replace("%middle%", player.getMiddleSup() + "/3");
		content = content.replace("%left%", player.getLeftSup() + "/3");
		content = content.replace("%left1%", player.getLeftSup1() + "/3");
		content = content.replace("%left2%", player.getLeftSup2() + "/3");
		content = content.replace("%right%", player.getRightSup() + "/3");
		content = content.replace("%right1%", player.getRightSup1() + "/3");
		content = content.replace("%right2%", player.getRightSup2() + "/3");
		content = content.replace("%left1_1%", player.getLeftSup1_1() + "/1");
		content = content.replace("%left1_2%", player.getLeftSup1_2() + "/1");
		content = content.replace("%left2_1%", player.getLeftSup2_1() + "/1");
		content = content.replace("%left2_2%", player.getLeftSup2_2() + "/1");
		content = content.replace("%right1_1%", player.getRightSup1_1() + "/1");
		content = content.replace("%right1_2%", player.getRightSup1_2() + "/1");
		content = content.replace("%right2_1%", player.getRightSup2_1() + "/1");
		content = content.replace("%right2_2%", player.getRightSup2_2() + "/1");
		if (player.getMiddleSup() == 0)
			content = content.replace("%iconmiddle%", iconmiddle + dis);
		else
			content = content.replace("%iconmiddle%", iconmiddle);
		if (player.getLeftSup() == 0)
			content = content.replace("%iconleft%", iconleft + dis);
		else
			content = content.replace("%iconleft%", iconleft);
		if (player.getLeftSup1() == 0)
			content = content.replace("%iconleft1%", iconleft1 + dis);
		else
			content = content.replace("%iconleft1%", iconleft1);
		if (player.getLeftSup2() == 0)
			content = content.replace("%iconleft2%", iconleft2 + dis);
		else
			content = content.replace("%iconleft2%", iconleft2);
		if (player.getRightSup() == 0)
			content = content.replace("%iconright%", iconright + dis);
		else
			content = content.replace("%iconright%", iconright);
		if (player.getRightSup1() == 0)
			content = content.replace("%iconright1%", iconright1 + dis);
		else
			content = content.replace("%iconright1%", iconright1);
		if (player.getRightSup2() == 0)
			content = content.replace("%iconright2%", iconright2 + dis);
		else
			content = content.replace("%iconright2%", iconright2);
		if (player.getLeftSup1_1() == 0)
			content = content.replace("%iconutiult1%", iconutiult1 + dis);
		else
			content = content.replace("%iconutiult1%", iconutiult1);
		if (player.getLeftSup1_2() == 0)
			content = content.replace("%iconutiult2%", iconutiult2 + dis);
		else
			content = content.replace("%iconutiult2%", iconutiult2);
		if (player.getLeftSup1_1() == 0)
			content = content.replace("%iconutiult1%", iconutiult1 + dis);
		else
			content = content.replace("%iconutiult1%", iconutiult1);
		if (player.getLeftSup2_1() == 0)
			content = content.replace("%iconutiult3%", iconutiult3 + dis);
		else
			content = content.replace("%iconutiult3%", iconutiult3);
		if (player.getLeftSup2_2() == 0)
			content = content.replace("%iconutiult4%", iconutiult4 + dis);
		else
			content = content.replace("%iconutiult4%", iconutiult4);
		if (player.getRightSup1_1() == 0)
			content = content.replace("%iconutiult5%", iconutiult5 + dis);
		else
			content = content.replace("%iconutiult5%", iconutiult5);
		if (player.getRightSup1_2() == 0)
			content = content.replace("%iconutiult6%", iconutiult6 + dis);
		else
			content = content.replace("%iconutiult6%", iconutiult6);
		if (player.getRightSup2_1() == 0)
			content = content.replace("%iconutiult7%", iconutiult7 + dis);
		else
			content = content.replace("%iconutiult7%", iconutiult7);
		if (player.getRightSup2_2() == 0)
			content = content.replace("%iconutiult8%", iconutiult8 + dis);
		else
			content = content.replace("%iconutiult8%", iconutiult8);
		if (player.getMiddleSup() == 0)
		{
			content = content.replace("%statmiddle%", "M.Reuse -3%");
		}
		else if (player.getMiddleSup() == 1)
		{
			content = content.replace("%statmiddle%", "M.Reuse -6%");
		}
		else
		{
			content = content.replace("%statmiddle%", "M.Reuse -10%");
		}
		if (player.getLeftSup() == 0)
		{
			content = content.replace("%statleft%", "MP +500");
		}
		else if (player.getLeftSup() == 1)
		{
			content = content.replace("%statleft%", "MP +1000");
		}
		else
		{
			content = content.replace("%statleft%", "MP +1800");
		}
		if (player.getLeftSup1() == 0)
		{
			content = content.replace("%statleftone%", "Heal +3%");
		}
		else if (player.getLeftSup1() == 1)
		{
			content = content.replace("%statleftone%", "Heal +6%");
		}
		else
		{
			content = content.replace("%statleftone%", "Heal +10%");
		}
		if (player.getLeftSup2() == 0)
		{
			content = content.replace("%statlefttwo%", "Mp.Cons -2.5%");
		}
		else if (player.getLeftSup2() == 1)
		{
			content = content.replace("%statlefttwo%", "Mp.Cons -5%");
		}
		else
		{
			content = content.replace("%statlefttwo%", "Mp.Cons -8.5%");
		}
		if (player.getRightSup() == 0)
		{
			content = content.replace("%statright%", "Debuff Vuln -2% ");
		}
		else if (player.getRightSup() == 1)
		{
			content = content.replace("%statright%", "Debuff Vuln -4%");
		}
		else
		{
			content = content.replace("%statright%", "Debuff Vuln -10%");
		}
		if (player.getRightSup1() == 0)
		{
			content = content.replace("%statrightone%", "Crit Vuln -1%");
		}
		else if (player.getRightSup1() == 1)
		{
			content = content.replace("%statrightone%", "Crit Vuln -2%");
		}
		else
		{
			content = content.replace("%statrightone%", "Crit Vuln -4%");
		}
		if (player.getRightSup2() == 0)
		{
			content = content.replace("%statrighttwo%", "Behind.Dmg -2%");
		}
		else if (player.getRightSup2() == 1)
		{
			content = content.replace("%statrighttwo%", "Behind.Dmg -5%");
		}
		else
		{
			content = content.replace("%statrighttwo%", "Behind.Dmg -10%");
		}
		return content;
	}
	
	private void offensive(L2PcInstance player)
	{
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		String path = "data/html/CommunityBoard/classpath/";
		String filepath = "";
		String content = "";
		filepath = path + "offensive.htm";
		content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), filepath);
		content = replaceVarsOff(player, content);
		separateAndSend(content, player);
	}
	
	private void mage(L2PcInstance player)
	{
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		String path = "data/html/CommunityBoard/classpath/";
		String filepath = "";
		String content = "";
		filepath = path + "mage.htm";
		content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), filepath);
		content = replaceVarsMage(player, content);
		separateAndSend(content, player);
	}
	
	private void def(L2PcInstance player)
	{
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		String path = "data/html/CommunityBoard/classpath/";
		String filepath = "";
		String content = "";
		filepath = path + "def.htm";
		content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), filepath);
		content = replaceVarsDef(player, content);
		separateAndSend(content, player);
	}
	
	private void sup(L2PcInstance player)
	{
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		String path = "data/html/CommunityBoard/classpath/";
		String filepath = "";
		String content = "";
		filepath = path + "sup.htm";
		content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), filepath);
		content = replaceVarsSup(player, content);
		separateAndSend(content, player);
	}
	
	private void offensiveDesc(L2PcInstance player)
	{
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		String path = "data/html/CommunityBoard/classpath/";
		String filepath = "";
		String content = "";
		filepath = path + "offensiveDesc.htm";
		content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), filepath);
		content = replaceVarsOff(player, content);
		separateAndSend(content, player);
	}
	
	private void mageDesc(L2PcInstance player)
	{
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		String path = "data/html/CommunityBoard/classpath/";
		String filepath = "";
		String content = "";
		filepath = path + "mageDesc.htm";
		content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), filepath);
		content = replaceVarsMage(player, content);
		separateAndSend(content, player);
	}
	
	private void defDesc(L2PcInstance player)
	{
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		String path = "data/html/CommunityBoard/classpath/";
		String filepath = "";
		String content = "";
		filepath = path + "defDesc.htm";
		content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), filepath);
		content = replaceVarsDef(player, content);
		separateAndSend(content, player);
	}
	
	private void supDesc(L2PcInstance player)
	{
		int remain = player.getMaxCpPoints() - player.getCpPoints();
		String path = "data/html/CommunityBoard/classpath/";
		String filepath = "";
		String content = "";
		filepath = path + "supDesc.htm";
		content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), filepath);
		content = replaceVarsSup(player, content);
		separateAndSend(content, player);
	}
	
	private static void pak(L2PcInstance player)
	{
		MagicSkillUse MSU = new MagicSkillUse((L2Character) player, (L2Character) player, 888, 1, 1, 0);
		player.broadcastPacket((L2GameServerPacket) MSU);
		player.broadcastUserInfo();
		player.incCpPoints();
	}
	
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		if (command.equals("_bbsclasspath") || command.equals("_friendlist_0_"))
		{
			mainPath(activeChar);
		}
		else if (command.equals("_bbsclasspathoff"))
		{
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbsclasspathmage"))
		{
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbsclasspathdef"))
		{
			def(activeChar);
			return;
		}
		else if (command.equals("_bbsclasspathsup"))
		{
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbsclasspathoff_desc"))
		{
			offensiveDesc(activeChar);
			return;
		}
		else if (command.equals("_bbsclasspathmage_desc"))
		{
			mageDesc(activeChar);
			return;
		}
		else if (command.equals("_bbsclasspathdef_desc"))
		{
			defDesc(activeChar);
			return;
		}
		else if (command.equals("_bbsclasspathsup_desc"))
		{
			supDesc(activeChar);
			return;
		}
		else if (command.equals("_bbscpmiddleoff"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getMiddleOff() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9421, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleOff();
			}
			else if (activeChar.getMiddleOff() == 1)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9422, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleOff();
			}
			else if (activeChar.getMiddleOff() == 2)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9423, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleOff();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftoff"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints() || activeChar.getRightOff() >= 1)
			{
				if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
					activeChar.sendMessage("You don't have enough path points.");
				if (activeChar.getRightOff() >= 1)
					activeChar.sendMessage("You can't choose both left and right path.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getLeftOff() == 0 && activeChar.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9424, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff();
			}
			else if (activeChar.getLeftOff() == 1 && activeChar.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9425, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff();
			}
			else if (activeChar.getLeftOff() == 2 && activeChar.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9426, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftoffone"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getLeftOff2() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getLeftOff1() == 0 && activeChar.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9427, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff1();
			}
			else if (activeChar.getLeftOff1() == 1 && activeChar.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9428, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff1();
			}
			else if (activeChar.getLeftOff1() == 2 && activeChar.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9429, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff1();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftofftwo"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getLeftOff1() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getLeftOff2() == 0 && activeChar.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9430, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff2();
			}
			else if (activeChar.getLeftOff2() == 1 && activeChar.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9431, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff2();
			}
			else if (activeChar.getLeftOff2() == 2 && activeChar.getLeftOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9432, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff2();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscprightoff"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints() || activeChar.getLeftOff() >= 1)
			{
				if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
					activeChar.sendMessage("You don't have enough path points.");
				if (activeChar.getLeftOff() >= 1)
					activeChar.sendMessage("You can't choose both left and right path.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getRightOff() == 0 && activeChar.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9433, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff();
			}
			else if (activeChar.getRightOff() == 1 && activeChar.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9434, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff();
			}
			else if (activeChar.getRightOff() == 2 && activeChar.getMiddleOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9435, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscprightoffone"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getRightOff2() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getRightOff1() == 0 && activeChar.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9436, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff1();
			}
			else if (activeChar.getRightOff1() == 1 && activeChar.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9437, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff1();
			}
			else if (activeChar.getRightOff1() == 2 && activeChar.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9438, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff1();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscprightofftwo"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getRightOff1() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getRightOff2() == 0 && activeChar.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9439, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff2();
			}
			else if (activeChar.getRightOff2() == 1 && activeChar.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9440, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff2();
			}
			else if (activeChar.getRightOff2() == 2 && activeChar.getRightOff() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9441, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff2();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftoffone_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getLeftOff1_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftOff1_2() == 0 && activeChar.getLeftOff1() >= 3 && activeChar.getLeftOff1_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94270, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff1_1();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftoffone_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getLeftOff1_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftOff1_1() == 0 && activeChar.getLeftOff1() >= 3 && activeChar.getLeftOff1_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94271, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff1_2();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftofftwo_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getLeftOff2_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftOff2_2() == 0 && activeChar.getLeftOff2() >= 3 && activeChar.getLeftOff2_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94300, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff2_1();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftofftwo_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getLeftOff2_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftOff2_1() == 0 && activeChar.getLeftOff2() >= 3 && activeChar.getLeftOff2_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94302, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftOff2_2();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscprightoffone_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getRightOff1_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightOff1_2() == 0 && activeChar.getRightOff1() >= 3 && activeChar.getRightOff1_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94350, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff1_1();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscprightoffone_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getRightOff1_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightOff1_1() == 0 && activeChar.getRightOff1() >= 3 && activeChar.getRightOff1_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94351, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff1_2();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscprightofftwo_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getRightOff2_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightOff2_2() == 0 && activeChar.getRightOff2() >= 3 && activeChar.getRightOff2_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94390, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff2_1();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscprightofftwo_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				offensive(activeChar);
				return;
			}
			if (activeChar.getRightOff2_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightOff2_1() == 0 && activeChar.getRightOff2() >= 3 && activeChar.getRightOff2_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94391, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightOff2_2();
			}
			else
			{
				offensive(activeChar);
				return;
			}
			pak(activeChar);
			offensive(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftmageone_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getLeftMage1_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftMage1_2() == 0 && activeChar.getLeftMage1() >= 3 && activeChar.getLeftMage1_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94501, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage1_1();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftmageone_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getLeftMage1_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftMage1_1() == 0 && activeChar.getLeftMage1() >= 3 && activeChar.getLeftMage1_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94504, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage1_2();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftmagetwo_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getLeftMage2_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftMage2_2() == 0 && activeChar.getLeftMage2() >= 3 && activeChar.getLeftMage2_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94510, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage2_1();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftmagetwo_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getLeftMage2_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftMage2_1() == 0 && activeChar.getLeftMage2() >= 3 && activeChar.getLeftMage2_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94512, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage2_2();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscprightmageone_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getRightMage1_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightMage1_2() == 0 && activeChar.getRightMage1() >= 3 && activeChar.getRightMage1_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94570, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage1_1();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscprightmageone_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getRightMage1_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightMage1_1() == 0 && activeChar.getRightMage1() >= 3 && activeChar.getRightMage1_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94571, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage1_2();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscprightmagetwo_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getRightMage2_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightMage2_2() == 0 && activeChar.getRightMage2() >= 3 && activeChar.getRightMage2_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94600, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage2_1();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscprightmagetwo_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getRightMage2_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightMage2_1() == 0 && activeChar.getRightMage2() >= 3 && activeChar.getRightMage2_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94603, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage2_2();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftdefone_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getLeftDef1_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftDef1_2() == 0 && activeChar.getLeftDef1() >= 3 && activeChar.getLeftDef1_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94710, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef1_1();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftdefone_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getLeftDef1_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftDef1_1() == 0 && activeChar.getLeftDef1() >= 3 && activeChar.getLeftDef1_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94711, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef1_2();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftdeftwo_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getLeftDef2_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftDef2_2() == 0 && activeChar.getLeftDef2() >= 3 && activeChar.getLeftDef2_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94740, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef2_1();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftdeftwo_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getLeftDef2_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftDef2_1() == 0 && activeChar.getLeftDef2() >= 3 && activeChar.getLeftDef2_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94741, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef2_2();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscprightdefone_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getRightDef1_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightDef1_2() == 0 && activeChar.getRightDef1() >= 3 && activeChar.getRightDef1_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94770, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef1_1();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscprightdefone_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getRightDef1_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightDef1_1() == 0 && activeChar.getRightDef1() >= 3 && activeChar.getRightDef1_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94772, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef1_2();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscprightdeftwo_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getRightDef2_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightDef2_2() == 0 && activeChar.getRightDef2() >= 3 && activeChar.getRightDef2_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94830, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef2_1();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscprightdeftwo_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getRightDef2_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightDef2_1() == 0 && activeChar.getRightDef2() >= 3 && activeChar.getRightDef2_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94832, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef2_2();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftsupone_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getLeftSup1_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftSup1_2() == 0 && activeChar.getLeftSup1() >= 3 && activeChar.getLeftSup1_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94920, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup1_1();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftsupone_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getLeftSup1_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftSup1_1() == 0 && activeChar.getLeftSup1() >= 3 && activeChar.getLeftSup1_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94922, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup1_2();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftsuptwo_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getLeftSup2_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftSup2_2() == 0 && activeChar.getLeftSup2() >= 3 && activeChar.getLeftSup2_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94950, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup2_1();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftsuptwo_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getLeftSup2_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getLeftSup2_1() == 0 && activeChar.getLeftSup2() >= 3 && activeChar.getLeftSup2_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(94952, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup2_2();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscprightsupone_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getRightSup1_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightSup1_2() == 0 && activeChar.getRightSup1() >= 3 && activeChar.getRightSup1_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(95010, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup1_1();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscprightsupone_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getRightSup1_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightSup1_1() == 0 && activeChar.getRightSup1() >= 3 && activeChar.getRightSup1_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(95012, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup1_2();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscprightsuptwo_1"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getRightSup2_2() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightSup2_2() == 0 && activeChar.getRightSup2() >= 3 && activeChar.getRightSup2_1() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(95040, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup2_1();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscprightsuptwo_2"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getRightSup2_1() == 1)
			{
				activeChar.sendMessage("You can only learn one Ultimate of each tree.");
				return;
			}
			if (activeChar.getRightSup2_1() == 0 && activeChar.getRightSup2() >= 3 && activeChar.getRightSup2_2() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(95041, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup2_2();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscpmiddlemage"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getMiddleMage() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9442, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleMage();
			}
			else if (activeChar.getMiddleMage() == 1)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9443, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleMage();
			}
			else if (activeChar.getMiddleMage() == 2)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9444, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleMage();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftmage"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints() || activeChar.getRightMage() >= 1)
			{
				if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
					activeChar.sendMessage("You don't have enough path points.");
				if (activeChar.getRightMage() >= 1)
					activeChar.sendMessage("You can't choose both left and right path.");
				mage(activeChar);
				return;
			}
			if (activeChar.getLeftMage() == 0 && activeChar.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9445, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage();
			}
			else if (activeChar.getLeftMage() == 1 && activeChar.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9446, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage();
			}
			else if (activeChar.getLeftMage() == 2 && activeChar.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9447, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftmageone"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getLeftMage2() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				mage(activeChar);
				return;
			}
			if (activeChar.getLeftMage1() == 0 && activeChar.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9448, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage1();
			}
			else if (activeChar.getLeftMage1() == 1 && activeChar.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9449, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage1();
			}
			else if (activeChar.getLeftMage1() == 2 && activeChar.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9450, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage1();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftmagetwo"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getLeftMage1() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				mage(activeChar);
				return;
			}
			if (activeChar.getLeftMage2() == 0 && activeChar.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9451, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage2();
			}
			else if (activeChar.getLeftMage2() == 1 && activeChar.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9452, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage2();
			}
			else if (activeChar.getLeftMage2() == 2 && activeChar.getLeftMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9453, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftMage2();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscprightmage"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints() || activeChar.getLeftMage() >= 1)
			{
				if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
					activeChar.sendMessage("You don't have enough path points.");
				if (activeChar.getLeftMage() >= 1)
					activeChar.sendMessage("You can't choose both left and right path.");
				mage(activeChar);
				return;
			}
			if (activeChar.getRightMage() == 0 && activeChar.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9454, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage();
			}
			else if (activeChar.getRightMage() == 1 && activeChar.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9455, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage();
			}
			else if (activeChar.getRightMage() == 2 && activeChar.getMiddleMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9456, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscprightmageone"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getRightMage2() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				mage(activeChar);
				return;
			}
			if (activeChar.getRightMage1() == 0 && activeChar.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9457, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage1();
			}
			else if (activeChar.getRightMage1() == 1 && activeChar.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9458, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage1();
			}
			else if (activeChar.getRightMage1() == 2 && activeChar.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9459, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage1();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscprightmagetwo"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				mage(activeChar);
				return;
			}
			if (activeChar.getRightMage1() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				mage(activeChar);
				return;
			}
			if (activeChar.getRightMage2() == 0 && activeChar.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9460, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage2();
			}
			else if (activeChar.getRightMage2() == 1 && activeChar.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9461, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage2();
			}
			else if (activeChar.getRightMage2() == 2 && activeChar.getRightMage() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9462, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightMage2();
			}
			else
			{
				mage(activeChar);
				return;
			}
			pak(activeChar);
			mage(activeChar);
			return;
		}
		else if (command.equals("_bbscpmiddledef"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getMiddleDef() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9463, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleDef();
			}
			else if (activeChar.getMiddleDef() == 1)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9464, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleDef();
			}
			else if (activeChar.getMiddleDef() == 2)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9465, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleDef();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftdef"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints() || activeChar.getRightDef() >= 1)
			{
				if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
					activeChar.sendMessage("You don't have enough path points.");
				if (activeChar.getRightDef() >= 1)
					activeChar.sendMessage("You can't choose both left and right path.");
				def(activeChar);
				return;
			}
			if (activeChar.getLeftDef() == 0 && activeChar.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9466, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef();
			}
			else if (activeChar.getLeftDef() == 1 && activeChar.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9467, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef();
			}
			else if (activeChar.getLeftDef() == 2 && activeChar.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9468, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftdefone"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getLeftDef2() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				def(activeChar);
				return;
			}
			if (activeChar.getLeftDef1() == 0 && activeChar.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9469, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef1();
			}
			else if (activeChar.getLeftDef1() == 1 && activeChar.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9470, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef1();
			}
			else if (activeChar.getLeftDef1() == 2 && activeChar.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9471, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef1();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftdeftwo"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getLeftDef1() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				def(activeChar);
				return;
			}
			if (activeChar.getLeftDef2() == 0 && activeChar.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9472, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef2();
			}
			else if (activeChar.getLeftDef2() == 1 && activeChar.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9473, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef2();
			}
			else if (activeChar.getLeftDef2() == 2 && activeChar.getLeftDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9474, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftDef2();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscprightdef"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints() || activeChar.getLeftDef() >= 1)
			{
				if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
					activeChar.sendMessage("You don't have enough path points.");
				if (activeChar.getLeftDef() >= 1)
					activeChar.sendMessage("You can't choose both left and right path.");
				def(activeChar);
				return;
			}
			if (activeChar.getRightDef() == 0 && activeChar.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9475, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef();
			}
			else if (activeChar.getRightDef() == 1 && activeChar.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9476, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef();
			}
			else if (activeChar.getRightDef() == 2 && activeChar.getMiddleDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9477, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscprightdefone"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getRightDef2() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				def(activeChar);
				return;
			}
			if (activeChar.getRightDef1() == 0 && activeChar.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9478, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef1();
			}
			else if (activeChar.getRightDef1() == 1 && activeChar.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9479, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef1();
			}
			else if (activeChar.getRightDef1() == 2 && activeChar.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9480, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef1();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscprightdeftwo"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				def(activeChar);
				return;
			}
			if (activeChar.getRightDef1() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				def(activeChar);
				return;
			}
			if (activeChar.getRightDef2() == 0 && activeChar.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9481, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef2();
			}
			else if (activeChar.getRightDef2() == 1 && activeChar.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9482, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef2();
			}
			else if (activeChar.getRightDef2() == 2 && activeChar.getRightDef() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9483, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightDef2();
			}
			else
			{
				def(activeChar);
				return;
			}
			pak(activeChar);
			def(activeChar);
			return;
		}
		else if (command.equals("_bbscpmiddlesup"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getMiddleSup() == 0)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9484, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleSup();
			}
			else if (activeChar.getMiddleSup() == 1)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9485, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleSup();
			}
			else if (activeChar.getMiddleSup() == 2)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9486, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incMiddleSup();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftsup"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints() || activeChar.getRightSup() >= 1)
			{
				if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
					activeChar.sendMessage("You don't have enough path points.");
				if (activeChar.getRightSup() >= 1)
					activeChar.sendMessage("You can't choose both left and right path.");
				sup(activeChar);
				return;
			}
			if (activeChar.getLeftSup() == 0 && activeChar.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9487, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup();
			}
			else if (activeChar.getLeftSup() == 1 && activeChar.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9488, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup();
			}
			else if (activeChar.getLeftSup() == 2 && activeChar.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9489, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftsupone"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getLeftSup2() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				sup(activeChar);
				return;
			}
			if (activeChar.getLeftSup1() == 0 && activeChar.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9490, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup1();
			}
			else if (activeChar.getLeftSup1() == 1 && activeChar.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9491, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup1();
			}
			else if (activeChar.getLeftSup1() == 2 && activeChar.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9492, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup1();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscpleftsuptwo"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getLeftSup1() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				sup(activeChar);
				return;
			}
			if (activeChar.getLeftSup2() == 0 && activeChar.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9493, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup2();
			}
			else if (activeChar.getLeftSup2() == 1 && activeChar.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9494, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup2();
			}
			else if (activeChar.getLeftSup2() == 2 && activeChar.getLeftSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9495, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incLeftSup2();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscprightsup"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints() || activeChar.getLeftSup() >= 1)
			{
				if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
					activeChar.sendMessage("You don't have enough path points.");
				if (activeChar.getLeftSup() >= 1)
					activeChar.sendMessage("You can't choose both left and right path.");
				sup(activeChar);
				return;
			}
			if (activeChar.getRightSup() == 0 && activeChar.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9496, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup();
			}
			else if (activeChar.getRightSup() == 1 && activeChar.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9497, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup();
			}
			else if (activeChar.getRightSup() == 2 && activeChar.getMiddleSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9498, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscprightsupone"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getRightSup2() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				sup(activeChar);
				return;
			}
			if (activeChar.getRightSup1() == 0 && activeChar.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9499, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup1();
			}
			else if (activeChar.getRightSup1() == 1 && activeChar.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9500, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup1();
			}
			else if (activeChar.getRightSup1() == 2 && activeChar.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9501, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup1();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equals("_bbscprightsuptwo"))
		{
			if (activeChar.getCpPoints() >= activeChar.getMaxCpPoints())
			{
				activeChar.sendMessage("You don't have enough path points.");
				sup(activeChar);
				return;
			}
			if (activeChar.getRightSup1() >= 1)
			{
				activeChar.sendMessage("You can't change your path now.");
				sup(activeChar);
				return;
			}
			if (activeChar.getRightSup2() == 0 && activeChar.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9502, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup2();
			}
			else if (activeChar.getRightSup2() == 1 && activeChar.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9503, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup2();
			}
			else if (activeChar.getRightSup2() == 2 && activeChar.getRightSup() >= 3)
			{
				L2Skill Skill = SkillTable.getInstance().getInfo(9504, 1);
				activeChar.addSkill(Skill, true);
				activeChar.incRightSup2();
			}
			else
			{
				sup(activeChar);
				return;
			}
			pak(activeChar);
			sup(activeChar);
			return;
		}
		else if (command.equalsIgnoreCase("_bbscpstoreCp"))
		{
			storeCP(activeChar);
		}
		else if (command.equals("_bbscpcancelpath"))
		{
			MagicSkillUse MSU = new MagicSkillUse((L2Character) activeChar, (L2Character) activeChar, 5426, 1, 1, 0);
			activeChar.broadcastPacket((L2GameServerPacket) MSU);
			activeChar.clearPath();
			activeChar.broadcastUserInfo();
			mainPath(activeChar);
		}
		else if (command.equals("_bbscpcancelpathoff"))
		{
			MagicSkillUse MSU = new MagicSkillUse((L2Character) activeChar, (L2Character) activeChar, 5426, 1, 1, 0);
			activeChar.broadcastPacket((L2GameServerPacket) MSU);
			activeChar.clearPathOffensive();
			activeChar.broadcastUserInfo();
			offensive(activeChar);
		}
		else if (command.equals("_bbscpcancelpathmage"))
		{
			MagicSkillUse MSU = new MagicSkillUse((L2Character) activeChar, (L2Character) activeChar, 5426, 1, 1, 0);
			activeChar.broadcastPacket((L2GameServerPacket) MSU);
			activeChar.clearPathMage();
			activeChar.broadcastUserInfo();
			mage(activeChar);
		}
		else if (command.equals("_bbscpcancelpathdef"))
		{
			MagicSkillUse MSU = new MagicSkillUse((L2Character) activeChar, (L2Character) activeChar, 5426, 1, 1, 0);
			activeChar.broadcastPacket((L2GameServerPacket) MSU);
			activeChar.clearPathDef();
			activeChar.broadcastUserInfo();
			def(activeChar);
		}
		else if (command.equals("_bbscpcancelpathsup"))
		{
			MagicSkillUse MSU = new MagicSkillUse((L2Character) activeChar, (L2Character) activeChar, 5426, 1, 1, 0);
			activeChar.broadcastPacket((L2GameServerPacket) MSU);
			activeChar.clearPathSup();
			activeChar.broadcastUserInfo();
			sup(activeChar);
		}
		else
		{
			ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", "101");
			activeChar.sendPacket(sb);
			activeChar.sendPacket(new ShowBoard(null, "102"));
			activeChar.sendPacket(new ShowBoard(null, "103"));
		}
	}
	
	private void storeCP(L2PcInstance activeChar)
	{
		Connection con = null;
		Connection con2 = null;
		try
		{
			PreparedStatement statement;
			PreparedStatement statement2;
			con = L2DatabaseFactory.getInstance().getConnection();
			con2 = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM class_paths WHERE objid = ?");
			statement2 = con2.prepareStatement("INSERT INTO class_paths_copy VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, activeChar.getObjectId());
			//statement.executeUpdate();
			//statement.close();
			ResultSet rset = statement.executeQuery();
			statement2.setInt(1, activeChar.getObjectId());
			statement2.setInt(2, ClassPathUID + 1);
			statement2.setString(3, "22");
			rset.next();
				int counter = 2;
					for (int rsetCounter = 1; rsetCounter < 61; rsetCounter++)
					{
						counter++;
						statement2.setInt(counter, rset.getInt(rsetCounter+1));
					}
			rset.close();
			statement.close();
			statement2.executeUpdate();
			statement2.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
				con2.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	protected void separateAndSend(String html, L2PcInstance acha)
	{
		if (html == null)
			return;
		acha.sendPacket(new ShowBoard(html, "101"));
	}
	
	private static class SingletonHolder
	{
		protected static final ClassPathsBBSManager _instance = new ClassPathsBBSManager();
	}
}
