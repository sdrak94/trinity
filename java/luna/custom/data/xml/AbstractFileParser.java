package luna.custom.data.xml;

import java.io.File;
import java.io.FileInputStream;

public abstract class AbstractFileParser<H extends AbstractHolder>
  extends AbstractParser<H>
{
  protected AbstractFileParser(H holder)
  {
    super(holder);
  }
  
  public abstract File getXMLFile();
  
  protected final void parse()
  {
    File file = getXMLFile();
    if (!file.exists())
    {
      warn("file " + file.getAbsolutePath() + " not exists");
      return;
    }
    try
    {
      parseDocument(new FileInputStream(file), file.getName());
    }
    catch (Exception e)
    {
      warn("Exception: " + e, e);
    }
  }
}
