package luna.custom.antibot.configsEngine.configs.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import luna.custom.antibot.configsEngine.AbstractConfigs;
import net.sf.l2j.util.L2Properties;

public class AntibotConfigs extends AbstractConfigs
{
  private static final String ANTIBOT_CONFIG_FILE = "./config/AntiBot.ini";
  public static boolean ENABLE_ANTIBOT_SYSTEMS;
  public static boolean ENABLE_DOUBLE_PROTECTION;
  public static boolean ENABLE_ANTIBOT_FOR_GMS;
  public static boolean ENABLE_ANTIBOT_FARM_SYSTEM;
  public static boolean ENABLE_ANTIBOT_FARM_SYSTEM_ON_RAIDS;
  public static int JAIL_TIMER;
  public static int TIME_TO_SPEND_IN_JAIL;
  public static int ANTIBOT_FARM_TYPE;
  public static float ANTIBOT_FARM_CHANCE;
  public static int ANTIBOT_MOB_COUNTER;
  public static boolean ENABLE_ANTIBOT_SPECIFIC_MOBS;
  public static List<Integer> ANTIBOT_FARM_MOBS_IDS;
  public static boolean ENABLE_ANTIBOT_ENCHANT_SYSTEM;
  public static int ENCHANT_CHANCE_TIMER;
  public static int ENCHANT_CHANCE_PERCENT_TO_START;
  public static int ENCHANT_CHANCE_PERCENT_TO_LOW;
  public static int ANTIBOT_ENCHANT_TYPE;
  public static int ANTIBOT_ENCHANT_COUNTER;
  public static int ANTIBOT_ENCHANT_CHANCE;
  
  public AntibotConfigs() {}
  
  public void loadConfigs()
  {
    L2Properties AntibotProperties = new L2Properties();
    File antibot = new File("./config/AntiBot.ini");
    try { InputStream is = new FileInputStream(antibot);
    Object localObject1 = null;
      try
      {
        AntibotProperties.load(is);
      }
      catch (Throwable localThrowable1)
      {
        localObject1 = localThrowable1;throw localThrowable1;
      }
      finally {
        if (is != null) if (localObject1 != null) try { is.close(); } catch (Throwable localThrowable2) { ((Throwable)localObject1).addSuppressed(localThrowable2); } else is.close();
      }
    } catch (Exception e) {
      _log.error("Error while loading Antibot settings!", e);
    }
    

    ENABLE_ANTIBOT_SYSTEMS = Boolean.parseBoolean(AntibotProperties.getProperty("EnableAntibotSystems", "True"));
    ENABLE_DOUBLE_PROTECTION = Boolean.parseBoolean(AntibotProperties.getProperty("EnableDoubleProtection", "True"));
    ENABLE_ANTIBOT_FOR_GMS = Boolean.parseBoolean(AntibotProperties.getProperty("EnableAntibotForGms", "True"));
    

    ENABLE_ANTIBOT_FARM_SYSTEM = Boolean.parseBoolean(AntibotProperties.getProperty("EnableAntibotFarmSystem", "True"));
    ENABLE_ANTIBOT_FARM_SYSTEM_ON_RAIDS = Boolean.parseBoolean(AntibotProperties.getProperty("EnableAntibotFarmSystemOnRaids", "True"));
    JAIL_TIMER = Integer.parseInt(AntibotProperties.getProperty("JailTimer", "180"));
    TIME_TO_SPEND_IN_JAIL = Integer.parseInt(AntibotProperties.getProperty("TimeToSpendInJail", "180"));
    ENABLE_ANTIBOT_SPECIFIC_MOBS = Boolean.parseBoolean(AntibotProperties.getProperty("EnableAntibotSpecificMobs", "True"));
    ANTIBOT_FARM_TYPE = Integer.parseInt(AntibotProperties.getProperty("AntibotFarmType", "0"));
    ANTIBOT_FARM_CHANCE = Float.parseFloat(AntibotProperties.getProperty("AntibotFarmChance", "50"));
    ANTIBOT_MOB_COUNTER = Integer.parseInt(AntibotProperties.getProperty("AntibotMobCounter", "100"));
    /*Object localObject1 = mobIds;
    int localThrowable1 = localObject1.toString().length();
    for (int localThrowable3 = 0;
    		localThrowable3 < localThrowable1;
    		localThrowable3++)
    {
    	String mobId = localObject1[localThrowable3];
      
      try
      {
        ANTIBOT_FARM_MOBS_IDS.add(Integer.valueOf(Integer.parseInt(mobId.trim())));
      }
      catch (NumberFormatException e)
      {
        _log.info("Antibot System: Error parsing mob id. Skipping " + mobId + ".");
      }
    }*/
    

    ENABLE_ANTIBOT_ENCHANT_SYSTEM = Boolean.parseBoolean(AntibotProperties.getProperty("EnableAntibotEnchantSystem", "True"));
    ENCHANT_CHANCE_TIMER = Integer.parseInt(AntibotProperties.getProperty("EnchantChanceTimer", "180"));
    ENCHANT_CHANCE_PERCENT_TO_START = Integer.parseInt(AntibotProperties.getProperty("EnchantChancePercentToStart", "80"));
    ENCHANT_CHANCE_PERCENT_TO_LOW = Integer.parseInt(AntibotProperties.getProperty("EnchantChancePercentToLow", "10"));
    ANTIBOT_ENCHANT_TYPE = Integer.parseInt(AntibotProperties.getProperty("AntibotEnchantType", "0"));
    ANTIBOT_ENCHANT_COUNTER = Integer.parseInt(AntibotProperties.getProperty("AntibotEnchantCounter", "100"));
    ANTIBOT_ENCHANT_CHANCE = Integer.parseInt(AntibotProperties.getProperty("AntibotEnchantChance", "100"));
  }
  
  public static AntibotConfigs getInstance()
  {
    return SingletonHolder._instance;
  }
  
  private static class SingletonHolder
  {
    protected static final AntibotConfigs _instance = new AntibotConfigs();
    
    private SingletonHolder() {}
  }
}
