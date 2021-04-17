package luna.custom.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LoggerObject
{
  protected final Logger _log = LoggerFactory.getLogger(getClass());
  
  public void error(String st, Exception e)
  {
    this._log.error(getClass().getSimpleName() + ": " + st, e);
  }
  
  public void error(String st)
  {
    this._log.error(getClass().getSimpleName() + ": " + st);
  }
  
  public void warn(String st, Exception e)
  {
    this._log.warn(getClass().getSimpleName() + ": " + st, e);
  }
  
  public void warn(String st)
  {
    this._log.warn(getClass().getSimpleName() + ": " + st);
  }
  
  public void info(String st, Exception e)
  {
    this._log.info(getClass().getSimpleName() + ": " + st, e);
  }
  
  public void info(String st)
  {
    this._log.info(getClass().getSimpleName() + ": " + st);
  }
}
