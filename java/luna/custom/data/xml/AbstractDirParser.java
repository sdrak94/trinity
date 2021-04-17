package luna.custom.data.xml;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

public abstract class AbstractDirParser<H extends AbstractHolder>
  extends AbstractParser<H>
{
  protected AbstractDirParser(H holder)
  {
    super(holder);
  }
  
  public abstract File getXMLDir();
  
  public abstract boolean isIgnored(File paramFile);
  
  protected final void parse()
  {
    File dir = getXMLDir();
    if (!dir.exists())
    {
      warn("Dir " + dir.getAbsolutePath() + " not exists");
      return;
    }
    try
    {
      Collection<File> files = FileUtils.listFiles(dir, FileFilterUtils.suffixFileFilter(".xml"), FileFilterUtils.directoryFileFilter());
      for (File f : files) {
        if (!f.isHidden()) {
          if (!isIgnored(f)) {
            try
            {
              parseCrypted(f);
            }
            catch (Exception e)
            {
              info("Exception: " + e + " in file: " + f.getName(), e);
            }
          }
        }
      }
    }
    catch (Exception e)
    {
      warn("Exception: " + e, e);
    }
  }
}
