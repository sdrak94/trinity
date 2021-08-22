package luna.custom.data.xml;

import java.io.File;
import java.io.InputStream;

import org.dom4j.io.SAXReader;

import luna.custom.data.xml.helpers.ErrorHandlerImpl;
import luna.custom.logging.LoggerObject;

public abstract class AbstractParser<H extends AbstractHolder> extends LoggerObject
{
	protected final H	_holder;
	protected String	_currentFile;
	protected SAXReader	_reader;
	
	protected AbstractParser(H holder)
	{
		this._holder = holder;
		this._reader = new SAXReader();
		this._reader.setValidation(true);
		this._reader.setErrorHandler(new ErrorHandlerImpl(this));
	}
	
	protected void parseDocument(InputStream f, String name)
	{
		this._currentFile = name;
		readData();
	}
	
	protected void parseCrypted(File file)
	{}
	
	protected abstract void readData();
	
	protected abstract void parse();
	
	protected H getHolder()
	{
		return this._holder;
	}
	
	public String getCurrentFileName()
	{
		return this._currentFile;
	}
	
	public void load()
	{
		parse();
		this._holder.process();
		this._holder.log();
	}
	
	public void reload()
	{
		info("reload start...");
		this._holder.clear();
		load();
	}
}
