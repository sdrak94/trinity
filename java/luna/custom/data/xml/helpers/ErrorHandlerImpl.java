package luna.custom.data.xml.helpers;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import luna.custom.data.xml.AbstractParser;

public class ErrorHandlerImpl
  implements ErrorHandler
{
  private final AbstractParser<?> _parser;
  
  public ErrorHandlerImpl(AbstractParser<?> parser)
  {
    this._parser = parser;
  }
  
  public void warning(SAXParseException exception)
  {
    this._parser.warn("File: " + this._parser.getCurrentFileName() + ":" + exception.getLineNumber() + " warning: " + exception.getMessage());
  }
  
  public void error(SAXParseException exception)
  {
    this._parser.error("File: " + this._parser.getCurrentFileName() + ":" + exception.getLineNumber() + " error: " + exception.getMessage());
  }
  
  public void fatalError(SAXParseException exception)
  {
    this._parser.error("File: " + this._parser.getCurrentFileName() + ":" + exception.getLineNumber() + " fatal: " + exception.getMessage());
  }
}
