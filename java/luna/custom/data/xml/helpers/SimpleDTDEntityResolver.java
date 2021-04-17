package luna.custom.data.xml.helpers;

import java.io.File;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class SimpleDTDEntityResolver
  implements EntityResolver
{
  private final String _fileName;
  
  public SimpleDTDEntityResolver(File f)
  {
    this._fileName = f.getAbsolutePath();
  }
  
  public InputSource resolveEntity(String publicId, String systemId)
  {
    return new InputSource(this._fileName);
  }
}
