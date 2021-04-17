package luna.custom.antibot.configsEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConfigs
{
  public static final Logger _log = LoggerFactory.getLogger(AbstractConfigs.class);
  
  public AbstractConfigs() {}
  
  public abstract void loadConfigs();
}
